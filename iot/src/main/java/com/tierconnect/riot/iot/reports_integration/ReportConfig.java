package com.tierconnect.riot.iot.reports_integration;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.api.database.base.FactoryDataBase;
import com.tierconnect.riot.api.database.base.GenericOperator;
import com.tierconnect.riot.api.database.base.Operation;
import com.tierconnect.riot.api.database.base.alias.Alias;
import com.tierconnect.riot.api.database.base.conditions.BooleanCondition;
import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.base.operator.MultipleOperator;
import com.tierconnect.riot.api.database.base.ordination.Order;
import com.tierconnect.riot.api.database.exception.OperationNotSupportedException;
import com.tierconnect.riot.api.database.exception.ValueNotPermittedException;
import com.tierconnect.riot.api.database.mongo.Mongo;
import com.tierconnect.riot.api.mongoShell.utils.CharacterUtils;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.iot.utils.VisibilityThingUtils;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.*;

import static com.tierconnect.riot.api.database.base.Operation.*;
import static com.tierconnect.riot.appcore.utils.Utilities.isEmptyOrNull;
import static com.tierconnect.riot.appcore.utils.Utilities.isNumber;
import static com.tierconnect.riot.commons.Constants.*;
import static com.tierconnect.riot.iot.entities.ReportDefinitionUtils.*;
import static com.tierconnect.riot.iot.utils.ReportExecutionUtils.DWELL_TIME_PROPERTY_LABEL;
import static java.lang.String.join;
import static java.lang.String.valueOf;

/**
 * This is the central class for configuration of all reports
 *
 * @author Julio Rocha
 * @author Vladimir alaro
 * @author Ruth Chirinos
 * @author Yazi Barriga
 */
public abstract class ReportConfig {

    private static Logger logger = Logger.getLogger(ReportConfig.class);
    protected static final List<String> ignoreFilters = Arrays.asList(GROUP_PROPERTY_ID, RELATIVE_DATE, START_DATE, END_DATE);
    protected static final List<String> ignoreEmptyOperators = Arrays.asList(OP_IS_EMPTY, OP_IS_NOT_EMPTY);
    protected static final Map<String, String> mapNonUDFs = new HashMap<>(10);
    protected List<PropertyReport> propertyReportList;
    protected DateFormatAndTimeZone dateFormatAndTimeZone;
    private Map<String, Date> timeStamps;

    static {
        mapNonUDFs.put(ID, _ID);
        mapNonUDFs.put(NAME, NAME);
        mapNonUDFs.put(LABEL_NAME, NAME);
        mapNonUDFs.put(GROUP_PROPERTY_ID, GROUP_TRANSLATE_ID);
        mapNonUDFs.put(SERIAL, SERIAL_NUMBER);
        mapNonUDFs.put(GROUP_PROPERTY_NAME, GROUP_TRANSLATE_NAME);
        mapNonUDFs.put(THING_TYPE_PROPERTY_ID, THING_TYPE_TRANSLATE_ID);
        mapNonUDFs.put(THING_TYPE_PROPERTY_NAME, THING_TYPE_TRANSLATE_NAME);
    }

    // utilities
    protected Map<String, String> paths;
    private Map<String, Zone> mapZone;
    private List<Zone> zoneList;
    private Map<Long, Shift> mapShift;
    private Map<Long, Group> mapGroup;
    private Map<Long, LogicalReader> mapLogicalReader;
    private Long dataTypeId = null;
    protected boolean skipDateFilters = false;


    private String collectionTarget;
    private boolean historicalReport = false; //Is the report historycal?
    protected boolean addNonUdfInProperties = false;
    ZoneTranslator zoneTranslator;
    public final Map<String, String> ZONE_TRANSLATOR;
    private String typeReport;

    //rules
    private List<IRule> displayRules;

    //Report definition
    protected ReportDefinition reportDefinition;

    //previously was values
    protected Map<String, Object> dynamicFilters;

    //Uses some thingType
    protected ThingType thingType;

    //pagination
    protected Integer pageNum;
    protected Integer pageSize;

    //interval for historical data
    protected Date endDate;
    protected Date startDate;

    // control pagination
    private Integer skip;
    private Integer limit;

    //objects for new API mongo
    private ConditionBuilder filters = new ConditionBuilder();
    private List<String> filtersDefinition = new LinkedList<>();

    private List<Alias> projection = new LinkedList<>();
    private Map<String, Order> sort;

    //Requirement for Mobile, send Thing Type UDF as an Object
    private boolean thingTypeUdfAsObject = false;

    public ReportConfig(ReportDefinition reportDefinition,
                        Map<String, Object> dynamicFilters,
                        Integer pageNum,
                        Integer pageSize,
                        Date startDate,
                        Date endDate,
                        boolean addNonUdfInProperties,
                        String typeReport,
                        boolean thingTypeUdfAsObject) {
        this.reportDefinition = reportDefinition;
        this.dynamicFilters = dynamicFilters;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.startDate = startDate;
        this.endDate = endDate;
        this.addNonUdfInProperties = addNonUdfInProperties;
        this.zoneTranslator = new ZoneTranslator(reportDefinition);
        this.ZONE_TRANSLATOR = zoneTranslator.getTranslatorMap();
        this.historicalReport = endDate != null || startDate != null;
        this.collectionTarget = isHistoricalReport() ? COLLECTION_THINGSNAPSHOT : COLLECTION_THINGS;
        this.typeReport = typeReport;
        this.thingTypeUdfAsObject = thingTypeUdfAsObject;
    }

    public ReportConfig(ReportDefinition reportDefinition,
                        Map<String, Object> dynamicFilters,
                        Integer pageNum,
                        Integer pageSize,
                        Date startDate,
                        Date endDate,
                        boolean addNonUdfInProperties,
                        String typeReport, boolean skipDateFilters,
                        boolean thingTypeUdfAsObject) {
        this(reportDefinition, dynamicFilters, pageNum, pageSize, startDate, endDate, addNonUdfInProperties,
                typeReport, thingTypeUdfAsObject);
        this.skipDateFilters = skipDateFilters;
    }

    public ReportConfig(ReportDefinition reportDefinition,
                        Map<String, Object> dynamicFilters,
                        Integer pageNum,
                        Integer pageSize,
                        Date startDate,
                        Date endDate) {
        this(reportDefinition, dynamicFilters, pageNum, pageSize, startDate, endDate, false, reportDefinition.getReportType(), false, false);
    }

    /**
     * Method that initiates the report configuration, is used by table type reports
     * <ul>
     * <li>Table Detail</li>
     * <li>Table History</li>
     * <li>Table Script</li>
     * </ul>
     *
     * @param now current date for the configuration
     */
    protected void processConfiguration(Date now) {
        try {
            // call zone in report
            loadZones();
            // clear filters
            filters.clear();
            // pagination
            initPagination();
            //simple filters
            processFilters(now, dynamicFilters);
            //date filters... start end combined in one filter
            parseDateFilters();
            // add projection
            defaultProjections();
            // add properties
            properties();
        } catch (ValueNotPermittedException e) {
            logger.error("Value not Permitted in Operations", e);
        }
    }

    /**
     * Method that initiates the report configuration, is used by table type reports
     * <ul>
     * <li>Table Summary</li>
     * </ul>
     *
     * @param now Current date for the configuration
     */
    protected void processConfigurationSummary(Date now) {
        // call zone in report
        loadZones();
        // clear filters
        filters.clear();
        // simple filters
        processFilters(now, dynamicFilters);
        //date filters... start end combined in one filter
        parseDateFilters();
    }

    /**
     * initialize pagination.
     */
    private void initPagination() {
        limit = 10000;
        skip = 0;
        if (StringUtils.equals(reportDefinition.getReportType(), "map") ||
                StringUtils.equals(reportDefinition.getReportType(), "table")) {

            User user = (User) SecurityUtils.getSubject().getPrincipal();
            try {
                if (StringUtils.equals(reportDefinition.getReportType(), "map")) {
                    limit = Integer.parseInt(ConfigurationService.getAsString(user, "mapReportLimit"));
                } else if (StringUtils.equals(reportDefinition.getReportType(), "table")) {
                    limit = Integer.parseInt(ConfigurationService.getAsString(user, "maxReportRecords"));
                }
            } catch (Exception e) {
                logger.error("Value" + limit, e);
            }
        }
        if (isPaginationSet()) {
            skip = (pageNum >= 1) ? (pageNum - 1) * pageSize : 0;
            logger.info("page " + pageNum + ", skip " + skip + ", size " + pageSize);
            if (pageSize <= limit) {
                limit = pageSize;
            }
        } else if (pageSize == null) {
            logger.warn("limit response to " + limit + ". page size = null");
        }
    }

    /**
     * Processes filters for all type of reports
     *
     * @param now                Current date for the configuration
     * @param dynamicFiltersBody Object that contains the values for the execution of the filters
     */
    protected void processFilters(Date now, Map<String, Object> dynamicFiltersBody) {
        Long groupId = null;
        for (ReportFilter reportFilter : reportDefinition.getReportFilterOrderByDisplayOrder()) {
            Object value = reportFilter.getValueByLabel(dynamicFiltersBody);
            if (value != null) {
                if (THING_TYPE_PROPERTY_ID.equals(reportFilter.getPropertyName())) {
                    thingType = null;
                    if (!isEmptyOrNull((String) value) && StringUtils.isNumeric((String) value)) {
                        thingType = ThingTypeService.getInstance().get(Long.parseLong((String) value));
                    }

                    if (OP_DOUBLE_EQUALS.equals(reportFilter.getOperator())) {
                        //check the the thing does not have parents
                        filters.addOperator(Operation.emptyArray(verifiedSnapshotsProperty(CHILDREN)));
                        filters.addOperator(Operation.notExists(verifiedSnapshotsProperty(PARENT)));
                        filtersDefinition.add(reportFilter.getDefinition());
                    }

                    if (thingType != null
                            && !Constants.REPORT_TYPE_TABLE_SUMMARY.equals(reportDefinition.getReportType())) {
                        addFilter(reportFilter.getLabel(),
                                THING_TYPE_TRANSLATE_ID,
                                reportFilter.getOperator(),
                                reportFilter.getPropertyName(),
                                translateValue(reportFilter, isHistoricalReport(), value, now),
                                "",
                                isHistoricalReport());
                        filtersDefinition.add(reportFilter.getDefinition());
                        logger.debug("Thing Type " + thingType.getId());
                    }
                } else if (GROUP_PROPERTY_ID.equals(reportFilter.getPropertyName())
                        && !isEmptyOrNull((String) value)
                        && StringUtils.isNumeric((String) value)) {
                    groupId = Long.valueOf(value.toString());
                } else if (!ignoreFilters.contains(reportFilter.getPropertyName())) {
                    //this case for Empty values
                    if (isEmptyOrNull(value.toString()) && !ignoreEmptyOperators.contains(reportFilter.getOperator())) {
                        continue;
                    }
                    //this case is just for zone
                    filtersDefinition.add(reportFilter.getDefinition());
                    addFilersNotIgnored(reportFilter, value, now);
                }
            }
        }
        getGroupVisibilityFilter(groupId);
    }

    /**
     * Add the filter to conditional filter.
     *
     * @param reportFilter A {@link ReportFilter} obtained from the database.
     * @param value        A {@link Object} instance containing the filter value.
     * @param now          A {@link Date} instance containing the now date.
     */
    public void addFilersNotIgnored(ReportFilter reportFilter, Object value, Date now) {
        addFilterOperator(reportFilter.getLabel(), createFilterNotIgnored(reportFilter, value, now));
    }

    public GenericOperator createFilterNotIgnored(ReportFilter reportFilter, Object value, Date now) {
        //this case is just for zone
        boolean projection = false;
        if (reportFilter.getOperator().equals(OP_CONTAINS)
                && !ZoneTranslator.isZoneProperty(reportFilter.getPropertyName())) {
            projection = true;
        }
        String translateProperties = translate(reportFilter, projection);
        return createFilter(reportFilter.getLabel(),
                translateProperties,
                reportFilter.getOperator(),
                reportFilter.getPropertyName(),
                translateValue(reportFilter, isHistoricalReport(), value, now),
                StringUtils.EMPTY,
                isHistoricalReport());
    }

    /**
     * Adds default projection for all things
     * <ul>
     * <li>name</li>
     * <li>serialNumber</li>
     * <li>groupName</li>
     * <li>thingTypeId</li>
     * <li>thingTypeName</li>
     * <li>groupTypeName</li>
     * </ul>
     */
    private void defaultProjections() {
        // add non UDF default
        addProjection(NAME);
        addProjection(SERIAL_NUMBER);
        addProjection(GROUP_TRANSLATE_NAME);
        addProjection(THING_TYPE_TRANSLATE_ID);
        addProjection(THING_TYPE_TRANSLATE_NAME);
        addProjection(GROUP_TYPE_TRANSLATE_NAME);
    }

    public abstract Map<String, Object> exportResult(Long total, List<Map<String, Object>> records);

    /**
     * Translate the property of the {@link ReportConfig} object
     *
     * @param reportProperty     Object containing the property to translate
     * @param isProjection       It indicates whether the translation will get the value of the property
     * @param isHistoricalReport It indicates whether the property is for a historical report
     * @return Translated property
     */
    protected String translate(ReportProperty reportProperty, boolean isProjection, boolean isHistoricalReport) {
        return translateKey(reportProperty, isProjection, isHistoricalReport);
    }

    /**
     * Translate the property of the {@link ReportProperty} object
     *
     * @param reportProperty Object containing the property to translate
     * @param isProjection   It indicates whether the translation will get the value of the property
     * @return Translated property
     */
    protected String translate(ReportProperty reportProperty, boolean isProjection) {
        return translateKey(reportProperty, isProjection, isHistoricalReport());
    }

    /**
     * Translate the property of the {@link ReportFilter} object
     *
     * @param reportFilter Object containing the property to translate
     * @param isProjection It indicates whether the translation will get the value of the property
     * @return Translated property
     */
    protected String translate(ReportFilter reportFilter, boolean isProjection) {
        return translateKey(reportFilter, isProjection, isHistoricalReport());
    }

    /**
     * Translate the property of the {@link ReportRule} object
     *
     * @param reportRule   Object containing the property to translate
     * @param isProjection It indicates whether the translation will get the value of the property
     * @return Translated property
     */
    protected String translate(ReportRule reportRule, boolean isProjection) {
        return translateKey(reportRule, isProjection, isHistoricalReport());
    }

    /**
     * Translate the property of the {@link ReportGroupBy} object
     *
     * @param groupBy      Object containing the property to translate
     * @param isProjection It indicates whether the translation will get the value of the property
     * @return Translated property
     */
    protected String translate(ReportGroupBy groupBy, boolean isProjection) {
        return translateKey(groupBy, isProjection, isHistoricalReport());
    }

    /**
     * Translate property
     *
     * @param propertyName    Value of property
     * @param targetThingType Adds the full path to the target thing type
     * @return Translated property
     */
    protected String translateString(String propertyName, ThingType targetThingType) {
        String out;
        if ((propertyName != null) && isTimestamp(propertyName)) {
            out = propertyName.substring(propertyName.indexOf("(") + 1, propertyName.length() - 1) + ".time";
        } else {
            out = mapNonUDFs.get(propertyName);
        }
        if (out == null) {
            out = propertyName + ".value";
        }
        return translateWithThingType(out, targetThingType);
    }

    /**
     * Translate property
     *
     * @param object             Object containing the property to translate
     * @param isProjection       It indicates whether the translation will get the value of the property
     * @param isHistoricalReport It indicates whether the property is for a historical report
     * @return Translated property
     */
    private String translateKey(Object object, boolean isProjection, boolean isHistoricalReport) {
        ThingType targetThingType = null;
        String out = null;
        try {
            String propertyName = StringUtils.substringBefore(BeanUtils.getProperty(object, "propertyName"), ",");
            propertyName = Utilities.removeSpaces(propertyName);
            if (isProjection &&
                    (ZoneTranslator.isZoneProperty(propertyName) || ZoneTranslator.isZoneTypeId(propertyName))) {
                propertyName += "Display";
            }
            Long dataTypeId = null;
            if (object instanceof ReportProperty && ((ReportProperty) object).getThingTypeField() != null
                    && !isDwell(propertyName)
                    && !isTimestamp(propertyName)) {
                dataTypeId = ((ReportProperty) object).getThingTypeField().getDataType().getId();
            } else if (object instanceof ReportFilter) {
                targetThingType = ((ReportFilter) object).getThingType();
                if (((ReportFilter) object).getThingTypeField() != null && !ZoneTranslator.isZoneProperty(propertyName)
                        && !isDwell(propertyName)
                        && !isTimestamp(propertyName)) {
                    dataTypeId = ((ReportFilter) object).getThingTypeField().getDataType().getId();
                }
            } else if (object instanceof ReportGroupBy && ((ReportGroupBy) object).getThingTypeField() != null) {
                dataTypeId = ((ReportGroupBy) object).getThingTypeField().getDataType().getId();
            } else if (object instanceof ReportRule && ((ReportRule) object).getThingTypeField() != null
                    && !isDwell(propertyName)
                    && !isTimestamp(propertyName)) {
                dataTypeId = ((ReportRule) object).getThingTypeField().getDataType().getId();
            }

            //we have to validate that the propertyName is not a zoneProperty.id
            if (!ZoneTranslator.isZoneProperty(propertyName) && dataTypeId != null) {
                if (dataTypeId.equals(ThingTypeField.Type.TYPE_SHIFT.value)) {
                    out = propertyName + (object instanceof ReportFilter ? ".value.id" : ".value.name");
                } else if (dataTypeId.equals(ThingTypeField.Type.TYPE_GROUP.value)
                        || dataTypeId.equals(ThingTypeField.Type.TYPE_LOGICAL_READER.value)
                        || dataTypeId.equals(ThingTypeField.Type.TYPE_THING_TYPE.value)) {
                    out = propertyName + ".value.name";
                } else if (dataTypeId.equals(ThingTypeField.Type.TYPE_ZONE.value)) {
                    //Zone translation
                    out = zoneTranslation(propertyName, isProjection, isHistoricalReport);
                }
            }
            //Zone translation if dataTypeId or out is null
            if ((out == null && dataTypeId == null)
                    || ZoneTranslator.isZoneProperty(propertyName) || ZoneTranslator.isZoneTypeId(propertyName)) {
                if (ZONE_TRANSLATOR.containsKey(propertyName)) {
                    out = zoneTranslation(propertyName, isProjection, isHistoricalReport);
                } else {
                    String inferedZone = zoneTranslator.inferZoneDwellFunction(propertyName);
                    if (ZONE_TRANSLATOR.containsKey(inferedZone)) {
                        out = zoneTranslation(inferedZone, isProjection, isHistoricalReport);
                    }
                }
            }

            if ((propertyName != null) && isTimestamp(propertyName) && (out == null)) {
                out = propertyName.substring(propertyName.indexOf("(") + 1, propertyName.length() - 1) + ".time";
            }

            if (out == null) {
                out = propertyName;
                if ((boolean) PropertyUtils.getProperty(object, "native")) {
                    out = mapNonUDFs.get(propertyName);
                } else if ((boolean) PropertyUtils.getProperty(object, "dwell")) {
                    out = stripDwell(propertyName) + (isHistoricalReport ? ".dwellTime" : ".time");
                } else if (!out.endsWith(".time")) {
                    out = propertyName + (".value"); //add ".value"
                }
            }
            out = translateWithThingType(out, targetThingType);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            logger.warn("No property name available on " + object, e);
        }

        return out;
    }

    protected String translateWithThingType(String out, ThingType targetThingType) {
        if (thingType != null && targetThingType != null) {
            ThingTypePath pathByThingTypes = ThingTypePathService.getInstance().getPathByThingTypes(thingType, targetThingType);
            if (pathByThingTypes != null) {
                out = pathByThingTypes.getPath() + "." + out;
            }
        }
        return out;
    }

    private String zoneTranslation(String propertyName, boolean isProjection, boolean isHistoricalReport) {
        String out;
        if (propertyName.startsWith(DWELL_TIME_PROPERTY_LABEL)) {
            out = getDwellTimeProperty(propertyName, isHistoricalReport);
        } else {
            out = ZONE_TRANSLATOR.get(propertyName);
        }
        if (isProjection && ZONE_TRANSLATOR.containsKey(out) &&
                !propertyName.contains("Property.id") &&
                !propertyName.contains("Type.name")) {
            out = ZONE_TRANSLATOR.get(out);
        }
        return out;
    }

    private String getDwellTimeProperty(String propertyName, boolean isHistoricalReport) {
        propertyName = stripDwell(propertyName);
        propertyName = (isHistoricalReport) ? (propertyName + "DTH") : (propertyName + "DT");
        return ZONE_TRANSLATOR.get(propertyName);
    }

    protected String reverseTranslate(String in) {
        String result = StringUtils.substringBefore(in, CharacterUtils.COMMA);
        if (isDwell(result)) {
            result = stripDwell(result);
        }
        return result;
    }

    protected Object translateValue(ReportFilter reportFilter, boolean historicalReport, Object value, Date now) {
        Object newValue = value;
        long type = reportFilter.getFieldType() == null ? 0L : reportFilter.getFieldType();
        DataType dataType = null;
        if (!ZoneTranslator.isZoneProperty(reportFilter.getPropertyName())) {
            if (reportFilter.getThingTypeField() != null) {
                dataType = reportFilter.getThingTypeField().getDataType();
                if (dataType != null) {
                    this.dataTypeId = dataType.getId();
                }
            }
        }
        if ((dataType != null) && dataType.getId().compareTo(ThingTypeField.Type.TYPE_SHIFT.value) == 0
                && !isEmptyOrNull((String) value)
                && StringUtils.isNumeric((String) value)
                && !isDwell(reportFilter.getPropertyName())
                && !isTimestamp(reportFilter.getPropertyName())) {
            newValue = Long.valueOf(valueOf(value));
        } else if (THING_TYPE_PROPERTY_ID.equals(reportFilter.getPropertyName())) {
            newValue = Long.valueOf(valueOf(value));
        } else if ((dataType != null) && (dataType.getId().compareTo(ThingTypeField.Type.TYPE_ZONE.value) == 0)
                && !reportFilter.getOperator().equals(OP_CONTAINS)
                && zoneTranslator.isZoneUDFName(reportFilter.getPropertyName())
                && !isEmptyOrNull((String) value) && StringUtils.isNumeric((String) value)) {
            newValue = Long.valueOf(valueOf(value));
        } else if (isTimestamp(reportFilter.getPropertyName())) {
            newValue = (Utilities.isNumber(value.toString()) ? new Date(Long.parseLong(value.toString())) : value.toString());
        } else if (isDwell(reportFilter.getPropertyName())) {
            if (historicalReport) {
                newValue = Long.parseLong(value.toString());
            } else {
                newValue = new Date(now.getTime() - Long.parseLong(value.toString()));
            }
        } else if (ZoneTranslator.isZoneProperty(reportFilter.getPropertyName()) ||
                ZoneTranslator.isZoneType(reportFilter.getPropertyName())) {
            if (reportFilter.getThingTypeField() != null) {
                newValue = getListZoneIdByZonePropertyOrZoneType(reportFilter.getThingTypeField().getId(), reportFilter.getOperator(), value.toString());
            } else {
                logger.warn("Not found zone properties filter in report[" + reportDefinition.getName() + "]");
                throw new UserException("Not found zone properties filter in report [" + reportDefinition.getName() + "]");
            }
        } else if (ZoneTranslator.isZoneTypeId(reportFilter.getPropertyName())
                && StringUtils.isNumeric((String) value)) {
            BooleanBuilder be = new BooleanBuilder();
            be = be.and(QZone.zone.zoneType.id.eq(Long.parseLong(valueOf(value.toString()))));
            List<Zone> zones = ZoneService.getInstance().listPaginated(be, null, null);
            List<Long> zoneIds = new ArrayList<>();
            for (Zone zone : zones) {
                zoneIds.add(zone.getId());
            }
            newValue = zoneIds;
        } else if ((dataType != null) && dataType.getId().equals(ThingTypeField.Type.TYPE_SEQUENCE.value)
                && !isEmptyOrNull((String) value)
                && !reportFilter.getOperator().equals(OP_CONTAINS)) {//operator contains is not supported by numeric types
            newValue = new BigDecimal((String) value);
        } else if (dataType != null && dataType.getId().equals(ThingTypeField.Type.TYPE_FORMULA.value)) {
            newValue = getListOfFormulaOptions(value, reportFilter.getOperator());
        } else if (type == ThingTypeField.Type.TYPE_TIMESTAMP.value
                || type == ThingTypeField.Type.TYPE_NUMBER.value
                || type == ThingTypeField.Type.TYPE_BOOLEAN.value
                || type == ThingTypeField.Type.TYPE_DATE.value) {
            newValue = DataTypeService.getValueDataType(reportFilter.getThingTypeField().getDataType(), value, null, null);
        }
        return newValue;
    }

    private Object getListOfFormulaOptions(Object value, String operation) {
        Object newValue = value;
        if (OP_EQUALS.equals(operation) || OP_NOT_EQUALS.equals(operation)) {
            if (Utilities.isValidBoolean(value)) {
                newValue = Arrays.asList(value.toString(), Boolean.valueOf(value.toString()));
            } else if (Utilities.isNumber(value)) {
                newValue = Arrays.asList(value.toString(), new BigDecimal(value.toString()));
            }
        }
        return newValue;
    }

    protected List<Long> getListZoneIdByZonePropertyOrZoneType(Long zonePropertyId, String operator, Object value) {
        ZonePropertyValueService service = ZonePropertyValueService.getInstance();
        List<ZonePropertyValue> zpv = service.getZonePropertyByValue(getGroupFilterValue(),
                zonePropertyId, operator, value.toString());
        List<Long> zoneIds = new ArrayList<>();
        for (ZonePropertyValue zonePropertyValue : zpv) {
            zoneIds.add(zonePropertyValue.getZoneId());
        }
        return zoneIds;
    }

    protected String verifiedSnapshotsProperty(String value) {
        return isHistoricalReport() ? "value." + value : value;
    }

    protected void properties() {
        paths = new HashMap<>();
        propertyReportList = new ArrayList<>();

        for (ReportProperty property : reportDefinition.getReportPropertyOrderByDisplayOrder()) {
            String translate = translate(property, true);
            PropertyReport propertyReport;
            if (property.getThingType() == null) {
                ThingTypeField thingTypeField = property.getThingTypeField();
                if (!isDwell(property.getPropertyName()) && !isTimestamp(property.getPropertyName())
                        && thingTypeField == null) {
                    List<ThingTypeField> fieldsByName = ThingTypeFieldService.getInstance().getThingTypeFieldByName(property.getPropertyName());
                    if (!fieldsByName.isEmpty()) {
                        thingTypeField = fieldsByName.get(0);
                    }
                }
                propertyReport = new PropertyReport(
                        translate,
                        property.getPropertyName(),
                        property.getLabel(), property.isNative(), thingTypeField, isHistoricalReport());
                addProjection(translate, property.getLabel(), ReportJSFunction.getJSFunction(propertyReport));
            } else {
                propertyReport = new PropertyReport(
                        translate,
                        property.getPropertyName(),
                        property.getLabel(), property.isNative(),
                        property.getThingType(), property.getThingTypeField(), isHistoricalReport());
                Map<String, String> mapPathsByThingType = ThingTypePathService.getInstance().getMapPathsByThingType(property.getThingType());
                addProjection(mapPathsByThingType.values(), translate, property.getLabel(), ReportJSFunction.getJSFunction(propertyReport));
                if (property.getThingTypeField() != null && property.getThingTypeField().getDataType().getId().compareTo(ThingTypeField.Type.TYPE_THING_TYPE.value) == 0 &&
                        (this.thingTypeUdfAsObject)) {
                    addProjection(mapPathsByThingType.values(), property.getPropertyName() + Constants.PATH_TTUDF_SERIAL_NUMBER,
                            property.getLabel(), ReportJSFunction.getJSFunction(propertyReport));
                }
                addProjection(mapPathsByThingType.values(), translate, property.getLabel(), ReportJSFunction.getJSFunction(propertyReport));
                paths.putAll(mapPathsByThingType);
            }
            if (isHistoricalReport() && !propertyReport.isPropertyNative()) {
                String dwellTimeChangedProperty = null;
                String dwellTimeBlinkedProperty = null;
                String propertyName = null;
                Long subDataTypeZone = 0L;
                if (!isDwell(property.getPropertyName()) && !isTimestamp(property.getPropertyName())) {
                    propertyName = property.getPropertyName();
                    subDataTypeZone = propertyReport.getSubDataTypeZone();
                } else {
                    propertyName = property.getThingTypeField().getName();
                    subDataTypeZone = propertyReport.getSubDataTypeZone();
                }
                if (propertyReport.getSubDataTypeZone() == 0L) {
                    dwellTimeChangedProperty = propertyName + Constants.LAST_KNOWN_CHANGED;
                    dwellTimeBlinkedProperty = propertyName + Constants.LAST_KNOWN_BLINKED;
                } else {
                    //this is a special case for Zone (Name) which  translation is equal to Zone (ZoneType)
                    dwellTimeChangedProperty = getDwellTimePropertyZone(subDataTypeZone, translate, Constants.DWELLTIME_CHANGED);
                    dwellTimeBlinkedProperty = getDwellTimePropertyZone(subDataTypeZone, translate, Constants.DWELLTIME_BLINKED);
                }
                if (dwellTimeChangedProperty != null) {
                    addProjection(dwellTimeChangedProperty);
                    propertyReport.setPropertyChanged(dwellTimeChangedProperty);
                }
                if (dwellTimeBlinkedProperty != null) {
                    addProjection(dwellTimeBlinkedProperty);
                    propertyReport.setPropertyBlinked(dwellTimeBlinkedProperty);
                }
            }
            propertyReport.setDateFormatAndTimeZone(dateFormatAndTimeZone);
            propertyReportList.add(propertyReport);
        }
    }

    protected String getDwellTimePropertyZone(Long subDataTypeZone, String translate, String typeBlinked) {
        if (subDataTypeZone == 96L) {
            return ZONE_TRANSLATOR.get(translate + "ZN");
        } else {
            return zoneTranslation(translate + typeBlinked, false, isHistoricalReport());
        }
    }

    protected void addSortTo(String property, Order order) {
        if (sort == null) {
            sort = new LinkedHashMap<>(1);
        }
        sort.put(property, order);
    }

    protected Object getRuleValue(ReportRule reportRule) {
        Object ruleValue = reportRule.getValue();
        if (ruleValue != null) {
            if ((isDwell(reportRule.getPropertyName()) || isTimestamp(reportRule.getPropertyName()))
                    && isNumber(ruleValue.toString())) {
                ruleValue = new BigDecimal(ruleValue.toString());
            } else if (reportRule.getThingTypeField() != null) {
                if (reportRule.getThingTypeField().isNativeObject()) {
                    ruleValue = getValueNativeObject(reportRule.getThingTypeField().getDataType(), ruleValue);
                } else if (isNumber(ruleValue.toString())
                        && (Objects.equals(ThingTypeField.Type.TYPE_TIMESTAMP.value, reportRule.getThingTypeField().getDataType().getId())
                        || Objects.equals(ThingTypeField.Type.TYPE_DATE.value, reportRule.getThingTypeField().getDataType().getId())
                        || Objects.equals(ThingTypeField.Type.TYPE_NUMBER.value, reportRule.getThingTypeField().getDataType().getId()))) {
                    ruleValue = new BigDecimal(ruleValue.toString());
                }
            }
        }
        return ruleValue;
    }

    /**
     * Get Group Visibility : List of children groups and list of thing Types
     *
     * @param reportGroupId Report Group by default
     */
    private void getGroupVisibilityFilter(Long reportGroupId) throws ValueNotPermittedException {
        Map<Long, List<Long>> groups = VisibilityThingUtils.calculateVisibilityThingsID(reportGroupId);
        if ((groups == null) || groups.isEmpty()) {
            throw new UserException("User does not have permissions to see any Group, check User's roles");
        }
        List<Long> groupIds = new ArrayList<>();
        List<Long> thingTypeIds = new ArrayList<>();
        for (Long groupId : groups.keySet()) {
            groupIds.addAll(GroupService.getInstance().getListGroupIdsChildren(groupId));
            thingTypeIds.addAll(getThingTypes(groups.get(groupId)));
        }
        String groupId = isHistoricalReport() ? GROUP_TS_TRANSLATE : GROUP_TRANSLATE_ID;
        String thingTypeId = isHistoricalReport() ? THING_TS_TYPE_TRANSLATE : THING_TYPE_TRANSLATE_ID;
        addInValues(groupIds, groupId);
        addInValues(thingTypeIds, thingTypeId);
    }

    private void addInValues(List<Long> ids, String id) {
        if (!ids.isEmpty()) {
            GenericOperator idOperator;
            if (ids.size() > 1) {
                idOperator = Operation.in(id, ids);
                filters.addOperator(idOperator);
            } else {
                idOperator = Operation.equals(id, ids.get(0));
                filters.addOperator(idOperator);
            }
        }
    }

    protected void parseDateFilters() throws ValueNotPermittedException {
        if (skipDateFilters) return;
        if (isHistoricalReport()) {
            if (REPORT_TYPE_TABLE_DETAIL.equals(typeReport)
                    || REPORT_TYPE_MAP.equals(typeReport)
                    || REPORT_TYPE_TABLE_SUMMARY.equals(typeReport)) {
                if (reportDefinition.containsGroupByPartition()) {//byPartition ie unique
                    if (startDate != null) {
                        addFilters(Operation.between(TIME, startDate, endDate));
                    } else {
                        addFilters(Operation.lessThanOrEquals(TIME, endDate));
                    }
                } else {//non-byPartition & non-unit
                    addFilters(getInQuery(startDate, endDate));
                }
            } else {
                addFilterDates();
            }
        }
    }

    public void addFilterDates() {
        if (startDate != null && endDate != null) {
            addFilters(between(TIME, startDate, endDate));
        } else if (startDate != null) {
            addFilters(greaterThanOrEquals(TIME, startDate));
        } else if (endDate != null) {
            addFilters(lessThanOrEquals(TIME, endDate));
        } else {
            addFilters(lessThanOrEquals(TIME, new Date()));
        }
    }

    protected GenericOperator getInQuery(Date startDate, Date endDate) {
        ConditionBuilder builderThingSnapshotIds = new ConditionBuilder();
        if (endDate != null && startDate != null) {
            builderThingSnapshotIds.addOperator(getElementMatch(startDate, endDate));
        } else if (endDate != null) {
            builderThingSnapshotIds.addOperator(getElementMatch(null, endDate));
        } else if (startDate != null) {
            //make now the end date
            builderThingSnapshotIds.addOperator(getElementMatch(startDate, new Date()));
        }
        return Operation.inSubquery(_ID, builderThingSnapshotIds, COLLECTION_THINGSNAPSHOT_IDS, "blinks[0].blink_id", builderThingSnapshotIds);
    }

    private GenericOperator getElementMatch(Date startDate, Date endDate) {
        if (startDate != null) {
            return Operation.elementMatch("blinks", Operation.between(TIME, startDate.getTime(), endDate.getTime()));
        }
        return Operation.elementMatch("blinks", Operation.lessThan(TIME, endDate.getTime()));
    }

    protected void addFilter(String label,
                             String property,
                             String operator,
                             String original,
                             Object value,
                             String childProperty,
                             boolean timeSeries) throws ValueNotPermittedException {
        addFilterOperator(label, createFilter(label, property, operator, original, value, childProperty, timeSeries));
    }

    protected GenericOperator createFilter(String label,
                                           String property,
                                           String operator,
                                           String original,
                                           Object value,
                                           String childProperty,
                                           boolean timeSeries) throws ValueNotPermittedException {

        GenericOperator operation;
        String[] properties = property.split(",");
        boolean isMultiple = properties.length > 1;
        if (!isMultiple) {
            property = timeSeries && StringUtils.isEmpty(childProperty) ? "value." + property : property;
        }
        // special case for dwellTime - we need to flip the operator value
        // because we are filtering by date rather than actual dwellTime
        // do this only for things collection, not thingSnapshots !
        if ((original != null) && original.startsWith(DWELLTIME) && !timeSeries) {
            if (operator.contains(OP_GREATHER_THAN)) {
                operator = operator.replace(OP_GREATHER_THAN, OP_LESS_THAN);
            } else if (operator.contains(OP_LESS_THAN)) {
                operator = operator.replace(OP_LESS_THAN, OP_GREATHER_THAN);
            }
        }
        switch (operator) {
            case OP_CONTAINS:
                if (value instanceof Collection) {
                    if (isMultiple) {
                        operation = buildMultipleFieldCondition(timeSeries, properties, childProperty, value);
                    } else {
                        operation = Operation.in(property, value);
                    }
                } else {
                    String valueForRegex = replaceSpecialCharForRegex((String) value);
                    if (property.contains(SERIAL_NUMBER)) {
                        operation = Operation.regex(property, valueForRegex.toUpperCase(), REGEX_CASE_SENSITIVE);
                    } else {
                        operation = Operation.regex(property, valueForRegex, REGEX_CASE_INSENSITIVE);
                    }
                }
                break;
            case OP_LESS_THAN:
                if (value instanceof Collection) {
                    operation = Operation.in(property, value);
                } else {
                    operation = Operation.lessThan(property, value);
                }
                break;
            case OP_GREATHER_THAN:
                operation = Operation.greaterThan(property, value);
                break;
            case OP_NOT_EQUALS:
                if (value instanceof Collection) {
                    if (original != null && (ZoneTranslator.isZoneProperty(original) ||
                            ZoneTranslator.isZoneType(original))) {
                        if (isMultiple) {
                            operation = buildMultipleFieldCondition(timeSeries, properties, childProperty, value);
                        } else {
                            operation = Operation.in(property, value);
                        }
                    } else {
                        operation = Operation.notIn(property, value);
                    }
                } else {//VIZIX-928
                    operation = Operation.notEquals(property, value);
                }
                break;
            case OP_GREATHER_THAN_EQUALS:
                operation = Operation.greaterThanOrEquals(property, value);
                break;
            case OP_LESS_THAN_EQUALS:
                if (value instanceof Collection) {
                    operation = Operation.notIn(_ID, value);
                } else {
                    operation = Operation.lessThanOrEquals(property, value);
                }
                break;
            case OP_IS_EMPTY:
                operation = new MultipleOperator(BooleanCondition.OR);
                if (original != null && (value instanceof Collection)
                        && (ZoneTranslator.isFacilityMap(original) || ZoneTranslator.isZoneGroup(original))) {
                    ((MultipleOperator) operation).addOperatorList(
                            Operation.notExists(property),
                            Operation.in(property, value)
                    );
                } else if (original != null
                        && (value instanceof Collection)
                        && (ZoneTranslator.isZoneProperty(original) || ZoneTranslator.isZoneType(original))) {
                    if (isMultiple) {
                        operation = buildMultipleFieldCondition(timeSeries, properties, childProperty, value);
                    } else {
                        ((MultipleOperator) operation).addOperatorList(
                                Operation.in(property, value)
                        );
                    }
                } else {//VIZIX-928
                    ((MultipleOperator) operation).addOperatorList(
                            Operation.empty(property),
                            Operation.empty(removeForNativeObjects(property)),
                            Operation.isNull(property),
                            Operation.notExists(property)
                    );
                }
                break;
            case OP_IS_NOT_EMPTY: //VIZIX-928
                property = removeForNativeObjects(property);
                if (!(value instanceof Collection)) {
                    operation = new MultipleOperator(BooleanCondition.AND);
                    ((MultipleOperator) operation).addOperatorList(
                            Operation.notEmpty(property),
                            Operation.isNotNull(property)
                    );
                } else {
                    operation = Operation.in(property, value);
                }

                break;
            case OP_EQUALS:
            default:
                if (value instanceof Collection) {
                    if (isMultiple) {
                        operation = buildMultipleFieldCondition(timeSeries, properties, childProperty, value);
                    } else {
                        operation = Operation.in(property, value);
                    }
                } else {
                    if (property.contains(SERIAL_NUMBER)) {
                        String serialNumber = value.toString();
                        operation = Operation.equals(property, serialNumber.toUpperCase());
                    } else {
                        operation = Operation.equals(property, value);
                    }
                }
                break;
        }
        //filter on children properties
        if (StringUtils.isNotEmpty(childProperty)) {
            List<GenericOperator> operationList = new LinkedList<>();
            if (operation instanceof MultipleOperator) {
                operationList = ((MultipleOperator) operation).getGenericOperatorList();
            } else {
                operationList.add(operation);
            }
            operation = Operation.elementMatch(
                    isHistoricalReport() ? "value." + childProperty : childProperty,
                    operationList
            );
        }
        return operation;
    }

    /**
     * This function replace special regex characters in order to execute Regex operation over it
     *
     * @param value string to replace special characters
     * @return string with special characters replaced
     */
    private String replaceSpecialCharForRegex(String value) {
        String response = value;
        response = response.replace("\\", "\\\\u005c\\\\u005c");
        response = response.replace(".", "\\\\u005c\\\\u002e");
        response = response.replace("[", "\\\\u005c\\\\u005b");
        response = response.replace("]", "\\\\u005c\\\\u005d");
        response = response.replace("{", "\\\\u005c\\\\u007b");
        response = response.replace("}", "\\\\u005c\\\\u007d");
        response = response.replace("(", "\\\\u005c\\\\u0028");
        response = response.replace(")", "\\\\u005c\\\\u0029");
        response = response.replace("*", "\\\\u005c\\\\u002a");
        response = response.replace("+", "\\\\u005c\\\\u002b");
        response = response.replace("-", "\\\\u005c\\\\u002d");
        response = response.replace("?", "\\\\u005c\\\\u003f");
        response = response.replace("^", "\\\\u005c\\\\u005e");
        response = response.replace("$", "\\\\u005c\\\\u0024");
        response = response.replace("|", "\\\\u005c\\\\u007c");
        response = response.replace("\"", "\\\\u0022");
        return response;
    }

    protected void addFilterOperator(String label, GenericOperator operation) {
        if (operation != null) {
            logger.info("Filter=" + operation);
            filters.addOperator(operation);
        }
    }

    private MultipleOperator buildMultipleFieldCondition(boolean timeSeries, String[] properties,
                                                         String childProperty, Object value) {
        MultipleOperator operation = new MultipleOperator(BooleanCondition.OR);
        for (String prop : properties) {
            prop = timeSeries && StringUtils.isEmpty(childProperty) ? "value." + prop : prop;
            operation.addOperator(Operation.in(prop, value));
        }
        return operation;
    }

    /*************************************
     * Method  to get the Group id from report filters
     *************************************/
    public Group getGroupFilterValue() {
        Group group = null;
        if (reportDefinition.getReportFilter() != null) {
            for (ReportFilter reportFilter : reportDefinition.getReportFilter()) {
                if (reportFilter.getPropertyName().equals(GROUP_PROPERTY_ID) &&
                        reportFilter.getValue() != null && !reportFilter.getValue().isEmpty()
                        && StringUtils.isNumeric(reportFilter.getValue())) {
                    group = GroupService.getInstance().get(Long.parseLong(reportFilter.getValue()));
                    break;
                }
            }
        }
        return group;
    }

    /**
     * This method must be override by each child as needed
     *
     * @param types
     * @return
     */
    public List<Long> getThingTypes(List<Long> types) {
        if (Constants.REPORT_TYPE_TABLE_SUMMARY.equals(reportDefinition.getReportType())
                && reportDefinition.getReportGroupBy() != null && !reportDefinition.getReportGroupBy().isEmpty()) {
            List<Long> ids = new ArrayList<>();
            for (ReportGroupBy groupBy : reportDefinition.getReportGroupBy()) {
                if (thingType != null) {
                    ThingTypeField fieldByName = thingType.getThingTypeFieldByName(groupBy.getPropertyName());
                    if (fieldByName != null && groupBy.getThingType() != null
                            && types.contains(groupBy.getThingType().getId())
                            && !ids.contains(groupBy.getThingType().getId())) {
                        ids.add(groupBy.getThingType().getId());
                    }
                } else {
                    if (groupBy.getThingType() != null
                            && types.contains(groupBy.getThingType().getId())
                            && !ids.contains(groupBy.getThingType().getId())) {
                        ids.add(groupBy.getThingType().getId());
                    }
                }
            }
            if (thingType != null && !ids.contains(thingType.getId())) {
                ids.add(thingType.getId());
            }
            return ids;
        }
        return types;
    }

    public void addFilters(GenericOperator operation) {
        filters.addOperator(operation);
    }

    /**
     * Adds a projection column to the query
     *
     * @param projectionField
     */
    public void addProjection(final String projectionField) {
        addProjection(projectionField, projectionField, null);
    }

    public void addProjection(final String projectionField, String function) {
        addProjection(projectionField, projectionField, function);
    }

    public void addProjection(final String projectionField, final String projectionLabel, String function) {
        Alias alias = Alias.create(verifiedSnapshotsProperty(projectionField), projectionLabel, function);
        if (!projection.contains(alias)) {
            projection.add(alias);
        }
    }

    public void addProjection(Collection<String> listPaths, String projectionField, String projectionLabel, String function) {
        for (String value : listPaths) {
            addProjection(value + "." + projectionField, function);
        }
        addProjection(projectionField, projectionLabel, function);
    }

    public String getQuery() throws OperationNotSupportedException {
        Mongo database = FactoryDataBase.get(Mongo.class, filters);
        return database.getConditionBuilderString();
    }

    public String getValueNativeObject(DataType dataType, Object value) {
        String currentValue = (value == null ? null : value.toString());
        if (!isEmptyOrNull(currentValue) && StringUtils.isNumeric(currentValue)) {
            if (Objects.equals(ThingTypeField.Type.TYPE_SHIFT.value, dataType.getId())) {
                currentValue = getShiftById(Long.parseLong(currentValue));
            } else if (Objects.equals(ThingTypeField.Type.TYPE_LOGICAL_READER.value, dataType.getId())) {
                currentValue = getLogicalReaderNameById(Long.parseLong(currentValue));
            } else if (Objects.equals(ThingTypeField.Type.TYPE_GROUP.value, dataType.getId())) {
                currentValue = getGroupNameById(Long.parseLong(currentValue));
            } else if (Objects.equals(ThingTypeField.Type.TYPE_ZONE.value, dataType.getId())) {
                currentValue = getZoneNameById(currentValue);
            }
        }
        return currentValue;
    }

    private String getShiftById(Long idShitf) {
        if (mapShift == null) {
            mapShift = ShiftService.getInstance().getMapShift();
        }
        if (mapShift.get(idShitf) != null) {
            return mapShift.get(idShitf).getName();
        }
        return null;
    }

    private String getGroupNameById(Long id) {
        if (mapGroup == null) {
            mapGroup = GroupService.getInstance().getMapGroup();
        }
        if (mapGroup.get(id) != null) {
            return mapGroup.get(id).getName();
        }
        return null;
    }

    private String getLogicalReaderNameById(Long id) {
        if (mapLogicalReader == null) {
            mapLogicalReader = LogicalReaderService.getInstance().getMapLogicalReader();
        }
        if (mapLogicalReader.get(id) != null) {
            return mapLogicalReader.get(id).getName();
        }
        return null;
    }

    private String getZoneNameById(String id) {
        if (mapZone.get(id) != null) {
            return mapZone.get(id).getName();
        }
        return null;
    }

    private void loadZones() {
        zoneList = ZoneService.getInstance().getZones();
        mapZone = new HashMap<>(zoneList.size() * 2);
        for (Zone zone : zoneList) {
            mapZone.put(zone.getId().toString(), zone);
            mapZone.put(zone.getName(), zone);
        }
    }

    public boolean isHistoricalReport() {
        return historicalReport;
    }

    public String getCollectionTarget() {
        return collectionTarget;
    }

    public ConditionBuilder getFilters() {
        return filters;
    }

    public List<Alias> getProjection() {
        return projection;
    }

    public boolean isPaginationSet() {
        return pageNum != null && pageSize != null && pageSize != -1;
    }


    public int getSkip() {
        return skip;
    }

    public int getLimit() {
        return limit;
    }

    public Map<String, Order> getSort() {
        return sort;
    }

    private List<String> getSortDefinition() {
        List<String> sortList = new LinkedList<>();
        if (sort != null) {
            for (Map.Entry<String, Order> orderEntry : sort.entrySet()) {
                sortList.add("'" + orderEntry.getKey() + "' : 1");
            }
        }
        return sortList;
    }

    private List<String> getFiltersDefinition() {
        return filtersDefinition;
    }

    public String getConfigDefinition() {
        return "{ 'filters' : {" + join(",", getFiltersDefinition()) + " }, 'sort': { " +
                join(",", getSortDefinition()) + " }}";
    }

    public List<PropertyReport> getPropertyReportList() {
        return propertyReportList;
    }

    public Map<String, String> getPaths() {
        return paths;
    }

    public Map<String, Zone> getMapZone() {
        return mapZone;
    }

    public List<Zone> getZoneList() {
        return zoneList;
    }

    public Map<String, Date> getTimeStamps() {
        return timeStamps;
    }

    public boolean isTwoDimension() {
        return false;
    }

    public List<IRule> getDisplayRules() {
        return displayRules;
    }

    public void setDisplayRules(List<IRule> displayRules) {
        this.displayRules = displayRules;
    }

    public String removeForNativeObjects(String property) {
        String newProperty = property;
        if (this.dataTypeId != null) {
            if (this.dataTypeId.equals(ThingTypeField.Type.TYPE_LOGICAL_READER.value) ||
                    (this.dataTypeId.equals(ThingTypeField.Type.TYPE_GROUP.value)) ||
                    (this.dataTypeId.equals(ThingTypeField.Type.TYPE_THING_TYPE.value))) {
                newProperty = property.replace(".name", "");
            } else if ((this.dataTypeId.equals(ThingTypeField.Type.TYPE_SHIFT.value)) ||
                    (this.dataTypeId.equals(ThingTypeField.Type.TYPE_ZONE.value))) {
                newProperty = property.replace(".id", "");
            }
        }
        return newProperty;
    }

    public void setCollectionTarget(String collectionTarget) {
        this.collectionTarget = collectionTarget;
    }

    public void setDateFormatAndTimeZone(DateFormatAndTimeZone dateFormatAndTimeZone) {
        this.dateFormatAndTimeZone = dateFormatAndTimeZone;
    }

    public boolean isThingTypeUdfAsObject() {
        return thingTypeUdfAsObject;
    }

    public ReportDefinition getReportDefinition() {
        return reportDefinition;
    }

    public void addHeatPropery() {
        addHeatPropery(reportDefinition.getHeatProperty());
    }

    public void addHeatPropery(Map<String, ReportProperty> propertyMap) {
        ReportProperty heatProperty = propertyMap.get(Constants.HEAT);
        if (heatProperty != null) {
            String translate = translate(heatProperty, true, false);
            propertyReportList.add(new PropertyReport(translate, heatProperty.getPropertyName(),
                    HEAT, heatProperty.isNative(), false));
            addProjection(translate);

            logger.info("Heat label: " + heatProperty.getPropertyName());
        }
    }
}
