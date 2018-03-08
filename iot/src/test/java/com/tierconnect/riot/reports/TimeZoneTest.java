package com.tierconnect.riot.reports;

import com.tierconnect.riot.appcore.services.GroupService;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * Created by vealaro on 3/7/17.
 */
public class TimeZoneTest {

    @Test
    public void listTimeZone() {
        Set<String> zoneIds = DateTimeZone.getAvailableIDs();
        org.joda.time.format.DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("ZZ");
        for (String zoneId : zoneIds) {
            String offset = dateTimeFormatter.withZone(DateTimeZone.forID(zoneId)).print(0);
            String longName = TimeZone.getTimeZone(zoneId).getDisplayName();
            System.out.println("(" + offset + ") " + zoneId + ", " + longName);
        }
        System.out.println("Size JODA= " + zoneIds.size());
        System.out.println("size J8  = " + GroupService.getInstance().getRegionalSettings().size());

    }

    @Test
    public void diffTimeZoneBetweenJava8Joda() {
        List<String> listJoda = new ArrayList<>(DateTimeZone.getAvailableIDs());
        List<String> listJava8 = new ArrayList<>(GroupService.getInstance().getRegionalSettings().keySet());
        List<String> listDiff = new ArrayList<>();

        for (String value : listJoda) {
            if (!listJava8.contains(value)) {
                listDiff.add("JODA - " + value);
            }
        }
        System.out.println("---");
        for (String value : listJava8) {
            if (!listJoda.contains(value)) {
                listDiff.add("  J8 - " + value);
            }
        }
        for (String diff : listDiff) {
            System.out.println("--> " + diff);
        }
        System.out.println(listDiff.size());
    }
}
