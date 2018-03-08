package com.tierconnect.riot.appgen.service;

import java.lang.reflect.InvocationTargetException;

/**
 * Created by pablo on 2/5/15.
 * 
 * @deprecated NO REPLACEMENT
 */

public interface Getter {
    public Object getProperty( String name )
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException;
}
