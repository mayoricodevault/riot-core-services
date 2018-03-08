package com.tierconnect.riot.iot.dao;

import com.tierconnect.riot.iot.entities.MlModel;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import javax.annotation.Generated;
import java.util.List;
import java.util.Map;

@Generated("com.tierconnect.riot.appgen.service.GenDAO")
public class MlModelDAO extends MlModelDAOBase 
{

    @Override
    public List<MlModel> selectAllBy(Map<String, Object> map) {
        Criteria c1 = getSession().createCriteria(getClazz());

        for (String key : map.keySet()) {
            Object o =  map.get(key);
            String property = key;

            //with the criteria api we need to create an alias for nested objects
            if( "businessModel".equals(key)) {
                c1.createAlias("extraction", "e");
                property = "e.businessModel";
            }

            c1.add(Restrictions.eq(property, o));
        }
        c1.setCacheable(true);

        return (List<MlModel>) c1.list();
    }
}

