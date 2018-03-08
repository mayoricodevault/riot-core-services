package com.tierconnect.riot.commons.actions.ruleconfigs;

import java.io.Serializable;
import java.util.Map;

/**
 * OutputAction that publish a message to Google cloud pub-sub
 * Created by vramos on 6/20/17.
 */
public class GooglePubSubConfig implements Serializable{
    public String connectionCode;
    public String topic;
    public Object data;
    public Map<String,String> attributes;
}
