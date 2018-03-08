package com.tierconnect.riot.migration.steps.thingType;

import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by hmartinez on 04-05-17.
 */
public class Migrate_ThigTypeTemplate_starFlexConfig_VIZIX_4417 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ThigTypeTemplate_starFlexConfig_VIZIX_4417.class);
    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateThingTypeTemplateStarFlexConfig();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    public void migrateThingTypeTemplateStarFlexConfig(){
        try{
            ThingTypeTemplate thingTypeTemplateStarFlexConfig = ThingTypeTemplateService.getInstance().getByName("STARflex Config");
            ThingTypeFieldTemplate thingTypeFieldTemplateUserName = ThingTypeFieldTemplateService.getInstance().insertUdfField("mqtt_username", "mqtt_username", "", "", DataTypeService.getInstance().get(1L), "DATA_TYPE", false, thingTypeTemplateStarFlexConfig, "");
            ThingTypeFieldTemplate thingTypeFieldTemplatePassword = ThingTypeFieldTemplateService.getInstance().insertUdfField("mqtt_password", "mqtt_password", "", "", DataTypeService.getInstance().get(1L), "DATA_TYPE", false, thingTypeTemplateStarFlexConfig, "");

            List<ThingType> thingTypeFieldList = ThingTypeService.getInstance().getThingTypesByThingTypeTemplate(thingTypeTemplateStarFlexConfig.getId());
            DataType dataType = DataTypeService.getInstance().get(1L);
            for(ThingType thingType:thingTypeFieldList) {
                ThingTypeFieldService.getInstance().insertThingTypeField(thingType,"mqtt_username","","","DATA_TYPE",dataType,false,"",thingTypeFieldTemplateUserName.getId());
                ThingTypeFieldService.getInstance().insertThingTypeField(thingType,"mqtt_password","","","DATA_TYPE",dataType,false,"",thingTypeFieldTemplatePassword.getId());
            }

        }catch (NonUniqueResultException e){
            logger.error("Error while getting ThingTypeTemplate STARflex Config", e);
        }
    }
}
