package com.tierconnect.riot.commons.services.broker;


import java.util.Map;

/**
 * Created by ruth on 09-08-17.
 */
public class BrokerEdgeBox {
    private Long id;
    private Long groupId;
    private String name;
    private String code;
    private String type;
    private BrokerConnection mqttConnection;
    private BrokerConnection kafkaConnection;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BrokerConnection getMqttConnection() {
        return mqttConnection;
    }

    public void setMqttConnection(BrokerConnection mqttConnection) {
        this.mqttConnection = mqttConnection;
    }

    public BrokerConnection getKafkaConnection() {
        return kafkaConnection;
    }

    public void setKafkaConnection(BrokerConnection kafkaConnection) {
        this.kafkaConnection = kafkaConnection;
    }
}
