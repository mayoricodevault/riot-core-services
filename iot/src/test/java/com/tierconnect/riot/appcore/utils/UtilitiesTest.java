package com.tierconnect.riot.appcore.utils;

import com.tierconnect.riot.appcore.services.GroupService;
import org.junit.Test;

import static com.tierconnect.riot.appcore.utils.Utilities.*;
import static org.junit.Assert.*;

/**
 * Created by vealaro on 9/16/16.
 */
public class UtilitiesTest {

    @Test
    public void testAlphanumericsAndCharacterSpecials() {
        String pattern = GroupService.CHARACTER_SPECIALS_GROUP_NAME;
        assertTrue(isAlphaNumericCharacterSpecials("Group Name Test", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("Group Name Test @", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("Group Name Test &", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("Group Name Test $", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("Group Name Test _", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("Group @ Name Test", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("Group & Name Test", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("Group $ Name Test", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("@ Group Name Test", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("& Group Name Test", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("$ Group Name Test", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("$$$ Group &&& Name @@@   Test", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("$$$    Group   &&&   Name   @@@   Test", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("$$$    Group   &&& ___  Name   @@@   Test", pattern));
        assertFalse(isAlphaNumericCharacterSpecials("$(#) Group __ Name Test", pattern));
        assertFalse(isAlphaNumericCharacterSpecials("Group &(#) Name @ Test", pattern));
        assertFalse(isAlphaNumericCharacterSpecials("Group & Name @(#) Test", pattern));
        // add new characters for username
        pattern = "@_\\-\\\\.";
        assertTrue(isAlphaNumericCharacterSpecials("..punto.punto.", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("@arroba.@punto.", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("@punto.", pattern));
        assertFalse(isAlphaNumericCharacterSpecials("@..../slash/..", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("@--guion-medio-----@", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("@guion-medio@", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("..@.--guion-medio-----@.", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("_guion__.@.__bajo__", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("_guion__\\_\\__bajo__", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("\\DOMINIO\\usurio", pattern));
        assertFalse(isAlphaNumericCharacterSpecials("/DOMINIO/usurio", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("falvarez@tierconnect.com", pattern));
        assertTrue(isAlphaNumericCharacterSpecials("tierconnect\\falvarez", pattern));
    }

    @Test
    public void testAlphanumericsAndCharacterSpecialsDateFormat() {
        String pattern = GroupService.CHARACTER_SPECIALS_DATE_FORMAT;
        int index = 1;
        assertEquals("check case  " + (index++), true, isAlphaNumericCharacterSpecials("DD-MM-YYYY", pattern));
        assertEquals("check case  " + (index++), true, isAlphaNumericCharacterSpecials("dd-MM-YYYY", pattern));
        assertEquals("check case  " + (index++), true, isAlphaNumericCharacterSpecials("DD.MM.YYYY", pattern));
        assertEquals("check case  " + (index++), true, isAlphaNumericCharacterSpecials("tYY/MM/DD", pattern));
        assertEquals("check case  " + (index++), true, isAlphaNumericCharacterSpecials("tYY_MM_DD", pattern));
        assertEquals("check case  " + (index++), true, isAlphaNumericCharacterSpecials("tYY\\MM\\DD", pattern));
        assertEquals("check case  " + (index++), true, isAlphaNumericCharacterSpecials("[Yesterday at] LT", pattern));
        assertEquals("check case  " + (index++), true, isAlphaNumericCharacterSpecials("dddd [at] LT", pattern));
        assertEquals("check case  " + (index++), true, isAlphaNumericCharacterSpecials("dddd [at] LT", pattern));
        assertEquals("check case  " + (index++), true, isAlphaNumericCharacterSpecials("ddd, MMM D YYYY LT", pattern));
        assertEquals("check case  " + (index++), true, isAlphaNumericCharacterSpecials("ddd, MMM D YYYY LT", pattern));
        assertEquals("check case  " + (index++), true, isAlphaNumericCharacterSpecials("ddd, \"MMM\" D YYYY LT", pattern));
        assertEquals("check case  " + (index++), true, isAlphaNumericCharacterSpecials("ddd, /\"MMM\" D YYYY LT", pattern));
        assertEquals("check case  " + (index++), true, isAlphaNumericCharacterSpecials("ddd, &\"MMM\" D YYYY LT", pattern));
        assertEquals("check case  " + (index++), true, isAlphaNumericCharacterSpecials("ddd, (\"MMM\") D YYYY LT", pattern));
        assertEquals("check case  " + (index++), true, isAlphaNumericCharacterSpecials("MM/DD/YYYY hh:mm:ss A (X)", pattern));

        assertEquals("check case  " + (index++), false, isAlphaNumericCharacterSpecials("ddd, %\"MMM\" D YYYY LT", pattern));
        assertEquals("check case  " + (index++), false, isAlphaNumericCharacterSpecials("ddd, =\"MMM\" D YYYY LT", pattern));
        assertEquals("check case  " + (index++), false, isAlphaNumericCharacterSpecials("ddd, ?\"MMM\" D YYYY LT", pattern));
        assertEquals("check case  " + (index++), false, isAlphaNumericCharacterSpecials("ddd, ¿\"MMM\" D YYYY LT", pattern));
        assertEquals("check case  " + (index++), false, isAlphaNumericCharacterSpecials("ddd, !\"MMM\" D YYYY LT", pattern));
        assertEquals("check case  " + (index++), false, isAlphaNumericCharacterSpecials("ddd, ·\"MMM\" D YYYY LT", pattern));
    }

    @Test
    public void testisNumber() {
        assertTrue(isNumber("4"));
        assertTrue(isNumber("4.5"));
        assertFalse(isNumber("4,1"));
        assertTrue(isNumber("4.0"));
        assertFalse(isNumber("sdasdas"));
        assertTrue(isNumber("4.4546546546546546540"));
        assertTrue(isNumber("4454654654654654654546546546546546540"));
        assertFalse(isNumber(""));
        assertFalse(isNumber(null));
    }

    @Test
    public void testRemoveSpaces() {
        assertEquals("ABC", removeSpaces("ABC"));
        assertEquals("ABC", removeSpaces("ABC "));
        assertEquals("A B C", removeSpaces("A B C"));
        assertEquals("A B C", removeSpaces("A  B   C"));
        assertEquals("A B C", removeSpaces("  A  B  C "));
        assertEquals("A B C D", removeSpaces("  A  B           C              D      "));
    }

    @Test
    public void testTimeZone() {
        assertTrue(timeZoneIsValid("UTC"));
        assertTrue(timeZoneIsValid("America/Caracas"));
        assertTrue(timeZoneIsValid("Europe/London"));
        assertTrue(timeZoneIsValid("GMT"));
        assertFalse(timeZoneIsValid("DEV"));
        assertFalse(timeZoneIsValid("TOOLS"));
        assertFalse(timeZoneIsValid(""));
        assertFalse(timeZoneIsValid(null));
        assertFalse(timeZoneIsValid("SystemV/AST4"));
        assertFalse(timeZoneIsValid("SystemV/AST4ADT"));
        assertFalse(timeZoneIsValid("SystemV/CST6"));
        assertFalse(timeZoneIsValid("SystemV/CST6CDT"));
        assertFalse(timeZoneIsValid("SystemV/EST5"));
        assertFalse(timeZoneIsValid("SystemV/EST5EDT"));
        assertFalse(timeZoneIsValid("SystemV/HST10"));
        assertFalse(timeZoneIsValid("SystemV/MST7"));
        assertFalse(timeZoneIsValid("SystemV/MST7MDT"));
        assertFalse(timeZoneIsValid("SystemV/PST8"));
        assertFalse(timeZoneIsValid("SystemV/PST8PDT"));
        assertFalse(timeZoneIsValid("SystemV/YST9"));
        assertFalse(timeZoneIsValid("SystemV/YST9YDT"));
    }
}