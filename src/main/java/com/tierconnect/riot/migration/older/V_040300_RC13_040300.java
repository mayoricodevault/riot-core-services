package com.tierconnect.riot.migration.older;

import com.tierconnect.riot.iot.dao.mongo.MongoScriptDAO;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.ThingTypeFieldTemplate;
import com.tierconnect.riot.iot.entities.ThingTypeTemplate;
import com.tierconnect.riot.iot.popdb.PopDBMojixRetail;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.DataTypeService;
import com.tierconnect.riot.iot.services.ThingTypeFieldTemplateService;
import com.tierconnect.riot.iot.services.ThingTypeTemplateService;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author : vealaro
 * @version : 4.3.0 RC-13
 * @date : 9/12/16
 */
@Deprecated
public class V_040300_RC13_040300 implements MigrationStepOld {
    Logger logger = Logger.getLogger(V_040300_RC13_040300.class);

    @Override
    public List<Integer> getFromVersions() {
        return Collections.singletonList(4030013);
    }

    @Override
    public int getToVersion() {
        return 40300;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateThingTypeFieldTemplates();
    }

    @Override
    public void migrateSQLAfter() throws Exception {
//        MongoDAOUtil.setupMongodb(Configuration.getProperty("mongo.host"),
//                Integer.parseInt(Configuration.getProperty("mongo.port")),
//                Configuration.getProperty("mongo.mongoAddressReplica"),
//                Configuration.getProperty("mongo.db"),
//                null,
//                null,
//                Configuration.getProperty("mongo.username"),
//                Configuration.getProperty("mongo.password"));
//        MongoDBHelper dbHelper = new MongoDBHelper();
//        dbHelper.executeMongoFile("mongo/JsonFormatter.js");
        try {
            URL fileURL = this.getClass().getClassLoader().getResource("mongo/JsonFormatter.js");
            if (fileURL != null) {
                String text = IOUtils.toString(fileURL, Charset.forName("UTF-8"));
                MongoScriptDAO.getInstance().insertRaw("JSONFormatter", text);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * migrateThingTypeFieldTemplates
     */
    public void migrateThingTypeFieldTemplates(){
        insertThingTypeFieldsRFIDTag();
     }

    /**
     * insertNewThingTypeFieldsRFIDTag
     * Insert a new thing type fields  into a RFID Tag Thing Type Template
     */
    public void insertThingTypeFieldsRFIDTag(){
        ThingTypeTemplate thingTypeTemplate = ThingTypeTemplateService.getInstance().getByNameUnique("RFID Tag");
        Set<String> labelsRFIDTag = ThingTypeFieldTemplateService.getInstance().getLabelsThingTypeFieldTemplate(thingTypeTemplate);
        if (!labelsRFIDTag.isEmpty()) {
            if (!labelsRFIDTag.contains("lastLocateTime")) {
                PopDBRequiredIOT.insertUdfField("lastLocateTime", "lastLocateTime", "", "",
                        DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), "DATA_TYPE", false, thingTypeTemplate, "");
            }
            if (!labelsRFIDTag.contains("lastDetectTime")) {
                PopDBRequiredIOT.insertUdfField("lastDetectTime", "lastDetectTime", "", "",
                        DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_TIMESTAMP.value), "DATA_TYPE", false, thingTypeTemplate, "");
            }
            if (!labelsRFIDTag.contains("zone")) {
                PopDBRequiredIOT.insertUdfField("zone", "zone", "", "",
                        DataTypeService.getInstance().get(ThingTypeField.Type.TYPE_ZONE.value), "DATA_TYPE", true, thingTypeTemplate, "");
            }
            if (labelsRFIDTag.contains("location")) {
                ThingTypeFieldTemplate thingTypeFieldTemplateLocation = ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplate.getId(),"location");
                if ( thingTypeFieldTemplateLocation != null ) {
                    thingTypeFieldTemplateLocation.setTimeSeries(true);
                    ThingTypeFieldTemplateService.getInstance().update(thingTypeFieldTemplateLocation);
                }
            }
            if (labelsRFIDTag.contains("locationXYZ")) {
                ThingTypeFieldTemplate thingTypeFieldTemplateLocation = ThingTypeFieldTemplateService.getInstance().getThingTypeFieldTemplateByThingTypeTemplate(thingTypeTemplate.getId(),"locationXYZ");
                if ( thingTypeFieldTemplateLocation != null ) {
                    thingTypeFieldTemplateLocation.setTimeSeries(true);
                    ThingTypeFieldTemplateService.getInstance().update(thingTypeFieldTemplateLocation);
                }
            }
        }
    }


}
