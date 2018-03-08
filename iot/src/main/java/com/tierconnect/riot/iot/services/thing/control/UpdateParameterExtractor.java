package com.tierconnect.riot.iot.services.thing.control;

import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.services.thing.entity.CrudParameters;
import com.tierconnect.riot.iot.utils.Cache;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by vealaro on 8/15/17.
 */
class UpdateParameterExtractor extends ParameterExtractor {
    private static Logger logger = Logger.getLogger(UpdateParameterExtractor.class);
    private static final UpdateParameterExtractor INSTANCE = new UpdateParameterExtractor();

    private UpdateParameterExtractor() {
    }

    public static UpdateParameterExtractor getInstance() {
        return INSTANCE;
    }

    CrudParameters extract(Thing thing,
                           String thingTypeCode,
                           String groupHierarchyCode,
                           String name,
                           String serialNumber,
                           Object parent,
                           Map<String, Object> udfs,
                           Object children,
                           Object childrenUdf,
                           boolean executeTickle,
                           boolean validateVisibility,
                           Date transactionDate, boolean disableFMCLogic,
                           Map<String, Boolean> validations,
                           Cache cache,
                           boolean updateAndFlush,
                           boolean recursiveUpdate, String facilityCode,
                           User currentUser,
                           Subject subject,
                           boolean fillSource) {
        List<String> messages = new ArrayList<>();
        subject = (subject == null) ? SecurityUtils.getSubject() : subject;
        CrudParameters parameters = new CrudParameters.CrudParametersBuilder()
                .setThingTypeCode(thingTypeCode)
                .setGroupHierarchyCode(groupHierarchyCode)
                .setName(name)
                .setSerialNumber(serialNumber)
                .setParent(parent)
                .setUdfs(udfs)
                .setChildren((List) children)
                .setExecuteTickle(executeTickle)
                .setChildrenUdf((List) childrenUdf)
                .setValidateVisibility(validateVisibility)
                .setTransactionDate(transactionDate)
                .setDisableFMCLogic(disableFMCLogic)
                .setValidations(validations)
                .setCache(cache)
                .setCreateUpdateAndFlush(updateAndFlush)
                // recursiveUpdate
                .setFacilityCode(facilityCode)
                .setCurrentUser(currentUser)
                .setSubject(subject)
                .setFillSource(fillSource)
                .setCreation(false).build();
        parameters.setThingToProcess(thing);
        try {
            //Validate and get ThingType
            parameters.setThingType(getThingType(parameters, messages));

            // Validate and get group. If groupHierarchyCode is null, we will get the group of the ThingType
            parameters.setGroup(getGroup(parameters, messages));

            //
            validateThingName(parameters, messages);

            //Validate Serial
            validateSerialNumber(parameters, messages);

            //Visibility
            validateVisibility(parameters, messages);

            //Validate Parent
            parameters.setParentObj(validateAndGetParent(parameters, messages));

            //Validate children
            parameters.setChildrenObj(validateAndGetChildren(parameters, messages));

            //Validate Udf's
            validateUdfs(parameters, messages);

            if (!messages.isEmpty()) {
                //logger.error("Error in Thing Parameters Extractor: " + tu.getLogString());
                String serialNumberMsg = StringUtils.isNotEmpty(
                        parameters.getSerialNumber()) ? "Serial Number [" + parameters.getSerialNumber() + "], " : "";
                throw new UserException(serialNumberMsg + StringUtils.join(messages, ";"));
            }
            parameters.setModifiedTime(new Date());
        } catch (Exception e) {
            logger.error("Error in validation ", e);
            throw new UserException("Error in validation " + e.getMessage(), e);
        }
        return parameters;
    }

}
