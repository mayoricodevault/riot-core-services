package com.tierconnect.riot.sdk.dao;

import com.mysema.query.dml.DeleteClause;
import com.mysema.query.dml.UpdateClause;
import com.mysema.query.jpa.JPAQueryBase;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.EntityPathBase;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface GenericDAO<T, K extends Serializable> {
    public T selectById(Class<T> clazz, K id);

    public T selectBy(Class<T> clazz, String property, Object value);

    public List<T> selectAllBy(Class<T> clazz, String property, Object value);

    public T selectBy(Class<T> clazz, Map<String, Object> map);

    public List<T> selectAllBy(Class<T> clazz, Map<String, Object> map);

    public List<T> selectAll(Class<T> clazz);

    public K insert(T object);

    public void update(T object);

    public void delete(T object);

//	public List<T> selectAllBy(Class<T> clazz, ListMeta listMeta);

    //QueryDSL
    public List<T> selectAllBy(EntityPathBase<T> clazz, Predicate be);

    public T selectBy(EntityPathBase<T> entityPathBase, Predicate be);

    public void deleteAllBy(EntityPathBase<T> entityPathBase, Predicate be);

    public JPAQueryBase getQuery(EntityPathBase<T> entityPathBase);

    public DeleteClause getDeleteQuery(EntityPathBase<T> entityPathBase);

    public UpdateClause getUpdateQuery(EntityPathBase<T> entityPathBase);

}
