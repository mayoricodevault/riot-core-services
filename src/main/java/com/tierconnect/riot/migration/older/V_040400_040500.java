package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.popdb.PopDBSpark;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by rchirinos on 10/25/2016
 */
@Deprecated
public class V_040400_040500 implements MigrationStepOld {
    static Logger logger = Logger.getLogger(V_040400_040500.class);

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(40400);
    }

    @Override
    public int getToVersion() {
        return 40500;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = DBHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V040400_to_040500.sql");
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateConnectiontype();
    }

    @Override
    public void migrateSQLAfter() throws Exception {
    }

    /**
     * create connexion (SQL, KAFKA, SERVICE, SPARK) if not exist
     */
    private void migrateConnectiontype() {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        ConnectionTypeService connectionTypeService = ConnectionTypeService.getInstance();
        if (connectionTypeService.getConnectionTypeByCode("SPARK") == null) {
            logger.info("create connection type SPARK");
            PopDBSpark.populateSparkConnection(rootGroup);
        }
        if (connectionTypeService.getConnectionTypeByCode("KAFKA") == null) {
            logger.info("create connection type KAFKA");
            PopDBSpark.populateKAFKAConnection(rootGroup);
        }
        if (connectionTypeService.getConnectionTypeByCode("SQL") == null) {
            logger.info("create connection type SQL");
            PopDBRequired.populateSQLConnection0(rootGroup);
        }
        if (connectionTypeService.getConnectionTypeByCode("HADOOP") == null) {
            logger.info("create connection type HADOOP");
            PopDBSpark.populateHadoopConnection(rootGroup);
        }
    }
}