package com.tierconnect.riot.iot.entities.reports;

import com.tierconnect.riot.sdk.dao.UserException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by achambi on 8/28/17.
 * Entity to map a date field formula verification.
 * POJO to validate thing data entry
 */
public class DataEntryHeader {

    private List<DataEntry> dataEntries;

    public static final Map<String, Class<?>> classOfProperty = new ConcurrentHashMap<>();

    static {
        classOfProperty.put("dataEntries", List.class);
    }

    public DataEntryHeader(List<Map<String, Object>> dataEntryList) {
        try {
            dataEntries = new ArrayList<>();
            for (Map<String, Object> item : dataEntryList) {
                DataEntry dataEntry = new DataEntry(item);
                dataEntries.add(dataEntry);
            }
        } catch (UserException ex) {
            throw new UserException("The body of the request could not be parsed: " + ex.getMessage());
        }
    }

    public List<DataEntry> getDataEntries() {
        return dataEntries;
    }
}
