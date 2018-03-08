package com.tierconnect.riot.appcore.version;

/**
 * Created by cvertiz on 01/09/17.
 */
public class CodeVersion {

    private static CodeVersion INSTANCE = new CodeVersion();

    private int codeVersion = 0;
    private String codeVersionName;

    public static CodeVersion getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CodeVersion();
        }
        return INSTANCE;
    }

    public int getCodeVersion() {
        return codeVersion;
    }

    public void setCodeVersion(int codeVersion) {
        this.codeVersion = codeVersion;
    }

    public void setCodeVersionName(String codeVersionName) {
        this.codeVersionName = codeVersionName;
    }

    public String getCodeVersionName() {
        return codeVersionName;
    }
}
