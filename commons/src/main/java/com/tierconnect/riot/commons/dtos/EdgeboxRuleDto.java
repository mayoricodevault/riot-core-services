package com.tierconnect.riot.commons.dtos;

import com.google.common.base.Objects;

/**
 * Created by vramos on 2/19/17.
 */
public class EdgeboxRuleDto {
    public Long id;
    public Boolean active;
    public String cronSchedule;
    public String description;
    public Boolean executeLoop;
    public Boolean honorLastDetect;
    public String input;
    public String name;
    public String output;
    public String outputConfig;
    public String rule;
    public Integer sortOrder;
    public Long edgeboxId;
    public Long groupId;
    public String conditionType;
    public String parameterConditionType;
    public Boolean runOnReorder;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EdgeboxRuleDto that = (EdgeboxRuleDto) o;
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
