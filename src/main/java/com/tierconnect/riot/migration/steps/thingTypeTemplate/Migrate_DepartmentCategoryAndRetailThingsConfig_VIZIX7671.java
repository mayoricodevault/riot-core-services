package com.tierconnect.riot.migration.steps.thingTypeTemplate;


import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;

import java.util.List;

import org.apache.log4j.Logger;

import static com.tierconnect.riot.commons.Constants.THING_TYPE_DATA_TYPE;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_DepartmentCategoryAndRetailThingsConfig_VIZIX7671 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_DepartmentCategoryAndRetailThingsConfig_VIZIX7671.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    private void migrateFeature() {
        //Group group = GroupService.getInstance().getRootGroup();

        deleteThingTypeTemplate("deptCategory");
        changeDepartment();
        deleteThingTypeTemplate("retailThingsConfig");
        changeItem();

    }

    private void deleteThingTypeTemplate(String code) {
        try {
            ThingTypeTemplate retailThingsConfig = ThingTypeTemplateService.getInstance().getByCode(code);
            if(retailThingsConfig != null){
                List<ThingTypeFieldTemplate> thingTypefields = ThingTypeFieldTemplateService.getInstance().getThingTypeFielTemplatedByThingTypeTemplateId(retailThingsConfig.getId());

                for (ThingTypeFieldTemplate ttf:thingTypefields) {
                    removeUdfField(ttf.getName(), retailThingsConfig);
                }
                ThingTypeTemplateService.getInstance().delete(retailThingsConfig);
            }
        } catch (NonUniqueResultException e) {
            throw new UserException("It is not possible to get a Thing Type template "+e.getMessage());
        }
    }

    private void changeDepartment() {
        try {
            ThingTypeTemplate department = ThingTypeTemplateService.getInstance().getByCode("dept");
            if(department != null){
                List<ThingTypeFieldTemplate> thingTypefields = ThingTypeFieldTemplateService.getInstance().getThingTypeFielTemplatedByThingTypeTemplateId(department.getId());

                for (ThingTypeFieldTemplate ttf:thingTypefields) {
                    removeUdfField(ttf.getName(), department);
                }

                DataTypeService typeService = DataTypeService.getInstance();

                PopDBRequiredIOT.insertUdfField("icon", "icon", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, department);
                PopDBRequiredIOT.insertUdfField("departmentGroup", "departmentGroup", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, department);
                PopDBRequiredIOT.insertUdfField("departmentGroupIcon", "departmentGroupIcon", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, department);
                PopDBRequiredIOT.insertUdfField("departmentGroupName", "departmentGroupName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, department);
                PopDBRequiredIOT.insertUdfField("departmentSubGroup", "departmentSubGroup", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, department);
                PopDBRequiredIOT.insertUdfField("departmentSubGroupIcon", "departmentSubGroupIcon", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, department);
                PopDBRequiredIOT.insertUdfField("departmentSubGroupName", "departmentSubGroupName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, department);

            }

        } catch (NonUniqueResultException e) {
            throw new UserException("It is not possible to get a Thing Type template "+e.getMessage());
        }
    }

    private void changeItem() {
        try {
            ThingTypeTemplate item = ThingTypeTemplateService.getInstance().getByCode("item");
            if(item != null){
                List<ThingTypeFieldTemplate> thingTypefields = ThingTypeFieldTemplateService.getInstance().getThingTypeFielTemplatedByThingTypeTemplateId(item.getId());

                for (ThingTypeFieldTemplate ttf:thingTypefields) {
                    if(ttf.getName().equals("deptCategoryCode")) {
                        removeUdfField(ttf.getName(), item);
                    }
                }

                DataTypeService typeService = DataTypeService.getInstance();

                PopDBRequiredIOT.insertUdfField("deptCode", "color", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, item);
            }

        } catch (NonUniqueResultException e) {
            throw new UserException("It is not possible to get a Thing Type template "+e.getMessage());
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    private void removeUdfField(String udfName, ThingTypeTemplate thingTypeTemplate) {
        ThingTypeFieldTemplateService thingTypeFieldTemplateService = ThingTypeFieldTemplateService.getInstance();

        ThingTypeFieldTemplate thingTypeFieldTemplate = null;
        thingTypeFieldTemplate = thingTypeFieldTemplateService.getThingTypeFieldTemplateByThingTypeTemplate(
                thingTypeTemplate.getId(), udfName);
        if(thingTypeFieldTemplate != null) {

            if (thingTypeFieldTemplate != null) {
                thingTypeFieldTemplateService.delete(thingTypeFieldTemplate);
            }
        }
    }

}
