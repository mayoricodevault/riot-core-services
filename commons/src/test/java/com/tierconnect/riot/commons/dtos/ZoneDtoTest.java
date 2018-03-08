package com.tierconnect.riot.commons.dtos;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * DataTypeDtoTest class.
 *
 * @author jantezana
 * @author achambi
 * @version 2017/11/05
 */
public class ZoneDtoTest {
    @Test
    public void equals() throws Exception {
        ZoneDto x = new ZoneDto();
        ZoneDto y = new ZoneDto();
        assertTrue(x.equals(y));

        x.facilityMap = new FacilityMapDto();
        x.facilityMap.code = "EQUALS";
        x.facilityMap.blinked = true;
        x.facilityMap.modified = true;
        x.facilityMap.time = new Date();
        y.facilityMap = new FacilityMapDto();
        y.facilityMap.code = "EQUALS";
        y.facilityMap.blinked = false;
        y.facilityMap.blinked = false;
        y.facilityMap.time = new Date();
        assertTrue(x.equals(y));

        x.facilityMap.name = "EQUALS";
        y.facilityMap.name = "NOT_EQUALS";
        assertTrue(x.equals(y));

        x.facilityMap.name = "EQUALS";
        y.facilityMap.name = "EQUALS";
        x.zonePoints = new ArrayList<>();
        x.zonePoints.add(new double[]{1d,2d,3d});
        y.zonePoints = new ArrayList<>();
        y.zonePoints.add(new double[]{1d,2d,3d});
        assertTrue(x.equals(y));

        x.zonePoints.add(new double[]{1d,2d,3d});
        y.zonePoints.add(new double[]{1d,2d,4d});
        assertFalse(x.equals(y));

        x.zonePoints.remove(1);
        y.zonePoints.remove(1);
        x.zoneGroup = new ZonePropertyDto();
        x.zoneGroup.code = "EQUALS";
        x.zoneGroup.blinked = true;
        x.zoneGroup.modified = true;
        x.zoneGroup.time = new Date();
        y.zoneGroup = new ZonePropertyDto();
        y.zoneGroup.code = "EQUALS";
        y.zoneGroup.blinked = false;
        y.zoneGroup.modified = false;
        y.zoneGroup.time = new Date();
        assertTrue(x.equals(y));

    }

    @Test
    public void testInstanceZone()
    throws Exception {
        ZoneDto zoneDto = new ZoneDto();
        assertNotEquals(null, zoneDto);
    }
}