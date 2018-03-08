package com.tierconnect.riot.migration.older;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QConnection;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.services.EdgeboxRuleService;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by ybarriga on 11/14/2016
 */
@Deprecated
public class V_040500_RC1_040500_RC2 implements MigrationStepOld {
    static Logger logger = Logger.getLogger(V_040500_RC1_040500_RC2.class);

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(4050001);
    }

    @Override
    public int getToVersion() {
        return 4050002;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = DBHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V040500_RC1_to_040500_RC2.sql");
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateConnectiontypeMongo();
        migrationMongoToShardConfig();
        migrateBridgeRule();
    }

    @Override
    public void migrateSQLAfter() throws Exception {
    }

    private void migrateConnectiontypeMongo() {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        ConnectionTypeService service = ConnectionTypeService.getInstance();
        ConnectionType mongo = service.getConnectionTypeByCode("MONGO");
        if (mongo == null) {
            PopDBRequired.populateMongoShardingConnection(rootGroup);
        } else {
            List<Map<String, Object>> propertyDefinitions = new ArrayList<>();
            propertyDefinitions.add(PopDBRequired.newPropertyDefinition("mongoPrimary", "Mongo Primary", "String", true)); //100.10.0.25:27017
            propertyDefinitions.add(PopDBRequired.newPropertyDefinition("mongoSecondary", "Mongo Secondary", "String", false)); //100.10.0.25:27017,100.10.0.26:27017
            propertyDefinitions.add(PopDBRequired.newPropertyDefinition("mongoReplicaSet", "Mongo Replica Set", "String", false)); //rs0_name
            propertyDefinitions.add(PopDBRequired.newPropertyDefinition("mongoSSL", "Mongo SSL", "Boolean", false));//true|false
            propertyDefinitions.add(PopDBRequired.newPropertyDefinition("username", "Username", "String", true));
            propertyDefinitions.add(PopDBRequired.newPropertyDefinition("password", "Password", "String", true));
            propertyDefinitions.add(PopDBRequired.newPropertyDefinition("mongoAuthDB", "Mongo Authentication DB", "String", true));//admin (--authenticationDatabase)
            propertyDefinitions.add(PopDBRequired.newPropertyDefinition("mongoDB", "Mongo Data Base", "String", true));//riot_main
            propertyDefinitions.add(PopDBRequired.newPropertyDefinition("mongoSharding", "Mongo Sharding", "Boolean", true));//true|false (*)
            propertyDefinitions.add(PopDBRequired.newPropertyDefinition("mongoConnectTimeout", "Connection Timeout", "Number", true));//30000 | 0
            propertyDefinitions.add(PopDBRequired.newPropertyDefinition("mongoMaxPoolSize", "Max Pool Size", "Number", true));//100 | 0
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                mongo.setPropertiesDefinitions(objectMapper.writeValueAsString(propertyDefinitions));
                service.update(mongo);
                logger.info("update connection type MONGO");
            } catch (Exception e) {
                logger.error("migration connection type MONGO", e);
            }

        }
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

    private void migrateBridgeRule() {
        EdgeboxRuleService instance = EdgeboxRuleService.getInstance();
        List<EdgeboxRule> edgeboxRuleList = instance.selectByAction("ThingPropertySetterJSSubscriber");
        for (EdgeboxRule rule : edgeboxRuleList) {
            rule.setConditionType("ALWAYS_TRUE");
            rule.setRule("select * from messageEventType where 1=1 ");
            instance.update(rule);
            logger.info("update rule " + rule.getName());
        }
    }
}