package com.tierconnect.riot.appcore.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.Predicate;
import com.tierconnect.riot.appcore.dao.UserFieldDAO;
import com.tierconnect.riot.appcore.entities.Field;
import com.tierconnect.riot.appcore.entities.GroupField;
import com.tierconnect.riot.appcore.entities.QUserField;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.entities.UserField;
import com.tierconnect.riot.sdk.dao.NotFoundException;
import com.tierconnect.riot.sdk.dao.Pagination;

/**
 * 
 * @author garivera
 *
 */
public class UserFieldService extends UserFieldServiceBase {

	private static final QUserField qUserField = QUserField.userField;


    public UserField selectByUserAndUserField(User user, Field field) {
        Predicate predicate = qUserField.user.id.eq(user.getId()).and(qUserField.field.id.eq(field.getId()));
        return getUserFieldDAO().selectBy(predicate);
    }

    public static List<UserField> listByUser(User user){
    	return getUserFieldDAO().getQuery().where(qUserField.user.eq(user)).list(qUserField);
    }

	public void unset(User user, Field field) {
		if (user == null) {
			throw new NotFoundException("Group not found");
		}
		if (field == null) {
			throw new NotFoundException("Field not found");
		}
		UserField userField = getUserFieldDAO()
				.getQuery()
				.where(qUserField.user.eq(user).and(
						qUserField.field.eq(field)))
				.uniqueResult(qUserField);
		if (userField != null) {
			delete(userField);
		}
	}

	public UserField set(User user, Field field, String value) {
		if (user == null) {
			throw new NotFoundException("Group not found");
		}
		if (field == null) {
			throw new NotFoundException("Field not found");
		}
		UserField userField = getUserFieldDAO()
				.getQuery()
				.where(qUserField.user.eq(user).and(
						qUserField.field.eq(field)))
				.uniqueResult(qUserField);
		if (userField == null) {
			userField = new UserField();
			userField.setField(field);
			userField.setUser(user);
			getUserFieldDAO().insert(userField);
		}
		userField.setValue(value);
		getUserFieldDAO().update(userField);
		return userField;
	}

	public static List<UserField> listFieldsPaginatedByUser(Pagination pagination, User user) {
		return getUserFieldDAO().selectAll(qUserField.user.eq(user), pagination);
	}

	public static Map<String, Object> listInheritedFieldsByUser(User user, boolean map) {
		Map<String, Object> result = new HashMap<>();
		Map<Long, GroupField> groupFields = GroupFieldService.getInstance().listInheritedFieldsByGroup(user.getActiveGroup());
		for (Map.Entry<Long, GroupField> gfe : groupFields.entrySet()) {
            GroupField gf = gfe.getValue();
            if (map) {
                result.put(gf.getField().getName(), gf.publicMap(true));
            } else {
                result.put(gf.getField().getName(), gf);
            }
		}
		List<UserField> userFields = listByUser(user);
		for (UserField uf : userFields) {
            if (map) {
                result.put(uf.getField().getName(), uf.publicMap(true));
            } else {
                result.put(uf.getField().getName(), uf);
            }
		}
		return result;
	}

	public void deleteByUser(User user) {
		UserFieldDAO userFieldDAO = getUserFieldDAO();

		BooleanBuilder be = new BooleanBuilder();
		be = be.and(QUserField.userField.user.id.eq(user.getId()));

		List<UserField> userFields = userFieldDAO.selectAll(be, null, null);
		for (UserField userField : userFields) {
			userFieldDAO.delete(userField);
		}

	}
}
