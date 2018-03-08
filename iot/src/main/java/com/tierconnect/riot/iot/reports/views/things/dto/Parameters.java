package com.tierconnect.riot.iot.reports.views.things.dto;

import com.tierconnect.riot.api.database.base.ordination.Order;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.iot.entities.ThingType;

import com.tierconnect.riot.iot.utils.VisibilityThingUtils;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.subject.Subject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by julio.rocha on 07-07-17.
 */
public class Parameters {
    public static final String SERIAL_NUMBER_PARAMETER_REGEX = "serialNumber=/(.+)/";
    public static final String ONLY_SERIAL_NUMBER_PARAMETER_REGEX = "serialNumber='(.+)'";
    public static final String ONLY_NAME_PARAMETER_REGEX = "name=/(.+)/";
    public static final String THING_TYPE_ID_PARAMETER_REGEX = "thingTypeId=(\\d+)[&]?";
    public static final String THING_ID_PARAMETER_REGEX = "_id=(\\d+)[&]?";

    private Integer pageSize;
    private Integer pageNumber;
    private String order;
    private String where;
    private String extra;
    private String only;
    private String groupBy;
    private Long visibilityGroupId;
    private String upVisibility;
    private String downVisibility;
    private boolean treeView;
    private Subject subject;
    private User currentUser;
    private boolean returnFavorite;
    //built parameters
    private String serialNumber = null;
    private String name = null;
    private Long thingTypeId = null;
    private Long thingId = null;
    private Map<Long, List<ThingType>> groups;
    private Integer skip;
    private Map<String, Order> sort;
    private String sortField;
    private boolean equalsSerialNumber;
    private boolean equalsName;
    private boolean includeResults;
    private boolean includeTotal;

    public Parameters(Integer pageSize,
                      Integer pageNumber,
                      String order,
                      String where,
                      String extra,
                      String only,
                      String groupBy,
                      Long visibilityGroupId,
                      String upVisibility,
                      String downVisibility,
                      boolean treeView,
                      Subject subject,
                      User currentUser,
                      boolean returnFavorite,
                      boolean includeResults,
                      boolean includeTotal) {
        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
        this.order = order;
        this.where = where;
        this.extra = extra;
        this.only = only;
        this.groupBy = groupBy;
        this.visibilityGroupId = visibilityGroupId;
        this.upVisibility = upVisibility;
        this.downVisibility = downVisibility;
        this.treeView = treeView;
        this.subject = subject;
        this.currentUser = currentUser;
        this.returnFavorite = returnFavorite;
        this.equalsSerialNumber = false;
        this.includeResults = includeResults;
        this.includeTotal = includeTotal;
        init();
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public Integer getPageNumber() {
        return pageNumber;
    }

    public String getOrder() {
        return order;
    }

    public String getWhere() {
        return where;
    }

    public String getExtra() {
        return extra;
    }

    public String getOnly() {
        return only;
    }

    public String getGroupBy() {
        return groupBy;
    }

    public Long getVisibilityGroupId() {
        return visibilityGroupId;
    }

    public String getUpVisibility() {
        return upVisibility;
    }

    public String getDownVisibility() {
        return downVisibility;
    }

    public boolean isTreeView() {
        return treeView;
    }

    public Subject getSubject() {
        return subject;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isReturnFavorite() {
        return returnFavorite;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getName() {
        return name;
    }

    public Long getThingTypeId() {
        return thingTypeId;
    }

    public Long getThingId() {
        return thingId;
    }

    public Map<Long, List<ThingType>> getGroups() {
        return groups;
    }

    public Integer getSkip() {
        return skip;
    }

    public Map<String, Order> getSort() {
        return sort;
    }

    public String getSortField() {
        return sortField;
    }

    public boolean isEqualsSerialNumber() {
        return equalsSerialNumber;
    }

    public boolean isEqualsName() {
        return equalsName;
    }

    public boolean isIncludeResults() {
        return includeResults;
    }

    public boolean isIncludeTotal() {
        return includeTotal;
    }

    private void init() {
        buildWhereParameters();
        initializeVisibility();
        validateParameters();
        buildPagination();
        buildSortParameter();
    }

    private void buildWhereParameters() {
        if (StringUtils.isNotEmpty(where)) {
            serialNumber = ParameterExtractor.getValueFromWhereParam(where, SERIAL_NUMBER_PARAMETER_REGEX, "serialNumber");
            name = serialNumber;
            if (StringUtils.isEmpty(serialNumber)) {
                serialNumber = ParameterExtractor.getValueFromWhereParam(where, ONLY_SERIAL_NUMBER_PARAMETER_REGEX, "serialNumber");
                if (StringUtils.isEmpty(serialNumber)) {
                    name = ParameterExtractor.getValueFromWhereParam(where, ONLY_NAME_PARAMETER_REGEX, "name");
                    equalsName = true;
                } else {
                    equalsSerialNumber = true;
                }
            }
            String id = ParameterExtractor.getValueFromWhereParam(where, THING_TYPE_ID_PARAMETER_REGEX, "thingTypeId");
            thingTypeId = (id != null) ? new Long(id) : null;
            id = ParameterExtractor.getValueFromWhereParam(where, THING_ID_PARAMETER_REGEX, "_id");
            thingId = (id != null) ? new Long(id) : null;
        }
    }

    private void initializeVisibility() {
        this.visibilityGroupId = (visibilityGroupId == null) ? currentUser.getActiveGroup().getId() : visibilityGroupId;
        this.groups = VisibilityThingUtils.calculateThingsVisibility(visibilityGroupId, subject);
        if (groups == null || groups.isEmpty()) {
            throw new UserException("Group of the user is not valid.");
        }
    }

    private void validateParameters() {
        StringBuffer message = new StringBuffer("");
        if (pageNumber != null && pageNumber.intValue() < 1) {
            message.append(String.format("'pageNumber' should have a number greater than 0."));
        }
        if (pageSize != null && pageSize.intValue() != -1 && pageSize.intValue() < 1) {
            message.append(String.format("'pageSize' should have a number greater than 0 or be equals to -1."));
        }
        if (pageSize != null && pageSize.intValue() == -1 && treeView) {
            message.append(String.format("'pageSize' should not be equals to -1 when tree view is active."));
        }
        if (groupBy != null && !groupBy.trim().equals("") && only == null || (only != null && only.trim().equals(""))) {
            message.append(String.format("If you want to get results with 'groupBy', 'only' cannot be empty."));
        }
        if (message != null && message.length() > 0) {
            throw new UserException(message.toString());
        }
    }

    private void buildPagination() {
        pageSize = (pageSize == null) ? 1 : (pageSize < 1) ? 0 : pageSize;
        this.skip = (pageNumber != null && pageNumber > 1) ? (pageNumber - 1) * pageSize : 0;
    }

    private void buildSortParameter() {
        sort = new LinkedHashMap<>(1);
        if (order != null) {
            String[] split = order.split(":");
            this.sortField = split[0];
            Order order = Order.ASC;
            if (split[1].equals("desc")) {
                order = Order.DESC;
            }
            sort.put(split[0], order);
        }
    }
}
