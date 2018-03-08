package com.tierconnect.riot.iot.utils.rest;

import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;
import com.tierconnect.riot.appcore.utils.Utilities;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.*;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.tierconnect.riot.commons.Constants.ActionHTTPConstants.*;

/**
 * Created by vealaro on 3/27/17.
 */
public class ActionHTTP {

    private static Logger logger = Logger.getLogger(ActionHTTP.class);

    private String response;
    private int statusCode;
    private String error;
    private Map<String, Object> mapConfiguration;
    private String body;

    public ActionHTTP(Map<String, Object> mapConfiguration, String body) {
        this.mapConfiguration = mapConfiguration;
        this.body = body;
    }

    @SuppressWarnings("unchecked")
    public void executePost() throws ExecuteActionException {
        // get url
        URI uri = getURI();
        // object POST with headers
        HttpPost httpPost = new HttpPost(uri);
        setHeaders(httpPost);
        if (mapConfiguration.get(EXECUTION_TYPE_TIMEOUT.value) instanceof Integer) {
            int connectionTimeoutMS = ((Integer) mapConfiguration.get(EXECUTION_TYPE_TIMEOUT.value)) * 1000;
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(connectionTimeoutMS)
                    .setConnectTimeout(connectionTimeoutMS)
                    .setSocketTimeout(connectionTimeoutMS).build();
            httpPost.setConfig(requestConfig);
        }

        if (mapConfiguration.get(EXECUTION_TYPE.value) != null
                && mapConfiguration.get(EXECUTION_TYPE.value).equals(EXECUTION_TYPE_FORM.value)) {
            // add post parameters
            httpPost.setEntity(getParametersEntityEncoding());
        } else {
            // add body
            StringEntity entity = new StringEntity(body, ContentType.create("text/plain", "UTF-8"));
            httpPost.setEntity(entity);
        }

        // call
        logger.info("Executing http POST uri=" + uri);
        execute(httpPost);
    }

    @SuppressWarnings("unchecked")
    private UrlEncodedFormEntity getParametersEntityEncoding() throws ExecuteActionException {
        try {
            List<NameValuePair> postParameters = new ArrayList<>();
            if (!Utilities.isEmptyOrNull(body)) {
                Map<String, Object> result = new ObjectMapper().readValue(body, Map.class);
                if (result != null) {
                    for (Map.Entry<String, Object> parameterJson : result.entrySet()) {
                        if (parameterJson.getValue() != null) {
                            postParameters.add(new BasicNameValuePair(parameterJson.getKey(), parameterJson.getValue().toString()));
                        } else {
                            postParameters.add(new BasicNameValuePair(parameterJson.getKey(), StringUtils.EMPTY));
                        }
                    }
                }
            }
            return new UrlEncodedFormEntity(postParameters, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            logger.error("Unsupported Encoding UTF-8", e);
            throw new ExecuteActionException("Unsupported Encoding UTF-8");
        } catch (Exception e) {
            logger.error("Error in build parameters", e);
            throw new ExecuteActionException("Error in build parameters");
        }
    }

    @SuppressWarnings("unchecked")
    private void setHeaders(HttpRequestBase httpPost) {
        if (mapConfiguration.get(EXECUTION_TYPE_BASIC_AUTH.value) instanceof Map) {
            Map<String, String> basicAuthMap = (Map<String, String>) mapConfiguration.get(EXECUTION_TYPE_BASIC_AUTH.value);
            String username = basicAuthMap.get(EXECUTION_TYPE_BASIC_AUTH_USERNAME.value);
            String password = basicAuthMap.get(EXECUTION_TYPE_BASIC_AUTH_PASSWORD.value);
            if (!Utilities.isEmptyOrNull(username) && !Utilities.isEmptyOrNull(password)) {
                String auth = String.valueOf(
                        basicAuthMap.get(EXECUTION_TYPE_BASIC_AUTH_USERNAME.value))
                        + ":"
                        + String.valueOf(basicAuthMap.get(EXECUTION_TYPE_BASIC_AUTH_PASSWORD.value));

                String authHeader = "Basic " + new String(Base64.encodeBase64(auth.getBytes(Charset.forName("UTF-8"))), Charset.forName("UTF-8"));
                httpPost.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
            }
        }
        if (mapConfiguration.get(EXECUTION_TYPE_HEADERS.value) instanceof Map) {
            Map<String, String> headers = (Map<String, String>) mapConfiguration.get(EXECUTION_TYPE_HEADERS.value);
            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> header : headers.entrySet()) {
                    httpPost.addHeader(header.getKey(), header.getValue());
                }
            }
        }
    }

    private URI getURI() throws ExecuteActionException {
        try {
            return new URI(String.valueOf(mapConfiguration.get(EXECUTION_TYPE_URL.value)));
        } catch (URISyntaxException e) {
            throw new ExecuteActionException("Error in configuration url ", e);
        }
    }

    private void execute(HttpRequestBase httpPost) throws ExecuteActionException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse responseHttp = null;
        try {
            responseHttp = httpClient.execute(httpPost);
            statusCode = responseHttp.getStatusLine().getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                Header[] allHeaders = responseHttp.getAllHeaders();
                StringBuilder stringBuilder = new StringBuilder();
                for (Header header : allHeaders) {
                    stringBuilder.append(header.getName()).append(" : ")
                            .append(header.getValue()).append("\n");
                }
                response = stringBuilder.toString();
            } else {
                HttpEntity entity = responseHttp.getEntity();
                if (entity != null) {
                    InputStream inputStream = entity.getContent();
                    setError(inputStream);
                } else {
                    logger.error("Response without content");
                }
            }
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException) {
                StringWriter errors = new StringWriter();
                e.printStackTrace(new PrintWriter(errors));
                setError(errors.toString());
                throw new ExecuteActionException(e);
            }
            throw new ExecuteActionException("Error in get content response", e);
        } finally {
            try {
                if (responseHttp != null) {
                    responseHttp.close();
                }
            } catch (IOException e) {
                logger.info("Cannot close response", e);
            }
        }
    }

    private void setResponse(InputStream inputStream) throws ExecuteActionException {
        response = getInputStreamToString(inputStream);
    }

    private void setError(InputStream inputStream) throws ExecuteActionException {
        error = getInputStreamToString(inputStream);
    }

    private void setError(String errorString) throws ExecuteActionException {
        error = errorString;
    }

    private String getInputStreamToString(InputStream inputStream) throws ExecuteActionException {
        try {
            return IOUtils.toString(inputStream, Charsets.UTF_8);
        } catch (IOException e) {
            throw new ExecuteActionException("Error with transform response to string", e);
        }
    }

    public String getResponse() {
        return response;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getError() {
        return error;
    }
}
