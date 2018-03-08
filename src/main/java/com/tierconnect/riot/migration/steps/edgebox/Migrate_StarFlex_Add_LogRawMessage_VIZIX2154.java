package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.List;

/**
 * Created by hmartinez on 19-05-17.
 */
public class Migrate_StarFlex_Add_LogRawMessage_VIZIX2154 implements MigrationStep {
    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        addLogRawMessagesToBridges();
        addLogRawMessagesToParameters();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void addLogRawMessagesToBridges() throws ParseException {
        List<Edgebox> edgeboxes = EdgeboxService.getInstance().getByType("STARflex");
        for (Edgebox starFlexTag : edgeboxes) {
            JSONObject outputConfig = (JSONObject) new JSONParser().parse(starFlexTag.getConfiguration());
            outputConfig.put("logRawMessages", 0);
            starFlexTag.setConfiguration(outputConfig.toJSONString());
            EdgeboxService.getInstance().update(starFlexTag);
        }
    }

    private void addLogRawMessagesToParameters() throws ParseException {
        Parameters parameterStarFlex = ParametersService.getInstance().getByCategoryAndCode(
                ParametersService.CATEGORY_BRIDGE_TYPE,
                "STARflex");

        if (parameterStarFlex != null) {
            JSONObject outputConfig = (JSONObject) new JSONParser().parse(parameterStarFlex.getValue());
            if (outputConfig.get("logRawMessages") == null) {
                outputConfig.put("logRawMessages", 0);
                parameterStarFlex.setValue(outputConfig.toJSONString());
                ParametersService.getInstance().update(parameterStarFlex);
            }
        }
    }
}
