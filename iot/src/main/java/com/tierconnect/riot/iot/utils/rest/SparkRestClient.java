package com.tierconnect.riot.iot.utils.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.utils.MlConfiguration;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SparkRestClient class.
 * <p>
 * Manages Jobs from jar files obtained from ???(hadoop or local harddisk -- IMPLEMENTATION) via Spark Hidden Rest Api
 */
public class SparkRestClient {

    public static final String SPARK_VERSION = "2.0.0";
    private static final String DATA = "data";
    private static final String JOB_EXECUTOR_CLASS = "com.tierconnect.riot.ml.execution.DriverJobExecutor";
    private static final String SPARK_CONNECTION_CODE = "SPARK";
    private final Logger logger = Logger.getLogger(SparkRestClient.class);

    private static final String MIME_TYPE_JSON = "application/json";
    private static final String CHARSET_UTF_8 = "charset=UTF-8";
    private static final String MIME_TYPE_JSON_UTF_8 = MIME_TYPE_JSON + ";" + CHARSET_UTF_8;

    private String sparkVersion;
    private Integer masterPort;
    private String masterHost;
    private String clusterMode;
    private Map<String, String> environmentVariables;
    private RestClient client;
    private final AtomicBoolean started = new AtomicBoolean();

    private static SparkRestClient instance;


    /**
     * Default constructor of an instance of SparkRestClient.
     */
    public SparkRestClient() {

        this.sparkVersion = SPARK_VERSION;
        this.masterHost = MlConfiguration.property("masterHost");
        this.masterPort = Integer.valueOf(MlConfiguration.property("masterPort"));
        this.clusterMode = MlConfiguration.property("clusterMode");
        this.environmentVariables = Collections.emptyMap();
        this.client = RestClient.instance();

    }

//    /**
//     * Restart spark application.
//     *
//     * @param submissionId    the submission ID
//     * @param restHost        the rest host
//     * @param restPort        the rest port
//     * @param restContextPath the rest context path
//     * @param restApiKey      the rest api key
//     * @param hadoopHost      the hadoop host
//     */
//    public void restart(final String submissionId,
//                        final String restHost,
//                        final int restPort,
//                        final String restContextPath,
//                        final String restApiKey,
//                        final String hadoopHost) {
//        synchronized (this) {
//            System.out.println("Restarting Spark Application...");
//            DriverState driverState = this.checkJobStatus(submissionId);
//
//            // Verify if the driver is running.
//            if (driverState != null && driverState.equals(DriverState.RUNNING)) {
//
//                // Try to kill the application.
//                final boolean success = killSparkApplication(submissionId);
//                if (success && !started.get()) {
//                    driverState = this.checkFinishedJobStatus(submissionId);
//                    if (success && driverState.equals(DriverState.KILLED)) {
//                        startSparkApplication(masterHost, restHost, restPort, restContextPath,
//                                restApiKey, hadoopHost);
//
//                        // Set started to false when the application is started.
//                        this.started.set(false);
//                    }
//                } else {
//                    System.out.println("Waiting... The spark application is restarting ...");
//                }
//            }
//        }
//    }
//
//    /**
//     * Kill the spark application.
//     *
//     * @param submissionId the submission ID.
//     */
//
//    public boolean killSparkApplication(final String submissionId) {
//        Preconditions.checkNotNull(submissionId, "The submissionId is null");
//        System.out.println("Killing Spark Application ...");
//        boolean success = false;
//        if (StringUtils.isNotBlank(submissionId)) {
//            URIBuilder uriBuilder = new URIBuilder();
//            uriBuilder.setScheme("http");
//            uriBuilder.setHost(this.getMasterHost());
//            uriBuilder.setPort(this.getMasterPort());
//            uriBuilder.setPath(String.format("/v1/submissions/kill/%s", submissionId));
//
//            try {
//                URI uri = uriBuilder.build();
//                success = HttpRequestUtil.executeHttpMethodAndGetResponse(this.getClient(),
//                        new HttpPost(uri),
//                        SparkResponse.class).getSuccess();
//            } catch (URISyntaxException e) {
//                logger.error(e);
//            } catch (FailedSparkRequestException e) {
//                logger.error(e);
//            }
//
//        }
//        return success;
//    }
//
//
//    /**
//     * Check finished job status.
//     *
//     * @param submissionId the submission ID
//     * @return the driver state
//     */
//    private DriverState checkFinishedJobStatus(final String submissionId) {
//        DriverState driverState;
//        this.started.set(true);
//
//        do {
//            driverState = checkJobStatus(submissionId);
//            if (driverState != null) switch (driverState) {
//                case ERROR: {
//                    System.out.println("ERROR...");
//                }
//                case FAILED: {
//                    System.out.println("FAILED...");
//                }
//                case UNKNOWN: {
//                    System.out.println("UNKNOWN...");
//                    this.started.set(false);
//                    continue;
//                }
//                case RUNNING: {
//                    System.out.println("RUNNING...");
//                    break;
//                }
//                case FINISHED:
//                    break;
//                case RELAUNCHING: {
//                    System.out.println("RELAUNCHING...");
//                    break;
//                }
//                case SUBMITTED: {
//                    System.out.println("SUBMITTED...");
//                    break;
//                }
//                case KILLED:
//                    break;
//            }
//
//            try {
//                Thread.sleep(3000);
//            } catch (InterruptedException e) {
//                logger.error(e.getMessage(), e.getCause());
//                this.started.set(false);
//            }
//        }
//        while (driverState != null && !driverState.equals(DriverState.KILLED));
//
//        return driverState;
//    }
//
//    /**
//     * CheckJobStatus.
//     *
//     * @param submissionId the submission ID
//     * @return the driver Status
//     */
//
//    public DriverState checkJobStatus(final String submissionId) {
//        Preconditions.checkNotNull(submissionId, "The submissionId is null");
//        System.out.println("Checking job status ...");
//        DriverState result = null;
//        if (StringUtils.isNotBlank(submissionId)) {
//            final String url = String.format("http://%s/v1/submissions/status/%s",
//                    this.getMasterUrl(), submissionId);
//            final JobStatusResponse response;
//            try {
//                response = HttpRequestUtil.executeHttpMethodAndGetResponse(this.getClient(),
//                        new HttpGet(url),
//                        JobStatusResponse.class);
//                if (!response.getSuccess()) {
//                    logger.error("submit was not successful.");
//                }
//                result = response.getDriverState();
//            } catch (FailedSparkRequestException e) {
//                logger.error(e);
//                this.started.set(false);
//            }
//
//            return result;
//        } else {
//            return result;
//        }
//    }

    /**
     * Start Spark Application.
     *
     * @param appArgs      Appname, appResource, appArgs and entry class name
     * @param responseHandler
     * @param appName
     */

    public void startSparkApplication(
            final List<String> appArgs,
            final RestClient.ResponseHandler responseHandler,
            final String appName) {

        logger.info("Starting Spark Application ...");

        try {

            Map<String, Object> data = new HashMap<>();
            // app args
            data.put("appArgs", appArgs);
            // where to find the jar
            //// IMPORTANT: jar can be a hadoop address
            final String appResource = MlConfiguration.property("jars.path")+ "/" + appName + ".jar";

            data.put("appResource", appResource);
            data.put("mainClass", JOB_EXECUTOR_CLASS);
            data.put("environmentVariables", new HashMap<String, String>() {{
                put("SPARK_ENV_LOADED", "1");
            }});
            data.put("action", "CreateSubmissionRequest");
            data.put("clientSparkVersion", SPARK_VERSION);
            data.put("sparkProperties", new HashMap<String, String>() {{
                put("spark.jars", appResource);
                put("spark.driver.supervise", "false");
                put("spark.app.name", appName);
                put("spark.eventLog.enabled", "true");
                put("spark.submit.deployMode", "cluster");
                put("spark.master", "spark://" + masterHost + ":" + masterPort);
            }});
            Map<String, String> headers = new HashMap<>();
            headers.put(HTTP.CONTENT_TYPE, MIME_TYPE_JSON_UTF_8);

            logger.info(" master port: " + masterPort);
            logger.info(" master host: " + masterHost);
            logger.info(" data       : " + data);


            URI uri = new URIBuilder()
                    .setScheme("http")
                    .setHost(masterHost)
                    .setPort(masterPort)
                    .setPath("/v1/submissions/create")
                    .build();

            client.post(uri, data, headers, responseHandler);

        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e.getCause());
            this.started.set(false);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (RestCallException e) {
            e.printStackTrace();
        }
    }

}
