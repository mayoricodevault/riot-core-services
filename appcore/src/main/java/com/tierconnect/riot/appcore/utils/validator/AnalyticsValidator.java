package com.tierconnect.riot.appcore.utils.validator;

import com.tierconnect.riot.appcore.entities.ConnectionType;
import com.tierconnect.riot.appcore.services.ConnectionTypeService;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;


public class AnalyticsValidator implements ConnectionValidator {
    private static Logger logger = Logger.getLogger(AnalyticsValidator.class);
    private int status;
    private String cause;

    @Override public int getStatus() {
        return status;
    }

    @Override public String getCause() {
        return cause;
    }

    @Override public boolean testConnection(ConnectionType connectionType, String properties) {
        JSONParser parser = new JSONParser();
        JavaStreamingContext jsc;
        try {
            JSONObject analyticsProperties = (JSONObject) parser.parse(properties);
            if (!StringUtils.isBlank(String.valueOf(analyticsProperties.get("masterHost")))
                && !StringUtils.isBlank(String.valueOf(analyticsProperties.get("masterPort")))
                && !StringUtils.isBlank(String.valueOf(analyticsProperties.get("responseTimeout")))) {
                SparkConf conf = new SparkConf().setAppName("Java API demo").setMaster(
                    "spark://" + analyticsProperties.get("masterHost") + ":" + analyticsProperties.get("masterPort"))
                    .set("spark.driver.host", String.valueOf(analyticsProperties.get("masterHost")))
                    .set("spark.driver.port", String.valueOf(analyticsProperties.get("masterPort")));
                jsc = new JavaStreamingContext(conf, Durations.seconds(
                    Long.parseLong(analyticsProperties.get("responseTimeout").toString())));
                jsc.stop();
            }
            MongoValidator mongoValidator = new MongoValidator();
            JSONObject mongoProperties = new JSONObject();
            mongoProperties.put("mongoPrimary", analyticsProperties.get("mongo.host") + ":" + analyticsProperties.get("mongo.port"));
            mongoProperties.put("mongoSecondary", "");
            mongoProperties.put("mongoReplicaSet", "");
            mongoProperties.put("mongoSSL", analyticsProperties.get("mongo.secure"));
            mongoProperties.put("username", analyticsProperties.get("mongo.username"));
            mongoProperties.put("password", analyticsProperties.get("password"));
            mongoProperties.put("mongoAuthDB", "admin");
            mongoProperties.put("mongoDB", analyticsProperties.get("mongo.dbname"));
            mongoProperties.put("mongoSharding", "false");
            mongoProperties.put("mongoConnectTimeout", 0);
            mongoProperties.put("mongoMaxPoolSize", 0);
            boolean mongoRes = mongoValidator.testConnection(ConnectionTypeService.getInstance().get(4L), mongoProperties.toJSONString());

            status = mongoValidator.getStatus();
            cause = mongoValidator.getCause();
            return mongoRes;
        } catch (ParseException e) {
            logger.warn("Cannot parse connection properties.", e);
            status = 400;
            cause = e.getMessage();
            return false;
        } catch (Exception e) {
            logger.warn("Error validating ANALYTICS connection.", e);
            status = 400;
            cause = e.getMessage();
            return false;
        }
    }
}
