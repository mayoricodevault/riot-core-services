package com.tierconnect.riot.migration.steps.connection;

import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dbascope
 * on 05/17/17
 */
public class Migrate_RequiredConnectionTypes_VIZIX3930 implements MigrationStep {
    private static Map<String, Map<String, Boolean>> nonRequiredMap = new HashMap<>();

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        setNonRequiredMap();
        List<ConnectionType> conns = ConnectionTypeService.getConnectionTypeDAO().selectAll();
        JSONParser parser = new JSONParser();
        for (ConnectionType connectionType : conns) {
            JSONArray json = (JSONArray) parser.parse(connectionType.getPropertiesDefinitions());
            for (Object property : json) {
                if (((JSONObject) property).get("code") != null) {
                    ((JSONObject) property).put("required", getRequiredValue(connectionType.getCode(), ((JSONObject) property).get("code").toString()));
                }
            }
            connectionType.setPropertiesDefinitions(json.toString());
            ConnectionTypeService.getInstance().update(connectionType);
        }
    }

    private void setNonRequiredMap() {
        Map<String, Boolean> nonRequired = new HashMap<>();
        nonRequired.put("secure", false);
        nonRequiredMap.put("SERVICES", nonRequired);
        nonRequired.put("username", false);
        nonRequired.put("password", false);
        nonRequired.put("secure", false);
        nonRequiredMap.put("MQTT", nonRequired);
        nonRequired = new HashMap<>();
        nonRequired.put("mongoSecondary", false);
        nonRequired.put("mongoReplicaSet", false);
        nonRequired.put("mongoSSL", false);
        nonRequiredMap.put("MONGO", nonRequired);
        nonRequired = new HashMap<>();
        nonRequired.put("secure", false);
        nonRequiredMap.put("FTP", nonRequired);
        nonRequired = new HashMap<>();
        nonRequired.put("masterHost", false);
        nonRequired.put("masterPort", false);
        nonRequired.put("clusterMode", false);
        nonRequired.put("responseTimeout", false);
        nonRequired.put("mongo.secure", false);
        nonRequiredMap.put("ANALYTICS", nonRequired);
        nonRequired = new HashMap<>();
        nonRequired.put("secure", false);
        nonRequiredMap.put("REST", nonRequired);
        nonRequired = new HashMap<>();
        nonRequired.put("referral", false);
        nonRequiredMap.put("ldap", nonRequired);
    }

    private Boolean getRequiredValue(String connectionTypeCode, String propertyCode) {
        if (nonRequiredMap.containsKey(connectionTypeCode)
                && nonRequiredMap.get(connectionTypeCode).containsKey(propertyCode)) {
            return nonRequiredMap.get(connectionTypeCode).get(propertyCode);
        }
        return true;
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }
}
