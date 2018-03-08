package com.tierconnect.riot.appcore.utils.validator;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.utils.LdapAuthentication;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

/**
 * Created by aruiz on 8/1/17.
 */
public class LdapValidator implements ConnectionValidator {

    private static Logger logger = Logger.getLogger(LdapValidator.class);
    private int status;
    private String cause;


    @Override
    public boolean testConnection(ConnectionType connectionType, String properties) {
        boolean result = false;
        LdapAuthentication ldapAuthentication = new LdapAuthentication();
        JSONParser parser = new JSONParser();
        try {
            JSONObject ldapProperties = (JSONObject) parser.parse(properties);
            ldapProperties.put("password", Connection.decode(                String.valueOf(ldapProperties.get("password"))));
            ldapAuthentication.setContextSource(ldapProperties);
            result = ldapAuthentication.testConnection(ldapProperties.get("userDn").toString(),ldapProperties.get("password").toString());
            status = 200;
            cause = "Success";
        }catch (ParseException e){
            status = 400;
            cause = "Cannot parse configuration. " + e.getMessage();
            result = false;
        }
        return result;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getCause() {
        return cause;
    }
}
