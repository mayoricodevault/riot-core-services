package com.tierconnect.riot.iot.entities;


import javax.persistence.Entity;
import javax.persistence.Table;

import org.geojson.Feature;
import org.geojson.LngLatAlt;
import org.geojson.Polygon;
import com.tierconnect.riot.commons.utils.MathUtils;

import java.util.*;

@Entity
@Table(name = "zone")
public class Zone extends ZoneBase {

    public static final List<String> zoneDwellProperties = Collections.unmodifiableList(Arrays.asList("facilityMap","zoneGroup","zoneType"));

    //TODO: refactor to eliminate this ! Bad idea ! Do this in the services class, or better yet, handle through use of the extra param !
    @Override
    public Map<String, Object> publicMap() {
        Map<String, Object> publicMap = super.publicMap();
        List<Map<String, Object>> points = new LinkedList<>();
        for (ZonePoint point : this.getZonePointsSorted()) {
            points.add(point.publicMap());
        }
        publicMap.put("zonePoints", points);
        if(getZoneGroup() != null){
            publicMap.put("zoneGroup", getZoneGroup().publicMap());
        }
        return publicMap;
    }
    
    /*For mongo*/
    public Map<String, Object> publicMapSummarized() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put( "id", getId() );
        map.put( "name", getName() );
        map.put( "code", getCode() );

        if(getLocalMap() != null){
            map.put("facilityMap", getLocalMap().getName());
        }
        if(getZoneGroup() != null){
            map.put("zoneGroup", getZoneGroup().getName());
        }
        if(getZoneType() != null){
            map.put("zoneType", getZoneType().getZoneTypeCode());
        }
        return map;
    }

    public Feature publicGeoJsonFeature() {
        Feature feature = new Feature();
        feature.setId(this.id + "");
        feature.setProperty("id", "" + this.id);
        feature.setProperty("name", this.name);
        feature.setProperty("description", this.description);
        feature.setProperty("color", this.color);

        //Adding zoneGroup
        Map<String, Object> zoneGroup = new HashMap<>();
        if(this.getZoneGroup() != null) {
            zoneGroup.put("id", this.getZoneGroup().getId());
            zoneGroup.put("name", this.getZoneGroup().getName());
            feature.setProperty("zoneGroup", zoneGroup);
        }

        //Adding zoneType
        Map<String, Object> zoneType = new HashMap<>();
        if(this.getZoneType() != null) {
            zoneType.put("id", this.getZoneType().getId());
            zoneType.put("name", this.getZoneType().getName());
            feature.setProperty("zoneType", zoneType);
        }

        Polygon polygon = new Polygon();
        List<LngLatAlt> coordinates = new LinkedList<>();
        ZonePoint firstPoint = null;
        ZonePoint[] zonePointsSorted = this.getZonePointsSorted();
        for (ZonePoint zonePoint : zonePointsSorted) {
            if (firstPoint == null) {
                firstPoint = zonePoint;
            }
            LngLatAlt point = new LngLatAlt(zonePoint.x, zonePoint.y, 0);
            coordinates.add(point);
        }
        if (firstPoint != null) {
            LngLatAlt point = new LngLatAlt(firstPoint.x, firstPoint.y, 0);
            coordinates.add(point);
        }
        polygon.add(coordinates);
        feature.setGeometry(polygon);
        return feature;
    }

    public ZonePoint[] getZonePointsSorted() {
        ZonePoint[] zonePointsSorted = new ZonePoint[this.zonePoints.size()];
        this.zonePoints.toArray(zonePointsSorted);
        Arrays.sort(zonePointsSorted, new Comparator<ZonePoint>() {

            @Override
            public int compare(ZonePoint z1, ZonePoint z2) {
                return z1.arrayIndex.compareTo(z2.arrayIndex);
            }

        });
        return zonePointsSorted;
    }

    public List<double[]> getZonePointsAsList()
    {
    	List<double[]> points = new ArrayList<double[]>();
    	
    	for( ZonePoint zp : this.getZonePoints() )
    	{
    		points.add( new double [] { zp.x, zp.y } );
    	}
    	
    	return points;
    }
    
    public double [] calculateCentroid()
	{
		return MathUtils.calculateCentroid( getZonePointsAsList() );
	}
}
