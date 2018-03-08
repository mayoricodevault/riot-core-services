package com.tierconnect.riot.iot.utils.sql.dialect.resolver;

import org.hibernate.integrator.spi.Integrator;

/**
 * Created by julio.rocha on 23-06-17.
 */
public interface CustomServiceContributingIntegrator extends Integrator {
    public void prepareServices(CustomServiceRegistryBuilder serviceRegistryBuilder);
}
