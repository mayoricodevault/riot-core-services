package com.tierconnect.riot.utils;

/**
 * Created by IntelliJ IDEA.
 *
 * @author : aruiz
 * @date : 10/18/16 2:36 PM
 * @version:
 */
public class MapUtil {
    // Radius of the earth (in feet)
    double Re = 6378137 / 0.3048;

    class Point {// in RADIANS
        double longitude;
        double latitude;
        // in feet (from the 'origin')
        double x;
        double y;
        // in pixels (from lower left hand corner of the image !)
        double px;
        double py;
    }

    // /**** @param p1* @param p2* @param imageWidth* in pixels* @param imageHeight in* pixels*/
    public void go(Point p1, Point p2, int imageWidth, int imageHeight) {
// in RADIANS
        double latitudeOrigin = p1.latitude - p1.y / Re;
// in RADIANS
        double longitudeOrigin = p1.longitude - p1.x / (Re * Math.cos(latitudeOrigin));
        double scaleLongitude = (p2.longitude - p1.longitude) / (p2.x - p1.y);
        double scaleLatitude = (p2.latitude - p1.latitude) / (p2.y - p1.y);
// in RADIANS
        double longitudeMin = p1.longitude - scaleLongitude * p1.x;
        double longitudeMax = longitudeMin + scaleLongitude * imageWidth;
// in RADIANS
        double latitudeMin = p1.latitude - scaleLatitude * p1.y;
        double latitudeMax = latitudeMin + scaleLatitude * imageHeight;
    }
}
