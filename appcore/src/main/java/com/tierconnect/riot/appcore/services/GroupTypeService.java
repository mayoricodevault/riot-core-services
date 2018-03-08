package com.tierconnect.riot.appcore.services;

import java.util.ArrayList;
import java.util.List;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.Predicate;
import com.tierconnect.riot.appcore.dao.GroupTypeDAO;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.entities.QGroupType;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;

public class GroupTypeService extends GroupTypeServiceBase {

	static Logger logger = Logger.getLogger(GroupTypeService.class);
	static private GroupTypeDAO _groupTypeDAO;
	static private QGroupType qGroupType = QGroupType.groupType;
	
	public GroupType getRootGroupType()
	{
		return getInstance().get( 1L );
	}
	
	public GroupType getTenantGroupType()
	{
		return getInstance().get( 2L );
	}


	private static final String BROKER_CLIENT_HELPER = "com.tierconnect.riot.iot.services.BrokerClientHelper";

    @Override
	public GroupType insert(GroupType groupType) {
		if (groupType == null) {
			throw new UserException("GroupType is Empty");
		}
		if (groupType.getName() == null || "".equals(groupType.getName())) {
			throw new UserException("Name is a required parameter.");			
		}
		if (groupType.getParent() == null) {
			if (getGroupTypeDAO().selectAllBy(qGroupType.parent.isNull()).size() > 0) {
				throw new UserException("Only one Root Type is allowed.");
			}
        } else if (groupType.getParent().getParent() == null) {
            GroupType groupTypeRoot = getGroupTypeDAO().selectAllBy(qGroupType.parent.isNull()).get(0);
            if (getGroupTypeDAO().selectAllBy(qGroupType.parent.eq(groupTypeRoot)).size() > 0) {
                throw new UserException("Only one Tenant Type is allowed.");
            }
        }
        validateUniqueName(groupType, true);
        Long id = getGroupTypeDAO().insert(groupType);
        groupType.setId(id);
        return groupType;
    }

    @Override
	public GroupType update(GroupType groupType) {
		if (groupType == null) {
			throw new UserException("GroupType is Empty");
		}
		if (groupType.getName() == null || "".equals(groupType.getName())) {
			throw new UserException("Name is a required parameter.");
		}
		if (groupType.getParent() != null && groupType.getParent().getId().equals(groupType.getId())) {
			throw new UserException("Incorrect Parent Group, cannot refer to himself as parent.");
		}
		if (groupType.getParent() == null) {
			List<GroupType> oldGroupTypes = getGroupTypeDAO().selectAllBy(QGroupType.groupType.parent.isNull());
			if (oldGroupTypes.size() > 0 && ! groupType.getId().equals(oldGroupTypes.get(0).getId())) {
				throw new UserException("Only one Root Type is allowed.");			
			}
		}
		validateCiclic(groupType, groupType.getParent());
		validateUniqueName(groupType, false);
		getGroupTypeDAO().update(groupType);
		updateFavorite(groupType);
        return groupType;
	}
	
	public static BooleanBuilder getSiblingsExcludingPredicateInSameCompany(GroupType groupType) {
		GroupType parent = groupType.getParent();
		BooleanBuilder b = new BooleanBuilder();
		b = b.and(parent == null ? qGroupType.parent.isNull() : qGroupType.parent.eq(parent));
		if (groupType.getId() != null) {
			b = b.or(qGroupType.ne(groupType));
		}else{
			b = b.or(qGroupType.group.eq(groupType.getGroup()));
		}
		return b;
	}
	
	public static Predicate getAncendantsExcludingPredicate(GroupType groupType) {
		BooleanBuilder b = new BooleanBuilder();
		GroupType parent = groupType.getParent();
		while (parent != null) {
			b=b.and(qGroupType.eq(parent));
			parent = parent.getParent();
		} 
		return b.getValue();
	}

    public static Predicate getDescendantsIncludingPredicate(GroupType groupType) {
		BooleanBuilder b = new BooleanBuilder();
		if (groupType.getId() != null) {
			List<GroupType> list = new ArrayList<>();
			list.add(groupType);
			int i = 1;
			while (!list.isEmpty() && i < Group.MAX_LEVEL) {
				b = b.and(qGroupType.parent.in(list));
				list = getGroupTypeDAO().selectAllBy(qGroupType.parent.in(list));
				i++;
			}
		}
		return b.getValue();
	}
	
	private static void validateUniqueName(GroupType groupType, boolean insert) {
		BooleanBuilder b = new BooleanBuilder();
		b=b.or(getSiblingsExcludingPredicateInSameCompany(groupType));
		//b=b.or(getAncendantsExcludingPredicate(groupType));
		//b=b.or(getDescendantsIncludingPredicate(groupType));
		Predicate p = qGroupType.name.eq(groupType.getName()).and(b.getValue());
		List<GroupType> otherGroups = getGroupTypeDAO().selectAllBy(p);
		validateUniqueName(groupType, insert, otherGroups);
	}

	private static void validateUniqueName(GroupType groupType, boolean insert,	List<GroupType> otherGroupTypes) {
		// commented by terry
		if (insert && otherGroupTypes.size() > 0) {
			throw new UserException("Duplicated group type name.");
		} else
		if (!insert && otherGroupTypes.size() > 0) {
			for (GroupType otherGroup:otherGroupTypes) {
				if (!groupType.getId().equals(otherGroup.getId())){
					throw new UserException("Duplicated group type name.");
				}
			}
		}
	}
	
	private static void validateCiclic(GroupType group, GroupType newParent) {
		while (newParent != null) {
			if (newParent.getId().equals(group.getId())) {
				throw new UserException(
						"Ciclic relation ship found for groups.");
			}
			newParent = newParent.getParent();
		}
	}

	public static List<GroupType> getChildren(GroupType group) {
		return getGroupTypeDAO().selectAllBy(QGroupType.groupType.parent.eq(group));
	}

    public List<GroupType> getDescendants(GroupType groupType) {
        List<GroupType> result = new ArrayList<>();
        List<GroupType> children = getGroupTypeDAO().selectAllBy(QGroupType.groupType.parent.eq(groupType));
        result.addAll(children);
        while (!children.isEmpty()) {
            children = getGroupTypeDAO().selectAllBy(QGroupType.groupType.parent.in(children));
            result.addAll(children);
        }
        return result;
    }

    public List<Long> getDescendantIds(GroupType groupType) {
        List<Long> result = new ArrayList<>();
        for (GroupType group: getDescendants(groupType)) {
            result.add(group.getId());
        }
        return result;
    }

    /**
     * @param groupType
     * @param treeRoot
     * @return true if groupType is a child of or is treeRoot
     */
    public boolean isGroupTypeInsideTree(GroupType groupType, GroupType treeRoot) {
        if (groupType.getId().equals(treeRoot.getId())) {
            return true;
        }
        GroupType parent = groupType.getParent();
        while (parent != null) {
            if (parent.getId().equals(treeRoot.getId())) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

	/*
	* This method gets a group type based on the name of the group
	* */
	public GroupType getByName(String name) throws NonUniqueResultException {
		try {
			return getGroupTypeDAO().selectBy("name", name);
		}
		catch (org.hibernate.NonUniqueResultException e) {
			throw new NonUniqueResultException(e);
		}
	}


	/**
	 * it updates a message of kafka cache topic ___v1___cache___grouptype.
	 *
	 * @param groupType
	 * @param delete
     */
	public void refreshCache(GroupType groupType, boolean delete){
		try
		{
			Class clazz = Class.forName(BROKER_CLIENT_HELPER);
			clazz.getMethod("refreshGroupTypeCache", GroupType.class,boolean.class).invoke(null, groupType, delete);
		}
		catch (Exception e) {
			logger.error("Could not call MQTT sendRefreshGroupTypesMessage method",e);
		}
	}

    public GroupType getByNameAndGroup(String name, Group group) {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QGroupType.groupType.name.eq(name));
        if (group != null) {
            be = be.and(QGroupType.groupType.group.eq(group));
        }
        return getGroupTypeDAO().selectBy(be);
    }
}
