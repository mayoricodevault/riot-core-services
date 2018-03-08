/**
 * Created by achambi on 8/22/16.
 *
 */
function V_040300_RC12_040300_RC13() {

    function verifyFieldExists(field) {
        var fieldType = Object.prototype.toString.call(field);
        if (!(fieldType === "[object Undefined]")
            && !(fieldType === "[object Null]")) {
            return true;
        }
        else {
            return false;
        }
    }

    var bulkThingSnapshotIds = db.thingSnapshotIds.initializeUnorderedBulkOp();
    var updateCounter = 0;
    var lotNumber = 10;

    db.things.find({})
        .forEach(
            function (thing) {
                /*Things field to set in thingSnapshotIds*/
                var updateJson = {};

                if (verifyFieldExists(thing.groupTypeId)) {
                    updateJson["groupTypeId"] = thing.groupTypeId;
                }

                if (verifyFieldExists(thing.groupTypeCode)) {
                    updateJson["groupTypeCode"] = thing.groupTypeCode;
                }

                if (verifyFieldExists(thing.thingTypeId)) {
                    updateJson["thingTypeId"] = thing.thingTypeId;
                }

                if (verifyFieldExists(thing.thingTypeCode)) {
                    updateJson["thingTypeCode"] = thing.thingTypeCode;
                }

                if (verifyFieldExists(thing.groupId)) {
                    updateJson["groupId"] = thing.groupId;
                }

                if (verifyFieldExists(thing.groupCode)) {
                    updateJson["groupCode"] = thing.groupCode;
                }

                if (updateJson.constructor === Object && Object.keys(updateJson).length != 0) {
                    bulkThingSnapshotIds.find({"_id": thing._id}).update(
                        {"$set": updateJson},
                        {
                            "upsert": false,
                            "multi": false
                        }
                    );
                    updateCounter++;
                }

                if (updateCounter % lotNumber == 0) {
                    bulkThingSnapshotIds.execute();
                    bulkThingSnapshotIds = db.thingSnapshotIds.initializeUnorderedBulkOp();
                }
            }
        );

    if (bulkThingSnapshotIds.nUpdateOps > 0) {
        bulkThingSnapshotIds.execute();
    }
    return "Update Success";
}
V_040300_RC12_040300_RC13();