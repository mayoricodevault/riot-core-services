package com.tierconnect.riot.iot.services;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.hibernate.Transaction;

import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.iot.entities.Unit;

public class UnitService extends UnitServiceBase
{
	private static final Map<String, Double> cache = new HashMap<String, Double>();

	private static ScriptEngine engine;

	private static List<Unit> units;

	static
	{
		ScriptEngineManager engineManager = new ScriptEngineManager();
		engine = engineManager.getEngineByName( "nashorn" );
	}

	public static void main( String[] args ) throws Exception
	{
		System.out.println( "hello" );

		PopDBRequired.initJDBCDrivers();
		Transaction transaction = UnitService.getUnitDAO().getSession().getTransaction();
		transaction.begin();
		units = UnitService.getUnitDAO().selectAll();
		System.out.println( "found " + units.size() + " units" );
		transaction.commit();

		System.out.println( "cp2" );

		test( 5, "ft", "m" );

		test( 9, "ft", "yd" );

		test( 36, "in", "ft" );
		test( 36, "in", "yd" );
		test( 36, "in", "m" );
		
		test( 1, "mile", "ft" );
		test( 1, "mile", "yd" );
		test( 1, "mile", "in" );
		
		test( 1, "mile", "m" );
		
		test( 1, "lbf", "N" );
		
		test( 1, "psi", "pascal" );

	}

	private static void test( double value, String from, String to ) throws Exception
	{
		System.out.println( value + " " + from + " = " + convert( value, from, to ) + " " + to );
	}

	/**
	 * This method converts a value, form unitsIn to unitsOut
	 * 
	 * @return value converted to new unit
	 * @throws Exception
	 */
	public static double convert( double value, String unitsIn, String unitsOut ) throws Exception
	{
		return value * base( unitsIn ) / base( unitsOut );
	}

	/**
	 * This method converts a value, form units to the base unit
	 * 
	 * @return value converted to base unit
	 */
	public static double toBase( double value, String units ) throws Exception
	{
		return value * base( units );
	}

	/**
	 * This method converts a value, to units from the base unit
	 * 
	 * @return value converted to units
	 */
	public static double fromBase( double value, String units ) throws Exception
	{
		return value / base( units );
	}

	/**
	 * This method returns the base unit
	 * 
	 * @return base unit
	 */
	private static double base( String units ) throws Exception
	{
		Double value = (Double) cache.get( units );
		if( value == null )
		{
			// System.out.println( " units=" + units );
			String str = subsitute( 0, units );
			Object o = engine.eval( str );
			value = Double.parseDouble( o.toString() );
			cache.put( units, value );
		}
		return value.doubleValue();
	}

	private static String subsitute( int count, String expression )
	{
		System.out.println( " in=" + expression );

		if( count > 1000 )
		{
			throw new Error( "recursion limit execeeded" );
		}

		String value = expression;
		//value = value.replaceAll( "\\^", "**" );
		// (a)^(b) => Math.pow( a, b )
		//TODO
		
		for( Iterator<Unit> i = units.iterator(); i.hasNext(); )
		{
			Unit unit = i.next();
			value = value.replaceAll( "\\b" + unit.getUnitSymbol() + "\\b", "(" + unit.getDefinition() + ")" );
		}

		System.out.println( "out=" + value );

		if( !value.equals( expression ) )
		{
			// System.out.println( " sub=" + value );
			value = subsitute( ++count, value );
		}

		return value;
	}

}
