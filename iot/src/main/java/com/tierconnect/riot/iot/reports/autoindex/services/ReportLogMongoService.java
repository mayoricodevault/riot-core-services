package com.tierconnect.riot.iot.reports.autoindex.services;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.reports.autoindex.IndexCreatorManager;
import com.tierconnect.riot.iot.reports.autoindex.dao.ReportLogMongoDAO;
import com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ListReportLog;
import com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogInfo;
import com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogStatus;
import com.tierconnect.riot.sdk.utils.HashUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.util.*;


import static com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogStatus.*;


/**
 * Created by rsejas on 2/1/17.
 * Modified by achambi on 31/3/17
 * Class to manage The business logic of report log.
 */
public class ReportLogMongoService {
    private static Logger logger = Logger.getLogger(ReportLogMongoService.class);
    private static ReportLogMongoService instance = new ReportLogMongoService();

    static private ReportLogMongoDAO reportLogMongoDAO = ReportLogMongoDAO.getInstance();

    private static ReportLogMongoDAO getReportLogMongoDAO() {
        return reportLogMongoDAO;
    }

    public static ReportLogMongoService getInstance() {
        return instance;
    }

    /**
     * Set Instance for Mock test
     */
    public static void setInstance(ReportLogMongoService instanceField) {
        instance = instanceField;
    }

    public void upsert(ReportLogInfo reportLogInfo, boolean reportLogEnable, long reportLogThreshold) throws
            IOException {
        if (reportLogEnable && reportLogInfo.getMaxDuration() >= reportLogThreshold) {
            getReportLogMongoDAO().upsert(reportLogInfo);
        }
    }

    public void upsert(Map<String, Object> reportLogMap, ReportDefinition reportDefinition, long reportLogThreshold,
                       long userId) {
        long duration = 0L;

        if (reportLogMap.get("duration") != null) {
            duration = Long.parseLong(reportLogMap.get("duration").toString());
        }

        if (duration >= reportLogThreshold) {
            String id = createId(reportDefinition.getId(), (String) reportLogMap.get("collectionName"), (String)
                    reportLogMap.get("filtersDefinition"));
            DBObject reportLog = getReportLog(id);
            if (reportLog == null) {
                reportLog = convertMapToObject(reportLogMap, reportDefinition, userId, id);
                insert(reportLog);
                /*call to query optimizer*/
                IndexCreatorManager.getInstance().executeIndexCreation(reportLog);
            } else {
                switch (ReportLogStatus.getEnum(reportLog.get("status").toString())) {
                    case IN_PROGRESS:
                        logger.info("ReportLog is not inserted because there is an in-process record!");
                        break;
                    case COMPLETED:
                        reportLog.put("status", SLOW_INDEX.getValue());
                        update(reportLog, reportLogMap, userId, id);
                        break;
                    case SLOW_INDEX:
                        logger.info("ReportLog has not been inserted because it has a slow index!");
                        break;
                    default:
                        reportLog.put("status", PENDING.getValue());
                        reportLog.put("checked", Boolean.FALSE);
                        reportLog.put("message", "");
                    case PENDING:
                        update(reportLog, reportLogMap, userId, id);
                        /*call to query optimizer*/
                        IndexCreatorManager.getInstance().executeIndexCreation(reportLog);
                        break;
                }
            }
        }
    }

    private String createId(Long reportId, String collectionName, String filtersDefinition) {
        return StringUtils.leftPad(reportId.toString(), 5, "0") + "-" + HashUtils.hashSHA256(collectionName +
                filtersDefinition);
    }

    private void insert(DBObject reportLog) {
        //Insert MongoDB
        MongoDAOUtil.getInstance().reportLogs.insert(reportLog);
        logger.info("\n\n**** REPORT LOG WAS CREATED ****\n\n");
    }

    private void update(DBObject reportLog, Map<String, Object> reportLogMap, long userId, String id) {
        BasicDBList executionList = (BasicDBList) reportLog.get("runs");
        if (executionList == null) {
            executionList = new BasicDBList();
        }
        Long totalRuns = 1L;
        if (reportLog.get("totalRuns") != null) {
            totalRuns = Long.valueOf(reportLog.get("totalRuns").toString());
        }
        totalRuns++;
        reportLog.put("totalRuns", totalRuns);
        reportLog.put("lastRunDate", new Date());
        DBObject execution = getExecution(reportLogMap, userId);
        Long maxDuration = Long.valueOf(reportLog.get("maxDuration").toString());
        Long duration = Long.valueOf(execution.get("duration").toString());
        if (duration > maxDuration) {
            reportLog.put("maxDuration", duration);
            reportLog.put("maxDurationDate", execution.get("date"));
            reportLog.put("maxDurationId", execution.get("id"));
        }
        executionList.add(0, execution);
        reportLog.put("runs", executionList);
        DBObject query = new BasicDBObject("_id", id);
        MongoDAOUtil.getInstance().reportLogs.update(query, reportLog);
        logger.info("REPORT LOG WAS UPDATED STATUS: " + ReportLogStatus.getEnum(reportLog.get("status").toString()));
    }

    void updateStatus(String id, ReportLogStatus status) {
        getReportLogMongoDAO().updateStatus(id, status);
    }

    public void updateStatusByIndexName(String indexName, ReportLogStatus status, String message) {
        getReportLogMongoDAO().updateStatusByIndexName(indexName, status, message);
    }

    public void updateStatusByIndexName(String indexName,
                                        Date endDate, ReportLogStatus statusToChange, ReportLogStatus statusToSearch,
                                        String message) {
        getReportLogMongoDAO().updateStatusAndIndexInformationByIndexName(
                indexName,
                statusToChange,
                statusToSearch,
                message,
                endDate);
    }

    public void updateStatusByAssociatedIndex(String associatedIndex,
                                              Date endDate, ReportLogStatus statusToChange, ReportLogStatus statusToSearch,
                                              String message) {
        getReportLogMongoDAO().updateStatusByAssociatedIndex(
                associatedIndex,
                statusToChange,
                statusToSearch,
                message,
                endDate);
    }

    public void updateStatusAndIndexInformation(String id,
                                                ReportLogStatus status,
                                                String indexName,
                                                String indexDefinition,
                                                Date startDate,
                                                Date endDate) {
        getReportLogMongoDAO().updateStatusAndIndexInformation(
                id,
                status,
                indexName,
                indexDefinition,
                startDate,
                endDate);
    }

    public void updateStatusAndAssociateIndex(String id,
                                              ReportLogStatus status,
                                              Date endDate,
                                              String indexToAssociate) {
        getReportLogMongoDAO().updateStatusAndAssociateIndex(
                id,
                status,
                endDate,
                indexToAssociate);
    }

    private DBObject convertMapToObject(Map<String, Object> reportLogMap, ReportDefinition reportDefinition,
                                        long userId, String id) {
        Date date = new Date();
        DBObject reportLog = new BasicDBObject();
        // Setting general data
        reportLog.put("_id", id);
        reportLog.put("type", reportDefinition.getReportTypeInView());
        reportLog.put("name", reportDefinition.getName());
        reportLog.put("collectionName", reportLogMap.get("collectionName"));
        reportLog.put("lastRunDate", date);
        reportLog.put("maxDuration", reportLogMap.get("duration"));
        reportLog.put("maxDurationDate", date);
        reportLog.put("totalRuns", 1);
        reportLog.put("filtersDefinition", reportLogMap.get("filtersDefinition"));
        reportLog.put("query", reportLogMap.get("query"));
        reportLog.put("status", PENDING.getValue());
        reportLog.put("checked", Boolean.FALSE);
        DBObject execution = getExecution(reportLogMap, userId);
        reportLog.put("maxDurationId", execution.get("id"));
        // Setting execution data
        BasicDBList executionList = new BasicDBList();
        executionList.add(execution);
        reportLog.put("runs", executionList);
        return reportLog;
    }

    @SuppressWarnings("unchecked")
    private DBObject getExecution(Map<String, Object> reportLogMap, long userId) {
        Date date = new Date();

        DBObject execution = new BasicDBObject();
        String fields[] = {"start", "end", "duration", "filtersDefinition", "query", "count", "collectionName", "sort"};
        for (String field : fields) {
            if (reportLogMap.containsKey(field)) {
                if (reportLogMap.get(field) instanceof Map) {
                    execution.put(field, reportLogMap.get(field).toString());
                } else {
                    execution.put(field, reportLogMap.get(field));
                }
            }
        }
        execution.put("id", new ObjectId());
        execution.put("userID", userId);
        execution.put("date", date);
        Map<String, Object> requestInfo = (Map<String, Object>) reportLogMap.get("requestInfo");
        String queryString = "";
        Object body = "";
        try {
            queryString = (((Map) requestInfo.get("input")).entrySet().toArray()[0]).toString();
            queryString = queryString.replaceAll("queryString=", "");
            body = ((Map<String, Object>) requestInfo.get("input")).get("body");
        } catch (Exception e) {
            logger.warn("error getting queryString");
        }
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("queryString", queryString);
        additionalInfo.put("header", requestInfo.get("header"));
        additionalInfo.put("body", body);
        execution.put("additionalInfo", additionalInfo);
        return execution;
    }

    private DBObject getReportLog(String id) {
        BasicDBObject q = new BasicDBObject("_id", id);
        return MongoDAOUtil.getInstance().reportLogs.find(q).one();
    }

    public List<ReportLogInfo> getInProgress() throws IOException {
        return getReportLogMongoDAO().getByStatus(IN_PROGRESS);
    }

    public ReportLogInfo getReportLogObject(String id) throws IOException {
        return getReportLogMongoDAO().get(id);
    }

    public ReportLogInfo searchReportLogForCancel(String id) throws Exception {
        ReportLogInfo reportLogInfo = getReportLogObject(id);
        if (reportLogInfo == null) {
            throw new Exception("There is no index in process with id:" + id);
        }
        switch (reportLogInfo.getStatus()) {
            case PENDING:
                throw new Exception("The creation of the index with id : '" + id + "' could not be canceled because " +
                        "it wasn't started.");
            case COMPLETED:
                throw new Exception("The creation of the index with id : '" + id + "' could not be canceled because " +
                        "it is completed.");
            case CANCELED:
                throw new Exception("The creation of the index with id : '" + id + "' could not be canceled because " +
                        "it was already canceled");
        }
        return reportLogInfo;
    }

    public List<ReportLogInfo> listIndexPendingAndInProcess(Long reportId, User currentUser) throws IOException {
        String partialId = StringUtils.leftPad(reportId.toString(), 5, "0") + "-";
        DateFormatAndTimeZone dateTimeZone = UserService.getInstance().getDateFormatAndTimeZone(currentUser);
        return getReportLogMongoDAO().listIndexByStatusReportLog(partialId, dateTimeZone, PENDING.getValue(),
                IN_PROGRESS.getValue());
    }

    public boolean isCurrentlyIndexing(Long reportId) {
        String partialId = StringUtils.leftPad(reportId.toString(), 5, "0") + "-";
        return getReportLogMongoDAO().isCurrentlyIndexing(partialId, PENDING.getValue(), IN_PROGRESS.getValue());
    }

    public ListReportLog listIndexStatusReportLog(Long reportId, User currentUser) throws IOException {
        String partialId = StringUtils.leftPad(reportId.toString(), 5, "0") + "-";
        DateFormatAndTimeZone dateTimeZone = UserService.getInstance().getDateFormatAndTimeZone(currentUser);
        List<ReportLogInfo> reportLogInfoList = getReportLogMongoDAO().listIndexByStatusReportLog(partialId,
                dateTimeZone,
                IN_PROGRESS.getValue(),
                COMPLETED.getValue(),
                CANCELED.getValue(),
                SLOW_INDEX.getValue());
        return new ListReportLog(reportLogInfoList);
    }

    public void insertLog(Map<String, Object> reportData,
                          ReportDefinition reportDefinition,
                          Map<String, Object> requestInfo,
                          long reportLogThreshold,
                          boolean reportLogEnable,
                          long userId) {
        if (reportLogEnable) {
            reportData.put("requestInfo", requestInfo);
            logger.info("\n========== REPORT LOG ==========\n" + reportData.toString() + "\n=======================");
            upsert(reportData, reportDefinition, reportLogThreshold, userId);
        } else {
            logger.info("reportLog is disabled!");
        }
    }

    public String getIndex(Long reportId, String collectionName, String filtersDefinition) {
        DBObject reportLog = getReportLog(createId(reportId, collectionName, filtersDefinition));
        if (reportLog != null &&
                reportLog.get("indexInformation") != null &&
                (COMPLETED.getValue().equals(reportLog.get("status")) ||
                        SLOW_INDEX.getValue().equals(reportLog.get("status")))) {
            logger.info("Obtaining the index information from the reportLog.Id: " + reportLog.get("_id"));
            DBObject indexInformation = (DBObject) reportLog.get("indexInformation");
            String indexName = indexInformation.get("indexName").toString();
            Object associated = indexInformation.get("associatedIndex");
            if (associated != null) {
                indexName = associated.toString();
            }
            return indexName;
        }
        return null;
    }

    /**
     * Method that gets the index name by filter definition.
     *
     * @param filtersDefinition A {@link String} containing the filter definition.
     * @return the index name.
     */
    public String getIndexByDefinition(String filtersDefinition) throws IOException {
        String indexName = StringUtils.EMPTY;
        ReportLogInfo reportLogInfo = getReportLogMongoDAO().getByIdAndStatus(filtersDefinition);
        if (reportLogInfo != null && reportLogInfo.getIndexInformation() != null) {
            indexName = reportLogInfo.getIndexInformation().getIndexName();
            String associatedIndex = reportLogInfo.getIndexInformation().getAssociatedIndex();
            if (associatedIndex != null) {
                indexName = associatedIndex;
            }
        }
        return indexName;
    }

    public List<Map<String, Object>> getByIndexName(String indexName, String excludeId) throws IOException {
        List<Map<String, Object>> response = new LinkedList<>();
        for (ReportLogInfo item : getReportLogMongoDAO().getByIndexName(indexName, null)) {
            if (!item.getId().equals(excludeId)) {
                response.add(item.getMap());
            }
        }
        return response;
    }

    String[] getIndexNamesByStatus(ReportLogStatus... reportLogStatus) throws IOException {
        String[] status = new String[reportLogStatus.length];
        for (int i = 0; i < reportLogStatus.length; i++) {
            status[i] = reportLogStatus[i].getValue();
        }
        return getReportLogMongoDAO().getIndexNamesByStatus(status);
    }

    public void markAsChecked(String id) throws IOException {
        DBObject reportLog = getReportLog(id);
        Boolean checked = (Boolean) reportLog.get("checked");
        if (!checked && (COMPLETED.getValue().equals(reportLog.get("status"))
                || CANCELED.getValue().equals(reportLog.get("status")))) {
            getReportLogMongoDAO().updateCheckedValue(id, Boolean.TRUE);
        }
    }
}
