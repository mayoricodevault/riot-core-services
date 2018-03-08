package com.tierconnect.riot.api.database.base.key;

import org.bson.types.ObjectId;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by vealaro on 12/27/16.
 */
public class PrimaryKeyTest {
    private PrimaryKey primaryKey;

    @Test
    public void testCreate() {
        primaryKey = PrimaryKey.create("UNO", "java.lang.String");
        assertEquals(String.class, primaryKey.getClazz());
        assertEquals("PrimaryKey{value=UNO, clazz=class java.lang.String}", primaryKey.toString());
        primaryKey = PrimaryKey.create("UNO", ObjectId.class);
        assertEquals(ObjectId.class.getName(), primaryKey.getClazz().getName());
        assertEquals("PrimaryKey{value=UNO, clazz=class org.bson.types.ObjectId}", primaryKey.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreate1() {
        primaryKey = PrimaryKey.create("UNO", "java.lang.NumberTest");
    }
}