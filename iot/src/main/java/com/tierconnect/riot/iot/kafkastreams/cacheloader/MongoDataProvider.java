package com.tierconnect.riot.iot.kafkastreams.cacheloader;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.commons.dtos.*;
import org.apache.log4j.Logger;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MongoDataProvider class.
 *
 * @author jantezana
 * @version 2017/01/11
 */
public class MongoDataProvider {

    private static Logger logger = Logger.getLogger(MongoDataProvider.class);

    private MongoConfig mongo;
    // private Map<Long, Long> dataTypesByThingField = new HashMap<>();

    public MongoDataProvider(MongoConfig mongo/*,
                             Map<Long, Long> dataTypesByThingField*/)
    throws UnknownHostException {
        this.mongo = mongo;
        //  this.dataTypesByThingField = dataTypesByThingField;
        this.initializeMongoDaoUtil();
    }

    /**
     * Initialize MongoDaoUtil.
     *
     * @throws UnknownHostException
     */
    private void initializeMongoDaoUtil()
    throws UnknownHostException {
        MongoDAOUtil.setupMongodb(this.mongo.mongoPrimary, this.mongo.mongoSecondary, this.mongo.mongoReplicaSet, this.mongo.mongoSSL,
                                  this.mongo.username, this.mongo.password, this.mongo.mongoAuthDB, this.mongo.mongoDB,
                                  Constants.MONGO_CONTROL_READ_PREFERENCE, Constants.MONGO_REPORTS_READ_PREFERENCE, this.mongo.mongoSharding,
                                  this.mongo.mongoConnectTimeout, this.mongo.mongoMaxPoolSize);
    }

    /**
     * Gets the list of things.
     * @param groupIds
     * @return
     * @throws Exception
     */
    public DBCursor getThings(Long[] groupIds)
    throws Exception {
//        List<ThingDto> thingDtos = new LinkedList<>();
        DBCollection thingsCollection = MongoDAOUtil.getInstance().db.getCollection("things");
        BasicDBObject inQuery = new BasicDBObject();
        if(groupIds!=null) {
            inQuery.put("groupId", new BasicDBObject("$in", groupIds));
        }
        DBCursor thingsCursor = thingsCollection.find(inQuery);
        /*while (thingsCursor.hasNext()) {
            BasicDBObject thingMongo = (BasicDBObject) thingsCursor.next();
            ThingDto thingDto = buildThingDto(thingMongo);
            thingDtos.add(thingDto);
        }*/
        return thingsCursor;
    }

    public static ThingDto buildThingDto(BasicDBObject thingMongo, Map<Long, Long> dataTypesByThingField, List<ZoneDto> zoneDtos, List<LogicalReaderDto> logicalReaderDtos, List<ShiftDto> shiftDtos, List<ThingTypeDto> thingTypeDtos, List<GroupDto> groupDtos, List<GroupTypeDto> groupTypeDtos){
        ThingDto thingDto = new ThingDto();

        thingDto.id = thingMongo.getLong("_id");
        thingDto.serialNumber = thingMongo.getString("serialNumber");
        thingDto.name = thingMongo.getString("name");
        thingDto.createdTime = thingMongo.getDate("createdTime");
        thingDto.modifiedTime = thingMongo.getDate("modifiedTime");
        thingDto.time = thingMongo.getDate("time");
        thingDto.meta = new MetaDto();

        long thingTypeId = thingMongo.getLong("thingTypeId");
        thingDto.thingType = thingTypeDtos.stream().filter(tt -> tt.id == thingTypeId).findAny().orElse(null);
        if(thingDto.thingType == null){
            thingDto.thingType = new ThingTypeDto();
            thingDto.thingType.id = thingMongo.getLong("thingTypeId");
            thingDto.thingType.code = thingMongo.getString("thingTypeCode");
            thingDto.thingType.name = thingMongo.getString("thingTypeName");
        }

        long groupId = thingMongo.getLong("groupId");
        thingDto.group = groupDtos.stream().filter(groupDto -> groupDto.id == groupId).findAny().orElse(null);
        if (thingDto.group == null) {
            thingDto.group = new GroupDto();
            thingDto.group.id = thingMongo.getLong("groupId");
            thingDto.group.code = thingMongo.getString("groupCode");
            thingDto.group.name = thingMongo.getString("groupName");
        }

        long groupTypeId = thingMongo.getLong("groupTypeId");
        thingDto.group.groupType = groupTypeDtos.stream().filter(groupTypeDto -> groupTypeDto.id == groupTypeId).findAny().orElse(null);
        if(thingDto.group.groupType == null) {
            thingDto.group.groupType = new GroupTypeDto();
            thingDto.group.groupType.id = thingMongo.getLong("groupTypeId");
            thingDto.group.groupType.code = thingMongo.getString("groupTypeCode");
            thingDto.group.groupType.name = thingMongo.getString("groupTypeName");
        }

        thingDto.properties = new LinkedList<>();
        thingDto.properties.add(new HashMap<>());
        Set<String> fields = thingMongo.keySet();
        for (String field : fields) {
            Object fieldObject = thingMongo.get(field);
            if (fieldObject instanceof BasicDBObject) {
                BasicDBObject fieldMongo = (BasicDBObject) fieldObject;
                ThingPropertyDto tpw = new ThingPropertyDto();
                tpw.id = fieldMongo.getLong("thingTypeFieldId", 0);
                tpw.time = fieldMongo.getDate("time");
                tpw.dataTypeId = dataTypesByThingField.get(tpw.id);

                if (fieldMongo.containsField("value")) {
                    Object valueObject = fieldMongo.get("value");
                    if (valueObject instanceof BasicDBObject) {
                        BasicDBObject value = (BasicDBObject) valueObject;
                        switch (tpw.dataTypeId.intValue()) {
                            case 7: //SHIFT
                                ShiftDto shift = shiftDtos.stream().filter(shiftDto -> shiftDto.id == value.getLong("id")).findAny().orElse(null);
                                if(shift == null) {
                                    shift = new ShiftDto();
                                }
                                shift.id = value.getLong("id");
                                shift.code = value.getString("code");
                                shift.name = value.getString("name");
                                shift.active = value.getBoolean("active");
                                shift.endTimeOfDay = value.getLong("endTimeOfDay");
                                shift.daysOfWeek = value.getString("daysOfWeek");

                                tpw.value = shift;
                                break;
                            case 9: //ZONE
                                ZoneDto zone = zoneDtos.stream().filter(zoneDto -> zoneDto.id == value.getLong("id")).findAny().orElse(null);
                                if(zone == null) {
                                    zone = new ZoneDto();
                                    zone.zoneType = new ZonePropertyDto();
                                    zone.zoneType.id = 0L;
                                    zone.zoneGroup = new ZonePropertyDto();
                                    zone.zoneGroup.id = 0L;
                                    zone.facilityMap = new FacilityMapDto();
                                    zone.facilityMap.id = 0L;
                                }
                                zone.id = value.getLong("id");
                                zone.code = value.getString("code");
                                zone.name = value.getString("name");

                                zone.zoneType.time = value.getDate("zoneTypeTime");
                                zone.zoneType.code = value.getString("zoneType");
                                zone.zoneType.name = value.getString("zoneType");

                                zone.zoneGroup.time = value.getDate("zoneGroupTime");
                                zone.zoneGroup.code = value.getString("zoneGroup");
                                zone.zoneGroup.name = value.getString("zoneGroup");

                                zone.facilityMap.time = value.getDate("facilityMapTime");
                                zone.facilityMap.code = value.getString("facilityMap");
                                zone.facilityMap.name = value.getString("facilityMap");

                                tpw.value = zone;
                                break;
                            case 22: //GROUP
                                GroupDto group = new GroupDto();
                                group.id = value.getLong("id");
                                group.code = value.getString("code");
                                group.name = value.getString("name");
                                tpw.value = group;
                                break;
                            case 23: //LOGICAL READER
                                LogicalReaderDto lr = logicalReaderDtos.stream().filter(logicalReaderDto -> logicalReaderDto.id == value.getLong("id")).findAny().orElse(null);
                                if(lr == null) {
                                    lr = new LogicalReaderDto();
                                }
                                lr.id = value.getLong("id");
                                lr.code = value.getString("code");
                                lr.name = value.getString("name");
                                lr.zoneInId = value.getLong("zoneInId");
                                lr.zoneOutId = value.getLong("zoneOutId");
                                lr.x = value.getString("x");
                                lr.y = value.getString("y");
                                lr.z = value.getString("z");

                                tpw.value = lr;
                                break;
                            case 27:
                                tpw.value = buildThingDto(value, dataTypesByThingField, zoneDtos, logicalReaderDtos, shiftDtos, thingTypeDtos, groupDtos, groupTypeDtos);
                                break;
                            default:
                                tpw.value = value;
                        }
                    } else {
                        tpw.value = valueObject;
                    }
                }
                thingDto.properties.get(0).put(field, tpw);
            }
        }
        return  thingDto;
    }

}
