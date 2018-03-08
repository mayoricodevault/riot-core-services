package com.tierconnect.riot.migration.steps.connection;

import com.fasterxml.jackson.core.JsonProcessingException;
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
public class Migrate_DeleteUserFromConnectionType_RIOT12142 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_DeleteUserFromConnectionType_RIOT12142.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        deleteUserFromConnectionType();
    }

    private void deleteUserFromConnectionType() {
        Group rootGroup = GroupService.getInstance().get(1L);
        ConnectionType connectionType = ConnectionTypeService.getInstance().getConnectionTypeByCode("ldap");

        if (connectionType != null) {
            List<Map<String, Object>> propertiesDefinition = new ArrayList<>();
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("userDn", "UserDn", "String", true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("password", "Password", "String", true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("base", "Base", "String", true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("url", "Url", "String", true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("referral", "Referral ('follow' by default)" +
                    "", "String", false));
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                connectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertiesDefinition));
                ConnectionTypeService.getInstance().update(connectionType);
            } catch (JsonProcessingException e) {
                logger.warn("Migrating LDAP connection, error updating connectionType", e);
            }
        } else {
            PopDBRequired.populateLDAPConnection(rootGroup);
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
