package com.tierconnect.riot.appgen.service;

import java.io.File;
import java.io.IOException;

import com.tierconnect.riot.appgen.model.Clazz;

public class GenService extends GenBase
{
	static void generate( Clazz clazz, File indir, File outdir ) throws IOException
	{
		String subpackage = "services";
		
		if( ! init( clazz, indir, outdir, subpackage, clazz.getServiceName() + ".java" ) )
			return;
		
		ps.println( "package " + clazz.getPackageName() + "." + subpackage + ";" );
		ps.println();
		ps.println( "import javax.annotation.Generated;" );
		ps.println();
		printAutoGenComment( GenService.class );
		ps.println( String.format( "public class %s extends %s ", clazz.getServiceName(), clazz.getServiceBaseName() ) );
		ps.println( "{" );
		ps.println();
		ps.println( "}" );
		ps.println();	
		
		ps.close();
	}
}
