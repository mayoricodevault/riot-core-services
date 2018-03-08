package com.tierconnect.riot.commons.spark.rest.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.tierconnect.riot.commons.utils.HttpRequestUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.protocol.HTTP;
import org.apache.log4j.Logger;
import com.tierconnect.riot.commons.sparkrest.FailedSparkRequestException;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SparkRestClient class.
 */
public class SparkRestClient {

    public static final String SPARK_VERSION = "2.0.1";
    public static final String APP_NAME = "CoreBridge";
    private final Logger logger = Logger.getLogger(SparkRestClient.class);

    private static final String MIME_TYPE_JSON = "application/json";
    private static final String CHARSET_UTF_8 = "charset=UTF-8";
    private static final String MIME_TYPE_JSON_UTF_8 = MIME_TYPE_JSON + ";" + CHARSET_UTF_8;

    private String sparkVersion;
    private Integer masterPort;
    private String masterHost;
    private String clusterMode;
    private Map<String, String> environmentVariables;
    private HttpClient client;
    private final AtomicBoolean started = new AtomicBoolean();

    private static SparkRestClient instance;


    /**
     * Default constructor of an instance of SparkRestClient.
     */
    private SparkRestClient() {
        this.sparkVersion = SPARK_VERSION;
        this.masterPort = 6066;
        this.clusterMode = "spark";
        this.client = HttpClientBuilder.create().setConnectionManager(
            new BasicHttpClientConnectionManager()).build();
        this.environmentVariables = Collections.emptyMap();
    }

    /**
     * Gets the instance of SparkRestClient.
     *
     * @return the instance of SparkRestClient
     */
    public static SparkRestClient getInstance() {
        if (instance == null) {
            synchronized (SparkRestClient.class) {
                if (instance == null) {
                    instance = new SparkRestClient();
                }
            }
        }

        return instance;
    }

    public String getMasterUrl() {
        return String.format("%s:%s", masterHost, masterPort);
    }

    public String getSparkVersion() {
        return sparkVersion;
    }

    public Integer getMasterPort() {
        return masterPort;
    }

    public String getMasterHost() {
        return masterHost;
    }

    public void setMasterHost(String masterHost) {
        this.masterHost = masterHost;
    }

    public String getClusterMode() {
        return clusterMode;
    }

    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    public HttpClient getClient() {
        return client;
    }

    public void setClient(HttpClient client) {
        this.client = client;
    }

    /**
     * Restart spark application.
     *
     * @param submissionId    the submission ID
     * @param restHost        the rest host
     * @param restPort        the rest port
     * @param restContextPath the rest context path
     * @param restApiKey      the rest api key
     * @param hadoopHost      the hadoop host
     */
    public void restart(final String submissionId,
                        final String restHost,
                        final int restPort,
                        final String restContextPath,
                        final String restApiKey,
                        final String hadoopHost,
                        final String sparkWorkers) {
        synchronized (this) {
            logger.info("Restarting Spark Application...");
            DriverState driverState = this.checkJobStatus(submissionId);

            // Verify if the driver is running.
            if (driverState != null && driverState.equals(DriverState.RUNNING)) {

                // Try to kill the application.
                final boolean success = killSparkApplication(submissionId);
                if (success && !started.get()) {
                    driverState = this.checkFinishedJobStatus(submissionId);
                    if (success && driverState.equals(DriverState.KILLED)) {
                        startSparkApplication(masterHost, restHost, restPort, restContextPath,
                                              restApiKey, hadoopHost, sparkWorkers);

                        // Set started to false when the application is started.
                        this.started.set(false);
                    }
                } else {
                    logger.info("Waiting... The spark application is restarting ...");
                }
            }
        }
    }

    /**
     * Kill the spark application.
     *
     * @param submissionId the submission ID.
     */

    public boolean killSparkApplication(final String submissionId) {
        Preconditions.checkNotNull(submissionId, "The submissionId is null");
        logger.info("Killing Spark Application ...");
        boolean success = false;
        if (StringUtils.isNotBlank(submissionId)) {
            URIBuilder uriBuilder = new URIBuilder();
            uriBuilder.setScheme("http");
            uriBuilder.setHost(this.getMasterHost());
            uriBuilder.setPort(this.getMasterPort());
            uriBuilder.setPath(String.format("/v1/submissions/kill/%s", submissionId));

            try {
                URI uri = uriBuilder.build();
                success = HttpRequestUtil.executeHttpMethodAndGetResponse(this.getClient(),
                                                                          new HttpPost(uri),
                                                                          SparkResponse.class).getSuccess();
            } catch (URISyntaxException e) {
                logger.error(e);
            } catch (FailedSparkRequestException e) {
                logger.error(e);
            }

        }
        return success;
    }


    /**
     * Check finished job status.
     *
     * @param submissionId the submission ID
     * @return the driver state
     */
    private DriverState checkFinishedJobStatus(final String submissionId) {
        DriverState driverState;
        this.started.set(true);

        do {
            driverState = checkJobStatus(submissionId);
            if (driverState != null) {
                switch (driverState) {
                    case ERROR: {
                        logger.info("ERROR...");
                    }
                    case FAILED: {
                        logger.info("FAILED...");
                    }
                    case UNKNOWN: {
                        logger.info("UNKNOWN...");
                        this.started.set(false);
                        continue;
                    }
                    case RUNNING: {
                        logger.info("RUNNING...");
                        break;
                    }
                    case RELAUNCHING: {
                        logger.info("RELAUNCHING...");
                        break;
                    }
                    case SUBMITTED: {
                        logger.info("SUBMITTED...");
                        break;
                    }
                    default: {
                        break;
                    }
                }
            }

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e.getCause());
                this.started.set(false);
            }
        }
        while (driverState != null && !driverState.equals(DriverState.KILLED));

        return driverState;
    }

    /**
     * CheckJobStatus.
     *
     * @param submissionId the submission ID
     * @return the driver Status
     */

    public DriverState checkJobStatus(final String submissionId) {
        Preconditions.checkNotNull(submissionId, "The submissionId is null");
        logger.info("Checking job status ...");
        DriverState result = null;
        if (StringUtils.isNotBlank(submissionId)) {
            final String url = String.format("http://%s/v1/submissions/status/%s",
                                             this.getMasterUrl(), submissionId);
            final JobStatusResponse response;
            try {
                response = HttpRequestUtil.executeHttpMethodAndGetResponse(this.getClient(),
                                                                           new HttpGet(url),
                                                                           JobStatusResponse.class);
                if (!response.getSuccess()) {
                    logger.error("submit was not successful.");
                }
                result = response.getDriverState();
            } catch (FailedSparkRequestException e) {
                logger.error(e);
                this.started.set(false);
            }
        }

        return result;
    }

    /**
     * Start Spark Application.
     *
     * @param masterHost      the master host
     * @param restHost        the rest host
     * @param restPort        the rest port
     * @param restContextPath the rest context path
     * @param restApiKey      the rest api key
     * @param hadoopHost      the hadoop host
     */
    public String startSparkApplication(final String masterHost,
                                        final String restHost,
                                        final int restPort,
                                        final String restContextPath,
                                        final String restApiKey,
                                        final String hadoopHost,
                                        final String sparkWorkers) {
        logger.info("Starting Spark Application ...");
        Preconditions.checkNotNull(masterHost, "The masterHost is null");
        Preconditions.checkNotNull(restHost, "The restHost is null");
        Preconditions.checkNotNull(restPort, "The restPort is null");
        Preconditions.checkNotNull(restContextPath, "The restContextPath is null");
        Preconditions.checkNotNull(restApiKey, "The restApiKey is null");
        Preconditions.checkNotNull(hadoopHost, "The hadoopHost is null");
        Preconditions.checkNotNull(sparkWorkers, "The sparkWorkers is null");
        String submissionId = null;
        List<String> appArgs = new LinkedList<>();
        appArgs.add(restHost);
        appArgs.add(String.valueOf(restPort));
        appArgs.add(restContextPath);
        appArgs.add(restApiKey);
        // Batch Interval.
        appArgs.add("-1");
        // Write to mongo.
        appArgs.add("-1");
        // Spark workers.
        appArgs.add(sparkWorkers);

        try {
            JobSubmitRequestSpecification jobSubmitRequestSpecification = this.prepareJobSubmit().appName(
                APP_NAME).appResource(
                String.format("hdfs://%s:9000/riot-core-bridges-all.jar", hadoopHost)).appArgs(
                appArgs).mainClass("com.tierconnect.riot.bridges.spark.SparkCoreBridge");
            submissionId = this.submit(jobSubmitRequestSpecification);
            logger.info(String.format("submission ID: %s", submissionId));
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e.getCause());
            this.started.set(false);
        } catch (FailedSparkRequestException e) {
            logger.error(e.getMessage(), e.getCause());
            this.started.set(false);
        } catch (Exception e) {
            logger.error(e.getMessage(), e.getCause());
            this.started.set(false);
        }

        return submissionId;
    }

    /**
     * Submit a new SPARK job to the Spark Standalone cluster.
     *
     * @return SubmissionId of task submitted to the Spark cluster, if submission was successful.
     * Please note that a successful submission does not guarantee successful deployment of app.
     * @throws FailedSparkRequestException iff submission failed.
     */
    private String submit(JobSubmitRequestSpecification specification)
    throws FailedSparkRequestException {
        if (specification.getMainClass() == null || specification.getAppResource() == null) {
            throw new IllegalArgumentException("mainClass and appResource values must not be null");
        }

        if (specification.getAppArgs() == null) {
            specification.appArgs(Collections.EMPTY_LIST);
        }

        // Create the spark properties.
        Map<String, String> sparkProperties = new HashMap<String, String>();
        sparkProperties.put("spark.jars", specification.getAppResource());
        sparkProperties.put("spark.app.name", specification.getAppName());
        sparkProperties.put("spark.master",
                            String.format("%s://%s", this.getClusterMode(), this.getMasterUrl()));

        final JobSubmitRequest jobSubmitRequest = JobSubmitRequest.builder().action(
            Action.CREATE_SUBMISSION_REQUEST).appArgs(specification.getAppArgs()).appResource(
            specification.getAppResource()).clientSparkVersion(
            this.getSparkVersion().toString()).mainClass(
            specification.getMainClass()).environmentVariables(
            this.getEnvironmentVariables()).sparkProperties(sparkProperties).build();

        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("http");
        uriBuilder.setHost(this.getMasterHost());
        uriBuilder.setPort(this.getMasterPort());
        uriBuilder.setPath("/v1/submissions/create");
        URI uri;
        try {
            uri = uriBuilder.build();
        } catch (URISyntaxException e) {
            this.started.set(false);
            throw new FailedSparkRequestException(e);
        }

        final HttpPost post = new HttpPost(uri);
        post.setHeader(HTTP.CONTENT_TYPE, MIME_TYPE_JSON_UTF_8);

        try {
            final ObjectMapper mapper = new ObjectMapper();
            final String message = mapper.writeValueAsString(jobSubmitRequest);
            post.setEntity(new StringEntity(message));
        } catch (UnsupportedEncodingException | JsonProcessingException e) {
            throw new FailedSparkRequestException(e);
        }

        final SparkResponse response = HttpRequestUtil.executeHttpMethodAndGetResponse(
            this.getClient(), post, SparkResponse.class);

        if (!response.getSuccess()) {
            throw new FailedSparkRequestException("submit was not successful.");
        }
        return response.getSubmissionId();
    }

    public JobSubmitRequestSpecification prepareJobSubmit() {
        return new JobSubmitRequestSpecification(this);
    }

}
