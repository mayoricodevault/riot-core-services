package com.tierconnect.riot.iot.entities;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.annotation.Generated;

@Entity

@Table(name="ml_business_model_predictor")@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class MlBusinessModelPredictor extends MlBusinessModelPredictorBase 
{

    public MlBusinessModelPredictor() {
    }

    public MlBusinessModelPredictor (String name) {
        this.name = name;
    }

    public MlBusinessModelPredictor (String name, String type) {
        this.name = name;
        this.type = type;
    }
}

