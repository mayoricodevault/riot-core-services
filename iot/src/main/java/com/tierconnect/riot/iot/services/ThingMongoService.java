package com.tierconnect.riot.iot.services;

import com.mongodb.*;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.sdk.dao.MongoExecutionException;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.bson.types.ObjectId;

import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringUtils.isBlank;


/**
 * Created by RUTH on 16/01/11.
 */
public class ThingMongoService {
    private static Logger logger = Logger.getLogger(ThingMongoService.class);

    private static ThingMongoDAO _thingMongoDAO;

    private static final List<String> skipFields = Collections.unmodifiableList(Arrays.asList("_id", "groupTypeId",
            "groupTypeName", "groupTypeCode", "groupId", "groupCode", "groupName", "thingTypeId", "thingTypeCode",
            "thingTypeName", "name", "serialNumber", "modifiedTime", "createdTime", "time", "parent", "children",
            "lastValue", "timeSeries", "sqn", "specName")
    );

    public static ThingMongoDAO getThingMongoDAO() {
        if (_thingMongoDAO == null) {
            _thingMongoDAO = new ThingMongoDAO();
        }
        return _thingMongoDAO;
    }

    static ThingMongoService instance;

    static {
        instance = new ThingMongoService();
    }

    private static ThingMongoService _thingMongoService = new ThingMongoService();

    public static ThingMongoService getInstance() {
        return instance;
    }

    /****************************************************************************
     * Create Thing In Mongo
     * ***************************************************************************/
    public void createThing(Thing thing, Map<String, Object> thingTypeFields, Date timestamp) {
        try {
            GroupType groupType = thing.getGroup().getGroupType();

            BasicDBObject doc = new BasicDBObject("_id", thing.getId());
            doc.append("groupTypeId", groupType.getId());
            doc.append("groupTypeName", groupType.getName());
            doc.append("groupTypeCode", groupType.getCode() == null ? "" : groupType.getCode());
            doc.append("groupId", thing.getGroup().getId());
            doc.append("groupCode", thing.getGroup().getCode() == null ? "" : thing.getGroup().getCode());
            doc.append("groupName", thing.getGroup().getName());
            doc.append("thingTypeId", thing.getThingType().getId());
            doc.append("thingTypeCode", thing.getThingType().getThingTypeCode());
            doc.append("thingTypeName", thing.getThingType().getName());
            doc.append("name", thing.getName());
            doc.append("serialNumber", thing.getSerial());

            Map<Long, String> thingTypeUdf = new HashMap<>();
            Map<String, Long> thingTypeUdfRemove = new HashMap<>();
            if (thingTypeFields != null) {
                for (Map.Entry<String, Object> current : thingTypeFields.entrySet()) {
                    HashMap<String, Object> tfvalue = (HashMap<String, Object>) current.getValue();
                    //If the UDF does not exist , it will be added
                    Object value = tfvalue.get("value");

                    BasicDBObject tfvalues = new BasicDBObject("thingTypeFieldId", tfvalue.get("thingTypeFieldId"))
                            .append("time", timestamp != null ? timestamp : tfvalue.get("time"))
                            .append("value", tfvalue.get("value"));

                    doc.append(current.getKey(), tfvalues);

                    //Update ThingType UDF , which are parent
                    if (value != null && isParentThingTypeUdf(thing.getThingType().getThingTypeFieldByName(current.getKey()))) {
                        Thing thingParentUdf = ThingService.getInstance().get((Long) ((BasicDBObject) value).get("_id"));
                        thingTypeUdfRemove.put(current.getKey(), thingParentUdf.getId());
                        if (thingParentUdf.getThingType().isIsParent()) {
                            //thingTypeUdf.put(thingTypeParent.getId(), tfkey);
                            thingTypeUdf.put(thingParentUdf.getId(), thingParentUdf.getThingType().getCode());
                        }
                    }
                }
            }
            //Insert MongoDB
            MongoDAOUtil.getInstance().things.insert(doc);
            //check to//get a new version of thing
            DBObject newThing =
                    MongoDAOUtil.getInstance().things.findOne(new BasicDBObject("_id", thing.getId()));

            //Add this thing to the <things>_children collection
            if (thingTypeUdf.size() > 0) {
                if (thingTypeUdfRemove.size() > 0) {
                    thingTypeUdfRemove.keySet().forEach(newThing::removeField);
                }

                for (Long id : thingTypeUdf.keySet()) {

                    DBObject query = new BasicDBObject("_id", id);
                    String action = "$push";
                    DBObject element = new BasicDBObject(thing.getThingType().getCode() + "_children", newThing);

                    DBObject listUpdate = new BasicDBObject(action, element);

                    logger.info(query);
                    logger.info(listUpdate);
                    MongoDAOUtil.getInstance().things.update(query, listUpdate);
                }
            }
        } catch (Exception e) {
            logger.error("Error occurred creating thing in MongoDB '" + thing.getSerialNumber() + "'.", e);
            throw new UserException("Error occurred creating thing in MongoDB '" + thing.getSerialNumber() + "'.", e);
        }
    }

    /**
     * Create a new Thing In Mongo
     *
     * @param thing
     * @param thingTypeFields
     * @param timestamp
     */
    public void createNewThing(Thing thing, Map<String, Object> thingTypeFields, Date timestamp, boolean executeTickle,
                               Stack recursivelyStack, Date modifiedTime) {
        try {
            GroupType groupType = thing.getGroup().getGroupType();
            User user = (User) SecurityUtils.getSubject().getPrincipal();

            BasicDBObject doc = new BasicDBObject("_id", thing.getId());
            doc.append("groupTypeId", groupType.getId());
            doc.append("groupTypeName", groupType.getName());
            doc.append("groupTypeCode", groupType.getCode() == null ? "" : groupType.getCode());
            doc.append("groupId", thing.getGroup().getId());
            doc.append("groupCode", thing.getGroup().getCode() == null ? "" : thing.getGroup().getCode());
            doc.append("groupName", thing.getGroup().getName());
            doc.append("thingTypeId", thing.getThingType().getId());
            doc.append("thingTypeCode", thing.getThingType().getThingTypeCode());
            doc.append("thingTypeName", thing.getThingType().getName());
            doc.append("name", thing.getName());
            doc.append("serialNumber", thing.getSerial());
            doc.append("modifiedTime", modifiedTime);
            doc.append("hierarchyCode", thingTypeFields.get("hierarchyCode"));
            doc.append("hierarchyName", thingTypeFields.get("hierarchyName"));
            doc.append( "hierarchyId", thingTypeFields.get("hierarchyId"));
            doc.append("createdTime", modifiedTime);
            doc.append("time", timestamp);
            thingTypeFields.remove("hierarchyCode");
            thingTypeFields.remove("hierarchyName");
            thingTypeFields.remove("hierarchyId");

            Map<Long, Object> thingTypeUdf = new HashMap<>();
            Map<String, Long> thingTypeUdfRemove = new HashMap<>();
            if (thingTypeFields != null) {
                for (Map.Entry<String, Object> current : thingTypeFields.entrySet()) {
                    HashMap<String, Object> tfvalue = (HashMap<String, Object>) current.getValue();
                    //Update ThingType UDF , which are parent
                    if ((tfvalue.get("value") != null) && (isParentThingTypeUdf(thing.getThingType().getThingTypeFieldByName(current.getKey())))) {
                        Thing thingParentUdf = ThingService.getInstance().get((Long) ((BasicDBObject) tfvalue.get("value")).get("_id"));
                        thingTypeUdfRemove.put(current.getKey(), thingParentUdf.getId());
                        if (thingParentUdf.getThingType().isIsParent()) {
                            thingTypeUdf.put(thingParentUdf.getId(), thingParentUdf);
                        }
                        ((Map<String, Object>) tfvalue.get("value")).remove(thing.getThingType().getCode() + "_children");
                    }
                    BasicDBObject tfvalues = new BasicDBObject("thingTypeFieldId", tfvalue.get("thingTypeFieldId"))
                            .append("time", timestamp != null ? timestamp : tfvalue.get("time"))
                            .append("value", tfvalue.get("value"));
                    doc.append(current.getKey(), tfvalues);
                }
            }
            //Insert MongoDB
            WriteResult insert = MongoDAOUtil.getInstance().things.insert(doc, WriteConcern.ACKNOWLEDGED);
            logger.info("Document insertion status: " + insert.toString());

            //Add this thing to the <things>_children collection of the parent
            if (thingTypeUdf.size() > 0) {
                if (thingTypeUdfRemove.size() > 0) {
                    thingTypeUdfRemove.keySet().forEach(doc::removeField);
                }
                //Add child into the children array's of the parent
                for (Object id : thingTypeUdf.keySet()) {
                    DBObject element = null;
                    DBObject query = new BasicDBObject("_id", (Long) id);
                    element = new BasicDBObject(thing.getThingType().getCode() + "_children", doc);
                    DBObject listUpdate = new BasicDBObject("$push", element);
                    logger.debug(query);
                    logger.debug(listUpdate);
                    MongoDAOUtil.getInstance().things.update(query, listUpdate);

                    //As the thing changed , Parent UDF has to be evaluated with its expressions
                    Thing parentUDF = (Thing) thingTypeUdf.get(id);
                    Map<String, Object> formulaValues = ThingsService.getInstance().getUDFFormulaValuesForParentUdf(
                            thing,
                            thingTypeFields,
                            timestamp);
                    if ((formulaValues != null) && (!formulaValues.isEmpty())) {
                        // update udf value for the parent
                        ThingService.getInstance().update(
                                recursivelyStack,
                                parentUDF, // thing
                                parentUDF.getThingType().getThingTypeCode(), // thingTypeCode
                                parentUDF.getGroup().getHierarchyName(false), // groupHierarchyCode
                                parentUDF.getName(), // name
                                parentUDF.getSerial(), // serialNumber
                                null, // parent
                                formulaValues, // udfs
                                null, // children
                                null, // childrenUdf
                                executeTickle, // executeTickle
                                false, // validateVisibility
                                timestamp, // transactionDate
                                true, // disableFMCLogic
                                null, // validations
                                null, // cache
                                true, // updateAndFlush
                                true, // recursivilyUpdate
                                user,
                                true
                        );
                    } else {
                        ThingMongoService.getInstance().updateThingInReferences(parentUDF);
                        if (executeTickle) {
                            ThingService.getInstance().executeTickle(
                                    parentUDF.getThingType().getThingTypeCode()
                                    , parentUDF.getSerial()
                                    , null
                                    , timestamp
                                    , false,
                                    GroupService.getInstance().getMqttGroups(parentUDF.getGroup()));
                        }
                    }


                }
            }
        } catch (Exception e) {
            logger.error("Error occurred creating thing in MongoDB '" + thing.getSerialNumber() + "'." + e.getMessage(), e);
            throw new UserException("Error occurred creating thing in MongoDB '" + thing.getSerialNumber() + "'.", e);
        }
    }

    /****************************************************************************
     * Create Thing In Mongo
     * ***************************************************************************/
    public void updateThing(Thing thing,
                            DBObject thingMongo,
                            Map<String, Object> thingTypeFields,
                            Date timestamp,
                            Date modifiedTime)
            throws MongoExecutionException {
        this.updateProcessThing(thing, thingMongo, thingTypeFields, timestamp, modifiedTime);
        this.updateThingInReferences(thing);
    }

    /****************************************************************************
     * Update Thing In Mongo
     * ***************************************************************************/
    public void updateProcessThing(Thing thing,
                                   DBObject thingMongo,
                                   Map<String, Object> thingTypeFields,
                                   Date timestamp,
                                   Date modifiedTime)
            throws MongoExecutionException {

        BasicDBObject doc = new BasicDBObject();

        Group group = thing.getGroup();
        GroupType groupType = group.getGroupType();
        ThingType thingType = thing.getThingType();

        try {
            /**
             * TODO review whitch fields will be updated
             */
            doc.append("serialNumber", thing.getSerial());
            doc.append("name", thing.getName());
            doc.append("groupTypeId", groupType.getId());
            doc.append("groupTypeName", groupType.getName());
            doc.append("groupTypeCode", isBlank(groupType.getCode()) ? "" : groupType.getCode());
            doc.append("groupId", group.getId());
            doc.append("groupCode", isBlank(group.getCode()) ? "" : group.getCode());
            doc.append("groupName", group.getName());
            doc.append("thingTypeId", thingType.getId());
            doc.append("thingTypeCode", thingType.getThingTypeCode());
            doc.append("thingTypeName", thingType.getName());
            doc.append("modifiedTime", modifiedTime);
            doc.append("hierarchyCode", thingTypeFields.get("hierarchyCode"));
            doc.append("hierarchyName", thingTypeFields.get("hierarchyName"));
            doc.append( "hierarchyId", thingTypeFields.get("hierarchyId"));
            doc.append("time", timestamp);
            thingTypeFields.remove("hierarchyCode");
            thingTypeFields.remove("hierarchyName");
            thingTypeFields.remove("hierarchyId");
        } catch (Exception e) {
            throw new MongoExecutionException("Unable to prepare thing with non-udfs", e);
        }

        //boolean udfTimeSeriesFound = false;
        BasicDBObject query1 = new BasicDBObject("_id", thing.getId());

        Map<Long, String> thingTypeUdfsToAdd = new HashMap<>();
        Map<Long, String> thingTypeUdfsToRemove = new HashMap<>();

        if (thingTypeFields != null) {
            for (Map.Entry<String, Object> current : thingTypeFields.entrySet()) {

                Map<String, Object> tfvalue = (Map<String, Object>) current.getValue();
                //If the UDF does not exist , it will be added
                Object oldValue = null;
                if (thingMongo != null) {
                    oldValue = thingMongo.get(current.getKey()) != null ?
                            ((BasicDBObject) thingMongo.get(current.getKey())).get("value") : null;
                }

                //Check if the Udf exist
                BasicDBObject query2 = new BasicDBObject(current.getKey(), new BasicDBObject("$exists", true));
                BasicDBList condtionalOperator = new BasicDBList();
                condtionalOperator.add(query1);
                condtionalOperator.add(query2);
                BasicDBObject andQuery = new BasicDBObject("$and", condtionalOperator);

                //collect ids to remove
                if (isParentThingTypeUdf(thingType.getThingTypeFieldByName(current.getKey()))) {
                    //add id into a hash map to delete
                    Long fieldId = parseId(thingMongo, current.getKey());

                    if (fieldId != null) {
                        //Map valueMap = (Map) aValue;
                        thingTypeUdfsToRemove.put(fieldId, current.getKey());
                    }
                }

                try {
                    if (MongoDAOUtil.getInstance().things.find(andQuery).size() > 0) {   //If there it has an old value, it needs tobe updated in other sites
                        if (oldValue != null) {
                            if (oldValue instanceof BasicDBObject) {
                                BasicDBObject queryOldValue = new BasicDBObject(thing.getThingType().getCode() + "_children._id"
                                        , thing.getId()).append("_id", ((BasicDBObject) oldValue).get("_id"));
                                BasicDBObject queryOldValue2 = new BasicDBObject("$pull"
                                        , new BasicDBObject(thing.getThingType().getCode() + "_children", new BasicDBObject("_id", thing.getId())));

                                MongoDAOUtil.getInstance().things.update(queryOldValue, queryOldValue2, false, true);
                            } else {
                                BasicDBObject queryOldValue = new BasicDBObject("_id", thing.getId());
                                MongoDAOUtil.getInstance().things.update(queryOldValue, new BasicDBObject("$unset", new
                                        BasicDBObject(current.getKey(), "")), false, true, WriteConcern.MAJORITY);
                            }
                        }

                        Object aValue = tfvalue.get("value");
                        BasicDBObject tfvalues = new BasicDBObject("thingTypeFieldId", tfvalue.get("thingTypeFieldId"))
                                .append("time", tfvalue.get("time") != null ? tfvalue.get("time") : timestamp)
                                .append("value", aValue);

                        //collect ids to add to this thing later
                        ThingType thingTypeAdd = thing.getThingType();
                        if (isParentThingTypeUdf(thingTypeAdd.getThingTypeFieldByName(current.getKey()))) {
                            Map valueMap = (Map) aValue;
                            if (valueMap != null) {
                                thingTypeUdfsToAdd.put(((Long) valueMap.get("_id")), current.getKey());
                            }
                        }

                        doc.append(current.getKey(), tfvalues);
                    }
                } catch (Exception e) {
                    throw new MongoExecutionException("Unable to update thing in multilevel references in mongo", e);
                }
                Object aValue = tfvalue.get("value");
                BasicDBObject tfvalues = new BasicDBObject("thingTypeFieldId", tfvalue.get("thingTypeFieldId"))
                        .append("time", tfvalue.get("time") != null ? tfvalue.get("time") : timestamp)
                        .append("value", aValue);

                //collect ids to add to this thing later
                ThingType thingTypeAdd = thing.getThingType();
                if (isParentThingTypeUdf(thingTypeAdd.getThingTypeFieldByName(current.getKey()))) {
                    Map valueMap = (Map) aValue;
                    if (valueMap != null) {
                        thingTypeUdfsToAdd.put(((Long) valueMap.get("_id")), current.getKey());
                    }
                }

                doc.append(current.getKey(), tfvalues);
            }
        }

        DBObject newThing = null;
        try {
            //update thing
            MongoDAOUtil.getInstance().things.update(query1, new BasicDBObject("$set", doc), false, true);
            newThing = MongoDAOUtil.getInstance().things.findOne(query1);
        } catch (Exception e) {
            throw new MongoExecutionException("Unable to update thing in mongo", e);
        }

        try {
            logger.info("Remove from collection " + thingTypeUdfsToRemove);
            //remove old references to thing
            for (Long id : thingTypeUdfsToRemove.keySet()) {
                //do the remove
                DBObject oldThingQuery = new BasicDBObject("_id", id);
                DBObject thisThing = new BasicDBObject("_id", thing.getId());
                DBObject findInList = new BasicDBObject(thing.getThingType().getCode() + "_children", thisThing);
                DBObject oldRemoveQuery = new BasicDBObject("$pull", findInList);

                MongoDAOUtil.getInstance().things.update(oldThingQuery, oldRemoveQuery);
            }
        } catch (Exception e) {
            throw new MongoExecutionException("Unable to disassociate thingTypeUDF in mongo", e);
        }

        try {
            logger.info("Add to collection " + thingTypeUdfsToAdd);
            //add this thing to the <things>_children collection
            if (thingTypeUdfsToAdd.size() > 0) {
                for (Long id : thingTypeUdfsToAdd.keySet()) {
                    //remove field from new thing
                    newThing.removeField(thingTypeUdfsToAdd.get(id));

                    //check to add or update
                    DBObject query = new BasicDBObject("_id", id);
                    query.put(thing.getThingType().getCode() + "_children._id", thing.getId());

                    DBObject inList = MongoDAOUtil.getInstance().things.findOne(query);
                    DBObject element;
                    String action;

                    if (inList == null) {
                        query = new BasicDBObject("_id", id);
                        action = "$push";
                        element = new BasicDBObject(thing.getThingType().getCode() + "_children", newThing);

                    }
                    //this should not happen since we delete the old reference
                    else {
                        action = "$set";
                        element = new BasicDBObject(thing.getThingType().getCode() + "_children.$", newThing);
                    }

                    DBObject listUpdate = new BasicDBObject(action, element);

                    MongoDAOUtil.getInstance().things.update(query, listUpdate);
                }
            }
        } catch (Exception e) {
            throw new MongoExecutionException("Unable to associate thingTypeUDF in mongo", e);
        }
    }

    /*****************************************************************
     * helper method to check if a thing type field is a thing type udf
     ***************************************************************/
    private boolean isParentThingTypeUdf(ThingTypeField thingTypeField) {
        boolean response = false;
        Long thingTypeUdfId = thingTypeField != null ? thingTypeField.getDataTypeThingTypeId() : null;
        if (thingTypeUdfId != null) {
            ThingType thingTypeUdf = ThingTypeService.getInstance().get(thingTypeUdfId);
            response = thingTypeUdf.isIsParent();
        }
        return response;
    }

    /*****************************************************************
     * parseId
     ***************************************************************/
    private Long parseId(DBObject toParse, String propertyName) {
        Long id = null;
        //remove from old collection
        if (toParse != null) {
            DBObject property = (DBObject) toParse.get(propertyName);
            if (property != null && property.get("value") != null && property.get("value") instanceof DBObject) {
                //parse old object looking for thing holding collection
                DBObject value = (DBObject) property.get("value");
                id = value != null ? (Long) value.get("_id") : null;
            }
        }
        return id;
    }

    /*****************************************************
     * This method updates a thing in its all references in things
     *
     * @param thing
     *****************************************************/
    public void updateThingInReferences(Thing thing) {
        Map<String, Object> thingParentMap = ThingTypeService.getInstance().getParentThingTypeUdf(thing);
        DBObject doc = null;
        //Update Thing Parent
        try {
            Thing parent = thing.getParent();
            if (parent != null) {
                doc = ThingMongoDAO.getInstance().getThing(thing.getId());
                ThingMongoDAO.getInstance().updateThingParent(thing.getId(), parent.getId(), doc);
                //Check if Parent has ParentUDF
                Map<String, Object> thingParentUdfMap = ThingTypeService.getInstance().getParentThingTypeUdf(parent);
                if (thingParentUdfMap != null && thingParentUdfMap.size() > 0) {
                    ThingMongoDAO.getInstance().updateThingTypeUDFParentWithChild(
                            parent.getId()
                            , parent.getThingType().getCode()
                            , thingParentUdfMap);
                }
            }
        } catch (Exception e) {
            throw new UserException("[Update Thing Parent for thing " + thing.getSerialNumber() + "]", e);
        }

        //Update Thing Children
        try {
            List<ThingTypeMap> thingTypesByParentId = thing.getThingType() != null ? ThingTypeMapService.getInstance().getThingTypeMapByParentId(thing.getThingType().getId()) : null;
            if (thingTypesByParentId != null && !thingTypesByParentId.isEmpty()) {
                List<Thing> things = ThingService.getInstance().getChildrenList(thing);
                if (things != null && things.size() > 0) {
                    if (doc == null) {
                        doc = ThingMongoDAO.getInstance().getThing(thing.getId());
                    }
                    ThingMongoDAO.getInstance().updateThingChildren(thing.getId(), doc, thingParentMap);
                }
            }
        } catch (Exception e) {
            logger.error("exception", e);
            throw new UserException("[Update Thing Children for thing " + thing.getSerialNumber() + "]", e);
        }

        //Update Thing Parent Udf with a child
        try {
            if (thingParentMap != null && thingParentMap.size() > 0) {
                ThingMongoDAO.getInstance().updateThingTypeUDFParentWithChild(
                        thing.getId()
                        , thing.getThingType().getCode()
                        , thingParentMap);
            }
        } catch (Exception e) {
            throw new UserException("[Update Thing Parent Udf with a child for thing " + thing.getSerialNumber() + "]", e);
        }

        //Update Thing Child with parent
        try {
            Map<String, Object> thingTypesChildrenUdf = ThingMongoService.getChildrenThingTypeUdf(thing);
            if (thingTypesChildrenUdf != null && thingTypesChildrenUdf.size() > 0) {
                ThingMongoDAO.getInstance().updateThingTypeUDFChildrenWithParent(thing.getId(), thingTypesChildrenUdf);
            }
        } catch (Exception e) {
            throw new UserException("[Update Thing Child with parent for thing " + thing.getSerialNumber() + "]", e);
        }

        try {
            List<Map<String, Object>> lstVisualParentUdf = thing.getThingType() != null ? ThingTypeService.getInstance().getReferencesThingTypeUdf(thing.getThingType()) : null;
            if (lstVisualParentUdf != null && lstVisualParentUdf.size() > 0) {
                if (doc == null) {
                    doc = ThingMongoDAO.getInstance().getThing(thing.getId());
                }
                UpdateReferencesRunnable runnableImp = new UpdateReferencesRunnable(thing, doc, lstVisualParentUdf);
                Thread updateReferencesThread = new Thread(runnableImp);
                updateReferencesThread.start();
            }
        } catch (Exception e) {
            throw new UserException("Preparing updating Thing in VISUAL Father for thing " + thing.getSerialNumber(), e);
        }


    }

    /*****************************************************
     * This method deletes a thing in all references in things
     *
     * @param thing Id of the thing
     *****************************************************/
    public void deleteThingInReferences(Thing thing) {
        Map<String, String> pathThingType = ThingTypePathService.getInstance().getMapPathsByThingType(thing.getThingType());
        if ((pathThingType != null) && (!pathThingType.isEmpty())) {
            //Update Thing in VISUAL Father
            List<Map<String, Object>> lstVisualParentUdf = ThingTypeService.getInstance().getReferencesThingTypeUdf(thing.getThingType());
            if (lstVisualParentUdf != null && lstVisualParentUdf.size() > 0) {
                //remove in thingType parent UDF
                ThingMongoDAO.getInstance().removeInVisualThingParentUdf(thing, lstVisualParentUdf);
            }
            //Update Thing in VISUAL Father
            ThingMongoDAO.getInstance().removeInThingParentUdf(thing);
        }
    }

    /***********************************************************************
     * Deletes a thing document from mongo
     ***********************************************************************/
    public Map<String, Object> deleteThing(Long id) throws MongoExecutionException {
        Map<String, Object> result = new HashMap<>();
        try {
            BasicDBObject document = new BasicDBObject();
            document.append("_id", id);
            WriteResult wr = MongoDAOUtil.getInstance().things.remove(document, WriteConcern.ACKNOWLEDGED);
            result.put("things", wr.getN());

            wr = MongoDAOUtil.getInstance().thingSnapshotIds.remove(document, WriteConcern.ACKNOWLEDGED);
            result.put("things", wr.getN());

            document = new BasicDBObject();
            document.append("value._id", id);
            wr = MongoDAOUtil.getInstance().thingSnapshots.remove(document, WriteConcern.ACKNOWLEDGED);
            result.put("things", wr.getN());
        } catch (Exception e) {
            throw new MongoExecutionException(e.getMessage(), e);
        }
        return result;
    }

    /***********************************************************
     * Get the children thingType Udf's
     ************************************************************/
    public static Map<String, Object> getChildrenThingTypeUdf(Thing thingParent) {
        Map<String, Object> result = new HashMap<>();
        if (thingParent != null && thingParent.getThingType() != null && thingParent.getThingType().isIsParent()) {
            List<ThingType> resultData = ThingTypeService.getInstance().getAllThingTypes();
            if (resultData != null && resultData.size() > 0) {
                for (ThingType thingType : resultData) {
                    if (thingType.getThingTypeFields() != null && thingType.getThingTypeFields().size() > 0) {
                        for (ThingTypeField thingTypeField : thingType.getThingTypeFields()) {
                            if (thingTypeField.getDataTypeThingTypeId() != null &&
                                    thingTypeField.getDataTypeThingTypeId().compareTo(thingParent.getThingType().getId()) == 0) {
                                Map<String, Object> data = new HashMap<>();
                                data.put("propertyName", thingTypeField.getName());
                                data.put("thingTypeCode", thingType.getCode());
                                result.put(thingType.getId().toString(), data);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Associate parent and one child. Appends to the list of the parent. Does not replace the list
     */
    public void associateChild(Thing parent, Thing oldParent, Thing child) {
        //disassociate first
        if (oldParent != null) {
            disAssociateChild(oldParent, child);
        }

        //db.bar.update( {user_id : 123456, "items.item_name" : {$ne : "my_item_two" }} ,
        //{$addToSet : {"items" : {'item_name' : "my_item_two" , 'price' : 1 }} } ,
        //false , true);
        logger.info("associate parent " + parent.getId() + " to child " + child.getId());

        //get child record
        DBObject childRecord = ThingMongoDAO.getInstance().getThing(child.getId());
        childRecord.removeField("parent");

        //associate/replace parent with child	`
        BasicDBObject find = new BasicDBObject("_id", parent.getId());
        BasicDBObject set = new BasicDBObject("$addToSet", new BasicDBObject("children", childRecord));
        MongoDAOUtil.getInstance().things.update(find, set, false, true);

        //update child to point to parent
        DBObject parentRecord = ThingMongoDAO.getInstance().getThing(parent.getId());
        if (parentRecord != null && parentRecord.containsField("children")) {
            parentRecord.removeField("children");
        }
//		BasicDBObject parentRecord = new BasicDBObject("id", parent.getId());
//		parentRecord.append("serial", parent.getSerial());
        BasicDBObject setParent = new BasicDBObject("$set", new BasicDBObject("parent", parentRecord));
        BasicDBObject findChild = new BasicDBObject("_id", new BasicDBObject("$eq", child.getId()));

        MongoDAOUtil.getInstance().things.update(findChild, setParent, true, true);

        ThingMongoService.getInstance().updateThingInReferences(parent);
    }

    /**
     * Associate parent and one child.
     * - Appends the 'child' object into the 'children' list of the parent.
     * - Appends the 'parent' object into the 'child' object
     *
     * @param parent    Thing object of the parent
     * @param oldParent Thing object of the old parent
     * @param childId   ID of the child Thing
     */
    public void associateParentChild(
            Thing parent
            , Thing oldParent
            , long childId) {
        //If it has an old Parent, disassociate it first
        if (oldParent != null) {
            disAssociateChild(oldParent, childId);
        }
        logger.info("associate parent " + parent.getId() + " to child " + childId);
        //get child record
        DBObject childRecord = ThingMongoDAO.getInstance().getThing(childId);
        childRecord.removeField("parent");

        //associate/replace parent with child
        BasicDBObject find = new BasicDBObject("_id", parent.getId());
        BasicDBObject set = new BasicDBObject("$addToSet", new BasicDBObject("children", childRecord));
        MongoDAOUtil.getInstance().things.update(find, set, false, true);
        //update child to point to parent
        DBObject parentRecord = ThingMongoDAO.getInstance().getThing(parent.getId());
        if (parentRecord.containsField("children")) {
            parentRecord.removeField("children");
        }
        BasicDBObject setParent = new BasicDBObject("$set", new BasicDBObject("parent", parentRecord));
        BasicDBObject findChild = new BasicDBObject("_id", new BasicDBObject("$eq", childId));
        MongoDAOUtil.getInstance().things.update(findChild, setParent, true, true);
    }

    /**
     * Disassociate parent and one child. Appends to the list of the parent. Does not replace the list
     */
    public void disAssociateChild(Thing parent, long childId) {
        logger.info("Dis-associate parent " + parent.getId() + " with " + childId);
        //remove parent reference from child
        BasicDBObject unset = new BasicDBObject("$unset", new BasicDBObject("parent", ""));
        BasicDBObject oldChildren = new BasicDBObject("_id", new BasicDBObject("$eq", childId));
        MongoDAOUtil.getInstance().things.update(oldChildren, unset);

        //remove from parent
        BasicDBObject find = new BasicDBObject("_id", parent.getId());
        BasicDBObject pull = new BasicDBObject("$pull",
                new BasicDBObject("children", new BasicDBObject("_id", childId)));
        MongoDAOUtil.getInstance().things.update(find, pull);

        //ThingMongoService.getInstance().updateThingInReferences(parent);
        //ThingMongoService.getInstance().updateThingInReferences(ThingService.getInstance().get(childId));
    }

    /**
     * Disassociate parent and one child. Appends to the list of the parent. Does not replace the list
     */
    public void disAssociateChild(Thing parent, Thing child) {
        disAssociateChild(parent, child.getId());
        logger.info("Updating thing in references");
        ThingMongoService.getInstance().updateThingInReferences(parent);
        ThingMongoService.getInstance().updateThingInReferences(child);
    }

    /**
     * Associate parent and one child. Appends to the list of the parent.
     * Does not replace entire list of children
     */
    public void associateChild(Thing parent, long childId) {
        //associateChild(parent, null, childId);
        associateChild(parent, null, ThingService.getInstance().get(childId));
    }

    /**
     * Associate parent and children. Replaces entire list
     * <p>
     * Important. Only use on insert
     */
    public void associateChildren(Thing parent, List<Thing> children) {
        List<Long> childrenIds = null;
        if (children != null) {
            childrenIds = children.stream().map(Thing::getId).collect(Collectors.toList());
        }
        associateChildren(parent, childrenIds, null);
    }

    /**
     * Associate parent and child. Replaces entire list.
     * Important. use when update so that the children do no reference the parent anymore
     */
    public void associateChildren(Thing parent, List<Long> childrenIds, List<Long> oldChildrenIds) {
        try {
            //remove link to parent from old children
            if (oldChildrenIds != null) {
                logger.info("dis-associate " + oldChildrenIds);
                //update all things to point to parent
                BasicDBObject unset = new BasicDBObject("$unset", new BasicDBObject("parent", ""));
                BasicDBObject oldChildren = new BasicDBObject("_id", new BasicDBObject("$in", oldChildrenIds));
                MongoDAOUtil.getInstance().things.update(oldChildren, unset, false, true);
            }

            //associate parent with list of children.
            BasicDBObject set = new BasicDBObject("$set", new BasicDBObject("children", ThingMongoDAO.getInstance().toDBList(childrenIds)));
            BasicDBObject find = new BasicDBObject("_id", parent.getId());
            MongoDAOUtil.getInstance().things.update(find, set);

            DBObject parentRecord = ThingMongoDAO.getInstance().getThing(parent.getId());
            parentRecord.removeField("children");

            //update all things to point to parent
//			BasicDBObject parentRecord = new BasicDBObject("id", parent.getId());
//			parentRecord.append("serial", parent.getSerial());
            BasicDBObject setParent = new BasicDBObject("$set", new BasicDBObject("parent", parentRecord));

            BasicDBObject findChildren = new BasicDBObject("_id", new BasicDBObject("$in", childrenIds));
            MongoDAOUtil.getInstance().things.update(findChildren, setParent, false, true);

            ThingMongoService.getInstance().updateThingInReferences(parent);
        } catch (MongoException me) {
            logger.warn("Mongo exception ", me);
        }
    }

    public void createUpdateThingSnapshot(Long thingID,
                                          String serialNumber,
                                          String thingTypeCode,
                                          Map<String, Object> changedFields,
                                          Date transactionDate,
                                          boolean isOlderSnapshot,
                                          Map<String, ThingTypeField> mapThinTypeFields,
                                          User user) {
        createUpdateThingSnapshot(thingID, serialNumber, thingTypeCode, changedFields, null,
                transactionDate, isOlderSnapshot, mapThinTypeFields, user);
    }

    public void createUpdateThingSnapshot(Long thingID,
                                          String serialNumber,
                                          String thingTypeCode,
                                          Map<String, Object> changedFields,
                                          Map<String, Object> missingNonUdfs,
                                          Date transactionDate,
                                          boolean isOlderSnapshot,
                                          Map<String, ThingTypeField> mapThinTypeFields,
                                          User user) {
        try {
            DBObject currentThingValue;
            boolean isLastValue = true;
            if (!isOlderSnapshot) {
                currentThingValue = ThingMongoDAO.getInstance().getThing(thingID);
            } else {
                currentThingValue = ThingMongoDAO.getPreviousSnapshotByTime(thingID, transactionDate.getTime());
                if (currentThingValue.keySet().isEmpty()) {
                    currentThingValue = ThingMongoDAO.getInstance().getThing(thingID);
                }
                if (missingNonUdfs != null && !missingNonUdfs.isEmpty()) {
                    for (Map.Entry<String, Object> e : missingNonUdfs.entrySet()) {
                        currentThingValue.put(e.getKey(), e.getValue());
                    }
                    if (!currentThingValue.containsField("createdTime")) {
                        currentThingValue.put("createdTime", new Date());
                    }
                }
                for (Map.Entry<String, Object> entry : changedFields.entrySet()) {
                    currentThingValue.put(entry.getKey(), new BasicDBObject((Map) entry.getValue()));
                }
                isLastValue = false;
            }

            boolean createSnapshot = true;
            boolean insertInSnapshotsIds = true;
            ObjectId objectIdSnapshot = new ObjectId();
            Date oldTime = transactionDate;
            Date createdTimeSnapshot = transactionDate;

            DBObject lastSnapshot = ThingMongoDAO.getInstance().getSnapshotCompleteByThingIdAndTime(thingID, transactionDate);
            DBObject lastSnapshotThingValue = null;
            if (lastSnapshot != null && lastSnapshot.get(Constants.SNAPSHOT_VALUE) != null) {
                lastSnapshotThingValue = (DBObject) lastSnapshot.get(Constants.SNAPSHOT_VALUE);
                boolean lastSnapshotThingValueIsTimeSeries = true;
                if (Utilities.isValidBoolean(String.valueOf(lastSnapshotThingValue.get(Constants.SNAPSHOT_TIMESERIES)))) {
                    lastSnapshotThingValueIsTimeSeries = Boolean.valueOf(String.valueOf(lastSnapshotThingValue.get(Constants.SNAPSHOT_TIMESERIES)));
                }
                if (!lastSnapshotThingValueIsTimeSeries) {
                    createSnapshot = false; // update last snapshot
                    oldTime = (Date) lastSnapshot.get(Constants.SNAPSHOT_TIME);
                    objectIdSnapshot = (ObjectId) lastSnapshot.get(Constants.SNAPSHOT_ID);
                    createdTimeSnapshot = (Date) (lastSnapshot.get(Constants.SNAPSHOT_CREATEDTIME) != null ?
                            lastSnapshot.get(Constants.SNAPSHOT_CREATEDTIME) : lastSnapshot.get(Constants.SNAPSHOT_TIME));
                    lastSnapshotThingValue = ThingMongoDAO.getInstance()
                            .getThingOfPreviousSnaphostByObjectIDAndTime(thingID, objectIdSnapshot, transactionDate);
                }
            } else {
                DBCursor cursorId = MongoDAOUtil.getInstance().thingSnapshotIds.find(new BasicDBObject("_id", thingID));
                if (cursorId.hasNext()) {
                    BasicDBObject docId = (BasicDBObject) cursorId.next();
                    for (Map<String, Object> timeStamp : (Collection<Map<String, Object>>) docId.get("blinks")) {
                        if ((Long) timeStamp.get("time") == transactionDate.getTime()) {
                            objectIdSnapshot = (ObjectId) timeStamp.get("blink_id");
                            insertInSnapshotsIds = false;
                        }
                    }
                }
            }

            currentThingValue = updateDocument(thingTypeCode, serialNumber, transactionDate, currentThingValue,
                    changedFields, mapThinTypeFields, lastSnapshotThingValue, isLastValue, isOlderSnapshot);

            boolean updatePreviousSnapshot = true;
            if (createSnapshot && lastSnapshot == null) {
                currentThingValue.put(Constants.SNAPSHOT_TIMESERIES, true);
                updatePreviousSnapshot = false;
            }
            if (updatePreviousSnapshot) {
                Map<String, Long> fieldTypes = getDataTypeByField(mapThinTypeFields, serialNumber, thingTypeCode, currentThingValue);
                (new Thread(new UpdateDwellPreviousSnapshot(changedFields, lastSnapshotThingValue, currentThingValue, true, transactionDate, fieldTypes, objectIdSnapshot))).start();
            }

            createUpdateDocument(thingID, createSnapshot, createdTimeSnapshot, transactionDate, oldTime, objectIdSnapshot, currentThingValue,
                    insertInSnapshotsIds, isOlderSnapshot, user);


        } catch (Exception e) {
            logger.error("Error occurred creating thing snapshot", e);
            throw new UserException("Error occurred creating thing snapshot for thing serielNumber: " + serialNumber + " id: " + thingID, e);
        }
    }

    private DBObject updateDocument(String thingTypeCode, String serialNumber, Date transactionDate,
                                    DBObject currentThingValue, Map<String, Object> changedFields,
                                    Map<String, ThingTypeField> mapThinTypeFields, DBObject lastSnapshotThingValue,
                                    boolean isLastValue, boolean isOlderSnapshot) {

        boolean containsUDFTimeSeries = false;
        for (String item : currentThingValue.keySet()) {
            if (!skipFields.contains(item) && currentThingValue.get(item) instanceof BasicDBObject) {

                // add changed and blinked
                ((BasicDBObject) currentThingValue.get(item)).append("dwellTime", 0);
                ((BasicDBObject) currentThingValue.get(item)).append("changed", true);
                ((BasicDBObject) currentThingValue.get(item)).append("blinked", true);

                // current
                Object currentValue = getValueUDF((BasicDBObject) currentThingValue.get(item), "value");
                Date currentTimeUDF = (Date) getValueUDF((BasicDBObject) currentThingValue.get(item), "time");

                if (currentTimeUDF != null) {
                    ((BasicDBObject) currentThingValue.get(item)).append("dwellTime", (transactionDate.getTime() - currentTimeUDF.getTime()));
                }

                if (changedFields.containsKey(item)) {
                    // check changes
                    if (lastSnapshotThingValue != null && lastSnapshotThingValue.containsField(item)) {

                        // previous
                        Object previousValue = getValueUDF((BasicDBObject) lastSnapshotThingValue.get(item), "value");
                        Date previousTime = (Date) getValueUDF((BasicDBObject) lastSnapshotThingValue.get(item), "time");
                        logger.info("check changes previousValue=[" + previousValue + "], currentValue=[" + currentValue + "]");
                        // If value has not changed
                        if (currentValue != null && previousValue != null
                                && (Objects.equals(currentValue, previousValue))
                                && previousTime != null) {
                            ((BasicDBObject) currentThingValue.get(item)).append("dwellTime", (transactionDate.getTime() - previousTime.getTime()));
                            ((BasicDBObject) currentThingValue.get(item)).append("time", previousTime);
                            ((BasicDBObject) currentThingValue.get(item)).append("changed", false);
                        }
                    }
                    ThingTypeField thingTypeField = mapThinTypeFields.get(item);
                    if (thingTypeField != null && changedFields.containsKey(item)) {
                        containsUDFTimeSeries = containsUDFTimeSeries || (thingTypeField.getTimeSeries() != null && thingTypeField.getTimeSeries());
                        // update udf Zone
                        if (thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_ZONE.value) == 0) {
                            setSnapshotZoneProperties(lastSnapshotThingValue, currentThingValue, item, transactionDate);
                        }
                    } else if (thingTypeField == null) {
                        logger.warn("Unable to find thingTypeField for thingTypeFieldName=" + item
                                + " thingTypeCode" + thingTypeCode + " serialNumber=" + serialNumber
                                + " newThingSnapshot=" + currentThingValue);
                    }
                } else if (lastSnapshotThingValue != null && lastSnapshotThingValue.containsField(item)) {
                    // previous
                    Object previousValue = getValueUDF((BasicDBObject) lastSnapshotThingValue.get(item), "value");
                    Date previousTime = (Date) getValueUDF((BasicDBObject) lastSnapshotThingValue.get(item), "time");

                    if (currentValue != null && Objects.equals(currentValue, previousValue)) {
                        Long dwell = transactionDate.getTime() - previousTime.getTime();
                        ((BasicDBObject) currentThingValue.get(item)).put("time", previousTime);
                        ((BasicDBObject) currentThingValue.get(item)).put("dwellTime", dwell);
                        ((BasicDBObject) currentThingValue.get(item)).put("blinked", false);
                        ((BasicDBObject) currentThingValue.get(item)).put("changed", false);
                    }
                    // update properties of UDF ZONE
                    ThingTypeField thingTypeField = mapThinTypeFields.get(item);
                    if (thingTypeField != null) {
                        if (thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_ZONE.value) == 0) {
                            for (String subField : Zone.zoneDwellProperties) {
                                Date zonePropUdfDate = (Date) ((BasicDBObject) ((BasicDBObject) lastSnapshotThingValue.get(item)).get("value")).get(subField + "Time");
                                Long zonePropDwell = transactionDate.getTime() - zonePropUdfDate.getTime();
                                ((BasicDBObject) ((BasicDBObject) currentThingValue.get(item)).get("value")).put(subField + "DwellTime", zonePropDwell);
                                ((BasicDBObject) ((BasicDBObject) currentThingValue.get(item)).get("value")).put(subField + "Changed", false);
                                ((BasicDBObject) ((BasicDBObject) currentThingValue.get(item)).get("value")).put(subField + "Blinked", false);
                            }
                        }
                    } else {
                        logger.warn("Unable to find thingTypeField for thingTypeFieldName=" + item
                                + " thingTypeCode" + thingTypeCode + " serialNumber=" + serialNumber
                                + " newThingSnapshot=" + currentThingValue.toString());
                    }
                } else if (currentValue != null) {
                    // update udf Zone
                    ThingTypeField thingTypeField = mapThinTypeFields.get(item);
                    if (thingTypeField != null && thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_ZONE.value) == 0) {
                        setSnapshotZoneProperties(lastSnapshotThingValue, currentThingValue, item, (currentTimeUDF != null ? currentTimeUDF : transactionDate));
                    }
                }
            }
        }
        if (isOlderSnapshot) {
            currentThingValue.put(Constants.TIME, transactionDate);
        }
        currentThingValue.put(Constants.SNAPSHOT_LASTVALUE, isLastValue);
        currentThingValue.put(Constants.SNAPSHOT_TIMESERIES, containsUDFTimeSeries);
        return currentThingValue;
    }

    private void createUpdateDocument(Long thingID, boolean createSnapshot, Date createdTimeSnapshot,
                                      Date transactionDate, Date oldTime, ObjectId objectIdSnapshot,
                                      DBObject currentThingValue, boolean insertInSnapshotsIds,
                                      boolean isOlderSnapshot, User user)
            throws MongoExecutionException {

        BasicDBObject saveThingSnapshot = new BasicDBObject(Constants.SNAPSHOT_ID, objectIdSnapshot);
        saveThingSnapshot.append(Constants.SNAPSHOT_VALUE, currentThingValue);
        saveThingSnapshot.append(Constants.SNAPSHOT_TIME, transactionDate);
        saveThingSnapshot.append(Constants.SNAPSHOT_CREATEDTIME, createdTimeSnapshot);
        if (createSnapshot) {
            logger.debug("DOC SAVED doc: " + saveThingSnapshot.toString());
            MongoDAOUtil.getInstance().thingSnapshots.save(saveThingSnapshot);
            if (insertInSnapshotsIds) {
                ThingMongoDAO.getInstance().pushThingSnapshotId(thingID, transactionDate, objectIdSnapshot, currentThingValue, user, isOlderSnapshot);
            }
        } else {
            logger.debug("UPDATE doc: " + saveThingSnapshot.toString());
            MongoDAOUtil.getInstance().thingSnapshots.update(new BasicDBObject(Constants.SNAPSHOT_ID, objectIdSnapshot), saveThingSnapshot);
            ThingMongoDAO.getInstance().updateThingSnapshotId(thingID, oldTime, transactionDate, objectIdSnapshot, user);
        }
    }

    private Object getValueUDF(BasicDBObject udf, String key) {
        Object value = null;
        if (udf != null && udf.get(key) != null) {
            value = udf.get(key);
            if (value instanceof BasicDBObject || value instanceof Map) {
                if (((Map) value).containsKey("_id")) {
                    value = ((Map) value).get("serialNumber");
                } else if (((Map) value).containsKey("code")) {
                    value = ((Map) value).get("code");
                }
            }
        }
        return value;
    }

    private Map<String, Long> getDataTypeByField(Map<String, ThingTypeField> thingTypeFieldMap, String serialNumber, String thingTypeCode, DBObject dbObject) {
        Map<String, Long> result = new HashMap<>();
        for (Map.Entry<String, ThingTypeField> thingTypeFieldEntry : thingTypeFieldMap.entrySet()) {
            try {
                result.put(thingTypeFieldEntry.getKey(), thingTypeFieldEntry.getValue().getDataType().getId());
            } catch (Exception e) {
                logger.warn("Unable to find dataType for thingTypeField "
                        + " thingTypeFieldName=" + thingTypeFieldEntry.getKey()
                        + " thingTypeFieldId=" + thingTypeFieldEntry.getValue().getId()
                        + " thingTypeCode" + thingTypeCode
                        + " serialNumber=" + serialNumber
                        + " newThingSnapshot=" + dbObject.toString());
            }
        }
        return result;
    }


    private class UpdateDwellPreviousSnapshot implements Runnable {

        Map<String, Object> changedFields = null;
        DBObject doc = null;
        DBObject newThingCollection = null;
        boolean saveDwellHistory = false;
        Date time = null;
        Map<String, Long> fieldTypes = null;
        ObjectId snapshotId = null;

        public UpdateDwellPreviousSnapshot(Map<String, Object> changedFields,
                                           DBObject doc,
                                           DBObject newThingCollection,
                                           boolean saveDwellHistory,
                                           Date time,
                                           Map<String, Long> fieldTypes,
                                           ObjectId snapshotId) {

            this.changedFields = changedFields;
            this.doc = doc;
            this.newThingCollection = newThingCollection;
            this.saveDwellHistory = saveDwellHistory;
            this.time = time;
            this.fieldTypes = fieldTypes;
            this.snapshotId = snapshotId;
        }

        @Override
        public void run() {

            for (Map.Entry<String, Object> changedField : changedFields.entrySet()) {

                //Get field value from thing in mongo
                Object docField = null;
                Object docFieldValue = null;
                if (doc != null && doc.containsField(changedField.getKey())) {
                    docField = doc.get(changedField.getKey());
                    docFieldValue = ((BasicDBObject) docField).get("value");
                    if (docFieldValue instanceof BasicDBObject) {
                        if (((Map) docFieldValue).containsKey("_id")) {
                            docFieldValue = ((BasicDBObject) docFieldValue).get("serialNumber");
                        } else if (((Map) docFieldValue).containsKey("code")) {
                            docFieldValue = ((BasicDBObject) docFieldValue).get("code");
                        }
                    }
                }

                //Get field value from changedFields
                Object changedFieldValue = null;
                if (((Map) changedField.getValue()).containsKey("value")) {
                    changedFieldValue = ((Map) changedField.getValue()).get("value");
                    if (changedFieldValue instanceof Map) {
                        if (((Map) changedFieldValue).containsKey("_id")) {
                            changedFieldValue = ((Map) changedFieldValue).get("serialNumber");
                        } else if (((Map) changedFieldValue).containsKey("code")) {
                            changedFieldValue = ((Map) changedFieldValue).get("code");
                        }
                    }
                }
                //If value has changed
                if (changedFieldValue != null && docFieldValue != null && !(changedFieldValue.equals(docFieldValue))
                        || changedFieldValue == null && docFieldValue != null
                        || changedFieldValue != null && docFieldValue == null) {
                    if (saveDwellHistory) {

                        //Update dwell of previous field change
                        BasicDBObject query = new BasicDBObject("value._id", Long.parseLong(doc.get("_id").toString()));
                        query.append("_id", new BasicDBObject("$ne", snapshotId));
                        query.append("time", new BasicDBObject("$lt", time));
                        query.append("value." + changedField.getKey() + ".changed", new BasicDBObject("$eq", true));

                        BasicDBObject sort = new BasicDBObject("time", -1);

                        DBCursor previousDocCursor = MongoDAOUtil.getInstance().
                                thingSnapshots.find(query).sort(sort).limit(1);

                        if (previousDocCursor.hasNext()) {
                            DBObject previousDoc = previousDocCursor.next();
                            Object fieldTime = previousDoc.get("value");
                            fieldTime = ((Map) fieldTime).get(changedField.getKey());
                            fieldTime = ((Map) fieldTime).get("time");
                            Date lastDateTime = (Date) fieldTime;
                            Long actualDwellTime = time.getTime() - lastDateTime.getTime();

                            //Update snapshot
                            BasicDBObject previousUpdateDoc = new BasicDBObject("value." + changedField.getKey() + ".dwellTime", actualDwellTime);
                            MongoDAOUtil.getInstance().
                                    thingSnapshots.
                                    update(new BasicDBObject("_id", (ObjectId) previousDoc.get("_id")),
                                            new BasicDBObject("$set", previousUpdateDoc), false, false);
                        }
                    }
                }

                //Update previous snapshots for zone properties
                if (!changedField.getKey().equals("parent") &&
                        !changedField.getKey().equals("children") &&
                        doc != null &&
                        (!doc.containsField(changedField.getKey()) || doc.get(changedField.getKey()) instanceof BasicDBObject) &&
                        fieldTypes.containsKey(changedField.getKey()) &&
                        fieldTypes.get(changedField.getKey()).compareTo(ThingTypeField.Type.TYPE_ZONE.value) == 0 &&
                        saveDwellHistory) {
                    saveSnapshotZoneProperties(doc, newThingCollection, changedField.getKey(), time);
                }
            }
            updatePreviousSnapshot();
        }

        private void updatePreviousSnapshot() {
            BasicDBObject query = new BasicDBObject("value._id", Long.parseLong(doc.get("_id").toString()));
            query.append("_id", new BasicDBObject("$ne", snapshotId));
            query.append("time", new BasicDBObject("$lt", time));
            BasicDBObject lastValueFalse = new BasicDBObject("value.lastValue", false);
            MongoDAOUtil.getInstance().
                    thingSnapshots.update(query, new BasicDBObject("$set", lastValueFalse), false, true);
        }

        /**
         * Method to save SnapshotZoneProperties: facilityMap, zoneGroup, zoneType
         *
         * @param lastSnapshot       Last snapshot of the thing
         * @param newThingCollection New thing document who is going to be inserted in thingSnapshots
         * @param fieldChanged       name of the field which is evaluated Ex: zone
         * @param time               Actual time
         */
        public void saveSnapshotZoneProperties(
                DBObject lastSnapshot,
                DBObject newThingCollection,
                String fieldChanged,
                Date time) {
            if ((newThingCollection.get(fieldChanged)) != null
                    && (((BasicDBObject) newThingCollection.get(fieldChanged)).get("value") != null)) {

                Map<String, Object> zoneMap = ((Map) ((BasicDBObject) newThingCollection.get(fieldChanged)).get("value"));
                DBObject setValues = new BasicDBObject();
                for (String zoneDwellProperty : Zone.zoneDwellProperties) {
                    if (zoneMap.containsKey(zoneDwellProperty)) {
                        updateZoneSnapshot(lastSnapshot, newThingCollection, fieldChanged, zoneDwellProperty, time);
                        setValues.put("zone.value." + zoneDwellProperty + "Time", zoneMap.get(zoneDwellProperty + "Time"));
                    }
                }
            }
        }

        /**
         * This method updates the dwellTime for zone UDFs: facilityMap, zoneGroup, zoneType
         *
         * @param lastSnapshot       Last snapshot of the thing
         * @param newThingCollection New thing document who is going to be inserted in thingSnapshots
         * @param fieldChanged       name of the field which is evaluated Ex: zone
         * @param subFieldChanged    name of the subField wwhisch is evalueated Ex:facilityMap, zoneGroup, zoneType
         * @param time               Actual time
         */
        public void updateZoneSnapshot(
                DBObject lastSnapshot,
                DBObject newThingCollection,
                String fieldChanged,
                String subFieldChanged,
                Date time) {
            //If the values are different
            String valueLastSnapshot = null;
            if (lastSnapshot.get(fieldChanged) != null
                    && ((DBObject) lastSnapshot.get(fieldChanged)).get("value") != null
                    && (((DBObject) ((DBObject) lastSnapshot.get(fieldChanged)).get("value")).get(subFieldChanged)) != null) {
                valueLastSnapshot = (((DBObject) ((DBObject) lastSnapshot.get(fieldChanged)).get("value")).get
                        (subFieldChanged)).toString();
            }
            Map zoneValue = null;
            String valueNewThing = null;
            if (newThingCollection.get(fieldChanged) != null) {

                zoneValue = (Map) ((Map) newThingCollection.get(fieldChanged)).get("value");
                if (zoneValue != null && zoneValue.get(subFieldChanged) != null) {

                    valueNewThing = zoneValue.get(subFieldChanged).toString();
                    //If value in LastSnapshot is different than the new one,update dwell for previous snapshot and initialize dwell
                    if ((valueLastSnapshot != null) && (valueNewThing != null) &&
                            !valueLastSnapshot.equals(valueNewThing)) {
                        updateDwellOfPreviousSnapshot(fieldChanged, subFieldChanged, time);
                    }
                }
            }
        }


        /**
         * This method updates Dwell time of the previous snapshot
         *
         * @param fieldChanged    Name of the field who changed
         * @param subFieldChanged Name of the sub field who changed
         * @param time            Actual time
         */
        public void updateDwellOfPreviousSnapshot(
                String fieldChanged
                , String subFieldChanged
                , Date time) {
            //Update dwell of previous field change
            BasicDBObject query = new BasicDBObject("value._id", Long.parseLong(doc.get("_id").toString()));
            query.append("_id", new BasicDBObject("$ne", snapshotId));
            query.append("time", new BasicDBObject("$lt", time));
            query.append("value." + fieldChanged + ".value." + subFieldChanged + "Changed", new BasicDBObject("$eq",
                    true));
            BasicDBObject sort = new BasicDBObject("time", -1);
            DBCursor previousDocCursor = MongoDAOUtil.getInstance().
                    thingSnapshots.find(query).sort(sort).limit(1);

            if (previousDocCursor.hasNext()) {
                DBObject previousDoc = previousDocCursor.next();
                Object fieldTime = previousDoc.get("value");
                fieldTime = ((Map) ((Map) ((Map) fieldTime).get(fieldChanged)).get("value")).get(subFieldChanged + "Time");
                Date lastDateTime = (Date) fieldTime;
                Long actualDwellTime = time.getTime() - lastDateTime.getTime();

                //Update snapshot
                BasicDBObject previousUpdateDoc = new BasicDBObject("value." + fieldChanged + ".value." + subFieldChanged + "DwellTime",
                        actualDwellTime); //.-
                MongoDAOUtil.getInstance().
                        thingSnapshots.
                        update(new BasicDBObject("_id", (ObjectId) previousDoc.get("_id")),
                                new BasicDBObject("$set", previousUpdateDoc), false, false);
            }
        }
    }

    public void setSnapshotZoneProperties(
            DBObject lastSnapshot,
            DBObject newThingCollection,
            String fieldChanged,
            Date time) {
        if ((newThingCollection.get(fieldChanged)) != null
                && (((BasicDBObject) newThingCollection.get(fieldChanged)).get("value") != null)) {

            Map<String, Object> zoneMap = ((Map) ((BasicDBObject) newThingCollection.get(fieldChanged)).get("value"));
            DBObject setValues = new BasicDBObject();
            int aux = 0;
            for (String zoneDwellProperty : Zone.zoneDwellProperties) {
                if (zoneMap.containsKey(zoneDwellProperty)) {
                    calculateZoneSnapshot(lastSnapshot, newThingCollection, fieldChanged, zoneDwellProperty, time);
                    setValues.put("zone.value." + zoneDwellProperty + "Time", zoneMap.get(zoneDwellProperty + "Time"));
                    aux++;
                }
            }
        }
    }

    /**
     * This method calculate the dwellTime for zone UDFs: facilityMap, zoneGroup, zoneType
     *
     * @param lastSnapshot       Last snapshot of the thing
     * @param newThingCollection New thing document who is going to be inserted in thingSnapshots
     * @param fieldChanged       name of the field which is evaluated Ex: zone
     * @param subFieldChanged    name of the subField wwhisch is evalueated Ex:facilityMap, zoneGroup, zoneType
     * @param time               Actual time
     */
    public void calculateZoneSnapshot(
            DBObject lastSnapshot,
            DBObject newThingCollection,
            String fieldChanged,
            String subFieldChanged,
            Date time) {
        //If the values are different
        String valueLastSnapshot = null;
        if (lastSnapshot != null
                && lastSnapshot.get(fieldChanged) != null
                && ((DBObject) lastSnapshot.get(fieldChanged)).get("value") != null
                && (((DBObject) ((DBObject) lastSnapshot.get(fieldChanged)).get("value")).get(subFieldChanged)) != null) {
            valueLastSnapshot = (((DBObject) ((DBObject) lastSnapshot.get(fieldChanged)).get("value")).get
                    (subFieldChanged)).toString();
        }
        Map zoneValue = null;
        String valueNewThing = null;
        if (newThingCollection.get(fieldChanged) != null) {

            zoneValue = (Map) ((Map) newThingCollection.get(fieldChanged)).get("value");
            if (zoneValue != null && zoneValue.get(subFieldChanged) != null) {

                valueNewThing = zoneValue.get(subFieldChanged).toString();
                //If value in LastSnapshot is different than the new one,update dwell for previous snapshot and initialize dwell
                if ((valueLastSnapshot != null) && (valueNewThing != null) &&
                        !valueLastSnapshot.equals(valueNewThing)) {
                    zoneValue.put(subFieldChanged + "DwellTime", 0L);
                    zoneValue.put(subFieldChanged + "Changed", true);
                    zoneValue.put(subFieldChanged + "Blinked", true);
                }
                //If last snapshot exists and old value and new one are equal then increment dwell
                else if (valueLastSnapshot != null && lastSnapshot.containsField(fieldChanged)) {
                    Date dateTime = (Date) ((BasicDBObject) ((BasicDBObject) lastSnapshot.get(fieldChanged)).get("value")).get
                            (subFieldChanged + "Time");
                    zoneValue.put(subFieldChanged + "Time", dateTime);
                    Long dwellTime = time.getTime() - ((Date) zoneValue.get(subFieldChanged + "Time")).getTime();
                    zoneValue.put(subFieldChanged + "DwellTime", dwellTime);
                    zoneValue.put(subFieldChanged + "Changed", false);
                    zoneValue.put(subFieldChanged + "Blinked", false);
                }
                //If last Snapshot does not exist, then initialize all values:time, dwell and changed
                else {
                    Long dwellTime = 0L;
                    zoneValue.put(subFieldChanged + "Time", time);
                    zoneValue.put(subFieldChanged + "DwellTime", dwellTime);
                    zoneValue.put(subFieldChanged + "Changed", true);
                    zoneValue.put(subFieldChanged + "Blinked", true);
                }
            }
        }
    }


    /**
     * Update zone properties dwell (if exists zone as type)
     *
     * @param newThingCollection The new Collection created for snapshot
     * @param item               name of the field zone
     * @param doc                Last snapshot of the thing in time n-1
     * @param user               User who does the modification
     * @param time               Time of the snapshot
     */
    public void setZoneNoChangeDwellTime(
            DBObject newThingCollection
            , String item
            , DBObject doc
            , User user
            , Date time) {
//        if ((ThingTypeFieldService.getInstance().get((Long) ((BasicDBObject) newThingCollection.get(item)).get("thingTypeFieldId"))
//                .getDataType().getId().compareTo(ThingTypeField.Type.TYPE_ZONE.value) == 0) &&
//                ConfigurationService.getAsBoolean(user, "saveDwellTimeHistory")) {
        if ((ThingTypeFieldService.getInstance().get((Long) ((BasicDBObject) newThingCollection.get(item)).get("thingTypeFieldId"))
                .getDataType().getId().compareTo(ThingTypeField.Type.TYPE_ZONE.value) == 0)) {
            for (String subField : Zone.zoneDwellProperties) {
                if (((Map) ((BasicDBObject) newThingCollection.get(item)).get("value")).containsKey(subField) &&
                        (doc.get(item) != null) && (((BasicDBObject) doc.get(item)).get("value") != null) &&
                        (((BasicDBObject) ((BasicDBObject) doc.get(item)).get("value")).get(subField + "Time") != null)) {
                    Date zonePropUdfDate = (Date) ((BasicDBObject) ((BasicDBObject) doc.get(item)).get("value")).get(subField + "Time");
                    Long zonePropDwell = time.getTime() - zonePropUdfDate.getTime();
                    ((BasicDBObject) ((BasicDBObject) newThingCollection.get(item)).get("value")).put(subField + "DwellTime", zonePropDwell);
                }
            }
        }
    }

    /**
     * Runnable Class for updating references
     */
    public class UpdateReferencesRunnable implements Runnable {
        private Thing thing = null;
        private DBObject doc = null;
        private List<Map<String, Object>> lstVisualParentUdf = null;


        public UpdateReferencesRunnable(Thing thing, DBObject doc, List<Map<String, Object>> lstVisualParentUdf) {
            this.thing = thing;
            this.doc = doc;
            this.lstVisualParentUdf = lstVisualParentUdf;
        }

        /**
         * Even though the commit transaction, this method does not do any update in MYSQL
         * because it is an isolated transaction
         */
        public void run() {
            if (thing != null) {
                //Update Thing in VISUAL Father
                try {
                    ThingMongoDAO.getInstance().updateVisualThingParentWithChild(thing, doc, lstVisualParentUdf);
                } catch (Exception e) {
                    throw new UserException("[Update Thing in VISUAL Father for thing " + thing.getSerialNumber() + "]", e);
                }

            }
        }
    }

    /**
     * Associates the thingTypeUDF to a thing
     *
     * @param thing         thing to associate
     * @param thingTypeUDFs List of thingTypeUDFs IDs
     * @throws MongoExecutionException If an error occurs, MongoExecutionException is thrown
     */
    public void associateThingTypeUDF(Thing thing, List<BasicDBObject> thingTypeUDFs) throws MongoExecutionException {
        try {
            logger.info("Associating " + thingTypeUDFs);
            if (thingTypeUDFs != null && thingTypeUDFs.size() > 0) {
                DBObject thingUpdate = MongoDAOUtil.getInstance().things.findOne(new BasicDBObject("_id", thing.getId()));

                for (BasicDBObject thingTypeUDF : thingTypeUDFs) {
                    Long id = thingTypeUDF.getLong("_id");
                    DBObject query = new BasicDBObject("_id", id);
                    query.put(thing.getThingType().getCode() + "_children._id", thing.getId());

                    Long countList = MongoDAOUtil.getInstance().things.count(query);
                    DBObject element;
                    String action;
                    if (countList > 0L) {
                        action = "$set";
                        element = new BasicDBObject(thing.getThingType().getCode() + "_children.$", thingUpdate);
                    } else {
                        query = new BasicDBObject("_id", id);
                        action = "$push";
                        element = new BasicDBObject(thing.getThingType().getCode() + "_children", thingUpdate);
                        query.removeField(thing.getThingType().getCode() + "_children._id");
                    }
                    DBObject listUpdate = new BasicDBObject(action, element);
                    MongoDAOUtil.getInstance().things.update(query, listUpdate);
                }
            }
        } catch (Exception e) {
            throw new MongoExecutionException("Unable to reassociate thingTypeUDF in mongo", e);
        }
    }
}
