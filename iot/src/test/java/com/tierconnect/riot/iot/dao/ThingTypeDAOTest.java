package com.tierconnect.riot.iot.dao;

import com.tierconnect.riot.appcore.core.BaseTestIOT;
import com.tierconnect.riot.iot.entities.ThingType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by achambi on 7/27/17.
 * Unit test for thing type dao.
 */
public class ThingTypeDAOTest extends BaseTestIOT {

    @Test
    public void getAllParents() throws Exception {
        ThingTypeDAO thingTypeDAO = new ThingTypeDAO();
        Long[] allParents = thingTypeDAO.getAllParents();
        assertNotNull(allParents);
    }

    @Test
    public void getThingTypeIn() throws Exception {
        ThingTypeDAO thingTypeDAO = new ThingTypeDAO();
        List<Long> thingTypeIds = new ArrayList<>();
        thingTypeIds.add(3L);
        thingTypeIds.add(5L);
        thingTypeIds.add(7L);
        List<ThingType> thingTypes = thingTypeDAO.getThingTypeIn(thingTypeIds);
        assertNotNull(thingTypes);
        assertEquals(3, thingTypes.size());
        assertEquals("default_rfid_thingtype", thingTypes.get(0).getCode());
        assertEquals("jackets_code", thingTypes.get(1).getCode());
        assertEquals("pants_code", thingTypes.get(2).getCode());
    }

    @Test
    public void getThingTypeInNullValue() throws Exception {
        ThingTypeDAO thingTypeDAO = new ThingTypeDAO();
        List<ThingType> thingTypes = thingTypeDAO.getThingTypeIn(null);
        assertNull(thingTypes);
    }
}