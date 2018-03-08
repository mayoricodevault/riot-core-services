package com.tierconnect.riot.sdk.servlet.exception;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import com.tierconnect.riot.sdk.dao.NotFoundException;
import com.tierconnect.riot.sdk.dao.ServerException;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;

@Provider
public class GeneralExceptionMapper implements ExceptionMapper<Throwable> {
    static Logger logger = Logger.getLogger(GeneralExceptionMapper.class);
	
	@Override
	public Response toResponse(final Throwable ex) {
        Map<String,String> result = new HashMap<>();
        if (ex instanceof ForbiddenException) {
            result.put("error", ex.getMessage());
            logger.warn(ex.getMessage());
            return Response.status(Response.Status.FORBIDDEN).type(MediaType.APPLICATION_JSON).entity(result).build();
        } else if (ex instanceof NotFoundException) {
            result.put("error", ex.getMessage());
            logger.warn(ex.getMessage());
            return Response.status(Status.NOT_FOUND).type(MediaType.APPLICATION_JSON).entity(result).build();
        } else if (ex instanceof UserException) {
            result.put("error", ex.getMessage());
            logger.warn(ex.getMessage());
            return Response.status(Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(result).build();
        } else if (ex instanceof ServerException) {
            logger.error(ex.getMessage());
            result.put("error", ex.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(result).build();
        } else {
            logger.error(ex.getMessage(), ex);
            result.put("error", ex.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.APPLICATION_JSON).entity(result).build();
        }
	}

}
