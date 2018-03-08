package com.tierconnect.riot.migration.steps.thingType;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by mauricio on 6/23/17.
 */
public class Migrate_RemoveDeprecatedFieldsFromSmartContractDocument_VIZIX4861 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_RemoveDeprecatedFieldsFromSmartContractDocument_VIZIX4861.class);

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
        ThingTypeTemplateService thingTypeTemplateService = ThingTypeTemplateService.getInstance();

        ThingTypeTemplate thingTypeTemplate = null;
        try {
            thingTypeTemplate = thingTypeTemplateService.getByCode("SMART_CONTRACT_DOCUMENT");
        } catch (NonUniqueResultException e) {
            e.printStackTrace();
        }
        if (thingTypeTemplate != null) {
            this.removeUdfFieldFromTemplate("smartContractConfig", thingTypeTemplate);
            this.removeUdfFieldFromTemplate("transitionEnabled", thingTypeTemplate);
            this.removeUdfFieldFromTemplate("transitionInExecution", thingTypeTemplate);
            this.removeFieldsFromTyngTypesByTemplate(thingTypeTemplate);
        }


    }
    private void removeFieldsFromTyngTypesByTemplate(ThingTypeTemplate thingTypeTemplate){
        ThingTypeService thingTypeService = ThingTypeService.getInstance();
        List<ThingType> thingTypes = thingTypeService.getThingTypesByThingTypeTemplate(thingTypeTemplate.getId());
        for(ThingType thingType : thingTypes){
            this.removeUdfFieldFromThingType("smartContractConfig", thingType);
            this.removeUdfFieldFromThingType("transitionEnabled", thingType);
            this.removeUdfFieldFromThingType("transitionInExecution", thingType);
        }
    }

    private void removeUdfFieldFromThingType(String udfName, ThingType thingType) {
        ThingTypeFieldService thingTypeFieldService = ThingTypeFieldService.getInstance();
        List<ThingTypeField> thingTypeFields = null;
        thingTypeFields = thingTypeFieldService.getThingTypeFieldByNameAndTypeCode(udfName,thingType.getCode());
        if(thingTypeFields!=null)
            for(ThingTypeField thingTypeField : thingTypeFields) {
                    thingTypeFieldService.delete(thingTypeField);
            }
    }

    private void removeUdfFieldFromTemplate(String udfName, ThingTypeTemplate thingTypeTemplate) {
        ThingTypeFieldTemplateService thingTypeFieldTemplateService = ThingTypeFieldTemplateService.getInstance();

        ThingTypeFieldTemplate thingTypeFieldTemplate = null;
        thingTypeFieldTemplate = thingTypeFieldTemplateService.getThingTypeFieldTemplateByThingTypeTemplate(
                thingTypeTemplate.getId(), udfName);

    }
}

