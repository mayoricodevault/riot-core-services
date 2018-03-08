package com.tierconnect.riot.iot.popdb;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QGroup;
import com.tierconnect.riot.appcore.services.ConnectionService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.iot.dao.mongo.MongoScriptDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.dao.MongoExecutionException;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.NotFoundException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URLConnection;
import java.util.Date;
import java.util.Map;
import java.util.Stack;

/**
 * Created by dbascope on 5/9/17
 */
@SuppressWarnings("unused")
public class PopDBBaseUtils {
    private String currentPopDB;
    private Date currentPopDBDate;

    public PopDBBaseUtils(String popDbName, Date popDBDate) {
        currentPopDB = popDbName;
        currentPopDBDate = popDBDate;
    }

    String getPathForClass(String innerClass, String type) {
        return innerClass.toLowerCase().startsWith("thing")
            || innerClass.toLowerCase().startsWith("datatype")
            || innerClass.toLowerCase().startsWith("shift")
            || innerClass.toLowerCase().startsWith("edge")
            || innerClass.toLowerCase().startsWith("local")
            || innerClass.toLowerCase().startsWith("zone")
            || innerClass.toLowerCase().startsWith("report")
            || innerClass.toLowerCase().startsWith("notification")
            || innerClass.toLowerCase().startsWith("ml")
            || innerClass.toLowerCase().startsWith("parameters")
            || innerClass.toLowerCase().startsWith("logicalreader")
            || innerClass.toLowerCase().startsWith("smartcontract")
            || innerClass.toLowerCase().startsWith("folder")
            ? "com.tierconnect.riot.iot." + type + "." :
            "com.tierconnect.riot.appcore." + type + ".";
    }

    Object getReferencedPublicMap(String innerClass, Object reference, int level) throws Exception {
        String importPathEntities = getPathForClass(innerClass, "entities");
        Class<?> cls = Class.forName(importPathEntities + innerClass);
        Method methodMap = cls.getMethod("referencedPublicMap", int.class);
        Map<String, Object> res = (Map<String, Object>) methodMap.invoke(reference, level);
        if (res.containsKey("RFIDPrint")) {
            res.put("rfidprint", res.get("RFIDPrint"));
            res.remove("RFIDPrint");
        }
        return res;
    }

    public long getIdByNameAndThingTypeCode(String name, String thingTypeCode) {

        ThingTypeField thingTypeField = ThingTypeFieldService.getInstance().getByNameAndThingTypeCode(name, thingTypeCode);

        return thingTypeField.getId();
    }

    public String toString(Object field) {
        if (field != null) {
            return field.toString();
        } else {
            return "";
        }
    }

    public String jsonToString(Object json) {
        Object field = ((JSONObject) json).get("jsonObject");
        return toString(field);
    }

    public String insertTableConnectionLink(String code) {
        Connection connection = ConnectionService.getInstance().getByCode(code);
        if(connection == null) {
            throw new RuntimeException("Connection with code '" + code + "' does not exists");
        }
        return connection.getId().toString();
    }

    public String readTableScript(String scriptFile) throws IOException{
        InputStream is = PopDBBaseUtils.class.getClassLoader().getResourceAsStream("popDB/" + currentPopDB +
                "/resources/" + scriptFile);
        if (is != null) {
            return IOUtils.toString(is, "UTF-8");
        } else {
            throw new IOException();
        }
    }

    public String insertTableScript(String scriptFile, String name) throws IOException, MongoExecutionException {
        String text = readTableScript(scriptFile);
        ReportDefinition rd = ReportDefinitionService.getInstance().getByNameAndType(name, "mongo");
        if (rd != null) {
            MongoScriptDAO.getInstance().insert(rd.getId().toString(), text);
            return text;
        } else {
            return null;
        }
    }

    public String insertMongoScript(String scriptFile, String name) throws IOException, MongoExecutionException {
        String text = readTableScript(scriptFile);
        MongoScriptDAO.getInstance().insertRaw(name, text);
        return text;
    }

    public byte[] resourceToByteArray(Object resource) throws IOException {
        InputStream is = PopDBBaseUtils.class.getClassLoader().getResourceAsStream("popDB/" + currentPopDB +
                "/resources/" + resource);
        if (is != null) {
            try {
                return IOUtils.toByteArray(is);
            } catch (IOException e) {
                return null;
            }
        } else {
            throw new IOException();
        }
    }

    public Long getThingTypeFieldTemplateId(String name, ThingTypeTemplate thingTypeTemplate) {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QThingTypeFieldTemplate.thingTypeFieldTemplate.name.eq(name))
                .and(QThingTypeFieldTemplate.thingTypeFieldTemplate.thingTypeTemplate.id.eq(thingTypeTemplate.getId()));
        ThingTypeFieldTemplate thingTypeFieldTemplate = ThingTypeFieldTemplateService.getThingTypeFieldTemplateDAO()
                .selectBy(be);
        return thingTypeFieldTemplate != null ? thingTypeFieldTemplate.getId() : null;
    }

    public Long getDataTypeThingTypeId(String code) {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QThingType.thingType.thingTypeCode.eq(code));
        ThingType thingType = ThingTypeService.getThingTypeDAO().selectBy(be);
        return  thingType.getId();
    }

    public Long getGroupId(String code) {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QGroup.group.code.eq(code));
        Group group = GroupService.getGroupDAO().selectBy(be);
        if (group != null){
            return group.getId();
        }
        return 0L;
    }

    public Long getThingTypeId(String thingTypeCode, String groupCode) {
        try {
            Group group = GroupService.getInstance().getByCode(groupCode);
            BooleanBuilder be = new BooleanBuilder();
            be = be.and(QThingType.thingType.thingTypeCode.eq(thingTypeCode));
            be = be.and(QThingType.thingType.group.eq(group));
            ThingType thingType = ThingTypeService.getThingTypeDAO().selectBy(be);
            if (thingType != null){
                return thingType.getId();
            }
            return 0L;
        } catch (NonUniqueResultException e) {
            return 0L;
        }
    }

    public Object getThingTypeResource(String thingTypeCode, String groupCode)
        throws Exception {
        String resourceName = "_thingtype_" + getThingTypeId(thingTypeCode, groupCode);
        Group group = GroupService.getInstance().getByCode(groupCode);
        try {
            return getReferencedPublicMap("Resource",
                ResourceService.getInstance().getByNameAndGroup(resourceName, group),
                1);
        } catch (Exception e) {
            return "";
        }
    }

    public Object getReportDefinitionResource(String reportName, String reportType) {
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().getByNameAndType(reportName, reportType);
        Long reportDefinitionId = reportDefinition.getId();
        String resourceName = "_reportDefinition_" + reportDefinitionId;
        try {
            return getReferencedPublicMap("Resource",
                ResourceService.getInstance().getByNameAndGroup(resourceName, reportDefinition.getGroup()),
                1);
        } catch (Exception e) {
            return "";
        }
    }

    public Long getLocalMapId(String name, String groupCode) {
        try {
            Group group = GroupService.getInstance().getByCode(groupCode);
            BooleanBuilder be = new BooleanBuilder();
            be = be.and(QLocalMap.localMap.name.eq(name));
            be = be.and(QLocalMap.localMap.group.eq(group));
            LocalMap localMap = LocalMapService.getLocalMapDAO().selectBy(be);
            if (localMap != null){
                return localMap.getId();
            }
            return 0L;
        } catch (NonUniqueResultException e) {
            return 0L;
        }
    }

    public void createThing(JSONObject thingDef) throws IOException {
        String thingTypeCode = String.valueOf(thingDef.get("thingTypeCode"));
        String groupHierarchyCode = String.valueOf(thingDef.get("groupHierarchyCode"));
        String name = String.valueOf(thingDef.get("name"));
        String serialNumber = String.valueOf(thingDef.get("serial"));
        Map<String, Object> udfs = (Map<String, Object>) thingDef.get("udfs");
//        Map<String, Object> parent = (Map<String, Object>) thingDef.get("parent");
//        Map<String, Object> children = (Map<String, Object>) thingDef.get("children");
//        Map<String, Object> childrenUDFs = (Map<String, Object>) thingDef.get("childrenUDFs");
        Map<String, Object> parent = null;
        Map<String, Object> children = null;
        Map<String, Object> childrenUDFs = null;
        for (String udf : udfs.keySet()) {
            if (((JSONObject) udfs.get(udf)).containsKey("type")
                && StringUtils.equals(((JSONObject) udfs.get(udf)).get("type").toString(), "image")){
                String imageFileName = ((JSONObject) udfs.get(udf)).get("value").toString();
                byte[] content = resourceToByteArray(imageFileName);
                ThingImage thingImage = new ThingImage();
                thingImage.setContentType(URLConnection.guessContentTypeFromStream(
                    new ByteArrayInputStream(content)));
                thingImage.setImage(content);
                thingImage.setFileName(imageFileName);
                ((JSONObject) udfs.get(udf)).put("value", imageFileName + "|"
                    + ThingImageService.getInstance().insert(thingImage).getId());
            }
        }
        ThingsService.getInstance().create(new Stack<>(), thingTypeCode, groupHierarchyCode, name,
            serialNumber, parent, udfs, children, childrenUDFs, false, true, currentPopDBDate,
            true, true, true, null, null, true, null);
    }
}
