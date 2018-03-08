
/**
 * Change zone name parameter here and run it for each ThingType containing zone type UDF
 */

var zoneUDFName = "zone";

function migrateZonePropertiesTimes(zoneUDFName) {

    function getFieldDwellTime(thingSnapshotDateTimeTimeStamp, firstFieldDwellTime) {
        return thingSnapshotDateTimeTimeStamp - firstFieldDwellTime;
    }

    function updateFirtSnapShot(previousFieldValue, fieldName, firstFieldValueThingSnapshotId, firstFieldDwellTime) {
        if (previousFieldValue != "") {
            var jsonUpdateFirstSnapShot = {};
            jsonUpdateFirstSnapShot["value." + zoneUDFName + ".value." + fieldName + "DwellTime"] = new NumberLong(firstFieldDwellTime);
            var result = db.thingSnapshots.update({"_id": firstFieldValueThingSnapshotId}, {$set: jsonUpdateFirstSnapShot});
        }
    }

    function getFirstFieldDwellTime(thingSnapshotDateTimeTimeStamp, fieldName, previousFieldValue, firstFieldValueThingSnapshotId, firstFieldTime) {


        firstFieldDwellTime = getFieldDwellTime(thingSnapshotDateTimeTimeStamp, firstFieldTime);
        firstFieldTime = thingSnapshotDateTimeTimeStamp;
        updateFirtSnapShot(previousFieldValue, fieldName, firstFieldValueThingSnapshotId, firstFieldDwellTime);
        return firstFieldTime;
    }

    function updateThingSnapshot(jsonUpdateSnapShot, fieldName, fieldValueDateTime, fieldValueDwellTime) {

        jsonUpdateSnapShot["value." + zoneUDFName + ".value." + fieldName + "Time"] = fieldValueDateTime;
        jsonUpdateSnapShot["value." + zoneUDFName + ".value." + fieldName + "DwellTime"] = new NumberLong(fieldValueDwellTime);
        return jsonUpdateSnapShot;

    }

    function updateTimesThing(zoneGroupDateTime, facilityMapDateTime, zoneTypeDateTime) {
        var jsonUpdateThing = {};
        jsonUpdateThing[zoneUDFName + ".value.zoneGroupTime"] = zoneGroupDateTime;
        jsonUpdateThing[zoneUDFName + ".value.facilityMapTime"] = facilityMapDateTime;
        jsonUpdateThing[zoneUDFName + ".value.zoneTypeTime"] = zoneTypeDateTime;
        db.things.update({"serialNumber": thingSnapShotSerialNumber}, {$set: jsonUpdateThing});
    }

    var thingSnapShotSerialNumber = "";
    var zoneFieldNotExists = false;


    var zoneGroupLabelName = "zoneGroup";
    var previousZoneGroup = "";
    var zoneGroupDateTime = null;
    var firstZoneGroupTime = 0;
    var firstZoneGroupThingSnapshotId = null;
    var zoneGroupChanged = false;
    var zoneGroupNotExists = false;

    var facilityMapLabelName = "facilityMap";
    var previousFacilityMap = "";
    var facilityMapDateTime = null;
    var firstFacilityMapDwellTime = 0;
    var firstFacilityMapThingSnapshotId = null;
    var facilityMapChanged = false;
    var facilityMapNotExists = true;

    var zoneTypeLabelName = "zoneType";
    var previousZoneType = "";
    var zoneTypeDateTime = null;
    var firstZoneTypeDwellTime = 0;
    var firstZoneTypeThingSnapshotId = null;
    var zoneTypeChanged = false;
    var zoneTypeNotExists = false;

    var query = {};
    query[zoneUDFName] = {$exists : true};

    db.thingSnapshots.createIndex({"value.serialNumber": 1, "time": 1});
    var ids = db.getCollection('things').find(query).map(function (x) {
        return x._id;
    });

    var queryTS = {};
    queryTS["value._id"] = {$in: ids};
    queryTS["value." + zoneUDFName] = {$exists : true};
    queryTS["value." + zoneUDFName + ".time"] = {$exists : true};

    var projectionTS = {};
    projectionTS["value.serialNumber"] = 1;
    projectionTS["time"] = 1;
    projectionTS["value." + zoneUDFName] = 1;

    var cursor = db.getCollection('thingSnapshots')
        .find(queryTS,
            projectionTS
        ).sort({"value.serialNumber": 1, "time": 1})
        .forEach(
            function (thingSnapShot) {

                var zoneData = thingSnapShot.value[zoneUDFName];
                var currentSerialNumber = thingSnapShot.value.serialNumber.replace(/,/g, "");


                if (thingSnapShotSerialNumber != currentSerialNumber || thingSnapShotSerialNumber == "") {
                    if (thingSnapShotSerialNumber != "") {
                        updateTimesThing(zoneGroupDateTime, facilityMapDateTime, zoneTypeDateTime);
                    }
                    thingSnapShotSerialNumber = currentSerialNumber;
                    zoneFieldNotExists = false;

                    previousZoneGroup = "";
                    zoneGroupDateTime = null;
                    firstZoneGroupTime = 0;
                    firstZoneGroupThingSnapshotId = null;
                    zoneGroupChanged = false;
                    zoneGroupNotExists = false;

                    previousFacilityMap = "";
                    facilityMapDateTime = null;
                    firstFacilityMapDwellTime = 0;
                    firstFacilityMapThingSnapshotId = null;
                    facilityMapChanged = false;
                    facilityMapNotExists = false;

                    previousZoneType = "";
                    zoneTypeDateTime = null;
                    firstZoneTypeDwellTime = 0;
                    firstZoneTypeThingSnapshotId = null;
                    zoneTypeChanged = false;
                    zoneTypeNotExists = false;
                }
                if ((Object.prototype.toString.call(zoneData) == "[object BSON]" &&
                    Object.prototype.toString.call(zoneData["value"]) == "[object BSON]") ||
                    (Object.prototype.toString.call(zoneData) == "[object Object]" &&
                    Object.prototype.toString.call(zoneData["value"]) == "[object Object]")

                ) {
                    if (!zoneData["value"].hasOwnProperty("code")
                        || !zoneData["value"].hasOwnProperty("name")
                        || !zoneData["value"].hasOwnProperty("id")
                        || zoneData["value"]["code"].toLowerCase() == "unknown"
                        || zoneData["value"]["name"].toLowerCase() == "unknown"
                        || zoneData["value"]["id"] == 0) {

                        zoneData["value"][zoneGroupLabelName] = "unknown";
                        zoneData["value"][facilityMapLabelName] = "unknown";
                        zoneData["value"][zoneTypeLabelName] = "unknown";
                        zoneFieldNotExists = true;

                    }
                    if (!zoneData["value"].hasOwnProperty(zoneGroupLabelName) ||
                        zoneData["value"][zoneGroupLabelName] == null) {
                        zoneData["value"][zoneGroupLabelName] = "unknown";
                        zoneGroupNotExists = true;
                    }

                    if (!zoneData["value"].hasOwnProperty(facilityMapLabelName) ||
                        zoneData["value"][facilityMapLabelName] == null) {
                        zoneData["value"][facilityMapLabelName] = "unknown";
                        facilityMapNotExists = true;
                    }

                    if (!zoneData["value"].hasOwnProperty(zoneTypeLabelName) ||
                        zoneData["value"][zoneTypeLabelName] == null) {
                        zoneData["value"][zoneTypeLabelName] = "unknown";
                        zoneTypeNotExists = true;
                    }

                    if (previousZoneGroup != zoneData["value"][zoneGroupLabelName].replace(/,/g, "")) {

                        firstZoneGroupTime = getFirstFieldDwellTime(thingSnapShot.time.getTime(),
                            zoneGroupLabelName,
                            previousZoneGroup,
                            firstZoneGroupThingSnapshotId,
                            firstZoneGroupTime);
                        firstZoneGroupThingSnapshotId = thingSnapShot._id;
                        zoneGroupDateTime = thingSnapShot.time;
                        zoneGroupChanged = true;
                    }
                    if (previousFacilityMap != zoneData["value"][facilityMapLabelName].replace(/,/g, "")) {
                        firstFacilityMapDwellTime = getFirstFieldDwellTime(thingSnapShot.time.getTime(), facilityMapLabelName, previousFacilityMap, firstFacilityMapThingSnapshotId, firstFacilityMapDwellTime);
                        firstFacilityMapThingSnapshotId = thingSnapShot._id;
                        facilityMapDateTime = thingSnapShot.time;
                        facilityMapChanged = true;
                    }
                    if (previousZoneType != zoneData["value"][zoneTypeLabelName].replace(/,/g, "")) {
                        firstZoneTypeDwellTime = getFirstFieldDwellTime(thingSnapShot.time.getTime(),
                            zoneTypeLabelName,
                            previousZoneType,
                            firstZoneTypeThingSnapshotId,
                            firstZoneTypeDwellTime);
                        firstZoneTypeThingSnapshotId = thingSnapShot._id;
                        zoneTypeDateTime = thingSnapShot.time;
                        zoneTypeChanged = true;
                    }

                    var jsonUpdateSnapShot = {};
                    var zoneGroupDwellTime = getFieldDwellTime(thingSnapShot.time.getTime(), zoneGroupDateTime.getTime());
                    jsonUpdateSnapShot = updateThingSnapshot(jsonUpdateSnapShot, zoneGroupLabelName, zoneGroupDateTime, zoneGroupDwellTime);

                    if (zoneGroupChanged) {
                        jsonUpdateSnapShot["value." + zoneUDFName + ".value." + zoneGroupLabelName + "Changed"] = zoneGroupChanged;
                        zoneGroupChanged = false;
                    }

                    var facilityMapDwellTime = getFieldDwellTime(thingSnapShot.time.getTime(), facilityMapDateTime.getTime());
                    jsonUpdateSnapShot = updateThingSnapshot(jsonUpdateSnapShot, facilityMapLabelName, facilityMapDateTime, facilityMapDwellTime);

                    if (facilityMapChanged) {
                        jsonUpdateSnapShot["value." + zoneUDFName + ".value." + facilityMapLabelName + "Changed"] = facilityMapChanged;
                        facilityMapChanged = false;
                    }

                    var zoneTypeDwellTime = getFieldDwellTime(thingSnapShot.time.getTime(), zoneTypeDateTime.getTime());
                    jsonUpdateSnapShot = updateThingSnapshot(jsonUpdateSnapShot, zoneTypeLabelName, zoneTypeDateTime, zoneTypeDwellTime);

                    if (zoneTypeChanged) {
                        jsonUpdateSnapShot["value." + zoneUDFName + ".value." + zoneTypeLabelName + "Changed"] = zoneTypeChanged;
                        zoneTypeChanged = false;
                    }

                    if (zoneFieldNotExists) {
                        jsonUpdateSnapShot["value." + zoneUDFName + ".value." + zoneGroupLabelName] = "unknown";
                        jsonUpdateSnapShot["value." + zoneUDFName + ".value." + facilityMapLabelName] = "unknown";
                        jsonUpdateSnapShot["value." + zoneUDFName + ".value." + zoneTypeLabelName] = "unknown";


                        jsonUpdateSnapShot["value." + zoneUDFName + ".value.code"] = "unknown";
                        jsonUpdateSnapShot["value." + zoneUDFName + ".value.name"] = "Unknown";
                        jsonUpdateSnapShot["value." + zoneUDFName + ".value.id"] = new NumberLong(0);
                        zoneFieldNotExists = false;
                    }
                    if (zoneGroupNotExists) {
                        jsonUpdateSnapShot["value." + zoneUDFName + ".value." + zoneGroupLabelName] = "unknown";
                        zoneGroupNotExists = false;
                    }
                    if (facilityMapNotExists) {
                        jsonUpdateSnapShot["value." + zoneUDFName + ".value." + facilityMapLabelName] = "unknown";
                        facilityMapNotExists = false;
                    }
                    if (zoneTypeNotExists) {
                        jsonUpdateSnapShot["value." + zoneUDFName + ".value." + zoneTypeLabelName] = "unknown";
                        zoneTypeNotExists = false;
                    }
                    previousZoneGroup = zoneData["value"][zoneGroupLabelName].replace(/,/g, "");
                    previousFacilityMap = zoneData["value"][facilityMapLabelName].replace(/,/g, "");
                    previousZoneType = zoneData["value"][zoneTypeLabelName].replace(/,/g, "");
                    db.thingSnapshots.update({"_id": thingSnapShot._id}, {$set: jsonUpdateSnapShot});

                }
            }
        );
    updateTimesThing(zoneGroupDateTime, facilityMapDateTime, zoneTypeDateTime);
    return "Update Success";
}
migrateZonePropertiesTimes(zoneUDFName);