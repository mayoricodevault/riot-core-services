package com.tierconnect.riot.commons.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.common.base.Preconditions;
import com.tierconnect.riot.commons.dtos.ThingPropertyDto;
import com.tierconnect.riot.commons.utils.FormatUtil;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ThingPropertyDeserializer class.
 *
 * @author achambi
 * @version 2016/11/07
 */
public class ThingPropertyDeserializer extends StdDeserializer<ThingPropertyDto> {
	private static final long serialVersionUID = 1L;

	private Logger logger = Logger.getLogger(ThingPropertyDeserializer.class);

    ThingPropertyDeserializer() {
        this(null);
    }

    ThingPropertyDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ThingPropertyDto deserialize(JsonParser jp,
                                        DeserializationContext context)
    throws IOException {
        JsonNode node = null;
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);

            // required nodes
            node = jp.getCodec().readTree(jp);
            JsonNode timeNode = node.get("time");
            
            Preconditions.checkNotNull(timeNode, "Time node is null.");
            
            // non required nodes
            JsonNode blinkedNode = node.get("blinked");
            JsonNode modifiedNode = node.get("modified");
            JsonNode timeSeriesNode = node.get("timeSeries");
            JsonNode idNode = node.get("id");
            JsonNode valueNode = node.get("value");
            
            // processing
			Boolean blinked = blinkedNode != null ? blinkedNode.asBoolean() : null;
            Boolean modified = modifiedNode != null ? modifiedNode.asBoolean() : null;
            Boolean timeSeries = timeSeriesNode != null ? timeSeriesNode.asBoolean() : null;
			final long id = (idNode == null) ? 0L : idNode.longValue();
			final Date time = dateFormat.parse(timeNode.textValue());

            boolean hasDataType = node.has("dataTypeId"); 
            Long dataTypeId = null;
            Object value = null;
            
			if (valueNode!=null && !valueNode.isNull()) {
				
				if (hasDataType) {
		            
					JsonNode dataTypeIdNode = node.get("dataTypeId");
					DataType dataType = DataType.getDataTypeById(dataTypeIdNode.longValue());
					if (dataType != null) {
						dataTypeId = dataType.getId();
					}
		            
					if (dataType.getType().equals("NATIVE")) {
						value = jp.getCodec().treeToValue(valueNode, dataType.getClazz());
					} else {
                        if(logger.isDebugEnabled()){
                            logger.debug(String.format("Deserialize dataTypeMethodName: %s, dataTypeArgument: %s",
                                    dataType.getParseMethodName(), dataType.getArgumentType()));
                        }

						Method m = dataType.getClazz().getMethod(dataType.getParseMethodName(),
								dataType.getArgumentType());

                        String argument = valueNode.isTextual() ?  valueNode.textValue() : valueNode.toString();
                        value = m.invoke(null, argument);
//						if (valueNode.isArray() || valueNode.isNumber() || valueNode.isBoolean() || valueNode.isTextual()) {
//							value = m.invoke(null, valueNode.toString());
//						} else {
//                            value = m.invoke(null, valueNode.textValue());
//						}
					}
					dataTypeId = dataType.getId();
				} else if (valueNode.isArray()) {
					value = FormatUtil.parseArray(valueNode.toString());
				} else {

					value = valueNode.textValue();
				}
			}

			if(logger.isDebugEnabled()){
                logger.debug(String.format("Deserialize thingProperty id: %s, dataTypeId: %s and value: %s", id, dataTypeId,value));
            }

            ThingPropertyDto thingPropertyDto = new ThingPropertyDto();
            thingPropertyDto.id = id;
            thingPropertyDto.time = time;
            thingPropertyDto.blinked = blinked;
            thingPropertyDto.modified = modified;
            thingPropertyDto.value = value;
            thingPropertyDto.timeSeries = timeSeries;
            thingPropertyDto.dataTypeId = dataTypeId;	

            return thingPropertyDto;
        }catch (Exception e) {
            throw new IOException("Error parsing thing property: "+node, e);
        }
    }
}
