package com.tierconnect.riot.appcore.job;

import org.apache.log4j.Logger;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by agutierrez on 2/3/15.
 */
public class JobUtils {
    private static Logger logger = Logger.getLogger( JobUtils.class );

    static Scheduler scheduler;

    public static Scheduler getScheduler() {
        return scheduler;
    }

    public static void startQuartz() {
        try {
            // Grab the Scheduler instance from the Factory
            scheduler = StdSchedulerFactory.getDefaultScheduler();

            // and start it off
            scheduler.start();
        } catch (SchedulerException se) {
            se.printStackTrace();
        }

    }

    public static void stopQuartz() {
        try {
            if (scheduler != null) {
                scheduler.shutdown();
            }
        } catch (SchedulerException se) {
            se.printStackTrace();
        }

    }

    public static void stopQuartzWaiting() {
        stopQuartz();

        int ct = 0;

        // Try waiting for the scheduler to shutdown. Only wait 30 seconds.
        while(ct < 30) {
            ct++;
            // Sleep for a second so the quartz worker threads die.  This
            // suppresses a warning from Tomcat during shutdown.
            try {
                logger.info("waiting for quartz");
                Thread.sleep(1000);
                if ((scheduler == null) || scheduler.isShutdown()) {
                    break;
                }
            }
            catch (Exception e) {
                logger.info("Error on quartz wait", e);
            }

        }
    }

    /**
     * This method is going to kill a specific Job
     * @param jobKeyToKill Job key name
     * @param jobGroupToKill Job group name
     */
    public static void killJob(String jobKeyToKill, String jobGroupToKill){
        try {
            for (String groupName : JobUtils.getScheduler().getJobGroupNames()) {
                for (JobKey jobKey : JobUtils.getScheduler().getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    String jobName = jobKey.getName();
                    String jobGroup = jobKey.getGroup();
                    //get job's trigger
                    logger.debug("[jobName] : " + jobName + " [groupName] : " + jobGroup );
                    if (jobName.equals(jobKeyToKill) && jobGroup.equals(jobGroupToKill)) {
                        List<JobKey> lst = new ArrayList<>();
                        lst.add(jobKey);
                        JobUtils.getScheduler().deleteJobs(lst);
                        logger.info("Job " + jobKey + " was killed.");
                        break;
                    }
                }
            }
        } catch(SchedulerException e) {
            logger.error("Cannot kill scheduler: ",e);
        }
    }

}
