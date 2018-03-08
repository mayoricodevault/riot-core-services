package com.tierconnect.riot.iot.utils;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by achambi on 9/1/17.
 */
public class MatrixUtilsTest {

    @Test
    public void getFirstElements() throws Exception {
        List<LinkedList<Long>> linkedLists = new ArrayList<>();
        LinkedList<Long> thingTypeIds = new LinkedList<>();
        thingTypeIds.add(5L);
        thingTypeIds.add(3L);
        LinkedList<Long> thingTypeIdsSecondPath = new LinkedList<>();
        thingTypeIdsSecondPath.add(5L);
        thingTypeIdsSecondPath.add(14L);
        linkedLists.add(thingTypeIds);
        linkedLists.add(thingTypeIdsSecondPath);
        Set<Long> response = MatrixUtils.getFirstElements(linkedLists);
        assertEquals("[5]", response.toString());
    }
}