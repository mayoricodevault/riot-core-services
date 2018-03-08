package com.tierconnect.riot.api.mongoTransform;

import org.apache.log4j.Logger;
import org.bson.*;
import org.bson.codecs.BsonArrayCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.util.*;

/**
 * Created by vealaro on 11/30/16.
 * Class to codec a database result.
 */
public class BsonArrayToResultSet extends BsonArrayCodec {

    private List<Map<String, Object>> resultSet = new ArrayList<>();

    /**
     * Construct an instance with the given registry
     *
     * @param codecRegistry the codec registry
     */
    public BsonArrayToResultSet(CodecRegistry codecRegistry) {
        super(codecRegistry);
    }

    @Override
    public BsonArray decode(final BsonReader reader, final DecoderContext decoderContext) {
        reader.readStartArray();

        List<BsonValue> list = new ArrayList<>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            BsonValue e = readValue(reader, decoderContext);
            resultSet.add(BsonToMap.getMap((BsonDocument) e));
            list.add(e);
        }

        reader.readEndArray();

        return new BsonArray(list);
    }

    public List<Map<String, Object>> getResultSet() {
        return resultSet;
    }

}
