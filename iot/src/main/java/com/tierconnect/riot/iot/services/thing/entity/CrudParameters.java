package com.tierconnect.riot.iot.services.thing.entity;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.utils.Cache;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.subject.Subject;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by julio.rocha on 02-08-17.
 */
public class CrudParameters {
    private String thingTypeCode;
    private String groupHierarchyCode;
    private String name;
    private String serialNumber;
    private Object parentMapOrObject;
    private Map<String, Object> parent;
    private Map<String, Object> udfs;
    private List<Map<String, Object>> children;
    private List<Map<String, Object>> childrenUdf;
    private boolean executeTickle;
    private boolean validateVisibility;
    private Date transactionDate;
    private boolean disableFMCLogic;
    private boolean createUpdateAndFlush;
    private Boolean useDefaultValues;
    private Map<String, Boolean> validations;
    private String facilityCode;
    private boolean fillSource;
    private Subject subject;
    private User currentUser;
    private Group activeGroup;
    private Cache cache;

    //computed parameters
    private ThingType thingType;
    private Group group;
    private Map<String, Object> parentObj;
    private List<Map<String, Object>> childrenObj;
    private List<Map<String, Object>> childrenUDFObj;
    private Group groupFacilityMap;
    private Date modifiedTime;
    private User userActionExecutor;
    private boolean deleteMongoFlag;
    private boolean secureDelete;
    private Thing thingToProcess;
    private Thing thingParentToProcess;
    private Map<String, Object> thingParentMapToProcess;
    private boolean isOlderTimestamp = false;
    private boolean isCreation = false;

    private CrudParameters(String thingTypeCode, String groupHierarchyCode, String name, String serialNumber,
                           Object parent, Map<String, Object> udfs, List children, List childrenUdf,
                           boolean executeTickle, boolean validateVisibility, Date transactionDate, boolean disableFMCLogic,
                           boolean createUpdateAndFlush, Boolean useDefaultValues, Map<String, Boolean> validations,
                           String facilityCode, boolean fillSource, Subject subject, User currentUser, Cache cache,
                           boolean isCreation) {
        this.thingTypeCode = thingTypeCode;
        this.groupHierarchyCode = groupHierarchyCode;
        this.name = name;
        this.serialNumber = formatSerialNumber(serialNumber);
        this.parentMapOrObject = parent;
        this.udfs = udfs;
        this.children = children;
        this.childrenUdf = childrenUdf;
        this.executeTickle = executeTickle;
        this.validateVisibility = validateVisibility;
        this.transactionDate = transactionDate;
        this.disableFMCLogic = disableFMCLogic;
        this.createUpdateAndFlush = createUpdateAndFlush;
        this.useDefaultValues = useDefaultValues;
        this.validations = validations;
        this.facilityCode = facilityCode;
        this.fillSource = fillSource;
        this.subject = subject;
        this.currentUser = currentUser;
        this.activeGroup = currentUser.getActiveGroup();
        this.cache = cache;
        this.isCreation = isCreation;
        initializeUDFIfNull();
        prepareParent();
    }

    @SuppressWarnings("unchecked")
    private void prepareParent() {
        if (parentMapOrObject instanceof Map) {
            this.parent = (Map<String, Object>) parentMapOrObject;
        } else if (parentMapOrObject instanceof Thing) {
            Thing parent = (Thing) parentMapOrObject;
            Map<String, Object> map = new HashMap<>();
            map.put("serialNumber", parent.getSerial());
            map.put("thingTypeCode", parent.getThingType().getThingTypeCode());
            this.parent = map;
        }
    }

    public Object getParentMapOrObject() {
        return parentMapOrObject;
    }

    public String getThingTypeCode() {
        return thingTypeCode;
    }

    public String getGroupHierarchyCode() {
        return groupHierarchyCode;
    }

    public String getName() {
        return name;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public Map<String, Object> getParent() {
        return parent;
    }

    public Map<String, Object> getUdfs() {
        return udfs;
    }

    public void setUdfs(Map<String, Object> udfs) {
        this.udfs = udfs;
    }

    public void initializeUDFIfNull() {
        if (udfs == null) {
            udfs = new HashMap<>();
        }
    }

    public List<Map<String, Object>> getChildren() {
        return children;
    }

    public List<Map<String, Object>> getChildrenUdf() {
        return childrenUdf;
    }

    public boolean getExecuteTickle() {
        return executeTickle;
    }

    public boolean getValidateVisibility() {
        return validateVisibility;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public boolean getDisableFMCLogic() {
        return disableFMCLogic;
    }

    public boolean getCreateUpdateAndFlush() {
        return createUpdateAndFlush;
    }

    public Boolean getUseDefaultValues() {
        return useDefaultValues;
    }

    public Map<String, Boolean> getValidations() {
        return validations;
    }

    public String getFacilityCode() {
        return facilityCode;
    }

    public boolean getFillSource() {
        return fillSource;
    }

    public Subject getSubject() {
        return subject;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public Group getActiveGroup() {
        return activeGroup;
    }

    public ThingType getThingType() {
        return thingType;
    }

    public Group getGroup() {
        return group;
    }

    public Map<String, Object> getParentObj() {
        return parentObj;
    }

    public List<Map<String, Object>> getChildrenObj() {
        return childrenObj;
    }

    public List<Map<String, Object>> getChildrenUDFObj() {
        return childrenUDFObj;
    }

    public Group getGroupFacilityMap() {
        return groupFacilityMap;
    }

    public Date getModifiedTime() {
        return modifiedTime;
    }

    public User getUserActionExecutor() {
        return userActionExecutor;
    }

    public boolean getDeleteMongoFlag() {
        return deleteMongoFlag;
    }

    public boolean getSecureDelete() {
        return secureDelete;
    }

    public Thing getThingToProcess() {
        return thingToProcess;
    }

    public Thing getThingParentToProcess() {
        return thingParentToProcess;
    }

    public Map<String, Object> getThingParentMapToProcess() {
        return thingParentMapToProcess;
    }

    public void setThingType(ThingType thingType) {
        this.thingType = thingType;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public void setParentObj(Map<String, Object> parentObj) {
        this.parentObj = parentObj;
    }

    public void setChildrenObj(List<Map<String, Object>> childrenObj) {
        this.childrenObj = childrenObj;
    }

    public void setChildrenUDFObj(List<Map<String, Object>> childrenUDFObj) {
        this.childrenUDFObj = childrenUDFObj;
    }

    public void setGroupFacilityMap(Group groupFacilityMap) {
        this.groupFacilityMap = groupFacilityMap;
    }

    public void setModifiedTime(Date modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public void setUserActionExecutor(User userActionExecutor) {
        this.userActionExecutor = userActionExecutor;
    }

    public void setDeleteMongoFlag(boolean deleteMongoFlag) {
        this.deleteMongoFlag = deleteMongoFlag;
    }

    public void setSecureDelete(boolean secureDelete) {
        this.secureDelete = secureDelete;
    }

    public void setThingToProcess(Thing thingToProcess) {
        this.thingToProcess = thingToProcess;
    }

    public void setThingParentToProcess(Thing thingParentToProcess) {
        this.thingParentToProcess = thingParentToProcess;
    }

    public void setThingParentMapToProcess(Map<String, Object> thingParentMapToProcess) {
        this.thingParentMapToProcess = thingParentMapToProcess;
    }

    private String formatSerialNumber(String serialNumber) {
        return (StringUtils.isNotEmpty(serialNumber)) ? serialNumber.toUpperCase() : serialNumber;
    }

    public Cache getCache() {
        return cache;
    }

    public boolean isOlderTimestamp() {
        return isOlderTimestamp;
    }

    public void setOlderTimestamp(boolean olderTimestamp) {
        isOlderTimestamp = olderTimestamp;
    }

    public boolean isCreation() {
        return isCreation;
    }

    public void setCreation(boolean creation) {
        isCreation = creation;
    }

    public static class CrudParametersBuilder {
        private String thingTypeCode;
        private String groupHierarchyCode;
        private String name;
        private String serialNumber;
        private Object parent;
        private Map<String, Object> udfs;
        private List children;
        private List childrenUdf;
        private boolean executeTickle;
        private boolean validateVisibility;
        private Date transactionDate;
        private boolean disableFMCLogic;
        private boolean createAndFlush;
        private Boolean useDefaultValues;
        private Map<String, Boolean> validations;
        private String facilityCode;
        private boolean fillSource;
        private Subject subject;
        private User currentUser;
        private Cache cache;
        private boolean isCreation = false;

        private void validateNullValue(Object value, String message) {
            if (value == null)
                throw new IllegalArgumentException(message);
        }

        public CrudParametersBuilder setThingTypeCode(String thingTypeCode) {
            validateNullValue(thingTypeCode, "'thingTypeCode' mustn't be null");
            this.thingTypeCode = thingTypeCode;
            return this;
        }

        public CrudParametersBuilder setGroupHierarchyCode(String groupHierarchyCode) {
            /*validateNullValue(groupHierarchyCode, "'groupHierarchyCode' mustn't be null");*/
            this.groupHierarchyCode = groupHierarchyCode;
            return this;
        }

        public CrudParametersBuilder setName(String name) {
            /*validateNullValue(name, "'name' mustn't be null");*/
            this.name = name;
            return this;
        }

        public CrudParametersBuilder setSerialNumber(String serialNumber) {
            validateNullValue(serialNumber, "'serialNumber' mustn't be null");
            this.serialNumber = serialNumber;
            return this;
        }

        public CrudParametersBuilder setParent(Object parent) {
            this.parent = parent;
            return this;
        }

        public CrudParametersBuilder setUdfs(Map<String, Object> udfs) {
            this.udfs = udfs;
            return this;
        }

        public CrudParametersBuilder setChildren(List children) {
            this.children = children;
            return this;
        }

        public CrudParametersBuilder setChildrenUdf(List childrenUdf) {
            this.childrenUdf = childrenUdf;
            return this;
        }

        public CrudParametersBuilder setExecuteTickle(boolean executeTickle) {
            this.executeTickle = executeTickle;
            return this;
        }

        public CrudParametersBuilder setValidateVisibility(boolean validateVisibility) {
            this.validateVisibility = validateVisibility;
            return this;
        }

        public CrudParametersBuilder setTransactionDate(Date transactionDate) {
            validateNullValue(transactionDate, "'transactionDate' mustn't be null");
            this.transactionDate = transactionDate;
            return this;
        }

        public CrudParametersBuilder setDisableFMCLogic(boolean disableFMCLogic) {
            this.disableFMCLogic = disableFMCLogic;
            return this;
        }

        public CrudParametersBuilder setCreateUpdateAndFlush(boolean createAndFlush) {
            this.createAndFlush = createAndFlush;
            return this;
        }

        public CrudParametersBuilder setUseDefaultValues(Boolean useDefaultValues) {
            this.useDefaultValues = (useDefaultValues == null) ? Boolean.TRUE : useDefaultValues;
            return this;
        }

        public CrudParametersBuilder setValidations(Map<String, Boolean> validations) {
            this.validations = validations;
            return this;
        }

        public CrudParametersBuilder setFacilityCode(String facilityCode) {
            this.facilityCode = facilityCode;
            return this;
        }

        public CrudParametersBuilder setFillSource(boolean fillSource) {
            this.fillSource = fillSource;
            return this;
        }

        public CrudParametersBuilder setSubject(Subject subject) {
            validateNullValue(subject, "'subject' mustn't be null");
            this.subject = subject;
            if (this.currentUser == null) {
                this.currentUser = (User) subject.getPrincipal();
                validateNullValue(currentUser, "'currentUser' mustn't be null");
            }
            return this;
        }

        public CrudParametersBuilder setCurrentUser(User currentUser) {
            validateNullValue(currentUser, "'currentUser' mustn't be null");
            this.currentUser = currentUser;
            return this;
        }

        public CrudParametersBuilder setCache(Cache cache) {
            this.cache = cache;
            return this;
        }

        public CrudParametersBuilder setCreation(boolean creation) {
            isCreation = creation;
            return this;
        }

        public CrudParameters build() {
            return new CrudParameters(thingTypeCode, groupHierarchyCode, name, serialNumber, parent, udfs,
                    children, childrenUdf, executeTickle, validateVisibility, transactionDate, disableFMCLogic,
                    createAndFlush, useDefaultValues, validations, facilityCode, fillSource, subject, currentUser,
                    cache, isCreation);
        }
    }
}
