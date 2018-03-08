package com.tierconnect.riot.commons.serdes;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.tierconnect.riot.commons.dtos.GroupDto;
import com.tierconnect.riot.commons.dtos.GroupTypeDto;
import com.tierconnect.riot.commons.dtos.MetaDto;
import com.tierconnect.riot.commons.dtos.ThingDto;
import com.tierconnect.riot.commons.dtos.ThingTypeDto;
import com.tierconnect.riot.commons.serializers.ThingSerializer;
import org.apache.kafka.common.serialization.Serializer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.tierconnect.riot.commons.serializers.Constants.DATE_FORMAT;

/**
 * ThingDtoSerializer class.
 *
 * Note: This class is not thread safe.
 * @author jantezana
 * @version 2017/02/22
 */
public class ThingDtoSerializer implements Serializer<ThingDto> {
    private ObjectMapper mapper = new ObjectMapper();
    
    public ThingDtoSerializer() {
        this.mapper.registerModule(new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
                context.addBeanSerializerModifier(new BeanSerializerModifier() {
                    @Override
                    public com.fasterxml.jackson.databind.JsonSerializer<?> modifySerializer(SerializationConfig config,
                                                                                             BeanDescription beanDesc,
                                                                                             com.fasterxml.jackson.databind.JsonSerializer<?> serializer) {
                        if (ThingDto.class.isAssignableFrom(beanDesc.getBeanClass())) {
                            return new ThingSerializer();
                        }

                        return serializer;
                    }
                });
            }
        });
        mapper.setDateFormat(new SimpleDateFormat(DATE_FORMAT));
    }

	@Override
    public void configure(Map<String, ?> map, boolean b) {
    }

    @Override
    public byte[] serialize(final String s,
                            ThingDto thingDto) {
        try {
            return mapper.writeValueAsBytes(thingDto);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void close() {
    }
    
    public static void main(String args[]) {
    	ThingDtoSerializer ser = new ThingDtoSerializer();
    	
    	ThingDto thingDto = new ThingDto();
    	thingDto.serialNumber = "0001";
    	thingDto.meta = new MetaDto();
    	thingDto.createdTime = new Date();
    	thingDto.modifiedTime = new Date();
    	thingDto.time = new Date();
    	thingDto.group = new GroupDto();
    	thingDto.group.groupType = new GroupTypeDto();
    	thingDto.thingType = new ThingTypeDto();
    	thingDto.properties = new ArrayList<>();
    	thingDto.properties.add(new HashMap<>());
    	thingDto.properties.add(new HashMap<>());
   
    	long t0 = System.currentTimeMillis();
    	for (int i = 0; i < 10000; i++) {
    		ser.serialize("", thingDto);	
		}
    	long t1 = System.currentTimeMillis();
    	System.out.println("total time: " + (t1-t0));
    	ser.close();
    }
}
