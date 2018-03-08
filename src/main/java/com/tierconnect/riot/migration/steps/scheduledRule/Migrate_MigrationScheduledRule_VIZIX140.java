package com.tierconnect.riot.migration.steps.scheduledRule;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.iot.entities.ScheduledRule;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

import static com.tierconnect.riot.appcore.entities.Resource.RESOURCE_SCHEDULED_RULE_NAME;
import static com.tierconnect.riot.appcore.entities.Resource.getClassResource;
import static com.tierconnect.riot.appcore.entities.Resource.getModuleResource;

/**
 * Created by fflores
 * on 06/26/17
 */
public class Migrate_MigrationScheduledRule_VIZIX140 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrationScheduledRule_VIZIX140.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateScheduledRule();
    }

    private void migrateScheduledRule() {
        Group group = GroupService.getInstance().get(1L);
        Resource moduleGateway = ResourceServiceBase.getResourceDAO().selectBy(QResource.resource.name.eq("Gateway"));
        Resource scheduledRule = ResourceService.getInstance().insert(getModuleResource(group,
                "Scheduled Rule",
                RESOURCE_SCHEDULED_RULE_NAME,
                moduleGateway));
        ResourceService.getInstance().insert(getClassResource(group, ScheduledRule.class, scheduledRule));
        Role root = RoleService.getInstance().getRootRole();
        Resource bridgeResource = ResourceServiceBase.getResourceDAO().selectBy(QResource.resource.name.eq("Bridges & Rules"));
        RoleResourceService.getInstance().insert(root,scheduledRule,"x");
        Resource resourceScheduled = ResourceServiceBase.getResourceDAO().selectBy(QResource.resource.name.eq("scheduledRule"));
        RoleResourceService.getInstance().insert(root,resourceScheduled,"riuda");
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QRoleResource.roleResource.resource.eq(bridgeResource));
        be = be.and(QRoleResource.roleResource.role.ne(root));
        List<RoleResource> roleResources = RoleResourceService.getInstance().listPaginated(be,null,null);
        for(RoleResource roleResource : roleResources){
            RoleResourceService.getInstance().insert(roleResource.getRole(),scheduledRule,"x");
            RoleResourceService.getInstance().insert(roleResource.getRole(),resourceScheduled,"riuda");
        }
        Resource resourceEdgeboxRule = ResourceServiceBase.getResourceDAO().selectBy(QResource.resource.name.eq("edgeboxRule"));
        if (resourceEdgeboxRule != null){
            resourceEdgeboxRule.setParent(moduleGateway);
            ResourceService.getInstance().update(resourceEdgeboxRule);
        }

        Resource resourceParameters = ResourceServiceBase.getResourceDAO().selectBy(QResource.resource.name.eq("parameters"));
        if (resourceParameters != null){
            resourceParameters.setParent(moduleGateway);
            ResourceService.getInstance().update(resourceParameters);
        }

        Field f66 = PopDBUtils.popFieldService("scheduledRule", "scheduledRule", "Scheduled Rule", group, "Ownership Levels",
                "java.lang.Integer", 3L, true);
        PopDBUtils.popGroupField(group, f66, "2");
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
