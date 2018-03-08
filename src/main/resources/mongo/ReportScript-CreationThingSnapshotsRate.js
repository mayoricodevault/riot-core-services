/**
 * Created by cvertiz on 6/16/2016.
 */

/**
 *
 * Calculates the conversion rate calculated by dividing DressingRoom and POS.
 * @author  Cristian Vertiz
 * @date    05/05/2016
 * @version 4.1.4_tasks
 *
 */
function (options) {

    var window = 5;
    var start = ISODate("2016-06-16 00:00:00.000BOT");
    var end = ISODate("2016-06-16 18:00:00.000BOT");


    var table = new Object();

    // required
    table.options = options;

    // optional, but recommended
    table.title = "Thing Creation Rate (windows of " + window + " from " + start + " to" + end + ")";

    // optional
    table.labelX = "New Thing Rate";

    // optional
    table.labelY = "Date Range";

    var colHeader = ["Count","per/sec"];
    var rowHeader = [];



    var result = db.thingSnapshots.aggregate(
        [
            {"$match": {time: {$gte: start, $lte: end}}},
            {"$sort": {time: 1}},
            {
                "$group": {
                    "_id": {
                        "range": {"$divide": [{"$subtract": [{"$minute": "$time"}, {"$mod": [{"$minute": "$time"}, window]}]}, window]},
                        "minute": {"$minute": "$time"},
                        "hour": {"$hour": "$time"},
                        "day": {"$dayOfMonth": "$time"},
                        "month": {"$month": "$time"},
                        "year": {"$year": "$time"},
                        "time": "$time",
                        "serialNumber": "$value.serialNumber"
                    }
                }
            },
            {
                "$group": {
                    "_id": {
                        "range": "$_id.range",
                        "hour": "$_id.hour",
                        "day": "$_id.day",
                        "month": "$_id.month",
                        "year": "$_id.year"
                    },
                    "count": {"$sum": 1}
                }
            },
            {"$sort" : {"_id.year" : 1, "_id.month" : 1, "_id.day" : 1, "_id.hour" : 1, "_id.range" : 1}}
        ]
    ).map(function (x) {

        var minStart = (x._id.range * window);
        if (minStart.toString().length == 1) {
            minStart = "0" + minStart;
        }
        var minEnd = ((x._id.range + 1) * window) - 1;
        if (minEnd.toString().length == 1) {
            minEnd = "0" + minEnd;
        }
        var date = x._id.year + "-" + x._id.month + "-" + x._id.day + " [from " + x._id.hour + ":" + minStart + ":00" + " to " + x._id.hour + ":" + minEnd + ":59" + "]";
        rowHeader.push(date);
        return [NumberInt(x.count),(NumberInt(x.count)/(window*60)).toFixed(2) ];
    });

    // optional
    table.columnNames = colHeader;

    // optional
    table.rowNames = rowHeader;

    // required
    table.data = result;

    return table;
}