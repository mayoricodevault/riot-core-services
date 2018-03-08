package com.tierconnect.riot.iot.services.thing.control.udf;

/**
 * Created by julio.rocha on 24-08-17.
 */
public class UdfValidator {
    private static final UdfValidator INSTANCE = new UdfValidator();

    private UdfValidator() {
    }

    public static UdfValidator getInstance() {
        return INSTANCE;
    }

    //TODO replace method ThingsService#validationUdfs
}
