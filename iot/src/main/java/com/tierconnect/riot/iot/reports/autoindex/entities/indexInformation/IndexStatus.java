package com.tierconnect.riot.iot.reports.autoindex.entities.indexInformation;

/**
 * Created by achambi on 5/26/17.
 * Enum Class from index information Status.
 */
public enum IndexStatus {

    STATS_GENERATED("statsGenerated"),
    DELETE_REQUEST("deleteRequest"),
    DELETE_PROCESS("deleteProcess"),
    DELETED("slowIndex");
    private final String statusValue;

    IndexStatus(final String statusValue) {
        this.statusValue = statusValue;
    }

    public String getValue() {
        return statusValue;
    }

    public static IndexStatus getEnum(String statusValue) {
        for (IndexStatus v : values())
            if (v.getValue().equalsIgnoreCase(statusValue)) return v;
        return null;
    }
}
