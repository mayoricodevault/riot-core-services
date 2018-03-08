package com.tierconnect.riot.controllers;

import com.google.common.base.Charsets;
import com.tierconnect.riot.commons.utils.DateTimeFormatterHelper;
import com.tierconnect.riot.iot.utils.rest.ExecuteActionException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.bson.Document;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by vealaro on 7/5/17.
 */
public class LastValueSnapshotTest {

    private static final Logger LOGGER = Logger.getLogger(LastValueSnapshotTest.class);

    private static final String ipAddress = "localhost";
    private static final String port = "8080";
    private static final String apiKey = "7B4BCCDC";
    private static final String URLBaseString = String.format("http://%1s:%1s/riot-core-services/api/", ipAddress, port);
    private static final boolean createBlink = true;
    private static int sn = 100;

    @Test
    public void allExample() throws Exception {
        example1();
        example2();
        example3();
        example4();
        example5();
    }

    /**
     * report history style
     * eNode no time series and status is time series
     */
    @Test
    public void example0() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "RFID0000A";
        valueThings.add(createJSON("20170101080000", serialNumber, new Document("eNode", "ABC")));
        valueThings.add(createJSON("20170101090000", serialNumber, new Document("status", "ACTIVE")));
        valueThings.add(createJSON("20170101100000", serialNumber, new Document("status", "INACTIVE")));
        valueThings.add(createJSON("20170101110000", serialNumber, new Document("status", "INACTIVE").append("eNode", "DOOR")));
        valueThings.add(createJSON("20170101120000", serialNumber, new Document("eNode", "TEST")));
        executeCreateUpdateThing(valueThings);
    }

    /**
     * create thing with one udf no timeseries
     */
    @Test
    public void example1() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "RFID0001";
        valueThings.add(createJSON("20170101080000", serialNumber, new Document("eNode", "ABC")));
        valueThings.add(createJSON("20170101090000", serialNumber, new Document("eNode", "XYZ")));
        valueThings.add(createJSON("20170101100000", serialNumber, new Document("eNode", "QWE")));
        valueThings.add(createJSON("20170101110000", serialNumber, new Document("eNode", "ABC")));
        valueThings.add(createJSON("20170101120000", serialNumber, new Document("status", "ACTIVE")));
        valueThings.add(createJSON("20170101140000", serialNumber, new Document("status", "INACTIVE").append("eNode", "DOOR")));
        valueThings.add(createJSON("20170101150000", serialNumber, new Document("eNode", "TEST")));
        valueThings.add(createJSON("20170101160000", serialNumber, new Document("eNode", "BASE")));
        valueThings.add(createJSON("20170101170000", serialNumber, new Document("status", "ACTIVE").append("eNode", "ENODEVAL")));
        valueThings.add(createJSON("20170101180000", serialNumber, new Document("eNode", "ABC")));
        executeCreateUpdateThing(valueThings);
    }

    /**
     * create thing with two udf no timeseries
     */
    @Test
    public void example2() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "RFID0002";
        valueThings.add(createJSON("20170101080000", serialNumber, new Document("eNode", "ABC")));
        valueThings.add(createJSON("20170101090000", serialNumber, new Document("udfNTS", "123")));
        valueThings.add(createJSON("20170101100000", serialNumber, new Document("udfNTS", "456")));
        valueThings.add(createJSON("20170101110000", serialNumber, new Document("eNode", "XYZ").append("udfNTS", "789")));
        valueThings.add(createJSON("20170101120000", serialNumber, new Document("status", "ACTIVE")));
        valueThings.add(createJSON("20170101140000", serialNumber, new Document("status", "INACTIVE").append("eNode", "DOOR")));
        valueThings.add(createJSON("20170101150000", serialNumber, new Document("eNode", "TEST")));
        valueThings.add(createJSON("20170101160000", serialNumber, new Document("eNode", "BASE")));
        valueThings.add(createJSON("20170101170000", serialNumber, new Document("status", "ACTIVE").append("udfNTS", "111")));
        valueThings.add(createJSON("20170101180000", serialNumber, new Document("eNode", "ABC")));
        executeCreateUpdateThing(valueThings);
    }

    /**
     * create thing without udf
     */
    @Test
    public void example3() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "RFID0003";
        valueThings.add(createJSON("20170101080000", serialNumber, null));
        valueThings.add(createJSON("20170101090000", serialNumber, new Document("eNode", "XYZ")));
        valueThings.add(createJSON("20170101100000", serialNumber, new Document("eNode", "QWE")));
        valueThings.add(createJSON("20170101110000", serialNumber, new Document("eNode", "ABC")));
        valueThings.add(createJSON("20170101120000", serialNumber, new Document("status", "ACTIVE")));
        valueThings.add(createJSON("20170101140000", serialNumber, new Document("status", "INACTIVE").append("eNode", "DOOR")));
        valueThings.add(createJSON("20170101150000", serialNumber, new Document("eNode", "TEST")));
        valueThings.add(createJSON("20170101160000", serialNumber, new Document("eNode", "BASE")));
        valueThings.add(createJSON("20170101170000", serialNumber, new Document("status", "ACTIVE").append("eNode", "ENODEVAL")));
        valueThings.add(createJSON("20170101180000", serialNumber, new Document("eNode", "ABC")));
        executeCreateUpdateThing(valueThings);
    }

    /**
     * example of thing with two udf no timeseries
     */
    @Test
    public void example4() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "RFID0004";
        valueThings.add(createJSON("20170101080000", serialNumber, new Document("eNode", "ABC")));
        valueThings.add(createJSON("20170101090000", serialNumber, new Document("eNode", "XYZ")));
        valueThings.add(createJSON("20170101100000", serialNumber, new Document("eNode", "QWE")));
        valueThings.add(createJSON("20170101110000", serialNumber, new Document("eNode", "ABC")));
        valueThings.add(createJSON("20170101120000", serialNumber, new Document("eNode", "ABC")));
        valueThings.add(createJSON("20170101140000", serialNumber, new Document("eNode", "DOOR")));
        valueThings.add(createJSON("20170101150000", serialNumber, new Document("eNode", "TEST")));
        valueThings.add(createJSON("20170101160000", serialNumber, new Document("eNode", "BASE")));
        valueThings.add(createJSON("20170101170000", serialNumber, new Document("eNode", "ENODEVAL")));
        valueThings.add(createJSON("20170101180000", serialNumber, new Document("eNode", "ABC")));
        executeCreateUpdateThing(valueThings);
    }

    /**
     * example of thing with two udf no timeseries
     */
    @Test
    public void example5() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "RFID0005";
        valueThings.add(createJSON("20170101150000", serialNumber, new Document("status", "NONE")));
        valueThings.add(createJSON("20170101160000", serialNumber, new Document("eNode", "XYZ")));
        valueThings.add(createJSON("20170101170000", serialNumber, new Document("status", "ACTIVE")));
        valueThings.add(createJSON("20170101180000", serialNumber, new Document("status", "INACTIVE")));
        executeCreateUpdateThing(valueThings);
    }

    /**
     * test 1 QA create thing without udf
     */
    @Test
    public void example6() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "RFID0006";
        valueThings.add(createJSON("20170101010000", serialNumber, null));
        valueThings.add(createJSON("20170101020000", serialNumber, new Document("status", "c TS")));
        valueThings.add(createJSON("20170101030000", serialNumber, new Document("eNode", "c NTS")));
        valueThings.add(createJSON("20170101040000", serialNumber, new Document("status", "u TS")));
        valueThings.add(createJSON("20170101050000", serialNumber, new Document("eNode", "u NTS")));
        valueThings.add(createJSON("20170101060000", serialNumber, new Document("eNode", "u NTS 2")));
        executeCreateUpdateThing(valueThings);
    }

    /**
     * test 2 QA example gromero
     */
    @Test
    public void example7() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "RFID0007";
        valueThings.add(createJSON("20170101010000", serialNumber, null));
        valueThings.add(createJSON("20170101020000", serialNumber, new Document("status", "NONE")));
        valueThings.add(createJSON("20170101030000", serialNumber, new Document("eNode", "ABC")));
        valueThings.add(createJSON("20170101040000", serialNumber, new Document("eNode", "CCC")));
        valueThings.add(createJSON("20170101050000", serialNumber, new Document("eNode", "DDD")));
        valueThings.add(createJSON("20170101060000", serialNumber, new Document("status", "ACTIVE")));
        executeCreateUpdateThing(valueThings);
    }

    /**
     * test 2 QA example mlimachi
     */
    @Test
    public void example8() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "RFID0008";
        valueThings.add(createJSON("20170101010000", serialNumber, null));
        valueThings.add(createJSON("20170101020000", serialNumber, new Document("eNode", "ABC")));
        valueThings.add(createJSON("20170101030000", serialNumber, new Document("status", "ACTIVE")));
        valueThings.add(createJSON("20170101040000", serialNumber, new Document("status", "INACTIVE")));
        valueThings.add(createJSON("20170101050000", serialNumber, new Document("eNode", "XYZ")));
        valueThings.add(createJSON("20170101060000", serialNumber, new Document("eNode", "CCC").append("status", "SOLD")));
        valueThings.add(createJSON("20170101070000", serialNumber, new Document("status", "ACT2")));
        valueThings.add(createJSON("20170101080000", serialNumber, new Document("eNode", "DDD")));
        valueThings.add(createJSON("20170101090000", serialNumber, new Document("status", "ACT2")));
        executeCreateUpdateThing(valueThings);
    }

    /**
     * test 11 QA create thing without udf
     */
    @Test
    public void example9() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "RFID0009";
        String name = "RFID0009A";
        valueThings.add(createJSON("20170101010000", serialNumber, null));
        valueThings.add(createJSON("20170101020000", serialNumber, name, null)); // change name
        valueThings.add(createJSON("20170101030000", serialNumber, name, new Document("eNode", "c NTS")));
        valueThings.add(createJSON("20170101040000", serialNumber, name, new Document("eNode", "u NTS")));
        valueThings.add(createJSON("20170101050000", serialNumber, name, new Document("status", "c TS")));
        executeCreateUpdateThing(valueThings);
    }

    @Test
    public void example10() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "RFID00010B";
        valueThings.add(createJSON("20170101010000", serialNumber, null));
        executeCreateUpdateThing(valueThings);
    }

    /**
     * test QA example gtc
     * eNode and status no Time series
     */
    @Test
    public void example11() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "RFID00011D";
        valueThings.add(createJSON("20170101020000", serialNumber, new Document("status", "ACTIVE")));
        valueThings.add(createJSON("20170101030000", serialNumber, new Document("eNode", "ABC")));
        valueThings.add(createJSON("20170101040000", serialNumber, new Document("status", "INACTIVE")));

        valueThings.add(createJSON("20170101050000", serialNumber, new Document("eNode", "XYZ")));
        valueThings.add(createJSON("20170101060000", serialNumber, new Document("status", "NONE")));

        executeCreateUpdateThing(valueThings);
    }

    @Test
    public void example1Automation() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "OLDSER120000A";
        valueThings.add(createJSON("20170711104559", serialNumber, new Document("zone", "Enance")));
        valueThings.add(createJSON("20170711104609", serialNumber, new Document("zone", "Enance")));
        valueThings.add(createJSON("20170711104619", serialNumber, new Document("zone", "Enance")));
        valueThings.add(createJSON("20170711104629", serialNumber, new Document("zone", "Enance")));
        valueThings.add(createJSON("20170711104639", serialNumber, new Document("zone", "Po1")));
        valueThings.add(createJSON("20170711104614", serialNumber, new Document("zone", "Saloor")));
        executeCreateUpdateThing(valueThings);
    }

    @Test
    public void example2Automation() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "AUTO00002";
        valueThings.add(createJSON("20170714184000", serialNumber, new Document("zone", "Enance")));
        valueThings.add(createJSON("20170714184100", serialNumber, new Document("status", "NONE")));
        valueThings.add(createJSON("20170714184200", serialNumber, new Document("status", "ACTIVE")));
        executeCreateUpdateThing(valueThings);
    }

    @Test
    public void example4Automation() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "OLDCASE00039B";
        valueThings.add(createJSON("20170701080010", serialNumber, new Document("zone", "Enance")));
        valueThings.add(createJSON("20170701080020", serialNumber, new Document("zone", "Enance")));
        valueThings.add(createJSON("20170701080030", serialNumber, new Document("zone", "Enance")));
        valueThings.add(createJSON("20170701080000", serialNumber, new Document("zone", "Enance")));
        valueThings.add(createJSON("20170701080008", serialNumber, new Document("status", "AUTO")));
        valueThings.add(createJSON("20170701080005", serialNumber, new Document("zone", "Saloor")));
        executeCreateUpdateThing(valueThings);
    }

    @Test
    public void example3Automation() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "AUTO00003D";
        valueThings.add(createJSON("20170718163608", serialNumber, null));
        valueThings.add(createJSON("20170718163618", serialNumber, new Document("status", "AUTO")));
        valueThings.add(createJSON("20170718163628", serialNumber, new Document("status", "AUTO1")));
        executeCreateUpdateThing(valueThings);
    }

    @Test
    public void example5Automation() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "OLDSER000015B";
        valueThings.add(createJSON("20170701080010", serialNumber, new Document("zone", "Po1")));
        valueThings.add(createJSON("20170701080020", serialNumber, new Document("status", "set")));
        valueThings.add(createJSON("20170701080030", serialNumber, new Document("status", "set")));
        valueThings.add(createJSON("20170701080040", serialNumber, new Document("status", "set")));
        valueThings.add(createJSON("20170701080050", serialNumber, new Document("status", "set")));
        valueThings.add(createJSON("20170701080005", serialNumber, new Document("status", "set")));
        executeCreateUpdateThing(valueThings);
    }

    @Test
    public void exampleForPageSize() throws Exception {
        List<Document> valueThings = new ArrayList<>();
        String serialNumber = "PAGEEXAMPLE001";
        valueThings.add(createJSON("20170701080000", serialNumber, new Document("zone", "Enance")));
        valueThings.add(createJSON("20170702080000", serialNumber, new Document("zone", "Po1")));
        valueThings.add(createJSON("20170703080000", serialNumber, new Document("zone", "Saloor")));
        executeCreateUpdateThing(valueThings);


        valueThings = new ArrayList<>();
        serialNumber = "PAGEEXAMPLE002";
        valueThings.add(createJSON("20170701080000", serialNumber, new Document("zone", "Po1")));
        valueThings.add(createJSON("20170702080000", serialNumber, new Document("zone", "Enance")));
        valueThings.add(createJSON("20170703080000", serialNumber, new Document("zone", "Saloor")));
        executeCreateUpdateThing(valueThings);

        valueThings = new ArrayList<>();
        serialNumber = "PAGEEXAMPLE003";
        valueThings.add(createJSON("20170701080000", serialNumber, new Document("zone", "Saloor")));
        valueThings.add(createJSON("20170702080000", serialNumber, new Document("zone", "Enance")));
        valueThings.add(createJSON("20170703080000", serialNumber, new Document("zone", "Po1")));
        executeCreateUpdateThing(valueThings);
    }

    private void executeCreateUpdateThing(List<Document> value) throws Exception {
        if (value != null && !value.isEmpty()) {
            Long thingID = createThingReturnId(value.get(0));
            if (value.size() > 1) {
                updateThing(thingID, value.subList(1, value.size()));
            }
            createContentBlink(value);
        }
    }

    private void createContentBlink(List<Document> values) {
        if (!createBlink) return;
        System.out.println("---------------------------------------------------------------------------------------");
        System.out.println("#!/bin/bash\n");
        List<String> listBlinks = new ArrayList<>();
        values.stream().filter(value -> value.get("udfs") instanceof Document).forEach(value ->
        {
            System.out.print("echo \"sn," + sn);
            Document udfsValues = (Document) value.get("udfs");
            for (Map.Entry<String, Object> udf : udfsValues.entrySet()) {
                System.out.print("\n$1," + value.get("time") + "," + udf.getKey() + "," + ((Document) udf.getValue()).get("value"));
            }
            System.out.println("\" > blink" + sn + "   #  " + new Date(Long.valueOf(value.get("time").toString())) + "\n");
            listBlinks.add("mosquitto_pub -t /v1/data/ALEB/default_rfid_thingtype -f blink" + sn + "\n");
            sn++;
        });
        System.out.println(String.join("read -n1 -r -p \"Press any key to continue...\" key\n", listBlinks));
        System.out.println("---------------------------------------------------------------------------------------");
    }

    private Document createJSON(String isoDate, String serialName, Document udfMap) {
        return createJSON(isoDate, ">mojix>SM", serialName, serialName, "default_rfid_thingtype", udfMap);
    }

    private Document createJSON(String isoDate, String serialName, String name, Document udfMap) {
        return createJSON(isoDate, ">mojix>SM", serialName, name, "default_rfid_thingtype", udfMap);
    }

    private Document createJSON(String isoDate, String group, String serialName, String name, String thingTypeCode, Document udfMap) {
        Document document = new Document();
        document.put("time", getTS(isoDate));
        document.put("group", group);
        document.put("serialNumber", serialName);
        document.put("name", name);
        document.put("thingTypeCode", thingTypeCode);
        if (udfMap != null && !udfMap.isEmpty()) {
            Document udfDocument = new Document();
            for (Map.Entry<String, Object> udf : udfMap.entrySet()) {
                udfDocument.put(udf.getKey(), new Document("value", udf.getValue()));
            }
            document.put("udfs", udfDocument);
        }
        return document;
    }

    private Long createThingReturnId(Document valueThing) throws Exception {
        URI uri = getURI("thing?createRecent=true&ts=" + new Date().getTime());
        HttpPut httpPut = new HttpPut(uri);
        httpPut.setEntity(new StringEntity(valueThing.toJson(), ContentType.create("text/plain", "UTF-8")));
        String jsonThing = execute(httpPut);
        JSONObject outputConfig = (JSONObject) new JSONParser().parse(jsonThing);
        LOGGER.info("stop"); // for debug
        return (Long) ((JSONObject) outputConfig.get("thing")).get("id");
    }

    private void updateThing(Long thingID, List<Document> valueThings) throws Exception {
        URI uri = getURI("thing/" + thingID + "?createRecent=true&extend=parent.thingType&" +
                "extra=parent,group,group.groupType,thingType,thingType.children,thingType.parents&ts=" + new Date().getTime());
        HttpPatch httpPatch = new HttpPatch(uri);
        for (Document value : valueThings) {
            httpPatch.setEntity(new StringEntity(value.toJson(), ContentType.create("text/plain", "UTF-8")));
            execute(httpPatch);
            LOGGER.info("stop"); // for debug
        }
    }

    private String getTS(String dateString) {
        LocalDateTime localDateTime = LocalDateTime.parse(dateString, DateTimeFormatterHelper.DTF_SW_yyyyMMdd);
        Date date = Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
        return String.valueOf(date.getTime());
    }

    private URI getURI(String url) throws Exception {
        try {
            return new URI(URLBaseString + url);
        } catch (URISyntaxException e) {
            LOGGER.error(e);
            throw new Exception(e);
        }
    }

    private String execute(HttpRequestBase http) throws ExecuteActionException {
        http.addHeader("Api_key", apiKey);
        http.addHeader("Content-Type", "application/json");
        String response = null;
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse responseHttp = null;
        try {
            responseHttp = httpClient.execute(http);
            HttpEntity entity = responseHttp.getEntity();
            if (entity != null) {
                InputStream inputStream = entity.getContent();
                int statusCode = responseHttp.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    response = getInputStreamToString(inputStream);
                    LOGGER.info(response);
                } else {
                    LOGGER.error(getInputStreamToString(inputStream));
                }
            } else {
                LOGGER.warn("Response without content");
            }
        } catch (IOException e) {
            LOGGER.error(e);
        } finally {
            try {
                if (responseHttp != null) {
                    responseHttp.close();
                }
            } catch (IOException e) {
                LOGGER.error(e);
            }
        }
        return response;
    }

    private String getInputStreamToString(InputStream inputStream) throws ExecuteActionException {
        try {
            return IOUtils.toString(inputStream, Charsets.UTF_8);
        } catch (IOException e) {
            throw new ExecuteActionException("Error with transform response to string", e);
        }
    }
}
