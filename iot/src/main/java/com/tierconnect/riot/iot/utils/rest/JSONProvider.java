package com.tierconnect.riot.iot.utils.rest;

import com.wordnik.swagger.jaxrs.json.JacksonJsonProvider;
import org.apache.log4j.Logger;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.core.MediaType;

/**
 * Created by cvertiz on 6/30/2015.
 * Class that Overrides JacksonJsonProvider in order to print incoming payload json
 * web.xml registration nedded
 * <context-param>
 *      <param-name>resteasy.providers</param-name>
 *      <param-value>com.tierconnect.riot.iot.utils.rest.JSONProvider,
 *                  [provider2],
 *                  [provider3],
 *                  ...
 *      </param-value>
 * <context-param>
 */
@Provider
public class JSONProvider extends JacksonJsonProvider {

    private static Logger logger = Logger.getLogger(JSONProvider.class);

    /**
     *  Overrides readFrom function from  JacksonJsonProvider class
     *  Function in the middle of request and RestEndPoint.
     *  Prints payload json incoming
     * @param type
     * @param genericType
     * @param annotations
     * @param mediaType
     * @param httpHeaders
     * @param entityStream
     * @return
     * @throws IOException
     */
    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType,
                           MultivaluedMap<String,String> httpHeaders, InputStream entityStream)
            throws IOException{

        //Clone InputStream to avoid NullPointerException
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = entityStream.read(buffer)) > -1 ) {
            baos.write(buffer, 0, len);
        }
        baos.flush();

        //is1 to extracts string in order to print it
        InputStream is1 = new ByteArrayInputStream(baos.toByteArray());

        //is2 to continue process
        InputStream is2 = new ByteArrayInputStream(baos.toByteArray());

        //Print incoming json value
        logger.info("*** JSON=" + convertStreamToString(is1));

        //Continue with process
        return super.readFrom(type, genericType, annotations, mediaType, httpHeaders, is2);
    }

    /**
     * COnverts InputStream to string
     * WARNING: Closes InputString after converting
     * @param is
     * @return
     */
    private String convertStreamToString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is,"UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

}
