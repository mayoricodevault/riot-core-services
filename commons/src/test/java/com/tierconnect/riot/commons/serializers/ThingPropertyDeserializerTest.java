package com.tierconnect.riot.commons.serializers;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.tierconnect.riot.commons.dtos.ThingPropertyDto;

public class ThingPropertyDeserializerTest {

	{
		SimpleModule module = new SimpleModule();
		module.addDeserializer(ThingPropertyDto.class, new ThingPropertyDeserializer());
	}

	@Test
	public void testTimestamp() throws JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		String source = buildProperty(24, "1488023456");
		ThingPropertyDto value = mapper.readValue(source.getBytes(), ThingPropertyDto.class);
		Assert.assertEquals(value.value, 1488023456L);
	}
	
	@Test
	public void testNullDate() throws JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		String source = buildProperty(11, "null");
		ThingPropertyDto value = mapper.readValue(source.getBytes(), ThingPropertyDto.class);
		Assert.assertNull(value.value);
	}

	@Test
	public void testBoolean() throws JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		String source = buildProperty(5, "true");
		ThingPropertyDto value = mapper.readValue(source.getBytes(), ThingPropertyDto.class);
		Assert.assertNotNull(value.value);
		Assert.assertEquals(true,value.value);

		String source2 = buildProperty(5, "false");
		ThingPropertyDto value2 = mapper.readValue(source2.getBytes(), ThingPropertyDto.class);
		Assert.assertNotNull(value2.value);
		Assert.assertEquals(false,value2.value);
	}

	
	@Test
	public void testNonExistingField() throws JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		String source = String.format("{\"dataTypeId\":%d,\"time\":\"2017-02-25T14:57:42.658-05:00\"}", 11);
		ThingPropertyDto value = mapper.readValue(source.getBytes(), ThingPropertyDto.class);
		Assert.assertNull(value.value);
	}
	
	@Test
	public void testDataTypeNotPresent() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		String source = String.format("{\"value\":%s,\"time\":\"2017-02-25T14:57:42.658-05:00\"}", 
				"\"ABC\"");
		ThingPropertyDto value = mapper.readValue(source.getBytes(), ThingPropertyDto.class);
		Assert.assertEquals(value.value, "ABC");
	}

	private String buildProperty(int dataType, String value) {
		return String.format("{\"dataTypeId\":%d,\"value\":%s,\"time\":\"2017-02-25T14:57:42.658-05:00\"}", dataType,
				value);
	}
}
