package com.tierconnect.riot.migration.steps.group;

    import com.tierconnect.riot.appcore.entities.Group;
    import com.tierconnect.riot.appcore.entities.GroupType;
    import com.tierconnect.riot.appcore.services.GroupService;
    import com.tierconnect.riot.appcore.services.GroupTypeService;
    import com.tierconnect.riot.migration.DBHelper;
    import com.tierconnect.riot.migration.steps.MigrationStep;
    import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
    import org.apache.log4j.Logger;

public class Migrate_ConsortiumGroupType_VIZIX5137 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ConsortiumGroupType_VIZIX5137.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateGroupType();
    }

    /**
     * Remove the Consortium Group Type.
     */
    private void migrateGroupType() {
        logger.info("Removing Consortium group type");

        GroupTypeService groupTypeService = GroupTypeService.getInstance();

        GroupType groupType = null;

        try {
            groupType = groupTypeService.getByName("Consortium");
        } catch (NonUniqueResultException e) {
            e.printStackTrace();
        }

        if(groupType != null) {
            logger.info("Removing Consortium group type...");
            groupTypeService.delete(groupType);
            logger.info("Consortium group type removed successfully");
        } else {
            logger.info("Consortium group type not found");
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }
}