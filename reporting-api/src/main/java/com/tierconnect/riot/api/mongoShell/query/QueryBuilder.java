package com.tierconnect.riot.api.mongoShell.query;

import com.tierconnect.riot.api.mongoShell.ResultQuery;
import com.tierconnect.riot.api.mongoShell.exception.IndexNotFoundException;
import com.tierconnect.riot.api.mongoShell.exception.MongoShellHandlerException;
import com.tierconnect.riot.api.mongoShell.operations.OperationExecutor;
import com.tierconnect.riot.api.mongoShell.utils.FileUtils;
import com.tierconnect.riot.api.mongoShell.utils.ResourceUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static com.tierconnect.riot.api.assertions.Assertions.*;
import static org.apache.commons.lang.StringUtils.EMPTY;

/**
 * Created by achambi on 12/1/16.
 * Class to build and execute mongo queries.
 */
public class QueryBuilder {

    private static final Logger logger = Logger.getLogger(QueryBuilder.class);

    private static final String FIND_TMP_FILE_QUERY_NAME = "findQuery";
    private static final String FIND_TMP_FILE_RES_NAME = "findResult";
    private static final String FIND_TEMPLATE = "find(%1$s%2$s%3$s)";
    private static final String FIND_WITH_HINT_TEMPLATE = ".hint(\"%s\")";
    private static final String FIND_COUNT_TEMPLATE = "db.%1$s.aggregate([{\"\\$match\" : %2$s}%3$s, { \"\\$group\": { _id: null, count: { \"\\$sum\": 1 } } }], { allowDiskUse: true })";
    private static final String FIND_COMMENT_TEMPLATE = ".comment(\"%1$s\")";
    private static final String FIND_EXPLAIN_TEMPLATE = "cursor%1$s.explain(\"allPlansExecution\")";
    private static final String FIND_ALIAS_TEMPLATE = "var alias = %1$s;";
    private static final String TMP_PATH = "/tmp";


    private String collection;
    private OperationExecutor operationExecutor;
    private Options options;
    private String tmpPath;


    /**
     * Default constructor to create a Query Builder.
     *
     * @param collection        the collection name to get documents.
     * @param operationExecutor the mongo connections object was created with mongo shell client.
     * @param options           the mongo report options usually: <b>skip, limit and sort</b>
     * @param tmpPath           the full path where it will be created all query files.
     */
    QueryBuilder(String collection, OperationExecutor operationExecutor, Options options, String tmpPath) {
        this.collection = collection;
        this.operationExecutor = operationExecutor;
        this.options = options;
        this.tmpPath = tmpPath;
    }

    /**
     * another constructor to create {@link QueryBuilder} instance  whit default options sort = null, skip = 0 and
     * limit = 0
     *
     * @param collection        the collection name to get documents.
     * @param operationExecutor the mongo connections object was created with mongo shell client.
     * @param tmpPath           the full path where it will be created all query files.
     */
    QueryBuilder(String collection, OperationExecutor operationExecutor, String tmpPath) {
        this(collection, operationExecutor, new Options(), tmpPath);
    }

    /**
     * another constructor to create {@link QueryBuilder} instance  whit default collection EMPTY, options [sort = null,
     * skip = 0,
     * limit = 0], path to save the temporal files is /tmp and delete the export files.
     *
     * @param operationExecutor the mongo connections used to run the query.
     */
    public QueryBuilder(OperationExecutor operationExecutor) {
        this(EMPTY, operationExecutor, new Options(), TMP_PATH);
    }

    /**
     * another constructor to create {@link QueryBuilder} instance  whit default options [sort = null,
     * skip = 0, limit = 0], path to save the temporal files is /tmp and delete the export files.
     *
     * @param collection        the collection name to get documents.
     * @param operationExecutor the mongo connections object was created with mongo shell client.
     */
    public QueryBuilder(String collection, OperationExecutor operationExecutor) {
        this(collection, operationExecutor, new Options(), TMP_PATH);
    }


    /**
     * Set the options to use in queries
     *
     * @param options options [sort = null,skip = 0, limit = 0]
     */
    public void setOptions(Options options) {
        this.options = options;
    }

    /**
     * method to set the collection to get documents.
     *
     * @param collection the collective name for get data.
     */
    public void setCollection(final String collection) {
        voidNotNull("collectionName", collection);
        isTrueArgument("collectionName", "not blank", isNotBlank(collection));
        this.collection = collection;
    }

    /**
     * method to skip registries.
     *
     * @param skip the number of registries to skip.
     */
    public void skip(int skip) {
        isTrue("skip", skip >= 0);
        options.skip(skip);
    }

    /**
     * method to set the number of registries to return.
     * NOTE: this method not work for export.
     *
     * @param limit Field for limit the result rows.
     */
    public void limit(int limit) {
        isTrue("limit", limit >= 0);
        options.limit(limit);
    }

    /**
     * Method for set the sort order.
     *
     * @param sort a {@link String} sort the result by string in json format
     */
    public void sort(final String sort) {
        isTrue("sort", isNotBlank(sort));
        this.options.sort(sort);
    }


    /**
     * Get the temporal result path
     *
     * @param defaultResultName A {@link String} containing the default temporary name.
     * @param tmpFileResultName A {@link String} containing the temporary name.
     * @param extension         A {@link String} containing the file extension.
     * @return A {@link String} containing the full path of the temporal file.
     */
    public String getTmpFileResultPath(String defaultResultName, String tmpFileResultName, String extension) {
        return isNotBlank(tmpFileResultName) ?
                Paths.get(this.tmpPath, tmpFileResultName + System.nanoTime() + extension).toString() :
                Paths.get(this.tmpPath, defaultResultName + System.nanoTime() + extension).toString();
    }

    /**
     * @param defaultTmpFileName A {@link String} containing the default temporary name.
     * @param tmpFileQueryName   A {@link String} containing the temporary name.
     * @return A {@link String} containing the full name of the temporal file.
     */
    public String getTmpFileQueryName(String defaultTmpFileName, String tmpFileQueryName) {
        return isNotBlank(tmpFileQueryName) ? defaultTmpFileName + tmpFileQueryName : defaultTmpFileName;
    }

    /**
     * Method for get a {@link ResultQuery} using find operator.
     *
     * @param filter        String containing filter for run in shell.
     * @param projection    String for project the fields.
     * @param cursorComment String containing the message to set in cursor comment.
     * @param tmpFileName   The temporal result name.
     * @return A {@link ResultQuery} instance.
     * @throws IOException          The Input/Output Exception.
     * @throws InterruptedException Throws Interrupt exception if a security manager exists.
     */
    public ResultQuery find(final String filter, final String projection, final String cursorComment, final String
            tmpFileName) throws IOException, InterruptedException, IndexNotFoundException {
        return find(filter, projection, cursorComment, tmpFileName, false);
    }

    /**
     * Method for get a {@link ResultQuery} using find operator.
     *
     * @param filter        String containing filter for run in shell.
     * @param projection    String for project the fields.
     * @param cursorComment String containing the message to set in cursor comment.
     * @param tmpFileName   The temporal result name.
     * @param enableExplain flag for enable or disable explain query.
     * @return A {@link ResultQuery} instance.
     */
    public ResultQuery find(final String filter, final String projection, final String cursorComment, final String
            tmpFileName, boolean enableExplain) throws IOException, InterruptedException, IndexNotFoundException {
        return find(filter, projection, cursorComment, tmpFileName, enableExplain, null, true, true);
    }

    /**
     * Method for get a {@link ResultQuery} using find operator.
     *
     * @param filter        String containing filter for run in shell.
     * @param projection    String for project the fields.
     * @param cursorComment String containing the message to set in cursor comment.
     * @param tmpFileName   The temporal result name.
     * @param enableExplain flag for enable or disable explain query.
     * @return A {@link ResultQuery} instance.
     */
    public ResultQuery find(final String filter, final String projection, final String cursorComment, final String
            tmpFileName, boolean enableExplain, String withIndex, boolean addQuery, boolean addCount) throws IOException, InterruptedException, IndexNotFoundException {

        isTrue("jsonFilter", isNotBlank(filter));
        isTrue("projection", isNotBlank(projection));
        ResultFormat bsonResultFormat = ResultFormat.BSON;
        String tmpResultFindPath = getTmpFileResultPath(QueryBuilder.FIND_TMP_FILE_RES_NAME,
                tmpFileName,
                bsonResultFormat.getResultExtension());
        String comment = (isNotBlank(cursorComment)) ? String.format(FIND_COMMENT_TEMPLATE, cursorComment) : EMPTY;
        String operationDefinition = String.format(FIND_TEMPLATE, filter, ",", projection);
        if(!StringUtils.isEmpty(withIndex)){
            logger.info("Using hint with index : " + withIndex);
            operationDefinition +=  String.format(FIND_WITH_HINT_TEMPLATE, withIndex);
        }
        String sortField = "";
        if(options != null && !StringUtils.isEmpty(options.getSort())){
            sortField = ", {\"\\$sort\" : " + options.getSort() + "}";
        }
        File fileQuery = createQueryFile(operationDefinition,
                EMPTY,
                String.format(QueryBuilder.FIND_COUNT_TEMPLATE, this.collection, filter.replaceAll("\\$",
                        "\\\\\\$"), sortField),
                getTmpFileQueryName(QueryBuilder.FIND_TMP_FILE_QUERY_NAME, tmpFileName),
                tmpResultFindPath,
                comment,
                enableExplain,
                FIND_EXPLAIN_TEMPLATE,
                bsonResultFormat.getResultTemplate(), addQuery, addCount);
        try {
            ResultQuery resultQuery = operationExecutor.executeQuery(fileQuery.getPath(), tmpResultFindPath,
                    enableExplain, addQuery, addCount);
            if (resultQuery.getTotal() == -2 && resultQuery.getErrorMessage() != null) {
                MongoShellHandlerException.handleException(resultQuery.getErrorMessage(), withIndex);
            }
            return resultQuery;
        } finally {
            FileUtils.deleteFile(fileQuery);
            FileUtils.deleteFile(new File(tmpResultFindPath));
        }
    }

    /**
     * Method for Export a Query in determinate format.
     *
     * @param filter       String Contains filter for run in shell.
     * @param projection   String for project the fields.
     * @param alias        String for change all names and format for fields.
     * @param tmpFileName  The temporal result name.
     * @param resultFormat The format result [CSV, JSON, BSON]
     * @return A {@link String} containing the path
     * @throws IOException          The Input/Output Exception.
     * @throws InterruptedException Throws Interrupt exception if a security manager exists.
     */
    public String export(final String filter,
                         final String projection,
                         final String alias,
                         final String tmpFileName,
                         final String cursorComment,
                         final ResultFormat resultFormat
    ) throws IOException, InterruptedException {
        isTrue("jsonFilter", isNotBlank(filter));
        isTrue("projection", isNotBlank(projection));
        isTrue("alias", isNotBlank(alias));
        String tmpResultFindPath = getTmpFileResultPath(QueryBuilder.FIND_TMP_FILE_RES_NAME, tmpFileName,
                resultFormat.getResultExtension());
        String comment = EMPTY;
        if (isNotBlank(cursorComment)) {
            comment = String.format(FIND_COMMENT_TEMPLATE, cursorComment);
        }
        String sortField = "";
        if(options != null && !StringUtils.isEmpty(options.getSort())){
            sortField = ", {\"\\$sort\" : " + options.getSort() + "}";
        }
        File fileQuery = createQueryFile(String.format(FIND_TEMPLATE, filter, ",", projection), String.format
                        (FIND_ALIAS_TEMPLATE, alias),
                String.format(QueryBuilder.FIND_COUNT_TEMPLATE, this.collection, filter.replaceAll("\\$", "\\\\\\$"), sortField),
                getTmpFileQueryName(QueryBuilder.FIND_TMP_FILE_QUERY_NAME, tmpFileName), tmpResultFindPath, comment,
                resultFormat.getResultTemplate());
        return executeFile(fileQuery, tmpResultFindPath);
    }

    /**
     * Method to Export a Query in determinate format.
     *
     * @param script       String containing the script for run.
     * @param tmpFileName  The Temporal name for the file result.
     * @param resultFormat The format result [CSV, JSON, BSON]
     * @return A {@link String} containing the path
     * @throws IOException          The Input/Output Exception.
     * @throws InterruptedException Throws Interrupt exception if a security manager exists.
     */
    public String export(String script,
                         final String tmpFileName,
                         final ResultFormat resultFormat) throws IOException, InterruptedException {
        String tmpQueryFindName = getTmpFileQueryName(QueryBuilder.FIND_TMP_FILE_QUERY_NAME, tmpFileName);
        String tmpResultFindPath = getTmpFileResultPath(QueryBuilder.FIND_TMP_FILE_RES_NAME, tmpFileName,
                resultFormat.getResultExtension());
        File fileQuery = createQueryFile(script, tmpQueryFindName, tmpResultFindPath, resultFormat.getResultTemplate());
        return executeFile(fileQuery, tmpResultFindPath);
    }

    /**
     * Method for execute a temporary file in mongo shell.
     *
     * @param fileQuery A file for run.
     * @return A {@link String} with contain the path.
     * @throws IOException          The Input/Output Exception.
     * @throws InterruptedException Throws Interrupt exception if a security manager exists.
     */
    private String executeFile(File fileQuery, String tmpFileResultPath) throws IOException,
            InterruptedException {
        try {
            return operationExecutor.exportFileQuery(fileQuery.getPath(), tmpFileResultPath);
        } finally {
            FileUtils.deleteFile(fileQuery);
        }
    }


    /**
     * Prepare Query File with parameters.
     *
     * @param operationDefinition   a {@link String} containing all filters to get all documents that
     *                              matched and optional projection.
     *                              {field1: [1|-1], field2: [1|-1],.....fieldN: [1|-1]}
     * @param alias                 a {@link String} in json format containing all alias fields:
     *                              {field1: [1|-1], field2: [1|-1],.....fieldN: [1|-1]}
     *                              {"field1": {"alias": "fieldAlias1", "function": [formatDate| formatDwellTime |
     *                              none],
     *                              "field2": {"alias": "fieldAlias2", "function": [formatDate| formatDwellTime | none],
     *                              "field3": {"alias": "fieldAlias3", "function": [formatDate| formatDwellTime | none]
     *                              .
     *                              .
     *                              .
     *                              "fieldN": {"alias": "fieldAliasN", "function": [formatDate| formatDwellTime | none]}
     * @param countTemplate         a {@link String} containing the count total method to run in mongo.
     * @param tmpFileQueryName      a {@link String} containing the query file name
     * @param tmpFileResultName     a {@link String} containing the Result file name.
     * @param cursorCommentTemplate a {@link String} containing the comment to set to the find cursor.
     * @param resultTemplate        a {@link String} containing the format result: [BSON, JSON, CSV]}
     * @return a {@link File} containing the script to run in shell.
     * @throws IOException Input output Exception with a explicit message.
     */
    private File createQueryFile(
            final String operationDefinition,
            final String alias,
            final String countTemplate,
            final String tmpFileQueryName,
            final String tmpFileResultName,
            final String cursorCommentTemplate,
            final String resultTemplate)
            throws IOException {
        return createQueryFile(operationDefinition,
                alias,
                countTemplate,
                tmpFileQueryName,
                tmpFileResultName,
                cursorCommentTemplate,
                false,
                EMPTY,
                resultTemplate, true, true);
    }


    /**
     * Prepare Query File with parameters.
     *
     * @param operationDefinition   a {@link String} containing all filters to get all documents that
     *                              matched and optional projection.
     *                              {field1: [1|-1], field2: [1|-1],.....fieldN: [1|-1]}
     * @param alias                 a {@link String} in json format containing all alias fields:
     *                              {field1: [1|-1], field2: [1|-1],.....fieldN: [1|-1]}
     *                              {"field1": {"alias": "fieldAlias1", "function": [formatDate| formatDwellTime |
     *                              none],
     *                              "field2": {"alias": "fieldAlias2", "function": [formatDate| formatDwellTime | none],
     *                              "field3": {"alias": "fieldAlias3", "function": [formatDate| formatDwellTime | none]
     *                              .
     *                              .
     *                              .
     *                              "fieldN": {"alias": "fieldAliasN", "function": [formatDate| formatDwellTime | none]}
     * @param countTemplate         a {@link String} containing the count total method to run in mongo.
     * @param tmpFileQueryName      a {@link String} containing the query file name
     * @param tmpFileResultName     a {@link String} containing the Result file name.
     * @param cursorCommentTemplate a {@link String} containing the comment to set to the find cursor.
     * @param enableExplain         a boolean for enable or disable explain information.
     * @param explainTemplate       a {@link String} the explain template definition.
     * @param resultTemplate        a {@link String} containing the following result format: [BSON, JSON, CSV]}
     * @return a {@link File} containing the script to run in shell.
     * @throws IOException Input output Exception with a explicit message.
     * @paramenableExplain A {@link String}
     */
     public File createQueryFile(
            final String operationDefinition,
            final String alias,
            final String countTemplate,
            final String tmpFileQueryName,
            final String tmpFileResultName,
            final String cursorCommentTemplate,
            final boolean enableExplain,
            final String explainTemplate,
            final String resultTemplate,
            final boolean addQuery,
            final boolean addCount)
            throws IOException {

        File queryFile = File.createTempFile(tmpFileQueryName, ".sh", new File(this.tmpPath));
        isTrue("Set executable file", queryFile.setExecutable(true));

        String queryString = ResourceUtils.readFile(resultTemplate);
        queryString = queryString.replaceAll("\u003cfileResultName\u003e", tmpFileResultName);

        //String format = String.format(FIND_TEMPLATE, filter, ",", projection);
        queryString = queryString.replaceAll("\u003ccollectionName\u003e", this.collection);
        queryString = queryString.replaceAll("\u003coperationDefinition\u003e", operationDefinition.replaceAll("\\$",
                "\\\\\\$"));
        queryString = queryString.replaceAll("\u003ctotalOperation\u003e", countTemplate);
        queryString = queryString.replaceAll("\u003calias\u003e", alias);
        queryString = queryString.replaceAll("\u003cqueryOptions\u003e", options.toString());
        queryString = queryString.replaceAll("\u003ccursorComment\u003e", cursorCommentTemplate);
        queryString = queryString.replaceAll("\u003cenableExplain\u003e", String.valueOf(enableExplain));
        queryString = queryString.replaceAll("\u003caddQuery\u003e", String.valueOf(addQuery));
        queryString = queryString.replaceAll("\u003caddCount\u003e", String.valueOf(addCount));
        String explainQuery = String.format(explainTemplate, options.toString());
        queryString = queryString.replaceAll("\u003cexplainDefinition\u003e", explainQuery.replaceAll("\\$",
                "\\\\\\$"));
        queryString = queryString.replaceAll("\\$", "\\\\\\$");
        queryString = queryString.replaceAll("\u003cmongoConnection\u003e", operationExecutor.getConnection());
        queryString = queryString.replaceAll("\u003cgrepFunction\u003e","| grep -v \"I NETWORK\"");
        FileUtils.writeFile(queryFile, queryString);
        return queryFile;
    }

    /**
     * Prepare Query File with script body.
     *
     * @param script            The script to prepare a file for run.
     * @param tmpFileQueryName  a {@link String} containing the query file name for run.
     * @param tmpFileResultName a {@link String} containing the result file name for save.
     * @param resultTemplate    a {@link String} containing the format result: [BSON, JSON, CSV]}
     * @return The {@link File} for run in mongo shell
     * @throws IOException The Input/Output Exception.
     */
    private File createQueryFile(String script,
                                 final String tmpFileQueryName,
                                 final String tmpFileResultName,
                                 final String resultTemplate)
            throws IOException {
        isTrue("script", isNotBlank(script));
        script = script.replaceAll("\\$", "\\\\\\$");
        File queryFile = File.createTempFile(tmpFileQueryName, ".sh", new File(this.tmpPath));
        isTrue("Set executable file", queryFile.setExecutable(true));
        String queryString = ResourceUtils.readFile(resultTemplate);
        queryString = queryString.replaceAll("\u003cfileResultName\u003e", tmpFileResultName);
        queryString = queryString.replaceAll("\u003ctableScript\u003e", script);
        queryString = queryString.replaceAll("\\$", "\\\\\\$");
        queryString = queryString.replaceAll("\u003cmongoConnection\u003e", operationExecutor.getConnection());
        queryString = queryString.replaceAll("\u003cgrepFunction\u003e","| grep -v \"I NETWORK\"");
        FileUtils.writeFile(queryFile, queryString);
        return queryFile;
    }

}
