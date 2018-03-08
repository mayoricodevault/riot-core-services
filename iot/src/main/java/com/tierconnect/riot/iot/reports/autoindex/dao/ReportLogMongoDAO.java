package com.tierconnect.riot.iot.reports.autoindex.dao;

import com.mongodb.*;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.IndexDescription;
import com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogInfo;
import com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogStatus;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by rsejas on 2/1/17.
 * Modified by achambi
 */
public class ReportLogMongoDAO {

    /**
     * Singleton instance.
     */
    private static ReportLogMongoDAO instance;
    /**
     * Variable for  logs.
     */
    private static Logger logger = Logger.getLogger(ReportLogMongoDAO.class);
    /**
     * Collection for save report logs in mongo.
     */
    private DBCollection reportLogCollection;

    static {
        instance = new ReportLogMongoDAO();
        instance.setup();
    }

    /**
     * Create a a system collection to save all functions.
     */
    public void setup() {
        reportLogCollection = MongoDAOUtil.getInstance().db.getCollection("reportLogs");
    }

    /**
     * Get Singleton Instance.
     *
     * @return return instance.
     */
    public static ReportLogMongoDAO getInstance() {
        return instance;
    }

    /**
     * Method for insert a report log object in mongo.
     *
     * @param reportLogInfo Object to insert.
     */
    public void insert(ReportLogInfo reportLogInfo) {
        WriteResult result = reportLogCollection.insert(reportLogInfo);
        logger.info("\n\n**** REPORT LOG WAS CREATED ****\n\n");
        logger.info(result);
    }

    /**
     * Get all Report log in database.
     *
     * @return A instance of {@link List}<{@link ReportLogInfo}>
     */
    public List<ReportLogInfo> getAll() throws IOException {
        List<ReportLogInfo> listReportLogInfo = new LinkedList<>();
        DBCursor reportLogs = reportLogCollection.find();
        while (reportLogs.hasNext()) {
            listReportLogInfo.add(new ReportLogInfo(reportLogs.next(), null));
        }
        return listReportLogInfo;
    }

    /**
     * Method for get report saved in database.
     *
     * @param id A {@link String} that contains a hash compose for a correlation id and parameters.
     * @return A {@link ReportLogInfo} instance or null if not found the record.
     * @throws IOException if the result cannot convert to {@link ReportLogInfo}
     */
    public ReportLogInfo get(String id) throws IOException {
        BasicDBObject queryFind = new BasicDBObject("_id", id);
        DBObject result = reportLogCollection.find(queryFind).one();
        return getResult(result);
    }

    /**
     * Method for get report saved in database.
     *
     * @param id              A {@link String} that contains a hash compose for a correlation id and parameters.
     * @param reportLogStatus The report log status to find.
     * @return A {@link ReportLogInfo} instance or null if not found the record.
     * @throws IOException if the result cannot convert to {@link ReportLogInfo}
     */
    @SuppressWarnings("unused")
    public ReportLogInfo getByIdAndStatus(String id, ReportLogStatus reportLogStatus) throws IOException {
        BasicDBObject queryFind = new BasicDBObject("_id", id);
        queryFind.append("status", reportLogStatus.getValue());
        DBObject result = reportLogCollection.find(queryFind).one();
        return getResult(result);
    }

    /**
     * @param definition A {@link String} containing the Index Definition.
     * @return A instance of  {@link ReportLogInfo}.
     * @throws IOException If input/output error exists.
     */
    //TODO: We need Create a unit test, Victor Angel Chambi Nina, 16/08/2017.
    public ReportLogInfo getByIdAndStatus(String definition) throws IOException {
        BasicDBObject queryFind = new BasicDBObject("indexInformation.definition", definition);
        DBObject result = reportLogCollection.find(queryFind).one();
        return getResult(result);
    }

    /**
     * Method for get report saved in database.
     *
     * @param reportLogStatus The report log status to find.
     * @return A {@link ReportLogInfo} instance or null if not found the record.
     * @throws IOException if the result cannot convert to {@link ReportLogInfo}
     */
    public List<ReportLogInfo> getByStatus(ReportLogStatus reportLogStatus) throws IOException {
        List<ReportLogInfo> listReportLogInfo = new LinkedList<>();
        BasicDBObject queryFind = new BasicDBObject("status", reportLogStatus.getValue());
        DBCursor reportLogs = reportLogCollection.find(queryFind);
        while (reportLogs.hasNext()) {
            listReportLogInfo.add(new ReportLogInfo(reportLogs.next(), null));
        }
        return listReportLogInfo;
    }

    /**
     * Method for get report saved in database.
     *
     * @param reportLogStatus The report log status to find.
     * @return A {@link String}[] array instance containing.
     * @throws IOException Input or Output Exception
     */
    public String[] getIndexNamesByStatus(String... reportLogStatus) throws IOException {
        BasicDBObject queryFind = new BasicDBObject("status", new BasicDBObject("$in", reportLogStatus));
        DBCursor cursor = reportLogCollection.find(queryFind);
        Set<String> reportLogIds = new TreeSet<>();
        while (cursor.hasNext()) {
            IndexDescription indexDescription = new ReportLogInfo(cursor.next(), null).getIndexInformation();
            if (indexDescription != null) {
                reportLogIds.add(indexDescription.getIndexName());
            }
        }
        String[] ids = new String[reportLogIds.size()];
        return reportLogIds.toArray(ids);
    }


    /**
     * @param partialId start pattern of the id
     * @param status    a list of status to filter the registers
     * @return A list of {@link ReportLogInfo}
     * @throws IOException if the result cannot convert to {@link ReportLogInfo}
     */
    public List<ReportLogInfo> listIndexByStatusReportLog(String partialId,
                                                          DateFormatAndTimeZone dateFormatAndTimeZone,
                                                          String... status) throws IOException {
        List<ReportLogInfo> listReportLogInfo = new LinkedList<>();
        Pattern compile = Pattern.compile("^" + partialId);
        BasicDBObject queryFind = new BasicDBObject("_id", compile);
        queryFind.append("checked", Boolean.FALSE);
        queryFind.append("status", new BasicDBObject("$in", status));
        DBCursor reportLogs = reportLogCollection.find(queryFind);
        while (reportLogs.hasNext()) {
            listReportLogInfo.add(new ReportLogInfo(reportLogs.next(), dateFormatAndTimeZone));
        }
        return listReportLogInfo;
    }

    /**
     * Method for verify if exist currently indexing.
     *
     * @param partialId start pattern of the report id
     * @param status    a list of status to filter the registers
     * @return boolean
     */
    public boolean isCurrentlyIndexing(String partialId,
                                       String... status) {
        Pattern compile = Pattern.compile("^" + partialId);
        BasicDBObject queryFind = new BasicDBObject("_id", compile);
        queryFind.append("checked", Boolean.FALSE);
        queryFind.append("status", new BasicDBObject("$in", status));
        return reportLogCollection.count(queryFind) > 0;
    }

    /**
     * Method for get report saved in database.
     *
     * @param indexName the index name to find in all report log collection.
     * @return A {@link ReportLogInfo} instance or null if not found the record.
     * @throws IOException if the result cannot convert to {@link ReportLogInfo}
     */
    public List<ReportLogInfo> getByIndexName(String indexName, DateFormatAndTimeZone dateFormatAndTimeZone) throws
            IOException {
        List<ReportLogInfo> reportLogInfoList = new LinkedList<>();
        BasicDBObject queryFind = new BasicDBObject("indexInformation.indexName", indexName);
        DBCursor result = reportLogCollection.find(queryFind);
        while (result.hasNext()) {
            DBObject doc = result.next();
            reportLogInfoList.add(new ReportLogInfo(doc, dateFormatAndTimeZone));
        }
        return reportLogInfoList;
    }

    /**
     * Method to get result
     *
     * @param result result log info in {@link DBObject} format.
     * @return A instance of {@link ReportLogInfo}.
     */
    private ReportLogInfo getResult(DBObject result) throws IOException {
        if (result == null) {
            return null;
        } else if (result instanceof BasicDBObject) {
            return new ReportLogInfo((BasicDBObject) result);
        } else {
            logger.error("The result cannot be convert in ReportLogInfo Class.");
            throw new IOException("The result cannot be convert in ReportLogInfo Class.");
        }
    }

    /**
     * Update ReportLogInfo
     *
     * @param reportLogInfo Instance of {@link ReportLogInfo}.
     */
    public void update(ReportLogInfo reportLogInfo) {
        DBObject queryFind = new BasicDBObject("_id", reportLogInfo.getId());
        WriteResult result = reportLogCollection.update(queryFind, reportLogInfo);
        logger.info("\n\n**** REPORT LOG WAS UPDATED ****\n\n");
        logger.info(result);
    }

    /**
     * Update only status and indexDescription fields
     *
     * @param id              the report log id to update.
     * @param status          A {@link String} that contains the status value.
     * @param indexName       A {@link String} that contains the indexName.
     * @param indexDefinition A {@link String} that contains the index definition in json format.
     */
    public void updateStatusAndIndexInformation(String id,
                                                ReportLogStatus status,
                                                String indexName,
                                                String indexDefinition,
                                                Date startDate,
                                                Date endDate) {
        DBObject queryFind = new BasicDBObject("_id", id);
        BasicDBObject fields = new BasicDBObject("status", status.getValue());
        fields.append("indexInformation.indexName", indexName);
        fields.append("indexInformation.definition", indexDefinition);
        if (startDate != null) {
            fields.append("indexInformation.starDate", startDate);
        }
        fields.append("indexInformation.endDate", endDate);
        DBObject dbObjectSet = new BasicDBObject("$set", fields);
        WriteResult result = reportLogCollection.update(queryFind, dbObjectSet);
        logger.info("\n\n**** REPORT LOG STATUS: " + status + " WAS UPDATED ****\n\n");
        logger.info(result);
    }

    public void updateStatusAndAssociateIndex(String id,
                                              ReportLogStatus status,
                                              Date endDate,
                                              String indexToAssociate) {
        DBObject queryFind = new BasicDBObject("_id", id);
        BasicDBObject fields = new BasicDBObject("status", status.getValue());
        if (endDate != null) {
            fields.append("indexInformation.endDate", endDate);
        }
        fields.append("indexInformation.associatedIndex", indexToAssociate);
        DBObject dbObjectSet = new BasicDBObject("$set", fields);
        WriteResult result = reportLogCollection.update(queryFind, dbObjectSet);
        logger.info("\n\n**** REPORT LOG STATUS: " + status + " WAS UPDATED ****\n\n");
        logger.info(result);
    }

    /**
     * Update only status field
     *
     * @param id     the report log id to update.
     * @param status a {@link String} new status value.
     */
    public void updateStatus(String id, ReportLogStatus status) {
        DBObject queryFind = new BasicDBObject("_id", id);
        DBObject dbObjectStatus = new BasicDBObject("$set", new BasicDBObject("status", status.getValue()));
        WriteResult result = reportLogCollection.update(queryFind, dbObjectStatus);
        logger.info("\n\n**** REPORT LOG STATUS: " + status + " WAS UPDATED ****\n\n");
        logger.info(result);
    }

    /**
     * Update only status field
     *
     * @param id    the report log id to update.
     * @param value a {@link String} new status value.
     */
    public void updateCheckedValue(String id, Boolean value) {
        DBObject queryFind = new BasicDBObject("_id", id);
        DBObject dbObjectStatus = new BasicDBObject("$set", new BasicDBObject("checked", value));
        WriteResult result = reportLogCollection.update(queryFind, dbObjectStatus);
        logger.info("\n\n**** REPORT LOG CHECKED: " + value + " WAS UPDATED ****\n\n");
        logger.info(result);
    }


    /**
     * Update only status field
     *
     * @param indexName A {@link String} containing the report log index name.
     * @param status    A {@link String} new status value.
     */
    public void updateStatusByIndexName(String indexName, ReportLogStatus status, String message) {
        DBObject queryFind = new BasicDBObject("indexInformation.indexName", indexName);
        new BasicDBObject("status", status).append("message", message);
        DBObject dbObjectUpdate = new BasicDBObject("$set",
                new BasicDBObject("status", status.getValue()).append("message", message));
        WriteResult result = reportLogCollection.update(queryFind, dbObjectUpdate, false, true);
        logger.info("\n\n**** REPORT LOG STATUS: " + status + " WAS UPDATED ****\n\n");
        logger.info(result);
    }

    /**
     * @param indexName      A {@link String} containing the report log index name.
     * @param statusToChange A {@link String} new status value.
     * @param statusToSearch A {@link String} status value to avoid on change.
     * @param message        The message to set in report log.
     * @param endDate        The date report log completed.
     */
    public void updateStatusAndIndexInformationByIndexName(String indexName, ReportLogStatus statusToChange,
                                                           ReportLogStatus statusToSearch,
                                                           String message, Date endDate) {
        DBObject queryFind = new BasicDBObject("indexInformation.indexName", indexName)
                .append("status", statusToSearch.getValue());

        BasicDBObject fields = new BasicDBObject("status", statusToChange.getValue()).append("message", message);
        fields.append("indexInformation.endDate", endDate);
        DBObject dbObjectUpdate = new BasicDBObject("$set", fields);
        WriteResult result = reportLogCollection.update(queryFind, dbObjectUpdate, false, true);
        logger.info("\n\n**** REPORT LOG STATUS: " + statusToChange + " WAS UPDATED ****\n\n");
        logger.info(result);
    }

    public void updateStatusByAssociatedIndex(String associatedIndex, ReportLogStatus statusToChange,
                                              ReportLogStatus statusToSearch,
                                              String message, Date endDate) {
        DBObject queryFind = new BasicDBObject("indexInformation.associatedIndex", associatedIndex)
                .append("status", statusToSearch.getValue());
        BasicDBObject fields = new BasicDBObject("status", statusToChange.getValue()).append("message", message);
        fields.append("indexInformation.endDate", endDate);
        DBObject dbObjectUpdate = new BasicDBObject("$set", fields);
        WriteResult result = reportLogCollection.update(queryFind, dbObjectUpdate, false, true);
        logger.info("\n\n**** REPORT LOG STATUS: " + statusToChange + " WAS UPDATED ****\n\n");
        logger.info(result);
    }

    /**
     * Update or Insert the report log Information.
     *
     * @param reportLogInfo A instance of {@link ReportLogInfo} to save.
     * @throws IOException If exists a error.
     */
    public void upsert(ReportLogInfo reportLogInfo) throws IOException {
        ReportLogInfo reportLogInfoSaved = get(reportLogInfo.getId());
        if (reportLogInfoSaved == null) {
            insert(reportLogInfo);
        } else {
            reportLogInfoSaved.merge(reportLogInfo);
            update(reportLogInfoSaved);
        }
    }
}
