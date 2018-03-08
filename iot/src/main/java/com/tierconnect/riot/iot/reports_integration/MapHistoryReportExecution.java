package com.tierconnect.riot.iot.reports_integration;

import com.tierconnect.riot.api.database.base.ordination.Order;
import com.tierconnect.riot.api.database.mongo.Mongo;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import org.apache.log4j.Logger;

import java.util.*;

import static com.tierconnect.riot.commons.Constants.TIME;

/**
 * Created by vealaro on 1/16/17.
 */
public class MapHistoryReportExecution extends ReportExecution {

    private static Logger logger = Logger.getLogger(MapHistoryReportExecution.class);

    public MapHistoryReportExecution(ReportConfig configuration, ITranslateResult translateResult, String comment) {
        super(configuration, translateResult, comment);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        try {

            long start = System.currentTimeMillis();
            Map<String, Map<String, Object>> uniqueRecords = new HashMap<>();
            getConfiguration().addSortTo(TIME, Order.DESC);
            super.dataBase = new Mongo(getConfiguration().getFilters());
            super.dataBase.executeFind(getConfiguration().getCollectionTarget(), getReportIndex(), getConfiguration().getProjection(),
                    getConfiguration().getSkip(), getConfiguration().getLimit(), getConfiguration().getSort(), null);
            count = super.dataBase.getCountAll();
            records = new ArrayList<>(super.dataBase.getResultSet().size());
            for (Map<String, Object> map : super.dataBase.getResultSet()) {
                translateResult.exportResult(map);
                Map<String, Object> labelValues = translateResult.getLabelValues();
                String serial = (String) labelValues.get(Constants.SERIAL);
                String location = (String) labelValues.get(Constants.LOCATION);
                if (!Utilities.isEmptyOrNull(location)) {
                    if (uniqueRecords.containsKey(serial)) {
                        Map<String, Object> uniqueRecord = uniqueRecords.get(serial);
                        // add locations
                        List<String> locations = (List<String>) uniqueRecord.get(Constants.LOCATION);
                        locations.add(location);
                        uniqueRecord.put(Constants.LOCATION, locations);

                        //get unique last style
                        List<List<Object>> styles = new ArrayList<>((List<List<Object>>) uniqueRecord.get("pinStyles"));
                        List<Object> lastUniqueStyle = styles.get(styles.size() - 1);
                        logger.trace("last: " + lastUniqueStyle);
                        // get current style
                        List<List<Object>> currentStyles = (List<List<Object>>) labelValues.get("pinStyles");
                        List<Object> currentUniqueStyle = currentStyles.get(currentStyles.size() - 1);
                        logger.trace("current: " + currentUniqueStyle);
                        // compare styles
                        logger.info(currentUniqueStyle.get(2) + ">" + lastUniqueStyle.get(2) + "=" + (((Long)
                                currentUniqueStyle.get(2)) > ((Long) lastUniqueStyle.get(2))));
                        boolean equalsStyle = lastUniqueStyle.get(0).equals(currentUniqueStyle.get(0))
                                && lastUniqueStyle.get(1).equals(currentUniqueStyle.get(1));
                        boolean greaterTimeStamp = ((Long) currentUniqueStyle.get(2)) < ((Long) lastUniqueStyle.get(2));

                        if (!equalsStyle && greaterTimeStamp) {
                            currentStyles.addAll(styles);
                            uniqueRecord.put("pinStyles", currentStyles);
                            logger.debug("styles " + uniqueRecord.get("pinStyles"));
                        }
                    } else {
                        List<String> locations = new ArrayList<>();
                        locations.add(location);
                        labelValues.put(Constants.LOCATION, locations);

                        if (!labelValues.containsKey("pinStyles")) {
                            List<Object> pinStyle = new LinkedList<>();
                            pinStyle.add(getConfiguration().reportDefinition.getDefaultColorIcon());
                            pinStyle.add(getConfiguration().reportDefinition.getDefaultTypeIcon());
                            pinStyle.add(getConfiguration().startDate != null ? getConfiguration().startDate.getTime
                                    () : 0);
                            labelValues.put("pinStyles", Arrays.asList(pinStyle));
                        }
                        uniqueRecords.put(serial, labelValues);
                    }
                }
            }
            buildResultWithGeoJson(uniqueRecords);
            long end = System.currentTimeMillis();
            long duration = end - start;
            buildReportLog(start, end, duration);
            logger.info("Records Size " + count + " in " + duration + " ms");
        } catch (Exception e) {
            logger.error("Error in execution find to Mongo", e);
        }
    }

    private void buildResultWithGeoJson(Map<String, Map<String, Object>> uniqueRecords) {
        for (Map.Entry<String, Map<String, Object>> recordMap : uniqueRecords.entrySet()) {
            buildGeoJson(recordMap.getValue());
        }
        count = (long) records.size();
        super.buildResult();
        buildResultWithGeoJson();
    }
}
