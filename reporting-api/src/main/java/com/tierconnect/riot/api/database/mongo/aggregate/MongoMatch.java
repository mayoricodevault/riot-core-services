package com.tierconnect.riot.api.database.mongo.aggregate;

import com.tierconnect.riot.api.assertions.Assertions;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Aggregates.match;
import static com.tierconnect.riot.api.mongoShell.utils.CharacterUtils.betweenBraces;
import static com.tierconnect.riot.api.mongoShell.utils.CharacterUtils.converterKeyValueToString;

/**
 * Created by vealaro on 1/30/17.
 */
public class MongoMatch extends PipelineBase implements Pipeline {

    private String filterString;
    private Bson filterBson;

    private MongoMatch(String filter) {
        this.filterString = filter;
    }

    private MongoMatch(Bson filter) {
        this.filterBson = filter;
    }

    public static MongoMatch create(Bson filter) {
        Assertions.voidNotNull("Filter", filter);
        return new MongoMatch(filter);
    }

    public static MongoMatch create(String filter) {
        Assertions.voidNotNull("Filter", filter);
        return new MongoMatch(filter);
    }

    public Bson toBson() {
        return match(filterBson);
    }

    @Override
    public String toString() {
        return betweenBraces(converterKeyValueToString(MATCH, filterString));
    }
}
