package com.tierconnect.riot.migration.steps.connection;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by wargandona
 * on 03/02/17.
 */
public class Migrate_RestConnection_VIZIX1957 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_RestConnection_VIZIX1957.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateConnection();
    }

    private void migrateConnection() {
        ConnectionType restConnectionType = ConnectionTypeService.getInstance().getConnectionTypeByCode("REST");
        if (restConnectionType == null) {
            restConnectionType = new ConnectionType();
            Group rootGroup = GroupService.getInstance().getRootGroup();

            restConnectionType.setCode("REST");
            restConnectionType.setGroup(rootGroup);
            restConnectionType.setDescription("REST Connection");
            restConnectionType.setPropertiesDefinitions("[{\"code\":\"host\",\"label\":\"Host\",\"type\":\"String\"}," +
                    "{\"code\":\"port\",\"label\":\"Port\",\"type\":\"Number\",\"port\":\"Number\"}," +
                    "{\"code\":\"contextpath\",\"label\":\"Contextpath\",\"type\":\"String\"}," +
                    "{\"code\":\"apikey\",\"label\":\"Apikey\",\"type\":\"String\"}," +
                    "{\"code\":\"secure\",\"label\":\"Secure\",\"type\":\"Boolean\"}]");

            restConnectionType = ConnectionTypeService.getInstance().insert(restConnectionType);

            Connection connection = new Connection();
            connection.setCode("BlockchainAdapter");
            connection.setName("Blockain adapter");
            connection.setGroup(rootGroup);
            connection.setConnectionType(restConnectionType);

            JSONObject jsonProperties = new JSONObject();
            Map<String, Object> mapProperties = new LinkedHashMap<>();
            mapProperties.put("host", "localhost");
            mapProperties.put("port", 3000);
            mapProperties.put("contextpath", "/");
            mapProperties.put("apiKey", "root");
            mapProperties.put("secure", false);
            jsonProperties.putAll(mapProperties);
            connection.setProperties(jsonProperties.toJSONString());

            ConnectionService.getInstance().insert(connection);
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
    }
}
