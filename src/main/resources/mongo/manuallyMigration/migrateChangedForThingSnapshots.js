/**
 * Created by achambi on 2/10/17.
 * Migrate thingsnapshots.
 */
db.thingSnapshotIds.find(
    {
        "blinks": {"$exists": true}
    },
    {
        "_id": -1,
        "blinks": {"$slice": -1}
    }).forEach(function(x){
    var thingSnapshots = db.thingSnapshots.findOne({"_id": x.blinks[0].blink_id})
    for(var index in thingSnapshots.value){
        if(
            Object.prototype.toString.call(thingSnapshots.value[index]) == "[object BSON]" &&
            index !== "parent"
        ){
            thingSnapshots.value[index]["changed"] = true;
        }
    }
    var resultUpdate = db.thingSnapshots.updateOne({"_id": x.blinks[0].blink_id},{"$set":thingSnapshots});
    printjson(resultUpdate);
});