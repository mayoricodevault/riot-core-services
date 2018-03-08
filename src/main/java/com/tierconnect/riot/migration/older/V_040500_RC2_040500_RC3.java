package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.ImportExport;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Created by rsejas on 11/24/16.
 */
@Deprecated
public class V_040500_RC2_040500_RC3 implements MigrationStepOld {
    static Logger logger = Logger.getLogger(V_040500_RC2_040500_RC3.class);

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(4050002);
    }

    @Override
    public int getToVersion() {
        return 4050003;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        logger.info("Migrating from: " + getFromVersions() + " To: " + getToVersion());
        DBHelper dbHelper = new DBHelper();
        String databaseType = DBHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V040500_RC2_to_040500_RC3.sql");
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateGroupConfiguration();
        insertImportResource();
    }

    @Override
    public void migrateSQLAfter() throws Exception {
    }

    public static void insertImportResource(){
        Group rootGroup       = GroupService.getInstance().getRootGroup();
        ResourceService resourceService = ResourceService.getInstance();
        ResourceDAO resourceDAO     = resourceService.getResourceDAO();
        Role rootRole        = RoleService.getInstance().getRootRole();
        Role              tenantAdminRole = RoleService.getInstance().getTenantAdminRole();
        HashSet<Resource> resources       = new HashSet<>();
        Resource moduleControl   = resourceDAO.selectBy(QResource.resource.name.eq("Control"));
        resources.add(moduleControl);
        Resource insertImport = Resource.getClassResource(rootGroup,
                ImportExport.class,
                moduleControl);
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
    public void migrateGroupConfiguration(){
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        Field field = PopDBUtils.popFieldService("thingListInTreeView", "thingListInTreeView", "Thing List In Tree View", rootGroup, "Look & Feel",
                "java.lang.Boolean", null, true);
        PopDBUtils.popGroupField(rootGroup, field, "false");
    }

}
