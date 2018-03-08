package com.tierconnect.riot.iot.bridgeAgent;

import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.services.BrokerClientHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Created by cfernandez
 * on 2/6/17.
 * This class manage the deploy, start and stop of bridges
 */
public class BridgeAgentService {
    private static Logger logger = Logger.getLogger(BridgeAgentService.class);

    public void doAction(String bridgeConfiguration, String bridgeCode, String bridgeType, String bridgeStatus, long groupId) {
        logger.info("Calling Bridge Agent service....");

        // building message
        BridgeAgentSpecBuilder builder = new BridgeAgentBasicSpecBuilder(bridgeConfiguration, bridgeCode, bridgeType,
                bridgeStatus, groupId);
        String payload = builder.buildBody();
        if (!StringUtils.isEmpty(payload)) {
            String topic = builder.buildTopic();
            // publishing to the broker
            BrokerClientHelper.publish(topic, payload, null,
                    GroupService.getInstance().getMqttGroups(GroupService.getInstance().get(groupId)),
                    false, Thread.currentThread().getName());
        }
    }

    public String getPayload(String bridgeConfiguration, String bridgeCode, String bridgeType, String bridgeStatus, long groupId) {
        logger.info("Calling Bridge Agent service....");

        BridgeAgentSpecBuilder builder = new BridgeAgentBasicSpecBuilder(bridgeConfiguration, bridgeCode, bridgeType,
                bridgeStatus, groupId);
        String payload = builder.buildBody();
        return payload;
    }
}
