package com.tierconnect.riot.iot.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.tierconnect.riot.appcore.entities.Connection;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.commons.dtos.*;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.EdgeboxService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;


/**
 * Translator class.
 *
 * @author jantezana
 * @version 2017/01/13
 */
public final class Translator {
	
	private static final Logger logger = Logger.getLogger(Translator.class);
	
    /**
     * Convert a list of thing types to DTOs.
     *
     * @param thingTypes the list of thing types
     * @return a list of thing types DTOs
     */
    public static List<ThingTypeDto> convertToThingTypeDTOs(final List<ThingType> thingTypes) {
        Preconditions.checkNotNull(thingTypes, "The list of thing types is null");
        List<ThingTypeDto> thingTypeDtos = new LinkedList<>();
        for (ThingType thingType : thingTypes) {
            thingTypeDtos.add(convertToThingTypeDTO(thingType));
        }
        return Collections.unmodifiableList(thingTypeDtos);
    }

    /**
     * Convert a thing type to DTO.
     *
     * @param thingType the thing type
     * @return the thing type DTO
     */
    public static ThingTypeDto  convertToThingTypeDTO(final ThingType thingType) {
        Preconditions.checkNotNull(thingType, "The thing type is null");
        Preconditions.checkNotNull(thingType.getGroup(), "The thing type is null");
        
        ThingTypeDto thingTypeDto = new ThingTypeDto();
        thingTypeDto.id = thingType.getId();
        thingTypeDto.name = thingType.getName();
        thingTypeDto.code = thingType.getCode();
        if (thingType.getThingTypeFields() != null){
            thingTypeDto.fields = convertToThingTypeFieldDTOMap(thingType.getThingTypeFields());
        }

        thingTypeDto.archived = thingType.isArchived();
        thingTypeDto.autoCreate = thingType.isAutoCreate();
        thingTypeDto.isParent = thingType.isIsParent();
        Group group = thingType.getGroup();
        
        thingTypeDto.groupId = group.getId();

        thingTypeDto.modifiedTime = thingType.getModifiedTime();
        thingTypeDto.serialFormula = thingType.getSerialFormula();
        GroupType defaultOwnerGroupType = thingType.getDefaultOwnerGroupType();
        thingTypeDto.defaultOwnerGroupTypeId = (defaultOwnerGroupType != null) ? defaultOwnerGroupType.getId() : null;
        ThingTypeTemplate thingTypeTemplate = thingType.getThingTypeTemplate();
        thingTypeDto.thingTypeTemplateId = (thingTypeTemplate != null) ? thingTypeTemplate.getId() : null;
        return thingTypeDto;
    }

    /**
     * Convert the list of thing type fields to map of thing type field DTO
     *
     * @param thingTypeFields the list of thing type fields
     * @return the map of thing type field DTO
     */
    public static Map<String, ThingTypeFieldDto> convertToThingTypeFieldDTOMap(final Set<ThingTypeField> thingTypeFields) {
        Preconditions.checkNotNull(thingTypeFields, "The list of thing type fields is null");
        Map<String, ThingTypeFieldDto> thingTypeFieldDtoMap = new HashMap<>();
        ThingTypeFieldDto thingTypeFieldDto;
        for (ThingTypeField thingTypeField : thingTypeFields) {
            thingTypeFieldDto = convertToThingTypeFieldDTO(thingTypeField);
            thingTypeFieldDtoMap.put(thingTypeFieldDto.name, thingTypeFieldDto);
        }
        return Collections.unmodifiableMap(thingTypeFieldDtoMap);
    }

    /**
     * Convert a list of thing type fields to DTOs.
     *
     * @param thingTypeFields the list of thing type fields
     * @return the list of thing type fields DTOs
     */
    public static List<ThingTypeFieldDto> convertToThingTypeFieldDTOs(final Set<ThingTypeField> thingTypeFields) {
        Preconditions.checkNotNull(thingTypeFields, "The list of thing type fields is null");
        List<ThingTypeFieldDto> thingTypeFieldDtos = new LinkedList<>();
        for (ThingTypeField thingTypeField : thingTypeFields) {
            thingTypeFieldDtos.add(convertToThingTypeFieldDTO(thingTypeField));
        }
        return Collections.unmodifiableList(thingTypeFieldDtos);
    }

    /**
     * Convert a thing type field to DTO.
     *
     * @param thingTypeField the thing type field
     * @return the thing type field DTO
     */
    public static ThingTypeFieldDto convertToThingTypeFieldDTO(final ThingTypeField thingTypeField) {
        Preconditions.checkNotNull(thingTypeField, "The thing type field is null");
        ThingTypeFieldDto thingTypeFieldDto = new ThingTypeFieldDto();
        thingTypeFieldDto.id = thingTypeField.getId();
        thingTypeFieldDto.name = thingTypeField.getName();
        thingTypeFieldDto.defaultValue = thingTypeField.getDefaultValue();
        thingTypeFieldDto.multiple = thingTypeField.getMultiple();
        thingTypeFieldDto.symbol = thingTypeField.getSymbol();
        thingTypeFieldDto.thingTypeFieldTemplateId = thingTypeField.getThingTypeFieldTemplateId();
        thingTypeFieldDto.timeSeries = thingTypeField.getTimeSeries();
        thingTypeFieldDto.timeToLive = thingTypeField.getTimeToLive();
        thingTypeFieldDto.typeParent = thingTypeField.getTypeParent();
        thingTypeFieldDto.unit = thingTypeField.getUnit();
        DataType dataType = thingTypeField.getDataType();
        thingTypeFieldDto.dataTypeId = (dataType != null) ? dataType.getId() : null;
        thingTypeFieldDto.dataTypeThingTypeId = thingTypeField.getDataTypeThingTypeId();
        return thingTypeFieldDto;
    }

    /**
     * Convert data type to DTO
     *
     * @param dataType the data type
     * @return the data type DTO
     */
    public static DataTypeDto convertToDataTypeDTO(final DataType dataType) {
        Preconditions.checkNotNull(dataType, "The data type is null");
        DataTypeDto dataTypeDto = DataTypeDto.fromCode(dataType.getCode());
        return dataTypeDto;
    }

    /**
     * Convert the list of groups to DTOs.
     *
     * @param groups the list of groups
     * @return the list of group DTOs
     */
    public static List<GroupDto> convertToGroupDTOs(List<Group> groups) {
        Preconditions.checkNotNull(groups, "The list the groups is null");
        List<GroupDto> groupDtos = new LinkedList<>();
        for (Group group : groups) {
            groupDtos.add(convertToGroupDTO(group));
        }
        return Collections.unmodifiableList(groupDtos);
    }

    /**
     * Convert group to DTO
     *
     * @param group the group
     * @return the group DTO
     */
    public static GroupDto convertToGroupDTO(Group group) {
        Preconditions.checkNotNull(group, "the group is null");
        GroupDto groupDto = new GroupDto();
        groupDto.id = group.getId();
        groupDto.code = group.getCode();
        groupDto.name = group.getName();

        GroupType groupType = group.getGroupType();
        if (groupType != null) {
            groupDto.groupType = new GroupTypeDto();
            groupDto.groupType.id = groupType.getId();
        }

        groupDto.description = group.getDescription();
        groupDto.hierarchyName = group.getHierarchyName();
        groupDto.archived = group.isArchived();
        groupDto.treeLevel = group.getTreeLevel();
        return groupDto;
    }

    /**
     * Convert list of group types to DTOs.
     *
     * @param groupTypes the list of group types
     * @return the list of group types DTOs
     */
    public static List<GroupTypeDto> convertToGroupTypeDTOs(List<GroupType> groupTypes) {
        Preconditions.checkNotNull(groupTypes, "The list the group types is null");
        List<GroupTypeDto> groupTypeDtos = new LinkedList<>();
        for (GroupType groupType : groupTypes) {
            groupTypeDtos.add(convertToGroupTypeDTO(groupType));
        }
        return Collections.unmodifiableList(groupTypeDtos);
    }

    /**
     * Convert group to DTO.
     *
     * @param groupType the group type
     * @return the group type DTO
     */
    public static GroupTypeDto convertToGroupTypeDTO(GroupType groupType) {
        Preconditions.checkNotNull(groupType, "The group type is null");
        GroupTypeDto groupTypeDto = new GroupTypeDto();
        groupTypeDto.id = groupType.getId();
        groupTypeDto.code = groupType.getCode();
        groupTypeDto.name = groupType.getName();
        return groupTypeDto;
    }

    /**
     * Convert list of zones to DTOs.
     *
     * @param zones the list of zones
     * @return the list of zone DTOs
     */
    public static List<ZoneDto> convertToZoneDTOs(List<Zone> zones,
                                                  Map<Long, Map<String, Object>> zoneProperties) {
        Preconditions.checkNotNull(zones, "The list the zones is null");
        Preconditions.checkNotNull(zoneProperties, "The zone properties is null");
        
        List<ZoneDto> zonesDtos = new LinkedList<>();
        for (Zone zone : zones) {
            zonesDtos.add(convertZoneDTO(zone, zoneProperties));
        }
        return Collections.unmodifiableList(zonesDtos);
    }

    /**
     * Convert a zone to DTO.
     *
     * @param zone the zone
     * @return the zone DTO
     */
    public static ZoneDto convertZoneDTO(Zone zone,
                                         Map<Long, Map<String, Object>> zoneProperties) {
        Preconditions.checkNotNull(zone, "The zone is null");
        Preconditions.checkNotNull(zoneProperties, "The zone properties is null");
        ZoneDto zoneDto = new ZoneDto();
        zoneDto.id = zone.getId();
        zoneDto.code = zone.getCode();
        zoneDto.name = zone.getName();
        zoneDto.groupId = zone.getGroup().getId();
        FacilityMapDto facilityMap = null;
        LocalMap localMap = zone.getLocalMap();
        if (localMap != null) {
            facilityMap = new FacilityMapDto();
            facilityMap.id = localMap.getId();
            facilityMap.name = localMap.getName();
            facilityMap.description = localMap.getDescription();
            facilityMap.lonOrigin = localMap.getLatOrigin();
            facilityMap.latOrigin = localMap.getLatOrigin();
            facilityMap.altOrigin = localMap.getAltOrigin();
            facilityMap.declination = localMap.getDeclination();
            facilityMap.imageWidth = localMap.getImageWidth();
            facilityMap.imageHeight = localMap.getImageHeight();
            facilityMap.xNominal = localMap.getXNominal();
            facilityMap.yNominal = localMap.getYNominal();
            facilityMap.latOriginNominal = localMap.getLatOriginNominal();
            facilityMap.lonOriginNominal = localMap.getLonOriginNominal();
            facilityMap.imageUnit = localMap.getImageUnit();
            facilityMap.lonmin = localMap.getLonmin();
            facilityMap.lonmax = localMap.getLonmax();
            facilityMap.latmin = localMap.getLatmin();
            facilityMap.latmax = localMap.getLatmax();
            facilityMap.modifiedTime = localMap.getModifiedTime();
        }
        zoneDto.facilityMap = facilityMap;
        ZoneGroup zoneGroup = zone.getZoneGroup();
        ZonePropertyDto zoneGroupDto = null;
        if (zoneGroup != null) {
            zoneGroupDto = new ZonePropertyDto();
            zoneGroupDto.id = zoneGroup.getId();
            zoneGroupDto.name = zoneGroup.getName();
        }
        zoneDto.zoneGroup = zoneGroupDto;
        ZoneType zoneType = zone.getZoneType();
        ZonePropertyDto zoneTypeDto = null;
        if (zoneType != null) {
            zoneTypeDto = new ZonePropertyDto();
            zoneTypeDto.id = zoneType.getId();
            zoneTypeDto.name = zoneType.getName();
            zoneTypeDto.code = zoneType.getZoneTypeCode();

            // Fill Zone Properties.
            Map<String, Object> map = zoneProperties.get(zone.getId());
            if(map!=null) {
            	zoneDto.zoneProperties = map;
            }
        }
        zoneDto.zoneType = zoneTypeDto;
        Set<ZonePoint> zonePoints = zone.getZonePoints();
        List<double[]> points = new LinkedList<>();
        for (ZonePoint zonePoint : zonePoints) {
            double[] point = new double[2];
            point[0] = zonePoint.getX();
            point[1] = zonePoint.getY();
            points.add(point);
        }
        zoneDto.zonePoints = new LinkedList<>(points);
        return zoneDto;
    }

    /**
     * Convert list of shifts to DTOs.
     *
     * @param shifts the list of shifts
     * @return the list of shift DTOs
     */
    public static List<ShiftDto> convertToShiftDTOs(List<Shift> shifts) {
        Preconditions.checkNotNull(shifts, "The list the shifts is null");
        List<ShiftDto> shiftDtos = new LinkedList<>();
        for (Shift shift : shifts) {
            shiftDtos.add(convertToShiftDTO(shift));
        }
        return Collections.unmodifiableList(shiftDtos);
    }

    /**
     * Convert a shift to DTO.
     *
     * @param shift the shift
     * @return the shift DTO
     */
    public static ShiftDto convertToShiftDTO(Shift shift) {
        Preconditions.checkNotNull(shift, "The shift is null");
        Preconditions.checkNotNull(shift.getGroup(), "The shift's group is null");
        
        ShiftDto shiftDto = new ShiftDto();
        shiftDto.id = shift.getId();
        shiftDto.code = shift.getCode();
        shiftDto.name = shift.getName();
        shiftDto.active = shift.getActive();
        shiftDto.startTimeOfDay = shift.getStartTimeOfDay();
        shiftDto.endTimeOfDay = shift.getEndTimeOfDay();
        shiftDto.daysOfWeek = shift.getDaysOfWeek();
        Group group = shift.getGroup();
        shiftDto.groupId = group.getId();
        return shiftDto;
    }

    /**
     * Convert list of logical readers to DTOs.
     *
     * @param logicalReaders the list of logical readers
     * @return the list of logical reader DTOs
     */
    public static List<LogicalReaderDto> convertToLogicalReaderDTOs(List<LogicalReader> logicalReaders) {
        Preconditions.checkNotNull(logicalReaders, "The list the logical readers is null");
        List<LogicalReaderDto> logicalReaderDtos = new LinkedList<>();
        for (LogicalReader logicalReader : logicalReaders) {
            logicalReaderDtos.add(convertLogicalReaderDTO(logicalReader));
        }
        return Collections.unmodifiableList(logicalReaderDtos);
    }

    /**
     * Convert a logical reader to DTO.
     *
     * @param logicalReader the logical reader
     * @return the logical reader DTO
     */
    public static LogicalReaderDto convertLogicalReaderDTO(LogicalReader logicalReader) {
        Preconditions.checkNotNull(logicalReader, "The logical reader is null");
        Preconditions.checkNotNull(logicalReader.getGroup(), "The logical reader's group is null");
        LogicalReaderDto logicalReaderDto = new LogicalReaderDto();
        logicalReaderDto.id = logicalReader.getId();
        logicalReaderDto.code = logicalReader.getCode();
        logicalReaderDto.name = logicalReader.getName();
        logicalReaderDto.zoneInId = logicalReader.getZoneIn().getId();
        logicalReaderDto.zoneOutId = logicalReader.getZoneOut().getId();
        logicalReaderDto.x = (logicalReader.getX() != null) ? String.valueOf(logicalReader.getX()) : null;
        logicalReaderDto.y = (logicalReader.getY() != null) ? String.valueOf(logicalReader.getY()) : null;
        logicalReaderDto.z = (logicalReader.getZ() != null) ? String.valueOf(logicalReader.getZ()) : null;
        Group group = logicalReader.getGroup();
        logicalReaderDto.groupId = group.getId();
        return logicalReaderDto;
    }

    /**
     * Convert list of edge boxes to DTOs.
     *
     * @param edgeboxes the list of edge boxes
     * @return the list of edge boxes DTOs
     */
    public static List<EdgeboxDto> convertToEdgeboxDTOs(List<Edgebox> edgeboxes) {
        Preconditions.checkNotNull(edgeboxes, "The list the edge boxes is null");
        List<EdgeboxDto> edgeboxDtos = new LinkedList<>();
        for (Edgebox edgebox : edgeboxes) {
            edgeboxDtos.add(convertEdgeboxDTO(edgebox));
        }
        return Collections.unmodifiableList(edgeboxDtos);
    }
    
    public static List<EdgeboxConfigurationDto> convertToEdgeboxConfigurationDTOs(List<Edgebox> edgeboxes) {
        Preconditions.checkNotNull(edgeboxes, "The list the edge boxes is null");
        List<EdgeboxConfigurationDto> edgeboxConfigurationDtos = new LinkedList<>();
        ObjectMapper mapper = new ObjectMapper();
        
        for (Edgebox edgebox : edgeboxes) {
        	Map<String, Object> configuration = EdgeboxService.getInstance().getConfiguration(edgebox.getCode());
			try {
				String formattedConfiguration = mapper.writeValueAsString(configuration);
				edgeboxConfigurationDtos.add(convertEdgeboxConfgurationDTO(edgebox, formattedConfiguration));
			} catch (JsonProcessingException e) {
				logger.error(String.format(
						"Unable to send update cache configuration for bridge with code: %s. Due to the following error: %s",
						edgebox.getCode(),e.getMessage()));
			}
        }
        return Collections.unmodifiableList(edgeboxConfigurationDtos);
    }


    /**
     * Convert a edgebox to DTO.
     *
     * @param edgebox the edgebox
     * @return the edgebox DTO
     */
    public static EdgeboxDto convertEdgeboxDTO(Edgebox edgebox) {
        Preconditions.checkNotNull(edgebox, "The edgebox is null");
        EdgeboxDto edgeboxDto = new EdgeboxDto();
        edgeboxDto.id = edgebox.getId();
        edgeboxDto.name = edgebox.getName();
        edgeboxDto.code = edgebox.getCode();
        edgeboxDto.type = edgebox.getType();
        edgeboxDto.parameterType = edgebox.getParameterType();
        edgeboxDto.description = edgebox.getDescription();
        edgeboxDto.active = edgebox.getActive();
        Group group = edgebox.getGroup();
        if (group != null) {
            edgeboxDto.group = new GroupDto();
            edgeboxDto.group.id = group.getId();
        }
        return edgeboxDto;
    }
    
    /**
     * Convert a edgebox to DTO.
     *
     * @param edgebox the edgebox
     * @return the edgebox DTO
     */
    public static EdgeboxConfigurationDto convertEdgeboxConfgurationDTO(Edgebox edgebox, String formatedConfiguration) {
        Preconditions.checkNotNull(edgebox, "The edgebox is null");
        EdgeboxConfigurationDto edgeboxConfigurationDto = new EdgeboxConfigurationDto();
        edgeboxConfigurationDto.id = edgebox.getId();
        edgeboxConfigurationDto.code = edgebox.getCode();
        edgeboxConfigurationDto.type = edgebox.getType();
		edgeboxConfigurationDto.configuration = formatedConfiguration;
        Group group = edgebox.getGroup();
        if (group != null) {
            edgeboxConfigurationDto.group = new GroupDto();
            edgeboxConfigurationDto.group.id = group.getId();
        }
        return edgeboxConfigurationDto;
    }

    /**
     * Convert list of edgebox rule readers to DTOs.
     *
     * @param edgeboxRules the list of edgebox rules
     * @return the list of edgebox rules DTOs
     */
    public static List<EdgeboxRuleDto> convertToEdgeboxRuleDTOs(List<EdgeboxRule> edgeboxRules) {
        Preconditions.checkNotNull(edgeboxRules, "The list of edgebox rules is null");
        List<EdgeboxRuleDto> edgeboxRuleDtos = new LinkedList<>();
        for (EdgeboxRule edgeboxRule : edgeboxRules) {
            edgeboxRuleDtos.add(convertEdgeboxRuleDTO(edgeboxRule));
        }
        return Collections.unmodifiableList(edgeboxRuleDtos);
    }

    /**
     * Convert a edgeboxrule to DTO.
     *
     * @param edgeboxRule the edgebox rule
     * @return the edgebox DTO
     */
    public static EdgeboxRuleDto convertEdgeboxRuleDTO(EdgeboxRule edgeboxRule) {
        Preconditions.checkNotNull(edgeboxRule, "The edgeboxrule is null");
        EdgeboxRuleDto edgeboxRuleDto = new EdgeboxRuleDto();
        edgeboxRuleDto.id = edgeboxRule.getId();
        edgeboxRuleDto.active = edgeboxRule.getActive();
        edgeboxRuleDto.cronSchedule = edgeboxRule.getCronSchedule();
        edgeboxRuleDto.description = edgeboxRule.getDescription();
        edgeboxRuleDto.executeLoop = edgeboxRule.getExecuteLoop();
        edgeboxRuleDto.honorLastDetect = edgeboxRule.getHonorLastDetect();
        edgeboxRuleDto.input = edgeboxRule.getInput();
        edgeboxRuleDto.name = edgeboxRule.getName();
        edgeboxRuleDto.output = edgeboxRule.getOutput();
        edgeboxRuleDto.outputConfig = edgeboxRule.getOutputConfig();
        edgeboxRuleDto.rule = edgeboxRule.getRule();
        edgeboxRuleDto.sortOrder = edgeboxRule.getSortOrder();
        edgeboxRuleDto.edgeboxId = edgeboxRule.getEdgebox().getId();
        edgeboxRuleDto.groupId = edgeboxRule.getGroup().getId();
        edgeboxRuleDto.conditionType = edgeboxRule.getConditionType();
        edgeboxRuleDto.parameterConditionType = edgeboxRule.getParameterConditionType();
        edgeboxRuleDto.runOnReorder = edgeboxRule.getRunOnReorder();

        return edgeboxRuleDto;
    }

    /**
     * Convert list of connections to DTOs.
     *
     * @param connections the list of connections
     * @return the list of connections DTOs
     */
    public static List<ConnectionDto> convertToConnectionDTOs(List<Connection> connections) {
        Preconditions.checkNotNull(connections, "The list of edgebox rules is null");
        List<ConnectionDto> connectionDtos = new LinkedList<>();
        for (Connection connection : connections) {
            connectionDtos.add(convertConnectionDTO(connection));
        }
        return Collections.unmodifiableList(connectionDtos);
    }

    /**
     * Convert a connection to DTO.
     *
     * @param connection the connection
     * @return the edgebox DTO
     */
    public static ConnectionDto convertConnectionDTO(Connection connection) {
        Preconditions.checkNotNull(connection, "The connection is null");
        ConnectionDto connectionDto = new ConnectionDto();
        connectionDto.id = connection.getId();
        connectionDto.code = connection.getCode();
        connectionDto.name = connection.getName();
        connectionDto.connectionTypeId = connection.getConnectionType().getId();
        ObjectMapper mapper = new ObjectMapper();
        try {
            connectionDto.properties = mapper.readValue(connection.getProperties(), Map.class);
        } catch (IOException e) {
        	logger.error(String.format("error when building connection properties due to error: %s", e));
        }
        return connectionDto;
    }

    /**
     * Convert the list of shift zones to DTOs
     *
     * @param shiftZones the list of shift zones
     * @return the list of shift zone DTOs
     */
    public static List<ShiftZoneDto> convertToShiftZoneDTOs(List<ShiftZone> shiftZones,
                                                            Map<Long, Map<String, Object>> zoneProperties) {
        Preconditions.checkNotNull(shiftZones, "The list the shift zones is null");
        Preconditions.checkNotNull(zoneProperties, "The zone properties is null");
        List<ShiftZoneDto> shiftZoneDtos = new LinkedList<>();

        // Gets the list of shift ids.
        Set<Long> shiftIds = new HashSet<>();
        for (ShiftZone shiftZone : shiftZones) {
            shiftIds.add(shiftZone.getShift().getId());
        }

        for (Long shiftId : shiftIds) {
            Shift shift = null;

            // Gets the list of zones by shift ID.
            List<Zone> zones = new LinkedList<>();
            for (ShiftZone shiftZone : shiftZones) {
                if (shiftZone.getShift().getId().equals(shiftId)) {
                    shift = shiftZone.getShift();
                    zones.add(shiftZone.getZone());
                }
            }

            if (shift != null) {
                shiftZoneDtos.add(convertShiftZoneDTO(shift, zones, zoneProperties));
            }
        }

        return Collections.unmodifiableList(shiftZoneDtos);
    }
    
    public static List<ThingDto> convertToThingPairs(List<ThingDto> things, List<Group> groups) {
    	Map<Long, Group> cache = new HashMap<>();
    	ArrayList<ThingDto> list = new ArrayList<>();
    	for (ThingDto thingDto : things) {
    		Long groupId = thingDto.group.id;
    		Group group = cache.get(groupId);
    		if(group == null) {
    			// find in list and put it in cache.
    			for (Group groupItem : groups) {
					if(groupItem.getId().equals(groupId)) {
						group = groupItem;
						
						// put group in local cache for improvement in performance.
						cache.put(groupId, group); 
						break;
					}
				}
    		}
    		
    		if(group != null) {
    			list.add(thingDto);
    		}
		}
		return list;
    }

    /**
     * Convert a shift zone to DTO.
     *
     * @param shift the shift
     * @param zones the list of zone
     * @return the shift zone DTO
     */
    public static ShiftZoneDto convertShiftZoneDTO(Shift shift,
                                                   List<Zone> zones,
                                                   Map<Long, Map<String, Object>> zoneProperties) {
        Preconditions.checkNotNull(shift, "The shift is null");
        Preconditions.checkNotNull(zones, "The list of zones is null");
        Preconditions.checkNotNull(zoneProperties, "The zone properties is null");
        ShiftZoneDto shiftZoneDto = new ShiftZoneDto();
		shiftZoneDto.shift = convertToShiftDTO(shift);
        
        List<ZoneDto> convertToZoneDTOs = convertToZoneDTOs(zones, zoneProperties);
		shiftZoneDto.zones = new LinkedList<>();
		for (ZoneDto pairDto : convertToZoneDTOs) {
			shiftZoneDto.zones.add(pairDto);
		}
        return shiftZoneDto;
    }

    /**
     * Convert list of zone types to DTOs.
     *
     * @param zoneTypes the list of zone types
     * @return the list of zone types DTOs
     */
    public static List<ZoneTypeDto> convertToZoneTypeDTOs(List<ZoneType> zoneTypes) {
        Preconditions.checkNotNull(zoneTypes, "The list the zone types is null");
        List<ZoneTypeDto> zoneTypeDtos = new LinkedList<>();
        for (ZoneType zoneType : zoneTypes) {
            zoneTypeDtos.add(convertZoneTypeDTO(zoneType));
        }
        return Collections.unmodifiableList(zoneTypeDtos);
    }

    /**
     * Convert a zone type to DTO.
     *
     * @param zoneType the zone type
     * @return the zone type DTO
     */
    public static ZoneTypeDto convertZoneTypeDTO(ZoneType zoneType) {
        Preconditions.checkNotNull(zoneType, "The zone type is null");
        ZoneTypeDto zoneTypeDto = new ZoneTypeDto();
        zoneTypeDto.id = zoneType.getId();
        zoneTypeDto.code = zoneType.getZoneTypeCode();
        zoneTypeDto.name = zoneType.getName();
        zoneTypeDto.zoneProperties = new ArrayList<>();
        List<ZoneProperty> zoneProperties = zoneType.getZoneProperties();
        for (ZoneProperty zoneProperty : zoneProperties){
            ZoneTypePropertyDto newZoneTypeProperty = new ZoneTypePropertyDto();
            newZoneTypeProperty.id = zoneProperty.getId();
            newZoneTypeProperty.name = zoneProperty.getName();
            newZoneTypeProperty.type = zoneProperty.getType();
            zoneTypeDto.zoneProperties.add(newZoneTypeProperty);
        }
        return zoneTypeDto;
    }

}
