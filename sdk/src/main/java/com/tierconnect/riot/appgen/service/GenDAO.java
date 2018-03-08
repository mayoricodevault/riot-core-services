package com.tierconnect.riot.appgen.service;

import java.io.File;
import java.io.IOException;

import com.tierconnect.riot.appgen.model.Clazz;

public class GenDAO extends GenBase
{
	static void generate( Clazz clazz, File indir, File outdir ) throws IOException
	{
		String subpackage = "dao";
		
		if( ! init( clazz, indir, outdir, subpackage, clazz.getDAOName() + ".java" ) )
			return;
		
		ps.println( "package " + clazz.getPackageName() + "." + subpackage + ";" );
		ps.println();
		ps.println( "import javax.annotation.Generated;" );
		ps.println();
		printAutoGenComment( GenDAO.class );
		ps.println( String.format( "public class %s extends %s ", clazz.getDAOName(), clazz.getDAOBaseName() ) );
		ps.println( "{" );
		ps.println();
		ps.println( "}" );
		ps.println();	
		
		ps.close();
	}
}
