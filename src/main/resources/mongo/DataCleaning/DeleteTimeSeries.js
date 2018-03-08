/*******************************************
 @author: Ruth Chirinos
 @date  : 18/04/2016
 @description: Script delete data (before Jan 1st 2016) 1451620800000
  1. get all documents in thingSnapshotIds which have blinks in the period of time
  2. Iterate each result. For each result iterate its blinks and check whether 'time' is between
     the period of time. Push those blinks in an array.
  3. Remove all blinks identified in thingSnapshotIds.blinks and thingSnapshots
  4. If the thing has not any blink in thingSnapshotIds.blinks, it has to be deleted
 ********************************************/
var ini = new Date();
var iniTime = ini.getMilliseconds();
var utcTimeIni = new Date(2016,3,26,11,0,0);
var timeValueTimeZoneIni = utcTimeIni.getTime();
var utcTimeEnd = new Date(2016,3,26,23,59,59);
var timeValueTimeZoneEnd = utcTimeEnd.getTime();
var _snapshotIds = db.getCollection('thingSnapshotIds');
var _thingSnapshots = db.getCollection('thingSnapshots');

//print(timeValueTimeZoneIni+" "+timeValueTimeZoneEnd);
//Initial Verification
var count      = 0;
var verifCount = 0;
count = _thingSnapshots.find({}).count();
//print(timeValueTimeZoneIni+" "+timeValueTimeZone);
//Get all snapshots which contains blinks before Jan 1st 2016
var cursor = _snapshotIds.find({
        "blinks": { $elemMatch : {
            "time": { $gte:timeValueTimeZoneIni,$lte: timeValueTimeZoneEnd  }
        }}
    }
).forEach( function(myDoc) {
    var blinks = myDoc.blinks;
    if(blinks!=null){
        var snap = [];
        for(i = 0; i<blinks.length; i++){
            //Delete Snapshot
            if(blinks[i].time>=timeValueTimeZoneIni &&
                blinks[i].time<=timeValueTimeZoneEnd ){
                var blinkData = blinks[i];
                var id    = blinkData.blink_id.valueOf();
                snap.push(ObjectId(id));
                verifCount = verifCount+1;
            }
        }
        _thingSnapshots.remove({_id:{$in:snap}});
        _snapshotIds.update({_id:myDoc._id}
            ,{ $pull:{
                "blinks":{
                    "time"  :{
                        $gte:timeValueTimeZoneIni,$lte: timeValueTimeZoneEnd  }
                }}});
        var cursorAggregation = _snapshotIds.aggregate([
            { $match:{_id:myDoc._id}},
            {$project:{_id:1,numberBlinks:{$size:"$blinks"}}}
        ]);
        while (cursorAggregation.hasNext()) {
            var toDelete = cursorAggregation.next();
            if(toDelete.numberBlinks ==0){
                _snapshotIds.remove({_id:myDoc._id});
            }
        }
    }
} );


var finalCount = _thingSnapshots.find({}).count();
var refCount   = count-verifCount;
var end = new Date();
var endTime = end.getMilliseconds();
var prcTime = endTime - iniTime;
//Verification Script
if(refCount==finalCount){
    print("RESULT("+ini+"-"+end+">"+prcTime+")-("+timeValueTimeZoneIni+"-"+timeValueTimeZoneEnd+"): Snapshots:" + count+","+"Snapshots Deleted:" + verifCount+">"+"Calculated:" +refCount +",Real:"+ finalCount+">OK");
}else{
    print("RESULT("+ini+"-"+end+">"+prcTime+")-("+timeValueTimeZoneIni+"-"+timeValueTimeZoneEnd+"): Snapshots:" + count+","+"Snapshots Deleted:" + verifCount+">"+"Calculated:" +refCount +",Real:"+ finalCount+">ERROR");
}
