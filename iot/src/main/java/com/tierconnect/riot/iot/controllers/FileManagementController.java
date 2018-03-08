package com.tierconnect.riot.iot.controllers;

import com.google.common.base.Charsets;
import com.mongodb.MongoException;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.iot.entities.ImportExport;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.services.FileExportService;
import com.tierconnect.riot.iot.services.FileImportService;
import com.tierconnect.riot.iot.services.ImportExportService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.net.ConnectException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pablo on 12/15/14.
 *
 * In charge of importing and exporting of entities to and from CSV files.
 */

@Path("/fileManagement")
@Api("/fileManagement")
public class FileManagementController {
    static Logger logger = Logger.getLogger(FileManagementController.class);

    //holds the form. Updated each request
    private Map<String, List<InputPart>> uploadForm;
    private FileImportService fis;

    @POST
    @Path("/import/{type}/{id}")
    @Consumes("multipart/form-data")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 1, value = "Import entities from a CSV file")
    public Response import_(@PathParam("type") String typeStr,@PathParam("id") Long id,
                            @QueryParam("thingTypeCode")String thingTypeCode,
                                  @QueryParam("runRules") Boolean runRules, MultipartFormDataInput input)
            throws IOException, NonUniqueResultException, UserException {
        String typeImport;
        if(typeStr.equals("thing")){
            typeImport=typeStr+ thingTypeCode;
        }else{
            typeImport=typeStr;
        }
        if (System.getProperties().containsKey("importing"+typeImport+"Status")) {
            throw new UserException("Services import process is running for: " + typeImport);
        }
        try {
            System.getProperties().put("importing" +typeImport + "Status", "active");


            long timeStamp = System.currentTimeMillis();
            FileImportService.Type type = FileImportService.Type.valueOf(typeStr.toUpperCase());

            logger.info("thingTypeCode: " + thingTypeCode);
            logger.info("runRules: " + runRules);

            if (type == FileImportService.Type.USER) {
                fis = new FileImportService((User)SecurityUtils.getSubject().getPrincipal());
            } else if (type == FileImportService.Type.THING) {
                ThingType tt = ThingTypeService.getInstance().getByCode(thingTypeCode);
                fis = new FileImportService((User)SecurityUtils.getSubject().getPrincipal(), tt, runRules);
            } else {
                fis = new FileImportService((User)SecurityUtils.getSubject().getPrincipal());
            }
            uploadForm = input.getFormDataMap();

            logger.info("logger " + uploadForm.keySet());

            //run files
            String[] results = processFile(type, id);

            //put results in map
            Map<String, Object> resultsMap = new LinkedHashMap<>();
            resultsMap.put("results", results);
            logger.info("Done import [" + type + "] in " + (System.currentTimeMillis() - timeStamp) + " ms.");
            return RestUtils.sendOkResponse(resultsMap);
        } catch (UserException e) {
            throw e;
        } catch (Exception e) {
            logger.info("Other exception");
            throw e;
        } finally {
            System.getProperties().remove("importing"+typeImport+"Status");
        }
    }

    @GET
    @Path("/import/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 1, value = "Insert an import entity")
    public Response insertImport(@PathParam("type") String type){

        Map<String, Object> resultMap = ImportExportService.getInstance().createImportEntity(type);
        if (resultMap.get("status").equals("error")) {
            return RestUtils.sendBadResponse(resultMap.get("message").toString());
        }else{
            return RestUtils.sendOkResponse(resultMap);
        }

    }

    @PATCH
    @Path("/export/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 1, value = "Insert an export entity")
    public Response insertExport(@PathParam("type") String type){

        Map<String, Object> resultMap = ImportExportService.getInstance().createExportEntity(type);
        if (resultMap.get("status").equals("error")) {
            return RestUtils.sendBadResponse(resultMap.get("message").toString());
        }else{
            return RestUtils.sendOkResponse(resultMap);
        }

    }



    @POST
    @Path("/import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 1, value = "Import entities from a CSV file by URL")
    public Response import_(Map<String,Object> input)
            throws IOException, NonUniqueResultException
    {
        // input validation
        String typeStr = null;
        String thingTypeCode = null;
        Boolean runRules = null;
        String url = null;
        if (input != null && !input.isEmpty()) {
            typeStr = (String) input.get("type");
            thingTypeCode = (String) input.get("thingTypeCode");
            runRules = (Boolean) input.get("runRules");
            url = (String) input.get("url");
        }
        if (System.getProperties().containsKey("importing"+thingTypeCode+"Status")) {
            throw new UserException("Services is importing: " + thingTypeCode);
        }
        if (typeStr == null || url == null){
            throw new UserException("Invalid input parameters");
        }

        try {
            System.getProperties().put("importing"+thingTypeCode+"Status", "active");
            long timeStamp = System.currentTimeMillis();
            FileImportService.Type type = null;
            try {
                type = FileImportService.Type.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new UserException("Invalid input parameter: " + typeStr, e);
            }
            logger.info("thingTypeCode: " + thingTypeCode);
            logger.info("runRules: " + runRules);

            if (type == FileImportService.Type.THING) {
                ThingType tt = ThingTypeService.getInstance().getByCode(thingTypeCode);
                if (tt == null)
                    throw new UserException("Invalid input parameter: " + thingTypeCode);
                fis = new FileImportService((User) SecurityUtils.getSubject().getPrincipal(), tt, runRules);
            } else {
                fis = new FileImportService((User) SecurityUtils.getSubject().getPrincipal());
            }

            Map<String, Object> resultsMap = new LinkedHashMap<>();
            //run files
            String[] results = processFile(type, url);
            //put results in map
            resultsMap.put("results", results);
            logger.info("Done import thing in " + (System.currentTimeMillis() - timeStamp) + " ms.");
            return RestUtils.sendOkResponse(resultsMap);
        } catch (MongoException me) {
            return RestUtils.sendBadResponse("Mongo error");
        } catch (ConnectException ce){
            return RestUtils.sendBadResponse("Connection error");
        }  finally {
            System.getProperties().remove("importing"+thingTypeCode+"Status");
        }
    }


    @GET
    @Path("/export/{type}/{id}")
    @Produces("text/plain")
    @RequiresAuthentication
    @ApiOperation(position = 1, value = "Export entities to a CSV")
    public Response export(@PathParam("type") String type, @PathParam("id") Long id, @QueryParam("code") String code, @QueryParam("encoding") @DefaultValue("ISO-8859-1") String encoding)
            throws IOException {
        logger.info("Going to export " + type + ", code " + code + ", encoding " + encoding + ", default SERVER " + Charset.defaultCharset());
        long timeStamp = System.currentTimeMillis();
        //Prepare a file object with file to return
        FileExportService fes = null;
        Response response;

        try {
            ThingType tt = ThingTypeService.getInstance().getByCode(code);

            fes = new FileExportService(tt);
            Charset charset;
            try {
                charset = Charset.forName(encoding);
            } catch (UnsupportedCharsetException e) {
                logger.error("error Charset invalid " + encoding);
                charset = Charsets.ISO_8859_1;
            }

            File fileExport = fes.export(FileExportService.Type.valueOf(type.toUpperCase()), charset, id);
            Response.ResponseBuilder rb = Response.ok(fileExport);
            if (fileExport.length() == 0){
                throw new UserException("It is not possible to complete Export process");
            }
            rb.type(MediaType.TEXT_PLAIN + ";charset=" + charset.toString());
            rb.header("Content-Disposition", "attachment; filename=\"" + type.toLowerCase() + ".csv\"");
            response = rb.build();
        } catch (UserException e) {
            response = RestUtils.sendBadResponse(e.getMessage());
        } catch (NonUniqueResultException e) {
            response = RestUtils.sendBadResponse(
                    String.format("Thing type code [%s] is not unique", code));
        } catch (IllegalArgumentException e) {
            logger.warn("Error exporting", e);
            response = RestUtils.sendBadResponse(e.getMessage());
        }
        logger.info("Done export [" + type + "] in " + (System.currentTimeMillis() - timeStamp) + " ms.");
        return response;
    }

    /**
     * Private helper method that process a file at a time. Get the file stream from the form
     * and passes it to the service
     */
    private String[] processFile(FileImportService.Type type, Long id) throws IOException
    {
        String[] results = null;
        List<InputPart> inputParts = uploadForm.get(type.name().toLowerCase());

        if (inputParts != null )
        {
            for (InputPart inputPart : inputParts)
            {
                String fileName = "";
                for (String content : inputPart.getHeaders().get("Content-Disposition").get(0).split(";")) {
                    if (content.trim().split("=")[0].equals("filename")) {
                        fileName = content.split("=")[1].replace("\"", "");
                        break;
                    }
                }
                //convert the uploaded file to inputstream
                InputStream inputStream = inputPart.getBody(InputStream.class,null);

                results = fis.parse(new BufferedReader(new InputStreamReader(inputStream,"UTF-8")), type, id, fileName);
            }
        }

        return results;
    }

    private String[] processFile(FileImportService.Type type, String filePath) throws IOException
    {
        String[] results = null;
        try {
            if (filePath != null && !filePath.isEmpty()) {
                String ext = FilenameUtils.getExtension(filePath);
                if (ext != null && ext.equals("csv")) {
                    URL url = new URL(filePath);
                    BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
                    results = fis.parse(br, type,null, filePath);
                } else {
                    throw new UserException("File must be csv");
                }
            }
        } catch (FileNotFoundException e){
            throw new UserException(filePath + " cannot be opened", e);
        }
        return results;
    }

}
