package com.tierconnect.riot.commons.actions.executors;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.actions.ruleconfigs.GooglePubSubConfig;
import com.tierconnect.riot.commons.dtos.ActionMessageDto;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores a map of executors.
 * This means, it will exist one executor instance per connection.
 * Delegates the logic of executor to the classes that inherit OutputActionExecutor
 * Created by vramos on 07-07-17.
 */
public abstract class OutputActionExecutor {
    private static final Object lock = new Object();
    private static Map<Integer,OutputActionExecutor> executors;

    /**
     * Return the executor associated to a connection
     * @param actionMessage A message
     * @return the executor instance
     * @throws Exception
     */
    public static OutputActionExecutor get(ActionMessageDto actionMessage, Map<String,Object> properties) throws Exception{
        if(executors == null){
            synchronized (lock){
                if(executors == null){
                    executors = new HashMap<>();
                }
            }
        }
        int key = getKey(actionMessage);
        OutputActionExecutor executor = executors.get(key);
        if(executor == null){
            switch (actionMessage.output){
                case Constants.ACTION_GOOGLE_PUBSUB_PUBLISH :
                    executor = new GooglePubSubExecutor(actionMessage, properties);
                    executors.put(key,executor);
                    break;
            }
        }
        return executor;
    }

    /**
     * Executes the action for a message
     * @param config the message configuration.
     * @throws Exception
     */
    public abstract void execute(Object config) throws Exception;

    /**
     * Calculate a unique key per message. (Add the description for every action type)
     *  - Google pubsub generates a key per connection and topic.
     *
     * @return
     */
    private static int getKey(ActionMessageDto actionMessage){
        int key = 0;
        switch (actionMessage.output) {
            case Constants.ACTION_GOOGLE_PUBSUB_PUBLISH:
                GooglePubSubConfig gConfig = (GooglePubSubConfig)actionMessage.configuration;
                key = actionMessage.connection.hashCode() + gConfig.topic.hashCode();
                break;
        }
        return key;
    }

}
