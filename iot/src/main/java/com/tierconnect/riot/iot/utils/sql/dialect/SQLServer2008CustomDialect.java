package com.tierconnect.riot.iot.utils.sql.dialect;

import org.hibernate.dialect.SQLServer2005Dialect;
import org.hibernate.dialect.function.NoArgSQLFunction;
import org.hibernate.type.StandardBasicTypes;

import java.sql.Types;

/**
 * Dialect for Microsoft SQL Server 2008 for hibernate 4.3.11.Final
 *
 * Created by julio.rocha on 23-06-17.
 */
public class SQLServer2008CustomDialect extends SQLServer2005Dialect {
    private static final int NVARCHAR_MAX_LENGTH = 4000;

    /**
     * Constructs a custom dialect
     */
    public SQLServer2008CustomDialect() {

        registerColumnType(Types.DATE, "date");
        registerColumnType(Types.TIME, "time");
        registerColumnType(Types.TIMESTAMP, "datetime2");
        registerColumnType(Types.NVARCHAR, NVARCHAR_MAX_LENGTH, "nvarchar($l)");
        registerColumnType(Types.NVARCHAR, "nvarchar(MAX)");
        registerColumnType(Types.NCHAR, NVARCHAR_MAX_LENGTH, "nchar($l)");
        registerColumnType(Types.NCHAR, "nchar(MAX)");

        registerHibernateType(Types.NVARCHAR, NVARCHAR_MAX_LENGTH, "string");
        registerHibernateType(Types.NCHAR, NVARCHAR_MAX_LENGTH, "string");

        registerFunction(
                "current_timestamp", new NoArgSQLFunction("current_timestamp", StandardBasicTypes.TIMESTAMP, false)
        );
    }
}
