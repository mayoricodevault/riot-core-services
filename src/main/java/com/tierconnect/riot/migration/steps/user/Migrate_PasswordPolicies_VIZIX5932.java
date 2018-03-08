package com.tierconnect.riot.migration.steps.user;

import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by dbascope on 07/07/2017
 */
public class Migrate_PasswordPolicies_VIZIX5932 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_PasswordPolicies_VIZIX5932.class);


    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        PopDBRequired.populatePasswordPoliciesFields(rootGroup);

        ResourceService resourceService = ResourceService.getInstance();
        Resource moduleSecurity = Resource.getModuleResource(rootGroup, "Security", "Security");
        resourceService.insert(moduleSecurity);

        resourceService.insert(Resource.getModuleResource(rootGroup,
            "Password Expiration Policy",
            "passwordExpirationPolicy",
            moduleSecurity));

    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

}
