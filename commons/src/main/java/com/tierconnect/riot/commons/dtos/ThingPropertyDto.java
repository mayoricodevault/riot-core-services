package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.base.Objects;
import com.tierconnect.riot.commons.serializers.ThingPropertyDeserializer;
import com.tierconnect.riot.commons.serializers.ThingPropertySerializer;

import java.io.Serializable;
import java.util.Date;

/**
 * ThingPropertyDto class.
 *
 * @author jantezana
 * @author achambi
 * @version 2017/01/25
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(using = ThingPropertySerializer.class)
@JsonDeserialize(using = ThingPropertyDeserializer.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ThingPropertyDto implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long id;
    public Date time;
    public Boolean blinked;
    public Boolean modified;
    public Boolean ruleChanged;
    public Boolean timeSeries;
    public Object value;
    public Long dataTypeId;

    public Object getValue(){
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThingPropertyDto that = (ThingPropertyDto) o;
        return Objects.equal(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
