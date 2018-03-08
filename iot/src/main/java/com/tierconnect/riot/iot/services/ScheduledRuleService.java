package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.expr.BooleanExpression;
import com.tierconnect.riot.appcore.entities.Favorite;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QFavorite;
import com.tierconnect.riot.appcore.job.JobUtils;
import com.tierconnect.riot.appcore.services.FavoriteService;
import com.tierconnect.riot.iot.dao.ScheduledRuleDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.job.ScheduledRuleJob;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import javax.annotation.Generated;
import java.util.*;

import static com.tierconnect.riot.commons.ConnectionConstants.BRIDGE_STARTUP_OPTIONS;
import static com.tierconnect.riot.commons.ConnectionConstants.SCHEDULED_RULE_SERVICES_CONNECTION_CODE;
import static com.tierconnect.riot.commons.Constants.BRIDGE_CODE;
import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class ScheduledRuleService extends ScheduledRuleServiceBase {
    static Logger logger = Logger.getLogger(ScheduledRuleService.class);

    public void scheduledRuleJob(ScheduledRule scheduledRule){
        try {
            String scheduledRuleNameJob = "scheduledRuleJob_" + scheduledRule.getCode();
            // check if the scheduledRule is active
            JobKey jobKey = controlScheduledJob(scheduledRuleNameJob);
            if (scheduledRule.getActive()) {
                scheduleJob(jobKey, scheduledRule);
            } else {
                killJob(jobKey);
            }
        } catch (SchedulerException e) {
            logger.error("Unable to schedule or kill job "+scheduledRule.getName()+", reason=", e);
        }
    }

    /**
     * Schedule job based on scheduledRule
     * @param jobKey
     * @param scheduledRule
     * @throws SchedulerException
     */
    private void scheduleJob(JobKey jobKey, ScheduledRule scheduledRule) throws SchedulerException {
        String scheduledRuleNameJob = "scheduledRuleJob_" + scheduledRule.getName();
        Scheduler scheduler = JobUtils.getScheduler();
        boolean rescheduleJob = false;

        // check if the ScheduledRule is not already scheduled
        if (jobKey != null){
            rescheduleJob = true;
        }

        String cronExpr = null;
        JobDataMap jobDataMap = new JobDataMap();
        if (scheduledRule.getCron_expression() != null) {
            cronExpr = scheduledRule.getCron_expression();
            logger.info("Cron Expression: " + cronExpr);
        } else {
            throw new SchedulerException("The cron expression is NULL");
        }

        if (scheduledRuleNameJob != null) {
            jobDataMap.put("jobName", scheduledRuleNameJob);
            logger.info("Scheduled Job: " + scheduledRuleNameJob);
        }

        // define and get report type
        ReportDefinition reportDefinition = scheduledRule.getReportDefinition();
        if (reportDefinition != null) {
            jobDataMap.put("reportId", reportDefinition.getId());
            logger.info("Report Id: " + reportDefinition.getId());
            jobDataMap.put("reportType", reportDefinition.getReportType());
            logger.info("Report Type: " + reportDefinition.getReportType());
        }

        String ruleExecutionMode = scheduledRule.getRule_execution_mode();
        if (ruleExecutionMode != null){
            jobDataMap.put("ruleExecutionMode", ruleExecutionMode);
            logger.info("Rule Execution Mode: " + ruleExecutionMode);
        }

        if (scheduledRule.getEdgebox() != null){
            jobDataMap.put("coreBridgeConf", scheduledRule.getEdgebox().getConfiguration());
            jobDataMap.put(BRIDGE_CODE, scheduledRule.getEdgebox().getCode());
        }

        String extraConf = scheduledRule.getExtra_configuration();
        if (extraConf != null){
            jobDataMap.put("extraConf", extraConf);
            logger.info("Extra Configuration: " + extraConf);
        }

        Trigger trigger = newTrigger()
                .withIdentity("triggerJob_" + scheduledRule.getCode(), "ScheduledRuleJob")
                .withSchedule(cronSchedule(cronExpr)
                        .withMisfireHandlingInstructionIgnoreMisfires())
                .build();

        if (rescheduleJob) {
            scheduler.standby();
            // update JobDataMap
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            jobDetail.getJobDataMap().clear();
            jobDetail.getJobDataMap().putAll(jobDataMap);
            // reschedule job
            TriggerKey triggerKey = new TriggerKey("triggerJob_" + scheduledRule.getCode(), "ScheduledRuleJob");
            Trigger oldTrigger = scheduler.getTrigger(triggerKey);
            scheduler.rescheduleJob(oldTrigger.getKey(), trigger);
            // update jobDataMap and replace the new one
            scheduler.addJob(jobDetail, true);
            logger.info("[ScheduledRuleJob] name: '"+scheduledRuleNameJob+"' re-scheduled sucessfully");
        } else {
            JobDetail job = newJob(ScheduledRuleJob.class)
                    .withIdentity(scheduledRuleNameJob, "ScheduledJob")
                    .storeDurably()
                    .usingJobData(jobDataMap)
                    .build();

            scheduler.scheduleJob(job, trigger);
            logger.info("[ScheduledRuleJob] name: '"+scheduledRuleNameJob+"' scheduled sucessfully");
        }
        // start scheduler
        scheduler.start();
    }

    /**
     * Check if job is scheduled/running or not
     * @return
     */
    private JobKey controlScheduledJob(String jobNameToControl) throws SchedulerException {
        Scheduler scheduler = JobUtils.getScheduler();
        for (String groupName : scheduler.getJobGroupNames()) {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                String jobName = jobKey.getName();
                String jobGroup = jobKey.getGroup();
                //get job's trigger
                List<Trigger> triggers = (List<Trigger>) JobUtils.getScheduler().getTriggersOfJob(jobKey);
                logger.debug("[ScheduledRuleName] : " + jobName + " [groupName] : " + jobGroup );
                if (jobName.equals(jobNameToControl)) {
                    return jobKey;
                }
            }
        }
        return null;
    }

    /**
     * Kill the job that is disabled by UI
     * @param jobNameToKill
     */
    private void killJob (JobKey jobKey) throws SchedulerException{
        if (jobKey != null) {
            Scheduler scheduler = null;
            scheduler = JobUtils.getScheduler();
            scheduler.deleteJob(jobKey);
            logger.info("[ScheduledRuleJob] " + jobKey.getName() + " was killed.");
        }
    }

    /**
     * Update rules by each update to regenerate rules
     * @param scheduledRule
     */
    public void updateRulesByReport(ScheduledRule scheduledRule) {
        // validate if the reportDefinition of ScheduledRule has changed
        ScheduledRule scheduledRuleDB = ScheduledRuleService.getInstance().get(scheduledRule.getId());
        // select all rules related to scheduler
        EdgeboxRuleService edgeboxRuleService = new EdgeboxRuleService();
        List<EdgeboxRule> edgeboxRuleList = edgeboxRuleService.selectByScheduledRuleId(scheduledRule.getId());
        // call EdgeboxRuleService to return new condition based on report modification
        Iterator<EdgeboxRule> ruleIterator = edgeboxRuleList.iterator();
        while (ruleIterator.hasNext()){
            EdgeboxRule edgeboxRule = ruleIterator.next();
            String ruleCondition = edgeboxRuleService.createConditionForRuleScheduled(edgeboxRule);
            // Update rules
            edgeboxRule.setRule(ruleCondition);
            edgeboxRuleService.update(edgeboxRule);
        }
    }

    /**
     * Control if the Scheduled Rule created already exists
     * @param code
     * @param group
     * @return
     */
    public boolean existsScheduledRuleCode(String code, Group group) {
        BooleanExpression predicate = QScheduledRule.scheduledRule.code.eq(code);
        predicate.and(QScheduledRule.scheduledRule.group.eq(group));
        ScheduledRuleDAO scheduledRuleDAO = getScheduledRuleDAO();
        return scheduledRuleDAO.getQuery().where(predicate).exists();
    }

    /**
     * Control if the Shcheduled Rule created already exists based on Id
     * @param code
     * @param group
     * @param excludeId
     * @return
     */
    public boolean existsScheduledRuleCode(String code, Group group, Long excludeId) {
        BooleanExpression predicate = QScheduledRule.scheduledRule.code.eq(code);
        predicate = predicate.and(QScheduledRule.scheduledRule.id.ne(excludeId).and(QScheduledRule.scheduledRule.group.eq(group)));
        ScheduledRuleDAO scheduledRuleDAO = getScheduledRuleDAO();
        return scheduledRuleDAO.getQuery().where(predicate).exists();
    }

    /**
     * Control if the Scheduled Rule created already exists
     * @param code
     * @param group
     * @return
     */
    public boolean existsScheduledRuleName(String name, Group group) {
        BooleanExpression predicate = QScheduledRule.scheduledRule.name.eq(name);
        predicate.and(QScheduledRule.scheduledRule.group.eq(group));
        ScheduledRuleDAO scheduledRuleDAO = getScheduledRuleDAO();
        return scheduledRuleDAO.getQuery().where(predicate).exists();
    }

    /**
     * Control if the Shcheduled Rule created already exists based on Id
     * @param code
     * @param group
     * @param excludeId
     * @return
     */
    public boolean existsScheduledRuleName(String name, Group group, Long excludeId) {
        BooleanExpression predicate = QScheduledRule.scheduledRule.name.eq(name);
        predicate = predicate.and(QScheduledRule.scheduledRule.id.ne(excludeId).and(QScheduledRule.scheduledRule.group.eq(group)));
        ScheduledRuleDAO scheduledRuleDAO = getScheduledRuleDAO();
        return scheduledRuleDAO.getQuery().where(predicate).exists();
    }

    /**
     * Control if coreBridge configuration has rest connection
     * @return
     */
    public boolean isCBRestConnection(String coreBridgeConfig) {
        JSONObject coreBridgeConfObject = null;
        try {
            coreBridgeConfObject = (JSONObject) new JSONParser().parse(coreBridgeConfig);
        } catch (ParseException e) {
            logger.error("Unable to parse coreBridgeConfiguration");
            return false;
        }
        // check REST connection exists
        if (coreBridgeConfObject.containsKey(BRIDGE_STARTUP_OPTIONS)){
            JSONObject bridgeStartupOptionsObj = null;
            try {
                bridgeStartupOptionsObj = (JSONObject) new JSONParser().parse(coreBridgeConfObject.get(BRIDGE_STARTUP_OPTIONS).toString());
                if (bridgeStartupOptionsObj.containsKey(SCHEDULED_RULE_SERVICES_CONNECTION_CODE)){
                    return true;
                }
            } catch (ParseException e) {
                logger.error("Unable to parse " + BRIDGE_STARTUP_OPTIONS);
                return false;
            }
        }
        return false;
    }

    /**
     * Control if the deleted Scheduled Rule has no rules
     * @param scheduledRuleId
     * @return
     */
    public boolean validateDeleteScheduledRule (Long scheduledRuleId){
        List<EdgeboxRule> edgeboxRules = EdgeboxRuleService.getEdgeBoxRulesByScheduledRule(scheduledRuleId);
        if (edgeboxRules.size() > 0) {
            return true;
        }
        return false;
    }

    /**
     * Delete Scheduled Rule
     * @param scheduledRule
     * @param cascadeDelete
     * @return
     */
    public static List<String> deleteCurrentScheduledRule(ScheduledRule scheduledRule, boolean cascadeDelete){
        List<String> errorMessages = new ArrayList<>();
        if (cascadeDelete == false) {
            // edgeboxrule validation
            if (ScheduledRuleService.getInstance().validateDeleteScheduledRule (scheduledRule.getId())) {
                errorMessages.add("Scheduled Rule has references in Rules.");
            }
        } else {
            // Starting to delete scheduled rule in cascade
            List<EdgeboxRule> edgeboxRules = EdgeboxRuleService.getEdgeBoxRulesByScheduledRule(scheduledRule.getId());
            if (edgeboxRules.size() > 0) {
                if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new HashMap<>(), "edgeboxRule:d")) {
                    throw new UserException("Permissions error: User does not have permission to delete rules.");
                }
                for (EdgeboxRule edgeboxRule : edgeboxRules){
                    EdgeboxRuleService.getInstance().delete(edgeboxRule);
                }
            }
        }
        if (errorMessages.isEmpty()){
            // stop scheduled rule
            if (scheduledRule.getActive()){
                scheduledRule.setActive(Boolean.FALSE);
                ScheduledRuleService.getInstance().update(scheduledRule);
            }
            ScheduledRuleService.getInstance().delete(scheduledRule);
        }
        return errorMessages;
    }

    /**
     * After start services load all ScheduledRules "actived"
     * and re-scheduled again
     */
    public void controlScheduledRuleAfterRestart(){
        // get sequences from DB
        Session session;
        try {
            session = HibernateSessionFactory.getInstance().getCurrentSession();
            Transaction transaction = session.getTransaction();
            transaction.begin();
            // Load all ScheduledRules actived
            Criteria criteria = session.createCriteria(ScheduledRule.class).add(
                    Restrictions.eq("active", true));
            for (final Object o : criteria.list()) {
                scheduledRuleJob((ScheduledRule) o);
            }
            transaction.commit();
        } catch (Exception ex) {
            System.err.println("Hibernate validation Error " + ex.getMessage());
            logger.error("Hibernate validation Error", ex);
            throw ex;
        }
    }

    public boolean validateCronExpression(String cronExpression) {
        try {
            CronExpression cronEx = new CronExpression(cronExpression);
            if (cronEx.getNextValidTimeAfter(new Date()) == null) {
                return false;
            }
        } catch (java.text.ParseException e) {
            return false;
        }

        return CronExpression.isValidExpression(cronExpression);
    }

    public void updateFavorite( ScheduledRule scheduledRule )
    {
        String typeElement = "scheduledRule";
        Long elementId = scheduledRule.getId();

        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QFavorite.favorite.typeElement.eq(typeElement));
        be = be.and(QFavorite.favorite.elementId.eq(elementId));
        List <Favorite> listFavorite = FavoriteService.getInstance().listPaginated(be,null, null);
        for (Favorite favorite: listFavorite){
            favorite.setElementName(scheduledRule.getName());
            FavoriteService.getInstance().update(favorite);
        }

    }
}

