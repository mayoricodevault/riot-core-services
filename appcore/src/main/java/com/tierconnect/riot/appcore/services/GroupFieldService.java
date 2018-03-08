package com.tierconnect.riot.appcore.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mysema.query.Tuple;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.NotFoundException;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;


public class GroupFieldService extends GroupFieldServiceBase
{
	private final static QGroupField qGroupField = QGroupField.groupField;

    public GroupField selectByGroupField(Group group, Field field) {
        GroupField groupField = GroupFieldService.getInstance().getGroupFieldDAO().getQuery().where(qGroupField.group.eq(group).and(qGroupField.field.eq(field))).uniqueResult(qGroupField);
        return  groupField;
    }

	public GroupField set(Group group, Field field, String value) {
		if (group == null) {
			throw new NotFoundException("Group not found");
		}
		if (field == null) {
			throw new NotFoundException("Field not found");
		}
		validateFieldValue(field, value);
		GroupField groupField = selectByGroupField(group, field);
		if (groupField == null) {
			groupField = new GroupField();
			groupField.setField(field);
			groupField.setGroup(group);
			getGroupFieldDAO().insert(groupField);
		}
		groupField.setValue(value);
		getGroupFieldDAO().update(groupField);
		return groupField;
	}

	private void validateFieldValue(Field field, String value) {
		if (field.getName().equals("passwordMinLength")
			&& Integer.parseInt(value) < Constants.PASSWORD_MIN_LENGTH) {
			throw new UserException("Password Min Length cannot be less than " + Constants.PASSWORD_MIN_LENGTH);
		}
		if (field.getName().equals("passwordMaxLength")
			&& Integer.parseInt(value) > Constants.PASSWORD_MAX_LENGTH) {
			throw new UserException("Password Max Length cannot be greater than " + Constants.PASSWORD_MAX_LENGTH);
		}
		if (field.getName().equals("passwordStrength")
			&& StringUtils.isBlank(value)) {
			throw new UserException("Password Strength cannot be empty");
		}
	}

	public void unset(Group group, Field field) {
		if (group == null) {
			throw new NotFoundException("Group not found");
		}
		if (field == null) {
			throw new NotFoundException("Field not found");
		}
		GroupField groupField = getGroupFieldDAO()
				.getQuery()
				.where(qGroupField.group.eq(group).and(
						qGroupField.field.eq(field)))
				.uniqueResult(qGroupField);
		if (groupField != null) {
			delete(groupField);
		}
	}

	public Long countFieldsByGroup(Group group) {
		return getGroupFieldDAO().countAll(qGroupField.group.eq(group));
	}

	public List<GroupField> listFieldsPaginatedByGroup(
			Pagination pagination, Group group) {
		return getGroupFieldDAO().selectAll(qGroupField.group.eq(group),
				pagination);
	}

	public Map<Long, GroupField> listInheritedFieldsByGroup(Group group) {
		Map<Long, Group> groups = getParentGroupMap(group);
		Map<Long, GroupField> groupFields = new HashMap<>();
		List<GroupField> groupFieldsLevel = getGroupFieldDAO().getQuery()
				.where(qGroupField.group.in(groups.values())).list(qGroupField);
		for (GroupField groupField : groupFieldsLevel) {
			GroupField groupFieldLevelOld = groupFields.get(groupField.getField().getId());
			if (groupFieldLevelOld == null
					|| groups.get(groupFieldLevelOld.getGroup().getId()).getTreeLevel() < groups.get(groupField.getGroup().getId()).getTreeLevel()
			) {
				groupFields.put(groupField.getField().getId(), groupField);
			}
		}
		return groupFields;
	}

	public Map<String, String> listInheritedFieldsByGroupNative(Group group){
		Map<String, String> result = new ConcurrentHashMap<>();
		Map<String, Integer> nameTreeLevel = new ConcurrentHashMap<>();
		Map<Long, Group> groups = getParentGroupMap(group);
		HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
		List<Tuple> list = query.from(qGroupField)
				.innerJoin(qGroupField.field, QField.field)
				.innerJoin(qGroupField.field.group, QGroup.group)
				.where(qGroupField.group.in(groups.values()))
				//.orderBy(QGroup.group.treeLevel.desc())
				.setCacheable(true)
				.list(QField.field.name, qGroupField.value, qGroupField.group.treeLevel);
		list.stream().forEach(t ->{
			String k = t.get(0, String.class);
			Integer treeLevel = t.get(2, Integer.class);
			if (nameTreeLevel.get(k) == null || treeLevel > nameTreeLevel.get(k)) {
                nameTreeLevel.put(k, treeLevel);
                result.put(k, t.get(1, String.class));
            }
		});
		return result;
	}

	private Map<Long, Group> getParentGroupMap(Group group) {
		Map<Long, Group> groups = new HashMap<>();
		for (int i = 1; i <= group.getTreeLevel(); i++) {
			Group aux = group.getParentLevel(i);
			groups.put(aux.getId(), aux);
		}
		return groups;
	}

	public String getOwnershipValue(Group group, String name){
		Map<Long, Group> groups = getParentGroupMap(group);

		String result = null;

		List<GroupField> groupFieldsLevel =  getGroupFieldDAO().getQuery().where(qGroupField.group.in(groups.values()).and(qGroupField.field.name.eq(name))).list(qGroupField);
		for (GroupField groupField : groupFieldsLevel) {
			if (groupField.getGroup().getId().equals(group.getId())){
				result = groupField.getValue();
			}
		}

		if (result == null && groupFieldsLevel.size() > 1){
			for (GroupField groupField : groupFieldsLevel) {
				for (int i=group.getTreeLevel();i>=2;i--) {
					if (groupField.getGroup().getId().equals(group.getParentLevel(i).getId())) {
						result = groupField.getValue();
						break;
					}
				}
			}
		}

		if (result == null && !groupFieldsLevel.isEmpty()) {
			GroupField groupField = groupFieldsLevel.get(0);
			result = groupField.getValue();
		}
		return result;
	}

	/**
	 * Get Group Field based on groupId and field Param
	 * @param group
	 * @param fieldParam
     * @return
     */
	public String getGroupField(Group group, String fieldParam){
		if (group == null) {
			throw new NotFoundException(String.format("Groupis null, please send the correct value "));
		}
		Field field;
		try {
			field = FieldService.getInstance().get(Long.valueOf(fieldParam));
		} catch (Exception ex) {
			field = FieldService.getInstance().selectByName(fieldParam);
		}
		if (field == null) {
			throw new NotFoundException(String.format("Field[%s] not found", fieldParam));
		}
		return ConfigurationService.getAsString(group, field.getName());
	}

	/**
	 *
	 * @param field
	 * @return GroupField
     */
	public GroupField getByField(Field field)
	{
		return getGroupFieldDAO().getQuery().where(qGroupField.field.eq(field)).uniqueResult(qGroupField);
	}
}
