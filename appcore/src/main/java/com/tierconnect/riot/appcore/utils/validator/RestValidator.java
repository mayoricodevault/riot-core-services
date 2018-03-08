package com.tierconnect.riot.appcore.utils.validator;

import com.tierconnect.riot.appcore.entities.ConnectionType;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;


public class RestValidator implements ConnectionValidator {
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
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            JSONObject restProperties = (JSONObject) parser.parse(properties);
            URIBuilder ub = new URIBuilder();
            if (Boolean.parseBoolean(String.valueOf(restProperties.get("secure")))) {
                ub.setScheme("https");
            } else {
                ub.setScheme("http");
            }
            ub.setHost((String) restProperties.get("host"));
            ub.setPort(Math.toIntExact((Long) restProperties.get("port")));
            ub.setPath((String) restProperties.get("contextpath"));

            URI uri = ub.build();
            HttpGet httpRequest = new HttpGet( uri );
            httpRequest.addHeader( "Api_key", (String) restProperties.get("apikey"));
            httpRequest.setConfig(RequestConfig.custom().setConnectTimeout(15000).build());

            CloseableHttpResponse response = httpclient.execute( httpRequest );

            status = response.getStatusLine().getStatusCode();
            if(status == 200) {
                cause = "Success";
                response.close();
                return true;
            } else {
                cause = response.toString();
                response.close();
                return false;
            }
        } catch (UnknownHostException | URISyntaxException e) {
            status = 400;
            cause = e.getMessage();
            return false;
        } catch (HttpHostConnectException e) {
            status = 403;
            cause = e.getMessage();
            return false;
        } catch (ConnectTimeoutException e) {
            status = 408;
            cause = e.getMessage();
            return false;
        } catch (ParseException e) {
            status = 400;
            cause = "Cannot parse configuration. " + e.getMessage();
            return false;
        } catch (IOException e) {
            status = 400;
            cause = "I/O error on validation. " + e.getMessage();
            return false;
        } catch (Exception e) {
            status = 400;
            cause = e.getMessage();
            return false;
        }
    }
}
