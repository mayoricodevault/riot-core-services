package com.tierconnect.riot.migration.older;

import com.mongodb.BasicDBObject;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.entities.ZoneType;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.iot.services.ZoneService;
import com.tierconnect.riot.iot.services.ZoneTypeService;
import com.tierconnect.riot.migration.DBHelper;
import org.apache.log4j.Logger;

import java.text.Normalizer;
import java.util.*;

/**
 * Created by cvertiz on 05/14/2016.
 * Modify by cvertiz on 05/14/2016.
 */
@Deprecated
public class V_040100_040200 implements MigrationStepOld {

    static Logger logger = Logger.getLogger(V_040100_040200.class);

    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(40100);
    }

    @Override
    public int getToVersion() {
        return 40200;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V040100_to_040200.sql");
    }

    @Override
    public void migrateHibernate() throws Exception {

        //Migrating zoneTypeCode foreach thingType
        List<Map<String, Object>> result = new ArrayList<>();
        for (ThingType thingType : ThingTypeService.getInstance().getAllThingTypes()) {
            for (ThingTypeField thingTypeField : thingType.getThingTypeFields()) {
                if ( thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_ZONE.value) == 0 ) {
                    //TODO DO NOT MERGE THIS CHANGE TO MASTER. ONLY FOR ANALYTICS
                    //result.add(migrateZoneType(thingType.getCode(), thingTypeField.getName()));
                }
            }
        }
        logger.info("Migration finished : " + result. toString());
    }

    @Override
    public void migrateSQLAfter() throws Exception {
    }

    public Map<String, Object> migrateZoneType(String thingTypeCode, String zoneUdfName) {
        String zoneUdfNameSnapshot = "value." + zoneUdfName;
        logger.info("Starting zoneTypeCode migration for " + thingTypeCode + " thingType.");

        // Migrate mysql zonTypeCode == null
        for (ZoneType zoneType : ZoneTypeService.getZoneTypeDAO().selectAll()) {
            if (zoneType.getZoneTypeCode() == null) {
                String code = zoneType.getName().replaceAll(" ", "");
                code = Normalizer.normalize(code, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
                zoneType.setZoneTypeCode(code);
                ZoneTypeService.getInstance().update(zoneType);
            }
        }

        Map<String, Object> result = new HashMap<>();
        Map<String, Object> affectedRows = new HashMap<>();

        List<Zone> zoneList = ZoneService.getZoneDAO().selectAll();

        for (Zone zone : zoneList) {
            Map<String, Object> collection = new HashMap<>();

            // things
            BasicDBObject queryDoc = new BasicDBObject(zoneUdfName, new BasicDBObject("$exists", true))
                    .append(zoneUdfName + ".value.zoneType", new BasicDBObject("$exists", false))
                    .append(zoneUdfName + ".value.code", zone.getCode())
                    .append("thingTypeCode", thingTypeCode);

            BasicDBObject setDoc = new BasicDBObject("$set", new BasicDBObject(zoneUdfName + ".value.zoneType", zone.getZoneType().getZoneTypeCode()));

            WriteResult wr = MongoDAOUtil.getInstance().things.update(queryDoc, setDoc, false, true, WriteConcern.ACKNOWLEDGED);
            collection.put("things", wr.getN());

            // thingSnapshots

            queryDoc = new BasicDBObject(zoneUdfNameSnapshot, new BasicDBObject("$exists", true))
                    .append(zoneUdfNameSnapshot + ".value.zoneType", new BasicDBObject("$exists", false))
                    .append(zoneUdfNameSnapshot + ".value.code", zone.getCode())
                    .append("value.thingTypeCode", thingTypeCode);


            setDoc = new BasicDBObject("$set", new BasicDBObject(zoneUdfNameSnapshot + ".value.zoneType", zone.getZoneType().getZoneTypeCode()));
            wr = MongoDAOUtil.getInstance().thingSnapshots.update(queryDoc, setDoc, false, true);
            collection.put("thingSnapshots", wr.getN());

            affectedRows.put(zone.getCode(), collection);

        }

        result.put("affectedDocuments", affectedRows);
        result.put("totalZones", zoneList.size());

        logger.info("Finished zoneTypeCode migration for " + thingTypeCode + " thingType.");

        return result;
    }
}