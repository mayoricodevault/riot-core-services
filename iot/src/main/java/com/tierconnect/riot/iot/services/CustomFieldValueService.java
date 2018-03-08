package com.tierconnect.riot.iot.services;

import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.mysema.query.types.OrderSpecifier;
import com.tierconnect.riot.iot.entities.CustomFieldValue;
import com.tierconnect.riot.iot.entities.QCustomFieldValue;
import com.tierconnect.riot.appcore.utils.QueryUtils;


import java.util.List;

/**
 * Created by cfernandez
 * 11/7/2014.
 */
public class CustomFieldValueService extends CustomFieldValueServiceBase{

    public List<CustomFieldValue> getCustomFieldValuesByCustomObjectId(Long objectId){
        HibernateQuery query = getCustomFieldValueDAO().getQuery();
        OrderSpecifier orderSpecifiers[] = QueryUtils.getOrderFields(QCustomFieldValue.customFieldValue, "customField.id:asc");
        return query.where(QCustomFieldValue.customFieldValue.customObject.id.eq(objectId)).orderBy(orderSpecifiers)
                .list(QCustomFieldValue.customFieldValue);
    }

    public List<CustomFieldValue> getCustomFieldValuesByCustomObjectId(Long objectId, String positionOrder){
        HibernateQuery query = getCustomFieldValueDAO().getQuery();
        OrderSpecifier orderSpecifiers[] = QueryUtils.getOrderFields(QCustomFieldValue.customFieldValue, positionOrder);
        return query.where(QCustomFieldValue.customFieldValue.customObject.id.eq(objectId)).orderBy(orderSpecifiers)
                .list(QCustomFieldValue.customFieldValue);
    }



}
