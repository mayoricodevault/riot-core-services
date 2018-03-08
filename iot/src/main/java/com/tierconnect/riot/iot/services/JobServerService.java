package com.tierconnect.riot.iot.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.iot.entities.JobServer;
import com.tierconnect.riot.iot.entities.exceptions.MLModelException;
import com.tierconnect.riot.appcore.utils.MlConfiguration;
import com.tierconnect.riot.iot.utils.rest.RestCallException;
import com.tierconnect.riot.iot.utils.rest.RestClient;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.time.LocalDate;

/**
 * Created by pablo on 6/21/16.
 * communicates with the job server
 */
public class JobServerService implements JobServer {

    static Logger logger = Logger.getLogger(JobServerService.class);

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String HOST_SPARK_JOB_SERVER = MlConfiguration.property("spark.host");
    private static final String PORT_SPARK_JOB_SERVER = MlConfiguration.property("spark.port");

    static {
        logger.info("spark.host: " + HOST_SPARK_JOB_SERVER);
        logger.info("spark.port: " + PORT_SPARK_JOB_SERVER);
    }


    private static final String PORT_CORE_SERVICES = "8080";
    private static final String HOST_CORE_SERVICES;

    static {
        String tmp;
        try {
            tmp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            tmp = "localhost";
        }
        HOST_CORE_SERVICES = tmp;
    }


    public Map<String, String> extract(LocalDate start, LocalDate end, String groupId, String collection,
                                       List<String> predictorsMappings,
                                       String sparkExtractionId, String appName) throws MLModelException {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);

        Map<String, String> params = new HashMap<>();

        params.put("startDate", start.format(formatter));
        params.put("endDate", end.format(formatter));
        params.put("collection", collection);
        params.put("groupId", groupId);
        params.put("uuid", sparkExtractionId);
        params.put("callbackURI",
                "\"http://" + HOST_CORE_SERVICES + ":" + PORT_CORE_SERVICES + "/riot-core-services/api/modelExtractions/" + sparkExtractionId + "\"");

        params.put("predictors", predictorsMappings.toString());

        logger.info("appName: " + appName);
        logger.info("spark server params: " + params);

        Map<String, String> extractResult = new HashedMap();
        try {
            RestClient client = RestClient.instance();

            com.tierconnect.riot.iot.services.JobServerService.Response rh = new com.tierconnect.riot.iot.services.JobServerService.Response();
            //todo parameterize the server, class.
            client.post(
                    new URI("http://" + HOST_SPARK_JOB_SERVER + ":" + PORT_SPARK_JOB_SERVER +
                            "/jobs?appName=" + appName + "&classPath=com.tierconnect.riot.ml.extraction.ExtractorJob"),
                    params,
                    rh
            );

            logger.info("RESPONSE " + rh.toMap());
            Map responseMap = (Map) rh.toMap();
            extractResult.put("status", (String) responseMap.get("duration"));
            extractResult.put("jobId", (String) responseMap.get("jobId"));

        } catch (URISyntaxException | RestCallException | IOException e) {
            logger.error(e);
            throw new MLModelException(e.getMessage(), e);
        }

        return extractResult;
    }


    public Map<String, String> train(String extractionUUID, String uuid) throws MLModelException {

        Map<String, String> params = new HashMap<>();

        params.put("extractionUUID", extractionUUID);
        params.put("uuid", uuid);
        params.put("callbackURI",
                "\"http://" + HOST_CORE_SERVICES + ":" + PORT_CORE_SERVICES + "/riot-core-services/api/modelTrainings/" + uuid + "\"");


        Map<String, String> trainResult = new HashedMap();
        try {
            RestClient client = RestClient.instance();

            Response rh = new Response();
            //todo parameterize the server, class.
            String url = "http://" + HOST_SPARK_JOB_SERVER + ":" + PORT_SPARK_JOB_SERVER + "" +
                    "/jobs?appName=trainJob&classPath=com.tierconnect.riot.ml.training.TrainerJob";

            logger.info("Job server call: " + url + " , " + params);
            client.post(
                    new URI(url),
                    params,
                    rh
            );

            logger.info("RESPONSE " + rh.toMap());
            Map responseMap = (Map) rh.toMap();
            trainResult.put("status", (String) responseMap.get("status"));
            trainResult.put("jobId", (String) responseMap.get("jobId"));

        } catch (URISyntaxException | RestCallException | IOException e) {
            logger.error(e);
            throw new MLModelException(e.getMessage(), e);
        }

        return trainResult;
    }

    //todo remove just inserted for demo
    public Map<String, String> train(Date start, Date end, String collection,
                                     String modelAlgorithm, String extractorClass,
                                     List<String> predictors, String appName) throws MLModelException {

        Map<String, String> params = new HashMap<>();
        params.put("start", "2016-03-01");
        params.put("end", "2016-03-05");
        params.put("collection", collection);
        params.put("modelAlgorithm", modelAlgorithm);
        params.put("extractorClass", extractorClass);
        //todo how do we pass the predictors?

        Map<String, String> trainResult = new HashedMap();
        try {
            RestClient client = RestClient.instance();

            Response rh = new Response();
            //todo parameterize the server, class.
            client.post(
                    new URI("http://" + HOST_SPARK_JOB_SERVER + ":" + PORT_SPARK_JOB_SERVER +
                            "/jobs?appName=" + appName + "&classPath=com.tierconnect.riot.ml.training.TrainerJob2"),
                    params,
                    rh
            );

            logger.info("RESPONSE " + rh.toMap());
            Map responseMap = (Map) rh.toMap();
            trainResult.put("status", (String) responseMap.get("status"));
            trainResult.put("jobId", (String) responseMap.get("jobId"));

        } catch (URISyntaxException | RestCallException | IOException e) {
            logger.error(e);
            throw new MLModelException(e.getMessage(), e);
        }

        return trainResult;
    }


    public Map<String, String> extractAndTrain(LocalDate start, LocalDate end, String collection,
                                               String modelName, Map<String, String> predictorsMappings,
                                               String sparkExtrTrainId, Long groupId, String appName) throws MLModelException {


        Map<String, String> params = new HashMap<>();

//        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
// TODO chech this date formatting - missing to define a uniform procedure
        params.put("startDate", start.toString());
        params.put("endDate", end.toString());
        params.put("collection", collection);
        params.put("modelName", modelName);
        params.put("sparkExtrTrainId", sparkExtrTrainId);
        params.put("groupId", groupId.toString());
        params.put("callbackURI",
                "\"http://" + HOST_CORE_SERVICES + ":" + PORT_CORE_SERVICES + "/riot-core-services/api/MlModels/" + sparkExtrTrainId + "\"");


        params.put("numPredictors", predictorsMappings.size() + "");
        for (Map.Entry<String, String> entry : predictorsMappings.entrySet()) {
            params.put("predictorName", entry.getKey());
            params.put("predictorMapping", entry.getValue());
        }

        logger.info("spark server params: " + params);

        Map<String, String> response = new HashedMap();
        try {
            RestClient client = RestClient.instance();

            com.tierconnect.riot.iot.services.JobServerService.Response rh = new com.tierconnect.riot.iot.services.JobServerService.Response();
            //todo parameterize the server, class.
            client.post(
                    new URI("http://" + HOST_SPARK_JOB_SERVER + ":" + PORT_SPARK_JOB_SERVER +
                            "/jobs?appName=" + appName + "&classPath=com.tierconnect.riot.ml.training.ExtractorTrainerJob"),
                    params,
                    rh
            );

            logger.info("RESPONSE " + rh.toMap());
            Map responseMap = (Map) rh.toMap();
            response.put("status", (String) responseMap.get("status"));
            response.put("jobId", (String) responseMap.get("jobId"));

        } catch (URISyntaxException | RestCallException | IOException e) {
            logger.error(e);
            throw new MLModelException(e.getMessage(), e);
        }

        return response;
    }


    public Map<String, Object> predict(String trainingId, String name, Map<String, String> predictorParams) throws MLModelException {
        Map<String, String> params = new HashMap<>();

        params.put("trainingUUID", trainingId);
        params.putAll(predictorParams);

        Map<String, Object> responseMap;

        try {
            RestClient client = RestClient.instance();

            Response rh = new Response();
            String URL = "http://" + HOST_SPARK_JOB_SERVER + ":" + PORT_SPARK_JOB_SERVER +
                    "/jobs?appName=predictApp&classPath=com.tierconnect.riot.ml.prediction.PredictorJob&timeout=20000&sync=true";

            logger.info("Prediction Job server call:  [ url=" + URL + ", params=" + params + "]");

            client.post(
                    new URI(URL),
                    params,
                    rh
            );
            responseMap = (Map) rh.toMap().get("result");
        } catch (URISyntaxException | RestCallException | IOException e) {
            logger.error(e);
            throw new MLModelException(e.getMessage(), e);
        }

        return responseMap;

    }


    public String checkStatus(String jobId) throws MLModelException {
        String status;
        try {
            RestClient client = RestClient.instance();

            Response rh = new Response();
            //todo parameterize the server, class.
            client.get(
                    new URI("http://" + HOST_SPARK_JOB_SERVER + ":" + PORT_SPARK_JOB_SERVER + "/jobs/" + jobId),
                    rh
            );

            logger.info("RESPONSE " + rh.toMap());
            Map responseMap = (Map) rh.toMap();
            status = (String) responseMap.get("status");

        } catch (URISyntaxException | RestCallException | IOException e) {
            logger.error(e);
            throw new MLModelException(e.getMessage(), e);
        }

        return status;
    }


    // TODO this method is the checkstatus for extraction and training but probably we could replace the other one?
    public JobStatus newCheckStatus(String jobId) throws MLModelException {
        JobStatus status = null;
        try {
            RestClient client = RestClient.instance();

            Response rh = new Response();
            //todo parameterize the server, class.
            client.get(
                    new URI("http://" + HOST_SPARK_JOB_SERVER + ":" + PORT_SPARK_JOB_SERVER + "/jobs/" + jobId),
                    rh
            );

            logger.info("RESPONSE " + rh.toMap());
            Map responseMap = (Map) rh.toMap();
            status = JobStatus.parseJobStatus((String) responseMap.get("status"));

        } catch (URISyntaxException | RestCallException | IOException e) {
            logger.error(e);
            throw new MLModelException(e.getMessage());
        }

        return status;
    }


    public static class Response implements RestClient.ResponseHandler {
        private String response;

        @Override
        public void success(InputStream is) throws IOException, RestCallException {
            response = IOUtils.toString(is);

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

    private String predictorsParameterBuild(Map<String, String> predictorsMappings) {

//        * predictors=[
//              time|time.YEAR|true,
//              time|time.MONTH|false,
//              time|time.WEEK_OF_YEAR|true,
//              value.serialNumber|serial|false,
//              value.status.value|sold|false|timeSoldStage,
//              value.zone.value.name|zone|true|zoneStage
//              ]"

        // TODO: hardcoded : addition of fields of time. define where they should come
        predictorsMappings.put("time.YEAR", "time");
        predictorsMappings.put("time.MONTH", "time");
        predictorsMappings.put("time.WEEK_OF_YEAR", "time");
        predictorsMappings.put("serial", " value.serialNumber");
        // TODO: hardcoded: addition of stages. It should come from ui
        Map<String, String> stagesMappings = new LinkedHashMap<>();
        stagesMappings.put("zone", "zoneStage");
        stagesMappings.put("status", "timeSoldStage");


        // Addition of fields
        List<String> predictors = new ArrayList<>();
        for (Map.Entry<String, String> entry : predictorsMappings.entrySet()) {
            String predictor = entry.getValue();
            predictor = predictor.concat("|");
            predictor = predictor.concat(entry.getKey());
            predictor = predictor.concat("|");
            predictor = predictor.concat("true");
            if (stagesMappings.containsKey(entry.getKey())) {
                predictor = predictor
                        .concat("|")
                        .concat(stagesMappings.get(entry.getKey()));
            }
            predictors.add(predictor);
        }

        // building process
        String predictorsParameter = "[";
        for (String p : predictors) {
            predictorsParameter = predictorsParameter.concat(p).concat(",");
        }
        predictorsParameter = predictorsParameter.substring(0, predictorsParameter.length() - 1).concat("]");
        return predictorsParameter;
    }

}
