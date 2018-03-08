package com.tierconnect.riot.migration.steps.thingTypeTemplate;

import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.ThingTypeFieldTemplate;
import com.tierconnect.riot.iot.entities.ThingTypeTemplate;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.DataTypeService;
import com.tierconnect.riot.iot.services.ThingTypeFieldTemplateService;
import com.tierconnect.riot.iot.services.ThingTypeTemplateService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;

import static com.tierconnect.riot.commons.Constants.THING_TYPE_DATA_TYPE;

public class Migrate_UserSettingsRetailAppConfig_VIZIX7512 implements MigrationStep {

    private static Logger logger = Logger.getLogger(Migrate_UserSettingsRetailAppConfig_VIZIX7512.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    private void migrateFeature() {

        migrateUserSettingsThingType();
        updatePriorityConfig();
        updateRetailAppConfiguration();
        updateUserActivity();
    }

    private void migrateUserSettingsThingType() {
        try {
            ThingTypeTemplate userSettings = ThingTypeTemplateService.getInstance().getByCode("userSettings");

            if (userSettings != null) {
                removeUdfField("departmentGroup", userSettings);
                removeUdfField("departmentGroupName", userSettings);

                DataTypeService typeService = DataTypeService.getInstance();
                PopDBRequiredIOT.insertUdfField("level", "level", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, userSettings);
                PopDBRequiredIOT.insertUdfField("departmentLevelCode", "departmentLevelCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, userSettings);
            }

        } catch (NonUniqueResultException e) {
            throw new UserException("It is not possible to get a Thing Type template " + e.getMessage());
        }
    }

    private void updatePriorityConfig() {
        try {
            ThingTypeTemplate priorityConfig = ThingTypeTemplateService.getInstance().getByCode("priorityConfig");

            if (priorityConfig != null) {
                removeUdfField("icon", priorityConfig);

                DataTypeService typeService = DataTypeService.getInstance();
                PopDBRequiredIOT.insertUdfField("color", "color", typeService.get(ThingTypeField.Type.TYPE_COLOR),
                        THING_TYPE_DATA_TYPE, false, priorityConfig);
            }
        } catch (NonUniqueResultException e) {
            throw new UserException("It is not possible to get a Thing Type template " + e.getMessage());
        }
    }

    private void updateRetailAppConfiguration() {
        try {
            ThingTypeTemplate retailAppConfig = ThingTypeTemplateService.getInstance().getByCode("retailAppConfig");

            if (retailAppConfig != null) {
                //region Remove and Add UDFs
                DataTypeService typeService = DataTypeService.getInstance();
                removeUdfField("homeTotalsReport", retailAppConfig);
                PopDBRequiredIOT.insertUdfField("tileNotificationsReport", "tileNotificationsReport",
                        typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, retailAppConfig);

                PopDBRequiredIOT.insertUdfField("dressingRoomZonesReport", "dressingRoomZonesReport",
                        typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, retailAppConfig);

                removeUdfField("dressingRoomReportAllDetail", retailAppConfig);

                removeUdfField("dressingRoomReportDetail", retailAppConfig);
                PopDBRequiredIOT.insertUdfField("dressingRoomDetailReport", "dressingRoomDetailReport",
                        typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, retailAppConfig);

                removeUdfField("replenishGroupsReport", retailAppConfig);
                PopDBRequiredIOT.insertUdfField("departmentsGroupReports", "departmentsGroupReports",
                        typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, retailAppConfig);

                //endregion

                //region Add new UDFs
                PopDBRequiredIOT.insertUdfField("thingTypeCodeDepartment", "thingTypeCodeDepartment",
                        typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, retailAppConfig);

                PopDBRequiredIOT.insertUdfField("level", "level",
                        typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, retailAppConfig);

                PopDBRequiredIOT.insertUdfField("dressingRoomReportColumns", "dressingRoomReportColumns",
                        typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, retailAppConfig);

                PopDBRequiredIOT.insertUdfField("dressingRoomDetailReportColumns", "dressingRoomDetailReportColumns",
                        typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, retailAppConfig);

                PopDBRequiredIOT.insertUdfField("hotReplenishDetailReportColumns", "hotReplenishDetailReportColumns",
                        typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, retailAppConfig);

                PopDBRequiredIOT.insertUdfField("hotReplenishReportColumns", "hotReplenishReportColumns",
                        typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, retailAppConfig);

                PopDBRequiredIOT.insertUdfField("replenishDetailReportColumns", "replenishDetailReportColumns",
                        typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, retailAppConfig);

                PopDBRequiredIOT.insertUdfField("replenishReportColumns", "replenishReportColumns",
                        typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, retailAppConfig);

                PopDBRequiredIOT.insertUdfField("sellThruReplenishReportColumns", "sellThruReplenishReportColumns",
                        typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, retailAppConfig);
                //endregion
            }
        } catch (NonUniqueResultException e) {
            throw new UserException("It is not possible to get a Thing Type template " + e.getMessage());
        }
    }

    private void updateUserActivity() {
        try {
            ThingTypeTemplate userActivity = ThingTypeTemplateService.getInstance().getByCode("UserActivity");

            if (userActivity != null) {
                DataTypeService typeService = DataTypeService.getInstance();
                PopDBRequiredIOT.insertUdfField("itemDepartmentGroupName", "itemDepartmentGroupName",
                        typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, userActivity);
                PopDBRequiredIOT.insertUdfField("itemDepartmentSubGroupName", "itemDepartmentSubGroupName",
                        typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, userActivity);
            }
        } catch (NonUniqueResultException e) {
            throw new UserException("It is not possible to get a Thing Type template " + e.getMessage());
        }
    }

    private void removeUdfField(String udfName, ThingTypeTemplate thingTypeTemplate) {
        ThingTypeFieldTemplate thingTypeFieldTemplate = ThingTypeFieldTemplateService.getInstance().
                getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplate.getId(), udfName);
        if(thingTypeFieldTemplate != null) {
            ThingTypeFieldTemplateService.getInstance().delete(thingTypeFieldTemplate);
        }
    }
}
