package com.tierconnect.riot.api.mongoShell.query;

import com.tierconnect.riot.api.mongoShell.MongoShellClient;
import com.tierconnect.riot.api.mongoShell.ResultQuery;
import com.tierconnect.riot.api.mongoShell.testUtils.MongoDataBaseTestUtils;
import com.tierconnect.riot.api.mongoShell.utils.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

/**
 * Created by achambi on 12/22/16.
 * Class to test export in query builder.
 */
public class QueryBuilderExportTest {
    private static MongoShellClient mongoShellClient;
    private static final String mongoDataBase = "riot_main_test";
    private static final String mongoDataBasePath = "databaseTest/riotMainTestComparator";
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

    @Test
    public void exportCSV() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor(),
                reportTemporalFolder);
        String pathResult = queryBuilder.export(
                "{\"$and\":[{\"_id\":{\"$eq\":24}}]}",
                "{\"groupTypeId\": 1, " +
                        "\"name\": 1, " +
                        "\"serialNumber\": 1," +
                        "\"asset_code_children.name\": 1," +
                        "\"asset_code_children.serialNumber\": 1," +
                        "\"asset_code_children.children.name\": 1," +
                        "\"asset_code_children.children.serialNumber\": 1}",
                "{" +
                        "\"name\": {\"alias\": \"Nombre\", \"function\": \"none\"}, " +
                        "\"serialNumber\": {\"alias\": \"Numero de serie\", \"function\": \"none\"}," +
                        "\"asset_code_children.name\": {\"alias\": \"Nombre del hijo\", \"function\": \"none\"}," +
                        "\"asset_code_children.serialNumber\": {\"alias\": \"Serial del hijo\", \"function\": " +
                        "\"none\"}" +
                        "}",
                "tempExport", "", ResultFormat.CSV);
        File result = FileUtils.getFile(pathResult);
        String extension = FilenameUtils.getExtension(result.getPath());
        assertThat(extension, is("csv"));
        URL url = this.getClass().getClassLoader().getResource("tempExportCase5CSV.txt");
        if (url != null) {
            assertThat(org.apache.commons.io.FileUtils.contentEquals(result, new File(url.getFile())), is(true));
        } else {
            fail("Error URL is null");
        }
    }

    @Test
    public void exportCSVWithComment() throws Exception {
        MongoDataBaseTestUtils.setProfile(mongoDataBase, 2);
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor(),
                reportTemporalFolder);
        String pathResult = queryBuilder.export(
                "{\"$and\":[{\"_id\":{\"$eq\":24}}]}",
                "{\"groupTypeId\": 1, " +
                        "\"name\": 1, " +
                        "\"serialNumber\": 1," +
                        "\"asset_code_children.name\": 1," +
                        "\"asset_code_children.serialNumber\": 1," +
                        "\"asset_code_children.children.name\": 1," +
                        "\"asset_code_children.children.serialNumber\": 1}",
                "{" +
                        "\"name\": {\"alias\": \"Nombre\", \"function\": \"none\"}, " +
                        "\"serialNumber\": {\"alias\": \"Numero de serie\", \"function\": \"none\"}," +
                        "\"asset_code_children.name\": {\"alias\": \"Nombre del hijo\", \"function\": \"none\"}," +
                        "\"asset_code_children.serialNumber\": {\"alias\": \"Serial del hijo\", \"function\": " +
                        "\"none\"}" +
                        "}",
                "tempExport", "TestCommentExport", ResultFormat.CSV);
        File result = FileUtils.getFile(pathResult);
        String Extension = FilenameUtils.getExtension(result.getPath());
        assertThat(Extension, is("csv"));
        URL url = this.getClass().getClassLoader().getResource("tempExportCase5CSV.txt");
        if (url != null) {
            assertThat(org.apache.commons.io.FileUtils.contentEquals(result, new File(url.getFile())), is(true));
        } else {
            fail("Error URL is null");
        }
        QueryBuilder queryBuilderSystem = new QueryBuilder("system.profile", mongoShellClient.getExecutor());
        ResultQuery resultQuerySystem = queryBuilderSystem.find(
                "{\"query.comment\": \"TestCommentExport\"}",
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
                assertEquals("{$and=[{_id={$eq=24}}]}", resultProfile.get("filter").toString());
            }
        }
        MongoDataBaseTestUtils.setProfile(mongoDataBase, 0);
        MongoDataBaseTestUtils.deleteProfileCollection(mongoDataBase);
    }

    @Test
    public void exportScriptCSV() throws Exception {
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor(),
                reportTemporalFolder);
        String pathResult = queryBuilder.export("function formatValue(key, value){\n" +
                        "    if(value ==='')\n" +
                        "        return value;\n" +
                        "    if(!value)\n" +
                        "        return null;\n" +
                        "    switch (key) {\n" +
                        "        case \"formatDate\":\n" +
                        "            return formatDate(value); break;\n" +
                        "        case \"formatDwellTime\":\n" +
                        "            return formatDwellTime(value); break;\n" +
                        "        case \"none\":\n" +
                        "            return value.valueOf(); break;\n" +
                        "    }\n" +
                        "}\n" +
                        "function formatDwellTime(value) {\n" +
                        "    var sign = (value < 0) ? \"-\" : \"\";\n" +
                        "    value = (value < 0) ? value * -1 : value;\n" +
                        "    //print(\"value = \" + value);\n" +
                        "    var x = Math.floor(value / 1000);\n" +
                        "    //print(\"x =\" + x);\n" +
                        "    var seconds = Math.floor(x % 60);\n" +
                        "    //print(\"seconds = \" + seconds);\n" +
                        "\n" +
                        "    x = (x / 60);\n" +
                        "    var minutes = Math.floor(x % 60);\n" +
                        "    //print(\"minutes = \" + minutes);\n" +
                        "\n" +
                        "    x = (x / 60);\n" +
                        "    var hours = Math.floor(x % 24);\n" +
                        "    x = (x / 24);\n" +
                        "    var days = Math.floor(x);\n" +
                        "    var hoursS = (hours < 10 ? \"0\" + hours + \":\" : hours + \":\");\n" +
                        "    var minutesS = (minutes < 10 ? \"0\" + minutes + \":\" : minutes + \":\");\n" +
                        "    var secondsS = seconds < 10 ? \"0\" + seconds : seconds + \"\";\n" +
                        "    return sign + days + \" Days \" + hoursS + minutesS + secondsS;\n" +
                        "}\n" +
                        "function formatDate(value) {    \n" +
                        "    if(value instanceof Date) {\n" +
                        "        return value.toLocaleDateString(\"en-US\") + \" \" + value.toLocaleTimeString" +
                        "(\"en-US\");\n" +
                        "    } else {               \n" +
                        "        return (new Date(value)).toLocaleDateString(\"en-US\") + \" \" + (new Date(value))" +
                        ".toLocaleTimeString(\"en-US\");    \n" +
                        "    }    \n" +
                        "}\n" +
                        "function getValueByPath(obj, path) {\n" +
                        "    var a = path.split('.');\n" +
                        "    for (var i = 0, n = a.length; i < n; ++i) {\n" +
                        "        var k = a[i];\n" +
                        "        if (k in obj) {\n" +
                        "            if(!(obj[k].constructor === Array)){\n" +
                        "                obj = obj[k];\n" +
                        "            }else{\n" +
                        "                    obj = obj[k][0];\n" +
                        "            }\n" +
                        "        } else {\n" +
                        "            return;\n" +
                        "        }\n" +
                        "    }\n" +
                        "    return obj;\n" +
                        "}\n" +
                        "    var paths = {\"7-6\":\"shippingOrderField.value\",\"6-7\":\"asset_code_children\"," +
                        "\"8-6\":\"parent.shippingOrderField.value\",\"6-8\":\"asset_code_children.children\"," +
                        "\"8-7\":\"parent\",\"7-8\":\"children\"};\n" +
                        "    var alias = {\"thingTypeId\":{\"alias\":\"thingTypeId\",\"function\":\"none\"}," +
                        "\"name\":{\"alias\":\"name\",\"function\":\"none\"},\"groupName\":{\"alias\":\"groupName\"," +
                        "\"function\":\"none\"},\"serialNumber\":{\"alias\":\"serialNumber\",\"function\":\"none\"}," +
                        "\"thingTypeName\":{\"alias\":\"thingTypeName\",\"function\":\"none\"}," +
                        "\"groupTypeName\":{\"alias\":\"groupTypeName\",\"function\":\"none\"}," +
                        "\"serialNumber\":{\"alias\":\"serialTHIS\",\"function\":\"none\"}," +
                        "\"name\":{\"alias\":\"nameTHIS\",\"function\":\"none\"},\"name\":{\"alias\":\"nameSO\"," +
                        "\"function\":\"none\"},\"shippingOrderField.value.name\":{\"alias\":\"shippingOrderField" +
                        ".value.name\",\"function\":\"none\"},\"parent.shippingOrderField.value" +
                        ".name\":{\"alias\":\"parent.shippingOrderField.value.name\",\"function\":\"none\"}," +
                        "\"name\":{\"alias\":\"nameASSET\",\"function\":\"none\"},\"asset_code_children" +
                        ".name\":{\"alias\":\"asset_code_children.name\",\"function\":\"none\"},\"parent" +
                        ".name\":{\"alias\":\"parent.name\",\"function\":\"none\"},\"name\":{\"alias\":\"nameTAG\"," +
                        "\"function\":\"none\"},\"asset_code_children.children" +
                        ".name\":{\"alias\":\"asset_code_children.children.name\",\"function\":\"none\"},\"children" +
                        ".name\":{\"alias\":\"children.name\",\"function\":\"none\"}}\n" +
                        "    print(\"serialTHIS,nameTHIS,nameSO,nameASSET,nameTAG\");\n" +
                        "    db.getCollection('things').find({\"$and\":[{\"thingTypeId\":{\"$eq\":6}}," +
                        "{\"groupId\":{\"$in\":[1,2,3,4,5,6,7]}},{\"thingTypeId\":{\"$in\":[1,2,3,4,5,6,7,8,9,10,11," +
                        "12,13,14,15]}}]},{\"thingTypeId\":1,\"name\":1,\"groupName\":1,\"serialNumber\":1," +
                        "\"thingTypeName\":1,\"groupTypeName\":1,\"shippingOrderField.value.name\":1,\"parent" +
                        ".shippingOrderField.value.name\":1,\"asset_code_children.name\":1,\"parent.name\":1," +
                        "\"asset_code_children.children.name\":1,\"children.name\":1,\"_id\":0}    ).forEach(function" +
                        "(row){ \n" +
                        "    var rowItem = [];\n" +
                        "    rowItem.push(getValueByPath(row,\"serialNumber\"));\n" +
                        "    rowItem.push(getValueByPath(row,\"name\"));\n" +
                        "  if (paths.hasOwnProperty(getValueByPath(row,\"thingTypeId\")+\"-\"+\"6\")){\n" +
                        "            rowItem.push(formatValue(alias[\"name\"][\"function\"], \n" +
                        "            getValueByPath(row,paths[getValueByPath(row,\"thingTypeId\")+\"-\"+\"6\"]+\"" +
                        ".\"+\"name\")));\n" +
                        "        } else {\n" +
                        "            rowItem.push(formatValue(alias[\"name\"][\"function\"], getValueByPath(row," +
                        "\"name\")));   \n" +
                        "        }\n" +
                        "  if (paths.hasOwnProperty(getValueByPath(row,\"thingTypeId\")+\"-\"+\"7\")){\n" +
                        "            rowItem.push(formatValue(alias[\"name\"][\"function\"], \n" +
                        "            getValueByPath(row,paths[getValueByPath(row,\"thingTypeId\")+\"-\"+\"7\"]+\"" +
                        ".\"+\"name\")));\n" +
                        "        } else {\n" +
                        "            rowItem.push(formatValue(alias[\"name\"][\"function\"], getValueByPath(row," +
                        "\"name\")));   \n" +
                        "        }\n" +
                        "  if (paths.hasOwnProperty(getValueByPath(row,\"thingTypeId\")+\"-\"+\"8\")){\n" +
                        "            rowItem.push(formatValue(alias[\"name\"][\"function\"], \n" +
                        "            getValueByPath(row,paths[getValueByPath(row,\"thingTypeId\")+\"-\"+\"8\"]+\"" +
                        ".\"+\"name\")));\n" +
                        "        } else {\n" +
                        "            rowItem.push(formatValue(alias[\"name\"][\"function\"], getValueByPath(row," +
                        "\"name\")));   \n" +
                        "        }\n" +
                        "      \n" +
                        "print(rowItem.join(\",\"));});",
                "tempExport", ResultFormat.CSV_SCRIPT);
        File result = FileUtils.getFile(pathResult);
        String Extension = FilenameUtils.getExtension(result.getPath());
        assertThat(Extension, is("csv"));
        URL url = this.getClass().getClassLoader().getResource("tempExportCase6CSV.txt");
        if (url != null) {
            assertThat(org.apache.commons.io.FileUtils.contentEquals(result, new File(url.getFile())), is(true));
        } else {
            fail("Error URL is null");
        }
    }

    @Test
    public void exportScriptCSVWithComment() throws Exception {
        MongoDataBaseTestUtils.setProfile(mongoDataBase, 2);
        QueryBuilder queryBuilder = new QueryBuilder("things", mongoShellClient.getExecutor(),
                reportTemporalFolder);
        String pathResult = queryBuilder.export("function formatValue(key, value){\n" +
                        "    if(value ==='')\n" +
                        "        return value;\n" +
                        "    if(!value)\n" +
                        "        return null;\n" +
                        "    switch (key) {\n" +
                        "        case \"formatDate\":\n" +
                        "            return formatDate(value); break;\n" +
                        "        case \"formatDwellTime\":\n" +
                        "            return formatDwellTime(value); break;\n" +
                        "        case \"none\":\n" +
                        "            return value.valueOf(); break;\n" +
                        "    }\n" +
                        "}\n" +
                        "function formatDwellTime(value) {\n" +
                        "    var sign = (value < 0) ? \"-\" : \"\";\n" +
                        "    value = (value < 0) ? value * -1 : value;\n" +
                        "    //print(\"value = \" + value);\n" +
                        "    var x = Math.floor(value / 1000);\n" +
                        "    //print(\"x =\" + x);\n" +
                        "    var seconds = Math.floor(x % 60);\n" +
                        "    //print(\"seconds = \" + seconds);\n" +
                        "\n" +
                        "    x = (x / 60);\n" +
                        "    var minutes = Math.floor(x % 60);\n" +
                        "    //print(\"minutes = \" + minutes);\n" +
                        "\n" +
                        "    x = (x / 60);\n" +
                        "    var hours = Math.floor(x % 24);\n" +
                        "    x = (x / 24);\n" +
                        "    var days = Math.floor(x);\n" +
                        "    var hoursS = (hours < 10 ? \"0\" + hours + \":\" : hours + \":\");\n" +
                        "    var minutesS = (minutes < 10 ? \"0\" + minutes + \":\" : minutes + \":\");\n" +
                        "    var secondsS = seconds < 10 ? \"0\" + seconds : seconds + \"\";\n" +
                        "    return sign + days + \" Days \" + hoursS + minutesS + secondsS;\n" +
                        "}\n" +
                        "function formatDate(value) {    \n" +
                        "    if(value instanceof Date) {\n" +
                        "        return value.toLocaleDateString(\"en-US\") + \" \" + value.toLocaleTimeString" +
                        "(\"en-US\");\n" +
                        "    } else {               \n" +
                        "        return (new Date(value)).toLocaleDateString(\"en-US\") + \" \" + (new Date(value))" +
                        ".toLocaleTimeString(\"en-US\");    \n" +
                        "    }    \n" +
                        "}\n" +
                        "function getValueByPath(obj, path) {\n" +
                        "    var a = path.split('.');\n" +
                        "    for (var i = 0, n = a.length; i < n; ++i) {\n" +
                        "        var k = a[i];\n" +
                        "        if (k in obj) {\n" +
                        "            if(!(obj[k].constructor === Array)){\n" +
                        "                obj = obj[k];\n" +
                        "            }else{\n" +
                        "                    obj = obj[k][0];\n" +
                        "            }\n" +
                        "        } else {\n" +
                        "            return;\n" +
                        "        }\n" +
                        "    }\n" +
                        "    return obj;\n" +
                        "}\n" +
                        "    var paths = {\"7-6\":\"shippingOrderField.value\",\"6-7\":\"asset_code_children\"," +
                        "\"8-6\":\"parent.shippingOrderField.value\",\"6-8\":\"asset_code_children.children\"," +
                        "\"8-7\":\"parent\",\"7-8\":\"children\"};\n" +
                        "    var alias = {\"thingTypeId\":{\"alias\":\"thingTypeId\",\"function\":\"none\"}," +
                        "\"name\":{\"alias\":\"name\",\"function\":\"none\"},\"groupName\":{\"alias\":\"groupName\"," +
                        "\"function\":\"none\"},\"serialNumber\":{\"alias\":\"serialNumber\",\"function\":\"none\"}," +
                        "\"thingTypeName\":{\"alias\":\"thingTypeName\",\"function\":\"none\"}," +
                        "\"groupTypeName\":{\"alias\":\"groupTypeName\",\"function\":\"none\"}," +
                        "\"serialNumber\":{\"alias\":\"serialTHIS\",\"function\":\"none\"}," +
                        "\"name\":{\"alias\":\"nameTHIS\",\"function\":\"none\"},\"name\":{\"alias\":\"nameSO\"," +
                        "\"function\":\"none\"},\"shippingOrderField.value.name\":{\"alias\":\"shippingOrderField" +
                        ".value.name\",\"function\":\"none\"},\"parent.shippingOrderField.value" +
                        ".name\":{\"alias\":\"parent.shippingOrderField.value.name\",\"function\":\"none\"}," +
                        "\"name\":{\"alias\":\"nameASSET\",\"function\":\"none\"},\"asset_code_children" +
                        ".name\":{\"alias\":\"asset_code_children.name\",\"function\":\"none\"},\"parent" +
                        ".name\":{\"alias\":\"parent.name\",\"function\":\"none\"},\"name\":{\"alias\":\"nameTAG\"," +
                        "\"function\":\"none\"},\"asset_code_children.children" +
                        ".name\":{\"alias\":\"asset_code_children.children.name\",\"function\":\"none\"},\"children" +
                        ".name\":{\"alias\":\"children.name\",\"function\":\"none\"}}\n" +
                        "    print(\"serialTHIS,nameTHIS,nameSO,nameASSET,nameTAG\");\n" +
                        "    db.getCollection('things').find({\"$and\":[{\"thingTypeId\":{\"$eq\":6}}," +
                        "{\"groupId\":{\"$in\":[1,2,3,4,5,6,7]}},{\"thingTypeId\":{\"$in\":[1,2,3,4,5,6,7,8,9,10,11," +
                        "12,13,14,15]}}]},{\"thingTypeId\":1,\"name\":1,\"groupName\":1,\"serialNumber\":1," +
                        "\"thingTypeName\":1,\"groupTypeName\":1,\"shippingOrderField.value.name\":1,\"parent" +
                        ".shippingOrderField.value.name\":1,\"asset_code_children.name\":1,\"parent.name\":1," +
                        "\"asset_code_children.children.name\":1,\"children.name\":1,\"_id\":0}    ).comment" +
                        "(\"ExportScriptComment\").forEach(function" +
                        "(row){ \n" +
                        "    var rowItem = [];\n" +
                        "    rowItem.push(getValueByPath(row,\"serialNumber\"));\n" +
                        "    rowItem.push(getValueByPath(row,\"name\"));\n" +
                        "  if (paths.hasOwnProperty(getValueByPath(row,\"thingTypeId\")+\"-\"+\"6\")){\n" +
                        "            rowItem.push(formatValue(alias[\"name\"][\"function\"], \n" +
                        "            getValueByPath(row,paths[getValueByPath(row,\"thingTypeId\")+\"-\"+\"6\"]+\"" +
                        ".\"+\"name\")));\n" +
                        "        } else {\n" +
                        "            rowItem.push(formatValue(alias[\"name\"][\"function\"], getValueByPath(row," +
                        "\"name\")));   \n" +
                        "        }\n" +
                        "  if (paths.hasOwnProperty(getValueByPath(row,\"thingTypeId\")+\"-\"+\"7\")){\n" +
                        "            rowItem.push(formatValue(alias[\"name\"][\"function\"], \n" +
                        "            getValueByPath(row,paths[getValueByPath(row,\"thingTypeId\")+\"-\"+\"7\"]+\"" +
                        ".\"+\"name\")));\n" +
                        "        } else {\n" +
                        "            rowItem.push(formatValue(alias[\"name\"][\"function\"], getValueByPath(row," +
                        "\"name\")));   \n" +
                        "        }\n" +
                        "  if (paths.hasOwnProperty(getValueByPath(row,\"thingTypeId\")+\"-\"+\"8\")){\n" +
                        "            rowItem.push(formatValue(alias[\"name\"][\"function\"], \n" +
                        "            getValueByPath(row,paths[getValueByPath(row,\"thingTypeId\")+\"-\"+\"8\"]+\"" +
                        ".\"+\"name\")));\n" +
                        "        } else {\n" +
                        "            rowItem.push(formatValue(alias[\"name\"][\"function\"], getValueByPath(row," +
                        "\"name\")));   \n" +
                        "        }\n" +
                        "      \n" +
                        "print(rowItem.join(\",\"));});",
                "tempExport", ResultFormat.CSV_SCRIPT);
        File result = FileUtils.getFile(pathResult);
        String Extension = FilenameUtils.getExtension(result.getPath());
        assertThat(Extension, is("csv"));
        URL url = this.getClass().getClassLoader().getResource("tempExportCase6CSV.txt");
        if (url != null) {
            assertThat(org.apache.commons.io.FileUtils.contentEquals(result, new File(url.getFile())), is(true));
        } else {
            fail("Error URL is null");
        }
        QueryBuilder queryBuilderSystem = new QueryBuilder("system.profile", mongoShellClient.getExecutor());
        ResultQuery resultQuerySystem = queryBuilderSystem.find(
                "{\"query.comment\": \"ExportScriptComment\"}",
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
                assertEquals("{$and=[{thingTypeId={$eq=6}}, {groupId={$in=[1, 2, 3, 4, 5, 6, 7]}}, " +
                        "{thingTypeId={$in=[" +
                        "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15]}}]}", resultProfile.get("filter")
                        .toString());
            }
        }
        MongoDataBaseTestUtils.setProfile(mongoDataBase, 0);
        MongoDataBaseTestUtils.deleteProfileCollection(mongoDataBase);

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
}
