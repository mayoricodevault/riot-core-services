package com.tierconnect.riot.datagen;

import java.math.BigInteger;
import java.security.SecureRandom;

public class UDFValueGenerator
{
	private SecureRandom random = new SecureRandom();

	private String[] udf1 = new String [] { "one", "two" };

	private String[] udf2 = new String [] { "red", "green", "blue", "black" };
	
	private String[] udf3 = new String [] { "happy", "sad", "mad", "ok", "notok", "fine", "good", "normal" };
	
	private static UDFValueGenerator INSTANCE;

	static
	{
		INSTANCE = new UDFValueGenerator();
	}

	public static UDFValueGenerator instance()
	{
		return INSTANCE;
	}

	public String nextValue( long typeId ) 
    {
    	String str;
    	switch( (int) typeId )
    	{
    	case 1:
    		str = udf1[ random.nextInt( udf1.length ) ];
    		break;
    		
    	case 2:
    		str = udf2[ random.nextInt( udf2.length ) ];
    		break;
    		
    	case 3:
    		str = udf3[ random.nextInt( udf3.length ) ];
    		break;
    		
    	default:
        	str = new BigInteger(130, random).toString(32);
    	}
    	return str;
    }
}
