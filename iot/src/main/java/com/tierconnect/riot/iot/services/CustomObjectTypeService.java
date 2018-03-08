package com.tierconnect.riot.iot.services;

import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.CustomObjectType;
import com.tierconnect.riot.iot.entities.QCustomObjectType;
import com.tierconnect.riot.sdk.dao.Pagination;

import java.util.List;

/**
 * Created by cfernandez
 * 10/24/2014.
 */
public class CustomObjectTypeService extends CustomObjectTypeServiceBase{

    public CustomObjectType selectByCode(String code){
        HibernateQuery query = getCustomObjectTypeDAO().getQuery();
        return query.where(QCustomObjectType.customObjectType.code.eq(code))
                .uniqueResult(QCustomObjectType.customObjectType);
    }

    public CustomObjectType selectByCodeAndApplication(String code, String applicationCode){
        HibernateQuery query = getCustomObjectTypeDAO().getQuery();
        return query.where(QCustomObjectType.customObjectType.code.eq(code)
                .and(QCustomObjectType.customObjectType.customApplication.code.eq(applicationCode)))
                  .uniqueResult(QCustomObjectType.customObjectType);
    }

    public List<CustomObjectType> selectWithExclude(Long excludeObjectId, Pagination pagination, String orderString){
        HibernateQuery query = getCustomObjectTypeDAO().getQuery();
        return query.where(QCustomObjectType.customObjectType.id.ne(excludeObjectId))
                    .list(QCustomObjectType.customObjectType);
    }

    public List<CustomObjectType> selectByApplication(Long applicationId){
        HibernateQuery query = getCustomObjectTypeDAO().getQuery();
        return query.where(QCustomObjectType.customObjectType.customApplication.id.eq(applicationId))
                .list(QCustomObjectType.customObjectType);
    }
}
