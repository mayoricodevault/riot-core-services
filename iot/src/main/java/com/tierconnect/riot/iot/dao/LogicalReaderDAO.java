package com.tierconnect.riot.iot.dao;

import com.mysema.query.jpa.JPQLQuery;
import com.tierconnect.riot.iot.entities.LogicalReader;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.QLogicalReader;

public class LogicalReaderDAO extends LogicalReaderDAOBase
{

    public boolean validateDuplicatedNameAndGroup (String name, long groupId)
    {
        JPQLQuery query = new HibernateQuery(getSession());
        QLogicalReader qLogicalReader = QLogicalReader.logicalReader;
        query.from(qLogicalReader).where(qLogicalReader.name.eq(name).and(qLogicalReader.group.id.eq(groupId)));
        Long constraintNum = query.count();
        boolean response = (null != constraintNum && constraintNum > 0)? false : true;
        return response;
    }

    public boolean validateDuplicatedCodeAndGroup (String code, long groupId)
    {
        JPQLQuery query = new HibernateQuery(getSession());
        QLogicalReader qLogicalReader = QLogicalReader.logicalReader;
        query.from(qLogicalReader).where(qLogicalReader.code.eq(code).and(qLogicalReader.group.id.eq(groupId)));
        Long constraintNum = query.count();
        boolean response = (null != constraintNum && constraintNum > 0)? false : true;
        return response;
    }

}

