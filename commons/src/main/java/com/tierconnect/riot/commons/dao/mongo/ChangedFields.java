package com.tierconnect.riot.commons.dao.mongo;

import java.util.Map;

/**
 * Created by cvertiz on 9/10/16.
 * Data contract for get change Fields
 */
public interface ChangedFields {
    Map<String, String> getChangedFields();
    Long getCurrentFieldDate(String fieldName);
}
