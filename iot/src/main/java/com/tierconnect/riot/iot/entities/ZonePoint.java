package com.tierconnect.riot.iot.entities;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.annotation.Generated;

@Entity
@Table(name="zonepoint")
@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class ZonePoint extends ZonePointBase 
{
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ZonePointBase)) return false;

        ZonePointBase that = (ZonePointBase) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}

