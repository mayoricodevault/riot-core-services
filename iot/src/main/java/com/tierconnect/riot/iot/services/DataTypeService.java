package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.appcore.cache.CacheBoundary;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.utils.DateHelper;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : rchirinos
 * @date : 5/6/16 9:49 AM
 * @version:
 */


public class DataTypeService extends DataTypeServiceBase {
    static Logger logger = Logger.getLogger(DataTypeService.class);
    private static final String DATA_TYPE_CACHE_NAME_FOR_THING_TYPE = "dataTypeCacheForThingType";

    /**
     * This method returns an object which is casted to a defined DataType based on DataType and an
     * obj
     *
     * @param dataType
     * @param obj       Object to be evaluated
     * @param group     Optional
     * @param thingType Optional
     * @return
     */
    public static Object getValueDataType(
            DataType dataType,
            Object obj,
            Group group,
            ThingType thingType) {
        Object response = null;
        if (obj != null) {
            try {
                //Standard Data Types
                if (Class.forName(dataType.getClazz()) == Boolean.class) {
                    Boolean data = Boolean.parseBoolean(obj.toString());
                    response = data;
                } else if (Class.forName(dataType.getClazz()) == BigDecimal.class) {
                    obj = obj.toString().trim().equals("") ? "0" : obj;
                    Double data = new Double(obj.toString()); //Double for MongoDB
                    response = data;

                } else if (Class.forName(dataType.getClazz()) == Long.class) {
                    obj = obj.toString().trim().equals("") ? "0" : obj;
                    Long data = (new BigDecimal(obj.toString())).longValue();
                    response = data;
                } else if (Class.forName(dataType.getClazz()) == Date.class) {
                    Date data = null;
                    if (obj.toString().matches("-?\\d+(\\.\\d+)?")) {
                        data = new Date(Long.parseLong(obj.toString()));
                    } else {
                        data = DateHelper.getDateAndDetermineFormat(obj.toString());
                    }
                    response = data;
                }
                //Native Objects
                else if (Class.forName(dataType.getClazz()) == LogicalReader.class) {
                    LogicalReader data = null;
                    if (group != null) {
                        data = LogicalReaderService.getInstance().getByCodeAndGroup(
                                obj.toString(),
                                group.getHierarchyName());
                    } else {
                        data = LogicalReaderService.getInstance().getByCode(obj.toString());
                    }
                    response = data;
                } else if (Class.forName(dataType.getClazz()) == Zone.class) {
                    Zone data = null;
                    if (group != null) {
                        data = ZoneService.getInstance().getByCodeAndGroup(
                                obj.toString(),
                                group);
                    } else {
                        data = ZoneService.getInstance().getByCode(obj.toString());
                    }
                    response = data;
                } else if (Class.forName(dataType.getClazz()) == Shift.class) {
                    Shift data = null;
                    if (group != null) {
                        data = ShiftService.getInstance().getByCodeAndGroup(
                                obj.toString(),
                                group.getHierarchyName());
                    } else {
                        data = ShiftService.getInstance().getByCode(obj.toString());
                    }
                    response = data;
                } else if (Class.forName(dataType.getClazz()) == Group.class) {
                    Group data = null;
                    if (group != null) {
                        data = GroupService.getInstance().getByCodeAndGroup(
                                obj.toString(),
                                group.getHierarchyName());
                    } else {
                        data = GroupService.getInstance().getByCode(obj.toString());
                    }
                    response = data;
                }
                //Things
                else if (Class.forName(dataType.getClazz()) == Group.class) {
                    Thing data = ThingService.getInstance().getBySerialNumber(obj.toString(), thingType);
                    response = data;
                } else {
                    response = false;
                }
            } catch (ClassNotFoundException e) {
                response = null;
            } catch (NonUniqueResultException e) {
                response = null;
            } catch (Exception e) {
                response = null;
            }
        }
        return response;
    }

    /**
     * This method evaluates if an object can be casted to a defined DataType
     *
     * @param dataType
     * @param obj       Object to be evaluated
     * @param group     Optional
     * @param thingType Optional
     * @return
     */
    public static boolean isValueDataType(
            DataType dataType,
            Object obj,
            Group group,
            ThingType thingType) {
        boolean response = false;
        if (obj != null) {
            try {
                //Standard Data Types
                if (Class.forName(dataType.getClazz()) == Boolean.class) {
                    Boolean data = Boolean.parseBoolean(dataType.toString());
                    response = true;
                } else if (Class.forName(dataType.getClazz()) == BigDecimal.class) {
                    obj = obj.toString().trim().equals("") ? "0" : obj;
                    BigDecimal data = new BigDecimal(obj.toString());
                    response = true;

                } else if (Class.forName(dataType.getClazz()) == Long.class) {
                    obj = obj.toString().trim().equals("") ? "0" : obj;
                    Long data = (new BigDecimal(obj.toString())).longValue();
                    response = true;
                } else if (Class.forName(dataType.getClazz()) == Date.class) {
                    Date data = null;
                    if (obj.toString().matches("-?\\d+(\\.\\d+)?")) {
                        data = new Date(Long.parseLong(obj.toString()));
                    } else {
                        data = DateHelper.getDateAndDetermineFormat(obj.toString());
                    }
                    response = true;
                }
                //Native Objects
                else if (Class.forName(dataType.getClazz()) == LogicalReader.class) {
                    LogicalReader data = LogicalReaderService.getInstance().getByCodeAndGroup(
                            obj.toString()
                            , group.getHierarchyName());
                    response = data != null ? true : false;
                } else if (Class.forName(dataType.getClazz()) == Zone.class) {
                    Zone data = ZoneService.getInstance().getByCodeAndGroup(
                            obj.toString(),
                            group);
                    response = data != null ? true : false;
                } else if (Class.forName(dataType.getClazz()) == Shift.class) {
                    Shift data = ShiftService.getInstance().getByCodeAndGroup(
                            obj.toString()
                            , group.getHierarchyName());
                    response = data != null ? true : false;
                } else if (Class.forName(dataType.getClazz()) == Group.class) {
                    Group data = GroupService.getInstance().getByCodeAndGroup(
                            obj.toString()
                            , group.getHierarchyName());
                    response = data != null ? true : false;
                }
                //Things
                else if (Class.forName(dataType.getClazz()) == Group.class) {
                    Thing data = ThingService.getInstance().getBySerialNumber(obj.toString(), thingType);
                    response = data != null ? true : false;
                } else {
                    response = false;
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                response = false;
            }
        }
        return response;
    }

    public DataType get(ThingTypeField.Type type) {
        return get(type.value);
    }


    public DataType getByCode(String code) throws NonUniqueResultException {
        try {
            return getDataTypeDAO().selectBy("code", code);
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }

    public List<DataType> getAllDataTypes() {
        return getDataTypeDAO().selectAll();
    }

    public void loadAllInCache() {
        boolean cacheCreated = true;
        boolean thingTypeCacheExists = CacheBoundary.getInstance().cacheExists(DATA_TYPE_CACHE_NAME_FOR_THING_TYPE);
        if (!thingTypeCacheExists) {
            cacheCreated = CacheBoundary.getInstance().createCache(DATA_TYPE_CACHE_NAME_FOR_THING_TYPE, String.class, List.class);
        }
        if (cacheCreated) {
            List<DataType> allDataTypes = getAllDataTypes();
            allDataTypes.forEach(dt -> putAllThingTypeFieldsInCache(dt.getId(), null));
        } else {
            logger.error("Could not create cache");
        }
    }

    private void putAllThingTypeFieldsInCache(Long dataTypeId, Long thingTypeId) {
        List<ThingTypeField> thingTypeFields;
        if (thingTypeId != null) {
            thingTypeFields = ThingTypeFieldService.getInstance()
                    .getThingTypeFieldsByThingTypeAndDataType(dataTypeId, thingTypeId);
        } else {
            thingTypeFields = ThingTypeFieldService.getInstance()
                    .getAllThingTypeFieldsByDataType(dataTypeId);
        }
        fillCacheWithThingTypeFields(thingTypeFields);
    }

    public List<ThingTypeField> getThingTypeFieldsFromCache(String thingTypeCode, Long dataTypeId) {
        List<ThingTypeField> thingTypeFields = CacheBoundary.getInstance().get(DATA_TYPE_CACHE_NAME_FOR_THING_TYPE,
                createKey(thingTypeCode, dataTypeId), String.class, List.class);
        return (thingTypeFields == null && !containsThingTypeFieldKey(thingTypeCode, dataTypeId))?
                lazyLoadThingTypeField(thingTypeCode, dataTypeId) : thingTypeFields;
    }

    public void removeThingTypeFieldsFromCache(String thingTypeCode) {
        List<DataType> allDataTypes = getAllDataTypes();
        allDataTypes.forEach(dt ->
                CacheBoundary.getInstance().remove(DATA_TYPE_CACHE_NAME_FOR_THING_TYPE, createKey(thingTypeCode, dt.getId()),
                        String.class, List.class)
        );
    }

    public void replaceThingTypeFields(Set<ThingTypeField> newThingTypeFields) {
        fillCacheWithThingTypeFields(new LinkedList<>(newThingTypeFields));
    }

    private String createKey(String thingTypeCode, Long dataTypeId) {
        return thingTypeCode + "-" + dataTypeId;
    }

    private void fillCacheWithThingTypeFields(List<ThingTypeField> thingTypeFields) {
        Map<String, List<ThingTypeField>> groupedTTFs = groupThingTypeFieldByThingTypeAndDataType(thingTypeFields);
        groupedTTFs.entrySet().forEach(g ->
                CacheBoundary.getInstance().put(DATA_TYPE_CACHE_NAME_FOR_THING_TYPE,
                        g.getKey(), g.getValue(), String.class, List.class
                )
        );
    }

    private Map<String, List<ThingTypeField>> groupThingTypeFieldByThingTypeAndDataType(List<ThingTypeField> thingTypeFields) {
        Map<String, List<ThingTypeField>> groupedTTFs = new HashMap<>();
        for (ThingTypeField ttf : thingTypeFields) {
            initializeAttributes(ttf);
            String key = createKey(ttf.getThingType().getThingTypeCode(), ttf.getDataType().getId());
            List<ThingTypeField> ttfList = groupedTTFs.get(key);
            if (ttfList == null) {
                ttfList = new LinkedList<>();
            }
            ttfList.add(ttf);
            groupedTTFs.put(key, ttfList);
        }
        return groupedTTFs;
    }

    private void initializeAttributes(ThingTypeField ttf) {
        Hibernate.initialize(ttf.getThingType());
        Hibernate.initialize(ttf.getDataType());
    }

    private List<ThingTypeField> lazyLoadThingTypeField(String thingTypeCode, Long dataTypeId){
        ThingType thingType = ThingTypeService.getInstance().getByThingTypeCode(thingTypeCode);
        if(thingType != null) {
            putAllThingTypeFieldsInCache(dataTypeId, thingType.getId());
        }
        return CacheBoundary.getInstance().get(DATA_TYPE_CACHE_NAME_FOR_THING_TYPE,
                createKey(thingTypeCode, dataTypeId), String.class, List.class);
    }

    private boolean containsThingTypeFieldKey(String thingTypeCode, Long dataTypeId) {
        return CacheBoundary.getInstance()
                .containsKey(DATA_TYPE_CACHE_NAME_FOR_THING_TYPE, createKey(thingTypeCode, dataTypeId),
                        String.class, List.class);
    }
}
