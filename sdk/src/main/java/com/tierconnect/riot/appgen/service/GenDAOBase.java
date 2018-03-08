package com.tierconnect.riot.appgen.service;

import java.io.File;
import java.io.IOException;

import com.tierconnect.riot.appgen.model.Clazz;

public class GenDAOBase extends GenBase
{
	static void generate( Clazz clazz, File outdir ) throws IOException
	{
		String subpackage = "dao";
		
		init( clazz, null, outdir, subpackage, clazz.getDAOBaseName() + ".java"  );
		
		ps.println( "package " + clazz.getPackageName() + "." + subpackage + ";" );
		ps.println();
		
		ps.println( String.format( "import %s.entities.%s;", clazz.getPackageName(), clazz.getModelName() ) );
		ps.println( String.format( "import %s.entities.Q%s;", clazz.getPackageName(), clazz.getModelName() ) );
		ps.println( "import com.tierconnect.riot.sdk.dao.DAOHibernateImp;" );
		ps.println( "import com.mysema.query.types.path.EntityPathBase;" );
		ps.println( "import javax.annotation.Generated;" );
		
		ps.println();
		
		printAutoGenComment( GenDAOBase.class );
		ps.println( String.format( "public class %s extends DAOHibernateImp<%s, Long>", clazz.getDAOBaseName(), clazz.getModelName() ) );
		ps.println( "{" );
		ps.println( "\t@Override" );
		ps.println( String.format( "\tpublic EntityPathBase<%s> getEntityPathBase()", clazz.getModelName() ) );
		ps.println( "\t{" );
		ps.println( String.format( "\t\treturn Q%s.%s;", clazz.getModelName(), clazz.getModelNameLowerCase() ) );
		ps.println( "\t}" );
		ps.println( "}" );
		
		ps.println();	
		
		ps.close();
	}
}
