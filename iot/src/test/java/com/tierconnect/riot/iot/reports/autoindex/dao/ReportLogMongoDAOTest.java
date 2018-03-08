package com.tierconnect.riot.iot.reports.autoindex.dao;


import com.mongodb.BasicDBObject;

import com.tierconnect.riot.appcore.core.BaseTestIOT;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;

import com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.*;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogStatus.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created by achambi on 4/6/17.
 * CLas for Test report log dao.
 */
public class ReportLogMongoDAOTest extends BaseTestIOT {

    @Before
    public void setUp() throws Exception {
        MongoDAOUtil.getInstance().db.getCollection("reportLogs").drop();
        MongoDAOUtil.getInstance().db.getCollection("reportLogs").insert(BasicDBObject.parse("{\n" +
                "    \"_id\" : \"00001-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\",\n" +
                "    \"type\" : \"Table Detail\",\n" +
                "    \"name\" : \"All RFID Tags\",\n" +
                "    \"lastRunDate\" : ISODate(\"2017-04-05\"),\n" +
                "    \"maxDuration\" : NumberLong(72),\n" +
                "    \"maxDurationDate\" : ISODate(\"2017-04-05\"),\n" +
                "    \"checked\" : false, \n" +
                "    \"totalRuns\" : 1,\n" +
                "    \"query\" : \"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2," +
                "3,4,5,6,7]}},{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                "    \"status\" : \"pending\",\n" +
                "    \"maxDurationId\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                "    \"runs\" : [ \n" +
                "        {\n" +
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
                "        }\n" +
                "    ]\n" +
                "}"));
        MongoDAOUtil.getInstance().db.getCollection("reportLogs").insert(BasicDBObject.parse("{\n" +
                "    \"_id\" : \"00002-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\",\n" +
                "    \"type\" : \"Table Detail\",\n" +
                "    \"name\" : \"All RFID Tags\",\n" +
                "    \"lastRunDate\" : ISODate(\"2017-04-05\"),\n" +
                "    \"maxDuration\" : NumberLong(72),\n" +
                "    \"maxDurationDate\" : ISODate(\"2017-04-05\"),\n" +
                "    \"totalRuns\" : 1,\n" +
                "    \"query\" : \"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2," +
                "3,4,5,6,7]}},{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                "    \"status\" : \"pending\",\n" +
                "    \"maxDurationId\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                "    \"runs\" : [ \n" +
                "        {\n" +
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
                "        }\n" +
                "    ]\n" +
                "}"));

        MongoDAOUtil.getInstance().db.getCollection("reportLogs").insert(BasicDBObject.parse("{\n" +
                "    \"_id\" : \"00003-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\",\n" +
                "    \"type\" : \"Table Detail\",\n" +
                "    \"name\" : \"All RFID Tags\",\n" +
                "    \"lastRunDate\" : ISODate(\"2017-04-05\"),\n" +
                "    \"maxDuration\" : NumberLong(72),\n" +
                "    \"maxDurationDate\" : ISODate(\"2017-04-05\"),\n" +
                "    \"totalRuns\" : 1,\n" +
                "    \"query\" : \"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2," +
                "3,4,5,6,7]}},{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                "    \"status\" : \"pending\",\n" +
                "    \"maxDurationId\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                "    \"runs\" : [ \n" +
                "        {\n" +
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
                "        }\n" +
                "    ]\n" +
                "}"));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void insert() throws Exception {
        ReportLogInfo reportLogInfo = new ReportLogInfo();
        reportLogInfo.setId("test-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2");
        reportLogInfo.setType("REPORT TYPE");
        reportLogInfo.setName("test");
        reportLogInfo.setCollectionName("collectionName");
        reportLogInfo.setLastRunDate(new Date());
        reportLogInfo.setMaxDuration(100);
        reportLogInfo.setMaxDurationDate(new Date());
        reportLogInfo.setTotalRuns(10L);
        reportLogInfo.setQuery("QUERY STRING");
        reportLogInfo.setStatus(PENDING);
        reportLogInfo.setMaxDurationId(new ObjectId());
        List<ReportLogDetail> runs = new LinkedList<>();
        ReportLogDetail reportLogDetail = new ReportLogDetail();
        reportLogDetail.setId(new ObjectId());
        reportLogDetail.setStart(123456);
        reportLogDetail.setStart(12L);
        reportLogDetail.setDuration(12L);
        reportLogDetail.setQuery("queryTest child");
        reportLogDetail.setCount(12L);
        reportLogDetail.setId(new ObjectId());
        reportLogDetail.setUserId(1);
        reportLogDetail.setDate(new Date());
        AdditionalInfo additionalInfo = new AdditionalInfo();
        Header header = new Header();
        header.setOrigin("http://0.0.0.0:9000");
        header.setHost("127.0.0.1:8081");
        header.setUtcoffset("-240");
        header.setUserAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/57.0.2987.110 Safari/537.36");
        header.setToken("615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077");
        additionalInfo.setHeader(header);
        additionalInfo.setQueryString("pageNumber=1&pageSize=15&ts=1491402430486");
        additionalInfo.setBody(BasicDBObject.parse("{\n" +
                "                    \"Thing Type\" : \"1\",\n" +
                "                    \"sortProperty\" : \"ASC\",\n" +
                "                    \"orderByColumn\" : NumberLong(1)\n" +
                "                }"));
        reportLogDetail.setAdditionalInfo(additionalInfo);
        runs.add(reportLogDetail);
        reportLogInfo.setRuns(runs);
        ReportLogMongoDAO.getInstance().insert(reportLogInfo);
    }

    @Test
    public void get() throws Exception {
        ReportLogInfo result = ReportLogMongoDAO.getInstance().get
                ("00002-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2");
        assertEquals("{ \"_id\" : \"00002-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\", " +
                "\"type\" : \"Table Detail\", \"name\" : \"All RFID Tags\", \"collectionName\" : null, " +
                "\"lastRunDate\" : { \"$date\" : 1491350400000 }, \"maxDuration\" : { \"$numberLong\" : \"72\" }, " +
                "\"maxDurationDate\" : { \"$date\" : 1491350400000 }, \"totalRuns\" : { \"$numberLong\" : \"1\" }, " +
                "\"query\" : \"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2," +
                "3,4,5,6,7]}},{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\", " +
                "\"status\" : \"pending\", \"maxDurationId\" : { \"$oid\" : \"58e4febe5d0de2038f323183\" }, " +
                "\"checked\" : false, \"filtersDefinition\" : null, \"runs\" : [{ \"start\" : { \"$numberLong\" : " +
                "\"1491402430619\" }, \"end\" : { \"$numberLong\" : \"1491402430691\" }, \"duration\" : { " +
                "\"$numberLong\" : \"72\" }, \"query\" : " +
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
                " : \"ASC\", \"orderByColumn\" : { \"$numberLong\" : \"1\" } } } }] }", result.toJson());
    }

    @Test
    public void getByIndexName() throws Exception {
        ReportLogMongoDAO.getInstance().updateStatusAndIndexInformation
                ("00001-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2",
                        COMPLETED,
                        "indexTest",
                        "indexDefinition",
                        null,
                        null);
        ReportLogMongoDAO.getInstance().updateStatusAndIndexInformation
                ("00002-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2",
                        COMPLETED,
                        "indexTest",
                        "indexDefinition",
                        null,
                        null);
        ReportLogMongoDAO.getInstance().updateStatusAndIndexInformation
                ("00003-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2",
                        COMPLETED,
                        "indexTest",
                        "indexDefinition",
                        null,
                        null);
        List<ReportLogInfo> reportLogInfoList = ReportLogMongoDAO.getInstance().getByIndexName("indexTest",
                new DateFormatAndTimeZone("Europe/London"));
        assertNotNull(reportLogInfoList);
        assertEquals("[{ \"_id\" : \"00001-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\" , " +
                "\"type\" : \"Table Detail\" , \"name\" : \"All RFID Tags\" , \"collectionName\" :  null  , " +
                "\"lastRunDate\" : { \"$date\" : \"2017-04-05T00:00:00.000Z\"} , \"maxDuration\" : 72 , " +
                "\"maxDurationDate\" : { \"$date\" : \"2017-04-05T00:00:00.000Z\"} , \"totalRuns\" : 1 , \"query\" : " +
                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}},{\\\"children\\\":null}," +
                "{\\\"children\\\":{\\\"$size\\\":0}}]},{\\\"parent\\\":{\\\"$exists\\\":false}}," +
                "{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\" , \"status\" : " +
                "\"completed\" , \"maxDurationId\" : { \"$oid\" : \"58e4febe5d0de2038f323183\"} , \"checked\" : false" +
                " , \"filtersDefinition\" :  null  , \"runs\" : [ { \"start\" : 1491402430619 , \"end\" : " +
                "1491402430691 , \"duration\" : 72 , \"query\" : " +
                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}},{\\\"children\\\":null}," +
                "{\\\"children\\\":{\\\"$size\\\":0}}]},{\\\"parent\\\":{\\\"$exists\\\":false}}," +
                "{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\" , \"count\" : 5 , " +
                "\"id\" : { \"$oid\" : \"58e4febe5d0de2038f323183\"} , \"userID\" : 1 , \"date\" : " +
                "\"2017-04-05T01:00:00.000+0100\" , \"additionalInfo\" : { \"header\" : { \"origin\" : " +
                "\"http://0.0.0.0:9000\" , \"host\" : \"127.0.0.1:8081\" , \"utcoffset\" : \"-240\" , \"user-agent\" " +
                ": \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.110 " +
                "Safari/537.36\" , \"token\" : \"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\"} " +
                ", \"queryString\" : \"pageNumber=1&pageSize=15&ts=1491402430486\" , \"body\" : { \"Thing Type\" : " +
                "\"1\" , \"sortProperty\" : \"ASC\" , \"orderByColumn\" : 1}}}] , \"indexInformation\" : { " +
                "\"indexName\" : \"indexTest\" , \"definition\" : \"indexDefinition\"}}, { \"_id\" : " +
                "\"00002-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\" , \"type\" : \"Table " +
                "Detail\" , \"name\" : \"All RFID Tags\" , \"collectionName\" :  null  , \"lastRunDate\" : { " +
                "\"$date\" : \"2017-04-05T00:00:00.000Z\"} , \"maxDuration\" : 72 , \"maxDurationDate\" : { \"$date\"" +
                " : \"2017-04-05T00:00:00.000Z\"} , \"totalRuns\" : 1 , \"query\" : " +
                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}},{\\\"children\\\":null}," +
                "{\\\"children\\\":{\\\"$size\\\":0}}]},{\\\"parent\\\":{\\\"$exists\\\":false}}," +
                "{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\" , \"status\" : " +
                "\"completed\" , \"maxDurationId\" : { \"$oid\" : \"58e4febe5d0de2038f323183\"} , \"checked\" : false" +
                " , \"filtersDefinition\" :  null  , \"runs\" : [ { \"start\" : 1491402430619 , \"end\" : " +
                "1491402430691 , \"duration\" : 72 , \"query\" : " +
                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}},{\\\"children\\\":null}," +
                "{\\\"children\\\":{\\\"$size\\\":0}}]},{\\\"parent\\\":{\\\"$exists\\\":false}}," +
                "{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\" , \"count\" : 5 , " +
                "\"id\" : { \"$oid\" : \"58e4febe5d0de2038f323183\"} , \"userID\" : 1 , \"date\" : " +
                "\"2017-04-05T01:00:00.000+0100\" , \"additionalInfo\" : { \"header\" : { \"origin\" : " +
                "\"http://0.0.0.0:9000\" , \"host\" : \"127.0.0.1:8081\" , \"utcoffset\" : \"-240\" , \"user-agent\" " +
                ": \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.110 " +
                "Safari/537.36\" , \"token\" : \"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\"} " +
                ", \"queryString\" : \"pageNumber=1&pageSize=15&ts=1491402430486\" , \"body\" : { \"Thing Type\" : " +
                "\"1\" , \"sortProperty\" : \"ASC\" , \"orderByColumn\" : 1}}}] , \"indexInformation\" : { " +
                "\"indexName\" : \"indexTest\" , \"definition\" : \"indexDefinition\"}}, { \"_id\" : " +
                "\"00003-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\" , \"type\" : \"Table " +
                "Detail\" , \"name\" : \"All RFID Tags\" , \"collectionName\" :  null  , \"lastRunDate\" : { " +
                "\"$date\" : \"2017-04-05T00:00:00.000Z\"} , \"maxDuration\" : 72 , \"maxDurationDate\" : { \"$date\"" +
                " : \"2017-04-05T00:00:00.000Z\"} , \"totalRuns\" : 1 , \"query\" : " +
                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}},{\\\"children\\\":null}," +
                "{\\\"children\\\":{\\\"$size\\\":0}}]},{\\\"parent\\\":{\\\"$exists\\\":false}}," +
                "{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\" , \"status\" : " +
                "\"completed\" , \"maxDurationId\" : { \"$oid\" : \"58e4febe5d0de2038f323183\"} , \"checked\" : false" +
                " , \"filtersDefinition\" :  null  , \"runs\" : [ { \"start\" : 1491402430619 , \"end\" : " +
                "1491402430691 , \"duration\" : 72 , \"query\" : " +
                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}},{\\\"children\\\":null}," +
                "{\\\"children\\\":{\\\"$size\\\":0}}]},{\\\"parent\\\":{\\\"$exists\\\":false}}," +
                "{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\" , \"count\" : 5 , " +
                "\"id\" : { \"$oid\" : \"58e4febe5d0de2038f323183\"} , \"userID\" : 1 , \"date\" : " +
                "\"2017-04-05T01:00:00.000+0100\" , \"additionalInfo\" : { \"header\" : { \"origin\" : " +
                "\"http://0.0.0.0:9000\" , \"host\" : \"127.0.0.1:8081\" , \"utcoffset\" : \"-240\" , \"user-agent\" " +
                ": \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.110 " +
                "Safari/537.36\" , \"token\" : \"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\"} " +
                ", \"queryString\" : \"pageNumber=1&pageSize=15&ts=1491402430486\" , \"body\" : { \"Thing Type\" : " +
                "\"1\" , \"sortProperty\" : \"ASC\" , \"orderByColumn\" : 1}}}] , \"indexInformation\" : { " +
                "\"indexName\" : \"indexTest\" , \"definition\" : \"indexDefinition\"}}]", reportLogInfoList.toString
                ());
    }

    @Test
    public void update() throws Exception {
        ReportLogInfo reportLogInfo = new ReportLogInfo(BasicDBObject.parse("{\n" +
                "    \"_id\" : \"00001-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\",\n" +
                "    \"type\" : \"Table Detail\",\n" +
                "    \"name\" : \"All RFID Tags [UPDATED]\",\n" +
                "    \"lastRunDate\" : ISODate(\"2017-04-05\"),\n" +
                "    \"maxDuration\" : NumberLong(72),\n" +
                "    \"maxDurationDate\" : ISODate(\"2017-04-05\"),\n" +
                "    \"totalRuns\" : 1,\n" +
                "    \"query\" : \"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2," +
                "3,4,5,6,7]}},{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                "    \"status\" : \"pending\",\n" +
                "    \"maxDurationId\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                "    \"runs\" : [ \n" +
                "        {\n" +
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
                "        }\n" +
                "    ]\n" +
                "}"));
        ReportLogMongoDAO.getInstance().update(reportLogInfo);
        ReportLogInfo result = ReportLogMongoDAO.getInstance().get(reportLogInfo.getId());
        assertEquals("All RFID Tags [UPDATED]", result.getName());
    }

    @Test
    public void upsert() throws Exception {
        String upsertTest = "{\n" +
                "    \"_id\" : \"00003-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\",\n" +
                "    \"type\" : \"Table Detail\",\n" +
                "    \"name\" : \"All RFID Tags Updated\",\n" +
                "    \"lastRunDate\" : ISODate(\"2017-04-05\"),\n" +
                "    \"maxDuration\" : NumberLong(72),\n" +
                "    \"maxDurationDate\" : ISODate(\"2017-04-05\"),\n" +
                "    \"totalRuns\" : 1,\n" +
                "    \"query\" : \"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2," +
                "3,4,5,6,7]}},{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                "    \"status\" : \"pending\",\n" +
                "    \"maxDurationId\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                "    \"runs\" : [ \n" +
                "        {\n" +
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
                "        }\n" +
                "    ]\n" +
                "}";
        ReportLogMongoDAO.getInstance().upsert(new ReportLogInfo(BasicDBObject.parse(upsertTest)));
        ReportLogInfo reportLogInfo = ReportLogMongoDAO.getInstance().get
                ("00003-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2");
        assertEquals(reportLogInfo.getName(), "All RFID Tags Updated");
    }

    @Test
    public void updateStatus() throws Exception {
        ReportLogMongoDAO.getInstance().updateStatus
                ("00001-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2", SLOW_INDEX);
        ReportLogInfo result = ReportLogMongoDAO.getInstance().get
                ("00001-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2");
        assertThat(result.getStatus(), is(SLOW_INDEX));
        ReportLogMongoDAO.getInstance().updateStatus
                ("00001-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2", DELETED);
        result = ReportLogMongoDAO.getInstance().get
                ("00001-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2");
        assertThat(result.getStatus(), is(DELETED));
    }

    @Test
    public void getNull() throws Exception {
        ReportLogInfo result = ReportLogMongoDAO.getInstance().get("NoIdFound");
        assertNull(result);
    }

    @Test
    public void getIdsByStatus() throws Exception {
        ReportLogMongoDAO.getInstance().updateStatusAndIndexInformation(
                "00001-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2",
                ReportLogStatus.COMPLETED,
                "indexName",
                "indexDefinition",
                null,
                null);
        ReportLogMongoDAO.getInstance().updateStatusAndIndexInformation(
                "00002-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2",
                ReportLogStatus.COMPLETED,
                "indexName",
                "indexDefinition",
                null,
                null);
        ReportLogMongoDAO.getInstance().updateStatusAndIndexInformation(
                "00003-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2",
                ReportLogStatus.COMPLETED,
                "indexName",
                "indexDefinition",
                null,
                null);
        String[] result = ReportLogMongoDAO.getInstance().getIndexNamesByStatus(
                ReportLogStatus.COMPLETED.getValue(),
                SLOW_INDEX.getValue());
        assertEquals(1, result.length);
    }

    @Test
    public void updateStatusAndIndexInformation() throws Exception {
        ReportLogMongoDAO.getInstance().updateStatusAndIndexInformation
                ("00001-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2",
                        COMPLETED,
                        "indexName",
                        "indexDefinition",
                        null,
                        null);
        ReportLogInfo result = ReportLogMongoDAO.getInstance().get
                ("00001-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2");
        assertThat(result.getStatus(), is(COMPLETED));
        assertThat(result.getIndexInformation().toString(), is("{ \"indexName\" : \"indexName\" , \"definition\" : " +
                "\"indexDefinition\"}"));
        assertThat("Index name not is: indexName ", result.getIndexInformation().getIndexName(), is("indexName"));
        assertThat("Index Definition not is: indexDefinition ", result.getIndexInformation().getIndexDefinition(), is
                ("indexDefinition"));
    }

    @Test
    public void isCurrentlyIndexing() throws Exception {
        assertEquals(true, ReportLogMongoDAO.getInstance().isCurrentlyIndexing("00001", ReportLogStatus.IN_PROGRESS
                .getValue(), ReportLogStatus.PENDING.getValue()));
    }

}

