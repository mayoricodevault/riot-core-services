package com.tierconnect.riot.api.mongoShell.parsers;

import org.bson.BsonArray;
import org.bson.Document;
import org.bson.codecs.*;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonReader;

import static java.util.Arrays.asList;

/**
 * Created by achambi on 11/30/16.
 * A class to parse Mongo result.
 */
public class MongoParser {

    private MongoParser(){}

    private final static CodecRegistry codecRegistry = CodecRegistries.fromProviders(asList(new ValueCodecProvider(),
            new BsonValueCodecProvider(),
            new DocumentCodecProvider()));

    /**
     * Parse A json string to array Bson
     *
     * @param json the string in json format to parse.
     * @return a {@link BsonArray} to contains the result parsed.
     */
     static BsonArray parseBsonArray(String json) {
        JsonReader reader = new JsonReader(json);
        BsonArrayCodec arrayReader = new BsonArrayCodec(codecRegistry);
        return arrayReader.decode(reader, DecoderContext.builder().build());
    }

    /**
     * Method to parse a single Json String to Document.
     *
     * @param json {@link String} A string containing a simple json.
     * @return a {@link Document} the document to return where all fields are in Mongo data types.
     */
    public static Document parseDocument(String json) {
        return Document.parse(json);
    }

}
