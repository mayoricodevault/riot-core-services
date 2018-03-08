package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.mongo.Mongo;
import com.tierconnect.riot.api.mongoShell.query.ResultFormat;
import com.tierconnect.riot.api.mongoShell.utils.FileUtils;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.dao.mongo.MongoScriptDAO;
import com.tierconnect.riot.commons.utils.DateHelper;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.entities.ReportFilter;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.joda.time.format.ISODateTimeFormat;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by julio.rocha on 01-02-17.
 */
public class TableScriptExecution {
    private static final Logger logger = Logger.getLogger(TableScriptExecution.class);
    private ReportDefinition reportDefinition;
    private TableScriptConfig tableScriptConfig;
    private Map<String, Object> values;
    private Integer pageSize;
    private Integer pageNumber;
    private Date startDate;
    private Date endDate;
    private Date now;
    private String requestInfo;
    private String offsetDate;
    private String dateFormat;
    private boolean export = true;
    private boolean jsonFormatterRetry = true;
    private boolean reportExistsRetry = true;
    private final boolean isSentEmail;

    public TableScriptExecution(ReportDefinition reportDefinition,
                                Map<String, Object> values,
                                Integer pageSize,
                                Integer pageNumber,
                                Date startDate,
                                Date endDate,
                                Date now,
                                String requestInfo,
                                TableScriptConfig tableScriptConfig,
                                boolean isSentEmail) {
        this.reportDefinition = reportDefinition;
        this.values = values;
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
        this.startDate = startDate;
        this.endDate = endDate;
        this.now = now;
        this.requestInfo = requestInfo;
        this.tableScriptConfig = tableScriptConfig;
        this.isSentEmail = isSentEmail;

        User user = (User) SecurityUtils.getSubject().getPrincipal();
        this.offsetDate = UserService.getInstance().getValueRegionalSettings(user, Constants.TIME_ZONE_CONFIG);
        this.dateFormat = UserService.getInstance().getValueRegionalSettings(user, Constants.DATE_FORMAT_CONFIG);
    }

    public Map<String, Object> run() {
        try {
            export = false;//to conserve JSON format
            logger.info("RUNNING RAW MONGO REPORT NAME = " + reportDefinition.getName());
            File fileExport = export();
            try {
                Object response = MongoDAOUtil.fileToJSON(fileExport);
                if (response instanceof Map)
                    return (Map<String, Object>) MongoDAOUtil.fileToJSON(fileExport);
                else {
                    //sometimes the file contains important information about the fail
                    final String content = new String(Files.readAllBytes(Paths.get(fileExport.getAbsolutePath())), "UTF-8");
                    if (content.indexOf("JSONFormatter") != -1){
                        if (jsonFormatterRetry) {
                            FileUtils.deleteFile(fileExport);
                            jsonFormatterRetry = false;
                            return run();
                        } else {
                            MongoScriptDAO.getInstance().createFromResource("JSONFormatter", "mongo/JsonFormatter.js");
                            FileUtils.deleteFile(fileExport);
                            jsonFormatterRetry = true;
                            return run();
                        }
                    }
                    if (content.indexOf("functionObject is null") != -1){
                        if (reportExistsRetry) {
                            FileUtils.deleteFile(fileExport);
                            reportExistsRetry = false;
                            return run();
                        } else {
                            MongoScriptDAO.getInstance().insert(reportDefinition.getId().toString(), reportDefinition.getReportDefinitionConfig().get(0).getKeyValue());
                            FileUtils.deleteFile(fileExport);
                            reportExistsRetry = true;
                            return run();
                        }
                    }
                    throw new Exception(content);
                }
            } finally {
                FileUtils.deleteFile(fileExport);
            }
        } catch (Exception e) {
            logger.error("Error in execution of Table Script function", e);
            throw new UserException("'Report Script' cannot be executed. " + e.getMessage(), e);
        }
    }

    /**
     * Export Results of the report
     *
     * @return
     * @throws Exception
     */
    public File export() throws Exception {
        logger.info("RUNNING RAW MONGO REPORT NAME = " + reportDefinition.getName());
        StringBuilder script = new StringBuilder();
        script.append("db.getMongo().setReadPref(\"secondary\");\n");
        ReportJSFunction f = new ReportJSFunction(reportDefinition, isSentEmail);

        script.append("var loadMoment = load(\""+ReportExport.class.getClassLoader().getResource("libraries").getPath()+"/moment.min.js\");");
        script.append("var loadMoment = load(\""+ReportExport.class.getClassLoader().getResource("libraries").getPath()+"/moment-timezone.min.js\");");

        script.append(f.lstJSFunction.get(ReportJSFunction.FORMAT_DWELLTIME));
        script.append(f.lstJSFunction.get(ReportJSFunction.FORMAT_DATE));
        script.append(f.lstJSFunction.get(ReportJSFunction.FUNCTION_LOADER));
        script.append("var tableScriptFunction = loadFunction(\"vizixFunction" + reportDefinition.getId() + "\");\n");//load user function
        script.append(buildOptions());
        script.append("var JSONFormatter = loadFunction(\"JSONFormatter\");\n");//load formatter function
        script.append("JSONFormatter(tableScriptFunction(options));");//execution of user function
        Mongo m = new Mongo(new ConditionBuilder());
        String tmpExport = m.export(script.toString(),
                Constants.TEMP_NAME_REPORT + reportDefinition.getId() + "_", ResultFormat.CSV_SCRIPT);
        return new File(tmpExport);
    }

    private String buildOptions() {
        processFilters();
        JSONObject dataTimeSeries = new JSONObject();
        dataTimeSeries.put("startDate", DateHelper.dateToISODate(startDate));
        dataTimeSeries.put("endDate", DateHelper.dateToISODate(endDate));
        dataTimeSeries.put("now", DateHelper.dateToISODate(now));
        JSONObject options = new JSONObject();
        options.put("pageSize", pageSize);
        options.put("pageNumber", pageNumber);
        options.put("dataTimeSeries", dataTimeSeries);
        options.put("filters", values);
        options.put("filtersExtended", this.tableScriptConfig.filterMap);
        options.put("export", export);
        options.put("comment", requestInfo);
        options.put(Constants.TIME_ZONE_CONFIG, offsetDate );
        options.put(Constants.DATE_FORMAT_CONFIG, dateFormat );
        String optionsString =  "var options = " + options.toJSONString().replaceAll("\\\\", "")
                .replaceAll("\"--", "")
                .replaceAll("--\"", "") + ";";
        logger.info(" OPTIONS:  " + optionsString);
        return optionsString;
    }

    private void processFilters() {
        if (values == null || values.isEmpty()) {
            values = new LinkedHashMap<>();
            for (ReportFilter filter : reportDefinition.getReportFilterOrderByDisplayOrder()) {
                values.put(filter.getLabel(), filter.getValue());
            }
        }
    }

    /**
     * Javascript function minify
     *
     * @param original
     * @return
     */
    public static String minifyFunction(String original) {
        original = deleteLineComments(deleteBlockComments(original));
        return original.replaceAll("\n+", "")
                .replaceAll("\t+", "")
                .replaceAll(" +", "");
    }

    private static String deleteBlockComments(String original) {
        return deleteOpenClosePatterns("/*", "*/", original);
    }

    private static String deleteLineComments(String original) {
        return deleteOpenClosePatterns("//", "\n", original);
    }

    private static int nextClose(int from, String pattern, String target) {
        return (from > -1) ? target.indexOf(pattern, from) : -1;
    }

    private static String deleteOpenClosePatterns(final String openPattern,
                                                  final String closePattern, String target) {
        int openIndex = target.indexOf(openPattern);
        int closeIndex = nextClose(openIndex, closePattern, target);
        while (openIndex > -1 && closeIndex > -1) {
            String tmp = target.substring(0, openIndex);
            closeIndex += 1;
            if (closeIndex < target.length()) {
                tmp += target.substring(closeIndex + 1, target.length());
            }
            target = tmp;
            openIndex = target.indexOf(openPattern);
            closeIndex = nextClose(openIndex, closePattern, target);
        }
        return target;
    }

    public static boolean hasForbiddenSentences(String functionMinified, String keyword) {
        String sentencePattern = "\\w*db\\.((\\w+\\.)|(\\w*))" + keyword + "\\(\\{*(.)*\\}*\\)";
        Pattern p = Pattern.compile(sentencePattern);
        Matcher m = p.matcher(functionMinified);
        return m.find();
    }
}
