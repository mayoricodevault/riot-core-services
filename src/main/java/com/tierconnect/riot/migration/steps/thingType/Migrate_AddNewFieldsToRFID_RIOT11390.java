package com.tierconnect.riot.migration.steps.thingType;

import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupField;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.FieldService;
import com.tierconnect.riot.appcore.services.GroupFieldService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.popdb.PopDBIOTUtils;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_AddNewFieldsToRFID_RIOT11390 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_AddNewFieldsToRFID_RIOT11390.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateBridgeThingTypeTemplates();
    }

    private void migrateBridgeThingTypeTemplates() {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        try {
            List<ThingTypeTemplate> thingTypeTemplates = ThingTypeTemplateService.getInstance().listPaginated(null, null, null);
            insertThingTypeTemplatesNewStructure(thingTypeTemplates, rootGroup);
            ThingTypeTemplateService instance = ThingTypeTemplateService.getInstance();
            if (!instance.existByName("CoreBridge")) {
                insertCoreBridgeTemplate4_3RC12(rootGroup);
                logger.info("Insert ThingTypeTemplate [CoreBridge] ");
            }
            if (!instance.existByName("EdgeBridge")) {
                PopDBRequiredIOT.insertEdgeBridgeTemplate(rootGroup);
                logger.info("Insert ThingTypeTemplate [EdgeBridge] ");
            }
            updateThingTypeTemplatesWithDependencies(thingTypeTemplates);
        } catch (Exception e) {
            logger.error("Error on migrateBridgeThingTypeTemplates method", e);
        }
    }

    private void updateThingTypeByFieldName(ThingTypeTemplate thingTypeTemplate, Set<String> labels){
        ThingTypeTemplateService.getInstance().update(thingTypeTemplate);
        List<ThingType> thingTypesCoreBridges = ThingTypeService.getInstance().getThingTypesByThingTypeTemplate(thingTypeTemplate.getId());
        if (thingTypesCoreBridges != null && !thingTypesCoreBridges.isEmpty()) {
            List<ThingTypeFieldTemplate> thingTypeFieldTemplateList =
                    ThingTypeFieldTemplateService.getInstance()
                            .getThingTypeFielTemplatedByThingTypeTemplateIdAndExcludeLabels(
                                    thingTypeTemplate.getId(), labels);
            if (thingTypeFieldTemplateList != null) {
                for (ThingType thingType : thingTypesCoreBridges) {
                    for (ThingTypeFieldTemplate thingTypeFieldTemplate : thingTypeFieldTemplateList) {
                        PopDBIOTUtils.popThingTypeField(thingType,
                                thingTypeFieldTemplate.getName(),
                                thingTypeFieldTemplate.getUnit(),
                                thingTypeFieldTemplate.getSymbol(),
                                thingTypeFieldTemplate.getTypeParent(),
                                thingTypeFieldTemplate.getType().getId(),
                                thingTypeFieldTemplate.isTimeSeries(),
                                thingTypeFieldTemplate.getDefaultValue(),
                                thingTypeFieldTemplate.getId(),
                                thingTypeFieldTemplate.getDataTypeThingTypeId());
                    }
                }
            }
        }
    }

    /**
     * Insert New Structure in Thing Type Templates
     * Insert new data into apc_field, group_field and thingtypefieldtemplate tables.
     * @param thingTypeTemplates list of thing Type Templates
     * @param rootGroup root Group
     */
    public void insertThingTypeTemplatesNewStructure(List<ThingTypeTemplate> thingTypeTemplates, Group rootGroup){
        insertFieldNewFields(rootGroup);
        insertThingTypeFieldTemplateNewUdfs(thingTypeTemplates);
    }

    /**
     *
     * @param thingTypeTemplates thing Type Templates
     */
    public void updateThingTypeTemplatesWithDependencies(
            List<ThingTypeTemplate> thingTypeTemplates) throws NonUniqueResultException {
        ThingTypeTemplate thingTypeTemplateGPIO = getThingTypeTemplateGPIO();
        if (thingTypeTemplateGPIO != null) {
            List<ThingType> lstThingTypeGPIO = ThingTypeService.getInstance().getThingTypesByThingTypeTemplate(thingTypeTemplateGPIO.getId());
            if ( (lstThingTypeGPIO!=null) && (!lstThingTypeGPIO.isEmpty())) {
                //Delete references of the GPIO template in Thing Types
                for (ThingType thingType : lstThingTypeGPIO) {
                    Set<ThingTypeField> fields = thingType.getThingTypeFields();
                    List<ThingTypeField> thingTypeFieldsGPIO =
                            ThingTypeFieldService.getInstance().getThingTypeFieldByTemplate(thingTypeTemplateGPIO.getId());
                    for (ThingTypeField thingTypeField: thingTypeFieldsGPIO){
                        thingTypeField.setThingTypeFieldTemplateId(null);
                        thingTypeField = ThingTypeFieldService.getInstance().update(thingTypeField);
                        fields.add(thingTypeField);
                    }
                    thingType.setThingTypeFields(fields);
                    thingType.setThingTypeTemplate(ThingTypeTemplateService.getInstance().getByName("Custom"));
                    ThingTypeService.getInstance().update(thingType);
                }
                //Delete Template GPIO
                ThingTypeTemplateService.getInstance().delete(thingTypeTemplateGPIO);
            }
            updateThingTypeFields(thingTypeTemplates);
        }
    }

    /**
     * Insert new fields into apc_field table
     * @param group group
     */
    public static void insertFieldNewFields(Group group){
        insertField("Map Report Limit", null, "Look & Feel", "mapReportLimit", "java.lang.Integer", true, group, "50000");
        insertField("Table Summary Report Limit", null, "Look & Feel", "tableSummaryReportLimit", "java.lang.Integer",
                true, group, "1000000");
    }

    /**
     * Insert  New Udfs into Thing Type Field Templates of RFID Printer, Logical Reader, RFID Tag, GPS Tag, FlexTag,
     * CoreBridge and EdgeBridge
     *
     * @param thingTypeTemplates List Thing Type Field Templates
     */
    public void insertThingTypeFieldTemplateNewUdfs(List<ThingTypeTemplate> thingTypeTemplates){
        for (ThingTypeTemplate thingTypeTemplate : thingTypeTemplates) {
            Set<String> labelsThingTypeFieldTemplate = ThingTypeFieldTemplateService.getInstance().getLabelsThingTypeFieldTemplate(thingTypeTemplate);
            switch (thingTypeTemplate.getName()) {
                case "RFID Printer":
                    insertRFIDPrinterNewUdfs(thingTypeTemplate, labelsThingTypeFieldTemplate);
                    break;
                case "Logical Reader":
                    PopDBRequiredIOT.insertLogicalReaderNewUdfs(thingTypeTemplate, labelsThingTypeFieldTemplate);
                    break;
                case "RFID Tag":
                    PopDBRequiredIOT.insertMandatoryValuesZoneInThingTypeFieldTemplate(thingTypeTemplate, labelsThingTypeFieldTemplate);
                    break;
                case "GPS Tag":
                    PopDBRequiredIOT.insertGPSTagNewUdfs(thingTypeTemplate, labelsThingTypeFieldTemplate);
                    break;
                case "FlexTag":
                    insertFlexTagNewUdfs(thingTypeTemplate,labelsThingTypeFieldTemplate);
                    break;
                case "CoreBridge":
                    insertCoreBridgeNewUdfs(thingTypeTemplate);
                    break;
                case "EdgeBridge":
                    insertEdgeBridgeNewUdfs(thingTypeTemplate);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * get GPIO Thing Type Template
     * @return
     */
    public ThingTypeTemplate getThingTypeTemplateGPIO (){
        HibernateQuery query = ThingTypeTemplateService.getThingTypeTemplateDAO().getQuery();
        return query.where(QThingTypeTemplate.thingTypeTemplate.name.eq("GPIO"))
                .uniqueResult(QThingTypeTemplate.thingTypeTemplate);
    }

    /**
     *
     * @param thingTypeTemplates thing Type Templates
     */
    public void updateThingTypeFields(List<ThingTypeTemplate> thingTypeTemplates){
        for (ThingTypeTemplate thingTypeTemplate: thingTypeTemplates){
            List<ThingType> thingTypes  = ThingTypeService.getInstance().getThingTypesByThingTypeTemplate(thingTypeTemplate.getId());
            switch (thingTypeTemplate.getName()){
                case "RFID Printer": insertThingTypeFieldsRFIDPrinter(thingTypes, thingTypeTemplate.getId());
                    break;
                case "Logical Reader": insertThingTypeFieldsLogicalReader(thingTypes, thingTypeTemplate.getId());
                    break;
                case "RFID Tag": insertThingTypeFieldsRFIDTag(thingTypes, thingTypeTemplate.getId());
                    break;
                case "GPS Tag": insertThingTypeFieldsGPSTag(thingTypes, thingTypeTemplate.getId());
                    break;
                case "FlexTag": insertThingTypeFieldsFlexTag(thingTypes, thingTypeTemplate.getId());
                    break;
                default: break;
            }
        }
    }

    /**
     *
     * @param thingTypeTemplate
     */
    public void insertCoreBridgeNewUdfs(ThingTypeTemplate thingTypeTemplate) {
        Set<ThingTypeFieldTemplate> fields = thingTypeTemplate.getThingTypeFieldTemplate() != null ?
                thingTypeTemplate.getThingTypeFieldTemplate() : new HashSet<ThingTypeFieldTemplate>();
        Set<String> labels = new HashSet<>();
        ThingTypeFieldTemplate thingTypeFieldTemplateRemove = null;
        for(ThingTypeFieldTemplate field : fields) {
            labels.add(field.getName());
            if ("sqn".equals(field.getName())) {
                thingTypeFieldTemplateRemove = field;
            }
        }
        if (!labels.isEmpty()){
            if (!labels.contains("age")) {
                PopDBRequiredIOT.insertUdfField("age", "age", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("lpt")) {
                PopDBRequiredIOT.insertUdfField("lpt", "lpt", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("lpt_cep")) {
                PopDBRequiredIOT.insertUdfField("lpt_cep", "lpt_cep", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("lpt_lazy_load")) {
                PopDBRequiredIOT.insertUdfField("lpt_lazy_load", "lpt_lazy_load", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("lpt_mongo")) {
                PopDBRequiredIOT.insertUdfField("lpt_mongo", "lpt_mongo", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("lpt_native")) {
                PopDBRequiredIOT.insertUdfField("lpt_native", "lpt_native", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("lpt_new")) {
                PopDBRequiredIOT.insertUdfField("lpt_new", "lpt_new", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("lpt_total")) {
                PopDBRequiredIOT.insertUdfField("lpt_total", "lpt_total", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("new_things")) {
                PopDBRequiredIOT.insertUdfField("new_things", "new_things", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("source")) {
                PopDBRequiredIOT.insertUdfField("source", "source", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("things")) {
                PopDBRequiredIOT.insertUdfField("things", "things", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("tsCoreIn")) {
                PopDBRequiredIOT.insertUdfField("tsCoreIn", "tsCoreIn", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("uptime")) {
                PopDBRequiredIOT.insertUdfField("uptime", "uptime", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
        }
        removeThingTypeFieldTemplate(thingTypeFieldTemplateRemove,thingTypeTemplate);
        try {
            thingTypeTemplate.setPathIcon("sprite template icon-corebridge");
            updateThingTypeByFieldName(thingTypeTemplate, labels);
        } catch (Exception e) {
            logger.error("Error in update CoreBridge ThingType with new Fields");
        }
    }

    public void insertEdgeBridgeNewUdfs(ThingTypeTemplate thingTypeTemplate) {
        Set<ThingTypeFieldTemplate> fields = thingTypeTemplate.getThingTypeFieldTemplate() != null ?
                thingTypeTemplate.getThingTypeFieldTemplate() : new HashSet<ThingTypeFieldTemplate>();
        Set<String> labels = new HashSet<>();
        ThingTypeFieldTemplate thingTypeFieldTemplateRemove = null;
        for (ThingTypeFieldTemplate field : fields) {
            labels.add(field.getName());
            if ("sqn".equals(field.getName())) {
                thingTypeFieldTemplateRemove = field;
            }
        }
        if (!labels.isEmpty()){
            if (!labels.contains("age")) {
                PopDBRequiredIOT.insertUdfField("age", "age", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("lastDetects")) {
                PopDBRequiredIOT.insertUdfField("lastDetects", "lastDetects", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("lastLocates")) {
                PopDBRequiredIOT.insertUdfField("lastLocates", "lastLocates", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("lpt")) {
                PopDBRequiredIOT.insertUdfField("lpt", "lpt", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("new_things")) {
                PopDBRequiredIOT.insertUdfField("new_things", "new_things", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("socketTimeoutCount")) {
                PopDBRequiredIOT.insertUdfField("socketTimeoutCount", "socketTimeoutCount", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("socketTimeouts")) {
                PopDBRequiredIOT.insertUdfField("socketTimeouts", "socketTimeouts", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("source")) {
                PopDBRequiredIOT.insertUdfField("source", "source", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("things")) {
                PopDBRequiredIOT.insertUdfField("things", "things", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("tsEdgeIn")) {
                PopDBRequiredIOT.insertUdfField("tsEdgeIn", "tsEdgeIn", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (!labels.contains("uptime")) {
                PopDBRequiredIOT.insertUdfField("uptime", "uptime", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_NUMBER.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
        }
        removeThingTypeFieldTemplate(thingTypeFieldTemplateRemove, thingTypeTemplate);
        try {
            thingTypeTemplate.setPathIcon("sprite template icon-edgebridge");
            updateThingTypeByFieldName(thingTypeTemplate, labels);
        } catch (Exception e) {
            logger.error("Error in update EdgeBridge ThingType with new Fields");
        }
    }

    private static void removeThingTypeFieldTemplate(ThingTypeFieldTemplate thingTypeFieldTemplateRemove, ThingTypeTemplate thingTypeTemplate){
        if (thingTypeFieldTemplateRemove != null) {
            List<ThingTypeField> thingTypeFieldByIdThingList = ThingTypeFieldService.getInstance().getThingTypeFieldByIdThing(thingTypeFieldTemplateRemove.getId());
            for (ThingTypeField thingTypeField : thingTypeFieldByIdThingList) {
                thingTypeField.setThingTypeFieldTemplateId(null);
                ThingTypeFieldService.getInstance().update(thingTypeField);
            }
            thingTypeTemplate.getThingTypeFieldTemplate().remove(thingTypeFieldTemplateRemove);
            ThingTypeFieldTemplateService.getInstance().delete(thingTypeFieldTemplateRemove);
        }
    }

    /**
     * Insert new udf's FlexTag
     * @param thingTypeTemplate thing Type Template
     */
    public static void insertFlexTagNewUdfs(ThingTypeTemplate thingTypeTemplate, Set<String> labels){
        if ((labels == null) || (!labels.contains("source"))) {
            PopDBRequiredIOT.insertUdfField("source", "source", "", "",
                    DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), "DATA_TYPE", false, thingTypeTemplate, "");
        }
        if ((labels == null) || (!labels.contains("tsCoreIn"))) {
            PopDBRequiredIOT.insertUdfField("tsCoreIn", "tsCoreIn", "", "",
                    DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), "DATA_TYPE", false, thingTypeTemplate, "");
        }
        if ((labels == null) || (!labels.contains("tsEdgeIn"))) {
            PopDBRequiredIOT.insertUdfField("tsEdgeIn", "tsEdgeIn", "", "",
                    DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), "DATA_TYPE", false, thingTypeTemplate, "");
        }
        if ((labels == null) || (!labels.contains("doorEvent"))) {
            PopDBRequiredIOT.insertUdfField("doorEvent", "doorEvent", "", "",
                    DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), "DATA_TYPE", false, thingTypeTemplate, "");
        }
        if ((labels == null) || (!labels.contains("lastLocateTime"))) {
            PopDBRequiredIOT.insertUdfField("lastLocateTime", "lastLocateTime", "", "",
                    DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), "DATA_TYPE", false, thingTypeTemplate, "");
        }
        ThingTypeFieldTemplate thingTypeFieldTemplate =
                ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplate.getId(), "logicalReader");
        if (thingTypeFieldTemplate != null) {
            thingTypeFieldTemplate.setTimeSeries(true);
            ThingTypeFieldTemplateService.getInstance().update(thingTypeFieldTemplate);
        }
        ThingTypeFieldTemplate thingTypeFieldTemplateZone =
                ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplate.getId(), "zone");
        if (thingTypeFieldTemplateZone != null) {
            thingTypeFieldTemplateZone.setTimeSeries(true);
            ThingTypeFieldTemplateService.getInstance().update(thingTypeFieldTemplateZone);
        }
    }

    public void insertThingTypeFieldsRFIDPrinter(List<ThingType> thingTypes, Long  thingTypeTemplateId){
        for (ThingType thingType:thingTypes){
            Set<ThingTypeField> fields = thingType.getThingTypeFields()!=null?thingType.getThingTypeFields():new HashSet<ThingTypeField>();
            Set<String> lstNames = new HashSet<>();
            for (ThingTypeField configThingtype: fields) {
                lstNames.add(configThingtype.getName());
            }
            if ( (!lstNames.isEmpty()) && (!lstNames.contains("zone")) ) {
                ThingTypeFieldTemplate thingTypeFieldTemplate =
                        ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplateId, "zone");
                ThingTypeField thingTypeField = insertThingTypeField("zone", null, thingTypeFieldTemplate.getId(), "DATA_TYPE",
                        DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_ZONE.value), thingType,"", "", true, "");
                fields.add(thingTypeField);
                thingType.setThingTypeFields(fields);
            }
        }
    }
    public void insertThingTypeFieldsLogicalReader(List<ThingType> thingTypes, Long  thingTypeTemplateId){
        for (ThingType thingType : thingTypes) {
            Set<ThingTypeField> fields = thingType.getThingTypeFields()!=null?thingType.getThingTypeFields():new HashSet<ThingTypeField>();
            Set<String> lstNames = new HashSet<>();
            for (ThingTypeField configThingtype : fields) {
                lstNames.add(configThingtype.getName());
            }
            if ((!lstNames.isEmpty()) && (!lstNames.contains("zoneIn"))) {
                ThingTypeFieldTemplate thingTypeFieldTemplate =
                        ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplateId, "zoneIn");
                fields.add(insertThingTypeField("zoneIn", null, thingTypeFieldTemplate.getId(), "DATA_TYPE",
                        DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_ZONE.value), thingType, "", "", true, ""));
                thingType.setThingTypeFields(fields);
            }
        }
    }
    public void insertThingTypeFieldsRFIDTag(List<ThingType> thingTypes, Long  thingTypeTemplateId){
        for (ThingType thingType:thingTypes){
            Set<ThingTypeField> fields = thingType.getThingTypeFields()!=null?thingType.getThingTypeFields():new HashSet<ThingTypeField>();
            Set<String> lstNames = new HashSet<>();
            for (ThingTypeField configThingtype : fields) {
                lstNames.add(configThingtype.getName());
            }
            createMandatoryValuesZone(thingTypeTemplateId, thingType, fields,lstNames);
            if (!fields.isEmpty()) {
                thingType.setThingTypeFields(fields);
            }
        }
    }

    public void insertThingTypeFieldsGPSTag(List<ThingType> thingTypes, Long  thingTypeTemplateId){
        for (ThingType thingType:thingTypes){
            Set<ThingTypeField> fields = thingType.getThingTypeFields()!=null?
                    thingType.getThingTypeFields():new HashSet<ThingTypeField>();
            Set<String> lstNames = new HashSet<>();
            for (ThingTypeField configThingtype : fields) {
                lstNames.add(configThingtype.getName());
            }
            createMandatoryValuesZone(thingTypeTemplateId, thingType,fields,lstNames);
            ThingTypeFieldTemplate thingTypeFieldTemplate = null;
            if ((!lstNames.isEmpty()) && (!lstNames.contains("zone"))) {
                thingTypeFieldTemplate = ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplateId, "zone");
                fields.add(insertThingTypeField("zone", null, thingTypeFieldTemplate.getId(), "DATA_TYPE",
                        DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_ZONE.value), thingType,"", "", true, ""));
            }
            if ((!lstNames.isEmpty()) && (!lstNames.contains("lastDetectTime"))) {
                thingTypeFieldTemplate = ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplateId, "lastDetectTime");
                fields.add(insertThingTypeField("lastDetectTime", null, thingTypeFieldTemplate.getId(), "DATA_TYPE",
                        DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), thingType,"", "", false, ""));
            }
            if ((!lstNames.isEmpty()) && (!lstNames.contains("lastLocateTime"))) {
                thingTypeFieldTemplate = ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplateId, "lastLocateTime");
                fields.add(insertThingTypeField("lastLocateTime", null, thingTypeFieldTemplate.getId(), "DATA_TYPE",
                        DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), thingType, "", "", false, ""));
            }
            if(!fields.isEmpty()) {
                thingType.setThingTypeFields(fields);
            }
        }
    }

    public void insertThingTypeFieldsFlexTag(List<ThingType> thingTypes, Long  thingTypeTemplateId){
        for (ThingType thingType:thingTypes){
            Set<ThingTypeField> fields = thingType.getThingTypeFields()!=null?
                    thingType.getThingTypeFields():new HashSet<ThingTypeField>();
            Set<String> lstNames = new HashSet<>();
            for (ThingTypeField configThingtype : fields) {
                lstNames.add(configThingtype.getName());
            }
            ThingTypeFieldTemplate thingTypeFieldTemplate = null;
            if ((!lstNames.isEmpty()) && (!lstNames.contains("source"))) {
                thingTypeFieldTemplate = ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplateId, "source");
                fields.add(insertThingTypeField("source", null, thingTypeFieldTemplate.getId(), "DATA_TYPE",
                        DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), thingType,"", "", false, ""));
            }
            if ((!lstNames.isEmpty()) && (!lstNames.contains("tsCoreIn"))) {
                thingTypeFieldTemplate = ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplateId, "tsCoreIn");
                fields.add(insertThingTypeField("tsCoreIn", null, thingTypeFieldTemplate.getId(), "DATA_TYPE",
                        DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), thingType, "", "", false, ""));
            }
            if ((!lstNames.isEmpty()) && (!lstNames.contains("tsEdgeIn"))) {
                thingTypeFieldTemplate = ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplateId, "tsEdgeIn");
                fields.add(insertThingTypeField("tsEdgeIn", null, thingTypeFieldTemplate.getId(), "DATA_TYPE",
                        DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), thingType,"", "", false, ""));
            }
            if ((!lstNames.isEmpty()) && (!lstNames.contains("doorEvent"))) {
                thingTypeFieldTemplate = ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplateId, "doorEvent");
                fields.add(insertThingTypeField("doorEvent", null, thingTypeFieldTemplate.getId(), "DATA_TYPE",
                        DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), thingType,"", "", false, ""));
            }
            if ((!lstNames.isEmpty()) && (!lstNames.contains("lastLocateTime"))) {
                thingTypeFieldTemplate = ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplateId, "lastLocateTime");
                fields.add(insertThingTypeField("lastLocateTime", null, thingTypeFieldTemplate.getId(), "DATA_TYPE",
                        DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), thingType,"", "", false, ""));
            }
            if(!fields.isEmpty()) {
                thingType.setThingTypeFields(fields);
            }
            ThingTypeField thingTypeFieldLogical = ThingTypeFieldService.getInstance().getThingTypeFielByThingType(thingType.getId(),"logicalReader");
            if ( thingTypeFieldLogical != null ) {
                thingTypeFieldLogical.setTimeSeries(true);
                thingTypeFieldLogical = ThingTypeFieldService.getInstance().update(thingTypeFieldLogical);
                thingType.getThingTypeFields().add(thingTypeFieldLogical);
            }
            ThingTypeField thingTypeFieldZone = ThingTypeFieldService.getInstance().getThingTypeFielByThingType(thingType.getId(),"zone");
            if ( thingTypeFieldZone != null ) {
                thingTypeFieldZone.setTimeSeries(true);
                thingTypeFieldZone = ThingTypeFieldService.getInstance().update(thingTypeFieldZone);
                thingType.getThingTypeFields().add(thingTypeFieldZone);
            }
        }
    }

    /**
     *
     * @param thingTypeTemplateId thing Type Field Template's thingTypeTemplateId
     * @param thingType thing Type Field Template's thingType
     */
    public static void createMandatoryValuesZone (Long thingTypeTemplateId, ThingType thingType,
                                                  Set<ThingTypeField> lstThingTypeField,Set<String> lstNames  ){
        ThingTypeFieldTemplate thingTypeFieldTemplate = null;
        if ((!lstNames.isEmpty()) && (!lstNames.contains("source"))) {
            thingTypeFieldTemplate = ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(
                    thingTypeTemplateId, "source");
            lstThingTypeField.add(insertThingTypeField("source", null, thingTypeFieldTemplate.getId(), "DATA_TYPE",
                    DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), thingType,"", "", false, ""));
        }
        if ((!lstNames.isEmpty()) && (!lstNames.contains("logicalReader"))) {
            thingTypeFieldTemplate = ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplateId, "logicalReader");
            lstThingTypeField.add(insertThingTypeField("logicalReader", null, thingTypeFieldTemplate.getId(), "DATA_TYPE",
                    DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_LOGICAL_READER.value), thingType,"", "", true, ""));
        }
        if ((!lstNames.isEmpty()) && (!lstNames.contains("tsCoreIn"))) {
            thingTypeFieldTemplate = ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplateId, "tsCoreIn");
            lstThingTypeField.add(insertThingTypeField("tsCoreIn", null, thingTypeFieldTemplate.getId(), "DATA_TYPE",
                    DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), thingType,"", "", false, ""));
        }
        if ((!lstNames.isEmpty()) && (!lstNames.contains("tsEdgeIn"))) {
            thingTypeFieldTemplate = ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplateId, "tsEdgeIn");
            lstThingTypeField.add(insertThingTypeField("tsEdgeIn", null, thingTypeFieldTemplate.getId(), "DATA_TYPE",
                    DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), thingType,"", "", false, ""));
        }
        if ((!lstNames.isEmpty()) && (!lstNames.contains("doorEvent"))) {
            thingTypeFieldTemplate = ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplateId, "doorEvent");
            lstThingTypeField.add(insertThingTypeField("doorEvent", null, thingTypeFieldTemplate.getId(), "DATA_TYPE",
                    DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TEXT.value), thingType,"", "", false, ""));
        }
    }

    /**
     * Insert a new Thing Type Field
     * @param name Thing Type field's name
     * @param dataTypeThingTypeId Thing Type field's dataTypeThingTypeId
     * @param thingTypeFieldTemplateId Thing Type field's thing Type Field TemplateId
     * @param typeParent Thing Type field's typeParent
     * @param dataType Thing Type field's dataType
     * @param thingType Thing Type field's thingType
     * @param unit Thing Type field's unit
     * @param symbol Thing Type field's symbol
     * @param timeSeries Thing Type field's timeSeries
     * @param defaultValue Thing Type field's default value
     */
    public static ThingTypeField insertThingTypeField(String name, Long dataTypeThingTypeId, Long thingTypeFieldTemplateId,
                                                      String typeParent, DataType dataType, ThingType thingType, String unit,
                                                      String symbol, Boolean timeSeries, String defaultValue){
        ThingTypeField field = new ThingTypeField();
        field.setName(name);
        field.setDataTypeThingTypeId(dataTypeThingTypeId);
        field.setThingTypeFieldTemplateId(thingTypeFieldTemplateId);
        field.setTypeParent(typeParent);
        field.setDataType(dataType);
        field.setThingType(thingType);
        field.setUnit(unit);
        field.setSymbol(symbol);
        field.setTimeSeries(timeSeries);
        field.setDefaultValue(defaultValue);
        ThingTypeField thingTypeField = ThingTypeFieldService.getInstance().insert(field);
        return thingTypeField;
    }

    /**
     * Insert Field into apc_field table
     * @param description field's description
     * @param editLevel field's edit Level
     * @param module field's module
     * @param name field's name
     * @param type field's type
     * @param userEditable field's user Editable
     * @param group field's group
     */
    public static void insertField(String description, Long editLevel, String module, String name, String type,
                                   Boolean userEditable, Group group, String quantity ){
        Field data = FieldService.getInstance().selectByName(name);
        if (data == null) {
            Field field = new Field();
            field.setDescription(description);
            field.setEditLevel(editLevel);
            field.setModule(module);
            field.setName(name);
            field.setType(type);
            field.setUserEditable(userEditable);
            field.setGroup(group);
            field = FieldService.getInstance().insert(field);
            insertGroupField(field, group,quantity);
        }
    }


    /**
     * Insert Group Field into groupfield table
     * @param field
     * @param group
     * @param value
     */
    public static void insertGroupField(Field field, Group group, String value){
        GroupField groupField = new GroupField();
        groupField.setField(field);
        groupField.setGroup(group);
        groupField.setValue(value);
        GroupFieldService.getInstance().insert(groupField);
    }
    /**
     *
     * @param group group
     */
    public static void insertCoreBridgeTemplate4_3RC12(Group group){
        ThingTypeTemplate thingTypeTemplateHead = PopDBRequiredIOT.insertThingTypeTemplateHead("CoreBridge", "CoreBridge", group, "sprite template icon-corebridge", true);
        PopDBRequiredIOT.insertBasicCoreBridgeTemplate(thingTypeTemplateHead);
    }

    /**
     * Populate RFID printer
     * @param thingTypeTemplate
     * @param labels
     */
    public void insertRFIDPrinterNewUdfs(ThingTypeTemplate thingTypeTemplate, Set<String> labels){
        if ((labels == null) || (!labels.isEmpty() && !labels.contains("zone"))){
            PopDBRequiredIOT.insertUdfField("zone", "zone", "", "", DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_ZONE.value), "DATA_TYPE", true, thingTypeTemplate, "");
        }
        if ((labels == null) || (!labels.isEmpty() && !labels.contains("zpl"))){
            try{
                ThingType thingTypeZpl = ThingTypeService.getInstance().getByCode("default_zpl_thingtype");
                if(thingTypeZpl != null){
                    PopDBRequiredIOT.addThingTypeTemplateZPL(thingTypeTemplate, thingTypeZpl);
                } else {
                    String level = ConfigurationService.getAsString(GroupService.getInstance().getRootGroup(), "thingType");
                    Group groupThingType = null;
                    if(level != null){
                        List<Group> lstGroup = GroupService.getInstance().getByLevel(Integer.parseInt(level));
                        if( (lstGroup != null) && (!lstGroup.isEmpty()) ) {
                            groupThingType = lstGroup.get(0);
                        } else {
                            groupThingType = GroupService.getInstance().getRootGroup();
                        }
                    } else {
                        groupThingType = GroupService.getInstance().getRootGroup();
                    }

                    ThingType zpl = PopDBIOTUtils.popThingTypeZPL(groupThingType, "default_zpl_thingtype");
                    PopDBRequiredIOT.addThingTypeTemplateZPL(ThingTypeTemplateService.getInstance().get(2L), zpl);
                }
            }catch(NonUniqueResultException ex) {
                logger.info("There is not a default ZPL Thing Type instantiated.");
            }
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
