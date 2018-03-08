package com.tierconnect.riot.iot.entities;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.annotation.Generated;

@Entity

@Table(name="ml_prediction_predictor")@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class MlPredictionPredictor extends MlPredictionPredictorBase 
{

    public MlPredictionPredictor() { }

    public MlPredictionPredictor(String name, String value) {
        this.name = name;
        this.value = value;
    }

}

