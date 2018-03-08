package com.tierconnect.riot.migration.steps.group;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by mauricio on 5/30/17.
 */
public class Migrate_BlockchainConsortiumOwnershipLevel_VIZIX5226  implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_InsertLDAP_RIOT11642.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        insertConsortiumOwnershipLevel();
    }

    private void insertConsortiumOwnershipLevel() {
        logger.debug("Start migrating Blockchain consortium ownership level");
        Group rootGroup = GroupService.getInstance().getRootGroup();
        Field f64 = PopDBUtils.popFieldService("consortium", "consortium", "Blockchain Consortium", rootGroup,
                "Ownership Levels",  "java.lang.Integer", 3L, false);
 		PopDBUtils.popGroupField(rootGroup, f64, "3");
        logger.debug("Blockchain consortium ownership level migration done");
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }
}
