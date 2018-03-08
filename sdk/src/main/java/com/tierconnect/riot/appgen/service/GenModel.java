package com.tierconnect.riot.appgen.service;

import java.io.File;
import java.io.IOException;

import com.tierconnect.riot.appgen.model.Clazz;

public class GenModel extends GenBase
{
	static void generate( Clazz clazz, File indir, File outdir ) throws IOException
	{
		String subpackage = "entities";
		
		if( ! init( clazz, indir, outdir, subpackage, clazz.getModelName() + ".java" ) )
			return;
		
		
		ps.println( "package " + clazz.getPackageName() + "." + subpackage + ";" );
		ps.println();
		
		ps.println( "import javax.persistence.Entity;" );
		ps.println( "import javax.persistence.Table;" );
		ps.println( "import javax.persistence.UniqueConstraint;" );
		ps.println( "import javax.annotation.Generated;" );
		ps.println();
		
		ps.println( "@Entity" );
		if( clazz.getTableName() != null )
		{
			ps.println();
			ps.print("@Table(name=\"" + clazz.getTableName() + "\"");
			if (null != clazz.getTableAnnotations() && clazz.getTableAnnotations().length > 0)
				for(String tableAnnotation : clazz.getTableAnnotations())
					ps.print(", " + tableAnnotation);
			ps.print( ")" );
		}
		
		printAutoGenComment(GenModel.class );
		ps.println( String.format( "public class %s extends %s ", clazz.getModelName(), clazz.getModelBaseName() ) );
		ps.println( "{" );
		ps.println();
		ps.println( "}" );
		ps.println();	
		
		ps.close();
	}
}
