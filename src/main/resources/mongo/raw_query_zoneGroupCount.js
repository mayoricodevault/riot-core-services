/************************************
 * Report to zone group count
 * This script gets a count of things by Zone and by Date
 *************************************/
function( options )
{
    var table = new Object();

    // required
    table.options = options;

    // optional, but recommended
    table.title = "Zone Group Count";

    // optional
    table.labelX = "Count";

    // optional
    table.labelY = "Zone Group";

    var labelsAbove = [];

    var start = new Date("01/05/2016 00:00:00 GMT-0800").getTime();
    var numberOfDays = 3;

    var out = {};
    var zones = [];
    var dates = [];
    var header = "ZoneGroup";
    for (var i = 1; i <= numberOfDays; i++) {
        var utcTime = new Date(start);
        var dateUtcTime = utcTime.getTime() + utcTime.getTimezoneOffset() * 60000;

        var date = ((new Date(dateUtcTime)).getMonth() + 1) + "/" + (new Date(dateUtcTime)).getDate() + "/" + ((new Date(dateUtcTime)).getFullYear());
        labelsAbove.push(date);
        var count = 0;
        var end = start + 86400000;
        if (i == 1) {
            db.thingSnapshots.distinct("value.zone.value.zoneGroup").map(
                function (z) {
                    zones.push(z);
                }
            )
            zones.push("Unknown")

            var start2 = start;
            for (var j = 1; j <= numberOfDays; j++) {
                var utcTimeAux = new Date(start2);
                var dateUtcTime2 = utcTimeAux.getTime() + utcTimeAux.getTimezoneOffset() * 60000;
                var dAux = ((new Date(dateUtcTime2)).getMonth() + 1) + "/" + (new Date(dateUtcTime2)).getDate() + "/" + ((new Date(dateUtcTime2)).getFullYear());
                dates.push(dAux);
                var start2 = start2 + 86400000;
            }
            for (var z in zones) {
                var a = {};
                a["name"] = zones[z];
                for (var d in dates) {
                    a[dates[d]] = "0";
                }
                out[zones[z]] = a;
            }

        }
        var ids = db.thingSnapshotIds.find(
            {"blinks": {"$elemMatch": {"time": {"$lte": end - 1, "$gte": start}}}},
            {"blinks": {"$elemMatch": {"time": {"$lte": end - 1, "$gte": start}}}}).map(
            function (x) {
                return x.blinks[0].blink_id;
            }
        );
        var dummy = db.thingSnapshots.aggregate([
            {
                "$match": {"$and": [{"_id": {"$in": ids}}, {"value.zone": {"$exists": true}}]}
            },
            {
                "$group": {
                    "_id": "$value.zone.value.zoneGroup",
                    "count": {"$sum": 1}
                }
            }

        ]);
        if (dummy.result != null) {
            dummy = dummy.result;
        }
        dummy.map(function (a) {
            if (out[a._id] != null) {
                out[a._id]["name"] = a._id;
                out[a._id][date] = a.count;
            } else {
                out["Unknown"]["name"] = "Unknown";
                out["Unknown"][date] = a.count;
            }

        });


        var headerRow = [];
        var result = [];
        if (i == numberOfDays) {
            for (z in zones) {
                var outStr = out[zones[z]]["name"];
                headerRow.push(outStr);
                var dataOne = [];
                for (var d in dates) {
                    dataOne.push(out[zones[z]][dates[d]]);
                }
                result.push(dataOne);
            }
        }

        var start = end;
    }

    // optional
    table.columnNames = labelsAbove;

    // optional
    table.rowNames = headerRow;

    // required
    table.data = result;

    return table;
}
