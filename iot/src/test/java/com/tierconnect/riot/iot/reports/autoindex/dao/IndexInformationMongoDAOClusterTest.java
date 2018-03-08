package com.tierconnect.riot.iot.reports.autoindex.dao;


import com.tierconnect.riot.appcore.core.BaseTestIOT;

import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.reports.autoindex.entities.indexInformation.IndexInformation;

import com.tierconnect.riot.iot.utils.PropReaderUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Created by achambi on 5/29/17.
 * Test unit from index stats thing DAO.
 */
public class IndexInformationMongoDAOClusterTest extends BaseTestIOT {

    @Before
    public void setUp() throws Exception {
        Properties properties = PropReaderUtil.getCustomConFile("confClusterTest.properties");
        Configuration.setConfigurationFilePath(properties);
    }

    @Test
    public void testTreeSet() throws Exception {
        class TestClass {

            private String stringName;
            public Date date;

            private TestClass(String stringName, Date date) {
                this.stringName = stringName;
                this.date = date;
            }
        }
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        List<TestClass> classList = new LinkedList<>();
        classList.add(new TestClass("A", dateFormat.parse("2017-01-01")));
        classList.add(new TestClass("A", dateFormat.parse("2017-01-02")));
        classList.add(new TestClass("A", dateFormat.parse("2017-01-02")));
        classList.add(new TestClass("A", dateFormat.parse("2017-01-03")));
        classList.add(new TestClass("A", dateFormat.parse("2017-01-03")));

        classList.add(new TestClass("B", dateFormat.parse("2017-01-06")));
        classList.add(new TestClass("B", dateFormat.parse("2017-01-04")));
        classList.add(new TestClass("B", dateFormat.parse("2017-01-01")));
        classList.add(new TestClass("B", dateFormat.parse("2017-01-02")));
        classList.add(new TestClass("B", dateFormat.parse("2017-01-04")));
        classList.sort((o1, o2) -> {
            int sComp = o1.stringName.compareTo(o2.stringName);
            if (sComp != 0) {
                return sComp;
            } else {
                return o1.date.compareTo(o2.date);
            }
        });
        assertNotNull(classList);
        Set<TestClass> testClassSet = new TreeSet<>((o1, o2) -> {
            int compareTo = o1.stringName.compareTo(o2.stringName);
            if (compareTo != 0) {
                return compareTo;
            }
            compareTo = o1.date.compareTo(o2.date);
            if (compareTo > 0) {
                return 0;
            }
            return compareTo;

        });
        testClassSet.addAll(classList);
        assertEquals(2, testClassSet.size());
        for (TestClass item : testClassSet) {
            assertEquals(dateFormat.parse("2017-01-01"), item.date);
            assertTrue(item.stringName.equals("A") || item.stringName.equals("B"));
        }
    }

    @Test
    public void testCollection() throws Exception {
        class Item {

            public String getName() {
                return name;
            }


            public int getNumber() {
                return number;
            }

            private String name;
            private int number;

            private Item(String name, int number) {
                this.name = name;
                this.number = number;
            }

        }

        List<Item> items = Arrays.asList(
                new Item("apple", 10),
                new Item("banana", 20),
                new Item("orang", 10),
                new Item("watermelon", 10),
                new Item("papaya", 20),
                new Item("apple", 10),
                new Item("banana", 10),
                new Item("apple", 20)
        );

        Map<String, Long> counting = items.stream().collect(
                Collectors.groupingBy(Item::getName, Collectors.counting()));

        System.out.println(counting);

        Map<String, Integer> sum = items.stream().collect(Collectors.groupingBy(Item::getName, Collectors.summingInt(Item::getNumber)));
        System.out.println(sum);
    }

    @Test
    public void getClusterIndexInformation() throws Exception {
        String secondaryList = Configuration.getProperty("mongo.secondary");
        List<IndexInformation> result = IndexInformationMongoDAO.getInstance().getClusterIndexInformation("things", secondaryList, "_id_", "name_1_");
        assertNotNull(result);
    }

    @After
    public void tearDown() throws Exception {

    }
}
