/************************************
 * Report to daily detect count
 *************************************/
function( options )
{
	var table = new Object();

	// required
	table.options = options;

	// optional, but recommended
	table.title = "Udf By Hour Count";

	// optional
	table.labelX = "Zones";

	// optional
	table.labelY = "Hours";

	// optional
	table.labelZ = "Count";

	//Logic of the script
	var fromH = 8;
	var start = new Date("02/02/2016  "+ fromH + ":00:00 GMT-0400").getTime();
	var fromH = 8;
	var toH = 23;
	var udf = "zone";

	var hours = [];
	var udfValues = [];
	var udfPath = "value." + udf + ".value";

	if (udf.toLowerCase() == "zone") {
		var udfPath = udfPath + ".name"
	}

	var out = {};
	var header=[];
	for (var i = fromH; i <= toH; i++) {

		var count = 0;
		var end = start + 3600000;

		var hourFrom = (new Date(start)).getHours();
		var hourTo = (new Date(end)).getHours();

		if (i == fromH) {
			var dummy = db.thingSnapshots.distinct(udfPath).map(
				function (z) {
					udfValues.push(z);
				}
			);
			for (var j = fromH; j <= toH; j++) {
				hours.push(j);
			}
			for (var h in hours) {
				var a = {};
				a["name"] = hours[h];
				for (var u in udfValues)
					a[udfValues[u]] = "0";
				out[hours[h]] = a;
			}
			for (var u in udfValues) {
				//header += "," + udfValues[u];
				header.push(udfValues[u]);
			}
		}

		var ids = db.thingSnapshotIds.find(
			{"blinks": {"$elemMatch": {"time": {"$lte": end - 1, "$gte": start}}}},
			{"blinks": {"$elemMatch": {"time": {"$lte": end - 1, "$gte": start}}}}).map(
			function (x) {
				return x.blinks[0].blink_id;
			}
		);

		var dummy = db.thingSnapshots.aggregate([
			{
				"$match": {"$and": [{"_id": {"$in": ids}}]}
			},
			{
				"$group": {
					"_id": "$" + udfPath,
					"count": {"$sum": 1}
				}
			}
		]);

		if (dummy.result != null) {
			dummy = dummy.result;
		}
		dummy.map(function (a) {
			out[i][a._id] = a.count;
		});

		var headerColumn = [];
		var result = [];
		var headerRow = [];

		if (i == toH) {
			headerColumn.push(header);
			for (var h in hours) {
				var dataOne = [];
				headerRow.push(out[hours[h]]["name"]);
				for (var u in udfValues) {
					dataOne.push(out[hours[h]][udfValues[u]]);
				}
				result.push(dataOne);
			}
		}
		var start = end;
	}


	table.columnNames = header;
	table.rowNames = headerRow;
	table.data = result;

	return table;
}