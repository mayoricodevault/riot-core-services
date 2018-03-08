package com.tierconnect.riot.iot.dao.mongo;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.mongodb.*;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.FavoriteService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.utils.TreeUtils;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingObject;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ValidationBean;
import com.tierconnect.riot.sdk.dao.MongoExecutionException;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.TimerUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.stream.Collectors;
public class ThingMongoDAO {
    static ThingMongoDAO instance;

    private static Logger logger = Logger.getLogger(ThingMongoDAO.class);

    private DBCollection thingsCollection;
    public static final String THINGS = "things";
    public static final String THING_SNAPSHOTS = "thingSnapshots";
    public static final String THING_SNAPSHOT_IDS = "thingSnapshotIds";

    static {
        instance = new ThingMongoDAO();
        instance.setup();
    }

    public void setup() {
        thingsCollection = MongoDAOUtil.getInstance().db.getCollection("things");
    }

    public enum comparator {$eq, $ne, $lt, $lte, $gt, $gte}

    public static comparator getComparator(String comparator) {
        switch (comparator) {
            case "=":
                return ThingMongoDAO.comparator.$eq;
            case "<>":
                return ThingMongoDAO.comparator.$ne;
            case "<":
                return ThingMongoDAO.comparator.$lt;
            case "<=":
                return ThingMongoDAO.comparator.$lte;
            case ">":
                return ThingMongoDAO.comparator.$gt;
            case ">=":
                return ThingMongoDAO.comparator.$gte;
            default:
                return ThingMongoDAO.comparator.$eq;
        }
    }

    public static ThingMongoDAO getInstance() {
        return instance;
    }

    /**
     * TODO new implementation of mongo DAO from here to down
     */

    /**
     * Inserts new mongo Thing Use this method to insert the whole thing in just one call to
     * database
     *
     * @param doc basicDBObject
     */
    public void insertThing(BasicDBObject doc)
            throws MongoExecutionException {
        try {
            //Insert MongoDB
            MongoDAOUtil.getInstance().things.insert(doc);

        } catch (Exception e) {
            throw new MongoExecutionException(e.getMessage(), e);
        }
    }


    /**
     * Update Thing in Mongo
     */
    public void updateThing(Thing thing, Map<String, Object> thingTypeFields, Date timestamp)
            throws MongoExecutionException {

    }


    /**
     * Creates a snapshot of thing from things
     */
    @Deprecated
    public void createSnapshot(Long thingId, Map<String, Object> fieldsChanged, Date time, User user) throws MongoExecutionException {

        DBCursor cursorId = MongoDAOUtil.getInstance().thingSnapshotIds.find(new BasicDBObject("_id", thingId));
        BasicDBObject docId = null;

        if (cursorId.hasNext()) {
            docId = (BasicDBObject) cursorId.next();
        }

        ObjectId existentThingId = null;


        if (docId != null) {

            for (Map<String, Object> timeStamp : (Collection<Map<String, Object>>) docId.get("blinks")) {
                if ((Long) timeStamp.get("time") == time.getTime()) {
                    existentThingId = (ObjectId) timeStamp.get("blink_id");
                    break;
                }
            }
        }

        //If exists time update snapshot
        if (existentThingId != null) {
            BasicDBObject doc = new BasicDBObject();
            if (fieldsChanged != null) {
                Iterator<Map.Entry<String, Object>> it = fieldsChanged.entrySet().iterator();

                while (it.hasNext()) {
                    Map.Entry<String, Object> current = it.next();
                    String tfkey = current.getKey();
                    HashMap<String, Object> tfvalue = (HashMap<String, Object>) current.getValue();

                    BasicDBObject tfvalues = new BasicDBObject("thingTypeFieldId", tfvalue.get("thingTypeFieldId"))
                            .append("time", time != null ? time : tfvalue.get("time"))
                            .append("value", tfvalue.get("value"))
                            .append("changed", true)
                            .append("blinked", true);

                    doc.append("value." + tfkey, tfvalues);
                }
            }


            BasicDBObject query = new BasicDBObject("_id", existentThingId);
            MongoDAOUtil.getInstance().thingSnapshots.findAndModify(query, null, null, false, new BasicDBObject("$set", doc), false, true);
            logger.debug("DOC UPDATE query: " + query.toString());
            logger.debug("STRING UPDATE:" + doc.toString());
        }
        //If not exists time create new snapshot
        else {
            ObjectId objectId = new ObjectId();

            DBObject thingDoc = getThing(thingId);

            for (String fieldChanged : fieldsChanged.keySet()) {
                if (((Map) fieldsChanged.get(fieldChanged)).get("value") != null) {
                    ((Map) thingDoc.get(fieldChanged)).put("changed", true);
                    ((Map) thingDoc.get(fieldChanged)).put("blinked", true);
                    ((Map) thingDoc.get(fieldChanged)).put("value", ((Map) fieldsChanged.get(fieldChanged)).get("value"));
                }
            }

            BasicDBObject doc = new BasicDBObject("_id", objectId).append("value", thingDoc).append("time", time);
            DBObject update = null;
            DBObject query = null;
            //Add dwellTime to new blink with value 0;
//            if (ConfigurationService.getAsBoolean(user, "saveDwellTimeHistory")) {
                for (Object field : ((Map) doc.get("value")).values()) {
                    if (field instanceof Map) {
                        ((Map) field).put("dwellTime", 0L);
                    }
                }

                ObjectId lastObjectId = null;
                DBObject lastDoc = null;

                if (docId != null) {

                    lastObjectId = (ObjectId) ((Map) ((List) docId.get("blinks")).get(0)).get("blink_id");
                    DBCursor cursorSnapshot = MongoDAOUtil.getInstance().thingSnapshots.find(new BasicDBObject("_id", lastObjectId));
                    if (cursorSnapshot.hasNext())
                        lastDoc = cursorSnapshot.next();
                    if (lastDoc != null) {
                        query = new BasicDBObject("_id", lastObjectId);

                        BasicDBObject fieldsUpdate = new BasicDBObject();
                        for (Map.Entry<String, Object> field : (Set<Map.Entry<String, Object>>) ((Map) lastDoc.get("value")).entrySet()) {
                            if (field.getValue() instanceof Map && !(field.getKey().equals("parent") || field.getKey().equals("children"))) {
                                Date fieldTime = ((Date) ((Map) field.getValue()).get("time"));
                                Long dwellTime = time.getTime() - fieldTime.getTime();
                                logger.info(field.getKey() + ": Long dwellTime = time.getTime() - fieldTime.getTime();   " + time.getTime() + " - " + fieldTime.getTime());

                                for (String fieldChanged : fieldsChanged.keySet()) {
                                    if (field.getKey().equals(fieldChanged)) {
                                        fieldsUpdate.append("value." + field.getKey() + ".dwellTime", dwellTime);//ojo
                                        break;
                                    } else {
                                        if (((Map) doc.get("value")).get(field.getKey()) != null) {
                                            Map mapField = (Map) ((Map) doc.get("value")).get(field.getKey());
                                            mapField.put("dwellTime", dwellTime);
                                        }
                                    }
                                }
                            }
                        }

                        update = new BasicDBObject("$set", fieldsUpdate);
                    }
                }
//            }

            //Save new snapshot collection
            logger.debug("DOC SAVED doc: " + doc.toString());
            MongoDAOUtil.getInstance().thingSnapshots.save(doc);

            //Update previous snapshot
            if (update != null && ((Map) ((Map) update).get("$set")).size() > 0) {
                logger.debug("DOC UPDATE query: " + query.toString());
                logger.debug("STRING UPDATE:" + update.toString());
                //Update the previous data
                MongoDAOUtil.getInstance().thingSnapshots.update(query, update);
            }

            pushThingSnapshotId(thingId, time, objectId, null, user, true);

        }


    }

    public BasicDBObject buildThingSnapshot(Thing thing,
                                            Map<String, Object> thingTypeFields,
                                            Date timestamp,
                                            ObjectId objectId) {
        GroupType groupType = thing.getGroup().getGroupType();

        BasicDBObject doc = new BasicDBObject("_id", thing.getId());
        doc.append("groupTypeId", groupType.getId());
        doc.append("groupTypeName", groupType.getName());
        doc.append("groupTypeCode", groupType.getCode() == null ? "" : groupType.getCode());
        doc.append("groupId", thing.getGroup().getId());
        doc.append("groupCode", thing.getGroup().getCode() == null ? "" : thing.getGroup().getCode());
        doc.append("groupName", thing.getGroup().getName());
        doc.append("thingTypeId", thing.getThingType().getId());
        doc.append("thingTypeCode", thing.getThingType().getThingTypeCode());
        doc.append("thingTypeName", thing.getThingType().getName());
        doc.append("name", thing.getName());
        doc.append("serialNumber", thing.getSerial());


        Iterator<Map.Entry<String, Object>> it = thingTypeFields.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> current = it.next();
            String tfkey = current.getKey();
            HashMap<String, Object> tfvalue = (HashMap<String, Object>) current.getValue();

            BasicDBObject tfValues = new BasicDBObject("thingTypeFieldId", tfvalue.get("thingTypeFieldId")).append
                    ("time", tfvalue.get("time") == null ? timestamp : tfvalue.get("time")).append("value", tfvalue
                    .get("value"));

            if (tfvalue.containsKey("dwellTime")) {
                tfValues = tfValues.append("dwellTime", tfvalue.get("dwellTime"));
            }
            if (tfvalue.containsKey("changed")) {
                tfValues = tfValues.append("changed", tfvalue.get("changed"));
            }
            if (tfvalue.containsKey("blinked")) {
                tfValues = tfValues.append("blinked", tfvalue.get("blinked"));
            }

            doc.append(tfkey, tfValues);
        }

        BasicDBObject docBase = new BasicDBObject("_id", objectId);
        docBase.append("time", timestamp);
        docBase.append("value", doc);
        return docBase;
    }

    public BasicDBObject buildThing(BasicDBObject doc,
                                    Thing thing,
                                    Map<String, Object> thingTypeFields,
                                    Date timestamp) {
        GroupType groupType = thing.getGroup().getGroupType();

        doc.append("groupTypeId", groupType.getId());
        doc.append("groupTypeName", groupType.getName());
        doc.append("groupTypeCode", groupType.getCode() == null ? "" : groupType.getCode());
        doc.append("groupId", thing.getGroup().getId());
        doc.append("groupCode", thing.getGroup().getCode() == null ? "" : thing.getGroup().getCode());
        doc.append("groupName", thing.getGroup().getName());
        doc.append("thingTypeId", thing.getThingType().getId());
        doc.append("thingTypeCode", thing.getThingType().getThingTypeCode());
        doc.append("thingTypeName", thing.getThingType().getName());
        doc.append("name", thing.getName());
        doc.append("serialNumber", thing.getSerial());

        if (thingTypeFields != null) {
            for (Map.Entry<String, Object> current : thingTypeFields.entrySet()) {
                HashMap<String, Object> tfvalue = (HashMap<String, Object>) current.getValue();
                doc.append(current.getKey(), new BasicDBObject("thingTypeFieldId", tfvalue.get("thingTypeFieldId"))
                        .append("time", timestamp != null ? timestamp : tfvalue.get("time"))
                        .append("value", tfvalue.get("value")));
            }
        }
        return doc;
    }

    public BasicDBObject buildUpsertThing(Thing thing, Map<String, Object> thingTypeFields, Date timestamp) {

        BasicDBObject doc = new BasicDBObject("_id", thing.getId());
        return new BasicDBObject("$set", buildThing(doc, thing, thingTypeFields, timestamp));
    }

    public BasicDBObject buildThingSnapshotIds(List<Map<String, Object>> times) {
        BasicDBList timeSeriesIdList = new BasicDBList();

        for (Map<String, Object> time : times) {
            timeSeriesIdList.add(time);
        }
        BasicDBObject indexDoc = new BasicDBObject("$each", timeSeriesIdList).append("$position", 0);
        BasicDBObject updateDoc = new BasicDBObject("$push", new BasicDBObject("blinks", indexDoc));
        return updateDoc;
    }

    /**
     * Inserts snapshot of thing
     */
    public ObjectId insertThingSnapshot(Thing thing,
                                        Map<String, Object> thingTypeFields,
                                        Date timestamp,
                                        ObjectId objectId) throws MongoExecutionException {
        try {
            ObjectId objectIdSave;

            if (objectId == null) {
                objectIdSave = new ObjectId();
            } else {
                objectIdSave = objectId;
            }

            MongoDAOUtil.getInstance().thingSnapshots.save(buildThingSnapshot(thing,
                    thingTypeFields,
                    timestamp,
                    objectIdSave));
            return objectIdSave;
        } catch (Exception e) {
            throw new MongoExecutionException(e.getMessage(), e);
        }
    }

    public void insertThingSnapshot(BasicDBObject thingSnapshotMongo) throws MongoExecutionException {
        try {
            MongoDAOUtil.getInstance().thingSnapshots.save(thingSnapshotMongo);
        } catch (Exception e) {
            throw new MongoExecutionException(e.getMessage(), e);
        }
    }


    public void insertThingSnapshotIds(BasicDBObject thingSnapshotIdMongo) throws MongoExecutionException {
        try {
            MongoDAOUtil.getInstance().thingSnapshotIds.save(thingSnapshotIdMongo);
        } catch (Exception e) {
            throw new MongoExecutionException(e.getMessage(), e);
        }
    }

    public void pushThingSnapshotId(Long thingId, Date time, ObjectId objectId, DBObject snapshot, User user, boolean isOlderSnapshot) throws
            MongoExecutionException {
        try {
            if (time != null) {
                BasicDBObject queryDoc = new BasicDBObject("_id", thingId);

                BasicDBObject element = new BasicDBObject("time", time.getTime()).append("blink_id", objectId);

                BasicDBList elements = new BasicDBList();
                elements.add(element);

                BasicDBObject indexDoc = new BasicDBObject("$each", elements);
                if(isOlderSnapshot){
                    indexDoc.append("$slice", 1000)
                            .append("$sort", new BasicDBObject("time", -1));
                }else{
                    indexDoc.append("$position", 0);
                }
                BasicDBObject updateDoc = new BasicDBObject("$push", new BasicDBObject("blinks", indexDoc));
                BasicDBObject nonUDF = new BasicDBObject();
                nonUDF.append("thingTypeId", snapshot.get("thingTypeId")).append("thingTypeCode", snapshot.get("thingTypeCode"))
                        .append("groupId", snapshot.get("groupId")).append("groupCode", snapshot.get("groupCode"))
                        .append("groupTypeId", snapshot.get("groupTypeId")).append("groupTypeCode", snapshot.get("groupTypeCode"));
                updateDoc.append("$set", nonUDF);

                WriteResult wrIndex = MongoDAOUtil.getInstance().thingSnapshotIds.update(queryDoc,
                        updateDoc, true, false, WriteConcern.ACKNOWLEDGED);

                // VIZIX-4347 verify that the configuration cutoff Time series is working.
                verifiedCutoff(wrIndex, thingId, user, queryDoc);
            }
        } catch (Exception e) {
            throw new MongoExecutionException(e.getMessage(),e);
        }
    }

    /**
     * Deletes a thing document from mongo
     *
     * @param id thing is to be deleted
     */
//	public Map<String, Object> deleteThing(Long id, String serialNumber) throws MongoExecutionException {
//		Map<String, Object> result = new HashMap<>();
//
//
//		try{
//			BasicDBObject document = new BasicDBObject();
//			document.append("_id", id).append("serialNumber", serialNumber);
//			WriteResult wr =  MongoDAOUtil.getInstance().things.remove(document);
//			result.put("things", wr.getN());
//
//			BasicDBObject queryIds = new BasicDBObject("_id", id);
//
//			DBCursor cursor = MongoDAOUtil.getInstance().thingSnapshotIds.find(queryIds);
//			DBObject docIds = null;
//			if(cursor.hasNext())
//				docIds = cursor.next();
//
//			List<ObjectId> objectIds = new ArrayList<>();
//			if(docIds != null){
//				for(Map<String, Object> blink : (List<Map>)docIds.get("blinks")){
//					objectIds.add((ObjectId)blink.get("blink_id"));
//				}
//				wr = MongoDAOUtil.getInstance().thingSnapshots.remove(
//						new BasicDBObject("_id", new BasicDBObject("$in", objectIds)));
//
//				result.put("thingSnapshots", wr.getN());
//				wr =  MongoDAOUtil.getInstance().thingSnapshotIds.remove(queryIds);
//				result.put("thingSnapshotIds", wr.getN());
//			}else{
//				result.put("thingSnapshots", 0);
//				wr =  MongoDAOUtil.getInstance().thingSnapshotIds.remove(queryIds);
//				result.put("thingSnapshotIds", 0);
//			}
//		}catch (Exception e){
//			throw new MongoExecutionException(e.getMessage());
//		}
//		return result;
//	}


    /**
     * TODO Review lastQueryLength to improve register counter
     */
    public static int lastQueryLength = 0;

    /**
     * Retrieves field history
     */
    public List<Map<String, Object>> getFieldHistoryWithPaging(long thingId,
                                                               String thingTypeFieldName,
                                                               Date startDate,
                                                               Date endDate,
                                                               Integer pageSize,
                                                               Integer pageIndex,
                                                               boolean ascending) {


        BasicDBObject query = new BasicDBObject("value._id", thingId);
        BasicDBObject checkBlinked = new BasicDBObject("value." + thingTypeFieldName + ".blinked", new BasicDBObject("$exists", true));
        checkBlinked.append("value." + thingTypeFieldName + ".blinked", true);
        BasicDBObject checkChanged = new BasicDBObject("value." + thingTypeFieldName + ".blinked", new BasicDBObject("$exists", false));
        checkChanged.append("value." + thingTypeFieldName + ".changed", true);
        BasicDBList checkHistory = new BasicDBList();
        checkHistory.add(checkBlinked);
        checkHistory.add(checkChanged);
        query = query.append("$or", checkHistory);

        //Date Filtering
        if (startDate != null && endDate != null) {
            BasicDBObject dateTimeFiler = new BasicDBObject();
            dateTimeFiler = dateTimeFiler.append("$gte", startDate);
            dateTimeFiler = dateTimeFiler.append("$lte", endDate);
            query = query.append("time", dateTimeFiler);
        } else if (startDate != null) {
            query = query.append("time", new BasicDBObject("$gte", startDate));
        } else if (endDate != null) {
            query = query.append("time", new BasicDBObject("$lte", endDate));
        }

        DBCursor cursor = MongoDAOUtil.getInstance().thingSnapshots.find(query);


        //Sorting
        if (ascending) {
            BasicDBObject sort = new BasicDBObject("time", 1);
            cursor.sort(sort);
        } else {
            BasicDBObject sort = new BasicDBObject("time", -1);
            cursor.sort(sort);
        }

        //Pagination
        if (pageIndex != null && pageSize != null && pageSize != -1) {
            //add pagination
            Integer skip = (pageIndex >= 1) ? (pageIndex - 1) * pageSize : 0;
            logger.info("page " + pageIndex + ", skip " + skip + ", size " + pageSize);
            cursor.skip(skip);
            cursor.limit(pageSize);
        }
        //limit if we don't get a default page size
        else if (pageSize == null) {
            //add pagination
            User user = (User) SecurityUtils.getSubject().getPrincipal();
            Integer limit = Integer.valueOf(ConfigurationService.getAsString(user, "maxReportRecords"));
            cursor.limit(limit);
            logger.warn("limit response to " + limit + ". page size = null");
        }


        List<Map<String, Object>> historyValues = new ArrayList<>();

        lastQueryLength = cursor.count();

        //Loop cursor Results
        while (cursor.hasNext()) {
            Map<String, Object> historyValue = new HashMap<>();

            BasicDBObject doc = (BasicDBObject) cursor.next();

            Object value = ((BasicDBObject) ((BasicDBObject) doc.get("value")).get(thingTypeFieldName)).get("value");
            Date time = (Date) doc.get("time");

            historyValue.put("value", value);
            historyValue.put("time", time);

            historyValues.add(historyValue);
        }

        return historyValues;
    }

    /**
     * Get a specific time snapshot by id
     */
    public BasicDBObject getThingSnapshotById(ObjectId id) {

        BasicDBObject result = null;
        DBCursor cursorIds = MongoDAOUtil.getInstance().thingSnapshots.find(new BasicDBObject("_id", id));
        if (cursorIds.hasNext()) {

            result = (BasicDBObject) cursorIds.next();
        }
        return result;
    }

    /**
     * Get a specific time snapshot
     */
    public static BasicDBObject getThingSnapshotByThingIdAndTime(Long thingId, Long time, comparator comp) {


//		db.getCollection("thingSnapshotIds").find(
        // 		{blinks:{$elemMatch:{time:{$eq:1439926853073}}}},
//		{blinks:{$elemMatch:{time:{$eq:1439926853073}}}}
// 		)
        BasicDBObject queryIds = new BasicDBObject("blinks", new BasicDBObject("$elemMatch", new BasicDBObject("time", new BasicDBObject(comp.toString(), time))))
                .append("_id", thingId);


        BasicDBObject findIds = new BasicDBObject("blinks", new BasicDBObject("$elemMatch", new BasicDBObject("time", new BasicDBObject(comp.toString(), time))))
                .append("_id", 0)
                .append("blinks.time", 0);

        logger.info("QUERY get Thing Snapshot By ThingId And Time, get Ids:" + queryIds.toString());
        DBCursor cursorIds = MongoDAOUtil.getInstance().thingSnapshotIds.find(queryIds, findIds);

        BasicDBObject resultIds = new BasicDBObject();
        BasicDBObject result = new BasicDBObject();
        if (cursorIds.hasNext()) {

            resultIds = (BasicDBObject) cursorIds.next();

            ObjectId id = (ObjectId) ((Map) ((List) resultIds.get("blinks")).get(0)).get("blink_id");

            BasicDBObject query = new BasicDBObject("_id", id);
            logger.info("QUERY get Thing Snapshot By ThingId And Time:" + query.toString());
            DBCursor cursor = MongoDAOUtil.getInstance().thingSnapshots.find(query);

            if (cursor.hasNext()) {
                result = (BasicDBObject) cursor.next();
            }else {
                return null;
            }
        }

        return result;
    }

    public static DBObject getPreviousSnapshotByTime( Long thingId, Long timestamp){

        try {
            BasicDBObject query = new BasicDBObject("value._id", thingId).append("time", new BasicDBObject("$lt",
                    new Date(timestamp)));
            BasicDBObject sort = new BasicDBObject("time", -1);
            DBCursor previousDocCursor = MongoDAOUtil.getInstance().thingSnapshots.find(query).sort(sort).limit(1);
            if (previousDocCursor.hasNext()) {
                DBObject result = (DBObject) previousDocCursor.next();
                return (DBObject) result.get("value");
            } else {
                return (new BasicDBObject()).append("_id", thingId);
            }
        }catch (MongoTimeoutException e){
            if(e.getMessage().contains("com.mongodb.MongoSocketOpenException")){
                logger.error("Mongo connection failed, host and/or port are closed");
            }
            throw e;
        }
    }

    public DBObject getSnapshotCompleteByThingIdAndTime(Long thingId, Date time) {
        try {
            DBObject result = null;
            BasicDBObject query = new BasicDBObject("value._id", thingId);
            query.append("time", new BasicDBObject("$lt", time));

            DBCursor snapshot = MongoDAOUtil.getInstance().thingSnapshots
                    .find(query).sort(new BasicDBObject("time", -1)).limit(1);
            if (snapshot.hasNext()) {
                result = snapshot.next();
            }
            return result;
        } catch (MongoTimeoutException e) {
            if (e.getMessage().contains("com.mongodb.MongoSocketOpenException")) {
                logger.error("Mongo connection failed, host and/or port are closed");
            }
            throw e;
        }
    }

    public DBObject getThingOfPreviousSnaphostByObjectIDAndTime(Long thingId, ObjectId objectId, Date time) {
        try {
            DBObject result = null;
            BasicDBObject query = new BasicDBObject("value._id", thingId);
            query.append("_id", new BasicDBObject("$ne", objectId));
            query.append("time", new BasicDBObject("$lt", time));
            DBCursor snapshot = MongoDAOUtil.getInstance().thingSnapshots
                    .find(query).sort(new BasicDBObject("time", -1)).limit(1);
            if (snapshot.hasNext()) {
                result = (DBObject) ((DBObject) snapshot.next()).get("value");
            }
            return result;
        } catch (MongoTimeoutException e) {
            if (e.getMessage().contains("com.mongodb.MongoSocketOpenException")) {
                logger.error("Mongo connection failed, host and/or port are closed");
            }
            throw e;
        }
    }

    public void updateThingSnapshotId(Long thingId, Date oldTime, Date newTime, ObjectId objectId, User user)
            throws MongoExecutionException {
        try {
            if (oldTime != null) {
                BasicDBObject queryDoc = new BasicDBObject("_id", thingId);
                queryDoc.append("blinks.blink_id", objectId);
                queryDoc.append("blinks.time", oldTime.getTime());

                BasicDBObject setValue = new BasicDBObject("blinks.$.time", newTime.getTime());
                WriteResult wrIndex = MongoDAOUtil.getInstance().thingSnapshotIds.update(queryDoc,
                        new BasicDBObject("$set", setValue), false, false, WriteConcern.ACKNOWLEDGED);
                verifiedCutoff(wrIndex, thingId, user, queryDoc);
            }
        } catch (Exception e) {
            throw new MongoExecutionException(e.getMessage(), e);
        }
    }

    private static void verifiedCutoff(WriteResult wrIndex, Long thingId, User user, BasicDBObject queryDoc) {
        // VIZIX-4347 verify that the configuration cutoff Time series is working.
        if (wrIndex.isUpdateOfExisting()) {
            DBCursor cursor = MongoDAOUtil.getInstance().thingSnapshotIds.find(
                    new BasicDBObject("_id", thingId));
            if (cursor.hasNext()) {
                DBObject doc = cursor.next();
                if (((List) doc.get("blinks")).size() >= ConfigurationService.getAsLong(user, "cutoffTimeseries")) {
                    MongoDAOUtil.getInstance().thingSnapshotIds.update(queryDoc,
                            new BasicDBObject("$pop", new BasicDBObject("blinks", 1)));
                }
            }
        }
    }

    public List<DBObject> getAllThingSnapshotByThingId(Long thingId) {

        BasicDBObject queryIds = new BasicDBObject("_id", thingId);


        DBCursor cursorIds = MongoDAOUtil.getInstance().thingSnapshotIds.find(queryIds);

        BasicDBObject resultIds = new BasicDBObject();
        List<DBObject> result = new ArrayList<>();
        if (cursorIds.hasNext()) {

            resultIds = (BasicDBObject) cursorIds.next();

            BasicDBList ids = (BasicDBList) resultIds.get("blinks");

            List<ObjectId> idsList = new ArrayList<>();

            for (Object blink : ids) {
                idsList.add((ObjectId) ((Map) blink).get("blink_id"));
            }

            BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$in", idsList));

            DBCursor cursor = MongoDAOUtil.getInstance().thingSnapshots.find(query);

            while (cursor.hasNext()) {
                result.add(cursor.next());
            }
        }

        return result;
    }

//    public List<DBObject> getAllThingSparseByThingId(Long thingId) {
//
//        BasicDBObject queryIds = new BasicDBObject("_id.id", thingId);
//
//        DBCursor cursorIds = MongoDAOUtil.getInstance().timeseriesCollection.find(queryIds);
//
//        List<DBObject> result = new ArrayList<>();
//        while (cursorIds.hasNext()) {
//            result.add(cursorIds.next());
//        }
//        return result;
//    }

    public Map<String, Object> getThingSnapshotByObjectId(ObjectId id) {

        BasicDBObject query = new BasicDBObject("_id", id);
        DBCursor cursor = MongoDAOUtil.getInstance().thingSnapshots.find(query);
        Map<String, Object> result = new HashMap<>();
        if (cursor.hasNext()) {
            result = (Map<String, Object>) cursor.next();
        }
        return (Map) result.get("value");
    }

    /**
     * Get udf values of things by specific fields
     *
     * @param whereThing      accepts & | = <> operators
     * @param whereFieldValue accepts & | = <> operators. Use the string ".value" at the end of UDF
     *                        name
     * @param filterFields    UDFs that are returned, accepts * for all UDFs
     * @param groupByFields   UDFs for groupedBy clause. If groupedByFields is not null, result
     *                        won't consider filterFields.
     *
     *                        whereThing > "thingTypeCode=sharaf.rfid" automatically parses to
     *                        number  ie _id=123" to avoid parsing to number add single quotes, ie
     *                        serialNumber='123'" whereFieldValue > "status.value=GRN&IsAlreadyBuzzed.value=false"
     *                        filterFields > "status,IsAlreadyBuzzed" groupedByFields > "zone"
     */
    public Map<String, Object> getThingUdfValues(String whereThing, String whereFieldValue, List<String>
            filterFields, List<String> groupByFields) {

        Map<String, Object> mapResponse = new HashMap<String, Object>();

        List<Map<String, Object>> result = listThingsByFieldValues(filterFields, groupByFields, whereFieldValue, whereThing);

        //put in map format
        mapResponse.put("total", result.size());
        mapResponse.put("results", result);

        return mapResponse;
    }

    public List<Map<String, Object>> listThingsByFieldValues(List<String> filterFields, List<String> groupByFields, String whereFieldValue, String whereThing) {
        List<Map<String, Object>> result = new ArrayList<>();

        // construct condition
        DBObject condition = null;
        if (StringUtils.isNotEmpty(whereThing) && StringUtils.isNotEmpty(whereFieldValue)) {
            condition = ThingMongoDAO.getInstance().getObjectCondition(
                    ThingMongoDAO.getInstance().parseWhereString(whereThing),
                    ThingMongoDAO.getInstance().parseWhereString(whereFieldValue), "$and");
        } else if (StringUtils.isNotEmpty(whereThing)) {
            condition = ThingMongoDAO.getInstance().parseWhereString(whereThing);
        } else if (StringUtils.isNotEmpty(whereFieldValue)) {
            condition = ThingMongoDAO.getInstance().parseWhereString(whereFieldValue);
        }

        if (filterFields != null && !filterFields.isEmpty()) {
            Map<String, Object> lista = ThingMongoDAO.getInstance().executeQuery(
                    condition,
                    filterFields,
                    null,
                    null,
                    null,
                    false,
                    false);
            result = ((List<Map<String, Object>>) lista.get("list"));
        } else {
            if (groupByFields != null && !groupByFields.isEmpty())
                result = ThingMongoDAO.getInstance().executeGroupedByQuery(condition, groupByFields, null, null);
        }
        return result;
    }

    public int updateThingsByFieldValues(String whereFieldValue, String whereThing, Map<String, Object> fieldValuesMap) {
        // construct condition
        DBObject condition = null;
        if (StringUtils.isNotEmpty(whereThing) && StringUtils.isNotEmpty(whereFieldValue)) {
            condition = ThingMongoDAO.getInstance().getObjectCondition(
                    ThingMongoDAO.getInstance().parseWhereString(whereThing),
                    ThingMongoDAO.getInstance().parseWhereString(whereFieldValue), "$and");
        } else if (StringUtils.isNotEmpty(whereThing)) {
            condition = ThingMongoDAO.getInstance().parseWhereString(whereThing);
        } else if (StringUtils.isNotEmpty(whereFieldValue)) {
            condition = ThingMongoDAO.getInstance().parseWhereString(whereFieldValue);
        }

        DBObject update = null;
        BasicDBObject fieldsUpdate = new BasicDBObject();
        for (Map.Entry<String, Object> entry : fieldValuesMap.entrySet())
            fieldsUpdate.append(entry.getKey(), entry.getValue());
        update = new BasicDBObject("$set", fieldsUpdate);
        WriteResult mongoResult = MongoDAOUtil.getInstance().things.update(condition, update, false, true, WriteConcern.ACKNOWLEDGED);
        return mongoResult.getN();
    }

    public List<Map<String, Object>> getThingChildren(Long id) {

        BasicDBObject query = new BasicDBObject("_id", id);


        List<Map<String, Object>> children = new ArrayList<>();

        DBCursor result = MongoDAOUtil.getInstance().things.find(query);

        while (result.hasNext()) {
            DBObject doc = result.next();

            if (doc.get("children") instanceof BasicDBList) {
                BasicDBList items = (BasicDBList) doc.get("children");
                for (Object item : items) {
                    children.add((Map<String, Object>) item);
                }
            }
        }

        return children;

    }

    public Long countTrueFlags(List<String> fields, List<Long> listThingsID) {
        Long resultCursor = 0L;
        if (!fields.isEmpty()) {
            BasicDBList query = fields.stream()
                    .map(field -> new BasicDBObject(field + ".value", true))
                    .collect(Collectors.toCollection(BasicDBList::new));
            BasicDBObject listThingID = new BasicDBObject("_id", new BasicDBObject("$in", listThingsID));
            BasicDBObject orListFields = new BasicDBObject("$or", query);
            BasicDBList queryList = new BasicDBList();
            queryList.add(listThingID);
            queryList.add(orListFields);
            try {
                BasicDBObject $and = new BasicDBObject("$and", queryList);
                logger.info($and);
                resultCursor = MongoDAOUtil.getInstance().things.count($and);
            } catch (MongoTimeoutException e) {
                logger.error("Mongo timeout exception", e);
                throw new RuntimeException("Connection refused, Mongo timeout", e);
            }
        }
        return resultCursor;
    }

    private DBObject parseWhereStringComparison(String operator, String operand1, String operand2)
    {
        BasicDBObject query = new BasicDBObject();
        if (operator.equals("=")) {
            if (operand1.matches("(.*)_id(.*)")) {
                query.append(operand1, Integer.parseInt(operand2.replaceAll("^'|'$", "")));
            } else if (operand2.matches("^-?\\d+$") && !operand2.startsWith("'") && !operand2.endsWith("'")) {
                query.append(operand1, Long.parseLong(operand2.replaceAll("^\'|\'$", "")));
            } else if (operand2.equals("true") || operand2.equals("false")) {
                query.append(operand1, Boolean.parseBoolean(operand2));
            } else if (operand2.startsWith("/") && operand2.endsWith("/")) {
                if(operand1.equals("serialNumber")) {
                    query.append(operand1,
                            new BasicDBObject("$regex", java.util.regex.Pattern.compile(operand2.replace("/", ""))));
                } else {
                    query.append(operand1,
                            new BasicDBObject("$regex", java.util.regex.Pattern.compile(operand2.replace("/", ""))).append("$options", "i"));
                }
            } else {
                query.append(operand1, operand2.replaceAll("^'|'$", ""));
            }
        } else if (operator.equals("<>")) {
            if (operand1.matches("(.*)_id(.*)")) {
                query.append(operand1, new BasicDBObject("$ne", Integer.parseInt(operand2.replaceAll("^'|'$", ""))));
            } else if (operand2.matches("^-?\\d+$") && !operand2.startsWith("'") && !operand2.endsWith("'")) {
                query.append(operand1, new BasicDBObject("$ne", Long.parseLong(operand2.replaceAll("^\'|\'$", ""))));
            } else if (operand2.equals("true") || operand2.equals("false")) {
                query.append(operand1, Boolean.parseBoolean(operand2));
            } else if (operand2.startsWith("/") && operand2.endsWith("/")) {
                if(operand1.equals("serialNumber")) {
                    query.append(operand1,
                            new BasicDBObject("$regex", java.util.regex.Pattern.compile(operand2.replace("/", ""))));
                } else {
                    query.append(operand1,
                            new BasicDBObject("$regex", java.util.regex.Pattern.compile(operand2.replace("/", ""))).append("$options", "i"));
                }
            } else {
                query.append(operand1, new BasicDBObject("$ne", operand2.replaceAll("^'|'$", "")));
            }
        }else if (operator.equals("$in") ||  operator.equals("$nin")) {
            if ((!operand2.startsWith("[") && !operand2.endsWith("]")) || operand2.contains(">") || operand2.contains("|") || operand2.contains("&")|| operand2.contains("$in")|| operand2.contains("$nin")|| operand2.contains("<")){
                throw new UserException("Invalid characters found in specified array for $in/$nin operator");
            }
            operand2 = operand2.replace("[","").replace("]","");
            BasicDBList listValues = new BasicDBList();
            List temporalList = Arrays.asList(operand2.split(","));
            for (Object temp:temporalList) {
                String value = temp.toString();
                if(value.contains("'")){
                        value = value.replace("'", "");
                        listValues.add(value);
                }else {
                    if ((value.charAt(0) == '-' && (value.charAt(1) >= '0' && value.charAt(1) <= '9')) || ((value.charAt(0) >= '0' && value.charAt(0) <= '9'))) {
                        listValues.add(Long.parseLong(value));
                    }else{
                        if (value.toLowerCase().equals("false") || value.toLowerCase().equals("true")) {
                            listValues.add(Boolean.valueOf(value));
                        }else{
                            if (value.startsWith("/") && value.endsWith("/") && value.length() > 2){
                                listValues.add(java.util.regex.Pattern.compile(value.replaceFirst("/", "(?)").replace("/","(.*)")));
                            }else {
                                listValues.add(value);
                            }
                        }
                    }
                }
            }
            DBObject inClause = new BasicDBObject(operator,listValues);
            query.append(operand1,inClause);
        }
        return query;
    }

    private List<String> parseWhereStringTokenize(String where) {
        // Get a list of tokens from the where string:
        List<String> result = new ArrayList<>();
        char opSymbol = '\0'; //need to consider that strings can appear between ' ' and regexes between / /
        String t = "";
        int i = 0;
        while (where != null && i < where.length() ) {
            char ch = where.charAt(i);
            if (opSymbol != '\0') {
                t = t + ch;
                if (opSymbol == ch) {
                    opSymbol = '\0';
                    result.add(t);
                    t = "";
                }
            } else if ( //separator characters:
                    (ch=='$') || (ch=='(') || (ch == ')') || (ch == '=') || (ch == '&') || (ch == '|') || (ch == '[')
                            || (ch == ']') || (ch == '{') || (ch=='}') || (ch=='\'') || (ch == '/') || (ch == '\\')
                            || (ch == '<') || (ch == '>') || (ch == ' ') || (ch == '\t')
                    ) {
                if (! t.isEmpty()) {
                    result.add(t);
                }
                t = "";
                if ( (ch == '<') && (i+1 < where.length()) && (where.charAt(i+1) == '>') ) {
                    result.add("<>"); // This special operator takes two characters.
                    i++;
                } else if ( (ch == '/') || (ch == '\'') ) { //Opening character for a regex or string:
                    t = "" + ch;
                    opSymbol = ch;
                } else if ( (ch == '$') && (i+1 < where.length()) && (where.charAt(i+1) == 'i') ){
                    result.add("$in");
                    i = i + 2;
                }else if ( (ch == '$') && (i+1 < where.length()) && (where.charAt(i+1) == 'n') ){
                    result.add("$nin");
                    i = i + 3;
                }else if ( (ch == '[') && (where.substring(i+1).contains("]")) ){
                    String array=where.substring(i,i+where.substring(i).indexOf("]")+1);
                    if (array != null){
                        result.add(array);
                        i = i + array.length()-1;
                    }
                }else if ( (ch != ' ') && (ch != '\t') ) { //Ignore white space
                    result.add("" + ch); //Insert operator as a token.
                }
            } else {
                t = t + ch;
            }
            i++;

        }
        if (! t.isEmpty()) {
            result.add(t);
        }
        return result;
    }

    private List<String> parseWhereStringInfixToPostfix(List<String> infixTokens) {
        // Parse the string, changes it from infix notation (and parenthesis) to postfix notation:
        List<String> postfixTokens = new ArrayList<>();
        Deque<String> opStack = new LinkedList<>();

        Map<String, Integer> operatorPrecedence = new TreeMap<>();

        // Allowed operators in order of precedence:
        final String[] operators = { "=", "<>","$in","$nin", "|" , "&", "(" };
        for (int i = 0; i < operators.length; i++) {
            operatorPrecedence.put(operators[i],i);
        }

        for (String token: infixTokens) {
            char ch = token.charAt(0);
            if (ch == '(') {
                opStack.push("(");
            } else if (ch == ')') {
                // Pop all operators until we find the matching "("
                while (! opStack.isEmpty()) {
                    String operator = opStack.pop();
                    if (operator.equals("(")) {
                        break;
                    } else {
                        postfixTokens.add(operator);
                    }
                }
            } else if ( (ch == '<') || (ch == '$') || (operatorPrecedence.containsKey("" + ch)) ) {
                String currentOp = "" + ch;
                if (ch == '<') {
                    currentOp = "<>";
                }
                if (ch == '$' && token.charAt(1) == 'i'){
                    currentOp = "$in";
                } else if (ch == '$' && token.charAt(1) == 'n'){
                    currentOp = "$nin";
                }
                // pop all operators with higher precedence:
                int currentPrecedence = operatorPrecedence.get(currentOp);
                while (! opStack.isEmpty()) {
                    String operator = opStack.peekFirst();
                    if ( operatorPrecedence.get(operator) <= currentPrecedence ) {
                        postfixTokens.add(operator);
                        opStack.pop();
                    } else {
                        break;
                    }
                }
                opStack.push(currentOp);
            } else {
                postfixTokens.add(token);
            }
        }
        // Pop any remaining operator:
        while (! opStack.isEmpty()) {
            postfixTokens.add(opStack.pop());
        }

        return postfixTokens;
    }

    public DBObject parseWhereString(String where) {
        // Parser entry point.
        // Operator precedence: bracket -> and -> or -> comparison
        DBObject result = new BasicDBObject();
        if(where != null){
            if (where.contains("serialNumber=/") ) {
                int indexSerialNumber = where.indexOf("serialNumber=/") +14;
                String subString = where.substring(indexSerialNumber, where.length());
                String serialNumber = where.substring(indexSerialNumber, indexSerialNumber+(subString.indexOf("/")));
                where = where.replace(serialNumber, serialNumber.toUpperCase());
            }
            List<String> tokens = parseWhereStringTokenize(where);
            if (tokens.size() != 0) {
                tokens = parseWhereStringInfixToPostfix(tokens);
                Deque<DBObject> stack = new LinkedList<>();
                String previous0 = "", previous1 = "";
                for (String token: tokens) {
                    if (token.equals("=") || (token.equals("<>")) || (token.equals("$in")) || (token.equals("$nin"))) {
                        DBObject dbo = parseWhereStringComparison(token, previous1, previous0);
                        stack.push(dbo);
                    } else if (token.equals("|") || token.equals("&")) {
                        if (stack.size() < 2) {
                            char ch = where.charAt(0);
                            String message = "There was a problem processing the filter string: '" + where + "'. ";
                            if ( (ch == '|') || (ch == '&') ) {
                                message = message + "The filter string cannot start with: '" + token + "'.";
                            } else {
                                ch = where.charAt(where.length() - 1);
                                if ((ch == '|') || (ch == '&')) {
                                    message = message + "The filter string cannot end with: '" + token + "'.";
                                } else {
                                    message = message + "Syntax error near: '" + token + "'.";
                                }
                            }
                            throw new UserException(message);
                        } else {
                            DBObject dbo2 = stack.pop();
                            DBObject dbo1 = stack.pop();
                            BasicDBList db_op = new BasicDBList();
                            db_op.add(dbo1);
                            db_op.add(dbo2);
                            DBObject queryDBO = null;
                            queryDBO = new BasicDBObject((token.equals("|") ? "$or" : "$and"), db_op);
                            stack.push(queryDBO);
                        }
                    }
                    previous1 = previous0;
                    if(previous1.equals("serialNumber")) {
                        token = token.toUpperCase();
                    }
                    previous0 = token;
                }
                result = stack.pop();
            }
        }
        return result;
    }

    public DBObject getObjectCondition(DBObject clause1, DBObject clause2, String operator) {
        if (null == operator || operator.isEmpty() || null == clause1 || null == clause2)
            return null;
        BasicDBList list = new BasicDBList();
        list.add(clause1);
        list.add(clause2);
        DBObject clauseResult = new BasicDBObject(operator, list);
        return clauseResult;
    }

    /**
     * This query gets a list of elements of a specific field
     * @param fieldName
     * @param fieldIdLabel
     * @param thingTypeId
     * @param thingTypeFieldId
     * @return
     */
    public List<Object> getFieldDistinctValues(
            String fieldName,
            String fieldIdLabel,
            Long thingTypeId,
            Long thingTypeFieldId) {
        List<Object> returnList;
        BasicDBObject fieldQuery = new BasicDBObject();
        fieldQuery.append("thingTypeId", thingTypeId);
        fieldQuery.append(fieldIdLabel, thingTypeFieldId);
        returnList = MongoDAOUtil.getInstance().things.distinct(fieldName, fieldQuery);
        if (logger.isDebugEnabled()) {
            logger.debug("Filter Field Name="+fieldName.toString()+", query="+fieldQuery.toString());
        }
        return returnList;
    }


    public List<Object> getFieldDistinctValues(
            String fieldName) {
        List<Object> returnList;
        returnList = MongoDAOUtil.getInstance().things.distinct(fieldName);
        if (logger.isDebugEnabled()) {
            logger.debug("Filter Field Name="+fieldName.toString());
        }
        return returnList;
    }
    /**
     * executes a query and return a map of udf values
     *
     * @param filterFields udf names
     */
    public Map<String, Object> executeQuery(
            DBObject query
            , List<String> filterFields
            , Integer pageNumber
            , Integer pageSize
            , String orderBy
            , boolean treeView
            , boolean returnFavorite) {
        Map<String, Object> dataResult = new HashMap<>();
        List<Map<String, Object>> result = new ArrayList<>();
        int countTotal = 0;
        logger.info("executeQuery: " + query);
        DBCursor cursor;
        try {
            cursor = MongoDAOUtil.getInstance().things.find(query);
            countTotal = cursor.count();
        } catch (com.mongodb.MongoTimeoutException e) {
            logger.error("Connection refused, mongo timeout");
            throw new RuntimeException("Connection refused, mongo timeout", e);
        }


        //Order cursor
        this.sortCursor(cursor, orderBy);

        //Pagination over a cursor
        this.addPaginationTo(cursor, pageNumber, pageSize);


        //Get values of the cursor in order to be displayed in UI
        for (Iterator<DBObject> it = cursor.iterator(); it.hasNext(); ) {
            BasicDBObject doc = (BasicDBObject) it.next();
            Map<String, Object> udfValues = new HashMap<>();
            if (filterFields.contains("*")) {
                for (Map.Entry<String, Object> entry : doc.entrySet()) {
                    if (entry.getValue() instanceof BasicDBObject) {
                        BasicDBObject udf = (BasicDBObject) entry.getValue();
                        Object value = null != udf ? udf.get("value") : null;
                        udfValues.put(entry.getKey(), value);
                    } else {
                        udfValues.put(entry.getKey(), entry.getValue());
                    }
                }
            } else {
//                udfValues = this.getValues(doc, filterFields, 1, treeView);
                udfValues = getValue(doc, filterFields, 1, treeView);
            }
            udfValues.put("treeLevel", 1);
            result.add(udfValues);
        }
        if (returnFavorite) {
            User user = (User) SecurityUtils.getSubject().getPrincipal();

            result = FavoriteService.getInstance().addFavoritesToList(result, user.getId(), "thing");
        }

        dataResult.put("list", result);
        dataResult.put("total", countTotal);
        return dataResult;
    }

    /***************************************************************
     * This method orders the results of a DBCursor
     ***************************************************************/
    public void sortCursor(DBCursor cursor, String orderBy) {
        if (orderBy != null) {
            try {
                String[] elements = orderBy.split(",");
                Map<String, String> mapOrder = new HashMap<>();
                for (String element : elements) {
                    String[] data = element.split(":");
                    mapOrder.put(data[0], data[1]);
                }
                Iterator entries = mapOrder.entrySet().iterator();
                BasicDBObject orderBo = null;
                while (entries.hasNext()) {
                    Map.Entry order = (Map.Entry) entries.next();
                    logger.info("sorting by " + order.getKey() + " " + order.getValue());
                    if (orderBo != null) {
                        orderBo.append(order.getKey().toString(), "asc".equals(order.getValue().toString()) ? 1 : -1);
                    } else {
                        orderBo = new BasicDBObject(order.getKey().toString(), "asc".equals(order.getValue().toString()) ? 1 : -1);
                    }
                }
                if (orderBo != null) {
                    cursor.sort(orderBo);
                }
            } catch (Exception e) {
                throw new UserException("The parameter 'order' must be in the format 'nameproperty:asc' or 'nameproperty:desc'", e);
            }

        }
    }

    /**
     * Method to get the value in MongoDB based on filter fields
     *
     * @param dbObject
     * @param filterFields
     * @param level
     * @param treeView
     * @return {@link Map}<{@link String},{@link Object}>
     */
    public Map<String, Object> getValue(BasicDBObject dbObject, List<String> filterFields, int level, boolean treeView) {
        Map<String, Object> udfValues = new HashMap<>();
        List<String> fields = getFiltersFields(dbObject, filterFields, false);
        List<String> childrenFields = getFiltersFields(dbObject, filterFields, true);

        for (String filterField : fields) {
            Object objectValue = dbObject;
            List<Object> lstObjectChildren = null;
            StringTokenizer st = new StringTokenizer(filterField, ".");
            while (st.hasMoreElements()) {
                String token = st.nextToken();
                if (objectValue instanceof BasicDBObject) {
                    if ("serialNumber".equals(token)) {
                        objectValue = ((String) ((BasicDBObject) objectValue).get(token)).toUpperCase();
                    } else {
                        objectValue = ((BasicDBObject) objectValue).get(token);
                    }
                } else if (objectValue instanceof BasicDBList) {
                    lstObjectChildren = new ArrayList<>();
                    for (Object objChildren : ((BasicDBList) objectValue).toArray()){
                        Map<String, Object> objectChildren = getValue((BasicDBObject) objChildren, this.childrenFilterFields(fields, level + 1), level + 1, treeView);
                        lstObjectChildren.add(objectChildren);
                    }
                } else {
                    objectValue = null;
                }
            }

            if (lstObjectChildren != null && lstObjectChildren.size() > 0) {
                udfValues.put(filterField, lstObjectChildren);
            } else {
                if (objectValue instanceof BasicDBObject) {
                    udfValues.put(filterField, objectValue);
                } else {
                    udfValues.put(filterField, objectValue != null ? objectValue : null);
                }
            }
        }

        if (childrenFields != null && !childrenFields.isEmpty()) {
            List<Object> lstObjectChildren = new ArrayList<>();
            for (String filterField : childrenFields) {
                Object objectValue = dbObject;
                if (filterField.contains(".value._id")) {
                    String token = filterField.substring(0, filterField.indexOf(".value._id"));
                    objectValue = objectValue != null ? ((BasicDBObject) objectValue).get(token) : null;
                    objectValue = objectValue != null ? ((BasicDBObject) objectValue).get("value") : null;
                } else {
                    objectValue = objectValue != null ? ((BasicDBObject) objectValue).get(filterField) : null;
                }
                Map<String, Object> dataChildren;

                if (objectValue instanceof BasicDBList){
                    for (Object objChildren : ((BasicDBList) objectValue).toArray()) {
                        dataChildren = getValue((BasicDBObject) objChildren, filterFields, level + 1, treeView);
                        dataChildren.put("treeLevel", level + 1);
                        lstObjectChildren.add(dataChildren);
                    }
                }else if (objectValue instanceof BasicDBObject && treeView){
                    dataChildren = getValue((BasicDBObject)objectValue, filterFields, level+1, treeView);
                    dataChildren.put("treeLevel", level + 1);
                    lstObjectChildren.add(dataChildren);
                }
            }
            if (!lstObjectChildren.isEmpty()) {
                udfValues.put("children", lstObjectChildren);
            }
        }
        return udfValues;
    }

    /**
     * filter childs
     * @param doc
     * @param filterFields
     * @param isChildren
     * @return
     */
    public List<String> getFiltersFields(BasicDBObject doc, List<String> filterFields, boolean isChildren) {
        List<String> resultFilters = new ArrayList<>();
        if (isChildren) {
            Collection<String> filterChildren = Collections2.filter(filterFields, Predicates.containsPattern("children"));
            if (filterChildren != null && !filterChildren.isEmpty()) {
                resultFilters.addAll(Collections2.filter(doc.keySet(), Predicates.containsPattern("children")));
            }
            resultFilters.addAll(Collections2.filter(filterFields, Predicates.containsPattern(".value._id")));
        } else {
            Predicate<String> predicate = new Predicate<String>() {
                @Override
                public boolean apply(String data) {
                    return !data.contains("children") && !data.contains(".value._id");
                }
            };
            resultFilters.addAll(Collections2.filter(filterFields, predicate));
        }
        return resultFilters;
    }

    /**********************************
     * Method to get Children Filters
     **********************************/
    public List<String> childrenFilterFields(List<String> filterFields, int upLevel) {
        List<String> childrenFilters = new ArrayList<>();
        for (String filter : filterFields) {
            if (filter.contains(".")) {
                String newData = "";
                String[] migra = filter.split("\\.");
                for (int i = upLevel; i < migra.length; i++) {
                    newData = newData + migra[i] + ".";
                }
                childrenFilters.add(newData.substring(0, newData.length() - 1));
            } else {
                childrenFilters.add(filter);
            }
        }
        return childrenFilters;
    }

    /******************************************************
     * This query builds a new query for tree view
     ******************************************************/
    public void getQueryForTreeView(Map<String, Object> queryForTree, BasicDBList lst) {
        Map<String, Long> nameUdfParent = (Map<String, Long>) queryForTree.get("nameUdfParent");
        List<Long> dataNativeChildren = (List<Long>) queryForTree.get("dataNativeChildren");

        //UDF Children
        if (nameUdfParent != null && nameUdfParent.size() > 0) {
            BasicDBList and = new BasicDBList();
            Iterator it = nameUdfParent.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                and.add(new BasicDBObject(pair.getKey() + ".value.thingTypeId", new BasicDBObject("$ne", pair.getValue())));
            }
            lst.add(new BasicDBObject("$and", and));
        }
        //Native Children
        if (dataNativeChildren != null && dataNativeChildren.size() > 0) {
            BasicDBObject notInNativeChildren = new BasicDBObject("thingTypeId", new BasicDBObject("$nin", dataNativeChildren));
            lst.add(notInNativeChildren);
        }

    }

    /**
     * executes a groupedBy query and return a map of udf values
     *
     * @param groupByFields udf names for grouping
     */
    public List<Map<String, Object>> executeGroupedByQuery(DBObject condition, List<String> groupByFields, Integer pageNumber, Integer pageSize) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();


        Map<String, Object> dbObjIdMap = new HashMap<String, Object>();
        for (String field : groupByFields) {
            dbObjIdMap.put(field, "$" + field + ".value");
        }
        DBObject groupFields = new BasicDBObject("_id", new BasicDBObject(dbObjIdMap));

        List<DBObject> listObjects = new LinkedList<DBObject>();
        DBObject match = null;
        if (null != condition) {
            match = new BasicDBObject("$match", condition);
            listObjects.add(match);
        }
        DBObject group = new BasicDBObject("$group", groupFields);
        listObjects.add(group);
        AggregationOutput output = MongoDAOUtil.getInstance().things.aggregate(listObjects);
        Iterable<DBObject> cursor = output.results();
        // TODO Iterable object cannot be converted to DBCursor for the following pagination
        //DBCursor cursor = (DBCursor) output.results();

        //Pagination over a cursor
        //this.addPaginationTo( cursor , pageNumber,  pageSize );

        for (Iterator<DBObject> it = cursor.iterator(); it.hasNext(); ) {
            BasicDBObject doc = (BasicDBObject) it.next();
            Map<String, Object> udfValues = new HashMap<>();
            for (String field : groupByFields) {
                BasicDBObject udf = (BasicDBObject) doc.get("_id");
                String value = null != udf ? ((BasicDBObject) doc.get("_id")).getString(field) : null;
                udfValues.put(field, value);
            }
            result.add(udfValues);
        }
        return result;
    }

    /**
     * Drops Things collection
     */
    public void dropThingsCollection() {
        logger.info("dropping things collection");
        MongoDAOUtil.getInstance().things.drop();

        //TODO review and reorganize
        set.clear();
    }

    /**
     * Takes Backup of Things collection
     */
    public void backupThingsCollecion(String bkpName) {
        if (MongoDAOUtil.getInstance().things.count() > 0) {
            TimerUtil tu = new TimerUtil();
            logger.info("Taking backup of thing collection into " + bkpName);
            tu.mark();
            MongoDAOUtil.getInstance().things.rename(bkpName);
            tu.mark();
            logger.info("Total time to get BackUp. " + tu.getLastDelt());
        } else {
            logger.info("Things collection empty, nothing to backup.");
        }
    }

    /**
     * Drops Thing snapshots collection
     */
    public void dropThingSnapshotsCollection() {
        logger.info("dropping thingSnapshots collection");
        MongoDAOUtil.getInstance().thingSnapshots.drop();
    }

//    /**
//     * Drops Things bucket collection
//     */
//    public void dropThingBucketCollection() {
//        logger.info("dropping thingBucket collection");
//        MongoDAOUtil.getInstance().thingBucketCollection.drop();
//    }

    /**
     * Drops Things ids for snapshots collection
     */
    public void dropThingSnapshotIdsCollection() {
        logger.info("dropping thingSnapshotIds collection");
        MongoDAOUtil.getInstance().thingSnapshotIds.drop();
    }

    public void createThingSnapshotsIndexes() {
        //Create Index

        BasicDBObject timeIdx = new BasicDBObject("time", 1);
        BasicDBObject parentIdx = new BasicDBObject("value.parent._id", 1);
        BasicDBObject serialIdx = new BasicDBObject("value.serialNumber", 1);

        //MongoDAOUtil.getInstance().thingSnapshots.createIndex(valueIdIdx, "value._id_");
        MongoDAOUtil.getInstance().thingSnapshots.createIndex(timeIdx, "time_");
        MongoDAOUtil.getInstance().thingSnapshots.createIndex(parentIdx, "value.parent._id_");
        MongoDAOUtil.getInstance().thingSnapshots.createIndex(serialIdx, "value.serialNumber_");
        MongoDAOUtil.getInstance().thingSnapshots.createIndex(new BasicDBObject("value._id", 1).append("time", -1));

        BasicDBObject parentIdx1 = new BasicDBObject("parent._id", 1);
        MongoDAOUtil.getInstance().things.createIndex(parentIdx1, "parent._id_");

        //ALEBLog
        if (!MongoDAOUtil.getInstance().db.collectionExists("ALEBLog")) {
            DBObject options = BasicDBObjectBuilder.start().add("capped", true).add("size", 10000000L).get();
            MongoDAOUtil.getInstance().db.createCollection("ALEBLog", options);
        }

        //timeseries
        if (!MongoDAOUtil.getInstance().db.collectionExists("timeseries")) {
            MongoDAOUtil.getInstance().db.createCollection("timeseries", new BasicDBObject());
        }

        //timeseriesControl
        if (!MongoDAOUtil.getInstance().db.collectionExists("timeseriesControl")) {
            DBObject options = BasicDBObjectBuilder.start().add("noPadding", true).get();
            MongoDAOUtil.getInstance().db.createCollection("timeseriesControl", options);
        }

    }

    /**
     * Retrieves Mongo DB object
     */
    public DB getDB() {
        return MongoDAOUtil.getInstance().db;
    }

    /**
     * Relieves Things Collection
     */
    public static DBCollection getThingsCollection() {
        return MongoDAOUtil.getInstance().things;
    }

    public DBObject getThing(Long id) {
        BasicDBObject q = new BasicDBObject("_id", id);
        logger.info("QUERY get Thing By ID:" + q.toString());
        DBCursor cursor = MongoDAOUtil.getInstance().things.find(q);
        DBObject thing = null;
        if (cursor.hasNext()) thing = cursor.next();
        return thing;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getThingToMap(Long id) {
        DBObject dbObject = getInstance().getThing(id);
        if (dbObject != null) {
            return (Map<String, Object>) dbObject.toMap();
        }
        return null;
    }


    // <editor-fold desc="Moved from ThingMongoInsertDAO.java, review">

    Set<Long> set = new HashSet<Long>();

    @Deprecated
    /**
     * TODO Review Method
     */

    public boolean contains(Long thingId) {
        if (set.contains(thingId)) {
            return true;
        }

        BasicDBObject query = new BasicDBObject("_id", thingId);
        DBCursor cursor = MongoDAOUtil.getInstance().things.find(query);
        try {
            while (cursor.hasNext()) {
                // logger.error( "****************** thingId=" + thingId +
                // " c='" + cursor.next() + "'" );
                set.add(thingId);
                return true;
            }
        } finally {
            cursor.close();
        }

        return false;
    }

    /**
     * Use this method to insert OR update a "native" mysql thing property
     */
    @Deprecated
    /**
     * TODO Review Method
     */
    public void insertOrUpdate(Long thingId, String property, String value) {
        if (thingId == null) {
            logger.error("thingId is null !!!");
            return;
        }

        try {
            if (!contains(thingId)) {
                insert(thingId, property, value);
            } else {
                update(thingId, property, value);
            }
        } catch (MongoException me) {
            logger.warn("", me);
        }
    }

    /**
     * Use this method to insert a "native" mysql thing property
     */
    @Deprecated
    /**
     * TODO Review Method
     */
    private void insert(Long thingId, String property, String value) {
        logger.debug("thingId=" + thingId + " name=" + property + " value=" + value);

        BasicDBObject doc = new BasicDBObject("_id", thingId);
        doc.append(property, value);
        MongoDAOUtil.getInstance().things.insert(doc);

        // add to set of known things
        set.add(thingId);
    }

    @Deprecated
    /**
     * TODO Review Method
     */
    public void insertFields(ThingObject thing) {
        BasicDBObject doc = new BasicDBObject("_id", thing.getId());
        doc.append("groupTypeId", thing.getGroupTypeId());
        doc.append("groupTypeCode", thing.getGroupTypeCode());
        doc.append("groupId", thing.getGroupId());
        doc.append("groupCode", thing.getGroupCode());
        doc.append("thingTypeId", thing.getThingTypeId());
        doc.append("thingTypeCode", thing.getThingTypeCode());
        doc.append("name", thing.getName());
        doc.append("serialNumber", thing.getSerialNumber());

        MongoDAOUtil.getInstance().things.insert(doc);

        // add to set of known things
        set.add(thing.getId());
    }

    @Deprecated
    /**
     * TODO Review Method
     */
    public void updateFields(ThingObject thing) {

        BasicDBObject doc = new BasicDBObject("groupTypeId", thing.getGroupTypeId());
        doc.append("groupTypeId", thing.getGroupTypeId());
        doc.append("groupTypeCode", thing.getGroupTypeCode());
        doc.append("groupId", thing.getGroupId());
        doc.append("groupCode", thing.getGroupCode());
        //doc.append( "groupTypeName", thing.getGroupTypeName());
        //doc.append( "groupName",     thing.getGroupName() );
        doc.append("thingTypeId", thing.getThingTypeId());
        doc.append("thingTypeCode", thing.getThingTypeCode());
        doc.append("name", thing.getName());
        doc.append("serialNumber", thing.getSerialNumber());

        BasicDBObject set = new BasicDBObject("$set", doc);
        BasicDBObject find = new BasicDBObject("_id", thing.getId());
        MongoDAOUtil.getInstance().things.update(find, set);
    }

    @Deprecated
    /**
     * TODO Review Method
     */
    /**
     * Use this method to update a "native" mysql thing property
     */
    private void update(Long thingId, String property, String value) {
        logger.debug("thingId=" + thingId + " name=" + property + " value=" + value);

        //BulkWriteOperation b = things.initializeOrderedBulkOperation();
        //BulkWriteRequestBuilder rb = b.find( new BasicDBObject( "_id", thingId ) );
        //BasicDBObject field = new BasicDBObject( property, value );
        //rb.updateOne( new BasicDBObject( "$set", field ) );
        //BulkWriteResult result = b.execute();

        BasicDBObject set = new BasicDBObject("$set", new BasicDBObject(property, value));
        BasicDBObject find = new BasicDBObject("_id", thingId);

        WriteResult result = MongoDAOUtil.getInstance().things.update(find, set, false, false, WriteConcern.ACKNOWLEDGED);

        logger.debug("thingId=" + thingId + " name=" + property + " value=" + value + " updated " + result.getN());
    }

    /**
     * Use this method to insert OR update the parent thing property
     */
    @Deprecated
    /**
     * TODO Review Method
     */
    public void insertOrUpdateParent(Long thingId, Long value) {
        if (thingId == null) {
            logger.error("thingId is null !!!");
            return;
        }

        try {
            //todo: get the active flag from UI, and set it according that value
            BasicDBObject parentField;
            if (value == null) {
                parentField = null;
            } else {
                parentField = new BasicDBObject("_id", value)
                        .append("active", true);
            }

            if (!contains(thingId)) {
                BasicDBObject doc = new BasicDBObject("_id", thingId);
                doc.append("parent", parentField);
                MongoDAOUtil.getInstance().things.insert(doc);

                set.add(thingId);
            } else {
                BasicDBObject set = new BasicDBObject("$set", new BasicDBObject("parent", parentField));
                BasicDBObject find = new BasicDBObject("_id", thingId);

                MongoDAOUtil.getInstance().things.update(find, set);
            }
        } catch (MongoException me) {
            logger.warn("", me);
        }
    }

    /**
     * Update the parent's copy of a thing
     *
     * @param thingId thing id of thing that changed and needs to be updated on parent
     */
    @Deprecated
    /**
     * TODO Review Method
     */
    public void updateParent(long thingId) {

        BasicDBObject query = new BasicDBObject("_id", thingId);
        DBObject child = MongoDAOUtil.getInstance().things.findOne(query);

        if (child.containsField("parent") && child.get("parent") instanceof DBObject) {

            DBObject parent = (DBObject) child.get("parent");

            Long parentId = (Long) parent.get("id");

            //remove parent property since we are adding the child to the parent
            child.removeField("parent");

            //set the parent with the child modifications\
            BasicDBObject setParent = new BasicDBObject("$set", new BasicDBObject("children.$", child));

            //find the parent
            BasicDBObject find = new BasicDBObject("_id", parentId);
            find.append("children._id", thingId);

            MongoDAOUtil.getInstance().things.update(find, setParent);
        }

        //db.students.update(
        //{ _id: 1, "children._id": childId },
        //{ $set: { "children.$" : child } }
        //)
    }

    @Deprecated
    /**
     * TODO Review Method
     */
    /** helper returns values that match the ids **/
    protected Cursor in(List<Long> childIds) {
        BasicDBObject query = new BasicDBObject("_id", new BasicDBObject("$in", childIds));
        return MongoDAOUtil.getInstance().things.find(query);
    }

    @Deprecated
    /**
     * TODO Review Method
     */
    /** helper that parses a cursor and returns a BasicDBList **/
    public BasicDBList toDBList(List<Long> childIds) {
        Cursor cursor = in(childIds);
        BasicDBList dbl = new BasicDBList();

        while (cursor.hasNext()) {
            DBObject record = cursor.next();
            record.removeField("parent");
            dbl.add(record);
        }

        return dbl;
    }


    @Deprecated
    /**
     * TODO Review Method
     */
    /**
     * Use this method to insert OR update a UDF thing property
     */
    public void insertOrUpdate(long thingId, long thingTypeFieldId, String thingTypeFieldName, Date time, String value) {
        try {
            if (value != null) {
                if (!contains(thingId)) {
                    insert(thingId, thingTypeFieldId, thingTypeFieldName, time, value);
                } else {
                    update(thingId, thingTypeFieldId, thingTypeFieldName, time, value);
                }
            }
        } catch (MongoException me) {
            logger.warn("", me);
        }
    }

    @Deprecated
    /**
     * TODO Review Method
     */
    public void insert(long thingId, long thingTypeFieldId, String thingTypeFieldName, Date time, String value) {
        BasicDBObject doc = new BasicDBObject("_id", thingId);

        BasicDBObject b2 = new BasicDBObject("thingTypeFieldId", thingTypeFieldId);
        b2.append("time", time);
        b2.append("value", value.trim());

        logger.info("thingId=" + thingId + " thingTypeFieldId=" + thingTypeFieldId + " name=" + thingTypeFieldName +
                " value=" + value.trim());

        BasicDBObject field = new BasicDBObject(thingTypeFieldName, b2);

        doc.append(thingTypeFieldName, field);

        MongoDAOUtil.getInstance().things.insert(doc);

        // add to set of known things
        set.add(thingId);
    }

    @Deprecated
    /**
     * TODO Review Method
     */
    public void update(long thingId, long thingTypeFieldId, String thingTypeFieldName, Date time, String value) {
        BasicDBObject newValues = new BasicDBObject("thingTypeFieldId", thingTypeFieldId);
        newValues.append("time", time);
        newValues.append("value", value.trim());

        BasicDBObject set = new BasicDBObject("$set", new BasicDBObject(thingTypeFieldName, newValues));
        BasicDBObject find = new BasicDBObject("_id", thingId);

        WriteResult result = MongoDAOUtil.getInstance().things.update(find, set, false, false, WriteConcern.ACKNOWLEDGED);

        logger.debug("thingId=" + thingId + " thingTypeFieldId=" + thingTypeFieldId + " name=" +
                thingTypeFieldName + " value=" + value.trim() + " updated " + result.getN());
    }

    @Deprecated
    /**
     * TODO Review Method
     */
    public Map<String, Long> getThingsByZone() {
        Map<String, Long> thingsByZone = new HashMap<>(100);

        DBObject group = new BasicDBObject("$group", new BasicDBObject("_id", "$zone.value")
                .append("n", new BasicDBObject("$sum", 1L)));


        AggregationOutput output = MongoDAOUtil.getInstance().things.aggregate(group);
//        CommandResult commandResult = output.getCommandResult();

//        BasicDBList results = (BasicDBList) commandResult.get("result");
        for (Iterator<DBObject> it = output.results().iterator(); it.hasNext(); ) {
            BasicDBObject doc = (BasicDBObject) it.next();
            thingsByZone.put(doc.getString("_id"), doc.getLong("n"));
        }

        return thingsByZone;
    }


    /**
     * update value for a property thing
     */
    @Deprecated
    /**
     * TODO Review Method
     */
    public void update(String propertyName, Object oldValue, Object newValue) {
        BasicDBObject set = new BasicDBObject("$set", new BasicDBObject(propertyName, newValue));
        BasicDBObject find = new BasicDBObject(propertyName, oldValue);
        MongoDAOUtil.getInstance().things.update(find, set, false, true);
    }

    /***************************************************
     * Method to apply pagination over a cursor
     **************************************************/
    public void addPaginationTo(DBCursor cursor, Integer pageNum, Integer pageSize) {
        if (pageNum != null && pageNum.intValue() > 0
                && pageSize != null && pageSize.intValue() > 0) {
            //add pagination
            Integer skip = (pageNum > 1) ? (pageNum - 1) * pageSize : 0;
            cursor.skip(skip);
            cursor.limit(pageSize);
        }
    }

    /*********************************************************
     * Get ist of things based on filters, groups, only...etc.
     *********************************************************/
    public List<Map<String, Object>> listThingsByFieldValues2(List<String> filterFields, List<String> groupByFields, String whereFieldValue, String whereThing) {
        List<Map<String, Object>> result = new ArrayList<>();

        // construct condition
        DBObject condition = null;
        if (StringUtils.isNotEmpty(whereThing) && StringUtils.isNotEmpty(whereFieldValue)) {
            condition = ThingMongoDAO.getInstance().getObjectCondition(
                    ThingMongoDAO.getInstance().parseWhereString(whereThing),
                    ThingMongoDAO.getInstance().parseWhereString(whereFieldValue), "$and");
        } else if (StringUtils.isNotEmpty(whereThing)) {
            condition = ThingMongoDAO.getInstance().parseWhereString(whereThing);
        } else if (StringUtils.isNotEmpty(whereFieldValue)) {
            condition = ThingMongoDAO.getInstance().parseWhereString(whereFieldValue);
        }

        if (filterFields != null && !filterFields.isEmpty()) {
            Map<String, Object> lista = ThingMongoDAO.getInstance().executeQuery(condition, filterFields, null, null, null, false, false);
            result = ((List<Map<String, Object>>) lista.get("list"));
        } else {
            if (groupByFields != null && !groupByFields.isEmpty())
                result = ThingMongoDAO.getInstance().executeGroupedByQuery(condition, groupByFields, null, null);
        }
        return result;
    }

    /************************************************************
     * This method does the filter tasks in order to get a list of things
     ************************************************************/
    public Map<String, Object> getListOfThings(
            Integer pageSize
            , Integer pageNumber
            , String order
            , String where
            , String extra
            , String only
            , String groupBy
            , Long visibilityGroupId
            , String upVisibility
            , String downVisibility
            , Map<Long, List<ThingType>> groups
            , Map<String, Object> queryForTree
            , String serialNumber
            , Long thingTypeId
            , boolean treeView
            , boolean returnFavorite
    ) throws Exception {
        Map<String, Object> dataResult = new HashMap<>();
        List<Map<String, Object>> result = new ArrayList<>();
        List<String> onlyFields = new ArrayList<>();
        List<String> groupByFields = new ArrayList<>();

        //Validation of the query
        ValidationBean validationBean = this.validateList(
                pageSize
                , pageNumber
                , order
                , where
                , extra
                , only
                , groupBy
                , visibilityGroupId
                , upVisibility
                , downVisibility);
        if (validationBean.isError()) {
            throw new UserException(validationBean.getErrorDescription());
        }

        // construct condition
        DBObject queryCondition = null;
        DBObject parentCondition = null;
        if ((serialNumber != null && !serialNumber.isEmpty()) || (thingTypeId != null)) {
            // query condition
            Map<String, Object> thingTypeIsParentMap = (Map) queryForTree.get("thingTypeIsParentMap");
            Map<String, List<String>> thingTypeIsNotParentMap = (Map) queryForTree.get("thingTypeIsNotParentMap");
            BasicDBList or = new BasicDBList();
            if (thingTypeIsNotParentMap != null) {
                for (Map.Entry<String, List<String>> entry : thingTypeIsNotParentMap.entrySet()) {
                    String thingTypeCode = entry.getKey();
                    List<String> udfNames = entry.getValue();
                    for (String udfName : udfNames) {
                        if (udfName != null) {
                            if (serialNumber != null) {
                                BasicDBObject query = new BasicDBObject();
                                query.append("thingTypeCode", thingTypeCode);
                                BasicDBList orDB = new BasicDBList();
                                DBObject orQuery = new BasicDBObject(udfName + ".value.serialNumber",
                                        new BasicDBObject("$regex", java.util.regex.Pattern.compile(serialNumber.toUpperCase())));
                                DBObject orQuery1 = new BasicDBObject(udfName + ".value.name",
                                        new BasicDBObject("$regex", java.util.regex.Pattern.compile(serialNumber)).append("$options", "i"));
                                orDB.add(orQuery);
                                orDB.add(orQuery1);
                                query.append("$or", orDB);
                                or.add(query);
                            } else {
                                BasicDBObject query1 = new BasicDBObject();
                                query1.append("thingTypeCode", thingTypeCode);
                                query1.append(udfName, new BasicDBObject("$exists", true));
                                if (thingTypeId != null)
                                    query1.append(udfName + ".value.thingTypeId", thingTypeId);
                                or.add(query1);
                            }
                        }
                    }
                }
            }
            if (thingTypeIsParentMap != null) {
                for (Map.Entry<String, Object> entry : thingTypeIsParentMap.entrySet()) {
                    String key = entry.getKey();
                    if (entry.getValue() != null) {
                        List<String> nameUDFChildrens = (List) entry.getValue();
                        for (String nameUDFChildren : nameUDFChildrens) {
                            BasicDBObject query = new BasicDBObject();
                            if (thingTypeId != null)
                                query.append("thingTypeId", thingTypeId);
                            else
                                query.append("thingTypeCode", key);
                            query.append(nameUDFChildren, new BasicDBObject("$exists", true));
                            if (serialNumber != null) {
                                BasicDBList orDB = new BasicDBList();
                                DBObject orQuery = new BasicDBObject("serialNumber", new BasicDBObject("$regex",
                                        java.util.regex.Pattern.compile(serialNumber)).append("$options", "i"));
                                DBObject orQuery1 = new BasicDBObject("name", new BasicDBObject("$regex",
                                        java.util.regex.Pattern.compile(serialNumber)).append("$options", "i"));
                                orDB.add(orQuery);
                                orDB.add(orQuery1);
                                query.append("$or", orDB);
                            }
                            or.add(query);

                            BasicDBObject query1 = new BasicDBObject();
                            query1.append("thingTypeCode", key);
                            query1.append(nameUDFChildren, new BasicDBObject("$exists", true));
                            if (thingTypeId != null)
                                query1.append(nameUDFChildren + ".thingTypeId", thingTypeId);
                            if (serialNumber != null) {
                                BasicDBList orDB = new BasicDBList();
                                DBObject orQuery = new BasicDBObject(nameUDFChildren + ".serialNumber", new BasicDBObject("$regex",
                                        java.util.regex.Pattern.compile(serialNumber)).append("$options", "i"));
                                DBObject orQuery1 = new BasicDBObject(nameUDFChildren + ".name", new BasicDBObject("$regex",
                                        java.util.regex.Pattern.compile(serialNumber)).append("$options", "i"));
                                orDB.add(orQuery);
                                orDB.add(orQuery1);
                                query1.append("$or", orDB);
                            }
                            or.add(query1);

                            BasicDBObject query2 = new BasicDBObject();
                            query2.append("thingTypeCode", key);
                            query2.append(nameUDFChildren, new BasicDBObject("$exists", true));
                            if (thingTypeId != null)
                                query2.append(nameUDFChildren + ".children.thingTypeId", thingTypeId);
                            if (serialNumber != null) {
                                BasicDBList orDB = new BasicDBList();
                                DBObject orQuery = new BasicDBObject(nameUDFChildren + ".children.serialNumber", new BasicDBObject("$regex",
                                        java.util.regex.Pattern.compile(serialNumber)).append("$options", "i"));
                                DBObject orQuery1 = new BasicDBObject(nameUDFChildren + ".children.name", new BasicDBObject("$regex",
                                        java.util.regex.Pattern.compile(serialNumber)).append("$options", "i"));
                                orDB.add(orQuery);
                                orDB.add(orQuery1);
                                query2.append("$or", orDB);
                            }
                            or.add(query2);

                        }
                    } else {
                        BasicDBObject query = new BasicDBObject();
                        query.append("thingTypeCode", key);
                        query.append("children", new BasicDBObject("$exists", true));
                        if (thingTypeId != null)
                            query.append("children.thingTypeId", thingTypeId);
                        if (serialNumber != null) {
                            BasicDBList orDB = new BasicDBList();
                            DBObject orQuery = new BasicDBObject("children.serialNumber", new BasicDBObject("$regex",
                                    java.util.regex.Pattern.compile(serialNumber)).append("$options", "i"));
                            DBObject orQuery1 = new BasicDBObject("children.name", new BasicDBObject("$regex",
                                    java.util.regex.Pattern.compile(serialNumber)).append("$options", "i"));
                            orDB.add(orQuery);
                            orDB.add(orQuery1);
                            query.append("$or", orDB);
                        }
                        or.add(query);

                        BasicDBObject query1 = new BasicDBObject();
                        if (thingTypeId != null)
                            query1.append("thingTypeId", thingTypeId);
                        else
                            query1.append("thingTypeCode", key);
                        query1.append("children", new BasicDBObject("$exists", true));
                        if (serialNumber != null) {
                            BasicDBList orDB = new BasicDBList();
                            DBObject orQuery = new BasicDBObject("serialNumber", new BasicDBObject("$regex",
                                    java.util.regex.Pattern.compile(serialNumber)).append("$options", "i"));
                            DBObject orQuery1 = new BasicDBObject("name", new BasicDBObject("$regex",
                                    java.util.regex.Pattern.compile(serialNumber)).append("$options", "i"));
                            orDB.add(orQuery);
                            orDB.add(orQuery1);
                            query1.append("$or", orDB);
                        }
                        or.add(query1);
                    }
                }
                BasicDBObject query = new BasicDBObject();
                query.append("children", new BasicDBObject("$exists", false));
                query.append("parent", new BasicDBObject("$exists", false));
                if (thingTypeId != null)
                    query.append("thingTypeId", thingTypeId);
                if (serialNumber != null) {
                    BasicDBList orDB = new BasicDBList();
                    DBObject orQuery = new BasicDBObject("serialNumber", new BasicDBObject("$regex",
                            java.util.regex.Pattern.compile(serialNumber)).append("$options", "i"));
                    DBObject orQuery1 = new BasicDBObject("name", new BasicDBObject("$regex",
                            java.util.regex.Pattern.compile(serialNumber)).append("$options", "i"));
                    orDB.add(orQuery);
                    orDB.add(orQuery1);
                    query.append("$or", orDB);
                }
                or.add(query);

                queryCondition = new BasicDBObject("$or", or);
            } else {
                queryCondition = ThingMongoDAO.getInstance().parseWhereString(where);
            }
        } else {
            queryCondition = ThingMongoDAO.getInstance().parseWhereString(where);
            if (treeView)
                parentCondition = new BasicDBObject("parent", new BasicDBObject("$exists", false));
        }

        // Get group by and onlyFields
        if (null != groupBy && !groupBy.isEmpty()) {
            groupByFields = Arrays.asList(StringUtils.split(groupBy, ","));
        } else {
            if (null != only && !only.isEmpty()) {
                if (!only.trim().equals("*")) {
                    onlyFields = Arrays.asList(StringUtils.split(only, ","));
                }
            } else {
                onlyFields.add("*");
            }
        }

        //Check Visibility
        DBObject basicDBObjectVisibility = groups.size() > 0 ? this.addGroupIdFilter(groups, upVisibility, downVisibility) : null;

        //Make condition
        BasicDBList lst = new BasicDBList();
        if (queryCondition != null) {
            lst.add(queryCondition);
        }
        if (parentCondition != null) {
            lst.add(parentCondition);
        }
        if (basicDBObjectVisibility != null) {
            lst.add(basicDBObjectVisibility);
        }
        if (queryForTree != null && queryForTree.size() > 0) {
            this.getQueryForTreeView(queryForTree, lst);
        }
        //Add logic data
        BasicDBObject condition = new BasicDBObject("$and", lst);

        //Execute Query
        if (onlyFields != null && !onlyFields.isEmpty()) {
            dataResult = ThingMongoDAO.getInstance().executeQuery(condition, onlyFields, pageNumber, pageSize, order, treeView, returnFavorite);
        } else {
            if (groupByFields != null && !groupByFields.isEmpty()) {
                result = ThingMongoDAO.getInstance().executeGroupedByQuery(condition, groupByFields, pageNumber, pageSize);
                if (returnFavorite) {
                    User user = (User) SecurityUtils.getSubject().getPrincipal();

                    result = FavoriteService.getInstance().addFavoritesToList(result, user.getId(), "thing");
                }

                dataResult.put("list", result);
                dataResult.put("total", result.size());
            }
        }

        //Sort List
        TreeUtils.sortObjects(order, result);

        // remove things that are duplicated when exists a relation by udf thingtype
        if (queryForTree != null) {
            removeDuplicated(queryForTree, dataResult);
        }

        return dataResult;
    }

    /********************************************
     * This method validate the values of the query of things
     *******************************************/
    public ValidationBean validateList(
            Integer pageSize
            , Integer pageNumber
            , String order
            , String where
            , String extra
            , String only
            , String groupBy
            , Long visibilityGroupId
            , String upVisibility
            , String downVisibility) {
        ValidationBean valida = new ValidationBean();
        StringBuffer message = new StringBuffer("");
        if (pageNumber != null && pageNumber.intValue() < 1) {
            message.append(String.format("'pageNumber' should have a number greater than 0."));
        }
        if (pageSize != null && pageSize.intValue() < 1) {
            message.append(String.format("'pageSize' should have a number greater than 0."));
        }
        if (groupBy != null && !groupBy.trim().equals("")
                && only == null || (only != null && only.trim().equals(""))) {
            message.append(String.format("If you want to get results with 'groupBy', 'only' cannot be empty."));
        }

        if (message.length() > 0) {
            valida.setErrorDescription(message.toString());
            valida.setError(false);
        }
        return valida;
    }

    /**********************************************
     * Check visibility of thingTypes. It returns a list of groups and list of thing types permitted
     * based on the  group of the user
     *
     * @params Long groupId : group Id to made the filter
     **********************************************/
    private DBObject addGroupIdFilter(
            Map<Long, List<ThingType>> groups
            , String upVisibility
            , String downVisibility) {
        BasicDBList and = new BasicDBList();
        List<Long> lstGroupIds = new ArrayList<>();
        List<Long> lstThingTypeIds = new ArrayList<>();
        if ((upVisibility != null && upVisibility.equals("false")) &&
                (downVisibility != null && downVisibility.equals("false"))) {
            //Show only things of the group
            Iterator entries = groups.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry thisEntry = (Map.Entry) entries.next();
                Long key = (Long) thisEntry.getKey();
                lstGroupIds.add(key);
                List<ThingType> value = (List<ThingType>) thisEntry.getValue();
                for (ThingType thingType : value) {
                    lstThingTypeIds.add(thingType.getId());
                }
            }
        } else if ((upVisibility != null && upVisibility.equals("false")) &&
                (downVisibility != null && (downVisibility.equals("") || downVisibility.equals("true")))) {
            for (Long groupId : groups.keySet()) {
                //Get Children
                lstGroupIds = GroupService.getInstance().getListGroupIdsChildren(groupId);
                lstThingTypeIds = getThingTypes(groups.get(groupId));
            }

        } else if ((downVisibility != null && downVisibility.equals("false")) &&
                (upVisibility != null && (upVisibility.equals("") || upVisibility.equals("true")))) {
            //TODO: This part of code is going to change when upVisibility will be available
            Iterator entries = groups.entrySet().iterator();
            while (entries.hasNext()) {
                Map.Entry thisEntry = (Map.Entry) entries.next();
                Long key = (Long) thisEntry.getKey();
                lstGroupIds.add(key);
                List<ThingType> value = (List<ThingType>) thisEntry.getValue();
                for (ThingType thingType : value) {
                    lstThingTypeIds.add(thingType.getId());
                }
            }
        } else {
            //By default, children are added to the list
            for (Long groupId : groups.keySet()) {
                //Get Children
                lstGroupIds = GroupService.getInstance().getListGroupIdsChildren(groupId);
                lstThingTypeIds = getThingTypes(groups.get(groupId));
            }
        }


        if (lstGroupIds != null && lstGroupIds.size() > 0) {
            and.add(
                    new BasicDBObject("groupId", new BasicDBObject("$in", lstGroupIds)
                    )
            );
        }
        if (lstThingTypeIds != null && lstThingTypeIds.size() > 0) {
            and.add(
                    new BasicDBObject("thingTypeId", new BasicDBObject("$in", lstThingTypeIds)
                    )
            );
        }

        BasicDBObject query = new BasicDBObject("$and", and);
        logger.debug("Filter Visibility: " + query);
        return query;
    }

    /*************************
     * Get a list of Id's of a Thingtypes's list
     **************************/
    private List<Long> getThingTypes(List<ThingType> types) {
        List<Long> ids = new ArrayList<>();
        for (ThingType type : types) {
            ids.add(type.getId());
        }
        return ids;
    }



	/*Mongo*/

    /**
     * Update the children associated to the parent thing with all of its current data
     *
     * @param thingId id of the thing which is parent
     */
    public void updateChild(Long thingId) {
        logger.debug("Updating parent in children thingId=" + thingId);
        BasicDBObject query = new BasicDBObject("_id", thingId);
        DBObject parent = thingsCollection.findOne(query);

        if (parent.containsField("children")) {
            parent.removeField("children");
            BasicDBObject setParent = new BasicDBObject("$set", new BasicDBObject("parent", parent));

            // searching children things
            BasicDBObject searchQuery = new BasicDBObject("parent._id", thingId);
            thingsCollection.update(searchQuery, setParent, false, true);
        }
    }

    /**
     * Update the nested asset in shippingOrder in asset_children array
     *
     * @param thingId       thingId
     * @param thingTypeCode thingTypeCode
     */
    public void updateShippingOrder(Long thingId, String thingTypeCode, String ttfOfTypeParent) {
        BasicDBObject query = new BasicDBObject("_id", thingId);
        DBObject asset = thingsCollection.findOne(query);

        if (asset.containsField(ttfOfTypeParent)) {
            // removing shippingOrder from the asset to prevent cycling references
            asset.removeField(ttfOfTypeParent);

            // preparing asset to be updated
            BasicDBObject setAsset = new BasicDBObject("$set", new BasicDBObject(thingTypeCode + "_children.$", asset));

            // searching asset to be updated in shippingOrder
            BasicDBObject searchQuery = new BasicDBObject(thingTypeCode + "_children",
                    new BasicDBObject("$elemMatch", new BasicDBObject("_id", thingId)));

            thingsCollection.update(searchQuery, setAsset);
        }
    }


    /**
     * Update nested ‘SO’ udf in all ‘Asset’s that reference this SO
     *
     * @param thingId              id of the thing
     * @param thingTypeCode        thingTypeCode (thingTypeCode of shippingOrder)
     * @param thingTypeCodeOfChild thingTypeCode of the asset
     * @param ttfNameOfChild       name of the field in Asset
     */
    public void updateAsset(Long thingId, String thingTypeCode, String thingTypeCodeOfChild, String ttfNameOfChild) {
        BasicDBObject query = new BasicDBObject("_id", thingId);
        DBObject shippingOrder = thingsCollection.findOne(query);

        if (shippingOrder.containsField(thingTypeCodeOfChild + "_children")) {
            // removing Asset_children from the shippingOrder to prevent cycling references
            shippingOrder.removeField(thingTypeCodeOfChild + "_children");

            // preparing shippingOrder to be updated
            BasicDBObject setShippingOrder = new BasicDBObject("$set", new BasicDBObject(ttfNameOfChild + ".value", shippingOrder));

            // searching shippingOrder to be updated in asset
            BasicDBObject searchQuery = new BasicDBObject(ttfNameOfChild + ".value._id", thingId);
            thingsCollection.update(searchQuery, setShippingOrder, false, true);
        }
    }

    public void updateThingParent(Long thingId, Long parentId, DBObject doc) {
        logger.debug("Updating child in parent thingId=" + thingId);

        if (doc.containsField("parent")) {
            DBObject parentDoc = getThing(parentId);
            //Fix bug VIZIX-230
            List<BasicDBObject> childrens = (List<BasicDBObject>) parentDoc.get("children");
            if(childrens != null){
                for (int i = 0; i < childrens.size(); i++) {
                    BasicDBObject child = childrens.get(i);
                    if (((long) child.get("_id")) == thingId) {
                        doc.removeField("parent");
                        BasicDBObject setChild = new BasicDBObject("$set", new BasicDBObject("children." + i, doc));
                        // searching children things
                        BasicDBObject searchQuery = new BasicDBObject("_id", parentId);
                        MongoDAOUtil.getInstance().things.update(searchQuery, setChild, true, true);
                    }

                }
            }
        }
    }

    /*************************************************************************
     * Remove Thing Parent
     ************************************************************************/
    public void removeInThingParent(Long thingId, Long parentId, DBObject doc) {
        logger.debug("Remove child in parent thingId=" + thingId);
        if (doc.containsField("parent")) {
            DBObject parentDoc = getThing(parentId);
            for (int i = 0; i < ((List<BasicDBObject>) parentDoc.get("children")).size(); i++) {
                BasicDBObject child = ((List<BasicDBObject>) parentDoc.get("children")).get(i);
                if (((long) child.get("_id")) == thingId.longValue()) {
                    BasicDBObject deletetChild = new BasicDBObject("$pull", new BasicDBObject("children." + i, doc));
                    // searching children things
                    BasicDBObject searchQuery = new BasicDBObject("_id", parentId);
                    MongoDAOUtil.getInstance().things.update(searchQuery, deletetChild, false, true);
                }
            }
        }
    }

    public void updateThingChildren(Long thingId, DBObject doc, Map<String, Object> thingParentMap) {
        logger.debug("Updating parent in children thingId=" + thingId);

        if (doc != null && doc.containsField("children")) {
            doc.removeField("children");
            BasicDBObject setParent = new BasicDBObject("$set", new BasicDBObject("parent", doc));

            // searching children things
            BasicDBObject searchQuery = new BasicDBObject("parent._id", thingId);
            MongoDAOUtil.getInstance().things.update(searchQuery, setParent, false, true);

            if (thingParentMap != null && thingParentMap.size() > 0) {
                this.updateThingTypeUDFParentWithChild(
                        thingId
                        , doc.get("thingTypeCode").toString()
                        , thingParentMap);
            }
        }

    }

    /*****************************************************
     * This method updates the parent of a ThingTypeUdf based on data of thing Type Children
     *
     * @param thingChildId   thingId of child
     * @param thingTypeCode  thing Type Code of child
     * @param thingParentMap It contains 'propertyName' and 'DBObject' of the thing parent
     *****************************************************/
    public void updateThingTypeUDFParentWithChild(Long thingChildId, String thingTypeCode, Map<String, Object> thingParentMap) {
        BasicDBObject query = new BasicDBObject("_id", thingChildId);
        DBObject asset = thingsCollection.findOne(query);

        if (asset.containsField(thingParentMap.get("propertyName").toString())) {
            // removing shippingOrder from the asset to prevent cycling references
            asset.removeField(thingParentMap.get("propertyName").toString());

            // preparing asset to be updated
            BasicDBObject setAsset = new BasicDBObject("$set", new BasicDBObject(thingTypeCode + "_children.$", asset));

            // searching asset to be updated in shippingOrder
            BasicDBObject searchQuery = new BasicDBObject(thingTypeCode + "_children",
                    new BasicDBObject("$elemMatch", new BasicDBObject("_id", thingChildId)));

            thingsCollection.update(searchQuery, setAsset, false, true);
        }

    }

    /**
     * This method updates the visual parent of a ThingTypeUdf based on data of thing Type Children
     *
     * @param thing             thingId of child
     * @param doc               DBObject of the thing
     * @param lstThingParentMap List of thing parent map
     */
    public void updateVisualThingParentWithChild(Thing thing, DBObject doc, List<Map<String, Object>> lstThingParentMap) {
        Long thingTypeId = null;
        String propertyName = null;
        List<String> removeFields = new ArrayList<>();
        //Delete children arrays
        for(String key: doc.keySet()){
            if( ( key.contains("children") ) && ( doc.get(key) instanceof List )  ){
                removeFields.add(key);
            }
        }
        for(String property:removeFields){
            doc.removeField(property);
        }
        //Update references
        for (Object thingParentMap : lstThingParentMap) {
            Map<String, Object> thingParent = (Map<String, Object>) thingParentMap;
            thingTypeId = thingParent.get("thingTypeId") != null ? Long.parseLong(thingParent.get("thingTypeId").toString())
                    :null;
            propertyName = thingParent.get("propertyName").toString();

            // preparing visual child to be updated
            BasicDBObject setVisualChild = new BasicDBObject("$set", new BasicDBObject(propertyName + ".value", doc));

            // Searching things Visual parents to be updated with  visual child
            List<BasicDBObject> and = new ArrayList<>();
            if (thingTypeId != null) {
                and.add(new BasicDBObject("thingTypeId", thingTypeId));
            }
            and.add(new BasicDBObject(propertyName, new BasicDBObject("$exists", true)));
            and.add(new BasicDBObject(propertyName + ".value._id", thing.getId()));
            BasicDBObject searchQuery = new BasicDBObject("$and", and);

            //globalAnd.add(searchQuery);
            int count = thingsCollection.find(searchQuery).count();
            if( count > 0 ){
                thingsCollection.update(searchQuery, setVisualChild, false, true);
            }
        }
//        logger.info("Query DATA: "+globalAnd);
//        things.update(globalAnd, setVisualChild, false, true);
    }

    /************************************************
     * This method removes thing references into thing parent udf
     ************************************************/
    public void removeInThingParentUdf(Thing thing) {
        // searching asset to be updated in shippingOrder
        BasicDBObject searchQuery = new BasicDBObject(thing.getThingType().getCode() + "_children",
                new BasicDBObject("$elemMatch", new BasicDBObject("_id", thing.getId())));

        //Delete reference
        BasicDBObject deletetChild = new BasicDBObject("$pull"
                , new BasicDBObject(thing.getThingType().getCode() + "_children", new BasicDBObject("_id", thing.getId())));

        //Execute update
        MongoDAOUtil.getInstance().things.update(searchQuery, deletetChild, false, true);

    }

    /************************************************
     * This method removes thing references into visual thing parent udf
     ************************************************/
    public void removeInVisualThingParentUdf(Thing thing, List<Map<String, Object>> lstVisualParents) {
        Long thingTypeId = null;
        String propertyName = "";
        for (Object obj : lstVisualParents) {
            Map<String, Object> visualParent = (Map<String, Object>) obj;
            thingTypeId = visualParent.get("thingTypeId") != null ?
                    Long.parseLong(visualParent.get("thingTypeId").toString()) : null;
            propertyName = visualParent.get("propertyName").toString();

            // Searching things Visual parents to be updated with  visual child
            List<BasicDBObject> and = new ArrayList<>();
            if (thingTypeId != null) {
                and.add(new BasicDBObject("thingTypeId", thingTypeId));
            }
            and.add(new BasicDBObject(propertyName, new BasicDBObject("$exists", true)));
            and.add(new BasicDBObject(propertyName + ".value._id", thing.getId()));
            BasicDBObject searchQuery = new BasicDBObject("$and", and);

            //Delete reference
            BasicDBObject deletetChild = new BasicDBObject("$unset", new BasicDBObject(propertyName, 1));

            //Execute update
            MongoDAOUtil.getInstance().things.update(searchQuery, deletetChild, false, true);

        }
    }


    /*****************************************************
     * This method updates the parent of a ThingTypeUdf based on data of thing Type Children
     *
     * @param thingParentId            thingId which is parent
     * @param lstThingTypesChildrenUdf It contains 'id' of thingTypes children as key and a sub Map
     *                                 with 'propertyName' and 'thingTypeCode' as value
     *****************************************************/
    public void updateThingTypeUDFChildrenWithParent(Long thingParentId, Map<String, Object> lstThingTypesChildrenUdf) {
        BasicDBObject query = new BasicDBObject("_id", thingParentId);
        DBObject shippingOrder = thingsCollection.findOne(query);

        for (Map.Entry<String, Object> entry : lstThingTypesChildrenUdf.entrySet()) {
            Map<String, Object> thingTypeChild = (Map<String, Object>) entry.getValue();

            if (shippingOrder.containsField(thingTypeChild.get("thingTypeCode") + "_children")) {
                // removing Asset_children from the shippingOrder to prevent cycling references
                shippingOrder.removeField(thingTypeChild.get("thingTypeCode") + "_children");

                // preparing shippingOrder to be updated
                BasicDBObject setShippingOrder = new BasicDBObject("$set"
                        , new BasicDBObject(thingTypeChild.get("propertyName") + ".value", shippingOrder));

                // searching shippingOrder to be updated in asset
                BasicDBObject searchQuery = new BasicDBObject(thingTypeChild.get("propertyName") + ".value._id", thingParentId);
                thingsCollection.update(searchQuery, setShippingOrder, false, true);

                // updating tags
                // preparing shippingOrder to be updated in tags
                BasicDBObject setShippingOrderTag = new BasicDBObject("$set"
                        , new BasicDBObject("parent." + thingTypeChild.get("propertyName") + ".value", shippingOrder));

                // searching shippingOrder to be updated in tags
                BasicDBObject searchQuery0 = new BasicDBObject("parent." + thingTypeChild.get("propertyName") + ".value._id", thingParentId);
                thingsCollection.update(searchQuery0, setShippingOrderTag, false, true);
            }
        }
    }


    /**
     * Method to remove duplicated things that are shown when exists a relation by udf thingtype
     */
    private static void removeDuplicated(Map<String, Object> queryForTree, Map<String, Object> dataResult) {
        List<Long> duplicatedThings = new ArrayList<>();
        if (queryForTree.containsKey("nativeThingTypeIsNotParentMap")) {
            // get duplicated things
            Set<Long> nativeThingTypeIsNotParentMap = (Set) queryForTree.get("nativeThingTypeIsNotParentMap");
            if (!nativeThingTypeIsNotParentMap.isEmpty() && dataResult.get("list") != null) {
                List<Map<String, Object>> list = (List) dataResult.get("list");
                for (Map<String, Object> thingMap : list) {
                    if (thingMap.get("children") != null) {
                        List<Map<String, Object>> children = (List) thingMap.get("children");
                        if (children != null) {
                            for (Map<String, Object> child : children) {
                                Long thingTypeId = (Long) child.get("thingTypeId");
                                if (thingTypeId != null) {
                                    for (Long nativeThingTypeCode : nativeThingTypeIsNotParentMap) {
                                        if (thingTypeId.compareTo(nativeThingTypeCode) == 0)
                                            duplicatedThings.add((Long) child.get("_id"));
                                    }
                                }
                            }
                        }
                    }
                }
                // remove duplicated things
                if (!duplicatedThings.isEmpty()) {
                    List<Map<String, Object>> resultList = new ArrayList<>();
                    List<Map<String, Object>> thingList = (List) dataResult.get("list");
                    for (Map<String, Object> thingMap : thingList) {
                        Long thingId = (Long) thingMap.get("_id");
                        boolean duplicated = false;
                        for (Long duplicatedThingId : duplicatedThings) {
                            if (thingId.compareTo(duplicatedThingId) == 0) {
                                duplicated = true;
                            }
                        }
                        if (!duplicated) {
                            resultList.add(thingMap);
                        }
                    }
                    dataResult.put("list", resultList);
                }
            }
        }
    }

    /**
     * Method to create index into a collection
     * @param collectionName
     * @param property
     */
    public static void createIndexInThingsCollection(String collectionName, String property){
        BasicDBObject dbObject = new BasicDBObject(property, 1);
        if(collectionName!=null && collectionName.equals(THINGS)){
            MongoDAOUtil.getInstance().things.createIndex(dbObject, property+"_");
        }else if(collectionName!=null && collectionName.equals(THING_SNAPSHOTS)){
            MongoDAOUtil.getInstance().thingSnapshots.createIndex(dbObject, property+"_");
        }else if(collectionName!=null && collectionName.equals(THING_SNAPSHOT_IDS)){
            MongoDAOUtil.getInstance().thingSnapshotIds.createIndex(dbObject, property+"_");
        }
    }

}
