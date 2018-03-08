package com.tierconnect.riot.commons.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Preconditions;
import com.tierconnect.riot.commons.dtos.GroupDto;
import com.tierconnect.riot.commons.dtos.MetaDto;
import com.tierconnect.riot.commons.dtos.ThingDto;
import com.tierconnect.riot.commons.dtos.ThingPropertyDto;
import com.tierconnect.riot.commons.dtos.ThingTypeDto;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * Created by achambi on 11/7/16.
 * ThingDeserializer class.
 */
public class ThingDeserializer extends StdDeserializer<ThingDto> {
	private static final long serialVersionUID = 1L;

	private static Logger logger = Logger.getLogger(ThingDeserializer.class);

    /**
     * Default constructor of {@link com.tierconnect.riot.commons.serializers.ThingDeserializer}
     */
    public ThingDeserializer() {
        this(null);
    }

    /**
     * Creates an instance of {@link com.tierconnect.riot.commons.serializers.ThingDeserializer}
     *
     * @param vc the class
     */
    protected ThingDeserializer(Class<?> vc) {
        super(vc);
    }

    /**
     * Overridden method to deserialize json string to java classes.
     *
     * @param jp      Object to parse the Json
     * @param context deserializer context.
     * @return Thing Wrapper instance with contains the json parsed.
     * @throws IOException input/output exception.
     */
    @Override
    public ThingDto deserialize(JsonParser jp,
                                DeserializationContext context)
    throws IOException {
    	JsonNode node = null;
        try {
            ObjectMapper mapper = (ObjectMapper) jp.getCodec();
            SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
            mapper.setDateFormat(dateFormat);

            node = mapper.readTree(jp);
            
            JsonNode serialNumberNode = node.get("serialNumber");
            JsonNode timeNode = node.get("time");
            JsonNode thingTypeNode = node.get("thingType");
            JsonNode metaNode = node.get("meta");

            Preconditions.checkNotNull(serialNumberNode, "serialNumber node is null");
            Preconditions.checkNotNull(timeNode, "time node is null");
            Preconditions.checkNotNull(thingTypeNode, "thingType node is null");
            Preconditions.checkNotNull(metaNode, "meta node is null");

            // TODO: Adds validations in the deserializer.
            //            Assertions.voidNotNull("serialNumber", node.get("serialNumber"));
            //            Assertions.voidNotNull("time", node.get("time"));
            //            Assertions.voidNotNull("thingType", node.get("thingType"));
            // REMOVED VALIDATIONS
            //            if(!topic.equals("___v1___datain")){
            //                Assertions.voidNotNull("id", node.get("id"));
            //                Assertions.voidNotNull("name", node.get("name"));
            //                Assertions.voidNotNull("createdTime", node.get("createdTime"));
            //                Assertions.voidNotNull("modifiedTime", node.get("modifiedTime"));
            //                Assertions.voidNotNull("group", node.get("group"));
            //
            //                Assertions.isTrueArgument("id", "Long", node.get("id").isNumber());
            //                Assertions.isTrueArgument("name", "String", node.get("name").isTextual());
            //                Assertions.isTrueArgument("createdTime", node.get("createdTime").asText(), Constants.DATE_REG_EXP);
            //                Assertions.isTrueArgument("modifiedTime", node.get("modifiedTime").asText(), Constants.DATE_REG_EXP);
            //                Assertions.isTrueArgument("group", "Group Class", node.get("group").isObject());
            //            }

            //            Assertions.isTrueArgument("serialNumber", "String", node.get("serialNumber").isTextual());
            //            Assertions.isTrueArgument("time", node.get("time").asText(), Constants.DATE_REG_EXP);
            //            Assertions.isTrueArgument("thingType", "ThingType Class", node.get("thingType").isObject());

            // Build thing DTO.
            MetaDto metaDto = mapper.treeToValue(metaNode, MetaDto.class);
            long id = (node.get("id") == null) ? 0L : node.get("id").longValue();
            String serialNumber = serialNumberNode.asText();
            String name = (node.get("name") == null) ? null : node.get("name").asText();
            Date createTime = (node.get("createdTime") == null) ? null : dateFormat.parse(node.get("createdTime").asText());
            Date modifiedTime = (node.get("modifiedTime") == null) ? null : dateFormat.parse(node.get("modifiedTime").asText());
            
            Date time = dateFormat.parse(timeNode.asText());
            GroupDto groupDto = (node.get("group") == null) ? null : mapper.treeToValue(node.get("group"), GroupDto.class);
            
            ThingTypeDto thingTypeDto = mapper.treeToValue(thingTypeNode, ThingTypeDto.class);
            List<Map<String, ThingPropertyDto>> properties = deserializeProperties(mapper, (ArrayNode) node.get("properties"));

            ThingDto thingDto = new ThingDto();
            thingDto.meta = metaDto;
            thingDto.id = id;
            thingDto.serialNumber = serialNumber;
            thingDto.name = name;
            thingDto.createdTime = createTime;
            thingDto.modifiedTime = modifiedTime;
            thingDto.time = time;
            thingDto.group = groupDto;
            thingDto.thingType = thingTypeDto;
            thingDto.properties = properties;

            return thingDto;
        } catch (Exception e) {
            throw new IOException("Error parsing thing: "+node, e);
        }
    }

    /**
     * Deserialize properties to list class.
     *
     * @param mapper     class to mapper the json String.
     * @param properties properties to convert in java class.
     * @return list of map[{@link String}, {@link com.tierconnect.riot.commons.dtos.ThingPropertyDto}]
     * @throws JsonProcessingException Exception to parse json string.
     */
    private List<Map<String, ThingPropertyDto>> deserializeProperties(ObjectMapper mapper,
                                                                      ArrayNode properties)
    throws JsonProcessingException {
        List<Map<String, ThingPropertyDto>> propertyList = new ArrayList<>();
        if (properties != null) {
            for (JsonNode propertyItem : properties) {
                Map<String, ThingPropertyDto> thingPropertyWrapperMap = new HashMap<>();
                Iterator<String> keys = propertyItem.fieldNames();
                while (keys.hasNext()) {
                    String currentKey = keys.next();
                    JsonNode entry = propertyItem.get(currentKey);
                    thingPropertyWrapperMap.put(currentKey, mapper.treeToValue(entry, ThingPropertyDto.class));
                }
                propertyList.add(thingPropertyWrapperMap);
            }
        }
        return propertyList;
    }
}
