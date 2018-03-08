package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.fmc.utils.FMCConstants;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.*;

/**
 * Created by julio.rocha on 06-01-17.
 */
public class ZoneTranslator {
    private static final String[][] HARDCODED_UI = {
            {"zoneProperty.id", "{0}.value.id"},
            {"zoneType.id", "{0}.value.id"},
    };

    private static final String[][] KEY_VALUE_TEMPLATE = {
            {"{0}", "{0}.value.id"},
            {"{0}Code.name", "{0}.value.code"},
            {"{0}.value.codeDTHC", "{0}.changed"},
            {"{0}.value.codeDTBK", "{0}.blinked"},
            {"{0}Group.id", "{0}.value.zoneGroup"},
            {"{0}.value.zoneGroupDwellTimeDTH", "{0}.value.zoneGroupChanged"},
            {"{0}.value.zoneGroupDTHC", "{0}.value.zoneGroupChanged"},
            {"{0}.value.zoneGroupDTBK", "{0}.value.zoneGroupBlinked"},
            {"timeStamp({0})", "{0}.time"},
            {"{0}.name", "{0}.value.name"},
            {"{0}.value.nameZN", "{0}.changed"},
            {"{0}LocalMap.id", "{0}.value.facilityMap"},
            {"{0}Property.idDisplay", "{0}.value.id"},
            {"{0}Type.idDisplay", "{0}.value.id"},
            {"{0}.value.facilityMapDwellTimeDTH", "{0}.value.facilityMapChanged"},
            {"{0}.value.facilityMapDTHC", "{0}.value.facilityMapChanged"},
            {"{0}.value.facilityMapDTBK", "{0}.value.facilityMapBlinked"},

            //dwell times
            {"dwellTime( {0} )", "zone"},
            {"dwellTime( {0}.facilityMap )", "zone"},
            {"dwellTime( {0}.zoneGroup )", "zone"},
            {"dwellTime( {0}.zoneType )", "zone"},
            //field resolver for dwell time things
            {"{0}DT", "{0}.time"},
            {"{0}.facilityMapDT", "{0}.value.facilityMapTime"},
            {"{0}.zoneGroupDT", "{0}.value.zoneGroupTime"},
            {"{0}.zoneTypeDT", "{0}.value.zoneTypeTime"},
            //field resolver for dwell time thingSnapshot
            {"{0}DTH", "{0}.dwellTime"},
            {"{0}.facilityMapDTH", "{0}.value.facilityMapDwellTime"},
            {"{0}.zoneGroupDTH", "{0}.value.zoneGroupDwellTime"},
            {"{0}.zoneTypeDTH", "{0}.value.zoneTypeDwellTime"},

            //to show just for zone property by now
            {"{0}.value.id", "{0}.value.name"},
            {"{0}.value.idDTHC", "{0}.value.zoneTypeChanged"},
            {"{0}.value.idDTBK", "{0}.value.zoneTypeBlinked"},
            {"{0}Type.name", "{0}.value.id"},
            {"{0}Type.id", "{0}.value.id"},
            {"{0}.value.zoneTypeDwellTimeDTH", "{0}.value.zoneTypeChanged"},
            {"{0}.value.zoneTypeDwellTimeDTB", "{0}.value.zoneTypeBlinked"},
            {"{0}.value.nameDTHC", "{0}.value.zoneTypeChanged"},
            {"{0}.value.nameDTBK", "{0}.value.zoneTypeBlinked"},
            {"{0}Property.id", "{0}.value.id"},

            //DwellTimes Zone Properties
            {"{0}.value.zoneTypeDwellTimeDTHC", "{0}.value.zoneTypeChanged"},
            {"{0}.value.zoneTypeDwellTimeDTBK", "{0}.value.zoneTypeBlinked"},
            {"{0}.value.zoneGroupDwellTimeDTHC", "{0}.value.zoneGroupChanged"},
            {"{0}.value.zoneGroupDwellTimeDTBK", "{0}.value.zoneGroupBlinked"},
            {"{0}.value.facilityMapDwellTimeDTHC", "{0}.value.facilityMapChanged"},
            {"{0}.value.facilityMapDwellTimeDTBK", "{0}.value.facilityMapBlinked"},

            // Timestamp Zone properties
            {"{0}.value.zoneTypeTimeDTHC", "{0}.value.zoneTypeChanged"},
            {"{0}.value.zoneTypeTimeDTBK", "{0}.value.zoneTypeBlinked"},
            {"{0}.value.facilityMapTimeDTHC", "{0}.value.facilityMapChanged"},
            {"{0}.value.facilityMapTimeDTBK", "{0}.value.facilityMapBlinked"},
            {"{0}.value.zoneGroupTimeDTHC", "{0}.value.zoneGroupChanged"},
            {"{0}.value.zoneGroupTimeDTBK", "{0}.value.zoneGroupBlinked"},

            //VIZIX-2765, Enable properties: zoneGroupTime, facilityMapTime, zoneTypeTime
            {"timeStamp({0}.zoneGroupTime)", "{0}.value.zoneGroupTime"},
            {"timeStamp({0}.facilityMapTime)", "{0}.value.facilityMapTime"},
            {"timeStamp({0}.zoneTypeTime)", "{0}.value.zoneTypeTime"}
    };

    private Map<String, String> translatorMap;
    private Map<String, String> backToPropertyName;
    private Set<String> multipleZoneProperty;
    private Set<String> zoneFields = new HashSet<>();
    private ReportDefinition reportDefinition;

    private String inferedZone = "zone";

    public ZoneTranslator(final ReportDefinition reportDefinition) {
        this.reportDefinition = reportDefinition;
        translatorMap = new HashMap<>();
        backToPropertyName = new HashMap<>();
        multipleZoneProperty = new HashSet<>();
        if (isFMC()) {
            inferedZone = FMCConstants.FMC_SCAN_ZONE;
        }
        buildTranslator();
    }

    private enum FILL_OPTIONS {
        BOTH,
        RIGHT,
        NONE
    }

    private void buildTranslator() {
        for (ReportFilter rf : reportDefinition.getReportFilter()) {
            if (rf.getThingType() != null) {
                builMapByThingType(rf.getThingType());
            } else if (rf.getThingTypeField() != null && rf.getThingTypeField().getDataType().getId().equals(ThingTypeField.Type.TYPE_ZONE.value)) {
                buildMap(rf.getThingTypeField().getName());
            }
        }
        for (ReportProperty rp : reportDefinition.getReportProperty()) {
            if (rp.getThingType() != null) {
                builMapByThingType(rp.getThingType());
            } else if (rp.getThingTypeField() != null && rp.getThingTypeField().getDataType().getId().equals(ThingTypeField.Type.TYPE_ZONE.value)) {
                buildMap(rp.getThingTypeField().getName());
            }
        }
        for (ReportGroupBy rg : reportDefinition.getReportGroupBy()) {
            if (rg.getThingType() != null) {
                builMapByThingType(rg.getThingType());
            } else if (rg.getThingTypeField() != null && rg.getThingTypeField().getDataType().getId().equals(ThingTypeField.Type.TYPE_ZONE.value)) {
                buildMap(rg.getThingTypeField().getName());
            }
        }
        if (zoneFields.isEmpty()) {
            buildMap(inferedZone);
        }
        translatorMap.put("zoneProperty.id", StringUtils.join(multipleZoneProperty, ","));
        translatorMap.put("zoneType.id", translatorMap.get("zoneProperty.id"));
        multipleZoneProperty = null;
    }

    private void builMapByThingType(ThingType thingType) {
        List<ThingTypeField> ttfList = thingType.getThingTypeFieldsByType(ThingTypeField.Type.TYPE_ZONE.value);
        if (!ttfList.isEmpty()) {
            buildMap(ttfList);
        }
    }

    private void buildMap(List<ThingTypeField> ttfList) {
        for (ThingTypeField ttf : ttfList) {
            buildMap(ttf.getName());
        }
    }

    private void buildMap(String udfZoneName) {
        if (!zoneFields.contains(udfZoneName)) {
            zoneFields.add(udfZoneName);
            for (String[] row : KEY_VALUE_TEMPLATE) {
                fillMap(udfZoneName, row[0], row[1], FILL_OPTIONS.BOTH);
            }
            for (String[] row : HARDCODED_UI) {
                fillMap(udfZoneName, row[0], row[1], FILL_OPTIONS.RIGHT);
            }
        }
    }

    private void fillMap(String udfZoneName, String key, String value, FILL_OPTIONS option) {
        switch (option) {
            case BOTH:
                key = MessageFormat.format(key, udfZoneName);
                value = MessageFormat.format(value, udfZoneName);
                translatorMap.put(key, value);
                backToPropertyName.put(key, udfZoneName);
                break;
            case RIGHT:
                value = MessageFormat.format(value, udfZoneName);
                if (isZoneProperty(key) || isZoneTypeId(key)) {
                    multipleZoneProperty.add(value);
                } else {
                    translatorMap.put(key, value);
                }
                backToPropertyName.put(key, udfZoneName);
                break;
            case NONE:
                translatorMap.put(key, value);
                break;
        }
    }

    public boolean isZoneUDFName(String property) {
        return zoneFields.contains(property);
    }

    public Map<String, String> getTranslatorMap() {
        return translatorMap;
    }

    public Map<String, String> getBackToPropertyName() {
        return backToPropertyName;
    }


    public static boolean isFacilityMap(String property) {
        return property.contains("ocalMap.id");
    }

    public static boolean isZoneGroup(String property) {
        return property.contains("Group.id");
    }

    public static boolean isZoneProperty(String property) {
        return property.contains("Property.id");
    }

    public static boolean isZoneType(String property) {
        return property.contains("Type.name");
    }

    public static boolean isZoneTypeId(String property) {
        return property.contains("Type.id");
    }

    public static boolean isZoneCode(String property) {
        return property.contains("Code.name");
    }

    public static boolean isPropertyOfZone(String property) {
        return isFacilityMap(property) ||
                isZoneGroup(property) ||
                isZoneProperty(property) ||
                isZoneType(property) ||
                isZoneTypeId(property) ||
                isZoneCode(property);
    }

    public static String reverseTranslateZoneProperty(String property) {
        if (isFacilityMap(property)) {
            property = StringUtils.replace(property, "ocalMap.id", "");
            return property.substring(0, property.length()-1);
        } else if (isZoneGroup(property)) {
            return StringUtils.replace(property, "Group.id", "");
        } else if (isZoneProperty(property)) {
            property = StringUtils.replace(property, "Property.id", "");
            if (property.contains(",")) {
                property = property.substring(0, property.indexOf(","));
            }
            return property;
        } else if (isZoneType(property)) {
            return StringUtils.replace(property, "Type.name", "");
        } else if (isZoneTypeId(property)) {
            return StringUtils.replace(property, "Type.id", "");
        } else if (isZoneCode(property)) {
            return StringUtils.replace(property, "Code.name", "");
        } else if (property.endsWith(".name")) {
            return StringUtils.replace(property, ".name", "");
        }
        return property;
    }

    private boolean isFMC() {
        try {
            Group fmcGroup = GroupService.getInstance().getByCode(FMCConstants.FMC.toLowerCase());
            return fmcGroup != null
                    //&& fmcGroup.getId().equals(2L)//maybe FMC not always has id equals 2
                    && fmcGroup.getHierarchyName().equals(">fmc");
        } catch (NonUniqueResultException e) {
            return false;
        }
    }

    public String inferZoneDwellFunction(String property) {
        if (containsDwellOperation(property)) {
            String field = ReportDefinitionUtils.stripDwell(property);
            return "dwellTime( " + inferedZone + "." + field + " )";
        }
        return null;
    }

    private boolean containsDwellOperation(String property) {
        return property.equals("dwellTime( facilityMap )")
                || property.equals("dwellTime( zoneGroup )")
                || property.equals("dwellTime( zoneType )");
    }

}
