package com.tierconnect.riot.iot.services.thing;

import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.services.ThingService;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by julio.rocha on 31-07-17.
 */
public class ThingServiceInsertTest extends ThingServiceTest {
    private static Logger logger = Logger.getLogger(ThingServiceInsertTest.class);

    @Test
    public void createSerialNameCT001() throws Exception {
        compareEqualityResult("2017-04-27T08:00Z","create/001.json", "create/001_response.json");
    }

    @Test
    public void createSerialNameStandardUDFCT002() throws Exception {
        compareEqualityResult("2017-04-27T08:00Z","create/002.json", "create/002_response.json");
    }

    @Test
    public void createSerialNameStandardUDFDefaultValuesCT003() throws Exception {
        compareEqualityResult("2017-04-27T08:00Z","create/003.json", "create/003_response.json", Boolean.TRUE);
    }

    @Test
    public void createSerialNameStandardUDFDefaultValuesNativeUDFCT004() throws Exception {
        compareEqualityResult("2017-04-27T08:00Z","create/004.json", "create/004_response.json", Boolean.TRUE);
    }

    @Test
    public void createSerialNameStandardUDFDefaultValueNaviteUDFUDFAsFieldCT005() throws Exception {
        Map<String, Object> udfField = executeAndFormatExpectedResult(getDate("2017-04-27T07:30Z"),"create/005_attribute.json",
                "create/005_attribute_response.json", Boolean.TRUE);
        Map<String, Map<String, Object>> attributesToReplace = new LinkedHashMap<>();
        attributesToReplace.put("thingTypeAttribute", udfField);
        compareEqualityResult("2017-04-27T08:00Z","create/005.json", "create/005_response.json", Boolean.TRUE,
                attributesToReplace);
    }

    @Test
    public void createSerialNameStandardUDFDefaultValueNaviteUDFUDFAsFieldSequenceCT006() throws Exception {
        Map<String, Object> udfField = executeAndFormatExpectedResult(getDate("2017-04-27T07:30Z"),"create/006_attribute.json",
                "create/006_attribute_response.json", Boolean.TRUE);
        Map<String, Map<String, Object>> attributesToReplace = new LinkedHashMap<>();
        attributesToReplace.put("thingTypeAttribute", udfField);
        compareEqualityResult("2017-04-27T08:00Z","create/006.json", "create/006_response.json", Boolean.TRUE,
                attributesToReplace);
    }

    @Test
    public void createSerialNameStandardUDFDefaultValueNaviteUDFUDFAsFieldSequenceExpressionCT007() throws Exception {
        Map<String, Object> udfField = executeAndFormatExpectedResult(getDate("2017-04-27T07:30Z"), "create/007_attribute.json",
                "create/007_attribute_response.json", Boolean.TRUE);
        Map<String, Map<String, Object>> attributesToReplace = new LinkedHashMap<>();
        attributesToReplace.put("thingTypeAttribute", udfField);
        compareEqualityResult("2017-04-27T08:00Z","create/007.json", "create/007_response.json", Boolean.TRUE,
                attributesToReplace);
    }

    @Test
    @Ignore
    public void deleteAllCreatedThings() throws Exception {
        String[] serials = {"CT001", "CT002", "CT003", "CT004", "CT005", "CTA005", "CT006", "CTA006", "CT007", "CTA007"};
        List<Long> thingIds = new LinkedList<>();
        for (String s : serials) {
            Thing thing = ThingService.getInstance().getBySerialNumber(s);
            if (thing != null) {
                thingIds.add(thing.getId());
            }
        }
        ThingService.getInstance().secureDelete(thingIds, true, true, true, true, false);
        waitForThingDeletion();
    }
}
