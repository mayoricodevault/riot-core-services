package com.tierconnect.riot.appgen.service;

import java.io.*;

import com.tierconnect.riot.appgen.model.Clazz;
import org.apache.commons.io.Charsets;

/**
 * Base class for all java generator classes
 * 
 * @author tcrown
 *
 */
public class GenBase 
{
	static Clazz clazz;
	static File indir;
	static File outdir;
	
	static PrintWriter ps;
	
	static public boolean init( Clazz clazz, File indir, File outdir, String subpackage, String fname ) throws IOException
	{
		GenBase.clazz = clazz;
		GenBase.indir = indir;
		GenBase.outdir = outdir;
		
		if( indir != null )
			System.out.println( "INDIR=" + indir.getCanonicalPath() );
		else
			System.out.println( "INDIR IS NULL" );
		System.out.println( "OUTDIR=" + outdir.getCanonicalPath() );
		
		File in0 = new File( indir, clazz.getPackagePath() + File.separator + subpackage );
		File in1 = new File( in0, fname );
		
		if( indir != null )
		{
			System.out.println( "Checking " + in1.getCanonicalPath() );
			if( in1.exists() )
			{
				System.out.println( "Skipping, file exists: " + in1.getCanonicalPath() );
				return false;
			}
		}
		
		System.out.println( "\nFile does not exists, generating " + clazz.getModelName() + " " + clazz.getClassName() + ":" );
		
		File out1 = new File( outdir, clazz.getPackagePath() + File.separator + subpackage );
		out1.mkdirs();
		File out2 = new File( out1, fname );
		
		System.out.println( "FILENAME=" + out2.getCanonicalPath() );
		FileOutputStream fw = new FileOutputStream( out2 );
		ps = new PrintWriter( new OutputStreamWriter(fw, Charsets.UTF_8) );
		
		return true;
	}
	
	static public void printAutoGenComment( Class clazz )
	{
		ps.println( String.format( "@Generated(\"%s\")", clazz.getName() ) );
	}
}
