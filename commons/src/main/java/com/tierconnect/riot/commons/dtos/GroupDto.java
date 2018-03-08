package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;
import org.bson.Document;

import java.io.Serializable;

/**
 * GroupDto class.
 *
 * @author jantezana
 * @author achambi
 * @version 2017/01/25
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupDto implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long id;
    public String name;
    public String code;
    public GroupTypeDto groupType;
    public String description;
    public String hierarchyName;
    public Boolean archived;
    public Integer treeLevel;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public GroupTypeDto getGroupType() {
        return groupType;
    }

    public String getDescription() {
        return description;
    }

    public String getHierarchyName() {
        return hierarchyName;
    }

    public Boolean getArchived() {
        return archived;
    }

    public Integer getTreeLevel() {
        return treeLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GroupDto groupDto = (GroupDto) o;
        return Objects.equal(id, groupDto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /**
     * Gets a mongo document to be used by mongo ingestor
     * @return              Mongo Document
     */
    public Document toDocument(){
        return new Document("id", id)
                .append("code", code)
                .append("name", name)
                .append("description", description)
                .append("hierarchyName", hierarchyName)
                .append("archived", archived)
                .append("treeLevel", treeLevel)
                ;
    }
}
