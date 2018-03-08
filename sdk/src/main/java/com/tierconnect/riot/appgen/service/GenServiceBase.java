package com.tierconnect.riot.appgen.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appgen.model.Clazz;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

public class GenServiceBase extends GenBase
{
	static void generate( Clazz clazz, File outdir ) throws IOException
	{
		String subpackage = "services";
		
		init( clazz, null, outdir, subpackage, clazz.getServiceBaseName() + ".java"  );
		
		ps.println( "package " + clazz.getPackageName() + "." + subpackage + ";" );
		ps.println();
		
		ps.println( String.format( "import %s.entities.%s;", clazz.getPackageName(), clazz.getModelName() ) );
		ps.println( String.format( "import %s.entities.Q%s;", clazz.getPackageName(), clazz.getModelName() ) );
		ps.println( String.format( "import %s.dao.%s;", clazz.getPackageName(), clazz.getDAOName() ) );
		ps.println( "import com.tierconnect.riot.appcore.services.FavoriteService;");
		ps.println( "import com.mysema.query.BooleanBuilder;");
		ps.println( "import com.tierconnect.riot.appcore.entities.Favorite;");
		ps.println( "import com.tierconnect.riot.appcore.entities.QFavorite;");
		ps.println( "import com.tierconnect.riot.appcore.dao.FavoriteDAO;");
		ps.println( "import org.apache.shiro.subject.Subject;");
		ps.println( "import com.tierconnect.riot.appcore.entities.User;");
		ps.println( "import org.apache.shiro.SecurityUtils;");

		ps.println( "import java.util.List;" );
		ps.println( "import com.tierconnect.riot.sdk.dao.Pagination;" );
		ps.println( "import com.mysema.query.types.OrderSpecifier;" );
		ps.println( "import com.mysema.query.types.Predicate;" );
		ps.println( "import javax.annotation.Generated;" );
		ps.println( "import com.tierconnect.riot.appcore.utils.QueryUtils;" );

		
		ps.println();
		
		printAutoGenComment( GenServiceBase.class );
		ps.println( String.format( "public class %s", clazz.getServiceBaseName() ) );
		ps.println( "{" );
		
		ps.println( String.format( "\tstatic private %s _%s;", clazz.getDAOName(), clazz.getDAONameLowerCase() ) );
		ps.println();
		ps.println( String.format( "\tpublic static %s get%s()", clazz.getDAOName(), clazz.getDAOName() ) );
		ps.println( "\t{" );
		ps.println( String.format( "\t\tif( _%s == null )", clazz.getDAONameLowerCase() ) );
		ps.println( "\t\t{" );
		ps.println( String.format( "\t\t\t_%s = new %s();", clazz.getDAONameLowerCase(), clazz.getDAOName() ) );
		ps.println( "\t\t}" );
		ps.println( String.format( "\t\treturn _%s;", clazz.getDAONameLowerCase() ) );
		ps.println( "\t}" );

        ps.println( String.format( "\tstatic private %s INSTANCE = new %s();", clazz.getServiceName(), clazz.getServiceName()) );
        ps.println();
        ps.println( String.format( "\tpublic static %s getInstance()", clazz.getServiceName()) );
        ps.println( "\t{" );
        ps.println( String.format( "\t\treturn %s;", "INSTANCE") );
        ps.println( "\t}" );

		ps.println();
		ps.println( String.format( "\tpublic %s get( Long id )", clazz.getModelName() ) );
		ps.println( "\t{" );
		ps.println( String.format( "\t\t%s %s = get%s().selectById( id );", clazz.getModelName(), clazz.getModelNameLowerCase(), clazz.getDAOName() ) );
		ps.println( String.format( "\t\treturn %s;", clazz.getModelNameLowerCase() ) );
		ps.println( "\t}" );
		
		ps.println();
		ps.println( ! clazz.isGenerateServiceInsert() ? "\t/*" : "" );
		ps.println( String.format( "\tpublic %s insert( %s %s )", clazz.getModelName(), clazz.getModelName(), clazz.getModelNameLowerCase() ) );
		ps.println( "\t{" );
		ps.println( String.format( "\t\tvalidateInsert( %s );", clazz.getModelNameLowerCase() ) );
		ps.println( String.format( "\t\tLong id = get%s().insert( %s );", clazz.getDAOName(), clazz.getModelNameLowerCase() ) );
		ps.println( String.format( "\t\t%s.setId( id );", clazz.getModelNameLowerCase() ) );
		ps.println( String.format( "\t\treturn %s;", clazz.getModelNameLowerCase() ) );
		ps.println( "\t}" );
		ps.println();
		ps.println( String.format( "\tpublic void validateInsert( %s %s )", clazz.getModelName(), clazz.getModelNameLowerCase() ) );
		ps.println( "\t{" );
		ps.println( "" );
		ps.println( "\t}" );
		ps.println( ! clazz.isGenerateServiceInsert() ? "\t*/" : "" );
	
		ps.println();
		ps.println( ! clazz.isGenerateServiceUpdate() ? "\t/*" : "" );
		ps.println( String.format( "\tpublic %s update( %s %s )", clazz.getModelName(), clazz.getModelName(), clazz.getModelNameLowerCase() ) );
		ps.println( "\t{" );
		ps.println( String.format( "\t\tvalidateUpdate( %s );", clazz.getModelNameLowerCase() ) );
		ps.println( String.format( "\t\tget%s().update( %s );", clazz.getDAOName(), clazz.getModelNameLowerCase() ) );
		if (clazz.getClassName().equals("Thing") || clazz.getClassName().equals("ThingType")|| clazz.getClassName().equals("Zone")|| clazz.getClassName().equals("Connection")
				|| clazz.getClassName().equals("Shift") || clazz.getClassName().equals("Group")|| clazz.getClassName().equals("GroupType")|| clazz.getClassName().equals("Role")
				|| clazz.getClassName().equals("Edgebox") || clazz.getClassName().equals("ZoneType")|| clazz.getClassName().equals("ZoneGroup")|| clazz.getClassName().equals("LocalMap")
				|| clazz.getClassName().equals("LogicalReader") || clazz.getClassName().equals("ReportDefinition") || clazz.getClassName().equals("User"))  {
			ps.println(String.format("\t\tupdateFavorite( %s );",clazz.getModelNameLowerCase()));
		}
		ps.println( String.format( "\t\treturn %s;", clazz.getModelNameLowerCase() ) );
		ps.println( "\t}" );
		ps.println();
		if (clazz.getClassName().equals("Thing") || clazz.getClassName().equals("ThingType")|| clazz.getClassName().equals("Zone")|| clazz.getClassName().equals("Connection")
				|| clazz.getClassName().equals("Shift") || clazz.getClassName().equals("Group")|| clazz.getClassName().equals("GroupType")|| clazz.getClassName().equals("Role")
				|| clazz.getClassName().equals("Edgebox") || clazz.getClassName().equals("ZoneType")|| clazz.getClassName().equals("ZoneGroup")|| clazz.getClassName().equals("LocalMap")
				|| clazz.getClassName().equals("LogicalReader") || clazz.getClassName().equals("ReportDefinition") || clazz.getClassName().equals("User")) {
			ps.println(String.format("\tpublic void updateFavorite( %s %s )", clazz.getModelName(), clazz.getModelNameLowerCase()));
			ps.println("\t{");
			if (clazz.getModelNameLowerCase().equals("reportDefinition")) {
				ps.println("\t\tString typeElement = \"report\";");
			}else{
				ps.println(String.format("\t\tString typeElement = \"%s\";", clazz.getModelNameLowerCase().toLowerCase()));
			}
			ps.println(String.format("\t\tLong elementId = %s.getId();", clazz.getModelNameLowerCase()));
			ps.println();
			ps.println("\t\t\tBooleanBuilder be = new BooleanBuilder();");
			ps.println("\t\t\tbe = be.and(QFavorite.favorite.typeElement.eq(typeElement));");
			ps.println("\t\t\tbe = be.and(QFavorite.favorite.elementId.eq(elementId));");
			ps.println("\t\t\tList <Favorite> listFavorite = FavoriteService.getInstance().listPaginated(be,null, null);");
			ps.println("\t\t\tfor (Favorite favorite: listFavorite){");
			if (clazz.getModelNameLowerCase().equals("user")) {
				ps.println(String.format("\t\t\t\tfavorite.setElementName(%s.getUsername());", clazz.getModelNameLowerCase()));
			}else{
				ps.println(String.format("\t\t\t\tfavorite.setElementName(%s.getName());", clazz.getModelNameLowerCase()));
			}
			ps.println("\t\t\t\tFavoriteService.getInstance().update(favorite);");
			ps.println("\t\t\t}");
			ps.println("");
			ps.println("\t}");
		}
		ps.println( String.format( "\tpublic void validateUpdate( %s %s )", clazz.getModelName(), clazz.getModelNameLowerCase() ) );
		ps.println( "\t{" );
		ps.println( "" );
		ps.println( "\t}" );
		ps.println( ! clazz.isGenerateServiceUpdate() ? "\t*/" : "" );
		
		ps.println();
		ps.println( ! clazz.isGenerateServiceDelete() ? "\t/*" : "" );
		ps.println( String.format( "\tpublic void delete( %s %s )", clazz.getModelName(), clazz.getModelNameLowerCase() ) );
		ps.println( "\t{" );
		ps.println( String.format( "\t\tvalidateDelete( %s );", clazz.getModelNameLowerCase() ) );
		ps.println( String.format( "\t\tget%s().delete( %s );", clazz.getDAOName(), clazz.getModelNameLowerCase() ) );
		if (!clazz.getClassName().startsWith("Favorite") && !clazz.getClassName().startsWith("Migration")) {
			ps.println(String.format("\t\tdeleteFavorite( %s );",clazz.getModelNameLowerCase()));
		}
		ps.println( "\t}" );
		ps.println();
		if (!clazz.getClassName().startsWith("Favorite") && !clazz.getClassName().startsWith("Migration")) {
			ps.println(String.format("\tpublic void deleteFavorite( %s %s )", clazz.getModelName(), clazz.getModelNameLowerCase()));
			ps.println("\t{");
			ps.println("\t\tSubject subject = SecurityUtils.getSubject();");
			ps.println("\t\tUser currentUser = (User) subject.getPrincipal();");
			if (clazz.getModelNameLowerCase().equals("reportDefinition")) {
				ps.println("\t\tString typeElement = \"report\";");
			}else{
				ps.println(String.format("\t\tString typeElement = \"%s\";", clazz.getModelNameLowerCase().toLowerCase()));
			}
			ps.println(String.format("\t\tLong elementId = %s.getId();", clazz.getModelNameLowerCase()));
			ps.println();
			ps.println("\t\tBooleanBuilder beAnd = new BooleanBuilder();");
			ps.println("\t\tbeAnd = beAnd.and(QFavorite.favorite.typeElement.eq(typeElement));");
			ps.println("\t\tbeAnd = beAnd.and(QFavorite.favorite.elementId.eq(elementId));");
			ps.println("\t\tbeAnd = beAnd.and(QFavorite.favorite.user.eq(currentUser));");
			ps.println();
			ps.println("\t\tList<Favorite> favorites = FavoriteService.getInstance().listPaginated(beAnd, null, null);");
			ps.println("\t\tif (!favorites.isEmpty()){");
			ps.println("\t\t\tFavoriteService.getInstance().delete(favorites.get(0));");
			ps.println("\t\t}");
			ps.println("\t\t\tBooleanBuilder be = new BooleanBuilder();");
			ps.println("\t\t\tbe = be.and(QFavorite.favorite.typeElement.eq(typeElement));");
			ps.println("\t\t\tbe = be.and(QFavorite.favorite.elementId.eq(elementId));");
			ps.println("\t\t\tList <Favorite> listFavorite = FavoriteService.getInstance().listPaginated(be,null, null);");
			ps.println("\t\t\tfor (Favorite favorite1: listFavorite){");
			ps.println("\t\t\t\tfavorite1.setStatus(\"DELETED\");");
			ps.println("\t\t\t\tFavoriteService.getInstance().update(favorite1);");
			ps.println("\t\t\t}");
			ps.println("");
			ps.println("\t}");
		}
		ps.println( String.format( "\tpublic void validateDelete( %s %s )", clazz.getModelName(), clazz.getModelNameLowerCase() ) );
		ps.println( "\t{" );
		ps.println( "" );
		ps.println( "\t}" );
		ps.println( ! clazz.isGenerateServiceDelete() ? "\t*/" : "" );
		
		ps.println();
		ps.println( String.format( "\tpublic List<%s> listPaginated( Pagination pagination, String orderString )", clazz.getModelName() ) );
		ps.println( "\t{" );
		ps.println( String.format( "\t\tOrderSpecifier orderSpecifiers[] = QueryUtils.getOrderFields( Q%s.%s, orderString );", clazz.getModelName(), clazz.getModelNameLowerCase() ) );
		ps.println( String.format( "\t\treturn get%s().selectAll( null, pagination, orderSpecifiers );", clazz.getDAOName() ) );
		ps.println( "\t}" );
		
		ps.println();
		ps.println( String.format( "\tpublic long countList( Predicate be )" ) );
		ps.println( "\t{" );
		ps.println( String.format( "\t\treturn get%s().countAll( be );", clazz.getDAOName() ) );
		ps.println( "\t}" );
		
		ps.println();
		ps.println( String.format( "\tpublic List<%s> listPaginated( Predicate be, Pagination pagination, String orderString ) ", clazz.getModelName() ) );
		ps.println( "\t{" );
		ps.println( String.format( "\t\tOrderSpecifier orderSpecifiers[] = QueryUtils.getOrderFields( Q%s.%s, orderString );", clazz.getModelName(), clazz.getModelNameLowerCase() ) );
		ps.println( String.format( "\t\treturn get%s().selectAll( be, pagination, orderSpecifiers );", clazz.getDAOName() ) );
		ps.println( "\t}" );
		
		ps.println( "}" );
		ps.println();	
		ps.close();
	}
}
