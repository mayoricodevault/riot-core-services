package com.tierconnect.riot.appgen.service;

import java.io.File;
import java.io.IOException;

import com.tierconnect.riot.appgen.model.Clazz;

public class GenController extends GenBase
{
	static void generate( Clazz clazz, File indir, File outdir ) throws IOException
	{
		String subpackage = "controllers";
		
		if( ! init( clazz, indir, outdir, subpackage, clazz.getControllerName() + ".java" ) )
			return;
		
		ps.println( "package " + clazz.getPackageName() + "." + subpackage + ";" );
		ps.println();
		ps.println( "import javax.annotation.Generated;" );
        ps.println( "import javax.ws.rs.Path;" );
        ps.println( "import com.wordnik.swagger.annotations.Api;" );
        ps.println( "import com.wordnik.swagger.annotations.ApiOperation;" );
		ps.println();
        ps.println( String.format( "@Path(\"/%s\")", clazz.getModelNameLowerCase() ) );
        ps.println( String.format( "@Api(\"/%s\")", clazz.getModelNameLowerCase() ) );
        printAutoGenComment( GenController.class );
		ps.println( String.format( "public class %s extends %s ", clazz.getControllerName(), clazz.getControllerBaseName() ) );
		ps.println( "{" );
		ps.println();
		ps.println( "}" );
		ps.println();	
		
		ps.close();
	}
}
