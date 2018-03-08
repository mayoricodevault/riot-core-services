package com.tierconnect.riot.iot.reports.autoindex.dao;

import com.mongodb.*;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.reports.autoindex.entities.indexInformation.IndexInformation;
import com.tierconnect.riot.iot.reports.autoindex.entities.indexInformation.IndexStatus;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.tierconnect.riot.commons.Constants.PREFIX_SYSTEM_COL;
import static org.apache.commons.lang.StringUtils.*;
import static org.apache.commons.lang.math.NumberUtils.isNumber;

/**
 * Created by achambi on 5/26/17.
 * Crud for Index stats.
 */
public class IndexInformationMongoDAO {

    /**
     * Singleton instance.
     */
    private static IndexInformationMongoDAO instance = new IndexInformationMongoDAO();
    /**
     * Variable for  logs.
     */
    private static Logger logger = Logger.getLogger(IndexInformationMongoDAO.class);
    /**
     * Collection for save report logs in mongo.
     */
    private DBCollection indexInformationCollection = MongoDAOUtil.getInstance().db.getCollection(PREFIX_SYSTEM_COL + "IndexInformation");

    /**
     * This method is only for mock test
     *
     * @param instanceField a instance of {@link IndexInformationMongoDAO}
     */
    public static void setInstance(IndexInformationMongoDAO instanceField) {
        instance = instanceField;
    }

    /**
     * Get Singleton Instance.
     *
     * @return return instance.
     */
    public static IndexInformationMongoDAO getInstance() {
        return instance;
    }

    /**
     * Method for insert a report log object in mongo.
     *
     * @param indexInformation Object to insert.
     */
    public void insert(IndexInformation indexInformation) {
        WriteResult result = indexInformationCollection.insert(indexInformation);
        logger.info("DAO: Index stats was created.");
        logger.info(result);
    }


    /**
     * Method for insert a report log object in mongo.
     *
     * @param indexInformationList {@link List} of {@link IndexInformation} to insert.
     */
    public void insert(List<IndexInformation> indexInformationList) {
        WriteResult result = indexInformationCollection.insert(indexInformationList);
        logger.info("DAO: Index stats was created.");
        logger.info(result);
    }

    /**
     * Method for get report saved in database.
     *
     * @param id A {@link String} that contains a hash compose for a correlation id and parameters.
     * @return A {@link IndexInformation} instance or null if not found the record.
     * @throws IOException if the result cannot convert to {@link IndexInformation}
     */
    public IndexInformation getById(String id, DateFormatAndTimeZone dateFormatAndTimeZone) throws IOException {
        BasicDBObject queryFind = new BasicDBObject("_id", id);
        DBCursor cursor = indexInformationCollection.find(queryFind);
        DBObject result = (cursor != null) ? cursor.one() : null;
        return (result != null) ? new IndexInformation(result, dateFormatAndTimeZone) : null;
    }

    /**
     * Method for get Index Information from  replica set.
     *
     * @param collectionName A {@link String} containing the collection name.
     * @param indexNames     A {@link String}[] Array containing the index names.
     * @return A instance of {@link List}<{@link List}<{@link IndexInformation}>> instance.
     * @throws IOException If Input/Output error exists.
     */
    public List<IndexInformation> getClusterIndexInformation(String collectionName, String secondaryList, String... indexNames) throws
            IOException {
        if (secondaryList == null) {
            throw new IOException("Error secondary list is null");
        }
        List<IndexInformation> indexInformationList = new LinkedList<>();

        String[] hosts = secondaryList.split(",");
        for (String hostString : hosts) {
            String[] host = hostString.split(":");
            if (host.length != 2) {
                throw new IOException("Error when parsing the replica set." + hostString);
            }
            if (isBlank(host[0]) && !isNumber(host[1])) {
                throw new IOException("Error parsing the name and port of the replica set." + hostString);
            }
            MongoDAO mongoDAO = new MongoDAO(Configuration.getProperty("mongo.username"),
                    Configuration.getProperty("mongo.password"),
                    Configuration.getProperty("mongo.authdb"),
                    host[0].trim(),
                    Integer.parseInt(host[1].trim()),
                    Configuration.getProperty("mongo.db"));
            indexInformationList.addAll(mongoDAO.getIndexStatistics(collectionName, indexNames));
        }

        Map<String, List<IndexInformation>> listMap = indexInformationList.stream().collect(Collectors.groupingBy(IndexInformation::getId));
        Iterator<Map.Entry<String, List<IndexInformation>>> iterator = listMap.entrySet().iterator();
        List<IndexInformation> indexInformationResult = new ArrayList<>();
        while (iterator.hasNext()) {
            Map.Entry<String, List<IndexInformation>> informationList = iterator.next();
            Long totalNumberQueriesDone = 0L;
            Date totalStartDateOpCount = new Date();
            for (IndexInformation item : informationList.getValue()) {
                totalNumberQueriesDone += item.getNumberQueriesDone();
                if (item.getStartDateOpCount().compareTo(totalStartDateOpCount) < 0) {
                    totalStartDateOpCount = item.getStartDateOpCount();
                }
            }
            indexInformationResult.add(new IndexInformation(informationList.getKey(), totalStartDateOpCount, totalNumberQueriesDone));
        }
        return indexInformationResult;
    }

    /**
     * Get Index information from mongo database
     *
     * @param collectionName the collection name to get index information.
     * @return A {@link List}<{@link IndexInformation}> instance.
     * @throws IOException if the query fail.
     */
    public List<IndexInformation> getCurrentIndexInformation(String collectionName, String... indexNames)
            throws IOException {
        List<IndexInformation> indexInformationList = new ArrayList<>();
        List<DBObject> pipeline = new LinkedList<>();
        pipeline.add(new BasicDBObject("$indexStats", new BasicDBObject()));
        pipeline.add(new BasicDBObject("$match", new BasicDBObject("name", new BasicDBObject("$in", indexNames))));
        AggregationOutput aggregationOutput = MongoDAOUtil
                .getInstance()
                .db.getCollection(collectionName)
                .aggregate(pipeline);
        for (final DBObject indexStats : aggregationOutput.results()) {
            indexInformationList.add(new IndexInformation(indexStats));
        }
        return indexInformationList;
    }

    public List<IndexInformation> getAll() throws IOException {
        DBCursor indexCursor = indexInformationCollection.find();
        List<IndexInformation> indexInformationList = new LinkedList<>();
        while (indexCursor.hasNext()) {
            indexInformationList.add(new IndexInformation(indexCursor.next(), null));
        }
        return indexInformationList;
    }

    /**
     * Update {@link IndexInformation}
     *
     * @param indexName           index Name to update in indexStats
     * @param indexInformationNew New index statistics getting from mongo.
     */
    public void update(String indexName, Long numberQueriesDone, int sliceNumber, IndexInformation
            indexInformationNew) {
        DBObject query = new BasicDBObject("_id", indexName);
        BasicDBList arrayLog = new BasicDBList();
        arrayLog.add(indexInformationNew);
        BasicDBObject statsLogDbObject = new BasicDBObject("$each", arrayLog);
        if (sliceNumber != 0) {
            statsLogDbObject.append("$slice", sliceNumber);
        }
        DBObject update = new BasicDBObject("$push", new BasicDBObject("statsLog", statsLogDbObject));
        DBObject fieldsUpdate = new BasicDBObject("numberQueriesDone", numberQueriesDone);
        update.put("$set", fieldsUpdate);
        WriteResult result = indexInformationCollection.update(query, update);
        logger.info("\n**** IndexInformation WAS UPDATED ****\n");
        logger.info(result);
    }


    /**
     * Update the last run date
     *
     * @param indexName   A {@link String} contains the indexName
     * @param lastRunDate A {@link Date} contains the last run date.
     */
    public void updateLastRunDate(String indexName, Date lastRunDate) {
        DBObject query = new BasicDBObject("_id", indexName);
        WriteResult update = indexInformationCollection.update(query, new BasicDBObject("$set", new BasicDBObject
                ("lastRunDate",
                        lastRunDate)));
        logger.info("the result update is: " + update.toString());
    }

    /**
     * Method to get all index Information with status matched.
     *
     * @param indexStatus A {@link IndexStatus} statistic.
     * @return a {@link LinkedList}<{@link IndexInformation}
     * @throws IOException Input and Output information error.
     */
    public List<IndexInformation> getByStatus(IndexStatus indexStatus, String... excludedIndexes) throws IOException {
        BasicDBObject query = new BasicDBObject("status", indexStatus.getValue());
        query.append("_id", new BasicDBObject("$nin", excludedIndexes));
        DBCursor indexCursor = indexInformationCollection.find(query);
        List<IndexInformation> indexInformationList = new LinkedList<>();
        while (indexCursor.hasNext()) {
            indexInformationList.add(new IndexInformation(indexCursor.next(), null));
        }
        return indexInformationList;
    }

    /**
     * Delete index information
     *
     * @param indexName the index name to delete NOTE: the index name is the _id in mongo.
     */
    public void delete(String indexName) {
        BasicDBObject queryDbObject = new BasicDBObject("_id", indexName);
        WriteResult writeResult = indexInformationCollection.remove(queryDbObject);
        logger.info("correct deletion of index information:" + writeResult);
    }
}
