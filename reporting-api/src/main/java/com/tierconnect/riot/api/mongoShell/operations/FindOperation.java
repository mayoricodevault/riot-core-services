package com.tierconnect.riot.api.mongoShell.operations;

import com.tierconnect.riot.api.database.exception.OperationNotImplementedException;

import java.util.Map;

import static com.tierconnect.riot.api.assertions.Assertions.isNotBlank;
import static com.tierconnect.riot.api.assertions.Assertions.isNotNull;
import static com.tierconnect.riot.api.assertions.Assertions.isTrue;

/**
 * Created by achambi on 4/27/17.
 * Class for Explain.
 */
public class FindOperation {


    /**
     * The name of the collection to as the input for the aggregation pipeline.
     */
    private final String collection;

    /**
     * The query
     */
    private final String query;

    /**
     * Fields to project.
     */
    private final String fields;

    /**
     * Explain Verbosity to Set.
     */
    private ExplainShellVerbosity explainShellVerbosity;


    /**
     * Query executor instance created by the principal class.
     */
    private OperationExecutor operationExecutor;

    public String getCollection() {
        return collection;
    }

    public String getFields() {
        return fields;
    }

    public String getQuery() {
        return query;
    }

    public OperationExecutor getOperationExecutor() {
        return operationExecutor;
    }

    public FindOperation(String collection, OperationExecutor operationExecutor, String query, String fields) {
        isTrue("collection", isNotBlank(collection));
        isTrue("operationExecutor", isNotNull(operationExecutor));
        isTrue("query", isNotBlank(query));
        isTrue("fields", isNotBlank(query));
        this.collection = collection;

        this.operationExecutor = operationExecutor;

        this.query = query;
        this.fields = fields;
    }

    public FindOperation explain(ExplainShellVerbosity explainShellVerbosity) {
        this.explainShellVerbosity = explainShellVerbosity;
        return this;
    }

    public Map<String, Object> execute() throws OperationNotImplementedException {
        if (this.explainShellVerbosity == null) {
            throw new OperationNotImplementedException("Find not supported, only the ExplainMode is supported!");
        }
        return null;
    }
}
