package com.tierconnect.riot.migration.steps.authentication;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_MigrationLDAPUserIdentifier_VIZIX897 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrationLDAPUserIdentifier_VIZIX897.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateLDAPUserIdentifier();
    }

    private static void migrateLDAPUserIdentifier() {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        Field field = PopDBUtils.popFieldService("ldapUserIdentifier", "ldapUserIdentifier", "LDAP/AD User Identifier",
                rootGroup, "Security Configuration", "java.lang.String", 3L, true);
        PopDBUtils.popGroupField(rootGroup, field, "sAMAccountName");
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
