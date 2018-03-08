package com.tierconnect.riot.iot.controllers;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.controllers.GroupController;
import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.FavoriteService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.GeneralVisibilityUtils;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.iot.entities.Folder;
import com.tierconnect.riot.iot.entities.QFolder;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.entities.QFolder;
import com.tierconnect.riot.iot.services.FolderService;
import com.tierconnect.riot.iot.services.ReportDefinitionService;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.wordnik.swagger.annotations.*;
import com.wordnik.swagger.jaxrs.PATCH;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.subject.Subject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/folder")
@Api("/folder")
public class FolderController extends FolderControllerBase
{
    private static Logger logger = Logger.getLogger(FolderController.class);

    @Override
    public void addToResults(List<Map<String,Object>> results, String extra, String only, Long visibilityGroupId, String upVisibility, String downVisibility, boolean returnFavorite)
    {

        StringBuilder extraReportDef = new StringBuilder();
        if(StringUtils.isNotBlank(extra) && extra.contains("reportDefinitions")) {
            for (String str : extra.split(",", -1)) {
                if (str.startsWith("reportDefinitions.")) {
                    if (extraReportDef.length() > 0) {
                        extraReportDef.append(",");
                    }
                    extraReportDef.append(str.replace("reportDefinitions.", ""));
                }
            }
            StringBuilder onlyReportDef = new StringBuilder();
            if (StringUtils.isNotBlank(only)) {
                for (String str : only.split(",", -1)) {
                    if (str.startsWith("reportDefinitions")) {
                        if (onlyReportDef.length() > 0) {
                            onlyReportDef.append(",");
                        }
                        onlyReportDef.append(str.replace("reportDefinitions.", ""));
                    }
                }
            }

            ReportDefinitionController rd = new ReportDefinitionController();
            Response response = rd.listReportDefinitions(
                    -1,
                    1,
                    null,
                    "folder.id=null",
                    extraReportDef.length() > 0 ? extraReportDef.toString() : null,
                    onlyReportDef.length() > 0 ? onlyReportDef.toString() : null,
                    visibilityGroupId,
                    upVisibility,
                    downVisibility,
                    false, null, null);

            List reportDefResult = (List) ((Map)response.getEntity()).get("results");

            Map<String, Object> nullFolderResult = new HashMap<>();
            nullFolderResult.put("reportDefinitions", reportDefResult);
            nullFolderResult.put("name", null);

            results.add(0,nullFolderResult);
        }

    }

    /**
     * LIST
     */

    @GET
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
    @ApiOperation(position=1, value="Get a List of Folders (AUTO)")
    public Response listFolders( @QueryParam("pageSize") Integer pageSize, @QueryParam("pageNumber") Integer pageNumber, @QueryParam("order") String order, @QueryParam("where") String where, @Deprecated @QueryParam("extra") String extra, @Deprecated @QueryParam("only") String only, @QueryParam("visibilityGroupId") Long visibilityGroupId, @DefaultValue("") @QueryParam("upVisibility") String upVisibility, @DefaultValue("") @QueryParam("downVisibility") String downVisibility, @DefaultValue("false") @QueryParam("returnFavorite") boolean returnFavorite, @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend, @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        Pagination pagination = new Pagination( pageNumber, pageSize );

        BooleanBuilder be = new BooleanBuilder();
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Folder.class.getCanonicalName(), visibilityGroupId);
        EntityVisibility entityVisibility = getEntityVisibility();
        be = be.and( GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QFolder.folder,  visibilityGroup, upVisibility, downVisibility ) );
        // 4. Implement filtering
        be = be.and( QueryUtils.buildSearch( QFolder.folder, where ) );

        Long count = FolderService.getInstance().countList( be );
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        // 3. Implement pagination
        list = FolderService.getInstance().listFolders(be,pagination, visibilityGroupId, upVisibility,downVisibility,
                order,extra,extend,only,project);
        if (returnFavorite) {
            User user = (User) SecurityUtils.getSubject().getPrincipal();
            list = FavoriteService.getInstance().addFavoritesToList(list,user.getId(),"folder");
        }
        addToResults(list, extend, project, visibilityGroupId, upVisibility, downVisibility, returnFavorite);
        Map<String,Object> mapResponse = new HashMap<String,Object>();
        mapResponse.put( "total", count );
        mapResponse.put( "results", list );
        return RestUtils.sendOkResponse( mapResponse );
    }

    @GET
    @Path("/tree")
    @Produces(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
    @ApiOperation(position=1, value="Get a List of Folders (AUTO)")
    public Response treeFolders( @QueryParam("order") String order,
                                 @Deprecated @QueryParam("extra") String extra,
                                 @Deprecated @QueryParam("only") String only,
                                 @QueryParam("visibilityGroupId") Long visibilityGroupId,
                                 @DefaultValue("") @QueryParam("upVisibility") String upVisibility,
                                 @DefaultValue("") @QueryParam("downVisibility") String downVisibility,
                                 @DefaultValue("") @QueryParam("isFilter") String isFilter,
                                 @ApiParam(value = "Extends nested properties") @QueryParam("extend") String extend,
                                 @ApiParam(value = "Projects only nested properties") @QueryParam("project") String project )
    {
        GroupController gc = new GroupController();
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(ReportDefinition.class.getCanonicalName(), visibilityGroupId);
        RiotShiroRealm.getOverrideVisibilityCache().put(Group.class.getCanonicalName(), visibilityGroup);
        Response response = gc.listGroups(
                -1,
                1,
                "treeLevel:asc",
                "treeLevel<3",
                null,
                null,
                visibilityGroupId,
                "true",
                "true",
                null,
                null);

        List groupListResults = (List) ((Map)response.getEntity()).get("results");
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();

        for (int i = 0; i < groupListResults.size(); i++) {
            Map<String, Object> groupElement = (Map<String, Object>) groupListResults.get(i);
            Group group = GroupService.getInstance().get(Long.parseLong(groupElement.get("id").toString()));
            Pagination pagination = new Pagination( 1, -1 );
            BooleanBuilder be = new BooleanBuilder();
            String customWhere = "";

            if (group != null) {
                customWhere = "group.id=" + group.getId().toString();
                be = be.and( QueryUtils.buildSearch( QFolder.folder, customWhere ) );
            }
            RiotShiroRealm.getOverrideVisibilityCache().put(Folder.class.getCanonicalName(), visibilityGroup);
            List<Map<String, Object>> folderList = FolderService.getInstance().simpleListFolders(be, pagination, visibilityGroupId, upVisibility, downVisibility, order, "reportDefinitions,group", extend, only, project, customWhere);
            List<Map<String, Object>> validFolderList = new LinkedList<Map<String, Object>>();

            for (Map<String, Object> folder : folderList) {
                folder.put("reportsTotal",  getTotalReportDefinitions(visibilityGroup, group, Long.parseLong(folder.get("id").toString()),false, group.getTreeLevel() == 1 ? isFilter : "false"));
                folder.remove("reportDefinitions");
                if (Integer.parseInt(folder.get("reportsTotal").toString()) > 0 || folder.get("behaviour").toString().equals("own") || UserService.getInstance().getRootUser().getId().longValue() == currentUser.getId().longValue()) {
                    validFolderList.add(folder);
                }
            }

            if (groupElement.get("treeLevel").toString().equals("2")) {
                int unassignedTotal = getTotalReportDefinitions(visibilityGroup, group, null,false, "false");
                int reportsTotal = getTotalReportDefinitions(visibilityGroup, group, null, true, "false");
                groupElement.put("unassignedTotal", unassignedTotal);
                groupElement.put("reportsTotal", reportsTotal);
            }

            groupElement.put("children", validFolderList);
        }

        Map<String,Object> mapResponse = new HashMap<String,Object>();
        mapResponse.put( "results", groupListResults );
        return RestUtils.sendOkResponse( mapResponse );
    }

    public int getTotalReportDefinitions(Group visibilityGroup, Group ownerFolder, Long folderId, Boolean allElements, String isFilter)
    {

        String selectedFolder = "folder.id="+ (folderId != null ? folderId.toString() : "null");
        String whereGroup = null;

        if (ownerFolder!= null && (isFilter != null && !isFilter.equals("true"))) {
            whereGroup = "group.parentLevel" + ownerFolder.getTreeLevel() + ".id=" + ownerFolder.getId().toString();
        } else if (visibilityGroup != null && (isFilter != null && isFilter.equals("true"))) {
            whereGroup = "group.parentLevel" + visibilityGroup.getTreeLevel() + ".id=" + visibilityGroup.getId().toString();
        }

        if (allElements) {
            selectedFolder = "";
        }

        Long customVisibility = isFilter != null && isFilter.equals("true") ? visibilityGroup.getId().longValue() : ownerFolder.getId().longValue();

        ReportDefinitionController rd = new ReportDefinitionController();
        Response response = rd.listReportDefinitions(
                -1,
                1,
                null,
                (whereGroup != null ? whereGroup + "&" : "") + selectedFolder,
                null,
                null,
                customVisibility,
                null,
                "true",
                false, null, null);

        List reportDefResult = (List) ((Map)response.getEntity()).get("results");

        return reportDefResult.size();
    }



    @PUT
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value = "Create Folder",
            position = 4,
            notes = "This method permits user to create a folder" )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 201, message = "Created"),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response insertFolder(@ApiParam(value = "name and group Id are required" ) Map folderMap) {
        Map<String,Object> result = null;
        result = FolderService.getInstance().createFolder(folderMap);
        return RestUtils.sendOkResponse(result);
    }

    @PATCH
    @Path("/updateReport")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value = "Create Folder",
            position = 4,
            notes = "This method permits user to update a Report" )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 201, message = "Created"),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response updateReport(@ApiParam(value = "report update values" ) Map reportMap) {
        Map<String,Object> result = null;
        result = FolderService.getInstance().updateReport(reportMap);
        return RestUtils.sendOkResponse(result);
    }

    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    // 1a. Limit access based on CLASS level resources
    @ApiOperation(position=5, value="Delete a Report Folder")
    public Response deleteReportFolder( @PathParam("id") Long id, @ApiParam(value = "folder Id where reports will be moved" ) Map folderMap )
    {
        FolderService.getInstance().deleteReportFolder (id, folderMap);
        return RestUtils.sendDeleteResponse();
    }

    @PATCH
    @Path("/updateSequence")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(value = "Update Sequence of List of Folders",
            position = 6,
            notes = "This method permits user to update a Report" )
    @ApiResponses(
            value = {
                    @ApiResponse(code = 200, message = "Ok"),
                    @ApiResponse(code = 201, message = "Created"),
                    @ApiResponse(code = 400, message = "Bad Request"),
                    @ApiResponse(code = 403, message = "Forbidden"),
                    @ApiResponse(code = 500, message = "Internal Server Error")
            }
    )
    public Response updateOrderFolders(@ApiParam(value = "folder ids list" ) List<Long> folderList)
    {
        Map<String,Object> result = null;
        FolderService.getInstance().updateFolderListSequence(folderList);
        return RestUtils.sendOkResponse(result);
    }

    public void validateUpdate( Folder folder )
    {
        //setting ever the old sequence value
        Folder folderBase = FolderService.getInstance().get( folder.getId() );
        folder.setSequence(folderBase.getSequence());
    }

    @PATCH
    @Path("/{id}")
    @Override
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    // 1a. Limit access based on CLASS level resources
    @RequiresAuthentication
    @ApiOperation(position=4, value="Update a Folder (AUTO)")
    public Response updateFolder( @PathParam("id") Long id, Map<String, Object> map )
    {
        Folder folder = FolderService.getInstance().get( id );
        if( folder == null )
        {
            return RestUtils.sendBadResponse( String.format( "FolderId[%d] not found", id) );
        }
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        EntityVisibility entityVisibility = getEntityVisibility();
        GeneralVisibilityUtils.limitVisibilityUpdate( entityVisibility, folder, VisibilityUtils.getObjectGroup(map));
        // 7. handle insert and update
        if (map.get("name") != null){
            if (!StringUtils.equals(folder.getName(), (String) map.get("name"))) {
                folder.setName(map.get("name").toString());
                FolderService.getInstance().validateName(folder, true);
            }
        }
        folder.setLastModificationDate(new Date());
        // 6. handle validation in an Extensible manner
        validateUpdate( folder );
        FolderService.getInstance().update( folder );
        Map<String,Object> publicMap = folder.publicMap();
        return RestUtils.sendOkResponse( publicMap );
    }

    @PATCH
    @Path("/changeFolder")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @ApiOperation(position=5, value="Update assigned Folder from element")
    public Response moveElementToFolder( Map<String, Object> map )
    {
        String typeElement = map.get("typeElement").toString();
        Long elementId = Long.parseLong(map.get("elementId").toString());

        if (typeElement.equals("report")) {
            ReportDefinition report = ReportDefinitionService.getInstance().get(elementId);

            if (report == null) {
                return RestUtils.sendBadResponse( String.format( "ElementId[%d] not found", elementId) );
            }

            Folder newFolder = null;
            if (map.get("folderId") != null) {
                newFolder = FolderService.getInstance().get(Long.parseLong(map.get("folderId").toString()));

                if (newFolder == null) {
                    return RestUtils.sendBadResponse( String.format( "FolderId[%s] not found", map.get("folderId").toString()) );
                }
                if (newFolder.getGroup().getParentLevel(2) == null || (report.getGroup().getParentLevel(2).getId().longValue() != newFolder.getGroup().getParentLevel(2).getId().longValue())) {
                   return RestUtils.sendBadResponse( String.format( "ElementId[%d] does not belong to the same Company group of FolderId[%s]", elementId, map.get("folderId").toString()) );
                }
            }

            report.setFolder(newFolder);
            ReportDefinitionService.getInstance().update(report);

            Map<String,Object> publicMap = report.publicMap();
            return RestUtils.sendOkResponse( publicMap );
        }

        return RestUtils.sendBadResponse( String.format( "TypeElement[%s] not supported", typeElement) );
    }
}

