package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.ThingTypeFieldTemplate;
import com.tierconnect.riot.iot.entities.ThingTypeTemplate;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.DataTypeService;
import com.tierconnect.riot.iot.services.ThingTypeFieldTemplateService;
import com.tierconnect.riot.iot.services.ThingTypeTemplateService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_UpdateCoreBridge_RIOT12086 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_UpdateCoreBridge_RIOT12086.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        updateCoreBridge();
    }

    private void updateCoreBridge() {
        Group rootGroup = GroupService.getInstance().get(1L);
        try {
            ThingTypeTemplate coreBridge = ThingTypeTemplateService.getInstance().getByName("CoreBridge");
            if (coreBridge == null) {
                logger.info("Insert ThingTypeTemplate [CoreBridge] ");
                PopDBRequiredIOT.insertCoreBridgeTemplate4_3RC13(rootGroup);
            } else {
                ThingTypeFieldTemplate lptCacheFieldTemplate = ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(coreBridge.getId(), "lpt_cache");
                if (lptCacheFieldTemplate == null){
                    logger.info("Update ThingTypeTemplate [CoreBridge] with new thing type field [lpt_cache] ");
                    PopDBRequiredIOT.insertUdfField("lpt_cache", "lpt_cache", "", "", DataTypeService.getInstance()
                            .get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, coreBridge, "");
                }
            }
        } catch (NonUniqueResultException e) {
            logger.error("Error in add new UDF [lpt_cache] to thing type template CoreBridge ");
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

}
