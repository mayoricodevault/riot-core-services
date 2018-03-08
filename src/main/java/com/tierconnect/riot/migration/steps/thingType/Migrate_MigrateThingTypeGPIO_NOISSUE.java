package com.tierconnect.riot.migration.steps.thingType;

import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.QThingTypeTemplate;
import com.tierconnect.riot.iot.entities.ThingTypeTemplate;
import com.tierconnect.riot.iot.services.ThingTypeTemplateService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_MigrateThingTypeGPIO_NOISSUE implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MigrateThingTypeGPIO_NOISSUE.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateGPIOThingType();
    }

    /**
     * Delete GPIO Thing Type
     */
    public static void migrateGPIOThingType() {
        ThingTypeTemplate thingTypeTemplateGPIO = getThingTypeTemplateGPIO();
        if (thingTypeTemplateGPIO != null) {
            ThingTypeTemplateService.getInstance().delete(thingTypeTemplateGPIO);
        }
    }

    /**
     * get GPIO Thing Type Template
     *
     * @return
     */
    public static ThingTypeTemplate getThingTypeTemplateGPIO() {
        HibernateQuery query = ThingTypeTemplateService.getThingTypeTemplateDAO().getQuery();
        return query.where(QThingTypeTemplate.thingTypeTemplate.name.eq("GPIO"))
                .uniqueResult(QThingTypeTemplate.thingTypeTemplate);
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
