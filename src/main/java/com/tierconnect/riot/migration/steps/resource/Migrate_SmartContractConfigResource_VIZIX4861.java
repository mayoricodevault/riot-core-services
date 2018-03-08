package com.tierconnect.riot.migration.steps.resource;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.entities.Role;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.iot.entities.SmartContractConfig;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

/**
 * Created by mauricio on 6/23/17.
 */
public class Migrate_SmartContractConfigResource_VIZIX4861 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_SmartContractConfigResource_VIZIX4861.class);


    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateResources();
    }

    private void migrateResources() {

        Group rootGroup = GroupService.getInstance().getRootGroup();
        Role rootRole = RoleService.getInstance().getRootRole();
        ResourceService resourceService = ResourceService.getInstance();
        RoleResourceService roleResourceService = RoleResourceService.getInstance();
        Resource module  = null;
        Resource resource = null;
        try {
            module = resourceService.getByNameAndGroup("Blockchain",rootGroup);
        } catch (NonUniqueResultException e) {
            e.printStackTrace();
        }
        if(module!=null ) {
            // Smart Contract Configuration Resource
            Resource smartContractConfigResource = Resource.getClassResource(rootGroup, SmartContractConfig.class, module);
            smartContractConfigResource.setTreeLevel(2);
            resource = resourceService.insert(smartContractConfigResource);
            roleResourceService.insert(rootRole, resource, resource.getAcceptedAttributes());
            resourceService.update(resource);
        }

    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

}
