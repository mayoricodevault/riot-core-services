package com.tierconnect.riot.iot.cassandra.dao;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;

import com.tierconnect.riot.appcore.dao.CassandraUtils;

import org.apache.log4j.Logger;

import java.util.*;

@SuppressWarnings("deprecation")
@Deprecated
public class FieldTypeHistoryDAO{
    static Logger logger = Logger.getLogger(FieldTypeHistoryDAO.class);

    // for all field value histories with in
    private  PreparedStatement historyInAllPreparedStatement;


    // field value history in between two dates
    private  PreparedStatement historyInBetweenPreparedStatement;

    public FieldTypeHistoryDAO() {
        historyInBetweenPreparedStatement = CassandraUtils.getSession().prepare("SELECT " +
                "field_type_id, " +
                "thing_id, time, value " +
                "FROM field_value_history2 " +
                "WHERE thing_id = :tId " +
                "AND field_type_id IN :ftIdx " +
                "AND time > :startTime " +
                "AND time < :endTime " +
                "ALLOW FILTERING");
        historyInAllPreparedStatement = CassandraUtils.getSession().prepare("SELECT " +
                "field_type_id, " +
                "thing_id, time, value " +
                "FROM field_value_history2 " +
                "WHERE thing_id = :tId " +
                "AND field_type_id IN :ftIdx;");
    }


    /**
     * Used by mongo migrator Method to get all history or between dates.
     *
     * @param thingId      Thing id to get history.
     * @param fieldTypeIds thing types ids to get history.
     * @param historyInAllPreStatement Statement to get all thing history.
     * @param historyInBetweenPreStatement Statement to get between dates thing history.
     * @param startDate    start date to get history.
     * @param endDate      end date to get history.
     * @return A map with all history of thing between dates.
     * @throws RuntimeException
     */
    private static Map<Long, List<Map<String, Object>>> getHistory(Long thingId,
                                                                  List<Long> fieldTypeIds,
                                                                  PreparedStatement historyInAllPreStatement,
                                                                  PreparedStatement historyInBetweenPreStatement,
                                                                  Date startDate,
                                                                  Date endDate
                                                                  ) throws RuntimeException {
        Map<Long, List<Map<String, Object>>> values = new HashMap<>();

        if (fieldTypeIds != null && !fieldTypeIds.isEmpty() && thingId != null) {

            //Query Helper
            QueryRequestHandler qh = new QueryRequestHandler() {

                Map<Long, List<Map<String, Object>>> fieldHistories = new HashMap<>();

                @Override
                public void handleRow(Row row) {
                    // get list of values for each id. If empty list initialize
                    Long fieldTypeId = row.getLong("field_type_id");
                    Date time = row.getDate("time");
                    String value = row.getString("value");
                    Map<String, Object> aRow = new HashMap<>();
                    aRow.put("value", value);
                    aRow.put("time", time);

                    if (!fieldHistories.containsKey(fieldTypeId)) {
                        List<Map<String, Object>> data = new ArrayList<>();
                        data.add(aRow);
                        fieldHistories.put(fieldTypeId, data);
                    } else {
                        fieldHistories.get(fieldTypeId).add(aRow);
                    }
                }

                @Override
                public Object results() {
                    return fieldHistories;
                }
            };

            //Choose query
            BoundStatement bs;

            if (endDate != null && startDate != null) {
                bs = new BoundStatement(historyInBetweenPreStatement);
                bs.setDate("startTime", startDate);
                bs.setDate("endTime", endDate);

            } else {
                bs = new BoundStatement(historyInAllPreStatement);
            }
            bs.setList("ftIdx", fieldTypeIds);
            bs.setLong("tId", thingId);
            //noinspection unchecked
            values.putAll((Map<Long, List<Map<String, Object>>>) qh.execute(bs).results());
            return values;
        } else {
            logger.info("Query with 0 list-length skipped");
            return values;
        }
    }


    /**
     * Used by mongo migrator Method to get all history or between dates.
     *
     * @param thingId      Thing id to get history.
     * @param fieldTypeIds thing types ids to get history.
     * @param startDate    start date to get history.
     * @param endDate      end date to get history.
     * @return a map with all history of thing between dates.
     */
    public Map<Long, List<Map<String, Object>>> getHistory(Long thingId,
                                                           List<Long> fieldTypeIds,
                                                           Date startDate,
                                                           Date endDate
    ) throws RuntimeException {
        return getHistory(thingId, fieldTypeIds, historyInAllPreparedStatement, historyInBetweenPreparedStatement,
                startDate, endDate);
    }

    /**
     * Helper class that handles a query request
     */
    private static abstract class QueryRequestHandler{
        public QueryRequestHandler execute(BoundStatement bs) throws RuntimeException{

            logger.info("cql " + bs.preparedStatement().getQueryString());
            long t1 = System.currentTimeMillis();
            ResultSet rs = CassandraUtils.getSession().execute(bs);
            long t2 = System.currentTimeMillis();
            logger.info("cassandra query execution_time=" + (t2 - t1));

            t1 = System.currentTimeMillis();
            int count = 0;
            for(Row row : rs){
                handleRow(row);
                count++;
            }
            t2 = System.currentTimeMillis();
            logger.info("resultSet.size=" + count);
            logger.info("post processing execution_time=" + (t2 - t1));
            return this;
        }

        /**
         * @param row returned from query
         */
        public abstract void handleRow(Row row);

        /**
         * @return data structure that holds the request
         */
        public abstract Object results();
    }

    /**
     * Gets Cassandra fields by historical values or current values.
     *
     * @param thingId                 Thing correlative Id.
     * @param thingSerial             Thing serial.
     * @param ListFieldTypeTimeSeries Time series fields List
     * @param ListThingsFailed        Failed things list
     * @param startDate               Init date to migration the fields of thing.
     * @param endDate                 End date to migration the fields of thing.
     * @param retries                 Number of retries to get  the fields data of a thing.
     * @return A HashMap of historical  fields or current fields of a thing..
     */
    public Object getFieldValueHistory(Long thingId, String thingSerial,
                                       List<Long> ListFieldTypeTimeSeries,
                                       List<Long> ListThingsFailed,
                                       Date startDate,
                                       Date endDate,
                                       int retries) {
        int retriesGetField = 0;
        boolean error = true;
        Map<Long, List<Map<String, Object>>> fieldTypeTimeSeries = new HashMap<>();

        while (error && (retriesGetField <= retries)) {
            try {
                error = false;
                fieldTypeTimeSeries.putAll(getHistory(thingId, ListFieldTypeTimeSeries, startDate, endDate));

            } catch (RuntimeException e) {
                error = true;
                if (retriesGetField == retries) {
                    if (!ListThingsFailed.contains(thingId)) {
                        ListThingsFailed.add(thingId);
                    }
                    logger.error("****** Error in processing field_value_history after " + retries + "" +
                            " retries for thing : " + thingSerial, e);
                } else {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException intEx) {
                        intEx.printStackTrace();
                    }
                }
            } finally {
                retriesGetField++;
            }
        }
        return fieldTypeTimeSeries;
    }
}