package com.tierconnect.riot.iot.services;

import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.CustomField;
import com.tierconnect.riot.iot.entities.QCustomField;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import java.util.List;

/**
 * Created by cfernandez
 * 10/24/2014.
 */
public class CustomFieldService extends CustomFieldServiceBase {

    public List<CustomField> selectById(Long id){

        Session session = getCustomFieldDAO().getSession();

        List<CustomField> customFields = session.createCriteria(CustomField.class)
                .setFetchMode("customFieldType", FetchMode.EAGER)
                /*.setFetchMode("customObjectType", FetchMode.LAZY)
                .setFetchMode("thingField", FetchMode.LAZY)
                .setFetchMode("lookupObject", FetchMode.LAZY)*/
                .add(Restrictions.eq("id",id))
                .list();

        return customFields;
    }

    public CustomField selectByCode(String code){
        HibernateQuery query = getCustomFieldDAO().getQuery();
        return query.where(QCustomField.customField.code.eq(code))
                .uniqueResult(QCustomField.customField);
    }

    public CustomField selectByCodeAndBusinessObj(String code, Long businessObjTypeId){
        HibernateQuery query = getCustomFieldDAO().getQuery();
        return query.where(QCustomField.customField.code.eq(code).and(QCustomField.customField.customObjectType.id.eq(businessObjTypeId)))
                .uniqueResult(QCustomField.customField);
    }
}
