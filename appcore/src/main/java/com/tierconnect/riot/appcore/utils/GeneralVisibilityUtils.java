package com.tierconnect.riot.appcore.utils;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.path.EntityPathBase;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.GroupTypeService;
import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import com.tierconnect.riot.sdk.utils.UpVisibility;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by agutierrez on 6/15/15.
 */
public class GeneralVisibilityUtils {
    static Logger logger = Logger.getLogger(GeneralVisibilityUtils.class);

    //up1->visibility up: for read
    //up2->visibility up: for read limited to sharing type, for write limited to shareToSelft(editOwn Permission)
    //down->visibility down: for  read, write, update, delete, archive
    public static BooleanBuilder limitVisibilitySelectAll(EntityVisibility cVisibility, EntityPathBase base, Group visibilityGroup, String upVisibility, String downVisibility) {
        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();

        GroupType visibilityGroupType = visibilityGroup.getGroupType();
        boolean up = StringUtils.isEmpty(upVisibility) ? cVisibility.isNotFalseUpVisibility() : cVisibility.isNotFalseUpVisibility() && Boolean.parseBoolean(upVisibility);
        boolean down = StringUtils.isEmpty(downVisibility) ? cVisibility.isDownVisibility() : cVisibility.isDownVisibility() && Boolean.parseBoolean(downVisibility);
        //!up && !down
        if (!up && !down) {
            return new BooleanBuilder(VisibilityUtils.limitVisibilityPredicate(visibilityGroup, cVisibility.getQGroup(base), false, false));
        }

        //!up && down
        BooleanBuilder objectVisibilityDown = new BooleanBuilder(VisibilityUtils.limitVisibilityPredicate(visibilityGroup, cVisibility.getQGroup(base), false, true));
        if (!up) {
            return objectVisibilityDown;
        }

        BooleanBuilder objectVisibilityUp = new BooleanBuilder(VisibilityUtils.limitVisibilityPredicate(visibilityGroup, cVisibility.getQGroup(base), true, false));
        if (UpVisibility.TRUE_R.equals(cVisibility.getUpVisibility())) {
            //up1 && !down
            if (!down) {
                return objectVisibilityUp;
                //up1 && down
            } else {
                return objectVisibilityUp.or(objectVisibilityDown);
            }
        }

        BooleanBuilder objectVisibilityUpAux = new BooleanBuilder();

        if (cVisibility.isSharedByGroupType()) {
            BooleanBuilder objectVisibilityUpGroupTypeFloor = new BooleanBuilder(cVisibility.getQGroupTypeFloor(base).eq(visibilityGroupType));
            List<Long> descendants = GroupTypeService.getInstance().getDescendantIds(visibilityGroupType);
            for (Long descendantId : descendants) {
                objectVisibilityUpGroupTypeFloor.or(cVisibility.getQGroupTypeFloor(base).id.eq(descendantId));
            }
            objectVisibilityUpAux = new BooleanBuilder(objectVisibilityUpGroupTypeFloor);
        }

        if (cVisibility.isSharedToSelf() && PermissionsUtils.isPermitted(subject, cVisibility.getEditOwnPermission())) {
            BooleanBuilder objectVisibilityUpCreatedByUser = new BooleanBuilder(cVisibility.getQCreatedByUser(base).eq(currentUser));
            objectVisibilityUpAux = objectVisibilityUpAux.or(objectVisibilityUpCreatedByUser);
        }

        if (cVisibility.isSharedByRole()) {
            objectVisibilityUpAux = objectVisibilityUpAux.or(cVisibility.getQRoleShare(base).id.in(currentUser.getRoleIds()));
        }

        if (cVisibility.isSharedByGroup()) {
            objectVisibilityUpAux = objectVisibilityUpAux.or(cVisibility.getQGroupShare(base).id.in(visibilityGroup.getId(), currentUser.getActiveGroup().getId()));
        }

        //up2 && down
        if (down) {
            return new BooleanBuilder(objectVisibilityDown.or(objectVisibilityUp.and(objectVisibilityUpAux)));
            //up2 && !down
        } else {
            return new BooleanBuilder(objectVisibilityUp.and(objectVisibilityUpAux));
        }
    }


    public static void limitVisibilitySelect(EntityVisibility cVisibility, Object object) {
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(object.getClass().getCanonicalName(), null);
        limitVisibilitySelect(cVisibility, object, visibilityGroup);
    }


    // user for select
    public static void limitVisibilitySelect(EntityVisibility cVisibility, Object object, Group currentUserVisibilityGroup) {

        Group objectGroup = cVisibility.getGroup(object);

        //You can see objects in your same group always
        if (currentUserVisibilityGroup.getId().equals(objectGroup.getId())) {
            return;
        }
        GroupService groupService = GroupService.getInstance();
        //test (!up && down) visibility
        if (UpVisibility.TRUE_R_U_SPECIAL.equals(cVisibility.getUpVisibility())) {

            if (cVisibility.isDownVisibility() && groupService.isGroupInsideTree(objectGroup, currentUserVisibilityGroup)) {
                return;
            }

            //test up visibility
            if (groupService.isGroupNotInsideTree(currentUserVisibilityGroup, objectGroup)) {
                throw new ForbiddenException("Not visible");
            }

            Subject subject = SecurityUtils.getSubject();
            User currentUser = (User) subject.getPrincipal();
            //test special up visibility read/edit own
            if (cVisibility.isSharedToSelf()) {
                User createdByUser = cVisibility.getCreatedByUser(object);
                if (createdByUser != null && currentUser.getId().equals(createdByUser.getId())) {
                    return;
                }
            }
            //test special up visibility shared by floorGroupType
            if (cVisibility.isSharedByGroupType()) {
                GroupType floorGroupType = cVisibility.getGroupTypeFloor(object);
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
            }

            //test special up visibility shared by role
            if (cVisibility.isSharedByRole()) {
                Set<Long> allowedRoles = cVisibility.getAllowedRoles(object);
                if (allowedRoles != null) {
                    if (currentUser.getRoleIds().removeAll(allowedRoles)) {
                        return;
                    }
                }
            }
            //test special up visibility shared by group
            if (cVisibility.isSharedByGroup()) {
                Set<Long> allowedGroups = cVisibility.getAllowedGroups(object);
                if (allowedGroups != null) {
                    Set<Long> groupIds = new HashSet<>();
                    groupIds.add(currentUser.getActiveGroup().getId());
                    if (groupIds.removeAll(allowedGroups)) {
                        return;
                    }
                }
            }
            //test special up, object directly shared by id
            if (PermissionsUtils.isPermitted(subject, cVisibility.getReadOneObjectPermission(cVisibility.getId(object)))) {
                return;
            }
            throw new ForbiddenException("Not visible");
        } else if (UpVisibility.TRUE_R.equals(cVisibility.getUpVisibility())) {
            //test up visibility
            if (groupService.isGroupNotInsideTree(currentUserVisibilityGroup, objectGroup)) {
                if (cVisibility.isDownVisibility()) {
                    //test down visibility
                    if (groupService.isGroupNotInsideTree(objectGroup, currentUserVisibilityGroup)) {
                        throw new ForbiddenException("Not visible");
                    }
                } else {
                    throw new ForbiddenException("Not visible");
                }
            }
        } else if (UpVisibility.FALSE.equals(cVisibility.getUpVisibility())) {
            if (cVisibility.isDownVisibility()) {
                //test down visibility
                if (groupService.isGroupNotInsideTree(objectGroup, currentUserVisibilityGroup)) {
                    throw new ForbiddenException("Not visible");
                }
            }
        }
    }

    public static void limitVisibilityDelete(EntityVisibility cVisibility, Object object) {
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(object.getClass().getCanonicalName(), null);
        limitVisibilityDelete(cVisibility, object, visibilityGroup);
    }


    public static void limitVisibilityDelete(EntityVisibility cVisibility, Object object, Group currentUserVisibilityGroup) {
        Group objectGroup = cVisibility.getGroup(object);

        //You can delete objects in you same group (!up, !down)
        if (currentUserVisibilityGroup.getId().equals(objectGroup.getId())) {
            return;
        }
        //test down visibility
        GroupService groupService = GroupService.getInstance();
        if (cVisibility.isDownVisibility() && groupService.isGroupInsideTree(objectGroup, currentUserVisibilityGroup)) {
            return;
        }
        throw new ForbiddenException("Not visible");
    }

    public static void limitVisibilityInsert(EntityVisibility cVisibility, Group objectGroup) {
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(cVisibility.getEntityPathBase().getType().getCanonicalName(), null);
        limitVisibilityInsert(cVisibility, visibilityGroup, objectGroup);
    }

    // use for insert
    public static void limitVisibilityInsert(EntityVisibility cVisibility, Group currentUserVisibilityGroup, Group objectGroup) {
        GroupService groupService = GroupService.getInstance();
        if (objectGroup == null)
            return;
        //You can insert objects in you same group (!up, !down)
        if (currentUserVisibilityGroup.getId().equals(objectGroup.getId())) {
            return;
        }
        //test down visibility
        if (cVisibility.isDownVisibility() && groupService.isGroupInsideTree(objectGroup, currentUserVisibilityGroup)) {
            return;
        }
        throw new ForbiddenException("Not visible");
    }

    public static void limitVisibilityUpdate(EntityVisibility cVisibility, Object object, Group objectNewGroup) {
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(cVisibility.getEntityPathBase().getType().getCanonicalName(), null);
        limitVisibilityUpdate(cVisibility, visibilityGroup, object, objectNewGroup);
    }

    // use for update (considers both current group.id (from object) and group.id in the map)
    public static void limitVisibilityUpdate(EntityVisibility cVisibility, Group currentUserVisibilityGroup, Object object, Group objectNewGroup) {
        Group objectOldGroup = cVisibility.getGroup(object);
        GroupService groupService = GroupService.getInstance();
        Subject subject = SecurityUtils.getSubject();
        if (objectNewGroup != null) {
            //You can update objects in you same group (!up, !down)
            if (currentUserVisibilityGroup.getId().equals(objectOldGroup.getId()) && currentUserVisibilityGroup.getId().equals(objectNewGroup.getId())) {
                return;
            }
            //boolean downVisible = groupService.isGroupInsideTree(objectOldGroup, currentUserVisibilityGroup) && groupService.isGroupInsideTree(objectNewGroup, currentUserVisibilityGroup);
            if (cVisibility.isDownVisibility()) {
                return;
            }
            boolean upVisible = groupService.isGroupInsideTree(currentUserVisibilityGroup, objectOldGroup) && groupService.isGroupInsideTree(currentUserVisibilityGroup, objectNewGroup);
            if (UpVisibility.TRUE_R_U_SPECIAL.equals(cVisibility.getUpVisibility()) && upVisible) {
                //Test special up visibility for sharedToSelf
                if (cVisibility.isSharedToSelf() && PermissionsUtils.isPermitted(subject, cVisibility.getEditOwnPermission())) {
                    User createdByUser = cVisibility.getCreatedByUser(object);
                    if (createdByUser != null) {
                        User currentUser = (User) subject.getPrincipal();
                        if (currentUser.getId().equals(createdByUser.getId())) {
                            return;
                        }
                    }
                }
                if (PermissionsUtils.isPermitted(subject, cVisibility.getUpdateOneObjectPermission(cVisibility.getId(object)))) {
                    return;
                }
            }
        } else {
            //You can update objects in you same group (!up, !down)
            if (currentUserVisibilityGroup.getId().equals(objectOldGroup.getId())) {
                return;
            }
            boolean downVisible = groupService.isGroupInsideTree(objectOldGroup, currentUserVisibilityGroup);
            if (cVisibility.isDownVisibility() && downVisible) {
                return;
            }
            boolean upVisible = groupService.isGroupInsideTree(currentUserVisibilityGroup, objectOldGroup);
            if (UpVisibility.TRUE_R_U_SPECIAL.equals(cVisibility.getUpVisibility()) && upVisible) {
                //Test special up visibility for sharedToSelf
                if (cVisibility.isSharedToSelf() && PermissionsUtils.isPermitted(subject, cVisibility.getEditOwnPermission())) {
                    User createdByUser = cVisibility.getCreatedByUser(object);
                    if (createdByUser != null) {
                        User currentUser = (User) subject.getPrincipal();
                        if (currentUser.getId().equals(createdByUser.getId())) {
                            return;
                        }
                    }
                }
                if (PermissionsUtils.isPermitted(subject, cVisibility.getUpdateOneObjectPermission(cVisibility.getId(object)))) {
                    return;
                }
            }
        }
        throw new ForbiddenException("Not visible");
    }

}
