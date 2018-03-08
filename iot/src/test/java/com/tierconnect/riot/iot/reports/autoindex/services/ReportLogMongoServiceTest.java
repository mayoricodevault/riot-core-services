package com.tierconnect.riot.iot.reports.autoindex.services;

import static com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogStatus.*;
import static org.junit.Assert.*;


import com.mongodb.BasicDBList;
import com.mongodb.DBObject;

import com.tierconnect.riot.appcore.core.BaseTestIOT;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.entities.ReportDefinition;

import com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs.ReportLogStatus;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;


/**
 * Created by achambi on 4/25/17.
 * Unit test for ReportLogMongoService.
 */
public class ReportLogMongoServiceTest extends BaseTestIOT {


    private HashMap<String, Object> reportLog;
    private HashMap<String, Object> requestInfo;
    private ReportDefinition reportDefinition;

    @Before
    public void setUp() throws Exception {
        MongoDAOUtil.getInstance().db.getCollection("reportLogs").drop();
        reportLog = new HashMap<>();
        reportLog.put("duration", 1000L);
        reportLog.put("start", 100000L);
        reportLog.put("end", 200000L);
        reportLog.put("duration", 100000L);
        reportLog.put("query", "{\"queryJson\": \"unitTest\"}");
        reportLog.put("filtersDefinition", "{ \"filtersDefinition\" : \"unitTest\"}");
        reportLog.put("count", 100L);
        reportLog.put("checked", false);
        reportLog.put("collectionName", "testCollectionName");
        reportLog.put("sort", "{ \"serialNumber\" : 1 }");
        reportLog.put("executionPlan", "{\"executionPlanTest\": \"true\"}");
        requestInfo = new HashMap<>();
        reportDefinition = Mockito.mock(ReportDefinition.class);
        Mockito.when(reportDefinition.getId()).thenReturn(101L);
        Mockito.when(reportDefinition.getReportTypeInView()).thenReturn("Report Type in View Mode");
        Mockito.when(reportDefinition.getName()).thenReturn("Report Mock Name");
    }

    @Test
    public void insertLog() throws Exception {
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        assertEquals(1, MongoDAOUtil.getInstance().db.getCollection("reportLogs").count());
    }

    @Test
    public void insertMultipleLogs() throws Exception {
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        assertEquals(1, MongoDAOUtil.getInstance().db.getCollection("reportLogs").count());

        reportLog.put("sort", "{\"thingTypeId\": 1}");
        reportLog.put("filtersDefinition", "{\"filtersDefinition\": \"unitTestUpdateSort\"}");
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        int result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").find().count();
        assertEquals(2, result);

        reportLog.put("sort", "{\"thingTypeId\": -1}");
        reportLog.put("filtersDefinition", "{\"filtersDefinition\": \"unitTestUpdateSort2\"}");
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").find().count();
        assertEquals(3L, result);
    }

    @Test
    @Ignore
    public void insertLogWithUpdatePending() throws Exception {
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        assertEquals(1, MongoDAOUtil.getInstance().db.getCollection("reportLogs").count());

        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        DBObject result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        assertEquals(1, result.get("totalRuns"));
        assertThat(result.get("runs"), instanceOf(BasicDBList.class));
        assertEquals(1, ((BasicDBList) result.get("runs")).size());
        assertEquals(1, MongoDAOUtil.getInstance().db.getCollection("reportLogs").count());

        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        assertEquals(1, MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne().get("totalRuns"));
        assertThat(result.get("runs"), instanceOf(BasicDBList.class));
        assertEquals(1, ((BasicDBList) result.get("runs")).size());
        assertEquals(1, MongoDAOUtil.getInstance().db.getCollection("reportLogs").count());
        assertThat(getEnum(result.get("status").toString()), is(IN_PROGRESS));
    }

    @Test
    public void insertLogWithStatusInProgress() {
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        DBObject result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        ReportLogMongoService.getInstance().updateStatus(result.get("_id").toString(), IN_PROGRESS);
        reportLog.put("sort", "{\"thingTypeId\": -1}");
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        assertEquals(1, MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne().get("totalRuns"));
        assertThat(result.get("runs"), instanceOf(BasicDBList.class));
        assertEquals(1, MongoDAOUtil.getInstance().db.getCollection("reportLogs").count());
        assertThat(result.get("status"), is("inProgress"));
    }

    @Test
    public void insertLogWithStatusComplete() {
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        DBObject result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        ReportLogMongoService.getInstance().updateStatus(result.get("_id").toString(), COMPLETED);
        reportLog.put("sort", "{\"thingTypeId\": -1}");
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        assertEquals(2L, MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne().get("totalRuns"));
        assertThat(result.get("runs"), instanceOf(BasicDBList.class));
        assertEquals(1, MongoDAOUtil.getInstance().db.getCollection("reportLogs").count());
        assertThat("The status should be equal to rejected!", result.get("status"), is(SLOW_INDEX.getValue()));
    }

    @Test
    public void insertLogWithStatusSlowIndex() {
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        DBObject result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        ReportLogMongoService.getInstance().updateStatus(result.get("_id").toString(), SLOW_INDEX);
        reportLog.put("sort", "{\"thingTypeId\": -1}");
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        assertEquals(1, MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne().get("totalRuns"));
        assertThat(result.get("runs"), instanceOf(BasicDBList.class));
        assertEquals(1, MongoDAOUtil.getInstance().db.getCollection("reportLogs").count());
        assertThat("The status should be equal to SLOW_INDEX!", getEnum(result.get("status").toString
                ()), is(SLOW_INDEX));
    }

    @Test
    public void insertLogWithStatusDeleted() {
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 50, true, 1);
        DBObject result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        ReportLogMongoService.getInstance().updateStatus(result.get("_id").toString(), DELETED);

        reportLog.put("sort", "{\"thingTypeId\": -1}");
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 50, true, 1);
        result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        assertEquals(2L, MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne().get("totalRuns"));

        assertThat(result.get("runs"), instanceOf(BasicDBList.class));
        assertEquals(1, MongoDAOUtil.getInstance().db.getCollection("reportLogs").count());
        assertThat("The status should be equal to SLOW_INDEX!", getEnum(result.get("status").toString
                ()), is(PENDING));
    }

    @Test
    public void insertLogWithStatusAndIndexInformation() {
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        DBObject result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        ReportLogMongoService.getInstance().updateStatusAndIndexInformation(
                result.get("_id").toString(),
                COMPLETED,
                "indexNameTest",
                "indexDefinitionTest",
                null,
                null);
        result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        assertEquals("{ \"indexName\" : \"indexNameTest\" , \"definition\" : \"indexDefinitionTest\" , \"endDate\" : " +
                " null }", result.get("indexInformation").toString());
    }

    @Test
    public void getByIndexName() throws Exception {
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        DBObject result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        ReportLogMongoService.getInstance().updateStatusAndIndexInformation(
                result.get("_id").toString(),
                COMPLETED,
                "indexNameTest",
                "indexDefinitionTest",
                null,
                null);
        List<Map<String, Object>> resultMap = ReportLogMongoService.getInstance().getByIndexName("indexNameTest", null);
        assertEquals("[{name=Report Mock Name, checked=false, " +
                "filtersDefinition={\"filtersDefinition\":\"unitTest\"}," +
                " _id=00101-8153bc349073c6db121fb443de3eaf66257f125a0cdfef3c54e3fb1076950902, " +
                "label=Completed, type=Report Type in View Mode, maxDuration=100000, status=COMPLETED, " +
                "indexInformation={endDate=null, indexName=indexNameTest, starDate=null, " +
                "definition=indexDefinitionTest}}]", resultMap.toString());
    }

    @Test
    public void getByIndexDefinition() throws Exception {
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        DBObject result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        ReportLogMongoService.getInstance().updateStatusAndIndexInformation(
                result.get("_id").toString(),
                COMPLETED,
                "indexNameTest",
                "{\"thingTypeId\":1,\"name\":1,\"groupId\":1}",
                null,
                null);
        String indexNameTest = ReportLogMongoService.getInstance().getIndexByDefinition("{\"thingTypeId\":1,\"name\":1,\"groupId\":1}");
        assertEquals("indexNameTest", indexNameTest);
    }

    @Test
    public void getByIndexDefinitionNullDefinitionInDataBase() throws Exception {
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        DBObject result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        ReportLogMongoService.getInstance().updateStatusAndIndexInformation(
                result.get("_id").toString(),
                COMPLETED,
                "indexNameTest",
                null,
                null,
                null);
        String indexNameTest = ReportLogMongoService.getInstance().getIndexByDefinition("{\"thingTypeId\":1,\"name\":1,\"groupId\":1}");
        assertEquals("", indexNameTest);
    }

    @Test
    public void getByIndexDefinitionNullDefinitionInput() throws Exception {
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        DBObject result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        ReportLogMongoService.getInstance().updateStatusAndIndexInformation(
                result.get("_id").toString(),
                COMPLETED,
                "indexNameTest",
                "{\"thingTypeId\":1,\"name\":1,\"groupId\":1}",
                null,
                null);
        String indexNameTest = ReportLogMongoService.getInstance().getIndexByDefinition(null);
        assertEquals("", indexNameTest);
    }

    @Test
    public void getByIndexDefinitionNullDefinitionDataBaseAndInput() throws Exception {
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        DBObject result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        ReportLogMongoService.getInstance().updateStatusAndIndexInformation(
                result.get("_id").toString(),
                COMPLETED,
                "indexNameTest",
                null,
                null,
                null);
        String indexNameTest = ReportLogMongoService.getInstance().getIndexByDefinition(null);
        assertEquals("indexNameTest", indexNameTest);
    }


    @Test
    public void getIndexNamesByStatus() throws Exception {
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        DBObject result = MongoDAOUtil.getInstance().db.getCollection("reportLogs").findOne();
        ReportLogMongoService.getInstance().updateStatusAndIndexInformation(
                result.get("_id").toString(),
                COMPLETED,
                "indexNameTest",
                "indexDefinitionTest",
                null,
                null);
        String[] reportLogInfoIds = ReportLogMongoService
                .getInstance()
                .getIndexNamesByStatus(ReportLogStatus.COMPLETED);
        assertEquals(1, reportLogInfoIds.length);
        assertEquals("indexNameTest", reportLogInfoIds[0]);
    }


    @Test
    public void isCurrentlyIndexing() throws Exception {
        ReportLogMongoService.getInstance().insertLog(reportLog, reportDefinition, requestInfo, 100, true, 1);
        assertEquals(true, ReportLogMongoService.getInstance().isCurrentlyIndexing(reportDefinition.getId()));
    }
}