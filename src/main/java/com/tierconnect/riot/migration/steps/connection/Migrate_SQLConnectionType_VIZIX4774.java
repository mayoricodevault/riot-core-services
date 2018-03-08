package com.tierconnect.riot.migration.steps.connection;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.*;

/**
 * Created by hmartinez on 18-05-17.
 */
public class Migrate_SQLConnectionType_VIZIX4774 implements MigrationStep{

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

    private void migrateConnectionType() throws ParseException {
        Connection dbConnection;
        dbConnection = ConnectionService.getInstance().getByCode("SQL");

        JSONParser parser = new JSONParser();
        JSONObject jsonProperties = (JSONObject) parser.parse(dbConnection.getProperties());

        String hashedPassword = Connection.encode(String.valueOf(jsonProperties.get("password")));
        jsonProperties.put("password", hashedPassword);

        dbConnection.setProperties(jsonProperties.toJSONString());
        ConnectionService.getInstance().update(dbConnection);
    }
}
