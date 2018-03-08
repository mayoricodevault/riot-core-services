package com.tierconnect.riot.iot.servlet;

import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.job.JobUtils;
import com.tierconnect.riot.appcore.job.TokenCleanupJob;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.LicenseService;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.commons.broker.BrokerSubscriber;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.commons.utils.MomentDateFormatUtils;
import com.tierconnect.riot.iot.dao.SequenceDAO;
import com.tierconnect.riot.iot.dao.mongo.MongoScriptDAO;
import com.tierconnect.riot.iot.job.*;
import com.tierconnect.riot.iot.reports.autoindex.IndexCreatorManager;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.Migration;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.version.MigrationDefinition;
import org.apache.log4j.Logger;
import org.apache.shiro.util.ThreadContext;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.quartz.SchedulerException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.math.NumberUtils.isNumber;


/**
 * Created by agutierrez on 12/5/2014.
 * AppContext Class
 */
// TODO: ADLO: these classes are still AFU - see Configuration,
// AppcoreConfigHelper, AppcoreContextListener
public class AppcoreContextListener implements ServletContextListener {

    private static Logger logger = Logger.getLogger(AppcoreContextListener.class);

    @java.lang.Override
    public void contextInitialized(ServletContextEvent sce) {
        PopDBRequired.initJDBCDrivers();
        MomentDateFormatUtils.initCompileMomentJS();

        //This should be "validate" on production/release branch
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
        Configuration.init(sce.getServletContext());

        for (Map.Entry<String, String> property : Configuration.getHibernateProperties().entrySet()) {
            logger.info(property.getKey() + "=" + property.getValue());
        }
        logger.info("mongo.primary=" + Configuration.getProperty("mongo.primary"));
        logger.info("mongo.secondary=" + Configuration.getProperty("mongo.secondary"));
        logger.info("mongo.replicaset=" + Configuration.getProperty("mongo.replicaset"));
        logger.info("mongo.ssl=" + Configuration.getProperty("mongo.ssl"));
        logger.info("mongo.sharding=" + Configuration.getProperty("mongo.sharding"));
        logger.info("mongo.connectiontimeout=" + Configuration.getProperty("mongo.connectiontimeout"));
        logger.info("mongo.maxpoolsize=" + Configuration.getProperty("mongo.maxpoolsize"));
        try {

            MongoDAOUtil.setupMongodb(Configuration.getProperty("mongo.primary"),
                    Configuration.getProperty("mongo.secondary"),
                    Configuration.getProperty("mongo.replicaset"),
                    Boolean.valueOf(Configuration.getProperty("mongo.ssl")),
                    Configuration.getProperty("mongo.username"),
                    Configuration.getProperty("mongo.password"),
                    Configuration.getProperty("mongo.authdb"),
                    Configuration.getProperty("mongo.db"),
                    Configuration.getProperty("mongo.controlReadPreference"),
                    Configuration.getProperty("mongo.reportsReadPreference"),
                    Boolean.valueOf(Configuration.getProperty("mongo.sharding")),
                    (isNotBlank(Configuration.getProperty("mongo.connectiontimeout")) && isNumber(Configuration
                            .getProperty("mongo.connectiontimeout"))) ? Integer.parseInt(Configuration.getProperty
                            ("mongo.connectiontimeout")) : null,
                    (isNotBlank(Configuration.getProperty("mongo.maxpoolsize")) && isNumber(Configuration.getProperty
                            ("mongo.maxpoolsize"))) ? Integer.parseInt(Configuration.getProperty("mongo.maxpoolsize"))
                            : null);

            Thread mongoIdxThread = new Thread("Check Mongo Indexes") {
                public void run() {
                    MongoDAOUtil.checkIndexes();
                }
            };
            mongoIdxThread.start();

            MongoScriptDAO.getInstance().createFromResource("JSONFormatter", "mongo/JsonFormatter.js");

        } catch (Exception e) {
            logger.error("An error occurred connecting to Mongo.", e);
            throw new RuntimeException("An error occurred connecting to Mongo.", e);
        }

        String licenseIgnore = Configuration.getProperty("license.ignore");
        LicenseService.setEnableLicense(licenseIgnore == null || licenseIgnore.toLowerCase().equals("false"));

        try {
            MigrationDefinition.getInstance().init();
        } catch (Exception e) {
            logger.error("An error occurred reading version components", e);
            throw new RuntimeException("An error occurred reading version components", e);
        }
        try {
            Migration.migrate(MigrationDefinition.getInstance());
        } catch (Exception e) {
            if (!Boolean.valueOf(Configuration.getProperty("forceStart"))) {
                throw new RuntimeException("An error occurred migrating application", e);
            }
        }
        JobUtils.startQuartz();
        try {
            JobUtils.getScheduler().clear();
        } catch (SchedulerException e) {
            logger.info("Cannot clear the Schedulers.");
        }
        UUID uuid = UUID.randomUUID();

        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try {
            transaction.begin();
            LicenseService.getInstance().logLicenses();
            BrokerClientHelper br = new BrokerClientHelper();
            br.init("APP-pub-" + uuid.toString(), Boolean.valueOf(Configuration.getProperty("broker" +
                    ".connection.wait")));
            NotificationService.init("APP-not-" + uuid.toString());
            HealthAndStatusService.init("APP-sub-" + uuid.toString());
            // StatusService.getInstance().init("APP-stat-" + uuid.toString(), "");
            StatusService.getInstance().init("APP-stat-" + uuid.toString());

            //Start services older snapshot fixer job
            if (!Boolean.parseBoolean(Configuration.getProperty("spark.enabled"))) {
                SnapshotFixerJobServices.init();
            }

            initCaches();

            transaction.commit();
        } catch (Exception ex) {
            logger.error("ERROR Starting up application", ex);
            HibernateDAOUtils.rollback(transaction);
        }
        initJobs();
        SequenceDAO.getInstance().initSequences();

        logger.warn("--------------------------------------------------------");
        logger.warn("---------******* ViZix SERVICES STARTED *******---------");
        logger.warn("--------------------------------------------------------");
    }

    private void initCaches() {
        GroupService.getInstance().loadAllInCache();
        ThingTypeService.getInstance().loadAllInCache();
        DataTypeService.getInstance().loadAllInCache();
    }

    private void initJobs() {
        SentEmailReportJob.initSentReportJobs();
        TokenCleanupJob.init(Configuration.getProperty("cronSchedule.TokenCleanUp"));

        logger.info("STARTING: ......indexStatisticSchedule");
        GetIndexInformationJob.init();
        logger.info("STARTING: ......indexCleanupSchedule");
        AutoDeletionIndexesJob.init();

        ShiftZoneRevalidationJob shiftZoneJob = new ShiftZoneRevalidationJob();
        shiftZoneJob.init();
        //VIZIX-1236, ReportBulkProcessJob DELETE should start automatically when Vizix is reinitialized
        BackgroundProcessService.getInstance().findPendingDeleteBulkAndExecute();
		//VIZIX-2057, Implement new method for Delete Thing. compensation algorithm Execution
		ThingService.getInstance().runCompensationAlgorithmJob(false, null);
		//Execute algorithm for broken processes
		IndexCreatorManager.getInstance().updateIndexCreationOnApplicationRestart();
		Boolean scheduledRuleActive = Boolean.valueOf(Configuration.getProperty("scheduledrule.enabled"));
        if (scheduledRuleActive) {
            ScheduledRuleService ruleService = new ScheduledRuleService();
            ruleService.controlScheduledRuleAfterRestart();
        }
    }

    @java.lang.Override
    public void contextDestroyed(ServletContextEvent sce) {

        PopDBRequired.closeJDBCDrivers();

        // cassandra shutdown
        CassandraUtils.shutdown();

        // shutdown hiberante
        HibernateSessionFactory.closeInstance();

        JobUtils.stopQuartzWaiting();

        //shiro
        ThreadContext.remove();
        ThreadContext.unbindSecurityManager();
        ThreadContext.unbindSubject();

        LicenseService.clearCaches();

        for (HealthAndStatusService healthAndStatusService : HealthAndStatusService.getInstances()) {
        healthAndStatusService.shutdown();
    }

		for (NotificationService notificationService : NotificationService.getInstances()) {
				notificationService.shutdown();
		}

        try {
            for (BrokerSubscriber brokerSubscriber : StatusService.getInstance().getBrokerSubscribers()) {
                brokerSubscriber.shutdown();
            }
        } catch (Exception exception) {
            logger.error(exception.getMessage(), exception);
        }

		//CacheManager.getInstance().shutdown();
	}
}
