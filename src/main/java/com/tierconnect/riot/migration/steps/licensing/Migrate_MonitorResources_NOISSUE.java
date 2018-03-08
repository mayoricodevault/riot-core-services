package com.tierconnect.riot.migration.steps.licensing;

import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_MonitorResources_NOISSUE implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MonitorResources_NOISSUE.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateResources();
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


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
