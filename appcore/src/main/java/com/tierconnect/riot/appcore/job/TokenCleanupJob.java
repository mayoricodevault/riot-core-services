package com.tierconnect.riot.appcore.job;

import com.tierconnect.riot.appcore.entities.QToken;
import com.tierconnect.riot.appcore.services.TokenService;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.quartz.*;

import java.util.Date;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Created by agutierrez on 6/1/15.
 */
public class TokenCleanupJob implements Job {

    static Logger logger = Logger.getLogger(TokenCleanupJob.class);

    public static void init(String cronScheduleTokenCleanUp) {
        schedule(cronScheduleTokenCleanUp);
    }

    private static void schedule(String cronScheduleTokenCleanUp) {
        JobDetail job = newJob(TokenCleanupJob.class).withIdentity("tokenCleanupJob", "TokenCleanupJob").build();
        //run every two hours
        Trigger trigger = newTrigger().withIdentity("tokenCleanupJob_Trigger", "TokenCleanupJob").withSchedule
                (cronSchedule(cronScheduleTokenCleanUp)).build();
        try {
            JobUtils.getScheduler().scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Running cleaning of tokens....");
        String key = context.getJobDetail().getKey().getName();
        try {
            Session session = HibernateSessionFactory.getInstance().getCurrentSession();
            Transaction transaction = session.getTransaction();
            try {
                transaction.begin();
                //remove expired tokens by an hour
                TokenService.getInstance().getTokenDAO().deleteAllBy(QToken.token.tokenExpirationTime.before(new Date
                        (new Date().getTime() - 1000 * 60 * 60)));
                transaction.commit();
            } catch (Exception ex) { //Unsupported expression type
                logger.error(ex.getMessage(), ex);
                HibernateDAOUtils.rollback(transaction);
            }
        } catch (Exception ex) {
            logger.error("Job: " + key + " failed", ex);
        }
    }
}
