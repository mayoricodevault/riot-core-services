package com.tierconnect.riot.iot.services;


import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.mysema.query.types.expr.BooleanExpression;
import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.dao.RoleResourceDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.I18NService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.dao.ScheduledRuleDAO;
import com.tierconnect.riot.iot.dao.mongo.MongoScriptDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;

import javax.annotation.Generated;
import java.lang.reflect.Array;
import java.util.*;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class ReportDefinitionService extends ReportDefinitionServiceBase {

    public static final QResource RESOURCE = QResource.resource;
    public static final QScheduledRule SCHEDULED_RULE = QScheduledRule.scheduledRule;
    private String[] IGNORE_REPORT_EXTRA = new String[]{"reportFilter"};


    public static List<ReportDefinition> getZonesByName(String name) {
        HibernateQuery query = ReportDefinitionService.getReportDefinitionDAO().getQuery();
        return query.where(QReportDefinition.reportDefinition.name.eq(name)).list(QReportDefinition.reportDefinition);
    }

    public List<ReportDefinition> getByReportType(String reportType) throws NonUniqueResultException {
        try {
            return getReportDefinitionDAO().selectAllBy("reportType", reportType);
        } catch (org.hibernate.HibernateException e) {
            throw new HibernateException(e);
        }
    }


    @Override
    public ReportDefinition insert(ReportDefinition reportDefinition) {
        ReportDefinition definition = super.insert(reportDefinition);
        createResource(definition);
        return definition;
    }

    public void createResource(ReportDefinition definition) {
        ResourceService resourceService = ResourceService.getInstance();
        ResourceDAO resourceDAO = resourceService.getResourceDAO();
        Resource re = new Resource();
        setAcceptedAttributes(re, definition);
        re.setGroup(definition.getGroup());
        Resource reportInstance = resourceDAO.selectBy(RESOURCE.name.eq(Resource.REPORT_INSTANCES_MODULE));
        re.setParent(reportInstance);
        setLabel(definition, re);
        setDescription(definition, re);
        re.setType(ResourceType.REPORT_DEFINITION.getId());
        re.setTreeLevel(reportInstance.getTreeLevel() + 1);
        re.setName(Resource.REPORT_DEFINITION + definition.getId());
        re.setFqname(Resource.REPORT_DEFINITION + definition.getId());
        re.setTypeId(definition.getId());
        resourceService.insert(re);
    }

    private void setLabel(ReportDefinition definition, Resource re) {
        re.setLabel(definition.getGroup().getName() + " -> " + getFormattedType(definition.getReportType()) + " -> " +
                "\"" + definition.getName() + "\"");
    }

    public String getFormattedType(String reportType) {
        String result;
        switch (reportType) {
            case "tableSummary":
                result = "Table Summary";
                break;
            case "mapSummary":
                result = "Map Summary";
                break;
            case "mapSummaryByZone":
                result = "Map Summary by Zone";
                break;
            case "tableTimeSeries":
                result = "Table Time Series";
                break;
            case "map":
                result = "Map";
                break;
            case "mapHistory":
                result = "Map History";
                break;
            case "table":
                result = "Table";
                break;
            default:
                result = reportType;
                break;
        }
        return result;
    }

    private void setDescription(ReportDefinition definition, Resource re) {
        re.setDescription(definition.getGroup().getName() + " -> " + getFormattedType(definition.getReportType()) + "" +
                " -> \"" + definition.getName() + "\"");
    }

    private void setAcceptedAttributes(Resource re, ReportDefinition definition) {
        Set<String> attributes = new LinkedHashSet();
        attributes.add(Resource.READ_PERMISSION);
        attributes.add(Resource.UPDATE_PERMISSION);
        attributes.add(Resource.EXECUTE_PERMISSION);
        attributes.add(Resource.PRINT_PERMISSION);
        re.setAcceptedAttributes(attributes);
    }

    @Override
    public ReportDefinition update(ReportDefinition reportDefinition) {
        ResourceDAO resourceDAO = ResourceService.getInstance().getResourceDAO();
        Resource re = resourceDAO.selectBy(RESOURCE.typeId.eq(reportDefinition.getId()).and(RESOURCE.type.eq
                (ResourceType.REPORT_DEFINITION.getId())));
        re.setGroup(reportDefinition.getGroup());
        setLabel(reportDefinition, re);
        setAcceptedAttributes(re, reportDefinition);
        setDescription(reportDefinition, re);
        return super.update(reportDefinition);
    }

    @Override
    public void validateDelete(ReportDefinition reportDefinition) {
        if (existsScheduledRuleDependency(reportDefinition)) {
            throw new UserException("It is not possible to delete the report it has a dependency with a Scheduled Rule");
        }
    }

    /**
     * Validate the delete if a report has a dependency with a Scheduled Rule
     *
     * @param reportDefinition
     * @return
     */
    private boolean existsScheduledRuleDependency(ReportDefinition reportDefinition) {
        // get the report definition id
        Long reportId = reportDefinition.getId();
        // select all Scheduled Rules that contains the Report Definition selected
        ScheduledRuleDAO scheduledRuleDAO = ScheduledRuleService.getInstance().getScheduledRuleDAO();
        List<ScheduledRule> scheduledRuleList = scheduledRuleDAO.selectAllBy(SCHEDULED_RULE.reportDefinition.id.eq(reportId));
        // throw an error if there exists a dependency
        if (scheduledRuleList.size() > 0) {
            return true;
        }
        return false;
    }

    @Override
    public void delete(ReportDefinition reportDefinition) {
        ResourceDAO resourceDAO = ResourceService.getInstance().getResourceDAO();
        Resource re = resourceDAO.selectBy(RESOURCE.typeId.eq(reportDefinition.getId()).and(RESOURCE.type.eq
                (ResourceType.REPORT_DEFINITION.getId())));
        RoleResourceDAO roleResourceDAO = RoleResourceService.getInstance().getRoleResourceDAO();
        roleResourceDAO.deleteAllBy(QRoleResource.roleResource.resource.eq(re));
        List<Resource> res = resourceDAO.selectAllBy(RESOURCE.parent.eq(re));
        for (Resource re1 : res) {
            roleResourceDAO.deleteAllBy(QRoleResource.roleResource.resource.eq(re1));
            resourceDAO.delete(re1);
        }
        if (reportDefinition.getReportType().compareTo("mongo") == 0) {
            MongoScriptDAO.getInstance().delete(reportDefinition.getId().toString());
        }
        resourceDAO.delete(re);
        //VIZIX-641, fix delete bulk registries
        BackgroundProcessService.getInstance().deleteBulkByReport(reportDefinition);
        // VIZIX-2025 change status actions(status=DELETED)
        List<ReportActions> reportActions = ReportActionsService.getInstance().getReportActions(reportDefinition.getId());
        ActionConfigurationService configurationService = ActionConfigurationService.getInstance();
        for (ReportActions actions : reportActions) {
            configurationService.delete(actions.getActionConfiguration());
        }
        super.delete(reportDefinition);
    }

    public String[] getReportsDefinitionNamesByUser(User user){

        List<ReportDefinition> reports= ReportDefinitionService.getInstance().getReportsDefinitionByUser(user);
        String[] names = new String[ reports.size()];
        int i = 0;
        for (ReportDefinition report:reports ) {
            names[i] = report.getName();i++;
        }
       return names;
    }

    public List<ReportDefinition> getReportsDefinitionByUser(User user) {

        BooleanBuilder beAnd = new BooleanBuilder();
        beAnd = beAnd.and(QReportDefinition.reportDefinition.createdByUser.eq(user));

        List<ReportDefinition> reports = ReportDefinitionService.getInstance().listPaginated(beAnd,null, null);

        return reports;
    }


    public List<String> getExtraPropertyNames() {
        return Arrays.asList(IGNORE_REPORT_EXTRA);
    }

    /**
     * @param reportEntryProperty
     */
    public ValidationBean validationDataTypeInReportEntryOptionProperty(ReportEntryOptionProperty reportEntryProperty, ReportDefinition reportDefinition) {
        if ((StringUtils.isNotEmpty(reportEntryProperty.getDefaultMobileValue()) && (reportEntryProperty.getThingTypeFieldId() != null))) {
            ThingTypeField thingTypeField = ThingTypeFieldService.getInstance().get(reportEntryProperty.getThingTypeFieldId());
            Map<String, Object> udfMap = new HashMap<>();
            udfMap.put("value", reportEntryProperty.getDefaultMobileValue());
            ValidationBean validationBeanDataType = ThingsService.getInstance().isValidDataType(thingTypeField, reportEntryProperty.getPropertyName(), udfMap, reportDefinition.getGroup(), true, null);
            return validationBeanDataType;
        }
        return null;
    }

    /**
     * Method to create a new Report Definition
     *
     * @param namereport
     * @param group         Group object
     * @param createdByUser User object
     * @param filters       Map with properties of filter
     * @param properties    Map with properties of the section 'properties'
     */
    public static void createReportDefinition(
            String namereport
            , Group group
            , User createdByUser
            , Map<String, Object> filters
            , Map<String, Object> properties) {
        ReportDefinition reportDefinitionDevice = new ReportDefinition();
        reportDefinitionDevice.setName(namereport.trim() + " - " + group.getName());
        reportDefinitionDevice.setCreatedByUser(createdByUser);
        reportDefinitionDevice.setGroup(group);
        reportDefinitionDevice.setReportType("table");
        reportDefinitionDevice.setDefaultTypeIcon("pin");

        reportDefinitionDevice.setPinLabels(false);
        reportDefinitionDevice.setZoneLabels(false);
        reportDefinitionDevice.setTrails(false);
        reportDefinitionDevice.setClustering(false);
        reportDefinitionDevice.setPlayback(true);
        reportDefinitionDevice.setNupYup(false);
        reportDefinitionDevice.setDefaultList(false);
        reportDefinitionDevice.setDefaultColorIcon("009F6B");
        reportDefinitionDevice.setRunOnLoad(true);
        reportDefinitionDevice = ReportDefinitionService.getInstance().insert(reportDefinitionDevice);
        reportDefinitionDevice.setDefaultColorIcon("009F6B");
        reportDefinitionDevice.setIsMobile(Boolean.FALSE);
        reportDefinitionDevice.setIsMobileDataEntry(Boolean.FALSE);

        //Report Filter
        if (filters != null && filters.size() > 0) {
            String[] labelsFilter = (String[]) filters.get("labels");
            String[] propertyNamesFilter = (String[]) filters.get("propertyNames");
            String[] propertyOrdersFilter = (String[]) filters.get("propertyOrders");
            String[] operatorFilter = (String[]) filters.get("operators");
            String[] value = (String[]) filters.get("values");
            Boolean[] isEditable = (Boolean[]) filters.get("isEditable");
            for (int it = 0; it < Array.getLength(labelsFilter); it++) {
                ReportFilter reportFilter = ReportFilterService.getInstance().createReportFilter(
                        labelsFilter[it]
                        , propertyNamesFilter[it]
                        , propertyOrdersFilter[it]
                        , operatorFilter[it]
                        , value[it]
                        , isEditable[it]
                        , null
                        , reportDefinitionDevice);
                ReportFilterService.getInstance().insert(reportFilter);
            }
        }

        //Report Property
        String[] labels3 = (String[]) properties.get("labels");
        String[] propertyNames3 = (String[]) properties.get("propertyNames");
        String[] propertyOrders3 = (String[]) properties.get("propertyOrders");
        Long[] propertyTypeIds3 = (Long[]) properties.get("propertyTypes");
        Boolean[] propertyShowPopUp3 = (Boolean[]) properties.get("showPopUp");
        for (int it = 0; it < Array.getLength(labels3); it++) {
            ReportProperty reportProperty = ReportPropertyService.getInstance().createReportProperty(labels3[it],
                    propertyNames3[it],
                    propertyOrders3[it],
                    propertyTypeIds3[it],
                    propertyShowPopUp3[it],
                    reportDefinitionDevice);
            ReportPropertyService.getInstance().insert(reportProperty);
        }
    }

    public List<ReportDefinition> getbyFolder(Folder folder) {
        return getReportDefinitionDAO().getQuery()
                .where(QReportDefinition.reportDefinition.folder.isNull()).list(QReportDefinition.reportDefinition);
    }

    public ReportDefinition getByNameAndType(String name, String type) {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QReportDefinition.reportDefinition.name.eq(name)).and(QReportDefinition.reportDefinition.reportType.eq(type));
        return getReportDefinitionDAO().selectBy(be);
    }

    /**
     * Method that checks if the record exists in the database.
     *
     * @param reportDefinition the {@link ReportDefinition} isntance to verify.
     * @return return a boolean.
     */
    private boolean exists(ReportDefinition reportDefinition) {
        BooleanExpression predicate = QReportDefinition.reportDefinition.name.eq(reportDefinition.getName()).
                and(QReportDefinition.reportDefinition.reportType.eq(reportDefinition.getReportType())).
                and(QReportDefinition.reportDefinition.group.eq(reportDefinition.getGroup()));
        return getReportDefinitionDAO().getQuery().where(predicate).exists();
    }

    /**
     * Method that checks if the record exists in the database.
     * This method is used by problems in the active transaction.
     *
     * @param reportDefinition the {@link ReportDefinition} isntance to verify.
     * @return return a boolean.
     */
    //TODO: This method is used by problems in the active transaction.
    //TODO: Remove this method when the transaction is corrected. Victor Angel chambi Nina, 17/08/2017.
    private boolean existsUpdateCase(ReportDefinition reportDefinition) {
        BooleanExpression predicate = QReportDefinition.reportDefinition.name.eq(reportDefinition.getName()).
                and(QReportDefinition.reportDefinition.reportType.eq(reportDefinition.getReportType())).
                and(QReportDefinition.reportDefinition.group.eq(reportDefinition.getGroup()));
        return getReportDefinitionDAO().getQuery().where(predicate).count() > 1;
    }

    public Long sortReportType(String typeReport) {

        HashMap<String, String> orderLabel = orderLabels();
        LinkedHashMap<String, Long> orderedMap = sortHashMapByValues(orderLabel);
        Long orderId = sortReportType(typeReport, orderedMap);
        return orderId;

    }

    private HashMap<String, String> orderLabels() {
        List<String> labelTypes = Arrays.asList("@REPORT_VIEW_LABEL_TABLE_DETAIL", "@REPORT_VIEW_LABEL_MAP", "@REPORT_VIEW_LABEL_ZONE_MAP",
                "@REPORT_VIEW_LABEL_TABLE_HISTORY", "@REPORT_VIEW_LABEL_TABLE_SUMMARY", "@REPORT_VIEW_LABEL_TABLE_MONGO", "@REPORT_VIEW_LABEL_TABLE_CONNECTION");

        HashMap<String, String> orderLabel = new HashMap<>();
        for (String labelType : labelTypes) {
            String type = (new I18NService()).getKey(labelType, null, null);
            orderLabel.put(labelType, type);
        }
        return orderLabel;
    }

    public Long sortReportType(String typeReport, LinkedHashMap<String, Long> orderedMap) {
        Long orderId = null;
        String key = "";
        switch (typeReport) {
            case Constants.REPORT_TYPE_TABLE_DETAIL:
                key = "@REPORT_VIEW_LABEL_TABLE_DETAIL";
                orderId = orderedMap.get(key);
                break;
            case Constants.REPORT_TYPE_MAP:
                key = "@REPORT_VIEW_LABEL_MAP";
                orderId = orderedMap.get(key);
                break;
            case Constants.REPORT_TYPE_MAP_SUMMARY:
                key = "@REPORT_VIEW_LABEL_ZONE_MAP";
                orderId = orderedMap.get(key);
                break;
            case Constants.REPORT_TYPE_TABLE_HISTORY:
                key = "@REPORT_VIEW_LABEL_TABLE_HISTORY";
                orderId = orderedMap.get(key);
                break;
            case Constants.REPORT_TYPE_TABLE_SUMMARY:
                key = "@REPORT_VIEW_LABEL_TABLE_SUMMARY";
                orderId = orderedMap.get(key);
                break;
            case Constants.REPORT_TYPE_TABLE_SCRIPT:
                key = "@REPORT_VIEW_LABEL_TABLE_MONGO";
                orderId = orderedMap.get(key);
                break;
            case Constants.REPORT_TYPE_TABLE_CONNECTION:
                key = "@REPORT_VIEW_LABEL_TABLE_CONNECTION";
                orderId = orderedMap.get(key);
                break;
        }

        return orderId;
    }

    private LinkedHashMap<String, Long> sortHashMapByValues(
            HashMap<String, String> passedMap) {
        List<String> mapKeys = new ArrayList<>(passedMap.keySet());
        List<String> mapValues = new ArrayList<>(passedMap.values());
        Collections.sort(mapValues);
        Collections.sort(mapKeys);

        LinkedHashMap<String, Long> sortedMap =
                new LinkedHashMap<>();

        Iterator<String> valueIt = mapValues.iterator();
        Long id = 0L;
        while (valueIt.hasNext()) {
            String val = valueIt.next();
            Iterator<String> keyIt = mapKeys.iterator();

            while (keyIt.hasNext()) {
                String key = keyIt.next();
                String comp1 = passedMap.get(key);
                String comp2 = val;

                if (comp1.equals(comp2)) {
                    keyIt.remove();
                    sortedMap.put(key, id);
                    id = id + 1;
                    break;
                }
            }
        }
        return sortedMap;
    }

    public void updateAllReports() {
        List<ReportDefinition> listAllReports = ReportDefinitionService.getInstance().listPaginated(null, null);
        HashMap<String, String> orderLabel = orderLabels();
        LinkedHashMap<String, Long> orderedMap = sortHashMapByValues(orderLabel);
        for (ReportDefinition reportDefinition : listAllReports) {
            Long typeOrder = sortReportType(reportDefinition.getReportType(), orderedMap);
            reportDefinition.setTypeOrder(typeOrder);
            try {
                ReportDefinitionService.getInstance().update(reportDefinition);
            } catch (UserException ignored) {
            }
        }
    }

    /**
     * @param reportDefinition
     * @return an error message when report filter or rule operator is invalid
     */
    public String validOperator(ReportDefinition reportDefinition) {
        List<ReportFilter> reportFilters = reportDefinition.getReportFilter();
        for (ReportFilter reportFilter : reportFilters) {
            if (isInvalidOperator(reportFilter.getOperator())) {
                return "Invalid Filter Operator: " + reportFilter.getOperator();
            }
        }
        List<ReportRule> reportRules = reportDefinition.getReportRule();
        for (ReportRule reportRule : reportRules) {
            if (isInvalidOperator(reportRule.getOperator())) {
                return "Invalid Rule Operator: " + reportRule.getOperator();
            }
        }
        return "";
    }

    /**
     * @param reportFilters
     * @param reportRules
     * @return an error message when report filter or rule operator is invalid
     */
    public String validOperatorMap(List<Map<String, Object>> reportFilters, List<Map<String, Object>> reportRules) {
        for (Map<String, Object> reportFilter : reportFilters) {
            if (isInvalidOperator(reportFilter.get("operator"))) {
                return "Invalid Filter Operator: " + reportFilter.get("operator");
            }
        }
        for (Map<String, Object> reportRule : reportRules) {
            if (isInvalidOperator(reportRule.get("operator"))) {
                return "Invalid Rule Operator: " + reportRule.get("operator");
            }
        }
        return "";
    }

    /**
     * @param operator
     * @return true if the operator is invalid
     */
    public Boolean isInvalidOperator(Object operator) {
        return (operator != null)
                && (StringUtils.isNotEmpty(operator.toString()))
                && (!Constants.OP_TYPES.contains(operator.toString()));
    }

    @Override
    public void validateInsert(ReportDefinition reportDefinition) {
        super.validateInsert(reportDefinition);
        if (!reportDefinition.isNewAndComparable()) {
            throw new UserException("Report definition is not comparable to validate.");
        }
        if (exists(reportDefinition)) {
            throw new UserException("Report definition already exists.");
        }
    }

    @Override
    public void validateUpdate(ReportDefinition reportDefinition) {
        super.validateUpdate(reportDefinition);
        if (!reportDefinition.isUpdateAndComparable()) {
            throw new UserException("Report definition is not comparable to validate.");
        }
        if (existsUpdateCase(reportDefinition)) {
            throw new UserException("Report definition already exists.");
        }
    }

    public void validateAndAssociateThingType(ReportEntryOption reportEntryOption, Object thingTypeIdObj) {
        if(reportEntryOption.getNewOption()) {
            validateThingTypeObject(reportEntryOption, thingTypeIdObj);
            Long thingTypeId = null;
            if(thingTypeIdObj instanceof Number){
                thingTypeId = Long.parseLong(thingTypeIdObj.toString());
            }
            validateThingTypeObject(reportEntryOption, thingTypeId);
            ThingType thingType = ThingTypeService.getInstance().get(thingTypeId);
            validateThingTypeObject(reportEntryOption, thingType);
            reportEntryOption.setThingType(thingType);
        } else {
            reportEntryOption.setThingType(null);
        }
    }

    private void validateThingTypeObject(ReportEntryOption reportEntryOption, Object thingTypeObj) {
        if(thingTypeObj == null) {
            throw new UserException("Data Entry '"+reportEntryOption.getName()+"': When option 'New' is selected, it is necessary to select a Thing Type.");
        }
    }
}

