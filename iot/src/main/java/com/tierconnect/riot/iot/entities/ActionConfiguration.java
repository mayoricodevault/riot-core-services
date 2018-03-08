package com.tierconnect.riot.iot.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import javax.annotation.Generated;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.io.IOException;
import java.util.Map;

@Entity
@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class ActionConfiguration extends ActionConfigurationBase {

    private static Logger logger = Logger.getLogger(ActionConfiguration.class);

    @Transient
    private Map<String, Object> configurationMap = null;

    @Transient
    private Integer displayOrder = null;

    public void mapConfiguration() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (getConfiguration() != null) {
                configurationMap = mapper.readValue(getConfiguration(), Map.class);
            }
        } catch (IOException e) {
            logger.error("error parsing action configurarion configurationMap:", e);
        }
    }

    @Override
    public Map<String, Object> publicMap() {
        mapConfiguration();
        Map<String, Object> publicMap = super.publicMap();
        publicMap.remove("code");
        publicMap.put("displayOrder", 0);
        if (displayOrder != null) {
            publicMap.put("displayOrder", displayOrder);
        }
        if (group != null) {
            publicMap.put("group.id", group.getId());
        }
        if (configurationMap != null) {
            publicMap.put("configuration", configurationMap);
        }
        return publicMap;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}

