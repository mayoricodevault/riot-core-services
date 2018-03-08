function (statusValue, thingTypeCodeItem, thingTypeCodeUPC, thingTypeCodeDepartment, thingTypeCodeUPCCategory, thingTypeCodeSellThruReplenishment, serialNumberThingSellThruReplenishment) {

    var start = new Date();
    start.setHours(0, 0, 0, 0);

    var end = new Date();
    end.setHours(23, 59, 59, 999);



    var itemDeptCode = getMapping("item", "deptCode.value").fromThingTypeUDF;
    var itemUpcCode = getMapping("item", "upcCode.value").fromThingTypeUDF;
    var itemStatus = getMapping("item", "status.value").fromThingTypeUDF;
    var itemZone = getMapping("item", "zone.value").fromThingTypeUDF;

    var itemLastDetectTime = getMapping("item", "lastDetectTime.time").fromThingTypeUDF;

    var itemDeptCodeUDFName = getMapping("item", "deptCode.value").fromThingTypeUDFName;

    var upcCategoryCodeUpc = getMapping("upc", "upcCategoryCode.value").fromThingTypeUDFName;
    var upcMin = getMapping("upc", "min.value").fromThingTypeUDFName;
    var upcMax = getMapping("upc", "max.value").fromThingTypeUDFName;
    var upcPrice = getMapping("upc", "price.value").fromThingTypeUDFName;
    var upcSize = getMapping("upc", "size.value").fromThingTypeUDFName;
    var upcColor = getMapping("upc", "color.value").fromThingTypeUDFName;
    var upcColorHexadecimal = getMapping("upc", "colorHexadecimal.value").fromThingTypeUDFName;
    var upcBrand = getMapping("upc", "brand.value").fromThingTypeUDFName;
    var upcMaterial = getMapping("upc", "material.value").fromThingTypeUDFName;

    var upcCategoryName = getMapping("upcCategory", "name").fromThingTypeUDFName;
    var upcCategoryCode = getMapping("upcCategory", "serialNumber").fromThingTypeUDFName;
    var upcCategoryFrontImage = getMapping("upcCategory", "frontImage.value").fromThingTypeUDFName;
    var upcCategoryBackImage = getMapping("upcCategory", "backImage.value").fromThingTypeUDFName;
    var upcCategorySideImage = getMapping("upcCategory", "sideImage.value").fromThingTypeUDFName;

    var deptCode = getMapping("dept", "serialNumber").fromThingTypeUDFName;
    var deptName = getMapping("dept", "name").fromThingTypeUDFName;
    var deptIcon = getMapping("dept", "icon.value").fromThingTypeUDFName;
    var deptSubGroup = getMapping("dept", "departmentSubGroup.value").fromThingTypeUDFName;
    var deptSubGroupName = getMapping("dept", "departmentSubGroupName.value").fromThingTypeUDFName;
    var deptSubGroupIcon = getMapping("dept", "departmentSubGroupIcon.value").fromThingTypeUDFName;

    var deptGroup = getMapping("dept", "departmentGroup.value").fromThingTypeUDFName;
    var deptGroupName = getMapping("dept", "departmentGroupName.value").fromThingTypeUDFName;
    var deptGroupIcon = getMapping("dept", "departmentGroupIcon.value").fromThingTypeUDFName;


    // Query to map reduce
    var queryMatch = {};

    queryMatch[itemDeptCode] = { $exists: true, $ne: "" };
    queryMatch[itemUpcCode] = { $exists: true, $ne: "" };
    queryMatch["thingTypeCode"] = thingTypeCodeItem;

    queryMatch[itemStatus] = statusValue;
    queryMatch[itemLastDetectTime] = { $gte: start, $lt: end };

    //scope to map, reduce, finalize functions
    var scopeMapReduce = {
        getMapping: getMapping
    };


    var map = function() {
        var mark = new Date();
        var replenishment = {};
        var itemDeptCode = getMapping("item", "deptCode.value").fromThingTypeUDFName;
        var itemUpcCode = getMapping("item", "upcCode.value").fromThingTypeUDFName;
        var itemZone = getMapping("item", "zone.value").fromThingTypeUDFName;

        var itemLastDetectTime = getMapping("item", "lastDetectTime.time").fromThingTypeUDFName;

        //var itemCreatedTime = getMapping("item", "createdTime", mapsConfig).fromThingTypeUDFName;

        var identifier = this[itemUpcCode]["value"] + "-" + this[itemDeptCode]["value"];

        replenishment["_id"] = identifier;
        replenishment["itemUpc"] = this[itemUpcCode]["value"];
        replenishment["itemDeptCode"] = this[itemDeptCode]["value"];

        replenishment.replenishSoldZones = [];

        replenishment.replenishSoldZones.push({ replenishSoldZoneName: this[itemZone]["value"]["name"], replenishSoldZoneId: this[itemZone]["value"]["id"] });
        replenishment.replenishSoldCount = 1;

        replenishment.replenishSoldZone = this[itemZone]["value"]["name"];

        replenishment["replenishThingsId"] = [];
        replenishment["replenishThingsId"].push(this._id);
        replenishment["replenishLastDate"] = mark;

        replenishment["itemSoldHour"] = this[itemLastDetectTime]["time"];

        emit(identifier, replenishment);
    }



    var reduce = function(identifier, replenishments) {

        function removeDuplicates(array, name, id) {
            var result = [];
            var obj = {};

            for (i = 0; i < array.length; i++) {

                var field = array[i][name];
                obj[field] = array[i][id];
            }

            for (var i in obj) {
                var newObject = {};
                newObject[name] = i;
                newObject[id] = obj[i];
                result.push(newObject);
            }

            return result;
        }

        var replenishmentReduced = {};
        if (Array.isArray(replenishments) && replenishments.length > 0) {
            replenishmentReduced = replenishments[0];

            for (i = 1; i < replenishments.length; i++) {
                var replenishment = replenishments[i];
                replenishmentReduced.replenishThingsId = replenishmentReduced.replenishThingsId.concat(replenishment.replenishThingsId);

                replenishmentReduced.replenishSoldZones = replenishmentReduced.replenishSoldZones.concat(replenishment.replenishSoldZones);
                replenishmentReduced.replenishSoldCount += replenishment.replenishSoldCount;


                replenishmentReduced.replenishSoldZone = replenishment.replenishSoldZone;

                replenishmentReduced.replenishSoldZones = removeDuplicates(replenishmentReduced.replenishSoldZones, "replenishSoldZoneName", "replenishSoldZoneId");

            }
        } else {
            replenishmentReduced = replenishments;
        }

        return replenishmentReduced;

    }

    db.things.mapReduce(map,
        reduce, {
            out: "retailSellThruReplenishmentTemp",
            query: queryMatch,
            scope: scopeMapReduce
        }
    )

    db.getCollection('thingSnapshots').deleteMany({ 'value.thingTypeCode': thingTypeCodeSellThruReplenishment });

    var bulk = db.thingSnapshots.initializeUnorderedBulkOp();

    var thingReplenishment = db.getCollection('things').findOne({ 'thingTypeCode': thingTypeCodeSellThruReplenishment, serialNumber: serialNumberThingSellThruReplenishment });
    var idThingReplenishment = thingReplenishment._id;

    db.retailSellThruReplenishmentTemp.find().forEach(function(x) {


        try {
            var row = {};
            row["_id"] = x._id;
            row["replenishId"] = x._id;

            row["replenishSoldCount"] = x.value["replenishSoldCount"];
            row["replenishSoldZone"] = x.value["replenishSoldZone"];
            row["replenishLastDate"] = x.value["replenishLastDate"];
            row["itemSoldHour"] = x.value["itemSoldHour"];
            row["itemUpc"] = x.value["itemUpc"];
            row["itemDeptCode"] = x.value["itemDeptCode"];

            var upc = db.things.findOne({ thingTypeCode: thingTypeCodeUPC, serialNumber: row["itemUpc"] });

            var upcCategory = db.things.findOne({ thingTypeCode: thingTypeCodeUPCCategory, serialNumber: upc[upcCategoryCodeUpc].value });

            var dept = db.things.findOne({ thingTypeCode: thingTypeCodeDepartment, serialNumber: row["itemDeptCode"] });

            row["itemName"] = upcCategory[upcCategoryName];

            row["itemDepartmentCode"] = dept[deptCode];
            row["itemDepartmentName"] = dept[deptName];
            row["itemDepartmentIcon"] = dept[deptIcon].value;

            row["itemStoreCode"] = "SM";
            row["itemStoreName"] = "Santa Monica";

            row["itemUPCCategoryCode"] = upcCategory[upcCategoryCode];
            row["itemUPCCategoryName"] = upcCategory[upcCategoryName];

            row["itemSize"] = upc[upcSize].value;
            row["itemBrand"] = upc[upcBrand].value;
            row["itemMaterial"] = upc[upcMaterial].value;

            row["itemDepartmentGroup"] = dept[deptGroup].value;
            row["itemDepartmentGroupName"] = dept[deptGroupName].value;
            row["itemDepartmentGroupIcon"] = dept[deptGroupIcon].value;

            row["itemDepartmentSubGroup"] = dept[deptGroup].value;
            row["itemDepartmentSubGroupName"] = dept[deptGroupName].value;
            row["itemDepartmentSubGroupIcon"] = dept[deptGroupIcon].value;

            row["itemPrice"] = upc[upcPrice].value;
            row["itemColor"] = upc[upcColor].value;

            row["itemColorHexadecimal"] = upc[upcColorHexadecimal].value;

            row["itemFrontImage"] = upcCategory[upcCategoryFrontImage].value;
            row["itemBackImage"] = upcCategory[upcCategoryBackImage].value;
            row["itemSideImage"] = upcCategory[upcCategorySideImage].value;

            var value = thingReplenishment;
            value["modifiedTime"] = new ISODate();
            value["time"] = new ISODate();
            for (var property in row) {
                if (value.hasOwnProperty(property)) {
                    value[property]["time"] = new ISODate();
                    value[property]["value"] = row[property];
                    value[property]["dwellTime"] = new NumberLong(0);
                    value[property]["changed"] = true;
                    value[property]["blinked"] = true;
                }
            }

            var snapshot = {};
            snapshot["value"] = value;
            snapshot["time"] = new ISODate();
            snapshot["createdTime"] = new ISODate();

            bulk.insert(snapshot);


        } catch (error) {
            print(error);
            print(x._id);
        }

    })


    bulk.execute();

    // Remove thingSnapshotsIds
    db.getCollection('thingSnapshotIds').update({ _id: idThingReplenishment }, { $set: { blinks: [] } });
    // Build blinks array
    var blinks = [];
    db.getCollection('thingSnapshots').find({ 'value.thingTypeCode': thingTypeCodeSellThruReplenishment }, { _id: 1 })
        .forEach(function(x) {

            var currentTime = new Date().getTime();
            var blink = {};
            blink.time = NumberLong(currentTime);
            blink.blink_id = x._id;

            blinks.push(blink);
        });


    // Add thingSnapshotsIds
    db.getCollection('thingSnapshotIds').update({
        _id: idThingReplenishment
    }, {
        $push: {
            blinks: { $each: blinks }
        }
    });


}


