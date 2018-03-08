package com.tierconnect.riot.commons.serdes;

import org.junit.Assert;
import org.junit.Test;

import com.tierconnect.riot.commons.dtos.ThingDto;


public class ThingDtoDeserializerTest {
	ThingDtoDeserializer des = new ThingDtoDeserializer();

	@Test
	public void readBridgeCode() {
		String source = "{\"meta\":{\"bridgeCode\":\"STAR\"},\"serialNumber\":\"\",\"time\":\"2017-02-25T10:06:15.457-04:00\",\"thingType\":{}}";
    	ThingDto thing = des.deserialize("", source.getBytes());
    	Assert.assertEquals("STAR", thing.meta.bridgeCode);
	}
}
