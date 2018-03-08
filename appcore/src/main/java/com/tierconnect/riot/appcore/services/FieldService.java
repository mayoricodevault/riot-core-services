package com.tierconnect.riot.appcore.services;

import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.mysema.query.types.expr.BooleanExpression;
import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QField;

import java.util.List;

public class FieldService extends FieldServiceBase 
{  
    //TODO refactor this method should throw exception when it cannot insert by duplicate name. duplication by name should be in tree structure
    public Field insert(Field field) {
        if (existsUserFieldByName(field.getName())) {
            return selectByName(field.getName());
        }
        Long id = getFieldDAO().insert(field);
        field.setId(id);
        return field;
    }

    //TODO refactor this method should throw exception when it cannot insert by duplicate name. duplication by name should be in tree structure
    public Field update(Field field) {
        if (existsUserFieldByName(field.getName(), field.getId())) {
            return selectByName(field.getName());
        }
        getFieldDAO().update(field);
        return field;
    }

    public boolean existsUserFieldByName(String name) {
        return existsUserFieldByName(name, null);
    }

    public boolean existsUserFieldByName(String name, Long excludeId) {
        BooleanExpression predicate = QField.field.name.eq(name);

        if (excludeId != null) {
            predicate = predicate.and(QField.field.id.ne(excludeId));
        }

        return getFieldDAO().getQuery().where(predicate).exists();
    }

	public Field selectByName(String UserFieldname) {
        return getFieldDAO().getQuery().where(QField.field.name.eq(UserFieldname)).uniqueResult(QField.field);
	}

    public static List<Field> getFieldsByParentField(Field field) {
        HibernateQuery query = FieldService.getFieldDAO().getQuery();
        return query.where(QField.field.parentField.eq(field)).list(QField.field);
    }

    /**
     * Get a specific field based on group and field name
     * @param group
     * @param UserFieldname
     * @return
     */
    public Field selectByGroupAndName(Group group , String UserFieldname) {
        return getFieldDAO().getQuery().where(
                QField.field.name.eq(UserFieldname).and(QField.field.group.eq(group))).uniqueResult(QField.field);
    }

}
