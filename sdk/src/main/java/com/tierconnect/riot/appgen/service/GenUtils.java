package com.tierconnect.riot.appgen.service;

import com.tierconnect.riot.appgen.model.Property;

public class GenUtils 
{

	static String genGetter( Property p )
	{
		StringBuffer sb = new StringBuffer();
		sb.append( String.format( "\t%s %s %s()\n", p.getVisibility(), p.getShortType(), p.getGetterName() ) );
		sb.append( "\t{\n" );
		if (p.getType().equals("Date")) {
			sb.append( "\t\treturn (" + p.getVariableName() + " == null ? null : new Date(" + p.getVariableName() + ".getTime()));\n" );
		} else {
			sb.append( "\t\treturn " + p.getVariableName() + ";\n" );
		}
		sb.append( "\t}" );
		
		return sb.toString();
	}

    static String genAbstractGetter( Property p )
    {
        StringBuffer sb = new StringBuffer();
        sb.append( String.format( "\t%s %s %s();\n", p.getVisibility(), p.getShortType(), p.getGetterName() ) );
        return sb.toString();
    }

	static String genSetter( Property p )
	{
		StringBuffer sb = new StringBuffer();
		sb.append( String.format("\t%s void %s( %s %s )\n" , p.getVisibility(), p.getSetterName(), p.getShortType(), p.getVariableName() ) );
		sb.append( "\t{\n" );
		if (p.getType().equals("Date")) {
			sb.append( String.format( "\t\tthis.%s = %s == null ? null : new Date(%s.getTime());\n", p.getVariableName(), p.getVariableName(), p.getVariableName() ) );
		} else {
			sb.append( String.format( "\t\tthis.%s = %s;\n", p.getVariableName(), p.getVariableName() ) );
		}
		sb.append( "\t}\n" );
		return sb.toString();
	}

}
