package com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs;

import com.mongodb.BasicDBObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by achambi on 4/5/17.
 * Class for test additional information.
 */
public class AdditionalInfoTest {
    private AdditionalInfo additionalInfo;

    @Before
    public void setUp() throws Exception {
        BasicDBObject header = BasicDBObject.parse(" {\n" +
                "\t\t\t\t\t\"origin\" : \"http://0.0.0.0:9000\",\n" +
                "\t\t\t\t\t\"host\" : \"127.0.0.1:8081\",\n" +
                "\t\t\t\t\t\"utcoffset\" : \"-240\",\n" +
                "\t\t\t\t\t\"user-agent\" : \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/57.0.2987.110 Safari/537.36\",\n" +
                "\t\t\t\t\t\"token\" : \"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\"\n" +
                "\t\t\t\t}");
        BasicDBObject body = BasicDBObject.parse("{\n" +
                "\t\t\t\t\t\"Thing Type\" : \"1\",\n" +
                "\t\t\t\t\t\"sortProperty\" : \"ASC\",\n" +
                "\t\t\t\t\t\"orderByColumn\" : NumberLong(1)\n" +
                "\t\t\t\t}");
        additionalInfo = new AdditionalInfo();
        additionalInfo.setHeader(new Header(header));
        additionalInfo.setQueryString("pageNumber=1&pageSize=15&ts=1491402430486");
        additionalInfo.setBody(body);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void AdditionalInfoSets() throws Exception {
        assertEquals("{ \"header\" : { \"origin\" : \"http://0.0.0.0:9000\", \"host\" : \"127.0.0.1:8081\", " +
                "\"utcoffset\" : \"-240\", \"user-agent\" : \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/57.0.2987.110 Safari/537.36\", \"token\" : " +
                "\"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\" }, \"queryString\" : " +
                "\"pageNumber=1&pageSize=15&ts=1491402430486\", \"body\" : { \"Thing Type\" : \"1\", \"sortProperty\"" +
                " : \"ASC\", \"orderByColumn\" : { \"$numberLong\" : \"1\" } } }", additionalInfo.toJson());
    }

    @Test
    public void AdditionalInfoGets() throws Exception {
        assertEquals("{ \"origin\" : \"http://0.0.0.0:9000\", \"host\" : \"127.0.0.1:8081\", \"utcoffset\" : " +
                "\"-240\", \"user-agent\" : \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/57.0.2987.110 Safari/537.36\", \"token\" : " +
                "\"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\" }", additionalInfo.getHeader()
                .toJson());
        assertEquals("pageNumber=1&pageSize=15&ts=1491402430486", additionalInfo.getQueryString());
        assertEquals("{ \"Thing Type\" : \"1\", \"sortProperty\" : \"ASC\", \"orderByColumn\" : { " +
                "\"$numberLong\" : \"1\" } }", additionalInfo.getBody().toJson());
    }
}