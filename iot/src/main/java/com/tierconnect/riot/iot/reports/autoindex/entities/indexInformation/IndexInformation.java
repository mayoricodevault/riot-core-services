package com.tierconnect.riot.iot.reports.autoindex.entities.indexInformation;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

import com.tierconnect.riot.commons.DateFormatAndTimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bson.Document;

import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by achambi on 5/26/17.
 * Entity Class for mapping the index status information from mongo database to a java class.
 */
@SuppressWarnings("unchecked")
public class IndexInformation extends BasicDBObject {

    /**
     * Variable for  logs.
     */
    private static Logger logger = Logger.getLogger(IndexInformation.class);

    private static final String ID = "_id";
    private static final String START_DATE_OP_COUNT = "startDateOpCount";
    private static final String NUMBER_QUERIES_DONE = "numberQueriesDone";
    private static final String STATUS = "status";
    private static final String STATS_LOG = "statsLog";
    private static final String COLLECTION_NAME = "collectionName";
    private static final String CREATION_DATE = "creationDate";
    private static final String LAST_RUN_DATE = "lastRunDate";

    private DateFormatAndTimeZone dateFormatAndTimeZone;

    private String startDateOpCount;

    /**
     * default Constructor.
     *
     * @param indexInfoDBObject     A {@link DBObject} containing the index information.
     * @param dateFormatAndTimeZone A {@link DateFormatAndTimeZone} instance to format the date fields.
     */
    public IndexInformation(DBObject indexInfoDBObject,
                            DateFormatAndTimeZone dateFormatAndTimeZone) throws IOException {
        if (indexInfoDBObject == null) {
            logger.error("The indexInfoDBObject is null.");
            throw new IOException("The indexInfoDBObject is null.");
        } else if (indexInfoDBObject instanceof BasicDBObject) {
            BasicDBObject basicDBObjIndexInfo = (BasicDBObject) indexInfoDBObject;
            this.dateFormatAndTimeZone = dateFormatAndTimeZone;
            setId(basicDBObjIndexInfo.getString(ID));
            setStartDateOpCount(basicDBObjIndexInfo.getDate(START_DATE_OP_COUNT));
            setStatus(IndexStatus.getEnum(basicDBObjIndexInfo.getString(STATUS)));
            setNumberQueriesDone(basicDBObjIndexInfo.getLong(NUMBER_QUERIES_DONE));
            setCollectionName(basicDBObjIndexInfo.getString(COLLECTION_NAME));
            setCreationDate(basicDBObjIndexInfo.getDate(CREATION_DATE));
            setLastRunDate(basicDBObjIndexInfo.getDate(LAST_RUN_DATE));
            if (indexInfoDBObject.get(STATS_LOG) instanceof BasicDBList) {
                List<IndexInformation> indexInformationList = new LinkedList<>();
                BasicDBList indexInfoDBList = (BasicDBList) basicDBObjIndexInfo.get(STATS_LOG);
                for (Object indexInfoObject : indexInfoDBList) {
                    IndexInformation indexInfo = new IndexInformation((BasicDBObject) indexInfoObject,
                            this.dateFormatAndTimeZone);
                    indexInfo.remove(LAST_RUN_DATE);
                    indexInformationList.add(indexInfo);
                }
                setStatsLog(indexInformationList);
            }
        } else {
            logger.error("The result cannot be convert in IndexInformation Class.");
            throw new IOException("The result cannot be convert in IndexInformation Class.");
        }
    }

    /**
     * Constructor that stores index information from a database object to a {@link IndexInformation} instance.
     *
     * @param systemIndexStats index stats
     * @throws IOException Input or Output Exception if systemIndexStats is null.
     */
    public IndexInformation(DBObject systemIndexStats) throws IOException {
        if (systemIndexStats == null) {
            logger.error("The result is null.");
            throw new IOException("The result is null.");
        } else if (systemIndexStats instanceof BasicDBObject) {
            BasicDBObject systemIndexStatsFiled = (BasicDBObject) systemIndexStats;
            setId(systemIndexStatsFiled.getString("name"));
            setStatus(IndexStatus.STATS_GENERATED);
            Object accesses = systemIndexStatsFiled.get("accesses");
            if (accesses != null && accesses instanceof BasicDBObject) {
                BasicDBObject accessesDBObject = (BasicDBObject) accesses;
                setStartDateOpCount(accessesDBObject.getDate("since"));
                setNumberQueriesDone(accessesDBObject.getLong("ops"));
            }
        }
    }

    /**
     * Constructor that stores index information from a database object to a {@link IndexInformation} instance.
     *
     * @param systemIndexStats index stats
     * @throws IOException Input or Output Exception if systemIndexStats is null.
     */
    public IndexInformation(Document systemIndexStats) throws IOException {
        if (systemIndexStats == null) {
            logger.error("The result is null.");
            throw new IOException("The result is null.");
        }
        setId(systemIndexStats.getString("name"));
        setStatus(IndexStatus.STATS_GENERATED);
        Object accesses = systemIndexStats.get("accesses");
        if (accesses != null && accesses instanceof Document) {
            Document accessesDocument = (Document) accesses;
            setStartDateOpCount(accessesDocument.getDate("since"));
            setNumberQueriesDone(accessesDocument.getLong("ops"));
        }
    }

    /**
     * Simple constructor .
     *
     * @param id                A {@link String} contains the index name.
     * @param startDateOpCount  A {@link Date} instance.
     * @param numberQueriesDone A {@link Long} containing the operation index number.
     * @throws IOException Input/Output Exception.
     */
    public IndexInformation(String id, Date startDateOpCount, Long numberQueriesDone) throws IOException {
        setId(id);
        setStatus(IndexStatus.STATS_GENERATED);
        setStartDateOpCount(startDateOpCount);
        setNumberQueriesDone(numberQueriesDone);
    }

    /**
     * Constructor for test
     */
    public IndexInformation() {
    }


    /**
     * Constructor for test
     *
     * @param dateFormatAndTimeZone Set {@link DateFormatAndTimeZone} from convert Date fields to ISODate
     *                              {@link String}.
     */
    public IndexInformation(DateFormatAndTimeZone dateFormatAndTimeZone) {
        this.dateFormatAndTimeZone = dateFormatAndTimeZone;
    }

    /**
     * getById Id
     *
     * @return A {@link String} that contains the index id that is equivalent to its name.
     */
    public String getId() {
        return this.getString(ID);
    }

    /**
     * getById start date opreation counting
     *
     * @return Date instance.
     */
    public Date getStartDateOpCount() {
        return getDate(START_DATE_OP_COUNT);
    }

    /**
     * Number of Queries.
     *
     * @return A {@link Long}.
     */
    public Long getNumberQueriesDone() {
        return getLong(NUMBER_QUERIES_DONE);
    }

    /**
     * getById start date operation count in iso Format without time zone.
     *
     * @return string containing a date in iso format.
     */
    String getStartDateOpCountIsoFormat() {
        return startDateOpCount;
    }

    /**
     * getById start date operation counting
     *
     * @return Date instance.
     */
    public IndexStatus getStatus() {
        return IndexStatus.getEnum(getString(STATUS));
    }

    /**
     * getById stats log list.
     *
     * @return A instance of {@link List}<{@link IndexInformation}>.
     */
    public LinkedList<IndexInformation> getStatsLog() {
        Object statsLog = this.get(STATS_LOG);
        if (statsLog instanceof LinkedList) {
            return (LinkedList<IndexInformation>) statsLog;
        } else {
            return null;
        }
    }

    /**
     * The collection name to getById index statistic information.
     *
     * @return the collection name.
     */
    public String getCollectionName() {
        return this.getString(COLLECTION_NAME);
    }

    /**
     * The field creation date.
     *
     * @return the creation date.
     */
    public Date getCreationDate() {
        return this.getDate(CREATION_DATE);
    }

    /**
     * The field last run date.
     *
     * @return the last run date.
     */
    public Date getLastRunDate() {
        return this.getDate(LAST_RUN_DATE);
    }

    /**
     * set Id.
     *
     * @param id a {@link String} containing the index name.
     */
    public void setId(String id) {
        this.put(ID, id);
    }

    /**
     * set start date operation counting.
     *
     * @param startDateOpCount a {@link Date} instance.
     */
    public void setStartDateOpCount(Date startDateOpCount) {
        if (startDateOpCount != null) {
            if (dateFormatAndTimeZone != null) {
                this.startDateOpCount = dateFormatAndTimeZone.getISODateTimeFormatWithoutTimeZone(startDateOpCount);
            }
            put(START_DATE_OP_COUNT, startDateOpCount);
        }
    }

    /**
     * set start date operation counting.
     *
     * @param numberQueries a {@link Date} instance.
     */
    public void setNumberQueriesDone(Long numberQueries) {
        if (numberQueries != null) {
            put(NUMBER_QUERIES_DONE, numberQueries);
        }
    }

    /**
     * set index status
     *
     * @param status a {@link String} containing the index status.
     */
    public void setStatus(IndexStatus status) {
        if (status != null) {
            this.put(STATUS, status.getValue());
        }
    }

    /**
     * Recursively set an array of "IndexInformation".
     *
     * @param statsLogList A {@link List}<{@link IndexInformation}> containing the index stats log.
     */
    public void setStatsLog(List<IndexInformation> statsLogList) {
        put(STATS_LOG, statsLogList);
    }

    /**
     * set Id.
     *
     * @param collectionName a {@link String} containing the index name.
     */
    public void setCollectionName(String collectionName) {
        if (StringUtils.isNotBlank(collectionName)) {
            this.put(COLLECTION_NAME, collectionName);
        }
    }

    /**
     * set Creation Date
     *
     * @param creationDate A {@link Date} field
     */
    public void setCreationDate(Date creationDate) {
        if (creationDate != null) {
            this.put(CREATION_DATE, creationDate);
        }
    }

    /**
     * set last run Date only on the header class.
     *
     * @param lastRunDate A {@link Date} field
     */
    public void setLastRunDate(Date lastRunDate) {
        if (lastRunDate != null) {
            this.put(LAST_RUN_DATE, lastRunDate);
        }
    }
}
