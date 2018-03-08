package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;
import java.util.List;

/**
 * Dto implemented for old JS rules.
 * Remove it after rule update to use ThingDto
 * Created by vramos on 4/6/17.
 */
public class ZoneTypeDto implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long id;
    public String name;
    public String code;
    public List<ZoneTypePropertyDto> zoneProperties;

    @JsonIgnore
    public String getZoneTypeCode(){
        return code;
    }

    public String getName(){
        return name;
    }

    public List<ZoneTypePropertyDto> getZoneProperties(){
        return zoneProperties;
    }
    
    public Long getId() {
        return id;
    }
}
