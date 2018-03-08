/**
 * Created by orivera on 10/27/16.
 */
function(options) {

    var table = new Object();
    table.title = "Replenishment";
    table.options = JSON.parse(JSON.stringify(options));

    var skip = options.pageSize * (options.pageNumber - 1);
    var limit = options.pageSize;

    //----------------------------------------------

    table.columnNames = [

        'itemDepartmentGroupName',
        'itemDepartmentGroup',
        'itemDepartmentGroupIcon',
        'itemDepartmentSubGroupName',
        'itemDepartmentSubGroup',
        'itemDepartmentSubGroupIcon',
        'itemDepartmentName',
        'itemDepartmentCode',
        'itemDepartmentIcon',
        'itemStoreCode',
        'itemStoreName',
        'replenishLastDate',
        'replenishQuantity',
        'replenishFrontCount',
        'replenishBackCount',
        'count',
        'itemPriceUPC',
        'itemUPCCategoryCode',
        'itemUPCCategoryName',
        'itemPriceSKU',
        'itemSKUCategoryCode',
        'itemSKUCategoryName'

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

    var type = "QUANTITY"; // QUANTITY | PRICE
    if (options.hasOwnProperty('filters') && options.filters.hasOwnProperty('type')) {
        type = options.filters.type;
    }

    var queryMatch = {
        'value.thingTypeCode': 'replenishment',
        'value.serialNumber': 'REPLENISHMENT',

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

    var querySort = {};

    // id UPCONLY and SKUONLY set queryGroup = {}
    // UPC is by default when "UPCSKU" is set.
    var queryGroup = {};
    if (mode === "UPC") {
        queryGroup = {
            _id: {
                upcCategoryCode: '$value.itemUPCCategoryCode.value'
            },
            priceUPC: { $sum: '$value.itemPriceUPC.value' },
            upcCategoryName: { $first: '$value.itemUPCCategoryName.value' }

        }

    }

    if (mode === "UPCSKU") {
        queryGroup = {
            _id: {
                upcCategoryCode: '$value.itemUPCCategoryCode.value'
            },

            priceUPC: { $sum: '$value.itemPriceUPC.value' },
            upcCategoryName: { $first: '$value.itemUPCCategoryName.value' },
            priceSKU: { $sum: '$value.itemPriceSKU.value' },
            skuCategoryName: { $first: '$value.itemSKUCategoryName.value' }

        }

    }

    if (mode === "SKU") {
        queryGroup = {
            _id: {
                skuCategoryCode: '$value.itemSKUCategoryCode.value'
            },
            priceSKU: { $sum: '$value.itemPriceSKU.value' },
            skuCategoryName: { $first: '$value.itemSKUCategoryName.value' }
        }
    }

    queryGroup2 = {
        departmentGroupName: { $first: '$value.itemDepartmentGroupName.value' },
        departmentGroup: { $first: '$value.itemDepartmentGroup.value' },
        departmentGroupIcon: { $first: '$value.itemDepartmentGroupIcon.value' },
        departmentSubGroupName: { $first: '$value.itemDepartmentSubGroupName.value' },
        departmentSubGroup: { $first: '$value.itemDepartmentSubGroup.value' },
        departmentSubGroupIcon: { $first: '$value.itemDepartmentSubGroupIcon.value' },
        departmentName: { $first: '$value.itemDepartmentName.value' },
        departmentCode: { $first: '$value.itemDepartmentCode.value' },
        departmentIcon: { $first: '$value.itemDepartmentIcon.value' },
        storeCode: { $first: '$value.itemStoreCode.value' },
        storeName: { $first: '$value.itemStoreName.value' },
        mark: { $first: '$value.replenishLastDate.value' },
        amount: { $sum: '$value.replenishQuantity.value' },
        shopCount: { $sum: '$value.replenishFrontCount.value' },
        backCount: { $sum: '$value.replenishBackCount.value' },
        count: { $sum: 1 }
    }

    Object.assign(queryGroup, queryGroup2);

    // This just to get the total number of rows
    var totalRows = 0;
    var totalAmount = 0;
    db.getCollection('thingSnapshots')
        .aggregate([{ $match: queryMatch },
            { $group: queryGroup },

            {
                $group: {
                    _id: { storeCode: '$storeCode' },
                    count: { $sum: 1 },
                    amount: { $sum: '$amount' }
                }
            }


        ]).forEach(function(item) {

        totalRows = item.count;
        totalAmount = item.amount;
    })

    // Get Data for table

    var dataReplenish = [];
    db.getCollection('thingSnapshots')
        .aggregate([{ $match: queryMatch },
            { $group: queryGroup },
            { $sort: { amount: -1 } },

            { $skip: skip },
            { $limit: limit },
        ]).forEach(function(item) {
        item = JSON.parse(JSON.stringify(item));

        var x = item._id;
        var replenish = [];

        replenish.push(

            item.departmentGroupName,
            item.departmentGroup,
            item.departmentGroupIcon,
            item.departmentSubGroupName,
            item.departmentSubGroup,
            item.departmentSubGroupIcon,
            item.departmentName,
            item.departmentCode,
            item.departmentIcon,
            item.storeCode,
            item.storeName,
            item.mark,
            item.amount,
            item.shopCount,
            item.backCount,
            item.count
        );


        if (mode === "UPCSKU") {
            replenish.push(
                item.priceUPC,
                x.upcCategoryCode,
                item.upcCategoryName,
                item.priceSKU,
                "", //x.skuCategoryCode,
                item.skuCategoryName
            )
        }
        if (mode === "UPC") {
            replenish.push(
                item.priceUPC,
                x.upcCategoryCode,
                item.upcCategoryName,
                "",
                "",
                ""
            )
        }
        if (mode === "SKU") {
            replenish.push(
                "",
                "",
                "",
                item.priceSKU,
                x.skuCategoryCode,
                item.skuCategoryName

            )
        }

        dataReplenish.push(replenish);

    })


    table.totalRows = totalRows;
    table.totalAmount = totalAmount;
    table.data = dataReplenish;

    return table;
}