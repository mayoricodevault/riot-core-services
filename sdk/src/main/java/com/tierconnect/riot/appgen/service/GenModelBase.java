package com.tierconnect.riot.appgen.service;

import com.tierconnect.riot.appgen.model.Clazz;
import com.tierconnect.riot.appgen.model.Property;

import java.io.File;
import java.io.IOException;

public class GenModelBase extends GenBase
{
	static void generate( Clazz clazz, File outdir ) throws IOException
	{
		String subpackage = "entities";
		
		init( clazz, null, outdir, subpackage, clazz.getModelBaseName() + ".java"  );
		
		ps.println( "package " + clazz.getPackageName() + "." + subpackage + ";" );
		ps.println();
		
		if( clazz.getImports() != null )
		for( String imports : clazz.getImports() )
		{
			ps.println( String.format( "import %s;", imports ) );
		}
		
		ps.println( "import java.util.HashMap;" );
		ps.println( "import java.util.Map;" );
		ps.println( "import java.util.Date;" );
        ps.println( "import java.util.concurrent.ConcurrentHashMap;");
        ps.println( "import javax.persistence.Table;" );
		ps.println( "import javax.persistence.MappedSuperclass;" );
		ps.println( "import javax.annotation.*;" );
		ps.println( "import javax.persistence.Id;" );
		ps.println( "import javax.persistence.GeneratedValue;" );
		ps.println( "import javax.persistence.OneToMany;" );
		ps.println( "import javax.persistence.ManyToOne;" );
		ps.println( "import javax.persistence.FetchType;" );
		ps.println( "import javax.persistence.Temporal;" );
		ps.println( "import javax.persistence.TemporalType;" );
		ps.println( "import javax.persistence.Column;" );
		ps.println( "import javax.persistence.CascadeType;" );
		ps.println( "import javax.validation.constraints.*;" );
		ps.println( "import com.tierconnect.riot.appgen.service.Getter;" );
		ps.println( "import java.lang.reflect.InvocationTargetException;" );
		ps.println( "import javax.persistence.Transient;" );
        ps.println( "" );
		ps.println( "" );
		ps.println();
		
		ps.println();
		
		ps.println( "@MappedSuperclass" );
		if( clazz.getTableName() != null )
		{
			ps.println( "@Table(name=\"" + clazz.getTableName() + "\")" );
		}
		
		printAutoGenComment( GenModelBase.class );
		ps.println( "public abstract class " + clazz.getModelBaseName() );
		if (clazz.getImplement() != null){
			ps.println( " implements " + clazz.getImplement());
		}
		ps.println( "{" );
		
		ps.println( "\tpublic static final Map<String, Class<?>> classOfProperty = new ConcurrentHashMap<String, Class<?>>();" );
		ps.println( "\tstatic" );
		ps.println( "\t{" );
		for( Property p : clazz.getProperties() )
		{
			if( p.isPrimitive() || p.isEntity() )
			{
				ps.println( String.format( "\t\tclassOfProperty.put( \"%s\", %s.class );", p.getName(), p.getShortType() ) );
			}
		}
		ps.println( "\t}" );
		ps.println();
		
		for( Property p : clazz.getProperties() )
		{
            if(!p.isDerived())
            {
                if (p.getAnnotations() != null)
                {
                    for (String str : p.getAnnotations())
                    {
                        ps.println("\t" + str);
                    }
                }
                ps.println("\tprotected " + p.getShortType() + " " + p.getName() + ";\n");
            }
		}

		ps.println();
        ps.println( String.format( "\t@Transient public Getter getter = null;"));
        ps.println();


		for( Property p : clazz.getProperties() )
		{
            if(!p.isDerived()) {
                ps.println(GenUtils.genGetter(p));
                ps.println();
                ps.println(GenUtils.genSetter(p));
            } else if (p.isDerived()) {
                ps.println(GenUtils.genAbstractGetter(p));
            }
		}
		
		// temporay until these can be refactored away
		String isFinal = "final";
		String n = clazz.getModelName();
		if( "User".equals( n ) || "Role".equals( n ) || "Resource".equals( n ) || "ReportDefinition".equals( n ) || "ThingField".equals( n ) 
				|| "ThingType".equals( n ) || "ThingTypeField".equals( n ) || "Zone".equals( n ) || "LocalMap".equals(n)
				|| "ReportActions".equals(n) || "ActionConfiguration".equals(n) || "LogExecutionAction".equals(n) )
			isFinal = "";
		
		ps.println( String.format( "\t//NOTE: this final to prevent people from adding to much to publicMap, it is a bad idea" ) );
		ps.println( String.format( "\t" + isFinal + " public Map<String,Object> publicMap()" ) );
		ps.println( "\t{" );
		ps.println( "\t\tMap<String, Object> map = new HashMap<String, Object>();" );
		for( Property p : clazz.getProperties() )
		{
			if( p.isPrimitive() )
			{
				ps.println( String.format( "\t\tmap.put( \"%s\", %s );", p.getName(), p.getGetterName( )+"()" ));
			}
			// hmmm, do we want this in the default ? Probably not ....
			//else if( p.isEntity() )
			//{
			//	ps.println( String.format( "\t\tif( %s != null )", p.getName() ) );
			//	ps.println( String.format( "\t\t\tmap.put( \"%sId\", %s.getId() );", p.getName(), p.getName() ) );
			//}
		}
		ps.println( "\t\treturn map;" );
		ps.println( "\t}" );


		ps.println( String.format( "\t//NOTE: new publicMap to prevent infinite loop" ) );
		ps.println( String.format( "\t" + isFinal + " public Map<String,Object> referencedPublicMap(int level)" ) );
		ps.println( "\t{" );
		ps.println( "\t\tMap<String, Object> map = new HashMap<String, Object>();" );
		StringBuilder subPs = new StringBuilder("");
		for( Property p : clazz.getProperties() )
		{
			if( p.isPrimitive() )
			{
				if (p.getType().equals("Date")) {
					ps.println(String.format("\t\tmap.put( \"%s\", %s );", p.getName(), p.getGetterName() + "().getTime()"));
				} else {
					ps.println(String.format("\t\tmap.put( \"%s\", %s );", p.getName(), p.getGetterName() + "()"));
				}
			} else if (!p.isArray() && !p.isLob()) {
				String newLine = "";
				if (!subPs.toString().equals("")) {
					newLine = System.getProperty("line.separator");
				}
				subPs.append( String.format( newLine + "\t\t\tmap.put( \"%s\", %s!=null?%s:null );", p.getName(),
						p.getGetterName( )+"()", p.getGetterName( )+"().referencedPublicMap(level - 1)" ));
			}
		}
		if (!subPs.toString().equals("")) {
			ps.println("\t\tif (level > 0){");
			ps.println(subPs);
			ps.println("\t\t}");
		}
		ps.println( "\t\treturn map;" );
		ps.println( "\t}" );


		ps.println();
        ps.println( String.format( "\tpublic Object getProperty(String name) " +
                "throws IllegalAccessException, InvocationTargetException, NoSuchMethodException" ) );
        ps.println( "\t{" );
        ps.println( "\t\treturn getter.getProperty( name );" );
        ps.println( "\t}" );
        ps.println();


		ps.println();
        ps.println( String.format( "\tpublic String toString()" ) );
        ps.println( "\t{" );
        ps.println( "\t\treturn super.toString()" + " + \"=\" + " + "publicMap().toString();" );
        ps.println( "\t}" );
        ps.println();

		ps.println( "}" );
		
		ps.println();


		ps.close();
	}
}
