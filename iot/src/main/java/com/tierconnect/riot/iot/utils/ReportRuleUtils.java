package com.tierconnect.riot.iot.utils;

import com.tierconnect.riot.iot.entities.*;
//import com.tierconnect.riot.iot.services.FieldValueService;
import com.tierconnect.riot.iot.services.ZoneGroupService;
import com.tierconnect.riot.iot.services.ZonePropertyValueService;
import com.tierconnect.riot.iot.services.ZoneService;

import java.util.*;

/**
 * Created by arojas on 11/25/14.
 */
public class ReportRuleUtils {

    public static void addDataToTimestampPoints(Map<Long, Object[] > timestampPoints,
                                                ReportRule reportRule,
                                                Thing thing,
                                                Thing parentThing,
                                                Thing childThing,
                                                Map<Long, List<Object[]>> dataPointsMap,
                                                Long timeStampInit) {
        String propertyName = reportRule.getPropertyName();
        boolean isDwellTimeSeries = false;

        if(propertyName != null && propertyName.contains("dwellTime(")) {
            propertyName = ReportExecutionUtils.removeDwellTimeString(propertyName);
            isDwellTimeSeries = true;
        }

        ThingTypeField thingTypeField = thing.getThingTypeField(propertyName);
        if(thingTypeField == null && childThing != null) {
            thingTypeField = childThing.getThingTypeField(propertyName);
        }
        if(thingTypeField == null && parentThing != null) {
            thingTypeField = parentThing.getThingTypeField(propertyName);
        }

        if(thingTypeField != null) {
            List<Object[]> pointsMap = dataPointsMap.get(thingTypeField.getId());

            boolean isNotTimeSeries = false;
            if(thingTypeField.getTimeSeries() == null || !thingTypeField.getTimeSeries()) {
                isNotTimeSeries = true;
            }

            //We have to generate more values
            Long dwellTimeValue = 0L;
            if(isDwellTimeSeries) {
                if(reportRule.getValue() != null && reportRule.getValue().length() > 0) {
                    dwellTimeValue =  Double.valueOf((String) reportRule.getValue()).longValue();
                }
                Object[] pointsMapAux = null;

                if(pointsMap != null && pointsMap.size() == 1) {
                    Object[] pointsMapIni = pointsMap.get(0);

                    dwellTimeValue = new Date().getTime() -  ((Date)(pointsMap.get(0)[0])).getTime();
                    Object[] pointsMapTmp = new Object[]{
                            pointsMapIni[0], dwellTimeValue.toString()};
                    timestampPoints.put(((Date) pointsMapTmp[0]).getTime(), pointsMapTmp);

                }else {
                    for (int it = 0; pointsMap != null && it < pointsMap.size(); it++) {
                        Object[] pointsMapIni = pointsMap.get(it);
                        Long dwellTimeValueIni = 0L;

                        boolean pointAdded = false;
                        if (it > 0) {
                            dwellTimeValueIni = ((Date) (pointsMap.get(it)[0])).getTime() - ((Date) (pointsMap.get(it - 1)[0])).getTime();
                        } else {
                            Object[] pointsMapTmp = new Object[]{
                                    pointsMapIni[0], dwellTimeValueIni.toString()};
                            timestampPoints.put(((Date) pointsMapIni[0]).getTime(), pointsMapTmp);
                            pointAdded = true;
                        }

                        if (dwellTimeValue < dwellTimeValueIni && pointsMapAux != null) {
                            Object[] pointsMapTmp = new Object[]{
                                    new Date(((Date) pointsMapAux[0]).getTime() + dwellTimeValue), dwellTimeValue.toString()};
                            timestampPoints.put(((Date) pointsMapTmp[0]).getTime(), pointsMapTmp);
                            pointAdded = true;
                        }
                        if (pointAdded && it != pointsMap.size() - 1) {
                            Object[] pointsMapTmp = new Object[]{new Date(((Date) pointsMapIni[0]).getTime() + 1000), "1000"};
                            timestampPoints.put(((Date) pointsMapTmp[0]).getTime(), pointsMapTmp);
                        } else {
                            if (pointsMapIni[0] != null) {
                                Object[] pointsMapTmp = new Object[]{new Date(((Date) pointsMapIni[0]).getTime() + 1000), (new Date().getTime() - ((Date) pointsMapIni[0]).getTime()) + ""};
                                timestampPoints.put(((Date) pointsMapIni[0]).getTime(), pointsMapTmp);
                            }
                        }
                        pointsMapAux = pointsMapIni;
                    }
                }
            }else {
                if (timeStampInit != null) {
                    for (int it = 0; pointsMap != null && it < pointsMap.size(); it++) {
                        Object[] pointsMapIni = pointsMap.get(it);
                        if (isNotTimeSeries) {
                            Object newObj[] = new Object[2];
                            newObj[0] = new Date(timeStampInit);
                            newObj[1] = pointsMapIni[1] + "," + thingTypeField.getId().toString();
                            if (timestampPoints != null) {
                                for (int itOb = 1; timestampPoints.get(timeStampInit) != null &&
                                        itOb < timestampPoints.get(timeStampInit).length; itOb++) {
                                    newObj = addAndRemoveDuplicatedInArray(newObj, timestampPoints.get(timeStampInit)[itOb]);
                                }
                                timestampPoints.put(timeStampInit, newObj);
                            }
//                        addAndRemoveDuplicatedInArray(newObj, pointsMapIni);
                        } else {
                            timestampPoints.put(((Date) pointsMapIni[0]).getTime(), new Object[]{
                                    pointsMapIni[0],
                                    pointsMapIni[1] + "," + thingTypeField.getId().toString()
                            });
                        }
                    }
                }
            }
        }
    }


    /**
     * Created for fieldTypeId
     * @param timestampPoints
     * @param reportRule
     * @param thing
     * @param parentThing
     * @param childThing
     * @param dataPointsMap
     * @param timeStampInit
     */
    public static void addDataToTimestampPoints2(Map<Long, Object[] > timestampPoints,
                                                ReportRule reportRule,
                                                Thing thing,
                                                Thing parentThing,
                                                Thing childThing,
                                                Map<Long, List<Object[]>> dataPointsMap,
                                                Long timeStampInit) {
        String propertyName = reportRule.getPropertyName();
        boolean isDwellTimeSeries = false;

        if(propertyName != null && propertyName.contains("dwellTime(")) {
            propertyName = ReportExecutionUtils.removeDwellTimeString(propertyName);
            isDwellTimeSeries = true;
        }

        ThingTypeField thingTypeField = thing.getThingTypeField(propertyName);
        if(thingTypeField == null && childThing != null) {
            thingTypeField = childThing.getThingTypeField(propertyName);
        }
        if(thingTypeField == null && parentThing != null) {
            thingTypeField = parentThing.getThingTypeField(propertyName);
        }

        if(thingTypeField != null) {
            List<Object[]> pointsMap = dataPointsMap.get(thingTypeField.getId());

            boolean isNotTimeSeries = false;
            if(thingTypeField.getTimeSeries() == null || !thingTypeField.getTimeSeries()) {
                isNotTimeSeries = true;
            }

            //We have to generate more values
            Long dwellTimeValue = 0L;
            if(isDwellTimeSeries) {
                if(reportRule.getValue() != null && reportRule.getValue().length() > 0) {
                    dwellTimeValue =  Double.valueOf((String) reportRule.getValue()).longValue();
                }
                Object[] pointsMapAux = null;

                if(pointsMap != null && pointsMap.size() == 1) {
                    Object[] pointsMapIni = pointsMap.get(0);

                    dwellTimeValue = new Date().getTime() -  ((Date)(pointsMap.get(0)[0])).getTime();
                    Object[] pointsMapTmp = new Object[]{
                            pointsMapIni[0], dwellTimeValue.toString()};
                    timestampPoints.put(((Date) pointsMapTmp[0]).getTime(), pointsMapTmp);

                }else {
                    for (int it = 0; pointsMap != null && it < pointsMap.size(); it++) {
                        Object[] pointsMapIni = pointsMap.get(it);
                        Long dwellTimeValueIni = 0L;

                        boolean pointAdded = false;
                        if (it > 0) {
                            dwellTimeValueIni = ((Date) (pointsMap.get(it)[0])).getTime() - ((Date) (pointsMap.get(it - 1)[0])).getTime();
                        } else {
                            Object[] pointsMapTmp = new Object[]{
                                    pointsMapIni[0], dwellTimeValueIni.toString()};
                            timestampPoints.put(((Date) pointsMapIni[0]).getTime(), pointsMapTmp);
                            pointAdded = true;
                        }

                        if (dwellTimeValue < dwellTimeValueIni && pointsMapAux != null) {
                            Object[] pointsMapTmp = new Object[]{
                                    new Date(((Date) pointsMapAux[0]).getTime() + dwellTimeValue), dwellTimeValue.toString()};
                            timestampPoints.put(((Date) pointsMapTmp[0]).getTime(), pointsMapTmp);
                            pointAdded = true;
                        }
                        if (pointAdded && it != pointsMap.size() - 1) {
                            Object[] pointsMapTmp = new Object[]{new Date(((Date) pointsMapIni[0]).getTime() + 1000), "1000"};
                            timestampPoints.put(((Date) pointsMapTmp[0]).getTime(), pointsMapTmp);
                        } else {
                            if (pointsMapIni[0] != null) {
                                Object[] pointsMapTmp = new Object[]{new Date(((Date) pointsMapIni[0]).getTime() + 1000), (new Date().getTime() - ((Date) pointsMapIni[0]).getTime()) + ""};
                                timestampPoints.put(((Date) pointsMapIni[0]).getTime(), pointsMapTmp);
                            }
                        }
                        pointsMapAux = pointsMapIni;
                    }
                }
            }else {
                for (int it = 0; pointsMap != null && it < pointsMap.size(); it++) {
                    Object[] pointsMapIni = pointsMap.get(it);
                    if(isNotTimeSeries) {
                        Object newObj[] = new Object[2];
                        newObj[0] = new Date(timeStampInit);
                        newObj[1] = pointsMapIni[1] + "," + thingTypeField.getId().toString();
                        if (timestampPoints != null) {
                            for (int itOb = 1; timestampPoints.get(timeStampInit) != null &&
                                    itOb < timestampPoints.get(timeStampInit).length; itOb++) {
                                newObj = addAndRemoveDuplicatedInArray(newObj, timestampPoints.get(timeStampInit)[itOb]);
                            }
                            timestampPoints.put(timeStampInit, newObj);
                        }
//                        addAndRemoveDuplicatedInArray(newObj, pointsMapIni);
                    } else {
                        timestampPoints.put(((Date) pointsMapIni[0]).getTime(), new Object[]{
                                pointsMapIni[0],
                                pointsMapIni[1] + "," + thingTypeField.getId().toString()
                        });
                    }
                }
            }
        }
    }

    public static void addingMissingPoints(ReportRule reportRule,
                                           Map<Long, Object[]> timestampPoints,
                                           Thing thing,
                                           Thing parentThing,
                                           Thing childThing,
                                           Map<Long, List<Object[]>>dataPointsMap,
                                           Long timeStampInit,
                                           Map<Long, Zone> zoneListMap,
                                           Map<String, ZonePropertyValue> zonePropertyValueMap) {
        String propertyName = reportRule.getPropertyName();

        ThingTypeField thingTypeField = thing.getThingTypeField(propertyName);
        if(thingTypeField == null && childThing != null) {
            thingTypeField = childThing.getThingTypeField(propertyName);
        }
        if(thingTypeField == null && parentThing != null) {
            thingTypeField = parentThing.getThingTypeField(propertyName);
        }

        boolean propertySerialName = false;
        propertyName = propertyName.replace("parent.", "");
        if(propertyName.equals("name")) {
            for(Map.Entry<Long, Object[]> timestampObj : timestampPoints.entrySet()) {
                ArrayList<Object> temp = new ArrayList<>(Arrays.asList(timestampObj.getValue()));
                temp.add(thing.getName());

                if(childThing != null) {
                    temp.add(childThing.getName());
                }
                Object items[] = new Object[10];
                for(int i = 0; i < temp.size(); i++) {
                    if(i > 0) {
                        items = addAndRemoveDuplicatedInArray(items, temp.get(i));
                    }else {
                        items = addAndRemoveDuplicatedInArray(timestampObj.getValue(), temp.get(i));
                    }
                }
                timestampPoints.put(timestampObj.getKey(), items);
            }
            if(timestampPoints.size() == 0) {
                timestampPoints.put(timeStampInit, new Object[] { new Date(timeStampInit), thing.getName() });
            }
            propertySerialName = true;
        }
        if(propertyName.equals("serial")) {
            for (Map.Entry<Long, Object[]> timestampObj : timestampPoints.entrySet()) {
                ArrayList<Object> temp = new ArrayList<Object>(Arrays.asList(timestampObj.getValue()));
                temp.add(thing.getSerial());
                if (childThing != null) {
                    temp.add(childThing.getSerial());
                }
                Object items[] = new Object[10];
                for(int i = 0; i < temp.size(); i++) {
                    if(i > 0) {
                        items = addAndRemoveDuplicatedInArray(items, temp.get(i));
                    }else {
                        items = addAndRemoveDuplicatedInArray(timestampObj.getValue(), temp.get(i));
                    }
                }
                timestampPoints.put(timestampObj.getKey(), items);
            }
            if(timestampPoints.size() == 0) {
                timestampPoints.put(timeStampInit, new Object[] { new Date(timeStampInit), thing.getSerial() });
            }
            propertySerialName = true;
        }
        if(propertyName.contains("zoneGroup") ||
                propertyName.contains("zoneCode") ||
                propertyName.contains("zoneType") ||
                propertyName.contains("zoneProperty") ||
                propertyName.contains("zone.name")
                ) {

            ThingTypeField zoneField = null;
            if(propertyName.contains("zone.name")) {
                List<ThingTypeField> zoneFieldList = thing.getThingTypeFieldByType(ThingTypeField.Type.TYPE_ZONE.value);
                if(zoneFieldList != null && zoneFieldList.size() > 0) {
                    zoneField = zoneFieldList.get(0);
                }
                if(zoneField == null && childThing != null) {
                    zoneFieldList = childThing.getThingTypeFieldByType(ThingTypeField.Type.TYPE_ZONE.value);
                    if(zoneFieldList != null && zoneFieldList.size() > 0) {
                        zoneField = zoneFieldList.get(0);
                    }
                }
                if(zoneField == null && parentThing != null) {
                    zoneFieldList = parentThing.getThingTypeFieldByType(ThingTypeField.Type.TYPE_ZONE.value);
                    if(zoneFieldList != null && zoneFieldList.size() > 0) {
                        zoneField = zoneFieldList.get(0);
                    }
                }
            }
            if(zoneField == null) {
                zoneField = thing.getThingTypeField("zone");
                if (zoneField == null && childThing != null) childThing.getThingTypeField("zone");
                if (zoneField == null && parentThing != null) parentThing.getThingTypeField("zone");
            }


            Zone zoneEntry = null;
//            TODO: Redundant null check found by find bugs, delete it
//            if (zoneField != null) {
//                String value = null;
//                //TODO FIX THIS METHOD OR DELETE IT
//                //String value = FieldValueService.value(thing.getId(), zoneField.getId());
//                if (value != null) {
//                    if(propertyName.equals("zone.name")) {
//                        zoneEntry = ReportUtils.getZoneFromZoneCode(zoneListMap, value);
//                    }
//                    if(zoneEntry == null) {
//                        zoneEntry = ReportUtils.getZoneFromZoneMap(zoneListMap, value);
//                    }
//                }
//            }

            // TODO: Redundant null check found by find bugs, delete it
//            if (propertyName.contains("zone.name")) {
//                if (zoneEntry != null) {
//                    for (Map.Entry<Long, Object[]> timestampObj : timestampPoints.entrySet()) {
//                        ArrayList<Object> temp = new ArrayList<>(Arrays.asList(timestampObj.getValue()));
//                        temp.add(zoneEntry.getName());
//
//                        Object items[] = new Object[10];
//                        for (int i = 0; i < temp.size(); i++) {
//                            if (i > 0) {
//                                items = addAndRemoveDuplicatedInArray(items, temp.get(i));
//                            } else {
//                                items = addAndRemoveDuplicatedInArray(timestampObj.getValue(), temp.get(i));
//                            }
//                        }
//                        if (timestampPoints!=null){
//                            timestampPoints.put(timestampObj.getKey(), items);
//                        }
//                    }
//                    if (timestampPoints.size() == 0) {
//                        timestampPoints.put(timeStampInit, new Object[]{new Date(timeStampInit), zoneEntry.getName()});
//                    }
//                }
//                propertySerialName = true;
//            }

            if (propertyName.contains("zone.name")) {
//            TODO: Redundant null check found by find bugs, delete it
//                if (zoneEntry != null) {
//                    for (Map.Entry<Long, Object[]> timestampObj : timestampPoints.entrySet()) {
//                        ArrayList<Object> temp = new ArrayList<>(Arrays.asList(timestampObj.getValue()));
//                        temp.add(zoneEntry.getName());
//
//                        Object items[] = new Object[10];
//                        for (int i = 0; i < temp.size(); i++) {
//                            if (i > 0) {
//                                items = addAndRemoveDuplicatedInArray(items, temp.get(i));
//                            } else {
//                                items = addAndRemoveDuplicatedInArray(timestampObj.getValue(), temp.get(i));
//                            }
//                        }
//                        timestampPoints.put(timestampObj.getKey(), items);
//                    }
//                    if (timestampPoints.size() == 0) {
//                        timestampPoints.put(timeStampInit, new Object[]{new Date(timeStampInit), zoneEntry.getName()});
//                    }
//                }
                propertySerialName = true;
            }


            if (propertyName.contains("zoneGroup")) {
//            TODO: Redundant null check found by find bugs, delete it
//                if (zoneEntry != null) {
//                    ZoneGroup zoneGroup = zoneEntry.getZoneGroup();
//                    if (zoneGroup != null) {
//                        for (Map.Entry<Long, Object[]> timestampObj : timestampPoints.entrySet()) {
//                            ArrayList<Object> temp = new ArrayList<>(Arrays.asList(timestampObj.getValue()));
//                            temp.add(zoneGroup.getName());
//
//                            Object items[] = new Object[10];
//                            for (int i = 0; i < temp.size(); i++) {
//                                if (i > 0) {
//                                    items = addAndRemoveDuplicatedInArray(items, temp.get(i));
//                                } else {
//                                    items = addAndRemoveDuplicatedInArray(timestampObj.getValue(), temp.get(i));
//                                }
//                            }
//                            timestampPoints.put(timestampObj.getKey(), items);
//                        }
//                        if (timestampPoints.size() == 0) {
//                            timestampPoints.put(timeStampInit, new Object[]{new Date(timeStampInit), zoneGroup.getName()});
//                        }
//                    }
//                }
                propertySerialName = true;
            }

            if (propertyName.contains("zoneCode")) {
                //Get ZoneGroup from a thing
                if (zoneField != null) {
//            TODO: Redundant null check found by find bugs, delete it
//                    if (!dataPointsMap.containsKey(zoneField.getId())) {
//                        Map<Long, List<Object[]>> fieldZoneMap = null;
//                        //TODO FIX THIS METHOD OR DELETE IT
//                        //Map<Long, List<Object[]>> fieldZoneMap = FieldValueService.getFieldsValues(new LinkedList<>(Arrays.asList(zoneField.getId())));
//                        dataPointsMap.put(zoneField.getId(), fieldZoneMap.get(zoneField.getId()));
//                    }
                    List<Object[]> pointsMap = dataPointsMap.get(zoneField.getId());
                    if (pointsMap != null && pointsMap.size() > 0) {
                        for (Object[] pointsItem : pointsMap) {
                            String zoneName = pointsItem[1].toString();
                            List<Zone> zoneList = ZoneService.getZonesByName(zoneName);
                            if (zoneList != null && zoneList.size() > 0) {
                                String zoneCode = zoneList.get(0).getCode();
                                Long timestampZoneCode = ((Date) (pointsItem[0])).getTime();
                                if (timestampPoints.containsKey(timestampZoneCode)) {
                                    timestampPoints.put(timestampZoneCode, addAndRemoveDuplicatedInArray(timestampPoints.get(timestampZoneCode), zoneCode));
                                } else {
                                    timestampPoints.put(timestampZoneCode, new Object[]{new Date(timestampZoneCode), zoneCode});
                                }
                            }
                        }
                    }
                }

                ZoneGroup zoneGroup = ZoneGroupService.getInstance().getZoneForThing(thing);
                if (zoneGroup == null) ZoneGroupService.getInstance().getZoneForThing(childThing);
                if (zoneGroup == null) ZoneGroupService.getInstance().getZoneForThing(parentThing);

                if (zoneGroup != null) {
                    for (Map.Entry<Long, Object[]> timestampObj : timestampPoints.entrySet()) {
                        ArrayList<Object> temp = new ArrayList<>(Arrays.asList(timestampObj.getValue()));
                        temp.add(zoneGroup.getName());

                        Object items[] = new Object[10];
                        for (int i = 0; i < temp.size(); i++) {
                            if (i > 0) {
                                items = addAndRemoveDuplicatedInArray(items, temp.get(i));
                            } else {
                                items = addAndRemoveDuplicatedInArray(timestampObj.getValue(), temp.get(i));
                            }
                        }
                        timestampPoints.put(timestampObj.getKey(), items);
                    }
                    if (timestampPoints.size() == 0) {
                        timestampPoints.put(timeStampInit, new Object[]{new Date(timeStampInit), zoneGroup.getName()});
                    }
                }
                propertySerialName = true;
            }

            if (propertyName.contains("zoneType") || propertyName.contains("zoneProperty")) {
                //Get ZoneGroup from a thing
                zoneField = thing.getThingTypeField("zone");
                if (zoneField != null) {

                    List<String> newValues = new LinkedList<>();
                    List<Object[]> pointsMap = dataPointsMap.get(zoneField.getId());
                    if (pointsMap != null && pointsMap.size() > 0) {
                        List<Zone> zones = ZoneService.getInstance().getZonesByName(pointsMap.get(0)[1].toString());
                        Zone zoneItem = zones != null && zones.size() > 0 ? zones.get(0) : null;

                        if (zoneItem != null && zoneItem.getZoneType() != null) {
                            if (propertyName.contains("zoneType")) {
                                newValues.add(zoneItem.getZoneType().getName());
                            }
                            if (propertyName.contains("zoneProperty")) {
                                List<ZoneProperty> zoneProperties = zoneItem.getZoneType().getZoneProperties();
                                List<ZonePropertyValue> zonePropertyValues = ZonePropertyValueService.getInstance().getZonePropertiesByZoneId(zoneItem.getId());
                                for (ZonePropertyValue zonePropertyValue : zonePropertyValues) {
                                    if (zonePropertyValue.getValue() != null && zonePropertyValue.getValue().equals("true")) {
                                        for (ZoneProperty zoneProperty : zoneProperties) {
                                            if (zoneProperty.getId().equals(zonePropertyValue.getZonePropertyId())) {
                                                newValues.add(zoneProperty.getName());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    for (Map.Entry<Long, Object[]> timestampObj : timestampPoints.entrySet()) {
                        ArrayList<Object> temp = new ArrayList<>(Arrays.asList(timestampObj.getValue()));

                        for (String newValue : newValues) {
                            temp.add(newValue);
                        }

                        Object items[] = new Object[100];
                        for (int i = 0; i < temp.size(); i++) {
                            if (i > 0) {
                                items = addAndRemoveDuplicatedInArray(items, temp.get(i));
                            } else {
                                items = addAndRemoveDuplicatedInArray(timestampObj.getValue(), temp.get(i));
                            }
                        }
                        timestampPoints.put(timestampObj.getKey(), items);
                    }
                    if (timestampPoints.size() == 0) {
                        ArrayList<Object> temp = new ArrayList<>();

                        for (String newValue : newValues) {
                            temp.add(newValue);
                        }

                        Object items[] = new Object[100];
                        items[0] = new Date(timeStampInit);
                        for (int i = 0; i < temp.size(); i++) {
                            items = addAndRemoveDuplicatedInArray(items, temp.get(i));
                        }
                        timestampPoints.put(timeStampInit, items);
                    }
                }
                propertySerialName = true;
            }
        }

        if(thingTypeField != null && !propertySerialName) {

            boolean isNotTimeSeries = false;
            if(thingTypeField.getTimeSeries() == null || !thingTypeField.getTimeSeries()) {
                isNotTimeSeries = true;
            }

            List<Object[]> pointsMap = dataPointsMap.get( thingTypeField.getId() );
            for(int it=0; pointsMap != null &&  it<pointsMap.size()-1; it++) {
                Object array[] = pointsMap.get(it);
                Object arraySec[] = pointsMap.get(it+1);
//            for(Object[] array : pointsMap) {
                Long initVal = 0L;
                Long finVal = ((Date)(arraySec[0])).getTime();

                if(isNotTimeSeries) {
                    initVal = timeStampInit;
                }else {
                    initVal = ((Date) (array[0])).getTime();
                }

                for(Map.Entry<Long, Object[]> timestampObj : timestampPoints.entrySet()) {
                    if(timestampObj.getKey() >= initVal && timestampObj.getKey() < finVal) {
                        timestampPoints.put(timestampObj.getKey(), addAndRemoveDuplicatedInArray(timestampObj.getValue(), array[1]+","+thingTypeField.getId().toString()));
                    }
                }
            }
            //Adding last element
            if(pointsMap != null) {
                Object array[] = pointsMap.get(pointsMap.size() - 1);
                Long initVal;
                if(isNotTimeSeries) {
                    initVal = timeStampInit;
                }else {
                    initVal = ((Date) (array[0])).getTime();
                }
                for (Map.Entry<Long, Object[]> timestampObj : timestampPoints.entrySet()) {
                    if (timestampObj.getKey() >= initVal) {
                        timestampPoints.put(timestampObj.getKey(), addAndRemoveDuplicatedInArray(timestampObj.getValue(), array[1] + "," + thingTypeField.getId().toString()));
                    }
                }
            }
        }
    }

    /**
     * Created for fieldTypeId
     * @param reportRule
     * @param timestampPoints
     * @param thing
     * @param parentThing
     * @param childThing
     * @param dataPointsMap
     * @param timeStampInit
     * @param zoneListMap
     * @param zonePropertyValueMap
     */
    public static void addingMissingPoints2(ReportRule reportRule,
                                           Map<Long, Object[]> timestampPoints,
                                           Thing thing,
                                           Thing parentThing,
                                           Thing childThing,
                                           Map<Long, List<Object[]>>dataPointsMap,
                                           Long timeStampInit,
                                           Map<Long, Zone> zoneListMap,
                                           Map<String, ZonePropertyValue> zonePropertyValueMap) {
        String propertyName = reportRule.getPropertyName();

        ThingTypeField thingTypeField = thing.getThingTypeField(propertyName);
        if(thingTypeField == null && childThing != null) {
            thingTypeField = childThing.getThingTypeField(propertyName);
        }
        if(thingTypeField == null && parentThing != null) {
            thingTypeField = parentThing.getThingTypeField(propertyName);
        }

        boolean propertySerialName = false;
        propertyName = propertyName.replace("parent.", "");
        if(propertyName.equals("name")) {
            for(Map.Entry<Long, Object[]> timestampObj : timestampPoints.entrySet()) {
                ArrayList<Object> temp = new ArrayList<>(Arrays.asList(timestampObj.getValue()));
                temp.add(thing.getName());

                if(childThing != null) {
                    temp.add(childThing.getName());
                }
                Object items[] = new Object[10];
                for(int i = 0; i < temp.size(); i++) {
                    if(i > 0) {
                        items = addAndRemoveDuplicatedInArray(items, temp.get(i));
                    }else {
                        items = addAndRemoveDuplicatedInArray(timestampObj.getValue(), temp.get(i));
                    }
                }
                timestampPoints.put(timestampObj.getKey(), items);
            }
            if(timestampPoints.size() == 0) {
                timestampPoints.put(timeStampInit, new Object[] { new Date(timeStampInit), thing.getName() });
            }
            propertySerialName = true;
        }
        if(propertyName.equals("serial")) {
            for (Map.Entry<Long, Object[]> timestampObj : timestampPoints.entrySet()) {
                ArrayList<Object> temp = new ArrayList<Object>(Arrays.asList(timestampObj.getValue()));
                temp.add(thing.getSerial());
                if (childThing != null) {
                    temp.add(childThing.getSerial());
                }
                Object items[] = new Object[10];
                for(int i = 0; i < temp.size(); i++) {
                    if(i > 0) {
                        items = addAndRemoveDuplicatedInArray(items, temp.get(i));
                    }else {
                        items = addAndRemoveDuplicatedInArray(timestampObj.getValue(), temp.get(i));
                    }
                }
                timestampPoints.put(timestampObj.getKey(), items);
            }
            if(timestampPoints.size() == 0) {
                timestampPoints.put(timeStampInit, new Object[] { new Date(timeStampInit), thing.getSerial() });
            }
            propertySerialName = true;
        }
        if(propertyName.contains("zoneGroup") ||
                propertyName.contains("zoneCode") ||
                propertyName.contains("zoneType") ||
                propertyName.contains("zoneProperty") ||
                propertyName.contains("zone.name")
                ) {

            ThingTypeField zoneField = null;
            if(propertyName.contains("zone.name")) {
                List<ThingTypeField> zoneFieldList = thing.getThingTypeFieldByType(ThingTypeField.Type.TYPE_ZONE.value);
                if(zoneFieldList != null && zoneFieldList.size() > 0) {
                    zoneField = zoneFieldList.get(0);
                }
                if(zoneField == null && childThing != null) {
                    zoneFieldList = childThing.getThingTypeFieldByType(ThingTypeField.Type.TYPE_ZONE.value);
                    if(zoneFieldList != null && zoneFieldList.size() > 0) {
                        zoneField = zoneFieldList.get(0);
                    }
                }
                if(zoneField == null && parentThing != null) {
                    zoneFieldList = parentThing.getThingTypeFieldByType(ThingTypeField.Type.TYPE_ZONE.value);
                    if(zoneFieldList != null && zoneFieldList.size() > 0) {
                        zoneField = zoneFieldList.get(0);
                    }
                }
            }
            if(zoneField == null) {
                zoneField = thing.getThingTypeField("zone");
                if (zoneField == null && childThing != null) childThing.getThingTypeField("zone");
                if (zoneField == null && parentThing != null) parentThing.getThingTypeField("zone");
            }


            Zone zoneEntry = null;
            if (zoneField != null) {
                //TODO FIX THIS METHOD OR DELETE IT
                //String value = FieldValueService.value(thing.getId(), zoneField.getId());
                String value = null;
//            TODO: Redundant null check found by find bugs, delete it
//                if (value != null) {
//                    if(propertyName.equals("zone.name")) {
//                        zoneEntry = ReportUtils.getZoneFromZoneCode(zoneListMap, value);
//                    }
//                    if(zoneEntry == null) {
//                        zoneEntry = ReportUtils.getZoneFromZoneMap(zoneListMap, value);
//                    }
//                }
            }

            if (propertyName.contains("zone.name")) {
//            TODO: Redundant null check found by find bugs, delete it
//                if (zoneEntry != null) {
//                    for (Map.Entry<Long, Object[]> timestampObj : timestampPoints.entrySet()) {
//                        ArrayList<Object> temp = new ArrayList<>(Arrays.asList(timestampObj.getValue()));
//                        temp.add(zoneEntry.getName());
//
//                        Object items[] = new Object[10];
//                        for (int i = 0; i < temp.size(); i++) {
//                            if (i > 0) {
//                                items = addAndRemoveDuplicatedInArray(items, temp.get(i));
//                            } else {
//                                items = addAndRemoveDuplicatedInArray(timestampObj.getValue(), temp.get(i));
//                            }
//                        }
//                        timestampPoints.put(timestampObj.getKey(), items);
//                    }
//                    if (timestampPoints.size() == 0) {
//                        timestampPoints.put(timeStampInit, new Object[]{new Date(timeStampInit), zoneEntry.getName()});
//                    }
//                }
                propertySerialName = true;
            }


            if (propertyName.contains("zoneGroup")) {
//            TODO: Redundant null check found by find bugs, delete it
//                if (zoneEntry != null) {
//                    ZoneGroup zoneGroup = zoneEntry.getZoneGroup();
//                    if (zoneGroup != null) {
//                        for (Map.Entry<Long, Object[]> timestampObj : timestampPoints.entrySet()) {
//                            ArrayList<Object> temp = new ArrayList<>(Arrays.asList(timestampObj.getValue()));
//                            temp.add(zoneGroup.getName());
//
//                            Object items[] = new Object[10];
//                            for (int i = 0; i < temp.size(); i++) {
//                                if (i > 0) {
//                                    items = addAndRemoveDuplicatedInArray(items, temp.get(i));
//                                } else {
//                                    items = addAndRemoveDuplicatedInArray(timestampObj.getValue(), temp.get(i));
//                                }
//                            }
//                            timestampPoints.put(timestampObj.getKey(), items);
//                        }
//                        if (timestampPoints.size() == 0) {
//                            timestampPoints.put(timeStampInit, new Object[]{new Date(timeStampInit), zoneGroup.getName()});
//                        }
//                    }
//                }
                propertySerialName = true;
            }

            if (propertyName.contains("zoneCode")) {
                //Get ZoneGroup from a thing
                if (zoneField != null) {
//            TODO: Redundant null check found by find bugs, delete it
//                    if (!dataPointsMap.containsKey(zoneField.getId())) {
//                        //TODO FIX THIS METHOD OR DELETE IT
//                        //Map<Long, List<Object[]>> fieldZoneMap = FieldValueService.getFieldsValuesInObjectList(thing.getId(),  new LinkedList<>(Arrays.asList(zoneField.getId())));
//                        Map<Long, List<Object[]>> fieldZoneMap = null;
//                        dataPointsMap.put(zoneField.getId(), fieldZoneMap.get(zoneField.getId()));
//                    }
                    List<Object[]> pointsMap = dataPointsMap.get(zoneField.getId());
                    if (pointsMap != null && pointsMap.size() > 0) {
                        for (Object[] pointsItem : pointsMap) {
                            String zoneName = pointsItem[1].toString();
                            List<Zone> zoneList = ZoneService.getZonesByName(zoneName);
                            if (zoneList != null && zoneList.size() > 0) {
                                String zoneCode = zoneList.get(0).getCode();
                                Long timestampZoneCode = ((Date) (pointsItem[0])).getTime();
                                if (timestampPoints.containsKey(timestampZoneCode)) {
                                    timestampPoints.put(timestampZoneCode, addAndRemoveDuplicatedInArray(timestampPoints.get(timestampZoneCode), zoneCode));
                                } else {
                                    timestampPoints.put(timestampZoneCode, new Object[]{new Date(timestampZoneCode), zoneCode});
                                }
                            }
                        }
                    }
                }

                ZoneGroup zoneGroup = ZoneGroupService.getInstance().getZoneForThing(thing);
                if (zoneGroup == null) ZoneGroupService.getInstance().getZoneForThing(childThing);
                if (zoneGroup == null) ZoneGroupService.getInstance().getZoneForThing(parentThing);

                if (zoneGroup != null) {
                    for (Map.Entry<Long, Object[]> timestampObj : timestampPoints.entrySet()) {
                        ArrayList<Object> temp = new ArrayList<>(Arrays.asList(timestampObj.getValue()));
                        temp.add(zoneGroup.getName());

                        Object items[] = new Object[10];
                        for (int i = 0; i < temp.size(); i++) {
                            if (i > 0) {
                                items = addAndRemoveDuplicatedInArray(items, temp.get(i));
                            } else {
                                items = addAndRemoveDuplicatedInArray(timestampObj.getValue(), temp.get(i));
                            }
                        }
                        timestampPoints.put(timestampObj.getKey(), items);
                    }
                    if (timestampPoints.size() == 0) {
                        timestampPoints.put(timeStampInit, new Object[]{new Date(timeStampInit), zoneGroup.getName()});
                    }
                }
                propertySerialName = true;
            }

            if (propertyName.contains("zoneType") || propertyName.contains("zoneProperty")) {
                //Get ZoneGroup from a thing
                zoneField = thing.getThingTypeField("zone");
                if (zoneField != null) {

                    List<String> newValues = new LinkedList<>();
                    List<Object[]> pointsMap = dataPointsMap.get(zoneField.getId());
                    if (pointsMap != null && pointsMap.size() > 0) {
                        List<Zone> zones = ZoneService.getInstance().getZonesByName(pointsMap.get(0)[1].toString());
                        Zone zoneItem = zones != null && zones.size() > 0 ? zones.get(0) : null;

                        if (zoneItem != null && zoneItem.getZoneType() != null) {
                            if (propertyName.contains("zoneType")) {
                                newValues.add(zoneItem.getZoneType().getName());
                            }
                            if (propertyName.contains("zoneProperty")) {
                                List<ZoneProperty> zoneProperties = zoneItem.getZoneType().getZoneProperties();
                                List<ZonePropertyValue> zonePropertyValues = ZonePropertyValueService.getInstance().getZonePropertiesByZoneId(zoneItem.getId());
                                for (ZonePropertyValue zonePropertyValue : zonePropertyValues) {
                                    if (zonePropertyValue.getValue() != null && zonePropertyValue.getValue().equals("true")) {
                                        for (ZoneProperty zoneProperty : zoneProperties) {
                                            if (zoneProperty.getId().equals(zonePropertyValue.getZonePropertyId())) {
                                                newValues.add(zoneProperty.getName());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    for (Map.Entry<Long, Object[]> timestampObj : timestampPoints.entrySet()) {
                        ArrayList<Object> temp = new ArrayList<>(Arrays.asList(timestampObj.getValue()));

                        for (String newValue : newValues) {
                            temp.add(newValue);
                        }

                        Object items[] = new Object[100];
                        for (int i = 0; i < temp.size(); i++) {
                            if (i > 0) {
                                items = addAndRemoveDuplicatedInArray(items, temp.get(i));
                            } else {
                                items = addAndRemoveDuplicatedInArray(timestampObj.getValue(), temp.get(i));
                            }
                        }
                        timestampPoints.put(timestampObj.getKey(), items);
                    }
                    if (timestampPoints.size() == 0) {
                        ArrayList<Object> temp = new ArrayList<>();

                        for (String newValue : newValues) {
                            temp.add(newValue);
                        }

                        Object items[] = new Object[100];
                        items[0] = new Date(timeStampInit);
                        for (int i = 0; i < temp.size(); i++) {
                            items = addAndRemoveDuplicatedInArray(items, temp.get(i));
                        }
                        timestampPoints.put(timeStampInit, items);
                    }
                }
                propertySerialName = true;
            }
        }

        if(thingTypeField != null && !propertySerialName) {

            boolean isNotTimeSeries = false;
            if(thingTypeField.getTimeSeries() == null || !thingTypeField.getTimeSeries()) {
                isNotTimeSeries = true;
            }

            List<Object[]> pointsMap = dataPointsMap.get( thingTypeField.getId() );
            for(int it=0; pointsMap != null &&  it<pointsMap.size()-1; it++) {
                Object array[] = pointsMap.get(it);
                Object arraySec[] = pointsMap.get(it+1);
//            for(Object[] array : pointsMap) {
                Long initVal = 0L;
                Long finVal = ((Date)(arraySec[0])).getTime();

                if(isNotTimeSeries) {
                    initVal = timeStampInit;
                }else {
                    initVal = ((Date) (array[0])).getTime();
                }

                for(Map.Entry<Long, Object[]> timestampObj : timestampPoints.entrySet()) {
                    if(timestampObj.getKey() >= initVal && timestampObj.getKey() < finVal) {
                        timestampPoints.put(timestampObj.getKey(), addAndRemoveDuplicatedInArray(timestampObj.getValue(), array[1]+","+thingTypeField.getId().toString()));
                    }
                }
            }
            //Adding last element
            if(pointsMap != null) {
                Object array[] = pointsMap.get(pointsMap.size() - 1);
                Long initVal;
                if(isNotTimeSeries) {
                    initVal = timeStampInit;
                }else {
                    initVal = ((Date) (array[0])).getTime();
                }
                for (Map.Entry<Long, Object[]> timestampObj : timestampPoints.entrySet()) {
                    if (timestampObj.getKey() >= initVal) {
                        timestampPoints.put(timestampObj.getKey(), addAndRemoveDuplicatedInArray(timestampObj.getValue(), array[1] + "," + thingTypeField.getId().toString()));
                    }
                }
            }
        }
    }

    public static boolean isNumeric(String str) {
        try
        {
            if (str == null) return false;
            double d = Double.parseDouble(str);
        }
        catch(NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }

    public static Object[] addAndRemoveDuplicatedInArray(Object[] itemArray, Object item) {
        ArrayList<Object> temp = new ArrayList<Object>(Arrays.asList(itemArray));
        temp.add(item);
        Set<Object> setData = new LinkedHashSet<>();
        setData.addAll(temp);
        return setData.toArray();
    }

    public static void addThingFieldIdsFromReportProperties(List<ReportProperty> reportProperties, List<Long> thingFieldsIds, ThingTypeField field) {
        for(ReportProperty reportProperty : reportProperties) {
            String fieldName = reportProperty.getPropertyName();
            fieldName = ReportExecutionUtils.removeDwellTimeString(fieldName);
            if(reportProperty.getPropertyName().startsWith(ReportExecutionUtils.DWELL_TIME_PROPERTY_LABEL) && field.getName().equals(fieldName)) {
                thingFieldsIds.add(field.getId());
            }
        }
    }

    public static void addThingFieldIdsFromReportRules(List<ReportRule> reportRules, List<Long> thingFieldsIds, ThingTypeField field) {
        for(ReportRule reportRule : reportRules) {
            String fieldName = reportRule.getPropertyName();
            fieldName = ReportExecutionUtils.removeDwellTimeString(fieldName);
            if(field.getName().equals(fieldName)) {
                thingFieldsIds.add(field.getId());
            }
        }
    }

    public static void addThingFieldIdsFromReportGroupBy(List<ReportGroupBy> reportGroupByList, List<Long> thingFieldsIds, ThingTypeField field) {
        for (ReportGroupBy reportGroupBy : reportGroupByList) {
            String fieldName = reportGroupBy.getPropertyName();
            fieldName = ReportExecutionUtils.removeDwellTimeString(fieldName);
            if (field.getName().equals(fieldName)) {
                thingFieldsIds.add(field.getId());
            }
        }
    }
}
