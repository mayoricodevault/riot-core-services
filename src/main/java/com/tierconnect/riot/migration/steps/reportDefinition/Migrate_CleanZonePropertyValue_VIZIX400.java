package com.tierconnect.riot.migration.steps.reportDefinition;

import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.entities.ZonePropertyValue;
import com.tierconnect.riot.iot.services.ZonePropertyValueService;
import com.tierconnect.riot.iot.services.ZoneService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;

import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : rchirinos
 * @date : 1/19/17 1:28 PM
 * @version:
 */
public class Migrate_CleanZonePropertyValue_VIZIX400 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_CleanZonePropertyValue_VIZIX400.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        cleanZonePropertyValue();
    }

    /**
     * Clean wrong properties values
     */
    public void cleanZonePropertyValue(){
        List<ZonePropertyValue> zonePropertyValues = ZonePropertyValueService.getInstance().getZonePropertyValues();
        if ((zonePropertyValues != null) && (zonePropertyValues.size() > 0)){
            for (ZonePropertyValue zonePropertyValue: zonePropertyValues){
                Zone zone = ZoneService.getInstance().get(zonePropertyValue.getZoneId());
                if (zone == null){
                    zonePropertyValue.setZonePropertyId(null);
                    zonePropertyValue.setZoneId(null);
                    ZonePropertyValueService.getInstance().delete(zonePropertyValue);
                }
            }
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }
}
