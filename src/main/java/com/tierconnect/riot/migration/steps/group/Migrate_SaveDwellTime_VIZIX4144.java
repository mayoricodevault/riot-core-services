package com.tierconnect.riot.migration.steps.group;

import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.GroupField;
import com.tierconnect.riot.appcore.services.FieldService;
import com.tierconnect.riot.appcore.services.GroupFieldService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_SaveDwellTime_VIZIX4144 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_SaveDwellTime_VIZIX4144.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFields();
    }

    private void migrateFields()
    {
        deleteSaveDwellTimeHistory();
    }

    /**
     * deleteSaveDwellTimeHistory
     */
    private void deleteSaveDwellTimeHistory(){
        Field saveDwellTimeHistory = FieldService.getInstance().selectByName("saveDwellTimeHistory");
        if (saveDwellTimeHistory != null){
            GroupField groupField = GroupFieldService.getInstance().getByField(saveDwellTimeHistory);
            GroupFieldService.getInstance().delete(groupField);
            FieldService.getInstance().delete(saveDwellTimeHistory);
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
    }


}
