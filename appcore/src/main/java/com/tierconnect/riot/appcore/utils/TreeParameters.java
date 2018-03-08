package com.tierconnect.riot.appcore.utils;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;

import java.util.*;

/**
 * Created by agutierrez on 11/27/2014.
 */
public class TreeParameters<T> {

    private List<Map<String, Object>> list = new LinkedList<>();

    private Map<Long, Map<String, Object>> objectCache = new HashMap<>();

    private Map<Long, Set<Long>> childrenMapCache = new HashMap<>();

    private Map<String, Boolean> permissionCache = new HashMap<>();

    private Group visibilityGroup;

    private String only;

    private String extra;

    private String order;

    private T topObject;

    private boolean upVisibility;

    private boolean downVisibility;

    public Group getVisibilityGroup() {
        return visibilityGroup;
    }

    public void setVisibilityGroup(Group visibilityGroup) {
        this.visibilityGroup = visibilityGroup;
    }

    public Map<Long, Map<String, Object>> getObjectCache() {
        return objectCache;
    }

    public void setObjectCache(Map<Long, Map<String, Object>> objectCache) {
        this.objectCache = objectCache;
    }

    public Map<Long, Set<Long>> getChildrenMapCache() {
        return childrenMapCache;
    }

    public void setChildrenMapCache(Map<Long, Set<Long>> childrenMapCache) {
        this.childrenMapCache = childrenMapCache;
    }

    public Map<String, Boolean> getPermissionCache() {
        return permissionCache;
    }

    public void setPermissionCache(Map<String, Boolean> permissionCache) {
        this.permissionCache = permissionCache;
    }

    public List<Map<String, Object>> getList() {
        return list;
    }

    public void setList(List<Map<String, Object>> list) {
        this.list = list;
    }

    public String getOnly() {
        return only;
    }

    public void setOnly(String only) {
        this.only = only;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public T getTopObject() {
        return topObject;
    }

    public void setTopObject(T topObject) {
        this.topObject = topObject;
    }

    public boolean isUpVisibility() {
        return upVisibility;
    }

    public void setUpVisibility(boolean upVisibility) {
        this.upVisibility = upVisibility;
    }

    public boolean isDownVisibility() {
        return downVisibility;
    }

    public void setDownVisibility(boolean downVisibility) {
        this.downVisibility = downVisibility;
    }
}
