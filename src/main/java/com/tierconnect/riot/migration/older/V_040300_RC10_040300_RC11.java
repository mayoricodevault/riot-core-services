package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.iot.entities.ThingTypeTemplate;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : dbascope
 * @date : 8/30/16 9:17 AM
 * @version:
 */
@Deprecated
public class V_040300_RC10_040300_RC11 implements MigrationStepOld {

    Logger logger = Logger.getLogger(V_040200_040300.class);

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(4030001);
    }

    @Override
    public int getToVersion() {
        return 4030011;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = DBHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V040300_RC10_to_040300_RC11.sql");
    }

    @Override
    public void migrateHibernate() throws Exception {
        logger.info("Migrating version RC11");
    }

    @Override
    public void migrateSQLAfter() throws Exception {
    }

    private void migrateConnection() {
    }

    private void migrateConnectionType() {
    }

    private void migrateCoreBridgeConfiguration() {
    }

    private void migrationAlienReaderRules(){
    }

    private void migrateBridgeThingTypeTemplates() {
    }


    private void insertThingTypeFieldTemplate(ThingTypeTemplate thingType, String UDFString) throws IOException {
    }

    public String getConfigForMqtt(String host, Integer port) {
        return null;
    }

    public String getConfigForMongo(String host, Integer port, String username, String password) {
        return null;
    }

    public String getConfigForFtp(String host, Integer port, String username, String password) {
        return null;
    }

    /**
     * @param edgeboxCode
     * @param edgeboxConfiguration
     * @return a replaced text Edgebox (to add a comma when the text is without it)
     */
    public String getChangeEdgebox(String edgeboxCode, String edgeboxConfiguration) {
        return null;
    }

    /**
     * Method to regularize types of edgeboxes
     */
    private void regularizeTypeEdgeBox() {
    }

    /**
     * @param configuration the configuration attribute of edgebox table
     * @return true if it is a core bridge configuration
     */
    public boolean isCoreBridge(String configuration) {
        return false;
    }
}