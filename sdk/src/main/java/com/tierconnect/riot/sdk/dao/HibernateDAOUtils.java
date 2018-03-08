package com.tierconnect.riot.sdk.dao;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

//import com.tierconnect.sdk.dao.CriteriaBuilder;
//import com.tierconnect.sdk.dao.ListMeta;
import com.mysema.query.types.path.ListPath;
import com.mysema.query.types.path.SetPath;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.hibernate.HibernateDeleteClause;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.EntityPathBase;

/**
 * Created by agutierrez on 15-04-14.
 */
public class HibernateDAOUtils {

    public static boolean enableQueryCacheForAllQueries = true;

    public static <T, K extends Serializable> T selectById(Session session, Class<T> clazz, K id) {
        return (T) session.get(clazz, id);
    }

    public static <T> T selectBy(Session session, Class<T> clazz, String property, Object value) {
        Criteria c1 = session.createCriteria(clazz);
        c1.add(Restrictions.eq(property, value));
        c1.setCacheable(true);
        return (T) c1.uniqueResult();
    }

    public static <T> List<T> selectAllBy(Session session, Class<T> clazz, String property, Object value) {
        Criteria c1 = session.createCriteria(clazz);
        c1.add(Restrictions.eq(property, value));
        c1.setCacheable(true);
        return (List<T>) c1.list();
    }

    public static <T> T selectBy(Session session, Class<T> clazz, Map<String, Object> map) {
        Criteria c1 = session.createCriteria(clazz);
        for (String key : map.keySet()) {
            c1.add(Restrictions.eq(key, map.get(key)));
        }
        c1.setCacheable(true);
        return (T) c1.uniqueResult();
    }


	public static <T> T selectBy(Session session, Class<T> clazz, Map<String, Object> map, String listProperty) {
		Criteria c1 = session.createCriteria(clazz);
		for (String key : map.keySet()) {
			c1.add(Restrictions.eq(key, map.get(key)));
		}
		c1.setFetchMode( listProperty, FetchMode.JOIN );
        c1.setCacheable(true);
		return (T) c1.uniqueResult();
	}


    public static <T> List<T> selectAllBy(Session session, Class<T> clazz, Map<String, Object> map) {
        Criteria c1 = session.createCriteria(clazz);
        for (String key : map.keySet()) {
            c1.add(Restrictions.eq(key, map.get(key)));
        }
        c1.setCacheable(true);
        return (List<T>) c1.list();
    }


    public static <T, K extends Serializable> K insert(Session session, T object) {
        session.save(object);
        K result = (K) session.getIdentifier(object);
        session.flush();
        return result;
    }

    public static <T, K extends Serializable> K insertWithoutFlush(Session session, T object) {
        session.save(object);
        K result = (K) session.getIdentifier(object);
        return result;
    }

    public static <T> void update(Session session, T object) {
        session.update(object);
        session.flush();
    }

    public static <T> void updateWithoutFlush(Session session, T object) {
        session.update(object);
    }

    public static <T> void delete(Session session, T object) {
        session.delete(object);
        session.flush();
    }

    public static <T> List<T> selectAll(Session session, Class<T> clazz) {
        Query query = session.createQuery("from " + clazz.getName());
        if (enableQueryCacheForAllQueries) {
            query.setCacheable(true);
        }
        return (List<T>) query.list();
    }

    public static <T> List<T> selectAllBy(Session session, EntityPathBase<T> entityPathBase, Predicate be) {
        JPQLQuery query = new HibernateQuery(session);
        query.from(entityPathBase);
        if (be != null) {
            query = query.where(be);
        }
        if (enableQueryCacheForAllQueries) {
            ((HibernateQuery) query).setCacheable(true);
        }
        return query.list(entityPathBase);
    }
    
    public static <T> List<T> selectAll(Session session, EntityPathBase<T> entityPathBase, Predicate be,Pagination pagination,OrderSpecifier... orders){

        return selectAll(session, entityPathBase, be, null, null, pagination, orders);
    }

    public static <T> List<T> selectAll(Session session,
                                        EntityPathBase<T> entityPathBase,
                                        Predicate be,
                                        List<EntityPathBase<?>> properties,
                                        ListPath<?, ?> leftJoinFetches,
                                        Pagination pagination,
                                        OrderSpecifier... orders) {

        JPQLQuery query = new HibernateQuery(session);
        query.from(entityPathBase);

        //add to query properties to fetch
        if(properties != null) {
            for (EntityPathBase<?> property : properties) {
                query.leftJoin(property).fetch();
            }
        }
        //add a collection property to fetch
        if(leftJoinFetches != null) {
            query.leftJoin(leftJoinFetches).fetch();
        }

        if (be != null) {
            query = query.where(be);
        }

        if(pagination != null){
            query = pagination.add(query);
        }

        if(orders != null){
            query = query.orderBy(orders);
        }
        if (enableQueryCacheForAllQueries) {
            ((HibernateQuery) query).setCacheable(true);
        }

        return query.list( entityPathBase );
    }

    /** TODO create one method tha trandsforms a set to a list **/
    public static <T> List<T> selectAllSet(Session session,
                                        EntityPathBase<T> entityPathBase,
                                        Predicate be,
                                        List<EntityPathBase<?>> properties,
                                        SetPath<?, ?> leftJoinFetches,
                                        Pagination pagination,
                                        OrderSpecifier... orders) {

        JPQLQuery query = new HibernateQuery(session);
        query.from(entityPathBase);

        //add to query properties to fetch
        if(properties != null) {
            for (EntityPathBase<?> property : properties) {
                query.leftJoin(property).fetch();
            }
        }
        //add a collection property to fetch
        if(leftJoinFetches != null) {
            query.leftJoin(leftJoinFetches).fetch();
        }

        if (be != null) {
            query = query.where(be);
        }

        if(pagination != null){
            query = pagination.add(query);
        }

        if(orders != null){
            query = query.orderBy(orders);
        }
        if (enableQueryCacheForAllQueries) {
            ((HibernateQuery) query).setCacheable(true);
        }

        return query.distinct().list( entityPathBase );
    }


    
    public static <T> Long countAll(Session session, EntityPathBase<T> entityPathBase, Predicate be) {
        JPQLQuery query = new HibernateQuery(session);
        query.from(entityPathBase);
        if (be != null) {
            query = query.where(be);
        }
        if (enableQueryCacheForAllQueries) {
            ((HibernateQuery) query).setCacheable(true);
        }
        return query.count();
    }

    public static <T> T selectBy(Session session, EntityPathBase<T> entityPathBase, Predicate be) {
        JPQLQuery query = new HibernateQuery(session);
        query.from(entityPathBase);
        if (be != null) {
            query = query.where(be);
        }
        if (enableQueryCacheForAllQueries) {
            ((HibernateQuery) query).setCacheable(true);
        }
        return query.uniqueResult(entityPathBase);
    }

    public static <T> void deleteAllBy(Session session, EntityPathBase<T> entityPathBase, Predicate be) {
        if (be == null) {
            throw new RuntimeException("Delete By Query should have a where parameter");
        }
        new HibernateDeleteClause(session, entityPathBase).where(be).execute();
    }

    public static <T> void deleteAll(Session session, EntityPathBase<T> entityPathBase) {
        new HibernateDeleteClause(session, entityPathBase).execute();
    }

    public static void rollback(Transaction transaction) {
        if (transaction != null && transaction.isActive()) {
            transaction.rollback();
        }
    }

    public static Query createQuery(Session session, String hqlQuery) {
        Query query = session.createQuery(hqlQuery);
        if (enableQueryCacheForAllQueries) {
            query.setCacheable(true);
        }
        return query;
    }

}
