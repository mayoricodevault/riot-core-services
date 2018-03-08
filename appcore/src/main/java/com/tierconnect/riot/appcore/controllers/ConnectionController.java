package com.tierconnect.riot.appcore.controllers;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.Generated;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.*;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.subject.Subject;
import org.jose4j.json.internal.json_simple.JSONValue;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.*;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.subject.Subject;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.JSONValue;
import org.jose4j.json.internal.json_simple.parser.JSONParser;

import javax.annotation.Generated;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Path("/connection")
@Api("/connection")
@Generated("com.tierconnect.riot.appgen.service.GenController")
public class ConnectionController extends ConnectionControllerBase 
{

    static Logger logger = Logger.getLogger(ConnectionController.class);
    private static final String BROKER_CLIENT_HELPER = "com.tierconnect.riot.iot.services.BrokerClientHelper";

    @Override
    public void addToPublicMap(Connection connection, Map<String, Object> publicMap, String extra) {
        ObjectMapper objectMapper = new ObjectMapper();
        String properties = connection.getProperties();
        if (StringUtils.isNotEmpty(properties)) {
            try {
                TreeMap<String, Object> propertiesMap = (TreeMap<String, Object>) objectMapper.readValue(properties, TreeMap.class);
                publicMap.put("properties", propertiesMap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            publicMap.put("properties", new HashMap<>());
        }
        if (extra != null && extra.contains("connectionType")) {
            publicMap.put("connectionType", connection.getConnectionType().publicMap());
            try {
                Map connectionTypeMap = (Map) publicMap.get("connectionType");
                connectionTypeMap.put("propertiesDefinitions", (List) objectMapper.readValue((String) connectionTypeMap.get("propertiesDefinitions"), List.class));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @PUT
    @Override
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position=3, value="Insert a Connection")
    public Response insertConnection( Map<String, Object> map,@QueryParam("createRecent") @DefaultValue("false") Boolean createRecent )
    {
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityInsert(entityVisibility, VisibilityUtils.getObjectGroup(map));
        Connection connection = new Connection();
        // 7. handle insert and update
        if (hasPasswordConnectionType(map, null)){
            if(map.containsKey("newPassword")){
                if(((Map)map.get("newPassword")).get("id")!= null){
                    Connection connectionOld = ConnectionService.getInstance()
                            .get(Long.parseLong(((Map)map.get("newPassword")).get("id").toString()));
                    ((Map)map.get("properties")).put("password", connectionOld.getPassword(true));
                } else if(((Map)map.get("newPassword")).get("password") == null &&
                        ((Map)map.get("newPassword")).get("confirmPassword") == null) {
                    logger.warn("Saving userPassword empty value");
                    ((Map)map.get("properties")).put("password", null);
                } else if(((Map)map.get("newPassword")).get("password") != null &&
                        ((Map)map.get("newPassword")).get("confirmPassword") != null &&
                        ((Map)map.get("newPassword")).get("password").equals(((Map)map.get("newPassword")).get("confirmPassword"))){
                    ((Map)map.get("properties")).put("password", ((Map)map.get("newPassword")).get("password"));
                }else{
                    throw new UserException("Password and Confirm Password do not match");
                }
            }else{
                throw new UserException("Password required");
            }
        }
        BeanUtils.setProperties(map, connection);
        if (connection.getCode() == null){
            return RestUtils.sendBadResponse("Invalid Connection Code.");
        }
        // 6. handle validation in an Extensible manner
        validateInsert( connection );
        connection.mapProperties();
        String requiredMessage = connection.requiredFieldsMessage();
        if (requiredMessage == null) {
            ConnectionService.getInstance().insert(connection);
            connection.mapProperties();
            sendMQTTSignal(GroupService.getInstance().getMqttGroups(connection.getGroup()));
            refreshCacheConnection(connection, false);
            if (createRecent){
                RecentService.getInstance().insertRecent(connection.getId(), connection.getName(),"connection",connection.getGroup());
            }
            // 1[b,c]. Restrict access based on OBJECT and PROPERTY level read permissions
            Map<String,Object> publicMap = connection.publicMap();
            return RestUtils.sendCreatedResponse(publicMap);
        } else {
            return RestUtils.sendBadResponse(requiredMessage);
        }
    }

    public void validateInsert( Connection connection )
    {

    }

    @GET
    @Override
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
    @ApiOperation(position=2, value="Select a Connection")
    public Response selectConnections(@PathParam("id") Long id, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project, @QueryParam("createRecent") @DefaultValue("false") Boolean createRecent )
    {
        Connection connection = ConnectionService.getInstance().get(id);
        if( connection == null )
        {
            return RestUtils.sendBadResponse( String.format( "ConnectionId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, connection);
        validateSelect(connection);
        // 5a. Implement extra
        Map<String,Object> publicMap = QueryUtils.mapWithExtraFields(connection, extra, getExtraPropertyNames());
        addToPublicMap(connection, publicMap, extra);
        if (createRecent){
            RecentService.getInstance().insertRecent(connection.getId(), connection.getName(), "connection",connection.getGroup());
        }
        // 5b. Implement only
        QueryUtils.filterOnly(publicMap, only, extra);
        QueryUtils.filterProjectionNested(publicMap, project, extend);
        return RestUtils.sendOkResponse(publicMap);
    }

    public void validateSelect( Connection connection )
    {

    }

    @PATCH
    @Override
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
    @ApiOperation(position=4, value="Update a Connection")
    public Response updateConnection( @PathParam("id") Long id, Map<String, Object> map )
    {
        Connection connection = ConnectionService.getInstance().get( id );
        if( connection == null )
        {
            return RestUtils.sendBadResponse( String.format( "ConnectionId[%d] not found", id) );
        }

        String oldPassword = connection.getPassword(true);

        if(hasPasswordConnectionType(map, connection)){
            if(map.containsKey("newPassword")){
                Subject subject = SecurityUtils.getSubject();
                if(((Map)map.get("newPassword")).get("userPassword") != null &&
                        UserService.getInstance().matchesPassword((User) subject.getPrincipal(), decode(((Map)map.get("newPassword")).get("userPassword").toString()))){
                    if(((Map)map.get("newPassword")).get("password") != null &&
                            ((Map)map.get("newPassword")).get("confirmPassword") != null &&
                            ((Map)map.get("newPassword")).get("password").equals(((Map)map.get("newPassword")).get("confirmPassword"))){
                        ((Map)map.get("properties")).put("password", ((Map)map.get("newPassword")).get("password"));
                    }else{
                        throw new UserException("Password and Confirm Password do not match");
                    }
                }else{
                    throw new UserException("Invalid User Password");
                }
            }else{
                ((Map)map.get("properties")).put("password", oldPassword);
            }
        }

        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityUpdate(entityVisibility, connection, VisibilityUtils.getObjectGroup(map));
        // 7. handle insert and update
        BeanUtils.setProperties(map, connection);
        // 6. handle validation in an Extensible manner
        connection = validatePasswordUpdate(connection);
        connection.mapProperties();
        if (connection.getCode() == null){
            return RestUtils.sendBadResponse("Invalid Connection Code.");
        }
        String requiredMessage = connection.requiredFieldsMessage();
        if (requiredMessage == null) {
            ConnectionService.getInstance().update(connection);
            RecentService.getInstance().updateName(connection.getId(), connection.getName(),"connection");
            connection.mapProperties();
            sendMQTTSignal(GroupService.getInstance().getMqttGroups(connection.getGroup()));
            refreshCacheConnection(connection, false);
            Map<String, Object> publicMap = connection.publicMap();
            return RestUtils.sendOkResponse(publicMap);
        } else {
            return RestUtils.sendBadResponse(requiredMessage);
        }
    }

    public void validateUpdate(Connection connection)
    {

    }

    private Connection validatePasswordUpdate ( Connection connection )
    {
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> mapResponse;
        try {
            mapResponse = mapper.readValue( connection.getProperties().toString(), Map.class );
            StringWriter out = new StringWriter();
            JSONValue.writeJSONString(mapResponse, out);
            String jsonConnection = out.toString();
            connection.setProperties(jsonConnection);
        } catch( Exception e ) {

        }
        return connection;
    }

    @DELETE
    @Override
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    // 1a. Limit access based on CLASS level resources
    @ApiOperation(position=5, value="Delete a Connection")
    public Response deleteConnection( @PathParam("id") Long id )
    {
        Connection connection = ConnectionService.getInstance().get( id );
        if( connection == null )
        {
            return RestUtils.sendBadResponse( String.format( "ConnectionId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, connection );
        // handle validation in an Extensible manner
        List<Long> groupMqtt = GroupService.getInstance().getMqttGroups(connection.getGroup());
        validateDelete( connection );
        ConnectionService.getInstance().delete(connection);
        RecentService.getInstance().deleteRecent(id,"connection");
        sendMQTTSignal(groupMqtt);
        refreshCacheConnection(connection,true);
        return RestUtils.sendDeleteResponse();
    }

    public void validateDelete( Connection connection )
    {
        Field field = FieldService.getInstance().selectByName("oauth2Connection");
        if (field != null){
            BooleanBuilder be = new BooleanBuilder();
            be = be.and(QGroupField.groupField.field.eq(field));
            be = be.and(QGroupField.groupField.value.eq(connection.getCode()));
            Long groupFields = GroupFieldService.getInstance().countList(be);
            if (groupFields > 0){
                throw new UserException("Connection "+connection.getCode()+" is being used and cannot be deleted.");
            }
        }
    }

    /**
     * Sending message to refresh connections in coreBridge
     */
    public void sendMQTTSignal(List<Long> groupMqtt)
    {
        // Sending message to refresh coreBridge
        try
        {
            Class clazz = Class.forName(BROKER_CLIENT_HELPER);
            clazz.getMethod("sendRefreshConnectionConfigs", Boolean.class, String.class, List.class)
                    .invoke(null, false, Thread.currentThread().getName(), groupMqtt);
        }
        catch (Exception e) {
            logger.error("Could call MQTT sendRefreshConnectionConfigs method", e);
        }
    }


    /**
     * it updates a message of kafka cache topic ___v1___cache___connection.
     *
     * @param connection
     * @param delete
     */
    public void refreshCacheConnection(Connection connection, boolean delete){
        try
        {
            Class clazz = Class.forName(BROKER_CLIENT_HELPER);
            clazz.getMethod("refreshConnectionCache",Connection.class,boolean.class).invoke(null,connection, delete);
        }
        catch (Exception e) {
            logger.error(String.format("Could not refresh kafka cache(topic='___v1___cache___connection'), connection='%s'.",connection), e);
        }
    }

    public static String decode(String hashed) {
        return new String(org.apache.commons.codec.binary.Base64.decodeBase64(hashed.getBytes(Charsets.UTF_8)), Charsets.UTF_8);
    }

    /**
     *
     * @param connectionMap
     * @param connection
     * @return true if the Connection Type has a value: password
     */
    public boolean hasPasswordConnectionType(Map<String, Object> connectionMap, Connection connection){
        String passwordText = "password";
        ObjectMapper conecctionTypeMapper = new ObjectMapper();
        ConnectionType connectionType;
        if(connection != null){
            connectionType = connection.getConnectionType();
        }else{
            connectionType = ConnectionTypeServiceBase.getInstance().get(Long.parseLong(connectionMap.get("connectionType.id").toString()));
        }
        try {
            List connectionTypeList = (List) conecctionTypeMapper.readValue(connectionType.getPropertiesDefinitions().toString(), List.class);
            for (Object connectionTypeItem : connectionTypeList) {
                Map<String, String> connectionTypeItemMap = (Map<String, String>) connectionTypeItem;
                if (passwordText.equals(connectionTypeItemMap.get("code"))){
                    return true;
                }
            }
        } catch (JsonParseException e) {
            logger.error(e.getMessage(),e);
        } catch (JsonMappingException e) {
            logger.error(e.getMessage(),e);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
        return false;
    }

    @PATCH
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
    @ApiOperation(position=6, value="Update a Connection")
    public Response testConnection(@ApiParam(value = "required when editting a connection") @QueryParam("id") Long connectionId,
        @ApiParam(value = "required when creating a connection, accepted values are:"
            + "<br/>1: Internal SQL connection<br/>2: External Relational DB<br/>3: MQTT Broker"
            + "<br/>4: Mongo DB<br/>5: FTP Server<br/>6: Analytics connection<br/>7: REST connection"
            + "<br/>8: KAFKA broker<br/>9: Google Cloud API<br/>10: OAUTH2 Connection<br/>11: LDAP/AD Connection")
        @QueryParam("connectionType") Long connectionTypeId, Map<String, Object> properties )
    {
        Connection testConnection = new Connection();
        ConnectionType connectionType = new ConnectionType();
        if (connectionId != null && connectionId > 0) {
            connectionType = ConnectionService.getInstance().get(connectionId).getConnectionType();
        } else {
            connectionType = ConnectionTypeService.getInstance().get(connectionTypeId);
        }
        if (connectionId != null && connectionId > 0) {
            testConnection.setId(connectionId);
        }
        testConnection.setName(connectionType.getCode());
        testConnection.setConnectionType(connectionType);
        try {
            testConnection.setProperties(checkPassword(testConnection, new JSONObject(properties)));
            Map<String,Object> publicMap = testConnection.publicMap();
            ConnectionService.getInstance().testConnection(testConnection);
        } catch (UserException e) {
            JSONObject error = new JSONObject();
            error.put("message", "Cannot connect to " + testConnection.getName());
            error.put("error", "Error: " + e.getMessage());
            error.put("errorCode", "ErrorCode: " + e.getStatus());
            return RestUtils.sendJSONResponseWithCode(error.toJSONString(), 200);
        }
        return RestUtils.sendOkResponse(testConnection.getName() + " connection successful.");
    }

    private String checkPassword(Connection connection, JSONObject properties) {
        JSONObject connectionProps = new JSONObject((Map) properties.get("properties"));
        if (properties.containsKey("newPassword")) {
            JSONObject connectionPass = new JSONObject((Map) properties.get("newPassword"));
            if (!StringUtils.equals(connectionPass.get("password").toString(), connectionPass.get("confirmPassword").toString())) {
                throw new UserException("Password and Confirm Password do not match", 400);
            }
            connectionProps.put("password", connectionPass.get("password"));
        }
        JSONParser parser = new JSONParser();
        if (connection.getId() != null) {
            Connection fastConnection = ConnectionService.getInstance().get(connection.getId());
            try {
                JSONObject oldProps = (JSONObject) parser.parse(fastConnection.getProperties());
                if (oldProps.containsKey("password") && !connectionProps.containsKey("password")) {
                    connectionProps.put("password", fastConnection.getPassword(true));
                }
            } catch (Exception e) {
                logger.error("Error parsing current connection properties.", e);
            }
        }
        return connectionProps.toJSONString();
    }

}
