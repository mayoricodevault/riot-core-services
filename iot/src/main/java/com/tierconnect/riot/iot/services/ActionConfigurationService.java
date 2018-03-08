package com.tierconnect.riot.iot.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.ActionConfiguration;
import com.tierconnect.riot.iot.entities.QActionConfiguration;
import com.tierconnect.riot.iot.entities.ReportActions;
import com.tierconnect.riot.sdk.dao.UserException;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.tierconnect.riot.appcore.utils.Utilities.*;
import static com.tierconnect.riot.commons.Constants.ActionHTTPConstants.*;

public class ActionConfigurationService extends ActionConfigurationServiceBase {

    public List<ActionConfiguration> getAllConfiguration() {
        HibernateQuery query = getActionConfigurationDAO().getQuery();
        return query.list(QActionConfiguration.actionConfiguration);
    }

    public ActionConfiguration getActionConfigurationActive(Long id) {
        HibernateQuery query = getActionConfigurationDAO().getQuery();
        query.where(QActionConfiguration.actionConfiguration.id.eq(id));
        query.where(QActionConfiguration.actionConfiguration.status.eq(Constants.ACTION_STATUS_ACTIVE));
        return query.uniqueResult(QActionConfiguration.actionConfiguration);
    }

    public void validateActionConfiguration(ActionConfiguration actionConfiguration) {
        if (isEmptyOrNull(actionConfiguration.getName())) {
            throw new UserException("Action name is empty, please check it and try again.");
        }
        if (isEmptyOrNull(actionConfiguration.getCode())) {
            throw new UserException("Action code is empty, please check it and try again.");
        }
        if (isEmptyOrNull(actionConfiguration.getConfiguration())) {
            throw new UserException("The configuration is empty, please check it and try again.");
        }
        if (isEmptyOrNull(actionConfiguration.getType())) {
            throw new UserException("Action type is empty, please check it and try again.");
        }
        if (!"http".equalsIgnoreCase(actionConfiguration.getType())) {
            throw new UserException("Action does not contain \"type\" valid");
        }
        if (actionConfiguration.getGroup() == null) {
            throw new UserException("The group is empty, please check it and try again.");
        }
        validateConfiguration(actionConfiguration);
    }

    @SuppressWarnings("unchecked")
    public void validateConfiguration(ActionConfiguration actionConfiguration) {
        Map<String, Object> configurationMap;
        try {
            ObjectMapper mapper = new ObjectMapper();
            configurationMap = mapper.readValue(actionConfiguration.getConfiguration(), Map.class);
        } catch (Exception e) {
            throw new UserException("Error in configuration [" + actionConfiguration.getName() + "]", e);
        }
        if (configurationMap.isEmpty()) {
            throw new UserException("The configuration is empty");
        }
        if (configurationMap.get(EXECUTION_TYPE_METHOD.value) == null) {
            throw new UserException("The configuration does not contains \"Method\"");
        }
        if (!"POST".equalsIgnoreCase(String.valueOf(configurationMap.get(EXECUTION_TYPE_METHOD.value)))) {
            throw new UserException("The configuration does not contains \"Method POST\"");
        }
        if (isEmptyOrNull((String) configurationMap.get(EXECUTION_TYPE_URL.value))) {
            throw new UserException("The configuration does not contains \"URL\"");
        }
        if (!urlIsValid(String.valueOf(configurationMap.get(EXECUTION_TYPE_URL.value)), new String[]{"http", "https"})) {
            throw new UserException("The configuration does not contains \"URL\" valid");
        }
        if (configurationMap.get(EXECUTION_TYPE_TIMEOUT.value) == null) {
            throw new UserException("The configuration does not contains \"Request Timeout\"");
        }
        if (!isInteger(configurationMap.get(EXECUTION_TYPE_TIMEOUT.value))) {
            throw new UserException("The configuration does not contains \"Request Timeout\" valid");
        }
        if (configurationMap.get("openResponseIn") == null) {
            throw new UserException("The configuration does not contains \"Open response\"");
        }
        if (configurationMap.get(Constants.ActionHTTPConstants.EXECUTION_TYPE.value) == null) {
            throw new UserException("The configuration does not contains \"Execution type\"");
        }

        List<String> executionType = Arrays.asList(Constants.ActionHTTPConstants.EXECUTION_TYPE_FORM.value, Constants.ActionHTTPConstants.EXECUTION_TYPE_REST.value);
        if (!executionType.contains(String.valueOf(configurationMap.get(Constants.ActionHTTPConstants.EXECUTION_TYPE.value)).toUpperCase())) {
            throw new UserException("The configuration does not contains \"Execution type\" valid");
        }

        List<String> openResponseInList = Arrays.asList("MODAL", "POPUP", "TAB");
        if (!openResponseInList.contains(String.valueOf(configurationMap.get("openResponseIn")).toUpperCase())) {
            throw new UserException("The configuration does not contains \"Open response\" valid");
        }
        if (configurationMap.get(EXECUTION_TYPE_BASIC_AUTH.value) != null) {
            if (!(configurationMap.get(EXECUTION_TYPE_BASIC_AUTH.value) instanceof Map)) {
                throw new UserException("Basic Authentication is invalid");
            }
            Map mapBasicAuth = (Map) configurationMap.get(EXECUTION_TYPE_BASIC_AUTH.value);
            if (!mapBasicAuth.isEmpty()
                    && (!mapBasicAuth.containsKey(EXECUTION_TYPE_BASIC_AUTH_USERNAME.value)
                    || !mapBasicAuth.containsKey(EXECUTION_TYPE_BASIC_AUTH_PASSWORD.value))) {
                throw new UserException("Username and Password is required for Basic Authentication");
            }
            if (!mapBasicAuth.isEmpty()
                    && (mapBasicAuth.get(EXECUTION_TYPE_BASIC_AUTH_USERNAME.value) == null
                    || mapBasicAuth.get(EXECUTION_TYPE_BASIC_AUTH_PASSWORD.value) == null)) {
                throw new UserException("Username or Password is null in Basic Authentication");
            }
        }
        if (configurationMap.get(EXECUTION_TYPE_HEADERS.value) != null) {
            if (!(configurationMap.get(EXECUTION_TYPE_HEADERS.value) instanceof Map)) {
                throw new UserException("The Headers is invalid");
            }
            Map<String, Object> mapHeaders = (Map) configurationMap.get(EXECUTION_TYPE_HEADERS.value);
            for (Map.Entry<String, Object> entry : mapHeaders.entrySet()) {
                if (isEmptyOrNull(entry.getKey())) {
                    throw new UserException("Key of the header is empty");
                }
                if (entry.getValue() == null || isEmptyOrNull(String.valueOf(entry.getValue()))) {
                    throw new UserException("value of key is empty or null");
                }
            }
        }

    }

    public ActionConfiguration update(ActionConfiguration actionConfiguration, String oldConfiguration) {
        validateActionConfiguration(actionConfiguration);
        updateConfiguration(actionConfiguration, oldConfiguration);
        return super.update(actionConfiguration);
    }

    @SuppressWarnings("unchecked")
    public void updateConfiguration(ActionConfiguration actionConfiguration, String oldConfiguration) {
        if (oldConfiguration != null) {
            JSONObject oldConfigurationJson;
            JSONObject newConfigurationJson;
            try {
                oldConfigurationJson = (JSONObject) new JSONParser().parse(oldConfiguration);
                newConfigurationJson = (JSONObject) new JSONParser().parse(actionConfiguration.getConfiguration());
            } catch (Exception e) {
                throw new UserException("Error in update configuration [" + actionConfiguration.getName() + "]", e);
            }
            JSONObject oldBasicAuth = (JSONObject) oldConfigurationJson.get(EXECUTION_TYPE_BASIC_AUTH.value);
            JSONObject newBasicAuth = (JSONObject) newConfigurationJson.get(EXECUTION_TYPE_BASIC_AUTH.value);

            if (!isEmptyOrNull(String.valueOf(newBasicAuth.get(EXECUTION_TYPE_BASIC_AUTH_USERNAME.value)))
                    && isEmptyOrNull(String.valueOf(newBasicAuth.get(EXECUTION_TYPE_BASIC_AUTH_PASSWORD.value)))) {
                newConfigurationJson.put(EXECUTION_TYPE_BASIC_AUTH.value, oldBasicAuth);
                actionConfiguration.setConfiguration(newConfigurationJson.toJSONString());
            }
        }
    }

    public void validateAssociationActionReport(Long reportDefinitionId, ActionConfiguration actionConfiguration) {
        if (reportDefinitionId != null && actionConfiguration != null && actionConfiguration.getId() != null) {
            List<ReportActions> reportActionsActives = ReportActionsService.getInstance().getReportActionsActives(reportDefinitionId, actionConfiguration);
            if (reportActionsActives != null && !reportActionsActives.isEmpty()) {
                throw new UserException("The action with ID[" + actionConfiguration.getId() + "] are associated with other reports");
            }
        }
    }

    @Override
    public void validateInsert(ActionConfiguration actionConfiguration) {
        validateActionConfiguration(actionConfiguration);
    }

    @Override
    public void delete(ActionConfiguration actionConfiguration) {
        // delete logic
        actionConfiguration.setStatus(Constants.ACTION_STATUS_DELETED);
        getActionConfigurationDAO().update(actionConfiguration);
    }
}

