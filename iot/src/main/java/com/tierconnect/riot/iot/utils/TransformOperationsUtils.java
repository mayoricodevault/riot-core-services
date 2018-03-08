package com.tierconnect.riot.iot.utils;

import org.apache.log4j.Logger;

/**
 * Created by rsejas on 3/21/17.
 */
public class TransformOperationsUtils {
    private static Logger logger = Logger.getLogger(TransformOperationsUtils.class);

    private static TransformOperationsUtils instance = null;

    public static TransformOperationsUtils getInstance() {
        if (instance == null) {
            instance = new TransformOperationsUtils();
        }
        return instance;
    }


}
