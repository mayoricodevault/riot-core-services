package com.tierconnect.riot.api.database.base.key;

import com.tierconnect.riot.api.assertions.Assertions;

/**
 * Created by vealaro on 12/20/16.
 */
public class PrimaryKey {

    private Object value;
    private Class<?> clazz;

    private PrimaryKey(Object value, String clazz) throws ClassNotFoundException {
        this.value = value;
        this.clazz = Class.forName(clazz);
    }

    private PrimaryKey(Object value, Class<?> clazz) {
        this.value = value;
        this.clazz = clazz;
    }

    public Object getValue() {
        return value;
    }

    public Class<?> getClazz() {
        return clazz;
    }


    public static PrimaryKey create(Object value, Class<?> clazz) {
        Assertions.voidNotNull("class", clazz);
        return new PrimaryKey(value, clazz);
    }

    public static PrimaryKey create(Object value, String clazz) {
        Assertions.voidNotNull("class", clazz);
        try {
            return new PrimaryKey(value, clazz);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(clazz + " not found ", e);
        }
    }

    @Override
    public String toString() {
        return "PrimaryKey{" +
                "value=" + value +
                ", clazz=" + clazz +
                '}';
    }
}
