package com.tierconnect.riot.sdk.dao;

import com.mysema.query.dml.DeleteClause;
import com.mysema.query.dml.UpdateClause;
import com.mysema.query.jpa.impl.JPADeleteClause;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.jpa.impl.JPAUpdateClause;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.EntityPathBase;
import org.apache.log4j.Logger;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Created by agutierrez on 15-04-14.
 */
public class GenericDAOJPAImp<T, K extends Serializable> implements GenericDAO<T, K> {
    static Logger logger = Logger.getLogger(GenericDAOHibernateImp.class);

    EntityManager entityManager;

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<T> selectAllBy(EntityPathBase<T> entityPathBase, Predicate be) {
        return JPADAOUtils.selectAllBy(entityManager, entityPathBase, be);
    }

    @Override
    public T selectBy(EntityPathBase<T> entityPathBase, Predicate be) {
        return JPADAOUtils.selectBy(entityManager, entityPathBase, be);
    }

    @Override
    public void deleteAllBy(EntityPathBase<T> entityPathBase, Predicate be) {
        JPADAOUtils.deleteAllBy(entityManager, entityPathBase, be);
    }

    @Override
    public JPAQuery getQuery(EntityPathBase<T> entityPathBase) {
        return new JPAQuery(entityManager).from(entityPathBase);
    }

    @Override
    public DeleteClause getDeleteQuery(EntityPathBase<T> entityPathBase) {
        return new JPADeleteClause(entityManager, entityPathBase);
    }

    @Override
    public UpdateClause getUpdateQuery(EntityPathBase<T> entityPathBase) {
        return new JPAUpdateClause(entityManager, entityPathBase);
    }

    @Override
    public T selectById(Class<T> clazz, K id) {
        return JPADAOUtils.selectById(entityManager, clazz, id);
    }

    @Override
    public T selectBy(Class<T> clazz, String property, Object value) {
        return JPADAOUtils.selectBy(entityManager, clazz, property, value);
    }

    @Override
    public List<T> selectAllBy(Class<T> clazz, String property, Object value) {
        return JPADAOUtils.selectAllBy(entityManager, clazz, property, value);
    }

    @Override
    public T selectBy(Class<T> clazz, Map<String, Object> map) {
        return JPADAOUtils.selectBy(entityManager, clazz, map);
    }

    @Override
    public List<T> selectAllBy(Class<T> clazz, Map<String, Object> map) {
        return JPADAOUtils.selectAllBy(entityManager, clazz, map);
    }

    @Override
    public List<T> selectAll(Class<T> clazz) {
        return JPADAOUtils.selectAll(entityManager, clazz);
    }

    @Override
    public K insert(T object) {
        return JPADAOUtils.insert(entityManager, object);
    }

    @Override
    public void update(T object) {
        JPADAOUtils.update(entityManager, object);
    }

    @Override
    public void delete(T object) {
        JPADAOUtils.delete(entityManager, object);
    }
}
