package com.tierconnect.riot.iot.services.thing;

import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.services.ThingService;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by ybarriga on 5/17/16.
 */
public class ThingServiceReadTest extends ThingServiceTest {

    private static Logger logger = Logger.getLogger(ThingServiceReadTest.class);

    @Test
    public void testIsAlphaNumeric() throws Exception {
        Assert.assertEquals(true, ThingService.getInstance().isAlphaNumeric("ABC12345abc"));
        Assert.assertEquals(true, ThingService.getInstance().isAlphaNumeric("12345abc"));
        Assert.assertEquals(true, ThingService.getInstance().isAlphaNumeric("11111"));
        Assert.assertEquals(true, ThingService.getInstance().isAlphaNumeric("abc123"));
        Assert.assertEquals(true, ThingService.getInstance().isAlphaNumeric("12345ABC"));
        Assert.assertEquals(false, ThingService.getInstance().isAlphaNumeric("#24%^77889=-="));
        Assert.assertEquals(false, ThingService.getInstance().isAlphaNumeric("ABC12345ab c"));
        Assert.assertEquals(false, ThingService.getInstance().isAlphaNumeric("ABC12345ab$c"));
        Assert.assertEquals(false, ThingService.getInstance().isAlphaNumeric("@!^ABC12345ab$c"));
        Assert.assertEquals(false, ThingService.getInstance().isAlphaNumeric(""));
        Assert.assertEquals(false, ThingService.getInstance().isAlphaNumeric(" "));
        Assert.assertEquals(true, ThingService.getInstance().isAlphaNumeric("1234565"));
        Assert.assertEquals(true, ThingService.getInstance().isAlphaNumeric("abcdef"));
        Assert.assertEquals(false, ThingService.getInstance().isAlphaNumeric("a;b;c"));
        Assert.assertEquals(false, ThingService.getInstance().isAlphaNumeric("ABC_"));
        Assert.assertEquals(false, ThingService.getInstance().isAlphaNumeric("abc*"));
        Assert.assertEquals(false, ThingService.getInstance().isAlphaNumeric("12345/"));
    }

    @Test
    public void testProcessListThings() {
        ThingService thingService = ThingService.getInstance();
        Integer pageSize = 1;
        Integer pageNumber = 1;
        String order = "name:asc";
        String where = "thingTypeId=6";
        String extra = null;
        String only = "_id,name,thingTypeName,serialNumber,groupName,groupTypeName,thingTypeId";
        String groupBy = null;
        Long visibilityGroupId = null;
        String upVisibility = "false";
        String downVisibility = "";
        boolean treeView = false;

        Map<String, Object> map = thingService.processListThings(
                pageSize, pageNumber, order, where, extra, only,
                groupBy, visibilityGroupId, upVisibility,
                downVisibility, treeView, userRoot, false);
        Assert.assertNotNull(map);
        Assert.assertFalse(map.isEmpty());
        logger.info(map);
    }

    @Test
    public void getDifferenceBetweenListsTest() {
        Thing thing1 = new Thing();
        Thing thing2 = new Thing();
        Thing thing3 = new Thing();
        Thing thing4 = new Thing();

        thing1.setId(1L);
        thing2.setId(2L);
        thing3.setId(3L);
        thing4.setId(4L);

        Thing thing5 = new Thing();
        Thing thing6 = new Thing();
        Thing thing7 = new Thing();
        thing5.setId(2L);
        thing6.setId(3L);
        thing7.setId(5L);

        List<Thing> ini = new ArrayList<>();
        ini.add(thing1);
        ini.add(thing2);
        ini.add(thing3);
        ini.add(thing4);

        List<Thing> last = new ArrayList<>();
        last.add(thing5);
        last.add(thing6);
        last.add(thing7);

        List<Long> differenceBetweenLists = ThingService.getInstance().getDifferenceBetweenLists(ini, last);
        logger.info(differenceBetweenLists);
        List<Long> expectedList = Arrays.asList(1L, 4L);
        Assert.assertTrue(differenceBetweenLists.containsAll(expectedList));

        List<Thing> betweenListsAB = ThingService.getInstance().getDifferenceBetweenListsA_B(ini, last);
        for (Thing thing : betweenListsAB) {
            logger.info(thing.getId());
            Assert.assertTrue(expectedList.contains(thing.getId()));
        }
    }
}
