package com.tierconnect.riot.sdk.dao;

import com.mysema.query.dml.DeleteClause;
import com.mysema.query.dml.UpdateClause;
import com.mysema.query.jpa.impl.JPADeleteClause;
import com.mysema.query.jpa.impl.JPAQuery;
import com.mysema.query.jpa.impl.JPAUpdateClause;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.EntityPathBase;
import org.hibernate.Session;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * Created by agutierrez on 15-04-14.
 * Base DAO, you must inherit from it for use it
 */
public abstract class DAOJPAImp<T, K extends Serializable> implements DAO<T, K> {

    EntityManager entityManager;

    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public abstract EntityPathBase<T> getEntityPathBase();

    public Class<T> getClazz() {
        return (Class<T>) getEntityPathBase().getType();
    }

    public Session getSession() {
        return entityManager.unwrap(Session.class);
    }

    public Connection getConnection() {
        return entityManager.unwrap(java.sql.Connection.class);
    }

    @Override
    public T selectById(K id) {
        return JPADAOUtils.selectById(entityManager, getClazz(), id);
    }

    @Override
    public T selectBy(String property, Object value) {
        return JPADAOUtils.selectBy(entityManager, getClazz(), property, value);
    }

    @Override
    public List<T> selectAllBy(String property, Object value) {
        return JPADAOUtils.selectAllBy(entityManager, getClazz(), property, value);
    }

    @Override
    public T selectBy(Map<String, Object> map) {
        return JPADAOUtils.selectBy(entityManager, getClazz(), map);
    }

    @Override
    public List<T> selectAllBy(Map<String, Object> map) {
        return JPADAOUtils.selectAllBy(entityManager, getClazz(), map);
    }

    @Override
    public List<T> selectAll() {
        return JPADAOUtils.selectAll(entityManager, getClazz());
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

    @Override
    public List<T> selectAllBy(Predicate be) {
        return JPADAOUtils.selectAllBy(entityManager, getEntityPathBase(), be);
    }

    @Override
    public T selectBy(Predicate be) {
        return JPADAOUtils.selectBy(entityManager, getEntityPathBase(), be);
    }

    @Override
    public void deleteAllBy(Predicate be) {
        JPADAOUtils.deleteAllBy(entityManager, getEntityPathBase(), be);
    }

    @Override
    public JPAQuery getQuery() {
        return new JPAQuery(entityManager).from(getEntityPathBase());
    }

    @Override
    public DeleteClause getDeleteQuery() {
        return new JPADeleteClause(entityManager, getEntityPathBase());
    }

    @Override
    public UpdateClause getUpdateQuery() {
        return new JPAUpdateClause(entityManager, getEntityPathBase());
    }

}
