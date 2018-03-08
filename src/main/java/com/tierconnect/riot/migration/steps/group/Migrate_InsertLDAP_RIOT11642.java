package com.tierconnect.riot.migration.steps.group;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_InsertLDAP_RIOT11642 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_InsertLDAP_RIOT11642.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        insertLdap();
    }

    private void insertLdap() {
        // Inserting data for LDAP - Authentication
        Group rootGroup = GroupService.getInstance().getRootGroup();
        Field f46 = PopDBUtils.popFieldService("native", "native", "Native Authentication",
                rootGroup, "Authentication Configuration", "java.lang.Boolean", 3L, true);
        PopDBUtils.popGroupField(rootGroup, f46, "true");
        Field f47 = PopDBUtils.popFieldService("ldap", "ldap", "LDAP - Active Directory Authentication",
                rootGroup, "Authentication Configuration", "java.lang.Boolean", 3L, true);
        PopDBUtils.popGroupField(rootGroup, f47, "false");
        Field f48 = PopDBUtils.popFieldService("ldapConnection", "ldapConnection", "LDAP Connection",
                rootGroup, "LDAP - Active Directory CONFIGURATION", "java.lang.String", 3L, true);
        PopDBUtils.popGroupField(rootGroup, f48, "");
        PopDBRequired.populateLDAPConnection(rootGroup);
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
