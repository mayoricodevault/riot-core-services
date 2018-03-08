package com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs;

import com.mongodb.BasicDBObject;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import org.bson.types.ObjectId;

import java.util.Date;


/**
 * Created by achambi on 4/3/17.
 * Class with contains the report log detail.
 */
public class ReportLogDetail extends BasicDBObject {

    private static final String START = "start";
    private static final String END = "end";
    private static final String DURATION = "duration";
    private static final String QUERY = "query";
    private static final String COUNT = "count";
    private static final String ID = "id";
    private static final String USER_ID = "userID";
    private static final String DATE = "date";
    private static final String ADDITIONAL_INFO = "additionalInfo";

    private DateFormatAndTimeZone dateFormatAndTimeZone;

    public ReportLogDetail() {
    }

    ReportLogDetail(BasicDBObject reportLogDetail,DateFormatAndTimeZone dateFormatAndTimeZone) {
        this.dateFormatAndTimeZone = dateFormatAndTimeZone;
        setStart(reportLogDetail.getLong(START));
        setEnd(reportLogDetail.getLong(END));
        setDuration(reportLogDetail.getLong(DURATION));
        setQuery(reportLogDetail.getString(QUERY));
        setCount(reportLogDetail.getLong(COUNT));
        setId(reportLogDetail.getObjectId(ID));
        setUserId(reportLogDetail.getLong(USER_ID));
        setDate(reportLogDetail.getDate(DATE));
        Object additionalInfo = reportLogDetail.get(ADDITIONAL_INFO);
        if (additionalInfo instanceof BasicDBObject) {
            setAdditionalInfo(new AdditionalInfo((BasicDBObject) additionalInfo));
        }
    }

    ReportLogDetail(BasicDBObject reportLogDetail) {
        this(reportLogDetail, null);
    }

    public long getStart() {
        return getLong(START);
    }

    public long getEnd() {
        return getLong(END);
    }

    public long getDuration() {
        return getLong(DURATION);
    }

    public String getQuery() {
        return getString(QUERY);
    }

    public long getCount() {
        return getLong(COUNT);
    }

    public ObjectId getId() {
        return getObjectId(ID);
    }

    public long getUserId() {
        return getLong(USER_ID);
    }

    public Date getDate() {
        return getDate(DATE);
    }

    public AdditionalInfo getAdditionalInfo() {
        return (AdditionalInfo) get(ADDITIONAL_INFO);
    }

    public void setStart(long start) {
        put(START, start);
    }

    public void setEnd(long end) {
        put(END, end);
    }

    public void setDuration(long duration) {
        put(DURATION, duration);
    }

    public void setQuery(String query) {
        put(QUERY, query);
    }

    public void setCount(long count) {
        put(COUNT, count);
    }

    public void setId(ObjectId id) {
        put(ID, id);
    }

    public void setUserId(long userId) {
        put(USER_ID, userId);
    }

    public void setDate(Date date) {
        put(DATE, dateFormatAndTimeZone != null ? dateFormatAndTimeZone.format(date) : date);
    }

    public void setAdditionalInfo(AdditionalInfo additionalInfo) {
        put(ADDITIONAL_INFO, additionalInfo);
    }
}
