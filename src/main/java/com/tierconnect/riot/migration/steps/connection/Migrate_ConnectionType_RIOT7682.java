package com.tierconnect.riot.migration.steps.connection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.entities.QConnectionType;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_ConnectionType_RIOT7682 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ConnectionType_RIOT7682.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateConnectionType();
    }

    private void migrateConnectionType()
    {
        ConnectionType dbConnectionType;
        dbConnectionType = ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy(QConnectionType.connectionType.code.eq("DBConnection"));

        List propertyDefinitions = new ArrayList();
        Map propertyDefinition = new HashMap();
        propertyDefinition.put("code", "driver");
        propertyDefinition.put("label", "Driver");
        propertyDefinition.put("type", "Array");

        List propertyArrayValues = new ArrayList();
        Map propertyArrayValue = new HashMap();
        propertyArrayValue.put("label", "SQLServer");
        propertyArrayValue.put("driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        propertyArrayValue.put("urlExample", "jdbc:sqlserver://localhost;DatabaseName=database");
        propertyArrayValues.add(propertyArrayValue);
        propertyArrayValue = new HashMap();
        propertyArrayValue.put("label", "MySQL");
        propertyArrayValue.put("driver", "com.mysql.jdbc.Driver");
        propertyArrayValue.put("urlExample", "jdbc:mysql://localhost:3306/database");
        propertyArrayValues.add(propertyArrayValue);
        propertyDefinition.put("values", propertyArrayValues);

        propertyDefinitions.add(propertyDefinition);
        propertyDefinition = new HashMap();
        propertyDefinition.put("code", "user");
        propertyDefinition.put("label", "User");
        propertyDefinition.put("type", "String");
        propertyDefinitions.add(propertyDefinition);
        propertyDefinition = new HashMap();
        propertyDefinition.put("code", "password");
        propertyDefinition.put("label", "Password");
        propertyDefinition.put("type", "String");
        propertyDefinitions.add(propertyDefinition);
        propertyDefinition = new HashMap();
        propertyDefinition.put("code", "url");
        propertyDefinition.put("label", "URL");
        propertyDefinition.put("type", "String");
        propertyDefinitions.add(propertyDefinition);
        propertyDefinition = new HashMap();
        propertyDefinition.put("code", "schema");
        propertyDefinition.put("label", "Schema");
        propertyDefinition.put("type", "String");
        propertyDefinition.put("defaultValue", "riot_main");
        propertyDefinitions.add(propertyDefinition);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            dbConnectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertyDefinitions));
            ConnectionTypeService.getInstance().update(dbConnectionType);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
