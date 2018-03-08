package com.tierconnect.riot.migration.steps.connection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_FixMigrationConnectionTypeMongo_RIOT13235 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_FixMigrationConnectionTypeMongo_RIOT13235.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateConnectiontypeMongo();
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

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
