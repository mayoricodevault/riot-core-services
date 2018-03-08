package com.tierconnect.riot.api.database.codecs;


import org.bson.BsonDocument;
import org.bson.BsonDocumentWrapper;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.Encoder;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriter;
import org.bson.json.JsonWriterSettings;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.bson.assertions.Assertions.notNull;

/**
 * A representation of a document as a {@code Map}.
 * Created by vealaro on 12/12/16.
 */
public class MapResult implements Map<String, Object>, Serializable, Bson {

    private static final long serialVersionUID = -8354856663226386836L;

    private final LinkedHashMap<String, Object> result;

    /**
     * creates an empty Map instance.
     */
    public MapResult() {
        result = new LinkedHashMap<String, Object>();
    }

    /**
     * Create an Map instance initialized with the given key/value pair.
     */
    public MapResult(final String key, final Object value) {
        result = new LinkedHashMap<String, Object>();
        result.put(key, value);
    }

    @Override
    public <TDocument> BsonDocument toBsonDocument(Class<TDocument> tDocumentClass, CodecRegistry codecRegistry) {
        return new BsonDocumentWrapper<MapResult>(this, codecRegistry.get(MapResult.class));
    }

    /**
     * Put the given key/value pair into this MapResult and return this.  Useful for chaining puts in a single expression, e.g.
     * <pre>
     * doc.append("a", 1).append("b", 2)}
     * </pre>
     *
     * @param key   key
     * @param value value
     * @return this
     */
    public MapResult append(final String key, final Object value) {
        result.put(key, value);
        return this;
    }

    public <T> T get(final Object key, final Class<T> clazz) {
        notNull("clazz", clazz);
        return clazz.cast(result.get(key));
    }

    public Map<String, Object> toMap() {
        return result;
    }

    public String toJson() {
        return toJson(new JsonWriterSettings());
    }

    public String toJson(final JsonWriterSettings writerSettings) {
        return toJson(writerSettings, new MapResultCodec());
    }

    public String toJson(final Encoder<MapResult> encoder) {
        return toJson(new JsonWriterSettings(), encoder);
    }

    public String toJson(final JsonWriterSettings writerSettings, final Encoder<MapResult> encoder) {
        JsonWriter writer = new JsonWriter(new StringWriter(), writerSettings);
        encoder.encode(writer, this, EncoderContext.builder().isEncodingCollectibleDocument(true).build());
        return writer.getWriter().toString();
    }

    @Override
    public int size() {
        return result.size();
    }

    @Override
    public boolean isEmpty() {
        return result.isEmpty();
    }

    @Override
    public boolean containsValue(Object value) {
        return result.containsValue(value);
    }

    @Override
    public boolean containsKey(final Object key) {
        return result.containsKey(key);
    }

    @Override
    public Object get(final Object key) {
        return result.get(key);
    }

    @Override
    public Object put(final String key, final Object value) {
        return result.put(key, value);
    }

    @Override
    public Object remove(final Object key) {
        return result.remove(key);
    }

    @Override
    public void putAll(final Map<? extends String, ?> map) {
        result.putAll(map);
    }

    @Override
    public void clear() {
        result.clear();
    }

    @Override
    public Set<String> keySet() {
        return result.keySet();
    }

    @Override
    public Collection<Object> values() {
        return result.values();
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return result.entrySet();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MapResult mapResult = (MapResult) o;
        return result.equals(mapResult.result);
    }

    @Override
    public int hashCode() {
        return result.hashCode();
    }

    @Override
    public String toString() {
        return "" + result;
    }
}
