package com.tierconnect.riot.commons;

import com.tierconnect.riot.commons.dtos.ThingPropertyDto;

import java.util.Map;

/**
 * Created by dbascope on 1/3/17
 */
public class PropertyWrapper {
    public Map<String, ThingPropertyDto> current;

    public Map<String, ThingPropertyDto> previous;

    public PropertyWrapper() {
    }

    public PropertyWrapper(Map<String, ThingPropertyDto> current,
                           Map<String, ThingPropertyDto> previous) {
        this.current = current;
        this.previous = previous;
    }

    public Map<String, ThingPropertyDto> getCurrent() {
        return current;
    }

    public void setCurrent(Map<String, ThingPropertyDto> current) {
        this.current = current;
    }

    public Map<String, ThingPropertyDto> getPrevious() {
        return previous;
    }

    public void setPrevious(Map<String, ThingPropertyDto> previous) {
        this.previous = previous;
    }
}