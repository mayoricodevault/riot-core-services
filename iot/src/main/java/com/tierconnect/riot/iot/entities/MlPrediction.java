package com.tierconnect.riot.iot.entities;

import com.tierconnect.riot.appcore.entities.Group;
import org.apache.log4j.Logger;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.annotation.Generated;
import java.text.SimpleDateFormat;
import java.util.*;

@Entity

@Table(name="ml_prediction")@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class MlPrediction extends MlPredictionBase 
{

    private static Logger logger = Logger.getLogger(MlPrediction.class);


    public MlPrediction() { }


    public MlPrediction(Builder b) {
        this.trainedModel = b.trainedModel;
        this.group = b.group;
        this.name = b.name;
        this.comments = b.comments;
        this.uuid = b.uuid;
        this.predictors = b.predictors;
        this.modifiedDate = new Date(System.currentTimeMillis());

        for (MlPredictionPredictor p : this.predictors) {
            p.setPrediction(this);
        }
    }


    public Map toMap() {


        Map<String, Object> modelMap = new HashMap<>();
        modelMap.put("id", trainedModel.getId());
        modelMap.put("name", trainedModel.getName());

        Map<String, Object> groupMap = new HashMap<>();
        groupMap.put("id", group.getId());
        groupMap.put("name", group.getName());

        Map<String, Object> businessModel = new HashMap<>();
        businessModel.put("id", trainedModel.getExtraction().getBusinessModel().getId());
        businessModel.put("name", trainedModel.getExtraction().getBusinessModel().getName());

        Map<String, Object> map = publicMap();
        map.put("model", modelMap);
        map.put("group", groupMap);
        map.put("businessModel", businessModel);

        Map<String, Object> predictorsMap = new HashMap<>();
        for(MlPredictionPredictor p : predictors) {
            // TODO: refactor this harcoded temporal solution
            // Predictors startDate and endDate are not processed as normal predictors
            // because they don't have to be part of "predictors" field. However,
            // this differentiated treatement most probably is related to something wrong
            // in the design.
            if (p.getName().equals("startDate") || p.getName().equals("endDate")) {
                map.put(p.getName(), p.getValue());
            }
            else {
                predictorsMap.put(p.getName(), p.getValue());
            }
        }
        map.put("predictors", predictorsMap);

        return map;
    }


    public static class Builder {
        private MlModel trainedModel;
        private Group group;
        private String name;
        private String comments;
        private String uuid;

        private List<MlPredictionPredictor> predictors = new ArrayList<>();

        public Builder(MlModel model, Group group, String name, String comments, String uuid) {
            this.trainedModel = model;
            this.group = group;
            this.name = name;
            this.comments = comments;
            this.uuid = uuid;
        }


        public Builder predictor(String name, String value) {
            predictors.add(new MlPredictionPredictor(name, value));
            return this;
        }

        public Builder predictorsFromEncodedString(String encodedStr) {
            for(String token : encodedStr.trim().replace("[", "").replace("]", "").split(",")) {
                logger.info("token: " + token);
                String[] nameAndValue = token.split("\\|");
                predictors.add(new MlPredictionPredictor(nameAndValue[0], nameAndValue[1].replace(";", ",")));
            }
            return this;
        }

        public MlPrediction build() {
            return new MlPrediction(this);
        }
    }
}

