package com.tierconnect.riot.iot.entities;

/**
 * Created by rchirinos on 11/5/2015.
 */
public class ValidationBean {
    private boolean isError;
    private String errorDescription;

    public ValidationBean() {
        this.isError = false;
    }

    public ValidationBean(boolean isError) {
        this.isError = isError;
    }

    public boolean isError() {
        return isError;
    }

    @Deprecated
    public void setError(boolean isError) {
        this.isError = isError;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
        this.isError = true;
    }
}
