package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.migration.DBHelper;

import java.util.Arrays;
import java.util.List;

/**
 * Created by agutierrez on 9/23/15.
 */
@Deprecated
public class V_030100_030101 implements MigrationStepOld {
    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(30100);
    }

    @Override
    public int getToVersion() {
        return 30101;
    }


    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V030100_to_030101.sql");
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateResources();
        migrateFields();
    }

    @Override
    public void migrateSQLAfter() throws Exception {

    }

    private void migrateFields() {
        GroupService.getInstance().refreshHierarchyName();
    }

    private void migrateResources() {
        ResourceDAO resourceDAO = ResourceService.getResourceDAO();
        Resource moduleMonitor = resourceDAO.selectBy(QResource.resource.name.eq("Monitor"));
        if (moduleMonitor == null) {
            Group rootGroup = GroupService.getInstance().getRootGroup();
            Resource re2 = ResourceService.getInstance().insert(Resource.getModuleResource(rootGroup, "Monitor", "Monitor"));
            Role rootRole = RoleService.getInstance().getRootRole();
            RoleResourceService.getInstance().insert(rootRole, re2, re2.getAcceptedAttributes());
        }
    }
}
