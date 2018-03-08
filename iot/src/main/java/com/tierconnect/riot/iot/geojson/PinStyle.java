package com.tierconnect.riot.iot.geojson;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Created by user on 11/3/14.
 */

@JsonSerialize(using = PinStyleSerializer.class)
public class PinStyle {
    private String color;
    private String iconImage;
    private Long timestamp;

    public PinStyle(String color, String iconImage, Long timestamp) {
        this.color = color;
        this.iconImage = iconImage;
        this.timestamp = timestamp;
    }

    public void setColor(String color) {
        this.color = color;
    }
    public String getColor() {
        return this.color;
    }

    public void setIconImage(String iconImage) {
        this.iconImage = iconImage;
    }
    public String getIconImage() {
        return this.iconImage;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    public Long getTimestamp() {
        return this.timestamp;
    }

}
