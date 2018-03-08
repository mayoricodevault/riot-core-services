/**
 *
 * Displays a date from mongodb in different timezones
 * @author  David Bascope Collao
 * @date    03/09/2017
 * @version 5.0.0_RC03
 *
 */

function (options) {
    var table = {};

    // required
    table.options = JSON.stringify(options);

    // optional, but recommended
    table.title = "TimeZone example";

    // optional
    table.labelX = "Dates";

    // optional
    table.rowNames = ['Default', 'Australia/Sydney', 'Asia/Tokyo', 'America/Los_Angeles', 'America/La_Paz'];

    // optional
    table.columnNames = ["Value"];

    var x = db.things.find({}, {"time": 1}).limit(1).map(function (x) {
        return x;
    })
    var dat = x[0].time
    // required
    table.data = [[dat], [moment(dat).tz('Australia/Sydney').format()], [moment(dat).tz('Asia/Tokyo').format()], [moment(dat).tz('America/Los_Angeles').format()], [moment(dat).tz('America/La_Paz').format()]];

    return table;
}