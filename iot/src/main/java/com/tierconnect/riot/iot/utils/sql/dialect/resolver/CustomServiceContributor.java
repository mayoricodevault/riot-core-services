package com.tierconnect.riot.iot.utils.sql.dialect.resolver;

/**
 * Created by julio.rocha on 23-06-17.
 */
public interface CustomServiceContributor {
    /**
     * Contribute services to the indicated registry builder.
     *
     * @param serviceRegistryBuilder The builder to which services (or initiators) should be contributed.
     */
    public void contribute(CustomServiceRegistryBuilder serviceRegistryBuilder);
}
