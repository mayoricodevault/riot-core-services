package com.tierconnect.riot.sdk.dao;

import com.mysema.query.jpa.impl.JPADeleteClause;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.EntityPathBase;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by agutierrez on 15-04-14.
 */
public class JPADAOUtils {
    private static <T> String getEntityName(Class<T> clazz) {
        return clazz.getSimpleName();
    }

    public static <T, K extends Serializable> T selectById(EntityManager entityManager, Class<T> clazz, K id) {
        return entityManager.find(clazz, id);
    }

    public static <T> T selectBy(EntityManager entityManager, Class<T> clazz, String property, Object value) {
        String qs = "SELECT x FROM " + getEntityName(clazz) + " x WHERE x." + property + " =:" + property;
        Query q = entityManager.createQuery(qs);
        q.setParameter(property, value);
        return (T) q.getSingleResult();
    }


    public static <T> List<T> selectAllBy(EntityManager entityManager, Class<T> clazz, String property, Object value) {
        String qs = "SELECT x FROM " + getEntityName(clazz) + " x WHERE x." + property + " =:" + property;
        Query q = entityManager.createQuery(qs);
        q.setParameter(property, value);
        return (List<T>) q.getResultList();
    }

    public static <T> T selectBy(EntityManager entityManager, Class<T> clazz, Map<String, Object> map) {
        StringBuilder qs = new StringBuilder("SELECT x FROM " + getEntityName(clazz) + " x ");
        if (map.size() == 0) {
            return null;
        }
        qs.append(" WHERE ");
        for (Map.Entry<String, Object> mapEntry : map.entrySet()) {
            qs.append(" x." + mapEntry.getKey() + " =:" + mapEntry.getKey());
        }
        Query q = entityManager.createQuery(qs.toString());
        for (Map.Entry<String, Object> mapEntry : map.entrySet()) {
            q.setParameter(mapEntry.getKey(), mapEntry.getValue());
        }
        return (T) q.getSingleResult();
    }

    public static <T> List<T> selectAllBy(EntityManager entityManager, Class<T> clazz, Map<String, Object> map) {
        StringBuilder qs = new StringBuilder("SELECT x FROM " + getEntityName(clazz) + " x ");
        if (map.size() == 0) {
            return null;
        }
        qs.append(" WHERE ");
        for (Map.Entry<String, Object> mapEntry : map.entrySet()) {
            qs.append(" x." + mapEntry.getKey() + " =:" + mapEntry.getKey());
        }
        Query q = entityManager.createQuery(qs.toString());
        for (Map.Entry<String, Object> mapEntry : map.entrySet()) {
            q.setParameter(mapEntry.getKey(), mapEntry.getValue());
        }
        return (List<T>) q.getResultList();
    }


    public static <T> List<T> selectAll(EntityManager entityManager, Class<T> clazz) {
        return entityManager.createQuery("SELECT x FROM " + getEntityName(clazz) + " x").getResultList();
    }

    public static <K extends Serializable, T> K insert(EntityManager entityManager, T object) {
        entityManager.persist(object);
        entityManager.flush();
        return (K) entityManager.getEntityManagerFactory().getPersistenceUnitUtil().getIdentifier(object);
    }

    public static <T> void update(EntityManager entityManager, T object) {
        entityManager.merge(object);
    }

    public static <T> void delete(EntityManager entityManager, T object) {
        entityManager.remove(entityManager.merge(object));
    }

//    public static <T> List<T> selectAllBy(EntityManager entityManager, Class<T> clazz, Predicate be) {
//        String entityName = clazz.getSimpleName().substring(0, 1).toLowerCase() + clazz.getSimpleName().substring(1);
//        PathBuilder<T> entityPath = new PathBuilder<T>(clazz, entityName);
//        return selectAllBy(entityManager, entityPath, be);
//    }

    public static <T> List<T> selectAllBy(EntityManager entityManager, EntityPathBase<T> entityPathBase, Predicate be) {
        JPAQuery query = new JPAQuery(entityManager);
        query.from(entityPathBase);
        if (be != null) {
            query = query.where(be);
        }
        return query.list(entityPathBase);
    }

//    public static <T> T selectBy(EntityManager entityManager, Class<T> clazz, Predicate be) {
//        String entityName = clazz.getSimpleName().substring(0, 1).toLowerCase() + clazz.getSimpleName().substring(1);
//        PathBuilder<T> entityPath = new PathBuilder<T>(clazz, entityName);
//        return selectBy(entityManager, entityPath, be);
//    }

    public static <T> T selectBy(EntityManager entityManager, EntityPathBase<T> entityPathBase, Predicate be) {
        JPAQuery query = new JPAQuery(entityManager);
        query.from(entityPathBase);
        if (be != null) {
            query = query.where(be);
        }
        return query.uniqueResult(entityPathBase);
    }


    public static <T> void deleteAllBy(EntityManager entityManager, EntityPathBase<T> entityPathBase, Predicate be) {
        if (be == null) {
            throw new RuntimeException("Delete By Query should have a where parameter");
        }
        JPADeleteClause deleteQuery = new JPADeleteClause(entityManager, entityPathBase);
        deleteQuery = deleteQuery.where(be);
        deleteQuery.execute();
    }
}
