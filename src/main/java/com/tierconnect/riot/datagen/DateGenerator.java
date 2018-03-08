package com.tierconnect.riot.datagen;

import org.apache.commons.lang.time.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

//generate a date between today a two weeks ago
public class DateGenerator
{
	private Date start;
	private long current;
	private long delt;

	public DateGenerator( int count )
	{
		// this.start = DateUtils.addWeeks( end, -2 );

		Date end = null;
		
		SimpleDateFormat sdf = new SimpleDateFormat( "yyyy/MM/dd" );
		try
		{
			this.start = sdf.parse( "2016/01/01" );
			end = DateUtils.addDays( start, 100 );
			current = start.getTime();
			long diffLong = end.getTime() - start.getTime();
			delt = diffLong / count;
		}
		catch( java.text.ParseException e )
		{
            // this catch is currently unreachable because parse is given "2016/01/01"
		}

		//logger.info( "Random dates start " + start + ", end " + end );
	}

	public Date generate()
	{
		// return new Date( start.getTime() + (new Random().nextInt( diff ) + 1)
		// );
		current += delt;
		return new Date( current );
	}
}
