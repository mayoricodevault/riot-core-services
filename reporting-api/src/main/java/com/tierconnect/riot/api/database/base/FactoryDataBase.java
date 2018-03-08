package com.tierconnect.riot.api.database.base;

import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.mongo.Mongo;
import com.tierconnect.riot.api.database.mongoDrive.MongoDriver;
import com.tierconnect.riot.api.database.sql.SQL;

/**
 * Created by vealaro on 12/13/16.
 */
public class FactoryDataBase {

    private FactoryDataBase() {
    }

    public static <T> T get(Class<? extends T> type, ConditionBuilder builder) {
        if (type == Mongo.class) {
            return type.cast(new Mongo(builder));
        }
        if (type == SQL.class) {
            return type.cast(new SQL(builder));
        }
        if (type == MongoDriver.class) {
            return type.cast(new MongoDriver(builder));
        }
        throw new IllegalArgumentException("DataBase " + type + "not implement");
    }
}
