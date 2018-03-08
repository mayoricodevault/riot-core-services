package com.tierconnect.riot.migration.steps.thingType;

import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.ThingTypeTemplate;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.DataTypeService;
import com.tierconnect.riot.iot.services.ThingTypeTemplateService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_ThingTypeTemplateRFIDPrinter_VIZIX325 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ThingTypeTemplateRFIDPrinter_VIZIX325.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateTemplateUdfsRFIDPrinter();
    }

    private void migrateTemplateUdfsRFIDPrinter() {
        try{
            ThingTypeTemplate thingTypeTemplateHead = ThingTypeTemplateService.getInstance().getByName("RFID Printer");
            PopDBRequiredIOT.insertUdfField("proxy", "proxy", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_BOOLEAN.value), "DATA_TYPE", false, thingTypeTemplateHead, "");
            PopDBRequiredIOT.insertUdfField("printerProxyIp", "printerProxyIp", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), "DATA_TYPE", false, thingTypeTemplateHead, "");
            PopDBRequiredIOT.insertUdfField("printerProxyPort", "printerProxyPort", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), "DATA_TYPE", false, thingTypeTemplateHead, "");
            PopDBRequiredIOT.insertUdfField("printerProxyEndPoint", "printerProxyEndPoint", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), "DATA_TYPE", false, thingTypeTemplateHead, "");
        }catch (NonUniqueResultException e){
            logger.error("Error while getting ThingTypeTemplate RFID Printer", e);
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
