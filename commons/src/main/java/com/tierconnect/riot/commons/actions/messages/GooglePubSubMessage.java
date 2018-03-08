package com.tierconnect.riot.commons.actions.messages;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by vramos on 21-07-17.
 */
public class GooglePubSubMessage implements Serializable {
    public String data;
    public Map<String,String> attributes;
    public String messageId;
    public String publishTime;
}
