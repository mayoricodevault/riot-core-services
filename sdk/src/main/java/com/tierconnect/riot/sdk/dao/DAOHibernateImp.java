package com.tierconnect.riot.sdk.dao;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import com.mysema.query.jpa.hibernate.HibernateDeleteClause;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.mysema.query.jpa.hibernate.HibernateUpdateClause;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.EntityPathBase;

/**
 * Created by agutierrez on 15-04-14.
 * Base DAO, you must inherit from it for use it
 */
public abstract class DAOHibernateImp<T, K extends Serializable> implements DAO<T, K> {
    static Logger logger = Logger.getLogger(DAOHibernateImp.class);

    private SessionFactory sessionFactory = HibernateSessionFactory.getInstance();

    public void setSessionFactory(SessionFactory sf) {
        this.sessionFactory = sf;
    }

    public SessionFactory getSessionFactory(){
    	return sessionFactory;
    }

    public Session getSession() {
        return getSessionFactory().getCurrentSession();
    }

    public abstract EntityPathBase<T> getEntityPathBase();

    public Class<T> getClazz() {
        return (Class<T>) getEntityPathBase().getType();
    }

    @Override
    public T selectById(K id) {
        return HibernateDAOUtils.selectById(getSession(), getClazz(), id);
    }

    @Override
    public T selectBy(String property, Object value) {
        return HibernateDAOUtils.selectBy(getSession(), getClazz(), property, value);
    }

    @Override
    public synchronized List<T> selectAllBy(String property, Object value) {
        return HibernateDAOUtils.selectAllBy(getSession(), getClazz(), property, value);
    }

    @Override
    public T selectBy(Map<String, Object> map) {
        return HibernateDAOUtils.selectBy(getSession(), getClazz(), map);
    }

	@Override
	public T selectBy(Map<String, Object> map, String property) {
		return HibernateDAOUtils.selectBy(getSession(), getClazz(), map, property);
	}

    @Override
    public List<T> selectAllBy(Map<String, Object> map) {
        return HibernateDAOUtils.selectAllBy(getSession(), getClazz(), map);
    }

    @Override
    public K insert(T object) {
        return HibernateDAOUtils.insert(getSession(), object);
    }

    @Override
    public K insertWithoutFlush(T object) {
        return HibernateDAOUtils.insertWithoutFlush(getSession(), object);
    }

    @Override
    public void update(T object) {
        HibernateDAOUtils.update(getSession(), object);
    }

    @Override
    public void updateWithoutFlush(T object) {
        HibernateDAOUtils.updateWithoutFlush(getSession(), object);
    }

    @Override
    public void delete(T object) {
        HibernateDAOUtils.delete(getSession(), object);
    }

    @Override
    public List<T> selectAll() {
        return HibernateDAOUtils.selectAll(getSession(), getClazz());
    }

    @Override
    public List<T> selectAllBy(Predicate be) {
        return HibernateDAOUtils.selectAllBy(getSession(), getEntityPathBase(), be);
    }

    @Override
    public T selectBy(Predicate be) {
        return HibernateDAOUtils.selectBy(getSession(), getEntityPathBase(), be);
    }

    @Override
    public void deleteAllBy(Predicate be) {
        HibernateDAOUtils.deleteAllBy(getSession(), getEntityPathBase(), be);
    }

    @Override
    public void deleteAll() {
        HibernateDAOUtils.deleteAll(getSession(), getEntityPathBase());
    }
    
    @Override
    public HibernateUpdateClause getUpdateQuery() {
        return new HibernateUpdateClause(getSession(), getEntityPathBase());
    }

    @Override
    public HibernateQuery getQuery() {
        final HibernateQuery hibernateQuery = new HibernateQuery(getSession());
        if (HibernateDAOUtils.enableQueryCacheForAllQueries) {
            hibernateQuery.setCacheable(true);
        }
        return hibernateQuery.from(getEntityPathBase());
    }

    @Override
    public HibernateDeleteClause getDeleteQuery() {
        return new HibernateDeleteClause(getSession(), getEntityPathBase());
    }
    
    @Override
    public Long countAll(Predicate be) {
    	return HibernateDAOUtils.countAll(getSession(), getEntityPathBase(), be);
    }
    
    @Override
    public List<T> selectAll(Predicate be, Pagination pagination,OrderSpecifier... orders) {
    	return HibernateDAOUtils.selectAll(getSession(), getEntityPathBase(), be, pagination,orders);
    }

}
