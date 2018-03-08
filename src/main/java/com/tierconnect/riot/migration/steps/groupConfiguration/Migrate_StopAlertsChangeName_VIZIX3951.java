package com.tierconnect.riot.migration.steps.groupConfiguration;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupField;
import com.tierconnect.riot.appcore.entities.QGroupField;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.FieldService;
import com.tierconnect.riot.appcore.services.GroupFieldService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_StopAlertsChangeName_VIZIX3951 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_StopAlertsChangeName_VIZIX3951.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateName();
    }

    private void migrateName() {
        String value = "false";
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        Field field = FieldService.getInstance().selectByName("stopAlerts");
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QGroupField.groupField.field.eq(field));
        List<GroupField> groupFields = GroupFieldService.getInstance().listPaginated(be, null,null);
        PopDBUtils.popFieldService("stopAlerts", "alert", "Alert", rootGroup, "Alerting & Notification",
                "java.lang.Boolean", null, true);
        Field f = FieldService.getInstance().selectByName("alert");

        if (!groupFields.isEmpty()){
            for (GroupField groupField: groupFields) {
                value = groupField.getValue().equals("false") ? "true" : "false";
                PopDBUtils.popGroupField(groupField.getGroup(), f, value);
            }
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
