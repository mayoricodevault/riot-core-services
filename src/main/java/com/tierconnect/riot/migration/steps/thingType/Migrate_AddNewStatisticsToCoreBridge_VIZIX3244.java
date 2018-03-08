package com.tierconnect.riot.migration.steps.thingType;

import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

import java.util.List;

import static com.tierconnect.riot.commons.Constants.THING_TYPE_DATA_TYPE;

/**
 * Created by ruth on 07-05-17.
 */
public class Migrate_AddNewStatisticsToCoreBridge_VIZIX3244 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_AddNewStatisticsToCoreBridge_VIZIX3244.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        ThingTypeTemplate template = addFieldsInThingTypeTemplate();
        modifyCurrentCoreBridgeThingTypes(template);
    }

    /**
     * Add new fields in  Thing Type Template Core Bridge
     * @return ThingTypeTemplate object
     * @throws NonUniqueResultException
     */
    public ThingTypeTemplate addFieldsInThingTypeTemplate() throws NonUniqueResultException {
        ThingTypeTemplate template = ThingTypeTemplateService.getInstance().getByName("CoreBridge");
        if (template != null ){
            ThingTypeFieldTemplateService tttInstance = ThingTypeFieldTemplateService.getInstance();
            tttInstance.create("que_poll_idletime", "que_poll_idletime", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, template, "");
            tttInstance.create("que_size_pop", "que_size_pop", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, template, "");
            tttInstance.create("que_size_to", "que_size_to", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, template, "");
            tttInstance.create("que_size_blog", "que_size_blog", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, template, "");
            tttInstance.create("que_size_ooo", "que_size_ooo", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER), THING_TYPE_DATA_TYPE, true, template, "");
        }
        return template;
    }

    /**
     * Modify Current instances of Thing Types who were instantiated based on Core Bridge Thing Type Template
     * @param template template of Core Bridge
     */
    public void modifyCurrentCoreBridgeThingTypes(ThingTypeTemplate template){
        List<ThingType> lstThingType = ThingTypeService.getInstance().getThingTypesByThingTypeTemplate(template.getId());
        if ((lstThingType!=null) && (!lstThingType.isEmpty())) {
            for(ThingType thingType : lstThingType) {
                ThingTypeFieldService ttField = ThingTypeFieldService.getInstance();
                thingType.getThingTypeTemplate().getThingTypeFieldTemplate().stream().forEach( (field)-> {
                    if (field.getName().equals("que_poll_idletime") ||
                            field.getName().equals("que_size_pop") || field.getName().equals("que_size_to") ||
                            field.getName().equals("que_size_blog") || field.getName().equals("que_size_ooo")) {
                        ttField.insertThingTypeField(thingType,field.getName(),field.getUnit(),field.getSymbol(),field.getTypeParent(),
                                field.getType(),field.isTimeSeries(),field.getDefaultValue(), field.getId());
                    }
                });
            }
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
    }

}
