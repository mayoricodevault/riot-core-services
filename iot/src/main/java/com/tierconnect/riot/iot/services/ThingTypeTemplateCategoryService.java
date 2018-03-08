package com.tierconnect.riot.iot.services;

import com.tierconnect.riot.iot.entities.QThingTypeTemplateCategory;
import com.tierconnect.riot.iot.entities.ThingTypeTemplate;
import com.tierconnect.riot.iot.entities.ThingTypeTemplateCategory;

import javax.annotation.Generated;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class ThingTypeTemplateCategoryService extends ThingTypeTemplateCategoryServiceBase {

    public ThingTypeTemplateCategory getByCode(String codeCategory) {
        return getThingTypeTemplateCategoryDAO().getQuery()
                .where(QThingTypeTemplateCategory.thingTypeTemplateCategory.code.eq(codeCategory))
                .uniqueResult(QThingTypeTemplateCategory.thingTypeTemplateCategory);
    }

    public List<Map<String, Object>> listCategoryWithTemplates() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<ThingTypeTemplateCategory> categoryList = getThingTypeTemplateCategoryDAO().getQuery()
                .orderBy(QThingTypeTemplateCategory.thingTypeTemplateCategory.displayOrder.asc())
                .list(QThingTypeTemplateCategory.thingTypeTemplateCategory);

        for (ThingTypeTemplateCategory category : categoryList) {
            List<Map<String, Object>> templateList = new ArrayList<>();
            for (ThingTypeTemplate template : category.getThingTypeTemplateListSort()) {
                Map<String, Object> templateMap = template.publicMap();
                templateMap.remove("autoCreate");
                templateMap.remove("description");
                templateList.add(templateMap);
            }
            Map<String, Object> publicMap = category.publicMap();
            publicMap.put("templateList", templateList);
            result.add(publicMap);
        }
        return result;
    }
}

