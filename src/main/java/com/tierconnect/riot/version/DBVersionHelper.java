package com.tierconnect.riot.version;

import com.hazelcast.util.StringUtil;
import com.tierconnect.riot.appcore.entities.MigrationStepResult;
import com.tierconnect.riot.appcore.utils.VersionUtils;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by achambi on 1/19/17.
 * Entity to get the last version in dataBase.
 * NOTE: This class exists to get the database version because Hibernate has not started up yet.
 */
class DBVersionHelper {

    private static Logger logger = Logger.getLogger(DBVersionHelper.class);

    private static final String MYSQL_SEL_VERSION = "SELECT id, CAST(dbVersion AS UNSIGNED) as versionNumber %1s" +
            " FROM %2s" +
            " ORDER BY id DESC LIMIT 1";
    private static final String SQL_SEL_VERSION = "SELECT id, CAST(dbVersion AS INT) as versionNumber %1s" +
            " FROM %2s " +
            " ORDER BY id DESC LIMIT 1";


    private static final String SELECT_MIGRATION_RESULT = "SELECT *" +
            " FROM migration_step_result" +
            " WHERE version_id IN (SELECT id from version where dbVersion = '%1s')";


    private static final String TABLE_NAME = "version";
    private static final String VERSION_NAME_SELECT = ", versionName";

    private int versionNumber;
    private long  versionId;
    private String versionName;

    String getVersionName() {
        if (StringUtil.isNullOrEmpty(versionName))
            return VersionUtils.getAppVersion(versionNumber);
        return versionName;
    }

    int getVersionNumber() {
        return versionNumber;
    }

    public long getVersionId() {
        return versionId;
    }

    private DBVersionHelper(int versionNumber, String versionName, Long versionId) throws SQLException {
        this.versionNumber = versionNumber;
        this.versionName = versionName;
        this.versionId = versionId;
    }

    private static String getVersionQuery(String dataBaseType, boolean flagVersionName) {
        switch (dataBaseType) {
            case DBHelper.MYSQLDB:
                return String.format(MYSQL_SEL_VERSION, ((flagVersionName) ? VERSION_NAME_SELECT : ""), TABLE_NAME);
            case DBHelper.MSSQLDB:
                return String.format(SQL_SEL_VERSION, ((flagVersionName) ? VERSION_NAME_SELECT : ""), TABLE_NAME);
            default:
                throw new IllegalArgumentException(dataBaseType + " not supported");
        }
    }

    static DBVersionHelper getCurrentVersion() {
        DBVersionHelper dbVersionHelper = DBVersionHelper.getVersion(getVersionQuery(DBHelper
                .getDataBaseType(), true));
        if (dbVersionHelper == null) {
            dbVersionHelper = DBVersionHelper.getVersion(getVersionQuery(DBHelper.getDataBaseType(),
                    false));
        }
        return dbVersionHelper;
    }

    private static DBVersionHelper getVersion(String sql) {
        Connection conn = DBHelper.getConnection();
        Statement statement = null;
        ResultSet resultSet = null;
        DBVersionHelper dbVersionHelper = null;
        try {
            statement = conn.createStatement();
            resultSet = statement.executeQuery(sql);
            if (resultSet != null && resultSet.next()) {

                dbVersionHelper = new DBVersionHelper(
                        (DBHelper.hasColumn(resultSet, "versionNumber") ?
                                Integer.parseInt(resultSet.getString("versionNumber")) : 0)
                        , (DBHelper.hasColumn(resultSet, "versionName") ?
                        resultSet.getString("versionName") : "")
                        , Long.parseLong(resultSet.getString("id")));
            }
            return dbVersionHelper;
        } catch (SQLException e) {
            logger.warn(e.getMessage());
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.error(e.getMessage());
            }
        }
        return null;
    }

    public static List<MigrationStepResult> getLastVersionMigrationResult(int versionNumber) {
        Connection conn = DBHelper.getConnection();
        Statement statement = null;
        ResultSet resultSet = null;

        List<MigrationStepResult> migrationStepResults = new ArrayList<>();

        try {
            statement = conn.createStatement();
            resultSet = statement.executeQuery(String.format(SELECT_MIGRATION_RESULT,versionNumber));

            while (resultSet != null && resultSet.next()) {
                MigrationStepResult migrationStepResult = new MigrationStepResult();
                migrationStepResult.setId(resultSet.getLong("id"));
                migrationStepResult.setMigrationResult(resultSet.getString("migrationResult"));
                migrationStepResult.setMigrationPath(resultSet.getString("migrationPath"));
                migrationStepResult.setHash(DBHelper.hasColumn(resultSet, "hash") ? resultSet.getString("hash") : null);
                migrationStepResults.add(migrationStepResult);
            }
        } catch (SQLException e) {
            logger.warn(e.getMessage());
        } finally {
            try {
                if (resultSet != null) {
                    resultSet.close();
                }
                if (statement != null) {
                    statement.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                logger.error(e.getMessage(), e);
            }
        }
        return migrationStepResults;
    }

}
