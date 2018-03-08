package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.entities.IThingField;
import com.tierconnect.riot.commons.services.LocationService;
import com.tierconnect.riot.commons.utils.MathUtils;
import com.tierconnect.riot.iot.entities.LocalMap;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.entities.ZonePoint;
import com.tierconnect.riot.iot.utils.ZonePointComparator;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by cfernandez
 * on 10/7/15.
 */
public class AppLocationService extends LocationService
{
    private static Logger logger = Logger.getLogger(AppLocationService.class);

    private Map<String, ThingTypeField> requiredZoneFields;
    private Zone zone;

    public AppLocationService() {

    }

    public AppLocationService(Map<String, ThingTypeField> requiredZoneFields, Zone zone) {
        this.requiredZoneFields = requiredZoneFields;
        this.zone = zone;
    }

    public Zone getZone() {
        return zone;
    }

    @Override
    protected String getZoneCode(double longitude, double latitude)
    {
        String zoneCode = null;
        ZoneService zoneService = ZoneService.getInstance();
        List<Zone> zones = zoneService.getZones();
        Set<ZonePoint> zonePoints;
        List<ZonePoint> zonePointsList;
        ZonePointComparator zonePointComparator = new ZonePointComparator();
        for (Zone zone : zones)
        {
            logger.debug("evaluating zone=" + zone.getCode());
            zonePoints = zone.getZonePoints();
            zonePointsList = new ArrayList<>(zonePoints);
            Collections.sort(zonePointsList, zonePointComparator);
            List<double[]> points = new ArrayList<>();
            for (ZonePoint zonePoint : zonePointsList)
            {
                points.add(new double[]{zonePoint.getX(), zonePoint.getY()});
                logger.debug("x=" + zonePoint.getX() + ", y=" + zonePoint.getY() + ", index=" + zonePoint.getArrayIndex());
            }
            if(MathUtils.isPointInsidePolygon(points, longitude, latitude))
            {
                zoneCode = zone.getCode();
                break;
            }
        }
        logger.debug("zone found: " + zoneCode);
        return zoneCode;
    }

    @Override
    protected Map<String, Object> getLocalMap(String zoneCode)
    {
        Map<String, Object> localMap = null;

        ZoneService zoneService = ZoneService.getInstance();
        Zone zone = null;
        try{
            zone = zoneService.getByCode(zoneCode);
            if (zone.getLocalMap() != null){
                localMap = zone.getLocalMap().publicMap();
            }
        }
        catch (Exception e) {
            logger.error(e);
        }
        return localMap;
    }

    @Override
    protected Map<String, Object> getLocalMap(String zoneCode, long groupId)
    {
        Map<String, Object> localMap = null;

        ZoneService zoneService = ZoneService.getInstance();
        Zone zone;
        try{
            if(getZone() == null) {
                Group group = GroupService.getInstance().get(groupId);
                zone = zoneService.getByCodeAndGroup(zoneCode, group.getHierarchyName());
            } else {
                zone = getZone();
            }
            if (zone.getLocalMap() != null){
                localMap = zone.getLocalMap().publicMap();
            }
        }
        catch (Exception e) {
            logger.error(e);
        }
        return localMap;
    }

    @Override
    protected List<double[]> getZonePoints(String zoneCode)
    {
        ZoneService zoneService = ZoneService.getInstance();
        Zone zone = null;
        try{
            zone = zoneService.getByCode(zoneCode);
        }
        catch (Exception e) {
            logger.error(e);
        }

        Set<ZonePoint> zonePoints = zone.getZonePoints();
        List<double[]> points = new ArrayList<>();
        for (ZonePoint zonePoint : zonePoints){
            points.add(new double[]{zonePoint.getX(), zonePoint.getY()});
        }

        return points;
    }

    @Override
    protected List<double[]> getZonePoints(String zoneCode, long groupId)
    {
        ZoneService zoneService = ZoneService.getInstance();
        Zone zone = null;
        try{
            if(getZone() == null) {
                Group group = GroupService.getInstance().get(groupId);
                zone = zoneService.getByCodeAndGroup(zoneCode, group.getHierarchyName());
            } else {
                zone = getZone();
            }
        }
        catch (Exception e) {
            logger.error(e);
        }

        Set<ZonePoint> zonePoints = zone.getZonePoints();
        List<double[]> points = new ArrayList<>();
        for (ZonePoint zonePoint : zonePoints){
            points.add(new double[]{zonePoint.getX(), zonePoint.getY()});
        }

        return points;
    }

    @Override
    protected Map<String, Object> getLocalMap(long groupId)
    {
        LocalMapService localMapService = LocalMapService.getInstance();
        List<LocalMap> maps = localMapService.selectAllByGroupId(groupId);

        LocalMap localMap = maps.get(0); // taking the first map assigned to the group

        return localMap.publicMap();
    }

    @Override
    protected void putField(List<IThingField> fields, String thingTypeCode, String dataTypeCode, String value, Date time)
    {
        ThingTypeField thingTypeField = getThingTypeFieldFromCache(dataTypeCode);
        if(thingTypeField == null){
            ThingTypeFieldService thingTypeFieldService = ThingTypeFieldServiceBase.getInstance();
            thingTypeField = thingTypeFieldService.getByThingTypeCodeAndDataTypeCode(thingTypeCode, dataTypeCode);
        }
        com.tierconnect.riot.iot.entities.ThingField field = new com.tierconnect.riot.iot.entities.ThingField(value, thingTypeField, time.getTime());
        fields.add(field);
    }

    @Override
    protected void putField(List<IThingField> fields, String thingTypeCode, String dataTypeCode, String fieldName, String value, Date time)
    {
        ThingTypeField thingTypeField = getThingTypeFieldFromCache(fieldName);// it is a special case for fields lastDetectTime and lastLocateTime.
        if(thingTypeField == null){
            ThingTypeFieldService ttfService = ThingTypeFieldServiceBase.getInstance();
            List<ThingTypeField> thingTypeFields = ttfService.getThingTypeFieldByNameAndTypeCode(fieldName, thingTypeCode);
            // it is a special case for fields lastDetectTime and lastLocateTime.
            thingTypeField = thingTypeFields.get(0);
        }

        com.tierconnect.riot.iot.entities.ThingField field = new com.tierconnect.riot.iot.entities.ThingField(value, thingTypeField, time.getTime());
        fields.add(field);
    }

    private ThingTypeField getThingTypeFieldFromCache(String dataTypeCode){
        ThingTypeField thingTypeField = null;
        if(existDataTypeInCache(dataTypeCode)) {
            thingTypeField = requiredZoneFields.get(dataTypeCode);
        }
        return thingTypeField;
    }

    private boolean existDataTypeInCache(String dataTypeCode) {
        return requiredZoneFields != null && !requiredZoneFields.isEmpty() && requiredZoneFields.get(dataTypeCode) != null;
    }
}
