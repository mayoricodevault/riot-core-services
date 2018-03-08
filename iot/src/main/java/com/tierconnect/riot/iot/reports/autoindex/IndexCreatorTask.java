package com.tierconnect.riot.iot.reports.autoindex;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.mongo.Mongo;
import com.tierconnect.riot.api.mongoShell.query.ResultFormat;
import com.tierconnect.riot.api.mongoShell.utils.FileUtils;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.controllers.ThingTypeController;
import com.tierconnect.riot.iot.dao.mongo.AutoIndexMongoDAO;
import com.tierconnect.riot.iot.reports.autoindex.entities.QueryField;
import com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogStatus;
import com.tierconnect.riot.iot.reports.autoindex.services.ReportLogMongoService;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.HashUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogStatus.IN_PROGRESS;

/**
 * Created by julio.rocha on 28-04-17.
 * IndexCreatorTask class
 */
public class IndexCreatorTask {
    private static Logger logger = Logger.getLogger(IndexCreatorTask.class);
    private static final int STATISTICS_COMPUTATION_TIMES = 3;
    private final String query;
    private final String queryWithoutValues;
    private final String sortField;
    private final String reportName;
    private final String realReportName;
    private final String collectionName;
    private String status;
    private final DBObject reportLog;

    public IndexCreatorTask(DBObject reportLog) {
        this.reportLog = reportLog;
        this.query = (String) reportLog.get("query");
        String sortTmp = ((String) ((DBObject) ((BasicDBList) reportLog.get("runs")).get(0)).get("sort"));
        this.sortField = (sortTmp != null) ? sortTmp.replaceAll("\\{", "").replaceAll("}", "").replaceAll("(:\\d)|(:-\\d)", "").replaceAll("\"", "") : "";
        this.reportName = ((String) reportLog.get("_id"));//.replaceAll("\\s+", "");
        this.collectionName = (String) reportLog.get("collectionName");
        this.status = (String) reportLog.get("status");
        this.realReportName = (String) reportLog.get("name");
        this.queryWithoutValues = null;
    }

    public IndexCreatorTask(String collectionName, String query, String sortField, String queryWithoutValues) {
        this.collectionName = collectionName;
        this.query = query;
        this.sortField = sortField;
        this.reportName = "noReportIDX";
        this.realReportName = null;
        this.reportLog = null;
        this.queryWithoutValues = queryWithoutValues;
    }

    boolean indexCreation(String indexDefinition) {
        String indexName = "auto_" + HashUtils.hashMD5(indexDefinition);
        try {
            logger.info("************Index Creation Started************");
            validateIndexSuggestion(indexDefinition);
            Date startDate = new Date();
            ReportLogMongoService.getInstance().updateStatusAndIndexInformation(
                    (String) reportLog.get("_id"),
                    IN_PROGRESS,
                    indexName,
                    indexDefinition,
                    startDate,
                    null);
            boolean indexInProgress = IndexCreatorManager.getInstance()
                    .isIndexInProgress((String) reportLog.get("_id"), collectionName, indexName);
            if (!indexInProgress) {//index is not in creation process
                //check if someone has the same definition
                String possibleEqualIndex = IndexCreatorManager.getInstance()
                        .getIndexNameByDefinition(reportName, collectionName, indexDefinition);
                if (StringUtils.isEmpty(possibleEqualIndex)) {
                    //check if it already exists
                    boolean exists = IndexCreatorManager.getInstance()
                            .indexAlreadyExists((String) reportLog.get("_id"), collectionName, indexName);
                    Date endDate = new Date();
                    if (!exists) {
                        executeIndexCreation(indexDefinition, indexName);
                        endDate = new Date();
                        //update register
                        ReportLogMongoService.getInstance().updateStatusAndIndexInformation(
                                (String) reportLog.get("_id"),
                                ReportLogStatus.COMPLETED,
                                indexName,
                                indexDefinition,
                                startDate,
                                endDate);
                        //replicate to all the records that are waiting for the index
                        ReportLogMongoService.getInstance().updateStatusByIndexName(
                                indexName,
                                endDate,
                                ReportLogStatus.COMPLETED,
                                ReportLogStatus.IN_PROGRESS,
                                "This record was updated by the report: " + realReportName);
                        AutoIndexMongoDAO.getInstance().updateStatusByAssociatedIndex(indexName, Boolean.TRUE);
                    } else {
                        ReportLogMongoService.getInstance().updateStatusAndIndexInformation(
                                (String) reportLog.get("_id"),
                                ReportLogStatus.COMPLETED,
                                indexName,
                                indexDefinition,
                                startDate,
                                endDate);
                    }
                    logger.info("************Index Creation Finished************");
                } else {
                    logger.info("************Index Already Defined, taking: " + possibleEqualIndex + " as index************");
                    ReportLogStatus status = ReportLogStatus.IN_PROGRESS;
                    indexInProgress = IndexCreatorManager.getInstance()
                            .isIndexInProgress((String) reportLog.get("_id"), collectionName, possibleEqualIndex);
                    Date associationDate = null;
                    if (!indexInProgress) {
                        status = ReportLogStatus.COMPLETED;
                        associationDate = new Date();
                    } else {
                        logger.info("************Index Association On Hold************");
                    }
                    ReportLogMongoService.getInstance().updateStatusAndAssociateIndex(
                            (String) reportLog.get("_id"),
                            status,
                            associationDate,
                            possibleEqualIndex);

                }
            } else {
                logger.info("************Index Creation On Hold************");
            }
            return true;
        } catch (Exception e) {
            ReportLogMongoService.getInstance().updateStatusByIndexName(indexName, ReportLogStatus.ERROR, e.toString());
            throw new UserException("Index creation failed", e);
        }
    }

    boolean noReportIndexCreation(String indexDefinition) {
        validateIndexSuggestion(indexDefinition);
        String indexName = AutoIndexMongoDAO.generateAutoIndexId(collectionName, queryWithoutValues, sortField);
        try {
            boolean exists = IndexCreatorManager.getInstance()
                    .indexAlreadyExists(reportName, collectionName, indexName);
            String possibleEqualIndex = IndexCreatorManager.getInstance()
                    .getIndexNameByDefinition(reportName, collectionName, indexDefinition);
            if (!exists) {
                logger.info("************Index Creation Started************");
                AutoIndexMongoDAO.getInstance().insert(collectionName, queryWithoutValues, sortField, indexDefinition);
                indexCreation(indexName, possibleEqualIndex, indexDefinition);
            }
            return true;
        } catch (DuplicateKeyException dke) {
            logger.warn("************Another process is creating the index************");
            return false;
        } catch (Exception e) {
            logger.warn("************Index Creation Failed************", e);
            return false;
        }
    }

    boolean reCreateIndex(String indexDefinition) {
        validateIndexSuggestion(indexDefinition);
        String indexName = AutoIndexMongoDAO.generateAutoIndexId(collectionName, queryWithoutValues, sortField);
        try {
            boolean exists = IndexCreatorManager.getInstance()
                    .indexAlreadyExists(reportName, collectionName, indexName);
            String possibleEqualIndex = IndexCreatorManager.getInstance()
                    .getIndexNameByDefinition(reportName, collectionName, indexDefinition);
            if (!exists) {
                if (StringUtils.isBlank(possibleEqualIndex)) {
                    indexName = ReportLogMongoService.getInstance().getIndexByDefinition(indexDefinition);
                }
                AutoIndexMongoDAO.getInstance().updateStatusAndName(indexName,
                        Boolean.FALSE,
                        StringUtils.isNotBlank(possibleEqualIndex)
                                ? possibleEqualIndex : indexName);
                indexCreation(indexName, possibleEqualIndex, indexDefinition);
            }
            return true;
        } catch (Exception e) {
            logger.warn("************Index Creation Failed************", e);
            return false;
        }
    }

    private void indexCreation(String indexName, String possibleEqualIndex, String indexDefinition) throws Exception {
        logger.info("************Index Creation Started************");
        if (StringUtils.isEmpty(possibleEqualIndex)) {
            executeIndexCreation(indexDefinition, indexName);
            AutoIndexMongoDAO.getInstance().updateStatusAndName(indexName, Boolean.TRUE, indexName);
            //replicate to all the records that are waiting for the index in report logs
            ReportLogMongoService.getInstance().updateStatusByAssociatedIndex(
                    indexName,
                    new Date(),
                    ReportLogStatus.COMPLETED,
                    ReportLogStatus.IN_PROGRESS,
                    "This record was updated by the report: " + reportName);
            logger.info("************Index Creation Finished************");
        } else {
            logger.info("************Index Already Defined, taking: " + possibleEqualIndex + " as index************");
            boolean indexInProgress = IndexCreatorManager.getInstance().isIndexInProgress(reportName, collectionName, possibleEqualIndex);
            if (!indexInProgress) {
                AutoIndexMongoDAO.getInstance().updateStatusAndName(indexName, Boolean.TRUE, possibleEqualIndex);
            } else {//associate and wait for creation
                AutoIndexMongoDAO.getInstance().updateStatusAndName(indexName, Boolean.FALSE, possibleEqualIndex);
                logger.info("************Index Association On Hold************");
            }
        }
    }

    void executeIndexCreation(String indexDefinition, String indexName) throws Exception {
        String query = "db." + collectionName + ".createIndex(\n" +
                indexDefinition + ",\n" +
                "{  \n" +
                "   name : \"" + indexName + "\",\n" +
                "   background : true \n" +
                "}\n" +
                ");";
        if (reportLog != null) {
            logger.info("Report Name: " + reportLog.get("name"));
        } else {
            logger.info("Report Name: " + reportName);
        }
        logger.info("Collection Name: " + collectionName);
        logger.info("Index Definition: \n" + query);
        Mongo m = new Mongo(new ConditionBuilder());
        String tmpExport = m.export(query,
                Constants.TEMP_NAME_REPORT + "IndexStatus_" + reportName + System.nanoTime(), ResultFormat.CSV_SCRIPT);
        File f = new File(tmpExport);
        Object response = MongoDAOUtil.fileToJSON(f);
        Map<String, Object> result = (Map<String, Object>) response;
        FileUtils.deleteFile(f);
        logger.info(result);
        if (((Integer) result.get("ok")).compareTo(1) != 0) {
            throw new UserException("Could not create index with definition '" + indexDefinition + "' : " + result.get("errmsg"));
        }
    }

    String executeIndexSuggestion() {
        try {
            logger.info("Starting Query and Field Statistics Analysis");
            //do not change the status to inProgress in this step
            //ReportLogMongoService.getInstance().updateStatus((String) reportLog.get("_id"), IN_PROGRESS);
            long start = System.currentTimeMillis();
            FieldStatistics statistics = new FieldStatistics(query, sortField, collectionName, reportName);
            List<CompletableFuture<Void>> parallelComputation = new LinkedList<>();
            for (int i = 0; i < IndexCreatorTask.STATISTICS_COMPUTATION_TIMES; i++) {
                CompletableFuture<Void> sample = CompletableFuture.runAsync(statistics::computeStatistics, ThingTypeController.executor);
                parallelComputation.add(sample);
            }
            parallelComputation.stream()
                    .map(CompletableFuture::join)
                    .count();//do nothing special, just join;
            //statistics.computeStatistics();

            logger.info("Statistics Estimation Time : " + (System.currentTimeMillis() - start) + " [ms] for " + STATISTICS_COMPUTATION_TIMES + " repetitions");
            List<QueryField> result = statistics.getResult();
            Collections.sort(result);
            Collections.reverse(result);
            logger.info("************Suggested index************");
            logger.info("Collection Name: " + collectionName);
            logger.info(result);
            String suggestedIndex = parseSuggestedIndex(result, "");
            logger.info(suggestedIndex);

            logger.info("************Suggested index************");
            return suggestedIndex;

        } catch (Exception e) {
            logger.error("************Index suggestion failed************", e);
            return null;
        }
    }

    private static boolean checkAlreadyAddedArray(QueryField qf, boolean alreadyAddedArray) {
        return (QueryField.isArray(qf.getDataType()) && alreadyAddedArray);
    }

    private static boolean checkIfIsAnInValidObjectToAdd(QueryField qf, String collectionName) {
//        return (QueryField.isObject(qf.getDataType()) &&
//                ((collectionName.equals(FieldStatistics.THINGS)) ?
//                        !qf.getFieldName().contains(".")
//                        : !qf.getFieldName().replace("value.", "").contains(".")));
        return QueryField.isObject(qf.getDataType());
    }

    private static boolean checkIfIsAnInValidArrayObjectToAdd(QueryField qf, String collectionName) {
        return (QueryField.isArrayOfObjects(qf.getDataType()) &&
                ((collectionName.equals(FieldStatistics.THINGS)) ?
                        !qf.getFieldName().contains(".")
                        : !qf.getFieldName().replace("value.", "").contains(".")));
    }

    private String parseSuggestedIndex(List<QueryField> queryFieldList, String prefix) {
        StringBuilder sb = new StringBuilder("{");
        boolean alreadyAddedArray = false;
        for (QueryField qf : queryFieldList) {
            if (StringUtils.isEmpty(qf.getDataType())) { //ignoring no representative fields
                logger.warn("Ignoring field '" + qf.getFieldName() + "' because it is not representative");
                continue;
            } else if (checkAlreadyAddedArray(qf, alreadyAddedArray)) {//ignore arrays if already have one
                logger.warn("Ignoring field '" + qf.getFieldName() + "' because was already added an array");
                continue;
            } else if (checkIfIsAnInValidObjectToAdd(qf, collectionName)) {
                logger.warn("Ignoring field '" + qf.getFieldName() + "' because it is an object with no attributes to be indexed");
                continue;
            } else if (checkIfIsAnInValidArrayObjectToAdd(qf, collectionName)) {
                logger.warn("Ignoring field '" + qf.getFieldName() + "' because it is an array of objects with no attributes to be indexed");
                continue;
            }
            String key = prefix + qf.getFieldName();
            sb.append("\"" + key + "\"").append(":").append(1).append(",");
            if (QueryField.isArray(qf.getDataType())) {
                alreadyAddedArray = true;
            }
        }
        if (sb.length() > 1) {
            sb = sb.replace(sb.length() - 1, sb.length(), "");
        }
        sb.append("}");
        return sb.toString();
    }

    private void validateIndexSuggestion(String indexDefinition) {
        if (StringUtils.isEmpty(indexDefinition)) {
            throw new UserException("Index definition could not be empty or null");
        } else if (indexDefinition.equals("{}")) {
            throw new UserException("No representative data was found to create an index");
        }
    }
}
