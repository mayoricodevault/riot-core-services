package com.tierconnect.riot.sdk.dao;

import com.mysema.query.dml.DeleteClause;
import com.mysema.query.dml.UpdateClause;
import com.mysema.query.jpa.JPAQueryBase;
import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.jpa.hibernate.HibernateDeleteClause;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.mysema.query.jpa.hibernate.HibernateUpdateClause;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.EntityPathBase;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class GenericDAOHibernateImp<T, K extends Serializable> implements GenericDAO<T, K> {
    static Logger logger = Logger.getLogger(GenericDAOHibernateImp.class);

    SessionFactory sessionFactory;

    public void setSessionFactory(SessionFactory sf) {
        this.sessionFactory = sf;
    }

    public SessionFactory getSessionFactory(){
        if(sessionFactory == null){
            this.sessionFactory = HibernateSessionFactory.getInstance();
        }
        return this.sessionFactory;
    }

    public Session getSession() {
        return getSessionFactory().getCurrentSession();
    }

    @Override
    public T selectById(Class<T> clazz, K id) {
        return (T) getSession().get(clazz, id);
    }

    @Override
    public T selectBy(Class<T> clazz, String property, Object value) {
        return HibernateDAOUtils.selectBy(getSession(), clazz, property, value);
    }

    @Override
    public List<T> selectAllBy(Class<T> clazz, String property, Object value) {
        return HibernateDAOUtils.selectAllBy(getSession(), clazz, property, value);
    }

    @Override
    public T selectBy(Class<T> clazz, Map<String, Object> map) {
        return HibernateDAOUtils.selectBy(getSession(), clazz, map);
    }

    @Override
    public List<T> selectAllBy(Class<T> clazz, Map<String, Object> map) {
        return HibernateDAOUtils.selectAllBy(getSession(), clazz, map);
    }

    @Override
    public List<T> selectAll(Class<T> clazz) {
        return HibernateDAOUtils.selectAll(getSession(), clazz);
    }

    @Override
    public K insert(T object) {
        return HibernateDAOUtils.insert(getSession(), object);
    }

    @Override
    public void update(T object) {
        HibernateDAOUtils.update(getSession(), object);
    }

    @Override
    public void delete(T object) {
        HibernateDAOUtils.delete(getSession(), object);
    }

//    public List<T> selectAllBy(Class<T> clazz, ListMeta listMeta) {
//        return HibernateDAOUtils.selectAllBy(getSession(), clazz, listMeta);
//    }

    @Override
    public List<T> selectAllBy(EntityPathBase<T> entityPathBase, Predicate be) {
        return HibernateDAOUtils.selectAllBy(getSession(), entityPathBase, be);
    }

    @Override
    public T selectBy(EntityPathBase<T> entityPathBase, Predicate be) {
        return HibernateDAOUtils.selectBy(getSession(), entityPathBase, be);
    }

    @Override
    public void deleteAllBy(EntityPathBase<T> entityPathBase, Predicate be) {
        HibernateDAOUtils.deleteAllBy(getSession(), entityPathBase, be);
    }

    @Override
    public HibernateQuery getQuery(EntityPathBase<T> entityPathBase) {
        return new HibernateQuery(getSession()).from(entityPathBase);
    }

    @Override
    public DeleteClause getDeleteQuery(EntityPathBase<T> entityPathBase) {
        return new HibernateDeleteClause(getSession(), entityPathBase);
    }

    @Override
    public UpdateClause getUpdateQuery(EntityPathBase<T> entityPathBase) {
        return new HibernateUpdateClause(getSession(), entityPathBase);
    }

}
