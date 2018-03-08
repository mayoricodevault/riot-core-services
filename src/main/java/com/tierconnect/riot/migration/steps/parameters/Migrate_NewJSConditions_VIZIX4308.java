package com.tierconnect.riot.migration.steps.parameters;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

/**
 * Created by vramos on 5/5/17.
 */
public class Migrate_NewJSConditions_VIZIX4308 implements MigrationStep {

    private static Logger logger = Logger.getLogger(Migrate_NewJSConditions_VIZIX4308.class);

    private static final String ALWAYS_TRUE_CONDITION_TYPE = "ALWAYS_TRUE";
    private static final String CEP_CONDITION_TYPE = "CEP";
    private static final String JS_CONDITION_TYPE = "JS";
    private static final String ALWAYS_TRUE_VALUE = "{\"group\":null,\"defaultCondition\":\"1=1\"}";
    private static final String CEP_VALUE = "{\"group\":\"@SYSTEM_PARAMETERS_CONDITION_GROUP_CEP\",\"defaultCondition\":\"1=1\"}";
    private static final String JS_RESOURCE_CODE = "@SYSTEM_PARAMETERS_CONDITION_TYPE_JS";
    private static final String JS_VALUE = "{\"group\":\"@SYSTEM_PARAMETERS_CONDITION_GROUP_FUNCTIONS\",\"defaultCondition\":\"function condition(thingWrapper,things,messages,logger){\\n\\tvar zoneCode = thingWrapper.getUdf(\\\"zone\\\");\\n\\tvar result = zoneCode === \\\"Stockroom\\\";\\n\\tlogger.info(\\\"JS condition zone=\\\"+zoneCode+\\\" result=\\\"+result);\\n\\treturn result;\\n}\"}";
    private static final String ROOT = "root";

    @Override
    public void migrateSQLBefore(String scriptPath)
            throws Exception {
    }

    @Override
    public void migrateHibernate()
            throws Exception {
        // Creates the new condition type: JS
        createParameters();

        // Updates the condition types: ALWAYS_TRUE and CEP
        updateParameters();
    }

    /**
     * Creates new condition type 'JS'.
     */
    private void createParameters() {
        Parameters parameter = null;
        try {
            parameter = ParametersService.getInstance().getByCategoryAndCode(Constants.CONDITION_TYPE, JS_CONDITION_TYPE);
        } catch (Exception exception) {
            logger.warn(exception.getMessage(), exception);
        }
        if (parameter == null) {
            parameter = new Parameters(Constants.CONDITION_TYPE, JS_CONDITION_TYPE, JS_RESOURCE_CODE, JS_VALUE);
            ParametersService.getInstance().insert(parameter);
            logger.info("The new parameter JS condition type was created successfully");
        } else if (parameter.getValue() == null) {
            parameter.setValue(JS_VALUE);
            ParametersService.getInstance().update(parameter);
            logger.info("JS condition type was updated successfully");
        }
    }

    private void updateParameters() {
        Parameters parameter = null;
        try {
            parameter = ParametersService.getInstance().getByCategoryAndCode(Constants.CONDITION_TYPE, ALWAYS_TRUE_CONDITION_TYPE);
        } catch (Exception exception) {
            logger.warn(exception.getMessage(), exception);
        }
        if (parameter != null && parameter.getValue() == null) {
            parameter.setValue(ALWAYS_TRUE_VALUE);
            ParametersService.getInstance().update(parameter);
            logger.info("ALWAYS_TRUE condition type was updated successfully");
        }

        parameter = null;
        try {
            parameter = ParametersService.getInstance().getByCategoryAndCode(Constants.CONDITION_TYPE, CEP_CONDITION_TYPE);
        } catch (Exception exception) {
            logger.warn(exception.getMessage(), exception);
        }
        if (parameter != null && parameter.getValue() == null) {
            parameter.setValue(CEP_VALUE);
            ParametersService.getInstance().update(parameter);
            logger.info("CEP condition type was updated successfully");
        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath)
            throws Exception {
    }
}
