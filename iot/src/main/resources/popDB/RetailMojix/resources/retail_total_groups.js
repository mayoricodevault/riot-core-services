/**
 * Created by orivera on 11/3/16.
 * [Retail] Totals
 */
function(options) {
    var table = new Object();
    table.title = "Home counts";
    table.options = JSON.parse(JSON.stringify(options));

    var skip = options.pageSize * (options.pageNumber - 1);
    var limit = options.pageSize;

    table.columnNames = [
        "Replenishment",
        "HotReplenishment",
        "DressingRoom"
    ];

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

    // ***********************************************
    // ********* Total for Replenishment *********
    // ***********************************************

    var queryMatchReplenishment = {
        'value.thingTypeCode': 'replenishment',
        'value.serialNumber': 'REPLENISHMENT',

        'value.itemStoreCode.value': storeCode
    }

    if (level === "1" && departmentLevelCode !== "" && departmentLevelCode !== "ALL") {
        queryMatchReplenishment["value.itemDepartmentCode.value"] = departmentLevelCode;
    }
    if (level === "2" && departmentLevelCode !== "" && departmentLevelCode !== "ALL") {
        queryMatchReplenishment["value.itemDepartmentSubGroup.value"] = departmentLevelCode;
    }
    if (level === "3" && departmentLevelCode !== "" && departmentLevelCode !== "ALL") {
        queryMatchReplenishment["value.itemDepartmentGroup.value"] = departmentLevelCode;
    }

    var queryGroupReplenishment = {
        _id: {
            upcCategoryCode: '$itemUPCCategoryCode',
            groupCategory: '$itemGroupCategory'
        },
        storeCode: { $first: '$itemStoreCode' },
        count: { $sum: 1 },
        amount: { $sum: '$replenishQuantity' }
    };


    var queryGroupReplenishment = {};
    if (mode === "UPC") {
        queryGroupReplenishment = {
            _id: {
                upcCategoryCode: '$value.itemUPCCategoryCode.value'
            },

            storeCode: { $first: '$value.itemStoreCode.value' },
            count: { $sum: 1 },
            amount: { $sum: '$value.replenishQuantity.value' }

        }

    }

    if (mode === "UPCSKU") {
        queryGroupReplenishment = {
            _id: {
                upcCategoryCode: '$value.itemUPCCategoryCode.value'
            },

            storeCode: { $first: '$value.itemStoreCode.value' },
            count: { $sum: 1 },
            amount: { $sum: '$value.replenishQuantity.value' }

        }

    }

    if (mode === "SKU") {
        queryGroupReplenishment = {
            _id: {
                skuCategoryCode: '$value.itemSKUCategoryCode.value'
            },
            storeCode: { $first: '$value.itemStoreCode.value' },
            count: { $sum: 1 },
            amount: { $sum: '$value.replenishQuantity.value' }
        }
    }

    // This just to get the total number of rows
    var replenishmentTotal = 0;
    var replenishmentTotalAmount = 0;
    db.getCollection('thingSnapshots')
        .aggregate([{ $match: queryMatchReplenishment },
            { $group: queryGroupReplenishment },
            {
                $group: {
                    _id: { storeCode: '$storeCode' },
                    count: { $sum: 1 },
                    amount: { $sum: '$amount' }
                }
            }

        ]).forEach(function(item) {
        item = JSON.parse(JSON.stringify(item));
        replenishmentTotal = item.count;
        replenishmentTotalAmount = item.amount;
    })

    // ***********************************************
    // ********* Total for Hot Replenishment *********
    // ***********************************************

    var queryMatchHotReplenishment = {
        'value.thingTypeCode': 'hotReplenishment',
        'value.serialNumber': 'HOTREPLENISHMENT',
        'value.replenishFrontZone.value': { $ne: '' },

        'value.itemStoreCode.value': storeCode
    }

    if (level === "1" && departmentLevelCode !== "" && departmentLevelCode !== "ALL") {
        queryMatchHotReplenishment["value.itemDepartmentCode.value"] = departmentLevelCode;
    }
    if (level === "2" && departmentLevelCode !== "" && departmentLevelCode !== "ALL") {
        queryMatchHotReplenishment["value.itemDepartmentSubGroup.value"] = departmentLevelCode;
    }
    if (level === "3" && departmentLevelCode !== "" && departmentLevelCode !== "ALL") {
        queryMatchHotReplenishment["value.itemDepartmentGroup.value"] = departmentLevelCode;
    }

    var queryGroupHotReplenishment = {
        _id: {
            upcCategoryCode: '$itemUPCCategoryCode',
            groupCategory: '$itemGroupCategory'
        },
        storeCode: { $first: '$itemStoreCode' },
        count: { $sum: 1 },
        amount: { $sum: '$replenishQuantity' }
    };


    var queryGroupHotReplenishment = {};
    if (mode === "UPC") {
        queryGroupHotReplenishment = {
            _id: {
                upcCategoryCode: '$value.itemUPCCategoryCode.value'
            },

            storeCode: { $first: '$value.itemStoreCode.value' },
            count: { $sum: 1 },
            amount: { $sum: '$value.replenishQuantity.value' }

        }

    }

    if (mode === "UPCSKU") {
        queryGroupHotReplenishment = {
            _id: {
                upcCategoryCode: '$value.itemUPCCategoryCode.value'
            },

            storeCode: { $first: '$value.itemStoreCode.value' },
            count: { $sum: 1 },
            amount: { $sum: '$value.replenishQuantity.value' }

        }

    }

    if (mode === "SKU") {
        queryGroupHotReplenishment = {
            _id: {
                skuCategoryCode: '$value.itemSKUCategoryCode.value'
            },
            storeCode: { $first: '$value.itemStoreCode.value' },
            count: { $sum: 1 },
            amount: { $sum: '$value.replenishQuantity.value' }
        }
    }

    // This just to get the total number of rows
    var hotReplenishmentTotal = 0;
    var hotReplenishmentTotalAmount = 0;
    db.getCollection('thingSnapshots')
        .aggregate([{ $match: queryMatchHotReplenishment },
            { $group: queryGroupHotReplenishment },

            {
                $group: {
                    _id: { storeCode: '$storeCode' },
                    count: { $sum: 1 },
                    amount: { $sum: '$amount' }
                }
            }

        ]).forEach(function(item) {
        item = JSON.parse(JSON.stringify(item));
        hotReplenishmentTotal = item.count;
        hotReplenishmentTotalAmount = item.amount;
    })


    // ***********************************************
    // ********* Total for Dressing Room *********
    // ***********************************************

    var queryMatchDressingRoom = {
        'value.thingTypeCode': 'dressingRoom',
        'value.serialNumber': 'DRESSINGROOM',

        'value.itemStoreCode.value': storeCode
    }

    if (level === "1" && departmentLevelCode !== "" && departmentLevelCode !== "ALL") {
        queryMatchDressingRoom["value.itemDepartmentCode.value"] = departmentLevelCode;
    }
    if (level === "2" && departmentLevelCode !== "" && departmentLevelCode !== "ALL") {
        queryMatchDressingRoom["value.itemDepartmentSubGroup.value"] = departmentLevelCode;
    }
    if (level === "3" && departmentLevelCode !== "" && departmentLevelCode !== "ALL") {
        queryMatchDressingRoom["value.itemDepartmentGroup.value"] = departmentLevelCode;
    }

    var queryGroupDressingRoom = {
        _id: {
            upcCategoryCode: '$itemUPCCategoryCode',
            groupCategory: '$itemGroupCategory'
        },
        storeCode: { $first: '$itemStoreCode' },
        count: { $sum: 1 },
        amount: { $sum: '$replenishQuantity' }
    };


    var queryGroupDressingRoom = {
        _id: null,
        quantity: { $sum: 1 },
        amount: { $sum: '$value.replenishQuantity.value' }

    };

    var dressingRoomTotal = 0;
    var dressingRoomTotalAmount = 0;
    db.getCollection('thingSnapshots').aggregate([{ $match: queryMatchDressingRoom },
        { $group: queryGroupDressingRoom }
    ]).forEach(function(item) {

        dressingRoomTotal = item.quantity;
        dressingRoomTotalAmount = item.amount

    })

    // ************************************************

    var data = [];
    data.push(replenishmentTotalAmount, hotReplenishmentTotalAmount, dressingRoomTotalAmount);

    var dataTotal = [];
    dataTotal.push(data);

    table.totalRows = 1;
    table.data = dataTotal;

    return table;
}