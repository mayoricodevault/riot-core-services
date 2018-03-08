package com.tierconnect.riot.iot.services;

import com.mysema.query.BooleanBuilder;
import com.mysema.query.group.GroupBy;
import com.mysema.query.jpa.hibernate.HibernateQuery;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.QGroup;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.utils.Utilities;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.sdk.dao.NonUniqueResultException;
import com.tierconnect.riot.sdk.dao.UserException;

import javax.annotation.Generated;
import java.util.*;

@Generated("com.tierconnect.riot.appgen.service.GenService")
public class ShiftService extends ShiftServiceBase
{
	// notify bridges there has been a zone update
	public void sendRefreshConfigurationMessage(boolean publishMessage, List<Long> groupMqtt)
	{
		BrokerClientHelper.sendRefreshShiftsMessage(publishMessage, groupMqtt);
	}

	@Override
    public Shift insert(Shift shift) {
        Shift s = super.insert(shift);
        s.setCode(shift.getCode());
        update(s);
        // RIOT-13659 send tickle for shift.
        sendRefreshConfigurationMessage(false, GroupService.getInstance().getMqttGroups(shift.getGroup()));
        return s;
    }

	@Override
    public Shift update(Shift shift) {
        shift.setCode(shift.getCode());
        Shift s = super.update(shift);
        // RIOT-13659 send tickle for shift.
        sendRefreshConfigurationMessage(false, GroupService.getInstance().getMqttGroups(shift.getGroup()));
        return s;
    }

    @Override
    public void delete(Shift shift) {
        List<Long> groupMqtt = GroupService.getInstance().getMqttGroups(shift.getGroup());
        super.delete(shift);
        // RIOT-13659 send tickle for shift.
        sendRefreshConfigurationMessage(false, groupMqtt);
    }


    public Shift getByName(String name) throws NonUniqueResultException {
        try {
            return getShiftDAO().selectBy("name", name);
        }
        catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }

    /*
    * Select Zone by code
    * */
    public Shift getByCode(String code) throws NonUniqueResultException {
        try {
            return getShiftDAO().selectBy("code", code);
        }
        catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }

    /**
     * This method gets a Shift based on the code of the zone and the group
     * */
    public Shift getByCodeAndGroup(String code, String hierarchyPathGroup) throws NonUniqueResultException {
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
            b = b.and( QShift.shift.group.id.in( ZoneService.getListOfIds( listGroups ) ) );
            b = b.and( QShift.shift.code.eq(code) ) ;
            Shift result = getShiftDAO().selectBy( b );

            return result;
        }
        catch (org.hibernate.NonUniqueResultException e) {
            throw new NonUniqueResultException(e);
        }
    }

    public boolean isInShift(long thingTypeId, String serialNumber, long shiftId) {
        ThingType thingType = ThingTypeService.getInstance().get(thingTypeId);
        if (thingType == null) {
            return false;
        }
        QThing qThing = QThing.thing;
        Thing thing = ThingService.getInstance().getThingDAO().getQuery().where(qThing.serial.eq(serialNumber).and(qThing.thingType.id.eq(thingTypeId))).uniqueResult(qThing);
        if (thing == null) {
            return false;
        }
        Shift shift = ShiftService.getInstance().get(shiftId);
        if (shift == null) {
            return false;
        }
        return isInShift(thing, shift);
    }

    public boolean isInShift(Thing thing, Shift shift) {
        ShiftThingService shiftThingService = ShiftThingService.getInstance();
        QShiftThing qShiftThing = QShiftThing.shiftThing;
        ShiftThing shiftThing = shiftThingService.getShiftThingDAO().getQuery().where(qShiftThing.shift.eq(shift).and(qShiftThing.thing.eq(thing))).uniqueResult(qShiftThing);
        if (shiftThing == null) {
            return false;
        }
        return true;
    }

    public List<Shift> findAllByThing(Thing thing) {
        ShiftThingService shiftThingService = ShiftThingService.getInstance();
        QShiftThing qShiftThing = QShiftThing.shiftThing;
        List<Shift> shifts = shiftThingService.getShiftThingDAO().getQuery().where(qShiftThing.thing.eq(thing)).list(qShiftThing.shift);
        return shifts;
    }

    public boolean isAllowedInZone(long thingTypeId, String serialNumber, long zoneId) {
        ThingType thingType = ThingTypeService.getInstance().get(thingTypeId);
        if (thingType == null) {
            return false;
        }
        QThing qThing = QThing.thing;
        Thing thing = ThingService.getInstance().getThingDAO().getQuery().where(qThing.serial.eq(serialNumber).and(qThing.thingType.id.eq(thingTypeId))).uniqueResult(qThing);
        if (thing == null) {
            return false;
        }
        Zone zone = ZoneService.getInstance().get(zoneId);
        if (zone == null) {
            return false;
        }
        return isAllowedInZone(thing, zone);
    }

    public boolean isAllowedInZone(Thing thing, Zone zone) {
        ShiftThingService shiftThingService = ShiftThingService.getInstance();
        QShiftThing qShiftThing = QShiftThing.shiftThing;
        List<Shift> shifts = shiftThingService.getShiftThingDAO().getQuery().where(qShiftThing.thing.eq(thing)).list(qShiftThing.shift);
        if (shifts.isEmpty()) {
            return false;
        }
        Calendar calendar = new GregorianCalendar();
        List<Shift> filteredShifts = new ArrayList<>();
        QShiftZone qShiftZone = QShiftZone.shiftZone;
        for (Shift shift: shifts) {
            List<Long> oldZoneIds = ShiftZoneService.getInstance().getShiftZoneDAO().getQuery().where(qShiftZone.shift.eq(shift)).list(qShiftZone.zone.id);
            if (oldZoneIds.contains(zone.getId())) {
                filteredShifts.add(shift);
            }
        }
        return IsAllowed(filteredShifts, calendar);
    }

    public List<String> getShiftNamesFromIds(List<Long> shiftIds) {
        HibernateQuery query = ShiftService.getShiftDAO().getQuery();

        if(shiftIds == null || shiftIds.size() == 0) return new LinkedList<>();

        return query.where(QShift.shift.id.in(shiftIds))
                .list(QShift.shift.name);
    }

    /**
     * Returns all Shifts on a Map
     *
     * @return Map&lt;Long, Shift&gt;
     */
    public Map<Long, Shift> getMapShift() {
        HibernateQuery query = ShiftService.getShiftDAO().getQuery();
        return query.transform(GroupBy.groupBy(QShift.shift.id).as(QShift.shift));
    }

    public boolean IsAllowed(List<Shift> shifts, Calendar calendar) {
        int dayOfTheWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int minuteOfDate = calendar.get(Calendar.MINUTE);
        int time = hourOfDay * 100 + minuteOfDate;
        for (Shift shift : shifts) {
                boolean dayInShift = inDay(shift.getDaysOfWeek(), dayOfTheWeek);
                if (dayInShift) {
                    if ((shift.getEndTimeOfDay() > shift.getStartTimeOfDay()) && (shift.getStartTimeOfDay() <= time && time <= shift.getEndTimeOfDay())) {
                        return true;
                    } else if ((shift.getEndTimeOfDay() < shift.getStartTimeOfDay()) && (shift.getStartTimeOfDay() <= time)) {
                        return true;
                    }
                } else {
                    boolean dayMinusOneInShift = inDay(shift.getDaysOfWeek(), substract(dayOfTheWeek));
                    if ((shift.getEndTimeOfDay() < shift.getStartTimeOfDay()) && (dayMinusOneInShift && time <= shift.getEndTimeOfDay())) {
                        return true;
                    }
                }
        }
        return false;
    }

    private boolean inDay(String shiftDays, int dayOfTheWeek) {
        return shiftDays.contains(String.valueOf(dayOfTheWeek));
    }

    private Integer substract(int dayOfWeek) {
        if (dayOfWeek == Calendar.SUNDAY) {
            return Calendar.SATURDAY;
        }
        return dayOfWeek -1;
    }

    private Integer add(int dayOfWeek) {
        if (dayOfWeek == Calendar.SATURDAY) {
            return Calendar.SUNDAY;
        }
        return dayOfWeek +1;
    }

    @Deprecated
    private String generateCode(Shift shift){
        return shift.getName() + String.format("%4s",shift.getId()).replace(' ', '0');
    }

    @Override
    public void validateUpdate(Shift shift) {
        super.validateUpdate(shift);
        validateShiftFieldRequired(shift);
        validateUniqueName(shift);
    }

    @Override
    public void validateInsert(Shift shift) {
        super.validateInsert(shift);
        validateShiftFieldRequired(shift);
        validateUniqueName(shift);
    }

    public void validateShiftFieldRequired(Shift shift) {
        if (Utilities.isEmptyOrNull(shift.getCode())) {
            throw new UserException("Shift Code is required.");
        }
        if (Utilities.isEmptyOrNull(shift.getName())) {
            throw new UserException("Shift 'Name' is required.");
        }
        if (Utilities.isEmptyOrNull(shift.getDaysOfWeek())) {
            throw new UserException("Shift 'Schedule' is required.");
        }
        if (shift.getStartTimeOfDay() == null) {
            throw new UserException("Start time of day of the Shift's schedule should have a value");
        }
        if (shift.getEndTimeOfDay() == null) {
            throw new UserException("End time of day of the Shift's schedule should have a value");
        }
    }

    public void validateUniqueName(Shift shift) {
        QShift shift1 = QShift.shift;
        // Insert
        HibernateQuery query = getShiftDAO().getQuery();
        if (shift.getId() == null) {
            Long count = query.where(shift1.name.eq(shift.getName()).and(shift1.group.parentLevel2.eq(shift.getGroup().getParentLevel2()))).count();
            if (count > 0) {
                throw new UserException("Duplicated Name");
            }
        }
        // Update
        else {
            Long count = query.where(shift1.name.eq(shift.getName()).and(shift1.group.parentLevel2.eq(shift.getGroup().getParentLevel2()).and(shift1.id.ne(shift.getId())))).count();
            if (count > 0) {
                throw new UserException("Duplicated Name");
            }
        }
   }

    /**
     *
     * @param shift
     * @param delete
     */
	public void refreshCache( Shift shift, boolean delete )
	{
		BrokerClientHelper.refreshShiftCache( shift, delete );
		BrokerClientHelper.refreshShiftZoneCache( shift, delete );
	}
}

