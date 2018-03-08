/**
 *
 * Example Filters Query (Custom)
 *
 *
 */
function (options) {
    var table = {};
    var labelRows = [];
    /******************Filters**********************/
    var tenantGroupQuery = options.filtersExtended['Tenant Group'].query;
    var thingTypeQuery = options.filtersExtended['Thing Type'].query;
    var zoneQuery = options.filtersExtended['zone'].query;
    var shiftQuery = options.filtersExtended['shift'].query;
    var registeredQuery = options.filtersExtended['registered'].query;
    /*****************Filters***************/

    var startDate = options.filtersExtended['Date'].startDate.getTime();  //ISODate("2017-03-01T10:57:05.000-04:00");
    var endDate = options.filtersExtended['Date'].endDate.getTime(); //ISODate("2017-03-24T10:57:07.999-04:00");

    var dbObject = db.getCollection('thingSnapshots').find({
        "$and": [ thingTypeQuery, tenantGroupQuery,zoneQuery, shiftQuery,registeredQuery,
            {
                "_id": {
                    "$in": db.thingSnapshotIds.find({
                        "$and": [{
                            "blinks": {
                                "$elemMatch": {
                                    "time": {
                                        "$gte": startDate,
                                        "$lte": endDate
                                    }
                                }
                            }
                        }]
                    }, {
                        "blinks": {
                            "$elemMatch": {
                                "time": {
                                    "$gte": startDate,
                                    "$lte": endDate
                                }
                            }
                        }
                    }).map(function(_paramToProject) {
                        return _paramToProject.blinks[0].blink_id;
                    })
                }
            }]
    });
    var data = dbObject.map(
        function(y){
            labelRows.push([y.value.name]);
            return [y.value.thingTypeName, y.value.zone.value.name, y.value.registered.value];
        });
    table.data = data;
    table.title = "Example Filters Query (Custom)";
    table.columnNames = [ "Thing Type", "Zone", "Registered"];
    table.rowNames = labelRows;
    return table;
}