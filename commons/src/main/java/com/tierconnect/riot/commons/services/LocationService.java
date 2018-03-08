package com.tierconnect.riot.commons.services;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.DataType;
import com.tierconnect.riot.commons.UDF;
import com.tierconnect.riot.commons.entities.IThingField;
import com.tierconnect.riot.commons.utils.CoordinateUtils;
import com.tierconnect.riot.commons.utils.MathUtils;
import org.apache.log4j.Logger;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

/**
 * Created by cfernandez
 * on 10/7/15.
 */
public abstract class LocationService
{
    private static Logger logger = Logger.getLogger(LocationService.class);

    /**
     * This method was deprecated on 2017/04/06
     * This method was deprecated on VIZIX Version: 5.0.0_RC06
     * calculates location, locationXYZ, lastDetectTime, and lastLocateTime fields according to the zone value
     * @param zoneCode code of the zone
     * @param transactionDate date
     * @return list of thingFields calculated
     */
    @Deprecated
    public List<IThingField> performZoneChange(String thingTypeCode, String zoneCode, Date transactionDate)
    {
        List<IThingField> fields = new ArrayList<>();

        List<double[]> points = getZonePoints(zoneCode);
        double[] longLatCenter = MathUtils.calculateCentroid((points));
        double longitude = longLatCenter[0];
        double latitude = longLatCenter[1];
        double altitude = 0;

        Map<String, Object> localMap = getLocalMap(zoneCode);

        double lonOrigin = 0.0;
        double latOrigin = 0.0;
        double altOrigin = 0.0;
        double declination = 0.0;
        String imageUnit = "";

        if (localMap !=  null)
        {
            lonOrigin = localMap.get("lonOrigin") != null ? (double)localMap.get("lonOrigin") : 0;
            latOrigin = localMap.get("latOrigin") != null ? (double)localMap.get("latOrigin") : 0;
            altOrigin = localMap.get("altOrigin") != null ? (double)localMap.get("altOrigin") : 0;
            declination = localMap.get("declination") != null ? (double)localMap.get("declination") : 0;
            imageUnit = localMap.get("imageUnit") != null ? (String)localMap.get("imageUnit") : "";
        }

        CoordinateUtils cu = new CoordinateUtils(lonOrigin, latOrigin, altOrigin, declination, imageUnit);
        double[] xyz = cu.lonlat2xy(longitude, latitude, altOrigin);
        double x = xyz[0];
        double y = xyz[1];
        double z = xyz[2];

        putField(fields, thingTypeCode, DataType.COORDINATES, printLocation(longitude, latitude, altOrigin), transactionDate);
        putField(fields, thingTypeCode, DataType.XYZ, printLocationXYZ(x, y, z), transactionDate);
        putField(fields, thingTypeCode, DataType.NUMBER, UDF.LAST_DETECT_TIME, String.valueOf(transactionDate.getTime()), transactionDate);
        putField(fields, thingTypeCode, DataType.NUMBER, UDF.LAST_LOCATE_TIME, String.valueOf(transactionDate.getTime()), transactionDate);

        return fields;
    }

    /**
     * calculates location, locationXYZ, lastDetectTime, and lastLocateTime fields according to the zone value
     * @param zoneCode code of the zone
     * @param transactionDate date
     * @return list of thingFields calculated
     */
    public List<IThingField> performZoneChange(String thingTypeCode, String zoneCode, Date transactionDate, long groupId)
    {
        List<IThingField> fields = new ArrayList<>();

        List<double[]> points = getZonePoints(zoneCode, groupId);
        double[] longLatCenter = MathUtils.calculateCentroid((points));
        double longitude = longLatCenter[0];
        double latitude = longLatCenter[1];
        double altitude = 0;

        Map<String, Object> localMap = getLocalMap(zoneCode, groupId);

        double lonOrigin = 0.0;
        double latOrigin = 0.0;
        double altOrigin = 0.0;
        double declination = 0.0;
        String imageUnit = "";

        if (localMap !=  null)
        {
            lonOrigin = localMap.get("lonOrigin") != null ? (double)localMap.get("lonOrigin") : 0;
            latOrigin = localMap.get("latOrigin") != null ? (double)localMap.get("latOrigin") : 0;
            altOrigin = localMap.get("altOrigin") != null ? (double)localMap.get("altOrigin") : 0;
            declination = localMap.get("declination") != null ? (double)localMap.get("declination") : 0;
            imageUnit = localMap.get("imageUnit") != null ? (String)localMap.get("imageUnit") : "";
        }

        CoordinateUtils cu = new CoordinateUtils(lonOrigin, latOrigin, altOrigin, declination, imageUnit);
        double[] xyz = cu.lonlat2xy(longitude, latitude, altOrigin);
        double x = xyz[0];
        double y = xyz[1];
        double z = xyz[2];

        putField(fields, thingTypeCode, DataType.COORDINATES, printLocation(longitude, latitude, altOrigin), transactionDate);
        putField(fields, thingTypeCode, DataType.XYZ, printLocationXYZ(x, y, z), transactionDate);
        putField(fields, thingTypeCode, DataType.NUMBER, UDF.LAST_DETECT_TIME, String.valueOf(transactionDate.getTime()), transactionDate);
        putField(fields, thingTypeCode, DataType.NUMBER, UDF.LAST_LOCATE_TIME, String.valueOf(transactionDate.getTime()), transactionDate);

        return fields;
    }

    /**
     *
     * @deprecated use {@link #performLocationChange(String, String, Date, CoordinateUtils)} instead.
     * Deprecated in version 4.5.0_RC4, date: 13/12/2016
     *
     * calculates zone, locationXYZ, lastDetectTime and lastLocateTime fields according to the location value
     * @param location location value
     * @param transactionDate date
     * @param groupId id of the group of the thing
     * @return list of thing fields calculated
     */
    @Deprecated public List<IThingField> performLocationChange(String thingTypeCode, String location, Date transactionDate, long groupId)
    {
        List<IThingField> fields = new ArrayList<>();

        String[] loc = location.split(";");
        double longitude = Double.parseDouble(loc[0]);
        double latitude = Double.parseDouble(loc[1]);
        double altitude = Double.parseDouble(loc[2]);

        String zoneCode = getZoneCode(longitude, latitude);

        Map<String, Object> localMap;
        if (zoneCode != null){
            localMap = getLocalMap(zoneCode, groupId);
        }
        else
        {
            // if the zone is "Unknown", the localMap will be gotten using the groupId of the thing, it is necessary
            // to calculate locationXYZ
            localMap = getLocalMap(groupId);
            zoneCode = Constants.UNKNOWN_ZONE_CODE;
        }

        double lonOrigin = localMap.get("lonOrigin") != null ? (double)localMap.get("lonOrigin") : 0;
        double latOrigin = localMap.get("latOrigin") != null ? (double)localMap.get("latOrigin") : 0;
        double altOrigin = localMap.get("altOrigin") != null ? (double)localMap.get("altOrigin") : 0;
        double declination = localMap.get("declination") != null ? (double)localMap.get("declination") : 0;
        String imageUnit = localMap.get("imageUnit") != null ? (String)localMap.get("imageUnit") : "";

        CoordinateUtils cu = new CoordinateUtils(lonOrigin, latOrigin, altOrigin, declination, imageUnit);
        double[] xyz = cu.lonlat2xy(longitude, latitude, altitude);
        double x = xyz[0];
        double y = xyz[1];
        double z = xyz[2];

        putField(fields, thingTypeCode, DataType.ZONE, zoneCode, transactionDate);
        putField(fields, thingTypeCode, DataType.XYZ, printLocationXYZ(x, y, z), transactionDate);
        putField(fields, thingTypeCode, DataType.NUMBER, UDF.LAST_DETECT_TIME, String.valueOf(transactionDate.getTime()), transactionDate);
        putField(fields, thingTypeCode, DataType.NUMBER, UDF.LAST_LOCATE_TIME, String.valueOf(transactionDate.getTime()), transactionDate);

        return fields;
    }

    /**
     *
     * @deprecated use {@link #performLocationXYZChange(String, String, Date, CoordinateUtils)} instead
     * Deprecated in version 4.5.0_RC4, date: 13/12/2016
     *
     * calculates zone, location, lastDetectTime and lastLocateTime fields according to the locationXYZ value
     * @param locationXYZ locationXYZ value
     * @param transactionDate date
     * @param groupId id of the group of the thing
     * @return list of thingFields calculated
     */
    @Deprecated public List<IThingField> performLocationXYZChange(String thingTypeCode, String locationXYZ, Date transactionDate, long groupId)
    {
        List<IThingField> fields = new ArrayList<>();

        String[] locXYZ = locationXYZ.split(";");
        double x = Double.parseDouble(locXYZ[0]);
        double y = Double.parseDouble(locXYZ[1]);
        double z = Double.parseDouble(locXYZ[2]);

        Map<String, Object> localMap = getLocalMap(groupId);
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

        String zoneCode = getZoneCode(longitude, latitude);
        if (zoneCode == null){
            zoneCode = Constants.UNKNOWN_ZONE_CODE;
        }

        String locationXYZString = printLocation(longitude, latitude, altitude);

        putField(fields, thingTypeCode, DataType.ZONE, zoneCode, transactionDate);
        putField(fields, thingTypeCode, DataType.COORDINATES, locationXYZString, transactionDate);
        putField(fields, thingTypeCode, DataType.NUMBER, UDF.LAST_DETECT_TIME, String.valueOf(transactionDate.getTime()), transactionDate);
        putField(fields, thingTypeCode, DataType.NUMBER, UDF.LAST_LOCATE_TIME, String.valueOf(transactionDate.getTime()), transactionDate);

        return fields;
    }

    /**
     * This method calculates zone, locationXYZ, lastDetectTime and lastLocateTime fields according to the location
     * value given
     * @param thingTypeCode thingTypeCode
     * @param location location value
     * @param transactionDate date which will be used as lastDetectTime and lastLocateTime
     * @param coordinateUtils coordinateUtils class instance which contains all data about the origin of the map
     * @return list of thingFields calculated
     */
    public List<IThingField> performLocationChange(String thingTypeCode, String location, Date transactionDate,
                                                   CoordinateUtils coordinateUtils){
        List<IThingField> fields = new ArrayList<>();

        String[] loc = location.split(";");
        double longitude = Double.parseDouble(loc[0]);
        double latitude = Double.parseDouble(loc[1]);
        double altitude = Double.parseDouble(loc[2]);

        String zoneCode = getZoneCode(longitude, latitude);
        if (zoneCode == null) {
            zoneCode = Constants.UNKNOWN_ZONE_CODE;
        }

        double[] xyz = coordinateUtils.lonlat2xy(longitude, latitude, altitude);
        double x = xyz[0];
        double y = xyz[1];
        double z = xyz[2];

        putField(fields, thingTypeCode, DataType.ZONE, zoneCode, transactionDate);
        putField(fields, thingTypeCode, DataType.XYZ, printLocationXYZ(x, y, z), transactionDate);
        putField(fields, thingTypeCode, DataType.NUMBER, UDF.LAST_DETECT_TIME,
                String.valueOf(transactionDate.getTime()), transactionDate);
        putField(fields, thingTypeCode, DataType.NUMBER, UDF.LAST_LOCATE_TIME,
                String.valueOf(transactionDate.getTime()), transactionDate);

        return fields;
    }

    /**
     * This method calculates zone, location, lastDetectTime and lastLocateTime fields according to the locationXYZ
     * value given
     * @param thingTypeCode thingTypeCode
     * @param locationXYZ locationXYZ value
     * @param transactionDate date which will be used as lastDetectTime and lastLocateTime
     * @param coordinateUtils coordinateUtils class instance which contains all data about the origin of the map
     * @return list of thingFields calculated
     */
    public List<IThingField> performLocationXYZChange(String thingTypeCode, String locationXYZ,
                                                      Date transactionDate, CoordinateUtils coordinateUtils)
    {
        List<IThingField> fields = new ArrayList<>();

        String[] locXYZ = locationXYZ.split(";");
        double x = Double.parseDouble(locXYZ[0]);
        double y = Double.parseDouble(locXYZ[1]);
        double z = Double.parseDouble(locXYZ[2]);

        double[] longLat = coordinateUtils.xy2lonlat(x, y, z);
        double longitude = longLat[0];
        double latitude = longLat[1];
        double altitude = longLat[2];

        String zoneCode = getZoneCode(longitude, latitude);
        if (zoneCode == null){
            zoneCode = Constants.UNKNOWN_ZONE_CODE;
        }

        String locationXYZString = printLocation(longitude, latitude, altitude);

        putField(fields, thingTypeCode, DataType.ZONE, zoneCode, transactionDate);
        putField(fields, thingTypeCode, DataType.COORDINATES, locationXYZString, transactionDate);
        putField(fields, thingTypeCode, DataType.NUMBER, UDF.LAST_DETECT_TIME,
                String.valueOf(transactionDate.getTime()), transactionDate);
        putField(fields, thingTypeCode, DataType.NUMBER, UDF.LAST_LOCATE_TIME,
                String.valueOf(transactionDate.getTime()), transactionDate);

        return fields;
    }

    protected abstract String getZoneCode(double longitude, double latitude);

    /**
     * This method was deprecated on 2017/04/06
     * This method was deprecated on VIZIX Version: 5.0.0_RC06
     */
    @Deprecated
    protected abstract Map<String,Object> getLocalMap(String zoneCode);

    protected abstract Map<String,Object> getLocalMap(String zoneCode, long groupId);

    /**
     * This method was deprecated on 2017/04/06
     * This method was deprecated on VIZIX Version: 5.0.0_RC06
     */
    @Deprecated
    protected abstract List<double[]> getZonePoints(String zoneCode);

    protected abstract List<double[]> getZonePoints(String zoneCode, long groupId);

    protected abstract Map<String,Object> getLocalMap(long groupId);

    protected abstract void putField(List<IThingField> fields, String thingTypeCode, String fieldName, String value, Date time);

    protected abstract void putField(List<IThingField> fields, String thingTypeCode, String dataTypeCode, String fieldName, String value, Date time);

    public String printLocationXYZ(double x, double y, double z)
    {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat df = new DecimalFormat("0.0", symbols);
        return df.format(x) + ";" + df.format(y) + ";" + df.format(z);
    }

    public String printLocation(double longitude, double latitude, double altitude){
        return longitude + ";" + latitude + ";" +  altitude;
    }

}
