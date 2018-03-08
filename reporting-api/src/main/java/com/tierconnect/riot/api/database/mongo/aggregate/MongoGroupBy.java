package com.tierconnect.riot.api.database.mongo.aggregate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.api.assertions.Assertions;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.mongodb.client.model.Accumulators.*;
import static com.mongodb.client.model.Aggregates.group;
import static com.tierconnect.riot.api.mongoShell.utils.CharacterUtils.COLON;
import static com.tierconnect.riot.api.mongoShell.utils.CharacterUtils.betweenBraces;

/**
 * Created by vealaro on 1/30/17.
 */
public class MongoGroupBy extends PipelineBase implements Pipeline {

    private static Logger logger = Logger.getLogger(MongoGroupBy.class);
    private Map<String, Object> groupID;
    private String groupIDString;
    private String labelGroupBy;
    private Accumulator accumulator;
    private Object valueGroupBy;

    public MongoGroupBy(String group) {
        this.groupIDString = group;
    }

    public MongoGroupBy(Map<String, Object> group) {
        this.groupID = group;
    }

    public MongoGroupBy(Map<String, Object> groupID, String labelGroupBy, Accumulator accumulator, String valueGroupBy) {
        Assertions.notNull("Label Group By", labelGroupBy);
        Assertions.notNull("Accumulator operator", accumulator);
        Assertions.notNull("Value Group By", valueGroupBy);
        this.groupID = groupID;
        this.labelGroupBy = labelGroupBy;
        this.accumulator = accumulator;
        this.valueGroupBy = valueGroupBy;
    }

    public MongoGroupBy(String groupID, String labelGroupBy, Accumulator accumulator, String valueGroupBy) {
        Assertions.notNull("Label Group By", labelGroupBy);
        Assertions.notNull("Accumulator operator", accumulator);
        Assertions.notNull("Value Group By", valueGroupBy);
        this.groupIDString = groupID;
        this.labelGroupBy = labelGroupBy;
        this.accumulator = accumulator;
        this.valueGroupBy = valueGroupBy;
    }

    public void setLabelGroupBy(String labelGroupBy) {
        Assertions.notNull("Label Group By", labelGroupBy);
        this.labelGroupBy = labelGroupBy;
    }

    public void setAccumulator(Accumulator accumulator, String valueGroupBy) {
        Assertions.notNull("Accumulator operator", accumulator);
        Assertions.notNull("Value Group By", valueGroupBy);
        this.accumulator = accumulator;
        this.valueGroupBy = valueGroupBy;
    }

    public void setGroupID(Map<String, Object> groupID) {
        this.groupID = groupID;
    }

    public Object getGroupID() {
        if (groupIDString != null) {
            return groupIDString;
        } else if (groupID != null) {
            return new Document(groupID);
        } else {
            return null;
        }
    }

    public Bson toBson() {
        Bson groupbson;
        if (Accumulator.AVG.equals(accumulator)) {
            groupbson = group(getGroupID(), avg(labelGroupBy, valueGroupBy));
        } else if (Accumulator.SUM.equals(accumulator)) {
            groupbson = group(getGroupID(), sum(labelGroupBy, valueGroupBy));
        } else if (Accumulator.MIN.equals(accumulator)) {
            groupbson = group(getGroupID(), min(labelGroupBy, valueGroupBy));
        } else if (Accumulator.MAX.equals(accumulator)) {
            groupbson = group(getGroupID(), max(labelGroupBy, valueGroupBy));
        } else if (Accumulator.COUNT.equals(accumulator)) {
            groupbson = group(getGroupID(), sum(labelGroupBy, 1));
        } else {
            groupbson = group(getGroupID());
        }
        return groupbson;
    }

    @Override
    public String toString() {
        StringBuilder group = new StringBuilder(GROUP).append(COLON);
        Map<String, Object> mapGroup = new LinkedHashMap<>(3);
        mapGroup.put("_id", getGroupID());
        if (Accumulator.COUNT.equals(accumulator)) {
            valueGroupBy = 1;
        }
        if (accumulator!=null){
            mapGroup.put(labelGroupBy, Collections.singletonMap(accumulator.getValue(), valueGroupBy));
        }
        group.append(mapToJson(mapGroup));
        return betweenBraces(group.toString());
    }

    private String mapToJson(@SuppressWarnings("rawtypes") Map map) {
        ObjectMapper mapper = new ObjectMapper();
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try {
            mapper.writeValue(b, map);
            return b.toString("UTF-8");
        } catch (IOException e) {
            logger.warn("error", e);
        }
        return null;
    }

    public enum Accumulator {

        SUM("$sum"),
        AVG("$avg"),
        MAX("$max"),
        MIN("$min"),
        COUNT("$sum");

        private String value;

        Accumulator(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
