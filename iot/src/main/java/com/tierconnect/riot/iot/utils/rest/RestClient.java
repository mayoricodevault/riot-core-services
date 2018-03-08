package com.tierconnect.riot.iot.utils.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;
import org.apache.http.protocol.HTTP;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.*;

/**
 * Created by pablo on 3/9/15.
 *
 * Handles the communication to rest endpoints.
 */
public class RestClient
{
    private static Logger logger = Logger.getLogger( RestClient.class );

    private static RestClient client;

    protected RestClient() {

    }

    public static RestClient instance() {
        if (client == null)
        {
            client = new RestClient();
        }
        return client;
    }

    /**
     * make a get call with header parameters
     * @param uri endpoint url in string format
     * @param rh response handler
     * @param headers header parameters like api key
     * @throws RestCallException if there is an error during the call
     */
    public void get(String uri, ResponseHandler rh , Map<String, String> headers) throws RestCallException
    {
        logger.info("executing http GET uri=" + uri);

        HttpGet request = new HttpGet(uri);
        //todo populate headers
        call(request,  rh);
    }

    /**
     * make a get call
     * @param uri enpoint url
     * @param rh response handler
     * @throws RestCallException
     */
    public void get(String uri, ResponseHandler rh) throws RestCallException
    {
        get(uri, rh, null);
    }

    /**
     * make a get call with header parameters
     * @param uri endpoint
     * @param rh response handler
     * @param headers header parameters like api key
     * @throws RestCallException if there is an error during the call
     */
    public void get(URI uri, ResponseHandler rh , Map<String, String> headers) throws RestCallException
    {
        logger.info("executing http GET uri=" + uri);

        HttpGet request = new HttpGet(uri);
        //todo populate headers
        call(request, rh);
    }

    /**
     * make a get call to endpoint
     * @param uri endpoint
     * @param rh response handler
     * @throws RestCallException if there is an error during the call
     */
    public void get(URI uri, ResponseHandler rh) throws RestCallException
    {
        get(uri,  rh, null);
    }

    public void put(URI uri, String body, ResponseHandler rh) throws RestCallException
    {
        logger.info("executing http PUT uri=" + uri);

        //todo send headers for text/plain
        StringEntity entity = new StringEntity( body, ContentType.create("application/json", "UTF-8") );
        HttpPut request = new HttpPut(uri);
        request.setEntity( entity );
        call(request, rh);
    }

    public void post(URI uri, Map<String, String> parameters, ResponseHandler rh) throws RestCallException
    {
        try {
            List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            for (String key : parameters.keySet()) {
                urlParameters.add(new BasicNameValuePair(key, parameters.get(key)));
            }

            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(new TextEncodedFormEntity(urlParameters));
            call(httpPost,  rh);
        }
        catch (UnsupportedEncodingException e) {
            logger.error("Error in post ", e);
            throw new RestCallException(e);
        }
    }

    public void post(URI uri, Map<String, Object> parameters, Map<String,String> headers, ResponseHandler rh) throws RestCallException
    {
        try {
            ObjectMapper mapper = new ObjectMapper();
            final HttpPost httpPost = new HttpPost(uri);
            httpPost.setHeader(HTTP.CONTENT_TYPE, String.valueOf(headers.get(HTTP.CONTENT_TYPE)));
            httpPost.setEntity(new StringEntity(mapper.writeValueAsString(parameters)));
            logger.info("HTTPPOST----->>>  " + mapper.writeValueAsString(parameters));
            call(httpPost,  rh);
        }
        catch (UnsupportedEncodingException e) {
            logger.error("Error in post ", e);
            throw new RestCallException(e);
        } catch (JsonProcessingException e) {
            logger.error("Error in post ", e);
            throw new RestCallException(e);
        }
    }

    public void post(URI uri, String filename, ResponseHandler rh) throws RestCallException
    {
            File file = new File(filename);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(new FileEntity(file));
            call(httpPost,  rh);
    }



    private void call(HttpRequestBase request, ResponseHandler rh) throws RestCallException
    {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpResponse response = null;

        try {
            response = httpClient.execute(request);

            StatusLine statusLine = response.getStatusLine();
            HttpEntity entity = response.getEntity();
            if (entity == null)
            {
                throw new RestCallException("Response contains no content");
            }

            InputStream is = entity.getContent();

            int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300)
            {
                rh.success(is);
            }
            else
            {
                rh.error(is);
            }
        }
        catch (IOException e)
        {
            throw new RestCallException("IO exception", e);
        }
        finally
        {
            try
            {
                if (response != null)
                    response.close();
            }
            catch (IOException e)
            {
                logger.info("Cannot close response" + e);
            }
        }
    }



    public static interface ResponseHandler
    {
        public void success (InputStream is) throws RestCallException, IOException;
        public void error (InputStream is) throws RestCallException, IOException;

    }

}
