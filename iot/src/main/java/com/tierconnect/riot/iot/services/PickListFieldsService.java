package com.tierconnect.riot.iot.services;

import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.*;

import javax.annotation.Generated;
import java.util.*;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class PickListFieldsService extends PickListFieldsServiceBase 
{

    public static final int LIMIT = 9999;
    public String getValueByThingFieldId( Long thingFieldId ) {
        HibernateQuery query = PickListFieldsService.getPickListFieldsDAO().getQuery();
        List<String> queryResult = query.where(QPickListFields.pickListFields.thingFieldId.eq(thingFieldId))
                .list(QPickListFields.pickListFields.fieldsStored);
        if(queryResult != null && queryResult.size() > 0) {
            return queryResult.get(0);
        }return "";
    }

    public PickListFields getPickListByThingFieldId( Long thingFieldId ) {
        HibernateQuery query = PickListFieldsService.getPickListFieldsDAO().getQuery();
        List<PickListFields> queryResult = query.where(QPickListFields.pickListFields.thingFieldId.eq(thingFieldId))
                .list(QPickListFields.pickListFields);
        if(queryResult != null && queryResult.size() > 0) {
            return queryResult.get(0);
        }return null;
    }

    public Set<String> getPickListSet(String values, String newValue) {
        if(values == null) values = "";
        String valueList[] = values.split(",");
        Set<String> setRes = new LinkedHashSet<>();
        for(int it=0; valueList != null && it < valueList.length; it++) {
            if(valueList[it] != null && valueList[it].length() > 0) {
                setRes.add(valueList[it]);
            }
        }
        if(newValue != null) {
            setRes.remove(newValue);
            setRes.add(newValue);
        }

        return setRes;
    }

    public void updatePickListWithFilters( ReportDefinition reportDefinition ) {

        List<ReportFilter> reportFilters = reportDefinition.getReportFilter();
        for (ReportFilter reportFilter : reportFilters) {
            List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().getThingTypeFieldByName(reportFilter.getPropertyName() != null ? reportFilter.getPropertyName() : "");
            updatePickListFromReportDefinition( reportFilter.getPropertyName(), reportFilter.getValue() );
        }

        for (ReportRule reportRule : reportDefinition.getReportRule()) {
            List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().getThingTypeFieldByName(reportRule.getPropertyName() != null ? reportRule.getPropertyName() : "");
            updatePickListFromReportDefinition( reportRule.getPropertyName(), reportRule.getValue() );
        }
    }

    public void updatePickListFromReportDefinition (String propertyName, String value) {
        List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().getThingTypeFieldByName(propertyName != null ? propertyName : "");

        if(value == null || value.isEmpty()) return;

        if ((thingTypeFields != null) && (!thingTypeFields.isEmpty())) {
            for (ThingTypeField thingTypeField : thingTypeFields) {
                PickListFields pickList = getPickListByThingFieldId(thingTypeField.getId());
                if (pickList != null) {
                    String pickData = pickList.getFieldsStored();
                    Set<String> newSet = getPickListSet(pickData, value);
                    pickList.setFieldsStored(getPickValuesFromSet(newSet));
                    PickListFieldsService.getInstance().update(pickList);
                } else {
                    pickList = new PickListFields();
                    pickList.setThingFieldId(thingTypeField.getId());
                    pickList.setFieldsStored(value);
                    PickListFieldsService.getInstance().insert(pickList);
                }
            }
        }
    }

    public String getPickValuesFromSet(Set<String> pickSet) {
        Object pickArrayObj[] = pickSet.toArray();
        String pickArray[] = new String[pickArrayObj.length];

        for(int it = 0; it < pickArrayObj.length; it++) {
            pickArray[it] = pickArrayObj[it].toString();
        }
        List<String> pickArrayRes = new ArrayList<>(Arrays.asList(pickArray).subList(0, Math.min(LIMIT ,pickArray.length)));

        String res = "";
        for(String value : pickArrayRes) {
            res += value + ",";
        }
        return res;
    }
}

