package com.tierconnect.riot.iot.job;

import com.tierconnect.riot.appcore.job.JobUtils;

import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.commons.Constants;

import com.tierconnect.riot.iot.reports.autoindex.services.IndexInformationMongoService;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.quartz.*;

import org.apache.log4j.Logger;

import java.util.Date;
import java.util.TimeZone;

import static com.tierconnect.riot.commons.Constants.*;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Created by achambi on 5/29/17.
 * Class to implement a job that generates index statistics..
 */
public class GetIndexInformationJob implements Job {

    static Logger logger = Logger.getLogger(GetIndexInformationJob.class);


    public static void init() {
        schedule(getParameter(INDEX_STATISTIC_SCHEDULE), getParameter(TIME_ZONE_CONFIG));
    }

    private static void schedule(String cronScheduleGetIndexInfo, String timeZoneConfiguration) {
        JobDetail job = newJob(GetIndexInformationJob.class)
                .withIdentity("getIndexInfoJob", "GetIndexInformationJob").build();
        Trigger trigger = newTrigger()
                .withIdentity("getIndexInfoJob_Trigger", "GetIndexInformationJob")
                .withSchedule(cronSchedule(cronScheduleGetIndexInfo).inTimeZone(TimeZone.getTimeZone(timeZoneConfiguration))).build();
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
    public static void reschedule(String cronScheduleGetIndexInfo, String timeZoneConfiguration) {
        logger.info("Restarting get index Information.....");
        JobUtils.killJob("getIndexInfoJob", "GetIndexInformationJob");
        schedule(cronScheduleGetIndexInfo, timeZoneConfiguration);
    }


    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Running get index Information.....");
        String key = context.getJobDetail().getKey().getName();
        try {
            Date currentDate = new Date();

            IndexInformationMongoService.getInstance().saveIndexStats(Constants.COLLECTION_THINGS,
                    REPORT_LOG_MAX_ARRAY_HISTORY_LOG, currentDate);
            IndexInformationMongoService.getInstance().saveIndexStats(Constants.COLLECTION_THINGSNAPSHOT,
                    REPORT_LOG_MAX_ARRAY_HISTORY_LOG, currentDate);
            logger.info("Get index usage statistics completed.");
        } catch (Exception ex) {
            logger.error("Job: " + key + " failed", ex);
        }
    }
}
