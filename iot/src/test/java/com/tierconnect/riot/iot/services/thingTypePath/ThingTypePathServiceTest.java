package com.tierconnect.riot.iot.services.thingTypePath;

import com.tierconnect.riot.appcore.core.BaseTestIOT;

import com.tierconnect.riot.iot.entities.ThingTypePath;
import com.tierconnect.riot.iot.entities.ThingTypePathBase;

import com.tierconnect.riot.iot.services.ThingTypePathService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Created by achambi on 7/21/17.
 * Test to get path method.
 */
public class ThingTypePathServiceTest extends BaseTestIOT {

    private List<Long> thingsVisibility;

    @Before
    public void setUp() throws Exception {
        thingsVisibility = new ArrayList<>();
        thingsVisibility.add(3L);

    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Method for verify asset, with thingTypeUdf: SO is Parent = true and tag native association.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNoChildrenCase() throws Exception {
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService
                .getInstance()
                .getPathByThingTypeId(1L, thingsVisibility);
        assertEquals(0L, pathByThingTypeNode.size());
        pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(2L,
                thingsVisibility);
        assertEquals(0L, pathByThingTypeNode.size());
        pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(4L,
                thingsVisibility);
        assertEquals(0L, pathByThingTypeNode.size());
        pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(12L,
                thingsVisibility);
        assertEquals(0L, pathByThingTypeNode.size());
    }

    /**
     * Method for verify asset, with thingTypeUdf: SO is Parent = true and tag native association.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode3_1() throws Exception {
        //AssetTest
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(20L,
                thingsVisibility);
        assertEquals(3, pathByThingTypeNode.size());

        assertEquals("SalesOrderTest", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("AssetTest", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("AssetTest_children", pathByThingTypeNode.get(0).getPath());

        assertEquals("SalesOrderTest", pathByThingTypeNode.get(1).getOriginThingType().getCode());
        assertEquals("TagTest", pathByThingTypeNode.get(1).getDestinyThingType().getCode());
        assertEquals("AssetTest_children.children", pathByThingTypeNode.get(1).getPath());

        assertEquals("AssetTest", pathByThingTypeNode.get(2).getOriginThingType().getCode());
        assertEquals("TagTest", pathByThingTypeNode.get(2).getDestinyThingType().getCode());
        assertEquals("children", pathByThingTypeNode.get(2).getPath());
    }

    /**
     * Method for verify asset, with thingTypeUdf: SO is Parent = true and tag native association.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode3_2() throws Exception {
        //SalesOrderTest
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(18L,
                thingsVisibility);
        assertEquals(3, pathByThingTypeNode.size());

        assertEquals("SalesOrderTest", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("AssetTest", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("AssetTest_children", pathByThingTypeNode.get(0).getPath());

        assertEquals("SalesOrderTest", pathByThingTypeNode.get(1).getOriginThingType().getCode());
        assertEquals("TagTest", pathByThingTypeNode.get(1).getDestinyThingType().getCode());
        assertEquals("AssetTest_children.children", pathByThingTypeNode.get(1).getPath());

        assertEquals("AssetTest", pathByThingTypeNode.get(2).getOriginThingType().getCode());
        assertEquals("TagTest", pathByThingTypeNode.get(2).getDestinyThingType().getCode());
        assertEquals("children", pathByThingTypeNode.get(2).getPath());
    }

    /**
     * Method for verify asset, with thingTypeUdf: SO is Parent = true and tag native association.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode3_3() throws Exception {
        //TagTest
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(19L,
                thingsVisibility);
        assertEquals(3, pathByThingTypeNode.size());

        assertEquals("SalesOrderTest", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("AssetTest", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("AssetTest_children", pathByThingTypeNode.get(0).getPath());

        assertEquals("SalesOrderTest", pathByThingTypeNode.get(1).getOriginThingType().getCode());
        assertEquals("TagTest", pathByThingTypeNode.get(1).getDestinyThingType().getCode());
        assertEquals("AssetTest_children.children", pathByThingTypeNode.get(1).getPath());

        assertEquals("AssetTest", pathByThingTypeNode.get(2).getOriginThingType().getCode());
        assertEquals("TagTest", pathByThingTypeNode.get(2).getDestinyThingType().getCode());
        assertEquals("children", pathByThingTypeNode.get(2).getPath());
    }

    /**
     * Method for verify asset, with thingTypeUdf: Asset and 2 native association RFID and GPS.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode4_1() throws Exception {
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(21L,
                thingsVisibility);
        assertEquals(1, pathByThingTypeNode.size());

        assertEquals("Asset2ChildrenTest", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("RFIDTest", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("children", pathByThingTypeNode.get(0).getPath());
    }

    /**
     * Method for verify asset, with thingTypeUdf: Asset and 2 native association RFID and GPS.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode4_2() throws Exception {
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(22L,
                thingsVisibility);
        assertEquals(1, pathByThingTypeNode.size());

        assertEquals("Asset2ChildrenTest", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("GPSTest", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("children", pathByThingTypeNode.get(0).getPath());
    }

    /**
     * Method for verify asset, with thingTypeUdf: Asset and 2 native association RFID and GPS.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode4_3() throws Exception {
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(23L,
                thingsVisibility);
        assertEquals(2, pathByThingTypeNode.size());

        assertEquals("Asset2ChildrenTest", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("RFIDTest", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("children", pathByThingTypeNode.get(0).getPath());

        assertEquals("Asset2ChildrenTest", pathByThingTypeNode.get(1).getOriginThingType().getCode());
        assertEquals("GPSTest", pathByThingTypeNode.get(1).getDestinyThingType().getCode());
        assertEquals("children", pathByThingTypeNode.get(1).getPath());
    }

    /**
     * Method for verify asset, with thingTypeUdf: Asset and 2 Thing types UDF association RFID and GPS.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode5_1() throws Exception {
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(24L,
                thingsVisibility);
        assertEquals(2, pathByThingTypeNode.size());

        assertEquals("AssetIsPTrue2Children", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("RfidChildren1Test", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("RfidChildren1Test_children", pathByThingTypeNode.get(0).getPath());

        assertEquals("AssetIsPTrue2Children", pathByThingTypeNode.get(1).getOriginThingType().getCode());
        assertEquals("GPSChildren1Test", pathByThingTypeNode.get(1).getDestinyThingType().getCode());
        assertEquals("GPSChildren1Test_children", pathByThingTypeNode.get(1).getPath());
    }

    /**
     * Method for verify asset, with thingTypeUdf: Asset and 2 Thing types UDF association RFID and GPS.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode5_2() throws Exception {
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(25L,
                thingsVisibility);
        assertEquals(1, pathByThingTypeNode.size());

        assertEquals("AssetIsPTrue2Children", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("RfidChildren1Test", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("RfidChildren1Test_children", pathByThingTypeNode.get(0).getPath());
    }

    /**
     * Method for verify asset, with thingTypeUdf: Asset and 2 Thing types UDF association RFID and GPS.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode5_3() throws Exception {
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(26L,
                thingsVisibility);
        assertEquals(1, pathByThingTypeNode.size());

        assertEquals("AssetIsPTrue2Children", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("GPSChildren1Test", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("GPSChildren1Test_children", pathByThingTypeNode.get(0).getPath());
    }

    /**
     * Method for verify asset, with thingTypeUdf: Asset and 2 Thing types UDF association RFID and GPS.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode6_1() throws Exception {
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(27L,
                thingsVisibility);
        assertEquals(1, pathByThingTypeNode.size());

        assertEquals("Father", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("childSingle", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("childfieldTest.value", pathByThingTypeNode.get(0).getPath());
    }

    /**
     * Method for verify asset, with thingTypeUdf: Asset and 2 Thing types UDF association RFID and GPS.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode6_2() throws Exception {
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(28L,
                thingsVisibility);
        assertEquals(1, pathByThingTypeNode.size());

        assertEquals("Father", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("childSingle", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("childfieldTest.value", pathByThingTypeNode.get(0).getPath());
    }

    /**
     * Method for verify asset, with thingTypeUdf: Asset and 2 Thing types UDF association RFID and GPS.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode7_1() throws Exception {
        //Asset
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(30L,
                thingsVisibility);
        assertEquals(2, pathByThingTypeNode.size());

        assertEquals("Case7Asset", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("case7Tag", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("children", pathByThingTypeNode.get(0).getPath());

        assertEquals("Case7Asset", pathByThingTypeNode.get(1).getOriginThingType().getCode());
        assertEquals("Case7Size", pathByThingTypeNode.get(1).getDestinyThingType().getCode());
        assertEquals("sizeField.value", pathByThingTypeNode.get(1).getPath());
    }

    /**
     * Method for verify asset, with thingTypeUdf: Asset and 2 Thing types UDF association RFID and GPS.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode7_2() throws Exception {
        //size
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(29L,
                thingsVisibility);
        assertEquals(1, pathByThingTypeNode.size());

        assertEquals("Case7Asset", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("Case7Size", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("sizeField.value", pathByThingTypeNode.get(0).getPath());
    }

    /**
     * Method for verify asset, with thingTypeUdf: Asset and 2 Thing types UDF association RFID and GPS.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode7_3() throws Exception {
        //size
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(31L,
                thingsVisibility);
        assertEquals(1, pathByThingTypeNode.size());

        assertEquals("Case7Asset", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("case7Tag", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("children", pathByThingTypeNode.get(0).getPath());
    }

    /**
     * Method for verify asset, with thingTypeUdf: SO, Asset and  Thing types UDF association Size and native parent tag.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode8_1() throws Exception {
        //Size
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(33L,
                thingsVisibility);
        assertEquals(3, pathByThingTypeNode.size());

        assertEquals("Case8SO", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("case8Asset", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("case8Asset_children", pathByThingTypeNode.get(0).getPath());

        assertEquals("Case8SO", pathByThingTypeNode.get(1).getOriginThingType().getCode());
        assertEquals("case8Size", pathByThingTypeNode.get(1).getDestinyThingType().getCode());
        assertEquals("case8Asset_children.case8Size.value", pathByThingTypeNode.get(1).getPath());

        assertEquals("case8Asset", pathByThingTypeNode.get(2).getOriginThingType().getCode());
        assertEquals("case8Size", pathByThingTypeNode.get(2).getDestinyThingType().getCode());
        assertEquals("case8Size.value", pathByThingTypeNode.get(2).getPath());
    }

    /**
     * Method for verify asset, with thingTypeUdf: SO, Asset and  Thing types UDF association Size and native parent tag.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode8_2() throws Exception {
        //Tag
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(34L,
                thingsVisibility);
        assertEquals(3, pathByThingTypeNode.size());

        assertEquals("Case8SO", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("case8Asset", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("case8Asset_children", pathByThingTypeNode.get(0).getPath());

        assertEquals("Case8SO", pathByThingTypeNode.get(1).getOriginThingType().getCode());
        assertEquals("Case8Tag", pathByThingTypeNode.get(1).getDestinyThingType().getCode());
        assertEquals("case8Asset_children.children", pathByThingTypeNode.get(1).getPath());

        assertEquals("case8Asset", pathByThingTypeNode.get(2).getOriginThingType().getCode());
        assertEquals("Case8Tag", pathByThingTypeNode.get(2).getDestinyThingType().getCode());
        assertEquals("children", pathByThingTypeNode.get(2).getPath());
    }

    /**
     * Method for verify asset, with thingTypeUdf: SO, Asset and  Thing types UDF association Size and native parent tag.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode8_3() throws Exception {
        //SO
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(35L,
                thingsVisibility);
        assertEquals(5, pathByThingTypeNode.size());

        assertEquals("Case8SO", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("case8Asset", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("case8Asset_children", pathByThingTypeNode.get(0).getPath());

        assertEquals("Case8SO", pathByThingTypeNode.get(1).getOriginThingType().getCode());
        assertEquals("Case8Tag", pathByThingTypeNode.get(1).getDestinyThingType().getCode());
        assertEquals("case8Asset_children.children", pathByThingTypeNode.get(1).getPath());

        assertEquals("case8Asset", pathByThingTypeNode.get(2).getOriginThingType().getCode());
        assertEquals("Case8Tag", pathByThingTypeNode.get(2).getDestinyThingType().getCode());
        assertEquals("children", pathByThingTypeNode.get(2).getPath());

        assertEquals("Case8SO", pathByThingTypeNode.get(3).getOriginThingType().getCode());
        assertEquals("case8Size", pathByThingTypeNode.get(3).getDestinyThingType().getCode());
        assertEquals("case8Asset_children.case8Size.value", pathByThingTypeNode.get(3).getPath());

        assertEquals("case8Asset", pathByThingTypeNode.get(4).getOriginThingType().getCode());
        assertEquals("case8Size", pathByThingTypeNode.get(4).getDestinyThingType().getCode());
        assertEquals("case8Size.value", pathByThingTypeNode.get(4).getPath());
    }

    /**
     * Method for verify asset, with thingTypeUdf: SO, Asset and  Thing types UDF association Size and native parent tag.
     *
     * @throws Exception any error
     */
    @Test
    public void getPathByThingTypeNode8_4() throws Exception {
        //Asset
        List<ThingTypePath> pathByThingTypeNode = ThingTypePathService.getInstance().getPathByThingTypeId(36L,
                thingsVisibility);
        assertEquals(5, pathByThingTypeNode.size());

        assertEquals("Case8SO", pathByThingTypeNode.get(0).getOriginThingType().getCode());
        assertEquals("case8Asset", pathByThingTypeNode.get(0).getDestinyThingType().getCode());
        assertEquals("case8Asset_children", pathByThingTypeNode.get(0).getPath());

        assertEquals("Case8SO", pathByThingTypeNode.get(1).getOriginThingType().getCode());
        assertEquals("Case8Tag", pathByThingTypeNode.get(1).getDestinyThingType().getCode());
        assertEquals("case8Asset_children.children", pathByThingTypeNode.get(1).getPath());

        assertEquals("case8Asset", pathByThingTypeNode.get(2).getOriginThingType().getCode());
        assertEquals("Case8Tag", pathByThingTypeNode.get(2).getDestinyThingType().getCode());
        assertEquals("children", pathByThingTypeNode.get(2).getPath());

        assertEquals("Case8SO", pathByThingTypeNode.get(3).getOriginThingType().getCode());
        assertEquals("case8Size", pathByThingTypeNode.get(3).getDestinyThingType().getCode());
        assertEquals("case8Asset_children.case8Size.value", pathByThingTypeNode.get(3).getPath());

        assertEquals("case8Asset", pathByThingTypeNode.get(4).getOriginThingType().getCode());
        assertEquals("case8Size", pathByThingTypeNode.get(4).getDestinyThingType().getCode());
        assertEquals("case8Size.value", pathByThingTypeNode.get(4).getPath());
    }

    @Test
    public void getAllPaths() throws Exception {
        List<ThingTypePath> allPaths = ThingTypePathService.getInstance().getPathByThingTypeId(null,
                thingsVisibility);
        assertNotNull(allPaths);
        List<String> allPathsList = allPaths.stream()
                .map(ThingTypePathBase::getPath)
                .collect(Collectors.toList());
        assertEquals("[children, " +
                "children, " +
                "asset_code_children, " +
                "asset_code_children.children, " +
                "children, " +
                "children, " +
                "ChildUDF_children, " +
                "AssetTest_children, " +
                "AssetTest_children.children, " +
                "children, " +
                "children, " +
                "children, " +
                "RfidChildren1Test_children, " +
                "GPSChildren1Test_children, " +
                "childfieldTest.value, " +
                "children, " +
                "sizeField.value, " +
                "case8Asset_children, " +
                "case8Asset_children.children, " +
                "children, " +
                "case8Asset_children.case8Size.value, " +
                "case8Size.value, " +
                "children, " +
                "sizeField.value, " +
                "children, " +
                "case10Asset_children, " +
                "case10Asset_children.SIZE.value, " +
                "SIZE.value]", allPathsList.toString());
    }
}