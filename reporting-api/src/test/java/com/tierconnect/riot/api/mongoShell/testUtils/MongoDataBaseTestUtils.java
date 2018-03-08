package com.tierconnect.riot.api.mongoShell.testUtils;

import com.tierconnect.riot.api.mongoShell.query.QueryBuilderTest;
import com.tierconnect.riot.api.mongoShell.utils.ShellCommand;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.fail;

/**
 * Created by achambi on 12/3/16.
 * Util to create dataBase.
 */
public class MongoDataBaseTestUtils {

    private static final String mongoDataBase = "riot_main_test";
    private static final String mongoDataBasePath = "databaseTest/riotMainTestComparator";
    private static final URL TEMP_FOLDER_REPORT = QueryBuilderTest.class.getClassLoader().getResource("temporalReportFolder");

    /**
     * Method used in test cases to create the test database.
     *
     * @param dataBase the dataBase ip or name to create the test database.
     * @throws IOException          If I/O error exists.
     * @throws InterruptedException If you not have permissions to run shell commands.
     */
    public static void createDataBase(String dataBase, String dataBasePath) throws IOException, InterruptedException {
        URL dataBaseUrl = MongoDataBaseTestUtils.class.getClassLoader().getResource(dataBasePath);
        if (dataBaseUrl != null) {
            (new ShellCommand()).executeCommand(String.format("mongorestore --db %1$s %2$s -h 127.0.0.1:27017 -u admin " +
                    "-p control123! --authenticationDatabase admin", dataBase, dataBaseUrl.getPath()), true);
        }

    }

    /**
     * Method used in test cases to drop the test database.
     *
     * @param dataBase the dataBase ip or name to drop the test database..
     * @throws IOException          If I/O error exists.
     * @throws InterruptedException If you not have permissions to run shell commands.
     */
    public static void dropDataBase(String dataBase) throws IOException, InterruptedException {
        (new ShellCommand()).executeCommand(String.format("echo 'db.runCommand( { dropDatabase: 1 } );' | " +
                        "mongo --host 127.0.0.1 %1$s -u admin -p control123! --quiet  --authenticationDatabase " +
                        "admin",
                dataBase), true);
    }

    /**
     * Modifies the current database profiler level used by the database profiling system to capture data about
     * performance. The method provides a wrapper around the database command profile.
     *
     * @param levelProfile Specifies a profiling level, which is either 0 for no profiling, 1 for only slow
     *                     operations, or 2 for all operations.
     * @throws IOException          If I/O error exists.
     * @throws InterruptedException If you not have permissions to run shell commands.
     */
    public static void setProfile(String dataBase, int levelProfile) throws IOException, InterruptedException {
        (new ShellCommand()).executeCommand(String.format("echo 'db.setProfilingLevel(%1$d);' | mongo --host " +
                        "127.0.0.1 %2$s -u admin -p control123! --quiet  --authenticationDatabase admin",
                levelProfile,
                dataBase),
                false);
    }

    /**
     * Method for delete profile collection
     * NOTE: The profile level should be set to 0 for delete it collection.
     *
     * @param dataBase dataBase to delete system.profile.
     * @throws IOException          If I/O error exists.
     * @throws InterruptedException If you not have permissions to run shell commands.
     */
    public static void deleteProfileCollection(String dataBase) throws IOException, InterruptedException {
        (new ShellCommand()).executeCommand(String.format("echo 'db.system.profile.drop()' | mongo --host " +
                        "127.0.0.1 %1$s -u admin -p control123! --quiet  --authenticationDatabase admin",
                dataBase),
                false);
    }

    /**
     * Create a function dummy to verify call function is correct.
     *
     * @param functionNameTest function name.
     * @param dataBase         database name.
     * @throws IOException          Exception if a parameter is null or invalid.
     * @throws InterruptedException exception if a security manager not found or not have permissions.
     */
    public static void createDummyFunction(String functionNameTest, String dataBase) throws IOException,
            InterruptedException {
        (new ShellCommand()).executeCommand(String.format("echo '" +
                        " db.system.js.save(\n" +
                        "    {\n" +
                        "        _id: %1$s,\n" +
                        "        value :" +
                        "function (options) {\n" +
                        "    var table = {};\n" +
                        "    table.options = JSON.stringify(options);\n" +
                        "    table.title = \"List of Things\";\n" +
                        "    var dbObject = db.things.find();\n" +
                        "    if ((options != null ) && (Object.keys(options).length > 0 ) && options.pageSize != -1) " +
                        "{\n" +
                        "        var skip = options.pageSize * (options.pageNumber - 1);\n" +
                        "        var limit = options.pageSize;\n" +
                        "        table.totalRows = dbObject.count();\n" +
                        "        var data = dbObject.sort({thingTypeName: 1}).skip(skip).limit(limit).map(\n" +
                        "            function (x) {\n" +
                        "                return [x.thingTypeName, x.serialNumber, x.name];\n" +
                        "            }\n" +
                        "        );\n" +
                        "        table.data = data;\n" +
                        "    } else {\n" +
                        "        var data = dbObject.sort({thingTypeName: 1}).map(\n" +
                        "            function (x) {\n" +
                        "                return [x.thingTypeName, x.serialNumber, x.name];\n" +
                        "            }\n" +
                        "        );\n" +
                        "        table.data = data;\n" +
                        "    }    \n" +
                        "    table.columnNames = [\"Type\", \"SerialNumber\", \"Name\"];\n" +
                        "    return table;\n" +
                        "}  " +
                        "})' | mongo --host " +
                        "127.0.0.1 %2$s -u admin -p control123! --quiet  --authenticationDatabase admin"
                , functionNameTest, dataBase),
                false);
    }

    public static void createUser(String userName, String password, String databaseName) throws IOException,
            InterruptedException {
        String createUserStr = String.format("db.createUser({user: \"%1$s\", pwd: \"%2$s\", roles: [\"readWrite\"]})" +
                "", userName, password);
        (new ShellCommand()).executeCommand(String.format("echo $'%1$s' | mongo --host " +
                        "127.0.0.1 %2$s -u admin -p control123! --quiet  --authenticationDatabase admin",
                createUserStr, databaseName),
                false);
    }

    public static void dropUser(String userName, String databaseName) throws IOException,
            InterruptedException {
        String createUserStr = String.format("db.dropUser(\"%1$s\")", userName);
        (new ShellCommand()).executeCommand(String.format("echo '%1$s' | mongo --host " +
                        "127.0.0.1 %2$s -u admin -p control123! --quiet  --authenticationDatabase admin",
                createUserStr, databaseName),
                false);
    }

    public static void prepareDataBase() throws IOException, InterruptedException {
        MongoDataBaseTestUtils.dropDataBase(mongoDataBase);
        MongoDataBaseTestUtils.createDataBase(mongoDataBase, mongoDataBasePath);
        if (TEMP_FOLDER_REPORT != null) {
            org.apache.commons.io.FileUtils.cleanDirectory(new File(TEMP_FOLDER_REPORT.getFile()));
        } else {
            fail("could not be clean the temp directory!");
        }
    }
}
