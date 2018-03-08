package com.tierconnect.riot.migration.steps.resource;

import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.entities.ReportEntryOption;
import com.tierconnect.riot.iot.services.ReportDefinitionService;
import com.tierconnect.riot.iot.services.ReportEntryOptionService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_ReportResourceName_NOISSUE implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ReportResourceName_NOISSUE.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    private void migrateFeature() {
        ResourceService resourceService = ResourceService.getInstance();
        ResourceDAO resourceDAO = ResourceService.getResourceDAO();
        Resource resourceRD = resourceDAO.selectBy("name", "reportDefinition");
        resourceRD.setAcceptedAttributes("riudaxp");
        resourceService.update(resourceRD);

        Group rootGroup = GroupService.getInstance().getRootGroup();

        if (resourceDAO.selectBy("name", Resource.REPORT_INSTANCES_MODULE) == null) {
            Resource moduleReportInstances = ResourceService.getInstance().insert(Resource.getModuleResource(rootGroup, Resource.REPORT_INSTANCES_MODULE, "Report Instances"));
            List<Resource> resources = resourceDAO.selectAllBy(QResource.resource.type.eq(ResourceType.REPORT_DEFINITION.getId()));
            for (Resource resource0: resources) {
               // resource0.setModule(moduleReportInstances.getName());
                resource0.setParent(moduleReportInstances);
            }
        }

        RoleResourceService roleResourceService = RoleResourceService.getInstance();
        List<RoleResource> roleResources = roleResourceService.getRoleResourceDAO().selectAllBy(QRoleResource.roleResource.resource.eq(resourceRD));
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
        Resource reportDefinitionResource = resourceRD;

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
        //r.setModule(reportDefinitionResource.getModule());
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
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
