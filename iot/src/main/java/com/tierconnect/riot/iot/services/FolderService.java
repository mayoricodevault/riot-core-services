package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.iot.controllers.FolderController;
import com.tierconnect.riot.iot.controllers.ReportDefinitionController;
import com.tierconnect.riot.iot.entities.Folder;
import com.tierconnect.riot.iot.entities.QFolder;
import com.tierconnect.riot.iot.entities.QReportDefinition;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import javax.ws.rs.core.Response;
import java.util.*;

public class FolderService extends FolderServiceBase
{
    public Map createFolder (Map folderMap) {
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();

        Map result = new HashMap<>();
        String name = folderMap.get("name") != null ? folderMap.get("name").toString() : null;

        if ( name == null ) {
            throw new UserException("Invalid input data: name");
        }
        Long idFolder =folderMap.get("folderId") != null? Long.valueOf(folderMap.get("folderId").toString()): null;
        Long idGroup =folderMap.get("groupId") != null? Long.valueOf(folderMap.get("groupId").toString()): null;
        String typeElement = folderMap.get("typeElement") != null ? folderMap.get("typeElement").toString():null;
        Folder folderChild = null;
        if (typeElement == null){
            throw new UserException("Invalid input data: type Element");
        }
        if (idFolder != null) {
            folderChild = FolderService.getInstance().get(idFolder);
        }
        if ( idGroup == null ) {
            throw new UserException("Invalid input data: group Id");
        }
        Group groupChild = GroupService.getInstance().get(idGroup);

        if(typeElement.equals("report") && !PermissionsUtils.isPermitted( SecurityUtils.getSubject(), "reportFolder:i")){
            throw new ForbiddenException("Not Authorized, Access Denied");
        }
        Folder folder = new Folder();
        folder.setCreationDate(new Date());
        folder.setName(name);
        folder.setFolderId(folderChild);
        folder.setGroup(groupChild);
        validateName(folder, false);
        folder.setCreatedByUser(currentUser);
        //getting the last sequence folder value
        BooleanBuilder be = new BooleanBuilder();
        Long sequenceValue = 0L;
        be = be.and(QFolder.folder.group.eq(groupChild));
        List<Folder> listFolder= FolderService.getInstance().listPaginated(be, null, "sequence:asc");
        if (listFolder.size() - 1 >= 0) {
            sequenceValue = listFolder.get(listFolder.size() - 1).getSequence() + 1L;
        }
        folder.setSequence(sequenceValue);
        folder.setTypeElement(typeElement);

        FolderService.getInstance().insert(folder);
        if (name.length()> 250){
            name = name.substring(0,250);
        }
        String code = name.concat(folder.getId().toString());
        folder.setCode(code);
        FolderService.getInstance().update(folder);
        result = folder.publicMap();
        return result;
    }


    public Map updateReport (Map updateReport){
        if( !PermissionsUtils.isPermitted( SecurityUtils.getSubject(), "reportFolder:u")){
            throw new ForbiddenException("Not Authorized, Access Denied");
        }
        Map result = new HashMap<>();
        Long folderId = updateReport.get("folderId") != null ? Long.valueOf(updateReport.get("folderId").toString()): null;
        Long reportId = updateReport.get("reportId") != null ? Long.valueOf(updateReport.get("reportId").toString()): null;

        if (reportId == null){
            throw  new UserException("Invalid input data: report Id");
        }

        ReportDefinition report = ReportDefinitionService.getInstance().get(reportId);
            if (report == null ){
                throw  new UserException(String.format( "Report Id[%d] not found", reportId));
            }
        Folder folder = null;
        if (folderId != null) {
            folder = FolderService.getInstance().get(folderId);
        }
        report.setFolder(folder);
        ReportDefinitionService.getInstance().update(report);
        result = report.publicMap();
        return result;
    }

    public void deleteReportFolder (Long id, Map folderMap){
        if( !PermissionsUtils.isPermitted( SecurityUtils.getSubject(), "reportFolder:d")){
            throw new ForbiddenException("Not Authorized, Access Denied");
        }
        Folder folder = FolderService.getInstance().get(id);
        if(folder == null) {
            throw new UserException("FolderId " + id + " not found");
        }
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QReportDefinition.reportDefinition.folder.id.eq(id));

        List<ReportDefinition> reports = ReportDefinitionService.getInstance().listPaginated(be, null,null);

        Long folderId = folderMap.get("folderId") != null? Long.valueOf(folderMap.get("folderId").toString()):null;
        Folder currentFolder = null;
        if (folderId != null) {
            currentFolder = FolderService.getInstance().get(folderId);
        }
        for (ReportDefinition reportDefinition: reports){
            reportDefinition.setFolder(currentFolder);
            ReportDefinitionService.getInstance().update(reportDefinition);
        }
        FolderService.getInstance().delete( folder );

    }

    public void updateFolderListSequence(List<Long> folderList) {
        List<Folder> validFolderList = new ArrayList<>();
        Group validGroup = null;
        //validations
        if (folderList != null && folderList.size() > 0) {
            Folder folder;
            for (int i = 0; i < folderList.size(); i++) {
                folder = FolderService.getInstance().get(folderList.get(i));
                if (folder == null) {
                    throw new UserException("FolderId " + folderList.get(i) + " not found");
                }
                if (validGroup == null) {
                    validGroup = folder.getGroup();
                }
                if (validGroup.getId().longValue() != folder.getGroup().getId().longValue()) {
                    throw new UserException("GroupId of FolderId [" + folderList.get(i) + "] is not valid for this list");
                }
                validFolderList.add(folder);
            }

            if (validFolderList.size() > 0) {
                for (int i = 0; i < validFolderList.size(); i++) {
                    folder = validFolderList.get(i);
                    folder.setSequence((long) i);
                    FolderService.getInstance().update(folder);
                }
            }
        }
    }

    /**
     * Method to get a folder with Name and Group
     * @param code, Code of the folder
     * @param group , Group of the folder
     * @return Folder Bean
     */
    public Folder getByCodeAndGroup(String code, Group group) {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QFolder.folder.code.eq(code));
        if (group != null) {
            be = be.and(QFolder.folder.group.eq(group));
        }
        return getFolderDAO().selectBy(be);
    }

    public Folder insert( Folder folder )
    {
        validateName( folder, false );
        return super.insert(folder);
    }

    public void validateName( Folder folder, boolean isUpdate )
    {
        BooleanBuilder beAnd = new BooleanBuilder();
        beAnd = beAnd.and(QFolder.folder.name.eq(folder.getName()));
        beAnd = beAnd.and(QFolder.folder.group.eq(folder.getGroup()));
        List<Folder> checkNames = FolderService.getInstance().listPaginated(beAnd,null,null);
        if (!checkNames.isEmpty() && (!isUpdate || checkNames.size() > 1)){
            throw new UserException("Folder name "+ folder.getName() +" already exists for Tenant Group "+folder.getGroup().getName());
        }
    }

    public List<Map<String, Object>> listFolders(BooleanBuilder be, Pagination pagination, Long visibilityGroupId, String upVisibility,
                            String downVisibility, String order, String extra, String extend,
                            String only, String project){
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();
        ReportDefinitionController rd = new ReportDefinitionController();
        for( Folder folder : FolderService.getInstance().listPaginated( be, pagination, order ) )
        {
            // 5a. Implement extra
            Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( folder, extra, new ArrayList<String>());
            publicMap = QueryUtils.mapWithExtraFieldsNested(folder, publicMap, extend, new ArrayList<String>());

            Response response = rd.listReportDefinitions(
                    -1,
                    1,
                    null,
                    "folder.id="+folder.getId(),
                    null,
                    null,
                    visibilityGroupId != null ? visibilityGroupId : currentUser.getActiveGroup().getId(),
                    upVisibility,
                    downVisibility,
                    false, null, null);

            List reportDefResult = (List) ((Map)response.getEntity()).get("results");
            if ( ( currentUser.getActiveGroup() != null &&
                    currentUser.getActiveGroup().getId().equals(GroupService.getInstance().getRootGroup().getId()) ) &&
                    (folder.getCreatedByUser() != null && !folder.getCreatedByUser().equals(currentUser) ) ){
                publicMap.put("reportDefinitions",reportDefResult);
                publicMap.put("behaviour","readOnly");
                // 5b. Implement only
                QueryUtils.filterOnly( publicMap, only, extra );
                QueryUtils.filterProjectionNested( publicMap, project, extend );
                list.add( publicMap );
            }else if( (folder.getCreatedByUser() != null) &&
                    (folder.getCreatedByUser().equals(currentUser) ||
                      (!folder.getCreatedByUser().equals(currentUser) && !reportDefResult.isEmpty()) )){
                publicMap.put("reportDefinitions",reportDefResult);
                QueryUtils.filterOnly( publicMap, only, extra );
                QueryUtils.filterProjectionNested( publicMap, project, extend );
                list.add( publicMap );
            }
        }
        return list;
    }

    public List<Map<String, Object>> simpleListFolders(BooleanBuilder be, Pagination pagination, Long visibilityGroupId, String upVisibility,
                                                 String downVisibility, String order, String extra, String extend,
                                                 String only, String project, String where) {
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();

        for( Folder folder : FolderService.getInstance().listPaginated( be, pagination, order ) )
        {
            // 5a. Implement extra
            Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( folder, extra, new ArrayList<String>());
            publicMap = QueryUtils.mapWithExtraFieldsNested(folder, publicMap, extend, new ArrayList<String>());
            String behaviorValue = "";
            if ( folder.getCreatedByUser() != null && !folder.getCreatedByUser().equals(currentUser) ) {
                behaviorValue = "readOnly";
            }else if( folder.getCreatedByUser() != null && folder.getCreatedByUser().equals(currentUser)) {
                behaviorValue = "own";
            }

            if (!behaviorValue.isEmpty()) {
                publicMap.put("behaviour", behaviorValue);
                // 5b. Implement only
                QueryUtils.filterOnly( publicMap, only, extra );
                QueryUtils.filterProjectionNested( publicMap, project, extend );
                list.add( publicMap );
            }
        }
        return list;
    }

    /**
     * Method to check if a user has folders
     * @param user
     * @return result
     */
    public List<Folder> getUserFolders(User user){

        BooleanBuilder beAnd = new BooleanBuilder();
        beAnd = beAnd.and(QFolder.folder.createdByUser.eq(user));

        List<Folder> foldersByUser = FolderService.getInstance().listPaginated(beAnd,null,null);

        return foldersByUser;
    }

    public String[] getUserFolderNames(User user){

        List<Folder> userFolders= FolderService.getInstance().getUserFolders(user);

        String[] names = new String[ userFolders.size()];
        int i = 0;
        for (Folder f:userFolders ) {
            names[i] = f.getName();i++;
        }
        return names;
    }

}

