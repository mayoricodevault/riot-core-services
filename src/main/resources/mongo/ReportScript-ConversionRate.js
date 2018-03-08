/**
 *
 * Calculates the conversion rate calculated by dividing DressingRoom and POS.
 * @author  Cristian Vertiz
 * @date    05/05/2016
 * @version 4.1.4_tasks
 *
 */
function (options) {

    var table = new Object();

    // required
    table.options = options;

    // optional, but recommended
    table.title = "Conversion Rate";

    // optional
    table.labelX = "Count";

    // optional
    table.labelY = "UDFs";


    var start = new Date("05/03/2015 00:00:00 GMT-0400").getTime();
    var end = new Date("08/08/2017 23:59:59 GMT-0400").getTime();
    var thingType = "Retail.tag"

    var colHeader = ["Day", "DR", "POS", "Converision Rate"];
    var rowHeader = [];

    var ids = db.thingSnapshotIds.find(
        {"blinks": {"$elemMatch": {"time": {"$lte": end, "$gte": start}}}},
        {"blinks": {"$elemMatch": {"time": {"$lte": end, "$gte": start}}}}).map(
        function (x) {
            return x.blinks[0].blink_id;
        });
    var result = db.thingSnapshots.aggregate(
        [
            {"$match": {"$and": [{"_id": {"$in": ids}}]}},
            {
                "$project": {
                    "time": 1,
                    "value": 1,
                    "DRNum": {
                        "$cond": [{"$eq": ["$value.Sale.value", true]}, 1, 0]
                    },
                    "POSNum": {
                        "$cond": [{"$eq": ["$value.Sold.value", true]}, 1, 0]
                    },
                    "timeNum": {
                        "$concat": [
                            {"$substr": [{"$year": "$time"}, 0, 4]},
                            {"$substr": [{"$month": "$time"}, 0, 2]},
                            {"$substr": [{"$dayOfMonth": "$time"}, 0, 2]}
                        ]
                    }
                }
            },
            {
                "$group": {
                    "_id": {
                        "date": {
                            "$concat": [
                                {"$substr": [{"$month": "$time"}, 0, 2]}, "/",
                                {"$substr": [{"$dayOfMonth": "$time"}, 0, 2]}, "/",
                                {"$substr": [{"$year": "$time"}, 0, 4]}
                            ]
                        }, "timeNum": "$timeNum"
                    },
                    "DR": {"$sum": "$DRNum"},
                    "POS": {"$sum": "$POSNum"}
                }
            },
            {
                "$project": {
                    "_id": 1,
                    "DR": 1,
                    "POS": 1,
                    "CR": {
                        "$concat": [
                            {
                                "$substr": [
                                    {
                                        "$multiply": [
                                            {
                                                "$divide": [
                                                    "$POS",
                                                    {"$cond": [{"$eq": ["$DR", 0]}, Number(1), "$DR"]}
                                                ]
                                            }, Number(100)]
                                    }, 0, 10
                                ]
                            }, " %"
                        ]
                    },
                    "timeNum": 1
                }
            },
            {
                "$sort": {"_id.timeNum": 1}
            }
        ]
    ).map(function (x) {
        return [x._id.date, x.DR, x.POS, x.CR];
    });


    // optional
    table.columnNames = colHeader;

    // optional
    //table.rowNames = headerRow;

    // required
    table.data = result;

    return table;
}
