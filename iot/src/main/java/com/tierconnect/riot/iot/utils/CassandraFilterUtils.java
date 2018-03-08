package com.tierconnect.riot.iot.utils;

import com.google.common.collect.Lists;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.ReportPropertyService;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by arojas on 4/29/15.
 */
public class CassandraFilterUtils {

    static Logger logger = Logger.getLogger(CassandraFilterUtils.class);

    public static List<CompositeThing> getThingsByPropertiesUsingCassandra(ReportDefinition reportDefinition,
                                                                           List<CompositeThing> thingList,
                                                                           int orderByColumn,
                                                                           String sortProp) {
        //ReportProperty order
        ReportProperty reportPropertyOrderColumn = ReportPropertyService.getInstance().getPropertyNameFromOrderColumn(
                reportDefinition.getReportProperty(),
                orderByColumn
        );

        if(reportPropertyOrderColumn == null) {
            return new LinkedList<>();
        }


        //Get Thing Ids -> This will be verified into cassandra fields
        Set<Long> thingSet = new LinkedHashSet<>();

        Map<Long, CompositeThing> compositeThingMap = new HashMap<>();
        Map<Long, CompositeThing> childThingMap = new HashMap<>();

        //Get Properties... (First property)
        Set<Long> thingListSet = new LinkedHashSet<>();
        LinkedList<Long> thingListResult;

        //First we are getting the cassandra values for the column with order value & sorting them
        String propertyNameToOrder = reportPropertyOrderColumn.getPropertyName();

        if(propertyNameToOrder.equals("name") || propertyNameToOrder.equals("serial")) {
            if(sortProp.equals("DESC")) {
                thingList = Lists.reverse(thingList);
            }
        }

        for (CompositeThing thing : thingList) {
            if(propertyNameToOrder.equals("name") || propertyNameToOrder.equals("serial")) {
                thingListSet.add(thing.getParent().getId());
            }
            compositeThingMap.put(thing.getParent().getId(), thing);
            thingSet.add(thing.getParent().getId());

            List<Thing> childrenList = thing.getChildren();
            if(childrenList != null && childrenList.size() > 0) {
                for(Thing child : childrenList) {
                    childThingMap.put(child.getId(), thing);
                    thingSet.add(child.getId());
                }
            }
        }
        if(propertyNameToOrder.equals("name") || propertyNameToOrder.equals("serial")) {

        }else {
            List<Long> thingTypeFieldIds = ThingTypeFieldService.getInstance().getThingTypeFieldIdsByPropertyName(new LinkedList<>(Arrays.asList(propertyNameToOrder)));
            if (thingTypeFieldIds.size() > 0) {

                List<Thing> things = ThingService.getInstance().selectAllThings();
                List<Long> thingIds = new LinkedList<>();
                for(Thing thing : things) {
                    thingIds.add(thing.getId());
                }
                //TODO FIX THIS METHOD OR DELETE IT
                //Map<Long,Map<Long, Map<String, Object>>> fieldValuesMap = FieldValueService.getFieldsValues(thingIds, thingTypeFieldIds);
                Map<Long,Map<Long, Map<String, Object>>> fieldValuesMap = new HashMap<>();
                List<FieldTypeCassandra> fieldTypeCassandraList = convertArrayToFieldValueCassandra(fieldValuesMap);
                fieldTypeCassandraList = orderFieldValueCassandra(fieldTypeCassandraList, thingSet, sortProp);

                for (FieldTypeCassandra fieldTypeCassandra : fieldTypeCassandraList) {
                    Long thingIdFromCassandra = fieldTypeCassandra.getThingId();

                    //Set Parent
                    if (childThingMap.containsKey(thingIdFromCassandra)) {
                        thingListSet.add(childThingMap.get(thingIdFromCassandra).getParent().getId());
                    }

                    //Set Children
                    if (compositeThingMap.containsKey(thingIdFromCassandra)) {
                        thingListSet.add(compositeThingMap.get(thingIdFromCassandra).getParent().getId());
                    }
                }
            }
        }

        thingListResult = new LinkedList<>(thingListSet);
        boolean includeSerialName = false;

        //Loop in all properties
        List<ReportProperty> reportProperties = reportDefinition.getReportProperty();
        List<String> reportPropertyValues = new LinkedList<>();
        for(ReportProperty reportProperty : reportProperties) {
            reportPropertyValues.add(reportProperty.getPropertyName());
        }

        //Get Data necessary for cassandra
        List<ThingTypeField> thingFieldIdsForProperties = ThingTypeFieldService.getInstance().getThingTypeFieldByPropertyName(reportPropertyValues);
        List<Long> thingFieldIds = getIdsFromFieldList( thingFieldIdsForProperties );

        List<Thing> things = ThingService.getInstance().selectAllThings();
        List<Long> thingIds = new LinkedList<>();
        for(Thing thing : things) {
            thingIds.add(thing.getId());
        }


        long t1 = System.currentTimeMillis();
        //TODO FIX THIS METHOD OR DELETE IT
        //Map<Long,Map<Long, Map<String, Object>>> fieldValuesMapForProperties = FieldValueService.getFieldsValues(thingIds, thingFieldIds);
        Map<Long,Map<Long, Map<String, Object>>> fieldValuesMapForProperties = new HashMap<>();
        long t2 = System.currentTimeMillis();
        logger.info( "Getting All ThingFields values for report() execution_time=" + (t2 - t1 ) );
        logger.info( "ThingFieldValues Cassandra Size=" + fieldValuesMapForProperties.size() );

        List<FieldTypeCassandra> fieldTypeCassandraList = convertArrayToFieldValueCassandra(fieldValuesMapForProperties);
        Map<String, List<FieldTypeCassandra> > fieldValuesCassandraList = getFieldValuesByProperty(thingFieldIdsForProperties, fieldTypeCassandraList);

        for(ReportProperty reportProperty : reportProperties) {
            if(reportProperty.getPropertyName().equals("name") || reportProperty.getPropertyName().equals("serial")) {
                includeSerialName = true;
            }
            if(reportProperty.getDisplayOrder() != null && Math.round(reportProperty.getDisplayOrder()) != orderByColumn) {
                List<FieldTypeCassandra> fieldTypeCassandraListForProperty = fieldValuesCassandraList.get(reportProperty.getPropertyName());
                if(fieldTypeCassandraListForProperty != null) {
                    for (FieldTypeCassandra fieldTypeCassandra : fieldTypeCassandraListForProperty) {
                        Long thingIdCassandra = fieldTypeCassandra.getThingId();
                        CompositeThing compositeThing = null;
                        if (childThingMap.containsKey(thingIdCassandra)) {
                            compositeThing = childThingMap.get(thingIdCassandra);
                        }
                        if (compositeThingMap.containsKey(thingIdCassandra)) {
                            compositeThing = compositeThingMap.get(thingIdCassandra);
                        }

                        if (compositeThing != null && !thingListSet.contains(compositeThing.getParent().getId())) {
                            if (sortProp.equals("DESC")) {
                                thingListResult.add(fieldTypeCassandra.getThingId());
                            } else {
                                thingListResult.addFirst(fieldTypeCassandra.getThingId());
                            }
                            thingListSet.add(fieldTypeCassandra.getThingId());
                        }
                    }
                }
            }
        }

        Set<CompositeThing> thingResult = new LinkedHashSet<>();

        if(includeSerialName) {
            for(CompositeThing compositeThing : thingList) {
                if(!thingListSet.contains(compositeThing.getId())) {
                    if (sortProp.equals("DESC")) {
                        thingListResult.add(compositeThing.getId());
                    } else {
                        thingListResult.addFirst(compositeThing.getId());
                    }
                }
            }
        }

        for(Long thingId : thingListResult) {
            if(childThingMap.containsKey(thingId)) {
                thingResult.add(childThingMap.get(thingId));
            }
            if(thingSet.contains(thingId)) {
                if (compositeThingMap.containsKey(thingId)) {
                    thingResult.add(compositeThingMap.get(thingId));
                }
            }
        }
        return new LinkedList<>(thingResult);
    }


    public static List<CompositeThing> filteringThingsUsingCassandra(ReportDefinition reportDefinition,
                                                                     List<CompositeThing> compositeThingList,
                                                                     Map<String, Object> filters,
                                                                     Map<Long, Zone> zoneListMap,
                                                                     Map<String, ZonePropertyValue> zonePropertyValueMap) {
        List<ReportFilter> reportFilters = reportDefinition.getReportFilter();
        List<String> reportFilterPropertyNames = new LinkedList<>();

        //Map to save UDF
        Map<String, ReportFilter> propertyValueMap = new HashMap<>();

        //Map to save external properties as zoneGroup, zoneType, zoneProperty, zoneName
        Map<String, ReportFilter> zonePropertiesMap = new HashMap<>();

        //Zone Map  Name -> Zone
        Map<String, Zone> zoneNameMap = new HashMap<>();

        //Map to Save Thing.id -> Zone
        Map<Long, Zone> thingZoneMap = new HashMap<>();

        //ChildrenMap
        Map<Long, Long> parentChildrenMap = new HashMap<>();

        for(CompositeThing compositeThing : compositeThingList) {
            if(compositeThing.getParent().getId() != compositeThing.getThing().getId()) {
                parentChildrenMap.put(compositeThing.getParent().getId(), compositeThing.getThing().getId());
            }
            List<Thing> children = compositeThing.getChildren();
            if(children != null && children.size() > 0) {
                for(Thing child : children) {
                    parentChildrenMap.put(child.getId(), compositeThing.getParent().getId());
                }
            }
        }


        boolean applyZoneFilter = false;

        for(ReportFilter reportFilter : reportFilters) {
            String filterValue = reportFilter.getValue();
            if(filters.containsKey(reportFilter.getId().toString())) {
                filterValue = filters.get(reportFilter.getId().toString()).toString();
            }
            if(filterValue != null && !filterValue.isEmpty()) {
                if(isZoneExternalProperty( reportFilter.getPropertyName() )) {
                    zonePropertiesMap.put( reportFilter.getPropertyName(), reportFilter );
                    applyZoneFilter = true;
                }
                else {
                    propertyValueMap.put(reportFilter.getPropertyName(), reportFilter);
                }
                //If the propertyName include zone ( zoneGroup, zoneProperty, zoneType ) -> get Zone thingField
                reportFilterPropertyNames.add( getExtendPropertyIfApply( reportFilter.getPropertyName() ) );
            }
        }

        List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().getThingTypeFieldByPropertyName(reportFilterPropertyNames);
        List<Long> thingFieldsIds = getIdsFromFieldList(thingTypeFields);

        List<Thing> things = ThingService.getInstance().selectAllThings();
        List<Long> thingIds = new LinkedList<>();
        for(Thing thing : things) {
            thingIds.add(thing.getId());
        }

        //TODO FIX THIS METHOD OR DELETE IT
        //Map<Long,Map<Long, Map<String, Object>>>  thingFieldValuesMap = FieldValueService.getFieldsValues(thingIds, thingFieldsIds);
        Map<Long,Map<Long, Map<String, Object>>>  thingFieldValuesMap = new HashMap<>();
        List<FieldTypeCassandra> fieldTypeCassandraList = convertArrayToFieldValueCassandra(thingFieldValuesMap);
        Map<String, List<FieldTypeCassandra> > fieldValuePropertyMap = getFieldValuesByProperty(thingTypeFields, fieldTypeCassandraList);

        boolean hasFilter = false;

        int thingMatchNumber = fieldValuePropertyMap.size() - zonePropertiesMap.size();

        if(thingMatchNumber > 0 || Utilities.isValueInTheList(reportFilterPropertyNames, "thingType.id") )
        {
            hasFilter = true;
        }

        //Filtering Things
        if(hasFilter) {
            Set<Long> validThingIds = new LinkedHashSet<>();
            Map<Long, Long> countThing = new HashMap<>();

            //Filter for UDF
            for (Map.Entry<String, ReportFilter> propertyValue : propertyValueMap.entrySet()) {
                String propertyName = propertyValue.getKey();
                List<FieldTypeCassandra> fieldTypeCassandraListTmp = fieldValuePropertyMap.get( propertyName );
                if (fieldTypeCassandraListTmp != null) {
                    for (FieldTypeCassandra fieldTypeCassandra : fieldTypeCassandraListTmp) {
                        //Filtering by value
                        if(applyFilter(propertyValue.getValue(), fieldTypeCassandra, filters)) {
                            //Counting the parent ++
                            if (!countThing.containsKey(fieldTypeCassandra.getThingId())) {
                                countThing.put(fieldTypeCassandra.getThingId(), 1L);
                            } else {
                                countThing.put(fieldTypeCassandra.getThingId(), countThing.get(fieldTypeCassandra.getThingId()) + 1);
                            }
                        }
                    }

                    //On this case... all the things should comply all the filters
                    //For example 3 filters -> the thing should comply 3 filters
                    for (Map.Entry<Long, Long> countThingItem : countThing.entrySet()) {

                        Long countVal = 0L;
                        if(parentChildrenMap.containsKey(countThingItem.getKey()) && countThing.containsKey(parentChildrenMap.get(countThingItem.getKey()))) {
                            countVal = countThing.get(parentChildrenMap.get(countThingItem.getKey()));
                        }

                        if (countVal + countThingItem.getValue().intValue() == thingMatchNumber) {
                            validThingIds.add(countThingItem.getKey());
                        }
                    }
                }
            }


            List<CompositeThing> newCompositeThingList = new LinkedList<>();
            for (CompositeThing compositeThing : compositeThingList) {
                boolean includeThing = false;
                if (validThingIds.contains(compositeThing.getId())) {
                    includeThing = true;
                }
                if (compositeThing.getParent() != null && validThingIds.contains(compositeThing.getParent().getId())) {
                    includeThing = true;
                }
                if (compositeThing.getChildren() != null) {
                    for (Thing thing : compositeThing.getChildren()) {
                        if (validThingIds.contains(thing.getId())) {
                            includeThing = true;
                            break;
                        }
                    }
                }
                //Filter for thingType.id , special case
                if(Utilities.isValueInTheList(reportFilterPropertyNames, "thingType.id") && (includeThing || thingMatchNumber == 0) )
                {
                    String operator = "=";
                    for (Map.Entry<String, ReportFilter> propertyValue : propertyValueMap.entrySet()) {
                        if(propertyValue.getKey().equals("thingType.id"))
                        {
                            operator = propertyValue.getValue().getOperator();
                        }
                    }
                    if ( operator.equals("==") && ( (compositeThing.getChildren() != null && compositeThing.getChildren().size() > 0)
                            || (compositeThing.getParent() != null && !compositeThing.getId().equals(compositeThing.getParent().getId())) )) {
                        continue;
                    }else
                    {
                        newCompositeThingList.add(compositeThing);
                    }
                }
            }



            if(!applyZoneFilter) {
                return newCompositeThingList;
            }else {
                compositeThingList = newCompositeThingList;
            }
        }

        //Get Zone for each Thing
        if(applyZoneFilter) {
            Set<Long> thingIdValids = new LinkedHashSet<>();
            for(CompositeThing compositeThing : compositeThingList) {
                thingIdValids.add(compositeThing.getId());
            }


            for(Map.Entry<Long, Zone> zoneEntry : zoneListMap.entrySet()) {
                zoneNameMap.put(zoneEntry.getValue().getName(), zoneEntry.getValue());
            }

            for(Long thing : thingIds){
                for (ThingTypeField thingField : thingTypeFields) {
                    if (thingField.getName().equals("zone")) {
                        Map<String, Object> valuesList = thingFieldValuesMap.get(thing).get(thingField.getId());
                        if (valuesList != null && valuesList.size() > 0) {
                            Map<String, Object> valueObject = valuesList;
                            FieldTypeCassandra fieldTypeCassandra = new FieldTypeCassandra(
                                    thingField.getId(),
                                    (Date) (valueObject.get("time")),
                                    valueObject.get("value"),
                                    Long.valueOf(thing),
                                    Long.valueOf(valueObject.get("write_time").toString()));
                            if(fieldTypeCassandra.getValue() != null ) {
                                Zone zone = zoneNameMap.get( fieldTypeCassandra.getValue() );
                                if(zone != null && thingIdValids.contains(fieldTypeCassandra.getThingId())) {
                                    thingZoneMap.put(fieldTypeCassandra.getThingId(), zone);
                                }
                            }
                        }
                    }
                }

            }
            List<CompositeThing> newCompositeThingList = new LinkedList<>();
            for(CompositeThing compositeThing : compositeThingList) {
                //Filter for ZoneGroup, ZoneType, ZoneProperty etc..
                boolean includeThing = true;
                for (Map.Entry<String, ReportFilter> propertyValue : zonePropertiesMap.entrySet()) {
                    if(!applyFilterForExternalProperties(propertyValue.getValue(), filters, compositeThing, thingZoneMap, zonePropertyValueMap)) {
                        includeThing = false;
                        break;
                    }
                }
                if(includeThing) {
                    newCompositeThingList.add( compositeThing );
                }
            }return newCompositeThingList;
        }

        return compositeThingList;
    }

    public static boolean applyFilterForExternalProperties(ReportFilter reportFilter, Map<String, Object> filters, CompositeThing compositeThing, Map<Long, Zone> thingZoneMap, Map<String, ZonePropertyValue> zonePropertyValueMap) {
        String value = reportFilter.getValue();
        if (filters.containsKey(reportFilter.getId().toString() ) ) {
            value = filters.get(reportFilter.getId().toString()).toString();
        }
        if (StringUtils.isNotEmpty(value)) {
            String filteredProperty = reportFilter.getPropertyName();
            //Filtering by ZoneGroup
            Long childId = compositeThing.getFirstChild() != null ? compositeThing.getFirstChild().getId() : 0;
            Zone zoneToFind = null;
            if(thingZoneMap.containsKey(compositeThing.getParent().getId()) || thingZoneMap.containsKey(compositeThing.getId()) || thingZoneMap.containsKey(childId)) {
                zoneToFind = thingZoneMap.get(compositeThing.getParent().getId());
                if(zoneToFind == null) thingZoneMap.get(compositeThing.getId());
                if(zoneToFind == null) thingZoneMap.get(childId);
            }
            if(zoneToFind != null) {
                //ZoneGroup & LocalMap
                if ((filteredProperty.contains("zoneGroup") || filteredProperty.contains("localMap"))) {
                    Long idToGetZones = ReportRuleUtils.isNumeric(value) ?
                            Long.parseLong(value) : 0;
                    Long filterIdToCompare = 0L;
                    if(filteredProperty.contains("zoneGroup")) {
                        if(zoneToFind.getZoneGroup() != null) {
                            filterIdToCompare = zoneToFind.getZoneGroup().getId();
                        }
                    }
                    else {
                        if(zoneToFind.getLocalMap() != null) {
                            filterIdToCompare = zoneToFind.getLocalMap().getId();
                        }
                    }

                    if (reportFilter.getOperator().equals("=") && idToGetZones.equals(filterIdToCompare)) {
                        return true;
                    }
                    if (reportFilter.getOperator().equals("!=") && !idToGetZones.equals(filterIdToCompare)) {
                        return true;
                    }
                }

                //ZoneCode
                if (reportFilter.getPropertyName().contains("zoneCode")) {
                    if (reportFilter.getOperator().equals("=") && zoneToFind.getCode() != null && zoneToFind.getCode().toLowerCase().equals(value.toLowerCase())) {
                        return true;
                    }
                    if (reportFilter.getOperator().equals("!=") && zoneToFind.getCode() != null && !zoneToFind.getCode().toLowerCase().equals(value.toLowerCase())) {
                        return true;
                    }
                    if (reportFilter.getOperator().equals("~") && zoneToFind.getCode() != null && zoneToFind.getCode().toLowerCase().contains(value.toLowerCase())) {
                        return true;
                    }
                }

                //ZoneType or ZoneProperty
                if (reportFilter.getPropertyName().contains("zoneType") || reportFilter.getPropertyName().contains("zoneProperty") ) {
                    Long zoneTypeOrPropertyId = 0L;
                    if (reportFilter.getPropertyName().contains("zoneType")) {
                        zoneTypeOrPropertyId = ReportRuleUtils.isNumeric(value) ? Long.parseLong(value) : 0L;
                        if (reportFilter.getOperator().equals("=") && zoneToFind.getZoneType() != null && zoneTypeOrPropertyId.equals(zoneToFind.getZoneType().getId())) {
                            return true;
                        }
                        if (reportFilter.getOperator().equals("!=") && zoneToFind.getZoneType() != null && !zoneTypeOrPropertyId.equals(zoneToFind.getZoneType().getId())) {
                            return true;
                        }
                    }
                    if (reportFilter.getPropertyName().contains("zoneProperty")) {
                        zoneTypeOrPropertyId = reportFilter.getThingTypeField().getId();
                        String zoneZonePropertyId = zoneToFind.getId() + "," + zoneTypeOrPropertyId;
                        if (zonePropertyValueMap.containsKey(zoneZonePropertyId)) {
                            ZonePropertyValue zonePropertyValue = zonePropertyValueMap.get(zoneZonePropertyId);
                            String zonePropertyValueItem = "";
                            if (zonePropertyValue != null) {
                                zonePropertyValueItem = zonePropertyValue.getValue().toLowerCase();
                            }

                            if (reportFilter.getOperator().equals("=") && value.toLowerCase().equals(zonePropertyValueItem)) {
                                return true;
                            }
                            if (reportFilter.getOperator().equals("!=") && !value.toLowerCase().equals(zonePropertyValueItem)) {
                                return true;
                            }
                        }
                    }
                }

            }



//            if (reportFilter.getPropertyName().contains("zone.name")) {
//                List<ThingField> thingFieldZoneType = compositeThing.getThingTypeFieldByType(ThingTypeField.Type.TYPE_ZONE.value);
//                ThingField thingField;
//                boolean applyZoneCode = false;
//                if(thingFieldZoneType != null && thingFieldZoneType.size() > 0) {
//                    thingField = thingFieldZoneType.get(0);
//                    applyZoneCode = true;
//                }else {
//                    thingField = compositeThing.getThingTypeField("zone");
//                }
//                if (thingField != null) {
//                    String zoneNamOrCode;
//                    if (fieldValues.containsKey(thingField.getId())) {
//                        zoneNamOrCode = fieldValues.get(thingField.getId()).containsKey("value") ?
//                                fieldValues.get(thingField.getId()).get("value").toString() :
//                                "";
//                    } else {
//                        zoneNamOrCode = ThingService.getThingFieldValue(thingField.getId());
//                    }
//
//                    Zone zoneEntry;
//                    if(applyZoneCode) {
//                        zoneEntry = getZoneFromZoneCode(zoneListMap, zoneNamOrCode);
//                    }else {
//                        zoneEntry = getZoneFromZoneMap(zoneListMap, zoneNamOrCode);
//                    }
//                    if(zoneEntry != null) {
//                        if (reportFilter.getOperator().equals("=") && zoneEntry.getName() != null && zoneEntry.getName().toLowerCase().equals(value.toLowerCase())) {
//                            addThingFromExternalField = true;
//                            break;
//                        }
//                        if (reportFilter.getOperator().equals("!=") && zoneEntry.getName() != null && !zoneEntry.getName().toLowerCase().equals(value.toLowerCase())) {
//                            addThingFromExternalField = true;
//                            break;
//                        }
//                    }
//                }
//            }


        }else {
            return true;
        }
        return false;
    }

    public static boolean applyFilter(ReportFilter reportFilter, FieldTypeCassandra fieldTypeCassandra, Map<String, Object> filters) {

        String filterValue = reportFilter.getValue();
        if(filters.containsKey(reportFilter.getId().toString())) {
            filterValue = filters.get(reportFilter.getId().toString()).toString();
        }
        String filterOperator = reportFilter.getOperator();
        return applyOperatorFilter(filterValue, fieldTypeCassandra.getValue(), filterOperator);
    }

    public static boolean applyOperatorFilter(String filterValue, String cassandraValue, String operator) {
        filterValue = filterValue  != null ? filterValue.toLowerCase() : "";
        cassandraValue = cassandraValue != null ? cassandraValue.toLowerCase() : "";

        if(operator.equals("=")) {
            return filterValue.equals(cassandraValue);
        }else if(operator.equals("!=")){
            return !filterValue.equals(cassandraValue);
        }else if(operator.equals("~")){
            return cassandraValue.contains(filterValue);
        }else if(operator.equals(">")){
            return ReportRuleUtils.isNumeric(filterValue + cassandraValue) &&  Double.valueOf(cassandraValue) > Double.valueOf(filterValue);
        }else if(operator.equals(">=")){
            return ReportRuleUtils.isNumeric(filterValue + cassandraValue) &&  Double.valueOf(cassandraValue) >= Double.valueOf(filterValue);
        }else if(operator.equals("<")){
            return ReportRuleUtils.isNumeric(filterValue + cassandraValue) &&  Double.valueOf(cassandraValue) < Double.valueOf(filterValue);
        }else if(operator   .equals("<=")){
            return ReportRuleUtils.isNumeric(filterValue + cassandraValue) &&  Double.valueOf(cassandraValue) <= Double.valueOf(filterValue);
        }
        return false;
    }

    //Utils methods
    public static Map<String, List<FieldTypeCassandra> > getFieldValuesByProperty( List<ThingTypeField> thingFieldIdsForProperties,
                                                                                    List<FieldTypeCassandra> fieldTypeCassandraList) {

        long t1 = System.currentTimeMillis();

        Map<String, List<FieldTypeCassandra> > fieldValuesCassandraMap = new HashMap<>();

        //thingFieldMap
        Map<Long, ThingTypeField> thingFieldMap = new HashMap<>();
        for(ThingTypeField thingTypeField : thingFieldIdsForProperties) {
            thingFieldMap.put(thingTypeField.getId(), thingTypeField);
        }

        for(FieldTypeCassandra fieldTypeCassandra : fieldTypeCassandraList) {
            ThingTypeField thingTypeField = thingFieldMap.get(fieldTypeCassandra.getThingTypeFieldId());
            if(thingTypeField != null) {
                if(!fieldValuesCassandraMap.containsKey(thingTypeField.getName())) {
                    fieldValuesCassandraMap.put(thingTypeField.getName(), new LinkedList<FieldTypeCassandra>());
                }
                List<FieldTypeCassandra> fieldTypeCassandraNewList = fieldValuesCassandraMap.get(thingTypeField.getName() );
                fieldTypeCassandraNewList.add(fieldTypeCassandra);

                fieldValuesCassandraMap.put(thingTypeField.getName(), fieldTypeCassandraNewList);
            }
        }

        long t2 = System.currentTimeMillis();
        logger.info( "Processing fieldValues Cassandra() execution_time=" + (t2 - t1 ) );

        return fieldValuesCassandraMap;

    }

    public static List<FieldTypeCassandra> convertArrayToFieldValueCassandra(Map<Long,Map<Long, Map<String, Object>>> fieldValuesMap) {
        List<FieldTypeCassandra> fieldTypeCassandraList = new LinkedList<>();
        for (Map.Entry<Long,Map<Long, Map<String, Object>>> thingValue : fieldValuesMap.entrySet()) {
            for (Map.Entry<Long, Map<String, Object>> fieldValue : thingValue.getValue().entrySet()) {
                Map<String, Object> fieldValuesList = fieldValue.getValue();
                if (fieldValuesList != null && fieldValuesList.size() > 0) {
                    Map<String, Object> fieldValueArray = fieldValuesList;
                    FieldTypeCassandra fieldTypeCassandra = new FieldTypeCassandra();
                    fieldTypeCassandra.setFieldValueFromObject(fieldValue.getKey(), fieldValueArray);
                    fieldTypeCassandraList.add(fieldTypeCassandra);
                }
            }


        }
        return fieldTypeCassandraList;
    }

    public static List<FieldTypeCassandra> orderFieldValueCassandra(List<FieldTypeCassandra> fieldTypeCassandraList,Set<Long> thingSet, String order) {
        List<FieldTypeCassandra> newFieldTypeCassandra = new LinkedList<>();
        for(FieldTypeCassandra fieldTypeCassandra : fieldTypeCassandraList) {
            if(thingSet.contains(fieldTypeCassandra.getThingId())) {
                newFieldTypeCassandra.add(fieldTypeCassandra);
            }
        }

        if(order.equals("DESC")) {
            sortingDescendig(newFieldTypeCassandra);
        }else {
            sortingAscendig(newFieldTypeCassandra);
        }
        return newFieldTypeCassandra;
    }

    public static void sortingAscendig(List<FieldTypeCassandra> newFieldTypeCassandra) {
        Collections.sort(newFieldTypeCassandra, new Comparator<FieldTypeCassandra>() {
            @Override
            public int compare(final FieldTypeCassandra object1, final FieldTypeCassandra object2) {
                String object1Value = object1.getValue() != null ? object1.getValue() : "";
                String object2Value = object2.getValue() != null ? object2.getValue() : "";
                return object1Value.compareTo(object2Value);
            }
        });
    }

    public static void sortingDescendig(List<FieldTypeCassandra> newFieldTypeCassandra) {
        Collections.sort(newFieldTypeCassandra, new Comparator<FieldTypeCassandra>() {
            @Override
            public int compare(final FieldTypeCassandra object1, final FieldTypeCassandra object2) {
                String object1Value = object1.getValue() != null ? object1.getValue() : "";
                String object2Value = object2.getValue() != null ? object2.getValue() : "";
                return object2Value.compareTo(object1Value);
            }
        });
    }

    public static List<Long> getIdsFromFieldList(List<ThingTypeField> thingTypeFields) {
        List<Long> thingFieldIds = new LinkedList<>();
        for(ThingTypeField thingField : thingTypeFields) {
            if(thingField != null) {
                thingFieldIds.add(thingField.getId());
            }
        }
        return thingFieldIds;
    }

    private static final String[]  THING_EXTERNAL_FIELDS_KEYS = new String[] {
            "zoneGroup.id", "localMap.id", "zoneProperty.id", "shift.id", "zoneType.id", "zoneCode.name", "zone.name"
    };

    public static boolean isZoneExternalProperty(String propertyName) {
        if(Arrays.asList(THING_EXTERNAL_FIELDS_KEYS).contains(propertyName)) {
            return true;
        }
        return false;
    }

    public static String getExtendPropertyIfApply(String propertyName) {
        if(propertyName.contains( "zone" )) {
            return "zone";
        }return propertyName;
    }
}
