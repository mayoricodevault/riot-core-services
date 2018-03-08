package com.tierconnect.riot.iot.reports;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.api.database.base.FactoryDataBase;
import com.tierconnect.riot.api.database.mongo.Mongo;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.utils.DateHelper;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.job.BulkProcessJob;
import com.tierconnect.riot.iot.reports_integration.TableDetailReportConfig;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;
import org.bson.BsonSerializationException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * Created by pablo on 6/16/15.
 * <p>
 * Application Service that handles operations of report execution
 */
public class ReportAppService {
    static Logger logger = Logger.getLogger(ReportAppService.class);
    BulkProcessJob bulkProcessJob;

    // what type of report to run
    // public enum ReportType {
    // MAP, TABLE, SUMMARY, ZONE_MAP, TABLE_HISTORY, MAP_HISTORY
    // }

    private static ReportAppService reportAppService;

    private ReportAppService() {
    }

    public static ReportAppService instance() {
        if (reportAppService == null) {
            reportAppService = new ReportAppService();
        }
        return reportAppService;
    }

    /**
     * Process to do bulk update, only execute values according to the filter
     * Thing Type Filter is != null -> only execute changes of this Thing Type
     * Thing Type Filter is = null -> execute changes of Thing Types registered in "values To Change"
     *
     * @param backgroundProcess
     * @param values
     * @param pageSize
     * @param pageNumber
     * @param startDate
     * @param endDate
     * @param now
     * @return
     * @throws UserException
     */
    public Map<String, Object> bulkUpdate(
            ReportDefinition reportDefinition,
            BackgroundProcess backgroundProcess,
            Map<String, Object> values,
            Integer pageSize,
            Integer pageNumber,
            Date startDate,
            Date endDate,
            Date now) throws UserException {
        Map<String, Object> result = new HashMap<>();
        ValidationBean validationBean = validateBulkProcess(values);
        if ((validationBean != null) && (validationBean.isError())) {
            throw new UserException(validationBean.getErrorDescription());
        }
        try {
            //Insert report Bulk process Details, that means by UDF
            LinkedHashMap udfs = (LinkedHashMap) values.get("udfs");
            Map<String, Object> lstUdfs = getLstUdfs(udfs);
            Map<String, Object> originalFilters = (Map<String, Object>) values.get("filters");
            for (String key : lstUdfs.keySet()) {
                String thingTypeFilter = null;
                if ((originalFilters.get("Thing Type") != null) && (!originalFilters.get("Thing Type").toString().trim().equals(""))) {
                    thingTypeFilter = originalFilters.get("Thing Type").toString().trim();
                }
                Map<String, Object> filters = new HashMap<>();
                filters.putAll(originalFilters);
                if (!key.equals("0")) {//This
                    if (thingTypeFilter != null) {
                        if (thingTypeFilter.equals(key)) {
                            //Execute WITHOUT modifying filters with key
                            registerBackgroundProcess(reportDefinition, backgroundProcess,
                                    filters, pageSize, pageNumber, startDate, endDate, now, lstUdfs, key);
                        }
                        //For else, it should not be registered in bulk process
                    } else {
                        //Execute  MODIFYING filters
                        filters.put("Thing Type", key);
                        getNewReportFilter(reportDefinition, key);
                        registerBackgroundProcess(reportDefinition, backgroundProcess,
                                filters, pageSize, pageNumber, startDate, endDate, now, lstUdfs, key);
                        getDeleteReportFilter(reportDefinition);
                    }
                } else {
                    //Execute WITHOUT modifying filters
                    registerBackgroundProcess(reportDefinition, backgroundProcess,
                            filters, pageSize, pageNumber, startDate, endDate, now, lstUdfs, key);
                }
            }

            result.put("message", "Massive update process in progress.");
            result.put("filters", udfs);
        } catch (NumberFormatException nfe) {
            logger.error("Report definition [" + reportDefinition.getName() + "-" +
                    reportDefinition.getId() + "]: There are problems with some values. "
                    + nfe.getMessage(), nfe);
            throw new UserException("There are problems with some values. " + nfe.getMessage(), nfe);
        } catch (ClassCastException cce) {
            logger.error("Report definition [" + reportDefinition.getName() + "-" +
                    reportDefinition.getId() + "]: There are problems with some filters.", cce);
            throw new UserException("There are problems with some filters.", cce);
        } catch (BsonSerializationException bse) {
            logger.error("Report definition [" + reportDefinition.getName() + "-" +
                    reportDefinition.getId() + "]: Please reduce report date filter or add additional filters and try again. " +
                    bse.getMessage(), bse);
            throw new UserException("Please reduce report date filter or add additional filters and try again.", bse);
        } catch (UserException ue) {
            logger.error("Report definition [" + reportDefinition.getName() + "-" +
                    reportDefinition.getId() + "]: " + ue.getMessage(), ue);
            throw ue;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new UserException("There was a problem doing the Bulk Update.", e);
        }
        return result;
    }

    /**
     * Massive bulk process, execute two operations: UPDATE and DELETE
     *
     * @param reportDefinition Object reportDefinition
     * @param typeProcess      Type process , it should be UPDATE or DELETE
     * @param dateHelper       Object Date Helper which has
     * @param now
     * @param currentUser
     * @param pageNumber
     * @param pageSize
     * @param body
     * @param totalRecords
     * @return
     */
    public Map<String, Object> massiveBulkProcess(
            ReportDefinition reportDefinition,
            String typeProcess,
            DateHelper dateHelper,
            Date now,
            User currentUser,
            Integer pageNumber,
            Integer pageSize,
            Map<String, Object> body,
            Long totalRecords) {
        Map<String, Object> result = new HashMap<>();
        ValidationBean validationBean = validateMassiveProcess(reportDefinition, typeProcess);
        if ((validationBean != null) && (validationBean.isError())) {
            throw new UserException(validationBean.getErrorDescription());
        }
        //Insert report Bulk Process
        BackgroundProcess backgroundProcess = new BackgroundProcess();
        backgroundProcess.setIniDate(now);
        backgroundProcess.setCreatedByUser(currentUser);
        backgroundProcess.setStatus(Constants.ADDED);
        backgroundProcess.setTypeProcess(typeProcess);
        backgroundProcess.setTotalRecords(totalRecords);
        backgroundProcess.setProgress(0);
        String nameThread = BulkProcessJob.class.getName() + "-RD-" + reportDefinition.getId();
        backgroundProcess.setThreadName(nameThread);
        backgroundProcess = BackgroundProcessService.getInstance().insert(backgroundProcess);
        BackgroundProcessEntity backgroundProcessEntity = new BackgroundProcessEntity();
        backgroundProcessEntity.setBackgroundProcess(backgroundProcess);
        backgroundProcessEntity.setColumnName("reportDefinitionId");
        backgroundProcessEntity.setModuleName("reports");
        backgroundProcessEntity.setColumnValue(reportDefinition.getId().toString());
        BackgroundProcessEntityService.getInstance().insert(backgroundProcessEntity);
        if (typeProcess.equals(Constants.UPDATE_PROCESS)) {
            result = ReportAppService.instance().bulkUpdate(
                    reportDefinition,
                    backgroundProcess,
                    body,
                    pageSize,
                    pageNumber,
                    dateHelper.from(),
                    dateHelper.to(),
                    now);
        } else if (typeProcess.equals(Constants.DELETE_PROCESS)) {
            result = ReportAppService.instance().bulkDelete(
                    reportDefinition,
                    backgroundProcess,
                    body,
                    pageSize,
                    pageNumber,
                    dateHelper.from(),
                    dateHelper.to(),
                    now);
        }
        commit();//save data and execute bulk
        return result;
    }

    /**
     * Validation Massive Bulk Process
     *
     * @param reportDefinition ID report Definition
     * @param typeProcess      Type process should be UPDATE | DELETE
     * @return
     */
    public ValidationBean validateMassiveProcess(ReportDefinition reportDefinition, String typeProcess) {
        List<String> messages = new ArrayList<>();
        ValidationBean validationBean = new ValidationBean();
        boolean massiveProcess = ReportAppService.instance().isMassiveProcessRunning();
        if (massiveProcess) {
            messages.add("The system already has a bulk process running, please try later");
        }
        if (typeProcess == null) {
            messages.add("Type Process should be UPDATE or DELETE");
        } else {
            if (!(typeProcess.equals(Constants.UPDATE_PROCESS) || typeProcess.equals(Constants.DELETE_PROCESS))) {
                messages.add("Type Process should be UPDATE or DELETE");
                validationBean.setErrorDescription("Type Process should be UPDATE or DELETE");
            }
        }
        if (Utilities.isNotEmpty(messages)) {
            validationBean.setErrorDescription(String.join(",", messages));
        }
        return validationBean;
    }


    /**
     * Register Report BulkProcess
     *
     * @param backgroundProcess
     * @param filters
     * @param pageSize
     * @param pageNumber
     * @param startDate
     * @param endDate
     * @param now
     * @param lstUdfs
     * @param key
     */
    public void registerBackgroundProcess(
            ReportDefinition reportDefinition,
            BackgroundProcess backgroundProcess,
            Map<String, Object> filters,
            Integer pageSize,
            Integer pageNumber,
            Date startDate,
            Date endDate,
            Date now,
            Map<String, Object> lstUdfs,
            String key) {
        try {
            //Get configuration of Report Definition
            TableDetailReportConfig tableReportConfig = new TableDetailReportConfig(reportDefinition, filters,
                    pageNumber, pageSize, startDate, endDate, now, true);
            Mongo mongo = FactoryDataBase.get(Mongo.class, tableReportConfig.getFilters());
            String query = mongo.getConditionBuilderString();
            logger.info(query);
            JSONObject json = new JSONObject((Map) lstUdfs.get(key));
            ThingType thingType = key.equals("0") ? null : ThingTypeService.getInstance().get(Long.parseLong(key));
            BackgroundProcessDetail backgroundProcessDetail = new BackgroundProcessDetail();
            backgroundProcessDetail.setBackgroundProcess(backgroundProcess);
            backgroundProcessDetail.setThingType(thingType);
            backgroundProcessDetail.setQuery(query);
            backgroundProcessDetail.setValuesToChange(json.toJSONString());
            backgroundProcessDetail.setIniDate(now);
            backgroundProcessDetail.setStatus(Constants.ADDED);
            BackgroundProcessDetailService.getInstance().insert(backgroundProcessDetail);

            //Throw job
            throwBulkProcessJob(reportDefinition.getId(),
                    backgroundProcess.getTypeProcess(), backgroundProcess.getThreadName());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new UserException("Error in register background process", e);
        }
    }

    private void throwBulkProcessJob(Long reportDefinitionId, String typeOfProcess, String threadName) {
        bulkProcessJob = new BulkProcessJob(
                reportDefinitionId, typeOfProcess);
        Thread threadBulkProcess = new Thread(bulkProcessJob, threadName);
        threadBulkProcess.start();
    }

    /**
     * Process to do bulk delete
     *
     * @param values
     * @param pageSize
     * @param pageNumber
     * @param startDate
     * @param endDate
     * @param now
     * @return
     * @throws UserException
     */
    public Map<String, Object> bulkDelete(
            ReportDefinition reportDefinition,
            BackgroundProcess backgroundProcess,
            Map<String, Object> values,
            Integer pageSize,
            Integer pageNumber,
            Date startDate,
            Date endDate,
            Date now) throws UserException {
        Map<String, Object> result = new HashMap<>();
        ValidationBean validationBean = validateBulkProcess(values);
        if ((validationBean != null) && (validationBean.isError())) {
            throw new UserException(validationBean.getErrorDescription());
        }
        try {
            LinkedHashMap udfs = (LinkedHashMap) values.get("udfs");
            Map<String, Object> filters = (Map<String, Object>) values.get("filters");

            //Get configuration of Report Definition
            TableDetailReportConfig tableReportConfig = new TableDetailReportConfig(reportDefinition,
                    filters,pageNumber,pageSize,startDate,endDate,now, true);
            Mongo mongo = FactoryDataBase.get(Mongo.class, tableReportConfig.getFilters());
            String query = mongo.getConditionBuilderString();
            logger.info(query);
            BackgroundProcessDetail backgroundProcessDetail = new BackgroundProcessDetail();
            backgroundProcessDetail.setBackgroundProcess(backgroundProcess);
            backgroundProcessDetail.setIniDate(now);
            backgroundProcessDetail.setQuery(query);
            backgroundProcessDetail.setValuesToChange(null);
            backgroundProcessDetail.setThingType(null);
            backgroundProcessDetail.setStatus(Constants.ADDED);
            BackgroundProcessDetailService.getInstance().insert(backgroundProcessDetail);

            //Create job
            throwBulkProcessJob(reportDefinition.getId(),
                    backgroundProcess.getTypeProcess(), backgroundProcess.getThreadName());
            result.put("message", "Massive delete process in progress.");
            result.put("filters", udfs);
        } catch (NumberFormatException nfe) {
            logger.error("Report definition [" + reportDefinition.getName() + "-" +
                    reportDefinition.getId() + "]: There are problems with some values. "
                    + nfe.getMessage(), nfe);
            throw new UserException("There are problems with some values. " + nfe.getMessage(), nfe);
        } catch (ClassCastException cce) {
            logger.error("Report definition [" + reportDefinition.getName() + "-" +
                    reportDefinition.getId() + "]: There are problems with some filters.", cce);
            throw new UserException("There are problems with some filters.", cce);
        } catch (BsonSerializationException bse) {
            logger.error("Report definition [" + reportDefinition.getName() + "-" +
                    reportDefinition.getId() + "]: Please reduce report date filter or add additional filters and try again. " +
                    bse.getMessage(), bse);
            throw new UserException("Please reduce report date filter or add additional filters and try again.", bse);
        } catch (UserException ue) {
            logger.error("Report definition [" + reportDefinition.getName() + "-" +
                    reportDefinition.getId() + "]: " + ue.getMessage(), ue);
            throw ue;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new UserException("There was a problem doing the Bulk Delete.", e);
        }
        return result;
    }

    /**
     * Get list of UDFs for Bulk Process
     *
     * @param dataFirst
     * @return
     */
    public Map<String, Object> getLstUdfs(Map<String, Object> dataFirst) {
        Map<String, Object> fields = new HashMap<>();
        for (String firstKey : dataFirst.keySet()) {
            Map<String, Object> udf = new HashMap<>();
            Map<String, Object> data = (Map<String, Object>) dataFirst.get(firstKey);
            for (String key : data.keySet()) {
                Map<String, Object> thingTypeFieldValue = (Map<String, Object>) data.get(key);
                Object obj = thingTypeFieldValue.get("thingTypeId");
                if ((obj != null)) {
                    udf.put(key, data.get(key));
                    if (fields.containsKey(obj.toString())) {
                        Map<String, Object> subData = (Map<String, Object>) fields.get(obj.toString());
                        subData.putAll(udf);
                        fields.put(obj.toString(), subData);
                    } else {
                        fields.put(obj.toString(), udf);
                    }
                }
            }
        }
        return fields;
    }

    /**
     * Validations for massive process in reports
     *
     * @param values
     * @return
     */
    public ValidationBean validateBulkProcess(Map<String, Object> values) {
        ValidationBean valida = new ValidationBean();
        //Check values to change
        if (values.get("udfs") == null) {
            valida.setErrorDescription("User should send values to change.");
        } else
            //Check get filters
            if (values.get("filters") == null) {
                valida.setErrorDescription("User should send filter.");
            }
        return valida;
    }

    /**
     * Check if exists a massive process already running
     *
     * @return
     */
    public boolean isMassiveProcessRunning() {
        boolean response = isProcessRunning(); //check if there is a Thread running
        if (!response) {// if no process is running then make a double check with database, for data consistency purposes
            BooleanBuilder be = new BooleanBuilder();
            BooleanBuilder beStatus = new BooleanBuilder();
            beStatus = beStatus.and(QBackgroundProcess.backgroundProcess.typeProcess.ne("import"));
            beStatus = beStatus.and(QBackgroundProcess.backgroundProcess.typeProcess.ne("export"));
            beStatus = beStatus.and(QBackgroundProcess.backgroundProcess.status.eq(Constants.ADDED));
            beStatus = beStatus.or(QBackgroundProcess.backgroundProcess.status.eq(Constants.IN_PROGRESS));
            be = be.and(beStatus);
            List<BackgroundProcess> reportBulkProcesses = BackgroundProcessService.getInstance().listPaginated(be, null, null);
            if ((reportBulkProcesses != null) && (!reportBulkProcesses.isEmpty())) {
                response = true;
            }
        }
        return response;
    }

    /**
     * Get List of report filters
     *
     * @param reportDefinition
     */
    public void getNewReportFilter(ReportDefinition reportDefinition, String key) {
        ReportFilter newReportFilter = new ReportFilter();
        newReportFilter.setValue(key);
        newReportFilter.setPropertyName("thingType.id");
        newReportFilter.setOperator("=");
        newReportFilter.setLabel("Thing Type");
        newReportFilter.setReportDefinition(reportDefinition);
        if ((reportDefinition != null) &&
                ((reportDefinition.getReportFilter() != null) && !reportDefinition.getReportFilter().isEmpty())) {
            int count = 0;
            for (ReportFilter reportFilter : reportDefinition.getReportFilter()) {
                if (reportFilter.getPropertyName().equals("thingTypeId")) {
                    reportFilter.setValue(key);
                    count++;
                    break;
                }
            }
            if (count == 0) {
                reportDefinition.getReportFilter().add(newReportFilter);
            }
        } else {
            List<ReportFilter> reportFilters = new ArrayList<>();
            reportFilters.add(newReportFilter);
            reportDefinition.setReportFilter(reportFilters);
        }
    }

    /**
     * Delete ThingtypeId report filter
     *
     * @param reportDefinition
     */
    public void getDeleteReportFilter(ReportDefinition reportDefinition) {
        if ((reportDefinition != null) &&
                ((reportDefinition.getReportFilter() != null) && !reportDefinition.getReportFilter().isEmpty())) {
            ReportFilter reportFilterDel = null;
            for (ReportFilter reportFilter : reportDefinition.getReportFilter()) {
                if (reportFilter.getPropertyName().equals("thingType.id")) {
                    reportFilterDel = reportFilter;
                    break;
                }
            }
            if (reportFilterDel != null) {
                reportDefinition.getReportFilter().remove(reportFilterDel);
                ReportFilterService.getInstance().delete(reportFilterDel);
            }
        }
    }

    public String getBulkPercentage() {
        if (bulkProcessJob != null) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
            DecimalFormat df = new DecimalFormat("0.0", symbols);
            return df.format(bulkProcessJob.getPercentageStatus());
        } else {
            return "0.0";
        }
    }

    public BackgroundProcess getCurrentProcess(Long reportDefinitionId) {
        if (bulkProcessJob != null) {
            return bulkProcessJob.getCurrentMainProcess(reportDefinitionId);
        } else {
            return null;
        }
    }

    private boolean isProcessRunning() {
        if (bulkProcessJob != null) {
            return bulkProcessJob.isBulkProcessRunning();
        } else {
            return false;
        }
    }

    /**
     * Retry the execution of a bulk process ID
     *
     * @param backgroundProcess
     */
    public void retryBulkProcess(ReportDefinition reportDefinition, BackgroundProcess backgroundProcess) {
        if (backgroundProcess != null) {
            String nameThread = BulkProcessJob.class.getName() + "-RD-" + reportDefinition.getId();
            backgroundProcess.setThreadName(nameThread);
            BackgroundProcessService.getInstance().update(backgroundProcess);
            throwBulkProcessJob(reportDefinition.getId(),
                    backgroundProcess.getTypeProcess(), backgroundProcess.getThreadName());
            logger.debug("Report Bulk Process ID:" + backgroundProcess.getId() + " has been started.");
        } else {
            logger.warn("Report Bulk Process does not exists.");
        }
    }

    /**
     * Logg information of the request for Reports
     *
     * @param reportDefinition
     * @param requestInfo
     */
    public void logStart(ReportDefinition reportDefinition, String requestInfo, String module) {
        logger.info(module + ">" + reportDefinition.getLogMessage() + " -REQUEST_INFO: " + requestInfo);
    }

    /**
     * Log at the end of the Report Process
     *
     * @param reportDefinition
     * @param start
     */
    public void logEnd(ReportDefinition reportDefinition, long start, String module) {
        logger.info(module + " " + reportDefinition.getLogMessage() + " -PROCESSING TIME:  " + (System
                .currentTimeMillis() - start) + " ms");
    }

    /**
     * This method just only be called by {@link BackgroundProcessService}
     *
     * @param pendingDeleteBulk
     */
    public void startPendingDeleteJob(final BackgroundProcess pendingDeleteBulk) {
        BackgroundProcessEntity backgroundProcessEntity = BackgroundProcessEntityService.getInstance().getModuleInformation(pendingDeleteBulk, Constants.BULK_PROCESS_REPORT_MODULE_NAME);
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(Long.valueOf(backgroundProcessEntity.getColumnValue()));
        throwBulkProcessJob(reportDefinition.getId(),
                pendingDeleteBulk.getTypeProcess(), pendingDeleteBulk.getThreadName());
    }

    private void commit() {
        logger.debug("Commit in ReportAppService");
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try {
            if (transaction.isActive()) {
                transaction.commit();
            }
        } catch (Exception e) {
            logger.error("Error in commit manually", e);
            HibernateDAOUtils.rollback(transaction);
        }
    }
}
