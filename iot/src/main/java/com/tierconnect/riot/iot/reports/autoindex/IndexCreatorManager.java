package com.tierconnect.riot.iot.reports.autoindex;

import com.mongodb.DBObject;
import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.mongo.Mongo;
import com.tierconnect.riot.api.mongoShell.query.ResultFormat;
import com.tierconnect.riot.api.mongoShell.utils.FileUtils;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.controllers.ThingTypeController;
import com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogInfo;
import com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogStatus;
import com.tierconnect.riot.iot.reports.autoindex.services.ReportLogMongoService;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by julio.rocha on 03-05-17.
 */
public class IndexCreatorManager {
    private static Logger logger = Logger.getLogger(IndexCreatorManager.class);
    private static final String KILL_OPERATION_QUERY = "db.killOp(%s)";

    private static IndexCreatorManager INSTANCE = new IndexCreatorManager();

    public static IndexCreatorManager getInstance() {
        return INSTANCE;
    }

    public void executeIndexCreation(DBObject reportLog) {
        /*call to query optimizer*/
        IndexCreatorTask indexCreator = new IndexCreatorTask(reportLog);

        CompletableFuture.supplyAsync(indexCreator::executeIndexSuggestion, ThingTypeController.executor)
                .thenApplyAsync(indexCreator::indexCreation, ThingTypeController.executor)
                .exceptionally(ex -> {
                    logger.error("*****Error executing Auto Index process*****", ex);
                    return false;
                });
    }

    public void executeIndexCreation(String collectionName, String query, String sortField, String queryWithoutValues) {
        IndexCreatorTask indexCreator = new IndexCreatorTask(collectionName, query, sortField, queryWithoutValues);
        CompletableFuture.supplyAsync(indexCreator::executeIndexSuggestion, ThingTypeController.executor)
                .thenApplyAsync(indexCreator::noReportIndexCreation, ThingTypeController.executor)
                .exceptionally(ex -> {
                    logger.error("*****Error executing Auto Index process*****", ex);
                    return false;
                });
    }

    public void executeIndexReCreation(String collectionName, String query, String sortField, String queryWithoutValues) {
        IndexCreatorTask indexCreator = new IndexCreatorTask(collectionName, query, sortField, queryWithoutValues);
        CompletableFuture.supplyAsync(indexCreator::executeIndexSuggestion, ThingTypeController.executor)
                .thenApplyAsync(indexCreator::reCreateIndex, ThingTypeController.executor)
                .exceptionally(ex -> {
                    logger.error("*****Error executing Auto Index process*****", ex);
                    return false;
                });
    }

    private Map<String, Object> executeQuery(String reportName, String query) throws Exception {
        Mongo m = new Mongo(new ConditionBuilder());
        String fileName = Constants.TEMP_NAME_REPORT + "IndexStatus_" + reportName + System.nanoTime();
        String tmpExport = m.export(query,
                fileName.replaceAll("\\s", ""), ResultFormat.CSV_SCRIPT);
        File f = new File(tmpExport);
        Object response = MongoDAOUtil.fileToJSON(f);
        try {
            return (Map<String, Object>) response;
        } finally {
            FileUtils.deleteFile(f);
        }
    }

    public Map<String, Object> indexStatus(String reportName, String collectionName, String indexName) throws Exception {
        String query = retrieveStatusFunction(collectionName, indexName);
        return executeQuery(reportName, query);
    }

    /**
     * Check if the index name is being created by an operation process or
     * if it exists in the list of indexes in the collection.
     *
     * @param reportName     A {@link String} containing a name to set a temporary file to execute.
     * @param collectionName A {@link String} containing the collection name to verify the index.
     * @param indexName A {@link String} containing the index name to verify.
     * @return
     * @throws Exception
     */
    public boolean indexAlreadyExists(String reportName, String collectionName, String indexName) throws Exception {
        if (isIndexInProgress(reportName, collectionName, indexName)) {
            return false;
        }
        Map<String, Object> response = findIndexByName(reportName, collectionName, indexName);
        if (Boolean.FALSE.equals(response.get("response"))) {
            return false;
        }
        return true;
    }

    public boolean isIndexInProgress(String reportName, String collectionName, String indexName) throws Exception {
        Map<String, Object> response = indexStatus(reportName, collectionName, indexName);
        return !(((Integer) response.get("ok")).compareTo(1) == 0 && response.get("name") == null);
    }

    /**
     * Checks whether the index definition was already created under another name and returns it.
     *
     * @param reportName      A {@link String} containing a name to set a temporary file to execute.
     * @param collectionName  A {@link String} containing the collection name to verify the index.
     * @param indexDefinition A {@link String} containing the definition of the index in json format.
     * @return The actual name of the index definition.
     * @throws Exception If error exists.
     */
    public String getIndexNameByDefinition(String reportName, String collectionName, String indexDefinition) throws Exception {
        Map<String, Object> response = findIndexByDefinition(reportName, collectionName, indexDefinition);
        return (String) response.get("indexName");
    }

    public Map<String, Object> findIndexByName(String reportName, String collectionName, String indexName) throws Exception {
        String query = findIndexByNameFunction(collectionName, indexName);
        return executeQuery(reportName, query);
    }

    public Map<String, Object> findIndexByDefinition(String reportName, String collectionName, String indexDefinition) throws Exception {
        String query = findIndexByDefinitionFunction(collectionName, indexDefinition);
        return executeQuery(reportName, query);
    }

    private static String findIndexByNameFunction(String collectionName, String indexName) {
        String response = "var indexToFind = \"" + indexName + "\";\n" +
                "var indexes = db.getCollection('" + collectionName + "').getIndexes();\n" +
                "var response = {\"response\" : false};\n" +
                "for(var i = 0; i < indexes.length; i++){\n" +
                "    if(indexes[i].name === indexToFind){\n" +
                "        var response = {\"response\" : true};\n" +
                "        break;\n" +
                "    }\n" +
                "}\n" +
                "printjson(response);";
        return response;
    }

    private static String findIndexByDefinitionFunction(String collectionName, String indexDefinition) {
        String response = "var indexDefinition = Object.keys(" + indexDefinition + ");\n" +
                "var indexes = db.getCollection(\"" + collectionName + "\").getIndexes();\n" +
                "var response = {\"indexName\" : null};\n" +
                "for(var i = 0; i < indexes.length; i++){\n" +
                "    var definition = Object.keys(indexes[i].key);        \n" +
                "    if(definition.length == indexDefinition.length){\n" +
                "        var equal = true;\n" +
                "        for(var j = 0; j < definition.length; j++){\n" +
                "            if(definition[j] != indexDefinition[j]){\n" +
                "                var equal = false;\n" +
                "                break;\n" +
                "            }            \n" +
                "        }\n" +
                "        if(equal){\n" +
                "            var response = {\"indexName\" : indexes[i].name};\n" +
                "            break;\n" +
                "        }\n" +
                "    }    \n" +
                "}\n" +
                "printjson(response);";
        return response;
    }

    private static String retrieveStatusFunction(String collectionName, String indexName) {
        String status = "function assingValues(x, indexName, executionResponse) {\n" +
                "    executionResponse.ok = x.ok;\n" +
                "    x = x.inprog;\n" +
                "    if(x.length == 1) {\n" +
                "        x = x[0];\n" +
                "        executionResponse.opid = x.opid;\n" +
                "        executionResponse.msg = x.msg;\n" +
                "        x = x.query.indexes;\n" +
                "        for(var i = 0; i < x.length; i++) {\n" +
                "            if(x[i].name == indexName) {\n" +
                "                executionResponse.key = x[i].key;\n" +
                "                executionResponse.name = x[i].name;\n" +
                "                break;\n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "var indexName = \"" + indexName + "\";\n" +
                "var collectionName = \"" + collectionName + "\";\n" +
                "var x = db.currentOP({$and:[{\"query.createIndexes\" : {$exists : true}}, {\"query.createIndexes\" : collectionName}, {\"query.indexes\": { $elemMatch : {name : indexName}}}]})\n" +
                "\n" +
                "var executionResponse = {\"ok\" : -1, \"opid\" : 0, \"key\" : {} , \"name\": null, \"msg\" : \"\"};\n" +
                "if(x.ok == 1) {\n" +
                "    assingValues(x, indexName, executionResponse);\n" +
                "}\n" +
                "\n" +
                "printjson(executionResponse);";
        return status;
    }

    /**
     * Method for cancel the index creation.
     *
     * @param reportName     A {@link String} containing the reportName.
     * @param collectionName A {@link String} containing te collectionName.
     * @param indexName      A {@link String} containing the indexName.
     * @throws Exception If exists a error.
     */
    public void cancelIndexCreation(String reportName, String collectionName, String indexName) throws Exception {
        logger.warn("Attempt to delete creation of index '" + indexName + "' in collection '" + collectionName + "'");
        reportName = reportName.replaceAll("\\s+", "");
        Map<String, Object> response = indexStatus(reportName, collectionName, indexName);
        if (((Integer) response.get("ok")).compareTo(1) == 0
                && response.get("name") != null) {
            Integer operationId = Integer.parseInt(response.get("opid").toString());
            if (operationId == 0) {
                logger.info("Could not cancel the creation of the index because it was already created, IndexName: "
                        + indexName);
                throw new Exception("Could not cancel the creation of the index because opid is null, " +
                        "the index was already created or the process was kill");
            }
            String query = String.format(KILL_OPERATION_QUERY, operationId);
            response = executeQuery(reportName, query);
            if (((Integer) response.get("ok")).compareTo(1) == 0) {
                logger.info("Index creation '" + indexName + "' stopped ");
                ReportLogMongoService.getInstance().updateStatusByIndexName(
                        indexName,
                        ReportLogStatus.CANCELED,
                        "This record was updated by the report:" + reportName);
            }
        } else {
            logger.warn("No operation for creation of index '" + indexName + "' in collection '" + collectionName +
                    "' was find");
        }
    }

    public void updateIndexCreationOnApplicationRestart() {
        try {
            List<ReportLogInfo> inProgressList = ReportLogMongoService.getInstance().getInProgress();
            for (ReportLogInfo rli : inProgressList) {
                Map<String, Object> response = indexStatus(rli.getName(), rli.getCollectionName(),
                        rli.getIndexInformation().getIndexName());
                if (((Integer) response.get("ok")).compareTo(1) == 0
                        && response.get("name") != null) {
                    cancelIndexCreation(rli.getName(), rli.getCollectionName(), rli.getIndexInformation().getIndexName());
                } else {
                    //check if index already exists in collection
                    boolean exists = IndexCreatorManager.getInstance()
                            .indexAlreadyExists(rli.getId(), rli.getCollectionName(), rli.getIndexInformation().getIndexName());
                    ReportLogStatus statusToChange = (exists) ? ReportLogStatus.COMPLETED : ReportLogStatus.CANCELED;
                    ReportLogStatus statusToSearch = ReportLogStatus.IN_PROGRESS;
                    //before change to completed check if the register was not updated by another process
                    ReportLogInfo rlo = ReportLogMongoService.getInstance().getReportLogObject(rli.getId());
                    if (rlo.getStatus().equals(rli.getStatus())) {
                        Date endDate = new Date();
                        //update register
                        ReportLogMongoService.getInstance().updateStatusAndIndexInformation(
                                rli.getId(),
                                statusToChange,
                                rli.getIndexInformation().getIndexName(),
                                rli.getIndexInformation().getIndexDefinition(),
                                null,
                                endDate);
                        //replicate to all the records that are waiting for the index
                        ReportLogMongoService.getInstance().updateStatusByIndexName(
                                rli.getIndexInformation().getIndexName(),
                                endDate,
                                statusToChange,
                                statusToSearch,
                                "This record was updated by the report: " + rli.getName());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Could not read report log documents to update index creation", e);
        }
    }
}
