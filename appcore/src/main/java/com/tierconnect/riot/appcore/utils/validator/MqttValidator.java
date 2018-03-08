package com.tierconnect.riot.appcore.utils.validator;

import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.commons.services.broker.MqttPublisher;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;


public class MqttValidator implements ConnectionValidator {
    private static Logger logger = Logger.getLogger(MqttValidator.class);
    private int status;
    private String cause;

    @Override public int getStatus() {
        return status;
    }

    @Override public String getCause() {
        return cause;
    }

    @Override public boolean testConnection(ConnectionType connectionType, String properties) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject mqttProperties = (JSONObject) parser.parse(properties);
            String host = mqttProperties.get("host").toString();
            int port = Integer.parseInt(mqttProperties.get("port").toString());
            int qos = Integer.parseInt(mqttProperties.get("qos").toString());
            String username = String.valueOf(mqttProperties.get("username"));
            String password = String.valueOf(mqttProperties.get("password"));
            MqttPublisher
                client = new MqttPublisher("testConn", host, port, qos, username, password);

                try {
                    String testMessage = client.publishTest();
                    if (StringUtils.isBlank(testMessage)) {
                        status = 200;
                        cause = "Success";
                        return true;
                    } else {
                        status = 400;
                        cause = testMessage;
                        return false;
                    }
                } catch (UserException e){
                    status = e.getStatus();
                    cause = e.getMessage();
                    return false;
                }

        } catch (ParseException e) {
            status = 400;
            cause = "Cannot parse configuration, " + e.getMessage();
            logger.warn("Cannot parse connection properties.", e);
            return false;
        }
    }
}
