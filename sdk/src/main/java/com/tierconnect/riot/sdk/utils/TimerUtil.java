package com.tierconnect.riot.sdk.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

public class TimerUtil
{
	private static Logger logger = Logger.getLogger(TimerUtil.class);
	List<Long> times = new ArrayList<Long>();

	String previousKey;

	Map<String, Long> map = new HashMap<String, Long>();

	public void mark()
	{
		times.add( System.currentTimeMillis() );
	}

	public long getTime( int index )
	{
		return times.get( index );
	}

	public long getDelt( int index )
	{
		return times.get( index + 1 ) - times.get( index );
	}

	public long getLastDelt()
	{
		return times.get( times.size() - 1 ) - times.get( times.size() - 2 );
	}

	public long getTotalDelt()
	{
		return times.get( times.size() - 1 ) - times.get( 0 );
	}

	/**
	 * This method saves in a file the results of times TimerUtil
	 * Only for tests purposes
	 * @param titles Titles of the columns
	 * @param path Path of the file
     */
	public void saveResults( String titles, String path, String serialNumber){
		String data = serialNumber+",";
		long total = 0;
		if( (times != null) && (!times.isEmpty()) ){
			for (int i = 0 ; i < times.size()-1 ; i++){
				data = data+this.getDelt(i)+",";
				total = total + this.getDelt(i);
			}
			data = data + total;
			File file = new File(path);
			BufferedWriter bw = null;
			try{
				if(file.exists()) {
					bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file,true),"UTF-8"));
					bw.newLine();
					bw.write(data);

				} else {
					bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),"UTF-8"));
					bw.write("SerialNumber,"+titles+",Total");
					bw.newLine();
					bw.write(data);
				}
				bw.close();

			}catch(Exception ex){
				logger.error(ex);
			}
		}
	}
}
