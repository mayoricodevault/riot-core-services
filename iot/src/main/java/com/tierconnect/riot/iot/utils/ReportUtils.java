package com.tierconnect.riot.iot.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.path.EntityPathBase;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.utils.QueryUtils;

import com.tierconnect.riot.iot.controllers.IOTController;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.FileExportService;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ZonePropertyValueService;
import com.tierconnect.riot.iot.services.ZoneService;
import com.tierconnect.riot.sdk.dao.Pagination;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

/*TODO: You will be changed this Class name to another name.Because used in thing Controller for print a thing in
json format.*/

public class ReportUtils {
    private static Logger logger = Logger.getLogger(ReportUtils.class);

    private static final String[] VALID_FILTER_KEYS = new String[]{
            "name", "serial", "group.id", "thingType.id", "group.groupType.id", "parent.name", "parent.serial"
    };

    private static final String[] THING_EXTERNAL_FIELDS_KEYS = new String[]{
            "zoneGroup.id", "localMap.id", "zoneProperty.id", "shift.id", "zoneType.id", "zoneCode.name", "zone.name"
    };


    /**
     * run report the time series report
     **/

    public static Map<String, Object> executeTimeSeriesReport(ReportDefinition reportDefinition,
                                                              Map<String, Object> filters,
                                                              Date startDate,
                                                              Date endDate,
                                                              Integer pageSize,
                                                              Integer pageNumber) {

        List<Thing> preFilteredThings = preFilteredThings(generateWhere(reportDefinition, filters),
                pageNumber, pageSize, null, true);

        List<CompositeThing> compositeThingList = ReportExecutionUtils.getCompositeThings(preFilteredThings);
        Map<Long, Thing> childrenMap = CompositeThing.getChildrenMap(compositeThingList);

        String propertyName = "zone";
        for (ReportFilter reportFilter : reportDefinition.getReportFilter()) {
            if (reportFilter.getPropertyName().startsWith(ReportExecutionUtils.DWELL_TIME_PROPERTY_LABEL)) {
                propertyName = reportFilter.getValue();
                break;
            }
        }
        propertyName = ReportExecutionUtils.removeDwellTimeString(propertyName);

        if (filters.get("propertyName") != null) {
            propertyName = filters.get("propertyName").toString();
            filters.remove("propertyName");
        }

        List<Long> ids = getZoneLocationThingTypeFieldIds(compositeThingList, null, propertyName);
        //TODO FIX THIS METHOD OR DELETE IT
        //Map<Long, Map<String, Object>> fieldValues = FieldValueService.values(ids);
        Map<Long, Map<String, Object>> fieldValues = new HashMap<>();

        //get filtered things
        //Get Zones
        Map<Long, Zone> zoneListMap = ZoneService.getInstance().getZonesMap();
        Map<String, ZonePropertyValue> zonePropertyValueMap = ZonePropertyValueService.getInstance()
                .getValuesFromZones(reportDefinition, zoneListMap);

        //get filtered things
//        t1 = System.currentTimeMillis();
        List<CompositeThing> filteredThings = filteredThings(
                compositeThingList,
                fieldValues,
                reportDefinition,
                filters,
                null,
                zoneListMap,
                zonePropertyValueMap
        );

        ids = getZoneLocationThingTypeFieldIds(filteredThings, childrenMap, propertyName);

        //TODO FIX THIS METHOD OR DELETE IT
        //Map<Long, List<Object[]> > fieldValuesCassandra = FieldValueService.getFieldValuesHistory(ids, startDate,
        // endDate);
        Map<Long, List<Object[]>> fieldValuesCassandra = new HashMap<>();

        //Getting facilityMap to display the report
        Long localMapId;
        if (filters.get("localMapId") != null) {
            localMapId = Long.valueOf(filters.get("localMapId").toString());
            filters.remove("localMapId");
        } else {
            localMapId = reportDefinition.getLocalMapId() != null ? reportDefinition.getLocalMapId() : 0L;
        }


        List<Zone> zones = ZoneService.getZonesByLocalMap(localMapId);
        Map<String, List<Long>> zoneGroupMap = new HashMap<>();
        for (Zone zone : zones) {
            zoneGroupMap.put(zone.getName(), new LinkedList<Long>());
        }

        int numberOfIntervals = 10;
        Map<String, Object[]> reportRes = new HashMap<>();
        Long[] intervalsGenerated = new Long[numberOfIntervals + 1];
        List<String> categories = new LinkedList<>();

        //Divide the zoneGroup in intervals according to -> numberOfIntervals
        ReportTimeSeriesUtils.settingIntervalsChart(
                zoneGroupMap,
                fieldValuesCassandra,
                intervalsGenerated,
                categories,
                numberOfIntervals
        );

        Map<String, Integer> zoneCountTotal = new HashMap<>();

        //Count the people into intervals
        ReportTimeSeriesUtils.countingZonesInIntervals(
                reportRes,
                zoneCountTotal,
                zones,
                zoneGroupMap,
                intervalsGenerated,
                numberOfIntervals
        );

        //Building chart
        BuildChart buildChart = new BuildChart(reportDefinition.getName(), "#People");
        buildChart.setCategories(categories);

        String dataChartType = reportDefinition.getChartType() != null && reportDefinition.getChartType().length() > 0 ?
                reportDefinition.getChartType().toLowerCase()
                : "column";
        buildChart.addSeriesData(
                reportRes,
                dataChartType,
                numberOfIntervals,
                zones.size(),
                null
        );

        buildChart.addPieChart("total DwellTime in Zone", zoneCountTotal);
        return buildChart.getChartMap();
    }

    /**
     * run report with datapoints at a daterange
     **/

    public static String generateWhere(ReportDefinition reportDefinition, Map<String, Object> filters) {
        String where = "";
        boolean foundGroup = false;
        for (ReportFilter reportFilter : reportDefinition.getReportFilter()) {
            String value = reportFilter.getValue();
            //add valid properties to the where clause
            if (Arrays.asList(VALID_FILTER_KEYS).contains(reportFilter.getPropertyName())) {
                if (filters.containsKey(reportFilter.getId().toString())) {
                    value = filters.get(reportFilter.getId().toString()).toString();
                }
                if (StringUtils.isNotEmpty(value) || reportFilter.getPropertyName().equals("name") || reportFilter
                        .getPropertyName().equals("serial")) {
                    if (!where.equals("")) {
                        where = where + "&";
                    }
                    //change the property from group.id to group?
                    //todo analyze this
                    if ("group.id".equals(reportFilter.getPropertyName())) {
                        if (value != null && value.length() > 0 && ReportRuleUtils.isNumeric(value)) {
                            Map<String, Group> visibilityGroups = IOTController.getThingVisibilityGroups(Long.valueOf
                                    (value));
                            Group visibilityGroup = visibilityGroups.get("_anyThing");
                            where = where + "group<" + visibilityGroup.getId();
                            foundGroup = true;
                        }
                    } else {
                        if (reportFilter.getOperator().equals("~")) {
                            where = where + reportFilter.getPropertyName() + reportFilter.getOperator() + "%" + value
                                    + "%";
                        } else {
                            where = where + reportFilter.getPropertyName() + reportFilter.getOperator() + value;
                        }
                    }

                }
            }
        }
        //todo analyze this
        if (!foundGroup) {
            if (!where.equals("")) {
                where = where + "&";
            }
            Map<String, Group> visibilityGroups = IOTController.getThingVisibilityGroups(null);
            Group visibilityGroup = visibilityGroups.get("_anyThing");
            where = where + "group<" + visibilityGroup.getId();
        }
        return where;
    }


    //Data Points

    /**
     * return a pre filtered list of things
     **/
    public static List<Thing> preFilteredThings(String where,
                                                int pageNumber,
                                                int pageSize,
                                                String order,
                                                boolean qthingFields) {
        logger.info("Where " + where);
        where = where.replace("==", "=");
        Pagination pagination = new Pagination(pageNumber, pageSize);
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(
                QueryUtils.buildSearch(
                        QThing.thing,
                        where));

        //things that are straight from the thing service
        List<EntityPathBase<?>> properties = Arrays.asList(QThing.thing.parent, QThing.thing.thingType);

        List<Thing> thingList;
        //if(qthingFields) {
        //    thingList = ThingService.getInstance().listPaginated(be, pagination, "serial:asc", properties, QThing
        // .thing.thingFields);
        //}
        //else {
        thingList = ThingService.getInstance().listPaginated(be, pagination, "serial:asc", properties, null);
        //}
        return thingList;
    }


    /**
     * filters the given list of things by considering the filters that are UDFs, and whose values are in
     * the 'fieldValues' map.
     *
     * @param preFilterThings
     * @param fieldValues
     * @param reportDefinition
     * @param filters
     * @return
     */
    private static List<CompositeThing> filteredThings(List<CompositeThing> preFilterThings,
                                                       Map<Long, Map<String, Object>> fieldValues,
                                                       ReportDefinition reportDefinition,
                                                       Map<String, Object> filters,
                                                       Map<Long, List<Object[]>> fieldThingValues,
                                                       Map<Long, Zone> zoneListMap,
                                                       Map<String, ZonePropertyValue> zonePropertyValueMap
    ) {


        List<ReportFilter> fieldFilters = new LinkedList<>();
        List<ReportFilter> fieldExternalFilters = new LinkedList<>();

        //has filters
        boolean hasThingFieldFilter = false;
        boolean hasThingExternalFields = false;


        //Zones for ZoneGroup, ZonePropertiesBean

        Long forceThingTypeId = 0L;
        for (ReportFilter reportFilter : reportDefinition.getReportFilter()) {
            boolean includeEmptyOperator = false;
            if (reportFilter.getOperator().equals("isEmpty")) {
                includeEmptyOperator = true;
            }

            if (reportFilter.getPropertyName().equals("thingType.id") && reportFilter.getOperator().equals("==")) {
                String value = reportFilter.getValue();
                if (filters.containsKey(reportFilter.getId().toString())) {
                    value = filters.get(reportFilter.getId().toString()).toString();
                }
                if (value == null) value = "";
                forceThingTypeId = Long.valueOf(value.length() > 0 ? value : "0");

            }
            if (!Arrays.asList(VALID_FILTER_KEYS).contains(reportFilter.getPropertyName()) &&
                    !reportFilter.getPropertyName().contains(ReportExecutionUtils.DWELL_TIME_PROPERTY_LABEL) &&
                    !Arrays.asList(THING_EXTERNAL_FIELDS_KEYS).contains(reportFilter.getPropertyName())) {
                if (
                        reportFilter.getPropertyName().equals("startDate") ||
                                reportFilter.getPropertyName().equals("endDate") ||
                                reportFilter.getPropertyName().equals("last") ||
                                reportFilter.getPropertyName().equals("dwellTimeProperty") ||
                                reportFilter.getPropertyName().equals("relativeDate")
                        ) {
                    continue;
                }
                if (reportFilter.getPropertyName().startsWith(ReportExecutionUtils.TIME_STAMP_PROPERTY_LABEL)) {
                    hasThingFieldFilter = true;
                    fieldFilters.add(reportFilter);
                } else {
                    if (filters.containsKey(reportFilter.getId().toString())) {
                        if (StringUtils.isNotEmpty(filters.get(reportFilter.getId().toString()).toString()) ||
                                includeEmptyOperator) {
                            hasThingFieldFilter = true;
                            fieldFilters.add(reportFilter);
                        }
                    } else {
                        if (StringUtils.isNotEmpty(reportFilter.getValue()) || includeEmptyOperator) {
                            hasThingFieldFilter = true;
                            fieldFilters.add(reportFilter);
                        }
                    }
                }
            }
            if (Arrays.asList(THING_EXTERNAL_FIELDS_KEYS).contains(reportFilter.getPropertyName())) {
                String value = reportFilter.getValue();
                if (filters.containsKey(reportFilter.getId().toString())) {
                    value = filters.get(reportFilter.getId().toString()).toString();
                }

                if (!value.isEmpty()) {
                    hasThingExternalFields = true;
                    fieldExternalFilters.add(reportFilter);
                }
            }
        }

        List<CompositeThing> filteredThings = new ArrayList<>();

        for (CompositeThing compositeThing : preFilterThings) {

            Map<Long, Integer> timeStampValid = new HashMap<>();
            Set<Long> thingFieldIdValid = new LinkedHashSet<>();

            if (forceThingTypeId > 0) {
                if (!compositeThing.onlyHasThingTypeId(forceThingTypeId)) {
                    continue;
                }
            }

            boolean addThingFromExternalField = false;
            if (hasThingExternalFields) {
                for (ReportFilter reportFilter : fieldExternalFilters) {
                    addThingFromExternalField = false;
                    String value = reportFilter.getValue();
                    if (filters.containsKey(reportFilter.getId().toString())) {
                        value = filters.get(reportFilter.getId().toString()).toString();
                    }
                    if (StringUtils.isNotEmpty(value)) {
                        String filteredProperty = reportFilter.getPropertyName();
                        //Filtering by ZoneGroup
                        if ((filteredProperty.contains("zoneGroup") || filteredProperty.contains("localMap"))) {
                            ThingTypeField thingField = compositeThing.getThingTypeField("zone");
                            if (thingField != null) {
                                String zoneName;
                                if (fieldValues.containsKey(thingField.getId())) {
                                    zoneName = fieldValues.get(thingField.getId()).containsKey("value") ?
                                            fieldValues.get(thingField.getId()).get("value").toString() :
                                            "";
                                } else {
                                    zoneName = ThingService.getThingFieldValue(compositeThing.getId(), thingField
                                            .getId());
                                }
                                if (zoneName != null && zoneName.length() > 0) {
                                    Zone zoneEntry = getZoneFromZoneMap(zoneListMap, zoneName);
                                    if (zoneEntry != null) {
                                        if (zoneEntry.getName().equals(zoneName)) {
                                            Long idToGetZones = ReportRuleUtils.isNumeric(value) ?
                                                    Long.parseLong(value) : 0;
                                            Long filterIdToCompare = 0L;
                                            if (filteredProperty.contains("zoneGroup")) {
                                                if (zoneEntry.getZoneGroup() != null) {
                                                    filterIdToCompare = zoneEntry.getZoneGroup().getId();
                                                }
                                            } else {
                                                if (zoneEntry.getLocalMap() != null) {
                                                    filterIdToCompare = zoneEntry.getLocalMap().getId();
                                                }
                                            }

                                            if (reportFilter.getOperator().equals("=") && idToGetZones.equals
                                                    (filterIdToCompare)) {
                                                addThingFromExternalField = true;
                                                break;
                                            }
                                            if (reportFilter.getOperator().equals("!=") && !idToGetZones.equals
                                                    (filterIdToCompare)) {
                                                addThingFromExternalField = true;
                                                break;
                                            }

                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        if (reportFilter.getPropertyName().contains("zone.name")) {
                            List<ThingTypeField> thingFieldZoneType = compositeThing.getThingFieldByType
                                    (ThingTypeField.Type.TYPE_ZONE.value);
                            ThingTypeField thingField;
                            boolean applyZoneCode = false;
                            if (thingFieldZoneType != null && thingFieldZoneType.size() > 0) {
                                thingField = thingFieldZoneType.get(0);
                                applyZoneCode = true;
                            } else {
                                thingField = compositeThing.getThingTypeField("zone");
                            }
                            if (thingField != null) {
                                String zoneNamOrCode;
                                if (fieldValues.containsKey(thingField.getId())) {
                                    zoneNamOrCode = fieldValues.get(thingField.getId()).containsKey("value") ?
                                            fieldValues.get(thingField.getId()).get("value").toString() :
                                            "";
                                } else {
                                    zoneNamOrCode = ThingService.getThingFieldValue(compositeThing.getId(),
                                            thingField.getId());
                                }

                                Zone zoneEntry;
                                if (applyZoneCode) {
                                    zoneEntry = getZoneFromZoneCode(zoneListMap, zoneNamOrCode);
                                } else {
                                    zoneEntry = getZoneFromZoneMap(zoneListMap, zoneNamOrCode);
                                }
                                if (zoneEntry != null) {
                                    if (reportFilter.getOperator().equals("=") && zoneEntry.getName() != null &&
                                            zoneEntry.getName().toLowerCase().equals(value.toLowerCase())) {
                                        addThingFromExternalField = true;
                                        break;
                                    }
                                    if (reportFilter.getOperator().equals("!=") && zoneEntry.getName() != null &&
                                            !zoneEntry.getName().toLowerCase().equals(value.toLowerCase())) {
                                        addThingFromExternalField = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if (reportFilter.getPropertyName().contains("zoneCode")) {
                            ThingTypeField thingField = compositeThing.getThingTypeField("zone");
                            if (thingField != null) {
                                String zoneName;
                                if (fieldValues.containsKey(thingField.getId())) {
                                    zoneName = fieldValues.get(thingField.getId()).containsKey("value") ?
                                            fieldValues.get(thingField.getId()).get("value").toString() :
                                            "";
                                } else {
                                    zoneName = ThingService.getThingFieldValue(compositeThing.getId(), thingField
                                            .getId());
                                }

                                Zone zoneEntry = getZoneFromZoneMap(zoneListMap, zoneName);
                                if (zoneEntry != null) {
                                    if (reportFilter.getOperator().equals("=") && zoneEntry.getCode() != null &&
                                            zoneEntry.getCode().toLowerCase().equals(value.toLowerCase())) {
                                        addThingFromExternalField = true;
                                        break;
                                    }
                                    if (reportFilter.getOperator().equals("!=") && zoneEntry.getCode() != null &&
                                            !zoneEntry.getCode().toLowerCase().equals(value.toLowerCase())) {
                                        addThingFromExternalField = true;
                                        break;
                                    }
                                    if (reportFilter.getOperator().equals("~") && zoneEntry.getCode() != null &&
                                            zoneEntry.getCode().toLowerCase().contains(value.toLowerCase())) {
                                        addThingFromExternalField = true;
                                        break;
                                    }
                                }
                            }
                        }

                        if (reportFilter.getPropertyName().contains("zoneType") || reportFilter.getPropertyName()
                                .contains("zoneProperty")) {
                            ThingTypeField thingField = compositeThing.getThingTypeField("zone");
                            if (thingField != null) {
                                String zoneName;
                                if (fieldValues.containsKey(thingField.getId())) {
                                    zoneName = fieldValues.get(thingField.getId()).containsKey("value") ?
                                            fieldValues.get(thingField.getId()).get("value").toString() :
                                            "";
                                } else {
                                    zoneName = ThingService.getThingFieldValue(compositeThing.getId(), thingField
                                            .getId());
                                }
                                if (zoneName != null && zoneName.length() > 0) {
                                    Zone zoneEntry = getZoneFromZoneMap(zoneListMap, zoneName);
                                    if (zoneEntry != null) {
                                        Long zoneTypeOrPropertyId = 0L;
                                        if (reportFilter.getPropertyName().contains("zoneType")) {
                                            zoneTypeOrPropertyId = value != null && ReportRuleUtils.isNumeric(value)
                                                    ? Long.parseLong(value) : 0L;
                                            if (reportFilter.getOperator().equals("=") && zoneEntry.getZoneType() !=
                                                    null && zoneTypeOrPropertyId.equals(zoneEntry.getZoneType().getId
                                                    ())) {
                                                addThingFromExternalField = true;
                                                break;
                                            }
                                            if (reportFilter.getOperator().equals("!=") && zoneEntry.getZoneType() !=
                                                    null && !zoneTypeOrPropertyId.equals(zoneEntry.getZoneType()
                                                    .getId())) {
                                                addThingFromExternalField = true;
                                                break;
                                            }
                                        }
                                        if (reportFilter.getPropertyName().contains("zoneProperty")) {
                                            zoneTypeOrPropertyId = reportFilter.getThingTypeField().getId();
                                            String zoneZonePropertyId = zoneEntry.getId() + "," + zoneTypeOrPropertyId;
                                            if (zonePropertyValueMap.containsKey(zoneZonePropertyId)) {
                                                ZonePropertyValue zonePropertyValue = zonePropertyValueMap.get
                                                        (zoneZonePropertyId);
                                                String zonePropertyValueItem = "";
                                                if (zonePropertyValue != null) {
                                                    zonePropertyValueItem = zonePropertyValue.getValue().toLowerCase();
                                                }

                                                if (reportFilter.getOperator().equals("=") && value.toLowerCase()
                                                        .equals(zonePropertyValueItem)) {
                                                    addThingFromExternalField = true;
                                                    break;
                                                }
                                                if (reportFilter.getOperator().equals("!=") && !value.toLowerCase()
                                                        .equals(zonePropertyValueItem)) {
                                                    addThingFromExternalField = true;
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (!addThingFromExternalField) {
                            break;
                        }
                    }
                }
            }

            //If the thing is not included in the Core filters as: Zone, ZoneGroup, ZoneProperty, LocalMap
            if (!addThingFromExternalField && hasThingExternalFields) {
                continue;
            }

            //Filter by Thing ThingFields
            boolean addThing = false;
            if (hasThingFieldFilter) {
                for (ReportFilter reportFilter : fieldFilters) {
                    addThing = false;
                    Long thingTypeId = reportFilter.getThingType().getId() != null ? reportFilter.getThingType()
                            .getId() : 0L;
                    //skip the thing if it was on the where clause
                    if (Arrays.asList(VALID_FILTER_KEYS).contains(reportFilter.getPropertyName()) ||
                            reportFilter.getPropertyName().contains(ReportExecutionUtils.DWELL_TIME_PROPERTY_LABEL)) {
                        continue;
                    }
                    if (!compositeThing.hasThingField(reportFilter.getPropertyName())) {
                        break;
                    }
                    String value = reportFilter.getValue();
                    if (filters.containsKey(reportFilter.getId().toString())) {
                        value = filters.get(reportFilter.getId().toString()).toString();
                    }

                    boolean verifyEmptyValues = false;
                    String reportFilterOperator = reportFilter.getOperator();
                    if (reportFilter.getOperator().equals("isEmpty")) {
                        reportFilterOperator = "=";
                        verifyEmptyValues = true;
                    }


                    if (StringUtils.isNotEmpty(value) || verifyEmptyValues) {
                        String filteredProperty = reportFilter.getPropertyName().replace("parent.", "");
                        boolean isTimeStamp = filteredProperty.contains(ReportExecutionUtils.TIME_STAMP_PROPERTY_LABEL);
                        if (isTimeStamp) {
                            filteredProperty = ReportExecutionUtils.removeTimeStamp(filteredProperty);
                        }
                        boolean hasIt = compositeThing.hasThingField(filteredProperty);

                        //thing has the property
                        if (hasIt) {
                            //get value in map with field id
                            if (fieldValues != null) {
                                ThingTypeField thingField = compositeThing.getThingTypeField(filteredProperty);
                                if (thingTypeId > 0 && !compositeThing.containThingTypeId(thingTypeId)) {
                                    thingField = null;
                                }

                                if (thingField != null) {
                                    Map<String, Object> fieldMap = fieldValues.get(thingField.getId());
                                    List<Object[]> historicalFieldList = new LinkedList<>();
                                    if (fieldThingValues != null && fieldThingValues.containsKey(thingField.getId())) {
                                        historicalFieldList = fieldThingValues.get(thingField.getId());
                                    }
//        Todo findBugs Redundant null check, refactor o delete it (dateItem always is null)
//                                    if(!fieldValues.containsKey(thingField.getId())) {
//                                        //TODO FIX THIS METHOD OR DELETE IT
//                                        //fieldMap = FieldValueService.valueTimeMap(compositeThing.getId(),
// thingField.getId());
//                                        fieldMap = null;
//                                        if(fieldMap != null && fieldMap.containsKey("time") && fieldMap.containsKey
// ("value")) {
//                                            fieldValues.put(thingField.getId(), fieldMap);
//                                        }
//                                    }
                                    String fieldValue = "";
                                    if (fieldMap != null) {
                                        if (isTimeStamp) {
                                            fieldValue = ((Date) (fieldMap.get("time"))).getTime() + "";
                                        } else {
                                            fieldValue = (String) fieldMap.get("value");
                                            if (fieldValue == null) {
                                                fieldValue = "";
                                            }
                                            if (thingField.getDataType().getId().compareTo(ThingTypeField.Type
                                                    .TYPE_SHIFT.value) == 0) {
                                                if (fieldValue != null) {
                                                    if (verifyEmptyValues) {
                                                        if (fieldValue.toString().equals("")) {
                                                            addThing = true;
                                                            break;
                                                        }
                                                    } else {
                                                        String shifts[] = fieldValue.split(",");
                                                        for (int it = 0; it < shifts.length; it++) {
                                                            if (ReportRuleUtils.isNumeric(shifts[it]) &&
                                                                    ReportRuleUtils.isNumeric(value)) {
                                                                if (shifts[it].equals(value)) {
                                                                    addThing = true;
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (reportFilterOperator.equals("=")) {
                                        if (thingField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_SHIFT
                                                .value) == 0) {
                                            if (fieldValue != null) {
                                                String shifts[] = fieldValue.split(",");
                                                for (int it = 0; it < shifts.length; it++) {
                                                    if (ReportRuleUtils.isNumeric(shifts[it]) && ReportRuleUtils
                                                            .isNumeric(value)) {
                                                        if (shifts[it].equals(value)) {
                                                            addThing = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            if (value.trim().toLowerCase().equals(fieldValue.trim().toLowerCase())) {
                                                addThing = true;
                                            }
                                            //Filtering history
                                            List<Object[]> historicalTmp = new LinkedList<>();
                                            for (Object[] item : historicalFieldList) {
                                                if (item[1] != null && item[1].toString().equals(value)) {
                                                    historicalTmp.add(item);
                                                    Long timeStampItem = ((Date) (item[0])).getTime();
                                                    timeStampValid.put(timeStampItem, timeStampValid.containsKey
                                                            (timeStampItem) ?
                                                            timeStampValid.get(timeStampItem) + 1 :
                                                            1
                                                    );
                                                    addThing = true;
                                                }
                                            }
                                            if (fieldThingValues != null) {
//                                                fieldThingValues.put(thingField.getId(), historicalTmp);
                                                thingFieldIdValid.add(thingField.getId());
                                            }
                                        }
                                    } else if (reportFilterOperator.equals("~")) {
                                        if (fieldValue.trim().toLowerCase().contains(value.trim().toLowerCase())) {
                                            addThing = true;
                                        }
                                        //Filtering history
                                        List<Object[]> historicalTmp = new LinkedList<>();
                                        for (Object[] item : historicalFieldList) {
                                            if (item[1] != null && item[1].toString().contains(value)) {
                                                historicalTmp.add(item);
                                                Long timeStampItem = ((Date) (item[0])).getTime();
                                                timeStampValid.put(timeStampItem, timeStampValid.containsKey
                                                        (timeStampItem) ?
                                                        timeStampValid.get(timeStampItem) + 1 :
                                                        1
                                                );
                                                addThing = true;
                                            }
                                        }
                                        if (fieldThingValues != null) {
                                            fieldThingValues.put(thingField.getId(), historicalTmp);
                                            thingFieldIdValid.add(thingField.getId());
                                        }
                                    } else if (reportFilterOperator.equals("<")) {
                                        if (ReportRuleUtils.isNumeric(value) && ReportRuleUtils.isNumeric(fieldValue)) {
                                            if (Long.parseLong(fieldValue) < Long.parseLong(value)) {
                                                addThing = true;
                                            }
                                        }
                                        //Filtering history
                                        List<Object[]> historicalTmp = new LinkedList<>();
                                        for (Object[] item : historicalFieldList) {
                                            if (ReportRuleUtils.isNumeric(item[1].toString()) && ReportRuleUtils
                                                    .isNumeric(value)) {
                                                if (Double.valueOf(item[1].toString()) < Double.valueOf(value)) {
                                                    historicalTmp.add(item);
                                                    Long timeStampItem = ((Date) (item[0])).getTime();
                                                    timeStampValid.put(timeStampItem, timeStampValid.containsKey
                                                            (timeStampItem) ?
                                                            timeStampValid.get(timeStampItem) + 1 :
                                                            1
                                                    );
                                                    addThing = true;
                                                }
                                            }
                                        }
                                        if (fieldThingValues != null) {
                                            fieldThingValues.put(thingField.getId(), historicalTmp);
                                            thingFieldIdValid.add(thingField.getId());
                                        }

                                    } else if (reportFilterOperator.equals("<=")) {
                                        if (ReportRuleUtils.isNumeric(value) && ReportRuleUtils.isNumeric(fieldValue)) {
                                            if (Long.parseLong(fieldValue) <= Long.parseLong(value)) {
                                                addThing = true;
                                            }
                                        }
                                        //Filtering history
                                        List<Object[]> historicalTmp = new LinkedList<>();
                                        for (Object[] item : historicalFieldList) {
                                            if (item[1] != null && ReportRuleUtils.isNumeric(item[1].toString()) &&
                                                    ReportRuleUtils.isNumeric(value)) {
                                                if (Double.valueOf(item[1].toString()) <= Double.valueOf(value)) {
                                                    historicalTmp.add(item);
                                                    Long timeStampItem = ((Date) (item[0])).getTime();
                                                    timeStampValid.put(timeStampItem, timeStampValid.containsKey
                                                            (timeStampItem) ?
                                                            timeStampValid.get(timeStampItem) + 1 :
                                                            1
                                                    );
                                                    addThing = true;
                                                }
                                            }
                                        }
                                        if (fieldThingValues != null) {
                                            fieldThingValues.put(thingField.getId(), historicalTmp);
                                            thingFieldIdValid.add(thingField.getId());
                                        }

                                    } else if (reportFilterOperator.equals(">")) {
                                        if (ReportRuleUtils.isNumeric(value) && ReportRuleUtils.isNumeric(fieldValue)) {
                                            if (Double.valueOf(fieldValue) > Double.valueOf(value)) {
                                                addThing = true;
                                            }
                                        }
                                        //Filtering history
                                        List<Object[]> historicalTmp = new LinkedList<>();
                                        for (Object[] item : historicalFieldList) {
                                            if (ReportRuleUtils.isNumeric(item[1].toString()) && ReportRuleUtils
                                                    .isNumeric(value)) {
                                                if (Double.valueOf(item[1].toString()) > Double.valueOf(value)) {
                                                    historicalTmp.add(item);
                                                    Long timeStampItem = ((Date) (item[0])).getTime();
                                                    timeStampValid.put(timeStampItem, timeStampValid.containsKey
                                                            (timeStampItem) ?
                                                            timeStampValid.get(timeStampItem) + 1 :
                                                            1
                                                    );
                                                    addThing = true;
                                                }
                                            }
                                        }
                                        if (fieldThingValues != null) {
                                            fieldThingValues.put(thingField.getId(), historicalTmp);
                                            thingFieldIdValid.add(thingField.getId());
                                        }

                                    } else if (reportFilterOperator.equals(">=")) {
                                        if (ReportRuleUtils.isNumeric(value) && ReportRuleUtils.isNumeric(fieldValue)) {
                                            if (Double.valueOf(fieldValue) >= Double.valueOf(value)) {
                                                addThing = true;
                                            }
                                        }
                                        //Filtering history
                                        List<Object[]> historicalTmp = new LinkedList<>();
                                        for (Object[] item : historicalFieldList) {
                                            if (item[1] != null && ReportRuleUtils.isNumeric(item[1].toString()) &&
                                                    ReportRuleUtils.isNumeric(value)) {
                                                if (Double.valueOf(item[1].toString()) >= Double.valueOf(value)) {
                                                    historicalTmp.add(item);
                                                    Long timeStampItem = ((Date) (item[0])).getTime();
                                                    timeStampValid.put(timeStampItem, timeStampValid.containsKey
                                                            (timeStampItem) ?
                                                            timeStampValid.get(timeStampItem) + 1 :
                                                            1
                                                    );
                                                    addThing = true;
                                                }
                                            }
                                        }
                                        if (fieldThingValues != null) {
                                            fieldThingValues.put(thingField.getId(), historicalTmp);
                                            thingFieldIdValid.add(thingField.getId());
                                        }

                                    } else if (reportFilterOperator.equals("!=")) {
                                        if (thingField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_SHIFT
                                                .value) == 0) {
                                            if (fieldValue != null) {
                                                String shifts[] = fieldValue.split(",");
                                                addThing = true;
                                                for (int it = 0; it < shifts.length; it++) {
                                                    if (ReportRuleUtils.isNumeric(shifts[it]) && ReportRuleUtils
                                                            .isNumeric(value)) {
                                                        if (shifts[it].equals(value)) {
                                                            addThing = false;
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            if (!value.trim().toLowerCase().equals(fieldValue.trim().toLowerCase())) {
                                                addThing = true;
                                            }
                                            //Filtering history
                                            List<Object[]> historicalTmp = new LinkedList<>();
                                            for (Object[] item : historicalFieldList) {
                                                if (item[1] != null && !item[1].toString().equals(value)) {
                                                    historicalTmp.add(item);
                                                    Long timeStampItem = ((Date) (item[0])).getTime();
                                                    timeStampValid.put(timeStampItem, timeStampValid.containsKey
                                                            (timeStampItem) ?
                                                            timeStampValid.get(timeStampItem) + 1 :
                                                            1
                                                    );
                                                    addThing = true;
                                                }
                                            }
                                            if (fieldThingValues != null) {
                                                fieldThingValues.put(thingField.getId(), historicalTmp);
                                                thingFieldIdValid.add(thingField.getId());
                                            }
                                        }
                                    } else {
                                        logger.error("case not implemented for operator='" + reportFilterOperator +
                                                "'");
                                    }
                                }
                            }
                            if (!addThing) break;
                        }
                    }
                }
                //add to list if it matches filter
                if (addThing && (addThingFromExternalField || !hasThingExternalFields)) {
                    filteredThings.add(compositeThing);
                }

                //Removing
                if (fieldThingValues != null) {
                    for (Map.Entry<Long, List<Object[]>> fieldThingValue : fieldThingValues.entrySet()) {
                        List<Object[]> listObject = fieldThingValue.getValue();
                        List<Object[]> newListObj = new LinkedList<>();
                        if (timeStampValid.size() > 0) {
                            for (Object[] itemObj : listObject) {
                                Long timeItem = ((Date) (itemObj[0])).getTime();
                                if (timeStampValid.containsKey(timeItem) && timeStampValid.get(timeItem).equals
                                        (thingFieldIdValid.size())) {
                                    newListObj.add(itemObj);
                                }
                            }
                            if (compositeThing.hasThingFieldId(fieldThingValue.getKey()))
                                fieldThingValues.put(fieldThingValue.getKey(), newListObj);
                        }
                    }
                }
            } else {
                filteredThings.add(compositeThing);
            }
        }


        return filteredThings;
    }

    public static List<Long> getZoneLocationThingTypeFieldIds(List<CompositeThing> things, Map<Long, Thing> childMap,
                                                              String propertyName) {
        List<Long> idList = new ArrayList<>();
        for (CompositeThing thing : things) {
            for (ThingTypeField thingTypeField : thing.getThingTypeFields()) {
                if (thingTypeField.getName().equals(propertyName)) {
                    idList.add(thingTypeField.getId());
                }
            }
            if (childMap != null) {
                Thing childThing = childMap.get(thing.getId());
                if (childThing != null) {
                    for (ThingTypeField thingTypeField : childThing.getThingType().getThingTypeFields()) {
                        if (thingTypeField.getName().equals(propertyName)) {
                            idList.add(thingTypeField.getId());
                        }
                    }
                }
            }
        }
        return idList;
    }

    public static Zone getZoneFromZoneMap(Map<Long, Zone> zoneListMap, String zoneName) {
        for (Map.Entry<Long, Zone> zoneMapEntry : zoneListMap.entrySet()) {
            Zone zoneEntry = zoneMapEntry.getValue();
            if (zoneEntry != null) {
                if (zoneEntry.getName().equals(zoneName)) {
                    return zoneEntry;
                }
            }
        }
        return null;
    }

    public static Zone getZoneFromZoneCode(Map<Long, Zone> zoneListMap, String zoneCode) {
        for (Map.Entry<Long, Zone> zoneMapEntry : zoneListMap.entrySet()) {
            Zone zoneEntry = zoneMapEntry.getValue();
            if (zoneEntry != null) {
                if (zoneEntry.getCode().equals(zoneCode)) {
                    return zoneEntry;
                }
            }
        }
        return null;
    }

    public static File fileCsv(List<String> headers, List<List<String>> values) {
        FileExportService fileExport;
        File file = null;
        try {
            fileExport = new FileExportService(headers, values);
            file = fileExport.export(FileExportService.Type.valueOf("REPORT"));
        } catch (IOException e) {
            logger.error("fileCsv", e);
        }
        return file;
    }

    // provides a pretty printed JSON string for the given map
    public static String getPrettyPrint(@SuppressWarnings("rawtypes") Map map) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try {
            mapper.writeValue(b, map);
            return b.toString("UTF-8");
        } catch (IOException e) {
            logger.warn("error", e);
        }
        return null;
    }

}
