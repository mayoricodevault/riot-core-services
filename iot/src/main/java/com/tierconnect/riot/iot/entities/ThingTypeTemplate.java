package com.tierconnect.riot.iot.entities;

import com.tierconnect.riot.appcore.entities.Group;

import javax.annotation.Generated;
import javax.persistence.Entity;

@Entity
@Generated("com.tierconnect.riot.appgen.service.GenModel")
public class ThingTypeTemplate extends ThingTypeTemplateBase implements Comparable<ThingTypeTemplate> {

    public ThingTypeTemplate() {
    }

    public ThingTypeTemplate(String code, String name, boolean autoCreate, Integer displayOrder, String pathIcon,
                             String description, Group group, ThingTypeTemplateCategory thingTypeTemplateCategory) {
        this.code = code;
        this.name = name;
        this.autoCreate = autoCreate;
        this.displayOrder = displayOrder;
        this.pathIcon = pathIcon;
        this.description = description;
        this.group = group;
        this.thingTypeTemplateCategory = thingTypeTemplateCategory;
    }

    public ThingTypeTemplate(Integer displayOrder, String code, String name, String description, String pathIcon,
                             boolean autoCreate, Group group, ThingTypeTemplateCategory thingTypeTemplateCategory) {
        this.code = code;
        this.name = name;
        this.autoCreate = autoCreate;
        this.displayOrder = displayOrder;
        this.pathIcon = pathIcon;
        this.description = description;
        this.group = group;
        this.thingTypeTemplateCategory = thingTypeTemplateCategory;
    }

    @Override
    public int compareTo(ThingTypeTemplate thingTypeTemplate) {
        return this.getDisplayOrder().compareTo(thingTypeTemplate.getDisplayOrder());
    }
}

