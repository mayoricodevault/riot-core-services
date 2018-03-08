package com.tierconnect.riot.iot.controllers;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.controllers.UserController;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.ThingTypeService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by agutierrez on 11/8/2014.
 */
public class IOTController {

    public static Map<String, Group> getThingVisibilityGroups(Long visibilityGroupId) {
        Map<String, Group> result = new HashMap<>();
        Group visibilityGroupThing = VisibilityUtils.getVisibilityGroup(Thing.class.getCanonicalName(), null);
        BooleanBuilder beGT = new BooleanBuilder();
        beGT = beGT.and(VisibilityUtils.limitVisibilityPredicate(visibilityGroupThing,  QThingType.thingType.group, true, true));
        List<ThingType> visibleThingTypes = ThingTypeService.getInstance().getThingTypeDAO().selectAll(beGT, null, null);
        visibilityGroupThing = VisibilityUtils.getVisibilityGroup(Thing.class.getCanonicalName(), visibilityGroupId);
        result.put("thing", visibilityGroupThing);

        Group anyThingVisibilityGroup = visibilityGroupThing;
        for (ThingType thingType: visibleThingTypes) {
            Group visibilityGroupTT = VisibilityUtils.getVisibilityGroup(Resource.THING_TYPE_PREFIX + thingType.getId(), visibilityGroupId);
            if (visibilityGroupTT.getTreeLevel() < anyThingVisibilityGroup.getTreeLevel()) {
                anyThingVisibilityGroup=visibilityGroupTT;
            }
            result.put("_" + thingType.getName(), visibilityGroupTT);
        }
        result.put("_anyThing", anyThingVisibilityGroup);
        return result;
    }

    //Do not remove this is used with reflection
    public static Map<String, Object> calculateVisibilityGroups() {
        Map<String, Object> visibilityGroup = new HashMap<>();
        visibilityGroup.put("thingType", VisibilityUtils.getVisibilityGroup(ThingType.class.getCanonicalName(), null).publicMap(true));
        Map<String, Group> thingVisibilityGroups = getThingVisibilityGroups(null);
        Group anyThingVisibilityGroup = thingVisibilityGroups.get("_anyThing");
        for (Map.Entry<String, Group> entry: thingVisibilityGroups.entrySet()) {
            visibilityGroup.put(entry.getKey(),  UserController.toSmallMap(entry.getValue()));
        }
        visibilityGroup.put("_anyThing", anyThingVisibilityGroup.publicMap(true));
        visibilityGroup.put("thingParentHistory", UserController.toSmallMap(VisibilityUtils.getVisibilityGroup(ThingParentHistory.class.getCanonicalName(), null)));
        visibilityGroup.put("thingFieldValue", UserController.toSmallMap(VisibilityUtils.getVisibilityGroup(ThingFieldValue.class.getCanonicalName(), null)));
        visibilityGroup.put("zone", UserController.toSmallMap(VisibilityUtils.getVisibilityGroup(Zone.class.getCanonicalName(), null)));
        visibilityGroup.put("zoneGroup", UserController.toSmallMap(VisibilityUtils.getVisibilityGroup(ZoneGroup.class.getCanonicalName(), null)));
        visibilityGroup.put("zonePoint", UserController.toSmallMap(VisibilityUtils.getVisibilityGroup(ZonePoint.class.getCanonicalName(), null)));
        visibilityGroup.put("zoneType", UserController.toSmallMap(VisibilityUtils.getVisibilityGroup(ZoneType.class.getCanonicalName(), null)));
        visibilityGroup.put("localMap", UserController.toSmallMap(VisibilityUtils.getVisibilityGroup(LocalMap.class.getCanonicalName(), null)));
        visibilityGroup.put("reportDefinition", UserController.toSmallMap(VisibilityUtils.getVisibilityGroup(ReportDefinition.class.getCanonicalName(), null)));
        visibilityGroup.put("reportFilter", UserController.toSmallMap(VisibilityUtils.getVisibilityGroup(ReportFilter.class.getCanonicalName(), null)));
        visibilityGroup.put("edgebox", UserController.toSmallMap(VisibilityUtils.getVisibilityGroup(Edgebox.class.getCanonicalName(), null)));
        visibilityGroup.put("connection", UserController.toSmallMap(VisibilityUtils.getVisibilityGroup(Connection.class.getCanonicalName(), null)));
        visibilityGroup.put("connectionType", UserController.toSmallMap(VisibilityUtils.getVisibilityGroup(ConnectionType.class.getCanonicalName(), null)));
        visibilityGroup.put("reportExecution", anyThingVisibilityGroup.publicMap(true));
        return visibilityGroup;
    }

    //Do not remove this is used with reflection
    public static List<String> calculateThingsPermissions() {
        BooleanBuilder beGT = new BooleanBuilder();
        Group thingVisibilityGroup = VisibilityUtils.getVisibilityGroup(Thing.class.getCanonicalName(), null);
        beGT = beGT.and(VisibilityUtils.limitVisibilityPredicate(thingVisibilityGroup,  QThingType.thingType.group, true, true));
        List<ThingType> visibleThingTypes = ThingTypeService.getInstance().getThingTypeDAO().selectAll(beGT, null, null);
        List<String> result = new ArrayList<>();
        for (ThingType thingType: visibleThingTypes) {
            result.add(Resource.THING_TYPE_PREFIX + thingType.getId());
        }
        return result;
    }
}
