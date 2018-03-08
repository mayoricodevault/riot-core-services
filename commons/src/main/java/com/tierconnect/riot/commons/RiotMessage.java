package com.tierconnect.riot.commons;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RiotMessage class.
 *
 * @author jantezana
 * @version 2017/02/2017
 */
public class RiotMessage {

    // Meta data.
    private String bridgeCode;
    private long sqn;
    private String specName;
    private String ruleExecutionMode;
    private List<Double> origin;
    private String units;
    private Boolean runRules;

    private String serialNumber;
    private long time;
    private String thingTypeCode;

    Map<String, Property> properties;

    public String getBridgeCode() {
        return bridgeCode;
    }

    public void setBridgeCode(String bridgeCode) {
        this.bridgeCode = bridgeCode;
    }

    public long getSqn() {
        return sqn;
    }

    public void setSqn(long sqn) {
        this.sqn = sqn;
    }

    public String getSpecName() {
        return specName;
    }

    public void setSpecName(String specName) {
        this.specName = specName;
    }

    public String getRuleExecutionMode() {
        return ruleExecutionMode;
    }

    public void setRuleExecutionMode(String ruleExecutionMode) {
        this.ruleExecutionMode = ruleExecutionMode;
    }

    public List<Double> getOrigin() {
        return origin;
    }

    public void setOrigin(List<Double> origin) {
        this.origin = origin;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public Boolean isRunRules() {
        return runRules;
    }

    public void setRunRules(Boolean runRules) {
        this.runRules = runRules;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getThingTypeCode() {
        return thingTypeCode;
    }

    public void setThingTypeCode(String thingTypeCode) {
        this.thingTypeCode = thingTypeCode;
    }

    public Map<String, Property> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Property> properties) {
        this.properties = properties;
    }

    public void addProperty(long time,
                            String name,
                            Object value) {
        if (this.properties == null) {
            this.properties = new HashMap<>();
        }

        RiotMessage.Property property = new RiotMessage.Property();
        property.setTime(time);
        property.setName(name);
        property.setValue(value);

        this.properties.put(name, property);
    }

    public static class Property {
        private long time;
        private String name;
        private Object value;

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }
    }
}
