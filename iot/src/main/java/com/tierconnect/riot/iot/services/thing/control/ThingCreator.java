package com.tierconnect.riot.iot.services.thing.control;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.utils.TimerUtil;
import com.tierconnect.riot.iot.dao.SequenceDAO;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.services.ThingMongoService;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.iot.services.ThingsService;
import com.tierconnect.riot.iot.services.thing.entity.CrudParameters;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;
import org.apache.shiro.subject.Subject;

import java.util.*;

/**
 * Created by julio.rocha on 09-08-17.
 */
public class ThingCreator {
    private static Logger logger = Logger.getLogger(ThingCreator.class);
    private static final ThingCreator INSTANCE = new ThingCreator();

    private ThingCreator() {
    }

    public static ThingCreator getInstance() {
        return INSTANCE;
    }

    /*private TimerUtil initTimerUtil() {
        List<String> laps = new ArrayList<>();
        laps.add("[Insert Process] : Validations");
        laps.add("[Insert Process] : Set values of the Thing");
        laps.add("[Insert Process] : Insert MySQL");
        laps.add("[Insert Process] : Set properties Mongo");
        laps.add("[Insert Process] : Insert MongoDB");
        laps.add("[Insert Process] : Parent");
        laps.add("[Insert Process] : children");
        laps.add("[Insert Process] : update thing in its references");
        laps.add("[Insert Process] : create snapshot");
        laps.add("[Insert Process] : execute FMC logic");
        laps.add("[Insert Process] : execute tickle");
        laps.add("[Insert Process] : Set response");

        TimerUtil tu = new TimerUtil(true);
        String[] lapList = new String[laps.size()];
        lapList = laps.toArray(lapList);
        tu.initLaps(lapList);
        return tu;
    }*/

    private String getlogTextOnFail(String serialNumber, String step) {
        return "Could not create thing with serialNumber '" + serialNumber + "' " +
                "because it failed in step '" + step + "'";
    }

    public Map<String, Object> create(Stack<Long> recursivelyStack,
                                      String thingTypeCode,
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
                                      String facilityCode,
                                      boolean fillSource,
                                      Subject subject) {
        long timeStamp = System.currentTimeMillis();
        String step = "Start";
        //Method values
        Map<String, Object> response = new HashMap<>();
        Thing thing = null;
        ////////TimerUtil tu = initTimerUtil();
        try {
            //Validations
            step = "Validations";
            ////////tu.start("[Insert Process] : Validations");
            CrudParameters parameters = ThingParameterExtractor.getInstance().extract(thingTypeCode, groupHierarchyCode,
                    name, serialNumber, parent, udfs, children, childrenUdf, executeTickle, validateVisibility,
                    transactionDate, disableFMCLogic, createAndFlush, useDefaultValues, validations,
                    facilityCode, fillSource, subject);
            ////////tu.stop("[Insert Process] : Validations");

            //Set serialNumber to upperCase
            //already do it in CrudParameters

            //Set values of the Thing
            ////////tu.start("[Insert Process] : Set values of the Thing");
            step = "Set values of the Thing";
            thing = ThingBuilder.getInstance().newThing(parameters);
            ////////tu.stop("[Insert Process] : Set values of the Thing");
            //Insert MySQL
            ////////tu.start("[Insert Process] : Insert MySQL");
            step = "Insert MySQL";
            ThingsService.getInstance().insertThingMySQL(thing, parameters.getCreateUpdateAndFlush());
            ////////tu.stop("[Insert Process] : Insert MySQL");
            //Set properties Mongo
            ////////tu.start("[Insert Process] : Set properties Mongo");
            step = "Set properties Mongo";
            //getThingMongoProperties(parameters, thing);
            Map<String, Object> thingMongoMap = ThingMongoBuilder.getInstance().newMongoThing(thing, parameters);
            ////////tu.stop("[Insert Process] : Set properties Mongo");
            //Insert MongoDB
            //Date modifiedTime = new Date();
            step = "Insert MongoDB";
            Map<String,Object> thingTypeFields = (Map<String, Object>) thingMongoMap.get("thingTypeFieldResponse");
            if (parameters.getGroupHierarchyCode() != null) {
                Map code = hierarchyValues(parameters.getGroupHierarchyCode());
                thingTypeFields.put("hierarchyCode", parameters.getGroupHierarchyCode());
                thingTypeFields.put("hierarchyName", code.get("hierarchyName"));
                thingTypeFields.put("hierarchyId", code.get("hierarchyId"));
            }
            ////////tu.start("[Insert Process] : Insert MongoDB");
            ThingMongoService.getInstance().createNewThing(
                    thing,
                    thingTypeFields,
                    parameters.getTransactionDate(),
                    parameters.getExecuteTickle(),
                    recursivelyStack,
                    parameters.getModifiedTime());
            ////////tu.stop("[Insert Process] : Insert MongoDB");
            //Parent
            ////////tu.start("[Insert Process] : Parent");
            step = "Parent";
            ThingsService.getInstance().associateParent(
                    thing.getParent(), null, thing, thingMongoMap, parameters.getTransactionDate(), recursivelyStack,
                    parameters.getExecuteTickle(), parameters.getCurrentUser(), parameters.getFillSource());
            ////////tu.stop("[Insert Process] : Parent");
            // children
            ////////tu.start("[Insert Process] : children");
            step = "Children";
            List<Thing> newChildrenIds = associateChildren(recursivelyStack, parameters.getExecuteTickle(), parameters, thing, thingMongoMap);
            ////////tu.stop("[Insert Process] : children");
            // update thing in its references
            ////////tu.start("[Insert Process] : update thing in its references");
            step = "update thing in its references";
            ThingMongoService.getInstance().updateThingInReferences(thing);
            ////////tu.stop("[Insert Process] : update thing in its references");
            ////////tu.start("[Insert Process] : create snapshot");
            step = "create snapshot";
            Map<String, ThingTypeField> typeFieldMap = ThingTypeFieldService.getInstance()
                    .getThingTypeFieldByThingTypeCode(parameters.getThingTypeCode());
            ThingMongoService.getInstance().createUpdateThingSnapshot(thing.getId(), thing.getSerialNumber(),
                    parameters.getThingTypeCode(), thingTypeFields,
                    parameters.getTransactionDate(), false, typeFieldMap, parameters.getCurrentUser());
            ////////tu.stop("[Insert Process] : create snapshot");
            ////////tu.start("[Insert Process] : execute FMC logic");
            step = "execute FMC logic";
            if (!parameters.getDisableFMCLogic()) {
                ThingService.getInstance().executeFMCLogic(thing,
                        parameters.getUdfs(),
                        thing.getParent(),
                        null,
                        newChildrenIds,
                        null,
                        parameters.getTransactionDate(),
                        parameters.getCurrentUser());
            }
            ////////tu.stop("[Insert Process] : execute FMC logic");
            // execute tickle
            ////////tu.start("[Insert Process] : execute tickle");
            step = "execute tickle";
            Map<String, Object> thingTypeFieldTickle = ThingsService.getInstance()
                    .getMapFieldsTickle(thingMongoMap, executeTickle, thing, transactionDate);
            ////////tu.stop("[Insert Process] : execute tickle");
            // Set response
            ////////tu.start("[Insert Process] : Set response");
            step = "Set response";
            Map<String, Object> thingresponse = thing.publicMap();
            thingresponse.put("createdTime", parameters.getModifiedTime());
            thingresponse.put("modifiedTime", parameters.getModifiedTime());

            response.put("thing", thingresponse);
            response.put("time", parameters.getTransactionDate().getTime());
            response.put("fields", thingTypeFieldTickle);
            ////////tu.stop("[Insert Process] : Set response");
            ////////logger.info("Insert Thing: " + tu.getLogString());
            logger.info("Thing was created successfully. serialNumber [" + serialNumber + "], " +
                    "transactionDate[" + parameters.getTransactionDate().toString() + "], " +
                    "modifiedTime[" + parameters.getModifiedTime() + "]," +
                    " in [" + (System.currentTimeMillis() - timeStamp) + "] ms.");
        } catch (Exception e) {
            ////////logger.error("Insert Thing Fail: " + tu.getLogString());
            logger.error(getlogTextOnFail(serialNumber, step), e);
            if ((thing != null) && (thing.getId() != null)) {
                // Decrement sequence values in case of error
                Map<String, Object> udfsWithSequenceValues = ThingService.getInstance().getSequenceValues(
                        thing.getThingType().getCode(),
                        transactionDate);
                if ((udfsWithSequenceValues != null) && (!udfsWithSequenceValues.isEmpty())) {
                    for (Map.Entry<String, Object> entry : udfsWithSequenceValues.entrySet()) {
                        ThingTypeField thingTypeField = thing.getThingType().getThingTypeFieldByName(entry.getKey());
                        SequenceDAO.getInstance().decrementAndGetSequence(thingTypeField.getId());
                    }
                }
                //Delete Thing
                ThingService.getInstance().secureDelete(
                        thing.getId(), subject, validateVisibility, executeTickle, fillSource, false, true);
            }
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

    private List<Thing> associateChildren(Stack<Long> recursivelyStack, boolean executeTickle, CrudParameters parameters,
                                         Thing thing, Map<String, Object> thingMongoMap) {
        String groupHierarchyCode = parameters.getGroupHierarchyCode();
        boolean validateVisibility = parameters.getValidateVisibility();
        User currentUser = parameters.getCurrentUser();
        boolean fillSource = parameters.getFillSource();
        Date transactionDate = parameters.getTransactionDate();
        Boolean defaultValues = parameters.getUseDefaultValues();
        List<Thing> newChildrenIds = ThingsService.getInstance().createChildren(thing, groupHierarchyCode,
                parameters.getChildrenObj(), executeTickle, validateVisibility, transactionDate,
                defaultValues, recursivelyStack, currentUser, fillSource);
        ThingsService.getInstance().createOrUpdateChildrenUDF(groupHierarchyCode, parameters.getChildrenUDFObj(),
                executeTickle, validateVisibility, transactionDate, defaultValues, recursivelyStack, currentUser, fillSource);
        ThingService.getInstance().updateExpressionsForChildren(recursivelyStack, thing, parameters.getThingTypeCode(), currentUser,
                (Map<String, Object>) thingMongoMap.get("thingTypeFieldResponse"), fillSource, transactionDate);
        ThingService.getInstance().updateExpressionsForChildrenUDF(recursivelyStack, parameters.getThingType(),
                thing, currentUser, (Map<String, Object>) thingMongoMap.get("thingTypeFieldResponse"), fillSource, transactionDate);
        return newChildrenIds;
    }
}
