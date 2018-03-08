package com.tierconnect.riot.iot.entities;

import javax.annotation.Generated;
import javax.persistence.Entity;
import java.util.Collections;
import java.util.Map;

@Entity
@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class LogExecutionAction extends LogExecutionActionBase {

    @Override
    public Map<String, Object> publicMap() {
        Map<String, Object> map = super.publicMap();
        if (createdByUser != null) {
            map.put("createdByUser", Collections.singletonMap("user.id", createdByUser.getId()));
        }
        if (actionConfiguration != null) {
            map.put("actionConfiguration", actionConfiguration.publicMap());
        }
        return map;
    }
}

