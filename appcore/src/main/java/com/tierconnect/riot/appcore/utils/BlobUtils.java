package com.tierconnect.riot.appcore.utils;

import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialException;
import javax.ws.rs.core.MultivaluedMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sergioaranda on 3/7/16.
 */
public class BlobUtils {


    /**
     * ******************************************
     * Get Blob data Type of the inputStream
     * *********************************************
     */
    public static Map<String, Object> getBlobMap( MultipartFormDataInput input, String key )
    {
        Map<String, Object> response = new HashMap<>();
        Blob blob = null;
        byte[] content = null;
        String fileName = "";
        int size = 0;
        //Set the Blob
        Map<String, List<InputPart>> formParts = input.getFormDataMap();
        List<InputPart> inPart = formParts.get( key );
        InputStream istream = null;
        for( InputPart inputPart : inPart )
        {
            try
            {
                // Retrieve headers, read the Content-Disposition header to obtain the original name of the file
                MultivaluedMap<String, String> headers = inputPart.getHeaders();
                fileName = parseFileName( headers );
                // Handle the body of that part with an InputStream
                istream = inputPart.getBody( InputStream.class, null );
                size = istream.available();

                content = IOUtils.toByteArray(istream);
                blob = new SerialBlob( content );
            }
            catch( SerialException e )
            {
                throw new UserException( "Error saving the file: '" + fileName + "'",e  );
            }
            catch( SQLException e )
            {
                throw new UserException( "Error saving the file: '" + fileName + "' in DB.", e );
            }
            catch( Exception e )
            {
                throw new UserException( "Error saving the file: '" + fileName + "'. " + e.getMessage(), e );
            }finally
            {
                if(istream!=null)
                {
                    try
                    {
                        istream.close();
                        istream = null;
                    }catch(Exception e)
                    {
                        istream = null;
                        throw new UserException( e.getMessage() , e);
                    }
                }
            }
        }
        response.put( "Blob", blob );
        response.put( "FileName", fileName );
        response.put( "Size", size );
        response.put( "byteArray", content );

        return response;
    }

    public static List<Map<String, Object>> getBlobList( MultipartFormDataInput input, String key )
    {
        List<Map<String, Object>> lstResponse = new ArrayList<>();
        Map<String, Object> response = new HashMap<>();
        Blob blob = null;
        byte[] content = null;
        String fileName = "";
        int size = 0;
        //Set the Blob
        Map<String, List<InputPart>> formParts = input.getFormDataMap();
        //Select all data
        for (Map.Entry<String, List<InputPart>> entry : formParts.entrySet())
        {
            List<InputPart> inPart = formParts.get( entry.getKey() );
            InputStream istream = null;
            for( InputPart inputPart : inPart )
            {
                try
                {
                    // Retrieve headers, read the Content-Disposition header to obtain the original name of the file
                    MultivaluedMap<String, String> headers = inputPart.getHeaders();
                    String[] contentDispositionHeader = headers.getFirst("Content-Disposition").split(";");
                    fileName = BlobUtils.parseFileName( headers );
                    // Handle the body of that part with an InputStream
                    istream = inputPart.getBody( InputStream.class, null );
                    size = istream.available();

                    content = IOUtils.toByteArray( istream );
                    blob = new SerialBlob( content );

                    //Save Data
                    response = new HashMap<>();
                    response.put( "Blob", blob );
                    response.put( "FileName", fileName );
                    response.put( "Size", size );
                    response.put( "byteArray", content );
                    lstResponse.add(response);
                }
                catch( SerialException e )
                {
                    throw new UserException( "Error saving the file: '" + fileName + "'", e );
                }
                catch( SQLException e )
                {
                    throw new UserException( "Error saving the file: '" + fileName + "' in DB.", e );
                }
                catch( Exception e )
                {
                    throw new UserException( "Error saving the file: '" + fileName + "'. " + e.getMessage(), e );
                }finally
                {
                    if(istream!=null)
                    {
                        try
                        {
                            istream.close();
                            istream = null;
                        }catch(Exception e)
                        {
                            istream = null;
                            throw new UserException( e.getMessage(), e );
                        }
                    }
                }
            }
        }



        return lstResponse;
    }

    public static Map<String, Object> getBlobMapByteArray( MultipartFormDataInput input, String key )
    {
        Map<String, Object> response = new HashMap<>();
        //Blob blob = null;
        byte[] content = null;
        String fileName = "";
        int size = 0;
        //Set the Blob
        Map<String, List<InputPart>> formParts = input.getFormDataMap();
        List<InputPart> inPart = formParts.get( key );
        InputStream istream = null;
        for( InputPart inputPart : inPart )
        {
            try
            {
                // Retrieve headers, read the Content-Disposition header to obtain the original name of the file
                MultivaluedMap<String, String> headers = inputPart.getHeaders();
                fileName = parseFileName( headers );
                // Handle the body of that part with an InputStream
                istream = inputPart.getBody( InputStream.class, null );
                size = istream.available();

                content = IOUtils.toByteArray(istream);

            }catch( Exception e )
            {
                throw new UserException( "Error saving the file: '" + fileName + "'. " + e.getMessage() , e);
            }finally
            {
                if(istream!=null)
                {
                    try
                    {
                        istream.close();
                        istream = null;
                    }catch(Exception e)
                    {
                        istream = null;
                        throw new UserException( e.getMessage(), e );
                    }
                }
            }
        }
        response.put( "Blob", content );
        response.put( "FileName", fileName );
        response.put( "Size", size );
        response.put( "byteArray", content );

        return response;
    }

    /**
     * ***********************************************************************
     * Parse Content-Disposition header to get the original file name
     * ***********************************************************************
     */
    public static String parseFileName( MultivaluedMap<String, String> headers )
    {
        String[] contentDispositionHeader = headers.getFirst( "Content-Disposition" ).split( ";" );

        for( String name : contentDispositionHeader )
        {
            if( (name.trim().startsWith( "filename" )) )
            {
                String[] tmp = name.split( "=" );
                String fileName = tmp[1].trim().replaceAll( "\"", "" );
                return fileName;
            }
        }
        return "randomName";
    }

    public static File getFileImage(String path, String fileName)
    {
        File f = null;

        //get image associated with parameter id
        f = new File(path + fileName);

        //check if the image exists. otherwise return unknown image
        if(!f.exists()) {
            f = new File(path + "unknown.png");
        }

        return f;
    }
}
