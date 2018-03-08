package com.tierconnect.riot.migration.steps.parameters;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.steps.MigrationStep;

/**
 * Created by vealaro on 3/22/17.
 */
public class Migration_AddParametersAction_VIZIX_3020 implements MigrationStep {

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        newParameterActionHTTP();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void newParameterActionHTTP() {
        Parameters http = ParametersService.getInstance().getByCategoryAndCode(Constants.ACTION_TYPE, "http");
        if (http == null) {
            Parameters parameterHTTP = new Parameters(Constants.ACTION_TYPE, "http", "@SYSTEM_PARAMETERS_ACTION_TYPE_HTTP",
                    "{\"method\": \"POST\",\"url\": \"\", \"timeout\": -1, \"openResponseIn\":\"MODAL\", " +
                            "\"headers\": {\"Content-Type\": \"application/json\"}, \"basicAuth\": {\"username\": \"\",\"password\": \"\"}}");
            ParametersService.getInstance().insert(parameterHTTP);
        }
    }

}
