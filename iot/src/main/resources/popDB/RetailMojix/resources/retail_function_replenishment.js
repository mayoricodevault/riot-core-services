function (thingTypeCodeUPC, thingTypeCodeSKU, thingTypeCodeDepartment, thingTypeCodeUPCCategory, thingTypeCodeSKUCategory, thingTypeCodeReplenishment, serialNumberThingReplenishment, mode) {

    var itemDeptCode = getMapping("item", "deptCode.value").fromThingTypeUDFName;

    if (mode === "UPC" || mode === "UPCSKU" || mode === "UPCONLY") {
        var upcCategoryCodeUpc = getMapping("upc", "upcCategoryCode.value").fromThingTypeUDFName;
        var upcMin = getMapping("upc", "min.value").fromThingTypeUDFName;
        var upcMax = getMapping("upc", "max.value").fromThingTypeUDFName;
        var upcPrice = getMapping("upc", "price.value").fromThingTypeUDFName;
        var upcSize = getMapping("upc", "size.value").fromThingTypeUDFName;
        var upcColor = getMapping("upc", "color.value").fromThingTypeUDFName;
        var upcColorHexadecimal = getMapping("upc", "colorHexadecimal.value").fromThingTypeUDFName;
        var upcBrand = getMapping("upc", "brand.value").fromThingTypeUDFName;
        var upcMaterial = getMapping("upc", "material.value").fromThingTypeUDFName;
    }
    if (mode === "SKU" || mode === "UPCSKU" || mode === "SKUONLY") {
        var skuCategoryCodeSku = getMapping("sku", "skuCategoryCode.value").fromThingTypeUDFName;
        var skuMin = getMapping("sku", "min.value").fromThingTypeUDFName;
        var skuMax = getMapping("sku", "max.value").fromThingTypeUDFName;
        var skuPrice = getMapping("sku", "price.value").fromThingTypeUDFName;
        var skuSize = getMapping("sku", "size.value").fromThingTypeUDFName;
        var skuColor = getMapping("sku", "color.value").fromThingTypeUDFName;
        var skuColorHexadecimal = getMapping("sku", "colorHexadecimal.value").fromThingTypeUDFName;
        var skuBrand = getMapping("sku", "brand.value").fromThingTypeUDFName;
        var skuMaterial = getMapping("sku", "material.value").fromThingTypeUDFName;
    }


    if (mode === "UPC" || mode === "UPCSKU") {
        var upcCategoryName = getMapping("upcCategory", "name").fromThingTypeUDFName;
        var upcCategoryCode = getMapping("upcCategory", "serialNumber").fromThingTypeUDFName;
        var upcCategoryFrontImage = getMapping("upcCategory", "frontImage.value").fromThingTypeUDFName;
        var upcCategoryBackImage = getMapping("upcCategory", "backImage.value").fromThingTypeUDFName;
        var upcCategorySideImage = getMapping("upcCategory", "sideImage.value").fromThingTypeUDFName;
    }
    if (mode === "SKU" || mode === "UPCSKU") {
        var skuCategoryName = getMapping("skuCategory", "name").fromThingTypeUDFName;
        var skuCategoryCode = getMapping("skuCategory", "serialNumber").fromThingTypeUDFName;
        var skuCategoryFrontImage = getMapping("skuCategory", "frontImage.value").fromThingTypeUDFName;
        var skuCategoryBackImage = getMapping("skuCategory", "backImage.value").fromThingTypeUDFName;
        var skuCategorySideImage = getMapping("skuCategory", "sideImage.value").fromThingTypeUDFName;
    }

    var deptCode = getMapping("dept", "serialNumber").fromThingTypeUDFName;
    var deptName = getMapping("dept", "name").fromThingTypeUDFName;
    var deptIcon = getMapping("dept", "icon.value").fromThingTypeUDFName;
    var deptSubGroup = getMapping("dept", "departmentSubGroup.value").fromThingTypeUDFName;
    var deptSubGroupName = getMapping("dept", "departmentSubGroupName.value").fromThingTypeUDFName;
    var deptSubGroupIcon = getMapping("dept", "departmentSubGroupIcon.value").fromThingTypeUDFName;

    var deptGroup = getMapping("dept", "departmentGroup.value").fromThingTypeUDFName;
    var deptGroupName = getMapping("dept", "departmentGroupName.value").fromThingTypeUDFName;
    var deptGroupIcon = getMapping("dept", "departmentGroupIcon.value").fromThingTypeUDFName;

    db.getCollection('thingSnapshots').deleteMany({ 'value.thingTypeCode': thingTypeCodeReplenishment, 'value.serialNumber': serialNumberThingReplenishment });

    // TODO: review bulkoperation, insert the same object all time
    //var bulk = db.thingSnapshots.initializeUnorderedBulkOp();

    var thingReplenishment = db.getCollection('things').findOne({ 'thingTypeCode': thingTypeCodeReplenishment, 'serialNumber': serialNumberThingReplenishment });
    var idThingReplenishment = thingReplenishment._id;

    db.retailItemsCount.find({}).forEach(function(x) {

        try {

            var row = {};
            row["_id"] = x._id;
            row["replenishId"] = x._id;
            row["replenishBackCount"] = x.value["replenishBackCount"];
            row["replenishFrontCount"] = x.value["replenishFrontCount"];
            row["replenishBackZone"] = x.value["replenishBackZone"];
            row["replenishFrontZone"] = x.value["replenishFrontZone"];
            row["replenishLastDate"] = x.value["replenishLastDate"];

            row["itemDeptCode"] = x.value["itemDeptCode"];

            var min = 0;
            var max = 0;

            var upc = {};
            if (mode === "UPC" || mode === "UPCSKU" || mode === "UPCONLY") {
                row["itemUPC"] = x.value["itemUPC"];
                upc = db.things.findOne({ thingTypeCode: thingTypeCodeUPC, serialNumber: row["itemUPC"] });

                min = upc[upcMin].value;
                max = upc[upcMax].value;

                row["itemSizeUPC"] = upc[upcSize].value;
                row["itemBrandUPC"] = upc[upcBrand].value;
                row["itemMaterialUPC"] = upc[upcMaterial].value;
                row["replenishMinUPC"] = min;
                row["replenishMaxUPC"] = max;

                row["itemPriceUPC"] = upc[upcPrice].value;
                row["itemColorUPC"] = upc[upcColor].value;
                row["itemColorHexadecimalUPC"] = upc[upcColorHexadecimal].value;


            }
            var upcCategory = {};
            if (mode === "UPC" || mode === "UPCSKU") {
                upcCategory = db.things.findOne({ thingTypeCode: thingTypeCodeUPCCategory, serialNumber: upc[upcCategoryCodeUpc].value });

                row["itemName"] = upcCategory[upcCategoryName];

                row["itemUPCCategoryCode"] = upcCategory[upcCategoryCode];
                row["itemUPCCategoryName"] = upcCategory[upcCategoryName];

                row["itemFrontImageUPC"] = upcCategory[upcCategoryFrontImage].value;
                row["itemBackImageUPC"] = upcCategory[upcCategoryBackImage].value;
                row["itemSideImageUPC"] = upcCategory[upcCategorySideImage].value;

            }
            var sku = {};
            if (mode === "SKU" || mode === "UPCSKU" || mode === "SKUONLY") {
                row["itemSKU"] = x.value["itemSKU"];
                var sku = db.things.findOne({ thingTypeCode: thingTypeCodeSKU, serialNumber: row["itemSKU"] });

                min = sku[skuMin].value;
                max = sku[skuMax].value;

                row["itemSKU"] = x.value["itemSKU"];
                row["itemSizeSKU"] = sku[skuSize].value;
                row["itemBrandSKU"] = sku[skuBrand].value;
                row["itemMaterialSKU"] = sku[skuMaterial].value;
                row["replenishMinSKU"] = min;
                row["replenishMaxSKU"] = max;

                row["itemPriceSKU"] = sku[skuPrice].value;
                row["itemColorSKU"] = sku[skuColor].value;
                row["itemColorHexadecimalSKU"] = sku[skuColorHexadecimal].value;


            }
            var skuCategory = {};
            if (mode === "SKU" || mode === "UPCSKU") {
                skuCategory = db.things.findOne({ thingTypeCode: thingTypeCodeSKUCategory, serialNumber: sku[skuCategoryCodeSku].value });

                row["itemName"] = skuCategory[skuCategoryName];

                row["itemSKUCategoryCode"] = skuCategory[skuCategoryCode];
                row["itemSKUCategoryName"] = skuCategory[skuCategoryName];

                row["itemFrontImageSKU"] = skuCategory[skuCategoryFrontImage].value;
                row["itemBackImageSKU"] = skuCategory[skuCategoryBackImage].value;
                row["itemSideImageSKU"] = skuCategory[skuCategorySideImage].value;
            }

            var dept = db.things.findOne({ thingTypeCode: thingTypeCodeDepartment, serialNumber: row["itemDeptCode"] });

            //var store = db.things.findOne({ thingTypeCode: "store", serialNumber: dept.storeCode.value });

            var backCount = row["replenishBackCount"];
            var shopCount = row["replenishFrontCount"];

            // Function calculateReplenishment => stored function.
            var amount = calculateReplenishment(shopCount, backCount, min, max);
            /*
             row["itemStockShelf"] = "-1";
             if (backCount > 0 && shopCount > 0) {
             row["itemStockShelf"] = "0";
             } else if (backCount > 0 && shopCount === 0) {
             row["itemStockShelf"] = "1";
             } else if (backCount === 0 && shopCount > 0) {
             row["itemStockShelf"] = "2";
             }
             */


            row["itemDepartmentCode"] = dept[deptCode];
            row["itemDepartmentName"] = dept[deptName];
            row["itemDepartmentIcon"] = dept[deptIcon].value;

            row["itemStoreCode"] = "SM";
            row["itemStoreName"] = "Santa Monica";

            row["itemDepartmentGroup"] = dept[deptGroup].value;
            row["itemDepartmentGroupName"] = dept[deptGroupName].value;
            row["itemDepartmentGroupIcon"] = dept[deptGroupIcon].value;

            row["itemDepartmentSubGroup"] = dept[deptSubGroup].value;
            row["itemDepartmentSubGroupName"] = dept[deptSubGroupName].value;
            row["itemDepartmentSubGroupIcon"] = dept[deptSubGroupIcon].value;

            row["replenishQuantity"] = amount;

            var thing = {};
            thing = Object.assign({}, thingReplenishment);
            // var value = thingReplenishment;

            thing["modifiedTime"] = new ISODate();
            thing["time"] = new ISODate();
            for (var property in row) {
                if (thing.hasOwnProperty(property)) {
                    thing[property]["time"] = new ISODate();
                    thing[property]["value"] = row[property];
                    thing[property]["dwellTime"] = new NumberLong(0);
                    thing[property]["changed"] = true;
                    thing[property]["blinked"] = true;
                }
            }

            var snapshot = {};
            snapshot["value"] = {};
            snapshot["value"] = thing;
            snapshot["time"] = new ISODate();
            snapshot["createdTime"] = new ISODate();
            // print("snapshot");
            // print(snapshot);
            // print(amount);

            if (amount > 0) {
                //  bulk.insert(snapshot);
                db.thingSnapshots.insertOne(snapshot);
            }


        } catch (error) {
            print(error);
            print(x._id);
            print(upcCategory);
        }

    })

    //var result = bulk.execute();

    // Remove thingSnapshotsIds
    db.getCollection('thingSnapshotIds').update({ _id: idThingReplenishment }, { $set: { blinks: [] } });
    // Build blinks array
    var blinks = [];
    db.getCollection('thingSnapshots').find({ 'value.thingTypeCode': thingTypeCodeReplenishment }, { _id: 1 })
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

