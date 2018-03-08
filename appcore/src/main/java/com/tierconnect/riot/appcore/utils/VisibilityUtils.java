package com.tierconnect.riot.appcore.utils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tierconnect.riot.appcore.controllers.RiotShiroRealm;
import com.tierconnect.riot.appcore.entities.GroupType;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.types.Predicate;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QGroup;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;
import org.apache.shiro.subject.Subject;

/**
 * 
 * Limit visibility based on relationship between the user's group and the subject object's groups in the group hierarchy
 * 
 * @author tcrown
 *
 */
public class VisibilityUtils 
{
	static Logger logger = Logger.getLogger( VisibilityUtils.class );


    // use for list
	public static Predicate limitVisibilityPredicate(Group visibilityGroup, QGroup base,boolean up,boolean down)
	{
        BooleanBuilder be = new BooleanBuilder();
        GroupService groupService = GroupService.getInstance();
        if(up)
		{
            be.or(base.id.eq(visibilityGroup.getId()));
			be.or(groupService.getAscendantsExcludingPredicate(base, visibilityGroup));
		}
		if(down)
		{
			be.or(groupService.getDescendantsIncludingPredicate(base, visibilityGroup));
		}
        if (!up && !down)
        {
            be.or(base.id.eq(visibilityGroup.getId()));
        }

		return be;
	}


    public static Group getVisibilityGroup(String clazz, Long visibilityGroupId) {
	    return getVisibilityGroup(clazz, visibilityGroupId, null);
    }

    public static Group getVisibilityGroup(String clazz, Long visibilityGroupId, Subject subject) {
        return getVisibilityGroup(clazz, visibilityGroupId, subject, true);
    }

    public static Group getVisibilityGroup(String clazz, Long visibilityGroupId, Subject subject, boolean initShiro) {
        subject = getSubjectIfIsNull(subject);
        //TODO please don't delete it calls getAuthorizationInfo from Shiro
        if(initShiro)
            subject.isPermitted("initShiro");
        Map<String, Group> overrideVisibilityMap = RiotShiroRealm.getOverrideVisibilityCache();
        Map<String, Group> visibilityMap = RiotShiroRealm.getVisibilityCache();
        Group visibilityGroup = overrideVisibilityMap.get(clazz);
        if (visibilityGroup == null) {
            visibilityGroup = visibilityMap.get(clazz);
        }
        if (visibilityGroup == null) {
            User currentUser = (User) subject.getPrincipal();
            if (currentUser == null) {
                Exception ex = new Exception();
                logger.error("VisibilityCache():"+ visibilityMap);
                logger.error("subject(:"+ subject);
                logger.error("authenticated:" + subject.isAuthenticated());
                logger.error("AGG NULL USER:", ex);
            }
            //logger.error("AGG not expected, getVisibilityCache should contain clazz: "+clazz);
            visibilityGroup = currentUser.getActiveGroup();
        }
        GroupService groupService = GroupService.getInstance();
        if (visibilityGroupId != null) {
            Group visibilityGroupForced = groupService.get(visibilityGroupId);
            if (!groupService.isGroupInsideTree(visibilityGroupForced, visibilityGroup)) {
//                throw new ForbiddenException( "Invalid visibility" );
            } else {
                visibilityGroup = visibilityGroupForced;
            }
        }
        return visibilityGroup;
    }

    public static Group getObjectGroup(Map<String, Object> objectMap)
	{
		Group objectGroup = null;
		try
		{
			 objectGroup = GroupService.getInstance().get( ( (Number) ((Map<?, ?>)objectMap.get( "group" )).get( "id" )).longValue() );
		}
		catch( NullPointerException npe )
		{
			try
			{
				 objectGroup = GroupService.getInstance().get( ( (Number) objectMap.get( "group.id" ) ).longValue() );
				 logger.warn( "*******WARNING: no \"'group':{'id':xxx}\" found, using \"group.id\" instead !" );
			}
			catch( NullPointerException npe2 )
			{
				logger.error( "*******ERROR: no \"'group':{'id':xxx}\" or  \"group.id\" found, IGNORING VISIBILITY CHECKS" );
				return null;
			}
		}
		return objectGroup;
	}

	public static Subject getSubjectIfIsNull(Subject subject){
	    return (subject == null) ? SecurityUtils.getSubject(): subject;
    }


}
