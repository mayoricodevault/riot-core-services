package com.tierconnect.riot.api.database.codecs;

import org.bson.Transformer;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

import static org.bson.assertions.Assertions.notNull;

/**
 * Created by vealaro on 12/12/16.
 */
public class MapResultCodecProvider implements CodecProvider {

    private final BsonTypeClassMap bsonTypeClassMap;
    private final Transformer valueTransformer;


    public MapResultCodecProvider() {
        this(new BsonTypeClassMap());
    }

    public MapResultCodecProvider(final Transformer valueTransformer) {
        this(new BsonTypeClassMap(), valueTransformer);
    }

    public MapResultCodecProvider(BsonTypeClassMap bsonTypeClassMap) {
        this(bsonTypeClassMap, null);
    }

    public MapResultCodecProvider(final BsonTypeClassMap bsonTypeClassMap, final Transformer valueTransformer) {
        this.bsonTypeClassMap = notNull("bsonTypeClassMap", bsonTypeClassMap);
        this.valueTransformer = valueTransformer;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
        if (clazz == CodeWithScopeMap.class) {
            return (Codec<T>) new CodeWithScopeCodecMap(registry.get(MapResult.class));
        }
        if (clazz == MapResult.class) {
            return (Codec<T>) new MapResultCodec(registry, bsonTypeClassMap, valueTransformer);
        }
        return null;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MapResultCodecProvider that = (MapResultCodecProvider) o;

        if (!bsonTypeClassMap.equals(that.bsonTypeClassMap)) {
            return false;
        }
        if (valueTransformer != null ? !valueTransformer.equals(that.valueTransformer) : that.valueTransformer != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = bsonTypeClassMap.hashCode();
        result = 31 * result + (valueTransformer != null ? valueTransformer.hashCode() : 0);
        return result;
    }
}
