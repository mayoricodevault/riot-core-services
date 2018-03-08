package com.tierconnect.riot.api.database.codecs;

import com.mongodb.MongoClient;
import com.tierconnect.riot.api.database.utils.Zone;
import com.tierconnect.riot.api.database.utils.ZoneCodeProvider;
import com.tierconnect.riot.api.database.utils.ZoneCodec;
import com.tierconnect.riot.api.database.utils.ZoneTransformer;
import org.apache.log4j.Logger;
import org.bson.*;
import org.bson.codecs.*;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.io.BasicOutputBuffer;
import org.bson.io.BsonInput;
import org.bson.io.ByteBufferBsonInput;
import org.bson.types.Binary;
import org.bson.types.Code;
import org.bson.types.MaxKey;
import org.bson.types.MinKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import static java.util.Arrays.asList;
import static org.bson.codecs.configuration.CodecRegistries.*;
import static org.junit.Assert.*;

/**
 * Created by vealaro on 12/27/16.
 */
public class MapResultCodecTest {

    private static Logger logger = Logger.getLogger(MapResultCodecTest.class);

    private BasicOutputBuffer buffer;
    private BsonBinaryWriter writer;
    private Zone zoneTest = new Zone("Po1", "PoS", "Map Store Santa Monica", "On-Site", "DefaultZoneType1");
    private final MapResult mapResultZone = new MapResult("zone", zoneTest);
    private final CodecRegistry mapResultZoneRegistry = fromRegistries(fromCodecs(new ZoneCodec()),
            fromProviders(new MapResultCodecProvider(), new ValueCodecProvider()));


    @Before
    public void setUp() throws Exception {
        buffer = new BasicOutputBuffer();
        writer = new BsonBinaryWriter(buffer);
    }

    @After
    public void tearDown() {
        writer.close();
    }

    @Test
    public void testPrimitiveBSONTypeCodecs() throws IOException {
        MapResultCodec codec = new MapResultCodec();
        MapResult mapResult = new MapResult();
//        mapResult.put("oid", new ObjectId("5862c882b4e3c86fd8dacf1f"));
        mapResult.put("_id", "ObjectId(\"5862c882b4e3c86fd8dacf1f\")");
        mapResult.put("integer", 1);
        mapResult.put("long", 2L);
        mapResult.put("string", "hello");
        mapResult.put("double", 3.2);
        mapResult.put("binary", new Binary(BsonBinarySubType.USER_DEFINED, new byte[]{0, 1, 2, 3}));
        mapResult.put("date", new Date(1000));
        mapResult.put("boolean", true);
        mapResult.put("code", new Code("var i = 0"));
        mapResult.put("minkey", new MinKey());
        mapResult.put("maxkey", new MaxKey());
        mapResult.put("null", null);
        mapResult.put("uuid", UUID.fromString("7ee973c0-54b5-11e4-aaed-0002a5d5c51b"));

        codec.encode(writer, mapResult, EncoderContext.builder().build());
        BsonInput bsonInput = createInputBuffer();
        MapResult decodeMapResult = codec.decode(new BsonBinaryReader(bsonInput), DecoderContext.builder().build());
        assertEquals(mapResult, decodeMapResult);
    }

    @Test
    public void testUUIDBSONTypeCodecs() throws IOException {
        UUID originUUID1 = UUID.fromString("b626c0d0-ce99-11e6-9598-0800200c9a66");
        UUID originUUID4 = UUID.fromString("EDD2EB80-CE99-11E6-9598-0800200C9A66");

        MapResultCodec codec = new MapResultCodec();
        MapResult mapResult = new MapResult();
        mapResult.put("uuidRandom", UUID.randomUUID());
        mapResult.put("uuid1", originUUID1);
        mapResult.put("uuid4", originUUID4);

        codec.encode(writer, mapResult, EncoderContext.builder().build());

        BsonInput bsonInput = createInputBuffer();
        MapResult decodeMapResult = codec.decode(new BsonBinaryReader(bsonInput), DecoderContext.builder().build());
        assertEquals(mapResult, decodeMapResult);
    }

    @Test
    public void testIterableContainingOtherIterableEncoding() throws IOException {
        MapResultCodec codec = new MapResultCodec();
        MapResult mapResult = new MapResult();
        List<List<Integer>> listOfList = Arrays.asList(Collections.singletonList(100), Collections.singletonList(100));
        mapResult.put("array", listOfList);
        codec.encode(writer, mapResult, EncoderContext.builder().build());
        BsonInput bsonInput = createInputBuffer();
        MapResult decodeMapResult = codec.decode(new BsonBinaryReader(bsonInput), DecoderContext.builder().build());
        assertEquals(mapResult, decodeMapResult);
    }

    @Test
    public void testIterableEncoding() throws IOException {
        MapResultCodec codec = new MapResultCodec();
        MapResult mapResult = new MapResult();
        mapResult.put("list", Arrays.asList(1, 2, 3, 4, 5, 6));
        mapResult.put("set", new HashSet<Integer>(Arrays.asList(4, 3, 2, 1)));
        codec.encode(writer, mapResult, EncoderContext.builder().build());
        BsonInput bsonInput = createInputBuffer();
        MapResult decodeMapResult = codec.decode(new BsonBinaryReader(bsonInput), DecoderContext.builder().build());

        MapResult mapResultExpected = new MapResult();
        mapResultExpected.put("list", Arrays.asList(1, 2, 3, 4, 5, 6));
        mapResultExpected.put("set", new HashSet<Integer>(Arrays.asList(4, 3, 2, 1)));
        assertEquals(mapResultExpected.toString(), decodeMapResult.toString());
    }

    @Test
    public void testIterableContainingMapResultEncoding() throws IOException {
        MapResultCodec codec = new MapResultCodec();
        MapResult mapResult = new MapResult();
        List<MapResult> listOfMapResult = Arrays.asList(new MapResult("name", "ONE"), new MapResult("price", Long.valueOf("10")));
        mapResult.put("array", listOfMapResult);

        codec.encode(writer, mapResult, EncoderContext.builder().build());

        BsonInput bsonInput = createInputBuffer();
        MapResult decodeMapResult = codec.decode(new BsonBinaryReader(bsonInput), DecoderContext.builder().build());
        assertEquals(mapResult.toString(), decodeMapResult.toString());
    }

    @Test
    public void shouldNotGenerateIdIfPresent() {
        MapResultCodec codec = new MapResultCodec();
        MapResult mapResult = new MapResult("_id", 1);
        assertTrue(codec.documentHasId(mapResult));
        mapResult = codec.generateIdIfAbsentFromDocument(mapResult);
        assertTrue(codec.documentHasId(mapResult));
        assertEquals(new BsonInt32(1), codec.getDocumentId(mapResult));
    }

    @Test
    public void shouldGenerateIdIfAbsent() {
        MapResultCodec codec = new MapResultCodec();
        MapResult mapResult = new MapResult();
        assertFalse(codec.documentHasId(mapResult));
        mapResult = codec.generateIdIfAbsentFromDocument(mapResult);
        assertTrue(codec.documentHasId(mapResult));
        assertEquals(BsonObjectId.class, codec.getDocumentId(mapResult).getClass());
    }

    @Test(expected = IllegalStateException.class)
    public void testIdIfNotPresent() {
        MapResultCodec codec = new MapResultCodec();
        MapResult mapResult = new MapResult("value", 1);
        assertFalse(codec.documentHasId(mapResult));
        assertNull(codec.getDocumentId(mapResult));
    }

    @Test
    public void testIdBsonValue() {
        MapResultCodec codec = new MapResultCodec();
        MapResult mapResult = new MapResult("value", 1);
        mapResult.put("_id", new BsonInt32(2));
        assertTrue(codec.documentHasId(mapResult));
        codec.getDocumentId(mapResult);
        assertEquals(BsonInt32.class, codec.getDocumentId(mapResult).getClass());
        mapResult.clear();
        mapResult.put("_id", new BsonInt64(2));
        assertTrue(codec.documentHasId(mapResult));
        codec.getDocumentId(mapResult);
        assertEquals(BsonInt64.class, codec.getDocumentId(mapResult).getClass());
    }

    @Test
    public void testContainingZoneEnconding() throws IOException {
        Zone zoneId = mapResultZone.get("zone", Zone.class);
        mapResultZone.clear();
        assertTrue(mapResultZone.isEmpty());
        MapResultCodec codec = new MapResultCodec(mapResultZoneRegistry, new BsonTypeClassMap());
        MapResult mapResult = new MapResult("_id", zoneId).append("value", zoneId);

        codec.encode(writer, mapResult, EncoderContext.builder().build());

        BsonInput bsonInput = createInputBuffer();
        MapResult decodeMapResult = codec.decode(new BsonBinaryReader(bsonInput), DecoderContext.builder().build());
        assertEquals(mapResult.toJson(codec), decodeMapResult.toJson());
    }

    @Test
    public void testCodeWithScopeEnconding() throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("codeA", new CodeWithScopeMap("javaScript code A",
                new MapResult("fieldNameOfScope", "valueOfScope")));
        map.put("codeB", new CodeWithScopeMap("javaScript code a",
                new MapResult("fieldNameOfScope", "valueOfScope")));

        MapResultCodec codec = new MapResultCodec();
        MapResult mapResult = new MapResult();
        mapResult.putAll(map);

        codec.encode(writer, mapResult, EncoderContext.builder().build());

        MapResult decodeMap = codec.decode(new BsonBinaryReader(createInputBuffer()),
                DecoderContext.builder().build());
        logger.info("decode " + decodeMap);
        assertEquals(mapResult, decodeMap);

    }

    @Test
    @Ignore
    public void testTransform() {
        CodecRegistry mapResultZoneRegistry = fromRegistries(fromCodecs(new ZoneCodec()),
                fromProviders(new MapResultCodecProvider(), new ValueCodecProvider()));
        DocumentCodec mapResultCodec = new DocumentCodec(mapResultZoneRegistry, new BsonTypeClassMap());

        MapResult mapResult = new MapResult();
        assertTrue(mapResultZone.containsValue(zoneTest));
        mapResult.put("zone", zoneTest);
        BsonDocument bsonDocument = mapResult.toBsonDocument(BsonDocument.class, mapResultZoneRegistry);

        Document document = new Document();
        document.put("zone", zoneTest);
        logger.info(bsonDocument.toJson());
        logger.info(document.toJson(mapResultCodec));
        assertEquals(document.toJson(mapResultCodec), bsonDocument.toJson());
        assertFalse(((Bson) mapResult).equals((Bson) document));
    }

    @Test
    public void testTransformDocument() throws Exception {
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(asList(new ZoneCodeProvider(), new MapResultCodecProvider(new ZoneTransformer()))),
                CodecRegistries.fromCodecs(new MapResultCodec()));

        MapResultCodec mapResultCodec = new MapResultCodec(codecRegistry, new BsonTypeClassMap());
        DocumentCodec documentCodec =  new DocumentCodec(codecRegistry, new BsonTypeClassMap());

        MapResult MapExample = new MapResult("zone", zoneTest);
        Document documentExample = new Document("zone", zoneTest);


        MapResult mapResult = new MapResult();
        mapResult.put("zoneDoc", documentExample);

        Document document = new Document();
        document.put("zoneDoc", MapExample);

        mapResultCodec.encode(writer, mapResult, EncoderContext.builder().build());
        MapResult decodeMapResult = mapResultCodec.decode(new BsonBinaryReader(createInputBuffer()), DecoderContext.builder().build());
        setUp();
        documentCodec.encode(writer,document,EncoderContext.builder().build());
        Document decodeDocument = documentCodec.decode(new BsonBinaryReader(createInputBuffer()), DecoderContext.builder().build());

        assertEquals(decodeDocument.toJson(), decodeMapResult.toJson());

        BsonDocument bsonDocumentMap = mapResult.toBsonDocument(BsonDocument.class, codecRegistry);
        BsonDocument bsonDocumentDoc = document.toBsonDocument(BsonDocument.class, codecRegistry);

        assertEquals(bsonDocumentMap, bsonDocumentDoc);

        assertEquals(documentExample.values().iterator().next(), MapExample.values().iterator().next());

    }

    private BsonInput createInputBuffer() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        buffer.pipe(baos);
        return new ByteBufferBsonInput(new ByteBufNIO(ByteBuffer.wrap(baos.toByteArray())));
    }
}