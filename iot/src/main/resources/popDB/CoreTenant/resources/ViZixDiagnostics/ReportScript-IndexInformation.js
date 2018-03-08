/**
 * @author  Angel Chambi Nina
 * @date    06/22/2017
 *
 */
function (options) {


    /*var options = {
     "pageSize": 50,
     "pageNumber": 1,
     "dataTimeSeries": {
     "startDate": null,
     "endDate": null,
     "now": "2017-03-09T18:28:01.552Z"
     },
     "filters": {
     "Window(Minutes)": "1",
     "UtcOffset": "0",
     "NumberOfHours": "6"
     },
     "export": false,
     "comment": "nameReport:[ACHAMBI]ViZix tsCoreIn Upsert(AVG)/s I,idReport:103,typeReport:mongo,FILTERS::relativeDate,Operqator:, Value: NOW,CountType,Operqator:~, Value: 1,CountType,Operqator:~,Value: 0,CountType,Operqator:~, Value: 6,"
     };*/


    function timestampToHHMM(timestamp) {
        return formatAMPM(new Date(timestamp));
    }

    Array.prototype.contains = function (obj) {
        let i = this.length;
        while (i--) {
            if (this[i] === obj) {
                return true;
            }
        }
        return false;
    };

    function formatAMPM(date) {
        let hours = date.getHours();
        let minutes = date.getMinutes();
        let amPm = hours >= 12 ? 'PM' : 'AM';
        hours = hours % 12;
        hours = hours ? hours : 12; // the hour '0' should be '12'
        minutes = minutes < 10 ? '0' + minutes : minutes;
        let strTime = hours + ':' + minutes + ' ' + amPm;
        return date.toLocaleDateString() + " " + strTime;
    }

    /*Default Filters*/
    /* set to your local UTC offset*/
    let utcOffset = (options.filters && options.filters["UTC Offset (+/- hours)"]) ? parseInt(options.filters["UTC Offset (+/- hours)"]) : -4; //-4 for Bolivia

    let table = {};
    table.totalRows = 1;
    table.options = JSON.stringify(options);
    table.title = "ViZix IndexStats Information";
    table.labelX = "Creation Date (UTC" + ((utcOffset >= 0) ? "+" : "") + utcOffset + ")";
    table.labelY = "Number of operations";

    table.columnNames = [];
    table.rowNames = [];
    let result = {};

    db.getCollection('vizixIndexInformation').find({}).forEach(function (x) {
        let colHeader = x._id;

        if (!table.columnNames.contains(colHeader)) {
            table.columnNames.push(colHeader);
        }

        for (let index = 0; index < x.statsLog.length; index++) {
            let rowHeader = timestampToHHMM(x.statsLog[index].creationDate.getTime() + (utcOffset * 3600 * 1000));
            if (!table.rowNames.contains(rowHeader)) {
                table.rowNames.push(rowHeader);
            }
            let value = x.statsLog[index].numberQueriesDone;

            if (result[colHeader]) {
                result[colHeader][rowHeader] = value;
            } else {
                let row = {};
                row[rowHeader] = value;
                result[colHeader] = row;
            }
        }
    });

    table.data = [];
    for (rowKey = 0; rowKey < table.rowNames.length; rowKey++) {
        let rowResult = [];
        let total = NumberLong(0);
        for (colKey = 0; colKey < table.columnNames.length; colKey++) {
            if (result[table.columnNames[colKey]][table.rowNames[rowKey]]) {
                total += result[table.columnNames[colKey]][table.rowNames[rowKey]];
                rowResult.push(result[table.columnNames[colKey]][table.rowNames[rowKey]]);
            } else {
                rowResult.push(NumberLong(0));
            }
        }
        rowResult.push(total);
        table.data.push(rowResult)
    }
    table.columnNames.push("Total");
    return table;
}