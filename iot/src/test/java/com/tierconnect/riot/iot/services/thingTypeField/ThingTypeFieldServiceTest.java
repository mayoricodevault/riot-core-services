package com.tierconnect.riot.iot.services.thingTypeField;

import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

/**
 * Created by rsejas on 5/9/16.
 */
public class ThingTypeFieldServiceTest {

    @Test
    public void testIsBoolean() throws Exception {
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isBoolean("ASDF"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isBoolean("false"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isBoolean("False"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isBoolean("FALSE"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isBoolean("true"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isBoolean("TRUE"));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isBoolean("TEST"));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isBoolean("123"));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isBoolean(123456));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isBoolean(null));
    }

    @Test
    public void testIsCoordinates() throws Exception {
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isCoordinates("10;100;A"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isCoordinates("10;100;10"));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isCoordinates(123456));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isCoordinates(null));
    }

    @Test
    public void testIsDate() throws Exception {
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isDate("25-05-2016"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isDate("05/25/2016"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isDate("25/05/2016"));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isDate("test"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isDate(new Date()));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isDate(123456));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isDate(null));
    }

    @Test
    public void testIsExpression() throws Exception {
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isExpression("value"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isExpression("ABC${color}"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isExpression(123456));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isExpression(null));
    }

    @Test
    public void testIsNumber() throws Exception {
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isNumber("value"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isNumber("12345"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isNumber(123456));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isNumber(null));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isNumber("-123123123123123123.123123123"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isNumber("-123123123123123123.123123123"));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isNumber("-AAAAAAA.123123123"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isNumber("1.44485E+12"));
    }

    @Test
    public void testIsNumberDouble() throws Exception {
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isNumberFloat("value"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isNumberFloat("123.45"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isNumberFloat(1234.56));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isNumberFloat(null));
    }

    @Test
    public void testIsZPLScript() throws Exception {
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isZPLScript("value"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isZPLScript("CT~~CD,~CC^~CT~^XA~TA000~JSN^LT0^MNW^MTT^FD${product}^FS^FD${batchId}^FS^FT0,525,0^^FD${serialNumber}^FS^RFW,H^FD${serialNumber}^FS^PQ1,0,1,Y^XZ"));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isZPLScript(123456));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isZPLScript(null));
    }

    @Test
    public void testIsURL() throws Exception {
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isURL("value"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isURL("https://www.google.com.bo/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png"));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isURL(123456));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isURL(null));
    }

    @Test
    public void testIsValidLocation() throws Exception {
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isValidLocation("value"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isValidLocation("10;100;10"));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isValidLocation(123456));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isValidLocation(null));
    }

    @Test
    public void testIsEmptyData() throws Exception {
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isEmptyData("value"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isEmptyData(""));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isEmptyData(123456));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isEmptyData(124.45));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isEmptyData(null));
    }

    @Test
    public void testIsValidLengthLocation() throws Exception {
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isValidLengthLocation("value"));
        Assert.assertEquals(true, ThingTypeFieldService.getInstance().isValidLengthLocation("a;b;c"));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isValidLengthLocation(1234));
        Assert.assertEquals(false, ThingTypeFieldService.getInstance().isValidLengthLocation(null));
    }
}