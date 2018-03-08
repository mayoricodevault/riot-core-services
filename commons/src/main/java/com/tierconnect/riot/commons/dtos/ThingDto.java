package com.tierconnect.riot.commons.dtos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.tierconnect.riot.commons.serializers.ThingDeserializer;
import org.apache.log4j.Logger;
import org.bson.Document;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ThingDto class.
 *
 * @author jantezana
 * @author achambi
 * @version 2017/01/25
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = ThingDeserializer.class)
// TODO: Deprecated the serializer because only is used to serialize the dates.
//@JsonSerialize(using = ThingSerializer.class)
@JsonPropertyOrder(value = {"meta", "id", "serialNumber", "name", "createdTime", "modifiedTime", "time", "group", "thingType", "properties"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ThingDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private Logger logger = Logger.getLogger( ThingDto.class );

    public Long id;
    public String serialNumber;
    public String name;
    public Date createdTime;
    public Date modifiedTime;
    public Date time;
    public GroupDto group;
    public ThingTypeDto thingType;
    public List<Map<String, ThingPropertyDto>> properties;
    public MetaDto meta;

    public Long getId() {
        return id;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getName() {
        return name;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public Date getModifiedTime() {
        return modifiedTime;
    }

    public Date getTime() {
        return time;
    }

    public GroupDto getGroup() {
        return group;
    }

    public ThingTypeDto getThingType() {
        return thingType;
    }

    public MetaDto getMeta() {
        return meta;
    }

    /**
     * Static method to create a instance of message wrapper.
     *
     * @param json A json string with contains the message.
     * @return instance of message wrapper.
     * @throws IOException input or output exception.
     */
    public static ThingDto parse(byte[] json)
    throws IOException {
        if (json == null) {
            return null;
        }
        ObjectMapper m = new ObjectMapper();
        return m.readValue(json, ThingDto.class);
    }

    public static ThingDto parse(String json)
    throws IOException {
        if (json == null) {
            return null;
        }
        ObjectMapper m = new ObjectMapper();
        return m.readValue(json, ThingDto.class);
    }

    public static String toJsonString(ThingDto tw)
    throws IOException {
        ObjectMapper m = new ObjectMapper();
        return m.writeValueAsString(tw);
    }

    /**
     * Gets a udf by name from current.
     *
     * @param name the value of name
     * @return the thing property DTO
     */
    public ThingPropertyDto getUdf(final String name) {
        Preconditions.checkNotNull(name, "The name is null");
        ThingPropertyDto thingPropertyDto = null;
        if (this.properties != null) {
            // Gets the current values. (the current values are saved in the index 0).
            Map<String, ThingPropertyDto> thingPropertyDtoMap = properties.get(0);

            if (thingPropertyDtoMap != null) {
                thingPropertyDto = thingPropertyDtoMap.get(name);
            }
        }

        return thingPropertyDto;
    }

    /**
     * Gets a udf by name from previous.
     *
     * @param name the value of name
     * @return the thing property DTO
     */
    public ThingPropertyDto getPreviousUdf(final String name) {
        Preconditions.checkNotNull(name, "The name is null");
        ThingPropertyDto thingPropertyDto = null;
        if (this.properties != null && this.properties.size()>1) {
            Map<String, ThingPropertyDto> thingPropertyDtoMap = properties.get(1);
            if (thingPropertyDtoMap != null) {
                thingPropertyDto = thingPropertyDtoMap.get(name);
            }
        }

        return thingPropertyDto;
    }

    /**
     * Adds a current property.
     *
     * @param key the key
     * @param thingPropertyDto the thing property DTO
     */
    public void addCurrentProperty(String key, ThingPropertyDto thingPropertyDto) {
        Preconditions.checkNotNull(key, "the key is null");
        Preconditions.checkNotNull(thingPropertyDto, "the thingPropertyDto is null");
        this.properties.get(0).put(key, thingPropertyDto);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ThingDto thingDto = (ThingDto) o;
        return Objects.equal(id, thingDto.id) && Objects.equal(serialNumber, thingDto.serialNumber) ;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, serialNumber);
    }

    /**
     * Gets a mongo document to be used by mongo ingestor
     * @return              Mongo Document
     * @throws Exception    When group, groupType or thingType are null
     */
    public Document toDocument() throws Exception {
        if (group == null || group.groupType == null || thingType == null){
            throw new Exception("Cannot parse ThingDTO.");
        }
        return  new Document("_id", id)
            .append("serialNumber", serialNumber)
            .append("name", name)
            .append("createdTime", createdTime)
            .append("modifiedTime", modifiedTime)
            .append("time", time)
            .append("groupId", group.id)
            .append("groupName", group.name)
            .append("groupCode", group.code)
            .append("groupTypeId", group.groupType.id)
            .append("groupTypeName", group.groupType.name)
            .append("groupTypeCode", group.groupType.code)
            .append("thingTypeId", thingType.id)
            .append("thingTypeName", thingType.name)
            .append("thingTypeCode", thingType.code)
        ;
    }

    public boolean hasPreviousUdfs(){
        return properties.size() > 1;
    }

    public boolean currentValueHasProperty(String fieldName){
        boolean  hasProperty = false;
        if (properties.get(0).containsKey(fieldName)){
            hasProperty = true;
        }
        return hasProperty;
    }

    public boolean previousValueHasProperty(String fieldName){
        boolean  hasProperty = false;
        if (properties.size()>1 && properties.get(1).containsKey(fieldName)){
            hasProperty = true;
        }
        return hasProperty;
    }

/*    public void putUdf(String udfName, ThingPropertyDto udf) {
        Preconditions.checkNotNull(udfName, "fieldName is null");
        Preconditions.checkNotNull(udf, "value for field is null");

        ThingPropertyDto currentUdf = getUdf(udfName);
        boolean valueHasChanged = currentUdf == null || !currentUdf.value.equals(udf.value);
        if( valueHasChanged ) {
            boolean hasBlinked = currentUdf != null && currentUdf.blinked;
            boolean hasPreviouslyChangedByARule = currentUdf != null && currentUdf.ruleChanged;
            boolean isNewBlink = meta.newBlink;
            //if( hasBlinked && !hasPreviouslyChangedByARule && !isNewBlink ) {
            if( !hasPreviouslyChangedByARule && !isNewBlink ) {
                if(!hasPreviousUdfs()) {
                    properties.add(new HashMap<>());
                }
                properties.get(1).put(udfName,currentUdf);
            }
            udf.modified = true;
            udf.ruleChanged = true;
            updateUdf(udf);
            properties.get(0).put(udfName,udf);
        }
    }*/


    public void putUdf(String udfName, ThingPropertyDto udf){
        Preconditions.checkNotNull(udfName, "fieldName is null");
        Preconditions.checkNotNull(udf, "value for field is null");

        ThingPropertyDto currentUdf = getUdf(udfName);
        // CASE 1: Udf does not exists in current and was added by a rule
        if( currentUdf == null || meta.newBlink){
            udf.modified = true;
            udf.blinked = true;
            udf.ruleChanged = true;
//            updateUdf(udf,currentUdf);
            if( udf.value instanceof ZoneDto ){
                ZoneDto zone = (ZoneDto) udf.value;
                zone.facilityMap.modified = true;
                zone.facilityMap.blinked = true;
                zone.facilityMap.time = udf.time;

                zone.zoneGroup.modified = true;
                zone.zoneGroup.blinked = true;
                zone.zoneGroup.time = udf.time;

                zone.zoneType.modified = true;
                zone.zoneType.blinked = true;
                zone.zoneType.time = udf.time;
            }
            properties.get(0).put(udfName,udf);
            return;
        }

        // CASE 2: Input udf value is different of current value
        if( !currentUdf.value.equals(udf.value) ) {
            udf.modified = true;
            udf.blinked = true;
            // CASE 3: First time that udf is changed by a rule and thing is not new
            if (!currentUdf.ruleChanged) {
                if (!hasPreviousUdfs()) {
                    properties.add(new HashMap<>());
                }
                properties.get(1).put(udfName, currentUdf);
            }

            if (udf.value instanceof ZoneDto) {
                ZoneDto zone = (ZoneDto) udf.value;
                ZoneDto previousZone = (ZoneDto) currentUdf.value;

                boolean hasChangedFacilityMap = !zone.facilityMap.name.equals(previousZone.facilityMap.name);
                zone.facilityMap.modified = hasChangedFacilityMap;
                zone.facilityMap.blinked = true;
                zone.facilityMap.time = hasChangedFacilityMap ? udf.time : previousZone.facilityMap.time;

                boolean hasChangedZoneType = !zone.zoneType.code.equals(previousZone.zoneType.code);
                zone.zoneType.modified = hasChangedZoneType;
                zone.zoneType.blinked = true;
                zone.zoneType.time = hasChangedZoneType ? udf.time : previousZone.zoneType.time;

                boolean hasChangedZoneGroup = !zone.zoneGroup.name.equals(previousZone.zoneGroup.name);
                zone.zoneGroup.modified = hasChangedZoneGroup;
                zone.zoneGroup.blinked = true;
                zone.zoneGroup.time = hasChangedZoneGroup ? udf.time : previousZone.zoneGroup.time;

                udf.value = zone;
            }

            udf.ruleChanged = true;
            properties.get(0).put(udfName, udf);
//            updateUdf(udf,currentUdf);
        } else if (udf.value instanceof ZoneDto && !currentUdf.ruleChanged) {
            ZoneDto zone = (ZoneDto) udf.value;
            ZoneDto previousZone = (ZoneDto) currentUdf.value;

            boolean hasChangedFacilityMap = !zone.facilityMap.name.equals(previousZone.facilityMap.name);
            previousZone.facilityMap.modified = hasChangedFacilityMap;
            previousZone.facilityMap.blinked = true;
            previousZone.facilityMap.time = hasChangedFacilityMap ? udf.time : previousZone.facilityMap.time;

            boolean hasChangedZoneType = !zone.zoneType.code.equals(previousZone.zoneType.code);
            previousZone.zoneType.modified = hasChangedZoneType;
            previousZone.zoneType.blinked = true;
            previousZone.zoneType.time = hasChangedZoneType ? udf.time : previousZone.zoneType.time;

            boolean hasChangedZoneGroup = !zone.zoneGroup.name.equals(previousZone.zoneGroup.name);
            previousZone.zoneGroup.modified = hasChangedZoneGroup;
            previousZone.zoneGroup.blinked = true;
            previousZone.zoneGroup.time = hasChangedZoneGroup ? udf.time : previousZone.zoneGroup.time;

            // completes mongo zone
            previousZone.facilityMap.id = previousZone.facilityMap.id == null ? zone.facilityMap.id: previousZone.facilityMap.id;
            previousZone.facilityMap.description = previousZone.facilityMap.description == null ? zone.facilityMap.description : previousZone.facilityMap.description;
            previousZone.facilityMap.modified = previousZone.facilityMap.modified == null ? zone.facilityMap.modified : previousZone.facilityMap.modified;
            previousZone.facilityMap.lonOrigin = previousZone.facilityMap.lonOrigin == null  ? zone.facilityMap.lonOrigin : previousZone.facilityMap.lonOrigin;
            previousZone.facilityMap.latOrigin = previousZone.facilityMap.latOrigin == null ? zone.facilityMap.latOrigin : previousZone.facilityMap.latOrigin;
            previousZone.facilityMap.altOrigin = previousZone.facilityMap.altOrigin == null ? zone.facilityMap.altOrigin : previousZone.facilityMap.altOrigin;
            previousZone.facilityMap.declination = previousZone.facilityMap.declination == null ? zone.facilityMap.declination : previousZone.facilityMap.declination;
            previousZone.facilityMap.imageWidth = previousZone.facilityMap.imageWidth == null ? zone.facilityMap.imageWidth : previousZone.facilityMap.imageWidth;
            previousZone.facilityMap.imageHeight = previousZone.facilityMap.imageHeight == null ? zone.facilityMap.imageHeight : previousZone.facilityMap.imageHeight;
            previousZone.facilityMap.xNominal = previousZone.facilityMap.xNominal == null ? zone.facilityMap.xNominal : previousZone.facilityMap.xNominal;
            previousZone.facilityMap.yNominal = previousZone.facilityMap.yNominal == null ? zone.facilityMap.yNominal : previousZone.facilityMap.yNominal;
            previousZone.facilityMap.latOriginNominal = previousZone.facilityMap.latOriginNominal == null ? zone.facilityMap.latOriginNominal : previousZone.facilityMap.latOriginNominal;
            previousZone.facilityMap.lonOriginNominal = previousZone.facilityMap.lonOriginNominal == null ? zone.facilityMap.lonOriginNominal : previousZone.facilityMap.lonOriginNominal;
            previousZone.facilityMap.imageUnit = previousZone.facilityMap.imageUnit == null ? zone.facilityMap.imageUnit : previousZone.facilityMap.imageUnit;
            previousZone.facilityMap.lonmin = previousZone.facilityMap.lonmin == null ? zone.facilityMap.lonmin : previousZone.facilityMap.lonmin;
            previousZone.facilityMap.lonmax = previousZone.facilityMap.lonmax == null ? zone.facilityMap.lonmax : previousZone.facilityMap.lonmax;
            previousZone.facilityMap.latmin = previousZone.facilityMap.latmin == null ? zone.facilityMap.latmin : previousZone.facilityMap.latmin;
            previousZone.facilityMap.latmax = zone.facilityMap.latmax;
            previousZone.zoneType.id = previousZone.zoneType.id == null ? zone.zoneType.id : previousZone.zoneType.id;
            previousZone.zoneGroup.id = previousZone.zoneGroup.id == null ? zone.zoneGroup.id : previousZone.zoneGroup.id;
            previousZone.zonePoints = previousZone.zonePoints == null ? zone.zonePoints : previousZone.zonePoints;
            previousZone.zoneProperties = previousZone.zoneProperties == null ? zone.zoneProperties : previousZone.zoneProperties;

            currentUdf.blinked = udf.blinked;
            currentUdf.timeSeries = udf.timeSeries;
            currentUdf.modified = false;
            currentUdf.value = previousZone;

            properties.get(0).put(udfName, currentUdf);
        }

    }

/*
    public void putUdf(String udfName, ThingPropertyDto udf) {
        Preconditions.checkNotNull(udfName, "fieldName is null");
        Preconditions.checkNotNull(udf, "value for field is null");

        ThingPropertyDto currentUdf = getUdf(udfName);
        boolean valueHasChanged = currentUdf == null || !currentUdf.value.equals(udf.value);

        if (valueHasChanged) {
            // boolean hasBlinked = currentUdf != null && currentUdf.blinked;
            boolean currentValueIsNotNull = currentUdf != null;
            boolean hasRuleChanged = currentUdf != null && currentUdf.ruleChanged;
            boolean isUpdate = !meta.newBlink;
            if (isUpdate && currentValueIsNotNull) {
                // allow to rotate if it is not a new blink.
                if (!hasRuleChanged ) {
                    // allow to rotate if the udf came came from a blink
//                    if (!hasBlinked) {
                        // allow to rotate if it is only the first time
                        // it is executing a rule
                        if (!hasPreviousUdfs() ) {
                            properties.add(new HashMap<>());
                        }
                        properties.get(1).put(udfName, currentUdf);
//                    }
                }
            }

            updateUdf(udf, currentUdf);
            udf.modified = true;
            udf.ruleChanged = true;
            udf.blinked = true;
            properties.get(0).put(udfName, udf);
        } else {
            // Update the current UDF
            updateCurrentUDF(udf, currentUdf);
        }
    }
*/
    /**
     * Update the UDF.
     *
     * @param udf the UDF to update
     */
    private void updateUdf(ThingPropertyDto udf, ThingPropertyDto currentUdf) {

        // VIZIX-2848  update the time of facility map, zone type and zone group.
        if( udf.value instanceof ZoneDto ) {
            ZoneDto zoneDto = (ZoneDto) udf.value;

            boolean hasChangedFacilityMap = true;
            boolean hasChangedZoneType = true;
            boolean hasChangedZoneGroup = true;
            if (currentUdf != null) {
                ZoneDto currentZone = (ZoneDto) currentUdf.value;
                hasChangedFacilityMap = !zoneDto.facilityMap.equals(currentZone.facilityMap);
                hasChangedZoneGroup = !zoneDto.zoneGroup.equals(currentZone.zoneGroup);
                hasChangedZoneType = !zoneDto.zoneType.equals(currentZone.zoneType);

                zoneDto.facilityMap.time = (hasChangedFacilityMap) ? udf.time : currentUdf.time;
                zoneDto.zoneType.time = (hasChangedZoneType) ? udf.time : currentUdf.time;
                zoneDto.zoneGroup.time = (hasChangedZoneGroup) ? udf.time : currentUdf.time;
            } else {
                zoneDto.facilityMap.time = udf.time;
                zoneDto.zoneType.time = udf.time;
                zoneDto.zoneGroup.time = udf.time;
            }

            zoneDto.facilityMap.modified = hasChangedFacilityMap;
            zoneDto.facilityMap.blinked = udf.blinked;
            zoneDto.zoneType.modified = hasChangedZoneType;
            zoneDto.zoneType.blinked = udf.blinked;
            zoneDto.zoneGroup.modified = hasChangedZoneGroup;
            zoneDto.zoneGroup.blinked = udf.blinked;
        }
    }

    private void updateCurrentUDF(ThingPropertyDto udf, ThingPropertyDto currentUdf ) {

        // VIZIX-2848  update the time of facility map, zone type and zone group.
        if( udf.value instanceof ZoneDto ) {
            ZoneDto zoneDto = (ZoneDto) udf.value;

            if (currentUdf != null) {
                ZoneDto currentZone = (ZoneDto) currentUdf.value;
                boolean hasChangedFacilityMap = !zoneDto.facilityMap.equals(currentZone.facilityMap);
                boolean hasChangedZoneGroup = !zoneDto.zoneGroup.equals(currentZone.zoneGroup);
                boolean hasChangedZoneType = !zoneDto.zoneType.equals(currentZone.zoneType);

                currentZone.facilityMap = (hasChangedFacilityMap) ? zoneDto.facilityMap : currentZone.facilityMap;
                currentZone.facilityMap.time = (hasChangedFacilityMap) ? udf.time : currentZone.facilityMap.time;
                currentZone.facilityMap.modified = hasChangedFacilityMap;
                currentZone.facilityMap.blinked = currentUdf.blinked;
                currentZone.zoneType = (hasChangedZoneType) ? zoneDto.zoneType : currentZone.zoneType;
                currentZone.zoneType.time = (hasChangedZoneType) ? udf.time : currentZone.zoneType.time;
                currentZone.zoneType.blinked = currentUdf.blinked;
                currentZone.zoneType.modified = hasChangedZoneType;
                currentZone.zoneGroup = (hasChangedZoneGroup) ? zoneDto.zoneGroup : currentZone.zoneGroup;
                currentZone.zoneGroup.time = (hasChangedZoneGroup) ? udf.time : currentZone.zoneGroup.time;
                currentZone.zoneGroup.blinked = currentUdf.blinked;
                currentZone.zoneGroup.modified = hasChangedZoneGroup;
            }
        }
    }

    public ThingTypeFieldDto getThingTypeField(String key){
        return thingType.fields.get(key);
    }

}
