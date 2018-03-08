package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import org.apache.log4j.Logger;

/**
 * Created by vealaro on 8/16/16.
 */
public class SecondLevelCacheService {

    static private Logger logger = Logger.getLogger(SecondLevelCacheService.class);

    static private SecondLevelCacheService INSTANCE = new SecondLevelCacheService();

    public static SecondLevelCacheService getInstance() {
        return INSTANCE;
    }

    /**
     *
     */
    public void clearCache() {
        logger.debug("clear entityRegions");
        HibernateSessionFactory.getInstance().getCache().evictEntityRegions();
        logger.debug("Clear Collection Regions");
        HibernateSessionFactory.getInstance().getCache().evictCollectionRegions();
        // enable if necessary clean querys cache - VEAC

//        logger.debug("Clear Default Query Region");
//        HibernateSessionFactory.getInstance().getCache().evictDefaultQueryRegion();
//        logger.debug("Clear Query Regions");
//        HibernateSessionFactory.getInstance().getCache().evictQueryRegions();
    }
}
