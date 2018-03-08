package com.tierconnect.riot.migration.steps.thingTypeTemplate;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.tierconnect.riot.commons.Constants.THING_TYPE_DATA_TYPE;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_UserActivity_VIZIX7514 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_UserActivity_VIZIX7514.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    private void migrateFeature() {
        Group group = GroupService.getInstance().getRootGroup();
        ThingTypeTemplateCategory mojixAppCategory = ThingTypeTemplateCategoryService.getInstance().getByCode("MOJIX_RETAIL_APP_SYNC");
        if (mojixAppCategory != null) {
            ThingTypeTemplate thingTypeTemplate = ThingTypeTemplateService.getInstance().insert(
                    new ThingTypeTemplate(5, "userActivity", "User Activity", "User Activity",
                            "icon-thingtype-replenishment", false, group, mojixAppCategory));
            List<ThingTypeFieldTemplate> results = createUserActivity(thingTypeTemplate);

            try {
                ThingTypeTemplate replenish = ThingTypeTemplateService.getInstance().getByCode("replenishment");

                ThingTypeTemplate hotReplenish = ThingTypeTemplateService.getInstance().getByCode("hotReplenishment");
                ThingTypeTemplate sellThru = ThingTypeTemplateService.getInstance().getByCode("sellThruReplenishment");
                ThingTypeTemplate dressRoom = ThingTypeTemplateService.getInstance().getByCode("dressingRoom");
                List<Long> values = new ArrayList<>();
                values.add(replenish != null ? replenish.getId() : 0);
                values.add(hotReplenish != null ? hotReplenish.getId() : 0);
                values.add(dressRoom != null ? dressRoom.getId() : 0);
                values.add(sellThru != null ? sellThru.getId() : 0);

                BooleanBuilder be = new BooleanBuilder();
                be = be.and(QThingTypeField.thingTypeField.thingTypeFieldTemplateId.in(values));
                List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().listPaginated(be, null, null);
                for (ThingTypeField thingTypeField : thingTypeFields) {
                    BooleanBuilder beAnd = new BooleanBuilder();
                    beAnd = beAnd.and(QThingTypeFieldTemplate.thingTypeFieldTemplate.thingTypeTemplate.code.eq("userActivity"));
                    beAnd = beAnd.and(QThingTypeFieldTemplate.thingTypeFieldTemplate.name.eq(thingTypeField.getName()));
                    ThingTypeFieldTemplate thingTypeFieldTemplate = ThingTypeFieldTemplateService.
                            getInstance().listPaginated(beAnd, null, null).get(0);

                    if (thingTypeFieldTemplate != null) {
                        thingTypeField.setThingTypeFieldTemplateId(thingTypeFieldTemplate.getId());
                    }
                    ThingTypeFieldService.getInstance().update(thingTypeField);
                }
                BooleanBuilder beTT = new BooleanBuilder();
                beTT = beTT.and(QThingType.thingType.thingTypeTemplate.id.in(values));
                List<ThingType> thingTypes = ThingTypeService.getInstance().listPaginated(beTT, null, null);
                for (ThingType thingType : thingTypes) {
                    thingType.setThingTypeTemplate(thingTypeTemplate);
                    ThingTypeService.getInstance().update(thingType);
                }

                if (replenish != null){
                    ThingTypeTemplateService.getInstance().delete(replenish);
                }
                if (sellThru != null){
                    ThingTypeTemplateService.getInstance().delete(sellThru);
                }
                if (hotReplenish != null){
                    ThingTypeTemplateService.getInstance().delete(hotReplenish);
                }
                if (dressRoom != null){
                    ThingTypeTemplateService.getInstance().delete(dressRoom);
                }
                ThingTypeTemplateCategory category = ThingTypeTemplateCategoryService.getInstance().getByCode("MOJIX_RETAIL_APP_CONFIG");
                BooleanBuilder beCat = new BooleanBuilder();
                beCat = beCat.and(QThingTypeTemplate.thingTypeTemplate.thingTypeTemplateCategory.eq(category));
                List<ThingTypeTemplate> thingTypeTemplates = ThingTypeTemplateService.getInstance().listPaginated(beCat, null, null);
                for (ThingTypeTemplate thingTypeTemp : thingTypeTemplates) {
                    thingTypeTemp.setThingTypeTemplateCategory(mojixAppCategory);
                    ThingTypeTemplateService.getInstance().update(thingTypeTemp);
                }
                ThingTypeTemplateCategoryService.getInstance().delete(category);
            } catch (NonUniqueResultException e) {
                throw new UserException("It is not possible to get a Thing Type template " + e.getMessage());
            }
        }else{
            logger.info("MOJIX_RETAIL_APP_SYNC thing type template category doesn't exist.");
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    private List<ThingTypeFieldTemplate > createUserActivity(ThingTypeTemplate thingTypeTemplate){
        DataTypeService typeService = DataTypeService.getInstance();

        List<ThingTypeFieldTemplate > ttfList = new ArrayList<>();
        ttfList.add(insertUdfField("itemUPCCategoryCode", "itemUPCCategoryCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("itemColor", "itemColor", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("itemPrice", "itemPrice", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("itemSize", "itemSize", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("itemStoreCode", "itemStoreCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("itemStoreName", "itemStoreName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("itemDepartmentSubGroup", "itemDepartmentSubGroup", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("itemUpc", "itemUpc", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("replenishBackCount", "replenishBackCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("replenishBackZone", "replenishBackZone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("itemMaterial", "itemMaterial", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("itemDepartmentCode", "itemDepartmentCode", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("itemDepartmentName", "itemDepartmentName", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("replenishFrontCount", "replenishFrontCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("replenishFrontZone", "replenishFrontZone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("itemBrand", "itemBrand", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("replenishLastDate", "replenishLastDate", typeService.get(ThingTypeField.Type.TYPE_DATE), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("replenishMax", "replenishMax", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("replenishMin", "replenishMin", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("replenishQuantity", "replenishQuantity", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("replenishStatus", "replenishStatus", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("replenishUser", "replenishUser", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("replenishQuantityDone", "replenishQuantityDone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("replenishBoxCount", "replenishBoxCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("replenishDressingCount", "replenishDressingCount", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("replenishDressingZone", "replenishDressingZone", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("itemDepartmentGroup", "itemDepartmentGroup", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("replenishRoom", "replenishRoom", typeService.get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        ttfList.add(insertUdfField("activityType", "activityType", typeService.get(ThingTypeField.Type.TYPE_TEXT), THING_TYPE_DATA_TYPE, false, thingTypeTemplate));
        return ttfList;
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
