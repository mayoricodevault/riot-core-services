package com.tierconnect.riot.api.mongoTransform;

import org.apache.log4j.Logger;
import org.bson.BsonDocument;
import org.bson.BsonType;
import org.bson.BsonValue;

import java.util.*;

/**
 * Created by vealaro on 11/30/16.
 * Class to codec a database result.
 */
public class BsonToMap {

    private static Logger logger = Logger.getLogger(BsonToMap.class);

    private BsonToMap() {
    }

    public static Map<String, Object> getMap(BsonDocument bsonDocument) {
        Map<String, Object> resultMap = new LinkedHashMap<>();
        for (Map.Entry<String, BsonValue> document : bsonDocument.entrySet()) {
            if (BsonType.DOCUMENT.equals(document.getValue().getBsonType())) {
                resultMap.put(document.getKey(), getMap((BsonDocument) document.getValue()));
            } else if (BsonType.ARRAY.equals(document.getValue().getBsonType())) {
                resultMap.put(document.getKey(), getValuesOfArray(document.getValue()));
            } else {
                resultMap.put(document.getKey(), getValue(document.getValue()));
            }
        }
        return resultMap;
    }


    private static Object getValuesOfArray(BsonValue bsonValue) {
        List<Object> resultSetArray = new ArrayList<>();
        for (BsonValue bsonValueArray : bsonValue.asArray().getValues()) {
            if (BsonType.DOCUMENT.equals(bsonValueArray.getBsonType())) {
                resultSetArray.add(getMap(bsonValueArray.asDocument()));
            } else if (BsonType.ARRAY.equals(bsonValueArray.getBsonType())) {
                resultSetArray.add(getValuesOfArray(bsonValueArray));
            } else {
                resultSetArray.add(getValue(bsonValueArray));
            }
        }
        return resultSetArray;
    }

    private static Object getValue(BsonValue bsonValue) {
        Object value;
        switch (bsonValue.getBsonType()) {
            case NULL:
                value = null;
                break;
            case DOUBLE:
                value = bsonValue.asDouble().getValue();
                break;
            case BOOLEAN:
                value = bsonValue.asBoolean().getValue();
                break;
            case INT64:
                value = bsonValue.asInt64().getValue();
                break;
            case INT32:
                value = bsonValue.asInt32().getValue();
                break;
            case STRING:
                value = bsonValue.asString().getValue();
                break;
            case DATE_TIME:
                value = new Date(bsonValue.asDateTime().getValue());
                break;
            case OBJECT_ID:
                value = "ObjectId(\"" + bsonValue.asObjectId().getValue() + "\")";
                break;
            default:
                logger.error("Error with type [" + bsonValue.getBsonType() + "] and value = " + bsonValue.toString());
                value = "ERROR: " + bsonValue.toString();
                break;
        }
        return value;
    }
}
