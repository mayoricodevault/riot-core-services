package com.tierconnect.riot.migration.steps.thingTypeTemplate;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

import com.tierconnect.riot.commons.Constants;

import java.util.List;

import static com.tierconnect.riot.commons.Constants.*;

public class Migrate_MojixRetailAndCustomThingTypeTemplate_VIZIX6615 implements MigrationStep {

    private static Logger logger = Logger.getLogger(Migrate_MojixRetailAndCustomThingTypeTemplate_VIZIX6615.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateThingTypeTemplate();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    private void migrateThingTypeTemplate() {

        migrateThingTypeTemplate("REPLENISHMENT");
        migrateThingTypeTemplate("HOT_REPLENISHMENT");
        migrateThingTypeTemplate("DRESSING_ROOM");
        migrateThingTypeTemplateCategory();

        migrateThingTypeTemplateBase();

        setThingTypeTemplateCustom();
        setThingTypeTemplateCategoryCustom();
    }

    private void migrateThingTypeTemplateBase() {

        ThingTypeTemplateCategoryService categoryService = ThingTypeTemplateCategoryServiceBase.getInstance();
        categoryService.insert(new ThingTypeTemplateCategory(CATEGORY_THING_TYPE_TEMPLATE_MOJIX_RETAIL_APP_BASE, "RETAIL THINGS", 4, "icon-thingtype-retail-app-things"));

        Group group = GroupService.getInstance().getRootGroup();
        PopDBRequiredIOT.populateMojixRetailAppThingTypeTemplateBase(group);
    }

    private void migrateThingTypeTemplateCategory() {
        ThingTypeTemplateCategory retailTemplateCategory = ThingTypeTemplateCategoryService.getInstance().getByCode("MOJIX_RETAIL_APP");
        if(retailTemplateCategory != null) {
            ThingTypeTemplateCategoryService.getInstance().delete(retailTemplateCategory);
        }
        retailTemplateCategory = ThingTypeTemplateCategoryService.getInstance().getByCode("MOJIX_RETAIL_APP_BASE");
        if(retailTemplateCategory != null) {
            ThingTypeTemplateCategoryService.getInstance().delete(retailTemplateCategory);
        }
    }

    private void setThingTypeTemplateCustom() {
        ThingTypeTemplateCategory customCategory = ThingTypeTemplateCategoryService.getInstance().getByCode("CUSTOM");

        ThingTypeTemplate template = null;
        try {
            template = ThingTypeTemplateService.getInstance().getByCode("Custom");
        } catch (NonUniqueResultException e) {
            e.printStackTrace();
        }
        if (template == null) {
            throw new RuntimeException("Not exist thing type template with name : " + "Custom");
        }
        template.setCode(template.getName());
        template.setName("Custom Thing Type");
        template.setPathIcon("icon-thingtype-custom");
        template.setDisplayOrder(1);
        template.setThingTypeTemplateCategory(customCategory);
        ThingTypeTemplateService.getInstance().update(template);
    }


    private void setThingTypeTemplateCategoryCustom() {
        ThingTypeTemplateCategory categoryService = ThingTypeTemplateCategoryServiceBase.getInstance().getByCode("CUSTOM");

        if (categoryService == null) {
            throw new RuntimeException("Not exist thing type template category with code : " + "CUSTOM");
        }
        categoryService.setCode("CUSTOM");
        categoryService.setName("CUSTOM");
        categoryService.setDisplayOrder(8);
        categoryService.setPathIcon("icon-thingtype-custom-custom");

        ThingTypeTemplateCategoryService.getInstance().update(categoryService);
    }

    private void migrateThingTypeTemplate(String code){
        ThingTypeTemplateService thingTypeTemplateService = ThingTypeTemplateService.getInstance();
        ThingTypeTemplate thingTypeTemplate = null;
        try {
            thingTypeTemplate = thingTypeTemplateService.getByCode(code);
        } catch (NonUniqueResultException e) {
            e.printStackTrace();
        }

        if (thingTypeTemplate != null) {

            List<ThingTypeFieldTemplate> thingTypefields = ThingTypeFieldTemplateService.getInstance().getThingTypeFielTemplatedByThingTypeTemplateId(thingTypeTemplate.getId());

            for (ThingTypeFieldTemplate ttf:thingTypefields) {
                removeUdfField(ttf.getName(), thingTypeTemplate);
            }

            List<ThingType> thingTypes = ThingTypeService.getInstance().getThingTypesByThingTypeTemplate(thingTypeTemplate.getId());

            ThingTypeTemplate thingTypeTemplateCustom = null;
            try {
                thingTypeTemplateCustom = thingTypeTemplateService.getByCode("Custom");

                for (ThingType tt:thingTypes) {
                    tt.setThingTypeTemplate(thingTypeTemplateCustom);
                    ThingTypeService.getInstance().update(tt);
                }

            } catch (NonUniqueResultException e) {
                e.printStackTrace();
            }

            thingTypeTemplateService.delete(thingTypeTemplate);
        }

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
