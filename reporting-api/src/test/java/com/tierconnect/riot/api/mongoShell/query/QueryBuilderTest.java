package com.tierconnect.riot.api.mongoShell.query;

import com.tierconnect.riot.api.mongoShell.MongoShellClient;
import com.tierconnect.riot.api.mongoShell.ResultQuery;
import com.tierconnect.riot.api.mongoShell.testUtils.MongoDataBaseTestUtils;
import com.tierconnect.riot.api.mongoShell.utils.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.*;

/**
 * Created by achambi on 12/2/16.
 * Class to test query builder.
 */
@SuppressWarnings("unchecked")
public class QueryBuilderTest {

    private static MongoShellClient mongoShellClient;
    private static final String mongoDataBase = "riot_main_test";
    private static final String mongoDataBasePath = "databaseTest/riotMainTestQuery";
    private static final URL TEMP_FOLDER_REPORT = QueryBuilderTest.class.getClassLoader().getResource
            ("temporalReportFolder");
    private static String reportTemporalFolder;

    @BeforeClass
    public static void setUp() throws Exception {
        mongoShellClient = new MongoShellClient("admin", "control123!", "admin",
                mongoDataBase, "127.0.0.1", 27017);
        MongoDataBaseTestUtils.dropDataBase(mongoDataBase);
        MongoDataBaseTestUtils.createDataBase(mongoDataBase, mongoDataBasePath);
        if (TEMP_FOLDER_REPORT != null) {
            org.apache.commons.io.FileUtils.cleanDirectory(new File(TEMP_FOLDER_REPORT.getFile()));
            reportTemporalFolder = TEMP_FOLDER_REPORT.getPath();
        } else {
            fail("could not be clean the temp directory!");
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        MongoDataBaseTestUtils.dropDataBase(mongoDataBase);
        URL tempFolder = QueryBuilderTest.class.getClassLoader().getResource("temporalReportFolder");
        if (tempFolder != null) {
            org.apache.commons.io.FileUtils.cleanDirectory(new File(tempFolder.getFile()));
        } else {
            fail("could not be clean the temp directory!");
        }
    }

    @Test
    public void setOptions() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor());
        queryBuilder.setOptions(new Options("{\"zone.value.code\":-1}", 10, 10));
        ResultQuery result = queryBuilder.find("{}", "{}", "", "tempFile");
        assertThat(result.getRows().size(), is(10));
    }

    @Test
    public void QueryBuilder() throws Exception {
        new QueryBuilder("things", mongoShellClient.getExecutor(), new Options(), "/tmp");
    }

    @Test
    public void setCollection() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor());
        queryBuilder.sort("{\"serialNumber\":1}");
        ResultQuery result = queryBuilder.find(
                "{\"thingTypeCode\": \"DiscoveredOne\"}",
                "{ \"serialNumber\": 1, \"zone.value.name\": 1, \"zone.value.code\": 1, \"logicalReader.value.code\":" +
                        " 1, \"groupUdf.value.name\": 1, \"shift.value.name\": 1}", "", "result.txt");
        assertThat(result.getTotal(), is(1299));
    }

    @Test
    public void skip() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor());

        /*TestCase 1 */
        queryBuilder.skip(1300);
        ResultQuery result = queryBuilder.find("{}", "{}", "", "temp.js");
        assertThat(result.getRows().size(), is(40));

        /*TestCase 2 */
        queryBuilder.skip(2);
        result = queryBuilder.find("{}", "{}", "", "temp.js");
        assertThat(result.getRows().size(), is(1338));

        /*TestCase 3 */
        queryBuilder.skip(1);
        result = queryBuilder.find("{}", "{}", "", "temp.js");
        assertThat(result.getRows().size(), is(1339));
    }

    @Test(expected = Exception.class)
    public void skipError() throws Exception {
        /*TestCase 4 */
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor());
        queryBuilder.skip(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void skipError2() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor());
        queryBuilder.skip(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void limitException() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor());
        queryBuilder.limit(-1);
    }

    @Test
    public void limit() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor());

        /*TestCase 1 */
        queryBuilder.limit(1300);
        ResultQuery result = queryBuilder.find("{}", "{}", "", "temp.js");
        assertThat(result.getRows().size(), is(1300));

        /*TestCase 2 */
        queryBuilder.limit(100);
        result = queryBuilder.find("{}", "{}", "", "temp.js");
        assertThat(result.getRows().size(), is(100));

        /*TestCase 3 */
        queryBuilder.limit(50);
        result = queryBuilder.find("{}", "{}", "", "temp.js");
        assertThat(result.getRows().size(), is(50));
    }

    @Test
    public void sort() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor());
        /*TestCase 1 */
        queryBuilder.sort("{\"_id\": 1}");
        queryBuilder.limit(10);
        ResultQuery result = queryBuilder.find("{}", "{\"serialNumber\": 1}", "", "temp.js");
        assertThat(result.getRows().size(), is(10));
        long index = 1;
        for (Map item : result.getRows()) {
            assertEquals(index, (long) item.get("_id"));
            index++;
        }
    }

    @Test
    public void executeFind() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor());
        for (int index = 50; index <= 1100; index = index + 50) {
            queryBuilder.limit(index);
            ResultQuery result = queryBuilder.find("{}", "{\"serialNumber\": 1}", "", null);
            assertThat(result.getRows().size(), is(index));
        }
        for (int index = 50; index <= 450; index = index + 50) {
            queryBuilder.limit(index);
            ResultQuery result = queryBuilder.find("{}",
                    "{\"serialNumber\": 1, " +
                            "\"zone.value.id\": 1," +
                            "\"zone.value.name\": 1}", "", null);
            assertThat(result.getRows().size(), is(index));
        }

        for (int index = 50; index <= 400; index = index + 50) {
            queryBuilder.limit(index);
            ResultQuery result = queryBuilder.find("{}",
                    "{\"serialNumber\": 1, " +
                            "\"zone.value.id\": 1," +
                            "\"zone.value.name\": 1," +
                            "\"zone.value.code\": 1" +
                            "}",
                    "",
                    null);
            assertThat(result.getRows().size(), is(index));
        }
    }

    @Test
    public void executeFindCount() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor());
        /*TestCase 1 */
        queryBuilder.sort("{\"_id\": 1}");
        queryBuilder.limit(10);
        int result = queryBuilder.find("{}", "{}", "", null).getTotal();
        assertThat(result, is(1340));
        result = queryBuilder.find("{\"thingTypeCode\": \"DiscoveredOne\"}", "{}", "", null).getTotal();
        assertThat(result, is(1299));
        result = queryBuilder.find("{\"thingTypeCode\": \"Color\"}", "{}", "", null).getTotal();
        assertThat(result, is(4));
        result = queryBuilder.find("{\"thingTypeCode\": \"asset_code\"}", "{}", "", null).getTotal();
        assertThat(result, is(5));
        result = queryBuilder.find("{\"thingTypeCode\": \"pants_code\"}", "{}", "", null).getTotal();
        assertThat(result, is(5));
        result = queryBuilder.find("{\"thingTypeCode\": \"tag_code\"}", "{}", "", null).getTotal();
        assertThat(result, is(5));
        result = queryBuilder.find("{\"thingTypeCode\": \"jackets_code\"}", "{}", "", null).getTotal();
        assertThat(result, is(5));
        result = queryBuilder.find("{\"thingTypeCode\": \"default_gps_thingtype\"}", "{}", "", null)
                .getTotal();
        assertThat(result, is(1));
        result = queryBuilder.find("{\"thingTypeCode\": \"shippingorder_code\"}", "{}", "", null).getTotal();
        assertThat(result, is(5));
        result = queryBuilder.find("{\"thingTypeCode\": \"default_rfid_thingtype\"}", "{}", "", null)
                .getTotal();
        assertThat(result, is(11));
    }

    @Ignore
    @Test(expected = Exception.class)
    public void executeFindCountFail() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor());
        queryBuilder.find("{error}", null, "", null);
    }

    @Test
    public void pagination() throws Exception {

    }

    @Test
    public void dateTest() throws Exception {
        System.out.println(new Date());
        SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
        System.out.println(format.format(new Date()));
    }

    @Test
    public void exportJSON() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor(),
                reportTemporalFolder);
        String pathResult = queryBuilder.export(
                "{\"thingTypeCode\": \"DiscoveredOne\"}",
                "{\"serialNumber\": 1, \"time\": 1, \"modifiedTime\": 1 }",
                "{" +
                        "\"_id\": {\"alias\": \"_id\", \"function\": \"none\"}, " +
                        "\"serialNumber\": {\"alias\": \"serial\", \"function\": \"none\"}," +
                        "\"time\": {\"alias\": \"timeAlias\", \"function\": \"formatDwellTime\"}," +
                        "\"modifiedTime\": {\"alias\": \"modified\", \"function\": \"formatDate\"}" +
                        "}",
                "tempExport", "", ResultFormat.JSON);
        File result = FileUtils.getFile(pathResult);
        URL url = this.getClass().getClassLoader().getResource("tempExportCase1JSON.txt");
        if (url != null) {
            assertThat(org.apache.commons.io.FileUtils.contentEquals(result, new File(url.getFile())), is(true));
        } else {
            fail("Error URL is null");
        }

    }

    @Test
    public void exportCSV() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor(),
                reportTemporalFolder);
        String pathResult = queryBuilder.export(
                "{\"thingTypeCode\": \"DiscoveredOne\"}",
                "{\"serialNumber\": 1, \"time\": 1, \"modifiedTime\": 1 }",
                "{" +
                        "\"_id\": {\"alias\": \"_id\", \"function\": \"none\"}, " +
                        "\"serialNumber\": {\"alias\": \"serial\", \"function\": \"none\"}," +
                        "\"time\": {\"alias\": \"timeAlias\", \"function\": \"formatDwellTime\"}," +
                        "\"modifiedTime\": {\"alias\": \"modified\", \"function\": \"formatDate\"}" +
                        "}",
                "tempExport", "", ResultFormat.CSV);
        File result = FileUtils.getFile(pathResult);
        String Extension = FilenameUtils.getExtension(result.getPath());
        assertThat(Extension, is("csv"));
        URL url = this.getClass().getClassLoader().getResource("tempExportCase1CSV.txt");
        if (url != null) {
            assertThat(org.apache.commons.io.FileUtils.contentEquals(result, new File(url.getFile())), is(true));
        } else {
            fail("Error URL is null");
        }
    }

    @Test
    public void exportWithCustomConfigCSV() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor(),
                reportTemporalFolder);
        String pathResult = queryBuilder.export(
                "{\"thingTypeCode\": \"DiscoveredOne\"}",
                "{\"serialNumber\": 1, \"time\": 1, \"modifiedTime\": 1 }",
                "{" +
                        "\"_id\": {\"alias\": \"_id\", \"function\": \"none\"}, " +
                        "\"serialNumber\": {\"alias\": \"serial\", \"function\": \"none\"}," +
                        "\"time\": {\"alias\": \"timeAlias\", \"function\": \"formatDwellTime\"}," +
                        "\"modifiedTime\": {\"alias\": \"modified\", \"function\": \"formatDate\"}" +
                        "}",
                "tempExport", "", ResultFormat.CSV);
        File result = FileUtils.getFile(pathResult);
        String Extension = FilenameUtils.getExtension(result.getPath());
        assertThat(Extension, is("csv"));
        URL url = this.getClass().getClassLoader().getResource("tempExportCase2CSV.txt");
        if (url != null) {
            assertThat(org.apache.commons.io.FileUtils.contentEquals(result, new File(url.getFile())), is(true));
        } else {
            fail("Error URL is null");
        }
    }

    @Test
    public void exportWithCustomConfigCSVAndUndefinedAlias() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor(),
                reportTemporalFolder);
        String pathResult = queryBuilder.export(
                "{\"thingTypeCode\": \"DiscoveredOne\"}",
                "{\"serialNumber\": 1, \"time\": 1, \"fieldNotExisting\": 1}",
                "{\"_id\": {\"alias\": \"_id\", \"function\": \"none\"}, " +
                        "\"serialNumber\": {\"alias\": \"serial\", \"function\": \"none\"}," +
                        "\"time\": {\"alias\": \"timeAlias\", \"function\": \"formatDwellTime\"}," +
                        "\"fieldNotExists\": {\"alias\": \"fieldNotExistsAlias\", \"function\": \"none\"}" +
                        "}", "tempExport", "", ResultFormat.CSV);
        File result = FileUtils.getFile(pathResult);
        URL url = this.getClass().getClassLoader().getResource("tempExportCase4CSV.txt");
        if (url != null) {
            assertThat(org.apache.commons.io.FileUtils.contentEquals(result, new File(url.getFile())), is(true));
        } else {
            fail("Error URL is null");
        }
    }

    @Test
    public void exportWithCustomConfigAndMultipleFieldsCSV() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("thingSnapshots", mongoShellClient.getExecutor(),
                reportTemporalFolder);
        String pathResult = queryBuilder.export(
                "{\"value.thingTypeCode\": \"DiscoveredOne\"}",
                "{\n" +
                        "    \"_id\": 1, \n" +
                        "    \"value.thingTypeCode\": 1,\n" +
                        "    \"value.thingTypeName\": 1,\n" +
                        "    \"value.name\": 1,\n" +
                        "    \"value.serialNumber\": 1,\n" +
                        "    \"value.serialNumber\": 1,\n" +
                        "    \"value.modifiedTime\": 1,\n" +
                        "    \"value.createdTime\": 1,\n" +
                        "    \"value.zone.thingTypeFieldId\": 1,\n" +
                        "    \"value.zone.time\": 1,\n" +
                        "    \"value.zone.value.code\": 1,\n" +
                        "    \"value.zone.dwellTime\": 1    \n" +
                        "}",
                "{\n" +
                        "    \"_id\": {\"alias\": \"_id\", \"function\": \"none\"}, \n" +
                        "    \"value.thingTypeCode\": {\"alias\": \"thing Type Code\", \"function\": \"none\"},\n" +
                        "    \"value.thingTypeName\": {\"alias\": \"thing Type Name\", \"function\": \"none\"},\n" +
                        "    \"value.name\": {\"alias\": \"name\", \"function\": \"none\"},\n" +
                        "    \"value.serialNumber\": {\"alias\": \"serialNumber\", \"function\": \"none\"},\n" +
                        "    \"value.modifiedTime\": {\"alias\": \"modifiedTime\", \"function\": \"formatDate\"},\n" +
                        "    \"value.createdTime\": {\"alias\": \"createdTime\", \"function\": \"formatDate\"},\n" +
                        "    \"value.zone.thingTypeFieldId\": {\"alias\": \"zone Thing Type\", \"function\": " +
                        "\"none\"},\n" +
                        "    \"value.zone.time\": {\"alias\": \"zone Time\", \"function\": \"formatDate\"},\n" +
                        "    \"value.zone.value.code\": {\"alias\": \"zone Code\", \"function\": \"none\"},\n" +
                        "    \"value.zone.dwellTime\": {\"alias\": \"zone dwellTime\", \"function\": " +
                        "\"formatDwellTime\"}    \n" +
                        "}",
                "tempExport", "", ResultFormat.CSV);
        File result = FileUtils.getFile(pathResult);
        URL url = this.getClass().getClassLoader().getResource("tempExportCase3CSV.txt");
        if (url != null) {
            assertThat(org.apache.commons.io.FileUtils.contentEquals(result, new File(url.getFile())), is(true));
        } else {
            fail("Error URL is null");
        }
    }

    @Test
    public void exportWithNoneFindAndFieldFormatCSV() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("thingSnapshots", mongoShellClient.getExecutor(),
                reportTemporalFolder);
        String pathResult = queryBuilder.export(
                "{}",
                "{\n" +
                        "    \"_id\": 1, \n" +
                        "    \"value.thingTypeCode\": 1,\n" +
                        "    \"value.thingTypeName\": 1,\n" +
                        "    \"value.name\": 1,\n" +
                        "    \"value.serialNumber\": 1,\n" +
                        "    \"value.serialNumber\": 1,\n" +
                        "    \"value.modifiedTime\": 1,\n" +
                        "    \"value.createdTime\": 1,\n" +
                        "    \"value.zone.thingTypeFieldId\": 1,\n" +
                        "    \"value.zone.time\": 1,\n" +
                        "    \"value.zone.value.code\": 1,\n" +
                        "    \"value.zone.dwellTime\": 1    \n" +
                        "}",
                "{\n" +
                        "    \"_id\": {\"alias\": \"_id\", \"function\": \"none\"}, \n" +
                        "    \"value.thingTypeCode\": {\"alias\": \"thing Type Code\", \"function\": \"none\"},\n" +
                        "    \"value.thingTypeName\": {\"alias\": \"thing Type Name\", \"function\": \"none\"},\n" +
                        "    \"value.name\": {\"alias\": \"name\", \"function\": \"none\"},\n" +
                        "    \"value.serialNumber\": {\"alias\": \"serialNumber\", \"function\": \"none\"},\n" +
                        "    \"value.modifiedTime\": {\"alias\": \"modifiedTime\", \"function\": \"formatDate\"},\n" +
                        "    \"value.createdTime\": {\"alias\": \"createdTime\", \"function\": \"formatDate\"},\n" +
                        "    \"value.zone.thingTypeFieldId\": {\"alias\": \"zone Thing Type\", \"function\": " +
                        "\"none\"},\n" +
                        "    \"value.zone.time\": {\"alias\": \"zone Time\", \"function\": \"formatDate\"},\n" +
                        "    \"value.zone.value.code\": {\"alias\": \"zone Code\", \"function\": \"none\"},\n" +
                        "    \"value.zone.dwellTime\": {\"alias\": \"zone dwellTime\", \"function\": " +
                        "\"formatDwellTime\"}    \n" +
                        "}",
                "tempExport", "", ResultFormat.CSV);
        System.out.println(pathResult);
    }

    @Test
    public void find() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor());
        ResultQuery result = queryBuilder.find("{\"thingTypeCode\": \"DiscoveredOne\"}", "{\"serialNumber\": 1, " +
                "\"time\": 1 }", "", "tempExport");
        assertThat(result.getTotal(), is(1299));
        assertThat(result.getRows().size(), is(1299));
    }

    @Test
    public void findWithExplain() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor());
        ResultQuery result = queryBuilder.find("{\"thingTypeCode\": \"DiscoveredOne\"}", "{\"serialNumber\": 1, " +
                "\"time\": 1 }", "", "tempExport", true);
        assertThat(result.getTotal(), is(1299));
        assertThat(result.getRows().size(), is(1299));
        assertNotNull(result.getExecutionPlan());
        assertThat(result.getExecutionPlan().get("queryPlanner"), instanceOf(LinkedHashMap.class));
        LinkedHashMap<String, Object> resultQueryPlanner = (LinkedHashMap) result.getExecutionPlan().get
                ("queryPlanner");
        assertThat(resultQueryPlanner.get("winningPlan"), instanceOf(LinkedHashMap.class));
        LinkedHashMap<String, Object> resultWinningPlan = (LinkedHashMap) resultQueryPlanner.get("winningPlan");
        assertThat(resultWinningPlan, instanceOf(LinkedHashMap.class));
        assertThat(resultWinningPlan.toString(), startsWith("{stage=PROJECTION, transformBy={serialNumber=1.0, " +
                "time=1.0}, inputStage={stage=FETCH, inputStage={stage=IXSCAN, keyPattern={thingTypeCode=1.0, " +
                "serialNumber=1.0}, indexName=uniqueSerialNumber, isMultiKey=false"));
    }

    @Test
    public void findWithSkipLimitAndExplain() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor());
        queryBuilder.skip(100);
        queryBuilder.limit(200);
        ResultQuery result = queryBuilder.find("{\"thingTypeCode\": \"DiscoveredOne\"}", "{\"serialNumber\": 1, " +
                "\"time\": 1 }", "", "tempExport", true);
        assertThat(result.getTotal(), is(1299));
        assertThat(result.getRows().size(), is(200));
        assertNotNull(result.getExecutionPlan());
        assertThat(result.getExecutionPlan().get("queryPlanner"), instanceOf(LinkedHashMap.class));
        LinkedHashMap<String, Object> resultQueryPlanner = (LinkedHashMap) result.getExecutionPlan().get
                ("queryPlanner");
        assertThat(resultQueryPlanner.get("winningPlan"), instanceOf(LinkedHashMap.class));
        LinkedHashMap<String, Object> resultWinningPlan = (LinkedHashMap) resultQueryPlanner.get("winningPlan");
        assertThat(resultWinningPlan, instanceOf(LinkedHashMap.class));
        assertThat(resultWinningPlan.toString(), startsWith("{stage=LIMIT, limitAmount=200.0, inputStage={stage=SKIP," +
                " skipAmount=0.0, inputStage={stage=PROJECTION, transformBy={serialNumber=1.0, time=1.0}, " +
                "inputStage={stage=FETCH, inputStage={stage=IXSCAN, keyPattern={thingTypeCode=1.0, " +
                "serialNumber=1.0}, indexName=uniqueSerialNumber"));
    }

    @Test
    public void findTestCase1() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("thingSnapshotIds", mongoShellClient.getExecutor());
        ResultQuery result = queryBuilder.find(
                "{\"_id\": 44}",
                "{\"thingTypeId\": 1, \"thingTypeCode\": 1, \"blinks\": 1}", "", "tempExport");
        assertThat(result.getTotal(), is(1));
        assertThat(result.getRows().size(), is(1));
    }

    @Test
    public void findTestCase2() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("thingSnapshotIds", mongoShellClient.getExecutor());
        ResultQuery result = queryBuilder.find("{\"$and\":[{\"_id\":{\"$eq\":2}}]}", "{}", "", "tempExport");
        assertThat(result.getTotal(), is(1));
        assertThat(result.getRows().size(), is(1));
    }

    @Test
    public void findTestCaseFieldNotExisting() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor());
        ResultQuery result = queryBuilder.find("{\"thingTypeCode\": \"DiscoveredOne\"}", "{\"serialNumber\": 1, " +
                "\"time\": 1, \"fieldNotExisting\": 1}", "", "tempExport");
        assertThat(result.getTotal(), is(1299));
        assertThat(result.getRows().size(), is(1299));
        for (Map<String, Object> item : result.getRows()) {
            assertThat(item.containsKey("fieldNotExistsAlias"), is(false));
        }
    }

    @Test
    public void findTestCaseSetComment() throws Exception {
        MongoDataBaseTestUtils.setProfile(mongoDataBase, 2);
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor());
        ResultQuery result = queryBuilder.find(
                "{\"thingTypeCode\": \"DiscoveredOne\"}",
                "{\"serialNumber\": 1, \"time\": 1, \"fieldNotExisting\": 1}",
                "TestCommentOne",
                "tempExport");
        assertThat(result.getTotal(), is(1299));
        assertThat(result.getRows().size(), is(1299));
        QueryBuilder queryBuilderSystem = new QueryBuilder("system.profile", mongoShellClient.getExecutor());
        ResultQuery resultQuerySystem = queryBuilderSystem.find(
                "{\"query.comment\": \"TestCommentOne\"}",
                "{\"query.filter\": 1}",
                "",
                "");
        assertEquals(resultQuerySystem.getTotal(), 1);
        for (Map<String, Object> operation :
                resultQuerySystem.getRows()) {
            Map resultProfile = null;
            if (operation.containsKey("query")) {
                resultProfile = (Map) operation.get("query");
            }
            if (resultProfile != null && resultProfile.containsKey("filter")) {
                assertEquals("{thingTypeCode=DiscoveredOne}", resultProfile.get("filter").toString());
            }
        }
        MongoDataBaseTestUtils.setProfile(mongoDataBase, 0);
        MongoDataBaseTestUtils.deleteProfileCollection(mongoDataBase);
    }

    @Test
    public void valueOfResultFormat() {
        assertEquals(ResultFormat.BSON, ResultFormat.valueOf("BSON"));
        assertEquals(ResultFormat.JSON, ResultFormat.valueOf("JSON"));
        assertEquals(ResultFormat.CSV, ResultFormat.valueOf("CSV"));
        assertEquals(ResultFormat.CSV_SCRIPT, ResultFormat.valueOf("CSV_SCRIPT"));
    }

    @Test
    public void findTestWithIndexError() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("thingSnapshotIds", mongoShellClient.getExecutor());
        try {
            queryBuilder.find(
                    "{\"thingTypeCode\": \"DiscoveredOne\"}",
                    "{\"serialNumber\": 1, \"time\": 1, \"fieldNotExisting\": 1}",
                    "",
                    "",
                    false,
                    "indexTest", true, true);
        } catch (IllegalArgumentException ex) {
            assertEquals("The index \"indexTest\" was deleted", ex.getMessage());
        }
    }

    @Test
    public void findTestWithSyntaxErrorCase1() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("thingSnapshotIds", mongoShellClient.getExecutor());
        try {
            queryBuilder.find(
                    "{\"thingTypeCode\": \"DiscoveredOne\"}",
                    "{\"serialNumber\": 1, \"time\": 1, \"fieldNotExisting\": 1:}",
                    "",
                    "",
                    false);
        } catch (IOException ex) {
            assertEquals("Error in query syntax", ex.getMessage());
        }
    }

    @Test
    public void findTestWithSyntaxErrorCase2() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("thingSnapshotIds", mongoShellClient.getExecutor());
        try {
            queryBuilder.find(
                    "{\"thingTypeCode\":: \"DiscoveredOne\"}",
                    "{\"serialNumber\": 1, \"time\": 1, \"fieldNotExisting\": 1}",
                    "",
                    "",
                    false,
                    "indexTest", true, true);
        } catch (IOException ex) {
            assertEquals("Error in query syntax", ex.getMessage());
        }
    }

    @Test
    public void findTestWithZeroRows() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("thingSnapshotIds", mongoShellClient.getExecutor());
        ResultQuery resultQuery = queryBuilder.find(
                "{\"thingTypeCode\": \"TTCodeNotExists\"}",
                "{\"serialNumber\": 1, \"time\": 1, \"fieldNotExisting\": 1}",
                "",
                "",
                true);
        assertEquals(0, resultQuery.getRows().size());
        assertNotNull(resultQuery.getExecutionPlan());
    }
}
