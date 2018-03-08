package com.tierconnect.riot.commons.dtos;

import com.tierconnect.riot.commons.serializers.DataType;
import org.junit.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * DataTypeDtoTest class.
 *
 * @author jantezana
 * @author achambi
 * @version 2017/11/08
 */
public class DataTypeDtoTest {

    @Test
    public void getDataTypeById()
    throws Exception {
        DataType dataType = DataType.getDataTypeById(12L);
        assertEquals(DataType.TYPE_URL, dataType);
        Method m = dataType.getClazz().getMethod(dataType.getParseMethodName(), dataType.getArgumentType());
        Object res = m.invoke(null, "hello");
        assertEquals("hello", res);

        DataType dataTypeNumber = DataType.getDataTypeById(4L);
        assertEquals(DataType.TYPE_NUMBER, dataTypeNumber);
        m = dataTypeNumber.getClazz().getMethod(dataTypeNumber.getParseMethodName(), dataTypeNumber.getArgumentType());
        res = m.invoke(null, "1212");
        assertEquals(new BigDecimal(1212).doubleValue(), res);

        DataType dataTypeTimeStamp = DataType.getDataTypeById(24L);
        assertEquals(DataType.TYPE_TIMESTAMP, dataTypeTimeStamp);
        m = dataTypeTimeStamp.getClazz().getMethod(dataTypeTimeStamp.getParseMethodName(), dataTypeTimeStamp.getArgumentType());
        res = m.invoke(null, "2016-11-08T15:12:50.601-04:00");
        assertEquals(true, res instanceof Long);
    }
}