package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.api.database.base.alias.Alias;
import com.tierconnect.riot.api.database.mongo.Mongo;
import com.tierconnect.riot.api.mongoShell.query.ResultFormat;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.entities.ZonePropertyValue;

import com.tierconnect.riot.iot.reports.autoindex.services.ReportLogMongoService;
import com.tierconnect.riot.iot.services.ZonePropertyValueService;
import com.tierconnect.riot.iot.services.ZoneService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : rchirinos
 * @date : 12/2/16 10:04 AM
 *
 */
public class ReportExport {
    //TODO: this variable is never used.
    private static Logger logger = Logger.getLogger(ReportExport.class);
    private ReportConfig reportConfig = null;
    private Date now;
    private String comment = "";
    private final boolean isSentEmail;
    private String aliasFunctionTemplate = "alias[\"%1s\"][\"function\"]";
    private boolean addColmunsDateFormat = false;
    private final String reportIndex;

     ReportExport(ReportConfig reportConfig, String comment, Date now, boolean isSentEmail, boolean addColmunsDateFormat) {
        this.reportConfig = reportConfig;
        this.comment = comment;
        this.now = now;
        this.isSentEmail = isSentEmail;
        this.addColmunsDateFormat = addColmunsDateFormat;
        this.reportIndex = ReportLogMongoService.getInstance().getIndex(reportConfig.getReportDefinition().getId(),
                reportConfig.getCollectionTarget(), reportConfig.getConfigDefinition());
         if(StringUtils.isNotEmpty(this.reportIndex)) {
             logger.info("Index for report '" + reportConfig.getReportDefinition().getName() +
                     "' is running with index: " + this.reportIndex);
         } else {
             logger.info("Index for report '" + reportConfig.getReportDefinition().getName() +
                     "' is running without index");
         }
    }


    /**
     * Export Results of the report
     *
     * @return A file
     * @throws Exception If it is not possible to create the file.
     */
    public File export() throws Exception {
        reportConfig.processConfiguration(now);
        Mongo m = new Mongo(reportConfig.getFilters());
        List<Alias> projection = reportConfig.getProjection();
        m.setAliasList(projection);
        StringBuffer script = generateScript(m.getConditionBuilderString(), m.getProjectionString(), m.getAliasListString());
        String tmpExport = m.export(script.toString(),
                Constants.TEMP_NAME_REPORT + reportConfig.reportDefinition.getId()+"_", ResultFormat.CSV_SCRIPT);
        return new File(tmpExport);
    }

    /**
     * Method to get the Script for export
     *
     * @param query A {@link String} containing the query to run.
     * @param projection A {@link String} containing the columns to display in the report.
     * @return a {@link StringBuffer} instance.
     */
    private StringBuffer generateScript(String query, String projection, String aliasString) {
        ReportJSFunction f = new ReportJSFunction(reportConfig.reportDefinition, isSentEmail);
        StringBuffer buffer = new StringBuffer("");
        buffer.append("var loadMoment = load(\""+ReportExport.class.getClassLoader().getResource("libraries").getPath()+"/moment.min.js\");\n");
        buffer.append("var loadMoment = load(\""+ReportExport.class.getClassLoader().getResource("libraries").getPath()+"/moment-timezone.min.js\");\n");

        buffer.append(f.lstJSFunction.get(ReportJSFunction.FORMAT_VALUE));
        buffer.append(f.lstJSFunction.get(ReportJSFunction.FORMAT_DWELLTIME));
        buffer.append(f.lstJSFunction.get(ReportJSFunction.FORMAT_DATE));
        buffer.append(f.lstJSFunction.get(ReportJSFunction.VALUE_BY_PATH));
        JSONObject jsonObject = new JSONObject();
        if (reportConfig.paths != null) {
            jsonObject.putAll(reportConfig.paths);
            buffer.append("    var paths = " + jsonObject.toJSONString().replaceAll(",", ",\n") + ";\n");
        } else {
            buffer.append("    var paths = {};\n");
        }
        buffer.append("    var currentTime = new Date().getTime();\n");
        buffer.append("    var zones = " + getZoneRelZoneTypes() + ";\n");
        buffer.append("    var zoneProperties = " + getZoneProperties() + ";\n");
        buffer.append("    var alias = " + aliasString + ";\n");
        buffer.append("    print(\"" + getColumnNames() + "\");\n");
        buffer.append("    db.getMongo().setReadPref(\"secondary\");\n");
        buffer.append("    db.getCollection('" + reportConfig.getCollectionTarget() + "').find(").append(query).append(",").append(projection);
        buffer.append("    ).comment(\""+comment+"\")" + getHintAsNeeded() + getSortFunctionAsNeeded());
        buffer.append("    .forEach(function(row){ \n");
        buffer.append("    var rowItem = [];\n");
        for (PropertyReport property : reportConfig.getPropertyReportList()) {
            String fieldProperty = reportConfig.verifiedSnapshotsProperty(property.getProperty());
            String thingTypeId = reportConfig.verifiedSnapshotsProperty("thingTypeId");
            String propertyValue = "getValueByPath(row,\"" + fieldProperty + "\")";
            String zonePropertyValue = "getValueByPath(row,\"" + fieldProperty + "\")+\"-\"+\""
                    +getKeyZoneProperty(property.getPropertyOriginal()+"\"");
            if (property.getThingType() != null) {
                fieldProperty = property.getProperty();
                String keyPath = "getValueByPath(row,\""+thingTypeId+"\")+\"-\"+\"" + property.getThingType().getId() + "\"";
                String chooseKeyPath = (reportConfig.isHistoricalReport())?"\"value.\"+paths[" + keyPath + "]+\".\"+\"":"paths[" + keyPath + "]+\".\"+\"";
                String propertyValuePath = "getValueByPath(row,"+ chooseKeyPath + fieldProperty + "\")";
                String zonePropertyValuePath = "getValueByPath(row," + chooseKeyPath + fieldProperty + "\")+\"-\"+"
                        +getKeyZoneProperty(property.getPropertyOriginal()+"\n");
                String aliasFunction = "alias[\"" + reportConfig.verifiedSnapshotsProperty(fieldProperty) + "\"][\"function\"]";

                //Script
                buffer.append("  if (getValueByPath(row,\""+thingTypeId+"\") == "+ property.getThingType().getId() +" || "+property.getThingType().getId()+" == 0 ){\n");
                buffer.append(getValidationOfNull(property, aliasFunction, propertyValue, zonePropertyValue));
                buffer.append("  } else {\n");
                buffer.append("        if (paths.hasOwnProperty(" + keyPath + ")){\n");
                buffer.append(getValidationOfNull(property, aliasFunction, propertyValuePath, zonePropertyValuePath));
                buffer.append("        } else {\n");
                buffer.append("               rowItem.push(\"\");\n");
                buffer.append("        }\n");
                buffer.append("  }\n");
            } else {
                //Script
                if(!property.getSubDataTypeZone().equals(Long.parseLong("0")) &&
                        property.getPropertyOriginal().contains(Constants.ZONE_PROPERTY_ID)) {
                    buffer.append(setValidaNullValueArray("zoneProperties",zonePropertyValue));
                } else if(!property.getSubDataTypeZone().equals(Long.parseLong("0")) &&
                        property.getPropertyOriginal().contains(Constants.ZONE_TYPE_NAME)) {
                    buffer.append(setValidaNullValueArray("zones", propertyValue));
                } else if(property.getPropertyOriginal().contains(Constants.DWELLTIME)) {
                    buffer.append(setValidaNullValueDwellTime(propertyValue));
                } else if(validateTimeStampProperty(property) ){
                    buffer.append(setValidaNullValue(String.format(aliasFunctionTemplate, fieldProperty), propertyValue, property));
                } else {
                    buffer.append(setValidaNullValue(null, propertyValue, property));
                }
            }
        }

        buffer.append("      print(rowItem.join(\",\"));\n");
        buffer.append("});\n");
        return buffer;
    }

    //TODO: Move this method to another class.
    private boolean validateTimeStampProperty(PropertyReport property){
        ThingTypeField thingTypeField = property.getThingTypeField();
        return (thingTypeField != null &&
                (isIdPropertyReportDate(thingTypeField.getDataType().getId())))
                || isIdPropertyReportDate(property.getDataType());
    }

    //TODO: Move this method to another class.
    private boolean isIdPropertyReportDate(Long id) {
        if (id == null)
            return false;
        return id.equals(ThingTypeField.Type.TYPE_TIMESTAMP.value)
                || id.equals(ThingTypeField.Type.TYPE_DATE.value);
    }

    //TODO: Move this method to another class.
    public StringBuffer getValidationOfNull(
            PropertyReport propertyReport, String aliasFunction, String propertyValue, String zonePropertyValue){
        StringBuffer buffer = new StringBuffer("");
        if(!propertyReport.getSubDataTypeZone().equals(Long.parseLong("0")) &&
                propertyReport.getPropertyOriginal().contains(Constants.ZONE_PROPERTY_ID)) {
            buffer.append(setValidaNullValueArray("zoneProperties",zonePropertyValue));
        } else if(!propertyReport.getSubDataTypeZone().equals(Long.parseLong("0")) &&
                propertyReport.getPropertyOriginal().contains(Constants.ZONE_TYPE_NAME)) {
            buffer.append(setValidaNullValueArray("zones",propertyValue));
        } else if(propertyReport.getPropertyOriginal().contains(Constants.DWELLTIME)) {
            buffer.append(setValidaNullValueDwellTime(propertyValue));
        } else {
            buffer.append(setValidaNullValue(aliasFunction, propertyValue, propertyReport));
        }
        return buffer;
    }
    /**
     * Validate & Get Values
     * @param function
     * @param property
     * @return
     */
    public StringBuffer setValidaNullValue(String function, String property, PropertyReport propertyReport) {
        StringBuffer buffer = new StringBuffer("");
        buffer.append("            if(" + verifyNotNullProperty(property) + ") {\n");
        if (function != null) {
            buffer.append("               rowItem.push(formatValue(" + function + ", " + property + "));\n");
            if (propertyReport.isTimestampOrDate() && addColmunsDateFormat) {
                buffer.append("               rowItem.push(formatValue(\"formatISODate\", ").append(property).append("));\n");
                buffer.append("               rowItem.push(formatValue(\"formatTimestamp\", ").append(property).append("));\n");
            }
        } else {
            buffer.append("               rowItem.push(" + property + "+\"\");\n");
        }
        buffer.append("            } else{ \n");
        buffer.append("               rowItem.push(\"\");\n");
        if (propertyReport.isTimestampOrDate() && addColmunsDateFormat) {
            buffer.append("               rowItem.push(\"\");\n");
            buffer.append("               rowItem.push(\"\");\n");
        }
        buffer.append("            } \n");
        return buffer;
    }

    /**
     * VAlidate & Get values inside of an array
     * @param arrayValues
     * @param property
     * @return
     */
    public StringBuffer setValidaNullValueArray(String arrayValues, String property){
        StringBuffer buffer = new StringBuffer("");
        buffer.append("           if("+verifyNotNullProperty(property)+") {\n");
        buffer.append("              if ("+arrayValues+".hasOwnProperty("+ property +"+\"\")){\n");
        buffer.append("                  rowItem.push("+arrayValues+"["+property+"+\"\"]);\n");
        buffer.append("              } else {\n");
        buffer.append("                  rowItem.push(\"\");\n");
        buffer.append("              }\n");
        buffer.append("            } else{ \n" );
        buffer.append("               rowItem.push(\"\");\n" );
        buffer.append("            } \n");
        return buffer;
    }

    /**
     * Validate & Get Values for Dwell Time
     * @param property
     * @return
     */
    public StringBuffer setValidaNullValueDwellTime(String property) {
        StringBuffer buffer = new StringBuffer("");
        buffer.append("            if("+verifyNotNullProperty(property)+") {\n");
        if(this.reportConfig.isHistoricalReport()) {
            buffer.append("               rowItem.push( formatValue(\"formatDwellTime\", " + property + "+\"\"));\n");
        } else {
            buffer.append("               rowItem.push( formatValue(\"formatDwellTime\", currentTime - " + property + "+\"\"));\n");
        }
        buffer.append("            } else{ \n");
        buffer.append("               rowItem.push(\"\");\n");
        buffer.append("            } \n");
        return buffer;
    }

    private String verifyNotNullProperty(String property){
        return "("+ property +" !== null ) && ( (" + property + ")!== 'undefined') ";
    }

    /**
     * Get column names
     *
     * @return
     */
    public String getColumnNames() {
        List<String> columnNames = new ArrayList<>();
        List<PropertyReport> lstPropertyReport = reportConfig.getPropertyReportList();
        if( (lstPropertyReport != null) && (!lstPropertyReport.isEmpty())) {
            for (PropertyReport property : lstPropertyReport) {
                columnNames.add(property.getLabel());
                if (property.isTimestampOrDate() && addColmunsDateFormat) {
                    columnNames.add(property.getLabel() + "(ISO)");
                    columnNames.add(property.getLabel() + "(TS)");
                }
            }
        }
        return String.join(",", columnNames);
    }

    /**
     * Get all zone properties
     * @return JSON String > Key:[Zone Id]-[Zone Property Id], Value:[Zone Property Value]
     */
    private String getZoneProperties(){
        JSONObject jsonObject = new JSONObject();
        List<ZonePropertyValue> lstZonePropertyValue = ZonePropertyValueService.getInstance().listPaginated(null, null, null);
        if( (lstZonePropertyValue != null) && (!lstZonePropertyValue.isEmpty()) ){
            for(ZonePropertyValue zoneProperty : lstZonePropertyValue ) {
                jsonObject.put(zoneProperty.getZoneId()+"-"+zoneProperty.getZonePropertyId(),zoneProperty.getValue() );
            }
        }
        return jsonObject.toJSONString().replaceAll(",", ",\n");
    }

    /**
     * Get all Zones and their relations with zoneTypes
     * @return JSON String > [ZoneId]-[ZoneTypeId], [Zone Type Name]
     */
    private String getZoneRelZoneTypes(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(0,"Unknown" );
        List<Zone> lstZone= ZoneService.getInstance().listPaginated(null, null, null);
        if( (lstZone != null) && (!lstZone.isEmpty()) ){
            for(Zone zone: lstZone) {
                jsonObject.put(zone.getId(),zone.getZoneType().getName() );
            }
        }
        return jsonObject.toJSONString().replaceAll(",", ",\n");
    }

    /**
     * Get ID of zone property
     * @param propertyOriginal
     * @return
     */
    private String getKeyZoneProperty(String propertyOriginal){
        String[] data = propertyOriginal.split(",");
        return (data.length > 1  ? data[1] : "");
    }

    private String getSortFunctionAsNeeded(){
        String result = "";
        if(reportConfig instanceof  TableHistoryReportConfig){
            result = ".sort({'time' : -1})";
        }
        return result;
    }

    private String getHintAsNeeded(){
        String result = "";
        if(!StringUtils.isEmpty(reportIndex)){
            result = ".hint(\"" + reportIndex + "\")";
        }
        return result;
    }
}
