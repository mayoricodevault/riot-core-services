package com.tierconnect.riot.iot.reports.views.things;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.api.database.base.GenericOperator;
import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.base.operator.MultipleOperator;
import com.tierconnect.riot.api.database.exception.OperationNotSupportedException;
import com.tierconnect.riot.api.database.mongo.Mongo;
import com.tierconnect.riot.api.database.mongo.aggregate.MongoGroupBy;
import com.tierconnect.riot.api.database.mongo.aggregate.Pipeline;
import com.tierconnect.riot.api.database.mongoDrive.MongoDriver;
import com.tierconnect.riot.api.mongoShell.exception.IndexNotFoundException;
import com.tierconnect.riot.appcore.services.FavoriteService;
import com.tierconnect.riot.appcore.utils.TreeUtils;
import com.tierconnect.riot.iot.controllers.ThingTypeController;
import com.tierconnect.riot.iot.dao.mongo.AutoIndexMongoDAO;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.QThingTypeField;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.reports.autoindex.IndexCreatorManager;
import com.tierconnect.riot.iot.reports.views.things.dto.ListResult;
import com.tierconnect.riot.iot.reports.views.things.dto.Parameters;
import com.tierconnect.riot.iot.reports.views.things.dto.VisibilityPermission;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.sdk.dao.NotFoundException;
import com.tierconnect.riot.sdk.dao.Pagination;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.tierconnect.riot.api.database.base.DataBase.ExecutionResultScope;

/**
 * Created by julio.rocha on 07-07-17.
 *
 * @author achambi
 * @author julio.rocha
 */
@SuppressWarnings("unchecked")
abstract class ThingList {
    private static Logger logger = Logger.getLogger(ThingList.class);

    protected Parameters parameters;

    private List<Long> repeatedSerialNumberList;
    private Set<String> nativeThingTypeNames;
    private String projection;
    private ConditionBuilder filter;
    private List<String> groupFields = new ArrayList<>();
    private List<String> onlyFields = new ArrayList<>();

    VisibilityPermission visibilityPermission;
    GenericOperator queryCondition;
    ThingConditionBuilder thingConditionBuilder;

    ThingList(Parameters parameters) {
        this.parameters = parameters;
        nativeThingTypeNames = new HashSet<>();
        filter = new ConditionBuilder();
        groupFields = new ArrayList<>();
        onlyFields = new ArrayList<>();
        this.thingConditionBuilder = ThingConditionBuilder.getInstance();
        repeatedSerialNumberList = new ArrayList<>();
        init();
        this.visibilityPermission = thingConditionBuilder
                .getThingListAndGroupIdList(parameters.getGroups(),
                        parameters.getUpVisibility(),
                        parameters.getDownVisibility());
    }

    private void init() {
        Map<String, Object> nativeThingTypeProperties = getNativeThingTypeProperties();
        nativeThingTypeProperties.forEach((key, value) -> nativeThingTypeNames.add((String) value));
        if (parameters.getOnly() != null) {
            String names = nativeThingTypeNames.stream()
                    .map(nn -> nn + ".value._id")
                    .collect(Collectors.joining(","));
            projection = (StringUtils.isNotEmpty(names)) ? parameters.getOnly() + "," + names : parameters.getOnly();
        }
    }

    /**
     * This method must build the base query
     */
    protected abstract void buildPartialQuery();

    /**
     * @return a map of lists that contains the results
     * @throws Exception If a error exists.
     */
    ListResult getList() throws Exception {
        buildQueryLogic();
        return executeQuery();
    }

    private Map<String, Object> getNativeThingTypeProperties() {
        Map<String, Object> result = new HashMap<>();
        Pagination pagination = new Pagination(1, -1);
        BooleanBuilder be1 = new BooleanBuilder();
        be1 = be1.and(QThingTypeField.thingTypeField.dataTypeThingTypeId.isNotNull());
        List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().listPaginated(be1, pagination, null);
        thingTypeFields
                .forEach(ttf -> {
                    ThingType thingType = ThingTypeService.getInstance().get(ttf.getDataTypeThingTypeId());
                    if (!thingType.isIsParent())
                        result.put(thingType.getCode(), ttf.getName());
                });
        return result;
    }

    private void fillGroupAndOnlyFieldsWithGroupBy() {
        if (StringUtils.isNotEmpty(parameters.getGroupBy())) {
            groupFields.addAll(Arrays.asList(StringUtils.split(parameters.getGroupBy(), ",")));
        } else if (StringUtils.isNotEmpty(projection)) {
            if (!projection.trim().equals("*")) {
                onlyFields.addAll(Arrays.asList(projection.trim().split("\\s*,\\s*")));
            }
        } else {
            onlyFields.add("*");
        }
    }

    private void buildQueryLogic() {
        //visibility filter
        buildPartialQuery();
        //Group by
        fillGroupAndOnlyFieldsWithGroupBy();

        List<GenericOperator> conditionList = new LinkedList<>();

        if (queryCondition != null) {
            if (!queryCondition.isMultipleOperator()) {
                conditionList.add(queryCondition);
            } else if (((MultipleOperator) queryCondition).getGenericOperatorList().size() > 0) {
                conditionList.add(queryCondition);
            }
        }

        GenericOperator visibilityFilter = parameters.getGroups().size() > 0 ?
                thingConditionBuilder.addGroupIdFilterConditionBuilder(visibilityPermission)
                : null;
        if (visibilityFilter != null) {
            conditionList.add(visibilityFilter);
        }

        filter.addAllOperator(conditionList);
    }


    protected ListResult executeQuery() throws Exception {
        ListResult listResult = new ListResult();
        //Execute Query
        if (onlyFields != null && !onlyFields.isEmpty()) {
            listResult = executeQueryConditionBuilder();
        } else if (groupFields != null && !groupFields.isEmpty()) {
            listResult = executeGroupedByQuery();
        }
        if (listResult.getResults() != null) {
            //Sort List
            TreeUtils.sortObjects(parameters.getOrder(), listResult.getResults());
        }
        return listResult;
    }

    /**
     * Method to group by query.
     *
     * @return A instance of {@link ListResult}.
     * instance containing the result
     */
    private ListResult executeGroupedByQuery()
            throws OperationNotSupportedException {
        List<Map<String, Object>> result = new ArrayList<>();
        MongoDriver mongo = new MongoDriver(filter);
        Map<String, Object> groupId = new HashMap<>();
        for (String field : groupFields) {
            groupId.put(field, "$" + field + ".value");
        }

        List<Pipeline> groupBy = new ArrayList<>();
        groupBy.add(new MongoGroupBy(groupId));
        mongo.executeAggregate("things", groupBy, -1);
        List<Map<String, Object>> resultSet = mongo.getResultSet();

        for (Map<String, Object> doc : resultSet) {
            Map<String, Object> udfValues = new HashMap<>();
            for (String field : groupFields) {
                if (doc.get("_id") instanceof Map) {
                    Map<String, Object> udf = (Map) doc.get("_id");
                    String value = (udf != null && udf.get(field) != null) ? udf.get(field).toString() : null;
                    udfValues.put(field, value);
                }
            }
            result.add(udfValues);
        }
        if (parameters.isReturnFavorite()) {
            result = FavoriteService.getInstance().addFavoritesToList(result, parameters.getCurrentUser().getId(), "thing");
        }

        ListResult listResult = new ListResult();
        listResult.setResults(result);
        Number resultTotal = result.size();
        listResult.setTotal(resultTotal.longValue());
        return listResult;
    }


    private ExecutionResultScope getResultScope() {
        ExecutionResultScope resultScope = ExecutionResultScope.INCLUDE_NOTHING;
        if (parameters.isIncludeResults() && parameters.isIncludeTotal()) {
            resultScope = ExecutionResultScope.INCLUDE_RESULT_AND_TOTAL;
        } else if (parameters.isIncludeResults()) {
            resultScope = ExecutionResultScope.INCLUDE_RESULT;
        } else if (parameters.isIncludeTotal()) {
            resultScope = ExecutionResultScope.INCLUDE_TOTAL;
        }
        return resultScope;
    }

    private List<Map<String, Object>> formatQueryResultSet(List<Map<String, Object>> resultSet) {
        resultSet = resultSet.stream().map(item -> {
            if (!onlyFields.contains("*")) {
                item = getValue(item, onlyFields, 1, parameters.isTreeView());
            } else {
                Map<String, Object> udfValues = new LinkedHashMap<>();
                item.forEach((key, value) -> {
                    if (value instanceof LinkedHashMap) {
                        LinkedHashMap udf = (LinkedHashMap) value;
                        udfValues.put(key, udf.get("value"));
                    } else {
                        udfValues.put(key, value);
                    }
                });
                item = udfValues;
            }
            item.put("treeLevel", 1);
            return item;
        }).collect(Collectors.toList());
        //FIXME: Fix this code to another strategy, Victor Angel Chambi Nina, 06/07/2017.
        if (parameters.isReturnFavorite()) {
            FavoriteService.getInstance().addFavoritesToList(resultSet, parameters.getCurrentUser().getId(), "thing");
        }
        if(parameters.isTreeView()){
            //noinspection SuspiciousMethodCalls
            resultSet = resultSet.stream().filter(e -> repeatedSerialNumberList.indexOf(e.get("_id")) == -1).collect(Collectors.toList());
        }
        return resultSet;
    }

    private ListResult executeQueryConditionBuilder() throws Exception {
        //MongoDriver mongo = new MongoDriver(filter);
        Mongo mongo = new Mongo(filter);
        String queryWithoutValues = mongo.getQueryWithoutValues();
        logger.debug("executed query: " + queryWithoutValues);
        String indexId = AutoIndexMongoDAO.generateAutoIndexId("things", queryWithoutValues, parameters.getSortField());
        String indexName = null;
        try {
            indexName = AutoIndexMongoDAO.getInstance().getIndexName(indexId);
            logger.info("Running with index: " + indexName);
        } catch (NotFoundException nfe) {
            logger.info("Running with no index particularly defined");
            IndexCreatorManager.getInstance().executeIndexCreation("things",
                    mongo.getConditionBuilderString(), parameters.getSortField(), queryWithoutValues);
        } catch (IllegalStateException ise) {
            logger.info("Running with no index particularly defined");
        }
        ExecutionResultScope resultScope = getResultScope();
        try {
            mongo.executeFind("things",
                    null,
                    parameters.getSkip(),
                    parameters.getPageSize(),
                    parameters.getSort(),
                    indexName,
                    ThingTypeController.executor,
                    resultScope);
        } catch (IndexNotFoundException ex) {
            logger.warn("Index name: " + indexName +
                    " not found, re-creating the Index and running with no index particularly defined");
            mongo.executeFind("things",
                    null,
                    parameters.getSkip(),
                    parameters.getPageSize(),
                    parameters.getSort(),
                    "",
                    ThingTypeController.executor,
                    resultScope);
            IndexCreatorManager.getInstance().executeIndexReCreation("things",
                    mongo.getConditionBuilderString(),
                    parameters.getSortField(),
                    queryWithoutValues);
        }

        ListResult listResult = new ListResult();
        if (mongo.includeTotal()) {
            listResult.setTotal(mongo.getCountAll());
        }
        if (mongo.includeResult()) {
            listResult.setResults(formatQueryResultSet(mongo.getResultSet()));
        }
        return listResult;
    }

    /**
     * Method to get the value in MongoDB based on filter fields
     *
     * @param thingField   A {@link Map}<{@link String}, {@link Object} instance that contains the field>
     * @param filterFields only some filters List.
     * @param level        the tree level of field.
     * @param treeView     Enable or not the tree view.
     * @return {@link Map}<{@link String},{@link Object}>
     */
    //FIXME,TODO,XXX,HACKED Simplify this algorithm and separate in two or tree methods..
    private Map<String, Object> getValue(Map<String, Object> thingField,
                                         List<String> filterFields,
                                         int level,
                                         boolean treeView) {

        Map<String, Object> udfValues = new HashMap<>();
        List<String> fields = getFiltersFields(filterFields);
        List<String> childrenFields = getFiltersChildrenFields(thingField, filterFields);

        if (level > 1 && treeView && thingField.get("_id") != null) {
            repeatedSerialNumberList.add((Long) thingField.get("_id"));
        }

        for (String filterField : fields) {
            Object objectValue = thingField;
            List<Object> lstObjectChildren = null;
            StringTokenizer st = new StringTokenizer(filterField, ".");
            while (st.hasMoreElements()) {
                String token = st.nextToken();
                if (objectValue instanceof LinkedHashMap) {
                    objectValue = "serialNumber".equals(token) ?
                            ((String) ((LinkedHashMap) objectValue).get(token)).toUpperCase() :
                            ((LinkedHashMap) objectValue).get(token);
                } else if (objectValue instanceof ArrayList) {
                    lstObjectChildren = new ArrayList<>();
                    for (Object objChildren : ((ArrayList) objectValue).toArray()) {
                        Map objectChildren = getValue(
                                (LinkedHashMap) objChildren,
                                ThingMongoDAO.getInstance().childrenFilterFields(fields, level + 1),
                                level + 1,
                                treeView);
                        lstObjectChildren.add(objectChildren);
                    }
                } else {
                    objectValue = null;
                }
            }
            //Set value for array
            if (lstObjectChildren != null && lstObjectChildren.size() > 0) {
                udfValues.put(filterField, lstObjectChildren);
            } else {
                udfValues.put(filterField, objectValue);
            }
        }

        List<Object> lstObjectChildren = getChildrenValue(thingField, filterFields, childrenFields, level, treeView);

        if (!(lstObjectChildren == null) && !lstObjectChildren.isEmpty()) {
            udfValues.put("children", lstObjectChildren);
        }
        return udfValues;
    }

    /**
     * get Children value of a data base record.
     *
     * @param thingField     a instance of {@link Map}<{@link String},{@link Object}> that contains the thing field
     * @param filterFields   a {@link List}<{@link String}> instance that contains the projection names.
     * @param childrenFields a {@link List}<{@link String}> instance that contains the projection children names.
     * @param level          a int tree level.
     * @param treeView       A boolean to check whether the result is in the tree view or not.
     * @return a instance of {@link List}<{@link Object}> that contains the children value.
     */
    private List<Object> getChildrenValue(Map<String, Object> thingField,
                                          List<String> filterFields,
                                          List<String> childrenFields,
                                          int level,
                                          boolean treeView) {

        if (childrenFields != null && !childrenFields.isEmpty()) {
            List<Object> lstObjectChildren = new ArrayList<>();
            for (String filterField : childrenFields) {
                Object objectValue = thingField;
                if (filterField.contains(".value._id")) {
                    String token = filterField.substring(0, filterField.indexOf(".value._id"));
                    objectValue = objectValue != null ? ((LinkedHashMap) objectValue).get(token) : null;
                    objectValue = objectValue != null ? ((LinkedHashMap) objectValue).get("value") : null;
                } else {
                    objectValue = objectValue != null ? ((LinkedHashMap) objectValue).get(filterField) : null;
                }
                Map<String, Object> dataChildren;
                if (objectValue instanceof ArrayList) {
                    for (Object objChildren : ((ArrayList) objectValue).toArray()) {
                        dataChildren = getValue((Map) objChildren, filterFields, level + 1, treeView);
                        dataChildren.put("treeLevel", level + 1);
                        lstObjectChildren.add(dataChildren);
                    }
                } else if (objectValue instanceof LinkedHashMap && treeView) {
                    //noinspection ConstantConditions
                    dataChildren = getValue((LinkedHashMap) objectValue, filterFields, level + 1, treeView);
                    dataChildren.put("treeLevel", level + 1);
                    lstObjectChildren.add(dataChildren);
                }
            }
            return lstObjectChildren;
        }
        return null;
    }

    /**
     * filter children
     *
     * @param doc          the document to filter with the filter list.
     * @param filterFields the filter list instance of {@link List}<{@link String}>
     * @return a instance of {@link List}<{@link String}> that contains the result.
     */
    //FIXME,TODO,XXX,HACKED Change this code to another class. Victor Angel Chambi Chambi 06/07/2017.
    private List<String> getFiltersChildrenFields(Map<String, Object> doc, List<String> filterFields) {
        List<String> resultFilters = new ArrayList<>();
        Collection<String> filterChildren = Collections2.filter(filterFields, Predicates.containsPattern("children"));
        if (filterChildren != null && !filterChildren.isEmpty()) {
            resultFilters.addAll(Collections2.filter(doc.keySet(), Predicates.containsPattern("children")));
        }
        resultFilters.addAll(Collections2.filter(filterFields, Predicates.containsPattern(".value._id")));
        return resultFilters;
    }

    /**
     * get filter list
     *
     * @param filterFields a list of filter fields.
     * @return a instance of {@link List}<{@link String}>
     */
    private List<String> getFiltersFields(List<String> filterFields) {
        List<String> resultFilters = new ArrayList<>();
        Predicate<String> predicate = data -> !data.contains("children") && !data.contains(".value._id");
        resultFilters.addAll(Collections2.filter(filterFields, predicate::test));
        return resultFilters;
    }

}
