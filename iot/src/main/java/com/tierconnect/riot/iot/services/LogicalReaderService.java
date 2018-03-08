package com.tierconnect.riot.iot.services;


import com.mysema.query.BooleanBuilder;
import com.mysema.query.group.GroupBy;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QGroup;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.commons.dtos.LogicalReaderDto;
import com.tierconnect.riot.commons.utils.TenantUtil;
import com.tierconnect.riot.iot.entities.LogicalReader;
import com.tierconnect.riot.iot.entities.QLogicalReader;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.utils.Translator;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LogicalReaderService extends LogicalReaderServiceBase
{
	// notify bridges there has been a zone update
	public void sendRefreshConfigurationMessage(boolean publishMessage, List<Long> groupMqtt)
	{
		BrokerClientHelper.sendRefreshLogicalReadersMessage(publishMessage, groupMqtt);
	}

	@Override
	public LogicalReader insert( LogicalReader logicalReader )
	{
		LogicalReader lr = super.insert( logicalReader );
		// RIOT-13659 send tickle for logical readers.
		sendRefreshConfigurationMessage(false, GroupService.getInstance().getMqttGroups(logicalReader.getGroup()));
		refreshCache(logicalReader, false);
		return lr;
	}

	@Override
	public LogicalReader update( LogicalReader logicalReader )
	{
		LogicalReader lr = super.update( logicalReader );
		// RIOT-13659 send tickle for logical readers.
		sendRefreshConfigurationMessage(false, GroupService.getInstance().getMqttGroups(logicalReader.getGroup()));
		refreshCache(logicalReader, false);
		return lr;
	}

	@Override
	public void delete( LogicalReader logicalReader )
	{
		List<Long> groupMqtt = GroupService.getInstance().getMqttGroups(logicalReader.getGroup());
		super.delete( logicalReader );
		// RIOT-13659 send tickle for logical readers.
		sendRefreshConfigurationMessage(false, groupMqtt);
		refreshCache(logicalReader, true);
	}

	/**
	 * This method update the topic ___v1___cache___logicalreader.
	 * @param logicalReader
	 * @param delete if it is true, then send a message null.
     */
	public void refreshCache( LogicalReader logicalReader, boolean delete )
	{
		BrokerClientHelper.refreshLogicalReaderCache( logicalReader, delete );
	}

	@Override
	public void validateInsert( LogicalReader logicalReader )
	{
		validateConstraintByNameAndGroup(logicalReader.getName(), logicalReader.getGroup().getId());
		validateConstraintByCodeAndGroup(logicalReader.getCode(), logicalReader.getGroup().getId());
		if(!(logicalReader.getZoneIn() != null && logicalReader.getZoneOut() != null)) {
			throw new RuntimeException("zoneIn or zoneOut are null");
		}
	}

	public void validateConstraintByNameAndGroup (String name, long groupId)
	{
		if (!getLogicalReaderDAO().validateDuplicatedNameAndGroup(name, groupId))
			throw new UserException ("Duplicate Name in Logical Reader: " + name);
	}

	public void validateConstraintByCodeAndGroup (String code, long groupId)
	{
		if (!getLogicalReaderDAO().validateDuplicatedCodeAndGroup(code, groupId))
			throw new UserException ("Duplicate Code in Logical Reader: " + code);
	}

	public LogicalReader getByCode(String code) throws NonUniqueResultException {
		try {
			return getLogicalReaderDAO().selectBy("code", code);
		}
		catch (org.hibernate.NonUniqueResultException e) {
			throw new NonUniqueResultException(e);
		}
	}

	/**
	 * This method gets a LogicalReader based on the code of the zone and the group
	 * */
	public LogicalReader getByCodeAndGroup(String code, String hierarchyPathGroup) throws NonUniqueResultException {
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
			b = b.and( QLogicalReader.logicalReader.group.id.in( ZoneService.getListOfIds( listGroups ) ) );
			b = b.and( QLogicalReader.logicalReader.code.eq(code) ) ;
			LogicalReader logicalReader = LogicalReaderService.getInstance().getLogicalReaderDAO().selectBy( b );

			return logicalReader;
		}
		catch (org.hibernate.NonUniqueResultException e) {
			throw new NonUniqueResultException(e);
		}
	}

	public LogicalReader getByName(String name) throws NonUniqueResultException {
		try {
			return getLogicalReaderDAO().selectBy("name", name);
		}
		catch (org.hibernate.NonUniqueResultException e) {
			throw new NonUniqueResultException(e);
		}
	}

	/*
	* @description This method checks if a Logical reader is valid or not based on its id or name
	* @params
	* id: id of Logical reader
	* name:  name of the logical reader
	* Method was deprecated on 2016/09/22
	* Deprecated version: 4.3.0_RC13
	* */
	@Deprecated
	public boolean isValidLogicalreader(Long id, String name)
	{
		// aruiz: TODO: The previous code would always return false and is incomplete and unused, this needs further revision
		//      commenting out so that it is not reported as null pointer reference by findBugs.
		return false;
		//boolean response = false;
		//if(id!=null)
		//{
		//	LogicalReader logicalReqader = this.get( id );
		//}else if(name!=null && !name.trim().equals( "" ))
		//{
		//	LogicalReader logicalReqader = this.get( id );
		//}
		//return response;
	}

	/**
	 * getLogicalReadersByNameLike
	 * @param name name to search in logical reader with like
     * @return logical reader list
     */
	public List<LogicalReader> getLogicalReadersByNameLike(String name) {
		HibernateQuery query = getLogicalReaderDAO().getQuery();
		BooleanBuilder logicalReaderWhereQuery = new BooleanBuilder(QLogicalReader.logicalReader.name.toLowerCase().like( "%"+name.toLowerCase()+"%" ));
		return query.where(logicalReaderWhereQuery).list(QLogicalReader.logicalReader);
	}

    /**
     * Returns all Logical Readers on a Map
     *
     * @return Map&lt;Long, LogicalReader&gt;
     */
    public Map<Long, LogicalReader> getMapLogicalReader() {
        HibernateQuery query = LogicalReaderService.getLogicalReaderDAO().getQuery();
        return query.transform(GroupBy.groupBy(QLogicalReader.logicalReader.id).as(QLogicalReader.logicalReader));
    }

	public List<LogicalReader> selectAllByZone(Zone zone) {
		HibernateQuery query = getLogicalReaderDAO().getQuery();
		BooleanBuilder logicalReaderWhereQuery = new BooleanBuilder(QLogicalReader.logicalReader.zoneIn.eq(zone)).or(
                QLogicalReader.logicalReader.zoneOut.eq(zone)
        );
		return query.where(logicalReaderWhereQuery).list(QLogicalReader.logicalReader);
	}

}
