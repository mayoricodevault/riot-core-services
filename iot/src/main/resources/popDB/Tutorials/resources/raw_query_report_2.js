/**
 *
 * Example RAW Mongo DB query
 *
 */
function (options) {
    var table = {};
    // required
    table.options = JSON.stringify(options);
    // optional, but recommended
    table.title = "List of Things";

    var dbObject = db.getCollection('things').find();

    if ((options != null ) && (Object.keys(options).length > 0 ) && options.pageSize != -1) {
        var skip = options.pageSize * (options.pageNumber - 1);
        var limit = options.pageSize;
        // required for pagination
        table.totalRows = dbObject.count();
        var data = dbObject.sort({thingTypeName: 1}).skip(skip).limit(limit).map(
            function (x) {
                return [x.time,
                    formatDate(x.time), // date format and timezone of user
                    moment(x.time).tz('Asia/Tokyo').format(options.timeZoneConfigurationdateFormatConfiguration), //example with moment.js
                    x.thingTypeName, x.serialNumber, x.name];
            }
        );
        table.data = data;
    } else {
        var data = dbObject.sort({thingTypeName: 1}).map(
            function (x) {
                return [x.time,
                    formatDate(x.time),  // date format and timezone of user
                    moment(x.time).tz('Asia/Tokyo').format(options.timeZoneConfigurationdateFormatConfiguration), //example with moment.js
                    x.thingTypeName, x.serialNumber, x.name];
            }
        );
        table.data = data;
    }

    // optional
    table.columnNames = ["Default", "Time ("+options.timeZoneConfiguration+")", "Time (Asia/Tokyo)","Type", "SerialNumber", "Name"];

    return table;
}