package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.iot.entities.DataType;
import com.tierconnect.riot.iot.entities.QThingTypeFieldTemplate;
import com.tierconnect.riot.iot.entities.ThingTypeFieldTemplate;
import com.tierconnect.riot.iot.entities.ThingTypeTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ThingTypeFieldTemplateService extends ThingTypeFieldTemplateServiceBase {

    public List<ThingTypeFieldTemplate> getThingTypeFieldTemplateBy(ThingTypeTemplate thingTypeTemplate) {
        HibernateQuery query = ThingTypeFieldTemplateService.getThingTypeFieldTemplateDAO().getQuery();
        return query.where(QThingTypeFieldTemplate.thingTypeFieldTemplate.thingTypeTemplate.eq(thingTypeTemplate))
                    .list(QThingTypeFieldTemplate.thingTypeFieldTemplate);
    }

    public List<ThingTypeFieldTemplate> getThingTypeFielTemplatedByThingTypeTemplateId(Long thingTypeTemplateId) {
        HibernateQuery query = ThingTypeFieldTemplateService.getThingTypeFieldTemplateDAO().getQuery();
        return query.where(QThingTypeFieldTemplate.thingTypeFieldTemplate.thingTypeTemplate.id.eq(thingTypeTemplateId))
                    .list(QThingTypeFieldTemplate.thingTypeFieldTemplate);
    }

    /**
     * Get Thing Type Field Template Id By Thing Type Template
     * @param thingTypeTemplateId thing Type Template Id
     * @param name name
     * @return Thing Type Field Template
     */
    public ThingTypeFieldTemplate getThingTypeFieldTemplateByThingTypeTemplate(Long thingTypeTemplateId, String name) {
        HibernateQuery query = ThingTypeFieldTemplateService.getThingTypeFieldTemplateDAO().getQuery();
        return query.where(QThingTypeFieldTemplate.thingTypeFieldTemplate.thingTypeTemplate.id.eq(thingTypeTemplateId)
                .and(QThingTypeFieldTemplate.thingTypeFieldTemplate.name.eq(name)))
                .uniqueResult(QThingTypeFieldTemplate.thingTypeFieldTemplate);
    }

    /**
     * Get ThingTypeFieldTemplate
     * @param UserFieldname
     * @return
     */
    public ThingTypeFieldTemplate selectByName(String UserFieldname) {
        return getThingTypeFieldTemplateDAO().getQuery().where(QThingTypeFieldTemplate.thingTypeFieldTemplate.name
                .eq(UserFieldname)).uniqueResult(QThingTypeFieldTemplate.thingTypeFieldTemplate);
    }

    /**
     * return Fields
     * @param thingTypeTemplateId
     * @param labels
     * @return
     */
    public List<ThingTypeFieldTemplate> getThingTypeFielTemplatedByThingTypeTemplateIdAndExcludeLabels(Long thingTypeTemplateId, Set<String> labels) {
        HibernateQuery query = ThingTypeFieldTemplateService.getThingTypeFieldTemplateDAO().getQuery();
        BooleanBuilder b = new BooleanBuilder();
        b= b.and(QThingTypeFieldTemplate.thingTypeFieldTemplate.thingTypeTemplate.id.eq(thingTypeTemplateId));
        b= b.and(QThingTypeFieldTemplate.thingTypeFieldTemplate.name.notIn(labels));
        return query.where(b).list(QThingTypeFieldTemplate.thingTypeFieldTemplate);
    }
    /**
     *
     * @param thingTypeTemplate thing type template
     * @return a Set<String> of Label's Thing Type Field Template
     */
    public Set<String> getLabelsThingTypeFieldTemplate(ThingTypeTemplate thingTypeTemplate){
        Set<ThingTypeFieldTemplate> fields = thingTypeTemplate.getThingTypeFieldTemplate() != null ?
                thingTypeTemplate.getThingTypeFieldTemplate() : new HashSet<ThingTypeFieldTemplate>();
        Set<String> labels = new HashSet<>();
        for (ThingTypeFieldTemplate field : fields) {
            labels.add(field.getName());
        }
        return labels;
    }

    public ThingTypeFieldTemplate create(
            String udfName, String udfDescription, String udfUnit, String udfSymbol,
            DataType udfType, String udfTypeParent, boolean udfTimeSeries,
            ThingTypeTemplate thingTypeTemplate, String defaultValue){
        ThingTypeFieldTemplate field = new ThingTypeFieldTemplate();
        field.setName(udfName);
        field.setDescription(udfDescription);
        field.setUnit(udfUnit);
        field.setSymbol(udfSymbol);
        field.setType(udfType);
        field.setTypeParent(udfTypeParent);
        field.setTimeSeries(udfTimeSeries);
        field.setThingTypeTemplate(thingTypeTemplate);
        field.setDefaultValue(defaultValue);
        return ThingTypeFieldTemplateService.getInstance().insert(field);
    }

    /**
     * Insert a new Udf Field
     * @param udfName udf Name
     * @param udfDescription udf Description
     * @param udfUnit udf Unit
     * @param udfSymbol udf Symbol
     * @param udfType udf Type
     * @param udfTypeParent udf Type Parent
     * @param udfTimeSeries udf TimeSeries
     * @param thingTypeTemplate thing Type Template
     */
    public ThingTypeFieldTemplate insertUdfField(String udfName, String udfDescription, String udfUnit, String udfSymbol, DataType udfType,
                                                        String udfTypeParent, boolean udfTimeSeries, ThingTypeTemplate thingTypeTemplate, String defaultValue){
        ThingTypeFieldTemplate field = new ThingTypeFieldTemplate();
        field.setName(udfName);
        field.setDescription(udfDescription);
        field.setUnit(udfUnit);
        field.setSymbol(udfSymbol);
        field.setType(udfType);
        field.setTypeParent(udfTypeParent);
        field.setTimeSeries(udfTimeSeries);
        field.setThingTypeTemplate(thingTypeTemplate);
        field.setDefaultValue(defaultValue);
        return insert(field);
    }


    public Long getThingTypeFieldTemplateId(String name){
        ThingTypeFieldTemplate thingTypeFieldTemplate = getThingTypeFieldTemplateDAO().selectBy("name", name);
        return thingTypeFieldTemplate.getId();
    }
}

