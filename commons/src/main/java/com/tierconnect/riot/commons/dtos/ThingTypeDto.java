package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * ThingTypeDto class.
 *
 * @author jantezana
 * @author achambi
 * @version 2017/01/25
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ThingTypeDto implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long id;
    public String name;
    public String code;
    public Map<String, ThingTypeFieldDto> fields;
    public Boolean archived;
    public Boolean autoCreate;
    public Boolean isParent;
    public Long groupId;
    public Long modifiedTime;
    public String serialFormula;
    public Long defaultOwnerGroupTypeId;
    public Long thingTypeTemplateId;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Map<String, ThingTypeFieldDto> getFields() {
        return fields;
    }

    public Boolean getArchived() {
        return archived;
    }

    public Boolean getAutoCreate() {
        return autoCreate;
    }

    public Boolean getIsParent() {
        return isParent;
    }

    public Long getGroupId() {
        return groupId;
    }

    public Long getModifiedTime() {
        return modifiedTime;
    }

    public String getSerialFormula() {
        return serialFormula;
    }

    public Long getDefaultOwnerGroupTypeId() {
        return defaultOwnerGroupTypeId;
    }

    public Long getThingTypeTemplateId() {
        return thingTypeTemplateId;
    }


    /**
     * This method verifies if a udf is : zone, LogicalReader, Shift or group.
     * @param fieldName
     * @return boolean
     */
    public boolean isFieldNativeUdf(final String fieldName){
        Preconditions.checkNotNull(fieldName, "The fieldName is null");
        boolean isNativeObject = false;
        ThingTypeFieldDto field = fields.get( fieldName );

        if( field != null &&
                (field.dataTypeId.longValue() == com.tierconnect.riot.commons.serializers.DataType.TYPE_ZONE.getId().longValue() ||
                        field.dataTypeId.longValue() == com.tierconnect.riot.commons.serializers.DataType.TYPE_LOGICAL_READER.getId().longValue() ||
                        field.dataTypeId.longValue() == com.tierconnect.riot.commons.serializers.DataType.TYPE_SHIFT.getId().longValue() ||
                        field.dataTypeId.longValue() == com.tierconnect.riot.commons.serializers.DataType.TYPE_GROUP.getId().longValue()) )
        {
            isNativeObject = true;
        }
        return isNativeObject;
    }

    public boolean hasField(String fieldName){
        Preconditions.checkNotNull(fieldName, "The fieldName is null");
        boolean hasField = false;
        ThingTypeFieldDto field = fields.get( fieldName );
        if (field!=null){
            hasField = true;
        }
        return hasField;
    }

    @JsonIgnore
    public List<ThingTypeFieldDto> getUdfs() {
        List<ThingTypeFieldDto> out = new LinkedList<>();
        if (fields != null) {
            out.addAll(fields.values());
            Collections.sort(out, Comparator.comparing(o -> o.name));
        }

        return out;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThingTypeDto that = (ThingTypeDto) o;
        return Objects.equal(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
