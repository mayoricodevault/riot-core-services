package com.tierconnect.riot.iot.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.job.ActionExecutionJob;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.tierconnect.riot.commons.Constants.ActionHTTPConstants.EXECUTION_TYPE_BASIC_AUTH;
import static com.tierconnect.riot.commons.Constants.ActionHTTPConstants.EXECUTION_TYPE_BASIC_AUTH_PASSWORD;

public class LogExecutionActionService extends LogExecutionActionServiceBase {
    private static Logger logger = Logger.getLogger(LogExecutionActionService.class);

    public String executeAction(ActionConfiguration actionConfiguration, String body, User user, Long reportID) {
        String respondeCode = "\"" + actionConfiguration.getName() + "\" Succeded.";
        if (!"HTTP".equalsIgnoreCase(actionConfiguration.getType())) {
            throw new UserException("Action type [" + actionConfiguration.getType() + "] not implemented.");
        }
        Map<String, Object> mapConfiguration = getMapConfiguration(actionConfiguration);
        // save
        LogExecutionAction logExecutionAction = saveLog(actionConfiguration, user, mapConfiguration, body);
        logger.info("Execution action [" + actionConfiguration.getName() + "] in mode [" + mapConfiguration.get("openResponseIn")
                + "] Action ID=" + actionConfiguration.getId());
        if ("POPUP".equalsIgnoreCase(String.valueOf(mapConfiguration.get("openResponseIn")))
                || "TAB".equalsIgnoreCase(String.valueOf(mapConfiguration.get("openResponseIn")))) {
            return respondeCode;
        } else if ("MODAL".equalsIgnoreCase(String.valueOf(mapConfiguration.get("openResponseIn")))) {
            body = removeConfigurationOfAttachment(reportID, body);
            ActionExecutionJob actionExecutionJob = new ActionExecutionJob(logExecutionAction.getId(), body, mapConfiguration);
            executeActionJob(actionExecutionJob, ActionExecutionJob.class.getName() + "-HTTP-A" + actionConfiguration.getId());
        } else {
            throw new UserException("openResponseIn not contains [MODAL, TAB or POPUP]");  ///TODO:
        }
        return respondeCode;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMapConfiguration(ActionConfiguration actionConfiguration) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(actionConfiguration.getConfiguration(), Map.class);
        } catch (IOException e) {
            logger.error("Error in configuration of action[" + actionConfiguration.getName()
                    + "] with ID [" + actionConfiguration.getId() + "] ", e);
            throw new UserException("Error with configuration of action");
        }
    }

    private LogExecutionAction saveLog(ActionConfiguration actionConfiguration, User user, Map<String, Object> mapConfiguration, String body) {
        JSONObject jsonObject = new JSONObject(mapConfiguration);
        jsonObject.put("body", body);
        LogExecutionAction logExecutionAction = new LogExecutionAction();
        logExecutionAction.setRequest(jsonObject.toJSONString());
        logExecutionAction.setIniDate(new Date());
        logExecutionAction.setCreatedByUser(user);
        logExecutionAction.setActionConfiguration(actionConfiguration);
        insert(logExecutionAction);
        return logExecutionAction;
    }

    /**
     * return logs by parameters
     *
     * @param mapParameters parameters
     * @return logs without request and response
     */
    public List<Map<String, Object>> getLogs(Map<String, Object> mapParameters) {
        String iniDateString = String.valueOf(mapParameters.get("iniDate"));
        String endDateString = String.valueOf(mapParameters.get("endDate"));
        String userIdExecutionString = String.valueOf(mapParameters.get("user.id"));
        String actionConfigurationIdString = String.valueOf(mapParameters.get("actionConfiguration.id"));

        Date iniDate = Utilities.isNumber(iniDateString) ? new Date(Long.parseLong(iniDateString)) : null;
        Date endDate = Utilities.isNumber(endDateString) ? new Date(Long.parseLong(endDateString)) : null;
        Long userIdExecution = Utilities.isNumber(userIdExecutionString) ? Long.valueOf(userIdExecutionString) : null;
        Long actionConfigurationId = Utilities.isNumber(actionConfigurationIdString) ? Long.valueOf(actionConfigurationIdString) : null;

        if (iniDate == null && endDate == null && userIdExecution == null && actionConfigurationId == null) {
            throw new UserException("Search parameters is empty");
        }

        BooleanBuilder builder = new BooleanBuilder();
        if (userIdExecution != null) {
            builder = builder.and(QLogExecutionAction.logExecutionAction.createdByUser.id.eq(userIdExecution));
        }
        if (actionConfigurationId != null) {
            builder = builder.and(QLogExecutionAction.logExecutionAction.actionConfiguration.id.eq(actionConfigurationId));
        }
        if (iniDate != null && endDate != null) {
            builder = builder.and(QLogExecutionAction.logExecutionAction.iniDate.between(iniDate, endDate));
        } else if (iniDate != null) {
            builder = builder.and(QLogExecutionAction.logExecutionAction.iniDate.gt(iniDate));
        } else if (endDate != null) {
            builder = builder.and(QLogExecutionAction.logExecutionAction.iniDate.lt(endDate));
        }
        List<LogExecutionAction> logExecutionActionsList = getLogExecutionActionDAO().getQuery().where(builder)
                .orderBy(QLogExecutionAction.logExecutionAction.iniDate.desc()).list(QLogExecutionAction.logExecutionAction);
        List<Map<String, Object>> logList = new ArrayList<>();
        for (LogExecutionAction logExecutionAction : logExecutionActionsList) {
            Map<String, Object> map = publicMapWithoutPassword(logExecutionAction.publicMap());
            map.remove("request");
            map.remove("response");
            logList.add(map);
        }
        return logList;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> publicMapWithoutPassword(Map<String, Object> map) {
        Map mapActionConfiguration = (Map) map.get("actionConfiguration");
        if (mapActionConfiguration != null) {
            Map configuration = (Map) mapActionConfiguration.get("configuration");
            if (configuration != null) {
                Map basicAuthentication = (Map) configuration.get(EXECUTION_TYPE_BASIC_AUTH.value);
                if (basicAuthentication != null) {
                    basicAuthentication.put(EXECUTION_TYPE_BASIC_AUTH_PASSWORD.value, StringUtils.EMPTY);
                    configuration.put(EXECUTION_TYPE_BASIC_AUTH.value, basicAuthentication);
                }
                mapActionConfiguration.put("configuration", configuration);
            }
            map.put("actionConfiguration", mapActionConfiguration);
        }
        return map;
    }

    private void executeActionJob(ActionExecutionJob actionExecutionJob, String name) {
        logger.debug("Commit in ReportAppService");
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try {
            if (transaction.isActive()) {
                transaction.commit();
            }
        } catch (Exception e) {
            logger.error("Error in commit manually", e);
            HibernateDAOUtils.rollback(transaction);
        }
        Thread threadAction = new Thread(actionExecutionJob, name);
        threadAction.start();
    }

    private String removeConfigurationOfAttachment(Long reportID, String body) {
        String newBody = body;
        List<String> propertiesAttachment = getPropertiesAttachment(reportID);
        if (!propertiesAttachment.isEmpty()) {
            try {
                JSONParser jsonParser = new JSONParser();
                JSONObject jsonObject = (JSONObject) jsonParser.parse(body);

                for (String property : propertiesAttachment) {
                    String attachmentConfig = String.valueOf(jsonObject.get(property));
                    jsonObject.put(property, getListFileName(attachmentConfig));
                }
                newBody = jsonObject.toJSONString();
            } catch (Exception e) {
                logger.error("Error in remove configuration Attachment", e);
            }
        }
        return newBody;
    }

    private List<String> getPropertiesAttachment(Long reportID) {
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(reportID);
        List<String> listAttachment = new ArrayList<>();
        if (reportDefinition != null) {
            for (ReportProperty property : reportDefinition.getReportProperty()) {
                ThingTypeField thingTypeField = property.getThingTypeField();
                if (thingTypeField != null && thingTypeField.getDataType() != null
                        && thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_ATTACHMENTS.value) == 0) {
                    listAttachment.add(property.getLabel());
                }
            }
        }
        return listAttachment;
    }

    private String getListFileName(String attachment) throws ParseException {
        if (!Utilities.isEmptyOrNull(attachment)) {
            List<String> fileNameList = new ArrayList<>();
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(attachment);
            if (jsonObject != null) {
                JSONArray fileArray = (JSONArray) jsonObject.get("attachments");
                if (fileArray != null) {
                    for (Object objectFileString : fileArray) {
                        JSONObject object = (JSONObject) objectFileString;
                        fileNameList.add(String.valueOf(object.get("name")));
                    }
                }
            }
            attachment = StringUtils.join(fileNameList, ",");
        }
        return attachment;
    }
}

