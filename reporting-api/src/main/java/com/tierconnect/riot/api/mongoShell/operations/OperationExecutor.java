package com.tierconnect.riot.api.mongoShell.operations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.api.assertions.Assertions;
import com.tierconnect.riot.api.mongoShell.MongoShellClientOption;
import com.tierconnect.riot.api.mongoShell.ResultQuery;
import com.tierconnect.riot.api.mongoShell.connections.BaseServerShellCluster;
import com.tierconnect.riot.api.mongoShell.parsers.MongoParser;
import com.tierconnect.riot.api.mongoShell.parsers.ResultParser;

import com.tierconnect.riot.api.mongoShell.utils.FileUtils;
import com.tierconnect.riot.api.mongoShell.utils.ResourceUtils;
import com.tierconnect.riot.api.mongoShell.utils.ShellCommand;


import org.apache.log4j.Logger;
import org.bson.Document;

import java.io.IOException;
import java.util.Map;

import static com.tierconnect.riot.api.assertions.Assertions.*;
import static com.tierconnect.riot.api.assertions.Assertions.isNotBlank;
import static com.tierconnect.riot.api.assertions.Assertions.isTrue;

/**
 * Created by achambi on 12/2/16.
 * class to connect with mongo and run queries in shell.
 */
public class OperationExecutor {

    private static Logger logger = Logger.getLogger(OperationExecutor.class);
    private static String FUNCTION_TEMPLATE = "FUNCTemplate.js";
    private final String connection;

    private final ShellCommand shellCommand;

    public String getConnection() {
        return connection;
    }

    /**
     * Constructor only valid for unit test.
     */
    OperationExecutor() {
        this.connection = "";
        this.shellCommand = new ShellCommand();
    }

    /**
     * Class constructor
     *
     * @param username           {@link String} the user Name to connect with mongo
     * @param password           {@link String} the password to connect with mongo.
     * @param authDataBase       {@link String} the database name associated with the user’s credentials.
     * @param options            {@link MongoShellClientOption} The mongo Options to
     * @param serverShellCluster {@link BaseServerShellCluster} The mongo server cluster.
     */
    public OperationExecutor(String username,
                             String password,
                             String authDataBase,
                             MongoShellClientOption options,
                             BaseServerShellCluster serverShellCluster) {
        String connectionUriShell = MongoURIBuilder.buildMongoURICommand(
                username,
                password,
                authDataBase,
                options,
                serverShellCluster);
        isTrue("connectionUriShell", isNotBlank(connectionUriShell));
        this.connection = connectionUriShell;
        this.shellCommand = new ShellCommand();
    }


    /**
     * Run a Mongo script in the shell terminal and return the result in a file.
     *
     * @param script         {@link String} The file path to run in shell console.
     * @param tempResultPath {@link String} The file path to get the result.
     * @return {@link ResultQuery} a object with contains the total rows and total results.
     * @throws IOException          The exception to contains the error message.
     * @throws InterruptedException The exception to contains the error message.
     */
    Document executeCommand(final String script, final String tempResultPath)
            throws IOException, InterruptedException {
        shellCommand.executeCommand(script);
        return MongoParser.parseDocument(FileUtils.readFile(tempResultPath));
    }

    /**
     * Run a Mongo script in the shell terminal and return the result in a file.
     *
     * @param script         {@link String} The file path to run in shell console.
     * @param tempResultPath {@link String} The file path to get the result.
     * @return {@link ResultQuery} a object with contains the total rows and total results.
     * @throws IOException          The exception to contains the error message.
     * @throws InterruptedException The exception to contains the error message.
     */
    public ResultQuery  executeQuery(final String script, final String tempResultPath, boolean enableExplain, boolean addQuery, boolean addCount)
            throws IOException, InterruptedException {
        shellCommand.executeCommand(script);
        return ResultParser.parseFile(tempResultPath, enableExplain, addQuery, addCount);
    }

    /**
     * Run a Mongo script in the shell terminal and return the result in a file.
     *
     * @param queryFilePath  {@link String} The file path to run in shell console.
     * @param tempResultPath {@link String} The file path to get the result.
     * @return {@link String} a path with contains the export result.
     * @throws IOException          The INPUT/OUTPUT exception to contains the error message.
     * @throws InterruptedException If the command to run in shell was interrupted.
     */
    public String exportFileQuery(final String queryFilePath, final String tempResultPath) throws IOException,
            InterruptedException {
        String result = shellCommand.executeCommand(queryFilePath);
        if (!result.isEmpty()) {
            logger.error("An error occurred while executing command.");
            throw new IOException(result);
        }
        return tempResultPath;
    }

    /**
     * Prepare Query File with script body.
     *
     * @param functionName   The function Name for run.
     * @param tempResultPath a {@link String} that contains the result file name for save.
     * @return A {@link Map}.
     * @throws IOException The Input/Output Exception.
     */
    Document executeFunction(final String functionName,
                             final Map<String, Object> parameters,
                             final String tempResultPath)
            throws IOException, InterruptedException {
        isTrue("script", isNotBlank(functionName));
        ObjectMapper objectMapper = new ObjectMapper();

        String queryString = ResourceUtils.readFile(FUNCTION_TEMPLATE);
        String ConnectionString = this.getConnection();
        queryString = queryString.replaceAll("\u003cmongoConnection\u003e", ConnectionString.replaceAll("\\$",
                "\\\\\\$"));
        queryString = queryString.replaceAll("\u003cfileResultPath\u003e", tempResultPath);
        queryString = queryString.replaceAll("\u003cfunctionName\u003e", functionName);
        queryString = queryString.replaceAll("\u003coptions\u003e", objectMapper.writeValueAsString(parameters));
        queryString = queryString.replaceAll("\u003cgrepFunction\u003e","| grep -v \"I NETWORK\"");
        queryString = queryString.replaceAll("\\$", "\\\\\\$");
        return this.executeCommand(queryString, tempResultPath);
    }
}
