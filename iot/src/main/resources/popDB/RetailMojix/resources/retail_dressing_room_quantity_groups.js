/**
 * Created by orivera on 11/8/16.
 * [Retail] Dressing Room Quantity Groups
 */
function(options) {
    var table = new Object();
    table.title = "Dressing Room Count";
    table.options = JSON.parse(JSON.stringify(options));

    //----------------------------------------------


    table.columnNames = [
        "number",
        "room",
        "quantity",
        "storeCode",
        "storeName",
        "totalAmount"
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

    var queryGroup = {
        _id: {
            room: '$replenishRoom',
        },

        storeCode: { $first: '$itemStoreCode' },
        storeName: { $first: '$itemStoreName' },
        quantity: { $sum: 1 },
        amount: { $sum: '$replenishQuantity' },
    };

    var c = 1;

    var dataReplenish = [];

    db.retailDressingRoomView.aggregate([{ $match: queryMatch },
        { $group: queryGroup }
    ])
        .forEach(function(item) {
            var replenish = [];
            var x = item._id;
            replenish.push(
                c,
                x.room,
                item.quantity,
                item.storeCode,
                item.storeName,
                item.amount
            )
            c++;
            dataReplenish.push(replenish);

        })

    table.totalRows = c - 1;
    table.data = dataReplenish;

    return table;
}