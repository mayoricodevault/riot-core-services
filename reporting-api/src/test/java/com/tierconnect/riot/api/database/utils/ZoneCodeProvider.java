package com.tierconnect.riot.api.database.utils;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * Created by vealaro on 12/30/16.
 */
public class ZoneCodeProvider implements CodecProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (clazz == Zone.class) {
            return (Codec<T>) new ZoneCodeRegistry(registry);
        }
        return null;
    }
}
