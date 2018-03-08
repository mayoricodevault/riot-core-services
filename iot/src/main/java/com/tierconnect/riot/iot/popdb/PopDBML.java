package com.tierconnect.riot.iot.popdb;

import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.appcore.utils.MlConfiguration;
import com.tierconnect.riot.iot.dao.MlBusinessModelDAO;
import com.tierconnect.riot.iot.dao.MlBusinessModelTenantDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by pablo on 7/13/16.
 */
public class PopDBML {

    private static final Logger logger = Logger.getLogger( PopDBML.class );

    public static void main( String args[] ) throws Exception
    {
        PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
        System.getProperties().put("hibernate.cache.use_second_level_cache", "false");
        System.getProperties().put("hibernate.cache.use_query_cache", "false");
        Configuration.init(null);
        //CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));

        PopDBML popdb = new PopDBML();
        Transaction transaction = GroupService.getGroupDAO().getSession().getTransaction();
        transaction.begin();
        PopDBIOTUtils.initShiroWithRoot();


        // TODO: remove this -> temp hack to populate database without populating everything
        try {
            PopDBRequired.populateAnalyticsConnection(GroupService.getInstance().getByCode("root"));
        } catch (NonUniqueResultException e) {
            e.printStackTrace();
        }

        popdb.run();
        transaction.commit();
        System.exit( 0 );
    }

    public void run() throws NonUniqueResultException {
        System.out.println("******* Start populating Riot ML data.....");
        createModelsTypes();
        addResources();
        createConnections();
        System.out.println("******* End populating Riot ML data.....");

    }


    public void  createModelsTypes() {

        MlBusinessModel mlBusinessModel1 = new MlBusinessModel.Builder("Money Mapping").
                predictor("Date").predictor("Zone").build();

        MlBusinessModel mlBusinessModel2 = new MlBusinessModel.Builder("Conversion Rate").
                predictor("Date").predictor("Zone").predictor("ProductType").build();

        MlBusinessModel mlBusinessModel3 = new MlBusinessModel.Builder("Health Care Models").
                predictor("Date").predictor("Zone").build();

        MlBusinessModelDAO mlModelCategoryDAO = new MlBusinessModelDAO();
        mlModelCategoryDAO.insert(mlBusinessModel1);
        mlModelCategoryDAO.insert(mlBusinessModel2);
        mlModelCategoryDAO.insert(mlBusinessModel3);


        Group group = GroupService.getInstance().get(2L);

        MlBusinessModelTenant mlModelTenant1 = new MlBusinessModelTenant(mlBusinessModel1, group, "thingSnapshots", true);
        MlBusinessModelTenant mlModelTenant2 = new MlBusinessModelTenant(mlBusinessModel2, group, "thingSnapshots", false);
        MlBusinessModelTenant mlModelTenant3 = new MlBusinessModelTenant(mlBusinessModel3, group, "thingSnapshots", false);

        MlBusinessModelTenantDAO mlModelTypeTenantDAO = new MlBusinessModelTenantDAO();
        mlModelTypeTenantDAO.insert(mlModelTenant1);
        mlModelTypeTenantDAO.insert(mlModelTenant2);
        mlModelTypeTenantDAO.insert(mlModelTenant3);

    }

    public void addResources() {
        ResourceService resourceService = ResourceService.getInstance();
        Group             rootGroup       = GroupService.getInstance().getRootGroup();
        HashSet<Resource> resources       = new HashSet<>();

        Role rootRole        = RoleService.getInstance().getRootRole();
        Role tenantAdminRole = RoleService.getInstance().getTenantAdminRole();

        ResourceDAO resourceDAO = ResourceServiceBase.getResourceDAO();
        Resource moduleAnalytics = resourceDAO.selectBy(QResource.resource.name.eq("Analytics"));
        if (moduleAnalytics == null){
            moduleAnalytics = resourceService.insert(Resource.getModuleResource(
                rootGroup,
                "Analytics",
                "Analytics Resources"));
        }
        resources.add(moduleAnalytics);

        resources.add(resourceService.insert(Resource.getClassResource(rootGroup, MlExtraction.class,
                moduleAnalytics)));
        resources.add(resourceService.insert(Resource.getClassResource(rootGroup, MlModel.class,
                moduleAnalytics)));
        resources.add(resourceService.insert(Resource.getClassResource(rootGroup, MlPrediction.class,
                moduleAnalytics)));
        resources.add(resourceService.insert(Resource.getClassResource(rootGroup, MlBusinessModel.class,
                moduleAnalytics)));


        for (Resource resource : resources) {
            RoleResourceService.getInstance().insert(rootRole, resource, resource.getAcceptedAttributes());
            RoleResourceService.getInstance().insert(tenantAdminRole, resource, resource.getAcceptedAttributes());
            resourceService.update(resource);

        }
    }


    public void createConnections() throws NonUniqueResultException {

        Connection connection = new Connection();
        connection.setName( "Analytics" );
        connection.setCode( MlConfiguration.ANALYTICS_CONNECTION_CODE );
        connection.setGroup( GroupService.getInstance().getByCode("root") );
        connection.setConnectionType(
                ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy( QConnectionType.connectionType.code.eq( "Analytics" ) ) );
        Map<String, Object>  mapProperties = new LinkedHashMap<>();

        // Mongo
        mapProperties.put( "mongo.host", "localhost" );
        mapProperties.put( "mongo.port",   "27017" );
        mapProperties.put( "mongo.dbname", "riot_main" );
        mapProperties.put( "mongo.username", "admin" );
        mapProperties.put( "mongo.password",   "" );
        mapProperties.put( "mongo.secure",   "true" );

        // Paths, etc.
        mapProperties.put( "extractions.path", "/var/local/riot/extractions" );
        mapProperties.put( "trainings.path",   "/var/local/riot/trainings" );
        mapProperties.put( "predictions.path", "/var/local/riot/predictions" );
        mapProperties.put( "jars.path",        "/var/local/riot/jars" );
        mapProperties.put( "responses.path",   "/var/local/riot/responses" );
        mapProperties.put( "extraction.collection",   "thingSnapshots" );

        connection.setProperties( new JSONObject( mapProperties ).toJSONString() );
        ConnectionService.getInstance().insert( connection );

    }
}
