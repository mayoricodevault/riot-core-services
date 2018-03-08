package com.tierconnect.riot.migration.steps.thingType;

import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.ThingTypeFieldTemplate;
import com.tierconnect.riot.iot.entities.ThingTypeTemplate;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by bbustillos on 8/29/17.
 */
public class Migrate_ThingTypeTemplate_SFTag_VIZIX5508 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ThingTypeTemplate_SFTag_VIZIX5508.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        addSeqNumFieldToSTARflexTagTemplate();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void addSeqNumFieldToSTARflexTagTemplate(){
        try {
            ThingTypeTemplate tTTSFTag = ThingTypeTemplateService.getInstance().getByCode("STARflex");
            ThingTypeFieldTemplate tTFTSeqNum = ThingTypeFieldTemplateService.getInstance().insertUdfField("seqNum", "Sequence Number",
                    "", "", DataTypeService.getInstance().get(4L), "DATA_TYPE", true, tTTSFTag, "");
            logger.info("ThingTypeField 'seqNum' has been added to ThingTypeTemplate STARflexTag");
            List<ThingType> thingTypesByTTT = ThingTypeService.getInstance().getThingTypesByThingTypeTemplate(tTTSFTag.getId());
            for (ThingType thingTypeTag : thingTypesByTTT){
                Set<ThingTypeField> setThingTypeFields = thingTypeTag.getThingTypeFields();
                // Control if the field already exists over the thingType
                boolean controlField = false;
                for(ThingTypeField thingTypeField : setThingTypeFields){
                    if (thingTypeField.getName().equals("seqNum")){
                        controlField = true;
                    }
                }
                // Add the field if doesn't exist
                if (!controlField){
                    ThingTypeFieldService.getInstance().insertThingTypeField(thingTypeTag,"seqNum","","","DATA_TYPE",
                            DataTypeService.getInstance().get(4L),true,"",tTFTSeqNum.getId());
                    logger.info("ThingTypeField 'seqNum' has been added to ThingTypes STARflexTag");
                } else {
                    logger.warn("ThingTypeField 'seqNum' already exists in thingType STARflexTag");
                }
            }
        } catch (NonUniqueResultException e) {
            logger.error("It is not possible to get the STARflexTagTemplate");
        }
    }
}
