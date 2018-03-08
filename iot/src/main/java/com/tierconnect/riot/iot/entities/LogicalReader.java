package com.tierconnect.riot.iot.entities;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.annotation.Generated;
import java.util.HashMap;
import java.util.Map;

@Entity

@Table(name="logicalReader", uniqueConstraints={@UniqueConstraint(columnNames={"code","group_id"}), @UniqueConstraint(columnNames={"name","group_id"})})@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class LogicalReader extends LogicalReaderBase {

    public Map<String, Object> publicMapExtended() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", getId());
        map.put("name", getName());
        map.put("code", getCode());
        map.put("x", getX());
        map.put("y", getY());
        map.put("z", getZ());
        map.put("zoneInId", getZoneIn().getId());
        map.put("zoneOutId", getZoneOut().getId());
        return map;
    }

}

