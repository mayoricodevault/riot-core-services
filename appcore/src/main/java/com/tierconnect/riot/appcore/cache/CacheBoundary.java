package com.tierconnect.riot.appcore.cache;

import org.apache.log4j.Logger;

import java.util.List;
import java.util.Map;

/**
 * Created by julio.rocha on 22-08-17.
 */
public class CacheBoundary {
    private static Logger logger = Logger.getLogger(CacheBoundary.class);
    private static final CacheBoundary INSTANCE = new CacheBoundary();

    /**
     * Construct the singleton object and initializes the cache
     */
    private CacheBoundary() {
        logger.info(CustomCacheManager.getInstance().cacheNames());
    }

    public static CacheBoundary getInstance() {
        return INSTANCE;
    }

    /**
     * @param cacheName
     * @param keyType
     * @param valueType
     * @param <K>
     * @param <V>
     * @return true if a cache with name 'cacheName' could be created with default configurations
     */
    public <K, V> boolean createCache(String cacheName, Class<K> keyType, Class<V> valueType) {
        return createCache(cacheName, null, keyType, valueType);
    }

    /**
     * @param cacheName
     * @param configuration
     * @param keyType
     * @param valueType
     * @param <K>
     * @param <V>
     * @return true if a cache with name 'cacheName' could be created
     */
    public <K, V> boolean createCache(String cacheName, CacheConfiguration configuration,
                                      Class<K> keyType, Class<V> valueType) {
        return CustomCacheManager.getInstance().createCache(cacheName, configuration, keyType, valueType);
    }

    /**
     * @param cacheName
     * @param configuration
     * @return true if a cache with name 'cacheName' of key type String and value Object could be created
     */
    public boolean createCache(String cacheName, CacheConfiguration configuration) {
        return createCache(cacheName, configuration, String.class, Object.class);
    }

    /**
     * @return list of created caches
     */
    public List<String> cacheNames() {
        return CustomCacheManager.getInstance().cacheNames();
    }

    /**
     * @param cacheName
     * @return true if the cache with 'cacheName' exists
     */
    public boolean cacheExists(String cacheName) {
        return CustomCacheManager.getInstance().cacheExists(cacheName);
    }

    /**
     * Deletes the cache with the name 'cacheName'
     *
     * @param cacheName
     */
    public void delete(String cacheName) {
        CustomCacheManager.getInstance().delete(cacheName);
    }

    public <K, V> boolean containsKey(String cacheName, K key, Class<K> keyType, Class<V> valueType) {
        return SingleCacheManager.getInstance().containsKey(cacheName, key, keyType, valueType);
    }

    public <K, V> V get(String cacheName, K key, Class<K> keyType, Class<V> valueType) {
        return SingleCacheManager.getInstance().get(cacheName, key, keyType, valueType);
    }

    public <K, V> void put(String cacheName, K key, V value, Class<K> keyType, Class<V> valueType) {
        SingleCacheManager.getInstance().put(cacheName, key, value, keyType, valueType);
    }

    public <K, V> void remove(String cacheName, K key, Class<K> keyType, Class<V> valueType) {
        SingleCacheManager.getInstance().remove(cacheName, key, keyType, valueType);
    }

    public <K, V> void removeAll(String cacheName, Class<K> keyType, Class<V> valueType) {
        SingleCacheManager.getInstance().removeAll(cacheName, keyType, valueType);
    }

    public <K, V> Map<K, V> getAll(String cacheName, Class<K> keyType, Class<V> valueType) {
        return SingleCacheManager.getInstance().getAll(cacheName, keyType, valueType);
    }

    /**
     * @param cacheName
     * @param key
     * @return value of type Object if a cache with name 'cacheName' of key type String and value Object exists
     */
    public Object get(String cacheName, String key) {
        return get(cacheName, key, String.class, Object.class);
    }

    /**
     * Put a value of type Object if a cache with name 'cacheName' of key type String and value Object exists
     *
     * @param cacheName
     * @param key
     * @param value
     */
    public void put(String cacheName, String key, Object value) {
        put(cacheName, key, value, String.class, Object.class);
    }

    /**
     * Remove a value of type Object if a cache with name 'cacheName' of key type String and value Object exists
     *
     * @param cacheName
     * @param key
     */
    public void remove(String cacheName, String key) {
        remove(cacheName, key, String.class, Object.class);
    }

    /**
     * Remove all values of type Object if a cache with name 'cacheName' of key type String and value Object exists
     *
     * @param cacheName
     */
    public void removeAll(String cacheName) {
        removeAll(cacheName, String.class, Object.class);
    }
}
