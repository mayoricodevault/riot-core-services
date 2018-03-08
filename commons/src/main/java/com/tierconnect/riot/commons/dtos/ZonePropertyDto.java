package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;

import java.io.Serializable;
import java.util.Date;

/**
 * ZonePropertyDto class, Class to parse a property value [zoneType, zoneGroup, facilityMap]
 *
 * @author jantezana
 * @author achambi
 * @version 2017/01/25
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ZonePropertyDto implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long id;
    public String name;
    public String code;
    public Date time;
    public boolean blinked;
    public Boolean modified;
    public Boolean timeSeries;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Date getTime() {
        return time;
    }

    public boolean isBlinked() {
        return blinked;
    }

    public Boolean getModified() {
        return modified;
    }

    public Boolean getTimeSeries() {
        return timeSeries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ZonePropertyDto that = (ZonePropertyDto) o;
        return Objects.equal(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
