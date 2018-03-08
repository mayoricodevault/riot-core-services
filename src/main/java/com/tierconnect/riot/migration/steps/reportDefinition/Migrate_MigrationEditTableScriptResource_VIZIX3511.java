package com.tierconnect.riot.migration.steps.reportDefinition;

import com.tierconnect.riot.appcore.dao.RoleDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Set;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_MigrationEditTableScriptResource_VIZIX3511 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrationEditTableScriptResource_VIZIX3511.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateEditTableScriptResource();
    }

    private void migrateEditTableScriptResource() throws NonUniqueResultException {
        String reportDefinition_tableScriptEdition_label = "Allow edit reportDefinition on table script type ";
        String reportDefinition_tableScriptEdition_description = "Allow edit reportDefinition on table script type ";

        ResourceService resourceService = ResourceService.getInstance();
        Group rootGroup  = GroupService.getInstance().getRootGroup();
        Resource reportDefinitionResource = resourceService.getByName("reportDefinition");
        Resource tableScriptEdition = resourceService.insert(Resource.getPropertyResource(rootGroup,
                reportDefinitionResource,
                "editTableScript",
                reportDefinition_tableScriptEdition_label,
                reportDefinition_tableScriptEdition_description));
        tableScriptEdition.setLabel("Edit Table Script");
        tableScriptEdition.setAcceptedAttributes("u");

        Role rootRole = RoleService.getInstance().getRootRole();
        RoleResource rs = RoleService.getInstance().updateResource(rootRole, tableScriptEdition, "u");
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
