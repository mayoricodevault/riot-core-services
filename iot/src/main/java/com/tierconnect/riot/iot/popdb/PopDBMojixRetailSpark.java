package com.tierconnect.riot.iot.popdb;

import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.hibernate.Transaction;
import org.jose4j.json.internal.json_simple.parser.ParseException;

public class PopDBMojixRetailSpark
{
    public static void main( String args[] ) throws Exception
    {
        PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
        System.getProperties().put("hibernate.cache.use_second_level_cache", "false");
        System.getProperties().put("hibernate.cache.use_query_cache", "false");
        Configuration.init(null);
        //CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));

        PopDBMojixRetailSpark popdb = new PopDBMojixRetailSpark();
        Transaction transaction = GroupService.getGroupDAO().getSession().getTransaction();
        transaction.begin();
        PopDBIOTUtils.initShiroWithRoot();
        popdb.run();
        transaction.commit();

        System.exit( 0 );
    }

    public void run() throws NonUniqueResultException, ParseException {
        createData();

        PopDBMLSpark pml = new PopDBMLSpark();
        pml.run();
    }

    private void createData() throws NonUniqueResultException, ParseException {
        String[] topicKafka1={"/v1/data/ALEB2,8,1"};
        String[] topicKafka2={"/v1/data/STAR,8,1"};

        Edgebox edgeboxMCB2 = EdgeboxService.getEdgeboxDAO().selectBy("code", "CB1");
        String mcbConfig1 = PopDBRequiredIOTSpark.getCoreBridgeConfiguration(edgeboxMCB2.getConfiguration(), topicKafka1);

        edgeboxMCB2.setConfiguration(mcbConfig1);
        EdgeboxService.getInstance().update( edgeboxMCB2 );

        Edgebox eb3 = EdgeboxService.getEdgeboxDAO().selectBy("code", "CB1");
        String mcbConfig2 = PopDBRequiredIOTSpark.getCoreBridgeConfiguration(eb3.getConfiguration(), topicKafka2);
        eb3.setConfiguration(mcbConfig2);
        EdgeboxService.getInstance().update( eb3 );
    }
}
