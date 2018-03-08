/**
 *
 * Example RAW Mongo DB query showing the expected properties in the response javascript object
 *
 */
function (options) {
    var table = {};

    // required
    table.options = JSON.stringify(options);

    // optional, but recommended
    table.title = "Example #1a";

    // optional
    table.labelX = "Words";

    // optional
    table.labelY = "Letters";

    // optional
    table.labelZ = "Count";

    // optional
    table.columnNames = ["One", "Two", "Three", "Four"];

    // optional
    table.rowNames = ["Alpha", "Bravo", "Charlie", "Delta"];

    // required
    table.data = [[1, 2, 3, 4], [4, 5, 6, 7], [7, 8, 9, 10], [10, 11, 12, 13]];

    return table;
}

