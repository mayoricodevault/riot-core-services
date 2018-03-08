package com.tierconnect.riot.migration.steps.parameters;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.Parameters;
import com.tierconnect.riot.iot.services.ParametersService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

/**
 * Created by hmartinez on 25-05-17.
 */
public class Migrate_StarFlexPlaySyncJobParameters_VIZIX5108 implements MigrationStep{
    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {

    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    public void InsertPlaySyncParameters(){
        Parameters starFlexParameters = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "STARflex");
        if (starFlexParameters != null) {
            JSONObject outputConfig = null;
            try {
                outputConfig = (JSONObject) new JSONParser().parse(starFlexParameters.getValue());
            } catch (ParseException e) {
                e.printStackTrace();
            }

            if (outputConfig.get("extra") != null){
                JSONObject extra = new JSONObject();
                extra = (JSONObject)outputConfig.get("extra");

                JSONObject playSyncJobStatus = new JSONObject();
                playSyncJobStatus.put("value", false);
                playSyncJobStatus.put("type","Boolean");
                playSyncJobStatus.put("required",false);

                JSONObject playSyncJobBackupPath = new JSONObject();
                playSyncJobBackupPath.put("value", "");
                playSyncJobBackupPath.put("type","String");
                playSyncJobBackupPath.put("required",false);


                JSONObject playSyncJobConf = new JSONObject();
                playSyncJobConf.put("active", playSyncJobStatus);
                playSyncJobConf.put("backupFolder", playSyncJobBackupPath);

                JSONObject playSyncJob = new JSONObject();
                playSyncJob.put("value", playSyncJobConf);
                playSyncJob.put("type", "JSON");
                playSyncJob.put("required", false);

                extra.put("playStarFlexSync",playSyncJob);

                starFlexParameters.setValue(extra.toJSONString());
                ParametersService.getInstance().update(starFlexParameters);
            }
        }
    }
}
