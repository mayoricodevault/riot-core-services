package com.tierconnect.riot.iot.utils;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.GroupServiceBase;
import com.tierconnect.riot.appcore.services.GroupTypeService;
import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.iot.controllers.ThingController;
import com.tierconnect.riot.iot.entities.QThing;
import com.tierconnect.riot.iot.entities.QThingType;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import java.util.*;

import static com.tierconnect.riot.appcore.utils.VisibilityUtils.getSubjectIfIsNull;
import static com.tierconnect.riot.appcore.utils.VisibilityUtils.limitVisibilityPredicate;

/**
 *
 * Limit visibility based on relationship between the user's group and the subject object's groups in the group hierarchy
 *
 *
 * Created by fflores on 4/9/2015.
 */
public class VisibilityThingUtils
{


    public static BooleanBuilder limitSelectAllT(String upVisibility, String downVisibility, Long visibilityGroupId) {
        QThing qThing = QThing.thing;
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();

        EntityVisibility entityVisibility = new ThingController().getEntityVisibility();

        boolean up = StringUtils.isEmpty(upVisibility) ? entityVisibility.isNotFalseUpVisibility() : Boolean.parseBoolean(upVisibility);
        boolean down = StringUtils.isEmpty(downVisibility) ? entityVisibility.isDownVisibility() : Boolean.parseBoolean(downVisibility);

        Map<Long, List<ThingType>> map = calculateThingsVisibility(visibilityGroupId);
        if (map == null || map.isEmpty()) {
            return null;
        }

        BooleanBuilder masterVisibilityOr = new BooleanBuilder();
        for (Map.Entry<Long, List<ThingType>> entry : map.entrySet()) {

            Group visibilityGroup = GroupService.getInstance().get(entry.getKey());
            List<ThingType> permittedThingTypes = entry.getValue();

            BooleanBuilder be = new BooleanBuilder();
            // 2. Limit visibility based on user's group and the object's group
            // (group based authorization)
            GroupType visibilityGroupType = visibilityGroup.getGroupType();
            BooleanBuilder classVisibilityDownVisibility = new BooleanBuilder(limitVisibilityPredicate(visibilityGroup, qThing.group, false, down));
            BooleanBuilder objectVisibilityUp = new BooleanBuilder(limitVisibilityPredicate(visibilityGroup, qThing.group, up, false));
            BooleanBuilder objectVisibilityUpGroupTypeFloor = new BooleanBuilder(qThing.groupTypeFloor.id.eq(visibilityGroupType.getId()));
            BooleanBuilder objectVisibilityUpCreatedByUser = new BooleanBuilder(qThing.createdByUser.id.eq(currentUser.getId()));
            List<Long> descendants = GroupTypeService.getInstance().getDescendantIds(visibilityGroupType);
            for (Long descendantId : descendants) {
                objectVisibilityUpGroupTypeFloor = objectVisibilityUpGroupTypeFloor.or(qThing.groupTypeFloor.id.eq(descendantId));
            }
            if (PermissionsUtils.isPermitted(subject, "thing_editOwn:u")) {
                BooleanBuilder aux = new BooleanBuilder(objectVisibilityUpGroupTypeFloor.or(objectVisibilityUpCreatedByUser));
                BooleanBuilder aux2 = new BooleanBuilder(objectVisibilityUp).and(aux);
                be = be.and(classVisibilityDownVisibility.or(aux2));
            } else {
                BooleanBuilder aux2 = new BooleanBuilder(objectVisibilityUp).and(objectVisibilityUpGroupTypeFloor);
                be = be.and(classVisibilityDownVisibility.or(objectVisibilityUp.and(aux2)));
            }

            // implement ThingType filtering
            BooleanBuilder beThingTypeFilter = new BooleanBuilder();
            for (ThingType thingType : permittedThingTypes) {
                beThingTypeFilter.or(qThing.thingType.id.eq(thingType.getId()));
            }
            be = be.and(beThingTypeFilter);
            masterVisibilityOr = masterVisibilityOr.or(be);
        }

        return masterVisibilityOr;
    }

    public static List<Thing> getVisibleThings(Long visibilityGroupId, String upVisibility, String downVisibility)
    {
        BooleanBuilder masterVisibilityOr = limitSelectAllT(upVisibility, downVisibility, visibilityGroupId);

        BooleanBuilder masterBe = new BooleanBuilder( masterVisibilityOr );

        List<Thing> things = ThingService.getInstance().listPaginated( masterBe, null, null );

        return things;
    }

    public static void limitVisibilityInsertT(ThingType thingType, Group objectGroup){
        limitVisibilityInsertT(thingType, objectGroup, null);
    }

    public static void limitVisibilityInsertT(ThingType thingType, Group objectGroup, Subject subject){
        limitVisibilityInsertT(thingType, objectGroup, subject, true);
    }

    public static void limitVisibilityInsertT(ThingType thingType, Group objectGroup, Subject subject, boolean initShiro)
    {
        subject = getSubjectIfIsNull(subject);
        Group visibilityGroup = getVisibilityGroup( thingType, subject, initShiro );

        if( !PermissionsUtils.isPermitted( subject, Resource.THING_TYPE_PREFIX + thingType.getId() + ":i" ) )
        {
            throw new ForbiddenException( "Not Allowed access" );
        }

        GroupService groupService = GroupService.getInstance();
        if( objectGroup == null )
            return;
        //You can insert objects in you same group (!up, !down)
        if (visibilityGroup.getId().equals(objectGroup.getId())) {
            return;
        }
        //test down visibility
        if( groupService.isGroupNotInsideTree(objectGroup, visibilityGroup) )
        {
            throw new ForbiddenException( "Not visible" );
        }

    }

    public static void limitVisibilityUpdateT(ThingType thingType, Group objectGroup) {
        Group visibilityGroup = getVisibilityGroup( thingType );

        if( !PermissionsUtils.isPermitted( SecurityUtils.getSubject(), Resource.THING_TYPE_PREFIX + thingType.getId() + ":u" ) )
        {
            throw new ForbiddenException( "Not Allowed access" );
        }

        GroupService groupService = GroupService.getInstance();
        if( objectGroup == null )
            return;
        //You can insert objects in you same group (!up, !down)
        if (visibilityGroup.getId().equals(objectGroup.getId())) {
            return;
        }
        //test down visibility
        if( groupService.isGroupNotInsideTree(objectGroup, visibilityGroup) )
        {
            throw new ForbiddenException( "Not visible" );
        }
    }

    public static void limitVisibilityUpdateT(Thing thing, Group objectGroup)
    {
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();
        Group visibilityGroup = getVisibilityGroup( thing );

        if( !PermissionsUtils.isPermitted( subject, Resource.THING_TYPE_PREFIX + thing.getThingType().getId() + ":u:" + thing.getId() ) )
        {
            if( !(thing.getCreatedByUser().getId().equals( currentUser.getId() ) && PermissionsUtils.isPermitted( subject,
                    "thing_editOwn:u" )) )
            {
                throw new ForbiddenException( "Not Allowed access" );
            }
        }

        Group objectOldGroup = thing.getGroup();
        User createdByUser = PermissionsUtils.isPermitted(subject, "thing_editOwn:u") ? thing.getCreatedByUser() : null;
        GroupService groupService = GroupService.getInstance();
        if (objectGroup != null) {
            //You can update objects in you same group (!up, !down)
            if (visibilityGroup.getId().equals(objectOldGroup.getId()) && visibilityGroup.getId().equals(objectGroup.getId())) {
                return;
            }
            boolean downVisible = groupService.isGroupInsideTree(objectOldGroup, visibilityGroup) && groupService.isGroupInsideTree(objectGroup, visibilityGroup);
            if (downVisible) {
                return;
            }
            boolean upVisible = groupService.isGroupInsideTree(visibilityGroup, objectOldGroup) && groupService.isGroupInsideTree(visibilityGroup, objectGroup);
            if (upVisible) {
                //Test special up visibility for createdByUser
                if (createdByUser != null) {
                    User currentUser1 = (User) SecurityUtils.getSubject().getPrincipal();
                    if (currentUser1.getId().equals(createdByUser.getId())) {
                        return;
                    }
                }
            }
        } else {
            //You can update objects in you same group (!up, !down)
            if (visibilityGroup.getId().equals(objectOldGroup.getId())) {
                return;
            }
            boolean downVisible = groupService.isGroupInsideTree(objectOldGroup, visibilityGroup);
            if (downVisible) {
                return;
            }
            boolean upVisible = groupService.isGroupInsideTree(visibilityGroup, objectOldGroup);
            if (upVisible) {
                //Test special up visibility for createdByUser
                if (createdByUser != null) {
                    User currentUser1 = (User) SecurityUtils.getSubject().getPrincipal();
                    if (currentUser1.getId().equals(createdByUser.getId())) {
                        return;
                    }
                }
            }
        }
        throw new ForbiddenException("Not visible");
    }

    public static void limitVisibilityDeleteT(Thing thing, Subject subject) {
        limitVisibilityDeleteT(thing, subject, true);
    }

    public static void limitVisibilityDeleteT(Thing thing, Subject subject, boolean initShiro) {
        subject = getSubjectIfIsNull(subject);
        Group visibilityGroup = getVisibilityGroup( thing, subject, initShiro );

        if (!PermissionsUtils.isPermitted(subject, "thing:d:" + thing.getId())) {
            if (!PermissionsUtils.isPermitted(subject, Resource.THING_TYPE_PREFIX + thing.getThingType().getId() + ":d:" + thing.getId())) {
                throw new ForbiddenException("Not Allowed access");
            }
        }
        Group objectGroup = thing.getGroup();
        //You can delete objects in you same group (!up, !down)
        if (visibilityGroup.getId().equals(objectGroup.getId())) {
            return;
        }
        //test down visibility
        GroupService groupService = GroupService.getInstance();
        if( groupService.isGroupNotInsideTree(objectGroup, visibilityGroup) )
        {
            throw new ForbiddenException( "Not visible" );
        }
    }

    private static List<ThingType> getListThingTypes(){
        Map<Long, String> visibleThingTypesMap = getVisibleThingTypes();
        List<ThingType> visibleThingTypes = new ArrayList<>();
        if (visibleThingTypesMap.size() > 0) {
            BooleanBuilder beGT = new BooleanBuilder();
            beGT = beGT.and(QThingType.thingType.id.in(visibleThingTypesMap.keySet()));
            visibleThingTypes = ThingTypeService.getThingTypeDAO().selectAll(beGT, null, null);
        }
        return visibleThingTypes;
    }

    public static List<Long> getListThingTypesID(){
        Map<Long, String> visibleThingTypesMap = getVisibleThingTypes();
        List<Long> visibleIdThingTypes = new ArrayList<>();
        if (!visibleThingTypesMap.isEmpty()) {
            BooleanBuilder beGT = new BooleanBuilder();
            beGT = beGT.and(QThingType.thingType.id.in(visibleThingTypesMap.keySet()));
            visibleIdThingTypes = ThingTypeService.getThingTypeDAO().getQuery().where(beGT).list(QThingType.thingType.id);
        }
        return visibleIdThingTypes;
    }

    public static Map<Long, List<Long>> calculateVisibilityThingsID(Long visibilityGroupId) {
        // VisibilityMap
        Map<Long, List<Long>> map = new HashMap<>();
        Subject subject = SecurityUtils.getSubject();
        List<ThingType> visibleThingTypes = getListThingTypes();
        for (ThingType thingType : visibleThingTypes) {
            if (PermissionsUtils.isPermitted(subject, Resource.THING_TYPE_PREFIX + thingType.getId() + ":r")) {
                Group visibilityGroup1 = VisibilityUtils.getVisibilityGroup(Resource.THING_TYPE_PREFIX + thingType.getId(), visibilityGroupId);
                if (map.get(visibilityGroup1.getId()) == null) {
                    map.put(visibilityGroup1.getId(), new ArrayList<Long>());
                }
                map.get(visibilityGroup1.getId()).add(thingType.getId());
            }
        }
        return map;
    }

    public static Map<Long, List<ThingType>> calculateThingsVisibility(Long visibilityGroupId) {
        return calculateThingsVisibility(visibilityGroupId, null);
    }

    /**
     * Calculate the things visibility for a user.
     *
     * @param visibilityGroupId A {@link Long} containing the visibility group Id.
     * @param subject           A {@link Subject} instance to verify thing type visibility.
     * @return A instance of {@link Map}<{@link Long},{@link List}<{@link ThingType}>>
     * containing the thingType permission per GroupId.
     */
    public static Map<Long, List<ThingType>> calculateThingsVisibility(Long visibilityGroupId, Subject subject) {
        subject = getSubjectIfIsNull(subject);
        // VisibilityMap
        Map<Long, List<ThingType>> map = new HashMap<>();
        Map<Long, String> visibleThingTypesMap = getVisibleThingTypes(subject);
        List<ThingType> visibleThingTypes = new ArrayList<>();
        if (visibleThingTypesMap.size() > 0) {
            BooleanBuilder beGT = new BooleanBuilder();
            for (Long ttId : visibleThingTypesMap.keySet()) {
                if (ttId == null) {
                    beGT = beGT.and(QThingType.thingType.id.in(visibleThingTypesMap.keySet()));
                    break;
                }
            }
            visibleThingTypes = ThingTypeService.getInstance().getThingTypeDAO().selectAll(beGT, null, null);
        }
        for (ThingType thingType : visibleThingTypes) {
            if (PermissionsUtils.isPermitted(subject, Resource.THING_TYPE_PREFIX + thingType.getId() + ":r")) {
                Group visibilityGroup1 = VisibilityUtils.getVisibilityGroup(Resource.THING_TYPE_PREFIX + thingType.getId(),
                        visibilityGroupId, subject);
                if (map.get(visibilityGroup1.getId()) == null) {
                    map.put(visibilityGroup1.getId(), new ArrayList<>());
                }
                map.get(visibilityGroup1.getId()).add(thingType);
            }
        }
        return map;
    }

    public static Map<Long, String> getVisibleThingTypes() {
        return getVisibleThingTypes(null);
    }

    public static Map<Long, String> getVisibleThingTypes(Subject subject) {
        //TODO please don't delete it calls getAuthorizationInfo from Shiro
        subject = getSubjectIfIsNull(subject);
        subject.isPermitted("initShiro");
        return RiotShiroRealm.getVisibilityTypeCache();
    }

    public static Set<Long> getVisibleThingTypeList(Subject subject) {
        return getVisibleThingTypes(subject).keySet();
    }

    public static Group getVisibilityGroup(Thing thing){
        return getVisibilityGroup(thing, null);
    }

    public static Group getVisibilityGroup(Thing thing, Subject subject){
        return getVisibilityGroup(thing, subject, true);
    }

    public static Group getVisibilityGroup(Thing thing, Subject subject, boolean initShiro)
    {
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup( Thing.class.getCanonicalName(), null, subject, initShiro );
        // RiotShiroRealm.setVisibilityGroup(null);
        Group visibilityGroup2 = VisibilityUtils.getVisibilityGroup( Resource.THING_TYPE_PREFIX + thing.getThingType().getId(), null, subject, initShiro );
        if( visibilityGroup2.getTreeLevel() < visibilityGroup.getTreeLevel() )
        {
            visibilityGroup = visibilityGroup2;
        }
        // RiotShiroRealm.setVisibilityGroup(visibilityGroup);
        return visibilityGroup;
    }

    public static Group getVisibilityGroup( ThingType thingTypeType ){
        return getVisibilityGroup(thingTypeType, null);
    }

    public static Group getVisibilityGroup( ThingType thingTypeType, Subject subject){
        return getVisibilityGroup(thingTypeType, subject, true);
    }

    public static Group getVisibilityGroup( ThingType thingTypeType, Subject subject, boolean initShiro)
    {
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup( Thing.class.getCanonicalName(), null , subject, initShiro);
        // RiotShiroRealm.setVisibilityGroup(null);
        Group visibilityGroup2 = VisibilityUtils.getVisibilityGroup( Resource.THING_TYPE_PREFIX + thingTypeType.getId(), null, subject, initShiro);
        if( visibilityGroup2.getTreeLevel() < visibilityGroup.getTreeLevel() )
        {
            visibilityGroup = visibilityGroup2;
        }
        // RiotShiroRealm.setVisibilityGroup(visibilityGroup);
        return visibilityGroup;
    }

    public static Group calculateUpperVisibilityGroup( Map<Long, List<ThingType>> map )
    {
        GroupService groupService = GroupServiceBase.getInstance();
        Group max = null;
        for( Long l : map.keySet() )
        {
            Group g = groupService.get( l );
            if( max == null )
            {
                max = g;
            }
            else if( g.getTreeLevel() < max.getTreeLevel() )
            {
                max = g;
            }
        }
        return max;
    }

    public static void limitVisibilitySelectT(Thing thing, Group currentUserVisibilityGroup, Subject subject) {
        EntityVisibility entityVisibility = new ThingController().getEntityVisibility();
        limitVisibilitySelect(currentUserVisibilityGroup, thing.getGroup(), entityVisibility.isNotFalseUpVisibility(), entityVisibility.isDownVisibility(),
                thing.getGroupTypeFloor(), PermissionsUtils.isPermitted(subject, "thing_editOwn:u") ? thing.getCreatedByUser() : null,
                null, null);
    }

    public static void limitVisibilitySelectT(Thing thing, Group currentUserVisibilityGroup, User createdByUser, User currentUser) {
        EntityVisibility entityVisibility = new ThingController().getEntityVisibility();
        limitVisibilitySelect(currentUserVisibilityGroup, thing.getGroup(), entityVisibility.isNotFalseUpVisibility(), entityVisibility.isDownVisibility(),
                thing.getGroupTypeFloor(), createdByUser, currentUser, null, null);
    }

    @Deprecated
    private static void limitVisibilitySelect(Group currentUserVisibilityGroup, Group objectGroup, boolean up, boolean down, GroupType floorGroupType, User createdByUser,
                                              Set<Long> allowedRoles, Set<Long> allowedGroups) {
        limitVisibilitySelect(currentUserVisibilityGroup, objectGroup, up, down, floorGroupType, createdByUser, ((User) SecurityUtils.getSubject().getPrincipal()),
                allowedRoles, allowedGroups);
    }

    @Deprecated
    private static void limitVisibilitySelect(Group currentUserVisibilityGroup, Group objectGroup, boolean up, boolean down, GroupType floorGroupType, User createdByUser,
                                              User currentUser, Set<Long> allowedRoles, Set<Long> allowedGroups) {
        //test (!up, !down). You can see objects in you same group
        if (currentUserVisibilityGroup.getId().equals(objectGroup.getId())) {
            return;
        }
        GroupService groupService = GroupService.getInstance();
        //test (!up && down) visibility
        if (!up && down) {
            if (groupService.isGroupNotInsideTree(objectGroup, currentUserVisibilityGroup)) {
                //test up visibility
                if (groupService.isGroupNotInsideTree(currentUserVisibilityGroup, objectGroup)) {
                    throw new ForbiddenException("Not visible");
                }
                if (createdByUser == null && floorGroupType == null && allowedRoles == null && allowedGroups == null) {
                    throw new ForbiddenException("Not visible");
                }
//                User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
                //test special up visibility read/edit own
                if (createdByUser != null) {
                    if (currentUser.getId().equals(createdByUser.getId())) {
                        return;
                    }
                }
                //test special up visibility shared by floorGroupType
                if (floorGroupType != null) {
                    if (floorGroupType.equals(currentUserVisibilityGroup.getGroupType())) {
                        return;
                    }
                    GroupType parent = floorGroupType.getParent();
                    while (parent != null) {
                        if (parent.equals(currentUserVisibilityGroup.getGroupType())) {
                            return;
                        }
                        parent = parent.getParent();
                    }
                }
                //test special up visibility shared by role
                if (allowedRoles != null) {
                    if (currentUser.getRoleIds().removeAll(allowedRoles)) {
                        return;
                    }
                }
                //test special up visibility shared by group
                if (allowedGroups != null) {
                    Set<Long> groupIds = new HashSet<>();
                    groupIds.add(currentUser.getActiveGroup().getId());
                    if (groupIds.removeAll(allowedGroups)) {
                        return;
                    }
                }
                throw new ForbiddenException("Not visible");
            }
        } else if (up && down) {
            //test down visibility
            if (groupService.isGroupNotInsideTree(objectGroup, currentUserVisibilityGroup)) {
            //test up visibility
                if (groupService.isGroupNotInsideTree(currentUserVisibilityGroup, objectGroup)) {
                    throw new ForbiddenException("Not visible");
                }
            }
        // test (up && !down)
        } else {
            if (groupService.isGroupNotInsideTree(currentUserVisibilityGroup, objectGroup)) {
                throw new ForbiddenException("Not visible");
            }
        }
    }
}
