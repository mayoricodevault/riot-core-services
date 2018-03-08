package com.tierconnect.riot.appcore.entities;

/**
 * Created by julio.rocha on 26-05-17.
 */
public class UserRoleResource {
    private Long roleId;
    private String roleName;
    private Long groupTypeCeiling;
    private Long resourceId;
    private String resourceName;
    private String resourceFqName;
    private Integer resourceType;
    private Long resourceTypeId;
    private String acceptedAttributes;
    private String permissions;

    public UserRoleResource(Long roleId, String roleName, Long groupTypeCeiling,
                            Long resourceId, String resourceName, String resourceFqName,
                            Integer resourceType, Long resourceTypeId, String acceptedAttributes, String permissions) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.groupTypeCeiling = groupTypeCeiling;
        this.resourceId = resourceId;
        this.resourceName = resourceName;
        this.resourceFqName = resourceFqName;
        this.resourceType = resourceType;
        this.resourceTypeId = resourceTypeId;
        this.acceptedAttributes = acceptedAttributes;
        this.permissions = permissions;
    }

    public Long getRoleId() {
        return roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public Long getGroupTypeCeiling() {
        return groupTypeCeiling;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getResourceFqName() {
        return resourceFqName;
    }

    public Integer getResourceType() {
        return resourceType;
    }

    public Long getResourceTypeId() {
        return resourceTypeId;
    }

    public String getAcceptedAttributes() {
        return acceptedAttributes;
    }

    public String getPermissions() {
        return permissions;
    }
}
