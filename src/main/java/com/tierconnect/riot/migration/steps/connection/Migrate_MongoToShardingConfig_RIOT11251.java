package com.tierconnect.riot.migration.steps.connection;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.QConnection;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_MongoToShardingConfig_RIOT11251 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MongoToShardingConfig_RIOT11251.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrationMongoToShardConfig();
    }

    public void migrationMongoToShardConfig() {
        try {
            List<Connection> connectionList = ConnectionService.getInstance().getConnectionDAO()
                    .selectAllBy(QConnection.connection.connectionType.code.eq("MONGO"));
            if ((connectionList != null) && (!connectionList.isEmpty())) {
                ObjectMapper mapper = new ObjectMapper();
                for (Connection conn : connectionList) {
                    Map<String, Object> map = mapper.readValue(conn.getProperties(), new TypeReference<Map<String, Object>>() {
                    });
                    Map<String, Object> tarjet = new LinkedHashMap<>();
                    tarjet.put("mongoPrimary", String.valueOf(map.get("host")) + ":" + String.valueOf(map.get("port")));
                    tarjet.put("mongoSecondary", StringUtils.EMPTY);
                    tarjet.put("mongoReplicaSet", StringUtils.EMPTY);
                    tarjet.put("mongoSSL", Boolean.FALSE);
                    tarjet.put("username", map.get("username"));
                    tarjet.put("password", map.get("password"));
                    tarjet.put("mongoAuthDB", map.get("username"));
                    tarjet.put("mongoDB", map.get("dbname"));
                    tarjet.put("mongoSharding", Boolean.FALSE);
                    tarjet.put("mongoConnectTimeout", 0);
                    tarjet.put("mongoMaxPoolSize", 0);

                    conn.setProperties(mapper.writeValueAsString(tarjet));
                    ConnectionService.getInstance().update(conn);
                }
            }
        } catch (Exception e) {
            logger.error("Error parsing Connection Configuration...", e);
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
