package com.tierconnect.riot.iot.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.path.EntityPathBase;
import com.mysema.query.types.path.SetPath;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QGroup;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupFieldService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.RecentService;
import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.GeneralVisibilityUtils;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.commons.utils.CoordinateUtils;
import com.tierconnect.riot.commons.utils.TenantUtil;
import com.tierconnect.riot.iot.controllers.ZoneController;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.dao.util.ElExpressionService;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.exceptions.InvalidVideoFeedException;
import com.tierconnect.riot.iot.services.exceptions.UserIsNotOperatorException;
import com.tierconnect.riot.iot.services.exceptions.ZoneHasNoCameraException;
import com.tierconnect.riot.iot.utils.ValidationUtils;
import com.tierconnect.riot.iot.utils.rest.RestCallException;
import com.tierconnect.riot.iot.utils.rest.RestClient;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.Pagination;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;

import javax.el.PropertyNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.*;

public class ZoneService extends ZoneServiceBase
{
    static Logger logger = Logger.getLogger( ZoneService.class );
    @Override
    public void validateInsert(Zone zone) {
        validateZoneCode(zone);
        validateName( zone );
        super.validateInsert( zone );
    }

    @Override
    public void validateUpdate(Zone zone) {
        validateZoneCode( zone );
        validateName( zone );
        super.validateUpdate(zone);
    }

    //ZoneCode is unique on the platform
    public static void validateZoneCode(Zone zone) {
        if (StringUtils.isEmpty(zone.getCode())) {
            return;
        }

        // validating unknown zone
        if ("unknown".equals(zone.getCode().toLowerCase())){
            throw new UserException("You cannot use '" + zone.getCode() + "' as a zone code. Please change it and try again.");
        }

        //Validate Zone Code with facilityMaps
        BooleanBuilder b = new BooleanBuilder();
        b = b.and(QZone.zone.code.eq(zone.getCode()));
        b = b.and(QZone.zone.group.code.eq(zone.getGroup().getCode()));
        List<Zone> others = getZoneDAO().selectAllBy(b);
        if (others.size() >= 2) {
            throw new UserException( String.format( "Zone with code [%s] already exists.", zone.getCode() ) );
        } else if (zone.getId() == null && others.size() > 0) {
            throw new UserException( String.format( "Zone with code [%s] already exists.", zone.getCode() ) );
        } else if (!(zone.getId() == null) && others.size() > 0 && !zone.getId().equals(others.get(0).getId())) {
            throw new UserException( String.format( "Zone with code [%s] already exists.", zone.getCode() ) );
        }

    }

    /************************
     * Method to get siblings by group
     ************************/
    public static Predicate getSiblingsByGroup(Zone zone)
    {
        BooleanBuilder b = new BooleanBuilder();
        QZone qZone = QZone.zone;
        Group group = zone.getGroup();
        if (group == null) {
            b = b.and(qZone.group.isNull() );
        } else {
            b = b.and(qZone.group.id.eq(group.getId()));
        }
        return b;
    }

    /************************
     * Validate Name
     ************************/
    public static void validateName(Zone zone) {
        if (StringUtils.isEmpty(zone.getName())) {
            return;
        }

        // validating unknown zone
        if ("unknown".equals(zone.getName().toLowerCase())){
            throw new UserException("You cannot use '" + zone.getName() + "' as a zone name. Please change it and try again.");
        }

        //Validate Zone Code with facilityMaps
        BooleanBuilder b = new BooleanBuilder();
        b=b.and(getSiblingsByNameAndFacilityCode( zone ));
        b=b.and(QZone.zone.name.eq(zone.getName()));
        List<Zone> others = getZoneDAO().selectAllBy(b);
        if (others.size() >= 2) {
            throw new UserException( String.format( "Zone with name [%s] already exists.", zone.getName() ) );
        } else if (zone.getId() == null && others.size() > 0) {
            throw new UserException( String.format( "Zone with name [%s] already exists.", zone.getName() ) );
        } else if (!(zone.getId() == null) && others.size() > 0 && !zone.getId().equals(others.get(0).getId())) {
            throw new UserException( String.format( "Zone with name [%s] already exists.", zone.getName() ) );
        }

    }

    /************************
     * Method to get siblings by name and facilityCode
     ************************/
    public static Predicate getSiblingsByNameAndFacilityCode(Zone zone)
    {
        BooleanBuilder b = new BooleanBuilder();
        QZone qZone = QZone.zone;
        Group group = zone.getGroup();
        LocalMap localMap = zone.getLocalMap();
        if (group == null) {
            b = b.and(qZone.group.isNull() );
        } else {
            b = b.and(qZone.group.id.eq(group.getId()));
        }
        if (localMap == null) {
            b = b.and( qZone.localMap.isNull() );
        } else {
            b = b.and(qZone.localMap.id.eq(localMap.getId()));
        }
        return b;
    }

    // notify bridges there has been a zone update
	public void refreshConfiguration(boolean publishMessage, List<Long> groupMqtt){
		BrokerClientHelper.sendRefreshZonesMessage(publishMessage, groupMqtt);
	}

	public void delete( Zone zone ) {
        List<Long> groupMqtt = GroupService.getInstance().getMqttGroups(zone.getGroup());
        List<ZonePropertyValue> lstZonePropertyValue = ZonePropertyValueService.getInstance().getZonePropertyValueByZoneId(zone.getId());
        if( (lstZonePropertyValue!=null) && (!lstZonePropertyValue.isEmpty()) ) {
            Set<Long> ids = new HashSet<>();
            for(ZonePropertyValue zonePropertyValue : lstZonePropertyValue) {
                ids.add(zonePropertyValue.getId());
            }
            for(Long id : ids) {
                ZonePropertyValueService.getInstance().delete(ZonePropertyValueService.getInstance().get(id));
            }
        }
        zone.setZoneType(null);
		super.delete( zone );
    // RIOT-13659 send tickle for zones.
		refreshConfiguration(false, groupMqtt);

        refreshCache(zone,true);
	}

	public void deleteAllZones()
	{
		for( Zone zone : ZoneService.getZoneDAO().selectAll() )
		{
			delete( zone );
		}

    // RIOT-13659 send tickle for zones.
		refreshConfiguration(false, null);
	}

	public void updateZonePoints( Long zoneId, List<List<Double>> points )
	{
		Zone zone = new Zone();
		zone.setGroup( GroupService.getInstance().get( 1L ) );
		ZoneService.getInstance().insert( zone );
		updateZonePoints(zone, points);
	}

	public void updateZonePoints( Zone zone, List<List<Double>> points )
	{

		Set<ZonePoint> oldZonePoints = zone.getZonePoints();
		if( oldZonePoints != null )
		{
			Iterator<ZonePoint> iterator = oldZonePoints.iterator();
			while( iterator.hasNext() )
			{
				ZonePoint zonePoint = iterator.next();
				// zonePoint.setZone(null);
				// ZonePointService.getInstance().update(zonePoint);
				iterator.remove();
				ZonePointService.getInstance().delete( zonePoint );
			}
		}

		zone.setZonePoints( null );

		Set<ZonePoint> zonePoints = new LinkedHashSet<>();
		long position = 0;
		for( List<Double> point : points )
		{
			ZonePoint zonePoint = new ZonePoint();
			zonePoint.setX( point.get( 0 ) );
			zonePoint.setY( point.get( 1 ) );
			if ((point.size() > 2) && (point.get(2) != null))
			{
				zonePoint.setArrayIndex( point.get( 2 ).longValue() );
			}
			else
			{
				zonePoint.setArrayIndex( position++ );
			}
			zonePoint.setZone( zone );
			ZonePointService.getInstance().insert( zonePoint );
			zonePoints.add( zonePoint );
		}
		zone.setZonePoints(zonePoints);
        ZoneService.getInstance().update(zone);


    // RIOT-13659 send tickle for zones.
		refreshConfiguration(false, GroupService.getInstance().getMqttGroups(zone.getGroup()));
        refreshCache(zone,false);
	}


    /**
     * Gets the map of zone properties.
     *
     * @return the map of zone properties
     * @throws Exception the exception
     */
    private Map<Long, Map<String, Object>> getZoneProperty(Zone zone)
            throws Exception {
        Map<Long, Map<String, Object>> zonePropertiesMap = new HashMap<>();

        Map<String, Object> properties = new HashMap<>();
            List<ZoneProperty>zoneProperties = ZonePropertyService.getInstance().getbyZoneType(zone.getZoneType());
            logger.info(zoneProperties);
            for (ZoneProperty zoneProperty : zoneProperties) {
                String propName = zoneProperty.getName();
                Object propValue = ZonePropertyValueService.getInstance().getZonePropertyValue(zone.getId(),zoneProperty.getId());
                if (propValue != null) {
                    if (BooleanUtils.toBooleanObject(propValue.toString()) != null) {
                        properties.put(propName, Boolean.valueOf(propValue.toString()));
                    } else {
                        properties.put(propName, propValue);
                    }
                } else {
                    properties.put(propName, propValue);
                }
        }
        zonePropertiesMap.put(zone.getId(), properties);

        return zonePropertiesMap;
    }

    /**
     *
     * This method update zoneCache using kafka publish
     * @param zone
     * @param delete
     */
    public void refreshCache(Zone zone,boolean delete){
        try {
			Map<Long, Map<String, Object>> properties = getZoneProperty( zone );
			BrokerClientHelper.refreshZoneCache( zone, properties, delete );
        } catch (Exception e) {
            logger.error("Cannot update zoneCache="+zone,e);
        }

    }

	public static List<Zone> getZonesByLocalMap( Long id )
	{
		HibernateQuery query = ZoneService.getZoneDAO().getQuery();
		return query.where( QZone.zone.localMap.id.eq( id ) ).list( QZone.zone );
	}

	public static List<Zone> getZonesByName( String name )
	{
		HibernateQuery query = ZoneService.getZoneDAO().getQuery();
		return query.where( QZone.zone.name.eq( name ) ).list( QZone.zone );
	}

    public List<Zone> getZonesByCode( String code )
    {
        HibernateQuery query = ZoneService.getZoneDAO().getQuery();
        return query.where( QZone.zone.code.eq( code ) ).list( QZone.zone );
    }


    public List<Zone> getZonesByCodeLike( String code )
    {
        HibernateQuery query = ZoneService.getZoneDAO().getQuery();
        return query.where( QZone.zone.code.toLowerCase().like(code.toLowerCase()) ).list( QZone.zone );
    }

	public static List<Zone> getZonesByZoneTypeId( Long zoneTypeId, Long zonePropertyId , String propertyValue)
	{
		HibernateQuery query = ZoneService.getZoneDAO().getQuery();
		List<Zone> zones = query.where( QZone.zone.zoneType.id.eq( zoneTypeId ) ).list( QZone.zone );

		List<Zone> newZones = new LinkedList<>();
		for( Zone zone : zones )
		{
			String value = ZonePropertyValueService.getInstance().getZonePropertyValue( zone.getId(), zonePropertyId );
			if( value != null && value.toLowerCase().equals( propertyValue.toLowerCase() ) )
			{
				newZones.add( zone );
			}
		}
		return newZones;
	}

    //Get Zones by list of zoneProperties
    public List<Zone> getZoneByZonePropertyList( Long zoneTypeId, List<ZoneProperty> zonePropertyListId )
    {
        HibernateQuery query = ZoneService.getZoneDAO().getQuery();
        List<Zone> zones = query.where( QZone.zone.zoneType.id.eq( zoneTypeId ) ).list( QZone.zone );

        List<Zone> newZones = new LinkedList<>();
        for( Zone zone : zones )
        {
            for(ZoneProperty zoneProperty : zonePropertyListId) {
                Long zonePropertyId = zoneProperty.getId();
                String value = ZonePropertyValueService.getInstance().getZonePropertyValue(zone.getId(), zonePropertyId);
                if (value != null && value.toLowerCase().equals("true")) {
                    newZones.add(zone);
                }
            }
        }
        return newZones;
    }

    public List<Zone> getZoneByZoneType( Long zoneTypeId )
    {
        HibernateQuery query = ZoneService.getZoneDAO().getQuery();
        List<Zone> zones = query.where( QZone.zone.zoneType.id.eq( zoneTypeId ) ).list( QZone.zone );

        List<Zone> newZones = new LinkedList<>();
        for( Zone zone : zones )
        {
            if(zone.getZoneType() != null && zone.getZoneType().getId().equals( zoneTypeId ) ) {
                newZones.add(zone);
            }
        }
        return newZones;
    }

    public List<Zone> getZones() {
        Session session = getZoneDAO().getSession();
        List<Zone> zoneList = session.createCriteria(Zone.class)
                .setFetchMode("zoneGroup", FetchMode.EAGER)
                .setFetchMode("zoneType", FetchMode.EAGER)
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                .list();
        return zoneList;
    }

    public List<Zone> getZones(Group group) {
        List<Zone> zones = getZoneDAO().getQuery().list(QZone.zone);
        List<Zone> zoneList = new LinkedList<>();
        for(Zone zone : zones) {
            if(zone.getGroup().getId() > group.getId()) {
                zoneList.add( zone );
            }
        }
        return zoneList;
    }

    public List<Zone> getZones(List<String> zoneNamesOrCode) {
        Session session = getZoneDAO().getSession();
        if(zoneNamesOrCode != null && zoneNamesOrCode.size() > 0) {
            List<Zone> zoneList = session.createCriteria(Zone.class)
                    .setFetchMode("zoneGroup", FetchMode.EAGER)
                    .setFetchMode("zoneType", FetchMode.EAGER)
                    .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY)
                    .add(Restrictions.or(
                            Restrictions.in("name", zoneNamesOrCode),
                            Restrictions.in("code", zoneNamesOrCode)
                    ))
                    .list();
            return zoneList;
        }return new LinkedList<>();
    }

    public Map<Long, Zone> getZonesMap(List<CompositeThing> things, Map<Long, Map<String, Object> > fieldValues) {
        Set<String> zoneNameOrCode = new HashSet<>();

        for(CompositeThing thing : things) {
            ThingTypeField zoneField = thing.getThingTypeField("zone");
            if(zoneField == null) {
                List<ThingTypeField> zoneThingFields = thing.getThingFieldByType(ThingTypeField.Type.TYPE_ZONE.value);
                if(zoneThingFields != null && zoneThingFields.size() > 0) {
                    zoneField = zoneThingFields.get(0);
                }
            }
            if(zoneField != null) {
                if (fieldValues.containsKey(zoneField.getId())) {
                    String value = fieldValues.get(zoneField.getId()).get("value") != null ?
                            fieldValues.get(zoneField.getId()).get("value").toString()
                            : "";
                    if(value != null && value.length() > 0) {
                        zoneNameOrCode.add(value);
                    }
                }
            }
        }
        List<Zone> zoneList = getZones(new LinkedList<>(zoneNameOrCode) );

        Map<Long, Zone> zoneListMap = new HashMap<>();
        for(Zone zone : zoneList) {
            zoneListMap.put(zone.getId(), zone);
        }
        return zoneListMap;
    }

    public Map<Long, Zone> getZonesMap() {
        List<Zone> zoneList = getZones();
        Map<Long, Zone> zoneListMap = new HashMap<>();
        for(Zone zone : zoneList) {
            zoneListMap.put(zone.getId(), zone);
        }return zoneListMap;
    }

    public ZoneProperty getZonePropertyByName(Zone zone, String name){
        List<ZoneProperty> zoneProperty = ZonePropertyService.getZonePropertyDAO().getQuery().where(
                QZoneProperty.zoneProperty.zoneType.id.eq(zone.getZoneType().getId()).and(
                        QZoneProperty.zoneProperty.name.eq(name))).list(QZoneProperty.zoneProperty);

        return zoneProperty.size()>0 ? zoneProperty.get(0) : null;
    }

    public  void assignFeedToCamera(Long id) throws ZoneHasNoCameraException,
            InvalidVideoFeedException, UnknownHostException,
            IllegalArgumentException, UserIsNotOperatorException {

        if(id == null){
            throw new IllegalArgumentException("Zone ID can not be null");
        }

        Map<String, Object> genetecObjects = new HashMap<>();
        List<Map<String, Object>> genetecObjectList = new ArrayList<>();
        Map<String, Object> response = new HashMap<>();

        Zone zone;

        try{
            //Get Zone
            zone = get(id);

            //Get camera ids from thing properties
            ZoneProperty zoneProperty = getZonePropertyByName(zone, "camera");

            String cameraProperty = "";

            if(zoneProperty == null)
                throw  new ZoneHasNoCameraException("ZoneProperty 'camera' not found in zone " + zone.getName());

            cameraProperty = ZonePropertyValueService.getInstance().
                    getZonePropertyValue(id, zoneProperty.getId());

            if(cameraProperty == null || cameraProperty.isEmpty()){
                throw  new ZoneHasNoCameraException("No camera has been configured for zone " + zone.getName());
            }

            String[] cameras = cameraProperty.split(",");

            //Get User session
            User user = (User) SecurityUtils.getSubject().getPrincipal();
            String username = user.getUsername();

            //Get thingType Workstation
            ThingType thingType = ThingTypeService.getInstance().getByCode("Workstations");
            ThingTypeField thingTypeFieldOperator = null;
            ThingTypeField thingTypeFieldWindowId = null;

            if(thingType != null){
                thingTypeFieldOperator = thingType.getThingTypeFieldByName("operator");
                thingTypeFieldWindowId = thingType.getThingTypeFieldByName("window_id");
                if (thingTypeFieldOperator == null || thingTypeFieldWindowId == null)
                    throw new UserIsNotOperatorException("Invalid Workstations configuration");
            }else
                throw new UserIsNotOperatorException("Workstations not configured");

            long thingTypeIdWindowId = thingTypeFieldWindowId.getId();

            String whereFields = thingTypeFieldOperator.getName() + "=" + username;
            List<String> fields = Arrays.asList("*");

            List<Map<String,Object>> docs = (List)ThingMongoDAO.getInstance().getThingUdfValues(null, whereFields, fields, null).get("results");

            List<Long> thingsIds = new ArrayList<>();
            for(Map<String,Object> doc : docs){
                thingsIds.add(Long.parseLong(doc.get("_id").toString()));
            }

            if(docs.size() == 0){
                throw new UserIsNotOperatorException("User " + username + " is not configured as operator" );
            }

            String windowId = ((Map)docs.get(0).get(thingTypeFieldWindowId.getName())).get("value").toString();

            if(windowId == null || windowId.equals(""))
                throw new UserIsNotOperatorException("User " + username + " has an invalid Monitor ID assigned");


            Map<String, Object> genetecObject = new HashMap<>();
            genetecObject.put("monitor_id", windowId);

            List<Map<String, Object>> cams = new ArrayList<>();

            int count = 1;
            for (String camera : cameras) {
                Map<String, Object> camMap = new HashMap<>();
                camMap.put("tile_id", count);
                camMap.put("camara_id", camera);
                cams.add(camMap);
                count++;
            }

            genetecObject.put("AssignFeed", cams);
            genetecObjectList.add(genetecObject);
            genetecObjects.put("GenetecObject", genetecObjectList);

            response.put("GenetecObjects", genetecObjects);

            //Build URI
            URIBuilder ub = new URIBuilder();
            ub.setScheme("http");

            ub.setHost(ConfigurationService.getAsString(user, "genetecServer"));
            ub.setPort(Integer.parseInt(ConfigurationService.getAsString(user, "genetecPort")));
            ub.setPath("/RiotGenetecService.svc/assignFeed");

            RestClient.ResponseHandler rh = new  RestClient.ResponseHandler (){

                @Override
                public void success(InputStream is) {
                }

                @Override
                public void error(InputStream is) {
                }
            };

            //Build Json String
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectMapper objectMapper = new ObjectMapper();

            objectMapper.writeValue(baos, response);

            String json = new String(baos.toString("UTF-8"));

            RestClient.instance().put(ub.build(), json, rh);

        } catch(UserException | NonUniqueResultException | URISyntaxException | IOException  e){
            throw new InvalidVideoFeedException(e.getMessage(), e);
        } catch (RestCallException e){
            throw  new UnknownHostException("Video server not found");
        }
    }

    public List<Zone> listPaginated( Predicate be,
                                      Pagination pagination,
                                      String orderString,
                                      List<EntityPathBase<?>> properties,
                                     SetPath<ZonePoint, QZonePoint> collectionProperty)
    {
        OrderSpecifier orderSpecifiers[] = QueryUtils.getOrderFields(QZone.zone, orderString);
        return getZoneDAO().selectAll( be, properties, collectionProperty, pagination, orderSpecifiers );
    }
    /*
	* This method gets a zone based on the name of the zone
	* */
    public Zone getByName(String name) throws NonUniqueResultException {
        try {
            return getZoneDAO().selectBy("name", name);
        }
        catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }

    /**
	* This method gets a zone based on the code of the zone
	* */
    public Zone getByCode(String code) throws NonUniqueResultException {
        try {
            return getZoneDAO().selectBy("code", code);
        }
        catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }

    public Zone getByCodeAndGroup(String zoneCode, Group group) {
        List<Long> listGroups = GroupService.getInstance().getGroupAndDescendantsIds(group);
        BooleanBuilder b = new BooleanBuilder();
        b = b.and( QZone.zone.code.eq(zoneCode) ) ;
        b = b.and( QZone.zone.group.id.in( listGroups ) );
        Zone zoneData = ZoneService.getInstance().getZoneDAO().selectBy( b );
        return zoneData;
    }

    /**
     * This method gets a zone based on the code of the zone and the group
     * */
    public Zone getByCodeAndGroup(String code, String hierarchyPathGroup) throws NonUniqueResultException {
        try {
            //Get the descendants of the group including the group
            List<Long> listGroups = null;
            Group group = GroupService.getInstance().getByHierarchyCode(hierarchyPathGroup);
            if(group !=null) {
                listGroups = GroupService.getInstance().getGroupAndDescendantsIds(group);
            }

            BooleanBuilder b = new BooleanBuilder();
            b = b.and( QZone.zone.group.id.in( listGroups ) );
            b = b.and( QZone.zone.code.eq(code) ) ;
            Zone zoneData = ZoneService.getInstance().getZoneDAO().selectBy( b );

            return zoneData;
        }
        catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }

    public Zone getByNameAndGroup(String name, String hierarchyPathGroup) throws NonUniqueResultException {
        try {
            //Get the descendants of the group including the group
            List<Group> listGroups = null;
            Group group = GroupService.getInstance().getByHierarchyCode(hierarchyPathGroup);
            if(group !=null)
            {
                BooleanBuilder groupBe = new BooleanBuilder();
                groupBe = groupBe.and(
                        GroupService.getInstance().getDescendantsIncludingPredicate( QGroup.group, group ) );
                listGroups = GroupService.getInstance().getGroupDAO().selectAllBy( groupBe );
            }

            BooleanBuilder b = new BooleanBuilder();
            b = b.and( QZone.zone.group.id.in( getListOfIds( listGroups ) ) );
            b = b.and( QZone.zone.name.eq(name) ) ;
            Zone zoneData = ZoneService.getInstance().getZoneDAO().selectBy( b );

            return zoneData;
        }
        catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }
    /**************************************
     * Method to get Zones by operator. (Used by Reports)
     **************************************/
    public List<Zone> getListZoneByPropertyAndOperator(String property, String operator, Group group, Object value)
    {
        List<Zone>  lstZone = null;
        //Get the descendants of the group including the group
        List<Group> listGroups = null;
        if(group !=null)
        {
            BooleanBuilder groupBe = new BooleanBuilder();
            QGroup qGroup = QGroup.group;
            groupBe = groupBe.and(
                    GroupService.getInstance().getDescendantsIncludingPredicate( qGroup, group ) );
            listGroups = GroupService.getInstance().getGroupDAO().selectAllBy( groupBe );
        }

        //Do the query for zones
        BooleanBuilder b = new BooleanBuilder();
        //--Choose the operation of the filter

        if(property.equals( "zone" ))
        {
            setBooleanBuilderForZone( b, operator, property,value );
        }else if(property.equals( "zone.name" ))
        {
            setBooleanBuilderForZoneName( b , operator, property,value );
        }else if(property.equals( "zoneCode.name" ))
        {
            setBooleanBuilderForZoneCodeName( b, operator, property, value );
        }else if(property.equals( "localMap.id" ))
        {
            setBooleanBuilderForLocalMap( b, operator, property, value, listGroups );
        }else if(property.equals( "zoneGroup.id" ))
        {
            setBooleanBuilderForZoneGroupId( b, operator, property, value, listGroups );
        }
        else if(property.equals( "zoneType.id" ))
        {
            setBooleanBuilderForZoneTypeId( b, operator, property, value, listGroups );
        }
        //--filter groups
        if(listGroups!=null && listGroups.size()>0)
        {
            b = b.and( QZone.zone.group.id.in( getListOfIds( listGroups ) ) );
        }
        //--Execute query
//        if(property.equals( "zone" ) && value.toString().equals( "0" ))
//        {
//            Zone zone0 = new Zone();
//            zone0.setId(0L);
//            zone0.setCode( "unknown" );
//            zone0.setName( "Unknown" );
//            lstZone.add( zone0 );
//        }else
//        {
            lstZone = ZoneService.getInstance().getZoneDAO().selectAllBy( b );
//        }
        return lstZone;
    }

    public List<Zone> getGlobalZone(BooleanBuilder b){
        b = b.and(QZone.zone.localMap.isNull());
        OrderSpecifier order[] = QueryUtils.getOrderFields(QZone.zone,"name:asc");
        return  ZoneService.getInstance().getZoneDAO().selectAll(b,null,order);

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
                if(data instanceof Zone)
                {
                    response.add( ((Zone) data).getId());
                }else if(data instanceof Group)
                {
                    response.add( ((Group) data).getId());
                }else if(data instanceof LocalMap)
                {
                    response.add( ((LocalMap) data).getId());
                }else if(data instanceof ZoneGroup)
                {
                    response.add( ((ZoneGroup) data).getId());
                }
            }
        }
        return response;
    }

    /*****************************
     * Set Boolen builder for zone
     *****************************/
    private BooleanBuilder setBooleanBuilderForZone(BooleanBuilder booleanBuilder, String operator, String property,  Object value)
    {
        BooleanBuilder b  = booleanBuilder==null?new BooleanBuilder():booleanBuilder;
        //--Choose the operation of the filter
        switch( operator )
        {
            case "=":
                b = b.and( QZone.zone.id.eq( Long.parseLong( value.toString() ) ));
                break;
            case "!=":
            {
                b = b.and( QZone.zone.id.notIn(Long.parseLong( value.toString() )) );
                break;
            }
            case "isEmpty":
                b = b.and( QZone.zone.id.eq(0L));
                break;

            case "~":
                logger.warn( "'isEmpty' will not be implemented for property=" + property + " !" );
                break;
        }
        return b;
    }

    /*****************************
     * Set Boolen builder for zone name
     *****************************/
    private BooleanBuilder setBooleanBuilderForZoneName(BooleanBuilder booleanBuilder, String operator, String property,  Object value)
    {
        BooleanBuilder b  = booleanBuilder==null?new BooleanBuilder():booleanBuilder;
        //--Choose the operation of the filter
        switch( operator )
        {
            case "=":
                b = b.and( QZone.zone.name.eq( value.toString() ) );
                break;
            case "!=":
                b = b.and( QZone.zone.name.notIn( value.toString() ));
                break;

            case "isEmpty":
                logger.warn( "'isEmpty' will not be implemented for property=" + property + " !" );
                break;

            case "~":
                b = b.and( QZone.zone.name.contains( value.toString() ));
                break;
        }
        return b;
    }
    /*****************************
     * Set Boolen builder for zone code Name
     *****************************/
    private BooleanBuilder setBooleanBuilderForZoneCodeName(BooleanBuilder booleanBuilder, String operator, String property,  Object value)
    {
        BooleanBuilder b  = booleanBuilder==null?new BooleanBuilder():booleanBuilder;
        //--Choose the operation of the filter
        switch( operator )
        {
            case "=":
                b = b.and( QZone.zone.code.eq( value.toString() ) );
                break;
            case "!=":
                b = b.and( QZone.zone.code.notIn( value.toString() ));
                break;

            case "isEmpty":
                logger.warn( "'isEmpty' will not be implemented for property=" + property + " !" );
                break;

            case "~":
                b = b.and( QZone.zone.code.contains( value.toString() ));
                break;
        }
        return b;
    }
    /*****************************
     * Set Boolen builder for zone code Name
     *****************************/
    private BooleanBuilder setBooleanBuilderForLocalMap(
            BooleanBuilder booleanBuilder,
            String operator,
            String property,
            Object value,
            List<Group> listGroups)
    {
        BooleanBuilder b  = booleanBuilder==null?new BooleanBuilder():booleanBuilder;
        //--Choose the operation of the filter
        switch( operator )
        {
            case "=":
                b = b.and( QZone.zone.localMap.id.eq( Long.parseLong( value.toString() ) ) );
                break;
            case "!=":
            {
                b = b.and( QZone.zone.localMap.id.notIn(Long.parseLong( value.toString() )) );
                break;
            }
            case "isEmpty":
                //logger.warn( "'isEmpty' will not be implemented for property=" + property + " !" );
                b = b.and( QZone.zone.localMap.isNull());
                break;

            case "~":
                //Search LocalMap
                BooleanBuilder localMapBe = new BooleanBuilder();
                localMapBe = localMapBe.and( QLocalMap.localMap.name.contains( value.toString() ) );
                if(listGroups!=null && listGroups.size()>0)
                {
                    localMapBe = localMapBe.and( QLocalMap.localMap.group.id.in( getListOfIds( listGroups ) ) );
                }
                List<LocalMap> lstLocalMap= LocalMapService.getInstance().getLocalMapDAO().selectAllBy( localMapBe );
                //Filter Zones
                b = b.and( QZone.zone.localMap.id.in( getListOfIds( lstLocalMap ) ));
                break;
        }
        return b;
    }

    /*****************************
     * Set Boolen builder for zone code Name
     *****************************/
    private BooleanBuilder setBooleanBuilderForZoneGroupId(
            BooleanBuilder booleanBuilder,
            String operator,
            String property,
            Object value,
            List<Group> listGroups)
    {
        BooleanBuilder b  = booleanBuilder==null?new BooleanBuilder():booleanBuilder;
        //--Choose the operation of the filter
        switch( operator )
        {
            case "=":
                b = b.and( QZone.zone.zoneGroup.id.eq( Long.parseLong( value.toString() ) ) );
                break;
            case "!=":
                b = b.and( QZone.zone.zoneGroup.id.notIn( Long.parseLong( value.toString() ) ) );
                break;

            case "isEmpty":
                logger.warn( "'isEmpty' will not be implemented for property=" + property + " !" );
                break;

            case "~":
                //Search ZoneGroup
                BooleanBuilder zoneGroupBe = new BooleanBuilder();
                zoneGroupBe = zoneGroupBe.and( QZoneGroup.zoneGroup.name.contains( value.toString() ) );
                if(listGroups!=null && listGroups.size()>0)
                {
                    zoneGroupBe = zoneGroupBe.and( QZoneGroup.zoneGroup.group.id.in( getListOfIds( listGroups ) ) );
                }
                List<ZoneGroup> lstZoneGroup= ZoneGroupService.getInstance().getZoneGroupDAO().selectAllBy( zoneGroupBe );
                //Filter Zones
                b = b.and( QZone.zone.zoneGroup.id.in( getListOfIds( lstZoneGroup ) ) );
                break;
        }
        return b;
    }

    /*****************************
     * Set Boolen builder for zone type id
     *****************************/
    private BooleanBuilder setBooleanBuilderForZoneTypeId(
            BooleanBuilder booleanBuilder,
            String operator,
            String property,
            Object value,
            List<Group> listGroups)
    {
        BooleanBuilder b  = booleanBuilder==null?new BooleanBuilder():booleanBuilder;
        //--Choose the operation of the filter
        switch( operator )
        {
            case "=":
                b = b.and( QZone.zone.zoneType.id.eq( Long.parseLong( value.toString() ) ) );
                break;
            case "!=":
                b = b.and( QZone.zone.zoneType.id.notIn( Long.parseLong( value.toString() ) ) );
                break;

            case "isEmpty":
                logger.warn( "'isEmpty' will not be implemented for property=" + property + " !" );
                break;

            case "~":
                //Search ZoneType
                BooleanBuilder zoneGroupBe = new BooleanBuilder();
                zoneGroupBe = zoneGroupBe.and( QZoneType.zoneType.name.contains( value.toString() ) );
                if(listGroups!=null && listGroups.size()>0)
                {
                    zoneGroupBe = zoneGroupBe.and( QZoneType.zoneType.group.id.in( getListOfIds( listGroups ) ) );
                }
                List<ZoneType> list= ZoneTypeService.getInstance().getZoneTypeDAO().selectAllBy( zoneGroupBe );
                //Filter Zones
                b = b.and( QZone.zone.zoneType.id.in( getListOfIds( list ) ) );
                break;
        }
        return b;
    }

    //Method to update a Zone
    public Map<String, Object>  updateZone(Long id, Map<String, Object> map , EntityVisibility entityVisibility)
    {
        Map<String, Object> response = null;
        Zone zone = ZoneService.getInstance().get( id );
        if( zone == null )
        {
            throw new UserException( String.format( "ZoneId[%d] not found", id ) );
        }
        List<Map<String, Object>> zonePoints = (List<Map<String, Object>>) map.get( "zonePoints" );
        map.remove( "zonePoints" );

        GeneralVisibilityUtils.limitVisibilityUpdate( entityVisibility, zone, VisibilityUtils.getObjectGroup( map ) );

        BeanUtils.setProperties( map, zone );
        validateUpdate( zone );
        List<List<Double>> points = new LinkedList<>();
        for( Map<String, Object> zonePoint : zonePoints )
        {
            List<Double> point = new LinkedList<>();
            point.add( new Double( zonePoint.get( "x" ).toString() ) );
            point.add( new Double( zonePoint.get( "y" ).toString() ) );
            if(zonePoint.get( "arrayIndex" ) != null) {
                point.add(new Double(zonePoint.get("arrayIndex").toString()));
            }else {
                point.add(new Double(points.size()));
            }
            points.add( point );
        }

        if(map.containsKey("zoneType")) {
            ZoneTypeService.updatingZoneTypeFromZone(map.get("zoneType"), zone);
        }
        RecentService.getInstance().updateName(zone.getId(), zone.getName(),"zone");

        ZoneService.getInstance().updateZonePoints( zone, points );
        response = zone.publicMap();
        return response;

    }

    /**
     *
     * @param name  zone name
     * @return list of zones
     */
    public List<Zone> getZonesByNameLike(String name) {
        HibernateQuery query = getZoneDAO().getQuery();
        BooleanBuilder zoneWhereQuery = new BooleanBuilder(QZone.zone.name.toLowerCase().like( "%"+name.toLowerCase()+"%" ));
        return query.where(zoneWhereQuery).list(QZone.zone);
    }

    public List<Map<String, Object>> fillChildren(Map map, BooleanBuilder mapBe, BooleanBuilder zoneBe, String where, String ownerName){
        ZoneController zonCon = new ZoneController();
        BooleanBuilder globZoneBe = new BooleanBuilder(zoneBe);
        BooleanBuilder mBe = new BooleanBuilder(mapBe);
        List<Map<String, Object>> listLocalMap = new LinkedList<>();
        Long groupId = (Long) map.get("id");
        mBe = mBe.and(QLocalMap.localMap.group.id.eq(groupId));
        List<LocalMap> localMap = LocalMapService.getInstance().selectByGroupId(mBe);
        if (PermissionsUtils.isPermittedAny(SecurityUtils.getSubject(), "zone:r")){
            List<Map<String, Object>> listMapZones = new LinkedList<>();
            globZoneBe = globZoneBe.and(QZone.zone.group.id.eq(groupId));
            List<Zone> globalZones = ZoneService.getInstance().getGlobalZone(globZoneBe);
            Map globalZone = new HashMap<>();
            for (Zone zone: globalZones){
                Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( zone, null, zonCon.getExtraPropertyNames());
                zonCon.addToPublicMap(zone, publicMap, "zoneType");
                listMapZones.add(publicMap);
            }
            if (listMapZones.size() > 0) {
                globalZone.put("globalZone", listMapZones);
                listLocalMap.add(globalZone);
            }
        }

        List<Map<String, Object>> listChildren = (List<Map<String, Object>>) map.get("children");


        if (listChildren.size() > 0){
            List<Map<String, Object>> toRemove = new ArrayList<>();
            for (Map childrenMap : listChildren){
                childrenMap.put("mapMaker",fillChildren(childrenMap,mapBe,zoneBe, where,ownerName));
                Group group = GroupService.getGroupDAO().selectById((Long)childrenMap.get("id"));
                if (ownerName != null) {
                    String owner = GroupFieldService.getInstance().getOwnershipValue(group, ownerName);
                    childrenMap.put("ownership", owner);
                }
                if (where != null && ((List)childrenMap.get("children")).size()==0 && ((List)childrenMap.get("mapMaker")).size()==0){
                    toRemove.add(childrenMap);
                }
            }
            listChildren.removeAll(toRemove);
        }
        OrderSpecifier order[] = QueryUtils.getOrderFields(QZone.zone,"name:asc");

        for (LocalMap loMap:localMap){
            Map mapFacility = new HashMap<>();

                BooleanBuilder be = new BooleanBuilder(zoneBe);
                be = be.and(QZone.zone.localMap.id.eq( loMap.getId() ));
                mapFacility.put("localMap", loMap.publicMap());
                List<Zone> listZones = ZoneService.getZoneDAO().selectAll(be,null,order);
                List<Map<String, Object>> listMapZones = new LinkedList<Map<String, Object>>();
                for (Zone zone: listZones){
                    Map<String,Object> publicMap = QueryUtils.mapWithExtraFields( zone,null , zonCon.getExtraPropertyNames());
                    if (PermissionsUtils.isPermittedAny(SecurityUtils.getSubject(), "zone:r")){
                        zonCon.addToPublicMap(zone, publicMap, "zoneType");
                        listMapZones.add(publicMap);
                    }
                }
                mapFacility.put("childrenZone", listMapZones);

                if (where == null) {
                    listLocalMap.add(mapFacility);
                }else{
                    if (where != null && listMapZones.size() != 0){
                        listLocalMap.add(mapFacility);
                    }
                }

        }

        return listLocalMap;
    }


    /**
     *
     * @param zoneTypeId
     * @return Zone Name List by Zone Type Id
     */
    public List <String> getZoneNamesByZoneType(Long zoneTypeId){
        HibernateQuery query = ZoneService.getZoneDAO().getQuery();
        return query.where( QZone.zone.zoneType.id.eq(zoneTypeId)).list(QZone.zone.name);
    }
    /**
     * get current Report Definition's name
     * @param currentReportNames list of current report definitions names
     * @param reportName report name
     * @return a list of Report Definition's name
     */
    public static List<String> getCurrentReportName(List<String>currentReportNames, String reportName){
        if ((currentReportNames.size() < 1) || (!currentReportNames.contains(reportName))) {
            currentReportNames.add(reportName);
        }
        return currentReportNames;
    }

    public static List<String> getReportFiltersDependencies(String propertyName , Zone zone, List<String> currentReportNames) {
        List<ReportFilter> reportFilters = ReportFilterService.getInstance().getFiltersByPropertyName(propertyName);
        for (ReportFilter reportFilter: reportFilters){
            getCurrentReportName(reportFilter.getOperator(), reportFilter.getValue(), zone.getId().toString(),
                    reportFilter.getReportDefinition().getName(), currentReportNames);
        }
        return currentReportNames;
    }
    /**
     * get Report Rules with Dependencies on  zone property's name
     * @param propertyName
     * @param currentReportNames
     * @return a list of Report Definition's name
     */
    public static List<String> getReportRulesDependencies(String propertyName, Zone zone, List<String> currentReportNames){
        List<ReportRule> reportRules = ReportRuleService.getInstance().getRuleByPropertyName(propertyName);
        if (reportRules.size() > 0) {
            for (ReportRule reportRule : reportRules) {
                getCurrentReportName(reportRule.getOperator(), reportRule.getValue(), zone.getId().toString(),
                        reportRule.getReportDefinition().getName(), currentReportNames);
            }
        }
        return currentReportNames;
    }

    public static List<String> getCurrentReportName(String operator, String value, String valueToSearch,
                                                    String reportName,List<String> currentReportNames){
        if ((StringUtils.equals(operator, "=")) || (StringUtils.equals(operator, "!="))){
            if (StringUtils.equals(value, valueToSearch)){
                currentReportNames = getCurrentReportName(currentReportNames, reportName);
            }
        }
        return currentReportNames;
    }

    public static List<String> deleteCurrentZone(Zone zone, boolean validateZonePropertyOnReports, boolean validateLogicalReader, boolean validateShift) {
        List<String> errorMessages = new ArrayList<>();
        // Validating zonePropertyValues on reports
        if (validateZonePropertyOnReports){
            List<String> currentReportNames = getCurrentReportNames(zone);
            List<ZonePropertyValue> zonePropertyValues = ZonePropertyValueService.getInstance().
                    getZonePropertiesByZoneId(zone.getId());
            if (zonePropertyValues.size() > 0) {
                String propertyName = "zoneProperty.id";
                for (ZonePropertyValue zonePropertyValue : zonePropertyValues) {
                    ZoneProperty zoneProperty = ZonePropertyService.getInstance().getZonePropertyById(zonePropertyValue.getZonePropertyId());
                    if (zoneProperty != null) {
                        List<ReportFilter> reportFilters = ReportFilterService.getInstance().getFiltersByPropertyNameValueLabel(
                                propertyName, zonePropertyValue.getValue(), zoneProperty.getName());
                        for (ReportFilter reportFilter : reportFilters) {
                            getCurrentReportName(reportFilter.getOperator(), reportFilter.getValue(), zonePropertyValue.getValue(),
                                    reportFilter.getReportDefinition().getName(), currentReportNames);
                        }
                    }
                }
                if (!currentReportNames.isEmpty()) {
                    String reportNamesString = StringUtils.join(currentReportNames, ", ");
                    errorMessages.add("have references into Reports: " + reportNamesString + ".\"");
                }
            }
        }
        // Validating zones on LogicalReaders
        if (validateLogicalReader){
            List<LogicalReader> logicalReaderList = LogicalReaderService.getInstance().selectAllByZone(zone);
            if (!logicalReaderList.isEmpty()) {
                List<String> logicalReaderNames = new ArrayList<>();
                for (LogicalReader logicalReader:logicalReaderList) {
                    logicalReaderNames.add(logicalReader.getName());
                }
                String message = "Is referenced on logical reader(s): [" + StringUtils.join(logicalReaderNames, ",") + "].";
                errorMessages.add(message);
            }
        }
        // Validating zones on Shifts
        if (validateShift){
            List<ShiftZone> shiftZoneList = ShiftZoneService.getInstance().selectAllByZone(zone);
            if (!shiftZoneList.isEmpty()) {
                List<String> shiftZoneNames = new ArrayList<>();
                for (ShiftZone shiftZone:shiftZoneList) {
                    shiftZoneNames.add(shiftZone.getShift().getName());
                }
                String message = "Is referenced on Shift(s): [" + StringUtils.join(shiftZoneNames, ",") + "].";
                errorMessages.add(message);
            }
        }
        // deleting the zone
        if (errorMessages.isEmpty()) {
            ZoneService.getInstance().delete(zone);
        }
        return errorMessages;
    }

    public static List<String> getCurrentReportNames(Zone zone){
        List<String> currentReportNames = new ArrayList<>();
        String propertyName = "zone";
        currentReportNames = getReportFiltersDependencies(propertyName, zone, currentReportNames);
        currentReportNames = getReportRulesDependencies(propertyName, zone, currentReportNames);
        return currentReportNames;
    }
    /**
     *
     * @param zoneId
     * @return Zone Name List by Zone Id
     */
    public String getZoneNameByZoneId(Long zoneId){
        HibernateQuery query = ZoneService.getZoneDAO().getQuery();
        return query.where( QZone.zone.id.eq(zoneId)).uniqueResult(QZone.zone.name);
    }

    public Map<String, Object> getZonePoints(Integer rowSize,Integer columnSize,
                                             Double zoneWidth, Double zoneHeight, Double gapWidth, Double gapHeight,Double zoneLatitude,
                                             Double zoneLongitude,Long facilityId){

        if (gapHeight == null){
            gapHeight = 0.0;
        }
        if (gapWidth == null){
            gapWidth = 0.0;
        }

        if (zoneHeight == null) {
            zoneHeight = 1.0;
        }
        if (zoneWidth == null) {
            zoneWidth = 1.0;
        }

        LocalMap facilityMap = null;
        if (facilityId == null) {
            logger.debug("validation for local map id");
        } else {
            facilityMap = LocalMapService.getInstance().get(facilityId);
            if (facilityMap == null) {
                throw new UserException("Local map id [" + facilityId + "] not found.");
            }
        }

        Double altitude = 0.0;
        CoordinateUtils cu = null;

        double [] coordinates = new double[2];


        boolean zonePoints = false;

        if ((zoneLatitude != null) && (zoneLongitude != null)) {
            coordinates[0] = zoneLongitude;
            coordinates[1] =  zoneLatitude;

            if (facilityMap.getAltOrigin() != null){
                altitude = facilityMap.getAltOrigin();
            }
            cu = new CoordinateUtils(facilityMap.getLonOrigin(), facilityMap.getLatOrigin(),
                    altitude ,0, facilityMap.getImageUnit());
            zonePoints = true;
        }
        List<Map<String, Object>> listResponse = new ArrayList<>();

        for (char i = 0; i < rowSize; i++){
            double [] point = coordinates;

            for (char j = 0; j < columnSize; j++){
                Map<String,Object> gridPoints = new HashMap<>();
                if (zonePoints) {
                    List<List<Double>> points = generateZonePoints(cu,zoneWidth, zoneHeight,point[0],point[1],altitude);
                    gridPoints.put("zonePoints", points);
                    point = cu.xy2lonlat((j+1)*(zoneWidth + gapWidth),0,0,coordinates[0],coordinates[1],altitude);
                }
                listResponse.add(gridPoints);
            }
            if (zonePoints) {
                coordinates = cu.xy2lonlat(0, (i + 1) * (-zoneHeight - gapHeight), 0, zoneLongitude, zoneLatitude, altitude);
            }
        }

        Map<String,Object> mapResponse = new HashMap<>();
        mapResponse.put("gridZonePoints", listResponse);
        return mapResponse;

    }

    public Map<String, Object> getGridZoneNames(Map<String, Object> body) {
        Integer rowSize = null;
        Integer columnSize = null;
        String nameOrExpression = null;
        String rowInitialValue = null;
        String columnInitialValue = null;
        String rowIncrementStr = null;
        String columnIncrementStr = null;
        Double zoneWidth = null;
        Double zoneHeight = null;
        Double gapWidth = null;
        Double gapHeight = null;
        String zoneCodeListStr = null;
        String zoneNameListStr = null;
        Long facilityId = null;
        if (body.get("rowSize") != null) {
            rowSize = Integer.valueOf(body.get("rowSize").toString());
        }
        if (body.get("columnSize") != null) {
            columnSize = Integer.valueOf(body.get("columnSize").toString());
        }
        if (body.get("nameOrExpression") != null) {
            nameOrExpression = body.get("nameOrExpression").toString();
        }
        if (body.get("rowInitialValue") != null) {
            rowInitialValue = body.get("rowInitialValue").toString();
        }
        if (body.get("columnInitialValue") != null) {
            columnInitialValue = body.get("columnInitialValue").toString();
        }
        if (body.get("rowIncrement") != null) {
            rowIncrementStr = body.get("rowIncrement").toString();
        }
        if (body.get("columnIncrement") != null) {
            columnIncrementStr = body.get("columnIncrement").toString();
        }
        if (body.get("facilityId") != null) {
            facilityId = Long.valueOf(body.get("facilityId").toString());
        }
        if (body.get("zoneWidth") != null) {
            zoneWidth = Double.valueOf(body.get("zoneWidth").toString());
        }
        if (body.get("zoneHeight") != null) {
            zoneHeight = Double.valueOf(body.get("zoneHeight").toString());
        }
        if (body.get("gapWidth") != null) {
            gapWidth = Double.valueOf(body.get("gapWidth").toString());
        }
        if (body.get("gapHeight") != null) {
            gapHeight = Double.valueOf(body.get("gapHeight").toString());
        }

        Integer rowIncrement;
        Integer columnIncrement;
        String temporalName = null;
        // Setting default values

        if (rowInitialValue == null || rowInitialValue.isEmpty()){
            rowInitialValue = "1";
            if ((nameOrExpression != null) && (nameOrExpression.contains("rowLetter"))) {
                rowInitialValue = "A";
            }
        }
        if (columnInitialValue == null || columnInitialValue.isEmpty()){
            columnInitialValue = "1";
            if ((nameOrExpression != null) && (nameOrExpression.contains("colLetter"))) {
                columnInitialValue = "A";
            }
        }
        try {
            if ((rowIncrementStr == null) || (rowIncrementStr.isEmpty())) {
                rowIncrement = 1;
            } else {
                rowIncrement = Integer.valueOf(rowIncrementStr);
            }
            if ((columnIncrementStr == null) || (columnIncrementStr.isEmpty())) {
                columnIncrement = 1;
            } else {
                columnIncrement = Integer.valueOf(columnIncrementStr);
            }
        } catch (Exception e) {
            throw new UserException("Row and Column increment values should be a number [1..26].", e);
        }


        // Validating values
        ValidationBean validationBean = validateParams(rowSize, columnSize, nameOrExpression, rowInitialValue,
                columnInitialValue, rowIncrement, columnIncrement,zoneWidth,zoneHeight, gapWidth,gapHeight);
        if (validationBean.isError()) {
            throw new UserException(validationBean.getErrorDescription());
        }

        if (!StringUtils.contains(nameOrExpression,"rowNumber") && !StringUtils.contains(nameOrExpression,"colNumber") &&
                !StringUtils.contains(nameOrExpression,"rowLetter") && !StringUtils.contains(nameOrExpression,"colLetter")&&
                rowInitialValue == null ){
            temporalName = "${rowNumber}";
        }
        if (!StringUtils.contains(nameOrExpression,"rowNumber") && !StringUtils.contains(nameOrExpression,"colNumber") &&
                !StringUtils.contains(nameOrExpression,"rowLetter") && !StringUtils.contains(nameOrExpression,"colLetter")&&
                StringUtils.isAlpha(rowInitialValue)){
            temporalName = "${rowLetter}";
        }
        if (!StringUtils.contains(nameOrExpression,"rowNumber") && !StringUtils.contains(nameOrExpression,"colNumber") &&
                !StringUtils.contains(nameOrExpression,"rowLetter") && !StringUtils.contains(nameOrExpression,"colLetter")&&
                StringUtils.isNumeric(rowInitialValue)){
            temporalName = "${rowNumber}";
        }
        if (!StringUtils.contains(nameOrExpression,"rowNumber") && !StringUtils.contains(nameOrExpression,"colNumber") &&
                !StringUtils.contains(nameOrExpression,"rowLetter") && !StringUtils.contains(nameOrExpression,"colLetter")&&
                columnInitialValue == null){
            nameOrExpression = nameOrExpression + temporalName + "${colNumber}";
        }

        if (!StringUtils.contains(nameOrExpression,"rowNumber") && !StringUtils.contains(nameOrExpression,"colNumber") &&
                !StringUtils.contains(nameOrExpression,"rowLetter") && !StringUtils.contains(nameOrExpression,"colLetter")&&
                StringUtils.isAlpha(columnInitialValue)){
            nameOrExpression = nameOrExpression + temporalName + "${colLetter}";
        }
        if (!StringUtils.contains(nameOrExpression,"rowNumber") && !StringUtils.contains(nameOrExpression,"colNumber") &&
                !StringUtils.contains(nameOrExpression,"rowLetter") && !StringUtils.contains(nameOrExpression,"colLetter")&&
                StringUtils.isNumeric(columnInitialValue)){
            nameOrExpression = nameOrExpression + temporalName + "${colNumber}";
        }

        LocalMap facilityMap = null;
        if (facilityId == null) {
//            throw new UserException("Local map id is required.");
            logger.debug("validation for local map id");
        } else {
            facilityMap = LocalMapService.getInstance().get(facilityId);
            if (facilityMap == null) {
                throw new UserException("Local map id [" + facilityId + "] not found.");
            }
        }

        // validating name or expression
        if (nameOrExpression != null && !nameOrExpression.isEmpty()){
            ValidationBean validationBean1 = ValidatorService.checkSyntaxExpression(nameOrExpression);
            if (validationBean1.isError()) {
                throw new UserException(validationBean1.getErrorDescription());
            }
        }

        // Creating grid zone names
        List<Map<String, String>> listResponse = new ArrayList<>();
        Map<String,Object> inputMap = new HashMap<>();
        inputMap.put("rowSize", rowSize);
        inputMap.put("columnSize", columnSize);
        inputMap.put("nameOrExpression", nameOrExpression);
        inputMap.put("rowIncrement", rowIncrement);
        inputMap.put("columnIncrement", columnIncrement);

        ElExpressionService ees = new ElExpressionService();

        // alpha rows and alpha columns
        if (StringUtils.isAlpha(rowInitialValue) && StringUtils.isAlpha(columnInitialValue)){
            // determine initial row char
            char[] rowInitialValueCharArray = rowInitialValue.toUpperCase().toCharArray();
            Character[] rowInitialValueArray = ArrayUtils.toObject(rowInitialValueCharArray);
            char rowInitialChar = rowInitialValueArray[0];
            // determine initial column char
            char[] colInitialValueCharArray = columnInitialValue.toUpperCase().toCharArray();
            Character[] colInitialValueArray = ArrayUtils.toObject(colInitialValueCharArray);
            char colInitialChar = colInitialValueArray[0];
            // generate zone names
            int rowValue = rowInitialChar - 64;
            for (char i = 1; i <= rowSize; i++){
                String rowName = createChain(rowValue);
                int columnValue = colInitialChar - 64;
                for (char j = 1; j <= columnSize; j++){
                    Map<String,String> gridNames = new HashMap<>();
                    String columnName = createChain(columnValue);
                    inputMap.put("rowLetter",rowName);
                    inputMap.put("colLetter",columnName);
                    ees.initialize(inputMap,null);
                    Object name = ees.evaluate(nameOrExpression,false);
                    if (name == null){
                        throw new UserException("Invalid Expression");
                    }
                    gridNames.put("name", name.toString());
                    columnValue = columnValue + columnIncrement;
                    if (listResponse.contains(gridNames)) {
                        throw new UserException("Expression produces repeated zone names");
                    }
                    zoneCodeListStr = zoneCodeListStr + "," + gridNames.get("name").toString();
                    zoneNameListStr = zoneNameListStr + "," + gridNames.get("name").toString();
                    if (isValidNewZoneCodeAndNewName(createBody(gridNames.get("name").toString(), body), true)) {
                        listResponse.add(gridNames);
                    }
                }
                rowValue =  rowValue + rowIncrement;
            }
        }

        // number rows and number columns
        if (StringUtils.isNumeric(rowInitialValue) && StringUtils.isNumeric(columnInitialValue)){
            int initialRow = Integer.parseInt(rowInitialValue);
            int initialColumn;

            for (int i = 1; i <= rowSize; i++){
                initialColumn = Integer.parseInt(columnInitialValue);
                for (int j = 1; j <= columnSize; j++){
                    Map<String,String> gridNames = new HashMap<>();
                    inputMap.put("rowNumber",initialRow);
                    inputMap.put("colNumber",initialColumn);
                    ees.initialize(inputMap,null);
                    initialColumn = initialColumn + columnIncrement;
                    Object name = ees.evaluate(nameOrExpression,false);
                    if (name == null){
                        throw new UserException("Invalid Expression");
                    }
                    gridNames.put("name", name.toString());
                    if (listResponse.contains(gridNames)) {
                        throw new UserException("Expression produces repeated zone names");
                    }
                    zoneCodeListStr = zoneCodeListStr + "," + gridNames.get("name").toString();
                    zoneNameListStr = zoneNameListStr + "," + gridNames.get("name").toString();
                    if (isValidNewZoneCodeAndNewName(createBody(gridNames.get("name").toString(), body), true)) {
                        listResponse.add(gridNames);
                    }
                }
                initialRow =  initialRow + rowIncrement;
            }
        }

        // number rows and alpha columns
        if (StringUtils.isNumeric(rowInitialValue) && StringUtils.isAlpha(columnInitialValue)){
            // determine initial column char
            char[] colInitialValueCharArray = columnInitialValue.toUpperCase().toCharArray();
            Character[] colInitialValueArray = ArrayUtils.toObject(colInitialValueCharArray);
            char colInitialChar = colInitialValueArray[0];
            // generate zone names
            int rowInitial = Integer.parseInt(rowInitialValue);

            for (int i = 1; i <= rowSize; i++){
                int columnValue = colInitialChar - 64;
                for (char j = 1; j <= columnSize; j++){
                    Map<String,String> gridNames = new HashMap<>();
                    String columnName = createChain(columnValue);
                    inputMap.put("rowNumber",rowInitial);
                    inputMap.put("colLetter",columnName);
                    ees.initialize(inputMap,null);

                    Object name = ees.evaluate(nameOrExpression,false);
                    if (name == null){
                        throw new UserException("Invalid Expression");
                    }
                    gridNames.put("name", name.toString());
                    columnValue = columnValue + columnIncrement;
                    if (listResponse.contains(gridNames)) {
                        throw new UserException("Expression produces repeated zone names");
                    }
                    zoneCodeListStr = zoneCodeListStr + "," + gridNames.get("name").toString();
                    zoneNameListStr = zoneNameListStr + "," + gridNames.get("name").toString();
                    if (isValidNewZoneCodeAndNewName(createBody(gridNames.get("name").toString(), body), true)) {
                        listResponse.add(gridNames);
                    }
                }
                rowInitial = rowInitial + rowIncrement;
            }
        }

        // alpha rows and number columns
        if (StringUtils.isAlpha(rowInitialValue) && StringUtils.isNumeric(columnInitialValue)){
            // determine initial row char
            char[] rowInitialValueCharArray = rowInitialValue.toUpperCase().toCharArray();
            Character[] rowInitialValueArray = ArrayUtils.toObject(rowInitialValueCharArray);
            char rowInitialChar = rowInitialValueArray[0];
            // generate zone names
            int rowValue = rowInitialChar - 64;

            for (char i = 1; i <= rowSize; i++){
                String rowName = createChain(rowValue);
                int columnInitial = Integer.parseInt(columnInitialValue);
                for (int j = 1; j <= columnSize; j++){
                    Map<String,String> gridNames = new HashMap<>();
                    inputMap.put("rowLetter",rowName);
                    inputMap.put("colNumber",columnInitial);
                    columnInitial = columnInitial + columnIncrement;
                    ees.initialize(inputMap,null);
                    Object evaluatedExpression = ees.evaluate(nameOrExpression,false);
                    if (evaluatedExpression == null){
                        throw new UserException("Parameters for expression have not been set or they are invalid");
                    }
                    gridNames.put("name",evaluatedExpression.toString());
                    logger.info("ZoneName ** "+Integer.valueOf(i)+" ** "+j+" ** "+gridNames);
                    if (listResponse.contains(gridNames)) {
                        throw new UserException("Expression produces repeated zone names");
                    }
                    if (zoneCodeListStr != null){
                        zoneCodeListStr = zoneCodeListStr + "," + gridNames.get("name").toString();
                    }
                    if (zoneNameListStr != null) {
                        zoneNameListStr = zoneNameListStr + "," + gridNames.get("name").toString();
                    }
                    if (gridNames.get("name") != null){
                        if (isValidNewZoneCodeAndNewName(createBody(gridNames.get("name").toString(), body), true)) {
                            listResponse.add(gridNames);
                        }
                    }
                }
                rowValue =  rowValue + rowIncrement;
            }
        }
        Map<String,Object> mapResponse = new HashMap<>();
        mapResponse.put("gridZoneNames", listResponse);
        return mapResponse;
    }

    private Map<String, Object> createBody(String name, Map<String, Object> bodyInput) {
        Map<String, Object> body = new HashMap<>();
        body.put("isNew", true);
        body.put("isNewGrid", true);
        Map<String, Object> zoneMap = new HashMap<>();
        zoneMap.put("name", name);
        zoneMap.put("code", name);
        // localMap.id, zoneGroup.id and zoneType.id are temporal values (only for initial validation)
        zoneMap.put("localMap.id", bodyInput.get("facilityId"));
        zoneMap.put("zoneGroup.id", bodyInput.get("facilityId"));
        zoneMap.put("zoneType.id", bodyInput.get("facilityId"));
        zoneMap.put("color", "FFFFFF");
        body.put("newZone", zoneMap);
        body.put("zones", bodyInput.get("zones"));
        return body;
    }

    private ValidationBean validateParams(Integer rowSize, Integer columnSize, String nameOrExpression, String rowInitialValue,
                                          String columnInitialValue, Integer rowIncrement, Integer columnIncrement,Double zoneWidth, Double zoneHeight, Double gapWidth,
                                          Double gapHeight) {
        Map<String, Object> validationValuesMap = fillValidationValues();
        String errorMessage;
        if (!(errorMessage = isValidValue("Rows", validationValuesMap, rowSize)).isEmpty()) {
            return ValidationUtils.getInstance().newValidationError(errorMessage);
        }
        if (!(errorMessage = isValidValue("Columns", validationValuesMap, columnSize)).isEmpty()) {
            return ValidationUtils.getInstance().newValidationError(errorMessage);
        }
        if ((nameOrExpression == null) || (nameOrExpression.isEmpty())) {
            return ValidationUtils.getInstance().newValidationError("[Prefix name or expression] value is required, and it should not empty");
        }
        if (nameOrExpression.length() > 100) {
            return ValidationUtils.getInstance().newValidationError("[Prefix name or expression] length should be have max 100 characters.");
        }
//        if ((nameOrExpression.contains("${rowLetter}") || nameOrExpression.contains("${rowNumber}"))
//                && !(nameOrExpression.contains("${colLetter}") || (nameOrExpression.contains("${colNumber}")))) {
//            return ValidationUtils.getInstance().newValidationError("You need to add ${colLetter} or ${colNumber} into expression");
//        }
//        if ((nameOrExpression.contains("${colLetter}") || nameOrExpression.contains("${colNumber}"))
//                && !(nameOrExpression.contains("${rowLetter}") || (nameOrExpression.contains("${rowNumber}")))) {
//            return ValidationUtils.getInstance().newValidationError("You need to add ${rowLetter} or ${rowNumber} into expression");
//        }
        if (nameOrExpression.contains("${rowLetter}") && (!StringUtils.isAlpha(rowInitialValue))) {
            return ValidationUtils.getInstance().newValidationError("[Row starts with] Should be any letter [A-Z]");
        }
        if (nameOrExpression.contains("${colLetter}") && (!StringUtils.isAlpha(columnInitialValue))) {
            return ValidationUtils.getInstance().newValidationError("[Column starts with] Should be any letter [A-Z]");
        }
        if (nameOrExpression.contains("${rowNumber}") && (!StringUtils.isNumeric(rowInitialValue))) {
            return ValidationUtils.getInstance().newValidationError("[Row starts with] Should be any number [0-9]");
        }
        if (nameOrExpression.contains("${colNumber}") && (!StringUtils.isNumeric(columnInitialValue))) {
            return ValidationUtils.getInstance().newValidationError("[Column starts with] Should be any number [0-9]");
        }
        if (StringUtils.isAlpha(rowInitialValue) && (rowInitialValue.length() > 1)) {
            return ValidationUtils.getInstance().newValidationError("rowInitialValue has to be only a letter, couldn't be a string");
        }
        if (StringUtils.isAlpha(columnInitialValue) && (columnInitialValue.length() > 1)) {
            return ValidationUtils.getInstance().newValidationError("columnInitialValue has to be only a letter, couldn't be a string");
        }
        if (!(errorMessage = isValidValue("rowIncrement", validationValuesMap, rowIncrement)).isEmpty()) {
            return ValidationUtils.getInstance().newValidationError(errorMessage);
        }
        if (!(errorMessage = isValidValue("columnIncrement", validationValuesMap, columnIncrement)).isEmpty()) {
            return ValidationUtils.getInstance().newValidationError(errorMessage);
        }

        if (!(errorMessage = isValidValue("Zone Height", validationValuesMap, zoneHeight)).isEmpty()) {
            return ValidationUtils.getInstance().newValidationError(errorMessage);
        }
        if (!(errorMessage = isValidValue("Zone Height", validationValuesMap, zoneWidth)).isEmpty()) {
            return ValidationUtils.getInstance().newValidationError(errorMessage);
        }
        if (!(errorMessage = isValidValue("Gap Width", validationValuesMap, gapHeight)).isEmpty()) {
            return ValidationUtils.getInstance().newValidationError(errorMessage);
        }
        if (!(errorMessage = isValidValue("Gap Height", validationValuesMap, gapWidth)).isEmpty()) {
            return ValidationUtils.getInstance().newValidationError(errorMessage);
        }
        // Setting default values for expressions
        Map<String,Object> propertiesList = new HashMap<>();
        propertiesList.put("rowLetter","A");
        propertiesList.put("rowNumber",1);
        propertiesList.put("colLetter","A");
        propertiesList.put("colNumber",1);
        // Evaluating default expression
        ElExpressionService ees = new ElExpressionService();
        ees.initialize(propertiesList,null);
        try {
            ees.evaluate(nameOrExpression,true);
        } catch (PropertyNotFoundException e) {
            return ValidationUtils.getInstance().newValidationError("[${"+e.getMessage().replace("Cannot find property ","")+"}] is not a valid expression for grid names.");
        }

        return ValidationUtils.getInstance().newValidationOk();
    }

    private String isValidValue(String field, Map<String, Object> validationValuesMap, Object valueObject) {
        Map<String, Object> mapValues = (Map)validationValuesMap.get(field);
        if (mapValues == null) {
            return "[" + field + "] has not initial values to validate";
        }
        if (Boolean.valueOf(mapValues.get("isRequired").toString()) && (valueObject == null)) {
            return "[" + field + "] is required.";
        }
        if (valueObject != null) {
            int value = Double.valueOf(valueObject.toString()).intValue();
            if (value < Integer.parseInt(mapValues.get("minValue").toString())) {
                return "[" + field + "] should be greater or equal to: " + mapValues.get("minValue").toString() + ".";
            }
            if (value > Integer.parseInt(mapValues.get("maxValue").toString())) {
                return "[" + field + "] should be lower or equal to: " + mapValues.get("maxValue").toString() + ".";
            }
        }
        return "";
    }

    private Map<String, Object> fillValidationValues() {
        Map<String, Object> mapResponse = new HashMap<>();
        // createValidatorValue(isRequired, minValue, maxValue)
        mapResponse.put("Rows", createValidatorValue(true, 1, 100));
        mapResponse.put("Columns", createValidatorValue(true, 1, 100));
        mapResponse.put("rowIncrement", createValidatorValue(true, 1, 26));
        mapResponse.put("columnIncrement", createValidatorValue(true, 1, 26));
        mapResponse.put("Zone Height", createValidatorValue(false, 1, 9999));
        mapResponse.put("Zone Width", createValidatorValue(false, 1, 9999));
        mapResponse.put("Gap Height", createValidatorValue(false, 0, 20));
        mapResponse.put("Gap Width", createValidatorValue(false, 0, 20));
        return mapResponse;
    }

    private Map<String, Object> createValidatorValue(Boolean isRequired, Integer minValue, Integer maxValue) {
        Map<String, Object> mapResponse = new HashMap<>();
        mapResponse.put("isRequired", isRequired);
        mapResponse.put("minValue", minValue);
        mapResponse.put("maxValue", maxValue);
        return mapResponse;
    }

    public String createChain(int value){
        List<Integer> prefixList = new ArrayList<>();
        char[] charAlphabet = "*ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        int j = 0;
        while (value >= 26){
            Integer intTemp = value / 26;
            if (intTemp > 26 ){
                int tempValue = intTemp/26;
                for (int k = 0; k < tempValue; k++ ){
                    prefixList.add(26);
                }
                value = value % (26 * tempValue);
                prefixList.add(intTemp - (26 * tempValue));
                j = j + 1;
            } else {
                prefixList.add(intTemp);
                value = value % 26;
            }
        }
        String result = "";
        int suffix = value;
        for (Integer prefixValue : prefixList){
            for (int i = 1; i <= prefixValue; i++) {
                result = result.concat(String.valueOf(charAlphabet[i]));
            }
        }
        if (suffix != 0) {
            result = result.concat(String.valueOf(charAlphabet[suffix]));
        }else{
            result = result.concat("Z");
        }
        return result;
    }

    public List<List<Double>> generateZonePoints(CoordinateUtils cu,double zoneWidth, double zoneHeigth, double zoneLongitude, double zoneLatitude, double altiOrigin){
        List<List<Double>> points = new LinkedList<>();
        List<Double> point = new LinkedList<>();
        point.add(zoneLongitude);
        point.add(zoneLatitude);
        point.add(points.size()+0.0);
        points.add(point);

        double [] temporal = cu.xy2lonlat(zoneWidth,0,0,zoneLongitude, zoneLatitude,altiOrigin);
        point = new LinkedList<>();
        point.add(temporal[0]);
        point.add(temporal[1]);
        point.add(points.size()+0.0);
        points.add(point);

        temporal = cu.xy2lonlat(0,-zoneHeigth,0,point.get(0), point.get(1),altiOrigin);
        point = new LinkedList<>();
        point.add(temporal[0]);
        point.add(temporal[1]);
        point.add(points.size()+0.0);
        points.add(point);

        point = new LinkedList<>();
        point.add(points.get(0).get(0));
        point.add(points.get(2).get(1));
        point.add(points.size()+0.0);
        points.add(point);

        return points;
    }

    public static void setZoneValues(Map<String,Object> zoneMap, Zone zone){
        Group group;
        if (zoneMap.containsKey("group.id")) {
            group = GroupService.getInstance().get(((Number) zoneMap.get("group.id")).longValue());
        }
        else {
            group = VisibilityUtils.getVisibilityGroup(Zone.class.getCanonicalName(), null);
        }
        zone.setGroup( group );

        if ( zoneMap.containsKey("localMap.id")) {
            if(!zoneMap.containsKey("zoneGroup.id")) {
                throw new UserException("ZoneGroup is required when a facility map is selected");
            }
        }

        if(zoneMap.containsKey("zoneGroup.id")) {
            ZoneGroup zoneGroup = ZoneGroupService.getInstance().get(Long.valueOf(zoneMap.get("zoneGroup.id").toString()));
            if(zoneGroup != null) {
                zone.setZoneGroup(zoneGroup);
                zone.setLocalMap(zoneGroup.getLocalMap());
            }
        }

        if((zoneMap.get("name") == null) || zoneMap.get("name").toString().isEmpty()) {
            zone.setName("Zone " + zone.getId().toString());
        } else {
            zone.setName((String) zoneMap.get("name"));
        }

        zone.setDescription( (String) zoneMap.get( "description"));
        zone.setColor((String) zoneMap.get( "color" ) );
        if(zoneMap.containsKey("code")) {
            zone.setCode(zoneMap.get("code").toString());
        }

    }

    public static void setZonePointValues(Map<String,Object> zoneMap, Zone zone){


        List<Map<String, Object>> zonePoints = (List<Map<String, Object>>) zoneMap.get( "zonePoints" );
        List<List<Double>> points = new LinkedList<>();
        for( Map<String, Object> zonePoint : zonePoints )
        {
            List<Double> point = new LinkedList<>();
            point.add( new Double( zonePoint.get( "x" ).toString() ) );
            point.add( new Double( zonePoint.get( "y" ).toString() ) );
            Double index = points.size() + 0.0;
            if( zonePoint.containsKey( "arrayIndex" ) )
            {
                index = new Double( zonePoint.get( "arrayIndex" ).toString() );
            }
            point.add( index );
            points.add( point );
        }
        if(zoneMap.containsKey("zoneType")) {
            ZoneTypeService.updatingZoneTypeFromZone(zoneMap.get("zoneType"), zone);
        }
        ZoneService.getInstance().updateZonePoints( zone, points );

    }

    public List<Map<String, Object>> bulkZoneProcess(List<Map<String, Object>> listMap) {
        List<Map<String, Object>> listMapResponse = new LinkedList<>();
        Map<String, Object> zonePublicMap;

        for (Map<String, Object> zoneMap:listMap) {
            Map<String, Object> mapResponse = new HashMap<>();
            ValidationBean validationBean = validateZoneMap(zoneMap);
            if (validationBean.isError()) {
                mapResponse.put("status", "error");
                mapResponse.put("errorMessage", validationBean.getErrorDescription());
                if (zoneMap.containsKey("id")) {
                    mapResponse.put("id", zoneMap.get("id"));
                }
                listMapResponse.add(mapResponse);
                continue;
            }
            String operation = zoneMap.get("operation").toString();
            Zone zone = null;

            try {
                switch (operation) {
                    case "add":
                        if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new HashMap<>(), "zone:i")) {
                            throw new UserException("Permissions error: User does not have permission to create zones.");
                        }
                        zone = new Zone();
                        ZoneService.setZoneValues(zoneMap, zone);
                        zone = ZoneService.getInstance().insert(zone);
                        ZoneService.setZonePointValues(zoneMap, zone);
                        zonePublicMap = zone.publicMap();
                        if (zonePublicMap.containsKey("tmpId")) {
                            zonePublicMap.put("oldId", zoneMap.get("tmpId"));
                        }
                        mapResponse.put("status", "success");
                        mapResponse.put("id", zone.getId());
                        listMapResponse.add(mapResponse);
                        break;

                    case "update":
                        if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new HashMap<>(), "zone:u")) {
                            throw new UserException("Permissions error: User does not have permission to update zones.");
                        }
                        if (zoneMap.get("id") != null) {
                            zone = ZoneService.getInstance().get(((Number) zoneMap.get("id")).longValue());
                        }
                        if (zone == null) {
                            mapResponse.put("status", "error");
                            mapResponse.put("errorMessage", String.format("LocalMapId[%d] not found", ((Number) zoneMap.get("id"))));
                            mapResponse.put("id", zoneMap.get("id"));
                            listMapResponse.add(mapResponse);
                            break;
                        }
                        ZoneService.setZoneValues(zoneMap, zone);
                        ZoneService.setZonePointValues(zoneMap, zone);
                        zone = ZoneService.getInstance().update(zone);
                        zonePublicMap = zone.publicMap();
                        if (zonePublicMap.containsKey("tmpId")) {
                            zonePublicMap.put("oldId", zoneMap.get("tmpId"));
                        }
                        mapResponse.put("status", "success");
                        mapResponse.put("id", zone.getId());
                        listMapResponse.add(mapResponse);
                        break;

                    case "delete":
                        if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new HashMap<>(), "zone:d")) {
                            throw new UserException("Permissions error: User does not have permission to delete zones.");
                        }
                        if (zoneMap.get("id") != null) {
                            zone = ZoneService.getInstance().get(((Number) zoneMap.get("id")).longValue());
                        }
                        if (zone == null) {
                            mapResponse.put("status", "error");
                            mapResponse.put("errorMessage", String.format("LocalMapId[%d] not found", ((Number) zoneMap.get("id"))));
                            mapResponse.put("id", zoneMap.get("id"));
                            listMapResponse.add(mapResponse);
                            break;
                        }
                        Long id = zone.getId();
                        validateDelete(zone);
                        List<String> messageErrors = deleteCurrentZone(zone, true, true, true);
                        if (!messageErrors.isEmpty()) {
                            for (int position = 0; position < messageErrors.size(); position++) {
                                messageErrors.set(position, "Zone could not be removed because there are Zone(s) that " + messageErrors.get(position));
                            }
                            mapResponse.put("status", "error");
                            mapResponse.put("errorMessage", StringUtils.join(messageErrors, ", "));
                            mapResponse.put("id", zone.getId());
                            listMapResponse.add(mapResponse);
                        } else {
                            mapResponse.put("status", "success");
                            mapResponse.put("id", id);
                            listMapResponse.add(mapResponse);
                        }
                        break;
                }
            } catch (UserException | ConstraintViolationException | StaleStateException e) {
                mapResponse.put("status", "error");
                mapResponse.put("errorMessage", e.getMessage());
                if (zoneMap.containsKey("name")) {
                    mapResponse.put("id", zoneMap.get("name"));
                }
                listMapResponse.add(mapResponse);
            }
        }
        return listMapResponse;
    }

    private ValidationBean validateZoneMap(Map<String, Object> zoneMap) {
        ValidationBean validationBean = new ValidationBean();
        if (!zoneMap.containsKey("operation")) {
            validationBean.setErrorDescription("[operation] is required.");
            return validationBean;
        }
        String operation = zoneMap.get("operation").toString();
        if (!operation.equals("add") && !operation.equals("update") && !operation.equals("delete")) {
            validationBean.setErrorDescription("Operation [" + operation + "] is not supported.");
            return validationBean;
        }
        String[] requiredFields = {"name", "code", "color", "localMap.id", "zoneGroup.id",
                "zoneType.id", "zoneProperties", "zonePoints"};
        if (operation.equals("add") || operation.equals("update")) {
            for (String requiredField : requiredFields) {
                if (!zoneMap.containsKey(requiredField)) {
                    validationBean.setErrorDescription("[" + requiredField + "] is required.");
                    return validationBean;
                }
                if ((zoneMap.get(requiredField) == null) || (zoneMap.get(requiredField).toString().isEmpty())) {
                    validationBean.setErrorDescription("[" + requiredField + "] is required, cannot be 'null' or empty.");
                    return validationBean;
                }
            }
            Long facilityId = Long.valueOf(zoneMap.get("localMap.id").toString());
            LocalMap tmpFacility = LocalMapService.getInstance().get(facilityId);
            if (tmpFacility == null) {
                return ValidationUtils.getInstance().newValidationError("Facility Id [" + facilityId + "] not found.");
            }
            zoneMap.put("facility", tmpFacility);
        }

        if ((zoneMap.get("description") != null) && (zoneMap.get("description").toString().length() > 255)) {
            validationBean.setErrorDescription("[description] should be have max 255 characters.");
            return validationBean;
        }

        // validating zone properties
        List<Map<String, Object>> zonePropertiesList = (List<Map<String, Object>>) zoneMap.get("zoneProperties");
        if (zonePropertiesList != null && !zonePropertiesList.isEmpty()){
            for (Map<String, Object> zonePropertyMap:zonePropertiesList) {
                if (!zonePropertyMap.containsKey("id")) {
                    validationBean.setErrorDescription("Zone property [id] is required.");
                    return validationBean;
                }
                ZoneProperty zoneProperty = ZonePropertyService.getInstance().get(Long.valueOf(zonePropertyMap.get("id").toString()));
                if (zoneProperty == null) {
                    validationBean.setErrorDescription("Zone property id [" + zonePropertyMap.get("id").toString() + "] not found.");
                    return validationBean;
                }
                if ((zonePropertyMap.get("value") != null) && (zonePropertyMap.get("value").toString().length() > 255)) {
                    validationBean.setErrorDescription("Zone property value for Zone property name [" + zoneProperty.getName() + "], should be have max 255 characters.");
                    return validationBean;
                }
            }
        }

        return validationBean;
    }

    public boolean isValidNewZoneCode(Map<String, Object> zoneMap, List<Map<String, Object>> zonesMap) {
        Integer count = 0;
        for (Map<String, Object> zoneAux:zonesMap) {
            if (compareField("code", zoneMap, zoneAux) || compareFieldGroup("code", zoneMap, zoneAux)) {
                count++;
            }
        }
        if (count > 1) {
            throw new UserException("Zone code [" + zoneMap.get("code").toString() + "] already exists.");
        }
        return true;
    }

    private boolean compareField(String field, Map<String, Object> zoneMap, Map<String, Object> zoneAux) {
        String field1 = zoneMap.get(field).toString();
        Long zoneGroupId1 = ((LocalMap)zoneMap.get("facility")).getId();
        String field2 = zoneAux.get(field).toString();
        Long zoneGroupId2 = ((LocalMap)zoneAux.get("facility")).getId();
        return (field1.equals(field2) && zoneGroupId1.equals(zoneGroupId2));
    }

    private boolean compareFieldGroup(String field, Map<String, Object> zoneMap, Map<String, Object> zoneAux) {
        String field1 = zoneMap.get(field).toString();
        Long zoneGroupId1 = Long.valueOf( zoneMap.get("group.id").toString());
        String field2 = zoneAux.get(field).toString();
        Long zoneGroupId2 = Long.valueOf( zoneAux.get("group.id").toString());
        return (field1.equals(field2) && zoneGroupId1.equals(zoneGroupId2));
    }

    public boolean isValidNewZoneName(String newZoneName, String zoneNameListStr, LocalMap localMap, Boolean isNewZone
            , String zoneCode) {
        if ((newZoneName == null) || newZoneName.isEmpty()) {
            throw new UserException("[New zone name] is required.");
        }
        if ((zoneNameListStr == null) || (zoneNameListStr.isEmpty())) {
            throw new UserException("[Zone name list] is required.");
        }
        String[] arrayNameList = zoneNameListStr.split(",");
        List<String> zoneNameList = new LinkedList<>(Arrays.asList(arrayNameList));
        zoneNameList.remove(newZoneName);
        if (zoneNameList.contains(newZoneName) && isNewZone) {
            throw new UserException("Zone name [" + newZoneName + "] already exists.");
        } else if (zoneNameList.contains(newZoneName)) {
            throw new UserException("You cannot update zone name to [" + newZoneName + "] because it already exists.");
        }
        try {
            Zone zone = ZoneService.getInstance().getByNameAndGroup(newZoneName, localMap.getGroup().getHierarchyName(false));
            if (zone != null && isNewZone) {
                throw new UserException("Zone name [" + newZoneName + "] already exists.");
            }
            if ((zone != null) && (!zone.getCode().equals(zoneCode))) {
                throw new UserException("You cannot update zone name to [" + newZoneName + "] because it already exists.");
            }
        } catch (NonUniqueResultException e) {
            throw new UserException(e.getMessage(), e);
        }
        return true;
    }

    public boolean isValidNewZoneCodeAndNewName(Map<String, Object> body, Boolean checkCode) {
        String[] requiredFields = {"isNew", "isNewGrid", "newZone", "zones"};
        for (String field:requiredFields) {
            if (body.get(field) == null) {
                throw new UserException("[" + field + "] is required.");
            }
        }
        Boolean isNewGrid = Boolean.valueOf(body.get("isNewGrid").toString());
        Boolean isNewZone = Boolean.valueOf(body.get("isNew").toString());

        if (!checkCode && isNewGrid && isNewZone) {
            // For new grid we are not doing validations in this section, for new grid validations are in newGridNames
            return true;
        }
        if (!(body.get("newZone") instanceof Map)) {
            throw new UserException("[newZone] should be an object.");
        }
        Map<String, Object> zoneMap = (Map) body.get("newZone");
        zoneMap.put("operation", "add");
        zoneMap.put("zoneProperties", new ArrayList<>());
        zoneMap.put("zonePoints", new ArrayList<>());
        ValidationBean validationBean = validateZoneMap(zoneMap);
        if (validationBean.isError()) {
            throw new UserException("Zone validation: " + validationBean.getErrorDescription());
        }
        if (!(body.get("zones") instanceof List)) {
            throw new UserException("[zones] should be a zone list.");
        }
        List<Map<String, Object>> zonesMap = (List) body.get("zones");
        for (Map<String, Object> tmpZone :zonesMap) {
            tmpZone.put("operation", "add");
            tmpZone.put("zoneProperties", new ArrayList<>());
            tmpZone.put("zonePoints", new ArrayList<>());
            validationBean = validateZoneMap(tmpZone);
            if (validationBean.isError()) {
                throw new UserException("Zone validation: " + validationBean.getErrorDescription());
            }
        }

        if (isNewZone) {
            isValidNewZoneCode(zoneMap, zonesMap);
        }
        return true;
    }

    public void moveOrResizeZones(LocalMap oldFacilityMap, LocalMap newFacilityMap) {
        Double scaleX = newFacilityMap.getImageWidth() / oldFacilityMap.getImageWidth();
        Double scaleY = newFacilityMap.getImageHeight() / oldFacilityMap.getImageHeight();
        List<Zone> zoneList = getZonesByLocalMap(oldFacilityMap.getId());
        Double midX = ((oldFacilityMap.getLonmax() - oldFacilityMap.getLonmin())/2) + oldFacilityMap.getLonmin();
        Double midY = ((oldFacilityMap.getLatmax() - oldFacilityMap.getLatmin())/2) + oldFacilityMap.getLatmin();
        Double angle = newFacilityMap.getRotationDegree() - oldFacilityMap.getRotationDegree();
        angle = Math.toRadians(angle);
        for (Zone zone : zoneList) {
            List<List<Double>> points = new LinkedList<>();
            Set<ZonePoint> zonePointList = zone.getZonePoints();
            for (ZonePoint zonePoint : zonePointList) {
                List<Double> point = new ArrayList<>();
                // rotate points
                Double new_x = ((zonePoint.getX() - midX) * Math.cos((angle)))
                        + ((midY - zonePoint.getY()) * Math.sin((angle))) + midX;
                Double new_y = ((midY - zonePoint.getY()) * Math.cos((angle)))
                        - ((zonePoint.getX() - midX) * Math.sin((angle))) + midY;
                logger.info(new_x + "\t" + new_y);
                // translate and resize
                point.add((new_x - oldFacilityMap.getLonmin()) * scaleX + newFacilityMap.getLonmin());
                point.add((new_y - oldFacilityMap.getLatmin()) * scaleY + newFacilityMap.getLatmin());
                point.add(new Double(zonePoint.getArrayIndex()));
                points.add(point);
            }
            updateZonePoints(zone, points);
            update(zone);
        }
        angle += 5.0;
        if (angle > 360) {
            angle = 5.0;
        }
    }
}
