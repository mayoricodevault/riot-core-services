package com.tierconnect.riot.commons.services.broker;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Connection object for BrokerPublisherService
 * Created by vramos on 10/13/16.
 */
public class BrokerConnection {
    //private List<String> bridges = new ArrayList<>();
    private String code;
    private String connectionType;
    private boolean active;
    private Long groupId;
    private Map<String, Object> properties;
    private String password;

    /**
     *
     * @param code Connection code.
     * @param brokerType Connection type code.
     * @param active Determines whether a message is sent or not.
     * @param properties Connection properties map: host, port, qos, servers, zookeeper, etc.
     */
    public BrokerConnection(String code, String brokerType, boolean active, Map<String, Object> properties,
                            String password, Long groupId) {
        this.code = code;
        this.connectionType = brokerType;
        this.active = active;
        this.properties = properties;
        this.password = password;
        this.groupId = groupId;
    }

    public String getCode() {
        return code;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public boolean isActive() {
        return active;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public String getPassword() {
        return password;
    }

    public Long getGroupId() {
        return groupId;
    }

    @Override
    public String toString(){
        return "Connection code: " + code + ", type: " + connectionType + ", host: " + properties.get("host");
    }
}
