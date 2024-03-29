/**
 * Report to get the quantity of UPCs which have almost one Item with Status Free, the groupping is By Zone Type
 * @company Mojix
 * @author  Ruth Chirinos
 * @Modified AngelChambi (add summary Row)
 * @date    24/06/2016
 * @version develop (4.3)
 */
function (options) {
    /************************************************************/
    var store     = "Derby";                  //Name of the Store
    var storeUDF  = "zone.value.facilityMap"; //UDF to evaluate Store
    var thingTypeIdItem  = 4;                 //ID of the Thing Type ID of the Item
    /************************************************************/
    //DO NOT CHANGE the code above

    var table = new Object();
    // required
    table.options = options;
    // optional, but recommended
    table.title = "UPC Free Count By Zone";
    // optional
    table.labelX = "Zone Types";
    // optional
    table.columnNames = [];
    table.data = [];

    /* Getting Zone Types */
    var queryMatch = {};
    queryMatch["thingTypeId"]= thingTypeIdItem;
    queryMatch[storeUDF]     = store;
    queryMatch["zone"]       = {$exists:true};
    var zoneTypeList = db.things.aggregate([
                                               { $match: queryMatch }
                                               ,{ $group: {_id: '$zone.value.zoneType' }}
                                               ,{ $sort : {_id: -1}}
                                           ]);

    //Initialize headers
    var zoneTypesJson= {};
    for(var i = 0 ; i< zoneTypeList["_batch"].length ; i++ ){
        var data = zoneTypeList["_batch"][i] ;
        zoneTypesJson[data["_id"]] = 0;
    }

    //Query general
    var queryMatch = {};
    queryMatch["thingTypeId"]= thingTypeIdItem;
    queryMatch[storeUDF]     = store;
    queryMatch["Status.value.name"]      = "Free";
    queryMatch["UPCCode.value"] = {$exists:1};
    var queryGroupOne   = {};
    queryGroupOne["upc"]= "$UPCCode.value";
    queryGroupOne["zoneType"]= "$zone.value.zoneType";

    var resultSum = [];
    db.things.aggregate([
                            {$match:queryMatch},
                            {$group:{ _id:queryGroupOne,count:{$sum:1}}},
                            {$sort :{ "_id.zoneType": 1 }}
                        ]).forEach(function(x){
        for(var a in zoneTypesJson){
            if( x["_id"]["zoneType"] == a ){
                zoneTypesJson[a] = zoneTypesJson[a] + 1;
            }
        }
    });

    var resultSum = [];
    var totalSum = 0;
    for(var a in zoneTypesJson){
        table.columnNames.push(a);
        totalSum = totalSum + zoneTypesJson[a];
        resultSum.push(zoneTypesJson[a]);
    }
    resultSum.unshift(totalSum);
    table.data.push(resultSum);
    table.columnNames.unshift("TOTAL");
    return table;
} 