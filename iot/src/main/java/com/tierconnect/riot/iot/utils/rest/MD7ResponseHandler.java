package com.tierconnect.riot.iot.utils.rest;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * Created by cfernandez
 * on 3/10/2015.
 */
public class MD7ResponseHandler implements RestClient.ResponseHandler
{

    static Logger logger = Logger.getLogger(MD7ResponseHandler.class);

    private String ip;
    private String imageURI;
    private String fieldName;

    public MD7ResponseHandler(String ip) {
        this.imageURI = "";
        this.ip = ip;
    }

    public MD7ResponseHandler(String ip, String fieldName) {
        this(ip);
        this.fieldName = fieldName;
    }

    @Override
    public void success(InputStream is) {

        // [{"id":"17","entryTime":"2014-10-10 15:38:32","image":"2014-10-10_03-38-40_3.jpg","epc":"ABA000000000000000000875",
        // "gateId":"GATE-1","imagedoc":"2014-10-10_03-38-47-PM.jpg”}]

        ObjectMapper mapper = new ObjectMapper();
        String image = "";
        try
        {
            List<Map<?,?>> objects = mapper.readValue(is, mapper.getTypeFactory().constructCollectionType(List.class, Map.class));

            if (objects != null && objects.size() > 0)
            {
                Map<?, ?> md7Map = objects.get(0);
                if (md7Map.get(fieldName) != null)
                {
                    image = (String)md7Map.get(fieldName);
                    logger.debug("image field: name=" + fieldName + ", value=" + image);

                    // http://<ip>/bio/f/a.jpg
                    if ("image".equals(fieldName)){
                        imageURI = "http://" + ip + "/bio/f/" + image;
                    }
                    else
                    {
                        // http://<ip>/bio/docx/b.jpg
                        if ("imagedoc".equals(fieldName)){
                            imageURI = "http://" + ip + "/bio/docx/" + image;
                        }
                    }
                }
                else{
                    logger.info("field=" + fieldName + " does not exist in MD7 response");
                }
            }
            logger.debug("imageURI=" + imageURI);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void error(InputStream is) {

    }

    public String getImageURI() {
        return imageURI;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
