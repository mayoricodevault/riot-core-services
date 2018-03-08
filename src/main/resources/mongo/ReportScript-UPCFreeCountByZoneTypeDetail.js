/**
 * Report to get UPC Free Count By Zone, for each UPC
 * @company Mojix
 * @author  Ruth Chirinos
 * @Modify  Angel Chambi (Add total row and total column)
 * @date    24/06/2016
 * @version develop (4.3)
 */
function( options ) {
    /************************************************************/
    var store = "Derby";                     //Name of the Store
    var storeUDF = "zone.value.facilityMap"; //UDF to evaluate Store
    var thingTypeIdItem = 4;                 //ID of the Thing Type ID of the Item
    /************************************************************/
    //DO NOT CHANGE the code above

    var lastUPCDoc  = {};
    var table       = new Object();
    table.options   = options;//{"pageNumber":1,"pageSize":-1}; printjson(table.options);
    table.title     = "UPC Free Count By Zone";
    table.labelX    = "Zone Types";
    table.columnNames = [];
    table.rowNames  = [];
    table.data      = [];
    var resultOne   = [];
    var preResult   = [];
    var preRowNames = [];

    //Function to paginate Results
    function paginationData(options, preResult){
        var pages = preResult.length/options["pageSize"];
        var resto = pages % 1;
        if(resto > 0){ pages = pages + 1; }
        var ini   = (options["pageNumber"] - 1 ) * options["pageSize"] ;
        var end   = ini + ( options["pageSize"] - 1 );
        for(var x = 0 ; x < preResult.length ; x++){
            if( (x >= ini) && (x <= end) )
            {
                table.rowNames.push(preRowNames[x]);
                table.data.push(preResult[x]);
            }
        }
    }

    //LOGIC of the Table Script
    /* Getting Zone Types */
    var queryMatch = {};
    queryMatch["thingTypeId"] = thingTypeIdItem;
    queryMatch[storeUDF] = store;
    queryMatch["zone"] = {$exists: true};
    var zoneTypeList = db.things.aggregate([
                                               {$match: queryMatch}
                                               , {$group: {_id: '$zone.value.zoneType'}}
                                               , {$sort: {_id: 1}}
                                           ]);

    //Initialize headers
    for (var i = 0; i < zoneTypeList["_batch"].length; i++) {
        var data = zoneTypeList["_batch"][i];
        table.columnNames.push(data["_id"]);
    }

    //Get rows total.
    var queryMatch = {};
    queryMatch["thingTypeId"] = thingTypeIdItem;
    queryMatch[storeUDF] = store;
    queryMatch["Status.value.name"]      = "Free";
    queryMatch["UPCCode.value"] = {$exists:1};

    var queryGroupOne = {};
    queryGroupOne["zoneType"] = "$zone.value.zoneType";
    var totalZones ={};

    db.things.aggregate([
        {$match: queryMatch},
        {$group: {_id: queryGroupOne, count: {$sum: 1}}},
        {$sort: {"_id.upc": 1, "_id.zoneType": 1}}
    ]).forEach(function(zones){

        totalZones["upc"] = "TOTAL";
        totalZones[zones["_id"]["zoneType"]] = zones["count"];
    });
    var totalZoneResultRow = 0;
    var totalResultZoneOneVar = 0;
    var countTotalZone        = 0;
    var resultSumZone         = [];
    preRowNames.push("TOTAL");
    for (var a in totalZones) {
        totalResultZoneOneVar++
    }

    for (var c = 0; c < table.columnNames.length; c++) { //headers
        countTotalZone = 0;
        for (var a in totalZones) { //Keys
            if (a == table.columnNames[c]) {
                resultSumZone.push(totalZones[a]);
                totalZoneResultRow = totalZoneResultRow + totalZones[a];
                break;
            }
            countTotalZone++;
        }
        if (countTotalZone == totalResultZoneOneVar) {
            resultSumZone.push(0);
        }
    }
    resultSumZone.unshift(totalZoneResultRow);
    preResult.push(resultSumZone);

    //Query general
    var queryMatch = {};
    queryMatch["thingTypeId"] = thingTypeIdItem;
    queryMatch[storeUDF] = store;
    queryMatch["Status.value.name"]      = "Free";
    queryMatch["UPCCode.value"] = {$exists:1};
    var queryGroupOne = {};
    queryGroupOne["upc"] = "$UPCCode.value";
    queryGroupOne["zoneType"] = "$zone.value.zoneType";
    db.things.aggregate([
                            {$match: queryMatch},
                            {$group: {_id: queryGroupOne, count: {$sum: 1}}},
                            {$sort: {"_id.upc": 1, "_id.zoneType": 1}}
                        ]).forEach(function (x) {
        if (lastUPCDoc["upc"] == x["_id"]["upc"]) {
            if (lastUPCDoc[x["_id"]["zoneType"]]) {
                lastUPCDoc[x["_id"]["zoneType"]] = ( lastUPCDoc[x["_id"]["zoneType"]] ) + x["count"];
            } else {
                lastUPCDoc[x["_id"]["zoneType"]] = x["count"];
            }
        } else {
            if ((lastUPCDoc != null ) && (Object.keys(lastUPCDoc).length > 0 )) {
                resultOne.push(lastUPCDoc);
            }
            lastUPCDoc = {};
            lastUPCDoc["upc"] = x["_id"]["upc"];
            lastUPCDoc[x["_id"]["zoneType"]] = x["count"];
        }
    });
    if ((lastUPCDoc != null ) && (Object.keys(lastUPCDoc).length > 0 )) {
        resultOne.push(lastUPCDoc);
    }

    //Query general
    var queryTotalZonesMatch = {};
    queryTotalZonesMatch["thingTypeId"] = thingTypeIdItem;
    queryTotalZonesMatch[storeUDF] = store;
    queryTotalZonesMatch["Status.value.name"] = "Free";

    //Format Report
    for (var v = 0; v < resultOne.length; v++) { //Each UPC
        var resultSum    = [];
        var resultOneVar = resultOne[v];
        var totalResultOneVar = 0;
        var count        = 0;
        preRowNames.push((resultOne[v])["upc"]);
        for (var a in resultOneVar) {
            totalResultOneVar = totalResultOneVar + 1;
        }
        var totalResultRow = 0;
        for (var c = 0; c < table.columnNames.length; c++) { //headers
            count = 0;
            for (var a in resultOneVar) { //Keys
                if (a == table.columnNames[c]) {
                    resultSum.push(resultOneVar[a]);
                    totalResultRow = totalResultRow + resultOneVar[a];
                    break;
                }
                count = count + 1;
            }
            if (count == totalResultOneVar) {
                resultSum.push(0);
            }
        }
        resultSum.unshift(totalResultRow);
        preResult.push(resultSum);
    }

    table.columnNames.unshift("TOTAL");
    table.totalRows = preResult.length;
    if( ( table.options["pageNumber"] == 1) && ( table.options["pageSize"] == -1) ){
        table.rowNames = preRowNames;
        table.data     = preResult;
    }else{
        paginationData(table.options, preResult)
    }
    return table;
} 