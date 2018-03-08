package com.tierconnect.riot.migration.older;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.popdb.PopDBIOTUtils;
import com.tierconnect.riot.iot.popdb.PopDBMojixRetail;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;

/**
 * Created by rsejas on 12/28/16.
 */
@Deprecated
public class V_040500_050000_RC1 implements MigrationStepOld {

    private static Logger logger = Logger.getLogger(V_040500_050000_RC1.class);


    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(40500);
    }

    @Override
    public int getToVersion() {
        return 5000001;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateTemplateUdfsRFIDPrinter();
        addAliasFieldInFlexTagThingType();
        cleanZonePropertyValue();
    }

    public void cleanZonePropertyValue(){
        List<ZonePropertyValue> zonePropertyValues = ZonePropertyValueService.getInstance().getZonePropertyValues();
        if ((zonePropertyValues != null) && (zonePropertyValues.size() > 0)){
            for (ZonePropertyValue zonePropertyValue: zonePropertyValues){
                Zone zone = ZoneService.getInstance().get(zonePropertyValue.getZoneId());
                if (zone == null){
                    zonePropertyValue.setZonePropertyId(null);
                    zonePropertyValue.setZoneId(null);
                    ZonePropertyValueService.getInstance().delete(zonePropertyValue);
                }
            }
        }
    }

    private void migrateTemplateUdfsRFIDPrinter() {
        try{
            ThingTypeTemplate thingTypeTemplateHead = ThingTypeTemplateService.getInstance().getByName("RFID Printer");
            PopDBRequiredIOT.insertUdfField("proxy", "proxy", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_BOOLEAN.value), "DATA_TYPE", false, thingTypeTemplateHead, "");
            PopDBRequiredIOT.insertUdfField("printerProxyIp", "printerProxyIp", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), "DATA_TYPE", false, thingTypeTemplateHead, "");
            PopDBRequiredIOT.insertUdfField("printerProxyPort", "printerProxyPort", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), "DATA_TYPE", false, thingTypeTemplateHead, "");
            PopDBRequiredIOT.insertUdfField("printerProxyEndPoint", "printerProxyEndPoint", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), "DATA_TYPE", false, thingTypeTemplateHead, "");
        }catch (NonUniqueResultException e){
            logger.error("Error while getting ThingTypeTemplate RFID Printer", e);
        }

    }

    @Override
    public void migrateSQLAfter() throws Exception {

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
                            "alias", "alias", "", "",DataTypeService.getInstance().get(1L),
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
}
