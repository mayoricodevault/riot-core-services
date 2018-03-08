package com.tierconnect.riot.iot.reports.autoindex;

import com.tierconnect.riot.api.database.base.conditions.ConditionBuilder;
import com.tierconnect.riot.api.database.mongo.Mongo;
import com.tierconnect.riot.api.mongoShell.query.ResultFormat;
import com.tierconnect.riot.api.mongoShell.utils.FileUtils;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;

import com.tierconnect.riot.iot.reports.autoindex.entities.QueryField;
import com.tierconnect.riot.sdk.dao.UserException;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by julio.rocha on 28-04-17.
 */
class FieldStatistics {
    public static final String THINGS = "things";

    private final QueryAnalyzer queryAnalyzer;
    private final String collection;
    private final String reportName;


    public FieldStatistics(String query, String sortField, String collection, String reportName) throws ParseException {
        this.queryAnalyzer = new QueryAnalyzer(query, sortField);
        this.collection = collection;
        this.reportName = reportName;
        queryAnalyzer.execute();
    }

    public void computeStatistics() {
        try {
            Map<String, Map<String, Object>> statisticsResult = estimateCardinalityByField();
            assingResults(statisticsResult);
        } catch (Exception e) {
            throw new UserException(e);
        }
    }

    private Map<String, Map<String, Object>> estimateCardinalityByField() throws Exception {
        String query = "function hasField(obj, path){        \n" +
                "    var splitedPath = path.split('.');    \n" +
                "    var isArray = false;\n" +
                "    var fieldType = \"native\";\n" +
                "    var failResponse = {\"response\" : false, \"dataType\" : null};\n" +
                "    for(var i = 0; i < splitedPath.length && (typeof obj == 'object'); i++){        \n" +
                "        var subPath = splitedPath[i];                        \n" +
                "        if(subPath in obj){            \n" +
                "            if(obj[subPath] == null){\n" +
                "               return failResponse;\n" +
                "            }\n" +
                "            if(!(obj[subPath].constructor === Array)){\n" +
                "                obj = obj[subPath];\n" +
                "            } else {     \n" +
                "                isArray = true;\n" +
                "                obj = obj[subPath][0];\n" +
                "            }\n" +
                "        } else {\n" +
                "            return failResponse;\n" +
                "        }\n" +
                "    }\n" +
                "    \n" +
                "    if(i != splitedPath.length || obj === undefined) {\n" +
                "        return failResponse;\n" +
                "    }    \n" +
                "       \n" +
                "    if(isArray){\n" +
                "        if(\"[object BSON]\" == obj.toString()){\n" +
                "            fieldType = \"object_array\";\n" +
                "        } else {\n" +
                "            fieldType = \"native_array\";\n" +
                "        }        \n" +
                "    } else if(\"[object BSON]\" == obj.toString()){\n" +
                "        fieldType = \"object\";        \n" +
                "    } \n" +
                "    return {\"response\" : true, \"dataType\" : fieldType};\n" +
                "}\n";
        query += "function assignation(testResponse, fields, key){\n" +
                "    fields[key].cardinality++;\n" +
                "    fields[key].dataType = testResponse.dataType;\n" +
                "}\n";
        query += "function estimateSampleSize(N){\n" +
                "    var Z = 2.575; //99%\n" +
                "    var e = 0.01;  //1%\n" +
                "    var p = 0.5;   //50%\n" +
                "    var p1 = (1 - p);\n" +
                "    var Z2 = Z*Z;\n" +
                "    var e2 = e*e;\n" +
                "    if(N < 100000) {\n" +
                "        var n = (N*Z2*p*p1) / ((N-1)*e2+Z2*p*p1);\n" +
                "    } else {\n" +
                "        var n = Z2*p*p1 / e2;\n" +
                "    }\n" +
                "    return Math.floor(n);\n" +
                "}\n";
        query += //"var sampleRepeatTimes = " + FieldStatistics.TIMES_TO_REPEAT + ";\n" +
                "var size = db.getCollection('" + collection + "').find({}).count();\n" +
                "var sampleSize = estimateSampleSize(size);\n" +
                "var fivePercentSize = Math.floor((size*5)/100);\n" +
                "var fields = " + queryAnalyzer.getJsonFields().toJSONString().replaceAll(",", ",\n") + ";\n" +
                //"var x = null;\n" +
                //"var y = null;\n" +
                "\n" +
                //"for(var j = 0; j < sampleRepeatTimes; j++){\n" +
                "    var x = db.getCollection('" + collection + "').aggregate([ { $sample: { size: Math.min(sampleSize, fivePercentSize) } } , { $project : " + queryAnalyzer.getJsonProjection().toJSONString() + "}], { cursor: { batchSize: 0 }}, { allowDiskUse: true });\n" +
                "    while(x.hasNext()){\n" +
                "        var y = x.next();\n" +
                "        for(var key in fields){\n" +
                "            var testResponse = hasField(y, key);\n" +
                "            if(testResponse.response){                \n" +
                "                assignation(testResponse, fields, key);                \n" +
                "            }\n" +
                "        }\n" +
                "    }\n" +
                //"}\n" +
                "printjson(fields);";
        try{
            return executeScript(query);
        } catch(Exception e) { //racing could try to execute the same file, so retry with another name
            return executeScript(query);
        }
    }

    private Map<String, Map<String, Object>> executeScript(String query) throws Exception {
        Mongo m = new Mongo(new ConditionBuilder());
        String tmpExport = m.export(query,
                "Cardinality_" + reportName + "-" +Thread.currentThread().getName() + "-" +
                        UUID.nameUUIDFromBytes(UUID.randomUUID().toString().getBytes(Charset.forName("UTF-8"))), ResultFormat.CSV_SCRIPT);
        File f = new File(tmpExport);
        Object response = MongoDAOUtil.fileToJSON(f);
        Map<String, Map<String, Object>> statisticsResult = (Map<String, Map<String, Object>>) response;
        FileUtils.deleteFile(f);
        return statisticsResult;
    }

    private void assingResults(Map<String, Map<String, Object>> statisticsResult) {
        queryAnalyzer.getMergedFields().stream()
                .forEach(qf -> {
                    Object objCardinality = statisticsResult.get(qf.getFieldName()).get("cardinality");
                    Long cardinality = (objCardinality instanceof Long)? (Long)objCardinality : Long.parseLong(objCardinality.toString());
                    qf.setCardinality(qf.getCardinality() + cardinality);
                    qf.setDataType((String) statisticsResult.get(qf.getFieldName()).get("dataType"));
                });
    }

    List<QueryField> getResult() {
        return queryAnalyzer.getMergedFields();
    }
}
