package com.tierconnect.riot.migration.steps.action;

import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.ActionConfiguration;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.services.ActionConfigurationService;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.List;

/**
 * Created by vealaro on 8/29/17.
 */
public class Migrate_ActionReport_VIZIX7849 implements MigrationStep {

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        addAttributeToParameterHTTP();
        addAttributeToActionConfiguration();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void addAttributeToParameterHTTP() throws ParseException {
        Parameters parameterHTTP = ParametersService.getInstance().getByCategoryAndCode(Constants.ACTION_TYPE, "http");
        if (parameterHTTP != null && !Utilities.isEmptyOrNull(parameterHTTP.getValue())) {
            parameterHTTP.setValue(addNewAttribute(parameterHTTP.getValue()));
            ParametersService.getInstance().update(parameterHTTP);
        }
    }

    private void addAttributeToActionConfiguration() throws ParseException {
        List<ActionConfiguration> allConfiguration = ActionConfigurationService.getInstance().getAllConfiguration();
        for (ActionConfiguration configuration : allConfiguration) {
            if (!Utilities.isEmptyOrNull(configuration.getConfiguration())) {
                configuration.setConfiguration(addNewAttribute(configuration.getConfiguration()));
                ActionConfigurationService.getInstance().update(configuration);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String addNewAttribute(String value) throws ParseException {
        JSONParser jsonParser = new JSONParser();
        JSONObject currentConfig = (JSONObject) jsonParser.parse(value);
        currentConfig.put(Constants.ActionHTTPConstants.EXECUTION_TYPE.value, Constants.ActionHTTPConstants.EXECUTION_TYPE_REST.value); // default value
        return currentConfig.toJSONString();
    }
}
