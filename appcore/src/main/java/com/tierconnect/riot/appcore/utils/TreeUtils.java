package com.tierconnect.riot.appcore.utils;

import java.util.Collections;

import java.util.List;
import java.util.Map;

/**
 * Created by agutierrez on 10/7/15.
 * Tree view utils.
 */
public class TreeUtils {

    public static void sortObjects(String orderDirection, List<Map<String, Object>> list) {
        if (orderDirection != null && !orderDirection.isEmpty()) {
            final String order = orderDirection.substring(0, orderDirection.indexOf(":"));
            final String direction = orderDirection.substring(orderDirection.indexOf(":") + 1);
            list.sort((m1, m2) -> {
                if (m1 == null || m2 == null) {
                    return 0;
                }
                Object val1;
                Object val2;
                if (order.contains(".")) {
                    val1 = ((Map) m1.get(order.substring(0, order.indexOf(".")))).get(order.substring(order.indexOf(".") + 1));
                    val2 = ((Map) m2.get(order.substring(0, order.indexOf(".")))).get(order.substring(order.indexOf(".") + 1));
                } else {
                    val1 = m1.get(order);
                    val2 = m2.get(order);
                }
                if (val1 == null || val2 == null) {
                    return 0;
                }
                if (val1 instanceof String) {
                    return (((String) val1).toLowerCase()).compareTo(((String) val2).toLowerCase());
                }
                if (val1 instanceof Number) {
                    return new Double(((Number) val1).doubleValue()).compareTo(((Number) val2).doubleValue());
                }
                return 0;
            });
            if (!"asc".equals(direction)) {
                Collections.reverse(list);
            }
        }
    }

}
