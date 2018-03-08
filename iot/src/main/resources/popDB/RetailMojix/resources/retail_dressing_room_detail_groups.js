/**
 * Created by orivera on 11/8/16.
 * [Retail] Dressing Romm Detail
 */
function(options) {
    var table = new Object();
    table.title = "Dressing Room Detail";
    table.options = JSON.parse(JSON.stringify(options));

    var skip = options.pageSize * (options.pageNumber - 1);
    var limit = options.pageSize;

    table.columnNames = [
        'item',
        'upc',
        "departmentCategoryName",
        "departmentCategoryCode",
        "departmentCategoryIcon",
        'amount',
        'min',
        'max',
        'price',
        'color',
        'colorHexadecimal',
        'id',
        'shopCount',
        'dressingCount',
        "groupCategory",
        "groupCategoryIcon",
        "departmentName",
        "departmentCode",
        "upcCategoryName",
        "upcCategoryCode",
        'from',
        'to',
        'mark',
        'size',
        'room',
        "imageFront",
        "imageBack",
        "imageSide"

    ];


    var dressing = "DressingRoom";
    if (options.hasOwnProperty('filters') && options.filters.hasOwnProperty('dressing')) {
        dressing = options.filters.dressing;
    }


    var store = "3803";
    if (options.hasOwnProperty('filters') && options.filters.hasOwnProperty('storeCode')) {
        store = options.filters.storeCode;
    }

    var departmentCode = "GCMEN";
    if (options.hasOwnProperty('filters') && options.filters.hasOwnProperty('departmentCode')) {
        departmentCode = options.filters.departmentCode;
    }

    var groupCategory = "gcmen";
    if (options.hasOwnProperty('filters') && options.filters.hasOwnProperty('groupCategory')) {
        groupCategory = options.filters.groupCategory;
    }

    var queryMatch = {
        'itemStoreCode': store,
        'itemDepartmentCode': departmentCode,

        'replenishRoom': dressing
    }

    if (groupCategory !== "") {
        queryMatch.itemGroupCategory = groupCategory;
    }

    // This just to get the total number of rows
    var totalRows = 0;
    var totalAmount = 0;
    db.getCollection('retailDressingRoomView')
        .aggregate([{ $match: queryMatch },


            {
                $group: {
                    _id: { storeCode: '$itemStoreCode' },
                    count: { $sum: 1 },
                    amount: { $sum: '$replenishQuantity' }
                }
            }


        ]).forEach(function(item) {

        totalRows = item.count;
        totalAmount = item.amount;
    })

    var dataReplenish = [];
    db.getCollection('retailDressingRoomView')
        .find(queryMatch).skip(skip).limit(limit).forEach(function(item) {
        item = JSON.parse(JSON.stringify(item));

        var replenish = [];

        replenish.push(

            item._id,
            item.itemUpc,
            item.itemDepartmentCategoryName,
            item.itemDepartmentCategoryCode,
            item.itemDepartmentCategoryIcon,
            item.replenishQuantity,
            item.replenishMin,
            item.replenishMax,
            item.itemPrice,
            item.itemColorName,
            item.itemColorHexadecimal,
            item._id,
            item.replenishFrontCount,
            item.replenishDressingCount,
            item.itemGroupCategory,
            item.itemGroupCategoryIcon,

            item.itemDepartmentName,
            item.itemDepartmentCode,
            item.itemUPCCategoryName,
            item.itemUPCCategoryCode,

            item.replenishDressingZone,
            item.replenishFrontZone,


            item.replenishLastDate,
            item.itemSize,
            item.replenishRoom,
            item.itemFrontImage,
            item.itemBackImage,
            item.itemSideImage

        );

        dataReplenish.push(replenish);

    });

    table.totalRows = totalRows;
    table.totalAmount = totalAmount;
    table.data = dataReplenish;

    return table;
}