package com.tierconnect.riot.migration.steps.thingTypeTemplate;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.ThingTypeTemplate;
import com.tierconnect.riot.iot.entities.ThingTypeTemplateCategory;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.DataTypeService;
import com.tierconnect.riot.iot.services.ThingTypeTemplateCategoryService;
import com.tierconnect.riot.iot.services.ThingTypeTemplateService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;

import static com.tierconnect.riot.commons.Constants.CATEGORY_THING_TYPE_TEMPLATE_MOJIX_RETAIL_APP_BASE;
import static com.tierconnect.riot.commons.Constants.THING_TYPE_DATA_TYPE;

public class Migrate_SkuCategorySkuAndItemThingType_VIZIX7585 implements MigrationStep {

    private static Logger logger = Logger.getLogger(Migrate_SkuCategorySkuAndItemThingType_VIZIX7585.class);

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
        addSkuCategoryThingType();
        addSkuThingType();
        updateItemThingType();
        updateOrderDeptThingType();
    }

    private void addSkuThingType() {
        Group group = GroupService.getInstance().getRootGroup();
        ThingTypeTemplateCategory category = ThingTypeTemplateCategoryService.getInstance().getByCode(CATEGORY_THING_TYPE_TEMPLATE_MOJIX_RETAIL_APP_BASE);
        DataTypeService typeService = DataTypeService.getInstance();

        ThingTypeTemplate thingTypeTemplate;

        thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("sku");
        thingTypeTemplate.setName("SKU");
        thingTypeTemplate.setDescription("SKU");
        thingTypeTemplate.setDisplayOrder(3);
        thingTypeTemplate.setThingTypeTemplateCategory(category);
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-sku");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        PopDBRequiredIOT.insertUdfField("price", "price", "dollar", "$", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate, "");
        PopDBRequiredIOT.insertUdfField("size", "size", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        PopDBRequiredIOT.insertUdfField("min", "min", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        PopDBRequiredIOT.insertUdfField("max", "max", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        PopDBRequiredIOT.insertUdfField("brand", "brand", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        PopDBRequiredIOT.insertUdfField("material", "material", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        PopDBRequiredIOT.insertUdfField("color", "color", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        PopDBRequiredIOT.insertUdfField("colorHexadecimal", "colorHexadecimal", typeService.get(ThingTypeField.Type.TYPE_COLOR), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        PopDBRequiredIOT.insertUdfField("skuCategoryCode", "skuCategoryCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
    }

    private void addSkuCategoryThingType() {
        Group group = GroupService.getInstance().getRootGroup();
        ThingTypeTemplateCategory category = ThingTypeTemplateCategoryService.getInstance().getByCode(CATEGORY_THING_TYPE_TEMPLATE_MOJIX_RETAIL_APP_BASE);
        DataTypeService typeService = DataTypeService.getInstance();

        ThingTypeTemplate thingTypeTemplate;

        thingTypeTemplate = new ThingTypeTemplate();
        thingTypeTemplate.setCode("skuCategory");
        thingTypeTemplate.setName("SKU Category");
        thingTypeTemplate.setDescription("SKU Category");
        thingTypeTemplate.setDisplayOrder(5);
        thingTypeTemplate.setThingTypeTemplateCategory(category);
        thingTypeTemplate.setGroup(group);
        thingTypeTemplate.setPathIcon("icon-thingtype-sku-category");
        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(thingTypeTemplate);

        PopDBRequiredIOT.insertUdfField("frontImage", "frontImage", typeService.get(ThingTypeField.Type.TYPE_IMAGE_ID), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        PopDBRequiredIOT.insertUdfField("backImage", "backImage", typeService.get(ThingTypeField.Type.TYPE_IMAGE_ID), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        PopDBRequiredIOT.insertUdfField("sideImage", "sideImage", typeService.get(ThingTypeField.Type.TYPE_IMAGE_ID), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
    }

    private void updateItemThingType() {
        try {
            ThingTypeTemplate item = ThingTypeTemplateService.getInstance().getByCode("item");
            if (item != null) {

                DataTypeService typeService = DataTypeService.getInstance();
                PopDBRequiredIOT.insertUdfField("skuCode", "skuCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, item);
            }

        } catch (NonUniqueResultException e) {
            throw new UserException("It is not possible to get a Thing Type template " + e.getMessage());
        }
    }

    private void updateOrderDeptThingType() {
        try {
            ThingTypeTemplate dept = ThingTypeTemplateService.getInstance().getByCode("dept");
            if (dept != null) {
                dept.setDisplayOrder(6);
                ThingTypeTemplateService.getInstance().update(dept);
            }

        } catch (NonUniqueResultException e) {
            throw new UserException("It is not possible to get a Thing Type template " + e.getMessage());
        }
    }
}
