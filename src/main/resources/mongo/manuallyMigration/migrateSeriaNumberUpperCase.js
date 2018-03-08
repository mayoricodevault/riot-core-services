/**
 * Created by cvertiz on 9/30/16.
 */
/**
 * Modified by orivera on 8/16/17.
 */
db.things.find().forEach(function(x) {
    db.things.update({ _id: x._id }, { "$set": { "serialNumber": x.serialNumber.toUpperCase() } }, { upsert: false, multi: false });
    if (x.hasOwnProperty("children")) {
        var childrenField = x.children;
        for(i = 0 ; i < childrenField.length ; i ++){
            var element = childrenField[i];
            db.things.update({ _id: x._id , "children.serialNumber": element.serialNumber}, {$set: {"children.$.serialNumber" : element.serialNumber.toUpperCase}}, { upsert: false, multi: false });
        }
    }
});
//var index = 1
var cursor = db.thingSnapshots.find();
//var total = cursor.count();
cursor.forEach(function (x) {
    //var percentage = Math.round((index/total)*100);
    //if(percentage % 1 == 0){
    //    print("Percentage: " + percentage + " %");
    //}
    db.thingSnapshots.update({"_id": x._id}, {"$set": {"value.serialNumber": x.value.name.toUpperCase()}}, {
        upsert: false,
        multi: false
    });
    //index++;
});
