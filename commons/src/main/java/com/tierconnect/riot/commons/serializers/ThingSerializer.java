package com.tierconnect.riot.commons.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.base.Preconditions;
import com.tierconnect.riot.commons.dtos.GroupDto;
import com.tierconnect.riot.commons.dtos.ThingDto;
import com.tierconnect.riot.commons.dtos.ThingTypeDto;
import com.tierconnect.riot.commons.dtos.ThingTypeFieldDto;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;


/**
 * ThingSerializer class.
 *
 * @author dbascope
 * @version 2017/01/25
 *          <p>
 *          TODO: We need to review it. possible mistakes creating thing snapshots (dates in other formats, coordinate utils is in other format )
 */
public class ThingSerializer extends JsonSerializer<ThingDto> {

    private static final Logger logger = Logger.getLogger(ThingSerializer.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);

    @Override
    public void serialize(ThingDto value,
                          JsonGenerator gen,
                          SerializerProvider serializers)
    throws IOException {
        Preconditions.checkNotNull(value, "value is null");
        Preconditions.checkNotNull(value.meta, "meta node is null");
        Preconditions.checkNotNull(value.createdTime, "created time value is null");
        Preconditions.checkNotNull(value.modifiedTime, "modified time value is null");
        Preconditions.checkNotNull(value.time, "time value is null");
        Preconditions.checkNotNull(value.group, "group node is null");
        Preconditions.checkNotNull(value.group.groupType, "group type node is null");
        Preconditions.checkNotNull(value.thingType, "thing type node is null");
        Preconditions.checkNotNull(value.properties, "properties node is null");
//        Preconditions.checkArgument(value.properties.size() > 1, "properties has less than 2 elements");

        ObjectMapper objectMapper = (ObjectMapper)gen.getCodec();
        objectMapper.setDateFormat(this.dateFormat);

        gen.writeStartObject();
        // Generates meta node
        gen.writeObjectField("meta", value.meta);
        // Generates nonUDFs fields
        gen.writeObjectField("id", value.id);
        gen.writeObjectField("serialNumber", value.serialNumber);
        gen.writeObjectField("name", value.name);
        gen.writeObjectField("createdTime", dateFormat.format(value.createdTime));
        gen.writeObjectField("modifiedTime", dateFormat.format(value.modifiedTime));
        gen.writeObjectField("time", dateFormat.format(value.time));
        // Generate group node
        gen.writeObjectField("group", buildGroup(value.group));
        // Generate thingType node
        gen.writeObjectField("thingType", buildThingType(value.thingType));
        gen.writeObjectField("properties", value.properties);
        gen.writeEndObject();
    }

    /**
     * Builds a group
     *
     * @param group the group base to copy their data
     * @return the group
     */
    private GroupDto buildGroup(GroupDto group) {
        GroupDto result = new GroupDto();
        result.id = group.id;
        result.name = group.name;
        result.code = group.code;
        result.groupType = group.groupType;

        return result;
    }

    private ThingTypeDto buildThingType(ThingTypeDto thingType) {
        ThingTypeDto result = new ThingTypeDto();
        result.id = thingType.id;
        result.name = thingType.name;
        result.code = thingType.code;
        if(thingType.fields != null ){
            Map<String, ThingTypeFieldDto> fields = new HashMap<>();
            ThingTypeFieldDto field;
            for (ThingTypeFieldDto thingTypeFieldDto : thingType.fields.values()) {
                field = new ThingTypeFieldDto();
                field.id = thingTypeFieldDto.id;
                field.name = thingTypeFieldDto.name;
                field.dataTypeId = thingTypeFieldDto.dataTypeId;
                fields.put(thingTypeFieldDto.name, field);
            }
            result.fields = fields;
        }
        return result;
    }
}
