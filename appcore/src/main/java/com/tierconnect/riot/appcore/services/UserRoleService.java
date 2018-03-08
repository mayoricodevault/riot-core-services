package com.tierconnect.riot.appcore.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.JPASubQuery;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.mysema.query.types.ConstructorExpression;
import com.mysema.query.types.Predicate;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.UserException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.util.List;
import java.util.Set;

public class UserRoleService extends UserRoleServiceBase {
    public static UserRole getByUserAndRole(User user, Role role) {
        Predicate predicate = QUserRole.userRole.user.id.eq(user.getId()).and(QUserRole.userRole.role.id.eq(role.getId()));
        return getUserRoleDAO().selectBy(predicate);
    }

    public static List<UserRole> listByUser(User user) {
        Criteria criteria = UserRoleService.getUserRoleDAO().getSession().createCriteria(UserRole.class);
        criteria.add(Restrictions.eq("user", user));
        criteria.setCacheable(true);
        return criteria.list();
    }

    /**
     * This nethod checks if the user has a rol asigned to him
     *
     * @param user
     * @param role
     * @return
     */
    public boolean isUserWithRoles(User user, Role role) {
        boolean response = false;
        List<UserRole> lstUserRole = null;
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QUserRole.userRole.user.eq(user));
        be = be.and(QUserRole.userRole.role.eq(role));
        lstUserRole = UserRoleService.getInstance().getUserRoleDAO().selectAllBy(be);
        if (lstUserRole != null && lstUserRole.size() > 0) {
            response = true;
        }
        return response;
    }

    /**
     * @param role
     * @return
     */
    public static List<UserRole> listUsersByRole(Role role) {
        Predicate predicate = QUserRole.userRole.role.id.eq(role.getId());
        return getUserRoleDAO().selectAllBy(predicate);
    }

    public void validateInsert( UserRole userRole )
    {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QUserRole.userRole.user.eq(userRole.getUser()));
        be = be.and(QUserRole.userRole.role.eq(userRole.getRole()));
        if (!getUserRoleDAO().selectAllBy(be).isEmpty()){
            throw  new UserException("This User Role association already exists.");
        }
    }

    public void validateUpdate( UserRole userRole )
    {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QUserRole.userRole.user.eq(userRole.getUser()));
        be = be.and(QUserRole.userRole.role.eq(userRole.getRole()));
        if (!getUserRoleDAO().selectAllBy(be).isEmpty()){
            throw  new UserException("This User Role association already exists.");
        }

    }

    /**
     * @param user
     * @return list of user roles and permissions for authorization process
     */
    public List<UserRoleResource> listUserRoleAndResources(User user) {
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        List<UserRoleResource> rolesAndResources = query.from(QRoleResource.roleResource)
                .distinct()
                .innerJoin(QRoleResource.roleResource.resource, QResource.resource)
                .innerJoin(QRoleResource.roleResource.role, QRole.role)
                .leftJoin(QRole.role.groupTypeCeiling, QGroupType.groupType)
                .where(QRoleResource.roleResource.role.id.in(
                        new JPASubQuery().from(QUserRole.userRole)
                                .where(QUserRole.userRole.user.id.eq(user.getId())).list(QUserRole.userRole.role.id)
                ))
                .setCacheable(true)
                .list(ConstructorExpression.create(UserRoleResource.class,
                        QRole.role.id, QRole.role.name, QRole.role.groupTypeCeiling.id,
                        QResource.resource.id, QResource.resource.name, QResource.resource.fqname,
                        QResource.resource.type, QResource.resource.typeId, QResource.resource.acceptedAttributes,
                        QRoleResource.roleResource.permissions));
        return rolesAndResources;
    }
}
