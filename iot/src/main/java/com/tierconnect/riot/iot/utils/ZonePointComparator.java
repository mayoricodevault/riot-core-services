package com.tierconnect.riot.iot.utils;

import com.tierconnect.riot.iot.entities.ZonePoint;

import java.util.Comparator;

/**
 * Created by cfernandez
 * on 12/16/2015.
 */
public class ZonePointComparator implements Comparator<ZonePoint>
{
    @Override
    public int compare(ZonePoint o1, ZonePoint o2) {
        return (o1.getArrayIndex()).compareTo(o2.getArrayIndex());
    }
}
