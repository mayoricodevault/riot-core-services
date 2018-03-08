package com.tierconnect.riot.appcore.utils.validator;

import com.mongodb.MongoSecurityException;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoTimeoutException;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.sdk.utils.HashUtils;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.net.UnknownHostException;

import static org.apache.commons.lang.math.NumberUtils.isNumber;

/**
 * Created by aruiz on 8/1/17.
 */
public class MongoValidator implements ConnectionValidator {

    private int status;
    private String cause;

    @Override
    public boolean testConnection(ConnectionType connectionType, String properties) {

        JSONParser parser = new JSONParser();
        try {
            JSONObject mongoProperties = (JSONObject) parser.parse(properties);
            String unHashPassword = Connection.decode(mongoProperties.get("password").toString());
            MongoDAOUtil.setupMongodb(mongoProperties.get("mongoPrimary").toString(),
                    mongoProperties.get("mongoSecondary").toString(),
                    mongoProperties.get("mongoReplicaSet").toString(),
                    Boolean.getBoolean(mongoProperties.get("mongoSSL").toString()),
                    mongoProperties.get("username").toString(),
                    unHashPassword,
                    mongoProperties.get("mongoAuthDB").toString(),
                    mongoProperties.get("mongoDB").toString(),
                    null,
                    null,
                    Boolean.getBoolean(mongoProperties.get("mongoSharding").toString()),
                    StringUtils.isBlank(mongoProperties.get("mongoConnectTimeout").toString()) &&
                            isNumber(mongoProperties.get("mongoConnectTimeout").toString())?
                            Integer.getInteger(mongoProperties.get("mongoConnectTimeout").toString()):0,
                    StringUtils.isBlank(mongoProperties.get("mongoMaxPoolSize").toString()) &&
                            isNumber(mongoProperties.get("mongoMaxPoolSize").toString())?
                    Integer.getInteger(mongoProperties.get("mongoMaxPoolSize").toString()):0);
            status = 200;
            cause = "Success";
            return  true;

        }catch (ParseException e){
            status = 400;
            cause = "Cannot parse configuration. " + e.getMessage();
            return  false;

        } catch (UnknownHostException | MongoSecurityException | MongoSocketOpenException | SecurityException e) {
            status = 400;
            cause = e.getMessage();
            return  false;
        } catch (MongoTimeoutException e){
            status = 400;
            cause = "Mongo service unreachable in host";
            return  false;
        }
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
