package com.tierconnect.riot.iot.fixdb;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;

/**
 * Created by fflores on 7/14/2015.
 */
@Deprecated
public class FixDBMigrationCassandra {

    static Logger logger = Logger.getLogger( FixDBMigrationCassandra.class );

//    static List<String> dumpFT = null;
//    static List<String> dumpFVH2 = null;

    public static void loadCassandra(Integer retires) throws Exception
    {
//        dumpFT = new ArrayList<>();
//        dumpFVH2 = new ArrayList<>();

        CassandraCache.clearCache();

        CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));

        logger.info("Starting loadCassandra... (it may take several minutes)");
        System.out.println("Starting loadCassandra... (it may take several minutes)");

        logger.info("Reading thingfield table with temporary connection");

        Connection conn = initMysqlJDBCDrivers();

        java.sql.ResultSet rs = null;
        if(conn != null && Configuration.getProperty("hibernate.connection.url").toLowerCase().contains("mysql")){
            rs = conn.createStatement().executeQuery("SELECT id, thing_id, thingTypeFieldId FROM thingfield");
        }else if (conn != null && Configuration.getProperty("hibernate.connection.url").toLowerCase().contains("sqlserver")){
            rs = conn.createStatement().executeQuery("SELECT id, thing_id, thingTypeFieldId FROM dbo.thingfield");
        }

        if(rs != null){
//            Map<Long, Map<String, Long>> thingFieldMap = new HashMap<>();
            Map<Long, Map<Long, Long>> thingFieldMap2 = new HashMap<>();
            logger.info("Building cache maps from thingfield table values");
            while(rs.next()){

                Long thingId = rs.getLong("thing_id");
                Long thingFieldId = rs.getLong("id");
                Long thingTypeFieldId = rs.getLong("thingTypeFieldId");

                Map<Long, Long> thingTemp = null;
                if (thingFieldMap2.containsKey(thingId)) {
                    thingTemp = thingFieldMap2.get(thingId);
                } else {
                    thingTemp = new HashMap<>();
                    thingFieldMap2.put(thingId, thingTemp);
                }
                thingTemp.put(thingFieldId, thingTypeFieldId);
            }

            conn.close();

            logger.info("Retrieving things.");
            List<Thing> things = ThingService.getThingDAO().selectAll();
            int counter = 0, percent = 0, lastPercentLogged = 0, totalThings = things.size();

            List<Long> failedThings = new ArrayList<>();

            logger.info("Migration progress (loading cassandra into cache): 0% (0 things of " + totalThings + ")");
            for (Thing thing : things) {
                boolean error, succeed;
                int retriesfv = 0;
                int retriesfvh = 0;
                if (thingFieldMap2.containsKey(thing.getId())) {

                    Map<Long, Long> thingTemp = thingFieldMap2.get(thing.getId());

                    List<Long> ids = new ArrayList<>();
                    for (Map.Entry<Long, Long> item : thingTemp.entrySet()) {
                        if (!ids.contains(item.getKey())) {
                            ids.add(item.getKey());
                        }
                    }

                    error = true; succeed = false;
                    while(error && !succeed && retriesfv <= retires){
                        try{
                            error = false;
                            processFieldValue(thing, ids, thingTemp);
                            succeed = true;
                        }catch(RuntimeException e){
                            error = true;
                            succeed = false;
                            if(retriesfv == retires){
                                if(!failedThings.contains(thing.getId())) failedThings.add(thing.getId());
                                logger.error("*@*@*@*@*@* Error in processing field_value after 5 retries for thing : " + thing.getSerial(), e);
                            }
                        }finally {
                            retriesfv++;
                        }
                    }
                    error = true; succeed = false;
                    while(error && !succeed && retriesfvh <= retires){
                        try{
                            error = false;
                            processFieldValueHistory(thing, ids, thingTemp);
                            succeed = true;
                        }catch(RuntimeException e){
                            error = true;
                            succeed = false;
                            if(retriesfvh == retires){
                                if(!failedThings.contains(thing.getId())) failedThings.add(thing.getId());
                                logger.error("*@*@*@*@*@* Error in processing field_value_history after 5 retries for thing : " + thing.getSerial(), e);
                            }
                        }finally {
                            retriesfvh++;
                        }
                    }

                }
                counter++;

                percent = (counter * 100 / totalThings);

                logger.info("Loaded thing : " + thing.getSerial() + " (" + counter + " things of " + totalThings + ")" +
                        "(field_value retries " + retriesfv + "(field_value_history retries " + retriesfv);
                if (percent != lastPercentLogged) {
                    logger.info("Migration progress (loading cassandra into cache): " + percent + "% (" + counter + " things of " + totalThings + ")");
                    lastPercentLogged = percent;
                }

            }

//            // get fieldValuesHistory from Cassandra
//            ResultSet result = CassandraUtils.getSession().execute("SELECT * FROM field_value_history");
//            orphan = new ArrayList<>();
//            for(Row row :  result.all()){
//                Long fieldId = row.getLong("field_id");
//                Date at = row.getDate("at");
//                String value = row.getString("value");
//                if(thingFieldMap.containsKey(fieldId)) {
//                    Map<String, Long> thingFieldValues = (Map<String, Long>) thingFieldMap.get(fieldId);
//
//                    // insert in fieldValueHistory2
//                    if (null != thingFieldValues)
//                        FieldTypeHistoryDAO.insertHistory(thingFieldValues.get("thingTypeFieldId"), thingFieldValues.get("thingId"), at, value);
//                }else
//                    orphan.add(fieldId);
//            }

//            logger.info("orphans field_value_history= " + orphan.size());
//            logger.info("orphans field_value_history= " + orphan.toString());
            logger.info("Loading cassandra into cache has been finished. ");
            logger.info("Failed things " + failedThings);

            CassandraUtils.shutdown();

            CassandraCache.getInstance().setCached(true);

        }else{
            logger.error("No connection available for " + Configuration.getProperty("hibernate.connection.url"));
        }
    }

    public static void loadCassandra24x(Integer retires) throws Exception {

        CassandraCache.clearCache();

        CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));

        logger.info("Starting loadCassandra... (it may take several minutes)");
        System.out.println("Starting loadCassandra... (it may take several minutes)");

//            Map<Long, Map<String, Long>> thingFieldMap = new HashMap<>();

        logger.info("Retrieving things.");
        List<Thing> things = ThingService.getThingDAO().selectAll();
        int counter = 0, percent = 0, lastPercentLogged = 0, totalThings = things.size();

        List<Long> failedThings = new ArrayList<>();
        List<Long> ids = ThingTypeFieldService.getIds(ThingTypeFieldService.getThingTypeFieldDAO().selectAll());

        logger.info("Migration progress (loading cassandra into cache): 0% (0 things of " + totalThings + ")");
        for (Thing thing : things) {
            boolean error, succeed;
            int retriesfv = 0;
            int retriesfvh = 0;

            error = true;
            succeed = false;
            while (error && !succeed && retriesfv <= retires) {
                try {
                    error = false;
                    processFieldType(thing, ids);
                    succeed = true;
                } catch (RuntimeException e) {
                    error = true;
                    succeed = false;
                    if (retriesfv == retires) {
                        if (!failedThings.contains(thing.getId())) failedThings.add(thing.getId());
                        logger.error("*@*@*@*@*@* Error in processing field_type after 5 retries for thing : " + thing.getSerial(), e);
                    }
                } finally {
                    retriesfv++;
                }
            }
            error = true;
            succeed = false;
            while (error && !succeed && retriesfvh <= retires) {
                try {
                    error = false;
                    processFieldValueHistory2(thing, ids);
                    succeed = true;
                } catch (RuntimeException e) {
                    error = true;
                    succeed = false;
                    if (retriesfvh == retires) {
                        if (!failedThings.contains(thing.getId())) failedThings.add(thing.getId());
                        logger.error("*@*@*@*@*@* Error in processing field_value_history2 after 5 retries for thing : " + thing.getSerial(), e);
                    }
                } finally {
                    retriesfvh++;
                }
            }


            counter++;

            percent = (counter * 100 / totalThings);

            logger.info("Loaded thing : " + thing.getSerial() + " (" + counter + " things of " + totalThings + ")" +
                    "(field_type retries " + retriesfv + "(field_value_history2 retries " + retriesfv);
            if (percent != lastPercentLogged) {
                logger.info("Migration progress (loading cassandra into cache): " + percent + "% (" + counter + " things of " + totalThings + ")");
                lastPercentLogged = percent;
            }

        }

//            // get fieldValuesHistory from Cassandra
//            ResultSet result = CassandraUtils.getSession().execute("SELECT * FROM field_value_history");
//            orphan = new ArrayList<>();
//            for(Row row :  result.all()){
//                Long fieldId = row.getLong("field_id");
//                Date at = row.getDate("at");
//                String value = row.getString("value");
//                if(thingFieldMap.containsKey(fieldId)) {
//                    Map<String, Long> thingFieldValues = (Map<String, Long>) thingFieldMap.get(fieldId);
//
//                    // insert in fieldValueHistory2
//                    if (null != thingFieldValues)
//                        FieldTypeHistoryDAO.insertHistory(thingFieldValues.get("thingTypeFieldId"), thingFieldValues.get("thingId"), at, value);
//                }else
//                    orphan.add(fieldId);
//            }

//            logger.info("orphans field_value_history= " + orphan.size());
//            logger.info("orphans field_value_history= " + orphan.toString());
        logger.info("Loading cassandra into cache has been finished. ");
        logger.info("Failed things " + failedThings);

        CassandraUtils.shutdown();

        CassandraCache.getInstance().setCached(true);

    }

    private static void processFieldValue(Thing thing, List<Long> ids, Map<Long, Long> thingTemp) throws RuntimeException{
        if(ids.size() <= 65535){
            String inClause = StringUtils.join(ids, ",");
            String query = "SELECT * FROM field_value WHERE field_id in (" + inClause + ");";
            ResultSet result = CassandraUtils.getSession().execute(query);

            for (Row row : result.all()) {
                Long fieldId = row.getLong("field_id");
                Date time = row.getDate("time");
                String value = row.getString("value");
                //FieldTypeDAO.insert(thing.getId(), thingTemp.get(fieldId), time, value);
                CassandraCache.getInstance().addValueToCache(thing.getId(), thingTemp.get(fieldId), time, value);
                //dumpFT.add("(" + thing.getId() + "," +  thingTemp.get(fieldId)  +"," + time.getTime() + ",'" + value + "')");
            }
        }else{
            int size = 0;
            while (size <= ids.size()){
                int to = size + 65535 >= ids.size() ? ids.size() - 1 : size + 65535;
                String inClause = StringUtils.join(ids.subList(size,size + 65534), ",");
                String query = "SELECT * FROM field_value WHERE field_id in (" + inClause + ");";
                ResultSet result = CassandraUtils.getSession().execute(query);

                for (Row row : result.all()) {
                    Long fieldId = row.getLong("field_id");
                    Date time = row.getDate("time");
                    String value = row.getString("value");
                    //FieldTypeDAO.insert(thing.getId(), thingTemp.get(fieldId), time, value);
                    CassandraCache.getInstance().addValueToCache(thing.getId(), thingTemp.get(fieldId), time, value);
                    //dumpFT.add("(" + thing.getId() + "," +  thingTemp.get(fieldId)  +"," + time.getTime() + ",'" + value + "')");
                }

                size = to+1;
            }
        }
    }

    private static void processFieldType(Thing thing, List<Long> ids) throws RuntimeException{
        if(ids.size() <= 65535){
            String inClause = StringUtils.join(ids, ",");
            String query = "SELECT * FROM field_type WHERE field_type_id in (" + inClause + ");";
            ResultSet result = CassandraUtils.getSession().execute(query);

            for (Row row : result.all()) {
                Long fieldId = row.getLong("field_type_id");
                Date time = row.getDate("time");
                String value = row.getString("value");
                //FieldTypeDAO.insert(thing.getId(), thingTemp.get(fieldId), time, value);
                CassandraCache.getInstance().addValueToCache(thing.getId(), fieldId, time, value);
                //dumpFT.add("(" + thing.getId() + "," +  thingTemp.get(fieldId)  +"," + time.getTime() + ",'" + value + "')");
            }
        }else{
            int size = 0;
            while (size <= ids.size()){
                int to = size + 65535 >= ids.size() ? ids.size() - 1 : size + 65535;
                String inClause = StringUtils.join(ids.subList(size,size + 65534), ",");
                String query = "SELECT * FROM field_type WHERE field_type_id in (" + inClause + ");";
                ResultSet result = CassandraUtils.getSession().execute(query);

                for (Row row : result.all()) {
                    Long fieldId = row.getLong("field_type_id");
                    Date time = row.getDate("time");
                    String value = row.getString("value");
                    //FieldTypeDAO.insert(thing.getId(), thingTemp.get(fieldId), time, value);
                    CassandraCache.getInstance().addValueToCache(thing.getId(), fieldId, time, value);
                    //dumpFT.add("(" + thing.getId() + "," +  thingTemp.get(fieldId)  +"," + time.getTime() + ",'" + value + "')");
                }

                size = to+1;
            }
        }
    }

    private static void processFieldValueHistory(Thing thing, List<Long> ids, Map<Long, Long> thingTemp) throws RuntimeException{
        if(ids.size() <= 65535){
            String inClause = StringUtils.join(ids, ",");
            String query = "SELECT * FROM field_value_history WHERE field_id in (" + inClause + ")";
            ResultSet result = CassandraUtils.getSession().execute(query);
            for (Row row : result.all()) {
                Long fieldId = row.getLong("field_id");
                Date at = row.getDate("at");
                String value = row.getString("value");
                //FieldTypeHistoryDAO.insertHistory(thingTemp.get(fieldId), thing.getId(), at, value);
                CassandraCache.getInstance().addHistoryToCache(thing.getId(), thingTemp.get(fieldId), at, value);
                //dumpFVH2.add("(" + thingTemp.get(fieldId) + "," +  thing.getId()  +"," + at.getTime() + ",'" + value + "')");
            }
        }else{
            int size = 0;
            while (size <= ids.size()){
                int to = size + 65535 >= ids.size() ? ids.size() - 1 : size + 65535;
                String inClause = StringUtils.join(ids.subList(size,to), ",");
                String query = "SELECT * FROM field_value_history WHERE field_id in (" + inClause + ")";
                ResultSet result = CassandraUtils.getSession().execute(query);

                for (Row row : result.all()) {
                    Long fieldId = row.getLong("field_id");
                    Date at = row.getDate("at");
                    String value = row.getString("value");
                    //FieldTypeHistoryDAO.insertHistory(thingTemp.get(fieldId), thing.getId(), at, value);
                    CassandraCache.getInstance().addHistoryToCache(thing.getId(), thingTemp.get(fieldId), at, value);
                    //dumpFVH2.add("(" + thingTemp.get(fieldId) + "," +  thing.getId()  +"," + at.getTime() + ",'" + value + "')");
                }
                size = to+1;
            }
        }
    }

    private static void processFieldValueHistory2(Thing thing, List<Long> ids) throws RuntimeException{
        if(ids.size() <= 65535){
            String inClause = StringUtils.join(ids, ",");
            String query = "SELECT * FROM field_value_history2 WHERE thing_id = " + thing.getId() + " AND field_type_id in (" + inClause + ")";
            ResultSet result = CassandraUtils.getSession().execute(query);
            for (Row row : result.all()) {
                Long fieldId = row.getLong("field_type_id");
                Date time = row.getDate("time");
                String value = row.getString("value");
                //FieldTypeHistoryDAO.insertHistory(thingTemp.get(fieldId), thing.getId(), at, value);
                CassandraCache.getInstance().addHistoryToCache(thing.getId(), fieldId, time, value);
                //dumpFVH2.add("(" + thingTemp.get(fieldId) + "," +  thing.getId()  +"," + at.getTime() + ",'" + value + "')");
            }
        }else{
            int size = 0;
            while (size <= ids.size()){
                int to = size + 65535 >= ids.size() ? ids.size() - 1 : size + 65535;
                String inClause = StringUtils.join(ids.subList(size,to), ",");
                String query = "SELECT * FROM field_value_history2 WHERE thing_id = " + thing.getId() + " AND field_type_id in (" + inClause + ")";
                ResultSet result = CassandraUtils.getSession().execute(query);

                for (Row row : result.all()) {
                    Long fieldId = row.getLong("field_type_id");
                    Date time = row.getDate("time");
                    String value = row.getString("value");
                    //FieldTypeHistoryDAO.insertHistory(thingTemp.get(fieldId), thing.getId(), at, value);
                    CassandraCache.getInstance().addHistoryToCache(thing.getId(), fieldId, time, value);
                    //dumpFVH2.add("(" + thingTemp.get(fieldId) + "," +  thing.getId()  +"," + at.getTime() + ",'" + value + "')");
                }
                size = to+1;
            }
        }
    }


    public static Connection initMysqlJDBCDrivers() {

        String url = Configuration.getProperty("hibernate.connection.url");
        String driverMysql = "org.gjt.mm.mysql.Driver";
        String driverMssql = "net.sourceforge.jtds.jdbc.Driver";
        String userName = Configuration.getProperty("hibernate.connection.username");
        String password = Configuration.getProperty("hibernate.connection.password");

        try {
            Class.forName(driverMysql).newInstance();
            Class.forName(driverMssql).newInstance();
            return DriverManager.getConnection(url, userName, password);

        } catch (Exception ex) {
            System.out.println(Arrays.asList(ex.getStackTrace()));
        }
        return null;
    }

}
