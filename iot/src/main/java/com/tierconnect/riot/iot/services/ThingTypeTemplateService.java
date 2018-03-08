package com.tierconnect.riot.iot.services;

import com.mysema.query.types.expr.BooleanExpression;
import com.tierconnect.riot.iot.entities.QThingTypeTemplate;
import com.tierconnect.riot.iot.entities.ThingTypeTemplate;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;

public class ThingTypeTemplateService extends ThingTypeTemplateServiceBase {

    @Deprecated
    public ThingTypeTemplate getByName(String name) throws NonUniqueResultException {
        try {
            return getThingTypeTemplateDAO().selectBy("name", name);
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }

    /**
     * Verify exist by name in {@link ThingTypeTemplate}
     *
     * @param nameTemplate
     * @return
     */
    @Deprecated
    public boolean existByName(String nameTemplate){
        BooleanExpression predicate = QThingTypeTemplate.thingTypeTemplate.name.eq(nameTemplate);
        return getThingTypeTemplateDAO().getQuery().where(predicate).exists();
    }

    /**
     *
     * @param name name thing type template
     * @return a thing type template searched by name
     */
    @Deprecated
    public ThingTypeTemplate getByNameUnique(String name) {
        return getThingTypeTemplateDAO().getQuery().where(QThingTypeTemplate.thingTypeTemplate.name
                .eq(name)).uniqueResult(QThingTypeTemplate.thingTypeTemplate);
    }

    public ThingTypeTemplate getByCode(String code) throws NonUniqueResultException {
        try {
            return getThingTypeTemplateDAO().selectBy("code", code);
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }
}

