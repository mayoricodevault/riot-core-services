package com.tierconnect.riot.iot.services.shift;

import com.tierconnect.riot.appcore.core.BaseTestIOT;
import com.tierconnect.riot.iot.entities.Shift;
import com.tierconnect.riot.iot.services.ShiftService;
import com.tierconnect.riot.iot.services.ShiftServiceBase;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.*;


/**
 * Created by agutierrez on 1/14/2015.
 */
public class ShiftServiceTest extends BaseTestIOT {

    private static Logger logger = Logger.getLogger(ShiftServiceTest.class);

    @Test
    public void testIsAllowed() {
        ShiftService shiftService = new ShiftService();
        List<Shift> list = new ArrayList<>();
        Shift s1 = new Shift();
        s1.setStartTimeOfDay(900L);
        s1.setEndTimeOfDay(1700L);
        s1.setDaysOfWeek("23456");
        list.add(s1);
        //Tests on a monday
        assertFalse(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 1)));
        assertFalse(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 1, 8, 0)));
        assertTrue(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 1, 10, 0)));
        assertTrue(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 1, 16, 0)));
        assertFalse(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 1, 20, 0)));
        assertFalse(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 1, 8, 59)));
        assertTrue(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 1, 9, 0)));
        assertTrue(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 1, 9, 1)));
        assertTrue(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 1, 16, 59)));
        assertTrue(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 1, 17, 0)));
        assertFalse(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 1, 17, 1)));

        //Tests on a sunday
        assertFalse(shiftService.IsAllowed(list, new GregorianCalendar(2012, 8, 30, 10, 0)));
        assertFalse(shiftService.IsAllowed(list, new GregorianCalendar(2012, 8, 30, 16, 0)));

        list = new ArrayList<>();
        s1 = new Shift();
        s1.setStartTimeOfDay(2200L);
        s1.setEndTimeOfDay(600L);
        s1.setDaysOfWeek("2");
        list.add(s1);
        //test on an overnight shift from monday to tuesday
        assertFalse(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 1, 21, 59)));
        assertTrue(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 1, 22, 0)));
        assertTrue(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 1, 22, 1)));
        assertTrue(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 1, 23, 59)));
        assertTrue(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 2, 00, 00)));
        assertTrue(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 2, 00, 01)));
        assertTrue(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 2, 5, 59)));
        assertTrue(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 2, 6, 00)));
        assertFalse(shiftService.IsAllowed(list, new GregorianCalendar(2012, 9, 2, 6, 01)));

    }

    @Test
    public void testGetByCode() throws Exception {
        logger.info("get SHIFT by code");
        ShiftService service = ShiftServiceBase.getInstance();
        assertNotNull(service);
        Shift shift = service.getByCode("DAY-M-W0001");
        assertNotNull(shift);
        assertEquals(shift.getCode(), "DAY-M-W0001");
        assertEquals(shift.getDaysOfWeek(), "23456");
    }

}
