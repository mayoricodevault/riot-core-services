package com.tierconnect.riot.api.database.utils;

import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.CollectibleCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * Created by vealaro on 12/29/16.
 */
public class ZoneCodec implements CollectibleCodec<Zone> {

    @Override
    public Zone decode(BsonReader reader, DecoderContext decoderContext) {
        reader.readStartDocument();
        String code = reader.readString("code");
        String name = reader.readString("name");
        String facilityMap = reader.readString("facilityMap");
        String zoneGroup = reader.readString("zoneGroup");
        String zoneType = reader.readString("zoneType");
        reader.readEndDocument();
        return new Zone(code, name, facilityMap, zoneGroup, zoneType);
    }

    @Override
    public void encode(BsonWriter writer, Zone value, EncoderContext encoderContext) {
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
        return Zone.class;
    }

    @Override
    public Zone generateIdIfAbsentFromDocument(Zone zone) {
        return zone;
    }

    @Override
    public boolean documentHasId(Zone document) {
        return false;
    }

    @Override
    public BsonValue getDocumentId(Zone document) {
        return null;
    }
}
