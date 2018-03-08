package com.tierconnect.riot.iot.reports.views.things.dto;

import java.util.List;
import java.util.Map;

/**
 * Created by achambi on 7/31/17.
 * DTO class to store the thing list results.
 */
public class ListResult {

    private Long total;
    private List<Map<String, Object>> results;

    public ListResult() {

    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public void setResults(List<Map<String, Object>> results) {
        this.results = results;
    }

    public Long getTotal() {
        return total;
    }

    public List<Map<String, Object>> getResults() {
        return results;
    }
}
