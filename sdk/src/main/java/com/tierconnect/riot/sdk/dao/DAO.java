package com.tierconnect.riot.sdk.dao;

import com.mysema.query.dml.DeleteClause;
import com.mysema.query.dml.UpdateClause;
import com.mysema.query.jpa.JPAQueryBase;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.EntityPathBase;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.hibernate.Session;

/**
 * Created by agutierrez on 15-04-14.
 */
public interface DAO<T, K extends Serializable> {
    public T selectById(K id);

    public T selectBy(String property, Object value);

    public List<T> selectAllBy(String property, Object value);

    public T selectBy(Map<String, Object> map);

	public T selectBy(Map<String, Object> map, String fetchProperty);

    public List<T> selectAllBy(Map<String, Object> map);

    public List<T> selectAll();

    public K insert(T object);

    public K insertWithoutFlush(T object);

    public void update(T object);

    public void updateWithoutFlush(T object);

    public void delete(T object);

    //QueryDSL
    public List<T> selectAllBy(Predicate be);

    public T selectBy(Predicate be);

    public void deleteAllBy(Predicate be);
    
    public void deleteAll();

    public JPAQueryBase getQuery();

    public DeleteClause getDeleteQuery();

    public UpdateClause getUpdateQuery();
    
    
    public Long countAll(Predicate be);
    public <T> List<T> selectAll(Predicate be,Pagination pagination,OrderSpecifier... orders);

}
