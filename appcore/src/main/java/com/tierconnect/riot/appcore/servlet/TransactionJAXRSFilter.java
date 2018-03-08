package com.tierconnect.riot.appcore.servlet;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import com.tierconnect.riot.commons.entities.IThing;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.Transaction;

//import com.mchange.v2.c3p0.C3P0Registry;
//import com.mchange.v2.c3p0.PooledDataSource;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;
import org.jboss.resteasy.core.interception.ResponseContainerRequestContext;
import org.jboss.resteasy.plugins.server.servlet.HttpServletInputMessage;

@Provider
public class TransactionJAXRSFilter implements ContainerRequestFilter, ContainerResponseFilter {
    static Logger logger = Logger.getLogger(TransactionJAXRSFilter.class);

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        before(requestContext);
    }

    @Override
    public void filter(ContainerRequestContext arg0,
                       ContainerResponseContext arg1) throws IOException {
        after(arg0, arg1);
    }

    public static void before(ContainerRequestContext requestContext)  throws IOException {
        logger.debug("AGG 1.START TransactionFilter{");
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
//        if (transaction.isActive()) {
//            transaction.rollback();
//            session = session.getSessionFactory().openSession(); //special case
//            transaction = session.getTransaction();
//        }
        try {
            transaction.begin();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
    }

    public static void after(ContainerRequestContext requestContext,ContainerResponseContext responseContext)  throws IOException {
        logger.debug("AGG 1.END TransactionFilter}");
        int status = responseContext.getStatus();
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();

        if (status >= 200 && status <= 299) {
            boolean isCommited = false;
            try {
                if (transaction.isActive()){
                    transaction.commit();
                } else {
                    logger.info("Transaction is not active [" + ((HttpServletInputMessage) ((ResponseContainerRequestContext) requestContext).getHttpRequest()).getHttpMethod() + "] " +
                            URLDecoder.decode(((HttpServletInputMessage) ((ResponseContainerRequestContext) requestContext).getHttpRequest()).getUri().getRequestUri().toString(), "UTF-8"));
                }
                isCommited = true;
            } catch (StaleObjectStateException e){
                logger.warn("Rollback due to concurrent modification on Recent Element");
                HibernateDAOUtils.rollback(transaction);
            } catch (Exception ex) {
                logger.error("COMMIT error on: " + requestContext.getMethod() + " " + requestContext.getUriInfo().getRequestUri(), ex);
                HibernateDAOUtils.rollback(transaction);
            }

            if(isCommited){
                try {
                    Class clazz = null;
                    Object obj = null;
                    clazz = Class.forName("com.tierconnect.riot.iot.services.BrokerClientHelper");
                    obj = clazz
                            .getMethod("publishTickle", String.class, String.class)
                            .invoke(null, Thread.currentThread().getName(), null);
                    if(obj != null){
                        logger.info("Tickles sent:\n" + obj);
                    }
                } catch (Exception ex) {
                    logger.error("Error occurred while publishing tickle", ex);
                }
            } else {
                try{
                    logger.info("Transaction is not committed, skipping sending tickles");
                    logger.info("Trying to remove tickles...");
                    Class.forName("com.tierconnect.riot.iot.services.BrokerClientHelper")
                            .getMethod("removeMessage", String.class)
                            .invoke(null, Thread.currentThread().getName());
                    logger.info("Tickle removed");
                } catch (Exception ex) {
                    logger.error("Error occurred while removing tickle", ex);
                }
            }


        } else {
            HibernateDAOUtils.rollback(transaction);
        }
//        try {
//            session.close();
//            //Closed manually created session with openSession (special case)
//        } catch (Exception ex) {
//            //Typically this should throw an error for sessions not created with openSession
//        }
    }

}
