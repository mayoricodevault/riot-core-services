package com.tierconnect.riot.iot.geojson;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.geojson.LngLatAlt;

@JsonSerialize(using = LngLatAltTimeSerializer.class)
public class LngLatAltTime extends LngLatAlt implements Comparable<LngLatAltTime> {

    private Long time;
    private Double color;

    public LngLatAltTime(Double lng, Double lat, Double alt, Long time) {
        super(lng, lat, alt);
        this.time = time;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public boolean hasTime() {
        return time != null;
    }

    public boolean hasColor() {
        return color != null;
    }

    public Double getColor() {
        return color;
    }

    public void setColor(Double color) {
        this.color = color;
    }

    @Override
    public int compareTo(LngLatAltTime lngLatAltTime) {
        if (lngLatAltTime != null) {
            return this.getTime().compareTo((lngLatAltTime.getTime()));
        }
        return 1;
    }
}
