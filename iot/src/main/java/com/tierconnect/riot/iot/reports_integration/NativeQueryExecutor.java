package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.iot.utils.sql.dialect.resolver.CustomServiceRegistryBuilder;
import com.tierconnect.riot.sdk.dao.UserException;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;
import org.hibernate.exception.SQLGrammarException;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.transform.AliasedTupleSubsetResultTransformer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by julio.rocha on 07-06-17.
 */
public class NativeQueryExecutor {
    private static final String TMP_PATH = "/tmp";
    private Connection connection;
    private Set<String> columns;
    private Integer totalRows = 0;
    private Session session;
    private SessionFactory sessionFactory;
    private boolean hasTotalRows = false;

    public NativeQueryExecutor(Connection connection) {
        this.connection = connection;
        this.columns = new LinkedHashSet<>();
        session = getHibernateSession();
    }

    private Session getHibernateSession() {
        try {
            Properties properties = new Properties();
            properties.setProperty("hibernate.connection.driver_class", connection.getPropertyAsString("driver"));
            properties.setProperty("hibernate.connection.url", connection.getPropertyAsString("url"));
            properties.setProperty("hibernate.connection.username", connection.getPropertyAsString("user"));
            properties.setProperty("hibernate.connection.password", connection.getPassword(false));
            properties.setProperty("hibernate.dialect_resolvers", "com.tierconnect.riot.iot.utils.sql.dialect.resolver.CustomDialectResolver");
            properties.setProperty("hibernate.cache.use_second_level_cache", "false");
            properties.setProperty("hibernate.cache.use_query_cache", "false");
            properties.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "true");
            Configuration cfg = new Configuration()
                    .setProperties(properties);
            //Using CustomServiceRegistryBuilder in order to avoid environment configuration of dialect and force to auto-detection
            ServiceRegistry serviceRegistry = new CustomServiceRegistryBuilder()
                    .applySettings(cfg.getProperties()).build();
            this.sessionFactory = cfg.buildSessionFactory(serviceRegistry);
            return this.sessionFactory.openSession();
        } catch (Exception e) {
            if (connection == null) {
                throw new UserException("Unable to reach connection: " +
                        "'The associated connection was deleted'", e);
            }
            throw new UserException("Unable to reach connection: '" +
                    connection.getName() +
                    "(" + getDriverName(connection.getPropertyAsString("url")) + ")'", e);
        }
    }

    private String getDriverName(String url) {
        Pattern pattern = Pattern.compile("jdbc:\\w*://");
        Matcher matcher = pattern.matcher(url);
        String response = "";
        if (matcher.find()) {
            response = matcher.group(0).replace("jdbc:", "").replace("://", "");
        }
        return response.toUpperCase();
    }

    public Set<String> getColumns() {
        return Collections.unmodifiableSet(columns);
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public List<List<Object>> executeQuery(String query, String comment) {
        return executeQuery(query, comment, true);
    }

    public List<List<Object>> executeQuery(String query, String comment, boolean closeSession) {
        try {
            SQLQuery sqlQuery = session.createSQLQuery(query);
            sqlQuery.setReadOnly(true);
            sqlQuery.setComment(comment);
            sqlQuery.setResultTransformer(new CustomAliasToEntityMapResultTransformer());
            List<List<Object>> result = sqlQuery.list();
            return result;
        } catch (MappingException me) {
            throw new UserException("Invalid script data type for Connection: '" +
                    connection.getName() +
                    "(" + getDriverName(connection.getPropertyAsString("url")) + ")'", me);
        } catch (SQLGrammarException esql) {
            throw new UserException("Invalid script syntax for Connection: '" +
                    connection.getName() +
                    "(" + getDriverName(connection.getPropertyAsString("url")) + ")'", esql);
        } catch (Exception e) {
            throw new UserException("It could not execute query: " + e.getMessage(), e);
        } finally {
            if (closeSession) {
                closeSession();
            }
        }
    }

    public File saveQueryInFile(String query, String comment, String singleQuery) {
        PrintWriter writer = null;
        try {
            executeQuery(singleQuery, "", false);//hack in order to get column names
            SQLQuery sqlQuery = session.createSQLQuery(query);
            sqlQuery.setReadOnly(true);
            sqlQuery.setComment(comment);
            sqlQuery.setResultTransformer(new CustomAliasToEntityMapResultTransformer());
            ScrollableResults scroll = sqlQuery.scroll(ScrollMode.FORWARD_ONLY);
            File exportFile = File.createTempFile("ConnectionReport" + System.nanoTime(), ".csv", new File(TMP_PATH));
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(exportFile), "UTF-8"));
            String labels = columns.toString();
            writer.println(labels.substring(1, labels.length() - 1));
            while (scroll.next()) {
                Object[] objects = scroll.get();
                String row = Arrays.stream(objects).map(c -> (c != null) ? c.toString() : "").collect(Collectors.joining(","));
                writer.println(row.substring(1, row.length() - 1));
            }
            closeSession();
            return exportFile;
            //List<List<Object>> result = sqlQuery.list();
        } catch (Exception e) {
            throw new UserException(e.getMessage(), e);
        } finally {
            closeSession();
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void closeSession() {
        try {
            if (session != null) {
                session.close();
                sessionFactory.close();
            }
        } catch (Exception e) {
        }
    }

    private class CustomAliasToEntityMapResultTransformer extends AliasedTupleSubsetResultTransformer {
        private CustomAliasToEntityMapResultTransformer() {
        }

        @Override
        public Object transformTuple(Object[] tuple, String[] aliases) {
            if (aliases.length > 0 && "total_rows".equals(aliases[aliases.length - 1])) {
                hasTotalRows = true;
            }
            if (totalRows == 0 && hasTotalRows) {
                totalRows = (Integer) tuple[tuple.length - 1];
            }
            if (columns.isEmpty()) {
                String[] subAliases = (hasTotalRows) ? Arrays.copyOf(aliases, tuple.length - 1)
                        : Arrays.copyOf(aliases, tuple.length);
                Arrays.stream(subAliases).filter(a -> a != null).forEach(columns::add);
            }
            Object[] subTuple = (hasTotalRows) ? Arrays.copyOf(tuple, tuple.length - 1)
                    : Arrays.copyOf(tuple, tuple.length);
            List<Object> result = Arrays.asList(subTuple);
            return result;
        }

        @Override
        public boolean isTransformedValueATupleElement(String[] strings, int i) {
            return false;
        }
    }
}
