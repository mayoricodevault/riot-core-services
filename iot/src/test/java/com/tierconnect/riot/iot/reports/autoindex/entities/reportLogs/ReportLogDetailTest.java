package com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs;


import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;

/**
 * Created by achambi on 4/5/17.
 * class for test report log detail ORM
 */
public class ReportLogDetailTest {

    private ReportLogDetail reportLogDetail;
    private SimpleDateFormat formatter;

    @Before
    public void setUp() throws Exception {
        BasicDBObject additionalInfo = BasicDBObject.parse("{\n" +
                "\t\t\t\t\"header\" : {\n" +
                "\t\t\t\t\t\"origin\" : \"http://0.0.0.0:9000\",\n" +
                "\t\t\t\t\t\"host\" : \"127.0.0.1:8081\",\n" +
                "\t\t\t\t\t\"utcoffset\" : \"-240\",\n" +
                "\t\t\t\t\t\"user-agent\" : \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/57.0.2987.110 Safari/537.36\",\n" +
                "\t\t\t\t\t\"token\" : \"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\"\n" +
                "\t\t\t\t},\n" +
                "\t\t\t\t\"queryString\" : \"pageNumber=1&pageSize=15&ts=1491402430486\",\n" +
                "\t\t\t\t\"body\" : {\n" +
                "\t\t\t\t\t\"Thing Type\" : \"1\",\n" +
                "\t\t\t\t\t\"sortProperty\" : \"ASC\",\n" +
                "\t\t\t\t\t\"orderByColumn\" : NumberLong(1)\n" +
                "\t\t\t\t}\n" +
                "\t\t\t}");
        formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        reportLogDetail = new ReportLogDetail();
        reportLogDetail.setStart(formatter.parse("2017-04-05 10:27:10").getTime());
        reportLogDetail.setEnd(formatter.parse("2017-04-05 10:27:10").getTime());
        reportLogDetail.setDuration(72L);
        reportLogDetail.setQuery("{\"$and\":[{\"$or\":[{\"children\":{\"$exists\":false}},{\"children\":null}," +
                "{\"children\":{\"$size\":0}}]},{\"parent\":{\"$exists\":false}},{\"thingTypeId\":1}," +
                "{\"groupId\":{\"$in\":[1,2,3,4,5,6,7]}},{\"thingTypeId\":{\"$in\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14," +
                "15]}}]}");
        reportLogDetail.setCount(5L);
        reportLogDetail.setId(new ObjectId("58e4febe5d0de2038f323183"));
        reportLogDetail.setUserId(1L);
        reportLogDetail.setDate(formatter.parse("2017-04-05 10:27:10"));
        reportLogDetail.setAdditionalInfo(new AdditionalInfo(additionalInfo));
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void setReportLogDetailSets() throws Exception {
        Assert.assertEquals("{ \"start\" : { \"$numberLong\" : \"1491388030000\" }, \"end\" : { \"$numberLong\" : " +
                "\"1491388030000\" }, \"duration\" : { \"$numberLong\" : \"72\" }, \"query\" : " +
                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}},{\\\"children\\\":null}," +
                "{\\\"children\\\":{\\\"$size\\\":0}}]},{\\\"parent\\\":{\\\"$exists\\\":false}}," +
                "{\\\"thingTypeId\\\":1},{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\", \"count\" : { " +
                "\"$numberLong\" : \"5\" }, \"id\" : { \"$oid\" : \"58e4febe5d0de2038f323183\" }, \"userID\" : { " +
                "\"$numberLong\" : \"1\" }, \"date\" : { \"$date\" : 1491388030000 }, \"additionalInfo\" : { " +
                "\"header\" : { \"origin\" : \"http://0.0.0.0:9000\", \"host\" : \"127.0.0.1:8081\", \"utcoffset\" : " +
                "\"-240\", \"user-agent\" : \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/57.0.2987.110 Safari/537.36\", \"token\" : " +
                "\"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\" }, \"queryString\" : " +
                "\"pageNumber=1&pageSize=15&ts=1491402430486\", \"body\" : { \"Thing Type\" : \"1\", \"sortProperty\"" +
                " : \"ASC\", \"orderByColumn\" : { \"$numberLong\" : \"1\" } } } }", reportLogDetail.toJson());
    }

    @Test
    public void setReportLogDetailGets() throws Exception {
        Assert.assertEquals(formatter.parse("2017-04-05 10:27:10").getTime(), reportLogDetail.getStart());
        Assert.assertEquals(formatter.parse("2017-04-05 10:27:10").getTime(), reportLogDetail.getEnd());
        Assert.assertEquals(72L, reportLogDetail.getDuration());
        Assert.assertEquals("{\"$and\":[{\"$or\":[{\"children\":{\"$exists\":false}}," +
                "{\"children\":null},{\"children\":{\"$size\":0}}]},{\"parent\":{\"$exists\":false}}," +
                "{\"thingTypeId\":1},{\"groupId\":{\"$in\":[1,2,3,4,5,6,7]}},{\"thingTypeId\":{\"$in\":[1,2,3,4,5,6," +
                "7,8,9,10,11,12,13,14,15]}}]}", reportLogDetail.getQuery());
        Assert.assertEquals(5L, reportLogDetail.getCount());
        Assert.assertEquals(new ObjectId("58e4febe5d0de2038f323183"), reportLogDetail.getId());
        Assert.assertEquals(1L, reportLogDetail.getUserId());
        Assert.assertEquals(formatter.parse("2017-04-05 10:27:10"), reportLogDetail.getDate());
        Assert.assertEquals("{ \"header\" : { \"origin\" : \"http://0.0.0.0:9000\", \"host\" : \"127.0.0.1:8081\", " +
                "\"utcoffset\" : \"-240\", \"user-agent\" : \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/57.0.2987.110 Safari/537.36\", \"token\" : " +
                "\"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\" }, \"queryString\" : " +
                "\"pageNumber=1&pageSize=15&ts=1491402430486\", \"body\" : { \"Thing Type\" : \"1\", \"sortProperty\"" +
                " : \"ASC\", \"orderByColumn\" : { \"$numberLong\" : \"1\" } } }", reportLogDetail.getAdditionalInfo
                ().toJson());
    }

}