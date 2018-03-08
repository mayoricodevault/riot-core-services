package com.tierconnect.riot.appcore.controllers;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import javax.annotation.security.PermitAll;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.input.ReversedLinesFileReader;

import com.tierconnect.riot.sdk.dao.NotFoundException;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

@Path("/logs")
@Api("/logs")
public class LogController {
	private static int DEFAULT_LINES = 100;
	private static int MAX_LINES = 500;
	
	@GET
    @Path("/tomcat") 
	@Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
	@PermitAll
    @ApiOperation(value="Tomcat logs",consumes=MediaType.TEXT_PLAIN)
	public String getTomcatLogs(@Context ServletContext servletContext, @QueryParam("lines")Integer lines) throws IOException{
		String logsPath = servletContext.getRealPath(File.separator)+".."+File.separator+".."+File.separator+"logs"+File.separator;
		logsPath += String.format("localhost.%d-%02d-%02d.log",Calendar.getInstance().get(Calendar.YEAR),Calendar.getInstance().get(Calendar.MONTH)+1,Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
		
		return processLog(lines, logsPath);
	}
	
	@GET
    @Path("/riot_events") 
	@Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
	@PermitAll
    @ApiOperation(value="Riot Events logs",consumes=MediaType.TEXT_PLAIN)
	public String getRiotEventsLogs(@Context ServletContext servletContext, @QueryParam("lines")Integer lines) throws IOException{
		String logsPath = servletContext.getRealPath(File.separator)+".."+File.separator+".."+File.separator+"logs"+File.separator;
		logsPath += String.format("localhost.%d-%02d-%02d.log",Calendar.getInstance().get(Calendar.YEAR),Calendar.getInstance().get(Calendar.MONTH)+1,Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
		
		return processLog(lines, logsPath);
	}
	
	private String processLog(Integer lines, String logsPath) throws IOException{
		File file = new File(logsPath);
		
		if(file.exists() == false){
			throw new NotFoundException(logsPath + " LOG not found");
		}
		if(lines == null){
			lines = DEFAULT_LINES;
		}
		lines = Math.min(lines, MAX_LINES);
		
		return getLastLines(file, lines).toString();
	}
	
	public static StringBuffer getLastLines(File logFile,int lines) throws IOException{
		ReversedLinesFileReader fileReader = new ReversedLinesFileReader(logFile);

		StringBuffer buffer = new StringBuffer();
		String line;

		for(int i=1;i<=lines;i++){
			line = fileReader.readLine();
			if(line == null){
				break;
			}
			buffer.append(line+"\n");
		}

		fileReader.close();
		return buffer;
	}
}
