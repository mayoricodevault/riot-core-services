package com.tierconnect.riot.iot.utils;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.text.DecimalFormat;

/**
 * Created by achambi on 8/18/17.
 */
public class NumberToStringFormat {
    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void stringToDecimal() throws Exception {
        DecimalFormat decimalFormat =  new DecimalFormat("#.##############");
        Assert.assertEquals("123456789012345",  decimalFormat.format(123456789012345D));
        Assert.assertEquals("123456789.1",  decimalFormat.format(123456789.1D));
        Assert.assertEquals("123456789.12345",  decimalFormat.format(123456789.12345D));
        Assert.assertEquals("156923234.001525",  decimalFormat.format( 156923234.001525D));
        Assert.assertEquals("156923234.01154",  decimalFormat.format(  156923234.01154D));
        Assert.assertEquals("156923234",  decimalFormat.format(  156923234.0000D));
        Assert.assertEquals("156923234",  decimalFormat.format(   156923234.0000D));
    }
}
