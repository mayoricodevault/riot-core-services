package com.tierconnect.riot.iot.services;

import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.iot.dao.SequenceDAO;
import com.tierconnect.riot.iot.entities.QSequence;
import com.tierconnect.riot.iot.entities.Sequence;
import com.tierconnect.riot.sdk.dao.Pagination;

import java.util.List;

/**
 * Created by fflores on 8/12/2015.
 */
public class SequenceService {
    static private SequenceDAO _sequenceDAO;

    public static SequenceDAO getSequenceDAO()
    {
        if( _sequenceDAO == null )
        {
            _sequenceDAO = SequenceDAO.getInstance();
        }
        return _sequenceDAO;
    }
    static private SequenceService INSTANCE = new SequenceService();

    public static SequenceService getInstance()
    {
        return INSTANCE;
    }

    public Sequence get( Long id )
    {
        Sequence sequence = getSequenceDAO().selectById( id );
        return sequence;
    }


    public Sequence insert( Sequence sequence )
    {
        validateInsert( sequence );
        Long id = getSequenceDAO().insert( sequence );
        sequence.setId( id );
        return sequence;
    }

    public void validateInsert( Sequence sequence )
    {

    }



    public Sequence update( Sequence sequence )
    {
        validateUpdate( sequence );
        getSequenceDAO().update( sequence );
        return sequence;
    }

    public void validateUpdate( Sequence sequence )
    {

    }



    public void delete( Sequence sequence )
    {
        validateDelete( sequence );
        getSequenceDAO().delete( sequence );
    }

    public void validateDelete( Sequence sequence )
    {

    }


    public List<Sequence> listPaginated( Pagination pagination, String orderString )
    {
        OrderSpecifier orderSpecifiers[] = QueryUtils.getOrderFields(QSequence.sequence, orderString);
        return getSequenceDAO().selectAll( null, pagination, orderSpecifiers );
    }

    public long countList( Predicate be )
    {
        return getSequenceDAO().countAll( be );
    }

    public List<Sequence> listPaginated( Predicate be, Pagination pagination, String orderString )
    {
        OrderSpecifier orderSpecifiers[] = QueryUtils.getOrderFields( QSequence.sequence, orderString );
        return getSequenceDAO().selectAll( be, pagination, orderSpecifiers );
    }
}
