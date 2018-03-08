package com.tierconnect.riot.migration.steps.resource;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.entities.ResourceType;
import com.tierconnect.riot.appcore.entities.Role;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.iot.entities.SmartContractDefinition;
import com.tierconnect.riot.iot.entities.SmartContractParty;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.popdb.PopulateResourcesIOT;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.HashSet;

/**
 * Created by mauricio on 5/30/17.
 */
public class Migrate_RetailAppResources_VIZIX6598 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_RetailAppResources_VIZIX6598.class);


    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateResources();
    }

    private void migrateResources() {
        logger.debug("Start migrating Retail App resources");
        Group rootGroup = GroupService.getInstance().getRootGroup();
        Role rootRole = RoleService.getInstance().getRootRole();

        HashSet<Resource> resources = new HashSet<>();
                PopulateResourcesIOT.populateRetailAppResources(rootGroup, resources);

        for (Resource resource : resources) {
            RoleResourceService.getInstance().insert(rootRole, resource, resource.getAcceptedAttributes());
            ResourceService.getInstance().update(resource);
        }

        logger.debug("End migrating Retail App resources");
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

}
