package com.tierconnect.riot.iot.reports_integration;

import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Created by vealaro on 3/9/17.
 */
public class MapGeoJsonReportExecution extends ReportExecution implements IReportExecution {

    private static Logger logger = Logger.getLogger(MapGeoJsonReportExecution.class);

    public MapGeoJsonReportExecution(ReportConfig configuration, ITranslateResult translateResult, String comment) {
        super(configuration, translateResult, comment);
    }

    @Override
    protected void addPropertiesToReports(Map<String, Object> labelValues) {
        buildGeoJson(labelValues);
    }

    @Override
    protected void buildResult() {
        logger.debug("build result with GeoJson information");
        super.buildResult();
        buildResultWithGeoJson();
    }
}
