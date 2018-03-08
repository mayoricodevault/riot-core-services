package com.tierconnect.riot.iot.utils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.*;
/**
 * Created by user on 11/20/14.
 */
public class RiotComparator implements Comparator<Object> {

    private String compareBy = "";
    private String orderBy = "ASC";

    public RiotComparator(String compareBy, String orderBy) {
        setCompareBy(compareBy);
        setOrderBy(orderBy);
    }
    public String getCompareBy() {
        return this.compareBy;
    }
    public void setCompareBy(String compareBy) {
        this.compareBy = compareBy;
    }

    public String getOrderBy() {
        return this.orderBy;
    }
    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public int compare(Object e1, Object e2)
    {
        Map<String, Object> firstElem = (Map<String, Object>) e1;
        Map<String, Object> secondElem = (Map<String, Object>) e2;

        String firstElemVal = firstElem.get(getCompareBy()) != null?firstElem.get(getCompareBy()).toString():"";
        String secondElemVal = secondElem.get(getCompareBy()) != null?secondElem.get(getCompareBy()).toString():"";

        if (getOrderBy().equals("ASC")) {
            return firstElemVal.compareTo(secondElemVal);
        } else {
            return secondElemVal.compareTo(firstElemVal);
        }
    }
}
