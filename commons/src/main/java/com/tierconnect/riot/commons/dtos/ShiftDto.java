package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;
import org.bson.Document;

import java.io.Serializable;

/**
 * ShiftDto class.
 *
 * @author jantezana
 * @author achambi
 * @version 2017/01/25
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ShiftDto implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long id;
    public String name;
    public String code;
    public Boolean active;
    public Long startTimeOfDay;
    public Long endTimeOfDay;
    public String daysOfWeek;
    public Long groupId;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Boolean getActive() {
        return active;
    }

    public Long getStartTimeOfDay() {
        return startTimeOfDay;
    }

    public Long getEndTimeOfDay() {
        return endTimeOfDay;
    }

    public String getDaysOfWeek() {
        return daysOfWeek;
    }

    public Long getGroupId() {
        return groupId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ShiftDto shiftDto = (ShiftDto) o;
        return Objects.equal(id, shiftDto.id) && Objects.equal(active, shiftDto.active) && Objects.equal(startTimeOfDay, shiftDto.startTimeOfDay)
            && Objects.equal(endTimeOfDay, shiftDto.endTimeOfDay) && Objects.equal(daysOfWeek, shiftDto.daysOfWeek);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, active, startTimeOfDay, endTimeOfDay, daysOfWeek);
    }

    /**
     * Gets a mongo document to be used by mongo ingestor
     * @return              Mongo Document
     */
    public Document toDocument(){
        return new Document("id", id)
                .append("code", code)
                .append("name", name)
                .append("active", active)
                .append("startTimeOfDay", startTimeOfDay)
                .append("endTimeOfDay", endTimeOfDay)
                .append("daysOfWeek", daysOfWeek)
                ;
    }
}
