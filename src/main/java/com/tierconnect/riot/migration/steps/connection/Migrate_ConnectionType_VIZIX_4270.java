package com.tierconnect.riot.migration.steps.connection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.entities.QConnectionType;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.migration.steps.MigrationStep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hmartinez on 04-05-17.
 */
public class Migrate_ConnectionType_VIZIX_4270 implements MigrationStep{
    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateConnectionType();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void migrateConnectionType() {
        ConnectionType dbConnectionType;
        dbConnectionType = ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy(QConnectionType.connectionType.code.eq("MQTT"));

        List propertyDefinitions = new ArrayList();
        Map propertyDefinition = new HashMap();
        propertyDefinition.put("code", "host");
        propertyDefinition.put("label", "Host");
        propertyDefinition.put("type", "String");
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap();
        propertyDefinition.put("code", "port");
        propertyDefinition.put("label", "Port");
        propertyDefinition.put("type", "Number");
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap();
        propertyDefinition.put("code", "qos");
        propertyDefinition.put("label", "Qos");
        propertyDefinition.put("type", "Number");
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap();
        propertyDefinition.put("code", "secure");
        propertyDefinition.put("label", "Secure");
        propertyDefinition.put("type", "Boolean");
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap();
        propertyDefinition.put("code", "username");
        propertyDefinition.put("label", "Username");
        propertyDefinition.put("type", "String");
        propertyDefinitions.add(propertyDefinition);

        propertyDefinition = new HashMap<>();
        propertyDefinition.put("code", "password");
        propertyDefinition.put("label", "Password");
        propertyDefinition.put("type", "String");
        propertyDefinitions.add(propertyDefinition);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            dbConnectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertyDefinitions));
            ConnectionTypeService.getInstance().update(dbConnectionType);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
