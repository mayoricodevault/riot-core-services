package com.tierconnect.riot.iot.utils;

import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.commons.utils.DateHelper;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.reports.ReportAppService;
import com.tierconnect.riot.iot.reports_integration.ReportFactory;
import com.tierconnect.riot.iot.services.ReportDefinitionService;
import com.tierconnect.riot.iot.services.ThingService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by user on 12/4/14.
 */
public class ReportExecutionUtils {
    static Logger logger = Logger.getLogger(ReportExecutionUtils.class);

    //    public static String DWELL_TIME_PROPERTY = "dwellTimeProperty";
    public final static String DWELL_TIME_PROPERTY_LABEL = "dwellTime(";
    public final static String TIME_STAMP_PROPERTY_LABEL = "timeStamp(";


    public final static String IS_TIME_SERIES = "isTimeSeries";
    public final static String IS_NOT_TIME_SERIES = "isNotTimeSeries";

    public final static SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss.SSS a");

    public final static SimpleDateFormat formatDate = new SimpleDateFormat("MM/dd/yyyy");


    /*
   * Init report table
   * */
    public static Map<String, String> init() {
        Map<String, String> propertyMap = new HashMap<>();
        propertyMap.put("id", "id");
        propertyMap.put("name", "Name");
        propertyMap.put("thingType.name", "Type");
        propertyMap.put("thingType.id", "TypeId");

        return propertyMap;
    }

    public static Map<String, Object> addingSettingsMapProperties(ReportDefinition reportDefinition) {
        Map<String, Object> mapJson = new HashMap<>();
        mapJson.put("defaultZoom", reportDefinition.getDefaultZoom());
        mapJson.put("centerLat", reportDefinition.getCenterLat());
        mapJson.put("centerLon", reportDefinition.getCenterLon());
        mapJson.put("pinLabels", reportDefinition.getPinLabels());
        mapJson.put("heatMap", reportDefinition.isHeatmap());
        mapJson.put("zoneLabels", reportDefinition.getZoneLabels());
        mapJson.put("trails", reportDefinition.getTrails());
        mapJson.put("clustering", reportDefinition.getClustering());
        mapJson.put("playback", reportDefinition.getPlayback());
        mapJson.put("nupYup", reportDefinition.getNupYup());
        mapJson.put("defaultList", reportDefinition.getDefaultList());
        mapJson.put("liveMap", reportDefinition.getLiveMap());
        mapJson.put("interpolation", reportDefinition.getInterpolation());
        mapJson.put("clusterDistance", reportDefinition.getClusterDistance());
        mapJson.put("pinDisplay", reportDefinition.getPinDisplay());
        mapJson.put("zoneDisplay", reportDefinition.getZoneDisplay());
        mapJson.put("pinIcons", reportDefinition.getPinIcons());
        mapJson.put("zoneOpacity", reportDefinition.getZoneOpacity());
        mapJson.put("mapOpacity", reportDefinition.getMapOpacity());
        mapJson.put("playbackMaxThing", reportDefinition.getPlaybackMaxThing());

        String pinPopupLabel = reportDefinition.getPinLabel();
        int orderPinLabel;

        if (pinPopupLabel != null && ReportRuleUtils.isNumeric(pinPopupLabel)) {
            orderPinLabel = Integer.parseInt(pinPopupLabel);
            for (ReportProperty reportProperty : reportDefinition.getReportProperty()) {
                int order = reportProperty.getDisplayOrder().intValue();
                if (order == orderPinLabel) {
                    mapJson.put("pinLabelPopup", reportProperty.getLabel());
                    break;
                }
            }
        }

        return mapJson;
    }

    public static Map<String, Long> getLabelProperties(List<ReportProperty> reportProperties) {
        Map<String, Long> labelPropertiesMap = new LinkedHashMap<>();
        for (ReportProperty reportProperty : reportProperties) {
            labelPropertiesMap.put(reportProperty.getLabel(), reportProperty.getDisplayOrder().longValue());
        }
        return labelPropertiesMap;
    }

    public static List<CompositeThing> getCompositeThings(List<Thing> preFilteredThings) {

        List<CompositeThing> compositeThingList = new LinkedList<>();
        List<Thing> parentList = new LinkedList<>();

        Set<Long> thingTypeIdSet = new LinkedHashSet<>();
        boolean filteredByThingType = true;
        Map<Long, List<Thing>> compositeThingMap = new HashMap<>();
        Map<Long, Thing> thingMap = new HashMap<>();
        Set<Long> parentIds = new HashSet<>();

        for (Thing thing : preFilteredThings) {
            Thing parentThing = thing.getParent();

            if (thingTypeIdSet.size() <= 2) {
                thingTypeIdSet.add(thing.getThingType().getId());
            }
            if (thingTypeIdSet.size() > 1) {
                filteredByThingType = false;
            }

            thingMap.put(thing.getId(), thing);
            if (parentThing != null) {
                thingMap.put(parentThing.getId(), parentThing);
                parentIds.add(parentThing.getId());
                if (compositeThingMap.containsKey(parentThing.getId())) {
                    List<Thing> thingList = compositeThingMap.get(parentThing.getId());
                    thingList.add(thing);
                } else {
                    List<Thing> thingList = new LinkedList<>(Arrays.asList(thing));
                    compositeThingMap.put(parentThing.getId(), thingList);
                }
            } else {
                if (!parentIds.contains(thing.getId())) {
                    parentList.add(thing);
                }
                if (!compositeThingMap.containsKey(thing.getId())) {
                    compositeThingMap.put(thing.getId(), new LinkedList<Thing>());
                }
            }
        }

        //Cleaning parentList
        List<Thing> parentToGet = new LinkedList<>();
        for (Thing thing : parentList) {
            if (!parentIds.contains(thing.getId())) {
                parentToGet.add(thing);
            }
        }

        Map<Long, Thing> childrenMap = ThingService.getInstance().selectByParents(parentToGet);

        //Building CompositeThings
        for (Map.Entry<Long, List<Thing>> childrenData : compositeThingMap.entrySet()) {
            if (thingMap.containsKey(childrenData.getKey())) {
                Thing thingParent = thingMap.get(childrenData.getKey());
                if (filteredByThingType) {
                    List<Thing> thingList = childrenData.getValue();
                    boolean addingChildren = false;
                    for (Thing thing : thingList) {
                        addingChildren = true;
                        CompositeThing compositeThing = new CompositeThing(thing);
                        compositeThing.addChild(thing);
                        compositeThingList.add(compositeThing);
                    }
                    if (!childrenMap.containsKey(thingParent.getId()) && !addingChildren) {
                        CompositeThing compositeThing = new CompositeThing(thingParent);
                        compositeThingList.add(compositeThing);
                    }
                } else {
                    CompositeThing compositeThing = new CompositeThing(thingParent, childrenData.getValue());
                    compositeThingList.add(compositeThing);
                }
            }
        }
        for (Map.Entry<Long, Thing> childrenData : childrenMap.entrySet()) {
            if (thingMap.containsKey(childrenData.getKey())) {
                Thing thingParent = thingMap.get(childrenData.getKey());
                CompositeThing compositeThing = new CompositeThing(thingParent);
                compositeThing.addChild(childrenData.getValue());
                compositeThingList.add(compositeThing);
            }
        }

        return compositeThingList;
    }

    public static Integer getPageSizeByDefault(ReportDefinition reportDefinition, String fieldName) {
        Integer pageSizeByDefault = 1 << 20;
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();

        if (reportDefinition != null && reportDefinition.getMaxRecords() != null) {
            return reportDefinition.getMaxRecords().intValue();
        }
        String pageSizeByDefaultStr = ConfigurationService.getAsString(currentUser, fieldName);
        if (ReportRuleUtils.isNumeric(pageSizeByDefaultStr)) {
            return Integer.valueOf(pageSizeByDefaultStr);
        }
        return pageSizeByDefault;
    }

    public static String removeTimeStamp(String timeStamp) {
        if (timeStamp != null) {
            timeStamp = timeStamp.replace("timeStamp(", "");
            timeStamp = timeStamp.replace(")", "");
            timeStamp = timeStamp.trim();
        } else return "";
        return timeStamp;
    }

    public static String removeDwellTimeString(String dwellTime) {
        if (dwellTime != null) {
            dwellTime = dwellTime.replace("dwellTime(", "");
            dwellTime = dwellTime.replace(")", "");
            dwellTime = dwellTime.trim();
        } else return "";
        return dwellTime;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> preparingForExporting(List<Object> results, List<ReportProperty>
            reportProperties, Map<String, Object> thingFieldTypeMap) {
        List<String> headers = new LinkedList<>();
        List<Object> valueList = new LinkedList<>();

        for (ReportProperty reportProperty : reportProperties) {
            String header = reportProperty.getLabel();
            if (!header.equals("id")) {
                headers.add(header);
            }
        }

        for (Object obj : results) {
            if (obj != null) {
                Map<String, Object> itemMap = (Map<String, Object>) obj;
                List<String> values = new LinkedList<>();
                for (String header : headers) {
                    String value = StringUtils.EMPTY;
                    if (itemMap.get(header) != null) {
                        value = itemMap.get(header).toString();

                        if (thingFieldTypeMap != null && thingFieldTypeMap.get(header) != null &&
                                ((Map<String, Object>) thingFieldTypeMap.get(header)).get("thingFieldType") != null) {

                            long type = (long) ((Map<String, Object>) thingFieldTypeMap.get(header)).get("thingFieldType");
                            logger.trace("header= " + header + " type= " + type);
                            if (Long.compare(ThingTypeField.Type.TYPE_TIMESTAMP.value, type) == 0
                                    && !Utilities.isEmptyOrNull(String.valueOf(itemMap.get(header)))) {
                                value = format.format(new Date(Long.parseLong(String.valueOf(itemMap.get(header)))));
                            }
                            if (Long.compare(ThingTypeField.Type.TYPE_DATE.value, type) == 0
                                    && itemMap.get(header) instanceof Date) {
                                value = format.format((Date) itemMap.get(header));
                            }
                            if ((Long.compare(0L, type) == 0)
                                    && !Utilities.isEmptyOrNull(String.valueOf(itemMap.get(header)))) {
                                // TODO: Ruth Function
                                value = DateHelper.formatDwellTime(Long.valueOf(itemMap.get(header).toString()), null);
                            }
                        }
                    }
                    values.add(value);
                }
                valueList.add(values);
            }
        }
        Map<String, Object> response = new HashMap<>();
        response.put("headers", headers);
        response.put("values", valueList);
        return response;
    }

    public static File exportingReport(Long id, DateFormatAndTimeZone dateFormatAndTimeZone) throws Exception {
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
        if (reportDefinition == null) {
            return null;
        }
        long start = System.currentTimeMillis();
        ReportAppService.instance().logStart(reportDefinition, reportDefinition.getFiltersFromReportInJSON(), Constants.SENT_EMAIL_REPORT);
        ReportFilter relativeDateFilter = getReportFilter("relativeDate", reportDefinition);
        String relativeDate = (relativeDateFilter != null) ? relativeDateFilter.getValue() : null;
        ReportFilter startDateFilter = getReportFilter("startDate", reportDefinition);
        Long startDate = (startDateFilter != null && !StringUtils.isBlank(startDateFilter.getValue())) ?
                getDateAsLong(startDateFilter.getValue()) : null;
        ReportFilter endDateFilter = getReportFilter("endDate", reportDefinition);
        Long endDate = (endDateFilter != null && !StringUtils.isBlank(endDateFilter.getValue())) ? getDateAsLong
                (endDateFilter.getValue()) : null;
        DateHelper dh = DateHelper.getRelativeDateHelper(relativeDate, startDate, endDate, new Date(), dateFormatAndTimeZone);
        ReportFactory reportFactory = new ReportFactory(dateFormatAndTimeZone);
        reportFactory.setSentEmail(true);
        File file = reportFactory.getFileResult(reportDefinition, new HashMap<String, Object>(), null, null,
                dh.from(), dh.to(), new Date(), false, reportDefinition.getFiltersFromReportInJSON());
        ReportAppService.instance().logEnd(reportDefinition, start, Constants.SENT_EMAIL_REPORT);
        return file;
    }

    private static Long getDateAsLong(String date) {
        //09/30/2015 03:34:52 PM
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a");

        try {
            return simpleDateFormat.parse(date).getTime();
        } catch (ParseException e) {
            try {
                return Long.valueOf(date);
            } catch (Exception ex) {
                logger.warn("Unable to parse date " + date, ex);
                throw new RuntimeException(e);
            }
        }
    }

    private static ReportFilter getReportFilter(String property, ReportDefinition reportDefinition) {
        ReportFilter reportFilter = null;
        for (ReportFilter filter : reportDefinition.getReportFilter()) {
            if (filter.getPropertyName().equals(property)) {
                reportFilter = filter;
                break;
            }
        }
        return reportFilter;
    }
}
