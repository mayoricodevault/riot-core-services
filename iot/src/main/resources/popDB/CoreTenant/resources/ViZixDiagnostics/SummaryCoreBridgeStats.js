/**
 * @author  Ruth Chirinos
 * @date    10/07/2017
 * @description Summarized stats of Core Bridge by tenant VIZIX-6137
 *
 */
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
    table.title = "Summary Statistic Core Bridge By Tenant";
    table.labelX = "Attributes";
    table.labelY = "Tenant Group";

    table.columnNames = ["que_pop_count", "que_size_to", "que_size_blog", "que_size_ooo", "que_idle_count", "que_period_to"];
    table.rowNames = [];
    table.data =[];
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
    var matchCondition ={};
    matchCondition["$match"] = andCondition;

    var aggregateValues ={};
    if(groupId & thingTypeId & serial) {
        aggregateValues["$match"] = matchCondition;
    }
    /*aggregateValues["$project"] = {
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
     };*/

    var groupValues = {};
    groupValues["_id"]="$value.groupName";
    var groupCondition = {_id:"$value.groupName",
        que_pop_count: { $sum: "$value.que_pop_count.value" },
        que_size_to: { $sum: "$value.que_size_to.value" },
        que_size_blog: { $sum: "$value.que_size_blog.value" },
        que_size_ooo: { $sum: "$value.que_size_ooo.value" },
        que_idle_count: { $sum: "$value.que_idle_count.value" },
        que_period_to: { $sum: "$value.que_period_to.value" } };

    aggregateValues["$group"] = groupCondition;

    db.thingSnapshots.aggregate(
        [
            aggregateValues,
            {"$sort": {"time": 1}},
        ] , {allowDiskUse: true}
    ).forEach(function (x) {
        if(table.rowNames) {
            table.rowNames.push(x._id)
        }
        var res = [];
        res.push(x.que_pop_count);
        res.push(x.que_size_to);
        res.push(x.que_size_blog);
        res.push(x.que_size_ooo);
        res.push(x.que_idle_count);
        res.push(x.que_period_to);
        table.data.push(res);
    });
    return table;
}