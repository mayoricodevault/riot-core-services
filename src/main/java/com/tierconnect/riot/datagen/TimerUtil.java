package com.tierconnect.riot.datagen;

public class TimerUtil
{
	long start;
	long t0; 
	long t1;
	
	double percent;
	double rate;
	String etas;
	
	public void start()
	{
		t0 = System.currentTimeMillis();
		start = t0;
	}
	
	public boolean step( long incount, long ncount, int maxCount )
	{
		long t1 = System.currentTimeMillis();
		if( t1 - t0 > 5000 )
		{
			percent = (100.0 * ncount / maxCount);
			rate = 1000.0 * ((double) (ncount - incount)) / (t1 - start);
			double eta = (maxCount - ncount) / rate;
			float[] t = this.toHHMMSS( eta );
			etas = String.format( "%02.0fh %02.0fm %02.0fs", t[0], t[1], t[2] );
			//System.out.print( String.format( "%s %.1f %.1f %s\n", formatter.format( ncount ), percent, rate, etas ) );
			t0 = t1;
			return true;
		}
		return false;
	}
	
	protected float[] toHHMMSS( double seconds )
	{
		float hh = (int) (seconds / 3600);
		float mm = (int) ((seconds % 3600) / 60);
		float ss = (float) ((seconds % 3600) % 60);
		return new float[] { hh, mm, ss };
	}
}
