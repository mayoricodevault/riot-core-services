package com.tierconnect.riot.migration.steps.reportDefinition;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupField;
import com.tierconnect.riot.appcore.services.FieldService;
import com.tierconnect.riot.appcore.services.GroupFieldService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_AddEmailConfigurationError_VIZIX1890 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_AddEmailConfigurationError_VIZIX1890.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    private void migrateFeature(){
        Group group = GroupService.getInstance().getRootGroup();
        Field fieldEmailConfigurationError = FieldService.getInstance().selectByGroupAndName(group,
                Constants.EMAIL_CONFIGURATION_ERROR);
        if (fieldEmailConfigurationError == null){
            Field field = new Field();
            field.setDescription("Email Configuration Error");
            field.setEditLevel(3L);
            field.setModule("SMTP Email Configuration");
            field.setName("emailConfigurationError");
            field.setType("java.lang.String");
            field.setUserEditable(false);
            field.setGroup(group);
            FieldService.getInstance().insert(field);
            GroupField groupFieldEmailConfiguration = GroupFieldService.getInstance().selectByGroupField(group, field);
            if (groupFieldEmailConfiguration == null) {
                GroupField groupField = new GroupField();
                groupField.setValue(Constants.EMAIL_RIOT_TEST);
                groupField.setGroup(group);
                groupField.setField(field);
                GroupFieldService.getInstance().insert(groupField);
            }
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
    }

}
