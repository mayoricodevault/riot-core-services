/**
 *
 * Displays a list differences between last value and last snapshot
 * @author  David Bascope Collao
 * @date    09/07/2016
 * @version 4.3.0_RC11
 *
 */

function (options) {

//Function to paginate Results
    function paginationData(options, result) {
        var pages = result.length / options["pageSize"];
        var resto = pages % 1;
        if (resto > 0) {
            pages = pages + 1;
        }
        var ini = (options["pageNumber"] - 1 ) * options["pageSize"];
        var end = ini + ( options["pageSize"] - 1 );
        for (var x = 0; x < result.length; x++) {
            if ((x >= ini) && (x <= end)) {
                table.data.push(result[x]);
            }
        }
    }

    var table = {};

    /******************Report Parameters**********************/
    var iniDate = options.dataTimeSeries.startDate; //ISODate("2016-08-30T00:00:00.000Z");
    var endDate = options.dataTimeSeries.endDate; //ISODate("2016-09-02T23:59:59.999Z");
    var ttCode = "default_rfid_thingtype";
    var limit = 100;
    var thingTypeUDFs = ["zone", "logicalReader"];
    /******************Report Parameters**********************/

        // optional, but recommended
    table.title = "Detailed Check";

    // optional
    table.labelX = "Check";

    // optional
    table.labelY = "UDFs";

    table.options = JSON.stringify(options);

    var colHeader = ["Serial Number", "UDF", "Thing value", "Last Snapshot value"];
    var output = [];

    var query = {};
    query.thingTypeCode = ttCode;
    if (iniDate != null && endDate != null){
        query.createdTime = {};
        query.createdTime["$gte"] = iniDate;
        query.createdTime["$lte"] = endDate;
    }

    db.things.find(query, {
                       "serialNumber": 1,
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
        .limit(limit).forEach(function (thing) {
                                  var keyCount = 0;
                                  var lastSnapshot = null;
                                  var existsLast = false;
                                  try {
                                      db.thingSnapshots.find({"value._id": thing._id}).sort({"time": -1}).limit(1).forEach(function (snapShot) {
                                          lastSnapshot = snapShot.value;
                                          existsLast = true;
                                      });

                                      for (var key in thing) {
                                          if (existsLast && Object.prototype.toString.call(thing[key]) == "[object BSON]" && lastSnapshot.hasOwnProperty(key) ) {
                                              for (var keyVal in thing[key]) {
                                                  if (thing[key][keyVal] != null && lastSnapshot[key][keyVal]
                                                                                    != null) {
                                                      if (thing[key][keyVal].toString()
                                                          !== lastSnapshot[key][keyVal].toString()) {
                                                          output.push([thing.serialNumber,
                                                                   key + "." + keyVal,
                                                                   thing[key][keyVal],
                                                                   lastSnapshot[key][keyVal]]);
                                                      }
                                                  } else {
                                                      if (thing[key][keyVal] != null) {
                                                          if (thing[key][keyVal].toString() != "null") {
                                                              output.push([thing.serialNumber,
                                                                       key + "." + keyVal,
                                                                       thing[key][keyVal], ""]);
                                                          }
                                                      } else {
                                                          if (lastSnapshot[key][keyVal] != null
                                                              && lastSnapshot[key][keyVal].toString()
                                                                 != "null") {
                                                              output.push([thing.serialNumber,
                                                                       key + "." + keyVal, "",
                                                                       lastSnapshot[key][keyVal]]);
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
                                                              output.push([thing.serialNumber, key + "." + keyUDF,
                                                                       thing[key]["value"][keyUDF],
                                                                       lastSnapshot[key]["value"][keyUDF]]);
                                                          }
                                                      } else {
                                                          if (thing[key]["value"][keyUDF] != null) {
                                                              if (thing[key]["value"][keyUDF].toString != "null") {
                                                                  output.push([thing.serialNumber, key + "." + keyUDF,
                                                                           thing[key]["value"][keyUDF], ""]);
                                                              }
                                                          } else {
                                                              if (lastSnapshot[key]["value"][keyUDF] != null
                                                                  && lastSnapshot[key]["value"][keyUDF].toString != "null") {
                                                                  output.push([thing.serialNumber, key + "." + keyUDF, "",
                                                                           lastSnapshot[key]["value"][keyUDF]]);
                                                              }
                                                          }
                                                      }
                                                  }
                                              }
                                          }
                                      }
                                  }
                                  catch
                                      (err) {
                                      print("ERR:" + err);
                                  }
                              }
    )
    ;

// optional
    table.columnNames = colHeader;

// required
    table.data = [];

    table.totalRows = output.length;
    if (options.pageSize != -1) {
        paginationData(options, output);
    } else {
        table.data = output;
    }
    return table;
}
