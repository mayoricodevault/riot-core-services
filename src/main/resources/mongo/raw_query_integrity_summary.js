/**
 *
 * Displays a list differences between last value and last snapshot
 *
 */

function (options) {
    var showKey;

    var strSummary = {};

    var table = {};

    table.options = JSON.stringify(options);

    /******************Report Parameters**********************/
    var iniDate = options.dataTimeSeries.startDate; //ISODate("2016-08-30T00:00:00.000Z");
    var endDate = options.dataTimeSeries.endDate; //ISODate("2016-09-02T23:59:59.999Z");
    var ttCode = "default_rfid_thingtype";
    var thingTypeUDFs = ["zone", "logicalReader"];
    /******************Report Parameters**********************/

        // optional, but recommended
    table.title = "Check";

    // optional
    table.labelX = "Result";

    // optional
    table.labelY = "Check";
    var colHeader = ["OK", "ERROR"];

    strSummary["OK"]=0;
    strSummary["ERROR"]=0;


    var query = {};
    query.thingTypeCode = ttCode;
    if (iniDate != null && endDate != null){
        query.createdTime = {};
        query.createdTime["$gte"] = iniDate;
        query.createdTime["$lte"] = endDate;
    }

    db.things.find(query, {
            "doorEvent": 1,
            "eNode": 1,
            "location": 1,
            "locationXYZ": 1,
            "logicalReader": 1,
            "registered": 1,
            "shift": 1,
            "status": 1,
            "zone": 1
        })
        .forEach(function (thing) {


            var count = 0;
            var keyCount = 0;
            var lastSnapshot = null;
            var existsLast = false;
            var prevSnapshot = null;
            var existsPrevious = false;
            try {
                db.thingSnapshots.find({"value._id": thing._id}).sort({"time": -1}).limit(2).forEach(function (snapShot) {
                    if (count == 0) {
                        lastSnapshot = snapShot.value;
                        existsLast = true;
                    } else {
                        prevSnapshot = snapShot.value;
                        existsPrevious = true;
                    }
                    count++;
                });

                var thingCheck = true;
                for (var key in thing) {
                    if (existsLast && Object.prototype.toString.call(thing[key]) == "[object BSON]" && thingTypeUDFs.indexOf(key) >= 0) {
                        showKey = "";
                        for (var keyVal in thing[key]) {
                            if (thing[key][keyVal] != null && lastSnapshot[key][keyVal]
                                != null) {
                                if (thing[key][keyVal].toString()
                                    !== lastSnapshot[key][keyVal].toString()) {
                                    thingCheck = false;
                                    break;
                                }
                            } else {
                                if (thing[key][keyVal] != null) {
                                    if (thing[key][keyVal].toString() != "null") {
                                        thingCheck = false;
                                        break;
                                    }
                                } else {
                                    if (lastSnapshot[key][keyVal] != null
                                        && lastSnapshot[key][keyVal].toString()
                                        != "null") {
                                        thingCheck = false;
                                        break;
                                    }
                                }
                            }
                        }
                        if (thingTypeUDFs.indexOf(key) >= 0) {
                            for (var keyUDF in thing[key]["value"]) {
                                if (thing[key]["value"][keyUDF] != null && lastSnapshot[key]["value"][keyUDF]
                                    != null) {
                                    if (thing[key]["value"][keyUDF].toString()
                                        !== lastSnapshot[key]["value"][keyUDF].toString()) {
                                        thingCheck = false;
                                        break;
                                    }
                                } else {
                                    if (thing[key]["value"][keyUDF] != null) {
                                        if (thing[key]["value"][keyUDF].toString != "null") {
                                            thingCheck = false;
                                            break;
                                        }
                                    } else {
                                        if (lastSnapshot[key]["value"][keyUDF] != null
                                            && lastSnapshot[key]["value"][keyUDF].toString != "null") {
                                            thingCheck = false;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                var countOk = strSummary["OK"];
                var countErr = strSummary["ERROR"];
                if (thingCheck){
                    countOk++;
                    strSummary["OK"] = countOk;
                } else {
                    countErr++;
                    strSummary["ERROR"] = countErr;
                }
            } catch (err) {
                print("ERR:" + err);
            }
        });

    // optional
    table.columnNames = colHeader;

    // required
    var output = [];
    output.push([strSummary["OK"],strSummary["ERROR"]]);
    table.data = output;

    return table;
}