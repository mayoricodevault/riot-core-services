package com.tierconnect.riot.iot.entities;

import java.util.Date;
import java.util.Map;

/**
 * Created by user on 4/29/15.
 */
public class FieldTypeCassandra {
    Date time;
    Long thingId;
    Long thingTypeFieldId;
    Long writeTime;
    String value;

    public FieldTypeCassandra() {

    }
    public FieldTypeCassandra(Long thingTypeFieldId, Date time, Object value, Long thingId, Long writeTime) {
        this.thingTypeFieldId = thingTypeFieldId;
        this.time = time;
        this.value = value != null ? value.toString() : "";
        this.thingId = thingId;
        this.writeTime = writeTime;
    }


    public Long getThingTypeFieldId() {
        return thingTypeFieldId;
    }

    public void setThingTypeFieldId(Long thingTypeFieldId) {
        this.thingTypeFieldId = thingTypeFieldId;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Long getThingId() {
        return thingId;
    }

    public void setThingId(Long thingId) {
        this.thingId = thingId;
    }

    public Long getWriteTime() {
        return writeTime;
    }

    public void setWriteTime(Long writeTime) {
        this.writeTime = writeTime;
    }

    public void setFieldValueFromObject(Long thingFieldId, Map<String, Object> fieldValueArray) {
        this.setThingTypeFieldId(thingFieldId);
        this.setTime((Date) fieldValueArray.get("time"));
        this.setValue(fieldValueArray.get("value") != null ? fieldValueArray.get("value").toString() : "");
        this.setWriteTime(Long.valueOf(fieldValueArray.get("").toString()));
        this.setThingId(Long.valueOf(fieldValueArray.get("thing_id").toString()));
    }


}
