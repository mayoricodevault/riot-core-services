/************************************
* Report to daily detect count
*************************************/

function( options )
{

	var table = new Object();

	// required 
	table.options = options;

	// optional, but recommended
	table.title = "Daily Detect Count";

	// optional
	table.labelX = "Things";

	// optional
	table.labelY = "Hours";

	// optional
	table.labelZ = "Count";

	// optional
	table.columnNames = [  "Count" ];

	// optional
	//table.rowNames = [ "Alpha", "Bravo", "Charlie", "Delta" ];

	// required
	//table.data = [ [ 1, 2, 3, 4 ], [ 4, 5, 6, 7 ], [ 7, 8, 9, 10 ], [ 10, 11, 12, 13 ] ];

	var start = new Date("01/05/2016 00:00:00 GMT-0800").getTime();
	var numberOfDays = 30;
	var resultGlobal = [];
	var labelsLeft = [];

	for (var i = 1; i <= numberOfDays; i++)
	{
		var result = [];
		var count = 0;
		var end = start + 86400000;
		db.thingSnapshotIds.find(
			{"blinks": {"$elemMatch": {"time": {"$lte": end - 1, "$gte": start}}}},
			{"blinks": {"$elemMatch": {"time": {"$lte": end - 1, "$gte": start}}}}).map(
			function (x) {
				count++;
			}
		);

		var utcTime = new Date(start);
		var dateReport =  utcTime.getTime() + utcTime.getTimezoneOffset() * 60000;

		var date = ((new Date(dateReport)).getMonth() + 1) + "/" + (new Date(dateReport)).getDate() + "/" + ((new Date(dateReport)).getFullYear() ) ;

		var result = [];
		labelsLeft.push( date);
		result.push( count);
		resultGlobal.push(result);
		var start = end;
	}

	table.rowNames = labelsLeft;
	table.data = resultGlobal;
	return table;
}