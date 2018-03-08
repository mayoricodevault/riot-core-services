package com.tierconnect.riot.api.mongoShell.query;

/**
 * Created by achambi on 10/21/16.
 * EnumOptions to ResultFormat.
 */
public enum ResultFormat {

    BSON(ResultFormat.BSON_FILE_TEMPLATE, ResultFormat.BSON_EXTENSION),
    JSON(ResultFormat.JSON_FILE_TEMPLATE, ResultFormat.JSON_EXTENSION),
    CSV(ResultFormat.CSV_FILE_TEMPLATE, ResultFormat.CSV_EXTENSION),
    CSV_SCRIPT(ResultFormat.CSV_SCRIPT_FILE_TEMPLATE, ResultFormat.CSV_EXTENSION);

    private final String resultTemplate;
    private final String resultExtension;

    public String getResultTemplate() {
        return resultTemplate;
    }
    public String getResultExtension() {
        return resultExtension;
    }


    ResultFormat(final String resultTemplate, String resultExtension) {
        this.resultTemplate = resultTemplate;
        this.resultExtension = resultExtension;
    }

    private static final String BSON_FILE_TEMPLATE = "BSONTemplate.js";
    private static final String BSON_EXTENSION = ".bson";

    private static final String JSON_FILE_TEMPLATE = "JSONTemplate.js";
    private static final String JSON_EXTENSION = ".json";

    private static final String CSV_FILE_TEMPLATE = "CSVTemplate.js";
    private static final String CSV_SCRIPT_FILE_TEMPLATE = "CSVCustomTemplate.js";
    private static final String CSV_EXTENSION = ".csv";
}