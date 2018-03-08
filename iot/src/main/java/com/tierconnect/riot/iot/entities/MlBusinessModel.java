package com.tierconnect.riot.iot.entities;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.iot.entities.exceptions.MLModelException;
import org.apache.log4j.Logger;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.annotation.Generated;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

//todo what does this class do?
@Entity
@Table(name="ml_business_model")@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class MlBusinessModel extends MlBusinessModelBase {

    private static Logger logger = Logger.getLogger(MlBusinessModel.class);

    public MlBusinessModel() {
    }

    private MlBusinessModel(Builder b) {
        this.name = b.name;
        this.description = b.description;
        this.appName = b.appName;
        this.jar = b.jar;
        this.predictors = b.predictors;
        this.modifiedDate = new Date(System.currentTimeMillis());
        logger.info("modified date: " + this.modifiedDate);

        for (MlBusinessModelPredictor predictor : this.predictors) {
            predictor.setBusinessModel(this);
        }
    }

    public static class Builder {
        private String name;
        private String description;
        private String appName;
        private String jar;

        private List<MlBusinessModelPredictor> predictors = new ArrayList<>();

        public Builder(String name) {
            this.name = name;
        }


        public Builder(String name, String desc, String appName, String jar) {
            this.name = name;
            this.description = desc;
            this.appName = appName;
            this.jar = jar;
        }

        public Builder predictor(String name) {
            predictors.add(new MlBusinessModelPredictor(name));
            return this;
        }

        public Builder predictor(String name, String type) {
            predictors.add(new MlBusinessModelPredictor(name, type));
            return this;
        }


        public MlBusinessModel build() {
            return new MlBusinessModel(this);
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = publicMap();
        List<Map<String, Object>> predictorsMaps = new ArrayList<>();

        for (MlBusinessModelPredictor mlBusinessModelPredictor : predictors) {
            predictorsMaps.add(mlBusinessModelPredictor.publicMap());
        }
        map.put("inputs", predictorsMaps);
        return map;
    }


    // todo remove this method - not used
    public MlModel train(Group group,
                         ThingType thingType,
                         LocalDate start,
                         LocalDate end,
                         String name,
                         String collection,
                         //List<MlModelPredictor> predictors,
                         JobServer jobServer) throws MLModelException {

//        String uuid = UUID.randomUUID().toString();
//
//        //todo.. get hashmap of predictors
//        Map<String, String> response = jobServer.extractAndTrain(start, end, collection, extractorTrainerName,
//                new HashMap<String, String>(), uuid, group.getId(), appName);
//
//        String status = response.get("status");
//        String sparkJobId = response.get("jobId");
//
//        Date startDate = Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant());
//        Date endDate = Date.from(end.atStartOfDay(ZoneId.systemDefault()).toInstant());
//
//        //return new MlModel(this, group, thingType, predictors, startDate, endDate, name, uuid, sparkJobId, status);
        return null;
    }


    public MlExtraction extraction(Group group,
                                   LocalDate start,
                                   LocalDate end,
                                   String name,
                                   String comments,
                                   List<MlExtractionPredictor> predictors) throws MLModelException {

        return new MlExtraction(this, group, name, comments, start, end, predictors);
    }
}

