package com.tierconnect.riot.iot.job;

import com.tierconnect.riot.appcore.job.JobUtils;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.iot.reports.autoindex.services.IndexInformationMongoService;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.quartz.*;

import java.util.Date;
import java.util.TimeZone;

import static com.tierconnect.riot.appcore.services.UserServiceBase.getInstance;
import static com.tierconnect.riot.commons.Constants.*;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Created by achambi on 5/29/17.
 * Class to implement a job that generates index statistics..
 */
public class AutoDeletionIndexesJob implements Job {

    static Logger logger = Logger.getLogger(AutoDeletionIndexesJob.class);

    public static void init() {
        schedule(getParameter(INDEX_CLEANUP_SCHEDULE), getParameter(TIME_ZONE_CONFIG));
    }

    private static void schedule(String cronScheduleDelIndex, String timeZoneConfiguration) {
        JobDetail job = newJob(AutoDeletionIndexesJob.class)
                .withIdentity("autoDeletionIndexesJob", "AutoDeletionIndexesJob").build();
        Trigger trigger = newTrigger()
                .withIdentity("autoDeletionIndexesJob_Trigger", "AutoDeletionIndexesJob")
                .withSchedule(cronSchedule(cronScheduleDelIndex)
                        .inTimeZone(TimeZone.getTimeZone(timeZoneConfiguration))).build();
        try {
            JobUtils.getScheduler().scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    //TODO: get parameter need fix!!!
    //FIXME: Change this code to command design pattern.
    @SuppressWarnings("Duplicates")
    private static String getParameter(String parameter) {
        String timeZoneConfig = "";
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try {
            if (!transaction.isActive()) {
                transaction = session.beginTransaction();
            }
            timeZoneConfig = ConfigurationService.getAsString(UserService.getInstance()
                    .getRootUser(), parameter);
            transaction.commit();
        } catch (Exception ex) {
            logger.error("ERROR Starting parameter application", ex);
            HibernateDAOUtils.rollback(transaction);
        }
        return timeZoneConfig;
    }

    /**
     * this method is called by reflexion from GroupConfiguration
     */
    @SuppressWarnings("unused")
    public static void reschedule(String cronScheduleDelIndex, String timeZoneConfiguration) {
        logger.info("Restarting Automatic deletion of indexes.....");
        JobUtils.killJob("autoDeletionIndexesJob", "AutoDeletionIndexesJob");
        schedule(cronScheduleDelIndex, timeZoneConfiguration);
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Starting Automatic Indexes Removal......");
        String key = context.getJobDetail().getKey().getName();
        try {
            Long indexRunNumberMin = getGroupParameter(INDEX_MIN_COUNT_TO_MAINTAIN);
            Long minimumNumberDays = getGroupParameter(INDEX_MIN_DAYS_TO_MAINTAIN);
            IndexInformationMongoService.getInstance().deleteUnusedIndexes(
                    indexRunNumberMin,
                    minimumNumberDays,
                    new Date());
            logger.info("Deletion of Unused Indexes Completed");
        } catch (Exception ex) {
            logger.error("Job: " + key + " failed", ex);
        }
    }

    private static Long getGroupParameter(String parameter) {
        Long result = 0L;
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try {
            transaction.begin();
            result = ConfigurationService.getAsLong(GroupService.getInstance().getRootGroup(), parameter);
            logger.debug("[AutoDeletionIndexesJob] Parameter found successfully: " + result);
            transaction.commit();
        } catch (Exception e) {
            logger.error("[AutoDeletionIndexesJob] Cannot read " + parameter + " configuration");
            HibernateDAOUtils.rollback(transaction);
        }
        return result;
    }
}
