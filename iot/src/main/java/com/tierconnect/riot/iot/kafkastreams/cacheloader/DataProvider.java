package com.tierconnect.riot.iot.kafkastreams.cacheloader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import com.mongodb.DBCursor;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.EdgeboxRule;
import com.tierconnect.riot.iot.entities.LocalMap;
import com.tierconnect.riot.iot.entities.LogicalReader;
import com.tierconnect.riot.iot.entities.Shift;
import com.tierconnect.riot.iot.entities.ShiftZone;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.entities.ZoneType;

/**
 * DataProvider class.
 *
 * @author jantezana
 * @version 2017/01/26
 */
public class DataProvider {
    private MysqlDataProvider mysqlDataProvider;
    private MongoDataProvider mongoDataProvider;
    
    /**
     * Builds an instance of {@link DataProvider}
     *
     * @param jdbcConfig  the jdbc configuration
     * @param mongoConfig the mongo configuration
     * @throws Exception the exception
     */
    public DataProvider(JdbcConfig jdbcConfig,
                        MongoConfig mongoConfig)
    throws Exception {
        Preconditions.checkNotNull(jdbcConfig, "The jdb configuration is null");
        Preconditions.checkNotNull(mongoConfig, "The mongo configuration is null");
        this.mysqlDataProvider = new MysqlDataProvider();
        this.mongoDataProvider = new MongoDataProvider(mongoConfig/*, dataTypesByThingField*/);
    }

    /**
     * Gets the list of thing types.
     * @return
     */
    public List<ThingType> getThingTypes() {
        return this.mysqlDataProvider.getThingTypes(null);
    }

    /**
     * Gets the list of thing types.
     * @param tenant
     * @return
     */
    public List<ThingType> getThingTypes(Group tenant) {
        return this.mysqlDataProvider.getThingTypes(tenant);
    }

    /**
     * Gets the list of things.
     *
     * @return the list of things
     * @throws Exception the exception
     */
    public DBCursor getThings(Long[] groupIds)
    throws Exception {
        return this.mongoDataProvider.getThings(groupIds);
    }

    /**
     * Gets the list of zones. 
     * @param tenant
     * @return
     * @throws Exception
     */
    public List<Zone> getZones(Group tenant)
    throws Exception {
        return this.mysqlDataProvider.getZones(tenant);
    }

    /**
     * Gets the list of zone types.
     *
     * @return the list of zone types
     * @throws Exception the exception
     */
    public List<ZoneType> getZoneTypes()
    throws Exception {
        return this.mysqlDataProvider.getZoneTypes();
    }

    /**
     * Gets the map of zone properties.
     *
     * @return the map of zone properties
     * @throws Exception the exception
     */
    public Map<Long, Map<String, Object>> getZoneProperties()
    throws Exception {
        return this.mysqlDataProvider.getZoneProperties();
    }

    /**
     * Gets the list of groups.
     *
     * @return the list of groups
     * @throws Exception the exception
     */
    public List<Group> getGroups()
    throws Exception {
        return this.mysqlDataProvider.getGroups();
    }

    /**
     * Gets the list of group types.
     *
     * @return the list of group types
     * @throws Exception the exception
     */
    public List<GroupType> getGroupTypes()
    throws Exception {
        return this.mysqlDataProvider.getGroupTypes();
    }

    /**
     * Gets the list of shifts.
     * @param tenant
     * @return
     * @throws Exception 
     */
    public List<Shift> getShifts(Group tenant) throws Exception {
        return this.mysqlDataProvider.getShifts(tenant);
    }

    /**
     * Gets the list of shift zones.
     *
     * @return the list of shift zones
     * @throws Exception
     */
    public List<ShiftZone> getShiftZones(Group tenant)
    throws Exception {
        return this.mysqlDataProvider.getShiftZones(tenant);
    }

    /**
     * Gets the list of logical readers.
     * @param tenant
     * @return
     * @throws Exception
     */
    public List<LogicalReader> getLogicalReaders(Group tenant)
    throws Exception {
        return this.mysqlDataProvider.getLogicalReaders(tenant);
    }

    /**
     * Gets the list of local maps.
     *
     * @return the list of local maps
     * @throws Exception the exception
     */
    public List<LocalMap> getLocalMaps()
    throws Exception {
        return this.mysqlDataProvider.getLocalMaps();
    }

    /**
     * Gets the list of edge boxes.
     *
     * @return the list of edge boxes
     * @throws Exception the exception
     */
    public List<Edgebox> getEdgeBoxes()
    throws Exception {
        return this.mysqlDataProvider.getEdgeBoxes(null);
    }

    /**
     * Gets the list of edge boxes.
     *
     * @return the list of edge boxes
     * @throws Exception the exception
     */
    public List<Edgebox> getEdgeBoxes(Group tenant)
    throws Exception {
        return this.mysqlDataProvider.getEdgeBoxes(tenant);
    }

    /**
     * Gets the list of edge box rules.
     *
     * @return the list of edge box rules
     * @throws Exception the exception
     */
    public List<EdgeboxRule> getEdgeBoxRules()
            throws Exception {
        return this.mysqlDataProvider.getEdgeBoxRules();
    }

    /**
     * Gets the list of connections.
     *
     * @return the list of connections
     * @throws Exception the exception
     */
    public List<Connection> getConnections()
            throws Exception {
        return this.mysqlDataProvider.getConnections();
    }

	public String getConfiguration(String bridgeCode) throws JsonProcessingException {
            Preconditions.checkNotNull(bridgeCode);
            return mysqlDataProvider.getConfiguration(bridgeCode);
	}

    public List<Group> getTenants() {
        return this.mysqlDataProvider.getTenants();
    }

    public List<Group> getTenantChildren(Group tenant) {
        return this.mysqlDataProvider.getTenantChildren(tenant);
    }
    
    public List<Long> getTenantChildrenIds(Group tenant) {
        List<Group> tenantChildren = getTenantChildren(tenant);
        List<Long> groupIds = new ArrayList<>();
        for (Group group : tenantChildren) {
            groupIds.add(group.getId());
        }
        return groupIds;
    }

}
