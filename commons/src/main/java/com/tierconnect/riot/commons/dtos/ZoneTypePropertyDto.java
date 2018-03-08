package com.tierconnect.riot.commons.dtos;

import java.io.Serializable;

/**
 * Dto implemented for old JS rules.
 * Remove it after rule update to use ThingDto
 * Created by vramos on 4/6/17.
 */
public class ZoneTypePropertyDto implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long id;
    public String name;
    public Integer type;
}
