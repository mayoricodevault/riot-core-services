package com.tierconnect.riot.iot.utils;

import java.util.Date;
import java.util.Map;

/**
 * Created by user on 5/18/15.
 */
public class MapCacheData {
    Date date;

    public Map<String, Object> getGeoJsonMap() {
        return geoJsonMap;
    }

    public void setGeoJsonMap(Map<String, Object> geoJsonMap) {
        this.geoJsonMap = geoJsonMap;
    }

    Map<String,Object> geoJsonMap;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
