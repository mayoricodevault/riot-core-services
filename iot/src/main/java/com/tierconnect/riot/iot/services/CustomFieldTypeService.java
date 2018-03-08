package com.tierconnect.riot.iot.services;

import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.CustomFieldType;
import com.tierconnect.riot.iot.entities.QCustomField;
import com.tierconnect.riot.iot.entities.QCustomFieldType;

/**
 * Created by cfernandez
 * 10/24/2014.
 */
public class CustomFieldTypeService extends CustomFieldTypeServiceBase{

    public CustomFieldType selectById(Long id, String extra, String only){
        CustomFieldType customFieldType = get(id);
        // apply extra
        // apply only

        return customFieldType;
    }

    public CustomFieldType selectByName(String name){
        HibernateQuery query = getCustomFieldTypeDAO().getQuery();
        return query.where(QCustomFieldType.customFieldType.name.eq(name))
                    .uniqueResult(QCustomFieldType.customFieldType);
    }

}
