package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.popdb.PopDBRequiredIOT;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.List;

/**
 * Created by bbustillos on 9/4/17.
 */
public class Migrate_CleanupConfigurationFTP_VIZIX7349 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_CleanupConfigurationFTP_VIZIX7349.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        updateFTPBridgeConfiguration();
        updateFTPAndOperatorsParameters();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void updateFTPBridgeConfiguration(){
        List<Edgebox> ftpBridgeList = EdgeboxService.getInstance().getByType("FTP");
        for (Edgebox ftpBridge: ftpBridgeList){
            // load configuration from edgebox
            JSONObject ftpOutputConfig = null;
            try {
                ftpOutputConfig = (JSONObject) new JSONParser().parse(ftpBridge.getConfiguration());
                // find properties:
                // -> processPolicy => processingType
                if (ftpOutputConfig.containsKey("processPolicy")){
                    changeActualConfiguration(ftpOutputConfig, "processPolicy", "processingType");
                    if (ftpOutputConfig.get("processingType").equals("Move")){
                        ftpOutputConfig.put("processingType", "full");
                    } else if (ftpOutputConfig.get("processingType").equals("Incremental")){
                        ftpOutputConfig.put("processingType", "incremental");
                    }
                }
                // -> processPolicy => processingType
                if (ftpOutputConfig.containsKey("pattern")){
                    changeActualConfiguration(ftpOutputConfig, "pattern", "filePattern");
                }
                updateRemoteFTPFoldersConfiguration(ftpOutputConfig);
                // -> schedule => cronSchedule
                if (ftpOutputConfig.containsKey("schedule")){
                    changeActualConfiguration(ftpOutputConfig, "schedule", "cronSchedule");
                }
                // remove ftp configuration don't needed
                removeProperty(ftpOutputConfig, "logRawMessages");
                removeProperty(ftpOutputConfig, "pattern");
                removeProperty(ftpOutputConfig, "localBackupFolder");
            } catch (ParseException e) {
                logger.error("Error reading FTP Bridge Configuration with code:" + ftpBridge.getCode(), e);
            }

            if (ftpOutputConfig != null){
                ftpBridge.setConfiguration(ftpOutputConfig.toJSONString());
                EdgeboxService.getEdgeboxDAO().update(ftpBridge);
            } else {
                logger.error("It was not possible to migrate cleanup FTPConfiguration");
            }
        }
    }

    private void changeActualConfiguration(JSONObject ftpOutputConfig, String property, String newProperty){
        String actualValue;
        actualValue = (String) ftpOutputConfig.get(property);
        ftpOutputConfig.remove(property);
        ftpOutputConfig.put(newProperty, actualValue);
    }

    private void updateRemoteFTPFoldersConfiguration(JSONObject ftpOutputConfig){
        String source;
        String destination;
        // -> path => getValue and remove for 'source'
        // -> ftpDestinationFolder => getValue and remove for 'destination'
        source = (String) ftpOutputConfig.get("path");
        ftpOutputConfig.remove("path");
        destination = (String) ftpOutputConfig.get("ftpDestinationFolder");
        ftpOutputConfig.remove("ftpDestinationFolder");
        // -> "" => remoteFTPFolders add (source and destination)
        JSONObject remoteFTPFolders = new JSONObject();
        remoteFTPFolders.put("source", source);
        remoteFTPFolders.put("destination", destination);
        ftpOutputConfig.put("remoteFTPFolders", remoteFTPFolders);
    }

    private void removeProperty(JSONObject ftpConfiguration, String propertyToRemove){
        if (ftpConfiguration.containsKey(propertyToRemove)) {
            ftpConfiguration.remove(propertyToRemove);
        }
    }

    private void updateFTPAndOperatorsParameters(){
        // set configuration for FTP Bridge
        Parameters param = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "FTP");
        param.setValue(PopDBRequiredIOT.getFtpConfiguration());
        ParametersService.getInstance().update(param);

        // set configuration for Operators
        PopDBRequiredIOT.populateOperators();
    }
}
