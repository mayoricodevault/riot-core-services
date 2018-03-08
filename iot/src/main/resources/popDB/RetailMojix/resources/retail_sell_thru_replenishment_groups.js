/**
 * Created by orivera on 11/1/16.
 */
function(options) {
    var table = new Object();
    table.title = "Sell Thru Replenishment";
    table.options = JSON.parse(JSON.stringify(options));

    var skip = options.pageSize * (options.pageNumber - 1);
    var limit = options.pageSize;

    table.columnNames = [
        'item',
        'upc',
        "departmentCategoryName",
        "departmentCategoryCode",
        "departmentCategoryIcon",

        'price',
        'color',
        'colorHexadecimal',
        'id',
        'soldCount',
        'soldZone',
        "groupCategory",
        "groupCategoryIcon",
        "departmentName",
        "departmentCode",
        "upcCategoryName",
        "upcCategoryCode",
        'mark',
        'size',

        'hour',
        "imageFront",
        "imageBack",
        "imageSide"
    ];


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
        'itemDepartmentCode': departmentCode
    }

    if (groupCategory !== "") {
        queryMatch.itemGroupCategory = groupCategory;
    }

    // This just to get the total number of rows
    var c = db.getCollection('retailSellThruReplenishmentView').find(queryMatch).count();

    var dataReplenish = [];
    db.getCollection('retailSellThruReplenishmentView')
        .find(queryMatch).sort({ amount: -1 }).skip(skip).limit(limit).forEach(function(item) {
        item = JSON.parse(JSON.stringify(item));

        var replenish = [];

        replenish.push(

            item._id,
            item.itemUpc,
            item.itemDepartmentCategoryName,
            item.itemDepartmentCategoryCode,
            item.itemDepartmentCategoryIcon,

            item.itemPrice,
            item.itemColorName,
            item.itemColorHexadecimal,
            item._id,
            item.replenishSoldCount,
            item.replenishSoldZone,
            item.itemGroupCategory,
            item.itemGroupCategoryIcon,

            item.itemDepartmentName,
            item.itemDepartmentCode,
            item.itemUPCCategoryName,
            item.itemUPCCategoryCode,

            item.replenishLastDate,
            item.itemSize,
            item.itemSoldHour,

            item.itemFrontImage,
            item.itemBackImage,
            item.itemSideImage

        );

        dataReplenish.push(replenish);

    });

    table.totalRows = c;
    table.data = dataReplenish;

    return table;
}