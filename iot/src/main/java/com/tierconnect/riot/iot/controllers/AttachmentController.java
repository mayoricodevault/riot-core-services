package com.tierconnect.riot.iot.controllers;

/**
 * Created by rchirinos on 10/7/2015.
 */


import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.utils.EncryptionUtils;
import com.tierconnect.riot.iot.entities.Attachment;
import com.tierconnect.riot.iot.reports_integration.TranslateResult;
import com.tierconnect.riot.iot.services.AttachmentService;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.annotation.Generated;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

//@Path("/attachment")
//@Api("/attachment")
@Generated("com.tierconnect.riot.appgen.service.GenControllerBase")
public class AttachmentController extends AttachmentControllerBase
{

	/*This service is to get more information of the data*/
	@POST
	@Path("/uploadAttachment")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@RequiresAuthentication
	@ApiOperation(position=1, value="Upload a File", notes="Upload a file temporarily, " +
			"the returned ID should be used in the  Create/Update Thing method in a specific UDF of the  " +
			"'Attachment' type")
	@ApiResponses(
			value = {
					@ApiResponse(code = 200, message = "Ok"),
					@ApiResponse(code = 201, message = "Created"),
					@ApiResponse(code = 400, message = "Bad Request"),
					@ApiResponse(code = 403, message = "Forbidden"),
					@ApiResponse(code = 500, message = "Internal Server Error")
			}
	)
	public Response createAttachmentMultipart(
			  @ApiParam(value = "File to be uploaded.")   MultipartFormDataInput input
			, @ApiParam(value = "Additional comments of the file.")
				@QueryParam("comments") String comment
			, @ApiParam(value = "Operation over file, it accepts: 'override'")
				@QueryParam("operationOverFile") String operationOverFile
			, @ApiParam(value = "ID of the thing") @QueryParam("thingId") Long thingId
			, @ApiParam(value = "ID of the thingTypeFieldID") @QueryParam("thingTypeFieldId") Long thingTypeFieldId
			, @ApiParam(value = "Path of Attachments configured in the Thing Type") @QueryParam("pathAttachments") String pathAttachments
			, @ApiParam(value = "Hierarchical name of the group") @QueryParam("hierarchicalNameGroup") String hierarchicalNameGroup
			, @ApiParam(value = "ID's of temporary attachments (Optional)") @QueryParam("attachmentTempIds") String attachmentTempIds
	) {

		Map<String, Object> result = null;
		User user = (User) SecurityUtils.getSubject().getPrincipal();
		try{
			//get the folder path of the attachments
			String folderAttachments = AttachmentService.getInstance().getPathDirectory(
					pathAttachments
					, hierarchicalNameGroup
					, thingId
					, thingTypeFieldId);
			//Save in temporary table
			result = AttachmentService.getInstance().saveFileInTempDB(
					comment
					, user.getId().toString()
					, input, operationOverFile
					, folderAttachments
					, attachmentTempIds);
		}catch(Exception e)
		{
			return RestUtils.sendResponseWithCode(e.getMessage(), 400);
		}
		return RestUtils.sendOkResponse( result );
	}

    @GET
    @Path("/download")
    @RequiresAuthentication
    @ApiOperation(position = 2, value = "Download file")
    public Response getFile(@QueryParam("pathFile") String pathFile) throws Exception {
        // compatibility
        File file = new File(pathFile);
        if (!file.exists()) {
            pathFile = EncryptionUtils.decrypt(TranslateResult.KEY_ID_IMAGE, TranslateResult.INIT_VECTOR_ID_IMAGE, pathFile);
            if (pathFile == null) {
                throw new UserException("File not exists");
            }
            file = new File(pathFile);
            if (!file.exists()) {
                throw new UserException("File not exists");
            }
        }
        String[] splitName = pathFile.split("\\.");
        String extension = splitName[splitName.length - 1];
        String[] splitFileName = pathFile.split("\\/");
        String fileName = splitFileName[splitFileName.length - 1];
        Response.ResponseBuilder response = Response.ok(file).type("application/" + extension);
        response.header("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        response.header("Content-Length", file.length());
        return response.build();
    }

	@DELETE
	@Path("/{pathFile}")
	@RequiresAuthentication
	@ApiOperation(position = 2, value = "Download file")
	public Response deleteFile(@PathParam ("pathFile") String pathFile) throws Exception
	{
		Map<String, Object> result = null;
		try{
			AttachmentService.getInstance().removeFile( pathFile );
		}catch(Exception e)
		{
			return RestUtils.sendResponseWithCode(e.getMessage(), 400);
		}
		return RestUtils.sendDeleteResponse();
	}

	@GET
	@Path("/validate")
	@RequiresAuthentication
	@ApiOperation(position = 21, value = "Validate File Path")
	public Response validatePathFile(@QueryParam ("pathFile") String pathFile) throws Exception
	{
		try {
			Map<String, Object> result = GroupService.getInstance().validatePathFile( pathFile );
			return RestUtils.sendOkResponse(result);
		} catch(Exception e) {
			return RestUtils.sendResponseWithCode(e.getMessage(), 400);
		}
	}

	@POST
	@Path("/buildJson")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@RequiresAuthentication
	@ApiOperation(position=1, value="Upload File")
	public Response buildJson(@QueryParam("pathFile") String pathFile)
	{
		Map<String, Object> result = null;
		User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
		try{
			//Validation
			if((pathFile == null) || (pathFile.trim().isEmpty())) {
				return RestUtils.sendResponseWithCode("Parameter 'pathFile' has to have a value path.", 400);
			}
			Map<String, Object> validatePath = GroupService.getInstance().validatePathFile( pathFile );
			if( validatePath.get( "errorCode" ).toString().equals( "NOK" ) )
			{
				return RestUtils.sendResponseWithCode(validatePath.get( "errorMessage" ).toString(), 400);
			}
			//Create Data
			List<Map<String, Object>> lstFiles = new ArrayList<Map<String, Object>>(  );
			lstFiles = AttachmentService.getInstance().getFilesOfDirectory( pathFile,lstFiles );
			for(Object file0:lstFiles)
			{
				Map<String, Object> fileMap = (Map<String, Object>) file0;
                Stack<Long> recursivelyStack = new Stack<>();
				result = AttachmentService.getInstance().processBuildAttachment(recursivelyStack, fileMap, currentUser);
			}

		}catch(Exception e)
		{
			return RestUtils.sendResponseWithCode(e.getMessage(), 400);
		}
		return RestUtils.sendOkResponse( result );
	}

	/*
	* Add to public Map
	* */
	public void addToPublicMap(Attachment attachment, Map<String,Object> publicMap, String extra )
	{
		User user = attachment.getUploadedBy();
		publicMap.put( "uploadedBy", user.getFirstName()+" "+user.getLastName() );
		publicMap.put("date", new SimpleDateFormat("YYYY-MM-dd").format(attachment.getDateUploaded()));
	}
}
