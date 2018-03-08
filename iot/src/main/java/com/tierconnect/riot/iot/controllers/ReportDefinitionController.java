package com.tierconnect.riot.iot.controllers;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.controllers.ConnectionController;
import com.tierconnect.riot.appcore.controllers.FavoriteController;
import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.*;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.iot.dao.mongo.MongoScriptDAO;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.entities.reports.DataEntry;
import com.tierconnect.riot.iot.entities.reports.DataEntryHeader;
import com.tierconnect.riot.iot.job.SentEmailReportJob;
import com.tierconnect.riot.iot.reports.autoindex.IndexCreatorManager;
import com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ListReportLog;
import com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogInfo;
import com.tierconnect.riot.iot.reports.autoindex.services.ReportLogMongoService;
import com.tierconnect.riot.iot.reports_integration.ReportDefinitionValidator;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.iot.utils.ReportRuleUtils;
import com.tierconnect.riot.iot.utils.VisibilityThingUtils;
import com.tierconnect.riot.sdk.dao.MongoExecutionException;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.*;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.subject.Subject;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.tierconnect.riot.commons.Constants.REPORT_TYPES;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static com.tierconnect.riot.commons.Constants.ActionHTTPConstants.*;

@Path("/reportDefinition")
@Api("/reportDefinition")
public class ReportDefinitionController extends ReportDefinitionControllerBase {
    static Logger logger = Logger.getLogger(ReportDefinitionController.class);

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
    @ApiOperation(position = 1, value = "Get a List of ReportDefinitions (AUTO)")
    public Response listReportDefinitions(@QueryParam("pageSize") Integer pageSize,
                                          @QueryParam("pageNumber") Integer pageNumber,
                                          @QueryParam("order") String order,
                                          @QueryParam("where") String where,
                                          @Deprecated @QueryParam("extra") String extra,
                                          @Deprecated @QueryParam("only") String only,
                                          @QueryParam("visibilityGroupId") Long visibilityGroupId,
                                          @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
                                          @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
                                          @DefaultValue("false") @QueryParam("returnFavorite") boolean returnFavorite, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project) {
        List<ReportDefinition> reportDefinitions = new ArrayList<>();
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        Long count = 0L;

        Pagination pagination = new Pagination(pageNumber, pageSize);

        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(ReportDefinition.class.getCanonicalName(),
                visibilityGroupId);
        EntityVisibility entityVisibility = getEntityVisibility();
        boolean hasAllReportDefinitionsRead = PermissionsUtils.isPermitted(SecurityUtils.getSubject(),
                "reportDefinition:r");
        BooleanBuilder beM = new BooleanBuilder();
        BooleanBuilder be = new BooleanBuilder();
        if (hasAllReportDefinitionsRead) {
            // 2. Limit visibility based on user's group and the object's group (group based authorization)
            be = be.and(GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QReportDefinition
                    .reportDefinition, visibilityGroup, upVisibility, downVisibility));
            beM = beM.or(be);
        } else {
            be = be.and(GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QReportDefinition
                    .reportDefinition, visibilityGroup, upVisibility, "false"));
            beM = beM.or(be);
        }
        // 4. Implement filtering
        Set objectPermissions = RiotShiroRealm.getObjectPermissionsCache().get("reportDefinition");
        if (objectPermissions != null && !objectPermissions.isEmpty()) {
            BooleanBuilder be2 = new BooleanBuilder(VisibilityUtils.limitVisibilityPredicate(visibilityGroup,
                    QReportDefinition.reportDefinition.group, true, true));
            be2 = be2.and(QReportDefinition.reportDefinition.id.in(objectPermissions));
            beM = beM.or(be2);
        }
        beM = beM.and(QueryUtils.buildSearch(QReportDefinition.reportDefinition, where));
        beM = addGroupFilter(visibilityGroupId, downVisibility, beM);
        count = ReportDefinitionService.getInstance().countList(beM);
        // 3. Implement pagination
        if (order != null) {
            order = order.replace("reportType", "typeOrder");
        }
        reportDefinitions = ReportDefinitionService.getInstance().listPaginated(beM, pagination, order);
        for (ReportDefinition reportDefinition : reportDefinitions) {
            //Operator Validation
            String validOperator = ReportDefinitionService.getInstance().validOperator(reportDefinition);
            if (validOperator.length() > 0) {
                return RestUtils.sendResponseWithCode(validOperator, 400);
            }
            // 5a. Implement extra
            Map<String, Object> publicMap = null;
            if (extra != null) {
                publicMap = QueryUtils.mapWithExtraFields(reportDefinition, extra, ReportDefinitionService.getInstance().getExtraPropertyNames());
                addToPublicMap(reportDefinition, publicMap, extra);
            } else {
                publicMap = reportDefinition.publicMapSimple();
            }
            if (only != null) {
                // 5b. Implement only
                QueryUtils.filterOnly(publicMap, only, extra);
                QueryUtils.filterProjectionNested(publicMap, project, extend);
            }
            list.add(publicMap);
        }
        if (returnFavorite) {
            User user = (User) SecurityUtils.getSubject().getPrincipal();
            list = FavoriteService.getInstance().addFavoritesToList(list, user.getId(), "report");
        }
        Map<String, Object> mapResponse = new HashMap<String, Object>();
        mapResponse.put("total", count);
        mapResponse.put("results", list);
        return RestUtils.sendOkResponse(mapResponse);
    }

    private BooleanBuilder addGroupFilter(Long visibilityGroupId, String downVisibility, BooleanBuilder beM) {
        if((StringUtils.isEmpty(downVisibility) || downVisibility.equals("false"))
                && visibilityGroupId != null && !visibilityGroupId.equals(1L)) { //ignore root
            beM = beM.and(QReportDefinition.reportDefinition.group.id.eq(visibilityGroupId));
        }
        return beM;
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
    @ApiOperation(position = 2, value = "Select a ReportDefinition (AUTO)")
    public Response selectReportDefinitions(@PathParam("id") Long id,
                                            @Deprecated @QueryParam("extra") String extra,
                                            @QueryParam("only") String only,
                                            @ApiParam(value = "Extends nested properties")
                                            @QueryParam("extend") String extend,
                                            @ApiParam(value = "Projects only nested properties")
                                            @QueryParam("project") String project,
                                            @QueryParam("createRecent")
                                            @DefaultValue("false") Boolean createRecent) {
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
        if (reportDefinition == null) {
            return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found", id));
        }
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        DateFormatAndTimeZone dateFormatAndTimeZone = UserService.getInstance().getDateFormatAndTimeZone(user);
        reportDefinition.setDateFormatAndTimeZone(dateFormatAndTimeZone);
        if (createRecent) {
            RecentService.getInstance().insertRecent(reportDefinition.getId(), reportDefinition.getName(), "report", reportDefinition.getGroup());
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, reportDefinition);
        validateSelect(reportDefinition);
        // 5a. Implement extra
        Map<String, Object> publicMap = QueryUtils.mapWithExtraFields(reportDefinition, extra, getExtraPropertyNames());
        publicMap = QueryUtils.mapWithExtraFieldsNested(reportDefinition, publicMap, extend, getExtraPropertyNames());
        addToPublicMap(reportDefinition, publicMap, extra);
        // 5b. Implement only
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QFavorite.favorite.typeElement.eq("report"));
        be = be.and(QFavorite.favorite.elementId.eq(reportDefinition.getId()));

        List<Favorite> favorites = FavoriteService.getInstance().listPaginated(be, null, null);
        QueryUtils.filterOnly(publicMap, only, extra);
        QueryUtils.filterProjectionNested(publicMap, project, extend);
        if (!favorites.isEmpty()){
            publicMap.put("favoriteId", favorites.get(0).getId());
        }
        return RestUtils.sendOkResponse(publicMap);
    }

    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresPermissions(value = {"reportDefinition:i"})
    @ApiOperation(position = 3, value = "Insert a ReportDefinition (AUTO)")
    public Response insertReportDefinition(Map<String, Object> map, @QueryParam("createRecent") @DefaultValue("false") Boolean createRecent) {

        Map<String, Object> publicMap = null;
        try {
            Subject subject = SecurityUtils.getSubject();
            User currentUser = (User) subject.getPrincipal();
            EntityVisibility entityVisibility = getEntityVisibility();
            Group reportGroup = VisibilityUtils.getObjectGroup(map);
            GeneralVisibilityUtils.limitVisibilityInsert(entityVisibility, reportGroup);

            //QueryUtils.filterWritePermissions( ReportDefinition.class, map );

            //1. Validation
            ValidationBean validationBean = this.validate(map);
            if (validationBean.isError()) {
                return RestUtils.sendBadResponse(validationBean.getErrorDescription());
            }

            ReportDefinitionValidator rdv = new ReportDefinitionValidator(reportGroup, map);

            validationBean = rdv.getValidationResponse();
            if (validationBean.isError()) {
                return RestUtils.sendBadResponse(validationBean.getErrorDescription());
            }

            List<Map<String, Object>> reportFilters = (List<Map<String, Object>>) map.get("reportFilter");
            List<Map<String, Object>> reportCustomFilters = (List<Map<String, Object>>) map.get("reportCustomFilter");
            List<Map<String, Object>> reportProperties = (List<Map<String, Object>>) map.get("reportProperty");
            List<Map<String, Object>> reportGroupBys = (List<Map<String, Object>>) map.get("reportGroupBy");
            List<Map<String, Object>> reportRules = (List<Map<String, Object>>) map.get("reportRules");
            List<Map<String, Object>> reportEntryOptionBy = (List<Map<String, Object>>) map.get("reportEntryOption");
            List<Map<String, Object>> reportDefinitionConfig = (List<Map<String, Object>>) map.get("reportDefinitionConfig");
            List<Map<String, Object>> reportAction = (List<Map<String, Object>>) map.get("reportActions");

            ReportDefinition reportDefinition = new ReportDefinition();
            map.remove("reportFilter");
            map.remove("reportCustomFilter");
            map.remove("reportProperty");
            map.remove("reportGroupBy");
            map.remove("reportRules");
            map.remove("groupTypeFloor");
            map.remove("reportEntryOption");
            map.remove("reportDefinitionConfig");
            map.remove("reportActions");

            BeanUtils.setProperties(map, reportDefinition);
            validateInsert(reportDefinition);
            ValidationBean validationBeanReportDefinition = this.validateReportDefinition(reportDefinition);
            if (validationBeanReportDefinition.isError()) {
                return RestUtils.sendBadResponse(validationBeanReportDefinition.getErrorDescription());
            }
            String validOperator = ReportDefinitionService.getInstance().validOperatorMap(reportFilters, reportRules);
            if (validOperator.length() > 0) {
                return RestUtils.sendResponseWithCode(validOperator, 400);
            }
            reportDefinition.setCenterLat(reportDefinition.getDoubleFromString(map.get("centerLat")));
            reportDefinition.setCenterLon(reportDefinition.getDoubleFromString(map.get("centerLon")));

            reportDefinition.setCreatedByUser(currentUser);
            Long typeOrder = ReportDefinitionService.getInstance().sortReportType(reportDefinition.getReportType());
            reportDefinition.setTypeOrder(typeOrder);
            ReportDefinitionService.getInstance().insert(reportDefinition);

            //ReportDefinitionConfig
            reportDefinition.setReportDefinitionConfig(this.getReportDefinitionConfigList(reportDefinitionConfig,
                    reportDefinition));
            //Insert in mongo If is a mongo report
            if (reportDefinition.getReportType().compareTo("mongo") == 0) {
                ReportDefinitionConfig repDefConfigMongoScript = reportDefinition.getReportDefConfigItem
                        ("SCRIPT");
                MongoScriptDAO.getInstance().insert(reportDefinition.getId().toString(),
                        repDefConfigMongoScript.getKeyValue());
            }

            //ReportFilter insert
            List<ReportFilter> reportFilterList = new LinkedList<>();
            if (reportFilters != null) {
                for (Map<String, Object> reportFilterMap : reportFilters) {
                    ReportFilter reportFilter = new ReportFilter();
                    //reportFilter.setProperties(reportFilterMap, reportDefinition);
                    ReportFilterService.getInstance().setProperties(reportFilter, reportFilterMap, reportDefinition);
                    ReportFilterService.getInstance().insert(reportFilter);
                    reportFilterList.add(reportFilter);
                }
            }
            reportDefinition.setReportFilter(reportFilterList);
            //ReportCustomFilter insert
            List<ReportCustomFilter> reportCustomFilterList = new LinkedList<>();
            if (reportCustomFilters != null) {
                for (Map<String, Object> reportFilterMap : reportCustomFilters) {
                    ReportCustomFilter reportCustomFilter = new ReportCustomFilter();
                    ReportCustomFilterService.getInstance().setProperties(reportCustomFilter, reportFilterMap, reportDefinition);
                    ReportCustomFilterService.getInstance().insert(reportCustomFilter);
                    reportCustomFilterList.add(reportCustomFilter);
                }
            }
            reportDefinition.setReportCustomFilter(reportCustomFilterList);

            //reportProperties insert
            List<ReportProperty> reportPropertyList = new LinkedList<>();
            Set<String> propertyLabels = new HashSet<>();
            if (reportProperties != null) {
                for (Map<String, Object> reportPropertyMap : reportProperties) {
                    ReportProperty reportProperty = new ReportProperty();
                    ReportPropertyService.getInstance().setProperties(reportProperty, reportPropertyMap,
                            reportDefinition);
                    if (!propertyLabels.contains(reportProperty.getLabel())) {
                        ReportPropertyService.getInstance().insert(reportProperty);
                        reportPropertyList.add(reportProperty);
                        propertyLabels.add(reportProperty.getLabel());
                    }
                }
            }
            reportDefinition.setReportProperty(reportPropertyList);

            //reportGroupBy insert
            List<ReportRule> reportRulesList = new LinkedList<>();
            if (reportRules != null) {
                for (Map<String, Object> reportRulesMap : reportRules) {
                    ReportRule reportRule = new ReportRule();
                    ReportRuleService.getInstance().setProperties(reportRule, reportRulesMap, reportDefinition);
                    ReportRuleService.getInstance().insert(reportRule);
                    reportRulesList.add(reportRule);
                }
            }
            reportDefinition.setReportRule(reportRulesList);

            //reportGroupBy insert
            List<ReportGroupBy> reportGroupByList = new LinkedList<>();
            if (reportGroupBys != null) {

                for (Map<String, Object> reportGroupByMap : reportGroupBys) {
                    Long thingTypeIdReport = null;
                    Long thingTypeFieldId = null;
                    Long parentThingTypeFieldId = null;

                    ReportGroupBy reportGroupBy = new ReportGroupBy();
                    reportGroupBy.setPropertyName((String) reportGroupByMap.get("propertyName"));
                    reportGroupBy.setLabel((String) reportGroupByMap.get("label"));
                    reportGroupBy.setSortBy((String) reportGroupByMap.get("sortBy"));
                    reportGroupBy.setRanking(reportDefinition.getDisplayOrder(reportGroupByMap.get("ranking")));
                    reportGroupBy.setOther((Boolean) reportGroupByMap.get("other"));

                    if (reportGroupByMap.get("thingTypeId") != null) {
                        thingTypeIdReport = ((Number) reportGroupByMap.get("thingTypeId")).longValue();
                        reportGroupBy.setThingType(ThingTypeService.getInstance().get(thingTypeIdReport));
                    }

                    if (reportGroupByMap.get("thingTypeFieldId") != null && !reportGroupByMap.get("thingTypeFieldId")
                            .toString().equals("")
                            && StringUtils.isNumeric(reportGroupByMap.get("thingTypeFieldId").toString())) {
                        thingTypeFieldId = ((Number) reportGroupByMap.get("thingTypeFieldId")).longValue();
                        reportGroupBy.setThingTypeField(ThingTypeFieldService.getInstance().get(thingTypeFieldId));
                    }

                    if (reportGroupByMap.get("parentThingTypeId") != null && !reportGroupByMap.get
                            ("parentThingTypeId").toString().equals("")) {
                        parentThingTypeFieldId = ((Number) reportGroupByMap.get("parentThingTypeId")).longValue();
                        reportGroupBy.setParentThingType(ThingTypeService.getInstance().get(parentThingTypeFieldId));
                    }

                    if (reportGroupByMap.get("unit") != null && !((String) reportGroupByMap.get("unit")).trim().isEmpty()) {
                        reportGroupBy.setUnit((String) reportGroupByMap.get("unit"));
                    }
                    reportGroupBy.setReportDefinition(reportDefinition);
                    reportGroupBy.setByPartition(reportGroupByMap.get("byPartition") != null && (boolean)
                            reportGroupByMap.get("byPartition"));
                    ReportGroupByService.getInstance().insert(reportGroupBy);
                    reportGroupByList.add(reportGroupBy);
                }
            }
            reportDefinition.setReportGroupBy(reportGroupByList);

            //reportEntryOptions insert
            List<ReportEntryOption> reportEntryOptionByList = new LinkedList<>();
            if (reportEntryOptionBy != null) {
                //TODO review this code because the code inside  the reportEntryOptionListOld's condition it isn't used
                //Delete past data
                List<ReportEntryOption> reportEntryOptionListOld = reportDefinition.getReportEntryOption();
                reportDefinition.setReportEntryOption(reportEntryOptionByList);
                if (reportEntryOptionListOld != null) {
                    for (ReportEntryOption reportEntryOption : reportEntryOptionListOld) {
                        ReportEntryOptionService.getInstance().delete(reportEntryOption);
                    }
                }
                //Create new data
                for (Map<String, Object> reportEntryOptionByMap : reportEntryOptionBy) {
                    ReportEntryOption reportEntryOption = new ReportEntryOption();
                    reportEntryOption.setName((String) reportEntryOptionByMap.get("name"));
                    reportEntryOption.setLabel((String) reportEntryOptionByMap.get("label"));
                    reportEntryOption.setDisplayOrder(Float.parseFloat(reportEntryOptionByMap.get("displayOrder")
                            .toString()));
                    reportEntryOption.setAssociate((Boolean) reportEntryOptionByMap.get("associate"));
                    reportEntryOption.setDisassociate((Boolean) reportEntryOptionByMap.get("disassociate"));
                    reportEntryOption.setNewOption((Boolean) reportEntryOptionByMap.get("newOption"));
                    reportEntryOption.setEditOption((Boolean) reportEntryOptionByMap.get("editOption"));
                    reportEntryOption.setDeleteOption((Boolean) reportEntryOptionByMap.get("deleteOption"));
                    reportEntryOption.setRFIDPrint((Boolean) reportEntryOptionByMap.get("RFIDPrint"));
                    reportEntryOption.setGroup(
                            reportEntryOptionByMap.get("group") != null ?
                                    GroupService.getInstance().get(Long.parseLong(reportEntryOptionByMap.get("group")
                                            .toString())) : null);
                    ReportDefinitionService.getInstance()
                            .validateAndAssociateThingType(reportEntryOption, reportEntryOptionByMap.get("thingTypeId"));
                    if (reportEntryOption.getRFIDPrint() && reportEntryOptionByMap.get("defaultRFIDPrint") != null &&
                            reportEntryOptionByMap.get("defaultZPLTemplate") != null) {
                        reportEntryOption.setDefaultRFIDPrint(Long.parseLong(reportEntryOptionByMap.get
                                ("defaultRFIDPrint").toString()));
                        reportEntryOption.setDefaultZPLTemplate(Long.parseLong(reportEntryOptionByMap.get
                                ("defaultZPLTemplate").toString()));
                    }
                    reportEntryOption.setIsMobile(reportEntryOptionByMap.get("isMobile") != null ?
                            (Boolean) reportEntryOptionByMap.get("isMobile") : false);
                    reportEntryOption.setReportDefinition(reportDefinition);
                    reportEntryOption = ReportEntryOptionService.getInstance().insert(reportEntryOption);

                    //Report Entry Option Properties
                    List<Map<String, Object>> reportEntryOptionPropertyBy = (List<Map<String, Object>>) reportEntryOptionByMap.get("reportEntryOptionProperty");
                    List<ReportEntryOptionProperty> reportEntryOptionPropByList = new LinkedList<>();
                    ReportEntryOptionProperty reportEntryOptionProperty = null;
                    if (reportEntryOptionPropertyBy != null) {
                        for (Map<String, Object> reportEntryPropertyMap : reportEntryOptionPropertyBy) {
                            ReportEntryOptionProperty reportEntryProperty = new ReportEntryOptionProperty();
                            //Object aTrue = ((Entry) reportEntryPropertyMap.entrySet().toArray()[11]).setValue("true");
                            reportEntryProperty.setReportEntryOption(reportEntryOption);
                            reportEntryProperty.setProperties(reportEntryPropertyMap, reportEntryOption);
                            reportEntryOptionProperty = ReportEntryOptionPropertyService.getInstance().insert
                                    (reportEntryProperty);

                            //insert the data of picklist
                            if (!reportEntryProperty.getAllPropertyData() &&
                                    (reportEntryProperty.getEntryFormPropertyDatas() != null)) {
                                for (EntryFormPropertyData data : reportEntryProperty.getEntryFormPropertyDatas()) {
                                    data.setReportEntryOptionProperty(reportEntryOptionProperty);
                                    EntryFormPropertyDataService.getInstance().insert(data);
                                }
                            }

                            reportEntryOptionPropByList.add(reportEntryProperty);
                        }
                    }

                    reportEntryOption.setReportEntryOptionProperties(reportEntryOptionPropByList);
                    reportEntryOptionByList.add(reportEntryOption);
                }
            }

            reportDefinition.setReportEntryOption(reportEntryOptionByList);

            // Update ReportAction
            if (reportAction != null) {
                saveActionConfiguration(reportDefinition, reportAction, true);
            }

            if (map.containsKey("groupTypeFloor.id") && map.get("groupTypeFloor.id") != null && !map.get
                    ("groupTypeFloor.id").toString().isEmpty()) {
                GroupType groupType = GroupTypeService.getInstance().get(((Number) map.get("groupTypeFloor.id"))
                        .longValue());
                reportDefinition.setGroupTypeFloor(groupType);
            }

            Object roleId = map.get("roleShare.id");
            if (roleId != null && roleId instanceof Number) {
                Role role = RoleService.getInstance().get(((Number) roleId).longValue());
                reportDefinition.setRoleShare(role);
            }

            Object groupId = map.get("groupShare.id");
            if (groupId != null && groupId instanceof Number) {
                Group group = GroupService.getInstance().get(((Number) groupId).longValue());
                reportDefinition.setGroupShare(group);
            }

            Object shiftId = map.get("shift.id");
            if (shiftId != null && shiftId instanceof Number) {
                Shift shift = ShiftService.getInstance().get(((Number) shiftId).longValue());
                reportDefinition.setShift(shift);
            }
            reportDefinition = validationEmailRecipients(map, reportDefinition);
            publicMap = reportDefinition.publicMap();

            SentEmailReportJob.reschedule(reportDefinition.getId());
            try {
                PickListFieldsService.getInstance().updatePickListWithFilters(reportDefinition);
            } catch (Exception e) {
                return RestUtils.sendCreatedResponse(publicMap);
            }

            if (createRecent) {
                RecentService.getInstance().insertRecent(reportDefinition.getId(), reportDefinition.getName(), "report", reportDefinition.getGroup());
            }
        } catch (UserException | MongoExecutionException e) {
            logger.error(e.getMessage());
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }

        return RestUtils.sendCreatedResponse(publicMap);
    }

    private void saveActionConfiguration(ReportDefinition reportDefinition, List<Map<String, Object>> reportActionList, boolean create) {
        ReportActionsService reportActionsService = ReportActionsService.getInstance();
        ActionConfigurationService actionConfigurationService = ActionConfigurationServiceBase.getInstance();

        // remove relations
        Map<Long, ActionConfiguration> mapActionDelete = new HashMap<>();
        List<ReportActions> reportActionsList = reportActionsService.getReportActions(reportDefinition.getId());
        for (ReportActions reportActions : reportActionsList) {
            mapActionDelete.put(reportActions.getActionConfiguration().getId(), reportActions.getActionConfiguration());
            reportActionsService.delete(reportActions);
        }

        // save or update actions
        List<String> nameActionsList = new ArrayList<>(reportActionList.size());
        for (Map<String, Object> mapAction : reportActionList) {
            ActionConfiguration actionConfiguration = null;
            String oldConfiguration = null;
            if (create) {
                mapAction.remove("id");
            }
            if (mapAction.get("id") != null) {
                actionConfiguration = actionConfigurationService.
                        getActionConfigurationActive(Long.valueOf(String.valueOf(mapAction.get("id"))));
            }
            if (actionConfiguration == null) {
                actionConfiguration = new ActionConfiguration();
                mapAction.remove("id");
            }
            oldConfiguration = actionConfiguration.getConfiguration();
            // update action
            BeanUtils.setProperties(mapAction, actionConfiguration);
            actionConfiguration.setDisplayOrder((Integer) mapAction.get("displayOrder"));
            actionConfiguration.setStatus(Constants.ACTION_STATUS_ACTIVE); // update status
            actionConfiguration.setName(Utilities.removeSpaces(actionConfiguration.getName()));
            if (actionConfiguration.getId() == null) {
                actionConfiguration.setCode("REPORT_" + System.currentTimeMillis()); // set code temp
            }
            // validation with other reports
            actionConfigurationService.validateAssociationActionReport(reportDefinition.getId(), actionConfiguration);

            if (actionConfiguration.getId() != null) {
                actionConfigurationService.update(actionConfiguration, oldConfiguration);
            } else {
                actionConfigurationService.insert(actionConfiguration);
            }
            // validate name
            if (nameActionsList.contains(actionConfiguration.getName())) {
                throw new UserException(String.format("Action name '[%s]' already exist in this report", actionConfiguration.getName()));
            }

            // associate to report
            ReportActions reportActions = new ReportActions();
            reportActions.setDisplayOrder(actionConfiguration.getDisplayOrder());
            reportActions.setActionConfiguration(actionConfiguration);
            reportActions.setReportDefinition(reportDefinition);
            reportActions.setCreatedByUser(reportDefinition.getCreatedByUser());
            reportActionsService.insert(reportActions);

            nameActionsList.add(actionConfiguration.getName());
            mapActionDelete.remove(actionConfiguration.getId());
        }

        // remove actions
        deleteActionConfiguration(mapActionDelete);
    }

    private void deleteActionConfiguration(Map<Long, ActionConfiguration> mapActionDelete) {
        for (Map.Entry<Long, ActionConfiguration> mapAction : mapActionDelete.entrySet()) {
            ActionConfigurationServiceBase.getInstance().delete(mapAction.getValue());
        }
    }

    /****************************************
     * Get Report Definition config List
     ***************************************/
    public List<ReportDefinitionConfig> getReportDefinitionConfigList(List<Map<String, Object>> reportDefinitionConfig,
                                                                      ReportDefinition reportDefinition) throws
            UserException, MongoExecutionException {
        List<ReportDefinitionConfig> reportDefinitionConfList = new LinkedList<>();
        if (reportDefinitionConfig != null) {
            for (Map<String, Object> reportDefConfigMap : reportDefinitionConfig) {
                ValidationBean validation = ReportDefinitionConfigService.getInstance().validaReportDefinitionConfig
                        (reportDefConfigMap, reportDefinition);
                if (validation.isError()) {
                    throw new UserException(validation.getErrorDescription());
                }
                ReportDefinitionConfig reportDefinitionConf = new ReportDefinitionConfig();
                ReportDefinitionConfigService.getInstance().setProperties(reportDefinitionConf, reportDefConfigMap,
                        reportDefinition);
                ReportDefinitionConfigService.getInstance().insert(reportDefinitionConf);
                reportDefinitionConfList.add(reportDefinitionConf);
            }
        }
        return reportDefinitionConfList;
    }


    private ValidationBean validate(Map<String, Object> map){
        ValidationBean response = new ValidationBean();
        ArrayList<String> messages = new ArrayList<>();

        if (!validateDuplicatedValuesMap(map, "reportProperty")) {
            messages.add("Labels in properties section should not have duplicate values.");
        }
        ValidationBean validationEntryPoint = validateDuplicatedValuesEntryOption(map, "reportEntryOption");
        if (validationEntryPoint.isError()) {
            messages.add(validationEntryPoint.getErrorDescription());
        }
        if (messages.size() > 0) {
            response.setErrorDescription(StringUtils.join(messages, ","));
        }
        return response;
    }

    @PATCH
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
    @ApiOperation(position = 4, value = "Update a ReportDefinition (AUTO)")
    public Response updateReportDefinition(@PathParam("id") Long id, Map<String, Object> map) {
        Map<String, Object> publicMap = null;
        try {
            //QueryUtils.filterWritePermissions( ReportDefinition.class, map );
            ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
            if (reportDefinition == null) {
                return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found", id));
            }
            Group reportGroup = VisibilityUtils.getObjectGroup(map);
            EntityVisibility entityVisibility = getEntityVisibility();
            GeneralVisibilityUtils.limitVisibilityUpdate(entityVisibility, reportDefinition, reportGroup);

            User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
            try {
                List<ReportLogInfo> reportLogInfoList = ReportLogMongoService.getInstance()
                        .listIndexPendingAndInProcess(reportDefinition.getId(), currentUser);
                if (!reportLogInfoList.isEmpty()) {
                    return RestUtils.sendBadResponse("You can not edit the report that has pending or in progress " +
                            "indexes.");
                }
            } catch (IOException ex) {
                return RestUtils.sendBadResponse(ex.getMessage());
            }

            //1. Validation
            ValidationBean validationBean = this.validate(map);
            if (validationBean.isError()) {
                return RestUtils.sendBadResponse(validationBean.getErrorDescription());
            }

            ReportDefinitionValidator rdv = new ReportDefinitionValidator(
                    reportGroup, map);

            validationBean = rdv.getValidationResponse();
            if (validationBean.isError()) {
                return RestUtils.sendBadResponse(validationBean.getErrorDescription());
            }

            List<Map<String, Object>> reportFilters = (List<Map<String, Object>>) map.get("reportFilter");
            List<Map<String, Object>> reportCustomFilters = (List<Map<String, Object>>) map.get("reportCustomFilter");
            List<Map<String, Object>> reportProperties = (List<Map<String, Object>>) map.get("reportProperty");
            List<Map<String, Object>> reportGroupBys = (List<Map<String, Object>>) map.get("reportGroupBy");
            List<Map<String, Object>> reportRules = (List<Map<String, Object>>) map.get("reportRules");
            List<Map<String, Object>> reportEntryOptionBy = (List<Map<String, Object>>) map.get("reportEntryOption");
            List<Map<String, Object>> reportDefinitionConfig = (List<Map<String, Object>>) map.get("reportDefinitionConfig");
            List<Map<String, Object>> reportAction = (List<Map<String, Object>>) map.get("reportActions");

            map.remove("reportFilter");
            map.remove("reportCustomFilter");
            map.remove("reportProperty");
            map.remove("reportGroupBy");
            map.remove("reportRules");
            map.remove("groupTypeFloor");
            map.remove("reportEntryOption");
            map.remove("reportDefinitionConfig");
            map.remove("reportActions");

            BeanUtils.setProperties(map, reportDefinition);

            reportDefinition.setCenterLat(reportDefinition.getDoubleFromString(map.get("centerLat")));
            reportDefinition.setCenterLon(reportDefinition.getDoubleFromString(map.get("centerLon")));
            Long typeOrder = ReportDefinitionService.getInstance().sortReportType(reportDefinition.getReportType());
            reportDefinition.setTypeOrder(typeOrder);

            validateUpdate(reportDefinition);

            //Report Definition Config
            ReportDefinitionConfigService.getInstance().updateReportDefinitionConfig(
                    reportDefinition, reportDefinitionConfig);
            if (reportDefinition.getReportType().compareTo("mongo") == 0) {
                ReportDefinitionConfig repDefConfScript = reportDefinition.getReportDefConfigItem("SCRIPT");
                try {
                    MongoScriptDAO.getInstance().update(reportDefinition.getId().toString(),
                            repDefConfScript.getKeyValue());
                } catch (MongoExecutionException e) {
                    throw new UserException(e.getMessage(), e);
                }
            }

            ValidationBean validationBeanReportDefinition = this.validateReportDefinition(reportDefinition);
            if (validationBeanReportDefinition.isError()) {
                return RestUtils.sendBadResponse(validationBeanReportDefinition.getErrorDescription());
            }

            RecentService.getInstance().updateName(reportDefinition.getId(), reportDefinition.getName(), "report");

            //ReportFilter update
            List<ReportFilter> reportFilterList = new LinkedList<>();
            List<ReportFilter> reportFilterListOld = reportDefinition.getReportFilter();
            reportDefinition.setReportFilter(reportFilterList);

            // Deleting report Filters
            for (ReportFilter reportFilter : reportFilterListOld) {
                Boolean deleteFilter = true;
                for (Map<String, Object> reportFilterMap : reportFilters) {
                    if (reportFilterMap.containsKey("id")) {
                        if (reportFilter.getId().equals(Long.valueOf(reportFilterMap.get("id").toString()))) {
                            deleteFilter = false;
                            break;
                        }
                    }
                }
                if (deleteFilter) {
                    if (reportFilter != null) {
                        ReportFilterService.getInstance().delete(reportFilter);
                    }
                }
            }
            // Inserting or updating report filters
            for (Map<String, Object> reportFilterMap : reportFilters) {
                // if report filter exists (has id) then update
                if (reportFilterMap.containsKey("id")) {
                    Long reportFilterId = StringUtils.isEmpty(reportFilterMap.get("id").toString()) ? 0L : Long
                            .parseLong(reportFilterMap.get("id").toString());
                    logger.info("Updating report filter: " + reportFilterId);
                    ReportFilter reportFilter = ReportFilterService.getInstance().get(reportFilterId);
                    if (reportFilter != null) {
                        ReportFilterService.getInstance().setProperties(reportFilter, reportFilterMap,
                                reportDefinition);
                        ReportFilterService.getInstance().update(reportFilter);
                        reportFilterList.add(reportFilter);
                    }
                }
                // otherwise asume it is a new report filter and then insert.
                else {
                    ReportFilter reportFilter = new ReportFilter();
                    ReportFilterService.getInstance().setProperties(reportFilter, reportFilterMap, reportDefinition);
                    ReportFilterService.getInstance().insert(reportFilter);
                    reportFilterList.add(reportFilter);
                }
            }

            //ReportCustomFilter update
            List<ReportCustomFilter> reportCustomFilterList = new LinkedList<>();
            List<ReportCustomFilter> reportCustomFilterListOld = reportDefinition.getReportCustomFilter();
            reportDefinition.setReportCustomFilter(reportCustomFilterList);

            // Deleting report Filters
            for (ReportCustomFilter reportCustomFilter : reportCustomFilterListOld) {
                Boolean deleteFilter = true;
                for (Map<String, Object> reportCustomFilterMap : reportCustomFilters) {
                    if (reportCustomFilterMap.containsKey("id")) {
                        if (reportCustomFilter.getId().equals(Long.valueOf(reportCustomFilterMap.get("id").toString()))) {
                            deleteFilter = false;
                            break;
                        }
                    }
                }
                if (deleteFilter) {
                    if (reportCustomFilter != null) {
                        ReportCustomFilterService.getInstance().delete(reportCustomFilter);
                    }
                }
            }
            // Inserting or updating report custom filters
            for (Map<String, Object> reportCustomFilterMap : reportCustomFilters) {
                // if report filter exists (has id) then update
                if (reportCustomFilterMap.containsKey("id")) {
                    Long reportCustomFilterId = StringUtils.isEmpty(reportCustomFilterMap.get("id").toString()) ? 0L : Long
                            .parseLong(reportCustomFilterMap.get("id").toString());
                    logger.info("Updating report custom filter: " + reportCustomFilterId);
                    ReportCustomFilter reportCustomFilter = ReportCustomFilterService.getInstance().get(reportCustomFilterId);
                    if (reportCustomFilter != null) {
                        ReportCustomFilterService.getInstance().setProperties(reportCustomFilter, reportCustomFilterMap,
                                reportDefinition);
                        ReportCustomFilterService.getInstance().update(reportCustomFilter);
                        reportCustomFilterList.add(reportCustomFilter);
                    }
                }
                // otherwise asume it is a new report filter and then insert.
                else {
                    ReportCustomFilter reportCustomFilter = new ReportCustomFilter();
                    ReportCustomFilterService.getInstance().setProperties(reportCustomFilter, reportCustomFilterMap, reportDefinition);
                    ReportCustomFilterService.getInstance().insert(reportCustomFilter);
                    reportCustomFilterList.add(reportCustomFilter);
                }
            }


            //reportProperties update
            List<ReportProperty> reportPropertyList = new LinkedList<>();
            List<ReportProperty> reportPropertyListOld = reportDefinition.getReportProperty();
            reportDefinition.setReportProperty(reportPropertyList);

            for (ReportProperty reportProperty : reportPropertyListOld) {
                ReportPropertyService.getInstance().delete(reportProperty);
            }

            Set<String> propertyLabels = new HashSet<>();
            for (Map<String, Object> reportPropertyMap : reportProperties) {
                ReportProperty reportProperty = new ReportProperty();
                ReportPropertyService.getInstance().setProperties(reportProperty, reportPropertyMap, reportDefinition);
                if (!propertyLabels.contains(reportProperty.getLabel())) {
                    ReportPropertyService.getInstance().insert(reportProperty);
                    reportPropertyList.add(reportProperty);
                    propertyLabels.add(reportProperty.getLabel());
                }
            }
            reportDefinition.setReportProperty(reportPropertyList);

            //ReportRules update
            List<ReportRule> reportRulesList = new LinkedList<>();
            List<ReportRule> reportRulesListOld = reportDefinition.getReportRule();
            reportDefinition.setReportRule(reportRulesList);

            for (ReportRule reportRule : reportRulesListOld) {
                ReportRuleService.getInstance().delete(reportRule);
            }
            for (Map<String, Object> reportRulesMap : reportRules) {
                ReportRule reportRule = new ReportRule();
                ReportRuleService.getInstance().setProperties(reportRule, reportRulesMap, reportDefinition);
                ReportRuleService.getInstance().insert(reportRule);
                reportRulesList.add(reportRule);
            }
            reportDefinition.setReportRule(reportRulesList);


            //reportGroupBy update
            List<ReportGroupBy> reportGroupList = new LinkedList<>();
            List<ReportGroupBy> reportGroupListOld = reportDefinition.getReportGroupBy();
            reportDefinition.setReportGroupBy(reportGroupList);

            for (ReportGroupBy reportGroupBy : reportGroupListOld) {
                ReportGroupByService.getInstance().delete(reportGroupBy);
            }
            for (Map<String, Object> reportGroupByMap : reportGroupBys) {
                Long thingTypeIdReport = null;
                Long thingTypeFieldId = null;
                Long parentThingTypeFieldId = null;

                ReportGroupBy reportGroupBy = new ReportGroupBy();

                reportGroupBy.setPropertyName((String) reportGroupByMap.get("propertyName"));
                reportGroupBy.setLabel((String) reportGroupByMap.get("label"));
                reportGroupBy.setSortBy((String) reportGroupByMap.get("sortBy"));
                reportGroupBy.setRanking(reportDefinition.getDisplayOrder(reportGroupByMap.get("ranking")));

                if (reportGroupByMap.get("thingTypeId") != null) {
                    thingTypeIdReport = ((Number) reportGroupByMap.get("thingTypeId")).longValue();
                    reportGroupBy.setThingType(ThingTypeService.getInstance().get(thingTypeIdReport));
                }

                if (reportGroupByMap.get("thingTypeFieldId") != null && !reportGroupByMap.get("thingTypeFieldId")
                        .toString().equals("")
                        && StringUtils.isNumeric(reportGroupByMap.get("thingTypeFieldId").toString())) {
                    thingTypeFieldId = ((Number) reportGroupByMap.get("thingTypeFieldId")).longValue();
                    reportGroupBy.setThingTypeField(ThingTypeFieldService.getInstance().get(thingTypeFieldId));
                }

                if (reportGroupByMap.get("parentThingTypeId") != null && !reportGroupByMap.get("parentThingTypeId")
                        .toString().equals("")) {
                    parentThingTypeFieldId = ((Number) reportGroupByMap.get("parentThingTypeId")).longValue();
                    reportGroupBy.setParentThingType(ThingTypeService.getInstance().get(parentThingTypeFieldId));
                }

                reportGroupBy.setOther((Boolean) reportGroupByMap.get("other"));
                if (reportGroupByMap.get("unit") != null && !((String) reportGroupByMap.get("unit")).trim().isEmpty()) {
                    reportGroupBy.setUnit((String) reportGroupByMap.get("unit"));
                }
                reportGroupBy.setReportDefinition(reportDefinition);
                reportGroupBy.setByPartition(reportGroupByMap.get("byPartition") != null && (boolean)
                        reportGroupByMap.get("byPartition"));

                ReportGroupByService.getInstance().insert(reportGroupBy);
                reportGroupList.add(reportGroupBy);
            }
            reportDefinition.setReportGroupBy(reportGroupList);

            //Update ReportEntry Options
            updateReportEntryOption(reportDefinition, reportEntryOptionBy);

            // Update ReportAction
            if (reportAction != null) {
                saveActionConfiguration(reportDefinition, reportAction, false);
            }


            if (map.containsKey("groupTypeFloor.id")) {
                Object groupTypeId = map.get("groupTypeFloor.id");
                GroupType groupType = null;
                if (groupTypeId != null && groupTypeId instanceof Number) {
                    groupType = GroupTypeService.getInstance().get(((Number) groupTypeId).longValue());
                    //it is useless to share to same groupType as the groupType of the Report as by default is
                    // already visible to it
                    if (groupType != null && groupType.getId().equals(reportDefinition.getGroup().getGroupType()
                            .getId())) {
                        groupType = null;
                    }
                }
                reportDefinition.setGroupTypeFloor(groupType);
            }

            if (map.containsKey("roleShare.id")) {
                Object roleId = map.get("roleShare.id");
                Role role = null;
                if (roleId != null && roleId instanceof Number) {
                    role = RoleService.getInstance().get(((Number) roleId).longValue());
                }
                reportDefinition.setRoleShare(role);
            }

            if (map.containsKey("groupShare.id")) {
                Object groupId = map.get("groupShare.id");
                Group group = null;
                if (groupId != null && groupId instanceof Number) {
                    group = GroupService.getInstance().get(((Number) groupId).longValue());
                }
                reportDefinition.setGroupShare(group);
            }

            if (map.containsKey("shift.id")) {
                Object shiftId = map.get("shift.id");
                Shift shift = null;
                if (shiftId != null && shiftId instanceof Number) {
                    shift = ShiftService.getInstance().get(((Number) shiftId).longValue());
                }
                reportDefinition.setShift(shift);
            }

            if (map.containsKey("createdByUser.id")) {
                Object userId = map.get("createdByUser.id");
                User user = null;
                if (userId != null && userId instanceof Number) {
                    user = UserService.getInstance().get(((Number) userId).longValue());
                }
                reportDefinition.setCreatedByUser(user);
            }
            reportDefinition = validationEmailRecipients(map, reportDefinition);
            ReportDefinitionService.getInstance().update(reportDefinition);

            publicMap = reportDefinition.publicMap();
            SentEmailReportJob.reschedule(reportDefinition.getId());

            try {
                PickListFieldsService.getInstance().updatePickListWithFilters(reportDefinition);
            } catch (Exception e) {
                return RestUtils.sendOkResponse(publicMap);
            }
        } catch (UserException e) {
            logger.error(e.getMessage());
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }

        return RestUtils.sendOkResponse(publicMap);

    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(value = {"reportDefinition:d:{id}"})
    // 1a. Limit access based on CLASS level resources
    @ApiOperation(position = 5, value = "Delete a ReportDefinition (AUTO)")
    @Override
    public Response deleteReportDefinition(@PathParam("id") Long id) {
        // 1c. TODO: Restrict access based on OBJECT level read permissions
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
        if (reportDefinition == null) {
            return RestUtils.sendBadResponse(String.format("ReportDefinitionId[%d] not found", id));
        }
        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        try {
            List<ReportLogInfo> reportLogInfoList = ReportLogMongoService.getInstance()
                    .listIndexPendingAndInProcess(reportDefinition.getId(), currentUser);
            if (!reportLogInfoList.isEmpty()) {
                return RestUtils.sendBadResponse("You can not delete the report that has pending or in progress indexes.");
            }
        } catch (IOException ex) {
            return RestUtils.sendBadResponse(ex.getMessage());
        }
        RecentService.getInstance().deleteRecent(id, "report");
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, reportDefinition);
        // handle validation in an Extensible manner
        validateDelete(reportDefinition);
        ReportDefinitionService.getInstance().delete(reportDefinition);
        return RestUtils.sendDeleteResponse();
    }

    @PATCH
    @Path("/reportLog/{reportLogId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 6, value = "Cancel Report Log")
    public Response updateReportLog(@PathParam("reportLogId") String reportLogId,
                                    @DefaultValue("true") @QueryParam("enableQuery") Boolean enableQuery) {
        try {
            ReportLogInfo reportLogInfo = ReportLogMongoService.getInstance().searchReportLogForCancel(reportLogId);
            if (enableQuery) {
                return RestUtils.sendOkResponse(ReportLogMongoService.getInstance().getByIndexName(reportLogInfo
                        .getIndexInformation().getIndexName(), reportLogId), false);
            }
            IndexCreatorManager.getInstance().cancelIndexCreation(
                    reportLogInfo.getName(),
                    reportLogInfo.getCollectionName(),
                    reportLogInfo.getIndexInformation().getIndexName());
            return RestUtils.sendOkResponse("Index successfully canceled.");
        } catch (Exception e) {
            logger.error(e);
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
    }

    @PATCH
    @Path("/reportLog/bulkUpdate")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 7, value = "Cancel Report Log Bulk")
    public Response updateReportLog(List<String> reportLogIds,
                                    @DefaultValue("true") @QueryParam("enableQuery") Boolean enableQuery) {
        if (enableQuery) {
            List<List<Map<String, Object>>> response = reportLogIds.stream().map(rli -> {
                try {
                    ReportLogInfo reportLogInfo = ReportLogMongoService.getInstance().searchReportLogForCancel(rli);
                    return ReportLogMongoService.getInstance().getByIndexName(reportLogInfo
                            .getIndexInformation().getIndexName(), rli);
                } catch (Exception e) {
                    logger.error("Error finding reportLod.id = " + rli, e);
                    List<Map<String, Object>> failed = new LinkedList<>();
                    Map<String, Object> resp = new HashMap<>();
                    resp.put(rli, "failed on search index");
                    failed.add(resp);
                    return failed;
                }
            }).collect(Collectors.toList());
            return RestUtils.sendOkResponse(response);
        } else {
            List<Map<String, Map<String, Integer>>> response = reportLogIds.stream().map(rli -> {
                Map<String, Map<String, Integer>> partialResponse = new HashMap<>();
                Map<String, Integer> message = new HashMap<>();
                try {
                    ReportLogInfo reportLogInfo = ReportLogMongoService.getInstance().searchReportLogForCancel(rli);
                    IndexCreatorManager.getInstance().cancelIndexCreation(
                            reportLogInfo.getName(),
                            reportLogInfo.getCollectionName(),
                            reportLogInfo.getIndexInformation().getIndexName());
                    message.put("Index successfully canceled.", 200);
                } catch (Exception e) {
                    logger.error("Error canceling reportLod.id = " + rli, e);
                    message.put(e.getMessage(), 400);
                }
                partialResponse.put(rli, message);
                return partialResponse;
            }).collect(Collectors.toList());
            return RestUtils.sendOkResponse(response);
        }
    }

    @GET
    @Path("/reportLog/{reportId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 8, value = "List In Progress Index of Report Log")
    public Response listIndexReportLog(@PathParam("reportId") Long reportId) {
        try {
            User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
            ListReportLog listReportLog = ReportLogMongoService.getInstance().listIndexStatusReportLog(reportId, currentUser);
            return RestUtils.sendOkResponse(listReportLog.getMap());
        } catch (Exception e) {
            logger.error(e);
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
    }

    @GET
    @Path("/reportLog/currentlyIndexing/{reportId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 9, value = "List In Progress Index of Report Log")
    public Response isCurrentlyIndexing(@PathParam("reportId") Long reportId) {
        try {
            boolean result = ReportLogMongoService.getInstance().isCurrentlyIndexing(reportId);
            Map<String, Boolean> map = new HashMap<>();
            map.put("result", result);
            return RestUtils.sendOkResponse(map);
        } catch (Exception e) {
            logger.error(e);
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
    }



    @PATCH
    @Path("/reportLog/check/{reportLogId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 10, value = "Check the ReportLog if its status is COMPLETED or CANCELED")
    public Response updateReportLogAsChecked(@PathParam("reportLogId") String reportLogId) {
        try {
            ReportLogMongoService.getInstance().markAsChecked(reportLogId);
            return RestUtils.sendOkResponse("ReportLog was checked");
        } catch (IOException e) {
            logger.error(e);
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
    }

    /***************************************
     * Update reportDefinition EntryOptions
     ***************************************/
    public void updateReportEntryOption(ReportDefinition reportDefinition, List<Map<String, Object>>
            reportEntryOptionBy) {
        List<ReportEntryOption> reportEntryOptionList = new LinkedList<>();
        List<ReportEntryOption> reportEntryOptionListOld = reportDefinition.getReportEntryOption();

        if (reportEntryOptionBy != null) {
            Map<String, ReportEntryOption> old_ = new HashMap<>();
            Map<String, Object> new_ = new HashMap<>();
            for (ReportEntryOption reportEntryOption : reportEntryOptionListOld) {
                old_.put(reportEntryOption.getName(), reportEntryOption);
            }
            for (Map<String, Object> reportEntryOptionByMap : reportEntryOptionBy) {
                new_.put((String) reportEntryOptionByMap.get("name"), reportEntryOptionByMap);
            }
            Set<String> deleteSet = new HashSet<>(old_.keySet());
            deleteSet.removeAll(new_.keySet());

            for (Map<String, Object> reportEntryOptionByMap : reportEntryOptionBy) {
                ReportEntryOption reportEntryOption;
                boolean isNew = !old_.containsKey(reportEntryOptionByMap.get("name"));
                if (isNew) {
                    reportEntryOption = new ReportEntryOption();
                } else {
                    reportEntryOption = old_.get(reportEntryOptionByMap.get("name"));
                }
                reportEntryOption.setName((String) reportEntryOptionByMap.get("name"));
                reportEntryOption.setLabel((String) reportEntryOptionByMap.get("label"));
                if (reportEntryOptionByMap.get("displayOrder") != null) {
                    reportEntryOption.setDisplayOrder(Float.parseFloat(reportEntryOptionByMap.get("displayOrder")
                            .toString()));
                }
                reportEntryOption.setAssociate((Boolean) reportEntryOptionByMap.get("associate"));
                reportEntryOption.setDisassociate((Boolean) reportEntryOptionByMap.get("disassociate"));
                reportEntryOption.setNewOption((Boolean) reportEntryOptionByMap.get("newOption"));
                reportEntryOption.setEditOption((Boolean) reportEntryOptionByMap.get("editOption"));
                reportEntryOption.setDeleteOption((Boolean) reportEntryOptionByMap.get("deleteOption"));
                reportEntryOption.setRFIDPrint((Boolean) reportEntryOptionByMap.get("RFIDPrint"));
                reportEntryOption.setGroup(reportEntryOptionByMap.get("group") != null ?
                        GroupService.getInstance().get(Long.parseLong(reportEntryOptionByMap.get("group").toString())
                        ) : null);
                ReportDefinitionService.getInstance()
                        .validateAndAssociateThingType(reportEntryOption, reportEntryOptionByMap.get("thingTypeId"));
                reportEntryOption.setIsMobile(reportEntryOptionByMap.get("isMobile") != null ?
                        (Boolean) reportEntryOptionByMap.get("isMobile") : false);

                if (reportEntryOption.getRFIDPrint() && reportEntryOptionByMap.get("defaultRFIDPrint") != null &&
                        reportEntryOptionByMap.get("defaultZPLTemplate") != null) {
                    reportEntryOption.setDefaultRFIDPrint(Long.parseLong(reportEntryOptionByMap.get
                            ("defaultRFIDPrint").toString()));
                    reportEntryOption.setDefaultZPLTemplate(Long.parseLong(reportEntryOptionByMap.get
                            ("defaultZPLTemplate").toString()));
                }
                reportEntryOption.setReportDefinition(reportDefinition);
                if (isNew) {
                    reportEntryOption = ReportEntryOptionService.getInstance().insert(reportEntryOption);
                } else {
                    reportEntryOption = ReportEntryOptionService.getInstance().update(reportEntryOption);
                }

                //Report Entry Option Properties
                List<Map<String, Object>> reportEntryOptionPropertyBy = (List<Map<String, Object>>)
                        reportEntryOptionByMap.get("reportEntryOptionProperty");
                List<ReportEntryOptionProperty> reportEntryOptionPropByList = new LinkedList<>();
                List<ReportEntryOptionProperty> reportEntryOptionProperties = reportEntryOption
                        .getReportEntryOptionProperties();
                if (reportEntryOptionProperties != null) {
                    Iterator<ReportEntryOptionProperty> iterator = reportEntryOptionProperties.iterator();
                    while (iterator.hasNext()) {
                        ReportEntryOptionProperty reportEntryOptionProperty = iterator.next();
                        iterator.remove();
                        BooleanBuilder be = new BooleanBuilder();
                        be = be.and(QEntryFormPropertyData.entryFormPropertyData.reportEntryOptionProperty.eq(reportEntryOptionProperty));
                        EntryFormPropertyDataService.getEntryFormPropertyDataDAO().deleteAllBy(be);
                        reportEntryOptionProperty.setEntryFormPropertyDatas(null);
                        ReportEntryOptionPropertyService.getInstance().delete(reportEntryOptionProperty);
                    }
                }

                if (reportEntryOptionPropertyBy != null) {
                    for (Map<String, Object> reportEntryPropertyMap : reportEntryOptionPropertyBy) {
                        ReportEntryOptionProperty reportEntryProperty = new ReportEntryOptionProperty();
                        reportEntryProperty.setProperties(reportEntryPropertyMap, reportEntryOption);
                        validationReportEntryOptionProperty(reportEntryProperty, reportDefinition);
                        //Insert in entryFormPropertyData
                        if ((reportEntryProperty.getEntryFormPropertyDatas() != null) &&
                                (reportEntryProperty.getAllPropertyData())) {
                            reportEntryProperty.setEntryFormPropertyDatas(null);
                        }
                        ReportEntryOptionProperty reportEntryOptionProperty = ReportEntryOptionPropertyService.getInstance().insert(reportEntryProperty);
                        reportEntryOptionPropByList.add(reportEntryProperty);

                        //Insert in entryFormPropertyData
                        if ((reportEntryProperty.getEntryFormPropertyDatas() != null) &&
                                !(reportEntryProperty.getAllPropertyData())) {
                            for (EntryFormPropertyData data : reportEntryProperty.getEntryFormPropertyDatas()) {
                                data.setReportEntryOptionProperty(reportEntryOptionProperty);
                                EntryFormPropertyDataService.getInstance().insert(data);
                            }
                        }

                    }
                }
                reportEntryOption.setReportEntryOptionProperties(reportEntryOptionPropByList);
                reportEntryOptionList.add(reportEntryOption);
            }
            for (String delete : deleteSet) {
                ReportEntryOptionService.getInstance().delete(old_.get(delete));
            }
        }
        reportDefinition.setReportEntryOption(reportEntryOptionList);
    }

    /**
     * validateReportDefinition
     *
     * @param reportDefinition
     * @return a ValidationBean with the error message
     */
    public ValidationBean validateReportDefinition(ReportDefinition reportDefinition) {
        ValidationBean response = new ValidationBean();
        if (isValidReportNameLength(reportDefinition)) {
            response.setErrorDescription("Report Name's longitude should be less than 100 characters.");
        }
        if (!REPORT_TYPES.contains(reportDefinition.getReportType())) {
            response.setErrorDescription("No valid report type defined.");
        }
        return response;

    }

    /**
     * isValidDescriptionResourceLength
     *
     * @param reportDefinition
     * @return true if the resource's label attribute is more than 255 characters
     */
    public boolean isValidReportNameLength(ReportDefinition reportDefinition) {
        String reportName = reportDefinition.getName();
        return (reportName.length() > 100);
    }


    /*************************************
     * This method validate duplicated values in a map, just one level
     ***********************************/
    public boolean validateDuplicatedValuesMap(Map<String, Object> publicMap, String mapEntryData) {
        boolean response = false;
        List properties = (ArrayList) publicMap.get(mapEntryData);
        if (properties != null) {
            HashSet<String> popupPropertiesSet = new HashSet<>();
            for (Object property : properties) {
                Map<String, Object> data = (Map) property;
                popupPropertiesSet.add(data.get("label").toString().toLowerCase());
            }
            if (popupPropertiesSet.size() == properties.size()) {
                response = true;
            }
        }
        return response;
    }

    /*************************************
     * This method does all the validations for the element reportEntryOption
     * the map in Update
     ***********************************/
    public ValidationBean validateDuplicatedValuesEntryOption(Map<String, Object> publicMap, String mapToEvaluate) {
        ValidationBean response = new ValidationBean();
        List<Map<String, Object>> reportEntryOptionBy = (List<Map<String, Object>>) publicMap.get(mapToEvaluate);
        Map<String, Object> reportEntryOptionBizz = new HashMap<>();
        Map<String, Object> reportEntryOptionPropBizz;
        if (reportEntryOptionBy != null) {
            for (Map<String, Object> reportEntryOption : reportEntryOptionBy) {
                if (!validateDuplicatedValuesMap(reportEntryOption, "reportEntryOptionProperty")) {
                    response.setErrorDescription("Labels in properties section of Data Entry Options '" +
                            reportEntryOption.get("name") + "' should not have duplicated values.");
                    break;
                }
                //Eliminate children nodes in json Object and verify
                reportEntryOptionPropBizz = new HashMap<>();


                //TODO: Refactor this code, We need mode all validation to entity class. Victor Angel Chambi, 11/09/17
                Boolean newOption = Boolean.parseBoolean(reportEntryOption.get("newOption").toString());
                Long thingTypeId = 0L;
                List<Long> thingTypesTreeList = null;
                if (newOption && reportEntryOption.get("thingTypeId") != null) {
                    thingTypeId = Long.parseLong(reportEntryOption.get("thingTypeId").toString());
                    thingTypesTreeList = ThingTypeService.getInstance()
                            .getThingTypeIdsOfPathsByThingTypeId(thingTypeId);
                    if (thingTypesTreeList.isEmpty()) {
                        thingTypesTreeList.add(thingTypeId);
                    }
                }
                Set<String> entryOptionNames = new TreeSet<>();
                for (Object reportEntryOptionProperty : ((List) reportEntryOption.get("reportEntryOptionProperty"))) {
                    Map<String, Object> entryOptionProperty = (Map<String, Object>) reportEntryOptionProperty;
                    if (newOption) {
                        Long thingTypeDataEntryPropId = (entryOptionProperty.get("thingTypeId") != null) ?
                                Long.parseLong(entryOptionProperty.get("thingTypeId").toString()) : 0L;

                        String propertyName = (entryOptionProperty.get("propertyName") != null) ?
                                entryOptionProperty.get("propertyName").toString() : StringUtils.EMPTY;
                        entryOptionNames.add(propertyName);

                        if (!thingTypeDataEntryPropId.equals(0L) &&  !thingTypesTreeList.contains(thingTypeDataEntryPropId)) {
                            response.setErrorDescription("the " + entryOptionProperty.get("label") + " field of the " +
                                    "input data properties is not part of the tree of the selected thing type.");
                        }

                    }
                    Iterator it = entryOptionProperty.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry) it.next();
                        if (!(pair.getValue() instanceof List)) {
                            reportEntryOptionPropBizz.put(pair.getKey().toString(), pair.getValue());
                        }
                    }
                }
                if (newOption) {
                    //TODO: Change this Logic to another class, Victor Angel Chambi Nina, 09/12/2017.
                    ThingTypeService.getInstance().validateFormulaFields(thingTypeId, entryOptionNames);
                }
                //Create a clone of the reportEntryOption data
                Utilities.cloneHashMap(reportEntryOption, reportEntryOptionBizz);
                //Put in the json object the properties without children
                reportEntryOptionBizz.put("reportEntryOptionProperty", reportEntryOptionPropBizz);

            }
        }
        return response;
    }

    /****
     * This method get a list of elements in Zone Properties
     *****/
    public static ArrayList<String> getListPinPopUpProperties(String linkString) {
        ArrayList<String> result = new ArrayList<String>();
        //String[] data = linkString.split( "}" );
        String[] data = linkString.split("}");

        String value = null;
        for (int i = 0; i < data.length - 1; i++) {
            value = data[i];
            value = value.substring(value.indexOf("label="), value.length());
            value = value.substring(6, value.indexOf(",") == -1 ? value.length() : value.indexOf(","));
            result.add(value);
        }
        return result;
    }

    @GET
    @Path("/valuesByThingTypeField/{thingTypeFieldId}")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
    @ApiOperation(position = 11, value = "Select a ReportDefinition (AUTO)")
    public Response selectReportDefinitions(@PathParam("thingTypeFieldId") Long thingTypeFieldId, @QueryParam
            ("zoneProperty") String zonePropertyId, @QueryParam
                                                    ("thingTypeId") @DefaultValue("-1") Long thingTypeId) {
        //TODO check permissioning
        if (zonePropertyId != null && ReportRuleUtils.isNumeric(zonePropertyId)) {
            List<String> zonePropertyValuesBack = ZonePropertyValueService.getInstance().getZonePropertyValue(Long
                    .valueOf(zonePropertyId));
            List<String> zonePropertyValues = Utilities.trimList(zonePropertyValuesBack);
            return RestUtils.sendOkResponse(new TreeSet<>(zonePropertyValues));
        }
        //Get values of thingTypeFieldId in the every instance of things
        ThingTypeField thingTypeField = ThingTypeFieldService.getInstance().get(thingTypeFieldId);
        DataType datatype = thingTypeField.getDataType();
        String fieldName = null;
        if (thingTypeField.getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value)) {
            datatype = thingTypeField.getDataType();
        }
        if (datatype != null) {
            if (thingTypeField.getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE
                    .value)
                    && datatype.getType().equals(ThingTypeField.TypeParentSubGroup.TYPE_PARENT_DATA_TYPE_STANDARD_DATA
                    .value)) {
                fieldName = thingTypeField.getName() + ".value";
            } else if (thingTypeField.getTypeParent().equals(ThingTypeField.TypeParent
                    .TYPE_PARENT_DATA_TYPE.value)
                    && datatype.getType().equals(ThingTypeField.TypeParentSubGroup.TYPE_PARENT_DATA_TYPE_NATIVE_OBJECT
                    .value)) {
                fieldName = thingTypeField.getName() + ".value.name";
            } else if (thingTypeField.getTypeParent().equals(ThingTypeField.TypeParent
                    .TYPE_PARENT_DATA_TYPE.value)
                    && datatype.getType().equals(ThingTypeField.TypeParentSubGroup.TYPE_PARENT_DATA_TYPE_NATIVE_OBJECT
                    .value)) {
                fieldName = thingTypeField.getName() + ".serialNumber";
            }
        }
        List<Object> fieldValues;
        if (thingTypeId != 0) {
            fieldValues = ThingMongoDAO.getInstance().getFieldDistinctValues(
                    fieldName,
                    thingTypeField.getName() + ".thingTypeFieldId",
                    thingTypeField.getThingType().getId(),
                    thingTypeField.getId());
        } else {
            fieldValues = ThingMongoDAO.getInstance().getFieldDistinctValues(
                    fieldName);
        }

        String pickList = PickListFieldsService.getInstance().getValueByThingFieldId(thingTypeFieldId);
        String[] pickListSplit = pickList.split(",");
        Set<String> pickSet = new LinkedHashSet<>();
        for (int it = 0; it < pickListSplit.length; it++) {
            if (pickListSplit[it] != null && pickListSplit[it].length() > 0) {
                pickSet.add((pickListSplit[it]).trim());
            }
        }
        Object pickArrayObj[] = pickSet.toArray();
        String pickArray[] = new String[pickArrayObj.length];
        for (int it = 0; it < pickArrayObj.length; it++) {
            pickArray[it] = pickArrayObj[it].toString();
        }

        List<String> pickArrayRes = new ArrayList<>(Arrays.asList(pickArray));
        fieldValues.addAll(pickArrayRes);
        List<String> strings = new ArrayList<>();
        for (Object object : fieldValues) {
            String newObject = object != null ? object.toString() : null;
            if (newObject != null) {
                strings.add(newObject);
            }
        }
        return RestUtils.sendOkResponse(new TreeSet<>(strings));
    }


    @GET
    @Path("/thingType/tree")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 12, value = "Get a List of Thing Types in Tree")
    public Response listThingTypesInTree(
            @QueryParam("pageSize") Integer pageSize,
            @QueryParam("pageNumber") Integer pageNumber,
            @QueryParam("order") String order,
            @QueryParam("where") String where,
            @Deprecated @QueryParam("extra") String extra,
            @Deprecated @QueryParam("only") String only,
            @QueryParam("visibilityGroupId") Long visibilityGroupId,
            @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
            @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
            @QueryParam("topId") String topId,
            @DefaultValue("false") @QueryParam("enableMultilevel") String enableMultilevel) {

        //TODO XXX FIXME: Change this logic to a service call, Victor Angel Chambi Nina, 04/09/2017.
        validateListPermissions();
        ThingTypeController tyc = new ThingTypeController();
        return tyc.listThingTypesInTree2(pageSize, pageNumber, order, where, extra, only, visibilityGroupId,
                upVisibility, downVisibility,
                "true", enableMultilevel, false);
    }


    @GET
    @Path("/thingType/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 13, value = "Get a List of ThingTypes.")
    public Response listThingTypes(@QueryParam("pageSize") Integer pageSize,
                                   @QueryParam("pageNumber") Integer pageNumber,
                                   @QueryParam("order") String order, @QueryParam("where") String where, @QueryParam
                                           ("extra") String extra, @Deprecated @QueryParam("only") String only,
                                   @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("")
                                   @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam
            ("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project) {
        validateListPermissions();
        ThingTypeController tyc = new ThingTypeController();
        return tyc.listThingTypes(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility,
                downVisibility, extend, project);
    }

    @GET
    @Path("/parameters/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 14, value = "Get a List of Parameters")
    public Response listParameters(@QueryParam("pageSize") Integer pageSize,
                                   @QueryParam("pageNumber") Integer pageNumber,
                                   @QueryParam("order") String order,
                                   @QueryParam("where") String where,
                                   @QueryParam("extra") String extra,
                                   @QueryParam("only") String only,
                                   @QueryParam("visibilityGroupId") Long visibilityGroupId,
                                   @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
                                   @DefaultValue("") @QueryParam("downVisibility") String downVisibility) {
        validateListPermissions();
        ParametersController controller = new ParametersController();
        return controller.listParameterss(pageSize, pageNumber, order, where,
                extra, only, visibilityGroupId, upVisibility, downVisibility, null, null);
    }

    /**
     * FILTER LIST
     */

    @GET
    @Path("/thing/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 15, value = "Get a List of Things")
    public Response listThings(@QueryParam("pageSize") Integer pageSize,
                               @QueryParam("pageNumber") Integer pageNumber,
                               @QueryParam("order") String order,
                               @QueryParam("where") String where,
                               @QueryParam("extra") String extra,
                               @Deprecated @QueryParam("only") String only,
                               @QueryParam("visibilityGroupId") Long visibilityGroupId,
                               @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
                               @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
                               @QueryParam("topId") String topId,
                               @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend,
                               @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project) {
        //TODO XXX FIXME: Change this logic to a service call, Victor Angel Chambi Nina, 04/09/2017.
        validateListPermissions();
        ThingController c = new ThingController();
        return c.listThings(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility,
                downVisibility, extend, project);
    }

    @GET
    @Path("/thing/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 16, value = "Get a Thing")
    public Response selectThings(@PathParam("id") Long id,
                                 @Deprecated @QueryParam("extra") String extra,
                                 @Deprecated @QueryParam("only") String only,
                                 @ApiParam(value = "Extends nested properties")
                                     @QueryParam("extend") String extend,
                                 @ApiParam(value = "Projects only nested properties")
                                     @QueryParam("project") String project) {
        validateListPermissions();
        ThingController c = new ThingController();
        return c.selectThings(id, extra, only, extend, project, false);
    }

    @PATCH
    @Path("/thing/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 17, value = "update Thing")
    public Response updateThing(@PathParam("id") Long thingId, Map<String, Object> thingMap) {
        ThingController c = new ThingController();
        return c.updateThing(thingId, thingMap);
    }


    public Response listReportDefinitionsInTree(Integer pageSize, Integer pageNumber, String order, String where,
                                                String extra, String only, Long visibilityGroupId, String
                                                        upVisibility, String downVisibility, String topId, String extend, String project) {
        throw new RuntimeException("Not Implemented");
    }

    //10.100.1.195:8080/riot-core-services/api/zoneGroup?ts=1418324888227

    @GET
    @Path("/zoneGroup/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 18, value = "Get a List of ZoneGroups (AUTO)")
    public Response listZoneGroups(@QueryParam("pageSize") Integer pageSize,
                                   @QueryParam("pageNumber") Integer pageNumber,
                                   @QueryParam("order") String order, @QueryParam("where") String where, @QueryParam
                                           ("extra") String extra, @Deprecated @QueryParam("only") String only,
                                   @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("")
                                   @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam
            ("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project) {
        validateListPermissions();
        ZoneGroupController zgc = new ZoneGroupController();
        return zgc.listZoneGroups(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility,
                downVisibility, false, extend, project);
    }

    @GET
    @Path("/zone/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 19, value = "Get a List of Zones (AUTO)")
    public Response listZone(@QueryParam("pageSize") Integer pageSize,
                             @QueryParam("pageNumber") Integer pageNumber,
                             @QueryParam("order") String order, @QueryParam("where") String where, @QueryParam
                                     ("extra") String extra, @Deprecated @QueryParam("only") String only,
                             @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam
            ("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String
                                     downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project) {
        validateListPermissions();
        ZoneController zc = new ZoneController();
        return zc.listZones(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility,
                downVisibility, false, extend, project);
    }

    @GET
    @Path("/zone/geojson")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 20, value = "Get a List of Zones in GeoJson")
    public Response listZonesGeoJson(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer
            pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @QueryParam
                                             ("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String
                                             upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility) {
        validateListPermissions();
        ZoneController zc = new ZoneController();
        return zc.listZonesGeoJson(pageSize, pageNumber, order, where, visibilityGroupId, upVisibility, downVisibility);
    }

    @GET
    @Path("/zoneType/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 21, value = "Get a List of Zone Types")
    public Response listZoneTypes(@QueryParam("pageSize") Integer pageSize,
                                  @QueryParam("pageNumber") Integer pageNumber,
                                  @QueryParam("order") String order, @QueryParam("where") String where, @QueryParam
                                          ("extra") String extra, @Deprecated @QueryParam("only") String only,
                                  @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("")
                                  @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam
            ("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project) {
        validateListPermissions();
        ZoneTypeController zc = new ZoneTypeController();
        return zc.listZoneTypes(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility,
                downVisibility, false, extend, project);
    }


    @GET
    @Path("/localMap/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 22, value = "Get a List of Local Maps (AUTO)")
    public Response listLocalMaps(@QueryParam("pageSize") Integer pageSize,
                                  @QueryParam("pageNumber") Integer pageNumber,
                                  @QueryParam("order") String order, @QueryParam("where") String where, @QueryParam
                                          ("extra") String extra, @Deprecated @QueryParam("only") String only,
                                  @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("")
                                  @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam
            ("downVisibility") String downVisibility, @QueryParam("topId") String topId, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project) {
        validateListPermissions();
        LocalMapController lm = new LocalMapController();
        return lm.listLocalMaps(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility,
                downVisibility, false, extend, project);
    }

    @GET
    @Path("/thing/fields/trueFlags")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 23, value = "Get a List of Field True Flags")
    public Response listTrueFlagsThingsFields() {
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Thing.class.getCanonicalName(), null);
        validateListPermissions();
        Long total = 0L;
        // add configurations map
        User rootUser = UserService.getInstance().getRootUser();
        Field stopAlerts = FieldService.getInstance().selectByName("alert");
        Field alertPollFrequency = FieldService.getInstance().selectByName("alertPollFrequency");
        GroupField stopAlertsField = GroupFieldService.getInstance().selectByGroupField(rootUser.getActiveGroup(),
                stopAlerts);
        GroupField alertPollFrequencyField = GroupFieldService.getInstance().selectByGroupField(rootUser
                .getActiveGroup(), alertPollFrequency);
        Map<String, Object> configurations = new HashMap<>();
        if (stopAlertsField != null && alertPollFrequencyField != null) {
            configurations.put(stopAlertsField.getField().getName(), stopAlertsField.publicMap(true));
            configurations.put(alertPollFrequencyField.getField().getName(), alertPollFrequencyField.publicMap(true));
            total = ThingService.getInstance().countTrueFlags(visibilityGroup);
        }
        Map<String, Object> mapResponse = new HashMap<>();
        mapResponse.put("configurations", configurations);
        mapResponse.put("total", total);
        return RestUtils.sendOkResponse(mapResponse);
    }

    /*Get List of logical readers with permissions of report definition and group of report definicion*/
    @GET
    @Path("/logicalReaders/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 24, value = "Get a List of LogicalReaders (AUTO)")
    public Response getListLogicalReaders(@QueryParam("pageSize") Integer pageSize
            , @QueryParam("pageNumber") Integer pageNumber
            , @QueryParam("order") String order
            , @QueryParam("where") String where
            , @Deprecated @QueryParam("extra") String extra
            , @Deprecated @QueryParam("only") String only
            , @QueryParam("visibilityGroupId") Long visibilityGroupId
            , @DefaultValue("") @QueryParam("upVisibility") String upVisibility
            , @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project) {
        validateListPermissions();
        LogicalReaderController logicalReaderController = new LogicalReaderController();
        return logicalReaderController.listLogicalReaders(pageSize, pageNumber, order, where, extra, only,
                visibilityGroupId, upVisibility,
                downVisibility, false, extend, project);
    }

    @GET
    @Path("/things/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 25, value = "Get a List of Things (AUTO)")
    public Response listThings(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
                               @QueryParam("order") String order, @QueryParam("where") String where, @QueryParam
                                       ("extra") String extra,
                               @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId,
                               @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
                               @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project) {
        validateListPermissions();
        ThingController thingController = new ThingController();
        return thingController.listThings(pageSize, pageNumber, order, where, extra, only, visibilityGroupId,
                upVisibility, downVisibility, extend, project);
    }

    @Override
    public void validateListPermissions() {
        if (!(PermissionsUtils.isPermitted(SecurityUtils.getSubject(), "reportDefinition:r") || RiotShiroRealm
                .getObjectPermissionsCache().containsKey("reportDefinition"))) {
            throw new com.tierconnect.riot.sdk.servlet.exception.ForbiddenException("Not Authorized, Access Denied");
        }
    }

    /*
    * Custom add to public map
    * */
    @SuppressWarnings("unchecked")
    public void addToPublicMap(ReportDefinition reportDefinition, Map<String, Object> publicMap, String extra) {
        /* adding actions in report definition */
        List<ReportActions> actionsList = ReportActionsService.getInstance().getReportActionsActives(reportDefinition.getId());
        List<Map<String, Object>> reportActionList = new LinkedList<>();
        for (ReportActions actions : actionsList) {
            Map<String, Object> objectMap = actions.publicMap();
            Map<String, Object> objectConfiguration = (Map<String, Object>) objectMap.get("configuration");
            /* remove password if contains Basic authentication */
            if (objectConfiguration != null) {
                Map<String, Object> basicAuth = (Map<String, Object>) objectConfiguration.get(EXECUTION_TYPE_BASIC_AUTH.value);
                if (basicAuth != null) {
                    basicAuth.put(EXECUTION_TYPE_BASIC_AUTH_PASSWORD.value, "");
                }
                objectConfiguration.put(EXECUTION_TYPE_BASIC_AUTH.value, basicAuth);
            }
            reportActionList.add(objectMap);
        }
        publicMap.put("reportActions", reportActionList);

         /*Adding a default value in report Entry options*/
        if (publicMap.get("reportEntryOption") != null) {
            for (Object reportEntryOption : (List) publicMap.get("reportEntryOption")) {
                Map<String, Object> reportEntryOptionMap = (Map<String, Object>) reportEntryOption;
                ReportEntryOption reportentryOption = ReportEntryOptionService.getInstance().get(Long.parseLong
                        (reportEntryOptionMap.get("id").toString()));
                if (reportentryOption.getGroup() != null) {
                    reportEntryOptionMap.put("group", reportentryOption.getGroup().getId());
                    reportEntryOptionMap.put("group.hierarchyName", reportentryOption.getGroup().getHierarchyName());
                }

                if (reportentryOption.getThingType() != null) {
                    reportEntryOptionMap.put("thingTypeId", reportentryOption.getThingType().getId());
                }

                List<ReportEntryOptionProperty> lstRepEntryOptProp = reportentryOption.getReportEntryOptionProperties();
                List<Map<String, Object>> lstMapEntryProp = new ArrayList<>();
                if (lstRepEntryOptProp != null) {
                    for (ReportEntryOptionProperty repPropertyMap : lstRepEntryOptProp) {
                        Map<String, Object> maps = repPropertyMap.publicMap();
                        List<EntryFormPropertyData> lstPropertyData = repPropertyMap.getEntryFormPropertyDatas();
                        if ((lstPropertyData != null) && (!lstPropertyData.isEmpty())) {
                            List<Map<String, Object>> mapProperty = new ArrayList<>();
                            for (EntryFormPropertyData entry : lstPropertyData) {
                                mapProperty.add(entry.publicMap());
                            }
                            maps.put("entryFormPropertyData", mapProperty);
                        }
                        if (maps.get("thingTypeIdReport") == null) {
                            maps.put("thingTypeIdReport", 0);
                        }
                        if (maps.get("thingTypeFieldId") != null) {
                            Long thingTypeFieldId = (Long) maps.get("thingTypeFieldId");
                            ThingTypeField thingTypeField = ThingTypeFieldService.getInstance().get(thingTypeFieldId);
                            if (thingTypeField != null) {
                                maps.put("defaultValue", thingTypeField.getDefaultValue());
                                maps.put("propertyName", thingTypeField.getName());

                                // Checking if allPropertyData is true to fill allData in pickList
                                if (repPropertyMap.getAllPropertyData() &&
                                        thingTypeField.getDataType().getId().equals(ThingTypeField.Type.TYPE_THING_TYPE.value)) {
                                    Subject subject = SecurityUtils.getSubject();
                                    User currentUser = (User) subject.getPrincipal();
                                    List<Thing> things = ThingService.getInstance().selectByThingTypeAndGroup(
                                            thingTypeField.getDataTypeThingTypeId(), currentUser.getGroup().getId());
                                    if (null != things) {
                                        List<Map<String, Object>> entryFormPropertyData = new ArrayList<>();
                                        for (Thing thing : things) {
                                            Map<String, Object> map = new HashMap<>();
                                            map.put("id", thing.getId());
                                            map.put("value", thing.getSerial());
                                            map.put("name", thing.getName());
                                            entryFormPropertyData.add(map);
                                        }
                                        maps.put("entryFormPropertyData", entryFormPropertyData);
                                    }
                                }
                            }
                        }
                        lstMapEntryProp.add(maps);
                        reportEntryOptionMap.put("reportEntryOptionProperty", lstMapEntryProp);
                    }
                }
            }
        }

    }

    /**
     * @param reportEntryProperty
     */
    public static void validationReportEntryOptionProperty(ReportEntryOptionProperty reportEntryProperty,
                                                           ReportDefinition reportDefinition) {
        ValidationBean validationBeanDataType = ReportDefinitionService.getInstance().
                validationDataTypeInReportEntryOptionProperty(reportEntryProperty, reportDefinition);
        if ((validationBeanDataType != null) && (validationBeanDataType.isError())) {
            String dataEntryName = reportEntryProperty.getReportEntryOption().getName();
            throw new UserException("Default Mobile Value in Data Entry Action " + dataEntryName + ": " +
                    validationBeanDataType.getErrorDescription());
        }
    }

    public boolean isNotEmptyDataInMap(String string, Map<String, Object> map) {
        return ((map.containsKey(string)) && (map.get(string) != null) && map.get(string).toString().length() > 2);
    }

    public ReportDefinition validationEmailRecipients(Map<String, Object> map, ReportDefinition reportDefinition) {
        boolean isEmptySchedule = true;
        boolean isEmptyEmailRunAs = true;
        boolean isEmptyEmail = true;
        if (isNotEmptyDataInMap("schedule", map)) {
            String schedule = (String) map.get("schedule");
            schedule = schedule.trim();
            try {
                newTrigger().withIdentity("a", "a").withSchedule(cronSchedule(reportDefinition.getSchedule())
                ).build();
            } catch (Exception ex) {
                logger.error("Invalid Cron Expression", ex);
                throw new UserException("Schedule has a invalid value", ex);
            }
            reportDefinition.setSchedule(schedule);
            isEmptySchedule = false;
        }
        if (isNotEmptyDataInMap("emailRunAs", map)) {
            isEmptyEmailRunAs = false;
        }
        if (isNotEmptyDataInMap("emails", map)) {
            String emails1 = (String) map.get("emails");
            String[] emails = emails1.split(",");
            List<String> cEmails = new ArrayList<>();
            for (String email : emails) {
                if (StringUtils.isNotEmpty(email)) {
                    email = email.trim();
                    try {
                        InternetAddress ia = new InternetAddress(email);
                        ia.validate();
                    } catch (AddressException ex) {
                        logger.error("Invalid Email", ex);
                        throw new UserException("Invalid Email " + email, ex);
                    }
                    cEmails.add(email);
                }
            }
            reportDefinition.setEmails(StringUtils.join(cEmails, ","));
            isEmptyEmail = false;
        }
        if ((!isEmptySchedule || !isEmptyEmailRunAs || !isEmptyEmail) &&
                (isEmptySchedule || isEmptyEmailRunAs || isEmptyEmail)) {
            throw new UserException("Please, fill in all required fields.");
        }
        return reportDefinition;
    }

    @GET
    @Path("/list/{module}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 26, value = "Get list of Recents ", notes = "Get list of Recents")
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response listRecentFavorite(@PathParam("module") String module, @QueryParam("order") String order, @Deprecated @QueryParam("extra") String extra, @QueryParam("visibilityGroupId") Long visibilityGroupId) {
        Map<String, Object> mapResponse = new HashMap<>();
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();
        String typeElement = "report";
        try {
            switch (module) {
                case "recent":

                    List<Recent> list = RecentService.getInstance().listRecents(currentUser.getId(), typeElement, order);

                    List result = new ArrayList<>();

                    for (Recent recent : list) {
                        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(recent.getElementId());
                        if (reportDefinition != null) {
                            Map<String, Object> mapResult = recent.publicMap();
                            mapResult.put("reportType", reportDefinition.getReportType());
                            result.add(mapResult);
                        } else {
                            logger.warn("Recent: report with ID[" + recent.getElementId() + "] not found");
                        }
                    }
                    Long count = (long) result.size();

                    mapResponse.put("total", count);
                    mapResponse.put("results", result);
                    break;
                case "favorite":
                    FavoriteController c = new FavoriteController();
                    List<Favorite> listFavorites = FavoriteService.getInstance().listFavorites(currentUser.getId(), typeElement, order, c.getEntityVisibility(), visibilityGroupId);
                    List resultMap = new ArrayList<>();
                    for (Favorite favorite : listFavorites) {
                        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(favorite.getElementId());
                        if (reportDefinition != null) {
                            //Map<String, Object> mapResult = favorite.publicMap();
                            Map<String, Object> mapResult = QueryUtils.mapWithExtraFields(favorite, extra, getExtraPropertyNames());
                            mapResult.put("reportType", reportDefinition.getReportType());
                            resultMap.add(mapResult);
                        } else {
                            logger.warn("Favorite: report with ID[" + favorite.getElementId() + "] not found");
                        }
                    }
                    Long countFav = (long) resultMap.size();

                    mapResponse.put("total", countFav);
                    mapResponse.put("results", resultMap);
                    break;
            }
        } catch (Exception e) {
            logger.info(e.getMessage());
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
        return RestUtils.sendOkResponse(mapResponse);
    }

    @GET
    @Path("/connection")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 27, value = "Get a list of Connections", notes = "Get a list of Connections")
    public Response listConnections(@QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber,
                                    @QueryParam("order") String order, @QueryParam("where") String where,
                                    @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only,
                                    @QueryParam("visibilityGroupId") Long visibilityGroupId,
                                    @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
                                    @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
                                    @DefaultValue("false") @QueryParam("returnFavorite") boolean returnFavorite,
                                    @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend,
                                    @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project) {
        return new ConnectionController().listConnections(pageSize, pageNumber, order, where, extra, only,
                visibilityGroupId, upVisibility, downVisibility, returnFavorite, extend, project);
    }

    @GET
    @Path("/thingType/getSerialFormulaFields/{thingTypeId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 28,
            value = "Verify that a type of thing uses expressions to create the serial number.",
            notes = "Get a list of fields necessaries to create the serial Number")
    public Response getThingTypeFieldsSerialFormula(@PathParam("thingTypeId") Long thingTypeId) {
        try {
            List<Map<String, Object>> serialFormulaFields = ThingTypeService.getInstance()
                    .getSerialFormulaFields(thingTypeId);
            return RestUtils.sendOkResponse(serialFormulaFields);
        } catch (NonUniqueResultException e) {
            logger.error(e);
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
    }

    @POST
    @Path("/thingType/verifyDataEntry")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 29,
            value = "Verifies that an array of thing types and fields contains formulas of serial number.",
            notes = "Body Format:<br>" +
                    "<font face=\"courier\">[" +
                    "<br>{ \n" +
                    "<br>\"thingTypeId\": 5,\n" +
                    "<br>\"thingTypeFields\": [\"field1\", \"field2\",\"field3\"]\n" +
                    "<br>}," +
                    "<br>{" +
                    "<br>\"thingTypeId\": 6, \n" +
                    "<br>\"thingTypeFields\": [\"field4\", \"field5\",\"field6\"]\n" +
                    "<br>}" +
                    "<br>]" +
                    "</font>")
    public Response validateDataEntryFields(List<Map<String, Object>> body) {
        validateListPermissions();
        try {
            DataEntryHeader dataEntryHeader = new DataEntryHeader(body);
            for (DataEntry entry : dataEntryHeader.getDataEntries()) {
                try {
                    ThingTypeService.getInstance().validateFormulaFields(
                            entry.getThingTypeId(),
                            entry.getThingTypeFields());
                } catch (UserException e) {
                    entry.setMessage(e.getMessage());
                }
            }
            return RestUtils.sendOkResponse(dataEntryHeader.getDataEntries());
        } catch (UserException e) {
            logger.error(e);
            return RestUtils.sendResponseWithCode(e.getMessage(), 400);
        }
    }

    @GET
    @Path("/thingType/treeView/")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position = 30, value = "Get a List of ThingTypes in tree view format")
    public Response listThingTypes(@QueryParam("thingTypeId")Long thingTypeId, @QueryParam("visibilityGroupId")Long visibilityGroupId) {
        try {
            validateListPermissions();
            Subject subject = SecurityUtils.getSubject();
            Map<Long, List<ThingType>> groupPermissions = VisibilityThingUtils.calculateThingsVisibility(visibilityGroupId, subject);
            List<Map<String, Object>> thingTypeTree = ThingTypeService.getInstance()
                    .getThingTypeTreeByIdAndGroups(thingTypeId, new ArrayList<>(groupPermissions.keySet()));
            return RestUtils.sendOkResponse(thingTypeTree);
        } catch (Exception e) {
            logger.error(e);
            return RestUtils.sendResponseWithCode(e.getMessage(), 500);
        }
    }
}
