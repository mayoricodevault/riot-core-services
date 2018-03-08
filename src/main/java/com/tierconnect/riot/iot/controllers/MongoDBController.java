package com.tierconnect.riot.iot.controllers;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.dao.mongo.ThingTypeMongoDAO;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.fixdb.FixDBMigrationCassandra;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.dao.MongoExecutionException;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.tierconnect.riot.sdk.utils.TimerUtil;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Path("/mongodb")
@Api("/mongodb")
public class MongoDBController{
    private static Logger logger = Logger.getLogger(MongoDBController.class);

    @POST
    @Path("/drop")
    // @Produces(MediaType.)
    // @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 1,
                  value = "Drops the MongoDB things, thingSnapshots and thingSnapshotIds collections")
    public Response nuke(){
        try{
            ThingMongoDAO dao = ThingMongoDAO.getInstance();

            dao.dropThingsCollection();
            dao.dropThingSnapshotIdsCollection();
            dao.dropThingSnapshotsCollection();

            return RestUtils.sendOkResponse("nuked all things");
        }
        catch(Exception e){
            logger.warn("error", e);
            return RestUtils.sendBadResponse(e.toString());
        }
    }

    @POST
    @Path("/buildThingSnapshotsStructure")
    @RequiresAuthentication
    @ApiOperation(position = 1,
                  value = "Rebuilds the MongoDB things, thingSnapshots and thingSnapshotIds collection from Mysql and Cassandra timeSeries "
                          + "(It must be run once after migrating from 2.x.x to 3.1.2 and immediately services has been started for the first time)")
    public Response buildThingSnapshotsStructure(
            @ApiParam(value = "Valid values are (not case sensitive): Sharaf, Aramco, Dupont, FMC, NetApp"
                    + " (empty for just build things and snapshots)",
                    required = false)
            @QueryParam("customer")
            final String customer,
            @ApiParam(value = "Sets the connection timeout in ms for mongo")
            @DefaultValue("10000")
            @QueryParam("connTimeout")
            Integer connTimeout,
            @ApiParam(value = "Sets the connection per hosts for mongo")
            @DefaultValue("20")
            @QueryParam("concurrentThreads")
            Integer concurrentThreads,
            @ApiParam(value = "Migrates just things in list (if null migrates all things) (thing ids separated by " +
                    "comma with no spaces)")
            @QueryParam("thingsFilter")
            String thingsFilter,
            @ApiParam(value = "Migration begin date")
            @DefaultValue("01/01/2009")
            @QueryParam("beginDate")
            Date beginDate,
            @ApiParam(value = "Migration end date")
            @DefaultValue("12/31/2016")
            @QueryParam("endDate")
            Date endDate,
            @ApiParam(value = "Number of retries in case of cassandra failure.")
            @DefaultValue("5")
            @QueryParam("retries")
            Integer retries,
            @ApiParam(value = "Number of things in a lot. (Only approach 3)")
            @DefaultValue("1000")
            @QueryParam("numberLotThings")
            Integer numberLotThings,
            @ApiParam(value = "Approach to run:<br>"
                    +
                    "1 )     cache approach for Migrate using field_value_history cache for migrateApproaches sharaf " +
                    "(from 2.x.x to 3.3.3)<br>"
                    +
                    "2 )     cache approach for Migrate Sharaf (from 2.4.x to 3.3.3)<br>"
                    +
                    "3 )     cache approach for massive migration of Sharaf between a dates (from 2.4.x to 4.1.3)<br>"
                    +
                    "empty ) to migrateApproaches normally (using field_value_history2)")
            @QueryParam("approach")
            Integer approach,
            @ApiParam(value = "A Switch to false or true to create snapshots or not (It only available to case 3).")
            @DefaultValue("false")
            @QueryParam("flagCreateSnapshots")
            Boolean flagCreateSnapshots){


        Object response;

        approach = approach == null?- 1:approach;

        switch(approach){
            case 1:
                response = approach1(customer, connTimeout, concurrentThreads, thingsFilter, retries);
                break;
            case 2:
                response = approach2(customer, connTimeout, concurrentThreads, thingsFilter, retries);
                break;
            case 3:
                response = MongoDBService.getInstance().bulkMigration(customer, connTimeout, concurrentThreads,
                        thingsFilter, beginDate, endDate, numberLotThings, retries, flagCreateSnapshots);
                break;
            default:
                response = MongoDBService.getInstance().defaultMigration(customer, connTimeout, concurrentThreads,
                        thingsFilter, retries);
                break;
        }

        return RestUtils.sendOkResponse(response);
    }

    private Map approach1(String customer,
                          Integer connTimeout,
                          Integer concurrentThreads,
                          String thingsFilter,
                          Integer retries) {
        try {
            FixDBMigrationCassandra.loadCassandra(retries);
        } catch (Exception e) {
            logger.error("Error loading cassandra ", e);
        }
        return MongoDBService.getInstance().migrateApproaches(customer, connTimeout, concurrentThreads, thingsFilter);
    }

    private Map approach2(String customer,
                          Integer connTimeout,
                          Integer concurrentThreads,
                          String thingsFilter,
                          Integer retries) {
        try {
            FixDBMigrationCassandra.loadCassandra24x(retries);
        } catch (Exception e) {
            logger.error("Error loading cassandra ", e);
        }
        return MongoDBService.getInstance().migrateApproaches(customer, connTimeout, concurrentThreads, thingsFilter);
    }


    @POST
    @Path("/buildThings")
    @RequiresAuthentication
    @ApiOperation(position = 1,
                  value = "Rebuilds the MongoDB things collection just from Mysql (without cassandra)")
    public Response buildThings(
            @ApiParam(value = "Set to true for rebuild all things (takes back up of things collection)")
            @DefaultValue("false")
            @QueryParam("rebuildAllThings")
            Boolean rebuildAllThings){

        long countInsert = 0, countExistent = 0;
        TimerUtil tu = new TimerUtil();
        tu.mark();

        //        Map<String,Map<String,Object>> cache = ThingService.getInstance().getCacheData();

        List<Thing> things = ThingService.getInstance().selectAllThings();

        long totalThings = things.size();
        logger.info("Total things in database :" + totalThings);
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        Date today = Calendar.getInstance().getTime();
        String dateString = df.format(today);
        if (totalThings > 0) {

            if (rebuildAllThings) {
                ThingMongoDAO.getInstance().backupThingsCollecion("things_BKP_" + dateString);
                ThingMongoDAO.getInstance().dropThingsCollection();
            }

            for(Thing thing : things){

                try{
                    BasicDBObject doc = (BasicDBObject)ThingMongoDAO.getInstance().getThing(thing.getId());

                    if (doc == null) {
                        ThingMongoService.getInstance().createThing(thing, new HashMap<String, Object>(), null);

                        //Associate parent
                        Thing thingParent = thing.getParent();
                        if (thingParent != null) {
                            ThingMongoService.getInstance().associateChild(thingParent, thing.getId());
                        }
                        //Associate children
                        List<Thing> childrenList = ThingService.getInstance().getChildrenList(thing);
                        if (!childrenList.isEmpty()) {
                            ThingMongoService.getInstance().associateChildren(thing, childrenList);
                        }

                        countInsert++;
                    }
                    else {
                        countExistent++;
                    }

                }
                catch(Exception e){
                    logger.error("*** Migration of thing with serial "
                                 + thing.getSerial()
                                 + " and id "
                                 + thing.getId()
                                 + " failed, details:"
                                 + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        tu.mark();
        int rate = (int)(countInsert / (tu.getLastDelt() / 1000.0));
        //        int ins = countExistent>countInsert?countExistent-countInsert:countExistent<countInsert?countInsert-countExistent
        String msg = "Inserted "
                     + countInsert
                     + " things, already in mongo "
                     + countExistent
                     + ", total in mongo "
                     + totalThings
                     + ", elapsed time in "
                     + tu.getLastDelt()
                     + " ms ("
                     + rate
                     + " things/sec)";
        if (rebuildAllThings) {
            msg += " ( backup collection things_BKP_" + dateString + ")";
        }

        logger.info(msg);
        return RestUtils.sendOkResponse(msg);

    }

    @POST
    @Path("/createIndexThingSnapshots")
    // @Produces(MediaType.)
    // @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 1,
                  value = "Creates indexes in MongoDB thingSnapshots collection")
    public Response createIndexThingSnapshots(){
        ThingMongoDAO.getInstance().createThingSnapshotsIndexes();
        return RestUtils.sendOkResponse("Indexes created successfully");
    }

    @POST
    @Path("/loadThingTypes")
    // @Produces(MediaType.)
    // @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 1,
                  value = "Load ThingTypes to mongo")
    public Response loadThingTypes(){

        TimerUtil tu = new TimerUtil();

        tu.mark();

        ThingTypeService ts = ThingTypeServiceBase.getInstance();
        List<ThingType> thingTypes = ts.getAllThingTypes();

        ThingTypeMongoDAO ttm = ThingTypeMongoDAO.getInstance();
        int count = ttm.insertAllThingTypes(thingTypes);

        tu.mark();

        int rate = (int)(count / (tu.getLastDelt() / 1000.0));
        String msg = "Inserted " + count + " thingTypes in " + tu.getLastDelt() + " ms (" + rate + " thingTypes/sec)";
        logger.info(msg);
        return RestUtils.sendOkResponse(msg);
    }

    private class RunnableDefinitionDataType implements Runnable{

        public Map<String, Map<String, Object>> cache;

        public RunnableDefinitionDataType(Map<String, Map<String, Object>> cache){
            this.cache = cache;
        }

        @Override
        public void run(){
            Session session = MongoDBService.getHibernateSession();
            Transaction transaction = session.getTransaction();
            transaction.begin();

            migrateDataType(cache);

            transaction.commit();
        }
    }

    private class RunnableMigrationDataType implements Runnable{

        private Thing thing;
        private Map cache;

        public RunnableMigrationDataType(Thing thing, Map cache){
            this.thing = thing;
            this.cache = cache;
        }

        @Override
        public void run(){
            try{
                DBObject doc = ThingMongoDAO.getInstance().getThing(thing.getId());
                List<DBObject> docSnapshots = ThingMongoDAO.getInstance().getAllThingSnapshotByThingId(thing.getId());

                BasicDBObject query = new BasicDBObject("_id", thing.getId());

                //Things
                BasicDBObject update = update((BasicDBObject)doc, "THING");
                if (! update.isEmpty()) {
                    MongoDAOUtil.getInstance().things.update(query,
                                                                       new BasicDBObject("$set", update),
                                                                       false,
                                                                       false,
                                                                       WriteConcern.UNACKNOWLEDGED);
                }

                //Snapshots
                for(DBObject docSnapshot : docSnapshots){
                    query = new BasicDBObject("_id", docSnapshot.get("_id"));
                    update = update((BasicDBObject)doc, "SNAPSHOT");
                    if (! update.isEmpty()) {
                        MongoDAOUtil.getInstance().thingSnapshots.update(query,
                                                                                   new BasicDBObject("$set", update),
                                                                                   false,
                                                                                   false,
                                                                                   WriteConcern.UNACKNOWLEDGED);
                    }
                }

//                //Sparse
//                List<DBObject> docSparseList = ThingMongoDAO.getInstance().getAllThingSparseByThingId(thing.getId());
//                for(DBObject docSparse : docSparseList){
//                    query = new BasicDBObject("_id",
//                                              new BasicDBObject("id", thing.getId()).append("segment",
//                                                                                            ((Map)docSparse.get("_id")).get(
//                                                                                                    "segment")));
//                    update = new BasicDBObject();
//                    int i = 0;
//                    for(Object value : (BasicDBList)docSparse.get("value")){
//                        if (value instanceof Map) {
//                            Map castedValue = update((BasicDBObject)value, "SPARSE|" + i);
//                            update.putAll(castedValue);
//                        }
//                        i++;
//                    }
//                    if (! update.isEmpty()) {
//                        MongoDAOUtil.getInstance().timeseriesCollection.update(query,
//                                                                               new BasicDBObject("$set", update),
//                                                                               false,
//                                                                               false,
//                                                                               WriteConcern.ACKNOWLEDGED);
//                    }
//                }

            }
            catch(Exception e){
                logger.error("*** Migration of thing with serial "
                             + thing.getSerial()
                             + " and id "
                             + thing.getId()
                             + " failed, details:"
                             + e.getMessage());
                e.printStackTrace();
            }
        }

        private BasicDBObject update(BasicDBObject doc, String collectionType){

            Integer index = 0;
            String type = "";

            if (collectionType.contains("|")) {
                String[] typeArray = collectionType.split("\\|");
                index = Integer.parseInt(typeArray[1]);
                type = typeArray[0];
            }
            else {
                type = collectionType;
            }


            BasicDBObject update = new BasicDBObject();
            if (doc != null) {
                for(Map.Entry<String, BasicDBObject> field : (Set<Map.Entry>)doc.toMap().entrySet()){
                    if (field.getValue() instanceof Map &&
                        ! (field.getValue().get("value") instanceof Map) &&
                        ! field.getKey().equalsIgnoreCase("children") &&
                        ! field.getKey().equalsIgnoreCase("parent")) {

                        ThingTypeField thingTypeField
                                = (ThingTypeField)((Map)cache.get("thingTypeField")).get(field.getValue()
                                                                                              .get("thingTypeFieldId")
                                                                                              .toString());

                        Object castedValue = ThingService.getInstance()
                                                         .getStandardDataType(thingTypeField.getDataType(),
                                                                              field.getValue().get("value"));

                        if (field.getValue().get("value") != null && castedValue != null &&
                            ! field.getValue().get("value").getClass().equals(castedValue.getClass())) {
                            switch(type){
                                case "THING":
                                    update.append(thingTypeField.getName() + ".value", castedValue);
                                    break;
                                case "SNAPSHOT":
                                    update.append("value." + thingTypeField.getName() + ".value", castedValue);
                                    break;
                                case "SPARSE":
                                    update.append("value." + index + "." + thingTypeField.getName() + ".value",
                                                  castedValue);
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }

            return update;
        }

    }

    public void migrateDataType(Map<String, Map<String, Object>> cache){

        try{
            //Update UDFs
            for(ThingTypeField thingTypeField : ThingTypeFieldService.getThingTypeFieldDAO().selectAll()){

                //Change zone from string to Zone type
                if (thingTypeField.getName().equals("lastDetectTime") && thingTypeField.getName().equals(
                        "lastLocateTime")) {
                    thingTypeField.setDataType(DataTypeService.getInstance()
                                                              .get(ThingTypeField.Type.TYPE_TIMESTAMP.value));
                    cache.get("thingTypeField").put(thingTypeField.getId().toString(), thingTypeField);
                    ThingTypeFieldService.getThingTypeFieldDAO().update(thingTypeField);
                }
            }

        }
        catch(Exception e){
            logger.warn("error", e);
        }
        String msg = "Migrated Data types";
        logger.info(msg);
    }

    @POST
    @Path("/migrateDataType")
    @RequiresAuthentication
    @ApiOperation(position = 1,
                  value = "Rebuilds the MongoDB things, thingSnapshots and thingSnapshotIds collection from Mysql and Cassandra timeSeries "
                          + "(It must be run once after migrating from 2.x.x to 3.1.2 and immediately services has been started for the first time)")
    public Response migrateDataType(
            @DefaultValue("10000")
            @QueryParam("connTimeout")
            Integer connTimeout,
            @ApiParam(value = "Sets the conn230000ection per hosts for mongo")
            @DefaultValue("800")
            @QueryParam("concurrentThreads")
            Integer concurrentThreads){


        try{
            MongoDAOUtil.setupMongodb(Configuration.getProperty("mongo.primary"),
                    Configuration.getProperty("mongo.secondary"),
                    Configuration.getProperty("mongo.replicaset"),
                    Boolean.valueOf(Configuration.getProperty("mongo.ssl")),
                    Configuration.getProperty("mongo.username"),
                    Configuration.getProperty("mongo.password"),
                    Configuration.getProperty("mongo.authdb"),
                    Configuration.getProperty("mongo.db"),
                    Configuration.getProperty("mongo.controlReadPreference"),
                    Configuration.getProperty("mongo.reportsReadPreference"),
                    Boolean.valueOf(Configuration.getProperty("mongo.sharding")),
                    connTimeout,
                    concurrentThreads + 50);
        }
        catch(UnknownHostException e){
            e.printStackTrace();
        }

        //        ThreadPool pool = new ThreadPool(connPerHost);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);

        Map<String, Map<String, Object>> cache = ThingService.getInstance().getCacheData();

        //New thread to write immediately to database and avoid session (hibernare, shiro, riot) timeout because migration takes a lot of time
        RunnableDefinitionDataType runnableDefinition = new RunnableDefinitionDataType(cache);

        Thread thread = new Thread(runnableDefinition);

        thread.start();

        while(thread.isAlive()){
            logger.info("Waiting for changes committed on db");
            try{
                Thread.sleep(3000);
            }
            catch(InterruptedException e){
                e.printStackTrace();
            }
        }


        logger.info("**** Starting Data Types Migration Process **** ");

        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        Date today = Calendar.getInstance().getTime();
        String dateString = df.format(today);

        //        long count = 0;
        TimerUtil tu = new TimerUtil();
        tu.mark();

        int lastPercentLogged = 0, percent = 0;

        //Reuild things thingSnapshotIds and thingSnapshots
        cache = runnableDefinition.cache;

        int counter = 0, totalThings = ((Collection<Thing>)((Map)cache.get("thingById")).values()).size();


//        MongoDAOUtil.getInstance().timeseriesCollection.setWriteConcern(WriteConcern.UNACKNOWLEDGED);

        logger.info("Total things in database :" + ((Collection<Thing>)((Map)cache.get("thingById")).values()).size());
        for(Thing thing : (Collection<Thing>)((Map)cache.get("thingById")).values()){


            RunnableMigrationDataType runnable = new RunnableMigrationDataType(thing, cache);
            executor.execute(runnable);

            counter++;
            percent = (counter * 100 / totalThings);
            if (percent % 5 == 0 && percent != lastPercentLogged) {
                logger.info("Migration progress: " + percent + "% (" + counter + " things of " + totalThings + ")");
                lastPercentLogged = percent;
            }

        }

        executor.shutdown();
        while(! executor.isTerminated()){
            logger.info("Waiting for threads finish migration ");
            try{
                Thread.currentThread().sleep(20000);
            }
            catch(InterruptedException e){
                e.printStackTrace();
            }
        }

        tu.mark();

        int rate = (int)(counter / (tu.getLastDelt() / 1000.0));
        String msg = "Migrated "
                     + counter
                     + " things' thingSnapshots and thingSnapshotIds collections id in "
                     + tu.getLastDelt()
                     + " ms ("
                     + rate
                     + " things/sec)";
        logger.info(msg);

        CassandraUtils.shutdown();

        return RestUtils.sendOkResponse(msg);
    }

    @POST
    @Path("/migrateZoneProperties")
    @RequiresAuthentication
    @ApiOperation(position = 1, value = "")
    public Response migrateZoneProperties(
            @DefaultValue("10000") @QueryParam("connTimeout") Integer connTimeout,
            @DefaultValue("zone") @QueryParam("zoneUdfName") String zoneUdfName,
            @DefaultValue("default_rfid_thingtype") @QueryParam("thingTypeCode") String thingTypeCode) {
        try {
            return RestUtils.sendOkResponse(MongoDBService.getInstance().migrateZoneType(connTimeout, zoneUdfName,
                    thingTypeCode));
        } catch (UnknownHostException e) {
            return RestUtils.sendBadResponse(e.getMessage());
        }
    }


    @POST
    @Path("/reassociateThingType")
    @RequiresAuthentication
    @ApiOperation(position = 2, value = "")
    public Response reasociateThingType(
            @DefaultValue("default_rfid_thingtype") @QueryParam("thingTypeCode") String thingTypeCode) {
        try {
            return RestUtils.sendOkResponse(MongoDBService.getInstance().reassociateThingType(thingTypeCode));
        } catch (MongoExecutionException | NonUniqueResultException e) {
            return RestUtils.sendBadResponse(e.getMessage());
        }
    }

}
