function (salesZoneType, stockZoneType, dressingZoneType, statusValue, thingTypeCodeItem, mode) {

    var itemDeptCode = getMapping("item", "deptCode.value").fromThingTypeUDF;
    var itemUpcCode = getMapping("item", "upcCode.value").fromThingTypeUDF;
    var itemSkuCode = getMapping("item", "skuCode.value").fromThingTypeUDF;
    var itemStatus = getMapping("item", "status.value").fromThingTypeUDF;
    var itemZone = getMapping("item", "zone.value").fromThingTypeUDF;
    var zoneTypes = [salesZoneType, stockZoneType, dressingZoneType];

    // Query to map reduce
    var queryMatch = {};

    queryMatch[itemDeptCode] = { $exists: true, $ne: "" };
    queryMatch["thingTypeCode"] = thingTypeCodeItem;
    queryMatch[itemZone + ".zoneType"] = { $in: zoneTypes };
    queryMatch[itemStatus] = statusValue;

    if (mode === "UPC" || mode === "UPCONLY") {
        queryMatch[itemUpcCode] = { $exists: true, $ne: "" };
    }
    if (mode === "SKU" || mode === "SKUONLY") {
        queryMatch[itemSkuCode] = { $exists: true, $ne: "" };
    }
    if (mode === "UPCSKU") {
        queryMatch[itemSkuCode] = { $exists: true, $ne: "" };
        queryMatch[itemUpcCode] = { $exists: true, $ne: "" };
    }

    //scope to map, reduce, finalize functions
    var scopeMapReduce = {
        getMapping: getMapping,
        salesZoneType: salesZoneType,
        stockZoneType: stockZoneType,
        dressingZoneType: dressingZoneType,
        mode: mode
    };

    var map = function() {
        var mark = new Date();
        var replenishment = {};
        var itemDeptCode = getMapping("item", "deptCode.value").fromThingTypeUDFName;
        var itemUpcCode = getMapping("item", "upcCode.value").fromThingTypeUDFName;
        var itemSkuCode = getMapping("item", "skuCode.value").fromThingTypeUDFName;
        var itemZone = getMapping("item", "zone.value").fromThingTypeUDFName;


        var identifier = "";
        if (mode === "UPC" || mode === "UPCONLY") {
            identifier = this[itemUpcCode]["value"] + "-" + this[itemDeptCode]["value"];
            replenishment["itemUPC"] = this[itemUpcCode]["value"];
        }
        if (mode === "SKU" || mode === "SKUONLY") {
            identifier = this[itemSkuCode]["value"] + "-" + this[itemDeptCode]["value"];
            replenishment["itemSKU"] = this[itemSkuCode]["value"];

        }
        if (mode === "UPCSKU") {
            identifier = this[itemUpcCode]["value"] + "-" + this[itemDeptCode]["value"];
            replenishment["itemUPC"] = this[itemUpcCode]["value"];
            replenishment["itemSKU"] = this[itemUpcCode]["value"];
        }

        replenishment["_id"] = identifier;

        replenishment["itemDeptCode"] = this[itemDeptCode]["value"];

        replenishment.replenishFrontZone = "";
        replenishment.replenishBackZone = "";
        replenishment.replenishDressingZone = "";

        replenishment.replenishFrontZones = [];
        replenishment.replenishBackZones = [];
        replenishment.replenishDressingZones = [];

        replenishment.replenishFrontCount = 0;
        replenishment.replenishBackCount = 0;
        replenishment.replenishDressingCount = 0;

        replenishment["itemZoneType"] = "";

        if (this[itemZone]["value"]["zoneType"] === salesZoneType) {

            replenishment["itemZoneType"] = this[itemZone]["value"]["zoneType"];
            replenishment.replenishFrontZones.push({ replenishFrontZoneName: this[itemZone]["value"]["name"], replenishFrontZoneId: this[itemZone]["value"]["id"] });
            replenishment.replenishFrontCount = 1;

            replenishment.replenishFrontZone = this[itemZone]["value"]["name"];

        }

        if (this[itemZone]["value"]["zoneType"] === stockZoneType) {

            replenishment["itemZoneType"] = this[itemZone]["value"]["zoneType"];
            replenishment.replenishBackZones.push({ replenishBackZoneName: this[itemZone]["value"]["name"], replenishBackZoneId: this[itemZone]["value"]["id"] });
            replenishment.replenishBackCount = 1;

            replenishment.replenishBackZone = this[itemZone]["value"]["name"];
        }

        if (this[itemZone]["value"]["zoneType"] === dressingZoneType) {

            replenishment["itemZoneType"] = this[itemZone]["value"]["zoneType"];
            replenishment.replenishDressingZones.push({ replenishDressingZoneName: this[itemZone]["value"]["name"], replenishDressingZoneId: this[itemZone]["value"]["id"] });
            replenishment.replenishDressingCount = 1;

            replenishment.replenishDressingZone = this[itemZone]["value"]["name"];
        }

        //replenishment["itemCreatedTime"] = this[itemCreatedTime];
        replenishment["replenishThingsId"] = [];
        replenishment["replenishThingsId"].push(this._id);
        replenishment["replenishLastDate"] = mark;

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

            for (var i = 1; i < replenishments.length; i++) {
                var replenishment = replenishments[i];
                replenishmentReduced.replenishThingsId = replenishmentReduced.replenishThingsId.concat(replenishment.replenishThingsId);
                if (replenishment.itemZoneType === "SalesZoneType") {

                    replenishmentReduced.replenishFrontZones = replenishmentReduced.replenishFrontZones.concat(replenishment.replenishFrontZones);
                    replenishmentReduced.replenishFrontCount += replenishment.replenishFrontCount;

                    replenishmentReduced.replenishFrontZone = replenishment.replenishFrontZone;

                }
                if (replenishment.itemZoneType === "StockZoneType") {
                    replenishmentReduced.replenishBackZones = replenishmentReduced.replenishBackZones.concat(replenishment.replenishBackZones);
                    replenishmentReduced.replenishBackCount += replenishment.replenishBackCount;

                    replenishmentReduced.replenishBackZone = replenishment.replenishBackZone;
                }
                if (replenishment.itemZoneType === "DressingZoneType") {
                    replenishmentReduced.replenishDressingZones = replenishmentReduced.replenishDressingZones.concat(replenishment.replenishDressingZones);
                    replenishmentReduced.replenishDressingCount += replenishment.replenishDressingCount;

                    replenishmentReduced.replenishDressingZone = replenishment.replenishDressingZone;
                }

                replenishmentReduced.replenishFrontZones = removeDuplicates(replenishmentReduced.replenishFrontZones, "replenishFrontZoneName", "replenishFrontZoneId");

                replenishmentReduced.replenishBackZones = removeDuplicates(replenishmentReduced.replenishBackZones, "replenishBackZoneName", "replenishBackZoneId");

                replenishmentReduced.replenishDressingZones = removeDuplicates(replenishmentReduced.replenishDressingZones, "replenishDressingZoneName", "replenishDressingZoneId");

            }
        } else {
            replenishmentReduced = replenishments;
        }

        delete replenishmentReduced.itemZoneType;
        return replenishmentReduced;

    }

    db.things.mapReduce(map,
        reduce, {
            out: "retailItemsCount",
            query: queryMatch,
            scope: scopeMapReduce
        }
    )



}


