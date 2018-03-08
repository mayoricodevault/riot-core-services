package com.tierconnect.riot.api.database.mongo.aggregate;

import com.mongodb.client.model.Aggregates;
import com.tierconnect.riot.api.assertions.Assertions;
import org.bson.conversions.Bson;

import static com.tierconnect.riot.api.mongoShell.utils.CharacterUtils.betweenBraces;
import static com.tierconnect.riot.api.mongoShell.utils.CharacterUtils.converterKeyValueToString;

/**
 * Created by vealaro on 1/30/17.
 */
public class MongoLimit extends PipelineBase implements Pipeline {

    private int value;

    private MongoLimit(int value) {
        this.value = value;
    }

    public static MongoLimit create(int value) {
        Assertions.voidNotNull("limit", value);
        return new MongoLimit(value);
    }

    public Bson toBson() {
        return Aggregates.limit(value);
    }

}
