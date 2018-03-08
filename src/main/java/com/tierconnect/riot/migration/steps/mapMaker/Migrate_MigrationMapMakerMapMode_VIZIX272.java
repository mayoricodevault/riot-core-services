package com.tierconnect.riot.migration.steps.mapMaker;

import com.tierconnect.riot.iot.entities.LocalMap;
import com.tierconnect.riot.iot.services.LocalMapService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_MigrationMapMakerMapMode_VIZIX272 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrationMapMakerMapMode_VIZIX272.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateMapMakerValues();
    }

    /**
     * Migrate values for Map Maker
     */
    private void migrateMapMakerValues() {
        List<LocalMap> localMapList = LocalMapService.getInstance().listPaginated(null, null);
        for (LocalMap localMap:localMapList) {
            localMap.setModifiedTime(new Date().getTime());
            localMap.setXNominal(0.0);
            localMap.setYNominal(0.0);
            localMap.setLatOriginNominal(localMap.getLatOrigin());
            localMap.setLonOriginNominal(localMap.getLonOrigin());
            LocalMapService.getInstance().update(localMap);
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
