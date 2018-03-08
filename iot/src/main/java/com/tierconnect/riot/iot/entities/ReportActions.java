package com.tierconnect.riot.iot.entities;

import javax.annotation.Generated;
import javax.persistence.Entity;
import java.util.HashMap;
import java.util.Map;

@Entity
@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class ReportActions extends ReportActionsBase {

    @Override
    public Map<String, Object> publicMap() {
        Map<String, Object> publicMap = new HashMap<String, Object>();
        if (actionConfiguration != null) {
            actionConfiguration.setDisplayOrder(getDisplayOrder());
            publicMap.putAll(actionConfiguration.publicMap());
        }
        return publicMap;
    }
}

