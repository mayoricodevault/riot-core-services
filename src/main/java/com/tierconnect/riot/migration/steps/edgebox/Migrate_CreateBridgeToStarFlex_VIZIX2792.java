package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.DevicesService;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.commons.lang.StringUtils;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tierconnect.riot.appcore.utils.Utilities.isEmptyOrNull;
import static com.tierconnect.riot.iot.services.DevicesService.*;
import static com.tierconnect.riot.iot.services.ParametersService.CODE_CORE;
import static com.tierconnect.riot.iot.services.ParametersService.CODE_EDGE;

/**
 * Created by vealaro on 3/16/17.
 */
public class Migrate_CreateBridgeToStarFlex_VIZIX2792 implements MigrationStep {

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {

    }

    @Override
    public void migrateHibernate() throws Exception {
        createNewBridgesToStarFlex();
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {

    }

    private void createNewBridgesToStarFlex() throws ParseException, IOException {
        EdgeboxService edgeboxService = EdgeboxService.getInstance();
        // MCB corebridge
        Edgebox corebridgeMCB = edgeboxService.selectByCode("MCB");
        // get port
        Edgebox maxPortBridges = edgeboxService.getMaxPortBridges();
        Long port = 9090L;
        if (maxPortBridges != null && maxPortBridges.getPort() != null){
            port =  maxPortBridges.getPort() + 1;
        }
        // get ALL StarFLEX
        Map<Group, List<Edgebox>> starFLEXList = edgeboxService.getEdgeBoxGroupByGroup("StarFLEX");
        for (Map.Entry<Group, List<Edgebox>> groupStarFlex : starFLEXList.entrySet()) {
            Edgebox starFlexDevice = null;
            Edgebox starFlexTag = null;
            for (Edgebox edge : groupStarFlex.getValue()) {
                if (!isEmptyOrNull(edge.getConfiguration())) {
                    JSONObject outputConfig = (JSONObject) new JSONParser().parse(edge.getConfiguration());
                    if ("StarFlex".equals(outputConfig.get("messageMode"))) {
                        starFlexDevice = edge;
                    } else if ("FlexTag".equals(outputConfig.get("messageMode"))) {
                        starFlexTag = edge;
                    }
                }
            }
            if (starFlexDevice != null && starFlexTag != null) {
                createBridges(starFlexDevice, starFlexTag, port, corebridgeMCB);
                port++;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void createBridges(Edgebox starFlexDevice, Edgebox starFlexTag, Long portAleBridge, Edgebox corebridgeMCB) throws ParseException {
        JSONObject configStarFlex = (JSONObject) new JSONParser().parse(starFlexDevice.getConfiguration());
        JSONObject configStarFlexTag = (JSONObject) new JSONParser().parse(starFlexTag.getConfiguration());
        String configuration = DevicesService.getInstance().getAleBridgeConfiguration(
                getValueConnectionCode("mqtt", configStarFlex),     // connection to mosquitto
                (String) configStarFlexTag.get("thingTypeCode"));   // thing type code STARFLEX TAG
        if (configuration != null) {
            Edgebox aleBridge = EdgeboxService.getInstance().selectByCode(ALEBRIDGES_CODE + starFlexTag.getGroup().getCode());
            if (aleBridge == null) {
                String[] types = {"edge"};
                List<Edgebox> aleList = EdgeboxService.getInstance().getByTypesAndGroup(types, starFlexTag.getGroup());
                if (aleList.isEmpty()) {
                    // create AleBridge
                    aleBridge = DevicesService.getInstance()
                        .createEdgebox(starFlexDevice.getGroup(),// Group
                            ALEBRIDGES_NAME + starFlexDevice.getGroup().getName(),   // Name bridge
                            ALEBRIDGES_CODE + starFlexDevice.getGroup().getCode(),   // Code bridge
                            CODE_EDGE,                                               // type
                            configuration,
                            // configuration
                            portAleBridge);                                          // port
                } else {
                    aleBridge = aleList.get(0);
                }
            }
            List<String> topics = new ArrayList<>(4);
            topics.add("/v1/data/APP2/#");                              // topic UI
            topics.add("/v1/data/" + aleBridge.getCode() + "/#");       // topic ALEBRIDGE
            topics.add("/v1/data/" + starFlexDevice.getCode() + "/#");  // topic STARFLEX DEVICE
//            topics.add("/v1/data/" + starFlexTag.getCode() + "/#");     // topic STARFLEX TAG

            configuration = DevicesService.getInstance().getCoreBridgeConfiguration(
                    getValueConnectionCode("mqtt", configStarFlex),     // connection to mosquitto
                    getValueConnectionCode("mongo", configStarFlex),    // connection to mongo
                    topics);

            List<String> exclude = new ArrayList<>();
            exclude.add("/v1/data/STAR/#");
            exclude.add("/v1/data/STAR1/#");
            if (configuration != null) {
                Edgebox coreBridge = EdgeboxService.getInstance().selectByCode(COREBRIDGES_CODE + starFlexDevice.getGroup().getCode());
                if (coreBridge == null) {
                    String[] types = {"core"};
                    List<Edgebox> coreList = EdgeboxService.getInstance().getByTypesAndGroup(types, starFlexTag.getGroup());
                    if (coreList.isEmpty()) {

                        // create coreBridge
                        DevicesService.getInstance().createEdgebox(starFlexDevice.getGroup(),
                            // Group
                            COREBRIDGES_NAME + starFlexDevice.getGroup().getName(),  // Name bridge
                            COREBRIDGES_CODE + starFlexDevice.getGroup().getCode(),  // Code bridge
                            CODE_CORE,                                               // type
                            configuration,
                            // configuration
                            0L);                                                     // port
                    } else {
                        coreBridge = coreList.get(0);
                        DevicesService.getInstance().updateTopicsCoreBridge(coreBridge, topics, exclude);
                    }
                } else {
                    DevicesService.getInstance().updateTopicsCoreBridge(coreBridge, topics, exclude);
                }
            }
            topics.clear();
            topics.add("/v1/data/#");
            DevicesService.getInstance().updateAllTopicsCoreBridge(corebridgeMCB, topics);
        }
    }


    private String getValueConnectionCode(String key, JSONObject jsonValue) throws ParseException {
        String result = StringUtils.EMPTY;
        if (jsonValue.get(key) != null && ((JSONObject) jsonValue.get(key)).get("connectionCode") != null) {
            result = (String) ((JSONObject) jsonValue.get(key)).get("connectionCode");
        }
        return result;
    }
}
