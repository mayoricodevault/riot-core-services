package com.tierconnect.riot.iot.dao;


import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.transform.Transformers;
import org.hibernate.type.StandardBasicTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author garivera
 *         Thing Type class
 */
@SuppressWarnings({"SqlDialectInspection", "unchecked"})
public class ThingTypeDAO extends ThingTypeDAOBase {

    public List<String> getDirectionPath(Long thingTypeIDOrigin, Long thingTypeIDDestiny) {
        List<String> paths = new ArrayList<>();
        Query query = HibernateSessionFactory.getInstance().getCurrentSession()
                .createSQLQuery("SELECT CONCAT(t1.thingTypeParentId, ',', " +
                        "              coalesce(t1.thingTypeChildId, 'null'), ',', " +
                        "              coalesce(t2.thingTypeChildId, 'null'), ',', " +
                        "              coalesce(t3.thingTypeChildId, 'null'), ',', " +
                        "              coalesce(t4.thingTypeChildId, 'null'), ',', " +
                        "              coalesce(t5.thingTypeParentId, 'null') " +
                        "       ) AS path " +
                        "FROM thingtypedirectionmap AS t1 " +
                        "  LEFT JOIN thingtypedirectionmap AS t2 ON t2.thingTypeParentId = t1.thingTypeChildId " +
                        "  LEFT JOIN thingtypedirectionmap AS t3 ON t3.thingTypeParentId = t2.thingTypeChildId " +
                        "  LEFT JOIN thingtypedirectionmap AS t4 ON t4.thingTypeParentId = t3.thingTypeChildId " +
                        "  LEFT JOIN thingtypedirectionmap AS t5 ON t5.thingTypeParentId = t4.thingTypeChildId " +
                        "WHERE t1.thingTypeParentId = (:thingTypeIDOrigin);");
        query.setParameter("thingTypeIDOrigin", thingTypeIDOrigin);
        List list = query.list();
        for (Object value : list) {
            if (Arrays.asList(StringUtils.split(String.valueOf(value), ","))
                    .contains(String.valueOf(thingTypeIDDestiny))) {
                paths.add(String.valueOf(value));
            }
        }
        return list;
    }

    /**
     * get list of thing types direction map.
     *
     * @return a instance of {@link List}<{@link ThingTypeDAO}>
     */
    public List<ThingTypeDirectionMap> getDirectedGraph(List<Long> groupIds) {
        Query query = HibernateSessionFactory
                .getInstance()
                .getCurrentSession()
                .createSQLQuery("select * " +
                        "from thingtypedirectionmap " +
                        "where thingTypeParentGroupId in (:groupIds) " +
                        "and thingTypeChildGroupId in (:groupIds)");
        query.setParameterList("groupIds", groupIds);
        query.setResultTransformer(Transformers.aliasToBean(ThingTypeDirectionMap.class));
        return query.list();
    }

    /**
     * get list of thing types direction map.
     *
     * @return a instance of {@link List}<{@link ThingTypeDAO}>
     */
    public List<ThingTypeDirectionMap> getDirectedGraph() {
        Query query = HibernateSessionFactory
                .getInstance()
                .getCurrentSession()
                .createSQLQuery("select * from thingtypedirectionmap");
        query.setResultTransformer(Transformers.aliasToBean(ThingTypeDirectionMap.class));
        return query.list();
    }


    /**
     * Get all parents by sql.
     *
     * @return A instance of {@link List}<{@link Long}> that contains all parents.
     */
    public Long[] getAllParents() {
        Query query = HibernateSessionFactory
                .getInstance()
                .getCurrentSession()
                .createSQLQuery("select tt.id as thingTypeId " +
                        "from thingtype tt " +
                        "left join thingtypedirectionmap ttm " +
                        "on (tt.id = ttm.thingTypeChildId) " +
                        "where ttm.thingTypeChildId is null")
                .addScalar("thingTypeId", StandardBasicTypes.LONG);
        List parentList = query.list();
        return (Long[]) parentList.toArray(new Long[parentList.size()]);
    }

    /**
     * get all thing types in a list of ids.
     *
     * @param thingTypeIds A list of ids to find.
     * @return a Instance of {@link List}<{@link ThingType}>. If the list is null return null.
     */
    public List<ThingType> getThingTypeIn(List<Long> thingTypeIds) {
        if (thingTypeIds == null || thingTypeIds.isEmpty()) {
            return null;
        }
        JPQLQuery jpqlQuery = new HibernateQuery(getSession());
        QThingType qThingType = QThingType.thingType;
        jpqlQuery.from(qThingType);
        jpqlQuery.where(qThingType.id.in(thingTypeIds));
        return jpqlQuery.list(QThingType.thingType);
    }
}
