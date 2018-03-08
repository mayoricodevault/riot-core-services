/// Creted by: Omar Rivera

function(options) {
    var table = new Object();
    table.title = "Job Report";

    table.columnNames = ["start", "end", "result"];

    var start = new Date();
    db.loadServerScripts();


    RefreshItemsCount("SalesZoneType", "StockZoneType", "DressingZoneType", "Free", "item", "UPCSKU")

    RefreshReplenishment("upc", "sku", "dept", "upcCategory", "skuCategory", "replenishment", "REPLENISHMENT", "UPCSKU");
    //RefreshHotReplenishment("StockZoneType", "Free", "item", "upc", "dept", "upcCategory", "hotReplenishment", "HOTREPLENISHMENT");
    //RefreshSellThruReplenishment("Sold", "item", "upc", "dept", "upcCategory", "sellThruReplenishment", "SELLTHRUREPLENISHMENT");
    //RefreshDressingRoom("upc", "dept", "upcCategory", "dressingRoom", "DRESSINGROOM");

    var end = new Date();
    var row = [start, end, "Success"];
    table.totalRows = 1;
    table.data = [];
    table.data.push(row);

    return table;
}