package com.tierconnect.riot.appcore.services;

/**
 * Created by oscar on 18-04-14.
 */
import java.util.List;

import com.mysema.query.types.expr.BooleanExpression;
import com.tierconnect.riot.appcore.entities.QResource;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.appcore.entities.Group;

public class ResourceService extends ResourceServiceBase {

	// only used in popdb. Should we ever hava a "selectAll" ? Seems dangerous ?
    public static List<Resource> list() {
        List<Resource> resources = getResourceDAO().selectAll();
        return resources;
    }

    public Resource get(Long resourceId){
        Resource resource = getResourceDAO().selectById(resourceId);
        //TODO: should this be common to all get() methods ? i.e., push to appgen ?
        if(resource == null) {
            throw new UserException(String.format("Resource id [%d] not found", resourceId));
        }
        return resource;
    }

    public void validateInsert( Resource resource )
    {
    	// should this be common to all inserts ? i.e., push to appgen ?
    	if(resource == null) {
            throw new UserException("Invalid Resource object");
        }
    }

    public static boolean existResourceByName(String name, Long excludeId) {
        BooleanExpression predicate = QResource.resource.name.eq(name);
        if(excludeId != null) {
            predicate = predicate.and(QResource.resource.id.ne(excludeId));
        }
        return getResourceDAO().getQuery().where(predicate).exists();
    }

    /*
	* This method gets a resource based on the name of the resource
	* */
    public Resource getByName(String name) throws NonUniqueResultException {
        try {
            return getResourceDAO().selectBy("name", name);
        }
        catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }

    /*
	* This method gets a resource based on the name and the group of the resource
	* */
    public Resource getByNameAndGroup(String name, Group group) throws NonUniqueResultException {
        Resource resource = null;
        try {
            resource =  getResourceDAO().getQuery().where(
                    QResource.resource.name.eq(name).and(QResource.resource.group.eq(group))).uniqueResult(QResource.resource);
            if (resource == null) {
                resource = getByName(name);
            }
        }
        catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
        return resource;
    }
}
