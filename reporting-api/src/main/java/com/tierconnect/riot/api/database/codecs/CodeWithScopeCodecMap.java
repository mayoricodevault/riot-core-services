package com.tierconnect.riot.api.database.codecs;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * Encodes and decodes {@code CodeWithScope} instances.
 */
public class CodeWithScopeCodecMap implements Codec<CodeWithScopeMap> {

    private final Codec<MapResult> mapResultCode;

    /**
     * Creates a new CodeWithScopeCodec.
     *
     * @param mapResultCode a Codec for encoding and decoding the {@link org.bson.types.CodeWithScope#getScope()}.
     */
    public CodeWithScopeCodecMap(final Codec<MapResult> mapResultCode) {
        this.mapResultCode = mapResultCode;
    }

    @Override
    public CodeWithScopeMap decode(BsonReader reader, DecoderContext decoderContext) {
        String code = reader.readJavaScriptWithScope();
        MapResult scope = mapResultCode.decode(reader, decoderContext);
        return new CodeWithScopeMap(code, scope);
    }

    @Override
    public void encode(BsonWriter writer, CodeWithScopeMap value, EncoderContext encoderContext) {
        writer.writeJavaScriptWithScope(value.getCode());
        mapResultCode.encode(writer, value.getScope(), encoderContext);
    }

    @Override
    public Class<CodeWithScopeMap> getEncoderClass() {
        return CodeWithScopeMap.class;
    }
}
