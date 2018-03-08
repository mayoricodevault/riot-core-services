package com.tierconnect.riot.iot.reports.autoindex.services;

import static com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogStatus.COMPLETED;
import static com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogStatus.SLOW_INDEX;
import static junitparams.JUnitParamsRunner.$;
import static org.junit.Assert.*;

import com.mongodb.BasicDBObject;

import com.mongodb.DBObject;

import com.tierconnect.riot.api.database.exception.ValueNotPermittedException;
import com.tierconnect.riot.appcore.core.BaseTestIOT;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.reports.autoindex.dao.IndexInformationMongoDAO;
import com.tierconnect.riot.iot.reports.autoindex.dao.ReportLogMongoDAO;
import com.tierconnect.riot.iot.reports.autoindex.entities.indexInformation.IndexInformation;
import com.tierconnect.riot.iot.reports.autoindex.entities.indexInformation.IndexStatus;
import com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogInfo;
import com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogStatus;
import org.apache.shiro.SecurityUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by achambi on 5/29/17.
 * Unit test from IndexInformationMongoService.
 */
@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public class IndexInformationMongoServiceTest extends BaseTestIOT {

    private IndexInformation indexInformation;
    private SimpleDateFormat formatter;
    private String thingsCollection;
    private String thingSnapShotCollection;

    @Before
    public void setUp() throws Exception {
        thingsCollection = "things";
        thingSnapShotCollection = "thingSnapshots";
        MongoDAOUtil.getInstance().db.getCollection("reportLogs").drop();
        MongoDAOUtil.getInstance().db.getCollection("vizixIndexInformation").drop();
        MongoDAOUtil.getInstance().db.getCollection(thingsCollection).dropIndexes();

        MongoDAOUtil.getInstance().db.getCollection(thingSnapShotCollection).dropIndexes();
        MongoDAOUtil.getInstance().db.getCollection(thingSnapShotCollection).createIndex(new BasicDBObject("time", -1), "time_-1_");

        formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        indexInformation = new IndexInformation(new DateFormatAndTimeZone("Europe/London"));
        indexInformation.setId("mockId");
        indexInformation.setStatus(IndexStatus.STATS_GENERATED);
        indexInformation.setNumberQueriesDone(100L);
        indexInformation.setStartDateOpCount(formatter.parse("2017-04-02 00:00:00"));
    }

    @Test
    public void insert() throws Exception {
        IndexInformationMongoService.getInstance().insert(indexInformation);
        assertEquals(MongoDAOUtil.getInstance().db.getCollection("vizixIndexInformation").count(), 1);
    }

    @Test
    public void get() throws Exception {
        IndexInformationMongoService.getInstance().insert(indexInformation);
        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        IndexInformation indexInformationResult = IndexInformationMongoService.getInstance().getById("mockId",
                currentUser);
        assertEquals(indexInformation, indexInformationResult);
    }

    @Test
    public void saveIndexInfoNew() throws Exception {
        ReportLogMongoDAO.getInstance().insert(new ReportLogInfo(BasicDBObject.parse(
                "{\n" +
                        "  \"_id\": \"00002-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\",\n" +
                        "  \"type\": \"Table Detail\",\n" +
                        "  \"name\": \"All RFID Tags\",\n" +
                        "  \"collectionName\": \"thingSnapshots\",\n" +
                        "  \"lastRunDate\": {\n" +
                        "    \"$date\": 1491364800000\n" +
                        "  },\n" +
                        "  \"maxDuration\": {\n" +
                        "    \"$numberLong\": \"72\"\n" +
                        "  },\n" +
                        "  \"maxDurationDate\": {\n" +
                        "    \"$date\": 1491364800000\n" +
                        "  },\n" +
                        "  \"totalRuns\": {\n" +
                        "    \"$numberLong\": \"1\"\n" +
                        "  },\n" +
                        "  \"query\": \"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                        "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                        "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                        "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}},{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4," +
                        "5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                        "  \"status\": \"slowIndex\",\n" +
                        "  \"maxDurationId\": {\n" +
                        "    \"$oid\": \"58e4febe5d0de2038f323183\"\n" +
                        "  },\n" +
                        "  \"checked\": false,\n" +
                        "  \"filtersDefinition\": null,\n" +
                        "  \"indexInformation\": {\n" +
                        "    \"indexName\": \"time_-1_\",\n" +
                        "    \"definition\": \"time_-1_\",\n" +
                        "    \"starDate\": null,\n" +
                        "    \"endDate\": null\n" +
                        "  },\n" +
                        "  \"runs\": [\n" +
                        "    {\n" +
                        "      \"start\": {\n" +
                        "        \"$numberLong\": \"1491402430619\"\n" +
                        "      },\n" +
                        "      \"end\": {\n" +
                        "        \"$numberLong\": \"1491402430691\"\n" +
                        "      },\n" +
                        "      \"duration\": {\n" +
                        "        \"$numberLong\": \"72\"\n" +
                        "      },\n" +
                        "      \"query\": \"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                        "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                        "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                        "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}},{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4," +
                        "5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                        "      \"count\": {\n" +
                        "        \"$numberLong\": \"5\"\n" +
                        "      },\n" +
                        "      \"id\": {\n" +
                        "        \"$oid\": \"58e4febe5d0de2038f323183\"\n" +
                        "      },\n" +
                        "      \"userID\": {\n" +
                        "        \"$numberLong\": \"1\"\n" +
                        "      },\n" +
                        "      \"date\": {\n" +
                        "        \"$date\": 1491364800000\n" +
                        "      },\n" +
                        "      \"additionalInfo\": {\n" +
                        "        \"header\": {\n" +
                        "          \"origin\": \"http://0.0.0.0:9000\",\n" +
                        "          \"host\": \"127.0.0.1:8081\",\n" +
                        "          \"utcoffset\": \"-240\",\n" +
                        "          \"user-agent\": \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like " +
                        "Gecko) Chrome/57.0.2987.110 Safari/537.36\",\n" +
                        "          \"token\": \"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\"\n" +
                        "        },\n" +
                        "        \"queryString\": \"pageNumber=1&pageSize=15&ts=1491402430486\",\n" +
                        "        \"body\": {\n" +
                        "          \"Thing Type\": \"1\",\n" +
                        "          \"sortProperty\": \"ASC\",\n" +
                        "          \"orderByColumn\": {\n" +
                        "            \"$numberLong\": \"1\"\n" +
                        "          }\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}"
        ), null));

        IndexInformationMongoService.getInstance().saveIndexStats(
                thingSnapShotCollection,
                0,
                formatter.parse("2017-04-01 00:00:00"));

        List<IndexInformation> indexInformationList = IndexInformationMongoService.getInstance().getAll();
        assertEquals(1, indexInformationList.size());
        assertEquals(IndexStatus.STATS_GENERATED, indexInformationList.get(0).getStatus());
        assertEquals(thingSnapShotCollection, indexInformationList.get(0).getCollectionName());
        assertEquals("time_-1_", indexInformationList.get(0).getId());
        assertEquals(formatter.parse("2017-04-01 00:00:00"), indexInformationList.get(0).getLastRunDate());
        assertNull(indexInformationList.get(0).getCreationDate());

        assertEquals(indexInformationList.get(0).getId(),
                indexInformationList.get(0).getStatsLog().getFirst().getId());
        assertEquals(indexInformationList.get(0).getStartDateOpCount(),
                indexInformationList.get(0).getStatsLog().getFirst().getStartDateOpCount());
        assertEquals(indexInformationList.get(0).getStatus(),
                indexInformationList.get(0).getStatsLog().getFirst().getStatus());
        assertEquals(indexInformationList.get(0).getNumberQueriesDone(),
                indexInformationList.get(0).getStatsLog().getFirst().getNumberQueriesDone());
        assertNull(indexInformationList.get(0).getStatsLog().get(0).getLastRunDate());
        assertEquals(formatter.parse("2017-04-01 00:00:00"),
                indexInformationList.get(0).getStatsLog().get(0).getCreationDate());
    }

    @Test
    public void saveIndexStatsWithSameStatistic() throws Exception {
        ReportLogMongoDAO.getInstance().insert(new ReportLogInfo(BasicDBObject.parse(
                "{\n" +
                        "  \"_id\": \"00002-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\",\n" +
                        "  \"type\": \"Table Detail\",\n" +
                        "  \"name\": \"All RFID Tags\",\n" +
                        "  \"collectionName\": \"thingSnapshots\",\n" +
                        "  \"lastRunDate\": {\n" +
                        "    \"$date\": 1491364800000\n" +
                        "  },\n" +
                        "  \"maxDuration\": {\n" +
                        "    \"$numberLong\": \"72\"\n" +
                        "  },\n" +
                        "  \"maxDurationDate\": {\n" +
                        "    \"$date\": 1491364800000\n" +
                        "  },\n" +
                        "  \"totalRuns\": {\n" +
                        "    \"$numberLong\": \"1\"\n" +
                        "  },\n" +
                        "  \"query\": \"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                        "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                        "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                        "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}},{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4," +
                        "5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                        "  \"status\": \"slowIndex\",\n" +
                        "  \"maxDurationId\": {\n" +
                        "    \"$oid\": \"58e4febe5d0de2038f323183\"\n" +
                        "  },\n" +
                        "  \"checked\": false,\n" +
                        "  \"filtersDefinition\": null,\n" +
                        "  \"indexInformation\": {\n" +
                        "    \"indexName\": \"time_-1_\",\n" +
                        "    \"definition\": \"time_-1_\",\n" +
                        "    \"starDate\": null,\n" +
                        "    \"endDate\": null\n" +
                        "  },\n" +
                        "  \"runs\": [\n" +
                        "    {\n" +
                        "      \"start\": {\n" +
                        "        \"$numberLong\": \"1491402430619\"\n" +
                        "      },\n" +
                        "      \"end\": {\n" +
                        "        \"$numberLong\": \"1491402430691\"\n" +
                        "      },\n" +
                        "      \"duration\": {\n" +
                        "        \"$numberLong\": \"72\"\n" +
                        "      },\n" +
                        "      \"query\": \"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                        "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                        "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                        "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}},{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4," +
                        "5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                        "      \"count\": {\n" +
                        "        \"$numberLong\": \"5\"\n" +
                        "      },\n" +
                        "      \"id\": {\n" +
                        "        \"$oid\": \"58e4febe5d0de2038f323183\"\n" +
                        "      },\n" +
                        "      \"userID\": {\n" +
                        "        \"$numberLong\": \"1\"\n" +
                        "      },\n" +
                        "      \"date\": {\n" +
                        "        \"$date\": 1491364800000\n" +
                        "      },\n" +
                        "      \"additionalInfo\": {\n" +
                        "        \"header\": {\n" +
                        "          \"origin\": \"http://0.0.0.0:9000\",\n" +
                        "          \"host\": \"127.0.0.1:8081\",\n" +
                        "          \"utcoffset\": \"-240\",\n" +
                        "          \"user-agent\": \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like " +
                        "Gecko) Chrome/57.0.2987.110 Safari/537.36\",\n" +
                        "          \"token\": \"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\"\n" +
                        "        },\n" +
                        "        \"queryString\": \"pageNumber=1&pageSize=15&ts=1491402430486\",\n" +
                        "        \"body\": {\n" +
                        "          \"Thing Type\": \"1\",\n" +
                        "          \"sortProperty\": \"ASC\",\n" +
                        "          \"orderByColumn\": {\n" +
                        "            \"$numberLong\": \"1\"\n" +
                        "          }\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}"
        ), null));
        IndexInformationMongoService.getInstance().saveIndexStats("thingSnapshots", 0, new Date());
        IndexInformationMongoService.getInstance().saveIndexStats("thingSnapshots", 0, new Date());
        List<IndexInformation> indexInformationList = IndexInformationMongoService.getInstance().getAll();
        assertEquals(1, indexInformationList.size());
        assertEquals(2, indexInformationList.get(0).getStatsLog().size());
    }

    @Test
    public void saveIndexStatsAddStatistic() throws Exception {
        ReportLogMongoDAO.getInstance().insert(new ReportLogInfo(BasicDBObject.parse(
                "{\n" +
                        "  \"_id\": \"00002-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\",\n" +
                        "  \"type\": \"Table Detail\",\n" +
                        "  \"name\": \"All RFID Tags\",\n" +
                        "  \"collectionName\": \"thingSnapshots\",\n" +
                        "  \"lastRunDate\": {\n" +
                        "    \"$date\": 1491364800000\n" +
                        "  },\n" +
                        "  \"maxDuration\": {\n" +
                        "    \"$numberLong\": \"72\"\n" +
                        "  },\n" +
                        "  \"maxDurationDate\": {\n" +
                        "    \"$date\": 1491364800000\n" +
                        "  },\n" +
                        "  \"totalRuns\": {\n" +
                        "    \"$numberLong\": \"1\"\n" +
                        "  },\n" +
                        "  \"query\": \"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                        "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                        "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                        "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}},{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4," +
                        "5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                        "  \"status\": \"completed\",\n" +
                        "  \"maxDurationId\": {\n" +
                        "    \"$oid\": \"58e4febe5d0de2038f323183\"\n" +
                        "  },\n" +
                        "  \"checked\": false,\n" +
                        "  \"filtersDefinition\": null,\n" +
                        "  \"indexInformation\": {\n" +
                        "    \"indexName\": \"time_-1_\",\n" +
                        "    \"definition\": \"time_-1_\",\n" +
                        "    \"starDate\": null,\n" +
                        "    \"endDate\": null\n" +
                        "  },\n" +
                        "  \"runs\": [\n" +
                        "    {\n" +
                        "      \"start\": {\n" +
                        "        \"$numberLong\": \"1491402430619\"\n" +
                        "      },\n" +
                        "      \"end\": {\n" +
                        "        \"$numberLong\": \"1491402430691\"\n" +
                        "      },\n" +
                        "      \"duration\": {\n" +
                        "        \"$numberLong\": \"72\"\n" +
                        "      },\n" +
                        "      \"query\": \"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                        "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                        "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                        "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}},{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4," +
                        "5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                        "      \"count\": {\n" +
                        "        \"$numberLong\": \"5\"\n" +
                        "      },\n" +
                        "      \"id\": {\n" +
                        "        \"$oid\": \"58e4febe5d0de2038f323183\"\n" +
                        "      },\n" +
                        "      \"userID\": {\n" +
                        "        \"$numberLong\": \"1\"\n" +
                        "      },\n" +
                        "      \"date\": {\n" +
                        "        \"$date\": 1491364800000\n" +
                        "      },\n" +
                        "      \"additionalInfo\": {\n" +
                        "        \"header\": {\n" +
                        "          \"origin\": \"http://0.0.0.0:9000\",\n" +
                        "          \"host\": \"127.0.0.1:8081\",\n" +
                        "          \"utcoffset\": \"-240\",\n" +
                        "          \"user-agent\": \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like " +
                        "Gecko) Chrome/57.0.2987.110 Safari/537.36\",\n" +
                        "          \"token\": \"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\"\n" +
                        "        },\n" +
                        "        \"queryString\": \"pageNumber=1&pageSize=15&ts=1491402430486\",\n" +
                        "        \"body\": {\n" +
                        "          \"Thing Type\": \"1\",\n" +
                        "          \"sortProperty\": \"ASC\",\n" +
                        "          \"orderByColumn\": {\n" +
                        "            \"$numberLong\": \"1\"\n" +
                        "          }\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}"
        ), null));
        IndexInformationMongoService.getInstance().saveIndexStats(thingSnapShotCollection, 0, new Date());
        MongoDAOUtil.getInstance().db.getCollection("vizixIndexInformation").update(new BasicDBObject("_id",
                        "time_-1_"),
                new BasicDBObject("$set", new BasicDBObject("numberQueriesDone", 100L)
                        .append("startDateOpCount", formatter.parse("2017-01-01 00:00:00"))
                        .append("statsLog.0.startDateOpCount", formatter.parse("2017-01-01 00:00:00"))
                        .append("statsLog.0.numberQueriesDone", 100L)));

        IndexInformationMongoService.getInstance().saveIndexStats(thingSnapShotCollection, 0, new Date());
        List<IndexInformation> indexInformationList = IndexInformationMongoService.getInstance().getAll();
        assertEquals(1, indexInformationList.size());
        assertEquals(2, indexInformationList.get(0).getStatsLog().size());
        Long expectedNumber = 100L;
        assertEquals(expectedNumber, indexInformationList.get(0).getStatsLog().get(0).getNumberQueriesDone());
        assertEquals(expectedNumber, indexInformationList.get(0).getNumberQueriesDone());
        expectedNumber = 0L;
        assertEquals(expectedNumber, indexInformationList.get(0).getStatsLog().get(1).getNumberQueriesDone());
    }

    @Test
    public void saveIndexStatsCase1() throws Exception {
        ReportLogMongoService.setInstance(null);
        IndexInformationMongoDAO.setInstance(null);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        ReportLogMongoService reportLogMongoService = Mockito.spy(new ReportLogMongoService());
        String[] indexTestMock = new String[]{"auto_indexTest"};
        Mockito.when(reportLogMongoService.getIndexNamesByStatus(COMPLETED, SLOW_INDEX)).thenReturn(indexTestMock);

        IndexInformationMongoDAO indexInformationMongoDAO = Mockito.spy(new IndexInformationMongoDAO());
        List<IndexInformation> indexInformationList = new LinkedList<>();
        IndexInformation item = new IndexInformation();
        item.setId("auto_indexTest");
        item.setCollectionName("things");
        item.setNumberQueriesDone(30L);
        item.setStartDateOpCount(formatter.parse("2017-04-01 00:00:00"));
        indexInformationList.add(item);
        Mockito.doReturn(indexInformationList).when(indexInformationMongoDAO).getCurrentIndexInformation(Mockito.any
                (String.class), Mockito.any(String.class));
        IndexInformation itemCopy = (IndexInformation) item.clone();
        itemCopy.setNumberQueriesDone(10L);
        itemCopy.setStatsLog(new LinkedList<>(Arrays.asList((IndexInformation) itemCopy.clone())));
        indexInformationMongoDAO.insert(itemCopy);
        Mockito.when(indexInformationMongoDAO.getById("auto_indexTest", null)).thenReturn(itemCopy);
        ReportLogMongoService.setInstance(reportLogMongoService);
        IndexInformationMongoDAO.setInstance(indexInformationMongoDAO);
        IndexInformationMongoService.getInstance().saveIndexStats("things", 0, new Date());
        List<IndexInformation> resultAll = IndexInformationMongoService.getInstance().getAll();
        assertEquals(1, resultAll.size());
        Long expected = 30L;
        assertEquals(expected, resultAll.get(0).getNumberQueriesDone());
    }

    @Test
    public void saveIndexStatsCase2() throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        ReportLogMongoService reportLogMongoService = Mockito.spy(new ReportLogMongoService());
        String[] indexTestMock = new String[]{"auto_indexTest"};
        Mockito.when(reportLogMongoService.getIndexNamesByStatus(COMPLETED, SLOW_INDEX)).thenReturn(indexTestMock);

        IndexInformationMongoDAO indexInformationMongoDAO = Mockito.spy(new IndexInformationMongoDAO());
        List<IndexInformation> indexInformationList = new LinkedList<>();
        IndexInformation item = new IndexInformation();
        item.setId("auto_indexTest");
        item.setCollectionName("things");
        item.setNumberQueriesDone(30L);
        item.setStartDateOpCount(formatter.parse("2017-04-02 00:00:00"));
        indexInformationList.add(item);
        ReportLogMongoService.setInstance(reportLogMongoService);

        Mockito.when(indexInformationMongoDAO
                .getCurrentIndexInformation("things", "auto_indexTest"))
                .thenReturn(indexInformationList);
        IndexInformation itemCopy = (IndexInformation) item.clone();
        itemCopy.setNumberQueriesDone(300L);
        itemCopy.setStartDateOpCount(formatter.parse("2017-04-01 00:00:00"));
        itemCopy.setStatsLog(new LinkedList<>(Arrays.asList((IndexInformation) itemCopy.clone())));
        indexInformationMongoDAO.insert(itemCopy);
        Mockito.when(indexInformationMongoDAO.getById("auto_indexTest", null)).thenReturn(itemCopy);
        IndexInformationMongoDAO.setInstance(indexInformationMongoDAO);
        IndexInformationMongoService.getInstance().saveIndexStats("things", 0, new Date());
        List<IndexInformation> resultAll = IndexInformationMongoService.getInstance().getAll();
        assertEquals(1, resultAll.size());
        Long expected = 330L;
        assertEquals(expected, resultAll.get(0).getNumberQueriesDone());
    }

    @Test
    public void saveIndexStatsCase3() throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        ReportLogMongoService reportLogMongoService = Mockito.spy(new ReportLogMongoService());
        String[] indexTestMock = new String[]{"auto_indexTest"};
        Mockito.when(reportLogMongoService.getIndexNamesByStatus(COMPLETED, SLOW_INDEX)).thenReturn(indexTestMock);

        IndexInformationMongoDAO indexInformationMongoDAO = Mockito.spy(new IndexInformationMongoDAO());
        List<IndexInformation> indexInformationList = new LinkedList<>();
        IndexInformation item = new IndexInformation();
        item.setId("auto_indexTest");
        item.setCollectionName("things");
        item.setNumberQueriesDone(30L);
        item.setStartDateOpCount(formatter.parse("2017-04-02 00:00:00"));
        indexInformationList.add(item);
        ReportLogMongoService.setInstance(reportLogMongoService);

        Mockito.when(indexInformationMongoDAO
                .getCurrentIndexInformation("things", "auto_indexTest"))
                .thenReturn(indexInformationList);

        IndexInformation itemCopy = (IndexInformation) item.clone();
        itemCopy.setNumberQueriesDone(120L);
        itemCopy.setStartDateOpCount(formatter.parse("2017-04-01 00:00:00"));

        IndexInformation stats1 = (IndexInformation) itemCopy.clone();
        stats1.setStartDateOpCount(formatter.parse("2017-04-01 00:00:00"));
        stats1.setCreationDate(formatter.parse("2017-04-01 00:00:00"));
        stats1.setNumberQueriesDone(100L);

        IndexInformation stats2 = (IndexInformation) itemCopy.clone();
        stats2.setStartDateOpCount(formatter.parse("2017-04-02 00:00:00"));
        stats2.setCreationDate(formatter.parse("2017-04-02 00:00:00"));
        stats2.setNumberQueriesDone(20L);

        itemCopy.setStatsLog(new LinkedList<>(Arrays.asList(stats1, stats2)));

        indexInformationMongoDAO.insert(itemCopy);
        Mockito.when(indexInformationMongoDAO.getById("auto_indexTest", null)).thenReturn(itemCopy);
        IndexInformationMongoDAO.setInstance(indexInformationMongoDAO);

        IndexInformationMongoService.getInstance().saveIndexStats("things", 0, formatter.parse("2017-04-03 00:00:00"));

        List<IndexInformation> resultAll = IndexInformationMongoService.getInstance().getAll();
        assertEquals(1, resultAll.size());
        Long expected = 130L;
        assertEquals(expected, resultAll.get(0).getNumberQueriesDone());
        assertEquals(3, resultAll.get(0).getStatsLog().size());
        assertNull(resultAll.get(0).getCreationDate());
        expected = 100L;
        assertEquals(expected, resultAll.get(0).getStatsLog().get(0).getNumberQueriesDone());
        assertEquals(formatter.parse("2017-04-01 00:00:00"), resultAll.get(0).getStatsLog().get(0).getCreationDate());
        expected = 20L;
        assertEquals(expected, resultAll.get(0).getStatsLog().get(1).getNumberQueriesDone());
        assertEquals(formatter.parse("2017-04-02 00:00:00"), resultAll.get(0).getStatsLog().get(1).getCreationDate());
        expected = 30L;
        assertEquals(expected, resultAll.get(0).getStatsLog().get(2).getNumberQueriesDone());
        assertEquals(formatter.parse("2017-04-03 00:00:00"), resultAll.get(0).getStatsLog().get(2).getCreationDate());

    }

    @Test
    public void saveIndexStatsCaseSliceNumber() throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        ReportLogMongoService reportLogMongoService = Mockito.spy(new ReportLogMongoService());
        String[] indexTestMock = new String[]{"auto_indexTest"};
        Mockito.when(reportLogMongoService.getIndexNamesByStatus(COMPLETED, SLOW_INDEX)).thenReturn(indexTestMock);

        IndexInformationMongoDAO indexInformationMongoDAO = Mockito.spy(new IndexInformationMongoDAO());
        List<IndexInformation> indexInformationList = new LinkedList<>();
        IndexInformation item = new IndexInformation();
        item.setId("auto_indexTest");
        item.setCollectionName("things");
        item.setNumberQueriesDone(30L);
        item.setStartDateOpCount(formatter.parse("2017-04-02 00:00:00"));
        indexInformationList.add(item);
        ReportLogMongoService.setInstance(reportLogMongoService);

        Mockito.when(indexInformationMongoDAO
                .getCurrentIndexInformation("things", "auto_indexTest"))
                .thenReturn(indexInformationList);

        IndexInformation itemCopy = (IndexInformation) item.clone();
        itemCopy.setNumberQueriesDone(120L);
        itemCopy.setStartDateOpCount(formatter.parse("2017-04-01 00:00:00"));

        IndexInformation stats1 = (IndexInformation) itemCopy.clone();
        stats1.setStartDateOpCount(formatter.parse("2017-04-01 00:00:00"));
        stats1.setCreationDate(formatter.parse("2017-04-01 00:00:00"));
        stats1.setNumberQueriesDone(100L);

        IndexInformation stats2 = (IndexInformation) itemCopy.clone();
        stats2.setStartDateOpCount(formatter.parse("2017-04-02 00:00:00"));
        stats2.setCreationDate(formatter.parse("2017-04-02 00:00:00"));
        stats2.setNumberQueriesDone(20L);

        itemCopy.setStatsLog(new LinkedList<>(Arrays.asList(stats1, stats2)));

        indexInformationMongoDAO.insert(itemCopy);
        Mockito.when(indexInformationMongoDAO.getById("auto_indexTest", null)).thenReturn(itemCopy);
        IndexInformationMongoDAO.setInstance(indexInformationMongoDAO);

        IndexInformationMongoService.getInstance().saveIndexStats("things", -2, formatter.parse("2017-04-03 00:00:00"));

        List<IndexInformation> resultAll = IndexInformationMongoService.getInstance().getAll();
        assertEquals(1, resultAll.size());
        Long expected = 130L;
        assertEquals(expected, resultAll.get(0).getNumberQueriesDone());
        assertEquals(2, resultAll.get(0).getStatsLog().size());
        assertNull(resultAll.get(0).getCreationDate());

        expected = 20L;
        assertEquals(expected, resultAll.get(0).getStatsLog().get(0).getNumberQueriesDone());
        assertEquals(formatter.parse("2017-04-02 00:00:00"), resultAll.get(0).getStatsLog().get(0).getCreationDate());

        expected = 30L;
        assertEquals(expected, resultAll.get(0).getStatsLog().get(1).getNumberQueriesDone());
        assertEquals(formatter.parse("2017-04-03 00:00:00"), resultAll.get(0).getStatsLog().get(1).getCreationDate());
    }

    @Test
    public void saveIndexStatsCaseSliceNumber2() throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        ReportLogMongoService reportLogMongoService = Mockito.spy(new ReportLogMongoService());
        String[] indexTestMock = new String[]{"auto_indexTest"};
        Mockito.when(reportLogMongoService.getIndexNamesByStatus(COMPLETED, SLOW_INDEX)).thenReturn(indexTestMock);

        IndexInformationMongoDAO indexInformationMongoDAO = Mockito.spy(new IndexInformationMongoDAO());
        List<IndexInformation> indexInformationList = new LinkedList<>();
        IndexInformation item = new IndexInformation();
        item.setId("auto_indexTest");
        item.setCollectionName("things");
        item.setNumberQueriesDone(90L);
        item.setStartDateOpCount(formatter.parse("2017-04-03 00:00:00"));
        indexInformationList.add(item);
        ReportLogMongoService.setInstance(reportLogMongoService);

        Mockito.when(indexInformationMongoDAO
                .getCurrentIndexInformation("things", "auto_indexTest"))
                .thenReturn(indexInformationList);

        IndexInformation itemCopy = (IndexInformation) item.clone();
        itemCopy.setNumberQueriesDone(180L);
        itemCopy.setStartDateOpCount(formatter.parse("2017-04-01 00:00:00"));

        IndexInformation stats1 = (IndexInformation) itemCopy.clone();
        stats1.setStartDateOpCount(formatter.parse("2017-04-01 00:00:00"));
        stats1.setCreationDate(formatter.parse("2017-04-01 00:00:00"));
        stats1.setNumberQueriesDone(100L);

        IndexInformation stats2 = (IndexInformation) itemCopy.clone();
        stats2.setStartDateOpCount(formatter.parse("2017-04-01 00:00:00"));
        stats2.setCreationDate(formatter.parse("2017-04-02 00:00:00"));
        stats2.setNumberQueriesDone(20L);

        IndexInformation stats3 = (IndexInformation) itemCopy.clone();
        stats3.setStartDateOpCount(formatter.parse("2017-04-03 00:00:00"));
        stats3.setCreationDate(formatter.parse("2017-04-03 00:00:00"));
        stats3.setNumberQueriesDone(10L);

        IndexInformation stats4 = (IndexInformation) itemCopy.clone();
        stats4.setStartDateOpCount(formatter.parse("2017-04-03 00:00:00"));
        stats4.setCreationDate(formatter.parse("2017-04-04 00:00:00"));
        stats4.setNumberQueriesDone(40L);

        IndexInformation stats5 = (IndexInformation) itemCopy.clone();
        stats5.setStartDateOpCount(formatter.parse("2017-04-03 00:00:00"));
        stats5.setCreationDate(formatter.parse("2017-04-05 00:00:00"));
        stats5.setNumberQueriesDone(60L);

        itemCopy.setStatsLog(new LinkedList<>(Arrays.asList(stats1, stats2, stats3, stats4, stats5)));

        indexInformationMongoDAO.insert(itemCopy);
        Mockito.when(indexInformationMongoDAO.getById("auto_indexTest", null)).thenReturn(itemCopy);
        IndexInformationMongoDAO.setInstance(indexInformationMongoDAO);

        IndexInformationMongoService.getInstance().saveIndexStats("things", -4, formatter.parse("2017-04-06 00:00:00"));

        List<IndexInformation> resultAll = IndexInformationMongoService.getInstance().getAll();
        assertEquals(1, resultAll.size());
        Long expected = 210L;
        assertEquals(expected, resultAll.get(0).getNumberQueriesDone());
        assertEquals(4, resultAll.get(0).getStatsLog().size());
        assertNull(resultAll.get(0).getCreationDate());

        expected = 10L;
        assertEquals(expected, resultAll.get(0).getStatsLog().get(0).getNumberQueriesDone());
        assertEquals(formatter.parse("2017-04-03 00:00:00"), resultAll.get(0).getStatsLog().get(0).getCreationDate());

        expected = 40L;
        assertEquals(expected, resultAll.get(0).getStatsLog().get(1).getNumberQueriesDone());
        assertEquals(formatter.parse("2017-04-04 00:00:00"), resultAll.get(0).getStatsLog().get(1).getCreationDate());

        expected = 60L;
        assertEquals(expected, resultAll.get(0).getStatsLog().get(2).getNumberQueriesDone());
        assertEquals(formatter.parse("2017-04-05 00:00:00"), resultAll.get(0).getStatsLog().get(2).getCreationDate());

        expected = 90L;
        assertEquals(expected, resultAll.get(0).getStatsLog().get(3).getNumberQueriesDone());
        assertEquals(formatter.parse("2017-04-06 00:00:00"), resultAll.get(0).getStatsLog().get(3).getCreationDate());
    }

    @Test
    public void saveIndexStatsCaseSliceNumber3() throws Exception {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        ReportLogMongoService reportLogMongoService = Mockito.spy(new ReportLogMongoService());
        String[] indexTestMock = new String[]{"auto_indexTest"};
        Mockito.when(reportLogMongoService.getIndexNamesByStatus(COMPLETED, SLOW_INDEX)).thenReturn(indexTestMock);

        IndexInformationMongoDAO indexInformationMongoDAO = Mockito.spy(new IndexInformationMongoDAO());
        List<IndexInformation> indexInformationList = new LinkedList<>();
        IndexInformation item = new IndexInformation();
        item.setId("auto_indexTest");
        item.setCollectionName("things");
        item.setNumberQueriesDone(90L);
        item.setStartDateOpCount(formatter.parse("2017-04-03 00:00:00"));
        indexInformationList.add(item);
        ReportLogMongoService.setInstance(reportLogMongoService);

        Mockito.when(indexInformationMongoDAO
                .getCurrentIndexInformation("things", "auto_indexTest"))
                .thenReturn(indexInformationList);

        IndexInformation itemCopy = (IndexInformation) item.clone();
        itemCopy.setNumberQueriesDone(120L);
        itemCopy.setStartDateOpCount(formatter.parse("2017-04-01 00:00:00"));

        IndexInformation stats1 = (IndexInformation) itemCopy.clone();
        stats1.setStartDateOpCount(formatter.parse("2017-04-01 00:00:00"));
        stats1.setCreationDate(formatter.parse("2017-04-01 00:00:00"));
        stats1.setNumberQueriesDone(100L);

        IndexInformation stats2 = (IndexInformation) itemCopy.clone();
        stats2.setStartDateOpCount(formatter.parse("2017-04-01 00:00:00"));
        stats2.setCreationDate(formatter.parse("2017-04-02 00:00:00"));
        stats2.setNumberQueriesDone(20L);

        itemCopy.setStatsLog(new LinkedList<>(Arrays.asList(stats1, stats2)));

        indexInformationMongoDAO.insert(itemCopy);
        Mockito.when(indexInformationMongoDAO.getById("auto_indexTest", null)).thenReturn(itemCopy);
        IndexInformationMongoDAO.setInstance(indexInformationMongoDAO);

        IndexInformationMongoService.getInstance().saveIndexStats("things", -4, formatter.parse("2017-04-06 00:00:00"));

        List<IndexInformation> resultAll = IndexInformationMongoService.getInstance().getAll();
        assertEquals(1, resultAll.size());
        Long expected = 210L;
        assertEquals(expected, resultAll.get(0).getNumberQueriesDone());
        assertEquals(3, resultAll.get(0).getStatsLog().size());
        assertNull(resultAll.get(0).getCreationDate());

        expected = 100L;
        assertEquals(expected, resultAll.get(0).getStatsLog().get(0).getNumberQueriesDone());
        assertEquals(formatter.parse("2017-04-01 00:00:00"), resultAll.get(0).getStatsLog().get(0).getCreationDate());

        expected = 20L;
        assertEquals(expected, resultAll.get(0).getStatsLog().get(1).getNumberQueriesDone());
        assertEquals(formatter.parse("2017-04-02 00:00:00"), resultAll.get(0).getStatsLog().get(1).getCreationDate());

        expected = 90L;
        assertEquals(expected, resultAll.get(0).getStatsLog().get(2).getNumberQueriesDone());
        assertEquals(formatter.parse("2017-04-06 00:00:00"), resultAll.get(0).getStatsLog().get(2).getCreationDate());
    }

    @Test
    public void deleteIndex() throws Exception {
        MongoDAOUtil.getInstance().db.getCollection(thingsCollection).createIndex(
                new BasicDBObject("thingTypeId", 1)
                        .append("groupId", 1)
                        .append("serialNumber", 1), "autoIndexTest00001");
        IndexInformationMongoService.getInstance().dropIndex("autoIndexTest00001", thingsCollection);

        for (DBObject dbObject : MongoDAOUtil.getInstance().db.getCollection(thingsCollection).getIndexInfo()) {
            assertNotEquals("autoIndexTest00001", dbObject.get("name").toString());
        }
    }

    @Test
    public void delete() throws Exception {

        //TODO This code is necessary because we do not have a correct test framework!
        IndexInformationMongoService.getInstance().insert(indexInformation);
        indexInformation.setId("mockId2");
        IndexInformationMongoService.getInstance().insert(indexInformation);
        indexInformation.setId("mockId3");
        IndexInformationMongoService.getInstance().insert(indexInformation);
        indexInformation.setId("mockId4");
        IndexInformationMongoService.getInstance().insert(indexInformation);
        indexInformation.setId("mockId5");
        IndexInformationMongoService.getInstance().insert(indexInformation);
        indexInformation.setId("mockId6");
        IndexInformationMongoService.getInstance().insert(indexInformation);
        indexInformation.setId("mockId7");
        IndexInformationMongoService.getInstance().insert(indexInformation);
        indexInformation.setId("mockId8");
        IndexInformationMongoService.getInstance().insert(indexInformation);
        indexInformation.setId("mockId9");
        IndexInformationMongoService.getInstance().insert(indexInformation);
        indexInformation.setId("mockId10");
        IndexInformationMongoService.getInstance().insert(indexInformation);

        /*Delete Index Information Test*/
        String indexName = "mockId";
        IndexInformationMongoService.getInstance().delete(indexName);
        indexName = "mockId2";
        IndexInformationMongoService.getInstance().delete(indexName);
        indexName = "mockId3";
        IndexInformationMongoService.getInstance().delete(indexName);
        indexName = "mockId4";
        IndexInformationMongoService.getInstance().delete(indexName);
        List<IndexInformation> response = IndexInformationMongoService.getInstance().getAll();
        for (int i = 0; i < response.size(); i++) {
            assertEquals("mockId" + (i + 5), response.get(i).getId());
        }
    }

    @Test
    public void deleteUnusedIndexes() throws Exception {
        MongoDAOUtil.getInstance().db.getCollection(thingsCollection).dropIndexes();
        Object[] parameters = parameterForIndexDelete();
        for (Object itemParam : parameters) {
            Object[] parameterItem = (Object[]) itemParam;
            MongoDAOUtil.getInstance().db.getCollection(thingsCollection).createIndex((BasicDBObject)
                    parameterItem[0], (String) parameterItem[1]);
            IndexInformationMongoService.getInstance().insert((IndexInformation) parameterItem[2]);
            ReportLogMongoDAO.getInstance().insert((ReportLogInfo) parameterItem[3]);
        }

        Long indexRunNumberMinimum = 5L;
        Long minimumNumberDays = 90L;
        Date now = formatter.parse("2017-04-01 00:00:00");

        IndexInformationMongoService.getInstance().deleteUnusedIndexes(indexRunNumberMinimum, minimumNumberDays, now);
        List<IndexInformation> indexInformationList = IndexInformationMongoService.getInstance().getAll();
        assertEquals(3, indexInformationList.size());
        List<DBObject> indexInfo = MongoDAOUtil.getInstance().db.getCollection(thingsCollection).getIndexInfo();
        indexInfo.remove(0);
        assertEquals(3, indexInfo.size());
        List<ReportLogInfo> reportLogInfoList = ReportLogMongoDAO.getInstance().getByStatus(COMPLETED);
        assertEquals(3, reportLogInfoList.size());
        List<ReportLogInfo> reportLogInfoListAll = ReportLogMongoDAO.getInstance().getAll();
        assertEquals(6, reportLogInfoListAll.size());
        for (int i = 0; i < indexInformationList.size(); i++) {
            assertNotEquals("autoTestIndex1", indexInformationList.get(i).getId());
            assertNotEquals("autoTestIndex2", indexInformationList.get(i).getId());
            assertNotEquals("autoTestIndex3", indexInformationList.get(i).getId());
            assertNotEquals("autoTestIndex1", indexInfo.get(i).get("name"));
            assertNotEquals("autoTestIndex2", indexInfo.get(i).get("name"));
            assertNotEquals("autoTestIndex3", indexInfo.get(i).get("name"));
        }
        for (ReportLogInfo item : reportLogInfoListAll) {
            if (item.getIndexInformation().getIndexName().equals("autoTestIndex1") ||
                    item.getIndexInformation().getIndexName().equals("autoTestIndex2") ||
                    item.getIndexInformation().getIndexName().equals("autoTestIndex3")) {
                assertEquals(ReportLogStatus.DELETED, item.getStatus());
            } else {
                assertEquals(COMPLETED, item.getStatus());
            }
        }
    }

    private Object[] parameterForIndexDelete() throws ValueNotPermittedException, IOException {
        return $(
                /*0*/
                $(new BasicDBObject("thingTypeId", 1).append("groupId", 1).append("serialNumber", 1),
                        "autoTestIndex1",
                        new IndexInformation(BasicDBObject.parse("{\n" +
                                "    \"_id\" : \"autoTestIndex1\",\n" +
                                "    \"startDateOpCount\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "    \"numberQueriesDone\" : 3,\n" +
                                "    \"collectionName\" : \"things\",\n" +
                                "    \"status\" : \"statsGenerated\",\n" +
                                "    \"lastRunDate\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "    \"statsLog\" : [ \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex1\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 1\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex1\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-02T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 1\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex1\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-03T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 0\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex1\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-04T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 3\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}"), null),
                        new ReportLogInfo(BasicDBObject.parse("{\n" +
                                "    \"_id\" : " +
                                "\"00001-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\",\n" +
                                "    \"type\" : \"Table Detail\",\n" +
                                "    \"name\" : \"autoTestIndex1\",\n" +
                                "    \"lastRunDate\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "    \"maxDuration\" : NumberLong(72),\n" +
                                "    \"maxDurationDate\" : ISODate(\"2017-04-05\"),\n" +
                                "    \"checked\" : false, \n" +
                                "    \"totalRuns\" : 1,\n" +
                                "    \"query\" : " +
                                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                                "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                                "    \"status\" : \"completed\",\n" +
                                "    \"maxDurationId\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                                "    \"indexInformation\": {\n" +
                                "      \"indexName\"  : \"autoTestIndex1\"\n" +
                                "      \"definition\" :{\n" +
                                "                      \"thingTypeId\"  : 1,\n" +
                                "                      \"groupId\"      : 1,\n" +
                                "                      \"serialNumber\" : 1\n" +
                                "      }\n" +
                                "      \"starDate\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "      \"endDate\" : ISODate(\"2017-01-01T00:30:00.000Z\")\n" +
                                "    }\n" +
                                "    \"runs\" : [ \n" +
                                "        {\n" +
                                "            \"start\" : NumberLong(1491402430619),\n" +
                                "            \"end\" : NumberLong(1491402430691),\n" +
                                "            \"duration\" : NumberLong(72),\n" +
                                "            \"query\" : " +
                                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                                "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                                "            \"count\" : NumberLong(5),\n" +
                                "            \"id\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                                "            \"userID\" : NumberLong(1),\n" +
                                "            \"date\" : ISODate(\"2017-04-05\"),\n" +
                                "            \"additionalInfo\" : {\n" +
                                "                \"header\" : {\n" +
                                "                    \"origin\" : \"http://0.0.0.0:9000\",\n" +
                                "                    \"host\" : \"127.0.0.1:8081\",\n" +
                                "                    \"utcoffset\" : \"-240\",\n" +
                                "                    \"user-agent\" : \"Mozilla/5.0 (X11; Linux x86_64) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.110 Safari/537.36\",\n" +
                                "                    \"token\" : " +
                                "\"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\"\n" +
                                "                },\n" +
                                "                \"queryString\" : \"pageNumber=1&pageSize=15&ts=1491402430486\",\n" +
                                "                \"body\" : {\n" +
                                "                    \"Thing Type\" : \"1\",\n" +
                                "                    \"sortProperty\" : \"ASC\",\n" +
                                "                    \"orderByColumn\" : NumberLong(1)\n" +
                                "                }\n" +
                                "            }\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}"), null)
                ),
                /*1*/
                $(new BasicDBObject("modifiedTime", -1).append("createdTime", -1).append("time", -1),
                        "autoTestIndex2",
                        new IndexInformation(BasicDBObject.parse("{\n" +
                                "    \"_id\" : \"autoTestIndex2\",\n" +
                                "    \"startDateOpCount\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "    \"numberQueriesDone\" : 100,\n" +
                                "    \"collectionName\" : \"things\",\n" +
                                "    \"status\" : \"statsGenerated\",\n" +
                                "    \"lastRunDate\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "    \"statsLog\" : [ \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex2\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 20\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex2\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 30\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex2\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 40\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex2\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 100\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}"), null),
                        new ReportLogInfo(BasicDBObject.parse("{\n" +
                                "    \"_id\" : " +
                                "\"00002-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\",\n" +
                                "    \"type\" : \"Table Detail\",\n" +
                                "    \"name\" : \"autoTestIndex2\",\n" +
                                "    \"lastRunDate\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "    \"maxDuration\" : NumberLong(72),\n" +
                                "    \"maxDurationDate\" : ISODate(\"2017-04-05\"),\n" +
                                "    \"checked\" : false, \n" +
                                "    \"totalRuns\" : 1,\n" +
                                "    \"query\" : " +
                                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                                "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                                "    \"status\" : \"completed\",\n" +
                                "    \"maxDurationId\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                                "    \"indexInformation\": {\n" +
                                "      \"indexName\"  : \"autoTestIndex2\"\n" +
                                "      \"definition\" :{\n" +
                                "                      \"modifiedTime\" : -1,\n" +
                                "                      \"createdTime\" : -1,\n" +
                                "                      \"time\" : -1\n" +
                                "      }\n" +
                                "      \"starDate\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "      \"endDate\" : ISODate(\"2017-01-01T00:30:00.000Z\")\n" +
                                "    }\n" +
                                "    \"runs\" : [ \n" +
                                "        {\n" +
                                "            \"start\" : NumberLong(1491402430619),\n" +
                                "            \"end\" : NumberLong(1491402430691),\n" +
                                "            \"duration\" : NumberLong(72),\n" +
                                "            \"query\" : " +
                                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                                "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                                "            \"count\" : NumberLong(5),\n" +
                                "            \"id\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                                "            \"userID\" : NumberLong(1),\n" +
                                "            \"date\" : ISODate(\"2017-04-05\"),\n" +
                                "            \"additionalInfo\" : {\n" +
                                "                \"header\" : {\n" +
                                "                    \"origin\" : \"http://0.0.0.0:9000\",\n" +
                                "                    \"host\" : \"127.0.0.1:8081\",\n" +
                                "                    \"utcoffset\" : \"-240\",\n" +
                                "                    \"user-agent\" : \"Mozilla/5.0 (X11; Linux x86_64) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.110 Safari/537.36\",\n" +
                                "                    \"token\" : " +
                                "\"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\"\n" +
                                "                },\n" +
                                "                \"queryString\" : \"pageNumber=1&pageSize=15&ts=1491402430486\",\n" +
                                "                \"body\" : {\n" +
                                "                    \"Thing Type\" : \"1\",\n" +
                                "                    \"sortProperty\" : \"ASC\",\n" +
                                "                    \"orderByColumn\" : NumberLong(1)\n" +
                                "                }\n" +
                                "            }\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}"), null)
                ),
                /*2*/
                $(new BasicDBObject("thingTypeCode", 1).append("name", 1).append("serialNumber", 1),
                        "autoTestIndex3",
                        new IndexInformation(BasicDBObject.parse("{\n" +
                                "    \"_id\" : \"autoTestIndex3\",\n" +
                                "    \"startDateOpCount\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "    \"numberQueriesDone\" : 4,\n" +
                                "    \"status\" : \"statsGenerated\",\n" +
                                "    \"collectionName\" : \"things\",\n" +
                                "    \"lastRunDate\" : ISODate(\"2017-03-01T00:00:00.000Z\"),\n" +
                                "    \"statsLog\" : [ \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex3\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-02T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 0\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex3\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-03T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 0\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex3\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-04T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 0\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex3\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-05T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 4\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}"), null),
                        new ReportLogInfo(BasicDBObject.parse("{\n" +
                                "    \"_id\" : " +
                                "\"00003-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\",\n" +
                                "    \"type\" : \"Table Detail\",\n" +
                                "    \"name\" : \"autoTestIndex3\",\n" +
                                "    \"lastRunDate\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "    \"maxDuration\" : NumberLong(72),\n" +
                                "    \"maxDurationDate\" : ISODate(\"2017-04-05\"),\n" +
                                "    \"checked\" : false, \n" +
                                "    \"totalRuns\" : 1,\n" +
                                "    \"query\" : " +
                                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                                "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                                "    \"status\" : \"completed\",\n" +
                                "    \"maxDurationId\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                                "    \"indexInformation\": {\n" +
                                "      \"indexName\"  : \"autoTestIndex3\"\n" +
                                "      \"definition\" :{\n" +
                                "                      \"thingTypeCode\" : 1,\n" +
                                "                      \"name\" : 1,\n" +
                                "                      \"serialNumber\" : 1\n" +
                                "      }\n" +
                                "      \"starDate\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "      \"endDate\" : ISODate(\"2017-01-01T00:30:00.000Z\")\n" +
                                "    }\n" +
                                "    \"runs\" : [ \n" +
                                "        {\n" +
                                "            \"start\" : NumberLong(1491402430619),\n" +
                                "            \"end\" : NumberLong(1491402430691),\n" +
                                "            \"duration\" : NumberLong(72),\n" +
                                "            \"query\" : " +
                                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                                "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                                "            \"count\" : NumberLong(5),\n" +
                                "            \"id\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                                "            \"userID\" : NumberLong(1),\n" +
                                "            \"date\" : ISODate(\"2017-04-05\"),\n" +
                                "            \"additionalInfo\" : {\n" +
                                "                \"header\" : {\n" +
                                "                    \"origin\" : \"http://0.0.0.0:9000\",\n" +
                                "                    \"host\" : \"127.0.0.1:8081\",\n" +
                                "                    \"utcoffset\" : \"-240\",\n" +
                                "                    \"user-agent\" : \"Mozilla/5.0 (X11; Linux x86_64) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.110 Safari/537.36\",\n" +
                                "                    \"token\" : " +
                                "\"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\"\n" +
                                "                },\n" +
                                "                \"queryString\" : \"pageNumber=1&pageSize=15&ts=1491402430486\",\n" +
                                "                \"body\" : {\n" +
                                "                    \"Thing Type\" : \"1\",\n" +
                                "                    \"sortProperty\" : \"ASC\",\n" +
                                "                    \"orderByColumn\" : NumberLong(1)\n" +
                                "                }\n" +
                                "            }\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}"), null)
                ),
                /*3*/
                $(new BasicDBObject("groupName", 1).append("groupCode", 1).append("groupId", 1),
                        "autoTestIndex4",
                        new IndexInformation(BasicDBObject.parse("{\n" +
                                "    \"_id\" : \"autoTestIndex4\",\n" +
                                "    \"startDateOpCount\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "    \"numberQueriesDone\" : 120,\n" +
                                "    \"status\" : \"statsGenerated\",\n" +
                                "    \"collectionName\" : \"things\",\n" +
                                "    \"lastRunDate\" : ISODate(\"2017-03-01T00:00:00.000Z\"),\n" +
                                "    \"statsLog\" : [ \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex4\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-02T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 50\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex4\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-03T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 25\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex4\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-04T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 25\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex4\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-05T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 20\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}"), null),
                        new ReportLogInfo(BasicDBObject.parse("{\n" +
                                "    \"_id\" : " +
                                "\"00004-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\",\n" +
                                "    \"type\" : \"Table Detail\",\n" +
                                "    \"name\" : \"autoTestIndex4\",\n" +
                                "    \"lastRunDate\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "    \"maxDuration\" : NumberLong(72),\n" +
                                "    \"maxDurationDate\" : ISODate(\"2017-04-05\"),\n" +
                                "    \"checked\" : false, \n" +
                                "    \"totalRuns\" : 1,\n" +
                                "    \"query\" : " +
                                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                                "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                                "    \"status\" : \"completed\",\n" +
                                "    \"maxDurationId\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                                "    \"indexInformation\": {\n" +
                                "      \"indexName\"  : \"autoTestIndex4\"\n" +
                                "      \"definition\" :{\n" +
                                "                \"groupName\" : 1,\n" +
                                "                \"groupCode\" : 1,\n" +
                                "                \"groupId\" : 1\n" +
                                "      }\n" +
                                "      \"starDate\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "      \"endDate\" : ISODate(\"2017-01-01T00:30:00.000Z\")\n" +
                                "    }\n" +
                                "    \"runs\" : [ \n" +
                                "        {\n" +
                                "            \"start\" : NumberLong(1491402430619),\n" +
                                "            \"end\" : NumberLong(1491402430691),\n" +
                                "            \"duration\" : NumberLong(72),\n" +
                                "            \"query\" : " +
                                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                                "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                                "            \"count\" : NumberLong(5),\n" +
                                "            \"id\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                                "            \"userID\" : NumberLong(1),\n" +
                                "            \"date\" : ISODate(\"2017-04-05\"),\n" +
                                "            \"additionalInfo\" : {\n" +
                                "                \"header\" : {\n" +
                                "                    \"origin\" : \"http://0.0.0.0:9000\",\n" +
                                "                    \"host\" : \"127.0.0.1:8081\",\n" +
                                "                    \"utcoffset\" : \"-240\",\n" +
                                "                    \"user-agent\" : \"Mozilla/5.0 (X11; Linux x86_64) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.110 Safari/537.36\",\n" +
                                "                    \"token\" : " +
                                "\"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\"\n" +
                                "                },\n" +
                                "                \"queryString\" : \"pageNumber=1&pageSize=15&ts=1491402430486\",\n" +
                                "                \"body\" : {\n" +
                                "                    \"Thing Type\" : \"1\",\n" +
                                "                    \"sortProperty\" : \"ASC\",\n" +
                                "                    \"orderByColumn\" : NumberLong(1)\n" +
                                "                }\n" +
                                "            }\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}"), null)
                ),
                /*4*/
                $(new BasicDBObject("size.value", 1).append("color.value", 1).append("children.serialNumber", 1),
                        "autoTestIndex5",
                        new IndexInformation(BasicDBObject.parse("{\n" +
                                "    \"_id\" : \"autoTestIndex5\",\n" +
                                "    \"startDateOpCount\" : ISODate(\"2017-03-01T00:00:00.000Z\"),\n" +
                                "    \"numberQueriesDone\" : 1,\n" +
                                "    \"status\" : \"statsGenerated\",\n" +
                                "    \"collectionName\" : \"things\",\n" +
                                "    \"lastRunDate\" : ISODate(\"2017-03-01T00:00:00.000Z\"),\n" +
                                "    \"statsLog\" : [ \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex5\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-06-01T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 0\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex5\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-06-01T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 0\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex5\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-06-01T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 0\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex5\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-06-01T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 1\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}"), null),
                        new ReportLogInfo(BasicDBObject.parse("{\n" +
                                "    \"_id\" : " +
                                "\"00005-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\",\n" +
                                "    \"type\" : \"Table Detail\",\n" +
                                "    \"name\" : \"autoTestIndex5\",\n" +
                                "    \"lastRunDate\" : ISODate(\"2017-06-01T00:00:00.000Z\"),\n" +
                                "    \"maxDuration\" : NumberLong(72),\n" +
                                "    \"maxDurationDate\" : ISODate(\"2017-04-05\"),\n" +
                                "    \"checked\" : false, \n" +
                                "    \"totalRuns\" : 1,\n" +
                                "    \"query\" : " +
                                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                                "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                                "    \"status\" : \"completed\",\n" +
                                "    \"maxDurationId\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                                "    \"indexInformation\": {\n" +
                                "      \"indexName\"  : \"autoTestIndex5\"\n" +
                                "      \"definition\" :{\n" +
                                "                      \"size.value\" : 1,\n" +
                                "                      \"color.value\" : 1,\n" +
                                "                      \"children.serialNumber\" : 1\n" +
                                "      }\n" +
                                "      \"starDate\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "      \"endDate\" : ISODate(\"2017-01-01T00:30:00.000Z\")\n" +
                                "    }\n" +
                                "    \"runs\" : [ \n" +
                                "        {\n" +
                                "            \"start\" : NumberLong(1491402430619),\n" +
                                "            \"end\" : NumberLong(1491402430691),\n" +
                                "            \"duration\" : NumberLong(72),\n" +
                                "            \"query\" : " +
                                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                                "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                                "            \"count\" : NumberLong(5),\n" +
                                "            \"id\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                                "            \"userID\" : NumberLong(1),\n" +
                                "            \"date\" : ISODate(\"2017-04-05\"),\n" +
                                "            \"additionalInfo\" : {\n" +
                                "                \"header\" : {\n" +
                                "                    \"origin\" : \"http://0.0.0.0:9000\",\n" +
                                "                    \"host\" : \"127.0.0.1:8081\",\n" +
                                "                    \"utcoffset\" : \"-240\",\n" +
                                "                    \"user-agent\" : \"Mozilla/5.0 (X11; Linux x86_64) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.110 Safari/537.36\",\n" +
                                "                    \"token\" : " +
                                "\"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\"\n" +
                                "                },\n" +
                                "                \"queryString\" : \"pageNumber=1&pageSize=15&ts=1491402430486\",\n" +
                                "                \"body\" : {\n" +
                                "                    \"Thing Type\" : \"1\",\n" +
                                "                    \"sortProperty\" : \"ASC\",\n" +
                                "                    \"orderByColumn\" : NumberLong(1)\n" +
                                "                }\n" +
                                "            }\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}"), null)
                ),
                /*5*/
                $(new BasicDBObject("active.value", 1).append("parent.status.value", 1).append("parent.serialNumber",
                        1),
                        "autoTestIndex6",
                        new IndexInformation(BasicDBObject.parse("{\n" +
                                "    \"_id\" : \"autoTestIndex6\",\n" +
                                "    \"startDateOpCount\" : ISODate(\"2017-03-01T00:00:00.000Z\"),\n" +
                                "    \"numberQueriesDone\" : 4,\n" +
                                "    \"status\" : \"statsGenerated\",\n" +
                                "    \"collectionName\" : \"things\",\n" +
                                "    \"lastRunDate\" : ISODate(\"2017-04-01T00:00:00.000Z\"),\n" +
                                "    \"statsLog\" : [ \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex6\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-02T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 0\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex6\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-03T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 0\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex6\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-04T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 2\n" +
                                "        }, \n" +
                                "        {\n" +
                                "            \"_id\" : \"autoTestIndex6\",\n" +
                                "            \"startDateOpCount\" : ISODate(\"2017-01-05T00:00:00.000Z\"),\n" +
                                "            \"numberQueriesDone\" : 2\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}"), null),
                        new ReportLogInfo(BasicDBObject.parse("{\n" +
                                "    \"_id\" : " +
                                "\"00006-4d4aeab6d432f09a311989c8f49e4f3bc332e4d71a2570c5b0bb7da2516ddde2\",\n" +
                                "    \"type\" : \"Table Detail\",\n" +
                                "    \"name\" : \"autoTestIndex6\",\n" +
                                "    \"lastRunDate\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "    \"maxDuration\" : NumberLong(72),\n" +
                                "    \"maxDurationDate\" : ISODate(\"2017-04-05\"),\n" +
                                "    \"checked\" : false, \n" +
                                "    \"totalRuns\" : 1,\n" +
                                "    \"query\" : " +
                                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                                "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                                "    \"status\" : \"completed\",\n" +
                                "    \"maxDurationId\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                                "    \"indexInformation\": {\n" +
                                "      \"indexName\"  : \"autoTestIndex6\"\n" +
                                "      \"definition\" : {\n" +
                                "                        \"active.value\" : 1,\n" +
                                "                        \"parent.status.value\" : 1,\n" +
                                "                        \"parent.serialNumber\" : 1\n" +
                                "      }\n" +
                                "      \"starDate\" : ISODate(\"2017-01-01T00:00:00.000Z\"),\n" +
                                "      \"endDate\" : ISODate(\"2017-01-01T00:30:00.000Z\")\n" +
                                "    }\n" +
                                "    \"runs\" : [ \n" +
                                "        {\n" +
                                "            \"start\" : NumberLong(1491402430619),\n" +
                                "            \"end\" : NumberLong(1491402430691),\n" +
                                "            \"duration\" : NumberLong(72),\n" +
                                "            \"query\" : " +
                                "\"{\\\"$and\\\":[{\\\"$or\\\":[{\\\"children\\\":{\\\"$exists\\\":false}}," +
                                "{\\\"children\\\":null},{\\\"children\\\":{\\\"$size\\\":0}}]}," +
                                "{\\\"parent\\\":{\\\"$exists\\\":false}},{\\\"thingTypeId\\\":1}," +
                                "{\\\"groupId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7]}}," +
                                "{\\\"thingTypeId\\\":{\\\"$in\\\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15]}}]}\",\n" +
                                "            \"count\" : NumberLong(5),\n" +
                                "            \"id\" : ObjectId(\"58e4febe5d0de2038f323183\"),\n" +
                                "            \"userID\" : NumberLong(1),\n" +
                                "            \"date\" : ISODate(\"2017-04-05\"),\n" +
                                "            \"additionalInfo\" : {\n" +
                                "                \"header\" : {\n" +
                                "                    \"origin\" : \"http://0.0.0.0:9000\",\n" +
                                "                    \"host\" : \"127.0.0.1:8081\",\n" +
                                "                    \"utcoffset\" : \"-240\",\n" +
                                "                    \"user-agent\" : \"Mozilla/5.0 (X11; Linux x86_64) " +
                                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.110 Safari/537.36\",\n" +
                                "                    \"token\" : " +
                                "\"615cdfb806a0e01e88897b80648df8fe115a8709fa7aef004c9db8d6103cc077\"\n" +
                                "                },\n" +
                                "                \"queryString\" : \"pageNumber=1&pageSize=15&ts=1491402430486\",\n" +
                                "                \"body\" : {\n" +
                                "                    \"Thing Type\" : \"1\",\n" +
                                "                    \"sortProperty\" : \"ASC\",\n" +
                                "                    \"orderByColumn\" : NumberLong(1)\n" +
                                "                }\n" +
                                "            }\n" +
                                "        }\n" +
                                "    ]\n" +
                                "}"), null)
                )
        );
    }

    @Test
    public void updateLastRunDate() throws Exception {
        IndexInformationMongoService.getInstance().insert(indexInformation);
        IndexInformationMongoService.getInstance().updateLastRunDate(indexInformation.getId(),
                formatter.parse("2017-04-10 00:00:00"));
        IndexInformation index = IndexInformationMongoService.getInstance().getById(indexInformation.getId(), null);
        assertEquals(formatter.parse("2017-04-10 00:00:00"), index.getLastRunDate());
    }
}