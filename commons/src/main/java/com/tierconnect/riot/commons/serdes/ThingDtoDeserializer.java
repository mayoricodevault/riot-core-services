package com.tierconnect.riot.commons.serdes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.tierconnect.riot.commons.dtos.ThingDto;
import com.tierconnect.riot.commons.serializers.ThingDeserializer;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Map;

import static com.tierconnect.riot.commons.serializers.Constants.DATE_FORMAT;

/**
 * ThingDtoDeserializer class..
 *
 * @author jantezana
 * @version 2017/02/22
 */
public class ThingDtoDeserializer implements Deserializer<ThingDto> {
    private ObjectMapper mapper = new ObjectMapper();
    
    public ThingDtoDeserializer() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(ThingDto.class, new ThingDeserializer());
        mapper.registerModule(module);    	
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setDateFormat(new SimpleDateFormat(DATE_FORMAT));
    }
    
    @Override
    public void configure(Map<String, ?> map,
                          boolean b) {
    }

    @Override
    public ThingDto deserialize(String topic,
                                byte[] data) {
    	ThingDto thingDto = null;
        if (data != null) {
            try {
                thingDto = mapper.readValue(data, ThingDto.class);
            } catch (IOException e) {
            	throw new IllegalArgumentException(e);
            }
        }

        return thingDto;
    }

    @Override
    public void close() {
    }
    
    public static void main(String args[]) throws UnsupportedEncodingException {
    	String source = "{\"meta\":{\"bridgeCode\":null,\"sqn\":null,\"specName\":null,\"units\":null,\"partition\":null,\"numPartitions\":null,\"reblinked\":null,\"outOfOrder\":null,\"newBlink\":null},\"id\":null,\"serialNumber\":\"0001\",\"name\":null,\"createdTime\":\"2017-02-25T10:06:15.457-04:00\",\"modifiedTime\":\"2017-02-25T10:06:15.457-04:00\",\"time\":\"2017-02-25T10:06:15.457-04:00\",\"group\":{\"groupType\":{\"id\":null,\"name\":null,\"code\":null},\"id\":null,\"name\":null,\"code\":null},\"thingType\":{\"id\":null,\"name\":null,\"code\":null},\"properties\":[{},{}]}";
    	ThingDtoDeserializer des = new ThingDtoDeserializer();

    	long t0 = System.currentTimeMillis();
    	for (int i = 0; i < 10000; i++) {
    		des.deserialize("", source.getBytes("UTF8"));
		}
    	long t1 = System.currentTimeMillis();
    	System.out.println("time: " + (t1 -t0));
    	des.close();
    }
}
