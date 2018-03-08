package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;
import org.bson.Document;

import java.io.Serializable;

/**
 * LogicalReaderDto class.
 *
 * @author jantezana
 * @author achambi
 * @version 2017/01/25
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogicalReaderDto implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long id;
    public String name;
    public String code;
    public Long zoneInId;
    public Long zoneOutId;
    public String x;
    public String y;
    public String z;
    public Long groupId;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Long getZoneInId() {
        return zoneInId;
    }

    public Long getZoneOutId() {
        return zoneOutId;
    }

    public String getX() {
        return x;
    }

    public String getY() {
        return y;
    }

    public String getZ() {
        return z;
    }

    public Long getGroupId() {
        return groupId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        LogicalReaderDto that = (LogicalReaderDto) o;
        return Objects.equal(id, that.id) && Objects.equal(name, that.name) && Objects.equal(zoneInId, that.zoneInId) && Objects.equal(zoneOutId,
                                                                                                                                       that.zoneOutId)
            && Objects.equal(x, that.x) && Objects.equal(y, that.y) && Objects.equal(z, that.z);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, name, zoneInId, zoneOutId, x, y, z);
    }

    /**
     * Gets a mongo document to be used by mongo ingestor
     * @return              Mongo Document
     */
    public Document toDocument(){
        return new Document("id", id)
                .append("code", code)
                .append("name", name)
                .append("zoneInId", zoneInId)
                .append("zoneOutId", zoneOutId)
                .append("x", x)
                .append("y", y)
                .append("z", z)
                ;
    }
}
