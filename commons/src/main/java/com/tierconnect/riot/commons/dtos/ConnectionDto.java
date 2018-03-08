package com.tierconnect.riot.commons.dtos;

import com.google.common.base.Objects;

import java.util.Map;

/**
 * Created by vramos on 2/19/17.
 */
public class ConnectionDto {
    public Long id;
    public String code;
    public String name;
    public Map<String, Object> properties;
    public Long connectionTypeId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionDto that = (ConnectionDto) o;
        return Objects.equal(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
    
    public Long getId() {
        return id;
    }

    public String getPropertyAsString(String propertyCode) {
        return properties.get(propertyCode).toString();
    }

    public Integer getPropertyAsNumber(String propertyCode) {
        return Integer.parseInt(getPropertyAsString(propertyCode));
    }

    public Boolean getPropertyAsBoolean(String propertyCode) {
        return Boolean.valueOf(getPropertyAsString(propertyCode));
    }
}
