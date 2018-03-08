package com.tierconnect.riot.iot.services.thing.control;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.LocalMap;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ValidationBean;
import com.tierconnect.riot.iot.services.LocalMapService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.iot.services.ThingsService;
import com.tierconnect.riot.iot.services.thing.entity.CrudParameters;
import com.tierconnect.riot.iot.utils.VisibilityThingUtils;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vealaro on 8/21/17.
 */
abstract class ParameterExtractor {
    private static Logger logger = Logger.getLogger(ParameterExtractor.class);

    protected ThingType getThingType(CrudParameters parameters, List<String> messages) {
        //TODO: Implement support for ThingTypeCode by tenant
        ThingType thingType = null;
        try {
            if (parameters.getCache() != null) {
                thingType = parameters.getCache().getThingType(parameters.getThingTypeCode());
            } else if (parameters.getValidations() == null || parameters.getValidations().get("thingType")) {
                if (parameters.getThingTypeCode() != null) {
                    /*thingType = ThingTypeService.getInstance().getByCode(parameters.getThingTypeCode());*/
                    thingType = ThingTypeService.getInstance().getFromCache(parameters.getThingTypeCode());
                    if (thingType == null) {
                        messages.add("Error getting thingType with code: " + parameters.getThingTypeCode());
                    }
                }
            }
        } catch (Exception e) {
            messages.add("Error getting thingType with code: " + parameters.getThingTypeCode());
        }
        return thingType;
    }

    /*private TimerUtil initGetGroup() {
        List<String> laps = new ArrayList<>();
        laps.add("[ThingParameterExtractor] : Get group Step 1");
        laps.add("[ThingParameterExtractor] : Get group Step 2");
        laps.add("[ThingParameterExtractor] : Get group Step 3");
        laps.add("[ThingParameterExtractor] : Get group Step 4");
        laps.add("[ThingParameterExtractor] : Get group Step 5");
        laps.add("[ThingParameterExtractor] : Get group Step 6");

        TimerUtil tu = new TimerUtil(true);
        String[] lapList = new String[laps.size()];
        lapList = laps.toArray(lapList);
        tu.initLaps(lapList);
        return tu;
    }*/

    protected Group getGroup(CrudParameters parameters, List<String> messages) {
        Group group = null;
        //TimerUtil tu = initGetGroup();
        //tu.start("[ThingParameterExtractor] : Get group Step 6");
        if (parameters.getValidations() == null || parameters.getValidations().get("group")) {
            //tu.start("[ThingParameterExtractor] : Get group Step 1");
            String groupHierarchyCode = parameters.getGroupHierarchyCode();
            //tu.stop("[ThingParameterExtractor] : Get group Step 1");
            //tu.start("[ThingParameterExtractor] : Get group Step 2");
            ThingType thingType = parameters.getThingType();
            if (thingType != null && groupHierarchyCode == null) {
                //tu.start("[ThingParameterExtractor] : Get group Step 3");
                groupHierarchyCode = thingType.getGroup().getHierarchyName(false);
                //tu.stop("[ThingParameterExtractor] : Get group Step 3");
            }
            //tu.stop("[ThingParameterExtractor] : Get group Step 2");
            //tu.start("[ThingParameterExtractor] : Get group Step 4");
            Map<String, Object> groupResultValidation = validateAndGetGroup(groupHierarchyCode, parameters);
            //tu.stop("[ThingParameterExtractor] : Get group Step 4");
            //tu.start("[ThingParameterExtractor] : Get group Step 5");
            ValidationBean validationBean = (ValidationBean) groupResultValidation.get("validationBean");
            if (validationBean.isError()) {
                messages.add(validationBean.getErrorDescription());
            } else {
                group = (Group) groupResultValidation.get("group");
            }
            //tu.stop("[ThingParameterExtractor] : Get group Step 5");
        } else if (parameters.getThingToProcess() != null) {
            group = parameters.getThingToProcess().getGroup();
        }
        //tu.stop("[ThingParameterExtractor] : Get group Step 6");
        //logger.info("Thing Parameters Extractor: " + tu.getLogString());
        return group;
    }

    protected Group validateAndGetGroupFacility(CrudParameters parameters, List<String> messages) throws NonUniqueResultException {
        Group groupFacilityMap = null;
        String facilityCode = parameters.getFacilityCode();
        Group group = parameters.getGroup();
        if (parameters.getUdfs() != null && parameters.getUdfs().containsKey("location")
                && StringUtils.isNotEmpty(facilityCode)) {
            LocalMap localMap = LocalMapService.getInstance().selectByCode(facilityCode);
            if (localMap != null) {
                String groupHierarchyCode = localMap.getGroup().getHierarchyName();
                groupFacilityMap = GroupService.getInstance().getByHierarchyCode(groupHierarchyCode);
                if (groupFacilityMap != null) {
                    boolean hasFacilityMap = LocalMapService.getInstance().selectAllByGroupId(groupFacilityMap.getId()).isEmpty();
                    if (hasFacilityMap) {
                        messages.add("FacilityCode [" + facilityCode + "] already has an unique group.");
                    }
                }
            } else {
                messages.add("FacilityCode [" + facilityCode + "] not found.");
            }
        } else {
            groupFacilityMap = group;
        }
        return groupFacilityMap;
    }

    /*private TimerUtil initValidateGetGroup() {
        List<String> laps = new ArrayList<>();
        laps.add("[ThingParameterExtractor] : Validate Get group Step 1");
        laps.add("[ThingParameterExtractor] : Validate Get group Step 2");
        laps.add("[ThingParameterExtractor] : Validate Get group Step 3");
        laps.add("[ThingParameterExtractor] : Validate Get group Step 4");
        laps.add("[ThingParameterExtractor] : Validate Get group Step 5");
        laps.add("[ThingParameterExtractor] : Validate Get group Step 6");
        laps.add("[ThingParameterExtractor] : Validate Get group Step 7");

        TimerUtil tu = new TimerUtil(true);
        String[] lapList = new String[laps.size()];
        lapList = laps.toArray(lapList);
        tu.initLaps(lapList);
        return tu;
    }*/

    private Map<String, Object> validateAndGetGroup(String groupHierarchyCode, CrudParameters parameters) {
        logger.debug("Validating: " + groupHierarchyCode);
        Map<String, Object> result = new HashMap<>();
        Group group = null;
        ValidationBean validationBean = new ValidationBean();
        //TimerUtil tu = initValidateGetGroup();
        try {
            //tu.start("[ThingParameterExtractor] : Validate Get group Step 7");
            if (StringUtils.isNotEmpty(groupHierarchyCode)) {
                //tu.start("[ThingParameterExtractor] : Validate Get group Step 1");
                /*group = GroupService.getInstance().getByHierarchyCode(groupHierarchyCode);*/
                group = GroupService.getInstance().getFromCache(groupHierarchyCode);
                //tu.stop("[ThingParameterExtractor] : Validate Get group Step 1");
                if (null == group) {
                    validationBean.setErrorDescription("Invalid Group");
                } else {
                    //tu.start("[ThingParameterExtractor] : Validate Get group Step 2");
                    if (parameters.getValidateVisibility()) {
                        // validate group level
                        //tu.start("[ThingParameterExtractor] : Validate Get group Step 3");
                        User currentUser = parameters.getCurrentUser();
                        //tu.stop("[ThingParameterExtractor] : Validate Get group Step 3");
                        logger.debug("User: " + currentUser.getId() + "-" + currentUser.getFirstName());
                        //tu.start("[ThingParameterExtractor] : Validate Get group Step 4");
                        Long userLevel = ConfigurationService.getAsLong(currentUser, group, "thing",
                                parameters.getActiveGroup());
                        //tu.stop("[ThingParameterExtractor] : Validate Get group Step 4");
                        if (userLevel == null) {
                            userLevel = 0L;
                        }
                        //tu.start("[ThingParameterExtractor] : Validate Get group Step 5");
                        int thingLevel = group.getTreeLevel();
                        //tu.stop("[ThingParameterExtractor] : Validate Get group Step 5");
                        //tu.start("[ThingParameterExtractor] : Validate Get group Step 6");
                        if (userLevel.intValue() != thingLevel) {
                            validationBean.setErrorDescription("The group's level is " + thingLevel + " and it has to be " + userLevel);
                        }
                        //tu.stop("[ThingParameterExtractor] : Validate Get group Step 6");
                    }
                    //tu.stop("[ThingParameterExtractor] : Validate Get group Step 2");
                }
            } else {
                validationBean.setErrorDescription("Invalid Group");
            }
            //tu.stop("[ThingParameterExtractor] : Validate Get group Step 7");
        /*} catch (NonUniqueResultException e) {
            validationBean.setErrorDescription("Non Unique Result in Group name: " + groupHierarchyCode);*/
        } catch (Exception e) {
            validationBean.setErrorDescription("Un expected error on getting group");
            logger.error(e.getMessage(), e);
        }
        //logger.info("Thing Parameters Extractor: " + tu.getLogString());
        result.put("group", group);
        result.put("validationBean", validationBean);
        return result;
    }

    protected void validateThingName(CrudParameters parameters, List<String> messages) {
        if (Utilities.isEmptyOrNull(parameters.getName())) {
            messages.add("Name must have a value. ");
        }
    }

    protected void validateSerialNumber(CrudParameters parameters, List<String> messages) {
        if (parameters.getValidations() == null || parameters.getValidations().get("thing.serial")) {
            ThingType thingType = parameters.getThingType();
            if (thingType != null &&
                    ((thingType.getSerialFormula() == null) ||
                            (thingType.getSerialFormula() != null) && (thingType.getSerialFormula().trim().equals("")))) {
                String serialNumber = parameters.getSerialNumber();
                ValidationBean response = validateSerialFormat(thingType, serialNumber, parameters.isCreation());
                if (response.isError()) {
                    messages.add(response.getErrorDescription());
                }
            }
        }
    }

    public ValidationBean validateSerialFormat(ThingType thingType, String serialNumber, boolean isCreation) {
        ValidationBean response = new ValidationBean();
        if (StringUtils.isNotEmpty(serialNumber)) {
            if (Utilities.isAlphaNumericCharacterSpecials(serialNumber, "_")) {
                try {
                    if (isCreation) {
                        boolean exists = ThingsService.getInstance().isExistingSerial(thingType.getId(), serialNumber, null);
                        if (exists) {
                            response.setErrorDescription(
                                    String.format("Serial '[%s]' already exist for Thing Type '%s' ", serialNumber,
                                            thingType.getName()));
                        }
                    }
                } catch (Exception e) {
                    response.setErrorDescription("Validate serial > " + e.getMessage());
                }
            } else {
                response.setErrorDescription("Serial has invalid characters, only alphanumeric characters are allowed.");
            }
        } else {
            response.setErrorDescription("Serial must have a value. ");
        }
        return response;
    }

    protected void validateVisibility(CrudParameters parameters, List<String> messages) {
        ThingType thingType = parameters.getThingType();
        Group group = parameters.getGroup();
        if (thingType != null && parameters.getValidateVisibility() && group != null) {
            ValidationBean response = new ValidationBean();
            try {
                VisibilityThingUtils.limitVisibilityInsertT(thingType, group, parameters.getSubject(), false);
            } catch (Exception e) {
                response.setErrorDescription("Visibility error: " + e.getMessage());
            }
            if (response.isError()) {
                messages.add(response.getErrorDescription());
            }
        }
    }

    protected Map<String, Object> validateAndGetParent(CrudParameters parameters, List<String> messages) {
        Map<String, Object> parentObj = null;
        if (parameters.getValidations() == null || parameters.getValidations().get("thing.parent")) {
            Map<String, Object> parent = parameters.getParent();
            ThingType thingType = parameters.getThingType();
            if (thingType != null && parent != null && !parent.isEmpty()) {
                //if (ThingTypeMapService.getInstance().isParent(thingType)) {
                if (thingType.getChildrenTypeMaps().size() > 0) {
                    messages.add("The new thing type is parent and it cannot accept a parent thing type");
                } else {
                    parentObj = ThingsService.getInstance().getThing(parameters.getParent());
                    checkMapWithThingAndThingType(parentObj, messages, (String) parent.get(Constants.SERIAL_NUMBER));
                }
            }
        } else if (parameters.getParentMapOrObject() instanceof Thing) {
            parentObj = new HashMap<>();
            Thing parent = (Thing) parameters.getParentMapOrObject();
            parentObj.put("thing", parent);
            parentObj.put("thingType", parent.getThingType());
            checkMapWithThingAndThingType(parentObj, messages, parent.getSerialNumber());
        }
        return parentObj;
    }

    private void checkMapWithThingAndThingType(Map<String, Object> parentObj, List<String> messages, String serialNumber) {
        if ((parentObj.get("thing") == null) || (parentObj.get("thingType") == null)) {
            messages.add("Parent with serial:" + serialNumber + " does not exist");
        }
    }

    protected List validateAndGetChildren(CrudParameters parameters, List<String> messages) throws NonUniqueResultException {
        List childrenObj = null;
        List<Map<String, Object>> children = parameters.getChildren();
        if (parameters.getValidations() == null || parameters.getValidations().get("thing.children")) {
            ThingType thingType = parameters.getThingType();
            Group group = parameters.getGroup();
            if (thingType != null && children != null && group != null && !children.isEmpty()) {
                //if (ThingTypeMapService.getInstance().isChild(thingType)) {
                if (thingType.getParentTypeMaps().size() > 0) {
                    messages.add("Thing Type of the Thing is child and it cannot accept children thing types");
                } else {
                    Map<String, Object> validaChildren = ThingsService.getInstance().validateChildren(
                            children,
                            group,
                            parameters.getValidateVisibility(),
                            parameters.getTransactionDate());
                    if (((ValidationBean) validaChildren.get("validationBean")).isError()) {
                        messages.add(((ValidationBean) validaChildren.get("validationBean")).getErrorDescription());
                    } else {
                        childrenObj = (List) validaChildren.get("lstChildren");
                    }
                }
            }
        } else {
            childrenObj = children;
        }
        return childrenObj;
    }

    protected void validateUdfs(CrudParameters parameters, List<String> messages) {
        if (parameters.getValidations() == null || parameters.getValidations().get("thing.udfs")) {
            ThingType thingType = parameters.getThingType();
            Group group = parameters.getGroup();
            if (thingType != null && parameters.getUdfs() != null && (group != null)) {
                ValidationBean valUdfs = ThingsService.getInstance().validationUdfs(thingType, parameters.getUdfs(),
                        group, parameters.getValidateVisibility(), parameters.getTransactionDate());
                if (valUdfs.isError()) {
                    messages.add(valUdfs.getErrorDescription());
                }
            }
        }
    }
}
