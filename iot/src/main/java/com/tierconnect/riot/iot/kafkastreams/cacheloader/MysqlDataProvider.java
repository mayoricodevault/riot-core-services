package com.tierconnect.riot.iot.kafkastreams.cacheloader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.BooleanUtils;
import org.hibernate.*;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

/**
 * MysqlDataProvider class.
 *
 * @author jantezana
 * @version 2017/01/11
 */
public class MysqlDataProvider {
    
    private static final long TENAN_GROUP_TYPE_ID = 2l;


    /**
     * Returns a list of all things types.
     * @return
     */
    public List<ThingType> getThingTypes() {

        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(ThingType.class);
        c.setReadOnly(true);

        List<ThingType> thingTypes = (List<ThingType>) c.list();

        // Initialize lazy objects.
        if (thingTypes != null) {
            for (ThingType thingType : thingTypes) {
                initializeLazyObjects(thingType);
            }
        }
        return thingTypes;
    }

    /**
     * Gets the list of things.
     * @param tenant
     * @return
     */
    public List<ThingType> getThingTypes(Group tenant) {

        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(ThingType.class);
        if(tenant!=null) {
            c.createAlias("group", "g");
            c.add(Restrictions.eq("g.parentLevel2", tenant));
        }
        c.setReadOnly(true);

        List<ThingType> thingTypes = (List<ThingType>) c.list();

        // Initialize lazy objects.
        if (thingTypes != null) {
            for (ThingType thingType : thingTypes) {
                initializeLazyObjects(thingType);
            }
        }
        return thingTypes;
    }

    /**
     * Initialize lazy objects of thing type.
     *
     * @param thingType the thing type
     */
    private void initializeLazyObjects(ThingType thingType) {
        Hibernate.initialize(thingType.getGroup());
        Hibernate.initialize(thingType.getParentTypeMaps());
        Hibernate.initialize(thingType.getChildrenTypeMaps());
        Hibernate.initialize(thingType.getThingTypeFields());
        Hibernate.initialize(thingType.getDefaultOwnerGroupType());
        Set<ThingTypeField> thingTypeFields = thingType.getThingTypeFields();

        for (ThingTypeField thingTypeField : thingTypeFields) {
            initializeLazyObjects(thingTypeField);
        }

        Hibernate.initialize(thingType.getThingTypeTemplate());
    }

    /**
     * Initialize lazy object of thing type field.
     *
     * @param thingTypeField the thing type field
     */
    private void initializeLazyObjects(ThingTypeField thingTypeField) {
        Hibernate.initialize(thingTypeField.getThingType());
        Hibernate.initialize(thingTypeField.getDataType());
    }

    /**
     * Gets the list of things
     *
     * @return the list of things
     * @throws Exception the exception
     */
    public List<Thing> getThings()
    throws Exception {
        return null;
    }

    /**
     * Get all zones
     * @return
     * @throws Exception
     */
    public List<Zone> getAllZones()
    throws Exception {
        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(Zone.class);
        c.setReadOnly(true);

        List<Zone> zones = (List<Zone>) c.list();

        if (zones != null) {
            for (Zone zone : zones) {
                initializeLazyObjects(zone);
            }
        }
        return zones;
    }
    
    /**
     * Gets the list of zones.
     * @param tenant
     * @return
     * @throws Exception
     */
    public List<Zone> getZones(Group tenant)
    throws Exception {
        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(Zone.class);
        if(tenant!=null) {
            c.createAlias("group", "g");
            c.add(Restrictions.eq("g.parentLevel2", tenant));
        }
        c.setReadOnly(true);

        List<Zone> zones = (List<Zone>) c.list();

        if (zones != null) {
            for (Zone zone : zones) {
                initializeLazyObjects(zone);
            }
        }
        return zones;
    }

    /**
     * Gets the list of zone types.
     *
     * @return the list of zone types
     * @throws Exception the exception
     */
    public List<ZoneType> getZoneTypes()
    throws Exception {
        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(ZoneType.class);
        c.setReadOnly(true);
        return (List<ZoneType>) c.list();
    }

    /**
     * Gets the map of zone properties.
     *
     * @return the map of zone properties
     * @throws Exception the exception
     */
    public Map<Long, Map<String, Object>> getZoneProperties()
    throws Exception {
        Map<Long, Map<String, Object>> zonePropertiesMap = new HashMap<>();
        List<Zone> zones = this.getAllZones();
        for (Zone zone : zones) {
            Map<String, Object> properties = new HashMap<>();
            List<ZoneProperty> zoneProperties = this.getZoneProperties(zone.getZoneType().getId());
            for (ZoneProperty zoneProperty : zoneProperties) {

                String propName = zoneProperty.getName();
                Object propValue = this.getZonePropertyValue(zone.getId(), zoneProperty.getId());
                if (propValue != null) {
                    if (BooleanUtils.toBooleanObject(propValue.toString()) != null) {
                        properties.put(propName, Boolean.valueOf(propValue.toString()));
                    } else {
                        properties.put(propName, propValue);
                    }
                } else {
                    properties.put(propName, propValue);
                }
            }
            zonePropertiesMap.put(zone.getId(), properties);
        }

        return zonePropertiesMap;
    }

    /**
     * Gets the zone properties by zone type id.
     *
     * @param zoneTypeId the zone type ID
     * @return the list of zone properties
     */
    public List<ZoneProperty> getZoneProperties(long zoneTypeId) {
        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(ZoneProperty.class);
        c.add(Restrictions.eq("zoneType.id", zoneTypeId));
        c.setReadOnly(true);

        List<ZoneProperty> zoneProperties = (List<ZoneProperty>) c.list();

        return zoneProperties;
    }

    /**
     * Gets the zone property value by zone id and zone property id.
     *
     * @param zoneId         the zone ID
     * @param zonePropertyId the zone property ID
     * @return the property value
     */
    public Object getZonePropertyValue(long zoneId,
                                       long zonePropertyId) {
        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(ZonePropertyValue.class);
        c.setProjection(Projections.projectionList().add(Projections.property("value"), "value"));
        c.add(Restrictions.eq("zoneId", zoneId));
        c.add(Restrictions.eq("zonePropertyId", zonePropertyId));
        c.setReadOnly(true);
        Object result = c.uniqueResult();

        return result;
    }

    /**
     * Gets the list of groups.
     *
     * @return the list of groups
     * @throws Exception the exception
     */
    public List<Group> getGroups()
    throws Exception {
        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(Group.class);
        c.setReadOnly(true);

        List<Group> groups = (List<Group>) c.list();

        return groups;
    }

    /**
     * Gets the list of group types.
     *
     * @return the list of group types
     * @throws Exception the exception
     */
    public List<GroupType> getGroupTypes()
    throws Exception {
        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(GroupType.class);
        c.setReadOnly(true);

        List<GroupType> groupTypes = (List<GroupType>) c.list();

        return groupTypes;
    }

    /**
     * Gets the list of shifts.
     *
     * @return the list of shifts
     * @throws Exception the exception
     */
    public List<Shift> getShifts(Group tenant)
    throws Exception {
        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(Shift.class);
        if(tenant!=null) {
            c.createAlias("group", "g");
            c.add(Restrictions.eq("g.parentLevel2", tenant));
        }
        c.setReadOnly(true);
        return (List<Shift>) c.list();
    }

    /**
     * Gets the list of logical readers.
     * @param tenant
     * @return
     * @throws Exception
     */
    public List<LogicalReader> getLogicalReaders(Group tenant)
    throws Exception {
        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(LogicalReader.class);
        if(tenant!=null) {
            c.createAlias("group", "g");
            c.add(Restrictions.eq("g.parentLevel2", tenant));    
        }
        c.setReadOnly(true);
        return (List<LogicalReader>) c.list();
    }

    /**
     * Gets the list of local maps
     *
     * @return the list of local maps
     * @throws Exception the exception
     */
    public List<LocalMap> getLocalMaps()
    throws Exception {
        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(LocalMap.class);
        c.setReadOnly(true);

        List<LocalMap> localMaps = (List<LocalMap>) c.list();

        return localMaps;
    }

    /**
     * Gets the list of edge boxes.
     *
     * @return the list of edge boxes
     * @throws Exception the exception
     */
    public List<Edgebox> getEdgeBoxes(Group tenant)
    throws Exception {
        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(Edgebox.class);
        if(tenant!=null) {
            c.createAlias("group", "g");
            c.add(Restrictions.eq("g.parentLevel2", tenant));    
        }
        
        c.setReadOnly(true);

        List<Edgebox> edgeboxes = (List<Edgebox>) c.list();

        return edgeboxes;
    }


    /**
     * Gets the list of edge box rules.
     *
     * @return the list of edge box rules
     * @throws Exception the exception
     */
    public List<EdgeboxRule> getEdgeBoxRules()
            throws Exception {
        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(EdgeboxRule.class);
        c.setReadOnly(true);

        List<EdgeboxRule> edgeboxRules = (List<EdgeboxRule>) c.list();

        return edgeboxRules;
    }

    /**
     * Gets the list of connections.
     *
     * @return the list of connections
     * @throws Exception the exception
     */
    public List<Connection> getConnections()
            throws Exception {
        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(Connection.class);
        c.setReadOnly(true);

        List<Connection> connections = (List<Connection>) c.list();

        return connections;
    }

    /**
     * Gets the list of shift zones.
     * @param tenant
     * @return
     * @throws Exception
     */
    public List<ShiftZone> getShiftZones(Group tenant)
    throws Exception {
        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(ShiftZone.class);
        if(tenant!=null) {
            c.createAlias("group", "g");
            c.add(Restrictions.eq("g.parentLevel2", tenant));    
        }
        c.setReadOnly(true);

        List<ShiftZone> shiftZones = (List<ShiftZone>) c.list();

        // Initialize lazy objects.
        if (shiftZones != null) {
            for (ShiftZone shiftZone : shiftZones) {
                initializeLazyObjects(shiftZone);
            }
        }

        return shiftZones;
    }
    
    /**
     * Return a list of all tenant groups
     * @return
     */
    public List<Group> getTenants() 
    {
        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(Group.class);
        c.add(Restrictions.eq("groupType", s.get(GroupType.class, TENAN_GROUP_TYPE_ID)));
        return c.list();
    }

    /**
     * Return tenant's children 
     * @param tenant
     * @return
     */
    public List<Group> getTenantChildren(Group tenant) {
        Session s = HibernateSessionFactory.getInstance().getCurrentSession();
        Criteria c = s.createCriteria(Group.class);
        c.add(Restrictions.eq("parentLevel2", tenant));
        return (List<Group>) c.list();
    }

    public List<Long> getTenantChildrenIds(Group tenant) {
        List<Group> tenantChildren = getTenantChildren(tenant);
        List<Long> groupIds = new ArrayList<>();
        for (Group group : tenantChildren) {
            groupIds.add(group.getId());
        }
        return groupIds;
    }
    
    /**
     * Initialize lazy objects of a shift zone.
     *
     * @param shiftZone the shift zone
     */
    private void initializeLazyObjects(ShiftZone shiftZone) {
        Shift shift = shiftZone.getShift();
        Hibernate.initialize(shift);
        if (shift != null) {
            Hibernate.initialize(shift.getGroup());
        }
        Hibernate.initialize(shiftZone.getZone());
        Hibernate.initialize(shiftZone.getGroup());
    }

    /**
     * initialize lazy objects of a zone.
     *
     * @param zone the zone
     */
    private void initializeLazyObjects(Zone zone) {
        Hibernate.initialize(zone.getZonePoints());
        Hibernate.initialize(zone.getZoneGroup());
        Hibernate.initialize(zone.getZoneType());
        Hibernate.initialize(zone.getLocalMap());
    }

    public String getConfiguration(String bridgeCode) throws JsonProcessingException {
        String mapAsJson = "";
        Map<String,Object> map = EdgeboxService.getInstance().getConfiguration(bridgeCode);
        mapAsJson = new ObjectMapper().writeValueAsString(map);
        return mapAsJson;
    }
}
