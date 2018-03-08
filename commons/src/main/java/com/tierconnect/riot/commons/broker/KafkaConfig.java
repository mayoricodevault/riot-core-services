package com.tierconnect.riot.commons.broker;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.base.Objects;

/**
 * KafkaConfig class.
 *
 * @author jantezana
 * @version 2017/06/06
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KafkaConfig implements BrokerConfig {
    public String connectionCode;
    public String zookeeper;
    public String server;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaConfig that = (KafkaConfig) o;
        return Objects.equal(connectionCode, that.connectionCode) &&
                Objects.equal(zookeeper, that.zookeeper) &&
                Objects.equal(server, that.server);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(connectionCode, zookeeper, server);
    }
}
