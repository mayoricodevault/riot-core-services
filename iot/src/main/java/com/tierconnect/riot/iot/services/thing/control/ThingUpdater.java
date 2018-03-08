package com.tierconnect.riot.iot.services.thing.control;

import com.mongodb.DBObject;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.RecentService;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.dao.util.FormulaUtil;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.job.ChangedFieldsServices;
import com.tierconnect.riot.iot.job.SnapshotFixerJobServices;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.iot.services.thing.entity.CrudParameters;
import com.tierconnect.riot.iot.utils.Cache;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.subject.Subject;

import java.util.*;

import static com.tierconnect.riot.commons.Constants.SERIAL_NUMBER;
import static com.tierconnect.riot.commons.Constants.THING_TYPE_CODE;

/**
 * Created by vealaro on 8/15/17.
 */
public class ThingUpdater {
    private static Logger logger = Logger.getLogger(ThingUpdater.class);
    private static final ThingUpdater INSTANCE = new ThingUpdater();
    private static final String OLDER_THING = "THING.OLDER";

    private ThingUpdater() {
    }

    public static ThingUpdater getInstance() {
        return INSTANCE;
    }

    /*private TimerUtil initTimerUtil() {
        List<String> laps = new ArrayList<>();
        laps.add("[Update Process] : Thing existence validation");
        laps.add("[Update Process] : Validations");
        laps.add("[Update Process] : Remove Sequence Udf");
        laps.add("[Update Process] : Set thing properties");
        laps.add("[Update Process] : Association Parent Children new");
        laps.add("[Update Process] : Association Parent Children");
        laps.add("[Update Process] : Update mysql");
        laps.add("[Update Process] : Process Fields");
        laps.add("[Update Process] : Update UDF Map for Mongo");
        laps.add("[Update Process] : Update Thing Mongo");
        laps.add("[Update Process] : Update Children");
        laps.add("[Update Process] : Associate and disassociate children in Mongo");
        laps.add("[Update Process] : Associate disassociate Parent Mongo");
        laps.add("[Update Process] : Execute Update Formula ThingType");
        laps.add("[Update Process] : updateFormulaValuesForParent");
        laps.add("[Update Process] : Update formula values for thingFields");
        laps.add("[Update Process] : Logic of childrenUdf");
        laps.add("[Update Process] : updateThingInReferences");
        laps.add("[Update Process] : createUpdateThingSnapshot");
        laps.add("[Update Process] : FMC Logic");
        laps.add("[Update Process] : executeTickle");
        laps.add("[Update Process] : Set response");
        laps.add("[Update Process] : Update Favorite");

        TimerUtil tu = new TimerUtil(true);
        String[] lapList = new String[laps.size()];
        lapList = laps.toArray(lapList);
        tu.initLaps(lapList);
        return tu;
    }*/

    private String getLogTextOnFail(String serialNumber, String step) {
        return "Could not update thing with serialNumber '" + serialNumber + "' " +
                "because it failed in step '" + step + "'";
    }

    public Map<String, Object> update(Stack<Long> recursivelyStack
            , Long thingId
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
            , boolean fillSource) {
        Thing thing = ThingService.getInstance().get(thingId);
        return update(recursivelyStack
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
                , null
                , fillSource);
    }

    public Map<String, Object> update(Stack<Long> recursivelyStack
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
            , boolean fillSource) {
        long timeStamp = System.currentTimeMillis();
        Map<String, Object> response = new HashMap<>();
        ////////TimerUtil tu = initTimerUtil();

        // Validations
        String step = "Thing existence validation";
        ////////tu.start("[Update Process] : Thing existence validation");
        if (thing == null) {
            logger.error(getLogTextOnFail(serialNumber, step));
            logger.error("Thing with serial number '" + serialNumber + "' does not exist.");
            ////////tu.stop("[Update Process] : Thing existence validation");
            throw new UserException("Thing with serial number '" + serialNumber + "' does not exist.");
        }

        //If thing has already been updated recursively return null
        //NOTE if thing has been processed at the first call re-process recursively
        if (recursivelyStack != null) {
            if (recursivelyStack.contains(thing.getId()) && recursivelyStack.indexOf(thing.getId()) > 0) {
                return null;
            } else {
                recursivelyStack.push(thing.getId());
            }
        }

        //when Name is null, why name is null and serial number no??
        name = StringUtils.isEmpty(name) ? thing.getName() : name;

        Thing thingOldParent = thing.getParent();
        Thing thingWithAggregationFormula = null;

        try {
            step = "Validations";
            ////////tu.start("[Update Process] : Validations");
            CrudParameters parameters = ThingParameterExtractor.getInstance().extract(thing, thingTypeCode,
                    groupHierarchyCode, name, serialNumber, parent, udfs, children, childrenUdf, executeTickle,
                    validateVisibility, transactionDate, disableFMCLogic, validations, cache, updateAndFlush,
                    recursiveUpdate, facilityCode, subject, currentUser, fillSource);
            ////////tu.stop("[Update Process] : Validations");

            step = "Remove Sequence Udf";
            ////////tu.start("[Update Process] : Remove Sequence Udf");
            ThingBuilder.getInstance().removeUdfsSequence(parameters);
            ////////tu.stop("[Update Process] : Remove Sequence Udf");

            step = "set thing properties";
            ////////tu.start("[Update Process] : Set thing properties");
            boolean updateThingInMySQL = isNecessaryUpdateThingInMySQL(parameters, thing);
            ////////tu.stop("[Update Process] : Set thing properties");


            step = "Association Parent Children new";
            ////////tu.start("[Update Process] : Association Parent Children new");
            updateThingInMySQL = updateThingInMySQL || isNecessaryUpdateParentChildInMySQL(parameters, thing, thingOldParent);
            ////////tu.stop("[Update Process] : Association Parent Children new");


            step = "Association Parent Children";
            ////////tu.start("[Update Process] : Association Parent Children");
            // TODO: parent is Object Thing
            ////////tu.stop("[Update Process] : Association Parent Children");

            step = "Update mysql";
            ////////tu.start("[Update Process] : Update mysql");
            ThingService.getInstance().updateThingMySQL(thing, updateThingInMySQL, parameters.getCreateUpdateAndFlush());
            ////////tu.stop("[Update Process] : Update mysql");

            step = "Process Fields";
            ////////tu.start("[Update Process] : Process Fields");
            processFields(parameters, thing);
            ////////tu.stop("[Update Process] : Process Fields");

            step = "Update Udf Map for Mongo";
            ////////tu.start("[Update Process] : Update UDF Map for Mongo");
            DBObject thingMongoCurrent = ThingMongoDAO.getInstance().getThing(thing.getId());
            Map<String, Object> udfMap = getUDFMapForMongo(parameters, thing, thingMongoCurrent);
            DBObject thingMongo = (DBObject) udfMap.remove(OLDER_THING);

            Map<String, Object> missingNonUdfs = ThingService.getInstance().addMissingNonUdfs(
                    thing, thingMongo, parameters.getTransactionDate(), parameters.getModifiedTime());
            Map<String, Object> thingTypeFieldTickle = getUdfofMap(udfMap, "thingTypeFieldTickle");
            Map<String, Object> thingTypeFieldResponse = getUdfofMap(udfMap, "thingTypeFieldResponse");
            thingTypeFieldResponse = addFormulaValues(parameters, thingTypeFieldResponse, thing);
            ////////tu.stop("[Update Process] : Update UDF Map for Mongo");

            List<Thing> newChildrenIds = new ArrayList<>();
            List<Thing> deleteChildrenIds = new ArrayList<>();
            List<Thing> actualChildren = new ArrayList<>();


            if (!parameters.isOlderTimestamp()) {
                step = "Update Thing Mongo";
                ////////tu.start("[Update Process] : Update Thing Mongo");

                if (parameters.getGroupHierarchyCode() != null) {
                    Map code = hierarchyValues(parameters.getGroupHierarchyCode());
                    thingTypeFieldResponse.put("hierarchyCode", parameters.getGroupHierarchyCode());
                    thingTypeFieldResponse.put("hierarchyName", code.get("hierarchyName"));
                    thingTypeFieldResponse.put("hierarchyId", code.get("hierarchyId"));
                    updateThing(parameters, thing, thingMongo, thingTypeFieldResponse);
                }
                ////////tu.stop("[Update Process] : Update Thing Mongo");

                step = "Update Children";
                List<Thing> oldChildren = ThingService.getInstance().getChildrenList(thing);
                if (parameters.getChildren() != null) {
                    ////////tu.start("[Update Process] : Update Children");
                    updateChildren(recursivelyStack, parameters, thing, actualChildren);
                    newChildrenIds = ThingService.getInstance().getDifferenceBetweenListsA_B(actualChildren, oldChildren);
                    deleteChildrenIds = ThingService.getInstance().getDifferenceBetweenListsA_B(oldChildren, actualChildren);
                    ////////tu.stop("[Update Process] : Update Children");

                    ////////tu.start("[Update Process] : Associate and disassociate children in Mongo");
                    thingWithAggregationFormula = associateDisassociateChildren(parameters, thing, deleteChildrenIds, thingWithAggregationFormula);
                    ////////tu.stop("[Update Process] : Associate and disassociate children in Mongo");
                }

                step = "Associate disassociate Parent  Mongo";
                ////////tu.start("[Update Process] : Associate disassociate Parent Mongo");
                thingWithAggregationFormula = associateDisassociateParent(parameters, thing, thingWithAggregationFormula, thingOldParent);
                ////////tu.stop("[Update Process] : Associate disassociate Parent Mongo");

                step = "Execute Update Formula ThingType";
                ////////tu.start("[Update Process] : Execute Update Formula ThingType");
                executeUpdateFormulaThingType(recursivelyStack, parameters, thing, recursiveUpdate);
                ////////tu.stop("[Update Process] : Execute Update Formula ThingType");

                step = "updateFormulaValuesForParent";
                ////////tu.start("[Update Process] : updateFormulaValuesForParent");
                updateFormulaValuesForParent(recursivelyStack, parameters, thingWithAggregationFormula, recursiveUpdate);
                ////////tu.stop("[Update Process] : updateFormulaValuesForParent");

                step = "Update Formula Values For thingFields";
                ////////tu.start("[Update Process] : Update formula values for thingFields");
                updateFormulaValuesForThingFields(recursivelyStack, parameters, thing, recursiveUpdate);
                ////////tu.stop("[Update Process] : Update formula values for thingFields");

                step = "Logic of childrenUdf";
                ////////tu.start("[Update Process] : Logic of childrenUdf");
                updateChildrenUDF(recursivelyStack, parameters);
                ////////tu.stop("[Update Process] : Logic of childrenUdf");

                step = "updateThingInReferences";
                ////////tu.start("[Update Process] : updateThingInReferences");
                updateThingInReferences(parameters, thing);
                ////////tu.stop("[Update Process] : updateThingInReferences");
            }
            step = "createSnapshotReloaded";
            ////////tu.start("[Update Process] : createUpdateThingSnapshot");
            createUpdateThingSnapshot(parameters, thing, thingTypeFieldResponse, missingNonUdfs, thingMongo, thingMongoCurrent);
            ////////tu.stop("[Update Process] : createUpdateThingSnapshot");

            step = "FMC Logic";
            ////////tu.start("[Update Process] : FMC Logic");
            FMCLogic(parameters, thing, thingOldParent, newChildrenIds, deleteChildrenIds);
            ////////tu.stop("[Update Process] : FMC Logic");

            step = "Update Favorite";
            ////////tu.start("[Update Process] : Update Favorite");
            updateRecentAndFavorites(thing, validations);
            ////////tu.stop("[Update Process] : Update Favorite");

            step = "executeTickle";
            ////////tu.start("[Update Process] : executeTickle");
            executeTickle(parameters, thing, thingTypeFieldTickle);
            ////////tu.stop("[Update Process] : executeTickle");

            ////////tu.start("[Update Process] : Set response");
            Map<String, Object> thingResponse = thing.publicMap();
            logger.debug("thingMongo : " + thingMongo);
            thingResponse.put("createdTime", thingMongo.get("createdTime"));
            thingResponse.put("modifiedTime", parameters.getModifiedTime());

            logger.debug("thingResponse : " + thingResponse);
            response.put("thing", thingResponse);
            response.put("time", transactionDate.getTime());
            response.put("fields", thingTypeFieldTickle);
            response.put("stack", recursivelyStack);
            ////////tu.stop("[Update Process] : Set response");

            String timeDeltMessage = "";
//            if (!parameters.isOlderTimestamp()) {
//                timeDeltMessage = "init=" + timerUtil.getDelt(0) + " validationThingType=" + timerUtil.getDelt(1) + " " +
//                        "removeSequenceUdf=" + timerUtil.getDelt(2) + " processFields=" + timerUtil.getDelt(3) + " validationUpdateNewThing=" + timerUtil.getDelt(4) + " associationParentChildrenNew=" + timerUtil.getDelt(5) + " associationParentChildrenUpdate=" + timerUtil.getDelt(6) + " updateMysql=" + timerUtil.getDelt(7) + " updateUdfMapForMongo=" + timerUtil.getDelt(8) + " updateThingMongo=" + timerUtil.getDelt(9) + " updateChildren=" + timerUtil.getDelt(10) + " associateDisassociateParentMongo=" + timerUtil.getDelt(11) + " executeUpdateFormulaThingType=" + timerUtil.getDelt(12) + " updateFormulaValuesForParent=" + timerUtil.getDelt(13) + " updateFormulaValuesForThingFields=" + timerUtil.getDelt(14) + " logicOfChildrenUdf=" + timerUtil.getDelt(15) + " updateThingInReferences=" + timerUtil.getDelt(16) + " createSnapshotReloaded=" + timerUtil.getDelt(17) + " fmcLogic=" + timerUtil.getDelt(18) + " executeTickle=" + timerUtil.getDelt(19) + " putResponse=" + timerUtil.getDelt(20) + " totalTime=" + timerUtil.getTotalDelt();
//            }
            ////////logger.info("Update Thing: " + tu.getLogString());
            logger.info("Done update thing, " + timeDeltMessage + " serialNumber[" + serialNumber
                    + "], transactionDate[" + parameters.getTransactionDate()
                    + "], modifiedTime[" + parameters.getModifiedTime()
                    + "], in [" + (System.currentTimeMillis() - timeStamp) + "] ms.");
        } catch (Exception e) {
            ////////logger.error("Insert Thing Fail: " + tu.getLogString());
            logger.error(getLogTextOnFail(serialNumber, step), e);
            throw e;
        }
        return response;
    }

    private Map<String, Object> hierarchyValues(String groupHierarchy){
        String[] array = groupHierarchy.split(">");
        String ids="";
        String name="";

        for (int i =0; i< array.length; i++){
            try {
                Group group = GroupService.getInstance().getByCode(array[i]);
                if (group != null){
                    name = name+">"+group.getName();
                    ids = ids+">"+group.getId();
                }
            } catch (NonUniqueResultException e) {
                throw new UserException(array[i]+" Tenant Group doesn't exist.");
            }
        }
        Map values = new HashMap();
        values.put("hierarchyName", name);
        values.put("hierarchyId", ids);
        return values;
    }

    private boolean isNecessaryUpdateThingInMySQL(CrudParameters parameters, Thing currentThing) {
        boolean updateThingInMysql = false;
        try {
            if (!currentThing.getGroup().getCode().equals(parameters.getGroup().getCode())) {
                currentThing.setGroup(parameters.getGroup());
                updateThingInMysql = true;
            }
            // set name
            if (!parameters.getName().equals(currentThing.getName())) {
                currentThing.setName(parameters.getName());
                updateThingInMysql = true;
            }
            // set Thing Type Code
            if (!parameters.getThingType().getCode().equals(currentThing.getThingType().getCode())) {
                currentThing.setThingType(parameters.getThingType());
                updateThingInMysql = true;
            }
            currentThing.setModifiedTime(parameters.getTransactionDate().getTime());
        } catch (Exception e) {
            logger.error("Error occurred while setting thing properties for thing '" +
                    parameters.getSerialNumber() + "'", e);
            throw new UserException("Error occurred while setting thing properties for thing '" +
                    parameters.getSerialNumber() + "'", e);
        }
        return updateThingInMysql;
    }

    private boolean isNecessaryUpdateParentChildInMySQL(CrudParameters parameters, Thing currentThing, Thing thingOldParent) {
        boolean updateThingInMysql = false;
        try {
            //Association Parent Children (New)
            if (parameters.getParent() != null) {
                if (parameters.getParent().isEmpty() && thingOldParent != null) {
                    currentThing.setParent(null);
                    updateThingInMysql = true;
                } else {
                    Thing newParent = (Thing) parameters.getParentObj().get("thing");
                    if (newParent != null &&
                            (thingOldParent == null || thingOldParent.getId().compareTo(newParent.getId()) != 0)) {
                        currentThing.setParent(newParent);
                        updateThingInMysql = true;
                    }
                }
            }
            return updateThingInMysql;
        } catch (Exception e) {
            logger.error("Error occurred while processing new parent association for thing '"
                    + parameters.getSerialNumber() + "'", e);
            throw new UserException("Error occurred while processing new parent association for thing '" +
                    parameters.getSerialNumber() + "'", e);
        }
    }

    private void processFields(CrudParameters parameters, Thing currentThing) {
        try {
            Long groupId = currentThing.getGroup().getId();
            if (currentThing.hasThingTypeField("location")) {
                if ((parameters.getFacilityCode() != null) && (!parameters.getFacilityCode().isEmpty())) {
                    Map<String, Object> groupResultValidation = ThingsService.getInstance()
                            .validateAndGetGroup(parameters.getGroupHierarchyCode(), parameters.getCurrentUser(),
                                    parameters.getThingType(), parameters.getValidateVisibility(), parameters.getFacilityCode());
                    ValidationBean validationBean = (ValidationBean) groupResultValidation.get("validationBean");
                    if (validationBean.isError()) {
                        throw new UserException(validationBean.getErrorDescription());
                    } else {
                        groupId = ((Group) groupResultValidation.get("group")).getId();
                    }
                    LocalMapService localMapService = new LocalMapService();
                    List<LocalMap> maps = localMapService.selectAllByGroupId(groupId);
                    if ((maps == null) || maps.isEmpty()) {
                        throw new UserException("Location fields cannot be calculated because the group [" + currentThing.getGroup().getName() + "] of the thing has not any facility map.");
                    }
                }
            }
            AppLocationAdapter locationAdapter = new AppLocationAdapter();
            locationAdapter.processFields(parameters.getUdfs(), parameters.getTransactionDate(), groupId, currentThing
                    .getThingType().getCode(), false);
        } catch (UserException e) {
            logger.error("Error occurred while processing service in thing '" + parameters.getSerialNumber() + "'");
            throw e;
        } catch (Exception e) {
            logger.error("Error occurred while processing service in thing '" + parameters.getSerialNumber() + "'", e);
            if (e.getMessage() != null && (e.getMessage().startsWith("Location") || e.getMessage().startsWith("Zone"))) {
                throw e;
            } else {
                throw new UserException("Error occurred while processing location service in thing '"
                        + parameters.getSerialNumber() + "'", e);
            }
        }
    }

    private Map<String, Object> getUDFMapForMongo(CrudParameters parameters, Thing thing, DBObject thingMongo) {
        Long thingMongoTime = ((Date) thingMongo.get("time")).getTime();
        boolean isOlderTimestamp = parameters.getTransactionDate().getTime() < thingMongoTime;
        if (isOlderTimestamp) {
            thingMongo = ThingMongoDAO.getPreviousSnapshotByTime(thing.getId(), parameters.getTransactionDate().getTime());
        }
        parameters.setOlderTimestamp(isOlderTimestamp);
        try {
            Map<String, Object> udfMapForMongo = ThingMongoBuilder.getInstance().getUdfMapForMongo(thing.getId(), parameters, thingMongo);
            udfMapForMongo.put(OLDER_THING, thingMongo);
            return udfMapForMongo;
        } catch (Exception e) {
            logger.error("Error occurred while preparing data for mongo in thing '"
                    + parameters.getSerialNumber() + "'", e);
            throw new UserException("Error occurred while preparing data for mongo in thing '"
                    + parameters.getSerialNumber() + "'", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getUdfofMap(Map<String, Object> udfsMap, String fieldName) {
        Map<String, Object> thingTypeField = null;
        if (udfsMap != null && !udfsMap.isEmpty()) {
            thingTypeField = (Map<String, Object>) udfsMap.get(fieldName);
        }
        return thingTypeField;
    }

    private Map<String, Object> addFormulaValues(CrudParameters parameters, Map<String, Object> udfMap, Thing thing) {
        try {
            return FormulaUtil.addFormulaValues(udfMap, thing, parameters.getTransactionDate());
        } catch (Exception e) {
            logger.error("Error occurred while preparing formulas for thing '"
                    + parameters.getSerialNumber() + "'", e);
            throw new UserException("Error occurred while preparing formulas for thing '"
                    + parameters.getSerialNumber() + "'", e);
        }
    }

    private void updateThing(CrudParameters parameters, Thing thing, DBObject thingMongo, Map<String, Object> thingTypeFields) {
        try {
            ThingMongoService.getInstance().updateThing(thing
                    , thingMongo
                    , thingTypeFields
                    , parameters.getTransactionDate()
                    , parameters.getModifiedTime());
        } catch (Exception e) {
            logger.error("Error occurred while updating thing '"
                    + parameters.getSerialNumber() + "' in mongo", e);
            throw new UserException("Error occurred while updating thing '"
                    + parameters.getSerialNumber() + "' in mongo", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateChildren(Stack<Long> recursivelyStack, CrudParameters parameters, Thing thing, List<Thing> actualChildren) {
        for (Map<String, Object> childThingMap : parameters.getChildren()) {
            try {
                Thing childThing = (Thing) childThingMap.get("thing");
                if (childThing == null) {
                    ThingType thingTypeChildren = ThingTypeService.getInstance().getByCode((String) childThingMap.get(THING_TYPE_CODE));
                    childThing = ThingService.getInstance().getBySerialNumber((String) childThingMap.get(SERIAL_NUMBER), thingTypeChildren);
                }
                actualChildren.add(childThing);

                Map<String, Object> parentChildMap = new HashMap<String, Object>();
                parentChildMap.put(SERIAL_NUMBER, thing.getSerial());
                parentChildMap.put(THING_TYPE_CODE, thing.getThingType().getThingTypeCode());

                updateChildrenRecursive(recursivelyStack, parameters, childThing, parentChildMap
                        , (Map<String, Object>) childThingMap.get("udfs"), true, true, true, true);
            } catch (NonUniqueResultException e) {
                logger.error("Error occurred while updating thing '" + parameters.getSerialNumber()
                        + "', child '" + childThingMap.get(SERIAL_NUMBER) + "' contains multiple values", e);
                throw new UserException("Error occurred while updating thing '" + parameters.getSerialNumber()
                        + "', child '" + childThingMap.get(SERIAL_NUMBER) + "' contains multiple values", e);
            } catch (Exception e) {
                logger.error("Error occurred while updating thing '" + parameters.getSerialNumber()
                        + "' in child '" + childThingMap.get(SERIAL_NUMBER) + "'", e);
                throw new UserException("Error occurred while updating thing '" + parameters.getSerialNumber()
                        + "' in child '" + childThingMap.get(SERIAL_NUMBER) + "'", e);
            }
        }
    }

    /**
     * for update children native
     */
    private void updateChildrenRecursive(
            Stack<Long> recursivelyStack
            , CrudParameters parameters
            , Thing thing
            , Map<String, Object> parent
            , Map<String, Object> udfs
            , boolean validateVisibility
            , boolean disableFMCLogic
            , boolean updateAndFlush
            , boolean recursiveUpdate) {
        update(recursivelyStack
                , thing
                , thing.getThingType().getThingTypeCode()
                , thing.getGroup().getHierarchyName(false)
                , thing.getName()
                , thing.getSerialNumber()
                , parent
                , udfs
                , null
                , null
                , parameters.getExecuteTickle()
                , validateVisibility
                , parameters.getTransactionDate()
                , disableFMCLogic
                , null
                , null
                , updateAndFlush
                , recursiveUpdate
                , null
                , parameters.getSubject()
                , parameters.getCurrentUser()
                , parameters.getFillSource());
    }

    private Thing associateDisassociateChildren(CrudParameters parameters, Thing thing,
                                                List<Thing> deleteChildrenIds, Thing thingWithAggregationFormula) {
        try {
            if (deleteChildrenIds != null) {
                for (Thing thingDel : deleteChildrenIds) {
//                    Thing thingDel = ThingService.getInstance().get(id);
                    thingDel.setParent(null);
                    ThingService.getThingDAO().update(thingDel);
                    ThingMongoService.getInstance().disAssociateChild(thing, thingDel);

                    ThingMongoService.getInstance().disAssociateChild(thing, thingDel);
                    if (parameters.getExecuteTickle()) {
                        ThingService.getInstance().executeTickle(
                                thingDel.getThingType().getThingTypeCode()
                                , thingDel.getSerial()
                                , null
                                , parameters.getTransactionDate()
                                , false,
                                GroupService.getInstance().getMqttGroups(thingDel.getGroup()));
                    }
                    thingWithAggregationFormula = thing;
                }
            }
            return thingWithAggregationFormula;
        } catch (Exception e) {
            logger.error("Error occurred while updating children association for thing '"
                    + parameters.getSerialNumber() + "' in mongo", e);
            throw new UserException("Error occurred while updating children association for thing '"
                    + parameters.getSerialNumber() + "' in mongo", e);
        }
    }

    private Thing associateDisassociateParent(CrudParameters parameters, Thing currentThing, Thing thingWithAggregationFormula, Thing oldParent) {
        if (parameters.getParent() != null) {
            try {
                if (parameters.getParent() != null && parameters.getParent().isEmpty() && oldParent != null) {
                    thingWithAggregationFormula = currentThing.getParent();
                    ThingMongoService.getInstance().disAssociateChild(oldParent, currentThing);
                } else if (parameters.getParent() != null && !parameters.getParent().isEmpty()) {
                    Thing newThingParent = (Thing) parameters.getParentObj().get("thing");
                    ThingMongoService.getInstance().associateChild(newThingParent, oldParent, currentThing);
                    thingWithAggregationFormula = newThingParent;
                }
            } catch (Exception e) {
                logger.error("Error occurred while updating parent association for thing '"
                        + parameters.getSerialNumber() + "'", e);
                throw new UserException("Error occurred while updating parent association for thing '"
                        + parameters.getSerialNumber() + "'", e);
            }
        } else {
            if (currentThing.getParent() != null) {
                thingWithAggregationFormula = currentThing.getParent();
            }
        }
        return thingWithAggregationFormula;
    }

    private void executeUpdateFormulaThingType(Stack<Long> recursivelyStack, CrudParameters parameters, Thing currentThing, boolean recursiveUpdate) {
        if (recursiveUpdate) {
            try {
                //Update Formula in children
                ThingService.getInstance().updateExpressionsForChildren(recursivelyStack, currentThing, null,
                        parameters.getCurrentUser(), parameters.getUdfs(), parameters.getFillSource(), parameters.getTransactionDate());
            } catch (Exception e) {
                logger.error("Error occurred while updating children formula references for thing '"
                        + parameters.getSerialNumber() + "' in mongo", e);
                throw new UserException("Error occurred while updating children formula references for thing '"
                        + parameters.getSerialNumber() + "' in mongo", e);
            }

            try {
                // update formula for nativeThingType relations
                ThingService.getInstance().updateExpressionsForChildrenUDF(recursivelyStack, parameters.getThingType()
                        , currentThing, parameters.getCurrentUser(), parameters.getUdfs(),
                        parameters.getFillSource(), parameters.getTransactionDate());
            } catch (Exception e) {
                logger.error("Error occurred while updating native thingType formula references for thing '"
                        + parameters.getTransactionDate() + "' in mongo", e);
                throw new UserException("Error occurred while updating native thingType formula references for thing '"
                        + parameters.getTransactionDate() + "' in mongo", e);
            }
        }
    }

    private void updateFormulaValuesForParent(Stack<Long> recursivelyStack, CrudParameters parameters, Thing thingWithAggregationFormula, boolean recursiveUpdate) {
        if (thingWithAggregationFormula != null && recursiveUpdate) {
            try {
                ThingService.getInstance().updateFormulaValuesForParent(
                        recursivelyStack
                        , thingWithAggregationFormula
                        , parameters.getTransactionDate()
                        , parameters.getExecuteTickle()
                        , parameters.getCurrentUser()
                        , parameters.getFillSource());
            } catch (Exception e) {
                logger.error("Error occurred while updating parent formula references for thing '"
                        + parameters.getSerialNumber() + "' in mongo", e);
                throw new UserException("Error occurred while updating parent formula references for thing '"
                        + parameters.getSerialNumber() + "' in mongo", e);
            }
        }
    }

    private void updateFormulaValuesForThingFields(Stack<Long> recursivelyStack, CrudParameters parameters, Thing currentThing, boolean recursiveUpdate) {
        List<ThingTypeField> thingTypeFieldsTT = currentThing.getThingTypeFieldByType(ThingTypeField.Type.TYPE_THING_TYPE.value);
        if (!thingTypeFieldsTT.isEmpty() && recursiveUpdate) {
            try {
                Map<String, Object> udfValuesAfterSaving = ThingService.getInstance().getUdfValues(currentThing.getId());
                Map<String, Object> thingForFormulaAfterSaving = ThingService.getInstance()
                        .getThingsToEvaluateFormula(udfValuesAfterSaving, thingTypeFieldsTT);
                ThingService.getInstance().updateFormulaValuesForThingTypeUdfs(
                        recursivelyStack, thingForFormulaAfterSaving,
                        parameters.getTransactionDate(), parameters.getExecuteTickle(),
                        parameters.getCurrentUser(), parameters.getFillSource());
            } catch (Exception e) {
                logger.error("Error occurred while updating thingTypeUdf parent formula for thing '" +
                        parameters.getSerialNumber() + "' in mongo", e);
                throw new UserException("Error occurred while updating thingTypeUdf parent formula for thing '" +
                        parameters.getSerialNumber() + "' in mongo", e);
            }
        }
    }

    private void updateChildrenUDF(Stack<Long> recursivelyStack, CrudParameters parameters) {
        if (parameters.getChildrenUdf() != null && !parameters.getChildrenUdf().isEmpty()) {
            for (Map<String, Object> childThingMap : parameters.getChildrenUdf()) {
                try {
                    ThingType thingTypeChild = ThingTypeService.getInstance().getByCode((String) childThingMap.get(THING_TYPE_CODE));
                    Thing thingChild = ThingService.getInstance().getBySerialNumber((String) childThingMap.get(SERIAL_NUMBER), thingTypeChild);
                    updateChildrenUDFRecursive(recursivelyStack, parameters, thingChild, childThingMap);
                } catch (Exception e) {
                    logger.error("Error occurred while updating child udfs for thing '" + parameters.getSerialNumber()
                            + "', child '" + childThingMap.get(SERIAL_NUMBER) + "", e);
                    throw new UserException("Error occurred while updating child udfs for thing '"
                            + parameters.getSerialNumber() + "', child '" + childThingMap.get(SERIAL_NUMBER) + "", e);
                }
            }
        }
    }

    /**
     * for update children UDF
     */
    private void updateChildrenUDFRecursive(
            Stack<Long> recursivelyStack
            , CrudParameters parameters
            , Thing thing
            , Map<String, Object> childThingMap) {
        update(recursivelyStack
                , thing
                , thing.getThingType().getThingTypeCode()
                , thing.getGroup().getHierarchyName(false)
                , thing.getName()
                , thing.getSerial()
                , null
                , (Map<String, Object>) childThingMap.get("udfs")
                , null, null
                , parameters.getExecuteTickle()
                , true
                , parameters.getTransactionDate()
                , parameters.getDisableFMCLogic()
                , null
                , null
                , parameters.getCreateUpdateAndFlush()
                , true
                , null
                , parameters.getSubject()
                , parameters.getCurrentUser()
                , parameters.getFillSource()
        );
    }

    private void updateThingInReferences(CrudParameters parameters, Thing thing) {
        try {
            ThingMongoService.getInstance().updateThingInReferences(thing);
        } catch (Exception e) {
            logger.error("Error occurred while updating other references for thing '"
                    + parameters.getSerialNumber() + "' in mongo", e);
            throw new UserException("Error occurred while updating other references for thing '"
                    + parameters.getSerialNumber() + "' in mongo", e);
        }
    }

    private void createUpdateThingSnapshot(CrudParameters parameters, Thing currentThing,
                                           Map<String, Object> thingTypeFieldResponse, Map<String, Object> missingNonUdfs,
                                           DBObject thingMongo, DBObject thingMongoCurrent) {
        try {
            Map<String, ThingTypeField> typeFieldMap = ThingTypeFieldService.getInstance().getThingTypeFieldByThingTypeCode(parameters.getThingTypeCode());
            ThingMongoService.getInstance().createUpdateThingSnapshot(currentThing.getId(), parameters.getSerialNumber(),
                    parameters.getThingTypeCode(), thingTypeFieldResponse, missingNonUdfs, parameters.getTransactionDate(), parameters.isOlderTimestamp(),
                    typeFieldMap, parameters.getCurrentUser());

            if (parameters.isOlderTimestamp()) {
                SnapshotFixerJobServices.offer(new ChangedFieldsServices(thingTypeFieldResponse, thingMongo, thingMongoCurrent),
                        currentThing.getId(),
                        parameters.getSerialNumber(),
                        parameters.getThingTypeCode(),
                        parameters.getTransactionDate().getTime(),
                        !thingMongo.keySet().isEmpty());

            }
        } catch (Exception e) {
            logger.error("Error occurred while creating/update thingSnapshot for thing '"
                    + parameters.getSerialNumber() + "' in mongo", e);
            throw new UserException("Error occurred while creating/update thingSnapshot for thing '"
                    + parameters.getSerialNumber() + "' in mongo", e);
        }
    }

    private void FMCLogic(CrudParameters parameters, Thing currentThing, Thing thingOldParent,
                          List<Thing> newChildrenIds, List<Thing> deleteChildrenIds) {
        if (!parameters.isOlderTimestamp() && !parameters.getDisableFMCLogic()) {
            try {
                ThingService.getInstance().executeFMCLogic(currentThing,
                        parameters.getUdfs(),
                        currentThing.getParent(),
                        thingOldParent,
                        newChildrenIds,
                        deleteChildrenIds,
                        parameters.getTransactionDate(),
                        parameters.getCurrentUser());
            } catch (Exception e) {
                logger.error("Error occurred while executing FMC logic for thing '"
                        + parameters.getSerialNumber() + "'", e);
                throw new UserException("Error occurred while executing FMC logic for thing '"
                        + parameters.getSerialNumber() + "'", e);
            }
        }
    }

    private void executeTickle(CrudParameters parameters, Thing currentThing, Map<String, Object> thingTypeFieldTickle) {
        if (parameters.getExecuteTickle()) {
            try {
                ThingService.getInstance().executeTickle(currentThing.getThingType().getThingTypeCode(), currentThing.getSerial()
                        , thingTypeFieldTickle, parameters.getTransactionDate()
                        , false, GroupService.getInstance().getMqttGroups(currentThing.getGroup()));
            } catch (Exception e) {
                logger.error("Error occurred while executing tickle for thing '"
                        + parameters.getSerialNumber() + "'", e);
                throw new UserException("Error occurred while executing tickle for thing '"
                        + parameters.getSerialNumber() + "'", e);
            }
        }
    }

    private void updateRecentAndFavorites(Thing thing, Map<String, Boolean> validations) {
        if (validations == null || (validations.get("thing.uFavoriteAndRecent") != null && validations.get("thing.uFavoriteAndRecent"))) {
            ThingService.getInstance().updateFavorite(thing);
            RecentService.getInstance().updateName(thing.getId(), thing.getName(), "thing");
        }
    }

}
