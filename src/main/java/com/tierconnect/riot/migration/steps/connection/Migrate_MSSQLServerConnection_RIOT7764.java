package com.tierconnect.riot.migration.steps.connection;

import com.tierconnect.riot.appcore.entities.*;
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
public class Migrate_MSSQLServerConnection_RIOT7764 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MSSQLServerConnection_RIOT7764.class);

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
        ConnectionType dbConnectionType = null;
        dbConnectionType = ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy(QConnectionType.connectionType.code.eq("DBConnection"));

        Connection connectionTemp = ConnectionService.getConnectionDAO().selectBy(QConnection.connection.code.eq("MSSQLServer"));
        if(connectionTemp == null){// Create only if connection does not exist
            // SQLServer connection example
            Connection connection = new Connection();
            connection.setName( "MSSQLServer" );
            connection.setCode( "MSSQLServer" );
            connection.setGroup(rootGroup);
            connection.setConnectionType( dbConnectionType );
            JSONObject jsonProperties = new JSONObject();
            Map<String, String> mapProperties = new LinkedHashMap<String, String>();
            mapProperties.put( "driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver" );
            mapProperties.put( "password", "Y29udHJvbDEyMyE=" );
            mapProperties.put( "schema", "DWMS" );
            mapProperties.put( "url", "jdbc:sqlserver://localhost;DatabaseName=DWMS" );
            mapProperties.put( "user", "sa" );
            jsonProperties.putAll(mapProperties);
            connection.setProperties(jsonProperties.toJSONString());
            ConnectionService.getInstance().insert( connection );
        }

    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
