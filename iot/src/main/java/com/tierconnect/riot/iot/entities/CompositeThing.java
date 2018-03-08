package com.tierconnect.riot.iot.entities;

import com.tierconnect.riot.appcore.entities.Group;

import java.util.*;

/**
 * Created by user on 1/20/15.
 */


public class CompositeThing {
    private Thing thing;
    private Thing parent;

    public List<Thing> getChildren() {
        return children;
    }

    public void setChildren(List<Thing> children) {
        this.children = children;
    }

    public Thing getParent() {
        return parent;
    }

    public void setParent(Thing parent) {
        this.parent = parent;
    }

    public Thing getThing() {
        return thing;
    }

    public void setThing(Thing thing) {
        this.thing = thing;
    }

    private List<Thing> children;

    public CompositeThing(Thing thing) {
        if(thing.getParent() != null) {
            this.parent = thing.getParent();
        }else {
            this.parent = thing;
        }
        this.thing = thing;
//        this.children = ThingService.getChildrenList(thing);
    }

    public CompositeThing(Thing thing, List<Thing> thingList) {
        this(thing);
        this.children = thingList;
    }

    public ThingTypeField getThingTypeFieldByName(String propertyName) {
        ThingTypeField thingTypeField = thing.getThingTypeField(propertyName);
        if(thingTypeField == null && children != null && children.size() > 0) thingTypeField = children.get(0) != null ? children.get(0).getThingTypeField(propertyName) : null;
        if(thingTypeField == null && parent != null) thingTypeField = parent.getThingTypeField(propertyName);
        return thingTypeField;
    }

    public Thing getFirstChild() {
        if(this.children != null && this.children.size() > 0) return children.get(0);
        return null;
    }

    public static Map<Long, Thing> getChildrenMap(List<CompositeThing> compositeThingList) {
        Map<Long, Thing> childrenMap = new HashMap<>();
        for(CompositeThing compositeThing : compositeThingList) {
            childrenMap.put(compositeThing.getParent().getId(), compositeThing.getFirstChild());
        }
        return childrenMap;
    }

    public void addChild(Thing child) {
        if(child == null) return ;
        if(children != null) {
            children.add(child);
        }else {
            children = new LinkedList<>();
            children.add(child);
        }
    }
    public List<ThingTypeField> getThingTypeFields() {
        Set<ThingTypeField> listThingFields = new LinkedHashSet<>();
        if(thing != null) {
            listThingFields.addAll(thing.getThingType().getThingTypeFields());
        }
        if(parent != null) {
            listThingFields.addAll(parent.getThingType().getThingTypeFields());
        }
        if(children != null) {
            for(Thing childThing : children) {
                listThingFields.addAll(childThing.getThingType().getThingTypeFields());
            }
        }
        return new LinkedList<>(listThingFields);
    }

    //Get Specific thingField

    public boolean hasThingFieldId(Long thingTypeFieldId) {
        if(this.parent != null) {
            ThingTypeField thingField = this.parent.getThingTypeFieldFromId(thingTypeFieldId);
            if(thingField != null) return true;
        }
        if(this.thing != null) {
            ThingTypeField thingField = this.thing.getThingTypeFieldFromId(thingTypeFieldId);
            if(thingField != null) return true;
        }
        if(this.children != null) {
            for(Thing child : this.children) {
                ThingTypeField thingField = child.getThingTypeFieldFromId(thingTypeFieldId);
                if(thingField != null) return true;
            }
        }return false;
    }

    public List<ThingTypeField> getThingFieldByType(Long thingFieldType) {
        List<ThingTypeField> thingTypeFieldsToReturn = new LinkedList<>();
        if(this.parent != null) {
            List<ThingTypeField> thingField = this.parent.getThingTypeFieldByType(thingFieldType);
            if(thingField != null) thingTypeFieldsToReturn.addAll(thingField);
        }
        if(this.thing != null) {
            List<ThingTypeField> thingField = this.thing.getThingTypeFieldByType(thingFieldType);
            if(thingField != null) thingTypeFieldsToReturn.addAll(thingField);
        }
        if(this.children != null) {
            for(Thing child : this.children) {
                List<ThingTypeField> thingField = child.getThingTypeFieldByType(thingFieldType);
                if(thingField != null) thingTypeFieldsToReturn.addAll(thingField);
            }
        }
        return thingTypeFieldsToReturn;
    }

    public ThingTypeField getThingTypeField(String thingFieldName)
    {
        if(this.parent != null) {
            ThingTypeField thingField = this.parent.getThingTypeField(thingFieldName);
            if(thingField != null) return thingField;
        }
        if(this.thing != null) {
            ThingTypeField thingField = this.thing.getThingTypeField(thingFieldName);
            if(thingField != null) return thingField;
        }
        if(this.children != null) {
            for(Thing child : this.children) {
                ThingTypeField thingField = child.getThingTypeField(thingFieldName);
                if(thingField != null) return thingField;
            }
        }
        return null;
    }

    public boolean containThingTypeId(Long thingTypeId) {
        if(this.parent != null && this.parent.getThingType().getId().equals(thingTypeId)) {
            return true;
        }
        if(this.thing != null && this.thing.getThingType().getId().equals(thingTypeId)) {
            return true;
        }
        if(this.children != null) {
            for(Thing child : this.children) {
                if(child != null && child.getThingType().getId().equals(thingTypeId)) {
                    return true;
                }
            }
        }return false;
    }

    public boolean containThingId(Long thingId) {
        if(this.parent != null && this.parent.getId().equals(thingId)) {
            return true;
        }
        if(this.thing != null && this.thing.getId().equals(thingId)) {
            return true;
        }
        if(this.children != null) {
            for(Thing child : this.children) {
                if(child != null && child.getId().equals(thingId)) {
                    return true;
                }
            }
        }return false;
    }

    public boolean onlyHasThingTypeId(Long thingTypeId) {
        if(this.parent != null && !this.parent.getThingType().getId().equals(thingTypeId)) {
            return false;
        }
        if(this.thing != null && !this.thing.getThingType().getId().equals(thingTypeId)) {
            return false;
        }
        if(this.children != null) {
            for(Thing child : this.children) {
                if(child != null && !child.getThingType().getId().equals(thingTypeId)) {
                    return false;
                }
            }
        }return true;
    }

    public boolean hasThingField( String thingFieldName )
    {
        if(this.parent != null) {
            boolean hasThingField = this.parent.hasThingTypeField(thingFieldName);
            if( hasThingField ) return hasThingField;
        }
        if(this.thing != null) {
            boolean hasThingField = this.thing.hasThingTypeField(thingFieldName);
            if(hasThingField) return hasThingField;
        }
        if(this.children != null) {
            for(Thing child : this.children) {
                boolean hasThingField = child.hasThingTypeField(thingFieldName);
                if(hasThingField) return hasThingField;
            }
        }
        return false;
    }

    public ThingType getThingType() {
        return thing.getThingType();
    }

    public Long getId() {
        return thing.getId();
    }

    public Group getGroup() {
        return thing.getGroup();
    }

    public String getName() {
        return thing.getName();
    }

    public String getNameParent() {
        if(this.parent != null) return this.parent.getName();
        return thing.getName();
    }

    public String getSerialParent() {
        if(this.parent != null) return this.parent.getSerial();
        return thing.getSerial();
    }

    public String getName( Long thingTypeId ) {
        if(thingTypeId == 0) return getNameParent();
        if(this.parent != null && this.parent.getThingType().getId().equals( thingTypeId )) return this.parent.getName();
        if(this.thing.getThingType().getId().equals(thingTypeId)) return this.getName();
        if(this.children != null) {
            for (Thing child : this.getChildren()) {
                if(child!= null && child.getThingType().getId().equals(thingTypeId)) {
                    return child.getName();
                }
            }
        }
        return "";
    }

    public String getSerial( Long thingTypeId ) {
        if(thingTypeId == 0) return getSerialParent();
        if(this.parent != null && this.parent.getThingType().getId().equals( thingTypeId )) return this.parent.getSerial();
        if(this.thing.getThingType().getId().equals(thingTypeId)) return this.getSerial();
        if(this.children != null) {
            for (Thing child : this.getChildren()) {
                if(child!= null && child.getThingType().getId().equals(thingTypeId)) {
                    return child.getSerial();
                }
            }
        }
        return "";
    }

    public String getSerial() {
        return thing.getSerial();
    }

}
