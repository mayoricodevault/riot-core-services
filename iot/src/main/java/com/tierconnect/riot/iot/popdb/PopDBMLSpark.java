package com.tierconnect.riot.iot.popdb;

import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.appcore.utils.MlConfiguration;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by pablo on 7/13/16.
 */
public class PopDBMLSpark {

    public static void main( String args[] ) throws Exception
    {
        PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
        System.getProperties().put("hibernate.cache.use_second_level_cache", "false");
        System.getProperties().put("hibernate.cache.use_query_cache", "false");
        Configuration.init(null);
        //CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));

        PopDBMLSpark popdb = new PopDBMLSpark();
        Transaction transaction = GroupService.getGroupDAO().getSession().getTransaction();
        transaction.begin();
        PopDBIOTUtils.initShiroWithRoot();

        popdb.run();
        transaction.commit();
        System.exit( 0 );
    }

    public void run() throws NonUniqueResultException {
        System.out.println("******* Start populating Riot ML data for Spark.....");
        createConnections();
        System.out.println("******* End populating Riot ML data for Spark.....");

    }

    public void createConnections() throws NonUniqueResultException {

        Connection connection = ConnectionService.getInstance().getByCode(MlConfiguration.ANALYTICS_CONNECTION_CODE);

        // Spark
        Map<String, Object>  mapProperties = new LinkedHashMap<>();
        mapProperties.put( "masterHost",  "localhost" );
        mapProperties.put( "masterPort",  "6066" );
        mapProperties.put( "clusterMode", "local" );
        mapProperties.put( "responseTimeout", "60" );

        connection.setProperties( new JSONObject( mapProperties ).toJSONString() );
        ConnectionService.getInstance().update( connection );

    }
}
