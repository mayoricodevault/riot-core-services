package com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by achambi on 8/4/17.
 * Class for return a list of ReportLogInfo.
 */
public class ListReportLog {

    private long totalValid;
    private List<Map<String, Object>> reportLogInfoList;

    /**
     * Default constructor.
     *
     * @param reportLogInfoList a instance of {@link List}<{@link ReportLogInfo}>.
     */
    public ListReportLog(List<ReportLogInfo> reportLogInfoList) {
        if (reportLogInfoList != null){
            this.reportLogInfoList = reportLogInfoList.stream().map(ReportLogInfo::getMap).collect(Collectors.toList());
            this.totalValid = reportLogInfoList.stream()
                    .filter(reportLogInfo -> reportLogInfo.getStatus() != ReportLogStatus.CANCELED)
                    .count();
        }else{
            this.reportLogInfoList = null;
            this.totalValid = 0L;
        }
    }

    long getTotalValid() {
        return totalValid;
    }

    List<Map<String, Object>> getReportLogInfoList() {
        return reportLogInfoList;
    }

    public Map<String, Object> getMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("totalValid", this.totalValid);
        map.put("data", this.reportLogInfoList);
        return map;
    }
}
