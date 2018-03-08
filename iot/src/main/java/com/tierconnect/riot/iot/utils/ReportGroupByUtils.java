package com.tierconnect.riot.iot.utils;


import com.tierconnect.riot.iot.entities.CompositeThing;
import com.tierconnect.riot.iot.entities.ReportGroupBy;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.entities.ZoneGroup;
//import com.tierconnect.riot.iot.services.FieldValueService;
import com.tierconnect.riot.iot.services.ShiftService;

import com.tierconnect.riot.iot.entities.*;

import java.util.*;
/**
 * Created by user on 12/17/14.
 */
public class ReportGroupByUtils {


    private static String MIN_TYPE = "MIN";
    private static String MAX_TYPE = "MAX";
    private static String AVG_TYPE = "AVG";
    private static String SUM_TYPE = "SUM";
    private static String COUNT_TYPE = "COUNT";

    private static String GROUP = "group.";
    private static String SHIFT = "shift";
    private static String ZONE_GROUP = "zoneGroup.";
    private static String ZONE_CODE = "zoneCode";
    private static String NAME = "name";
    private static String SERIAL = "serial";

    private static String ZONE_PROPERTIES = "zoneProperty.id";


    public static Map<String, List<CompositeThing> > groupByProperties(List<ReportGroupBy> reportGroupByList,
                                                              List<CompositeThing> things,
                                                              Map<Long, Thing> childrenMap,
                                                              Map<Long, Map<String, Object> > fieldValues,
                                                              Map<Long, Zone> zoneListMap,
                                                              Map<String, ZonePropertyValue> zonePropertyValueMap) {
        ReportGroupBy reportGroupByX = null;
        ReportGroupBy reportGroupByY = null;
        if(reportGroupByList.size() > 0) reportGroupByX = reportGroupByList.get(0);
        if(reportGroupByList.size() > 1) reportGroupByY = reportGroupByList.get(1);

        Map<String, List<CompositeThing> > groupByList = new HashMap<>();

        Map<Long, String> fieldsMemoizationTable = new HashMap<>();

        for(CompositeThing thing : things) {
            //Getting values for X
            String keyMap = "";
            if(reportGroupByX != null) {
                boolean propertyFound = false;
                if(reportGroupByX.getPropertyName().equals(NAME)) {
                    keyMap = thing.getName();
                    propertyFound = true;
                }
                if(reportGroupByX.getPropertyName().equals(SERIAL)) {
                    keyMap = thing.getSerial();
                    propertyFound = true;
                }
                if(reportGroupByX.getPropertyName().contains(GROUP)) {
                    keyMap = thing.getGroup().getName();
                    propertyFound = true;
                }
                if(reportGroupByX.getPropertyName().contains(SHIFT)) {

                    ThingTypeField thingTypeField = thing.getThingTypeField(reportGroupByX.getPropertyName());

                    if (thingTypeField != null && fieldValues.containsKey(thingTypeField.getId())) {
                        String shiftsString = fieldValues.get(thingTypeField.getId()).containsKey("value") ?
                                fieldValues.get(thingTypeField.getId()).get("value").toString() :
                                "";

                        String []shiftList = shiftsString.split(",");
                        List<Long> shiftListIds = new LinkedList<>();
                        for(int it = 0; it < shiftList.length; it++) {
                            if(ReportRuleUtils.isNumeric(shiftList[it])) {
                                shiftListIds.add(Long.valueOf(shiftList[it].toString()));
                            }
                        }
                        if(shiftListIds.size() > 0) {
                            List<String> shiftListNames = ShiftService.getInstance().getShiftNamesFromIds(shiftListIds);

                            for(String shiftName : shiftListNames) {
                                keyMap = keyMap + shiftName + "-";
                            }

                            if(keyMap.length() > 0) {
                                keyMap = keyMap.substring(0, keyMap.length() - 1);
                            }
                        }

                    }
                    propertyFound = true;
                }
                if(reportGroupByX.getPropertyName().contains(ZONE_GROUP)) {
                    ThingTypeField zoneField = thing.getThingTypeField("zone");
                    if (zoneField == null) zoneField = childrenMap.get(thing.getId()) != null ? childrenMap.get(thing.getId()).getThingTypeField("zone") : null;
                    if (zoneField == null) zoneField = thing.getParent() != null ? thing.getParent().getThingTypeField("zone") : null;

                    if(zoneField != null) {
                        String value ;
                        if(fieldsMemoizationTable.containsKey(zoneField.getId())) {
                            value = fieldsMemoizationTable.get(zoneField.getId());
                        }else {
                            value = null;
                            //TODO FIX THIS METHOD OR DELETE IT
                            //value = FieldValueService.value(thing.getId(),zoneField.getId());
                            fieldsMemoizationTable.put(zoneField.getId(), value);
                        }
                        if(value != null && value.length() > 0) {
                            Zone zoneGroupBy = ReportUtils.getZoneFromZoneMap(zoneListMap, value);
                            if(zoneGroupBy != null) {
                                ZoneGroup zoneGroup = zoneGroupBy.getZoneGroup();
                                if(zoneGroup != null) {
                                    keyMap = zoneGroup.getName();
                                }else {
                                    keyMap = "";
                                }
                            }
                        }
                    }
                    propertyFound = true;
                }

                if(reportGroupByX.getPropertyName().contains(ZONE_CODE)) {
                    ThingTypeField zoneField = thing.getThingTypeField("zone");
                    if (zoneField == null) zoneField = childrenMap.get(thing.getId()) != null ? childrenMap.get(thing.getId()).getThingTypeField("zone") : null;
                    if (zoneField == null) zoneField = thing.getParent() != null ? thing.getParent().getThingTypeField("zone") : null;

                    if(zoneField != null) {
                        String value ;
                        if(fieldsMemoizationTable.containsKey(zoneField.getId())) {
                            value = fieldsMemoizationTable.get(zoneField.getId());
                        }else {
                            //TODO FIX THIS METHOD OR DELETE IT
                            //value = FieldValueService.value(thing.getId(),zoneField.getId());
                            value = null;
                            fieldsMemoizationTable.put(zoneField.getId(), value);
                        }
                        if(value != null && value.length() > 0) {
                            Zone zoneGroupBy = ReportUtils.getZoneFromZoneMap(zoneListMap, value);
                            if(zoneGroupBy != null) {
                                keyMap = zoneGroupBy.getCode();
                            }
                        }
                    }
                    propertyFound = true;
                }

                if(reportGroupByX.getPropertyName().contains(ZONE_PROPERTIES)) {
                    String groupByValue = reportGroupByX.getPropertyName();
                    String splitGroup[] = groupByValue.split(",");
                    if(splitGroup.length == 2) {
                        Long zonePropertyId = Long.valueOf(splitGroup[1]);
                        ThingTypeField thingTypeFieldZone = thing.getThingTypeField("zone");
                        String zoneFieldValue = "";
                        if(thingTypeFieldZone != null) {
                            if( fieldValues.containsKey(thingTypeFieldZone.getId())){
                                Map<String, Object> dataPointItem = fieldValues.get(thingTypeFieldZone.getId());
                                zoneFieldValue = dataPointItem.get("value").toString();
                            }
                            // rsejas: Todo: FindBugs redundant null check, refactor or delete it. (fieldValueMap always is null)
//                            else {
//                                //TODO FIX THIS METHOD OR DELETE IT
//                                //Map<String, Object> fieldValueMap = FieldValueService.valueTimeMap(thing.getId(), thingTypeFieldZone.getId());
//                                Map<String, Object> fieldValueMap = null;
//                                if(fieldValueMap != null && fieldValueMap.get("value") != null) {
//                                    zoneFieldValue = fieldValueMap.get("value").toString();
//                                    fieldValues.put(thingTypeFieldZone.getId(), fieldValueMap);
//                                }
//                            }
                            Zone zoneGroupBy = ReportUtils.getZoneFromZoneMap(zoneListMap, zoneFieldValue);
                            if (zoneGroupBy != null) {
                                ZonePropertyValue zonePropertyValue = zonePropertyValueMap.get(zoneGroupBy.getId() + "," + zonePropertyId);
                                if (zonePropertyValue != null) {
                                    keyMap = zonePropertyValue.getValue();
                                    propertyFound = true;
                                }
                            }
                        }
                    }
                }

                if(!propertyFound) {
                    ThingTypeField thingTypeField = thing.getThingTypeField(reportGroupByX.getPropertyName());
                    if (thingTypeField != null && fieldValues.containsKey(thingTypeField.getId())) {
                        keyMap = fieldValues.get(thingTypeField.getId()).containsKey("value") ?
                                fieldValues.get(thingTypeField.getId()).get("value").toString() :
                                "";
                    }else {
                        keyMap = "";
                    }
                }
                keyMap = keyMap.trim();
                if( keyMap.isEmpty() ) {
                    keyMap = "{}";
                }
            }

            //Getting values for X
            if(reportGroupByY != null) {
                boolean propertyFound = false;
                if(reportGroupByY.getPropertyName().equals(NAME)) {
                    keyMap = keyMap + "," + thing.getName();
                    propertyFound = true;
                }
                if(reportGroupByY.getPropertyName().equals(SERIAL)) {
                    keyMap = keyMap + "," + thing.getSerial();
                    propertyFound = true;
                }
                if(reportGroupByY.getPropertyName().contains(GROUP)) {
                    keyMap = keyMap + "," + thing.getGroup().getName();
                    propertyFound = true;
                }
                if(reportGroupByY.getPropertyName().contains(SHIFT)) {
                    ThingTypeField thingTypeField = thing.getThingTypeField(reportGroupByY.getPropertyName());

                    if (thingTypeField != null && fieldValues.containsKey(thingTypeField.getId())) {
                        String shiftsString = fieldValues.get(thingTypeField.getId()).containsKey("value")?
                                fieldValues.get(thingTypeField.getId()).get("value").toString() :
                                "";

                        String []shiftList = shiftsString.split(",");
                        List<Long> shiftListIds = new LinkedList<>();
                        for(int it = 0; it < shiftList.length; it++) {
                            if(ReportRuleUtils.isNumeric(shiftList[it])) {
                                shiftListIds.add(Long.valueOf(shiftList[it].toString()));
                            }
                        }
                        if(shiftListIds.size() > 0) {
                            List<String> shiftListNames = ShiftService.getInstance().getShiftNamesFromIds(shiftListIds);

                            StringBuilder shiftConcat = new StringBuilder();
                            for(String shiftName : shiftListNames) {
                                shiftConcat.append(shiftName + "-");
                            }
                            keyMap = keyMap  + "," +  shiftConcat.toString();
                            if(keyMap.length() > 0) {
                                keyMap = keyMap.substring(0, keyMap.length() - 1);
                            }
                        }

                    }
                    propertyFound = true;
                }

                if(reportGroupByY.getPropertyName().contains(ZONE_GROUP)) {
                    ThingTypeField zoneField = thing.getThingTypeField("zone");
                    if (zoneField == null) zoneField = childrenMap.get(thing.getId()) != null ? childrenMap.get(thing.getId()).getThingTypeField("zone") : null;
                    if (zoneField == null) zoneField = thing.getParent() != null ? thing.getParent().getThingTypeField("zone") : null;

                    if(zoneField != null) {
                        String value ;
                        if(fieldsMemoizationTable.containsKey(zoneField.getId())) {
                            value = fieldsMemoizationTable.get(zoneField.getId());
                        }else {
                            //TODO FIX THIS METHOD OR DELETE IT
                            //value = FieldValueService.value(thing.getId(),zoneField.getId());
                            value = null;
                            fieldsMemoizationTable.put(zoneField.getId(), value);
                        }
                        if(value != null && value.length() > 0) {
                            Zone zoneGroupBy = ReportUtils.getZoneFromZoneMap(zoneListMap, value);
                            if(zoneGroupBy != null) {
                                ZoneGroup zoneGroup = zoneGroupBy.getZoneGroup();
                                if(zoneGroup != null) {
                                    keyMap = keyMap + "," + zoneGroup.getName();
                                }else {
                                    keyMap = keyMap + ",";
                                }
                            }
                        }
                    }
                    propertyFound = true;
                }

                if(reportGroupByY.getPropertyName().contains(ZONE_CODE)) {
                    ThingTypeField zoneField = thing.getThingTypeField("zone");
                    if (zoneField == null) zoneField = childrenMap.get(thing.getId()) != null ? childrenMap.get(thing.getId()).getThingTypeField("zone") : null;
                    if (zoneField == null) zoneField = thing.getParent() != null ? thing.getParent().getThingTypeField("zone") : null;

                    if(zoneField != null) {
                        String value ;
                        if(fieldsMemoizationTable.containsKey(zoneField.getId())) {
                            value = fieldsMemoizationTable.get(zoneField.getId());
                        }else {
                            //TODO FIX THIS METHOD OR DELETE IT
                            //value = FieldValueService.value(zoneField.getId());
                            value = null;
                            fieldsMemoizationTable.put(zoneField.getId(), value);
                        }
                        if(value != null && value.length() > 0) {
                            Zone zoneGroupBy = ReportUtils.getZoneFromZoneMap(zoneListMap, value);
                            if(zoneGroupBy != null) {
                                keyMap = keyMap + "," + zoneGroupBy.getCode();
                            }
                        }
                    }
                    propertyFound = true;
                }

                if(reportGroupByY.getPropertyName().contains(ZONE_PROPERTIES)) {
                    String groupByValue = reportGroupByY.getPropertyName();
                    String splitGroup[] = groupByValue.split(",");
                    if(splitGroup.length == 2) {
                        Long zonePropertyId = Long.valueOf(splitGroup[1]);
                        ThingTypeField thingFieldZone = thing.getThingTypeField("zone");
                        String zoneFieldValue = "";
                        if(thingFieldZone != null) {
                            if( fieldValues.containsKey( thingFieldZone.getId() )){
                                Map<String, Object> dataPointItem = fieldValues.get(thingFieldZone.getId());
                                zoneFieldValue = dataPointItem.get("value").toString();
                            }
                            // rsejas: Todo: FindBugs redundant null check, refactor or delete it. (fieldValueMap always is null)
//                            else {
//                                //TODO FIX THIS METHOD OR DELETE IT
//                                //Map<String, Object> fieldValueMap = FieldValueService.valueTimeMap(thing.getId(), thingFieldZone.getId());
//                                Map<String, Object> fieldValueMap = null;
//                                if(fieldValueMap != null && fieldValueMap.get("value") != null) {
//                                    zoneFieldValue = fieldValueMap.get("value").toString();
//                                    fieldValues.put(thingFieldZone.getId(), fieldValueMap);
//                                }
//                            }
                            Zone zoneGroupBy = ReportUtils.getZoneFromZoneMap(zoneListMap, zoneFieldValue);
                            if (zoneGroupBy != null) {
                                ZonePropertyValue zonePropertyValue = zonePropertyValueMap.get(zoneGroupBy.getId() + "," + zonePropertyId);
                                if (zonePropertyValue != null) {
                                    keyMap = keyMap + "," + zonePropertyValue.getValue();
                                    propertyFound = true;
                                }
                            }
                        }
                    }
                }

                if(!propertyFound) {
                    ThingTypeField thingTypeField = thing.getThingTypeField(reportGroupByY.getPropertyName());
                    if (thingTypeField != null) {
                        if (fieldValues.containsKey(thingTypeField.getId())) {
                            String secondKey = fieldValues.get(thingTypeField.getId()).containsKey("value") ? fieldValues.get(thingTypeField.getId()).get("value").toString() : "";
                            secondKey = secondKey.trim();
                            if (secondKey.length() > 0) {
                                keyMap = keyMap + "," + secondKey;
                            } else {
                                keyMap = keyMap + ",";
                            }
                        }else {
                            String secondKey = "";
                            if (secondKey.length() > 0) {
                                keyMap = keyMap + "," + secondKey;
                            } else {
                                keyMap = keyMap + ",";
                            }
                        }
                    }
                }
            }
            keyMap = keyMap.trim();
            if(keyMap.isEmpty()) keyMap = "{},{}";
            if(keyMap.split(",").length == 1) keyMap = keyMap.replace(",","") + ",{}";
            if(keyMap.endsWith(",")) keyMap = keyMap + "{}";

            boolean includeInMap = true;

            if(reportGroupByX != null && ( reportGroupByX.getOther() == null || !reportGroupByX.getOther() )) {
                if(keyMap.startsWith("{}")) includeInMap = false;
            }
            if(reportGroupByY != null && ( reportGroupByY.getOther() == null || !reportGroupByY.getOther() )) {
                if(keyMap.endsWith("{}")) includeInMap = false;
            }
            if(reportGroupByY == null) {
                keyMap = keyMap.replace(",{}","");
            }
            if(includeInMap) {
                if (groupByList.containsKey(keyMap)) {
                    List<CompositeThing> thingListForMap = groupByList.get(keyMap);
                    thingListForMap.add(thing);
                    groupByList.put(keyMap, thingListForMap);
                } else {
                    List<CompositeThing> newThingList = new LinkedList<>();
                    newThingList.add(thing);
                    if (keyMap.length() > 0) groupByList.put(keyMap, newThingList);
                }
            }
        }
        return groupByList;
    }

    public static Map<String, Object> summarizeByProperty(Map<String, List<CompositeThing> > groupByThingMap,
                                                          Map<Long, Thing> childrenMap,
                                                          Map<Long, Map<String, Object> > datapointsMap,
                                                          String summarizeBy,
                                                          String functionEval) {
        Map<String, Object> summarizeMap = new HashMap<>();

        for(Map.Entry<String, List<CompositeThing> > summarizeItem : groupByThingMap.entrySet()) {
            List<CompositeThing> thingList = summarizeItem.getValue();
            Double summarizeRes = 0.0;
            for(CompositeThing thing : thingList) {
                ThingTypeField thingTypeField = thing.getThingTypeField(summarizeBy);

                if (thingTypeField != null) {
                    List<Object[]> dataPoint = new LinkedList<>();
                    if(datapointsMap.containsKey(thingTypeField.getId())) {
                        Map<String, Object> dataPointItem = datapointsMap.get( thingTypeField.getId() );
                        if(dataPointItem != null && dataPointItem.containsKey("time") && dataPointItem.containsKey("value")) {
                            dataPoint.add(new Object[] {dataPointItem.get("time"), dataPointItem.get("value")} );
                        }
                    }
                    String value  = dataPoint != null && dataPoint.size() > 0 ? dataPoint.get(0)[1].toString() : "";
                    summarizeRes = makeFunctionOperation(functionEval, summarizeRes, value);
                } else {
                    summarizeRes = makeFunctionOperation(COUNT_TYPE, summarizeRes, "1");
                }
            }
            summarizeMap.put(summarizeItem.getKey(), summarizeRes);
        }
        return sortByValue(summarizeMap);
    }

//    public static ThingField getThingFieldForGroupBy(CompositeThing thing,
//                                                     String propertyName) {
//        if(propertyName == null) return null;
//        ThingField thingField = thing.getThingTypeField( propertyName != null ? propertyName : "");
//
//        return thingField;
//    }

    public static Object[] getCategoriesList (Map<String, Object> groupByThingResult) {
        Set<String> categories = new LinkedHashSet<>();
        Set<String> series = new LinkedHashSet<>();
        for (Map.Entry<String, Object> groupByElem : groupByThingResult.entrySet()) {
            String[] groupByArray = groupByElem.getKey().split(",");
            if(groupByArray.length > 0) categories.add(groupByArray[0]);
            if(groupByArray.length > 1) series.add(groupByArray[1]);
        }
        List<String> seriesList = new ArrayList<>(series);
        Collections.sort(seriesList);

        series = new LinkedHashSet<>();
        for(String value : seriesList) {
            series.add(value);
        }
        return new Object[]{ categories, series };
    }

    private static Double makeFunctionOperation(String func, Double previousValue, String nextValueStr) {

        Double nextValue = 0.0;
        boolean isNumeric = ReportRuleUtils.isNumeric( nextValueStr );

        if (isNumeric) {
            nextValue = Double.valueOf(nextValueStr);
        }else {
            //Convert to Double
            func = COUNT_TYPE;
        }

        //Make Operations
        if(func.equals(MIN_TYPE)) {
            return Math.min(previousValue, nextValue);
        }else if(func.equals(MAX_TYPE)) {
            return Math.max(previousValue, nextValue);
        }else if(func.equals(AVG_TYPE)) {
            return previousValue + nextValue;
        }else if(func.equals(SUM_TYPE)) {
            return previousValue + nextValue;
        }else if(func.equals(COUNT_TYPE)) {
            return previousValue + 1;
        }
        return 0.0;
    }

    public static Map<String, Object[] > getSeriesResult(
              Map<String, Object> groupByThingResult
            , Set<String> categories
            , Set<String> series
            , boolean verticalTotal
            , boolean horizontalTotal) {
        Map<String, Object[] > seriesResultMap = new LinkedHashMap<>();

        ArrayList seriesDataTotal = new ArrayList(Collections.nCopies(Math.max(series.size(), 1), 0));

        Double totalSeries = 0.0;
        for (String categoryLabel : categories) {
            //Verify if the report needs to show vertical total
            int size = 0;
            if(verticalTotal)
            {
                size =series.size() + 1;
            }else
            {
                size = series.size();
            }
            ArrayList seriesData = new ArrayList(Collections.nCopies(size, 0));

            int it = 0;
            if(series.size() > 0) {
                Double totalRow = 0.0;
                for (String seriesLabel : series) {
                    String mapKey = ( categoryLabel + "," + seriesLabel );
                    if (groupByThingResult.containsKey(mapKey)) {
                        totalRow = totalRow + Double.valueOf(groupByThingResult.get(mapKey).toString());
                        seriesData.set(it, groupByThingResult.get(mapKey));
                        seriesDataTotal.set(it, Double.valueOf(seriesData.get(it).toString()) + Double.valueOf(seriesDataTotal.get(it).toString()) );
                    } else {
                        seriesData.set(it, 0.0);
                        seriesDataTotal.set(it, seriesDataTotal.get(it));
                    }
                    it++;
                }
                totalSeries = totalSeries + totalRow;
                if(verticalTotal) {
                    seriesData.set(seriesData.size() - 1, totalRow);
                }
                seriesResultMap.put(categoryLabel, seriesData.toArray());
            }else {
                seriesResultMap.put(categoryLabel, new Object[]{ groupByThingResult.get(categoryLabel) } );
                if(groupByThingResult.containsKey(categoryLabel)) {
                    seriesDataTotal.set(0, Double.valueOf(seriesDataTotal.get(0).toString()) + Double.valueOf(groupByThingResult.get(categoryLabel).toString()));
                }
            }
        }

        seriesResultMap = sortByKey(seriesResultMap);

        Map<String, Object[] > seriesMapRes = new LinkedHashMap<>();

        for(Map.Entry<String, Object[] > groupByItem : seriesResultMap.entrySet()) {
            Double value = 0.0;
            for(int it=0; it<groupByItem.getValue().length; it++) {
                if(groupByItem.getValue()[it] != null) {
                    value = value + Double.valueOf((groupByItem.getValue()[it].toString()));
                }
            }
            if(value > 0) seriesMapRes.put(groupByItem.getKey().replace("{","").replace("}", ""), groupByItem.getValue());
        }

        /*Total series*/
        //The total of vertical and horizontal total is displayed only if both are selected
        if(series.size() > 0 && horizontalTotal && verticalTotal) {
            seriesDataTotal.add(totalSeries);
        }
        if(verticalTotal) {
            series.add("Total");
        }
        //Verify if the report needs to show horizontal total
        if(horizontalTotal) {
            seriesMapRes.put("Total", seriesDataTotal.toArray());
        }

        /*Total series*/
        //The total of vertical and horizontal total is displayed only if both are selected
//        if(series.size() > 0)
//        {
//            seriesDataTotal.add(totalSeries);
//            if( horizontalTotal && verticalTotal) {
//                series.add("Total"); //Vertical Title ->Total
//                seriesMapRes.put("Total", seriesDataTotal.toArray()); //Horizontal title + total
//            }else if( !horizontalTotal && verticalTotal) {
//                int its = 0;
//                for (String categoryLabel : categories)
//                {
//                    seriesDataTotal.set(its, "");
//                    its++;
//                }
//                series.add("Total"); //Vertical Title ->Total
//                seriesMapRes.put("Total", seriesDataTotal.toArray());//Horizontal title + total
//            }else if( horizontalTotal && !verticalTotal) {
//                seriesMapRes.put("Total", seriesDataTotal.toArray());//Horizontal title + total
//            }
//        }

        return seriesMapRes;
    }

    public static Map sortByValue(Map unsortMap) {
        List list = new LinkedList(unsortMap.entrySet());

        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o2)).getValue())
                        .compareTo(((Map.Entry) (o1)).getValue());
            }
        });

        Map sortedMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    public static Map sortByKey(Map unsortMap) {
        Map<String, Object[]> treeMap = new TreeMap<String, Object[]>(unsortMap);

        Map<String, Object> mapRes = new LinkedHashMap<>();
        for(Map.Entry<String, Object[]> treeMapItem : treeMap.entrySet()) {
            mapRes.put(treeMapItem.getKey(), treeMapItem.getValue());
        }
        return mapRes;
    }


    public static String getCategoriesLabel(List<ReportGroupBy> reportGroupByList) {
        return reportGroupByList.size() > 0 ? reportGroupByList.get(0).getLabel() : "";
    }

    public static String getSeriesLabel(List<ReportGroupBy> reportGroupByList) {
        return reportGroupByList.size() > 1 ? reportGroupByList.get(1).getLabel() : "";
    }


}
