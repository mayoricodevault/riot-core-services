package com.tierconnect.riot.api.database.codecs;

import com.tierconnect.riot.api.database.utils.Zone;
import com.tierconnect.riot.api.database.utils.ZoneCodec;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecConfigurationException;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonReader;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.bson.codecs.configuration.CodecRegistries.*;
import static org.junit.Assert.*;

/**
 * Created by vealaro on 12/29/16.
 */
public class MapResultTest {

    private final MapResult emptyMapResult = new MapResult();
    private final MapResult mapResult = new MapResult()
            .append("a", 1)
            .append("b", 2)
            .append("c", new MapResult("x", true))
            .append("d", asList(new MapResult("y", false), 1));
    private final MapResult mapResultZone = new MapResult("_id",
            new Zone("Po1", "PoS", "Map Store Santa Monica", "On-Site", "DefaultZoneType1"));
    private final CodecRegistry mapResultZoneRegistry = fromRegistries(fromCodecs(new ZoneCodec()),
            fromProviders(new MapResultCodecProvider(), new ValueCodecProvider()));
    private final MapResultCodec mapResultCodec = new MapResultCodec(mapResultZoneRegistry, new BsonTypeClassMap());

    @Test
    public void shouldBeEqualToItself() {
        assertEquals(emptyMapResult,emptyMapResult);
        assertEquals(mapResult,mapResult);
        assertNotEquals(emptyMapResult, mapResult);
    }

    @Test
    public void shouldNotBeEqualToDifferentBsonDocument() {
        // expect
        assertFalse(emptyMapResult.equals(mapResult));
    }

    @Test
    public void shouldHaveSameHashCodeAsEquivalentBsonDocument() {
        assertEquals(emptyMapResult.hashCode(), new BsonDocument().hashCode());
    }

    @Test
    public void toMapShouldReturnEquivalent() {
        assertEquals(new MapResultCodec().decode(new JsonReader(mapResult.toJson()), DecoderContext.builder().build()),
                mapResult);
    }

    @Test
    public void toJsonShouldTakeACustomDocumentCodec() {
        try {
            mapResultZone.toJson();
            fail("Should fail due to custom type");
        } catch (CodecConfigurationException e) {
            // noop
        }
        Document documentZone = new Document("_id",
                new Document("code", "Po1").append("name", "PoS").append("facilityMap", "Map Store Santa Monica")
                        .append("zoneGroup", "On-Site").append("zoneType", "DefaultZoneType1")
        );
        assertEquals(documentZone.toJson(), mapResultZone.toJson(mapResultCodec));
    }



    @Test
    public void testGetIdZoneEncoding() {
        Zone zoneId = mapResultZone.get("_id", Zone.class);
        mapResultZone.remove("_id");
        assertTrue(mapResultZone.isEmpty());
        MapResultCodec codec = new MapResultCodec(mapResultZoneRegistry, new BsonTypeClassMap());
        MapResult mapResult = new MapResult("_id", zoneId);
        assertTrue(codec.documentHasId(mapResult));
        assertNotNull(codec.getDocumentId(mapResult));
    }


}