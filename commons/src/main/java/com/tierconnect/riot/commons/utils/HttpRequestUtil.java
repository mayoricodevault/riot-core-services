package com.tierconnect.riot.commons.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.tierconnect.riot.commons.sparkrest.FailedSparkRequestException;
import joptsimple.internal.Strings;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicResponseHandler;
import org.jose4j.json.internal.json_simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

import static org.apache.commons.lang.StringUtils.EMPTY;

/**
 * HttpRequestUtil util class.
 */
public class HttpRequestUtil {

    //private Map<String, Object> infoRequestMap;
    //private String infoRequestString;

    private HttpServletRequest httpServletRequest;
    private Map<String, Object> body;

    public HttpRequestUtil() {
    }

    public HttpRequestUtil(HttpServletRequest request, Map<String, Object> body) {

        this.httpServletRequest = request;
        this.body = body;
    }

    /**
     * Executes a http method and get the response.
     *
     * @param client      the client
     * @param httpRequest the http request
     * @param clazz       the class
     * @param <T>         the type
     * @return the response
     * @throws FailedSparkRequestException SparkException with contains the error message.
     */
    public static <T> T executeHttpMethodAndGetResponse(HttpClient client,
                                                        HttpRequestBase httpRequest,
                                                        final Class<T> clazz)
            throws FailedSparkRequestException {
        Preconditions.checkNotNull(client, "The client is null");
        Preconditions.checkNotNull(httpRequest, "The httpRequest is null");
        Preconditions.checkNotNull(clazz, "The clazz is null");

        T response;

        try {
            final String stringResponse = client.execute(httpRequest, new BasicResponseHandler());
            final ObjectMapper mapper = new ObjectMapper();
            if (stringResponse != null) {
                response = mapper.readValue(stringResponse, clazz);
            } else {
                throw new FailedSparkRequestException("Received empty string response");
            }
        } catch (Exception e) {
            throw new FailedSparkRequestException(e);
        } finally {
            httpRequest.releaseConnection();
        }
        if (response == null) {
            throw new FailedSparkRequestException("An issue occurred with the cluster's response.");
        }
        return response;
    }

    /**
     * This method gets the main information of the request in JSON
     *
     * @return {@link String} with all request information and body information in JSON format.
     */
    public String getInfoRequestJSON() {
        if (this.httpServletRequest == null) {
            return "";
        }
        return new JSONObject(this.getInfoRequestMap()).toJSONString();
    }

    /**
     * This method gets the main information of the request in MAP
     *
     * @return {@link Map}<{@link String},{@link Object}> with all request information and body information in MAP
     * format.
     */
    public Map<String, Object> getInfoRequestMap() {
        if (this.httpServletRequest == null) {
            return null;
        }
        Map<String, Object> infoRequestMap = new HashMap<>();
        Map<String, Object> header = new HashMap<>();
        for (String key : Collections.list(this.httpServletRequest.getHeaderNames())) {
            if (key.equals("host") || key.equals("origin") || key.equals("user-agent") || key.equals("token") || key
                    .equals("utcoffset")) {
                header.put(key, this.httpServletRequest.getHeader(key));
            }
        }

        Map<String, Object> input = new HashMap<>();
        input.put("queryString", this.httpServletRequest.getQueryString());
        input.put("body", this.body);
        infoRequestMap.put("input", input);
        infoRequestMap.put("header", header);
        infoRequestMap.put("method", this.httpServletRequest.getMethod());
        infoRequestMap.put("pathTranslated", this.httpServletRequest.getPathTranslated());
        infoRequestMap.put("serverName", this.httpServletRequest.getServerName());
        infoRequestMap.put("serverPort", this.httpServletRequest.getServerPort());
        infoRequestMap.put("remoteAddr", this.httpServletRequest.getRemoteAddr());
        infoRequestMap.put("remoteHost", this.httpServletRequest.getRemoteHost());
        infoRequestMap.put("remotePort", this.httpServletRequest.getRemotePort());
        infoRequestMap.put("localName", this.httpServletRequest.getLocalName());
        infoRequestMap.put("localAddr", this.httpServletRequest.getLocalAddr());
        infoRequestMap.put("localPort", this.httpServletRequest.getLocalPort());
        return infoRequestMap;
    }

    /**
     * This method gets the main information of the request in STRING
     *
     * @return A {@link String} with contains a log with all request body and request parameters.
     */
    public String getInfoRequestString() {
        String response =  "" ;
        if (this.httpServletRequest == null) {
            return EMPTY;
        }
        List<String> info = new ArrayList<>();
        for (String key : Collections.list(this.httpServletRequest.getHeaderNames())) {
            if (key.equals("host") || key.equals("origin") || key.equals("user-agent") || key.equals("token") || key
                    .equals("utcoffset")) {
                info.add(key + ":" + this.httpServletRequest.getHeader(key));
            }
        }
        info.add("queryString:" + this.httpServletRequest.getQueryString());
        info.add("body:" + this.body);
        info.add("method:" + this.httpServletRequest.getMethod());
        info.add("pathTranslated:" + this.httpServletRequest.getPathTranslated());
        info.add("serverName:" + this.httpServletRequest.getServerName());
        info.add("serverPort:" + this.httpServletRequest.getServerPort());
        info.add("remoteAddr:" + this.httpServletRequest.getRemoteAddr());
        info.add("remoteHost:" + this.httpServletRequest.getRemoteHost());
        info.add("remotePort:" + this.httpServletRequest.getRemotePort());
        info.add("localName:" + this.httpServletRequest.getLocalName());
        info.add("localAddr:" + this.httpServletRequest.getLocalAddr());
        info.add("localPort:" + this.httpServletRequest.getLocalPort());
        response = Strings.join(info, ",");
        response = response.replaceAll("[`\\|&;<>\\$\"'\\s\t\n]", "");
        return response;
    }
}
