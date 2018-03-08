package com.tierconnect.riot.commons.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.tierconnect.riot.commons.actions.connections.GooglePubSubConnection;
import com.tierconnect.riot.commons.actions.ruleconfigs.GooglePubSubConfig;
import com.tierconnect.riot.commons.dtos.ActionMessageDto;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;

/**
 * Generates a ActionMessageDto for ActionExecutor
 * Created by vramos on 6/20/17.
 */
public class ActionMessageDeserializer  extends StdDeserializer<ActionMessageDto> {

    public ActionMessageDeserializer(){
        this(null);
    }

    protected ActionMessageDeserializer(Class<?> vc) {
        super(vc);
    }

    /**
     * Reads a json string a returns a ActionMessageDto.
     * "configuration" is a object type. It has different structure according to the "output".
     * "configuration" will contain a configuration any action: GooglePuSub, MqttPush, HTTPFlow.
     * "retryCount" always has a value, default 0.
     * @throws IOException throws an exception to be captured by a corresponding serde.
     */
    @Override
    public ActionMessageDto deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ActionMessageDto actionMessageDto = new ActionMessageDto();
        JsonNode node = null;
        try {
            ObjectMapper mapper = (ObjectMapper) jp.getCodec();
            SimpleDateFormat dateFormat = new SimpleDateFormat(Constants.DATE_FORMAT);
            mapper.setDateFormat(dateFormat);

            node = mapper.readTree(jp);
            JsonNode outputNode = node.get("output");
            actionMessageDto.output = outputNode.asText();

            Class configClass = null;
            Class connectionClass = null;
            switch (actionMessageDto.output){
                case com.tierconnect.riot.commons.Constants.ACTION_GOOGLE_PUBSUB_PUBLISH:
                    configClass = GooglePubSubConfig.class;
                    connectionClass = GooglePubSubConnection.class;
                    break;
            }

            actionMessageDto.connectionCode = node.get("connectionCode").asText();
            actionMessageDto.connection = mapper.treeToValue(node.get("connection"), connectionClass);
            actionMessageDto.configuration = mapper.treeToValue(node.get("configuration"), configClass);
            actionMessageDto.time = dateFormat.parse(node.get("time").asText());

            return actionMessageDto;
        }catch (Exception e){
            throw new IOException("Error parsing action message: " + node, e);
        }
    }
}
