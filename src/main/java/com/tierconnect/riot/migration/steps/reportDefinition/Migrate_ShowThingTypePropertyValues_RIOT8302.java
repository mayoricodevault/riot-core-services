package com.tierconnect.riot.migration.steps.reportDefinition;

import com.tierconnect.riot.iot.entities.ReportEntryOptionProperty;
import com.tierconnect.riot.iot.services.ReportEntryOptionPropertyService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_ShowThingTypePropertyValues_RIOT8302 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ShowThingTypePropertyValues_RIOT8302.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    private void migrateFeature(){
        List<ReportEntryOptionProperty> reportEntryOptionPropertyList = ReportEntryOptionPropertyService.getReportEntryOptionPropertyDAO().selectAll();
        if (null != reportEntryOptionPropertyList){
            for (ReportEntryOptionProperty reportEntryOptionProperty : reportEntryOptionPropertyList){
                reportEntryOptionProperty.setAllPropertyData(Boolean.FALSE);
            }
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
