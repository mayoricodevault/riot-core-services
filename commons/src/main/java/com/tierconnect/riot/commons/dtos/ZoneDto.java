package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.Objects;
import org.bson.Document;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * ZoneDto class.
 *
 * @author jantezana
 * @author achambi
 * @version 2017/01/25
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ZoneDto implements Serializable {
    private static final long serialVersionUID = 1L;
    public Long id;
    public String name;
    public String code;
    public Long groupId;
    public FacilityMapDto facilityMap;
    public ZonePropertyDto zoneType;
    public ZonePropertyDto zoneGroup;
    public List<double[]> zonePoints;
    public Map<String, Object> zoneProperties;

    public Long getId(){
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public Long getGroupId() {
        return groupId;
    }

    public FacilityMapDto getFacilityMap() {
        return facilityMap;
    }

    public ZonePropertyDto getZoneType() {
        return zoneType;
    }

    public ZonePropertyDto getZoneGroup() {
        return zoneGroup;
    }

    public List<double[]> getZonePoints() {
        return zonePoints;
    }

    public Map<String, Object> getZoneProperties() {
        return zoneProperties;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ZoneDto zoneDto = (ZoneDto) o;
        return Objects.equal(id, zoneDto.id) /*&& Objects.equal(zoneGroup, zoneDto.zoneGroup)*/
                /* && ( zonePoints!=null && zoneDto.zonePoints!=null ? Arrays.deepEquals(zonePoints.toArray(), zoneDto.zonePoints.toArray()) : zonePoints==zoneDto.zonePoints );*/;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, zoneGroup, zonePoints);
    }

    /**
     * Gets a mongo document to be used by mongo ingestor
     * @return              Mongo Document
     * @throws Exception    When zoneGroup, zoneType or facilityMap are null
     */
    public Document toDocument() throws Exception {
        if (zoneGroup == null || zoneType == null || facilityMap == null){
            throw new Exception("Cannot parse ZoneDTO.");
        }
        return new Document("id", id)
                .append("code", code)
                .append("name", name)
                .append("zoneGroup", zoneGroup.name)
                .append("zoneGroupTime", zoneGroup.time)
                .append("zoneType", zoneType.code)
                .append("zoneTypeTime", zoneType.time)
                .append("facilityMap", facilityMap.name)
                .append("facilityMapTime", facilityMap.time)
        ;
    }
}
