package com.tierconnect.riot.iot.services;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.mysema.query.jpa.hibernate.HibernateSubQuery;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.BooleanExpression;
import com.mysema.query.types.query.ListSubQuery;
import com.tierconnect.riot.appcore.cache.CacheBoundary;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.GeneralVisibilityUtils;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.controllers.ThingTypeController;
import com.tierconnect.riot.iot.dao.ThingTypeDAO;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.dao.util.FormulaUtil;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.utils.DirectedGraph;
import com.tierconnect.riot.sdk.dao.*;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import com.tierconnect.riot.sdk.utils.RestUtils;
import com.tierconnect.riot.iot.utils.MatrixUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.hibernate.Hibernate;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jose4j.json.internal.json_simple.JSONObject;
import org.jose4j.json.internal.json_simple.parser.JSONParser;
import org.jose4j.json.internal.json_simple.parser.ParseException;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author garivera
 */
public class ThingTypeService extends ThingTypeServiceBase {

    private List<String> reservedWords;

    private static Logger logger = Logger.getLogger(ThingTypeService.class);

    private static final String THING_TYPE_CACHE_NAME = "thingTypeCache";
    private static final String THING_TYPE_DATA_TYPE_CACHE_NAME = "thingTypeDataTypeCache";

    @Override
    public void validateInsert(ThingType thingType) {
        if (StringUtils.isEmpty(thingType.getName())) {
            throw new UserException("Name cannot be null or empty.");
        }
        /*
        if( validateThingTypeName(thingType))
        {
            throw new UserException( String.format( "Name [%s] already exists.", thingType.getName() ) );
        }
        */
        validateThingTypeCode(thingType);
        super.validateInsert(thingType);
    }

    @Override
    public void validateUpdate(ThingType thingType) {
        if (StringUtils.isEmpty(thingType.getName())) {
            throw new UserException("Name cannot be null or empty.");
        }
        /*
        if( validateThingTypeName(thingType))
        {
            throw new UserException( String.format( "Name [%s] already exists.", thingType.getName() ) );
        }
        */
        validateThingTypeCode(thingType);
        super.validateUpdate(thingType);
    }

    @Override
    public ThingType insert(ThingType thingType) {
        ThingType res = super.insert(thingType);
        Resource re = new Resource();
        re.setAcceptedAttributes("riuda");
        re.setGroup(thingType.getGroup());
        Resource thingTypes = ResourceService.getInstance().getResourceDAO().selectBy(QResource.resource.name.eq(Resource.THING_TYPES_MODULE));
        re.setParent(thingTypes);
        re.setLabel(thingType.getName());
        re.setDescription((thingType.getGroup().getCode() != null ? thingType.getGroup().getCode() + ": " : "") + thingType.getName());
        re.setType(ResourceType.THING_TYPE_CLASS.getId());
        re.setTreeLevel(thingTypes.getTreeLevel() + 1);
        re.setName(Resource.THING_TYPE_PREFIX + thingType.getId());
        re.setFqname(Resource.THING_TYPE_PREFIX + thingType.getId());
        re.setTypeId(thingType.getId());
        ResourceService.getInstance().insert(re);

        Role rootRole = RoleService.getInstance().getRootRole();
        Set<RoleResource> lstRoleResource = rootRole.getRoleResources();
        lstRoleResource.add(RoleResourceService.getInstance().insert(rootRole, re, re.getAcceptedAttributes()));
        rootRole.setRoleResources(lstRoleResource);

        Subject subject = SecurityUtils.getSubject();
        User currentUser = (User) subject.getPrincipal();
        Set<Role> newRoleResource = new HashSet<>();
        if (!PermissionsUtils.isPermittedAny(subject, Resource.THING_TYPE_PREFIX + thingType.getId())) {
            for (UserRole userRole : currentUser.getUserRoles()) {
                for (RoleResource roleResource : userRole.getRole().getRoleResources()) {
                    if (roleResource.getResource().getFqname().equals(ThingType.class.getCanonicalName()) &&
                            roleResource.getResource().getAcceptedAttributeList().contains("i")) {
                        newRoleResource.add(userRole.getRole());
                    }
                }
            }
        }
        for (Role role : newRoleResource) {
            RoleResourceService.getInstance().insert(role, re, re.getAcceptedAttributes());
        }
        return res;
    }

    @Override
    public ThingType update(ThingType thingType) {
        Resource re = ResourceService.getInstance().getResourceDAO().selectBy(QResource.resource.typeId.eq(thingType.getId()).and(
                QResource.resource.type.eq(ResourceType.THING_TYPE_CLASS.getId())));
        re.setLabel(thingType.getName());
        re.setDescription((thingType.getGroup().getCode() != null ? thingType.getGroup().getCode() + ": " : "") + thingType.getName());
        ResourceService.getInstance().update(re);
        return super.update(thingType);
    }

    public void associate(ThingType newThingType,
                          List<ThingTypeField> thingTypeFieldUDFList) {
        associate(newThingType,
                thingTypeFieldUDFList, null);
    }

    public void associate(ThingType newThingType,
                          List<ThingTypeField> thingTypeFieldUDFList,
                          List<ThingTypeField> thingTypeFieldToDelete) {
        associate(newThingType,
                thingTypeFieldUDFList, thingTypeFieldToDelete, null, null);
    }

    public void associate(ThingType newThingType,
                          List<ThingTypeField> thingTypeFieldUDFList,
                          List<ThingTypeField> thingTypeFieldToDelete,
                          List<ThingType> parentThingTypesToDelete,
                          List<ThingType> childrenThingTypesToDelete) {
        if (thingTypeFieldToDelete != null && !thingTypeFieldToDelete.isEmpty()) {
            deleteUDFFieldsAssociation(newThingType, thingTypeFieldToDelete);
        }
        if (thingTypeFieldUDFList != null && !thingTypeFieldUDFList.isEmpty()) {
            addUDFFields(newThingType, thingTypeFieldUDFList);
        }
        if (parentThingTypesToDelete != null && !parentThingTypesToDelete.isEmpty()) {
            deleteThingTypeFieldsAssociation(newThingType, parentThingTypesToDelete);
        }
        if (childrenThingTypesToDelete != null && !childrenThingTypesToDelete.isEmpty()) {
            deleteThingTypeFieldsAssociation(newThingType, childrenThingTypesToDelete);
        }
        Set<ThingTypeMap> parentTypeMaps = newThingType.getParentTypeMaps();
        if (parentTypeMaps != null && !parentTypeMaps.isEmpty()) {
            associateThingWithParent(newThingType, parentTypeMaps);
        }
        Set<ThingTypeMap> childrenTypeMaps = newThingType.getChildrenTypeMaps();
        if (childrenTypeMaps != null && !childrenTypeMaps.isEmpty()) {
            associateThingWithChildren(newThingType, childrenTypeMaps);
        }
    }

    public void disassociateAll(ThingType newThingType) {
        List<ThingTypePath> deleteRelation = ThingTypePathService.getThingTypePathDAO().selectAllBy("originThingType.id",
                newThingType.getId());
        deleteRelation.addAll(
                ThingTypePathService.getThingTypePathDAO().
                        selectAllBy("destinyThingType.id", newThingType.getId())
        );
        for (ThingTypePath ttp : deleteRelation) {
            ThingTypePathService.getInstance().delete(ttp);
            ThingType origin = ttp.getOriginThingType();
            ThingType destiny = ttp.getDestinyThingType();
            //in case of shipping order asset tag
            Map<String, Object> parentOfParent = hasParentFieldThingType(origin);
            deleteUDFParentAssociation(parentOfParent, destiny);
            parentOfParent = hasParentFieldThingType(destiny);
            deleteUDFParentAssociation(parentOfParent, origin);
        }
    }

    private void associateThingWithParent(ThingType newThingType,
                                          Set<ThingTypeMap> parentTypeMaps) {
        for (ThingTypeMap ttm : parentTypeMaps) {
            ThingType parent = ttm.getParent();
            createLink(newThingType,
                    parent,
                    "parent",
                    "children"
            );
            //check if the parent has thing type as parent
            Map<String, Object> parentOfParent = hasParentFieldThingType(parent);
            if (!parentOfParent.isEmpty()) {
                String fieldParentName = (String) parentOfParent.get("fieldParentName");
                ThingType parentObject = (ThingType) parentOfParent.get("parentObject");
                //associate children with the parent's children
                createLink(newThingType,
                        parentObject,
                        "parent." + fieldParentName + ".value",
                        parent.getThingTypeCode() + "_children.children"
                );
            }
        }
    }

    private void associateThingWithChildren(ThingType newThingType, Set<ThingTypeMap> childrenTypeMaps) {
        //check if the new thing has thing type as parent
        Map<String, Object> parentOfThing = hasParentFieldThingType(newThingType);
        String fieldParentName = null;
        ThingType parentObject = null;
        if (!parentOfThing.isEmpty()) {
            fieldParentName = (String) parentOfThing.get("fieldParentName");
            parentObject = (ThingType) parentOfThing.get("parentObject");
        }
        for (ThingTypeMap ttm : childrenTypeMaps) {
            ThingType child = ttm.getChild();
            createLink(newThingType,
                    child,
                    "children",
                    "parent"
            );
            //check if the new thing has thing type as parent
            if (fieldParentName != null) {
                //associate children with the parent's children
                createLink(child,
                        parentObject,
                        "parent." + fieldParentName + ".value",
                        newThingType.getThingTypeCode() + "_children.children"
                );
            }
        }
    }

    private Map<String, Object> hasParentFieldThingType(ThingType parent) {
        Map<String, Object> response = new HashMap<>();
        List<ThingTypeField> parentThingTypeFields =
                parent.getThingTypeFieldsByType(ThingTypeField.Type.TYPE_THING_TYPE.value);
        for (ThingTypeField ttf : parentThingTypeFields) {
            Long parentID = ttf.getDataTypeThingTypeId();
            if (parentID != null) {
                ThingType thingTypeParent = ThingTypeService.getInstance().get(parentID);
                if (thingTypeParent.isIsParent()) {
                    response.put("fieldParentName", ttf.getName());
                    response.put("parentObject", thingTypeParent);
                    break;
                }
            }
        }
        return response;
    }

    private void addUDFFields(ThingType newThingType,
                              List<ThingTypeField> thingTypeFieldUDFList) {
        for (ThingTypeField ttf : thingTypeFieldUDFList) {
            ThingType destiny = ThingTypeService.getInstance().get(ttf.getDataTypeThingTypeId());
            if (destiny.isIsParent()) {
                createLink(newThingType,
                        destiny,
                        ttf.getName() + ".value",
                        newThingType.getThingTypeCode() + "_children"
                );
            } else {
                createLink(newThingType,
                        destiny,
                        ttf.getName() + ".value",
                        null
                );
                //check special case
                specialCaseAssociation(newThingType, destiny, ttf);
            }
        }
    }

    /**
     * Associates a UDF that is inside of an structure SO>ASSET>thingDestiny or SO>ASSET>TAG>thingDestiny
     *
     * @param newThingType
     * @param destiny
     * @param ttf
     */
    private void specialCaseAssociation(ThingType newThingType, ThingType destiny, ThingTypeField ttf) {
        Map<String, Object> response = hasParentFieldThingType(newThingType);
        if (!response.isEmpty()) {
            ThingType parentObject = (ThingType) response.get("parentObject");
            //associate children with the parent's children
            uniDirectionalAssociation(parentObject,
                    destiny,
                    newThingType.getThingTypeCode() + "_children." + ttf.getName() + ".value");
        } else {
            response = parentChildRelationHasParentUDFField(newThingType);
            if (!response.isEmpty()) {
                ThingType parentObject = (ThingType) response.get("parentObject");
                ThingType intermediateParent = (ThingType) response.get("intermediateParent");
                uniDirectionalAssociation(parentObject, destiny,
                        intermediateParent.getThingTypeCode() + "_children.children." + ttf.getName() + ".value");
            }
        }
    }

    private void uniDirectionalAssociation(ThingType parent, ThingType destiny, String path) {
        createLink(parent,
                destiny,
                path,
                null
        );
    }

    private Map<String, Object> parentChildRelationHasParentUDFField(ThingType parent) {
        Map<String, Object> response = new HashMap<>();
        List<ThingTypeMap> parentList = ThingTypeMapService.getInstance().
                getThingTypeMapByChildId(parent.getId());
        for (ThingTypeMap ttm : parentList) {
            response = hasParentFieldThingType(ttm.getParent());
            if (!response.isEmpty()) {
                response.put("intermediateParent", ttm.getParent());
                break;
            }
        }
        return response;
    }

    private void deleteUDFFieldsAssociation(ThingType newThingType, List<ThingTypeField> thingTypeFieldToDelete) {
        for (ThingTypeField ttf : thingTypeFieldToDelete) {
            ThingType destiny = ThingTypeService.getInstance().get(ttf.getDataTypeThingTypeId());
            deleteLink(newThingType, destiny);
        }
    }

    private void deleteThingTypeFieldsAssociation(ThingType newThingType, List<ThingType> thingTypesToDelete) {
        for (ThingType tt : thingTypesToDelete) {
            deleteLink(newThingType, tt);
            //in case of Shipping Order Asset Tag
            Map<String, Object> parentOfParent = hasParentFieldThingType(newThingType);
            deleteUDFParentAssociation(parentOfParent, tt);
            parentOfParent = hasParentFieldThingType(tt);
            deleteUDFParentAssociation(parentOfParent, newThingType);
        }
    }

    private void deleteUDFParentAssociation(Map<String, Object> parentOfParent, ThingType child) {
        if (!parentOfParent.isEmpty()) {
            ThingType parentObject = (ThingType) parentOfParent.get("parentObject");
            deleteLink(parentObject, child);
        }
    }

    private Map<String, Object> existThingTypePath(ThingType origin, ThingType destiny) {
        Map<String, Object> response = new HashMap<>();
        response.put("exists", Boolean.FALSE);
        Map<String, Object> params = new HashMap<>();
        params.put("originThingType.id", origin.getId());
        params.put("destinyThingType.id", destiny.getId());
        ThingTypePath thingTypePath = ThingTypePathService.getThingTypePathDAO().selectBy(params);
        if (thingTypePath != null) {
            response.put("thingTypePath", thingTypePath);
            response.put("exists", Boolean.TRUE);
        }
        return response;
    }

    private void createLink(ThingType origin, ThingType destiny,
                            String originPath,
                            String destinyPath) {
        //from child to parent
        Map<String, Object> existResponse = existThingTypePath(origin, destiny);
        Boolean exists = (Boolean) existResponse.get("exists");
        if (!exists) {
            insertThingTypePathLink(origin, destiny, originPath);
        } else {
            updateThingTypePathLink((ThingTypePath) existResponse.get("thingTypePath"), originPath);
        }
        //from parent to child
        existResponse = existThingTypePath(destiny, origin);
        exists = (Boolean) existResponse.get("exists");
        if (!exists) {
            if (destinyPath != null) {
                insertThingTypePathLink(destiny, origin, destinyPath);
            }
        } else {
            updateThingTypePathLink((ThingTypePath) existResponse.get("thingTypePath"), destinyPath);
        }
    }

    private void deleteLink(ThingType origin, ThingType destiny) {
        Map<String, Object> existResponse = existThingTypePath(origin, destiny);
        Boolean exists = (Boolean) existResponse.get("exists");
        if (exists) {
            deleteThingTypePathLink((ThingTypePath) existResponse.get("thingTypePath"));
        }
        existResponse = existThingTypePath(destiny, origin);
        exists = (Boolean) existResponse.get("exists");
        if (exists) {
            deleteThingTypePathLink((ThingTypePath) existResponse.get("thingTypePath"));
        }
    }

    private void insertThingTypePathLink(ThingType origin, ThingType destiny, String path) {
        ThingTypePath ttp = new ThingTypePath();
        ttp.setOriginThingType(origin);
        ttp.setDestinyThingType(destiny);
        ttp.setPath(path);
        ThingTypePathService.getInstance().insert(ttp);
    }

    private void updateThingTypePathLink(ThingTypePath ttp, String path) {
        ttp.setPath(path);
        ThingTypePathService.getInstance().update(ttp);
    }

    private void deleteThingTypePathLink(ThingTypePath ttp) {
        ThingTypePathService.getInstance().delete(ttp);
    }

    /*
    public static boolean validateThingTypeName(ThingType thingType) {
        BooleanBuilder b = new BooleanBuilder();
        b=b.or(getSiblingsExcludingPredicateInSameCompany(thingType));
        //b=b.or(getAncendantsExcludingPredicate(groupType));
        //b=b.or(getDescendantsIncludingPredicate(groupType));
        Predicate p = QThingType.thingType.name.eq(thingType.getName()).and(b.getValue());
        List<ThingType> otherThingTypes = getThingTypeDAO().selectAllBy(p);
        if (thingType.getId() == null && otherThingTypes.size() > 0) {
            return true;
        } else if (!(thingType.getId() == null) && otherThingTypes.size() > 0 && !thingType.getId().equals(otherThingTypes.get(0).getId())) {
            return true;
        }
        return false;
    }*/

    private static Predicate getSiblingsExcludingPredicateInSameCompany(ThingType thingType) {
        List<ThingType> parents = thingType.getParents();
        BooleanBuilder b = new BooleanBuilder();
        QThingType qThingType = QThingType.thingType;
        if (parents.isEmpty()) {
            b = b.and(qThingType.parentTypeMaps.isEmpty());
        } else {
            BooleanBuilder b2 = new BooleanBuilder();
            for (ThingType parent : parents) {
                b2 = b2.or(qThingType.parentTypeMaps.any().parent.eq(parent));
            }
            b = b.and(b2);
        }
        if (thingType.getId() != null) {
            b = b.and(qThingType.ne(thingType));
        }
        b = b.and(qThingType.group.eq(thingType.getGroup()));
        return b;
    }


    /**
     * @deprecated use ThingService.insert TODO !!!!
     */
    public static Thing instantiate(ThingType thingType, String serial, Date now, User createdUser) {
        return instantiate(thingType, serial, thingType.getGroup(), now, createdUser);
    }

    /**
     * @deprecated use ThingService.insert
     */
    @Deprecated
    public static Thing instantiate(ThingType thingType, String serial, Group group, Date now, User createdUser) {
        Thing thing = new Thing();
        thing.setCreatedByUser(createdUser);
        thing.setThingType(thingType);
        thing.setSerial(serial);
        thing.setName(serial);
        thing.setActivated(false);
        thing.setGroup(group);

        List<ThingTypeField> fields = new LinkedList<>();
        if (thingType.getThingTypeFields() != null) {
            for (ThingTypeField thingTypeField : thingType.getThingTypeFields()) {
                ThingTypeField thingField = new ThingTypeField();
                thingField.setName(thingTypeField.getName());
                thingField.setSymbol(thingTypeField.getSymbol());
                thingField.setUnit(thingTypeField.getUnit());
                thingField.setDataType(thingTypeField.getDataType());
                thingField.setTimeSeries(thingTypeField.getTimeSeries());
                //ThingFieldService.getInstance().insert(thingField);

                fields.add(thingField);
            }
        }

        ThingService.getInstance().insert(thing, now);
        return thing;
    }

//    public static long countList() {
//        return countList(null);
//    }
//
//    public static long countList(Predicate predicate) {
//        return getThingTypeDAO().countAll(predicate);
//    }
//
//    public static List<ThingType> selectByParent(ThingType thingType) {
//        return getThingTypeDAO().selectAllBy(QThingType.thingType.parent.eq(thingType));
//    }
//

    public void delete(ThingType thingType) {
        validateDelete(thingType);
        thingType.setArchived(true);
        getThingTypeDAO().update(thingType);
    }

    public void validateDelete(ThingType thingType) {
        if (thingType.isArchived()) {
            throw new UserException("Thing type already deleted (archived)");
        }
    }

    public boolean existsThingTypeCode(String code, Group group) {
        BooleanBuilder predicate = new BooleanBuilder(QThingType.thingType.thingTypeCode.eq(code));
        predicate.and(QThingType.thingType.group.eq(group));
        ThingTypeDAO thingTypeDAO = getThingTypeDAO();
        return thingTypeDAO.getQuery().where(predicate).exists();
    }

    public boolean existsThingTypeCode(String code, Group group, Long excludeId) {
        BooleanBuilder predicate = new BooleanBuilder(QThingType.thingType.thingTypeCode.eq(code));
        predicate = predicate.and(QThingType.thingType.id.ne(excludeId));
        ThingTypeDAO thingTypeDAO = getThingTypeDAO();
        return thingTypeDAO.getQuery().where(predicate).exists();
    }

    /**
     * Method that return a {@link ThingType} instance by code.
     *
     * @param code a {@link String} that contained the thing type code.
     * @return A instance of {@link ThingType} or null if the thing type not exists.
     * @throws NonUniqueResultException Throw exception if the thing type code return more of a result.
     */
    public ThingType getByCode(String code) throws NonUniqueResultException {
        try {
            return getThingTypeDAO().selectBy("thingTypeCode", code);
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }

    /**
     * Method that return a {@link ThingType} instance by code.
     *
     * @param id a {@link String} that contained the thing type code.
     * @return A instance of {@link ThingType} or null if the thing type not exists.
     * @throws NonUniqueResultException Throw exception if the thing type code return more of a result.
     */
    public ThingType getById(Long id) throws NonUniqueResultException {
        try {
            return getThingTypeDAO().selectBy("id", id);
        } catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }

    /**
     * Get Thing Type by code and group
     *
     * @param code
     * @param group
     * @return
     */
    public ThingType getByCodeAndGroup(String code, Group group) {
        ThingType result = null;
        BooleanExpression predicate = QThingType.thingType.thingTypeCode.eq(code);
        predicate = predicate.and(QThingType.thingType.group.eq(group));
        List<ThingType> lst = ThingTypeService.getInstance().listPaginated(predicate, null, null);
        if ((lst != null) && (!lst.isEmpty())) {
            result = lst.get(0);
        }
        return result;
    }

    private void validateThingTypeCode(ThingType thingType) {
        // System.out.println("validateThingTypeCode method");
        String thingTypeCode = thingType.getThingTypeCode();
        if (StringUtils.isEmpty(thingTypeCode)) {
            throw new UserException(String.format("Thing Type Code is required."));
        }

        boolean existsThingTypeCode;
        if (thingType.getId() == null) {
            // validating for insert case
            existsThingTypeCode = existsThingTypeCode(thingTypeCode, thingType.getGroup());
        } else {
            // validating for update case
            existsThingTypeCode = existsThingTypeCode(thingTypeCode, thingType.getGroup(), thingType.getId());
        }

        if (existsThingTypeCode) {
            // System.out.println("existsThingTypeCode true");
            throw new UserException(String.format("Thing Type Code '[%s]' already exists.", thingType.getThingTypeCode()));
        }
    }

    public List<ThingType> getAllThingTypes() {
        return getThingTypeDAO().selectAll();
    }

    /**
     * get Thing Types By Thing Type Template
     *
     * @param thingTypeTemplateId thing Type Template Id
     * @return List of thing Types
     */
    public List<ThingType> getThingTypesByThingTypeTemplate(Long thingTypeTemplateId) {
        HibernateQuery query = getThingTypeDAO().getQuery();
        return query.where(QThingType.thingType.thingTypeTemplate.id.eq(thingTypeTemplateId))
                .list(QThingType.thingType);
    }

    public Long countAllActive() {
        return getThingTypeDAO().countAll(QThingType.thingType.archived.isFalse());
    }

    public Long countAllActive(Group tenantGroup) {
        return getThingTypeDAO().countAll(QThingType.thingType.archived.isFalse().and(QThingType.thingType.group.parentLevel2.id.eq(tenantGroup.getParentLevel2().getId())));
    }

    public void updateFields(List<Map<String, Object>> thingTypeMap, ThingType thingType, boolean remove) {
        updateFields(thingTypeMap, thingType, remove, null, null);
    }

    /**
     * Update fields with the values from the map
     *
     * @param thingTypeMap new values to update
     */
    public void updateFields(List<Map<String, Object>> thingTypeMap, ThingType thingType, boolean remove,
                             List<ThingTypeField> thingTypeFieldUDFList, List<ThingTypeField> thingTypeFieldToDelete) {
        Set<ThingTypeField> thingTypeFields = thingType.getThingTypeFields();
        //first we remove
        if (remove) {
            for (Iterator<ThingTypeField> i = thingTypeFields.iterator(); i.hasNext(); ) {
                ThingTypeField element = i.next();
                boolean found = false;

                for (Map<String, Object> stringObjectMap : thingTypeMap) {
                    Long id = null;
                    if (stringObjectMap.get("id") instanceof Long)
                        id = (Long) stringObjectMap.get("id");
                    else if (stringObjectMap.get("id") instanceof Integer)
                        id = Long.valueOf((Integer) stringObjectMap.get("id"));
                    if (id != null && element.getId().equals(id.longValue()))
                        found = true;
                }
                if (!found) {
                    i.remove();
                    // remove sequence just for sequence type
                    ThingTypeFieldService.getInstance().removeSequence(element);

                    DBCollection thingsCollection = MongoDAOUtil.getInstance().things;
                    BasicDBObject queryDoc = new BasicDBObject("thingTypeCode", thingType.getThingTypeCode().toString());
                    BasicDBObject unsetDoc = new BasicDBObject("$unset", new BasicDBObject(element.getName(), 1));

                    //Validate if the thing type or thing type property is used in Bridges Rules
                    String reportRules = EdgeboxRuleService.getInstance().getRulesUsingPropertyAndThingType(element.getName(), thingType.getThingTypeCode());
                    if ((reportRules != null) && (!reportRules.isEmpty())) {
                        String message = String.format("Thing Type '%s' cannot be updated because its field '%s' has references in Bridges Rules: %s",
                                thingType.getName(), element.getName(), reportRules);
                        throw new UserException(message);
                    }
                    //Validate if the property is in Report Tables
                    String reportsUsingField = getNameReportsReferenced(element.getId());
                    if ((reportsUsingField != null) && (!reportsUsingField.isEmpty())) {
                        String message = String.format("Thing Type '%s' cannot be updated because its field '%s' has references in Reports: %s.",
                                thingType.getName(), element.getName(), reportsUsingField.toString());
                        throw new UserException(message);
                    }
                    thingsCollection.update(queryDoc, unsetDoc, false, true);
                    if (thingTypeFieldToDelete != null && element.isThingTypeUDF())
                        thingTypeFieldToDelete.add(element);
                }
            }
        }

        //now we update or insert.
        for (Map<String, Object> stringObjectMap : thingTypeMap) {
            ThingTypeField ttf = null;
            String data = null;
            if (stringObjectMap.get("defaultValue") != null) {
                if (!(stringObjectMap.get("defaultValue") instanceof ArrayList)) {
                    data = stringObjectMap.get("defaultValue").toString();
                } else {
                    data = "";
                    ArrayList<String> aa = (ArrayList) stringObjectMap.get("defaultValue");
                    for (String a : aa) {
                        data = data + a + ",";
                    }
                    if (data != null) {
                        data = data.substring(0, data.length() - 1);
                    }
                }
            }
            if (null == stringObjectMap.get("type"))
                throw new UserException("Type has to be specified for property " + stringObjectMap.get("name"));
            Long id = null;
            if (stringObjectMap.get("id") instanceof Long) {
                id = (Long) stringObjectMap.get("id");
            } else if (stringObjectMap.get("id") instanceof Integer) {
                id = Long.valueOf(stringObjectMap.get("id").toString());
            }

            if (id != null) {
                String oldFieldName = thingType.getThingTypeFieldById(id).getName();

                for (ThingTypeField thingTypeField : thingType.getThingTypeFields()) {
                    if (thingTypeField.getId().compareTo(id.longValue()) == 0) {
                        ttf = thingTypeField;
                        break;
                    }
                }

                if (ttf == null) {
                    logger.error("ThingType field with id " + id + " is null, it is not possible to update it.");
                    throw new UserException("Error trying to update a ThingType Property.");
                } else {
                    Long dataTypeId = Long.parseLong(stringObjectMap.get("type").toString());
                    Long newType = dataTypeId;
                    Long oldType = ttf.getDataType().getId();
                    // remove sequence if the old thingTypeField type is sequence and the
                    // new thingTypeField type isn't sequence
                    if ((oldType == ThingTypeField.Type.TYPE_SEQUENCE.value)
                            && (newType != ThingTypeField.Type.TYPE_SEQUENCE.value)) {
                        ThingTypeFieldService.getInstance().removeSequence(ttf);
                    }
                    //Validation change of name in thing type property
                    if (!ttf.getName().equals(stringObjectMap.get("name"))) {
                        //Validate if the property is in Report Tables
                        String reportsUsingField = getNameReportsReferenced(id);
                        if ((reportsUsingField != null) && (!reportsUsingField.isEmpty())) {
                            String message = String.format("Thing Type '%s' cannot be updated because its field '%s' has references in Reports: %s.",
                                    thingType.getName(), ttf.getName(), reportsUsingField.toString());
                            throw new UserException(message);
                        }
                        //Validate if the thing type or thing type property is used in Bridges Rules
                        String reportRules = EdgeboxRuleService.getInstance().getRulesUsingPropertyAndThingType(ttf.getName(), thingType.getThingTypeCode());
                        if ((reportRules != null) && (!reportRules.isEmpty())) {
                            String message = String.format("Thing Type '%s' cannot be updated because its field '%s' has references in Bridges Rules: %s",
                                    thingType.getName(), ttf.getName(), reportRules);
                            throw new UserException(message);
                        }
                    }

                    ttf.setName((String) stringObjectMap.get("name"));
                    ttf.setSymbol((String) stringObjectMap.get("symbol"));
                    ttf.setUnit((String) stringObjectMap.get("unit"));
                    ttf.setDataType(DataTypeService.getInstance().get(dataTypeId));
                    if (stringObjectMap.get("dataTypeThingTypeId") != null && !stringObjectMap.get("dataTypeThingTypeId").toString().equals("")) {
                        ttf.setDataTypeThingTypeId(Long.parseLong(stringObjectMap.get("dataTypeThingTypeId").toString()));
                    }
                    ttf.setTypeParent((String) stringObjectMap.get("typeParent"));
                    ttf.setMultiple((Boolean) stringObjectMap.get("multiple"));
                    ttf.setTimeSeries((Boolean) stringObjectMap.get("timeSeries"));
                    if (stringObjectMap.get("thingTypeFieldTemplateId") != null) {
                        ttf.setThingTypeFieldTemplateId(Long.parseLong(stringObjectMap.get("thingTypeFieldTemplateId").toString()));
                    }
                    if (newType != ThingTypeField.Type.TYPE_SEQUENCE.value) {
                        ttf.setDefaultValue(data);
                    }
                    // create sequence if the old thingTypeField type isn't sequence and the
                    // new thingTypeField type is sequence
                    if ((oldType != ThingTypeField.Type.TYPE_SEQUENCE.value)
                            && (newType == ThingTypeField.Type.TYPE_SEQUENCE.value)) {
                        ThingTypeFieldService.getInstance().createSequence(ttf);
                    }

                    ThingTypeFieldService.getInstance().update(ttf);
                }
                //VIZIX-1266, Control>Attachments, attachments are lost when try to rename UDF referred in report
                //We must rename fields after validation
                if (!stringObjectMap.get("name").equals(oldFieldName)) {
                    //rename in things collection
                    DBCollection thingsCollection = MongoDAOUtil.getInstance().things;
                    BasicDBObject queryDoc = new BasicDBObject("thingTypeCode", thingType.getThingTypeCode().toString());
                    BasicDBObject unsetDoc = new BasicDBObject("$rename", new BasicDBObject(oldFieldName, stringObjectMap.get("name")));
                    thingsCollection.update(queryDoc, unsetDoc, false, true);

                    //rename in thingSnapshots collection
                    DBCollection thingSnapshotsCollection = MongoDAOUtil.getInstance().thingSnapshots;
                    BasicDBObject querySnapshotDoc = new BasicDBObject("thingTypeCode", thingType.getThingTypeCode().toString());
                    BasicDBObject unsetSnapshotDoc = new BasicDBObject("$rename", new BasicDBObject("value." + oldFieldName, "value." + stringObjectMap.get("name")));
                    thingSnapshotsCollection.update(querySnapshotDoc, unsetSnapshotDoc, false, true);
                }
            } else {
                ttf = new ThingTypeField();
                ttf.setName((String) stringObjectMap.get("name"));
                ttf.setUnit((String) stringObjectMap.get("unit"));
                ttf.setSymbol((String) stringObjectMap.get("symbol"));
                Long dataTypeId = Long.parseLong(stringObjectMap.get("type").toString());
                ttf.setDataType(DataTypeService.getInstance().get(dataTypeId));
                if (stringObjectMap.get("dataTypeThingTypeId") != null && !stringObjectMap.get("dataTypeThingTypeId").toString().equals("")) {
                    ttf.setDataTypeThingTypeId(Long.parseLong(stringObjectMap.get("dataTypeThingTypeId").toString()));
                }
                ttf.setTypeParent((String) stringObjectMap.get("typeParent"));
                ttf.setMultiple((Boolean) stringObjectMap.get("multiple"));
                ttf.setTimeSeries((Boolean) stringObjectMap.get("timeSeries"));
                ttf.setDefaultValue(data);
                thingTypeFields.add(ttf);
                ttf.setThingType(thingType);
                ThingTypeFieldService.getInstance().insert(ttf);
            }
            validateThingTypeUDF(ttf);
            if (thingTypeFieldUDFList != null && ttf.isThingTypeUDF())
                thingTypeFieldUDFList.add(ttf);
        }
    }

    /**
     * Identify if a thing type field id is referencesd in reports
     *
     * @param thingTypeFieldId Id of the field to search
     * @return
     */
    public String getNameReportsReferenced(Long thingTypeFieldId) {
        Set<String> nameReports = new HashSet<>();
        List<ReportProperty> lstReportProperties = ReportPropertyService.getInstance().getPropertiesByThingTypeId(thingTypeFieldId);
        if ((lstReportProperties != null) && (!lstReportProperties.isEmpty())) {
            for (ReportProperty reportProperty : lstReportProperties) {
                nameReports.add(reportProperty.getReportDefinition().getName());
            }
        }

        List<ReportEntryOptionProperty> lstReportEntryOptionProperty =
                ReportEntryOptionPropertyService.getInstance().getReportEntryOptionPropertiesByThingTypeId(thingTypeFieldId);
        if ((lstReportEntryOptionProperty != null) && (!lstReportEntryOptionProperty.isEmpty())) {
            for (ReportEntryOptionProperty reportProperty : lstReportEntryOptionProperty) {
                nameReports.add(reportProperty.getReportEntryOption().getName());
            }
        }

        List<ReportFilter> lstReportFilters = ReportFilterService.getInstance().getFiltersByThingTypeId(thingTypeFieldId);
        if ((lstReportFilters != null) && (!lstReportFilters.isEmpty())) {
            for (ReportFilter reportFilter : lstReportFilters) {
                nameReports.add(reportFilter.getReportDefinition().getName());
            }
        }

        List<ReportRule> lstReportRules = ReportRuleService.getInstance().getReportRulesByThingTypeId(thingTypeFieldId);
        if ((lstReportRules != null) && (!lstReportRules.isEmpty())) {
            for (ReportRule reportRule : lstReportRules) {
                nameReports.add(reportRule.getReportDefinition().getName());
            }
        }
        List<ReportGroupBy> lstReportGroupBy = ReportGroupByService.getInstance().getReportGroupByThingTypeId(thingTypeFieldId);
        if ((lstReportGroupBy != null) && (!lstReportGroupBy.isEmpty())) {
            for (ReportGroupBy reportGroupBy : lstReportGroupBy) {
                nameReports.add(reportGroupBy.getReportDefinition().getName());
            }
        }
        return String.join(",", nameReports);
    }

    /**
     * This method get the list of thingTypes who use thingTypes as Udfs
     */
    public List<Map<String, Object>> getThingTypeUdf(Set<ThingTypeField> thingTypeFields) {
        return getThingTypeUdf(thingTypeFields, null);
    }

    /**
     * This method get the list of thingTypes who use thingTypes as Udfs
     */
    public List<Map<String, Object>> getThingTypeUdf(Set<ThingTypeField> thingTypeFields, Boolean parent) {
        List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        Set<Long> hsThingType = new HashSet<Long>();
        if (thingTypeFields != null && thingTypeFields.size() > 0) {
            for (ThingTypeField thingTypeField : thingTypeFields) {
                if (thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_THING_TYPE.value) == 0) {
                    hsThingType.add(thingTypeField.getDataTypeThingTypeId());
                }
            }
        }
        for (Long idThingType : hsThingType) {
            ThingType thingType = this.get(idThingType);
            if (parent == null) {
                response.add(thingType.publicMap(true, false));
            } else if (Boolean.compare(parent, thingType.isIsParent()) == 0) {
                response.add(thingType.publicMap(true, false));
            }
        }

        return response;
    }

    /*******************************************************************
     * Identify what ThingTypes have ThingType as UDF, all thingTypes with UDF ThingType
     * are going to be at the start of the list
     *******************************************************************/
    public List<Map<String, Object>> getThingTypesWithThingsUDFs(List<Map<String, Object>> list, String order) throws Exception {
        Map<String, ThingType> thingTypes = new HashMap<>();
        for (ThingType tt : ThingTypeService.getInstance().getAllThingTypes()) {
            thingTypes.put(tt.getCode(), tt);
        }
        List<Map<String, Object>> response = new ArrayList<Map<String, Object>>();
        List<Map<String, Object>> responseTemp = new ArrayList<Map<String, Object>>();
        for (Object dataThingType : list) {
            //Get the thingTyoe
            Map<String, Object> data = (Map<String, Object>) dataThingType;
//            ThingType thingType = ThingTypeService.getInstance().getByCode( data.get( "thingTypeCode" ).toString() );
            ThingType thingType = thingTypes.get(data.get("thingTypeCode").toString());
            List<Map<String, Object>> lstThingTypeUdf = ThingTypeService.getInstance().getThingTypeUdf(thingType.getThingTypeFields());
            // Check if the thingType has ThingType as udf's
            if ((lstThingTypeUdf != null && lstThingTypeUdf.size() > 0)) {
                response.add(data);
            } else {
                responseTemp.add(data);
            }
        }

//        TreeUtils.sortObjects(order, response);
//        TreeUtils.sortObjects(order, responseTemp);
        response.addAll(responseTemp);
        return response;
    }

    /********************************
     *get Difference Between Lists
     *******************************/
    public List<Map<String, Object>> getDifferenceBetweenLists(List<Map<String, Object>> ini, List<Map<String, Object>> last) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> iniOne : ini) {
            Map<String, Object> iniOneMap = (Map) iniOne;
            if (last != null && last.size() > 0) {
                int cont = 0;
                for (Map<String, Object> lastOne : last) {
                    Map<String, Object> lastOneMap = (Map) lastOne;
                    if (Long.parseLong(iniOneMap.get("id").toString()) == Long.parseLong(lastOneMap.get("id").toString())) {
                        break;
                    }
                    cont++;
                }
                if (cont == last.size()) {
                    result.add(iniOne);
                }
            } else {
                result.add(iniOne);
            }
        }
        return result;
    }

    /**********************************************
     * Validate Attachment configuration
     * **********************************************/
    public ValidationBean validateAttachmentConfig(String type, String name, String defaultValue) {
        ValidationBean response = new ValidationBean();
        if (type != null && Long.valueOf(type).compareTo(ThingTypeField.Type.TYPE_ATTACHMENTS.value) == 0) {
            if (ThingService.getInstance().isJSONValid(defaultValue)) {
                try {
                    JSONObject json = (JSONObject) new JSONParser().parse(defaultValue);
                    JSONObject configuration = (JSONObject) json.get("configuration");
                    String fileSystemPath = (String) configuration.get("fileSystemPath");
                    if (fileSystemPath == null || fileSystemPath.trim().isEmpty()) {
                        response.setErrorDescription("'fileSystemPath' is not configured in the UDFs: '" + name +
                                "'. Please, check 'fileSystemPath' configuration in 'Tenant Group' module.");
                    } else {
                        String blockedExtensions = configuration.get("blockedExtensions").toString();
                        String allowedExtensions = configuration.get("allowedExtensions").toString();
                        response = attachmentExtensionsValidation(allowedExtensions, blockedExtensions, name);
                    }
                } catch (Exception e) {
                    response.setErrorDescription(name + " (" + e.getMessage() + "),");
                }
            } else {
                response.setErrorDescription(name + " (JSON is not Valid),");
            }
        }
        return response;
    }

    //RIOT-13355: CONTROL > ATTACHMENTS settings > Wrong behaviors in attachments settings Allowed VS Blocked
    public ValidationBean isUploadedAttachmentValid(Long thingTypeFieldId, MultipartFormDataInput input) {
        ValidationBean response = new ValidationBean();
        ThingTypeField thingTypeField = ThingTypeFieldService.getInstance().get(thingTypeFieldId);
        try {
            JSONObject json = (JSONObject) new JSONParser().parse(thingTypeField.getDefaultValue());
            JSONObject configuration = (JSONObject) json.get("configuration");
            String blockedExtensions = formatExtensionString(configuration.get("blockedExtensions").toString());
            String allowedExtensions = formatExtensionString(configuration.get("allowedExtensions").toString());
            if (StringUtils.isEmpty(allowedExtensions)) {
                allowedExtensions = "*";
            }
            Map<String, List<InputPart>> formDataMap = input.getFormDataMap();
            breakWhenIsNotValid:
            for (String key : formDataMap.keySet()) {
                if (key.startsWith("file")) {
                    List<InputPart> partList = formDataMap.get(key);
                    for (InputPart part : partList) {
                        MultivaluedMap<String, String> headers = part.getHeaders();
                        String[] contDisp = headers
                                .getFirst("Content-Disposition")
                                .replaceAll(" +", "")
                                .split(";");
                        for (String s : contDisp) {
                            if (s.startsWith("filename")) {
                                s = s.replaceAll("\"", "");
                                String[] fileString = s.split("=");
                                int index = fileString[1].lastIndexOf('.');
                                String filename = fileString[1].substring(0, (index >= 0) ? index : fileString[1].length());
                                String Extension = (index >= 0) ? fileString[1].substring(index + 1) : "";
                                logger.info("[filename = " + filename + "] [Extension = " + Extension + "]");
                                if (blockedExtensions.contains(Extension)
                                        || blockedExtensions.contains("*")
                                        || !allowedExtensions.contains("*")) {
                                    if (!allowedExtensions.contains(Extension)) {
                                        if (StringUtils.isEmpty(Extension)) {
                                            response.setErrorDescription("Files without Extension are not allowed");
                                        } else {
                                            response.setErrorDescription("Files with Extension '" + Extension +
                                                    "' are not allowed");
                                        }
                                        break breakWhenIsNotValid;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (ParseException e) {
            response.setErrorDescription(" (" + e.getMessage() + "),");
        }
        return response;
    }

    //RIOT-13268: Handle the case when the "allowed Extensions" and "blocked Extensions" are empty.
    //RIOT-12740: Validate that allowedExtensions not contains any element of blockedExtensions
    //RIOT-13355: CONTROL > ATTACHMENTS settings > Wrong behaviors in attachments settings Allowed VS Blocked
    public ValidationBean attachmentExtensionsValidation(String allowed, String blocked, String name) {
        ValidationBean response = new ValidationBean();
        allowed = formatExtensionString(allowed);
        blocked = formatExtensionString(blocked);
        if (validateExtensionFormat(allowed) && validateExtensionFormat(blocked)) {
            if (!StringUtils.isEmpty(allowed) && !StringUtils.isEmpty(blocked)) {
                if (!blocked.contains("*")) {
                    List<String> blockedExtList = Arrays.asList(blocked.split(","));
                    //JDK 1.8
                    //boolean match = blockedExtensionsList.stream().anyMatch(s -> allowedExtensions.contains(s));
                    for (String s : blockedExtList) {
                        if (allowed.contains(s)) {
                            response.setErrorDescription("'" + name + "' 'Blocked Extensions' can't contain elements" +
                                    " of 'Allowed Extensions'");
                            break;
                        }
                    }
                } else {
                    response.setErrorDescription("'" + name + "' 'Blocked Extensions' can't contains '*'" +
                            " when 'Allowed Extensions' is not empty");
                }
            }
        } else {
            response.setErrorDescription("'" + name + "'. Invalid format in 'Allowed Extensions' or 'Blocked Extensions'");
        }
        return response;
    }

    private String formatExtensionString(String ExtensionList) {
        if (StringUtils.isEmpty(ExtensionList))
            return "";
        else
            return ExtensionList
                    .replaceAll(" +", "") //deleting spaces
                    .replaceAll("\\*.", "") //deleting *.Extension
                    .toLowerCase();
    }

    private boolean validateExtensionFormat(String str) {
        final String commaSeparatedValidator = "(([*]|\\w)+(([,]\\w+)|([,][*]+))*)*";
        Pattern p = Pattern.compile(commaSeparatedValidator);
        Matcher m = p.matcher(str);
        return m.matches();
    }

    /**********************************************************
     * Check if the UDF is child
     ***********************************************************/
    public boolean isChild(ThingType thingType) {
        boolean response = false;
        QThingTypeMap qThingTypeMap = QThingTypeMap.thingTypeMap;
        List<ThingType> isChild = ThingTypeMapService.getThingTypeMapDAO().getQuery().where(qThingTypeMap.child.eq(thingType))
                .list(qThingTypeMap.child);

        if (isChild != null && isChild.size() > 0) {
            response = true;
        }
        return response;
    }

    /**********************************************************
     * Check if the UDF is child
     ***********************************************************/
    public boolean isNativeParent(ThingType thingType) {
        boolean response = false;
        QThingTypeMap qThingTypeMap = QThingTypeMap.thingTypeMap;
        List<ThingType> isParent = ThingTypeMapService.getThingTypeMapDAO().getQuery().where(qThingTypeMap.parent.eq(thingType))
                .list(qThingTypeMap.parent);

        if (isParent != null && isParent.size() > 0) {
            response = true;
        }
        return response;
    }

    /**********************************************************
     * Check if the UDF is child UDF
     ***********************************************************/
    public boolean isWithThingTypeUdf(ThingType thingType) {
        boolean response = false;
        if (thingType.getThingTypeFields() != null && thingType.getThingTypeFields().size() > 0) {
            for (ThingTypeField thingTypeField : thingType.getThingTypeFields()) {
                if (thingTypeField.getDataTypeThingTypeId() != null) {
                    response = true;
                    break;
                }
            }
        }
        return response;
    }

    /**********************************************************
     * Children UDf Thing Type Code
     ***********************************************************/
    public List<ThingType> getChildrenUdfThingTypeCode(Long thingTypeParentId) {
        List<ThingType> results = new ArrayList<ThingType>();
        List<ThingType> resultData = ThingTypeService.getInstance().getAllThingTypes();
        if (resultData != null && resultData.size() > 0) {
            for (ThingType thingType : resultData) {
                if (thingType.getThingTypeFields() != null && thingType.getThingTypeFields().size() > 0) {
                    for (ThingTypeField thingTypeField : thingType.getThingTypeFields()) {
                        if (thingTypeField.getDataTypeThingTypeId() != null &&
                                thingTypeField.getDataTypeThingTypeId().compareTo(thingTypeParentId) == 0) {
                            results.add(thingType);
                        }
                    }
                }
            }
        }
        return results;
    }

    /***********************************************************
     * Get the thingTypeUDF Parent
     ************************************************************/
    public Map<String, Object> getParentThingTypeUdf(Thing thingChild) {
        Map<String, Object> result = new HashMap<>();
        BasicDBObject parentValue = null;
        String nameUdf = null;

        DBObject thing = ThingMongoDAO.getInstance().getThing(thingChild.getId());
        if (thingChild.getThingType() != null && thingChild.getThingType().getThingTypeFields() != null && thingChild.getThingType().getThingTypeFields().size() > 0) {
            //Iterate Udfs of the thing children
            for (ThingTypeField thingTypeField : thingChild.getThingType().getThingTypeFields()) {
                //Find ThingType UDF
                if (thingTypeField.getDataTypeThingTypeId() != null) {
                    //Check if the thingType UDF  'isParent : true'
                    ThingType thingTypeUdf = ThingTypeService.getInstance().get(thingTypeField.getDataTypeThingTypeId());
                    if (thingTypeUdf.isIsParent()) {
                        nameUdf = thingTypeField.getName();
                        break;
                    }

                }
            }
        }

        //Get the Value of thingTypeUdf
        if (nameUdf != null) {
            Object parent = thing.get(nameUdf);
            parentValue = parent != null ? (BasicDBObject) ((BasicDBObject) parent).get("value") : null;
            if (parentValue != null) {
                result.put("propertyName", nameUdf);
                result.put("object", parentValue);
            }
        }
        return result;
    }

    /**
     * Get VISUAL parent thing Type UDF
     *
     * @param thingChild, its thingType is not marked as 'isParent'
     * @return Map of the VISUAL parent thing type UDF
     */
    public List<Map<String, Object>> getVisualParentThingTypeUdf(Thing thingChild) {
        List<Map<String, Object>> result = new ArrayList<>();

        //Search in thingTypeFields if there are references of thingChild's thingType
        List<ThingType> lstThingTypes = ThingTypeService.getInstance().getAllThingTypes();
        if (lstThingTypes != null && lstThingTypes.size() > 0) {
            for (ThingType thingtype : lstThingTypes) {
                for (ThingTypeField thingTypeField : thingtype.getThingTypeFields()) {
                    if (thingTypeField.getDataTypeThingTypeId() != null
                            && thingTypeField.getDataTypeThingTypeId().compareTo(thingChild.getThingType().getId()) == 0) {
                        Map<String, Object> mapUdf = new HashMap<>();
                        mapUdf.put("propertyName", thingTypeField.getName());
                        mapUdf.put("thingTypeCode", thingtype.getCode());
                        result.add(mapUdf);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Get VISUAL parent thing Type UDF
     *
     * @param thingChild, its thingType is not marked as 'isParent'
     * @return Map of the VISUAL parent thing type UDF
     */
    public List<Map<String, Object>> getReferencesThingTypeUdf(ThingType thingChild) {
        List<Map<String, Object>> result = new ArrayList<>();

        //Search in thingTypeFields if there are references of thingChild's thingType
        List<ThingTypePath> lstThingTypePaths = ThingTypePathService.getInstance().getPathByThingTypeDestiny(thingChild);

        for (ThingTypePath ttPath : lstThingTypePaths) {
            ThingType ttOrigin = ttPath.getOriginThingType();

            if (ttOrigin.getThingTypeFields() == null) {
                ttOrigin.setThingTypeFields(new HashSet<ThingTypeField>());
                List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().getByThingTypeCode(ttOrigin.getThingTypeCode());
                for (ThingTypeField thingTypeField : thingTypeFields) {
                    ttOrigin.getThingTypeFields().add(thingTypeField);
                }
            }

            for (ThingTypeField thingTypeField : ttOrigin.getThingTypeFields()) {
                if (thingTypeField.getDataTypeThingTypeId() != null
                        && thingTypeField.getDataTypeThingTypeId().compareTo(thingChild.getId()) == 0) {
                    Map<String, Object> mapUdf = new HashMap<>();
                    mapUdf.put("propertyName", thingTypeField.getName());
                    mapUdf.put("thingTypeCode", ttOrigin.getCode());
                    mapUdf.put("thingTypeId", ttOrigin.getId());
                    mapUdf.put("object", ttOrigin);
                    result.add(mapUdf);
                    if (this.isNativeParent(ttOrigin)) {
                        List<ThingTypeMap> thingMap = ThingTypeMapService.getInstance().getThingTypeMapByParentId
                                (ttOrigin.getId());
                        for (ThingTypeMap child : thingMap) {
                            mapUdf = new HashMap<>();
                            mapUdf.put("propertyName", "parent." + thingTypeField.getName());
                            mapUdf.put("thingTypeId", child.getChild().getId());
                            mapUdf.put("object", child.getChild());
                            result.add(mapUdf);
                        }
                    }
                }
            }
        }


        /*//Search in thingTypeFields if there are references of thingChild's thingType
        List<ThingType> lstThingTypes = ThingTypeService.getInstance().getAllThingTypes();
        if(lstThingTypes!=null && lstThingTypes.size()>0)
        {
            for(ThingType thingtype: lstThingTypes)
            {
                if (thingtype.getThingTypeFields() == null)
                {   thingtype.setThingTypeFields(new HashSet<ThingTypeField>());
                    List<ThingTypeField> thingTypeFields = ThingTypeFieldService.getInstance().getByThingTypeCode(thingtype.getThingTypeCode());
                    for (ThingTypeField thingTypeField: thingTypeFields) {
                        thingtype.getThingTypeFields().add(thingTypeField);
                    }
                }
                for(ThingTypeField thingTypeField: thingtype.getThingTypeFields())
                {
                    if(thingTypeField.getDataTypeThingTypeId()!=null
                            && thingTypeField.getDataTypeThingTypeId().compareTo(thingChild.getId())==0)
                    {
                        Map<String, Object> mapUdf = new HashMap<>();
                        mapUdf.put("propertyName", thingTypeField.getName());
                        mapUdf.put("thingTypeCode", thingtype.getCode());
                        mapUdf.put("thingTypeId", thingtype.getId());
                        result.add(mapUdf);

                        if(this.isNativeParent(thingtype))
                        {
                            List<ThingTypeMap> thingMap = ThingTypeMapService.getInstance().getThingTypeMapByParentId
                                (thingtype.getId());
                            for(ThingTypeMap child : thingMap){
                                mapUdf = new HashMap<>();
                                mapUdf.put("propertyName", "parent."+thingTypeField.getName());
                                mapUdf.put("thingTypeId", child.getChild().getId());
                                result.add(mapUdf);
                            }
                        }

                    }
                }
            }
        }*/

        //Check list in children

        List<Map<String, Object>> lstDataMap = new ArrayList<>();
        Map<String, Object> dataMap;
        //Searching children
        for (Object obj : result) {
            Map<String, Object> data = (Map<String, Object>) obj;
            ThingType thingType = (ThingType) data.get("object");
            List<ThingType> lstChildrenUdf = ThingTypeService.getInstance().getChildrenUdfThingTypeCode(thingType.getId());
            for (ThingType child : lstChildrenUdf) {
                for (ThingTypeField thingtypeField : child.getThingTypeFields()) {
                    if (thingtypeField.getDataTypeThingTypeId() != null
                            && thingtypeField.getDataTypeThingTypeId().compareTo(thingType.getId()) == 0) {
                        dataMap = new HashMap<>();
                        dataMap.put("propertyName", thingtypeField.getName() + ".value." + data.get("propertyName").toString());
                        dataMap.put("thingTypeCode", thingtypeField.getThingType().getCode());
                        dataMap.put("thingTypeId", thingtypeField.getThingType().getId());
                        lstDataMap.add(dataMap);

                        if (ThingTypeMapService.getInstance().isParent(thingtypeField.getThingType())) {
                            dataMap = new HashMap<>();
                            dataMap.put("thingTypeId", thingtypeField.getThingType().getId());
                            dataMap.put("propertyName", "parent." + thingtypeField.getName() + ".value." + data.get("propertyName").toString());
                            lstDataMap.add(dataMap);
                        }
                    }
                }
            }
        }

        result.addAll(lstDataMap);
        return result;
    }

    /**
     * This method dies the Validations for non-allowed parent-child/multilevel relationship
     */
    public ValidationBean validateMultilevelRelationship(Map<String, Object> thingTypeMap) {
        ValidationBean valida = new ValidationBean();

        //3 levels using thingType UDfs
        valida = this.validateParentUdfCannotBeChild(thingTypeMap);
        if (valida.isError()) {
            return valida;
        }
        //Mixed Parents
        valida = this.validateMixedParent(thingTypeMap);
        if (valida.isError()) {
            return valida;
        }
        //2 Parents by thing Type udf
        valida = this.validateJustOneParentUdf(thingTypeMap);
        if (valida.isError()) {
            return valida;
        }
        //Mixed Children
        valida = this.validateMixedChildren(thingTypeMap);
        if (valida.isError()) {
            return valida;
        }
        //Thing Type Udf cannot be children too
        valida = this.validateThingTypeChildrenAndUdf(thingTypeMap);
        if (valida.isError()) {
            return valida;
        }

        return valida;
    }

    /***************************************************************
     * This method validates that a parent thing type by UDF cannot
     * be child of another parent thing type UDF
     ***************************************************************/
    public ValidationBean validateParentUdfCannotBeChild(Map<String, Object> thingTypeMap) {
        ValidationBean valida = new ValidationBean();
        if (thingTypeMap.containsKey("isParent") && thingTypeMap.containsKey("fields")) {
            Boolean isParent = Boolean.parseBoolean(thingTypeMap.get("isParent").toString());
            List<Map<String, Object>> fields = (List) thingTypeMap.get("fields");
            if (isParent && fields != null && fields.size() > 0) {
                if (this.countThingTypeUdf(fields, true) > 0) {
                    valida.setErrorDescription("A parent thing type by UDF cannot be child of another parent thing type.");
                }
            }
        }
        if (!valida.isError() && thingTypeMap.containsKey("childrenUdf")) {
            Boolean isParent = Boolean.parseBoolean(thingTypeMap.get("isParent").toString());
            List<Map<String, Object>> childrenUdf = (List<Map<String, Object>>) thingTypeMap.get("childrenUdf");
            if (isParent && childrenUdf != null && childrenUdf.size() > 0) {
                for (Object data : childrenUdf) {
                    Map<String, Object> dataMap = (Map<String, Object>) data;
                    ThingType thingChildType = ThingTypeService.getInstance().get(Long.parseLong(dataMap.get("id").toString()));
                    if ((dataMap.get("operation").equals("add")) && thingChildType.isIsParent()) {
                        valida.setErrorDescription("A parent thing type UDF cannot be child of another parent thing type UDF.");
                        break;
                    }
                }
            }
        }
        return valida;
    }

    /***************************************************************
     * This method validates that If a thing is a child then it
     * cannot add any thing type UDF
     ***************************************************************/
    public ValidationBean validateMixedParent(Map<String, Object> thingTypeMap) {
        ValidationBean valida = new ValidationBean();
        try {
            if (thingTypeMap.get("parent.ids") != null && ((List) thingTypeMap.get("parent.ids")).size() > 0
                    && this.countThingTypeUdf((List) thingTypeMap.get("fields"), true) > 0) {
                valida.setErrorDescription("Thing Type is a child, it cannot have a Parent Thing Type UDF as property.");
            }

        } catch (Exception e) {
            valida.setErrorDescription(e.getMessage());
        }

        return valida;
    }

    /***************************************************************
     * It validates that: if a thing is a child by UDF then it cannot add another thing type UDF
     ***************************************************************/
    public ValidationBean validateJustOneParentUdf(Map<String, Object> thingTypeMap) {
        ValidationBean valida = new ValidationBean();
        try {
            if (this.countThingTypeUdf((List) thingTypeMap.get("fields"), true) > 1) {
                valida.setErrorDescription("Thing Type cannot have more than one Parent Thing Type UDF as properties.");
            }

        } catch (Exception e) {
            valida.setErrorDescription(e.getMessage());
        }

        return valida;
    }

    /***************************************************************
     * This method validates this four validations:
     * 1. Parent thing type cannot be set as Parent Udf (checkbox disabled)
     * 2. Child thing type cannot be set as Parent (checkbox disabled)
     * 3. When a Thing has *parent checked, Parent/Child association should not be allowed (disable Assign Parent and/or Children)
     * 4. When a Thing try to associate a parent or child, None thing with *parent enabled should be displayed on the thing type list
     ***************************************************************/
    public ValidationBean validateMixedChildren(Map<String, Object> thingTypeMap) {
        ValidationBean valida = new ValidationBean();

        try {
            Boolean isParent = thingTypeMap.get("isParent") != null ? Boolean.parseBoolean(thingTypeMap.get("isParent").toString()) : null;
            //1. Parent thing type cannot be set as Parent Udf (checkbox disabled)
            if (thingTypeMap.get("child.ids") != null && ((List) thingTypeMap.get("child.ids")).size() > 0
                    && isParent != null && isParent) {
                valida.setErrorDescription("Native Parent Thing Type cannot be Parent Thing Type UDF.");
                return valida;
            }
            //2. Child thing type cannot be set as Parent (checkbox disabled)
            if (thingTypeMap.get("parent.ids") != null && ((List) thingTypeMap.get("parent.ids")).size() > 0
                    && isParent != null && isParent) {
                valida.setErrorDescription("Native Child Thing Type cannot be set as Parent Thing Type UDF.");
                return valida;
            }
            //3. When a Thing has *parent checked, Parent/Child association should not be allowed (disable Assign Parent and/or Children)
            if (isParent != null && isParent &&
                    ((thingTypeMap.get("parent.ids") != null && ((List) thingTypeMap.get("parent.ids")).size() > 0)
                            || (thingTypeMap.get("child.ids") != null && ((List) thingTypeMap.get("child.ids")).size() > 0))) {
                valida.setErrorDescription("Parent Thing Type UDF cannot have Native Parent Thing Type or Native Children Thing Type.");
                return valida;
            }
            //4. When a Thing try to associate a parent or child, None thing with *parent enabled should be displayed on the thing type list
            if (((thingTypeMap.get("parent.ids") != null && ((List) thingTypeMap.get("parent.ids")).size() > 0)
                    || (thingTypeMap.get("child.ids") != null && ((List) thingTypeMap.get("child.ids")).size() > 0))) {
                List<Number> lstParent = (List<Number>) thingTypeMap.get("parent.ids");
                List<Number> lstChildren = (List<Number>) thingTypeMap.get("child.ids");
                if (lstChildren != null && lstChildren.size() > 0) {
                    for (Number data : lstChildren) {
                        if (ThingTypeService.getInstance().get(data.longValue()).isIsParent()) {
                            valida.setErrorDescription("Thing Type cannot have Parent Thing Type UDF as children.");
                            break;
                        }
                    }
                }
                if (!valida.isError() && lstParent != null && lstParent.size() > 0) {
                    for (Number data : lstParent) {
                        if (ThingTypeService.getInstance().get(data.longValue()).isIsParent()) {
                            valida.setErrorDescription("Thing Type cannot have Parent Thing Type UDF as Native Parent.");
                            break;
                        }
                    }
                }
                return valida;
            }
        } catch (Exception e) {
            valida.setErrorDescription(e.getMessage());
        }

        return valida;
    }

    /***************************************************************
     * This method validates that Thing type cannot have the same thing type udf
     * as children
     ***************************************************************/
    public ValidationBean validateThingTypeChildrenAndUdf(Map<String, Object> thingTypeMap) {
        ValidationBean valida = new ValidationBean();
        if ((this.countThingTypeUdf((List) thingTypeMap.get("fields"), false) > 0)
                && (thingTypeMap.get("child.ids") != null && ((List) thingTypeMap.get("child.ids")).size() > 0
                || thingTypeMap.containsKey("childrenUdf"))) {
            List<Number> lstChildrenNative = thingTypeMap.get("child.ids") != null ?
                    (List<Number>) thingTypeMap.get("child.ids") : new ArrayList<Number>();
            List<Map<String, Object>> lstChildrenUdf = thingTypeMap.get("childrenUdf") != null ?
                    (List<Map<String, Object>>) thingTypeMap.get("childrenUdf") : new ArrayList<Map<String, Object>>();
            List fields = (List) thingTypeMap.get("fields");
            if (fields != null && fields.size() > 0) {
                for (Object field : fields) {
                    Map<String, Object> fieldMap = (Map<String, Object>) field;

                    if (((Integer) (fieldMap.get("type"))).longValue() == (ThingTypeField.Type.TYPE_THING_TYPE.value) &&
                            fieldMap.get("dataTypeThingTypeId") != null) {
                        ThingType thingType = ThingTypeService.getInstance().get(Long.parseLong(fieldMap.get("dataTypeThingTypeId").toString()));
                        if (lstChildrenUdf != null && lstChildrenUdf.size() > 0) {
                            for (Object data : lstChildrenUdf) {
                                Map<String, Object> dataMap = (Map<String, Object>) data;
                                ThingType thingChildType = ThingTypeService.getInstance().get(Long.parseLong(dataMap.get("id").toString()));
                                lstChildrenNative.add(thingChildType.getId());
                            }
                        }
                        for (Number id : lstChildrenNative) {
                            if (id.longValue() == thingType.getId().longValue()) {
                                valida.setErrorDescription("You cannot assign a Thing Type as UDF and Children in the same definition of a second Thing Type.");
                                return valida;
                            }
                        }
                    }

                }
            }
        }
        return valida;
    }

    /***************************************************************
     * This method validates that Thing type that was set as thing type
     * property cannot be set as Parent (checkbox disabled)
     * Update
     ***************************************************************/
    public ValidationBean validateThingTypeProperty(Long thingTypeId, Map<String, Object> thingTypeMap) {
        ValidationBean valida = new ValidationBean();
        try {
            Boolean isParent = thingTypeMap.get("isParent") != null ? Boolean.parseBoolean(thingTypeMap.get("isParent").toString()) : null;
            if (isParent != null && isParent && this.isThingTypeProperty(thingTypeId)) {
                valida.setErrorDescription("Thing type that was set as thing type property cannot be set as Parent.");
            }

        } catch (Exception e) {
            valida.setErrorDescription(e.getMessage());
        }

        return valida;
    }

    /***************************************************************
     * This method validates that the same Thing type cannot be
     * parent thing type
     ***************************************************************/
    public ValidationBean validateParentEqualsThingType(Long thingTypeId, Map<String, Object> thingTypeMap) {
        ValidationBean valida = new ValidationBean();

        if (thingTypeMap.get("parent.ids") != null) {
            List<Number> lstParent = (List<Number>) thingTypeMap.get("parent.ids");
            if (lstParent != null && lstParent.size() > 0) {
                for (Number data : lstParent) {
                    if (data.longValue() == thingTypeId.longValue()) {
                        valida.setErrorDescription("The same Thing Type cannot be Parent Thing Type of itself.");
                        break;
                    }
                }
            }
        }

        return valida;
    }

    /**********************************************************
     * Check if the ThingType is UDF of another Thing Type
     * Without check if it is Parent Udf or not
     ***********************************************************/
    public boolean isThingTypeProperty(Long thingTypeId) {
        boolean response = false;
        ThingType thingType = ThingTypeService.getInstance().get(thingTypeId);
        if (!thingType.isIsParent()) {
            QThingTypeField qThingTypeField = QThingTypeField.thingTypeField;
            List<Long> isThingTypeProperty =
                    ThingTypeFieldService.getThingTypeFieldDAO().getQuery().where(qThingTypeField.dataTypeThingTypeId.eq(thingTypeId))
                            .list(qThingTypeField.id.longValue());

            if (isThingTypeProperty != null && isThingTypeProperty.size() > 0) {

                response = true;
            }
        }
        return response;
    }

    /****************************************************
     * Checks if fields Map has a ThingTypeUdf
     ***************************************************/
    public int countThingTypeUdf(List<Map<String, Object>> fields, boolean validaIsParent) {
        int result = 0;
        if (fields != null && fields.size() > 0) {
            for (Object field : fields) {
                Map<String, Object> fieldMap = (Map<String, Object>) field;

                if (((Integer) (fieldMap.get("type"))).longValue() == (ThingTypeField.Type.TYPE_THING_TYPE.value) &&
                        fieldMap.get("dataTypeThingTypeId") != null) {
                    ThingType thingType = ThingTypeService.getInstance().get(Long.parseLong(fieldMap.get("dataTypeThingTypeId").toString()));
                    if (!validaIsParent) {
                        result++;
                    } else {
                        if (thingType.isIsParent()) {
                            result++;
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * List all the thing types that have the thingType as a thing type udf
     *
     * @param thingType that is contained in another thing types
     * @return all thingTypes that are associated to thingType or an empty list if none found
     */
    public List<ThingType> thingTypesAssociatedTo(ThingType thingType) {
        QThingTypeField qThingTypeField = QThingTypeField.thingTypeField;

        //get the ids of the thing types that are associate to thin type fields
        ListSubQuery thingTypeFieldSubQuery =
                new HibernateSubQuery().from(qThingTypeField).
                        where(qThingTypeField.dataTypeThingTypeId.eq(thingType.getId())).
                        list(qThingTypeField.thingType.id);

        //get the actual thing types
        QThingType qThingType = QThingType.thingType;

        return ThingTypeService.getThingTypeDAO().getQuery().where(
                qThingType.id.in(thingTypeFieldSubQuery)).list(qThingType);

    }

    public List<String> getReservedWords() {
        List<String> result = new ArrayList<>();
        for (ThingType.NonUDF item : ThingType.NonUDF.values()) {
            result.add(item.toString());
        }
        return result;
    }

    /**
     * Get thype multilevel: UDF, PARENT, CHILDREN
     *
     * @return
     */
    public String getTypeMultilevel(ThingType thingType) {
        String result = "";
        List<ThingType> lstParents = thingType.getParents();
        List<ThingType> lstChildren = thingType.getChildren();
        if (thingType.isThingTypeParent() || isThingTypeUDF(thingType)) {
            result = "UDF";
        } else if ((lstParents != null) && (!lstParents.isEmpty())) {
            result = "CHILD";
        } else if ((lstChildren != null) && (!lstChildren.isEmpty())) {
            result = "PARENT";
        } else {
            result = "PARENT";
        }
        return result;
    }

    /**
     * Is thingTyupeUDF
     *
     * @param thingType
     * @return
     */
    public boolean isThingTypeUDF(ThingType thingType) {
        boolean response = false;
        BooleanBuilder be = new BooleanBuilder();
        be.and(QThingTypeField.thingTypeField.dataTypeThingTypeId.eq(thingType.getId()));
        List<ThingTypeField> fields = ThingTypeFieldService.getInstance().listPaginated(be, null, null);
        if ((fields != null) && (!fields.isEmpty())) {
            response = true;
        }
        return response;
    }

    /**
     * Insert a new Thing Type
     *
     * @param group
     * @param name
     * @param thingTypeCode
     * @param template
     * @param autoCreate
     * @return
     */
    public ThingType insertThingTypeAndFieldsWithTemplate(
            Group group,
            String name,
            String thingTypeCode,
            ThingTypeTemplate template,
            boolean autoCreate) {

        ThingType thingType = null;

        //Validate Group
        if ((group != null) && StringUtils.isEmpty(name) && StringUtils.isEmpty(thingTypeCode) && (template != null)) {
            throw new UserException("Group, name, code and template are mandatory parameters.");
        }
        //Validate Thing Type
        if (ThingTypeService.getInstance().existsThingTypeCode(thingTypeCode, group)) {
            thingType = ThingTypeService.getInstance().getByCodeAndGroup(thingTypeCode, group);
        } else {
            thingType = insertThingTypeWithTemplate(group, name, thingTypeCode, template, autoCreate);
            List<ThingTypeFieldTemplate> thingTypeFieldTemplateList =
                    ThingTypeFieldTemplateService.getInstance()
                            .getThingTypeFielTemplatedByThingTypeTemplateId(template.getId());
            if (thingTypeFieldTemplateList != null) {
                for (ThingTypeFieldTemplate thingTypeFieldTemplate : thingTypeFieldTemplateList) {
                    ThingTypeFieldService.getInstance().insertThingTypeField(thingType,
                            thingTypeFieldTemplate.getName(),
                            thingTypeFieldTemplate.getUnit(),
                            thingTypeFieldTemplate.getSymbol(),
                            thingTypeFieldTemplate.getTypeParent(),
                            thingTypeFieldTemplate.getType(),
                            thingTypeFieldTemplate.isTimeSeries(),
                            thingTypeFieldTemplate.getDefaultValue(),
                            thingTypeFieldTemplate.getId());
                }
            }
        }
        return thingType;
    }

    /**
     * Populate new Thing Type with values of the template
     *
     * @param group
     * @param name
     * @param thingTypeCode
     * @param thingTypeTemplate
     * @param autoCreate
     * @return
     */
    private ThingType insertThingTypeWithTemplate(
            Group group, String name, String thingTypeCode, ThingTypeTemplate thingTypeTemplate, boolean autoCreate) {
        ThingType thingType = new ThingType(name);
        thingType.setArchived(false);
        thingType.setGroup(group);
        thingType.setThingTypeTemplate(thingTypeTemplate);
        thingType.setModifiedTime(new Date().getTime());
        thingType.setThingTypeCode(thingTypeCode);
        thingType.setAutoCreate(autoCreate);
        return ThingTypeService.getInstance().insert(thingType);
    }

    /**
     * Generate Code for Thing Type
     *
     * @param name
     * @return
     */
    public String getThingTypeCodeByName(String name) {
        if (name != null && !StringUtils.isEmpty(name)) {
            return name.toLowerCase().replace(" ", "_") + "_code";
        } else {
            throw new UserException("'name' value is mandatory. ");
        }
    }

    public ThingType popThingType(Group group, ThingType parent, String name, String thingTypeCode, ThingTypeTemplate template, boolean autocreate) {
        ThingType thingType = new ThingType(name);
        thingType.setArchived(false);
        thingType.setGroup(group);
        thingType.setThingTypeTemplate(template);
        thingType.setModifiedTime(new Date().getTime());
        thingType.setThingTypeCode(thingTypeCode);
        thingType.setAutoCreate(autocreate);
        ThingTypeService.getInstance().insert(thingType);
        return thingType;
    }

    public Long getThingTypeId(String name) {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QThingType.thingType.thingTypeCode.eq(name));
        ThingType thingType = getThingTypeDAO().selectBy(be);
        return thingType.getId();
    }

    /**
     * @param ttParent
     * @return the thing types that have references to ttParent
     */
    public List<ThingType> getReferences(ThingType ttParent) {
        return getReferences(ttParent.getId());
    }

    /**
     * @param parentId
     * @return the thing types that have references to parentId
     */
    public List<ThingType> getReferences(Long parentId) {
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        List<ThingType> list = query.from(QThingTypeField.thingTypeField)
                .innerJoin(QThingTypeField.thingTypeField.thingType, QThingType.thingType)
                .where(QThingTypeField.thingTypeField.dataType.id.eq(ThingTypeField.Type.TYPE_THING_TYPE.value)
                        .and(QThingTypeField.thingTypeField.dataTypeThingTypeId.eq(parentId)))
                .setCacheable(true)
                .list(QThingType.thingType);
        return list;
    }

    public void refreshCache(ThingType thingType, boolean delete) {
        BrokerClientHelper.refreshThingTypeCache(thingType, delete);
    }

    /**
     * Get graph for thing type map direction.
     *
     * @return a instance of {@link DirectedGraph}.
     */
    public DirectedGraph getThingTypeGraphByGroupId(List<Long> groupIds) {
        List<ThingTypeDirectionMap> listThingTypeDirection = getThingTypeDAO().getDirectedGraph(groupIds);
        DirectedGraph directedGraph = new DirectedGraph(listThingTypeDirection.size());
        for (ThingTypeDirectionMap thingTypeDirectionMap : listThingTypeDirection) {
            directedGraph.addEdge(
                    thingTypeDirectionMap.getThingTypeParentId(),
                    thingTypeDirectionMap.getThingTypeChildId());
        }
        return directedGraph;
    }

    /**
     * Get a  graph of all thing types.
     *
     * @return a instance of {@link DirectedGraph}.
     */
    public DirectedGraph getThingTypeGraph() {
        List<ThingTypeDirectionMap> listThingTypeDirection = getThingTypeDAO().getDirectedGraph();
        DirectedGraph directedGraph = new DirectedGraph(listThingTypeDirection.size());
        for (ThingTypeDirectionMap thingTypeDirectionMap : listThingTypeDirection) {
            directedGraph.addEdge(
                    thingTypeDirectionMap.getThingTypeParentId(),
                    thingTypeDirectionMap.getThingTypeChildId());
        }
        return directedGraph;
    }

    public List<String> getDirectionPath(Long thingTypeIDOrigin, Long thingTypeIDDestiny) {
        List<String> pathList = new ArrayList<>();
        List<String> directionPathList = getThingTypeDAO().getDirectionPath(thingTypeIDOrigin, thingTypeIDDestiny);
        for (String directionPath : directionPathList) {
            directionPath = StringUtils.remove(directionPath, ",null");
            String[] splitArray = StringUtils.split(directionPath, ",");
            for (String value : splitArray) {
                pathList.add(value);
                if (value.equals(String.valueOf(thingTypeIDDestiny))) {
                    break;
                }
            }
            break;
        }
        return pathList;
    }

    public Response listThingTypes(Integer pageSize, Integer pageNumber,
                                   String order, String where, String extra, String only, Long visibilityGroupId, String upVisibility,
                                   String downVisibility, String extend, String project, String... thingTypeCodes) {
        ThingTypeController thingTypeController = new ThingTypeController();
        Pagination pagination = new Pagination(pageNumber, pageSize);
        BooleanBuilder be = new BooleanBuilder();
        // 2. Limit visibility based on user's group and the object's group (group based authorization)
        Group visibilityGroup = VisibilityUtils.getVisibilityGroup(ThingType.class.getCanonicalName(), visibilityGroupId);
        EntityVisibility entityVisibility = thingTypeController.getEntityVisibility();
        be = be.and(GeneralVisibilityUtils.limitVisibilitySelectAll(entityVisibility, QThingType.thingType, visibilityGroup, upVisibility, downVisibility));
        // 4. Implement filtering
        be = be.and(QueryUtils.buildSearch(QThingType.thingType, where));
        User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
        Set ttIds = currentUser.getThingTypeResources();
        String code = null;
        try {
            for (String thingTypeCode : thingTypeCodes) {
                code = thingTypeCode;
                ThingType thingTypeValue = ThingTypeService.getInstance().getByCode(thingTypeCode);
                if (thingTypeValue != null) {
                    ttIds.add(thingTypeValue.getId());
                }
            }
        } catch (NonUniqueResultException e) {
            throw new UserException(code + "is an invalid Thing Type");
        }

        if (ttIds.size() > 0) {
            be = be.and(QThingType.thingType.id.in(ttIds));
        }

        Long count = ThingTypeService.getInstance().countList(be);
        List<Map<String, Object>> list = new LinkedList<Map<String, Object>>();
        // 3. Implement pagination
        for (ThingType thingType : ThingTypeService.getInstance().listPaginated(be, pagination, order)) {
            // Additional filter
            if (!thingTypeController.includeInSelect(thingType)) {
                continue;
            }
            // 5a. Implement extra
            String extraNew = extra;
            if ((extra != null) && (extra.contains("typeMultilevel"))) {
                String[] data = extra.split(",");
                List<String> lstData = new ArrayList<>();
                for (int i = 0; i < data.length; i++) {
                    if (!data[i].equals("typeMultilevel")) {
                        lstData.add(data[i]);
                    }
                }
                extraNew = StringUtils.join(lstData, ",");
            }
            Map<String, Object> publicMap = QueryUtils.mapWithExtraFields(thingType, extraNew, thingTypeController.getExtraPropertyNames());
            thingTypeController.addToPublicMap(thingType, publicMap, extra);
            // 5b. Implement only
            QueryUtils.filterOnly(publicMap, only, extraNew);
            QueryUtils.filterProjectionNested(publicMap, project, extend);

            publicMap.put("childrenUdf",
                    thingTypeController.getChildrenThingTypeUdf(thingType.getId(), ThingTypeService.getInstance().listPaginated(be, pagination, order)));
            publicMap.put("hasThingTypeUdf", ThingTypeService.getInstance().isWithThingTypeUdf(thingType));
            list.add(publicMap);
        }
        Map<String, Object> mapResponse = new HashMap<String, Object>();
        mapResponse.put("total", count);
        mapResponse.put("results", list);
        return RestUtils.sendOkResponse(mapResponse);
    }

    /**
     * Method that get the thing type fields implicated in a serial number formula.
     *
     * @param thingTypeCode A {@link String} that contained the thing type code to return the serial formula fields.
     * @return a {@link List}<{@link Map}<{@link String}, {@link Object}>> that contains the fields in report data entry
     * format  that used in a serial number formula. NOTE: Not return sequence types.
     * @throws NonUniqueResultException exception if the thing type not is a unique.
     */
    public List<Map<String, Object>> getSerialFormulaFields(String thingTypeCode) throws NonUniqueResultException {
        ThingType thingType = this.getByCode(thingTypeCode);
        if (thingType == null) {
            logger.warn("thing type with code : " + thingTypeCode + " not found.");
            throw new NotFoundException("thing type with code : " + thingTypeCode + " not found.");
        }
        String serialFormula = thingType.getSerialFormula();
        if (StringUtils.isBlank(serialFormula)) {
            return null;
        }
        List<Map<String, Object>> thingTypeFieldList = new ArrayList<>();
        for (ThingTypeField thinTypeField : thingType.getThingTypeFields()) {
            if (FormulaUtil.isPartOfFormula(serialFormula, thinTypeField.getName()) &&
                    thinTypeField.getDataType() != null &&
                    !thinTypeField.getDataType().isSequenceType()) {
                thingTypeFieldList.add(thinTypeField.getReportFieldMap());
            }
        }
        return thingTypeFieldList;
    }

    /**
     * Method that get the thing type fields implicated in a serial number formula.
     *
     * @param thingTypeId A {@link Long} that contained the thing type code to return the serial formula fields.
     * @return a {@link List}<{@link Map}<{@link String}, {@link Object}>> that contains the fields in report data entry
     * format  that used in a serial number formula. NOTE: Not return sequence types.
     * @throws NonUniqueResultException exception if the thing type not is a unique.
     */
    public List<Map<String, Object>> getSerialFormulaFields(Long thingTypeId) throws NonUniqueResultException {
        ThingType thingType = this.getById(thingTypeId);
        if (thingType == null) {
            logger.warn("thing type with id : " + thingTypeId + " not found.");
            throw new NotFoundException("thing type with id : " + thingTypeId + " not found.");
        }
        String serialFormula = thingType.getSerialFormula();
        if (StringUtils.isBlank(serialFormula)) {
            return null;
        }
        List<Map<String, Object>> thingTypeFieldList = new ArrayList<>();
        for (ThingTypeField thinTypeField : thingType.getThingTypeFields()) {
            if (FormulaUtil.isPartOfFormula(serialFormula, thinTypeField.getName()) &&
                    thinTypeField.getDataType() != null &&
                    !thinTypeField.getDataType().isSequenceType()) {
                thingTypeFieldList.add(thinTypeField.getReportFieldMap());
            }
        }
        return thingTypeFieldList;
    }

    /**
     * Method that get the thing type fields implicated in a serial number formula.
     *
     * @param thingTypeCode A {@link String} that contained the thing type code to return the serial formula fields.
     * @return a {@link List}<{@link Map}<{@link String}, {@link Object}>> that contains the fields in report data entry
     * format  that used in a serial number formula. NOTE: Not return sequence types.
     * @throws NonUniqueResultException exception if the thing type not is a unique.
     */
    public Set<String> getSerialFormulaList(String thingTypeCode) {

        ThingType thingType;

        try {
            thingType = this.getByCode(thingTypeCode);
        } catch (NonUniqueResultException ex) {
            logger.warn("More than one record has the same type code type.", ex);
            throw new ServerException("More than one record has the same type code type." + thingTypeCode);
        }

        if (thingType == null) {
            logger.warn("thing type with code : " + thingTypeCode + " not found.");
            throw new NotFoundException("thing type with code : " + thingTypeCode + " not found.");
        }
        String serialFormula = thingType.getSerialFormula();
        if (StringUtils.isBlank(serialFormula)) {
            return null;
        }
        Set<String> thingTypeFieldList = new TreeSet<>();
        for (ThingTypeField thinTypeField : thingType.getThingTypeFields()) {
            if (FormulaUtil.isPartOfFormula(serialFormula, thinTypeField.getName()) &&
                    thinTypeField.getDataType() != null &&
                    !thinTypeField.getDataType().isSequenceType()) {
                thingTypeFieldList.add(thinTypeField.getName());
            }
        }
        return thingTypeFieldList;
    }

    /**
     * Method that get the thing type fields implicated in a serial number formula.
     *
     * @param thingTypeId A {@link String} that contained the thing type code to return the serial formula fields.
     * @return a {@link List}<{@link Map}<{@link String}, {@link Object}>> that contains the fields in report data entry
     * format  that used in a serial number formula. NOTE: Not return sequence types.
     * @throws NonUniqueResultException exception if the thing type not is a unique.
     */
    public Set<String> getSerialFormulaList(Long thingTypeId) {

        ThingType thingType;

        try {
            thingType = this.getById(thingTypeId);
        } catch (NonUniqueResultException ex) {
            logger.warn("More than one record has the same type code type.", ex);
            throw new ServerException("More than one record has the same thing type id." + thingTypeId);
        }

        if (thingType == null) {
            logger.warn("thing type with id : " + thingTypeId + " not found.");
            throw new NotFoundException("thing type with id : " + thingTypeId + " not found.");
        }
        String serialFormula = thingType.getSerialFormula();
        if (StringUtils.isBlank(serialFormula)) {
            return null;
        }
        Set<String> thingTypeFieldList = new TreeSet<>();
        for (ThingTypeField thinTypeField : thingType.getThingTypeFields()) {
            if (FormulaUtil.isPartOfFormula(serialFormula, thinTypeField.getName()) &&
                    thinTypeField.getDataType() != null &&
                    !thinTypeField.getDataType().isSequenceType()) {
                thingTypeFieldList.add(thinTypeField.getName());
            }
        }
        return thingTypeFieldList;
    }

    /**
     * Verify if the thing type need fields to calculate the serial number with formula.
     *
     * @param thingTypeCode   A {@link String} that contains the thing type code.
     * @param thingFieldNames a {@link Set}<{@link String}> that contains the field names to validate.
     * @throws NonUniqueResultException if exists a error with the thing type code.
     */
    public void validateFormulaFields(String thingTypeCode, Set<String> thingFieldNames) {
        Set<String> formulaMissingFields = this.getSerialFormulaList(thingTypeCode);
        if (formulaMissingFields == null) {
            return;
        }
        formulaMissingFields.removeAll(thingFieldNames);
        if (!formulaMissingFields.isEmpty()) {
            throw new UserException("There are missing fields to create the serial number: " +
                    formulaMissingFields.toString() +
                    " for the type type: " + thingTypeCode);
        }
    }

    /**
     * Verify if the thing type need fields to calculate the serial number with formula.
     *
     * @param thingTypeId   A {@link String} that contains the thing type code.
     * @param thingFieldNames a {@link Set}<{@link String}> that contains the field names to validate.
     * @throws NonUniqueResultException if exists a error with the thing type code.
     */
    public void validateFormulaFields(Long thingTypeId, Set<String> thingFieldNames) {
        Set<String> formulaMissingFields = this.getSerialFormulaList(thingTypeId);
        if (formulaMissingFields == null) {
            return;
        }
        formulaMissingFields.removeAll(thingFieldNames);
        if (!formulaMissingFields.isEmpty()) {
            throw new UserException("There are missing fields to create the serial number: " +
                    formulaMissingFields.toString() +
                    " for the thing type id: " + thingTypeId);
        }
    }

    /**
     * Get thing type tree list by thing type code
     *
     * @param thingTypeId A long that contains the thing type code
     * @returna A {@link List}<ThingType>
     */
    public List<ThingType> getByThingTypeAndGroup(Long thingTypeId, List<Long> groupIds) {
        return getThingTypeDAO().getThingTypeIn(getByThingTypeIdAndGroupsIds(thingTypeId, groupIds));
    }

    /**
     * Get thing type tree list by thing type code
     *
     * @param thingTypeId A long that contains the thing type code
     * @returna A {@link List}<ThingType>
     */
    public List<Long> getByThingTypeIdAndGroupsIds(Long thingTypeId, List<Long> groupIds) {
        //Get all parents.
        Long[] allParents = ThingTypeService.getThingTypeDAO().getAllParents();

        //Get recursively all paths for a thing type directed graph  view.
        DirectedGraph thingTypeGraph = ThingTypeService.getInstance().getThingTypeGraphByGroupId(groupIds);
        return thingTypeGraph.findAllPathsThroughMerged(thingTypeId, allParents);
    }

    /**
     * Get thing type tree list by thing type code
     *
     * @param thingTypeId A long that contains the thing type code
     * @returna A {@link List}<ThingType>
     */
    public List<Long> getThingTypeIdsOfPathsByThingTypeId(Long thingTypeId) {
        //Get all parents.
        Long[] allParents = ThingTypeService.getThingTypeDAO().getAllParents();
        //Get recursively all paths for a thing type directed graph  view.
        DirectedGraph thingTypeGraph = ThingTypeService.getInstance().getThingTypeGraph();
        return thingTypeGraph.findAllPathsThroughMerged(thingTypeId, allParents);
    }

    public List<Map<String, Object>> getThingTypeTreeByIdAndGroups(Long thingTypeId, List<Long> groupIds) {
        //Get all parents.
        Long[] allParents = ThingTypeService.getThingTypeDAO().getAllParents();

        //Get recursively all paths for a thing type directed graph  view.
        DirectedGraph thingTypeGraph = ThingTypeService.getInstance().getThingTypeGraphByGroupId(groupIds);

        List<LinkedList<Long>> allPathsThrough = thingTypeGraph.findAllPathsThrough(thingTypeId, allParents);

        if (allPathsThrough.size() == 0) {
            LinkedList<Long> thingTypeIdList = new LinkedList<>();
            thingTypeIdList.add(thingTypeId);
            allPathsThrough.add(thingTypeIdList);
        }

        //Merge all paths in a unique list.
        List<Long> allPathsTroughMerged = allPathsThrough.stream()
                .flatMap(List::stream).distinct().collect(Collectors.toList());
        List<ThingType> thingTypes = getThingTypeDAO().getThingTypeIn(allPathsTroughMerged);
        List<Map<String, Object>> thingTypeList = new LinkedList<>();
        for (Long parent : MatrixUtils.getFirstElements(allPathsThrough)) {
            ThingType thingType = thingTypes
                    .stream()
                    .filter(x -> x.getId().equals(parent))
                    .findFirst().orElse(null);
            if (thingType != null) {
                thingTypeList.add(findPathAt(thingType, thingTypeGraph, thingTypes));
            }
        }
        return thingTypeList;
    }

    private Map<String, Object> findPathAt(ThingType thingType,
                                           DirectedGraph thingTypeGraph,
                                           List<ThingType> thingTypes) {
        LinkedList<Long> adj = thingTypeGraph.getAdjByVertex(thingType.getId());
        if (adj == null || adj.isEmpty()) {
            return thingType.publicMapTreeView(true);
        } else {
            Map<String, Object> thingTypeMap = thingType.publicMapTreeView(true);
            List<Map<String, Object>> children = new LinkedList<>();
            for (Long w : adj) {
                ThingType child = thingTypes
                        .stream()
                        .filter(x -> x.getId().equals(w))
                        .findFirst().orElse(null);
                if (child != null) {
                    children.add(findPathAt(child, thingTypeGraph, thingTypes));
                }
            }
            if (!children.isEmpty()) {
                thingTypeMap.put("children", children);
            }
            return thingTypeMap;
        }
    }

    public void loadAllInCache() {
        boolean cacheCreated = true;
        boolean thingTypeCacheExists = CacheBoundary.getInstance().cacheExists(THING_TYPE_CACHE_NAME);
        if (!thingTypeCacheExists) {
            cacheCreated = CacheBoundary.getInstance().createCache(THING_TYPE_CACHE_NAME, String.class, ThingType.class);
        }
        if (cacheCreated) {
            List<ThingType> allThingTypes = getAllThingTypes();
            allThingTypes.forEach(tt -> putOneInCache(tt));
        } else {
            logger.error("Could not create cache");
        }
    }

    public void putOneInCache(ThingType tt) {
        initializeLazyThingType(tt);
        tt.getThingTypeFields().stream()
                .filter(ttf -> ttf.getDataTypeThingTypeId() != null)
                .map(ttf -> ttf.getDataTypeThingTypeId())
                .forEach(ttid -> putThingTypeDataType(ttid));
        CacheBoundary.getInstance().put(THING_TYPE_CACHE_NAME,
                //tt.getGroup().getId() + "-" + tt.getThingTypeCode(), tt,
                tt.getThingTypeCode(), tt,
                String.class, ThingType.class);
    }

    public ThingType getFromCache(String thingTypeCode) {
        ThingType thingType = CacheBoundary.getInstance().get(THING_TYPE_CACHE_NAME, thingTypeCode, String.class, ThingType.class);
        return (thingType == null && !containsKey(thingTypeCode))? lazyLoad(thingTypeCode) : thingType;
    }

    public List<ThingType> getChildrenFromCache(String thingTypeCode) {
        List<ThingType> result = new ArrayList<>();
        ThingType thingTypeFromCache = ThingTypeService.getInstance().getFromCache(thingTypeCode);
        if (thingTypeFromCache.getChildrenTypeMaps() != null) {
            thingTypeFromCache.getChildrenTypeMaps().stream()
                    .filter(ctm -> ctm.getChild() != null)
                    .forEach(ctm -> result.add(ctm.getChild()));
        }
        return result;
    }

    public void removeOneFromCache(String thingTypeCode) {
        CacheBoundary.getInstance().remove(THING_TYPE_CACHE_NAME, thingTypeCode, String.class, ThingType.class);
    }

    public List<ThingType> getThingTypeDataFromCache(Long dataTypeThingTypeId) {
        List<ThingType> thingTypeList = CacheBoundary.getInstance().get(THING_TYPE_DATA_TYPE_CACHE_NAME, dataTypeThingTypeId, Long.class, List.class);
        return (thingTypeList == null && !containsThingTypeDataKey(dataTypeThingTypeId))? lazyLoadThingTypeData(dataTypeThingTypeId) : thingTypeList;
    }

    public ThingType getByThingTypeCode(String thingTypeCode) {
        HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
        return query.from(QThingType.thingType)
                .where(QThingType.thingType.thingTypeCode.eq(thingTypeCode))
                .setCacheable(true)
                .uniqueResult(QThingType.thingType);
    }

    private ThingType lazyLoad(String thingTypeCode){
        ThingType thingType = getByThingTypeCode(thingTypeCode);
        if(thingType != null){
            putOneInCache(thingType);
        }
        return thingType;
    }

    private boolean containsKey(String thingTypeCode) {
        return CacheBoundary.getInstance()
                .containsKey(THING_TYPE_CACHE_NAME, thingTypeCode, String.class, ThingType.class);
    }

    private void putThingTypeDataType(Long dataTypeThingTypeId) {
        boolean cacheCreated = true;
        boolean thingTypeDataTypeCacheExists = CacheBoundary.getInstance().cacheExists(THING_TYPE_DATA_TYPE_CACHE_NAME);
        if (!thingTypeDataTypeCacheExists) {
            cacheCreated = CacheBoundary.getInstance().createCache(THING_TYPE_DATA_TYPE_CACHE_NAME, Long.class, List.class);
        }
        if (cacheCreated) {
            List<ThingType> parentThingTypeList = getReferences(dataTypeThingTypeId);
            for (ThingType tt : parentThingTypeList) {
                initializeLazyThingType(tt);
            }
            CacheBoundary.getInstance().put(THING_TYPE_DATA_TYPE_CACHE_NAME,
                    dataTypeThingTypeId, parentThingTypeList,
                    Long.class, List.class);
        } else {
            logger.error("Could not create cache");
        }
    }

    private List<ThingType> lazyLoadThingTypeData(Long dataTypeThingTypeId){
        putThingTypeDataType(dataTypeThingTypeId);
        return CacheBoundary.getInstance().get(THING_TYPE_DATA_TYPE_CACHE_NAME, dataTypeThingTypeId, Long.class, List.class);
    }

    private boolean containsThingTypeDataKey(Long dataTypeThingTypeId) {
        return CacheBoundary.getInstance()
                .containsKey(THING_TYPE_DATA_TYPE_CACHE_NAME, dataTypeThingTypeId, Long.class, List.class);
    }

    private void initializeLazyThingType(ThingType tt) {
        Hibernate.initialize(tt.getGroup());
        Hibernate.initialize(tt.getThingTypeTemplate());
        Hibernate.initialize(tt.getThingTypeFields());
        Hibernate.initialize(tt.getChildrenTypeMaps());
        Hibernate.initialize(tt.getParentTypeMaps());
        //POPDB hack
        if(tt.getThingTypeFields() == null){
            tt.setThingTypeFields(new HashSet<>(ThingTypeFieldService.getInstance().getThingTypeField(tt.getId())));
        }
        for (ThingTypeField ttf : tt.getThingTypeFields()) {
            Hibernate.initialize(ttf.getDataType());
        }
    }

    public void validateThingTypeUDF(ThingTypeField ttf) {
        if((ttf.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_THING_TYPE.value)==0)
                && !ttf.getTimeSeries()) {
            throw new UserException("Time Series value in Property Thing Type UDF" +
                    " '"+ttf.getName()+"' should be true");
        }
        if(ttf.getMultiple() != null && ttf.getMultiple()) {
            throw new UserException("Multiple value in Property Thing Type UDF" +
                    " '"+ttf.getName()+"' should be false");
        }
    }
}
