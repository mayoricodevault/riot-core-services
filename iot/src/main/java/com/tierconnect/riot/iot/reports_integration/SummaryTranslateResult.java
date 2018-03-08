package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

import static com.tierconnect.riot.appcore.utils.Utilities.isEmptyOrNull;
import static com.tierconnect.riot.appcore.utils.Utilities.isNumber;
import static com.tierconnect.riot.commons.Constants.*;

/**
 * Created by vealaro on 2/16/17.
 */
public class SummaryTranslateResult extends TranslateResult {

    private static Logger logger = Logger.getLogger(SummaryTranslateResult.class);
    private List<String> properties1;
    private Map<String, String> mapProperties1Equivalency;
    private List<String> properties2;
    private Map<String, String> mapProperties2Equivalency;
    private Map<String, BigDecimal> grouping;
    public static final String X_ = "X_";
    public static final String Y_ = "Y_";
    private static final String TYPE_COLUMN = "column";
    private DecimalFormat decimalFormat = new DecimalFormat("00");
    private List<ReportGroupBy> groupByList;
    private SummaryReportConfig summaryReportConfig;
    protected boolean export = false;

    public SummaryTranslateResult(MapSummaryReportConfig configuration) {
        super(configuration);
        this.summaryReportConfig = configuration;
        this.groupByList = configuration.reportDefinition.getReportGroupBy();
    }

    public SummaryTranslateResult(TableSummaryReportConfig configuration) {
        super(configuration);
        this.summaryReportConfig = configuration;
        this.groupByList = configuration.reportDefinition.getReportGroupBy();
    }

    @Override
    public void exportResult(Map<String, Object> result) {

    }

    @SuppressWarnings("unchecked")
    public void loadAxisDistinct(List<Map<String, Object>> result) {
        if (summaryReportConfig.isTwoDimension()) {
            grouping = new HashMap<>();
            properties1 = new ArrayList<>();
            properties2 = new ArrayList<>();
            for (Map<String, Object> mapID : result) {
                BigDecimal value = count(mapID.get(COUNT));
                String valueProperty1 = getValueProperty(mapID.get(_ID), summaryReportConfig.getLabelPropertyOne(), groupByList.get(0));
                String valueProperty2 = getValueProperty(mapID.get(_ID), summaryReportConfig.getLabelPropertyTwo(), groupByList.get(1));
                //
                if (valueProperty1 != null && !properties1.contains(valueProperty1)) {
                    properties1.add(valueProperty1);
                }
                if (valueProperty2 != null && !properties2.contains(valueProperty2)) {
                    properties2.add(valueProperty2);
                }

                String labelGroup = OTHER;
                if (valueProperty1 != null && valueProperty2 != null) {
                    labelGroup = valueProperty1 + "+" + valueProperty2;
                } else if (valueProperty1 != null) {
                    labelGroup = X_ + valueProperty1;
                } else if (valueProperty2 != null) {
                    labelGroup = Y_ + valueProperty2;
                }
                grouping.put(labelGroup, value);
            }
        } else {
            loadAxisDistinctOneDimension(result);
        }
        limitChecking();
    }

    /**
     * Is responsible for verifying the limits configured in groups
     */
    private void limitChecking() {
        if (!export) {
            String maxNumberOfRows = ConfigurationService.getAsString(summaryReportConfig.reportDefinition.getGroup(), "max_number_of_rows");
            String maxNumberOfColumns = ConfigurationService.getAsString(summaryReportConfig.reportDefinition.getGroup(), "max_number_of_columns");
            if (isEmptyOrNull(maxNumberOfRows) || isEmptyOrNull(maxNumberOfColumns)) {
                throw new UserException("It is not possible to read \"Max Table Summary Columns\" and \"Max Table Summary Rows\" configuration.");
            }

            Long maxProperty1 = Long.valueOf(maxNumberOfRows);
            if (!summaryReportConfig.isTwoDimension()) {
                if (properties1.size() > maxProperty1) {
                    logger.info("Property " + groupByList.get(0).getLabel() + " has [" + properties1.size()
                            + "] items that exceed the allowed limit of [" + maxProperty1 + "]");
                    throw new UserException("Report results exceed configured Max Row display limit."
                            + " Do you want to export results instead?");     // max rows
                }
            } else {
                Long maxProperty2 = Long.valueOf(maxNumberOfColumns);
                if ((properties1.size() > maxProperty1) && (properties2.size() > maxProperty2)) {
                    logger.info("Property " + groupByList.get(0).getLabel() + " has [" + properties1.size()
                            + "] items that exceed the allowed limit of [" + maxProperty1 + "] \n"
                            + "Property " + groupByList.get(1).getLabel() + " has [" + properties2.size()
                            + "] items that exceed the allowed limit of [" + maxProperty2 + "]");
                    throw new UserException("Report results exceed configured Max Column and Max Row display limits. "
                            + "Do you want to export results instead?"); // max columns and rows
                } else if (properties1.size() > maxProperty1) {
                    logger.info("Property " + groupByList.get(0).getLabel() + " has [" + properties1.size()
                            + "] items that exceed the allowed limit of [" + maxProperty1 + "]");
                    throw new UserException("Report results exceed configured Max Row display limit. "
                            + "Do you want to export results instead?"); // max rows
                } else if (properties2.size() > maxProperty2) {
                    logger.info("Property " + groupByList.get(1).getLabel() + " has [" + properties2.size()
                            + "] items that exceed the allowed limit of [" + maxProperty2 + "]");
                    throw new UserException("Report results exceed configured Max Column display limit. "
                            + "Do you want to export results instead?"); // max columns
                }
            }
        }
    }

    protected void loadAxisDistinctOneDimension(List<Map<String, Object>> result) {
        grouping = new HashMap<>();
        properties1 = new ArrayList<>();
        for (Map<String, Object> mapID : result) {
            String valueProperty = getValueProperty(mapID.get(_ID), summaryReportConfig.getLabelPropertyTwo(), groupByList.get(0));
            if (valueProperty != null) {
                if (!properties1.contains(valueProperty)) {
                    properties1.add(valueProperty);
                }
                grouping.put(valueProperty, count(mapID.get(COUNT)));
            } else {
                grouping.put(OTHER, count(mapID.get(COUNT)));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void sortAxisProperties() {
        logger.info("Property to sort [" + groupByList.get(0).getLabel() + "] ");
        mapProperties1Equivalency = new HashMap<>(properties1.size());
        sortProperties(properties1, mapProperties1Equivalency, summaryReportConfig.getLabelPropertyOne());
        logger.info("Total ordered properties(1) [" + groupByList.get(0).getLabel() + "]:" + properties1.size());
        if (summaryReportConfig.isTwoDimension()) {
            logger.info("Property to sort [" + groupByList.get(0).getLabel() + "] ");
            mapProperties2Equivalency = new HashMap<>(properties2.size());
            sortProperties(properties2, mapProperties2Equivalency, summaryReportConfig.getLabelPropertyTwo());
            logger.info("Total ordered properties(2) [" + groupByList.get(1).getLabel() + "]:" + properties2.size());
        }
    }

    private void sortProperties(List<String> list, Map<String, String> mapEquivalency, String label) {
        if (list != null && !list.isEmpty()) {
            // special case with values 000000000473, 000000000474, 0000474
            Map<BigDecimal, List<String>> mapOriginalNumbers = new HashMap<>(list.size());
            List<String> stringList = new ArrayList<>();
            List<BigDecimal> numbersList = new ArrayList<>();
            for (String value : list) {
                if (isNumber(value)) {
                    if (StringUtils.split(label, ",").length >= 2) {
                        mapEquivalency.put(value, changeEmptyProperty(verifiedZonePropertyId(label, value)));
                        stringList.add(value);
                    } else {
                        BigDecimal temp = new BigDecimal(String.valueOf(value));
                        List<String> duplicates = new ArrayList<>();
                        if (!numbersList.contains(temp)) {
                            numbersList.add(temp);
                        }
                        if (mapOriginalNumbers.containsKey(temp)) {
                            duplicates = mapOriginalNumbers.get(temp);
                        }
                        duplicates.add(value);
                        mapOriginalNumbers.put(temp, duplicates);
                    }
                } else {
                    mapEquivalency.put(value, changeEmptyProperty(value));
                    stringList.add(value);
                }
            }
            Collections.sort(numbersList);
            Collections.sort(stringList, String.CASE_INSENSITIVE_ORDER);
            list.clear();
            for (BigDecimal number : numbersList) {
                if (mapOriginalNumbers.get(number) != null && !mapOriginalNumbers.get(number).isEmpty()) {
                    mapEquivalency.put(mapOriginalNumbers.get(number).get(0), mapOriginalNumbers.get(number).get(0));
                }
                List<String> duplicates = mapOriginalNumbers.get(number);
                Collections.sort(duplicates);
                list.addAll(duplicates);
                mapOriginalNumbers.remove(number);
            }
            list.addAll(stringList);
        }
    }

    //    one dimension process
    protected List<Map<String, Object>> processSeriesOneDimension() {
        List<Map<String, Object>> series = new ArrayList<>(3);
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal temp;
        for (String property : properties1) {
            temp = safeBigdecimal(property);
            series.add(
                    getRowOneDimension(temp, executionRules(mapProperties1Equivalency.get(property), temp),
                            translateProperty(summaryReportConfig.getLabelPropertyOne(),
                                    property, summaryReportConfig.isTimestampOrDatePropertyOne())));
            total = temp.add(total);
        }
        // property others
        if (summaryReportConfig.isOtherSet(1)) {
            temp = safeBigdecimal(OTHER);
            series.add(getRowOneDimension(temp, executionRules(OTHER_Y, temp), OTHER_Y));
            total = temp.add(total);
        }
        if (configuration.reportDefinition.getHorizontalTotal()) {
            series.add(getRowOneDimension(total, null, TOTAL));
        }
        return series;
    }

    protected BigDecimal sumArrayOfBigdecimal(List<BigDecimal> listTotal) {
        BigDecimal result = BigDecimal.ZERO;
        for (BigDecimal data : listTotal) {
            result = data.add(result);
        }
        return result;
    }

    protected List<BigDecimal> sumTotalRow(List<BigDecimal> rowArray, List<BigDecimal> totalArray) {
        if (totalArray.isEmpty()) {
            totalArray.addAll(rowArray);
        } else {
            for (int i = 0; i < rowArray.size(); i++) {
                totalArray.set(i, rowArray.get(i).add(totalArray.get(i)));
            }
        }
        return totalArray;
    }

    private String executionRules(String labels, BigDecimal value) {
        String color = configuration.reportDefinition.getDefaultColorIcon();
        if (summaryReportConfig.getDisplayRulesSummary() != null && !summaryReportConfig.getDisplayRulesSummary().isEmpty()) {
            String[] labelArray = labels.split("\\+");
            String label1 = labelArray[0];
            String label2 = labelArray.length > 1 ? labelArray[1] : null;
            boolean executedLabel1 = false;
            boolean executedLabel2 = false;
            Map<String, Object> ruleValues = new HashMap<>(2);
            ruleValues.put(SummaryRule.RULE_LABEL, label1);
            ruleValues.put(SummaryRule.RULE_VALUE, value);
            List<IRule> summaryRuleList = summaryReportConfig.getDisplayRulesSummary().get(label1);
            if (summaryRuleList != null) {
                for (IRule iRule : summaryRuleList) {
                    if (!iRule.isExecuted() && iRule.matches(ruleValues)) {
                        executedLabel1 = true;
                        color = iRule.getColor();
                        if (iRule.getStopOnMatch()) {
                            iRule.setExecuted(true);
                            return color;
                        }
                    }
                }
            }
            ruleValues.clear();
            ruleValues.put(SummaryRule.RULE_LABEL, label2);
            ruleValues.put(SummaryRule.RULE_VALUE, value);
            summaryRuleList = summaryReportConfig.getDisplayRulesSummary().get(label2);
            if (summaryRuleList != null && !executedLabel1) {
                for (IRule iRule : summaryRuleList) {
                    if (!iRule.isExecuted() && iRule.matches(ruleValues)) {
                        color = iRule.getColor();
                        executedLabel2 = true;
                        if (iRule.getStopOnMatch()) {
                            iRule.setExecuted(true);
                            return color;
                        }
                    }
                }
            }
            if (!executedLabel1 && !executedLabel2 && summaryReportConfig.getDisplayRulesSummary().containsKey(SummaryRule.ALL_PROPERTIES)) {
                summaryRuleList = summaryReportConfig.getDisplayRulesSummary().get(SummaryRule.ALL_PROPERTIES);
                ruleValues.put(SummaryRule.RULE_LABEL, SummaryRule.ALL_PROPERTIES);
                for (IRule iRule : summaryRuleList) {
                    if (!iRule.isExecuted() && iRule.matches(ruleValues)) {
                        color = iRule.getColor();
                        if (iRule.getStopOnMatch()) {
                            iRule.setExecuted(true);
                            return color;
                        }
                    }
                }
            }
        }
        return color;
    }

    public String changeEmptyProperty(String value) {
        if (StringUtils.isEmpty(value)) {
            return PROPERTY_EMPTY;
        }
        return value;
    }

    private String concatProperties(String value1, String value2) {
        return mapProperties1Equivalency.get(value1) + "+" + mapProperties2Equivalency.get(value2);
    }

    protected List<Map<String, Object>> processSeriesTwoDimension() {
        List<Map<String, Object>> series = new ArrayList<>();
        List<BigDecimal> rowCountTotal = new ArrayList<>();

        List<String> colors;
        List<BigDecimal> rowCount;
        for (String property1 : properties1) {
            colors = new ArrayList<>();
            rowCount = new ArrayList<>();
            for (String property2 : properties2) {
                colors.add(executionRules(concatProperties(property1, property2), safeBigdecimal(property1 + "+" + property2)));
                rowCount.add(safeBigdecimal(property1 + "+" + property2));
            }
            // add an other column
            if (summaryReportConfig.isOtherSet(0)) {
                colors.add(executionRules(property1 + "+" + OTHER, safeBigdecimal(X_ + property1)));
                rowCount.add(safeBigdecimal(X_ + property1));
            }
            // add vertical total
            if (configuration.reportDefinition.getVerticalTotal()) {
                rowCount.add(sumArrayOfBigdecimal(rowCount));
            }
            // add to series
            series.add(getRowTwoDimension(colors, rowCount,
                    translateProperty(summaryReportConfig.getLabelPropertyOne(),
                            property1, summaryReportConfig.isTimestampOrDatePropertyOne())
            ));
            // update total array
            if (configuration.reportDefinition.getHorizontalTotal()) {
                rowCountTotal = sumTotalRow(rowCount, rowCountTotal);
            }
        }

        // calculate other row
        if (summaryReportConfig.isOtherSet(1)) {
            colors = new ArrayList<>();
            rowCount = new ArrayList<>();
            for (String property2 : properties2) {
                colors.add(executionRules(property2 + "+" + OTHER_Y, safeBigdecimal(Y_ + property2)));
                rowCount.add(safeBigdecimal(Y_ + property2));
            }

            // add one if other column us set
            if (summaryReportConfig.isOtherSet(0)) {
                colors.add(executionRules(OTHER + "+" + OTHER_Y, safeBigdecimal(OTHER)));
                rowCount.add(safeBigdecimal(OTHER));
            }

            // add total vertical
            if (configuration.reportDefinition.getVerticalTotal()) {
                rowCount.add(sumArrayOfBigdecimal(rowCount));
            }

            series.add(getRowTwoDimension(colors, rowCount, OTHER_Y));

            // update total array
            if (configuration.reportDefinition.getHorizontalTotal()) {
                rowCountTotal = sumTotalRow(rowCount, rowCountTotal);
            }
        }

        // add the total row
        if (configuration.reportDefinition.getHorizontalTotal()) {
            series.add(getRowTwoDimension(null, rowCountTotal, TOTAL));
        }
        return series;
    }

    private List<String> translateProperty(String label, List<String> properties, boolean isTimestampOrDate) {
        if (isTimestampOrDate) {
            for (int i = 0; i < properties.size(); i++) {
                if (Utilities.isNumber(properties.get(i))) {
                    properties.set(i, processTimestampValueJSONString(Long.valueOf(properties.get(i)), summaryReportConfig.dateFormatAndTimeZone));
                }
            }
        }
        return verifiedZonePropertyId(label, properties);
    }

    private Object translateProperty(String label, String property, boolean isTimestampOrDate) {
        if (isTimestampOrDate && Utilities.isNumber(property)) {
            return processTimestampValue(Long.valueOf(property), summaryReportConfig.dateFormatAndTimeZone);
        }
        return verifiedZonePropertyId(label, property);
    }

    protected String verifiedZonePropertyId(String label, String property) {
        String[] labelArray = StringUtils.split(label, ",");
        if (labelArray.length == 3) {
            return addValueZonePropertyId(property, labelArray[1]);
        }
        return property;
    }

    private List<String> verifiedZonePropertyId(String label, List<String> properties) {
        String[] labelArray = StringUtils.split(label, ",");
        if (labelArray.length == 3) {
            for (int i = 0; i < properties.size(); i++) {
                properties.set(i, addValueZonePropertyId(properties.get(i), labelArray[1]));
            }
        }
        return properties;
    }

    private String addValueZonePropertyId(String idZone, String idPropertyId) {
        Zone zone = summaryReportConfig.getMapZone().get(idZone);
        if (zone != null) {
            return zone.getName() + " (" + summaryReportConfig.getValueOfZoneProperty(idZone, idPropertyId) + ")";
        }
        return Constants.UNKNOWN_ZONE_NAME + " (" + summaryReportConfig.getValueOfZoneProperty(idZone, idPropertyId) + ")";
    }

    protected BigDecimal safeBigdecimal(String key) {
        BigDecimal safeBigDecimal = BigDecimal.ZERO;
        if (grouping.get(key) != null) {
            safeBigDecimal = grouping.get(key);
        }
        return safeBigDecimal;
    }

    protected void exportMap(List<Map<String, Object>> series, String nameResult) {
        labelValues = new LinkedHashMap<>(10);
        labelValues.put("startDate", configuration.startDate != null ? configuration.startDate.getTime() : null);
        labelValues.put("endDate", configuration.endDate != null ? configuration.endDate.getTime() : null);

        List<String> properties = Collections.emptyList();
        if (summaryReportConfig.isTwoDimension()) {
            properties = translateProperty(summaryReportConfig.getLabelPropertyTwo(), properties2, summaryReportConfig.isTimestampOrDatePropertyTwo());
            if (summaryReportConfig.isOtherSet(0)) {
                properties.add("otherX");
            }
            if (configuration.reportDefinition.getVerticalTotal()) {
                properties.add(TOTAL);
            }
        }
        labelValues.put("xAxis", properties);

        // to result
        labelValues.put("thingFieldTypeResult", String.valueOf(ThingTypeField.Type.TYPE_NUMBER.value));
        if (configuration.reportDefinition.getChartSummarizeBy() != null
                && ReportDefinitionUtils.isDwell(configuration.reportDefinition.getChartSummarizeBy())) {
            labelValues.put("thingFieldTypeResult", "0");
        }

        if (groupByList != null && groupByList.size() > 0) {
            labelValues.put("labelY", groupByList.get(0).getLabel());
            labelValues.put("title", configuration.reportDefinition.getName());

            labelValues.put("labelX", summaryReportConfig.isTwoDimension() ? groupByList.get(1).getLabel() : StringUtils.EMPTY);
            labelValues.put("yAxis", summaryReportConfig.isTwoDimension() ? groupByList.get(1).getLabel() : StringUtils.EMPTY);

            // get field Type1
            labelValues.put("thingFieldTypeY", getDataTypeOfThingField(groupByList.get(0)));
            //
            if (groupByList.size() > 1) {
                labelValues.put("thingFieldTypeX", getDataTypeOfThingField(groupByList.get(1)));
            }
        }
        labelValues.put(nameResult, series);
    }

    private String getDataTypeOfThingField(ReportGroupBy groupBy) {
        ThingType thingType1 = groupBy.getThingType() != null ? groupBy.getThingType() : null;
        ThingTypeField thingTypeField1 = thingType1 != null ?
                thingType1.getThingTypeFieldByName(configuration.reverseTranslate(groupBy.getPropertyName())) :
                null;
        String fieldType;
        if (thingTypeField1 == null) {
            fieldType = "1";
        } else if (groupBy.getPropertyName().endsWith(".time")) {
            fieldType = "11";
        } else {
            fieldType = thingTypeField1.getDataType().getId().toString();
        }
        return fieldType;
    }

    private Map<String, Object> getRowOneDimension(BigDecimal count, String color, Object name) {
        if (color != null) {
            return getRowTwoDimension(Collections.singletonList(color), Collections.singletonList(count), name);
        }
        return getRowTwoDimension(null, Collections.singletonList(count), name);
    }

    private Map<String, Object> getRowTwoDimension(List<String> colors, List<BigDecimal> rowCounts, Object name) {
        Map<String, Object> row = new HashMap<>(4);
        row.put("type", TYPE_COLUMN);
        row.put("data", rowCounts);
        row.put("name", name);
        if (!(name instanceof Map)) {
            row.put("name", String.valueOf(name));
        }
        if (colors != null) {
            row.put("colors", colors);
        }
        return row;
    }

    //get safe count
    protected BigDecimal count(Object countObj) {
        BigDecimal count = BigDecimal.ZERO;
        logger.trace("countObj=" + countObj);
        if (countObj instanceof Integer) {
            count = new BigDecimal((Integer) countObj);
        } else if (countObj instanceof Double) {
            count = new BigDecimal(String.valueOf(countObj));
        } else if (countObj instanceof Long) {
            count = new BigDecimal(String.valueOf(countObj));
        } else if (countObj instanceof BigDecimal) {
            count = (BigDecimal) countObj;
        } else if (countObj instanceof String
                && !isEmptyOrNull((String) countObj)
                && StringUtils.isNumeric((String) countObj)) {
            count = new BigDecimal((String) countObj);
        }
        return count;
    }

    private String getUnitValue(Object _ID) {
        Object value = _ID;
        if (_ID != null) {
            if (((Map) _ID).containsKey("year")) {
                value = count(((Map) _ID).get("year")).longValue();
            }
            if (((Map) _ID).containsKey("month")) {
                value = value + "/" + decimalFormat.format(((Map) _ID).get("month"));
            }
            if (((Map) _ID).containsKey("day")) {
                value = value + "/" + decimalFormat.format(((Map) _ID).get("day"));
            }
            if (((Map) _ID).containsKey("hour")) {
                value = value + " " + decimalFormat.format(((Map) _ID).get("hour")) + ":00";
            }
        }
        return (value != null ? String.valueOf(value) : null);
    }

    protected String getValueProperty(Object mapID, String key, ReportGroupBy groupBy) {
        Object newValue = null;
        if (mapID != null) {
            if (!isEmptyOrNull(groupBy.getUnit())) {
                newValue = getUnitValue(((Map) mapID).get(key));
            } else {
                newValue = translateValue(mapID, key, groupBy);
            }
        }
        return (newValue != null ? String.valueOf(newValue) : null);

    }

    private String translateValue(Object value, String key, ReportGroupBy groupBy) {
        Object newValue = value;
        if (value != null) {
            if (value instanceof Map) {
                newValue = ((Map) value).get(key);
            }
            if (newValue instanceof Collection) {
                newValue = StringUtils.join((List) newValue, ",");
            }
            ThingTypeField thingTypeField = groupBy.getThingTypeField();
            if (thingTypeField == null) {
                ThingType thingType = groupBy.getThingType();
                if (thingType != null) {
                    thingTypeField = thingType.getThingTypeFieldByName(configuration.reverseTranslate(groupBy.getPropertyName()));
                }
            }
            if (thingTypeField != null) {
                DataType dataType = thingTypeField.getDataType();
                if (dataType != null) {
                    if (dataType.getId().equals(ThingTypeField.Type.TYPE_DATE.value) && (newValue instanceof Date)) {
                        newValue = String.valueOf(((Date) newValue).getTime());
                    }
                }
            }
        }
        return (newValue != null ? String.valueOf(newValue) : null);
    }

    public List<String> getProperties1() {
        return properties1;
    }

    public List<String> getProperties2() {
        return properties2;
    }

    public Map<String, BigDecimal> getGrouping() {
        return grouping;
    }

    public SummaryReportConfig getSummaryReportConfig() {
        return summaryReportConfig;
    }
}
