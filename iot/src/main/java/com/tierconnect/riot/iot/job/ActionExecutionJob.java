package com.tierconnect.riot.iot.job;

import com.tierconnect.riot.iot.entities.LogExecutionAction;
import com.tierconnect.riot.iot.services.LogExecutionActionService;
import com.tierconnect.riot.iot.utils.rest.ActionHTTP;
import com.tierconnect.riot.iot.utils.rest.ExecuteActionException;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Date;
import java.util.Map;

/**
 * Created by vealaro on 3/31/17.
 */
public class ActionExecutionJob implements Runnable {

    private static Logger logger = Logger.getLogger(ActionExecutionJob.class);

    private Long logExecutionId;
    private String body;
    private Map<String, Object> mapConfiguration;
    private LogExecutionActionService actionService;

    public ActionExecutionJob(Long logExecutionId, String body, Map<String, Object> mapConfiguration) {
        this.logExecutionId = logExecutionId;
        this.body = body;
        this.mapConfiguration = mapConfiguration;
    }

    @Override
    public void run() {
        logger.info("Start call action http");
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        ActionHTTP actionHTTP = new ActionHTTP(mapConfiguration, body);
        LogExecutionAction logExecutionAction = null;
        long start = 0L;
        try {
            transaction.begin();
            actionService = LogExecutionActionService.getInstance();
            logExecutionAction = actionService.get(logExecutionId);
            start = System.currentTimeMillis();
            actionHTTP.executePost();
            long duration = System.currentTimeMillis() - start;
            logger.info("duration call action HTTP " + duration + " ms");
            updateLog(logExecutionAction, actionHTTP, duration); // update log
        } catch (ExecuteActionException e) {
            logger.error("Error in execution action ", e);
            updateLog(logExecutionAction, actionHTTP, (System.currentTimeMillis() - start)); // update log
        } catch (Exception e) {
            logger.error("Error processing the Thread Action HTTP", e);
            HibernateDAOUtils.rollback(transaction);
        } finally {
            if (transaction.isActive()) {
                transaction.commit();
                transaction = null;
            }
            if (session.isOpen()) {
                session.close();
            }
        }
        logger.info("End call action http");
    }

    private void updateLog(LogExecutionAction logExecutionAction, ActionHTTP actionHTTP, long duration) {
        if (logExecutionAction != null) {
            if (actionHTTP.getError() == null) { // or  status code between 200 and 3000
                logExecutionAction.setResponse(actionHTTP.getResponse());
            } else {
                logExecutionAction.setResponse(actionHTTP.getError());
            }
            logExecutionAction.setResponseCode(String.valueOf(actionHTTP.getStatusCode()));
            logExecutionAction.setEndDate(new Date());
            logExecutionAction.setProcessTime(duration);
            actionService.update(logExecutionAction);
        }
    }
}
