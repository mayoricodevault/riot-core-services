function(options) {
    var table = new Object();
    table.title = "Replenishment Detail";
    table.options = JSON.parse(JSON.stringify(options));

    var skip = options.pageSize * (options.pageNumber - 1);
    var limit = options.pageSize;

    //----------------------------------------------

    table.columnNames = [
        'itemName',
        'itemDepartmentName',
        'itemDepartmentCode',
        'itemDepartmentIcon',
        'replenishQuantity',
        'replenishId',
        'replenishFrontCount',
        'replenishBackCount',
        'itemDepartmentGroupIcon',
        'itemDepartmentGroupName',
        'itemDepartmentGroup',
        'itemDepartmentSubGroupIcon',
        'itemDepartmentSubGroupName',
        'itemDepartmentSubGroup',
        'replenishBackZone',
        'replenishFrontZone',
        'replenishLastDate',
        'replenishQuantity',
        'itemUPCCategoryName',
        'itemUPCCategoryCode',
        'itemUPC',
        'itemSizeUPC',
        'replenishMaxUPC',
        'replenishMinUPC',
        'itemMaterialUPC',
        'itemBrandUPC',
        'itemPriceUPC',
        'itemColorUPC',
        'itemColorHexadecimalUPC',
        'itemFrontImageUPC',
        'itemBackImageUPC',
        'itemSideImageUPC',
        'itemSKUCategoryName',
        'itemSKUCategoryCode',
        'itemSKU',
        'itemSizeSKU',
        'replenishMaxSKU',
        'replenishMinSKU',
        'itemMaterialSKU',
        'itemBrandSKU',
        'itemPriceSKU',
        'itemColorSKU',
        'itemColorHexadecimalSKU',
        'itemFrontImageSKU',
        'itemBackImageSKU',
        'itemSideImageSKU'
    ];

    var storeCode = "SM";
    if (options.hasOwnProperty('filters') && options.filters.hasOwnProperty('storeCode')) {
        storeCode = options.filters.storeCode;
    }

    var categoryUpcSku = "0504A";
    if (options.hasOwnProperty('filters') && options.filters.hasOwnProperty('categoryUpcSku')) {
        categoryUpcSku = options.filters.categoryUpcSku;
    }

    var departmentCode = "T19";
    if (options.hasOwnProperty('filters') && options.filters.hasOwnProperty('departmentCode')) {
        departmentCode = options.filters.departmentCode;
    }

    var mode = "UPC"; // UPC | SKU | UPCSKU | UPCONLY | SKUONLY
    if (options.hasOwnProperty('filters') && options.filters.hasOwnProperty('mode')) {
        mode = options.filters.mode;
    }

    var queryMatch = {
        'value.thingTypeCode': 'replenishment',
        'value.serialNumber': 'REPLENISHMENT',

        'value.itemStoreCode.value': storeCode,
        'value.itemDepartmentCode.value': departmentCode
    }

    if (mode === "UPC") {
        queryMatch["value.itemUPCCategoryCode.value"] = categoryUpcSku;
    }

    if (mode === "SKU") {
        queryMatch["value.itemSKUCategoryCode.value"] = categoryUpcSku;
    }

    if (mode === "UPCSKU") {
        queryMatch["value.itemUPCCategoryCode.value"] = categoryUpcSku;
    }


    var c = db.getCollection('thingSnapshots').find(queryMatch).count();

    var totalAmount = 0;
    db.getCollection('thingSnapshots').aggregate([
        { $match: queryMatch },
        {
            $group: {
                _id: null,
                totalAmount: { $sum: '$value.replenishQuantity.value' }
            }
        }
    ]).forEach(function(x) {
        totalAmount = x.totalAmount;
    })

    var dataReplenish = [];
    db.getCollection('thingSnapshots')
        .find(queryMatch).skip(skip).limit(limit).forEach(function(item) {
        item = item.value;

        var replenish = [];

        replenish.push(
            item.itemName.value,
            item.itemDepartmentName.value,
            item.itemDepartmentCode.value,
            item.itemDepartmentIcon.value,
            item.replenishQuantity.value,
            item.replenishId.value,
            item.replenishFrontCount.value,
            item.replenishBackCount.value,

            item.itemDepartmentGroupIcon.value,
            item.itemDepartmentGroupName.value,
            item.itemDepartmentGroup.value,
            item.itemDepartmentSubGroupIcon.value,
            item.itemDepartmentSubGroupName.value,
            item.itemDepartmentSubGroup.value,

            item.replenishBackZone.value,
            item.replenishFrontZone.value,
            item.replenishLastDate.value,
            item.replenishQuantity.value,

            item.itemUPCCategoryName.value,
            item.itemUPCCategoryCode.value,
            item.itemUPC.value,
            item.itemSizeUPC.value,
            item.replenishMaxUPC.value,
            item.replenishMinUPC.value,
            item.itemMaterialUPC.value,
            item.itemBrandUPC.value,
            item.itemPriceUPC.value,
            item.itemColorUPC.value,
            item.itemColorHexadecimalUPC.value,
            item.itemFrontImageUPC.value,
            item.itemBackImageUPC.value,
            item.itemSideImageUPC.value,

            item.itemSKUCategoryName.value,
            item.itemSKUCategoryCode.value,
            item.itemSKU.value,
            item.itemSizeSKU.value,
            item.replenishMaxSKU.value,
            item.replenishMinSKU.value,
            item.itemMaterialSKU.value,
            item.itemBrandSKU.value,
            item.itemPriceSKU.value,
            item.itemColorSKU.value,
            item.itemColorHexadecimalSKU.value,
            item.itemFrontImageSKU.value,
            item.itemBackImageSKU.value,
            item.itemSideImageSKU.value


        );

        dataReplenish.push(replenish);

    });


    table.totalRows = c;
    table.totalAmount = totalAmount;
    table.data = dataReplenish;

    return table;
}