package com.tierconnect.riot.iot.dao;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.QParameters;
import com.tierconnect.riot.iot.entities.QThing;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : rchirinos
 * @date : 9/14/16 2:49 PM
 * @version:
 */
public class ParametersDAO extends ParametersDAOBase {

    /**
     * Check if Category and code already exists in Parameters
     * @param category
     * @param code
     * @return
     */
    public boolean existsParameter(String category, String code)
    {
        JPQLQuery query = new HibernateQuery(getSession());
        QParameters qParameters = QParameters.parameters;
        query.from(qParameters).where(qParameters.category.eq(category).and(qParameters.code.eq(code)));
        Long constraintNum = query.count();
        boolean response = (null != constraintNum && constraintNum > 0)? true : false;
        return response;
    }
}
