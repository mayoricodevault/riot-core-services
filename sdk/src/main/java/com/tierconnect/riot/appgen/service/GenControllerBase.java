package com.tierconnect.riot.appgen.service;

import com.tierconnect.riot.appgen.model.Clazz;
import com.tierconnect.riot.appgen.model.Property;
import com.tierconnect.riot.sdk.utils.UpVisibility;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class GenControllerBase extends GenBase
{
	private static Logger logger = Logger.getLogger(GenControllerBase.class);
	
    static void generate( Clazz clazz, File outdir ) throws IOException
    {
        if( ! clazz.isGenerateController() )
            return;
        
        if(clazz.getAboveVisibility() == null)
        {
        	logger.error(String.format("aboveVisibility field for %s cannot be null",clazz.getControllerBaseName()));
        	throw new RuntimeException(String.format("aboveVisibility field for %s cannot be null",clazz.getControllerBaseName()));
        }
        if(clazz.isBelowVisibility() == null)
        {
        	logger.error(String.format("belowVisibility field for %s cannot be null",clazz.getControllerBaseName()));
        	throw new RuntimeException(String.format("belowVisibility field for %s cannot be null",clazz.getControllerBaseName()));
        }
        
        String subpackage = "controllers";
        
        init(clazz, null, outdir, subpackage, clazz.getControllerBaseName() + ".java");
        
        ps.println("package " + clazz.getPackageName() + "." + subpackage + ";");
        ps.println();
        ps.println( "import javax.ws.rs.DELETE;" );
        ps.println( "import javax.ws.rs.GET;" );
        ps.println( "import javax.ws.rs.PUT;" );
        ps.println( "import com.wordnik.swagger.jaxrs.PATCH;" );
        ps.println( "import javax.ws.rs.Path;" );
        ps.println( "import javax.ws.rs.DefaultValue;" );
        ps.println( "import com.wordnik.swagger.annotations.Api;" );
        ps.println( "import com.wordnik.swagger.annotations.ApiOperation;" );
        ps.println( "" );
        if (clazz.getModelNameLowerCase().equals("thing")|| clazz.getModelNameLowerCase().equals("user") || clazz.getModelNameLowerCase().equals("group") ||
                clazz.getModelNameLowerCase().equals("groupType") || clazz.getModelNameLowerCase().equals("role") || clazz.getModelNameLowerCase().equals("shift")
                || clazz.getModelNameLowerCase().equals("edgebox") || clazz.getModelNameLowerCase().equals("connection") || clazz.getModelNameLowerCase().equals("zone")
                || clazz.getModelNameLowerCase().equals("zoneType") || clazz.getModelNameLowerCase().equals("zoneGroup") || clazz.getModelNameLowerCase().equals("localMap")
                || clazz.getModelNameLowerCase().equals("logicalReader")|| clazz.getModelNameLowerCase().equals("reportDefinition")
                || clazz.getModelNameLowerCase().equals("scheduledRule")) {
            ps.println( "import com.tierconnect.riot.appcore.services.RecentService;");
        }
        ps.println( "import javax.ws.rs.Produces;" );
        ps.println( "import javax.ws.rs.Consumes;" );
        ps.println( "import javax.ws.rs.core.MediaType;" );
        ps.println( "import org.apache.shiro.SecurityUtils;" );
        ps.println( "import org.apache.shiro.authz.annotation.RequiresPermissions;" );
        ps.println( "import org.apache.shiro.authz.annotation.RequiresAuthentication;" );
        ps.println( "import com.tierconnect.riot.sdk.utils.PermissionsUtils;" );
        ps.println( "import javax.ws.rs.QueryParam;" );
        ps.println( "import com.wordnik.swagger.annotations.ApiParam;" );
        ps.println( "import javax.ws.rs.PathParam;" );
        ps.println( "import javax.ws.rs.core.Response;" );
        ps.println( "import org.hibernate.Session;" );
        ps.println( "" );
        ps.println( "import java.util.*;" );
        ps.println( "import java.util.Map.Entry;" );
        ps.println( "import org.apache.commons.lang.StringUtils;" );
        ps.println( "" );
        ps.println( "import com.tierconnect.riot.appcore.entities.*;" );
        ps.println( "import com.tierconnect.riot.appcore.controllers.*;" );
        ps.println( "import com.tierconnect.riot.sdk.utils.BeanUtils;" );
        ps.println( "import com.tierconnect.riot.appcore.utils.QueryUtils;" );
        ps.println( "import com.tierconnect.riot.sdk.utils.RestUtils;" );
        ps.println( "import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;" );
        ps.println( "import com.tierconnect.riot.appcore.entities.User;" );
        ps.println( "import com.tierconnect.riot.appcore.entities.Group;" );
        ps.println( "import com.tierconnect.riot.appcore.services.FavoriteService;");
        ps.println( "import com.tierconnect.riot.appcore.services.GroupService;" );
        ps.println( "import com.tierconnect.riot.appcore.utils.QueryUtils;" );
        ps.println( "import com.tierconnect.riot.appcore.utils.VisibilityUtils;" );
        ps.println( "import com.tierconnect.riot.appcore.utils.GeneralVisibilityUtils;" );
        ps.println( "import com.tierconnect.riot.appcore.utils.EntityVisibility;" );
        ps.println( "import com.tierconnect.riot.sdk.utils.UpVisibility;" );
        ps.println( "import com.mysema.query.types.path.EntityPathBase;" );
        if (clazz.getModelName().equals("Zone")) {
            ps.println("import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;");
        }

        ps.println( "" );
        ps.println( String.format( "import %s.entities.%s;", clazz.getPackageName(), clazz.getModelName() ) );
        ps.println( String.format( "import %s.entities.Q%s;", clazz.getPackageName(), clazz.getModelName() ) );
        ps.println( String.format( "import %s.services.%s;", clazz.getPackageName(), clazz.getServiceName() ) );

        ps.println( "import com.tierconnect.riot.appcore.entities.QGroup;" );

        ps.println( "import com.tierconnect.riot.sdk.dao.Pagination;" );
        ps.println( "import com.mysema.query.types.OrderSpecifier;" );
        ps.println( "import com.mysema.query.BooleanBuilder;" );
        ps.println( "import javax.annotation.Generated;" );
        ps.println();
        
        ps.println( String.format( "@Path(\"/%s\")", clazz.getModelNameLowerCase() ) );
        ps.println( String.format( "@Api(\"/%s\")", clazz.getModelNameLowerCase() ) );
        printAutoGenComment( GenControllerBase.class );
        ps.println( String.format( "public class %s", clazz.getControllerBaseName() ) );
        ps.println( "{" );
        
        String ag = " (AUTO)";

        ps.println();
        ps.println( String.format( "\tprotected static final EntityVisibility<%s> entityVisibility;", clazz.getClassName()) );
        ps.println();
        ps.println( String.format( "\tstatic {") );
        ps.println( String.format( "\t\tentityVisibility = new EntityVisibility<%s>() {", clazz.getClassName()) );
        ps.println( String.format( "\t\t\t@Override") );
        ps.println( String.format( "\t\t\tpublic QGroup getQGroup(EntityPathBase<%s> base) {", clazz.getClassName()) );
        if( clazz.hasGroup() ) {
            ps.println(String.format("\t\t\t\treturn ((Q%s) base).group;", clazz.getClassName()));
        } else {
            ps.println(String.format("\t\t\t\treturn null;"));
        }
        ps.println( String.format( "\t\t\t}") );
        ps.println();
        ps.println( String.format( "\t\t\t@Override") );
        ps.println( String.format( "\t\t\tpublic Group getGroup(%s object) {", clazz.getClassName()) );
        if ("Group".equals(clazz.getClassName())) {
            ps.println(String.format("\t\t\t\treturn object;"));
        } else {
            if( clazz.hasGroup() ) {
                ps.println(String.format("\t\t\t\treturn object.getGroup();"));
            } else {
                ps.println(String.format("\t\t\t\treturn null;"));
            }
        }
        ps.println( String.format( "\t\t\t}") );
        ps.println( String.format( "\t\t};") );
        ps.println( String.format( "\t\tentityVisibility.setUpVisibility(UpVisibility.%s);", clazz.getAboveVisibility().toString()) );
        ps.println( String.format( "\t\tentityVisibility.setDownVisibility(%s);", clazz.isBelowVisibility()) );
        ps.println( String.format( "\t\tentityVisibility.setEntityPathBase(Q%s.%s);", clazz.getClassName(), clazz.getModelNameLowerCase() ));
        if (UpVisibility.TRUE_R_U_SPECIAL.equals(clazz.getAboveVisibility())) {
            if (clazz.hasProperty("createdByUser")) {
                ps.println( String.format( "\t\tentityVisibility.setSharedToSelf(true);") );
            }
            if (clazz.hasProperty("groupTypeFloor")) {
                ps.println( String.format( "\t\tentityVisibility.setSharedByGroupType(true);") );
            }
            if (clazz.hasProperty("roleShare")) {
                ps.println( String.format( "\t\tentityVisibility.setSharedByRole(true);") );
            }
            if (clazz.hasProperty("groupShare")) {
                ps.println( String.format( "\t\tentityVisibility.setSharedByGroup(true);") );
            }
        }
        ps.println( String.format( "\t}") );
        ps.println();

        ps.println( String.format( "\tpublic EntityVisibility getEntityVisibility() {") );
        ps.println( String.format( "\t\treturn entityVisibility;") );
        ps.println( String.format( "\t}") );
        ps.println();

        /**
         * LIST
         */
        ps.println( String.format( "\t/**" ) );
        ps.println( String.format( "\t * LIST" ) );
        ps.println( String.format( "\t */" ) );
        ps.println( ! clazz.isGenerateControllerList() ? "\t/*" : "" );
        ps.println( String.format( "\t@GET" ) );
        ps.println( String.format( "\t@Path(\"/\")" ) );
        ps.println( String.format( "\t@Produces(MediaType.APPLICATION_JSON)" ) );
        ps.println( "\t// 1a. Limit access based on CLASS level resources" );
        if (clazz.getModelNameLowerCase().equals("folder")) {
            ps.println("\t@RequiresAuthentication");
        }else{
            ps.println(String.format("\t@RequiresPermissions(value={\"%s:r\"})", clazz.getModelNameLowerCase()));
        }
        ps.println( String.format( "\t@ApiOperation(position=1, value=\"Get a List of %s%s\")", clazz.getModelNamePlural(), ag ) );
        if (  (! clazz.getModelNameLowerCase().startsWith("group")) && (!clazz.getModelNameLowerCase().startsWith("thing")) && (!clazz.getModelNameLowerCase().startsWith("notification")) && (!clazz.getModelNameLowerCase().startsWith("favorite"))&&
                (!clazz.getModelNameLowerCase().startsWith("parameter") && (!clazz.getModelNameLowerCase().startsWith("recent")))) {
            ps.println( String.format( "\tpublic Response list%s( @QueryParam(\"pageSize\") Integer pageSize, @QueryParam(\"pageNumber\") Integer pageNumber"
                    + ", @QueryParam(\"order\") String order, @QueryParam(\"where\") String where, @Deprecated @QueryParam(\"extra\") String extra, @Deprecated @QueryParam(\"only\") String only, @QueryParam(\"visibilityGroupId\") Long visibilityGroupId"
                    + ", @DefaultValue(\"\") @QueryParam(\"upVisibility\") String upVisibility"
                    + ", @DefaultValue(\"\") @QueryParam(\"downVisibility\") String downVisibility"
                    + ", @DefaultValue(\"false\") @QueryParam(\"returnFavorite\") boolean returnFavorite"
                    + ", @ApiParam(value = \"Extends nested properties\") @QueryParam(\"extend\") String extend, @ApiParam(value = \"Projects only nested properties\") @QueryParam(\"project\") String project"
                    + " )", clazz.getModelNamePlural() ) );
        }else{
            if (clazz.getModelNameLowerCase().startsWith("favorite") || clazz.getModelNameLowerCase().startsWith("recent")) {
                ps.println(String.format("\tpublic Response list%s(@QueryParam(\"typeElement\") String typeElement, @QueryParam(\"pageSize\") Integer pageSize, @QueryParam(\"pageNumber\") Integer pageNumber"
                        + ", @QueryParam(\"order\") String order, @QueryParam(\"where\") String where, @Deprecated @QueryParam(\"extra\") String extra, @Deprecated @QueryParam(\"only\") String only, @QueryParam(\"visibilityGroupId\") Long visibilityGroupId"
                        + ", @DefaultValue(\"\") @QueryParam(\"upVisibility\") String upVisibility"
                        + ", @DefaultValue(\"\") @QueryParam(\"downVisibility\") String downVisibility"
                        + ", @ApiParam(value = \"Extends nested properties\") @QueryParam(\"extend\") String extend, @ApiParam(value = \"Projects only nested properties\") @QueryParam(\"project\") String project"
                        + " )", clazz.getModelNamePlural()));
            }else{
                ps.println(String.format("\tpublic Response list%s( @QueryParam(\"pageSize\") Integer pageSize, @QueryParam(\"pageNumber\") Integer pageNumber"
                        + ", @QueryParam(\"order\") String order, @QueryParam(\"where\") String where, @Deprecated @QueryParam(\"extra\") String extra, @Deprecated @QueryParam(\"only\") String only, @QueryParam(\"visibilityGroupId\") Long visibilityGroupId"
                        + ", @DefaultValue(\"\") @QueryParam(\"upVisibility\") String upVisibility"
                        + ", @DefaultValue(\"\") @QueryParam(\"downVisibility\") String downVisibility"
                        + ", @ApiParam(value = \"Extends nested properties\") @QueryParam(\"extend\") String extend, @ApiParam(value = \"Projects only nested properties\") @QueryParam(\"project\") String project"
                        + " )", clazz.getModelNamePlural()));
            }
        }

        ps.println( "\t{" );

        ps.println( String.format( "\t\tPagination pagination = new Pagination( pageNumber, pageSize );" ) );
        ps.println();
        ps.println( String.format( "\t\tBooleanBuilder be = new BooleanBuilder();" ) );
        if( ! clazz.hasGroup() )
            ps.println( "\t\t//TODO: how to handle case without a group property ?" );
        ps.println( "\t\t// 2. Limit visibility based on user's group and the object's group (group based authorization)" );
        ps.println( String.format( "\t\tGroup visibilityGroup = VisibilityUtils.getVisibilityGroup(%s.class.getCanonicalName(), visibilityGroupId);", clazz.getClassName()));
        ps.println( "\t\tEntityVisibility entityVisibility = getEntityVisibility();" );

        if ("Group".equals(clazz.getClassName()))
        {
            ps.println( String.format( "\t\t%sbe = be.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, Q%s.%s, visibilityGroup, upVisibility, downVisibility ) );", clazz.hasGroup() ? "" : "//",clazz.getModelName(), clazz.getModelNameLowerCase()));
        } else
        {
            ps.println( String.format( "\t\t%sbe = be.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, Q%s.%s,  visibilityGroup, upVisibility, downVisibility ) );", clazz.hasGroup() ? "" : "//",clazz.getModelName(), clazz.getModelNameLowerCase()));
        }
        ps.println( "\t\t// 4. Implement filtering" );
        ps.println( String.format( "\t\tbe = be.and( QueryUtils.buildSearch( Q%s.%s, where ) );", clazz.getModelName(), clazz.getModelNameLowerCase() ) );
        ps.println();
        ps.println( String.format( "\t\tLong count = %s.getInstance().countList( be );", clazz.getServiceName() ) );
        ps.println( String.format( "\t\tList<Map<String, Object>> list = new LinkedList<Map<String, Object>>();" ) );
        //ps.println( String.format( "\t\tMap<String, Boolean> permissionCache = new HashMap<>();" ) );
        ps.println( "\t\t// 3. Implement pagination" );
        if (clazz.getModelName().equals("Zone")){
            ps.println( String.format( "\t\tHibernateDAOUtils.enableQueryCacheForAllQueries = false;"));
        }
        ps.println( String.format( "\t\tfor( %s %s : %s.getInstance().listPaginated( be, pagination, order ) ) ", clazz.getModelName(), clazz.getModelNameLowerCase(),
                clazz.getServiceName() ) );
        ps.println( "\t\t{" );
        ps.println( "\t\t\t// Additional filter" );
        ps.println( String.format( "\t\t\tif (!includeInSelect(%s))",clazz.getModelNameLowerCase()));
        ps.println( "\t\t\t{" );
        ps.println( String.format( "\t\t\t\tcontinue;"));
        ps.println( "\t\t\t}" );

        ps.println( "\t\t\t// 5a. Implement extra" );
        ps.println( String.format( "\t\t\tMap<String,Object> publicMap = QueryUtils.mapWithExtraFields( %s, extra, getExtraPropertyNames());", clazz.getModelNameLowerCase() ) );
        ps.println( String.format( "\t\t\tpublicMap = QueryUtils.mapWithExtraFieldsNested(%s, publicMap, extend, getExtraPropertyNames());", clazz.getModelNameLowerCase() ) );
        ps.println( String.format( "\t\t\taddToPublicMap(%s, publicMap, extra);", clazz.getModelNameLowerCase()) );
        //ps.println( "\t\t\t// 1[b,c]. Restrict access based on OBJECT and PROPERTY level read permissions" );
        //ps.println( String.format( "\t\t\tQueryUtils.filterReadPermissions( %s.class, publicMap, permissionCache, ignoreFieldsForPermissions() );", clazz.getModelName() ) );
        ps.println( "\t\t\t// 5b. Implement only" );
        ps.println( String.format( "\t\t\tQueryUtils.filterOnly( publicMap, only, extra );") );
        ps.println( String.format( "\t\t\tQueryUtils.filterProjectionNested( publicMap, project, extend );") );
        ps.println( String.format( "\t\t\tlist.add( publicMap );" ) );
        ps.println( "\t\t}" );
        if (clazz.getModelName().equals("Zone")){
            ps.println( String.format( "\t\tHibernateDAOUtils.enableQueryCacheForAllQueries = true;"));
        }

        if (  (! clazz.getModelNameLowerCase().startsWith("group")) && (!clazz.getModelNameLowerCase().startsWith("thing")) && (!clazz.getModelNameLowerCase().startsWith("notification"))
                && (!clazz.getModelNameLowerCase().startsWith("favorite")) && (!clazz.getModelNameLowerCase().startsWith("recent"))
                && (!clazz.getModelNameLowerCase().startsWith("parameter"))){
            ps.println( String.format("\t\tif (returnFavorite) {"));
            ps.println( String.format("\t\tUser user = (User) SecurityUtils.getSubject().getPrincipal();"));
            if (  (clazz.getModelName().startsWith("Report"))) {
                ps.println(String.format("\t\tlist = FavoriteService.getInstance().addFavoritesToList(list,user.getId(),\"report\");"));
            }else{
                ps.println(String.format("\t\tlist = FavoriteService.getInstance().addFavoritesToList(list,user.getId(),\"" + clazz.getModelName().toLowerCase() + "\");"));
            }
            ps.println( String.format("\t\t}"));
        }

        if (  (! clazz.getModelNameLowerCase().startsWith("group")) && (!clazz.getModelNameLowerCase().startsWith("thing")) && (!clazz.getModelNameLowerCase().startsWith("notification")) &&
                (!clazz.getModelNameLowerCase().startsWith("favorite")) && (!clazz.getModelNameLowerCase().startsWith("recent"))
                && (!clazz.getModelNameLowerCase().startsWith("parameter"))) {
            ps.println( "\t\t\taddToResults(list, extend, project, visibilityGroupId, upVisibility, downVisibility, returnFavorite);");
        }else{
            ps.println( "\t\t\taddToResults(list, extend, project, visibilityGroupId, upVisibility, downVisibility);");
        }


        ps.println( String.format( "\t\tMap<String,Object> mapResponse = new HashMap<String,Object>();" ) );
        ps.println( String.format( "\t\tmapResponse.put( \"total\", count );" ) );
        ps.println( String.format( "\t\tmapResponse.put( \"results\", list );" ) );
        ps.println( String.format( "\t\treturn RestUtils.sendOkResponse( mapResponse );" ) );
        ps.println( "\t}" );

        ps.println();
        ps.println( String.format( "\tpublic boolean includeInSelect( %s %s )", clazz.getModelName(), clazz.getModelNameLowerCase() ) );
        ps.println( "\t{" );
        ps.println( "\t\treturn true;" );
        ps.println( "\t}" );

        ps.println( ! clazz.isGenerateControllerList() ? "\t*/" : "" );

        /**
         * SELECT
         */
        ps.println();
        ps.println( ! clazz.isGenerateControllerSelect() ? "\t/*" : "" );
        ps.println( String.format( "\t@GET" ) );
        ps.println( String.format( "\t@Path(\"/{id}\")" ) );
        ps.println( String.format( "\t@Produces(MediaType.APPLICATION_JSON)" ) );
        ps.println( "\t// 1a. Limit access based on CLASS level resources" );
        ps.println( String.format( "\t@RequiresPermissions(value={\"%s:r:{id}\"})", clazz.getModelNameLowerCase() ) );
        ps.println( String.format( "\t@ApiOperation(position=2, value=\"Select a %s%s\")", clazz.getModelName(), ag ) );
        if (clazz.getModelNameLowerCase().equals("thing")|| clazz.getModelNameLowerCase().equals("user") || clazz.getModelNameLowerCase().equals("group") ||
                clazz.getModelNameLowerCase().equals("groupType") || clazz.getModelNameLowerCase().equals("role") || clazz.getModelNameLowerCase().equals("shift")
                || clazz.getModelNameLowerCase().equals("edgebox") || clazz.getModelNameLowerCase().equals("connection") || clazz.getModelNameLowerCase().equals("zone")
                || clazz.getModelNameLowerCase().equals("zoneType") || clazz.getModelNameLowerCase().equals("zoneGroup") || clazz.getModelNameLowerCase().equals("localMap")
                || clazz.getModelNameLowerCase().equals("logicalReader") || clazz.getModelNameLowerCase().equals("reportDefinition") || clazz.getModelNameLowerCase().equals("thingType")
                || clazz.getModelNameLowerCase().equals("scheduledRule")) {
            ps.println(String.format("\tpublic Response select%s( @PathParam(\"id\") Long id, @Deprecated @QueryParam(\"extra\") String extra, @Deprecated @QueryParam(\"only\") String only, "
                    + "@ApiParam(value = \"Extends nested properties\") @QueryParam(\"extend\") String extend, @ApiParam(value = \"Projects only nested properties\") @QueryParam(\"project\") String project, "
                    + "@QueryParam(\"createRecent\") @DefaultValue(\"false\") Boolean createRecent "
                    + ")", clazz.getModelNamePlural() ) );
        }else{
            ps.println(String.format("\tpublic Response select%s( @PathParam(\"id\") Long id, @Deprecated @QueryParam(\"extra\") String extra, @Deprecated @QueryParam(\"only\") String only, "
                    + " @ApiParam(value = \"Extends nested properties\") @QueryParam(\"extend\") String extend, @ApiParam(value = \"Projects only nested properties\") @QueryParam(\"project\") String project"
                    + ")", clazz.getModelNamePlural() ) );
        }
        ps.println( "\t{" );
        ps.println( String.format( "\t\t%s %s = %s.getInstance().get( id );", clazz.getModelName(), clazz.getModelNameLowerCase(), clazz.getServiceName() ) );
        if (clazz.isCheckNullValue()) {
            ps.println(String.format("\t\tif( %s == null )", clazz.getModelNameLowerCase()));
            ps.println("\t\t{");
            ps.println(String.format("\t\t\treturn RestUtils.sendBadResponse( String.format( \"%sId[%%d] not found\", id) );", clazz.getModelName()));
            ps.println("\t\t}");
        }
        if (clazz.getModelNameLowerCase().equals("user") || clazz.getModelNameLowerCase().equals("group") || clazz.getModelNameLowerCase().equals("groupType")
                || clazz.getModelNameLowerCase().equals("role") || clazz.getModelNameLowerCase().equals("shift")|| clazz.getModelNameLowerCase().equals("edgebox")
                || clazz.getModelNameLowerCase().equals("zone")|| clazz.getModelNameLowerCase().equals("zoneType") || clazz.getModelNameLowerCase().equals("zoneGroup")
                || clazz.getModelNameLowerCase().equals("localMap")|| clazz.getModelNameLowerCase().equals("logicalReader")|| clazz.getModelNameLowerCase().equals("reportDefinition")
                || clazz.getModelNameLowerCase().equals("scheduledRule")) {
            ps.println( String.format( "\t\tif ( createRecent ){" ) );
            switch (clazz.getModelNameLowerCase()){
                case "user":
                    ps.println(String.format("\t\t\tRecentService.getInstance().insertRecent(%s.getId(), %s.getUsername(),\"%s\",%s.getGroup());", clazz.getModelNameLowerCase(), clazz.getModelNameLowerCase(), clazz.getModelNameLowerCase().toLowerCase(), clazz.getModelNameLowerCase()));
                    break;
                case "reportDefinition":
                    ps.println(String.format("\t\t\tRecentService.getInstance().insertRecent(%s.getId(), %s.getName(),\"report\",%s.getGroup());", clazz.getModelNameLowerCase(), clazz.getModelNameLowerCase(),clazz.getModelNameLowerCase()));
                    break;
                case "group":
                    ps.println(String.format("\t\t\tRecentService.getInstance().insertRecent(%s.getId(), %s.getName(),\"%s\",%s.getTenantGroup());", clazz.getModelNameLowerCase(), clazz.getModelNameLowerCase(), clazz.getModelNameLowerCase().toLowerCase(),clazz.getModelNameLowerCase()));
                    break;
                default:
                    ps.println(String.format("\t\t\tRecentService.getInstance().insertRecent(%s.getId(), %s.getName(),\"%s\",%s.getGroup());", clazz.getModelNameLowerCase(), clazz.getModelNameLowerCase(), clazz.getModelNameLowerCase().toLowerCase(),clazz.getModelNameLowerCase()));

            }
            ps.println( String.format( "\t\t}" ) );
        }
        if( clazz.hasGroup() )
        {
        ps.println( String.format( "\t\t// 2. Limit visibility based on user's group and the object's group (group based authorization)" ) );
        ps.println( "\t\tEntityVisibility entityVisibility = getEntityVisibility();" );

        //ps.println( String.format( "\t\tGroup visibilityGroup = VisibilityUtils.getVisibilityGroup(%s.class.getCanonicalName(), null);", clazz.getClassName()));

          if ("Group".equals(clazz.getClassName()))
          {
              //ps.println( String.format( "\t\tVisibilityUtils.limitVisibilitySelect(visibilityGroup, %s, defaultUpVisibility, defaultDownVisibility, null, %s, null, null );", clazz.getModelNameLowerCase(), clazz.hasCreatedByUser()?clazz.getModelNameLowerCase()+".getCreatedByUser()":null) );
              ps.println( String.format( "\t\tGeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, %s);", clazz.getModelNameLowerCase()));
          } else {
              //ps.println( String.format( "\t\tVisibilityUtils.limitVisibilitySelect(visibilityGroup, %s.%s(), defaultUpVisibility, defaultDownVisibility, null, %s, null, null );", clazz.getModelNameLowerCase(), clazz.getVisibilityGroupGetter(), clazz.hasCreatedByUser()?clazz.getModelNameLowerCase()+".getCreatedByUser()":null ) );
              ps.println( String.format( "\t\tGeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, %s);", clazz.getModelNameLowerCase()));
          }
        }
        else
        {
        	ps.println( String.format( "\t\t//2. TODO: Limit visibility based on user's group and the object's group (group based authorization)" ) );

        }

        ps.println( String.format( "\t\tvalidateSelect( %s );", clazz.getModelNameLowerCase() ) );
        ps.println( "\t\t// 5a. Implement extra" );
        ps.println( String.format( "\t\tMap<String,Object> publicMap = QueryUtils.mapWithExtraFields( %s, extra, getExtraPropertyNames());", clazz.getModelNameLowerCase() ) );
        ps.println( String.format( "\t\tpublicMap = QueryUtils.mapWithExtraFieldsNested( %s, publicMap, extend, getExtraPropertyNames());", clazz.getModelNameLowerCase() ) );
        ps.println( String.format( "\t\taddToPublicMap(%s, publicMap, extra);", clazz.getModelNameLowerCase()) );
        //ps.println( "\t\t// 1[b,c]. Restrict access based on OBJECT and PROPERTY level read permissions" );
        //ps.println( String.format( "\t\tQueryUtils.filterReadPermissions( %s.class, publicMap, ignoreFieldsForPermissions() );", clazz.getModelName() ) );
        ps.println( "\t\t// 5b. Implement only" );
        ps.println( String.format( "\t\tQueryUtils.filterOnly( publicMap, only, extra );" ) );
        ps.println( String.format( "\t\tQueryUtils.filterProjectionNested( publicMap, project, extend );" ) );
        ps.println( String.format( "\t\treturn RestUtils.sendOkResponse( publicMap );" ) );
        ps.println( "\t}" );

        ps.println();
        ps.println( String.format( "\tpublic void validateSelect( %s %s )", clazz.getModelName(), clazz.getModelNameLowerCase() ) );
        ps.println( "\t{" );
        ps.println( "" );
        ps.println( "\t}" );

        ps.println( ! clazz.isGenerateControllerSelect() ? "\t*/" : "" );

        /**
         * INSERT
         */
        ps.println( ! clazz.isGenerateControllerInsert() ? "\t/*" : "" );
        ps.println();
        ps.println( String.format( "\t@PUT" ) );
        ps.println( String.format( "\t@Path(\"/\")" ) );
        ps.println( String.format( "\t@Produces(MediaType.APPLICATION_JSON)" ) );
        ps.println( String.format( "\t@Consumes(MediaType.APPLICATION_JSON)" ) );
        ps.println( "\t// 1a. Limit access based on CLASS level resources" );
        ps.println( String.format( "\t@RequiresPermissions(value={\"%s:i\"})", clazz.getModelNameLowerCase() ) );
        ps.println( String.format( "\t@ApiOperation(position=3, value=\"Insert a %s%s\")", clazz.getModelName(), ag ) );
        if (  ( clazz.getModelNameLowerCase().equals("group")) || (clazz.getModelNameLowerCase().equals("shift")) || (clazz.getModelNameLowerCase().equals("connection")) || (clazz.getModelNameLowerCase().equals("zone"))
                || clazz.getModelNameLowerCase().equals("user") || clazz.getModelNameLowerCase().equals("edgebox") || clazz.getModelNameLowerCase().equals("zoneType") || clazz.getModelNameLowerCase().equals("zoneGroup")
                || clazz.getModelNameLowerCase().equals("localMap") || clazz.getModelNameLowerCase().equals("logicalReader") || clazz.getModelNameLowerCase().equals("reportDefinition")|| clazz.getModelNameLowerCase().equals("thingType")) {
            ps.println(String.format("\tpublic Response insert%s( Map<String, Object> map,@QueryParam(\"createRecent\") @DefaultValue(\"false\") Boolean createRecent )", clazz.getModelName()));
        }else{
            ps.println(String.format("\tpublic Response insert%s( Map<String, Object> map )", clazz.getModelName()));
        }
        ps.println( "\t{" );

        //ps.println( "\t\tSystem.out.println( \"DEBUG: map=\" + map );" );
        //ps.println( "\t\tfor( Entry<String, Object> e : map.entrySet() )" );
        //ps.println( "\t\t{" );
        //ps.println( "\t\t\tSystem.out.println( \"DEBUG: map.name=\" + e.getKey() + \" map.value=\" + e.getValue() + \" map.class=\" + e.getValue().getClass() );" );
        //ps.println( "\t\t}" );

        if( clazz.hasGroup() )
        {
        	ps.println( String.format( "\t\t// 2. Limit visibility based on user's group and the object's group (group based authorization)" ) );
            //ps.println( String.format( "\t\tGroup visibilityGroup = VisibilityUtils.getVisibilityGroup(%s.class.getCanonicalName(), null);", clazz.getClassName()));
        	//ps.println( String.format( "\t\tVisibilityUtils.limitVisibilityInsert( visibilityGroup, VisibilityUtils.getObjectGroup(map) );") );
            ps.println( "\t\tEntityVisibility entityVisibility = getEntityVisibility();" );
            ps.println( "\t\tGeneralVisibilityUtils.limitVisibilityInsert(entityVisibility, VisibilityUtils.getObjectGroup(map));" );
        }
        else
        {
        	ps.println( String.format( "\t\t//2. TODO: Limit visibility based on user's group and the object's group (group based authorization)" ) );

        }

        //ps.println( "\t\t// 1[b,c]. Restrict access based on OBJECT and PROPERTY level write permissions" );
        //ps.println( String.format( "\t\tQueryUtils.filterWritePermissions( %s.class, map );", clazz.getModelName() ) );
        ps.println( String.format( "\t\t%s %s = new %s();", clazz.getModelName(), clazz.getModelNameLowerCase(), clazz.getModelName() ) );

        ps.println( String.format( "\t\t// 7. handle insert and update" ) );
        ps.println( String.format( "\t\tBeanUtils.setProperties( map, %s );", clazz.getModelNameLowerCase() ) );

        ps.println( String.format( "\t\t// 6. handle validation in an Extensible manner" ) );
        ps.println( String.format( "\t\tvalidateInsert( %s );", clazz.getModelNameLowerCase() ) );

        ps.println( String.format( "\t\t%s.getInstance().insert( %s );", clazz.getServiceName(), clazz.getModelNameLowerCase() ) );
        ps.println( "\t\t// 1[b,c]. Restrict access based on OBJECT and PROPERTY level read permissions" );
        if (clazz.getModelNameLowerCase().equals("shift")){
            ps.println( String.format( "\t\tif ( createRecent ){" ) );
            ps.println( String.format( "\t\t\tRecentService.getInstance().insertRecent(shift.getId(), shift.getName(),\"shift\",shift.getGroup());" ) );
            ps.println( String.format( "\t\t}" ) );
        }
        ps.println( String.format( "\t\tMap<String,Object> publicMap = %s.publicMap();", clazz.getModelNameLowerCase() ) );
        //ps.println( String.format( "\t\tQueryUtils.filterReadPermissions( %s.class, publicMap, ignoreFieldsForPermissions() );", clazz.getModelName() ) );
        ps.println( String.format( "\t\treturn RestUtils.sendCreatedResponse( publicMap );" ) );
        ps.println( "\t}" );

        ps.println();
        ps.println( String.format( "\tpublic void validateInsert( %s %s )", clazz.getModelName(), clazz.getModelNameLowerCase() ) );
        ps.println( "\t{" );
        ps.println( "" );
        ps.println( "\t}" );
        ps.println( ! clazz.isGenerateControllerInsert() ? "\t*/" : "" );

        /**
         * UPDATE
         */
        ps.println();
        ps.println( ! clazz.isGenerateControllerUpdate() ? "\t/*" : "" );
        ps.println( String.format( "\t@PATCH" ) );
        ps.println( String.format( "\t@Path(\"/{id}\")" ) );
        ps.println( String.format( "\t@Produces(MediaType.APPLICATION_JSON)" ) );
        ps.println( String.format( "\t@Consumes(MediaType.APPLICATION_JSON)" ) );
        ps.println( "\t// 1a. Limit access based on CLASS level resources" );
        if (clazz.getModelNameLowerCase().equals("folder")) {
            ps.println("\t@RequiresAuthentication");
        }else{
            ps.println(String.format("\t@RequiresPermissions(value={\"%s:u:{id}\"})", clazz.getModelNameLowerCase()));
        }
        ps.println( String.format( "\t@ApiOperation(position=4, value=\"Update a %s%s\")", clazz.getModelName(), ag ) );
        ps.println( String.format( "\tpublic Response update%s( @PathParam(\"id\") Long id, Map<String, Object> map )", clazz.getModelName() ) );
        ps.println( "\t{" );
        //ps.println( "\t\t// 1[b,c]. Restrict access based on OBJECT and PROPERTY level write permissions" );
        //ps.println( String.format( "\t\tQueryUtils.filterWritePermissions( %s.class, map );", clazz.getModelName() ) );
        ps.println( String.format( "\t\t%s %s = %s.getInstance().get( id );", clazz.getModelName(), clazz.getModelNameLowerCase(), clazz.getServiceName() ) );
        if (clazz.isCheckNullValue()) {
            ps.println(String.format("\t\tif( %s == null )", clazz.getModelNameLowerCase()));
            ps.println("\t\t{");
            ps.println(String.format("\t\t\treturn RestUtils.sendBadResponse( String.format( \"%sId[%%d] not found\", id) );", clazz.getModelName()));
            ps.println("\t\t}");
        }
        if( clazz.hasGroup() )
        {
        	ps.println( String.format( "\t\t// 2. Limit visibility based on user's group and the object's group (group based authorization)" ) );
            //ps.println( String.format( "\t\tGroup visibilityGroup = VisibilityUtils.getVisibilityGroup(%s.class.getCanonicalName(), null);", clazz.getClassName()));
            ps.println( "\t\tEntityVisibility entityVisibility = getEntityVisibility();" );
            if ("Group".equals(clazz.getClassName()))
            {
                //ps.println( String.format( "\t\tVisibilityUtils.limitVisibilityUpdate( visibilityGroup, VisibilityUtils.getObjectGroup(map), %s, %s );", clazz.getModelNameLowerCase(),  clazz.hasCreatedByUser()?clazz.getModelNameLowerCase()+".getCreatedByUser()":null ) );
                ps.println( String.format( "\t\tGeneralVisibilityUtils.limitVisibilityUpdate( entityVisibility, %s, VisibilityUtils.getObjectGroup(map));", clazz.getModelNameLowerCase()));
            } else
            {
                //ps.println( String.format( "\t\tVisibilityUtils.limitVisibilityUpdate( visibilityGroup, VisibilityUtils.getObjectGroup(map), %s.%s(), %s );", clazz.getModelNameLowerCase(), clazz.getVisibilityGroupGetter(),  clazz.hasCreatedByUser()?clazz.getModelNameLowerCase()+".getCreatedByUser()":null ) );
                ps.println( String.format( "\t\tGeneralVisibilityUtils.limitVisibilityUpdate( entityVisibility, %s, VisibilityUtils.getObjectGroup(map));", clazz.getModelNameLowerCase()));
            }
        }
        else
        {
        	ps.println( String.format( "\t\t//2. TODO: Limit visibility based on user's group and the object's group (group based authorization)" ) );

        }
        ps.println( String.format( "\t\t// 7. handle insert and update" ) );
        ps.println( String.format( "\t\tBeanUtils.setProperties( map, %s );", clazz.getModelNameLowerCase() ) );

        if (clazz.getModelNameLowerCase().equals("shift")){
            ps.println( String.format( "\t\t\tRecentService.getInstance().updateName(shift.getId(), shift.getName(),\"shift\");" ) );

        }

        ps.println( String.format( "\t\t// 6. handle validation in an Extensible manner" ) );
        ps.println( String.format( "\t\tvalidateUpdate( %s );", clazz.getModelNameLowerCase() ) );
        ps.println( String.format( "\t\t%s.getInstance().update( %s );", clazz.getServiceName(), clazz.getModelNameLowerCase() ) );
        ps.println( String.format( "\t\tMap<String,Object> publicMap = %s.publicMap();", clazz.getModelNameLowerCase() ) );
        //ps.println( "\t\t// 1[b,c]. Restrict access based on OBJECT and PROPERTY level read permissions" );
        //ps.println( String.format( "\t\tQueryUtils.filterReadPermissions( %s.class, publicMap, ignoreFieldsForPermissions() );", clazz.getModelName() ) );
        ps.println( String.format( "\t\treturn RestUtils.sendOkResponse( publicMap );" ) );
        ps.println( "\t}" );

        ps.println();
        ps.println( String.format( "\tpublic void validateUpdate( %s %s )", clazz.getModelName(), clazz.getModelNameLowerCase() ) );
        ps.println( "\t{" );
        ps.println( "" );
        ps.println( "\t}" );
        ps.println( ! clazz.isGenerateControllerUpdate() ? "\t*/" : "" );

        /**
         * DELETE
         */
        ps.println( ! clazz.isGenerateControllerDelete() ? "\t/*" : "" );
        ps.println();
        ps.println( String.format( "\t@DELETE" ) );
        ps.println( String.format( "\t@Path(\"/{id}\")" ) );
        ps.println( String.format( "\t@Produces(MediaType.APPLICATION_JSON)" ) );
        ps.println( String.format( "\t@RequiresPermissions(value={\"%s:d:{id}\"})", clazz.getModelNameLowerCase() ) );
        ps.println( "\t// 1a. Limit access based on CLASS level resources" );
        ps.println( String.format( "\t@ApiOperation(position=5, value=\"Delete a %s%s\")", clazz.getModelName(), ag ) );
        ps.println( String.format( "\tpublic Response delete%s( @PathParam(\"id\") Long id )", clazz.getModelName() ) );
        ps.println( "\t{" );
        ps.println( "\t\t// 1c. TODO: Restrict access based on OBJECT level read permissions" );
        ps.println( String.format( "\t\t%s %s = %s.getInstance().get( id );", clazz.getModelName(), clazz.getModelNameLowerCase(), clazz.getServiceName() ) );
        if (clazz.isCheckNullValue()) {
            ps.println(String.format("\t\tif( %s == null )", clazz.getModelNameLowerCase()));
            ps.println("\t\t{");
            ps.println(String.format("\t\t\treturn RestUtils.sendBadResponse( String.format( \"%sId[%%d] not found\", id) );", clazz.getModelName()));
            ps.println("\t\t}");
        }
        if (clazz.getModelNameLowerCase().equals("shift")|| clazz.getModelNameLowerCase().equals("edgebox")|| clazz.getModelNameLowerCase().equals("logicalReader")){
            ps.println(String.format("RecentService.getInstance().deleteRecent(id,\"%s\");",clazz.getModelNameLowerCase().toLowerCase()));

        }
        if (clazz.getModelNameLowerCase().equals("reportDefinition")){
            ps.println("RecentService.getInstance().deleteRecent(id,\"report\");");
        }
        if( clazz.hasGroup() )
        {
        	ps.println( String.format( "\t\t// 2. Limit visibility based on user's group and the object's group (group based authorization)" ) );
            ps.println( "\t\tEntityVisibility entityVisibility = getEntityVisibility();" );
            if ("Group".equals(clazz.getClassName()))
            {
                ps.println( String.format( "\t\tGeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, %s );", clazz.getModelNameLowerCase() ) );
            } else
            {
                ps.println( String.format( "\t\tGeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, %s );", clazz.getModelNameLowerCase() ) );
            }

        }
        else
        {
        	ps.println( String.format( "\t\t//2. TODO: Limit visibility based on user's group and the object's group (group based authorization)" ) );

        }
        ps.println( String.format( "\t\t// handle validation in an Extensible manner" ) );
        ps.println( String.format( "\t\tvalidateDelete( %s );", clazz.getModelNameLowerCase() ) );
        ps.println( String.format( "\t\t%s.getInstance().delete( %s );", clazz.getServiceName(), clazz.getModelNameLowerCase() ) );
        ps.println( String.format( "\t\treturn RestUtils.sendDeleteResponse();" ) );
        ps.println( "\t}" );
        ps.println();
        ps.println( String.format( "\tpublic void validateDelete( %s %s )", clazz.getModelName(), clazz.getModelNameLowerCase() ) );
        ps.println( "\t{" );
        ps.println( "" );
        ps.println( "\t}" );
        ps.println( ! clazz.isGenerateControllerDelete() ? "\t*/" : "" );

        ps.println( String.format( "\tpublic void addToPublicMap(%s %s, Map<String,Object> publicMap, String extra )", clazz.getModelName(), clazz.getModelNameLowerCase()));
        ps.println( "\t{" );
        ps.println( "\t}" );
        ps.println( "" );

        ps.print( "\tpublic void addToResults(List<Map<String,Object>> results, String extend, String project, Long visibilityGroupId, String upVisibility, String downVisibility");
        if (  (! clazz.getModelNameLowerCase().startsWith("group")) && (!clazz.getModelNameLowerCase().startsWith("thing")) && (!clazz.getModelNameLowerCase().startsWith("notification")) && (!clazz.getModelNameLowerCase().startsWith("favorite"))
                && (!clazz.getModelNameLowerCase().startsWith("parameter")) && (!clazz.getModelNameLowerCase().startsWith("recent"))) {
            ps.print( "\t\t\t, boolean returnFavorite");
        }
        ps.println( ")");
        ps.println( "\t{" );
        ps.println( "\t}" );
        ps.println( "" );


        ps.println( String.format( "\tpublic List<String> getExtraPropertyNames()"));
        ps.println( "\t{" );
        ps.println( "\t\treturn new ArrayList<String>();" );
        ps.println( "\t}" );
        ps.println( "" );

        /**
         * Filter Urls
         */
        Set<String> references = new HashSet<>();
        references.add(clazz.getClassName());
        for( Property p : clazz.getProperties() ) {

            if (!p.isDerived() && p.isEntity()) {
                Clazz clazz2 = Clazz.get(p.getShortType());
                if (!references.contains(clazz2.getClassName()) && clazz2.isGenerateController()) {
                    references.add(clazz2.getClassName());

                    ps.println(String.format("\t/**"));
                    ps.println(String.format("\t * FILTER LIST"));
                    ps.println(String.format("\t */"));
                    ps.println(!clazz2.isGenerateControllerList() ? "\t/*" : "");
                    ps.println(String.format("\t@GET"));
                    ps.println(String.format("\t@Path(\"/" + clazz2.getModelNameLowerCase() + "/\")"));
                    ps.println(String.format("\t@Produces(MediaType.APPLICATION_JSON)"));
                    ps.println(String.format("\t@RequiresAuthentication"));
                    //ps.println(String.format("\t@RequiresPermissions(value={\"%s:r\"})", clazz.getModelNameLowerCase()));
                    ps.println(String.format("\t@ApiOperation(position=1, value=\"Get a List of %s%s\")", clazz2.getModelNamePlural(), ag));
                    if ((clazz.getModelNameLowerCase().equals("thing") && clazz2.hasParent()) || (clazz.getModelNameLowerCase().equals("thing") && clazz2.getModelNameLowerCase().equals("user")) || clazz.getModelNameLowerCase().equals("zoneType")) {
                        ps.println(String.format("\tpublic Response list%s( @QueryParam(\"pageSize\") Integer pageSize, @QueryParam(\"pageNumber\") Integer pageNumber"
                                + ", @QueryParam(\"order\") String order, @QueryParam(\"where\") String where, @Deprecated @QueryParam(\"extra\") String extra, @Deprecated @QueryParam(\"only\") String only"
                                + ", @QueryParam(\"visibilityGroupId\") Long visibilityGroupId"
                                + ", @DefaultValue(\"\") @QueryParam(\"upVisibility\") String upVisibility"
                                + ", @DefaultValue(\"\") @QueryParam(\"downVisibility\") String downVisibility"
                                + ", @QueryParam(\"topId\") String topId"
                                + ", @ApiParam(value = \"Extends nested properties\") @QueryParam(\"extend\") String extend, @ApiParam(value = \"Projects only nested properties\") @QueryParam(\"project\") String project"
                                + "  )", clazz2.getModelNamePlural()));
                    }else{
                        ps.println(String.format("\tpublic Response list%s( @QueryParam(\"pageSize\") Integer pageSize, @QueryParam(\"pageNumber\") Integer pageNumber"
                                + ", @QueryParam(\"order\") String order, @QueryParam(\"where\") String where, @Deprecated @QueryParam(\"extra\") String extra, @Deprecated @QueryParam(\"only\") String only"
                                + ", @QueryParam(\"visibilityGroupId\") Long visibilityGroupId"
                                + ", @DefaultValue(\"\") @QueryParam(\"upVisibility\") String upVisibility"
                                + ", @DefaultValue(\"\") @QueryParam(\"downVisibility\") String downVisibility"
                                + ", @ApiParam(value = \"Extends nested properties\") @QueryParam(\"extend\") String extend, @ApiParam(value = \"Projects only nested properties\") @QueryParam(\"project\") String project"
                                + "  )", clazz2.getModelNamePlural()));
                    }
                    ps.println("\t{");
                    ps.println("\t\tvalidateListPermissions();");
                    ps.println("\t\t"+clazz2.getControllerName() + " c = new "+ clazz2.getControllerName()+"();");
                    if (clazz2.getModelNameLowerCase().equals("group") || clazz2.getModelNameLowerCase().equals("groupType")) {
                        ps.println( String.format( "\t\tGroup visibilityGroup = VisibilityUtils.getVisibilityGroup(%s.class.getCanonicalName(), visibilityGroupId);", clazz.getClassName()));
                        ps.println( String.format( "\t\tRiotShiroRealm.getOverrideVisibilityCache().put(%s, visibilityGroup);", clazz2.getClassName() + ".class.getCanonicalName()"));
                    }
                    if (  (! clazz2.getModelNameLowerCase().startsWith("group")) && (!clazz2.getModelNameLowerCase().startsWith("thing"))
                            && (!clazz2.getModelNameLowerCase().startsWith("notification"))
                            && (!clazz2.getModelNameLowerCase().startsWith("log"))
                            && (!clazz2.getModelNameLowerCase().startsWith("action"))) {
                        ps.println("\t\treturn c.list" + clazz2.getModelNamePlural() + " (pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility,false, extend, project);");
                    }else{
                        if ( (clazz2.getModelNameLowerCase().startsWith("reportAction")) || (clazz2.getModelNameLowerCase().startsWith("log")) || clazz2.getModelNameLowerCase().startsWith("action")) {
                            ps.println("\t\treturn c.list" + clazz2.getModelNamePlural() + " (pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility);");
                        }else{
                            ps.println("\t\treturn c.list" + clazz2.getModelNamePlural() + " (pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, extend, project);");
                        }
                    }
                    ps.println("\t}");
                    ps.println(!clazz.isGenerateControllerList() ? "\t*/" : "");

                    if (clazz2.hasParent()) {

                        ps.println(String.format("\t/**"));
                        ps.println(String.format("\t * FILTER TREE"));
                        ps.println(String.format("\t */"));
                        ps.println(!clazz2.isGenerateControllerList() ? "\t/*" : "");
                        ps.println(String.format("\t@GET"));
                        ps.println(String.format("\t@Path(\"/" + clazz2.getModelNameLowerCase() + "/tree\")"));
                        ps.println(String.format("\t@Produces(MediaType.APPLICATION_JSON)"));
                        ps.println(String.format("\t@RequiresAuthentication"));
                        //ps.println(String.format("\t@RequiresPermissions(value={\"%s:r\"})", clazz.getModelNameLowerCase()));
                        ps.println(String.format("\t@ApiOperation(position=1, value=\"Get a Tree of %s%s\")", clazz2.getModelNamePlural(), ag));
                        ps.println(String.format("\tpublic Response list%sInTree( @QueryParam(\"pageSize\") Integer pageSize, @QueryParam(\"pageNumber\") Integer pageNumber"
                                + ", @QueryParam(\"order\") String order, @QueryParam(\"where\") String where, @Deprecated @QueryParam(\"extra\") String extra, @Deprecated @QueryParam(\"only\") String only"
                                + ", @QueryParam(\"visibilityGroupId\") Long visibilityGroupId"
                                + ", @DefaultValue(\"\") @QueryParam(\"upVisibility\") String upVisibility"
                                + ", @DefaultValue(\"\") @QueryParam(\"downVisibility\") String downVisibility"
                                + ", @QueryParam(\"topId\") String topId, @ApiParam(value = \"Extends nested properties\") @QueryParam(\"extend\") String extend, @ApiParam(value = \"Projects only nested properties\") @QueryParam(\"project\") String project )", clazz2.getModelNamePlural()));
                        ps.println("\t{");
                        ps.println("\t\tvalidateListPermissions();");
                        ps.println("\t\t"+clazz2.getControllerName() + " c = new "+ clazz2.getControllerName()+"();");
                        if (clazz2.getModelNameLowerCase().equals("group") || clazz2.getModelNameLowerCase().equals("groupType")) {
                            ps.println( String.format( "\t\tGroup visibilityGroup = VisibilityUtils.getVisibilityGroup(%s.class.getCanonicalName(), visibilityGroupId);", clazz.getClassName()));
                            ps.println( String.format( "\t\tRiotShiroRealm.getOverrideVisibilityCache().put(%s, visibilityGroup);", clazz2.getClassName() + ".class.getCanonicalName()"));
                        }
                        if (clazz2.getModelNameLowerCase().equals("group") || clazz2.getModelNameLowerCase().equals("groupType")) {
                            ps.println("\t\treturn c.list" + clazz2.getModelNamePlural() + "InTree(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, topId, false, extend, project);");
                        }else{
                            ps.println("\t\treturn c.list" + clazz2.getModelNamePlural() + "InTree(pageSize, pageNumber, order, where, extra, only, visibilityGroupId, upVisibility, downVisibility, topId, extend, project);");
                        }
                        ps.println("\t}");
                        ps.println(!clazz.isGenerateControllerList() ? "\t*/" : "");

                    }

                }
            }
        }
        ps.println( String.format( "\tpublic void validateListPermissions() {"));
        ps.println( String.format( "\t\t if (!(PermissionsUtils.isPermitted(SecurityUtils.getSubject(),\"%s:r\"))) {", clazz.getModelNameLowerCase()) );
        ps.println( String.format( "\t\t\t  throw new com.tierconnect.riot.sdk.servlet.exception.ForbiddenException(\"Not Authorized, Access Denied\");"));
        ps.println( String.format( "\t\t}"));
        ps.println(String.format("\t}"));


        ps.println( "}" );
        ps.println();
        ps.close();
    }
}
