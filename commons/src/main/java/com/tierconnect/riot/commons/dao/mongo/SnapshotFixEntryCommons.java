package com.tierconnect.riot.commons.dao.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import static com.mongodb.client.model.Filters.eq;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.utils.TimerUtil;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.Serializable;
import java.util.*;

/**
 * Created by cvertiz on 9/10/16.
 */

public class SnapshotFixEntryCommons implements Comparable<SnapshotFixEntryCommons>  {

    private final static Logger logger = Logger.getLogger(SnapshotFixEntryCommons.class);

    private ChangedFields thingMessage;
    private Long thingId;
    private String serialNumber;
    private Long timestamp;
    private Boolean hasPreviousSnapshot;

    private MongoCollection thingsCollection;
    private MongoCollection thingSnapshotsCollection;

    private String thingTypeCode;

    private int zoneTypeCount;
    private int zoneGroupCount;
    private int facilityMapCount;
    private Long processTimestamp;

    public SnapshotFixEntryCommons(ChangedFields thingMessage, Long thingId, String serialNumber, String thingTypeCode,
                                   Long timestamp, Boolean hasPreviousSnapshot, MongoCollection thingsCollection,
                                   MongoCollection thingSnapshotsCollection, Long processTimestamp) {
        this.thingMessage = thingMessage;
        this.thingId = thingId;
        this.serialNumber = serialNumber;
        this.timestamp = timestamp;
        this.hasPreviousSnapshot = hasPreviousSnapshot;
        this.thingTypeCode = thingTypeCode;
        this.thingsCollection = thingsCollection;
        this.thingSnapshotsCollection = thingSnapshotsCollection;
        this.processTimestamp = processTimestamp;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    private TimerUtil initTimerUtil() {
        List<String> laps = new ArrayList<>();
        laps.add("getSnapshots");
        for (String property : thingMessage.getChangedFields().keySet()) {
            if (!Constants.NON_UDFS.contains(property) && !Constants.META_FIELDS.contains(property)) {
                laps.add(property + "-Current");
                laps.add(property + "-Next");
                laps.add(property + "-Previous");
            }
        }
        laps.add("write");
        //TODO: get zone UDF from dataType
        String zoneProperty = "zone";
        if (thingMessage.getChangedFields().keySet().contains(zoneProperty)) {
            laps.add("getZonePropSnapshots");
            laps.add("getZonePropPreviousSnapshot");

            laps.add("zoneType-Changed");
            laps.add("zoneGroup-Changed");
            laps.add("facilityMap-Changed");

            laps.add(zoneProperty + "Prop-getNextUpd");
            laps.add("zoneType-getNextUpd");
            laps.add("zoneGroup-getNextUpd");
            laps.add("facilityMap-getNextUpd");

            laps.add("zoneType-Current");
            laps.add("zoneGroup-Current");
            laps.add("facilityMap-Current");

            laps.add("zoneType-Next");
            laps.add("zoneGroup-Next");
            laps.add("facilityMap-Next");

            laps.add("zoneType-Previous");
            laps.add("zoneGroup-Previous");
            laps.add("facilityMap-Previous");

            laps.add("writeZoneProp");
        }
        TimerUtil tu = new TimerUtil();
        String[] lapList = new String[laps.size()];
        lapList = laps.toArray(lapList);
        tu.initLaps(lapList);
        return tu;
    }

    void fixSnapshot(int zoneTypeCount, int zoneGroupCount, int facilityMapCount) {
        //Wait 35ms to ensure mongo operations were finished
        do {
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
            }
        } while (System.currentTimeMillis() - processTimestamp < 35L);

        this.facilityMapCount = facilityMapCount;
        this.zoneTypeCount = zoneTypeCount;
        this.zoneGroupCount = zoneGroupCount;

        TimerUtil tu = initTimerUtil();
        try {
            if(!thingMessage.getChangedFields().entrySet().isEmpty()){
                Map.Entry<String, String> zoneProp = fixSnapshotUdfs(tu);
                if (zoneProp != null) {
                    fixSnapshotZoneProperties(zoneProp, tu);
                }
            }else{
                logger.warn("Empty entry set, skipping out of order fixer thingId=" + thingId);
            }
        } catch (Exception e) {
            logger.error("ERROR", e);
        }
        logger.info("LPT.outOfOrder: " + tu.getLogString());
    }

    private Map.Entry<String, String> fixSnapshotUdfs(TimerUtil tu) {
        tu.start("getSnapshots");
        List<Document> nextSnapshots = SnapshotFixEntryUtilsCommons.getSnapshots(thingId, timestamp, "gte", -1, 1,
                null, thingSnapshotsCollection);
        tu.stop("getSnapshots");

        if (nextSnapshots.isEmpty() || ((Date) nextSnapshots.get(0).get("time")).getTime() != timestamp) {
            logger.warn("Snapshot with serialNumber " + serialNumber + " and time " + timestamp + " not found, " +
                    "details: " + thingMessage.toString());
            return null;
        }
        Document currentSnapshot = nextSnapshots.remove(0);

        Document thingsUpdate = new Document();
        Map<ObjectId, Map<String, Document>> thingSnapshotUpdate = new HashMap<>();

        Map.Entry<String, String> zoneProp = null;

        //loop each property for fixing its time and dwell
        for (Map.Entry<String, String> property : thingMessage.getChangedFields().entrySet()) {

            //Ask if property is different to null JIC
            if (property == null) {
                logger.warn("Won't fix property from snapshot because property null found "
                        + "thing=" + serialNumber
                        + " thingTypeCode=" + thingTypeCode);
                continue;
            }

            if (Constants.NON_UDFS.contains(property.getKey()) || Constants.META_FIELDS.contains(property.getKey())) {
                logger.warn("Won't fix property from snapshot because of non-UDF or meta fields property found "
                        + "thing=" + serialNumber
                        + " thingTypeCode=" + thingTypeCode
                        + " property=" + property.getKey());
                continue;
            }

            if (property.getValue() != null) {

                if (SnapshotFixEntryUtilsCommons.isPropertyZone(property.getKey(), currentSnapshot)) {
                    zoneProp = property;
                }

                try {
                    //fix dwell for current snapshot
                    tu.start(property.getKey() + "-Current");
                    fixDwellCurrentSnapshot(timestamp, property.getKey(), property.getValue(), currentSnapshot,
                            nextSnapshots, thingSnapshotUpdate);
                    tu.stop(property.getKey() + "-Current");
                } catch (Exception e) {
                    logger.warn("An exception occurred when fixing thing " + serialNumber + " property " + property
                            .getKey() + " dwell in current snapshot."
                            + " thingMessage=" + thingMessage.getChangedFields().toString(), e);
                }

                try {
                    //fix dwell for each next snapshots
                    tu.start(property.getKey() + "-Next");
                    fixDwellNextSnapshots(timestamp, property.getKey(), property.getValue(), currentSnapshot,
                            nextSnapshots, hasPreviousSnapshot, thingSnapshotUpdate, thingsUpdate);
                    tu.stop(property.getKey() + "-Next");
                } catch (Exception e) {
                    logger.warn("An exception occurred when fixing thing " + serialNumber + " property " + property
                            .getKey() + " value, time and dwell in next snapshots."
                            + " thingMessage=" + thingMessage.getChangedFields().toString(), e);
                }

                try {
                    //fix dwell previous changed snapshot
                    tu.start(property.getKey() + "-Previous");
                    if (hasPreviousSnapshot) {
                        fixDwellPreviousChangedSnapshot(timestamp, property.getKey(), thingSnapshotUpdate);
                    }
                    tu.stop(property.getKey() + "-Previous");
                } catch (Exception e) {
                    logger.warn("An exception occurred when fixing thing " + serialNumber + " property " + property
                            .getKey() + " dwell in previous snapshot."
                            + " thingMessage=" + thingMessage.getChangedFields().toString(), e);
                }
            } else {
                //TODO check alternatives for null ThingPropertyMessage
                logger.warn("Unable to fix snapshot because of null ThingPropertyValue "
                        + "thing=" + serialNumber
                        + " thingTypeCode=" + thingTypeCode
                        + " thingMessage=" + thingMessage.getChangedFields().toString());
            }
        }

        tu.start("write");
        writeMongo(thingSnapshotUpdate, thingsUpdate);
        tu.stop("write");

        return zoneProp;
    }

    private void fixSnapshotZoneProperties(Map.Entry<String, String> property, TimerUtil tu) {

        Document thingsUpdate = new Document();
        Map<ObjectId, Map<String, Document>> thingSnapshotUpdate = new HashMap<>();

        tu.start("getZonePropSnapshots");
        List<Document> nextSnapshots = SnapshotFixEntryUtilsCommons.getSnapshots(thingId, timestamp, "gte", -1, 1,
                null, thingSnapshotsCollection);
        tu.stop("getZonePropSnapshots");

        tu.start("getZonePropPreviousSnapshot");
        Document previousSnapshot = SnapshotFixEntryUtilsCommons.getPreviousSnapshot(thingId,
                timestamp ,null , thingSnapshotsCollection);
        tu.stop("getZonePropPreviousSnapshot");

        if (nextSnapshots.isEmpty() || ((Date) nextSnapshots.get(0).get("time")).getTime() != timestamp) {
            logger.warn("Snapshot with serialNumber " + serialNumber + " and time " + timestamp + " not found, " +
                    "details: " + thingMessage.toString());
            return;
        }
        Document currentSnapshot = nextSnapshots.remove(0);

        if (SnapshotFixEntryUtilsCommons.isPropertyZone(property.getKey(), currentSnapshot)) {

            try {
                fixChangedZoneProperties(property.getKey(), currentSnapshot, nextSnapshots, tu);
            } catch (Exception e) {
                logger.warn("An exception occurred when fixing thing " + serialNumber + " property zone " +
                        "properties for udf " + property.getKey() + " changed in next snapshot.", e);
            }

            tu.start(property.getKey() + "Prop-getNextUpd");
            nextSnapshots = SnapshotFixEntryUtilsCommons.getSnapshots(thingId, timestamp, "gt", -1, 1, null,
                    thingSnapshotsCollection);
            tu.stop(property.getKey() + "Prop-getNextUpd");
            try {
                fixDwellCurrentZoneProperties(timestamp, property.getKey(), currentSnapshot, nextSnapshots,
                        thingSnapshotUpdate, previousSnapshot, tu);
            } catch (Exception e) {
                logger.warn("An exception occurred when fixing thing " + serialNumber + " zone properties for udf" +
                        " " + property.getKey() + " dwell in current snapshot.", e);
            }

            try {
                fixDwellNextZoneProperties(timestamp, property.getKey(), currentSnapshot,
                        nextSnapshots, thingSnapshotUpdate, thingsUpdate, previousSnapshot, tu);
            } catch (Exception e) {
                logger.warn("An exception occurred when fixing thing " + serialNumber + " zone properties for udf" +
                        " " + property.getKey() + " dwell in next snapshots.", e);
            }

            try {
                fixDwellPreviousZoneProperties(timestamp, property.getKey(), currentSnapshot, nextSnapshots,
                        thingSnapshotUpdate, previousSnapshot, tu);
            } catch (Exception e) {
                logger.warn("An exception occurred when fixing thing " + serialNumber + " for zone properties for" +
                        " udf " + property.getKey() + " dwell in previous snapshots.", e);
            }
        }

        tu.start("writeZoneProp");
        writeMongo(thingSnapshotUpdate, thingsUpdate);
        tu.stop("writeZoneProp");

    }

    private void writeMongo(Map<ObjectId, Map<String, Document>> thingSnapshotUpdate,
                            Document thingsUpdate) {

        try {
            if (!thingSnapshotUpdate.isEmpty()) {
                List<Object> bulkOperations = new ArrayList<>();
                boolean hasBulkItems = false;

                Map<ObjectId, Document> set = new HashMap<>();
                Map<ObjectId, Document> unset = new HashMap<>();
                for (Map.Entry<ObjectId, Map<String, Document>> snapshot : thingSnapshotUpdate.entrySet()) {
                    if (snapshot.getValue().containsKey("$set")) {
                        set.put(snapshot.getKey(), snapshot.getValue().get("$set"));
                    }
                    if (snapshot.getValue().containsKey("$unset")) {
                        unset.put(snapshot.getKey(), snapshot.getValue().get("$unset"));
                    }
                }

                for (Map.Entry<ObjectId, Document> setValue : set.entrySet()) {
                    Document objectUpdates = new Document();
                    for (Map.Entry<String, Object> field : setValue.getValue().entrySet()) {
                        objectUpdates.append(field.getKey(), field.getValue());
                    }
                    if (!objectUpdates.isEmpty()) {
                        bulkOperations.add(new UpdateOneModel<>(new Document("_id", setValue.getKey()),
                                new Document("$set", objectUpdates)));
                        hasBulkItems = true;
                    }
                }

                for (Map.Entry<ObjectId, Document> unsetValue : unset.entrySet()) {
                    Document objectUpdates = new Document();
                    for (Map.Entry<String, Object> field : unsetValue.getValue().entrySet()) {
                        objectUpdates.append(field.getKey(), field.getValue());
                    }
                    if (!objectUpdates.isEmpty()) {
                        bulkOperations.add(new UpdateOneModel<>(new Document("_id", unsetValue.getKey()),
                                new Document("$unset", objectUpdates)));
                        hasBulkItems = true;
                    }
                }
                if (hasBulkItems) {
                    thingSnapshotsCollection.bulkWrite(bulkOperations);
                }
            }
        } catch (Exception e) {
            logger.warn("An exception occurred when executing bulk updating for thing " + serialNumber + " in " +
                    "snapshots. Thing with Id " + thingId, e);
        }

        try {
            if (!thingsUpdate.entrySet().isEmpty()) {
                thingsCollection.updateOne(new Document("_id", thingId), new Document("$set", thingsUpdate), new
                        UpdateOptions().upsert(true));
            }
        } catch (Exception e) {
            logger.warn("An exception occurred when updating thing " + serialNumber + " with Id " + thingId, e);
        }
    }

    /**
     * UDFS
     **/

    private void fixDwellCurrentSnapshot(Long currentSnapshotTimestamp,
                                         String propertyName,
                                         String propertyValue,
                                         Document currentSnapshot,
                                         List<Document> nextSnapshots,
                                         Map<ObjectId, Map<String, Document>> thingSnapshotUpdate) {

        Long nextChangedSnapshotDate = SnapshotFixEntryUtilsCommons.getNextChangedDate(propertyValue, propertyName,
                nextSnapshots, timestamp);

        Long currentPropertyTTFId = SnapshotFixEntryUtilsCommons.getSnapshotPropertyTTFId(propertyName, currentSnapshot, null);

        //extract current snapshot id
        ObjectId currentSnapshotId = (ObjectId) currentSnapshot.get("_id");

        Long newDwell = nextChangedSnapshotDate - currentSnapshotTimestamp;

        if (!thingSnapshotUpdate.containsKey(currentSnapshotId)) {
            thingSnapshotUpdate.put(currentSnapshotId, new HashMap<>());
        }
        if (!thingSnapshotUpdate.get(currentSnapshotId).containsKey("$set")) {
            thingSnapshotUpdate.get(currentSnapshotId).put("$set", new Document());
        }
        thingSnapshotUpdate.get(currentSnapshotId).get("$set")
                .append("value." + propertyName + ".thingTypeFieldId", currentPropertyTTFId)
                .append("value." + propertyName + ".dwellTime", newDwell)
                .append("value." + propertyName + ".time", new Date(currentSnapshotTimestamp))
                .append("value." + propertyName + ".changed", true)
                .append("value." + propertyName + ".blinked", true);
    }


    private void fixDwellNextSnapshots(Long currentSnapshotTimestamp,
                                       String propertyName,
                                       String propertyValue,
                                       Document currentSnapshot,
                                       List<Document> nextSnapshots,
                                       Boolean hasPreviousSnapshot,
                                       Map<ObjectId, Map<String, Document>> thingSnapshotUpdate,
                                       Document thingUpdate) {

        Long currentSnapPropTTFId = SnapshotFixEntryUtilsCommons.getSnapshotPropertyTTFId(propertyName, currentSnapshot, null);

        //Get current snapshot property value as object
        Object currentSnapPropValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName, currentSnapshot);

        //get value from property value as object
        String currentPropertyValue = SnapshotFixEntryUtilsCommons.getValueFromProperty(currentSnapPropValueObject);

        boolean isFirstNext = true;
        boolean lastSnapshotReached = true;
        boolean blinkedReached = false;
        for (Document nextSnapshot : nextSnapshots) {

            //extract next snapshot date
            ObjectId nextSnapshotId = (ObjectId) nextSnapshot.get("_id");

            //extract next snapshot property value
            Long nextSnapshotTimestamp = SnapshotFixEntryUtilsCommons.getSnapshotTime(nextSnapshot).getTime();

            //extract next snapshot property value
            Object nextSnapPropValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName,
                    nextSnapshot);

            Document propValue = (Document)((Document) nextSnapshot.get("value")).get(propertyName);
            if (propValue != null
                    && propValue.get("blinked") != null && Boolean.parseBoolean(propValue.get("blinked").toString())
                    && (currentSnapPropValueObject != null
                    && !currentSnapPropValueObject.equals(nextSnapPropValueObject)
                    || nextSnapPropValueObject != null
                    && !nextSnapPropValueObject.equals(currentSnapPropValueObject))
                    && !blinkedReached) {
                blinkedReached = true;
                if (propValue.get("changed") == null || !Boolean.parseBoolean(propValue.get("changed").toString())) {

                    if (!thingSnapshotUpdate.containsKey(nextSnapshotId)) {
                        thingSnapshotUpdate.put(nextSnapshotId, new HashMap<>());
                    }
                    if (!thingSnapshotUpdate.get(nextSnapshotId).containsKey("$set")) {
                        thingSnapshotUpdate.get(nextSnapshotId).put("$set", new Document());
                    }
                    Long nextBlinkedDwell = 0L;
                    List<Document> nextChanged = SnapshotFixEntryUtilsCommons.getSnapshots(thingId,
                            currentSnapshotTimestamp, "gt", 1, -1, "value." + propertyName + ".changed",
                            thingSnapshotsCollection);
                    if (nextChanged.size()>0){
                        nextBlinkedDwell = ((Date) nextChanged.get(0).get("time")).getTime() -
                                ((Date) nextSnapshot.get("time")).getTime();
                    }
                    thingSnapshotUpdate.get(nextSnapshotId).get("$set")
                            .append("value." + propertyName + ".thingTypeFieldId", currentSnapPropTTFId)
                            .append("value." + propertyName + ".changed", true)
                            .append("value." + propertyName + ".blinked", true)
                            .append("value." + propertyName + ".time", nextSnapshot.get("time"))
                            .append("value." + propertyName + ".dwellTime", nextBlinkedDwell);

                    currentSnapshot = new Document(nextSnapshot);
                    currentSnapPropValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName,
                            currentSnapshot);
                    propertyValue = currentPropertyValue = SnapshotFixEntryUtilsCommons.getValueFromProperty(currentSnapPropValueObject);
                    currentSnapshotTimestamp = nextSnapshot.getDate("time").getTime();

                    continue;
                }
            }

            String nextSnapPropValue = null;
            if (nextSnapPropValueObject != null) {
                nextSnapPropValue = SnapshotFixEntryUtilsCommons.getValueFromProperty(nextSnapPropValueObject);
            }

            boolean nextPropertyChanged = SnapshotFixEntryUtilsCommons.getChangedFromProperty(propertyName,
                    nextSnapshot);

            //TODO move to root function
            if (propertyValue == null) {
                logger.warn("Property value is null for thing=" + serialNumber + ", property=" + propertyName);
            }
            logger.info("Property Value=" + propertyValue + ", serialNumber=" + serialNumber + ", property=" +
                    propertyName);


            if (nextSnapPropValue == null //when next snapshot does not contain field, propagate
                    || nextSnapPropValue.equals(currentPropertyValue) //when next snapshot value is equals to current
                    // snapshot, propagate
                    || nextSnapPropValue.equals(propertyValue) // when next snapshot value is equals to incoming
                    // value, keep propagating
                    || (isFirstNext && hasPreviousSnapshot && !nextPropertyChanged)) {// when older snapshot is
                // between two equal snapshot value

                isFirstNext = false;

                Long newDwell = nextSnapshotTimestamp - currentSnapshotTimestamp;

                if (!thingSnapshotUpdate.containsKey(nextSnapshotId)) {
                    thingSnapshotUpdate.put(nextSnapshotId, new HashMap<>());
                }
                if (!thingSnapshotUpdate.get(nextSnapshotId).containsKey("$set")) {
                    thingSnapshotUpdate.get(nextSnapshotId).put("$set", new Document());
                }
                thingSnapshotUpdate.get(nextSnapshotId).get("$set").append("value." + propertyName + ".dwellTime", newDwell)
                        .append("value." + propertyName + ".thingTypeFieldId", currentSnapPropTTFId)
                        .append("value." + propertyName + ".time", new Date(currentSnapshotTimestamp))
//                        .append("value." + propertyName + ".value", currentSnapPropValueObject)
                        .append("value." + propertyName + ".changed", false);

                if(SnapshotFixEntryUtilsCommons.isPropertyZone(propertyName, currentSnapshot)){
                    for(Map.Entry<String,Object> zoneProperty : ((Document) currentSnapPropValueObject).entrySet()){
                        if(!zoneProperty.getKey().contains("Dwell")){
                            thingSnapshotUpdate.get(nextSnapshotId).get("$set").append("value." + propertyName + ".value." + zoneProperty.getKey(), zoneProperty.getValue());
                        }
                    }
                }else{
                    thingSnapshotUpdate.get(nextSnapshotId).get("$set").append("value." + propertyName + ".value", currentSnapPropValueObject);
                }
                if (!thingSnapshotUpdate.get(nextSnapshotId).get("$set")
                        .containsKey("value." + propertyName + ".blinked")){
                    thingSnapshotUpdate.get(nextSnapshotId).get("$set")
                            .append("value." + propertyName + ".blinked", propValue != null ? propValue.get("blinked") : false);
                }

            } else {
                lastSnapshotReached = false;
                break;
            }

            currentPropertyValue = nextSnapPropValue;
        }

        if (lastSnapshotReached) {

            Document lastValue = (Document) thingsCollection.find(eq("_id", thingId)).first();

            Long currentLastValueFieldDate = thingMessage.getCurrentFieldDate(propertyName);
            Long lastValueFieldDate = lastValue.get(propertyName) != null ?
                ((Date) ((Document) lastValue.get(propertyName)).get("time")).getTime() : null;

            boolean isCurrentNewerThanField = thingMessage.getCurrentFieldDate(propertyName) != null
                    && currentSnapshotTimestamp >= thingMessage.getCurrentFieldDate(propertyName);

            boolean isCurrentNewerThanLast = lastValueFieldDate == null || currentLastValueFieldDate == null
                    || currentLastValueFieldDate >= lastValueFieldDate;

            if (isCurrentNewerThanField || isCurrentNewerThanLast) {

                if (SnapshotFixEntryUtilsCommons.isPropertyZone(propertyName, currentSnapshot)) {
                    Document zoneValue = new Document(((Document) currentSnapPropValueObject));
                    zoneValue.remove("zoneTypeDwellTime");
                    zoneValue.remove("zoneGroupDwellTime");
                    zoneValue.remove("facilityMapDwellTime");
                    zoneValue.remove("zoneTypeChanged");
                    zoneValue.remove("zoneGroupChanged");
                    zoneValue.remove("facilityMapChanged");
                    zoneValue.remove("zoneTypeBlinked");
                    zoneValue.remove("zoneGroupBlinked");
                    zoneValue.remove("facilityMapBlinked");
                    currentSnapPropValueObject = zoneValue;
                }
                thingUpdate
                    .append(propertyName + ".thingTypeFieldId", currentSnapPropTTFId)
                            .append(propertyName + ".time", new Date(currentSnapshotTimestamp))
                            .append(propertyName + ".value", currentSnapPropValueObject);
            }
        }
    }


    private void fixDwellPreviousChangedSnapshot(Long currentSnapshotTimestamp,
                                                 String propertyName,
                                                 Map<ObjectId, Map<String, Document>> thingSnapshotUpdate) {

            Document previousChangedSnapshot = SnapshotFixEntryUtilsCommons.getPreviousSnapshot(thingId,
                currentSnapshotTimestamp,"value." + propertyName + ".changed", thingSnapshotsCollection);
        if (previousChangedSnapshot == null) {
            return;
        }

        //extract next snapshot Object Id
        ObjectId previousChangedSnapshotId = (ObjectId) previousChangedSnapshot.get("_id");

        //extract next snapshot date
        Object previousChangedSnapshotPropValue = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue
                (propertyName, previousChangedSnapshot);

        //TO fix inconsistencies
        Date previousChangedSnapshotPropTime = SnapshotFixEntryUtilsCommons.getSnapshotPropertyTime(propertyName,previousChangedSnapshot);
        if (previousChangedSnapshotPropTime == null) {
            if (!thingSnapshotUpdate.containsKey(previousChangedSnapshotId)) {
                thingSnapshotUpdate.put(previousChangedSnapshotId, new HashMap<>());
            }
            if (!thingSnapshotUpdate.get(previousChangedSnapshotId).containsKey("$set")) {
                thingSnapshotUpdate.get(previousChangedSnapshotId).put("$set", new Document());
            }
            Date previousChangedSnapshotTime = (Date) previousChangedSnapshot.get("time");
            thingSnapshotUpdate.get(previousChangedSnapshotId).get("$set")
                    .append("value." + propertyName + ".time", previousChangedSnapshotTime);
            if (previousChangedSnapshotPropValue == null) {
                thingSnapshotUpdate.get(previousChangedSnapshotId).get("$set")
                        .append("value." + propertyName + ".value", null);
            }
        }

        Long prevChangedSnapTimestamp = SnapshotFixEntryUtilsCommons.getSnapshotTime(previousChangedSnapshot).getTime();

        Long newPreviousChangedDwell = currentSnapshotTimestamp - prevChangedSnapTimestamp;

        if (!thingSnapshotUpdate.containsKey(previousChangedSnapshotId)) {
            thingSnapshotUpdate.put(previousChangedSnapshotId, new HashMap<>());
        }
        if (!thingSnapshotUpdate.get(previousChangedSnapshotId).containsKey("$set")) {
            thingSnapshotUpdate.get(previousChangedSnapshotId).put("$set", new Document());
        }
        thingSnapshotUpdate.get(previousChangedSnapshotId).get("$set")
                .append("value." + propertyName + ".dwellTime", newPreviousChangedDwell);
    }

    /**
     * Zone Properties
     */

    private void fixChangedZoneProperties(String propertyName,
                                          Document currentSnapshot,
                                          List<Document> nextSnapshots,
                                          TimerUtil tu) {

        Map<ObjectId, Map<String, Document>> thingSnapshotUpdate = new HashMap<>();

        tu.start("facilityMap-Changed");
        fixChangedZoneProperty("facilityMap", propertyName, currentSnapshot, nextSnapshots, thingSnapshotUpdate);
        tu.stop("facilityMap-Changed");
        tu.start("zoneGroup-Changed");
        fixChangedZoneProperty("zoneGroup", propertyName, currentSnapshot, nextSnapshots, thingSnapshotUpdate);
        tu.stop("zoneGroup-Changed");
        tu.start("zoneType-Changed");
        fixChangedZoneProperty("zoneType", propertyName, currentSnapshot, nextSnapshots, thingSnapshotUpdate);
        tu.stop("zoneType-Changed");

        if (!thingSnapshotUpdate.isEmpty()) {
            for (Map.Entry<ObjectId, Map<String, Document>> snapshot : thingSnapshotUpdate.entrySet()) {
                thingSnapshotsCollection.updateOne(new Document("_id", snapshot.getKey()),
                        new Document("$set", snapshot.getValue().get("$set")));
            }
        }
    }

    private void fixChangedZoneProperty(String zonePropertyName,
                                        String propertyName,
                                        Document currentSnapshot,
                                        List<Document> nextSnapshots,
                                        Map<ObjectId, Map<String, Document>> thingSnapshotUpdate) {

        Object currentPropertyValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName,
                currentSnapshot);

        String zonePropertyValue = null;
        if (currentPropertyValueObject != null) {
            zonePropertyValue = ((Document) currentPropertyValueObject).get(zonePropertyName).toString();
        }

        for (Document nextSnapshot : nextSnapshots) {

            //extract next snapshot date
            ObjectId nextSnapshotId = (ObjectId) nextSnapshot.get("_id");

            //extract next snapshot property value
            Object snapshotPropertyValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName,
                    nextSnapshot);

            String snapshotZonePropertyValue = "";
            if (snapshotPropertyValueObject != null && ((Document) snapshotPropertyValueObject).containsKey
                    (zonePropertyName)) {
                snapshotZonePropertyValue = ((Document) snapshotPropertyValueObject).get(zonePropertyName).toString();
            }

            if (!zonePropertyValue.equals(snapshotZonePropertyValue)) {

                if (!thingSnapshotUpdate.containsKey(nextSnapshotId)) {
                    thingSnapshotUpdate.put(nextSnapshotId, new HashMap<>());
                }
                if (!thingSnapshotUpdate.get(nextSnapshotId).containsKey("$set")) {
                    thingSnapshotUpdate.get(nextSnapshotId).put("$set", new Document());
                }
                thingSnapshotUpdate.get(nextSnapshotId).get("$set").append("value." + propertyName + ".value." +
                        zonePropertyName + "Changed", true);
                thingSnapshotUpdate.get(nextSnapshotId).get("$set").append("value." + propertyName + ".value." +
                        zonePropertyName + "Blinked", true);
                break;
            }
        }
    }

    private void fixDwellCurrentZoneProperties(Long timestamp, String propertyName,
                                               Document currentSnapshot,
                                               List<Document> nextSnapshots,
                                               Map<ObjectId, Map<String, Document>> thingSnapshotUpdate,
                                               Document previousSnapshot,
                                               TimerUtil tu) {

        Object nextPropertyValueObject = null;
        if(!nextSnapshots.isEmpty()){
            nextPropertyValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName,
                    nextSnapshots.get(0));
        }

        Object prevPropertyValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName, previousSnapshot);
        Object currentPropertyValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName, currentSnapshot);

        //FACILITY MAP
        tu.start("facilityMap-Current");
        String prevFacilityMapValue = prevPropertyValueObject != null ? (String) ((Document)prevPropertyValueObject).get("facilityMap") : "";
        String currentFacilityMapValue = currentPropertyValueObject != null ? (String) ((Document)currentPropertyValueObject).get("facilityMap") : "";
        String nextFacilityMapValue = nextPropertyValueObject != null ? (String) ((Document)nextPropertyValueObject).get("facilityMap") : "";
        boolean propagateFacilityMap = prevFacilityMapValue.equals(currentFacilityMapValue) && currentFacilityMapValue.equals(nextFacilityMapValue);

        if(!propagateFacilityMap || facilityMapCount > 1){
            fixDwellCurrentZoneProperty(timestamp, "facilityMap", propertyName, currentSnapshot, nextSnapshots,
                    thingSnapshotUpdate);
        }
        tu.stop("facilityMap-Current");

        //ZONE TYPE
        tu.start("zoneType-Current");
        String prevZoneTypeValue = prevPropertyValueObject != null ? (String) ((Document)prevPropertyValueObject).get("zoneType") : "";
        String currentZoneTypeValue = currentPropertyValueObject != null ? (String) ((Document)currentPropertyValueObject).get("zoneGroup") : "";
        String nextZoneTypeValue = nextPropertyValueObject != null ? (String) ((Document)nextPropertyValueObject).get("zoneType") : "";
        boolean propagateZoneType = prevZoneTypeValue.equals(currentZoneTypeValue) && currentZoneTypeValue.equals(nextZoneTypeValue);
        if(!propagateZoneType || zoneTypeCount > 1){
            fixDwellCurrentZoneProperty(timestamp, "zoneType", propertyName, currentSnapshot, nextSnapshots,
                    thingSnapshotUpdate);
        }
        tu.stop("zoneType-Current");

        //ZONE GROUP
        tu.start("zoneGroup-Current");
        String prevZoneGroupValue = prevPropertyValueObject != null ? (String) ((Document)prevPropertyValueObject).get("zoneGroup") : "";
        String currentZoneGroupValue = currentPropertyValueObject != null ? (String) ((Document)currentPropertyValueObject).get("zoneType") : "";
        String nextZoneGroupValue = nextPropertyValueObject != null ? (String) ((Document)nextPropertyValueObject).get("zoneGroup") : "";
        boolean propagateZoneGroup = prevZoneGroupValue.equals(currentZoneGroupValue) && currentZoneGroupValue.equals(nextZoneGroupValue);
        if(!propagateZoneGroup || zoneGroupCount > 1){
            fixDwellCurrentZoneProperty(timestamp, "zoneGroup", propertyName, currentSnapshot, nextSnapshots,
                    thingSnapshotUpdate);
        }
        tu.stop("zoneGroup-Current");
    }

    private void fixDwellCurrentZoneProperty(Long timestamp,
                                             String zonePropertyName,
                                             String propertyName,
                                             Document currentSnapshot,
                                             List<Document> nextSnapshots,
                                             Map<ObjectId, Map<String, Document>> thingSnapshotUpdate) {

        Date currentSnapshotDate = new Date(timestamp);

        //extract current snapshot id
        ObjectId currentSnapshotId = (ObjectId) currentSnapshot.get("_id");

        Object snapshotPropertyValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName,
                currentSnapshot);

        Boolean currentZonePropertyChanged = ((Document) snapshotPropertyValueObject).containsKey(zonePropertyName +
                "Changed")
                && Boolean.parseBoolean(((Document) snapshotPropertyValueObject).get(zonePropertyName + "Changed")
                .toString());

        String snapshotPropertyValue = null;
        if (snapshotPropertyValueObject != null) {
            snapshotPropertyValue = ((Document) snapshotPropertyValueObject).get(zonePropertyName).toString();
        }

        Long newDwell = 0L;
        Date newDate = new Date(timestamp);
        if (currentZonePropertyChanged) {
            Date nextChangedSnapshotDate = SnapshotFixEntryUtilsCommons.getNextChangedZonePropertyDate(propertyName,
                    zonePropertyName, snapshotPropertyValue, nextSnapshots);
            if (nextChangedSnapshotDate == null) {
                nextChangedSnapshotDate = new Date(timestamp);
            }
            newDwell = nextChangedSnapshotDate.getTime() - currentSnapshotDate.getTime();
            newDate = currentSnapshotDate;
        } else {

            Document previousChangedSnapshot = SnapshotFixEntryUtilsCommons.getPreviousSnapshot(thingId, timestamp,
                    "value." + propertyName + ".value." + zonePropertyName + "Changed", thingSnapshotsCollection);
            if (previousChangedSnapshot != null) {
                Date previousChangedSnapshotTime = (Date) previousChangedSnapshot.get("time");
                newDwell = currentSnapshotDate.getTime() - previousChangedSnapshotTime.getTime();
                newDate = previousChangedSnapshotTime;
            }
        }
        if (!thingSnapshotUpdate.containsKey(currentSnapshotId)) {
            thingSnapshotUpdate.put(currentSnapshotId, new HashMap<>());
        }
        if (!thingSnapshotUpdate.get(currentSnapshotId).containsKey("$set")) {
            thingSnapshotUpdate.get(currentSnapshotId).put("$set", new Document());
        }
        thingSnapshotUpdate.get(currentSnapshotId).get("$set").append("value." + propertyName + ".value." +
                zonePropertyName + "DwellTime", newDwell);
        thingSnapshotUpdate.get(currentSnapshotId).get("$set").append("value." + propertyName + ".value." +
                zonePropertyName + "Time", newDate);
    }


    private void fixDwellNextZoneProperties(Long timestamp,
                                            String propertyName,
                                            Document currentSnapshot,
                                            List<Document> nextSnapshots,
                                            Map<ObjectId, Map<String, Document>> thingSnapshotUpdate,
                                            Document thingUpdate,
                                            Document previousSnapshot,
                                            TimerUtil tu) {

        Object nextPropertyValueObject = null;
        if(!nextSnapshots.isEmpty()){
            nextPropertyValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName,
                    nextSnapshots.get(0));
        }

        Object prevPropertyValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName, previousSnapshot);
        Object currentPropertyValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName, currentSnapshot);

        //FACILITY MAP
        tu.start("facilityMap-Next");
        String prevFacilityMapValue = prevPropertyValueObject != null ? (String) ((Document)prevPropertyValueObject).get("facilityMap") : "";
        String currentFacilityMapValue = currentPropertyValueObject != null ? (String) ((Document)currentPropertyValueObject).get("facilityMap") : "";
        String nextFacilityMapValue = nextPropertyValueObject != null ? (String) ((Document)nextPropertyValueObject).get("facilityMap") : "";
        boolean propagateFacilityMap = prevFacilityMapValue.equals(currentFacilityMapValue) && currentFacilityMapValue.equals(nextFacilityMapValue);

        if(!propagateFacilityMap || facilityMapCount > 1){
            fixDwellNextZoneProperty(timestamp, "facilityMap", propertyName, currentSnapshot, nextSnapshots,
                    thingSnapshotUpdate, thingUpdate, true);
        }
        tu.stop("facilityMap-Next");

        //ZONE TYPE
        tu.start("zoneType-Next");
        String prevZoneTypeValue = prevPropertyValueObject != null ? (String) ((Document)prevPropertyValueObject).get("zoneType") : "";
        String currentZoneTypeValue = currentPropertyValueObject != null ? (String) ((Document)currentPropertyValueObject).get("zoneGroup") : "";
        String nextZoneTypeValue = nextPropertyValueObject != null ? (String) ((Document)nextPropertyValueObject).get("zoneType") : "";
        boolean propagateZoneType = prevZoneTypeValue.equals(currentZoneTypeValue) && currentZoneTypeValue.equals(nextZoneTypeValue);
        if(!propagateZoneType || zoneTypeCount > 1){
            fixDwellNextZoneProperty(timestamp, "zoneType", propertyName, currentSnapshot, nextSnapshots,
                    thingSnapshotUpdate, thingUpdate, true);
        }
        tu.stop("zoneType-Next");

        //ZONE GROUP
        tu.start("zoneGroup-Next");
        String prevZoneGroupValue = prevPropertyValueObject != null ? (String) ((Document)prevPropertyValueObject).get("zoneGroup") : "";
        String currentZoneGroupValue = currentPropertyValueObject != null ? (String) ((Document)currentPropertyValueObject).get("zoneType") : "";
        String nextZoneGroupValue = nextPropertyValueObject != null ? (String) ((Document)nextPropertyValueObject).get("zoneGroup") : "";
        boolean propagateZoneGroup = prevZoneGroupValue.equals(currentZoneGroupValue) && currentZoneGroupValue.equals(nextZoneGroupValue);
        if(!propagateZoneGroup || zoneGroupCount > 1){
            fixDwellNextZoneProperty(timestamp, "zoneGroup", propertyName, currentSnapshot, nextSnapshots,
                    thingSnapshotUpdate, thingUpdate, true);
        }
        tu.stop("zoneGroup-Next");



    }


    private void fixDwellNextZoneProperty(Long timestamp,
                                          String zonePropertyName,
                                          String propertyName,
                                          Document currentSnapshot,
                                          List<Document> nextSnapshots,
                                          Map<ObjectId, Map<String, Document>> thingSnapshotUpdate,
                                          Document thingUpdate, boolean recusively) {

        Object currentPropertyValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName,
                currentSnapshot);

        String zonePropertyValue = null;
        if (currentPropertyValueObject != null) {
            zonePropertyValue = ((Document) currentPropertyValueObject).get(zonePropertyName).toString();
        }

        Date currentSnapshotDate = new Date(timestamp);
        Boolean isCurrentZonePropertyChanged = ((Document) currentPropertyValueObject).containsKey(zonePropertyName +
                "Changed")
                && Boolean.parseBoolean(((Document) currentPropertyValueObject).get(zonePropertyName + "Changed")
                .toString());

        if (!isCurrentZonePropertyChanged) {
            Document previousChangedSnapshot = SnapshotFixEntryUtilsCommons.getPreviousSnapshot(thingId, timestamp,
                    "value." + propertyName + ".value." + zonePropertyName + "Changed", thingSnapshotsCollection);
            if (previousChangedSnapshot != null) {
                currentSnapshotDate = (Date) previousChangedSnapshot.get("time");
            }
        }

        Object currentZonePropertyValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName,
                currentSnapshot);
        String currentZonePropertyValue = ((Document) currentZonePropertyValueObject).get(zonePropertyName).toString();

        boolean lastSnapshotReached = true;
        for (Document nextSnapshot : nextSnapshots) {

            //extract next snapshot date
            ObjectId nextSnapshotId = (ObjectId) nextSnapshot.get("_id");

            //extract next snapshot property value
            Date nextSnapshotDate = SnapshotFixEntryUtilsCommons.getSnapshotTime(nextSnapshot);

            //extract next snapshot property value
            Object snapshotPropertyValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName,
                    nextSnapshot);

            String snapshotZonePropertyValue = "";
            if (snapshotPropertyValueObject != null && ((Document) snapshotPropertyValueObject).containsKey
                    (zonePropertyName)) {
                snapshotZonePropertyValue = ((Document) snapshotPropertyValueObject).get(zonePropertyName).toString();
            }

            if (!thingSnapshotUpdate.containsKey(nextSnapshotId)) {
                thingSnapshotUpdate.put(nextSnapshotId, new HashMap<>());
            }
            if (!thingSnapshotUpdate.get(nextSnapshotId).containsKey("$set")) {
                thingSnapshotUpdate.get(nextSnapshotId).put("$set", new Document());
            }

            if (zonePropertyValue.equals(snapshotZonePropertyValue)) {

                Long newDwell = nextSnapshotDate.getTime() - currentSnapshotDate.getTime();

                thingSnapshotUpdate.get(nextSnapshotId).get("$set").append("value." + propertyName + ".value." +
                        zonePropertyName + "DwellTime", newDwell)
                        .append("value." + propertyName + ".value." + zonePropertyName + "Time", currentSnapshotDate)
                        .append("value." + propertyName + ".value." + zonePropertyName, zonePropertyValue)
                        .append("value." + propertyName + ".value." + zonePropertyName + "Changed", false);
            } else {
                thingSnapshotUpdate.get(nextSnapshotId).get("$set").append("value." + propertyName + ".value." +
                        zonePropertyName + "Changed", true);
                thingSnapshotUpdate.get(nextSnapshotId).get("$set").append("value." + propertyName + ".value." +
                        zonePropertyName + "Blinked", true);
                if (recusively) {
                    List<Document> subNextSnapshots = new ArrayList<>();
                    if (nextSnapshots.indexOf(nextSnapshot) + 1 <= nextSnapshots.size()) {
                        subNextSnapshots = nextSnapshots.subList(nextSnapshots.indexOf(nextSnapshot) + 1, nextSnapshots
                                .size());
                    }
                    fixDwellCurrentZoneProperty(nextSnapshotDate.getTime(), zonePropertyName, propertyName,
                            nextSnapshot, subNextSnapshots, thingSnapshotUpdate);
                    fixDwellNextZoneProperty(nextSnapshotDate.getTime(), zonePropertyName, propertyName,
                            nextSnapshot, subNextSnapshots, thingSnapshotUpdate, thingUpdate, false);
                }
                lastSnapshotReached = false;
                break;
            }

            currentZonePropertyValue = snapshotZonePropertyValue;
        }

        if (lastSnapshotReached) {
            thingUpdate.append(propertyName + ".value." + zonePropertyName + "Time", currentSnapshotDate)
                    .append(propertyName + ".value." + zonePropertyName, currentZonePropertyValue);
        }
    }

    private void fixDwellPreviousZoneProperties(Long timestamp,
                                                String propertyName,
                                                Document currentSnapshot,
                                                List<Document> nextSnapshots,
                                                Map<ObjectId, Map<String, Document>> thingSnapshotUpdate,
                                                Document previousSnapshot,
                                                TimerUtil tu) {

        Object nextPropertyValueObject = null;
        if(!nextSnapshots.isEmpty()){
            nextPropertyValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName,
                    nextSnapshots.get(0));
        }

        Object prevPropertyValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName, previousSnapshot);
        Object currentPropertyValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName, currentSnapshot);

        //FACILITY MAP
        tu.start("facilityMap-Previous");
        String prevFacilityMapValue = prevPropertyValueObject != null ? (String) ((Document)prevPropertyValueObject).get("facilityMap") : "";
        String currentFacilityMapValue = currentPropertyValueObject != null ? (String) ((Document)currentPropertyValueObject).get("facilityMap") : "";
        String nextFacilityMapValue = nextPropertyValueObject != null ? (String) ((Document)nextPropertyValueObject).get("facilityMap") : "";
        boolean propagateFacilityMap = prevFacilityMapValue.equals(currentFacilityMapValue) && currentFacilityMapValue.equals(nextFacilityMapValue);

        if(!propagateFacilityMap || facilityMapCount > 1){
            fixDwellPreviousZoneProperty("facilityMap", timestamp, propertyName, currentSnapshot, nextSnapshots,
                    thingSnapshotUpdate);
        }
        tu.stop("facilityMap-Previous");

        //ZONE TYPE
        tu.start("zoneType-Previous");
        String prevZoneTypeValue = prevPropertyValueObject != null ? (String) ((Document)prevPropertyValueObject).get("zoneType") : "";
        String currentZoneTypeValue = currentPropertyValueObject != null ? (String) ((Document)currentPropertyValueObject).get("zoneGroup") : "";
        String nextZoneTypeValue = nextPropertyValueObject != null ? (String) ((Document)nextPropertyValueObject).get("zoneType") : "";
        boolean propagateZoneType = prevZoneTypeValue.equals(currentZoneTypeValue) && currentZoneTypeValue.equals(nextZoneTypeValue);
        if(!propagateZoneType || zoneTypeCount > 1){
            fixDwellPreviousZoneProperty("zoneType", timestamp, propertyName, currentSnapshot, nextSnapshots,
                    thingSnapshotUpdate);
        }
        tu.stop("zoneType-Previous");

        //ZONE GROUP
        tu.start("zoneGroup-Previous");
        String prevZoneGroupValue = prevPropertyValueObject != null ? (String) ((Document)prevPropertyValueObject).get("zoneGroup") : "";
        String currentZoneGroupValue = currentPropertyValueObject != null ? (String) ((Document)currentPropertyValueObject).get("zoneType") : "";
        String nextZoneGroupValue = nextPropertyValueObject != null ? (String) ((Document)nextPropertyValueObject).get("zoneGroup") : "";
        boolean propagateZoneGroup = prevZoneGroupValue.equals(currentZoneGroupValue) && currentZoneGroupValue.equals(nextZoneGroupValue);
        if(!propagateZoneGroup || zoneGroupCount > 1){
            fixDwellPreviousZoneProperty("zoneGroup", timestamp, propertyName, currentSnapshot, nextSnapshots,
                    thingSnapshotUpdate);
        }
        tu.stop("zoneGroup-Previous");

    }

    private void fixDwellPreviousZoneProperty(String zonePropertyName,
                                              Long timestamp,
                                              String propertyName,
                                              Document currentSnapshot,
                                              List<Document> nextSnapshots,
                                              Map<ObjectId, Map<String, Document>> thingSnapshotUpdate) {

        Document previousChangedSnapshot = SnapshotFixEntryUtilsCommons.getPreviousSnapshot(thingId, timestamp,
                "value." + propertyName + ".value." + zonePropertyName + "Changed", thingSnapshotsCollection);

        if (previousChangedSnapshot == null) {
            return;
        }

        //extract next snapshot date
        ObjectId previousChangedSnapshotId = (ObjectId) previousChangedSnapshot.get("_id");

        Date previousChangedSnapshotTime = (Date) previousChangedSnapshot.get("time");

        Object currentPropertyValueObject = SnapshotFixEntryUtilsCommons.getSnapshotPropertyValue(propertyName,
                currentSnapshot);

        String snapshotPropertyValue = "";
        if (currentPropertyValueObject != null) {
            snapshotPropertyValue = SnapshotFixEntryUtilsCommons.getValueFromProperty(currentPropertyValueObject);
        }

        Boolean isCurrentZonePropertyChanged = ((Document) currentPropertyValueObject).containsKey
                (zonePropertyName + "Changed")
                && Boolean.parseBoolean(((Document) currentPropertyValueObject).get(zonePropertyName + "Changed")
                .toString());

        Date currentSnapshotDate = new Date(timestamp);
        if (!isCurrentZonePropertyChanged) {
            Date nextChangedSnapshotDate = SnapshotFixEntryUtilsCommons.getNextChangedZonePropertyDate
                    (propertyName, zonePropertyName, snapshotPropertyValue, nextSnapshots);
            if (nextChangedSnapshotDate == null) {
                nextChangedSnapshotDate = previousChangedSnapshotTime;
            }
            currentSnapshotDate = nextChangedSnapshotDate;
        }
        Long newPreviousChangedDwell = currentSnapshotDate.getTime() - previousChangedSnapshotTime.getTime();

        if (!thingSnapshotUpdate.containsKey(previousChangedSnapshotId)) {
            thingSnapshotUpdate.put(previousChangedSnapshotId, new HashMap<String, Document>());
        }
        if (!thingSnapshotUpdate.get(previousChangedSnapshotId).containsKey("$set")) {
            thingSnapshotUpdate.get(previousChangedSnapshotId).put("$set", new Document());
        }
        thingSnapshotUpdate.get(previousChangedSnapshotId).get("$set").append("value." + propertyName + ".value."
                + zonePropertyName + "DwellTime", newPreviousChangedDwell);

    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(SnapshotFixEntryCommons o) {
        return o.getTimestamp().compareTo(this.getTimestamp());
    }

    public ChangedFields getThingMessage() {
        return thingMessage;
    }

    public void setThingMessage(ChangedFields thingMessage) {
        this.thingMessage = thingMessage;
    }

    public Long getThingId() {
        return thingId;
    }

    public void setThingId(Long thingId) {
        this.thingId = thingId;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getHasPreviousSnapshot() {
        return hasPreviousSnapshot;
    }

    public void setHasPreviousSnapshot(Boolean hasPreviousSnapshot) {
        this.hasPreviousSnapshot = hasPreviousSnapshot;
    }

    public String getThingTypeCode() {
        return thingTypeCode;
    }

    public void setThingTypeCode(String thingTypeCode) {
        this.thingTypeCode = thingTypeCode;
    }

    public int getZoneTypeCount() {
        return zoneTypeCount;
    }

    public void setZoneTypeCount(int zoneTypeCount) {
        this.zoneTypeCount = zoneTypeCount;
    }

    public int getZoneGroupCount() {
        return zoneGroupCount;
    }

    public void setZoneGroupCount(int zoneGroupCount) {
        this.zoneGroupCount = zoneGroupCount;
    }

    public int getFacilityMapCount() {
        return facilityMapCount;
    }

    public void setFacilityMapCount(int facilityMapCount) {
        this.facilityMapCount = facilityMapCount;
    }

    public Long getProcessTimestamp() {
        return processTimestamp;
    }

    public void setProcessTimestamp(Long processTimestamp) {
        this.processTimestamp = processTimestamp;
    }
}
