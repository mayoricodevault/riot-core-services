package com.tierconnect.riot.iot.services;

import org.jose4j.json.internal.json_simple.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by ruth on 30-05-17.
 */
public class DevicesServiceTest {

    @Test
    public void getConfigurationOfBridgeType() throws Exception {
        JSONObject result = DevicesService.getInstance().getConfigurationOfBridgeType("{\"configuration\":{\"mongo\":{\"type\":\"JSON\",\"value\":{\"connectionCode\":{\"type\":\"String\",\"value\":\"\",\"required\":false}},\"required\":false},\"thingTypeCodeConfig\":{\"type\":\"String\",\"value\":\"STR_400\",\"required\":false},\"thingTypeCode\":{\"type\":\"String\",\"value\":\"flextag_code\",\"required\":false},\"thingTypeCodeStatus\":{\"type\":\"String\",\"value\":\"starflex_status_code\",\"required\":false},\"mqtt\":{\"type\":\"JSON\",\"value\":{\"connectionCode\":{\"type\":\"String\",\"value\":\"\",\"required\":false}},\"required\":false},\"numberOfThreads\":{\"type\":\"Number\",\"value\":1,\"required\":false},\"lastDetectFilterTypes\":{\"type\":\"String\",\"value\":\"\",\"required\":false},\"logRawMessages\":{\"type\":\"Boolean\",\"value\":false,\"required\":false}},\"extra\":{\"playStarFlexSync\":{\"type\":\"JSON\",\"value\":{\"active\":{\"type\":\"Boolean\",\"value\":false,\"required\":false},\"backupFolder\":{\"type\":\"String\",\"value\":\"\",\"required\":false}},\"required\":false}},\"filters\":{\"zoneDwellFilter\":{\"type\":\"JSON\",\"value\":{\"lastDetectTimeActive\":{\"type\":\"Boolean\",\"value\":true,\"required\":false},\"active\":{\"type\":\"Boolean\",\"value\":true,\"required\":false},\"zoneDwellTime\":{\"type\":\"Number\",\"value\":10,\"required\":false},\"inZoneDistance\":{\"type\":\"Number\",\"value\":0,\"required\":false},\"lastDetectTimeWindow\":{\"type\":\"Number\",\"value\":20,\"required\":false},\"unlockDistance\":{\"type\":\"Number\",\"value\":0,\"required\":false}},\"required\":false},\"rateFilter\":{\"type\":\"JSON\",\"value\":{\"timeLimit\":{\"type\":\"Number\",\"value\":20,\"required\":false},\"active\":{\"type\":\"Boolean\",\"value\":false,\"required\":false}},\"required\":false}}}");
        assertNotNull(result);
        assertTrue(result.toString().equals("{\"mongo\":{\"connectionCode\":\"\"},\"zoneDwellFilter\":{\"active\":true,\"zoneDwellTime\":10,\"inZoneDistance\":0,\"lastDetectTimeWindow\":20,\"lastDetectTimeActive\":true,\"unlockDistance\":0},\"thingTypeCodeConfig\":\"STR_400\",\"thingTypeCode\":\"flextag_code\",\"thingTypeCodeStatus\":\"starflex_status_code\",\"rateFilter\":{\"timeLimit\":20,\"active\":false},\"playStarFlexSync\":{\"active\":false,\"backupFolder\":\"\"},\"mqtt\":{\"connectionCode\":\"\"},\"numberOfThreads\":1,\"lastDetectFilterTypes\":\"\",\"logRawMessages\":false}"));
        System.out.println(result.toString());
    }
}