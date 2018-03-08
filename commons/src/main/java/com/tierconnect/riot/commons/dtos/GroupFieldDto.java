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
public class GroupFieldDto implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long id;
    public String value;
    public GroupDto group;
    public FieldDto fieldDto;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GroupFieldDto groupFieldDto = (GroupFieldDto) o;
        return Objects.equal(id, groupFieldDto.id)
                && Objects.equal(value, groupFieldDto.value)
                && Objects.equal(group, groupFieldDto.group)
                && Objects.equal(fieldDto,groupFieldDto.fieldDto);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, value, group, fieldDto);
    }
}
