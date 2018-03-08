package com.tierconnect.riot.iot.reports.autoindex;

import com.tierconnect.riot.iot.reports.autoindex.entities.QueryField;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by julio.rocha on 26-04-17.
 */
class QueryAnalyzer {

    static final List<String> RANGE_OP = Collections.unmodifiableList(
            Arrays.asList("$lt", "$lte", "$gt", "$gte", "$in", "$nin", "$neq", "$ne",
                    "$exists", "$mod", "$all", "$regex", "$size")
    );


    String removeDots(String str) {
        return str.replaceFirst("^\\.", "").replaceAll("\\.$", "");
    }

    private Map<String, Set<QueryField>> fieldsForIndex = new HashMap<>();
    private List<QueryField> mergedFields;
    private JSONObject jsonFields;
    private JSONObject jsonProjection;
    private final String query;
    private final String sortField;
    private boolean alreadyExecuted;

    public QueryAnalyzer(String query, String sortField) {
        this.query = transformQueryFromMongoDriver(query);
        this.sortField = sortField;
        this.alreadyExecuted = false;
        fieldsForIndex.put("exactFields", new HashSet<>());
        fieldsForIndex.put("sortFields", new HashSet<>());
        fieldsForIndex.put("rangeFields", new HashSet<>());
    }

    public void execute() throws ParseException {
        extractAndClassificate();
        mergeFields();
        parseToJsonForMongoRequest();
    }

    void extractAndClassificate() throws ParseException {
        //START, replace sub-query if exists
        //Pattern p = Pattern.compile("\\{\\\"_id\\\"\\:\\{\\\"\\$in\\\"\\w*\\:\\w*db.thingSnapshotIds.find(\\{*(.)*\\}).*\\}\\}|,");
        Pattern p = Pattern.compile("\\{\\\"_id\\\"\\:\\{\\\"\\$in\\\"\\w*\\:\\w*db.thingSnapshotIds.find(\\{*(.)*\\}).*\\)\\}\\}");
        Matcher m = p.matcher(query);
        String queryToAnalize = m.replaceAll("{\"_id\":{\"\\$in\":[]}}");
        //END, replace sub-query if exists
        //START, Replace ISODate
        queryToAnalize = queryToAnalize.replaceAll("ISODate.*\\)", "\"\"");
        Object queryParsed = (new JSONParser()).parse(queryToAnalize);
        JSONObject queryObject = null;
        if (queryParsed instanceof JSONArray) {
            JSONArray aggregationQuery = (JSONArray) queryParsed;
            for (int i = 0; i < aggregationQuery.size(); i++) {
                JSONObject tmp = (JSONObject) aggregationQuery.get(i);
                if (tmp.get("$match") != null) {
                    queryObject = (JSONObject) tmp.get("$match");
                    break;
                }
            }
            if (queryObject == null) {
                throw new UserException("Invalid query, it will not be possible analyze");
            }
        } else {
            queryObject = (JSONObject) queryParsed;
        }
        //END, Replace ISODate
        analyzeQuery(queryObject, "", 0);
        if (!StringUtils.isEmpty(sortField)) {
            QueryField qfSort = new QueryField(sortField, QueryField.FieldType.SORT, "sort", 0);
            fieldsForIndex.get("sortFields").add(qfSort);
        }
    }

    void mergeFields() {
        mergedFields = Collections.synchronizedList(new LinkedList<>());

        for (QueryField qfr : fieldsForIndex.get("rangeFields")) {
            QueryField testQF = fieldsForIndex.get("exactFields").stream()
                    .filter(qf -> qf.getFieldName().equals(qfr.getFieldName()))
                    .findFirst()
                    .orElse(null);
            if (testQF != null) {
                int compareResult = qfr.compareTo(testQF);
                if (compareResult < 0) {
                    mergedFields.add(testQF);
                } else {
                    mergedFields.add(qfr);
                }
            } else {
                mergedFields.add(qfr);
            }
        }

        List<QueryField> complementaryExactFields = fieldsForIndex.get("exactFields").stream()
                .filter(qf -> mergedFields.stream().noneMatch(qfs -> qfs.getFieldName().equals(qf.getFieldName())))
                .collect(Collectors.toList());
        mergedFields.addAll(complementaryExactFields);

        //remove range fields with the same name that the sortFields
        for (QueryField qfr : fieldsForIndex.get("sortFields")) {
            QueryField elemToRemove = mergedFields.stream()
                    .filter(qf -> qf.getFieldName().equals(qfr.getFieldName()) && qf.compareTo(qfr) < 0)
                    .findFirst()
                    .orElse(null);
            if (elemToRemove != null) {
                mergedFields.remove(elemToRemove);
                mergedFields.add(qfr);
            }
        }

        //add field if it is not present
        QueryField sortFieldTest = fieldsForIndex.get("sortFields").stream()
                .filter(qf -> mergedFields.stream().noneMatch(qfs -> qfs.getFieldName().equals(qf.getFieldName())))
                .findFirst()
                .orElse(null);

        if (sortFieldTest != null) {
            mergedFields.add(sortFieldTest);
        }
    }

    private void parseToJsonForMongoRequest() {
        jsonFields = new JSONObject();
        jsonProjection = new JSONObject();
        for (QueryField qf : getMergedFields()) {
            JSONObject params = new JSONObject();
            params.put("cardinality", 0L);
            params.put("dataType", null);
            jsonFields.put(qf.getFieldName(), params);
        }
        parseToJsonForProjection();
    }

    private void parseToJsonForProjection() {
        //List<QueryField> uniqueFields = new LinkedList<>();
        List<String> orderedFields = mergedFields.stream()
                .map(qf -> qf.getFieldName())
                .sorted((s1, s2) -> (s1.split("\\.").length - s2.split("\\.").length))
                .collect(Collectors.toList());
        for (int i = 0; i < orderedFields.size(); i++) {
            String s1 = orderedFields.get(i);
            boolean flag = true;
            for (int j = 0; j < orderedFields.size(); j++) {
                String s2 = orderedFields.get(j);
                if (i != j && s1.startsWith(s2)) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                //uniqueFields.add(mergedFields.get(i));
                jsonProjection.put(s1, 1);
            }
        }
        //uniqueFields.stream().forEach(qf -> jsonProjection.put(qf.getFieldName(), 1));
    }

    private void analyzeQuery(JSONObject query, String root, Integer nestedLevel) {
        for (Object key : query.keySet()) {
            String keyStr = (String) key;
            Object value = query.get(key);
            if (keyStr.charAt(0) != '$') {
                if (value instanceof JSONObject) {
                    analyzeQuery((JSONObject) value, root + keyStr, nestedLevel + 1);
                } else {
                    QueryField qf = new QueryField(removeDots(root + keyStr), QueryField.FieldType.EQUALITY, "$eq", nestedLevel);
                    fieldsForIndex.get("exactFields").add(qf);
                }
            } else {
                if (RANGE_OP.contains(keyStr)) {
                    QueryField qf = new QueryField(removeDots(root), QueryField.FieldType.RANGE, keyStr, nestedLevel);
                    fieldsForIndex.get("rangeFields").add(qf);
                } else if ("$eq".equals(keyStr)) {
                    QueryField qf = new QueryField(removeDots(root), QueryField.FieldType.EQUALITY, "$eq", nestedLevel);
                    fieldsForIndex.get("exactFields").add(qf);
                } else if ("$not".equals(keyStr)) {
                    analyzeQuery((JSONObject) value, root, nestedLevel + 1);
                } else if ("$elemMatch".equals(keyStr)) {
                    analyzeQuery((JSONObject) value, root + ".", nestedLevel + 1);
                } else if ("$options".equals(keyStr) || "$hint".equals(keyStr) || "$explain".equals(keyStr) || "$text".equals(keyStr)) {
                    //ignore it
                } else if ("$and".equals(keyStr) || "$or".equals(keyStr)) {
                    //ignore it, there will be processed after
                } else {
                    System.err.println("Unreconized field query command : " + keyStr);
                }
            }
        }
        if (query.get("$and") != null) {
            JSONArray andQuery = (JSONArray) query.get("$and");
            for (int i = 0; i < andQuery.size(); i++) {
                analyzeQuery((JSONObject) andQuery.get(i), root, nestedLevel + 1);
            }
        }

        if (query.get("$or") != null) {
            JSONArray andQuery = (JSONArray) query.get("$or");
            for (int i = 0; i < andQuery.size(); i++) {
                analyzeQuery((JSONObject) andQuery.get(i), root, nestedLevel + 1);
            }
        }
    }

    /**
     * Method to sanitize the query to prepare to transform to json.
     *
     * @param query query int {@link String} format.
     * @return the same query sanitized.
     */
    private String transformQueryFromMongoDriver(String query) {
        return query.replaceAll("\"\\$numberLong\" :", "\"\\$eq\" :")
                .replaceAll("\n", "")
                .replaceAll("\\s+", "");
    }

    Map<String, Set<QueryField>> getFieldsForIndex() {
        return fieldsForIndex;
    }

    List<QueryField> getMergedFields() {
        return mergedFields;
    }

    public JSONObject getJsonFields() {
        return jsonFields;
    }

    public JSONObject getJsonProjection() {
        return jsonProjection;
    }
}
