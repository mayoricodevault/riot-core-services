package com.tierconnect.riot.iot.reports_integration;

import java.util.Map;

/**
 * Created by vealaro on 1/11/17.
 */
public interface ITranslateResult {

    Map<String, Object> getLabelValues();

    void exportResult(Map<String, Object> result);
}
