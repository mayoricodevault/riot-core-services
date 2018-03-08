package com.tierconnect.riot.commons.dao.mongo;


import com.mongodb.Block;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.mongodb.client.model.Sorts.descending;

/**
 * Created by cvertiz on 9/1/16.
 */
public class SnapshotFixEntryUtilsCommons {

    private final static Logger logger = Logger.getLogger(SnapshotFixEntryUtilsCommons.class);

    public static List<Document> getSnapshots(Long thingId,
                                              Long date,
                                              String comparator,
                                              Integer limit,
                                              Integer sort,
                                              String fieldExists,
                                              MongoCollection thingSnapshotsCollection) {
        Document query = new Document("value._id", thingId).append("time", new Document("$" + comparator, new Date(date)));

        if (fieldExists != null) {
            query.append(fieldExists, new Document("$eq", true));
        }

        Document sortDoc = new Document("time", sort);

        FindIterable docCursor = thingSnapshotsCollection.find(query);
        if (sort != 0) {
            docCursor = docCursor.sort(sortDoc);
        }
        if (limit != -1) {
            docCursor = docCursor.limit(limit);
        }

        final List<Document> snapshots = new ArrayList<>();
        Block addSnapshot = new Block<Document>() {
            @Override
            public void apply(final Document document) {
                snapshots.add(document);
            }
        };
        docCursor.forEach(addSnapshot);
        return snapshots;
    }

    public static Document getPreviousSnapshot(Long thingId,
                                               Long date,
                                               String fieldExists,
                                               MongoCollection thingSnapshotsCollection) {
        Document query = new Document("value._id", thingId).append("time", new Document("$lt", new Date(date)));

        if (fieldExists != null) {
            query.append(fieldExists, true);
        }

        FindIterable docCursor = thingSnapshotsCollection.find(query).sort(descending("time"));
        return (Document) docCursor.first();
    }

    public static Long getNextChangedDate(String propertyValue,
                                          String propertyName,
                                          List<Document> nextSnapshots,
                                          Long timestamp) {
        //get next change date
        String currentValue = propertyValue;
        for (Document nextSnapshot : nextSnapshots) {

            //extract next snapshot property value
            Object nextSnapPropValueObject = getSnapshotPropertyValue(propertyName, nextSnapshot);

            String nextSnapPropValue = null;
            if(nextSnapPropValueObject != null){
                nextSnapPropValue = getValueFromProperty(nextSnapPropValueObject);
            }

            Boolean nextPropertyChanged = false;
            Boolean nextPropertyBlinked = false;
            if(nextSnapPropValueObject != null){
                nextPropertyChanged = getChangedFromProperty(propertyName, nextSnapshot)
                        && !propertyValue.equals(nextSnapPropValue);
                nextPropertyBlinked = getBlinkedFromProperty(propertyName, nextSnapshot)
                        && !propertyValue.equals(nextSnapPropValue);
            }

            if (nextSnapPropValue != null && (!nextSnapPropValue.equals(currentValue) && nextPropertyChanged
                    || !nextSnapPropValue.equals(propertyValue) && nextPropertyBlinked)) {
                return ((Date) nextSnapshot.get("time")).getTime();
            }
            currentValue = nextSnapPropValue;
        }
        return timestamp;
    }

    public static Date getNextChangedZonePropertyDate(String propertyName,
                                                      String zonePropertyName,
                                                      String zonePropertyValue,
                                                      List<Document> nextSnapshots) {
        //get next change date
        String currentValue = zonePropertyValue;
        for (Document nextSnapshot : nextSnapshots) {

            //extract next snapshot property value
            Object snapshotPropertyValueObject = getSnapshotPropertyValue(propertyName, nextSnapshot);

            String snapshotPropertyValue = null;
            if(snapshotPropertyValueObject != null && ((Document)snapshotPropertyValueObject).containsKey(zonePropertyName)){
                snapshotPropertyValue = ((Document)snapshotPropertyValueObject).get(zonePropertyName).toString();
            }

            Object nextPropertyValueObject = getSnapshotPropertyValue(propertyName, nextSnapshot);

            Boolean nextPropertyChanged = false;
            if(nextPropertyValueObject != null){
                nextPropertyChanged = ((Document)nextPropertyValueObject).containsKey(zonePropertyName+"Changed")
                        && !zonePropertyValue.equals(snapshotPropertyValue)
                        &&  Boolean.parseBoolean(((Document)nextPropertyValueObject).get(zonePropertyName+"Changed").toString());
            }


            if ((snapshotPropertyValue != null && !snapshotPropertyValue.equals(currentValue) && nextPropertyChanged)) {
//                return (Date) ((Document)snapshotPropertyValueObject).get(zonePropertyName+"Time");
                return (Date) nextSnapshot.get("time");
            }
            currentValue = snapshotPropertyValue;
        }
        return null;

    }

    public static Object getSnapshotPropertyValue(String propertyName,
                                                  Document snapshot) {
        try{
            Document snapshotValue = (Document) snapshot.get("value");
            //extract next snapshot property value
            if(snapshotValue.containsKey(propertyName)){
                return ((Document) (snapshotValue.get(propertyName))).get("value");
            }
        }catch (Exception e){
            logger.error("Error occurred when trying to get value for property " + propertyName + " in snapshot " + (snapshot != null ? snapshot.toString() : "'snapshot is null'"));
        }

        return null;
    }

    public static Long getSnapshotPropertyTTFId(String propertyName,
                                                Document nextSnapshot, Map<String, Long> thingTypeFieldIds) {
        Document snapshotValue = (Document) nextSnapshot.get("value");
        //extract next snapshot property value
        if(snapshotValue.containsKey(propertyName)){
            return (Long)((Document) (snapshotValue.get(propertyName))).get("thingTypeFieldId");
        }else if (thingTypeFieldIds != null){
            return thingTypeFieldIds.get(propertyName);
        }else{
            return null;
        }
    }

    public static Date getSnapshotPropertyTime(String propertyName,
                                               Document snapshot) {
        Document snapshotValue = (Document) snapshot.get("value");
        //extract next snapshot property value
        return (Date) (((Document) (snapshotValue.get(propertyName))).get("time"));

    }

    public static Date getSnapshotTime(Document snapshot) {
        //extract next snapshot property value
        return (Date) snapshot.get("time");

    }

    public static String getValueFromProperty(Object snapshotPropertyValueObject) {
        String snapshotPropertyValue = null;
        if (snapshotPropertyValueObject instanceof Document) {
            if(((Document) snapshotPropertyValueObject).containsKey("serialNumber")){
                snapshotPropertyValue = ((Document) snapshotPropertyValueObject).get("serialNumber").toString();
            }else{
                snapshotPropertyValue = ((Document) snapshotPropertyValueObject).get("id").toString();
            }
        } else if (snapshotPropertyValueObject instanceof Date) {
            snapshotPropertyValue = ((Date) snapshotPropertyValueObject).getTime() + "";
        } else if(snapshotPropertyValueObject != null){
            snapshotPropertyValue = snapshotPropertyValueObject.toString();
        }
        return snapshotPropertyValue;
    }

    public static Boolean getChangedFromProperty(String propertyName, Document nextSnapshot) {
        Document value = (Document)nextSnapshot.get("value");
        return value.containsKey(propertyName)
                && ((Document)value.get(propertyName)).containsKey("changed")
                && Boolean.parseBoolean(((Document)value.get(propertyName)).get("changed").toString());
    }

    public static Boolean getBlinkedFromProperty(String propertyName, Document nextSnapshot) {
        Document value = (Document)nextSnapshot.get("value");
        return value.containsKey(propertyName)
                && ((Document)value.get(propertyName)).containsKey("blinked")
                && Boolean.parseBoolean(((Document)value.get(propertyName)).get("blinked").toString());
    }

    public static boolean isPropertyZone(String propertyName,
                                         Document snapshot){
        Object value = getSnapshotPropertyValue(propertyName, snapshot);
        if(value instanceof Document){
            return ((Document)value).containsKey("facilityMap");
        }
        return false;
    }


}
