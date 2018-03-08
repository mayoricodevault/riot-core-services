package com.tierconnect.riot.appcore.controllers;

import javax.annotation.Generated;
import javax.ws.rs.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.wordnik.swagger.annotations.Api;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Path("/connectionType")
@Api("/connectionType")
@Generated("com.tierconnect.riot.appgen.service.GenController")
public class ConnectionTypeController extends ConnectionTypeControllerBase 
{
    @Override
    public void addToPublicMap(ConnectionType connectionType, Map<String, Object> publicMap, String extra) {
        ObjectMapper objectMapper = new ObjectMapper();
        String propertiesDefinitionString = connectionType.getPropertiesDefinitions();
        if (StringUtils.isNotEmpty(propertiesDefinitionString)) {
            try {
                publicMap.put("propertiesDefinitions", objectMapper.readValue(propertiesDefinitionString, List.class));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

