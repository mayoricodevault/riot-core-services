package com.tierconnect.riot.migration.steps.thingType;

import com.tierconnect.riot.iot.entities.ThingTypeTemplate;
import com.tierconnect.riot.iot.services.ThingTypeTemplateService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;

import java.util.List;

/**
 * Created by vlad
 * on 06/27/17.
 */
public class Migrate_ThingTypeTemplate_VIZIX6082 implements MigrationStep {

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateSTARflexName("Starflex H&S", "STARflex H&S");
        migrateSTARflexName("Starflex tag", "STARflex tag");
    }

    private void migrateSTARflexName(String originName, String targetName) {
        List<ThingTypeTemplate> thingTypeTemplateList = ThingTypeTemplateService.getThingTypeTemplateDAO().selectAllBy("name", originName);

        if (thingTypeTemplateList != null && !thingTypeTemplateList.isEmpty()) {
            for (ThingTypeTemplate thingTypeTemplate : thingTypeTemplateList) {
                thingTypeTemplate.setName(targetName);
                ThingTypeTemplateService.getInstance().update(thingTypeTemplate);
            }
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

}
