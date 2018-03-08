package com.tierconnect.riot.iot.utils;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;

/**
 * Created by fflores on 10/23/2015.
 */
public class Cache {
    static Logger logger = Logger.getLogger(Cache.class);

    private Map<String, Group> groupCache = new HashMap<>();
    private Map<String, Group> groupByCodeCache = new HashMap<>();
    private Map<Long, String> hierarchyNameGroupByIdCache = new HashMap<>();
    private Map<String, Zone> zoneCache = new HashMap<>();
    private Map<String, ThingType> thingTypeCache = new HashMap<>();
    private Map<Long, ThingType> thingTypeByIdCache = new HashMap<>();
    private Map<String, ThingTypeTemplate> thingTypeTemplateCache = new HashMap<>();
    private Map<String, Shift> shiftCache = new HashMap<>();
    private Map<String, LogicalReader> logicalReaderCache = new HashMap<>();
    private Map<String, Thing> thingCache = new HashMap<>();


    /**
     * Get a thing type by code. First check on the cache, if not found check on the storage. If found, store
     * in cache.
     * @param code thing type code
     * @return thing type that matches code
     * @throws UserException if not a valid code or more than one thing type that matches
     * the code is found
     */
    public ThingType getThingType(String code) throws UserException
    {
        return getThingTypeByCode(code, true);
    }

    /**
     * Get a thing type by code. First check on the cache, if not found check on the storage. If found, store
     * in cache.
     * @param code thing type code
     * @param required if required and not found, throw an InvalidEntityException.
     * @return thing type that matches code, null if required == false
     * @throws UserException if required == true and not a valid code or more than one thing type
     * that matches the same code is found
     */
    public ThingType getThingTypeByCode(String code, boolean required) //throws InvalidEntityException
    {
        logger.info("getting thing type " + code);
        ThingType thingType = thingTypeCache.get(code);
        if (thingType == null)
        {
            try {
                thingType = ThingTypeService.getInstance().getByCode(code);
                if (thingType != null)
                {
                    thingTypeCache.put(code, thingType);
                }
                //throw exception if flag set, otherwise return null
                else if(required)
                {
                    throw new UserException(
                            "Unable to get thing type with code " + code + ". Code not found.");
                }
            } catch (NonUniqueResultException e) {
                throw new UserException(
                        "Unable to get thing type with code " + code + ". Duplicate code.", e);
            }
        }

        return thingType;
    }

    public ThingType getThingTypeById(Long id)
    {
        logger.info("getting thing type by id " + id);
        ThingType thingType = thingTypeByIdCache.get(id);
        if (thingType == null)
        {
            thingType = ThingTypeService.getInstance().get(id);
            if (thingType != null) {
                thingTypeByIdCache.put(id, thingType);
            }
            else {
                throw new UserException("Unable to get thingType with id " + id + ". Id not found.");
            }
        }
        return thingType;
    }

    // Remove this method when the group code is displayed instead of the hierarchical name
    public Group getGroup(String hierarchyCode)
    {
        logger.info("getting group " + hierarchyCode);
        Group group = groupCache.get(hierarchyCode);
        if (group == null)
        {
            try
            {
                group = GroupService.getInstance().getByHierarchyCode(hierarchyCode);
                if (group != null) {
                    groupCache.put(hierarchyCode, group);
                }
                else {
                    if (hierarchyCode.isEmpty())
                        throw new UserException("Thing Type Owner Group cannot be empty.");
                    throw new UserException("Thing Type Owner Group Error. '" + hierarchyCode + "' code not found.");
                }
            }
            catch (NonUniqueResultException e)
            {
                throw new UserException("Thing Type Owner Group Error. '" + hierarchyCode + "' Duplicate code.", e);
            }
        }
        return group;
    }

    public ThingTypeTemplate getThingTypeTemplate(String name)
    {
        logger.info("getting Thing Type Template " + name);
        ThingTypeTemplate thingTypeTemplate = thingTypeTemplateCache.get(name);
        if (thingTypeTemplate == null)
        {
            try
            {
                thingTypeTemplate = ThingTypeTemplateService.getThingTypeTemplateDAO().selectBy("name",name);
                if (thingTypeTemplate != null) {
                    thingTypeTemplateCache.put(name, thingTypeTemplate);
                }
            }
            catch (Exception e)
            {
                throw new UserException("Unable to get Thing Type Template with name " + name + ". Duplicate name.", e);
            }
        }
        if (thingTypeTemplate == null) {
            throw new UserException("Unable to get Thing Type Template with name " + name + ". Name not found.");
        }
        return thingTypeTemplate;
    }

    public Group getGroupByCode (String code)
    {
        logger.info("getting group by code " + code);
        Group group = groupByCodeCache.get(code);
        if (group == null)
        {
            group = GroupService.getGroupDAO().selectBy("code",code);
            if (group != null) {
                groupByCodeCache.put(code, group);
            }
            else {
                throw new UserException("Unable to get group with code " + code + ". Code not found.");
            }
        }
        return group;
    }

    public String getHierarchyNameGroupById (Long id)
    {
        logger.info("getting Hierarchy Name Group by id " + id);
        String hierarchyNameGroup = hierarchyNameGroupByIdCache.get(id);
        if (hierarchyNameGroup == null)
        {
            Group group = GroupService.getGroupDAO().selectById(id);
            if (group != null){
                hierarchyNameGroup = group.getHierarchyName();
                if (hierarchyNameGroup != null && !hierarchyNameGroup.isEmpty())
                    hierarchyNameGroupByIdCache.put(id, hierarchyNameGroup);
                else
                    throw new UserException("Unable to get group with id " + id + ". ID not found.");
            }
            else {
                throw new UserException("Unable to get group with id " + id + ". ID not found.");
            }
        }
        return hierarchyNameGroup;
    }

    public Zone getZoneByCode(String code)
    {
        logger.info("getting zone by code: " + code);
        Zone zone = zoneCache.get(code);
        if (zone == null)
        {
            try
            {
                List<Zone> zones = ZoneService.getInstance().getZonesByCode(code);
                if (null != zones && !zones.isEmpty()) {
                    if (zones.size() > 1)
                        throw new NonUniqueResultException();
                    zone = zones.get(0);
                    zoneCache.put(code, zone);
                } else {
                    throw new UserException("Unable to get zone with code " + code + ". Zone not found.");
                }
            }
            catch (NonUniqueResultException e)
            {
                throw new UserException("Unable to get zone with code " + code + ". Duplicate code.", e);
            }
        }
        return zone;
    }

    public Shift getShiftByCode(String code)
    {
        logger.info("getting shift by code " + code);
        Shift shift = shiftCache.get(code);
        if (shift == null)
        {
            try
            {
                shift = ShiftService.getInstance().getByCode(code);
                if (shift != null) {
                    shiftCache.put(code, shift);
                }
                else {
                    throw new UserException("Unable to get shift with code " + code + ". Code not found.");
                }
            }
            catch (NonUniqueResultException e)
            {
                throw new UserException(
                        "Unable to get shift with code " + code + ". Duplicate code.", e);
            }
        }
        return shift;
    }

    public LogicalReader getLogicalReaderByCode(String code)
    {
        logger.info("getting logical reader by code " + code);
        LogicalReader logicalReader = logicalReaderCache.get(code);
        if (logicalReader == null)
        {
            try
            {
                logicalReader = LogicalReaderService.getInstance().getByCode( code );
                if (logicalReader != null) {
                    logicalReaderCache.put(code, logicalReader);
                }
                else {
                    throw new UserException("Unable to get logical reader with code " + code + ". Code not found.");
                }
            }
            catch (NonUniqueResultException e)
            {
                throw new UserException("Unable to get logical reader with code " + code + ". Duplicate code.", e);
            }
        }
        return logicalReader;
    }

    public Thing getThingBySerialAndThingType(String serial, Long thingTypeId)
    {
        logger.info("getting thing by serial " + serial);
        Thing thing = thingCache.get(serial);
        if (thing == null)
        {
            try
            {
                if (null == thingTypeId)
                    throw new UserException("Type Thing Type Code cannot be null");
                thing = ThingService.getInstance().getBySerialAndThingTypeCode(serial, thingTypeId);
                if (thing != null) {
                    thingCache.put(serial, thing);
                }
                else {
                    throw new UserException("Unable to get thing with serial " + serial + ". Serial not found.");
                }
            }
            catch (NonUniqueResultException e)
            {
                throw new UserException("Unable to get thing with serial " + serial + ". Duplicate serial.", e);
            }
        }
        return thing;
    }

}

