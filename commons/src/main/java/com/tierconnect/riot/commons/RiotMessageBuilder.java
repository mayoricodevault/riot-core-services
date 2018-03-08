package com.tierconnect.riot.commons;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.commons.dtos.MetaDto;
import com.tierconnect.riot.commons.dtos.ThingDto;
import com.tierconnect.riot.commons.dtos.ThingPropertyDto;
import com.tierconnect.riot.commons.dtos.ThingTypeDto;
import com.tierconnect.riot.commons.services.broker.MqttPublisher;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.tierconnect.riot.commons.serializers.Constants.DATE_FORMAT;

/**
 * RiotMessageBuilder class.
 *
 * @author jantezana
 * @version 2017/02/2017
 */
public class RiotMessageBuilder {
    private static final Logger logger = Logger.getLogger(RiotMessageBuilder.class);
    private RiotMessage riotMessage;
    private ObjectMapper objectMapper;
    private long messageSize;

    public RiotMessageBuilder() {
        this(50);
    }

    public RiotMessageBuilder(long messageSize) {
        this.riotMessage = new RiotMessage();

        // Initialize date format.
        this.objectMapper = new ObjectMapper();
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        this.objectMapper.setDateFormat(dateFormat);
        this.messageSize = messageSize;
    }

    public String getSerialNumber() {
        return riotMessage.getSerialNumber();
    }

    public String getThingTypeCode() {
        return riotMessage.getThingTypeCode();
    }

    public RiotMessageBuilder setBridgeCode(String bridgeCode) {
        this.riotMessage.setBridgeCode(bridgeCode);
        return this;
    }

    public RiotMessageBuilder setSqn(long sqn) {
        this.riotMessage.setSqn(sqn);
        return this;
    }

    public RiotMessageBuilder setSpecName(String specName) {
        this.riotMessage.setSpecName(specName);
        return this;
    }

    public RiotMessageBuilder setRuleExecutionMode(String ruleExecutionMode) {
        this.riotMessage.setRuleExecutionMode(ruleExecutionMode);
        return this;
    }

    public RiotMessageBuilder setOrigin(List<Double> origin) {
        this.riotMessage.setOrigin(origin);
        return this;
    }

    public RiotMessageBuilder setUnits(String units) {
        this.riotMessage.setUnits(units);
        return this;
    }

    public RiotMessageBuilder setRunRules(Boolean runRules) {
        this.riotMessage.setRunRules(runRules);
        return this;
    }

    public RiotMessageBuilder setSerialNumber(String serialNumber) {
        this.riotMessage.setSerialNumber(serialNumber);
        return this;
    }

    public RiotMessageBuilder setTime(long time) {
        this.riotMessage.setTime(time);
        return this;
    }

    public RiotMessageBuilder setThingTypeCode(String thingTypeCode) {
        this.riotMessage.setThingTypeCode(thingTypeCode);
        return this;
    }

    public RiotMessageBuilder setProperties(Map<String, RiotMessage.Property> properties) {
        this.riotMessage.setProperties(properties);
        return this;
    }

    public Map<String, RiotMessage.Property> getProperties() {
        return this.riotMessage.getProperties();
    }

    public RiotMessageBuilder addProperty(long time,
                                          String name,
                                          Object value) {
        if (time > this.riotMessage.getTime()) {
            this.riotMessage.setTime(time);
        }

        this.riotMessage.addProperty(time, name, value);
        return this;
    }

    /**
     * Builds a riot message by format
     *
     * @param format the value of format (CSV or JSON)
     * @return the riot message
     */
    public String build(final String format) {

        String result = null;
        switch (StringUtils.upperCase(format)) {
            case "CSV": {
                result = buildRiotMessageCSV();
                break;
            }
            case "JSON": {
                result = buildRiotMessageJSON();
                break;
            }
            default: {
                break;
            }
        }
        return result;
    }

    public void buildAndPublish(final String format, MqttPublisher mqttPublisher, String topic){
        switch (StringUtils.upperCase(format)) {
            case "CSV": {
                buildMultipleRiotMessageCSV(mqttPublisher, topic);
                break;
            }
        }
    }

    /**
     * Builds the thing DTO.
     *
     * @return the thing DTO
     */
    public ThingDto buildThingDTO() {
        return buildRiotMessageObject();
    }

    /**
     * Builds riot message object.
     *
     * @return the thing DTO
     */
    private ThingDto buildRiotMessageObject() {
        ThingDto thingDto = new ThingDto();

        // Build Meta DTO.
        MetaDto metaDto = new MetaDto();
        metaDto.bridgeCode = this.riotMessage.getBridgeCode();
        metaDto.sqn = this.riotMessage.getSqn();
        metaDto.specName = this.riotMessage.getSpecName();
        if (this.riotMessage.getOrigin() != null) {
            metaDto.origin = this.riotMessage.getOrigin().stream().toArray(value -> new Double[this.riotMessage.getOrigin().size()]);
        }
        metaDto.units = this.riotMessage.getUnits();
        metaDto.runRules = this.riotMessage.isRunRules();
        thingDto.meta = metaDto;

        thingDto.serialNumber = this.riotMessage.getSerialNumber();
        thingDto.time = new Date(this.riotMessage.getTime());

        // Builds the thing type DTO.
        ThingTypeDto thingTypeDto = new ThingTypeDto();
        thingTypeDto.code = this.riotMessage.getThingTypeCode();
        thingDto.thingType = thingTypeDto;

        thingDto.properties = new LinkedList<>();
        Map<String, RiotMessage.Property> properties = this.riotMessage.getProperties();
        Map<String, ThingPropertyDto> data = new HashMap();
        for (RiotMessage.Property property : properties.values()) {
            ThingPropertyDto thingPropertyDto = new ThingPropertyDto();
            thingPropertyDto.time = new Date(property.getTime());
            thingPropertyDto.value = property.getValue();
            data.put(property.getName(), thingPropertyDto);
        }

        thingDto.properties.add(data);

        return thingDto;
    }

    /**
     * Builds a riot message in CSV format.
     *
     * @return the riot message in CSV format
     */
    private String buildRiotMessageCSV() {
        StringBuilder builder = new StringBuilder();
        // Builds the header
        builder.append(String.format("sn,%d,%s", this.riotMessage.getSqn(), this.riotMessage.getSpecName())).append("\n");
        if (this.riotMessage.getOrigin() != null && !this.riotMessage.getOrigin().isEmpty()) {
            if (this.riotMessage.getOrigin().size() == 4) {
                builder.append(String.format(",0,___CS___,%d;%d;%d;%d;%s", this.riotMessage.getOrigin().get(0), this.riotMessage.getOrigin().get(1),
                                             this.riotMessage.getOrigin().get(2), this.riotMessage.getOrigin().get(3),
                                             this.riotMessage.getUnits())).append("\n");
            } else {
                logger.error("Invalid origin format");
            }
        } else {
            logger.warn("The message not contains origin");
        }

        if (this.riotMessage.getProperties() != null || !this.riotMessage.getProperties().isEmpty()) {
            for (RiotMessage.Property property : this.riotMessage.getProperties().values()) {
                builder.append(
                    String.format("%s,%d,%s,%s", this.riotMessage.getSerialNumber(), property.getTime(), property.getName(), property.getValue()));
            }
        }

        String message = builder.toString();
        logger.info(String.format("Message: %s", message));

        return message;
    }

    /**
     * Builds a riot message in JSON format.
     *
     * @return the riot message in json format
     */
    private String buildRiotMessageJSON() {
        ThingDto thingDto = this.buildRiotMessageObject();
        String message = null;
        try {
            message = this.objectMapper.writeValueAsString(thingDto);
            logger.info(String.format("message: %s", message));
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage(), e);
        }

        return message;
    }

    /**
     * build multiple messages and returned into a List
     * based on a message size 'messageSize'
     * @return
     * @param mqttPublisher
     * @param topic
     */
    private void buildMultipleRiotMessageCSV(MqttPublisher mqttPublisher, String topic) {
        long sqn = 0;

        if (this.riotMessage.getProperties() != null || !this.riotMessage.getProperties().isEmpty()) {
            StringBuilder builderPartial = null;
            long counter = 0;
            for (Map.Entry<String, RiotMessage.Property> entry: this.riotMessage.getProperties().entrySet()){
                // Builds the header
                if (counter == 0){
                    sqn++;
                    builderPartial = new StringBuilder();
                    setMessageHeader(builderPartial, sqn);
                }
                RiotMessage.Property property = entry.getValue();
                String serialNumber = entry.getKey();
                builderPartial.append(
                        String.format("%s,%d,%s,%s", serialNumber, property.getTime(), property.getName(), property.getValue())).append("\n");
                counter++;
                if (counter >= messageSize){
                    // publish messages while processing
                    mqttPublisher.publish(topic, builderPartial.toString());
                    counter=0;
                }
            }
            if (counter > 0){
                mqttPublisher.publish(topic, builderPartial.toString());
            }
        }
    }

    private void setMessageHeader(StringBuilder builder, long sqn){
        builder.append(String.format("sn,%d,%s", sqn, this.riotMessage.getSpecName())).append("\n");
        // set the __ruleExecutionMode__ header
        switch (this.riotMessage.getRuleExecutionMode()){
            case "scheduledRule": case "coreBridge":
                builder.append(String.format("___ruleExecutionMode___,%s", this.riotMessage.getRuleExecutionMode())).append("\n");
                break;
        }

    }
}
