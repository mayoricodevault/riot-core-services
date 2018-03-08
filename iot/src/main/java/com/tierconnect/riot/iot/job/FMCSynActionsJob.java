package com.tierconnect.riot.iot.job;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.job.JobUtils;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.dao.mongo.SapMongoDAO;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.quartz.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * Created by rsejas on 4/20/16.
 */
public class FMCSynActionsJob implements Job {

    static Logger logger = Logger.getLogger(FMCSynActionsJob.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Transaction transaction = null;
        try {
            Session session = HibernateSessionFactory.getInstance().getCurrentSession();
            transaction = session.getTransaction();
            transaction.begin();
            try {
                DBObject query = new BasicDBObject("status", "error");
                DBObject sortBy = new BasicDBObject("nextCheckDate", 1);
                DBCursor cursor = MongoDAOUtil.getInstance().sapCollection.find(query).sort(sortBy);
                if (!cursor.hasNext()) {
                    query.put("status", "syncing");
                    cursor = MongoDAOUtil.getInstance().sapCollection.find(query).sort(sortBy);
                    if (!cursor.hasNext()) {
                        query.put("status", "Pending");
                        cursor = MongoDAOUtil.getInstance().sapCollection.find(query).sort(sortBy);
                        if (cursor.hasNext()) {
                            DBObject sap = cursor.next();
                            java.util.Calendar sapCheckDate = Calendar.getInstance();
                            sapCheckDate.add(Calendar.SECOND, -3);
                            if (sap.containsField("nextCheckDate")) {
                                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault());
                                sapCheckDate.setTime(sdf.parse(sap.get("nextCheckDate").toString()));
                            }
                            if (Calendar.getInstance().after(sapCheckDate)) {
                                // updating sap
                                sap.put("status", "syncing");
                                Long sapId = Long.valueOf(sap.get("_id").toString());
                                logger.info("Processing: "+sapId);
                                query.put("_id", sapId);
                                MongoDAOUtil.getInstance().sapCollection.findAndModify(query, null, null, false, new BasicDBObject("$set", sap), false, true);

                                // Getting values
                                int retry = Integer.parseInt(sap.get("retry").toString());
                                Long parentId = Long.parseLong(sap.get("parentId").toString());
                                String childSerial = sap.get("childSerial").toString();
                                Long userId = Long.parseLong(sap.get("userId").toString());
                                Long groupId = Long.parseLong(sap.get("groupId").toString());
                                Long timestamp = Long.parseLong(sap.get("timestamp").toString());
                                String tCode = sap.get("tCode").toString();
                                Long operationId = Long.parseLong(sap.get("operationId").toString());
                                switch (sap.get("action").toString()) {
                                    case "assign":
                                        FMCSyncAssignJob.reschedule(retry, parentId, childSerial, userId, groupId, timestamp, tCode, operationId, sapId);
                                        break;
                                    case "unAssign":
                                        FMCSyncUnAssignJob.reschedule(retry, parentId, childSerial, userId, groupId, timestamp, tCode, operationId, sapId);
                                        break;
                                    case "statusChange":
                                        FMCSyncStatusChangeJob.reschedule(retry, parentId, childSerial, userId, groupId, timestamp, tCode, operationId, sapId);
                                        break;
                                    case "delete":
                                        FMCSyncDeleteJob.reschedule(retry, childSerial, userId, groupId, timestamp, tCode, operationId, sapId);
                                        break;
                                }
                            }
                        } else {
                            logger.debug("queue is empty");
                        }
                    } else {
                        logger.info("An item is syncing");
                    }
                    RiotShiroRealm.initCaches();
//                ApiKeyToken token = new ApiKeyToken(UserService.getInstance().getUserDAO().selectBy(QUser.user.id.eq(userId)).getApiKey());
//                PermissionsUtils.loginUser(token);
                } else {
                    logger.info("There are error on sync");
                    DBObject sapError = cursor.next();
                    logger.info("SAP ID:" + sapError.get("_id"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            transaction.commit();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            HibernateDAOUtils.rollback(transaction);
        }
    }

    public static void init() {
        try {
            logger.info("FMCSyncActionsJob was initialized");
            updateToPending();
            schedule();
        } catch (Exception e) {
            logger.error("Error initializing job");
            e.printStackTrace();
        }
    }

    private static void updateToPending() {
        DBObject query = new BasicDBObject("$or", Arrays.asList(new BasicDBObject("status","error"),
                                                                new BasicDBObject("status","syncing")));
        DBCursor cursor = MongoDAOUtil.getInstance().sapCollection.find(query);
        for (DBObject anObject:cursor) {
            Long sapId = Long.valueOf(anObject.get("_id").toString());
            Map<String, Object> statusMap = new HashMap<>();
            statusMap.put("status", "Pending");
            SapMongoDAO.getInstance().updateValues(sapId, statusMap);
        }
    }

    private static void schedule() {
        JobDetail job = newJob(FMCSynActionsJob.class)
                .withIdentity("FMCSyncActionsJob", "FMCSyncActionsJob")
                .build();
        Trigger trigger = newTrigger()
                .withIdentity("FMCSyncActionsJob_Trigger", "FMCSyncActionsJob")
                .withSchedule(cronSchedule("0,10,20,30,40,50 * * * * ?"))
                .build();
        try {
            JobUtils.getScheduler().scheduleJob(job, trigger);
            logger.info("FMCSyncActions scheduled");
        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }
}
