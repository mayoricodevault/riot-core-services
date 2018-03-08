package com.tierconnect.riot.iot.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 
 * @author garivera
 *
 */
@SuppressWarnings("JpaDataSourceORMInspection")
@Entity
@Table(name = "ThingType", indexes = {@Index(name = "IDX_thingtype_thingTypeCode", columnList = "thingTypeCode")})
@XmlRootElement(name = "ThingType")
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE)
public class ThingType extends ThingTypeBase implements com.tierconnect.riot.commons.entities.IThingType
{
    public enum NonUDF {
        id("id"),
        groupTypeCode("groupTypeCode"),
        groupTypeName("groupTypeName"),
        group("group"),
        groupCode("groupCode"),
        groupName("groupName"),
        thingType("thingType"),
        thingTypeCode("thingTypeCode"),
        thingTypeName("thingTypeName"),
        name("name"),
        serialNumber("serialNumber"),
        parent("parent"),
        children("children"),
        dwelltime("dwelltime"),
        timestamp("timestamp"),
        sqn("sqn"),
        specName("specName"),
        thingTypeFieldId("thingTypeFieldId"),
        time("time"),
        value("value"),
        code("code"),
        facilityMap("facilityMap"),
        zoneType("zoneType"),
        zoneGroup("zoneGroup"),
        facilityMapTime("facilityMapTime"),
        zoneGroupTime("zoneGroupTime"),
        facilityMapDwellTime("facilityMapDwellTime"),
        facilityMapChanged("facilityMapChanged"),
        zoneGroupDwellTime("zoneGroupDwellTime"),
        zoneGroupChanged("zoneGroupChanged"),
        zoneTypeDwellTime("zoneTypeDwellTime"),
        zoneTypeChanged("zoneTypeChanged"),
        _id("_id"),
        groupTypeId("groupTypeId"),
        groupId("groupId"),
        thingTypeId("thingTypeId"),
        modifiedTime("modifiedTime"),
        createdTime("createdTime"),
        // VIZIX-4339
        group_id("group.id"),
        relativeDate("relativeDate"),
        startDate("startDate"),
        endDate("endDate"),
        zoneType_id("zoneType.id"),
        zoneProperty_id("zoneProperty.id"),
        thingType_id("thingType.id");


        private String valueNonUDF;

        private NonUDF(String valueNonUDF) {
            this.valueNonUDF = valueNonUDF;
        }

        public String getValueNonUDF() {
            return valueNonUDF;
        }

        @Override
        public String toString() {
            return getValueNonUDF();
        }

        public static HashSet<String> getEnums() {
            HashSet<String> values = new HashSet<String>();
            for (ThingType.NonUDF nonUDF: ThingType.NonUDF.values()) {
                values.add(nonUDF.getValueNonUDF());
            }
            return values;
        }
    }

    public static final List<String> allowedSimpleExpressions = Collections.unmodifiableList(Arrays.asList(
            "color",
            "name",
            "serialNumber",
            "thingType.thingTypeCode",
            "thingType.name",
            // GROUP
            "group.code",
            "group.name",
            "group.groupType.code",
            "group.groupType.name",
            "group.archived",
            "group.code",
            "group.description",
            "group.id",
            "group.name",
            "group.treeLevel",
            "parent.grupo.archived",
            "parent.grupo.code",
            "parent.grupo.description",
            "parent.grupo.id",
            "parent.grupo.name",
            "parent.grupo.treeLevel",
            // LOGICAL
            "logical.code",
            "logical.id",
            "logical.name",
            "logical.x",
            "logical.y",
            "logical.z",
            "parent.logical.code",
            "parent.logical.id",
            "parent.logical.name",
            "parent.logical.x",
            "parent.logical.y",
            "parent.logical.z",
            // SHIFT
            "shift.active",
            "shift.code",
            "shift.daysOfWeek",
            "shift.endTimeOfDay",
            "shift.id",
            "shift.name",
            "shift.startTimeOfDay",
            "parent.shift.active",
            "parent.shift.code",
            "parent.shift.daysOfWeek",
            "parent.shift.endTimeOfDay",
            "parent.shift.id",
            "parent.shift.name",
            "parent.shift.startTimeOfDay",
            // ZONE
            "zone.code",
            "zone.facilityMap",
            "zone.id",
            "zone.name",
            "zone.zoneGroup",
            "parent.zone.code",
            "parent.zone.facilityMap",
            "parent.zone.id",
            "parent.zone.name",
            "parent.zone.zoneGroup",
            // PARENT/CHILD
            "parent.color",
            "parent.serialNumber",
            // NATIVE THING TYPE
            "asset.color",
            "asset.serialNumber",
            // TIMESTAMP/DWELLTIME
            "color.timestamp",
            "color.dwelltime",
            // SERIAL NUMBER FORMULA
            "AFAF0000000000FF${String:upperCase(String:leftPad(long:toHexString(DocumentId),8,\"0\"))}",
            //PARENT SHIPPING ORDER
            "parent.shippingOrderField",
            "parent.shippingOrderField.color",
            "parent.shippingOrderField.serialNumber",
            "asset.name",
            "asset.serialNumber",
            "asset.color",
            "null"
    ));

    public static final Map<String, List<String>> allowedFunctions = Collections.unmodifiableMap(
            new HashMap<String, List<String>>(){{
                put("currentUser", Arrays.asList("userActiveGroup", "groupCode", "activeGroupCode", "id", "username",
                        "firstName", "lastName", "email", "userGroup") );
            }});


    public ThingType() {
	    super();
	}

	public ThingType(String name) {
		this.name = name;
	}

	public ThingType(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	@Override
	public String getCode()
	{
		return this.thingTypeCode;
	}

    @Override
    public com.tierconnect.riot.commons.entities.IThingTypeField getThingTypeField(String s) {
        return getThingTypeFieldByName(s);
    }

    /**
     *
     * @param s
     * @return properties thing type field values
     */
    public ThingTypeField getAllThingTypeField(String s) {
        return getThingTypeFieldByName(s);
    }

    @Override
    public Boolean isThingTypeParent() {
        return isIsParent();
    }

    public Map<String, Object> publicMap(boolean fillFields, boolean fillChildren) {
        Map<String,Object> map = new HashMap<String,Object>();
        List<Map<String, Object>> fields = new LinkedList<>();

        if (fillFields) {
            if (this.thingTypeFields != null) {
                for (ThingTypeField thingTypeField : this.thingTypeFields) {
                    fields.add(thingTypeField.publicMap());
                }
            }
        }

        if (fillChildren) {
            List<Map<String, Object>> childrenMaps = new LinkedList<>();
            for (ThingType child : getChildren()) {
                childrenMaps.add(child.publicMap(true, false));
            }
            map.put("children", childrenMaps);
        }

        map.put("id",this.id);
        map.put("name",this.name);
        map.put("archived",this.archived);
        map.put("fields", fields);
        map.put("thingTypeCode", this.getThingTypeCode());
        map.put("autoCreate", this.isAutoCreate());
        map.put("group", this.group.publicMap());
        map.put("serialFormula", this.serialFormula);
        List<Map<String, Object>> parentsMaps = new LinkedList<>();
        for (ThingType parent : getParents()) {
           parentsMaps.add(parent.publicMap(true, false));
        }
        map.put("parents", parentsMaps);
        return map;
    }

    public Map<String, Object> publicMapTreeView(boolean fillFields) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", this.id);
        map.put("name", this.name);
        map.put("thingTypeCode", this.getThingTypeCode());
        if (fillFields) {
            List<Map<String, Object>> fields = new LinkedList<>();
            if (this.thingTypeFields != null) {
                for (ThingTypeField thingTypeField : this.thingTypeFields) {
                    fields.add(thingTypeField.getThingTypeFieldMap());
                }
                map.put("fields", fields);
            }
        }
        return map;
    }

    public List<ThingType> getParents() {
        List<ThingType> result = new ArrayList<>();
        if (this.getParentTypeMaps() != null) {
            for (ThingTypeMap thingTypeMap : this.getParentTypeMaps()) {
                if (thingTypeMap.getParent() != null) {
                    result.add(thingTypeMap.getParent());
                }
            }
        }
        return result;
    }

    public List<ThingType> getChildren() {
        List<ThingType> result = new ArrayList<>();
        if (this.getChildrenTypeMaps() != null) {
            for (ThingTypeMap thingTypeMap : this.getChildrenTypeMaps()) {
                if (thingTypeMap.getChild() != null) {
                    result.add(thingTypeMap.getChild());
                }
            }
        }
        return result;
    }

    /**
     * Update fields with the values from the map
     *
     * @param thingTypeMap new values to update
     */
/*    public void updateFields(List<Map<String, Object>> thingTypeMap) {
        //first we remove
        for (Iterator<ThingTypeField> i = thingTypeFields.iterator(); i.hasNext();)
        {
            ThingTypeField element = i.next();
            boolean found = false;

            for (Map<String, Object> stringObjectMap : thingTypeMap)
            {
                Integer id = (Integer) stringObjectMap.get("id");
                if (id != null && element.getId().equals(id.longValue()))
                {
                    found = true;
                }
            }
            if (!found) {
                i.remove();
            }
        }

        //now we update or insert.
        for (Map<String, Object> stringObjectMap : thingTypeMap)
        {
            ThingTypeField ttf = null;
            String data = null;
            try{
                data = (String) stringObjectMap.get("defaultValue");
            }catch(Exception e)
            {
                data = "";
                ArrayList<String> aa = (ArrayList) stringObjectMap.get("defaultValue");
                for(String a : aa)
                {
                    data = data+a+",";
                }
                if(data!=null && data.length()>0)
                {
                    data = data.substring(0, data.length()-1);
                }
            }
            Integer id = (Integer) stringObjectMap.get("id");
            if (id != null)
            {
                ttf = getThingTypeFieldById(id.longValue());
                ttf.setName((String) stringObjectMap.get("name"));
                ttf.setSymbol((String) stringObjectMap.get("symbol"));
                ttf.setUnit((String) stringObjectMap.get("unit"));
                ttf.setType(Long.parseLong(stringObjectMap.get("type").toString()));
                ttf.setTypeParent((String) stringObjectMap.get("typeParent"));
                ttf.setMultiple((Boolean) stringObjectMap.get("multiple"));
                ttf.setTimeSeries((Boolean) stringObjectMap.get("timeSeries"));
                if(stringObjectMap.get("thingTypeFieldTemplateId")!=null)
                {
                    ttf.setThingTypeFieldTemplateId(Long.parseLong(stringObjectMap.get("thingTypeFieldTemplateId").toString()));
                }
                ttf.setDefaultValue(data);
            }
            else
            {
                ttf = new ThingTypeField(
                        (String) stringObjectMap.get("name"),
                        (String) stringObjectMap.get("unit"),
                        (String) stringObjectMap.get("symbol"),
                        (Integer) stringObjectMap.get("type"),
                        (String) stringObjectMap.get("typeParent"),
                        (Boolean) stringObjectMap.get("multiple"),
                        (Boolean) stringObjectMap.get("timeSeries"),
                        data);

                thingTypeFields.add(ttf);
                ttf.setThingType(this);
            }
        }
    }*/

    /**
     * Find a thing type field by id
     * @param thingTypeFieldId type id
     * @return thing type field that matches the id
     */
    public  ThingTypeField getThingTypeFieldById(Long thingTypeFieldId)
    {
        ThingTypeField thingTypeField = null;

        for( ThingTypeField aThingTypeField : this.thingTypeFields )
        {
            if( aThingTypeField.getId().equals(thingTypeFieldId) )
            {
                thingTypeField = aThingTypeField;
                break;
            }
        }
        return thingTypeField;
    }

    /**
     * Find a thing type field by name. Check in the current thing type. If checkChildren is true, check the
     * current thing type's children. The search on children stops on the first match.
     *
     * @param thingTypeFieldName thing type field name
     * @param checkChildren if true check on the children for marches
     *
     * @return thing type field that matches the name or null if no match
     */
    public  ThingTypeField getThingTypeFieldByName (String thingTypeFieldName, boolean checkChildren)
    {
        ThingTypeField thingTypeField = null;

        if (this.thingTypeFields != null) {
            for( ThingTypeField aThingTypeField : this.thingTypeFields )
            {
                if( null != aThingTypeField.getName() && aThingTypeField.getName().equals(thingTypeFieldName) )
                {
                    thingTypeField = aThingTypeField;
                    break;
                }
            }

            if (thingTypeField == null && checkChildren) {
                for (ThingType tt : getChildren()){
                    if (tt.hasField(thingTypeFieldName)) {
                        thingTypeField = tt.getThingTypeFieldByName(thingTypeFieldName);
                        break;
                    }
                }

            }
        }
        return thingTypeField;
    }

    /**
     * Find a thing type field by name in the current thing type
     * @param thingTypeFieldName thing type field name
     * @return thing type field that matches the name or null if no match
     */
    public  ThingTypeField getThingTypeFieldByName (String thingTypeFieldName) {
        return getThingTypeFieldByName(thingTypeFieldName, false);
    }



    /**
     * Find a thing type field by type
     * @param type
     * @return
     */
    public List<ThingTypeField> getThingTypeFieldsByType (Long type)
    {
        List<ThingTypeField> thingTypeFields = new ArrayList<ThingTypeField>();

        if (this.thingTypeFields != null) {
            thingTypeFields = this.thingTypeFields
                    .stream()
                    .filter(ttf -> ttf.isThisDataType(type))
                    .collect(Collectors.toList());

//            for (ThingTypeField thingTypeField : this.thingTypeFields) {
//                if (thingTypeField.isThisDataType(type)) {
//                    thingTypeFields.add(thingTypeField);
//                }
//            }
        }
        return thingTypeFields;
    }

    public boolean isChild(Long childId) {
        boolean found = false;
        List<ThingType> children = getChildren();
        for (ThingType child : children) {
            if(child.getId().equals(childId)) {
                found = true;
                break;
            }
        }
        return found;
    }


    public ThingTypeField getMatchingThingTypeUdf(Long thingTypeId) {
        ThingTypeField thingTypeField = null;
        Set<ThingTypeField> fields = getThingTypeFields();
        for (ThingTypeField field : fields) {
            if(field.getDataTypeThingTypeId() != null && field.getDataTypeThingTypeId().equals(thingTypeId)) {
                thingTypeField = field ;
                break;
            }
        }
        return thingTypeField;
    }

    public boolean hasMatchingThingTypeUdf(Long thingTypeId) {
        return getMatchingThingTypeUdf(thingTypeId) != null;
    }


    public boolean hasField(String name) {
        boolean hasField = false;

        if (this.thingTypeFields != null && name != null) {
            for (ThingTypeField thingTypeField : thingTypeFields) {
                if (name.equals(thingTypeField.getName())) {
                    hasField = true;
                    break;
                }
            }
        }
        return hasField;

    }

    /**
     * This method returns the thingTypeFields of type expression
     * @return
     */
    public List<ThingTypeField> getExpressionFields(){
        List<ThingTypeField> result = new ArrayList<>();
        for(ThingTypeField thingTypefield: this.getThingTypeFields()){
            if(thingTypefield.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_FORMULA.value) == 0){
                result.add(thingTypefield);
            }
        }
        return result;
    }

    /**
     * Checks if thingTypeFieldId is of a specific data Type
     * @param thingTypeFieldId ID of the field to compare
     * @param dataTypeId ID Data Type who has to evaluate
     * @return
     */
    public boolean isDataType(Long thingTypeFieldId, Long dataTypeId){
        boolean result = false;
        for(ThingTypeField fieldData: this.getThingTypeFields()){
            if( (thingTypeFieldId.compareTo(fieldData.getId())==0) &&
                    (fieldData.getDataType().getId().compareTo(dataTypeId)==0) ){
                result = true;
                break;
            }
        }
        return result;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ThingType)  || obj == null){
            return false;
        }
        ThingType other = (ThingType) obj;
        if((this.id == null && other.id != null)
                || (this.id != null && !this.id.equals(other.id))){
            return false;
        }

        return true;
    }
}
