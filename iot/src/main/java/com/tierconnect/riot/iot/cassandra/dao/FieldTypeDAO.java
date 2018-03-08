package com.tierconnect.riot.iot.cassandra.dao;

import java.util.*;

import com.datastax.driver.core.ResultSet;
import org.apache.log4j.Logger;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.tierconnect.riot.appcore.dao.CassandraUtils;

/**
 * Created by pablo on 5/14/15.
 * <p/>
 * DAO for table field_type.
 */
@SuppressWarnings("deprecation")
@Deprecated
public class FieldTypeDAO {
    Logger logger;

    PreparedStatement psInsert;

    PreparedStatement psValuesIn;

    public FieldTypeDAO() {
        logger = Logger.getLogger(FieldTypeDAO.class);
        psInsert = CassandraUtils.getSession().prepare(
                "INSERT INTO field_type (field_type_id, thing_id, time, value) VALUES (?, ?, ?, ?) ;");
        psValuesIn = CassandraUtils.getSession().prepare("select field_type_id, thing_id, value, time, writetime" +
                "(value) as write_time from field_type where thing_id in :tIdx and field_type_id in :ftIdx ALLOW " +
                "FILTERING;");
    }

    /**
     * Used by cassandra migrator
     *
     * @param thingId The thing ID
     * @param fieldTypeId Field Id
     * @param time time to set
     * @param value a value to set
     */
    public void insert(long thingId, long fieldTypeId, Date time, String value) {
        try {
            CassandraUtils.getSession().executeAsync(psInsert.bind(fieldTypeId, thingId, time, value));
        } catch (RuntimeException e) {
            logger.error("Error ", e);
        }
    }

    /**
     * Used by mongo migrator
     *
     * @param thingId The Thing Id
     * @param thingTypeFields the thing types Ids to get on cassandra.
     * @return a Map of thing fields.
     */
    public Map<Long, Map<String, Object>> valuesMap(Long thingId, List<Long> thingTypeFields) throws RuntimeException {
        List<Long> thingIds = new ArrayList<>();
        thingIds.add(thingId);

        Map<Long, Map<Long, Map<String, Object>>> values = valuesMap(thingIds, thingTypeFields);

        if (values.keySet().size() > 0) {
            return values.get(thingId);
        } else {
            return new HashMap<>();
        }
    }


    /**
     * Used by mongo migrator (indirect form)
     *
     * @param thingIds A thing Ids
     * @param thingTypeFields the thin values.
     * @return A map of thing Ids.
     */
    public Map<Long, Map<Long, Map<String, Object>>> valuesMap(List<Long> thingIds, List<Long> thingTypeFields) throws
            RuntimeException {
        Map<Long, Map<Long, Map<String, Object>>> values = new HashMap<>();
        return  getFieldsValuesForMapObject(thingIds, thingTypeFields, values);
    }

    /**
     * Used by mongo migrator (indirect form)
     *
     * @param thingIds        helper method for recursive method call
     * @param thingTypeFields field ids
     * @param valuesIn        values already collected
     * @return new values based on fields
     */
    private Map<Long, Map<Long, Map<String, Object>>> getFieldsValuesForMapObject(List<Long> thingIds,
                                                                                  List<Long> thingTypeFields,
                                                                                  Map<Long, Map<Long, Map<String,
                                                                                          Object>>> valuesIn)
            throws RuntimeException {
        // slice
        int fetchSize = 5000;
        int maxCollectionProtocol = 65535;
        if (thingTypeFields.size() > maxCollectionProtocol) {
            BoundStatement bs = new BoundStatement(psValuesIn);
            bs.setFetchSize(fetchSize);
            bs.setList("tIdx", thingIds.subList(0, maxCollectionProtocol));
            bs.setList("ftIdx", thingTypeFields);
            valuesIn.putAll(getFieldsValuesForMapObject(thingIds.subList(maxCollectionProtocol, thingIds.size()),
                    thingTypeFields, executeBoundStatement(bs)));
            return valuesIn;
        } else {
            BoundStatement bs = new BoundStatement(psValuesIn);
            bs.setFetchSize(fetchSize);
            bs.setList("tIdx", thingIds);
            bs.setList("ftIdx", thingTypeFields);
            return executeBoundStatement(bs);
        }
    }

    /**
     * Used by mongo migration (indirect form)
     *
     * @param bs
     * @return
     */
    /**
     * Used by mongo migrator (indirect form)
     * @param bs Bound Statement.
     * @return a map of thing fields.
     * @throws RuntimeException
     */
    private Map<Long, Map<Long, Map<String, Object>>> executeBoundStatement(BoundStatement bs) throws
            RuntimeException {
        Map<Long, Map<Long, Map<String, Object>>> values = new HashMap<>();
        ResultSet result  = CassandraUtils.getSession().execute(bs);
        for (Row row : result) {
            Long thingId = row.getLong("thing_id");
            Long fieldTypeId = row.getLong("field_type_id");
            String value = row.getString("value");
            Date time = row.getDate("time");
            Long writeTime = row.getLong("write_time");

            Map<String, Object> aRow = new HashMap<>();
            aRow.put("value", value);
            aRow.put("time", time);
            aRow.put("write_time", writeTime);

            if (!values.containsKey(thingId)) {
                Map<Long, Map<String, Object>> thingTypeField = new HashMap<>();
                thingTypeField.put(fieldTypeId, aRow);
                values.put(thingId, thingTypeField);

            } else {
                values.get(thingId).put(fieldTypeId, aRow);
            }

        }
        return values;
    }

    /**
     * Gets Cassandra fields by historical values or current values.
     *
     * @param thingId                   Thing to get values.
     * @param thingSerial               Thing Serial.
     * @param ListFieldTypeNoTimeSeries Fields list which are not time series.
     * @param ListThingsFailed          Failed things list
     * @param retries                   Number of retries to get  the fields data of a thing.
     * @return A HashMap of historical  fields or current fields of a thing..
     */
    public Map<Long, Map<String, Object>> getFieldValue(Long thingId,
                                String thingSerial,
                                List<Long> ListFieldTypeNoTimeSeries,
                                List<Long> ListThingsFailed,
                                int retries) {
        int retriesGetField = 0;
        boolean error = true;
        Map<Long, Map<String, Object>> fieldTypeNoTimeSeries = new HashMap<>();

        while (error && (retriesGetField <= retries)) {
            try {
                error = false;
                fieldTypeNoTimeSeries.putAll(valuesMap(thingId, ListFieldTypeNoTimeSeries));
            } catch (RuntimeException e) {
                error = true;
                if (retriesGetField == retries) {
                    if (!ListThingsFailed.contains(thingId)) {
                        ListThingsFailed.add(thingId);
                    }
                    logger.error("****** Error in processing field_value or field_value_history after " + retries + "" +
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
        return fieldTypeNoTimeSeries;
    }

    /**
     *
     * @param thingList List of Things to get values.
     * @param ListFieldTypeNoTimeSeries Fields list which are not time series.
     * @param ListThingsFailed          Failed things list
     * @param retries                   Number of retries to get  the fields data of a thing.
     * @return A HashMap of current fields of a things lot.
     */
    public List<Map<Long, Map<String, Object>>> bulkGetFieldValue(List<Long> thingList,
                                                                       List<Long> ListFieldTypeNoTimeSeries,
                                                                       List<Long> ListThingsFailed,
                                                                       int retries) {
        int retriesGetField = 0;
        boolean error = true;
        List<Map<Long, Map<String, Object>>> listFieldsTypeNoTimeSeries = new ArrayList<>();

        while (error && (retriesGetField <= retries)) {
            try {
                error = false;
                Map<Long, Map<Long, Map<String, Object>>> ascSortedMap = new TreeMap<>();
                ascSortedMap.putAll(valuesMap(thingList, ListFieldTypeNoTimeSeries));
                listFieldsTypeNoTimeSeries.addAll(ascSortedMap.values());
            } catch (RuntimeException e) {
                error = true;
                if (retriesGetField == retries) {
                    if (!ListThingsFailed.containsAll(thingList)) {
                        ListThingsFailed.addAll(thingList);
                    }
                    logger.error("****** Error in processing field_value or field_value_history after " + retries + "" +
                            " retries for thing : " + thingList.toString(), e);
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
        return listFieldsTypeNoTimeSeries;
    }
}
