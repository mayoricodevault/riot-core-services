package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.iot.entities.CustomApplication;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.QCustomApplication;

/**
 * Created by cfernandez
 * 10/29/2014.
 */
public class CustomApplicationService extends CustomApplicationServiceBase{

    public CustomApplication selectByCode(String code){
        HibernateQuery query = getCustomApplicationDAO().getQuery();
        return query.where(QCustomApplication.customApplication.code.eq(code))
                .uniqueResult(QCustomApplication.customApplication);
    }
}
