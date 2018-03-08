package com.tierconnect.riot.iot.entities;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by pablo on 1/7/15.
 *
 * A thing that overcomes some limitations of the regular thing.
 */
public interface ThingEx {

    /**
     * @return map of all the fields and values.
     */
    public Map<String,Object> publicMap();


    public boolean hasParent();
}
