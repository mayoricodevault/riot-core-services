package com.tierconnect.riot.commons.sql.generator;

import com.tierconnect.riot.commons.dtos.DataTypeDto;
import com.tierconnect.riot.commons.dtos.ThingPropertyDto;

public interface SQLMapperInt {
    String getTableName(String name);

    String getColumnName(String name);

    String getPKType();

    String getFKType();

    String getStringType();

    String getTimestampType();

    String getSQLType(DataTypeDto dataType);

    String getSQLTimestampValue(String value);

    String getSQLValue(DataTypeDto dataType,
                       ThingPropertyDto udf);
}
