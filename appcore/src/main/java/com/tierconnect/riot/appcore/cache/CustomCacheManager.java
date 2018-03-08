package com.tierconnect.riot.appcore.cache;

import com.hazelcast.cache.HazelcastCachingProvider;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.client.config.XmlClientConfigBuilder;
import com.hazelcast.config.Config;
import com.hazelcast.config.GroupConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.tierconnect.riot.appcore.utils.Configuration;
import org.apache.log4j.Logger;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Factory;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableCacheEntryListenerConfiguration;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Created by julio.rocha on 22-08-17.
 */
class CustomCacheManager {
    private static Logger logger = Logger.getLogger(CustomCacheManager.class);

    private static final CustomCacheManager INSTANCE = new CustomCacheManager();

    private CachingProvider cachingProvider;
    private CacheManager cacheManager;

    private CustomCacheManager() {
        logger.debug("Getting Provider and Initializing Cache");
        initCache(true);
    }

    static CustomCacheManager getInstance() {
        return INSTANCE;
    }

    private void initCache(boolean asServer) {
        if (asServer)
            initCacheAsServer();
        else
            initCacheAsClient();
    }

    private void initCacheAsClient() {
        System.setProperty("hazelcast.jcache.provider.type", "client");
        ClientConfig clientConfig = new XmlClientConfigBuilder().build();
        clientConfig.setGroupConfig(createGroupConfig());
        clientConfig.setProperty("hazelcast.client.config", Configuration.getProperty("hibernate.cache.hazelcast.client.config"));
        clientConfig.setProperty("hazelcast.native_client_address", Configuration.getProperty("hibernate.cache.hazelcast.native_client_address"));
        clientConfig.setProperty("hazelcast.use_native_client", Configuration.getProperty("hibernate.cache.hazelcast.use_native_client"));
        ClientNetworkConfig networkConfig = clientConfig.getNetworkConfig();
        networkConfig.addAddress(Configuration.getProperty("hibernate.cache.hazelcast.native_client_address"));
        HazelcastInstance instance = HazelcastClient.newHazelcastClient(clientConfig);
        Properties properties = new Properties();
        properties.setProperty(HazelcastCachingProvider.HAZELCAST_INSTANCE_NAME, instance.getName());
        this.cachingProvider = Caching.getCachingProvider();
        URI cacheManagerName;
        try {
            cacheManagerName = new URI("services-cache");
        } catch (URISyntaxException e) {
            cacheManagerName = this.cachingProvider.getDefaultURI();
        }
        this.cacheManager = cachingProvider.getCacheManager(cacheManagerName, null, properties);
    }

    private void initCacheAsServer() {
        System.setProperty("hazelcast.jcache.provider.type", "server");
        Config config = new Config();
        config.setInstanceName("services-jcache");
        String enableMC = Configuration.getProperty("vizix.hazelcast.managementcenter.enable");
        if (enableMC != null && Boolean.valueOf(enableMC)) {
            config.getManagementCenterConfig().setEnabled(true);
            config.getManagementCenterConfig().setUrl(Configuration.getProperty("vizix.hazelcast.managementcenter.url"));
        }
        String enableMulticast = Configuration.getProperty("vizix.hazelcast.services.multicast.enable");
        if (enableMulticast != null && Boolean.valueOf(enableMulticast)) {
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);
        } else {
            config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        }
        config.getNetworkConfig().getJoin().getMulticastConfig().setMulticastGroup("224.9.7.3");
        config.getNetworkConfig().getJoin().getMulticastConfig().setMulticastPort(55438);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getAwsConfig().setEnabled(false);
        config.getNetworkConfig().setPort(7701).setPortAutoIncrement(true).setPortCount(200);
        config.setGroupConfig(createGroupConfig());
        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);
        URI cacheManagerName;
        this.cachingProvider = Caching.getCachingProvider();
        try {
            cacheManagerName = new URI("services-cache");
        } catch (URISyntaxException e) {
            cacheManagerName = this.cachingProvider.getDefaultURI();
        }
        Properties properties = new Properties();
        properties.setProperty(HazelcastCachingProvider.HAZELCAST_INSTANCE_NAME, instance.getName());
        this.cacheManager = cachingProvider.getCacheManager(cacheManagerName, null, properties);
    }

    private GroupConfig createGroupConfig() {
        return new GroupConfig(Configuration.getProperty("hibernate.cache.hazelcast.native_client_group"),
                Configuration.getProperty("hibernate.cache.hazelcast.native_client_password"));
    }

    /**
     * @return list of created caches
     */
    List<String> cacheNames() {
        List<String> caches = new ArrayList<>();
        Iterable<String> names = this.cacheManager.getCacheNames();
        names.forEach(caches::add);
        return caches;
    }

    /**
     * @param cacheName
     * @return true if the cache with 'cacheName' exists
     */
    boolean cacheExists(String cacheName) {
        return cacheNames().contains(cacheName);
    }

    /**
     * Shutdowns the cache service
     */
    void shutdown() {
        this.cacheManager.close();
        this.cachingProvider.close();
    }

    /**
     * @param cacheName
     * @param configuration
     * @param keyType
     * @param valueType
     * @param <K>
     * @param <V>
     * @return true if the cache with name 'cacheName' could be created
     */
    <K, V> boolean createCache(String cacheName, CacheConfiguration configuration,
                               Class<K> keyType, Class<V> valueType) {
        if (configuration == null) {
            configuration = new CacheConfiguration();
        }
        MutableConfiguration<K, V> mutableConfiguration = new MutableConfiguration<>();
        if (cacheNames().contains(cacheName)) {
            this.cacheManager.enableManagement(cacheName, configuration.isManagementEnabled());
            this.cacheManager.enableStatistics(cacheName, configuration.isStatisticsEnabled());
            return false;
        }

        Factory<AccessedExpiryPolicy> accessedExpiryPolicyFactory = calculateExpiryPolicy(configuration);

        mutableConfiguration = mutableConfiguration
                .setStoreByValue(configuration.isStoreByValue())
                .setTypes(keyType, valueType)
                .setManagementEnabled(configuration.isManagementEnabled())
                .setStatisticsEnabled(configuration.isStatisticsEnabled())
                .setExpiryPolicyFactory(accessedExpiryPolicyFactory)
                .setReadThrough(configuration.isReadThrough())
                .setWriteThrough(configuration.isWriteThrough());
        Cache<K, V> cache = this.cacheManager.createCache(cacheName, mutableConfiguration);
        registerListener(cache);
        return true;
    }

    /**
     * @param cacheName
     * @param keyType
     * @param valueType
     * @param <K>
     * @param <V>
     * @return the cache with name 'cacheName'
     */
    <K, V> Cache<K, V> getCache(String cacheName, Class<K> keyType, Class<V> valueType) {
        return this.cacheManager.getCache(cacheName, keyType, valueType);
    }

    /**
     * Deletes the cache with name 'cacheName'
     *
     * @param cacheName
     */
    void delete(String cacheName) {
        this.cacheManager.destroyCache(cacheName);
    }

    private Factory<AccessedExpiryPolicy> calculateExpiryPolicy(CacheConfiguration configuration) {
        return configuration.getExpiryForAccess() == 0L ? null : FactoryBuilder.factoryOf(
                new AccessedExpiryPolicy(
                        new Duration(
                                TimeUnit.MILLISECONDS, configuration.getExpiryForAccess()
                        )
                )
        );
    }

    private <K, V> void registerListener(Cache<K, V> cache) {
        CacheChangedListener<K, V> listener = new CacheChangedListener<>(cache.getName());
        Factory<CacheChangedListener<K, V>> listenerFactory = FactoryBuilder.factoryOf(listener);
        MutableCacheEntryListenerConfiguration configuration = new MutableCacheEntryListenerConfiguration(listenerFactory,
                null, true, true);
        cache.registerCacheEntryListener(configuration);
    }
}
