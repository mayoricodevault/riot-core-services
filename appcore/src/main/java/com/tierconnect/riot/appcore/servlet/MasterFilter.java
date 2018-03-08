package com.tierconnect.riot.appcore.servlet;

import org.apache.log4j.Logger;
import org.jboss.resteasy.core.interception.ContainerResponseContextImpl;
import org.jboss.resteasy.core.interception.ResponseContainerRequestContext;
import org.jboss.resteasy.plugins.server.servlet.HttpServletInputMessage;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.*;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by agutierrez on 10/30/2014.
 */
@Provider
public class MasterFilter implements
        javax.ws.rs.container.ContainerRequestFilter, ContainerResponseFilter {
    static Logger logger = Logger.getLogger(MasterFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        TransactionJAXRSFilter.before(requestContext);
        SecurityFilter.before(requestContext);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        SecurityFilter.after();
        List<Integer> okResponses = Arrays.asList(200, 201, 204);
        if (!okResponses.contains( ((ContainerResponseContextImpl) responseContext).getJaxrsResponse().getStatus())) {
            Map<String, Object> infoMap = new HashMap<>();
            String url = ((HttpServletInputMessage) ((ResponseContainerRequestContext) requestContext).getHttpRequest()).getUri().getRequestUri().toString();
            String errorMsg = "";
            try {
                errorMsg = ((ContainerResponseContextImpl)responseContext).getJaxrsResponse().getEntity().toString();
            } catch (Exception e) {
                logger.debug("Error getting message error");
            }
            infoMap.put("Request URL", "[" + URLDecoder.decode(url, "UTF-8") + "]");
            infoMap.put("Request Method", "[" + ((HttpServletInputMessage) ((ResponseContainerRequestContext) requestContext).getHttpRequest()).getHttpMethod() + "]");
            try {
                infoMap.put("Body", "[" + convertStreamToString(requestContext.getEntityStream()) + "]");
            } catch (IOException e) {
                infoMap.put("Body", "[ Not available due to problems when converting entity stream to a string ]");
            }
            if (!errorMsg.isEmpty()) {
                infoMap.put("Message", "[" + errorMsg + "]");
            }
            logger.error("Additional information for detected error: " + infoMap);
        }
        TransactionJAXRSFilter.after(requestContext, responseContext);
    }

    /**
     * To convert the InputStream to String
     * @param is Input Stream
     * @return
     * @throws IOException
     */
    public String convertStreamToString(InputStream is) throws IOException {
        // To convert the InputStream to String we use the
        // Reader.read(char[] buffer) method. We iterate until the
        // Reader return -1 which means there's no more data to
        // read. We use the StringWriter class to produce the string.
        if (is != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(
                        new InputStreamReader(is, "UTF-8"));
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                is.close();
            }
            return writer.toString();
        }
        return "";
    }
}
