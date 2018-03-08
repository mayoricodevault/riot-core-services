package com.tierconnect.riot.migration.steps.resource;
import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.iot.entities.SmartContractDefinition;
import com.tierconnect.riot.iot.entities.SmartContractParty;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;
/**
 * Created by mauricio on 5/30/17.
 */
public class Migrate_BlockchainResources_VIZIX5226 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_BlockchainResources_VIZIX5226.class);


    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateResources();
    }

    private void migrateResources() {
        logger.debug("Start migrating Blockchain resources");
        Group rootGroup = GroupService.getInstance().getRootGroup();
        Role rootRole = RoleService.getInstance().getRootRole();
        Role tenantAdminRole = RoleService.getInstance().getTenantAdminRole();
        ResourceService resourceService = ResourceService.getInstance();
        RoleResourceService roleResourceService = RoleResourceService.getInstance();
        Resource resource;

        Resource moduleBlockchain = resourceService.insert(Resource.getModuleResource(rootGroup, "Blockchain", "Blockchain"));
        roleResourceService.insert(rootRole, moduleBlockchain, moduleBlockchain.getAcceptedAttributes());
        resourceService.update(moduleBlockchain);

        // Smart Contract Definition Resource
        Resource smartContractDefResource = Resource.getClassResource(rootGroup, SmartContractDefinition.class, moduleBlockchain);
        smartContractDefResource.setTreeLevel(2);
        resource = resourceService.insert(smartContractDefResource);
        roleResourceService.insert(rootRole, resource, resource.getAcceptedAttributes());
        resourceService.update(resource);

        // Smart Contract Party Resource
        Resource smartContractPartyResource = Resource.getClassResource(rootGroup, SmartContractParty.class, moduleBlockchain);
        smartContractPartyResource.setTreeLevel(2);
        resource = resourceService.insert(smartContractPartyResource);
        roleResourceService.insert(rootRole, resource, resource.getAcceptedAttributes());
        resourceService.update(resource);

        // Smart Contract Resource
        Resource smartContractResource = new Resource(rootGroup, "smartcontract", "r");
        smartContractResource.setParent(moduleBlockchain);
        smartContractResource.setTreeLevel(2);
        smartContractResource.setLabel(smartContractResource.getName());
        smartContractResource.setFqname(smartContractResource.getName());
        smartContractResource.setDescription("smart contract things");
        smartContractResource.setType(ResourceType.THING_TYPE_CLASS.getId());
        resource = resourceService.insert(smartContractResource);
        roleResourceService.insert(rootRole, resource, resource.getAcceptedAttributes());
        resourceService.update(resource);
        logger.debug("End migrating Blockchain resources");
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

}
