package com.tierconnect.riot.iot.utils;

import com.tierconnect.riot.iot.entities.Zone;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by user on 11/26/14.
 */
public class ReportTimeSeriesUtils {



    public static void settingIntervalsChart(Map<String, List<Long> > zoneGroupMap,
                                           Map<Long, List<Object[]> > fieldValuesCassandra,
                                           Long[] intervalsGenerated,
                                           List<String> categories,
                                           int numberOfIntervals) {
        Long timeMin = Long.MAX_VALUE;
        Long timeMax = Long.MIN_VALUE;
        for (Map.Entry<Long, List<Object[]> > entry : fieldValuesCassandra.entrySet()) {
            List<Object[]> locationList = entry.getValue();
            for(int i=0; i < locationList.size() - 1; i++) {
                Object[] locationDataPrev = locationList.get(i);
                Object[] locationDataNext = locationList.get(i + 1);
                if(locationDataPrev.length > 0 && zoneGroupMap.get(locationDataPrev[1].toString()) != null) {
                    Long locationValuePrev = ((Date)(locationDataPrev[0])).getTime();
                    Long locationValueNext = ((Date)(locationDataNext[0])).getTime();

                    Long diffLocation = locationValueNext - locationValuePrev;
                    zoneGroupMap.get(locationDataPrev[1]).add(diffLocation);
                    timeMin = Math.min(diffLocation, timeMin);
                    timeMax = Math.max(diffLocation, timeMax);
                }
            }
        }
        long intervalTime = (timeMax - timeMin) / numberOfIntervals;
        Long toDays = TimeUnit.MILLISECONDS.toDays(timeMax);
        Long toHours = TimeUnit.MILLISECONDS.toHours(timeMax);
        Long toMinutes = TimeUnit.MILLISECONDS.toMinutes(timeMax);
        Long toSeconds = TimeUnit.MILLISECONDS.toSeconds(timeMax);

        String convertTo = "seconds";
        if(toSeconds > 0 && toSeconds/numberOfIntervals > 0)
            convertTo = "seconds";
        if(toMinutes > 0 && toMinutes/numberOfIntervals > 0)
            convertTo = "minutes";
        if(toHours > 0 && toHours/numberOfIntervals > 0)
            convertTo = "hours";
        if(toDays > 1 && toDays/numberOfIntervals > 0)
            convertTo = "days";

        for(int interval = 0; interval <= numberOfIntervals; interval++) {
            intervalsGenerated[interval] = interval * intervalTime;
            if (interval > 0) {
                categories.add(ReportTimeSeriesUtils.convertMillisTo(convertTo, intervalsGenerated[interval]));
            }
        }
        intervalsGenerated[intervalsGenerated.length-1] = timeMax + 1;
    }

    public static void countingZonesInIntervals(Map<String, Object[] > reportRes,
                                               Map<String, Integer> zoneCountTotal,
                                               List<Zone> zones,
                                               Map<String, List<Long> > zoneGroupMap,
                                               Long[] intervalsGenerated,
                                               int numberOfIntervals) {
        for(Zone zone : zones) {
            String zoneName = zone.getName();
            zoneCountTotal.put(zoneName, 0);
            List<Long> zoneTimestampList = zoneGroupMap.get(zoneName);
            Long[] countArray = new Long[numberOfIntervals];
            Arrays.fill(countArray, 0L);
            reportRes.put(zoneName, countArray);
            if(zoneTimestampList != null) {
                for(int dwellTime = 0; dwellTime < zoneTimestampList.size(); dwellTime++) {
                    int closestK = getClosestK(intervalsGenerated, zoneTimestampList.get(dwellTime));
                    if(closestK - 1 >= 0) {
                        Long newValue = Long.parseLong(reportRes.get(zoneName)[closestK - 1].toString()) + 1;
                        reportRes.get(zoneName)[closestK - 1] = newValue;
                        zoneCountTotal.put(zoneName, zoneCountTotal.get(zoneName) + 1);
                    }
                }
            }
        }
    }

    public static String convertMillisTo(String convertTo, Long time) {
        if(convertTo.equals("seconds"))
            return TimeUnit.MILLISECONDS.toSeconds(time) + "s";
        if(convertTo.equals("minutes"))
            return TimeUnit.MILLISECONDS.toMinutes(time) + "m";
        if(convertTo.equals("hours"))
            return TimeUnit.MILLISECONDS.toHours(time) + "h";
        if(convertTo.equals("days"))
            return TimeUnit.MILLISECONDS.toDays(time) + "d";
        return TimeUnit.MILLISECONDS.toSeconds(time) + "s";
    }

    public static int getClosestK(Long[] a, Long x) {
        int idx = java.util.Arrays.binarySearch(a, x);
        if ( idx < 0 ) {
            idx = -idx - 1;
        }
        return idx;
    }
}
