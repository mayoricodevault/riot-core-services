package com.tierconnect.riot.migration.steps.dataType;

import com.tierconnect.riot.iot.entities.DataType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.services.DataTypeService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

import static com.tierconnect.riot.commons.Constants.THING_TYPE_DATA_TYPE;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_IconColorDataType_VIZIX7670_VIZIX7264 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_IconColorDataType_VIZIX7670_VIZIX7264.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateFeature();
    }

    private void migrateFeature() {
        try {
            DataType dataType = new DataType();
            if (DataTypeService.getInstance().getByCode("ICON") == null) {
                dataType.setId(ThingTypeField.Type.TYPE_ICON.value);
                dataType.setTypeParent(THING_TYPE_DATA_TYPE);
                dataType.setCode("ICON");
                dataType.setValue("Icon");
                dataType.setType("Standard Data Types");
                dataType.setDescription("Icon data type");
                dataType.setClazz("java.lang.String");
                DataTypeService.getInstance().insert(dataType);
            }
            if (DataTypeService.getInstance().getByCode("COLOR") == null) {
                dataType = new DataType();
                dataType.setId(ThingTypeField.Type.TYPE_COLOR.value);
                dataType.setTypeParent(THING_TYPE_DATA_TYPE);
                dataType.setCode("COLOR");
                dataType.setValue("Color");
                dataType.setType("Standard Data Types");
                dataType.setDescription("Color type");
                dataType.setClazz("java.lang.String");
                DataTypeService.getInstance().insert(dataType);
            }
        } catch (NonUniqueResultException ignored) {
        }

    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
