package com.tierconnect.riot.api.mongoShell;

import com.tierconnect.riot.api.mongoShell.operations.AggregateOperation;
import com.tierconnect.riot.api.mongoShell.operations.FindOperation;
import com.tierconnect.riot.api.mongoShell.operations.OperationExecutor;

/**
 * Created by achambi on 1/31/17.
 * Mongo collection connection for get data.
 */
public class MongoCollection {


    private final String name;
    private final OperationExecutor executor;


    MongoCollection(String name, OperationExecutor executor) {
        this.name = name;
        this.executor = executor;
    }

    OperationExecutor getExecutor() {
        return executor;
    }

    public AggregateOperation aggregate(String pipeline) {
        return new AggregateOperation(name, executor, pipeline);
    }

    public FindOperation find(String query, String projection) {
        return new FindOperation(name, executor, query, projection);
    }
}