package com.tierconnect.riot.api.mongoShell;

import java.util.List;
import java.util.Map;

/**
 * Created by achambi on 12/9/16.
 * entity to get result
 */
public class ResultQuery {

    private int total;

    private MongoErrorMessage errorMessage;
    private List<Map<String, Object>> rows;
    private Map<String, Object> executionPlan;

    public ResultQuery(int total, List<Map<String, Object>> rows, Map<String, Object> executionPlan) {
        this.total = total;
        this.rows = rows;
        this.executionPlan = executionPlan;
    }

    public ResultQuery(int total, MongoErrorMessage errorMessage) {
        this.total = total;
        this.errorMessage = errorMessage;
    }

    public int getTotal() {
        return total;
    }

    public List<Map<String, Object>> getRows() {
        return rows;
    }

    public Map<String, Object> getExecutionPlan() {
        return executionPlan;
    }

    public MongoErrorMessage getErrorMessage() {
        return errorMessage;
    }
}
