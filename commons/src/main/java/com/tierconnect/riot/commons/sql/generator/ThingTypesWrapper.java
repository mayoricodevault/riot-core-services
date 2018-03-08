package com.tierconnect.riot.commons.sql.generator;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.commons.dtos.ThingTypeDto;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThingTypesWrapper {

    private static final Logger logger = Logger.getLogger(ThingTypesWrapper.class);

    public int total;

    public List<ThingTypeDto> results;

    private Map<Long, ThingTypeDto> map = new HashMap<Long, ThingTypeDto>();

    public static ThingTypesWrapper parse(String json)
    throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper m = new ObjectMapper();
        return m.readValue(json, ThingTypesWrapper.class);
    }

    public static ThingTypesWrapper parse(String host,
                                          int port,
                                          String contextPath,
                                          String apikey)
    throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper m = new ObjectMapper();
        return m.readValue(getThingTypeJson(host, port, contextPath, apikey),
                           ThingTypesWrapper.class);
    }

    public ThingTypeDto getThingTypeWrapper(long thingTypeId) {
        ThingTypeDto ttw = map.get(thingTypeId);
        if (ttw == null) {
            ttw = findThingTypeWrapper(thingTypeId);
            map.put(thingTypeId, ttw);
        }
        return ttw;
    }

    private ThingTypeDto findThingTypeWrapper(long thingTypeId) {
        for (ThingTypeDto ttw : results) {
            if (ttw.id == thingTypeId) {
                return ttw;
            }
        }
        return null;
    }


    public static String getThingTypeJson(String host,
                                          int port,
                                          String contextPath,
                                          String apikey) {
        String json = null;

        CloseableHttpClient httpclient = HttpClients.createDefault();

        URIBuilder ub = new URIBuilder();
        ub.setScheme("http");
        ub.setHost(host);
        ub.setPort(port);
        ub.setPath(contextPath + "/api/thingType/");
        ub.addParameter("pageSize", "-1");

        URI uri;

        CloseableHttpResponse response = null;
        try {
            uri = ub.build();

            HttpGet httprequest = new HttpGet(uri);
            httprequest.addHeader("Api_key", apikey);

            logger.debug("executing http GET uri=" + uri);
            long t1 = System.currentTimeMillis();

            response = httpclient.execute(httprequest);
            long t2 = System.currentTimeMillis();

            logger.debug("got response delt=" + (t2 - t1));

            logger.debug("getting content");
            InputStream is = response.getEntity().getContent();

            StringWriter writer = new StringWriter();
            IOUtils.copy(is, writer, "UTF-8");
            json = writer.toString();

            logger.debug("parsing");
            t1 = System.currentTimeMillis();

            t2 = System.currentTimeMillis();
            logger.debug("done parsing delt=" + (t2 - t1));
        } catch (IOException | URISyntaxException e) {
            logger.error("error", e);
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                    logger.error("error", e);
                }
            }
        }

        logger.debug("done");

        return json;
    }
}
