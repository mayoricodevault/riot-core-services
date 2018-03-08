package com.tierconnect.riot.migration;

import com.hazelcast.util.StringUtil;
import com.tierconnect.riot.appcore.utils.Configuration;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.*;

/**
 * Created by cvertiz on 01/10/17.
 */
public class DBHelper {
    static Logger logger = Logger.getLogger(DBHelper.class);
    //private int DBVersionTableCurrentVersion;

    public static final String MYSQLDB = "MYSQL";
    public static final String MSSQLDB = "MSSQL";

    public static Connection getConnection() {
        Connection conn = null;
        Properties connectionProps = new Properties();
        connectionProps.put("user", Configuration.getProperty("hibernate.connection.username"));
        connectionProps.put("password", Configuration.getProperty("hibernate.connection.password"));
        connectionProps.put("connectTimeout", "30000");

        boolean retry = true;
        do{
            try {
                conn = DriverManager.getConnection(Configuration.getProperty("hibernate.connection.url"), connectionProps);
                retry = false;
            } catch (com.mysql.jdbc.exceptions.jdbc4.CommunicationsException e) {
                logger.warn("Startup process for [Services] waiting for [MySQL], retry in 30s");
                try {
                    Thread.sleep(30000);
                } catch (InterruptedException e1) {
                    Thread.currentThread().interrupt();
                }
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
            }

        }while(retry);
        return conn;
    }

    public boolean existTable(String table) {
        Connection conn = null;
        try {
            conn = getConnection();
            DatabaseMetaData metadata = conn.getMetaData();
            ResultSet resultSet;
            resultSet = metadata.getTables(null, null, table, null);
            return resultSet.next();
        } catch (java.lang.NullPointerException e) {
            logger.error("MySQL connection is null.");
        } catch (com.mysql.jdbc.exceptions.jdbc4.CommunicationsException e) {
            logger.error("Connection refused, there is a problem with connectivity to MySQL.");
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return false;
    }

    public boolean existColumn(String table, String column) {
        Connection conn = getConnection();
        try {
            DatabaseMetaData metadata = conn.getMetaData();
            ResultSet resultSet;
            resultSet = metadata.getColumns(null, null, table, column);
            return resultSet.next();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return false;
    }

    public static String getDataBaseType() {
        String url = Configuration.getProperty("hibernate.connection.url");
        if (url.contains("mysql")) {
            return MYSQLDB;
        } else if (url.contains("sqlserver")) {
            return MSSQLDB;
        }
        return "";
    }

    public static void executeSQLFile(String file) {
        try {
            String scriptQuery = toString(file);
            if (!StringUtil.isNullOrEmpty(scriptQuery)) {
                executeSQLScript(scriptQuery);
                logger.info("Finished script " + file);
            }
        } catch (Exception e) {
            logger.error("Error executing file: " + file, e);
        }
    }

    private static String toString(String filename) throws IOException {
        URL resource = DBHelper.class.getClassLoader().getResource(filename);
        if (resource != null) {
            return IOUtils.toString(resource, Charset.forName("UTF-8"));
        } else {
            logger.warn("No script found with name " + filename);
            return "";
        }
    }

    public static void executeSQLScript(String sql) throws Exception {
        List<String> commands = new ArrayList<>();
        try {
            try (BufferedReader br = new BufferedReader(new StringReader(sql))) {
                boolean commentBlock = false;
                StringBuilder command = new StringBuilder();
                int procedureBlock = 0;
                for (String line; (line = br.readLine()) != null; ) {
                    String oldLine = line;
                    line = line.trim();
                    if (getDataBaseType().equals(MSSQLDB) && line.equalsIgnoreCase("BEGIN")) {
                        procedureBlock++;
                    } else if (getDataBaseType().equals(MYSQLDB) && line.contains("DELIMITER $$")) {
                        procedureBlock++;
                        oldLine = oldLine.replace("DELIMITER $$", "");
                    }
                    if (getDataBaseType().equals(MSSQLDB) && line.equalsIgnoreCase("END")) {
                        procedureBlock--;
                    } else if ((getDataBaseType().equals(MYSQLDB) && line.contains("$$ DELIMITER"))) {
                        procedureBlock--;
                        oldLine = oldLine.replace("$$ DELIMITER", "");
                    }
                    if (line.startsWith("/*")) {
                        commentBlock = true;
                    }
                    if (commentBlock) {
                        if (line.endsWith("*/")) {
                            commentBlock = false;
                            continue;
                        }
                    }
                    if (line.startsWith("--") || line.startsWith("#")) {
                        continue;
                    }
                    if ((line.trim().equalsIgnoreCase("GO") && (procedureBlock == 0)) ||
                            (line.contains("DELIMITER $$") && (procedureBlock == 0))) {
                        commands.add(command.toString());
                        command = new StringBuilder();
                        continue;
                    }
                    command.append(oldLine + "\n");
                    if ((line.endsWith(";") && (procedureBlock == 0)) ||
                            (line.contains("$$ DELIMITER") && (procedureBlock == 0))) {
                        commands.add(command.toString());
                        command = new StringBuilder();
                        continue;
                    }
                    //TODO this looks like it duplicates some sql commands for SQL Server as oldLine is already
                    // append to command
                    if (line.trim().toUpperCase().endsWith(" GO") && procedureBlock == 0) {
                        String s = command.toString();
                        commands.add(s.substring(0, s.toUpperCase().lastIndexOf(" GO")));
                        command = new StringBuilder();
                        continue;
                    }
                }
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new Exception(e);
        }

        Connection conn = getConnection();
        try {
            conn.setAutoCommit(false);
            for (String command : commands) {
                Statement statement = null;
                try {
                    statement = conn.createStatement();
                    statement.execute(command);
                    logger.info("Success executing: " + command);
                } catch (SQLException e) {
                    logger.error("Error executing: " + command + " error: " + e.getMessage());
                }
            }
            conn.commit();
        } catch (SQLException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static int getDBVersionTableCurrentVersion() {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        int versionNumber = 0;
        String sql = "SELECT CAST(dbVersion AS INT) FROM Version ORDER BY id DESC LIMIT 1";
        if (databaseType.equals(MYSQLDB)) {
            sql = "SELECT CAST(dbVersion AS UNSIGNED) FROM Version ORDER BY id DESC LIMIT 1";
        }
        String Version = getStringValue(sql);
        if (Version != null) {
            versionNumber = Math.max(versionNumber, Integer.parseInt(Version));
        }
        String sql2 = "SELECT CAST(dbVersion AS INT) FROM version ORDER BY id DESC LIMIT 1";
        if (databaseType.equals(MYSQLDB)) {
            sql2 = "SELECT CAST(dbVersion AS UNSIGNED) FROM version ORDER BY id DESC LIMIT 1";
        }
        String version = getStringValue(sql2);
        if (version != null) {
            versionNumber = Math.max(versionNumber, Integer.parseInt(version));
        }
        return versionNumber;
    }

    public int getMaxDBVersionTable() {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        int versionNumber = 0;
        String sql = "SELECT max(CAST(dbVersion AS INT)) FROM Version";
        if ("mysql".equals(databaseType)) {
            sql = "SELECT max(CAST(dbVersion AS UNSIGNED)) FROM Version";
        }
        String Version = getStringValue(sql);
        if (Version != null) {
            versionNumber = Math.max(versionNumber, Integer.parseInt(Version));
        }
        String sql2 = "SELECT max(CAST(dbVersion AS INT)) FROM version";
        if ("mysql".equals(databaseType)) {
            sql2 = "SELECT max(CAST(dbVersion AS UNSIGNED)) FROM version";
        }
        String version = getStringValue(sql2);
        if (version != null) {
            versionNumber = Math.max(versionNumber, Integer.parseInt(version));
        }
        return versionNumber;
    }

    public static String getStringValue(String sql) {
        Connection conn = getConnection();
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            statement = conn.createStatement();
            resultSet = statement.executeQuery(sql);
            if (resultSet.next()) {
                try {
                    return resultSet.getString(1);
                } catch (Exception ex) {
                    logger.error(ex.getMessage());
                }
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
        }
        return null;
    }

    public static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int columns = rsmd.getColumnCount();
        for (int x = 1; x <= columns; x++) {
            if (columnName.equals(rsmd.getColumnName(x))) {
                return true;
            }
        }
        return false;
    }

}
