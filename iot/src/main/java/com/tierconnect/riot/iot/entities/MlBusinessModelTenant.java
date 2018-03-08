package com.tierconnect.riot.iot.entities;

import com.tierconnect.riot.appcore.entities.Group;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.annotation.Generated;

@Entity

@Table(name="ml_business_model_tenant")@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class MlBusinessModelTenant extends MlBusinessModelTenantBase
{

    public MlBusinessModelTenant() {
    }

    public MlBusinessModelTenant(MlBusinessModel businessModel, Group group, String collection, Boolean enabled) {
        this.businessModel = businessModel;
        this.group = group;
        this.collection = collection;
        this.enabled = enabled;
    }
}

