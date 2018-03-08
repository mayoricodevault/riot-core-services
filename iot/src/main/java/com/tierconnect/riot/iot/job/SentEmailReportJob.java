package com.tierconnect.riot.iot.job;


import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.job.JobUtils;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupFieldService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.utils.EmailSender;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.iot.entities.QReportDefinition;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.services.ReportDefinitionService;
import com.tierconnect.riot.iot.utils.ReportExecutionUtils;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.security.ApiKeyToken;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.mail.EmailException;
import org.apache.log4j.Logger;
import org.apache.shiro.util.ThreadContext;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;

import javax.mail.AuthenticationFailedException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Calendar;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Created by agutierrez on 2/3/15.
 */
public class SentEmailReportJob implements Job {
    static Logger logger = Logger.getLogger(SentEmailReportJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        String key = context.getJobDetail().getKey().getName();

        int countJob = (jobDataMap.get("countJob") != null) ? jobDataMap.getInt("countJob") : 1;

        Transaction transaction = null;
        ReportDefinition reportDefinition = null;
        Group group = null;
        String sid = key.split("_")[1];
        String messageError = "";
        Long reportDefinitionId = Long.valueOf(sid);
        if (reportDefinitionId == null) {
            messageError = " Report definition id: " + sid + " not found.";
        }
        try {
            Session session = HibernateSessionFactory.getInstance().getCurrentSession();
            transaction = session.getTransaction();
            transaction.begin();
            reportDefinition = ReportDefinitionService.getInstance().get(reportDefinitionId);
            if (reportDefinition != null) {
                Group reportGroup = reportDefinition.getGroup();
                group = reportGroup.getParentLevel3() != null ? reportGroup.getParentLevel3() : reportGroup
                        .getParentLevel2();
                logger.warn("Job: " + key + " executed with report " + reportDefinition.publicMap());
                sendEmail(group, reportDefinition);
            } else {
                logger.warn("Job: " + key + " not executed with reportDefinition doesn't exist");
            }
        } catch (EmailException ex) {
            if (ex.getCause() instanceof AuthenticationFailedException) {
                logger.info(String.format("Error produced by failed authentication, number attempt [%d] with Job: [%s], report name [%s]"
                        , countJob, key, reportDefinition.getName()));
                try {
                    Thread.sleep(10000); // 10 seconds
                } catch (InterruptedException e1) {
                    logger.error("Error in sleep 10 seconds in retry email");
                }
                // retry mail
                countJob++;
                jobDataMap.put("countJob", countJob);
                if (countJob < 6) {
                    throw new JobExecutionException(Boolean.TRUE);
                }
            } else {
                logger.error("Exception thrown when a checked error occurs in commons-email." + messageError + " Job: " +
                        key + " failed", ex);
            }
        } catch (IOException ex) {
            logger.error("Error produced by failed or interrupted I/O operations." + messageError, ex);
            if (group != null) {
                sendEmailError(group, reportDefinition, ex);
            }
        } catch (Exception ex) {
            logger.error("Error when process execute method for sending email Report." + messageError + " Job: " +
                    key + " failed", ex);
            if (group != null) {
                sendEmailError(group, reportDefinition, ex);
            }
        } finally {
            if (transaction!=null && transaction.isActive()) {
                transaction.commit();
            }
        }
    }

    public static void initSentReportJobs() {
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try {
            transaction.begin();
            List<ReportDefinition> reports = ReportDefinitionService.getInstance().getReportDefinitionDAO().selectAllBy(QReportDefinition.reportDefinition.emails.isNotEmpty().and(QReportDefinition.reportDefinition.schedule.isNotEmpty()));
            for (ReportDefinition reportDefinition : reports) {
                schedule(reportDefinition);
            }
            transaction.commit();
        } catch (Exception ex) { //Unsupported expression type
            logger.error(ex.getMessage(), ex);
            HibernateDAOUtils.rollback(transaction);
        }
    }

    private static void schedule(ReportDefinition reportDefinition) {
        if (StringUtils.isNotEmpty(reportDefinition.getSchedule())) {
            JobDetail job = newJob(SentEmailReportJob.class)
                    .withIdentity("reportJob_" + reportDefinition.getId(), "SentReportEmailJob")
                    .build();

            String timeZoneConfiguration = GroupFieldService.getInstance().getGroupField(reportDefinition.getGroup(), Constants.TIME_ZONE_CONFIG);
            logger.info("TIME ZONE: " + timeZoneConfiguration);
            Trigger trigger = newTrigger()
                    .withIdentity("triggerJob_" + reportDefinition.getId(), "SentReportEmailJob")
                    .withSchedule(cronSchedule(reportDefinition.getSchedule())
                            .inTimeZone(TimeZone.getTimeZone(timeZoneConfiguration))
                    )
                    .build();
            try {
                JobUtils.getScheduler().scheduleJob(job, trigger);
                logger.warn("Job Scheduled : reportJob_" + reportDefinition.getId());
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method reschedules Job of the report definition
     * @param reportDefinitionId ID report Definition
     */
    public static void reschedule(Long reportDefinitionId) {
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(reportDefinitionId);
        long startTime = System.currentTimeMillis() + 5000L;
        logger.info("Reschedule Sent Job startTime: "+startTime);
        String timeZoneConfiguration = GroupFieldService.getInstance().getGroupField(reportDefinition.getGroup(), Constants.TIME_ZONE_CONFIG);
        logger.info("TIME ZONE: " + timeZoneConfiguration);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(
                timeZoneConfiguration
        ));
        cal.setTimeInMillis(startTime);
        String keyJob = "reScheduleReportJob_" + reportDefinitionId;
        String groupJob = "SentReportEmailJobReschedule";
        JobUtils.killJob(keyJob, groupJob);
        if((reportDefinition.getSchedule()!=null) && (!reportDefinition.getSchedule().trim().isEmpty())) {
            JobDetail job = newJob(RescheduleSentEmailReportJob.class)
                    .withIdentity(keyJob, groupJob)
                    .build();

            Trigger trigger = newTrigger()
                    .withIdentity("triggerJob_" + reportDefinitionId, "SentReportEmailJobReschedule")
                    .startAt(cal.getTime())
                    .withSchedule(cronSchedule(reportDefinition.getSchedule())
                            .inTimeZone(TimeZone.getTimeZone(timeZoneConfiguration))
                    )
                    .build();
            try {
                JobUtils.getScheduler().scheduleJob(job, trigger);
            } catch (SchedulerException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This method is going to kill a specific Job
     * @param jobKeyToKill Job key name
     */
    public static void killJob(String jobKeyToKill){
        try {
            for (String groupName : JobUtils.getScheduler().getJobGroupNames()) {
                for (JobKey jobKey : JobUtils.getScheduler().getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    String jobName = jobKey.getName();
                    String jobGroup = jobKey.getGroup();
                    //get job's trigger
                    List<Trigger> triggers = (List<Trigger>) JobUtils.getScheduler().getTriggersOfJob(jobKey);
                    logger.debug("[jobName] : " + jobName + " [groupName] : " + jobGroup );
                    if (jobName.equals(jobKeyToKill)) {
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

    public static class RescheduleSentEmailReportJob implements Job {

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {

            String key = context.getJobDetail().getKey().getName();
            try {
                Session session = HibernateSessionFactory.getInstance().getCurrentSession();
                Transaction transaction = session.getTransaction();
                try {
                    transaction.begin();
                    String sid = key.split("_")[1];
                    Long reportDefinitionId = Long.valueOf(sid);
                    try {
                        JobUtils.getScheduler().deleteJob(new JobKey("reportJob_" + reportDefinitionId, "SentReportEmailJob"));
                    } catch (SchedulerException e) {
                        e.printStackTrace();
                    }
                    ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(reportDefinitionId);
                    if (reportDefinition != null) {
                        schedule(reportDefinition);
                    }
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

    /**
     *
     * @param group
     * @param reportDefinition
     */
    public void sendEmail(Group group, final ReportDefinition reportDefinition) throws Exception {
        final EmailSender emailSender = getEmailSenderConfiguration(group);
        final EmailSender.EmailMessageParameters messageParameters = getEmailMessageParameters(group, reportDefinition);
        try {
            emailSender.send(messageParameters);
            logger.info("Report definition ID [" + reportDefinition.getId() + "]: Mailing report success to: " + messageParameters.getTo());
        } catch (UserException ue) {
            logger.warn("Report definition ID [" + reportDefinition.getId() + "]: Mailing report error to: " + messageParameters.getTo() + " Authentication error");
        } catch (EmailException e) {
            if (e.getCause() instanceof AuthenticationFailedException) {
                throw e;
            } else {
                logger.warn("Report definition ID [" + reportDefinition.getId() + "]: Mailing report error to: " + messageParameters.getTo());
            }
        }
    }
    /**
     *
     * @param group
     * @param reportDefinition
     */
    public void sendEmailError(Group group, ReportDefinition reportDefinition, Throwable throwable){
        EmailSender emailSender = getEmailSenderConfiguration(group);
        EmailSender.EmailMessageParameters messageParameters = getEmailMessageParametersError(reportDefinition, group, throwable);
        try {
                emailSender.send(messageParameters);
        } catch(Exception ex){
            logger.error("Mailing error", ex);
        }
    }

    /**
     *
     * @param reportDefinition
     * @return EmailMessageParameters (Email Message Parameters Error)
     */
    public EmailSender.EmailMessageParameters getEmailMessageParametersError(ReportDefinition reportDefinition, Group group, Throwable throwable) {
        EmailSender.EmailMessageParameters messageParameters = new EmailSender.EmailMessageParameters();
        String email = ConfigurationService.getAsString(group, Constants.EMAIL_CONFIGURATION_ERROR);
        RiotShiroRealm.initCaches();
        ApiKeyToken token = new ApiKeyToken(reportDefinition.getEmailRunAs().getApiKey());
        PermissionsUtils.loginUser(token);
        StringWriter stringWriterThrowable = new StringWriter();
        PrintWriter printWriterThrowable = new PrintWriter(stringWriterThrowable);
        throwable.printStackTrace(printWriterThrowable);
        String ip = "";
        String hostName= "";
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            logger.warn("Couldn't get the server ip or host name", ex);
        }
        ip = "<tr><td><strong>Server IP: </strong></td><td>" + ip + "</td>";
        hostName = "<td><strong>Host Name: </strong></td><td>" + hostName + "</td></tr>";
        messageParameters.setFrom(ConfigurationService.getAsString(group, Constants.EMAIL_SMTP_USER));
        messageParameters.setTo(email);
        messageParameters.setSubject("ViZix Report: " + reportDefinition.getName());
        messageParameters.setMsg("<html><head></head><h2>This is an automated ERROR email from ViZix, please do not reply.</h2>"
                +"<body><table width=\"700\" border=\"0\"><tbody>"
                + "<tr><td><strong>ViZix Report definition ID: </strong></td><td>" + reportDefinition.getId() + "</td>"
                + "<td><strong>ViZix Report type: </strong></td><td>" + reportDefinition.getReportType() + "</td></tr>"
                + "<tr><td><strong>ViZix Report Name: </strong></td><td>" + reportDefinition.getName() + "</td>"
                + "<td><strong>ViZix Report Date: </strong></td><td>" + new Date() +"</td></tr>"
                + ip
                + hostName
                + "<tr><td colspan=\"4\" valign=\"top\"><strong>Report Definition: </strong></td></tr><tr><td colspan=\"4\" valign=\"top\">" + reportDefinition.toString() + "</td></tr>"
                + "<tr><td colspan=\"4\" valign=\"top\"><strong>Error Detail: </strong></td></tr><tr><td colspan=\"4\" valign=\"top\">" + stringWriterThrowable.toString() + "</td></tr></tbody></table></body></html>");
        messageParameters.setContentType("text/html");
        return messageParameters;
    }

    /**
     *
     * @param group
     * @return Email Sender (Report Email Configuration)
     */
    public EmailSender getEmailSenderConfiguration( Group group){
        EmailSender.SmtpParameters mailParameters = new EmailSender.SmtpParameters();
        mailParameters.setHost(ConfigurationService.getAsString(group, "emailSmtpHost"));
        Long emailSmtpPort = ConfigurationService.getAsLong(group, "emailSmtpPort");
        mailParameters.setPort(emailSmtpPort != null? emailSmtpPort.intValue(): 25);
        mailParameters.setSsl(ConfigurationService.getAsBoolean(group, "emailSmtpSsl", false));
        mailParameters.setTls(ConfigurationService.getAsBoolean(group, "emailSmtpTls", false));
        mailParameters.setUserName(ConfigurationService.getAsString(group, "emailSmtpUser"));
        mailParameters.setPassword(ConfigurationService.getAsString(group, "emailSmtpPassword"));
        return new EmailSender(mailParameters);
    }

    /**
     *
     * @param group
     * @param reportDefinition
     * @return EmailMessageParameters (Email values to send)
     */
    public EmailSender.EmailMessageParameters getEmailMessageParameters(Group group, ReportDefinition reportDefinition) throws Exception {
        EmailSender.EmailMessageParameters messageParameters = new EmailSender.EmailMessageParameters();
        String emails = reportDefinition.getEmails();
        String[] emailsList = emails.split(",");
        RiotShiroRealm.initCaches();
        ApiKeyToken token = new ApiKeyToken(reportDefinition.getEmailRunAs().getApiKey());
        PermissionsUtils.loginUser(token);

        DateFormatAndTimeZone dateFormatAndTimeZone = GroupService.getInstance().getDateFormatAndTimeZone(group);
        logger.info("GROUP [" + group.getName() + "] REGIONAL SETTING " + dateFormatAndTimeZone);
        File file = ReportExecutionUtils.exportingReport(reportDefinition.getId(), dateFormatAndTimeZone);
        ThreadContext.unbindSubject();
        ThreadContext.unbindSecurityManager();
        RiotShiroRealm.removeCaches();
        messageParameters.setFrom(ConfigurationService.getAsString(group, Constants.EMAIL_SMTP_USER));
        if (file != null) {
            EmailSender.Attachment attachment = new EmailSender.Attachment();
            attachment.setFile(file);
            attachment.setFileName(reportDefinition.getName()+".csv");
            attachment.setMimeType("text/csv");
            messageParameters.getAttachments().add(attachment);
        }
        messageParameters.setTo(emailsList);
        messageParameters.setSubject("ViZix Report: " + reportDefinition.getName());
        messageParameters.setMsg("<html><head></head><i>This is an automated email from ViZix, please do not reply. For any questions please contact your system administrator.</i><br><br>"
                + "<body>"
                + "Please find attached:<br>"
                + "<strong>ViZix Report Name:</strong> " + reportDefinition.getName()+"<br>"
                + "<strong>ViZix Report Date: </strong>" + new Date() + "</body></html>");
        messageParameters.setContentType("text/html");
        return messageParameters;
    }
}
