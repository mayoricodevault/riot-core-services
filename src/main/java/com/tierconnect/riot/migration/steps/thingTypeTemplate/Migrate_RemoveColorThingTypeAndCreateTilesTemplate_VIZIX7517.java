package com.tierconnect.riot.migration.steps.thingTypeTemplate;

import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

import static com.tierconnect.riot.commons.Constants.*;

public class Migrate_RemoveColorThingTypeAndCreateTilesTemplate_VIZIX7517 implements MigrationStep {

    private static Logger logger = Logger.getLogger(Migrate_RemoveColorThingTypeAndCreateTilesTemplate_VIZIX7517.class);

    DataTypeService typeService = DataTypeService.getInstance();

    ThingTypeFieldTemplateService thingTypeFieldTemplateService = ThingTypeFieldTemplateService.getInstance();

    ThingTypeTemplateCategoryService thingTypeTemplateCategoryService = ThingTypeTemplateCategoryService.getInstance();

    ThingTypeTemplateService thingTypeTemplateService = ThingTypeTemplateService.getInstance();

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateColorThingType();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    private void migrateColorThingType() throws NonUniqueResultException {
        removeThingTypeTemplate("color");
        changeUPCThingColorFields();
        migrateTilesThingTypeConfigs();
    }

    private void removeThingTypeTemplate(String code) throws NonUniqueResultException {
        try {
            ThingTypeTemplate thingTypeTemplate = thingTypeTemplateService.getByCode(code);
            if (thingTypeTemplate != null) {
                thingTypeFieldTemplateService.getThingTypeFieldTemplateBy(thingTypeTemplate).forEach(ttft -> {
                    removeUdfField(ttft.getName(), thingTypeTemplate);
                });
                thingTypeTemplateService.delete(thingTypeTemplate);
            }
        } catch (NonUniqueResultException e) {
            throw e;
        }
    }

    private void removeUdfField(String udfName, ThingTypeTemplate thingTypeTemplate) {
        ThingTypeFieldTemplate thingTypeFieldTemplate = thingTypeFieldTemplateService.
                getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplate.getId(), udfName);
        if(thingTypeFieldTemplate != null) {
            thingTypeFieldTemplateService.delete(thingTypeFieldTemplate);
        }
    }

    private void changeUPCThingColorFields() throws NonUniqueResultException {
        try{
            ThingTypeTemplate thingTypeTemplate = thingTypeTemplateService.getByCode("upc");
            if (thingTypeTemplate != null) {
                removeUdfField("colorCode", thingTypeTemplate);
                PopDBRequiredIOT.insertUdfField("color", "Color", typeService.get(ThingTypeField.Type.TYPE_TEXT),
                    THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
                PopDBRequiredIOT.insertUdfField("colorHexadecimal", "Color Hexadecimal", typeService.get(ThingTypeField.Type.TYPE_COLOR),
                    THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
            }
        } catch (NonUniqueResultException e) {
            throw e;
        }
    }

    private void migrateTilesThingTypeConfigs(){
        ThingTypeTemplateCategory mojixRetailAppSync = thingTypeTemplateCategoryService.getByCode("MOJIX_RETAIL_APP_SYNC");
        if(mojixRetailAppSync != null){
            ThingTypeTemplate thingTypeTemplate = new ThingTypeTemplate();
            thingTypeTemplate.setCode("tilesConfiguration");
            thingTypeTemplate.setName("Home Tiles Config");
            thingTypeTemplate.setDescription("Home Tiles Configuration");
            thingTypeTemplate.setDisplayOrder(5);
            thingTypeTemplate.setThingTypeTemplateCategory(mojixRetailAppSync);
            thingTypeTemplate.setGroup(GroupService.getInstance().getRootGroup());
            thingTypeTemplate.setPathIcon("icon-thingtype-mobile-tiles-config");
            thingTypeTemplate = thingTypeTemplateService.insert(thingTypeTemplate);
            PopDBRequiredIOT.insertUdfField("resource", "Target of Resource", typeService.get(ThingTypeField.Type.TYPE_TEXT),
                    THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
            PopDBRequiredIOT.insertUdfField("backgroundColor", "Background Color", typeService.get(ThingTypeField.Type.TYPE_COLOR),
                    THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
            PopDBRequiredIOT.insertUdfField("textColor", "Text Color", typeService.get(ThingTypeField.Type.TYPE_COLOR),
                    THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
            PopDBRequiredIOT.insertUdfField("badgeColor", "Badge Color", typeService.get(ThingTypeField.Type.TYPE_COLOR),
                    THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
            PopDBRequiredIOT.insertUdfField("badgeTextColor", "Badge Text Color", typeService.get(ThingTypeField.Type.TYPE_COLOR),
                    THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
            PopDBRequiredIOT.insertUdfField("order", "Order/Position of Tile", typeService.get(ThingTypeField.Type.TYPE_NUMBER),
                    THING_TYPE_DATA_TYPE, false, thingTypeTemplate);
        }
    }

}
