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
public class MqttConfig implements BrokerConfig {
    public String host;
    public int port;
    public int qos;
    public String username;
    public String password;

    public MqttConfig(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public MqttConfig(String host, int port, int qos) {
        this.host = host;
        this.port = port;
        this.qos = qos;
    }

    public MqttConfig(String host, int port, int qos, String username, String password) {
        this.host = host;
        this.port = port;
        this.qos = qos;
        this.username = username;
        this.password = password;
    }

    public MqttConfig() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MqttConfig that = (MqttConfig) o;
        return port == that.port && Objects.equal(host, that.host) && Objects.equal(username, that.username) &&
               Objects.equal(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(host, port, username, password);
    }
}
