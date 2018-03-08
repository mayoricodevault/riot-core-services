package com.tierconnect.riot.api.database.codecs;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.tierconnect.riot.api.database.utils.ZoneCodeProvider;
import com.tierconnect.riot.api.database.utils.ZoneTransformer;
import org.bson.BsonType;
import org.bson.Transformer;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.junit.Assert.*;

/**
 * Created by vealaro on 12/30/16.
 */
public class MapResultCodecProviderTest {
    private Map<BsonType, Class<?>> map = Collections.<BsonType, Class<?>>singletonMap(BsonType.JAVASCRIPT_WITH_SCOPE, CodeWithScopeMap.class);
    private Transformer transformer = new ZoneTransformer();
    private CodecProvider providerA = new MapResultCodecProvider();
    private CodecProvider providerB = new MapResultCodecProvider(new BsonTypeClassMap());
    private CodecProvider providerC = new MapResultCodecProvider(transformer);
    private CodecProvider providerD = new MapResultCodecProvider(new BsonTypeClassMap(), transformer);
    private CodecProvider providerE = new MapResultCodecProvider(new BsonTypeClassMap(map), transformer);
    private CodecProvider providerF = new MapResultCodecProvider(new BsonTypeClassMap(map), new ZoneTransformer());
    private CodecProvider providerG = new MapResultCodecProvider(new BsonTypeClassMap(map));
    private CodecProvider providerH = new DocumentCodecProvider(new BsonTypeClassMap(), transformer);

    @Test
    public void testEquals() {
        assertEquals(providerA, providerA);
        assertTrue(providerA.equals(providerB));
        assertTrue(providerB.equals(providerA));
        assertTrue(providerC.equals(providerD));
        assertTrue(providerA.hashCode() == providerB.hashCode());
        assertTrue(providerC.hashCode() == providerD.hashCode());
    }

    @Test
    public void testWithTransformAndBsontypeClassMap() {
        assertFalse(providerE.equals(providerF));
        assertFalse(providerG.equals(providerB));
        assertFalse(providerA.equals(providerC));
        assertFalse(providerC.equals(providerA));
        assertFalse(providerE.equals(providerH));
        assertFalse(providerE.equals(null));
    }
}