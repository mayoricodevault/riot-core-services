package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.entities.ReportDefinitionUtils;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Class for create mongo scripts.
 *
 * @author : rchirinos
 * @date : 1/6/17 11:46 AM
 */
public class ReportJSFunction {
    private static Logger logger = Logger.getLogger(ReportJSFunction.class);
    public Map<String, String> lstJSFunction = new HashMap<>();
    public static final String FORMAT_VALUE = "getFormatValue";
    public static final String FORMAT_DWELLTIME = "formatDwellTime";
    public static final String FORMAT_DATE = "formatDate";
    public static final String VALUE_BY_PATH = "getValueByPath";
    public static final String FUNCTION_LOADER = "loadFunction";
    /*A backslash for script running in mongo shell*/
    public static final String BACKSLASH = "\\\\\\\\";


    public ReportJSFunction(final ReportDefinition reportDefinition, final boolean isSentEmail) {
        DateFormatAndTimeZone dateFormatAndTimeZone;
        if (isSentEmail) {
            Group reportGroup = reportDefinition.getGroup();
            Group group = reportGroup.getParentLevel3() != null ?
                    reportGroup.getParentLevel3() : reportGroup.getParentLevel2();
            dateFormatAndTimeZone = GroupService.getInstance().getDateFormatAndTimeZone(group);
            logger.info("EMAIL RUN AS GROUP [" + group.getName() + "]");
        } else {
            User user = (User) SecurityUtils.getSubject().getPrincipal();
            dateFormatAndTimeZone = UserService.getInstance().getDateFormatAndTimeZone(user);
            logger.info("EXPORT WITH USER [" + user.getUsername() + "]");
        }

        logger.info("REGIONAL SETTING  timezone :" + dateFormatAndTimeZone.getTimeZone());
        logger.info("REGIONAL SETTING  Date Format:" + dateFormatAndTimeZone.getMomentDateFormat());

        lstJSFunction.put(FORMAT_VALUE, getFormatValueJS().toString());
        lstJSFunction.put(FORMAT_DWELLTIME, formatDwellTimeJS().toString());
        lstJSFunction.put(FORMAT_DATE, formatDateJS(dateFormatAndTimeZone.getTimeZone(), dateFormatAndTimeZone.getMomentDateFormat()).toString());
        lstJSFunction.put(VALUE_BY_PATH, getValueByPath().toString());
        lstJSFunction.put(FUNCTION_LOADER, getFunctionLoader());
    }

    /**
     * Get correct function for each property
     *
     * @param property
     * @return
     */
    public static String getJSFunction(PropertyReport property) {
        String function = null;
        if (property.getThingTypeField() != null) {
            if (property.getThingTypeField().getDataType().getId().equals(ThingTypeField.Type.TYPE_DATE.value) ||
                    property.getThingTypeField().getDataType().getId().equals(ThingTypeField.Type.TYPE_TIMESTAMP.value)) {
                function = "formatDate";
            }
        }
        if (StringUtils.isEmpty(function)) {
            function = getJSFunctionByPropertyName(property.getPropertyOriginal());
        }
        return function;
    }

    private static String getJSFunctionByPropertyName(String originalName) {
        String function = null;
        if (ReportDefinitionUtils.isDwell(originalName)) {
            function = "formatDwellTime";
        } else if (ReportDefinitionUtils.isTimestamp(originalName)) {
            function = "formatDate";
        }
        return function;
    }

    /**
     * JS function to apply a specific format
     *
     * @return
     */
    private static StringBuffer getFormatValueJS() {
        StringBuffer buffer = new StringBuffer("");
        buffer.append("function formatValue(key, value){\n");
        buffer.append("    if(value ==='')\n");
        buffer.append("        return value;\n");
        buffer.append("    if(value == 'undefined' || value == null)\n");
        buffer.append("        return value;\n");
        buffer.append("    switch (key) {\n");
        buffer.append("        case \"formatDate\":\n");
        buffer.append("            return formatDate(value); break;\n");
        buffer.append("        case \"formatISODate\":\n");
        buffer.append("            return formatISODate(value); break;\n");
        buffer.append("        case \"formatTimestamp\":\n");
        buffer.append("            return formatTimestamp(value); break;\n");
        buffer.append("        case \"formatDwellTime\":\n");
        buffer.append("            return formatDwellTime(value); break;\n");
        buffer.append("        case \"none\":\n");
        buffer.append("            return value.valueOf()+\"\"; break;\n");
        buffer.append("    }\n");
        buffer.append("}\n");
        return buffer;
    }

    /**
     * JS function to format a dwell time
     *
     * @return
     */
    private static StringBuffer formatDwellTimeJS() {
        StringBuffer buffer = new StringBuffer("");
        buffer.append("function formatDwellTime(value) {\n" +
//                "    var sign = (value < 0) ? \"-\" : \"\";\n" +
//                "    value = (value < 0) ? value * -1 : value;\n" +
//                "    var x = Math.floor(value / 1000);\n" +
//                "    var seconds = Math.floor(x % 60);\n" +
//                "\n" +
//                "    x = (x / 60);\n" +
//                "    var minutes = Math.floor(x % 60);\n" +
//                "\n" +
//                "    x = (x / 60);\n" +
//                "    var hours = Math.floor(x % 24);\n" +
//                "    x = (x / 24);\n" +
//                "    var days = Math.floor(x);\n" +
//                "    var hoursS = (hours < 10 ? \"0\" + hours + \":\" : hours + \":\");\n" +
//                "    var minutesS = (minutes < 10 ? \"0\" + minutes + \":\" : minutes + \":\");\n" +
//                "    var secondsS = seconds < 10 ? \"0\" + seconds : seconds + \"\";\n" +
//                "    return sign + days + \" Days \" + hoursS + minutesS + secondsS;\n" +
                "    return value;\n" +
                "}\n");
        return buffer;
    }

    /**
     * JS function to format a Date
     *
     * @return
     */
    private static StringBuffer formatDateJS(String offsetTimeZone, String dateFormat) {
        StringBuffer buffer = new StringBuffer("");
        //Format Date
        buffer.append("");
        buffer.append(" function formatDate(value) {    \n");
        buffer.append("    var applyOffset = null ;\n");
        buffer.append("    var dateValue = value;    \n");
        buffer.append("    if(!(value instanceof Date)) {        \n");
        buffer.append("        dateValue = new Date(Number(value));        \n");
        buffer.append("    } \n");
//        buffer.append("    applyOffset  = moment.tz(new Date(), '").append(offsetTimeZone).append("').utcOffset();\n");
//        buffer.append("    return moment(dateValue.getTime()).utcOffset(applyOffset).format('").append(dateFormat).append("');\n");
        buffer.append("    return moment(dateValue.getTime()).tz('").append(offsetTimeZone).append("').format('").append(dateFormat).append("');\n");
        buffer.append(" } \n");

        //function to format time
        buffer.append("function formatTime(date) {    \n");
        buffer.append("    hours = date.getHours();\n");
        buffer.append("    minutes = date.getMinutes();\n");
        buffer.append("    seconds = date.getSeconds();\n");
        buffer.append("    ampm = hours >= 12 ? 'PM' : 'AM';\n");
        buffer.append("    hours = hours % 12;\n");
        buffer.append("    hours = hours ? hours : 12;\n");//hour 0 should be 12
        buffer.append("    hours = hours < 10 ? '0'+hours : hours;\n");
        buffer.append("    minutes = minutes < 10 ? '0'+minutes : minutes;\n");
        buffer.append("    seconds = seconds < 10 ? '0'+seconds : seconds;\n");
        buffer.append("    return hours + ':' + minutes + ':' + seconds + ' ' + ampm;\n");
        buffer.append("}    \n\n");

        // function to ISODate
        buffer.append("function formatISODate(value) {\n");
        buffer.append("    var dateValue = value;\n");
        buffer.append("    if(!(value instanceof Date)) {\n");
        buffer.append("        dateValue = new Date(Number(value));\n");
        buffer.append("    }\n");
        buffer.append("    return moment(dateValue.getTime()).toISOString();\n");
        buffer.append("}\n\n");

        // function timestamp
        buffer.append("function formatTimestamp(value) {\n");
        buffer.append("    var dateValue = value;\n");
        buffer.append("    if(value instanceof Date) {\n");
        buffer.append("        dateValue = value.getTime();\n");
        buffer.append("    }\n");
        buffer.append("    return dateValue.valueOf();\n");
        buffer.append("}\n\n");
        return buffer;
    }

    /**
     * JS function to get a Value by Path
     *
     * @return
     */
    private static StringBuffer getValueByPath() {
        StringBuffer buffer = new StringBuffer("");
        buffer.append("function getValueByPath(obj, path) {\n");
        buffer.append("    var a = path.split('.');\n");
        buffer.append("    for (var i = 0, n = a.length; i < n && (typeof obj == 'object'); ++i) {\n");
        buffer.append("        var k = a[i];\n");
        buffer.append("        if (k in obj) {\n");
        buffer.append("            if(obj[k] == null){\n");
        buffer.append("                return \"undefined\";\n");
        buffer.append("            }\n");
        buffer.append("            if(!(obj[k].constructor === Array)){\n");
        buffer.append("                obj = obj[k];\n");
        buffer.append("            }else{\n");
        buffer.append("                    obj = obj[k][0];\n");
        buffer.append("            }\n");
        buffer.append("        } else {\n");
        buffer.append("            return \"undefined\";\n");
        buffer.append("        }\n");
        buffer.append("    }\n");
        buffer.append("    if(i != a.length){\n");
        buffer.append("        return null; //path was not completely resolved\n");
        buffer.append("    }\n");
        buffer.append("    if(\"[object BSON]\" == obj.toString()){\n");
        buffer.append("        if(obj.hasOwnProperty('id') && obj['id'].hasOwnProperty('floatApprox')){\n");
        buffer.append("            var value = obj['id'];\n");
        buffer.append("            delete obj['id'];\n");
        buffer.append("            value = value.floatApprox;\n");
        buffer.append("            obj['id'] = value;\n");
        buffer.append("        }\n");
        buffer.append("        obj = JSON.stringify(obj);\n");
        buffer.append("    }\n");
        buffer.append("    if(typeof obj == 'string' && obj.includes(',')){\n");
        buffer.append("        return '\\\"'+obj.replace(new RegExp('\\\"', 'g'), '\\\"\\\"')+'\\\"';\n");
        buffer.append("    } else {\n");
        buffer.append("        return obj;\n");
        buffer.append("    }\n");
        buffer.append("}\n");
        return buffer;
    }

    /**
     * @return script to load a function
     */
    private String getFunctionLoader() {
        return "function loadFunction(functionName){\n" +
                "    var functionObject = db.getCollection(\"system.js\").findOne({\"_id\":functionName});\n" +
                "    if ((typeof Code !== 'undefined') && (functionObject.value.constructor === Code)) {\n" +
                "        functionObject = eval(\"(\" + functionObject.value.code + \")\");\n" +
                "    } else {\n" +
                "        functionObject = functionObject.value;\n" +
                "    }\n" +
                "    return functionObject;\n" +
                "}\n";
    }
}
