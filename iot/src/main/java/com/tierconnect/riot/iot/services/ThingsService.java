package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.appcore.controllers.GroupController;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.LicenseDetail;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.LicenseService;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.GeneralVisibilityUtils;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.utils.DateHelper;
import com.tierconnect.riot.commons.utils.DateTimeFormatterHelper;
import com.tierconnect.riot.commons.utils.FormatUtil;
import com.tierconnect.riot.commons.utils.TimerUtil;
import com.tierconnect.riot.iot.controllers.LogicalReaderController;
import com.tierconnect.riot.iot.controllers.ShiftController;
import com.tierconnect.riot.iot.controllers.ThingController;
import com.tierconnect.riot.iot.controllers.ZoneController;
import com.tierconnect.riot.iot.dao.SequenceDAO;
import com.tierconnect.riot.iot.dao.ThingDAO;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.dao.util.FormulaUtil;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.popdb.PopulateThings;
import com.tierconnect.riot.iot.services.thing.ThingBoundary;
import com.tierconnect.riot.iot.utils.VisibilityThingUtils;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;

import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;

import static com.tierconnect.riot.commons.serializers.Constants.DATE_FORMAT;

/**
 * Created by rchirinos on 4/1/2016.
 */
public class ThingsService {
    private static Logger logger = Logger.getLogger(ThingsService.class);
    static private ThingsService INSTANCE = new ThingsService();

    private static ThingDAO _thingDAO = new ThingDAO();

    public static ThingDAO getThingDAO() {
        return _thingDAO;
    }

    public static ThingsService getInstance() {
        return INSTANCE;
    }

    public Map<String, Object> create(
            Stack<Long> recursivelyStack,
            String thingTypeCode,
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
            Boolean useDefaultValues,
            boolean fillSource) throws UserException {
        return create(
                recursivelyStack,
                thingTypeCode,
                groupHierarchyCode,
                name,
                serialNumber,
                parent,
                udfs,
                children,
                childrenUdf,
                executeTickle,
                validateVisibility,
                transactionDate,
                false,
                true,
                useDefaultValues,
                null,
                null,
                fillSource,
                null);
    }

    public Map<String, Object> create(
            Stack<Long> recursivelyStack,
            String thingTypeCode,
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
            Boolean useDefaultValues,
            String facilityCode,
            boolean fillSource) throws UserException {
        return create(
                recursivelyStack,
                thingTypeCode,
                groupHierarchyCode,
                name,
                serialNumber,
                parent,
                udfs,
                children,
                childrenUdf,
                executeTickle,
                validateVisibility,
                transactionDate,
                false,
                true,
                useDefaultValues,
                null,
                facilityCode,
                fillSource,
                null);
    }

    /**
     * @param thingTypeCode      Code of thing type
     * @param groupHierarchyCode Hirearchy Name of the group
     * @param name               Name of the thing
     * @param serialNumber       Serial Number of the thing
     * @param parent             Parent of the thing (optional)
     * @param udfs               UDFs of the thing (optional)
     * @param children           Children of the thing (optional)
     * @param childrenUdf        Children UDFs of the thing (optional)
     * @param executeTickle      Boolean value to execute tickle or not
     * @param validateVisibility Boolean value to execute validation of visibility or not
     * @param transactionDate    Date of the transaction
     * @param disableFMCLogic    Boolean value to execute FMC logic or not
     * @param createAndFlush     Boolean value to flush values in MySQL or not
     * @param useDefaultValues   Boolean value to use default values of the thing type or not
     * @param recursivelyStack   LIFO value to control recursive updating of the thing type or not
     * @return
     * @throws UserException
     */
    public Map<String, Object> create(
            Stack<Long> recursivelyStack,
            String thingTypeCode,
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
            Subject subject) throws UserException {
            return ThingBoundary.getInstance().create(
                    recursivelyStack,
                    thingTypeCode,
                    groupHierarchyCode,
                    name,
                    serialNumber,
                    parent,
                    udfs,
                    children,
                    childrenUdf,
                    executeTickle,
                    validateVisibility,
                    transactionDate,
                    disableFMCLogic,
                    createAndFlush,
                    useDefaultValues,
                    validations,
                    facilityCode,
                    fillSource,
                    subject);
    }

    /**
     * This method validates the input parameters
     *
     * @param thingTypeCode
     * @param groupHierarchyCode
     * @param name               Name of the thing
     * @param serialNumber       Serial number of the thing
     * @param parent             Parent of the thing
     * @param udfs               UDFs of the thing
     * @param children           Children of the thing
     * @param validateVisibility Boolean value for validating or not the visibility
     * @param transactionDate    Transaction Date
     * @return Map () Contains validationBean (true or false), Beans of: thingType, Group, Parent, Children
     */
    public Map<String, Object> validateCreateThing(
            String thingTypeCode,
            String groupHierarchyCode,
            String name,
            Object serialNumber,
            Object parent,
            Map<String, Object> udfs,
            Object children,
            Object childrenUdf,
            boolean validateVisibility,
            Date transactionDate,
            Map<String, Boolean> validations,
            User currentUser) {
        Map<String, Object> result = new HashMap<>();
        List<String> messages = new ArrayList<>();
        ValidationBean validationBean = new ValidationBean();
        try {
            //Validate Thing Type Code
            ThingType thingType = null;
            try {
                if (validations == null || validations.get("thingType")) {
                    if (thingTypeCode != null) {
                        thingType = ThingTypeService.getInstance().getByCode(thingTypeCode);
                        if (thingType == null) {
                            messages.add("Error getting thingType with code: " + thingTypeCode);
                        } else {
                            result.put("thingType", thingType);
                        }
                    }
                }
            } catch (NonUniqueResultException e) {
                messages.add("Error getting thingType with code: " + thingTypeCode);
            }
            // Validate and get group. If groupHierarchyCode is null, we will get the group of the ThingType
            Group group = null;
            if(validations == null || validations.get("group")){
                if (thingType != null && groupHierarchyCode == null) {
                    groupHierarchyCode = thingType.getGroup().getHierarchyName(false);
                }
                Map<String, Object> groupResultValidation = validateAndGetGroup(groupHierarchyCode, validateVisibility, currentUser);
                validationBean = (ValidationBean) groupResultValidation.get("validationBean");
                if (validationBean.isError()) {
                    messages.add(validationBean.getErrorDescription());
                } else {
                    group = (Group) groupResultValidation.get("group");
                    result.put("group", group);
                }
            }
            //Validate Max Number of things
            if(validations == null || validations.get("thing.maxNumberThings")){
                ValidationBean valMaxThing = this.validateMaxNumberOfThings(currentUser);
                if (valMaxThing.isError()) {
                    messages.add(valMaxThing.getErrorDescription());
                }
                if (name == null || name.trim().isEmpty()) {
                    messages.add("Name must have a value. ");
                }
            }
            //Validate Serial
            if(validations == null || validations.get("thing.serial")){
                if ( thingType != null &&
                        ( (thingType.getSerialFormula() == null ) ||
                                (thingType.getSerialFormula()!=null ) && (thingType.getSerialFormula().trim().equals("") ) )) {
                    validationBean = this.validateSerial(thingType, serialNumber, null);
                    if (validationBean.isError()) {
                        messages.add(validationBean.getErrorDescription());
                    }
                }
            }
            //Visibility
            if (thingType != null && validateVisibility && group != null) {
                validationBean = validateVisibility(thingType, group);
                if (validationBean.isError()) {
                    messages.add(validationBean.getErrorDescription());
                }
            }
            //Validate Parent
            if(validations == null || validations.get("thing.parent")){
                if (thingType != null && parent != null && parent instanceof Map && !((Map) parent).isEmpty()) {
                    if (ThingTypeMapService.getInstance().isParent(thingType)) {
                        messages.add("The new thing type is parent and it cannot accept a parent thing type");
                    } else {
                        Map<String, Object> parentObj = this.getThing((Map<String, Object>) parent);
                        if ((parentObj.get("thing") != null) && (parentObj.get("thingType") != null)) {
                            result.put("parent", parentObj);
                        } else {
                            messages.add("Parent with serial:" + ((Map) parent).get("serialNumber") + " does not exist");
                        }
                    }
                }
            }
            //Validate children
            if(validations == null || validations.get("thing.children")){
                if (thingType != null && children != null && group != null && children instanceof List && ((List) children).size() > 0) {
                    if (ThingTypeMapService.getInstance().isChild(thingType)) {
                        messages.add("Thing Type of the Thing is child and it cannot accept children thing types");
                    } else {
                        Map<String, Object> validaChildren = validateChildren(
                                children
                                , group
                                , validateVisibility
                                , transactionDate);
                        if (((ValidationBean) validaChildren.get("validationBean")).isError()) {
                            messages.add(((ValidationBean) validaChildren.get("validationBean")).getErrorDescription());
                        } else {
                            result.put("children", validaChildren.get("lstChildren"));
                        }
                    }
                }
            } else {
                result.put("children", children);
            }
            //Validate children UDF
            if(validations == null || validations.get("thing.childrenUDF")){
                if (childrenUdf != null && childrenUdf instanceof List && !((List) childrenUdf).isEmpty()) {
                    Map<String, Object> validaChildren = validateChildrenUdf(childrenUdf);
                    if (((ValidationBean) validaChildren.get("validationBean")).isError()) {
                        messages.add(((ValidationBean) validaChildren.get("validationBean")).getErrorDescription());
                    } else {
                        result.put("childrenUdf", validaChildren.get("lstChildren"));
                    }
                }
            } else {
                result.put("childrenUdf", childrenUdf);
            }
            //Validate Udf's
            if(validations == null || validations.get("thing.udfs")){
                if (thingType != null && udfs != null && (group != null)) {
                    ValidationBean valUdfs = validationUdfs(thingType, udfs, group, validateVisibility, transactionDate);
                    if (valUdfs.isError()) {
                        messages.add(valUdfs.getErrorDescription());
                    }
                }
            }
            /*Validate duplicity in labels of UDf's*/
            /*Assign parent or children, but not both */
            if (!messages.isEmpty()) {
                validationBean.setErrorDescription(StringUtils.join(messages, ";"));
            }
        } catch (Exception e) {
            logger.error("Error in validation ", e);
            throw new UserException("Error in validation " + e.getMessage(), e);
        }

        result.put("validationBean", validationBean);
        return result;
    }

    public Map<String, Object> validateAndGetGroup(String groupHierarchyCode, User currentUser, ThingType thingType, boolean validateVisibility, String facilityCode) {
        ValidationBean validationBean = new ValidationBean();
        Map<String, Object> groupResultValidation = new HashMap<>();
        LocalMap localMap = LocalMapService.getInstance().selectByCode(facilityCode);
        if (localMap == null) {
            validationBean.setErrorDescription("FacilityCode [" + facilityCode + "] not found.");
            groupResultValidation.put("validationBean", validationBean);
            return groupResultValidation;
        }
        groupHierarchyCode = localMap.getGroup().getHierarchyName();
        groupResultValidation = validateAndGetGroup(groupHierarchyCode, false, currentUser);
        if (groupHasAnUniqueFacilityMap(groupResultValidation)) {
            return groupResultValidation;
        }
        Group group = (Group) groupResultValidation.get("group");

        validationBean.setErrorDescription("Location fields cannot be calculated because the group [" + group.getName() + "] of the thing has not any facility map.");
        groupResultValidation.put("validationBean", validationBean);
        return groupResultValidation;
    }

    private boolean groupHasAnUniqueFacilityMap(Map<String, Object> groupResultValidation) {
        Group group;
        List<LocalMap> maps;
        ValidationBean validationBean = (ValidationBean) groupResultValidation.get("validationBean");
        if (!validationBean.isError()) {
            group = (Group) groupResultValidation.get("group");
            maps = LocalMapService.getInstance().selectAllByGroupId(group.getId());
            return (!maps.isEmpty());
        }
        return false;
    }

    /**************************************************
     * get thing
     ***************************************************/
    public Map<String, Object> getThing(Map<String, Object> thingMap) {
        Map<String, Object> response = new HashMap<>();
        try {
            if ((thingMap.get("serialNumber") != null) && (thingMap.get("thingTypeCode") != null)) {
                ThingType thingType = ThingTypeService.getInstance().getByCode((String) thingMap.get("thingTypeCode"));
                if (thingType != null) {
                    response.put("thingType", thingType);
                    Thing thing = ThingService.getInstance().getBySerialNumber((String) thingMap.get("serialNumber"), thingType);
                    if (thing != null) {
                        response.put("thing", thing);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception in getThing with error: " + e.getMessage(), e);
            response = null;
        }
        return response;
    }

    /**
     * This method validate if a group is valid or not
     */
    public Map<String, Object> validateAndGetGroup(String groupHierarchyCode, boolean validateVisibility) {
        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        return validateAndGetGroup(groupHierarchyCode, validateVisibility, currentUser);
    }

    /**
     * This method validate if a group is valid or not
     */
    public Map<String, Object> validateAndGetGroup(String groupHierarchyCode, boolean validateVisibility, User currentUser) {
        logger.debug("val: " + groupHierarchyCode);
        Map<String, Object> result = new HashMap<String, Object>();
        Group group = null;
        ValidationBean validationBean = new ValidationBean();
        try {
            if (!Utilities.isEmptyOrNull(groupHierarchyCode)) {
                group = GroupService.getInstance().getByHierarchyCode(groupHierarchyCode);
                if (null == group) {
                    validationBean.setErrorDescription("Invalid Group");
                } else {
                    if (validateVisibility) {
                        // validate group level
                        logger.debug("user: " + currentUser.getId() + "-" + currentUser.getFirstName());
                        Long userLevel = ConfigurationService.getAsLong(currentUser, group, "thing");
                        if (userLevel == null) {
                            userLevel = 0L;
                        }
                        int thingLevel = group.getTreeLevel();
                        if (userLevel.intValue() != thingLevel) {
                            validationBean.setErrorDescription("The group's level is " + thingLevel + " and it has to be " + userLevel);
                        }
                    }
                }
            } else {
                validationBean.setErrorDescription("Invalid Group");
            }
        } catch (NonUniqueResultException e) {
            validationBean.setErrorDescription("Non Unique Result in Group name: " + groupHierarchyCode);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            validationBean.setErrorDescription("Error in validate group: " + groupHierarchyCode);
        }
        result.put("group", group);
        result.put("validationBean", validationBean);
        return result;
    }

    /*****************************************************
     * This method validate if a serial is valid or not
     ******************************************************/
    public ValidationBean validateSerial(ThingType thingType, Object serial, Long thingId) {
        ValidationBean response = new ValidationBean();
        if (serial == null) {
            response.setErrorDescription("Serial must have a value. ");
        } else {
            if ((serial).toString().trim().isEmpty()) {
                response.setErrorDescription("Serial must have a value.");
            } else {
                if (!Utilities.isAlphaNumeric(serial.toString())) {
                    response.setErrorDescription("Serial has invalid characters, only alphanumeric characters are allowed.");
                } else {
                    try {
                        if (isExistingSerial(thingType.getId(), serial, thingId)) {
                            response.setErrorDescription(
                                    String.format("Serial '[%s]' already exist for Thing Type '%s' ", serial, thingType.getName()));
                        }
                    } catch (Exception e) {
                        response.setErrorDescription("Validate serial > " + e.getMessage());
                    }

                }
            }
        }
        return response;
    }

    /****************************************************
     * this method Checks if the serial is existent or not
     ***************************************************/
    public boolean isExistingSerial(Long thingTypeId, Object serial, Long excludeId) {
        boolean response = false;
        try {
            if (excludeId != null && this.existsSerial(serial.toString(), thingTypeId, excludeId)) {
                response = true;
            } else if (excludeId == null && this.existsSerial(serial.toString(), thingTypeId)) {
                response = true;
            }
        } catch (Exception e) {
            response = false;
        }
        return response;
    }

    public boolean existsSerial(String serial, Long thingTypeId) {
        return getThingDAO().existsSerial(serial, thingTypeId);
    }

    public boolean existsSerial(String serial, Long thingTypeId, Long excludeId) {
        return getThingDAO().existsSerial(serial, thingTypeId, excludeId);
    }

    /******************************************
     * Validate Thing Type code
     *****************************************/
    public Map<String, Object> validateAndGetThingType(String thingTypeCode) {
        Map<String, Object> response = new HashMap<>();
        ValidationBean validationBean = new ValidationBean();
        ThingType thingType = null;
        if (thingTypeCode == null || thingTypeCode.isEmpty()) {
            validationBean.setErrorDescription("Invalid Thing Type. ");
        } else {
            try {
                thingType = ThingTypeService.getInstance().getByCode(thingTypeCode);
                if (thingType == null) {
                    validationBean.setErrorDescription("Thing Type with code:" + thingTypeCode + " does not exist");
                }
            } catch (Exception e) {
                validationBean.setErrorDescription("Invalid Thing Type. ");
            }
        }
        response.put("thingType", thingType);
        response.put("validationBean", validationBean);
        return response;
    }

    /******************************************
     * Validate visibility of the group and thingType
     *****************************************/
    public ValidationBean validateVisibility(ThingType thingType, Group group) {
        ValidationBean response = new ValidationBean();
        try {
            VisibilityThingUtils.limitVisibilityInsertT(thingType, group);
        } catch (Exception e) {
            response.setErrorDescription("Visibility error: " + e.getMessage());
        }
        return response;
    }


    /******************************************
     * Validate children of the thing
     *****************************************/
    public Map<String, Object> validateChildren(
            Object children,
            Group group,
            boolean validateVisibility,
            Date transactionDate) throws NonUniqueResultException {
        Map<String, Object> response = new HashMap<>();
        ValidationBean validationBean = new ValidationBean();

        for (Object child : (List) children) {
            Map<String, Object> childData = (Map<String, Object>) child;
            Map<String, Object> childObj = this.getThing(childData);
            if (childObj != null){
                childData.putAll(childObj);
            }
            Thing childThing = childObj != null? childObj.get("thing") != null ? (Thing) childObj.get("thing") : null : null;
            ThingType thingTypeChild = childObj != null? childObj.get("thingType") != null ? (ThingType) childObj.get("thingType") : null : null;
            if ((childThing != null) && (ThingService.getInstance().getFirstChild(childThing) != null)) {
                validationBean.setErrorDescription("The thing [ " + childThing.getId() + " ] is a parent.");
                break;
            }
            if (thingTypeChild != null) {
                //Check Udf's of the Children
                ValidationBean valUdfs = validationUdfs(
                        thingTypeChild
                        , (Map<String, Object>) childData.get("udfs")
                        , group
                        , validateVisibility
                        , transactionDate);
                if (valUdfs.isError()) {
                    validationBean.setErrorDescription(valUdfs.getErrorDescription());
                    break;
                }
            } else {
                validationBean.setErrorDescription("The Thing Type of the child [ " + childThing.getSerialNumber() +
                        " ] is invalid.");
                break;
            }
        }
        response.put("validationBean", validationBean);
        response.put("lstChildren", children);
        return response;
    }

    /**
     * Validate childrenUDdf
     *
     * @param childrenUdf
     * @return
     * @throws NonUniqueResultException
     */
    public Map<String, Object> validateChildrenUdf(Object childrenUdf) throws NonUniqueResultException {
        Map<String, Object> response = new HashMap<>();
        ValidationBean validationBean = new ValidationBean();

        for (Object child : (List) childrenUdf) {
            Map<String, Object> childThingMap = (Map<String, Object>) child;
            Map<String, Object> childObj = this.getThing(childThingMap);
            if (childObj != null){
                childThingMap.putAll(childObj);
            }
        }
        response.put("validationBean", validationBean);
        response.put("lstChildren", childrenUdf);
        return response;
    }

    /******************************************
     * Validation of UDF's
     ******************************************/
    public ValidationBean validationUdfs(
            ThingType thingType
            , Map<String, Object> udfsData
            , Group group
            , boolean validateVisibility
            , Date transactionDate) {
        ValidationBean response = new ValidationBean();
        if (udfsData != null) {
            //Get the definition of the thing
            //Iterate Udf's data
            for (Map.Entry<String, Object> udfObject : udfsData.entrySet()) {
                int cont = 0;
                //Iterate fields of the config of the thing type
                Set<ThingTypeField> thingTypeFields = thingType.getThingTypeFields();
                for (ThingTypeField thingTypeField : thingTypeFields) {
                    String udfLabel = udfObject.getKey().trim();
                    DataType dataType = DataTypeService.getInstance().get(thingTypeField.getDataType().getId());
                    if (udfLabel.equals(thingTypeField.getName())) {
                        if (udfLabel.equals("lastDetectTime") && udfObject.getValue().toString().equals("{value=0}")) {
                            udfsData.remove(udfLabel);
                        }
                        Map<String, Object> udfMap = (Map<String, Object>) udfObject.getValue();
                        if (thingTypeField.getDataType().getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value)
                                && dataType.getType().equals(ThingTypeField.TypeParentSubGroup.TYPE_PARENT_DATA_TYPE_STANDARD_DATA.value)) {
                            response = validationStandardDataTypes(thingTypeField, udfLabel, udfMap);
                            if (!response.isError() && udfMap.get("value") != null && !StringUtils.isEmpty(udfMap.get("value").toString())
                                    && ThingTypeFieldService.getInstance().isValidDataTypeToCheck(dataType.getId())) {
                                response = ThingTypeFieldService.getInstance()
                                        .validateFieldValue(udfLabel, udfMap.get("value"), dataType.getId());
                            }
                            break;
                        } else if (thingTypeField.getDataType().getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value)
                                && dataType.getType().equals(ThingTypeField.TypeParentSubGroup.TYPE_PARENT_DATA_TYPE_NATIVE_OBJECT.value)) {
                            response = validationNativeObject(
                                    thingTypeField, udfLabel, udfMap, group, validateVisibility, transactionDate);
                            break;
                        } else if (thingTypeField.getDataType().getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_NATIVE_THING_TYPE.value)) {
                            response = validationNativeThingType(thingTypeField, udfLabel, udfMap, group, validateVisibility);
                            break;
                        }
                    } else {
                        cont++;
                    }
                }
                if (response.isError()) {
                    break;
                }
                if (cont == thingTypeFields.size() && !StringUtils.equals(Constants.UDF_SOURCE, udfObject.getKey())) {
                    response.setErrorDescription("UDF Thing Type: " + udfObject.getKey() + " does not exist.");
                    break;
                }
            }
        }
        return response;
    }

    /**
     * This method checks if the Standard data type Object is valid
     *
     * @param thingTypeField field of thing type
     * @param udfLabel       label
     * @param udfMap         udf's
     * @return Object with error description
     */
    @Deprecated
    public ValidationBean validationStandardDataTypes(ThingTypeField thingTypeField, String udfLabel,  Map<String, Object> udfMap) {
        ValidationBean response = new ValidationBean(false);
        try {
            Object udfValue = udfMap.get("value");
            //Check Multivalue, Array of names
            if (udfValue instanceof String[]) {
                String[] udfValueName = (String[]) udfValue;
                if (udfValueName.length > 0) {
                    for (String anUdfValueName : udfValueName) {
                        ValidationBean standardValidDataType = this.isStandardValidDataType(thingTypeField.getDataType(), anUdfValueName);
                        if (standardValidDataType.isError()) {
                            response.setErrorDescription("Standard data type property [" + udfLabel + "]. The value ["
                                    + Arrays.toString(udfValueName) + "] does not correspond to the type of data.");
                            if (!Utilities.isEmptyOrNull(standardValidDataType.getErrorDescription())) {
                                response.setErrorDescription(standardValidDataType.getErrorDescription());
                            }
                            break;
                        }
                    }
                }
                //Check single name
            } else {
                ValidationBean standardValidDataType = this.isStandardValidDataType(thingTypeField.getDataType(), udfValue);
                if (standardValidDataType.isError()) {
                    response.setErrorDescription(
                            "Standard data type property [" + udfLabel + "]. The value ["
                                    + udfValue + "] does not correspond to the type of data '" + thingTypeField.getDataType().getClazz() + "'.");
                    if (!Utilities.isEmptyOrNull(standardValidDataType.getErrorDescription())) {
                        response.setErrorDescription(standardValidDataType.getErrorDescription());
                    }
                } else {
                    if (thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_ATTACHMENTS.value) == 0 &&
                            !this.isJSONValid(udfValue)) {
                        response.setErrorDescription("Standard data type property [" + udfLabel + "]. The value [" + udfValue
                                + "] does not have a correct format.");
                    } else if ((com.tierconnect.riot.commons.DataType.XYZ.equals(thingTypeField.getDataType().getCode()) ||
                            com.tierconnect.riot.commons.DataType.COORDINATES.equals(thingTypeField.getDataType().getCode())) &&
                            !AppLocationAdapter.isValidLocation(udfMap, thingTypeField.getDataType().getCode())) {
                        if (com.tierconnect.riot.commons.DataType.XYZ.equals(thingTypeField.getDataType().getCode())) {
                            response.setErrorDescription(udfLabel + ": '" + udfValue + "' is invalid, the correct format is: " +
                                    "X;Y;Z with number data, please check it and try again.");
                        } else {
                            response.setErrorDescription(udfLabel + ": '" + udfValue + "' is invalid, the correct format is: " +
                                    "Longitude;Latitude;Altitude with number data, please check it and try again.");
                        }
                    } else if ((com.tierconnect.riot.commons.DataType.COLOR.equals(thingTypeField.getDataType().getCode()))) {
                        if (!String.valueOf(udfValue).matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
                            response.setErrorDescription(udfLabel + ": '" + udfValue + "' is not a valid color, " +
                                "please check it and try again.");
                        }
                    } else if ((com.tierconnect.riot.commons.DataType.ICON.equals(thingTypeField.getDataType().getCode()))
                        && udfValue != null &&!StringUtils.isBlank(String.valueOf(udfValue))) {
                        String path = FilenameUtils.getFullPath(this.getClass().getProtectionDomain()
                            .getCodeSource().getLocation().toURI().getPath()) + "../../" + Constants.TYPE_ICON_PATH;
                        InputStream is = new FileInputStream(path + "/css/" + Constants.TYPE_ICON_PREFIX + "-codes.css");
                        if (is == null) {
                            response.setErrorDescription("Cannot find any icon.");
                        }
                        String[] text = IOUtils.toString(is, "UTF-8").split("\n");
                        boolean control = false;
                        for (String line : text) {
                            if (!StringUtils.isBlank(line)
                                && (line.substring(1).split(":")[0]).equals(udfValue)) {
                                control = true;
                                break;
                            }
                        }
                        if (!control) {
                            response.setErrorDescription(udfLabel + ": '" + udfValue + "' is not a valid icon, " +
                                "please check it and try again.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            response.setErrorDescription(e.getMessage());
        }
        return response;
    }

    /************************************
     * this method checks if the Native Object is valid
     ************************************/
    @Deprecated
    public ValidationBean validationNativeObject(
            ThingTypeField thingTypeField,
            String udfLabel,
            Map<String, Object> udfMap,
            Group group,
            boolean validateVisibility,
            Date transactionDate) {
        ValidationBean response = new ValidationBean();

        try {
            Object udfValue = udfMap.get("value");
            //Check Multivalue, Array of names
            if (udfValue != null && udfValue instanceof String[]) {
                String[] udfValueName = (String[]) udfValue;
                if (udfValueName.length > 0) {
                    for (int i = 0; i < udfValueName.length; i++) {
                        Object nativeObject = ThingTypeFieldService.getInstance().getNativeObject(thingTypeField.getDataType().getId(),
                                Arrays.toString(udfValueName), null, group, transactionDate);
                        if (nativeObject == null) {
                            response.setErrorDescription("Native Object property [" + udfLabel + "]. The value [" + Arrays.toString(udfValueName) + "] does not exist.");
                            break;
                        } else {
                            if (validateVisibility && validationVisibilityUdf(nativeObject, group).isError()) {
                                response.setErrorDescription("Native Object property [" + udfLabel + "] The value [" + Arrays.toString(udfValueName)
                                        + "] Wrong visibility." + validationVisibilityUdf(nativeObject, group).getErrorDescription());
                            }
                        }
                    }
                }
                //Check single name
            } else if (udfValue != null && udfValue instanceof String) {
                Object nativeObject = ThingTypeFieldService.getInstance().getNativeObject(
                        thingTypeField.getDataType().getId(),
                        udfValue.toString(),
                        null, group,
                        transactionDate);
                if (nativeObject == null) {
                    response.setErrorDescription("Native Object property [" + udfLabel + "]. The value [" + udfValue + "] does not exist.");
                }
            }
        } catch (Exception e) {
            response.setErrorDescription(e.getMessage());
        }
        return response;
    }

    /************************************
     * this method checks if the Native Objec or
     * Things UDFs are with the correct Visibility
     ************************************/
    public ValidationBean validationVisibilityUdf(Object nativeObject, Group groupUser) {
        ValidationBean response = new ValidationBean();

        try {
            EntityVisibility entityVisibility = null;
            if (nativeObject instanceof Group) {
                entityVisibility = (new GroupController()).getEntityVisibility();
            } else if (nativeObject instanceof Shift) {
                entityVisibility = (new ShiftController()).getEntityVisibility();
            } else if (nativeObject instanceof LogicalReader) {
                entityVisibility = (new LogicalReaderController()).getEntityVisibility();
            } else if (nativeObject instanceof Zone) {
                entityVisibility = (new ZoneController()).getEntityVisibility();
            } else if (nativeObject instanceof Thing) {
                entityVisibility = (new ThingController()).getEntityVisibility();
            }

            GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, nativeObject, groupUser);
        } catch (ForbiddenException e) {
            response.setErrorDescription(e.getMessage());
        }
        return response;
    }

    /************************************
     * this method checks if the Native thing type Object is valid
     ************************************/
    @Deprecated
    public ValidationBean validationNativeThingType(ThingTypeField thingTypeField, String udfLabel, Map<String, Object> udfMap,
                                                    Group group, boolean validateVisibility) {
        ValidationBean response = new ValidationBean();

        try {
            Object udfValue = udfMap.get("value");//serial
            //Check Multivalue, Array of names
            if (udfValue instanceof String[]) {
                String[] udfValueName = (String[]) udfValue;
                if (udfValueName.length > 0) {
                    for (int i = 0; i < udfValueName.length; i++) {
                        Thing thing = ThingService.getInstance().getBySerialNumber(Arrays.toString(udfValueName), ThingTypeService.getInstance().get(
                                thingTypeField.getDataType().getId()));
                        if (thing == null) {
                            response.setErrorDescription("Native thing type property [" + udfLabel + "]. The value [" + Arrays.toString(udfValueName) + "] does not exist.");
                            break;
                        }
                    }
                }
                //Check single name
            } else if (udfValue != null && udfValue instanceof String) {
                Thing thing = ThingService.getInstance().getBySerialNumber(udfValue.toString().trim(),
                        ThingTypeService.getInstance().get(thingTypeField.getDataTypeThingTypeId()));
                if (thing == null) {
                    response.setErrorDescription("Native thing type property [" + udfLabel + "]. The value [" + udfValue + "] does not exist.");
                } else {
                    if (validateVisibility && validationVisibilityUdf(thing, group).isError()) {
                        response.setErrorDescription(
                                "Native thing type property [" + udfLabel + "]. The value [" + udfValue + "] wrong visibilities."
                                        + validationVisibilityUdf(thing, group).getErrorDescription());
                    }
                }
            }

        } catch (NonUniqueResultException e) {
            response.setErrorDescription("Non Unique Result in Udf:  " + udfLabel);
        } catch (Exception e) {
            response.setErrorDescription("Error validating Udf:  " + udfLabel);
        }

        return response;
    }

    /*************************************
     * This method validates if the user wants to create more than the number permitted
     ************************************/
    public ValidationBean validateMaxNumberOfThings(User currentUser) {
        ValidationBean validationBean = new ValidationBean();
        //User user = (User) SecurityUtils.getSubject().getPrincipal();
        if (LicenseService.enableLicense) {
            LicenseDetail licenseDetail = LicenseService.getInstance().getLicenseDetail(currentUser.getActiveGroup(), true);
            Long maxNumberOfThings = licenseDetail.getMaxThings();
            if (maxNumberOfThings != null && maxNumberOfThings > 0) {
                Long countAll = count(licenseDetail);
                if (countAll >= maxNumberOfThings) {
                    validationBean.setErrorDescription("You have reached the limit (" + maxNumberOfThings + ") to create things.");
                }
            }
        }
        return validationBean;
    }

    /*************************************
     * Method to get the quantity of things created by license
     ************************************/
    public static Long count(LicenseDetail licenseDetail) {
        GroupService groupService = GroupService.getInstance();
        ThingDAO thingDAO = ThingService.getThingDAO();
        Long countAll;
        Group licenseGroup = groupService.get(licenseDetail.getGroupId());
        boolean isRootLicense = groupService.getRootGroup().getId().equals(licenseGroup.getId());
        if (isRootLicense) {
            countAll = thingDAO.countAll(null);
        } else {
            countAll = thingDAO.countAll(QThing.thing.group.parentLevel2.id.eq(licenseGroup.getParentLevel2().getId()));
        }
        return countAll;
    }

    /**
     * Method to check if the standard data type is valid or not
     */
    public ValidationBean isStandardValidDataType(DataType type, Object value) {
        ValidationBean validationBean = new ValidationBean(false);
        if (value != null && !Utilities.isEmptyOrNull(value.toString())) {
            Object valueType = null;
            if (ThingTypeField.Type.isDateOrTimestamp(type.getId())) {
                try {
                    if (value.toString().matches("-?\\d+(\\.\\d+)?")) {
                        valueType = new Date(Long.parseLong(value.toString()));
                    } else {
                        valueType = DateTimeFormatterHelper.parseDateTextAndDetermineFormat(Utilities.removeSpaces(value.toString()));
                    }
                } catch (Exception e) {
                    logger.error("Error in validation standard data type: " + e.getMessage() + value);
                    validationBean.setErrorDescription(e.getMessage());
                }
            } else {
                valueType = getStandardDataType(type, value);
            }
            validationBean.setError(valueType == null);
        }
        return validationBean;
    }

    /**
     * Get Standard Data Type
     */
    public Object getStandardDataType(DataType type, Object value) {
        Object response = null;
        if (value != null) {
            try {
                if (Class.forName(type.getClazz()) == Boolean.class && Utilities.isValidBoolean(value.toString())) {
                    response = Boolean.parseBoolean(value.toString());
                } else if (Class.forName(type.getClazz()) == BigDecimal.class) {
                    value = StringUtils.isEmpty(value.toString()) ? "0" : value;
                    BigDecimal data = new BigDecimal(value.toString());
                    response = data.doubleValue();
                } else if (Class.forName(type.getClazz()) == Long.class) {
                    value = StringUtils.isEmpty(value.toString()) ? "0" : value;
                    response = (new BigDecimal(value.toString())).longValue();
                } else if (Class.forName(type.getClazz()) == Date.class) {
                    if (value.toString().matches("-?\\d+(\\.\\d+)?")) {
                        response = new Date(Long.parseLong(value.toString()));
                    } else {
                        response = DateHelper.getDateAndDetermineFormat(value.toString());
                    }
                } else {
                    response = Utilities.removeSpaces(value.toString());
                }
            } catch (Exception e) {
                response = null;
            }
        }
        return response;
    }

    /*****************************************************
     * Method to check if the JSON String has a valid format
     *******************************************************/
    public boolean isJSONValid(Object value) {
        boolean response = true;
        if (value != null && !value.toString().trim().equals("")) {
            try {
                //JSONObject json = (JSONObject)new JSONParser().parse("{\"name\":\"MyNode\", \"width\":200, \"height\":100}");
                JSONObject json = (JSONObject) new JSONParser().parse(value.toString());
            } catch (Exception e) {
                response = false;
            }
        } else {
            response = false;
        }
        return response;
    }

    /**
     * Set thing properties
     *
     * @param udfs
     * @param useDefaultValues
     * @param thingType
     * @param transactionDate
     * @param group
     * @param serialNumber
     * @param name
     * @param currentUser
     * @param parent
     * @return
     */
    public Thing setThingProperties(Map<String, Object> udfs, Boolean useDefaultValues, ThingType thingType, Date transactionDate,
                                    Group group, String serialNumber, String name, User currentUser, Map parent, Group groupFacilityMap) {
        Thing thing = new Thing();
        thing.setThingType(thingType);
        thing.setGroup(group);
        thing.setName(name);
        thing.setCreatedByUser(currentUser);
        thing.setModifiedTime(transactionDate.getTime());

        //set values
        setUdfValues(udfs,useDefaultValues,thingType,transactionDate);

        // set zones
        AppLocationAdapter locationAdapter = new AppLocationAdapter();
        locationAdapter.processFields(udfs, transactionDate, groupFacilityMap.getId(), thingType.getThingTypeCode(), true);

        // set sequence values
        setSequenceValues(udfs, thingType, transactionDate);

        // set parent
        setThingParent(parent, thing);

        // set serial number
        setThingSerialNumber(udfs, thingType, thing, serialNumber);

        return thing;
    }


    /**
     * set thing sequence values
     *
     * @param udfs
     * @param thingType
     * @param transactionDate
     */
    public void setSequenceValues(Map<String, Object> udfs, ThingType thingType, Date transactionDate) {
        try {
            Map<String, Object> udfsWithSequenceValues = new HashMap<>();
            for (ThingTypeField thingTypeField : thingType.getThingTypeFields()) {
                if (thingTypeField.getDataType().getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value) &&
                        thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_SEQUENCE.value) == 0) {
                    Map<String, Object> value = new HashMap<>();
                    value.put("value", SequenceDAO.getInstance().incrementAndGetSequence(thingTypeField.getId()));
                    value.put("time", transactionDate.getTime());
                    udfsWithSequenceValues.put(thingTypeField.getName(), value);
                }
            }
            if (null != udfsWithSequenceValues && !udfsWithSequenceValues.isEmpty()) {
                udfs.putAll(udfsWithSequenceValues);
            }
        } catch (Exception e) {
            throw new UserException("Error building sequence udf's Map.", e);
        }
    }

    /**
     * set thing serial number
     *
     * @param udfs
     * @param thingType
     * @param thing
     * @param serialNumber
     */
    public void setThingSerialNumber(Map<String, Object> udfs, ThingType thingType, Thing thing, String serialNumber) {
        // verify if serial has to be generated
        if (thingType.getSerialFormula() != null && !thingType.getSerialFormula().isEmpty()) {
            // generate serial based in formula
            Object formula = FormulaUtil.getFormulaValues(udfs, thing, thingType.getSerialFormula());
            if (formula != null) {
                serialNumber = formula.toString();
            }
            if (serialNumber != null && !serialNumber.isEmpty()) {
                //Validate Serial
                ValidationBean validationBean = this.validateSerial(thingType, serialNumber, null);
                if (validationBean.isError()) {
                    throw new UserException(validationBean.getErrorDescription()+" - "+serialNumber);
                }else{
                    thing.setSerial(serialNumber.toString());
                }
            } else {
                throw new UserException
                        ("Serial number cannot be evaluated");
            }
        } else {
            if (serialNumber != null && !serialNumber.isEmpty()) {
                thing.setSerial(serialNumber.toString());
            }
        }
    }

    /**
     * set thing parent
     *
     * @param parent
     * @param thing
     */
    public void setThingParent(Map parent, Thing thing) {
        if (parent != null && !parent.isEmpty()) {
            if (parent.get("thing") != null && parent.get("thing") instanceof Thing) {
                thing.setParent((Thing) parent.get("thing"));
            }
        }
    }

    public void setThingChildren(Map children, Thing thing) {
    }

    /**
     * Method to insert the thing in MySQL.
     * Check constraint apc_thing_uniqueconstraint1 when create is called in
     * different threads
     *
     * @param thing          Thing object
     * @param createAndFlush boolean parameter for flushing or not
     */
    public void insertThingMySQL(Thing thing, boolean createAndFlush) {
        try {
            Long id = null;
            if (createAndFlush) {
                id = getThingDAO().insert(thing);
            } else {
                id = getThingDAO().insertWithoutFlush(thing);
            }
            thing.setId(id);
        } catch (Exception e) {
            logger.error("Error occurred creating thing '" + thing.getSerialNumber() + "' in mysql", e);
            throw new UserException(String.format("Serial '%s' already exist for Thing Type '%s' in mySQL", thing.getSerialNumber(), thing.getThingType().getName()), e);
        }
    }

    /**
     * This methods gets the properties to save in Mongo DB
     * @param thing
     * @param udfs
     * @param transactionDate
     */
    /**
     * @param thing
     * @param thingType
     * @param udfs
     * @param transactionDate
     * @return returns a Map with keys: thingTypeFieldTickle, isTimeSeries, thingTypeFieldResponse
     */
    public Map<String, Object> setThingMongoProperties(
            Thing thing
            , ThingType thingType
            , Group group
            , Map<String, Object> udfs
            , Date transactionDate
            , boolean fillSource) {
        Map<String, Object> udfMapMongo = this.getUdfMapForMongo(
                thing.getId()
                , thingType
                , udfs
                , transactionDate
                , null
                , group
                , fillSource);
        Map<String, Object> thingTypeFieldResponse = null;
        if (udfMapMongo != null && udfMapMongo.size() > 0) {
            if (udfMapMongo.get("thingTypeFieldResponse") != null) {
                thingTypeFieldResponse = (Map<String, Object>) udfMapMongo.get("thingTypeFieldResponse");
                thingTypeFieldResponse = FormulaUtil.getFormulaValues(thingTypeFieldResponse, thing, thingType);
                udfMapMongo.put("thingTypeFieldResponse", thingTypeFieldResponse);
            }
        }
        return udfMapMongo;
    }

    /**
     * This method builds the map to send Mongo so as to.
     *
     * @param thingId   Thing Id to create mongo Map.
     * @param thingType Thing Type.
     * @param udfs      Thing Type Fields of a Thing.
     * @param storeDate Store Date.
     * @param cache     cache to generate Mongo Thing.
     * @param group     Group of mongo thing.
     * @return Map Mongo of a thing or exception to create Mongo fields.
     */
    public Map<String, Object> getUdfMapForMongo(
            Long thingId
            , ThingType thingType
            , Object udfs
            , Date storeDate
            , Map<String, Map<String, Object>> cache
            , Group group
            , boolean fillSource) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> thingTypeFieldResponse = new HashMap<>();
        Map<String, Object> thingTypeFieldTickle = new HashMap<>();
        boolean timeSeries = false;
        try {
            if (udfs != null) {
                Map<String, Object> mapUdfs = (Map<String, Object>) udfs;
                List<ThingTypeField> thingTypeFields;
                if (fillSource) {
                    thingTypeFields = ThingTypeFieldService.getInstance().getThingTypeFieldByNameAndTypeCode(Constants.UDF_SOURCE, thingType.getThingTypeCode());
                    if (thingTypeFields != null && !thingTypeFields.isEmpty()
                            && (mapUdfs.get(Constants.UDF_SOURCE) == null || ((Map) mapUdfs.get(Constants.UDF_SOURCE)).isEmpty())) {
                        LinkedHashMap<Object, Object> valueSourceDefault = new LinkedHashMap<>();
                        valueSourceDefault.put("value", Constants.SOURCE_SERVICE);
                        mapUdfs.put(Constants.UDF_SOURCE, valueSourceDefault);
                    }
                }
                //Iterate Udf's data
                for (Map.Entry<String, Object> udfObject : mapUdfs.entrySet()) {
                    Map<String, Object> udfField = new HashMap<>();
                    String udfLabel = udfObject.getKey().trim();

                    //Iterate fields of the config of the thing type
                    thingTypeFields = new ArrayList<>();
                    if (cache != null) {
                        for (Object thingTypeField : cache.get("thingTypeField").values()) {
                            if (((ThingTypeField) thingTypeField).getName().equals(udfLabel) &&
                                    ((ThingTypeField) thingTypeField).getThingType().getThingTypeCode().equals(
                                            thingType.getThingTypeCode()))
                                thingTypeFields.add((ThingTypeField) thingTypeField);
                        }
                    } else {
                        thingTypeFields = ThingTypeFieldService.getInstance().getThingTypeFieldByNameAndTypeCode(
                                udfLabel,
                                thingType.getThingTypeCode());
                    }

                    if (thingTypeFields.size() > 0) {
                        ThingTypeField thingTypeField = thingTypeFields.get(0);
                        timeSeries = timeSeries || thingTypeField.getTimeSeries();
                        DataType dataType = null;
                        if (thingTypeField.getDataType().getTypeParent()
                                .equals(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value)) {
                            if (cache != null) {
                                dataType = (DataType) cache.get("dataType")
                                        .get(thingTypeField.getDataType().getId().toString());
                            } else {
                                dataType = DataTypeService.getInstance().get(thingTypeField.getDataType().getId());
                            }
                        }

                        for (Map.Entry<String, Object> udfProperties : ((Map<String, Object>) udfObject.getValue())
                                .entrySet()) {
                            if (!udfProperties.getKey().equals("value")) {
                                udfField.put(udfProperties.getKey(), udfProperties.getValue());
                            }
                        }

                        if (!udfField.containsKey("thingTypeFieldId")) {
                            udfField.put("thingTypeFieldId", thingTypeField.getId());
                        }
                        if (!udfField.containsKey("time")) {
                            udfField.put("time", storeDate);
                        }

                        Map<String, Object> udfMap = (Map<String, Object>) udfObject.getValue();
                        Object udfValue = udfMap.get("value");

                        HashMap<String, Object> values = getValueMapForMongo(
                                thingId,
                                thingTypeField,
                                dataType,
                                udfValue,
                                cache,
                                group,
                                storeDate,
                                fillSource);
                        udfField.put("value", values.get("responseMap"));
                        thingTypeFieldTickle.put(thingTypeField.getName(), values.get("responseObject"));
                        thingTypeFieldResponse.put(udfLabel, udfField);
                    }
                }
                response.put("thingTypeFieldResponse", thingTypeFieldResponse);
                response.put("thingTypeFieldTickle", thingTypeFieldTickle);
                response.put("isTimeSeries", timeSeries);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new UserException("Error when build the udf's Map for Mongo." + e.getMessage(), e);
        }
        return response;
    }

    /*********************************
     * get value Map for mongo
     *********************************/
    public HashMap<String, Object> getValueMapForMongo(
            Long thingId,
            ThingTypeField thingTypeField,
            DataType dataType,
            Object udfValue,
            Map<String, Map<String, Object>> cache,
            Group group,
            Date storeDate,
            boolean fillSource) {

        HashMap<String, Object> response = new HashMap<String, Object>();
        Object responseMap = new Object();
        String responseObject = null;

        String kafkaEnabledValue = Configuration.getProperty("kafka.enabled");
        boolean kafkaEnabled = kafkaEnabledValue != null ? Boolean.parseBoolean(kafkaEnabledValue) : false;

        if (udfValue != null) {
                if (thingTypeField.getDataType().getTypeParent().equals(ThingTypeField.TypeParent
                        .TYPE_PARENT_DATA_TYPE.value)
                        && dataType != null
                        && dataType.getType().equals(ThingTypeField.TypeParentSubGroup
                        .TYPE_PARENT_DATA_TYPE_STANDARD_DATA.value)) {
                    udfValue = this.getStandardDataType(thingTypeField.getDataType(), udfValue);
                    udfValue = this.getUdfValueAttachments(thingId, thingTypeField, group, udfValue);
                    if (fillSource && (udfValue == null || udfValue.toString().trim().isEmpty())
                            && Constants.UDF_SOURCE.compareTo(thingTypeField.getName()) == 0) {
                        udfValue = Constants.SOURCE_SERVICE;
                    }
                    responseMap = udfValue;
                    if (udfValue != null) {
                        // VIZIX-2567: we need to change the date format to yyyy-MM-dd'T'HH:mm:ss.SSSXXX.
                        if (udfValue instanceof Date) {
                            try {
                                Date date = (Date) udfValue;
                                responseObject = FormatUtil.format(date, DATE_FORMAT);
                            } catch (ParseException e) {
                                logger.error(String.format("Mistakes to format the date: %s", udfValue), e);
                                responseObject = null;
                            }
                        } else {
                            responseObject = udfValue.toString();
                        }
                    }
                } else if (thingTypeField.getDataType().getTypeParent().equals(ThingTypeField.TypeParent
                        .TYPE_PARENT_DATA_TYPE.value)
                        && dataType != null
                        && dataType.getType().equals(ThingTypeField.TypeParentSubGroup
                        .TYPE_PARENT_DATA_TYPE_NATIVE_OBJECT.value)) {
                    Object nativeObject = ThingTypeFieldService.getInstance()
                            .getNativeObject(thingTypeField.getDataType().getId(),
                                    udfValue.toString(),
                                    cache,
                                    group, storeDate);
                    Map<String, Object> nativeObjectMap = null;
                    if (nativeObject instanceof LogicalReader) {
                        nativeObjectMap = ((LogicalReader) nativeObject).publicMapExtended();
                        if(kafkaEnabled){
                            responseObject = ((LogicalReader) nativeObject).getCode();
                        }else {
                            responseObject = ((LogicalReader) nativeObject).getId().toString();
                        }
                    } else if (nativeObject instanceof Shift) {
                        nativeObjectMap = ((Shift) nativeObject).publicMap();
                        if(kafkaEnabled){
                            responseObject = ((Shift) nativeObject).getCode();
                        }else {
                            responseObject = ((Shift) nativeObject).getId().toString();
                        }
                    } else if (nativeObject instanceof Group) {
                        nativeObjectMap = ((Group) nativeObject).publicMap();
                        if(kafkaEnabled){
                            responseObject = ((Group) nativeObject).getCode();
                        }else {
                            responseObject = ((Group) nativeObject).getId().toString();
                        }
                    } else if (nativeObject instanceof Zone) {
                        nativeObjectMap = ((Zone) nativeObject).publicMapSummarized();
                        nativeObjectMap.put("zoneTypeTime", storeDate);
                        nativeObjectMap.put("facilityMapTime", storeDate);
                        nativeObjectMap.put("zoneGroupTime", storeDate);
                        if(kafkaEnabled){
                            responseObject = ((Zone) nativeObject).getCode();
                        }else {
                            responseObject = ((Zone) nativeObject).getId().toString();
                        }
                    } else if (nativeObject instanceof Map) {
                        nativeObjectMap = (Map) nativeObject;
                    }
                    responseMap = nativeObjectMap;
                } else if (thingTypeField.getDataType().getTypeParent()
                        .equals(ThingTypeField.TypeParent.TYPE_PARENT_NATIVE_THING_TYPE.value)) {
                    Thing thing = null;
                    try {
                        if (cache != null) {
                            for (Object thingObj : cache.get("thing").values()) {
                                if (((Thing) thingObj).getSerial().equals(udfValue.toString()) &&
                                        ((Thing) thingObj).getThingType().getThingTypeCode().
                                                equals(thingTypeField.getThingType().getThingTypeCode())) {
                                    thing = (Thing) thingObj;
                                }
                            }
                        } else {
                            thing = ThingService
                                    .getInstance().getBySerialNumber(udfValue.toString(), ThingTypeService.getInstance().get(
                                            thingTypeField.getDataTypeThingTypeId()));
                        }
                    } catch (Exception e) {
                        throw new UserException("Value Map for Mongo: " + udfValue.toString(), e);
                    }
                    //Get Thing of the ThingType UDf
                    if(thing != null) {
                        responseMap = ThingMongoDAO.getInstance().getThing(thing.getId());
                       if(kafkaEnabled){
                           responseObject = thing.getThingType().getCode()+"-"+thing.getSerialNumber();
                       }else {
                        responseObject = thing.getId().toString();
                       }
                    }
                }
        } else {
            responseMap = null;
            responseObject = null;
        }
        response.put("responseMap", responseMap != null ? responseMap : null);
        response.put("responseObject", responseObject);//for tickles
        return response;
    }

    /*********************************************************************************
     * This method creates a file based on the AttachmentID and
     * it adds  the value of the udf with the downloadURL value for new files created
     *********************************************************************************/
    public Object getUdfValueAttachments(Long thingId, ThingTypeField thingTypeField, Group group, Object udfValueObject) {
        Object response = udfValueObject;
        String path = "fileSystemPath";
        if (thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_ATTACHMENTS.value) == 0) {
            String udfValue = udfValueObject.toString();
            try {
                JSONObject json = (JSONObject) new JSONParser().parse(udfValue);
                List<JSONObject> lstFilesToDelete = new ArrayList<>();
                //Iterate the attachments
                JSONObject configuration = (JSONObject) json.get("configuration");
                String pathAttachments = (String) configuration.get(path);
                JSONArray attachments = (JSONArray) json.get("attachments");
                if (attachments != null) {
                    for (int i = 0; i < attachments.size(); i++) {
                        JSONObject attachment = (JSONObject) attachments.get(i);
                        //Create the file physically in the server
                        if (attachment.get("operation") != null && attachment.get("operation").toString().equals("add")
                                && attachment.get("id") != null) {
                            logger.info("Writting file '" + attachment.get("name") + "'");
                            String downloadURL = AttachmentService.getInstance().createFile(
                                    Long.parseLong(attachment.get("id").toString()),
                                    pathAttachments.toString(),
                                    thingId,
                                    thingTypeField.getId(),
                                    group.getHierarchyName(false));
                            logger.info("File '" + attachment.get("name") + "' created");
                            attachment.put(path, downloadURL);
                            attachment.remove("id");
                            //Delete the file physically in the server
                        } else if (attachment.get("operation") != null && attachment.get("operation").toString().equals("remove")
                                && attachment.get(path) != null) {
                            logger.info("Removing file '" + attachment.get("name") + "'");
                            AttachmentService.getInstance().removeFile(attachment.get(path).toString());
                            logger.info("File '" + attachment.get("name") + "' removed");
                            lstFilesToDelete.add(attachment);
                        }
                        //Clone the file physically in the server
                        else if (attachment.get("operation") != null && attachment.get("operation").toString().equals("clone")
                                && attachment.get(path) != null) {
                            logger.info("Cloning file '" + attachment.get("name") + "'");
                            String downloadURL = AttachmentService.getInstance().cloneFile(
                                    attachment.get(path).toString(),
                                    pathAttachments.toString(),
                                    thingId,
                                    thingTypeField.getId(),
                                    group.getHierarchyName(false));
                            logger.info("File '" + attachment.get("name") + "' cloned");
                            attachment.put(path, downloadURL);
                            attachment.remove("id");
                        }
                    }
                }

                //Delete the info in JSON  of the attachments deleted
                for (JSONObject i : lstFilesToDelete) {
                    attachments.remove(i);
                }
                //Delete attachments records who are old than one day
                AttachmentService.getInstance().deleteOldAttachments();

                response = json.toString();
            } catch (Exception e) {
                e.printStackTrace();
                throw new UserException("Error creating attachments. " + e.getMessage(), e);
            }
        }
        return response;
    }

    /**
     * This method gets the udfs of type Formula which were changed in this iteration
     *
     * @param thing
     * @param transactionDate
     * @throws Exception
     */
    private Map<String, Object>  getUDFFormulaValuesForParent( Thing thing, Date transactionDate) {
        Map<String, Object> formulaValues = new HashMap<>();
        if (thing.getThingType().getThingTypeFields() == null){
            thing.getThingType().setThingTypeFields(new HashSet<ThingTypeField>());
            List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().getByThingTypeCode(thing.getThingType().getThingTypeCode());
            for (ThingTypeField thingTypeField: thingTypeFields) {
                thing.getThingType().getThingTypeFields().add(thingTypeField);
            }
        }
        List<ThingTypeField> thingTypeFields = thing.getThingTypeFieldByType(ThingTypeField.Type.TYPE_FORMULA.value);
        if (null != thingTypeFields && !thingTypeFields.isEmpty()) {
            for (ThingTypeField thingTypeField : thingTypeFields) {
                String formula = thingTypeField.getDefaultValue();
                // replace value for formula UDFs
                String replacedFormula = StringUtils.replace(formula, "${count(", "${countParentChild(id,");
//                replacedFormula = StringUtils.replace(replacedFormula, ")", ",tenantCode)");
                Object value = FormulaUtil.getFormulaValues(null, thing, replacedFormula);
                if (null != value) {
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("thingTypeFieldId", thingTypeField.getId());
                    properties.put("time", transactionDate);
                    properties.put("value", value);
                    formulaValues.put(thingTypeField.getName(), properties);
                }
            }
        }
        return formulaValues;
    }

    /**
     * This method gets the udfs of type Formula which were changed in this iteration
     *
     * @param thingContainerUdf  thing which contains Parent UDF . Example : Asset
     * @param udfs  map of the UDFs of the thing which contains the Parent UDF. Example: Asset's UDFs
     * @param transactionDate
     * @throws Exception
     */
    public Map<String, Object>  getUDFFormulaValuesForParentUdf( Thing thingContainerUdf, Map udfs, Date transactionDate) {
        Map<String, Object> formulaValues = new HashMap<>();
        try {
            List<ThingTypeField> thingTypeFieldsTT = thingContainerUdf.getThingTypeFieldByType(ThingTypeField.Type.TYPE_THING_TYPE.value);
            if (null != thingTypeFieldsTT && !thingTypeFieldsTT.isEmpty()) {
                Map<String, Object> thingsWithFormula = ThingService.getInstance().getThingsToEvaluateFormula(udfs, thingTypeFieldsTT);
                if (null != thingsWithFormula && !thingsWithFormula.isEmpty()) {
                    for (Map.Entry<String, Object> entry : thingsWithFormula.entrySet()) {
                        String udfName = entry.getKey();
                        Thing thingUdf = (Thing) entry.getValue();
                        if (null != thingUdf){
                            List<ThingTypeField> thingTypeFields = thingUdf.getThingTypeFieldByType(ThingTypeField.Type.TYPE_FORMULA.value);
                            if (null != thingTypeFields && !thingTypeFields.isEmpty()) {
                                Map<String,Object> udfCondition = new HashMap<>();
                                udfCondition.put("udfCondition", udfName + ".value._id=");
                                formulaValues = new HashMap<>();
                                for (ThingTypeField thingTypeField : thingTypeFields) {
                                    String formula = thingTypeField.getDefaultValue();
                                    // replace value for formula UDFs
                                    String replacedFormula = StringUtils.replace(formula, "${count(","${countThingTypeUDF(id,udfCondition,");
                                    Object value = FormulaUtil.getFormulaValues(udfCondition,thingUdf,replacedFormula);
                                    if (null != value) {
                                        Map<String, Object> properties = new HashMap<>();
                                        properties.put("thingTypeFieldId", thingTypeField.getId());
                                        properties.put("time", transactionDate);
                                        properties.put("value", value);
                                        formulaValues.put(thingTypeField.getName(), properties);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e){
            logger.error("Error occurred while evaluating expression for parent UDF '" + thingContainerUdf.getSerialNumber() , e);
            throw new UserException("Error occurred while evaluating expression for parent UDF '" + thingContainerUdf.getSerialNumber() , e);
        }
        return formulaValues;
    }

    public List<Thing> createChildren(
            Thing thing,
            String groupHierarchyCode,
            Object children,
            boolean executeTickle,
            boolean validateVisibility,
            Date transactionDate,
            Boolean useDefaultValues,
            Stack<Long> recursivelyStack,
            User currentUser,
            boolean fillSource) {
        List<Thing> newChildrenIds = new ArrayList<>();
        try{
            if (children != null && !((List) children).isEmpty()) {
                for (Object childThingObject : (List) children) {
                    Map<String, Object> childThingMap = (Map<String, Object>) childThingObject;
                    ThingType thingTypeChild = childThingMap.containsKey("thingType") ? ((ThingType)childThingMap.get("thingType")) : null;
                    if (thingTypeChild == null){
                        String thingTypeCode = childThingMap.containsKey("thingTypeCode") ? (String)childThingMap.get("thingTypeCode") : null;
                        thingTypeChild = (ThingType) validateAndGetThingType(thingTypeCode).get("thingType");
                    }
                    Thing childThing;
                    if (childThingMap.get("thing") != null){
                        childThing = (Thing) childThingMap.get("thing");
                    } else {
                        childThing = ThingService.getInstance().getBySerialNumber((String) childThingMap.get( "serialNumber" ), thingTypeChild );
                    }
                    if (childThing == null) {
                        Map<String, Object> resultCreateChildren = this.create(
                                recursivelyStack,
                                thingTypeChild.getThingTypeCode(),
                                groupHierarchyCode,
                                (String) childThingMap.get("serialNumber"),
                                (String) childThingMap.get("serialNumber"),
                                null,
                                (Map<String, Object>) childThingMap.get("udfs"),
                                null,
                                null,
                                executeTickle,
                                validateVisibility,
                                transactionDate,
                                useDefaultValues,
                                fillSource);
                        childThing = getThingDAO().selectById((Long.parseLong(((Map) resultCreateChildren.get("thing")).get("id").toString())));
                    }
                    newChildrenIds.add(childThing);
                    /*Disassociate old parent*/
                    if (childThing.getParent() != null) {
                        ThingMongoService.getInstance().disAssociateChild(childThing.getParent(), childThing.getId());
                    }
                    childThing.setParent(thing);
                }
                //Associate children to new thing
                ThingMongoService.getInstance().associateChildren(thing, newChildrenIds);
                // update formula values for thing
                updateFormulaValuesForParent(recursivelyStack, thing, transactionDate, executeTickle, currentUser, fillSource);
            }
        } catch (Exception e){
            logger.error("Error occurred while trying to create children", e);
            throw new UserException("Error occurred while trying to create children", e);
        }
        return newChildrenIds;
    }

    public void createOrUpdateChildrenUDF(String groupHierarchyCode,
                                  Object childrenUdf,
                                  boolean executeTickle,
                                  boolean validateVisibility,
                                  Date transactionDate,
                                  Boolean useDefaultValues,
                                  Stack<Long> recursivelyStack,
                                  User currentUser,
                                  boolean fillSource) {
        try{
            if (childrenUdf != null && ((List) childrenUdf).size() > 0) {
                for (Object childThingObject : (List) childrenUdf) {
                    Map<String, Object> childThingMap = (Map<String, Object>) childThingObject;
                    ThingType thingTypeChild = childThingMap.containsKey("thingType") ? ((ThingType)childThingMap.get("thingType")) : null;
                    if (thingTypeChild == null){
                        String thingTypeCode = childThingMap.containsKey("thingTypeCode") ? (String)childThingMap.get("thingTypeCode") : null;
                        thingTypeChild = (ThingType) validateAndGetThingType(thingTypeCode).get("thingType");
                    }
                    Thing childThing;
                    if (childThingMap.get("thing") != null){
                        childThing = (Thing) childThingMap.get("thing");
                    } else {
                        childThing = ThingService.getInstance().getBySerialNumber((String) childThingMap.get( "serialNumber" ), thingTypeChild );
                    }
                    Map<String, Object> resultChildrenUdf = null;
                    if (childThing == null) {
                            resultChildrenUdf = this.create(
                                    recursivelyStack,
                                    (String) childThingMap.get("thingTypeCode"),
                                    groupHierarchyCode,
                                    (String) childThingMap.get("serialNumber"),
                                    (String) childThingMap.get("serialNumber"),
                                    null,
                                    (Map<String, Object>) childThingMap.get("udfs"),
                                    null,
                                    null,
                                    executeTickle,
                                    validateVisibility,
                                    transactionDate,
                                    useDefaultValues,
                                    fillSource);

                        } else {
                            resultChildrenUdf = ThingService.getInstance().update(
                                    recursivelyStack,
                                    childThing.getId(),
                                    childThing.getThingType().getThingTypeCode(),
                                    childThing.getGroup().getHierarchyName(false),
                                    childThing.getName(),
                                    childThing.getSerial(),
                                    null,
                                    (Map<String, Object>) childThingMap.get("udfs"),
                                    null,
                                    null,
                                    executeTickle,
                                    true,
                                    transactionDate,
                                    true,
                                    currentUser,
                                    fillSource);
                        }
                    }
                }

        } catch (Exception e){
            logger.error("Error occurred while trying to create children UDF", e);
            throw new UserException("Error occurred while trying to create children UDF", e);
        }
    }

    /**
     * This method associates a thing (child) A whit a thing B (parent)
     * @param parent Thing object of the parent
     * @param oldParent Thing object of the old parent
     * @param thingChild ID of the child thing
     * @param transactionDate Date of the transaction
     * @param recursivelyStack Boolean value in order to avoid resursive update
     * @param executeTickle Boolean value in order to send or not tickles to Bridges
     */
    public void associateParent(Thing parent,
                                Thing oldParent,
                                Thing thingChild,
                                Map<String, Object> childThingUdfsMongoMap,
                                Date  transactionDate,
                                Stack recursivelyStack,
                                boolean executeTickle,
                                User currentUser,
                                boolean fillSource){
        Map<String, Object> childUdfsFormulaValues = null;
        if (parent != null) {
            ThingMongoService.getInstance().associateParentChild(parent, oldParent, thingChild.getId());
            Map<String, Object> formulaValues = getUDFFormulaValuesForParent(parent,transactionDate);
            formulaValues = getMapUdfServices(thingChild.getThingType(), formulaValues);
            if( ( formulaValues!= null ) && ( !formulaValues.isEmpty() ) ){
                // update udf value for the parent
                ThingService.getInstance().update(
                        recursivelyStack,
                        parent, // thing
                        parent.getThingType().getThingTypeCode(), // thingTypeCode
                        parent.getGroup().getHierarchyName(false), // groupHierarchyCode
                        parent.getName(), // name
                        parent.getSerial(), // serialNumber
                        null, // parent
                        formulaValues, // udfs
                        null, // children
                        null, // childrenUdf
                        executeTickle, // executeTickle
                        false, // validateVisibility
                        transactionDate, // transactionDate
                        true, // disableFMCLogic
                        null, // validations
                        null, // cache
                        true, // updateAndFlush
                        true, // recursivilyUpdate
                        currentUser,
                        fillSource
                );
            }else{
                ThingMongoService.getInstance().updateThingInReferences(parent);
                if(executeTickle){
                    ThingService.getInstance().executeTickle(
                            parent.getThingType().getThingTypeCode(),
                            parent.getSerial(),
                            null,
                            transactionDate,
                            false,
                            GroupService.getInstance().getMqttGroups(parent.getGroup()));
                }
            }

            //As we have a new relation of parent, formula for "parent expressions" in child has to be evaluated
            //and the child has to be updated with the new Udfs
            if ( (childThingUdfsMongoMap != null) && (childThingUdfsMongoMap.size() > 0) &&
                    (childThingUdfsMongoMap.get("thingTypeFieldResponse") != null ) ) {
                List<ThingTypeField> thingTypeFieldsFormula = thingChild.getThingType().getThingTypeFieldsByType(
                        Long.valueOf(ThingTypeField.Type.TYPE_FORMULA.value));
                if (thingTypeFieldsFormula != null && !thingTypeFieldsFormula.isEmpty()) {
                    boolean containsExprParent = false;
                    for (ThingTypeField thingTypeField : thingTypeFieldsFormula) {
                        if(thingTypeField.getDefaultValue().contains("parent.")){
                            for(String data :((Map<String, Object>)childThingUdfsMongoMap.get("thingTypeFieldResponse")).keySet()){
                                if(data.equals(thingTypeField.getName())){
                                    ((Map<String, Object>)((Map<String, Object>)childThingUdfsMongoMap.get("thingTypeFieldResponse")).get(data))
                                            .put("value", thingTypeField.getDefaultValue());
                                }
                            }
                            containsExprParent = true;
                        }
                    }
                    if(containsExprParent){
                        childUdfsFormulaValues = (Map<String, Object>) childThingUdfsMongoMap.get("thingTypeFieldResponse");
                        childUdfsFormulaValues = FormulaUtil.getFormulaValues(childUdfsFormulaValues, thingChild, thingChild.getThingType());
                        childUdfsFormulaValues = getMapUdfServices(thingChild.getThingType(), childUdfsFormulaValues);

                        // update udf value for the child
                        ThingService.getInstance().update(
                                recursivelyStack,
                                thingChild, // thing
                                thingChild.getThingType().getThingTypeCode(), // thingTypeCode
                                thingChild.getGroup().getHierarchyName(false), // groupHierarchyCode
                                thingChild.getName(), // name
                                thingChild.getSerial(), // serialNumber
                                null, // parent
                                childUdfsFormulaValues, // udfs
                                null, // children
                                null, // childrenUdf
                                executeTickle, // executeTickle
                                false, // validateVisibility
                                transactionDate, // transactionDate
                                true, // disableFMCLogic
                                null, // validations
                                null, // cache
                                true, // updateAndFlush
                                true, // recursivilyUpdate
                                currentUser,
                                fillSource
                        );
                    }
                }//end fields formula
            }//end childThingUdfsMongoMap
        }//end parent
    }

    private static void updateFormulaValuesForParent(Stack<Long> recursivelyStack, Thing thing,Date transactionDate,
                                                     boolean executeTickle, User currentUser, boolean fillSource){
        List<ThingTypeField> thingTypeFields = thing.getThingTypeFieldByType(ThingTypeField.Type.TYPE_FORMULA.value);
        if (null != thingTypeFields && !thingTypeFields.isEmpty()) {
            Map<String,Object> formulaValues = new HashMap<>();
            for (ThingTypeField thingTypeField : thingTypeFields) {
                String formula = thingTypeField.getDefaultValue();
                // replace value for formula UDFs
                String replacedFormula = StringUtils.replace(formula, "${count(","${countParentChild(id,");
//                replacedFormula = StringUtils.replace(replacedFormula, ")", ",tenantCode)");
                Object value = FormulaUtil.getFormulaValues(null,thing,replacedFormula);
                if (null != value) {
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("thingTypeFieldId", thingTypeField.getId());
                    properties.put("time", transactionDate);
                    properties.put("value", value);
                    formulaValues.put(thingTypeField.getName(), properties);
                }
            }
            // update udf value for the parent
            ThingService.getInstance().update(
                    recursivelyStack
                    , thing // thingId
                    , thing.getThingType().getThingTypeCode() // thingTypeCode
                    , thing.getGroup().getHierarchyName(false) // groupHierarchyCode
                    , thing.getName() // name
                    , thing.getSerial() // serialNumber
                    , null // parent
                    , formulaValues // udfs
                    , null // children
                    , null // childrenUdf
                    , executeTickle // executeTickle
                    , false // validateVisibility
                    , transactionDate // transactionDate
                    , true // disableFMCLogic
                    , null // validations
                    , null // cache
                    , true // updateAndFlush
                    , true // recursivilyUpdate
                    , currentUser
                    , fillSource
            );
        }
    }

    public void updateExpressionsForParentUDF(
            Thing thing,Map udfs,Stack<Long> recursivelyStack,boolean executeTickle,Date transactionDate
            , User currentUser, boolean fillSource){
        try {
            List<ThingTypeField> thingTypeFieldsTT = thing.getThingTypeFieldByType(ThingTypeField.Type.TYPE_THING_TYPE.value);
            if (null != thingTypeFieldsTT && !thingTypeFieldsTT.isEmpty()) {
                Map<String, Object> thingsWithFormula = ThingService.getInstance().getThingsToEvaluateFormula(udfs, thingTypeFieldsTT);
                ThingService.getInstance().updateFormulaValuesForThingTypeUdfs(
                        recursivelyStack, thingsWithFormula, transactionDate, executeTickle, currentUser, fillSource);
            }
        } catch (Exception e){
            logger.error("Error occurred while updating thingTypeUdf parent formula for thing '" + thing.getSerialNumber() + "' in mongo", e);
            throw new UserException("Error occurred while updating thingTypeUdf parent formula for thing '" + thing.getSerialNumber() + "' in mongo", e);
        }
    }

    /**
     * Gets Map of thing type fields with the values to send by Mqtt to Bridges (tickle)
     * @param thingMongoMap
     * @param executeTickle
     * @param thing
     * @param transactionDate
     * @return
     */
    public static Map<String, Object> getMapFieldsTickle(Map<String, Object> thingMongoMap,boolean executeTickle,Thing thing,Date transactionDate){
        if (thingMongoMap == null || thingMongoMap.isEmpty()){
            return null;
        }
        Map<String, Object> thingTypeFieldTickle = null;
        Map<String, Object> thingTypeFieldResponse = null;
        if(thingMongoMap.containsKey("thingTypeFieldTickle") && thingMongoMap.get("thingTypeFieldTickle") instanceof Map){
            thingTypeFieldTickle = (Map) thingMongoMap.get("thingTypeFieldTickle");
        }
        if(thingMongoMap.containsKey("thingTypeFieldResponse") && thingMongoMap.get("thingTypeFieldResponse") instanceof Map){
            thingTypeFieldResponse = (Map) thingMongoMap.get("thingTypeFieldResponse");
        }
        if (thingTypeFieldTickle == null){
            return null;
        }

        Iterator<Map.Entry<String, Object>> it = thingTypeFieldResponse.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, Object> current = it.next();
            String tfkey = current.getKey();
            if ( (thingTypeFieldTickle.containsKey(tfkey)) && (current.getValue() instanceof Map) &&
                    (((Map)current.getValue()).get("thingTypeFieldId")!=null) &&
                    (thing.getThingType().isDataType((Long)((Map)current.getValue()).get("thingTypeFieldId")
                            , ThingTypeField.Type.TYPE_FORMULA.value) )) {
                HashMap<String, Object> tfvalue = (HashMap<String, Object> ) current.getValue();
                if (null != tfvalue && tfvalue.containsKey("value")) {
                    thingTypeFieldTickle.put(tfkey,tfvalue.get("value"));
                }
            }
        }

        if(executeTickle) {
            ThingService.getInstance().executeTickle(thing.getThingType().getThingTypeCode(), thing.getSerial(),
                    thingTypeFieldTickle, transactionDate, false, GroupService.getInstance().getMqttGroups(thing.getGroup()));
        }

        return thingTypeFieldTickle;
    }

    /**
     * Set Ufd values from enter values or default values (thingtypefield table)
     * @param udfs
     * @param useDefaultValues
     * @param thingType
     * @param transactionDate
     */
    public void setUdfValues(Map<String, Object> udfs, Boolean useDefaultValues, ThingType thingType, Date transactionDate) {
        Map<String, Object> udfValues = new HashMap<>();
        useDefaultValues = useDefaultValues == null ? true: useDefaultValues;
        if (thingType.getThingTypeFields() == null){
            thingType.setThingTypeFields(new HashSet<ThingTypeField>());

            List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().getByThingTypeCode(thingType.getThingTypeCode());
            for (ThingTypeField thingTypeField: thingTypeFields) {
                thingType.getThingTypeFields().add(thingTypeField);
            }
        }
        for (ThingTypeField thingTypeField : thingType.getThingTypeFields()) {
            if (udfs != null && udfs.size()> 0 && udfs.get(thingTypeField.getName())!= null){
                udfValues.put(thingTypeField.getName(), udfs.get(thingTypeField.getName()));
            } else if (useDefaultValues){
                Map<String, Object> udfValue = new HashMap<>();
                udfValue = getDefaultValues(thingTypeField, transactionDate);
                if (udfValue != null && udfValue.size() > 0){
                    udfValues.putAll(udfValue);
                }
            }
        }
        if( udfs != null){
            udfs.putAll(udfValues);
        }

    }

    /**
     * Get udfs with Default Values from thingtypefield table
     * @param thingTypeField
     * @param transactionDate
     * @return a udfs Map with Default Values
     */
    public  Map<String, Object> getDefaultValues(ThingTypeField thingTypeField, Date transactionDate) {
        try {
            Map<String, Object> udfsWithDefaultValues = new HashMap<>();
            if (thingTypeField.getDefaultValue() != null && !thingTypeField.getDefaultValue().trim().equals("")){
                Map<String, Object> value = new HashMap<>();
                value.put("value", thingTypeField.getDefaultValue());
                value.put("time", transactionDate.getTime());
                udfsWithDefaultValues.put(thingTypeField.getName(), value);
            }
            return udfsWithDefaultValues;
        } catch (Exception e) {
            logger.error("Error building default values udf's Map." + e.getMessage(), e);
            throw new UserException("Error building default values udf's Map.", e);
        }
    }

    /**
     * Get a Map with the format for createThing method: "udf{property":{"value":"Hello"}}
     * @param thingType
     * @param udf
     * @return
     */
    public Map<String, Object> getMapUdfServices(ThingType thingType, Map<String, Object> udf){
        Map<String, Object> resultMap = udf;
        if( (udf != null) && (!udf.isEmpty())){
            for (Map.Entry<String, Object> entry : udf.entrySet())
            {
                Long data = (Long)((Map)entry.getValue()).get("thingTypeFieldId");
                for(ThingTypeField thingTypeField : thingType.getThingTypeFields()){
                    if(data.compareTo(thingTypeField.getId())==0){
                        if( (((Map)entry.getValue()).get("value") != null) &&
                            (((Map) entry.getValue()).get("value") instanceof Map) ) {

                            if ((thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_GROUP.value) == 0) ||
                                    (thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_LOGICAL_READER.value) == 0) ||
                                    (thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_SHIFT.value) == 0) ||
                                    (thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_ZONE.value) == 0)) {
                                ((Map) resultMap.get(entry.getKey())).put("value", ((Map) ((Map) entry.getValue()).get("value")).get("code").toString());
                            } else if (thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_THING_TYPE.value) == 0) {
                                ((Map) resultMap.get(entry.getKey())).put("value", ((Map) ((Map) entry.getValue()).get("value")).get("serialNumber").toString());
                            }
                        }
                        break;
                    }
                }
            }
        }
        return resultMap;
    }
    public ValidationBean isValidDataType (ThingTypeField thingTypeField,  String udfLabel,  Map<String, Object> udfMap  , Group group, boolean validateVisibility, Date transactionDate){
        DataType dataType = DataTypeService.getInstance().get(thingTypeField.getDataType().getId());
        ValidationBean response = new ValidationBean();
        if (thingTypeField.getName().equals(udfLabel)) {
            if ((thingTypeField.getDataType().getTypeParent().equals(
                    ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value))
                    && (dataType.getType().equals(ThingTypeField.TypeParentSubGroup.TYPE_PARENT_DATA_TYPE_STANDARD_DATA.value))) {
                response = validationStandardDataTypes(thingTypeField, udfLabel, udfMap);
                } else if ((thingTypeField.getDataType().getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value))
                    && (dataType.getType().equals(ThingTypeField.TypeParentSubGroup.TYPE_PARENT_DATA_TYPE_NATIVE_OBJECT.value))) {
                response = validationNativeObject(
                        thingTypeField, udfLabel, udfMap, group, validateVisibility, transactionDate);
                } else if (thingTypeField.getDataType().getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_NATIVE_THING_TYPE.value)) {
                response = validationNativeThingType(thingTypeField, udfLabel, udfMap, group, validateVisibility);
                }
            }
        return response;
    }

    public Thing insert(Thing thing){
        //validation to avoid hibernate error on popDB
        try {
            Thing checkThing = ThingService.getInstance().getBySerialAndThingTypeCode(thing.getSerialNumber(), thing.getThingType().getCode());
            if (checkThing != null) {
                throw new UserException("Thing already exists");
            }
        } catch (NonUniqueResultException e) {
            throw new UserException("Thing already exists");
        }

        Subject subject =SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();
        Map<String, Object> udfs = new HashMap<>();

        ThingType thingType = null;
            if (thing.getThingType().getThingTypeFields() == null)
            {
                thing.getThingType().setThingTypeFields(new HashSet<ThingTypeField>());
            }
            List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().getByThingTypeCode(thing.getThingType().getThingTypeCode());
            for (ThingTypeField thingTypeField: thingTypeFields) {
                thing.getThingType().getThingTypeFields().add(thingTypeField);
            }
        thingType = thing.getThingType();
        List<Map<String, Object>> childrenUdf = null;
        if (!thing.getName().startsWith("RFID") && !thing.getName().startsWith("GPS")){
            switch (thingType.getThingTypeCode()){
                case "default_rfid_thingtype":
                    udfs = PopulateThings.getRFIDItemMap(0,thing.getModifiedTime(), thingType);
                    break;
                case "jackets_code":
                    udfs = PopulateThings.getClothingItemUdf(0, thing.getModifiedTime(), thingType);
                    break;
                case "pants_code":
                    udfs = PopulateThings.getClothingItemUdf(0, thing.getModifiedTime(), thingType);
                    break;
                case "tag_code":
                    udfs = PopulateThings.getTagMap(thing.getModifiedTime(), thingType);
                    break;
                case "shippingorder_code":
                    String name = thing.getName().replace("ShippingOrder","");
                    int i = Integer.parseInt(name);
                    udfs = PopulateThings.getShippingOrderItemUdf(i,
                            thing.getModifiedTime(),
                            thingType);
                    break;
                case "asset_code":
                    int j  = Integer.parseInt(thing.getName());
                    j = j  - 482;
                    String shipName = "SO0000"+j;
                    try {
                        Thing shippingOrder = ThingService.getInstance().getBySerialAndThingTypeCode(shipName,"shippingorder_code");
                        Map<String, Object> shippingOrderMap= new HashMap<>();
                        shippingOrderMap.put("thing", shippingOrder.publicMap());
                        udfs = PopulateThings.getAssetMap(thing.getModifiedTime(), thingType, shippingOrderMap);
                    } catch (NonUniqueResultException e) {
                       logger.info("Failed to get Shipping Order thing "+e);
                    }

                    break;
            }
        }
        Date transactionDate = new Date();
        HashMap<String, Object> parent = new HashMap<>();
        parent.put("thing", thing.getParent());
        thing = setThingProperties(udfs, true, thingType, transactionDate,
                    thing.getGroup(), thing.getSerial(), thing.getName(), currentUser, parent, thing.getGroup());

        insertThingMySQL(thing, true);
        Map<String, Object> thingMongoMap = null;
        thingMongoMap = setThingMongoProperties(
                    thing,
                    thingType,
                    thing.getGroup(),
                    udfs,
                    transactionDate,
                    true);

        ThingMongoService.getInstance().createNewThing(
                thing,
                thingMongoMap != null ?(Map<String, Object>) thingMongoMap.get("thingTypeFieldResponse"): null,
                transactionDate,
                false,
                new Stack<Long>(),
                new Date());

        associateParent(
                thing.getParent(),null,thing,thingMongoMap,transactionDate,new Stack<Long>(),
                false, currentUser, true);

        createOrUpdateChildrenUDF (thing.getGroup().getHierarchyName(false),childrenUdf,false,false,transactionDate,true,
                new Stack<Long>(), currentUser, true);
        ThingService.getInstance().updateExpressionsForChildren(new Stack<Long>(),thing, null, currentUser,
                (Map<String, Object>) thingMongoMap.get("thingTypeFieldResponse"), true, transactionDate);
        ThingService.getInstance().updateExpressionsForChildrenUDF(new Stack<Long>(),thingType,thing,currentUser,
                (Map<String, Object>) thingMongoMap.get("thingTypeFieldResponse"), true, transactionDate);

        ThingMongoService.getInstance().updateThingInReferences(thing);

        Map<String, ThingTypeField> typeFieldMap = ThingTypeFieldService.getInstance()
                .getThingTypeFieldByThingTypeCode(thing.getThingType().getThingTypeCode());
        ThingMongoService.getInstance().createUpdateThingSnapshot(thing.getId(),
                thing.getSerialNumber(),
                thing.getThingType().getThingTypeCode(),
                (Map<String, Object>) thingMongoMap.get("thingTypeFieldResponse"),
                transactionDate,
                false,
                typeFieldMap,
                currentUser);
        return thing;
    }
}
