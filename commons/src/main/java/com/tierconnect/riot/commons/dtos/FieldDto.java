package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;

import java.io.Serializable;

/**
 * GroupFieldDto class.
 *
 * @author aquiroz
 * @author jantezana
 * @author achambi
 * @version 2017/01/25
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FieldDto implements Serializable {

    private static final long serialVersionUID = 1L;
    public Long id;
    public String name;
    public String description;
    public Long editLevel;
    public String module;
    public String type;
    public Boolean userEditable;
    public GroupDto group;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldDto fieldDto = (FieldDto) o;
        return Objects.equal(id, fieldDto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
