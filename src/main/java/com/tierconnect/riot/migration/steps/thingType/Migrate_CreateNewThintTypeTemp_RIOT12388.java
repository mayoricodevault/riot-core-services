package com.tierconnect.riot.migration.steps.thingType;

import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.ThingTypeFieldTemplate;
import com.tierconnect.riot.iot.entities.ThingTypeTemplate;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.DataTypeService;
import com.tierconnect.riot.iot.services.ThingTypeFieldTemplateService;
import com.tierconnect.riot.iot.services.ThingTypeTemplateService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.Set;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_CreateNewThintTypeTemp_RIOT12388 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_CreateNewThintTypeTemp_RIOT12388.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        insertThingTypeFieldsRFIDTag();
    }

    /**
     * insertNewThingTypeFieldsRFIDTag
     * Insert a new thing type fields  into a RFID Tag Thing Type Template
     */
    public void insertThingTypeFieldsRFIDTag() {
        ThingTypeTemplate thingTypeTemplate = ThingTypeTemplateService.getInstance().getByNameUnique("RFID Tag");
        Set<String> labelsRFIDTag = ThingTypeFieldTemplateService.getInstance().getLabelsThingTypeFieldTemplate
                (thingTypeTemplate);
        if (!labelsRFIDTag.isEmpty()) {
            if (!labelsRFIDTag.contains("lastLocateTime")) {
                PopDBRequiredIOT.insertUdfField("lastLocateTime", "lastLocateTime", "", "",
                        DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), "DATA_TYPE",
                        false, thingTypeTemplate, "");
            }
            if (!labelsRFIDTag.contains("lastDetectTime")) {
                PopDBRequiredIOT.insertUdfField("lastDetectTime", "lastDetectTime", "", "",
                        DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), "DATA_TYPE",
                        false, thingTypeTemplate, "");
            }
            if (!labelsRFIDTag.contains("zone")) {
                PopDBRequiredIOT.insertUdfField("zone", "zone", "", "",
                        DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_ZONE.value), "DATA_TYPE", true,
                        thingTypeTemplate, "");
            }
            if (labelsRFIDTag.contains("location")) {
                ThingTypeFieldTemplate thingTypeFieldTemplateLocation = ThingTypeFieldTemplateService.getInstance()
                        .getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplate.getId(), "location");
                if (thingTypeFieldTemplateLocation != null) {
                    thingTypeFieldTemplateLocation.setTimeSeries(true);
                    ThingTypeFieldTemplateService.getInstance().update(thingTypeFieldTemplateLocation);
                }
            }
            if (labelsRFIDTag.contains("locationXYZ")) {
                ThingTypeFieldTemplate thingTypeFieldTemplateLocation = ThingTypeFieldTemplateService.getInstance()
                        .getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplate.getId(), "locationXYZ");
                if (thingTypeFieldTemplateLocation != null) {
                    thingTypeFieldTemplateLocation.setTimeSeries(true);
                    ThingTypeFieldTemplateService.getInstance().update(thingTypeFieldTemplateLocation);
                }
            }
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
