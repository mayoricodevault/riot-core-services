package com.tierconnect.riot.migration.older;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.ReportProperty;
import com.tierconnect.riot.iot.services.ReportPropertyService;
import com.tierconnect.riot.migration.DBHelper;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.util.*;

/**
 * Created by cfernandez
 * on 11/17/15.
 */
@Deprecated
public class V_030300_030301 implements MigrationStepOld
{

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(30300);
    }

    @Override
    public int getToVersion() {
        return 30301;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V030300_to_030301.sql");
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateGroupFields();
        migrateConnectionType();
        migrateInlineEdit();
        migrateConnection();
    }

    private void migrateGroupFields()
    {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        PopDBUtils.migrateFieldService("sendThingFieldTickle", "sendThingFieldTickle", "Run Rules After Import", rootGroup, "Import Configuration", "java.lang.Boolean", 3L, true, "false");
    }

    @Override
    public void migrateSQLAfter() throws Exception {

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

    private void migrateInlineEdit(){

        for(ReportProperty reportProperty : ReportPropertyService.getReportPropertyDAO().selectAll()){
            reportProperty.setEditInline(false);
            ReportPropertyService.getReportPropertyDAO().update(reportProperty);
        }

    }

    private void migrateConnection()
    {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        ConnectionType dbConnectionType = new ConnectionType();
        dbConnectionType = ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy(QConnectionType.connectionType.code.eq("DBConnection"));

        // MySQL connection example
        Connection connection = new Connection();
        connection.setName("MySQLServer");
        connection.setCode("MySQLServer");
        connection.setGroup(rootGroup);
        connection.setConnectionType( dbConnectionType );
        JSONObject jsonProperties = new JSONObject();
        Map<String, String> mapProperties = new LinkedHashMap<String, String>();
        mapProperties.put( "driver", "com.mysql.jdbc.Driver" );
        mapProperties.put( "password", "Y29udHJvbDEyMyE=" );
        mapProperties.put( "schema", "ct-app-center" );
        mapProperties.put( "url", "jdbc:mysql://localhost:3306/ct-app-center" );
        mapProperties.put( "user", "root" );
        jsonProperties.putAll(mapProperties);
        connection.setProperties(jsonProperties.toJSONString());
        ConnectionService.getInstance().insert( connection );
    }
}
