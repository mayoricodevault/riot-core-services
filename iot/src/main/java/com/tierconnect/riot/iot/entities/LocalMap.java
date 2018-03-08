package com.tierconnect.riot.iot.entities;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.*;

/**
 * Created by rsejas on 3/14/17.
 */
@Entity
@Table(name="localmap")
public class LocalMap extends LocalMapBase {
    @Override
    public Map<String, Object> publicMap() {
        Map<String, Object> publicMap = super.publicMap();
        if (localMapPoints == null) {
            return publicMap;
        }
        List<Map<String, Object>> points = new LinkedList<>();
        for (LocalMapPoint point : this.getMapPointsSorted()) {
            points.add(point.publicMap());
        }
        publicMap.put("mapPoints", points);
        return publicMap;
    }
    public LocalMapPoint[] getMapPointsSorted() {
        LocalMapPoint[] mapPointsSorted = new LocalMapPoint[this.localMapPoints.size()];
        this.localMapPoints.toArray(mapPointsSorted);
        Arrays.sort(mapPointsSorted, new Comparator<LocalMapPoint>() {

            @Override
            public int compare(LocalMapPoint z1, LocalMapPoint z2) {
                return z1.arrayIndex.compareTo(z2.arrayIndex);
            }

        });
        return mapPointsSorted;
    }
}
