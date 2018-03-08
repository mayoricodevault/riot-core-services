package com.tierconnect.riot.iot.utils;

import com.tierconnect.riot.iot.entities.ValidationBean;

/**
 * Created by rsejas on 1/13/17.
 */
public class ValidationUtils {
    static private ValidationUtils instance = new ValidationUtils();

    public static ValidationUtils getInstance() {
        return instance;
    }

    public ValidationBean newValidationError(String message) {
        ValidationBean validationBean = new ValidationBean();
        validationBean.setErrorDescription(message);
        return validationBean;
    }

    public ValidationBean newValidationOk() {
        return new ValidationBean();
    }
}
