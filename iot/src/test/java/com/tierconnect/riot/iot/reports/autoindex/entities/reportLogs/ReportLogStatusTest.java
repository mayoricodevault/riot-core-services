package com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by achambi on 5/9/17.
 * Status enum of report log.
 */
public class ReportLogStatusTest {

    @Test
    public void getValue() throws Exception {
        assertEquals("inProgress", ReportLogStatus.IN_PROGRESS.getValue());
        assertEquals("pending", ReportLogStatus.PENDING.getValue());
        assertEquals("completed", ReportLogStatus.COMPLETED.getValue());
        assertEquals("slowIndex", ReportLogStatus.SLOW_INDEX.getValue());
        assertEquals("canceled", ReportLogStatus.CANCELED.getValue());
        assertEquals("deleted", ReportLogStatus.DELETED.getValue());
        assertEquals("error", ReportLogStatus.ERROR.getValue());
    }

    @Test
    public void getLabel() throws Exception {
        assertEquals("In Progress", ReportLogStatus.IN_PROGRESS.getLabel());
        assertEquals("Pending", ReportLogStatus.PENDING.getLabel());
        assertEquals("Completed", ReportLogStatus.COMPLETED.getLabel());
        assertEquals("Slow Index", ReportLogStatus.SLOW_INDEX.getLabel());
        assertEquals("Canceled", ReportLogStatus.CANCELED.getLabel());
        assertEquals("Deleted", ReportLogStatus.DELETED.getLabel());
        assertEquals("Error", ReportLogStatus.ERROR.getLabel());
    }
}