package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupField;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.FieldService;
import com.tierconnect.riot.appcore.services.GroupFieldService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.appcore.utils.BlobUtils;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.dao.AttachmentDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;

import javax.annotation.Generated;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Blob;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rchirinos on 17/11/2015.
 */
@Generated("com.tierconnect.riot.appgen.service.GenService")
public class AttachmentService extends AttachmentServiceBase
{
	static Logger logger = Logger.getLogger(AttachmentService.class);


	/**
	 * *************************************************************************
	 * This method saves the file in database temporary. It is going to be here
	 * until the thing is created/updated explicitly
	 * ***************************************************************************
	 */
	public Map<String, Object> saveFileInTempDB(
			  String comment
			, String userId
			, MultipartFormDataInput input
	        , String operationOverFile
	        , String folderAttachments
	        , String attachmentTempIds)
			throws Exception
	{
		Map<String, Object> response = new HashMap<>();
		List<Map<String, Object>> data = new ArrayList<>();
		Date dateUpload = new Date();
		List<Map<String, Object>> lstBlobData = BlobUtils.getBlobList( input, "file" );

		//Validations
		ValidationBean valida = validaAttachment(operationOverFile);
		if(valida.isError())
		{
			throw new UserException(valida.getErrorDescription());
		}

		//Register
		if(lstBlobData!=null && lstBlobData.size()>0)
		{
			for(Object blobDataMap: lstBlobData)
			{
				Map<String, Object> blobData = (Map<String, Object>) blobDataMap;
				String fileName = (String) blobData.get( "FileName" );
				Long userIdLong = Long.parseLong( userId != null ? userId : "1" );

				logger.info( "Saving the file temporarily '"+fileName+"' ..." );
				String[] splitExension = fileName.split( "\\." );
				String Extension = splitExension[splitExension.length - 1];
				String size = String.valueOf(blobData.get("Size"));
				Attachment attachment = new Attachment();
				attachment.setFileAttachment( (Blob) blobData.get( "Blob" ) );
				attachment.setComment( comment );
				attachment.setType( Extension );
				attachment.setSize( "null".equalsIgnoreCase(size)? 0: Long.parseLong(size) );
				attachment.setUploadedBy( UserService.getInstance().get( userIdLong ) );
				attachment.setDateUploaded( dateUpload );
				attachment.setOperationOverFile(operationOverFile);
				attachment.setName( fileName );

				//Insert data
				attachment = AttachmentService.getInstance().insert( attachment );
				logger.info( "File '"+fileName+"' saved temporarily" );
				data.add(attachment.publicDataMap());

				/*String newFileName = this.getFileNameAttachment(
						folderAttachments
						, attachment.getName()
						, attachment.getType()
						, attachment.getOperationOverFile()
						, attachmentTempIds);
				if(newFileName==null)
				{
					attachment.setComment("Info: The file was skipped.");
				}else
				{
					attachment.setName( newFileName );
					logger.info( "Final file to save:"+newFileName+"'" );
					//Insert data
					attachment = AttachmentService.getInstance().insert( attachment );
					logger.info( "File '"+fileName+"' saved temporarily" );
					data.add(attachment.publicDataMap());
				}*/

				blobData = null;
				attachment = null;
			}
		}

		response.put("files",data);
		return response;
	}

	/**
	 * Validation for save attachments
	 */
	public ValidationBean validaAttachment(String operation)
	{
		ValidationBean valida = new ValidationBean();
		if(operation!=null)
		{
			if(!operation.equals(Constants.FileOperation.OVERRIDE.value) &&
					!operation.equals(Constants.FileOperation.RENAME.value) &&
					!operation.equals(Constants.FileOperation.SKIP.value) )
			{
				valida.setErrorDescription("Operation value is not valid.");
			}
		}
		return valida;
	}
	/**
	 * Get Max File Upload configuration
	 */
	public Long getMaxFileUpload(Group group)
	{
		Long maxSize = 0L;
		try{
			Field maxSizeField = FieldService.getInstance().selectByName("maxUploadSize");//51200L;
			GroupField groupField = GroupFieldService.getInstance().selectByGroupField(group, maxSizeField);
			maxSize = groupField.getValue()!=null?Long.parseLong(groupField.getValue()):26500L;
		}catch(Exception e)
		{
			logger.error("It is not possible to read Max File Upload configuration. "+e.getMessage());
			maxSize=26500L;
		}

		return maxSize;
	}
	/**
	 * ******************************************
	 * Get Blob data Type of the inputStream
	 * *********************************************
	 */


	/**
	 * This method creates a file
	 * @param id Id of the attachment temporary table
	 * @param pathAttachments
	 * @param thingId
	 * @param thingTypeFieldId
	 * @param hirearchyName
	 * @return
     * @throws Exception
     */
	public String createFile(
			  Long id
			, String pathAttachments
			, Long thingId
			, Long thingTypeFieldId
			, String hirearchyName ) throws Exception
	{
		String response = "";
		Attachment attachment = AttachmentService.getInstance().get( id );

		//Construct the path
		String[] arrayHirearchyName = hirearchyName.substring( 1, hirearchyName.length() ).split( ">" );
		for( String folder : arrayHirearchyName )
		{
			pathAttachments = pathAttachments + "/" + folder;
		}
		String folderAttachments = pathAttachments + "/" + thingId + "/" + thingTypeFieldId;
		String fileName = attachment.getName();
		int dotIndex = fileName.lastIndexOf('.');
		String targetFileName = fileName.substring(0, dotIndex) + "_T" + System.nanoTime() + "_" + fileName.substring(dotIndex);
		pathAttachments = folderAttachments + "/" + targetFileName;

		//Create folder
		File folder = new File( folderAttachments );
		folder.mkdirs();

		//Check if Override or not
		/*pathAttachments = this.getFileNameAttachment(
				  folderAttachments
				, attachment.getName()
				, attachment.getType()
				, attachment.getOperationOverFile());*/

		if(pathAttachments !=null)
		{
			//Save attachment
			InputStream inputStream = attachment.getFileAttachment().getBinaryStream();
			this.saveFile(inputStream, pathAttachments);
			inputStream = null;
		}
		//Delete the temp Attachment
		attachment.getFileAttachment().getBinaryStream().close();
		attachment.setFileAttachment(null);
		AttachmentService.getInstance().delete( attachment );

		//Delete attachments with date less than today
		response = pathAttachments;
		return response;
	}

	/**
	 * Get path of directory of attachment
	 * @param pathAttachments
	 * @param hirearchyName
	 * @param thingId
	 * @param thingTypeFieldId
     * @return
     */
	public String getPathDirectory(String pathAttachments, String hirearchyName, Long thingId, Long thingTypeFieldId)
	{
		String folderAttachments = null;
		if(thingId!=null)
		{
			String[] arrayHirearchyName = hirearchyName.substring( 1, hirearchyName.length() ).split( ">" );
			for( String folder : arrayHirearchyName )
			{
				pathAttachments = pathAttachments + "/" + folder;
			}
			folderAttachments = pathAttachments + "/" + thingId + "/" + thingTypeFieldId;
		}
		return folderAttachments;
	}

	/**
	 * Get File Name of the attachment
	 * @param pathFolder
	 * @param nameFile
	 * @param operation
	 * @param attachmentTempIds String with the ID's of the attachment Table. Ex: "1,2,5"
     * @return
     */
	public String getFileNameAttachment(
			String pathFolder, String nameFile,String type, String operation, String attachmentTempIds)
	{
		String result = nameFile;
		File fileData = null;
		File newFileData = null;
		if(pathFolder!=null)
		{
			String newPathAttachments = pathFolder + "/" + nameFile;
			fileData = new File( newPathAttachments );
		}

		if((fileData!=null && fileData.exists()) || getListOftemporaryAttachNames(nameFile,type, attachmentTempIds).size()>0)
		{
			if(operation.equals(Constants.FileOperation.OVERRIDE.value))
			{
				//pathAttachments = pathFolder + "/" + nameFile;
				result = nameFile;
			}else if(operation.equals(Constants.FileOperation.RENAME.value))
			{
				String file = nameFile.split("\\.")[0];
				String regexPath = file+"([^,])*"+"."+type;

				//Get Index of the consolidated files
				List<String> lstFiles = getListOfFilesWithPattern(pathFolder, regexPath);
				 	//int indexFile = getIndexFile(pathFolder, file, "([^,])","."+type , lstFiles);
				int indexFileDB = getIndexFile( file,"."+type , lstFiles, 0);

				//Get Index of the temporary files
				lstFiles = getListOftemporaryAttachNames(nameFile, type, attachmentTempIds);
				int indexFileTemp =  getIndexFile( file,"."+type , lstFiles, 0);

				//Increment the index of the file
				int indexFile = 0;
				if(indexFileDB>indexFileTemp)
				{
					indexFile = indexFileDB+1;
				}else
				{
					indexFile = indexFileTemp+1;
				}

				result = file+"("+indexFile+")."+type;
			}else if(operation.equals(Constants.FileOperation.SKIP.value))
			{
				result = null;
			}
		}

		return result;
	}

	/**
	 * This method checks if a file is in temporary attachments
	 */
	public List<String> getListOftemporaryAttachNames(String nameFile, String type, String attachmentTempIds)
	{
		List<String> result = new ArrayList<>();

		if(attachmentTempIds!=null && !attachmentTempIds.trim().equals(""))
		{
			String[] data = attachmentTempIds.split(",");
			AttachmentDAO dao = this.getAttachmentDAO();
			BooleanBuilder bb = new BooleanBuilder();
			//Construct the query
			for(int i = 0; i<data.length ; i++)
			{
				bb = bb.or(QAttachment.attachment.id.eq(Long.parseLong(data[i])));
			}
			bb = bb.and(QAttachment.attachment.name.like("%"+nameFile.replace("."+type,"")+"%."+type));
			List<Attachment> attachmentTemp = dao.selectAllBy(bb);
			if(attachmentTemp!=null && attachmentTemp.size()>0)
			{
				for(Attachment attach: attachmentTemp)
				{
					result.add(attach.getName());
				}
			}
		}

		return result;
	}

	/**
	 * Get Next index file
	 * @param directoryPath
	 * @param fileName
	 * @param regex
	 * @param fileExtension
	 * @param oldIndex If we have a previous index, it has to be added to the new index
     * @return
     */
	public int getIndexFile(String fileName, String fileExtension, List<String> lstFiles, int oldIndex)
	{
		int maxIndex = 0;
		//String regexPath = fileName+"([^,])*"+fileExtension;
		//List<String> lstFiles = getListOfFilesWithPattern(directoryPath, regexPath);
		List<Integer> index = new ArrayList<>();
		for(String data : lstFiles)
		{
			String cad = data.replace(fileName, "");
			cad = cad.replace(fileExtension, "");
			cad = cad.replace("(", "");
			cad = cad.replace(")", "");
			if(cad!=null && cad.trim().length()>0)
			{
				index.add(Integer.parseInt(cad));
			}
		}
		if(index!=null && index.size()>0)
		{
			Collections.sort(index);
			maxIndex = index.get(index.size()-1);
		}
		return maxIndex+oldIndex;
	}

	/**
	 * Get list of files with a pattern
	 * @param pathDirectory
	 * @param regex expression
     * @return
     */
	public List<String> getListOfFilesWithPattern(String pathDirectory, String regex) throws UserException {
		List<String> lstFiles = new ArrayList<>();
		if (pathDirectory != null) {
			File folder = new File(pathDirectory);
			File[] listOfFiles = folder.listFiles();
			if (listOfFiles == null) {
				throw new UserException("Provided path: \"" + pathDirectory + "\" is not a directory.");
			}
			for (int i = 0; i < listOfFiles.length; i++){
				if (listOfFiles[i].isFile()) {
					Pattern pat = Pattern.compile(regex);
					Matcher mat = pat.matcher(listOfFiles[i].getName());
					if (mat.matches()) {
						lstFiles.add(listOfFiles[i].getName());
					}
				}
			}
		}

		return lstFiles;
	}

	/**
	 * This method clone a file
	 * @param pathOriginalFile
	 * @param pathAttachments
	 * @param thingId
	 * @param thingTypeFieldId
	 * @param hirearchyName
	 * @return
     * @throws Exception
     */
	public String cloneFile(
			  String pathOriginalFile
			, String pathAttachments
			, Long thingId
			, Long thingTypeFieldId
			, String hirearchyName ) throws Exception
	{
		String response = "";
		//Construct the path
		String[] arrayHirearchyName = hirearchyName.substring( 1, hirearchyName.length() ).split( ">" );
		for( String folder : arrayHirearchyName )
		{
			pathAttachments = pathAttachments + "/" + folder;
		}
		String folderAttachments = pathAttachments + "/" + thingId + "/" + thingTypeFieldId;
		//Create folder
		File folder = new File( folderAttachments );
		folder.mkdirs();

		String[] data = pathOriginalFile.split( "/" );
		pathAttachments = folderAttachments + "/" + data[data.length-1];
		InputStream originFile = new FileInputStream(pathOriginalFile);
		this.saveFile( originFile, pathAttachments );
		originFile = null;

		//Delete the temp Attachment
		response = pathAttachments;
		return response;
	}


	/**
	 * Save uploaded file to a defined location on the server
	 * @param inputStream
	 * @param fileName
	 * @throws Exception
     */
	public void saveFile( InputStream inputStream, String fileName) throws UserException, Exception
	{
		File fileData = new File( fileName );
		FileOutputStream outpuStream = null;
		try
		{
			int read = 0;
			byte[] bytes = new byte[1024];
			outpuStream = new FileOutputStream( new File( fileName ) );
			while( (read = inputStream.read( bytes )) != -1 )
			{
				outpuStream.write( bytes, 0, read );
			}
		}
		catch( Exception e )
		{
			throw new UserException( "Error writting the file '"+fileName+"'" , e);
		}finally{
			if (outpuStream != null) {
				outpuStream.flush();
				outpuStream.close();
				outpuStream = null;
			}
			inputStream.close();
			inputStream = null;
			fileData = null;
		}
	}

	/**
	 * ***********************************************************************
	 * remove file from a defined location on the server
	 * ***********************************************************************
	 */
	public void removeFile(String fileName) throws Exception{
		File file = new File(fileName);
		file.setWritable(true);
		boolean isFileDeleted = false;

		if (file.isDirectory()) {
			Path fp = file.toPath();
			FileUtils.cleanDirectory(file);
			Files.delete(fp);
			isFileDeleted = true;
		}
		else {
			isFileDeleted = file.delete();
		}

		if (! isFileDeleted) {
			logger.error("The file:'" + fileName + "' could not be deleted.");
		}
	}


	/**
	 * ***********************************************************************
	 * Delete all the files in Hibernate
	 * ***********************************************************************
	 */
	public void deleteOldAttachments()
	{
		Date today = new Date();
		Calendar calendarDate = Calendar.getInstance();
		calendarDate.setFirstDayOfWeek(Calendar.MONDAY);
		calendarDate.setTime(today);
		calendarDate.add( Calendar.HOUR_OF_DAY, -1 );
		BooleanBuilder be = new BooleanBuilder();

		Pagination pagination = new Pagination( 1, 2000 );
		be = be.and( QAttachment.attachment.dateUploaded.before( calendarDate.getTime() ) );
		for( Attachment attachment : AttachmentService.getInstance().listPaginated( be, pagination, null ) )
		{
			HibernateDAOUtils.delete( getAttachmentDAO().getSession(), attachment );
		}
	}

	/***********************************************************************
	 * This method rebuilds the structure of the Json Attachment based on the main path
	 * where the files are
	 * @param path
	 * @return HashMap with results
	 ***********************************************************************/
	public Map<String, Object> rebuildJsonStructure(String path)
	{
		Map<String, Object> response = new HashMap<>(  );
		File folder =null;
		File[] listOfFiles = null;
		try{
			folder = new File(path);
			listOfFiles = folder.listFiles();
            if (listOfFiles == null) {
                throw new UserException("Provided path: \"" + path + "\" is not a directory.");
            }
			for (File file : listOfFiles) {
				if (file.isDirectory()) {
					String newPath = path+"/"+file.getName();
					//System.out.println("---->"+newPath);
					rebuildJsonStructure( path + "/" + file.getName() );
				}
                // rsejas, Todo: Delete this code
//                else
//				{
					//Pattern COMMA_PATTERN = Pattern.compile( "*\\*\\*." );
					//String[] records  = COMMA_PATTERN.split(lines[i]);
					//System.out.println(file.getPath() );
					//String[] thingId = file.getPath().split("\\", 5);

//				}
			}
		}catch(Exception e)
		{
			if(folder!=null)
			{
				folder = null;
			}
			if(listOfFiles!=null)
			{
				listOfFiles = null;
			}
		}
		return response;
	}

	/***********************************************
	 * Get list of files of the directory
	 ***********************************************/
	public List<Map<String, Object>> getFilesOfDirectory(String path, List<Map<String, Object>> files)
	{
		File folder = new File(path);
		File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null) {
            throw new UserException("Provided path: \"" + path + "\" is not a directory.");
        }
		for (File file : listOfFiles) {
			if (file.isDirectory()) {
				String newPath = path+"/"+file.getName();
				getFilesOfDirectory(path+"/"+file.getName(), files);
			}else
			{
				Map<String, Object> data = new HashMap<>(  );
				data.put( "name", file.getName() );
				data.put( "fileSystemPath", file.getAbsolutePath() );
				data.put( "type", file.getName().split( "\\." )[1]);
				data.put( "size", file.getTotalSpace());
				files.add(data);
			}
		}
		return files;
	}

	/***********************************************
	 * Get list of files of the directory
	 ***********************************************/
	public Map<String, Object> processBuildAttachment(Stack<Long> recursivelyStack, Map<String, Object> file, User currentUser)
	{
		Map<String, Object> response = new HashMap<String, Object>();
		try{
			String fileTemp = file.get( "fileSystemPath" ).toString().replace( "\\", "\\\\" );
			String[] data = fileTemp.split( "\\\\\\\\" );
			Long thingId = Long.parseLong( data[data.length - 3] );
			Long thingTypeFieldId = Long.parseLong( data[data.length-2] );

			//Creating the data
			Thing thing = ThingService.getInstance().get( thingId );
			ThingTypeField thingTypeField = ThingTypeFieldService.getInstance().get( thingTypeFieldId );
			String configAttachment = thingTypeField.getDefaultValue();
			JSONParser parser = new JSONParser();
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date dateRegister = new Date();
			String dateString = df.format(dateRegister);
			String attachmentValue = null;

			JSONObject jsonAttachment = new JSONObject(  );
			jsonAttachment.put( "uploadedBy", "Root User" );
			jsonAttachment.put( "fileSystemPath", file.get( "fileSystemPath" ).toString() );
			jsonAttachment.put( "name", file.get( "name" ).toString() );
			jsonAttachment.put( "type",file.get( "type" ).toString() );
			jsonAttachment.put( "date", dateString );
			jsonAttachment.put( "comment", "JSON generated by script" );
			jsonAttachment.put( "size", file.get( "size" ).toString() );

			if(thing.getValueOfThingField( thingTypeField.getName())!=null)
			{
				JSONObject jsonUpdate = (JSONObject) parser.parse(
						((ThingField) thing.getValueOfThingField( thingTypeField.getName() )).getValue().toString() );
				JSONArray attachmentLst = (JSONArray) jsonUpdate.get( "attachments" );
				if(attachmentLst!=null && attachmentLst.size()>0)
				{
					JSONObject dataAux = null;
					for(Object obj : attachmentLst)
					{
						JSONObject objJson = (JSONObject) obj;
						if(objJson.get( "name" ).toString().equals( file.get( "name" ).toString() ))
						{
							dataAux = objJson;
							break;
						}
					}
					if(dataAux!=null)
					{
						int index = attachmentLst.indexOf( dataAux );
						attachmentLst.set( index, jsonAttachment );
					}else
					{
						attachmentLst.add( jsonAttachment );
					}
				}
				jsonUpdate.put( "attachments", attachmentLst );
				attachmentValue = jsonUpdate.toJSONString();

			}else
			{
				JSONObject jsonCreate = (JSONObject) parser.parse( configAttachment);
				JSONArray attachmentObj = new JSONArray(  );
				attachmentObj.add( jsonAttachment );
				jsonCreate.put( "count", attachmentObj.size() );
				jsonCreate.put( "lastUpdate", dateString );
				jsonCreate.put( "attachments", attachmentObj );
				attachmentValue = jsonCreate.toJSONString();
			}
			if(attachmentValue!=null && !attachmentValue.trim().toString().equals( "" ))
			{
				Map<String, Object> valueMap = new HashMap<String , Object>();
				valueMap.put( "value",  attachmentValue );
				Map<String, Object> attachmentUdfMap = new HashMap<String , Object>();
				attachmentUdfMap.put( thingTypeField.getName(), valueMap );

				//Update Thing with the new value of attachment
				response = ThingService.getInstance().update(
                        recursivelyStack
						, thing.getId()
						, thing.getThingType().getThingTypeCode()
						, thing.getGroup().getHierarchyName( false )
						, thing.getName()
						, thing.getSerial()
						, null
						, attachmentUdfMap
						, null, null
						, true, true, dateRegister, true , currentUser, true);
			}
		}catch(Exception e)
		{
			logger.error( e.getMessage() );
		}

		return response;
	}
}
