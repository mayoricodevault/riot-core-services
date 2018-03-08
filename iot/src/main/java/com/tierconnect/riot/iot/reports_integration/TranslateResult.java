package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.ReportDefinitionUtils;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.iot.services.ZonePropertyValueService;
import com.tierconnect.riot.sdk.dao.ReportRecordException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;

import static com.tierconnect.riot.appcore.utils.EncryptionUtils.encrypt;
import static com.tierconnect.riot.commons.Constants.THING_TYPE_DATA_TYPE;
import static com.tierconnect.riot.commons.Constants.UNKNOWN_ZONE_NAME;

/**
 * Created by vealaro on 1/4/17.
 */
public abstract class TranslateResult {

    private static Logger logger = Logger.getLogger(TranslateResult.class);
    protected Map<String, Object> labelValues;
    protected Map<String, Date> timeStamps;
    protected Map<String, Boolean> labelValuesPathExist;
    protected ReportConfig configuration;
    protected List<Map<String, Object>> errorlog;
    protected boolean hasValueInPath = true;
    public static final String PATH_EXIST = "PATH.EXIST";
    public static final String KEY_ID_IMAGE = "Ima1geS2erv3let4";        // 128 bit key
    public static final String INIT_VECTOR_ID_IMAGE = "RandomInitVector"; // 16 bytes IV
    private String serverName = StringUtils.EMPTY;
    private String contextPath = StringUtils.EMPTY;

    public TranslateResult(ReportConfig configuration) {
        this.configuration = configuration;
    }

    public abstract void exportResult(Map<String, Object> result);

    protected void addlabelValues(String label, Object value) {
        addlabelValuesAndTimeStamp(label, value, null, null);
    }

    protected void addlabelValuesAndTimeStamp(String label, Object value, String propertyOriginal, Date timeStamp) {
        if (!labelValues.containsKey(label) ||
                (labelValues.containsKey(label) &&
                        (labelValues.get(label) == null ||
                                labelValues.get(label).toString().isEmpty()))) {
            labelValues.put(label, value);
            if (labelValuesPathExist != null) {
                labelValuesPathExist.put(label, this.hasValueInPath);
            }
            if (propertyOriginal != null && timeStamps != null) {
                timeStamps.put(propertyOriginal, timeStamp);
            }
            this.hasValueInPath = false;
        }
    }

    protected Map<String, Object> createErrorLog(Map<String, Object> record, Exception re) {
        Map<String, Object> mapErrorLog = new HashMap<>();
        mapErrorLog.put("exception", re);
        mapErrorLog.put("object", record);
        mapErrorLog.put("message", re.getMessage());
        return mapErrorLog;
    }

    protected Date time(PropertyReport propertyReport, Map<String, Object> result, Map<String, String> path, boolean isReportTimeSeries) {
        String property = propertyReport.getPropertyOriginal() + ".time";
        Object objectResult = valueToObject(propertyReport.getThingType(), property, result, path, isReportTimeSeries);
        return (objectResult instanceof Date ? (Date) objectResult : null);
    }

    protected Object value(PropertyReport propertyReport, Map<String, Object> result, Map<String, String> path, boolean isReportTimeSeries) {
        Object objectResult;
        try {
            if (ZoneTranslator.isZoneProperty(propertyReport.getPropertyOriginal())) {
                objectResult = valueToObject(null, propertyReport.getProperty(), result, path, isReportTimeSeries);
            } else {
                if( (propertyReport.getThingTypeField() != null) &&
                        propertyReport.getThingTypeField().getDataType().getId().compareTo(ThingTypeField.Type.TYPE_THING_TYPE.value) == 0 &&
                        (this.configuration.isThingTypeUdfAsObject())) {
                    objectResult = valueToObject(propertyReport.getThingType(),
                            propertyReport.getPropertyOriginal()+ Constants.PATH_TTUDF_VALUE, result, path, isReportTimeSeries);
                } else {
                    objectResult = valueToObject(propertyReport.getThingType(), propertyReport.getProperty(), result, path, isReportTimeSeries);
                }
            }
            if (objectResult != null) {
                validateObjectResult(objectResult, propertyReport); // validation with data type
                objectResult = processObject(objectResult, propertyReport); // process object
            } else if (propertyReport.getDataType().equals(ThingTypeField.Type.TYPE_ATTACHMENTS.value)) {
                ThingTypeField thingTypeFieldAttach;
                ThingType thingType = getThingTypeDateBase(getThingTypeId(result, isReportTimeSeries));
                if (thingType != null && (thingTypeFieldAttach = thingType.getAllThingTypeField(propertyReport.getPropertyOriginal())) != null) {
                    objectResult = thingTypeFieldAttach.getDefaultValue();
                }
            }
        } catch (Exception e) {
            errorlog = new ArrayList<>();
            errorlog.add(createErrorLog(result, e));
            Map<String, Object> mapValue = new HashMap<>();
            mapValue.put("error", "inconsistent data");
            mapValue.put("message", e.getMessage());
            objectResult = mapValue;
        }
        return objectResult;
    }

    private Object processObject(Object objectResult, PropertyReport propertyReport) {
        if (propertyReport.isTimestamp()) {
            return processTimestampValue((Long) objectResult, propertyReport);
        }
        if (propertyReport.getDataType().equals(ThingTypeField.Type.TYPE_IMAGE_ID.value)) {
            return urlDownloadImage(objectResult.toString());
        }
        if (propertyReport.getDataType().equals(ThingTypeField.Type.TYPE_ATTACHMENTS.value)) {
            return processAttachment(objectResult.toString());
        } else {
            if (objectResult instanceof Date) {
                if (ReportDefinitionUtils.isDwell(propertyReport.getPropertyOriginal())) {
                    objectResult = toDwell((Date) objectResult);
                } else if (propertyReport.isDate()) {
                    objectResult = processTimestampValue(((Date) objectResult).getTime(), propertyReport);
                }
            } else if (propertyReport.getSubDataTypeZone().equals(91L) || propertyReport.getSubDataTypeZone().equals(92L)) {
                objectResult = valueZoneTypeOrZoneProperty(propertyReport, objectResult, propertyReport.getSubDataTypeZone());
            } else {
                if (objectResult instanceof Map) {
                    if ((propertyReport.getThingTypeField() == null) || (propertyReport.getThingTypeField() != null &&
                            propertyReport.getThingTypeField().getDataType().getId().compareTo(ThingTypeField.Type.TYPE_THING_TYPE.value) != 0 &&
                            (!this.configuration.isThingTypeUdfAsObject()))) {
                        throw new ReportRecordException("'Value' does not correspond to data type: "
                                + ThingTypeField.Type.getTypeByValue(propertyReport.getDataType()).toString());
                    }
                } else if (objectResult instanceof Double) {
                    DecimalFormat decimalFormat = new DecimalFormat("#.##############");
                    decimalFormat.format(objectResult);
                } else {
                    objectResult = String.valueOf(objectResult);
                }
            }
        }
        return objectResult;
    }

    private void validateObjectResult(Object objectResult, PropertyReport propertyReport) {
        if (!objectResult.toString().isEmpty() && StringUtils.equals(propertyReport.getTypeParent(), THING_TYPE_DATA_TYPE)
                && (ThingTypeFieldService.getInstance().isValidDataTypeToCheck(propertyReport.getDataType())
                || propertyReport.getDataType().equals(ThingTypeField.Type.TYPE_TEXT.value)) // validate text data type
                && propertyReport.getSubDataTypeZone() == 0) {
            if (!ThingTypeFieldService.getInstance().checkDataType(objectResult, propertyReport.getDataType(), true)) {
                throw new ReportRecordException("'" + objectResult + "' does not correspond to data type: "
                        + ThingTypeField.Type.getTypeByValue(propertyReport.getDataType()).toString());
            }
        }
    }

    private Map<String, Object> processTimestampValue(Long objectResult, PropertyReport propertyReport) {
        return processTimestampValue(objectResult, propertyReport.getDateFormatAndTimeZone());
    }

    protected Map<String, Object> processTimestampValue(Long objectResult, DateFormatAndTimeZone dateFormatAndTimeZone) {
        Map<String, Object> mapTimestamp = new HashMap<>(3);
        mapTimestamp.put("value", dateFormatAndTimeZone.format(objectResult));
        mapTimestamp.put("iso", dateFormatAndTimeZone.getISODateTimeFormatWithoutTimeZone(objectResult));
        mapTimestamp.put("ts", objectResult);
        return mapTimestamp;
    }

    protected String processTimestampValueJSONString(Long objectResult, DateFormatAndTimeZone dateFormatAndTimeZone) {
        JSONObject jsonObject = new JSONObject(processTimestampValue(objectResult, dateFormatAndTimeZone));
        return jsonObject.toJSONString();
    }

    @SuppressWarnings("unchecked")
    private String processAttachment(String configurationAttachment) {
        String newValue = configurationAttachment;
        try {
            JSONObject configurationAttachmentJson = (JSONObject) new JSONParser().parse(configurationAttachment);
            if (configurationAttachmentJson.get("configuration") instanceof JSONObject) {
                // remove file system path
                JSONObject configurationJson = (JSONObject) configurationAttachmentJson.get("configuration");
                configurationJson.remove("fileSystemPath");
                configurationAttachmentJson.put("configuration", configurationJson);
            }

            if (configurationAttachmentJson.get("attachments") instanceof JSONArray) {
                // process download file
                JSONArray arrayAttachment = (JSONArray) configurationAttachmentJson.get("attachments");
                for (Object object : arrayAttachment) {
                    if (object instanceof JSONObject) {
                        JSONObject objectAttachment = (JSONObject) object;
                        String fileSystemPath = (String) objectAttachment.get("fileSystemPath");
                        if (fileSystemPath != null) {
                            String pathEncrypt = encrypt(KEY_ID_IMAGE, INIT_VECTOR_ID_IMAGE, fileSystemPath);
                            objectAttachment.put("fileSystemPath", pathEncrypt);
                            objectAttachment.put("download", getPath() + "/attachment/download?file=" + urlEncoder(pathEncrypt));
                        }
                    }
                }
            }
            newValue = configurationAttachmentJson.toJSONString();
        } catch (ParseException e) {
            logger.error("Error set configuration \"fileSystemPath and download\" attachment", e);
        }
        return newValue;
    }

    /**
     * return url image of servlet
     *
     * @param value udf value
     * @return url Download
     */
    private String urlDownloadImage(String value) {
        String[] pathImage = value.split(Pattern.quote("|"));
        if (pathImage.length >= 2) {
            String imageID = String.format("%019d", Long.valueOf(pathImage[1]));
            // image servlet ThingImageServlet.java
            return pathImage[0] + "|" + getPath() + "/thingImageServlet?id=" + urlEncoder(encrypt(KEY_ID_IMAGE, INIT_VECTOR_ID_IMAGE, imageID));
        }
        return value;
    }

    private String getPath() {
        return serverName.substring(0, serverName.indexOf(contextPath)) + contextPath;
    }

    private String urlEncoder(String url) {
        try {
            return URLEncoder.encode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.error("Error in generate URL encoder", e);
        }
        return url;
    }

    private String valueZoneTypeOrZoneProperty(PropertyReport propertyReport, Object value, Long subTypeZone) {
        String newValue = value != null ? UNKNOWN_ZONE_NAME : null;
        if (value instanceof Long) {
            if (ReportDefinitionUtils.isDwell(propertyReport.getPropertyOriginal())) {
                newValue = value.toString();
            } else {
                newValue = subTypeZone.equals(91L) ? getValueZoneType(value.toString()) :
                        getValueZoneProperty(value.toString(), propertyReport.getPropertyOriginal());
            }
        } else if (value instanceof String && !((String) value).isEmpty()) {
            newValue = subTypeZone.equals(91L) ? getValuesZoneType(StringUtils.split((String) value, ",")) :
                    getValuesZoneProperties(StringUtils.split((String) value, ","), propertyReport.getPropertyOriginal());
        }
        return newValue;
    }

    private String getValuesZoneType(String[] zoneIds) {
        String newValue = UNKNOWN_ZONE_NAME;
        List<String> values = new ArrayList<>(zoneIds.length);
        for (String zoneId : zoneIds) {
            values.add(getValueZoneType(zoneId));
        }
        if (!values.isEmpty()) {
            newValue = StringUtils.join(values, ", ");
        }
        return newValue;
    }

    private String getValuesZoneProperties(String[] zoneIds, String zonePropertyId) {
        String newValue = UNKNOWN_ZONE_NAME;
        List<String> values = new ArrayList<>(zoneIds.length);
        for (String zoneId : zoneIds) {
            values.add(getValueZoneProperty(zoneId, zonePropertyId));
        }
        if (!values.isEmpty()) {
            newValue = StringUtils.join(values, ", ");
        }
        return newValue;
    }

    private String getValueZoneType(String zoneIdorName) {
        if (Objects.equals(zoneIdorName, "0")) {
            return UNKNOWN_ZONE_NAME;
        }
        return getValueZone(zoneIdorName).getZoneType().getName();
    }

    private String getValueZoneProperty(String zoneIdorName, String zonePropertyId) {
        if (Objects.equals(zoneIdorName, "0")) {
            return UNKNOWN_ZONE_NAME;
        }
        String[] tokens = StringUtils.split(zonePropertyId, ",");
        return ZonePropertyValueService.getInstance().getZonePropertyValue(getValueZone(zoneIdorName).getId(), Long.valueOf(tokens[1]));
    }

    private Zone getValueZone(String zoneIdorName) {
        Zone zone = configuration.getMapZone().get(zoneIdorName);
        if (zone == null) {
            throw new ReportRecordException("Zone with (name or id)=" + zoneIdorName + " cannot be found");
        }
        return zone;
    }

    private Long toDwell(Date date) {
        return new Date().getTime() - date.getTime();
    }

    protected Object valueToObject(ThingType thingType, String property, Map<String, Object> result, Map<String, String> path, boolean isReportTimeSeries) {
        Long idTargetThingType = (thingType != null ? thingType.getId() : null);
        Long thingTypeIdOrigin = getThingTypeId(result, isReportTimeSeries);
        logger.debug("ID thingType Origin(" + thingTypeIdOrigin + "), ID thingType Target (" + idTargetThingType + ")");
        String pathString = isReportTimeSeries ? "value." + property : property;
        Object valueResult = null;
        if (idTargetThingType == null || thingTypeIdOrigin.equals(idTargetThingType)) {
            valueResult = getValueResult(result, pathString);
        } else {
            pathString = path.get(thingTypeIdOrigin + "-" + idTargetThingType);
            if (pathString != null) {
                pathString = isReportTimeSeries ? "value." + pathString : pathString;
                valueResult = getValueResult(result, pathString + "." + property);
            }
        }
        logger.debug("path: " + pathString + " return value= " + valueResult);
        return valueResult;
    }

    private Long getThingTypeId(Map<String, Object> result, boolean isReportTimeSeries) {
        Long thingTypeIdOrigin;
        if (isReportTimeSeries) {
            thingTypeIdOrigin = (Long) ((Map) result.get("value")).get("thingTypeId");
        } else {
            thingTypeIdOrigin = (Long) result.get("thingTypeId");
        }
        return thingTypeIdOrigin;
    }

    private ThingType getThingTypeDateBase(Long thingTypeId) {
        return ThingTypeService.getInstance().get(thingTypeId);
    }

    @SuppressWarnings("unchecked")
    private Object getValueResult(Map<String, Object> result, String pathString) {
        String[] tokens = pathString.split(Pattern.quote("."));
        Object value = null;
        Map<String, Object> resultCopy = result;
        int countTokens = 0;
        for (String token : tokens) {
            countTokens++;
            value = resultCopy.get(token);
            if (value instanceof Map) {
                resultCopy = (Map<String, Object>) resultCopy.get(token);
                this.hasValueInPath = (!token.equals("value"));
            } else if (value instanceof List) {
                List listMap = (List) value;
                if (!listMap.isEmpty()) {
                    resultCopy = (Map<String, Object>) listMap.get(0);
                    this.hasValueInPath = resultCopy.size() > 0;
                } else {
                    this.hasValueInPath = false;
                    return null;
                }
            } else {
                if (value == null) {
                    if (!pathString.contains("time")) {
                        this.hasValueInPath = false;
                    }
                } else {
                    this.hasValueInPath = true;
                }
                return (countTokens == tokens.length) ? value : null;
            }
        }
        return value;
    }

    public Map<String, Object> getLabelValues() {
        return labelValues;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
}
