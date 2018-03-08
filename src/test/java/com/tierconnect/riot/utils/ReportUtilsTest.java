package com.tierconnect.riot.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Transaction;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.services.ReportDefinitionService;
import com.tierconnect.riot.iot.utils.ReportUtils;

public class ReportUtilsTest {

    static Transaction transaction;
    
    @BeforeClass
    public static void beforeClass() {
        transaction = ReportDefinitionService.getReportDefinitionDAO().getSession().getTransaction();
        transaction.begin();
    }

    @AfterClass
    public static void afterClass() {
        transaction.commit();
    }
    
    @Test
    public void testReportExecution() {
/*
        int historyPageSize = 3;
        int pageSize = 10;
        int pageNumber = 1;
        ReportDefinition reportDefinition = ReportDefinitionService.getInstance().get(1L);
        Map<String, Object> filters = new HashMap<>();
//        filters.put("4", "C201401008");
//        filters.put("4", "00210BC7");
        filters.put("3", "rfid");
        Map<String, Object> result = ReportUtils.executeReport(reportDefinition, filters, null, null, historyPageSize, pageSize, pageNumber, null, null, null, null);
        System.out.println("total: " + result.get("total"));
        System.out.println("results: ");
        List<Map<String, Object>> results = (List<Map<String, Object>>)result.get("results");
        for (Map<String, Object> map : results) {
            for (String key : map.keySet()) {
                System.out.println(key + ": " + map.get(key));
            }
        }
*/
    }

}
