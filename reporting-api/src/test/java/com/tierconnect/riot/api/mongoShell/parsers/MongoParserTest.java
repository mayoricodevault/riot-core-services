package com.tierconnect.riot.api.mongoShell.parsers;

import org.bson.BsonArray;
import org.bson.Document;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created by achambi on 12/23/16.
 * Class to test the document parser and array of document parser
 */
public class MongoParserTest {
    @Test
    public void parseDocument() throws Exception {
        final String jsonData = "{\"data\": [\n" +
                "    {   \"_id\" : NumberLong(1), \n" +
                "        \"groupTypeId\" : NumberLong(3), \n" +
                "        \"groupTypeName\" : \"LOQUESEAFac\", \n" +
                "        \"groupTypeCode\" : \"\", \n" +
                "        \"groupId\" : NumberLong(3), \n" +
                "        \"groupCode\" : \"DF\", \n" +
                "        \"groupName\" : \"Default Facility\", \n" +
                "        \"thingTypeId\" : NumberLong(1), \n" +
                "        \"thingTypeCode\" : \"default_rfid_thingtype\", \"thingTypeName\" : \"Default RFID Thing " +
                "Type\", \n" +
                "        \"name\" : \"RFID1234567890\", \n" +
                "        \"serialNumber\" : \"RFID1234567890\", \n" +
                "        \"modifiedTime\" : ISODate(\"2016-11-21T19:49:41.428Z\"), \"createdTime\" : ISODate" +
                "(\"2016-11-21T19:49:41.428Z\"), \"time\" : ISODate(\"2016-11-21T19:49:40.137Z\") \n" +
                "    },\n" +
                "    { \"_id\" : NumberLong(2), \n" +
                "      \"groupTypeId\" : NumberLong(3), \n" +
                "      \"groupTypeName\" : \"Facility\", \n" +
                "      \"groupTypeCode\" : \"\", \n" +
                "      \"groupId\" : NumberLong(3), \n" +
                "      \"groupCode\" : \"DF\", \n" +
                "      \"groupName\" : \"Default Facility\", \n" +
                "      \"thingTypeId\" : NumberLong(2), \n" +
                "      \"thingTypeCode\" : \"default_gps_thingtype\", \n" +
                "      \"thingTypeName\" : \"Default GPS Thing Type\", \n" +
                "      \"name\" : \"GPS1234567890\", \n" +
                "      \"serialNumber\" : \"GPS1234567890\", \n" +
                "      \"modifiedTime\" : ISODate(\"2016-11-21T19:49:42.283Z\"), \n" +
                "      \"createdTime\" : ISODate(\"2016-11-21T19:49:42.283Z\"), \n" +
                "      \"time\" : ISODate(\"2016-11-21T19:49:40.137Z\") \n" +
                "    }\n" +
                "]\n" +
                "}";
        Document document = MongoParser.parseDocument(jsonData);
        assertNotNull(document);
        List<Document> rows = (ArrayList<Document>) document.get("data");
        assertThat(rows.size(), is(2));
        int index = 1;
        for (Document item : rows) {
            assertThat(Integer.parseInt(item.get("_id").toString()), is(index));
            index++;
        }
    }

    @Test
    public void parseBsonArray() throws Exception {
        final String jsonData
                = "[{\"_id\" : NumberLong(1), \n" +
                "\"groupTypeId\" : NumberLong(3), \n" +
                "\"groupTypeName\" : \"LOQUESEAFac\", \n" +
                "\"groupTypeCode\" : \"\", \n" +
                "\"groupId\" : NumberLong(3), \n" +
                "\"groupCode\" : \"DF\", \n" +
                "\"groupName\" : \"Default Facility\", \n" +
                "\"thingTypeId\" : NumberLong(1), \n" +
                "\"thingTypeCode\" : \"default_rfid_thingtype\", \"thingTypeName\" : \"Default RFID Thing Type\", \n" +
                "\"name\" : \"RFID1234567890\", \n" +
                "\"serialNumber\" : \"RFID1234567890\", \n" +
                "\"modifiedTime\" : ISODate(\"2016-11-21T19:49:41.428Z\"), \"createdTime\" : ISODate" +
                "(\"2016-11-21T19:49:41.428Z\"), \"time\" : ISODate(\"2016-11-21T19:49:40.137Z\") \n" +
                "\n" +
                "},\n" +
                "{ \"_id\" : NumberLong(2), \"groupTypeId\" : NumberLong(3), \"groupTypeName\" : \"Facility\", " +
                "\"groupTypeCode\" : \"\", \"groupId\" : NumberLong(3), \"groupCode\" : \"DF\", \"groupName\" : " +
                "\"Default Facility\", \"thingTypeId\" : NumberLong(2), \"thingTypeCode\" : " +
                "\"default_gps_thingtype\", \"thingTypeName\" : \"Default GPS Thing Type\", \"name\" : " +
                "\"GPS1234567890\", \"serialNumber\" : \"GPS1234567890\", \"modifiedTime\" : ISODate" +
                "(\"2016-11-21T19:49:42.283Z\"), \"createdTime\" : ISODate(\"2016-11-21T19:49:42.283Z\"), \"time\" : " +
                "ISODate(\"2016-11-21T19:49:40.137Z\") }]";
        BsonArray result = MongoParser.parseBsonArray(jsonData);
        assertNotNull(result);
    }

}