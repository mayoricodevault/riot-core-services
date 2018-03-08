package com.tierconnect.riot.migration.steps.thingType;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.iot.entities.QThingType;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeFieldTemplate;
import com.tierconnect.riot.iot.entities.ThingTypeTemplate;
import com.tierconnect.riot.iot.popdb.PopDBIOTUtils;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.DataTypeService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.iot.services.ThingTypeTemplateService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_FlexTagAliasUdf_VIZIX178 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_FlexTagAliasUdf_VIZIX178.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        addAliasFieldInFlexTagThingType();
    }

    /**
     * Add Alias Field into FlexTag Thing Type and Thing Type template
     */
    public void addAliasFieldInFlexTagThingType() {
        try{
            //Insert alias Field in FlexTag Thing Type Template
            ThingTypeTemplate flexTemplate = ThingTypeTemplateService.getInstance().getByName("FlexTag");
            ThingTypeFieldTemplate aliasTemplateField =
                    PopDBRequiredIOT.insertUdfField(
                            "alias", "alias", "", "", DataTypeService.getInstance().get(1L),
                            "DATA_TYPE", true, flexTemplate, "");
            //Insert alias Field in all Thing Type instances of FlexTag Thing Type Template
            BooleanBuilder b = new BooleanBuilder();
            b = b.and(QThingType.thingType.thingTypeTemplate.id.eq(flexTemplate.getId()));
            List<ThingType> lstThingType = ThingTypeService.getInstance().listPaginated(b, null, null);
            for (ThingType thingType : lstThingType ) {
                PopDBIOTUtils.popThingTypeField(thingType,
                        aliasTemplateField.getName(),
                        aliasTemplateField.getUnit(),
                        aliasTemplateField.getSymbol(),
                        aliasTemplateField.getTypeParent(),
                        aliasTemplateField.getType().getId(),
                        aliasTemplateField.isTimeSeries(),
                        aliasTemplateField.getDefaultValue(),
                        aliasTemplateField.getId(),
                        aliasTemplateField.getDataTypeThingTypeId());
            }
        }catch(NonUniqueResultException e) {
            logger.error("Error getting Template FlexTag in Thing Type Templates.", e);
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
