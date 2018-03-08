package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.api.database.base.FactoryDataBase;
import com.tierconnect.riot.api.database.mongo.Mongo;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.utils.DateHelper;
import com.tierconnect.riot.iot.entities.ReportDefinitionUtils;
import com.tierconnect.riot.iot.entities.ReportGroupBy;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tierconnect.riot.commons.Constants.*;

/**
 * Created by vealaro on 2/13/17.
 */
public class TableSummaryTranslateExport extends TableSummaryTranslateResult {

    private static Logger logger = Logger.getLogger(TableSummaryTranslateExport.class);
    private List<Map<String, Object>> records;
    private Long count = null;
    private Long dataTypeX;
    private Long dataTypeY;
    private Long dataTypeResult;

    public TableSummaryTranslateExport(TableSummaryReportConfig tableSummaryReportConfig) {
        super(tableSummaryReportConfig);
        export = true;
    }

    public File exportSummary() throws IOException {
        executeAggregate();
        loadAxisDistinct(records);
        sortAxisProperties();
        setDataType();
        File file = null;
        CSVPrinter printer = null;
        try {
            file = File.createTempFile("export", ".csv");
            file.deleteOnExit();
            FileOutputStream fileStream = new FileOutputStream(file);
            OutputStreamWriter writer = new OutputStreamWriter(fileStream, Charset.defaultCharset());
            printer = processHeaders(writer);
            if (summaryReportConfig.isTwoDimension()) {
                processSeriesTwoDimensionExport(printer);
            } else {
                processSeriesOneDimensionExport(printer);
            }
        } catch (IOException e) {
            logger.error("Error export report with name " + summaryReportConfig.reportDefinition.getName(), e);
            throw new UserException("Error in export report", e);
        } finally {
            if (printer != null) {
                printer.flush();
                printer.close();
            }
        }
        return file;
    }

    private void executeAggregate() {
        try {
            long start = System.currentTimeMillis();
            Mongo database = FactoryDataBase.get(Mongo.class, summaryReportConfig.getFilters());
            database.executeAggregate(summaryReportConfig.getCollectionTarget(), summaryReportConfig.getPipelineList(), summaryReportConfig.getLimit());
            count = database.getCountAll();
            records = database.getResultSet();
            logger.info("Records Size " + count + " in " + (System.currentTimeMillis() - start) + " ms, before export");
        } catch (Exception e) {
            logger.error("Error in execution aggregation to Mongo", e);
        }
    }

    public void processSeriesTwoDimensionExport(CSVPrinter printer) throws IOException {
        List<BigDecimal> rowCountTotal = new ArrayList<>();
        List<BigDecimal> rowCount;
        List<String> rowCountString;

        List<String> properties1 = getProperties1();
        BigDecimal temp;
        Long countProperty = 1L;
        for (String property1 : properties1) {
            rowCount = new ArrayList<>();
            rowCountString = new ArrayList<>();
            rowCountString.add(formatProperty(property1, dataTypeX, countProperty, summaryReportConfig.getLabelPropertyOne()));
            countProperty++;
            for (String property2 : getProperties2()) {
                temp = safeBigdecimal(property1 + "+" + property2);
                rowCount.add(temp);
                rowCountString.add(formatObject(temp, dataTypeResult));
            }
            // add an other column
            if (summaryReportConfig.isOtherSet(0)) {
                temp = safeBigdecimal(X_ + property1);
                rowCount.add(temp);
                rowCountString.add(formatObject(temp, dataTypeResult));
            }
            // add vertical total
            if (summaryReportConfig.reportDefinition.getVerticalTotal()) {
                temp = sumArrayOfBigdecimal(rowCount);
                rowCount.add(temp);
                rowCountString.add(formatObject(temp, dataTypeResult));
            }
            // add to body
            printer.printRecord(rowCountString);
            // update total array
            if (summaryReportConfig.reportDefinition.getHorizontalTotal()) {
                rowCountTotal = sumTotalRow(rowCount, rowCountTotal);
            }
        }

        // calculate other row
        if (summaryReportConfig.isOtherSet(1)) {
            rowCount = new ArrayList<>();
            rowCountString = new ArrayList<>();
            rowCountString.add(OTHER_Y);
            for (String property2 : getProperties2()) {
                temp = safeBigdecimal(Y_ + property2);
                rowCount.add(temp);
                rowCountString.add(formatObject(temp, dataTypeResult));
            }

            // add one if other column us set
            if (summaryReportConfig.isOtherSet(0)) {
                temp = safeBigdecimal(OTHER);
                rowCount.add(temp);
                rowCountString.add(formatObject(temp, dataTypeResult));
            }

            // add total vertical
            if (summaryReportConfig.reportDefinition.getVerticalTotal()) {
                temp = sumArrayOfBigdecimal(rowCount);
                rowCount.add(temp);
                rowCountString.add(formatObject(temp, dataTypeResult));
            }

            printer.printRecord(rowCountString);
            // update total array
            if (summaryReportConfig.reportDefinition.getHorizontalTotal()) {
                rowCountTotal = sumTotalRow(rowCount, rowCountTotal);
            }
        }
        rowCountString = null;

        // add the total row
        if (summaryReportConfig.reportDefinition.getHorizontalTotal()) {
            List<String> rowCountTotalString = new ArrayList<>();
            rowCountTotalString.add(TOTAL);
            for (BigDecimal total : rowCountTotal) {
                rowCountTotalString.add(formatObject(total, dataTypeResult));
            }
            printer.printRecord(rowCountTotalString);
            rowCountTotalString = null;
        }
    }

    public void processSeriesOneDimensionExport(CSVPrinter printer) throws IOException {
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal temp;
        Long countProperty = 1L;
        for (String property : getProperties1()) {
            temp = safeBigdecimal(property);
            printer.printRecord(getRowOneDimension(property, temp, countProperty));
            total = temp.add(total);
            countProperty++;
        }
        // property others
        if (summaryReportConfig.isOtherSet(1)) {
            temp = safeBigdecimal(OTHER);
            printer.printRecord(getRowOneDimension(OTHER_Y, temp, null));
            total = temp.add(total);
        }
        if (summaryReportConfig.reportDefinition.getHorizontalTotal()) {
            printer.printRecord(getRowOneDimension(TOTAL, total, null));
        }
    }

    private List<String> getRowOneDimension(String property, BigDecimal value, Long countProperty) {
        List<String> row = new ArrayList<>(2);
        row.add(formatProperty(property, dataTypeX, countProperty, summaryReportConfig.getLabelPropertyOne()));
        row.add(formatObject(value, dataTypeResult));
        return row;
    }

    private void setDataType() {
        dataTypeResult = ThingTypeField.Type.TYPE_NUMBER.value;
        if (summaryReportConfig.reportDefinition.getChartSummarizeBy() != null
                && ReportDefinitionUtils.isDwell(summaryReportConfig.reportDefinition.getChartSummarizeBy())) {
            dataTypeResult = 0L; // Dwelltime value
        }
        List<ReportGroupBy> groups = summaryReportConfig.reportDefinition.getReportGroupBy();
        dataTypeX = getDatatype(groups.get(0));
        if (summaryReportConfig.isTwoDimension()) {
            dataTypeY = getDatatype(groups.get(1));
        }
    }

    private Long getDatatype(ReportGroupBy groupBy) {
        ThingType thingType1 = groupBy.getThingType() != null ? groupBy.getThingType() : null;
        ThingTypeField thingTypeField1 = thingType1 != null ?
                thingType1.getThingTypeFieldByName(summaryReportConfig.reverseTranslate(groupBy.getPropertyName())) :
                null;
        Long fieldType = ThingTypeField.Type.TYPE_TEXT.value;
        if (thingTypeField1 != null) {
            fieldType = thingTypeField1.getDataType().getId();
        }
        return fieldType;
    }

    private CSVPrinter processHeaders(OutputStreamWriter writer) throws IOException {
        List<String> headers = new ArrayList<>();
        List<ReportGroupBy> groups = summaryReportConfig.reportDefinition.getReportGroupBy();
        String labelX = groups.get(0).getLabel();
        if (summaryReportConfig.isTwoDimension()) {
            String label = groups.get(1).getLabel();
            headers.add(labelX);
            Long countProperty = 1L;
            for (String property2 : getProperties2()) {
                headers.add(label + ":" + formatProperty(property2, dataTypeY, countProperty, summaryReportConfig.getLabelPropertyTwo()));
                countProperty++;
            }
            if (summaryReportConfig.isOtherSet(0)) {
                headers.add(OTHER);
            }
            if (summaryReportConfig.reportDefinition.getVerticalTotal()) {
                headers.add(TOTAL);
            }
        } else {
            headers.add(labelX);
            headers.add(StringUtils.EMPTY);
        }
        String[] arrayHeader = new String[headers.size()];
        headers.toArray(arrayHeader);
        return CSVFormat.EXCEL.withHeader(arrayHeader).print(writer);
    }

    private String formatProperty(String property, Long dataType, Long countProperty, String label) {
        String newProperty = changeEmptyProperty(property);
        if (!PROPERTY_EMPTY.equals(newProperty)) {
            if (label != null) {
                newProperty = verifiedZonePropertyId(label, property);
            }
            if (Utilities.isNumber(newProperty) && !dataType.equals(ThingTypeField.Type.TYPE_TEXT.value)) {
                newProperty = formatObject(new BigDecimal(property), dataType);
            }
            if (ThingTypeFieldService.isDateTimeStampType(dataType) && countProperty != null) {
                newProperty = "(" + countProperty + "):" + newProperty;
            }
        }
        return newProperty;
    }

    private String formatObject(BigDecimal value, Long dataType) {
        String newValue = String.valueOf(value);
        if (ThingTypeFieldService.isDateTimeStampType(dataType)
                && StringUtils.isNumeric(String.valueOf(value.longValue()))) {
            newValue = summaryReportConfig.dateFormatAndTimeZone.format(value.longValue());
        } else if (dataType.equals(0L) && StringUtils.isNumeric(String.valueOf(value.longValue()))) {
            newValue = DateHelper.formatDwellTime(value.longValue());
        }
        return newValue;
    }
}
