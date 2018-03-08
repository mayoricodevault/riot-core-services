package com.tierconnect.riot.iot.job;

import org.apache.log4j.Logger;
import org.quartz.Scheduler;
import org.quartz.SchedulerConfigException;
import org.quartz.SchedulerException;
import org.quartz.impl.DirectSchedulerFactory;
import org.quartz.simpl.RAMJobStore;
import org.quartz.simpl.SimpleThreadPool;

/**
 * Created by brayan on 6/20/17.
 */
@Deprecated
public class ScheduledRuleThreadPool {
    private static Logger logger = Logger.getLogger(ScheduledRuleThreadPool.class);
    private static ScheduledRuleThreadPool instance = new ScheduledRuleThreadPool();
    private static SimpleThreadPool threadPool = new SimpleThreadPool(10, 5);
    final DirectSchedulerFactory schedulerFactory = DirectSchedulerFactory.getInstance();

    public static ScheduledRuleThreadPool getInstance() {
        return instance;
    }

    public DirectSchedulerFactory getSchedulerFactory() {
        return schedulerFactory;
    }

    public static SimpleThreadPool getThreadPool() {
        return threadPool;
    }

    public void initializeThreadPool() throws SchedulerConfigException {
        threadPool.initialize();
    }

    public void startScheduler() throws SchedulerException {
        schedulerFactory.createScheduler(threadPool, new RAMJobStore());
    }

    public Scheduler getScheduler() throws SchedulerException {
        return schedulerFactory.getScheduler();
    }

}
