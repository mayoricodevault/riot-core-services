package com.tierconnect.riot.api.mongoShell.operations;

import com.tierconnect.riot.api.mongoShell.ResultQuery;
import com.tierconnect.riot.api.mongoShell.query.QueryBuilder;
import com.tierconnect.riot.api.mongoShell.query.ResultFormat;
import com.tierconnect.riot.api.mongoShell.utils.FileUtils;

import java.io.File;
import java.io.IOException;

import static org.apache.commons.lang.StringUtils.*;

/**
 * Created by achambi on 1/31/17.
 * Aggregate class to set pipelines.
 */
public class AggregateOperation {

    /**
     * Aggregate template to set in temporal executor file.
     */
    private static final String AGGREGATE_TEMPLATE = "aggregate(%1$s,{\"allowDiskUse\": %2$s})";
    /**
     * Aggregate explain template to set in temporal executor file.
     */
    private static final String AGGREGATE_EXPLAIN_TEMPlATE = "db.%1$s.aggregate(%2$s, {\"allowDiskUse\": %3$s, " +
            "\"explain\": %4$b})";
    /**
     * Default count value is -1 in aggregate. Because the rows count is calculated by the driver's client.
     */
    private static final String AGGREGATE_COUNT_TEMPLATE = "-1";

    /**
     * Aggregate default temporal file name.
     */
    private static final String AGGREGATE_TEMP_FILE_NAME = "mongoAggregateTemp";

    /**
     * Optional. Enables writing to temporary files. When set to true, aggregation stages can write data to the _tmp
     * subdirectory in the dbPath directory.
     */
    private boolean allowDiskUse;
    /**
     * Optional. Specifies to return the information on the processing of the pipeline.
     */
    private boolean explain;
    /**
     * An array of aggregation pipeline stages that process and transform the document stream as part of the
     * aggregation pipeline.
     */
    private String pipeline;
    /**
     * The name of the collection to as the input for the aggregation pipeline.
     */
    private String collection;

    /**
     * Query executor instance created by the principal class.
     */
    private OperationExecutor operationExecutor;

    /**
     * return allowDiskUse
     *
     * @return allowDiskUse
     */
    public boolean isAllowDiskUse() {
        return allowDiskUse;
    }

    /**
     * return collection Name
     *
     * @return collection
     */
    public String getCollection() {
        return collection;
    }

    /**
     * return explain
     *
     * @return explain
     */
    public boolean isExplain() {
        return explain;
    }

    /**
     * return the {@link OperationExecutor} instance.
     *
     * @return {@link OperationExecutor}
     */
    public OperationExecutor getOperationExecutor() {
        return operationExecutor;
    }

    /**
     * prepare aggregate Operations and set default allowDiskUse with false.
     *
     * @param collection        a {@link String} that contains the collectionName.
     * @param operationExecutor The mongo {@link OperationExecutor} to use.
     * @param pipeline          a {@link String} pipelines to execute.
     */
    public AggregateOperation(String collection, OperationExecutor operationExecutor,
                              String pipeline) {
        this.collection = collection;
        this.pipeline = pipeline;
        this.allowDiskUse = true;
        this.explain = false;
        this.operationExecutor = operationExecutor;
    }

    /**
     * set allowDiskUse.
     *
     * @param allowDiskUse a boolean.
     * @return the same {@link AggregateOperation} instance.
     */
    public AggregateOperation setAllowDiskUse(boolean allowDiskUse) {
        this.allowDiskUse = allowDiskUse;
        return this;
    }

    /**
     * set explain.
     *
     * @param explain a boolean.
     * @return the same {@link AggregateOperation} instance.
     */
    public AggregateOperation setExplain(boolean explain) {
        this.explain = explain;
        return this;
    }

    /**
     * Execute the aggregate operation.
     *
     * @param tmpFileQueryName optional: A {@link String} that contains the File query name.
     * @return A instance of {@link ResultQuery}.
     * @throws IOException          if any input or output parameter is invalid.
     * @throws InterruptedException if the current thread is
     *                              {@linkplain Thread#interrupt() interrupted} by another
     *                              thread while it is waiting, then the wait is ended and
     *                              an {@link InterruptedException} is thrown.
     */
    public ResultQuery execute(String tmpFileQueryName) throws IOException, InterruptedException {
        QueryBuilder queryBuilder = new QueryBuilder(this.collection, this.operationExecutor);
        String operationDefinition = String.format(AGGREGATE_TEMPLATE, this.pipeline, this.allowDiskUse);
        ResultFormat resultFormat = ResultFormat.BSON;
        String tmpResultFindPath = queryBuilder.getTmpFileResultPath(AggregateOperation.AGGREGATE_TEMP_FILE_NAME,
                tmpFileQueryName, resultFormat.getResultTemplate());
        String explainTemplate = (explain) ? String.format(AGGREGATE_EXPLAIN_TEMPlATE,
                collection,
                pipeline,
                allowDiskUse,
                explain) : EMPTY;
        File fileQuery = queryBuilder.createQueryFile(
                operationDefinition,
                EMPTY,
                AGGREGATE_COUNT_TEMPLATE,
                queryBuilder.getTmpFileQueryName(AggregateOperation.AGGREGATE_TEMP_FILE_NAME, tmpFileQueryName),
                tmpResultFindPath,
                EMPTY,
                explain,
                explainTemplate,
                resultFormat.getResultTemplate(), true, true);
        try {
            return operationExecutor.executeQuery(fileQuery.getPath(), tmpResultFindPath, explain, true, true);
        } finally {
            FileUtils.deleteFile(fileQuery);
            FileUtils.deleteFile(new File(tmpResultFindPath));
        }
    }

    /**
     * Execute the aggregate operation.
     *
     * @return A instance of {@link ResultQuery}
     * @throws IOException          if any input or output parameter is invalid.
     * @throws InterruptedException if the current thread is
     *                              {@linkplain Thread#interrupt() interrupted} by another
     *                              thread while it is waiting, then the wait is ended and
     *                              an {@link InterruptedException} is thrown.
     */
    public ResultQuery execute() throws IOException, InterruptedException {
        return execute(EMPTY);
    }
}
