package com.tierconnect.riot.migration.steps.thingType;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.iot.entities.QThingTypeField;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by rchirinos
 * on 30/08/2017
 */
public class Migrate_MakeTimeseriesTrueForThingTypeUdf_VIZIX7845 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_MakeTimeseriesTrueForThingTypeUdf_VIZIX7845.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateThingTypeUdfWithTimeseries();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    public void migrateThingTypeUdfWithTimeseries(){
        BooleanBuilder and = new BooleanBuilder();
        and.and(QThingTypeField.thingTypeField.dataTypeThingTypeId.isNotNull());
        List<ThingTypeField> lstThingTypeField = ThingTypeFieldService.getInstance().listPaginated(and, null, null);
        for(ThingTypeField thingTypeField:lstThingTypeField){
            thingTypeField.setTimeSeries(true);
            ThingTypeFieldService.getInstance().update(thingTypeField);
        }
    }


}
