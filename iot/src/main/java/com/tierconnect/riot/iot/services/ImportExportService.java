package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.iot.entities.ImportExport;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : aruiz
 * @date : 11/28/16 4:18 PM
 * @version:
 */
public class ImportExportService extends ImportExportServiceBase{
    static Logger logger = Logger.getLogger(ImportExportService.class);

    public ImportExport insert(ImportExport importExport )
    {
        if (importExport == null){
            throw  new UserException(" Import/Export information is empty.");
        }

        if (importExport.getUserId() == null){
            throw new UserException("UserId is a required parameter.");
        }

        if (importExport.getType() == null){
            throw new UserException("Type is a required parameter.");
        }

        if (importExport.getProcessType() == null){
            throw new UserException("Process type is a required parameter.");
        }
        validateInsert( importExport );
        Long id = getImportExportDAO().insert( importExport );
        importExport.setId( id );
        return importExport;
    }

    public ImportExport update( ImportExport importExport )
    {
        if (importExport == null){
            throw  new UserException(" Import/Export information is empty.");
        }

        if (importExport.getUserId() == null){
            throw new UserException("UserId is a required parameter.");
        }

        if (importExport.getType() == null){
            throw new UserException("Type is a required parameter.");
        }

        if (importExport.getProcessType() == null){
            throw new UserException("Process type is a required parameter.");
        }

        importExport.setDuration(importExport.getEndDate().getTime() - importExport.getStartDate().getTime());
        validateUpdate( importExport );
        getImportExportDAO().update( importExport );
        return importExport;
    }


    public Map<String, Object> createImportEntity(String type){

        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();
        Map<String, Object> resultsMap = null;

        ImportExport importEntity = new ImportExport();
        importEntity.setProcessType("Import");
        importEntity.setType(FileImportService.Type.valueOf(type.toUpperCase()).toString());
        importEntity.setUserId(currentUser.getId());

        importEntity = ImportExportService.getInstance().insert(importEntity);
        if (importEntity == null){
            resultsMap = new HashMap<>();
            resultsMap.put("status","error");
            resultsMap.put("message","Cannot create Import Entity");
        }else {
            resultsMap = importEntity.publicMap();
            resultsMap.put("status","ok");
        }

        return  resultsMap;
    }


    public Map<String, Object> createExportEntity(String type){

        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();
        Map<String, Object> resultsMap = null;

        ImportExport importEntity = new ImportExport();
        importEntity.setProcessType("Export");
        importEntity.setType(FileImportService.Type.valueOf(type.toUpperCase()).toString());
        importEntity.setUserId(currentUser.getId());

        importEntity = ImportExportService.getInstance().insert(importEntity);
        if (importEntity == null){
            resultsMap = new HashMap<>();
            resultsMap.put("status","error");
            resultsMap.put("message","Cannot create Export Entity");
        }else {
            resultsMap = importEntity.publicMap();
            resultsMap.put("status","ok");
        }

        return  resultsMap;
    }
}
