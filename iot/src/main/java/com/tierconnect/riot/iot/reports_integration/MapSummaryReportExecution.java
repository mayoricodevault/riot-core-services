package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.api.database.base.FactoryDataBase;
import com.tierconnect.riot.api.database.mongo.Mongo;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.fmc.utils.FMCUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.geojson.Feature;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by vealaro on 2/15/17.
 * Class that is responsible for the execution of the Map report.
 */
public class MapSummaryReportExecution extends ReportExecution {

    private static Logger logger = Logger.getLogger(MapSummaryReportExecution.class);
    static final String RESULT_COUNT = "RESULT_COUNT";
    static final String RESULT_HEAT = "RESULT_HEAT";
    private MapSummaryReportConfig mapSummaryReportConfig;
    private ITranslateResult summaryTranslateResult;

    MapSummaryReportExecution(MapSummaryReportConfig mapSummaryReportConfig, ITranslateResult summaryTranslateResult) {
        super(mapSummaryReportConfig, summaryTranslateResult);
        this.mapSummaryReportConfig = mapSummaryReportConfig;
        this.summaryTranslateResult = summaryTranslateResult;
    }

    @Override
    public void run() {
        executeAggregate();
    }

    private void executeAggregate() {
        try {
            long start = System.currentTimeMillis();
            Map<String, Object> mapResult = new HashMap<>(2);
            dataBase= FactoryDataBase.get(Mongo.class, getConfiguration().getFilters());

            // execute count
            dataBase.executeAggregate(getConfiguration().getCollectionTarget(), mapSummaryReportConfig
                    .getPipelineCount(), getConfiguration().getLimit(), isEnableSaveReportLog());
            count = dataBase.getCountAll();
            mapResult.put(RESULT_COUNT, dataBase.getResultSet());

            // execute heat
            if (mapSummaryReportConfig.getPipelineHead() != null) {
                dataBase.executeAggregate(getConfiguration().getCollectionTarget(), mapSummaryReportConfig
                        .getPipelineHead(), getConfiguration().getLimit(), isEnableSaveReportLog());
                mapResult.put(RESULT_HEAT, dataBase.getResultSet());
            }
            long end = System.currentTimeMillis();
            long duration = end - start;
            logger.info("TIME EXECUTION AGGREGATE [" + duration + "] ms");
            buildReportLog(start, end, duration);
            if (isEnableSaveReportLog()) {
                reportLog.put("query", dataBase.getAggregateString());
            }
            summaryTranslateResult.exportResult(mapResult);
        } catch (Exception e) {
            logger.error("Error in execution aggregation to Mongo", e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> getMapResult() {
        Map<String, BigDecimal> groupingCount = (Map<String, BigDecimal>) summaryTranslateResult.getLabelValues().get
                (MapSummaryReportExecution.RESULT_COUNT);
        Map<String, BigDecimal> groupingHeat = (Map<String, BigDecimal>) summaryTranslateResult.getLabelValues().get
                (MapSummaryReportExecution.RESULT_HEAT);

        List<Zone> zoneList = mapSummaryReportConfig.getZoneList();
        final Map<String, BigDecimal> groupingHeat2 = groupingHeat;
        List<Feature> results = zoneList.stream()/*parallelStream()*///to avoid lazy loading troubles with hibernate
                .map(zone -> getZoneCount(zone, groupingCount, groupingHeat2))
                .collect(Collectors.toList());

        featureCollection.addAll(results);
        mapResult.put("total", results.size());
        buildResultWithGeoJson();
        mapResult.remove("reportLog");
        return mapResult;

    }

    private Feature getZoneCount(Zone zone, Map<String, BigDecimal> resultSetCount, Map<String, BigDecimal>
            resultSetHeat) {
        Map<String, Object> row = new LinkedHashMap<>(6);
        String[] centroid = FMCUtils.getCalculatedLocationsCenters(zone);

        row.put("zoneId", zone.getId());
        row.put("zoneCode", zone.getCode());
        row.put("zoneName", zone.getName());
        row.put("location", StringUtils.join(centroid, ";"));

        BigDecimal count = BigDecimal.ZERO;
        String heat = null;

        if (resultSetCount.get(zone.getCode()) != null) {
            count = resultSetCount.get(zone.getCode());
        }
        if (resultSetHeat.get(zone.getCode()) != null) {
            heat = String.valueOf(resultSetHeat.get(zone.getCode()));
        }
        row.put("count", count);
        row.put("heat", heat);

        return buildFeature(row);
    }

}
