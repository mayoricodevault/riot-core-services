package com.tierconnect.riot.migration.steps.thingTypeTemplate;

import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

public class Migrate_RemoveSmartContractThingTypeTemplate_VIZIX5137 implements MigrationStep {

    private static Logger logger = Logger.getLogger(Migrate_RemoveSmartContractThingTypeTemplate_VIZIX5137.class);

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
            removeUdfField("blockchainId", thingTypeTemplate);
            removeUdfField("status", thingTypeTemplate);
            removeUdfField("smartContractConfig", thingTypeTemplate);
            removeUdfField("transitionEnabled", thingTypeTemplate);
            removeUdfField("transitionInExecution", thingTypeTemplate);
            removeUdfField("active", thingTypeTemplate);
        }
    }

    private void removeUdfField(String udfName, ThingTypeTemplate thingTypeTemplate) {
        ThingTypeFieldTemplateService thingTypeFieldTemplateService = ThingTypeFieldTemplateService.getInstance();

        ThingTypeFieldTemplate thingTypeFieldTemplate = null;
        thingTypeFieldTemplate = thingTypeFieldTemplateService.getThingTypeFieldTemplateByThingTypeTemplate(
                thingTypeTemplate.getId(), udfName);

        if(thingTypeFieldTemplate != null) {
            thingTypeFieldTemplateService.delete(thingTypeFieldTemplate);
        }
    }
}
