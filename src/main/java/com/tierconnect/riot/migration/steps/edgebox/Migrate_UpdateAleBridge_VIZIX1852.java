package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.List;

/**
 * Created by vealaro on 2/17/17.
 */
public class Migrate_UpdateAleBridge_VIZIX1852 implements MigrationStep {

    private static Logger logger = Logger.getLogger(Migrate_UpdateAleBridge_VIZIX1852.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        updateAleBridge();
        udpateParameters();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void updateAleBridge() {
        List<Edgebox> edgeBridgeList = EdgeboxService.getInstance().getByType("edge");
        for (Edgebox edgebox : edgeBridgeList) {
            if (!Utilities.isEmptyOrNull(edgebox.getConfiguration())) {
                try {
                    JSONObject outputConfig = (JSONObject) new JSONParser().parse(edgebox.getConfiguration());
                    if (!outputConfig.containsKey("streaming")) {
                        outputConfig.put("streaming", getStreamingMap());
                        edgebox.setConfiguration(outputConfig.toJSONString());
                        EdgeboxService.getInstance().update(edgebox);
                    }
                } catch (ParseException e) {
                    logger.error("Error in update configuration EDGE with code:" + edgebox.getCode(), e);
                }
            }
        }
    }

    private void udpateParameters() {
        Parameters edge = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "edge");
        try {
            if (!Utilities.isEmptyOrNull(edge.getValue())) {
                JSONObject outputConfig = (JSONObject) new JSONParser().parse(edge.getValue());
                if (!outputConfig.containsKey("streaming")) {
                    outputConfig.put("streaming", getStreamingMap());
                    edge.setValue(outputConfig.toString());
                    ParametersService.getInstance().update(edge);
                }
            }
        } catch (ParseException e) {
            logger.error("Error in update value of Parameter with category=\"BRIDGE_TYPE\" and code=\"edge\" ", e);
        }
    }

    @SuppressWarnings("unchecked")
    private JSONObject getStreamingMap() {
        // VIZIX-1852: Add "streaming" mode to ALEBridges configuration
        JSONObject streaming = new JSONObject();
        streaming.put("active", Boolean.FALSE);
        streaming.put("bufferSize", 10);
        return streaming;
    }
}
