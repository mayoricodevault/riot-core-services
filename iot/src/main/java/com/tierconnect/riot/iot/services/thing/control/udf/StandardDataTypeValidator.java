package com.tierconnect.riot.iot.services.thing.control.udf;

import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.utils.DateTimeFormatterHelper;
import com.tierconnect.riot.iot.entities.DataType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.ValidationBean;
import com.tierconnect.riot.iot.services.AppLocationAdapter;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingsService;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

/**
 * Created by julio.rocha on 24-08-17.
 */
public class StandardDataTypeValidator {
    private static Logger logger = Logger.getLogger(StandardDataTypeValidator.class);
    private static final StandardDataTypeValidator INSTANCE = new StandardDataTypeValidator();

    private StandardDataTypeValidator() {
    }

    public static StandardDataTypeValidator getInstance() {
        return INSTANCE;
    }

    /**
     * This method checks if the Standard data type Object is valid
     *
     * @param thingTypeField field of thing type
     * @param udfLabel       label
     * @param udfMap         udf's
     * @return Object with error description
     */
    public ValidationBean validationStandardDataTypes(ThingTypeField thingTypeField, String udfLabel, Map<String, Object> udfMap) {
        ValidationBean response = new ValidationBean(false);

        try {
            Object udfValue = udfMap.get("value");
            //Check Multivalue, Array of names
            if (udfValue instanceof String[]) {
                String[] udfValueName = (String[]) udfValue;
                if (udfValueName.length > 0) {
                    for (String anUdfValueName : udfValueName) {
                        ValidationBean standardValidDataType = this.isStandardValidDataType(thingTypeField.getDataType(), anUdfValueName);
                        if (standardValidDataType.isError()) {
                            response.setErrorDescription("Standard data type property [" + udfLabel + "]. The value ["
                                    + Arrays.toString(udfValueName) + "] does not correspond to the type of data.");
                            if (!Utilities.isEmptyOrNull(standardValidDataType.getErrorDescription())) {
                                response.setErrorDescription(standardValidDataType.getErrorDescription());
                            }
                            break;
                        }
                    }
                }
                //Check single name
            } else {
                ValidationBean standardValidDataType = this.isStandardValidDataType(thingTypeField.getDataType(), udfValue);
                if (standardValidDataType.isError()) {
                    response.setErrorDescription(
                            "Standard data type property [" + udfLabel + "]. The value ["
                                    + udfValue + "] does not correspond to the type of data '" + thingTypeField.getDataType().getClazz() + "'.");
                    if (!Utilities.isEmptyOrNull(standardValidDataType.getErrorDescription())) {
                        response.setErrorDescription(standardValidDataType.getErrorDescription());
                    }
                } else {
                    if (thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_ATTACHMENTS.value) == 0 &&
                            !ThingsService.getInstance().isJSONValid(udfValue)) {
                        response.setErrorDescription("Standard data type property [" + udfLabel + "]. The value [" + udfValue
                                + "] does not have a correct format.");
                    } else if ((com.tierconnect.riot.commons.DataType.XYZ.equals(thingTypeField.getDataType().getCode()) ||
                            com.tierconnect.riot.commons.DataType.COORDINATES.equals(thingTypeField.getDataType().getCode())) &&
                            !AppLocationAdapter.isValidLocation(udfMap, thingTypeField.getDataType().getCode())) {
                        if (com.tierconnect.riot.commons.DataType.XYZ.equals(thingTypeField.getDataType().getCode())) {
                            response.setErrorDescription(udfLabel + ": '" + udfValue + "' is invalid, the correct format is: " +
                                    "X;Y;Z with number data, please check it and try again.");
                        } else {
                            response.setErrorDescription(udfLabel + ": '" + udfValue + "' is invalid, the correct format is: " +
                                    "Longitude;Latitude;Altitude with number data, please check it and try again.");
                        }
                    }
                }
            }
        } catch (Exception e) {
            response.setErrorDescription(e.getMessage());
        }
        return response;
    }

    /**
     * Method to check if the standard data type is valid or not
     */
    public ValidationBean isStandardValidDataType(DataType type, Object value) {
        ValidationBean validationBean = new ValidationBean(false);
        if (value != null && !Utilities.isEmptyOrNull(value.toString())) {
            Object valueType = null;
            if (ThingTypeField.Type.isDateOrTimestamp(type.getId())) {
                try {
                    if (value.toString().matches("-?\\d+(\\.\\d+)?")) {
                        valueType = new Date(Long.parseLong(value.toString()));
                    } else {
                        valueType = DateTimeFormatterHelper.parseDateTextAndDetermineFormat(Utilities.removeSpaces(value.toString()));
                    }
                } catch (Exception e) {
                    logger.error("Error in validation standard data type: " + e.getMessage() + value);
                    validationBean.setErrorDescription(e.getMessage());
                }
            } else {
                valueType = ThingService.getInstance().getStandardDataType(type, value);
            }
            validationBean.setError(valueType == null);
        }
        return validationBean;
    }
}
