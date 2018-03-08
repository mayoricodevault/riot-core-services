package com.tierconnect.riot.iot.entities;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.annotation.Generated;
import java.util.HashMap;
import java.util.Map;

@Entity

@Table(name="ml_extraction_predictor")@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class MlExtractionPredictor extends MlExtractionPredictorBase 
{

    public static final String SEPARATOR = "|";
    private String featureName;

    public MlExtractionPredictor() {
    }


    public MlExtractionPredictor(MlBusinessModelPredictor businessModelPredictor, ThingType thingType,
                            String propertyName, String propertyPath, String featureName) {
        this.thingType = thingType;
        this.businessModelPredictor = businessModelPredictor;
        this.propertyName = propertyName;
        this.propertyPath = propertyPath;
        this.featureName = featureName;
    }

    public MlExtractionPredictor( String propertyName, String propertyPath) {
        this.propertyName = propertyName;
        this.propertyPath = propertyPath;
        this.featureName = propertyName;// REVIEW this is internal property of riot-ml by now but in the feature
                                        // it may be configurable
    }


    public String toConfigurationString(){
        String configurationString = featureName;
        configurationString = configurationString.concat(SEPARATOR).concat(propertyPath);
        return configurationString;
    }

    public Map toMap() {
        Map map = publicMap();

        Map<String, Object> businessModelPredictors = new HashMap();
        businessModelPredictors.put("id", businessModelPredictor.getId());
        businessModelPredictors.put("name", businessModelPredictor.getName());

        Map<String, Object> thingTypeMap = new HashMap();
        thingTypeMap.put("id", thingType.getId());
        thingTypeMap.put("name", thingType.getName());

        map.put("thingType", thingTypeMap);
        map.put("predictor", businessModelPredictors);
        return map;
    }

}

