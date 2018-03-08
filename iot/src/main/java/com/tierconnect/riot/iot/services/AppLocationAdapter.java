package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.DataType;
import com.tierconnect.riot.commons.UDF;
import com.tierconnect.riot.commons.entities.IThingField;
import com.tierconnect.riot.commons.services.LocationService;
import com.tierconnect.riot.commons.utils.CoordinateUtils;
import com.tierconnect.riot.commons.utils.MathUtils;
import com.tierconnect.riot.iot.entities.LocalMap;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.entities.ZonePoint;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by cfernandez
 * on 11/10/15.
 */
public class AppLocationAdapter
{
    private static Logger logger = Logger.getLogger(AppLocationAdapter.class);
    private Map<String, Boolean> dataTypeCodes;
    private Map<String, ThingTypeField> requiredZoneFields;
    private Zone zone = null;
    private static final List<Long> ZONE_DATA_TYPE_LIST =
            Collections.unmodifiableList(
                    Arrays.asList(ThingTypeField.Type.TYPE_XYZ.value,
                            ThingTypeField.Type.TYPE_LONLATALT.value, ThingTypeField.Type.TYPE_ZONE.value));
    private static final List<String> ZONE_DATA_NAME_LIST =
            Collections.unmodifiableList(
                    Arrays.asList(DataType.XYZ, DataType.COORDINATES, DataType.ZONE));

    public void processFields(Map<String, Object> udfs, Date transactionDate, long groupId, String thingTypeCode,
                              boolean create) throws UserException
    {
        dataTypeCodes = getDataTypeCodes(udfs, thingTypeCode);
        if(containsLocationFields(udfs, thingTypeCode))
        {
            if (requiredThingTypeFieldsAvailable(thingTypeCode))
            {
                try {
                    if (isZoneChange(udfs, thingTypeCode)) {
                        processZoneChange(udfs, transactionDate, thingTypeCode, create);
                    } else if (isLocationChange(udfs, thingTypeCode)) {
                        processLocationChange(udfs, transactionDate, groupId, thingTypeCode, create);
                    } else if (isLocationXYZChange(udfs, thingTypeCode)) {
                        processLocationXYZChange(udfs, transactionDate, groupId, thingTypeCode, create);
                    } else if (allFieldChanged(udfs, thingTypeCode)) {
                        allFieldsChange(udfs, transactionDate, groupId, thingTypeCode, create);
                    } else {
                        logger.error("The combination of location fields is not supported.");
                        throw new UserException("The combination of location fields that you are trying to edit is not supported, " +
                                "please review it and try again.");
                    }
                } catch (IndexOutOfBoundsException e) {
                    // IndexOutOfBoundsException happen when groupId has not any facilityMap.
                    Group group = GroupService.getInstance().get(groupId);
                    String groupName = (group != null)?group.getName():"null";
                    throw new UserException("Location fields cannot be calculated because the group [" + groupName + "] of the thing has not any facility map.", e);
                }
            }
            else {
                logger.debug("Cannot calculate location fields because the thing fields required are not available.");
            }
        }
    }

    public Map<String, Boolean> getDataTypeCodes(Map<String, Object> udfs, String thingTypeCode)
    {
        DataTypeService dataTypeService = DataTypeService.getInstance();
        Map<String, Boolean> dataTypeCodes = new HashMap<>();
        dataTypeCodes.put(DataType.COORDINATES, Boolean.FALSE);
        dataTypeCodes.put(DataType.ZONE, Boolean.FALSE);
        dataTypeCodes.put(DataType.XYZ, Boolean.FALSE);
        if(udfs != null && !udfs.isEmpty()){
            ZONE_DATA_TYPE_LIST.forEach(zdt -> {
                List<ThingTypeField> thingTypeFields = dataTypeService.getThingTypeFieldsFromCache(thingTypeCode, zdt);
                if(thingTypeFields != null && thingTypeFields.size() == 1
                        && udfs.get(thingTypeFields.get(0).getName()) != null){
                    dataTypeCodes.put(thingTypeFields.get(0).getDataType().getCode(), Boolean.TRUE);
                }
            });
        }
        return dataTypeCodes;
    }


    public boolean containsLocationFields(Map<String, Object> udfs, String thingTypeCode)
    {
        if (udfs == null){
            return false;
        }

        //Map<String, Boolean> dataTypeCodes = getDataTypeCodes(udfs, thingTypeCode);
        //List<String> dataTypeCodes = getDataTypeCodes(udfs, thingTypeCode);
        if (dataTypeCodes.get(DataType.ZONE) || dataTypeCodes.get(DataType.COORDINATES)
                || dataTypeCodes.get(DataType.XYZ)){
            return true;
        }
        return false;
    }

    public boolean isZoneChange(Map<String, Object> udfs, String thingTypeCode)
    {
        //Map<String, Boolean> dataTypeCodes = getDataTypeCodes(udfs, thingTypeCode);
        if (dataTypeCodes.get(DataType.ZONE) && !dataTypeCodes.get(DataType.COORDINATES) &&
                !dataTypeCodes.get(DataType.XYZ)){
            return true;
        }
        return false;
    }

    public boolean isLocationChange(Map<String, Object> udfs, String thingTypeCode)
    {
        //Map<String, Boolean> dataTypeCodes = getDataTypeCodes(udfs, thingTypeCode);
        if (dataTypeCodes.get(DataType.COORDINATES) && !dataTypeCodes.get(DataType.ZONE) &&
                !dataTypeCodes.get(DataType.XYZ)){
            return true;
        }
        return false;
    }

    public boolean isLocationXYZChange(Map<String, Object> udfs, String thingTypeCode)
    {
        //Map<String, Boolean> dataTypeCodes = getDataTypeCodes(udfs, thingTypeCode);
        if (dataTypeCodes.get(DataType.XYZ) && !dataTypeCodes.get(DataType.ZONE) &&
                !dataTypeCodes.get(DataType.COORDINATES)){
            return true;
        }
        return false;
    }

    public boolean isZoneLocationXYZChange(Map<String, Object> udfs, String thingTypeCode)
    {
        //Map<String, Boolean> dataTypeCodes = getDataTypeCodes(udfs, thingTypeCode);
        if (dataTypeCodes.get(DataType.XYZ) && dataTypeCodes.get(DataType.ZONE) &&
                !dataTypeCodes.get(DataType.COORDINATES)){
            return true;
        }
        return false;
    }

    private boolean allFieldChanged(Map<String, Object> udfs, String thingTypeCode)
    {
        //Map<String, Boolean> dataTypeCodes = getDataTypeCodes(udfs, thingTypeCode);
        if (dataTypeCodes.get(DataType.ZONE) && dataTypeCodes.get(DataType.COORDINATES) &&
                dataTypeCodes.get(DataType.XYZ)){
            return true;
        }
        return false;
    }

    public void processZoneChange(Map<String, Object> udfs, Date transactionDate, String thingTypeCode, boolean create)
    {
        ThingTypeField thingTypeField = requiredZoneFields.get(DataType.ZONE);

        // validating thingTypeGroup

        if ((thingTypeField.getThingType() == null) || (thingTypeField.getThingType().getGroup() == null)) {
            throw new UserException("Thing Type Group was not detected for thing type Code [" + thingTypeCode + "].");
        }

        Map object = (Map)udfs.get(thingTypeField.getName());
        Group group = thingTypeField.getThingType().getGroup();

        if (object.get("value") == null && create)
        {
            // this is a special case when it is not necessary to calculate location fields because the thing is created with UDF zone blank
            return;
        }

        if (!isValidZone(object, group)) {
            throw new UserException("Zone='" + object + "' is invalid, please check it and try again.");
        }

        LocationService locationService = new AppLocationService(requiredZoneFields, zone);
        String zoneCode = object.get("value").toString();
        List<IThingField> fields = locationService.performZoneChange(thingTypeCode, zoneCode, transactionDate, group.getId());
        putAll(fields, udfs);
    }

    public void processLocationChange(Map<String, Object> udfs, Date transactionDate, long groupId, String thingTypeCode,
                                      boolean create) throws UserException
    {
        LocationService locationService = new AppLocationService();

        ThingTypeFieldService thingTypeFieldService = ThingTypeFieldServiceBase.getInstance();
        ThingTypeField thingTypeField = thingTypeFieldService.getByThingTypeCodeAndDataTypeCode(thingTypeCode, DataType.COORDINATES);

        Map locationObject = (Map)udfs.get(thingTypeField.getName());

        if(locationObject.get("value") == null ||  StringUtils.isBlank(locationObject.get("value").toString()) && create)
        {
            /*
                this is a special case when it is not necessary to calculate location fields because the thing is created
                with UDF location blank
            */
            return;
        }

        if (!isValidLocation(locationObject, "Location")){
            throw new UserException("Location: '" + locationObject + "' is invalid, the correct format is: " +
                    "Longitude;Latitude;Altitude with number data, please check it and try again.");
        }

        String location = locationObject.get("value").toString();
        List<IThingField> fields = locationService.performLocationChange(thingTypeCode, location, transactionDate, groupId);
        putAll(fields, udfs);
    }

    public void processLocationXYZChange(Map<String, Object> udfs, Date transactionDate, long groupId,
                                         String thingTypeCode, boolean create)
    {
        LocationService locationService = new AppLocationService();

        ThingTypeFieldService thingTypeFieldService = ThingTypeFieldServiceBase.getInstance();
        ThingTypeField thingTypeField = thingTypeFieldService.getByThingTypeCodeAndDataTypeCode(thingTypeCode, DataType.XYZ);

        Map locationXYZObject = (Map)udfs.get(thingTypeField.getName());

        if ((locationXYZObject.get("value") == null || StringUtils.isBlank(locationXYZObject.get("value").toString())) && create)
        {
            /*
                this is a special case when it is not necessary to calculate location fields because the thing is created
                with UDF locationXYZ blank
            */
            return;
        }

        if (!isValidLocation(locationXYZObject, "LocationXYZ")){
            throw new UserException("LocationXYZ: '" + locationXYZObject + "' is invalid, the correct format is: " +
                    "X;Y;Z with number data, please check it and try again.");
        }

        LocalMapService localMapService = LocalMapService.getInstance();
        List<LocalMap> maps = localMapService.selectAllByGroupId(groupId);
        if (maps != null && maps.size() > 1){
            throw new UserException("Location fields cannot be calculated because the group of the thing has more than one facility map.");
        }

        String locationXYZ = locationXYZObject.get("value").toString();
        List<IThingField> fields = locationService.performLocationXYZChange(thingTypeCode, locationXYZ, transactionDate, groupId);
        putAll(fields, udfs);
    }

    private void processZoneLocationXYZChange(Map<String, Object> udfs, Date transactionDate, String thingTypeCode, boolean create)
    {
        ThingTypeFieldService thingTypeFieldService = ThingTypeFieldServiceBase.getInstance();
        ThingTypeField thingTypeFieldZone = thingTypeFieldService.getByThingTypeCodeAndDataTypeCode(thingTypeCode, DataType.ZONE);
        Map zoneObject = (Map)udfs.get(thingTypeFieldZone.getName());

        ThingTypeField thingTypeFieldLocationXYZ = thingTypeFieldService.getByThingTypeCodeAndDataTypeCode(thingTypeCode, DataType.XYZ);
        Map locationXYZObject = (Map)udfs.get(thingTypeFieldLocationXYZ.getName());

        if (zoneObject.get("value") == null
                && (locationXYZObject.get("value") == null || StringUtils.isBlank(locationXYZObject.get("value").toString()))  && create)
        {
            /*
                this is a special case when it is not necessary to calculate location fields because the thing is created
                with UDF zone and locationXYZ blank
            */
            return;
        }

        if (!isValidZone(zoneObject, thingTypeFieldZone.getThingType().getGroup())){
            throw new UserException("Zone='" + zoneObject + "' is invalid, please check it and try again.");
        }

        if (!isValidLocation(locationXYZObject, "LocationXYZ")){
            throw new UserException("LocationXYZ: '" + locationXYZObject + "' is invalid, the correct format is: " +
                    "X;Y;Z with number data, please check it and try again.");
        }

        String zoneCode = zoneObject.get("value").toString();
        String locationXYZ = locationXYZObject.get("value").toString();

        ZoneService zoneService = ZoneService.getInstance();
        Zone zone = null;
        try {
            zone = zoneService.getByCode(zoneCode);
        } catch (Exception e) {
            logger.error(e);
        }

        String[] locXYZ = locationXYZ.split(";");
        double x = Double.parseDouble(locXYZ[0]);
        double y = Double.parseDouble(locXYZ[1]);
        double z = Double.parseDouble(locXYZ[2]);

        Map<String, Object> localMap = zone.getLocalMap().publicMap();

        double lonOrigin = localMap.get("lonOrigin") != null ? (double)localMap.get("lonOrigin") : 0;
        double latOrigin = localMap.get("latOrigin") != null ? (double)localMap.get("latOrigin") : 0;
        double altOrigin = localMap.get("altOrigin") != null ? (double)localMap.get("altOrigin") : 0;
        double declination = localMap.get("declination") != null ? (double)localMap.get("declination") : 0;
        String imageUnit = localMap.get("imageUnit") != null ? (String)localMap.get("imageUnit") : "";

        CoordinateUtils cu = new CoordinateUtils(lonOrigin, latOrigin, altOrigin, declination, imageUnit);
        double[] longLat = cu.xy2lonlat(x, y, z);
        double longitude = longLat[0];
        double latitude = longLat[1];
        double altitude = longLat[2];

        // validating zone
        Zone calculatedZone = findPolygon(longitude, latitude);
        if (calculatedZone == null || !calculatedZone.getCode().equals(zone.getCode()))
        {
            logger.error("The LocationXYZ value specified does not correspond to the zone=" + zoneCode);
            throw new UserException("The LocationXYZ value specified does not correspond to the zone=" + zoneCode);
        }

        String locationXYZString = printLocation(longitude, latitude, altitude);

        Map<String, Object> fields = new HashMap<>();
        putField(fields, thingTypeCode, DataType.COORDINATES, locationXYZString, transactionDate);
        putField(fields, UDF.LAST_DETECT_TIME, String.valueOf(transactionDate.getTime()), transactionDate);
        putField(fields, UDF.LAST_LOCATE_TIME, String.valueOf(transactionDate.getTime()), transactionDate);
        udfs.putAll(fields);
    }

    public void putAll(List<IThingField> fields, Map<String, Object> udfs)
    {
        Map udfObject;
        ThingTypeField thingTypeField;
        for (IThingField field : fields)
        {
            udfObject = new LinkedHashMap();

            udfObject.put("value", field.getValue().toString());
            udfObject.put("time", new Date(field.getTimestamp()));

            thingTypeField = (ThingTypeField) field.getThingTypeField();
            udfs.put(thingTypeField.getName(), udfObject);
        }
    }

    public void allFieldsChange(Map<String, Object> udfs, Date transactionDate, long groupId, String thingTypeCode, boolean create)
    {
        ThingTypeFieldService thingTypeFieldService = ThingTypeFieldServiceBase.getInstance();
        ThingTypeField thingTypeFieldZone = thingTypeFieldService.getByThingTypeCodeAndDataTypeCode(thingTypeCode, DataType.ZONE);
        Map zoneObject = (Map)udfs.get(thingTypeFieldZone.getName());

        ThingTypeField thingTypeFieldLocation = thingTypeFieldService.getByThingTypeCodeAndDataTypeCode(thingTypeCode, DataType.COORDINATES);
        Map locationObject = (Map)udfs.get(thingTypeFieldLocation.getName());

        ThingTypeField thingTypeFieldLocationXYZ = thingTypeFieldService.getByThingTypeCodeAndDataTypeCode(thingTypeCode, DataType.XYZ);
        Map locationXYZObject = (Map)udfs.get(thingTypeFieldLocationXYZ.getName());

        if (zoneObject.get("value") == null && locationObject.get("value") == null && locationXYZObject.get("value") == null && create)
        {
            // this is a special case when it is not necessary to calculate location fields because the thing is created with UDF zone blank
            return;
        }

        if (!isUnknownZone(zoneObject))
        {
            if (!isValidZone(zoneObject, thingTypeFieldZone.getThingType().getGroup())) {
                throw new UserException("Zone='" + zoneObject + "' is invalid, please check it and try again.");
            }
        }

        if (!isValidLocation(locationObject, "Location")){
            throw new UserException("Location: '" + locationObject + "' is invalid, the correct format is: " +
                    "Longitude;Latitude;Altitude with number data, please check it and try again.");
        }

        if (!isValidLocation(locationXYZObject, "LocationXYZ")){
            throw new UserException("LocationXYZ: '" + locationXYZObject + "' is invalid, the correct format is: " +
                    "X;Y;Z with number data, please check it and try again.");
        }

        String zoneCode = zoneObject.get("value").toString();
        String location = locationObject.get("value").toString();
        String locationXYZ = locationXYZObject.get("value").toString();

        ZoneService zoneService = ZoneService.getInstance();
        Zone zone = null;
        try {
            zone = zoneService.getByCode(zoneCode);
        } catch (Exception e) {
            logger.error(e);
        }

        String[] loc = location.split(";");
        double longitude = Double.parseDouble(loc[0]);
        double latitude = Double.parseDouble(loc[1]);

        String zoneName;
        if (zoneCode.equals("unknown")){
            zoneName = "Unknown";
        }else{
            zoneName = zone.getName();
        }

        // validating zone and location
        Zone calculatedZone = findPolygon(longitude, latitude);
        String calculatedZoneCode;
        if (calculatedZone != null){
            calculatedZoneCode = calculatedZone.getCode();
        }else{
            calculatedZoneCode = "unknown";
        }

        if (!calculatedZoneCode.equals(zoneCode))
        {
            logger.error("The Location value specified does not correspond to the zone='" + zoneName + "'");
            throw new UserException("The Location value specified does not correspond to the zone='" + zoneName + "'");
        }

        // validating location and locationXYZ
        String locationXYZCalculated;
        if (!zoneCode.equals("unknown")){
            locationXYZCalculated = getLocationXYZ(zone, longitude, latitude);
        }else{
            locationXYZCalculated = locationXYZ;
        }

        if (!equivalentLocationXYZ(locationXYZ, locationXYZCalculated))
        {
            logger.error("The LocationXYZ value specified does not match its equivalent location value. LocationXYZ='"
                    + locationXYZ + "', locationXYZ calculated='" + locationXYZCalculated + "'");
            throw new UserException("The LocationXYZ value specified does not match its equivalent location value. LocationXYZ='"
                    + locationXYZ + "', locationXYZ calculated='" + locationXYZCalculated + "'");
        }

        Map<String, Object> fields = new HashMap<>();
        putField(fields, UDF.LAST_DETECT_TIME, String.valueOf(transactionDate.getTime()), transactionDate);
        putField(fields, UDF.LAST_LOCATE_TIME, String.valueOf(transactionDate.getTime()), transactionDate);
        udfs.putAll(fields);
    }

    public boolean equivalentLocationXYZ(String locationXYZ, String locaXYZCalculated)
    {
        //coodinates originals
        String[] locXYZ = locationXYZ.split(";");
        double x = Double.parseDouble(locXYZ[0]);
        double y = Double.parseDouble(locXYZ[1]);
        double z = Double.parseDouble(locXYZ[2]);

        //rounded to one decimal
        x=Math.rint(x*10)/10;
        y=Math.rint(y*10)/10;
        z=Math.rint(z*10)/10;


        //coodinates calculates
        String[] locXYZCalculated = locaXYZCalculated.split(";");
        double x0 = Double.parseDouble(locXYZCalculated[0]);
        double y0 = Double.parseDouble(locXYZCalculated[1]);
        double z0 = Double.parseDouble(locXYZCalculated[2]);

        return Double.compare(x,x0)==0 && Double.compare(y,y0)==0;
    }

    public void putField(Map<String, Object> fields, String field, String value, Date time)
    {
        Map<String, Object> udfObject = new HashMap<>();
        udfObject.put("value", value);
        udfObject.put("time", String.valueOf(time.getTime()));

        fields.put(field, udfObject);
    }

    public boolean isValidZone(Map zoneObject, Group group)
    {
        if (zoneObject.get("value") == null)
        {
            logger.error("Zone is invalid. ZoneLocation fields cannot be calculated.");
            throw new UserException("Zone is invalid, please choose a valid Zone.");
        }

        String zoneCode = zoneObject.get("value").toString();
        if (StringUtils.isEmpty(zoneCode))
        {
            logger.error("Zone value is empty. ZoneLocation fields cannot be calculated.");
            throw new UserException("Zone is empty. please choose a Zone.");
        }

        ZoneService zoneService = ZoneService.getInstance();
        Zone zone = null;
        try {
            zone = zoneService.getByCodeAndGroup(zoneCode, group);
        } catch (Exception e) {
            logger.error(e);
        }

        if (zone == null)
        {
            logger.error("Zone code=" + zoneCode + " is invalid, ZoneLocation fields cannot be calculated.");
            throw new UserException("Zone code=" + zoneCode + " is invalid, please choose a valid Zone.");
        }
        this.zone = zone;
        return true;
    }

    public boolean isUnknownZone(Map zoneObject)
    {
        if (zoneObject.get("value") == null)
        {
            logger.error("Zone is invalid. ZoneLocation fields cannot be calculated.");
            throw new UserException("Zone is invalid, please choose a valid Zone.");
        }

        String zoneCode = zoneObject.get("value").toString();
        if (StringUtils.isEmpty(zoneCode))
        {
            logger.error("Zone value is empty. ZoneLocation fields cannot be calculated.");
            throw new UserException("Zone is empty. please choose a Zone.");
        }

        return zoneCode.equals("unknown");
    }

    public static boolean isValidLocation(Map locationObject, String locationFieldName)
    {
        if (locationObject.get("value") == null)
        {
            logger.error(locationFieldName + " is null. ZoneLocation fields cannot be calculated.");
            return false;
        }

        String location = locationObject.get("value").toString();
        if (StringUtils.isEmpty(location))
        {
            logger.error(locationFieldName + " value is empty. ZoneLocation fields cannot be calculated.");
            return false;
        }

        if (!ThingTypeFieldService.getInstance().isCoordinates(location)) {
            logger.error(String.format("%s value='%s' is invalid, format incorrect format ", locationFieldName,
                    location));
            return false;
        }

        // validating location value
        String[] loc = location.split(";");

        if (loc.length != 3)
        {
            logger.error(locationFieldName + " value=" + location + " is invalid, ZoneLocation fields cannot be calculated");
            return false;
        }

        if (loc[0] == null || loc[1] == null)
        {
            logger.error(locationFieldName + " value=" + location + " is invalid, ZoneLocation fields cannot be calculated");
            return false;
        }
        return true;
    }

    public static Zone findPolygon(double longitude, double latitude)
    {
        ZoneService zoneService = ZoneService.getInstance();
        List<Zone> zones = zoneService.getZones();
        for (Zone zone : zones)
        {
            Set<ZonePoint> zonePoints = zone.getZonePoints();
            List<double[]> points = new ArrayList<>();
            for (ZonePoint zonePoint : zonePoints){
                points.add(new double[]{zonePoint.getX(), zonePoint.getY()});
            }
            if(MathUtils.isPointInsidePolygon(points, longitude, latitude))
            {
                return zone;
            }
        }
        return null;
    }

    public String getLocationXYZ(Zone zone, double longitude, double latitude)
    {
        String locationXYZ = "";
        LocalMap localMap = zone.getLocalMap();
        if (localMap != null)
        {
            double lonOrigin = localMap.getLonOrigin() != null ? localMap.getLonOrigin() : 0;
            double latOrigin = localMap.getLatOrigin() != null ? localMap.getLatOrigin() : 0;
            double altOrigin = localMap.getAltOrigin() != null ? localMap.getAltOrigin() : 0;
            double declination = localMap.getDeclination() != null ? localMap.getDeclination() : 0;
            String imageUnit = localMap.getImageUnit();

            CoordinateUtils cu = new CoordinateUtils(lonOrigin, latOrigin, altOrigin, declination, imageUnit);
            double[] xyz = cu.lonlat2xy(longitude, latitude, 0.0);
            double x = xyz[0];
            double y = xyz[1];
            double z = xyz[2];
            locationXYZ = printLocationXYZ(x, y, z);
        }
        return locationXYZ;
    }

    public String printLocationXYZ(double x, double y, double z)
    {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("0.0", symbols);
        return df.format(x) + ";" + df.format(y) + ";" + z;
    }

    public String printLocation(double longitude, double latitude, double altitude){
        return longitude + ";" + latitude + ";" +  "0.0";
    }

    public boolean requiredThingTypeFieldsAvailable(String thingTypeCode)
    {
        requiredZoneFields = new HashMap<>();
        DataTypeService dataTypeService = DataTypeService.getInstance();
        for(int i = 0; i < ZONE_DATA_TYPE_LIST.size(); i++) {
            Long zdt = ZONE_DATA_TYPE_LIST.get(i);
            String zdn = ZONE_DATA_NAME_LIST.get(i);
            List<ThingTypeField> thingTypeFields = dataTypeService.getThingTypeFieldsFromCache(thingTypeCode, zdt);
            if(thingTypeFields == null || thingTypeFields.size() != 1){
                return false;
            }
            requiredZoneFields.put(zdn, thingTypeFields.get(0));
        }
        List<ThingTypeField> thingTypeFields = dataTypeService.getThingTypeFieldsFromCache(thingTypeCode,
                ThingTypeField.Type.TYPE_TIMESTAMP.value);
        if(thingTypeFields == null || thingTypeFields.size() < 2){
            return false;
        }
        List<ThingTypeField> udfLastLocateAndDetect = thingTypeFields.stream()
                .filter(ttf -> ttf.getName().equals(UDF.LAST_DETECT_TIME) || ttf.getName().equals(UDF.LAST_LOCATE_TIME))
                .collect(Collectors.toList());
        if(udfLastLocateAndDetect == null || udfLastLocateAndDetect.size() != 2){
            return false;
        }
        udfLastLocateAndDetect.forEach(e -> requiredZoneFields.put(e.getName(), e));
        return true;
    }

    public void putField(Map<String, Object> fields, String thingTypeCode, String dataTypeCode, String value, Date time)
    {
        ThingTypeFieldService thingTypeFieldService = ThingTypeFieldServiceBase.getInstance();
        ThingTypeField thingTypeField = thingTypeFieldService.getByThingTypeCodeAndDataTypeCode(thingTypeCode, dataTypeCode);

        Map<String, Object> udfObject = new HashMap<>();
        udfObject.put("value", value);
        udfObject.put("time", String.valueOf(time.getTime()));

        fields.put(thingTypeField.getName(), udfObject);
    }

}
