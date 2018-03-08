package com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by achambi on 8/4/17.
 * class for test List report log.
 */
public class ListReportLogTest {

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * If we have:
     * 10 Index Cancelled
     * 10 Index In Progress
     * 10 Index Completed
     * 10 Index Slow Index
     * Count should display just a count of 30, because Cancelled is not taking in account
     * Total 30
     *
     * @throws Exception If test fails.
     */
    @Test
    public void verifyTotal() throws Exception {
        List<ReportLogInfo> reportLogInfoList = new ArrayList<>();
        reportLogInfoList.addAll(createReportLog(ReportLogStatus.CANCELED));
        reportLogInfoList.addAll(createReportLog(ReportLogStatus.IN_PROGRESS));
        reportLogInfoList.addAll(createReportLog(ReportLogStatus.COMPLETED));
        reportLogInfoList.addAll(createReportLog(ReportLogStatus.SLOW_INDEX));
        ListReportLog listReportLog = new ListReportLog(reportLogInfoList);
        assertNotNull(listReportLog);
        assertEquals(30L, listReportLog.getTotalValid());
        assertEquals(40, listReportLog.getReportLogInfoList().size());
    }

    @Test
    public void verifyTotalError() throws Exception {
        ListReportLog listReportLog = new ListReportLog(null);
        assertNotNull(listReportLog);
        assertNull(listReportLog.getReportLogInfoList());
        assertEquals(0L, listReportLog.getTotalValid());
    }

    private List<ReportLogInfo> createReportLog(ReportLogStatus reportLogStatus) {
        List<ReportLogInfo> reportLogInfoList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ReportLogInfo reportLogInfo = new ReportLogInfo();
            reportLogInfo.setId(new Long(i));
            reportLogInfo.setStatus(reportLogStatus);
            reportLogInfo.setType("testType");
            reportLogInfo.setName("nameTest");
            reportLogInfo.setName("nameTest");
            reportLogInfo.setChecked(false);
            reportLogInfo.setMaxDuration(100L);
            reportLogInfo.setFiltersDefinition("{}");
            reportLogInfoList.add(reportLogInfo);
        }
        return reportLogInfoList;
    }
}