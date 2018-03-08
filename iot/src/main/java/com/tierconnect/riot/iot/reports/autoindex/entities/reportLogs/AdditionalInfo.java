package com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs;

import com.mongodb.BasicDBObject;

/**
 * Created by achambi on 4/3/17.
 * Class for save Additional Information.
 */
public class AdditionalInfo extends BasicDBObject {

    private static final String HEADER = "header";
    private static final String QUERY_STRING = "queryString";
    private static final String BODY = "body";

    public AdditionalInfo() {

    }

    public AdditionalInfo(BasicDBObject additionalInfo) {
        Object header = additionalInfo.get(HEADER);
        if (header instanceof BasicDBObject) {
            setHeader(new Header((BasicDBObject) header));
        }
        setQueryString(additionalInfo.getString(QUERY_STRING));
        Object body = additionalInfo.get(BODY);
        if (body instanceof BasicDBObject) {
            setBody((BasicDBObject) body);
        }
    }

    public Header getHeader() {
        return (Header) get(HEADER);
    }

    public String getQueryString() {
        return getString(QUERY_STRING);
    }

    public BasicDBObject getBody() {
        return (BasicDBObject) get(BODY);
    }


    public void setHeader(Header header) {
        put(HEADER, header);
    }

    public void setQueryString(String queryString) {
        put(QUERY_STRING, queryString);
    }

    public void setBody(BasicDBObject body) {
        put(BODY, body);
    }
}
