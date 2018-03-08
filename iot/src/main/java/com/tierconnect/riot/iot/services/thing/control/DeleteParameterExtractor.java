package com.tierconnect.riot.iot.services.thing.control;

import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.services.thing.entity.CrudParameters;
import com.tierconnect.riot.iot.utils.VisibilityThingUtils;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by julio.rocha on 09-08-17.
 */
class DeleteParameterExtractor {

    private static Logger logger = Logger.getLogger(InsertParameterExtractor.class);
    private static final DeleteParameterExtractor INSTANCE = new DeleteParameterExtractor();

    private DeleteParameterExtractor() {
    }

    static DeleteParameterExtractor getInstance() {
        return INSTANCE;
    }

    public CrudParameters extract(Thing thing,
                                  boolean validateVisibility,
                                  Date transactionDate,
                                  boolean executeTickle,
                                  User userExecutor,
                                  Subject subject,
                                  User userLogged,
                                  boolean fillSource,
                                  boolean deleteMongoFlag,
                                  boolean secure) {
        subject = (subject == null) ? SecurityUtils.getSubject() : subject;
        CrudParameters parameters = new CrudParameters.CrudParametersBuilder()
                .setValidateVisibility(validateVisibility)
                .setTransactionDate(transactionDate)
                .setExecuteTickle(executeTickle)
                .setCurrentUser(userLogged)
                .setSubject(subject)
                .setFillSource(fillSource)
                .build();
        parameters.setUserActionExecutor(userExecutor);
        parameters.setDeleteMongoFlag(deleteMongoFlag);
        parameters.setSecureDelete(secure);
        parameters.setThingToProcess(thing);
        parameters.setThingType(thing.getThingType());
        parameters.setGroup(thing.getGroup());
        validateDeleteThing(parameters);
        addThingParent(parameters);
        return parameters;
    }

    private void validateDeleteThing(CrudParameters parameters) {
        List<String> messages = new LinkedList<>();
        Thing thing = parameters.getThingToProcess();
        if (thing == null) {
            messages.add("The thing to update does not exist.");
        }

        if (parameters.getValidateVisibility()) {
            try {
                VisibilityThingUtils.limitVisibilityDeleteT(thing, parameters.getSubject(), false);
            } catch (Exception e) {
                logger.error("Visibility error: ", e);
                messages.add("Visibility error: " + e.getMessage());
            }
        }

        if (!messages.isEmpty()) {
            throw new UserException("Error in delete validation: " + StringUtils.join(messages, ","));
        }
    }

    private void addThingParent(CrudParameters parameters) {
        Thing parent = parameters.getThingToProcess().getParent();
        if (parent != null) {
            Thing parentPojo = new Thing();
            parentPojo.setThingType(parent.getThingType());
            parentPojo.setSerial(parent.getSerial());
            parameters.setThingParentToProcess(parentPojo);
            parameters.setThingParentMapToProcess(new HashMap<>());
        }
    }
}
