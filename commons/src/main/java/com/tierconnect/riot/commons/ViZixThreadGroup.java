package com.tierconnect.riot.commons;

import org.apache.log4j.Logger;

/**
 * Created by hugoloza on 3/1/17.
 * Thread Group to handle errors when a thread dies
 */


public class ViZixThreadGroup extends ThreadGroup {

    private static final Logger logger = Logger.getLogger( ViZixThreadGroup.class );

    public ViZixThreadGroup(String s) {
        super(s);
    }

    public void uncaughtException(Thread thread, Throwable throwable) {
        logger.info("Thread died: " + thread.getName() + " " + thread.getState() + ", exception: ",throwable);
        //throwable.printStackTrace();
    }
}
