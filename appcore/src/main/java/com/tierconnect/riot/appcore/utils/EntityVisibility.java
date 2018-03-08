package com.tierconnect.riot.appcore.utils;

import com.mysema.query.types.path.EntityPathBase;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.sdk.utils.UpVisibility;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by agutierrez on 6/15/15.
 */
public class EntityVisibility<T> {
    private String groupProperty = "group";
    private String createdByUserProperty = "createdByUser";
    private String groupTypeFloorProperty = "groupTypeFloor";
    private String roleShareProperty = "roleShare";
    private String groupShareProperty = "groupShare";

    private UpVisibility upVisibility = UpVisibility.FALSE;
    private boolean downVisibility = true;
    private boolean sharedToSelf = false;
    private boolean sharedByRole = false;
    private boolean sharedByGroup = false;
    private boolean sharedByGroupType = false;
    private EntityPathBase<T> entityPathBase = null;

    public EntityVisibility() {
    }

    public Group getGroup(T object) {
        if (object instanceof  Group) {
            return (Group) object;
        }
        return (Group) get(object, getGroupProperty());
    }

    public QGroup getQGroup(EntityPathBase<T> base) {
        //Path<Group> aux = Expressions.path(Group.class, base, getGroupProperty());
        //return (QGroup) aux;
        return (QGroup) get(base, getGroupProperty());
    }

    public GroupType getGroupTypeFloor(T object) {
        return (GroupType) get(object, getGroupTypeFloorProperty());
    }

    public QGroupType getQGroupTypeFloor(EntityPathBase<T> base) {
        return (QGroupType) get(base, getGroupTypeFloorProperty());
        //Path<GroupType> aux = Expressions.path(GroupType.class, base, getGroupTypeFloorProperty());
        //return (QGroupType) aux;
    }

    public User getCreatedByUser(T object) {
        return (User) get(object, getCreatedByUserProperty());
    }

    public QUser getQCreatedByUser(EntityPathBase<T> base) {
        return (QUser) get(base, getCreatedByUserProperty());
//        Path<User> aux = Expressions.path(User.class, base, getCreatedByUserProperty());
//        return (QUser) aux;
    }

    public Set<Long> getAllowedRoles(Object object) {
        Role role = (Role) get(object, getRoleShareProperty());
        if (role != null) {
            Set<Long> set = new HashSet<>();
            set.add(role.getId());
            return set;
        } else {
            return null;
        }
    }

    public QRole getQRoleShare(EntityPathBase<T> base) {
        return (QRole) get(base, getRoleShareProperty());
        //Path<Role> aux = Expressions.path(Role.class, base, getRoleShareProperty());
        //return (QRole) aux;
    }


    public Set<Long> getAllowedGroups(Object object) {
        Group group = (Group) get(object, getGroupShareProperty());
        if (group != null) {
            Set<Long> set = new HashSet<>();
            set.add(group.getId());
            return set;
        } else {
            return null;
        }
    }

    public QGroup getQGroupShare(EntityPathBase<T> base) {
        //Path<Group> aux = Expressions.path(Group.class, base, getGroupShareProperty());
        //return (QGroup) aux;
        return (QGroup) get(base, getGroupShareProperty());

    }

    private Object get(Object object, String property) {
        try {
            if (object instanceof  EntityPathBase) {
                return object.getClass().getDeclaredField(property).get(object);
            } else {
                return object.getClass().getMethod("get" + property.substring(0, 1).toUpperCase() + property.substring(1)).invoke(object);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getEditOwnPermission() {
        String simpleName = entityPathBase.getType().getSimpleName();
        return simpleName.substring(0,1).toLowerCase()+simpleName.substring(1)+"_editOwn:u";
    }

    public String getReadOneObjectPermission(Long id) {
        String simpleName = entityPathBase.getType().getSimpleName();
        return simpleName.substring(0,1).toLowerCase()+simpleName.substring(1)+":r:"+id+"";
    }

    public String getUpdateOneObjectPermission(Long id) {
        String simpleName = entityPathBase.getType().getSimpleName();
        return simpleName.substring(0,1).toLowerCase()+simpleName.substring(1)+":u:"+id+"";
    }

    private String getGroupProperty() {
        return groupProperty;
    }

    private String getCreatedByUserProperty() {
        return createdByUserProperty;
    }

    private String getGroupTypeFloorProperty() {
        return groupTypeFloorProperty;
    }

    private String getRoleShareProperty() {
        return roleShareProperty;
    }

    private String getGroupShareProperty() {
        return groupShareProperty;
    }

    public void setGroupProperty(String groupProperty) {
        this.groupProperty = groupProperty;
    }

    public void setCreatedByUserProperty(String createdByUserProperty) {
        this.createdByUserProperty = createdByUserProperty;
    }

    public void setGroupTypeFloorProperty(String groupTypeFloorProperty) {
        this.groupTypeFloorProperty = groupTypeFloorProperty;
    }

    public void setRoleShareProperty(String roleShareProperty) {
        this.roleShareProperty = roleShareProperty;
    }

    public void setGroupShareProperty(String groupShareProperty) {
        this.groupShareProperty = groupShareProperty;
    }

    public UpVisibility getUpVisibility() {
        return upVisibility;
    }

    public void setUpVisibility(UpVisibility upVisibility) {
        this.upVisibility = upVisibility;
    }

    public boolean isDownVisibility() {
        return downVisibility;
    }

    public void setDownVisibility(boolean downVisibility) {
        this.downVisibility = downVisibility;
    }

    public boolean isSharedToSelf() {
        return sharedToSelf;
    }

    public void setSharedToSelf(boolean sharedToSelf) {
        this.sharedToSelf = sharedToSelf;
    }

    public boolean isSharedByRole() {
        return sharedByRole;
    }

    public void setSharedByRole(boolean sharedByRole) {
        this.sharedByRole = sharedByRole;
    }

    public boolean isSharedByGroup() {
        return sharedByGroup;
    }

    public void setSharedByGroup(boolean sharedByGroup) {
        this.sharedByGroup = sharedByGroup;
    }

    public boolean isSharedByGroupType() {
        return sharedByGroupType;
    }

    public void setSharedByGroupType(boolean sharedByGroupType) {
        this.sharedByGroupType = sharedByGroupType;
    }

    public EntityPathBase<T> getEntityPathBase() {
        return entityPathBase;
    }

    public void setEntityPathBase(EntityPathBase<T> entityPathBase) {
        this.entityPathBase = entityPathBase;
    }

    public Long getId(T object) {
        return (Long) get(object, "id");
    }

    public boolean isNotFalseUpVisibility() {
        return  upVisibility != null && !UpVisibility.FALSE.equals(upVisibility);
    }
}
