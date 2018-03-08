package com.tierconnect.riot.migration.steps.group;

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
public class Migrate_MigrateLdapParameter_NOISSUE implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrateLdapParameter_NOISSUE.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateLdapParameter();
    }

    /**
     * migrate ldapValidateUserCreation parameter for LDAP/AD Authentication
     */
    private static void migrateLdapParameter() {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        Field field = PopDBUtils.popFieldService("ldapValidateUserCreation", "ldapValidateUserCreation", "LDAP/AD " +
                "Validate User Creation", rootGroup, "Security Configuration", "java.lang.Boolean", 3L, true);
        PopDBUtils.popGroupField(rootGroup, field, "false");
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
