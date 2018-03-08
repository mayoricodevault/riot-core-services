package com.tierconnect.riot.appcore.services;

import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.entities.QConnectionType;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;

import javax.annotation.Generated;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class ConnectionTypeService extends ConnectionTypeServiceBase 
{

    public ConnectionType getConnectionTypeByCode(String code) {
        HibernateQuery query = getConnectionTypeDAO().getQuery();
        return query.where( QConnectionType.connectionType.code.eq(code)).uniqueResult( QConnectionType.connectionType);
    }

}

