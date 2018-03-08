package com.tierconnect.riot.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thetransactioncompany.cors.HTTPMethod;
import com.tierconnect.riot.iot.servlet.RiotRestEasyApplication;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.resteasy.plugins.server.tjws.TJWSEmbeddedJaxrsServer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import static org.junit.Assert.*;

public class TestUtils {
    public static final ThreadLocal<String> apiKey = new ThreadLocal<>();

	private static TJWSEmbeddedJaxrsServer server = null;
	public static TJWSEmbeddedJaxrsServer startTestServer(int port){		
		if(server == null){
			server = new TJWSEmbeddedJaxrsServer();
	        server.setPort(port);                
	        server.getDeployment().setApplication(new RiotRestEasyApplication());
            server.setThreadPoolSize(1);
	        server.start();//This start need to be done here.
		}
        return server;
	}	
	
	public static void stopServer(){
		server.stop();
		server = null;
	}
	
	public static Map<String,Object> expectResponse(String uri,int expectedStatus,HTTPMethod method){
		return expectResponse(uri, expectedStatus, method,null);
	}

    static DefaultHttpClient client = new DefaultHttpClient();

    public static Map<String,Object> expectResponse(String uri,int expectedStatus,HTTPMethod method,Map<String,Object> body){
		try {
			HttpUriRequest request = null;
			String jsonBody = null; 

			if(body != null){
				jsonBody = new ObjectMapper().writeValueAsString(body);
			}else{
				jsonBody = "{}";
			}

			if(method == HTTPMethod.GET){
				request = new HttpGet(uri);
			}else if(method == HTTPMethod.POST){
				HttpPost httpPost = new HttpPost(uri);
				httpPost.setEntity(new StringEntity(jsonBody));
				request = httpPost;
				request.addHeader("content-type", "application/json");
			}else if(method == HTTPMethod.PUT){
				HttpPut httpPut = new HttpPut(uri);
				httpPut.setEntity(new StringEntity(jsonBody));
				request = httpPut;
				request.addHeader("content-type", "application/json");
			}else if(method == HTTPMethod.PATCH){
				HttpPatch httpPatch = new HttpPatch(uri);
				httpPatch.setEntity(new StringEntity(jsonBody));
				request = httpPatch;
				request.addHeader("content-type", "application/json");
			}else if(method == HTTPMethod.DELETE){
				request = new HttpDelete(uri);
			};

            String key = apiKey.get();
            if (StringUtils.isEmpty(key)) {
            	System.out.println("asd1");
                key = "root";
            }
			if (request != null) {
				request.addHeader("api_key", key);
			}
			HttpResponse response = client.execute(request);

			HttpEntity entity = response.getEntity();
			StringBuffer jsonResponse = new StringBuffer();
			if(entity != null && entity.getContent() != null){
				BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					jsonResponse.append(inputLine);
				}
				in.close();
			}
			
			if(response.getStatusLine().getStatusCode() != expectedStatus){				
				fail("expected status:"+ expectedStatus + " actual status:"+ response.getStatusLine().getStatusCode() + " input:" +  jsonBody + " body:"+jsonResponse.toString());
			}

			Map<String,Object> result = null;
			if(StringUtils.isNotBlank(jsonResponse.toString())){
				result = new ObjectMapper().readValue(jsonResponse.toString(), HashMap.class);
			}

			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}
	
    public static Map<String,Object> expectResponse(String uri,int expectedStatus,HTTPMethod method,Map<String,Object> body,String api){
		try {
			HttpUriRequest request = null;
			String jsonBody = null; 
			
			if(body != null){
				jsonBody = new ObjectMapper().writeValueAsString(body);
			}else{
				jsonBody = "{}";
			}

			if(method == HTTPMethod.GET){
				request = new HttpGet(uri);
			}else if(method == HTTPMethod.POST){
				HttpPost httpPost = new HttpPost(uri);
				httpPost.setEntity(new StringEntity(jsonBody));
				request = httpPost;
				request.addHeader("content-type", "application/json");
			}else if(method == HTTPMethod.PUT){
				HttpPut httpPut = new HttpPut(uri);
				httpPut.setEntity(new StringEntity(jsonBody));
				request = httpPut;
				request.addHeader("content-type", "application/json");
			}else if(method == HTTPMethod.PATCH){
				HttpPatch httpPatch = new HttpPatch(uri);
				httpPatch.setEntity(new StringEntity(jsonBody));
				request = httpPatch;
				request.addHeader("content-type", "application/json");
			}else if(method == HTTPMethod.DELETE){
				request = new HttpDelete(uri);
			};

            String key = apiKey.get();
            
           // if (StringUtils.isEmpty(key)) {
            	
            if ((api != null) && !api.isEmpty()) {
				key = api;
			}
            //}
                if(key==null){
                	System.out.println("asdssdasdasadsdsda");
                	key=api;
                	System.out.println("key cambiada"+key);
                }
			if (request != null) {
				request.addHeader("api_key", key);
				System.out.println("api " + api + " " + request.getLastHeader("api_key"));
			}
			HttpResponse response = client.execute(request);

			HttpEntity entity = response.getEntity();
			StringBuffer jsonResponse = new StringBuffer();
			if(entity != null && entity.getContent() != null){
				BufferedReader in = new BufferedReader(new InputStreamReader(entity.getContent()));
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					jsonResponse.append(inputLine);
					System.out.println("ini"+jsonResponse+"fin");
				}
				in.close();
			}
			if((api!=null) && api.equals("oscar")) {
                System.out.println("Este es el cod oscar" + response.getStatusLine().getStatusCode());
            }
			if(response.getStatusLine().getStatusCode() != expectedStatus){	
				System.out.println("Este es el cod"+response.getStatusLine().getStatusCode());
				System.out.println("jsonaaa"+ apiKey.get());
				System.out.println((request.getURI()!=null)?request.getURI():"request is null");
				System.out.println("api malo"+api+" "+request.getLastHeader("api_key"));							
				fail("expected status:"+ expectedStatus + " actual status:"+ response.getStatusLine().getStatusCode() + " input:" +  jsonBody + " body:"+jsonResponse.toString());
				
			}
			

			Map<String,Object> result = null;
			if(StringUtils.isNotBlank(jsonResponse.toString())){
				result = new ObjectMapper().readValue(jsonResponse.toString(), HashMap.class);
			}
			
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}
	public static void validateOutputMap(Map<String, Object> inputMap, Map<String, Object> outputMap) {
		for (Map.Entry<String, Object> entry: inputMap.entrySet()) {
			if (!entry.getKey().contains(".")) {
				assertEquals(entry.getValue(), outputMap.get(entry.getKey()));
			}
		}
	}

    public static void validateOutputMap(Map<String, Object> inputMap, Map<String, Object> outputMap, String... ignoreFields) {
        for (Map.Entry<String, Object> entry: inputMap.entrySet()) {
            if (!entry.getKey().contains(".")) {
                if (!Arrays.asList(ignoreFields).contains(entry.getKey())) {
                    assertEquals(entry.getValue(), outputMap.get(entry.getKey()));
                }
            }
        }
    }

	public static void validateErrorMap(Map<String, Object> response) {
		assertTrue(response.get("error") != null);
	}
	public static int getNumberOfAttribsFromASingleJsonObject(Map<String, Object> response)
	{int tot=0;
	Map.Entry<String, Object> sol=null;
    		
	for (Map.Entry<String, Object> entry: response.entrySet()) {
		System.out.println("key o id"+entry.getKey());
		
		if(entry.getKey().toString().equals("results"))
			{
				System.out.println("test paso100"+entry.getValue().toString());
				String d=entry.getValue().toString();
				d=d.replace('{', ' ');
				d=d.replace('}', ' ');
				d=d.replace('[', ' ');
				d=d.replace(']', ' ');
				//d=d.replace(" , ",":");
				 sol=entry;
				StringTokenizer b=new StringTokenizer(d,",");
				System.out.println(b.countTokens()+" "+d);
				tot=b.countTokens();
				
			}
		
	}	
	
	return tot;
	}
	public static String getEntityNameFromAsingleJsonObject(Map<String, Object> response,int i)
	{int tot=0;
	Map.Entry<String, Object> sol=null;		
	for (Map.Entry<String, Object> entry: response.entrySet()) {
		System.out.println("key o id"+entry.getKey());
		
		if(entry.getKey().toString().equals("results"))
			{
				System.out.println("test paso100"+entry.getValue().toString());
				String d=entry.getValue().toString();
				d=d.replace('{', ' ');
				d=d.replace('}', ' ');
				d=d.replace('[', ' ');
				d=d.replace(']', ' ');
				d=d.replace(" , ",":");
				//d=d.replace(',', ' ');
				 sol=entry;
				StringTokenizer b=new StringTokenizer(d,":");
				int k=0;
				while(b.hasMoreTokens())
				{k++;
				if(k==i){
					String aux=b.nextToken();
					
					int lim=aux.indexOf('=');
					System.out.println(aux.substring(0,lim));
					return 	aux.substring(0,lim).trim();
				}
					
				}
				System.out.println(b.countTokens()+" "+d);
				tot=b.countTokens();
				
			}
		
	}	
	
	return "";
	}
	public static boolean isEntityNameAvailableInJsonObject(Map<String, Object> response,String i)
	{	
	for (Map.Entry<String, Object> entry: response.entrySet()) {
		System.out.println("key o id"+entry.getKey());
		
		if(entry.getKey().toString().equals("results"))
			{
				if(entry.getValue().toString().contains(i))
					return true;
				return false;
				
			}
		
	}	
	
	return false;
	}
}
