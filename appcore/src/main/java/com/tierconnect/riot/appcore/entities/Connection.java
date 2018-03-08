package com.tierconnect.riot.appcore.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;

import javax.annotation.Generated;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tierconnect.riot.commons.Constants.CONNECTION_INTEGER_PROPERTIES;

@Entity

@Table(name = "connection0")
@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class Connection extends ConnectionBase {

    static Logger logger = Logger.getLogger(Connection.class);

    @Transient
    private Map<String, Object> propertiesMap = null;

    public static String decode(String hashed) {
        return new String(Base64.decodeBase64(hashed.getBytes(Charsets.UTF_8)), Charsets.UTF_8);
    }

    public static String encode(String password) {
        return new String(Base64.encodeBase64(password.getBytes(Charsets.UTF_8)), Charsets.UTF_8);
    }

    public Object getProperty(String propertyCode) {
        if (propertiesMap == null) {
            mapProperties();
        }

        return propertiesMap.get(propertyCode);
    }

    public String getPropertyAsString(String propertyCode) {
        if (propertiesMap == null) {
            mapProperties();
        }
        Object object = propertiesMap.get(propertyCode);
        return (object != null) ? object.toString() : "";
    }

    public Integer getPropertyAsNumber(String propertyCode) {
        if (propertiesMap == null) {
            mapProperties();
        }
        String number = getPropertyAsString(propertyCode);
        return NumberUtils.isNumber(number) ? Integer.parseInt(number) : 0;
    }

    public Boolean getPropertyAsBoolean(String propertyCode) {
        if (propertiesMap == null) {
            mapProperties();
        }

        return Boolean.valueOf(getPropertyAsString(propertyCode));
    }

    public void mapProperties() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            propertiesMap = mapper.readValue(getProperties(), Map.class);
        } catch (IOException e) {
            logger.error("error parsing connection propertiesMap:", e);
        }
    }

    public String getPassword(boolean encypted) {
        if (propertiesMap == null) {
            mapProperties();
        }

        StringBuffer a = new StringBuffer();
        if (propertiesMap.get("password") != null) {
            String password = propertiesMap.get("password").toString();
            a.append(encypted ? password : decode(password));
            return a.toString();
        } else {
            return null;
        }
    }

    public Map<String, Object> getPropertiesMap() {
        if (propertiesMap == null) {
            mapProperties();
        }
        return propertiesMap;
    }

    @Override
    public String toString() {
        StringBuffer a = new StringBuffer("id:" + getId() + "\n");
        a.append("name:" + getName() + "\n");
        a.append("code:" + getCode() + "\n");
        a.append("propertiesMap:" + getProperties() + "\n");
        return a.toString();
    }

    /**
     * Clone
     *
     * @param connection
     * @return
     */
    public Connection duplicateConnetion() {
        Connection conn = new Connection();
        conn.setConnectionType(this.getConnectionType());
        conn.setCode(this.getCode());
        conn.setGroup(this.getGroup());
        conn.setName(this.getName());
        conn.setProperties(this.getProperties());
        return conn;
    }

    public String requiredFieldsMessage() {
        List<String> fields = new ArrayList<>();
        JSONParser parser = new JSONParser();
        try {
            JSONArray arr = (JSONArray) parser.parse(connectionType.getPropertiesDefinitions());
            int i = 0;
            for (Object obj : arr) {
                if (((JSONObject) obj).size() > 0) {
                    String propertyName = String.valueOf(((JSONObject) obj).get("code"));
                    Object propertyValue = this.getProperty(propertyName);
                    String label = ((JSONObject) obj).get("label").toString();
                    if (((JSONObject) obj).containsKey("required") && Boolean
                        .parseBoolean(((JSONObject) obj).get("required").toString()) && (
                        propertyValue == null || StringUtils.isBlank(propertyValue.toString()))) {
                        fields.add(label);
                        i++;
                    }
                    validateIntegerParameters(propertyValue, propertyName, label);
                }
            }
            connectionType.publicMap();
            if (i == 0) {
                return null;
            } else {
                if (i == 1) {
                    return fields.toString() + " is required.";
                } else {
                    return fields.toString() + " are required.";
                }
            }
        } catch (Exception e) {
            logger.error("An error occured in validation.", e);
            return "An error occured in validation.";
        }
    }

    public void validateIntegerParameters(Object propertyValue, String propertyName, String label) throws Exception {
        if (CONNECTION_INTEGER_PROPERTIES.contains(propertyName)) {
            if (propertyValue != null && !StringUtils.isEmpty(propertyValue.toString()) &&
                    !((propertyValue instanceof Integer) ||
                            StringUtils.isNumeric(propertyValue.toString()))) {
                throw new Exception("The property '" + label + "' should be numeric");
            }
        }
    }
}

