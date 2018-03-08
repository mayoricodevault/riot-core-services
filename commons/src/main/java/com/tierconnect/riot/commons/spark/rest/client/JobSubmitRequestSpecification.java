package com.tierconnect.riot.commons.spark.rest.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JobSubmitRequestSpecification class.
 */
public class JobSubmitRequestSpecification {

    private String appResource;
    private List<String> appArgs;
    private String mainClass;
    private String appName;
    private Set<String> jars;
    private SparkRestClient sparkRestClient;
    private Map<String, String> props = new HashMap<>();

    public String getAppResource() {
        return appResource;
    }

    public List<String> getAppArgs() {
        return appArgs;
    }

    public String getMainClass() {
        return mainClass;
    }

    public String getAppName() {
        return appName;
    }

    public Set<String> getJars() {
        return jars;
    }

    public SparkRestClient getSparkRestClient() {
        return sparkRestClient;
    }

    public Map<String, String> getProps() {
        return props;
    }

    /**
     * Creates an instance of JobSubmitRequestSpecification.
     *
     * @param sparkRestClient the spark rest client
     */
    public JobSubmitRequestSpecification(SparkRestClient sparkRestClient) {
        this.sparkRestClient = sparkRestClient;
    }

    /**
     * @param appResource location of jar which contains application containing your <code>mainClass</code>.
     * @return The request specification
     */
    public JobSubmitRequestSpecification appResource(String appResource) {
        this.appResource = appResource;
        return this;
    }

    /**
     * @param appArgs args needed by the main() method of your <code>mainClass</code>.
     * @return The request specification
     */
    public JobSubmitRequestSpecification appArgs(List<String> appArgs) {
        this.appArgs = appArgs;
        return this;
    }

    /**
     * @param mainClass class containing the main() method which defines the Spark application driver and tasks.
     * @return The request specification
     */
    public JobSubmitRequestSpecification mainClass(String mainClass) {
        this.mainClass = mainClass;
        return this;
    }

    /**
     * @param appName name of your Spark job.
     * @return The request specification
     */
    public JobSubmitRequestSpecification appName(String appName) {
        this.appName = appName;
        return this;
    }
}
