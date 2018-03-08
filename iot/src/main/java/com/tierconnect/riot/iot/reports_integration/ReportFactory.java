package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.tierconnect.riot.commons.Constants.*;
import static org.apache.commons.lang.StringUtils.EMPTY;

/**
 * Created by vealaro on 1/10/17.
 * Class factory for run any report.
 */
@SuppressWarnings("SameParameterValue")
public class ReportFactory {

    private static Logger logger = Logger.getLogger(ReportFactory.class);

    private Map<String, Object> reportInfo;
    private String indexName;
    private boolean isSentEmail = false; //this just be changed to true when an email should be sent
    private DateFormatAndTimeZone dateFormatAndTimeZone;

    public ReportFactory(DateFormatAndTimeZone dateFormatAndTimeZone) {
        this.dateFormatAndTimeZone = dateFormatAndTimeZone;
    }

    public ReportFactory() {
    }

    public Map<String, Object> getReportInfo() {
        return reportInfo;
    }

    public String getIndexName() {
        return indexName;
    }

    public Map<String, Object> getResult(ReportDefinition reportDefinition, Map<String, Object> dynamicFilters,
                                         Integer pageNum,
                                         Integer pageSize, Date startDate, Date endDate, Date now, boolean
                                                 enableExplain) throws Exception {
        return getResult(reportDefinition.getReportType(), reportDefinition, dynamicFilters, pageNum, pageSize,
                startDate, endDate, now, true, EMPTY, EMPTY, EMPTY, enableExplain, false);
    }

    /**
     * Get Report result
     *
     * @param reportDefinition      The report Definition object.
     * @param dynamicFilters        A {@link Map}<{@link String},{@link Object}> that contains additional filters.
     * @param pageNum               A number with contains the report page number.
     * @param pageSize              A number with contains the report page size.
     * @param startDate             Report start date.
     * @param endDate               Report start date.
     * @param now                   Report current date.
     * @param addNonUdfInProperties Flag for add non UDF in properties.
     * @param comment               the query comment.
     * @param serverName            A {@link String} containing the server name.
     * @param contextPath           A {@link String} containing the context path.
     * @param enableSaveReportLog   A {@link Boolean} to enable or disable save the report log information.
     * @return A {@link Map}<{@link String},{@link Object}> with contains all report result.
     */
    public Map<String, Object> getResult(ReportDefinition reportDefinition,
                                         Map<String, Object> dynamicFilters,
                                         Integer pageNum,
                                         Integer pageSize,
                                         Date startDate,
                                         Date endDate,
                                         Date now,
                                         boolean addNonUdfInProperties,
                                         String comment,
                                         String serverName,
                                         String contextPath,
                                         boolean enableSaveReportLog) throws Exception {
        return getResult(reportDefinition.getReportType(), reportDefinition, dynamicFilters, pageNum, pageSize,
                startDate, endDate, now, addNonUdfInProperties, comment, serverName, contextPath,
                enableSaveReportLog, false);
    }

    /**
     * Get Report result
     *
     * @param reportDefinition      The report Definition object.
     * @param dynamicFilters        A {@link Map}<{@link String},{@link Object}> that contains additional filters.
     * @param pageNum               A number with contains the report page number.
     * @param pageSize              A number with contains the report page size.
     * @param startDate             Report start date.
     * @param endDate               Report start date.
     * @param now                   Report current date.
     * @param addNonUdfInProperties Flag for add non UDF in properties.
     * @param comment               the query comment.
     * @param serverName            A {@link String} containing the server name.
     * @param contextPath           A {@link String} containing the context path.
     * @param enableSaveReportLog   A {@link Boolean} to enable or disable the saving of the report log information.
     * @param thingTypeUdfAsObject  A {@link Boolean} to return a thinTypeUdf object.
     * @return A {@link Map}<{@link String},{@link Object}> with contains all report result.
     */
    public Map<String, Object> getResult(String type, ReportDefinition reportDefinition,
                                         Map<String, Object> dynamicFilters,
                                         Integer pageNum,
                                         Integer pageSize,
                                         Date startDate,
                                         Date endDate,
                                         Date now,
                                         boolean addNonUdfInProperties,
                                         String comment,
                                         String serverName, String contextPath, boolean enableSaveReportLog,
                                         boolean thingTypeUdfAsObject) throws Exception {
        Map<String, Object> result = new HashMap<>(5);
        IReportExecution reportExecution;
        long start = System.currentTimeMillis();
        switch (type) {
            case REPORT_TYPE_TABLE_SUMMARY:
                TableSummaryReportConfig tableSummaryReportConfig = new TableSummaryReportConfig(reportDefinition,
                        dynamicFilters, pageNum, pageSize, startDate, endDate, now, dateFormatAndTimeZone);
                logger.info("END CONFIGURATION " + REPORT_TYPE_TABLE_SUMMARY + " WITH TIME [" + (System
                        .currentTimeMillis() - start) + "] ms");
                reportExecution = new SummaryReportExecution(tableSummaryReportConfig, new
                        TableSummaryTranslateResult(tableSummaryReportConfig));
                reportExecution.setEnableSaveReportLog(enableSaveReportLog);
                reportExecution.run();
                result.putAll(reportExecution.getMapResult());
                if (StringUtils.isNotBlank(reportExecution.getReportIndex())) {
                    result.put("indexName", reportExecution.getReportIndex());
                }
                reportInfo = reportExecution.getReportInfo();
                indexName = reportExecution.getReportIndex();
                break;
            case REPORT_TYPE_MAP_SUMMARY:
                MapSummaryReportConfig mapSummaryReportConfig = new MapSummaryReportConfig(reportDefinition,
                        dynamicFilters, pageNum, pageSize, startDate, endDate, now, dateFormatAndTimeZone);
                logger.info("END CONFIGURATION " + REPORT_TYPE_MAP_SUMMARY + " WITH TIME [" + (System
                        .currentTimeMillis() - start) + "] ms");
                reportExecution = new MapSummaryReportExecution(mapSummaryReportConfig, new MapSummaryTranslateResult
                        (mapSummaryReportConfig));
                reportExecution.setEnableSaveReportLog(enableSaveReportLog);
                reportExecution.run();
                result.putAll(reportExecution.getMapResult());
                if (StringUtils.isNotBlank(reportExecution.getReportIndex())) {
                    result.put("indexName", reportExecution.getReportIndex());
                }
                reportInfo = reportExecution.getReportInfo();
                indexName = reportExecution.getReportIndex();
                break;
            case REPORT_TYPE_MAP_SUMMARY_BY_ZONE:
                TableDetailByZoneReportConfig tableDetailByZoneReportConfig = new TableDetailByZoneReportConfig
                        (reportDefinition, dynamicFilters, pageNum,
                                pageSize, startDate, endDate, now, dateFormatAndTimeZone);
                logger.info("END CONFIGURATION " + REPORT_TYPE_MAP_SUMMARY_BY_ZONE + "WITH TIME [" + (System
                        .currentTimeMillis() - start) + "] ms");
                reportExecution = new ReportExecution(tableDetailByZoneReportConfig, new TableDetailTranslateResult
                        (tableDetailByZoneReportConfig), comment, false);
                reportExecution.setEnableSaveReportLog(enableSaveReportLog);
                reportExecution.run();
                result.putAll(reportExecution.getMapResult());
                if (StringUtils.isNotBlank(reportExecution.getReportIndex())) {
                    result.put("indexName", reportExecution.getReportIndex());
                }
                reportInfo = reportExecution.getReportInfo();
                indexName = reportExecution.getReportIndex();
                break;
            case REPORT_TYPE_MAP:
                MapReportConfig mapReportConfig = new MapReportConfig(reportDefinition, dynamicFilters, pageNum,
                        pageSize, startDate, endDate, now, REPORT_TYPE_MAP, dateFormatAndTimeZone);
                logger.info("END CONFIGURATION " + REPORT_TYPE_MAP + " WITH TIME [" + (System.currentTimeMillis() -
                        start) + "] ms");
                reportExecution = new MapGeoJsonReportExecution(mapReportConfig, new MapTranslateResult
                        (mapReportConfig), comment); // TODO
                reportExecution.setEnableSaveReportLog(enableSaveReportLog);
                reportExecution.run();
                result.putAll(reportExecution.getMapResult());
                if (StringUtils.isNotBlank(reportExecution.getReportIndex())) {
                    result.put("indexName", reportExecution.getReportIndex());
                }
                reportInfo = reportExecution.getReportInfo();
                indexName = reportExecution.getReportIndex();
                break;
            case REPORT_TYPE_MAP_HISTORY:
                MapHistoryReportConfig mapHistoryReportConfig = new MapHistoryReportConfig(reportDefinition,
                        dynamicFilters, pageNum, pageSize, startDate, endDate, now, dateFormatAndTimeZone);
                logger.info("END CONFIGURATION " + REPORT_TYPE_MAP_HISTORY + " WITH TIME [" + (System
                        .currentTimeMillis() - start) + "] ms");
                reportExecution = new MapHistoryReportExecution(mapHistoryReportConfig, new MapTranslateResult
                        (mapHistoryReportConfig), comment);
                reportExecution.setEnableSaveReportLog(enableSaveReportLog);
                reportExecution.run();
                result.putAll(reportExecution.getMapResult());
                if (StringUtils.isNotBlank(reportExecution.getReportIndex())) {
                    result.put("indexName", reportExecution.getReportIndex());
                }
                reportInfo = reportExecution.getReportInfo();
                indexName = reportExecution.getReportIndex();
                break;
            case REPORT_TYPE_TABLE_HISTORY:
                ReportConfig historyReportConfig;
                if (reportDefinition.getName().toLowerCase().contains("_exit")) {
                    historyReportConfig = new TableHistoryExitReportConfig(reportDefinition,
                            dynamicFilters, pageNum, pageSize, startDate, endDate, now, addNonUdfInProperties,
                            dateFormatAndTimeZone);
                } else {
                    historyReportConfig = new TableHistoryReportConfig(reportDefinition,
                            dynamicFilters, pageNum, pageSize, startDate, endDate, now, addNonUdfInProperties,
                            dateFormatAndTimeZone);
                }
                logger.info("END CONFIGURATION " + REPORT_TYPE_TABLE_HISTORY + " WITH TIME [" + (System
                        .currentTimeMillis() - start) + "] ms");
                reportExecution = new ReportExecution(historyReportConfig,
                        new TableHistoryTranslateResult(historyReportConfig, serverName, contextPath), comment);
                reportExecution.setEnableSaveReportLog(enableSaveReportLog);
                reportExecution.run();
                result.putAll(reportExecution.getMapResult());
                if (StringUtils.isNotBlank(reportExecution.getReportIndex())) {
                    result.put("indexName", reportExecution.getReportIndex());
                }
                reportInfo = reportExecution.getReportInfo();
                indexName = reportExecution.getReportIndex();
                break;
            case REPORT_TYPE_TABLE_SCRIPT:
                User user = (User) SecurityUtils.getSubject().getPrincipal();
                TableScriptConfig tableScriptConfig = new TableScriptConfig(reportDefinition,
                        dynamicFilters, pageNum, pageSize, startDate, endDate, now, addNonUdfInProperties, user);
                TableScriptExecution ts = new TableScriptExecution(reportDefinition,
                        dynamicFilters, pageSize, pageNum, startDate, endDate, now, comment, tableScriptConfig,
                        isSentEmail);
                logger.info("END CONFIGURATION " + REPORT_TYPE_TABLE_SCRIPT + " WITH TIME [" + (System
                        .currentTimeMillis() - start) + "] ms");
                result.putAll(ts.run());
                break;
            case REPORT_TYPE_TABLE_CONNECTION:
                TableScriptConfig tableConnectionConfig = new TableScriptConfig(reportDefinition,
                        dynamicFilters, pageNum, pageSize, startDate, endDate, now, addNonUdfInProperties, (User)
                        SecurityUtils.getSubject().getPrincipal());
                logger.info("END CONFIGURATION " + REPORT_TYPE_TABLE_CONNECTION + " WITH TIME [" + (System
                        .currentTimeMillis() - start) + "] ms");
                TableConnectionExecution tcs = new TableConnectionExecution(reportDefinition, dynamicFilters,
                        pageSize, pageNum, startDate, endDate, now, comment, tableConnectionConfig, isSentEmail, false);
                result.putAll(tcs.run());
                break;
            default:
            case REPORT_TYPE_TABLE_DETAIL:
                TableDetailReportConfig tableDetailReportConfig = new TableDetailReportConfig(reportDefinition,
                        dynamicFilters, pageNum, pageSize, startDate, endDate, now, addNonUdfInProperties,
                        dateFormatAndTimeZone,
                        thingTypeUdfAsObject);
                logger.info("END CONFIGURATION " + REPORT_TYPE_TABLE_DETAIL + " WITH TIME [" + (System
                        .currentTimeMillis() - start) + "] ms");
                reportExecution = new ReportExecution(tableDetailReportConfig, new TableDetailTranslateResult
                        (tableDetailReportConfig, serverName, contextPath), comment);
                reportExecution.setEnableSaveReportLog(enableSaveReportLog);
                reportExecution.run();
                result.putAll(reportExecution.getMapResult());
                if (StringUtils.isNotBlank(reportExecution.getReportIndex())) {
                    result.put("indexName", reportExecution.getReportIndex());
                }
                reportInfo = reportExecution.getReportInfo();
                indexName = reportExecution.getReportIndex();
                break;
        }
        return result;
    }

    /**
     * Get Report export
     *
     * @param reportDefinition      The report Definition
     * @param dynamicFilters        The dynamic filters.
     * @param pageNum               A number with contains the report page number.
     * @param pageSize              A number with contains the report page size.
     * @param startDate             Report start date.
     * @param endDate               Report start date.
     * @param now                   Report current date.
     * @param addNonUdfInProperties Flag for add non UDF in properties.
     * @return A {@link File} that contains the report exported.
     * @throws Exception An Exception that describe the error message.
     */
    public File getFileResult(ReportDefinition reportDefinition, Map<String, Object> dynamicFilters, Integer pageNum,
                              Integer pageSize, Date startDate, Date endDate, Date now, boolean addNonUdfInProperties,
                              String comment) throws Exception {
        File result;
        ReportConfig reportConfig = null;
        switch (reportDefinition.getReportType()) {
            case REPORT_TYPE_MAP:
            case REPORT_TYPE_TABLE_DETAIL:
                reportConfig = new TableDetailReportConfig(reportDefinition, dynamicFilters, pageNum, pageSize,
                        startDate, endDate, now, addNonUdfInProperties, dateFormatAndTimeZone, false);
                break;
            case REPORT_TYPE_MAP_HISTORY:
            case REPORT_TYPE_TABLE_HISTORY:
                if (reportDefinition.getName().toLowerCase().contains("_exit")) {
                    reportConfig = new TableHistoryExitReportConfig(reportDefinition,
                            dynamicFilters, pageNum, pageSize, startDate, endDate, now, addNonUdfInProperties, dateFormatAndTimeZone);
                } else {
                    reportConfig = new TableHistoryReportConfig(reportDefinition,
                            dynamicFilters, pageNum, pageSize, startDate, endDate, now, addNonUdfInProperties, dateFormatAndTimeZone);
                }
                break;
            case REPORT_TYPE_TABLE_SCRIPT:
                User user = (User) SecurityUtils.getSubject().getPrincipal();
                TableScriptConfig tableScriptConfig = new TableScriptConfig(reportDefinition,
                        dynamicFilters, pageNum, pageSize, startDate, endDate, now, addNonUdfInProperties, user);
                TableScriptExecution ts = new TableScriptExecution(reportDefinition,
                        dynamicFilters, pageSize, pageNum, startDate, endDate, now, comment, tableScriptConfig, isSentEmail);
                return ts.export();
            case REPORT_TYPE_TABLE_SUMMARY:
                TableSummaryReportConfig tableSummaryReportConfig = new TableSummaryReportConfig(reportDefinition,
                        dynamicFilters, pageNum, pageSize, startDate, endDate, now, dateFormatAndTimeZone);
                TableSummaryTranslateExport translateExport = new TableSummaryTranslateExport(tableSummaryReportConfig);
                return translateExport.exportSummary();
            case REPORT_TYPE_TABLE_CONNECTION:
                TableScriptConfig tableConnectionConfig = new TableScriptConfig(reportDefinition,
                        dynamicFilters, pageNum, pageSize, startDate, endDate, now, addNonUdfInProperties, (User) SecurityUtils.getSubject().getPrincipal());
                TableConnectionExecution tcs = new TableConnectionExecution(reportDefinition, dynamicFilters,
                        pageSize, pageNum, startDate, endDate, now, comment, tableConnectionConfig, isSentEmail, true);
                return tcs.export();

        }
        ReportExport reportExport = new ReportExport(reportConfig, comment, now, isSentEmail, reportDefinition.isDateFormatColumns());
        result = reportExport.export();
        return result;
    }

    public void setSentEmail(boolean sentEmail) {
        isSentEmail = sentEmail;
    }
}
