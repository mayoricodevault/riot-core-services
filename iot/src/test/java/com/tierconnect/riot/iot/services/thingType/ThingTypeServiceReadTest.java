package com.tierconnect.riot.iot.services.thingType;

import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.iot.utils.DirectedGraph;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

/**
 * Created by vealaro on 8/31/17.
 */
public class ThingTypeServiceReadTest extends ThingTypeServiceTest {

    @Test
    public void getListThingTypeDirection() throws Exception {
        List<Long> groupList = new ArrayList<>();
        groupList.add(3L);
        DirectedGraph thingTypeGraph = new ThingTypeService().getThingTypeGraphByGroupId(groupList);
        assertNotNull(thingTypeGraph);
    }


}
