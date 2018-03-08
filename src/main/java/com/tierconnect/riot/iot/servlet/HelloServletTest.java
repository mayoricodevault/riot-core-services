package com.tierconnect.riot.iot.servlet;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;

/**
 * Created by vealaro on 3/27/17.
 */
@WebServlet("/testServlet")
public class HelloServletTest extends HttpServlet {

    private static Logger logger = Logger.getLogger(HelloServletTest.class);

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("<h1>Hello POST</h1>");
        response.getWriter().println("session=" + request.getSession(true).getId());
        if (request.getInputStream() != null) {
            StringWriter stringWriter = new StringWriter();
            IOUtils.copy(request.getInputStream(), stringWriter, "UTF-8");
            String message = stringWriter.toString();
            logger.info("------------------ TEST -------------------------------");
            logger.info(message);
            logger.info("-------------------------------------------------");
            response.getWriter().println("<br />" + message);
        }
    }
}
