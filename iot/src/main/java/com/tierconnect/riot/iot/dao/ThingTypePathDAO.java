package com.tierconnect.riot.iot.dao;

import com.mysema.query.jpa.JPQLQuery;

import com.tierconnect.riot.iot.entities.QThingTypePath;
import com.tierconnect.riot.iot.entities.ThingTypePath;

import javax.annotation.Generated;
import java.util.List;

@Generated("com.tierconnect.riot.appgen.service.GenDAO")
public class ThingTypePathDAO extends ThingTypePathDAOBase {

    /**
     * Get all the roads to the destination
     *
     * @param thingTypeId a {@link Long} containing the destiny.
     * @return a list of {@link ThingTypePath}
     */
    public List<ThingTypePath> getPaths() {
        return getQuery().list(QThingTypePath.thingTypePath);
    }

    /**
     * Get all the roads to the destination
     *
     * @param thingTypeId a {@link Long} containing the destiny.
     * @param jpqlQuery   a {@link JPQLQuery} instance
     * @return a list of {@link ThingTypePath}
     */
    public List<ThingTypePath> getPaths(JPQLQuery jpqlQuery) {
        QThingTypePath qThingTypePath = QThingTypePath.thingTypePath;
        jpqlQuery.from(qThingTypePath);
        return jpqlQuery.list(QThingTypePath.thingTypePath);
    }
}

