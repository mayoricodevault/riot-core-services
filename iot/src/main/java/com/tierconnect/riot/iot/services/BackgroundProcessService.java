package com.tierconnect.riot.iot.services;

import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.job.BulkProcessJob;
import com.tierconnect.riot.iot.reports.ReportAppService;
import com.tierconnect.riot.iot.utils.BackgroundProgressStatus;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import java.sql.Timestamp;
import java.util.*;

import static com.tierconnect.riot.iot.services.BackgroundProcessDetailServiceBase.getBackgroundProcessDetailDAO;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : rchirinos
 * @date : 11/17/16 2:44 PM
 * @version:
 */
public class BackgroundProcessService extends BackgroundProcessServiceBase {

    static Logger logger = Logger.getLogger(BackgroundProcessService.class );



    public Map<String, Object> getStatus( String module, Long id){
        Map<String, Object> result = new HashMap<>();
        BackgroundProcess backgroundProcess = null;
        String temporalName;
        Map<String, Object> progressMap;

        try {
            if (id != null) {
                switch (module){
                    case "reports":
                            ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
                            if (reportDefinition == null) {
                                result.put("message", String.format("ReportDefinitionId[%d] not found.", id));
                                result.put("status", "error");
                                break;
                            }

                            backgroundProcess = ReportAppService.instance().getCurrentProcess(id);//get from current Job

                            if(backgroundProcess == null){ //if it is null retrieve from database
                                backgroundProcess = BackgroundProcessService.getInstance().getMaxReportBulkProcess(id, module);
                            }

                            if (backgroundProcess != null) {
                                result = backgroundProcess.publicMap();
                                result.put("moduleId", id);
                                result.put("retry", BackgroundProcessService.getInstance().existPendingBulkProcess(reportDefinition, backgroundProcess));
                                result.put("message", "Report Definition ID " + id + " has background Process In Progress");
                                result.put("statusBar", result.get("status"));
                                result.put("status", "ok");
                                if (backgroundProcess.getProgress() != 100) {
                                    result.put("progress", ReportAppService.instance().getBulkPercentage());
                                }
                            } else {
                                result.put("message", "Report Definition ID " + id + " does not have background Processes");
                                result.put("status", "ok");
                            }

                        break;
                    case "import":
                        ImportExport importExport = ImportExportService.getInstance().get(id);
                        if (importExport == null) {
                            result.put("message",String.format("Import[%d] not found.", id));
                            result.put("status","error");
                            break;
                        }
                        temporalName = id + module;
                        progressMap = BackgroundProgressStatus.getInstance().getMapStatus(temporalName);
                        result = processResult(progressMap,result,backgroundProcess,id,module);
                        break;
                    case "export":
                        ImportExport exportImport = ImportExportService.getInstance().get(id);
                        if (exportImport == null) {
                            result.put("message",String.format("Export[%d] not found.", id));
                            result.put("status","error");
                            break;
                        }
                        temporalName = id + module;
                        progressMap = BackgroundProgressStatus.getInstance().getMapStatus(temporalName);
                        result = processResult(progressMap,result,backgroundProcess,id,module);

                        break;
                }
            } else {
                result.put("message",String.format("Module Id[%d] for [%s] should be valid.", id,module));
                result.put("status","error");
            }
        } catch (UserException e) {
            result.put("message",e.getMessage().toString());
            result.put("status","error");
        }catch (HazelcastInstanceNotActiveException e){
            result.put("message",e.getMessage().toString());
            result.put("status","error");
        }
        return result;
    }


    public Map<String, Object> processResult (Map<String, Object> progressMap, Map<String, Object> result, BackgroundProcess backgroundProcess, Long id, String module){
        String temporalName = id + module;
        if (progressMap != null) {
            backgroundProcess = (BackgroundProcess) progressMap.get("backgroundProcess");
        }else{
            backgroundProcess = BackgroundProcessService.getInstance().getMaxReportBulkProcess(id,module);
            if (backgroundProcess == null){
                progressMap = BackgroundProgressStatus.getInstance().getMapTemporalStatus(temporalName);
            }
        }
        if (backgroundProcess != null){
            result = backgroundProcess.publicMap();
            result.put("moduleId",id);
            result.put("retry", BackgroundProcessService.getInstance().existPendingBulkProcess(null,backgroundProcess));
            result.put("message", module+" ID " + id + " has background Process In Progress");
            result.put("statusBar",result.get("status"));
            result.put("status","ok");
            if (backgroundProcess.getProgress() < 100 && progressMap != null) {
                result.put("progress",progressMap.get("percent"));
                result.put("totalRecords",progressMap.get("totalRecords"));
                result.put("totalProcessed",progressMap.get("processedRecords"));
            }else{
                ImportExport importValues = ImportExportService.getInstance().get(id);
                result.put("progress", backgroundProcess.getProgress());
                result.put("entityType", importValues.getType());
            }
        } else {
            if (progressMap == null) {
                result.put("message", module+" ID " + id + " does not have background Processes");
                result.put("status", "ok");
            }else{
                result.put("progress",progressMap.get("percent"));
                result.put("moduleId",id);
                result.put("message", module+" ID " + id + " has background Process In Progress");
                result.put("statusBar",progressMap.get("status"));
                result.put("status","ok");
                result.put("totalRecords",progressMap.get("totalRecords"));
                result.put("totalProcessed",progressMap.get("processedRecords"));
                result.put("retry", false);
                result.put("checked", false);
                result.put("fileName", progressMap.get("fileName"));
                result.put("thingTypes", progressMap.get("thingTypes"));
                result.put("iniDate", progressMap.get("iniDate") != null ? progressMap.get("iniDate"):new Date());
            }
        }
        return  result;
    }


    public List<Map<String, Object>> getActiveProcesses(){
        List<Map<String, Object>> result = new ArrayList<>();
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        BooleanBuilder be = new BooleanBuilder();
        ImportExport dummyProcess;
        be = be.and(QBackgroundProcessEntity.backgroundProcessEntity.backgroundProcess.checked.eq(false));
        be = be.and(QBackgroundProcessEntity.backgroundProcessEntity.backgroundProcess.createdByUser.eq(user));
        be = be.and(QBackgroundProcessEntity.backgroundProcessEntity.moduleName.ne("reports"));
        List<Map<String, Object>> listTemporalStatus = BackgroundProgressStatus.getInstance().getAllMapStatus();
        if (listTemporalStatus != null) {
            for (Map<String, Object> temporalMap : listTemporalStatus) {
                dummyProcess = ImportExportService.getInstance().get(Long.valueOf(temporalMap.get("moduleId").toString()));
                temporalMap.remove("backgroundProcess");
                temporalMap.put("entityId", dummyProcess.getType());
                result.add(temporalMap);
            }
        }
        List<BackgroundProcessEntity> listProcesses = BackgroundProcessEntityService.getBackgroundProcessEntityDAO().selectAllBy(be);
        for (BackgroundProcessEntity backgroundProcessEntity:listProcesses) {
            Map<String,Object> process = backgroundProcessEntity.getBackgroundProcess().publicMap();
            dummyProcess = ImportExportService.getInstance().get(Long.valueOf(backgroundProcessEntity.getColumnValue()));
            process.put("moduleId",backgroundProcessEntity.getColumnValue());
            process.put("entityId", dummyProcess.getType());
            result.add(process);
        }
        return result;
    }


    public Map<String, Object> cancelBackgroundProcess( String module, Long id){
        Map<String, Object> result = new HashMap<>();

        try {
            ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
            BackgroundProcess backgroundProcess = null;
            if (reportDefinition == null) {
                result.put("status","error");
                result.put("message",String.format("ReportDefinitionId[%d] not found.", id));
                return result;
            }
            backgroundProcess = BackgroundProcessService.getInstance().getMaxReportBulkProcess(reportDefinition.getId(),module);
            if (backgroundProcess != null) {
                BackgroundProcessService.getInstance().cancelBulkProcess(backgroundProcess.getId());
            } else {
                result.put("status","error");
                result.put("message",String.format("Report Bulk Process ID should be valid."));
                return result;
            }
        } catch (UserException e) {
            result.put("status","error");
            result.put("message",e.getMessage());
            return result;
        }
        result.put("status","ok");
        result.put("message","Bulk Processes canceled for Report Definition ID: " + id + ".");

        return result;

    }

    public Map<String, Object> getAcknowledge( String module, Long id) {
        Map<String, Object> result = new HashMap<>();

        try {

            switch (module) {
                case "reports":
                        if (id != null) {
                            ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
                            if (reportDefinition == null) {
                                result.put("status", "error");
                                result.put("message", String.format("ReportDefinitionId[%d] not found.", id));
                                break;
                            }
                            BackgroundProcess backgroundProcess = BackgroundProcessService.getInstance().getMaxReportBulkProcess(reportDefinition.getId(), module);
                            if (backgroundProcess != null) {
                                backgroundProcess.setChecked(true);
                                BackgroundProcessService.getInstance().update(backgroundProcess);
                                result = backgroundProcess.publicMap();
                                result.put("statusBar",result.get("status"));
                            } else {
                                result.put("message", "Report Definition ID " + id + " does not have Background Processes");
                            }
                        } else {
                            result.put("status", "error");
                            result.put("status", String.format("ReportDefinitionId[%d] should be valid.", id));
                            break;
                        }
                    result.put("status", "ok");
                    break;
                case "import":
                        if (id != null) {
                            ImportExport importExport = ImportExportService.getInstance().get(id);
                            if (importExport == null) {
                                result.put("status", "error");
                                result.put("message", String.format("Import Id[%d] not found.", id));
                                break;
                            }
                            BackgroundProcess backgroundProcess = BackgroundProcessService.getInstance().getMaxReportBulkProcess(importExport.getId(), module);
                            if (backgroundProcess != null) {
                                backgroundProcess.setChecked(true);
                                BackgroundProcessService.getInstance().update(backgroundProcess);
                                result = backgroundProcess.publicMap();
                            } else {
                                result.put("message", "Import Definition ID " + id + " does not have Background Processes");
                            }
                        } else {
                            result.put("status", "error");
                            result.put("status", String.format("Import Id[%d] should be valid.", id));
                            break;
                        }
                    result.put("status", "ok");
                    break;
                case "export":
                        if (id != null) {
                            ImportExport  importExport = ImportExportService.getInstance().get(id);
                            if (importExport == null) {
                                result.put("status", "error");
                                result.put("message", String.format("Export Id[%d] not found.", id));
                                break;
                            }
                            BackgroundProcess backgroundProcess = BackgroundProcessService.getInstance().getMaxReportBulkProcess(importExport.getId(), module);
                            if (backgroundProcess != null) {
                                backgroundProcess.setChecked(true);
                                BackgroundProcessService.getInstance().update(backgroundProcess);
                                result = backgroundProcess.publicMap();
                            } else {
                                result.put("message", "Export Definition ID " + id + " does not have Background Processes");
                            }
                        } else {
                            result.put("status", "error");
                            result.put("status", String.format("Export Id[%d] should be valid.", id));
                            break;
                        }
                    result.put("status", "ok");
                    break;
            }
        }catch (UserException e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }
        return  result;
    }


    public BackgroundProcess insertBackgroundProcess(BackgroundProcess backgroundProcess, Long importId, String module, String name){
            insert(backgroundProcess);
            BackgroundProcessEntity backgroundProcessEntity = new BackgroundProcessEntity();
            backgroundProcessEntity.setBackgroundProcess(backgroundProcess);
            backgroundProcessEntity.setColumnValue(importId.toString());
            backgroundProcessEntity.setModuleName(module);
            backgroundProcessEntity.setColumnName(name);
            BackgroundProcessEntityService.getInstance().insert(backgroundProcessEntity);

        return backgroundProcess;
    }

    public Map<String, Object> retryBackgroundProcess( String module, Long id) {
        Map<String, Object> result = new HashMap<>();

        try {
            ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(id);
            BackgroundProcess backgroundProcess = null;
            if (reportDefinition == null) {
                result.put("status","error");
                result.put("message",String.format("ReportDefinitionId[%d] not found.", id));
                return result;
            }
            backgroundProcess = BackgroundProcessService.getInstance().getMaxReportBulkProcess(reportDefinition.getId(),module);
            if (backgroundProcess != null) {
                ReportAppService.instance().retryBulkProcess( reportDefinition,backgroundProcess);
            } else {
                result.put("status","error");
                result.put("message",String.format("Report Definition ID [%d] should be valid.", id));
                return result;
            }
        } catch (UserException e) {
            result.put("status","error");
            result.put("message",e.getMessage());
        }
        result.put("status","ok");
        result.put("message","Retry Bulk Process  has been started.");

        return result;
    }


    /**
     * Get the last Bulk process executed
     * @param id
     * @return
     */
    public BackgroundProcess getMaxReportBulkProcess(Long id, String module){
        BackgroundProcess reportBulkProcess = null;
        if (module.equals("reports")) {
            Criteria criteria = BackgroundProcessEntityService.getBackgroundProcessEntityDAO().getSession()
                    .createCriteria(BackgroundProcessEntity.class)
                    .createAlias("backgroundProcess", "backgroundProcess")
                    .add(Restrictions.eq("backgroundProcess.checked", false))
                    .add(Restrictions.eq("moduleName", module))
                    .add(Restrictions.eq("columnValue", id.toString()))
                    .setProjection(Projections.max("backgroundProcess.iniDate"));
            Timestamp maxIniDate = (Timestamp) criteria.uniqueResult();
            if (maxIniDate != null) {
                BooleanBuilder be = new BooleanBuilder();
                be = be.and(QBackgroundProcess.backgroundProcess.iniDate.eq(maxIniDate));
                List<BackgroundProcess> lstReportbulkProcess = BackgroundProcessService.getInstance().listPaginated(be, null, null);
                if ((lstReportbulkProcess != null) && (!lstReportbulkProcess.isEmpty())) {
                    reportBulkProcess = lstReportbulkProcess.get(0);
                }
            }
        }else{
            BooleanBuilder be = new BooleanBuilder();
            be = be.and(QBackgroundProcessEntity.backgroundProcessEntity.columnValue.eq(id.toString()));
            be = be.and(QBackgroundProcessEntity.backgroundProcessEntity.moduleName.eq(module));
            List<BackgroundProcessEntity> lstReportbulkProcess = BackgroundProcessEntityService.getInstance().listPaginated(be, null, null);
            if (!lstReportbulkProcess.isEmpty()) {
                reportBulkProcess = lstReportbulkProcess.get(0).getBackgroundProcess();
            }
        }
        return reportBulkProcess;
    }

    /**
     * Method to get if a Bulk Process Job is running or not
     * @param reportBulkProcess
     * @return
     */
    public boolean existPendingBulkProcess(ReportDefinition reportDefinition,BackgroundProcess reportBulkProcess){
        boolean response = true;
        if(!reportBulkProcess.getStatus().equals(Constants.COMPLETED) && reportDefinition != null){
            String nameThread = BulkProcessJob.class.getName()+"-RD-"+reportDefinition.getId();
            Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
            for (Thread t : threadSet){
                if(t.getName().contains(nameThread)){
                    response = false;
                    break;
                }
            }
        } else {
            response = false;
        }
        return response;
    }

    /**
     * Process to cancel a Bulk Process
     * @param bulkProcessId
     */
    public void cancelBulkProcess(Long bulkProcessId){
        BooleanBuilder b = new BooleanBuilder();
        b= b.and(QBackgroundProcess.backgroundProcess.id.eq(bulkProcessId));
        List<BackgroundProcess> lstReportBulkProcess = BackgroundProcessService.getInstance().listPaginated(b, null, null);
        int count = 0 ;
        if(lstReportBulkProcess != null ){
            for(BackgroundProcess reportBulkProcess : lstReportBulkProcess) {
                BooleanBuilder bb = new BooleanBuilder();
                bb.and(QBackgroundProcessDetail.backgroundProcessDetail.backgroundProcess.eq(reportBulkProcess));
                List<BackgroundProcessDetail> lstReportBulkDetail = BackgroundProcessDetailService.getInstance().listPaginated(bb, null, null);
                if((lstReportBulkDetail!=null) && (!lstReportBulkDetail.isEmpty())){
                    for(BackgroundProcessDetail reportBulkDetail : lstReportBulkDetail) {
                        reportBulkDetail.setStatus(Constants.CANCELED);
                        BackgroundProcessDetailService.getInstance().update(reportBulkDetail);
                    }
                }
                reportBulkProcess.setChecked(true);
                reportBulkProcess.setStatus(Constants.CANCELED);
                BackgroundProcessService.getInstance().update(reportBulkProcess);
                count++;
                logger.info("Bulk Process :" + reportBulkProcess.getId()+" has been canceled.");
            }
        } else {
            logger.warn("There is not any BUl Process to cancel.");
        }
    }

    /**
     * Deletes all the bulk processes associated to a report
     *
     * @param reportDefinition
     */
    public void deleteBulkByReport(ReportDefinition reportDefinition){
        //VIZIX-641, fix delete bulk registries



        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QBackgroundProcessEntity.backgroundProcessEntity.moduleName.eq("reports"));
        be = be.and(QBackgroundProcessEntity.backgroundProcessEntity.columnValue.eq(reportDefinition.getId().toString()));
        List<BackgroundProcessEntity> reportBulkProcessList = BackgroundProcessEntityService.getInstance()
                .listPaginated(be, null, null);
        deleteReportBulkProcessList(reportBulkProcessList);
    }

    /**
     * Deletes all the references for each ReportBulkProcess and itself
     * @param reportBulkProcessList
     */
    private void deleteReportBulkProcessList(List<BackgroundProcessEntity> reportBulkProcessList){
        for(BackgroundProcessEntity backgroundProcess : reportBulkProcessList){
            BooleanBuilder beEntity = new BooleanBuilder();
            beEntity = beEntity.and(QBackgroundProcessEntity.backgroundProcessEntity.backgroundProcess.id.
                    eq(backgroundProcess.getBackgroundProcess().getId())).and(QBackgroundProcessEntity.backgroundProcessEntity.moduleName.eq("reports"));
            List<BackgroundProcessEntity> backgroundProcessEntityList = BackgroundProcessEntityService.getInstance().listPaginated(beEntity,null,null);
            for ( BackgroundProcessEntity backgroundProcessEntity:backgroundProcessEntityList) {
                BackgroundProcessEntityService.getInstance().delete(backgroundProcessEntity);
            }
            BooleanBuilder be = new BooleanBuilder();
            be = be.and(QBackgroundProcessDetail.backgroundProcessDetail.backgroundProcess.id
                    .eq(backgroundProcess.getBackgroundProcess().getId()));
            List<BackgroundProcessDetail> reportBulkProcessDetailsList = BackgroundProcessDetailService.getInstance()
                    .listPaginated(be, null, null);
            for(BackgroundProcessDetail rbpd : reportBulkProcessDetailsList){
                be = new BooleanBuilder();
                be = be.and(QBackgroundProcessDetailLog.backgroundProcessDetailLog.backgroundProcessDetail.id
                        .eq(rbpd.getId()));
                List<BackgroundProcessDetailLog> reportBulkProcessDetailLogs = BackgroundProcessDetailLogService.getInstance()
                        .listPaginated(be, null, null);
                for(BackgroundProcessDetailLog rbpdl : reportBulkProcessDetailLogs){
                    BackgroundProcessDetailLogService.getInstance().delete(rbpdl);
                }
                BackgroundProcessDetailService.getInstance().delete(rbpd);
            }
            BackgroundProcessService.getInstance().delete(backgroundProcess.getBackgroundProcess());
        }
    }

    /**
     * This function just only be called in AppcoreContextListener class
     * @return pending to delete ReportBulkProcess job
     */
    public void findPendingDeleteBulkAndExecute(){
        logger.info("Looking for pending delete Bulk Process");
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try {
            transaction.begin();
            BooleanBuilder be = new BooleanBuilder();
            be = be.and(QBackgroundProcess.backgroundProcess.typeProcess.eq(Constants.DELETE_PROCESS));
            be = be.and(QBackgroundProcess.backgroundProcess.status.ne(Constants.COMPLETED));
            List<BackgroundProcess> reportBulkProcessList = BackgroundProcessService.getInstance()
                    .listPaginated(be, null, null);
            BackgroundProcess pendingDeleteBulk = (reportBulkProcessList.size() > 0)? reportBulkProcessList.get(0) : null;
            if(pendingDeleteBulk != null){
                logger.warn("Pending delete Bulk Process found, starting it");
                ReportAppService.instance().startPendingDeleteJob(pendingDeleteBulk);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            HibernateDAOUtils.rollback(transaction);
        } finally {
            transaction.commit();
        }
    }


    public  String[] getBackGroundProcessInProgressByUser(User currentUser){
        BooleanBuilder be = new BooleanBuilder();

        be = be.and(QBackgroundProcess.backgroundProcess.createdByUser.id.eq(currentUser.getId()));
        be = be.and(QBackgroundProcess.backgroundProcess.status.notIn(Constants.COMPLETED, Constants.CANCELED));

        List <BackgroundProcess> backgroundProcess = BackgroundProcessService.getInstance().listPaginated(be, null, null);

        String[] result = new String[backgroundProcess.size()];
        int i =0;
        for (BackgroundProcess pb: backgroundProcess) {
            result[i] = pb.getTypeProcess().toUpperCase() + " in progress.";
        }

        return result;
    }

    public void deleteBackgroundProcessByUser(User user){

        BooleanBuilder be = new BooleanBuilder();

        be = be.and(QBackgroundProcess.backgroundProcess.createdByUser.id.eq(user.getId()));

        List <BackgroundProcess> backgroundProcess = BackgroundProcessService.getInstance().listPaginated(be, null, null);

        for (BackgroundProcess back:backgroundProcess ) {

            BackgroundProcessEntityService.getInstance().deleteBackgroundProcessEntity(back.getId());
            List<BackgroundProcessDetail> backgroundProcessDetails = BackgroundProcessDetailService.getInstance().getBackgroundProcessDetail(back.getId());

            for (BackgroundProcessDetail processdetail: backgroundProcessDetails) {
                BackgroundProcessDetailLogService.getInstance().deleteBackgroundProcessDetailLog(processdetail.getId());
                BackgroundProcessDetailService.getInstance().deleteBackgroundProcessDetail(processdetail.getId());
            }

            getBackgroundProcessDAO().delete( back );
        }
    }


}
