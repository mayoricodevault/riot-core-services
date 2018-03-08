package com.tierconnect.riot.iot.reports.autoindex.entities.indexInformation;

import static org.junit.Assert.*;

import com.mongodb.BasicDBObject;

import com.tierconnect.riot.commons.DateFormatAndTimeZone;

import org.junit.Test;

import java.text.SimpleDateFormat;

import java.util.LinkedList;
import java.util.List;


/**
 * Created by achambi on 5/26/17.
 * Test for Index Stats.
 */
public class IndexInformationTest {
    private SimpleDateFormat formatter;
    private DateFormatAndTimeZone dateFormatAndTimeZone = new DateFormatAndTimeZone("Europe/London");

    @Test
    public void getTest() throws Exception {
        formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        IndexInformation indexInformation = new IndexInformation(
                BasicDBObject.parse("{\n" +
                        "    \"_id\" : \"indexNameIdTest\",\n" +
                        "    \"startDateOpCount\" : ISODate(\"2017-04-02T00:00:00.000Z\"),\n" +
                        "    \"numberQueriesDone\" : 200,\n" +
                        "    \"status\" : \"statsGenerated\",\n" +
                        "    \"collectionName\" : \"thingSnapshots\",\n" +
                        "    \"lastRunDate\" : ISODate(\"2017-04-02T00:00:00.000Z\"),\n" +
                        "    \"statsLog\" : [ \n" +
                        "        {\n" +
                        "            \"_id\" : \"indexNameIdTest\",\n" +
                        "            \"startDateOpCount\" : ISODate(\"2017-04-01T00:00:00.000Z\"),\n" +
                        "            \"numberQueriesDone\" : 50,\n" +
                        "            \"creationDate\" : ISODate(\"2017-04-01T01:00:00.000Z\"),\n" +
                        "            \"lastRunDate\" : ISODate(\"2017-04-02T00:00:00.000Z\"),\n" +
                        "            \"status\" : \"statsGenerated\"\n" +
                        "        }, \n" +
                        "        {\n" +
                        "            \"_id\" : \"indexNameIdTest\",\n" +
                        "            \"startDateOpCount\" : ISODate(\"2017-04-02T00:00:00.000Z\"),\n" +
                        "            \"numberQueriesDone\" : 50,\n" +
                        "            \"creationDate\" : ISODate(\"2017-04-01T02:00:00.000Z\"),\n" +
                        "            \"lastRunDate\" : ISODate(\"2017-04-02T00:00:00.000Z\"),\n" +
                        "            \"status\" : \"statsGenerated\"\n" +
                        "        }, \n" +
                        "        {\n" +
                        "            \"_id\" : \"indexNameIdTest\",\n" +
                        "            \"startDateOpCount\" : ISODate(\"2017-04-03T00:00:00.000Z\"),\n" +
                        "            \"numberQueriesDone\" : 50,\n" +
                        "            \"creationDate\" : ISODate(\"2017-04-01T03:00:00.000Z\"),\n" +
                        "            \"lastRunDate\" : ISODate(\"2017-04-02T00:00:00.000Z\"),\n" +
                        "            \"status\" : \"statsGenerated\"\n" +
                        "        }, \n" +
                        "        {\n" +
                        "            \"_id\" : \"indexNameIdTest\",\n" +
                        "            \"startDateOpCount\" : ISODate(\"2017-04-04T00:00:00.000Z\"),\n" +
                        "            \"creationDate\" : ISODate(\"2017-04-01T04:00:00.000Z\"),\n" +
                        "            \"numberQueriesDone\" : 50,\n" +
                        "            \"lastRunDate\" : ISODate(\"2017-04-02T00:00:00.000Z\"),\n" +
                        "            \"status\" : \"statsGenerated\"\n" +
                        "        }\n" +
                        "    ]\n" +
                        "}"),
                new DateFormatAndTimeZone("Europe/London"));
        assertEquals("indexNameIdTest", indexInformation.getId());
        assertEquals("2017-04-02T01:00+0100", indexInformation.getStartDateOpCountIsoFormat());
        assertEquals(formatter.parse("2017-04-02 00:00:00"), indexInformation.getStartDateOpCount());
        assertEquals(Long.valueOf(200), indexInformation.getNumberQueriesDone());
        assertEquals(IndexStatus.STATS_GENERATED, indexInformation.getStatus());
        assertEquals("thingSnapshots", indexInformation.getCollectionName());
        assertEquals(formatter.parse("2017-04-02 00:00:00"), indexInformation.getLastRunDate());

        for (int i = 0; i < indexInformation.getStatsLog().size(); i++) {
            assertEquals("indexNameIdTest", indexInformation.getStatsLog().get(i).getId());
            assertEquals(formatter.parse("2017-04-0" + (i + 1) + " 00:00:00"),
                    indexInformation.getStatsLog().get(i).getStartDateOpCount());
            Long count = 50L;
            assertEquals(count, indexInformation.getStatsLog().get(i).getNumberQueriesDone());
            assertEquals(formatter.parse("2017-04-01 0" + (i + 1) + ":00:00"),
                    indexInformation.getStatsLog().get(i).getCreationDate());
            assertNull(indexInformation.getStatsLog().get(i).getLastRunDate());
        }
    }

    @Test
    public void setTest() throws Exception {
        formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        IndexInformation indexInformation = new IndexInformation(dateFormatAndTimeZone);
        indexInformation.setId("indexNameIdTest");
        indexInformation.setStartDateOpCount(formatter.parse("2017-04-01 00:00:00"));
        indexInformation.setNumberQueriesDone(200L);
        indexInformation.setStatus(IndexStatus.STATS_GENERATED);
        indexInformation.setLastRunDate(formatter.parse("2017-04-01 00:00:00"));
        List<IndexInformation> indexInformationList = new LinkedList<>();

        for (int i = 1; i <= 4; i++) {
            IndexInformation indexInformationField = new IndexInformation(dateFormatAndTimeZone);
            indexInformationField.setId("indexNameIdTest");
            indexInformationField.setStartDateOpCount(formatter.parse("2017-04-0" + i + " 00:00:00"));
            indexInformationField.setCreationDate(formatter.parse("2017-04-0" + i + " 00:00:00"));
            indexInformationField.setNumberQueriesDone(50L);
            indexInformationList.add(indexInformationField);
        }
        indexInformation.setStatsLog(indexInformationList);
        assertEquals("{ \"_id\" : \"indexNameIdTest\" , \"startDateOpCount\" : { \"$date\" : " +
                        "\"2017-04-01T00:00:00.000Z\"} , \"numberQueriesDone\" : 200 , \"status\" : " +
                        "\"statsGenerated\" , " +
                        "\"lastRunDate\" : { \"$date\" : \"2017-04-01T00:00:00.000Z\"} , \"statsLog\" : [ { \"_id\" :" +
                        " " +
                        "\"indexNameIdTest\" , \"startDateOpCount\" : { \"$date\" : \"2017-04-01T00:00:00.000Z\"} , " +
                        "\"creationDate\" : { \"$date\" : \"2017-04-01T00:00:00.000Z\"} , \"numberQueriesDone\" : 50}" +
                        " , { " +
                        "\"_id\" : \"indexNameIdTest\" , \"startDateOpCount\" : { \"$date\" : " +
                        "\"2017-04-02T00:00:00.000Z\"} ," +
                        " \"creationDate\" : { \"$date\" : \"2017-04-02T00:00:00.000Z\"} , \"numberQueriesDone\" : " +
                        "50} , { " +
                        "\"_id\" : \"indexNameIdTest\" , \"startDateOpCount\" : { \"$date\" : " +
                        "\"2017-04-03T00:00:00.000Z\"} ," +
                        " \"creationDate\" : { \"$date\" : \"2017-04-03T00:00:00.000Z\"} , \"numberQueriesDone\" : " +
                        "50} , { " +
                        "\"_id\" : \"indexNameIdTest\" , \"startDateOpCount\" : { \"$date\" : " +
                        "\"2017-04-04T00:00:00.000Z\"} ," +
                        " \"creationDate\" : { \"$date\" : \"2017-04-04T00:00:00.000Z\"} , \"numberQueriesDone\" : 50}]}",
                indexInformation.toString());
    }
}