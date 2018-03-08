package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.iot.entities.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

public class ValidatorService {
    public static final List<String> RESERVED_EXPRESSION = Arrays.asList(
            //prefix
            "groupTypeCode.", "groupTypeName.", "groupCode.", "groupName.", "thingTypeCode.",
            "thingTypeName.", "name.", "serialNumber.", "size.", "brand.",
            // suffix
            ".parent",
            // restrict
            "{group}",
            "${groupName}",
            "${groupId}",
            "${groupCode}",
            "${groupTypeName}",
            "${groupTypeCode}",
            "{thingType}",
            "{dwelltime}",
            "{timestamp}",
            "{parent}");
    public static final List<String> EPC_OPTIONS = Arrays.asList(
            "\"COMPANY_PREFIX\"", "\"SERIAL\"", "\"GS1KEY\"", "\"GS1KEY_NOCOMPANY_PREFIX\"", "\"GS1KEYSERIAL\"",
            "\"UPC_CHECKDIGIT\"", "\"SCHEMA\"", "\"EPC_TAG_URI\"", "\"EPC_PURE_IDENTITY_URI\"", "NULL", "\"\"");

    public static final List<String> CURRENT_USER_OPTIONS = Arrays.asList(
            "\"id\", \"username\", \"firstName\", \"lastName\", \"email\", \"userGroup\", \"userActiveGroup\"", "\"groupCode\"", "\"activeGroupCode\"");

    private static Logger logger = Logger.getLogger(ValidatorService.class);

    public static ValidationBean validateValuesIsNotEmpty(Map<String, Object> reportValues) {
        ValidationBean response = new ValidationBean();

        if (reportValues != null) {
            for (Object o : reportValues.entrySet()) {
                Map.Entry udfObject = (Map.Entry) o;
                //noinspection unchecked
                for (Map.Entry<String, Object> udfProperties : ((Map<String, Object>) udfObject.getValue())
                        .entrySet()) {
                    if (udfProperties.getKey().equals("value")) {
                        String value = (udfProperties.getValue() == null) ? "" : udfProperties.getValue().toString();
                        if (StringUtils.isEmpty(value)) {
                            response.setErrorDescription("A field value is blank or empty.");
                            return response;
                        }
                    }
                }
            }
        }
        return response;
    }

    public static ValidationBean validateExpressionFields(Map<String, Object> thingTypeMap) {
        ValidationBean response = new ValidationBean();

        List<Map<String, Object>> expressionsList = getExpressions(thingTypeMap);
        for (Map<String, Object> expression : expressionsList) {
            String expressionValue = expression.get("value").toString();
            if (Boolean.valueOf(expression.get("isExpression").toString())) {
                response = checkSyntaxExpression(expressionValue);
                if (response.isError()) {
                    return response;
                }
                response = discardObjects(expressionValue);
                if (response.isError()) {
                    return response;
                }
                response = checkParent(expression, thingTypeMap);
                if (response.isError()) {
                    return response;
                }
            }
        }

        return response;
    }

    public static ValidationBean isNullOrEmpty(Object value) {
        ValidationBean response = new ValidationBean();
        if (value == null) {
            response.setErrorDescription("The value is empty");
        } else if (value instanceof String && Objects.equals(value.toString().trim(), "")) {
            response.setErrorDescription("The value is NULL");
        }
        return response;
    }

    private static ValidationBean checkParent(Map<String, Object> expression, Map<String, Object> thingTypeMap) {
        ValidationBean response = new ValidationBean();
        String formula = expression.get("value").toString();
        if (formula.contains("parent.")) {
            //noinspection unchecked
            if (thingTypeMap.containsKey("parent.ids") && ((List<Long>) thingTypeMap.get("parent.ids")).size() > 0) {
                String[] formulas = formula.split("\\$");
                List<String> legalFormula = new ArrayList<>();
                for (String aFormula : formulas) {
                    if (aFormula.contains("parent.")) {
                        if (aFormula.startsWith("{parent.") && aFormula.endsWith("}")) {
                            legalFormula.add(aFormula.replace("{parent.", "").replace("}", ""));
                        } else {
                            if (aFormula.startsWith("{parent.")) {
                                Integer pos = aFormula.indexOf('}');
                                legalFormula.add(aFormula.substring(0, pos).replace("{parent.", "").replace("}", ""));
                            } else {
                                Integer pos = aFormula.indexOf('{');
                                legalFormula.add(aFormula.substring(pos, aFormula.length()).replace("{parent.", "").replace("}", ""));
                            }
                        }
                    }
                }
                logger.info("parents: " + legalFormula);
                @SuppressWarnings("unchecked")
                List<Integer> parentIdsList = (List<Integer>) thingTypeMap.get("parent.ids");
                for (Integer parentId : parentIdsList) {
                    ThingType parent = ThingTypeService.getInstance().get(Long.valueOf(parentId.toString()));
                    if (parent == null) {
                        logger.error("parent not found");
                        response.setErrorDescription("parent not found");
                        return response;
                    }
                    for (String property : legalFormula) {
                        Boolean answer = isPropertyOf(parent, property);
                        if (!answer) {
                            logger.error("Cannot find property " + property);
                            response.setErrorDescription("Cannot find property " + property);
                        }
                    }
                }
            } else {
                logger.error("ThingType has not parent assigned");
                response.setErrorDescription("ThingType has not parent assigned");
            }
        }
        return response;
    }

    private static Boolean isPropertyOf(Object object, String property) {
        if (property.isEmpty()) {
            return true;
        }
        Integer pos = property.indexOf('.');
        String newProperty = "";
        Object newObject = null;
        if (pos > 0) {
            newProperty = property.substring(pos + 1, property.length());
            property = property.substring(0, pos);
        } else {
            if (object instanceof ThingTypeField) {
                if (((ThingTypeField) object).getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_NATIVE_THING_TYPE.value)) {
                    newObject = ThingTypeService.getInstance().get(((ThingTypeField) object).getDataTypeThingTypeId());
                    return isPropertyOf(newObject, property);
                } else if (((ThingTypeField) object).getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value)) {
                    newObject = DataTypeService.getInstance().get(((ThingTypeField) object).getDataType().getId()).publicMap();
                    Long id = Long.valueOf(((Map) newObject).get("id").toString());
                    Map<String, Object> auxMap = new HashMap<>();
                    if (id.equals(ThingTypeField.Type.TYPE_GROUP.value)) {
                        auxMap = new Group().publicMap();
                        replaceNullsFromMap(auxMap, "");
                    } else if (id.equals(ThingTypeField.Type.TYPE_LOGICAL_READER.value)) {
                        auxMap = new LogicalReader().publicMap();
                        replaceNullsFromMap(auxMap, "");
                    } else if (id.equals(ThingTypeField.Type.TYPE_SHIFT.value)) {
                        auxMap = new Shift().publicMap();
                        replaceNullsFromMap(auxMap, "");
                    } else if (id.equals(ThingTypeField.Type.TYPE_ZONE.value)) {
                        auxMap = new Zone().publicMapSummarized();
                        auxMap.put("facilityMap", "");
                        auxMap.put("zoneGroup", "");
                        replaceNullsFromMap(auxMap, "");
                    }
                    return isPropertyOf(auxMap, property);
                }
            }
            for (ThingType.NonUDF nonUDF : ThingType.NonUDF.values()) {
                if (nonUDF.toString().equals(property)) {
                    return true;
                }
            }
        }
        if (object instanceof ThingType) {
            newObject = ((ThingType) object).getThingTypeField(property);
        }
        if (object instanceof Map) {
            newObject = ((Map) object).get(property);
        }
        if (object instanceof ThingTypeField) {
            newObject = ((ThingTypeField) object).publicMap();
            newObject = ((Map) newObject).get(property);
            if ((newObject == null) && (!newProperty.isEmpty())) {
                newObject = ((ThingTypeField) object).publicMap();
                newObject = ((Map) newObject).get(newProperty);
            }
        }
        if (newObject == null) {
            return false;
        }
        if (pos == -1) {
            return true;
        }

        return isPropertyOf(newObject, newProperty);
    }

    private static void replaceNullsFromMap(Map<String, Object> objectMap, Object replaceObject) {
        for (Map.Entry<String, Object> item : objectMap.entrySet()) {
            objectMap.put(item.getKey(), item.getValue() != null ? item.getValue() : replaceObject);
        }
    }

    /**
     * @param expression expression's value
     * @return true if a value contains a reserved expression
     */
    public static boolean isValueReservedExpression(String expression) {
        for (String restrict : RESERVED_EXPRESSION) {
            if (expression.contains(restrict)) {
                return true;
            }
        }
        return false;
    }

    private static ValidationBean discardObjects(String expression) {
        ValidationBean response = new ValidationBean();
        if (isValueReservedExpression(expression)) {
            logger.error("Illegal expression '" + expression + "'");
            response.setErrorDescription("Illegal expression '" + expression + "'");
        }
        return response;
    }

    public static ValidationBean checkSyntaxExpression(String expression) {
        ValidationBean response = new ValidationBean();
        expression = expression.replaceAll("\\s+", "");

        if (expression.contains("${}")) {
            response.setErrorDescription("Error with '${}', invalid expression");
        }
        if (expression.contains("${count()}")) {
            response.setErrorDescription("Error with '${count()}', invalid expression");
        }
        if (!expression.contains("${")) {
            response.setErrorDescription("Error with '${', it is required for Expression Data Type");
        }
        // validate that $ is before to {
        for (int i = 0; i < expression.length(); i++) {
            if (expression.charAt(i) == '{' && i == 0) {
                response.setErrorDescription("Error with '{', invalid expression");
                break;
            }
            if (expression.charAt(i) == '{' && i > 0 && expression.charAt(i - 1) != '$') {
                response.setErrorDescription("Error with '{', invalid expression");
                break;
            }
        }

        try {
            Map<Character, Integer> map = new HashMap<Character, Integer>();
            for (int i = 0; i < expression.length(); i++) {
                char c = expression.charAt(i);
                if ((c == '{') || (c == '}') ||
                        (c == '(') || (c == ')')) {
                    if (map.containsKey(c)) {
                        int cnt = map.get(c);
                        map.put(c, ++cnt);
                    } else {
                        map.put(c, 1);
                    }
                }
            }
            Integer left = (map.containsKey('{')) ? map.get('{') : 0;
            Integer right = (map.containsKey('}')) ? map.get('}') : 0;
            if (left - right != 0) {
                logger.error("Error with '{}' ");
                response.setErrorDescription("Error with '{}' ");
            }
            left = (map.containsKey('(')) ? map.get('(') : 0;
            right = (map.containsKey(')')) ? map.get(')') : 0;
            if (left - right != 0) {
                logger.error("Error with '()' ");
                response.setErrorDescription("Error with '()' ");
            }

            if (expression.contains("${epcDecode(")) {
                boolean status;
                String epc[] = expression.split(",");
                if (epc.length != 2) {
                    status = false;
                } else {
                    status = validateEPCDecoder(epc[1]);
                }
                if (!status) {
                    logger.error(expression + " invalid argument for epcDecode returnType. Valid options " + EPC_OPTIONS.toString());
                    response.setErrorDescription(expression + " invalid argument for epcDecode returnType. Valid options " + EPC_OPTIONS.toString());
                }
            }

            if (expression.contains("${currentUser(")) {
                if (!validateCurrentUserExp(expression)) {
                    logger.error(expression + " invalid argument for currentUser returnType. Valid options " + CURRENT_USER_OPTIONS.toString());
                    response.setErrorDescription(expression + " invalid argument for currentUser returnType. Valid options " + CURRENT_USER_OPTIONS.toString());
                }
            }
        } catch (PatternSyntaxException e) {
            response.setErrorDescription(e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    /**
     * @param expression
     * @return A string if the property expression's is invalid
     */
    public static String validateSimpleExpression(String expression) {
        Pattern pattern = Pattern.compile("\\$\\{(.+?)\\}");
        Matcher m = pattern.matcher(expression);
        m.find();
        return ThingType.allowedSimpleExpressions.contains(m.group(1)) ? null :
                "Error validating the expression: Cannot find property '" + m.group(1) + "'. Verify the property";

    }

    /**
     * @param expression   EL Expression to validate
     * @param functionsMap Functions Map that are allowed
     * @return A string if any function is invalid
     */
    private static String validateAllowedFunctions(String expression, Map<String, List<String>> functionsMap) {
        String allowedFunctions = String.join("|",
                functionsMap.keySet().stream().map(x -> "(" + x + ")").collect(Collectors.toList()));
        String pattern1 = "^\\$\\{(" + allowedFunctions + ")\\(\\\"[a-zA-Z0-9\\\\-]+\\\"\\)\\}$";
        if (expression.matches(pattern1)) {
            return functionsMap.entrySet().stream().
                    filter(p ->
                            expression.matches("^\\$\\{" + p.getKey() + "\\(\\\"[a-zA-Z0-9\\\\-]+\\\"\\)\\}$")
                    ).
                    collect(Collectors.toMap(p ->
                            p.getKey(), p -> validateAllowedParam(expression, p.getKey(), p.getValue())
                    )).
                    entrySet().stream().
                    filter(p -> (p.getValue() != "")).
                    findFirst().
                    map(p -> p.getValue()).
                    orElse(null);
        } else {
            return "Error with '" + expression + "' expression, invalid function name";
        }
    }

    /**
     * @param expression    EL Expression to validate
     * @param functionName  Functions name that is validate
     * @param allowedParams Function params that are allowed
     * @return A string with error message if it is wrong, else a empty string
     */
    private static String validateAllowedParam(String expression, String functionName, List<String> allowedParams) {
        String paramsExpression = String.join("|",
                allowedParams.stream().map(x -> "(" + x + ")").collect(Collectors.toList()));
        String pattern = "^\\$\\{(" + functionName + ")\\(\\\"(" + paramsExpression + ")+\\\"\\)\\}$";
        return expression.matches(pattern) ? "" : "Error with '" + expression + "' expression";
    }


    private static List<Map<String, Object>> getExpressions(Map<String, Object> thingTypeMap) {
        List<Map<String, Object>> response = new ArrayList<>();
        Map<String, Object> name = new HashMap<>();
        name.put("field", "name");
        name.put("value", thingTypeMap.get("name"));
        name.put("isExpression", false);
        Map<String, Object> serialNumber = new HashMap<>();
        serialNumber.put("field", "serialNumber");
        serialNumber.put("value", thingTypeMap.get("serialFormula"));
        serialNumber.put("isExpression", !thingTypeMap.get("serialFormula").toString().isEmpty());
        response.add(name);
        response.add(serialNumber);
        List<Map<String, Object>> fieldsList = new ArrayList<>();
        if (thingTypeMap.containsKey("fields")) {
            fieldsList = (List<Map<String, Object>>) thingTypeMap.get("fields");
        }
        for (Map<String, Object> field : fieldsList) {
            Map<String, Object> expression = new HashMap<>();
            expression.put("field", field.get("name"));
            expression.put("value", (field.get("defaultValue") == null) ? "" : field.get("defaultValue"));
            Long type = Long.valueOf(field.get("type").toString());
            expression.put("isExpression", type.equals(ThingTypeField.Type.TYPE_FORMULA.value));
            if (Long.valueOf(field.get("type").toString()).equals(ThingTypeField.Type.TYPE_THING_TYPE.value)) {
                Long dataTypeId = Long.valueOf(field.get("dataTypeThingTypeId").toString());
                ThingType thingType = ThingTypeService.getInstance().get(dataTypeId);
                if (thingType != null) {
                    expression.put("value", thingType);
                }
            }
            response.add(expression);
        }
        logger.info("Expressions: " + response);
        return response;
    }

    private static ValidationBean validateSyntaxFormula(Map<String, Object> expression, List<Map<String, Object>> expressionsList) {
        ValidationBean response = new ValidationBean();
        for (Map<String, Object> expression1 : expressionsList) {
            String formula = expression1.get("value").toString();
            if (formula.contains("${" + expression.get("field"))) {
                Integer pos = formula.indexOf("${" + expression.get("field"));

            }
        }
        return response;
    }

    public static boolean validateEPCDecoder(String secondArgument) {
        String values[] = secondArgument.split("\\)");
        if (EPC_OPTIONS.contains(values[0].toUpperCase())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Validate a current user expression
     *
     * @param expression String expression
     * @return Boolean value
     */
    public static boolean validateCurrentUserExp(String expression) {
        boolean resp = false;
        if (expression.equals("${currentUser(\"id\")}") ||
                expression.equals("${currentUser(\"username\")}") ||
                expression.equals("${currentUser(\"firstName\")}") ||
                expression.equals("${currentUser(\"lastName\")}") ||
                expression.equals("${currentUser(\"email\")}") ||
                expression.equals("${currentUser(\"userGroup\")}") ||
                expression.equals("${currentUser(\"userActiveGroup\")}") ||
                expression.equals("${currentUser(\"groupCode\")}") ||
                expression.equals("${currentUser(\"activeGroupCode\")}") ) {
            resp = true;
        }
        return resp;
    }
}
