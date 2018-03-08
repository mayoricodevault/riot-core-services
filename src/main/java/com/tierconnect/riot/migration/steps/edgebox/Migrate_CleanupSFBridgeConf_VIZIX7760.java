package com.tierconnect.riot.migration.steps.edgebox;

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
 * Created by bbustillos on 8/25/17.
 */
public class Migrate_CleanupSFBridgeConf_VIZIX7760 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_CleanupSFBridgeConf_VIZIX7760.class);


    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        removeSFBridgeConfigurations();
        removeSFBridgeParameters();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void removeSFBridgeConfigurations(){
        List<Edgebox> starflexBridgeList = EdgeboxService.getInstance().getByType("STARflex");
        for (Edgebox starflexBridge : starflexBridgeList){
            JSONObject STARflexOutputConfig = null;
            try {
                STARflexOutputConfig = (JSONObject) new JSONParser().parse(starflexBridge.getConfiguration());
                if (STARflexOutputConfig.containsKey("playStarFlexSync")){
                    // verify if the value 'active' is enabled
                    JSONObject replaySFMessagesValue = (JSONObject) STARflexOutputConfig.get("playStarFlexSync");
                    if ((Boolean)replaySFMessagesValue.get("active")){
                        // if is enabled move/migrate to new name 'replaySTARflexMessages'
                        STARflexOutputConfig.put("replaySTARflexMessages", replaySFMessagesValue);
                    }
                    STARflexOutputConfig.remove("playStarFlexSync");
                }
            } catch (ParseException e) {
                logger.error("Error reading STARflex Bridge Configuration with code:" + starflexBridge.getCode(), e);
            }
            if (STARflexOutputConfig != null){
                starflexBridge.setConfiguration(STARflexOutputConfig.toJSONString());
                EdgeboxService.getInstance().update(starflexBridge);
                logger.info("Starflex bridge code=" + starflexBridge.getCode() + " updated successfully");
            }
        }
    }

    private void removeSFBridgeParameters(){
        Parameters starflexParameters = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "STARflex");
        if (starflexParameters != null){
            try {
                JSONObject starflexOutputParameters = (JSONObject) new JSONParser().parse(starflexParameters.getValue());
                if (starflexOutputParameters.containsKey("extra")){
                    // remove configuration from 'playStarFlexSync' from parameters
                    JSONObject starflexConfiguration = (JSONObject) starflexOutputParameters.get("extra");
                    if (starflexConfiguration.containsKey("playStarFlexSync")){
                        starflexConfiguration.remove("playStarFlexSync");
                    }
                    starflexOutputParameters.put("extra", starflexConfiguration);
                }
                starflexParameters.setValue(starflexOutputParameters.toJSONString());
                ParametersService.getInstance().update(starflexParameters);
                logger.info("Starflex bridge parameters updated successfully");
            } catch (ParseException e) {
                logger.error("Error reading STARflex Bridge Parameters with bridge type:" + Constants.BRIDGE_TYPE, e);
            }
        }
    }
}
