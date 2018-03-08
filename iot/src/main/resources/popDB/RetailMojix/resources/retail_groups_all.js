/// Creted by: Omar Rivera
/// Groups Report
/// For Replenishment, Hot Replenishment, Sell Thru Replenishment
///
function(options) {
    var table = new Object();
    table.title = "Reports Groups";
    table.options = JSON.parse(JSON.stringify(options));

    table.columnNames = [
        "departmentLevelName",
        "departmentLevelCode",
        "departmentLevelIcon",
        "quantity",
        "totalAmount"
    ];

    function capitalize(name) {
        return name.charAt(0).toUpperCase() + name.slice(1);
    }

    var storeCode = "SM";
    if (options.hasOwnProperty('filters') && options.filters.hasOwnProperty('storeCode')) {
        storeCode = options.filters.storeCode;
    }

    var level = "1"; // 1 | 2 | 3
    if (options.hasOwnProperty('filters') && options.filters.hasOwnProperty('mode')) {
        categoryUpcSku = options.filters.mode;
    }

    var departmentLevelCode = "T19";
    if (options.hasOwnProperty('filters') && options.filters.hasOwnProperty('departmentLevelCode')) {
        departmentLevelCode = options.filters.departmentLevelCode;
    }

    var mode = "UPC"; // UPC | SKU | UPCSKU | UPCONLY | SKUONLY
    if (options.hasOwnProperty('filters') && options.filters.hasOwnProperty('mode')) {
        mode = options.filters.mode;
    }

    var typeGroup = "R"; // R | HR | STR
    if (options.hasOwnProperty('filters') && options.filters.hasOwnProperty('typeGroup')) {
        typeGroup = options.filters.typeGroup;
    }

    var queryMatch = {

        'value.itemStoreCode.value': storeCode
    }

    if (level === "1" && departmentLevelCode !== "" && departmentLevelCode !== "ALL") {
        queryMatch["value.itemDepartmentCode.value"] = departmentLevelCode;
    }
    if (level === "2" && departmentLevelCode !== "" && departmentLevelCode !== "ALL") {
        queryMatch["value.itemDepartmentSubGroup.value"] = departmentLevelCode;
    }
    if (level === "3" && departmentLevelCode !== "" && departmentLevelCode !== "ALL") {
        queryMatch["value.itemDepartmentGroup.value"] = departmentLevelCode;
    }

    var queryGroup = {};
    if (level === "1") {
        queryGroup = {
            _id: { departmentLevelGroup: '$value.itemDepartmentCode.value', },
            count: { $sum: 1 },
            totalAmount: { $sum: '$value.replenishQuantity.value' },
            departmentLevelGroupName: { $first: "$value.itemDepartmentName.value" },
            departmentLevelGroupIcon: { $first: "$value.itemDepartmentIcon.value" }
        };
    }
    if (level === "2") {
        queryGroup = {
            _id: { departmentLevelGroup: '$value.itemDepartmentSubGroup.value', },
            count: { $sum: 1 },
            totalAmount: { $sum: '$value.replenishQuantity.value' },
            departmentLevelGroupName: { $first: "$value.itemDepartmentSubGroupName.value" },
            departmentLevelGroupIcon: { $first: "$value.itemDepartmentSubGroupIcon.value" }
        };
    }
    if (level === "3") {
        queryGroup = {
            _id: { departmentLevelGroup: '$value.itemDepartmentGroup.value', },
            count: { $sum: 1 },
            totalAmount: { $sum: '$value.replenishQuantity.value' },
            departmentLevelGroupName: { $first: "$value.itemDepartmentGroupName.value" },
            departmentLevelGroupIcon: { $first: "$value.itemDepartmentGroupIcon.value" }
        };
    }

    var groups = [];

    if (typeGroup === "R") {
        var queryMatch2 = {
            'value.thingTypeCode': 'replenishment',
            'value.serialNumber': 'REPLENISHMENT'
        }

        Object.assign(queryMatch, queryMatch2);

        db.getCollection('thingSnapshots')
            .aggregate([{ $match: queryMatch },
                { $group: queryGroup },

            ]).forEach(function(item) {
            item = JSON.parse(JSON.stringify(item));

            var x = item._id;
            var group = [];

            group.push(

                x.departmentLevelGroup,
                item.departmentLevelGroupName,
                item.departmentLevelGroupName,
                item.count,
                item.totalAmount
            );

            groups.push(group);

        });
    }

    if (typeGroup === "HR") {

        var queryMatch2 = {
            'value.thingTypeCode': 'hotReplenishment',
            'value.serialNumber': 'HOTREPLENISHMENT'
        };

        Object.assign(queryMatch, queryMatch2);

        db.getCollection('thingSnapshots')
            .aggregate([{ $match: queryMatch },
                { $group: queryGroup },

            ]).forEach(function(item) {
            item = JSON.parse(JSON.stringify(item));

            var x = item._id;
            var group = [];
            group.push(

                x.departmentLevelGroup,
                item.departmentLevelGroupName,
                item.departmentLevelGroupName,
                item.count,
                item.totalAmount
            );

            groups.push(group);

        });
    }

    if (typeGroup === "STR") {

        var queryMatch2 = {
            'value.thingTypeCode': 'replenishment',
            'value.serialNumber': 'REPLENISHMENT'
        }

        Object.assign(queryMatch, queryMatch2);

        db.getCollection('thingSnapshots')
            .aggregate([{ $match: queryMatch },
                { $group: queryGroup },

            ]).forEach(function(item) {
            item = JSON.parse(JSON.stringify(item));

            var x = item._id;
            var group = [];
            group.push(

                x.departmentLevelGroup,
                item.departmentLevelGroupName,
                item.departmentLevelGroupName,
                item.count,
                item.totalAmount
            );
            groups.push(group);

        });
    }

    table.totalRows = groups.length;
    table.data = groups;

    return table;
}