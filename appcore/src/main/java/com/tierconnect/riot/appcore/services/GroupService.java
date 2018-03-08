package com.tierconnect.riot.appcore.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.group.GroupBy;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.mysema.query.types.Predicate;
import com.tierconnect.riot.appcore.cache.CacheBoundary;
import com.tierconnect.riot.appcore.dao.GroupDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.utils.AuthenticationUtils;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.commons.DateFormatAndTimeZone;
import com.tierconnect.riot.sdk.dao.HibernateSessionFactory;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.NotFoundException;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.servlet.exception.ForbiddenException;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.hibernate.Hibernate;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.tierconnect.riot.appcore.utils.Utilities.isEmptyOrNull;
import static com.tierconnect.riot.commons.Constants.*;
import static com.tierconnect.riot.sdk.dao.QueryDSLUtils.getPath;

public class GroupService extends GroupServiceBase {
    static Logger logger = Logger.getLogger(GroupService.class);

	private static final QGroup qGroup = QGroup.group;
	static private GroupDAO _groupDAO;
	public static final String CHARACTER_SPECIALS_GROUP_NAME = " _$&@";
	public static final String CHARACTER_SPECIALS_GROUP_CODE = "_$&@";
	public static final String CHARACTER_SPECIALS_DATE_FORMAT = ":\\_\\-&\\/.,\"\\[\\]\\(\\)\\\\ ";
	public static final List<String> zoneList;
	private static final String BROKER_CLIENT_HELPER = "com.tierconnect.riot.iot.services.BrokerClientHelper";
	private static final String GROUP_CACHE_NAME = "groupCache";

	static {
		// not support in moment.js
		List<String> zoneListIds = new ArrayList<>(ZoneId.getAvailableZoneIds());
		zoneListIds.remove("SystemV/AST4");
		zoneListIds.remove("SystemV/AST4ADT");
		zoneListIds.remove("SystemV/CST6");
		zoneListIds.remove("SystemV/CST6CDT");
		zoneListIds.remove("SystemV/EST5");
		zoneListIds.remove("SystemV/EST5EDT");
		zoneListIds.remove("SystemV/HST10");
		zoneListIds.remove("SystemV/MST7");
		zoneListIds.remove("SystemV/MST7MDT");
		zoneListIds.remove("SystemV/PST8");
		zoneListIds.remove("SystemV/PST8PDT");
		zoneListIds.remove("SystemV/YST9");
		zoneListIds.remove("SystemV/YST9YDT");
		zoneList = Collections.unmodifiableList(zoneListIds);
	}

	public static GroupDAO getGroupDAO(){
		if(_groupDAO == null){
			_groupDAO = new GroupDAO();
		}
		return _groupDAO;
	}

	public Group getRootGroup()
	{
		return getInstance().get( 1L );
	}

	public Group insert(Group group) {
		if (group == null) {
			throw new UserException("Group is Empty");
		}
		if (group.getName() == null || "".equals(group.getName())) {
			throw new UserException("Name is a required parameter.");
		}
		if (group.getCode() == null || "".equals(group.getCode())) {
			throw new UserException("Code is a required parameter.");
		}
		if (!group.getCode().equals("root") && group.getGroupType() == null) {
			throw new IllegalArgumentException("GroupType is a required parameter.");
		}
		validateUniqueCode(group, null);
		validateUniqueName(group, null);
		group.setParentGroupAndRecalculate();
        group.setHierarchyName(group.getHierarchyName(group.getName().equals("root")));
        Long id = getGroupDAO().insert(group);
        refreshHierarchyName();
        group.setId(id);

        return group;
    }

	public Group update(Group group) {
		if (group == null) {
			throw new UserException("Group is Empty");
		}
		if (group.getName() == null || "".equals(group.getName())) {
			throw new UserException("Name is a required parameter.");
		}
		if (group.getParent() != null && group.getParent().getId().equals(group.getId())) {
			throw new UserException("Incorrect Parent Group, cannot refer to himself.");
		}
		if (group.getCode() == null || "".equals(group.getCode())) {
			throw new UserException("Code is a required parameter.");
		}
		if (!Utilities.isAlphaNumericCharacterSpecials(group.getCode(), CHARACTER_SPECIALS_GROUP_CODE)) {
			throw new UserException("Code group has invalid characters, only alphanumeric and character [_ & $ @] are allowed.");
		}
		// replate character specials
		group.setCode(group.getCode().replaceAll("@", "a"));
		group.setCode(group.getCode().replaceAll("&", "And"));
		group.setCode(group.getCode().replaceAll("\\$", "s"));
		validateUniqueCode(group, group.getId());
        validateUniqueName(group, group.getId());
        validateCiclic(group, group.getParent());
		group.setParentGroupAndRecalculate();
        group.setHierarchyName(group.getHierarchyName(group.getName().equals("root")));
        getGroupDAO().update(group);
		updateFavorite(group);
        refreshHierarchyName();
        return group;
    }

	public void delete(Group group) {
    	// Hibernate cannot delete an object that references itself
    	group.setParentLevel(group.getTreeLevel(), null);
		getGroupDAO().update(group);
		Group group2 = getGroupDAO().selectById(group.getId());
		getGroupDAO().delete(group2);
        refreshHierarchyName();
	}

    public void refreshHierarchyName(){
        for(Group group : getGroupDAO().selectAll()){
            String hierachyName = group.getHierarchyName(group.getName().equals("root"));
            if(!hierachyName.isEmpty())group.setHierarchyName(hierachyName);
            getGroupDAO().update(group);
        }
    }

	public List<Group> findByParent(Group group) {
		return getGroupDAO().selectAllBy(qGroup.parent.eq(group));
	}


    /**
     * Finds all groups under this parent group.
     * @param group parent group
     * @return all children including the parent
     */
    public List<Group> findByAllParent(Group group) {
        List<Group> children = new ArrayList<>();
        childGroups(group, children);
        return children;
    }

	/**
	 * Returns all Groups on a Map
	 *
	 * @return Map&lt;Long, Group&gt;
	 */
	public Map<Long, Group> getMapGroup() {
		HibernateQuery query = GroupService.getGroupDAO().getQuery();
		return query.transform(GroupBy.groupBy(QGroup.group.id).as(QGroup.group));
	}

    /**
     * Helper method to iterate through all the groups under group
     * @param group parent group
     * @param children store children
     */
    private void childGroups(Group group, List<Group> children) {
        children.add(group);
        for (Group g : getGroupDAO().selectAllBy(qGroup.parent.eq(group)) ) {
            childGroups(g, children);
        }
    }

	public Predicate getDescendantsIncludingPredicate(QGroup base,Group group){
        try {
            return getQParentGroupLevel(base, group.getTreeLevel()).id.eq(group.getId());
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            return null;
        }
	}

	public Predicate getSiblingsExcludingPredicate(Group group) {
		Group parent = group.getParent();
		BooleanBuilder b = new BooleanBuilder();
		b = b.and(parent == null ? qGroup.parent.isNull() : qGroup.parent.eq(parent));
		if (group.getId() != null) {
			b = b.and(qGroup.ne(group));
		}
		return b.getValue();
	}

	public Predicate getAscendantsExcludingPredicate(Group group) {
		return getAscendantsExcludingPredicate(qGroup, group);
	}

	//TODO: Improve performance
	public Predicate getAscendantsExcludingPredicate(QGroup base, Group group) {
		BooleanBuilder b = new BooleanBuilder();
		List<Long> ascendants = new ArrayList<>();
		if (group.getId() == null) {
			if (group.getParent() != null) {
				ascendants = group.getAscendantIds();
				ascendants.add(group.getParent().getId());
			}
		} else {
			ascendants = group.getAscendantIds();
		}
		for (Long ascendantId: ascendants) {
			b = b.or(base.id.eq(ascendantId));
		}
		return b.getValue();
	}

	public void validateUniqueName(Group group, Long excludeId){
		BooleanBuilder groupBe = new BooleanBuilder();
		//Siblings
		if(group.getParent() != null){
			groupBe = groupBe.and(QGroup.group.parent.eq(group.getParent()));
		}
		//Descendants
		if(group.getId() != null){
			groupBe = groupBe.or(getDescendantsIncludingPredicate(QGroup.group,group));
		}
		//Ascendants
		groupBe = groupBe.or(getAscendantsExcludingPredicate(group));

		BooleanBuilder be = new BooleanBuilder();
		if(excludeId != null){
			be = be.and(QGroup.group.id.ne(excludeId));
		}
		be.and(groupBe).and(QGroup.group.name.eq(group.getName()));

		if(getGroupDAO().getQuery().where(be).exists()){
			throw new UserException("Duplicated Name");
		}
	}

	private void validateUniqueCode(Group group, Long excludeId) {
		BooleanBuilder groupBe = new BooleanBuilder();
		//Siblings
		QGroup qGroup = QGroup.group;
		if(group.getParent() != null){
			groupBe = groupBe.or(qGroup.parent.id.eq(group.getParent().getId()));
		}
		//Descendants
		if(group.getId() != null){
			groupBe = groupBe.or(getDescendantsIncludingPredicate(qGroup,group));
		}
		//Ascendants
		groupBe = groupBe.or(getAscendantsExcludingPredicate(group));

		BooleanBuilder be = new BooleanBuilder();
		if(excludeId != null){
			be = be.and(qGroup.id.ne(excludeId));
		}
		be.and(groupBe).and(qGroup.code.eq(group.getCode()));

		if(getGroupDAO().getQuery().where(be).exists()){
			throw new UserException("Duplicated Code group");
		}
	}


	/**
	 *
	 * @param group
	 * @param treeRoot
	 * @return true if group is a child of or is treeRoot
	 */
	public boolean isGroupInsideTree(Group group,Group treeRoot){
		if (group.getId().equals(treeRoot.getId())) {
			return true;
		}
		int rootLevel = treeRoot.getTreeLevel();
		if(group.getTreeLevel() < rootLevel){
			return false;
		}
		if (treeRoot.getId().equals(group.getParentLevel(rootLevel).getId()) == false) {
			return false;
		}
		return true;
	}

	public boolean isGroupNotInsideTree(Group group,Group treeRoot){
		return !isGroupInsideTree(group, treeRoot);
	}

	private void validateCiclic(Group group, Group newParent) {
		if (newParent != null && newParent.getAscendantIds().contains(group.getId())) {
			throw new UserException("Ciclic relation ship found for groups.");
		}
	}

	public QGroup getQParentGroupLevel(QGroup qGroup,int level){
		try {
			return (QGroup) qGroup.getClass().getField("parentLevel"+level).get(qGroup);
		} catch (Exception e) {
			throw new UserException("Invalid parentLevel"+level, e);
		}
    }

	/**
	 * Matches the group name and its parent to retrieve a group
	 *
	 * @param groupName name in hierarchy format ex company > facility
	 * @return group that matches. null if the group name is invalid
	 * @throws NonUniqueResultException if there are more groups with the exact name
	 */
	public Group getByHierarchyName(String groupName) throws NonUniqueResultException
	{
		Group group = null;
		if (StringUtils.isNotEmpty(groupName)) {
			try
			{
				String[] groupNames = StringUtils.split(groupName, ">");

				if (groupNames.length > 0)
				{
					//get name
					String name = StringUtils.trim(groupNames[groupNames.length - 1]);
					BooleanBuilder b = new BooleanBuilder(qGroup.name.eq(name));

					//get parent name
					if (groupNames.length >= 2)
					{
						String parentName = StringUtils.trim(groupNames[groupNames.length - 2]);
						b.and(qGroup.parent.name.eq(parentName));
					}

					group =  getGroupDAO().selectBy(b);
				}
			}
			catch (org.hibernate.NonUniqueResultException e)
			{
				throw new NonUniqueResultException(e);
			}
		}
		return group;
	}

	/**
	 * Matches the group code and its parent to retrieve a group
	 *
	 * @param groupCode code in hierarchy format ex company > facility
	 * @return group that matches. null if the group code is invalid
	 * @throws NonUniqueResultException if there are more groups with the exact name
	 */
	public Group getByHierarchyCode(String groupCode) throws NonUniqueResultException
	{
		Group group = null;
		if (StringUtils.isNotEmpty(groupCode)) {
			try{
                String hierarchyCode = groupCode.equals(">root") ? groupCode : groupCode.replaceAll("root>", "");

                BooleanBuilder b = new BooleanBuilder(qGroup.hierarchyName.eq(hierarchyCode));
                group = getGroupDAO().selectBy(b);
            }catch (org.hibernate.NonUniqueResultException e){
				throw new NonUniqueResultException(e);
			}
		}
		return group;
	}

	public Long countAllActive() {
		return getGroupDAO().countAll(qGroup.archived.isFalse());
	}

	public Long countAllActiveTenants() {
		return getGroupDAO().countAll(qGroup.archived.isFalse().and(qGroup.treeLevel.eq(2)));
	}

	public Long countAllActiveSubTenants() {
		return getGroupDAO().countAll(qGroup.archived.isFalse().and(qGroup.treeLevel.eq(3)));
	}

	public Long countAllActiveSubTenants(Group tenantGroup) {
		return getGroupDAO().countAll(qGroup.archived.isFalse().and(qGroup.treeLevel.eq(3).and(qGroup.parent.eq(tenantGroup))));
	}

	/*
	* This method gets a group based on the name of the group
	* */
	public Group getByName(String name) throws NonUniqueResultException {
		try {
			return getGroupDAO().selectBy("name", name);
		}
		catch (org.hibernate.NonUniqueResultException e) {
			throw new NonUniqueResultException(e);
		}
	}

	/*
	* This method gets a group based on the code of the group
	* */
	public Group getByCode(String code) throws NonUniqueResultException {
		try {
			return getGroupDAO().selectBy("code", code);
		}
		catch (org.hibernate.NonUniqueResultException e) {
			throw new NonUniqueResultException(e);
		}
	}

	/**
	 * This method gets a Group based on the code of the zone and the group
	 * */
	public Group getByCodeAndGroup(String code, String hierarchyPathGroup) throws NonUniqueResultException {
		try {
			//Get the descendants of the group including the group
			List<Group> lstGroups = null;
			Group group = GroupService.getInstance().getByHierarchyCode(hierarchyPathGroup);
			if(group !=null)
			{
				BooleanBuilder groupBe = new BooleanBuilder();
				groupBe = groupBe.and(
						GroupService.getInstance().getDescendantsIncludingPredicate( QGroup.group, group ) );
				lstGroups = GroupService.getInstance().getGroupDAO().selectAllBy( groupBe );
			}

			BooleanBuilder b = new BooleanBuilder();
			b = b.and( QGroup.group.group.id.in( getListOfIds( lstGroups )  ) );
			b = b.and( QGroup.group.code.eq(code) ) ;
			return getGroupDAO().selectBy( b );
		}
		catch (org.hibernate.NonUniqueResultException e) {
			throw new NonUniqueResultException(e);
		}
	}

	/*********************************
	 * Method to get a List of Ids of an Object
	 ********************************/
	public static List<Long> getListOfIds(List<?> listOfObjects)
	{
		List<Long> response =  new ArrayList<>(  );
		if(listOfObjects!=null && listOfObjects.size()>0)
		{
			response = new ArrayList<Long>();

			for(Object data : listOfObjects)
			{
				if(data instanceof Group)
				{
					response.add( ((Group) data).getId());
				}
			}
		}
		return response;
	}

	/**************************************************************************
	 * Validate path  file from a defined location on the server
	 *************************************************************************/
	public Map<String , Object> validatePathFile(String fileName)  throws  Exception
	{
		Map<String , Object> response = new HashMap<>(  );
		response.put( "errorCode", "OK");
		response.put( "errorMessage", "" );
		if(fileName!=null && !fileName.trim().equals( "" ))
		{
			if( this.isValidPathFile( fileName ) )
			{
				if( !this.hasValidPermissionsToCreateFile( fileName ) )
				{
					response.put( "errorCode", "NOK" );
					response.put( "errorMessage", "System does not have permission to write in file path: '" + fileName + "' " );
				}
			}
			else
			{
				response.put( "errorCode", "NOK" );
				response.put( "errorMessage", "The file path: '" + fileName + "' is not valid." );
			}
		}else
		{
			response.put( "errorCode", "NOK" );
			response.put( "errorMessage", "The file path: '" + fileName + "' is not valid." );
		}

		return response;
	}
	/**************************************************************************
	 * Validate path  file from a defined location on the server
	 *************************************************************************/
	public boolean isValidPathFile(String fileName)  throws  Exception
	{
		boolean response = false;
		File file =  new File(fileName);
		if(file!=null && file.exists())
		{
			response =  true;
		}
		return response;
	}

	/**************************************************************************
	 * Validate path  file from a defined location on the server
	 *************************************************************************/
	public boolean hasValidPermissionsToCreateFile(String fileName)  throws  Exception
	{
		boolean response = false;
		//Create folder
		File folder = new File( fileName );
		boolean folderExists = folder.exists();
		folder.mkdirs();
		fileName = fileName+"\\test.txt";
		File file =  new File(fileName);
		try {
			file.createNewFile();
			file.delete();
            file = null;
			response =  true;
		}
		catch (Exception e) {
			return false;
		}
		if(!folderExists)
		{
			folder.delete();
		}
		return response;
	}

	/*******************************************************
	 * This method returns the list of groups children based on a
	 * group id parent
	 *******************************************************/
	public List<Long> getListGroupIdsChildren(Long groupId) {
		// get child group ids
		GroupService groupService = GroupService.getInstance();
		Group group = groupService.get(groupId);
		List<Group> children = groupService.findByAllParent(group);
		return children.stream().map(Group::getId).collect(Collectors.toList());
	}

	/**
	 * Sending message to refresh groups in coreBridge
	 */
	public void sendMQTTSignal(List<Long> groupMqtt)
	{
		try
		{
			Class clazz = Class.forName(BROKER_CLIENT_HELPER);
			clazz.getMethod("sendRefreshGroupsMessage", Boolean.class, String.class, List.class)
					.invoke(null, false, Thread.currentThread().getName(), groupMqtt);
		}
		catch (Exception e) {
			logger.error("Could not call MQTT sendRefreshGroupsMessage method");
		}
	}

	/**
	 * it updates a message of kafka cache topic ___v1___cache___group.
	 *
	 * @param group
	 * @param delete
     */
	public void refreshGroupCache(Group group, boolean delete){
		try
		{
			Class clazz = Class.forName(BROKER_CLIENT_HELPER);
			clazz.getMethod("refreshGroupCache",Group.class, boolean.class).invoke(null, group, delete);
		}
		catch (Exception e) {
			logger.error(String.format("Could not refresh kafka cache (topic='___v1___cache___group'), group=['%s']",group),e);
		}
	}

	public void validateInsert(Group group) {
		User user = (User) SecurityUtils.getSubject().getPrincipal();
		GroupService groupService = GroupService.getInstance();
		if (!Utilities.isAlphaNumericCharacterSpecials(group.getCode(), CHARACTER_SPECIALS_GROUP_CODE)) {
			throw new UserException("Code group has invalid characters, only alphanumeric and character [_ & $ @] are allowed.");
		}
		// replate character specials
		group.setCode(group.getCode().replaceAll("@", "a"));
		group.setCode(group.getCode().replaceAll("&", "And"));
		group.setCode(group.getCode().replaceAll("\\$", "s"));
		group.setParentGroupAndRecalculate();
		if (group.getTreeLevel() == 2 || group.getTreeLevel() == 3) {
			if (LicenseService.enableLicense) {
				LicenseDetail licenseDetail = LicenseService.getInstance().getLicenseDetail(user.getActiveGroup(), true);
				Group licenseGroup = groupService.get(licenseDetail.getGroupId());
				boolean isRootLicense = groupService.getRootGroup().getId().equals(licenseGroup.getId());
				if (group.getTreeLevel() == 2) {
					Long maxLevel2Groups = licenseDetail.getMaxLevel2Groups();
					if (maxLevel2Groups != null && maxLevel2Groups > 0) {
						Long countAll = groupService.countAllActiveTenants();
						if (countAll >= maxLevel2Groups) {
							throw new UserException("You cannot insert more tenant groups because you reach the limit of your license");
						}
					}
				}
				if (group.getTreeLevel() == 3) {
					Long maxLevel3Groups = licenseDetail.getMaxLevel3Groups();
					if (maxLevel3Groups != null && maxLevel3Groups > 0) {
						Long countAll;
						if (isRootLicense) {
							countAll = groupService.countAllActiveSubTenants();
						} else {
							countAll = groupService.countAllActiveSubTenants(licenseGroup.getParentLevel2());
						}
						if (countAll >= maxLevel3Groups) {
							throw new UserException("You cannot insert more sub-tenant groups because you reach the limit of your license");
						}
					}
				}
			}
		}
		super.validateInsert(group);
	}

	/***********************************************************
	 * Method to create a group with  validations
     ***********************************************************/
	public Group createGroup(Map<String, Object> params)
	{
//		User currentUser = (User) SecurityUtils.getSubject().getPrincipal();
		Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Group.class.getCanonicalName(), null);

		Group group = new Group();
		group.setName((String) params.get(getPath(qGroup.name)));
		group.setCode((String) params.get(getPath(qGroup.code)));
		group.setDescription((String) params.get(getPath(qGroup.description)));

		Object parentId = params.get(getPath(qGroup.parent.id));
		if (parentId != null) {
			Group parentGroup = GroupService.getInstance().getGroupDAO().selectById(Long.valueOf(parentId.toString()));
			if (parentGroup == null) {
				throw new NotFoundException(String.format("Parent Group[%d] not found", parentId));
			}
			if(GroupService.getInstance().isGroupNotInsideTree(parentGroup, visibilityGroup)){
				throw new ForbiddenException(String.format("Forbidden Group[%d]", parentId));
			}
			group.setParent(parentGroup);
		} else {
			throw new UserException("You need to define a Parent Group");
		}
		Object typeId = params.get(getPath(qGroup.groupType.id));
		if (typeId != null) {
			GroupType type = GroupTypeService.getInstance().getGroupTypeDAO().selectById(Long.valueOf(typeId.toString()));
			if (type == null) {
				throw new UserException(String.format("Type [%d] not found", parentId));
			}
			group.setGroupType(type);
		} else {
			throw new UserException("You need to define a Group Type");
		}
		validateInsert(group);
		insert(group);
		return group;
	}

	public List setGroupFieldsBase(Long groupId, Map<String, Object> m) {
		Group visibilityGroup = VisibilityUtils.getVisibilityGroup(Group.class.getCanonicalName(), null);
		Group group = GroupService.getInstance().get(groupId);
		if (group == null) {
			throw new NotFoundException(String.format("Group[%d] not found", groupId));
		}
		if(GroupService.getInstance().isGroupNotInsideTree(group, visibilityGroup)) {
			throw new ForbiddenException(String.format("Forbidden Group[%d]", group.getId()));
		}
		String[] permissions = new String[] {"group:i:"+group.getId(),"group:u:"+group.getId()};
		if (!PermissionsUtils.isPermittedAny(SecurityUtils.getSubject(), Arrays.asList(permissions))) {
			throw new ForbiddenException(String.format("Forbidden Group[%d]", group.getId()));
		}
		List list = new ArrayList();
		GroupFieldService instance = GroupFieldService.getInstance();
		Group rootGroup = GroupService.getInstance().getRootGroup();
		for (Map.Entry<String,Object> entry: m.entrySet()) {
			String key  = entry.getKey();
			Field field = null;
			try {
				field = FieldService.getInstance().get(Long.valueOf(key));
			} catch (Exception ex) {
				field = FieldService.getInstance().selectByGroupAndName( rootGroup, key);
			}
			if (field == null) {
				throw new NotFoundException(String.format("Field[%s] not found", key));
			}
			if (field.getEditLevel()!= null && entry.getValue() != null) {
				if (group.getTreeLevel() > field.getEditLevel()) {
					throw new ForbiddenException(String.format("Forbidden field %s, for group %s", field.getName(), visibilityGroup.getName()));
				}
			}
			if (!LicenseService.getInstance().isValidField(group, field.getName())) {
				throw new ForbiddenException(String.format("Forbidden GroupField[%d]", field.getId()));
			}
			Object value = entry.getValue();
            if (field.getName().equals(Constants.TIME_ZONE_CONFIG)) {
                if (Objects.equals(rootGroup.getId(), group.getId()) && (value == null || isEmptyOrNull(value.toString()))) {
                    throw new ForbiddenException(String.format("GroupField[%d], " + field.getDescription() + " should not be empty", field.getId()));
                }
                if (value != null && !isEmptyOrNull(value.toString()) && !Utilities.timeZoneIsValid(String.valueOf(value))) {
                    throw new UserException("Invalid timezone [" + String.valueOf(value) + "]");
                }
			}
			if (field.getName().equals(Constants.GROUP_PAGE_SIZE)) {
				if (Objects.equals(rootGroup.getId(), group.getId()) && (value == null || isEmptyOrNull(value.toString()))) {
					throw new ForbiddenException(String.format("GroupField[%d], " + field.getDescription() + " should not be empty", field.getId()));
				}
				if (value != null && !isEmptyOrNull(value.toString())) {
					String pageSizeValue = String.valueOf(value);
					if (!Utilities.isNumber(pageSizeValue)) {
						throw new UserException("Invalid 'Page size'.");
					}
					BigDecimal valueInteger = new BigDecimal(pageSizeValue);
					if (valueInteger.compareTo(BigDecimal.ONE) < 0 || valueInteger.compareTo(BigDecimal.valueOf(10000)) > 0) {
						throw new UserException("Page Size must be between 1 and 10000.");
					}
				}
			}
			if (field.getName().equals(Constants.DATE_FORMAT_CONFIG)) {
                if (Objects.equals(rootGroup.getId(), group.getId()) && (value == null || isEmptyOrNull(value.toString()))) {
                    throw new ForbiddenException(String.format("GroupField[%d], " + field.getDescription() + " should not be empty", field.getId()));
                }
                if (value != null && !isEmptyOrNull(value.toString()) &&
                        !Utilities.isAlphaNumericCharacterSpecials(value.toString(), GroupService.CHARACTER_SPECIALS_DATE_FORMAT)) {
                    throw new UserException("Date format has invalid characters, only alphanumeric and character _-&/.:,\"[]()\\  are allowed.");
                }
			}
			if (Constants.EMAIL_CONFIGURATION_ERROR.equals(field.getName()) || Constants.EMAIL_SMTP_USER.equals(field.getName())) {
				if (Objects.equals(rootGroup.getId(), group.getId()) && (value == null || isEmptyOrNull(value.toString()))) {
					throw new ForbiddenException(String.format("GroupField[%1d], %2s should not be empty", field.getId(), field.getDescription()));
				}
				if (value != null && !isEmptyOrNull(value.toString()) && !EmailValidator.getInstance().isValid(value.toString())) {
					if (Constants.EMAIL_CONFIGURATION_ERROR.equals(field.getName())) {
						throw new UserException("Please, insert a valid email in Email Configuration Error field.");
					}
					throw new UserException("Please, insert a valid email in User field.");
				}
			}
			if (value != null) {
                List<String> licenseFeatures = LicenseService.getInstance().getLicenseDetail(rootGroup, false).getFeatures();
                if (group.equals(rootGroup) &&
                        field.getName().equals(AuthenticationUtils.AUTHENTICATION_MODE) &&
                        (value.toString().equals(AuthenticationUtils.LDAP_AD_AUTHENTICATION)) &&
                        !licenseFeatures.contains(LicenseDetail.LDAP_AUTH_ROOT_GROUP)) {
                    throw new NotFoundException("Not enough permissions to apply LDAP Auth to 'root' group");
                }
                if (group.equals(rootGroup) && value.toString().equals(AuthenticationUtils.OAUTH2_AUTHENTICATION)){
					BooleanBuilder be = new BooleanBuilder();
					be = be.and(QGroupField.groupField.value.eq(AuthenticationUtils.LDAP_AD_AUTHENTICATION)).
							or(QGroupField.groupField.value.eq(AuthenticationUtils.NATIVE_AUTHENTICATION));
					be = be.and(QGroupField.groupField.group.ne(rootGroup));
					List<GroupField> groupFields = instance.listPaginated(be, null, null);
					for (GroupField groupField: groupFields){
						instance.delete(groupField);
					}
				}
				GroupField groupField = instance.set(group, field, value.toString());
				list.add(groupField.publicMap(true));
				if (field.getName().equals("language")){
					try {
						Class clazz = Class.forName("com.tierconnect.riot.iot.services.ReportDefinitionService");
						clazz.getMethod("updateAllReports").invoke(clazz.newInstance());
					}catch (Exception e){
						logger.info("Could not update Reports order ", e);
					}
				}
			} else {
				instance.unset(group, field);
			}

			//TODO, XXX, KLUDGE, KLUGE, HACK:  Change this code to another class or method.
			//FIXME: The method is invoked via reflection by poor architecture. 30/06/2017.
			//This code was copied form previeous method
			if("shiftZoneValidation".equals(field.getName())){
				logger.debug("updating Shift re-validation job schedule");
				try {
					Class clazz =   Class.forName("com.tierconnect.riot.iot.job.ShiftZoneRevalidationJob");
					clazz.getMethod("reschedule").invoke(null);
				} catch (Exception e) {
					logger.info("Could not update Shift re-validation job schedule: ", e);
				}
			}

			//TODO, XXX, KLUDGE, KLUGE, HACK:  Change this code to another class or method.
			//FIXME: The method is invoked via reflection by poor architecture. 30/06/2017.
			if("shiftZoneValidationEnabled".equals( field.getName() )){
				logger.debug("enable/disable Shift re-validation job schedule");
				try {
					Class clazz = Class.forName("com.tierconnect.riot.iot.job.ShiftZoneRevalidationJob");
					if (value != null && Boolean.FALSE.equals(Boolean.parseBoolean(value.toString())) )
						clazz.getMethod("stop").invoke(null);
					else
					{
						boolean checkExistJob = (Boolean) clazz.getMethod("checkExistJob").invoke(null);
						if (!checkExistJob)
							clazz.getMethod("reschedule").invoke(null); // enabled by default
					}
				} catch (Exception e) {
					logger.info("Could not enable/disable Shift re-validation job schedule: ", e);
				}
			}

			//TODO, XXX, KLUDGE, KLUGE, HACK:  Change this code to another class or method.
			//FIXME: The method is invoked via reflection by poor architecture. 30/06/2017.
			if (field != null && field.getName() != null && INDEX_STATISTIC_SCHEDULE.equals(field.getName())) {
				logger.debug("restarting IndexStats job schedule");
				try {
					Class clazz = Class.forName("com.tierconnect.riot.iot.job.GetIndexInformationJob");
					clazz.getMethod("reschedule", String.class, String.class).invoke(null, value.toString(),
									ConfigurationService.getAsString(
											UserService.getInstance().getRootUser(), TIME_ZONE_CONFIG));
				} catch (Exception ex) {
					logger.info("Could not restart IndexStats job schedule: ", ex);
				}
			}

			//TODO, XXX, KLUDGE, KLUGE, HACK:  Change this code to another class or method.
			//FIXME: The method is invoked via reflection by poor architecture. 30/06/2017.
			if (field != null && field.getName() != null && INDEX_CLEANUP_SCHEDULE.equals(field.getName())) {
				logger.debug("restarting auto delete indexes job schedule");
				try {
					Class clazz = Class.forName("com.tierconnect.riot.iot.job.AutoDeletionIndexesJob");
					clazz.getMethod("reschedule", String.class, String.class).invoke(null, value.toString(),
							ConfigurationService.getAsString(UserService.getInstance().getRootUser(), TIME_ZONE_CONFIG));
				} catch (Exception ex) {
					logger.info("Could not restart auto delete indexes job schedule: ", ex);
				}
			}

			// Check if fileSystemPath has the correct value
			if(field.getName().equals( "fileSystemPath" ) && !field.getName().trim().equals( "" ))
			{
				if(value!=null && !value.toString().equals( "" ))
				{
					try {
						Map<String, Object> validatePath = GroupService.getInstance().validatePathFile( value.toString() );
						if(validatePath!=null && !validatePath.get( "errorCode" ).toString().equals( "OK" ) )
						{
							throw new UserException( validatePath.get( "errorMessage" ).toString() );
						}
					} catch (Exception e) {
						e.printStackTrace();
						throw new UserException( e.getMessage(), e );
					}
				}
			}
			if(field.getName().equals("reportLogIndexRunNumberMin")) {
				if (Utilities.isNotEmptyOrNull(value) && !Utilities.isLongNumber(value.toString())){
					throw new UserException("Please, insert a valid number in 'Indexing: Minimum number of executions before an Index is deleted.' field.");
				}
			}
			if(field.getName().equals("reportLogMinimumNumberDays")) {
				if (Utilities.isNotEmptyOrNull(value) && !Utilities.isLongNumber(value.toString())){
					throw new UserException("Please, insert a valid number in 'Indexing: Minimum number of days to analyze an index.' field.");
				}
			}

			//TODO, XXX, KLUDGE, KLUGE, HACK:  Change this code to another class or method.
			//FIXME: The method is invoked via reflection by poor architecture. 30/06/2017.
            if (Constants.BRIDGE_ERROR_STATUS_TIMEOUT.equals(field.getName()) && !field.getName().trim().equals("")){
                if (value != null && !value.toString().isEmpty()){
                    try {
                        Class clazz = Class.forName("com.tierconnect.riot.iot.services.StatusService");
                        Object ssInstance = clazz.getMethod("getInstance").invoke(null);
                        ssInstance.getClass().getMethod("setBridgeErrorStatusTimeout", Long.class).invoke(ssInstance, Long.parseLong(value.toString()));
                    } catch (Exception e) {
                        logger.info("Could not enable/disable Bridge Error Status Timeout: ", e);
                    }
                }
            }
		}

		//TODO, XXX, KLUDGE, KLUGE, HACK:  Change this code to another class or method.
		//FIXME: The method is invoked via reflection by poor architecture. 30/06/2017.
		// Sending message to refresh coreBridge
		try
		{
			Class clazz = Class.forName("com.tierconnect.riot.iot.services.BrokerClientHelper");
			clazz.getMethod("sendRefreshGroupConfiguration", Boolean.class, String.class, List.class)
					.invoke(null, false, Thread.currentThread().getName(), GroupService.getInstance().getMqttGroups(group));
		}
		catch (Exception e) {
			logger.error("Could call MQTT sendRefreshGroupConfiguration method");
		}

		return list;
	}

	/**
	 *
	 * @param name group name
     * @return List of group names
     */
	public List<Group> getGroupsByNameLike(String name) {
		HibernateQuery query = getGroupDAO().getQuery();
		BooleanBuilder groupWhereQuery = new BooleanBuilder(QGroup.group.name.toLowerCase().like( "%"+name.toLowerCase()+"%" ));
		return query.where(groupWhereQuery).list(QGroup.group);
	}

	/**
	 * Select all groups by level
	 * @param level
	 * @return
	 * @throws NonUniqueResultException
     */
	public List<Group> getByLevel(int level) throws NonUniqueResultException
	{
		List<Group> lstGroup = new ArrayList<>();
		BooleanBuilder b = new BooleanBuilder(qGroup.treeLevel.eq(level));
		lstGroup = getGroupDAO().selectAll(b, null, null);
		return lstGroup;
	}

    public Map<String, String> getRegionalSettings() {
        Map<String, String> mapRegionalTimeZone = new LinkedHashMap<>();
        //Get all ZoneIds
        Map<String, String> allZoneIds = getAllZoneIds(zoneList);
        //sort map by key
        allZoneIds.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(e -> mapRegionalTimeZone.put(e.getKey(),
                        String.format("%1s ( UTC%2s )",
                                StringUtils.replace(e.getKey(), "_", " "),
                                e.getValue())));
        return mapRegionalTimeZone;
    }

	private Map<String, String> getAllZoneIds(List<String> zoneList) {
		Map<String, String> result = new HashMap<>();
		LocalDateTime dt = LocalDateTime.now();
		for (String zoneId : zoneList) {
			ZoneId zone = ZoneId.of(zoneId);
			ZonedDateTime zdt = dt.atZone(zone);
			ZoneOffset zos = zdt.getOffset();
			//replace Z to +00:00
			String offset = zos.getId().replaceAll("Z", "+00:00");
			result.put(zone.toString(), offset);
		}
		return result;
	}

	public DateFormatAndTimeZone getDateFormatAndTimeZone(Group group) {
		String timeZone = GroupFieldService.getInstance().getGroupField(group, Constants.TIME_ZONE_CONFIG);
		String dateFormat = GroupFieldService.getInstance().getGroupField(group, Constants.DATE_FORMAT_CONFIG);
		if (Utilities.isEmptyOrNull(timeZone)) {
			timeZone = Constants.DEFAULT_TIME_ZONE;
		}
		if (Utilities.isEmptyOrNull(dateFormat)) {
			dateFormat= Constants.DEFAULT_DATE_FORMAT;
		}
		return new DateFormatAndTimeZone(timeZone, dateFormat);
	}

	/**
	 * This method create a topic for a group if it is root level
	 * @param hierarchyName
	 */
	/*private void createTopic(final String hierarchyName){
		boolean kafkaEnabled = Configuration.getProperty("kafka.enabled")==null?false:Boolean.parseBoolean(Configuration.getProperty("kafka.enabled"));
		if (kafkaEnabled){
			final String kafkaConnCode = "KAFKA";
			final String ZOOKEEPER_SERVER_PROPERTY = "zookeeper";
			Connection connection = ConnectionService.getInstance().getByCode(kafkaConnCode);
			if (connection!=null){
				String zookeeper = (String) connection.getProperty(ZOOKEEPER_SERVER_PROPERTY);
				KafkaZkUtils kafkaZkUtils = new KafkaZkUtils(zookeeper);
				String tenantCode = TenantUtil.getTenantCode(hierarchyName);
				String topic = String.format("___v1___data2___%s",tenantCode);
				if (!kafkaZkUtils.existTopic(topic)){
					String topicData1=StringUtils.replace(Topics.DATA_EDGE.getName(),"/","___");
					int partitions = kafkaZkUtils.getNumberOfPartitions(topicData1);
					kafkaZkUtils.createOrAddPartitionReplicas(topic, partitions, DEFAULT_DATA_TOPIC_NUMBER_REPLICAS);
					logger.info(String.format("Topic created=['%s'], noPartition=['%d']",topic,partitions));
				}else {
					logger.warn(String.format("Cannot create topic=['%s'] because it already exists.",topic));
				}
			}
		}
	}
	*/

	/**
	 * Get group Mqtt of the current Group*
	 * @return List of Group ID from Group ID Parent Level
	 * - If this is of Level 2, the same Group Id is returned
	 * - If this has parent level2, its parent is returned
	 * - If this has children level 2 , its children are returned
	 */
	public List<Long> getMqttGroups(Group group){
		List<Long> groupIds = new ArrayList<>();
		if(group.getTreeLevel() == Constants.PARENT_GROUP_LEVEL_MQTT) {
			groupIds.add(group.getId());
		} else {
			Group parentLevel2 = group.getParentLevel(Constants.PARENT_GROUP_LEVEL_MQTT);
			if(parentLevel2 != null) {
				groupIds.add(parentLevel2.getId());
			} else {
				BooleanBuilder groupBe = new BooleanBuilder();
				groupBe = groupBe.and(
						GroupService.getInstance().getDescendantsIncludingPredicate( QGroup.group, group ) );
				List<Group> listGroups = GroupService.getInstance().getGroupDAO().selectAllBy( groupBe );
				if( (listGroups != null) && (!listGroups .isEmpty())) {
					for(Group groupData : listGroups ) {
						groupIds.add(groupData.getId());
					}
				}
			}
		}
		return groupIds;
	}
	public List<Long> getGroupAndDescendantsIds(Group group){
		BooleanBuilder groupBe = new BooleanBuilder();
		groupBe = groupBe.and(getDescendantsIncludingPredicate(QGroup.group, group));
		HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
		return query.from(getGroupDAO().getEntityPathBase())
				.where(groupBe)
				.setCacheable(true)
				.list(QGroup.group.id);
	}

	private List<Group> getAllGroups() {
		return  getGroupDAO().selectAll();
	}

	public Group getByHierarchyNameAttribute(String hierarchyName) {
		HibernateQuery query = new HibernateQuery(HibernateSessionFactory.getInstance().getCurrentSession());
		return query.from(QGroup.group)
				.where(QGroup.group.hierarchyName.eq(hierarchyName))
				.setCacheable(true)
				.uniqueResult(QGroup.group);
	}

	public void loadAllInCache() {
		boolean cacheCreated = true;
		boolean thingTypeCacheExists = CacheBoundary.getInstance().cacheExists(GROUP_CACHE_NAME);
		if (!thingTypeCacheExists) {
			cacheCreated = CacheBoundary.getInstance().createCache(GROUP_CACHE_NAME, String.class, Group.class);
		}
		if (cacheCreated) {
			List<Group> allGroups = getAllGroups();
			allGroups.forEach(g -> putOneInCache(g));
		} else {
			logger.error("Could not create cache");
		}
	}

	public void putOneInCache(Group group) {
		Hibernate.initialize(group.getGroupType());
		Hibernate.initialize(group.getParentLevel1());
		Hibernate.initialize(group.getParentLevel2());
		Hibernate.initialize(group.getParentLevel3());
        Hibernate.initialize(group);
		CacheBoundary.getInstance().put(GROUP_CACHE_NAME,
				group.getHierarchyName(), group,
				String.class, Group.class);
	}

    public Group getFromCache(String hierarchyName) {
		Group group = CacheBoundary.getInstance().get(GROUP_CACHE_NAME, hierarchyName, String.class, Group.class);
        return (group == null && !containsKey(hierarchyName))? lazyLoad(hierarchyName) : group;
    }

    public void removeOneFromCache(String hierarchyName){
	    CacheBoundary.getInstance().remove(GROUP_CACHE_NAME, hierarchyName, String.class, Group.class);
    }

    private Group lazyLoad(String hierarchyName){
        Group group = getByHierarchyNameAttribute(hierarchyName);
        if(group != null){
            putOneInCache(group);
        }
        return group;
    }

    private boolean containsKey(String hierarchyName) {
        return CacheBoundary.getInstance()
                .containsKey(GROUP_CACHE_NAME, hierarchyName, String.class, Group.class);
    }
}
