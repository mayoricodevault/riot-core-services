package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QConnectionType;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by brayan on 7/3/17.
 */
public class Migrate_AddRestConnectionToCB_VIZIX140 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_AddRestConnectionToCB_VIZIX140.class);
    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        controlCoreBridgeRestConnection();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void controlCoreBridgeRestConnection() {
        // select all coreBridges
        List<Edgebox> edgeboxes = EdgeboxService.getInstance().getByType("core");
        for (Edgebox edgebox : edgeboxes){
            // check if each coreBridge contains a rest connection
            try {
                JSONObject outputConfig = (JSONObject) new JSONParser().parse(edgebox.getConfiguration());
                if (!outputConfig.containsKey("rest")) {
                    // if not contains
                    // create a new rest connection based on CoreBridge Group
                    String connectionCode;
                    if (edgebox.getCode().equals("MCB")) {
                        connectionCode = "SERVICES";
                    } else {
                        connectionCode = createRestConnectionByTenant(edgebox.getGroup());
                    }
                    JSONObject connectionRestObjectValue = new JSONObject();
                    connectionRestObjectValue.put("connectionCode", connectionCode);
                    outputConfig.put("rest", connectionRestObjectValue);
                    edgebox.setConfiguration(outputConfig.toJSONString());
                    // update the coreBridge configuration with the last rest connection created
                    EdgeboxService.getInstance().update(edgebox);
                    logger.info("Edgebox code=" + edgebox.getCode() + " updated with a new restConnection code=" + connectionCode);
                }
            } catch (ParseException e) {
                logger.error("Error in update configuration CORE with code:" + edgebox.getCode(), e);
            }
        }
    }

    private String createRestConnectionByTenant(Group group){
        Connection connection = new Connection();
        String connectionName = "Services_" + group.getCode();
        String connectionCode = "SERVICES" + group.getCode();
        // check if connection service already exist
        connection = ConnectionService.getInstance().getByCode(connectionCode);
        if (connection == null) {
            connection = new Connection();
            connection.setName(connectionName);
            connection.setCode(connectionCode);
            connection.setGroup(group);
            connection.setConnectionType(
                    ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy(QConnectionType.connectionType.code.eq("SERVICES")));
            JSONObject jsonProperties = new JSONObject();
            LinkedHashMap<String, Object> mapProperties = new LinkedHashMap<String, Object>();
            mapProperties.put("host", "localhost");
            mapProperties.put("port", 8080);
            mapProperties.put("contextpath", "/riot-core-services");
            mapProperties.put("apikey", "7B4BCCDC");
            mapProperties.put("secure", false);
            jsonProperties.putAll(mapProperties);
            connection.setProperties(jsonProperties.toJSONString());
            ConnectionService.getInstance().insert(connection);
        }
        return connection.getCode();
    }
}
