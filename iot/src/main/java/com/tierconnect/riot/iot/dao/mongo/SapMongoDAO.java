package com.tierconnect.riot.iot.dao.mongo;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.sdk.dao.MongoExecutionException;
import org.jboss.logging.Logger;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by rsejas on 4/19/16.
 */
public class SapMongoDAO {

    static SapMongoDAO instance;

    private static Logger logger = Logger.getLogger(SapMongoDAO.class);

    static {
        instance = new SapMongoDAO();
    }

    public void addNRetries(Long sapId, long startTime, String message, int retry) {
        BasicDBObject query = new BasicDBObject("_id", sapId);
        try {
            DBCursor cursor = MongoDAOUtil.getInstance().sapCollection.find(query);
            DBObject oldSap = null;
            if (cursor.hasNext()) {
                oldSap = cursor.next();
                oldSap.put("retry", retry);
                oldSap.put("status", "syncing");
                BasicDBList sapResponse = new BasicDBList();
                try {
                    if (oldSap.containsField("SAP_Response"))
                        sapResponse = (BasicDBList) oldSap.get("SAP_Response");
                } catch (ClassCastException e) {
                    logger.debug("first test");
                }
                DBObject newAnswer = new BasicDBObject();
                newAnswer.put("time Next Retry", new Date(startTime));
                newAnswer.put("message", message);
                sapResponse.add(0, newAnswer);
                oldSap.put("SAP_Response", sapResponse);
                MongoDAOUtil.getInstance().sapCollection.findAndModify(query, null, null, false, new BasicDBObject("$set", oldSap), false, true);
            } else {
                logger.debug(sapId + " ID not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setPendingOnNewDate(Long sapId) {
        logger.info("Setting new Date");
        try {
            DBObject query = new BasicDBObject("_id", sapId);
            DBCursor cursor = MongoDAOUtil.getInstance().sapCollection.find(query);
            DBObject oldSap = null;
            if (cursor.hasNext()) {
                logger.info("SAP_Id was found: "+sapId);
                oldSap = cursor.next();
                List<String> statusList = new ArrayList<>(Arrays.asList("error","Pending", "syncing"));
                BasicDBObject querySerial = new BasicDBObject();
                querySerial.put("serial", oldSap.get("serial"));
                querySerial.append("status", new BasicDBObject("$in", statusList));
                logger.info("Before to query");
                DBCursor cursorSerial = MongoDAOUtil.getInstance().sapCollection.find(querySerial).sort(new BasicDBObject("nextCheckDate", 1));
                logger.info("Records to update: "+cursorSerial.count());
                Calendar checkDate = Calendar.getInstance();
                checkDate.add(Calendar.DAY_OF_MONTH, 1);
                for (DBObject sap:cursorSerial) {
                    logger.info("trying to process " + sap.get("_id") + " serial: "+sap.get("serial"));
                    query = new BasicDBObject("_id", Long.valueOf(sap.get("_id").toString()));
                    DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
                    sap.put("status", "Pending");
                    sap.put("nextCheckDate", df.format(checkDate.getTime()));
                    sap.put("retry", 0);
                    MongoDAOUtil.getInstance().sapCollection.findAndModify(query, null, null, false, new BasicDBObject("$set", sap), false, true);
                    checkDate.add(Calendar.SECOND, 1);
                }
            } else {
                logger.info(sapId + " ID not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public enum comparator{$eq, $ne, $lt, $lte, $gt, $gte}

    public static comparator getComparator(String comparator) {
        switch (comparator) {
            case "=":
                return SapMongoDAO.comparator.$eq;
            case "<>":
                return SapMongoDAO.comparator.$ne;
            case "<":
                return SapMongoDAO.comparator.$lt;
            case "<=":
                return SapMongoDAO.comparator.$lte;
            case ">":
                return SapMongoDAO.comparator.$gt;
            case ">=":
                return SapMongoDAO.comparator.$gte;
            default:
                return SapMongoDAO.comparator.$eq;
        }
    }

    public static SapMongoDAO getInstance() {
        return instance;
    }

    public void insertSap(String action, int retry, Long parentId, String childSerial, Long userId, Long groupId,
                          Long timestamp, String tCode, long operationId, String serial) throws MongoExecutionException {
        try {
            insertSap(action, retry, parentId, childSerial, userId, groupId, timestamp, tCode, operationId, "Pending", serial);
        } catch (Exception e) {
            throw new MongoExecutionException(e.getMessage(), e);
        }
    }

    /**
     *
     * @param action for sync with SAP (associate, disassociate, changeStatus, delete)
     * @param retry Retries number
     * @param parentId Parent thing ID
     * @param childSerial Child's serial Number
     * @param userId User ID
     * @param groupId Group ID
     * @param timestamp
     * @param tCode
     * @param operationId
     * @param status
     * @throws MongoExecutionException
     */
    private void insertSap(String action, int retry, Long parentId, String childSerial, Long userId, Long groupId,
                           Long timestamp, String tCode, long operationId, String status, String serial) throws MongoExecutionException {
        try {
            DBObject queryParentId = new BasicDBObject("serial", serial);
            DBCursor cursorSerial = MongoDAOUtil.getInstance().sapCollection.find(queryParentId).sort(new BasicDBObject("nextCheckDate", -1));
            DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
            Calendar checkDate = Calendar.getInstance();
            if (cursorSerial.hasNext()) {
                DBObject sap = cursorSerial.next();
                checkDate = Calendar.getInstance();
                if (sap.containsField("nextCheckDate")) {
                    checkDate.setTime(df.parse(sap.get("nextCheckDate").toString()));
                }
                checkDate.add(Calendar.SECOND, 1);
            } else {
                checkDate.setTime(new Date(timestamp));
            }
            BasicDBObject doc = new BasicDBObject();
            doc.append("_id", new Date().getTime());
            doc.append("action", action);
            doc.append("retry", retry);
            doc.append("serial", serialFrom(action, childSerial, serial));
            doc.append("parentId", parentId);
            doc.append("childSerial", childSerial);
            doc.append("userId", userId);
            doc.append("groupId", groupId);
            doc.append("timestamp", timestamp);
            doc.append("tCode", tCode);
            doc.append("operationId", operationId);
            doc.append("status", status);

            doc.append("nextCheckDate", df.format(checkDate.getTime()));

            MongoDAOUtil.getInstance().sapCollection.insert(doc);
            logger.debug("sap inserted");
        } catch (Exception e) {
            throw new MongoExecutionException(e.getMessage(), e);
        }
    }

    private String serialFrom(String action, String childSerial, String serial) {
        return ("delete".equals(action))?childSerial:serial;
    }

    public void updateValues(Long sapId, Map<String, Object> values) {
        try {
            DBObject query = new BasicDBObject("_id", sapId);
            DBCursor cursor = MongoDAOUtil.getInstance().sapCollection.find(query);
            DBObject oldSap = null;
            if (cursor.hasNext()) {
                oldSap = cursor.next();
                for (Map.Entry<String, Object> entry:values.entrySet()) {
                    oldSap.put(entry.getKey(), entry.getValue());
                }
                MongoDAOUtil.getInstance().sapCollection.findAndModify(query, null, null, false, new BasicDBObject("$set", oldSap), false, true);
            } else {
                logger.info(sapId + " ID not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateSapOKorError(Long sapId, Map<String, Object> answer, String status) {
        BasicDBObject query = new BasicDBObject("_id", sapId);
        try {
            DBCursor cursor = MongoDAOUtil.getInstance().sapCollection.find(query);
            DBObject oldSap = null;
            if (cursor.hasNext()) {
                oldSap = cursor.next();
                BasicDBList sapAnswer = new BasicDBList();
                try {
                    if (oldSap.containsField("SAP_Response"))
                        sapAnswer = (BasicDBList) oldSap.get("SAP_Response");
                } catch (ClassCastException e) {
                    logger.debug("first test");
                }
                DBObject newAnswer = new BasicDBObject();
                newAnswer.put("status", status);
                newAnswer.put("SAP_Response", answer);
                newAnswer.put("Message Date", new Date());
                sapAnswer.add(0, newAnswer);
                oldSap.put("SAP_Response", sapAnswer);
                oldSap.put("status", status);
                MongoDAOUtil.getInstance().sapCollection.findAndModify(query, null, null, false, new BasicDBObject("$set", oldSap), false, true);
                if ("error".equals(status.trim().toLowerCase())) {
                    logger.info("Updating error to pending status");
                    this.setPendingOnNewDate(sapId);
                }
            } else {
                logger.info(sapId + " ID not found");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Long id =  1461100913418L;
        Long end = new Date().getTime();
        Map<String, Object> answer = new HashMap<>();
        answer.put("Description","7 1/16\"-10K, Manual GV Assy VLT");
        answer.put("UserStatus","0003");
        answer.put("SystemStatus","AVLB");
        answer.put("AssetNum","000006011655");
        answer.put("ValidFromDate","2014-12-18");
        answer.put("ValidToDate","9999-12-31");
        answer.put("SerialNum","300529879-1");
        answer.put("MaterialNum","REM-P1000056421");
        answer.put("CategoryCode","E");
        answer.put("Owner","7393");
        answer.put("Administrator","7393");
        answer.put("CurrentLocation",null);
        answer.put("MaintPlant","7393");
        answer.put("ServiceCallStatus","1");
        answer.put("ErrorMessage","");
        answer.put("Time","start " + id + " end " + end +" elapsed " + convertToDays(end - id));
        SapMongoDAO.getInstance().updateSapOKorError(1461100913418L, answer, "success");
    }

    private static String convertToDays(long l) {
        long fms = 1000;
        long fmm = 60;
        long fhh = 3600;
        long fdd = 86400;
        long ms = l % fms;
        l = (int)(l / fms);
        long dd = (int) (l / fdd);
        l = l % fdd;
        long hh = (int) (l / fhh);
        l = l % fhh;
        long mm = (int) (l / fmm);
        long ss = l % 60;
        return dd + "dd " + hh + "hh " + mm + "mm " + ss + "ss " + ms + "ms";
    }
}
