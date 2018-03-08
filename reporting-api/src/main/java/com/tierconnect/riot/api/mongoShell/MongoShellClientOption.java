package com.tierconnect.riot.api.mongoShell;

import com.tierconnect.riot.api.configuration.PropertyReader;

import static com.tierconnect.riot.api.assertions.Assertions.*;

/**
 * Created by achambi on 11/24/16.
 * Create Mongo Shell Client Options
 */
public class MongoShellClientOption {

    private final ReadShellPreference readPreference;
    private final int connectTimeout;
    private final int maxPoolSize;
    private final boolean sslEnabled;
    private final String requiredReplicaSetName;

    /**
     * Creates a builder instance.
     *
     * @return a builder
     * @since 3.0.0
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Mongo shell constructor.
     *
     * @param builder Shell options constructor.
     */
    public MongoShellClientOption(final Builder builder) {
        readPreference = builder.readPreference;
        connectTimeout = builder.connectTimeout;
        maxPoolSize = builder.maxPoolSize;
        sslEnabled = builder.sslEnabled;
        requiredReplicaSetName = builder.requiredReplicaSetName;
    }

    /**
     * <p>Gets the ReadShellPreference for this MongoClient, which is used in various places.</p>
     * <p>
     * <p>Default is null.</p>
     *
     * @return the readPreference
     */
    public ReadShellPreference getReadPreference() {
        return readPreference;
    }

    /**
     * <p>Gets the ConnectTimeout for this MongoClient, which is used in various places.</p>
     * <p>
     * <p>Default is 0.</p>
     *
     * @return the readPreference
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * <p>Gets the MaxPoolSize for this MongoClient, which is used in various places.</p>
     * <p>
     * <p>Default is 0.</p>
     *
     * @return the MaxPoolSize
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    /**
     * <p>Gets the sslEnabled for this MongoClient, which is used in various places.</p>
     * <p>
     * <p>Default is false.</p>
     *
     * @return the sslEnabled
     */
    public boolean getSslEnabled() {
        return sslEnabled;
    }

    /**
     * <p>Gets the requiredReplicaSetName for this MongoClient, which is used in various places.</p>
     * <p>
     * <p>Default is a {@link String} blank.</p>
     *
     * @return the requiredReplicaSetName
     */
    public String getRequiredReplicaSetName() {
        return requiredReplicaSetName;
    }

    public static class Builder {

        private ReadShellPreference readPreference = ReadShellPreference.primary();
        private int connectTimeout;
        private int maxPoolSize;
        private boolean sslEnabled;
        private String requiredReplicaSetName;

        /**
         * Creates a Builder for MongoClientOptions, getting the appropriate system properties for initialization.
         */
        public Builder() {
            readPreference(ReadShellPreference.valueOf(PropertyReader.getProperty("mongo.reportsReadPreference",
                    "primary",
                    true)));
            connectTimeout(Integer.parseInt(PropertyReader.getProperty("mongo.connectiontimeout", "0", true)));
            maxPoolSize(Integer.parseInt(PropertyReader.getProperty("mongo.maxpoolsize", "100", true)));
            sslEnabled(Boolean.parseBoolean(PropertyReader.getProperty("mongo.ssl", "false", true)));
            requiredReplicaSetName(PropertyReader.getProperty("mongo.replicaset", "", true));
        }

        /**
         * Sets the minimum number of connections per host.
         *
         * @param readPreference read preference in string format.
         * @return {@code this}
         * @throws IllegalArgumentException if {@readPreference is null or empty}
         * @see MongoShellClientOption#getReadPreference()
         */
        public Builder readPreference(final ReadShellPreference readPreference) {
            voidNotNull("readPreference", readPreference);
            this.readPreference = readPreference;
            return this;
        }

        /**
         * Sets the connectTimeout.
         *
         * @param connectTimeout of this MongoClient
         * @return {@code this}
         * @see MongoShellClientOption#getConnectTimeout()
         */
        public Builder connectTimeout(final int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * Sets the maxPoolSize.
         *
         * @param maxPoolSize of this MongoClient
         * @return {@code this}
         * @see MongoShellClientOption#getMaxPoolSize()
         */
        public Builder maxPoolSize(final int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        /**
         * Sets the sslEnabled.
         *
         * @param sslEnabled of this MongoClient
         * @return {@code this}
         * @see MongoShellClientOption#getSslEnabled()
         */
        public Builder sslEnabled(final boolean sslEnabled) {
            this.sslEnabled = sslEnabled;
            return this;
        }

        /**
         * Sets the sslEnabled.
         *
         * @param requiredReplicaSetName of this MongoClient
         * @return {@code this}
         * @see MongoShellClientOption#getRequiredReplicaSetName()
         */
        public Builder requiredReplicaSetName(final String requiredReplicaSetName) {
            this.requiredReplicaSetName = requiredReplicaSetName;
            return this;
        }
    }
}
