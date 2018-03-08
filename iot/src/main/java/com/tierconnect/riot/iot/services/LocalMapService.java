package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.mysema.query.types.OrderSpecifier;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.services.GroupFieldService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.utils.EntityVisibility;
import com.tierconnect.riot.appcore.utils.GeneralVisibilityUtils;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.appcore.utils.VisibilityUtils;
import com.tierconnect.riot.commons.utils.CoordinateUtils;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.utils.ValidationUtils;
import com.tierconnect.riot.sdk.dao.UserException;
import com.tierconnect.riot.sdk.utils.BeanUtils;
import com.tierconnect.riot.sdk.utils.PermissionsUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;

import javax.annotation.Generated;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Pattern;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class LocalMapService extends LocalMapServiceBase
{
    private static Logger logger = Logger.getLogger(LocalMapService.class);
    /**
     * Calculate Latitude and Longitude maximus from local map data
     * @param localMap
     */
	public static void calculateLatLonMax(LocalMap localMap) {
		Double[] maxPoint = caculateMaxPoint(localMap.getLonmin(), localMap.getLatmin(),
                localMap.getImageWidth(), localMap.getImageHeight(),
                localMap.getImageUnit());

		localMap.setLonmax(maxPoint[0]);
		localMap.setLatmax(maxPoint[1]);
	}

    /**
     * Calculate maxPoint from minPoint, width, height and unit
     * @param lonMin
     * @param latMin
     * @param width
     * @param height
     * @param unit
     * @return
     */
    private static Double[] caculateMaxPoint(Double lonMin, Double latMin, Double width, Double height, String unit) {
        // constant in meters
        Double earthSemiMajorAxis = 6378137.0;

        if(unit.equals("ft" )) {
            earthSemiMajorAxis = earthSemiMajorAxis / 0.3048;
        }



		double f = Math.cos( latMin * Math.PI / 180 );

		Double delLon = (width / (earthSemiMajorAxis * f)) * (180 / Math.PI);
		Double delLat = (height / earthSemiMajorAxis) * (180 / Math.PI);

		Double[] maxPoint = new Double[2];
        maxPoint[0] = lonMin + delLon ;
		maxPoint[1] = latMin + delLat ;
	return maxPoint;
    }

    public static Map<String, Object> calculateLatLonMax(Map<String, Object> body) {
        if (!body.containsKey("imageWidth")) {
            throw new UserException("Facility map width is required");
        }
        if (!body.containsKey("imageHeight")) {
            throw new UserException("Facility map height is required");
        }
        if (!body.containsKey("lonmin")) {
            throw new UserException("Facility map minimum longitude is required");
        }
        if (!body.containsKey("latmin")) {
            throw new UserException("Facility map minimum latitude is required");
        }
        if (!body.containsKey("lonmax")) {
            throw new UserException("Facility map maximum longitude is required");
        }
        if (!body.containsKey("latmax")) {
            throw new UserException("Facility map maximum latitude is required");
        }
        if (!body.containsKey("imageUnit")) {
            throw new UserException("Facility map image Unit is required");
        }
        Double width = CoordinateUtils.getInstance().roundCoordinate(body.get("imageWidth").toString());
        Double height = CoordinateUtils.getInstance().roundCoordinate(body.get("imageHeight").toString());
        Double lonMin = CoordinateUtils.getInstance().roundCoordinate(body.get("lonmin").toString());
        Double latMin = CoordinateUtils.getInstance().roundCoordinate(body.get("latmin").toString());
        Double lonMax = CoordinateUtils.getInstance().roundCoordinate(body.get("lonmax").toString());
        Double latMax = CoordinateUtils.getInstance().roundCoordinate(body.get("latmax").toString());
        String units = body.get("imageUnit").toString();
        List<Map<String, Object>> mapPointList = (List<Map<String, Object>>)body.get("mapPoints");
        List<List<Double>> points;
        if ((mapPointList == null) || (mapPointList.isEmpty())) {
            return generateDefaultPoints(lonMin, latMin, width, height, units);
        } else {
            points = extractPointsFromMap((List<Map<String, Object>>) body.get("mapPoints"));
        }
        double p0X = points.get(0).get(0);
        double p0Y = points.get(0).get(1);
        double p1X = points.get(1).get(0);
        double p1Y = points.get(1).get(1);
        double p2X = points.get(2).get(0);
        double p2Y = points.get(2).get(1);
        CoordinateUtils cu = CoordinateUtils.getInstance();
        cu.setUnits(units);
        double oldWidth = CoordinateUtils.getInstance().calculateDistance(p0Y, p0X, p1Y, p1X);
        double oldHeight = CoordinateUtils.getInstance().calculateDistance(p1Y, p1X, p2Y, p2X);
//        width = oldWidth * 1.2;
//        height = oldHeight * 1.2;
        Map<String,Object> responseMap = calculateLocalMapPoints(
                points, // pointList List<List<Double>
                height, width, // new width and height
                latMin, lonMin, // Minimun Point
                units,
                oldWidth, oldHeight
                );
//        if (body.containsKey("lonOrigin")) {
//            Map<String, Object> newOriginPoint = calculateOriginFromNewSize(body, responseMap);
//            responseMap.putAll(newOriginPoint);
//        }
        return responseMap;
    }

    private static Map<String, Object> calculateOriginFromNewSize(Map<String, Object> body, Map<String, Object> responseMap) {
        Map<String, Object> bodyTmp = new HashMap<>();
        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("mapPoints", body.get("mapPoints"));
        oldValues.put("latmax", body.get("latMax"));
        oldValues.put("latmin", body.get("latMin"));
        oldValues.put("lonmax", body.get("lonMax"));
        oldValues.put("lonmin", body.get("lonMin"));
        oldValues.put("imageWidth", body.get("width"));
        oldValues.put("imageHeight", body.get("height"));
        oldValues.put("rotationDegree", body.get("rotationDegree"));
        oldValues.put("imageUnit", body.get("imageUnit"));
        oldValues.put("latOrigin", body.get("latOrigin"));
        oldValues.put("lonOrigin", body.get("lonOrigin"));
        if (body.containsKey("lonOriginNominal")) {
            oldValues.put("latOriginNominal", body.get("latOriginNominal"));
            oldValues.put("lonOriginNominal", body.get("lonOriginNominal"));
        }
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("mapPoints", responseMap.get("mapPoints"));
        newValues.put("latmax", responseMap.get("latMax"));
        newValues.put("latmin", body.get("latMin"));
        newValues.put("lonmax", responseMap.get("lonMax"));
        newValues.put("lonmin", body.get("lonMin"));
        newValues.put("imageWidth", Double.valueOf(body.get("width").toString()));
        newValues.put("imageHeight", Double.valueOf(body.get("height").toString()));
        newValues.put("rotationDegree", body.get("rotationDegree"));
        newValues.put("imageUnit", body.get("imageUnit"));
        bodyTmp.put("oldValues", oldValues);
        bodyTmp.put("newValues", newValues);
        return calculateOriginWithPoints(bodyTmp);}

    public List<LocalMap> selectByGroupId(BooleanBuilder be){
		OrderSpecifier order[] = QueryUtils.getOrderFields(QLocalMap.localMap,"name:asc");
		return  getLocalMapDAO().selectAll(be,null,order);
	}

    public List<LocalMap> selectAllByGroupId(long groupId)
    {
        return getLocalMapDAO().selectAllBy(QLocalMap.localMap.group.id.eq(groupId));
    }

	/**
	 * Return a Local Map by Code (Name)
	 * @param facilityCode
	 * @return a LocalMap
     */
	public LocalMap selectByCode(String facilityCode) {
		HibernateQuery query = getLocalMapDAO().getQuery();
		return query.where(QLocalMap.localMap.name.eq(facilityCode))
				.uniqueResult(QLocalMap.localMap);
	}

    /**
     *
     * @param mapLocalMap Local map object
     * @return ValidationBean with validation's error
     */
    private ValidationBean validateLocalMap(Map<String, Object> mapLocalMap) {
        // Validating operation
        if (!mapLocalMap.containsKey("operation")) {
            return ValidationUtils.getInstance().newValidationError("[operation] is required.");
        }
        String operation = mapLocalMap.get("operation").toString();
        if ((operation == null) || operation.isEmpty()) {
            return ValidationUtils.getInstance().newValidationError("[operation] cannot be null or empty.");
        }
        if (!operation.equals("add") && !operation.equals("update") && !operation.equals("delete")) {
            return ValidationUtils.getInstance().newValidationError("Operation [" + operation + "] is not supported.");
        }

        // Validating required fields
        // group, name, origin lat, origin lon, min lat, min lon, widht, height
        String[] requiredFields = {"name", "latOrigin", "lonOrigin", "latmin",
                "lonmin", "imageWidth", "imageHeight",  "imageUnit"};
        LocalMap localMap = null;
        if (operation.equals("add") || operation.equals("update")) {
            for (String requiredField : requiredFields) {
                if (!mapLocalMap.containsKey(requiredField)) {
                    return ValidationUtils.getInstance().newValidationError("[" + requiredField + "] is required.");
                }
                if ((mapLocalMap.get(requiredField) == null) || (mapLocalMap.get(requiredField).toString().isEmpty())) {
                    return ValidationUtils.getInstance().newValidationError("[" + requiredField + "] is required, it cannot be 'null' or empty.");
                }
            }
        }

        // Validating Local map id
        if (operation.equals("update") || operation.equals("delete")) {
            if (!mapLocalMap.containsKey("id")) {
                return ValidationUtils.getInstance().newValidationError("Local map [id] is required.");
            }
            if ((mapLocalMap.get("id") == null) || (mapLocalMap.get("id").toString().isEmpty())) {
                return ValidationUtils.getInstance().newValidationError("Local map [id] is required, it cannot be 'null' or empty.");
            }
            Long id = Long.valueOf(mapLocalMap.get("id").toString());
            localMap = LocalMapService.getInstance().get(id);
            if (localMap == null) {
                return ValidationUtils.getInstance().newValidationError("Local map id [" + id + "] not found.");
            }
            if (operation.equals("delete")) {
                return ValidationUtils.getInstance().newValidationOk();
            }
        }

		// validating required fields for add operation
		if (operation.equals("add")) {
			if ((mapLocalMap.get("image") == null) || (mapLocalMap.get("image").toString().isEmpty())) {
				return ValidationUtils.getInstance().newValidationError("[image] is required for new facility.");
			}
			if ((mapLocalMap.get("group") == null) || ((mapLocalMap.get("group").toString().isEmpty()))) {
                return ValidationUtils.getInstance().newValidationError("[group] is required for new facility.");
            }
		}

        // validating content fields
        if (mapLocalMap.get("name").toString().length() > 255) {
            return ValidationUtils.getInstance().newValidationError("[name] should be have max 255 characters.");
        }
        Pattern p = Pattern.compile("[^a-zA-Z0-9\\s]");
        if (p.matcher(mapLocalMap.get("name").toString()).find()) {
            return ValidationUtils.getInstance().newValidationError("[name] contains special characteres, it only should have letters or numbers [a-z, A-Z, 0-9].");
        }
        if ((mapLocalMap.get("description") != null) && (mapLocalMap.get("description").toString().length() > 255)) {
            return ValidationUtils.getInstance().newValidationError("[description] should be have max 255 characters.");
        }

        // validating image unit
        if (!mapLocalMap.get("imageUnit").equals("ft") && !mapLocalMap.get("imageUnit").equals("m")) {
            return ValidationUtils.getInstance().newValidationError("Image Unit [" + mapLocalMap.get("imageUnit").toString() + "] is not supported. It should be equal to 'ft' or 'm'");
        }

        if (mapLocalMap.get("group") != null) {
            if (!(mapLocalMap.get("group") instanceof Map)) {
                return ValidationUtils.getInstance().newValidationError("[group] should be an object group");
            }
            Map<String, Object> groupMap = (Map<String, Object>) mapLocalMap.get("group");
            if (!groupMap.containsKey("id")) {
                return ValidationUtils.getInstance().newValidationError("Group [id] is required.");
            }
            if ((groupMap.get("id") == null) || (groupMap.get("id").toString().isEmpty())) {
                return ValidationUtils.getInstance().newValidationError("Group [id] is required, it cannot be 'null' or empty.");
            }
        }

        // Validating coordinate values
        String numberStr = "Latitude Origin";
        String value = mapLocalMap.get("latOrigin").toString();
        try {
            if (!isValidLatitude(Double.valueOf(mapLocalMap.get("latOrigin").toString()))) {
                return ValidationUtils.getInstance().newValidationError("Latitude Origin [" + mapLocalMap.get("latOrigin").toString() + "] invalid, it should be between -90 and 90 degrees");
            }
            numberStr = "Longitude Origin";
            value = mapLocalMap.get("lonOrigin").toString();
            if (!isValidLongitude(Double.valueOf(mapLocalMap.get("lonOrigin").toString()))) {
                return ValidationUtils.getInstance().newValidationError("Longitude Origin [" + mapLocalMap.get("lonOrigin").toString() + "] invalid, it should be between -180 and 180 degrees");
            }
            numberStr = "Latitude min";
            value = mapLocalMap.get("latmin").toString();
            if (!isValidLatitude(Double.valueOf(mapLocalMap.get("latmin").toString()))) {
                return ValidationUtils.getInstance().newValidationError("Latitude Min [" + mapLocalMap.get("latmin").toString() + "] invalid, it should be between -90 and 90 degrees");
            }
            numberStr = "Longitude min";
            value = mapLocalMap.get("lonmin").toString();
            if (!isValidLongitude(Double.valueOf(mapLocalMap.get("lonmin").toString()))) {
                return ValidationUtils.getInstance().newValidationError("Longitude Min [" + mapLocalMap.get("lonmin").toString() + "] invalid, it should be between -180 and 180 degrees");
            }
        } catch (NumberFormatException e) {
            return ValidationUtils.getInstance().newValidationError(numberStr + " [" + value + "] is not a valid coordinate");
        }
        return ValidationUtils.getInstance().newValidationOk();
    }

	public  List<Map<String, Object>> bulkOperation(List<Map<String, Object>> listMap, EntityVisibility entityVisibility){
		List<Map<String, Object>> listMapResponse = new LinkedList<>();
		for (Map<String, Object> localMap : listMap) {
			Map<String,Object> mapResponse = new HashMap<>();
			ValidationBean validationBean = validateLocalMap(localMap);
			if (validationBean.isError()){
				mapResponse.put("status","400");
				mapResponse.put("errorMessage",validationBean.getErrorDescription());
				if (localMap.containsKey("id")){
					mapResponse.put("id",localMap.get("id"));
				}else{ if (localMap.containsKey("name") && localMap.get("name") != null){
					mapResponse.put("name",localMap.get("name"));
					}else{
					mapResponse.put("name","");
				}
				}
				listMapResponse.add(mapResponse);
				continue;
			}

            LocalMap facilityMap = null;
            String operation = localMap.get("operation").toString();

			Group group = null;
            Long groupId = null;
            if (!operation.equals("delete")) {
                if (localMap.containsKey("group")) {
                    Map<String, Object> groupObject = (Map<String, Object>) localMap.get("group");
                    groupId = new Long(groupObject.get("id").toString());
                    group = GroupService.getInstance().get(groupId);
                } else {
                    group = VisibilityUtils.getVisibilityGroup(LocalMap.class.getCanonicalName(), null);
                }
                if (group == null) {
                    mapResponse.put("status", "400");
                    mapResponse.put("errorMessage", "Group id [" + groupId + "] not found.");
                    if (localMap.containsKey("id")) {
                        mapResponse.put("id", localMap.get("id"));
                    } else {
                        if (localMap.containsKey("name") && localMap.get("name") != null) {
                            mapResponse.put("name", localMap.get("name"));
                        } else {
                            mapResponse.put("name", "");
                        }
                    }
                    listMapResponse.add(mapResponse);
                    continue;
                }
            }

            List<Long> groupMqtt = GroupService.getInstance().getMqttGroups(group);
			try {
				switch (operation) {
					case "add":
						if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new HashMap<>(), "localmap:i")) {
							throw new UserException("Permissions error: User does not have permission to create maps.");
						}
						Date transactiontime = new Date();
						facilityMap = new LocalMap();
						facilityMap.setGroup(group);
						facilityMap.setModifiedTime(transactiontime.getTime());
                        List<Map<String, Object>> mapPoints = (List<Map<String, Object>>)localMap.get("mapPoints");
                        localMap.remove("mapPoints");
						setFacilityValues(localMap, facilityMap);
						validateInsert(facilityMap);
						LocalMap facility = insert(facilityMap);
                        if (facility.getOpacity() != null){
                            udpateOpacity(facility.getId(), facility.getOpacity().intValue());
                        }
                        updateMapPoints(facilityMap, mapPoints);
                        localMap.remove("mapPoints");

						List<ZoneGroup> zoneGroups = new LinkedList<>();
						String nameFacility = facilityMap.getName();
						if (nameFacility != null && nameFacility.length() > 246){
							nameFacility= nameFacility.substring(0,nameFacility.length()-9);
						}

						ZoneGroup zoneGroupOnSite = new ZoneGroup();
						zoneGroupOnSite.setDescription((nameFacility != null ? nameFacility : "") + "On-Site");
						zoneGroupOnSite.setName("On-Site");
						zoneGroupOnSite.setGroup(group);
						zoneGroupOnSite.setLocalMap(facilityMap);

						ZoneGroup zoneGroupOffSite = new ZoneGroup();
						zoneGroupOffSite.setDescription((nameFacility != null ? nameFacility : "") + " Off-Site");
						zoneGroupOffSite.setName("Off-Site");
						zoneGroupOffSite.setGroup(group);
						zoneGroupOffSite.setLocalMap(facilityMap);

						zoneGroups.add(zoneGroupOnSite);
						zoneGroups.add(zoneGroupOffSite);

						facilityMap.setZoneGroup(zoneGroups);

						ZoneGroupService.getInstance().insert(zoneGroupOnSite);
						ZoneGroupService.getInstance().insert(zoneGroupOffSite);

						BrokerClientHelper.sendRefreshFacilityMapsMessage(false, groupMqtt);
						mapResponse.put("status", "200");
						mapResponse.put("id", facilityMap.getId());
						listMapResponse.add(mapResponse);

						break;
					case "update":
						if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new HashMap<>(), "localmap:u")) {
							throw new UserException("Permissions error: User does not have permission to update maps.");
						}
						if (localMap.get("id") != null) {
							facilityMap = get(((Number) localMap.get("id")).longValue());
						}
						if (facilityMap == null) {
							mapResponse.put("status", "400");
							mapResponse.put("errorMessage", validationBean.getErrorDescription());
							mapResponse.put("id", localMap.get("id"));
							listMapResponse.add(mapResponse);
							break;
						}
                        if (!group.equals(facilityMap.getGroup())) {
                            mapResponse.put("status", "400");
                            mapResponse.put("errorMessage", "Company for facility [" + facilityMap.getName() + "] cannot be updated");
                            mapResponse.put("id", localMap.get("id"));
                            listMapResponse.add(mapResponse);
                            break;
                        }
                        // is not necessary to update facility's group
//						facilityMap.setGroup(group);
                        mapPoints = (List<Map<String, Object>>)localMap.get("mapPoints");
                        localMap.remove("mapPoints");
						LocalMap oldFacilityMap = getCopy(facilityMap);
                        List<List<Double>> oldLocalMapPoints = extractPointsFromSet(oldFacilityMap.getLocalMapPoints());
						setFacilityValues(localMap, facilityMap);

						validateUpdate(facilityMap);
						LocalMap facilityUpdate = update(facilityMap);
                        if (facilityUpdate.getOpacity() != null){
                            udpateOpacity(facilityUpdate.getId(), facilityUpdate.getOpacity().intValue());
                        }
                        updateMapPoints(facilityMap, mapPoints);
                        localMap.remove("mapPoints");
						LocalMap newFacilityMap = getCopy(facilityMap);
						BrokerClientHelper.sendRefreshFacilityMapsMessage(false, groupMqtt);
						mapResponse.put("status", "200");
						mapResponse.put("id", facilityMap.getId());
                        try {
                            moveOrResizeZones(oldFacilityMap, newFacilityMap, oldLocalMapPoints);
                        } catch (Exception e) {
                            logger.error("There are an error updating zones", e);
                        }
						listMapResponse.add(mapResponse);
						break;

					case "delete":
						if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new HashMap<>(), "localmap:d")) {
							throw new UserException("Permissions error: User does not have permission to delete maps.");
						}
						if (localMap.get("id") != null) {
							facilityMap = get(((Number) localMap.get("id")).longValue());
						}
						if (facilityMap == null) {
							mapResponse.put("status", "400");
							mapResponse.put("errorMessage", validationBean.getErrorDescription());
							mapResponse.put("id", localMap.get("id"));
							listMapResponse.add(mapResponse);
							break;
						}

						GeneralVisibilityUtils.limitVisibilityDelete(entityVisibility, facilityMap);
						validateDelete(facilityMap);

						boolean cascadeDelete = false;
						if (localMap.get("cascadeDelete") != null && localMap.get("cascadeDelete").toString().equals("true")){
                            cascadeDelete = true;
                        }
						List<String> messageErrors = LocalMapService.getInstance().deleteCurrentLocalMap(facilityMap,cascadeDelete);
						if (!messageErrors.isEmpty()) {
							mapResponse.put("status", "400");
							mapResponse.put("errorMessage", StringUtils.join(messageErrors, ","));
							mapResponse.put("id", facilityMap.getId());
							listMapResponse.add(mapResponse);
						} else {
							mapResponse.put("status", "200");
							mapResponse.put("id", facilityMap.getId());
							listMapResponse.add(mapResponse);
                            BrokerClientHelper.sendRefreshFacilityMapsMessage(false, groupMqtt);
						}
						break;
				}
			} catch(NumberFormatException e) {
                mapResponse.put("status","400");
                mapResponse.put("errorMessage", "Is not possible to parse this number: " + e.getMessage());
                if (localMap.containsKey("name"))
                    mapResponse.put("name",localMap.get("name"));
                listMapResponse.add(mapResponse);
            } catch (UserException e){
				mapResponse.put("status","400");
				mapResponse.put("errorMessage",e.getMessage());
				if (localMap.containsKey("name"))
					mapResponse.put("name",localMap.get("name"));
				listMapResponse.add(mapResponse);

			}
		}
		return listMapResponse;
	}

    private List<List<Double>> extractPointsFromSet(Set<LocalMapPoint> localMapPoints) {
        List<List<Double>> responseList = new ArrayList<>();
        for (LocalMapPoint point:localMapPoints) {
            List<Double> newPoint = new ArrayList<>();
            newPoint.add(point.getX());
            newPoint.add(point.getY());
            responseList.add(newPoint);
        }
        return responseList;
    }

    private void moveOrResizeZones(LocalMap oldFacilityMap, LocalMap newFacilityMap, List<List<Double>> oldLocalMapPoints) {
        Map<String, Object> oldLocalMapTmp = valuesFromLocalMap(oldFacilityMap);
        LocalMapTmp oldLocalMap = new LocalMapTmp(oldLocalMapTmp);
        oldLocalMap.generateMapPoints(oldLocalMapTmp);
        LocalMapTmp newLocalMap = new LocalMapTmp(valuesFromLocalMap(newFacilityMap));
        oldLocalMap.calculateBounds(newLocalMap, oldLocalMapPoints);

        List<Zone> zoneList = ZoneService.getZonesByLocalMap(oldFacilityMap.getId());
        for (Zone zone : zoneList) {
            Set<ZonePoint> zonePointList = zone.getZonePoints();
            Double points[][] = new Double[zonePointList.size()][3];
            int i = 0;
            for (ZonePoint zonePoint : zonePointList) {
                points[i][0] = zonePoint.getX();
                points[i][1] = zonePoint.getY();
                points[i][2] = Double.valueOf(zonePoint.getArrayIndex().toString());
                i++;
            }
            List<List<Double>> updatedPoints = oldLocalMap.transformPoints(points, newLocalMap);
            ZoneService.getInstance().updateZonePoints(zone, updatedPoints);
            ZoneService.getInstance().update(zone);
        }
    }

    private Map<String, Object> valuesFromLocalMap(LocalMap localMap) {
        Map<String, Object> mapResponse = new HashMap<>();
        if (localMap.getLocalMapPoints() == null) {
            Set<LocalMapPoint> mapPoints = new HashSet<LocalMapPoint>(LocalMapPointService.getInstance().getLocalMapPointByMap(localMap));
            localMap.setLocalMapPoints(mapPoints);
        }
        mapResponse.put("mapPoints", localMap.getMapPointsSorted());
        mapResponse.put("latmax", localMap.getLatmax());
        mapResponse.put("latmin", localMap.getLatmin());
        mapResponse.put("lonmax", localMap.getLonmax());
        mapResponse.put("lonmin", localMap.getLonmin());
        mapResponse.put("imageWidth", localMap.getImageWidth());
        mapResponse.put("imageHeight", localMap.getImageHeight());
        mapResponse.put("rotationDegree", localMap.getRotationDegree());
        mapResponse.put("imageUnit", localMap.getImageUnit());
        mapResponse.put("latOrigin", localMap.getLatOrigin());
        mapResponse.put("lonOrigin", localMap.getLonOrigin());
        return mapResponse;
    }

    public static List<String> deleteCurrentLocalMap(LocalMap localMap, boolean cascadeDelete){
		List<String> errorMessages = new ArrayList<>();
		if (cascadeDelete == false) {
		    // zone validation
            List<Zone> zonesOfFacility = ZoneService.getZonesByLocalMap(localMap.getId());
            if (zonesOfFacility.size() > 0) {
                errorMessages.add("Facility has references in Zones.");
            }
        } else {
            // Starting to delete facility in cascade
            List<Zone> zonesOfFacility = ZoneService.getZonesByLocalMap(localMap.getId());
            if (zonesOfFacility.size() > 0) {
                if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new HashMap<>(), "zone:d")) {
                    throw new UserException("Permissions error: User does not have permission to delete zones.");
                }
                for (Zone zone : zonesOfFacility){

                    // get logical readers for each zone and delete it
                    List<LogicalReader> logicalReaderList = LogicalReaderService.getInstance().selectAllByZone(zone);
                    if (!logicalReaderList.isEmpty()) {
                        if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new HashMap<>(), "logicalReader:d")) {
                            throw new UserException("Permissions error: User does not have permission to delete logical readers.");
                        }
                        for (LogicalReader logicalReader:logicalReaderList) {
                            // delete logicalReader
                            LogicalReaderService.getInstance().delete( logicalReader );
                        }
                    }

                    // get shifts for each zone and delete it
                    List<ShiftZone> shiftZoneList = ShiftZoneService.getInstance().selectAllByZone(zone);
                    if (!shiftZoneList.isEmpty()) {
                        if (!PermissionsUtils.buildSearch(SecurityUtils.getSubject(), new HashMap<>(), "shift:d")) {
                            throw new UserException("Permissions error: User does not have permission to delete shifts.");
                        }
                        for (ShiftZone shiftZone : shiftZoneList) {
                            // delete shift zone
                            if (shiftZone.getShift() != null) {
                                ShiftZoneService.getInstance().deleteAllByShift(shiftZone.getShift());
                                // delete shift
                                if (ShiftService.getInstance().get(shiftZone.getShift().getId()) != null) {
                                    ShiftService.getInstance().delete(shiftZone.getShift());
                                }
                            }
                        }
                    }
                    List<String> messageErrors = ZoneService.getInstance().deleteCurrentZone(zone, true, false, false);
                    if (messageErrors.size() > 0) {
                        errorMessages = messageErrors;
                    }
                }
            }
        }
		if (errorMessages.isEmpty()){
            LocalMapService.getInstance().delete(localMap);
        }
		return errorMessages;
	}

	public List<Map<String, Object>> fillChildren(Map map, BooleanBuilder mapBe, String where, String ownerName){
		BooleanBuilder mBe = new BooleanBuilder(mapBe);
		List<Map<String, Object>> listLocalMap = new LinkedList<>();
		Long groupId = (Long) map.get("id");
		mBe = mBe.and(QLocalMap.localMap.group.id.eq(groupId));
		List<LocalMap> localMap = LocalMapService.getInstance().selectByGroupId(mBe);

		List<Map<String, Object>> listChildren = (List<Map<String, Object>>) map.get("children");

		if (listChildren.size() > 0){
			List<Map<String, Object>> toRemove = new ArrayList<>();
			for (Map childrenMap : listChildren){
				childrenMap.put("mapMaker",fillChildren(childrenMap,mapBe,where,ownerName));
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

		for (LocalMap loMap:localMap){
			Map mapFacility = new HashMap<>();
			if (PermissionsUtils.isPermittedAny(SecurityUtils.getSubject(), "localmap:r:"+loMap.getId())){
				mapFacility.put("localMap", loMap.publicMap());
			}
			listLocalMap.add(mapFacility);
		}

		return listLocalMap;

	}

	public static void setFacilityValues(Map<String,Object> localMap, LocalMap facilityMap){
        Map<String,Object> temporalMap = localMap;
        temporalMap.remove("operation");
        BeanUtils.setProperties(temporalMap, facilityMap);
		facilityMap.setName((String) localMap.get("name"));
		facilityMap.setLonmin(new Double(localMap.get("lonmin").toString()));
		facilityMap.setLatmin(new Double(localMap.get("latmin").toString()));
		if (localMap.get("altOrigin") != null && !localMap.get("altOrigin").toString().isEmpty()) {
			facilityMap.setAltOrigin(new Double(localMap.get("altOrigin").toString()));
		}else{
			facilityMap.setAltOrigin(0.0);
			localMap.put("altOrigin","0.0");
		}
		if (localMap.get("declination") != null && !localMap.get("declination").toString().isEmpty()) {
			facilityMap.setDeclination(new Double(localMap.get("declination").toString()));
		}else{
			facilityMap.setDeclination(0.0);
			localMap.put("declination","0.0");
		}
		facilityMap.setLatOrigin(new Double(localMap.get("latOrigin").toString()));
		facilityMap.setLonOrigin(new Double(localMap.get("lonOrigin").toString()));


		facilityMap.setImageWidth(new Double(localMap.get("imageWidth").toString()));
		facilityMap.setImageHeight(new Double(localMap.get("imageHeight").toString()));
		if (localMap.get("latOriginNominal") != null) {
			facilityMap.setLatOriginNominal(new Double(localMap.get("latOriginNominal").toString()));
			facilityMap.setXNominal(new Double(localMap.get("xNominal").toString()));
		}else{
			facilityMap.setLatOriginNominal(facilityMap.getLatOrigin());
			facilityMap.setXNominal(0.0);
		}
		if (localMap.get("lonOriginNominal") != null) {
			facilityMap.setLonOriginNominal(new Double(localMap.get("lonOriginNominal").toString()));
			facilityMap.setYNominal(new Double(localMap.get("yNominal").toString()));
		}else{
			facilityMap.setLonOriginNominal(facilityMap.getLonOrigin());
			facilityMap.setYNominal(0.0);
		}
		if (facilityMap.getDescription() != null) {
			facilityMap.setDescription(localMap.get("description").toString());
		}
        if (localMap.get("opacity") != null){
            facilityMap.setOpacity(Long.parseLong(localMap.get("opacity").toString()));
        }
		facilityMap.setImageUnit(localMap.get("imageUnit").toString());

		if (localMap.get("image") != null) {
			String temporal;
			String inputStream = "";
			temporal = new String((String) localMap.get("image"));
			inputStream = temporal.substring(22);

			byte[] decodedBytes;
			decodedBytes = Base64.decodeBase64(inputStream.getBytes(Charsets.UTF_8));
			if (!Arrays.equals(decodedBytes, facilityMap.getImage())) {
				Date transactionTime = new Date();
				facilityMap.setModifiedTime(transactionTime.getTime());
			}
			facilityMap.setImage(decodedBytes);
		}
		if (localMap.get("rotationDegree") != null){
            Double rotationDegree = Double.valueOf(localMap.get("rotationDegree").toString()) % 360;
            if (rotationDegree < 0){
                rotationDegree += 360;
            }
            facilityMap.setRotationDegree(rotationDegree);
		}

		if (localMap.get("lonmax") == null && localMap.get("latmax") == null) {
			calculateLatLonMax(facilityMap);
		}else{
			if (localMap.get("lonmax") != null)
				facilityMap.setLonmax(new Double(localMap.get("lonmax").toString()));
			if (localMap.get("latmax") != null)
				facilityMap.setLatmax(new Double(localMap.get("latmax").toString()));
		}
	}

    /**
     * To calculate origin point (longitude, longitude)
     * @param id Local Map ID
     * @param latitude Latitude value
     * @param longitude Longitude value
     * @param xValue Coordinate X value
     * @param yValue Coordinate Y value
     * @return Origin point (longitude, longitude)
     */
    public static Map<String, Object> calculateOrigin(String imageUnit, Double latitude, Double longitude, Double xValue, Double yValue) {
        Map<String, Object> mapResponse = new HashMap<>();
        // validating longitude
        if (!isValidLongitude(longitude)) {
            mapResponse.put("error","Longitude [" + longitude + "] invalid, it should be between -180 and 180 degrees");
            return mapResponse;
        }
        // validating latitude
        if (!isValidLatitude(latitude)) {
            mapResponse.put("error","Latitude [" + latitude + "] invalid, it should be between -90 and 90 degrees");
            return mapResponse;
        }
		if (imageUnit == null) {
			mapResponse.put("error", "[imageUnit] is required.");
            return mapResponse;
		}
        if (!imageUnit.equals("m") && !imageUnit.equals("ft")) {
            mapResponse.put("error", "[imageUnit] should be equal to 'm' or 'ft'.");
            return mapResponse;
        }
        CoordinateUtils coordinateUtils = new CoordinateUtils(longitude, latitude, 0, 0, imageUnit);
        double[] origin = coordinateUtils.xy2lonlat(-xValue, -yValue, 0);
        if (origin.length != 3) {
            mapResponse.put("error", "Error calculating origin point");
            return mapResponse;
        }
        DecimalFormat decimalFormat = new DecimalFormat("###.######");
        mapResponse.put("latitude", Double.valueOf(decimalFormat.format(origin[1])));
        mapResponse.put("longitude", Double.valueOf(decimalFormat.format(origin[0])));
        mapResponse.put("elevation", origin[2]);
        return mapResponse;
    }

    private static Boolean isValidLatitude(Double latitude) {
        return ((latitude >= -90) && (latitude <= 90));
    }

    private static Boolean isValidLongitude(Double longitude) {
        return ((longitude >= -180) && (longitude <= 180));
    }

	public static Map<String, Object> setScale(Long id, String pointA, String pointB, Double distance) {
		Map<String, Object> result = new HashMap<>();
		ValidationBean validationBean = new ValidationBean();
		LocalMap localMap = LocalMapService.getInstance().get(id);
		if (localMap == null) {
			result.put("error", "Local Map [" + id + "] not found.");
		}
		validationBean = validate(pointA);
		if (validationBean.isError()) {
			result.put("error", validationBean.getErrorDescription());
			return result;
		}
		validationBean = validate(pointB);
		if (validationBean.isError()) {
			result.put("error", validationBean.getErrorDescription());
			return result;
		}
		if (!ThingTypeFieldService.getInstance().isNumberFloat(distance)) {
			String message = "Distance [" + distance + "] is not a valid number";
			result.put("error", message);
			return result;
		}
		try {
            String[] pAStr = pointA.split(",");
            String[] pBStr = pointB.split(",");
            String lat1Str = pAStr[0];
            double lat1 = new Double(pAStr[0]);
            double lon1 = new Double(pAStr[1]);
            double lat2 = new Double(pBStr[0]);
            double lon2 = new Double(pBStr[1]);
            double distanceAB = new CoordinateUtils(0, 0, 0, 0, localMap.getImageUnit()).calculateDistance(lat1, lon1, lat2, lon2);
            result.put("distance", distanceAB);
            result.put("scale", distanceAB/distance);
        } catch (Exception e){
			result.put("error", "Error: "+e.getMessage());
		}
		return result;
	}

	private static Float convert(String unit, String imageUnit, Float d) {
		if (unit.equals(imageUnit)) {
			return d;
		}
		if (imageUnit.equals("ft")) {
			return d/0.3048f;
		}
		return d*0.3048f;
	}

	private static ValidationBean validate(String point) {
		ValidationBean result = new ValidationBean();
		String[] points = point.split(",");
		if (points.length != 2) {
			result.setErrorDescription("Point [" + point + "] should be equal to two coordinates separed by colon (latitude,longitude)");
			return result;
		}
		if (!ThingTypeFieldService.getInstance().isNumberFloat(points[0])) {
			result.setErrorDescription("Latitude [" + points[0] + "] is not a valid number");
			return result;
		}
		if (!ThingTypeFieldService.getInstance().isNumberFloat(points[1])) {
			result.setErrorDescription("Longitude [" + points[1] + "] is not a valid number");
			return result;
		}
		return result;
	}

    public static void updateMapPoints(LocalMap localMap, List<Map<String, Object>> map) {
        List<List<Double>> points;
        if (map == null) {
            points = generateDefaultPoints(localMap);
        } else {
            points = extractPointsFromMap(map);
        }
        updateMapPointsWithPoints(localMap, points);
    }

    public static void updateMapPointsWithPoints(LocalMap localMap, List<List<Double>> points) {
            Set<LocalMapPoint> oldMapPoints = localMap.getLocalMapPoints();
            if (oldMapPoints != null) {
                Iterator<LocalMapPoint> iterator = oldMapPoints.iterator();
                while (iterator.hasNext()) {
                    LocalMapPoint mapPoint = iterator.next();
                    iterator.remove();
                    LocalMapPointService.getInstance().delete(mapPoint);
                }
            }
            localMap.setLocalMapPoints(null);
        Set<LocalMapPoint> localMapPoints = doublePointsToSet(points);
            for (LocalMapPoint mapPoint:localMapPoints) {
                mapPoint.setLocalMap(localMap);
                LocalMapPointService.getInstance().insert(mapPoint);
            }
            localMap.setLocalMapPoints(localMapPoints);
            LocalMapService.getInstance().update(localMap);
    }

    private static Set<LocalMapPoint> doublePointsToSet(List<List<Double>> points) {
        Set<LocalMapPoint> localMapPoints = new LinkedHashSet<>();
        long position = 0;
        for(List<Double> point : points) {
            LocalMapPoint mapPoint = new LocalMapPoint();
            mapPoint.setX(point.get(0));
            mapPoint.setY(point.get(1));
            if(point.size() > 2) {
                mapPoint.setArrayIndex(point.get(2).longValue());
            } else {
                mapPoint.setArrayIndex(position++);
            }
            localMapPoints.add(mapPoint);
        }
        return localMapPoints;
    }

    private static List<List<Double>> extractPointsFromMap(List<Map<String, Object>> mapPoints) {
        List<List<Double>> answer = new ArrayList<>();
        for (Map<String, Object> mapPoint:mapPoints) {
            List<Double> point = new LinkedList<>();
            point.add(CoordinateUtils.getInstance().roundCoordinate(mapPoint.get("x").toString()));
            point.add(CoordinateUtils.getInstance().roundCoordinate(mapPoint.get("y").toString()));
            Double index = answer.size() + 0.0;
            if(mapPoint.containsKey("arrayIndex")) {
                index = new Double(mapPoint.get("arrayIndex").toString());
            }
            point.add(index);
            answer.add(point);
        }
        return answer;
    }

    private static List<List<Double>> generateDefaultPoints(LocalMap localMap) {
        List<List<Double>> defaultMapPoints = new ArrayList<>();
        List<Double> mapPoint0 = new ArrayList<>();
        mapPoint0.add(localMap.getLonmin());
        mapPoint0.add(localMap.getLatmin());
        mapPoint0.add(0.0);
        List<Double> mapPoint1 = new ArrayList<>();
        mapPoint1.add(localMap.getLonmax());
        mapPoint1.add(localMap.getLatmin());
        mapPoint1.add(1.0);
        List<Double> mapPoint2 = new ArrayList<>();
        mapPoint2.add(localMap.getLonmax());
        mapPoint2.add(localMap.getLatmax());
        mapPoint2.add(2.0);
        List<Double> mapPoint3 = new ArrayList<>();
        mapPoint3.add(localMap.getLonmin());
        mapPoint3.add(localMap.getLatmax());
        mapPoint3.add(3.0);
        defaultMapPoints.add(mapPoint0);
        defaultMapPoints.add(mapPoint1);
        defaultMapPoints.add(mapPoint2);
        defaultMapPoints.add(mapPoint3);
        return defaultMapPoints;
    }

	public static List<Double> mapPoint(Double lonMin, Double latMin,
                                        Double x, Double y,
                                        Double x1, Double y1,
                                        double newLenght, int indice,
                                        double oldLenght, String units){
		List<Double> pointResponse = new ArrayList<>();

//		CoordinateUtils cu = new CoordinateUtils(lonMin, latMin, facilityMap.getAltOrigin(),facilityMap.getDeclination(), facilityMap.getImageUnit());
		CoordinateUtils cu = CoordinateUtils.getInstance();
        cu.setUnits(units);
		double addedValue = cu.calculateDistance(lonMin, 0.0, x, 0.0);
		double addedValueY = cu.calculateDistance(0.0, latMin, 0.0, y);
		double measure = cu.calculateDistance(x, 0.0, x1, 0.0);
		Double totalLength = ((measure * newLenght) / oldLenght);

		switch (indice){
			case 0:
				double[] points = cu.xy2lonlat(totalLength, 0, 0, x1, y1, 0);
				pointResponse.add(points[0]);
				pointResponse.add(latMin);
				pointResponse.add(Double.valueOf(indice));
				break;
			case 1:
				addedValueY = cu.calculateDistance(0.0, y, 0.0, y1);
				totalLength = totalLength + addedValue;
				double measureY = cu.calculateDistance(0.0, latMin, 0.0, y1);
				Double totalY = ((measureY * newLenght) / oldLenght);
				double[] points2 = cu.xy2lonlat(totalLength, totalY, 0, x1, y1, 0);
				pointResponse.add(points2[0]);
				pointResponse.add(points2[1]);
				pointResponse.add(Double.valueOf(indice));
				break;
			case 2:
				CoordinateUtils cu2 = new CoordinateUtils(lonMin, latMin, 0.0,20, "ft");
				double measureX = cu2.calculateDistance(x1, 0.0, x, 0.0);
				Double totalLengthX = ((measureX * newLenght) / oldLenght);
				double measureW = cu2.calculateDistance(0.0, y1, 0.0, y);
				Double totalW = ((measureW * newLenght) / oldLenght);
				double[] points4 = cu2.xy2lonlat(totalLengthX, totalW, 0, x, y, 0);
				pointResponse.add(points4[0]);
				pointResponse.add(points4[1]);
				pointResponse.add(Double.valueOf(indice));
				break;
			case 3:
				double measurePoint = cu.calculateDistance(0.0, y, 0.0, y1);
				Double totalPoint = ((measurePoint * newLenght) / oldLenght);
				double[] points3 = cu.xy2lonlat(0, totalPoint, 0, x, y, 0);
				pointResponse.add(lonMin);
				pointResponse.add(points3[1]);
				pointResponse.add(Double.valueOf(indice));
				break;
		}
		return  pointResponse;
	}

	public static Map<String, Object> calculateLocalMapPoints(List<List<Double>> localPoints,
															   double height, double width,
															   double latMin, double lonMin,
															   String imageUnit,
															   double oldWidth,
															   double oldHeight){

		Map <String, Object> mapResponse = new HashMap<>();
		List<List<Double>> listResponse = new ArrayList<>();
		listResponse = calculatePoints(localPoints, height, oldHeight, width, oldWidth, latMin, lonMin);
        List<Double> maxPoint = listResponse.get(4);
        listResponse.remove(4);
		mapResponse.put("mapPoints", doublePointsToSet(listResponse));
		mapResponse.put("latMax", maxPoint.get(1));
		mapResponse.put("lonMax", maxPoint.get(0));
		return mapResponse;

	}

    public static List<List<Double>> calculatePoints(List<List<Double>> listPoints, double A, double a, double B, double b, double latMin, double lonMin) {
        double lonMax = 0, latMax = 0;
        double py0 = listPoints.get(0).get(1);
        double py1 = listPoints.get(1).get(1);
        double py2 = listPoints.get(2).get(1);
        double py3 = listPoints.get(3).get(1);

        List<List<Double>> listAux = null;
        if (py0 == latMin || (py0 < py1  && py0 < py3)){
            listAux = generatePoints(listPoints, A, a, B, b, latMin, lonMin);
            lonMax = listAux.get(1).get(0);
            latMax = listAux.get(2).get(1);
        }else{
            if (py1 == latMin || (py1 < py0 && py1 < py2)){
                List<Double> aux = listPoints.get(0);
                listPoints.remove(0);
                listPoints.add(aux);
                listAux = generatePoints(listPoints, B, b, A, a, latMin, lonMin);
                aux = listAux.get(3);
                listAux.remove(3);
                listAux.add(0,aux);
                lonMax = listAux.get(2).get(0);
                latMax = listAux.get(3).get(1);
            }else{
                if (py3 == latMin || (py3 < py0  && py3 <py2)){
                    List<Double> aux = listPoints.get(3);
                    listPoints.remove(3);
                    listPoints.add(0,aux);
                    listAux = generatePoints(listPoints, B, b, A, a, latMin, lonMin);
                    aux = listAux.get(0);
                    listAux.remove(0);
                    listAux.add(aux);
                    lonMax = listAux.get(0).get(0);
                    latMax = listAux.get(1).get(1);
                }else{
                    List<Double> listAux2 = listPoints.get(2);
                    List<Double> listAux3 = listPoints.get(3);
                    listPoints.remove(2);
                    listPoints.remove(2);
                    listPoints.add(0,listAux2);
                    listPoints.add(1,listAux3);
                    listAux = generatePoints(listPoints, A, a, B, b, latMin, lonMin);
                    listAux2 = listAux.get(0);
                    listAux3 = listAux.get(1);
                    listAux.remove(0);
                    listAux.remove(0);
                    listAux.add(listAux2);
                    listAux.add(listAux3);
                    lonMax = listAux.get(3).get(0);
                    latMax = listAux.get(0).get(1);
                }
            }
        }
        List<Double> maxPoint = new ArrayList<>();
        maxPoint.add(lonMax);
        maxPoint.add(latMax);
        listAux.add(maxPoint);
        return listAux;
    }


    public static List<List<Double>> generatePoints(List<List<Double>> listPoints, double A, double a, double B, double b, double latMin, double lonMin){
        CoordinateUtils cu = new CoordinateUtils(lonMin, latMin, 0, 0, "ft");
        double aux[];
        // Changing points from (lon, lat) to (x, y)
        for (List<Double> point:listPoints) {
            aux = cu.lonlat2xy(point.get(0).doubleValue(), point.get(1).doubleValue(), 0);
            point.add(0, aux[0]);
            point.add(1, aux[1]);
        }
        aux = cu.lonlat2xy(lonMin, latMin, 0);
        lonMin = aux[0];
        latMin = aux[1];

		double p0x = listPoints.get(0).get(0);
		double p0y = listPoints.get(0).get(1);
		double p1x = listPoints.get(1).get(0);
		double p1y = listPoints.get(1).get(1);
		double p2x = listPoints.get(2).get(0);
		double p2y = listPoints.get(2).get(1);
		double p3x = listPoints.get(3).get(0);
		double p3y = listPoints.get(3).get(1);

		double x0 = p0x-lonMin;
		double xp0= (x0 * A) / a;

		List <Double> np0 = new ArrayList<>();
		np0.add(xp0 + lonMin);
		np0.add(latMin);

		double x1 = p1x - p0x;
		double xp1 = (x1 *B) / b;
		double y1 = p1y - latMin;
		double yp1 = (y1 * B) / b;

		List <Double> np1 = new ArrayList<>();
		np1.add(xp1 + np0.get(0));
		np1.add(yp1 + latMin);

		double y3 = p3y - latMin;
		double yp3 = (y3 * A)/ a;

		List <Double> np3 = new ArrayList<>();
		np3.add(lonMin);
		np3.add(yp3 + latMin);

		double x2 = p2x - lonMin;
		double xp2 = (x2 * B) / b;
		double y2 = p2y - p3y;
		double yp2 = (y2 * B) / b;

		List <Double> np2 = new ArrayList<>();
		np2.add(xp2 + lonMin);
		np2.add(yp2 + np3.get(1));

        aux = cu.xy2lonlat(np0.get(0).doubleValue(), np0.get(1).doubleValue(), 0);
        np0.set(0, aux[0]);
        np0.set(1, aux[1]);
        aux = cu.xy2lonlat(np1.get(0).doubleValue(), np1.get(1).doubleValue(), 0);
        np1.set(0, aux[0]);
        np1.set(1, aux[1]);
        aux = cu.xy2lonlat(np2.get(0).doubleValue(), np2.get(1).doubleValue(), 0);
        np2.set(0, aux[0]);
        np2.set(1, aux[1]);
        aux = cu.xy2lonlat(np3.get(0).doubleValue(), np3.get(1).doubleValue(), 0);
        np3.set(0, aux[0]);
        np3.set(1, aux[1]);

		List <List<Double>> pointsResponse = new ArrayList<>();

		pointsResponse.add(np0);
		pointsResponse.add(np1);
		pointsResponse.add(np2);
		pointsResponse.add(np3);

		return  pointsResponse;
	}

    private static Map<String, Object> generateDefaultPoints(double lonMin, double latMin, Double width, Double height, String imageUnit) {
        Map<String,Object> resultValues = new HashMap<>();
        List<List<Double>> listPointsResponse = new ArrayList<>();
        Double maxPoint[] = caculateMaxPoint(lonMin, latMin, width, height, imageUnit);

        List<Double> p0 = new ArrayList<>();
        p0.add(lonMin);
        p0.add(latMin);
        p0.add(0.0);
        listPointsResponse.add(p0);
        List<Double> p1 = new ArrayList<>();
        p1.add(maxPoint[0]); // maxX
        p1.add(latMin); // minY
        p1.add(1.0);
        listPointsResponse.add(p1);
        List<Double> p2 = new ArrayList<>();
        p2.add(maxPoint[0]); // maxX
        p2.add(maxPoint[1]); // maxY
        p2.add(2.0);
        listPointsResponse.add(p2);
        List<Double> p3 = new ArrayList<>();
        p3.add(lonMin); // minX
        p3.add(maxPoint[1]); // maxY
        p3.add(3.0);
        listPointsResponse.add(p3);
        resultValues.put("mapPoints", doublePointsToSet(listPointsResponse));
        resultValues.put("latMax", maxPoint[1]);
        resultValues.put("lonMax", maxPoint[0]);
        return resultValues;
    }

    public static Map<String, Object> calculateOriginWithPoints(Map<String, Object> body) {
        Map<String, Object> mapResponse = new HashMap<>();
        Map<String, Object> oldValues = (Map<String, Object>)body.get("oldValues");
        Map<String, Object> newValues = (Map<String, Object>)body.get("newValues");
        if ((body.get("resizeMapPoints") != null) && (Boolean.valueOf(body.get("resizeMapPoints").toString()))) {
//            oldValues.put("mapPoints", newValues.get("mapPoints"));
            oldValues.put("rotationDegree", newValues.get("rotationDegree"));
            Map<String, Object> mapPoints = calculateLatLonMax(newValues);

            mapResponse.putAll(mapPoints);
            newValues.remove("latmax");
            newValues.remove("lonmax");
            newValues.put("latmax", new Double(mapPoints.get("latMax").toString()));
            newValues.put("lonmax", new Double(mapPoints.get("lonMax").toString()));
//            oldValues.put("imageWidth", 17);
        } else {
            Map<String, Object> widthAndHeigth = getWidthAndHeight(oldValues, newValues);
            mapResponse.putAll(widthAndHeigth);
        }
        LocalMapTmp oldLocalMap = new LocalMapTmp(oldValues);
        LocalMapTmp newLocalMap = new LocalMapTmp(newValues);
//        if (body.containsKey("resizeMapPoints") && (Boolean.valueOf(body.get("resizeMapPoints").toString()))) {
//        }
        List<Double> newOrigin = oldLocalMap.getNewOrigin(newLocalMap);
        mapResponse.put("lonOrigin", newOrigin.get(0));
        mapResponse.put("latOrigin", newOrigin.get(1));
        List<Double> newOriginNominal = oldLocalMap.getNewOriginNominal(newLocalMap);
        if (!newOriginNominal.isEmpty()) {
            mapResponse.put("lonOriginNominal", newOriginNominal.get(0));
            mapResponse.put("latOriginNominal", newOriginNominal.get(1));
        }
        return mapResponse;
    }

    private static Map<String, Object> getWidthAndHeight(Map<String, Object> oldValues, Map<String, Object> newValues) {
        Map<String, Object> mapResponse = new HashMap<>();
        double angle = Double.valueOf(newValues.get("rotationDegree").toString()).doubleValue() -
                Double.valueOf(oldValues.get("rotationDegree").toString()).doubleValue();
        double imageWidth = 0;
        double imageHeight = 0;
        if (angle == 0) {
            List<Map<String, Object>> mapPoints = (List<Map<String, Object>>)newValues.get("mapPoints");
            double lat0 = Double.valueOf(mapPoints.get(0).get("y").toString()).doubleValue();
            double lon0 = Double.valueOf(mapPoints.get(0).get("x").toString()).doubleValue();
            double lat1 = Double.valueOf(mapPoints.get(1).get("y").toString()).doubleValue();
            double lon1 = Double.valueOf(mapPoints.get(1).get("x").toString()).doubleValue();
            double lat3 = Double.valueOf(mapPoints.get(3).get("y").toString()).doubleValue();
            double lon3 = Double.valueOf(mapPoints.get(3).get("x").toString()).doubleValue();
            CoordinateUtils cu = CoordinateUtils.getInstance();
            cu.setUnits(newValues.get("imageUnit").toString());
            imageWidth = cu.calculateDistance(lat0, lon0, lat1, lon1);
            imageHeight = cu.calculateDistance(lat0, lon0, lat3, lon3);
        } else {
            imageWidth = Double.valueOf(newValues.get("imageWidth").toString()).doubleValue();
            imageHeight = Double.valueOf(newValues.get("imageHeight").toString()).doubleValue();
        }
        mapResponse.put("imageWidth", imageWidth);
        mapResponse.put("imageHeight", imageHeight);
        return mapResponse;
    }

    private static LocalMap getCopy(LocalMap localMap) {
        LocalMap response = new LocalMap();
        response.setId(localMap.getId());
        response.setAltOrigin(localMap.getAltOrigin());
        response.setDeclination(localMap.getDeclination());
        response.setDescription(localMap.getDescription());
        response.setImage(localMap.getImage());
        response.setImageHeight(localMap.getImageHeight());
        response.setImageUnit(localMap.getImageUnit());
        response.setImageWidth(localMap.getImageWidth());
        response.setLatOrigin(localMap.getLatOrigin());
        response.setLatOriginNominal(localMap.getLatOriginNominal());
        response.setLatmax(localMap.getLatmax());
        response.setLatmin(localMap.getLatmin());
        response.setLonOrigin(localMap.getLonOrigin());
        response.setLonOriginNominal(localMap.getLonOriginNominal());
        response.setLonmax(localMap.getLonmax());
        response.setLonmin(localMap.getLonmin());
        response.setModifiedTime(localMap.getModifiedTime());
        response.setName(localMap.getName());
        response.setGroup(localMap.getGroup());
        response.setYNominal(localMap.getYNominal());
        response.setXNominal(localMap.getXNominal());
        response.setRotationDegree(localMap.getRotationDegree());
        response.setLocalMapPoints(localMap.getLocalMapPoints());
        return response;
    }

    private static class LocalMapTmp {
        private List<List<Double>> mapPoints;
        private Double imageWidth;
        private Double imageHeight;
        private Double latMin;
        private Double lonMin;
        private Double latMax;
        private Double lonMax;
        private Double rotationDegree;
        private Double latOrigin;
        private Double lonOrigin;
        private Double latOriginNominal;
        private Double lonOriginNominal;
        String imageUnit;

        public List<List<Double>> getNewZonePoints(LocalMapTmp localMapTmp) {
            setAction(localMapTmp);

            Zone zoneAux = ZoneService.getInstance().get(4L);
            List<double[]> zonePoints = zoneAux.getZonePointsAsList();
            Double[][] points = new Double[zonePoints.size()][2];
            int i = 0;
            for (double[] point:zonePoints) {
                points[i][0] = point[0];
                points[i++][1] = point[1];
            }
            List<List<Double>> listResponse = getNewPoints(points, localMapTmp);
            ZoneService.getInstance().updateZonePoints(zoneAux, listResponse);
            return listResponse;
        }

        public void calculateBounds(LocalMapTmp localMap, List<List<Double>> oldLocalMapPoints) {
            double angle = Math.toRadians(rotationDegree - localMap.getRotationDegree());
            logger.info("old min values: " + this.latMin + " .. " + this.lonMin);
            logger.info("old max values: " + this.latMax + " .. " + this.lonMax);

            Double[][] points = new Double[oldLocalMapPoints.size()][oldLocalMapPoints.get(0).size()];
            int i = 0;
            for (List<Double> point : oldLocalMapPoints) {
                points[i][0] = point.get(0);
                points[i][1] = point.get(1);
                i++;
            }
            List<List<Double>> newMapPoints = rotatePoints(points, localMap);
            double lonMax = newMapPoints.get(0).get(0);
            double latMax = newMapPoints.get(0).get(1);
            double lonMin = newMapPoints.get(0).get(0);
            double latMin = newMapPoints.get(0).get(1);
            for (List<Double> point:newMapPoints) {
                if (point.get(0) > lonMax) {
                    lonMax = point.get(0);
                }
                if (point.get(0) < lonMin) {
                    lonMin = point.get(0);
                }
                if (point.get(1) > latMax) {
                    latMax = point.get(1);
                }
                if (point.get(1) < latMin) {
                    latMin = point.get(1);
                }
            }
            this.lonMin = lonMin;
            this.latMin = latMin;
            this.lonMax = lonMax;
            this.latMax = latMax;

            logger.info("new min values: " + localMap.getLatMin() + " .. " + localMap.getLonMin());
            logger.info("new max values: " + localMap.getLatMax() + " .. " + localMap.getLonMax());
        }

        public void generateMapPoints(Map<String, Object> map) {
            LocalMapPoint[] localMapPoints = (LocalMapPoint[]) map.get("mapPoints");
            mapPoints = new ArrayList<>();
            for (LocalMapPoint localMapPoint:localMapPoints) {
                List<Double> point = new ArrayList<>();
                point.add(localMapPoint.getX());
                point.add(localMapPoint.getY());
                mapPoints.add(point);
            }
        }

        public enum ACTION {NONE, ROTATE, TRANSLATE, SCALE};
        public ACTION action;
        public LocalMapTmp (Map<String, Object> map) {
            latOriginNominal = 0.0;
            lonOriginNominal = 0.0;
            imageWidth = Double.valueOf(map.get("imageWidth").toString());
            imageHeight = Double.valueOf(map.get("imageHeight").toString());
            latMin = Double.valueOf(map.get("latmin").toString());
            lonMin = Double.valueOf(map.get("lonmin").toString());
            latMax = Double.valueOf(map.get("latmax").toString());
            lonMax = Double.valueOf(map.get("lonmax").toString());
            imageUnit = map.get("imageUnit").toString();
            rotationDegree = Double.valueOf(map.get("rotationDegree").toString());
            if (map.containsKey("lonOrigin")) {
                lonOrigin = Double.valueOf(map.get("lonOrigin").toString());
                latOrigin = Double.valueOf(map.get("latOrigin").toString());
            }
            if (map.containsKey("lonOriginNominal")) {
                lonOriginNominal = Double.valueOf(map.get("lonOriginNominal").toString());
                latOriginNominal = Double.valueOf(map.get("latOriginNominal").toString());
            }
        }

        public void setAction(LocalMapTmp localMapTmp) {
            if (this.rotationDegree.doubleValue() != localMapTmp.getRotationDegree().doubleValue()) {
                action = ACTION.ROTATE;
            } else {
                if ((this.imageWidth.doubleValue() != localMapTmp.getImageWidth().doubleValue() ) || (imageHeight.doubleValue() != localMapTmp.getImageHeight().doubleValue() )) {
                    action = ACTION.SCALE;
                } else {
                    action = ACTION.TRANSLATE;
                }
            }
        }

        public List<Double> getNewOrigin(LocalMapTmp localMapTmp) {
            setAction(localMapTmp);
            Double[][] points = new Double[1][3];
            points[0][0] = lonOrigin; points[0][1] = latOrigin; points[0][2] = 1.0;
            List<List<Double>> listResponse = getNewPoints(points, localMapTmp);
            return listResponse.get(0);
        }

        public List<Double> getNewOriginNominal(LocalMapTmp localMapTmp) {
            if ((lonOriginNominal.doubleValue() == 0) && (latOriginNominal.doubleValue() == 0)) {
                List<Double> listResponse = new ArrayList<>();
                return listResponse;
            }
            setAction(localMapTmp);
            Double[][] points = new Double[1][3];
            points[0][0] = lonOriginNominal; points[0][1] = latOriginNominal; points[0][2] = 1.0;
            List<List<Double>> listResponse = getNewPoints(points, localMapTmp);
            return listResponse.get(0);
        }

        private List<List<Double>> getNewPoints(Double[][] points, LocalMapTmp localMapTmp) {
            List<List<Double>> pointsResponse;
            logger.info("Action ===> " + action.toString());
            switch (action) {
                case ROTATE: pointsResponse = rotatePoints(points, localMapTmp);
                    break;
                case SCALE: pointsResponse = scalePoints(points, localMapTmp);
                    break;
                case TRANSLATE: pointsResponse = scalePoints(points, localMapTmp);
                    break;
                case NONE: default: pointsResponse = pointsToArray(points);
                    break;
            }
            return pointsResponse;
        }

        private List<List<Double>> rotatePoints(Double[][] points, LocalMapTmp localMapTmp) {
            Double newPoints[][] = new Double[points.length][points[0].length];
            Double centerX = (lonMax - lonMin) / 2 + lonMin;
            Double centerY = (latMax - latMin) / 2 + latMin;
            CoordinateUtils cu = CoordinateUtils.getInstance();
            cu.setProperties(localMapTmp.getLonMin(), localMapTmp.getLatMin(), 0, 0, "ft");
            double xyAux[] = cu.lonlat2xy(centerX, centerY, 0);
            centerX = xyAux[0];
            centerY = xyAux[1];
            Double angle = rotationDegree - localMapTmp.getRotationDegree();
            angle = Math.toRadians(angle);
            for (int i = 0; i < points.length; i++) {
                xyAux = cu.lonlat2xy(points[i][0], points[i][1], 0);
                AffineTransform.getRotateInstance(angle, centerX, centerY)
                        .transform(xyAux, 0, xyAux, 0, 1);
                double auxLonLat[] = cu.xy2lonlat(xyAux[0], xyAux[1], 0);
                newPoints[i][0] = auxLonLat[0];
                newPoints[i][1] = auxLonLat[1];
            }
            return pointsToArray(newPoints);
        }

        private List<List<Double>> transformPoints(Double[][] points, LocalMapTmp localMapTmp) {
            CoordinateUtils cu = CoordinateUtils.getInstance();
            cu.setProperties(localMapTmp.getLonMin(), localMapTmp.getLatMin(), 0, 0, localMapTmp.getImageUnit());
            Double newPoints[][] = new Double[points.length][points[0].length];
            double[] xyOldMin = cu.lonlat2xy(lonMin.doubleValue(), latMin.doubleValue(), 0.0);
            double[] xyOldMax = cu.lonlat2xy(lonMax.doubleValue(), latMax.doubleValue(), 0.0);
            double[] xyNewMin = cu.lonlat2xy(localMapTmp.getLonMin().doubleValue(), localMapTmp.getLatMin().doubleValue(), 0.0);
            double[] xyNewMax = cu.lonlat2xy(localMapTmp.getLonMax().doubleValue(), localMapTmp.getLatMax().doubleValue(), 0.0);
            double centerX = (xyOldMax[0] - xyOldMin[0]) / 2 + xyOldMin[0];
            double centerY = (xyOldMax[1] - xyOldMin[1]) / 2 + xyOldMin[1];
            double centerX1 = (xyNewMax[0] - xyNewMin[0]) / 2 + xyNewMin[0];
            double centerY1 = (xyNewMax[1] - xyNewMin[1]) / 2 + xyNewMin[1];


            double xyAux[] = new double[3];
            double tx = centerX1 - centerX;
            double ty = centerY1 - centerY;
            double sx = 1;
            double sy = 1;
            Double angle = rotationDegree - localMapTmp.getRotationDegree();
            angle = Math.toRadians(angle);
            if ((imageHeight.doubleValue() != localMapTmp.getImageHeight().doubleValue()) ||
                    (imageWidth.doubleValue() != localMapTmp.getImageWidth().doubleValue())) {
                double distance0 = xyOldMax[0] - xyOldMin[0];
                double distance1 = xyNewMax[0] - xyNewMin[0];
                sx = distance1 / distance0;
                distance0 = xyOldMax[1] - xyOldMin[1];
                distance1 = xyNewMax[1] - xyNewMin[1];
                sy = distance1 / distance0;
//                sx = localMapTmp.getImageWidth() / imageWidth;
//                sy = localMapTmp.getImageHeight() / imageHeight;
            }
            for (int i = 0; i < points.length; i++) {
                xyAux = cu.lonlat2xy(points[i][0], points[i][1], 0);
//                AffineTransform at = new AffineTransform();
//                at.setToIdentity();
//                at.scale(sx, sy);
//                at.rotate(angle, centerX, centerY);
//                at.translate(tx, ty);
//                at.transform(xyAux, 0, xyAux, 0, 1);
//                AffineTransform.getScaleInstance(sx, sy)..getRotateInstance(angle, centerX1, centerY1).getTranslateInstance(tx, ty).transform(xyAux, 0, xyAux, 0, 1);
//                AffineTransform.getTranslateInstance(tx, ty).transform(xyAux, 0, xyAux, 0, 1);
                xyAux[0] = (xyAux[0] - xyOldMin[0]) * sx + xyNewMin[0];
                xyAux[1] = (xyAux[1] - xyOldMin[1]) * sy + xyNewMin[1];
                AffineTransform.getRotateInstance(angle, centerX1, centerY1)
                        .transform(xyAux, 0, xyAux, 0, 1);
                double auxLonLat[] = cu.xy2lonlat(xyAux[0], xyAux[1], 0);

                newPoints[i][0] = auxLonLat[0];
                newPoints[i][1] = auxLonLat[1];
            }
            return pointsToArray(newPoints);
        }

        private List<List<Double>> scalePoints(Double[][] points, LocalMapTmp localMapTmp) {
            Double newPoints[][] = new Double[points.length][points[0].length];
            CoordinateUtils cu = CoordinateUtils.getInstance();
            cu.setUnits(localMapTmp.getImageUnit());
            double distance0 = lonMax - lonMin;
            double distance1 = localMapTmp.getLonMax() - localMapTmp.getLonMin();
            double sx = distance1 / distance0;
            distance0 = latMax - latMin;
            distance1 = localMapTmp.getLatMax() - localMapTmp.getLatMin();
            double sy = distance1 / distance0;

            for (int i = 0; i < points.length; i++) {
                double xyAux[] = cu.lonlat2xy(points[i][0], points[i][1], 0);
                xyAux[0] = (points[i][0] - lonMin) * sx + localMapTmp.getLonMin();
                xyAux[1] = (points[i][1] - latMin) * sy + localMapTmp.getLatMin();
                newPoints[i][0] = xyAux[0];
                newPoints[i][1] = xyAux[1];
            }
            return pointsToArray(newPoints);
        }

        private List<List<Double>> translatePoints(Double[][] points, LocalMapTmp localMapTmp) {
            return pointsToArray(points);
        }

        private List<List<Double>> pointsToArray(Double[][] points) {
            List<List<Double>> response = new ArrayList<>();
            for (int i = 0; i < points.length; i++) {
                List<Double> row = Arrays.asList(points[i]);
                response.add(row);
            }
            return response;
        }

        public List<List<Double>> getMapPoints() {
            return mapPoints;
        }

        public void setMapPoints(List<List<Double>> mapPoints) {
            this.mapPoints = mapPoints;
        }

        public Double getImageWidth() {
            return imageWidth;
        }

        public void setImageWidth(Double imageWidth) {
            this.imageWidth = imageWidth;
        }

        public Double getImageHeight() {
            return imageHeight;
        }

        public void setImageHeight(Double imageHeight) {
            this.imageHeight = imageHeight;
        }

        public Double getLatMin() {
            return latMin;
        }

        public void setLatMin(Double latMin) {
            this.latMin = latMin;
        }

        public Double getLonMin() {
            return lonMin;
        }

        public void setLonMin(Double lonMin) {
            this.lonMin = lonMin;
        }

        public Double getLatMax() {
            return latMax;
        }

        public void setLatMax(Double latMax) {
            this.latMax = latMax;
        }

        public Double getLonMax() {
            return lonMax;
        }

        public void setLonMax(Double lonMax) {
            this.lonMax = lonMax;
        }

        public Double getRotationDegree() {
            return rotationDegree;
        }

        public void setRotationDegree(Double rotationDegree) {
            this.rotationDegree = rotationDegree;
        }

        public Double getLatOrigin() {
            return latOrigin;
        }

        public void setLatOrigin(Double latOrigin) {
            this.latOrigin = latOrigin;
        }

        public Double getLonOrigin() {
            return lonOrigin;
        }

        public void setLonOrigin(Double lonOrigin) {
            this.lonOrigin = lonOrigin;
        }

        public String getImageUnit() {
            return imageUnit;
        }

        public void setImageUnit(String imageUnit) {
            this.imageUnit = imageUnit;
        }

        public ACTION getAction() {
            return action;
        }

        public void setAction(ACTION action) {
            this.action = action;
        }
    }

	public boolean isValidNewLocalMapName(Map<String, Object> body) {
		String[] requiredFields = {"newLocalMap", "localMaps"};
		for (String field:requiredFields) {
			if (body.get(field) == null) {
				throw new UserException("[" + field + "] is required.");
			}
		}

		if (!(body.get("newLocalMap") instanceof Map)) {
			throw new UserException("[newLocalMap] should be an object.");
		}
		Map<String, Object> localMap = (Map) body.get("newLocalMap");
		localMap.put("operation", "add");
		localMap.put("zoneProperties", new ArrayList<>());
		localMap.put("zonePoints", new ArrayList<>());
		ValidationBean validationBean = validateLocalMap(localMap);
		if (validationBean.isError()) {
			throw new UserException("LocalMap validation: " + validationBean.getErrorDescription());
		}
		if (!(body.get("localMaps") instanceof List)) {
			throw new UserException("[localMaps] should be a localMap list.");
		}
		List<Map<String, Object>> localMaps = (List) body.get("localMaps");
		for (Map<String, Object> tmpLocalMap :localMaps) {
			tmpLocalMap.put("operation", "add");
			tmpLocalMap.put("zoneProperties", new ArrayList<>());
			tmpLocalMap.put("zonePoints", new ArrayList<>());
			if (!tmpLocalMap.containsKey("name")
					|| !tmpLocalMap.containsKey("group")
					|| !((Map<String, Object>) tmpLocalMap.get("group")).containsKey("id")) {
				throw new UserException("LocalMap validation: [name] or [group] are required for validation");
			}
		}

        Integer count = 0;
        String localMapName = localMap.get("name").toString();
        Integer groupId = (Integer) ((Map<String, Object>) localMap.get("group")).get("id");
        for (Map<String, Object> localMapAux:localMaps) {
            String localMapNameAux = localMapAux.get("name").toString();
            Integer groupIdAux = (Integer) ((Map<String, Object>) localMapAux.get("group")).get("id");
            if (StringUtils.equalsIgnoreCase(localMapName, localMapNameAux) && groupId.intValue() == groupIdAux.intValue()) {
                count++;
            }
        }
        if (count > 1) {
            throw new UserException("LocalMap name [" + localMap.get("name").toString() + "] already exists.");
        }
		return true;
	}

    public boolean isValidNewLocalMapName(String localMapName, Long groupId, Long excludeId) {
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QLocalMap.localMap.name.eq(localMapName));
        be = be.and(QLocalMap.localMap.group.id.eq(groupId));
        if (excludeId != null){
            be = be.and(QLocalMap.localMap.id.ne(excludeId));
        }
        return getLocalMapDAO().countAll(be) == 0;
    }

    public void udpateOpacity(Long facilityId, Integer opacity){
        BooleanBuilder be = new BooleanBuilder();
        be = be.and(QReportDefinition.reportDefinition.localMapId.eq(facilityId));
        List<ReportDefinition> listReportDefinitions = ReportDefinitionService.getInstance().listPaginated(be,null, null);
        for (ReportDefinition reportDefinition: listReportDefinitions){
            reportDefinition.setMapOpacity(opacity);
        }
    }

    public boolean validateDeleteLocalMap (Long localMapId){
        List<Zone> facilityZones = ZoneService.getZonesByLocalMap(localMapId);
        if (facilityZones.size() > 0) {
            return true;
        }
        return false;
    }

    @Override
    public LocalMap insert( LocalMap localMap )
    {
        validateInsert( localMap );
        return super.insert(localMap);
    }

    public void validateInsert( LocalMap localMap )
    {
        if (!getInstance().isValidNewLocalMapName(localMap.getName(), localMap.getGroup().getId(), null)) {
            throw new UserException("Local Map already exists");
        }
    }

}
