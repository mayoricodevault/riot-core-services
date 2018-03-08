package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;

import java.io.Serializable;

/**
 * EdgeboxDto class.
 *
 * @author jantezana
 * @version 2017/02/06
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EdgeboxConfigurationDto implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long id;
    public String code;
    public String type;
    public String configuration;
    public GroupDto group;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EdgeboxConfigurationDto that = (EdgeboxConfigurationDto) o;
        return Objects.equal(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
    
    public Long getId() {
        return id;
    }
}