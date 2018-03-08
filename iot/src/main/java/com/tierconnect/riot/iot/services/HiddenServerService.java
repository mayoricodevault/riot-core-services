package com.tierconnect.riot.iot.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.appcore.utils.MlConfiguration;
import com.tierconnect.riot.iot.utils.rest.SparkRestClient;
import com.tierconnect.riot.iot.entities.JobServer;
import com.tierconnect.riot.iot.entities.exceptions.MLModelException;
import com.tierconnect.riot.iot.utils.rest.RestCallException;
import com.tierconnect.riot.iot.utils.rest.RestClient;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.log4j.Logger;

/**
 * Created by ariel on 11/18/16.
 */
public class HiddenServerService implements JobServer {

    private final static Logger logger = Logger.getLogger(HiddenServerService.class);
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String EXTRACTOR_APP_NAME = "ExtractorApp";
    private static final String TRAINER_APP_NAME = "TrainerApp";
    private static final String PREDICTOR_APP_NAME = "PredictorApp";
    private static final String PORT_CORE_SERVICES = "8080";
    static final String HOST_CORE_SERVICES;

    static {
        String tmp;
        try {
            tmp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            tmp = "localhost";
        }
        HOST_CORE_SERVICES = tmp;
    }

    @Override
    public Map<String, String> extract(LocalDate start, LocalDate end, String groupId, String collection,
                                       List<String> predictors, String sparkExtractionId,
                                       String appName) throws MLModelException {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        String responseUUID = UUID.randomUUID().toString();
        Map<String, Object> mongoMap = new HashMap<>();
        mongoMap.put("host", MlConfiguration.property("mongo.host"));
        mongoMap.put("port", MlConfiguration.property("mongo.port"));
        mongoMap.put("db", MlConfiguration.property("mongo.db"));
        mongoMap.put("username", MlConfiguration.property("mongo.username"));
        mongoMap.put("password", MlConfiguration.property("mongo.password"));

        Map<String, Object> servicesMap = new HashMap<>();
        servicesMap.put("keyapi", UserService.getInstance().getRootUser().getApiKey());


        // ******* Parameters
        // arguments
        Map<String, Object> appArgs = new HashMap<>();
        appArgs.put("startDate", start.format(formatter));
        appArgs.put("endDate", end.format(formatter));
        appArgs.put("collection", collection);
        appArgs.put("groupId", groupId);
        appArgs.put("uuid", sparkExtractionId);
        appArgs.put("callbackURI",
                "http://" + HOST_CORE_SERVICES + ":" + PORT_CORE_SERVICES +
                        "/riot-core-services/api/modelExtractions/"
                        + sparkExtractionId);
        appArgs.put("callbackURIResponse",
                "http://" + HOST_CORE_SERVICES + ":" + PORT_CORE_SERVICES +
                        "/riot-core-services/api/modelResponses/" + responseUUID);
        appArgs.put("synchronized",true);
        appArgs.put("predictors", predictors);
        appArgs.put("jobName", EXTRACTOR_APP_NAME);
        appArgs.put("extractionsPath", MlConfiguration.property("extractions.path"));
        appArgs.put("mongo", mongoMap);
        appArgs.put("riotcoreservices", servicesMap);
        appArgs.put("clusterMode", MlConfiguration.property("clusterMode"));

        Map<String, String> extractResult = new HashedMap();
        try {
            logger.info("--> EXTRACTION");
            logger.info("application Name    : " + appName);
            logger.info("application params  : " + appArgs);

            String appArgsString = new ObjectMapper().writeValueAsString(appArgs);

            List<String> appArgsList = Arrays.asList(appArgsString);

            SparkRestClient client = new SparkRestClient();

            Response rh = new Response();
            client.startSparkApplication(appArgsList, rh, appName.substring(4));

            logger.info("RESPONSE " + rh.toMap());
            Map responseMap = (Map) rh.toMap();
            if (String.valueOf(responseMap.get("success")).equals("true")) {
                Map<String, Object> validation = new ResponseReader(responseUUID).read();
                String status = String.valueOf(validation.get("status"));
                extractResult.put("status", status);
                extractResult.put("jobId", responseUUID);
                if ("ERROR".equals(status)) {
                    logger.error(validation.get("statusMessage"));
                } else {
                    logger.info("Extraction started");
                }
            }

        } catch (InterruptedException | IOException e) {
            logger.error(e);
            throw new MLModelException(e.getMessage(), e);
        }
        return extractResult;
    }

    @Override
    public Map<String, String> train(String extractionUUID, String uuid) throws MLModelException {
        String responseUUID = UUID.randomUUID().toString();
        Map<String, Object> servicesMap = new HashMap<>();
        servicesMap.put("keyapi", UserService.getInstance().getRootUser().getApiKey());

        // ******* Parameters
        // arguments
        Map<String, Object> appArgs = new HashMap<>();
        appArgs.put("extractionUUID", extractionUUID);
        appArgs.put("uuid", uuid);
        appArgs.put("callbackURI",
                "http://" + HOST_CORE_SERVICES + ":" + PORT_CORE_SERVICES + "/riot-core-services/api/modelTrainings/" + uuid);
        appArgs.put("callbackURIResponse",
                "http://" + HOST_CORE_SERVICES + ":" + PORT_CORE_SERVICES +
                        "/riot-core-services/api/modelResponses/" + responseUUID);
        appArgs.put("synchronized",true);
        appArgs.put("jobName", TRAINER_APP_NAME);
        appArgs.put("extractionsPath", MlConfiguration.property("extractions.path"));
        appArgs.put("trainingsPath", MlConfiguration.property("trainings.path"));
        appArgs.put("riotcoreservices", servicesMap);
        appArgs.put("clusterMode", MlConfiguration.property("clusterMode"));


        Map<String, String> trainResult = new HashedMap();
        String appName = TRAINER_APP_NAME;
        try {
            logger.info("--> TRAINING");
            logger.info("application Name    : " + appName);
            logger.info("application params  : " + appArgs);

            String appArgsString = new ObjectMapper().writeValueAsString(appArgs);
            List<String> appArgsList = Arrays.asList(appArgsString);

            SparkRestClient client = new SparkRestClient();
            Response rh = new Response();
            client.startSparkApplication(appArgsList, rh, appName);

            logger.info("RESPONSE " + rh.toMap());
            Map responseMap = (Map) rh.toMap();
            if (String.valueOf(responseMap.get("success")).equals("true")) {
                responseMap = new ResponseReader(responseUUID).read();
                String status = String.valueOf(responseMap.get("status"));
                trainResult.put("status", String.valueOf(responseMap.get("status")));
                trainResult.put("jobId",  responseUUID);
                if ("ERROR".equals(status)) {
                    logger.error(responseMap.get("statusMessage"));
                } else {
                    logger.info("Training started");
                }

            }


        } catch (InterruptedException | IOException e) {
            logger.error(e);
            throw new MLModelException(e.getMessage(), e);
        }

        return trainResult;
    }

    @Override
    public Map<String, Object> predict(String trainingId, String name,
                                       Map<String, String> predictorParams) throws MLModelException {
        String responseUUID = UUID.randomUUID().toString();
        List<String> predictorsList =
                Arrays.asList(predictorParams.get("predictors").replace("[", "").replace("]", "").split(","));
        Map<String, Object> servicesMap = new HashMap<>();
        servicesMap.put("keyapi", UserService.getInstance().getRootUser().getApiKey());

        // Parameters
        Map<String, Object> appArgs = new HashMap<>();
        appArgs.put("trainingUUID", trainingId);
        appArgs.putAll(predictorParams);
        appArgs.put("jobName", PREDICTOR_APP_NAME);
        appArgs.put("responseUUID", responseUUID);
        appArgs.put("callbackURIResponse",
                "http://" + HOST_CORE_SERVICES + ":" + PORT_CORE_SERVICES +
                        "/riot-core-services/api/modelResponses/" + responseUUID);
        appArgs.put("synchronized", false);
        appArgs.put("predictors", predictorsList);
        appArgs.put("trainingsPath", MlConfiguration.property("trainings.path"));
        appArgs.put("riotcoreservices", servicesMap);
        appArgs.put("clusterMode", MlConfiguration.property("clusterMode"));


        Map<String, Object> responseMap;
        String appName = PREDICTOR_APP_NAME;

        try {
            String appArgsString = new ObjectMapper().writeValueAsString(appArgs);
            List<String> appArgsList = Arrays.asList(appArgsString);
            SparkRestClient client = new SparkRestClient();
            Response rh = new Response();
            client.startSparkApplication(appArgsList, rh, appName);
            responseMap = (Map) rh.toMap();
            if (String.valueOf(responseMap.get("success")).equals("true")) {
                responseMap = (Map<String, Object>) new ResponseReader(responseUUID).read().get("result");
            }

        } catch (IOException | InterruptedException e) {
            logger.error(e);
            throw new MLModelException(e.getMessage(), e);
        }

        return responseMap;
    }


    public static class Response implements RestClient.ResponseHandler {
        private String response;

        @Override
        public void success(InputStream is) throws IOException, RestCallException {
            //          TODO: manage response in the handler!!!!!
            response = IOUtils.toString(is);
            logger.info("======> SUCCESS RESPONSE:  " + response);
            ObjectMapper mapper = new ObjectMapper();
            Map error = mapper.readValue(response, HashMap.class);

            if ("ERROR".equals(error.get("status"))) {
                logger.error(response);
                String message = (String) ((Map) error.get("result")).get("message");
                throw new RestCallException(message);
            }
        }

        @Override
        public void error(InputStream is) throws IOException, RestCallException {
            ObjectMapper mapper = new ObjectMapper();
            String errorResponse = IOUtils.toString(is);
            logger.error("error response " + errorResponse);
            Map error = mapper.readValue(errorResponse, HashMap.class);
            String message;
            Object result = error.get("result");
            if (result instanceof Map) {
                Map resultMap = (Map) result;
                message = (String) resultMap.get("message");
            } else {
                message = (String) result;
            }

            throw new RestCallException(message);
        }

        public String toString() {
            //this is a hack to parse a escaped json
            //todo upgrade to commons-lang 3.2 and use StringScapeUtils.unescapeJson
            return response.replace("\\", "").replace("\"{", "{").replace("}\"", "}");
        }

        public Map<String, Object> toMap() throws IOException {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(toString(), HashMap.class);
        }
    }


    private class ResponseReader {
        private final String path;
        private final String fileName;
        private final Integer timeout;

        public ResponseReader(String responseUUID) {
            // this is separated cause path can be in hadoop and Java.file.io is not used
            this.path = MlConfiguration.property("responses.path");
            this.fileName = responseUUID + ".json";
            this.timeout = Integer.parseInt(MlConfiguration.property("responseTimeout"));
        }

        public Map<String, Object> read() throws InterruptedException, IOException {
            HashMap result = null;
            File f = new File(path, fileName);
            for (int i = 0; i < timeout; i++) {
                Thread.sleep(1000);
                /* find file */
                if (Arrays.asList(f.getParentFile().listFiles()).contains(f)) {
                    // if(success)
                    ObjectMapper mapper = new ObjectMapper();
                    result = mapper.readValue(f, HashMap.class);
                    logger.info("File found after " + (i) + " seconds.");
                    break;
                }
            }

            if(result == null){
                throw new FileNotFoundException("Response from spark cluster has not been received after " + timeout + " seconds");
            }

//            if("ERROR".equals(String.valueOf(result.get("status"))) ){
//                String msg = String.valueOf(result.get("statusMessage"));
//                throw new IOException("Job reported an error. " + msg);
//            }
            return result;
        }
    }
}
