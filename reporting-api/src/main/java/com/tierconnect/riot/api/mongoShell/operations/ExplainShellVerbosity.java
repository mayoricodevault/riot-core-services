package com.tierconnect.riot.api.mongoShell.operations;

/**
 * Created by achambi on 4/27/17.
 * Enum class to set the differents verbosity modes.
 */
public enum ExplainShellVerbosity {

    QUERY_PLANNER(ExplainShellVerbosity.QUERY_PLANNER_VALUE),
    EXECUTION_STATS(ExplainShellVerbosity.EXECUTION_STATS_VALUE),
    ALL_PLANS_EXECUTIONS(ExplainShellVerbosity.ALL_PLANS_EXECUTIONS_VALUE);


    public String getExplainVerbosity() {
        return explainVerbosity;
    }

    private final String explainVerbosity;

    ExplainShellVerbosity(final String explainVerbosity) {
        this.explainVerbosity = explainVerbosity;
    }

    private static final String QUERY_PLANNER_VALUE = "queryPlanner";
    private static final String EXECUTION_STATS_VALUE = "executionStats";
    private static final String ALL_PLANS_EXECUTIONS_VALUE = "allPlansExecution";
}
