package com.tierconnect.riot.iot.reports.views.things.dto;

import java.util.List;

/**
 * Created by achambi on 8/3/17.
 * Dao tor save the visibility Permission for list things.
 */
public class VisibilityPermission {

    private List<Long> listGroupIds;
    private List<Long> listThingTypeIds;


    public VisibilityPermission(List<Long> listGroupIds, List<Long> listThingTypeIds) {
        this.listGroupIds = listGroupIds;
        this.listThingTypeIds = listThingTypeIds;
    }

    public List<Long> getListGroupIds() {
        return listGroupIds;
    }

    public List<Long> getListThingTypeIds() {
        return listThingTypeIds;
    }
}
