package com.tierconnect.riot.appcore.utils.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.commons.actions.executors.GooglePubSubExecutor;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;
import java.io.ByteArrayInputStream;


import java.util.*;

/**
 * Created by aruiz on 8/1/17.
 */
public class GooglePubSubValidator implements ConnectionValidator {
    private int status;
    private String cause;
    private Logger logger = Logger.getLogger( GooglePubSubExecutor.class );
    private ObjectMapper mapper = new ObjectMapper();
    private JacksonFactory jsonFactory;
    private NetHttpTransport transport;
    private GoogleCredential credential;
    private HttpRequestFactory requestFactory;

    @Override
    public boolean testConnection(ConnectionType connectionType, String properties) {
        JSONParser parser = new JSONParser();
        try {
            JSONObject googleProperties = (JSONObject) parser.parse(properties);
            return test(googleProperties);
        }catch (ParseException e){
            status = 400;
            cause = "Cannot parse configuration. " + e.getMessage();
            return false;
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


    public boolean test (JSONObject properties) {
        try {
            jsonFactory = JacksonFactory.getDefaultInstance();
            transport = GoogleNetHttpTransport.newTrustedTransport();
            Collection<String> scopes = Arrays.asList("https://www.googleapis.com/auth/pubsub",
                    "https://www.googleapis.com/auth/cloud-platform");

            credential = GoogleCredential.fromStream(
                    new ByteArrayInputStream(mapper.writeValueAsBytes(properties)),
                    transport,
                    jsonFactory);
            credential = credential.createScoped(scopes);
            requestFactory = transport.createRequestFactory();
            GenericUrl url = new GenericUrl(String.format("https://pubsub.googleapis.com/v1/projects/%s/subscriptions/pubsub:testIamPermissions", properties.get("project_id").toString()));

            HttpRequest request = requestFactory.buildPostRequest(url,
                    ByteArrayContent.fromString("application/json", ""));
            credential.initialize(request);
            HttpResponse response = request.execute();
            logger.info("This is credential access token"+credential.getAccessToken());
            if (response.getStatusCode() == 200) {
                status = 200;
                cause = "Success";
                return true;
            } else {
                status = 400;
                cause = "Cannot parse configuration. " +  response.getStatusMessage();
                logger.error(String.format("Test of permissions on GoogleCloud Pub/Sub returns code=%d message=%s", response.getStatusCode(), response.getStatusMessage()));
                return false;
            }
    }
    catch (Exception e){
            status = 400;
            cause = "Cannot parse configuration. " + e.getMessage();
            return false;
        }
    }
}
