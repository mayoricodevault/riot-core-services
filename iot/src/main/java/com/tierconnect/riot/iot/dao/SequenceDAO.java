package com.tierconnect.riot.iot.dao;

import com.tierconnect.riot.iot.entities.Sequence;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by fflores on 8/12/2015.
 */
public class SequenceDAO extends SequenceDAOBase {

    static Logger logger = Logger.getLogger( SequenceDAO.class );

    public Map<String, Object> sequenceContext;

    private SequenceDAO()
    {
    }

    public static SequenceDAO getInstance(){
        return instance;
    }

    private static SequenceDAO instance = new SequenceDAO();

    public void initSequences (){
        sequenceContext = new HashMap<String, Object>();

        // get sequences from DB
        Session session;
        try {
            session = HibernateSessionFactory.getInstance().getCurrentSession();
            Transaction transaction = session.getTransaction();
            transaction.begin();
            List<Sequence> sequences = selectAll();
            if (null != sequences && !sequences.isEmpty()){
                for (Sequence sequence : sequences){
                    loadSequence(sequence.getThingTypeField(), sequence.getCurrentValue(), false);
                }
            }
            transaction.commit();
        } catch (Exception ex) {
            System.err.println("Hibernate validation Error " + ex.getMessage());
            logger.error("Hibernate validation Error", ex);
            throw ex;
        }
    }

    public void loadSequence (ThingTypeField thingTypeField, Long initialValue, boolean createAtDB){
        AtomicLong sequenceObject = null;
        try {
            if (null == thingTypeField.getId()){
                throw new ForbiddenException("thingTypeFieldId cannot be null");
            }

            StringBuilder sb = new StringBuilder();
            sb.append("seq_");
            sb.append(thingTypeField.getId());

            // persist sequence at DB
            if (createAtDB) {
                if (null != selectBy("name", sb.toString()) ){
                    throw new ForbiddenException(sb.toString() + " sequence for thingTypeField already exists");
                }
                Sequence sequence = new Sequence();
                sequence.setThingTypeField(thingTypeField);
                sequence.setInitialValue(initialValue);
                sequence.setCurrentValue(initialValue-1);
                sequence.setName(sb.toString());
                insert(sequence);
                // create sequence by reflection
                sequenceObject = AtomicLong.class.getConstructor(long.class).newInstance(initialValue-1);
            } else {
                // create sequence by reflection
                sequenceObject = AtomicLong.class.getConstructor(long.class).newInstance(initialValue);
            }

            // put sequence in memory
            sequenceContext.put(sb.toString(), sequenceObject);
            logger.info(sb.toString() + " sequence has been created with initial value: " + initialValue);

        } catch (NoSuchMethodException nsme){
            nsme.printStackTrace();
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        } catch (InstantiationException ie) {
            ie.printStackTrace();
        } catch (InvocationTargetException ite) {
            ite.printStackTrace();
        }
    }

    public Long incrementAndGetSequence (long thingTypeFieldId){
        Long result = null;
        if ( null != sequenceContext){
            StringBuilder sb = new StringBuilder();
            sb.append("seq_");
            sb.append(thingTypeFieldId);
            if ( sequenceContext.get(sb.toString()) instanceof AtomicLong ){
                Sequence sequence = selectBy("thingTypeField.id",thingTypeFieldId);
                // generate sequence next value
                result = ( (AtomicLong) sequenceContext.get(sb.toString()) ).incrementAndGet();
                sequence.setCurrentValue(result);
                update(sequence);
            }
        }
        return result;
    }

    public void removeSequence (ThingTypeField thingTypeField){
        StringBuilder sb = new StringBuilder();
        sb.append("seq_");
        sb.append(thingTypeField.getId());
        String sequenceName = sb.toString();

        if (null != sequenceContext.get(sequenceName)){
            // remove sequence from memory
            sequenceContext.remove(sequenceName);
            // remove sequence from DB
            Sequence sequence = selectBy("name", sequenceName);
            delete(sequence);
            logger.info(sb.toString() + " sequence has been removed");
        }
    }

    public Long decrementAndGetSequence (long thingTypeFieldId){
        Long result = null;
        if ( null != sequenceContext){
            StringBuilder sb = new StringBuilder();
            sb.append("seq_");
            sb.append(thingTypeFieldId);
            if ( sequenceContext.get(sb.toString()) instanceof AtomicLong ){
                Sequence sequence = selectBy("thingTypeField.id",thingTypeFieldId);
                // decrement sequence
                result = ( (AtomicLong) sequenceContext.get(sb.toString()) ).decrementAndGet();
                sequence.setCurrentValue(result);
                update(sequence);
            }
        }
        return result;
    }

}
