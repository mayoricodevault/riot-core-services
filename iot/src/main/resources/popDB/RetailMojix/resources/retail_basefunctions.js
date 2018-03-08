function(options) {
    var table = new Object();
    table.title = "Mapping functions";

    table.columnNames = ["start", "end", "result"];

    var start = new Date();

    var getMapping = function(toThingType, toThingTypeUDF) {
        //**************************************************
        //  Start Array Mapping
        //**************************************************
        //  Don't change toThingType, toThingTypeUDF values
        //**************************************************
        var mapping = [
            { "fromThingType": "dept", "fromThingTypeUDF": "name", "toThingType": "dept", "toThingTypeUDF": "name" },
            { "fromThingType": "dept", "fromThingTypeUDF": "serialNumber", "toThingType": "dept", "toThingTypeUDF": "serialNumber" },
            { "fromThingType": "dept", "fromThingTypeUDF": "icon.value", "toThingType": "dept", "toThingTypeUDF": "icon.value" },
            { "fromThingType": "dept", "fromThingTypeUDF": "departmentGroup.value", "toThingType": "dept", "toThingTypeUDF": "departmentGroup.value" },
            { "fromThingType": "dept", "fromThingTypeUDF": "departmentGroupName.value", "toThingType": "dept", "toThingTypeUDF": "departmentGroupName.value" },
            { "fromThingType": "dept", "fromThingTypeUDF": "departmentGroupIcon.value", "toThingType": "dept", "toThingTypeUDF": "departmentGroupIcon.value" },
            { "fromThingType": "dept", "fromThingTypeUDF": "departmentSubGroup.value", "toThingType": "dept", "toThingTypeUDF": "departmentSubGroup.value" },
            { "fromThingType": "dept", "fromThingTypeUDF": "departmentSubGroupName.value", "toThingType": "dept", "toThingTypeUDF": "departmentSubGroupName.value" },
            { "fromThingType": "dept", "fromThingTypeUDF": "departmentSubGroupIcon.value", "toThingType": "dept", "toThingTypeUDF": "departmentSubGroupIcon.value" },
            { "fromThingType": "upcCategory", "fromThingTypeUDF": "name", "toThingType": "upcCategory", "toThingTypeUDF": "name" },
            { "fromThingType": "upcCategory", "fromThingTypeUDF": "serialNumber", "toThingType": "upcCategory", "toThingTypeUDF": "serialNumber" },
            { "fromThingType": "upcCategory", "fromThingTypeUDF": "frontImage.value", "toThingType": "upcCategory", "toThingTypeUDF": "frontImage.value" },
            { "fromThingType": "upcCategory", "fromThingTypeUDF": "backImage.value", "toThingType": "upcCategory", "toThingTypeUDF": "backImage.value" },
            { "fromThingType": "upcCategory", "fromThingTypeUDF": "sideImage.value", "toThingType": "upcCategory", "toThingTypeUDF": "sideImage.value" },
            { "fromThingType": "skuCategory", "fromThingTypeUDF": "name", "toThingType": "skuCategory", "toThingTypeUDF": "name" },
            { "fromThingType": "skuCategory", "fromThingTypeUDF": "serialNumber", "toThingType": "skuCategory", "toThingTypeUDF": "serialNumber" },
            { "fromThingType": "skuCategory", "fromThingTypeUDF": "frontImage.value", "toThingType": "skuCategory", "toThingTypeUDF": "frontImage.value" },
            { "fromThingType": "skuCategory", "fromThingTypeUDF": "backImage.value", "toThingType": "skuCategory", "toThingTypeUDF": "backImage.value" },
            { "fromThingType": "skuCategory", "fromThingTypeUDF": "sideImage.value", "toThingType": "skuCategory", "toThingTypeUDF": "sideImage.value" },
            { "fromThingType": "upc", "fromThingTypeUDF": "name", "toThingType": "upc", "toThingTypeUDF": "name" },
            { "fromThingType": "upc", "fromThingTypeUDF": "serialNumber", "toThingType": "upc", "toThingTypeUDF": "serialNumber" },
            { "fromThingType": "upc", "fromThingTypeUDF": "max.value", "toThingType": "upc", "toThingTypeUDF": "max.value" },
            { "fromThingType": "upc", "fromThingTypeUDF": "min.value", "toThingType": "upc", "toThingTypeUDF": "min.value" },
            { "fromThingType": "upc", "fromThingTypeUDF": "price.value", "toThingType": "upc", "toThingTypeUDF": "price.value" },
            { "fromThingType": "upc", "fromThingTypeUDF": "brand.value", "toThingType": "upc", "toThingTypeUDF": "brand.value" },
            { "fromThingType": "upc", "fromThingTypeUDF": "material.value", "toThingType": "upc", "toThingTypeUDF": "material.value" },
            { "fromThingType": "upc", "fromThingTypeUDF": "size.value", "toThingType": "upc", "toThingTypeUDF": "size.value" },
            { "fromThingType": "upc", "fromThingTypeUDF": "color.value", "toThingType": "upc", "toThingTypeUDF": "color.value" },
            { "fromThingType": "upc", "fromThingTypeUDF": "colorHexadecimal.value", "toThingType": "upc", "toThingTypeUDF": "colorHexadecimal.value" },
            { "fromThingType": "upc", "fromThingTypeUDF": "upcCategoryCode.value", "toThingType": "upc", "toThingTypeUDF": "upcCategoryCode.value" },
            { "fromThingType": "sku", "fromThingTypeUDF": "name", "toThingType": "sku", "toThingTypeUDF": "name" },
            { "fromThingType": "sku", "fromThingTypeUDF": "serialNumber", "toThingType": "sku", "toThingTypeUDF": "serialNumber" },
            { "fromThingType": "sku", "fromThingTypeUDF": "max.value", "toThingType": "sku", "toThingTypeUDF": "max.value" },
            { "fromThingType": "sku", "fromThingTypeUDF": "min.value", "toThingType": "sku", "toThingTypeUDF": "min.value" },
            { "fromThingType": "sku", "fromThingTypeUDF": "price.value", "toThingType": "sku", "toThingTypeUDF": "price.value" },
            { "fromThingType": "sku", "fromThingTypeUDF": "brand.value", "toThingType": "sku", "toThingTypeUDF": "brand.value" },
            { "fromThingType": "sku", "fromThingTypeUDF": "material.value", "toThingType": "sku", "toThingTypeUDF": "material.value" },
            { "fromThingType": "sku", "fromThingTypeUDF": "size.value", "toThingType": "sku", "toThingTypeUDF": "size.value" },
            { "fromThingType": "sku", "fromThingTypeUDF": "color.value", "toThingType": "sku", "toThingTypeUDF": "color.value" },
            { "fromThingType": "sku", "fromThingTypeUDF": "colorHexadecimal.value", "toThingType": "sku", "toThingTypeUDF": "colorHexadecimal.value" },
            { "fromThingType": "sku", "fromThingTypeUDF": "skuCategoryCode.value", "toThingType": "sku", "toThingTypeUDF": "skuCategoryCode.value" },
            { "fromThingType": "item", "fromThingTypeUDF": "name", "toThingType": "item", "toThingTypeUDF": "name" },
            { "fromThingType": "item", "fromThingTypeUDF": "serialNumber", "toThingType": "item", "toThingTypeUDF": "serialNumber" },
            { "fromThingType": "item", "fromThingTypeUDF": "createdTime", "toThingType": "item", "toThingTypeUDF": "createdTime" },
            { "fromThingType": "item", "fromThingTypeUDF": "lastDetectTime.time", "toThingType": "item", "toThingTypeUDF": "lastDetectTime.time" },
            { "fromThingType": "item", "fromThingTypeUDF": "zone.value", "toThingType": "item", "toThingTypeUDF": "zone.value" },
            { "fromThingType": "item", "fromThingTypeUDF": "status.value", "toThingType": "item", "toThingTypeUDF": "status.value" },
            { "fromThingType": "item", "fromThingTypeUDF": "upcCode.value", "toThingType": "item", "toThingTypeUDF": "upcCode.value" },
            { "fromThingType": "item", "fromThingTypeUDF": "deptCode.value", "toThingType": "item", "toThingTypeUDF": "deptCode.value" },
            { "fromThingType": "item", "fromThingTypeUDF": "skuCode.value", "toThingType": "item", "toThingTypeUDF": "skuCode.value" }
        ];

        //*********************************************************
        //****** End Array Mapping*********************************
        //*********************************************************

        var from = {
            fromThingType: "",
            fromThingTypeUDF: "",
            fromThingTypeFieldName: "",
            fromThingTypeFieldType: ""
        };

        for (i = 0; i < mapping.length; i++) {
            if (mapping[i].toThingType === toThingType && mapping[i].toThingTypeUDF === toThingTypeUDF) {
                from.fromThingType = mapping[i].fromThingType;
                from.fromThingTypeUDF = mapping[i].fromThingTypeUDF;
                var values = mapping[i].fromThingTypeUDF.split(".");
                /// Expected results in value[0]: name UDF (price, size) or Base field (createdTime, name, serialNumber)
                /// Expected results in value[1]: value, time
                if (values.length > 0) {
                    from.fromThingTypeUDFName = values[0];
                }
                if (values.length > 1) {
                    from.fromThingTypeFieldType = values[1];
                } else {
                    from.fromThingTypeFieldType = 'base';
                }
            }
        }

        return from;

    }

    // Function to calculate Replenishment
    var calculateReplenishment = function(sellingFloor, backRoom, min, max) {
        var transfer = 0;
        if (sellingFloor < min) {
            var add = max - sellingFloor;
            if (add < backRoom) {
                transfer = add;
            } else {
                transfer = backRoom;
            }
        }
        return transfer;
    }

    /// Store javascript functions on mongo collection system.js

    db.system.js.save({
        _id: "getMapping",
        value: getMapping
    });

    db.system.js.save({
        _id: "calculateReplenishment",
        value: calculateReplenishment
    });

    var end = new Date();
    var row = [start, end, "Success"];
    table.totalRows = 1;
    table.data = [];
    table.data.push(row);

    return table;
}