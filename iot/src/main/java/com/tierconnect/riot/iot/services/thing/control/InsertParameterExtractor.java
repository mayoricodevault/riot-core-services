package com.tierconnect.riot.iot.services.thing.control;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.LicenseDetail;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.LicenseService;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.iot.entities.LocalMap;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ValidationBean;
import com.tierconnect.riot.iot.services.LocalMapService;
import com.tierconnect.riot.iot.services.ThingTypeMapService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.iot.services.ThingsService;
import com.tierconnect.riot.iot.services.thing.entity.CrudParameters;
import com.tierconnect.riot.iot.utils.VisibilityThingUtils;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import java.util.*;

/**
 * Created by julio.rocha on 09-08-17.
 */
class InsertParameterExtractor extends ParameterExtractor {
    private static Logger logger = Logger.getLogger(InsertParameterExtractor.class);
    private static final InsertParameterExtractor INSTANCE = new InsertParameterExtractor();

    private InsertParameterExtractor() {
    }

    static InsertParameterExtractor getInstance() {
        return INSTANCE;
    }

    /*private TimerUtil initTimerUtil() {
        List<String> laps = new ArrayList<>();
        laps.add("[ThingParameterExtractor] : Validate and get ThingType");
        laps.add("[ThingParameterExtractor] : Validate and Get Group");
        laps.add("[ThingParameterExtractor] : Validate Max Number of things");
        laps.add("[ThingParameterExtractor] : Validate Serial");
        laps.add("[ThingParameterExtractor] : Validate Visibility");
        laps.add("[ThingParameterExtractor] : Validate Parent");
        laps.add("[ThingParameterExtractor] : Validate children");
        laps.add("[ThingParameterExtractor] : Validate children UDF");
        laps.add("[ThingParameterExtractor] : Validate and get Group Facility Map");
        laps.add("[ThingParameterExtractor] : Validate Udf");

        TimerUtil tu = new TimerUtil(true);
        String[] lapList = new String[laps.size()];
        lapList = laps.toArray(lapList);
        tu.initLaps(lapList);
        return tu;
    }*/

    CrudParameters extract(String thingTypeCode,
                           String groupHierarchyCode,
                           String name,
                           String serialNumber,
                           Map<String, Object> parent,
                           Map<String, Object> udfs,
                           Object children,
                           Object childrenUdf,
                           boolean executeTickle,
                           boolean validateVisibility,
                           Date transactionDate,
                           boolean disableFMCLogic,
                           boolean createAndFlush,
                           Boolean useDefaultValues,
                           Map<String, Boolean> validations,
                           String facilityCode,
                           boolean fillSource,
                           Subject subject) {
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
                .setCreateUpdateAndFlush(createAndFlush)
                .setUseDefaultValues(useDefaultValues)
                .setValidations(validations)
                .setFacilityCode(facilityCode)
                .setFillSource(fillSource)
                .setSubject(subject).build();
        try {
            //TimerUtil tu = initTimerUtil();
            //Validate and get ThingType
            //tu.start("[ThingParameterExtractor] : Validate and get ThingType");
            parameters.setThingType(getThingType(parameters, messages));
            //tu.stop("[ThingParameterExtractor] : Validate and get ThingType");
            // Validate and get group. If groupHierarchyCode is null, we will get the group of the ThingType
            //tu.start("[ThingParameterExtractor] : Validate and Get Group");
            parameters.setGroup(getGroup(parameters, messages));
            //tu.stop("[ThingParameterExtractor] : Validate and Get Group");
            //Validate Max Number of things
            //tu.start("[ThingParameterExtractor] : Validate Max Number of things");
            validateMaxNumberOfThings(parameters, messages);
            validateThingName(parameters, messages);
            //tu.stop("[ThingParameterExtractor] : Validate Max Number of things");
            //Validate Serial
            //tu.start("[ThingParameterExtractor] : Validate Serial");
            validateSerialNumber(parameters, messages);
            //tu.stop("[ThingParameterExtractor] : Validate Serial");
            //Visibility
            //tu.start("[ThingParameterExtractor] : Validate Visibility");
            validateVisibility(parameters, messages);
            //tu.stop("[ThingParameterExtractor] : Validate Visibility");
            //Validate Parent
            //tu.start("[ThingParameterExtractor] : Validate Parent");
            parameters.setParentObj(validateAndGetParent(parameters, messages));
            //tu.stop("[ThingParameterExtractor] : Validate Parent");
            //Validate children
            //tu.start("[ThingParameterExtractor] : Validate children");
            parameters.setChildrenObj(validateAndGetChildren(parameters, messages));
            //tu.stop("[ThingParameterExtractor] : Validate children");
            //Validate children UDF
            //tu.start("[ThingParameterExtractor] : Validate children UDF");
            parameters.setChildrenUDFObj(validateAndGetChildrenUDF(parameters, messages));
            //tu.stop("[ThingParameterExtractor] : Validate children UDF");
            //tu.start("[ThingParameterExtractor] : Validate and get Group Facility Map");
            parameters.setGroupFacilityMap(validateAndGetGroupFacility(parameters, messages));
            //tu.stop("[ThingParameterExtractor] : Validate and get Group Facility Map");
            //Validate Udf's
            //tu.start("[ThingParameterExtractor] : Validate Udf");
            validateUdfs(parameters, messages);
            //tu.stop("[ThingParameterExtractor] : Validate Udf");
            if (!messages.isEmpty()) {
                //logger.error("Error in Thing Parameters Extractor: " + tu.getLogString());
                String serialNumberMsg = StringUtils.isNotEmpty(
                        parameters.getSerialNumber()) ? "Serial Number [" + parameters.getSerialNumber() + "], " : "";
                throw new UserException(serialNumberMsg + StringUtils.join(messages, ";"));
            }
            parameters.setModifiedTime(new Date());
            //logger.info("Thing Parameters Extractor: " + tu.getLogString());
        } catch (Exception e) {
            logger.error("Error in validation ", e);
            throw new UserException("Error in validation " + e.getMessage(), e);
        }
        return parameters;
    }

    private void validateMaxNumberOfThings(CrudParameters parameters, List<String> messages) {
        if (parameters.getValidations() == null || parameters.getValidations().get("thing.maxNumberThings")) {
            ValidationBean validationBean = new ValidationBean();
            if (LicenseService.enableLicense) {
                LicenseDetail licenseDetail = LicenseService.getInstance().getLicenseDetail(parameters.getActiveGroup(), true);
                Long maxNumberOfThings = licenseDetail.getMaxThings();
                if (maxNumberOfThings != null && maxNumberOfThings > 0) {
                    Long countAll = ThingsService.count(licenseDetail);
                    if (countAll >= maxNumberOfThings) {
                        validationBean.setErrorDescription("You have reached the limit (" + maxNumberOfThings + ") to create things.");
                    }
                }
            }
            if (validationBean.isError()) {
                messages.add(validationBean.getErrorDescription());
            }
        }
    }

    private List validateAndGetChildrenUDF(CrudParameters parameters, List<String> messages) {
        List childrenUdf = null;
        if (parameters.getValidations() == null || parameters.getValidations().get("thing.childrenUDF")) {
            if (parameters.getChildrenUdf() != null && !parameters.getChildrenUdf().isEmpty()) {
                //WTF ???!!
                /*Map<String, Object> validaChildren = validateChildrenUdf(parameters.getChildrenUdf());
                if (((ValidationBean) validaChildren.get("validationBean")).isError()) {
                    messages.add(((ValidationBean) validaChildren.get("validationBean")).getErrorDescription());
                } else {
                    result.put("childrenUdf", validaChildren.get("lstChildren"));
                }*/
                childrenUdf = parameters.getChildrenUdf();
            }
        } else {
            /*result.put("childrenUdf", parameters.getChildrenUdf());*/
            childrenUdf = parameters.getChildrenUdf();
        }
        return childrenUdf;
    }
}
