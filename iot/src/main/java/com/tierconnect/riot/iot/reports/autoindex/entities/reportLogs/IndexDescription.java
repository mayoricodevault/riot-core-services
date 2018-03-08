package com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs;

import com.mongodb.BasicDBObject;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by achambi on 4/26/17.
 * class that contains the index information.
 */
public class IndexDescription extends BasicDBObject {

    private static final String INDEX_NAME = "indexName";
    private static final String INDEX_DEFINITION = "definition";
    private static final String START_DATE = "starDate";
    private static final String END_DATE = "endDate";
    private static final String ASSOCIATED_INDEX = "associatedIndex";

    private DateFormatAndTimeZone dateFormatAndTimeZone;

    private String startDateIsoFormat;
    private String endDateIsoFormat;

    IndexDescription(BasicDBObject indexInformation, DateFormatAndTimeZone dateFormatAndTimeZone) {
        this.dateFormatAndTimeZone = dateFormatAndTimeZone;
        setIndexName(indexInformation.getString(INDEX_NAME));
        setIndexDefinition(indexInformation.getString(INDEX_DEFINITION));
        setStartDate(indexInformation.getDate(START_DATE));
        setEndDate(indexInformation.getDate(END_DATE));
        setAssociatedIndex(indexInformation.getString(ASSOCIATED_INDEX));
    }

    public String getIndexName() {
        return getString(INDEX_NAME);
    }

    public String getAssociatedIndex() {
        return getString(ASSOCIATED_INDEX);
    }

    public String getIndexDefinition() {
        return getString(INDEX_DEFINITION);
    }

    public Date getStartDate() {
        return getDate(START_DATE);
    }

    public Date getEndDate() {
        return getDate(END_DATE);
    }

    public String getStartDateIsoFormat() {
        return startDateIsoFormat;
    }

    public String getEndDateIsoFormat() {
        return endDateIsoFormat;
    }

    private void setIndexName(String indexName) {
        put(INDEX_NAME, indexName);
    }

    private void setIndexDefinition(String indexDefinition) {
        put(INDEX_DEFINITION, indexDefinition);
    }

    /**
     * set Start Date.
     *
     * @param startDate a {@link Date} instance.
     */
    public void setStartDate(Date startDate) {
        if (startDate != null) {
            if (dateFormatAndTimeZone != null) {
                this.startDateIsoFormat = dateFormatAndTimeZone.getISODateTimeFormatWithoutTimeZone(startDate);
            }
            put(START_DATE, startDate);
        }
    }

    /**
     * set End Date.
     *
     * @param endDate a {@link Date} instance,
     */
    public void setEndDate(Date endDate) {
        if (endDate != null) {
            if (dateFormatAndTimeZone != null) {
                this.endDateIsoFormat = dateFormatAndTimeZone.getISODateTimeFormatWithoutTimeZone(endDate);
            }
            put(END_DATE, endDate);
        }
    }

    private void setAssociatedIndex(String associatedIndex) {
        if(StringUtils.isNotEmpty(associatedIndex)){
            put(ASSOCIATED_INDEX, associatedIndex);
        }
    }

    public Map<String, String> getMap() {
        Map<String, String> indexInformationMap = new HashMap<>();
        indexInformationMap.put(INDEX_NAME, getIndexName());
        indexInformationMap.put(INDEX_DEFINITION, getIndexDefinition());
        indexInformationMap.put(START_DATE, getStartDateIsoFormat());
        indexInformationMap.put(END_DATE, getEndDateIsoFormat());
        return indexInformationMap;
    }
}
