package com.tierconnect.riot.commons.utils;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;

import static com.tierconnect.riot.commons.Constants.DEFAULT_SEPARATOR;
import static com.tierconnect.riot.commons.Constants.KAFKA_SEPARATOR;

/**
 * Topics enumerator.
 */
public enum Topics {

    //Topic for KTables
    CACHE_THING_TYPE("/v1/cache/thingtype", "store_thingtype"),
    CACHE_THING("/v1/cache/things", "store_things"),
    CACHE_ZONE("/v1/cache/zone", "store_zone"),
    CACHE_ZONE_TYPE("/v1/cache/zonetype", "store_zonetype"),
    CACHE_GROUP("/v1/cache/group", "store_group"),
    CACHE_GROUP_TYPE("/v1/cache/grouptype", "store_grouptype"),
    CACHE_SHIFT("/v1/cache/shift", "store_shift"),
    CACHE_SHIFT_ZONE("/v1/cache/shiftzone", "store_shiftzone"),
    CACHE_LOGICAL_READER("/v1/cache/logicalreader", "store_logicalreader"),
    CACHE_LOCALMAP("/v1/cache/localmap", "store_localmap"),
    CACHE_ZONE_PROPERTIES("/v1/cache/zoneproperty", "store_zoneproperty"),
    CACHE_EDGEBOX("/v1/cache/edgebox", "store_edgebox"),
    CACHE_EDGEBOX_RULE("/v1/cache/edgeboxrule", "store_edgeboxrule"),
    CACHE_CONNECTION("/v1/cache/connection", "store_connection"),
    CACHE_EDGEBOXES_CONFIGURATION("/v1/cache/edgeboxconfiguration", "store_edgeboxconfiguration"),

    //topic for KStream
    DATA_1("/v1/data1", "store_data1"),

    // data output topic of kafka core bridge.
    DATA_2("/v1/data2", "store_data2"),

    BROKER_CHECK("/v1/broker/check", "");

    private String name;
    private String store;

    /**
     * Constructor of {@link com.tierconnect.riot.commons.utils}
     *
     * @param name the topic name
     */
    Topics(final String name,
           final String store) {
        Preconditions.checkNotNull(name, "The topic name is null");
        this.name = name;
        this.store = store;
    }

    /**
     * Gets the topic name.
     *
     * @return the topic name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the topic name for kafka.
     * A kafka topic uses '___' instead '/'
     * @return the formatted name
     */
    public String getKafkaName() {
        final String formattedName = StringUtils.replace(name, DEFAULT_SEPARATOR, KAFKA_SEPARATOR);
        return formattedName;
    }

    /**
     * Gets the state store name for the topic.
     *
     * @return
     */
    public String getStore() {
        return store;
    }
}
