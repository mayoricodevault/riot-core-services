package com.tierconnect.riot.commons.utils;

import java.text.DecimalFormat;

/**
 * Created by agutierrez on 3/19/15.
 */
public class CoordinateUtils {
    public double lonOrigin;
    public double latOrigin;
    public double altOrigin;
    public double declination;
    public String units;

    // double rEarth = 3959.0 * 5280.0;
    final double rEarth_m = 6378137;
    final double rEarth_ft = 6378137 / 0.3048;

    double rEarth;

    double cosdec;
    double sindec;
    double coslat;

    double feet_2_meters = 0.3048;
    double meters_2_feet = 3.28084;

    double f = 1.0;

    private static CoordinateUtils instance = null;

    public static CoordinateUtils getInstance() {
        if (instance == null) {
            instance = new CoordinateUtils(0, 0, 0, 0, "ft");
        }
        return instance;
    }

    /**
     * @param lonOrigin   in degrees
     * @param latOrigin   in degrees
     * @param declination in degrees
     * @param units       either "feet" or "meters"
     */
    public CoordinateUtils(double lonOrigin, double latOrigin, double altOrigin, double declination, String units) {
        setProperties(lonOrigin, latOrigin, altOrigin, declination, units);
    }

    public double[] xy2lonlat(double x, double y, double z) {
        double x2 = x * cosdec - y * sindec;
        double y2 = x * sindec + y * cosdec;

        double lon = lonOrigin * (Math.PI / 180.0) + x2 / (rEarth * coslat);
        double lat = latOrigin * (Math.PI / 180.0) + y2 / rEarth;
        double el = altOrigin + z;

        return new double[]{lon * 180.0 / Math.PI, lat * 180.0 / Math.PI, el};
    }

    public double[] xy2lonlat(double x, double y, double z, double longOrigin, double latiOrigin, double altiOrigin) {
        double x2 = x * cosdec - y * sindec;
        double y2 = x * sindec + y * cosdec;
        double coslati = Math.cos(latiOrigin * Math.PI / 180.0);

        double lon = longOrigin * (Math.PI / 180.0) + x2 / (rEarth * coslati);
        double lat = latiOrigin * (Math.PI / 180.0) + y2 / rEarth;
        double el = altiOrigin + z;

        return new double[]{lon * 180.0 / Math.PI, lat * 180.0 / Math.PI, el};
    }

    // lon, lat in degress
    public double[] lonlat2xy(double lon, double lat, double ele) {
        double x2 = (lon - lonOrigin) * (Math.PI / 180.0) * (rEarth * coslat);
        double y2 = (lat - latOrigin) * (Math.PI / 180.0) * rEarth;
        double z = ele - altOrigin;

        double x = x2 * cosdec + y2 * sindec;
        double y = -x2 * sindec + y2 * cosdec;

        return new double[]{x, y, z};
    }

    public void setProperties(double lonOrigin, double latOrigin, double altOrigin, double declination, String units) {
        this.lonOrigin = lonOrigin;
        this.latOrigin = latOrigin;
        this.altOrigin = altOrigin;
        this.declination = declination;
        this.units = units;

        cosdec = Math.cos(this.declination * Math.PI / 180.0);
        sindec = Math.sin(this.declination * Math.PI / 180.0);
        coslat = Math.cos(this.latOrigin * Math.PI / 180.0);

        // "feet" or "meters"
        if ("feet".equals(units) || "ft".equals(units)) {
            rEarth = rEarth_ft;
        } else {
            rEarth = rEarth_m;
        }
    }

    /**
     * @param lat1 Point A's latitude
     * @param lon1 Point A's longitude
     * @param lat2 Point B's latitude
     * @param lon2 Point B's longitude
     * @return Distance between Point A and Point B
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = rEarth;
        double deltaLatitude = Math.toRadians(lat2 - lat1);
        double deltaLongitude = Math.toRadians(lon2 - lon1);

        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.sin(deltaLatitude/2) * Math.sin(deltaLatitude/2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(deltaLongitude/2) * Math.sin(deltaLongitude/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    public Double roundCoordinate(String value) {
        DecimalFormat df = new DecimalFormat("#.######");
        return new Double(df.format(Double.valueOf(value)));
    }

    public void setUnits(String units) {
        rEarth = rEarth_m;
        // "feet"
        if ("feet".equals(units) || "ft".equals(units)) {
            rEarth = rEarth_ft;
        }
    }

    public static void main(String[] args) {
        CoordinateUtils cu = new CoordinateUtils(-118.0, 45.0, 10, 20, "feet");

        for (double i = 0; i < 10; i++) {
            double[] l = cu.xy2lonlat(i, i, 0);

            double[] x = cu.lonlat2xy(l[0], l[1], l[2]);

            System.out.println(String.format("(%.1f,%.1f,%.1f)  (%.6f,%.6f,%.6f) (%.1f,%.1f,%.1f)", i, i, 0.0, l[0], l[1], l[2], x[0],
                    x[1], x[2]));
        }
    }
}
