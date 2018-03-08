package com.tierconnect.riot.api.database.utils;

import com.mongodb.DBRef;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import static com.mongodb.assertions.Assertions.notNull;

/**
 * Created by vealaro on 12/30/16.
 */
public class ZoneCodeRegistry implements Codec<Zone> {

    private final CodecRegistry registry;

    public ZoneCodeRegistry(final CodecRegistry registry) {
        this.registry = notNull("registry", registry);
    }

    @Override
    public Zone decode(final BsonReader reader, final DecoderContext decoderContext) {
        throw new UnsupportedOperationException("DBRefCodec does not support decoding");
    }

    @Override
    public void encode(final BsonWriter writer, final Zone value, final EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeString("code", value.getCode());
        writer.writeString("name", value.getName());
        writer.writeString("facilityMap", value.getFacilityMap());
        writer.writeString("zoneGroup", value.getZoneGroup());
        writer.writeString("zoneType", value.getZoneType());
        writer.writeEndDocument();
    }

    @Override
    public Class<Zone> getEncoderClass() {
        return null;
    }
}
