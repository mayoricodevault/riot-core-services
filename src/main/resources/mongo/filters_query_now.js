/**
 *
 * Example Filters Query (Now)
 *
 *
 */
function (options) {
    var table = {};
    var query = {};
    var output = [];

    /*****************Filters***************/
    var tenantGroupQuery = options.filtersExtended['Tenant Group'].query;
    var shiftQuery = options.filtersExtended['shift'].query;
    var thingTypeQuery = options.filtersExtended['Thing Type'].query;
    /*****************Filters***************/
    query["$and"]=[{}];
    /*************query********************/
    query["$and"].push(tenantGroupQuery);
    query["$and"].push(shiftQuery);
    query["$and"].push(thingTypeQuery);
    /*************query*******************/
    db.things.find(query).forEach(
        function (thing) {
            output.push([thing.name, thing.thingTypeName, thing.groupName]);
        }
    );
    table.title = "Example Filters Query (Now)";
    table.columnNames = ["Name", "Thing Type", "Group Name"];
    table.totalRows = output.length;
    table.data = output;
    return table;
}