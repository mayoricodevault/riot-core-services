package com.tierconnect.riot.iot.utils.sql.dialect;

/**
 * Dialect for Microsoft SQL Server 2012 for hibernate 4.3.11.Final
 *
 * Created by julio.rocha on 23-06-17.
 */
public class SQLServer2012CustomDialect extends SQLServer2008CustomDialect {
    @Override
    public boolean supportsSequences() {
        return true;
    }

    @Override
    public boolean supportsPooledSequences() {
        return true;
    }

    @Override
    public String getCreateSequenceString(String sequenceName) {
        return "create sequence " + sequenceName;
    }

    @Override
    public String getDropSequenceString(String sequenceName) {
        return "drop sequence " + sequenceName;
    }

    @Override
    public String getSelectSequenceNextValString(String sequenceName) {
        return "next value for " + sequenceName;
    }

    @Override
    public String getSequenceNextValString(String sequenceName) {
        return "select " + getSelectSequenceNextValString(sequenceName);
    }

    @Override
    public String getQuerySequencesString() {
        return "select name from sys.sequences";
    }
}
