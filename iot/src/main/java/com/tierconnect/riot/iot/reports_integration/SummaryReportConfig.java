package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.api.database.base.Operation;
import com.tierconnect.riot.api.database.mongo.aggregate.MongoGroupBy;
import com.tierconnect.riot.api.database.mongo.aggregate.MongoUnwind;
import com.tierconnect.riot.api.database.mongo.aggregate.Pipeline;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.iot.services.ThingTypePathService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.iot.services.ZonePropertyValueService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;

import java.util.*;

import static com.tierconnect.riot.appcore.utils.Utilities.isEmptyOrNull;
import static com.tierconnect.riot.commons.Constants.*;

/**
 * Created by vealaro on 2/15/17.
 */
public class SummaryReportConfig extends ReportConfig {

    private static Logger logger = Logger.getLogger(SummaryReportConfig.class);
    private List<Pipeline> pipelineList;
    private Integer mongoLimit;
    private boolean twoDimension = false;
    private String axisLabelX;
    private String axisLabelY;
    private boolean isTimestampOrDatePropertyOne = false;
    private boolean isTimestampOrDatePropertyTwo = false;

    private static final String HOUR = "hour";
    private static final String DAY = "day";
    private static final String MONTH = "month";
    private static final String YEAR = "year";

    private Map<String, String> mapZonePropertyValue;
    protected Map<String, List<IRule>> displayRulesSummary;

    public SummaryReportConfig(ReportDefinition reportDefinition, Map<String, Object> dynamicFilters,
                               Integer pageNum, Integer pageSize, Date startDate, Date endDate,
                               Date now) {
        super(reportDefinition, dynamicFilters, pageNum, pageSize, startDate, endDate);
        processConfigurationSummary(now);
        processReportGroupBy();
    }

    protected void processReportGroupBy() {
        for (ReportGroupBy reportGroupBy : reportDefinition.getReportGroupBy()) {
            // add property exist to filters
            addPropertiesGroupByExist(reportGroupBy);
            // add changed to property
            groupByPartition(reportGroupBy);
            // add filter Group by ZonePropertyId
            addFilterbyZoneProperty(reportGroupBy);
        }
        twoDimension = reportDefinition.getReportGroupBy() != null && reportDefinition.getReportGroupBy().size() > 1;
    }

    private void addFilterbyZoneProperty(ReportGroupBy groupBy) {
        if (ZoneTranslator.isZoneProperty(groupBy.getPropertyName())) {
            String[] splitPropertyId = StringUtils.split(groupBy.getPropertyName(), ",");
            if (splitPropertyId != null && splitPropertyId.length >= 2 && Utilities.isNumber(splitPropertyId[1])) {
                List<Long> zoneProperty = getListZoneIdByZonePropertyOrZoneType(Long.valueOf(splitPropertyId[1].trim()), Constants.OP_CONTAINS, StringUtils.EMPTY);
                String translate = translate(groupBy, true);
                logger.info("add filter for Zone Property ID = " + splitPropertyId[1]);
                addFilter(groupBy.getLabel(), translate, Constants.OP_EQUALS, groupBy.getPropertyName(), zoneProperty, StringUtils.EMPTY, isHistoricalReport());
                // add map values to map(ZonepropertyValue)
                if (mapZonePropertyValue == null) {
                    mapZonePropertyValue = new HashMap<>();
                }
                mapZonePropertyValue.putAll(ZonePropertyValueService.getInstance().getMapZonePropertyValue(Long.valueOf(splitPropertyId[1].trim())));
            }
        }
    }

    public String getValueOfZoneProperty(String zoneId, String zonePropertyId) {
        if (mapZonePropertyValue == null) return null;
        return mapZonePropertyValue.get(zoneId + "-" + zonePropertyId);
    }

    /**
     * add exist:true to the properties of report group
     */
    private void addPropertiesGroupByExist(ReportGroupBy reportGroupBy) {
        if (reportGroupBy.getOther() != null && !reportGroupBy.getOther()) {
            String propertyName = reportGroupBy.getPropertyName();
            if (mapNonUDFs.containsKey(propertyName)) {
                propertyName = mapNonUDFs.get(propertyName);
            } else if (ZoneTranslator.isPropertyOfZone(propertyName)) {
                propertyName = translate(reportGroupBy, true);
            }
            addFilters(Operation.exists(verifiedSnapshotsProperty(getPathComplete(propertyName, reportGroupBy.getThingType()))));
        }
    }

    private void groupByPartition(ReportGroupBy reportGroupBy) {
        if (reportGroupBy.getByPartition() != null && reportGroupBy.getByPartition() && isEmptyOrNull(reportGroupBy.getUnit())) {
            addFilters(Operation.equals(getPropertyChanged(reportGroupBy), true));
        }
    }

    private String getPropertyChanged(ReportGroupBy groupBy) {
        String translate = groupBy.getPropertyName() + Constants.LAST_KNOWN_BLINKED;
        if (ZoneTranslator.isPropertyOfZone(groupBy.getPropertyName()) && groupBy.getThingType() != null) {
            // TODO review report definition refactor
            List<ThingTypeField> fieldsByType = groupBy.getThingType().getThingTypeFieldsByType(ThingTypeField.Type.TYPE_ZONE.value);
            if (fieldsByType != null && !fieldsByType.isEmpty()) {
                groupBy.setThingTypeField(fieldsByType.get(0));
            }
            translate = translate(groupBy, true) + Constants.DWELLTIME_CHANGED;
            if (ZONE_TRANSLATOR.containsKey(translate)) {
                return verifiedSnapshotsProperty(ZONE_TRANSLATOR.get(translate));
            }
        }
        return verifiedSnapshotsProperty(getPathComplete(translate, groupBy.getThingType()));
    }

    private String getPathComplete(String property, ThingType thingTypeTarget) {
        if (thingType != null && thingTypeTarget != null) {
            ThingTypePath path = ThingTypePathService.getInstance().getPathByThingTypes(thingType, thingTypeTarget);
            if (path != null) {
                property = path.getPath() + "." + property;
            }
        }
        return property;
    }

    protected void createGroupForAggregate(String propertyAccumulator) {
        pipelineList = new ArrayList<>(3);
        List<ReportGroupBy> groupBy = reportDefinition.getReportGroupBy();

        String propertyName0 = groupBy.get(0).getPropertyName();
        String propertyTranslated0 = verifiedSnapshotsProperty(groupProperty(groupBy.get(0)));
        Map<String, Object> mapID = new LinkedHashMap<>();
        MongoGroupBy mongoGroupBy1;
        axisLabelY = StringUtils.remove(propertyName0, ".") + ",y";
        isTimestampOrDatePropertyOne = isTimestampOrDate(groupBy.get(0));
        if (groupBy.size() > 1) {
            axisLabelX = StringUtils.remove(propertyName0, ".") + ",x";
            isTimestampOrDatePropertyTwo = isTimestampOrDate(groupBy.get(1));
            String propertyName1 = groupBy.get(1).getPropertyName();
            String propertyTranslated1 = verifiedSnapshotsProperty(groupProperty(groupBy.get(1)));
            axisLabelY = StringUtils.remove(propertyName1, ".") + ",y";

            //first dimension grouping
            //change property to have a "x" to distinguish other properties in the second dimension
            //with the same name
            mapID.putAll(propertyToObject(axisLabelX, propertyTranslated0, groupBy.get(0).getUnit(), 0));


            //second dimension grouping
            //change property to have a "y" to distinguish other properties in the first dimension
            //with the same name
            mapID.putAll(propertyToObject(axisLabelY, propertyTranslated1, groupBy.get(1).getUnit(), 1));

            // calculate unwinds
            calculateUnwinds(groupBy.get(0), groupBy.get(1));

            mongoGroupBy1 = new MongoGroupBy(mapID);

        } else { //only one property/one dimension
            if (isEmptyOrNull(groupBy.get(0).getUnit())) {
                mongoGroupBy1 = new MongoGroupBy("$" + propertyTranslated0);
            } else {
                ThingType groupThingType = groupBy.get(0).getThingType();
                mapID.putAll(propertyToObject(axisLabelY, propertyTranslated0, groupBy.get(0).getUnit(), 1));
                logger.info("Thing type " + groupThingType.getName());
                mongoGroupBy1 = new MongoGroupBy(mapID);
            }
            calculateUnwinds(groupBy.get(0));
        }

        if (reportDefinition.containsGroupByPartition() && reportDefinition.containsUnit()) {
            //add pre-group
            //pre-group groups values in order to emulate distinct (for unique values)
            mapID.put("thingId", (isHistoricalReport() ? "$value." : "$") + "_id");
            if (!COUNT.equalsIgnoreCase(reportDefinition.getChartFunction())) {
                addAccumulator(mongoGroupBy1, "MOJIX_COUNTER", reportDefinition.getChartFunction(), propertyAccumulator);
            }
            mongoGroupBy1.setGroupID(mapID);
            pipelineList.add(mongoGroupBy1);

            //add post-group
            //post-group counts values pre-grouped
            Map<String, Object> secondMapId = new HashMap<>(mapID.size());
            for (Map.Entry<String, Object> preGroup : mapID.entrySet()) {
                if (!"thingId".equals(preGroup.getKey())) {
                    secondMapId.put(preGroup.getKey(), "$_id." + preGroup.getKey());
                }
            }
            MongoGroupBy mongoGroupBy2 = new MongoGroupBy(secondMapId);
            addAccumulator(mongoGroupBy2, COUNT, reportDefinition.getChartFunction(), "MOJIX_COUNTER");
            pipelineList.add(mongoGroupBy2);
        } else {
            addAccumulator(mongoGroupBy1, COUNT, reportDefinition.getChartFunction(), propertyAccumulator);
            pipelineList.add(mongoGroupBy1);
        }
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        mongoLimit = Integer.valueOf(ConfigurationService.getAsString(user, "tableSummaryReportLimit"));
        logger.debug("limit " + mongoLimit);
    }

    private boolean isTimestampOrDate(ReportGroupBy groupBy) {
        ThingTypeField thingTypeField = groupBy.getThingTypeField();
        if (thingTypeField == null) {
            ThingType thingType = groupBy.getThingType();
            if (thingType != null) {
                thingTypeField = thingType.getThingTypeFieldByName(reverseTranslate(groupBy.getPropertyName()));
            }
        }
        if (thingTypeField != null) {
            DataType dataType = thingTypeField.getDataType();
            return (dataType != null && ThingTypeFieldService.isDateTimeStampType(dataType.getId()));
        }
        return false;
    }

    private Map<String, Object> propertyToObject(String property, String propertyTranslate, String unit, Integer index) {
        Map<String, Object> newMapID = new HashMap<>(1);
        if (!isEmptyOrNull(unit)) {
            Map<String, Object> mapUnits = new LinkedHashMap<>();
            // filter Date in report is Custom with starDate=null and endDate = null
            Long timeZoneOffset = new Date().getTimezoneOffset() * 60000L;
            if (isHistoricalReport()) {
                timeZoneOffset = (startDate == null ? endDate.getTimezoneOffset() : startDate.getTimezoneOffset()) * 60000L;
            }
            List<Object> subtractOffset = new ArrayList<>(1);
            String propertyCondition;
            if (reportDefinition.containsUnit() && reportDefinition.containsGroupByPartition()) {
                propertyCondition = "$" + TIME;
            } else {
                propertyCondition = "$" + propertyTranslate;
            }
            subtractOffset.add(propertyCondition);
            subtractOffset.add(timeZoneOffset);
            Map<String, Object> timeInLocalZone = createMap(1, "$subtract", subtractOffset);
            switch (unit.toLowerCase()) {
                //no breaks so that the final toQuery have the sum of the properties
                case HOUR:
                    mapUnits.put(HOUR, createMap(1, "$hour", timeInLocalZone));
                case DAY:
                    mapUnits.put(DAY, createMap(1, "$dayOfMonth", timeInLocalZone));
                case MONTH:
                    mapUnits.put(MONTH, createMap(1, "$month", timeInLocalZone));
                case YEAR:
                    mapUnits.put(YEAR, createMap(1, "$year", timeInLocalZone));
            }
            //when doing a group by date we need to make sure the date property exists
            if (!isOtherSet(index)) {
                addFilters(Operation.exists(propertyTranslate));
            }
            newMapID.put(property, createMapConditionToUnit(mapUnits, propertyCondition));
        } else {
            newMapID.put(property, "$" + propertyTranslate);
        }
        return newMapID;
    }

    private Map<String, Object> createMapConditionToUnit(Map<String, Object> conditionThen, String propertyCondition) {
        Map<String, Object> mapIDThenCondition = new LinkedHashMap<>(5);
        Map<String, Object> mapCondition = new LinkedHashMap<>(3);
        mapCondition.put("if", Collections.singletonMap("$gt", Arrays.asList(propertyCondition, 0)));
        mapCondition.put("then", conditionThen);
        mapCondition.put("else", null);
        mapIDThenCondition.put("$cond", mapCondition);
        return mapIDThenCondition;
    }

    private void calculateUnwinds(ReportGroupBy groupBy0, ReportGroupBy groupBy1) {
        if (groupBy0.getThingType() != null && groupBy1.getThingType() != null
                && !Objects.equals(groupBy0.getThingType(), groupBy1.getThingType())) {
            calculateUnwinds(groupBy0);
            calculateUnwinds(groupBy1);
        } else {
            calculateUnwinds(groupBy0);
        }
    }

    private void calculateUnwinds(ReportGroupBy groupBy) {
        if (thingType != null && groupBy.getThingType() != null) {
            ThingTypePath path = ThingTypePathService.getInstance().getPathByThingTypes(thingType, groupBy.getThingType());
            if (path != null) {
                List<String> directionMap = ThingTypeService.getInstance()
                        .getDirectionPath(thingType.getId(), groupBy.getThingType().getId());
                if (!directionMap.isEmpty()) {
                    for (int i = 1; i < directionMap.size(); i++) {
                        path = ThingTypePathService.getInstance().getPathByThingTypes(thingType, Long.valueOf(directionMap.get(i)));
                        if (path != null) {
                            if (groupBy.getOther() != null && groupBy.getOther()) {
                                pipelineList.add(new MongoUnwind("$" + verifiedSnapshotsProperty(path.getPath()), Boolean.TRUE));
                            } else {
                                pipelineList.add(new MongoUnwind("$" + verifiedSnapshotsProperty(path.getPath())));
                            }
                        }
                    }
                } else {
                    if (groupBy.getOther() != null && groupBy.getOther()) {
                        pipelineList.add(new MongoUnwind("$" + verifiedSnapshotsProperty(path.getPath()), Boolean.TRUE));
                    } else {
                        pipelineList.add(new MongoUnwind("$" + verifiedSnapshotsProperty(path.getPath())));
                    }
                }

            }
        }
    }

    private void addAccumulator(MongoGroupBy mongoGroupBy, String label, String accumulator, String propertyAccumulator) {
        mongoGroupBy.setLabelGroupBy(label);
        mongoGroupBy.setAccumulator(MongoGroupBy.Accumulator.COUNT, "1");
        if ((!isEmptyOrNull(accumulator) && !isEmptyOrNull(reportDefinition.getChartSummarizeBy()))
                || (!isEmptyOrNull(propertyAccumulator))) {
            String property = propertyAccumulator;
            if (!StringUtils.equals("MOJIX_COUNTER", propertyAccumulator)) {
                //This logic is for heat accumulator
                if (!isEmptyOrNull(propertyAccumulator)) {
                    property = propertyAccumulator.contains(".value") ? propertyAccumulator : propertyAccumulator + ".value";
                    accumulator = SUM;
                } else {
                    property = verifiedSnapshotsProperty(translateString(reportDefinition.getChartSummarizeBy(), null));
                }
            }
            if (SUM.equals(accumulator)) {
                mongoGroupBy.setAccumulator(MongoGroupBy.Accumulator.SUM, "$" + property);
            } else if (AVG.equals(accumulator)) {
                mongoGroupBy.setAccumulator(MongoGroupBy.Accumulator.AVG, "$" + property);
            } else if (MAX.equals(accumulator)) {
                mongoGroupBy.setAccumulator(MongoGroupBy.Accumulator.MAX, "$" + property);
            } else if (MIN.equals(accumulator)) {
                mongoGroupBy.setAccumulator(MongoGroupBy.Accumulator.MIN, "$" + property);
            }
        }
    }

    @Override
    protected String translateString(String propertyName, ThingType targetThingType) {
        if (propertyName != null && ReportDefinitionUtils.isDwell(propertyName)) {
            return ReportDefinitionUtils.stripDwell(propertyName) + ".dwellTime";
        }
        return super.translateString(propertyName, targetThingType);
    }

    private String groupProperty(ReportGroupBy reportGroupBy) {
        String property = translate(reportGroupBy, true);
        property = translateWithThingType(property, reportGroupBy.getThingType());
        logger.debug("property translated for distinct: " + property);
        return property;
    }

    private Map<String, Object> createMap(int size, String initialKey, Object initialValue) {
        Map<String, Object> map = new HashMap<>(size);
        map.put(initialKey, initialValue);
        return map;
    }

    public Boolean isOtherSet(Integer index) {
        // I inverted order from other row or other column
        if (index == 0) index = 1;
        else index = 0;
        Boolean other = false;
        if (reportDefinition.getReportGroupBy() != null && index < reportDefinition.getReportGroupBy().size()) {
            other = reportDefinition.getReportGroupBy().get(index).getOther();
        }

        return other;
    }

    @Override
    public Map<String, Object> exportResult(Long total, List<Map<String, Object>> records) {
        throw new UnsupportedOperationException("not necessary implementations ");
    }

    @Override
    public int getLimit() {
        return mongoLimit;
    }

    public List<Pipeline> getPipelineList() {
        return pipelineList;
    }

    @Override
    public boolean isTwoDimension() {
        return twoDimension;
    }

    public String getLabelPropertyTwo() {
        return axisLabelY;
    }

    public String getLabelPropertyOne() {
        return twoDimension ? axisLabelX : axisLabelY;
    }

    public boolean isTimestampOrDatePropertyOne() {
        return isTimestampOrDatePropertyOne;
    }

    public boolean isTimestampOrDatePropertyTwo() {
        return isTimestampOrDatePropertyTwo;
    }

    public Map<String, List<IRule>> getDisplayRulesSummary() {
        return displayRulesSummary;
    }

}
