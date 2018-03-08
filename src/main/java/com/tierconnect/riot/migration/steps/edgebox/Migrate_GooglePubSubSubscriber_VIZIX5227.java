package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QConnectionType;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by dbascope on 06/08/17
 */
public class Migrate_GooglePubSubSubscriber_VIZIX5227 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_GooglePubSubSubscriber_VIZIX5227.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        PopDBRequired.populateGooglePubSubConnection(GroupService.getInstance().getRootGroup());
        PopDBRequiredIOT.createRulesActionParameteres();
        crateGPubSubConnection();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    public void crateGPubSubConnection() {
        Group rootGroup  = GroupService.getInstance().getRootGroup();
        Connection connection = new Connection();
        connection.setName( "Google Cloud API" );
        connection.setCode( "GPubSub" );
        connection.setGroup( rootGroup );
        connection.setConnectionType( ConnectionTypeService.getInstance().getConnectionTypeDAO()
            .selectBy( QConnectionType.connectionType.code.eq( "GPubSub" ) ) );
        JSONObject jsonProperties = new JSONObject();
        Map<String, Object> mapProperties  = new LinkedHashMap<>();
        mapProperties.put( "type", "service_account" );
        mapProperties.put( "project_id", "YOURPROJECT-123456" );
        mapProperties.put( "private_key_id", "594f55525f505249564154455f4b4559" );
        mapProperties.put( "private_key", "-----BEGIN PRIVATE KEY-----"
            + "\nPASTE_YOUR_PRIVATE_KEY_HERE\n-----END PRIVATE KEY-----\n" );
        mapProperties.put( "client_email", "your_client@email.com" );
        mapProperties.put( "client_id", "0123456789" );
        mapProperties.put( "auth_uri", "https://accounts.google.com/o/oauth2/auth" );
        mapProperties.put( "token_uri", "https://accounts.google.com/o/oauth2/token" );
        mapProperties.put( "auth_provider_x509_cert_url", "https://www.googleapis.com/oauth2/v1/certs" );
        mapProperties.put( "client_x509_cert_url", "https://www.googleapis.com/robot/v1/metadata/x509/YOUR_CLIENT_CERT_URL" );
        jsonProperties.putAll( mapProperties );
        connection.setProperties( jsonProperties.toJSONString() );
        ConnectionService.getInstance().insert( connection );
    }
}
