package com.tierconnect.riot.iot.job;

import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.commons.dao.mongo.SnapshotFixEntryCommons;
import com.tierconnect.riot.commons.dao.mongo.SnapshotFixerJobCommons;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.services.LocalMapService;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.iot.services.ZoneGroupService;
import com.tierconnect.riot.iot.services.ZoneTypeService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created by cvertiz on 9/15/16.
 */
public class SnapshotFixerJobServices {


    private static PriorityBlockingQueue<SnapshotFixEntryCommons> queue = new PriorityBlockingQueue<>(1000);
    private static SnapshotFixerJobCommons job;
    private static Map<String, Long> thingTypeFieldIds;

    public static void init(){
        thingTypeFieldIds = new HashMap<>();
        for(ThingTypeField thingTypeField : ThingTypeFieldService.getThingTypeFieldDAO().selectAll()){
            thingTypeFieldIds.put(thingTypeField.getName(), thingTypeField.getId());
        }

        job = new SnapshotFixerJobCommons(queue,
                "SnapshotFixerJob",
                ZoneTypeService.getZoneTypeDAO().selectAll().size(),
                ZoneGroupService.getZoneGroupDAO().selectAll().size(),
                LocalMapService.getLocalMapDAO().selectAll().size());
        job.start();

    }

    public static void offer(ChangedFieldsServices changedFields, Long thingId, String serialNumber, String thingTypeCode, Long timestamp, boolean hasPreviousSnapshot){
        queue.offer(new SnapshotFixEntryCommons(changedFields,
                thingId,
                serialNumber,
                thingTypeCode,
                timestamp,
                hasPreviousSnapshot,
                MongoDAOUtil.getInstance().mongoClient.getDatabase(MongoDAOUtil.getInstance().db.getName()).getCollection("things"),
                MongoDAOUtil.getInstance().mongoClient.getDatabase(MongoDAOUtil.getInstance().db.getName()).getCollection("thingSnapshots"),
                System.currentTimeMillis()
        ));
    }
}
