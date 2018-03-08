package com.tierconnect.riot.migration.steps.resource;

import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_MoveReportInstances_NOISSUE implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MoveReportInstances_NOISSUE.class);

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
        ResourceService resourceService = ResourceService.getInstance();
        ResourceDAO resourceDAO = ResourceService.getResourceDAO();

        if (resourceDAO.selectBy("name", Resource.REPORT_INSTANCES_MODULE) == null) {
            Resource moduleReportInstances = ResourceService.getInstance().insert(Resource.getModuleResource(rootGroup, Resource.REPORT_INSTANCES_MODULE, "Report Instances"));
            List<Resource> resources = resourceDAO.selectAllBy(QResource.resource.type.eq(ResourceType.REPORT_DEFINITION.getId()));
            for (Resource resource : resources) {
               // resource.setModule(moduleReportInstances.getName());
                resource.setParent(moduleReportInstances);
            }
        }

        Resource rEditOwn = resourceDAO.selectBy("name", "reportDefinition_editOwn");
        rEditOwn.setLabel("Edit own report");

        Resource emailRecipients = resourceDAO.selectBy("name", "reportDefinition_emailRecipients");
        emailRecipients.setLabel("Set email recipients");
        emailRecipients.setAcceptedAttributes("ru");
        // Resource rdAssignThing = resourceDAO.selectBy("name", "reportDefinition_assignThing");
        // rdAssignThing.setLabel("Thing Associate");

        // Resource rdUnAssignThing = resourceDAO.selectBy("name", "reportDefinition_unAssignThing");
        // rdUnAssignThing.setLabel("Thing Disassociate");

        modifyAssociateDisassociateResource();

        Resource rdAssignUnAssignThing = resourceDAO.selectBy("name", "reportDefinition_assignUnAssignThing");
        rdAssignUnAssignThing.setLabel("Thing Associate & Disassociate");

        Resource rdInlineEdit = resourceDAO.selectBy("name", "reportDefinition_inlineEdit");
        rdInlineEdit.setLabel("Thing Property");

        Resource r = resourceDAO.selectBy("name", "reportDefinition_inlineEditGroup");
        r.setFqname("Inline Edit");
        r.setLabel("Inline Edit");

        updateZPLEThingType();
    }

    private void updateZPLEThingType()
    {
        // Get Group level 3
        long groupId = 3;
        Group group = GroupService.getInstance().get(groupId);
        // If there not exist the group level 3 get the level 2
        if ( group == null )
        {
            groupId = 2;
            group = GroupService.getInstance().get(groupId);
            if (group != null)
            {
                groupId = 1;
                group = GroupService.getInstance().get(groupId);
            }
        }

        // if there not exist a group do nothing
        if ( group != null )
        {
            // Find thingtype ZPL default and update it
            ThingType thingTypeZPL = null;
            try {
                thingTypeZPL = ThingTypeService.getInstance().getByCode("default_zpl_thingtype");
                if (thingTypeZPL != null) {
                    thingTypeZPL.setGroup(group);
                    ThingTypeService.getInstance().update(thingTypeZPL);
                }
            } catch (NonUniqueResultException e) {
                e.printStackTrace();
            }
        }
    }

    private static void modifyAssociateDisassociateResource(){
        // get old resources
        List<Resource> resourceAssignList = ResourceService.getResourceDAO().selectAllBy("name", "reportDefinition_assignThing");
        Resource resourceAssign = resourceAssignList != null && !resourceAssignList.isEmpty() ? resourceAssignList.get(0): null;
        List<Resource> resourceUnAssignList = ResourceService.getResourceDAO().selectAllBy("name","reportDefinition_unAssignThing");
        Resource resourceUnAssign = (resourceUnAssignList == null)? null:(!resourceUnAssignList.isEmpty() ? resourceUnAssignList.get(0):null);

        if (resourceAssign != null && resourceUnAssign != null) {
            // create new resource
            Resource rsAssignUnAssignThing = null;
            List<Resource> resourceAssignUnAssignList = ResourceService.getResourceDAO().selectAllBy("name","reportDefinition_assignUnAssignThing");
            if(resourceAssignUnAssignList == null){
                rsAssignUnAssignThing = asignResource(resourceAssign);
            } else if(resourceAssignUnAssignList.isEmpty()){
                rsAssignUnAssignThing = asignResource(resourceAssign);
            } else {
                rsAssignUnAssignThing = resourceAssignUnAssignList.get(0);
            }

            List<Role> roles = RoleService.getRoleDAO().selectAll();
            for (Role role : roles) {
                Set<RoleResource> rolResources = role.getRoleResources();
                String permissions = null;
                boolean createRoleResource = false;
                List<RoleResource> roleResourceToRemove = new ArrayList<RoleResource>();
                for (RoleResource rolResource : rolResources) {
                    Resource resource = rolResource.getResource();
                    if (resource.getId().compareTo(resourceAssign.getId()) == 0 || resource.getId().compareTo(resourceUnAssign.getId()) == 0) {
                        permissions = rolResource.getPermissions();
                        createRoleResource = true;
                        // remove old relation
                        roleResourceToRemove.add(rolResource);
                    }
                }
                // update roleResources
                if (createRoleResource){
                    RoleResource roleResourceNew = new RoleResource();
                    roleResourceNew.setPermissions(permissions);
                    roleResourceNew.setResource(rsAssignUnAssignThing);
                    roleResourceNew.setRole(role);
                    RoleResourceService.getInstance().insert(roleResourceNew);
                    rolResources.add(roleResourceNew);
                }
                if (!roleResourceToRemove.isEmpty()){
                    for (RoleResource rolResource : roleResourceToRemove){
                        rolResources.remove(rolResource);
                        RoleResourceService.getRoleResourceDAO().delete(rolResource);
                    }
                }
                role.setRoleResources(rolResources);
                RoleService.getRoleDAO().update(role);
            }
            // remove old resources
            ResourceService.getResourceDAO().delete(resourceAssign);
            ResourceService.getResourceDAO().delete(resourceUnAssign);
        }
    }

    private static Resource asignResource(Resource resourceAssign){
        Resource rsAssignUnAssignThing = new Resource();
        rsAssignUnAssignThing.setAcceptedAttributes("u");
        rsAssignUnAssignThing.setName("reportDefinition_assignUnAssignThing");
        rsAssignUnAssignThing.setDescription("Allow user to assign or un-assign a thing to other on report definition");
        rsAssignUnAssignThing.setFqname("com.tierconnect.riot.iot.entities.ReportDefinition");
        rsAssignUnAssignThing.setGroup(GroupService.getInstance().getRootGroup());
        rsAssignUnAssignThing.setLabel("Thing Associate & Disassociate");
        //rsAssignUnAssignThing.setModule("Reports");
        rsAssignUnAssignThing.setParent(resourceAssign.getParent());
        rsAssignUnAssignThing.setTreeLevel(3);
        rsAssignUnAssignThing.setType(4);
        ResourceService.getInstance().insert(rsAssignUnAssignThing);
        return rsAssignUnAssignThing;
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
