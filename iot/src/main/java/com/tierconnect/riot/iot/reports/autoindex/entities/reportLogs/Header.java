package com.tierconnect.riot.iot.reports.autoindex.entities.reportLogs;

import com.mongodb.BasicDBObject;

/**
 * Created by achambi on 4/3/17.
 * Class for save Header information.
 */
public class Header extends BasicDBObject {

    private static final String ORIGIN = "origin";
    private static final String HOST = "host";
    private static final String UTC_OFFSET = "utcoffset";
    private static final String USER_AGENT = "user-agent";
    private static final String TOKEN = "token";


    public Header() {

    }

    public Header(BasicDBObject header) {
        setOrigin(header.getString(ORIGIN));
        setHost(header.getString(HOST));
        setUtcoffset(header.getString(UTC_OFFSET));
        setUserAgent(header.getString(USER_AGENT));
        setToken(header.getString(TOKEN));
    }

    public String getOrigin() {
        return getString(ORIGIN);
    }

    public String getHost() {
        return getString(HOST);
    }

    public String getUtcOffset() {
        return getString(UTC_OFFSET);
    }

    public String getUserAgent() {
        return getString(USER_AGENT);
    }

    public String getToken() {
        return getString(TOKEN);
    }

    public void setOrigin(String origin) {
        put(ORIGIN, origin);
    }

    public void setHost(String host) {
        put(HOST, host);
    }

    public void setUtcoffset(String utcoffset) {
        put(UTC_OFFSET, utcoffset);
    }

    public void setUserAgent(String userAgent) {
        put(USER_AGENT, userAgent);
    }

    public void setToken(String token) {
        put(TOKEN, token);
    }
}
