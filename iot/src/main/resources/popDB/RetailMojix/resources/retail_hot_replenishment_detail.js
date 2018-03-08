/**
 * Created by orivera on 10/27/16.
 * [Retail] Hot Replenishment Detail
 */
function(options) {

    var table = new Object();
    table.options = JSON.parse(JSON.stringify(options));

    var skip = options.pageSize * (options.pageNumber - 1);
    var limit = options.pageSize;

    table.title = "Hot Replenishment Detail";

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
        'backCount',
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
        'initialQuantity',
        'box',
        'status',
        "imageFront",
        "imageBack",
        "imageSide"
    ];

    var store = "3803";
    if (options.hasOwnProperty('filters') && options.filters.hasOwnProperty('storeCode')) {
        store = options.filters.storeCode;
    }


    var upcCategory = "A001";
    if (options.hasOwnProperty('filters') && options.filters.hasOwnProperty('upcCategory')) {
        upcCategory = options.filters.upcCategory;
    }


    var departmentCategoryCode = "T09";
    if (options.hasOwnProperty('filters') && options.filters.hasOwnProperty('departmentCategoryCode')) {
        departmentCategoryCode = options.filters.departmentCategoryCode;
    }



    var queryMatch = {
        'itemStoreCode': store,
        'itemUPCCategoryCode': upcCategory,
        'itemDepartmentCategoryCode': departmentCategoryCode,

        'replenishFrontZone': { $ne: '' }
    }

    var c = db.getCollection('retailHotReplenishmentView').find(queryMatch).count();

    var totalAmount = 0;
    db.getCollection('retailHotReplenishmentView').aggregate([
        { $match: queryMatch },
        {
            $group: {
                _id: null,
                totalAmount: { $sum: '$replenishQuantity' }
            }
        }
    ]).forEach(function(x) {
        totalAmount = x.totalAmount;
    })

    var dataReplenish = [];
    db.getCollection('retailHotReplenishmentView')
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
            item.replenishBackCount,
            item.itemGroupCategory,
            item.itemGroupCategoryIcon,

            item.itemDepartmentName,
            item.itemDepartmentCode,
            item.itemUPCCategoryName,
            item.itemUPCCategoryCode,

            item.replenishBackZone,
            item.replenishFrontZone,
            item.replenishLastDate,
            item.itemSize,
            item.replenishQuantity,
            item.replenishBox,
            item.itemStatus,

            item.itemFrontImage,
            item.itemBackImage,
            item.itemSideImage
        );

        dataReplenish.push(replenish);

    });


    table.totalRows = c;
    table.totalAmount = totalAmount;
    table.data = dataReplenish;

    return table;
}