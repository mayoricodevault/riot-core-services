package com.tierconnect.riot.api.database.utils;

/**
 * Created by vealaro on 12/29/16.
 */
public class Zone {
    private final String code;
    private final String name;
    private final String facilityMap;
    private final String zoneGroup;
    private final String zoneType;

    public Zone(String code, String name, String facilityMap, String zoneGroup, String zoneType) {
        this.code = code;
        this.name = name;
        this.facilityMap = facilityMap;
        this.zoneGroup = zoneGroup;
        this.zoneType = zoneType;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getFacilityMap() {
        return facilityMap;
    }

    public String getZoneGroup() {
        return zoneGroup;
    }

    public String getZoneType() {
        return zoneType;
    }
}
