package com.tierconnect.riot.iot.services;

import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.CustomObject;
import com.tierconnect.riot.iot.entities.QCustomObject;

import java.util.List;

/**
 * Created by cfernandez
 * 11/7/2014.
 */
public class CustomObjectService extends CustomObjectServiceBase {

    public List<CustomObject> getCustomObjectsByCustomObjectTypeId(Long objectTypeId){
        HibernateQuery query = getCustomObjectDAO().getQuery();
        return query.where(QCustomObject.customObject.customObjectType.id.eq(objectTypeId))
                .list(QCustomObject.customObject);
    }

}
