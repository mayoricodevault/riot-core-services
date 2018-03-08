package com.tierconnect.riot.sdk.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

public class RestUtils
{
	private static ResponseBuilder createBaseResponse()
	{
		return Response.status( 200 ).header( "content-type", "application/json" );
	}

	public static Response sendBadResponse( String cause )
	{
		Map<String, String> messageMap = new HashMap<String, String>();
		messageMap.put( "message", cause );

		return createBaseResponse().status( 400 ).entity(messageMap).build();
	}

	public static Response sendResponseWithCode( String cause, int code)
	{
		Map<String, String> messageMap = new HashMap<>();
		messageMap.put( "message", cause );

		return createBaseResponse().status( code ).entity( messageMap ).build();
	}

	public static Response sendOkResponse( Object object )
	{
		return sendOkResponse( object, true );
	}

	public static Response sendOkResponse( Object object, boolean asMessage )
	{
		if( object instanceof String && asMessage )
		{
			Map<String, String> map = new HashMap<String, String>();
			map.put( "message", (String) object );
			return createBaseResponse().status( 200 ).entity( map ).build();
		}
		return createBaseResponse().status( 200 ).entity( object ).build();
	}

	public static Response sendCreatedResponse( Object object )
	{
		if( object instanceof String )
		{
			Map<String, String> map = new HashMap<String, String>();
			map.put( "message", (String) object );
			return createBaseResponse().status( 201 ).entity( map ).build();
		}

		return createBaseResponse().status( 201 ).entity( object ).build();
	}

	public static Response sendDeleteResponse()
	{
		return Response.noContent().status( 204 ).header( "content-type", "application/json" ).build();
	}

	public static Response sendEmptyResponse()
	{
		return Response.noContent().status( 204 ).header( "content-type", "application/json" ).build();
	}

	/**
	 * Sends a JSON response with a given status code
	 * @param json	JSON response in string format
	 * @param code	code of the response
	 * @return Response
	 */
	public static Response sendJSONResponseWithCode( String json, int code )
	{
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			Map<String, String> messageMap = objectMapper.readValue(json, HashMap.class);
			return createBaseResponse().status( code ).entity(messageMap).build();
		} catch (IOException e) {
			return sendBadResponse(e.getMessage());
		}
	}
}
