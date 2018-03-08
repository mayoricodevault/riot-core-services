package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.migration.DBHelper;

import java.util.Arrays;
import java.util.List;

/**
 * Created by agutierrez on 8/20/15.
 */
@Deprecated
public class V_030000_030100 implements MigrationStepOld {
    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(30000);
    }

    @Override
    public int getToVersion() {
        return 30100;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V030000_to_030100.sql");
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateResources();
        migrateFields();
        migrateConnectionType();
    }

    private void migrateConnectionType() {
        Group root = GroupService.getInstance().getRootGroup();
        PopDBRequired.populateConnectionTypes(root);
    }

    private void migrateFields() {
        GroupService.getInstance().refreshHierarchyName();
    }

    private void migrateResources() {
        ResourceDAO resourceDAO = ResourceService.getResourceDAO();
        Resource moduleControl = resourceDAO.selectBy(QResource.resource.name.eq("Control"));
        Group rootGroup = GroupService.getInstance().getRootGroup();
        Resource re = ResourceService.getInstance().insert(Resource.getClassResource(rootGroup, Connection.class, moduleControl));
        Resource re2 = ResourceService.getInstance().insert( Resource.getClassResource( rootGroup, ConnectionType.class, moduleControl ));
        Role rootRole = RoleService.getInstance().getRootRole();
        RoleResourceService.getInstance().insert(rootRole, re, re.getAcceptedAttributes());
        RoleResourceService.getInstance().insert(rootRole, re2, re2.getAcceptedAttributes());
    }

    @Override
    public void migrateSQLAfter() throws Exception {

    }
}
