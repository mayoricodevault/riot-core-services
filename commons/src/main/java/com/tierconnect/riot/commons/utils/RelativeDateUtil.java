package com.tierconnect.riot.commons.utils;

import com.tierconnect.riot.commons.Constants;
import org.apache.log4j.Logger;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.*;

/**
 * Created by rchirinos on 21/9/2015.
 * This class calculate the relative date based on defined codes
 */
public class RelativeDateUtil
{
	static Logger logger = Logger.getLogger( RelativeDateUtil.class );

	private Date startDate = null;
	private Date endDate = null;

	//Enum for global times
	public enum Time {
		TODAY("TODAY"),
		MINUTE("MINUTE"),
		HOUR("HOUR"),
		DAY("DAY"),
		WEEK("WEEK"),
		MONTH("MONTH"),
		YEAR("YEAR");

		public String value;
		Time(String value)
		{
			this.value = value;
		}
	}
	//enum for codes of relative dates
	public enum RelativeDate {

		THIS_HOUR("THIS_HOUR"),
		TODAY("TODAY"),
		THIS_WEEK("THIS_WEEK"),
		THIS_MONTH("THIS_MONTH"),
		THIS_YEAR("THIS_YEAR"),

		LAST_HOUR_1("LAST_HOUR_1"),
		LAST_DAY_1("LAST_DAY_1"),
		LAST_WEEK_1("LAST_WEEK_1"),
		LAST_MONTH_1("LAST_MONTH_1"),
		LAST_YEAR_1("LAST_YEAR_1"),

		ALL_DAY("ALL_DAY"),

		AGO_HOUR_1("AGO_HOUR_1"),
		AGO_DAY_1("AGO_DAY_1"),
		AGO_WEEK_1("AGO_WEEK_1"),
		AGO_MONTH_1("AGO_MONTH_1"),
		AGO_YEAR_1("AGO_YEAR_1"),

		AS_OF_HOUR_1("AS_OF_HOUR_1"),
		AS_OF_DAY_1("AS_OF_DAY_1"),
		AS_OF_WEEK_1("AS_OF_WEEK_1"),
		AS_OF_MONTH_1("AS_OF_MONTH_1"),
		AS_OF_YEAR_1("AS_OF_YEAR_1");

		public String value;
		RelativeDate(String value)
		{
			this.value = value;
		}
	}

	/*********************
	 * @method setRelativeDate
	 * Method to calculate the period of time
	 * based on relative dates
	 * ********************/
	public  Map<String, Object> setRelativeDate( String codeRelativeDate, Date referenceDate, String offsetTimeZone)
	{
		Map<String, Object> response = new HashMap<>();
		if(offsetTimeZone == null) {
			offsetTimeZone = Constants.DEFAULT_TIME_ZONE;
		}
		Calendar calendarDate = Calendar.getInstance(TimeZone.getTimeZone(ZoneId.of(offsetTimeZone)));
		Calendar calendarDateIni = new GregorianCalendar(TimeZone.getTimeZone(ZoneId.of(offsetTimeZone)));
		calendarDateIni.set(1900,0,1);

		calendarDate.setFirstDayOfWeek(Calendar.MONDAY);
		calendarDate.setTime(referenceDate);
		calendarDate.set(Calendar.MILLISECOND, 0);

		Calendar startDateCalendar = null;
		Calendar endDateCalendar = null;

		if(codeRelativeDate.equals( RelativeDate.THIS_HOUR.value))
		{
			startDateCalendar = RelativeDateUtil.startTimeOfThisTime( calendarDate, Time.HOUR.value );
			endDateCalendar = calendarDate;
		}
		else if(codeRelativeDate.equals( RelativeDate.TODAY.value))
		{
			startDateCalendar = RelativeDateUtil.startTimeOfThisTime( calendarDate, Time.TODAY.value );
			endDateCalendar = calendarDate;
		}
		else if(codeRelativeDate.equals( RelativeDate.THIS_WEEK.value))
		{
			startDateCalendar = RelativeDateUtil.startTimeOfThisTime( calendarDate, Time.WEEK.value );
			endDateCalendar = calendarDate;
		}
		else if(codeRelativeDate.equals( RelativeDate.THIS_MONTH.value))
		{
			startDateCalendar = RelativeDateUtil.startTimeOfThisTime(calendarDate, Time.MONTH.value);
			endDateCalendar = calendarDate;
		}
		else if(codeRelativeDate.equals( RelativeDate.THIS_YEAR.value))
		{
			startDateCalendar = RelativeDateUtil.startTimeOfThisTime(calendarDate, Time.YEAR.value);
			endDateCalendar = calendarDate;
		}

		else if(codeRelativeDate.equals( RelativeDate.LAST_HOUR_1.value))
		{
			startDateCalendar = RelativeDateUtil.firstTimeOfLastTime(calendarDate, -1, Time.HOUR.value);
			endDateCalendar = RelativeDateUtil.endTimeOfLastTime(calendarDate, -1, Time.HOUR.value);
		}
		else if(codeRelativeDate.equals( RelativeDate.LAST_DAY_1.value))
		{
			startDateCalendar = RelativeDateUtil.firstTimeOfLastTime(calendarDate, -1, Time.DAY.value);
			endDateCalendar = RelativeDateUtil.endTimeOfLastTime(calendarDate, -1, Time.DAY.value);
		}
		else if(codeRelativeDate.equals( RelativeDate.LAST_WEEK_1.value))
		{
			startDateCalendar = RelativeDateUtil.firstTimeOfLastTime(calendarDate, -1, Time.WEEK.value);
			endDateCalendar = RelativeDateUtil.endTimeOfLastTime(calendarDate, -1, Time.WEEK.value);
		}
		else if(codeRelativeDate.equals( RelativeDate.LAST_MONTH_1.value))
		{
			startDateCalendar = RelativeDateUtil.firstTimeOfLastTime(calendarDate, -1, Time.MONTH.value);
			endDateCalendar = RelativeDateUtil.endTimeOfLastTime(calendarDate, -1, Time.MONTH.value);
		}
		else if(codeRelativeDate.equals( RelativeDate.LAST_YEAR_1.value))
		{
			startDateCalendar = RelativeDateUtil.firstTimeOfLastTime(calendarDate, -1, Time.YEAR.value);
			endDateCalendar = RelativeDateUtil.endTimeOfLastTime(calendarDate, -1, Time.YEAR.value);
		}

		else if(codeRelativeDate.equals( RelativeDate.ALL_DAY.value))
		{
			startDateCalendar = RelativeDateUtil.firstTimeOfLastTime(calendarDate, 0, Time.DAY.value);
			endDateCalendar = RelativeDateUtil.endTimeOfLastTime(calendarDate, 0, Time.DAY.value);
		}

		else if(codeRelativeDate.equals( RelativeDate.AGO_HOUR_1.value))
		{
			startDateCalendar = RelativeDateUtil.firstTimeOfTimeAgo(calendarDate, -1, Time.HOUR.value);
			endDateCalendar = calendarDate;
		}else if(codeRelativeDate.equals( RelativeDate.AGO_DAY_1.value))
		{
			startDateCalendar = RelativeDateUtil.firstTimeOfTimeAgo(calendarDate, -1, Time.DAY.value);
			endDateCalendar = calendarDate;
		}else if(codeRelativeDate.equals( RelativeDate.AGO_WEEK_1.value))
		{
			startDateCalendar = RelativeDateUtil.firstTimeOfTimeAgo(calendarDate, -1, Time.WEEK.value);
			endDateCalendar = calendarDate;
		}else if(codeRelativeDate.equals( RelativeDate.AGO_MONTH_1.value))
		{
			startDateCalendar = RelativeDateUtil.firstTimeOfTimeAgo(calendarDate, -1, Time.MONTH.value);
			endDateCalendar = calendarDate;
		}else if(codeRelativeDate.equals( RelativeDate.AGO_YEAR_1.value))
		{
			startDateCalendar = RelativeDateUtil.firstTimeOfTimeAgo(calendarDate, -1, Time.YEAR.value);
			endDateCalendar = calendarDate;
		}

		else if(codeRelativeDate.equals( RelativeDate.AS_OF_HOUR_1.value))
		{
			startDateCalendar = calendarDateIni;
			endDateCalendar = RelativeDateUtil.endTimeOfLastTime(calendarDate, -1, Time.HOUR.value);
		}
		else if(codeRelativeDate.equals( RelativeDate.AS_OF_DAY_1.value))
		{
			startDateCalendar = calendarDateIni;
			endDateCalendar = RelativeDateUtil.endTimeOfLastTime(calendarDate, -1, Time.DAY.value);
		}
		else if(codeRelativeDate.equals( RelativeDate.AS_OF_WEEK_1.value))
		{
			startDateCalendar = calendarDateIni;
			endDateCalendar = RelativeDateUtil.endTimeOfLastTime(calendarDate, -1, Time.WEEK.value);
		}
		else if(codeRelativeDate.equals( RelativeDate.AS_OF_MONTH_1.value))
		{
			startDateCalendar = calendarDateIni;
			endDateCalendar = RelativeDateUtil.endTimeOfLastTime(calendarDate, -1, Time.MONTH.value);
		}
		else if(codeRelativeDate.equals( RelativeDate.AS_OF_YEAR_1.value))
		{
			startDateCalendar = calendarDateIni;
			endDateCalendar = RelativeDateUtil.endTimeOfLastTime(calendarDate, -1, Time.YEAR.value);
		}else
		{
			throw new DateTimeException( "Relative Date Code: '" + codeRelativeDate + "' is not defined.");
		}

		this.startDate =  startDateCalendar.getTime();
		endDateCalendar.set(Calendar.MILLISECOND, 999);
		this.endDate = endDateCalendar.getTime();
		logger.info( "Relative Date Code: "+ codeRelativeDate );
		logger.info( "Start Date : " + startDateCalendar.getTime() +", "+startDate.getTime());
		logger.info( "End Date   : " + endDateCalendar.getTime() +", "+endDate.getTime());
		return response;
	}


	/******************************
	 * @method  startTimeOfThisTime
	 * @description This method returns the start time of this Time
	 *****************************/
	public static Calendar startTimeOfThisTime(Calendar c, String time)
	{
		c = (Calendar) c.clone();
		if(time.equals(Time.TODAY.value))
		{
			c.set(Calendar.HOUR_OF_DAY, 0);
		}else if(time.equals(Time.HOUR.value))
		{
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);
		}else if(time.equals(Time.WEEK.value))
		{
			c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
			c.set(Calendar.HOUR_OF_DAY, 0);
		}else if(time.equals(Time.MONTH.value))
		{
			c.set(Calendar.DAY_OF_MONTH, 1);
			c.set(Calendar.HOUR_OF_DAY, 0);
		}else if(time.equals(Time.YEAR.value))
		{
			c.set(Calendar.DAY_OF_YEAR, 1);
			c.set(Calendar.HOUR_OF_DAY, 0);
		}
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		return c;
	}

	/******************************
	 * @method  firstTimeOfLastTime
	 * @description This method returns the first time of the last time
	 *****************************/
	public static Calendar firstTimeOfLastTime(Calendar c, int previousTime, String time)
	{
		c = (Calendar) c.clone();
		if(time.equals(Time.HOUR.value))
		{
			c.add(Calendar.HOUR_OF_DAY, previousTime);

		}else if(time.equals(Time.DAY.value))
		{
			c.add(Calendar.DAY_OF_MONTH, previousTime );
			c.set(Calendar.HOUR_OF_DAY, 0);
		}else if(time.equals(Time.WEEK.value))
		{
			c.add(Calendar.WEEK_OF_YEAR, previousTime);
			c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
			c.set(Calendar.HOUR_OF_DAY, 0);
		}else if(time.equals(Time.MONTH.value))
		{
			c.add(Calendar.MONTH, previousTime);
			c.set(Calendar.DAY_OF_MONTH, 1);
			c.set(Calendar.HOUR_OF_DAY, 0);
		}else if(time.equals(Time.YEAR.value))
		{
			c.add(Calendar.YEAR, previousTime);
			c.set(Calendar.DAY_OF_YEAR, 1);
			c.set(Calendar.HOUR_OF_DAY, 0);
		}
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		return c;

	}


	/******************************
	 * @method  endTimeOfLastTime
	 * @description This method returns the end time of the last time
	 *****************************/
	public static Calendar endTimeOfLastTime(Calendar c, int previousTime, String time)
	{
		c = (Calendar) c.clone();
		if(time.equals(Time.HOUR.value))
		{
			c.add(Calendar.HOUR_OF_DAY, previousTime);
		}else if(time.equals(Time.DAY.value))
		{
			c.add(Calendar.DAY_OF_MONTH, previousTime);
			c.set(Calendar.HOUR_OF_DAY, 23);
		}else if(time.equals(Time.WEEK.value))
		{
			c.set(Calendar.DAY_OF_WEEK, c.getFirstDayOfWeek());
			c.add(Calendar.DAY_OF_MONTH, -1);
			c.set(Calendar.HOUR_OF_DAY, 23);
		}else if(time.equals(Time.MONTH.value))
		{
			c.add(Calendar.MONTH, previousTime);
			c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
			c.set(Calendar.HOUR_OF_DAY, 23);
		}else if(time.equals(Time.YEAR.value))
		{
			c.add(Calendar.YEAR, previousTime);
			c.set(Calendar.DAY_OF_YEAR, c.getActualMaximum(Calendar.DAY_OF_YEAR));
			c.set(Calendar.HOUR_OF_DAY, 23);
		}
		c.set(Calendar.MINUTE, 59);
		c.set(Calendar.SECOND, 59);
		return c;
	}

	/******************************
	 * @method  firstTimeOfMinutesAgo
	 * @description This method returns the first time of the time ago
	 *****************************/
	public static Calendar firstTimeOfTimeAgo(Calendar c, int previousTime, String typeTime)
	{
		c = (Calendar) c.clone();
		if(typeTime.equals(Time.MINUTE.value))
		{
			c.set(Calendar.MINUTE, previousTime);
		}else if(typeTime.equals(Time.HOUR.value))
		{
			c.add(Calendar.HOUR_OF_DAY, previousTime);
		}else if(typeTime.equals(Time.DAY.value))
		{
			c.add(Calendar.DAY_OF_MONTH, previousTime);
		}else if(typeTime.equals(Time.WEEK.value))
		{
			c.add(Calendar.WEEK_OF_YEAR, previousTime);
		}else if(typeTime.equals(Time.MONTH.value))
		{
			c.add(Calendar.MONTH, previousTime);
		} else if(typeTime.equals(Time.YEAR.value))
		{
			c.add(Calendar.YEAR, previousTime);
		}
		return c;
	}

	/************************
	 * Method to check if a code is a valid relative date code
	 ************************/
	public static boolean isValidRelativeDateCode(String relativeCode)
	{
		boolean response = false;
		if(relativeCode.equals(RelativeDate.THIS_HOUR.value ) ||
				relativeCode.equals( RelativeDate.TODAY.value ) ||
				relativeCode.equals( RelativeDate.THIS_WEEK.value ) ||
				relativeCode.equals( RelativeDate.THIS_MONTH.value ) ||
				relativeCode.equals( RelativeDate.THIS_YEAR.value ) ||
				relativeCode.equals( RelativeDate.LAST_HOUR_1.value ) ||
				relativeCode.equals( RelativeDate.LAST_DAY_1.value ) ||
				relativeCode.equals( RelativeDate.LAST_WEEK_1.value ) ||
				relativeCode.equals( RelativeDate.LAST_MONTH_1.value ) ||
				relativeCode.equals( RelativeDate.LAST_YEAR_1.value ) ||
				relativeCode.equals( RelativeDate.AGO_HOUR_1.value ) ||
				relativeCode.equals( RelativeDate.AGO_DAY_1.value ) ||
				relativeCode.equals( RelativeDate.ALL_DAY.value ) ||
				relativeCode.equals( RelativeDate.AGO_WEEK_1.value ) ||
				relativeCode.equals( RelativeDate.AGO_MONTH_1.value ) ||
				relativeCode.equals( RelativeDate.AGO_YEAR_1.value ) ||
				relativeCode.equals( RelativeDate.AS_OF_HOUR_1.value ) ||
				relativeCode.equals( RelativeDate.AS_OF_DAY_1.value ) ||
				relativeCode.equals( RelativeDate.AS_OF_WEEK_1.value ) ||
				relativeCode.equals( RelativeDate.AS_OF_MONTH_1.value ) ||
				relativeCode.equals( RelativeDate.AS_OF_YEAR_1.value ) )
		{
			response = true;
		}
		return response ;
	}

	public Date getEndDate()
	{
		return endDate;
	}

	public void setEndDate( Date endDate )
	{
		this.endDate = endDate;
	}

	public Date getStartDate()
	{
		return startDate;
	}

	public void setStartDate( Date startDate )
	{
		this.startDate = startDate;
	}
}
