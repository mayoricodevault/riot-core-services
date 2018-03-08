package com.tierconnect.riot.appcore.cache;

import org.apache.log4j.Logger;

import javax.cache.Cache;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by julio.rocha on 22-08-17.
 */
class SingleCacheManager {
    private static Logger logger = Logger.getLogger(SingleCacheManager.class);
    private static final SingleCacheManager INSTANCE = new SingleCacheManager();

    private SingleCacheManager() {
    }

    static SingleCacheManager getInstance() {
        return INSTANCE;
    }

    <K, V> boolean containsKey(String cacheName, K key, Class<K> keyType, Class<V> valueType) {
        Cache<K, V> cache = CustomCacheManager.getInstance().getCache(cacheName, keyType, valueType);
        if (isCacheNull(cache)) return false;
        return cache.containsKey(key);
    }

    <K, V> V get(String cacheName, K key, Class<K> keyType, Class<V> valueType) {
        Cache<K, V> cache = CustomCacheManager.getInstance().getCache(cacheName, keyType, valueType);
        if (isCacheNull(cache)) return null;
        return cache.get(key);
    }

    <K, V> Map<K, V> getAll(String cacheName, Class<K> keyType, Class<V> valueType) {
        Cache<K, V> cache = CustomCacheManager.getInstance().getCache(cacheName, keyType, valueType);
        if (isCacheNull(cache)) return null;
        Map<K, V> result = new LinkedHashMap<>();
        cache.iterator().forEachRemaining(e -> result.put(e.getKey(), e.getValue()));
        return result;
    }


    <K, V> void put(String cacheName, K key, V value, Class<K> keyType, Class<V> valueType) {
        Cache<K, V> cache = CustomCacheManager.getInstance().getCache(cacheName, keyType, valueType);
        if (isCacheNull(cache)) return;
        cache.put(key, value);
    }

    <K, V> void remove(String cacheName, K key, Class<K> keyType, Class<V> valueType) {
        Cache<K, V> cache = CustomCacheManager.getInstance().getCache(cacheName, keyType, valueType);
        if (isCacheNull(cache)) return;
        cache.remove(key);
    }

    <K, V> void removeAll(String cacheName, Class<K> keyType, Class<V> valueType) {
        Cache<K, V> cache = CustomCacheManager.getInstance().getCache(cacheName, keyType, valueType);
        if (isCacheNull(cache)) return;
        cache.removeAll();
    }

    private <K, V> boolean isCacheNull(Cache<K, V> cache) {
        if (cache == null) {
            logger.debug("cache does not exist");
            return true;
        }
        return false;
    }
}
