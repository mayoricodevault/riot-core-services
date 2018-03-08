function(options) {

    var table = new Object();
    table.options = JSON.parse(JSON.stringify(options));

    var skip = options.pageSize * (options.pageNumber - 1);
    var limit = options.pageSize;

    table.title = "Hot Replenishment";

    table.columnNames = [
        "item",
        "departmentCategoryName",
        "departmentCategoryCode",
        "departmentCategoryIcon",
        "amount",
        "id",
        "shopCount",
        "backCount",
        "departmentName",
        "departmentCode",
        "groupCategory",
        "groupCategoryIcon",
        "priority",

        "upcCategoryCode",
        "upcCategoryName",
        "mark",
        "price",

        "status",
        "box"

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
        'itemDepartmentCode': departmentCode,
        'replenishFrontZone': { $ne: '' }
    }

    if (groupCategory !== "") {
        queryMatch.itemGroupCategory = groupCategory;
    }

    var queryGroup = {
        _id: {
            upcCategoryCode: '$itemUPCCategoryCode',
            groupCategory: '$itemGroupCategory'
        },

        upcCategoryName: { $first: '$itemUPCCategoryName' },

        departmentCategoryName: { $first: '$itemDepartmentCategoryName' },
        departmentCategoryCode: { $first: '$itemDepartmentCategoryCode' },
        departmentCategoryIcon: { $first: '$itemDepartmentCategoryIcon' },

        departmentName: { $first: '$itemDepartmentName' },
        departmentCode: { $first: '$itemDepartmentCode' },

        storeCode: { $first: '$itemStoreCode' },
        storeName: { $first: '$itemStoreName' },
        mark: { $first: '$replenishLastDate' },

        status: { $first: '$itemStatus' },

        groupCategoryIcon: { $first: '$itemGroupCategoryIcon' },

        amount: { $sum: '$replenishQuantity' },
        shopCount: { $sum: '$replenishFrontCount' },
        backCount: { $sum: '$replenishBackCount' },
        price: { $sum: '$itemPrice' },

        box: { $sum: '$replenishBox' },

        count: { $sum: 1 }
    };

    // This just to get the total number of rows
    var totalRows = 0;
    var totalAmount = 0;
    db.getCollection('retailHotReplenishmentView')
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
        item = JSON.parse(JSON.stringify(item));
        totalRows = item.count;
        totalAmount = item.amount;
    })


    // Get Data for table
    var dataReplenish = [];
    db.getCollection('retailHotReplenishmentView')
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

            item.upcCategoryName + '-' + item.departmentCategoryName,
            item.departmentCategoryName,
            item.departmentCategoryCode,
            item.departmentCategoryIcon,
            item.amount,
            x.upcCategoryCode + '-' + item.departmentCategoryCode,
            item.shopCount,
            item.backCount,
            item.departmentName,
            item.departmentCode,
            x.groupCategory,
            item.groupCategoryIcon,
            0,
            x.upcCategoryCode,
            item.upcCategoryName,
            item.mark,
            item.price,

            item.status,
            item.box

        );

        dataReplenish.push(replenish);

    })

    table.totalRows = totalRows;
    table.totalAmount = totalAmount;
    table.data = dataReplenish;

    return table;
}