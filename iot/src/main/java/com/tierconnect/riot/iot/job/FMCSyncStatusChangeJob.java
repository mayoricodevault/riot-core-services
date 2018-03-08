package com.tierconnect.riot.iot.job;

import com.google.common.base.Charsets;
import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.QUser;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.job.JobUtils;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.iot.dao.mongo.SapMongoDAO;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ZoneService;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.ServerException;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.security.ApiKeyToken;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import org.apache.commons.codec.binary.Base64;
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
 * Created by agutierrez on 3/11/15.
 */
public class FMCSyncStatusChangeJob implements Job {
    static Logger logger = Logger.getLogger(FMCSyncStatusChangeJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String key = context.getJobDetail().getKey().getName();
        Transaction transaction = null;
        try {
            Session session = HibernateSessionFactory.getInstance().getCurrentSession();
            String[] split = key.split("_");
            Integer nRetries = Integer.valueOf(split[1]);
            Long parentId = Long.valueOf(split[2]);
            String childSerial = new String(Base64.decodeBase64(split[3]), Charsets.UTF_8);
            Long userId = Long.valueOf(split[4]);
            Long zoneId = Long.valueOf(split[5]);
            Long timeStamp = Long.valueOf(split[6]);
            String tCode = split[7];
            Long operationId = Long.valueOf(split[8]);
            Long sapId = Long.valueOf(split[9]);

            transaction = session.getTransaction();
            transaction.begin();
            try {
                RiotShiroRealm.initCaches();
                ApiKeyToken token = new ApiKeyToken(UserService.getInstance().getUserDAO().selectBy(QUser.user.id.eq(userId)).getApiKey());
                PermissionsUtils.loginUser(token);

                statusChange(nRetries, parentId, childSerial, userId, zoneId, timeStamp, tCode, operationId, sapId);
            } catch (Exception ex) {
                nRetries++;
                User user = UserService.getInstance().get(userId);
                long startTime = System.currentTimeMillis() + (nRetries == 0 ? 500 : getMinimumTimeSecondsToWaitBetweenRetries(user));
                SapMongoDAO.getInstance().addNRetries(sapId, startTime, ex.getMessage(), nRetries);
                reschedule(nRetries, parentId, childSerial, userId, zoneId, timeStamp, tCode, operationId, sapId);
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

    public void statusChange(int nRetries, Long parentId, String childSerial, Long userId, Long zoneId, Long timestamp, String tCode, long operationId, long sapId) throws Exception {
        Thing parent = ThingService.getInstance().get(parentId);
        User user = UserService.getInstance().get(userId);
        Zone zone = ZoneService.getInstance().get(zoneId);
        Date sapSyncStoreDate = new Date();
        Date handHeldEventDate = new Date(timestamp);
        Map message = new HashMap<>();
        Map messageDetail = new HashMap<>();
        message.put("RFIDEquipmentStatus_MT", messageDetail);
        messageDetail.put("TagID", childSerial);
        messageDetail.put("EquipmentNum", parent.getSerial());
        messageDetail.put("DateTime", getDate(timestamp));
        messageDetail.put("ScanZone", zone.getCode().toUpperCase());
        messageDetail.put("Plant", zone.getGroup().getCode());
        messageDetail.put("User", user.getUsername());
        fmcHandleSapSendMessage(EVENT_TYPE_CHANGE_STATUS, message, messageDetail, parent, user, sapSyncStoreDate, handHeldEventDate, nRetries, tCode, operationId, sapId);
    }

    public static void reschedule(int retry, Long parentId, String childSerial, Long userId, Long zoneId, Long timeStamp, String tCode, long operationId, long sapId) {
        logger.info("StatusChangeJOB");
        if (parentId != null && childSerial != null && userId != null && zoneId != null) {
            User user = UserService.getInstance().get(userId);
            long startTime = System.currentTimeMillis() + (retry == 0 ? 500 : getMinimumTimeSecondsToWaitBetweenRetries(user));
            String jobKey = retry + "_" + parentId + "_" + new String(Base64.encodeBase64(childSerial.getBytes(Charsets.UTF_8)), Charsets.UTF_8)
                    + "_" + userId + "_" + zoneId + "_" + timeStamp + "_"+tCode + "_" + operationId + "_" +sapId;
            JobKey jobKey1 = new JobKey("fmcSyncSapStatusChangeJob_" + jobKey, "fmcSyncSAPJob");
            if (retry < getMaxNumberOfRetries(user)) {
                JobDetail job = newJob(FMCSyncStatusChangeJob.class)
                        .withIdentity("fmcSyncSapStatusChangeJob_" + jobKey, "fmcSyncSAPJob")
                        .build();

                Trigger trigger = newTrigger()
                        .withIdentity("fmcSyncSapStatusChangeTrigger_" + jobKey, "fmcSyncSAPJob")
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
                logger.error("Task fmcSyncSapStatusChangeJob_"+jobKey+ " cannot be reschedule the maximum # of retries has been used ");
                SapMongoDAO.getInstance().setPendingOnNewDate(sapId);
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("1231_asdfsd".split("_")[0]);
    }

}
