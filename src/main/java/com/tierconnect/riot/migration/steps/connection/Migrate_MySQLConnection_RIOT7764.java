package com.tierconnect.riot.migration.steps.connection;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QConnectionType;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_MySQLConnection_RIOT7764 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MySQLConnection_RIOT7764.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateConnection();
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


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
