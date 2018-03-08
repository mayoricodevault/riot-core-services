package com.tierconnect.riot.migration.steps.connection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.steps.MigrationStep;

/**
 * Created by rhuanca on 11/17/15.
 */
public class Migrate_SQLConnectionType_VIZIX1298 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_SQLConnectionType_VIZIX1298.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        // no script file
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    private void migrateFeature() {
        ConnectionType connectionTypeByCode = ConnectionTypeService.getInstance().getConnectionTypeByCode("SQL");
        if (connectionTypeByCode == null) {
            Group rootGroup = GroupService.getInstance().getRootGroup();

            // SQL CONNECTION
            ConnectionType dbConnectionType = new ConnectionType();
            dbConnectionType.setGroup(rootGroup);
            dbConnectionType.setCode("SQL");
            dbConnectionType.setDescription("Internal SQL Connection");

            List<Map<String, Object>> propertyDefinitions = new ArrayList<>();

            propertyDefinitions.add(newPropertyDefinition("driver", "Driver", "String", null));
            propertyDefinitions.add(newPropertyDefinition("dialect", "Dialect", "String", null));
            propertyDefinitions.add(newPropertyDefinition("username", "Username", "String", null));
            propertyDefinitions.add(newPropertyDefinition("password", "Password", "String", null));
            propertyDefinitions.add(newPropertyDefinition("url", "URL", "String", null));
            propertyDefinitions.add(newPropertyDefinition("hazelcastNativeClientAddress",
                    "hazelcastNativeClientAddress", "String", "hazelcast"));

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                logger.info("Creating SQL Connection type.");
                dbConnectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertyDefinitions));
                ConnectionTypeService.getInstance().insert(dbConnectionType);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Adding SQLConnectionType failed.", e);
            }
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        // no script file
    }

    private static Map<String, Object> newPropertyDefinition(String code, String label, String type,
            String defaultValue) {
        Map<String, Object> propertyDefinition = new LinkedHashMap<>();
        propertyDefinition.put("code", code);
        propertyDefinition.put("label", label);
        propertyDefinition.put("type", type);
        if (defaultValue != null) {
            propertyDefinition.put("defaultValue", defaultValue);
        }
        return propertyDefinition;
    }

}
