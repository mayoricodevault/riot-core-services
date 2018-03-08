package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.ValidationBean;
import com.tierconnect.riot.iot.entities.ZoneProperty;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.iot.services.ZonePropertyService;
import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.tierconnect.riot.iot.reports_integration.ZoneTranslator.isZoneProperty;
import static com.tierconnect.riot.iot.reports_integration.ZoneTranslator.isZoneTypeId;

/**
 * Created by julio.rocha on 16-02-17.
 */
public class ReportDefinitionValidator {

    private static final String THING_TYPE_FIELD_ID_PARAMETER = "thingTypeFieldId";
    private static final String THING_TYPE_ID_PARAMETER = "thingTypeId";
    private static final String LABEL_PARAMETER = "label";
    private static final String PROPERTY_NAME_PARAMETER = "propertyName";

    private static final Map<String, String> SECTIONS_TO_VALIDATE;

    static {
        Map<String,String> tmpMapForSections = new LinkedHashMap<>();
        //The key will be to resolve the property map, and value for identity the section
        tmpMapForSections.put("reportFilter","Filters Section");
        tmpMapForSections.put("reportProperty","Display Options >> Report Properties Section");
        tmpMapForSections.put("reportRules","Display Options >> Display Rules Section");
        tmpMapForSections.put("reportGroupBy","Display Options >> Group By Section");
        tmpMapForSections.put("reportEntryOption","Data Entry Action");
        SECTIONS_TO_VALIDATE = Collections.unmodifiableMap(tmpMapForSections);
    }

    private final Group group;
    private Map<String, Object> parameters;
    private ValidationBean response;

    public ReportDefinitionValidator(Group group,
                                     Map<String, Object> parameters) {
        this.group = group;
        this.parameters = parameters;
        response = new ValidationBean();
        executeValidation();
    }

    private void executeValidation() {
        if (group == null) {
            setMessage("Group has not been selected, please select one");
        } else {
            sectionsLoop:
            for(String key : SECTIONS_TO_VALIDATE.keySet()){
                List<Map<String, Object>> maps = (List<Map<String, Object>>) parameters.get(key);
                if("reportEntryOption".equals(key)){
                    for(Map<String, Object> reportEntryOptionByMap : maps){
                        List<Map<String, Object>> reportEntryOptionPropertyBy = (List<Map<String, Object>>)
                                reportEntryOptionByMap.get("reportEntryOptionProperty");
                        if(hasInvalidParametersForGroup(
                                reportEntryOptionPropertyBy,
                                SECTIONS_TO_VALIDATE.get(key))){
                            break sectionsLoop;
                        }
                    }
                }else if(hasInvalidParametersForGroup(
                        maps,
                        SECTIONS_TO_VALIDATE.get(key))){
                    break sectionsLoop;
                }
            }
        }
    }

    private boolean hasInvalidParametersForGroup(List<Map<String, Object>> parameterList, String section) {
        if (parameterList == null || parameterList.isEmpty()) {
            return false;
        }
        boolean isValid = true;
        for (Map<String, Object> parameter : parameterList) {
            Object propertyObject = parameter.get(PROPERTY_NAME_PARAMETER);
            String propertyName = (propertyObject != null) ? propertyObject.toString() : null;
            if(StringUtils.isEmpty(propertyName) || isZoneTypeId(propertyName) || (isZoneProperty(propertyName) && !propertyName.contains(","))){
                continue;
            }

            if(isZoneProperty(propertyName) && propertyName.contains(",")){
                Long propertyID = Long.parseLong(propertyName.split(",")[1]);
                isValid = isZonePropertyInGroup(propertyID);
            } else {
                Long ttID = getLongIDParameter(parameter, THING_TYPE_ID_PARAMETER);
                Long ttfID = getLongIDParameter(parameter, THING_TYPE_FIELD_ID_PARAMETER);

                if(ttID != null && ttID != 0) {
                    isValid = isThingTypeInGroup(ttID);
                } else if(ttfID != null && ttfID != 0) {
                    ThingTypeField thingTypeField = ThingTypeFieldService.getInstance().get(ttfID);
                    isValid = (thingTypeField == null)? true : isThingTypeInGroup(thingTypeField.getThingType().getId());
                }
            }

            if(!isValid){
                if(parameter.containsKey(LABEL_PARAMETER) && !StringUtils.isEmpty(parameter.get(LABEL_PARAMETER)+"")){
                    setMessage(section + ": " + parameter.get(LABEL_PARAMETER) + " is not present in the selected group");
                } else {
                    setMessage(section + ": " + propertyName + " is not present in the selected group");
                }
                break;
            }
        }
        return !isValid;
    }

    private Long getLongIDParameter(Map<String, Object> parameter, String key){
        Object value = parameter.get(key);
        return (value != null && !StringUtils.isEmpty(value.toString()) && StringUtils.isNumeric(value.toString()))?
                Long.parseLong(value.toString()) : null;
    }

    private boolean isThingTypeInGroup(Long ttID){
        ThingType thingType = ThingTypeService.getInstance().get(ttID);
        if(thingType == null) {
            return false;
        } else {
            return groupValidation(thingType.getGroup());
        }
    }

    private boolean isZonePropertyInGroup(Long zpID){
        ZoneProperty zoneProperty = ZonePropertyService.getInstance().get(zpID);
        if(zoneProperty != null){
            Group groupOfProperty = zoneProperty.getZoneType().getGroup();
            return groupValidation(groupOfProperty);
        }
        return false;
    }

    /**
     * @param groupToTest
     * @return true/false depending if groupToTest is inside tree up|down
     */
    private boolean groupValidation(Group groupToTest){
        return this.group.getId().equals(groupToTest.getId())
                || groupToTest.getAscendantIds().contains(this.group.getId())
                || GroupService.getInstance().isGroupInsideTree(this.group, groupToTest);//groupToTest can be parent of this.group
    }

    private void setMessage(String message) {
        response.setErrorDescription(message);
    }

    public ValidationBean getValidationResponse() {
        return response;
    }
}
