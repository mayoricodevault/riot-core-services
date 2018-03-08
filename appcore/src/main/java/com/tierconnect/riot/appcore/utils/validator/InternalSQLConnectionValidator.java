package com.tierconnect.riot.appcore.utils.validator;


import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.core.HazelcastInstance;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import org.hibernate.Session;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.Iterator;
import java.util.Properties;

/**
 * Created by aruiz on 8/1/17.
 */
public class InternalSQLConnectionValidator implements ConnectionValidator {
    private int status;
    private String cause;
    ExternalConnectionJdbc externalConnectionJdbc=null;
    @Override
    public boolean testConnection(ConnectionType connectionType, String properties) {

        JSONParser parser = new JSONParser();
        try {
            JSONObject internalProperties = (JSONObject) parser.parse(properties);
            String unHashPassword = Connection.decode(internalProperties.get("password").toString());
            externalConnectionJdbc = ExternalConnectionJdbc.getInstance(internalProperties.get("driver").toString(),
                    internalProperties.get("url").toString(), null,
                    internalProperties.get("username").toString(),unHashPassword);
            ClientConfig clientConfig = new ClientConfig();
            ClientNetworkConfig cnc = clientConfig.getNetworkConfig();
            cnc.addAddress(internalProperties.get("hazelcastNativeClientAddress").toString()+":5701");
            HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);
            if (client != null){
                client.shutdown();

            }
            status = 200;
            cause = "Success";
            return true;
        }catch (ParseException e){
            status = 400;
            cause = "Cannot parse configuration. " + e.getMessage();
            return false;
        }finally {
            if (externalConnectionJdbc != null) {
                externalConnectionJdbc.closeConnection();
            }
        }
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getCause() {
        return cause;
    }

    private void testHibernate(JSONObject internalProperties ) {
        Properties propertiesHibernate = new Properties();
        Iterator<String> keys = internalProperties.keySet().iterator();
        while (keys.hasNext()){
            String key = keys.next();
            propertiesHibernate.put(key,internalProperties.get(key));
        }

        if (!HibernateSessionFactory.instanceTest) {
            Configuration configuration = new Configuration();
            configuration.addProperties(propertiesHibernate);
            configuration.configure();
            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();
            HibernateSessionFactory.setInstanceForTest(configuration.buildSessionFactory(serviceRegistry));
        }

        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        HibernateSessionFactory.closeInstance();
    }
}
