package com.tierconnect.riot.iot.entities;

import java.util.*;

public class ThingObject {

    private Long id;
    private String serialNumber;
    private String name;
    private String thingTypeCode;
    private String thingTypeId;
    private String groupCode;
    private String groupId;
    private String groupTypeCode;
    private String groupTypeId;
    private Map<String,ThingFieldObject> fields;

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setThingTypeCode(String thingTypeCode) {
        this.thingTypeCode = thingTypeCode;
    }

    public void setThingTypeId(String thingTypeId) {
        this.thingTypeId = thingTypeId;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setGroupTypeCode(String groupTypeCode) {
        this.groupTypeCode = groupTypeCode;
    }

    public void setGroupTypeId(String groupTypeId) {
        this.groupTypeId = groupTypeId;
    }

    public String getSerialNumber() {

        return serialNumber;
    }

    public String getName() {
        return name;
    }

    public String getThingTypeCode() {
        return thingTypeCode;
    }

    public String getThingTypeId() {
        return thingTypeId;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getGroupTypeCode() {
        return groupTypeCode;
    }

    public String getGroupTypeId() {
        return groupTypeId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Map<String, ThingFieldObject> getFields() {
        return fields;
    }

    public void setFields(Map<String, ThingFieldObject> fields) {
        this.fields = fields;
    }

    public void addField(String fieldId, ThingFieldObject fieldObject) { fields.put(fieldId, fieldObject); }

    /**
     * Thing constructor
     *
     */
    public ThingObject(long id) { // constructor
        this.id = id;
        this.fields = new HashMap<>();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("id:" + id + ",");
        sb.append("serialNumber:" + serialNumber +",");
    //sb.append("name:" + name + ",");
        sb.append("thingTypeCode:" + thingTypeCode + ",");
    //sb.append("thingTypeId:" + thingTypeId + ",");
        sb.append("groupCode:" + groupCode + ",");
    //sb.append("groupId:" + groupId + ",");
        sb.append("groupTypeCode:" + groupTypeCode + ",");
    //sb.append("groupTypeId:" + groupTypeId );

        sb.append("\n[");

        if (fields != null) {
            Iterator it = fields.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                sb.append(pair.getKey() + ":{" + pair.getValue()+"}");
                if (it.hasNext()) {
                    sb.append(",");
                }
            }
        }
        sb.append("]");

        return sb.toString();
    }
}
