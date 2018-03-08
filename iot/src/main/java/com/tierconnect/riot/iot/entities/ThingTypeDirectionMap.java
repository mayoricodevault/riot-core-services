package com.tierconnect.riot.iot.entities;


import java.math.BigInteger;

/**
 * Created by achambi on 7/21/17.
 * Meta-entity to verify path direction between thingTypes.
 * This Class is calling for hibernate via reflexion.
 */
@SuppressWarnings("unused")
public class ThingTypeDirectionMap {

    private Long thingTypeParentId;
    private String thingTypeParentCode;
    private Long thingTypeParentGroupId;
    private Long thingTypeChildId;
    private String thingTypeChildCode;
    private Long thingTypeChildGroupId;
    private String mapDirection;

    public ThingTypeDirectionMap() {
    }

    //region getters
    public Long getThingTypeParentId() {
        return thingTypeParentId;
    }

    public String getThingTypeParentCode() {
        return thingTypeParentCode;
    }

    public Long getThingTypeParentGroupId() {
        return thingTypeParentGroupId;
    }

    public Long getThingTypeChildId() {
        return thingTypeChildId;
    }

    public String getThingTypeChildCode() {
        return thingTypeChildCode;
    }

    public Long getThingTypeChildGroupId() {
        return thingTypeChildGroupId;
    }

    public String getMapDirection() {
        return mapDirection;
    }
    //endregion

    //region setters
    public void setThingTypeParentId(BigInteger thingTypeParentId) {
        this.thingTypeParentId = thingTypeParentId.longValue();
    }

    public void setThingTypeParentCode(String thingTypeParentCode) {
        this.thingTypeParentCode = thingTypeParentCode;
    }

    public void setThingTypeParentGroupId(BigInteger thingTypeParentGroupId) {
        this.thingTypeParentGroupId = thingTypeParentGroupId.longValue();
    }

    public void setThingTypeChildId(BigInteger thingTypeChildId) {
        this.thingTypeChildId = thingTypeChildId.longValue();
    }

    public void setThingTypeChildCode(String thingTypeChildCode) {
        this.thingTypeChildCode = thingTypeChildCode;
    }

    public void setThingTypeChildGroupId(BigInteger thingTypeChildGroupId) {
        this.thingTypeChildGroupId = thingTypeChildGroupId.longValue();
    }

    public void setMapDirection(String mapDirection) {
        this.mapDirection = mapDirection;
    }
    //endregion
}
