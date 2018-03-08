package com.tierconnect.riot.iot.job;

import com.mongodb.BasicDBObject;
import com.mongodb.Bytes;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.Tuple;
import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.iot.utils.Cache;
import com.tierconnect.riot.sdk.dao.HibernateDAOUtils;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.MongoExecutionException;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.servlet.security.ApiKeyToken;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * This Bulk process Job executes a UPDATE or DELETE bulk process
 * In addition, with correct configurations in Groups for Bulk process it will execute Tickles and rules
 *
 * @author : rchirinos
 * @date : 10/4/16 10:04 AM
 * @version: This Job starts to execute
 */

public class BulkProcessJob implements Runnable {

    static Logger logger = Logger.getLogger(BulkProcessJob.class);
    private static final ExecutorService TICKLE_EXECUTOR = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 10);
    private Long reportDefinitionId = null;
    private String typeProcess = null;
    private static final int SUBPROCESS_CHUNK = 5;//percentage
    private float percentageStatus;
    private int subProcessSequence = 0;
    private Session session;
    private Transaction transaction;
    private Cache cache;
    private static int CHUNK_SIZE = 10000;
    private BackgroundProcess currentMainProcess;
    private Subject subject;
    private User rootUser;
    private User userLogged;

    public BulkProcessJob(Long reportDefinitionId, String typeProcess) {
        this.reportDefinitionId = reportDefinitionId;
        this.typeProcess = typeProcess;
        this.percentageStatus = 0.0f;
        Integer batchSizeBulkProcessTmp = ConfigurationService.getAsInteger(
                (User) SecurityUtils.getSubject().getPrincipal(),
                "batchSize_bulkProcess");
        CHUNK_SIZE = (batchSizeBulkProcessTmp != null) ? batchSizeBulkProcessTmp : CHUNK_SIZE;
        rootUser = UserService.getInstance().getRootUser();
    }

    /**
     * Execute process of Bulk Update
     */
    public void run() {
        logger.info("Start Bulk Process Job");
        session = HibernateSessionFactory.getInstance().getCurrentSession();
        transaction = session.getTransaction();
        try {
            transaction.begin();
            Long reloadAllThingsThreshold = ConfigurationService.getAsLong(UserService.getInstance().getRootUser(),
                    "reloadAllThingsThreshold_bulkProcess");
            Boolean sendThingFieldTickle = ConfigurationService.getAsBoolean(rootUser, "sendThingFieldTickle_bulkProcess");
            initSessionIfIsNull();
            cache = new Cache();

            Set<Long> processIds = new HashSet<>();
            Set<Long> subProcessIds = new HashSet<>();
            BooleanBuilder be = new BooleanBuilder();
            be = be.and(QBackgroundProcessEntity.backgroundProcessEntity.moduleName.eq("reports"));
            be = be.and(QBackgroundProcessEntity.backgroundProcessEntity.columnValue.eq(reportDefinitionId.toString()));
            be = be.and(QBackgroundProcessEntity.backgroundProcessEntity.backgroundProcess.status.eq(Constants.ADDED));
            be = be.and(QBackgroundProcessEntity.backgroundProcessEntity.backgroundProcess.typeProcess.eq(typeProcess));

            List<BackgroundProcessEntity> reportBulkProcesses = BackgroundProcessEntityService.getInstance()
                    .listPaginated(be, null, null);
            if ((reportBulkProcesses != null) && (!reportBulkProcesses.isEmpty())) {
                boolean enableFMCLogic = ConfigurationService.getAsBoolean(rootUser, "fmcSapEnableSapSync_bulkProcess");
                for (BackgroundProcessEntity reportBulkProcess : reportBulkProcesses) {
                    processIds.add(reportBulkProcess.getBackgroundProcess().getId());
                }

                for (Long updateId : processIds) {
                    BackgroundProcess reportBulkProcess = BackgroundProcessService.getInstance().get(updateId);
                    currentMainProcess = reportBulkProcess;
                    if (currentMainProcess.getProgress() != null
                            && currentMainProcess.getProgress() > 0
                            && !typeProcess.equals(Constants.UPDATE_PROCESS)) {
                        setPercentageStatus(currentMainProcess.getProgress());
                    }
                    Long affectedRecords = 0L;
                    Long ommitedRecords = 0L;
                    Long processTime = 0L;

                    BooleanBuilder builderIn = new BooleanBuilder();
                    builderIn = builderIn.and(QBackgroundProcessDetail.backgroundProcessDetail.backgroundProcess
                            .id.eq(reportBulkProcess.getId()));
                    List<BackgroundProcessDetail> lstReportBulkProcessesDetail =
                            BackgroundProcessDetailService.getInstance().listPaginated(builderIn, null, null);
                    final float percentagePerChunk = 100 / (float) lstReportBulkProcessesDetail.size();
                    if ((lstReportBulkProcessesDetail != null) && (!lstReportBulkProcessesDetail.isEmpty())) {
                        for (BackgroundProcessDetail reportBulkProcDetail : lstReportBulkProcessesDetail) {
                            subProcessIds.add(reportBulkProcDetail.getId());
                        }
                        for (Long subProcessId : subProcessIds) {
                            Map<String, Object> result = null;
                            BackgroundProcessDetail subProcess = BackgroundProcessDetailService.getInstance()
                                    .get(subProcessId);
                            long startTime = System.currentTimeMillis();
                            ++subProcessSequence;
                            subProcess.setStatus(Constants.IN_PROGRESS);
                            if (typeProcess.equals(Constants.UPDATE_PROCESS)) {
                                result = executeBulkUpdateProcess(
                                        subProcess, enableFMCLogic, percentagePerChunk, reloadAllThingsThreshold,
                                        sendThingFieldTickle);
                            } else if (typeProcess.equals(Constants.DELETE_PROCESS)) {
                                result = executeBulkDeleteProcess(
                                        subProcess, percentagePerChunk, reloadAllThingsThreshold, sendThingFieldTickle);
                            }
                            long endTime = System.currentTimeMillis();
                            //Update status of the bulk process detail from IN_PROGRESS to COMPLETED
                            subProcess.setTotalRecords((Long) result.get("totalRecords"));
                            subProcess.setTotalAffectedRecords((Long) result.get("affectedRecords"));
                            subProcess.setTotalOmittedRecords((Long) result.get("omittedRecords"));
                            subProcess.setProcessTime(endTime - startTime);
                            subProcess.setEndDate(new Date());
                            subProcess.setStatus(Constants.COMPLETED);
                            BackgroundProcessDetailService.getInstance().update(subProcess);

                            affectedRecords = affectedRecords + subProcess.getTotalAffectedRecords();
                            processTime = processTime + subProcess.getProcessTime();
                            ommitedRecords = ommitedRecords + subProcess.getTotalOmittedRecords();
                        }
                    }
                    //Update status of the bulk process detail from IN_PROGRESS to COMPLETED
                    if (affectedRecords > reportBulkProcess.getTotalRecords()) { //patch for THIS process
                        reportBulkProcess.setTotalAffectedRecords(reportBulkProcess.getTotalRecords());
                        reportBulkProcess.setTotalOmittedRecords(ommitedRecords);
                    } else {
                        reportBulkProcess.setTotalAffectedRecords(affectedRecords);
                        reportBulkProcess.setTotalOmittedRecords(reportBulkProcess.getTotalRecords() - affectedRecords);
                    }
                    reportBulkProcess.setProcessTime(processTime);
                    reportBulkProcess.setEndDate(new Date());
                    reportBulkProcess.setStatus(Constants.COMPLETED);
                    reportBulkProcess.setProgress(100);
                    BackgroundProcessService.getInstance().update(reportBulkProcess);
                    logger.info("Quantity of things affected by Massive Bulk Process: " + reportBulkProcess.getTotalRecords());
                }

            }
        } catch (Exception e) {
            logger.error("Error processing the Thread Bulk Process", e);
            HibernateDAOUtils.rollback(transaction);
        } finally {
            if (transaction.isActive()) {
                transaction.commit();
                transaction = null;
            }
            if (session.isOpen()) {
                session.close();
            }
        }
        currentMainProcess = null;//process finished, user now need to retrieve the state from database
        logger.info("End Bulk Process Job");
    }

    private void initSessionIfIsNull() {
        if (subject == null || subject.getPrincipal() == null) {
            RiotShiroRealm.initCaches();
            subject = SecurityUtils.getSubject();
            ApiKeyToken token = new ApiKeyToken(rootUser.getApiKey());
            subject.login(token);
            subject = PermissionsUtils.loginUser(token);
            PermissionsUtils.isPermitted(subject, "anyResource");
            userLogged = (User) subject.getPrincipal();
        }
    }

    /**
     * Computes the size of the chunks based on the SUBPROCESS_CHUNK percentage
     *
     * @param totalRegisters
     * @return
     */
    private int estimateChunk(final int totalRegisters) {
        int estimateChunk = (totalRegisters * SUBPROCESS_CHUNK) / 100;
        //because we are using integers, the minimum value must be 1
        //and because the process is slow, the maximum value must be 1000
        //this in order to keep moving the progressbar
        int chunk = Math.min(Math.max(estimateChunk, 1), CHUNK_SIZE);
        logger.info("Estimated chunk size to update main process: " + chunk);
        return chunk;
    }

    /**
     * Updates the percentage status of the process
     */
    private void updateMainProcess() {
        logger.info("Begin updating main process");
        currentMainProcess.setProgress(Math.round(getPercentageStatus()));
        BackgroundProcessService.getInstance().update(this.currentMainProcess);
        logger.info("End updating main process");
    }

    private void updateProgressBar(final int total, final float chunkPercentage) {
        setPercentageStatus(estimatePercentage(total, 1, chunkPercentage));
    }

    private float estimatePercentage(final int total, final int chunk, final float chunkPercentage) {
        float subProcessPercentage = (chunkPercentage * chunk) / total;
        float sumPercentage = getPercentageStatus() + subProcessPercentage;
        int totalRoundChunk = Math.round(chunkPercentage) * this.subProcessSequence;
        if (sumPercentage > totalRoundChunk) {
            sumPercentage = totalRoundChunk;
        }
        return sumPercentage;
    }

    /**
     * Report Bulk Process Update
     *
     * @param backgroundProcessDetail
     * @param enableFMCLogic
     * @return
     * @throws NonUniqueResultException
     */
    public Map<String, Object> executeBulkUpdateProcess(
            BackgroundProcessDetail backgroundProcessDetail,
            boolean enableFMCLogic,
            float percentagePerChunk,
            Long reloadAllThingsThreshold,
            Boolean sendThingFieldTickle) throws NonUniqueResultException {
        Map<String, Object> resultBulkProcess = new HashMap<>();
        List<Long> groupMqtt = backgroundProcessDetail.getThingType() != null ?
                GroupService.getInstance().getMqttGroups(backgroundProcessDetail.getThingType().getGroup()) : null;

        String thingTypeCode;
        User user = backgroundProcessDetail.getBackgroundProcess().getCreatedByUser();
        boolean validateThingType;
        if (backgroundProcessDetail.getThingType() != null) {
            thingTypeCode = backgroundProcessDetail.getThingType().getCode();
            cache.getThingType(thingTypeCode);
            validateThingType = false;
        } else {
            thingTypeCode = "All Thing Types";
            validateThingType = true;
        }
        try {
            logger.info("ThingTypeCode: " + thingTypeCode);
            logger.info("Query: " + backgroundProcessDetail.getQuery());
            Date storeData = new Date();
            List<TickleItem> thingsToTickle = new ArrayList<>();
            List<Long> lstIds = getListOfIdsMassiveProcess(backgroundProcessDetail.getQuery());
            final int totalRecords = lstIds.size();
            final int chunk = estimateChunk(totalRecords);

            Map<String, Object> result;
            JSONParser json = new JSONParser();
            Map<String, Object> data = (Map<String, Object>) json.parse(backgroundProcessDetail.getValuesToChange());
            Map<String, Boolean> validations = new HashMap<>();
            validations.put("thingType", validateThingType);
            validations.put("group", false);
            validations.put("thing.exists", false);
            validations.put("thing.serial", false);
            validations.put("thing.parent", false);
            validations.put("thing.children", false);
            validations.put("thing.udfs", true);
            validations.put("thing.uFavoriteAndRecent", false);
            FlushMode flushMode = HibernateSessionFactory.getInstance().getCurrentSession().getFlushMode();
            HibernateSessionFactory.getInstance().getCurrentSession().setFlushMode(FlushMode.COMMIT);
            int count = 0;
            for (int i = 0; i < lstIds.size(); i++) {
                try {
                    Tuple dataForBulk = ThingService.getInstance().getDataForBulk(lstIds.get(i));
                    Thing thing = dataForBulk.get(0, Thing.class);
                    String thingTypeCodeToProcess = dataForBulk.get(1, String.class);
                    String hierarchyName = dataForBulk.get(2, String.class);
                    Stack<Long> recursivelyStack = new Stack<>();
                    result = ThingService.getInstance().update(
                            recursivelyStack
                            , thing
                            , thingTypeCodeToProcess
                            , hierarchyName
                            , thing.getName()
                            , thing.getSerialNumber()
                            , null//parent
                            , data
                            , null
                            , null
                            , false, false, storeData, !enableFMCLogic, validations
                            , cache
                            , false, true, subject, user, true);
                    TickleItem tickleItem = getTickleItem(thing.getSerialNumber(), thingTypeCodeToProcess,
                            storeData, (Map<String, Object>) result.get("fields"));
                    thingsToTickle.add(tickleItem);

                    subject.getSession().touch();

                    count++;
                    logger.info("Updated register #" + count);
                    updateProgressBar(totalRecords, percentagePerChunk);
                    if (count % chunk == 0) {
                        updateMainProcess();
                    }
                    if (count % CHUNK_SIZE == 0) {
                        logger.info("Begin commit and send tickles by chunk");
                        commitTransactionByChunks(count, backgroundProcessDetail, totalRecords);
                        sendTicklesToUpdate(sendThingFieldTickle, thingsToTickle, reloadAllThingsThreshold, groupMqtt);
                        thingsToTickle = null;
                        thingsToTickle = new ArrayList<>();
                        logger.info("End commit and send tickles by chunk");
                    }
                } catch (Exception e) {
                    logger.info("Thing ID" + lstIds.get(i) + " was not updated. Cause: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            if (count < totalRecords) {
                updateMainProcess();
            }

            int omitted = lstIds.size() - count;
            HibernateSessionFactory.getInstance().getCurrentSession().setFlushMode(flushMode);
            sendTicklesToUpdate(sendThingFieldTickle, thingsToTickle, reloadAllThingsThreshold, groupMqtt);

            resultBulkProcess.put("totalRecords", Long.valueOf(lstIds.size() + ""));
            resultBulkProcess.put("affectedRecords", Long.valueOf(count + ""));
            resultBulkProcess.put("omittedRecords", Long.valueOf(omitted + ""));
        } catch (ParseException e) {
            BackgroundProcessEntity backgroundProcessEntity = BackgroundProcessEntityService.getInstance()
                    .getModuleInformation(backgroundProcessDetail.getBackgroundProcess(), "imports");
            ReportDefinition reportDefinition = ReportDefinitionService.getInstance()
                    .get(Long.valueOf(backgroundProcessEntity.getColumnValue()));
            logger.error("Error parsing values to change for report " + reportDefinition.getName()
                    + " with ID " + reportDefinition.getId());
        }
        logger.info("Report " + backgroundProcessDetail.getBackgroundProcess().getId() +
                " executed with query: " + backgroundProcessDetail.getQuery());
        return resultBulkProcess;
    }

    /**
     * commits and open a new transaction
     */
    private void commitAndStartTransaction() {
        session.flush();
        session.clear();
        transaction.commit();
        session = HibernateSessionFactory.getInstance().getCurrentSession();
        session.setFlushMode(FlushMode.COMMIT);
        transaction = session.getTransaction();
        transaction.begin();
    }

    /**
     * Proceed to execute the compensation data between SQL/NoSQL databases
     *
     * @param backgroundProcessDetail
     * @throws MongoExecutionException
     */
    private void doDeleteCompensationProcess(
            BackgroundProcessDetail backgroundProcessDetail,
            Long reloadAllThingsThreshold,
            Boolean sendThingFieldTickle) throws MongoExecutionException {
        deleteMongo(backgroundProcessDetail, true, reloadAllThingsThreshold,
                sendThingFieldTickle);//proceed to delete in mongo
        commitAndStartTransaction();//commit deleted elements in reportbulkprocessdetaillog
    }

    /**
     * Report Bulk Process Delete
     *
     * @param backgroundProcessDetail object
     * @return Map of results
     * @throws NonUniqueResultException
     */
    public Map<String, Object> executeBulkDeleteProcess(
            BackgroundProcessDetail backgroundProcessDetail,
            float percentagePerChunk,
            Long reloadAllThingsThreshold,
            Boolean sendThingFieldTickle) throws NonUniqueResultException, MongoExecutionException {
        logger.info("Query: " + backgroundProcessDetail.getQuery());
        Map<String, Object> resultBulkProcess = new HashMap<>();
        List<Long> groupMqtt = backgroundProcessDetail.getThingType() != null ?
                GroupService.getInstance().getMqttGroups(backgroundProcessDetail.getThingType().getGroup()) : null;

        User user = backgroundProcessDetail.getBackgroundProcess().getCreatedByUser();
        Date storeData = new Date();
        List<TickleItem> thingsToTickle = new ArrayList<>();
        FlushMode flushMode = HibernateSessionFactory.getInstance().getCurrentSession().getFlushMode();
        HibernateSessionFactory.getInstance().getCurrentSession().setFlushMode(FlushMode.COMMIT);
        int count = (backgroundProcessDetail.getTotalAffectedRecords() != null) ?
                backgroundProcessDetail.getTotalAffectedRecords().intValue() : 0;
        if (count > 0) {
            logger.warn("Things already deleted in relational database, initializing compensation process " +
                    "in NoSQL database after system crash");
            //compensation process
            doDeleteCompensationProcess(backgroundProcessDetail, reloadAllThingsThreshold, sendThingFieldTickle);
        }
        List<Long> lstIds = getListOfIdsMassiveProcess(backgroundProcessDetail.getQuery());
        logger.info("Query executed.");
        final int totalRecords = (backgroundProcessDetail.getTotalRecords() != null) ?
                backgroundProcessDetail.getTotalRecords().intValue() : lstIds.size();
        final int chunk = estimateChunk(totalRecords);
        for (Long idThing : lstIds) {
            Stack<Long> recursivelyStack = new Stack<>();
            Thing thing = ThingService.getInstance().get(idThing);
            try {
                ThingService.getInstance().delete(recursivelyStack, thing, false, storeData,
                        false, user, subject, userLogged, true, false, false);
                insertBulkProcessDetailLog(thing, backgroundProcessDetail);
                TickleItem tickleItem = getTickleItem(thing.getSerialNumber(), thing.getThingType().getThingTypeCode(),
                        storeData, null);
                thingsToTickle.add(tickleItem);
                subject.getSession().touch();
                count++;
                logger.info("Deleted register #" + count);
                updateProgressBar(totalRecords, percentagePerChunk);
                if (count % chunk == 0) {
                    updateMainProcess();
                }
                if (count % CHUNK_SIZE == 0) {
                    logger.info("Begin commit and send tickles by chunk");
                    commitTransactionByChunks(count, backgroundProcessDetail, totalRecords);
                    deleteMongo(backgroundProcessDetail, false, reloadAllThingsThreshold,
                            sendThingFieldTickle);
                    commitAndStartTransaction();//clear the temporal table of ids
                    sendTicklesToDelete(sendThingFieldTickle, thingsToTickle, reloadAllThingsThreshold, groupMqtt);
                    thingsToTickle = new ArrayList<>();
                    logger.info("End commit and send tickles by chunk");
                }
            } catch (Exception e) {
                logger.info("Thing ID" + idThing + " was not deleted. Cause: " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (count < totalRecords) {
            updateMainProcess();
        }
        commitTransactionByChunks(count, backgroundProcessDetail, totalRecords);
        deleteMongo(backgroundProcessDetail, false, reloadAllThingsThreshold, sendThingFieldTickle);
        HibernateSessionFactory.getInstance().getCurrentSession().setFlushMode(flushMode);
        int omitted = totalRecords - count;//lstIds.size() - count;
        sendTicklesToDelete(sendThingFieldTickle, thingsToTickle, reloadAllThingsThreshold, groupMqtt);

        resultBulkProcess.put("totalRecords", Long.valueOf(totalRecords + ""));
        resultBulkProcess.put("affectedRecords", Long.valueOf(count + ""));
        resultBulkProcess.put("omittedRecords", Long.valueOf(omitted + ""));
        logger.info("Report " + backgroundProcessDetail.getBackgroundProcess().getId() + " executed with query: " +
                backgroundProcessDetail.getQuery());
        lstIds.clear();
        lstIds = null;
        return resultBulkProcess;
    }

    /**
     * Send Tickles to delete
     *
     * @param sendThingFieldTickle     Boolean value, send thing field tickle
     * @param thingsToTickle           List of things for sending tickles
     * @param reloadAllThingsThreshold Configuration tickles threshold of number of things
     * @param groupMqtt                List of Mqtt groups
     */
    private void sendTicklesToDelete(
            Boolean sendThingFieldTickle,
            List<TickleItem> thingsToTickle,
            Long reloadAllThingsThreshold,
            List<Long> groupMqtt) {
        boolean successfullyCalled = performThingTickle(thingsToTickle, reloadAllThingsThreshold, groupMqtt);
        if (successfullyCalled) {
            performThingFieldTickle(sendThingFieldTickle, true, thingsToTickle, true, groupMqtt);
        }
    }

    /**
     * @param sendThingFieldTickle
     * @param thingsToTickle
     * @param reloadAllThingsThreshold
     */
    private void sendTicklesToUpdate(
            Boolean sendThingFieldTickle,
            List<TickleItem> thingsToTickle,
            Long reloadAllThingsThreshold,
            List<Long> groupMqtt) {
        boolean successfullyCalled = performThingTickle(thingsToTickle, reloadAllThingsThreshold, groupMqtt);
        if (successfullyCalled) {
            performThingFieldTickle(sendThingFieldTickle, true, thingsToTickle, false, groupMqtt);
        }
    }

    /**
     * Insert Bulk Process Detail Log
     *
     * @param thing                   Thing
     * @param backgroundProcessDetail Object
     */
    public void insertBulkProcessDetailLog(Thing thing, BackgroundProcessDetail backgroundProcessDetail) {
        BackgroundProcessDetailLog bulkLog = new BackgroundProcessDetailLog();
        bulkLog.setThingId(thing.getId());
        bulkLog.setBackgroundProcessDetail(backgroundProcessDetail);
        bulkLog.setStatus(Constants.ADDED);
        bulkLog.setSerialNumber(thing.getSerialNumber());
        bulkLog.setThingTypeCode(thing.getThingType().getThingTypeCode());
        BackgroundProcessDetailLogService.getInstance().insert(bulkLog);
    }

    /**
     * Delete data in Mongo Things, ThingSnapshotIds, ThingSnapshots
     *
     * @param backgroundProcessDetail object
     * @throws MongoExecutionException Exception
     */
    public void deleteMongo(
            BackgroundProcessDetail backgroundProcessDetail,
            boolean isCompensationProcess,
            Long reloadAllThingsThreshold,
            Boolean sendThingFieldTickle) throws MongoExecutionException {
        BooleanBuilder be = new BooleanBuilder();
        List<Long> groupMqtt = backgroundProcessDetail.getThingType() != null ?
                GroupService.getInstance().getMqttGroups(backgroundProcessDetail.getThingType().getGroup()) : null;

        be.and(QBackgroundProcessDetailLog.backgroundProcessDetailLog.backgroundProcessDetail.eq(backgroundProcessDetail));
        List<BackgroundProcessDetailLog> lstIdsToDeleteObj = BackgroundProcessDetailLogService.getInstance()
                .listPaginated(be, null, null);
        Date storeData = new Date();
        List<TickleItem> thingsToTickle = new ArrayList<>();
        for (int i = 0; i < lstIdsToDeleteObj.size(); i++) {
            BackgroundProcessDetailLog rpbpDetailLog = lstIdsToDeleteObj.get(i);
            ThingMongoService.getInstance().deleteThing(rpbpDetailLog.getThingId());
            BackgroundProcessDetailLogService.getInstance().delete(rpbpDetailLog);
            if (isCompensationProcess) {
                TickleItem tickleItem = getTickleItem(rpbpDetailLog.getSerialNumber(), rpbpDetailLog.getThingTypeCode(),
                        storeData, null);
                thingsToTickle.add(tickleItem);
            }
        }
        if (isCompensationProcess) {
            logger.info("Sending tickles after system crash");
            sendTicklesToDelete(sendThingFieldTickle, thingsToTickle, reloadAllThingsThreshold, groupMqtt);
        }
        lstIdsToDeleteObj = new ArrayList<>();
        lstIdsToDeleteObj = null;
    }

    /**
     * Executes commit transaction by Chunk
     *
     * @param count                   Total affected records
     * @param backgroundProcessDetail report Bulk proces detail
     * @param totalRecords            total records of the bulk process detail
     */
    public void commitTransactionByChunks(int count, BackgroundProcessDetail backgroundProcessDetail,
                                          int totalRecords) {
        backgroundProcessDetail.setTotalRecords((long) totalRecords);
        backgroundProcessDetail.setTotalOmittedRecords((long) (totalRecords - count));
        backgroundProcessDetail.setTotalAffectedRecords((long) count);
        BackgroundProcessDetailService.getInstance().update(backgroundProcessDetail);
        commitAndStartTransaction();
    }

    /**
     * Method to do ticvkle item
     *
     * @param serialNumber
     * @param thingTypeCode
     * @param transactionDate
     * @param fields
     * @return
     */
    public TickleItem getTickleItem(String serialNumber, String thingTypeCode, Date transactionDate,
                                    Map<String, Object> fields) {
        TickleItem tickleItem = new TickleItem();
        tickleItem.setSerialNumber(serialNumber);
        tickleItem.setThingTypeCode(thingTypeCode);
        tickleItem.setTransactionDate(transactionDate);

        if ((fields != null) && (!fields.isEmpty())) {
            for (String fieldName : fields.keySet()) {
                TickleFieldItem field = new TickleFieldItem();
                field.setName(fieldName);
                if (fields.get(fieldName) != null) {
                    field.setValue(fields.get(fieldName).toString());
                } else {
                    field.setValue("");
                }
                tickleItem.addField(field);
            }
        }
        return tickleItem;
    }

    /**
     * Execution of tickles
     *
     * @param thingsToTickle           List of things for sending tickles
     * @param reloadAllThingsThreshold Configuration threshold of tickles
     * @param mqttGroup                List of Mqtt groups
     * @return boolean Value
     */
    public boolean performThingTickle(List<TickleItem> thingsToTickle, Long reloadAllThingsThreshold, List<Long> mqttGroup) {
        if (reloadAllThingsThreshold == null) {
            logger.error("Cannot call the message to reload CoreBridge cache because " +
                    "the things cache reload threshold specified is invalid.");
            return false;
        } else {
            logger.info("reloadAllThingsThreshold_bulkProcess=" + reloadAllThingsThreshold);
            if (thingsToTickle.size() > reloadAllThingsThreshold) {
                BrokerClientHelper.sendRefreshThingMessage(true, mqttGroup);
            } else {
                if (thingsToTickle.size() > 0) {
                    List<CompletableFuture<Boolean>> ticklesSent = thingsToTickle.parallelStream()
                            .map(tickleItem -> CompletableFuture.supplyAsync(
                                    () -> sendOneTickle(tickleItem, mqttGroup), TICKLE_EXECUTOR))
                            .collect(Collectors.toList());
                    ticklesSent.stream().map(CompletableFuture::join).count();//just wait completion
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean sendOneTickle(TickleItem tickleItem, List<Long> mqttGroup) {
        try {
            logger.info("Sending to refresh single thing message : " + tickleItem.getSerialNumber());
            BrokerClientHelper.sendRefreshSingleThingMessage(
                    tickleItem.getThingTypeCode(),
                    tickleItem.getSerialNumber(), true, mqttGroup);
            logger.info("Refresh single thing message sent : " + tickleItem.getSerialNumber());
            return true;
        } catch (Exception e) {
            logger.error("Exception Send Refresh Single Thing Message " + tickleItem.getSerialNumber(), e);
            return false;
        }
    }

    public void performThingFieldTickle(
            Boolean sendThingFieldTickle,
            boolean runRules,
            List<TickleItem> thingsToTickle,
            boolean delete,
            List<Long> groupMqtt) {
        logger.info("sendThingFieldTickle_bulkProcess=" + sendThingFieldTickle);
        if (runRules) {
            sendThingFieldTickle = true;
        }
        if ((sendThingFieldTickle != null) && sendThingFieldTickle) {
            AtomicInteger i = new AtomicInteger(1);
            if (!delete) {
                logger.info("Init Thing Field Tickle for Thing Type Code: " + thingsToTickle.get(0).getThingTypeCode());
                BrokerClientHelper.initThingFieldTickle(thingsToTickle.get(0).getThingTypeCode(), groupMqtt);
                List<CompletableFuture<Boolean>> ticklesSent = thingsToTickle.parallelStream()
                        .map(tickleItem -> CompletableFuture.supplyAsync(
                                () -> sendOneTickleField(tickleItem), TICKLE_EXECUTOR))
                        .collect(Collectors.toList());
                ticklesSent.stream().map(CompletableFuture::join).count();//just wait completion
                // sending the message
                BrokerClientHelper.sendThingFieldTickle(true);
            } else {
                List<CompletableFuture<Boolean>> ticklesSent = thingsToTickle.parallelStream()
                        .map(tickleItem -> CompletableFuture.supplyAsync(
                                () -> sendOneTickleToDelete(i, tickleItem, groupMqtt), TICKLE_EXECUTOR))
                        .collect(Collectors.toList());
                ticklesSent.stream().map(CompletableFuture::join).count();//just wait completion
            }
        }
    }

    private boolean sendOneTickleField(TickleItem tickleItem) {
        logger.info("Sending thingField tickle: " + tickleItem.getSerialNumber());
        if (tickleItem.getFields().size() != 0) {
            // Building message with UDFs
            for (TickleFieldItem tickleFieldItem : tickleItem.getFields()) {
                BrokerClientHelper.setThingField(
                        tickleItem.getSerialNumber(),
                        tickleItem.getTransactionDate().getTime(),
                        tickleFieldItem.getName(),
                        "\"" + tickleFieldItem.getValue() + "\"");
            }
            return true;
        } else {
            return false;
        }
    }

    private boolean sendOneTickleToDelete(AtomicInteger i, TickleItem tickleItem, List<Long> groupMqtt) {
        logger.info("Sending thingField tickle " + i + ": " + tickleItem.getSerialNumber());
        BrokerClientHelper.sendDeleteThingMessage(
                tickleItem.getThingTypeCode(),
                tickleItem.getSerialNumber(), true, groupMqtt);
        return true;
    }

    /**
     * get List of IDs to be modified by massive process
     *
     * @param query Query to execute bulk process
     * @return
     */
    public List<Long> getListOfIdsMassiveProcess(String query) {
        List<Long> result = new ArrayList<>();
        BasicDBObject queryDBObject = BasicDBObject.parse(query);
        DBCursor cursor = MongoDAOUtil.getInstance().things.find(queryDBObject, new BasicDBObject("_id", 1));
        cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
        cursor.addOption(Bytes.QUERYOPTION_SLAVEOK);
        cursor.batchSize(Constants.CHUNK_AMOUNT);
        while (cursor.hasNext()) {
            DBObject idDoc = cursor.next();
            result.add(Long.parseLong(idDoc.get("_id").toString()));
        }
        return result;
    }

    public List<Long> getListOfIdsMassiveProcess2(BackgroundProcessDetail BackgroundProcessDetail, String query) {
        List<Long> result = new ArrayList<>();
        int count = 0;
        BasicDBObject queryDBObject = BasicDBObject.parse(query);
        DBCursor cursor = MongoDAOUtil.getInstance().things.find(queryDBObject, new BasicDBObject("_id", 1));
        cursor.addOption(Bytes.QUERYOPTION_NOTIMEOUT);
        cursor.addOption(Bytes.QUERYOPTION_SLAVEOK);
        cursor.batchSize(Constants.CHUNK_AMOUNT);
        //Insert
        while (cursor.hasNext()) {
            count++;
            DBObject idDoc = cursor.next();
            Long thingId = Long.parseLong(idDoc.get("_id").toString());
            result.add(thingId);

            BackgroundProcessDetailLog bulkLog = new BackgroundProcessDetailLog();
            bulkLog.setBackgroundProcessDetail(BackgroundProcessDetail);
            bulkLog.setThingId(thingId);
            bulkLog.setStatus(Constants.ADDED);
            BackgroundProcessDetailLogService.getInstance().insert(bulkLog);

            if (count % CHUNK_SIZE == 0) {
                session.flush();
            }
        }
        commitAndStartTransaction();
        cursor = null;
        return result;
    }

    public synchronized float getPercentageStatus() {
        return percentageStatus;
    }

    private synchronized void setPercentageStatus(float percentageStatus) {
        if (percentageStatus >= 100) {
            this.percentageStatus = 99.9f;
        } else if (percentageStatus > this.percentageStatus) {
            this.percentageStatus = percentageStatus;
        }
    }

    public synchronized BackgroundProcess getCurrentMainProcess(Long reportDefinitionId) {
        if (this.reportDefinitionId.equals(reportDefinitionId)) {
            return currentMainProcess;
        }
        return null;
    }

    public synchronized boolean isBulkProcessRunning() {
        return (currentMainProcess != null);
    }
}
