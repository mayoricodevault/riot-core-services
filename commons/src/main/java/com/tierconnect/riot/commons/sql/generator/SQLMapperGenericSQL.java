package com.tierconnect.riot.commons.sql.generator;

import com.tierconnect.riot.commons.dtos.DataTypeDto;
import com.tierconnect.riot.commons.dtos.ThingPropertyDto;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SQLMapperGenericSQL implements SQLMapperInt {
    private static final String DFS = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    Pattern p = Pattern.compile("id=(\\d+)");

    @Override
    public String getTableName(String name) {
        return name;
    }

    @Override
    public String getColumnName(String name) {
        return name;
    }

    @Override
    public String getPKType() {
        // return "BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY";
        return "BIGINT NOT NULL";
    }

    @Override
    public String getFKType() {
        return "BIGINT";
    }

    @Override
    public String getStringType() {
        return "VARCHAR(255)";
    }

    @Override
    public String getTimestampType() {
        return "BIGINT";
    }

    public String getSQLType(DataTypeDto dataType) {
        switch (dataType.id.intValue()) {
            case 1:
                return "VARCHAR(255)";

            // code=COORDINATES
            case 2:
                return "VARCHAR(255)";

            // code=XYZ
            case 3:
                return "VARCHAR(255)";

            case 4:
                return "DOUBLE";

            case 6:
                return "BLOB";

            // 7=Shift
            case 7:
            case 9:
            case 22:
            case 23:
            case 24:
                return "BIGINT";

            default:
                return "UNMAPPED dataType.code=" + dataType.code + "\n";
        }
    }

    @Override
    public String getSQLTimestampValue(String value) {
        DateFormat df = new SimpleDateFormat(DFS);
        Date d = null;
        try {
            d = df.parse(value);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return String.valueOf(d.getTime());
    }

    @Override
    public String getSQLValue(DataTypeDto dataType,
                              ThingPropertyDto udf) {
        // System.out.println( "**** id=" + dataType.id + " code=" +
        // dataType.code + "\n" );
        switch (dataType.id.intValue()) {
            // location
            case 2:
                return "'" + String.valueOf(udf.value) + "'";

            // localtionXYZ
            case 3:
                return "'" + String.valueOf(udf.value) + "'";

            case 24:
                return getSQLTimestampValue(String.valueOf(udf.value));

            // 7=SHIFT
            case 7:
                // 9=ZONE
            case 9:
                // 22=GROUP
            case 22:
                // 23=LOGICAL READER
            case 23:
                // 27=THING
            case 27:
                Matcher m = p.matcher(String.valueOf(udf.value));
                if (m.find()) {
                    return m.group(1);
                }
                return "-1";

            default:
                return "'" + String.valueOf(udf.value) + "'";
        }
    }
}
