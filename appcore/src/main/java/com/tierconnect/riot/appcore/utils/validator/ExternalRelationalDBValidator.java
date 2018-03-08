package com.tierconnect.riot.appcore.utils.validator;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

/**
 * Created by aruiz on 8/1/17.
 */
public class ExternalRelationalDBValidator implements ConnectionValidator{
    private int status;
    private String cause;
    @Override
    public boolean testConnection(ConnectionType connectionType, String properties) {
        ExternalConnectionJdbc externalConnectionJdbc=null;
        JSONParser parser = new JSONParser();
        try {
            JSONObject externalProperties = (JSONObject) parser.parse(properties);
            String unHashPassword = Connection.decode(externalProperties.get("password").toString());
            externalConnectionJdbc = ExternalConnectionJdbc.getInstance(externalProperties.get("driver").toString(),
                    externalProperties.get("url").toString(), externalProperties.get("schema").toString(),
                    externalProperties.get("user").toString(),unHashPassword);
            status = 200;
            cause = "Success";
            return true;
        }catch (ParseException e){
            status = 400;
            cause = "Cannot parse configuration. " + e.getMessage();
            return false;
        }finally {
            if (externalConnectionJdbc != null) {
                externalConnectionJdbc.closeConnection();
            }
        }

    }

    @Override
    public int getStatus() {
        return 0;
    }

    @Override
    public String getCause() {
        return null;
    }
}
