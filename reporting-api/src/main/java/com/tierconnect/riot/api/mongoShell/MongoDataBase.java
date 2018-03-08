package com.tierconnect.riot.api.mongoShell;

import com.tierconnect.riot.api.mongoShell.operations.OperationExecutor;

/**
 * Created by achambi on 1/31/17.
 * Class to connect a mongo data base.
 */
public class MongoDataBase {

    private final OperationExecutor executor;

    MongoDataBase(OperationExecutor executor) {
        this.executor = executor;
    }

    public MongoCollection getCollection(String collectionName) {
        return new MongoCollection(collectionName, this.getExecutor());
    }

    public OperationExecutor getExecutor() {
        return executor;
    }
}
