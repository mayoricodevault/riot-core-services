package com.tierconnect.riot.iot.reports.autoindex.dao;


import com.mongodb.BasicDBObject;

import com.tierconnect.riot.appcore.core.BaseTestIOT;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.reports.autoindex.entities.indexInformation.IndexInformation;
import com.tierconnect.riot.iot.reports.autoindex.entities.indexInformation.IndexStatus;
import org.junit.Before;
import org.junit.Test;


import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by achambi on 5/29/17.
 * Test unit from index stats thing DAO.
 */
public class IndexInformationMongoDAOTest extends BaseTestIOT {

    private BasicDBObject indexInfoBasicDBObject;
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private String indexInfoCollection = "vizixIndexInformation";

    @Before
    public void setUp() throws Exception {
        MongoDAOUtil.getInstance().db.getCollection(indexInfoCollection).drop();
        indexInfoBasicDBObject = BasicDBObject.parse("{\n" +
                "    \"_id\" : \"indexNameIdTest\",\n" +
                "    \"startDateOpCount\" : ISODate(\"2017-04-01T00:00:00.000Z\"),\n" +
                "    \"numberQueriesDone\" : 200,\n" +
                "    \"status\" : \"statsGenerated\",\n" +
                "    \"lastRunDate\" : ISODate(\"2017-04-01T00:00:00.000Z\"),\n" +
                "    \"statsLog\" : [ \n" +
                "        {\n" +
                "            \"_id\" : \"indexNameIdTest\",\n" +
                "            \"startDateOpCount\" : ISODate(\"2017-04-01T00:00:00.000Z\"),\n" +
                "            \"numberQueriesDone\" : 50\n" +
                "        }, \n" +
                "        {\n" +
                "            \"_id\" : \"indexNameIdTest\",\n" +
                "            \"startDateOpCount\" : ISODate(\"2017-04-02T00:00:00.000Z\"),\n" +
                "            \"numberQueriesDone\" : 50\n" +
                "        }, \n" +
                "        {\n" +
                "            \"_id\" : \"indexNameIdTest\",\n" +
                "            \"startDateOpCount\" : ISODate(\"2017-04-03T00:00:00.000Z\"),\n" +
                "            \"numberQueriesDone\" : 50\n" +
                "        }, \n" +
                "        {\n" +
                "            \"_id\" : \"indexNameIdTest\",\n" +
                "            \"startDateOpCount\" : ISODate(\"2017-04-04T00:00:00.000Z\"),\n" +
                "            \"numberQueriesDone\" : 50\n" +
                "        }\n" +
                "    ]\n" +
                "}");
    }

    @Test
    public void insert() throws Exception {
        IndexInformation indexInformation = new IndexInformation(indexInfoBasicDBObject, new DateFormatAndTimeZone
                ("Europe/London"));

        IndexInformationMongoDAO.getInstance().insert(indexInformation);

        List<IndexInformation> indexInformationList = IndexInformationMongoDAO.getInstance().getAll();
        assertEquals(1, indexInformationList.size());
        assertEquals(indexInformation, indexInformationList.get(0));
    }

    @Test
    public void insertList() throws Exception {
        IndexInformation indexInformation = new IndexInformation(indexInfoBasicDBObject, new DateFormatAndTimeZone
                ("Europe/London"));
        List<IndexInformation> indexInformationList = new LinkedList<>();
        indexInformationList.add(indexInformation);
        IndexInformationMongoDAO.getInstance().insert(indexInformationList);
        assertEquals(1, MongoDAOUtil.getInstance().db.getCollection("vizixIndexInformation").count());
    }

    @Test
    public void update() throws Exception {
        IndexInformation indexInformation = new IndexInformation(
                BasicDBObject.parse("{\n" +
                        "    \"_id\" : \"indexNameIdTest\",\n" +
                        "    \"startDateOpCount\" : ISODate(\"2017-04-01T00:00:00.000Z\"),\n" +
                        "    \"numberQueriesDone\" : 260,\n" +
                        "    \"status\" : \"statsGenerated\",\n" +
                        "    \"collectionName\" : \"thingSnapshots\",\n" +
                        "    \"statsLog\" : [ \n" +
                        "        {\n" +
                        "            \"_id\" : \"indexNameIdTest\",\n" +
                        "            \"startDateOpCount\" : ISODate(\"2017-04-01T00:00:00.000Z\"),\n" +
                        "            \"numberQueriesDone\" : 50\n" +
                        "            \"collectionName\" : \"thingSnapshots\"\n" +
                        "        }, \n" +
                        "        {\n" +
                        "            \"_id\" : \"indexNameIdTest\",\n" +
                        "            \"startDateOpCount\" : ISODate(\"2017-04-02T00:00:00.000Z\"),\n" +
                        "            \"numberQueriesDone\" : 60\n" +
                        "            \"collectionName\" : \"thingSnapshots\"\n" +
                        "        }, \n" +
                        "        {\n" +
                        "            \"_id\" : \"indexNameIdTest\",\n" +
                        "            \"startDateOpCount\" : ISODate(\"2017-04-03T00:00:00.000Z\"),\n" +
                        "            \"numberQueriesDone\" : 70\n" +
                        "            \"collectionName\" : \"thingSnapshots\"\n" +
                        "        }, \n" +
                        "        {\n" +
                        "            \"_id\" : \"indexNameIdTest\",\n" +
                        "            \"startDateOpCount\" : ISODate(\"2017-04-04T00:00:00.000Z\"),\n" +
                        "            \"numberQueriesDone\" : 80\n" +
                        "            \"collectionName\" : \"thingSnapshots\"\n" +
                        "        }\n" +
                        "    ]\n" +
                        "}"), null);

        MongoDAOUtil.getInstance().db.getCollection("vizixIndexInformation").insert(indexInformation);
        IndexInformation indexInformationNew = new IndexInformation();
        indexInformationNew.setId("indexNameIdTest");
        indexInformationNew.setNumberQueriesDone(90L);
        indexInformationNew.setCollectionName("thingSnapshots");
        indexInformationNew.setStartDateOpCount(formatter.parse("2017-04-05 00:00:00"));
        IndexInformationMongoDAO.getInstance().update("indexNameIdTest",
                350L,
                0,
                indexInformationNew);
        IndexInformation result = IndexInformationMongoDAO.getInstance().getById("indexNameIdTest", null);
        List<IndexInformation> indexInformationLog = result.getStatsLog();
        assertEquals(5, indexInformationLog.size());
        for (int i = 0; i < indexInformationLog.size(); i++) {
            assertEquals(formatter.parse("2017-04-0" + (i + 1) + " 00:00:00"), indexInformationLog.get(i)
                    .getStartDateOpCount());
            Long count = 50L + (i * 10L);
            assertEquals(count, indexInformationLog.get(i).getNumberQueriesDone());
        }
        assertEquals(formatter.parse("2017-04-01 00:00:00"), result.getStartDateOpCount());
        Long totalExpected = 350L;
        assertEquals(totalExpected, result.getNumberQueriesDone());
    }

    @Test
    public void updateWithSliceArray() throws Exception {
        IndexInformation indexInformation = new IndexInformation(
                BasicDBObject.parse("{\n" +
                        "    \"_id\" : \"indexNameIdTest\",\n" +
                        "    \"startDateOpCount\" : ISODate(\"2017-04-01T00:00:00.000Z\"),\n" +
                        "    \"numberQueriesDone\" : 260,\n" +
                        "    \"status\" : \"statsGenerated\",\n" +
                        "    \"collectionName\" : \"thingSnapshots\",\n" +
                        "    \"statsLog\" : [ \n" +
                        "        {\n" +
                        "            \"_id\" : \"indexNameIdTest\",\n" +
                        "            \"startDateOpCount\" : ISODate(\"2017-04-01T00:00:00.000Z\"),\n" +
                        "            \"numberQueriesDone\" : 50\n" +
                        "            \"collectionName\" : \"thingSnapshots\"\n" +
                        "        }, \n" +
                        "        {\n" +
                        "            \"_id\" : \"indexNameIdTest\",\n" +
                        "            \"startDateOpCount\" : ISODate(\"2017-04-02T00:00:00.000Z\"),\n" +
                        "            \"numberQueriesDone\" : 60\n" +
                        "            \"collectionName\" : \"thingSnapshots\"\n" +
                        "        }, \n" +
                        "        {\n" +
                        "            \"_id\" : \"indexNameIdTest\",\n" +
                        "            \"startDateOpCount\" : ISODate(\"2017-04-03T00:00:00.000Z\"),\n" +
                        "            \"numberQueriesDone\" : 70\n" +
                        "            \"collectionName\" : \"thingSnapshots\"\n" +
                        "        }, \n" +
                        "        {\n" +
                        "            \"_id\" : \"indexNameIdTest\",\n" +
                        "            \"startDateOpCount\" : ISODate(\"2017-04-04T00:00:00.000Z\"),\n" +
                        "            \"numberQueriesDone\" : 80\n" +
                        "            \"collectionName\" : \"thingSnapshots\"\n" +
                        "        }\n" +
                        "    ]\n" +
                        "}"), null);

        MongoDAOUtil.getInstance().db.getCollection("vizixIndexInformation").insert(indexInformation);
        IndexInformation indexInformationNew = new IndexInformation();
        indexInformationNew.setId("indexNameIdTest");
        indexInformationNew.setNumberQueriesDone(90L);
        indexInformationNew.setCollectionName("thingSnapshots");
        indexInformationNew.setStartDateOpCount(formatter.parse("2017-04-05 00:00:00"));

        //UNIT TO TEST
        IndexInformationMongoDAO.getInstance().update("indexNameIdTest", 350L, -4, indexInformationNew);


        IndexInformation result = IndexInformationMongoDAO.getInstance().getById("indexNameIdTest", null);
        List<IndexInformation> indexInformationLog = result.getStatsLog();
        assertEquals(4, indexInformationLog.size());
        int index = 2;
        for (int i = 0; i < indexInformationLog.size(); i++) {
            assertEquals(formatter.parse("2017-04-0" + index + " 00:00:00"), indexInformationLog.get(i)
                    .getStartDateOpCount());
            Long count = 60L + (i * 10L);
            assertEquals(count, indexInformationLog.get(i).getNumberQueriesDone());
            index++;
        }
        assertEquals(formatter.parse("2017-04-01 00:00:00"), result.getStartDateOpCount());
        Long totalExpected = 350L;
        assertEquals(totalExpected, result.getNumberQueriesDone());
    }

    @Test
    public void getById() throws Exception {
        MongoDAOUtil.getInstance().db.getCollection("vizixIndexInformation").insert(indexInfoBasicDBObject);
        IndexInformation indexInformation = IndexInformationMongoDAO.getInstance().getById("indexNameIdTest", new
                DateFormatAndTimeZone
                ("Europe/London"));
        assertEquals("{ \"_id\" : \"indexNameIdTest\" , \"startDateOpCount\" : { \"$date\" : " +
                "\"2017-04-01T00:00:00.000Z\"} , \"status\" : \"statsGenerated\" , \"numberQueriesDone\" : 200 , " +
                "\"lastRunDate\" : { \"$date\" : \"2017-04-01T00:00:00.000Z\"} , \"statsLog\" : [ { \"_id\" : " +
                "\"indexNameIdTest\" , \"startDateOpCount\" : { \"$date\" : \"2017-04-01T00:00:00.000Z\"} , " +
                "\"numberQueriesDone\" : 50} , { \"_id\" : \"indexNameIdTest\" , \"startDateOpCount\" : { \"$date\" :" +
                " \"2017-04-02T00:00:00.000Z\"} , \"numberQueriesDone\" : 50} , { \"_id\" : \"indexNameIdTest\" , " +
                "\"startDateOpCount\" : { \"$date\" : \"2017-04-03T00:00:00.000Z\"} , \"numberQueriesDone\" : 50} , {" +
                " \"_id\" : \"indexNameIdTest\" , \"startDateOpCount\" : { \"$date\" : \"2017-04-04T00:00:00.000Z\"} " +
                ", \"numberQueriesDone\" : 50}]}", indexInformation.toString());
    }

    @Test
    public void getAll() throws Exception {
        MongoDAOUtil.getInstance().db.getCollection("vizixIndexInformation").insert(indexInfoBasicDBObject);
        List<IndexInformation> indexInformationList = IndexInformationMongoDAO.getInstance().getAll();
        assertEquals(1, indexInformationList.size());
        assertEquals("[{ \"_id\" : \"indexNameIdTest\" , \"startDateOpCount\" : { \"$date\" : " +
                "\"2017-04-01T00:00:00.000Z\"} , \"status\" : \"statsGenerated\" , \"numberQueriesDone\" : 200 , " +
                "\"lastRunDate\" : { \"$date\" : \"2017-04-01T00:00:00.000Z\"} , \"statsLog\" : [ { \"_id\" : " +
                "\"indexNameIdTest\" , \"startDateOpCount\" : { \"$date\" : \"2017-04-01T00:00:00.000Z\"} , " +
                "\"numberQueriesDone\" : 50} , { \"_id\" : \"indexNameIdTest\" , \"startDateOpCount\" : { \"$date\" :" +
                " \"2017-04-02T00:00:00.000Z\"} , \"numberQueriesDone\" : 50} , { \"_id\" : \"indexNameIdTest\" , " +
                "\"startDateOpCount\" : { \"$date\" : \"2017-04-03T00:00:00.000Z\"} , \"numberQueriesDone\" : 50} , {" +
                " \"_id\" : \"indexNameIdTest\" , \"startDateOpCount\" : { \"$date\" : \"2017-04-04T00:00:00.000Z\"} " +
                ", \"numberQueriesDone\" : 50}]}]", indexInformationList.toString());
    }

    @Test
    public void getCurrentIndexInformation() throws Exception {
        List<IndexInformation> result = IndexInformationMongoDAO.getInstance()
                .getCurrentIndexInformation("thingSnapshots");
        assertNotNull(result);
    }

    @Test
    public void getNullValue() throws Exception {
        IndexInformation indexInformation = IndexInformationMongoDAO.getInstance().getById("indexNameIdTest", null);
        assertNull(indexInformation);
    }

    @Test
    public void getStatisticsMongoIndexInformation() throws Exception {
        MongoDAOUtil.getInstance().db.getCollection("vizixIndexInformation").insert(indexInfoBasicDBObject);
        indexInfoBasicDBObject.put("_id", "indexNameIdTestTwo");
        MongoDAOUtil.getInstance().db.getCollection("vizixIndexInformation").insert(indexInfoBasicDBObject);
        List<IndexInformation> result = IndexInformationMongoDAO.getInstance().getByStatus(IndexStatus.STATS_GENERATED);
        assertNotNull(result);
        assertEquals(2L, result.size());
        assertEquals("indexNameIdTest", result.get(0).getId());
        assertEquals(IndexStatus.STATS_GENERATED, result.get(0).getStatus());
        assertEquals("indexNameIdTestTwo", result.get(1).getId());
        assertEquals(IndexStatus.STATS_GENERATED, result.get(1).getStatus());
    }

    @Test
    public void getStatisticsMongoIndexInforExcludeIndex() throws Exception {

//        MongoDAOUtil.getInstance().db.getCollection("vizixIndexInformation").insert(indexInfoBasicDBObject);
//
//        for(int i = 1; i<10 ; i ++)
//        indexInfoBasicDBObject.put("_id", "indexNameIdTestTwo");
//        MongoDAOUtil.getInstance().db.getCollection("vizixIndexInformation").insert(indexInfoBasicDBObject);
//
//        List<IndexInformation> result = IndexInformationMongoDAO.getInstance().getByStatus(IndexStatus.STATS_GENERATED,"");
//
//        assertNotNull(result);
//        assertEquals(2L, result.size());
//        assertEquals("indexNameIdTest", result.get(0).getId());
//        assertEquals(IndexStatus.STATS_GENERATED, result.get(0).getStatus());
//        assertEquals("indexNameIdTestTwo", result.get(1).getId());
//        assertEquals(IndexStatus.STATS_GENERATED, result.get(1).getStatus());
    }

    @Test
    public void delete() throws Exception {
        MongoDAOUtil.getInstance().db.getCollection(indexInfoCollection).insert(indexInfoBasicDBObject);
        assertEquals(1, MongoDAOUtil.getInstance().db.getCollection(indexInfoCollection).count());
        IndexInformationMongoDAO.getInstance().delete(indexInfoBasicDBObject.getString("_id"));
        assertEquals(0, MongoDAOUtil.getInstance().db.getCollection(indexInfoCollection).count());
    }

    @Test
    public void updateLastRunDate() throws Exception {
        MongoDAOUtil.getInstance().db.getCollection(indexInfoCollection).insert(indexInfoBasicDBObject);
        Date dateExpected = formatter.parse("2017-04-10 00:00:00");
        IndexInformationMongoDAO.getInstance().updateLastRunDate(indexInfoBasicDBObject.getString("_id"), dateExpected);
        IndexInformation result = IndexInformationMongoDAO.getInstance().getById(
                indexInfoBasicDBObject.getString("_id"), null);
        assertEquals(result.getLastRunDate(), dateExpected);
    }
}