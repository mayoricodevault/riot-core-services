package com.tierconnect.riot.iot.reports_integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.tierconnect.riot.api.database.mongo.Mongo;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.geojson.LngLatAltTime;

import com.tierconnect.riot.iot.reports.autoindex.entities.indexInformation.IndexInformation;
import com.tierconnect.riot.iot.reports.autoindex.services.ReportLogMongoService;
import com.tierconnect.riot.iot.utils.ReportExecutionUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.LineString;

import java.util.*;

import static com.tierconnect.riot.appcore.utils.Utilities.isEmptyOrNull;
import static com.tierconnect.riot.appcore.utils.Utilities.isNumber;
import static com.tierconnect.riot.commons.Constants.*;

/**
 * Created by vealaro on 1/10/17.
 * Modified by achambi on 4/42017.
 * Class that is responsible for the execution of the report.
 */
public class ReportExecution implements IReportExecution {

    private static Logger logger = Logger.getLogger(ReportExecution.class);

    FeatureCollection featureCollection = new FeatureCollection();
    ITranslateResult translateResult;
    Map<String, Object> mapResult = new LinkedHashMap<>(5);

    private boolean includeHeader = true;

    private boolean enableSaveReportLog = false;

    protected List<Map<String, Object>> records;
    protected Long count = null;
    protected ReportConfig configuration;
    protected String comment;
    protected Mongo dataBase;
    protected Map<String, Object> reportLog;
    //private ReportLogDetail reportLogDetail;
    private final String reportIndex;

    ReportExecution(ReportConfig configuration, ITranslateResult translateResult) {
        this(configuration, translateResult, "");
    }

    ReportExecution(ReportConfig configuration, ITranslateResult translateResult, String comment) {
        this.configuration = configuration;
        this.translateResult = translateResult;
        this.comment = comment;
        this.reportIndex = ReportLogMongoService.getInstance().getIndex(configuration.getReportDefinition().getId(),
                configuration.getCollectionTarget(), configuration.getConfigDefinition());
        if(StringUtils.isNotEmpty(this.reportIndex)) {
            logger.info("Index for report '" + configuration.getReportDefinition().getName() +
                    "' is running with index: " + this.reportIndex);
        } else {
            logger.info("Index for report '" + configuration.getReportDefinition().getName() +
                    "' is running without index");
        }
    }

    ReportExecution(ReportConfig configuration, ITranslateResult translateResult, String comment, boolean
            includeHeader) {
        this(configuration, translateResult, comment);
        this.includeHeader = includeHeader;
    }

    @Override
    public void run() throws Exception {
        long start = System.currentTimeMillis();
        executeFind();
        buildResult();
        long end = System.currentTimeMillis();
        long duration = end - start;
        buildReportLog(start, end, duration);
        logger.info("Records Size " + count + " in " + duration + " ms");
    }

    @Override
    public Map<String, Object> getReportInfo() {
        return this.reportLog;
    }

    private void executeFind() throws Exception {
        try {

            dataBase = new Mongo(configuration.getFilters());
            dataBase.executeFind(configuration.getCollectionTarget(),
                    configuration.getProjection(),
                    configuration.getSkip(),
                    configuration.getLimit(),
                    configuration.getSort(),
                    null,
                    comment,
                    getReportIndex());
            count = dataBase.getCountAll();
            records = new ArrayList<>(dataBase.getResultSet().size());
            for (Map<String, Object> map : dataBase.getResultSet()) {
                translateResult.exportResult(map);
                Map<String, Object> labelValues = translateResult.getLabelValues();
                if (includeHeader) {
                    labelValues.put(POPUP_COMMON_FIELDS_NAME, buildPopupCommonFields(map));
                }
                addPropertiesToReports(labelValues);
            }
        } catch (Exception e) {
            logger.error("Failed to execute command in database.", e);
            throw new Exception("Failed to execute command in database.", e);
        }
    }

    /**
     * Method to prepare report Log Object for analyse.
     *
     * @param start    the start Time in Milliseconds
     * @param end      the end Time in Milliseconds
     * @param duration the difference between start and end time in milliseconds.
     */
    protected void buildReportLog(long start, long end, long duration) {

        if (enableSaveReportLog) {
            // Saving report values and time values for report log
            reportLog = new HashMap<>();
            reportLog.put("count", count);
            reportLog.put("filtersDefinition", configuration.getConfigDefinition());
            reportLog.put("query", dataBase.getFilterString());
            reportLog.put("start", start);
            reportLog.put("end", end);
            reportLog.put("duration", duration);
            reportLog.put("collectionName", configuration.getCollectionTarget());
            reportLog.put("sort", dataBase.getSortString());

            //TODO: 2017/04/21 New report Log in OEM mode to improve in the next sprint.
            //            reportLogDetail = new ReportLogDetail();
            //            reportLogDetail.setCount(count);
            //            reportLogDetail.setQuery(mongo.getSortString());
            //            reportLogDetail.setStart(start);
            //            reportLogDetail.setEnd(end);
            //            reportLogDetail.setDuration(duration);
        }
    }

    protected void addPropertiesToReports(Map<String, Object> labelValues) {
        records.add(labelValues);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> buildPopupCommonFields(final Map<String, Object> map) {
        Map<String, String> thingHeaders = new HashMap<>();
        if (configuration.isHistoricalReport()) {
            thingHeaders.put(NAME, ((Map<String, Object>) map.get("value")).get(NAME).toString());
            thingHeaders.put(SERIAL_NUMBER, ((Map<String, Object>) map.get("value")).get(SERIAL_NUMBER).toString());
            thingHeaders.put(THING_TYPE_TRANSLATE_NAME, ((Map<String, Object>) map.get("value")).get
                    (THING_TYPE_TRANSLATE_NAME).toString());
            thingHeaders.put(THING_TYPE_TRANSLATE_ID, ((Map<String, Object>) map.get("value")).get
                    (THING_TYPE_TRANSLATE_ID).toString());
        } else {
            thingHeaders.put(NAME, map.get(NAME).toString());
            thingHeaders.put(SERIAL_NUMBER, map.get(SERIAL_NUMBER).toString());
            thingHeaders.put(THING_TYPE_TRANSLATE_NAME, map.get(THING_TYPE_TRANSLATE_NAME).toString());
            thingHeaders.put(THING_TYPE_TRANSLATE_ID, map.get(THING_TYPE_TRANSLATE_ID).toString());
        }
        return thingHeaders;
    }

    protected void buildResult() {
        mapResult.putAll(configuration.exportResult(count, records));
    }

    private LineString getLineStringFromLocation(List<String> locationThing) {
        List<LngLatAltTime> points = new ArrayList<>(locationThing.size());
        for (String token : locationThing) {
            String[] lonLat = token.split(";");
            if (lonLat.length >= 3 && isNumber(lonLat[0]) && isNumber(lonLat[1]) && isNumber(lonLat[2])) {
                points.add(new LngLatAltTime(new Double(lonLat[0]), new Double(lonLat[1]), new Double(lonLat[2]),
                        (lonLat.length == 4 ? Long.parseLong(lonLat[3]) : 0L)));

            }
        }
        Collections.sort(points);
        LineString lineString = new LineString();
        for (LngLatAltTime point : points) {
            lineString.add(point);
        }
        return lineString;
    }

    @SuppressWarnings("unchecked")
    void buildGeoJson(Map<String, Object> labelValues) {
        featureCollection.add(buildFeature(labelValues));
    }

    @SuppressWarnings("unchecked")
    protected Feature buildFeature(Map<String, Object> labelValues) {
        Object locationThing = labelValues.get(Constants.LOCATION);
        LineString lineString = new LineString();
        if (locationThing instanceof String && !isEmptyOrNull((String) locationThing)) {
            lineString = getLineStringFromLocation(Arrays.asList(StringUtils.split((String) locationThing, ",")));
        } else if (locationThing instanceof List) {
            //noinspection unchecked
            lineString = getLineStringFromLocation((List<String>) locationThing);
        }
        labelValues.remove(Constants.LOCATION);
        // add feature
        Feature feature = new Feature();
        feature.setGeometry(lineString);
        feature.setProperties(labelValues);
        return feature;
    }

    void buildResultWithGeoJson() {
        Map<String, Object> mapJson = ReportExecutionUtils.addingSettingsMapProperties(getConfiguration()
                .reportDefinition);
        mapJson.put("total", mapResult.get("total"));
        mapJson.put("startDate", mapResult.get("startDate"));
        mapJson.put("endDate", mapResult.get("endDate"));
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapResult = mapper.readValue(new ObjectMapper().writeValueAsString(featureCollection),
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    });
            mapResult.put("defaultProperties", mapJson);
            mapResult.put("labelProperties", ReportExecutionUtils.getLabelProperties(getConfiguration()
                    .reportDefinition.getReportProperty()));
        } catch (Exception e) {
            logger.error("Error in converter 'Feature Collection' in Map", e);
        }
    }

    @Override
    public Map<String, Object> getMapResult() {
        return mapResult;
    }

    public ReportConfig getConfiguration() {
        return configuration;
    }

    @Override
    public boolean isEnableSaveReportLog() {
        return enableSaveReportLog;
    }

    @Override
    public void setEnableSaveReportLog(boolean enableSaveReportLog) {
        this.enableSaveReportLog = enableSaveReportLog;
    }

    @Override
    public String getReportIndex() {
        return reportIndex;
    }
}
