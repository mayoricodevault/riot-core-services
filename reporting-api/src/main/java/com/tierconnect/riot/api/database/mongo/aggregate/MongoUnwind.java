package com.tierconnect.riot.api.database.mongo.aggregate;

import com.mongodb.client.model.UnwindOptions;
import org.apache.log4j.Logger;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Aggregates.unwind;
import static com.tierconnect.riot.api.assertions.Assertions.notNull;

/**
 * Created by vealaro on 2/8/17.
 */
public class MongoUnwind extends PipelineBase implements Pipeline {

    private static Logger logger = Logger.getLogger(MongoUnwind.class);

    private String property;
    private Boolean preserveNullAndEmptyArrays;
    private String includeArrayIndex;

    public MongoUnwind(String property) {
        notNull("property", property);
        this.property = property;
    }

    public MongoUnwind(String property, Boolean preserveNullAndEmptyArrays) {
        notNull("property", property);
        this.property = property;
        this.preserveNullAndEmptyArrays = preserveNullAndEmptyArrays;
    }

    @Override
    public Bson toBson() {
        UnwindOptions unwindOptions = new UnwindOptions();
        if (preserveNullAndEmptyArrays != null && includeArrayIndex != null) {
            unwindOptions.preserveNullAndEmptyArrays(preserveNullAndEmptyArrays);
            unwindOptions.includeArrayIndex(includeArrayIndex);
            return unwind(property, unwindOptions);
        } else if (preserveNullAndEmptyArrays != null) {
            unwindOptions.preserveNullAndEmptyArrays(preserveNullAndEmptyArrays);
            return unwind(property, unwindOptions);
        } else if (includeArrayIndex != null) {
            unwindOptions.includeArrayIndex(includeArrayIndex);
            return unwind(property, unwindOptions);
        }
        return unwind(property);
    }



}
