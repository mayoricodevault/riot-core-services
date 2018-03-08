package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.commons.Constants;
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
 * Created by brayan on 5/7/17.
 */
public class Migrate_RemoveReloadCacheTickleAndThreadDispatchMode_VIZIX3468_3627 implements MigrationStep {

    public final String RELOAD_CACHE_TICKLE = "reloadCacheTickle";
    public final String THREAD_DISPATCH_MODE = "threadDispatchMode";

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
        for(Edgebox edgebox:edgeboxList) {
            if(!Utilities.isEmptyOrNull(edgebox.getConfiguration())) {
                JSONObject outputConfig = (JSONObject) new JSONParser().parse(edgebox.getConfiguration());

                if(outputConfig.containsKey(RELOAD_CACHE_TICKLE)) {
                    outputConfig.remove(RELOAD_CACHE_TICKLE);
                }

                if(outputConfig.containsKey(THREAD_DISPATCH_MODE)) {
                    outputConfig.remove(THREAD_DISPATCH_MODE);
                }

                edgebox.setConfiguration(outputConfig.toJSONString());
                EdgeboxService.getInstance().update(edgebox);
            }
        }
    }

    private void updateParameters() throws ParseException{
        Parameters edge = ParametersService.getInstance().getByCategoryAndCode(Constants.BRIDGE_TYPE, "core");
        if (!Utilities.isEmptyOrNull(edge.getValue())) {
            JSONObject outputConfig = (JSONObject) new JSONParser().parse(edge.getValue());
            outputConfig.remove(THREAD_DISPATCH_MODE);
            edge.setValue(outputConfig.toString());
            ParametersService.getInstance().update(edge);
        }
    }
}
