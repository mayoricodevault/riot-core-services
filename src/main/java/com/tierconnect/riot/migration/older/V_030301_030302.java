package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;

import java.util.*;

/**
 * Created by agutierrez on 10/30/15.
 */
@Deprecated
public class V_030301_030302 implements MigrationStepOld
{
    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(30301);
    }

    @Override
    public int getToVersion() {
        return 30302;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V030301_to_030302.sql");
    }

    @Override
    public void migrateHibernate() throws Exception {

        PopDBRequired.populateAttachmentConfig(GroupService.getInstance().get(1L));
    }

    public void migrateResources() {
        ResourceService resourceService = ResourceService.getInstance();
        ResourceDAO resourceDAO = ResourceService.getResourceDAO();
        Resource resource = resourceDAO.selectBy("name", "reportDefinition");
        resource.setAcceptedAttributes("riudaxp");
        resourceService.update(resource);
        RoleResourceService roleResourceService = RoleResourceService.getInstance();
        List<RoleResource> roleResources = roleResourceService.getRoleResourceDAO().selectAllBy(QRoleResource.roleResource.resource.eq(resource));
        for (RoleResource roleResource : roleResources) {
            if (roleResource.getPermissionsList().contains("r")) {
                Set set = new LinkedHashSet<>(roleResource.getPermissionsList());
                set.add("x");
                set.add("p");
                roleResource.setPermissions(set);
                roleResourceService.update(roleResource);
            }
        }
        ReportDefinitionService reportDefinitionService = ReportDefinitionService.getInstance();
        ReportEntryOptionService reos = ReportEntryOptionService.getInstance();
        List<ReportDefinition> reportDefinitions = reportDefinitionService.getReportDefinitionDAO().selectAll();
        for (ReportDefinition reportDefinition: reportDefinitions) {
            reportDefinitionService.createResource(reportDefinition);
            for (ReportEntryOption reportEntryOption : reportDefinition.getReportEntryOption()) {
                reos.createResource(reportEntryOption);
            }
        }

        Resource rdAssignThing = resourceDAO.selectBy("name", "reportDefinition_assignThing");
        Resource rdUnAssignThing = resourceDAO.selectBy("name", "reportDefinition_unAssignThing");
        Resource rdInlineEdit = resourceDAO.selectBy("name", "reportDefinition_inlineEdit");
        Resource reportDefinitionResource = resource;

        Group rootGroup = GroupService.getInstance().getRootGroup();

        Resource r = resourceDAO.selectBy("name", "reportDefinition_inlineEditGroup");
        boolean isNew = false;
        if (r == null) {
            isNew = true;
            r = new Resource();
        }
        r.setGroup(rootGroup);
        r.setFqname("Report Inline Edit");
        r.setName("reportDefinition_inlineEditGroup");
        r.setAcceptedAttributes("x");
        r.setLabel(""+r.getFqname());
        r.setDescription(r.getFqname());
        r.setTreeLevel(2);
        r.setParent(reportDefinitionResource.getParent());
        r.setType(ResourceType.MODULE.getId());
        if (isNew) {
            resourceService.insert(r);
        }

        rdAssignThing.setParent(r);
        rdUnAssignThing.setParent(r);
        rdInlineEdit.setParent(r);

    }

    @Override
    public void migrateSQLAfter() throws Exception {

    }

    private void migrateFields()
    {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        PopDBUtils.migrateFieldService("reloadAllThingsThreshold", "reloadAllThingsThreshold", "Things Cache Reload Threshold", rootGroup, "Import Configuration", "java.lang.Long", 3L, true, "1000");
        PopDBUtils.migrateFieldService("sendThingFieldTickle", "sendThingFieldTickle", "Run Rules After Import", rootGroup, "Import Configuration", "java.lang.Boolean", 3L, true, "true");
        PopDBUtils.migrateFieldService("fmcSapEnableSapSyncOnImport", "fmcSapEnableSapSyncOnImport", "Enable SAP Sync on import", rootGroup, "Import Configuration", "java.lang.Boolean", 2L, false,
                "false");
    }
}

