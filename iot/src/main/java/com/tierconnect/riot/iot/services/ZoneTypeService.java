package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.expr.BooleanExpression;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.iot.dao.ZoneTypeDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Generated;
import java.util.*;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class ZoneTypeService extends ZoneTypeServiceBase {
    public static final String ZONE_PROPERTIES = "zoneProperties";


    public static void parseZoneProperties(ZoneType zoneType, Map<String, Object> map) {
        List<Map<String, Object>> zonePropertiesMap= (List<Map<String,Object>>)map.get(ZONE_PROPERTIES);
        map.remove(ZONE_PROPERTIES);
        ValidationBean validationBean = validateZonePropertyMapStructure(zonePropertiesMap);
        if (validationBean.isError()){
            throw new UserException("[\"Some properties are not valid: " + validationBean.getErrorDescription() + "\"]");
        }
        List<ZoneProperty> zonePropertiesMapToDelete = new ArrayList<>();
        List<ZoneProperty> zonePropertiesMapToUpdate = new ArrayList<>();
        List<ZoneProperty> zonePropertiesMapToAdd = new ArrayList<>();
        for (Map<String,Object>zonePropertyMap : zonePropertiesMap){
            ZoneProperty zonePropertyChanged = new ZoneProperty();
            BeanUtils.setProperties(zonePropertyMap, zonePropertyChanged);
            zonePropertyChanged.setZoneType(zoneType);
            String operation = zonePropertyMap.get("operation").toString();
            switch (operation){
                case "update": zonePropertiesMapToUpdate.add(zonePropertyChanged);
                    break;
                case "add": zonePropertiesMapToAdd.add(zonePropertyChanged);
                    break;
                default: zonePropertiesMapToDelete.add(zonePropertyChanged);
                    break;
            }
        }
        List<String> messages = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();
        if (zonePropertiesMapToDelete.size() > 0){
            errorMessages = deleteZoneProperties(zonePropertiesMapToDelete, zoneType);
            if (errorMessages.size() > 0){
                for (int position = 0 ; position <errorMessages.size();position++){
                    errorMessages.set(position, "Zone Properties could not be removed because " + errorMessages.get(position));
                }
                messages.addAll(errorMessages);
            }
        }
        if (zonePropertiesMapToUpdate.size() > 0){
            errorMessages = updateZoneProperty(zonePropertiesMapToUpdate, zoneType);
            if (errorMessages.size() > 0){
                messages.addAll(errorMessages);
            }
        }
        if (zonePropertiesMapToAdd.size() > 0){
            errorMessages =  addZoneProperty(zonePropertiesMapToAdd, zoneType);
            if (errorMessages.size() > 0){
                messages.addAll(errorMessages);
            }
        }
        List<ZoneProperty> zonePropertiesNews = ZonePropertyService.getInstance().getbyZoneType(zoneType);
        zoneType.setZoneProperties(zonePropertiesNews);
        if (messages.size() > 0){
            throw new UserException(StringUtils.join(messages, ", ") );
        }
    }

    public static void updatingZoneTypeFromZone(Object zoneType, Zone zone) {
        Map<String, Object> zoneTypeMap = (Map<String, Object>) zoneType;

        if (zoneTypeMap != null) {
            Long zoneTypeId = 0L;
            if (zoneTypeMap.containsKey("id")) {
                zoneTypeId = Long.valueOf(zoneTypeMap.get("id").toString());
                ZoneType zoneTypeItem = ZoneTypeService.getInstance().get( zoneTypeId );
                zone.setZoneType(zoneTypeItem);
            }

            List<Map<String, Object> > zonePropertiesMapList;
            if (zoneTypeMap.containsKey(ZONE_PROPERTIES)) {
                zonePropertiesMapList = (List<Map<String, Object> >) zoneTypeMap.get( ZONE_PROPERTIES );
                ZonePropertyValueService.updateZonePropertyValue( zone, zonePropertiesMapList );
            }
        }
    }

    /**
     * Validates zoneType before insert it
     * @param zoneType
     */
    @Override
    public void validateInsert(ZoneType zoneType) {
        if (StringUtils.isEmpty(zoneType.getName())) {
            throw new UserException("Name cannot be null or empty.");
        }
        validateZoneTypeCode(zoneType);
        super.validateInsert(zoneType);
    }

    /**
     * Validates zoneTypeCode
     * @param zoneType
     */
    private void validateZoneTypeCode(ZoneType zoneType) {
        // System.out.println("validateThingTypeCode method");
        String thingTypeCode = zoneType.getZoneTypeCode();
        if (StringUtils.isEmpty(thingTypeCode)) {
            throw new UserException(String.format("Zone Type Code is required."));
        }

        boolean existsThingTypeCode;
        if (zoneType.getId() == null) {
            // validating for insert case
            existsThingTypeCode = existsZoneTypeCode(thingTypeCode, zoneType.getGroup());
        } else {
            // validating for update case
            existsThingTypeCode = existsZoneTypeCode(thingTypeCode, zoneType.getGroup(), zoneType.getId());
        }

        if (existsThingTypeCode) {
            // System.out.println("existsThingTypeCode true");
            throw new UserException(String.format("Zone Type Code '[%s]' already exists.", zoneType.getZoneTypeCode()));
        }
    }

    /**
     * Validates if zonetypeCode exists in db
     * @param code
     * @param group
     * @return
     */
    public boolean existsZoneTypeCode(String code, Group group) {
        BooleanExpression predicate = QZoneType.zoneType.zoneTypeCode.eq(code);
        ZoneTypeDAO zoneTypeDAO = getZoneTypeDAO();
        return zoneTypeDAO.getQuery().where(predicate).exists();
    }

    /**
     * Validates if zonetypeCode exists in db
     * @param code
     * @param group
     * @param excludeId
     * @return
     */
    public boolean existsZoneTypeCode(String code, Group group, Long excludeId) {
        BooleanExpression predicate = QZoneType.zoneType.zoneTypeCode.eq(code);
        predicate = predicate.and(QZoneType.zoneType.id.ne(excludeId));
        ZoneTypeDAO zoneTypeDAO = getZoneTypeDAO();
        return zoneTypeDAO.getQuery().where(predicate).exists();
    }

    public void delete( ZoneType zoneType )
    {
        validateDelete( zoneType );
        getZoneTypeDAO().delete( zoneType );
        deleteFavorite( zoneType );
    }

    /**
     * Valid delete zoneType
     * @param zoneType
     */
    public void validateDelete( ZoneType zoneType )
    {
        validDependenciesWithZone(zoneType);
        validDeleteDependencies(zoneType);
    }

    /**
     * validation of Zone Type's Zone Properties Dependencies before to delete
     * @param zoneType zone Type
     */
    public void validDeleteDependencies(ZoneType zoneType){
        List<ZoneProperty> zoneProperties = ZonePropertyService.getInstance().getbyZoneType(zoneType);
        List<String> messageErrors = ZoneTypeService.deleteZoneProperties(zoneProperties, zoneType);
        if (messageErrors.size() > 0){
            for (int position=0; position<messageErrors.size(); position++){
                messageErrors.set(position, "\"Zone Type could not be removed because there are Zone Properties for this" + messageErrors.get(position));
            }
            throw new UserException("[" + StringUtils.join(messageErrors, ", ") + "]");
        }
    }

    /**
     * Valid
     * @param zoneType
     */
    public void validDependenciesWithZone(ZoneType zoneType){
        List<Zone> zones = ZoneService.getInstance().getZoneByZoneType(zoneType.getId());
        if ((zones != null) && (!zones.isEmpty())){
            String messageErrors = "Zone Type could not be removed because there are instances of Zone with this Zone Type.";
            throw new UserException(messageErrors);
        }
    }


    /**
     * delete Zone Properties when them don't have dependencies with reports
     * @param zoneProperties
     * @return if there are zone properties that don't exist or have dependencies with reports
     * is going to return a list of them
     */
    public static List<String> deleteZoneProperties(List<ZoneProperty> zoneProperties, ZoneType zoneType){
        List<String> errorMessages = new ArrayList<>();
        if (zoneProperties.size() > 0){
            List<String> zonePropertyNames = new ArrayList<>();
            List<String> currentReportNamesDependencies = new ArrayList<>();
            Set<Long> zonePropertyIds = new HashSet<>();
            List<String> zonePropertiesValuesDependencies = new ArrayList<>();
            List<Long> zoneIds = new ArrayList<>();
            for (ZoneProperty zoneProperty : zoneProperties){
                ZoneProperty zonePropertyFromDataBase = ZonePropertyService.getInstance().getZonePropertyByZoneProperty(zoneProperty);

                if (zoneProperty.getId() != null){
                    String propertyName = "zoneProperty.id," + zoneProperty.getId();
                    currentReportNamesDependencies = getReportPropertiesDependencies(propertyName,currentReportNamesDependencies);
                    currentReportNamesDependencies = getReportFiltersDependencies(propertyName,currentReportNamesDependencies);
                    currentReportNamesDependencies = getReportRulesDependencies(propertyName,currentReportNamesDependencies);
                    currentReportNamesDependencies = getReportGroupsByDependencies(propertyName,currentReportNamesDependencies);
                    if (currentReportNamesDependencies.size() == 0){
                        List<ZonePropertyValue> zonePropertyValues = ZonePropertyValueService.getInstance().getZonePropertyValueExistValue(zonePropertyFromDataBase.getId());
                        if ((zonePropertyValues != null) && (zonePropertyValues.size() > 0)){ //With Zone dependencies
                            for (ZonePropertyValue currentZonePropertyValue: zonePropertyValues){
                                zoneIds = getUniqueIds(zoneIds, currentZonePropertyValue.getZoneId());
                            }
                            zonePropertiesValuesDependencies.add(zoneProperty.getName());
                        } else {
                            //Without zone dependencies
                            zonePropertyValues = ZonePropertyValueService.getInstance().getZonePropertiesByZonePropertyId(zonePropertyFromDataBase.getId());
                            ZonePropertyValueService.getInstance().deleteZonePropertyValues(zonePropertyValues);
                            zoneProperty.setZoneType(null);
                            zonePropertyIds.add(zoneProperty.getId());
                        }

                    }
                } else {
                    zonePropertyNames.add(zoneProperty.getName());
                }
            }
            //To remove
            if(!zonePropertyIds.isEmpty()){
                for (Long id : zonePropertyIds) {
                    for(int i = 0; i< zoneProperties.size() ; i++) {
                        if(id.compareTo(zoneProperties.get(i).getId()) == 0){
                            zoneProperties.remove(zoneProperties.get(i));
                        }
                    }
                }
                zoneType.setZoneProperties(zoneProperties);
                ZoneTypeService.getInstance().update(zoneType);
                for (Long id : zonePropertyIds) {
                    ZonePropertyService.getInstance().delete(ZonePropertyService.getInstance().get(id));
                }
            }
            if (zonePropertyNames.size() > 0){
                errorMessages.add("don't exist: " + StringUtils.join(zonePropertyNames, ", ") + ".");
            }
            if (currentReportNamesDependencies.size() > 0){
                String reportNamesString = StringUtils.join(currentReportNamesDependencies, ", ");
                errorMessages.add("have references into Reports: " + reportNamesString + ".");
            }

            if ((zonePropertiesValuesDependencies.size() > 0) && (zoneIds.size() > 0) ){
                List<String> zoneNames = new ArrayList<>();
                for (Long zoneId: zoneIds){
                    String currentZoneName = ZoneService.getInstance().getZoneNameByZoneId(zoneId);
                    zoneNames.add(currentZoneName);
                }
                if (zoneNames.size() > 0){
                    errorMessages.add(" the Zone Type Properties: " + StringUtils.join(zonePropertiesValuesDependencies, ", ") + " have references in Zones: " + StringUtils.join(zoneNames, ", ") + ".");
                }
            }
        }
        return errorMessages;
    }
    /**
     * update zoneproperty table when exist the zone properties
     * @param zoneProperty a zone property that is going to be updated in zoneproperty table
     * @return if there are zone properties that don't exist is going to return a list of them
     */
    public static List<String> updateZoneProperty(List<ZoneProperty> zoneProperties, ZoneType zoneType){
        List<String> errorMessages = new ArrayList<>();
        List<String> currentReportNamesDependencies = new ArrayList<>();
        if (zoneProperties.size() > 0) {
            List<String> zonePropertiesNames = new ArrayList<>();
            Map<String, String> duplicateZonePropertiesNames = new LinkedHashMap<>();
            Map<String, String> zonePropertiesValuesDependencies = new LinkedHashMap<>();
            List<Long> zoneIds = new ArrayList<>();
            for (ZoneProperty zoneProperty : zoneProperties) {
                if (zoneProperty.getId() != null) {
                    //Verify Report dependencies
                    String propertyName = "zoneProperty.id," + zoneProperty.getId();
                    currentReportNamesDependencies = getReportNameDependencies(propertyName, currentReportNamesDependencies);
                    zonePropertiesValuesDependencies.put(zoneProperty.getName(), zoneProperty.getName());
                    //Logic to update
                    if (currentReportNamesDependencies.size() == 0){
                        ZoneProperty zonePropertyFromDataBaseByName = ZonePropertyService.getInstance().getZonePropertyByName(zoneProperty.getName(), zoneType);
                        ZoneProperty zonePropertyCurrentFromDataBase = ZonePropertyService.getInstance().getZonePropertyById(zoneProperty.getId());//This is related to UI
                        List<ZonePropertyValue> zonePropertyValues = ZonePropertyValueService.getInstance().getZonePropertyValueExistValue(zoneProperty.getId());
                        if (zonePropertyCurrentFromDataBase != null){
                            if ((zonePropertyValues != null) && (zonePropertyValues.size() > 0)){ //With Zone dependencies
                                for (ZonePropertyValue currentZonePropertyValue: zonePropertyValues){
                                    zoneIds = getUniqueIds(zoneIds, currentZonePropertyValue.getZoneId());
                                }
                                zonePropertiesValuesDependencies.put(zoneProperty.getName(), zoneProperty.getName());
                            } else {
                                //Without zone dependencies
                                if ((zonePropertyFromDataBaseByName == null) || (zonePropertyFromDataBaseByName.equals(zonePropertyCurrentFromDataBase))) {
                                    updateZoneProperty(zonePropertyCurrentFromDataBase, zoneProperty);
                                } else {
                                    boolean isDuplicated = true;
                                    for (ZoneProperty zonePropertyIn : zoneProperties) {
                                        if (zonePropertyFromDataBaseByName.getId().equals(zonePropertyIn.getId())) {
                                            isDuplicated = false;
                                        }
                                    }
                                    if (isDuplicated) {
                                        duplicateZonePropertiesNames.put(zoneProperty.getName(), zoneProperty.getName());
                                    } else {
                                        updateZoneProperty(zonePropertyCurrentFromDataBase,zoneProperty);
                                    }
                                }
                            }
                        } else {
                            zonePropertiesNames.add(zoneProperty.getName());
                        }
                    }
                }
            }
            if (currentReportNamesDependencies.size() > 0){
                String reportNamesString = StringUtils.join(currentReportNamesDependencies, ", ");
                errorMessages.add("Zone Type Properties: " + StringUtils.join(zonePropertiesValuesDependencies.values(), ", ") + " could not be updated because they have references into Reports:" + reportNamesString + ".");
            }
            if (zonePropertiesNames.size() > 0){
                errorMessages.add("Zone Properties could not be updated because don't exist: " + StringUtils.join(zonePropertiesNames, ", ") + ".");
            }
            if (duplicateZonePropertiesNames.size() > 0){
                errorMessages.add("Zone Properties could not be updated because is duplicated: " + StringUtils.join(duplicateZonePropertiesNames.values(), ", ") + ".");
            }
            if ((zonePropertiesValuesDependencies.size() > 0) && (zoneIds.size() > 0) ){
                List<String> zoneNames = new ArrayList<>();
                for (Long zoneId: zoneIds){
                    String currentZoneName = ZoneService.getInstance().getZoneNameByZoneId(zoneId);
                    zoneNames.add(currentZoneName);
                }
                if (zoneNames.size() > 0){
                    errorMessages.add("Zone Type Properties: " + StringUtils.join(zonePropertiesValuesDependencies.values(), ", ") + " could not be updated because they have references in Zones: " + StringUtils.join(zoneNames, ", ") + ".");
                }
            }
        }
        return errorMessages;
    }

    public static List<String> getReportNameDependencies(String propertyName, List<String> currentReportNamesDependencies){
        currentReportNamesDependencies = getReportPropertiesDependencies(propertyName, currentReportNamesDependencies);
        currentReportNamesDependencies = getReportRulesDependencies(propertyName, currentReportNamesDependencies);
        currentReportNamesDependencies = getReportGroupsByDependencies(propertyName, currentReportNamesDependencies);
        return currentReportNamesDependencies;
    }

    /**
     *
     * @param zoneTypeIds
     * @param zoneTypeIdFromDataBase
     * @return  Zone Type Id List
     */
    public static List<Long> getUniqueIds(List<Long> ids, Long idFromDataBase){
        Boolean hasZoneTypeId = false;
        if (ids.size() > 0){
            for (Long zoneTypeId: ids){
                if (zoneTypeId.equals(idFromDataBase)){
                    hasZoneTypeId = true;
                }
            }
        }
        if (!hasZoneTypeId){
            ids.add(idFromDataBase);
        }
        return ids;
    }

    public static void updateZoneProperty (ZoneProperty zonePropertyCurrentFromDataBase, ZoneProperty zoneProperty){
        zonePropertyCurrentFromDataBase.setType(zoneProperty.getType());
        zonePropertyCurrentFromDataBase.setName(zoneProperty.getName());
        zonePropertyCurrentFromDataBase.setZoneType(zoneProperty.getZoneType());
        ZonePropertyService.getInstance().update(zonePropertyCurrentFromDataBase);
    }

    /**
     * add zoneproperty table when don't exist the zone properties
     * @param zoneProperties list of zone properties
     * @param zoneType zone type
     * @return if there are zone properties dulplicated is going to return a list of them
     */
    public static List<String> addZoneProperty(List<ZoneProperty> zoneProperties, ZoneType zoneType){
        List<String> errorMessages = new ArrayList<>();
        if (zoneProperties.size() > 0) {
            List<String> zonePropertiesNames = new ArrayList<>();
            for (ZoneProperty zoneProperty : zoneProperties) {
                ZoneProperty zonePropertyFromDataBase = ZonePropertyService.getInstance().getZonePropertyByName(zoneProperty.getName(), zoneType);
                if (zonePropertyFromDataBase == null){
                    ZonePropertyService.getInstance().insert(zoneProperty);
                } else {
                    zonePropertiesNames.add(zoneProperty.getName());
                }
            }
            if (zonePropertiesNames.size() > 0){
                errorMessages.add("\"Zone Properties could not be added because already exist: " + StringUtils.join(zonePropertiesNames, ", ") + ".\"");
            }
        }
        return errorMessages;
    }

    /**
     * get current Report Definition's name
     * @param currentReportNames list of current report definitions names
     * @param reportName report name
     * @return a list of Report Definition's name
     */
    public static List<String> getCurrentReportName(List<String>currentReportNames, String reportName){
        if ((currentReportNames.size() < 1) || (!currentReportNames.contains(reportName))) {
            currentReportNames.add(reportName);
        }
        return currentReportNames;
    }

    /**
     * get Report Properties with dependencies on zone property's name
     * @param propertyName zone property's name
     * @param currentReportNames current reports definitions names
     * @return a list of Report Definition's name
     */
    public static List<String> getReportPropertiesDependencies(String propertyName, List<String> currentReportNames ){
        String reportName;
        List<ReportProperty> reportProperties = ReportPropertyService.getInstance().getPropertiesByPropertyName(propertyName);
        if (reportProperties.size() > 0) {
            for (ReportProperty reportProperty : reportProperties) {
                reportName = reportProperty.getReportDefinition().getName();
                currentReportNames = getCurrentReportName(currentReportNames, reportName);
            }
        }
        return currentReportNames;
    }

    /**
     * get Report Rules with Dependencies on  zone property's name
     * @param propertyName
     * @param currentReportNames
     * @return a list of Report Definition's name
     */
    public static List<String> getReportRulesDependencies(String propertyName, List<String> currentReportNames){
        String reportName;
        List<ReportRule> reportRules = ReportRuleService.getInstance().getRuleByPropertyName(propertyName);
        if (reportRules.size() > 0) {
            for (ReportRule reportRule : reportRules) {
                reportName = reportRule.getReportDefinition().getName();
                currentReportNames = getCurrentReportName(currentReportNames, reportName);
            }
        }
        return currentReportNames;
    }

    /**
     * get Report Filters with Dependencies on  zone property's name
     * @param propertyName
     * @param currentReportNames
     * @return a list of Report Definition's name
     */
    public static List<String> getReportFiltersDependencies(String propertyName, List<String> currentReportNames){
        String reportName;
        List<ReportFilter> reportFilters = ReportFilterService.getInstance().getFiltersByPropertyName(propertyName);
        if (reportFilters.size() > 0) {
            for (ReportFilter reportFilter : reportFilters) {
                reportName = reportFilter.getReportDefinition().getName();
                currentReportNames = getCurrentReportName(currentReportNames, reportName);
            }
        }
        String[] propertyNameArray = propertyName.split(",");
        if (propertyNameArray != null && propertyNameArray.length == 2) {
            reportFilters = ReportFilterService.getInstance()
                    .getFiltersByPropertyNameAndZonePropertyId(propertyNameArray[0], Long.valueOf(propertyNameArray[1]));
            for (ReportFilter reportFilter : reportFilters) {
                reportName = reportFilter.getReportDefinition().getName();
                currentReportNames = getCurrentReportName(currentReportNames, reportName);
            }
        }
        return currentReportNames;
    }

    /**
     * get Report GroupBy with Dependencies on  zone property's name
     * @param propertyName
     * @param currentReportNames
     * @return a list of Report Definition's name
     */
    public static List<String> getReportGroupsByDependencies(String propertyName, List<String> currentReportNames ){
        String reportName;
        List<ReportGroupBy> reportGroupsBy = ReportGroupByService.getInstance().getGroupByPropertyName(propertyName);
        if (reportGroupsBy.size() > 0) {
            for (ReportGroupBy reportGroupBy : reportGroupsBy) {
                reportName = reportGroupBy.getReportDefinition().getName();
                currentReportNames = getCurrentReportName(currentReportNames, reportName);
            }
        }
        return currentReportNames;
    }



    /**
     *
     * @param zonePropertiesMap Map List of zone Properties
     * @return a ValidationBean with messages if there are errors
     */
    public static ValidationBean validateZonePropertyMapStructure(List<Map<String, Object>> zonePropertiesMap){
        List<String> messages = new ArrayList<>();
        ValidationBean validationBean = new ValidationBean();
        if (zonePropertiesMap != null){
            for (Map<String,Object> zonePropertyMap : zonePropertiesMap){
                Object operation = zonePropertyMap.get("operation");
                if ((!Utilities.isString(operation)) || !(operation.equals("update") || operation.equals("add") ||
                        operation.equals("delete"))){
                    messages.add("operation");
                } else {
                    Object id = zonePropertyMap.get("id");
                    if ((operation.equals("update")) && (!Utilities.isInteger(id))){
                        messages.add("id");
                    }
                    Object name = zonePropertyMap.get("name");
                    if ((operation.equals("update") || operation.equals("add")) && (!Utilities.isString(name))){
                        messages.add("name");
                    }
                    Object type = zonePropertyMap.get("type");
                    if (!Utilities.isInteger(type)){
                        messages.add("type");
                    }
                }
            }
        }
        if (validationBean.isError()){
            validationBean.setErrorDescription(StringUtils.join(messages, ", ") + ".");
        }
        return validationBean;
    }

    public ZoneType getByCode(String code, Group group){
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QZoneType.zoneType.zoneTypeCode.eq(code));
        be = be.and(QZoneType.zoneType.group.eq(group));
        ZoneType zoneType = getZoneTypeDAO().selectBy(be);
        return zoneType;
    }

    /**
     * it updates a message of kafka cache topic ___v1___cache___zonetype.
     *
     * @param zoneType
     * @param delete
     */
	public static void refreshCache( ZoneType zoneType, boolean delete )
	{
		BrokerClientHelper.refreshZoneTypeCache( zoneType, delete );
	}
}

