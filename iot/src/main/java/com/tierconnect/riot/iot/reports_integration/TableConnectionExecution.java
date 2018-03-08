package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.iot.entities.ReportCustomFilter;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.entities.ReportDefinitionConfig;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;

import java.io.File;
import java.util.*;

import static com.tierconnect.riot.appcore.utils.Utilities.isEmptyOrNull;
import static com.tierconnect.riot.iot.entities.ReportCustomFilter.isDateType;

/**
 * Created by julio.rocha on 06-06-17.
 */
public class TableConnectionExecution {
    private static final Logger logger = Logger.getLogger(TableConnectionExecution.class);
    private static final String DATE_FILTER_TEMPLATE = "startDate:=%1$s&endDate:=%2$s&now:=%3$s&operator:=%4$s";
    private static final String EXTENDED_FILTER_TEMPLATE = "%1$s|%2$s|%3$s&";

    private static final String DATE_FORMAT_CONFIGURATION = "@@dateFormatConfiguration@@";
    private static final String TIME_ZONE_CONFIGURATION = "@@timeZoneConfiguration@@";
    private static final String PAGE_NUMBER = "@@pageNumber@@";
    private static final String PAGE_SIZE = "@@pageSize@@";
    private static final String FILTER_EXTENDED = "@@filtersExtended@@";
    private static final String SYSTEM_INFORMATION = "@@systemInformation@@";
    private static final String DATE_FILTER_STRING = "@@dateFiltersString@@";


    private ReportDefinition reportDefinition;
    private TableScriptConfig tableScriptConfig;
    private Map<String, Object> values;
    private String requestInfo;
    private boolean export;
    private final boolean isSentEmail;
    private Date startDate;
    private Date endDate;
    private Date now;
    private Integer pageSize;
    private Integer pageNumber;
    private String dateFiltersString;
    private String systemInformation;
    private String filtersExtended;
    private String timeZoneConfiguration;
    private String dateFormatConfiguration;
    private DateFormatAndTimeZone dateFormatAndTimeZone;
    private Connection connection;
    private String query;
    private String singleQuery;
    private Map<String, Object> options;

    public TableConnectionExecution(ReportDefinition reportDefinition,
                                    Map<String, Object> values,
                                    Integer pageSize,
                                    Integer pageNumber,
                                    Date startDate,
                                    Date endDate,
                                    Date now,
                                    String requestInfo,
                                    TableScriptConfig tableScriptConfig,
                                    boolean isSentEmail,
                                    boolean export) {
        this.reportDefinition = reportDefinition;
        this.values = values;
        this.pageSize = (pageSize == null) ? -1 : pageSize;
        this.pageNumber = (pageNumber == null) ? 1 : pageNumber;
        this.startDate = startDate;
        this.endDate = endDate;
        this.now = now;
        this.requestInfo = requestInfo;
        this.tableScriptConfig = tableScriptConfig;
        this.isSentEmail = isSentEmail;
        this.export = export;
        buildParameters();
    }

    public Map<String, Object> run() {
        logger.info("RUNNING RAW SQL REPORT NAME = " + reportDefinition.getName());
        logger.info("RUNNING WITH OPTIONS: " + options);
        NativeQueryExecutor nqe = new NativeQueryExecutor(connection);
        List<List<Object>> queryResult = nqe.executeQuery(query, requestInfo);
        Map<String, Object> response = new LinkedHashMap<>();
        if (isGreatherThanColumnsPermitted(nqe.getColumns().size())) {
            logger.info("Table Connection has [" + nqe.getColumns().size()
                    + "] items that exceed the allowed limit of [" + nqe.getColumns().size() + "]");
            throw new UserException("Report results exceed configured Max Columns display limit."
                    + " Do you want to export results instead?");
        }
        response.put("columnNames", nqe.getColumns());
        response.put("data", queryResult);
        response.put("totalRows", nqe.getTotalRows());
        response.put("options", options);
        return response;
    }

    /**
     * Check if quantity of columns is greather than Max Number Of Columns configured
     *
     * @param numberOfColumns Number of columns of the result
     * @return true if is greather .
     */
    public boolean isGreatherThanColumnsPermitted(int numberOfColumns) {
        String maxNumberOfColumns = ConfigurationService.getAsString(tableScriptConfig.user, "max_number_of_table_columns");
        if (isEmptyOrNull(maxNumberOfColumns)) {
            throw new UserException("It is not possible to read \"Max Table Columns\" configuration.");
        }
        return numberOfColumns > Long.parseLong(maxNumberOfColumns);
    }


    public File export() {
        logger.info("EXPORTING RAW SQL REPORT NAME = " + reportDefinition.getName());
        logger.info("EXPORTING WITH OPTIONS: " + options);
        NativeQueryExecutor nqe = new NativeQueryExecutor(connection);
        return nqe.saveQueryInFile(query, requestInfo, singleQuery);
    }

    private void buildParameters() {
        if (isSentEmail) {
            Group reportGroup = reportDefinition.getGroup();
            Group group = reportGroup.getParentLevel3() != null ?
                    reportGroup.getParentLevel3() : reportGroup.getParentLevel2();
            dateFormatAndTimeZone = GroupService.getInstance().getDateFormatAndTimeZone(group);
            this.dateFormatConfiguration = dateFormatAndTimeZone.getMomentDateFormat();
            this.timeZoneConfiguration = dateFormatAndTimeZone.getTimeZone();
            logger.info("EMAIL RUN AS GROUP [" + group.getName() + "]");
        } else {
            User user = (User) SecurityUtils.getSubject().getPrincipal();
            dateFormatAndTimeZone = UserService.getInstance().getDateFormatAndTimeZone(user);
            this.dateFormatConfiguration = dateFormatAndTimeZone.getMomentDateFormat();
            this.timeZoneConfiguration = dateFormatAndTimeZone.getTimeZone();
            if (export) {
                logger.info("EXPORT WITH USER [" + user.getUsername() + "]");
            } else {
                logger.info("RUNNING WITH USER [" + user.getUsername() + "]");
            }
        }
        logger.info("REGIONAL SETTING  timezone :" + this.timeZoneConfiguration);
        logger.info("REGIONAL SETTING  Date Format:" + this.dateFormatConfiguration);
        reportDefinition.setDateFormatAndTimeZone(dateFormatAndTimeZone);

        StringBuilder sb = new StringBuilder(parseCustomFilters());
        Map<String, Map<String, Object>> filters = this.tableScriptConfig.filterMap;
        for (String k : filters.keySet()) {
            if (k.equals("Date")) {
                continue;
            }
            Map<String, Object> values = filters.get(k);
            if (!"systemInformation".equals(k)) {
                sb.append(String.format(EXTENDED_FILTER_TEMPLATE, k, values.get("operator"), values.get("value")));
            } else {
                StringBuilder sbSysInfo = new StringBuilder();
                for (String ksi : values.keySet()) {
                    Map<String, Object> sysInfoElem = (Map<String, Object>) values.get(ksi);
                    for (String ksiE : sysInfoElem.keySet()) {
                        if (ksiE.startsWith("userFields")) {
                            continue;
                        }
                        if (!ksiE.startsWith("user")) {
                            sbSysInfo.append(ksi)
                                    .append(ksiE.substring(0, 1).toUpperCase()).append(ksiE.substring(1))
                                    .append(":=").append(sysInfoElem.get(ksiE)).append("&");
                        }
                    }
                }
                systemInformation = sbSysInfo.substring(0, sbSysInfo.length() - 1);
            }
        }

        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }

        filtersExtended = sb.toString();
        dateFiltersString = getDateFilterString();
        ReportDefinitionConfig script = reportDefinition.getReportDefConfigItem("SCRIPT");
        ReportDefinitionConfig connectionStr = reportDefinition.getReportDefConfigItem("CONNECTION");
        connection = ConnectionService.getInstance().get(Long.valueOf(connectionStr.getKeyValue()));
        query = script.getKeyValue();
        query = query.replaceAll(DATE_FORMAT_CONFIGURATION, dateFormatConfiguration)
                .replaceAll(TIME_ZONE_CONFIGURATION, timeZoneConfiguration)
                .replaceAll(DATE_FILTER_STRING, dateFiltersString)
                .replaceAll(SYSTEM_INFORMATION, systemInformation)
                .replaceAll(DATE_FILTER_STRING, dateFiltersString)
                .replaceAll(FILTER_EXTENDED, filtersExtended);
        if (export) {
            singleQuery = query.replaceAll(PAGE_SIZE, "1")
                    .replaceAll(PAGE_NUMBER, "1");
        }
        query = query.replaceAll(PAGE_SIZE, pageSize.toString())
                .replaceAll(PAGE_NUMBER, pageNumber.toString());

        options = new HashMap<>();
        options.put(PAGE_SIZE, pageSize);
        options.put(PAGE_NUMBER, pageNumber);
        options.put(DATE_FORMAT_CONFIGURATION, dateFormatConfiguration);
        options.put(TIME_ZONE_CONFIGURATION, timeZoneConfiguration);
        options.put(DATE_FILTER_STRING, dateFiltersString);
        options.put(SYSTEM_INFORMATION, systemInformation);
        options.put(DATE_FILTER_STRING, dateFiltersString);
        options.put(FILTER_EXTENDED, filtersExtended);
    }

    private String parseCustomFilters() {
        StringBuilder sb = new StringBuilder();
        for (ReportCustomFilter rcf : reportDefinition.getReportCustomFilter()) {
            Object value = rcf.getValueByLabel(values);
            if (value != null) {
                if (isDateType(rcf.getDataTypeId()) && Utilities.isNumber(value.toString())) {
                    sb.append(String.format(EXTENDED_FILTER_TEMPLATE, rcf.getPropertyName(), rcf.getOperator(),
                            reportDefinition.getDateFormatAndTimeZone().getISODateTimeFormatWithoutTimeZone(Long.valueOf(value.toString()))));
                } else {
                    sb.append(String.format(EXTENDED_FILTER_TEMPLATE, rcf.getPropertyName(), rcf.getOperator(), value));
                }

            } else {
                sb.append(String.format(EXTENDED_FILTER_TEMPLATE, rcf.getPropertyName(), rcf.getOperator(), rcf.getValueParsed()));
            }
        }
        return sb.toString();
    }

    private String getDateFilterString() {
        String operator = "<=";
        if (startDate != null && endDate != null) {
            operator = "between";
        } else if (startDate != null) {
            operator = ">=";
        }

        String dateFilterString = String.format(DATE_FILTER_TEMPLATE,
                (startDate == null) ? null : dateFormatAndTimeZone.getISODateTimeFormatWithoutTimeZone(startDate),
                (endDate == null) ? null : dateFormatAndTimeZone.getISODateTimeFormatWithoutTimeZone(endDate),
                (now == null) ? null : dateFormatAndTimeZone.getISODateTimeFormatWithoutTimeZone(now), operator);
        return dateFilterString;
    }
}
