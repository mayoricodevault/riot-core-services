package com.tierconnect.riot.migration.steps.thingTypeTemplate;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import static com.tierconnect.riot.commons.Constants.*;

/**
 * Created by vealaro on 5/5/17.
 */
public class Migrations_ThingTypeTemplate_VIZIX4077 implements MigrationStep {

    private static Logger logger = Logger.getLogger(Migrations_ThingTypeTemplate_VIZIX4077.class);

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
        // insert category
        ThingTypeTemplateCategoryService categoryService = ThingTypeTemplateCategoryServiceBase.getInstance();
//        ThingTypeTemplateCategory customCategory = categoryService.insert(new ThingTypeTemplateCategory(CATEGORY_THING_TYPE_TEMPLATE_CUSTOM, "Custom", 1, ""));
        ThingTypeTemplateCategory customCategory = ThingTypeTemplateCategoryService.getInstance().getByCode("CUSTOM");
        ThingTypeTemplateCategory sensorsCategory = categoryService.insert(new ThingTypeTemplateCategory(CATEGORY_THING_TYPE_TEMPLATE_SENSORS, "SENSORS", 2, "icon-thingtype-sensors"));
        ThingTypeTemplateCategory m2MCategory = categoryService.insert(new ThingTypeTemplateCategory(CATEGORY_THING_TYPE_TEMPLATE_M2M, "M2M", 3, "icon-thingtype-m2m"));
        ThingTypeTemplateCategory mojixAppCategory = categoryService.insert(new ThingTypeTemplateCategory(CATEGORY_THING_TYPE_TEMPLATE_MOJIX_RETAIL_APP_BASE, "MOJIX RETAIL APP", 4, "icon-thingtype-retail-app"));

        // Custom Category
        setThingTypeTemplate(customCategory, "Custom", 1, "icon-thingtype-custom");

        // Sensors Category
        setThingTypeTemplate(sensorsCategory, "RFID Tag", 1, "icon-thingtype-rfid-tag");
        setThingTypeTemplate(sensorsCategory, "STARflex", "Starflex tag", 2, "icon-thingtype-flextag"); // change name
        setThingTypeTemplate(sensorsCategory, "STARflex Config", 3, "icon-thingtype-star");
        setThingTypeTemplate(sensorsCategory, "STARflex Status", "Starflex H&S", 4, "icon-thingtype-antenna-status"); // change name
        setThingTypeTemplate(sensorsCategory, "GPS Tag", 5, "icon-thingtype-gps-tag");
        setThingTypeTemplate(sensorsCategory, "Logical Reader", 6, "icon-thingtype-logical-reader");

        // Sensors M2M
        setThingTypeTemplate(m2MCategory, "CoreBridge", 1, "icon-thingtype-corebridge");
        setThingTypeTemplate(m2MCategory, "EdgeBridge", 2, "icon-thingtype-edgebridge");
        setThingTypeTemplate(m2MCategory, "RFID Printer", 3, "icon-thingtype-rfid-printer");
        setThingTypeTemplate(m2MCategory, "ZPL", 4, "icon-thingtype-zpl");

        Group group = GroupService.getInstance().getRootGroup();
        // Add new thing type template
        populateMojixRetailAppThingTypeTemplate(group, mojixAppCategory);
    }

    private void setThingTypeTemplate(ThingTypeTemplateCategory category, String name, Integer displayOrder, String icon) {
        setThingTypeTemplate(category, name, name, displayOrder, icon);
    }

    private void setThingTypeTemplate(ThingTypeTemplateCategory category, String name, String newName, Integer displayOrder, String icon) {
        ThingTypeTemplate template = ThingTypeTemplateService.getInstance().getByNameUnique(name);
        if (template == null) {
            throw new RuntimeException("Not exist thing type template with name : " + name);
        }
        template.setCode(template.getName());
        template.setName(newName);
        template.setPathIcon(icon);
        template.setDisplayOrder(displayOrder);
        template.setThingTypeTemplateCategory(category);
        ThingTypeTemplateService.getInstance().update(template);
    }

    private void populateMojixRetailAppThingTypeTemplate(Group group, ThingTypeTemplateCategory category) {

        DataTypeService typeService = DataTypeService.getInstance();
        //
        ThingTypeTemplate thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(
                new ThingTypeTemplate(1, "REPLENISHMENT", "Replenishment", "Replenishment",
                        "icon-thingtype-replenishment", false, group, category));

        insertUdfField("itemCategory", "itemCategory", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemColor", "itemColor", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemPrice", "itemPrice", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemSize", "itemSize", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemStoreCode", "itemStoreCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemStoreName", "itemStoreName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemSubCategory", "itemSubCategory", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemUpc", "itemUpc", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishBackCount", "replenishBackCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishBackZone", "replenishBackZone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishBackZoneId", "replenishBackZoneId", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDepartmentCode", "replenishDepartmentCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDepartmentName", "replenishDepartmentName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishFrontCount", "replenishFrontCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishFrontZone", "replenishFrontZone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishFrontZoneId", "replenishFrontZoneId", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishLastDate", "replenishLastDate", typeService.get(ThingTypeField.Type.TYPE_DATE), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishMax", "replenishMax", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishMin", "replenishMin", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishQuantity", "replenishQuantity", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishStatus", "replenishStatus", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishUser", "replenishUser", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishQuantityDone", "replenishQuantityDone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);

        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(
                new ThingTypeTemplate(2, "HOT_REPLENISHMENT", "Hot Replenishment", "Hot Replenishment",
                        "icon-thingtype-hot-replenishment", false, group, category));

        insertUdfField("itemCategory", "itemCategory", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemColor", "itemColor", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemPrice", "itemPrice", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemSize", "itemSize", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemStoreCode", "itemStoreCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemStoreName", "itemStoreName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemSubCategory", "itemSubCategory", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemUpc", "itemUpc", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishBackCount", "replenishBackCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishBackZone", "replenishBackZone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishBackZoneId", "replenishBackZoneId", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishBoxCount", "replenishBoxCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDepartmentCode", "replenishDepartmentCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDepartmentName", "replenishDepartmentName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishFrontCount", "replenishFrontCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishFrontZone", "replenishFrontZone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishFrontZoneId", "replenishFrontZoneId", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishLastDate", "replenishLastDate", typeService.get(ThingTypeField.Type.TYPE_DATE), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishMax", "replenishMax", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishMin", "replenishMin", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishQuantity", "replenishQuantity", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishStatus", "replenishStatus", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishUser", "replenishUser", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishQuantityDone", "replenishQuantityDone", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);

        thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(
                new ThingTypeTemplate(3, "DRESSING_ROOM", "Dressing Room Clean Up", "Dressing Room Clean Up",
                        "icon-thingtype-dressingroom-cleanup", false, group, category));

        insertUdfField("itemCategory", "itemCategory", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemColor", "itemColor", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemPrice", "itemPrice", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemSize", "itemSize", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemStoreCode", "itemStoreCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemStoreName", "itemStoreName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemSubCategory", "itemSubCategory", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemUpc", "itemUpc", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDressingCount", "replenishDressingCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDressingZone", "replenishDressingZone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDepartmentCode", "replenishDepartmentCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishDepartmentName", "replenishDepartmentName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishFrontCount", "replenishFrontCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishFrontZone", "replenishFrontZone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishBackZoneId", "replenishBackZoneId", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishLastDate", "replenishLastDate", typeService.get(ThingTypeField.Type.TYPE_DATE), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishQuantity", "replenishQuantity", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishStatus", "replenishStatus", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishUser", "replenishUser", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("itemHexadecimal", "itemHexadecimal", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishMin", "replenishMin", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        insertUdfField("replenishMax", "replenishMax", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
    }

    private ThingTypeFieldTemplate insertUdfField(String udfName, String udfDescription, DataType udfType,
                                                  String udfTypeParent, boolean udfTimeSeries, ThingTypeTemplate thingTypeTemplate) {
        ThingTypeFieldTemplate field = new ThingTypeFieldTemplate();
        field.setName(udfName);
        field.setDescription(udfDescription);
        field.setUnit("");
        field.setSymbol("");
        field.setType(udfType);
        field.setTypeParent(udfTypeParent);
        field.setTimeSeries(udfTimeSeries);
        field.setThingTypeTemplate(thingTypeTemplate);
        field.setDefaultValue("");
        return ThingTypeFieldTemplateService.getInstance().insert(field);
    }
}
