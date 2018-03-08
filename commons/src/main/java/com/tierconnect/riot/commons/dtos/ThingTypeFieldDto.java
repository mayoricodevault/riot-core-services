package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;

import java.io.Serializable;

/**
 * ThingTypeFieldDto class.
 *
 * @author jantezana
 * @author achambi
 * @version 2017/01/25
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ThingTypeFieldDto implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long id;
    public String name;
    public String defaultValue;
    public Boolean multiple;
    public String symbol;
    public Long thingTypeFieldTemplateId;
    public Boolean timeSeries;
    public Long timeToLive;
    public String typeParent;
    public String unit;
    public Long dataTypeId;
    public Long dataTypeThingTypeId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThingTypeFieldDto that = (ThingTypeFieldDto) o;
        return Objects.equal(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
