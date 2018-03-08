package com.tierconnect.riot.migration.steps.mapMaker;

import com.tierconnect.riot.iot.entities.LocalMap;
import com.tierconnect.riot.iot.services.LocalMapService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;

import java.util.List;

/**
 * Created by rsejas on 3/10/17.
 */
public class Migrate_MigrationMapMakerZones_VIZIX2420 implements MigrationStep  {

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateLocalMapValues();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void migrateLocalMapValues() {
        List<LocalMap> localMapList = LocalMapService.getInstance().listPaginated(null, null);
        for (LocalMap localMap:localMapList) {
            localMap.setRotationDegree(0.0);
            LocalMapService.getInstance().update(localMap);
            LocalMapService.updateMapPoints(localMap, null);
        }
    }
}
