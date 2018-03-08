package com.tierconnect.riot.iot.job;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.RiotMessage;
import com.tierconnect.riot.commons.RiotMessageBuilder;
import com.tierconnect.riot.commons.services.broker.MqttPublisher;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;
import org.quartz.*;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.tierconnect.riot.commons.ConnectionConstants.BRIDGE_STARTUP_OPTIONS;
import static com.tierconnect.riot.commons.ConnectionConstants.SCHEDULED_RULE_SERVICES_CONNECTION_CODE;
import static com.tierconnect.riot.commons.Constants.BRIDGE_CODE;

/**
 * Created by brayan on 6/22/17.
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class ScheduledRuleJob implements Job {
    private static Logger logger = Logger.getLogger(ScheduledRuleJob.class);

    private Long reportId;
    private URI uri;
    private long seqNum;
    private Long time;
    private MqttPublisher mqttPublisher;
    private UUID uuid;
    private String apikey;
    private String ruleExecutionMode;
    private long messageSize;
    private String reportType;
    private String bridgeCode;

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public String getUuid() {
        return uuid.toString();
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
        logger.info("ReportId: " + reportId);
    }

    public String getRuleExecutionMode() {
        return ruleExecutionMode;
    }

    public void setRuleExecutionMode(String ruleExecutionMode) {
        this.ruleExecutionMode = ruleExecutionMode;
        logger.info("RuleExecutionMode: " + ruleExecutionMode);
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // execution time
        time = System.currentTimeMillis();

        // set the session to get needed properties
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        String jobName = (String) jobDataMap.get("jobName");
        logger.info("[ScheduledRule] JobExecutionContext info: " + context.getFireTime() + " JobDetail JobName: " + jobName);

        setReportId(jobDataMap.getLong("reportId"));
        setRuleExecutionMode(jobDataMap.getString("ruleExecutionMode"));

        seqNum = (jobDataMap.get("seqNum") != null) ? jobDataMap.getLong("seqNum") : 1;
        String coreBridgeConf = jobDataMap.getString("coreBridgeConf");
        bridgeCode = jobDataMap.getString(BRIDGE_CODE);
        String extraConf = jobDataMap.getString("extraConf");
        this.uuid = UUID.randomUUID();

        reportType = jobDataMap.getString("reportType");

        try {
            setConnections(coreBridgeConf);
            setExtraConfiguration(extraConf);
        } catch (ParseException e) {
            throw new JobExecutionException("Unable to parse configurations Job: " + jobName, e);
        }

        try {
            executeJob();
        } catch (IOException e) {
            throw new JobExecutionException("Unable to execute ScheduledRule: " + jobName + " cause=", e);
        }
        seqNum++;
        jobDataMap.put("seqNum", seqNum);
        logger.info("[ScheduledRule] total time job execution: " + (System.currentTimeMillis() - time) );
    }

    /**
     * Execution of the Job with all variables initialized
     */
    public void executeJob() throws IOException {
        // execute endpoint
        CloseableHttpResponse response = executePost();
        // parse result
        JSONObject outputResponse = handleResponse(response);
        // build MQTT/KAFKA message
        Map<String, RiotMessageBuilder> messages;
        if (outputResponse != null) {
            messages = buildMessage(outputResponse);
            for (Map.Entry<String, RiotMessageBuilder> entry: messages.entrySet()){
                RiotMessageBuilder message = entry.getValue();
                String topic = Constants.SCHEDULED_RULE_DATA + bridgeCode + "/" + message.getThingTypeCode();
                message.buildAndPublish("CSV", mqttPublisher, topic);
            }
        }
    }

    /**
     * Fill the MQTT/KAFKA RiotMessageBuilder to send
     * @param outputResponse
     * @return
     * @throws UnknownHostException
     */
    private Map<String, RiotMessageBuilder> buildMessage(JSONObject outputResponse) {
        Map<String, RiotMessageBuilder> builderMap = new HashMap<>();

        // Verify the kind of report (switch)
        // create a method by each kind of report
        switch (reportType){
            case "map": case "table": case "tableTimeSeries":
                builderMap = buildDetailAndTimeSeriesMessage(outputResponse);
                break;
            case "mongo": case "tableConnection":
                builderMap = buildScriptMessage(outputResponse);
                break;
        }

        return builderMap;
    }

    /**
     * build a riotMessageCVS whit a map, table detail scheme validation
     * scheme for tableScript/tableConnection = {"totalRows":value, "columnNames":["thingTypeId", "serialNumber"], "data":[index, index]}
     * @param outputResponse
     * @return
     */
    private Map<String, RiotMessageBuilder> buildScriptMessage(JSONObject outputResponse) {
        Map<String, RiotMessageBuilder> builderMap = new HashMap<>();
        Map<Long, String> thingTypeCodeIdMap = new HashMap<>();

        if (!outputResponse.containsKey("totalRows")) {
            throw new UserException("It is not possible to get 'total' from report result...");
        }
        Long totalMessage = (Long) outputResponse.get("totalRows");

        if (totalMessage != null && totalMessage > 0) {
            if (!outputResponse.containsKey("data")){
                throw new UserException("It is not possible to get 'data' from report result...");
            }

            if (!outputResponse.containsKey("columnNames")){
                throw new UserException("It is not possible to get 'columnNames' from report result...");
            }

            // get indexes from names
            int indexOfThingTypeId;
            int indexOfSerialNumber;
            JSONArray JSONArrayColumnNames = (JSONArray) outputResponse.get("columnNames");
            if (!JSONArrayColumnNames.contains("thingTypeId")){
                throw new UserException("It is not possible to get 'thingTypeId' from report result...");
            }

            if (!JSONArrayColumnNames.contains("serialNumber")){
                throw new UserException("It is not possible to get 'serialNumber' from report result...");
            }
            indexOfThingTypeId = JSONArrayColumnNames.indexOf("thingTypeId");
            indexOfSerialNumber = JSONArrayColumnNames.indexOf("serialNumber");

            JSONArray JSONArrayResult = (JSONArray) outputResponse.get("data");
            Iterator<JSONArray> iterator = JSONArrayResult.iterator();
            while (iterator.hasNext()) {

                JSONArray JSONArrayResultRow = iterator.next();
                Long thingTypeId = (Long) JSONArrayResultRow.get(indexOfThingTypeId);

                // get the thinTypeCode from cache or dataBase
                String thingTypeCode = setThingTypeCodeById(thingTypeCodeIdMap, thingTypeId);

                // control if the thingTypeCode exists in builderMap
                controlRiotMessage(builderMap, thingTypeCode);

                RiotMessageBuilder riotMessageBuilder = builderMap.get(thingTypeCode);
                riotMessageBuilder.setSqn(seqNum);
                riotMessageBuilder.setRuleExecutionMode(getRuleExecutionMode());
                Map<String, RiotMessage.Property> propertyMap = riotMessageBuilder.getProperties();

                riotMessageBuilder.setThingTypeCode(thingTypeCode);
                String serialNumber = (String) JSONArrayResultRow.get(indexOfSerialNumber);
                addProperty(riotMessageBuilder, propertyMap, serialNumber);
            }
        }
        return builderMap;
    }

    /**
     * build a riotMessageCVS whit a map, table detail scheme validation
     * scheme for timeSeriesReportTable = {"total":value, "results":[{"thing.headers":{"thingTypeId":"value", "serialNumber":"value"}}]}
     * @param outputResponse
     * @return
     */
    private Map<String, RiotMessageBuilder> buildDetailAndTimeSeriesMessage(JSONObject outputResponse) {
        Map<String, RiotMessageBuilder> builderMap = new HashMap<>();
        Map<Long, String> thingTypeCodeIdMap = new HashMap<>();

        if (!outputResponse.containsKey("total")) {
            throw new UserException("It is not possible to get 'total' from report result...");
        }
        Long totalMessage = (Long) outputResponse.get("total");

        if (totalMessage != null && totalMessage > 0) {
            if (!outputResponse.containsKey("results")){
                throw new UserException("It is not possible to get 'results' from report result...");
            }
            JSONArray JSONArrayResult = (JSONArray) outputResponse.get("results");
            Iterator<JSONObject> iterator = JSONArrayResult.iterator();
            while (iterator.hasNext()) {
                JSONObject JSONResult = iterator.next();

                if (!JSONResult.containsKey("thing.headers")) {
                    throw new UserException("It is not possible to get 'thing.headers' from report result...");
                }

                JSONObject JSONThingHeaders = (JSONObject) JSONResult.get("thing.headers");

                if (!JSONThingHeaders.containsKey("thingTypeId")){
                    throw new UserException("It is not possible to get 'thingTypeId' from report result...");
                }
                Long thingTypeId = Long.valueOf ((String)JSONThingHeaders.get("thingTypeId"));

                // get the thinTypeCode from cache or dataBase
                String thingTypeCode = setThingTypeCodeById(thingTypeCodeIdMap, thingTypeId);

                // control if the thingTypeCode exists in builderMap
                controlRiotMessage(builderMap, thingTypeCode);

                RiotMessageBuilder riotMessageBuilder = builderMap.get(thingTypeCode);
                riotMessageBuilder.setSqn(seqNum);
                riotMessageBuilder.setRuleExecutionMode(getRuleExecutionMode());
                Map<String, RiotMessage.Property> propertyMap = riotMessageBuilder.getProperties();

                riotMessageBuilder.setThingTypeCode(thingTypeCode);

                // control if the "serialNumber" field exists in the response
                if (!JSONThingHeaders.containsKey("serialNumber")) {
                    throw new UserException("It is not possible to get 'serialNumber' from report result...");
                }
                String serialNumber = (String) JSONThingHeaders.get("serialNumber");
                addProperty(riotMessageBuilder, propertyMap, serialNumber);
            }
        }
        return builderMap;
    }

    private void addProperty(RiotMessageBuilder riotMessageBuilder, Map<String, RiotMessage.Property> propertyMap, String serialNumber){
        RiotMessage.Property property = new RiotMessage.Property();
        property.setName("source");
        property.setValue("REP_" + getReportId());
        property.setTime(time);
        propertyMap.put(serialNumber, property);
        riotMessageBuilder.setProperties(propertyMap);
    }

    /**
     * Set all configurations for all connections required
     *
     * @param coreBridgeConf
     * @throws ParseException
     */
    private void setConnections(String coreBridgeConf) throws ParseException {
        JSONObject coreBridgeConfObject = (JSONObject) new JSONParser().parse(coreBridgeConf);
        // set mqttConnection
        if (coreBridgeConfObject.containsKey("mqtt")){
            JSONObject mqttObject = (JSONObject) new JSONParser().parse(coreBridgeConfObject.get("mqtt").toString());
            setMqttConnection(mqttObject);
        } else {
            throw new UserException("It is not possible to execute the Job, because the coreBridge doesn't have a 'mqtt' connection");
        }

        // set restConnection
        if (coreBridgeConfObject.containsKey(BRIDGE_STARTUP_OPTIONS)){
            JSONObject bridgeStartupOptions = (JSONObject) new JSONParser().parse(coreBridgeConfObject.get(BRIDGE_STARTUP_OPTIONS).toString());
            String servicesConnectionCode = (String) bridgeStartupOptions.get(SCHEDULED_RULE_SERVICES_CONNECTION_CODE);
            setRestConnection(servicesConnectionCode);
        } else {
            throw new UserException("It is not possible to execute the Job, because the coreBridge doesn't have a valid '"+SCHEDULED_RULE_SERVICES_CONNECTION_CODE+"' connection");
        }

        // TODO: set kafkaConnection if kafkaEnabled is true
    }

    private void setExtraConfiguration(String extraConf) throws ParseException {
        if (extraConf!=null){
            JSONObject extraConfObject = (JSONObject) new JSONParser().parse(extraConf);
            // setting messageSize (50 by default if doesn't exists)
            if (extraConfObject.containsKey("messageSize")){
                this.messageSize = (Long) extraConfObject.get("messageSize");
            } else {
                this.messageSize = Constants.SCHEDULED_RULE_MESSAGE_SIZE;
            }
        } else {
            this.messageSize = Constants.SCHEDULED_RULE_MESSAGE_SIZE;
        }
    }

    /**
     * Get the thingTypeCode once from DataBase or cache
     * @param thingTypeCodeIdMap
     * @param thingTypeId
     * @return
     */
    private String setThingTypeCodeById(Map<Long, String> thingTypeCodeIdMap, Long thingTypeId){
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        transaction.begin();
        String thingTypeCode = null;
        try {
            if (thingTypeCodeIdMap.containsKey(thingTypeId)){
                thingTypeCode = thingTypeCodeIdMap.get(thingTypeId);
            } else {
                ThingType thingType = ThingTypeService.getInstance().get(thingTypeId);
                thingTypeCode = thingType.getCode();
                thingTypeCodeIdMap.put(thingTypeId, thingTypeCode);
            }
        } catch (UserException e){
            logger.error(e.getMessage(), e);
            HibernateDAOUtils.rollback(transaction);
        }
        transaction.commit();
        return thingTypeCode;
    }

    /**
     * Set the mqttPublisher based on mqttConfiguration
     * @param mqttObject
     */
    private void setMqttConnection(JSONObject mqttObject) {
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        transaction.begin();
        try {
            if (mqttObject.containsKey("connectionCode")) {
                String mqttConnectionCode = (String) mqttObject.get("connectionCode");
                Connection mqttConnection = ConnectionService.getInstance().getByCode(mqttConnectionCode);
                Map<String, Object> properties = mqttConnection.getPropertiesMap();

                String host = String.valueOf(properties.get("host"));
                int port = Integer.parseInt(String.valueOf(properties.get("port")));
                int qos = Integer.parseInt(String.valueOf(properties.get("qos")));

                String mqttUsername = null;
                String mqttPassword = null;
                if (properties.containsKey("username")) {
                    mqttUsername = (String) properties.get("username");
                }
                if (properties.containsKey("password")) {
                    mqttPassword = mqttConnection.getPassword(false);
                }
                mqttPublisher = new MqttPublisher("pub-" + uuid, host, port, qos, mqttUsername, mqttPassword);
            }
        } catch (UserException e) {
            HibernateDAOUtils.rollback(transaction);
            throw new UserException("Is not possible to set MQTT configuration", e);
        }
        transaction.commit();
    }

    /**
     * Build the Rest URI based on restConfiguration
     * @param servicesConnectionCode
     */
    private void setRestConnection(String servicesConnectionCode) {
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        transaction.begin();
        try {
            if (!servicesConnectionCode.isEmpty()) {
                Connection restConnection = ConnectionService.getInstance().getByCode(servicesConnectionCode);
                Map<String, Object> properties = restConnection.getPropertiesMap();

                String host = String.valueOf(properties.get("host"));
                int port = Integer.parseInt(String.valueOf(properties.get("port")));
                String contextPath = "";
                logger.info("reportType: " + reportType);
                switch (reportType){
                    case "map": case "table":
                        contextPath = String.valueOf(properties.get("contextpath")) + "/api/reportExecution/" + getReportId();
                        break;
                    case "mongo": case "tableConnection":
                        contextPath = String.valueOf(properties.get("contextpath")) + "/api/reportExecution/mongo/" + getReportId();
                        break;
                    case "tableTimeSeries":
                        contextPath = String.valueOf(properties.get("contextpath")) + "/api/reportExecution/timeSeriesReportTable/" + getReportId();
                        break;
                }
                Boolean secure = Boolean.valueOf(String.valueOf(properties.get("secure")));
                apikey = String.valueOf(properties.get("apikey"));
                String scheme = (secure) ? "https" : "http";

                // build URI
                uri = buildURI(scheme, host, port, contextPath);
            }
        } catch (UserException e) {
            HibernateDAOUtils.rollback(transaction);
            throw new UserException("Is not possible to set REST configuration", e);
        } catch (URISyntaxException e) {
            throw new UserException("Is not possible to set the URI Rest", e);
        }
        transaction.commit();
    }

    /**
     * TODO: Set the kafka configuration if is kafkaEnabled is true
     */

    /**
     * Control the modified fields into the map
     * @param builderMap
     * @param thingTypeCode
     */
    private void controlRiotMessage(Map<String, RiotMessageBuilder> builderMap, String thingTypeCode){
        if (!builderMap.containsKey(thingTypeCode)){
            RiotMessageBuilder riotMessageBuilder = new RiotMessageBuilder(this.messageSize);
            Map<String, RiotMessage.Property> propertyMap = new HashMap<>();
            riotMessageBuilder.setSpecName("SCHEDULED");
            riotMessageBuilder.setBridgeCode("APP2");
            riotMessageBuilder.setProperties(propertyMap);
            builderMap.put(thingTypeCode, riotMessageBuilder);
        }
    }

    private JSONObject handleResponse(CloseableHttpResponse response) {
        try {
            logger.debug("response_status=" + response.getStatusLine());
            logger.debug("getting content");
            InputStream is = response.getEntity().getContent();

            logger.debug("parsing...");
            long t1 = System.currentTimeMillis();

            InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null) {
                logger.debug("lineRead='" + line + "'");
                JSONObject outputResponse = (JSONObject) new JSONParser().parse(line);
                long t2 = System.currentTimeMillis();
                logger.debug("done parsing delt=" + (t2 - t1));

                return outputResponse;
            }
        }
        catch( Exception e ) {
            logger.warn("exception handling response: ", e);
        }
        finally {
            try {
                response.close();
            } catch (Exception e) {
                logger.warn("exception closing response: ", e);
            }
        }
        return null;
    }

    /**
     * Executes the POST method execution
     * @return Returns the response from execution
     * @throws IOException
     */
    private CloseableHttpResponse executePost() throws IOException {
        CloseableHttpClient client = getClient();
        HttpEntityEnclosingRequestBase request = new HttpPost(uri);
        addHeaders(request);
        setBody(request);
        return executeAPIReport(client, request);
    }

    /**
     * Set the JSON body to send (empty)
     * @param request
     */
    private void setBody(HttpEntityEnclosingRequestBase request) {
        StringEntity entity = new StringEntity("{}", ContentType.create("application/json", "UTF-8"));
        request.setEntity(entity);
    }

    /**
     * Executes the rest endpoint call
     * @param client Default client ro execute the call
     * @param request API rest execution
     * @return
     * @throws IOException
     */
    private CloseableHttpResponse executeAPIReport(CloseableHttpClient client, HttpEntityEnclosingRequestBase request)
            throws IOException {
        long t1 = System.currentTimeMillis();

        CloseableHttpResponse response = client.execute(request);

        long t2 = System.currentTimeMillis();
        logger.info("got response delt=" + (t2 - t1));

        return response;
    }

    /**
     * Return a default HttpClient
     * @return
     */
    private CloseableHttpClient getClient() {
        return HttpClients.createDefault();
    }

    /**
     * Build the path rest endpoint execution
     * @return URI already built
     * @throws URISyntaxException
     * @param scheme
     * @param host
     * @param port
     * @param contextPath
     */
    private URI buildURI(String scheme, String host, int port, String contextPath) throws URISyntaxException {
        URIBuilder ub = new URIBuilder();
        ub.setScheme(scheme);
        ub.setHost(host);
        ub.setPort(port);
        ub.setPath(contextPath);
        ub.setParameter("export", "false");
        ub.setParameter("thingTypeUdfAsObject", "false");
        if (!(reportType.equals("mongo") || reportType.equals("tableConnection"))) {
            ub.setParameter("pageSize", "-1");
        }
        return ub.build();
    }

    private void addHeaders(HttpRequestBase request) {
        request.addHeader("api_key", apikey);
    }
}
