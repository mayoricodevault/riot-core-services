package com.tierconnect.riot.migration.steps.connection;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.entities.QConnectionType;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.utils.JsonUtils;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;

import java.io.IOException;
import java.util.*;

import static com.tierconnect.riot.iot.popdb.PopDBKafka.newPropertyDefinition;

/**
 * Migrate_Connection_VIZIX3318 class.
 *
 * @author rchirinos
 * @version 25/05/2017
 */
public class Migrate_ConnectionRequiredFields_VIZIX5062 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ConnectionRequiredFields_VIZIX5062.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateConnectionsRequiredFields();
    }

    /**
     * Migrate connection type.
     *
     * @throws NonUniqueResultException
     */
    private void migrateConnectionsRequiredFields() throws NonUniqueResultException, IOException {
        List<Connection> lstConnection = ConnectionService.getInstance().listPaginated(null, null);
        for(Connection connection : lstConnection) {
            if(connection.getConnectionType().getCode().equals("MONGO")) {
                try{
                    JSONObject jsonObj = (JSONObject) new JSONParser().parse(connection.getProperties());
                    if(!jsonObj.containsKey("mongoConnectTimeout")
                            || (jsonObj.get("mongoConnectTimeout") == null)
                            || (jsonObj.get("mongoConnectTimeout")!=null && jsonObj.get("mongoConnectTimeout").toString().isEmpty()) ) {
                        jsonObj.put("mongoConnectTimeout", 0);
                    }
                    if(!jsonObj.containsKey("mongoMaxPoolSize")
                            || (jsonObj.get("mongoMaxPoolSize") == null)
                            || (jsonObj.get("mongoMaxPoolSize")!=null && jsonObj.get("mongoMaxPoolSize").toString().isEmpty()) ) {
                        jsonObj.put("mongoMaxPoolSize", 0);
                    }
                    connection.setProperties(jsonObj.toJSONString());
                    ConnectionService.getInstance().update(connection);
                } catch (Exception e) {
                    logger.error("Error parsing properties of connections.", e);
                }
            }
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath)
    throws Exception {
    }
}
