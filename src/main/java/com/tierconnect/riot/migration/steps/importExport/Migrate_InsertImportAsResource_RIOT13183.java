package com.tierconnect.riot.migration.steps.importExport;

import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QResource;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.entities.Role;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.iot.entities.ImportExport;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.HashSet;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_InsertImportAsResource_RIOT13183 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_InsertImportAsResource_RIOT13183.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        insertImportResource();
    }

    public static void insertImportResource(){
        Group rootGroup = GroupService.getInstance().getRootGroup();
        ResourceService resourceService = ResourceService.getInstance();
        ResourceDAO resourceDAO = resourceService.getResourceDAO();
        Role rootRole = RoleService.getInstance().getRootRole();
        Role tenantAdminRole = RoleService.getInstance().getTenantAdminRole();
        HashSet<Resource> resources = new HashSet<>();
        Resource moduleControl = resourceDAO.selectBy(QResource.resource.name.eq("Control"));
        resources.add(moduleControl);
        Resource insertImport = Resource.getClassResource(rootGroup,ImportExport.class,moduleControl);
        insertImport.setAcceptedAttributes("x");
        insertImport.setDescription("Class level resource for ImportExport ");
        Resource importResource = resourceService.insert(insertImport);
        resources.add(importResource);

        for (Resource resource : resources) {
            RoleResourceService.getInstance().insert(rootRole, resource, resource.getAcceptedAttributes());
            RoleResourceService.getInstance().insert(tenantAdminRole, resource, resource.getAcceptedAttributes());
            resourceService.update(resource);
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
