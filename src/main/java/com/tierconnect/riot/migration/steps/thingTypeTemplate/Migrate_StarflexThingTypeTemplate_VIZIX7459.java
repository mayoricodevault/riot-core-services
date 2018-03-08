package com.tierconnect.riot.migration.steps.thingTypeTemplate;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.iot.services.ThingTypeFieldTemplateService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

public class Migrate_StarflexThingTypeTemplate_VIZIX7459 implements MigrationStep {

    private static Logger logger = Logger.getLogger(Migrate_StarflexThingTypeTemplate_VIZIX7459.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QThingTypeFieldTemplate.thingTypeFieldTemplate.defaultValue.like("%${macId}%"));
        for (ThingTypeFieldTemplate ttft : ThingTypeFieldTemplateService.getThingTypeFieldTemplateDAO().selectAllBy(be)) {
            String defaultValue = ttft.getDefaultValue().replace("${macId}", "${serialNumber}");
            ttft.setDefaultValue(defaultValue);
            ThingTypeFieldTemplateService.getInstance().update(ttft);
        }
        be = new BooleanBuilder();
        be = be.and(QThingTypeField.thingTypeField.defaultValue.like("%${macId}%"));
        for (ThingTypeField ttf : ThingTypeFieldService.getThingTypeFieldDAO().selectAllBy(be)) {
            String defaultValue = ttf.getDefaultValue().replace("${macId}", "${serialNumber}");
            ttf.setDefaultValue(defaultValue);
            ThingTypeFieldService.getInstance().update(ttf);
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

}
