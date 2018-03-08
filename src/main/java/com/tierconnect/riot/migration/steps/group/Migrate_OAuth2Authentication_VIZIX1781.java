package com.tierconnect.riot.migration.steps.group;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import com.tierconnect.riot.appcore.services.FieldService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_OAuth2Authentication_VIZIX1781 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_OAuth2Authentication_VIZIX1781.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        migrateOAuthFields(rootGroup);
        createOAuthConnection(rootGroup);
    }

    private void migrateOAuthFields(Group rootGroup) {

        Field f50 = FieldService.getInstance().selectByName("authenticationMode");

        PopDBUtils.popFieldWithParentService("oauth2Authentication", "oauth2Authentication", "OAuth 2.0",
                rootGroup, "Security Configuration", "java.lang.String", 3L, false, f50);
        Field f68 = PopDBUtils.popFieldService("oauth2Connection", "oauth2Connection", "OAuth 2.0 Connection",
                rootGroup, "Security Configuration", "java.lang.String", 1L, true);
        PopDBUtils.popGroupField(rootGroup, f68, "");
        Field f67 = PopDBUtils.popFieldService("oauth2LoginMessage", "oauth2LoginMessage", "OAuth 2.0 Login Message",
                rootGroup, "Security Configuration", "java.lang.String", 1L, true);
        PopDBUtils.popGroupField(rootGroup, f67, "Log in with:");

    }

    private void createOAuthConnection(Group rootGroup){
        ConnectionType connectionType = ConnectionTypeService.getInstance().getConnectionTypeByCode("OAUTH2");

        if (connectionType != null) {
            List<Map<String, String>> possibleValues=new ArrayList<>();
            possibleValues.add(PopDBRequired.newServerValue("custom","Custom","username"));
            possibleValues.add(PopDBRequired.newServerValue("google","Google","email","email"));
            possibleValues.add(PopDBRequired.newServerValue("facebook","Facebook","email","email"));
            possibleValues.add(PopDBRequired.newServerValue("linkedin","LinkedIn","emailAddress"));
            possibleValues.add(PopDBRequired.newServerValue("github","GitHub","login"));

            List<Map<String, String>> methodOptions=new ArrayList<>();
            methodOptions.add(PopDBRequired.newServerValue("get","GET", null));
            methodOptions.add(PopDBRequired.newServerValue("post","POST",null));

            List<Map<String, String>> strategyOptions=new ArrayList<>();
            strategyOptions.add(PopDBRequired.newServerValue("implicit","Implicit Grant",null));
            strategyOptions.add(PopDBRequired.newServerValue("authorizationCode","Authorization Code Grant",null));
            // Adding Properties definition
            List<Map<String, Object>> propertiesDefinition = new ArrayList<>();
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("clientId", "Client Id", "String", true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("clientSecret", "Client Secret", "String", true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("accessTokenUri", "Access Token Uri", "String", true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("userAuthUri", "User Authorization Uri", "String", true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("userInfoUri", "User Information Uri", "String", true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("redirectUri", "Redirection Uri", "String", true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("provider", "Provider", "String",possibleValues, true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("accessTokenMethod", "Access Token Method", "String",methodOptions, true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("userIdentifier", "User Identifier", "String", true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("grantType", "Grant Type", "String", strategyOptions, true));
            propertiesDefinition.add(PopDBRequired.newPropertyDefinition("scope", "Scope", "String", false));

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                connectionType.setPropertiesDefinitions(objectMapper.writeValueAsString(propertiesDefinition));
                ConnectionTypeService.getInstance().update(connectionType);
            } catch (JsonProcessingException e) {
                logger.warn("Migrating OAuth 2.0 connection, error updating connectionType", e);
            }
        } else {
            PopDBRequired.populateOAuth2Connection(rootGroup);
        }

    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
