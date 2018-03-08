/**
 *
 * This script has the purpose of show which Zone Types has deficit in their warehouse.
 * In addition, it shows if the backroom (Big warehouse) has enough items to supply Zone Types
 * Type report: Summary, Vertical: Values of a UDF, Horizontal: Zone Types.
 * @author  Ruth Chirinos
 * @date    29/04/2016
 * @version 4.1.4, sv1.0.0
 *
 */
function( options )
{
    var table = new Object();

    // required
    table.options = options;

    // optional, but recommended
    table.title = "Example #1a";

    // optional
    table.labelX = "Zone Types";

    // optional
    table.labelY = "UDF Values";

    // optional
    table.labelZ = "Count";

    //Values
    var thingTypeCode = "default_rfid_thingtype";//(Required)
    var nameUdf       = "status";                //(Required)
    var nameFacility  = "Map Store Santa Monica";//(Optional)
    var groupCode     = "SM";                    //(Optional)
    var nameSoldUdf   = "Sold";                  //(Required)
    var minCount      = NumberInt(2);            //(Required)
    var zoneTypeBackroom = "backroom_code";      //(Required)
    var lstZoneTypeCheck = ["salesfloor_code","dressingroom_code"];//(Required)

    var lstZonesTypes = [];
    var result        = [];
    var storage       = [];
    var udf           = nameUdf+".value";

    lstZonesTypes.push(zoneTypeBackroom);
    for(var e in lstZoneTypeCheck){
        lstZonesTypes.push(lstZoneTypeCheck[e]);
    }

    //Get Vertical and Horizontal pivotes
    var curVertical   = db.things.distinct( udf,{ thingTypeCode :thingTypeCode } );
    var queryDistinctZT = {};
    queryDistinctZT["thingTypeCode"]      =thingTypeCode;
    queryDistinctZT["zone.value.zoneType"]={$in:lstZonesTypes};
    queryDistinctZT["Sold.value"]         =false;
    if(nameFacility){
        queryDistinctZT["zone.value.facilityMap"]=nameFacility;
    }
    if(groupCode){
        queryDistinctZT["groupCode"]=groupCode;
    }
    var curHorizontal = db.things.distinct("zone.value.zoneType",queryDistinctZT);
    table.columnNames = curHorizontal;
    table.rowNames    = curVertical;

    //Data Aggregate other Backroom
    var queryMatch   ={};
    queryMatch[udf]  ={$exists:true};
    queryMatch["Sold.value"]            =false;
    queryMatch["zone.value.zoneType"]   =zoneTypeBackroom;
    if(nameFacility){
        queryMatch["zone.value.facilityMap"]=nameFacility;
    }
    if(groupCode){
        queryMatch["groupCode"]=groupCode;
    }
    var querySort1   ={};
    querySort1[udf]  =1;
    var querySubGroup={};
    querySubGroup["zoneType"]="$zone.value.zoneType";
    querySubGroup[nameUdf]   ="$"+udf;
    var queryGroup     ={};
    queryGroup["_id"]  =querySubGroup;
    queryGroup["count"]={$sum:1};
    var querySort2     ={};
    querySort2["_id"+nameUdf]=1;

    var curBackRoom = db.things.aggregate( [
        {$match:queryMatch}
        ,{$sort :querySort1}
        ,{$group:queryGroup}
        ,{$sort:querySort2}
    ]);
    while(curBackRoom.hasNext()){
        var cur = curBackRoom.next();
        storage.push(cur);
    }

    //Data Aggregate other stores
    var queryMatchZT={};
    queryMatchZT[udf]={$exists:true};
    queryMatchZT["zone.value.zoneType"]   ={$in:lstZoneTypeCheck};
    queryMatchZT["Sold.value"]            =false;
    if(nameFacility){
        queryMatchZT["zone.value.facilityMap"]=nameFacility;
    }
    if(groupCode){
        queryMatchZT["groupCode"]=groupCode;
    }

    var curOthers = db.things.aggregate( [
        {$match:queryMatchZT}
        ,{$group:queryGroup}
        ,{$sort:{"_id.zoneType":1}}
        ,{$match:{count:{$lte:minCount}}}
    ]);
    while(curOthers.hasNext()){
        var cur2 = curOthers.next();
        storage.push(cur2);
    }

    //Iterate Pivotes and get result Data
    for(var i in curVertical){
        data = [];
        for(var x in curHorizontal){
            var printValue = -1;
            for(var s in storage){
                var valueData = storage[s];
                var udfValuePrint;
                for (var key in valueData._id) {
                    if(key==nameUdf){
                        udfValuePrint = valueData._id[key];
                    }
                }
                //print(curHorizontal[x]+" "+valueData._id.zoneType+" - "+curVertical[i]+" "+valueData._id.status+">"+valueData.count)
                if( (curHorizontal[x]==valueData._id.zoneType) && (curVertical[i] == udfValuePrint) ){
                    //print("**"+curHorizontal[x]+" "+valueData._id.zoneType+" - "+curVertical[i]+" "+valueData._id.status+">"+valueData.count)
                    printValue = valueData.count;
                    break;
                }
            }
            if(printValue>0){
                data.push(printValue+"");
            }else{
                data.push("");
            }
        }
        result.push(data);
    }

    // required
    table.data = result;
    return table;
}