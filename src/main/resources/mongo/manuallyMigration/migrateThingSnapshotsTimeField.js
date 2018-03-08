/**
 * Created by achambi on 2/8/17.
 */
var cursor = db.thingSnapshots.find({"time": {"$exists": true}});
var total = cursor.count(false);
print("Total things to verify if have time field : " + total);
var index = 1;
cursor.forEach(function (snapshot) {
    var percentage = Math.round((index / total) * 100);
    if (percentage % 1 == 0) {
        print("Percentage: " + percentage + " %");
    }
    db.thingSnapshots.update({_id: snapshot._id}, {"$set": {"value.time": snapshot.time}});
    index++;
});