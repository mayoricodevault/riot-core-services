package com.tierconnect.riot.iot.services.thing.control;

import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.RecentService;
import com.tierconnect.riot.commons.utils.TimerUtil;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.fmc.utils.FMCUtils;
import com.tierconnect.riot.iot.services.BrokerClientHelper;
import com.tierconnect.riot.iot.services.ThingMongoService;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.iot.services.thing.entity.CrudParameters;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;
import org.apache.shiro.subject.Subject;

import java.util.*;

/**
 * Created by julio.rocha on 09-08-17.
 */
public class ThingDeleter {
    private static Logger logger = Logger.getLogger(ThingDeleter.class);
    private static final ThingDeleter INSTANCE = new ThingDeleter();

    private ThingDeleter() {
    }

    public static ThingDeleter getInstance() {
        return INSTANCE;
    }

    /*private TimerUtil initDeleteTimerUtil() {
        List<String> laps = new ArrayList<>();
        laps.add("[Delete Process] : Validations to delete thing");
        laps.add("[Delete Process] : Disassociate any relation of the thing, parent and/or children");
        laps.add("[Delete Process] : Check get UDFs if we have Attachments or Expressions");
        laps.add("[Delete Process] : Delete in Mysql");
        laps.add("[Delete Process] : Delete Favorite");
        laps.add("[Delete Process] : Remove Thing In Parent Udf");
        laps.add("[Delete Process] : Delete in Mongo");
        laps.add("[Delete Process] : update formula values for thing type fields");
        laps.add("[Delete Process] : Execute Fmc Logic");
        laps.add("[Delete Process] : Execute tickle");

        TimerUtil tu = new TimerUtil(true);
        String[] lapList = new String[laps.size()];
        lapList = laps.toArray(lapList);
        tu.initLaps(lapList);
        return tu;
    }*/

    public void delete(Stack<Long> recursivelyStack,
                       Thing thing,
                       boolean validateVisibility,
                       Date transactionDate,
                       boolean executeTickle,
                       User currentUser,
                       Subject subject,
                       User userLogged,
                       boolean fillSource,
                       boolean deleteMongoFlag,
                       boolean secure) {
        ////////TimerUtil tu = initDeleteTimerUtil();
        try {
            ////////tu.start("[Delete Process] : Validations to delete thing");
            CrudParameters parameters = ThingParameterExtractor.getInstance().extract(thing, validateVisibility,
                    transactionDate, executeTickle, currentUser, subject, userLogged, fillSource, deleteMongoFlag, secure);
            ////////tu.stop("[Delete Process] : Validations to delete thing");
            //Disassociate any relation of the thing, parent and/or children
            ////////tu.start("[Delete Process] : Disassociate any relation of the thing, parent and/or children");
            Map<String, Object> parentMap = parameters.getThingParentMapToProcess();
            List<Thing> children = ThingService.getInstance().getChildrenList(parameters.getThingToProcess());
            disassociateRelations(recursivelyStack, parameters, parentMap, children);
            ////////tu.stop("[Delete Process] : Disassociate any relation of the thing, parent and/or children");
            //Check get UDFs if we have Attachments or Expressions
            ////////tu.start("[Delete Process] : Check get UDFs if we have Attachments or Expressions");
            ThingType thingType = parameters.getThingType();
            //List<ThingTypeField> thingTypeFieldsTT = thing.getThingTypeFieldByType(ThingTypeField.Type.TYPE_THING_TYPE.value);
            List<ThingTypeField> thingTypeFieldsTT = ThingTypeFieldService.getInstance()
                    .getThingTypeFieldByType(ThingTypeField.Type.TYPE_THING_TYPE.value, thingType.getId());
            Map<String, Object> udfValues = deleteAttachmentsAndGetUdfValues(parameters, thingTypeFieldsTT);
            ////////tu.stop("[Delete Process] : Check get UDFs if we have Attachments or Expressions");
            //Delete in Mysql
            ////////tu.start("[Delete Process] : Delete in Mysql");
            ThingService.getThingDAO().delete(parameters.getThingToProcess());
            ////////tu.stop("[Delete Process] : Delete in Mysql");
            ////////tu.start("[Delete Process] : Delete Favorite");
            deleteRecentAndFavorites(parameters.getThingToProcess(), parameters.getUserActionExecutor());
            ////////tu.stop("[Delete Process] : Delete Favorite");
            //Remove Thing In Parent Udf
            ////////tu.start("[Delete Process] : Remove Thing In Parent Udf");
            ThingMongoService.getInstance().deleteThingInReferences(parameters.getThingToProcess());
            ////////tu.stop("[Delete Process] : Remove Thing In Parent Udf");
            //Delete in Mongo
            ////////tu.start("[Delete Process] : Delete in Mongo");
            deleteThingInMongo(parameters.getThingToProcess(), parameters);
            ////////tu.stop("[Delete Process] : Delete in Mongo");
            // if the thing has native thing type udfs -> update formula values for those things
            ////////tu.start("[Delete Process] : update formula values for thing type fields");
            updateFormulaForUdfs(recursivelyStack, parameters, thingTypeFieldsTT, udfValues);
            ////////tu.stop("[Delete Process] : update formula values for thing type fields");
            //Execute Fmc Logic
            ////////tu.start("[Delete Process] : Execute Fmc Logic");
            FMCUtils.fmcHandleDeleteAsset(parameters.getThingToProcess(), parameters.getGroup(), new Date(), currentUser);
            ////////tu.stop("[Delete Process] : Execute Fmc Logic");
            //Execute tickle
            ////////tu.start("[Delete Process] : Execute tickle");
            sendTickles(parameters, parentMap, children);
            ////////tu.stop("[Delete Process] : Execute tickle");
            ////////logger.info("Delete Thing: " + tu.getLogString());
        } catch (Exception e) {
            ////////logger.error("Delete Thing Fail: " + tu.getLogString());
            logger.error("Could not delete thing", e);
            throw e;
        }
    }

    private void disassociateRelations(Stack<Long> recursivelyStack, CrudParameters parameters,
                                       Map<String, Object> parentMap, List<Thing> children) {
        if (parentMap != null ||
                (children != null && children.size() > 0)) {
            Thing thingToProcess = parameters.getThingToProcess();
            ThingService.getInstance().update(recursivelyStack,
                    thingToProcess.getId(),
                    thingToProcess.getThingType().getThingTypeCode(),
                    thingToProcess.getGroup().getHierarchyName(false),
                    thingToProcess.getName(),
                    thingToProcess.getSerial(),
                    parentMap,
                    null,  //udf
                    new ArrayList<Map<String, Object>>(),  //children
                    null, false, parameters.getValidateVisibility(),
                    parameters.getTransactionDate(), false,
                    parameters.getUserActionExecutor(), parameters.getFillSource());

        }
    }

    private Map<String, Object> deleteAttachmentsAndGetUdfValues(CrudParameters parameters, List<ThingTypeField> thingTypeFieldsTT) {
        Map<String, Object> udfValues = null;
        Thing thing = parameters.getThingToProcess();
        ThingType thingType = parameters.getThingType();
        List<ThingTypeField> ttfAttachments = ThingTypeFieldService.getInstance()
                .getThingTypeFieldByType(ThingTypeField.Type.TYPE_ATTACHMENTS.value, thingType.getId());
        if (((ttfAttachments != null) && (!ttfAttachments.isEmpty())) ||
                ((thingTypeFieldsTT != null) && !thingTypeFieldsTT.isEmpty())) {
            // get udf values before delete
            udfValues = ThingService.getInstance().getUdfValues(thing.getId());
            //Delete files
            ThingService.getInstance().deleteAttachments(thingType, ttfAttachments, udfValues);
        }
        return udfValues;
    }

    private void deleteRecentAndFavorites(Thing thing, User currentUser) {
        ThingService.getInstance().deleteFavorite(currentUser, thing);
        RecentService.getInstance().deleteRecent(thing.getId(), "thing", currentUser);
    }

    private void deleteThingInMongo(Thing thing, CrudParameters parameters) {
        if (parameters.getDeleteMongoFlag()) {
            try {
                ThingMongoService.getInstance().deleteThing(thing.getId());
            } catch (Exception e) {
                throw new UserException("Error to delete the thing " + thing.getId() + " in Mongo: " + e.getMessage(), e);
            }
        } else if (parameters.getSecureDelete()) {
            ThingService.getInstance().insertIntoTempTable(thing);
        }
    }

    private void updateFormulaForUdfs(Stack<Long> recursivelyStack, CrudParameters parameters,
                                      List<ThingTypeField> thingTypeFieldsTT, Map<String, Object> udfValues) {
        if (null != thingTypeFieldsTT && !thingTypeFieldsTT.isEmpty() && (udfValues != null)) {
            Map<String, Object> thingForFormula = ThingService.getInstance()
                    .getThingsToEvaluateFormula(udfValues, thingTypeFieldsTT);
            try {
                ThingService.getInstance().updateFormulaValuesForThingTypeUdfs(
                        recursivelyStack, thingForFormula, parameters.getTransactionDate(),
                        parameters.getExecuteTickle(), parameters.getUserActionExecutor(), parameters.getFillSource());
            } catch (Exception e) {
                throw new UserException("Error updating values for formula of dependent things.", e);
            }
        }
    }

    private void sendTickles(CrudParameters parameters, Map<String, Object> parentMap, List<Thing> children) {
        if (parameters.getExecuteTickle()) {
            Thing thing = parameters.getThingToProcess();
            if (parentMap != null && parentMap.size() == 0) {
                Thing thingParent = parameters.getThingParentToProcess();
                ThingService.getInstance().executeTickle(thingParent.getThingType().getThingTypeCode(),
                        thingParent.getSerial(), null, parameters.getTransactionDate(), false,
                        GroupService.getInstance().getMqttGroups(thingParent.getGroup()));
            }
            if (children != null && children.size() > 0) {
                for (Thing thingChild : children) {
                    ThingService.getInstance().executeTickle(thingChild.getThingType().getThingTypeCode(),
                            thingChild.getSerial(), null, parameters.getTransactionDate(), false,
                            GroupService.getInstance().getMqttGroups(thingChild.getGroup()));
                }
            }
            BrokerClientHelper.sendDeleteThingMessage(parameters.getThingType().getThingTypeCode(),
                    thing.getSerial(), false, GroupService.getInstance().getMqttGroups(thing.getGroup()));
            ThingService.getInstance().deleteCacheThing(thing);
        }
    }
}
