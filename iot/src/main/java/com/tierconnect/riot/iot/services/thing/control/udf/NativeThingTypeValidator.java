package com.tierconnect.riot.iot.services.thing.control.udf;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.ValidationBean;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.iot.services.ThingsService;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Map;

/**
 * Created by julio.rocha on 24-08-17.
 */
public class NativeThingTypeValidator {
    private static Logger logger = Logger.getLogger(NativeThingTypeValidator.class);
    private static final NativeThingTypeValidator INSTANCE = new NativeThingTypeValidator();

    private NativeThingTypeValidator() {
    }

    public static NativeThingTypeValidator getInstance() {
        return INSTANCE;
    }

    /************************************
     * this method checks if the Native thing type Object is valid
     ************************************/
    public ValidationBean validationNativeThingType(ThingTypeField thingTypeField, String udfLabel, Map<String, Object> udfMap,
                                                    Group group, boolean validateVisibility) {
        ValidationBean response = new ValidationBean();

        try {
            Object udfValue = udfMap.get("value");//serial
            //Check Multivalue, Array of names
            if (udfValue instanceof String[]) {
                String[] udfValueName = (String[]) udfValue;
                if (udfValueName.length > 0) {
                    for (int i = 0; i < udfValueName.length; i++) {
                        Thing thing = ThingService.getInstance().getBySerialNumber(Arrays.toString(udfValueName), ThingTypeService.getInstance().get(
                                thingTypeField.getDataType().getId()));
                        if (thing == null) {
                            response.setErrorDescription("Native thing type property [" + udfLabel + "]. The value [" + Arrays.toString(udfValueName) + "] does not exist.");
                            break;
                        }
                    }
                }
                //Check single name
            } else if (udfValue != null && udfValue instanceof String) {
                Thing thing = ThingService.getInstance().getBySerialNumber(udfValue.toString().trim(),
                        ThingTypeService.getInstance().get(thingTypeField.getDataTypeThingTypeId()));
                if (thing == null) {
                    response.setErrorDescription("Native thing type property [" + udfLabel + "]. The value [" + udfValue + "] does not exist.");
                } else {
                    ValidationBean visibilityUdfValidation = ThingsService.getInstance().validationVisibilityUdf(thing, group);
                    if (validateVisibility && visibilityUdfValidation.isError()) {
                        response.setErrorDescription(
                                "Native thing type property [" + udfLabel + "]. The value [" + udfValue + "] wrong visibilities."
                                        + visibilityUdfValidation.getErrorDescription());
                    }
                }
            }

        } catch (NonUniqueResultException e) {
            logger.error(e);
            response.setErrorDescription("Non Unique Result in Udf:  " + udfLabel);
        } catch (Exception e) {
            logger.error(e);
            response.setErrorDescription("Error validating Udf:  " + udfLabel);
        }

        return response;
    }
}