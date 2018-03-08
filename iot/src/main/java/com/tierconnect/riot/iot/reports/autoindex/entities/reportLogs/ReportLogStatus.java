package com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs;

/**
 * Created by achambi on 5/9/17.
 * Enum Class from Report Log Status.
 */
public enum ReportLogStatus {

    IN_PROGRESS("inProgress", "In Progress"),
    PENDING("pending", "Pending"),
    COMPLETED("completed", "Completed"),
    SLOW_INDEX("slowIndex", "Slow Index"),
    CANCELED("canceled", "Cancelled"),
    DELETED("deleted", "Deleted"),
    ERROR("error", "Error");

    private final String statusValue;
    private final String label;

    ReportLogStatus(final String statusValue, final String label) {
        this.statusValue = statusValue;
        this.label = label;
    }

    public String getValue() {
        return statusValue;
    }

    public String getLabel() {
        return label;
    }

    public static ReportLogStatus getEnum(String statusValue) {
        for (ReportLogStatus v : values())
            if (v.getValue().equalsIgnoreCase(statusValue)) return v;
        throw new IllegalArgumentException();
    }

}
