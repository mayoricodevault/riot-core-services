package com.tierconnect.riot.iot.servlet;

import com.tierconnect.riot.sdk.dao.NotFoundException;
import com.tierconnect.riot.sdk.dao.UserException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created by vealaro on 4/5/17.
 */
public class HttpServletBase extends HttpServlet {

    protected void handleException(HttpServletResponse response, Exception ex) {
        if (ex instanceof NotFoundException) {
            response.setStatus(Response.Status.NOT_FOUND.getStatusCode());
        } else if (ex instanceof UserException) {
            response.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
        } else {
            response.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
        PrintWriter out;
        try {
            Map res = new HashMap();
            res.put("error", ex.getMessage());
            response.setContentType("application/json");
            out = response.getWriter();
            out.println(toJson(res));
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String toJson(List l) {
        StringBuilder b = new StringBuilder();
        if (l == null) {
            return "null";
        }
        b.append("[");
        int i = 0;
        for (Object a : l) {
            if (i > 0) {
                b.append(", ");
            }
            b.append(toJson(a));
            i++;
        }
        b.append("]");
        return b.toString();
    }

    protected String toJson(Object m) {
        if (m == null) {
            return "null";
        } else if (m instanceof Number) {
            return m.toString();
        } else if (m instanceof Boolean) {
            return m.toString();
        } else if (m instanceof String) {
            return "\"" + m + "\"";
        } else if (m instanceof Collection) {
            return toJson(new ArrayList((Collection) m));
        } else if (m instanceof Map) {
            return toJson((Map) m);
        }
        return "\"" + m.toString() + "\"";
    }


    protected String toJson(Map m) {
        StringBuilder b = new StringBuilder();
        if (m == null) {
            return "null";
        }
        b.append("{");
        int i = 0;
        for (Object a : m.keySet()) {
            if (i > 0) {
                b.append(", ");
            }
            b.append("\"" + a.toString() + "\":" + toJson(m.get(a)));
            i++;
        }
        b.append("}");
        return b.toString();
    }
}
