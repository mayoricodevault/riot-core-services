package com.tierconnect.riot.iot.entities;

import java.util.Date;
import java.util.HashMap;

public class ThingFieldObject {
    private String name; //name of field
    private Long id;
    private String value;
    private Date time;
    private String thingTypeFieldId;
    private Long dwell;
    private HashMap<String, Object> thingfields;

    /**
     * Thing constructor
     */
    public ThingFieldObject(String name) { // constructor
        this.name = name;
        this.thingfields = new HashMap<>(10);
        this.time = null;
        this.dwell = null;
        this.value = null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public String getThingTypeFieldId() {
        return thingTypeFieldId;
    }

    public void setThingTypeFieldId(String thingTypeFieldId) {
        this.thingTypeFieldId = thingTypeFieldId;
    }

    public Long getDwell() {
        return dwell;
    }

    public void setDwell(Long dwell) {
        this.dwell = dwell;
    }

    public HashMap<String, Object> getThingfields() {
        return thingfields;
    }

    public void setThingfields(HashMap<String, Object> thingfields) {
        this.thingfields = thingfields;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("value:" + this.value + ",");
        sb.append("at:" + this.time + ",");
        if (this.thingTypeFieldId.compareTo("") == 0) {
            sb.append("thingTypeFieldId:" + this.thingTypeFieldId + ",");
        }
        sb.append("]");

        return sb.toString();
    }
}