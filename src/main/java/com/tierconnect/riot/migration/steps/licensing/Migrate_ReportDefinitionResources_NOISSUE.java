package com.tierconnect.riot.migration.steps.licensing;

import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_ReportDefinitionResources_NOISSUE implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ReportDefinitionResources_NOISSUE.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateResources();
    }

    private void migrateResources() {
        Resource resource;
        ResourceService resourceService = ResourceService.getInstance();
        ResourceDAO resourceDAO = ResourceService.getResourceDAO();
        resource = resourceDAO.selectBy("name", "thing_editOwn");
        resource.setLabel(PopDBRequiredIOT.thing_editOwn_label);
        resource.setDescription(PopDBRequiredIOT.thing_editOwn_description);
        resourceService.update(resource);
        resource = resourceDAO.selectBy("name", "reportDefinition_emailRecipients");
        resource.setLabel(PopDBRequiredIOT.reportDefinition_emailRecipients_label);
        resource.setDescription(PopDBRequiredIOT.reportDefinition_emailRecipients_description);
        resourceService.update(resource);
        resource = resourceDAO.selectBy("name", "reportDefinition_emailRecipients");
        resource.setLabel(PopDBRequiredIOT.reportDefinition_emailRecipients_label);
        resource.setDescription(PopDBRequiredIOT.reportDefinition_emailRecipients_description);
        resourceService.update(resource);
        resource = resourceDAO.selectBy("name", "reportDefinition_assignThing");
        resource.setLabel(PopDBRequiredIOT.reportDefinition_assignThing_label);
        resource.setDescription(PopDBRequiredIOT.reportDefinition_assignThing_description);
        resourceService.update(resource);
        resource = resourceDAO.selectBy("name", "reportDefinition_unAssignThing");
        resource.setLabel(PopDBRequiredIOT.reportDefinition_unAssignThing_label);
        resource.setDescription(PopDBRequiredIOT.reportDefinition_unAssignThing_description);
        resourceService.update(resource);
        resource = resourceDAO.selectBy("name", "reportDefinition_inlineEdit");
        resource.setLabel(PopDBRequiredIOT.reportDefinition_inlineEdit_label);
        resource.setDescription(PopDBRequiredIOT.reportDefinition_inlineEdit_description);
        resourceService.update(resource);
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
