package com.tierconnect.riot.appcore.cache;

import org.apache.log4j.Logger;

import javax.cache.event.*;
import java.io.Serializable;

/**
 * Created by julio.rocha on 25-08-17.
 */
public class CacheChangedListener<K, V> implements CacheEntryCreatedListener<K, V>,
        CacheEntryExpiredListener<K, V>,
        CacheEntryUpdatedListener<K, V>,
        CacheEntryRemovedListener<K, V>,
        Serializable {

    private static Logger logger = Logger.getLogger(CacheChangedListener.class);

    private final String cacheName;

    public CacheChangedListener(String cacheName) {
        this.cacheName = cacheName;
    }

    @Override
    public void onCreated(Iterable<CacheEntryEvent<? extends K, ? extends V>> cacheEntryEvents) throws CacheEntryListenerException {
        logEvent(cacheEntryEvents);
    }

    @Override
    public void onExpired(Iterable<CacheEntryEvent<? extends K, ? extends V>> cacheEntryEvents) throws CacheEntryListenerException {
        logEvent(cacheEntryEvents);
    }

    @Override
    public void onRemoved(Iterable<CacheEntryEvent<? extends K, ? extends V>> cacheEntryEvents) throws CacheEntryListenerException {
        logEvent(cacheEntryEvents);
    }

    @Override
    public void onUpdated(Iterable<CacheEntryEvent<? extends K, ? extends V>> cacheEntryEvents) throws CacheEntryListenerException {
        logEvent(cacheEntryEvents);
    }

    private void logEvent(Iterable<CacheEntryEvent<? extends K, ? extends V>> cacheEntryEvents) {
        cacheEntryEvents.forEach(e -> logger.info("cacheName=" + cacheName + " eventType=" + e.getEventType() +
                " key=" + e.getKey() + " currentValue=" + e.getOldValue() + " oldValue=" + e.getValue()));
    }
}
