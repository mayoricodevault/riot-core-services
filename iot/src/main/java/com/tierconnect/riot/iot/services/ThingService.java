package com.tierconnect.riot.iot.services;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.Tuple;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.ListPath;
import com.tierconnect.riot.appcore.controllers.GroupController;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.FavoriteService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.LicenseService;
import com.tierconnect.riot.appcore.utils.*;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.commons.utils.DateHelper;
import com.tierconnect.riot.commons.utils.FormatUtil;
import com.tierconnect.riot.commons.utils.TenantUtil;
import com.tierconnect.riot.iot.controllers.LogicalReaderController;
import com.tierconnect.riot.iot.controllers.ShiftController;
import com.tierconnect.riot.iot.controllers.ThingController;
import com.tierconnect.riot.iot.controllers.ZoneController;
import com.tierconnect.riot.iot.dao.SequenceDAO;
import com.tierconnect.riot.iot.dao.ThingDAO;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.dao.util.FormulaUtil;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.fmc.utils.FMCUtils;
import com.tierconnect.riot.iot.reports_integration.*;
import com.tierconnect.riot.iot.services.thing.ThingBoundary;
import com.tierconnect.riot.iot.utils.Cache;
import com.tierconnect.riot.iot.utils.VisibilityThingUtils;
import com.tierconnect.riot.sdk.dao.*;
import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;
import joptsimple.internal.Strings;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;

import java.io.File;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.tierconnect.riot.commons.serializers.Constants.DATE_FORMAT;
import static com.tierconnect.riot.iot.utils.VisibilityThingUtils.limitVisibilityDeleteT;

public class ThingService extends ThingServiceBase {
    private static Logger logger = Logger.getLogger(ThingService.class);
    final private static int maxCollection = System.getProperties().contains("hibernate.dialect") ? System.getProperty("hibernate.dialect").contains("SQLServerDialect") ? 2000 : 1048576 : 2000;

    private static ThingDAO _thingDAO = new ThingDAO();

    public static ThingDAO getThingDAO() {
        return _thingDAO;
    }

    static private ThingService INSTANCE = new ThingService();

    public static ThingService getInstance() {
        return INSTANCE;
    }


    /**************************************************
     * @method upsert
     * @description This method creates  or pdates thing based on the param values
     * @param thingType thing type Class instance
     * @param groupHierarchyCode Name of the group
     * @param name Name of the group
     * @param serialNumber Serial number of the new thing
     * @param parent Data of the parent: serialNumber, thingTypeCode
     * @param udfs Map  of Udfs ofthe thing
     * @param children Map of children
     * @param executeTickle Flag to enable executeTickle (call to bridges)
     * @param validateVisibility Flag to enable limitVisibility to create a new thing
     * @param transactionDate Transaction date
     * @param disableFMCLogic thing created from import option
     * @param createAndFlush To flush after create in DB
     * @return Udf's of the new thing type
     * @throws UserException
     ***********************************************/
    public Map<String, Object> upsert(
            Stack<Long> recursivelyStack,
            ThingType thingType,
            String groupHierarchyCode,
            String name,
            String serialNumber,
            Map<String, Object> parent,
            Map<String, Object> udfs,
            Object children,
            Object childrenUdf,
            boolean executeTickle,
            boolean validateVisibility,
            Date transactionDate,
            boolean disableFMCLogic,
            boolean createAndFlush,
            Boolean useDefaultValues,
            Map<String, Boolean> validations,
            Subject subject,
            boolean fillSource) throws UserException {


        Thing thing;
        try {
            thing = getBySerialAndThingType(serialNumber, thingType);
        } catch (NonUniqueResultException e) {
            throw new UserException("Multiple things exist in Thing Type '" + thingType.getCode() + "' with the same serial '"
                    + serialNumber + "'", e);
        }

        //TODO cvertiz: Review and fix this ugly code, retry update when error occurs requested by GusR
        if (thing == null) {
            try {
                return ThingsService.getInstance().create(
                        recursivelyStack,
                        thingType.getCode(),
                        groupHierarchyCode,
                        name,
                        serialNumber,
                        parent,
                        udfs,
                        children,
                        childrenUdf,
                        executeTickle,
                        validateVisibility,
                        transactionDate,
                        disableFMCLogic,
                        createAndFlush,
                        useDefaultValues,
                        validations,
                        null,
                        fillSource,
                        subject);
            } catch (ConstraintViolationException e) {
                logger.warn("Error occurred creating thing '" + serialNumber + "', thing already exists, trying to update thing");
                try {
                    thing = getBySerialAndThingType(serialNumber, thingType);
                    return update(
                            recursivelyStack,
                            thing,
                            thingType.getCode(),
                            groupHierarchyCode,
                            name,
                            serialNumber,
                            parent,
                            udfs,
                            children,
                            childrenUdf,
                            executeTickle,
                            validateVisibility,
                            transactionDate,
                            disableFMCLogic,
                            null, // validations
                            null, // cache
                            createAndFlush, // updateAndFlush
                            true,// recursivilyUpdate
                            (User) subject.getPrincipal(),
                            fillSource);//fillSource
                } catch (Exception ex) {
                    throw new UserException("Error occurred creating thing '" + serialNumber + "'", ex);
                }
            }
        } else {
            return update(
                    recursivelyStack,
                    thing,
                    thingType.getCode(),
                    groupHierarchyCode,
                    name,
                    serialNumber,
                    parent,
                    udfs,
                    children,
                    childrenUdf,
                    executeTickle,
                    validateVisibility,
                    transactionDate,
                    disableFMCLogic,
                    null, // validations
                    null, // cache
                    createAndFlush, // updateAndFlush
                    true,// recursivilyUpdate
                    (User) subject.getPrincipal(),
                    fillSource);
        }
    }

    public Map<String, Object> update(Stack<Long> recursivelyStack
            , Thing child
            , Map<String, Object> parent
            , Map<String, Object> udfs
            , Object children
            , Object childrenUdf
            , boolean executeTickle
            , boolean validateVisibility
            , Date transactionDate, boolean disableFMCLogic
            , User currentUser
            , boolean fillSource) throws UserException {
        return update(recursivelyStack
                , child
                , child.getThingType().getThingTypeCode()
                , child.getGroup().getHierarchyName(true)
                , child.getName()
                , child.getSerial()
                , parent
                , udfs
                , children
                , childrenUdf
                , executeTickle
                , validateVisibility
                , transactionDate
                , disableFMCLogic
                , null
                , null
                , true
                , true
                , currentUser
                , fillSource);
    }

    public Map<String, Object> update(Stack<Long> recursivelyStack
            , Long thingId
            , String thingTypeCode
            , String groupHierarchyCode
            , String name
            , String serialNumber
            , Map<String, Object> parent
            , Map<String, Object> udfs
            , Object children //List<Map<String,Object>>
            , Object childrenUdf
            , boolean executeTickle
            , boolean validateVisibility
            , Date transactionDate
            , boolean disableFMCLogic
            , User user
            , boolean fillSource) throws UserException {

        Thing thing = ThingService.getInstance().get(thingId);
        return update(recursivelyStack,
                thing
                , thingTypeCode
                , groupHierarchyCode
                , name
                , serialNumber
                , parent
                , udfs
                , children //List<Map<String,Object>>
                , childrenUdf
                , executeTickle
                , validateVisibility
                , transactionDate
                , disableFMCLogic
                , null
                , null
                , true
                , true
                , user
                , fillSource);
    }

    public Map<String, Object> update(
            Stack<Long> recursivelyStack
            , Thing thing
            , String thingTypeCode
            , String groupHierarchyCode
            , String name
            , String serialNumber
            , Object parent
            , Map<String, Object> udfs
            , Object children
            , Object childrenUdf
            , boolean executeTickle
            , boolean validateVisibility
            , Date transactionDate
            , boolean disableFMCLogic
            , Map<String, Boolean> validations
            , Cache cache
            , boolean updateAndFlush
            , boolean recursiveUpdate
            , Subject subject
            , User user
            , boolean fillSource) throws UserException {
        return update(recursivelyStack,
                thing
                , thingTypeCode
                , groupHierarchyCode
                , name
                , serialNumber
                , parent
                , udfs
                , children //List<Map<String,Object>>
                , childrenUdf
                , executeTickle
                , validateVisibility
                , transactionDate
                , disableFMCLogic
                , validations
                , cache
                , updateAndFlush
                , recursiveUpdate
                , null
                , subject
                , user
                , fillSource
        );
    }


    public Map<String, Object> update(
            Stack<Long> recursivelyStack
            , Thing thing
            , String thingTypeCode
            , String groupHierarchyCode
            , String name
            , String serialNumber
            , Object parent
            , Map<String, Object> udfs
            , Object children
            , Object childrenUdf
            , boolean executeTickle
            , boolean validateVisibility
            , Date transactionDate
            , boolean disableFMCLogic
            , Map<String, Boolean> validations
            , Cache cache
            , boolean updateAndFlush
            , boolean recursiveUpdate
            , User user
            , boolean fillSource) throws UserException {
        return update(recursivelyStack,
                thing
                , thingTypeCode
                , groupHierarchyCode
                , name
                , serialNumber
                , parent
                , udfs
                , children //List<Map<String,Object>>
                , childrenUdf
                , executeTickle
                , validateVisibility
                , transactionDate
                , disableFMCLogic
                , validations
                , cache
                , updateAndFlush
                , recursiveUpdate
                , null
                , null
                , user
                , fillSource
        );
    }

    /**************************************************
     * @method update
     * @description This method update a new thing based on the param values
     * @param thing thing to modify
     * @param thingTypeCode Code of thing type
     * @param groupHierarchyCode Name of the group
     * @param name Name of the group
     * @param serialNumber Serial number of the new thing
     * @param parent If parent is map, it contains serialNumber and thingTypeCode. These are the options:
     *               - parent{serialNumber, thingTypeCode}: If we want to change the parent of the thing
     *               - parent{}: If we want to delete the relation with the actual parent
     *               - [we do not send parent]: If we do not want to change any data of the actual parent
     *               If parent is a Thing, set and associate directly.
     * @param udfs Map of udf's of the thing
     * @param children Map of the children of the thing
     * @param executeTickle Flag to enable executeTickle (call to bridges)
     * @param validateVisibility Flag to enable limitVisibility to create a new thing
     * @param transactionDate Transaction date
     * @param disableFMCLogic thing created from import option
     * @param validations Flags for validations
     * @param cache Cache that contains thingTypes and Data Types
     * @param updateAndFlush To flush after update in DB
     * @param recursiveUpdate Flag to permit recursively update
     * @param facilityCode Facility Code to calculate location
     * @return Udf's of the new thing type
     * @throws UserException
     ***********************************************/
    public Map<String, Object> update(
            Stack<Long> recursivelyStack
            , Thing thing
            , String thingTypeCode
            , String groupHierarchyCode
            , String name
            , String serialNumber
            , Object parent
            , Map<String, Object> udfs
            , Object children
            , Object childrenUdf
            , boolean executeTickle
            , boolean validateVisibility
            , Date transactionDate
            , boolean disableFMCLogic
            , Map<String, Boolean> validations
            , Cache cache
            , boolean updateAndFlush
            , boolean recursiveUpdate
            , String facilityCode
            , Subject subject
            , User currentUser
            , boolean fillSource) throws UserException {
        return ThingBoundary.getInstance().update(recursivelyStack
                , thing
                , thingTypeCode
                , groupHierarchyCode
                , name
                , serialNumber
                , parent
                , udfs
                , children
                , childrenUdf
                , executeTickle
                , validateVisibility
                , transactionDate
                , disableFMCLogic
                , validations
                , cache
                , updateAndFlush
                , recursiveUpdate
                , facilityCode
                , subject
                , currentUser
                , fillSource);
    }

    public Map<String, Object> addMissingNonUdfs(Thing thing, DBObject thingMongo, Date transactionTime, Date modifiedTime) {
        Map<String, Object> nonUdfs = new LinkedHashMap<>();
        if (thingMongo.get("groupTypeId") == null) {
            Group group = thing.getGroup();
            GroupType groupType = group.getGroupType();
            ThingType thingType = thing.getThingType();
            nonUdfs.put("groupTypeId", groupType.getId());
            nonUdfs.put("groupTypeName", groupType.getName());
            nonUdfs.put("groupTypeCode", groupType.getCode() == null ? "" : groupType.getCode());
            nonUdfs.put("groupId", group.getId());
            nonUdfs.put("groupCode", group.getCode() == null ? "" : group.getCode());
            nonUdfs.put("groupName", group.getName());
            nonUdfs.put("thingTypeId", thingType.getId());
            nonUdfs.put("thingTypeCode", thingType.getThingTypeCode());
            nonUdfs.put("thingTypeName", thingType.getName());
            nonUdfs.put("name", thing.getName());
            nonUdfs.put("serialNumber", thing.getSerial());
        }
        return nonUdfs;
    }

    /**************************************************
     * @method delete
     * @description This method delete a thing
     * @param thing Thing to modify
     * @param validateVisibility boolean to validate or not the visibility
     * @param transactionDate Transaction Date
     * @throws UserException
     ***********************************************/
    public void delete(
            Stack<Long> recursivelyStack,
            Thing thing,
            boolean validateVisibility,
            Date transactionDate,
            boolean executeTickle,
            User currentUser,
            Subject subject,
            User userLogged,
            boolean fillSource,
            boolean deleteMongoFlag,
            boolean secure) throws UserException {
        ThingBoundary.getInstance().delete(recursivelyStack, thing, validateVisibility,
                transactionDate, executeTickle, currentUser, subject, userLogged, fillSource, deleteMongoFlag, secure);
    }

    public void deleteFavorite(User currentUser, Thing thing) {
        String typeElement = "thing";
        Long elementId = thing.getId();

        BooleanBuilder beAnd = new BooleanBuilder();
        beAnd = beAnd.and(QFavorite.favorite.typeElement.eq(typeElement));
        beAnd = beAnd.and(QFavorite.favorite.elementId.eq(elementId));
        beAnd = beAnd.and(QFavorite.favorite.user.eq(currentUser));

        List<Favorite> favorites = FavoriteService.getInstance().listPaginated(beAnd, null, null);
        if (!favorites.isEmpty()) {
            FavoriteService.getInstance().delete(favorites.get(0));
        }
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QFavorite.favorite.typeElement.eq(typeElement));
        be = be.and(QFavorite.favorite.elementId.eq(elementId));
        List<Favorite> listFavorite = FavoriteService.getInstance().listPaginated(be, null, null);
        for (Favorite favorite1 : listFavorite) {
            favorite1.setStatus("DELETED");
            FavoriteService.getInstance().update(favorite1);
        }
    }

    /**
     * Delete a thing of cache
     *
     * @param thing
     */
    public void deleteCacheThing(Thing thing) {
        String serialNumber = thing.getSerialNumber();
        String thingTypeCode = thing.getThingType().getThingTypeCode();
        Group tenant = thing.getThingType().getGroup();
        String tenantCode = TenantUtil.getTenantCode(tenant.getHierarchyName());
        final String key = String.format("%s-%s-%s", tenantCode, thingTypeCode, serialNumber);
        BrokerClientHelper.deleteCacheThing(key);
    }

    /**
     * Executes a commit to the relational database
     */
    private void commit() {
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        try {
            transaction.commit();
        } catch (Exception e) {
            rollBack(e, true);
        }
    }

    /**
     * Starts a new transaction
     */
    private void startTransaction() {
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        if (!transaction.isActive()) {
            transaction.begin();
        }
    }

    /**
     * Executes a rollback to the current session
     */
    private void rollBack(Exception e, boolean printAndThrowError) {
        Session session = HibernateSessionFactory.getInstance().getCurrentSession();
        Transaction transaction = session.getTransaction();
        HibernateDAOUtils.rollback(transaction);
        if (printAndThrowError) {
            logger.error("Error in commit manually to relational database", e);
            throw new UserException("Error in commit manually to relational database", e);
        }
    }

    /**
     * Inserts the basic data to delete a thing inside the compensation algorithm
     *
     * @param thing
     */
    public void insertIntoTempTable(Thing thing) {
        BackgroundProcessDetailLog tempTable = new BackgroundProcessDetailLog();
        tempTable.setThingId(thing.getId());
        tempTable.setStatus(Constants.ADDED);
        tempTable.setSerialNumber(thing.getSerialNumber());
        tempTable.setThingTypeCode(thing.getThingType().getThingTypeCode());
        BackgroundProcessDetailLogService.getInstance().insert(tempTable);
    }

    /**
     * Deletes multiple things in the system. Guaranties that the things
     * are deleted in SQL/NoSQL databases
     *
     * @param thingIds
     */
    public List<Map> secureDelete(
            List<Long> thingIds,
            boolean validateVisibility,
            boolean executeTickle,
            boolean fillSource,
            boolean secure,
            boolean processInMemory) {
        return secureDelete(thingIds, null, validateVisibility, executeTickle, fillSource, secure, processInMemory);
    }

    /**
     * Deletes multiple things in the system. Guaranties that the things
     * are deleted in SQL/NoSQL databases
     *
     * @param thingIds
     */
    public List<Map> secureDelete(
            List<Long> thingIds,
            Subject subject,
            boolean validateVisibility,
            boolean executeTickle,
            boolean fillSource,
            boolean secure,
            boolean processInMemory) {
        if (thingIds == null) {
            throw new UserException("Error deleting things, thingIds could not be null");
        }
        Date transactionDate = new Date();
        List<Long> groupMqtt = null;
        subject = (subject == null) ? SecurityUtils.getSubject() : subject;
        User currentUser = (User) subject.getPrincipal();
        List<Map> results = new ArrayList<>();
        List<Long> processedThings = new LinkedList<>();
        int operationId = 0;
        for (Long thingId : thingIds) {
            if (thingId == null) {
                throw new UserException("Error deleting thing, thingId could not be null");
            }
            operationId++;
            Map<String, Object> mapResponce = new HashMap();
            mapResponce.put("error", false);
            mapResponce.put("errorMessage", "");
            mapResponce.put("operationId", operationId);
            results.add(mapResponce);
            try {
                Stack<Long> recursivelyStack = new Stack<>();
                Thing thing = ThingService.getInstance().get(thingId);
                if (groupMqtt == null) {
                    groupMqtt = GroupService.getInstance().getMqttGroups(thing.getGroup());
                }
                delete(recursivelyStack,
                        thing,
                        validateVisibility, //validate Visibility
                        transactionDate,
                        false, //Execute Tickle
                        currentUser, //user executor
                        subject, //Subject
                        currentUser, //user logged
                        fillSource, // fillSource
                        false, //deleteMongoFlag
                        secure); //secure
                processedThings.add(thingId);
            } catch (Exception ex) {
                logger.error("Error deleting thing with id = " + thingId, ex);
                mapResponce.put("error", true);
                mapResponce.put("errorMessage", ex.getMessage());
                return results;
            }
        }
        commit();
        if (thingIds.size() > 0) {
            final List<Long> lambdaGroupMqtt = new LinkedList<>(groupMqtt);
            CompletableFuture.runAsync(() -> executeCompensationAlgorithm(processedThings, executeTickle, processInMemory, lambdaGroupMqtt));
        }
        return results;
    }

    /**
     * Deletes one thing in the system. Guaranties that the thing
     * is deleted in SQL/NoSQL databases
     *
     * @param thingId
     */
    public void secureDelete(
            Long thingId,
            Subject subject,
            boolean validateVisibility,
            boolean executeTickle,
            boolean fillSource,
            boolean secure,
            boolean processInMemory) {
        secureDelete(Arrays.asList(thingId), subject, validateVisibility, executeTickle, fillSource, secure, processInMemory);
    }

    /**
     * Deletes one thing in the system. Guaranties that the thing
     * is deleted in SQL/NoSQL databases
     *
     * @param thingId
     * @param validateVisibility
     * @param executeTickle
     * @param fillSource
     * @param secure
     */
    public void secureDelete(
            Long thingId,
            boolean validateVisibility,
            boolean executeTickle,
            boolean fillSource,
            boolean secure,
            boolean processInMemory) {
        secureDelete(Arrays.asList(thingId), validateVisibility, executeTickle, fillSource, secure, processInMemory);
    }

    /**
     * Executes a compensation delete in case of fail in middle of a transaction
     */
    private void executeInmemoryCompensationDelete(List<Long> processedThings) {
        if ((processedThings != null) && (!processedThings.isEmpty())) {
            for (Long id : processedThings) {
                try {
                    ThingMongoService.getInstance().deleteThing(id);
                    BackgroundProcessDetailLogService.getInstance().deleteOne(id);
                } catch (Exception e) {
                    throw new UserException("Error to delete the thing " + id + " in Mongo: " + e.getMessage(), e);
                }
            }
            logger.info("Quantity of things deleted on In Memory Compensation process: " + processedThings.size());
        }
    }

    /*private TimerUtil initCompensationDeleteTimerUtil() {
        List<String> laps = new ArrayList<>();
        laps.add("[Delete Process] : Get Things Pending to Delete");
        laps.add("[Delete Process] : Delete Pending in Mongo");
        laps.add("[Delete Process] : Delete Pending in MySQL");
        laps.add("[Delete Process] : Build Tickle Object");
        TimerUtil tu = new TimerUtil(true);
        String[] lapList = new String[laps.size()];
        lapList = laps.toArray(lapList);
        tu.initLaps(lapList);
        return tu;
    }*/

    /**
     * Executes a compensation delete in between SQL/NoSQL databases
     */
    private synchronized List<TickleItem> executeCompensationDelete() {
        FlushMode flushMode = HibernateSessionFactory.getInstance().getCurrentSession().getFlushMode();
        HibernateSessionFactory.getInstance().getCurrentSession().setFlushMode(FlushMode.COMMIT);
        ////////TimerUtil tu = initCompensationDeleteTimerUtil();
        ////////tu.start("[Delete Process] : Get Things Pending to Delete");
        List<BackgroundProcessDetailLog> lstIdsToDeleteObj = BackgroundProcessDetailLogService.getInstance().getThingsPendingToDelete();
        ////////tu.stop("[Delete Process] : Get Things Pending to Delete");
        List<TickleItem> tickleList = new ArrayList<>();
        for (int i = 0; i < lstIdsToDeleteObj.size(); i++) {
            BackgroundProcessDetailLog rpbpDetailLog = lstIdsToDeleteObj.get(i);
            try {
                ////////tu.start("[Delete Process] : Delete Pending in Mongo");
                ThingMongoService.getInstance().deleteThing(rpbpDetailLog.getThingId());
                ////////tu.stop("[Delete Process] : Delete Pending in Mongo");
                ////////tu.start("[Delete Process] : Delete Pending in MySQL");
                BackgroundProcessDetailLogService.getInstance().delete(rpbpDetailLog);
                ////////tu.stop("[Delete Process] : Delete Pending in MySQL");
                ////////tu.start("[Delete Process] : Build Tickle Object");
                TickleItem ti = new TickleItem();
                ti.setThingTypeCode(rpbpDetailLog.getThingTypeCode());
                ti.setSerialNumber(rpbpDetailLog.getSerialNumber());
                ////////tu.stop("[Delete Process] : Build Tickle Object");
                tickleList.add(ti);
            } catch (Exception e) {
                throw new UserException("Error to delete the thing " + rpbpDetailLog.getThingId() + " in Mongo: " + e.getMessage(), e);
            }
        }
        HibernateSessionFactory.getInstance().getCurrentSession().setFlushMode(flushMode);
        logger.info("Quantity of things deleted in compensation process: " + lstIdsToDeleteObj.size());
        ////////logger.info("Compensation Delete: " + tu.getLogString());
        return tickleList;
    }

    /*private TimerUtil initTicklesDeleteTimerUtil() {
        List<String> laps = new ArrayList<>();
        laps.add("[Delete Process] : Send Refresh tickle");
        laps.add("[Delete Process] : Send Delete tickle");
        TimerUtil tu = new TimerUtil(true);
        String[] lapList = new String[laps.size()];
        lapList = laps.toArray(lapList);
        tu.initLaps(lapList);
        return tu;
    }*/

    private void sendTickles(List<TickleItem> tickles, List<Long> groupMqtt) {
        if ((tickles != null) && (!tickles.isEmpty())) {
            ////////TimerUtil tu = initTicklesDeleteTimerUtil();
            for (int i = 0; i < tickles.size(); i++) {
                TickleItem ti = tickles.get(i);
                logger.info("Sending to refresh single thing message " + (i + 1) + ": " + ti.getSerialNumber());
                ////////tu.start("[Delete Process] : Send Refresh tickle");
                BrokerClientHelper.sendRefreshSingleThingMessage(ti.getThingTypeCode(), ti.getSerialNumber(), true, groupMqtt);
                ////////tu.stop("[Delete Process] : Send Refresh tickle");
                ////////tu.start("[Delete Process] : Send Delete tickle");
                BrokerClientHelper.sendDeleteThingMessage(ti.getThingTypeCode(), ti.getSerialNumber(), true, groupMqtt);
                ////////tu.stop("[Delete Process] : Send Delete tickle");
                ////////logger.info("Send tickles to Delete: " + tu.getLogString());
            }
        }
    }


    /**
     * Executes the compensation algorithm after system crash
     */
    private void executeCompensationAlgorithm(boolean executeTickle, boolean processInMemory, List<Long> groupMqtt) {
        executeCompensationAlgorithm(null, executeTickle, processInMemory, groupMqtt);
    }

    /**
     * Executes the compensation algorithm after system crash
     */
    private void executeCompensationAlgorithm(
            List<Long> processedThings, boolean executeTickle, boolean processInMemory, List<Long> groupMqtt) {
        startTransaction();
        List<TickleItem> tickleItems = null;
        try {
            if (processInMemory) {
                executeInmemoryCompensationDelete(processedThings);
            } else {
                tickleItems = executeCompensationDelete();
            }
        } catch (UserException e) {
            logger.error("Error occurred executing compensation algorithm", e);
            rollBack(e, false);
            throw new UserException("Error occurred executing compensation algorithm", e);
        }
        commit();
        if (executeTickle) {
            sendTickles(tickleItems, groupMqtt);
        }

    }

    public synchronized void runCompensationAlgorithmJob(boolean throwException, List<Long> groupMqtt) {
        Runnable compensationTask = () -> {
            try {
                logger.info("Starting compensation algorithm for thing delete after system crash");
                executeCompensationAlgorithm(true, false, groupMqtt);
                logger.info("Compensation algorithm after system crash was executed successfully");
            } catch (Exception e) {
                logger.error("Fail executing compensation algorithm after system crash", e);
                if (throwException) {
                    throw e;
                }
            }
        };
        if (!throwException) {
            new Thread(compensationTask).start(); //async execution
        } else {
            compensationTask.run();//sync execution
        }
    }

    /**
     * This method updates in bulk things according to incoming conditions
     *
     * @param whereThing
     * @param whereFieldValue
     * @param udfs
     * @param storageDate
     * @param async
     * @param asyncBatchSize
     * @param bridgeCode
     */
    public Map updateThingsByConditions(Stack<Long> recursivelyStack,
                                        String whereThing,
                                        String whereFieldValue,
                                        Map<String, Object> udfs,
                                        Date storageDate,
                                        User currentUser,
                                        boolean fillSource,
                                        boolean async,
                                        int asyncBatchSize,
                                        String bridgeCode) {

        List<String> filterFields = new ArrayList<>();
        filterFields.add("*");
        List<Map<String, Object>> things = ThingMongoDAO.getInstance().listThingsByFieldValues(filterFields, null, whereFieldValue, whereThing);

        Map<String, Object> resultUpdate = new HashMap<>();
        List<Map<String, Object>> resultUpdateList = new ArrayList<>();
        int count = 0;

        if (async) {
            for (Map<String, Object> thing : things) {

                Map<String, Object> thingTypeFieldTickle = new HashMap<>();
                for (Map.Entry<String, Object> field : udfs.entrySet()) {
                    if (!ThingType.NonUDF.getEnums().contains(field.getKey())) {
                        thingTypeFieldTickle.put(field.getKey(), ((Map) field.getValue()).get("value"));
                    }
                }

                List<Long> groupMqtt = null;
                if (thing.get("groupId") != null) {
                    Group group = GroupService.getInstance().get(Long.parseLong(thing.get("groupId").toString()));
                    groupMqtt = GroupService.getInstance().getMqttGroups(group);
                }
                ThingService.getInstance().executeTickle(thing.get("thingTypeCode").toString(), thing.get("serialNumber").toString(),
                        thingTypeFieldTickle, storageDate, false, groupMqtt);
                //If batch size or last things list element reached send tickles
                if (asyncBatchSize == 0 || (things.indexOf(thing) > 0 && things.indexOf(thing) % asyncBatchSize == 0) || things.indexOf(thing) == things.size() - 1) {
                    Object tickleResponse = BrokerClientHelper.publishTickle(Thread.currentThread().getName(), bridgeCode);

                    if (tickleResponse != null) {
                        logger.info("Tickles sent:\n" + tickleResponse);
                    } else {
                        logger.warn("No tickles sent for threadName=" + Thread.currentThread().getName() + " and bridgeCode=" + bridgeCode);
                    }
                }
                Map<String, Object> result = new HashMap<>();
                Map<String, Object> thingMap = new HashMap<>();
                thingMap.put("serial", thing.get("serialNumber"));
                thingMap.put("modifiedTime", thing.get("modifiedTime"));
                thingMap.put("name", thing.get("name"));
                thingMap.put("id", thing.get("_id"));
                thingMap.put("createdTime", thing.get("createdTime"));
                thingMap.put("activated", thing.get(null));
                result.put("thing", thingMap);
                result.put("time", storageDate.getTime());
                result.put("fields", thingTypeFieldTickle);
                result.put("async", async);

                resultUpdateList.add(result);
                count++;
            }


        } else {
            for (Map<String, Object> thing : things) {
                Map<String, Object> result = new HashMap<>();
                try {
                    Group thingGroup = GroupService.getInstance().get(Long.parseLong(thing.get("groupId").toString()));
                    result = ThingService.getInstance().update(
                            recursivelyStack
                            , ThingService.getInstance().get(Long.parseLong(thing.get("_id").toString()))
                            , (String) thing.get("thingTypeCode")
                            , thingGroup.getHierarchyName(false)
                            , (String) thing.get("name")
                            , (String) thing.get("serialNumber")
                            , null
                            , udfs
                            , null
                            , null
                            , true //executeTickle
                            , true //validateVisibility
                            , storageDate
                            , false //disableFMCLogic
                            , null //validations
                            , null //cache
                            , false //updateAndFlush
                            , true
                            , currentUser
                            , fillSource
                    );


                } catch (UserException e) {
                    result.put("ERROR", e.getMessage());
                }
                result.put("async", async);
                resultUpdateList.add(result);
                count++;
            }
        }


        resultUpdate.put("total", count);
        resultUpdate.put("result", resultUpdateList);

        return resultUpdate;

    }

    /********************************************************
     * Validate the data of thing and Udf's to update the Thing
     *********************************************************/
    public ValidationBean validateUpdateThing(ThingType thingType,
                                              String groupHierarchyCode,
                                              String name,
                                              String serialNumber,
                                              Object parent,
                                              Map<String, Object> udfs,
                                              Object children,
                                              Long thingId,
                                              boolean validateVisibility,
                                              Map<String, Boolean> validations,
                                              Date transactionDate,
                                              User currentUser) {
        ValidationBean validationBean = new ValidationBean();
        List<String> messages = new ArrayList<>();
        Group group = null;

        try {
            // validate and get group
            if (validations == null || validations.get("group")) {
                // If groupHierarchyCode is null, we will get the group of the ThingType
                if (groupHierarchyCode == null) {
                    groupHierarchyCode = thingType.getGroup().getHierarchyName(false);
                }
                Map<String, Object> groupResultValidation = validateAndGetGroup(groupHierarchyCode, validateVisibility, currentUser);
                validationBean = (ValidationBean) groupResultValidation.get("validationBean");
                if (validationBean.isError()) {
                    messages.add(validationBean.getErrorDescription());
                }
                group = (Group) groupResultValidation.get("group");
                //Visibility
                if (validateVisibility && null != group) {
                    ValidationBean validationBean1 = validateVisibility(thingType, group, false);
                    if (validationBean1.isError()) {
                        messages.add(validationBean1.getErrorDescription());
                    }
                }
            }

            // validate if thing exists
            if (validations == null || validations.get("thing.exists")) {
                if ((thingId == null) || (ThingService.getInstance().get(thingId) == null)) {
                    messages.add("The thing to update does not exist.");
                }
            }

            if (name != null && name.trim().equals("")) {
                messages.add("Name must have a value. ");
            }

            // serial validation
            if (validations == null || validations.get("thing.serial")) {
                ValidationBean vb = this.validateSerial(thingType, serialNumber, thingId);
                if (serialNumber != null && vb.isError()) {
                    messages.add(vb.getErrorDescription());
                }
            }

            if (validations == null || validations.get("thing.parent")) {
            /*Validate Parent*/
                if (parent != null && parent instanceof Map && !((Map<String, Object>) parent).isEmpty()) {
                    if (ThingTypeMapService.getInstance().isParent(thingType)) {
                        messages.add("The new thing type is of the level parent and it cannot accept a parent thing " +
                                "type");
                    } else {
                        if (!this.isValidThing((Map<String, Object>) parent)) {
                            messages.add("Parent with serial:" + ((Map<String, Object>) parent).get("serialNumber")
                                    + " does not exist");
                        }
                    }
                }
            }

            if (validations == null || validations.get("thing.children")) {
            /*Validate children*/
                //TODO verify is children a List or a Object[] or a List<Map<String, Object>>
                if (children != null && null != group && children instanceof Object[] && ((Object[]) children).length
                        > 0) {
                    if (ThingTypeMapService.getInstance().isChild(thingType)) {
                        messages.add("The new thing type is of the level child and it cannot accept children thing " +
                                "types");
                    } else {
                        ValidationBean validaChildren = validateChildren(
                                children, group, validateVisibility, transactionDate);
                        if (validaChildren.isError()) {
                            messages.add(validaChildren.getErrorDescription());
                        }
                    }
                }
            }

            if (validations == null || validations.get("thing.udfs")) {
                /*Validate ThingTypeFields*/
                if (udfs != null && group != null) {
                    ValidationBean valThingTypeFields = ThingsService.getInstance().validationUdfs(thingType, udfs, group, validateVisibility,
                            transactionDate);
                    if (valThingTypeFields.isError()) {
                        messages.add(valThingTypeFields.getErrorDescription());
                    }
                }
            }
            /*Validate duplicity in labels of UDf's*/
            /*Assign parent or children, but not both */
            if (Utilities.isNotEmpty(messages)) {
                validationBean.setErrorDescription(Strings.join(messages, ","));
            }
        } catch (Exception e) {
            throw new UserException("Error in validation" + e.getMessage(), e);
        }
        return validationBean;
    }


    /**
     * Validate the data of thing and Udf's to delete the Thing
     */
    public ValidationBean validateDeleteThing(Thing thing, boolean validateVisibility, Subject subject) {
        ValidationBean validationBean = new ValidationBean();
        List<String> messages = new ArrayList<String>();

        try {
            if (thing == null) {
                messages.add("The thing to update does not exist.");
            }
            //Visibility
            if (validateVisibility) {
                limitVisibilityDeleteT(thing, subject);
            }

            if (!messages.isEmpty()) {
                validationBean.setErrorDescription(StringUtils.join(messages, ","));
            }
        } catch (Exception e) {
            throw new UserException("Error in validation" + e.getMessage(), e);
        }

        return validationBean;
    }

    /**************************************************
     * This method validates if a parent is valid or not
     ***************************************************/
    public boolean isValidThing(Map<String, Object> thingMap) {
        boolean response = false;
        try {
            if (thingMap.get("serialNumber") != null) {
                ThingType thingType = ThingTypeService.getInstance().getByCode((String) thingMap.get("thingTypeCode"));
                Thing thing = getBySerialNumber((String) thingMap.get("serialNumber"), thingType);
                if (thing != null) {
                    response = true;
                }
            }
        } catch (Exception e) {
            response = false;
        }
        return response;
    }

    /*****************************************************
     * This method validate if a group is valid or not
     ******************************************************/
    public Map validateAndGetGroup(String groupHierarchyCode, boolean validateVisibility, User currentUser) {
        logger.debug("val: " + groupHierarchyCode);
        Map<String, Object> result = new HashMap<String, Object>();
        Group group = null;
        ValidationBean validationBean = new ValidationBean();
        try {
            if ((groupHierarchyCode == null) || groupHierarchyCode.trim().isEmpty()) {
                validationBean.setErrorDescription("Invalid Group");
            } else {
                group = GroupService.getInstance().getByHierarchyCode(groupHierarchyCode);
                if (null == group) {
                    validationBean.setErrorDescription("Invalid Group");
                } else {
                    if (validateVisibility) {
                        logger.debug("Usuario" + currentUser.getId() + "-" + currentUser.getFirstName());
                        Long userLevel = ConfigurationService.getAsLong(currentUser, group, "thing");
                        int thingLevel = group.getTreeLevel();
                        if ((userLevel != null) && (userLevel.intValue() != thingLevel)) {
                            validationBean.setErrorDescription("The group's level is " + thingLevel + " and it has to be " + userLevel);
                        }
                    }
                }
            }
        } catch (NonUniqueResultException e) {
            validationBean.setErrorDescription("Non Unique Result in Group name: " + groupHierarchyCode);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage(), e);
        }
        result.put("group", group);
        result.put("validationBean", validationBean);
        return result;
    }

    /*****************************************************
     * This method validate if a serial is valid or not
     ******************************************************/
    public ValidationBean validateSerial(ThingType thingType, Object serial, Long thingId) {
        ValidationBean response = new ValidationBean();
        if (serial == null) {
            response.setErrorDescription("Serial must have a value. ");
        } else {
            if ((serial).toString().trim().isEmpty()) {
                response.setErrorDescription("Serial must have a value.");
            } else {
                if (!Utilities.isAlphaNumericCharacterSpecials(serial.toString(), "_")) {
                    response.setErrorDescription("Serial has invalid characters, only alphanumeric characters are allowed.");
                } else {
                    try {
                        if (isExistingSerial(thingType.getId(), serial, thingId)) {
                            response.setErrorDescription(
                                    String.format("Serial '[%s]' already exist for Thing Type '%s' ", serial, thingType.getName()));
                        }
                    } catch (Exception e) {
                        response.setErrorDescription("Validate serial > " + e.getMessage());
                    }

                }
            }
        }
        return response;
    }

    /****************************************************
     * this metod Checks if the serial is existent or not
     ***************************************************/
    private boolean isExistingSerial(Long thingTypeId, Object serial, Long excludeId) {
        boolean response = false;
        try {
            if (excludeId != null && this.existsSerial(serial.toString(), thingTypeId, excludeId)) {
                response = true;
            } else if (excludeId == null && this.existsSerial(serial.toString(), thingTypeId)) {
                response = true;
            }
        } catch (Exception e) {
            response = false;
        }
        return response;
    }

    /**
     * Validate Thing Type code
     */
    public Map<String, Object> validateAndGetThingType(String thingTypeCode) {
        Map<String, Object> response = new HashMap<String, Object>();
        ValidationBean validationBean = new ValidationBean(false);
        ThingType thingType = null;
        if (thingTypeCode == null || thingTypeCode.isEmpty()) {
            validationBean.setErrorDescription("Invalid Thing Type. ");
        } else {
            try {
                thingType = ThingTypeService.getInstance().getByCode(thingTypeCode);
                if (thingType == null) {
                    validationBean.setErrorDescription("Thing Type with code:" + thingTypeCode + " does not exist");
                }
            } catch (Exception e) {
                validationBean.setErrorDescription("Invalid Thing Type. ");
            }
        }
        response.put("thingType", thingType);
        response.put("validationBean", validationBean);
        return response;
    }

    /**
     * Validate visibility of the group and thingType
     */
    public ValidationBean validateVisibility(ThingType thingType, Group group) {
        ValidationBean response = new ValidationBean();
        try {
            VisibilityThingUtils.limitVisibilityInsertT(thingType, group);
        } catch (Exception e) {
            response.setErrorDescription("Visibility error: " + e.getMessage());
        }
        return response;
    }

    /**
     * Validate visibility for create or update of the group and thingType
     */
    public ValidationBean validateVisibility(ThingType thingType, Group group, boolean validateInsert) {
        ValidationBean response = new ValidationBean();
        try {
            if (validateInsert) {
                VisibilityThingUtils.limitVisibilityInsertT(thingType, group);
            } else {
                VisibilityThingUtils.limitVisibilityUpdateT(thingType, group);
            }
        } catch (Exception e) {
            response.setErrorDescription("Visibility error: " + e.getMessage());
        }
        return response;
    }


    /**
     * Validate children of the thing
     */
    public ValidationBean validateChildren(
            Object children,
            Group group,
            boolean validateVisibility,
            Date transactionDate) {
        ValidationBean response = new ValidationBean();
        try {
            for (Object child : (List) children) {
                Map<String, Object> childData = (Map<String, Object>) child;
                if (!this.isValidThing(childData)) {
                    response.setErrorDescription("Child with serial number:" + childData.get("serialNumber") + " does not exist");
                    break;
                } else {
                    ThingType thingType = ThingTypeService.getInstance().getByCode((String) childData.get("thingTypeCode"));
                    Thing childThing = getBySerialNumber((String) childData.get("serialNumber"), thingType);
                    if (getFirstChild(childThing) != null) {
                        response.setErrorDescription("The thing [ " + childThing.getId() + " ] is a parent.");
                        break;
                    }
                    String thingTypeCode = (String) childData.get("thingTypeCode");
                    ThingType thingTypeChild = null;
                    if (thingTypeCode != null) {
                        thingTypeChild = ThingTypeService.getInstance().getByCode(thingTypeCode.toString());
                        if (thingTypeChild == null) {
                            throw new UserException("Error getting thingType with code: " + thingTypeCode);
                        }
                    }
                    //Check Udf's of the Children
                    ValidationBean valUdfs = ThingsService.getInstance().validationUdfs(
                            thingTypeChild,
                            (Map<String, Object>) childData.get("udfs"),
                            group,
                            validateVisibility,
                            transactionDate);
                    if (valUdfs.isError()) {
                        response.setErrorDescription(valUdfs.getErrorDescription());
                        break;
                    }
                }

            }
        } catch (NonUniqueResultException e) {
            response.setErrorDescription("Validate Children error: " + e.getMessage());
        }
        return response;
    }

    /**
     * Validation of UDF's
     */
    public ValidationBean validationUdfs(
            ThingType thingType,
            Map<String, Object> udfs,
            Group group,
            boolean validateVisibility,
            Date transactionDate) {
        ValidationBean response = new ValidationBean();
        if (udfs != null) {
            //Get the definition of the thing
            Map<String, Object> udfsData = udfs;
            Iterator it = udfsData.entrySet().iterator();
            //Iterate Udf's data
            while (it.hasNext()) {
                Map.Entry udfObject = (Map.Entry) it.next();
                int cont = 0;
                //Iterate fields of the config of the thing type
                for (ThingTypeField thingTypeField : thingType.getThingTypeFields()) {
                    String udfLabel = "";
                    if (udfObject != null) {
                        udfLabel = udfObject.getKey().toString().trim();
                    }
                    DataType dataType = DataTypeService.getInstance().get(thingTypeField.getDataType().getId());
                    if (thingTypeField.getName().equals(udfLabel)) {
                        if (udfObject != null && thingTypeField.getDataType().getTypeParent().equals(
                                ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value)
                                && dataType.getType().equals(ThingTypeField.TypeParentSubGroup.TYPE_PARENT_DATA_TYPE_STANDARD_DATA.value)) {
                            response = validationStandardDataTypes(thingTypeField, udfLabel, udfObject);
                            break;
                        } else if (udfObject != null && thingTypeField.getDataType().getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value)
                                && dataType.getType().equals(ThingTypeField.TypeParentSubGroup.TYPE_PARENT_DATA_TYPE_NATIVE_OBJECT.value)) {
                            response = validationNativeObject(
                                    thingTypeField, udfLabel, udfObject, group, validateVisibility, transactionDate);
                            break;
                        } else if (udfObject != null && thingTypeField.getDataType().getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_NATIVE_THING_TYPE.value)) {
                            response = validationNativeThingType(thingTypeField, udfLabel, udfObject, group, validateVisibility);
                            break;
                        }
                    } else {
                        cont++;
                    }
                }
                if (response.isError()) {
                    break;
                }
                if (cont == thingType.getThingTypeFields().size() && udfObject != null && !StringUtils.equals(Constants.UDF_SOURCE, udfObject.getKey().toString())) {
                    response.setErrorDescription("UDF Thing Type: " + udfObject.getKey().toString() + " does not exist.");
                    break;
                }
            }
        }
        return response;
    }

    /************************************
     * this method checks if the Standard data type is valid
     * This method was deprecated on 2016-12-15
     * This method was deprecated on Vizix: 4.5.0_RC4
     * This method is duplicated in ThingsService
     ************************************/
    @Deprecated
    public ValidationBean validationStandardDataTypes(ThingTypeField thingTypeField, String udfLabel, Map.Entry udfValueObject) {
        ValidationBean response = new ValidationBean();

        try {
            Map<String, Object> udfMap = (Map<String, Object>) udfValueObject.getValue();
            Object udfValue = udfMap.get("value");
            //Check Multivalue, Array of names
            if (udfValue instanceof String[]) {
                String[] udfValueName = (String[]) udfValue;
                if (udfValueName.length > 0) {
                    for (int i = 0; i < udfValueName.length; i++) {
                        if (!this.isStandardValidDataType(thingTypeField.getDataType(), udfValueName[i])) {
                            response.setErrorDescription("UDF Standard Data type: '" + udfLabel + "', the value :'" + Arrays.toString(udfValueName) + "' does not correspond to the type of data.");
                            break;
                        }
                    }
                }
                //Check single name
            } else {
                if (!this.isStandardValidDataType(thingTypeField.getDataType(), udfValue)) {
                    response.setErrorDescription(
                            "UDF Standard Data type: '" + udfLabel + "', the value: '" + udfValue + "' does not correspond to the type of data '"
                                    + thingTypeField.getDataType().getClazz() + "'.");
                } else {
                    if (thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_ATTACHMENTS.value) == 0 &&
                            !this.isJSONValid(udfValue)) {
                        response.setErrorDescription("UDF Standard Data type: " + udfLabel + ", the value: '" + udfValue
                                + "' does not have a correct format.");
                    } else if ((com.tierconnect.riot.commons.DataType.XYZ.equals(thingTypeField.getDataType().getCode()) ||
                            com.tierconnect.riot.commons.DataType.COORDINATES.equals(thingTypeField.getDataType().getCode())) &&
                            !AppLocationAdapter.isValidLocation(udfMap, thingTypeField.getDataType().getCode())) {
                        if (com.tierconnect.riot.commons.DataType.COORDINATES.equals(thingTypeField.getDataType().getCode())) {
                            response.setErrorDescription(udfLabel + ": '" + udfValue + "' is invalid, the correct format is: " +
                                    "Longitude;Latitude;Altitude with number data, please check it and try again.");
                        } else {
                            response.setErrorDescription(udfLabel + ": '" + udfValue + "' is invalid, the correct format is: " +
                                    "X;Y;Z with number data, please check it and try again.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            response.setErrorDescription(e.getMessage());
        }

        return response;
    }

    /************************************
     * this method checks if the Native Object is valid
     * This method was deprecated on 2016-12-15
     * This method was deprecated on Vizix: 4.5.0_RC4
     * This method is duplicated in ThingsService
     ************************************/
    @Deprecated
    public ValidationBean validationNativeObject(
            ThingTypeField thingTypeField,
            String udfLabel,
            Map.Entry udfValueObject,
            Group group,
            boolean validateVisibility,
            Date transactionDate) {
        ValidationBean response = new ValidationBean();
        try {
            Map<String, Object> udfMap = (Map<String, Object>) udfValueObject.getValue();
            Object udfValue = udfMap.get("value");
            //Check Multivalue, Array of names
            if (udfValue != null && udfValue instanceof String[]) {
                String[] udfValueName = (String[]) udfValue;
                if (udfValueName.length > 0) {
                    for (int i = 0; i < udfValueName.length; i++) {
                        Object nativeObject = ThingTypeFieldService.getInstance().getNativeObject(thingTypeField.getDataType().getId(),
                                Arrays.toString(udfValueName), null, group, transactionDate);
                        if (nativeObject == null) {
                            response.setErrorDescription("UDF Native Object: '" + udfLabel + "', the value does not exist.");
                            break;
                        } else {
                            if (validateVisibility && validationVisibilityUdf(nativeObject, group).isError()) {
                                response.setErrorDescription("UDF Native Object: '" + udfLabel + "', the value :'" + Arrays.toString(udfValueName)
                                        + "' Wrong visibility." + validationVisibilityUdf(nativeObject, group).getErrorDescription());
                            }
                        }
                    }
                }
                //Check single name
            } else if (udfValue != null && udfValue instanceof String) {
                Object nativeObject = ThingTypeFieldService.getInstance().getNativeObject(
                        thingTypeField.getDataType().getId(),
                        udfValue.toString(),
                        null, group,
                        transactionDate);
                if (nativeObject == null) {
                    response.setErrorDescription("UDF Native Object: " + udfLabel + ". The value does not exist.");
                }
            }
        } catch (Exception e) {
            response.setErrorDescription(e.getMessage());
        }

        return response;
    }

    /**
     * this method checks if the Native Objec or
     * Things UDFs are with the correct Visibility
     */
    public ValidationBean validationVisibilityUdf(Object nativeObject, Group groupUser) {
        ValidationBean response = new ValidationBean();
        try {
            EntityVisibility entityVisibility = null;
            if (nativeObject instanceof Group) {
                entityVisibility = (new GroupController()).getEntityVisibility();
            } else if (nativeObject instanceof Shift) {
                entityVisibility = (new ShiftController()).getEntityVisibility();
            } else if (nativeObject instanceof LogicalReader) {
                entityVisibility = (new LogicalReaderController()).getEntityVisibility();
            } else if (nativeObject instanceof Zone) {
                entityVisibility = (new ZoneController()).getEntityVisibility();
            } else if (nativeObject instanceof Thing) {
                entityVisibility = (new ThingController()).getEntityVisibility();
            }

            GeneralVisibilityUtils.limitVisibilitySelect(entityVisibility, nativeObject, groupUser);
        } catch (ForbiddenException e) {
            response.setErrorDescription(e.getMessage());
        }
        return response;
    }

    /************************************
     * this method checks if the Native Object is valid
     * This method was deprecated on 2016-12-15
     * This method was deprecated on Vizix: 4.5.0_RC4
     * This method is duplicated in ThingsService
     ************************************/
    @Deprecated
    public ValidationBean validationNativeThingType(ThingTypeField thingTypeField, String udfLabel, Map.Entry udfValueObject,
                                                    Group group, boolean validateVisibility) {
        ValidationBean response = new ValidationBean();

        try {
            Map<String, Object> udfMap = (Map<String, Object>) udfValueObject.getValue();
            Object udfValue = udfMap.get("value");//serial
            //Check Multivalue, Array of names
            if (udfValue instanceof String[]) {
                String[] udfValueName = (String[]) udfValue;
                if (udfValueName.length > 0) {
                    for (int i = 0; i < udfValueName.length; i++) {
                        Thing thing = getBySerialNumber(Arrays.toString(udfValueName), ThingTypeService.getInstance().get(
                                thingTypeField.getDataType().getId()));
                        if (thing == null) {
                            response.setErrorDescription("UDF Thing Type: " + udfLabel + ", the value :" + Arrays.toString(udfValueName) + " does not exist.");
                            break;
                        }
                    }
                }
                //Check single name
            } else if (udfValue != null && udfValue instanceof String) {
                Thing thing = getBySerialNumber(udfValue.toString().trim(),
                        ThingTypeService.getInstance().get(thingTypeField.getDataTypeThingTypeId()));
                if (thing == null) {
                    response.setErrorDescription("UDF Thing Type: " + udfLabel + ", the value :" + udfValue + " does not exist.");
                } else {
                    if (validateVisibility && validationVisibilityUdf(thing, group).isError()) {
                        response.setErrorDescription(
                                "UDF Thing Type: " + udfLabel + ", the value :" + udfValue + " Wrong visibilitys."
                                        + validationVisibilityUdf(thing, group).getErrorDescription());
                    }
                }
            }

        } catch (NonUniqueResultException e) {
            response.setErrorDescription("Non Unique Result in Udf:  " + udfLabel);
        } catch (Exception e) {
            response.setErrorDescription("Error validating Udf:  " + udfLabel);
        }

        return response;
    }

    /**
     * This method validates if the user wants to create more than the number permitted
     */
    public ValidationBean validateMaxNumberOfThings() {
        ValidationBean validationBean = new ValidationBean();
        User user = (User) SecurityUtils.getSubject().getPrincipal();
        if (LicenseService.enableLicense) {
            LicenseDetail licenseDetail = LicenseService.getInstance().getLicenseDetail(user.getActiveGroup(), true);
            Long maxNumberOfThings = licenseDetail.getMaxThings();
            if (maxNumberOfThings != null && maxNumberOfThings > 0) {
                Long countAll = count(licenseDetail);
                if (countAll >= maxNumberOfThings) {
                    validationBean.setErrorDescription("You have reached the limit (" + maxNumberOfThings + ") to create things.");
                }
            }
        }
        return validationBean;
    }

    /*************************************
     * Method to get the quantity of things created by license
     ************************************/
    public Long count(LicenseDetail licenseDetail) {
        GroupService groupService = GroupService.getInstance();
        ThingDAO thingDAO = ThingService.getThingDAO();
        Long countAll;
        Group licenseGroup = groupService.get(licenseDetail.getGroupId());
        boolean isRootLicense = groupService.getRootGroup().getId().equals(licenseGroup.getId());
        if (isRootLicense) {
            countAll = thingDAO.countAll(null);
        } else {
            countAll = thingDAO.countAll(QThing.thing.group.parentLevel2.id.eq(licenseGroup.getParentLevel2().getId()));
        }
        return countAll;
    }

    /*****************************************************
     * Method to check if the standard data type is valid or not
     *******************************************************/
    public boolean isStandardValidDataType(DataType type, Object value) {
        boolean response = true;
        if (value != null && !value.toString().trim().equals("")) {
            Object valueType = getStandardDataType(type, value);
            response = (valueType != null);
        }
        return response;
    }

    /*****************************************************
     * Get Standard Data Type
     *******************************************************/
    public Object getStandardDataType(DataType type, Object value) {
        Object response = null;
        if (value != null) {
            try {
                if (Class.forName(type.getClazz()) == Boolean.class) {
                    Boolean data = Boolean.parseBoolean(value.toString());
                    response = data;
                } else if (Class.forName(type.getClazz()) == BigDecimal.class) {
                    value = value.toString().trim().equals("") ? "0" : value;
                    BigDecimal data = new BigDecimal(value.toString());
                    response = data.doubleValue();

                } else if (Class.forName(type.getClazz()) == Long.class) {
                    value = value.toString().trim().equals("") ? "0" : value;
                    Long data = (new BigDecimal(value.toString())).longValue();
                    response = data;
                } else if (Class.forName(type.getClazz()) == Date.class) {
                    Date data = null;
                    if (value.toString().matches("-?\\d+(\\.\\d+)?")) {
                        data = new Date(Long.parseLong(value.toString()));
                    } else {
                        data = DateHelper.getDateAndDetermineFormat(value.toString());
                    }
                    response = data;
                } else {
                    response = value.toString().trim();
                }
            } catch (Exception e) {
                response = null;
            }
        }
        return response;
    }

    /*****************************************************
     * Method to check if the JSON String has a valid format
     *******************************************************/
    public boolean isJSONValid(Object value) {
        boolean response = true;
        if (value != null && !value.toString().trim().equals("")) {
            try {
                //JSONObject json = (JSONObject)new JSONParser().parse("{\"name\":\"MyNode\", \"width\":200, \"height\":100}");
                JSONObject json = (JSONObject) new JSONParser().parse(value.toString());
            } catch (Exception e) {
                response = false;
            }
        } else {
            response = false;
        }
        return response;
    }


    /******************************************
     * This method executes the logic for FMC
     ******************************************/
    public void executeFMCLogic(
            Thing thing,
            Map<String, Object> thingTypeFields, Thing parent,
            Thing oldParent,
            List<Thing> newChildrenIds,
            List<Thing> deleteChildrenIds,
            Date storeDate, User user)
    {
        //THIS IS A VERY BAD HACK
        Group fmcGroup = ThingController.fmcGroup.get() != null ? GroupService.getInstance().get(ThingController.fmcGroup.get()) : null;
        ThingController.fmcGroup.set(null);

        //Update Tag
        if(thingTypeFields!=null && thingTypeFields.size()>0)
        {
            FMCUtils.fmcHandleUpdateUdfs( thing , thingTypeFields, storeDate, user);
        }

        //Update information of the parent, associate new parent and disassociate old parent in FMC
        if(parent!=null && oldParent!=null && parent.getId().compareTo( oldParent.getId() ) !=0 )
        {
            FMCUtils.fmcHandleUnAssignTag( oldParent, thing, fmcGroup, storeDate, user );
            FMCUtils.fmcHandleAssignTag( parent, thing, fmcGroup, storeDate, user );
        } else if(parent!=null && oldParent==null )
        {
            FMCUtils.fmcHandleAssignTag( parent, thing, fmcGroup, storeDate, user );
        } else if (parent == null && oldParent != null) {
            FMCUtils.fmcHandleUnAssignTag( oldParent, thing, fmcGroup, storeDate, user );
        }

        //Associate New Children in FMC
        if (newChildrenIds != null && newChildrenIds.size() > 0) {
            for (Thing newThing : newChildrenIds) {
                FMCUtils.fmcHandleAssignTag(thing, newThing, fmcGroup, storeDate, user);
            }
        }

        //Disassociate New Children in FMC
        if (deleteChildrenIds != null && deleteChildrenIds.size() > 0) {
            for (Thing deleteThing : deleteChildrenIds) {
                FMCUtils.fmcHandleUnAssignTag(thing, deleteThing, fmcGroup, storeDate, user);
            }
        }
    }

    /******************************************
     * This method executes Tickle
     ******************************************/
    public void executeTickle(
            String thingTypeCode,
            String serial,
            Map<String, Object> thingTypeFieldTickle,
            Date storage,
            boolean publishMessage,
            List<Long> groupMqtt) {
        logger.debug("calling executeTickle: serial=" + serial + ", thingTypeCode=" + thingTypeCode + ", date=" + storage.toString());
        BrokerClientHelper.sendRefreshSingleThingMessage(thingTypeCode, serial, publishMessage, groupMqtt);
        executeThingFieldTickle(thingTypeCode, serial, thingTypeFieldTickle, storage, groupMqtt);
    }

    public void executeThingFieldTickle(
            String thingTypeCode,
            String serial,
            Map<String, Object> thingTypeFieldTickle,
            Date storage,
            List<Long> groupMqtt) {

        if (thingTypeFieldTickle == null) {
            thingTypeFieldTickle = new HashMap<>();
        }
        //If thingFieldTickle doesn't contain source force to send tickle with source
        if (!thingTypeFieldTickle.containsKey("source")) {
            thingTypeFieldTickle.put("source", Constants.SOURCE_SERVICE);
        }

        if (thingTypeFieldTickle != null) {
            if (thingTypeFieldTickle.size() != 0) {
                // Beginning thingFiled tickle
                BrokerClientHelper.initThingFieldTickle(thingTypeCode, groupMqtt);

                Iterator it = thingTypeFieldTickle.entrySet().iterator();
                //Iterate Udf's data
                while (it.hasNext()) {
                    Map.Entry field = (Map.Entry) it.next();
                    String value = null;
                    if (field.getValue() != null) {
                        value = field.getValue().toString().trim();
                    } else {
                        value = "";
                    }
                    // BrokerClientHelper.setThingField(serial, storage.getTime(),field.getKey().toString().trim(), "\""+value+"\"");
                    BrokerClientHelper.setThingField(serial, storage.getTime(), field.getKey().toString().trim(), value);
                }

                // Ending thingField tickle sending just one message with all changed fields
                BrokerClientHelper.sendThingFieldTickle();
            }
        }
    }


    /**
     * This method builds the map to send Mongo so as to.
     *
     * @param thingId   Thing Id to create mongo Map.
     * @param thingType Thing Type.
     * @param udfs      Thing Type Fields of a Thing.
     * @param storeDate Store Date.
     * @param cache     cache to generate Mongo Thing.
     * @param group     Group of mongo thing.
     * @return Map Mongo of a thing or exception to create Mongo fields.
     */
    public Map<String, Object> getUdfMapForMongo(Long thingId,
                                                 DBObject thingMongo,
                                                 ThingType thingType,
                                                 Object udfs,
                                                 Date storeDate,
                                                 Map<String, Map<String, Object>> cache,
                                                 Group group,
                                                 boolean fillSource) {

        Map<String, Object> response = new HashMap<>();
        Map<String, Object> thingTypeFieldResponse = new HashMap<>();
        Map<String, Object> thingTypeFieldTickle = new HashMap<>();
        boolean timeSeries = false;

        try {
            if (udfs != null) {
                Map<String, Object> typeFieldsMapAux = (Map<String, Object>) udfs;
                List<ThingTypeField> thingTypeFields;
                if (fillSource) {
                    thingTypeFields = ThingTypeFieldService.getInstance().getThingTypeFieldByNameAndTypeCode(Constants.UDF_SOURCE, thingType.getThingTypeCode());
                    if (thingTypeFields != null && !thingTypeFields.isEmpty() &&
                            (typeFieldsMapAux.get(Constants.UDF_SOURCE) == null || ((Map) typeFieldsMapAux.get(Constants.UDF_SOURCE)).isEmpty())) {
                        LinkedHashMap<Object, Object> valueSourceDefault = new LinkedHashMap<>();
                        valueSourceDefault.put("value", Constants.SOURCE_SERVICE);
                        typeFieldsMapAux.put(Constants.UDF_SOURCE, valueSourceDefault);
                    }
                }

                for (Map.Entry<String, Object> udfObject : typeFieldsMapAux.entrySet()) {
                    Map<String, Object> udfField = new HashMap<>();
                    String udfLabel = udfObject.getKey().trim();

                    //Iterate fields of the config of the thing type
                    thingTypeFields = new ArrayList<>();
                    if (cache != null) {
                        for (Object thingTypeField : cache.get("thingTypeField").values()) {
                            if (((ThingTypeField) thingTypeField).getName().equals(udfLabel) &&
                                    ((ThingTypeField) thingTypeField).getThingType().getThingTypeCode().equals(
                                            thingType.getThingTypeCode()))
                                thingTypeFields.add((ThingTypeField) thingTypeField);
                        }
                    } else {
                        thingTypeFields = ThingTypeFieldService.getInstance().getThingTypeFieldByNameAndTypeCode(
                                udfLabel,
                                thingType.getThingTypeCode());
                    }

                    if (thingTypeFields.size() > 0) {
                        ThingTypeField thingTypeField = thingTypeFields.get(0);

                        timeSeries = timeSeries || thingTypeField.getTimeSeries();

                        for (Map.Entry<String, Object> udfProperties : ((Map<String, Object>) udfObject.getValue())
                                .entrySet()) {
                            if (!udfProperties.getKey().equals("value")) {
                                udfField.put(udfProperties.getKey(), udfProperties.getValue());
                            }
                        }

                        Map<String, Object> udfMap = (Map<String, Object>) udfObject.getValue();
                        Object udfValue = udfMap.get("value");

                        Object prevVal = null;
                        DBObject thingUdfLabel = null;
                        if (thingMongo.get(udfLabel) != null) {
                            thingUdfLabel = (DBObject) thingMongo.get(udfLabel);
                        }
                        if (thingUdfLabel != null) {
                            if (thingTypeField.isNativeObject()) {
                                DBObject thingDbObject = null;
                                if (thingUdfLabel.get("value") != null) {
                                    thingDbObject = ((DBObject) thingUdfLabel.get("value"));
                                }
                                if (thingDbObject != null) {
                                    prevVal = thingDbObject.get("code");
                                }
                            } else if (thingTypeField.isThingTypeUDF()) {
                                prevVal = (thingUdfLabel.get("value") != null ?
                                        ((DBObject) thingUdfLabel.get("value")).get("id") :
                                        null);
                            } else {
                                prevVal = thingUdfLabel.get("value");
                            }
                            if (udfValue != null && udfValue.equals(prevVal) || udfValue == null && prevVal == null
                                    || prevVal instanceof Boolean && String.valueOf(prevVal).equals(udfValue)) {
                                Date udfTime = (Date) ((DBObject) thingMongo.get(udfLabel)).get("time");
                                udfField.put("time", udfTime);
                            } else {
                                udfField.put("time", storeDate);
                            }
                        } else {
                            udfField.put("time", storeDate);
                        }

                        HashMap<String, Object> values = getValueMapForMongo(
                                thingId,
                                thingTypeField,
                                udfValue,
                                cache,
                                group,
                                storeDate,
                                thingMongo,
                                fillSource);
                        udfField.put("thingTypeFieldId", thingTypeField.getId());
                        udfField.put("value", values.get("responseMap"));
                        thingTypeFieldTickle.put(thingTypeField.getName(), values.get("responseObject"));
                        thingTypeFieldResponse.put(udfLabel, udfField);
                    }
                }
                response.put("thingTypeFieldResponse", thingTypeFieldResponse);
                response.put("thingTypeFieldTickle", thingTypeFieldTickle);
                response.put("isTimeSeries", timeSeries);
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new UserException("Error when build the udf's Map for Mongo." + e.getMessage(), e);
        }
        return response;

    }

    /*********************************
     * get value Map for mongo
     *********************************/
    public HashMap<String, Object> getValueMapForMongo(
            Long thingId,
            ThingTypeField thingTypeField,
            Object udfValue,
            Map<String, Map<String, Object>> cache,
            Group group,
            Date storeDate,
            DBObject dbThingOld,
            boolean fillSource) {

        HashMap<String, Object> response = new HashMap<>();
        Object responseMap = new Object();
        String responseObject = null;
        DataType dataType = thingTypeField.getDataType();

        String kafkaEnabledValue = Configuration.getProperty("kafka.enabled");
        boolean kafkaEnabled = kafkaEnabledValue != null ? Boolean.parseBoolean(kafkaEnabledValue) : false;

        if (udfValue != null) {
            if (dataType != null) {
                if (dataType.getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value) &&
                        dataType.getType().equals(ThingTypeField.TypeParentSubGroup
                                .TYPE_PARENT_DATA_TYPE_STANDARD_DATA.value)) {

                    udfValue = this.getStandardDataType(dataType, udfValue);
                    udfValue = this.getUdfValueAttachments(thingId, thingTypeField, dataType, group, udfValue);
                    if (fillSource && (udfValue == null || udfValue.toString().trim().isEmpty())
                            && Constants.UDF_SOURCE.compareTo(thingTypeField.getName()) == 0) {
                        udfValue = Constants.SOURCE_SERVICE;
                    }
                    responseMap = udfValue;
                    if (udfValue != null) {
                        // VIZIX-2567: we need to change the date format to yyyy-MM-dd'T'HH:mm:ss.SSSXXX.
                        if (udfValue instanceof Date) {
                            try {
                                Date date = (Date) udfValue;
                                responseObject = FormatUtil.format(date, DATE_FORMAT);
                            } catch (ParseException e) {
                                logger.error(String.format("Mistakes to format the date: %s", udfValue), e);
                                responseObject = null;
                            }
                        } else {
                            responseObject = udfValue.toString();
                        }
                    }

                } else if (dataType.getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value)
                        && dataType.getType().equals(ThingTypeField.TypeParentSubGroup
                        .TYPE_PARENT_DATA_TYPE_NATIVE_OBJECT.value)) {

                    Object nativeObject = ThingTypeFieldService.getInstance()
                            .getNativeObject(dataType.getId(),
                                    udfValue.toString(),
                                    cache,
                                    group, storeDate);
                    Map<String, Object> nativeObjectMap = null;
                    if (nativeObject instanceof LogicalReader) {
                        LogicalReader logicalReader = (LogicalReader) nativeObject;
                        nativeObjectMap = logicalReader.publicMapExtended();
                        if (kafkaEnabled) {
                            responseObject = logicalReader.getCode();
                        } else {
                            responseObject = logicalReader.getId().toString();
                        }
                    } else if (nativeObject instanceof Shift) {
                        Shift shift = (Shift) nativeObject;
                        nativeObjectMap = shift.publicMap();
                        if (kafkaEnabled) {
                            responseObject = shift.getCode();
                        } else {
                            responseObject = shift.getId().toString();
                        }
                    } else if (nativeObject instanceof Group) {
                        Group groupObject = (Group) nativeObject;
                        nativeObjectMap = groupObject.publicMap();
                        if (kafkaEnabled) {
                            responseObject = groupObject.getCode();
                        } else {
                            responseObject = groupObject.getId().toString();
                        }
                    } else if (nativeObject instanceof Zone) {
                        DBObject zoneDb = null;
                        Zone zone = (Zone) nativeObject;
                        if (dbThingOld != null) {
                            zoneDb = (DBObject) dbThingOld.get("zone");
                        }
                        if (zoneDb != null && (zoneDb.get("value") != null)) {
                            nativeObjectMap = getResponseObjectByZone(zone, zoneDb, nativeObject, storeDate);
                        } else {
                            nativeObjectMap = ((Zone) nativeObject).publicMapSummarized();
                            nativeObjectMap.put("zoneTypeTime", storeDate);
                            nativeObjectMap.put("facilityMapTime", storeDate);
                            nativeObjectMap.put("zoneGroupTime", storeDate);
                        }

                        if (kafkaEnabled) {
                            responseObject = zone.getCode();
                        } else {
                            responseObject = zone.getId().toString();
                        }

                    } else if (nativeObject instanceof Map) {
                        nativeObjectMap = (Map) nativeObject;
                    }
                    responseMap = nativeObjectMap;

                } else if (dataType.getTypeParent()
                        .equals(ThingTypeField.TypeParent.TYPE_PARENT_NATIVE_THING_TYPE.value)) {
                    Thing thing = null;
                    try {
                        if (cache != null) {
                            for (Object thingObj : cache.get("thing").values()) {
                                if (((Thing) thingObj).getSerial().equals(udfValue.toString()) &&
                                        ((Thing) thingObj).getThingType().getThingTypeCode().
                                                equals(thingTypeField.getThingType().getThingTypeCode())) {
                                    thing = (Thing) thingObj;
                                }
                            }
                        } else {
                            thing = getBySerialNumber(udfValue.toString(),
                                    ThingTypeService.getInstance().get(thingTypeField.getDataTypeThingTypeId()));
                        }
                    } catch (Exception e) {
                        throw new UserException("Value Map for Mongo: " + udfValue.toString(), e);
                    }
                    //Get Thing of the ThingType UDf
                    if (thing != null) {
                        responseMap = ThingMongoDAO.getInstance().getThing(thing.getId());
                        if (kafkaEnabled) {
                            responseObject = thing.getThingType().getCode() + "-" + thing.getSerialNumber();
                        } else {
                            responseObject = thing.getId().toString();
                        }

                    }

                }
            }
        } else {
            responseMap = null;
            responseObject = null;
        }
        response.put("responseMap", responseMap != null ? responseMap : null);
        response.put("responseObject", responseObject);
        return response;
    }

    public List<Thing> getDifferenceBetweenListsA_B(List<Thing> ini, List<Thing> last) {
        Set<Long> ids = last.stream().map(Thing::getId).collect(Collectors.toSet());
        return ini.stream().filter(thing -> !ids.contains(thing.getId())).collect(Collectors.toList());
    }

    /********************************
     *get Difference Between Lists
     *******************************/
    @Deprecated
    public List<Long> getDifferenceBetweenLists(List<Thing> ini, List<Thing> last)
    {
        List<Long> result = new ArrayList<Long>();
        for (Thing thingIniChild : ini) {
            if (last != null && last.size() > 0) {
                int cont = 0;
                for (Thing thingOldChild : last) {
                    if (thingIniChild.getId().longValue() == thingOldChild.getId().longValue()) {
                        break;
                    }
                    cont++;
                }
                if (cont == last.size()) {
                    result.add(thingIniChild.getId());
                }
            } else {
                result.add(thingIniChild.getId());
            }
        }
        return result;
    }

    /*********************************************
     * This method add to the udf input value the default values
     * based on thingTypeCode
     *********************************************/
    public Map<String, Object> getDefaultValues(String thingTypeCode, Date storeDate) {
        Map<String, Object> response = new HashMap<String, Object>();
        Map<String, Object> value = null;
        try {
            //Get the definition of the thing
            ThingType thingType = ThingTypeService.getInstance().getByCode(thingTypeCode.toString());
            for (ThingTypeField thingTypeField : thingType.getThingTypeFields()) {
                if (thingTypeField.getDefaultValue() != null && !thingTypeField.getDefaultValue().trim().equals("")) {
                    value = new HashMap<String, Object>();
                    value.put("value", thingTypeField.getDefaultValue());
                    value.put("time", storeDate.getTime());
                    response.put(thingTypeField.getName(), value);
                }
            }
        } catch (Exception e) {
            throw new UserException("Error when build the default udf's Map.", e);
        }
        return response;
    }

    /**
     * get sequence values
     *
     * @param thingTypeCode
     * @param storeDate
     * @return
     */
    public Map<String, Object> getSequenceValues(String thingTypeCode, Date storeDate) {
        Map<String, Object> response = new HashMap<String, Object>();
        /*Map<String, Object> value = null;*/
        try {
            //Get the definition of the thing
            List<ThingTypeField> sequenceThingTypeFields = DataTypeService.getInstance()
                    .getThingTypeFieldsFromCache(thingTypeCode, ThingTypeField.Type.TYPE_SEQUENCE.value);
            sequenceThingTypeFields.forEach(sttf -> {
                Map<String, Object> value = new HashMap<>();
                value.put("value", SequenceDAO.getInstance().incrementAndGetSequence(sttf.getId()));
                value.put("time", storeDate.getTime());
                response.put(sttf.getName(), value);
            });
            /*ThingType thingType = ThingTypeService.getInstance().getByCode(thingTypeCode.toString());
            for (ThingTypeField thingTypeField : thingType.getThingTypeFields()) {
                if (thingTypeField.getDataType().getTypeParent().equals(ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value) &&
                        thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_SEQUENCE.value) == 0) {
                    value = new HashMap<String, Object>();
                    value.put("value", SequenceDAO.getInstance().incrementAndGetSequence(thingTypeField.getId()));
                    value.put("time", storeDate.getTime());
                    response.put(thingTypeField.getName(), value);
                }
            }*/
        } catch (Exception e) {
            throw new UserException("Error when build the sequence udf's Map.", e);
        }
        return response;
    }

    /************************************************
     * Method to do the map for Create and Update
     ***********************************************/
    public Map<String, Object> getMapRequestForAssociate(Map<String, Object> associateMap, Long timestamp) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> udfs = new HashMap<>();
        try {
            Group group = null;
            if (associateMap.get("facilityCode") != null && !associateMap.get("facilityCode").toString().trim().equals("")) {
                group = GroupService.getInstance().getByCode(associateMap.get("facilityCode").toString());

            }
            response.put("thingTypeCode", associateMap.get("thingTypeCode"));
            if (group != null) {
                response.put("group", group.getHierarchyName(false));
            } else {
                throw new UserException("Error in the process to construct the map for create/update. Group [null], thingTypeCode [" + associateMap.get("thingTypeCode") + "].");
            }
            response.put("name", associateMap.get("tagID"));
            response.put("serialNumber", associateMap.get("tagID"));

            Iterator it = associateMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry field = (Map.Entry) it.next();
                if (!field.getKey().toString().equals("thingTypeCode")
                        && !field.getKey().toString().equals("tagID")) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("value", field.getValue());
                    data.put("time", timestamp);
                    udfs.put(field.getKey().toString(), data);
                }
            }
            if (udfs.size() > 0) {
                response.put("udfs", udfs);
            }
        } catch (NonUniqueResultException e) {
            throw new UserException("Error in the process to construct the map for create/update.", e);
        }
        return response;
    }

    /************************************************
     * Method to do the map HH for Create and Update
     ***********************************************/
    public Map<String, Object> getMapRequestForAssociateHH(Map<String, Object> associateMap, Long timestamp) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> udfs = new HashMap<>();
        try {
            Group group = null;
            if (associateMap.get("facilityCode") != null && !associateMap.get("facilityCode").toString().trim().equals("")) {
                group = GroupService.getInstance().getByCode(associateMap.get("facilityCode").toString());

            }
            response.put("thingTypeCode", associateMap.get("thingTypeCode"));
            if (group != null) {
                response.put("group", group.getHierarchyName(false));
            } else {
                throw new UserException("Error in the process to construct the map for create/update. Group [null], thingTypeCode [" + associateMap.get("thingTypeCode") + "].");
            }
            response.put("name", associateMap.get("serial"));
            response.put("serialNumber", associateMap.get("serial"));

            Iterator it = associateMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry field = (Map.Entry) it.next();
                if (!field.getKey().toString().equals("thingTypeCode")
                        && !field.getKey().toString().equals("serial")) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("value", field.getValue());
                    data.put("time", timestamp);
                    udfs.put(field.getKey().toString(), data);
                }
            }
            if (udfs.size() > 0) {
                response.put("udfs", udfs);
            }
        } catch (NonUniqueResultException e) {
            throw new UserException("Error in the process to construct the map for create/update.", e);
        }
        return response;
    }

    /************************************************
     * Method to do the map for Bulk Update in report Execution
     ***********************************************/
    public Map<String, Object> getMapRequestForBulkUpdate(Thing thing, Map<String, Object> bulkUpdateMap) {
        Map<String, Object> response = new HashMap<>();
        Map<String, Object> udfValue;
        Map<String, Object> udfs = new HashMap<String, Object>();
        try {
            if (thing != null) {
                response.put("thingTypeCode", thing.getThingType().getThingTypeCode());
                response.put("group", thing.getGroup().getHierarchyName(false));
                response.put("serialNumber", thing.getSerial());
                //Construct correct Udfs
                Iterator<Map.Entry<String, Object>> it = bulkUpdateMap.entrySet().iterator();
                while (it.hasNext()) {
                    udfValue = null;
                    Map.Entry<String, Object> current = it.next();
                    for (ThingTypeField thingTypeField : thing.getThingType().getThingTypeFields()) {
                        if (thingTypeField.getName().equals(current.getKey())) {
                            udfValue = (Map<String, Object>) bulkUpdateMap.get(current.getKey());
                            udfValue.remove("thingTypeId");
                            udfs.put(current.getKey(), udfValue);
                            break;
                        }
                    }
                }
                response.put("udfs", udfs);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new UserException("Error in the process to construct the map to update bulk. " + e.getMessage(), e);
        }
        return response;
    }

    /*********************************************************************************
     * This method creates a file based on the AttachmentID and
     * it adds  the value of the udf with the downloadURL value for new files created
     *********************************************************************************/
    public Object getUdfValueAttachments(Long thingId,
                                         ThingTypeField thingTypeField,
                                         DataType dataType,
                                         Group group,
                                         Object udfValueObject) {
        Object response = udfValueObject;
        String path = "fileSystemPath";
        if (dataType.getId().compareTo(ThingTypeField.Type.TYPE_ATTACHMENTS.value) == 0) {
            String udfValue = udfValueObject.toString();
            try {
                JSONObject json = (JSONObject) new JSONParser().parse(udfValue);
                List<JSONObject> lstFilesToDelete = new ArrayList<>();
                //Iterate the attachments
                JSONObject configuration = (JSONObject) json.get("configuration");
                String pathAttachments = (String) configuration.get(path);
                JSONArray attachments = (JSONArray) json.get("attachments");
                if (attachments != null) {
                    for (int i = 0; i < attachments.size(); i++) {
                        JSONObject attachment = (JSONObject) attachments.get(i);
                        //Create the file physically in the server
                        if (attachment.get("operation") != null && attachment.get("operation").toString().equals("add")
                                && attachment.get("id") != null) {
                            logger.info("Writting file '" + attachment.get("name") + "'");
                            String downloadURL = AttachmentService.getInstance().createFile(
                                    Long.parseLong(attachment.get("id").toString()),
                                    pathAttachments.toString(),
                                    thingId,
                                    thingTypeField.getId(),
                                    group.getHierarchyName(false));
                            logger.info("File '" + attachment.get("name") + "' created");
                            attachment.put(path, downloadURL);
                            attachment.remove("id");
                            //Delete the file physically in the server
                        } else if (attachment.get("operation") != null && attachment.get("operation").toString()
                                .equals("remove")
                                && attachment.get(path) != null) {
                            lstFilesToDelete.add(attachment);//Files to delete into json
                        }
                        //Clone the file physically in the server
                        else if (attachment.get("operation") != null && attachment.get("operation").toString().equals
                                ("clone")
                                && attachment.get(path) != null) {
                            logger.info("Cloning file '" + attachment.get("name") + "'");
                            String downloadURL = AttachmentService.getInstance().cloneFile(
                                    attachment.get(path).toString(),
                                    pathAttachments.toString(),
                                    thingId,
                                    thingTypeField.getId(),
                                    group.getHierarchyName(false));
                            logger.info("File '" + attachment.get("name") + "' cloned");
                            attachment.put(path, downloadURL);
                            attachment.remove("id");
                        }
                    }
                }
                //Delete the info in JSON  of the attachments deleted
                for (JSONObject i : lstFilesToDelete) {
                    attachments.remove(i);
                }
                //Delete attachments records who are old than one day
                AttachmentService.getInstance().deleteOldAttachments();

                response = json.toString();
            } catch (Exception e) {
                e.printStackTrace();
                throw new UserException("Error creating attachments. " + e.getMessage(), e);
            }
        }
        return response;
    }


    /************************************************
     * This method construct the map for UDF
     ***********************************************/
    public Map<String, Object> getMapOfUdf(Thing thing) {
        Map<String, Object> propertyMap = new HashMap<>();
        DBObject dbo = ThingMongoDAO.getInstance().getThing(thing.getId());
        for (String key : dbo.keySet()) {
            Object obj = dbo.get(key);
            if (obj instanceof DBObject) {
                Map<String, Object> valueMap = new HashMap<>();
                Object value = ((DBObject) obj).get("value");
                if (value instanceof DBObject) {
                    valueMap.put("value", ((DBObject) value).get("serialNumber"));
                } else {
                    valueMap.put("value", value);
                }
                propertyMap.put(key, valueMap);
            }
        }
        return propertyMap;
    }

    /*****OLD METHODS************************************************************************************************/

     /*
    * This method validates if a thing exist or not
    * */
    public Thing getBySerialNumber(String serialNumber) throws NonUniqueResultException,
            IllegalArgumentException {
        if (StringUtils.isEmpty(serialNumber)) {
            throw new IllegalArgumentException("serial number is empty");
        }
        try {
            return getThingDAO().selectBySerial(serialNumber.toUpperCase());
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }

    public Thing getBySerialNumber(String serialNumber, ThingType tt) throws NonUniqueResultException,
            IllegalArgumentException {
        //check serial
        if (StringUtils.isEmpty(serialNumber)) {
            throw new IllegalArgumentException("serial number is empty");
        }

        if (tt == null) {
            throw new IllegalArgumentException("thingType is null");
        }

        //if(group == null) {
        //    throw new IllegalArgumentException("group is null");
        //}

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("serial", serialNumber.toUpperCase());
            params.put("thingType", tt);
            //params.put("group", group);

            return getThingDAO().selectBy(params);
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }

    public Thing getBySerialNumberWithFields(String serialNumber, ThingType tt) throws NonUniqueResultException,
            IllegalArgumentException {
        //check serial
        if (StringUtils.isEmpty(serialNumber)) {
            // throw new IllegalArgumentException("serial number is empty");
            return null;
        }

        if (tt == null) {
            throw new IllegalArgumentException("The entered thingTypeCode does not exist. ");
        }

        //if(group == null) {
        //    throw new IllegalArgumentException("group is null");
        //}

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("serial", serialNumber.toUpperCase());
            params.put("thingType", tt);
            //params.put("group", group);

            return getThingDAO().selectBy(params, "group");
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }


    public boolean existThingByName(String name, Long excludeId) {
        BooleanExpression predicate = QThing.thing.name.eq(name);

        if (excludeId != null) {
            predicate = predicate.and(QThing.thing.id.ne((excludeId)));
        }

        return getThingDAO().getQuery().where(predicate).exists();
    }

    // 4 refs
    @Deprecated
    public static Thing insert(Thing thing, Date now) {
        //Validations

        if (StringUtils.isBlank(thing.getName())) {
            throw new UserException("Invalid Thing Name");
        }
        //if(ThingService.getInstance().existThingByName(thing.getName(), null)){
        //	throw new UserException("Duplicated Thing Name");
        //}
        if (thing.getParent() != null && thing.getParent().getParent() != null) {
            throw new UserException("There can only be two levels");
        }

        thing.setModifiedTime(now.getTime());

        //Insert Thing
        //todo, this is wrong use hibernate to cascade the insert and not do a manual insert on each field
        Long id = getThingDAO().insert(thing);
        thing.setId(id);
        getThingDAO().update(thing);

        //Field Names for Cassandra
        //List<CassandraField> fields = new LinkedList<>();
        //for(ThingField thingField : thing.getThingTypeFields()){
        //	//fieldNames.put(thingField.getId(), thingField.getName());
        //	fields.add(new CassandraField(thingField.getId(), thing.getId(), null, thingField.getName(),thingField.getType(),null));
        //}

        //Insert Parent History
        if (thing.getParent() != null) {
            ThingParentHistory thingParentHistory = new ThingParentHistory();
            thingParentHistory.setParent(thing.getParent());
            thingParentHistory.setChild(thing);
            thingParentHistory.setStartDate(now);
        }

        //CassandraThingDAO.insert(thing.getId(),thing.getParent() == null ? null : thing.getParent().getId(),fields, thing.getSerial());

        //TODO: insert into mongo
        insertNewMongo(thing);

        return thing;
    }

    public static void updateImage(ThingImage thingImage, Map<String, Object> thingMap) {
        //Getting image property
        if (thingMap.containsKey("image") && thingMap.get("image") != null) {
            String imageUploaded = new String((String) thingMap.get("image"));
            String inputStream = imageUploaded.substring(22);
            byte[] decodedBytes = null;
            decodedBytes = Base64.decodeBase64(inputStream.getBytes(Charsets.UTF_8));
            thingImage.setImage(decodedBytes);
        }
    }

    // 11 refs (x10 popsb, x1 ThingTypeController)
    public Thing insert(
            ThingType thingType, String name, String serial, Group group, User createdUser) {
        return insert(thingType, name, serial, group, createdUser, null, true);
    }

    @Deprecated
    public Thing insert(ThingType thingType, String name, String serial, Group group,
                        User createdUser, Thing parent, boolean fillSource) {
        /*Date date = new Date(); //TODO: pass in argument list !!!!!!
        Thing thing = instance (thingType, name, serial, group, createdUser, parent);
        thing.setModifiedTime(date.getTime());
        getThingDAO().getSession().save(thing);
        insertNewMongo( thing );
        if (parent != null) {
            ThingMongoDAO.getInstance().associateChild(parent, thing.getId());
        }*/

        Date date = new Date(); //TODO: pass in argument list !!!!!!

        Thing thing = new Thing();
        thing.setCreatedByUser(createdUser);
        thing.setThingType(thingType);
        thing.setSerial(serial);
        thing.setName(name);
        thing.setActivated(false);
        thing.setGroup(group);

        thing.setModifiedTime(date.getTime());

        if (parent != null) {
            thing.setParent(parent);
        }

        /**********************/
        Long id = this.getThingDAO().insert(thing);
        thing.setId(id);
        /*Insert Mongo*/
        Map<String, Object> udfMap = this.getUdfMapForMongo(id, null, thingType, null, date, null, group, fillSource);
        Map<String, Object> thingTypeFieldResponse = null;
        boolean isTimeSeries = false;
        if (udfMap != null && udfMap.size() > 0) {
            thingTypeFieldResponse = udfMap.get("thingTypeFieldResponse") != null ? (Map<String, Object>) udfMap.get("thingTypeFieldResponse") : null;
            isTimeSeries = udfMap.get("isTimeSeries") != null ? (boolean) udfMap.get("isTimeSeries") : false;
        }

        try {
            ThingMongoService.getInstance().createThing(
                    thing,
                    thingTypeFieldResponse,
                    new Date()/*,
                    isTimeSeries*/);
        } catch (Exception e) {
            thing = null;
        }

        return thing;
    }

    /**
     * Instantiate a new thing.
     *
     * @param thingType   new things type
     * @param serial      serial number
     * @param group       group this thing belongs
     * @param createdUser user creating this thing
     * @param parent      thing's parent
     * @return thing instantiated
     */
    // 2 refs (x1 ThingHandler, x1 ThingService)
    public Thing instance(ThingType thingType, String name, String serial, Group group,
                          User createdUser, Thing parent) {
        Date date = new Date(); //TODO: pass in argument list !!!!!!

        Thing thing = new Thing();
        thing.setCreatedByUser(createdUser);
        thing.setThingType(thingType);
        thing.setSerial(serial);
        thing.setName(name);
        thing.setActivated(false);
        thing.setGroup(group);

        thing.setModifiedTime(date.getTime());

        if (parent != null) {
            thing.setParent(parent);
        }


        //Insert Parent History
        if (thing.getParent() != null) {
            ThingParentHistory thingParentHistory = new ThingParentHistory();
            thingParentHistory.setParent(thing.getParent());
            thingParentHistory.setChild(thing);
            thingParentHistory.setStartDate(date);
        }

        //No need to insert at this point.
        //insertNewMongo( thing );

        return thing;
    }

    @Deprecated
    public static void update(Thing thing, Date now) {

        if (StringUtils.isBlank(thing.getName())) {
            throw new UserException("Invalid Thing Name");
        }
        //if(ThingService.getInstance().existThingByName(thing.getName(), thing.getId())){
        //	throw new UserException("Duplicated Thing Name");
        //}
        if (thing.getParent() != null) {
            if (thing.getParent().getParent() != null) {
                throw new UserException("There can only be two levels");
            }
            if (ThingService.getInstance().selectByParent(thing).size() > 0) {//TODO: Improve performance
                throw new UserException("There can only be two levels");
            }
        }

        thing.setModifiedTime(new Date().getTime());

        Thing oldThing = ThingService.getInstance().get(thing.getId());
        getThingDAO().update(thing);

        // update thing in mongo
        ThingMongoDAO dao = ThingMongoDAO.getInstance();

        Group group = thing.getGroup();

        if (group != null) {
            dao.insertOrUpdate(thing.getId(), "groupTypeId", "" + group.getGroupType().getId());
            dao.insertOrUpdate(thing.getId(), "groupTypeCode", group.getGroupType().getCode() == null
                    ? "" : thing.getGroup().getGroupType().getCode());
            dao.insertOrUpdate(thing.getId(), "groupId", "" + group.getId());
            dao.insertOrUpdate(thing.getId(), "groupCode", "" + group.getCode());
            dao.insertOrUpdate(thing.getId(), "groupName", "" + group.getName());
        }

        dao.insertOrUpdate(thing.getId(), "name", thing.getName());
        dao.insertOrUpdate(thing.getId(), "serialNumber", thing.getSerial());
        //CassandraThing cassandraThing = CassandraThingDAO.getThing(thing.getId());
        //if (!thing.getSerial().equals(cassandraThing.getSerial())) {
        //    CassandraThingDAO.updateSerial(thing.getId(), thing.getSerial());
        //}

        //Updating parent history
        if (oldThing.getParent() != thing.getParent()) {
            //Close old relationship
            if (oldThing.getParent() != null) {
                ThingParentHistory thingParentHistory = ThingParentHistoryService.getInstance().selectActiveByParentAndChild(oldThing.getParent(), thing);

                if (thingParentHistory != null) {
                    thingParentHistory.setEndDate(now);
                    ThingParentHistoryService.getInstance().update(thingParentHistory);
                } else {
                    throw new RuntimeException("Expecting thingParentHistory record");
                }
            }

            //Open a new one
            if (thing.getParent() != null) {
                ThingParentHistory thingParentHistory = new ThingParentHistory();
                thingParentHistory.setParent(thing.getParent());
                thingParentHistory.setChild(thing);
                thingParentHistory.setStartDate(now);
            }
        }
    }

    public List<Thing> getThingsFromList(List<Long> thingList) {
        if (thingList == null || thingList.isEmpty()) return null;

        return getThingDAO().getQuery().where(QThing.thing.id.in(thingList)).list(QThing.thing);
    }

    public List<Thing> selectByParent(Thing thing) {
        return getThingDAO().selectAllBy(QThing.thing.parent.id.eq(thing.getId()));
    }

    public List<ThingTypeField> getChildrenThingFields(Thing thing) {
        List<ThingTypeField> childrenFields = new LinkedList<>();
        List<Thing> children = getThingDAO().selectAll();
        if (children != null) {
            for (Thing child : children) {
                if (child.getParent() == thing) {
                    for (ThingTypeField thingField : child.getThingType().getThingTypeFields()) {
                        childrenFields.add(thingField);
                    }
                }
            }
        }
        return childrenFields;
    }

    public Thing getFirstChild(Thing thing) {
        List<Thing> childrenList = selectByParent(thing);
        Thing childrenThing = null;
        for (Thing childThingData : childrenList) {
            childrenThing = childThingData;
            break;
        }
        return childrenThing;
    }

    public List<Thing> getChildrenList(Thing thing) {
        try {
            return selectByParent(thing);
        } catch (Exception e) {
            logger.warn("Looks like the object is not committed yet or does not exists");
            return null;
        }
    }

    public static String getPropertyNameOrSerial(Thing thing, String property) {
        if (property.equals("name"))
            return thing.getName();
        if (property.equals("serial"))
            return thing.getSerial();
        return "";
    }

    @Deprecated
    public static String getThingFieldValue(Long thingId, Long thingTypeFieldId) {
//        String value = FieldValueService.value(thingId, thingTypeFieldId);
        return null;
    }

    @Deprecated
    public static String getThingFieldTimeStamp(Long thingFieldId) {
//        Date timeStamp = (Date) FieldValueService.timeStamp(thingFieldId);
        return null;
    }

    /**
     * Get map of first children from a list of parents
     *
     * @param parents list of parents who's children we want
     * @return parent ids mapped to their first children
     */
    public Map<Long, Thing> selectByParents(List<Thing> parents) {
        Map<Long, Thing> values = new HashMap<>();
        logger.info("Total childrenMap to extract from db " + parents.size());
        Map<Long, Thing> childrenMap = selectByParents(parents, values);
        logger.info("Total childrenMap returned from db : " + childrenMap.keySet().size());

        return childrenMap;
    }

    /**
     * Get map of first children from a list of parents in chunks
     *
     * @param parents  list of parents who's children we want
     * @param valuesIn recursive map to store results
     * @return parent ids mapped to their first children
     */
    public Map<Long, Thing> selectByParents(List<Thing> parents, Map<Long, Thing> valuesIn) {

        if (parents.size() > maxCollection) {
            logger.info("parents size (when > " + maxCollection + ") " + parents.size());

            Map<Long, Thing> childrenMap = new HashMap<>();
            childrenMap.putAll(processChildrenMap(parents.subList(0, maxCollection)));

            Map<Long, Thing> data = selectByParents(parents.subList(maxCollection, parents.size()), childrenMap);
            logger.info("data chunk returned size " + data.keySet().size());

            valuesIn.putAll(data);

            return valuesIn;
        }
        //return subset
        else {
            logger.info("parents size (when < " + maxCollection + ") " + parents.size());

            Map<Long, Thing> childrenMap = new HashMap<>();
            childrenMap.putAll(processChildrenMap(parents));

            logger.info("data chunk returned size " + childrenMap.keySet().size());

            valuesIn.putAll(childrenMap);

            return valuesIn;
        }
    }

    /**
     * processes the children parents map
     *
     * @param parents
     * @return
     */
    private Map<Long, Thing> processChildrenMap(List<Thing> parents) {
        Map<Long, Thing> childrenMap = new HashMap<>();
        List<Thing> children = new LinkedList<>();
        if (parents != null && parents.size() > 0) {
            children = getThingDAO().selectAllBy(QThing.thing.parent.in(parents));
        }

        //todo bad, n+1. need to fetch. could work only if parents are pre fetched
        for (Thing child : children) {
            childrenMap.put(child.getParent().getId(), child);
        }

        return childrenMap;
    }

    /**
     * List things paginating them and pre-fetching associations.
     *
     * @param be
     * @param pagination
     * @param orderString
     * @param properties         list of properties to pre-fetch
     * @param collectionProperty list property to pre-fetch. can be only one
     * @return things paginated with properties pre-fetched
     */
    public List<Thing> listPaginated(Predicate be,
                                     Pagination pagination,
                                     String orderString,
                                     List<EntityPathBase<?>> properties, ListPath<?, ?> collectionProperty) {
        OrderSpecifier orderSpecifiers[] = QueryUtils.getOrderFields(QThing.thing, orderString);
        return getThingDAO().selectAll(be, properties, collectionProperty, pagination, orderSpecifiers);
    }

    public List<Thing> selectByThingType(Long thingTypeId) {
        HibernateQuery query = getThingDAO().getQuery();
        return query.where(QThing.thing.thingType.id.eq(thingTypeId))
                .list(QThing.thing);
    }

    public List<Thing> selectByThingType(ThingType thingType) {
        return getThingDAO().selectAllBy("thingType", thingType);
    }

    public List<Thing> selectByThingTypeAndGroup(Long thingTypeId, Long groupId) {
        List<Thing> things = new ArrayList<>();
        Map<Long, List<Long>> mapGroup = VisibilityThingUtils.calculateVisibilityThingsID(groupId);
        if ((mapGroup != null) && (!mapGroup.isEmpty())) {
            List<Long> groupIds = new ArrayList<>();
            for (Long groupInd : mapGroup.keySet()) {
                groupIds.addAll(GroupService.getInstance().getListGroupIdsChildren(groupInd));
            }
            HibernateQuery query = getThingDAO().getQuery();
            things = query.where(QThing.thing.thingType.id.eq(thingTypeId).and(QThing.thing.group.id.in(groupIds)))
                    .list(QThing.thing);
        }
        return things;
    }

    /**
     * Synchronize all thing fields
     *
     * @param master thingType to sync
     */
    public void synchronizeThings(ThingType master) {
        logger.debug("going to sync " + master.getId());
        //get all things that have the type master
        List<Thing> things = ThingService.getInstance().selectByThingType(master.getId());
        //loop all things and change their things
        for (Thing thing : things) {
            logger.debug("processing " + thing.getId());

            getThingDAO().update(thing);
        }

        BrokerClientHelper.sendRefreshThingMessage(false, GroupService.getInstance().getMqttGroups(master.getGroup()));

        logger.info("updated " + things.size() + " things");
    }

    public boolean existsSerial(String serial, Long thingTypeId) {
        return getThingDAO().existsSerial(serial, thingTypeId);
    }

    public boolean existsSerial(String serial, Long thingTypeId, Long excludeId) {
        return getThingDAO().existsSerial(serial, thingTypeId, excludeId);
    }

    /**
     * Return all things from the database
     *
     * @return a list with all things
     */
    public List<Thing> selectAllThings() {
        return getThingDAO().selectAll();
    }

    /**
     * Return all things that are on the list of ids
     *
     * @param ids list of ids of things to return
     * @return a list of things who's ids match ids
     */
    public List<Thing> selectAllIn(String where, Collection<Long> ids) {
        logger.info("where " + where);
        BooleanBuilder bb = QueryUtils.buildSearch(QThing.thing, where);
        return getThingDAO().selectAllBy(bb.and(QThing.thing.id.in(ids)));
    }

    public List<Thing> selectWith(String where) {
        return getThingDAO().selectAllBy(QueryUtils.buildSearch(QThing.thing, where));
    }

    /**
     * TODO Review possible NullPointer bugs
     *
     * @param id
     * @return
     * @throws IllegalArgumentException
     */
    public Map<String, Object> getThingLastZone(Long id) throws IllegalArgumentException {

        if (id == null)
            throw new IllegalArgumentException("Thing ID can not be null");

        Map<String, Object> response = new HashMap<>();


        Map<String, Object> doc = (Map) ThingMongoDAO.getInstance().getThing(id);

        ThingTypeField thingTypeField = (ThingTypeField) ThingTypeFieldService.getInstance().
                getThingTypeFieldByType(ThingTypeField.Type.TYPE_ZONE.value);

        int camCount = 0;

        if (thingTypeField != null && doc.containsKey(thingTypeField.getName())) {
            Map<String, Object> zone = (Map) doc.get(thingTypeField.getName());

            ZoneProperty zoneProperty = ZoneService.getInstance().
                    getZonePropertyByName(ZoneService.getInstance().
                            get(Long.parseLong(zone.get("_id").toString())), "camera");
            String cameraProperty = ZonePropertyValueService.getInstance().
                    getZonePropertyValue(Long.parseLong(zone.get("_id").toString()), zoneProperty.getId());

            if (cameraProperty.length() > 0) {
                camCount = cameraProperty.split(",").length;
            }

            response.put("zoneName", zone.get("name"));
            response.put("zoneId", zone.get("_id"));
            response.put("cams", camCount);
        }

        return response;
    }


    /**
     * Update the field modifiedTime of all records using a bulk update
     */
    public void updateModifiedTime() {
        Date date = new Date();
        Session session = getThingDAO().getSession();
        String hqlQuery = "update Thing set modifiedTime = :currentTime";
        Query query = HibernateDAOUtils.createQuery(session, hqlQuery);
        query.setLong("currentTime", date.getTime());
        query.executeUpdate();
    }

    static public void insertNewMongo(Thing thing) {
        Long id = thing.getId();
        if (id == null) {
            logger.warn("thingId=null");
            return;
        }

        insertNewMongo2(thing);

    }

    static public void insertNewMongo2(Thing thing) {
        ThingMongoDAO dao = ThingMongoDAO.getInstance();

        GroupType groupType = thing.getGroup().getGroupType();
        //groupType
        dao.insertOrUpdate(thing.getId(), "groupTypeId", "" + groupType.getId());
        dao.insertOrUpdate(thing.getId(), "groupTypeName", groupType.getName() == null ? "" : groupType.getName());
        dao.insertOrUpdate(thing.getId(), "groupTypeCode", groupType.getCode() == null ? "" : groupType.getCode());
        //group
        dao.insertOrUpdate(thing.getId(), "groupId", "" + thing.getGroup().getId());
        dao.insertOrUpdate(thing.getId(), "groupCode", "" + thing.getGroup().getCode());
        dao.insertOrUpdate(thing.getId(), "groupName", "" + thing.getGroup().getName());

        dao.insertOrUpdate(thing.getId(), "thingTypeId", "" + thing.getThingType().getId());
        dao.insertOrUpdate(thing.getId(), "thingTypeCode", "" + thing.getThingType().getThingTypeCode());
        dao.insertOrUpdate(thing.getId(), "thingTypeName", "" + thing.getThingType().getName());
        dao.insertOrUpdate(thing.getId(), "name", thing.getName());
        dao.insertOrUpdate(thing.getId(), "serialNumber", thing.getSerial());

    }

    public Map<String, Long> getThingsByZone() {
        ThingMongoDAO dao = ThingMongoDAO.getInstance();

        return dao.getThingsByZone();
    }

    public Thing getBySerialAndThingTypeCode(String serial, Long thingTypeId) throws NonUniqueResultException {
        try {
            Map<String, Object> values = new HashMap<>();
            values.put("serial", serial);
            values.put("thingType.id", thingTypeId);
            return getThingDAO().selectBy(values);
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }

    /***************************************
     * Get Thing by serial and ThingTypeCode
     **************************************/
    public Thing getBySerialAndThingTypeCode(String serial, String thingTypeCode) throws NonUniqueResultException {
        try {
            ThingType thingType = ThingTypeService.getInstance().getByCode(thingTypeCode);
            return this.getBySerialAndThingTypeCode(serial, thingType.getId());
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }

    public Thing getBySerialAndThingType(String serial, ThingType thingType) throws NonUniqueResultException {
        try {
            return this.getBySerialAndThingTypeCode(serial, thingType.getId());
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }

    public Long countTrueFlags(Group visibilityGroupUser) {

        BooleanBuilder bb = new BooleanBuilder(QThingTypeField.thingTypeField.name.toLowerCase().like("%flag%"));
        bb = bb.and(QThingTypeField.thingTypeField.dataType.id.eq((long) ThingTypeField.Type.TYPE_BOOLEAN.value));
        bb = bb.and(QThingTypeField.thingTypeField.thingType.id.in(VisibilityThingUtils.getListThingTypesID()));

        List<String> fieldsNameFlag = ThingTypeFieldService.getThingTypeFieldDAO().getQuery()
                .where(bb).distinct().list(QThingTypeField.thingTypeField.name);
        List<Long> listThingID = new ArrayList<>();
        if (visibilityGroupUser != null) {
            BooleanBuilder builder = VisibilityThingUtils.limitSelectAllT("", "", visibilityGroupUser.getId());
            listThingID = ThingService.getThingDAO().getQuery().where(builder).list(QThing.thing.id);
        }
        long time = System.currentTimeMillis();
        Long result = ThingMongoDAO.getInstance().countTrueFlags(fieldsNameFlag, listThingID);
        logger.info("TIME: " + (System.currentTimeMillis() - time));
        return result;
    }

    /**
     * Generates map with all udfs
     *
     * @param id   thing id
     * @param time specific thingSnapshot's time  (null for las value)
     * @return Map of udfs
     */
    public Map<String, Object> getThingRecord(Long id, Long time, String comparator) {
        //Get Thing
        Thing thing = ThingService.getInstance().get(id);

        Map<String, Object> doc = new HashMap<>();
        if (time != null) {
            doc = ThingMongoDAO.getInstance().getThingSnapshotByThingIdAndTime(id, time, ThingMongoDAO.getComparator(comparator));
        } else {
            doc = (Map<String, Object>) ThingMongoDAO.getInstance().getThing(id);
        }
        if ((thing == null) || (doc == null)) {
            throw new UserException(String.format("Thing[%d] at not found", id));
        } else if (time != null) {
            doc = (BasicDBObject) doc.get("value");
        }

        CompositeThing compositeThing = new CompositeThing(thing, getChildrenList(thing));

        Map<String, Object> result = new HashMap<>();
        result.putAll(parseThingFromMongo(compositeThing.getThing(), doc));

//        List<Map<String, Object> > childrenDoc = (List) doc.get("children");

        List<Map<String, Object>> children = new ArrayList<>();

//        if (compositeThing.getChildren() != null) {
//            for(Thing thingChild : compositeThing.getChildren()){
//                for(Map<String, Object> docChild : childrenDoc){
//                    if(thingChild.getId() == Long.parseLong(docChild.get("_id").toString())){
//                        children.add(parseThingFromMongo(thingChild, docChild));
//                    }
//                }
//            }
//        }
        if (compositeThing.getChildren() != null) {
            List<Thing> childrens = compositeThing.getChildren();
            for (Thing item : childrens) {
                children.add(parseThingFromMongo(item, null));
            }
        }


        result.put("children", children);

        return result;

    }

    /**
     * Generates map with all udfs
     *
     * @param id       thing id
     * @param time     specific thingSnapshot's time  (null for las value)
     * @param repDefId report definition id witch thing will be filtered by (null for all fields)
     * @return Map of udfs
     */
    public Map<String, Object> getThingRecord(Long id, Long time, Long repDefId, String comparator, String mode,
                                              String serverName, String contextPath, DateFormatAndTimeZone dateFormatAndTimeZone) {

        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(repDefId);
        if (reportDefinition == null) {
            throw new UserException(String.format("ReportDefinitionId[%d] not found", repDefId));
        }

        boolean created = true;

        Map<String, Object> thingDoc;
        Map<String, Object> thingHeaders = new LinkedHashMap<>();
        if (time != null) {
            thingDoc = ThingMongoDAO.getThingSnapshotByThingIdAndTime(id, time, ThingMongoDAO.getComparator(comparator));
            if (thingDoc != null) {
                thingHeaders.putAll(getInfoHeader((BasicDBObject) thingDoc.get("value")));
            }
        } else {
            thingDoc = ThingMongoDAO.getThingToMap(id);
            thingHeaders.putAll(getInfoHeader(thingDoc));
            if (thingDoc != null && (thingDoc.keySet() != null) && (thingDoc.keySet().size() == 0)) {
                DBObject thingTemp = ThingMongoDAO.getInstance().getThing(id);

                for (String field : thingTemp.keySet()) {
                    if (!(thingTemp.get(field) instanceof BasicDBObject)) {
                        thingDoc.put(field, thingTemp.get(field));
                    }
                }
                created = false;
            }
        }
        Map<String, Object> resultMap = new HashMap<>();
        if (thingDoc != null) {
            Date now = new Date();
            ITranslateResult translateResult;
            com.tierconnect.riot.iot.reports_integration.ReportConfig reportConfig;
            Date endTime = time != null ? new Date(time) : null;
            if (time != null) {
                reportConfig = new TableHistoryReportConfig(reportDefinition,
                        Collections.<String, Object>emptyMap(), null, null, null, endTime, now, true, true, dateFormatAndTimeZone);

                translateResult = new TableHistoryTranslateResult(reportConfig, serverName, contextPath);
            } else {
                reportConfig = new TableDetailReportConfig(reportDefinition,
                        Collections.<String, Object>emptyMap(), null, null, null, null, now, true, dateFormatAndTimeZone, false);
                translateResult = new TableDetailTranslateResult(reportConfig, serverName, contextPath);
            }
            translateResult.exportResult(thingDoc);
            Map<String, Object> thingProperties = translateResult.getLabelValues();
            Map<String, Object> thingTypeFieldMap = reportConfig.exportResult(1L, Collections.<Map<String, Object>>singletonList(thingProperties));

            Map<String, Object> thingPropertiesFiltered = new LinkedHashMap<>();
            Map<String, Object> thingTypeFieldMapFiltered = new LinkedHashMap<>();
            //TODO Review to implement another way to ger showHover reportProperties
            if (mode != null && mode.toLowerCase().equals("hover")) {//For hover option
                for (ReportProperty reportProperty : reportDefinition.getReportProperty()) {
                    if (reportProperty.getShowHover()) {
                        thingPropertiesFiltered.put(reportProperty.getLabel(),
                                thingProperties.get(reportProperty.getLabel()));
                        thingTypeFieldMapFiltered.put(reportProperty.getLabel(),
                                ((Map) thingTypeFieldMap.get(Constants.THING_FIELD_TYPE_MAP)).get(reportProperty.getLabel()));
                    }
                }
            } else {//if (mode.toLowerCase().equals("popup")){ For popup option
                thingProperties.remove(Constants.ID);
                thingProperties.remove(Constants.THING_TYPE_TRANSLATE_ID);
                thingPropertiesFiltered.putAll(thingProperties);
                thingTypeFieldMapFiltered.putAll(((Map) thingTypeFieldMap.get(Constants.THING_FIELD_TYPE_MAP)));

            }
            thingPropertiesFiltered.remove("fieldsNotChanged");

            resultMap.put(Constants.THING_CREATED, created);
            resultMap.put(Constants.THING_HEADERS, thingHeaders);
            resultMap.put(Constants.THING_FIELD_TYPE_MAP, thingTypeFieldMapFiltered);
            resultMap.put(Constants.THING_PROPERTIES, thingPropertiesFiltered);
        }

        return resultMap;
    }

    private Map<String, Object> getInfoHeader(Map<String, Object> basicDBObject) {
        Map<String, Object> thingHeaders = new LinkedHashMap<>();
        if (basicDBObject != null) {
            thingHeaders.put(Constants.NAME, basicDBObject.get(Constants.NAME));
            if (basicDBObject.get(Constants.SERIAL_NUMBER) != null) {
                thingHeaders.put(Constants.SERIAL_NUMBER, StringUtils.upperCase(basicDBObject.get(Constants.SERIAL_NUMBER).toString()));
            }
            thingHeaders.put(Constants.THING_TYPE_TRANSLATE_NAME, basicDBObject.get(Constants.THING_TYPE_TRANSLATE_NAME));
            // add location for VIZIX-1789
            thingHeaders.put(Constants.LOCATION,
                    getLocationOfRecord(basicDBObject,
                            (Long) basicDBObject.get(Constants.THING_TYPE_TRANSLATE_ID),
                            ThingTypeField.Type.TYPE_LONLATALT.value));
        }
        return thingHeaders;
    }

    private String getLocationOfRecord(Map<String, Object> basicDBObject, Long thingTypeId, Long thingFieldType) {
        String value = null;
        ThingType thingType = ThingTypeService.getInstance().get(thingTypeId);

        if (thingType != null) {
            List<ThingTypeField> thingTypeFieldsByType = thingType.getThingTypeFieldsByType(thingFieldType);
            if (!thingTypeFieldsByType.isEmpty()) {
                Object fieldMongo = basicDBObject.get(thingTypeFieldsByType.get(0).getName());
                if (fieldMongo instanceof Map && ((Map) fieldMongo).get("value") != null) {
                    value = String.valueOf(((Map) fieldMongo).get("value"));
                }
            }
        }
        return value;
    }

    private Map<String, Object> parseThingFromMongo(Thing thing, Map<String, Object> doc) {
        Map<String, Object> response = new LinkedHashMap<>();

        List<ThingTypeField> thingTypeFieldList = new ArrayList<>(thing.getThingType().getThingTypeFields());

        response.put("Name", thing.getName());
        response.put("Serial", thing.getSerial().toUpperCase());

        List<Object> thingFieldsListRes = new LinkedList<>();
        if (doc != null) {
            for (ThingTypeField thingTypeField : thingTypeFieldList) {

                if (doc.containsKey(thingTypeField.getName())) {
                    Map<String, Object> thingFieldsMap = new TreeMap<>();
                    if (thingTypeField.getTimeSeries() != null && thingTypeField.getTimeSeries()) {
                        Date timeStampField = (Date) ((Map) doc.get(thingTypeField.getName())).get("time");
                        if (timeStampField != null) {
                            Map<String, Object> dwellTimeMap = new TreeMap<>();
                            dwellTimeMap.put("name", "dwellTime(" + thingTypeField.getName() + ")");
                            dwellTimeMap.put("value", ((new Date().getTime()) - timeStampField.getTime()));
                            dwellTimeMap.put("type", 0);
                            thingFieldsListRes.add(dwellTimeMap);
                        }
                    }

                    if (thingTypeField.getDataType().getId() == ThingTypeField.Type.TYPE_ZONE.value) {
                        Long zoneId = null;

                        if (doc.get(thingTypeField.getName()) instanceof Map &&
                                ((Map) doc.get(thingTypeField.getName())).containsKey("value")) {
                            if (((Map) doc.get(thingTypeField.getName())).get("value") instanceof Map &&
                                    ((Map) ((Map) doc.get(thingTypeField.getName())).get("value")).containsKey("id")) {
                                zoneId = Long.parseLong(((Map) ((Map) doc.get(thingTypeField.getName())).get("value")).get("id").toString());
                            }
                        }

                        if (zoneId != null) {
                            Zone zone = ZoneService.getZoneDAO().selectById(zoneId);
                            if (zone != null) {
                                //ZoneGroup
                                Map<String, Object> extraMap = new TreeMap<>();
                                thingFieldsMap.put("Zone Last Entry", ((Map) doc.get(thingTypeField.getName())).get("time"));
                                if (zone.getZoneGroup() != null) {
                                    extraMap.put("name", "Zone Group");
                                    extraMap.put("value", zone.getZoneGroup().getName());
                                    extraMap.put("type", ThingTypeField.Type.TYPE_TEXT.value);
                                }
                                if (zone.getLocalMap() != null) {
                                    extraMap.put("name", "Facility Map");
                                    extraMap.put("value", zone.getLocalMap().getName());
                                    extraMap.put("type", ThingTypeField.Type.TYPE_TEXT.value);
                                }
                                if (zone.getZoneType() != null) {
                                    extraMap.put("name", "Zone Type");
                                    extraMap.put("value", zone.getZoneType().getName());
                                    extraMap.put("type", ThingTypeField.Type.TYPE_TEXT.value);
                                }
                                if (extraMap.size() > 0) {
                                    thingFieldsListRes.add(extraMap);
                                }
                                thingFieldsMap.put("name", thingTypeField.getName());
                                thingFieldsMap.put("type", thingTypeField.getDataType().getId());
                                thingFieldsMap.put("value", zone.getName());
                                thingFieldsListRes.add(thingFieldsMap);
                            }
                        } else {
                            thingFieldsMap.put("name", thingTypeField.getName());
                            thingFieldsMap.put("type", thingTypeField.getDataType().getId());
                            thingFieldsMap.put("value", "");
                            thingFieldsListRes.add(thingFieldsMap);
                        }
                    } else if (thingTypeField.getDataType().getId() == ThingTypeField.Type.TYPE_SHIFT.value) {

                        Object shifts = ((Map) doc.get(thingTypeField.getName())).get("value");

                        if (doc.get(thingTypeField.getName()) instanceof Map && ((Map) doc.get(thingTypeField.getName())).containsKey("value")) {
                            shifts = ((Map) doc.get(thingTypeField.getName())).get("value");
                        }

                        if (shifts != null) {

                            List<Long> shiftIds = new ArrayList<>();

                            if (shifts instanceof List) {
                                for (Object shiftObj : (List) shifts) {
                                    shiftIds.add((Long) ((Map) shiftObj).get("id"));
                                }
                            } else if (shifts instanceof Map && ((Map) shifts).containsKey("id")) {
                                shiftIds.add((Long) ((Map) shifts).get("id"));
                            }

                            if (shiftIds.size() > 0) {
                                String shiftValue = ShiftService.getInstance().getShiftNamesFromIds(shiftIds).toString().replace("[", "").replace("]", "");
                                thingFieldsMap.put("name", thingTypeField.getName());
                                thingFieldsMap.put("type", thingTypeField.getDataType().getId());
                                thingFieldsMap.put("value", shiftValue);
                                thingFieldsListRes.add(thingFieldsMap);
                            } else {
                                thingFieldsMap.put("name", thingTypeField.getName());
                                thingFieldsMap.put("type", thingTypeField.getDataType().getId());
                                thingFieldsMap.put("value", "");
                                thingFieldsListRes.add(thingFieldsMap);
                            }
                        }
                    } else if (thingTypeField.getDataType().getId() == ThingTypeField.Type.TYPE_LOGICAL_READER.value) {

                        Object lr = null;

                        if (doc.get(thingTypeField.getName()) instanceof Map &&
                                ((Map) doc.get(thingTypeField.getName())).containsKey("value")) {
                            if (((Map) doc.get(thingTypeField.getName())).get("value") instanceof Map &&
                                    ((Map) ((Map) doc.get(thingTypeField.getName())).get("value")).containsKey("name")) {
                                lr = ((Map) ((Map) doc.get(thingTypeField.getName())).get("value")).get("name");
                            }
                        }

                        if (lr != null) {
                            thingFieldsMap.put("name", thingTypeField.getName());
                            thingFieldsMap.put("type", thingTypeField.getDataType().getId());
                            thingFieldsMap.put("value", lr);

                            thingFieldsListRes.add(thingFieldsMap);
                        } else {
                            thingFieldsMap.put("name", thingTypeField.getName());
                            thingFieldsMap.put("type", thingTypeField.getDataType().getId());
                            thingFieldsMap.put("value", "");
                            thingFieldsListRes.add(thingFieldsMap);
                        }

                    } else if (thingTypeField.getDataType().getId() == ThingTypeField.Type.TYPE_GROUP.value) {
                        Object group = null;

                        if (doc.get(thingTypeField.getName()) instanceof Map &&
                                ((Map) doc.get(thingTypeField.getName())).containsKey("value")) {
                            if (((Map) doc.get(thingTypeField.getName())).get("value") instanceof Map &&
                                    ((Map) ((Map) doc.get(thingTypeField.getName())).get("value")).containsKey("code")) {
                                group = ((Map) ((Map) doc.get(thingTypeField.getName())).get("value")).get("code");
                            }
                        }

                        if (group != null) {
                            thingFieldsMap.put("name", thingTypeField.getName());
                            thingFieldsMap.put("type", thingTypeField.getDataType().getId());
                            thingFieldsMap.put("value", group);

                            thingFieldsListRes.add(thingFieldsMap);
                        } else {
                            thingFieldsMap.put("name", thingTypeField.getName());
                            thingFieldsMap.put("type", thingTypeField.getDataType().getId());
                            thingFieldsMap.put("value", "");
                            thingFieldsListRes.add(thingFieldsMap);
                        }

                    } else if (thingTypeField.getDataType().getId() == ThingTypeField.Type.TYPE_THING_TYPE.value) {
                        Object ntt = null;

                        if (doc.get(thingTypeField.getName()) instanceof Map &&
                                ((Map) doc.get(thingTypeField.getName())).containsKey("value")) {
                            if (((Map) doc.get(thingTypeField.getName())).get("value") instanceof Map &&
                                    ((Map) ((Map) doc.get(thingTypeField.getName())).get("value")).containsKey("serialNumber")) {
                                ntt = ((Map) ((Map) doc.get(thingTypeField.getName())).get("value")).get("serialNumber");
                            }
                        }

                        if (ntt != null) {
                            thingFieldsMap.put("name", thingTypeField.getName());
                            thingFieldsMap.put("type", thingTypeField.getDataType().getId());
                            thingFieldsMap.put("value", ntt);

                            thingFieldsListRes.add(thingFieldsMap);
                        } else {
                            thingFieldsMap.put("name", thingTypeField.getName());
                            thingFieldsMap.put("type", thingTypeField.getDataType().getId());
                            thingFieldsMap.put("value", "");
                            thingFieldsListRes.add(thingFieldsMap);
                        }

                    } else {
                        Object value = null;

                        if (doc.get(thingTypeField.getName()) instanceof Map && ((Map) doc.get(thingTypeField.getName())).containsKey("value")) {
                            value = ((Map) doc.get(thingTypeField.getName())).get("value");
                        }

                        if (value != null) {
                            thingFieldsMap.put("name", thingTypeField.getName());
                            thingFieldsMap.put("type", thingTypeField.getDataType().getId());
                            thingFieldsMap.put("value", value);

                            thingFieldsListRes.add(thingFieldsMap);
                        } else {
                            thingFieldsMap.put("name", thingTypeField.getName());
                            thingFieldsMap.put("type", thingTypeField.getDataType().getId());
                            thingFieldsMap.put("value", "");
                            thingFieldsListRes.add(thingFieldsMap);
                        }

                    }
                }
            }

        }
        response.put("thingType", thing.getThingType().publicMap());
        response.put("fields", thingFieldsListRes);
        return response;
    }

    public Map<String, Map<String, Object>> getCacheData() {

        List<Thing> things;
        //          Uncomment to debug a thing.
//        boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString()
//                .indexOf("-agentlib:jdwp") > 0;
//
//        if (isDebug) {
//            Collection arrayThingsTest = new ArrayList<>();
//            for (long i = 773000; i <= 773000 ; i++) {
//                arrayThingsTest.add(i);
//            }
//            things = ThingService.getInstance().selectAllIn("", arrayThingsTest);
//        } else {
        things = ThingService.getInstance().selectAllThings();
//        }
        logger.info("Number of Migration Things: " + things.size());

        //Build Cache
        Map<String, Map<String, Object>> cache = new HashMap<>();

        //ThingType Cache
        Map<String, Object> thingTypeCache = new HashMap<>();
        Map<String, Object> thingTypeFieldCache = new HashMap<>();
        for (ThingType thingType : ThingTypeService.getInstance().getAllThingTypes()) {
            thingTypeCache.put(thingType.getThingTypeCode(), thingType);

            for (ThingTypeField thingTypeField : thingType.getThingTypeFields()) {
                thingTypeFieldCache.put(thingTypeField.getId().toString(), thingTypeField);
            }
        }
        cache.put("thingType", thingTypeCache);
        cache.put("thingTypeField", thingTypeFieldCache);


        //Thing Cache
        Map<String, Object> thingCache = new HashMap<>();
        Map<String, Object> thingCacheById = new LinkedHashMap<>();
        Map<String, Object> thingParentCache = new HashMap<>();
        Map<String, Object> thingChildrenCache = new HashMap<>();

        for (Thing thing : things) {
            thingCache.put(thing.getSerial(), thing);
            thingCacheById.put(thing.getId().toString(), thing);

            if (thing.getThingType().getChildren().size() > 0 || thing.getThingType().getParents().size() > 0) {
                //Parent
                Thing thingParent = thing.getParent();
                if (thingParent != null) {
                    thingParentCache.put(thing.getSerial(), thingParent);
                }
                //Children
                List<Thing> childrenList = getChildrenList(thing);
                if (childrenList != null && childrenList.size() > 0) {
                    thingChildrenCache.put(thing.getSerial(), childrenList);
                }
            }
        }
        cache.put("thing", thingCache);
        cache.put("thingById", thingCacheById);
        cache.put("thingParent", thingParentCache);
        cache.put("thingChildren", thingChildrenCache);


        //Zone Cache
        Map<String, Object> zoneCacheByCode = new HashMap<>();
        Map<String, Object> zoneCacheByName = new HashMap<>();
        Map<String, Object> zoneCacheById = new HashMap<>();
        for (Zone zone : ZoneService.getInstance().getZones()) {
            zoneCacheByCode.put(zone.getCode(), zone);
            zoneCacheByName.put(zone.getName(), zone);
            zoneCacheById.put(zone.getId().toString(), zone);
        }
        cache.put("zone", zoneCacheByCode);
        cache.put("zoneByName", zoneCacheByName);
        cache.put("zoneById", zoneCacheById);

        //Shift Cache
        Map<String, Object> shiftCache = new HashMap<>();
        Map<String, Object> shiftCacheByName = new HashMap<>();
        Map<String, Object> shiftCacheById = new HashMap<>();
        for (Shift shift : ShiftService.getShiftDAO().selectAll()) {
            shiftCache.put(shift.getCode(), shift);
            shiftCacheByName.put(shift.getName(), shift);
            shiftCacheById.put(shift.getId().toString(), shift);
        }
        cache.put("shift", shiftCache);
        cache.put("shiftByName", shiftCacheByName);
        cache.put("shiftById", shiftCacheById);

        //LogicalReader Cache
        Map<String, Object> lrCache = new HashMap<>();
        for (LogicalReader lr : LogicalReaderService.getLogicalReaderDAO().selectAll()) {
            lrCache.put(lr.getCode(), lr);
        }
        cache.put("logicalReader", lrCache);

        //Group Cache
        Map<String, Object> groupCache = new HashMap<>();
        for (Group group : GroupService.getGroupDAO().selectAll()) {
            groupCache.put(group.getCode(), group);
        }
        cache.put("group", groupCache);

        //Dattype Cache
        Map<String, Object> dtCache = new HashMap<>();
        for (DataType dt : DataTypeService.getDataTypeDAO().selectAll()) {
            dtCache.put(dt.getId().toString(), dt);
        }
        cache.put("dataType", dtCache);

        return cache;
    }

    /*********************
     * Get Date based o0n Long timestamp
     *********************/
    public Date getDate(String dateTimestamp) {
        Date response;
        if (dateTimestamp != null) {
            try {
                long dateLong = Long.parseLong(dateTimestamp.toString());
                response = new Date(dateLong);
            } catch (NumberFormatException e) {
                response = new Date();
            }
        } else {
            response = new Date();
        }
        return response;
    }


    public Map<String, Object> getUdfValues(Long thingId) {
        Map<String, Object> udfValues = new HashMap<String, Object>();
        String whereThing = "_id=" + thingId;
        List<String> filterFields = new ArrayList<>();
        filterFields.add("*");
        Map<String, Object> udfResult = ThingMongoDAO.getInstance().getThingUdfValues(whereThing, null, filterFields, null);
        if (null != udfResult && !udfResult.isEmpty()) {
            List<Map<String, Object>> udfValuesList = (List<Map<String, Object>>) udfResult.get("results");
            // convert udfValues to Map
            if (null != udfValuesList && !udfValuesList.isEmpty()) {
                for (Map<String, Object> udfValuesAsMap : udfValuesList) {
                    for (Map.Entry<String, Object> entry : udfValuesAsMap.entrySet()) {
                        Map<String, Object> newEntry = new HashMap<String, Object>();
                        newEntry.put("value", entry.getValue());
                        udfValues.put(entry.getKey(), newEntry);
                    }
                }
            }
        }
        return udfValues;
    }

    public void updateFormulaValuesForParent(
            Stack<Long> recursivelyStack, Thing thing, Date transactionDate,
            boolean executeTickle, User currentUser, boolean fillSource) throws Exception {
        List<ThingTypeField> thingTypeFields = thing.getThingTypeFieldByType(ThingTypeField.Type.TYPE_FORMULA.value);
        if (null != thingTypeFields && !thingTypeFields.isEmpty()) {
            Map<String, Object> formulaValues = new HashMap<>();
            for (ThingTypeField thingTypeField : thingTypeFields) {
                String formula = thingTypeField.getDefaultValue();
                // replace value for formula UDFs
                String replacedFormula = StringUtils.replace(formula, "${count(", "${countParentChild(id,");
//                replacedFormula = StringUtils.replace(replacedFormula, ")", ",tenantCode)");
                Object value = FormulaUtil.getFormulaValues(null, thing, replacedFormula);
                if (null != value) {
                    Map<String, Object> properties = new HashMap<>();
                    properties.put("thingTypeFieldId", thingTypeField.getId());
                    properties.put("time", transactionDate);
                    properties.put("value", value);
                    formulaValues.put(thingTypeField.getName(), properties);
                }
            }
            // update udf value for the parent
            ThingService.getInstance().update(
                    recursivelyStack
                    , thing // thingId
                    , thing.getThingType().getThingTypeCode() // thingTypeCode
                    , thing.getGroup().getHierarchyName(false) // groupHierarchyCode
                    , thing.getName() // name
                    , thing.getSerial() // serialNumber
                    , null // parent
                    , formulaValues // udfs
                    , null // children
                    , null // childrenUdf
                    , executeTickle // executeTickle
                    , false // validateVisibility
                    , transactionDate // transactionDate
                    , true // disableFMCLogic
                    , null // validations
                    , null // cache
                    , true // updateAndFlush
                    , true // recursivilyUpdate
                    , currentUser
                    , fillSource
            );
        }
    }

    public void updateFormulaValuesForThingTypeUdfs(
            Stack<Long> recursivelyStack, Map<String, Object> thingsWithFormula,
            Date transactionDate, boolean executeTickle, User currentUser, boolean fillSource) throws Exception {
        if (null != thingsWithFormula && !thingsWithFormula.isEmpty()) {
            for (Map.Entry<String, Object> entry : thingsWithFormula.entrySet()) {
                String udfName = entry.getKey();
                Thing thing = (Thing) entry.getValue();
                if (null != thing) {
                    List<ThingTypeField> thingTypeFields = thing.getThingTypeFieldByType(ThingTypeField.Type.TYPE_FORMULA.value);
                    if (null != thingTypeFields && !thingTypeFields.isEmpty()) {
                        Map<String, Object> udfCondition = new HashMap<>();
                        udfCondition.put("udfCondition", udfName + ".value._id=");
                        Map<String, Object> formulaValues = new HashMap<>();
                        for (ThingTypeField thingTypeField : thingTypeFields) {
                            String formula = thingTypeField.getDefaultValue();
                            // replace value for formula UDFs
                            String replacedFormula = StringUtils.replace(formula, "${count(", "${countThingTypeUDF(id,udfCondition,");
                            Object value = FormulaUtil.getFormulaValues(udfCondition, thing, replacedFormula);
                            if (null != value) {
                                Map<String, Object> properties = new HashMap<>();
                                properties.put("thingTypeFieldId", thingTypeField.getId());
                                properties.put("time", transactionDate);
                                properties.put("value", value);
                                formulaValues.put(thingTypeField.getName(), properties);
                            }
                        }
                        // update udf value for the parent
                        ThingService.getInstance().update(
                                recursivelyStack
                                , thing // thingId
                                , thing.getThingType().getThingTypeCode() // thingTypeCode
                                , thing.getGroup().getHierarchyName(false) // groupHierarchyCode
                                , thing.getName() // name
                                , thing.getSerial() // serialNumber
                                , null // parent
                                , formulaValues // udfs
                                , null // children
                                , null // childrenUdf
                                , executeTickle // executeTickle
                                , false // validateVisibility
                                , transactionDate // transactionDate
                                , true // disableFMCLogic
                                , null // validations
                                , null // cache
                                , true // updateAndFlush
                                , true // recursivilyUpdate
                                , currentUser
                                , fillSource
                        );
                    }
                }
            }
        }
    }

    public Map<String, Object> getThingsToEvaluateFormula(Map<String, Object> udfValues, List<ThingTypeField>
            thingTypeFields) {
        Map<String, Object> result = new HashMap<>();
        try {
            for (ThingTypeField thingTypeField : thingTypeFields) {
                if (null != udfValues && !udfValues.isEmpty() && null != udfValues.get(thingTypeField.getName()) &&
                        udfValues.get(thingTypeField.getName()) instanceof Map) {
                    if (null != ((Map) udfValues.get(thingTypeField.getName())).get("value")) {
                        if (((Map) udfValues.get(thingTypeField.getName())).get("value") instanceof BasicDBObject) {
                            BasicDBObject udfValue = (BasicDBObject) ((Map) udfValues.get(thingTypeField.getName())).get("value");
                            if (null != udfValue) {
                                Thing thing = ThingService.getInstance().get((Long) udfValue.get("_id"));
                                result.put((String) thingTypeField.getName(), thing);
                            }
                        }
                        if (((Map) udfValues.get(thingTypeField.getName())).get("value") instanceof String) {
                            String serialNumber = (String) ((Map) udfValues.get(thingTypeField.getName())).get("value");
                            ThingType thingType = ThingTypeService.getInstance().get(thingTypeField.getDataTypeThingTypeId());
                            if (null != serialNumber && !serialNumber.isEmpty() && null != thingType) {
                                Thing thing = getBySerialNumberWithFields(serialNumber, thingType);
                                if (null != thing) {
                                    result.put((String) thingTypeField.getName(), thing);
                                }
                            }
                        }
                    }
                }
            }
        } catch (NonUniqueResultException e) {
            throw new UserException("Error getting things to evaluate formula", e);
        }
        return result;
    }

    /***********************************************************
     * Get a list of map of things according to the values of the query
     ************************************************************/
    public List<Map<String, Object>> listThingsByFieldValues(
            List<String> filterFields
            , List<String> groupByFields
            , String whereFieldValue
            , String whereThing
            , Integer pageNumber
            , Integer pageSize) {
        //TODO CHANGE TO:
        // return ThingMongoDAO.getInstance().getThingUdfValues(whereThing, whereFieldValue, filterFields, groupByFields).get("results");
        List<Map<String, Object>> result = new ArrayList<>();

        // construct condition
        DBObject condition = null;
        if (StringUtils.isNotEmpty(whereThing) && StringUtils.isNotEmpty(whereFieldValue)) {
            condition = ThingMongoDAO.getInstance().getObjectCondition(
                    ThingMongoDAO.getInstance().parseWhereString(whereThing),
                    ThingMongoDAO.getInstance().parseWhereString(whereFieldValue), "$and");
        } else if (StringUtils.isNotEmpty(whereThing)) {
            condition = ThingMongoDAO.getInstance().parseWhereString(whereThing);
        } else if (StringUtils.isNotEmpty(whereFieldValue)) {
            condition = ThingMongoDAO.getInstance().parseWhereString(whereFieldValue);
        }

        if (!groupByFields.isEmpty()) {
            result = ThingMongoDAO.getInstance().executeGroupedByQuery(condition, groupByFields, null, null);
        }
        if (!filterFields.isEmpty() && groupByFields.isEmpty()) {
            Map<String, Object> lista = ThingMongoDAO.getInstance().executeQuery(condition, filterFields, pageNumber, pageSize, null, false, false);
            result = ((List<Map<String, Object>>) lista.get("list"));
        }
        return result;
    }

    public Map<String, Object> getThingFullFields(Long thingId, Long thingTypeId) {

        String whereThing = "_id=" + thingId;
        List<String> filterFields = new ArrayList<>();
        filterFields.add("*");
        Map<String, Object> udfResult = ThingMongoDAO.getInstance().getThingUdfValues(whereThing,
                null,
                filterFields,
                null);
        if (thingTypeId != null) {
            List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().getThingTypeField(thingTypeId);
            List<Map<String, Object>> udfValuesList = (List<Map<String, Object>>) udfResult.get("results");
            //                {
//                    udfResult.put(item.getName(), "");
//                }
            for (Map<String, Object> itemUdfValues : udfValuesList) {
                for (ThingTypeField item : thingTypeFields) {
                    if (itemUdfValues.get(item.getName()) == null) {
                        itemUdfValues.put(item.getName(), "");
                    }
                }
            }
            return udfResult;
        }
        return null;
    }

    /***************************************************************
     * This method contains all logic for get a list of things
     * This method is going to be called just for controllers
     ***************************************************************/
    public Map<String, Object> processListThings(
            Integer pageSize
            , Integer pageNumber
            , String order
            , String where
            , String extra
            , String only
            , String groupBy
            , Long visibilityGroupId
            , String upVisibility
            , String downVisibility
            , boolean treeView
            , User currentUser
            , boolean returnFavorite) {
        Map<String, Object> mapResponse = new HashMap<>();
        String serialNumber = null;
        Long thingTypeId = null;
        if (StringUtils.isNotEmpty(where)) {
            // get serial number
            if (StringUtils.contains(where, '/')) {
                Pattern p = Pattern.compile(Pattern.quote("serialNumber=/") + "(.*?)" + Pattern.quote("/"));
                Matcher m = p.matcher(where);
                while (m.find()) {
                    serialNumber = m.group(1);
                    break;
                }
            }
            // get thing type id
            if (StringUtils.contains(where, "thingTypeId")) {
                Pattern p = Pattern.compile(Pattern.quote("thingTypeId=") + "(.*?)" + Pattern.quote("&"));
                Matcher m = p.matcher(where);
                String thingTypeIdStr = null;
                while (m.find()) {
                    thingTypeIdStr = m.group(1);
                    break;
                }
                if (thingTypeIdStr == null) {
                    thingTypeIdStr = where.substring(where.lastIndexOf("=") + 1, where.length());
                }
                thingTypeId = Long.parseLong(thingTypeIdStr);
            }
        }

        Map<String, Object> queryForTree = new HashMap<>();
        long timeStamp = System.currentTimeMillis();
        //Get data of the user //rrccc
//        Subject subject = SecurityUtils.getSubject();
//        User currentUser = (User) subject.getPrincipal();
        try {
            //Get groups for visibility
            visibilityGroupId = visibilityGroupId == null ? currentUser.getActiveGroup().getId() : visibilityGroupId;
            Map<Long, List<ThingType>> groups = VisibilityThingUtils.calculateThingsVisibility(visibilityGroupId);
//            if((groups == null) || (groups.isEmpty())) {
            if (groups == null) {
                throw new UserException("Group of the user is not valid.");
            }
            logger.info("Group visibility " + groups.keySet());
            //Is tree view
            Map<String, Object> nativeThingTypeProperties = getNativeThingTypeProperties();
            if (treeView) {
                queryForTree = this.getQueryForTreeView();
                // exclude thingTypes that are UDFs for other thingTypes and they aren't parents
                for (Map.Entry<String, Object> entryMap : nativeThingTypeProperties.entrySet()) {
                    ThingType thingType = ThingTypeService.getInstance().getByCode(entryMap.getKey());
                    for (Map.Entry<Long, List<ThingType>> entry : groups.entrySet()) {
                        List<ThingType> thingTypes = entry.getValue();
                        //thingTypes.remove(thingType);
                    }
                }
            }
            //concat native thing type name properties to only string
            Set<String> nativeThingTypeNames = new HashSet();
            for (Map.Entry<String, Object> entry : nativeThingTypeProperties.entrySet()) {
                nativeThingTypeNames.add((String) entry.getValue());
            }
            if (only != null) {
                for (String nativeThingTypeName : nativeThingTypeNames) {
                    only = only + "," + nativeThingTypeName + ".value._id";
                }
            }
            //Get List of things
            Map<String, Object> result = ThingMongoDAO.getInstance().getListOfThings(pageSize, pageNumber, order, where, extra, only,
                    groupBy, visibilityGroupId, upVisibility, downVisibility, groups, queryForTree, serialNumber, thingTypeId, treeView, returnFavorite);
            if (result != null && result.size() > 0) {
                mapResponse.put("total", result.get("total"));
                mapResponse.put("results", result.get("list"));
            }

            logger.info("Done with getting things in  " + (System.currentTimeMillis() - timeStamp));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new UserException(e.getMessage(), e);
        }
        return mapResponse;
    }

    public Map<String, Object> getNativeThingTypeProperties() {
        Map<String, Object> result = new HashMap<>();
        Pagination pagination = new Pagination(1, -1);
        BooleanBuilder be1 = new BooleanBuilder();
        be1 = be1.and(QThingTypeField.thingTypeField.dataTypeThingTypeId.isNotNull());
        List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().listPaginated(be1, pagination, null);
        for (ThingTypeField thingTypeField : thingTypeFields) {
            ThingType thingType = ThingTypeService.getInstance().get(thingTypeField.getDataTypeThingTypeId());
            if (!thingType.isIsParent())
                result.put(thingType.getCode(), thingTypeField.getName());
        }
        return result;
    }

    /********************************************
     * This method arm the query in order to filter data
     * of the tree view
     *******************************************/
    public Map<String, Object> getQueryForTreeView() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Long> nameUdfParent = new HashMap<>();
        Map<String, Long> parentUDFs = new HashMap<>();

        //Get Data of parent udf's
        List<Long> parentUdfIds = new ArrayList<>();
        Pagination pagination = new Pagination(1, -1);
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QThingType.thingType.isParent.isTrue());
        List<ThingType> lstThingsRepeated = ThingTypeService.getInstance().listPaginated(be, pagination, null);
        if (lstThingsRepeated != null && lstThingsRepeated.size() > 0) {
            for (ThingType thingsRepeatedList : lstThingsRepeated) {
                parentUdfIds.add(thingsRepeatedList.getId());
                parentUDFs.put(thingsRepeatedList.getCode(), thingsRepeatedList.getId());
            }
        }

        //get names of children properties
        Map<String, Object> thingTypeIsParentMap = new HashMap<>();
        if (!parentUDFs.isEmpty()) {
            for (Map.Entry<String, Long> entry : parentUDFs.entrySet()) {
                BooleanBuilder be4 = new BooleanBuilder();
                be4 = be4.and(QThingTypeField.thingTypeField.dataTypeThingTypeId.in((Long) entry.getValue()));
                List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().listPaginated(be4, pagination, null);
                List<String> nameUDFChildrens = new ArrayList<>();
                for (ThingTypeField thingTypeField : thingTypeFields) {
                    BooleanBuilder be3 = new BooleanBuilder();
                    be3 = be3.and(QThingType.thingType.thingTypeFields.contains(thingTypeField));
                    List<ThingType> thingTypes = ThingTypeService.getInstance().listPaginated(be3, pagination, null);
                    if (thingTypes != null && !thingTypes.isEmpty()) {
                        for (ThingType thingType : thingTypes)
                            nameUDFChildrens.add(thingType.getCode() + "_children");
                    }
                    thingTypeIsParentMap.put(entry.getKey(), nameUDFChildrens);
                }
            }
            result.put("thingTypeIsParentMap", thingTypeIsParentMap);
        }

        //get names of UDF native thing type properties
        Map<String, List<String>> thingTypeIsNotParentMap = new HashMap<>();
        BooleanBuilder be1 = new BooleanBuilder();
        be1 = be1.and(QThingTypeField.thingTypeField.dataTypeThingTypeId.isNotNull());
        List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().listPaginated(be1, pagination, null);
        Set<String> thingTypeCodes = new HashSet<String>();
        for (ThingTypeField thingTypeField : thingTypeFields) {
            thingTypeCodes.add(thingTypeField.getThingType().getCode());
        }
        for (String thingTypeCode : thingTypeCodes) {
            List<String> udfNames = new ArrayList<>();
            for (ThingTypeField thingTypeField : thingTypeFields) {
                if (thingTypeField.getThingType().getCode().equals(thingTypeCode)) {
                    ThingType dataTypeThing = ThingTypeService.getInstance().get(thingTypeField.getDataTypeThingTypeId());
                    if (!dataTypeThing.isIsParent())
                        udfNames.add(thingTypeField.getName());
                }
            }
            thingTypeIsNotParentMap.put(thingTypeCode, udfNames);
        }

        result.put("thingTypeIsNotParentMap", thingTypeIsNotParentMap);

        //get names of properties who are parent Udf's
        if (!parentUdfIds.isEmpty()) {
            BooleanBuilder be2 = new BooleanBuilder();
            be2 = be2.and(QThingTypeField.thingTypeField.dataTypeThingTypeId.in(parentUdfIds));
            List<ThingTypeField> lstThingsFieldRepeated = ThingTypeFieldService.getInstance().listPaginated(be2, pagination, null);
            if (lstThingsFieldRepeated != null && lstThingsFieldRepeated.size() > 0) {
                for (ThingTypeField thingsRepeatedList : lstThingsFieldRepeated) {
                    nameUdfParent.put(thingsRepeatedList.getName(), thingsRepeatedList.getDataTypeThingTypeId());
                }
            }
            result.put("nameUdfParent", nameUdfParent);
        }

        //get thingTypeCodes that are defined as UDF in other thingType (not parent)
        BooleanBuilder be2 = new BooleanBuilder();
        be2 = be2.and(QThingTypeField.thingTypeField.dataTypeThingTypeId.isNotNull());
        List<ThingTypeField> nativeThingTypeFields = ThingTypeFieldService.getInstance().listPaginated(be2, pagination, null);
        Set<Long> nativeThingTypeCodes = new HashSet<>();
        for (ThingTypeField thingTypeField : nativeThingTypeFields) {
            ThingType thingType = ThingTypeService.getInstance().get(thingTypeField.getDataTypeThingTypeId());
            if (!thingType.isIsParent())
                nativeThingTypeCodes.add(thingType.getId());
        }
        result.put("nativeThingTypeIsNotParentMap", nativeThingTypeCodes);

        List<ThingTypeMap> data = ThingTypeMapService.getInstance().listPaginated(null, pagination, null);
        if (data != null && data.size() > 0) {
            for (ThingTypeMap thingTypeMap : data) {
                thingTypeIsParentMap.put(thingTypeMap.getParent().getCode(), null);
            }
            result.put("thingTypeIsParentMap", thingTypeIsParentMap);
        }

        return result;
    }

    /**
     * This method executes formula in all children
     *
     * @param currentUser
     * @param originalUdfMap
     */
    public void updateExpressionsForChildren(
            Stack<Long> recursivelyStack, Thing thing, String thingTypeCode, User currentUser, Map<String, Object> originalUdfMap, boolean fillSource, Date transactionDate) {
        try {
            //Get List of fields to update
            //list of Native Children  and children UDF, just in a unique list
            Map<ThingType, List<ThingTypeField>> lstThingTypeField = getListChildrenFields(thing.getId(), thingTypeCode);
            if (lstThingTypeField != null && !lstThingTypeField.isEmpty()) {
                Map<ThingType, List<ThingTypeField>> finalFields = getFieldsToUpdate(lstThingTypeField, thing.getId(), originalUdfMap);
                Map<String, Object> mapResponse;
                //Iterate final fields
                for (Map.Entry<ThingType, List<ThingTypeField>> entry : finalFields.entrySet()) {
                    ThingType thingType = entry.getKey();
                    //Get all children of the thing
                    String where = "thingTypeCode='" + thingType.getCode() +
                            "'&groupCode='" + thingType.getGroup().getCode() + "'" +
                            "&parent.serialNumber='" + thing.getSerialNumber() + "'";
                    String only = "_id,thingTypeCode,name,serialNumber";
                    mapResponse = ThingService.getInstance().processListThings(
                            10000 //pageSize
                            , 1     //pageNumber
                            , null  //order
                            , where //where
                            , null  //extra
                            , only  //only
                            , null  //groupBy
                            , null //visibilityGroupId
                            , null //upVisibility
                            , null //downVisibility
                            , false /*treeView*/
                            , currentUser
                            , false);

                    //If it has children, they have to be updated with the value of the Parent Thing in expression UDFs
                    List<Map<String, Object>> result = (List<Map<String, Object>>) mapResponse.get("results");
                    if (result != null && result.size() > 0) {
                        for (Object things : result) {
                            Date storageDate = transactionDate;
                            Map<String, Object> thingMap = (Map<String, Object>) things;

                            this.update(
                                    recursivelyStack
                                    , ThingService.getInstance().get(Long.parseLong(thingMap.get("_id").toString()))
                                    , (String) thingMap.get("thingTypeCode")
                                    , null //group
                                    , null //name
                                    , (String) thingMap.get("serialNumber")
                                    , null //parent
                                    , null//udfsToUpdate//this.udf//
                                    , null //children
                                    , null //childrenUdf
                                    , true //executeTickle
                                    , true //validateVisibility
                                    , storageDate //transactionDate
                                    , false // disableFMCLogic
                                    , null // validations
                                    , null // cache
                                    , true // updateAndFlush
                                    , false
                                    , currentUser // recursivilyUpdate
                                    , fillSource);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            if (ex instanceof UserException) {
                logger.warn(ex.getMessage());
            } else if (ex instanceof ServerException) {
                logger.error(ex.getMessage());
            } else {
                logger.error(ex.getMessage(), ex);
            }
        }
    }

    public void updateExpressionsForChildrenUDF(
            Stack<Long> recursivelyStack, ThingType thingType, Thing thing, User currentUser,
            Map<String, Object> originalUdfMap, boolean fillSource, Date transactionDate) {
        Map<String, Object> mapResponse;
        Set<String> fields = new HashSet<>();
        List<ThingType> thingTypeDataFromCache = ThingTypeService.getInstance().getThingTypeDataFromCache(thingType.getId());
        if (thingTypeDataFromCache != null) {
            thingTypeDataFromCache.stream()
                    .map(tt ->
                            DataTypeService.getInstance()
                                    .getThingTypeFieldsFromCache(tt.getThingTypeCode(), ThingTypeField.Type.TYPE_FORMULA.value))
                    .filter(ttl -> ttl != null && !ttl.isEmpty())
                    .flatMap(ttl -> ttl.stream())
                    .forEach(ttf -> {
                        if(originalUdfMap != null){
                            originalUdfMap.entrySet().stream()
                                    .filter(e -> ttf.getDefaultValue().contains(e.getKey()))
                                    .forEach(e -> fields.add(ttf.getName()));
                        }
                    });
        }

        /*List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getThingTypeFieldsByDataTypeThingType(thingType.getId());
        Set<String> fields = new HashSet<>();
        for (ThingTypeField thingTypeField : thingTypeFields) {
            // find expression thingTypeFields
            List<ThingTypeField> thingTypeFieldsFormula = thingTypeField.getThingType().getThingTypeFieldsByType(
                    Long.valueOf(ThingTypeField.Type.TYPE_FORMULA.value));
            if ((thingTypeFieldsFormula != null) && (originalUdfMap != null)) {
                for (ThingTypeField ttField : thingTypeFieldsFormula) {
                    for (Map.Entry<String, Object> entryUdf : originalUdfMap.entrySet()) {
                        if (ttField.getDefaultValue().contains(entryUdf.getKey())) {
                            fields.add(thingTypeField.getName());
                        }
                    }
                }
            }
        }*/
        for (String field : fields) {
            String where = field + ".value._id=" + thing.getId();
            String only = "_id,thingTypeCode,name,serialNumber";
            mapResponse = ThingService.getInstance().processListThings(
                    10000 //pageSize
                    , 1     //pageNumber
                    , null  //order
                    , where //where
                    , null  //extra
                    , only  //only
                    , null  //groupBy
                    , null //visibilityGroupId
                    , null //upVisibility
                    , null //downVisibility
                    , false /*treeView*/
                    , currentUser
                    , true);

            List<Map<String, Object>> result = (List<Map<String, Object>>) mapResponse.get("results");
            if (result != null && result.size() > 0) {
                for (Object things : result) {
                    Date storageDate = transactionDate;
                    Map<String, Object> thingMap = (Map<String, Object>) things;
                    Thing thingToUpdate = ThingService.getInstance().get(Long.parseLong(thingMap.get("_id").toString()));
                    this.update(
                            recursivelyStack
                            , thingToUpdate
                            , (String) thingMap.get("thingTypeCode")
                            , null //group
                            , null //name
                            , (String) thingMap.get("serialNumber")
                            , null //parent
                            , null //udfsToUpdate
                            , null //children
                            , null //childrenUdf
                            , true //executeTickle
                            , true //validateVisibility
                            , storageDate //transactionDate
                            , false // disableFMCLogic
                            , null // validations
                            , null // cache
                            , true // updateAndFlush
                            , false
                            , currentUser// recursivilyUpdate
                            , fillSource);
                    // update children in the third level
                    updateExpressionsForChildren(recursivelyStack, thingToUpdate, null, currentUser, originalUdfMap, fillSource, transactionDate);
                }
            }
        }
    }

    /**
     * This method returns all list of Native Children  and children UDF, just in a unique list
     *
     * @param thingId
     * @return
     */
    public Map<ThingType, List<ThingTypeField>> getListChildrenFields(Long thingId, String thingTypeCode) {
        Map<ThingType, List<ThingTypeField>> lstThingTypeField = new HashMap<>();

        //Get List of native children thingTypes
        thingTypeCode = StringUtils.isEmpty(thingTypeCode) ?
                ThingService.getInstance().get(thingId).getThingType().getThingTypeCode() : thingTypeCode;
        //////List<ThingType> lstThingType = thing.getThingType().getChildren();
        List<ThingType> lstThingType = ThingTypeService.getInstance().getChildrenFromCache(thingTypeCode);

        //Get List of children UDFs thingTypes
        if (lstThingType != null && !lstThingType.isEmpty()) {
            //lstThingTypeField = FormulaUtil.getListOfFieldsWithExpression(lstThingType);
            lstThingTypeField = ThingTypeFieldService.getInstance().getListOfFieldsWithExpressionCache(lstThingType);
        }
        return lstThingTypeField;
    }

    /**
     * This method get a list of thingTypeFields Filters only things with the value of key
     *
     * @param lstThingTypeField
     * @param thingId
     * @param udf               udf's of the parent thing
     * @return
     */
    public Map<ThingType, List<ThingTypeField>> getFieldsToUpdate(
            Map<ThingType, List<ThingTypeField>> lstThingTypeField
            , Long thingId
            , Map<String, Object> udf) {
        Map<ThingType, List<ThingTypeField>> finalFields = new HashMap<>();
        if (lstThingTypeField != null && lstThingTypeField.size() > 0 && udf != null) {
            //Filter only things with the value of the key
            for (Map.Entry<ThingType, List<ThingTypeField>> entry : lstThingTypeField.entrySet()) {
                ThingType thingType = entry.getKey();
                List<ThingTypeField> lstTTFieldValue = entry.getValue();
                List<ThingTypeField> lstNewTTFieldValue = new ArrayList<>();
                for (ThingTypeField thingTypeField : lstTTFieldValue) {
                    for (Map.Entry<String, Object> entryUdf : udf.entrySet()) {
                        if (thingTypeField.getDefaultValue().contains(entryUdf.getKey())) {
                            lstNewTTFieldValue.add(thingTypeField);
                        }
                    }
                }
                if (lstNewTTFieldValue.size() > 0) {
                    finalFields.put(thingType, lstNewTTFieldValue);
                }
            }
        }
        return finalFields;
    }

    /**
     * This method filters only thing type field with have the same udfValues
     *
     * @param finalFields
     * @return
     */
    public static Map<String, Object> getThingTypeFieldsByUdf(
            Map<ThingType, List<ThingTypeField>> lstThingTypeField
            , Map<ThingType, List<ThingTypeField>> finalFields
            , Thing thing
            , Map<String, Object> udf
            , Map<String, Object> originalUdfMap) {
        Map<String, Object> result = new HashMap<>();
        if (finalFields != null && finalFields.size() > 0) {
            for (Map.Entry<ThingType, List<ThingTypeField>> entry : lstThingTypeField.entrySet()) {
                ThingType thingType = entry.getKey();
                List<ThingTypeField> lstTTFieldValue = entry.getValue();

                if (lstTTFieldValue != null && lstTTFieldValue.size() > 0) {
                    Map<String, Object> values = FormulaUtil.getMapValues(udf);
                    for (ThingTypeField thingTypeField : lstTTFieldValue) {
                        for (Map.Entry<String, Object> entryData : values.entrySet()) {
                            //Check Expression Children equals udfValue
                            if (thingTypeField.getDefaultValue().contains("${parent." + entryData.getKey())) {
                                Map<String, Object> value = new HashMap<>();

                                value.put("value", entryData.getValue());
                                //Map<String, Object> data = new HashMap<>();
                                String formula = thingTypeField.getDefaultValue().replace("parent.", "");
                                Object dataFormula = FormulaUtil.getFormulaValues(
                                        (Map<String, Object>) originalUdfMap.get("thingTypeFieldResponse")
                                        , thing
                                        , formula);
                                value.put("value", dataFormula);
                                result.put(thingTypeField.getName(), value);
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Delete attachment files of the HD
     */
    public void deleteAttachments(ThingType thingType, List<ThingTypeField> ttfAttachments, Map<String, Object> udfMap) {
        if (ttfAttachments != null && !ttfAttachments.isEmpty()) {
            StringBuilder fileSystemPath = new StringBuilder();
            String fileSystemPathOrigin = null;
            JSONParser parser = new JSONParser();
            for (ThingTypeField thingTypeField : ttfAttachments) {
                JSONObject jsonObject = new JSONObject();
                Map<String, Object> data = (Map<String, Object>) udfMap.get(thingTypeField.getName());
                try {
                    jsonObject = (JSONObject) parser.parse(data.get("value").toString());
                } catch (Exception e) {
                    logger.error("It is not possible to parse Thing's data to JSON. " + e.getMessage() + "-" + e);
                }
                //Delete all attachments of the thingTypeField
                try {
                    JSONObject configuration = (JSONObject) parser.parse(jsonObject.get("configuration").toString());
                    if (configuration != null && configuration.size() > 0) {
                        fileSystemPathOrigin = configuration.get("fileSystemPath").toString();
                    }
                } catch (Exception e) {
                    logger.error("It is not possible to parse Thing's configuration to JSON. ", e);
                }
                if (fileSystemPathOrigin != null) {
                    fileSystemPath.append(fileSystemPathOrigin);
                    String hierarchyName = thingType.getGroup().getHierarchyName(false);
                    String[] hierarchyNameArray = hierarchyName.split(">");
                    for (String name : hierarchyNameArray) {
                        if (name.length() > 0) {
                            fileSystemPath.append("/" + name);
                        }
                    }
                    Map<String, Object> udfMapId = (Map<String, Object>) udfMap.get("_id");
                    if (udfMapId != null && udfMapId.size() > 0) {
                        fileSystemPath.append("/").append(udfMapId.get("value"));
                        File file = new File(fileSystemPath.toString());
                        if (file.isDirectory()) {
                            logger.info("Number of files to delete of " + fileSystemPath + " file: " + file.listFiles().length);
                        }
                        try {
                            AttachmentService.getInstance().removeFile(fileSystemPath.toString());
                        } catch (Exception e) {
                            logger.error("It is not possible to remove Thing folder from the HD."
                                    + e.getMessage()
                                    + "-"
                                    + e);
                        }
                    }
                }
            }
        }
    }


    /**
     * Delete attachment files of the HD
     */
    public void deleteBatchAttachments(ThingType thingType, Map<String, Object> udfMap, List<ThingTypeField> lstThingTypeField) {
        if (thingType.getThingTypeFields() != null && thingType.getThingTypeFields().size() > 0) {
            StringBuilder fileSystemPath = new StringBuilder();
            String fileSystemPathOrigin = null;
            JSONParser parser = new JSONParser();
            for (ThingTypeField thingTypeField : lstThingTypeField) {
                if (thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_ATTACHMENTS.value) == 0) {
                    JSONObject jsonObject = new JSONObject();
                    Map<String, Object> data = (Map<String, Object>) udfMap.get(thingTypeField.getName());
                    try {
                        jsonObject = (JSONObject) parser.parse(data.get("value").toString());
                    } catch (Exception e) {
                        logger.error("It is not possible to parse Thing's data to JSON. " + e.getMessage() + "-" + e);
                    }
                    //Delete all attachments of the thingTypeField
                    try {
                        JSONObject configuration = (JSONObject) parser.parse(jsonObject.get("configuration").toString());
                        if (configuration != null && configuration.size() > 0) {
                            fileSystemPathOrigin = configuration.get("fileSystemPath").toString();
                        }
                    } catch (Exception e) {
                        logger.error("It is not possible to parse Thing's configuration to JSON. ", e);
                    }
                    if (fileSystemPathOrigin != null) {
                        fileSystemPath.append(fileSystemPathOrigin);
                        String hierarchyName = thingType.getGroup().getHierarchyName(false);
                        String[] hierarchyNameArray = hierarchyName.split(">");
                        for (String name : hierarchyNameArray) {
                            if (name.length() > 0) {
                                fileSystemPath.append("/" + name);
                            }
                        }
                        Map<String, Object> udfMapId = (Map<String, Object>) udfMap.get("_id");
                        if (udfMapId != null && udfMapId.size() > 0) {
                            fileSystemPath.append("/").append(udfMapId.get("value"));
                            File file = new File(fileSystemPath.toString());
                            if (file.isDirectory()) {
                                logger.info("Number of files to delete of " + fileSystemPath + " file: " + file.listFiles().length);
                            }
                            try {
                                AttachmentService.getInstance().removeFile(fileSystemPath.toString());
                            } catch (Exception e) {
                                logger.error("It is not possible to remove Thing folder from the HD."
                                        + e.getMessage()
                                        + "-"
                                        + e);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @param value
     * @return true if it is an AlphaNumeric value
     */
    public boolean isAlphaNumeric(String value) {
        Pattern pattern = Pattern.compile("^[0-9a-zA-Z]+$");
        Matcher matcher = pattern.matcher(value);
        return (matcher.matches());
    }


    /***************************************************************
     * This method contains all logic for get a list of things
     * This method is going to be called just for controllers
     ***************************************************************/
    public List<Map<String, Object>> processQueryThings(
            String query,
            String projection,
            String sort,
            Integer skip,
            Integer limit) {
        List<Map<String, Object>> response = new ArrayList<>();
        BasicDBObject mongoQuery = new BasicDBObject();
        BasicDBObject mongoProjection = new BasicDBObject();
        BasicDBObject mongoSort = new BasicDBObject();
        if (StringUtils.isNotEmpty(query)) {
            try {
                mongoQuery.putAll((DBObject) JSON.parse(query));
            } catch (JSONParseException e) {
                logger.error("Query must be in JSON format.");
                throw new UserException("Query must be in JSON format.", e);
            }
        }
        if (StringUtils.isNotEmpty(projection)) {
            try {
                mongoProjection.putAll((DBObject) JSON.parse(projection));
            } catch (JSONParseException e) {
                logger.error("Projection must be in JSON format.");
                throw new UserException("Projection must be in JSON format.", e);
            }
        }
        if (StringUtils.isNotEmpty(sort)) {
            try {
                mongoSort.putAll((DBObject) JSON.parse(sort));
            } catch (JSONParseException e) {
                logger.error("Sort must be in JSON format.");
                throw new UserException("Sort must be in JSON format.", e);
            }
        }
        if (skip == null) {
            skip = 0;
        }
        if (limit == null) {
            limit = 0;
        }

        DBCollection things = MongoDAOUtil.getInstance().things;
        DBCursor cursor = (DBCursor) things.find(mongoQuery, mongoProjection).sort(mongoSort).skip(skip).limit
                (limit).iterator();
        while (cursor.hasNext()) {
            response.add(cursor.next().toMap());
        }
        return response;
    }

    public Thing getThingById(Long id) {
        return getThingDAO().selectById(id);
    }

    /**
     * @param zone
     * @param zoneDb
     * @param nativeObject
     * @param storeDate
     * @return response object by zone logic
     */
    public Map<String, Object> getResponseObjectByZone(Zone zone, DBObject zoneDb, Object nativeObject, Date storeDate) {
        Map<String, Object> nativeObjectMap;
        DBObject zoneValue = (DBObject) zoneDb.get("value");
        String zoneType = zoneValue.get("zoneType").toString();
        String facilityMap = zoneValue.get("facilityMap").toString();
        String zoneGroup = zoneValue.get("zoneGroup").toString();
        nativeObjectMap = ((Zone) nativeObject).publicMapSummarized();
        if (!zoneType.equals(zone.getZoneType().getZoneTypeCode())) {
            nativeObjectMap.put("zoneTypeTime", storeDate);
        } else {
            nativeObjectMap.put("zoneTypeTime", zoneValue.get("zoneTypeTime"));
        }
        if (!facilityMap.equals(zone.getLocalMap().getName())) {
            nativeObjectMap.put("facilityMapTime", storeDate);
        } else {
            nativeObjectMap.put("facilityMapTime", zoneValue.get("facilityMapTime"));
        }
        if (!zoneGroup.equals(zone.getZoneGroup().getName())) {
            nativeObjectMap.put("zoneGroupTime", storeDate);
        } else {
            nativeObjectMap.put("zoneGroupTime", zoneValue.get("zoneGroupTime"));
        }
        return nativeObjectMap;
    }

    public Tuple getDataForBulk(Long thingId) {
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        return query.from(QThing.thing)
                .join(QThing.thing.thingType, QThingType.thingType)
                .join(QThing.thing.group, QGroup.group)
                .where(QThing.thing.id.eq(thingId))
                .setCacheable(true)
                .singleResult(QThing.thing, QThingType.thingType.thingTypeCode, QGroup.group.hierarchyName);
    }

    public void updateThingMySQL(Thing thing, boolean updateThingInMysql, boolean updateAndFlush) {
        try {
            if (updateThingInMysql) {
                if (updateAndFlush) {
                    getThingDAO().update(thing);
                } else {
                    getThingDAO().updateWithoutFlush(thing);
                }
            } else {
                if (updateAndFlush) {
                    getThingDAO().getSession().flush();
                }
                logger.debug("Nothing to update in MySQL for thing " + thing.getSerial());
            }
        } catch (ConstraintViolationException e) {
            logger.error("Error occurred while updating thing '"
                    + thing.getSerialNumber() + "', thing already exists", e);
            throw new UserException("Error occurred while updating thing '"
                    + thing.getSerialNumber() + "', thing already exists", e);
        } catch (Exception e) {
            logger.error("Error occurred while updating thing '"
                    + thing.getSerialNumber() + "' in mysql", e);
            throw new UserException("Error occurred while updating thing '"
                    + thing.getSerialNumber() + "' in mysql", e);
        }
    }
}
