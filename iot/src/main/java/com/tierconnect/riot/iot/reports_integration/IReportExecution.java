package com.tierconnect.riot.iot.reports_integration;

import java.util.Map;

/**
 * Created by vealaro on 2/3/17.
 * Template ReportExecution.
 */
public interface IReportExecution {
    void run() throws Exception;

    Map<String, Object> getReportInfo();

    Map<String, Object> getMapResult();

    void setEnableSaveReportLog(boolean enableSaveReportLog);

    boolean isEnableSaveReportLog();

    String getReportIndex();
}
