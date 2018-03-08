package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;

import java.io.Serializable;

/**
 * GroupTypeDto class.
 *
 * @author jantezana
 * @version 2017/01/31
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupTypeDto implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long id;
    public String name;
    public String code;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupTypeDto that = (GroupTypeDto) o;
        return Objects.equal(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
