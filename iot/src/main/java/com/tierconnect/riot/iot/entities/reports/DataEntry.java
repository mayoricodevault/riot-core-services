package com.tierconnect.riot.iot.entities.reports;

import com.tierconnect.riot.sdk.utils.BeanUtils;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by achambi on 8/28/17.
 * POJO to validate thing data entry
 */
public class DataEntry {

    public static final Map<String, Class<?>> classOfProperty = new ConcurrentHashMap<>();

    static {
        classOfProperty.put("thingTypeId", Integer.class);
        classOfProperty.put("thingTypeFields", List.class);

    }


    private Long thingTypeId;
    private Set<String> thingTypeFields;
    private String message;

    DataEntry(Map<String, Object> dataEntry) {
        BeanUtils.setPOJOProperties(dataEntry, this);
        message = StringUtils.EMPTY;
    }

    public void setThingTypeId(Integer thingTypeId) {
        this.thingTypeId = new Long(thingTypeId);
    }

    public void setThingTypeFields(List<String> thingTypeFields) {
        this.thingTypeFields = new TreeSet<>(thingTypeFields);
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getThingTypeId() {
        return thingTypeId;
    }

    public Set<String> getThingTypeFields() {
        return thingTypeFields;
    }

    public String getMessage() {
        return message;
    }
}
