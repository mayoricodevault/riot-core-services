package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.List;

/**
 * Created by rchirinos
 * on 24/03/17.
 */
public class Migrate_AddDwellFilterStarFlexTagBridge_VIZIX3063 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_AddDwellFilterStarFlexTagBridge_VIZIX3063.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override
    public void migrateHibernate() throws Exception {
        updateStarflexTagConfiguration();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
    }

    /**
     * Update StarFlex Tag configuration with Zone Dwell Filter
     */
    private void updateStarflexTagConfiguration() {
        List<Edgebox> edgeBridgeList = EdgeboxService.getInstance().getByType("StarFLEX");
        for (Edgebox edgebox : edgeBridgeList) {
            if ( ( edgebox.getCode().startsWith("START_") || edgebox.getCode().equals("STAR1") )
                    && !Utilities.isEmptyOrNull(edgebox.getConfiguration())) {
                try {
                    JSONObject outputConfig = (JSONObject) new JSONParser().parse(edgebox.getConfiguration());
                    if (!outputConfig.containsKey("zoneDwellFilter")) {
                        outputConfig.put("zoneDwellFilter", getZoneDwellFilter());
                        edgebox.setConfiguration(outputConfig.toJSONString());
                        EdgeboxService.getInstance().update(edgebox);
                    }
                } catch (ParseException e) {
                    logger.error("Error in update configuration EDGE with code:" + edgebox.getCode(), e);
                }
            }
        }
    }

    /**
     * Get Zone Dwell filter configuration
     * @return
     */
    private JSONObject getZoneDwellFilter() {
        JSONObject zoneDwellFilter = new JSONObject();
        zoneDwellFilter.put("inZoneDistance", 0);
        zoneDwellFilter.put("lastDetectTimeActive", 1);
        zoneDwellFilter.put("lastDetectTimeWindow", 30);
        zoneDwellFilter.put("unlockDistance", 0);
        zoneDwellFilter.put("zoneDwellTime", 1);
        zoneDwellFilter.put("active", 1);
        return  zoneDwellFilter;
    }

}
