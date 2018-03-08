package com.tierconnect.riot.migration.steps.thingType;

import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;
import java.util.List;

/**
 * Created by ybarriga
 * on 29/05/2017
 */
public class Migrate_ThingTypeField_VIZIX5140 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ThingTypeField_VIZIX5140.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateDateDefaultValue();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
    }

    /**
     * migrateDateDefaultValue
     * set null when a Date data type has an invalid value.
     */
    private void migrateDateDefaultValue(){
        List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().
                getThingTypeFieldsByType(ThingTypeField.Type.TYPE_DATE.value);
        for (ThingTypeField thingTypeField: thingTypeFields){
            Object value = thingTypeField.getDefaultValue();
            if (!ThingTypeFieldService.getInstance().isDate(value)){
                thingTypeField.setDefaultValue(null);
                ThingTypeFieldService.getInstance().update(thingTypeField);
            }
        }
    }
}
