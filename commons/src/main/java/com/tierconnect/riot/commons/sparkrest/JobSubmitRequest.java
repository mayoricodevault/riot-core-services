package com.tierconnect.riot.commons.sparkrest;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * JobSubmitRequest class.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class JobSubmitRequest {

    private Action action;
    private String appResource;
    private List<String> appArgs;
    private String clientSparkVersion;
    private String mainClass;
    private Map<String, String> environmentVariables;
    private Map<String, String> sparkProperties;

    /**
     * Creates an instance of JobSubmitRequest.
     *
     * @param action
     * @param appResource
     * @param appArgs
     * @param clientSparkVersion
     * @param mainClass
     * @param environmentVariables
     * @param sparkProperties
     */
    public JobSubmitRequest(Action action,
                            final String appResource,
                            List<String> appArgs,
                            final String clientSparkVersion,
                            final String mainClass,
                            Map<String, String> environmentVariables,
                            Map<String, String> sparkProperties) {
        this.action = action;
        this.appResource = appResource;
        this.appArgs = appArgs;
        this.clientSparkVersion = clientSparkVersion;
        this.mainClass = mainClass;
        this.environmentVariables = environmentVariables;
        this.sparkProperties = sparkProperties;
    }

    /**
     * Gets the action.
     *
     * @return the action
     */
    public Action getAction() {
        return action;
    }

    /**
     * Gets the app resource.
     *
     * @return the app resource
     */
    public String getAppResource() {
        return appResource;
    }

    /**
     * Gets the app arguments.
     *
     * @return the app arguments
     */
    public List<String> getAppArgs() {
        return appArgs;
    }

    /**
     * Gets the client spark version
     *
     * @return the client spark version
     */
    public String getClientSparkVersion() {
        return clientSparkVersion;
    }

    /**
     * Gets the main class.
     *
     * @return the main class
     */
    public String getMainClass() {
        return mainClass;
    }

    /**
     * Gets the environment variables.
     *
     * @return the environment variables
     */
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }

    /**
     * Gets the spark properties.
     *
     * @return the spark properties
     */
    public Map<String, String> getSparkProperties() {
        return sparkProperties;
    }

    /**
     * Builds a Job Submit Request Builder.
     *
     * @return the Job Submit Request Builder
     */
    public static JobSubmitRequestBuilder builder() {
        return new JobSubmitRequestBuilder();
    }

    /**
     * JobSubmitRequestBuilder class.
     */
    public static class JobSubmitRequestBuilder {
        private Action action;
        private String appResource;
        private List<String> appArgs;
        private String clientSparkVersion;
        private String mainClass;
        private Map<String, String> environmentVariables;
        private Map<String, String> sparkProperties;

        public JobSubmitRequestBuilder action(Action action) {
            this.action = action;
            return this;
        }

        public JobSubmitRequestBuilder appResource(String appResource) {
            this.appResource = appResource;
            return this;
        }

        public JobSubmitRequestBuilder appArgs(List<String> appArgs) {
            this.appArgs = appArgs;
            return this;
        }

        public JobSubmitRequestBuilder clientSparkVersion(String clientSparkVersion) {
            this.clientSparkVersion = clientSparkVersion;
            return this;
        }

        public JobSubmitRequestBuilder mainClass(String mainClass) {
            this.mainClass = mainClass;
            return this;
        }

        public JobSubmitRequestBuilder environmentVariables(Map<String, String> environmentVariables) {
            this.environmentVariables = environmentVariables;
            return this;
        }

        public JobSubmitRequestBuilder sparkProperties(Map<String, String> sparkProperties) {
            this.sparkProperties = sparkProperties;
            return this;
        }

        public JobSubmitRequestBuilder sparkProperty(final String property,
                                                     final String value) {
            this.sparkProperties.put(property, value);
            return this;
        }

        public JobSubmitRequest build() {
            return new JobSubmitRequest(action, appResource, appArgs, clientSparkVersion, mainClass,
                                        environmentVariables, sparkProperties);
        }
    }
}
