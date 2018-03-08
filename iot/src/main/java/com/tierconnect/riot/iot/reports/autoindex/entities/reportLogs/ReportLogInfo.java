package com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.sdk.utils.HashUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.io.IOException;
import java.util.*;

/**
 * Created by achambi on 3/31/17.
 * Class entities for save mongo report log.
 */
@SuppressWarnings({"WeakerAccess", "unchecked"})
public class ReportLogInfo extends BasicDBObject {

    private static final String ID = "_id";
    private static final String TYPE = "type";
    private static final String NAME = "name";
    private static final String COLLECTION_NAME = "collectionName";
    private static final String LAST_RUN_DATE = "lastRunDate";
    private static final String MAX_DURATION = "maxDuration";
    private static final String MAX_DURATION_DATE = "maxDurationDate";
    private static final String TOTAL_RUNS = "totalRuns";
    private static final String QUERY = "query";
    private static final String STATUS = "status";
    private static final String LABEL = "label";
    private static final String MAX_DURATION_ID = "maxDurationId";
    private static final String RUNS = "runs";
    private static final String INDEX_INFORMATION = "indexInformation";
    private static final String CHECKED = "checked";
    private static final String FILTERS_DEFINITION = "filtersDefinition";


    private static Logger logger = Logger.getLogger(ReportLogInfo.class);

    private DateFormatAndTimeZone dateFormatAndTimeZone;

    private String lastRunDateIsoFormat;
    private String maxDurationDateIsoFormat;

    /**
     * Default constructor.
     */
    public ReportLogInfo() {

    }

    /**
     * Second constructor with date format.
     *
     * @param reportLogInfo         A {@link BasicDBObject} in the reportLogInfo format.
     * @param dateFormatAndTimeZone A instance of {@link DateFormatAndTimeZone} for Date format.
     */
    public ReportLogInfo(DBObject reportLogInfo, DateFormatAndTimeZone dateFormatAndTimeZone) throws IOException {
        if (reportLogInfo == null) {
            logger.error("The result is null.");
            throw new IOException("The result is null.");
        } else if (reportLogInfo instanceof BasicDBObject) {
            BasicDBObject basicDBObjReportlogInfo = (BasicDBObject) reportLogInfo;
            this.dateFormatAndTimeZone = dateFormatAndTimeZone;
            setId(basicDBObjReportlogInfo.getString(ID));
            setType(basicDBObjReportlogInfo.getString(TYPE));
            setName(basicDBObjReportlogInfo.getString(NAME));
            setCollectionName(basicDBObjReportlogInfo.getString(COLLECTION_NAME));
            setLastRunDate(basicDBObjReportlogInfo.getDate(LAST_RUN_DATE));
            setMaxDuration(basicDBObjReportlogInfo.getLong(MAX_DURATION));
            setMaxDurationDate(basicDBObjReportlogInfo.getDate(MAX_DURATION_DATE));
            setTotalRuns(basicDBObjReportlogInfo.getLong(TOTAL_RUNS));
            setQuery(basicDBObjReportlogInfo.getString(QUERY));
            setStatus(ReportLogStatus.getEnum(basicDBObjReportlogInfo.getString(STATUS)));
            setMaxDurationId(basicDBObjReportlogInfo.getObjectId(MAX_DURATION_ID));
            setChecked(basicDBObjReportlogInfo.getBoolean(CHECKED));
            setFiltersDefinition(basicDBObjReportlogInfo.getString(FILTERS_DEFINITION));
            if (reportLogInfo.get(RUNS) instanceof BasicDBList) {
                List<ReportLogDetail> runsList = new LinkedList<>();
                BasicDBList runs = (BasicDBList) reportLogInfo.get(RUNS);
                for (Object run : runs) {
                    if (run instanceof BasicDBObject) {
                        runsList.add(new ReportLogDetail((BasicDBObject) run, this.dateFormatAndTimeZone));
                    }
                }
                setRuns(runsList);
            }
            Object indexInformation = reportLogInfo.get(INDEX_INFORMATION);
            if (indexInformation instanceof BasicDBObject) {
                setIndexInformation(new IndexDescription((BasicDBObject) indexInformation, this.dateFormatAndTimeZone));
            }
        } else {
            logger.error("The reportLogInfo parameter cannot be convert in ReportLogInfo Class.");
            throw new IOException("The reportLogInfo parameter cannot be convert in ReportLogInfo Class.");
        }
    }

    /**
     * Third constructor.
     *
     * @param reportLogInfo A {@link BasicDBObject} in the reportLogInfo format.
     */
    public ReportLogInfo(BasicDBObject reportLogInfo) throws IOException {
        this(reportLogInfo, null);
    }


    public void merge(ReportLogInfo reportLogInfo) throws IOException {
        this.setType(reportLogInfo.getType());
        this.setName(reportLogInfo.getName());
        this.setLastRunDate(reportLogInfo.getLastRunDate());
        if (this.getMaxDuration() < reportLogInfo.getMaxDuration()) {
            this.setMaxDuration(reportLogInfo.getMaxDuration());
            this.setMaxDurationDate(reportLogInfo.getMaxDurationDate());
            this.setMaxDurationId(reportLogInfo.getMaxDurationId());
        }
        this.setTotalRuns(this.getTotalRuns() + 1);
        this.setQuery(this.getQuery());
        boolean resultAddRun = this.getRuns().addAll(0, reportLogInfo.getRuns());
        if (!resultAddRun) {
            logger.error("I can not add an item to the runs list.");
            throw new IOException("I can not add an item to the runs list.");
        }
    }

    public String getId() {
        return getString(ID);
    }

    public String getName() {
        return getString(NAME);
    }

    public String getCollectionName() {
        return getString(COLLECTION_NAME);
    }

    public String getType() {
        return getString(TYPE);
    }

    public Date getLastRunDate() {
        return getDate(LAST_RUN_DATE);
    }

    public String getLastRunDateIsoFormat() {
        return lastRunDateIsoFormat;
    }

    public long getMaxDuration() {
        return getLong(MAX_DURATION);
    }

    public Date getMaxDurationDate() {
        return getDate(MAX_DURATION_DATE);
    }

    public long getTotalRuns() {
        return getLong(TOTAL_RUNS);
    }

    public String getQuery() {
        return getString(QUERY);
    }

    public ReportLogStatus getStatus() {
        return ReportLogStatus.getEnum(getString(STATUS));
    }

    public ObjectId getMaxDurationId() {
        return getObjectId(MAX_DURATION_ID);
    }

    public LinkedList<ReportLogDetail> getRuns() {
        Object runs = this.get(RUNS);
        if (runs instanceof LinkedList) {
            return (LinkedList<ReportLogDetail>) runs;
        } else {
            return null;
        }
    }

    public IndexDescription getIndexInformation() {
        return (IndexDescription) get(INDEX_INFORMATION);
    }

    public void setId(String id) {
        put(ID, id);
    }

    public void setId(Long id) {
        String query = getQuery();
        query = query == null ? "Unknown" : query;
        put(ID, StringUtils.leftPad(id.toString(), 5, "0") + "-" + HashUtils.hashSHA256(query));
    }

    public void setName(String name) {
        put(NAME, name);
    }

    public void setCollectionName(String name) {
        put(COLLECTION_NAME, name);
    }

    public void setType(String type) {
        put(TYPE, type);
    }

    public void setLastRunDate(Date lastRunDate) {
        if (dateFormatAndTimeZone != null) {
            this.lastRunDateIsoFormat = dateFormatAndTimeZone.getISODateTimeFormatWithoutTimeZone(lastRunDate);
        }
        put(LAST_RUN_DATE, lastRunDate);
    }

    public void setMaxDuration(long maxDuration) {
        put(MAX_DURATION, maxDuration);
    }

    public void setMaxDurationDate(Date maxDurationDate) {
        if (dateFormatAndTimeZone != null) {
            this.maxDurationDateIsoFormat = dateFormatAndTimeZone.getISODateTimeFormatWithoutTimeZone(maxDurationDate);
        }
        put(MAX_DURATION_DATE, maxDurationDate);
    }

    public String getMaxDurationDateIsoFormat() {
        return maxDurationDateIsoFormat;
    }

    public void setTotalRuns(long totalRuns) {
        put(TOTAL_RUNS, totalRuns);
    }

    public void setQuery(String query) {
        put(QUERY, query);
    }

    public void setStatus(ReportLogStatus status) {
        put(STATUS, status.getValue());
    }

    public void setMaxDurationId(ObjectId maxDurationId) {
        put(MAX_DURATION_ID, maxDurationId);
    }

    public void setRuns(List<ReportLogDetail> runs) {
        put(RUNS, runs);
    }

    public void setIndexInformation(IndexDescription indexDescription) {
        this.put(INDEX_INFORMATION, indexDescription);
    }

    public void setChecked(Boolean value) {
        this.put(CHECKED, value);
    }

    public Boolean getChecked() {
        return super.getBoolean(CHECKED);
    }

    public void setFiltersDefinition(String filtersDefinition) {
        this.put(FILTERS_DEFINITION, filtersDefinition);
    }

    public String getFiltersDefinition() {
        return super.getString(FILTERS_DEFINITION).replaceAll("'", "\"");
    }

    public Map<String, Object> getMap() {
        Map<String, Object> reportLogInfoMap = new HashMap<>();
        reportLogInfoMap.put(ID, this.getId());
        reportLogInfoMap.put(TYPE, this.getType());
        reportLogInfoMap.put(NAME, this.getName());
        reportLogInfoMap.put(STATUS, this.getStatus());
        reportLogInfoMap.put(LABEL, this.getStatus() != null ? this.getStatus().getLabel() : StringUtils.EMPTY);
        reportLogInfoMap.put(CHECKED, this.getChecked());
        reportLogInfoMap.put(MAX_DURATION, this.getMaxDuration());
        try {
            reportLogInfoMap.put(FILTERS_DEFINITION, new JSONParser().parse(this.getFiltersDefinition()));
        } catch (ParseException e) {
            logger.error("Invalid json format for filter definition string", e);
            reportLogInfoMap.put(FILTERS_DEFINITION, this.getFiltersDefinition());
        }
        IndexDescription indexDescription = this.getIndexInformation();
        if (indexDescription != null) {
            reportLogInfoMap.put(INDEX_INFORMATION, indexDescription.getMap());
        }
        return reportLogInfoMap;
    }
}
