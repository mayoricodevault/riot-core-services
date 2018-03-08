package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.iot.entities.LocalMap;
import com.tierconnect.riot.iot.popdb.PopDBMojixRetail;
import com.tierconnect.riot.iot.services.LocalMapService;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by rsejas on 1/17/17.
 */
@Deprecated
public class V_050000_RC2_050000_RC3 implements MigrationStepOld {
    private static Logger logger = Logger.getLogger(V_050000_RC2_050000_RC3.class);

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(5000002);
    }

    @Override
    public int getToVersion() {
        return 5000003;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        logger.info("Migrating from: " + getFromVersions() + " To: " + getToVersion());
        DBHelper dbHelper = new DBHelper();
        String databaseType = DBHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V050000_RC2_to_050000_RC3.sql");
        if(!dbHelper.existTable("thingtypepath") && !dbHelper.existColumn("localMap", "latOriginNominal")) {
            dbHelper.executeSQLFile("sql/" + databaseType + "/V050000_RC2_to_050000_RC3.sql");
        }
    }

    @Override
    public void migrateHibernate() throws Exception {
        PopDBMojixRetail.populateThingTypePath();
        migrateMapMakerValues();
    }

    @Override
    public void migrateSQLAfter() throws Exception {

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
}
