package com.tierconnect.riot.iot.reports.autoindex.services;

import com.mongodb.ReadPreference;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;

import com.tierconnect.riot.iot.dao.mongo.AutoIndexMongoDAO;
import com.tierconnect.riot.iot.reports.autoindex.dao.IndexInformationMongoDAO;
import com.tierconnect.riot.iot.reports.autoindex.entities.indexInformation.IndexInformation;

import com.tierconnect.riot.iot.reports.autoindex.entities.indexInformation.IndexStatus;
import com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogStatus.*;


/**
 * Created by achambi on 5/29/17.
 * IndexInformation Service
 */
public class IndexInformationMongoService {
    private static Logger logger = Logger.getLogger(IndexInformationMongoService.class);

    private static IndexInformationMongoService instance = new IndexInformationMongoService();

    private static IndexInformationMongoDAO indexInformationMongoDAO = IndexInformationMongoDAO.getInstance();

    private static IndexInformationMongoDAO getIndexInformationMongoDAO() {
        return indexInformationMongoDAO;
    }

    public static IndexInformationMongoService getInstance() {
        return instance;
    }

    public void insert(IndexInformation indexInformation) {
        getIndexInformationMongoDAO().insert(indexInformation);
        logger.info("Service: IndexInformation was created success.");
    }

    /**
     * get Index information instance by id
     *
     * @param id          the index name
     * @param currentUser the user to format the date.
     * @return A instance of {@link IndexInformation}.
     * @throws IOException If a error of Input or Output information exists.
     */
    IndexInformation getById(String id, User currentUser) throws IOException {
        DateFormatAndTimeZone dateTimeZone = (currentUser != null) ? UserService.getInstance()
                .getDateFormatAndTimeZone(currentUser) : null;
        return getIndexInformationMongoDAO().getById(id, dateTimeZone);
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    public void saveIndexStats(String collectionName, int sliceNumber, Date currentDate) throws IOException {
        String[] indexNamesArray = ReportLogMongoService.getInstance().getIndexNamesByStatus(COMPLETED, SLOW_INDEX);
        String secondaryList = Configuration.getProperty("mongo.secondary");
        ReadPreference readPreference = ReadPreference.valueOf(Configuration.getProperty("mongo.reportsReadPreference"));
        List<IndexInformation> currentIndexInfList;

        if (StringUtils.isNotBlank(secondaryList) && readPreference.equals(ReadPreference.secondary())) {
            currentIndexInfList = getIndexInformationMongoDAO().getClusterIndexInformation(collectionName, secondaryList, indexNamesArray);
        } else {
            currentIndexInfList = getIndexInformationMongoDAO().getCurrentIndexInformation(collectionName, indexNamesArray);
        }

        for (IndexInformation currentIndexInfo : currentIndexInfList) {
            IndexInformation indexInformation = getIndexInformationMongoDAO()
                    .getById(currentIndexInfo.getId(), null);
            //insert new indexInformation
            if (indexInformation == null) {
                IndexInformation indexInformationStatsLog = (IndexInformation) currentIndexInfo.clone();
                indexInformationStatsLog.setCreationDate(currentDate);
                currentIndexInfo.setStatsLog(new LinkedList<>(Arrays.asList(indexInformationStatsLog)));
                currentIndexInfo.setCollectionName(collectionName);
                currentIndexInfo.setLastRunDate(currentDate);
                getIndexInformationMongoDAO().insert(currentIndexInfo);
            } else {
                Date indexStatsDate = indexInformation.getStartDateOpCount();
                Date currentIndexInfoStartDateOp = currentIndexInfo.getStartDateOpCount();
                Long numberQueriesDone;
                if (indexStatsDate != null && currentIndexInfoStartDateOp != null && (indexStatsDate.compareTo
                        (currentIndexInfoStartDateOp) < 0)) {
                    LinkedList<IndexInformation> statsLog = indexInformation.getStatsLog();
                    IndexInformation statsLogLast = statsLog.getLast();
                    if (statsLogLast.getStartDateOpCount().compareTo(currentIndexInfoStartDateOp) == 0) {
                        numberQueriesDone = indexInformation.getNumberQueriesDone() + (currentIndexInfo
                                .getNumberQueriesDone() - statsLogLast.getNumberQueriesDone());
                    } else {
                        numberQueriesDone = indexInformation.getNumberQueriesDone() + currentIndexInfo
                                .getNumberQueriesDone();
                    }
                } else {
                    numberQueriesDone = currentIndexInfo.getNumberQueriesDone();
                }
                currentIndexInfo.setCreationDate(currentDate);
                getIndexInformationMongoDAO().update(indexInformation.getId(),
                        numberQueriesDone,
                        sliceNumber,
                        currentIndexInfo);
            }
        }
    }

    public List<IndexInformation> getAll() throws IOException {
        return getIndexInformationMongoDAO().getAll();
    }

    void dropIndex(String indexName, String collectionName) {
        MongoDAOUtil.getInstance().db.getCollection(collectionName).dropIndex(indexName);
    }

    public void delete(String indexName) {
        getIndexInformationMongoDAO().delete(indexName);
    }

    /**
     * This method update the last run date.
     *
     * @param indexName   A {@link String} containing the index name
     * @param lastRunDate A {@link Date} containing the last run date.
     */
    public void updateLastRunDate(String indexName, Date lastRunDate) {
        if (StringUtils.isNotBlank(indexName)) {
            getIndexInformationMongoDAO().updateLastRunDate(indexName, lastRunDate);
        }
    }

    /**
     * Delete all unused index in Data Base.
     *
     * @param indexRunNumberMinimum A {@link Long} containing the Minimum number of executions of an index.
     * @param minimumNumberDays     A {@link Long} containing the Minimum number of days to analyze the indexes.
     * @throws IOException If exists error getting the index information.
     */
    public void deleteUnusedIndexes(Long indexRunNumberMinimum, Long minimumNumberDays, Date now) throws IOException {
        logger.info("Starting automatic index deletion.");
        List<IndexInformation> indexInformationList = getIndexInformationMongoDAO().getByStatus(IndexStatus
                .STATS_GENERATED, AutoIndexMongoDAO.getInstance().getAssociatedIndexes());
        for (IndexInformation indexInformation : indexInformationList) {
            try {
                if (indexInformation.getStartDateOpCount() == null) {
                    throw new NullPointerException("startDateOpCount is null.");
                }
                if (indexInformation.getLastRunDate() == null) {
                    throw new NullPointerException("lastRunDate is null.");
                }
                if (indexInformation.getNumberQueriesDone() == null) {
                    throw new NullPointerException("numberQueriesDone is null.");
                }
                if (TimeUnit.DAYS.convert(now.getTime() - indexInformation.getStartDateOpCount().getTime(),
                        TimeUnit.MILLISECONDS) >= minimumNumberDays) {
                    if (TimeUnit.DAYS.convert(now.getTime() -
                            indexInformation.getLastRunDate().getTime(), TimeUnit.MILLISECONDS) >= minimumNumberDays ||
                            indexInformation.getNumberQueriesDone() < indexRunNumberMinimum) {
                        logger.info("Beginning the deletion of the index: " + indexInformation.getId());
                        dropIndex(indexInformation.getId(), indexInformation.getCollectionName());
                        delete(indexInformation.getId());
                        ReportLogMongoService.getInstance().updateStatusByIndexName(
                                indexInformation.getId(),
                                ReportLogStatus.DELETED,
                                "Index deleted for not being used.");
                        logger.info("Deletion of completed index: " + indexInformation.getId());
                    }
                }
            } catch (NullPointerException ex) {
                logger.warn("Null field when trying to analyze the index: " + indexInformation.getId(), ex);
            }
        }
    }
}
