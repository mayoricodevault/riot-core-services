package com.tierconnect.riot.iot.utils;

import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.entities.ReportRule;
import com.tierconnect.riot.iot.services.ReportRuleService;

import java.util.*;

/**
 * Created by user on 11/26/14.
 */
public class BuildChart {

    private Map<String, Object> pieChart;
    private String title;
    private String yAxis;
    private String labelX;
    private String labelY;
    private List<String> categories;
    private List<Object> seriesList;

    public BuildChart(String title, String labelX, String labelY) {
        this.title = title;
        this.yAxis = labelY;
        this.labelX = labelX;
        this.labelY = labelY;
        this.categories = new LinkedList<>();
        this.seriesList = new LinkedList<>();
        this.pieChart = new HashMap<>();
    }

    public BuildChart(String title, String yAxis) {
        this.title = title;
        this.yAxis = yAxis;
        this.categories = new LinkedList<>();
        this.seriesList = new LinkedList<>();
        this.pieChart = new HashMap<>();
    }

    public void setCategories(List<String> categories) {
        for(String category : categories) {
            this.categories.add(category.replace("{", "").replace("}",""));
        }
    }

    public void setSeriesList(List<Object> seriesList) {
        this.seriesList = seriesList;
    }

    public void addPieChart(String title, Map<String, Integer> pieData) {
        this.pieChart.put("type", "pie");
        this.pieChart.put("name", title);
        this.pieChart.put("center", new Integer[]{ 1000, 80 });
        this.pieChart.put("size", 140);

        List<Object> pieList = new LinkedList<>();
        for(Map.Entry<String, Integer> pieItem : pieData.entrySet()) {
            Map<String, Object> seriesItem = new HashMap<>();
            seriesItem.put("name", pieItem.getKey());
            seriesItem.put("y", pieItem.getValue());
            pieList.add(seriesItem);
        }
        this.pieChart.put("data", pieList);
        seriesList.add(this.pieChart);
    }

    public void addSeriesData( Map<String, Object[] > seriesData,
                               String dataChartType,
                               int numberOfIntervals,
                               int totalCountForAverage,
                               ReportDefinition reportDefinition) {
        Double[] averageList = new Double[numberOfIntervals];
        Arrays.fill(averageList, 0.0);

        if (reportDefinition != null) {
            for (Map.Entry<String, Object[]> entry : seriesData.entrySet()) {
                Map<String, Object> seriesItem = new HashMap<>();

                seriesItem.put("type", dataChartType);
                seriesItem.put("name", entry.getKey());
                seriesItem.put("data", entry.getValue());

                String[] colors = new String[entry.getValue().length];
                Arrays.fill(colors, reportDefinition.getDefaultColorIcon());

                //Applying rules
                if (reportDefinition.getReportRule() != null) {
                    boolean stopRules = false;
                    for (ReportRule reportRule : reportDefinition.getReportRule()) {
                        if ((reportRule.getPropertyName() != null && reportRule.getPropertyName().equals(entry.getKey()) ||
                                reportRule.getPropertyName().isEmpty())) {
                            Object[] values = entry.getValue();
                            for (int it = 0; values != null && it < values.length; it++) {
                                String newColor = ReportRuleService.applyRuleForGroupBy(reportRule, reportDefinition, values[it]);
                                if (!newColor.equals(reportDefinition.getDefaultColorIcon())) {
                                    colors[it] = newColor;
                                    if (reportRule.getStopRules()) {
                                        stopRules = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (stopRules) break;
                    }
                }

                seriesItem.put("colors", colors);

                Object[] averageData = entry.getValue();
                for (int it = 0; it < numberOfIntervals && it < averageData.length; it++) {
                    Double value = null;
                    try {
                        value = Double.valueOf(averageData[it].toString());
                        averageList[it] += Double.valueOf(averageData[it].toString());
                    } catch (Exception numberErr) {
                        //
                    }
                }
                this.seriesList.add(seriesItem);
            }
            for (int it = 0; it < numberOfIntervals; it++) {
                averageList[it] = averageList[it] / totalCountForAverage;
            }
            //Adding averageData
            //Adding average
            if (reportDefinition.getReportType().equals("timeSeries")) {
                Map<String, Object> averageChart = new HashMap<>();
                averageChart.put("type", "spline");
                averageChart.put("name", "All: Average");
                averageChart.put("data", averageList);
                Map<String, Object> styleMarker = new HashMap<>();
                styleMarker.put("lineWidth", 2);
                styleMarker.put("lineColor", "Highcharts.getOptions().colors[3]");
                styleMarker.put("fillColor", "fillColor");
                averageChart.put("marker", styleMarker);

                this.seriesList.add(averageChart);
            }
        }
    }

    public Map<String, Object> getChartMap() {
        Map<String, Object> chartResMap = new HashMap<>();
        chartResMap.put("title", this.title);
        chartResMap.put("labelY", this.labelX);
        chartResMap.put("labelX", this.labelY);
        chartResMap.put("xAxis", this.categories);
        chartResMap.put("yAxis", this.yAxis);
        chartResMap.put("series", this.seriesList);
        return chartResMap;
    }
}
