/**
 * Blink Counts by SerialNumber
 */
function( options ) 
{
	 var table = new Object();
	 table.options = options;
	 table.title = "Blink Counts by SerialNumber";
	
	 var skip = options.pageSize * (options.pageNumber - 1);
	 var limit = options.pageSize;
	 
	 table.totalRows = db.getCollection( 'things' ).find().count();
	 
	var a = db.getCollection( 'thingSnapshots' ).aggregate(
	[
		//{ $match : { 'value.thingTypeCode' : 'default_rfid_thingtype' } },
		{
			$group : {
				_id : "$value.serialNumber",
				blinks_count : { $sum : 1 }
			}
		},
		{ $sort : { "blinks_count" : -1 } },
		{ $skip : skip },
		{ $limit : limit }
	]);
	 
	table.rowNames = [];
	 var data = [];
	 for( var i = 0; i < a._batch.length; i++ )
	 {
		 table.rowNames[i] = a._batch[i]._id;
	     data[i] = [ a._batch[i].blinks_count ];
	 }
	 
	 data.sort( function( a, b ) {  return ( a[1] == b[1] ? a[0] > b[0] : a[1] < b[1] ) } );
	 table.columnNames = [ "blinks" ];
	 table.data = data;
	 
	 return table;
}