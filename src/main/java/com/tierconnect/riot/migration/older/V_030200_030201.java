package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.migration.DBHelper;

import java.util.Arrays;
import java.util.List;

/**
 * Created by rchirinos on 26/10/2015.
 */
@Deprecated
public class V_030200_030201 implements MigrationStepOld
{
	@Override
	public List<Integer> getFromVersions() {
		return Arrays.asList( 30200 );
	}

	@Override
	public int getToVersion() {
		return 30201;
	}

	@Override
	public void migrateSQLBefore() throws Exception {
		DBHelper dbHelper = new DBHelper();
		String databaseType = dbHelper.getDataBaseType();
		dbHelper.executeSQLFile("sql/" + databaseType + "/V030200_to_030201.sql");
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
	public void migrateSQLAfter() throws Exception {

	}
}
