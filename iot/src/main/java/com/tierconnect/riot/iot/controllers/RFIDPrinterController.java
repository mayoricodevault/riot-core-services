package com.tierconnect.riot.iot.controllers;

import com.google.common.base.Charsets;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.dao.util.FormulaUtil;
import com.tierconnect.riot.iot.entities.ReportEntryOption;
import com.tierconnect.riot.iot.entities.ReportEntryOptionProperty;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.iot.utils.RFIDPrinterUtil;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import scala.collection.mutable.StringBuilder;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by fflores on 7/3/2015.
 */
@Path("/RFIDPrinter")
@Api("/RFIDPrinter")
public class RFIDPrinterController {
    private static Logger logger = Logger.getLogger(RFIDPrinterController.class);

    @POST
    @Path("/encodeSerial")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 1, value = "print tag")
    public Response encodeSerial(List<Map<String, Object>> inList) {
        logger.info("Starting to encode tags...");
        return RestUtils.sendOkResponse(RFIDPrinterService.getInstance().encodeSerial(inList,Boolean.FALSE));
    }

    @PATCH
    @Path("/export")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces("text/plain")
    @RequiresAuthentication
    @ApiOperation(position = 1, value = "Export entities to a CSV")
    public Response export(List<Map<String, Object>> inList)
            throws IOException
    {
        logger.info("Starting to export ZPL... ");
        Response response;
        String encoding = "ISO-8859-1";
        try
        {
            Charset charset = Charset.forName(encoding);
            Map<String, Object> zplMap = RFIDPrinterService.getInstance().encodeSerial(inList,Boolean.TRUE);
            StringBuilder sb = new StringBuilder();
            if (zplMap != null){
                if (zplMap.get("zplPrintedTags") != null){
                    List<String> zplPrintedTags = (List) zplMap.get("zplPrintedTags");
                    for (String zpl : zplPrintedTags){
                        sb.append(zpl);
                    }
                }
            }
            File file = File.createTempFile("export", ".zpl");
            file.deleteOnExit();
            FileOutputStream fileStream = new FileOutputStream(file);
            OutputStreamWriter bw = new OutputStreamWriter(fileStream, charset);
            bw.write(sb.toString());
            bw.close();
            Response.ResponseBuilder rb = Response.ok(file);
            rb.type(MediaType.TEXT_PLAIN + ";charset=" + charset.toString());
            rb.header("Content-Disposition", "attachment; filename=\"" + "exported" +  ".zpl\"");
            response  = rb.build();
        } catch (UserException e) {
            response = RestUtils.sendBadResponse(e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("Error exporting", e);
            response = RestUtils.sendBadResponse(e.getMessage());
        }
        return response;
    }

}
