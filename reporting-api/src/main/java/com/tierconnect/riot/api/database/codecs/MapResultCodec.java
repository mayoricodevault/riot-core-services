package com.tierconnect.riot.api.database.codecs;

import org.bson.*;
import org.bson.codecs.*;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;

import java.util.*;

import static java.util.Arrays.asList;
import static org.bson.assertions.Assertions.notNull;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

/**
 * asdas
 * Created by vealaro on 12/12/16.
 */
public class MapResultCodec implements CollectibleCodec<MapResult> {

    private static final String ID_FIELD_NAME = "_id";
    private static final CodecRegistry DEFAULT_REGISTRY = fromProviders(asList(new ValueCodecProvider(),
            new BsonValueCodecProvider(),
            new MapResultCodecProvider()
    ));
    private static final BsonTypeClassMap DEFAULT_BSON_TYPE_CLASS_MAP =
            new BsonTypeClassMap(Collections.<BsonType, Class<?>>singletonMap(
                    BsonType.JAVASCRIPT_WITH_SCOPE, CodeWithScopeMap.class));

    private final BsonTypeClassMap bsonTypeClassMap;
    private final CodecRegistry registry;
    private final IdGenerator idGenerator;
    private final Transformer valueTransformer;

    public MapResultCodec() {
        this(DEFAULT_REGISTRY, DEFAULT_BSON_TYPE_CLASS_MAP);
    }

    public MapResultCodec(final CodecRegistry registry, final BsonTypeClassMap bsonTypeClassMap) {
        this(registry, bsonTypeClassMap, null);
    }

    public MapResultCodec(final CodecRegistry registry, final BsonTypeClassMap bsonTypeClassMap, final Transformer valueTransformer) {
        this.registry = notNull("registry", registry);
        this.bsonTypeClassMap = notNull("bsonTypeClassMap", bsonTypeClassMap);
        this.idGenerator = new ObjectIdGenerator();
        this.valueTransformer = valueTransformer != null ? valueTransformer : new Transformer() {
            @Override
            public Object transform(final Object value) {
                return value;
            }
        };
    }

    @Override
    public MapResult generateIdIfAbsentFromDocument(final MapResult mapResult) {
        if (!documentHasId(mapResult)) {
            mapResult.put(ID_FIELD_NAME, idGenerator.generate());
        }
        return mapResult;
    }

    @Override
    public boolean documentHasId(MapResult mapResult) {
        return mapResult.containsKey(ID_FIELD_NAME);
    }

    @Override
    public BsonValue getDocumentId(MapResult mapResult) {
        if (!documentHasId(mapResult)) {
            throw new IllegalStateException("The document does not contain an _id");
        }
        Object id = mapResult.get(ID_FIELD_NAME);
        if (id instanceof BsonValue) {
            return (BsonValue) id;
        }
        BsonDocument idHoldingDocument = new BsonDocument();
        BsonWriter writer = new BsonDocumentWriter(idHoldingDocument);
        writer.writeStartDocument();
        writer.writeName(ID_FIELD_NAME);
        writeValue(writer, EncoderContext.builder().build(), id);
        writer.writeEndDocument();
        return idHoldingDocument.get(ID_FIELD_NAME);
    }

    @Override
    public MapResult decode(BsonReader reader, DecoderContext decoderContext) {
        MapResult mapResult = new MapResult();
        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String fieldName = reader.readName();
            mapResult.put(fieldName, readValue(reader, decoderContext));
        }
        reader.readEndDocument();
        return mapResult;
    }

    @Override
    public void encode(final BsonWriter writer, final MapResult value, final EncoderContext encoderContext) {
        writeMap(writer, value, encoderContext);
    }

    @Override
    public Class<MapResult> getEncoderClass() {
        return MapResult.class;
    }

    private void writeMap(final BsonWriter writer, final Map<String, Object> map, final EncoderContext encoderContext) {
        writer.writeStartDocument();

        beforeFields(writer, encoderContext, map);

        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            if (skipField(encoderContext, entry.getKey())) {
                continue;
            }
            writer.writeName(entry.getKey());
            writeValue(writer, encoderContext, entry.getValue());
        }
        writer.writeEndDocument();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void writeValue(final BsonWriter writer, final EncoderContext encoderContext, final Object value) {
        if (value == null) {
            writer.writeNull();
        } else if (value instanceof Iterable) {
            writeIterable(writer, (Iterable<Object>) value, encoderContext.getChildContext());
        } else if (value instanceof Map) {
            writeMap(writer, (Map<String, Object>) value, encoderContext.getChildContext());
        } else {
            Codec codec = registry.get(value.getClass());
            encoderContext.encodeWithChildContext(codec, writer, value);
        }
    }

    private void writeIterable(final BsonWriter writer, final Iterable<Object> list, final EncoderContext encoderContext) {
        writer.writeStartArray();
        for (final Object value : list) {
            writeValue(writer, encoderContext, value);
        }
        writer.writeEndArray();
    }

    private void beforeFields(final BsonWriter bsonWriter, final EncoderContext encoderContext, final Map<String, Object> document) {
        if (encoderContext.isEncodingCollectibleDocument() && document.containsKey(ID_FIELD_NAME)) {
            bsonWriter.writeName(ID_FIELD_NAME);
            writeValue(bsonWriter, encoderContext, document.get(ID_FIELD_NAME));
        }
    }

    private boolean skipField(final EncoderContext encoderContext, final String key) {
        return encoderContext.isEncodingCollectibleDocument() && key.equals(ID_FIELD_NAME);
    }

    private Object readValue(final BsonReader reader, final DecoderContext decoderContext) {
        if (BsonType.NULL.equals(reader.getCurrentBsonType())) {
            reader.readNull();
            return null;
        } else if (BsonType.OBJECT_ID.equals(reader.getCurrentBsonType())) {
            ObjectId objectId = reader.readObjectId();
            return "ObjectId(\"" + objectId.toString() + "\")";
        } else if (BsonType.DOCUMENT.equals(reader.getCurrentBsonType())) {
            return decode(reader, decoderContext).toMap();
        } else if (BsonType.ARRAY.equals(reader.getCurrentBsonType())) {
            return readList(reader, decoderContext);
        } else if (BsonType.BINARY.equals(reader.getCurrentBsonType())) {
            byte bsonSubType = reader.peekBinarySubType();
            if (bsonSubType == BsonBinarySubType.UUID_LEGACY.getValue()
                    || bsonSubType == BsonBinarySubType.UUID_STANDARD.getValue()) {
                return registry.get(UUID.class).decode(reader, decoderContext);
            }
        }
        return valueTransformer.transform(registry.get(bsonTypeClassMap.get(reader.getCurrentBsonType())).decode(reader, decoderContext));
    }

    private List<Object> readList(final BsonReader reader, final DecoderContext decoderContext) {
        reader.readStartArray();
        List<Object> list = new ArrayList<Object>();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            list.add(readValue(reader, decoderContext));
        }
        reader.readEndArray();
        return list;
    }
}
