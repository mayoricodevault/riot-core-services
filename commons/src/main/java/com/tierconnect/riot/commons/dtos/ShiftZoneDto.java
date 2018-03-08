package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;

import java.io.Serializable;
import java.util.List;

/**
 * ShiftZoneDto class.
 *
 * @author jantezana
 * @version 2017/01/31
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShiftZoneDto implements Serializable {
    private static final long serialVersionUID = 1L;
    public ShiftDto shift;
    public List<ZoneDto> zones;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ShiftZoneDto that = (ShiftZoneDto) o;
        return Objects.equal(shift, that.shift) && Objects.equal(zones, that.zones);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(shift, zones);
    }
}
