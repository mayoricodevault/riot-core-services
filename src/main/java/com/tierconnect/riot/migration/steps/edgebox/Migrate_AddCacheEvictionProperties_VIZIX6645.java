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
 * Created by brayan on 5/7/17.
 */
public class Migrate_AddCacheEvictionProperties_VIZIX6645 implements MigrationStep {

    private static Logger logger = Logger.getLogger(Migrate_AddCacheEvictionProperties_VIZIX6645.class);


    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        updateCoreBridge();
        updateParameters();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    public void updateCoreBridge() throws ParseException {
        List<Edgebox> edgeboxList = EdgeboxService.getInstance().getByType("core");
        for (Edgebox edgebox : edgeboxList) {
            if (!Utilities.isEmptyOrNull(edgebox.getConfiguration())) {
                JSONObject outputConfig = (JSONObject) new JSONParser().parse(edgebox.getConfiguration());

                JSONObject thingCache;
                if(outputConfig.containsKey("thingCache")){
                    thingCache = (JSONObject) outputConfig.get("thingCache");
                }else{
                    thingCache = new JSONObject();
                }

                if (!thingCache.containsKey("size")) {
                    thingCache.put("size", 1000000);
                }

                if (!thingCache.containsKey("evictionTime")) {
                    thingCache.put("evictionTime", 60);
                }

                outputConfig.put("thingCache", thingCache);
                edgebox.setConfiguration(outputConfig.toJSONString());
                EdgeboxService.getInstance().update(edgebox);
            }
        }
    }

    private void updateParameters() throws ParseException {
        Parameters edgeParameters = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "core");
        if (edgeParameters != null) {
            try {
                JSONObject outputConfig = (JSONObject) new JSONParser().parse(edgeParameters.getValue());
                if (outputConfig.get("configuration") != null) {
                    JSONObject configuration = (JSONObject) outputConfig.get("configuration");

                    JSONObject thingCache;

                    if(outputConfig.containsKey("thingCache")){
                        thingCache = (JSONObject) outputConfig.get("thingCache");
                    }else{
                        thingCache = new JSONObject();
                        thingCache.put("value", new JSONObject());
                        thingCache.put("type", "JSON");
                        thingCache.put("order", 4);
                        thingCache.put("required", false);
                    }

                    JSONObject thingCacheValue;
                    if(thingCache.containsKey("value")){
                        thingCacheValue = (JSONObject) thingCache.get("value");
                    }else{
                        thingCacheValue = new JSONObject();
                    }

                    if (!thingCacheValue.containsKey("size")) {
                        JSONObject size = new JSONObject();
                        size.put("value", 1000000);
                        size.put("type", "Number");
                        size.put("order", 0);
                        size.put("required", true);

                        thingCacheValue.put("size", size);
                    }

                    if (!thingCacheValue.containsKey("evictionTime")) {
                        JSONObject evictionTime = new JSONObject();
                        evictionTime.put("value", 60);
                        evictionTime.put("type", "Number");
                        evictionTime.put("order", 1);
                        evictionTime.put("required", true);

                        thingCacheValue.put("evictionTime", evictionTime);
                    }

                    thingCache.put("value", thingCacheValue);
                    configuration.put("thingCache", thingCache);
                    outputConfig.put("configuration", configuration);
                    edgeParameters.setValue(outputConfig.toJSONString());
                    ParametersService.getInstance().update(edgeParameters);
                }
            } catch (ParseException e) {
                logger.error("Error updating coreBridge parameters: " + edgeParameters.getCode(), e);
            }

        }
    }
}
