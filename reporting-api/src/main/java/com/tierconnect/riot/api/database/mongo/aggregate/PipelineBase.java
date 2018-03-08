package com.tierconnect.riot.api.database.mongo.aggregate;

import com.mongodb.MongoClient;
import org.bson.BsonDocument;
import org.bson.conversions.Bson;

/**
 * Created by vealaro on 1/30/17.
 */
public abstract class PipelineBase {

    protected static final String LIMIT = "$limit";
    protected static final String GROUP = "\"$group\"";
    protected static final String MATCH = "\"$match\"";

    public abstract Bson toBson();

    @Override
    public String toString() {
        Bson bson = toBson();
        if (bson == null) return "null";
        return bson.toBsonDocument(BsonDocument.class, MongoClient.getDefaultCodecRegistry()).toJson();
    }
}
