package com.tierconnect.riot.iot.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.appcore.utils.MlConfiguration;
import com.tierconnect.riot.iot.entities.exceptions.MLModelException;
import com.tierconnect.riot.iot.services.MlModelService;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by alfredo on 9/13/16.
 */

@Path("/modelUploadJars")
@Api("/modelUploadJars")
public class MlUploadJarController {

    private static Logger logger = Logger.getLogger(MlUploadJarController.class);

    /**
     *
     * Query example (using curl):
     *
     * curl \
     * -H 'api_key: root' \
     * -F 'jarFile=@build/libs/riot-ml-all.jar' \
     * localhost:8080/riot-core-services/api/modelUploadJars
     *
     * Response example:
     *
     * {
     *   "id" : "b37b1979-4d68-46b7-983f-cf72b0ad0d61"
     * }
     *
     * @param input multipart form data
     * @return
     * @throws IOException
     * @throws NonUniqueResultException
     * @throws UserException
     */
    @POST
    @Path("/")
    @Consumes("multipart/form-data")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value={"mlBusinessModel:i"})
    @ApiOperation(position = 1, value = "Upload JAR file")
    public Response upload(MultipartFormDataInput input) {

        Response response;

        try {

            // Get jar file
            Map<String, List<InputPart>> uploadForm = input.getFormDataMap();
            InputPart inputPart = uploadForm.get("jarFile").get(0);
            MultivaluedMap<String, String> header = inputPart.getHeaders();
            String jarFileName = getFileName(header);
            InputStream inputStream = inputPart.getBody(InputStream.class, null);

            // Upload jar file to riot-core-services
            MlModelService service = new MlModelService();
            String jarId = service.uploadJarToCoreServices(jarFileName, inputStream);

            Map<String, Object> map = new HashMap<>();
            map.put("id", jarId);
            response = RestUtils.sendOkResponse(map);

        } catch (MLModelException | IOException e) {
            response = RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }

        return response;

    }


    /**
     *
     * Query example:
     *
     * GET /modelUploadJars/b37b1979-4d68-46b7-983f-cf72b0ad0d61
     *
     * Response example:
     *
     * Response:
     * {
     *   "jarFileName": "riot-ml-all.jar",
     *   "modelInputs": [
     *     { "name" : "Time", "type" : "Timestamp" },
     *     { "name" : "Zone", "type" : "String" },
     *     { "name" : "Status", "type" : "Boolean" },
     *     { "name" : "Hohoho", "type" : "Integer" }
     *   ]
     * }
     *
     * @param id identifier of the uploaded JAR
     * @return
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(value={"mlBusinessModel:r"})
    @ApiOperation(position = 1, value = "Get details of a specific uploaded JAR")
    public Response getUploadById(@ApiParam(value = "Unique JAR ID") @PathParam("id") String id) {

        Response response;

        try {

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = mapper.readValue(
                    new File(MlConfiguration.property("jars.path") + "/" + id + ".json"),
                    new TypeReference<Map<String, Object>>() {});

            response = RestUtils.sendOkResponse(map);

        } catch (IOException e) {
            response = RestUtils.sendResponseWithCode("Problems with uploaded JAR", 400);
        }

        return response;
    }


    /**
     *
     * source: https://www.mkyong.com/webservices/jax-rs/file-upload-example-in-resteasy/
     *
     * header sample
     * {
     * Content-Type=[image/png],
     * Content-Disposition=[form-data; name="file"; filename="filename.Extension"]
     * }
     *
     **/
    //get uploaded filename, is there a easy way in RESTEasy?
    private String getFileName(MultivaluedMap<String, String> header) {
        String[] contentDisposition = header.getFirst("Content-Disposition").split(";");
        for (String filename : contentDisposition) {
            if ((filename.trim().startsWith("filename"))) {
                String[] name = filename.split("=");
                String finalFileName = name[1].trim().replaceAll("\"", "");
                return finalFileName;
            }
        }
        return "unknown";
    }


}