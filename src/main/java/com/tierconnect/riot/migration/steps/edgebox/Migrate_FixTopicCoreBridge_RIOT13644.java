package com.tierconnect.riot.migration.steps.edgebox;

import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.Edgebox;
import com.tierconnect.riot.iot.services.EdgeboxService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;
import org.jose4j.json.internal.json_simple.JSONArray;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_FixTopicCoreBridge_RIOT13644 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_FixTopicCoreBridge_RIOT13644.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        populateTopicForService();
    }

    /**
     * This method populates the topic APP2 for Services
     */
    public void populateTopicForService() {
        List<Edgebox> lstEdgeBox = EdgeboxService.getInstance().getByType(Constants.EDGEBOX_CORE_TYPE);
        String topics = "topics";
        String mqtt = "mqtt";
        if ((lstEdgeBox != null) && (!lstEdgeBox.isEmpty())) {
            try {
                for (Edgebox edgebox : lstEdgeBox) {
                    if ((edgebox.getConfiguration() != null) && (!edgebox.getConfiguration().isEmpty())) {
                        JSONObject outputConfig = (JSONObject) new JSONParser().parse(edgebox.getConfiguration());
                        if ((outputConfig != null) && (outputConfig.containsKey(mqtt))) {
                            JSONObject mqttJson = ((JSONObject) outputConfig.get(mqtt));
                            if (mqttJson.containsKey(topics)) {
                                JSONArray topicsArray = (JSONArray) mqttJson.get(topics);
                                if ((topics != null) && (!topics.isEmpty())) {
                                    int count = 0;
                                    for (Object topic : topicsArray) {
                                        if (topic.equals(Constants.APP2_MQTT_TOPIC)) {
                                            break;
                                        }
                                        count++;
                                    }
                                    if (count < topicsArray.size()) {
                                        topicsArray.add(Constants.APP2_MQTT_TOPIC);
                                        mqttJson.put(topics, topicsArray);
                                        outputConfig.put(mqtt, mqttJson);
                                        edgebox.setConfiguration(outputConfig.toJSONString());
                                        EdgeboxService.getInstance().update(edgebox);
                                    }
                                }
                            } else {
                                JSONArray topicsArray = new JSONArray();
                                topicsArray.add(Constants.APP2_MQTT_TOPIC);
                                mqttJson.put(topics, topicsArray);
                                outputConfig.put(mqtt, mqttJson);
                                edgebox.setConfiguration(outputConfig.toJSONString());
                                EdgeboxService.getInstance().update(edgebox);
                            }
                        }
                    }
                }

            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
