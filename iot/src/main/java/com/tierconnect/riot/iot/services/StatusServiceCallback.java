package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.commons.utils.Timer;

import java.util.concurrent.ConcurrentHashMap;

/**
 * StatusServiceCallback interface
 *
 * @author fflores
 * @author jantezana
 * @version 2017/06/07
 */
public interface StatusServiceCallback {

    /**
     * Gets the map of timers.
     *
     * @return the map of timers
     */
    ConcurrentHashMap<String, Timer> getTimers();
}
