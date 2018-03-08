package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.api.database.base.FactoryDataBase;
import com.tierconnect.riot.api.database.mongo.Mongo;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Map;

/**
 * Created by vealaro on 2/2/17.
 * Modified by achambi on 4/24/17
 */
public class SummaryReportExecution extends ReportExecution {
    protected static final String RESULT = "RESULT";
    private TableSummaryReportConfig summaryReportConfig;
    private ITranslateResult summaryTranslateResult;

    private static Logger logger = Logger.getLogger(SummaryReportExecution.class);

    protected SummaryReportExecution(TableSummaryReportConfig configuration, ITranslateResult translateResult) {
        super(configuration, translateResult);
        this.summaryReportConfig = configuration;
        this.summaryTranslateResult = translateResult;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        executeAggregate();
        long end = System.currentTimeMillis();
        long duration = end - start;
        logger.info("Records Size " + count + " in " + duration + " ms");
        buildReportLog(start, end, duration);
        if (isEnableSaveReportLog()) {
            reportLog.put("query", dataBase.getAggregateString());
        }
    }

    private void executeAggregate() {
        try {
            dataBase = FactoryDataBase.get(Mongo.class, getConfiguration().getFilters());
            dataBase.executeAggregate(getConfiguration().getCollectionTarget(), summaryReportConfig.getPipelineList()
                    , getConfiguration().getLimit(), isEnableSaveReportLog());
            count = dataBase.getCountAll();
            this.summaryTranslateResult.exportResult(Collections.singletonMap(RESULT, dataBase.getResultSet()));
        } catch (UserException e) {
            logger.error("Error in execution report with aggregation", e);
            throw e;
        } catch (Exception e) {
            logger.error("Error in execution aggregation to Mongo", e);
            throw new UserException("Error in execution report", e);
        }
    }

    @Override
    public Map<String, Object> getMapResult() {
        return summaryTranslateResult.getLabelValues();
    }
}
