package com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs;


import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.sdk.utils.HashUtils;

import org.bson.types.ObjectId;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;

import java.util.LinkedList;

import static com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogStatus.PENDING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by achambi on 4/5/17.
 * Test ORM Mongo DAO.
 */
public class ReportLogInfoTest {

    private ReportLogInfo reportLogInfo;
    private SimpleDateFormat formatter;
    private LinkedList<ReportLogDetail> runs;

    @Before
    public void setUp() throws Exception {
        formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        reportLogInfo = new ReportLogInfo();
        String testId = HashUtils.hashSHA256("{\"$and\":[{\"$or\":[{\"children\":{\"$exists\":false}}," +
                "{\"children\":null}," +
                "{\"children\":{\"$size\":0}}]},{\"parent\":{\"$exists\":false}},{\"thingTypeId\":1}," +
                "{\"groupId\":{\"$in\":[1,2,3,4,5,6,7]}},{\"thingTypeId\":{\"$in\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14," +
                "15]}}]}");
        assertNotNull("The id field could not be null.", testId);
        reportLogInfo.setId("00002-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2");
        reportLogInfo.setType("Table Detail");
        reportLogInfo.setName("All RFID Tags");
        reportLogInfo.setLastRunDate(formatter.parse("2017-04-05 10:27:10"));
        reportLogInfo.setMaxDuration(72L);
        reportLogInfo.setMaxDurationDate(formatter.parse("2017-04-05 10:27:10"));
        reportLogInfo.setTotalRuns(1L);
        reportLogInfo.setQuery("{\"$and\":[{\"$or\":[{\"children\":{\"$exists\":false}},{\"children\":null}," +
                "{\"children\":{\"$size\":0}}]},{\"parent\":{\"$exists\":false}},{\"thingTypeId\":1}," +
                "{\"groupId\":{\"$in\":[1,2,3,4,5,6,7]}},{\"thingTypeId\":{\"$in\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14," +
                "15]}}]}");
        reportLogInfo.setStatus(PENDING);
        reportLogInfo.setMaxDurationId(new ObjectId("58e4febe5d0de2038f323183"));
        BasicDBList basicDBListRuns = new BasicDBList();
        basicDBListRuns.add(BasicDBObject.parse("  {\n" +
                "            \"start\" : NumberLong(1491402430619),\n" +
                "            \"end\" : NumberLong(1491402430691),\n" +
                "            \"duration\" : NumberLong(72),\n" +
                "            \"query\" : \"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2," +
                "3,4,5,6,7]}},{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                "            \"count\" : NumberLong(5),\n" +
                "            \"id\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                "            \"userID\" : NumberLong(1),\n" +
                "            \"date\" : ISODate(\"2017-04-05\"),\n" +
                "            \"additionalInfo\" : {\n" +
                "                \"header\" : {\n" +
                "                    \"origin\" : \"http://0.0.0.0:9000\",\n" +
                "                    \"host\" : \"127.0.0.1:8081\",\n" +
                "                    \"utcoffset\" : \"-240\",\n" +
                "                    \"user-agent\" : \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, " +
                "like Gecko) Chrome/57.0.2987.110 Safari/537.36\",\n" +
                "                    \"token\" : " +
                "\"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\"\n" +
                "                },\n" +
                "                \"queryString\" : \"pageNumber=1&pageSize=15&ts=1491402430486\",\n" +
                "                \"body\" : {\n" +
                "                    \"Thing Type\" : \"1\",\n" +
                "                    \"sortProperty\" : \"ASC\",\n" +
                "                    \"orderByColumn\" : NumberLong(1)\n" +
                "                }\n" +
                "            }\n" +
                "        }"));
        runs = new LinkedList<>();
        for (Object item : basicDBListRuns) {
            runs.add(new ReportLogDetail((BasicDBObject) item));
        }
        reportLogInfo.setRuns(runs);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void TestReportLogSets() throws Exception {

        assertEquals("ERROR: ReportLogInfo parse ERROR!", "{ \"_id\" : " +
                "\"00002-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\", \"type\" : \"Table " +
                "Detail\", \"name\" : \"All RFID Tags\", \"lastRunDate\" : { \"$date\" : 1491388030000 }, " +
                "\"maxDuration\" : { \"$numberLong\" : \"72\" }, \"maxDurationDate\" : { \"$date\" : 1491388030000 }," +
                " \"totalRuns\" : { \"$numberLong\" : \"1\" }, \"query\" : " +
                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}},{\\\"children\\\":null}," +
                "{\\\"children\\\":{\\\"$size\\\":0}}]},{\\\"parent\\\":{\\\"$exists\\\":false}}," +
                "{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\", \"status\" : " +
                "\"pending\", \"maxDurationId\" : { \"$oid\" : \"58e4febe5d0de2038f323183\" }, \"runs\" : [{ " +
                "\"start\" : { \"$numberLong\" : \"1491402430619\" }, \"end\" : { \"$numberLong\" : \"1491402430691\"" +
                " }, \"duration\" : { \"$numberLong\" : \"72\" }, \"query\" : " +
                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}},{\\\"children\\\":null}," +
                "{\\\"children\\\":{\\\"$size\\\":0}}]},{\\\"parent\\\":{\\\"$exists\\\":false}}," +
                "{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\", \"count\" : { " +
                "\"$numberLong\" : \"5\" }, \"id\" : { \"$oid\" : \"58e4febe5d0de2038f323183\" }, \"userID\" : { " +
                "\"$numberLong\" : \"1\" }, \"date\" : { \"$date\" : 1491350400000 }, \"additionalInfo\" : { " +
                "\"header\" : { \"origin\" : \"http://0.0.0.0:9000\", \"host\" : \"127.0.0.1:8081\", \"utcoffset\" : " +
                "\"-240\", \"user-agent\" : \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/57.0.2987.110 Safari/537.36\", \"token\" : " +
                "\"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\" }, \"queryString\" : " +
                "\"pageNumber=1&pageSize=15&ts=1491402430486\", \"body\" : { \"Thing Type\" : \"1\", \"sortProperty\"" +
                " : \"ASC\", \"orderByColumn\" : { \"$numberLong\" : \"1\" } } } }] }", reportLogInfo.toJson());
    }

    @Test
    public void TestReportLogGets() throws Exception {
        assertEquals("00002-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2",
                reportLogInfo.getId());
        assertEquals(reportLogInfo.getType(), "Table Detail");
        assertEquals(reportLogInfo.getName(), "All RFID Tags");
        assertEquals(reportLogInfo.getLastRunDate(), formatter.parse("2017-04-05 10:27:10"));
        assertEquals(reportLogInfo.getMaxDuration(), 72L);
        assertEquals(reportLogInfo.getMaxDurationDate(), formatter.parse("2017-04-05 10:27:10"));
        assertEquals(reportLogInfo.getTotalRuns(), 1L);
        assertEquals(reportLogInfo.getQuery(), "{\"$and\":[{\"$or\":[{\"children\":{\"$exists\":false}}," +
                "{\"children\":null}," +
                "{\"children\":{\"$size\":0}}]},{\"parent\":{\"$exists\":false}},{\"thingTypeId\":1}," +
                "{\"groupId\":{\"$in\":[1,2,3,4,5,6,7]}},{\"thingTypeId\":{\"$in\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14," +
                "15]}}]}");
        assertEquals(PENDING, reportLogInfo.getStatus());
        assertEquals(reportLogInfo.getMaxDurationId(), new ObjectId("58e4febe5d0de2038f323183"));
        assertEquals(reportLogInfo.getRuns(), runs);
    }

    @Test
    public void merge() throws Exception {

    }

    @Test
    public void setIdAndGetId() throws Exception {
        reportLogInfo.setId(20L);
        assertEquals("00020-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2", reportLogInfo.getId());
    }

    @Test
    public void ReportLogInfoConstructorWithDateFormat() throws Exception {
        String reportLogInfoString = "{\n" +
                "    \"_id\" : \"00002-a16b8b3b3c7bd3e824d6dbecb3ed80fc286448b79b80adc2f79db5d99b901acd\",\n" +
                "    \"type\" : \"Table Detail\",\n" +
                "    \"name\" : \"All RFID Tags\",\n" +
                "    \"collectionName\" : \"things\",\n" +
                "    \"lastRunDate\" : ISODate(\"2017-05-10\"),\n" +
                "    \"maxDuration\" : NumberLong(213),\n" +
                "    \"maxDurationDate\" : ISODate(\"2017-05-10\"),\n" +
                "    \"totalRuns\" : 1,\n" +
                "    \"filtersDefinition\" : \"{ 'filters' : {'Thing Type' : '==','Thing Type' : '==' }, 'sort': { " +
                "'serialNumber' : 1 }\",\n" +
                "    \"query\" : \"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2," +
                "3,4,5,6,7]}},{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]}}]}\",\n" +
                "    \"status\" : \"completed\",\n" +
                "    \"maxDurationId\" : ObjectId(\"5913905e5d0de24cce399791\"),\n" +
                "    \"runs\" : [ \n" +
                "        {\n" +
                "            \"start\" : NumberLong(1494454362892),\n" +
                "            \"end\" : NumberLong(1494454363105),\n" +
                "            \"duration\" : NumberLong(213),\n" +
                "            \"filtersDefinition\" : \"{ 'filters' : {'Thing Type' : '==','Thing Type' : '==' }, " +
                "'sort': { 'serialNumber' : 1 }\",\n" +
                "            \"query\" : \"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2," +
                "3,4,5,6,7]}},{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16]}}]}\",\n" +
                "            \"count\" : NumberLong(1),\n" +
                "            \"collectionName\" : \"things\",\n" +
                "            \"sort\" : \"{\\\"serialNumber\\\":1}\",\n" +
                "            \"id\" : ObjectId(\"5913905e5d0de24cce399791\"),\n" +
                "            \"userID\" : NumberLong(1),\n" +
                "            \"date\" : ISODate(\"2017-05-10\"),\n" +
                "            \"additionalInfo\" : {\n" +
                "                \"header\" : {\n" +
                "                    \"origin\" : \"http://0.0.0.0:9000\",\n" +
                "                    \"host\" : \"127.0.0.1:8081\",\n" +
                "                    \"utcoffset\" : \"-240\",\n" +
                "                    \"user-agent\" : \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, " +
                "like Gecko) Chrome/57.0.2987.110 Safari/537.36\",\n" +
                "                    \"token\" : " +
                "\"4e32d15c586ce637c620f2309770daad3065ae0ac9c5c1c92d54029ee4282f1b\"\n" +
                "                },\n" +
                "                \"queryString\" : \"pageNumber=1&pageSize=15&ts=1494454362199\",\n" +
                "                \"body\" : {\n" +
                "                    \"Thing Type\" : \"1\",\n" +
                "                    \"orderByColumn\" : 1,\n" +
                "                    \"sortProperty\" : \"ASC\"\n" +
                "                }\n" +
                "            }\n" +
                "        }\n" +
                "    ],\n" +
                "    \"indexInformation\" : {\n" +
                "        \"indexName\" : \"auto_83d4d4e5e4466d718fe901c9a80889a2\",\n" +
                "        \"definition\" : \"{\\\"thingTypeId\\\":1,\\\"serialNumber\\\":1,\\\"groupId\\\":1}\",\n" +
                "        \"starDate\" : ISODate(\"2017-05-10\"),\n" +
                "        \"endDate\" : ISODate(\"2017-05-10\")\n" +
                "    }\n" +
                "}";
        ReportLogInfo reportLogInfo = new ReportLogInfo(
                BasicDBObject.parse(reportLogInfoString),
                new DateFormatAndTimeZone("Europe/London"));
        assertNotNull(reportLogInfo);
        String dateExpected = "2017-05-10T01:00+0100";
        assertEquals(dateExpected, reportLogInfo.getLastRunDateIsoFormat());
        assertEquals(dateExpected, reportLogInfo.getMaxDurationDateIsoFormat());
        assertEquals(dateExpected, reportLogInfo.getIndexInformation().getStartDateIsoFormat());
        assertEquals(dateExpected, reportLogInfo.getIndexInformation().getEndDateIsoFormat());
    }
}