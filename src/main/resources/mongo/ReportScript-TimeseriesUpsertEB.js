/**
 * @author  Angel Chambi Nina
 * @date    06/16/2016
 *
 */
function (options) {

    function timestampToHHMM(timestamp) {
        function pad(s) {
            return (s < 10) ? '0' + s : s;
        }

        var d = new Date(timestamp);
        return formatAMPM(d);
    }
    
    Array.prototype.contains = function (obj) {
        var i = this.length;
        while (i--) {
            if (this[i] === obj) {
                return true;
            }
        }
        return false;
    };
    
    function formatAMPM(date) {
        var hours = date.getHours();
        var minutes = date.getMinutes();
        var ampm = hours >= 12 ? 'PM' : 'AM';
        hours = hours % 12;
        hours = hours ? hours : 12; // the hour '0' should be '12'
        minutes = minutes < 10 ? '0'+minutes : minutes;
        var strTime = hours + ':' + minutes + ' ' + ampm;
        return date.toLocaleDateString() +" "+ strTime; 
    }
    
    /*Default Filters*/
    /* the time bucket size, in minutes*/
    var window          = (options.filters && options.filters["Window Size (minutes)"]) ? parseInt(options.filters["Window Size (minutes)"]) : 1;
    /* set to your local UTC offset*/
    var utcOffset       = (options.filters && options.filters["UTC Offset (+/- hours)"]) ? parseInt(options.filters["UTC Offset (+/- hours)"]) : -4; //-4 for Bolivia
    var numberOfHours   = (options.filters && options.filters["History (hours)"]) ? parseInt(options.filters["History (hours)"]) : 6;
    var now             = new Date();
    var start           = new Date(Math.floor(now.getTime() / 60000) * 60000 - numberOfHours * 3600 * 1000);
    var end             = now;
    var thingTypeId   = (options.filters && options.filters["Thing Type"]) ? parseInt(options.filters["Thing Type"]) : 1;

    //var start = ISODate("2016-07-31 00:00:00.000");  // Add utcOffset to get offset time 
    //var end   = ISODate("2016-07-31 05:00:00.000");  // Add utcOffset to get offset time 
    
    var table = {};
    table.totalRows = 1;
    table.options = options;
    table.title = "ViZix Timeseries Upsert Rate, by Source, tsEdgeIn Timestamp";
    table.labelX = "Upserts/second";
    table.labelY = "Time (UTC" + utcOffset + ")";

    table.columnNames = [];
    table.rowNames = [];
    var result = {};

    db.thingSnapshots.aggregate(
        [
            {
                "$match": {
                    "value.thingTypeId":      thingTypeId, 
                    /*"value.tsEdgeIn.value":     {"$gte": start, "$lte": end},*/
                    "time":                     {"$gte": start, "$lte": end}, 
                    "value.source.value" :      { "$exists": true }
                }
            },
            {"$project" : {
                "_id" : "$_id",
                "serialNumber" : "$value.serialNumber",
                "source" :  "$value.source.value",                                 
                "time"          :   {
                                            "$cond": [
                                                {
                                                    "$and": [
                                                        {"$ne": ["$value.tsEdgeIn.value", null]},
                                                        {"$ne": ["$value.tsEdgeIn.value", undefined]}
                                                    ]
                                                }, 
                                                {"$add": [new Date(0),"$value.tsEdgeIn.value"]}, 
                                                "$value.modifiedTime"]
                                        }
            }},
            {"$sort": {"time": 1}},
            {
                "$group": {
                    "_id": {
                        "range": {"$divide": [{"$subtract": [{"$minute": "$time"}, {"$mod": [{"$minute": "$time"}, window]}]}, window]},                        
                        "hour": {"$hour": "$time"},
                        "day": {"$dayOfMonth": "$time"},
                        "month": {"$month": "$time"},
                        "year": {"$year": "$time"},                        
                        "source": "$source"
                    },
                    "count": {"$sum": 1}
                }
            },
            {"$sort": {"_id.year": 1, "_id.month": 1, "_id.day": 1, "_id.hour": 1, "_id.range": 1}}
        ],
        {allowDiskUse: true}
    ).forEach(function (x) {

        var minStart = (x._id.range * window);
        if (minStart.toString().length == 1) {
            minStart = "0" + minStart;
        }
        var d = new Date(x._id.year, x._id.month, x._id.day, x._id.hour, minStart, 0, 0);

        //rowHeader.push(date);
        var rowHeader = timestampToHHMM(d.getTime() + utcOffset * 3600 * 1000);
        if (!table.rowNames.contains(rowHeader)){
            table.rowNames.push(rowHeader);
        }
        var colHeader = x._id.source;
     
        if (!table.columnNames.contains(colHeader)){
            table.columnNames.push(colHeader);
        }

        var value = parseFloat((NumberInt(x.count) / (window * 60))).toFixed(2);
        
        if(result[colHeader]){
            result[colHeader][rowHeader] = value;
        }else{
            var row = {};
            row[rowHeader] = value;
            result[colHeader] = row;    
        }
    });

    table.data = [];
    
    for (rowKey = 0; rowKey < table.rowNames.length; rowKey++) {
        var rowResult = [];
        var total = 0.0;
        for (colKey = 0; colKey < table.columnNames.length; colKey++) {
            if(result[table.columnNames[colKey]][table.rowNames[rowKey]]){
                total += parseFloat(result[table.columnNames[colKey]][table.rowNames[rowKey]]);
                rowResult.push(result[table.columnNames[colKey]][table.rowNames[rowKey]]);
            }else{
                rowResult.push(0);
            }
        }
        rowResult.push(total.toFixed(2));
        table.data.push(rowResult)
    }

    table.data.splice(-1, 1);
    table.columnNames.push("Total");    
    return table;
}