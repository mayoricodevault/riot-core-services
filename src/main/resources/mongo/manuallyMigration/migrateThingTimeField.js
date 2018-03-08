/**
 * Created by cvertiz on 9/30/16.
 */
db.things.find().forEach(function (thing) {
    var thingTime = new Date(0);
    var thingFieldName = "";
    for (var key in thing) {
        if (thing.hasOwnProperty(key) &&
            Object.prototype.toString.call(thing[key]) == "[object BSON]") {
            if (thing[key].hasOwnProperty("time")) {
                print("current Date, " + thingTime.getTime());
                if (thing[key]["time"].getTime() > thingTime.getTime()) {
                    thingTime = thing[key]["time"];
                    thingFieldName = key;
                }
            }
        }
    }
    var updateResult = db.things.updateOne(
        {"_id": thing._id},
        {$set: {"time": thingTime}},
        {upsert: true});
    printjson(updateResult);
});