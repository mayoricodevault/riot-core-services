package com.tierconnect.riot.iot.job;

import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QUser;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.job.JobUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.iot.dao.mongo.SapMongoDAO;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.ServerException;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.security.ApiKeyToken;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.quartz.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.tierconnect.riot.iot.fmc.utils.FMCUtils.*;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Created by agutierrez on 8/31/15.
 */
public class FMCSyncDeleteJob implements Job {
    static Logger logger = Logger.getLogger(FMCSyncDeleteJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String key = context.getJobDetail().getKey().getName();
        Transaction transaction = null;
        try {
            Session session = HibernateSessionFactory.getInstance().getCurrentSession();
            String[] split = key.split("__");
            Integer nRetries = Integer.valueOf(split[1]);
            String serial = String.valueOf(split[2]);
            //Long childId = Long.valueOf(split[3]);
            Long userId = Long.valueOf(split[3]);
            Long groupId = Long.valueOf(split[4]);
            Long timeStamp = Long.valueOf(split[5]);
            String tCode = split[6];
            Long operationId = Long.valueOf(split[7]);
            Long sapId = Long.valueOf(split[8]);
            transaction = session.getTransaction();
            transaction.begin();
            try {
                RiotShiroRealm.initCaches();
                ApiKeyToken token = new ApiKeyToken(UserService.getInstance().getUserDAO().selectBy(QUser.user.id.eq(userId)).getApiKey());
                PermissionsUtils.loginUser(token);

                delete(nRetries, userId, groupId, timeStamp, tCode, operationId, serial, sapId);
            } catch (Exception ex) {
                nRetries++;
                reschedule(nRetries, serial, userId, groupId, timeStamp, tCode, operationId, sapId);
                if (ex instanceof UserException) {
                    logger.warn(ex.getMessage());
                } else if (ex instanceof ServerException) {
                    logger.error(ex.getMessage());
                } else {
                    logger.error(ex.getMessage(), ex);
                }
            }
            transaction.commit();

        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            HibernateDAOUtils.rollback(transaction);
        }
    }

    public void delete(int nRetries, Long userId, Long groupId, Long timestamp, String tCode, long operationId, String serial, long sapId) throws Exception {
        User user = UserService.getInstance().get(userId);
        Group group = GroupService.getInstance().get(groupId);
        Date sapSyncStoreDate = new Date();
        Date handHeldEventDate = new Date(timestamp);
        Map message = new HashMap<>();
        Map messageDetail = new HashMap<>();
        message.put("RFIDEquipmentTag_MT", messageDetail);
        messageDetail.put("TagID", "");
        messageDetail.put("EquipmentNum", serial);
        messageDetail.put("DateTime", getDate(timestamp));
        messageDetail.put("Plant", "");
        //messageDetail.put("Plant", group.getCode());
        messageDetail.put("Action", DELETE_CODE);
        messageDetail.put("User", user.getUsername());
        fmcHandleSapSendMessage(EVENT_TYPE_DELETE, message, messageDetail,null, user, sapSyncStoreDate, handHeldEventDate, nRetries, tCode, operationId, sapId);
    }


    public static void reschedule(int retry, String serial, Long userId, Long groupId, Long timestamp, String tCode, long operationId, long sapId) {
        if (serial != null  && userId != null && groupId != null) {
            User user = UserService.getInstance().get(userId);
            long startTime = System.currentTimeMillis() + (retry == 0 ? 500 : getMinimumTimeSecondsToWaitBetweenRetries(user));
            String jobKey = retry + "__" + serial + "__"  + user.getId() + "__" + groupId + "__" + timestamp + "__"
                    + tCode + "__" + operationId + "__" +sapId;
            JobKey jobKey1 = new JobKey("fmcSyncSapDeleteJob__" + jobKey, "fmcSyncSAPJob");
            if (retry < getMaxNumberOfRetries(user)) {
                JobDetail job = newJob(FMCSyncDeleteJob.class)
                        .withIdentity("fmcSyncSapDeleteJob__" + jobKey, "fmcSyncSAPJob")
                        .build();

                Trigger trigger = newTrigger()
                        .withIdentity("fmcSyncSapDeleteTrigger__" + jobKey, "fmcSyncSAPJob")
                        .startAt(new Date(startTime))
                        .build();
                try {
                    JobUtils.getScheduler().scheduleJob(job, trigger);
                } catch (SchedulerException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    if (JobUtils.getScheduler().checkExists(jobKey1)) {
                        JobUtils.getScheduler().deleteJob(jobKey1);
                    }
                } catch (SchedulerException e) {
                    e.printStackTrace();
                }
                logger.error("Task fmcSyncSapDeleteJob_"+jobKey+ " cannot be reschedule the maximum # of retries has been used ");
                SapMongoDAO.getInstance().setPendingOnNewDate(sapId);
            }
        }
    }

}
