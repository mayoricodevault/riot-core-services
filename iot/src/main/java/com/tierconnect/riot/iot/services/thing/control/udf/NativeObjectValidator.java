package com.tierconnect.riot.iot.services.thing.control.udf;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.ValidationBean;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.iot.services.ThingsService;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * Created by julio.rocha on 24-08-17.
 */
public class NativeObjectValidator {
    private static Logger logger = Logger.getLogger(NativeObjectValidator.class);
    private static final NativeObjectValidator INSTANCE = new NativeObjectValidator();

    private NativeObjectValidator() {
    }

    public static NativeObjectValidator getInstance() {
        return INSTANCE;
    }

    /************************************
     * this method checks if the Native Object is valid
     ************************************/
    public ValidationBean validationNativeObject(
            ThingTypeField thingTypeField,
            String udfLabel,
            Map<String, Object> udfMap,
            Group group,
            boolean validateVisibility,
            Date transactionDate) {
        ValidationBean response = new ValidationBean();

        try {
            Object udfValue = udfMap.get("value");
            //Check Multivalue, Array of names
            if (udfValue != null && udfValue instanceof String[]) {
                String[] udfValueName = (String[]) udfValue;
                if (udfValueName.length > 0) {
                    for (int i = 0; i < udfValueName.length; i++) {
                        Object nativeObject = ThingTypeFieldService.getInstance().getNativeObject(thingTypeField.getDataType().getId(),
                                Arrays.toString(udfValueName), null, group, transactionDate);
                        if (nativeObject == null) {
                            response.setErrorDescription("Native Object property [" + udfLabel + "]. The value [" + Arrays.toString(udfValueName) + "] does not exist.");
                            break;
                        } else {
                            ValidationBean visibilityUdfValidation = ThingsService.getInstance().validationVisibilityUdf(nativeObject, group);
                            if (validateVisibility && visibilityUdfValidation.isError()) {
                                response.setErrorDescription("Native Object property [" + udfLabel + "] The value [" + Arrays.toString(udfValueName)
                                        + "] Wrong visibility." + visibilityUdfValidation.getErrorDescription());
                            }
                        }
                    }
                }
                //Check single name
            } else if (udfValue != null && udfValue instanceof String) {
                Object nativeObject = ThingTypeFieldService.getInstance().getNativeObject(
                        thingTypeField.getDataType().getId(),
                        udfValue.toString(),
                        null, group,
                        transactionDate);
                if (nativeObject == null) {
                    response.setErrorDescription("Native Object property [" + udfLabel + "]. The value [" + udfValue + "] does not exist.");
                }
            }
        } catch (Exception e) {
            logger.error(e);
            response.setErrorDescription(e.getMessage());
        }
        return response;
    }
}
