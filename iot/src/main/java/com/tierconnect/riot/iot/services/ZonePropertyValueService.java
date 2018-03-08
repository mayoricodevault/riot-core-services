package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QGroup;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.Constants;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.sdk.dao.UserException;
import org.apache.log4j.Logger;

import javax.annotation.Generated;
import java.util.*;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class ZonePropertyValueService extends ZonePropertyValueServiceBase 
{
    static Logger logger = Logger.getLogger(ZonePropertyValueService.class);
    public static void updateZonePropertyValue(Zone zone, List<Map<String, Object> > zonePropertiesMapList ) {

        Long zoneId = zone.getId();
        List<ZonePropertyValue> zonePropertyValuesList = getInstance().getZonePropertiesByZoneId(zoneId);
        Set<Long> zonePropertiesValids = new HashSet<>();

        for (Map<String, Object> zonePropertyItem : zonePropertiesMapList) {
            if(zonePropertyItem.containsKey("id") && zonePropertyItem.containsKey("value")) {
                Long zonePropertyItemId = Long.valueOf(zonePropertyItem.get("id").toString());
                String zonePropertyItemValue = zonePropertyItem.get("value").toString();
                if (zonePropertyItemValue.length() > 255) {
                    throw new UserException("Zone property value should be have max 255 characters");
                }

                boolean foundProperty = false;
                for (ZonePropertyValue zonePropertyValue : zonePropertyValuesList) {
                    if ( zonePropertyValue.getId().equals(zonePropertyItemId) && zonePropertyValue.getValue().equals(zonePropertyItemValue) ) {
                        foundProperty = true;
                        zonePropertyValue.setZoneId( zoneId );
                        zonePropertyValue.setZonePropertyId( zonePropertyItemId );
                        ZonePropertyValueService.getInstance().update(zonePropertyValue);
                        zonePropertiesValids.add(zonePropertyValue.getId());
                        break;
                    }
                }
                if (!foundProperty) {
                    ZonePropertyValue zonePropertyValue = new ZonePropertyValue();
                    zonePropertyValue.setZoneId( zoneId );
                    zonePropertyValue.setZonePropertyId( zonePropertyItemId );
                    zonePropertyValue.setValue( zonePropertyItemValue );
                    ZonePropertyValueService.getInstance().insert(zonePropertyValue);
                }

            }
        }

        //TODO delete this block or FIX as !zonePropertiesValids.contains(zonePropertyValue) it always is false which is a bug
        for (ZonePropertyValue zonePropertyValue : zonePropertyValuesList) {
            if(!zonePropertiesValids.contains(zonePropertyValue.getId())) {
                ZonePropertyValueService.getInstance().delete(zonePropertyValue);
            }
        }


    }

    public List<ZonePropertyValue> getZonePropertiesByZoneId ( Long id ) {
        HibernateQuery query = ZonePropertyValueService.getZonePropertyValueDAO().getQuery();
        return query.where(QZonePropertyValue.zonePropertyValue.zoneId.eq(id))
                .list(QZonePropertyValue.zonePropertyValue);
    }

    public static List<ZonePropertyValue> getZonePropertiesByZonePropertyId ( Long id ) {
        HibernateQuery query = ZonePropertyValueService.getZonePropertyValueDAO().getQuery();
        return query.where(QZonePropertyValue.zonePropertyValue.zonePropertyId.eq(id))
                .list(QZonePropertyValue.zonePropertyValue);
    }

    public String getZonePropertyValue(Long zoneId, Long zonePropertyId) {
        List<ZonePropertyValue>  values = getZonePropertyValueItem(zoneId, zonePropertyId);
        return values != null && values.size() > 0 ? values.get(0).getValue() : "";
    }

    public List<ZonePropertyValue> getZonePropertyValueItem(Long zoneId, Long zonePropertyId) {
        HibernateQuery query = ZonePropertyValueService.getZonePropertyValueDAO().getQuery();
        List<ZonePropertyValue>  values =  query.where(
                (QZonePropertyValue.zonePropertyValue.zonePropertyId.eq(zonePropertyId)).and(
                        QZonePropertyValue.zonePropertyValue.zoneId.eq(zoneId))
        ).list(QZonePropertyValue.zonePropertyValue);
        return values;
    }

    public List<ZonePropertyValue> getZonePropertyValueList(List<Long> zoneIds, List<Long> zonePropertyIds) {
        HibernateQuery query = ZonePropertyValueService.getZonePropertyValueDAO().getQuery();
        if(zoneIds != null && zonePropertyIds != null && zoneIds.size() > 0 && zonePropertyIds.size() > 0) {
            List<ZonePropertyValue> values = query.where(
                    (QZonePropertyValue.zonePropertyValue.zonePropertyId.in(zonePropertyIds)).and(
                            QZonePropertyValue.zonePropertyValue.zoneId.in(zoneIds))
            ).list(QZonePropertyValue.zonePropertyValue);
            return values;
        }
        return new LinkedList<>();
    }

    public List<String> getZonePropertyValue(Long zonePropertyId) {
        HibernateQuery query = ZonePropertyValueService.getZonePropertyValueDAO().getQuery();
        List<String>  values =  query.where(
                (QZonePropertyValue.zonePropertyValue.zonePropertyId.eq(zonePropertyId))
        ).list(QZonePropertyValue.zonePropertyValue.value);
        return values;
    }

	public List<ZonePropertyValue> getZonePropertyByValue( Group group, Long zonePropertyId, String operator, String value )
	{
		BooleanBuilder b = new BooleanBuilder();
		b.and( QZonePropertyValue.zonePropertyValue.zonePropertyId.eq( zonePropertyId ) );
		
		if( group != null )
		{
			BooleanBuilder groupBe = new BooleanBuilder();
			QGroup qGroup = QGroup.group;
			groupBe = groupBe.and( GroupService.getInstance().getDescendantsIncludingPredicate( qGroup, group ) );
			List<Group> listGroups = GroupService.getGroupDAO().selectAllBy( groupBe );
			logger.debug( "groups=" + listGroups );
			
			if( listGroups != null && listGroups.size() > 0 )
			{
				BooleanBuilder zbb = new BooleanBuilder();
				zbb = zbb.and( QZone.zone.group.id.in( getListOfIds( listGroups ) ) );
				List<Zone> listZones = ZoneService.getZoneDAO().selectAllBy( zbb );
				logger.debug( "zones=" + listZones );
				
				
				if( listZones != null && listZones.size() > 0 )
				{
					b = b.and( QZonePropertyValue.zonePropertyValue.zoneId.in( getListOfIds( listZones ) ) );
				}
			}
		}
		
		switch( operator )
		{
			//TODO: any differences here needed between type string and type boolean ?
            case Constants.OP_EQUALS:
                //logger.debug( " OP = value='" + value + "'" );
                b.and( QZonePropertyValue.zonePropertyValue.value.eq( value ) );
                break;

            case Constants.OP_NOT_EQUALS:
                //logger.debug( " OP != value='" + value + "'" );
                b.and( QZonePropertyValue.zonePropertyValue.value.notIn( value ) );
                break;

            case Constants.OP_IS_EMPTY://VIZIX-928
                BooleanBuilder emptyOperatorBuilder = new BooleanBuilder();
                emptyOperatorBuilder = emptyOperatorBuilder.or(QZonePropertyValue.zonePropertyValue.value.isEmpty()).or(
                        QZonePropertyValue.zonePropertyValue.value.isNull()
                );
                b.and(emptyOperatorBuilder);
                break;

            case Constants.OP_CONTAINS:
				b.and( QZonePropertyValue.zonePropertyValue.value.contains( value ) );
				break;
            case Constants.OP_IS_NOT_EMPTY: //VIZIX-928
                b.and(QZonePropertyValue.zonePropertyValue.value.isNotEmpty());
                break;
		}
		
		logger.debug( "bb='" + b.toString() + "'" );
		
		return ZonePropertyValueService.getZonePropertyValueDAO().selectAllBy( b );
	}

    public Map<String, ZonePropertyValue> getValuesFromZones(ReportDefinition reportDefinition, Map<Long, Zone> zoneListMap) {

        Set<Long> zonePropertiesIds = new HashSet<>();
        //ZonePropertiesId from Properties
        zonePropertiesIds.addAll(ReportPropertyService.getInstance().getZonePropertiesIds(reportDefinition.getReportProperty()));

        //ZonePropertiesId from Filters
        zonePropertiesIds.addAll(ReportFilterService.getInstance().getZonePropertiesIds(reportDefinition.getReportFilter()));

        //ZonePropertiesId from groupBy
        zonePropertiesIds.addAll(ReportGroupByService.getInstance().getZonePropertiesIds(reportDefinition.getReportGroupBy()));

        Map<String, ZonePropertyValue> zonePropertyValueMap = new HashMap<>();
        List<Long> zoneIds = new LinkedList<>();
        if(zonePropertiesIds != null && zonePropertiesIds.size() > 0) {
            for(Map.Entry<Long, Zone> item : zoneListMap.entrySet()) {
                zoneIds.add(item.getKey());
            }
        }

        List<ZonePropertyValue> zonePropertyValues = ZonePropertyValueService.getInstance().getZonePropertyValueList(zoneIds, new LinkedList<Long>(zonePropertiesIds));

        for(ZonePropertyValue zonePropertyValue : zonePropertyValues) {
            String zonePropertyValueId = zonePropertyValue.getZoneId() + "," + zonePropertyValue.getZonePropertyId();
            if (!zonePropertyValueMap.containsKey(zonePropertyValueId)) {
                zonePropertyValueMap.put(zonePropertyValueId, zonePropertyValue);
            }
        }

//            for(Map.Entry<Long, Zone> item : zoneListMap.entrySet()) {
//                Zone zone = item.getValue();
//                for(Long zonePropertyId : zonePropertiesIds) {
//                    String zonePropertyValueId = zone.getId() + "," + zonePropertyId;
//                    if (!zonePropertyValueMap.containsKey(zonePropertyValueId)) {
//                        long t1 = System.currentTimeMillis();
//                        List<ZonePropertyValue> zonePropertyValues = ZonePropertyValueService.getInstance().getZonePropertyValueItem(zone.getId(), zonePropertyId);
//                        if (zonePropertyValues != null && zonePropertyValues.size() > 0) {
//                            zonePropertyValueMap.put(zonePropertyValueId, zonePropertyValues.get(0));
//                        }
//                        long t2 = System.currentTimeMillis();
//                        logger.info("******* Zone execution_time=" + (t2 - t1 )  + "    -->>>  " + zone.getName() + " ->> " +  zonePropertyId);
//                    }
//                }
//            }
//        }

        return zonePropertyValueMap;
    }
    
    /*********************************
     * Method to get a List of Ids of an Object
     ********************************/
    public List<Long> getListOfIds(List<?> listOfObjects)
    {
        List<Long> response =  null;
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
                else
                {
                	throw new Error( "unsupported type=" + data );
                }
            }
        }
        return response;
    }

    /**
     *
     * @param zonePropertyId
     * @return a Zone Property Value List
     */
    public List<ZonePropertyValue> getZonePropertyValueExistValue(Long zonePropertyId) {
        HibernateQuery query = ZonePropertyValueService.getZonePropertyValueDAO().getQuery();
        List<ZonePropertyValue>  zonePropertyValues =  query.where(
                (QZonePropertyValue.zonePropertyValue.zonePropertyId.eq(zonePropertyId)).and(QZonePropertyValue.zonePropertyValue.value.isNotEmpty())
        ).list(QZonePropertyValue.zonePropertyValue);
        return zonePropertyValues;
    }

    public void deleteZonePropertyValues (List<ZonePropertyValue> zonePropertyValues){
        if ((zonePropertyValues != null) && (zonePropertyValues.size() > 0)){
            for (ZonePropertyValue zonePropertyValue:zonePropertyValues){
                if (zonePropertyValue.getValue() == null){
                    zonePropertyValue.setZonePropertyId(null);
                    zonePropertyValue.setZoneId(null);
                    ZonePropertyValueService.getInstance().delete(zonePropertyValue);
                }
            }
        }
    }

    public static List<ZonePropertyValue> getZonePropertyValues() {
        HibernateQuery query = ZonePropertyValueService.getZonePropertyValueDAO().getQuery();
        return query.list(QZonePropertyValue.zonePropertyValue);
    }

    public List<ZonePropertyValue> getZonePropertyValueByZoneId(Long zone) {
        HibernateQuery query = ZonePropertyValueService.getZonePropertyValueDAO().getQuery();
        List<ZonePropertyValue>  zonePropertyValues =  query.where(
                (QZonePropertyValue.zonePropertyValue.zoneId.eq(zone))
        ).list(QZonePropertyValue.zonePropertyValue);
        return zonePropertyValues;
    }


    public Map<String, String> getMapZonePropertyValue(Long zonePropertyId) {
        HibernateQuery query = ZonePropertyValueService.getZonePropertyValueDAO().getQuery();
        List<ZonePropertyValue> zonePropertyValues = query.where(QZonePropertyValue.zonePropertyValue.zonePropertyId.eq(zonePropertyId))
                .list(QZonePropertyValue.zonePropertyValue);
        Map<String, String> mapZonePropertyValue = new HashMap<>(zonePropertyValues.size());
        for (ZonePropertyValue zoneProperty : zonePropertyValues) {
            mapZonePropertyValue.put(zoneProperty.getZoneId() + "-" + zoneProperty.getZonePropertyId(), zoneProperty.getValue());
        }
        return mapZonePropertyValue;
    }
}