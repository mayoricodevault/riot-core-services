package com.tierconnect.riot.iot.reports.views.things;

import com.tierconnect.riot.iot.reports.views.things.dto.Parameters;

/**
 * Created by julio.rocha on 07-07-17.
 */
class ThingListAsTable extends ThingList {
    ThingListAsTable(Parameters parameters) {
        super(parameters);
    }

    @Override
    protected void buildPartialQuery() {
        queryCondition = thingConditionBuilder.buildDefaultFilterConditionBuilder(parameters);
    }
}
