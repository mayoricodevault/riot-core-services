package com.tierconnect.riot.iot.services;

import com.mongodb.*;
import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.version.CodeVersion;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.commons.entities.IThingField;
import com.tierconnect.riot.commons.entities.IThingField;
import com.tierconnect.riot.commons.utils.DateHelper;
import com.tierconnect.riot.iot.cassandra.dao.FieldTypeDAO;
import com.tierconnect.riot.iot.cassandra.dao.FieldTypeHistoryDAO;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.dao.mongo.MongoMigrationDAO;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.fixdb.CassandraCache;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.MongoExecutionException;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.utils.TimerUtil;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.bson.types.ObjectId;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tierconnect.riot.iot.entities.ThingTypeField.Type.*;
import static com.tierconnect.riot.iot.services.ReportDefinitionServiceBase.getReportDefinitionDAO;
import static com.tierconnect.riot.iot.services.ThingTypeFieldServiceBase.getThingTypeFieldDAO;
import static java.lang.Long.MAX_VALUE;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * Created by cvertiz on 4/29/16.
 * Service to Use in migrating cassandra data to mongo.
 */
@SuppressWarnings("unchecked")
public class MongoDBService {

    private static Logger logger = Logger.getLogger(MongoDBService.class);
    static private MongoDBService INSTANCE = new MongoDBService();

    public static MongoDBService getInstance() {
        return INSTANCE;
    }

    /**
     * Runnable class to migrate a data types.
     */
    private class RunnableDataTypeMigration implements Runnable {

        public Map<String, Map<String, Object>> cache;
        private String customer;

        /**
         * Runnable data type migration class constructor.
         *
         * @param customer A customer to migrate data types.
         * @param cache    A cache data to migration.
         */
        public RunnableDataTypeMigration(String customer, Map<String, Map<String, Object>> cache) {
            this.customer = customer == null ? "" : customer;
            this.cache = cache;
        }

        /**
         * Run the data type migration.
         */
        @Override
        public void run() {

            Session session = MongoDBService.getHibernateSession();
            Transaction transaction = session.getTransaction();
            transaction.begin();
            switch (customer.toLowerCase()) {
                case "dupont":
                    logger.info("************************** Case dupont");
                    migDupontThingTypeField(cache);
                    break;
                case "fmc":
                    logger.info("************************** Case fmc");
                    migFMCThingTypeFields(cache);
                    break;
                case "sharaf":
                    logger.info("************************** Case sharaf");
                    migSharafThingTypeFields(cache, "sharaf.rfid");
                    break;
                case "aramco":
                    logger.info("************************** Case aramco");
                    migAramcoThingTypeFields(cache);
                    break;
                case "netapp":
                    logger.info("************************** Case netapp");
                    migNetAppThingTypeFields(cache);
                    break;
                case "":
                    logger.info("************************** Case empty");
                    migrateDefaultData(cache);
                    break;
                default:
                    logger.info("************************** Case default");
                    break;
            }
            transaction.commit();
        }

        /**
         * A method to migrate all thing type fields in mysql.
         *
         * @param cache         A cache to update thing type fields.
         * @param thingTypeCode thing type code to get thing type fields.
         */
        public void migSharafThingTypeFields(Map<String, Map<String, Object>> cache, String thingTypeCode) {

            try {
                //Update group HierarchyName
                GroupService.getInstance().refreshHierarchyName();

                //Update UDFs
                for (ThingTypeField thingTypeField : ThingTypeFieldService.getInstance().getByThingTypeCode
                        (thingTypeCode)) {
                    //Change zone from string to Zone type
                    if (thingTypeField.getName().equals("zone")) {
                        thingTypeField.setDataType(DataTypeService.getInstance().get(TYPE_ZONE.value));
                        cache.get("thingTypeField").put(thingTypeField.getId().toString(), thingTypeField);
                        cache = migrateOnlyZoneTypes(cache);
                        migrateReports(thingTypeField);
                    }
                    //Change zone from string to Zone type
                    if (thingTypeField.getName().equals("facilityCode")) {
                        thingTypeField.setDataType(DataTypeService.getInstance().get(TYPE_GROUP.value));
                        cache.get("thingTypeField").put(thingTypeField.getId().toString(), thingTypeField);
                    }
                    //Change fields ends with "date" to Date type
                    if (thingTypeField.getName().toLowerCase().endsWith("date")) {
                        thingTypeField.setDataType(DataTypeService.getInstance().get(TYPE_DATE.value));
                        cache.get("thingTypeField").put(thingTypeField.getId().toString(), thingTypeField);
                    }
                    if (thingTypeField.getName().toLowerCase().equals("timestamp")) {
                        thingTypeField.setDataType(DataTypeService.getInstance().get(TYPE_TIMESTAMP.value));
                        cache.get("thingTypeField").put(thingTypeField.getId().toString(), thingTypeField);
                    }
                    getThingTypeFieldDAO().update(thingTypeField);
                }
            } catch (Exception e) {
                logger.warn("error", e);
            }
            logger.info("Migrated Sharaf ThingType");
        }

        /**
         * A method to migrate all thing type fields in mysql.
         *
         * @param cache A cache to update thing type fields.
         */
        public void migDupontThingTypeField(Map<String, Map<String, Object>> cache) {
            try {
                //Update group HierarchyName
                GroupService.getInstance().refreshHierarchyName();


                //Update UDFs
                for (ThingTypeField thingTypeField : getThingTypeFieldDAO().selectAll()) {

                    //Change zone from string to Zone type
                    if (thingTypeField.getName().equals("zone") && thingTypeField.getThingType().getThingTypeCode()
                            .equals("epc.plant")) {
                        thingTypeField.setDataType(DataTypeService.getInstance().get(TYPE_ZONE.value));
                        cache.get("thingTypeField").put(thingTypeField.getId().toString(), thingTypeField);
                    }
                    getThingTypeFieldDAO().update(thingTypeField);
                }
            } catch (Exception e) {
                logger.warn("error", e);
            }
            String msg = "Migrated Dupont ThingType";
            logger.info(msg);
        }

        /**
         * A method to migrate all thing type fields in mysql.
         *
         * @param cache A cache to update thing type fields.
         */
        public void migFMCThingTypeFields(Map<String, Map<String, Object>> cache) {
            try {
                //Update group HierarchyName
                GroupService.getInstance().refreshHierarchyName();
                //Update UDFs
                for (ThingTypeField thingTypeField : ThingTypeFieldService.getThingTypeFieldDAO().selectAll()) {
                    //Change zone from string to Zone type
                    if (thingTypeField.getName().equals("zone") || thingTypeField.getName().equals("ScanZone")) {
                        thingTypeField.setDataType(DataTypeService.getInstance().get(TYPE_ZONE.value));
                        cache.get("thingTypeField").put(thingTypeField.getId().toString(), thingTypeField);
                    }
                    //Change 'Plant' String to 'Plant' Group
                    if (thingTypeField.getName().equals("Plant")) {
                        thingTypeField.setDataType(DataTypeService.getInstance().get(TYPE_GROUP.value));
                        cache.get("thingTypeField").put(thingTypeField.getId().toString(), thingTypeField);
                    }
                    ThingTypeFieldService.getThingTypeFieldDAO().update(thingTypeField);
                    migrateReports(thingTypeField);
                }
            } catch (Exception e) {
                logger.warn("error", e);
            }
            String msg = "Migrated FMC ThingTypes";
            logger.info(msg);
        }

        /**
         * A method to migrate all thing type fields in mysql.
         *
         * @param cache A cache to update thing type fields.
         */
        public void migAramcoThingTypeFields(Map<String, Map<String, Object>> cache) {
            try {
                //Update group HierarchyName
                GroupService.getInstance().refreshHierarchyName();
                //Update UDFs
                for (ThingTypeField thingTypeField : ThingTypeFieldService.getThingTypeFieldDAO().selectAll()) {

                    //Change zone from string to Zone type
                    if (thingTypeField.getName().equals("zone") && thingTypeField.getThingType().getThingTypeCode()
                            .equals(
                                    "aramco.tag")) {
                        thingTypeField.setDataType(DataTypeService.getInstance().get(TYPE_ZONE.value));
                        cache.get("thingTypeField").put(thingTypeField.getId().toString(), thingTypeField);
                    }
                    if (thingTypeField.getName().equals("shift") && thingTypeField.getThingType().getThingTypeCode()
                            .equals("aramco.tag")) {
                        thingTypeField.setDataType(DataTypeService.getInstance().get(TYPE_SHIFT.value));
                        cache.get("thingTypeField").put(thingTypeField.getId().toString(), thingTypeField);
                    }
                    ThingTypeFieldService.getThingTypeFieldDAO().update(thingTypeField);
                    migrateReports(thingTypeField);
                }
            } catch (Exception e) {
                logger.warn("error", e);
            }
            String msg = "Migrated Aramco ThingType";
            logger.info(msg);
        }

        /**
         * A method to migrate all thing type fields in mysql.
         *
         * @param cache A cache to update thing type fields.
         */
        public void migNetAppThingTypeFields(Map<String, Map<String, Object>> cache) {
            try {
                //Update group HierarchyName
                GroupService.getInstance().refreshHierarchyName();
                //Update UDFs
                for (ThingTypeField thingTypeField : ThingTypeFieldService.getThingTypeFieldDAO().selectAll()) {
                    thingTypeField.setTypeParent("DATA_TYPE");

                    //Change zone from string to Zone type
                    if (thingTypeField.getName().equals("zone") && thingTypeField.getThingType().getThingTypeCode()
                            .equals("NetApp.tag")) {
                        thingTypeField.setDataType(DataTypeService.getInstance().get(TYPE_ZONE.value));
                        cache.get("thingTypeField").put(thingTypeField.getId().toString(), thingTypeField);
                        migrateReports(thingTypeField);
                    }
                    ThingTypeFieldService.getThingTypeFieldDAO().update(thingTypeField);
                }

            } catch (Exception e) {
                logger.warn("error", e);
            }
            String msg = "Migrated NetApp ThingType";
            logger.info(msg);
        }

        /**
         * A method to migrate all thing type fields in mysql.
         *
         * @param cache A cache to update thing type fields.
         */
        public void migrateDefaultData(Map<String, Map<String, Object>> cache) {
            try {
                //Update group HierarchyName
                GroupService.getInstance().refreshHierarchyName();


                //Update UDFs
                for (ThingTypeField thingTypeField : ThingTypeFieldService.getThingTypeFieldDAO().selectAll()) {
                    //Change zone from string to Zone type
                    if (thingTypeField.getName().equals("zone")) {
                        thingTypeField.setDataType(DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_ZONE
                                .value));
                        cache.get("thingTypeField").put(thingTypeField.getId().toString(), thingTypeField);
                        migrateReports(thingTypeField);
                    }

                    //Change fields ends with "date" to Date type
                    if (thingTypeField.getName().toLowerCase().endsWith("date")) {
                        thingTypeField.setDataType(DataTypeService.getInstance()
                                .get(ThingTypeField.Type.TYPE_DATE.value));
                        cache.get("thingTypeField").put(thingTypeField.getId().toString(), thingTypeField);
                    }

                    ThingTypeFieldService.getThingTypeFieldDAO().update(thingTypeField);
                }
            } catch (Exception e) {
                logger.warn("error", e);
            }
            String msg = "Migrated Default ThingType";
            logger.info(msg);
        }

        /**
         * set a zone by id or code or name.
         *
         * @param cache all cache data.
         * @param value a value to get zone.
         * @return a zone object,
         */
        private Zone setZone(Map<String, Map<String, Object>> cache, String value) {
            Zone zone = null;
            if (cache.get("zoneById").containsKey(value)) {
                zone = (Zone) cache.get("zoneById").get(value);
            } else if (cache.get("zone").containsKey(value)) {
                zone = (Zone) cache.get("zone").get(value);
            } else if (cache.get("zoneByName").containsKey(value)) {
                zone = (Zone) cache.get("zoneByName").get(value);
            }
            return zone;
        }

        /**
         * Convert Relative Date of a report filter
         *
         * @param reportFilterValue A report filter object.
         * @return String with the new date format
         */
        private String convertRelativeDate(String reportFilterValue) {
            if (!reportFilterValue.contains("/")) {
                return reportFilterValue;
            }
            String[] values = reportFilterValue.split("/");
            Long minutes = Long.parseLong(values[0]) * 1440L + Long.parseLong(values[1]) * 60 + Long.parseLong
                    (values[2]);
            String result = "NOW";
            if (1 <= minutes && minutes <= 60) {
                result = "AGO_HOUR_1";
            } else if (61 <= minutes && minutes <= 1440) {
                result = "AGO_DAY_1";
            } else if (1441 <= minutes && minutes <= 10080) {
                result = "AGO_WEEK_1";
            } else if (10080 <= minutes && minutes <= 43200) {
                result = "AGO_MONTH_1";
            } else if (43201 <= minutes && minutes <= 525600) {
                result = "AGO_YEAR_1";
            }
            return result;
        }

        /**
         * A method to migrate reports to Vizix last version.
         */
        private void migrateReports(ThingTypeField thingTypeField) {
            for (ReportDefinition reportDefinition : getReportDefinitionDAO().selectAll()) {
                for (ReportFilter reportFilter : reportDefinition.getReportFilter()) {
                    if (reportFilter.getOperator() != null && reportFilter.getThingTypeField() != null &&
                            reportFilter.getThingTypeField().getId().compareTo(thingTypeField.getId()) ==
                                    0 && reportFilter.getOperator().equals("=")) {
                        Zone zone = setZone(cache, reportFilter.getValue());
                        if (zone != null) {
                            reportFilter.setValue(zone.getId().toString());
                        }
                    }
                    if (reportFilter.getPropertyName().equals("relativeDate")) {
                        reportFilter.setValue(convertRelativeDate(reportFilter.getValue()));
                    }
                }

                for (ReportRule reportRule : reportDefinition.getReportRule()) {
                    if ("zone".equals(reportRule.getPropertyName())) {
                        Zone zone = setZone(cache, reportRule.getValue());
                        if (zone != null) {
                            reportRule.setValue(zone.getId().toString());
                        }
                    }
                }
                if (reportDefinition.getPinLabel().equals("true")) {
                    reportDefinition.setPinLabel("1");
                }
                ReportDefinitionService.getInstance().update(reportDefinition);
            }
        }
    }

    /**
     * Runnable class to migrate a single Things.
     */
    private class RunnableMigration implements Runnable {

        /**
         * A object to migrate a single thing.
         */
        private ThingMigration thingMigration;


        public RunnableMigration(Thing thing,
                                 Map cache,
                                 Boolean saveDwellTime,
                                 Long cutoffTimeSeries,
                                 Map<Long, List<Map<String, Object>>> fieldTypeTimeSeries,
                                 Map<Long, Map<String, Object>> fieldTypeNoTimeSeries,
                                 List<Long> thingsFailed) {
            this.thingMigration = new ThingMigration(thing,
                    cache,
                    saveDwellTime,
                    cutoffTimeSeries,
                    fieldTypeTimeSeries,
                    fieldTypeNoTimeSeries,
                    thingsFailed, null);
        }

        @Override
        public void run() {
            thingMigration.runThingMigration();
            try {
                ThingMongoDAO.getInstance().insertThing(thingMigration.thingMongo);
                for (int i = 0; i < thingMigration.lisThingSnapshot.size(); i++) {
                    ThingMongoDAO.getInstance().insertThingSnapshot(thingMigration.lisThingSnapshot.get(i));
                }
                ThingMongoDAO.getInstance().insertThingSnapshotIds(thingMigration.thingSnapshotIds);

                //Associate parent
                if (((Map) thingMigration.cache.get("thingParent")).containsKey(thingMigration.thing.getSerial())) {
                    Thing thingParent
                            = (Thing) ((Map) thingMigration.cache.get("thingParent")).get(thingMigration.thing
                            .getSerial());
                    ThingMongoService.getInstance().associateChild(thingParent, thingMigration.thing.getId());
                }

                //Associate children
                if (((Map) thingMigration.cache.get("thingChildren")).containsKey(thingMigration.thing.getSerial())) {
                    @SuppressWarnings("unchecked")
                    Map<String, List<Thing>> thingCache = (Map<String, List<Thing>>) thingMigration.cache.get
                            ("thingChildren");
                    List<Thing> childrenList = thingCache.get(thingMigration.thing.getSerial());
                    ThingMongoService.getInstance().associateChildren(thingMigration.thing, childrenList);
                }
            } catch (MongoExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Runnable class to migrate Things in a lot.
     */
    private class RunnableBulkMigration implements Runnable {

        private List<Thing> listThing;
        private List<Long> listThingFilter;
        private Map cache;
        private Boolean saveDwellTime;
        private Long cutoffTimeSeries;
        List<Map<Long, List<Map<String, Object>>>> listFieldTypeTimeSeries;
        List<Map<Long, Map<String, Object>>> listFieldTypeNoTimeSeries;
        private List<Long> thingsFailed;
        private ConcurrentLinkedQueue<DBObject> thingsFailedMessages;

        public RunnableBulkMigration(List<Thing> listThing,
                                     List<Long> listThingFilter,
                                     Map cache,
                                     Boolean saveDwellTime,
                                     Long cutOffTimeSeries,
                                     List<Long> thingsFailed,
                                     List<Map<Long, List<Map<String, Object>>>> ListFieldTypeTimeSeries,
                                     List<Map<Long, Map<String, Object>>> ListFieldTypeNoTimeSeries,
                                     ConcurrentLinkedQueue<DBObject> thingsFailedMessages) {
            this.listThing = listThing;
            this.listThingFilter = listThingFilter;
            this.cache = cache;
            this.saveDwellTime = saveDwellTime;
            this.cutoffTimeSeries = cutOffTimeSeries;
            this.listFieldTypeTimeSeries = ListFieldTypeTimeSeries;
            this.listFieldTypeNoTimeSeries = ListFieldTypeNoTimeSeries;
            this.thingsFailed = thingsFailed;
            this.thingsFailedMessages = thingsFailedMessages;

        }

        @Override
        public void run() {
            try {

                BulkWriteOperation bulkThings = MongoDAOUtil.getInstance().things
                        .initializeUnorderedBulkOperation();
                BulkWriteOperation bulkThingSnapshot = MongoDAOUtil.getInstance().thingSnapshots
                        .initializeUnorderedBulkOperation();
                BulkWriteOperation bulkThingSnapshotIds = MongoDAOUtil.getInstance().thingSnapshotIds
                        .initializeUnorderedBulkOperation();

                long numberThingsToProcess = 0L;
                long numberThingSnapshotIdsToProcess = 0L;
                long numberThingSnapShotToProcess = 0L;
                for (int i = 0; i < this.listThing.size(); i++) {
                    if (listThingFilter == null || listThingFilter.contains(this.listThing.get(i).getId())) {

                        ThingMigration thingMigration = new ThingMigration(listThing.get(i),
                                cache,
                                saveDwellTime,
                                cutoffTimeSeries,
                                listFieldTypeTimeSeries.get(i),
                                listFieldTypeNoTimeSeries.get(i),
                                thingsFailed,
                                thingsFailedMessages);
                        thingMigration.runThingMigration();

                        if (thingMigration.thingMongo != null) {
                            /* Add update thing to bulk transaction */
                            BasicDBObject idFinder = new BasicDBObject("_id", thingMigration.thing.getId());
                            bulkThings.find(idFinder).upsert().update(thingMigration.thingMongo);
                            numberThingsToProcess++;
                            /*Add things SnapshotIds  to bulk transaction*/
                            if (thingMigration.thingSnapshotIds != null) {
                                bulkThingSnapshotIds.find(idFinder).upsert().update(thingMigration.thingSnapshotIds);
                                numberThingSnapshotIdsToProcess++;
                            }

                            /*Add things Snapshots  to bulk transaction*/
                            for (int j = 0; j < thingMigration.lisThingSnapshot.size(); j++) {
                                bulkThingSnapshot.insert(thingMigration.lisThingSnapshot.get(j));
                                numberThingSnapShotToProcess++;
                            }
                        }

                    }
                }
                if (numberThingSnapShotToProcess > 0)
                    bulkThingSnapshot.execute();

                if (numberThingSnapshotIdsToProcess > 0)
                    bulkThingSnapshotIds.execute();

                if (numberThingsToProcess > 0)
                    bulkThings.execute();
                logger.info("End Bulk migration step.");

            } catch (Exception e) {
                logger.warn("Exception while running MongoDBService ",e);
            }
        }
    }

    public class ThingMigration {

        private Thing thing;
        private Map cache;
        private Boolean saveDwellTime;
        private Long cutoffTimeSeries;
        private List<Long> thingsFailed;
        private Map<Long, List<Map<String, Object>>> fieldTypeTimeSeries;
        private Map<Long, Map<String, Object>> fieldTypeNoTimeSeries;
        private List<BasicDBObject> lisThingSnapshot;
        private BasicDBObject thingMongo;
        private BasicDBObject thingSnapshotIds;
        private ConcurrentLinkedQueue<DBObject> thingsFailedMessages;

        public ThingMigration(Thing thing,
                              Map cache,
                              Boolean saveDwellTime,
                              Long cutoffTimeSeries,
                              Map<Long, List<Map<String, Object>>> fieldTypeTimeSeries,
                              Map<Long, Map<String, Object>> fieldTypeNoTimeSeries,
                              List<Long> thingsFailed,
                              ConcurrentLinkedQueue<DBObject> thingsFailedMessages) {
            this.thing = thing;
            this.cache = cache;
            this.saveDwellTime = saveDwellTime;
            this.cutoffTimeSeries = cutoffTimeSeries;
            this.fieldTypeTimeSeries = fieldTypeTimeSeries;
            this.fieldTypeNoTimeSeries = fieldTypeNoTimeSeries;
            this.thingsFailed = thingsFailed;
            this.lisThingSnapshot = new ArrayList<>();
            this.thingsFailedMessages = thingsFailedMessages;
        }

        /**
         * run a single thing migration.
         */
        public void runThingMigration() {
            BasicDBList thingErrors = null;

            try {

                TimerUtil timeLog = new TimerUtil();
                timeLog.mark();

                List<Long> timeSeriesArrayList = new ArrayList<>();
                List<Long> noTimeSeriesArrayList = new ArrayList<>();

                Map<String, Date> timesMapTimeSeries = new HashMap<>();
                Map<String, Date> timesMapNoTimeSeries = new HashMap<>();

                Map<String, Object> valuesMapTimeSeries = new HashMap<>();
                Map<String, Object> valuesMapNoTimeSeries = new HashMap<>();

                @SuppressWarnings("unchecked")
                Map<String, ThingTypeField> thingTypeFieldVales = (Map<String, ThingTypeField>) (cache.get
                        ("thingTypeField"));
                Collection<ThingTypeField> thingTypeFieldCollection = thingTypeFieldVales.values();

                for (ThingTypeField tf : thingTypeFieldCollection) {
                    if (fieldTypeTimeSeries.containsKey(tf.getId())) {
                        List<Map<String, Object>> thingTypeFieldValueList = fieldTypeTimeSeries.get(tf.getId());
                        for (Map<String, Object> thingTypeFieldValues : thingTypeFieldValueList) {
                            Long timestamp = DateUtils.round(((Date) thingTypeFieldValues.get("time")), Calendar
                                    .SECOND).getTime();
                            if (!timeSeriesArrayList.contains(timestamp))
                                timeSeriesArrayList.add(timestamp);
                            timesMapTimeSeries.put(timestamp + "|" + tf.getId(), ((Date) thingTypeFieldValues.get
                                    ("time")));
                            valuesMapTimeSeries.put(timestamp + "|" + tf.getId(), thingTypeFieldValues.get("value")
                                    != null ? thingTypeFieldValues.get("value").toString() : "");
                        }

                    } else if (fieldTypeNoTimeSeries.containsKey(tf.getId())) { //No time series
                        Map<String, Object> thingTypeFieldValue = fieldTypeNoTimeSeries.get(tf.getId());
                        Long timestamp = DateUtils.round((Date) thingTypeFieldValue.get("time"), Calendar.SECOND)
                                .getTime();
                        if (!noTimeSeriesArrayList.contains(timestamp))
                            noTimeSeriesArrayList.add(timestamp);
                        timesMapNoTimeSeries.put(timestamp + "|" + tf.getId(), ((Date) thingTypeFieldValue.get
                                ("time")));
                        valuesMapNoTimeSeries.put(timestamp + "|" + tf.getId(), thingTypeFieldValue.get("value") !=
                                null ? thingTypeFieldValue.get("value").toString() : "");
                    }
                }

                Collections.sort(timeSeriesArrayList);
                Collections.sort(noTimeSeriesArrayList);

                Map<String, Object> thingTypeFields = new HashMap<>();

                List<Long> timeEpoch = new ArrayList<>();
                Map<Long, ObjectId> objectIds = new HashMap<>();

                //Cutting off snapshots ids
                if (timeSeriesArrayList.size() > cutoffTimeSeries.intValue()) {
                    timeSeriesArrayList = timeSeriesArrayList.subList(timeSeriesArrayList.size() - cutoffTimeSeries
                            .intValue(), timeSeriesArrayList.size() - 1);
                } else if (timeSeriesArrayList.size() == 0) {
                    timeSeriesArrayList.add(noTimeSeriesArrayList.get(noTimeSeriesArrayList.size() - 1));
                }

                for (Long timeStampTimeSeries : timeSeriesArrayList) {

                    for (String key : thingTypeFields.keySet()) {
                        if (thingTypeFields.get(key) instanceof Map) {
                            ((Map) thingTypeFields.get(key)).remove("changed");
                        }
                    }

                    for (ThingTypeField thingTypeField : thingTypeFieldCollection) {

                        //for no time series
                        for (Long timeStampNoTimeSeries : noTimeSeriesArrayList) {

                            if ((timeStampNoTimeSeries <= timeStampTimeSeries) && timesMapNoTimeSeries.containsKey
                                    (timeStampNoTimeSeries + "|" + thingTypeField.getId()) && !thingTypeFields
                                    .containsKey(thingTypeField.getName())) {

                                Object valueTFNoTimeSeries = valuesMapNoTimeSeries.get(timeStampNoTimeSeries + "|" +
                                        thingTypeField.getId());

                                valueTFNoTimeSeries = getValue(valueTFNoTimeSeries, thingTypeField);

                                ThingTypeFieldService tTFService = ThingTypeFieldService.getInstance();

                                if (tTFService.isValidDataTypeToCheck(thingTypeField.getDataType().getId())) {
                                    if (tTFService.checkDataType(valueTFNoTimeSeries, thingTypeField.getDataType()
                                            .getId(), false)) {
                                        thingTypeFields.put(thingTypeField.getName(), createTFieldNoTimeSeries
                                                (timesMapNoTimeSeries, timeStampNoTimeSeries, valueTFNoTimeSeries,
                                                        thingTypeField, timeSeriesArrayList, timeStampTimeSeries,
                                                        thingTypeField.getTimeSeries()));
                                    } else {
                                        if (thingErrors == null) {
                                            thingErrors = new BasicDBList();
                                        }
                                        thingErrors.add(new BasicDBObject("tfNoTimeSeriesWarning", "Error to " +
                                                "validate" + " thing field value: " + valueTFNoTimeSeries + ", data " +
                                                "type: " + thingTypeField.getDataType().getCode() + ", " + "thing " +
                                                "type field: " + thingTypeField.getName()).append("time",
                                                timesMapNoTimeSeries.get(timeStampNoTimeSeries + "|" + thingTypeField
                                                        .getId())));
                                    }
                                } else if (!ValidatorService.isNullOrEmpty(valueTFNoTimeSeries).isError()) {
                                    thingTypeFields.put(thingTypeField.getName(), createTFieldNoTimeSeries
                                            (timesMapNoTimeSeries, timeStampNoTimeSeries, valueTFNoTimeSeries,
                                                    thingTypeField, timeSeriesArrayList, timeStampTimeSeries,
                                                    thingTypeField.getTimeSeries()));
                                } else {
                                    if (thingErrors == null) {
                                        thingErrors = new BasicDBList();
                                    }
                                    thingErrors.add(new BasicDBObject("tfNoTimeSeriesWarning", "The " + "field value " +
                                            "is empty \"" +
                                            valueTFNoTimeSeries + "\", data type: " + thingTypeField.getDataType()
                                            .getCode() + ", thing " +
                                            "type field: " + thingTypeField.getName()).append("time",
                                            timesMapNoTimeSeries.get
                                                    (timeStampNoTimeSeries + "|" + thingTypeField.getId())));
                                }
                            }
                        }

                        //for time series
                        if (timesMapTimeSeries.containsKey(timeStampTimeSeries + "|" + thingTypeField.getId())) {

                            Object valueTFTimeSeries = valuesMapTimeSeries.get(timeStampTimeSeries + "|" +
                                    thingTypeField.getId());

                            valueTFTimeSeries = getValue(valueTFTimeSeries, thingTypeField);

                            ThingTypeFieldService tTFService = ThingTypeFieldService.getInstance();
                            if (tTFService.isValidDataTypeToCheck(thingTypeField.getDataType().getId())) {
                                if (tTFService.checkDataType(valueTFTimeSeries, thingTypeField.getDataType().getId(), false)) {
                                    thingTypeFields.put(thingTypeField.getName(), createTFieldTimeSeries
                                            (timesMapTimeSeries, timeStampTimeSeries, valueTFTimeSeries,
                                                    timeSeriesArrayList, thingTypeField));
                                } else {
                                    if (thingErrors == null) {
                                        thingErrors = new BasicDBList();
                                    }
                                    thingErrors.add(new BasicDBObject("tfTimeSeriesWarning",
                                            "Error to validate thing field value: " + valueTFTimeSeries + ", " +
                                                    "data" + " type: " + thingTypeField.getDataType().getCode() +
                                                    ", thing type Field: " + thingTypeField.getName()).append("time", timesMapTimeSeries
                                            .get(timeStampTimeSeries
                                                    + "|" + thingTypeField.getId())));
                                }
                            } else if (!ValidatorService.isNullOrEmpty(valueTFTimeSeries).isError()) {
                                thingTypeFields.put(thingTypeField.getName(), createTFieldTimeSeries
                                        (timesMapTimeSeries, timeStampTimeSeries, valueTFTimeSeries,
                                                timeSeriesArrayList, thingTypeField));
                            } else {
                                if (thingErrors == null) {
                                    thingErrors = new BasicDBList();
                                }
                                thingErrors.add(new BasicDBObject("tfTimeSeriesWarning", "The " +
                                        "field value is empty" + valueTFTimeSeries + ", data type: " +
                                        thingTypeField.getDataType().getCode() + ", thing type field: " +
                                        thingTypeField.getName()).append("time", timesMapTimeSeries.get(timeStampTimeSeries + "|" +
                                        thingTypeField.getId())));
                            }
                        }
                    }

                    Map<String, Object> thingTypeFieldsInsert;

                    DBObject thingMongo = ThingMongoDAO.getInstance().getThing(thing.getId());

                    //Verify exist real time Series and add new thing SnapShot if true.
                    if (thingTypeFields.size() != 0) {
                        ObjectId objectId = new ObjectId();
                        try {
                            //noinspection unchecked
                            thingTypeFieldsInsert = ThingService.getInstance().getUdfMapForMongo(thing.getId(), thingMongo, thing
                                    .getThingType(), thingTypeFields, new Date(timeStampTimeSeries), cache, thing
                                    .getGroup(), false);

                            //noinspection unchecked
                            this.lisThingSnapshot.add(ThingMongoDAO.getInstance().buildThingSnapshot(thing,
                                    (Map<String, Object>) thingTypeFieldsInsert.get("thingTypeFieldResponse"), new
                                            Date(timeStampTimeSeries), objectId));
                            timeEpoch.add(timeStampTimeSeries);
                            objectIds.put(timeStampTimeSeries, objectId);
                        } catch (Exception ex) {
                            if (thingErrors == null) {
                                thingErrors = new BasicDBList();
                            }
                            thingErrors.add(new BasicDBObject("thingSnapShotError", ex));
                        }
                    }

                    //verify last timeSeriesArrayList to insert on things.
                    if (timeSeriesArrayList.indexOf(timeStampTimeSeries) == timeSeriesArrayList.size() - 1) {

                        for (ThingTypeField thingTypeField : thingTypeFieldCollection) {

                            //complete after last timeSeriesArrayList with no time series
                            for (Long timeNoTimeSeries : noTimeSeriesArrayList) {

                                if (timeNoTimeSeries >= timeStampTimeSeries &&
                                        timesMapNoTimeSeries.containsKey(timeNoTimeSeries + "|" + thingTypeField
                                                .getId()) &&
                                        !thingTypeFields.containsKey(thingTypeField.getName())) {

                                    Object valueField = valuesMapNoTimeSeries.get(timeNoTimeSeries + "|" +
                                            thingTypeField.getId());

                                    valueField = getValue(valueField, thingTypeField);

                                    ThingTypeFieldService tTFService = ThingTypeFieldService.getInstance();

                                    // Verify if data type have validator.
                                    if (tTFService.isValidDataTypeToCheck(thingTypeField.getDataType().getId())) {
                                        if (tTFService.checkDataType(valueField, thingTypeField.getDataType().getId(), false
                                        )) {
                                            thingTypeFields.put(thingTypeField.getName(), createThingField
                                                    (timesMapNoTimeSeries, timeNoTimeSeries, valueField,
                                                            thingTypeField));
                                        } else {
                                            if (thingErrors == null) {
                                                thingErrors = new BasicDBList();
                                            }
                                            thingErrors.add(new BasicDBObject("thingWarning", "Error to " +
                                                    "validate thing field value: " + valueField + ", " + "data type: " +
                                                    "" + thingTypeField.getDataType().getCode() + ", thing type " +
                                                    "field: " + thingTypeField.getName()).append("time",
                                                    timesMapNoTimeSeries.get
                                                            (timeNoTimeSeries + "|" + thingTypeField.getId())));
                                        }
                                    } else if (!ValidatorService.isNullOrEmpty(valueField).isError()) {
                                        thingTypeFields.put(thingTypeField.getName(), createThingField
                                                (timesMapNoTimeSeries, timeNoTimeSeries, valueField, thingTypeField));
                                    } else {
                                        if (thingErrors == null) {
                                            thingErrors = new BasicDBList();
                                        }
                                        thingErrors.add(new BasicDBObject("thingWarning", "The " + "field" +
                                                " value is empty" + valueField + ", data type: " + thingTypeField
                                                .getDataType().getCode() + ", thing type field: " + thingTypeField
                                                .getName()).append("time", timesMapNoTimeSeries.get(timeNoTimeSeries + "|" +
                                                thingTypeField.getId())));
                                    }
                                }
                            }
                        }

                        try {

                            for (String key : thingTypeFields.keySet()) {
                                //noinspection unchecked
                                ((Map<String, Object>) thingTypeFields.get(key)).remove("dwellTime");
                            }
                            //noinspection unchecked
                            thingTypeFieldsInsert = ThingService.getInstance().getUdfMapForMongo(thing.getId(), thingMongo, thing
                                    .getThingType(), thingTypeFields, new Date(timeStampTimeSeries), cache, thing
                                    .getGroup(), false);
                            if (thingTypeFieldsInsert != null) {
                                //noinspection unchecked
                                this.thingMongo = ThingMongoDAO.getInstance().buildUpsertThing(this.thing,
                                        (Map<String, Object>) thingTypeFieldsInsert.get("thingTypeFieldResponse"),
                                        null);
                            }
                        } catch (Exception ex) {
                            if (thingErrors == null) {
                                thingErrors = new BasicDBList();
                            }
                            thingErrors.add(new BasicDBObject("thingError", ex));
                        }
                    }
                }

                createThingSnapshotIds(timeEpoch, objectIds);
                timeLog.mark();

            } catch (Exception e) {
                logger.error("*** Migration of thing with serial " +
                        thing.getSerial() +
                        " and id " +
                        thing.getId() +
                        " failed, details:" +
                        e.getMessage());
                this.thingsFailed.add(thing.getId());
                e.printStackTrace();
            } finally {
                if (thingErrors != null) {
                    thingsFailedMessages.add(new BasicDBObject("thingId", thing.getId()).append("errorDetail",
                            thingErrors));
                }
            }
        }

        /**
         * Create thing snapshot Ids
         *
         * @param timeEpoch List of Time Epoch.
         * @param objectIds Map of Object Ids
         */
        private void createThingSnapshotIds(List<Long> timeEpoch, Map<Long, ObjectId> objectIds) {
            Collections.sort(timeEpoch, Collections.reverseOrder());
            List<Map<String, Object>> ids = new ArrayList<>();
            for (Long time : timeEpoch) {
                Map<String, Object> item = new HashMap<>();
                item.put("time", time);
                item.put("blink_id", objectIds.get(time));
                ids.add(item);
            }
            if (ids.size() > 0) {
                this.thingSnapshotIds = ThingMongoDAO.getInstance().buildThingSnapshotIds(ids);
            }
        }

        /**
         * Create a Map of a thing field in the mongo map format to insert or update a thing.
         *
         * @param timesMapNoTimeSeries A map that has the thing fields in Timestamp and date.
         * @param timeNoTimeSeries     Date in timeStamp format to a thing field to convert in map.
         * @param valueField           Value of a thing field.
         * @param thingTypeField       The thing type field of a field.
         * @return The thing type field on map format.
         */
        private Map<String, Object> createThingField(Map<String, Date> timesMapNoTimeSeries, Long timeNoTimeSeries,
                                                     Object valueField, ThingTypeField thingTypeField) {
            Map<String, Object> thingField = new HashMap<>();
            thingField.put("id", thingTypeField.getId());
            thingField.put("thingFieldId", thingTypeField.getId());
            thingField.put("time", timesMapNoTimeSeries.get(timeNoTimeSeries + "|" + thingTypeField.getId()));
            thingField.put("value", valueField.toString().trim());
            return thingField;
        }

        /**
         * Create thing field no time series.
         *
         * @param timesMapNoTimeSeries  A map that has the thing fields no time series in Timestamp and date format.
         * @param timeStampNoTimeSeries Date in timeStamp format to a thing field no time series to convert in map.
         * @param valueTFNoTimeSeries   Value of a thing field.
         * @param thingTypeField        The thing type field of a field.
         * @param timeSeriesArrayList   Array of a thing fields with time series timeStamps.
         * @param timeStampTimeSeries   A element of a timeSeriesArrayList Array.
         * @return The thing type field no time series on map format with DwellTime.
         */
        private Map<String, Object> createTFieldNoTimeSeries(Map<String, Date> timesMapNoTimeSeries,
                                                             Long timeStampNoTimeSeries,
                                                             Object valueTFNoTimeSeries,
                                                             ThingTypeField thingTypeField,
                                                             List<Long> timeSeriesArrayList,
                                                             Long timeStampTimeSeries,
                                                             Boolean isTimeSeries) {

            Map<String, Object> tempTf = createThingField(timesMapNoTimeSeries, timeStampNoTimeSeries,
                    valueTFNoTimeSeries, thingTypeField);
            if (saveDwellTime) {
                Long nextTimestamp = timeSeriesArrayList.indexOf(timeStampTimeSeries) + 1 < timeSeriesArrayList.size
                        () ? timeSeriesArrayList.get
                        (timeSeriesArrayList.indexOf(timeStampTimeSeries) + 1) : 0L;
                tempTf.put("dwellTime", nextTimestamp == 0 ? 0 : (nextTimestamp -
                        timesMapNoTimeSeries.get(timeStampNoTimeSeries + "|" + thingTypeField.getId()).getTime()));
                if (isTimeSeries) {
                    tempTf.put("changed", true);
                    tempTf.put("blinked", true);
                }
            }
            return tempTf;
        }

        /**
         * Create thing field time series.
         *
         * @param timesMapTimeSeries  A map that has the thing fields time series in Timestamp and date format.
         * @param timeStampTimeSeries Date in timeStamp format to a thing field to convert in map.
         * @param valueTFTimeSeries   Date in timeStamp format to a thing field time series to convert in map.
         * @param timeSeriesArrayList Array of a thing fields with time series timeStamps.
         * @param thingTypeField      The thing type field of a field.
         * @return The thing type field time series on map format with DwellTime.
         */
        private Map<String, Object> createTFieldTimeSeries(Map<String, Date> timesMapTimeSeries,
                                                           Long timeStampTimeSeries,
                                                           Object valueTFTimeSeries,
                                                           List<Long> timeSeriesArrayList,
                                                           ThingTypeField thingTypeField) {
            Map<String, Object> tempTf = new HashMap<>();
            tempTf.put("thingTypeFieldId", thingTypeField.getId());
            tempTf.put("time", timesMapTimeSeries.get(timeStampTimeSeries + "|" + thingTypeField.getId()));
            tempTf.put("value", valueTFTimeSeries.toString().trim());

            if (saveDwellTime) {
                Long nextTimestamp = timeSeriesArrayList.indexOf(timeStampTimeSeries) + 1 < timeSeriesArrayList.size() ?
                        timeSeriesArrayList.get(timeSeriesArrayList.indexOf(timeStampTimeSeries) + 1) : 0L;
                tempTf.put("dwellTime", nextTimestamp == 0 ? 0 : nextTimestamp -
                        timesMapTimeSeries.get(timeStampTimeSeries + "|" + thingTypeField.getId()).getTime());
            }
            tempTf.put("changed", true);
            tempTf.put("blinked", true);
            return tempTf;
        }

        /**
         * get Standard Value migration
         *
         * @param thingTypeFieldValue Field value of thing
         * @param thingTypeField      A type of thing field
         * @return value of thing field
         */
        private Object getValue(Object thingTypeFieldValue, ThingTypeField thingTypeField) {
            if ((thingTypeFieldValue != null) && thingTypeField.getDataType().getId().equals
                    (TYPE_TIMESTAMP.value) && thingTypeField.getName().equals("Timestamp")) {

                thingTypeFieldValue = DateHelper.getDateAndDetermineFormat(thingTypeFieldValue.toString());
                if (thingTypeFieldValue != null) {
                    return ((Date) thingTypeFieldValue).getTime();
                } else {
                    return null;
                }
            } else {
                return ThingService.getInstance().getStandardDataType(thingTypeField
                        .getDataType(), thingTypeFieldValue);
            }
        }
    }

    public static Session getHibernateSession() {
        try {
            return HibernateSessionFactory.getInstance().getCurrentSession();
        } catch (Exception ex) {
            logger.error("Hibernate validation Error", ex);
            throw ex;
        }
    }

    public Map<String, Map<String, Object>> migrateOnlyZoneTypes(Map<String, Map<String, Object>> cache) {

        for (ZoneType zoneType : ZoneTypeService.getZoneTypeDAO().selectAll()) {
            if (zoneType.getZoneTypeCode() == null || Objects.equals(zoneType.getZoneTypeCode(), "")) {
                String code = Normalizer.normalize(zoneType.getName().replaceAll(" ", ""), Normalizer.Form.NFD)
                        .replaceAll("[^\\p{ASCII}]", "");
                zoneType.setZoneTypeCode(code);
                ZoneTypeService.getInstance().update(zoneType);
            }
        }

        Map<String, Object> zones = cache.get("zone");
        for (String key : zones.keySet()) {
            Zone zoneItem = (Zone) zones.get(key);
            String zoneTypeCode = zoneItem.getZoneType().getZoneTypeCode();
            if (zoneTypeCode == null || Objects.equals(zoneTypeCode, "")) {
                zoneItem.getZoneType().setZoneTypeCode(Normalizer.normalize(zoneItem.getZoneType().getName().replaceAll(" ", ""), Normalizer.Form.NFD)
                        .replaceAll("[^\\p{ASCII}]", ""));
            }
            cache.get("zone").put(zoneItem.getCode(), zoneItem);
            cache.get("zoneByName").put(zoneItem.getName(), zoneItem);
            cache.get("zoneById").put(zoneItem.getId().toString(), zoneItem);
        }
        return cache;
    }

    /**
     * Creates zoneType inside zone native object mongo document
     *
     * @param connTimeout   TimeOut mongo connection.
     * @param zoneUdfName   A name of zone UDF.
     * @param thingTypeCode A code of the Thing Type.
     * @return A Map of the new zones after the migration.
     */
    public Map<String, Object> migrateZoneType(
            Integer connTimeout,
            String zoneUdfName,
            String thingTypeCode) throws UnknownHostException {

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
                null);

        // Migrate mysql zonTypeCode == null
        //noinspection Duplicates
        for (ZoneType zoneType : ZoneTypeService.getZoneTypeDAO().selectAll()) {
            if (zoneType.getZoneTypeCode() == null) {
                String code = zoneType.getName().replaceAll(" ", "");
                code = Normalizer.normalize(code, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
                zoneType.setZoneTypeCode(code);
                ZoneTypeService.getInstance().update(zoneType);
            }
        }


        Map<String, Object> result = new HashMap<>();
        Map<String, Object> affectedRows = new HashMap<>();

        List<Zone> zoneList = ZoneService.getZoneDAO().selectAll();
        String zoneUdfNameTS = "value." + zoneUdfName;

        for (Zone zone : zoneList) {
            Map<String, Object> collection = new HashMap<>();

            // things
            BasicDBList orZoneProp = new BasicDBList();
            orZoneProp.add(new BasicDBObject(zoneUdfName + ".value.zoneType", new BasicDBObject("$exists", false)));
            orZoneProp.add(new BasicDBObject(zoneUdfName + ".value.zoneGroup", new BasicDBObject("$exists", false)));
            orZoneProp.add(new BasicDBObject(zoneUdfName + ".value.facilityMap", new BasicDBObject("$exists", false)));

            BasicDBObject queryDoc = new BasicDBObject(zoneUdfName, new BasicDBObject("$exists", true))
                    .append("$or", orZoneProp)
                    .append(zoneUdfName + ".value.code", zone.getCode())
                    .append("thingTypeCode", thingTypeCode);

            BasicDBObject setDoc = new BasicDBObject("$set",
                    new BasicDBObject(zoneUdfName + ".value.zoneType", zone.getZoneType().getZoneTypeCode())
                    .append(zoneUdfName + ".value.zoneGroup", zone.getZoneGroup().getName())
                    .append(zoneUdfName + ".value.facilityMap", zone.getLocalMap().getName())
            );
            WriteResult wr = MongoDAOUtil.getInstance().things.update(queryDoc, setDoc, false, true, WriteConcern.ACKNOWLEDGED);

            collection.put("things", wr.getN());

            // thingSnapshots

            orZoneProp = new BasicDBList();
            orZoneProp.add(new BasicDBObject(zoneUdfNameTS + ".value.zoneType", new BasicDBObject("$exists", false)));
            orZoneProp.add(new BasicDBObject(zoneUdfNameTS + ".value.zoneGroup", new BasicDBObject("$exists", false)));
            orZoneProp.add(new BasicDBObject(zoneUdfNameTS + ".value.facilityMap", new BasicDBObject("$exists", false)));
            queryDoc = new BasicDBObject(zoneUdfNameTS, new BasicDBObject("$exists", true))
                    .append("$or", orZoneProp)
                    .append(zoneUdfNameTS + ".value.code", zone.getCode())
                    .append("value.thingTypeCode", thingTypeCode);


            setDoc = new BasicDBObject("$set",
                    new BasicDBObject(zoneUdfNameTS + ".value.zoneType", zone.getZoneType().getZoneTypeCode())
                            .append(zoneUdfNameTS + ".value.zoneGroup", zone.getZoneGroup().getName())
                            .append(zoneUdfNameTS + ".value.facilityMap", zone.getLocalMap().getName())
            );
            wr = MongoDAOUtil.getInstance().thingSnapshots.update(queryDoc, setDoc, false, true, WriteConcern.ACKNOWLEDGED);
            collection.put("thingSnapshots", wr.getN());

            affectedRows.put(zone.getCode(), collection);
        }


        //Unknown Zone

        Map<String, Object> collection = new HashMap<>();

        // things
        BasicDBList orZoneProp = new BasicDBList();
        orZoneProp.add(new BasicDBObject(zoneUdfName + ".value.zoneType", new BasicDBObject("$exists", false)));
        orZoneProp.add(new BasicDBObject(zoneUdfName + ".value.zoneGroup", new BasicDBObject("$exists", false)));
        orZoneProp.add(new BasicDBObject(zoneUdfName + ".value.facilityMap", new BasicDBObject("$exists", false)));
        BasicDBObject queryDoc = new BasicDBObject(zoneUdfName, new BasicDBObject("$exists", true))
                .append("$or", orZoneProp)
                .append(zoneUdfName + ".value.code", "unknown")
                .append("thingTypeCode", thingTypeCode);

        BasicDBObject setDoc =
                new BasicDBObject("$set", new BasicDBObject(zoneUdfName + ".value.zoneType", "unknown")
                        .append(zoneUdfName + ".value.zoneGroup", "unknown")
                        .append(zoneUdfName + ".value.facilityMap", "unknown")
                );

        WriteResult wr = MongoDAOUtil.getInstance().things.update(queryDoc, setDoc, false, true, WriteConcern.MAJORITY);
        collection.put("things", wr.getN());

        // thingSnapshots

        orZoneProp = new BasicDBList();
        orZoneProp.add(new BasicDBObject(zoneUdfNameTS + ".value.zoneType", new BasicDBObject("$exists", false)));
        orZoneProp.add(new BasicDBObject(zoneUdfNameTS + ".value.zoneGroup", new BasicDBObject("$exists", false)));
        orZoneProp.add(new BasicDBObject(zoneUdfNameTS + ".value.facilityMap", new BasicDBObject("$exists", false)));

        queryDoc = new BasicDBObject(zoneUdfNameTS, new BasicDBObject("$exists", true))
                .append("$or", orZoneProp)
                .append(zoneUdfNameTS + ".value.code", "unknown")
                .append("value.thingTypeCode", thingTypeCode);


        setDoc = new BasicDBObject("$set",
                new BasicDBObject(zoneUdfNameTS + ".value.zoneType", "unknown")
                        .append(zoneUdfNameTS + ".value.zoneGroup", "unknown")
                        .append(zoneUdfNameTS + ".value.facilityMap", "unknown"));
        wr = MongoDAOUtil.getInstance().thingSnapshots.update(queryDoc, setDoc, false, true, WriteConcern.MAJORITY);
        collection.put("thingSnapshots", wr.getN());

        affectedRows.put("unknown", collection);

        result.put("affectedDocuments", affectedRows);
        result.put("totalZones", zoneList.size());

        return result;
    }

    @SuppressWarnings("ConstantConditions")
    public Map defaultMigration(String customer,
                                Integer connTimeout,
                                Integer concurrentThreads,
                                String thingsFilter,
                                Integer retries) {
        try {
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
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        //        ThreadPool pool = new ThreadPool(connPerHost);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);


        User rootUser = (User) SecurityUtils.getSubject().getPrincipal();

        Map<String, Map<String, Object>> cache = ThingService.getInstance().getCacheData();

        //New thread to write immediately to database and avoid session (hibernare, shiro, riot) timeout because
        // migration takes a lot of time
        cache = migrateDataTypes(customer, cache);

        CassandraUtils.init(Configuration.getProperty("cassandra.host"),
                Configuration.getProperty("cassandra.keyspace"));

        logger.info("**** Starting Migration Process **** ");

        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        Date today = Calendar.getInstance().getTime();

        ThingMongoDAO dao = ThingMongoDAO.getInstance();
        dao.backupThingsCollecion("things_BKP_" + df.format(today));
        dao.dropThingsCollection();
        dao.dropThingSnapshotsCollection();
        dao.dropThingSnapshotIdsCollection();
        dao.createThingSnapshotsIndexes();

        TimerUtil tu = new TimerUtil();
        tu.mark();
        int lastPercentLogged = 0;
        int percent;
//        boolean saveDwellTime = ConfigurationService.getAsBoolean(rootUser, "saveDwellTimeHistory");
        Long cutOffTimeSeries = ConfigurationService.getAsLong(rootUser, "cutOffTimeSeries");
        int counter = 0;
        int totalThings = ((Map) cache.get("thingById")).values().size();
        logger.info("Total things in database :" + totalThings);

        List<Long> thingsFilterList = createThingsFilters(thingsFilter);
        List<Long> failedThings = new ArrayList<>();
        List<Long> thingTypeFieldIdsTimeSeries = new ArrayList<>();
        List<Long> thingTypeFieldIdsNoTimeSeries = new ArrayList<>();
        //noinspection unchecked
        for (ThingTypeField tf : (Collection<ThingTypeField>) ((Map) cache.get("thingTypeField")).values()) {
            if (tf.getTimeSeries()) {
                thingTypeFieldIdsTimeSeries.add(tf.getId());
            }
            thingTypeFieldIdsNoTimeSeries.add(tf.getId());
        }
        //noinspection unchecked
        for (Thing thing : (Collection<Thing>) ((Map) cache.get("thingById")).values()) {
            if (thingsFilterList == null || thingsFilterList.contains(thing.getId())) {

                Map<Long, List<Map<String, Object>>> fieldTypeTimeSeries = new HashMap<>();
                Map<Long, Map<String, Object>> fieldTypeNoTimeSeries = new HashMap<>();

                boolean error, succeed;
                int retriesfv = 0;
                int retriesfvh = 0;

                error = true;
                succeed = false;
                while (error && !succeed && retriesfv <= retries) {
                    try {
                        error = false;
                        @SuppressWarnings("deprecation")
                        FieldTypeHistoryDAO fieldTypeHistoryDAO = new FieldTypeHistoryDAO();
                        fieldTypeTimeSeries.putAll(fieldTypeHistoryDAO.getHistory(thing.getId(), thingTypeFieldIdsTimeSeries, null, null));
                        succeed = true;
                    } catch (RuntimeException e) {
                        error = true;
                        succeed = false;
                        if (retriesfv == retries) {
                            if (!failedThings.contains(thing.getId())) {
                                failedThings.add(thing.getId());
                            }
                            logger.error("*@*@*@*@*@* Error in processing field_value_history after "
                                    + retries
                                    + " retries for thing : "
                                    + thing.getSerial(), e);
                        } else {
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        }
                    } finally {
                        retriesfv++;
                    }
                }
                error = true;
                succeed = false;
                while (error && !succeed && retriesfvh <= retries) {
                    try {
                        error = false;
                        @SuppressWarnings("deprecation")
                        FieldTypeDAO fieldTypeDAO = new FieldTypeDAO();
                        fieldTypeNoTimeSeries.putAll(fieldTypeDAO.valuesMap(thing.getId(),
                                thingTypeFieldIdsNoTimeSeries));
                        succeed = true;
                    } catch (RuntimeException e) {
                        error = true;
                        succeed = false;
                        if (retriesfvh == retries) {
                            if (!failedThings.contains(thing.getId())) {
                                failedThings.add(thing.getId());
                            }
                            logger.error("*@*@*@*@*@* Error in processing field_value after "
                                    + retries
                                    + " retries for thing : "
                                    + thing.getSerial(), e);
                        } else {
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        }
                    } finally {
                        retriesfvh++;
                    }
                }

                RunnableMigration runnable = new RunnableMigration(thing,
                        cache,
                        true,
//                        saveDwellTime,
                        cutOffTimeSeries,
                        fieldTypeTimeSeries,
                        fieldTypeNoTimeSeries,
                        failedThings);
                executor.execute(runnable);
                counter++;
                percent = (counter * 100 / totalThings);
                if (percent != lastPercentLogged) {
                    logger.info("Migration progress: " + percent + "% (" + counter + " things of " + totalThings + ")");
                    lastPercentLogged = percent;
                }

            }
        }

        executor.shutdown();
        try {
            logger.info("Waiting for threads finish migration.");
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        tu.mark();

        int rate = (int) (counter / (tu.getLastDelt() / 1000.0));
        String msg = "Inserted " + counter + " things' thingSnapshots and thingSnapshotIds collections id in " + tu
                .getLastDelt() + " ms (" + rate + " things/sec)";
        Map<String, Object> response = new HashMap<>();
        response.put("Result", msg);
        response.put("failedThings", failedThings);
        logger.info(response);

        CassandraUtils.shutdown();
        return response;
    }

    public Map bulkMigration(String customer,
                             Integer connTimeOut,
                             Integer concurrentThreads,
                             String thingsFilter,
                             Date beginDate,
                             Date endDate,
                             Integer numberLotThings,
                             Integer retries, Boolean flagCreateSnapshots) {
        try {

            logger.info(" ********** Starting Bulk Migration Process ********** ");
            TimerUtil tu = new TimerUtil();
            tu.mark();
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
                    connTimeOut,
                    concurrentThreads + 50);

            valBeginAndEndDates(beginDate, endDate);
            createMongoBackUp(beginDate);


            //Get user root and cache.
            User rootUser = (User) SecurityUtils.getSubject().getPrincipal();
            @SuppressWarnings("ConstantConditions")
//            boolean saveDwellTime = ConfigurationService.getAsBoolean(rootUser, "saveDwellTimeHistory");
            Long cutOffTimeSeries = ConfigurationService.getAsLong(rootUser, "cutoffTimeseries");

            Map<String, Map<String, Object>> cache = ThingService.getInstance().getCacheData();
            cache = migrateDataTypes(customer, cache);

            CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra" +
                    ".keyspace"));


            int lastPercentLogged = 0;
            int percent;

            int totalThings = ((Collection<Thing>) ((Map) cache.get("thingById")).values()).size();
            int counter = 0;

            logger.info("Total things in the relational database :" + totalThings);

            List<Long> thingsFilterList = createThingsFilters(thingsFilter);
            List<Long> failedThings = new ArrayList<>();
            ConcurrentLinkedQueue<DBObject> mongoMigrationDetail = new ConcurrentLinkedQueue<>();
            int count = 0;

            ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);

            List<Thing> thingLot = new ArrayList<>();
            List<Map<Long, List<Map<String, Object>>>> listFieldTypeTimeSeries = new ArrayList<>();
            List<Map<Long, Map<String, Object>>> listFieldTypeNoTimeSeries = new ArrayList<>();

            @SuppressWarnings("deprecation")
            FieldTypeHistoryDAO fieldTypeHistoryDAO = new FieldTypeHistoryDAO();
            @SuppressWarnings("deprecation")
            FieldTypeDAO fieldTypeDAO = new FieldTypeDAO();
            List<Long> thingsIds = null;

            List<Long> thingTypeFieldIdsTimeSeries;
            List<Long> thingTypeFieldIdsNoTimeSeries;
            //noinspection unchecked
            Collection<Thing> allThingsList = (Collection<Thing>) ((Map) cache.get("thingById")).values();
            Collection<ThingTypeField> thingTypeFieldCollection = (Collection<ThingTypeField>) ((Map) cache.get("thingTypeField")).values();
            if (allThingsList.size() == 0) {
                return responseMigration("No things to migrate.");
            }

            for (Thing thing : allThingsList) {

                Map<Long, List<Map<String, Object>>> fieldTypeTimeSeries;
                Map<Long, Map<String, Object>> fieldTypeNoTimeSeries;

                if (flagCreateSnapshots) {
                    thingTypeFieldIdsTimeSeries = new ArrayList<>();
                    thingTypeFieldIdsNoTimeSeries = new ArrayList<>();
                    filterThingTypeTimeSeriesAndNoTimeSeries(thing, thingTypeFieldIdsTimeSeries,
                            thingTypeFieldIdsNoTimeSeries);
                    //noinspection unchecked
                    fieldTypeTimeSeries = (Map<Long, List<Map<String, Object>>>) fieldTypeHistoryDAO
                            .getFieldValueHistory(thing.getId(), thing.getSerialNumber(),
                                    thingTypeFieldIdsTimeSeries, thingsFilterList,
                                    beginDate,
                                    endDate, retries);
                    thingTypeFieldIdsTimeSeries.removeAll(fieldTypeTimeSeries.keySet());
                    thingTypeFieldIdsNoTimeSeries.addAll(thingTypeFieldIdsTimeSeries);
                    fieldTypeNoTimeSeries = fieldTypeDAO.getFieldValue(thing.getId(), thing.getSerialNumber(),
                            thingTypeFieldIdsNoTimeSeries, thingsFilterList, retries);
                    listFieldTypeNoTimeSeries.add(fieldTypeNoTimeSeries);
                } else {
                    fieldTypeTimeSeries = new HashMap<>();
                    if (thingsIds == null) {
                        thingsIds = new ArrayList<>();
                    }
                    thingsIds.add(thing.getId());
                }

                listFieldTypeTimeSeries.add(fieldTypeTimeSeries);
                count++;
                thingLot.add(thing);

                if (count % numberLotThings == 0) {

                    if (!flagCreateSnapshots) {
                        thingTypeFieldIdsNoTimeSeries = getAllThingTypesFieldsIds(thingTypeFieldCollection);
                        TimerUtil tuGetTimeSeriesFields = new TimerUtil();
                        tuGetTimeSeriesFields.mark();
                        listFieldTypeNoTimeSeries.addAll(fieldTypeDAO.bulkGetFieldValue(thingsIds,
                                thingTypeFieldIdsNoTimeSeries, failedThings, retries));
                        tuGetTimeSeriesFields.mark();
                        logger.info("Verify Get bulk Cassandra field Values time: " + tuGetTimeSeriesFields
                                .getLastDelt());
                    }

                    logger.info("send a lot of: " + count + " Things");
//                    RunnableBulkMigration runnable = new RunnableBulkMigration(thingLot, thingsFilterList, cache,
//                            saveDwellTime, cutOffTimeSeries, failedThings, listFieldTypeTimeSeries,
//                            listFieldTypeNoTimeSeries, mongoMigrationDetail);
                    RunnableBulkMigration runnable = new RunnableBulkMigration(thingLot, thingsFilterList, cache,
                            true, cutOffTimeSeries, failedThings, listFieldTypeTimeSeries,
                            listFieldTypeNoTimeSeries, mongoMigrationDetail);
                    executor.execute(runnable);

                    thingLot = new ArrayList<>();
                    thingsIds = new ArrayList<>();
                    listFieldTypeTimeSeries = new ArrayList<>();
                    listFieldTypeNoTimeSeries = new ArrayList<>();

                    counter = counter + numberLotThings;
                    percent = (counter * 100 / totalThings);

                    if (percent != lastPercentLogged) {
                        logger.info("Migration progress: "
                                + percent
                                + "% ("
                                + counter
                                + " things of "
                                + totalThings
                                + ")");
                        lastPercentLogged = percent;
                    }
                }
            }

            if (thingLot.size() != 0) {
                if (!flagCreateSnapshots) {
                    thingTypeFieldIdsNoTimeSeries = getAllThingTypesFieldsIds(thingTypeFieldCollection);
                    TimerUtil tuGetTimeSeriesFields = new TimerUtil();
                    tuGetTimeSeriesFields.mark();
                    listFieldTypeNoTimeSeries.addAll(fieldTypeDAO.bulkGetFieldValue(thingsIds,
                            thingTypeFieldIdsNoTimeSeries, failedThings, retries));
                    tuGetTimeSeriesFields.mark();
                    logger.info("The Lot Last: Verify Get bulk Cassandra field Values time: " + tuGetTimeSeriesFields
                            .getLastDelt());
                }

                if (listFieldTypeTimeSeries.size() != 0 && listFieldTypeNoTimeSeries.size() != 0) {


                    RunnableBulkMigration runnable = new RunnableBulkMigration(thingLot,
                            thingsFilterList,
                            cache,
                            true,
//                            saveDwellTime,
                            cutOffTimeSeries,
                            failedThings,
                            listFieldTypeTimeSeries,
                            listFieldTypeNoTimeSeries,
                            mongoMigrationDetail);
                    executor.execute(runnable);
                    counter += thingLot.size();
                }
            }
            executor.shutdown();

            try {
                logger.info("Waiting for bulk threads finish default migration.");
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            tu.mark();
            int rate = (int) (counter / (tu.getLastDelt() / 1000.0));

            String msg = "Inserted " + counter + " things' thingSnapshots and thingSnapshotIds collections id in " +
                    tu.getLastDelt() + " ms (" + rate + " things/sec)";

            Map<String, Object> response = new HashMap<>();
            response.put("Result", msg);
            response.put("failedThings", failedThings);

            ObjectId objectId = new ObjectId();
            BasicDBObject mongoMigration = new BasicDBObject("_id", objectId);
            mongoMigration.append("customer", customer);
            try {
                mongoMigration.append("computerName", InetAddress.getLocalHost().getHostName());
                mongoMigration.append("computerUser", System.getProperty("user.name"));
                mongoMigration.append("computerId", InetAddress.getLocalHost().getHostAddress());

            } catch (UnknownHostException e) {
                mongoMigration.append("computerName", "Host Name can not be resolved");
                mongoMigration.append("computerUser", System.getProperty("user.name"));
                mongoMigration.append("computerId", "Host IP can not be resolved");
            }
            mongoMigration.append("customer", customer);
            mongoMigration.append("date", new Date());
            mongoMigration.append("endDate", endDate);
            mongoMigration.append("beginDate", beginDate);
            mongoMigration.append("summary", msg + " Failed Things: " + failedThings.toString());
            mongoMigration.append("version", Integer.toString(CodeVersion.getInstance().getCodeVersion()));
            MongoDAOUtil.getInstance().db.getCollection("mongoMigration").insert(mongoMigration);

            DBCollection mongoMigDetailCollection = MongoDAOUtil.getInstance().db.getCollection("mongoMigDetail");
            BulkWriteOperation bulkMongoMigDetail = mongoMigDetailCollection.initializeUnorderedBulkOperation();

            long numberMongoMigrationDetail = 0L;

            for (DBObject aMongoMigrationDetail : mongoMigrationDetail) {
                BasicDBObject mongoMigDetailItem = (BasicDBObject) aMongoMigrationDetail;
                mongoMigDetailItem.append("MongoMigrationId", objectId);
                bulkMongoMigDetail.insert(mongoMigDetailItem);
                numberMongoMigrationDetail++;
            }

            if (numberMongoMigrationDetail > 0) {
                BulkWriteResult result = bulkMongoMigDetail.execute(WriteConcern.ACKNOWLEDGED);
                logger.info(result);
            }
            logger.info(response);
            CassandraUtils.shutdown();
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
            return responseMigration(e.getMessage());
        }
    }


    public Map migrateApproaches(String customer,
                                 Integer connTimeout,
                                 Integer concurrentThreads,
                                 String thingsFilter) {
        try {
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
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        //        ThreadPool pool = new ThreadPool(connPerHost);
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);


        User rootUser = (User) SecurityUtils.getSubject().getPrincipal();

        logger.info("Loading cache data");
        Map<String, Map<String, Object>> cache = ThingService.getInstance().getCacheData();

        //New thread to write immediately to database and avoid session (hibernare, shiro, riot) timeout because
        // migration takes a lot of time
        cache = migrateDataTypes(customer, cache);

        logger.info("**** Starting Migration Process TEST 1 **** ");

        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        Date today = Calendar.getInstance().getTime();
        String dateString = df.format(today);


        ThingMongoDAO.getInstance().backupThingsCollecion("things_BKP_" + dateString);

        ThingMongoDAO.getInstance().dropThingsCollection();

        TimerUtil tu = new TimerUtil();
        tu.mark();

        ThingMongoDAO dao = ThingMongoDAO.getInstance();
        dao.dropThingSnapshotsCollection();
        dao.dropThingSnapshotIdsCollection();

        dao.createThingSnapshotsIndexes();

        int lastPercentLogged = 0, percent;


        @SuppressWarnings("ConstantConditions")
//        boolean saveDwellTime = ConfigurationService.getAsBoolean(rootUser, "saveDwellTimeHistory");
        Long cutoffTimeSeries = ConfigurationService.getAsLong(rootUser, "cutoffTimeseries");

        int counter = 0;
        int totalThings = ((Collection<Thing>) ((Map) cache.get("thingById")).values()).size();

        List<Long> thingsFilterList = createThingsFilters(thingsFilter);

        List<Long> failedThings = new ArrayList<>();

        logger.info("Total things in database :" + ((Collection<Thing>) ((Map) cache.get("thingById")).values()).size
                ());

        for (Thing thing : (Collection<Thing>) ((Map) cache.get("thingById")).values()) {

            if (thingsFilterList == null || thingsFilterList.contains(thing.getId())) {

                @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
                List<Long> thingTypeFieldIdsTimeSeries = new ArrayList<>();
                @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
                List<Long> thingTypeFieldIdsNoTimeSeries = new ArrayList<>();

                for (ThingTypeField tf : (Collection<ThingTypeField>) ((Map) cache.get("thingTypeField")).values()) {
                    if (tf.getTimeSeries()) {
                        thingTypeFieldIdsTimeSeries.add(tf.getId());
                    }
                    thingTypeFieldIdsNoTimeSeries.add(tf.getId());
                }


                Map<Long, List<Map<String, Object>>> fieldTypeTimeSeries = new HashMap<>();
                Map<Long, Map<String, Object>> fieldTypeNoTimeSeries = new HashMap<>();

                Map aux = CassandraCache.getInstance().getHistory(thing.getId());
                if (aux != null) {
                    fieldTypeTimeSeries.putAll(aux);
                }

                aux = CassandraCache.getInstance().getValue(thing.getId());
                if (aux != null) {
                    fieldTypeNoTimeSeries.putAll(aux);
                }

                RunnableMigration runnable = new RunnableMigration(thing,
                        cache,
                        true,
//                        saveDwellTime,
                        cutoffTimeSeries,
                        fieldTypeTimeSeries,
                        fieldTypeNoTimeSeries,
                        failedThings);
                executor.execute(runnable);
                counter++;
                percent = (counter * 100 / totalThings);
                logger.info("Migrated thing : "
                        + thing.getSerial()
                        + "% ("
                        + counter
                        + " things of "
                        + totalThings
                        + ")");
                if (percent != lastPercentLogged) {
                    logger.info("Migration progress: " + percent + "% (" + counter + " things of " + totalThings + ")");
                    lastPercentLogged = percent;
                }
            }
        }

        executor.shutdown();
        try {
            logger.info("Waiting for bulk threads finish default migration.");
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        tu.mark();

        int rate = (int) (counter / (tu.getLastDelt() / 1000.0));
        String msg = "Inserted "
                + counter
                + " things' thingSnapshots and thingSnapshotIds collections id in "
                + tu.getLastDelt()
                + " ms ("
                + rate
                + " things/sec)";
        Map<String, Object> response = new HashMap<>();
        response.put("Result", msg);
        response.put("failedThings", failedThings);
        logger.info(response);

        //        CassandraUtils.shutdown();
        return response;
    }

    //TODO private methods

    private void filterThingTypeTimeSeriesAndNoTimeSeries(Thing thing, List<Long> thingTypeFieldIdsTimeSeries, List<Long>
            thingTypeFieldIdsNoTimeSeries) {
        for (ThingTypeField thingTypeField : thing.getThingType().getThingTypeFields()) {
            if (thingTypeField.getTimeSeries()) {
                thingTypeFieldIdsTimeSeries.add(thingTypeField.getId());
            } else {
                thingTypeFieldIdsNoTimeSeries.add(thingTypeField.getId());
            }
        }
    }

    private List<Long> getAllThingTypesFieldsIds(Collection<ThingTypeField> thingTypeFieldCollection) {
        List<Long> thingTypeFieldIdsNoTimeSeries = new ArrayList<>();
        if (thingTypeFieldCollection != null) {
            for (ThingTypeField thingTypeField : thingTypeFieldCollection) {
                if (thingTypeField != null) {
                    thingTypeFieldIdsNoTimeSeries.add(thingTypeField.getId());
                }
            }
            return thingTypeFieldIdsNoTimeSeries;
        } else {
            return null;
        }
    }

    /**
     * A method to migrate Data Thing types.
     *
     * @param customer A customer to migrate Data Types.
     * @param cache    A cache to get MYSQL data types.
     * @return A new cache.
     */
    private Map<String, Map<String, Object>> migrateDataTypes(String customer, Map<String, Map<String, Object>> cache) {
        RunnableDataTypeMigration runnableDataTypeMigration = new RunnableDataTypeMigration(customer, cache);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(runnableDataTypeMigration);
        executorService.shutdown();
        try {
            logger.info("Migrate Data Types : Waiting for changes committed on cache.");
            executorService.awaitTermination(MAX_VALUE, NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return runnableDataTypeMigration.cache;
    }

    /**
     * Get mongo Backup if it is the first things migration.
     *
     * @param beginDate Initial date of the current migration.
     */
    private void createMongoBackUp(Date beginDate) throws Exception {
        DBObject mongoMigration = MongoMigrationDAO.getInstance().getLastMigrationStep();
        if (mongoMigration == null) {
            logger.info("**** Start Prepare Mongo DataBase with backup**** ");
            createBackUpMongoDB();
        } else {
            logger.info("A date of the last Step Migration is ; " + mongoMigration.get("endDate"));
            logger.info("**** Start Prepare Mongo DataBase without backup**** ");
            verifyEndDateLess(beginDate, (Date) mongoMigration.get("endDate"));
        }
    }

    /**
     * Create things collection backUp.
     */
    private void createBackUpMongoDB() {
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        Date today = Calendar.getInstance().getTime();
        logger.info("Create mongo things backup: " + "thingsBackUp" + df.format(today));
        ThingMongoDAO.getInstance().backupThingsCollecion("thingsBackUp" + df.format(today));
        ThingMongoDAO.getInstance().dropThingsCollection();
        ThingMongoDAO.getInstance().dropThingSnapshotsCollection();
        ThingMongoDAO.getInstance().dropThingSnapshotIdsCollection();
        ThingMongoDAO.getInstance().createThingSnapshotsIndexes();
    }

    /**
     * Verify completion date is less than the current date.
     *
     * @param beginDateCurrMig The initial date of current migration.
     * @param endDateLastMig   The end date of last migration.
     */
    private void verifyEndDateLess(Date beginDateCurrMig, Date endDateLastMig) throws Exception {
        if (!(beginDateCurrMig.after(endDateLastMig))) {
            throw new Exception("The end Date will have be after the last migration, the end date of the " +
                    "last migration is " + endDateLastMig);
        }
    }

    /**
     * Validate the start and end dates of migration.
     *
     * @param beginDate the begin date to get data.
     * @param endDate   the end date to get data.
     */
    private void valBeginAndEndDates(Date beginDate, Date endDate) throws Exception {

        if (beginDate == null || endDate == null) {
            throw new NullPointerException("Begin date or end date is null.");
        }
        beginDate = DateHelper.truncateDate(beginDate, "day");
        endDate = DateHelper.roundDate(endDate, "day");
        if (!beginDate.before(endDate)) {
            throw new Exception("The end date will have be after the begin date.");
        }
    }

    /**
     * Return message to Mongo DB Controller
     *
     * @param message A string to contains the message to return.
     * @return A Map with the message
     */
    private Map<String, Object> responseMigration(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("Result", message);
        logger.info(response);
        return response;
    }

    /**
     * Create a filter to get things.
     *
     * @param thingsFilter String contains the identifiers of one thing with the following format (123 123 123) or (123
     *                     123 123).
     * @return The list of Ids to migrate.
     */
    private List<Long> createThingsFilters(String thingsFilter) {
        List<Long> thingsFilterList = null;
        if (thingsFilter != null && !thingsFilter.isEmpty()) {
            thingsFilterList = new ArrayList<>();
            String[] thingIds;
            if (!thingsFilter.contains(",")) {
                thingIds = new String[]{thingsFilter};
            } else {
                thingIds = thingsFilter.split(",");
            }
            for (String id : thingIds) {
                thingsFilterList.add(Long.parseLong(id));
            }
        }
        return thingsFilterList;
    }
    /**
     * Reassociates a thing with parent, children and ThingTypeUDF
     *
     * @param thingTypeCode A code of the Thing Type.
     * @return A Map of the things after the reassociation.
     */
    public Map<String, Object> reassociateThingType(String thingTypeCode)
            throws NonUniqueResultException, MongoExecutionException {


        Map<String, Object> result = new LinkedHashMap<>();
        ThingType thingType = ThingTypeService.getInstance().getByCode(thingTypeCode);
        for (ThingType parent : thingType.getParents()){
            if (!result.containsKey(parent.getCode())) {
                result.putAll(reassociateThingType(parent.getCode()));
            }
        }
        for (ThingTypeField udf : thingType.getThingTypeFieldsByType(27L)){
            if (!result.containsKey(ThingTypeService.getInstance().get(udf.getDataTypeThingTypeId()).getCode())) {
                result.put(ThingTypeService.getInstance().get(udf.getDataTypeThingTypeId()).getCode(),
                        reassociateByThingTypeCode(ThingTypeService.getInstance().get(udf.getDataTypeThingTypeId()).getCode()));

            }
        }
        if (!result.containsKey(thingTypeCode)) {
            result.put(thingTypeCode, reassociateByThingTypeCode(thingTypeCode));
        }
        for (ThingType children : thingType.getChildren()){
            if (!result.containsKey(children.getCode())) {
                result.put(children.getCode(), reassociateByThingTypeCode(children.getCode()));
            }
        }
        return result;
    }

    private Map<Long, Object> reassociateByThingTypeCode(String thingTypeCode) throws MongoExecutionException {
        Map<Long, Object> result = new HashMap<>();
        List<Thing> things = ThingService.getThingDAO().selectAllBy(QThing.thing.thingType.thingTypeCode.eq(thingTypeCode));
        int percent, counter = 0, lastPercentLogged = 0;
        if(things != null && things.size() > 0){
            int totalThings=things.size();
            ThingService thingService = new ThingService();
            for(Thing thing : things){
                Map<String, Object> detail = new HashMap<>();
                //noinspection AccessStaticViaInstance
                if(ThingMongoDAO.getInstance().getThing(thing.getId()) != null){
                    //Associate parent
                    Thing thingParent = thing.getParent();
                    if(thingParent != null){
                        ThingMongoService.getInstance().associateChild(thingParent, thing.getId());
                        detail.put("parent", thingParent.getId());
                    }

                    //Associate children
                    //noinspection AccessStaticViaInstance
                    List<Thing> childrenList = thingService.getChildrenList(thing);
                    if(childrenList != null && childrenList.size() > 0){
                        List<Long> childrenIds = childrenList.stream().map(Thing::getId).collect(Collectors.toList());
                        detail.put("children", childrenIds);
                        ThingMongoService.getInstance().associateChildren(thing, childrenList);
                    }

                    //Associate thingTypeUDFs
                    Map<String, IThingField> udfList = thing.getThingFields();
                    if(udfList != null && udfList.size() > 0){
                        List<BasicDBObject> thingTypes = new ArrayList<>();
                        List<Long>thingTypeIds = new ArrayList<>();
                        for(String udf : udfList.keySet()){
                            if (((ThingTypeField)udfList.get(udf).getThingTypeField()).getDataType().getId() == 27L) {
                                thingTypes.add((BasicDBObject)udfList.get(udf).getValue());
                                thingTypeIds.add(((BasicDBObject)udfList.get(udf).getValue()).getLong("_id"));
                            }
                        }
                        if (thingTypes.size() > 0) {
                            detail.put("thingTypeUDFs", thingTypeIds);
                            ThingMongoService.getInstance().associateThingTypeUDF(thing, thingTypes);
                        }
                    }

                    counter ++;
                    percent = (counter * 100 / totalThings);
                    if (percent != lastPercentLogged) {
                        logger.info("Association progress: " + percent + "% (" + counter + " things of " + totalThings + ")");
                        lastPercentLogged = percent;
                    }
                }
                result.put(thing.getId(), detail);
            }
        }
        return result;
    }
}
