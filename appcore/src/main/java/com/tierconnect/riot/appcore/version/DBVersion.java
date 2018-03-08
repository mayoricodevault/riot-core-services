package com.tierconnect.riot.appcore.version;

/**
 * Created by cvertiz on 01/09/17.
 */
public class DBVersion {

    private static DBVersion INSTANCE = new DBVersion();

    private int dbVersion = 0;
    private String dbVersionName;

    public static DBVersion getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DBVersion();
        }
        return INSTANCE;
    }

    public int getDbVersion() {
        return dbVersion;
    }

    public void setDbVersion(int dbVersion) {
        this.dbVersion = dbVersion;
    }

    public void setDbVersionName(String dbVersionName) {
        this.dbVersionName = dbVersionName;
    }

    public String getDbVersionName() {
        return dbVersionName;
    }
}
