package com.tierconnect.riot.api.database.utils;

import org.bson.Transformer;

import java.util.Map;

/**
 * Created by vealaro on 12/30/16.
 */
public class ZoneTransformer implements Transformer {

    @Override
    @SuppressWarnings("unchecked")
    public Object transform(Object objectToTransform) {
                                    if (objectToTransform instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) objectToTransform;
            if (map.containsKey("code") && map.containsKey("name") && map.containsKey("facilityMap")
                    && map.containsKey("zoneGroup") && map.containsKey("zoneType")) {
                return new Zone(
                        (String) map.get("code"),
                        (String) map.get("name"),
                        (String) map.get("facilityMap"),
                        (String) map.get("zoneGroup"),
                        (String) map.get("zoneType")
                );
            }
        }
        return objectToTransform;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
