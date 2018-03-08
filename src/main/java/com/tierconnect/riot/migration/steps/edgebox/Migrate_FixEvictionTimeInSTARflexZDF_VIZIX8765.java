package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

/**
 * Created by renan huanca on 08/11/2017.
 */
public class Migrate_FixEvictionTimeInSTARflexZDF_VIZIX8765 implements MigrationStep {

    private static Logger logger = Logger.getLogger(Migrate_FixEvictionTimeInSTARflexZDF_VIZIX8765.class);

    @Override public void migrateSQLBefore(String scriptPath) throws Exception {
    }

    @Override public void migrateHibernate() throws Exception {
        migrateZoneDwellFilter();
    }

    @Override public void migrateSQLAfter(String scriptPath) throws Exception {
    	
    }

    private void migrateZoneDwellFilter() throws Exception {
        JSONParser parser = new JSONParser();
        for (Edgebox edgeBridge : EdgeboxService.getInstance().getByType("STARflex")) {
            try {
                JSONObject currentConfig = (JSONObject) parser.parse(edgeBridge.getConfiguration());
                if(currentConfig.containsKey("zoneDwellFilter") && currentConfig.get("zoneDwellFilter") != null){
                    JSONObject zoneDwellFilter = (JSONObject)currentConfig.get("zoneDwellFilter");
                    zoneDwellFilter.putIfAbsent("evictionTime",24);
                    currentConfig.put("zoneDwellFilter",zoneDwellFilter);
                }
                edgeBridge.setConfiguration(currentConfig.toJSONString());
                EdgeboxService.getInstance().update(edgeBridge);
            } catch (ParseException e) {
                throw new Exception("Error migrating STARFlex bridge " + edgeBridge.getCode(), e);
            }
        }
    }
}
