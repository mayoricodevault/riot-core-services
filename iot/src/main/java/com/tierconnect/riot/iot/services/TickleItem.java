package com.tierconnect.riot.iot.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by cfernandez
 * on 11/5/15.
 */
public class TickleItem
{
    private String serialNumber;
    private String thingTypeCode;
    private Date transactionDate;
    private List<TickleFieldItem> fields;
    private List<Long> groupMqtt;

    public TickleItem() {
        fields = new ArrayList<>();
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getThingTypeCode() {
        return thingTypeCode;
    }

    public void setThingTypeCode(String thingTypeCode) {
        this.thingTypeCode = thingTypeCode;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public List<TickleFieldItem> getFields() {
        return fields;
    }

    public void setFields(List<TickleFieldItem> fields) {
        this.fields = fields;
    }

    public void addField(TickleFieldItem field){
        fields.add(field);
    }

    public List<Long> getGroupMqtt() {
        return groupMqtt;
    }

    public void setGroupMqtt(List<Long> groupMqtt) {
        this.groupMqtt = groupMqtt;
    }
}
