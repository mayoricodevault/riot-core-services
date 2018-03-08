package com.tierconnect.riot.iot.entities;


import javax.persistence.Entity;
import java.util.Collections;
import java.util.List;

@Entity
public class ThingTypeTemplateCategory extends ThingTypeTemplateCategoryBase {

    public ThingTypeTemplateCategory() {
    }

    public ThingTypeTemplateCategory(String code, String name, Integer displayOrder, String pathIcon) {
        this.code = code;
        this.name = name;
        this.displayOrder = displayOrder;
        this.pathIcon = pathIcon;
    }

    public List<ThingTypeTemplate> getThingTypeTemplateListSort() {
        Collections.sort(thingTypeTemplateList);
        return thingTypeTemplateList;
    }

}

