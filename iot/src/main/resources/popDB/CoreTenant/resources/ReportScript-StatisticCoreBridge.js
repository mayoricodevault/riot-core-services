function (options) {

    Array.prototype.contains = function (obj) {
        var i = this.length;
        while (i--) {
            if (this[i] === obj) {
                return true;
            }
        }
        return false;
    };

    var thingTypeId = (options.filters && options.filters["Thing Type"]) ? parseInt(options.filters["Thing Type"]) : null;
    var groupId     = (options.filters && options.filters["Tenant Group"]) ? parseInt(options.filters["Tenant Group"]) : 1;
    var serial      = (options.filters && options.filters["serial"]) ? options.filters["serial"]+"" : null;
    var dateFilter  = options.filtersExtended['Date'].query;
    var timezone    = options.timeZoneConfiguration;
    var dateformat  = options.dateFormatConfiguration;

    var table = {};
    table.totalRows = 1;
    table.options = JSON.stringify(options);
    table.title = "Diagnostic CoreBridge's UDFs";
    table.labelX = "Attributes";
    table.labelY = "Time";

    table.columnNames = [];
    table.rowNames = [];
    var result = {};

    var andVal = [];
    andVal.push(dateFilter);
    if(groupId){
        var group = {};
        group["value.groupId"]=groupId;
        andVal.push(group);
    }
    if(thingTypeId){
        var thingType = {};
        thingType["value.thingTypeId"]=thingTypeId;
        andVal.push(thingType);
    }
    if(serial){
        var serialNumber ={};
        serialNumber["value.serialNumber"]=serial;
        andVal.push(serialNumber);
    }
    var andCondition ={};
    andCondition["$and"] = andVal;

    db.thingSnapshots.aggregate(
        [
            {
                "$match":andCondition
            },
            {
                "$project":{
                    "_id"               : "$_id",
                    "serialNumber"      : "$value.serialNumber",
                    "source"            : "$value.source.value",
                    "que_pop_count"     : "$value.que_pop_count.value",
                    "que_size_to"       : "$value.que_size_to.value",
                    "que_size_blog"     : "$value.que_size_blog.value",
                    "que_size_ooo"      : "$value.que_size_ooo.value",
                    "que_idle_count"    : "$value.que_idle_count.value",
                    "que_period_to"     : "$value.que_period_to.value",
                    "time"              : "$value.modifiedTime"
                }
            },
            {"$sort": {"time": 1}},
        ], {allowDiskUse: true}
    ).forEach(function (x) {

        // modified Time
        var d = x.time;

        // var rowHeader = formatDate(d.getTime()); // format date with configuration user or group
        // var rowHeader = moment(d).tz(timezone).format(); // format date with moment.js
        // var rowHeader = moment(d).format(dateformat); // format date with moment.js
        var rowHeader = moment(d).format("'"+dateformat+"'"); // format date with moment.js

        if (!table.rowNames.contains(rowHeader)){
            table.rowNames.push(rowHeader);
        }
        // headers
        var colHeader = "que_pop_count";
        if (!table.columnNames.contains(colHeader)){
            table.columnNames.push(colHeader);
            table.columnNames.push("que_size_to");
            table.columnNames.push("que_size_blog");
            table.columnNames.push("que_size_ooo");
            table.columnNames.push("que_idle_count");
            table.columnNames.push("que_period_to");
        }

        // values
        var valuePOP = x.que_pop_count;
        var valueTO = x.que_size_to;
        var valueBLOG = x.que_size_blog;
        var valueOOO = x.que_size_ooo;
        var valueIdletime = x.que_idle_count;
        var valuePeriodTo = x.que_period_to;

        if(result[colHeader]){
            result[colHeader][rowHeader] = valuePOP;
            result["que_size_to"][rowHeader] = valueTO;
            result["que_size_blog"][rowHeader] = valueBLOG;
            result["que_size_ooo"][rowHeader] = valueOOO;
            result["que_idle_count"][rowHeader] = valueIdletime;
            result["que_period_to"][rowHeader] = valuePeriodTo;
        } else {
            var rowPOP = {};
            rowPOP[rowHeader] = valuePOP;
            var rowTO = {};
            rowTO[rowHeader] = valueTO;
            var rowBLOG = {};
            rowBLOG[rowHeader] = valueBLOG;
            var rowOOO = {};
            rowOOO[rowHeader] = valueOOO;
            var rowIdleTime = {};
            rowIdleTime[rowHeader] = valueIdletime;
            var rowPeriodTo = {};
            rowPeriodTo[rowHeader] = valuePeriodTo;

            result[colHeader] = rowPOP;
            result["que_size_to"] = rowTO;
            result["que_size_blog"] = rowBLOG;
            result["que_size_ooo"] = rowOOO;
            result["que_idle_count"] = rowIdleTime;
            result["que_period_to"] = rowPeriodTo;
        }
    });

    table.data = [];
    for (rowKey = 0; rowKey < table.rowNames.length; rowKey++) {
        var rowResult = [];
        for (colKey = 0; colKey < table.columnNames.length; colKey++) {
            if(result[table.columnNames[colKey]][table.rowNames[rowKey]]){
                rowResult.push(result[table.columnNames[colKey]][table.rowNames[rowKey]]);
            }else{
                rowResult.push(0);
            }
        }
        table.data.push(rowResult)
    }

    // table.data.splice(-1, 1);
    return table;
}