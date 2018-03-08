package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.tierconnect.riot.commons.serializers.ActionMessageDeserializer;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 *
 * Example:
 *  {
 *      "output": "GooglePubSubPublish",
 *      "connectionCode": "GooglePubSub",
 *      "connection": {
 *          "type": "service_account",
 *          "project_id": "vizix-1223",
 *          "private_key_id": "db4a1f325f211d614c25eba9a819975cf31bf652",
 *          "private_key": "-----BEGIN PRIVATE KEY-----"
 *      },
 *      "configuration": {
 *          "connectionCode": "GPubSub",
 *          "topic": "TestViZix",
 *          "payload": "Message body serial=000000000000000000001 lastDetectTime=1499698614818",
 *          "attributes": {
 *              "epc": "000000000000000000001",
 *              "zone_entered": "Po1"
 *          }
 *      }
 *      "time": "2017-05-08 15:14:69Z"
 *  }
 *
 * Created by vramos on 6/19/17.
 */
@JsonDeserialize(using = ActionMessageDeserializer.class)
public class ActionMessageDto implements Serializable{
    public String output;
    public String connectionCode;
    public Object connection;
    public Object configuration;
    public Date time;
}
