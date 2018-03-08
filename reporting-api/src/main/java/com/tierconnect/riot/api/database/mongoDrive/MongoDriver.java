package com.tierconnect.riot.api.database.mongoDrive;


import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.Projections;
import com.tierconnect.riot.api.configuration.PropertyReader;
import com.tierconnect.riot.api.database.base.DataBase;
import com.tierconnect.riot.api.database.base.GenericDataBase;
import com.tierconnect.riot.api.database.base.GenericOperator;
import com.tierconnect.riot.api.database.base.Operation;
import com.tierconnect.riot.api.database.base.alias.Alias;
import com.tierconnect.riot.api.database.base.conditions.BooleanCondition;
import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.base.key.PrimaryKey;
import com.tierconnect.riot.api.database.base.operator.MultipleOperator;
import com.tierconnect.riot.api.database.base.operator.SingleOperator;
import com.tierconnect.riot.api.database.base.operator.SubQueryOperator;
import com.tierconnect.riot.api.database.base.ordination.Order;
import com.tierconnect.riot.api.database.codecs.MapResult;
import com.tierconnect.riot.api.database.exception.OperationNotImplementedException;
import com.tierconnect.riot.api.database.exception.OperationNotSupportedException;
import com.tierconnect.riot.api.database.mongo.aggregate.MongoLimit;
import com.tierconnect.riot.api.database.mongo.aggregate.MongoMatch;
import com.tierconnect.riot.api.database.mongo.aggregate.Pipeline;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonReader;
import org.bson.types.ObjectId;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Filters.elemMatch;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Sorts.*;
import static org.apache.commons.lang.StringUtils.EMPTY;

/**
 * Created by vealaro on 12/8/16.
 */
public class MongoDriver extends DataBase<Bson> implements GenericDataBase {

    private static Logger logger = Logger.getLogger(MongoDriver.class);
    private static final MongoClient mongoClient = MongoClientFactory.createClient();
    private static final MongoDatabase database = mongoClient.getDatabase(PropertyReader.getProperty("mongo.db", "", true));
    private MongoCollection<MapResult> collectionObject;
    private MongoCursor<MapResult> mapResultCursor;
    private Pattern pattern;
    private Bson filterBson;
    private Bson sortBson;
    private Bson projectionBson;
    private int skip;
    private int limit;
    private String collection;
    private List<Pipeline> pipelineList;
    private Document hint;

    public MongoDriver(ConditionBuilder builder) {
        super(builder);
    }

    public void executeAggregate(String collection, List<Pipeline> groupByList, int limit)
            throws OperationNotSupportedException {
        prepareObjects(collection, groupByList, limit);
        executeAggregate();
    }

    public void executeFind(String collection, List<Alias> listAliasFind, int skip, int limit)
            throws OperationNotSupportedException {
        executeFind(collection, listAliasFind, skip, limit, null);
    }

    public void executeFind(String collection, List<Alias> listAliasFind, int skip, int limit,
                            Map<String, Order> orderMap) throws OperationNotSupportedException {
        prepareObjects(collection, listAliasFind, skip, limit, orderMap);
        execute();
    }

    public void executeFind(String collection, List<Alias> listAliasFind, int skip, int limit,
                            Map<String, Order> orderMap, String indexName,
                            ExecutorService executor, ExecutionResultScope resultScope) throws OperationNotSupportedException {
        prepareObjects(collection, listAliasFind, skip, limit, orderMap, indexName, executor, resultScope);
        if (!resultScope.equals(ExecutionResultScope.INCLUDE_NOTHING)) {
            execute();
        }
    }

    @Override
    public String getConditionBuilderString() throws OperationNotSupportedException {
        if (builder.getListGenericOperator().isEmpty()) {
            return EMPTY;
        }
        filterBson = formatFilterBson();
        return bsonToString(filterBson);
    }

    @Override
    public Bson transformMultiOperator(MultipleOperator operator) throws OperationNotSupportedException {
        if (BooleanCondition.AND.equals(operator.getBooleanOperator())) {
            return and(transformMultiOperatorList(operator.getGenericOperatorList()));
        }
        return or(transformMultiOperatorList(operator.getGenericOperatorList()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public Bson transformSingleOperator(SingleOperator operator) throws OperationNotSupportedException {
        List<Object> listValues;
        if (Operation.OperationEnum.EQUALS.equals(operator.getOperator())) {
            return eq(operator.getKey(), transformObject(operator.getValue()));
        } else if (Operation.OperationEnum.NOT_EQUALS.equals(operator.getOperator())) {
            return ne(operator.getKey(), transformObject(operator.getValue()));
        } else if (Operation.OperationEnum.GREATER_THAN.equals(operator.getOperator())) {
            return gt(operator.getKey(), transformObject(operator.getValue()));
        } else if (Operation.OperationEnum.LESS_THAN.equals(operator.getOperator())) {
            return lt(operator.getKey(), transformObject(operator.getValue()));
        } else if (Operation.OperationEnum.GREATER_THAN_OR_EQUALS.equals(operator.getOperator())) {
            return gte(operator.getKey(), transformObject(operator.getValue()));
        } else if (Operation.OperationEnum.LESS_THAN_OR_EQUALS.equals(operator.getOperator())) {
            return lte(operator.getKey(), transformObject(operator.getValue()));
        } else if (Operation.OperationEnum.CONTAINS.equals(operator.getOperator())) {
            pattern = Pattern.compile(".*" + Pattern.quote(transformObject(operator.getValue()) + "") + ".*", Pattern.CASE_INSENSITIVE);
            return regex(operator.getKey(), pattern);
        } else if (Operation.OperationEnum.STARTS_WITH.equals(operator.getOperator())) {
            pattern = Pattern.compile("^" + Pattern.quote(transformObject(operator.getValue()) + ""), Pattern.CASE_INSENSITIVE);
            return regex(operator.getKey(), pattern);
        } else if (Operation.OperationEnum.ENDS_WITH.equals(operator.getOperator())) {
            pattern = Pattern.compile(Pattern.quote(transformObject(operator.getValue()) + "") + "$", Pattern.CASE_INSENSITIVE);
            return regex(operator.getKey(), pattern);
        } else if (Operation.OperationEnum.IN.equals(operator.getOperator())) {
            return in(operator.getKey(), (List) transformObject(operator.getValue()));
        } else if (Operation.OperationEnum.NOT_IN.equals(operator.getOperator())) {
            return nin(operator.getKey(), (List) transformObject(operator.getValue()));
        } else if (Operation.OperationEnum.BETWEEN.equals(operator.getOperator())) {
            listValues = (List<Object>) transformObject(operator.getValue());
            return and(gte(operator.getKey(), listValues.get(0)), lte(operator.getKey(), listValues.get(1)));
        } else if (Operation.OperationEnum.EXISTS.equals(operator.getOperator())) {
            return exists(operator.getKey());
        } else if (Operation.OperationEnum.NOT_EXISTS.equals(operator.getOperator())) {
            return exists(operator.getKey(), false);
        } else if (Operation.OperationEnum.EMPTY.equals(operator.getOperator())) {
            return eq(operator.getKey(), EMPTY);
        } else if (Operation.OperationEnum.NOT_EMPTY.equals(operator.getOperator())) {
            return ne(operator.getKey(), EMPTY);
        } else if (Operation.OperationEnum.IS_NULL.equals(operator.getOperator())) {
            return and(exists(operator.getKey()), eq(operator.getKey(), null));
        } else if (Operation.OperationEnum.IS_NOT_NULL.equals(operator.getOperator())) {
            return ne(operator.getKey(), null);
        } else if (Operation.OperationEnum.EMPTY_ARRAY.equals(operator.getOperator())) {
            return or(exists(operator.getKey(), false), eq(operator.getKey(), null), size(operator.getKey(), 0));
        } else if (Operation.OperationEnum.NOT_EMPTY_ARRAY.equals(operator.getOperator())) {
            return and(exists(operator.getKey()), not(size(operator.getKey(), 0)));
        } else if (Operation.OperationEnum.ARRAY_SIZE_MATCH.equals(operator.getOperator())) {
            return size(operator.getKey(), Integer.parseInt(String.valueOf(transformObject(operator.getValue()))));
        } else if (Operation.OperationEnum.REGEX.equals(operator.getOperator())) {
            listValues = (List<Object>) transformObject(operator.getValue());
            return regex(operator.getKey(), String.valueOf(listValues.get(0)), String.valueOf(listValues.get(1)));
        } else if (Operation.OperationEnum.ELEMENT_MATCH.equals(operator.getOperator())) {
            return elemMatch(operator.getKey(), and(elementMatchList((List<GenericOperator>) operator.getValue())));
        }
        throw new OperationNotSupportedException(operator.getOperator() + " Operation not supported in Mongo driver");
    }

    @Override
    public Bson transformSubQueryOperator(SubQueryOperator operator) throws OperationNotSupportedException {
        MongoDriver subQuery = new MongoDriver(operator.getCondition());
        subQuery.prepareObjects(operator.getTarget(), null, -1);
        boolean hasProjectionFilter = operator.getProjectionFilter() != null;

        MongoCollection<Document> collection = subQuery.database.getCollection(operator.getTarget());
        FindIterable<Document> subQueryResult;

        if (hasProjectionFilter) {
            MongoDriver subQueryFilter = new MongoDriver(operator.getProjectionFilter());
            subQueryFilter.prepareObjects(operator.getTarget(), null, -1);
            subQueryResult = collection.find(subQuery.filterBson)
                    .filter(subQuery.filterBson)
                    .projection(subQueryFilter.filterBson)
                    .projection(fields(include(operator.getFieldToProject()), excludeId()));
        } else {
            subQueryResult = collection.find(subQuery.filterBson)
                    .filter(subQuery.filterBson)
                    .projection(fields(include(operator.getFieldToProject()), excludeId()));
        }
        return in(operator.getKey(), resultAsflatMap(subQueryResult, operator.getFieldToProject()));
    }


    private List<Object> resultAsflatMap(FindIterable<Document> subQueryResult, String fieldToProject) {
        List<Object> projectionResults = new LinkedList<>();
        String[] pathPartition = fieldToProject.split("\\.");
        for (Document document : subQueryResult) {
            Object objToAdd = document.get(pathPartition[0]);
            for (int i = 1; i < pathPartition.length; i++) {
                if (objToAdd instanceof List) {
                    objToAdd = ((List<Document>) objToAdd).get(0).get(pathPartition[i]);
                } else {
                    objToAdd = ((Document) objToAdd).get(pathPartition[i]);
                }
            }
            projectionResults.add(objToAdd);
        }
        return projectionResults;
    }

    private List<Bson> elementMatchList(List<GenericOperator> operationList) throws OperationNotSupportedException {
        List<Bson> listOperator = new ArrayList<>(operationList.size());
        for (GenericOperator operation : operationList) {
            if (operation.isMultipleOperator()) {
                logger.error("operation ELEMENT MATCH is not implemented as Multiple Operator, " + operation);
                throw new OperationNotImplementedException("operation ELEMENT MATCH is not implemented as Multiple Operator");
            }
            listOperator.add(transformSingleOperator((SingleOperator) operation));
        }
        return listOperator;
    }

    private Object transformObject(Object value) {
        if (value instanceof PrimaryKey) {
            if (((PrimaryKey) value).getClazz() == ObjectId.class) {
                return new ObjectId(String.valueOf(((PrimaryKey) value).getValue()));
            }
            return ((PrimaryKey) value).getValue();
        } else if (value instanceof String && ((String) value).startsWith("ObjectId(\"")) {
            return stringToObjectId(value.toString());
        } else if (value instanceof Collection && !((List) value).isEmpty() && (((List) value).get(0) instanceof String)
                && (((List) value).get(0).toString().startsWith("ObjectId(\""))) {
            Function<String, ObjectId> function = new Function<String, ObjectId>() {
                @Nullable
                @Override
                public ObjectId apply(@Nullable String value) {
                    return stringToObjectId(value);
                }
            };
            return Lists.transform((List) value, function);
        }
        return value;
    }

    /**
     * transform String in ObjectId
     * Example: ObjectId("58480a5bb4e3c816fc3d7f24")
     *
     * @param objectIdString
     * @return
     */
    private ObjectId stringToObjectId(String objectIdString) {
        JsonReader jsonReader = new JsonReader(objectIdString);
        return jsonReader.readObjectId();
    }

    @Override
    public void setAliasList(List<Alias> aliasList) {
        super.setAliasList(aliasList);
        formatProjection();
    }

    @Override
    public void setMapOrder(Map<String, Order> mapOrder) {
        super.setMapOrder(mapOrder);
        formatSort();
    }

    private void prepareObjects(String collection, List<Alias> listAliasFind, int skip, int limit,
                                Map<String, Order> mapOrder, String indexName,
                                ExecutorService executor, ExecutionResultScope resultScope) throws OperationNotSupportedException {
        prepareObjects(collection, listAliasFind, skip, limit, mapOrder);
        if (StringUtils.isNotEmpty(indexName)) {
            hint = new Document("$hint", indexName);
        }
        if (executor != null) {
            this.executor = executor;
        }
        if (resultScope == null) {
            throw new IllegalArgumentException("ResultScope could not be null for async requests");
        }
        this.resultScope = resultScope;
    }

    private void prepareObjects(String collection, List<Alias> listAliasFind, int skip, int limit,
                                Map<String, Order> mapOrder) throws OperationNotSupportedException {
        this.collection = collection;
        setMapOrder(mapOrder);
        setAliasList(listAliasFind);
        this.skip = skip;
        this.limit = limit;
        filterBson = formatFilterBson();
    }

    private void prepareObjects(String collection, List<Pipeline> groupByList, int limit) throws OperationNotSupportedException {
        this.collection = collection;
        this.limit = limit;
        this.pipelineList = groupByList;
        filterBson = formatFilterBson();
    }

    private Bson formatFilterBson() throws OperationNotSupportedException {
        List<Bson> bsonList = transformMultiOperatorList(builder.getListGenericOperator());
        if (bsonList.isEmpty()) return null;
        if (BooleanCondition.AND.equals(builder.getBooleanCondition())) {
            return and(bsonList);
        }
        return or(bsonList);
    }

    private void formatSort() {
        this.sortBson = null;
        if (this.mapOrder != null && !this.mapOrder.isEmpty()) {
            List<Bson> listSort = new ArrayList<>(mapOrder.size());
            for (Map.Entry<String, Order> order : mapOrder.entrySet()) {
                listSort.add(orderBy(Order.ASC.equals(order.getValue()) ? ascending(order.getKey()) :
                        descending(order.getKey())));
            }
            this.sortBson = orderBy(listSort);
        }
    }

    private void formatProjection() {
        projectionBson = null;
        if (getAliasList() != null && !getAliasList().isEmpty()) {
            List<Bson> listPropertyDisplay = new ArrayList<>(getAliasList().size());
            for (Alias alias : getAliasList()) {
                if (alias.isExclude()) {
                    listPropertyDisplay.add(Projections.exclude(alias.getProperty()));
                } else {
                    listPropertyDisplay.add(include(alias.getProperty()));
                }
            }
            projectionBson = fields(listPropertyDisplay);
        }
    }

    private void execute() throws OperationNotSupportedException {
        try {
            logger.info("\n COLLECTION: " + collection + " \n QUERY:" + getConditionBuilderString() + "\n PROJECTION:" + bsonToString(projectionBson));
            long start = System.currentTimeMillis();
            collectionObject = this.database.getCollection(collection, MapResult.class);
            List<CompletableFuture<Void>> parallelComputation = new LinkedList<>();
            if (includeResult()) {
                if (executor != null) {
                    parallelComputation.add(CompletableFuture.runAsync(this::executeFindQuery, executor));
                } else {
                    parallelComputation.add(CompletableFuture.runAsync(this::executeFindQuery));
                }

            }
            if (includeTotal()) {
                if (executor != null) {
                    parallelComputation.add(CompletableFuture.runAsync(this::executeCountForFindQuery, executor));
                } else {
                    parallelComputation.add(CompletableFuture.runAsync(this::executeCountForFindQuery));
                }
            }
            parallelComputation.parallelStream()
                    .map(CompletableFuture::join)
                    .count();//do nothing special, just join;
            logger.info("RUN QUERY WITH TIME [" + (System.currentTimeMillis() - start) + "] ms");
        } catch (CompletionException  ce) {
            handleException(ce.getCause());
        }
    }

    private void handleException(Throwable e) {
        e = (e.getCause() instanceof MongoException) ? (MongoException) e.getCause() : e;
        if (e instanceof MongoException ) {
            MongoException me = (MongoException) e;
            if (me.getCode() == 2 && me.getMessage().contains("bad hint")) {
                throw new IllegalArgumentException("The index \"" + hint.getString("$hint") + "\" was deleted", e);
            } else {
                throw new RuntimeException(me);
            }
        }
        throw new RuntimeException(e);
    }

    private void executeFindQuery() {
        resultSet.clear();
        long start = System.currentTimeMillis();
        if (this.filterBson != null && this.sortBson != null && this.projectionBson != null) {
            mapResultCursor = collectionObject.find(filterBson).modifiers(hint).sort(sortBson).projection(projectionBson).skip(skip).limit(limit).iterator();
        } else if (this.filterBson != null && this.sortBson != null) {
            mapResultCursor = collectionObject.find(filterBson).modifiers(hint).sort(sortBson).skip(skip).limit(limit).iterator();
        } else if (this.filterBson != null && this.projectionBson != null) {
            mapResultCursor = collectionObject.find(filterBson).modifiers(hint).projection(projectionBson).skip(skip).limit(limit).iterator();
        } else if (this.filterBson != null) {
            mapResultCursor = collectionObject.find(filterBson).modifiers(hint).skip(skip).limit(limit).iterator();
        } else if (this.projectionBson != null) {
            mapResultCursor = collectionObject.find().projection(projectionBson).skip(skip).limit(limit).iterator();
        } else {
            mapResultCursor = collectionObject.find().skip(skip).limit(limit).iterator();
        }
        while (mapResultCursor.hasNext()) {
            resultSet.add(mapResultCursor.next().toMap());
        }
        logger.info("\nQuery execution time: " + (System.currentTimeMillis() - start) + "ms");
    }

    private void executeCountForFindQuery() {
        long start = System.currentTimeMillis();
        if (filterBson != null) {
            countAll = collectionObject.count(filterBson, new CountOptions().hint(hint));
        } else {
            countAll = collectionObject.count();
        }
        logger.info("\nCount execution time: " + (System.currentTimeMillis() - start) + "ms");
    }

    private void executeAggregate() {
        StringBuilder aggregateLog = new StringBuilder(",");
        resultSet.clear();
        collectionObject = this.database.getCollection(collection, MapResult.class);
        List<Bson> aggregateList = new ArrayList<>(3);
        Bson bsonMatch = MongoMatch.create(filterBson).toBson();
        Bson bsonLimit = MongoLimit.create(limit).toBson();
        aggregateList.add(bsonMatch);
        aggregateLog.append(bsonToString(bsonMatch));
        if (pipelineList != null && !pipelineList.isEmpty()) {
            aggregateList = new ArrayList<>(pipelineList.size() + 2);
            aggregateList.add(MongoMatch.create(filterBson).toBson());
            for (Pipeline groupBy : pipelineList) {
                Bson bsonGroup = groupBy.toBson();
                aggregateList.add(bsonGroup);
                aggregateLog.append(",").append(bsonToString(bsonGroup));
            }
        }
        //TODO: CREATE TEST FOR THIS CODE, Angel Chambi Nina 07/07/2017
        if (limit > 0) {
            aggregateList.add(bsonLimit);
            aggregateLog.append(",").append(bsonLimit);
        }
        aggregateLog.deleteCharAt(0);
        logger.info("\n COLLECTION: " + collection + " \n AGGREGATE: [" + aggregateLog.toString() + "]");
        long start = System.currentTimeMillis();
        mapResultCursor = collectionObject.aggregate(aggregateList).iterator();
        while (mapResultCursor.hasNext()) {
            resultSet.add(mapResultCursor.next().toMap());
        }
        countAll = (long) resultSet.size();
        logger.info("RUN QUERY OF SIZE [" + countAll + "} WITH TIME [" + (System.currentTimeMillis() - start) + "] ms");
    }

    public static String bsonToString(Bson bson) {
        if (bson == null) return "null";
        return bson.toBsonDocument(BsonDocument.class, MongoClient.getDefaultCodecRegistry()).toJson();
    }

    public String getSortString() {
        return bsonToString(sortBson);
    }

    public String getProjectionString() {
        return bsonToString(projectionBson);
    }
}