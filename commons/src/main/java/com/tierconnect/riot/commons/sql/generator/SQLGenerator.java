package com.tierconnect.riot.commons.sql.generator;

import com.tierconnect.riot.commons.dtos.DataTypeDto;
import com.tierconnect.riot.commons.dtos.ThingDto;
import com.tierconnect.riot.commons.dtos.ThingPropertyDto;
import com.tierconnect.riot.commons.dtos.ThingTypeDto;
import com.tierconnect.riot.commons.dtos.ThingTypeFieldDto;

import java.util.ArrayList;
import java.util.List;

public class SQLGenerator {
    private ThingTypesWrapper tts;
    private ThingDto thing;
    private SQLMapperInt sqlm;

    public static void main(String[] args)
    throws Exception {
        //		File f1 = new File( args[0] );
        //		File f2 = new File( args[1] );

        //		String json1 = Files.toString( f1, Charsets.UTF_8 );
        //		String json2 = Files.toString( f2, Charsets.UTF_8 );
        //
        //		// SQLMapper sqlm = new SQLMapperHive();
        //		SQLMapperInt sqlm = new SQLMapperGenericSQL();
        //
        //		ThingTypesWrapper ttsw = ThingTypesWrapper.parse( json1 );
        //
        //		ThingDto tw = ThingDto.parse( json2 );
        //
        //		SQLGenerator sqlg = new SQLGenerator( sqlm, ttsw );
        //
        //		sqlg.setThingWrapper( tw );
        //
        //		System.out.println( "\n" + sqlg.generateCreateTableSQL() + "\n" );
        //
        //		for( String str : sqlg.generateCreateLinkSQL() )
        //		{
        //			System.out.println( str + "\n" );
        //		}
        //
        //		System.out.println( "\n" + sqlg.generateInsertSQL() + "\n" );
        //
        //		for( String str : sqlg.generateInsertParentLinksSQL() )
        //		{
        //			System.out.println( str + "\n" );
        //		}
        //
        //		for( String str : sqlg.generateInsertChildLinksSQL() )
        //		{
        //			System.out.println( str + "\n" );
        //		}
    }

    public SQLGenerator(SQLMapperInt sqlm,
                        ThingTypesWrapper ttsw) {
        this.sqlm = sqlm;
        this.tts = ttsw;
    }

    public void setThingWrapper(ThingDto tw) {
        this.thing = tw;
    }

    /**
     * <pre>
     * CREATE TABLE page_view(
     * viewTime INT,
     * userid BIGINT,
     * page_url STRING,
     * referrer_url STRING,
     * ip STRING
     * )
     * </pre>
     */
    public String generateCreateTableSQL() {
        StringBuffer sb = new StringBuffer();
        sb.append("CREATE TABLE " + sqlm.getTableName(thing.thingType.code) + " (\n");
        sb.append("id " + sqlm.getPKType() + ",\n");
        sb.append("serialNumber " + sqlm.getStringType() + ",\n");
        sb.append("name " + sqlm.getStringType() + ",\n");
        sb.append("createdDate " + sqlm.getTimestampType() + ",\n");
        sb.append("modifiedDate " + sqlm.getTimestampType() + ",\n");
        sb.append("time " + sqlm.getTimestampType() + ",\n");
        sb.append("groupId " + sqlm.getFKType() + ",\n");
        sb.append("thingTypeId " + sqlm.getFKType());

        ThingTypeDto tt = tts.getThingTypeWrapper(thing.thingType.id);
        for (ThingTypeFieldDto ttf : tt.getUdfs()) {
            // thing types (27) go in link tables
            if (ttf.dataTypeId != 27) {
                sb.append(",\n" + ttf.name + " " + sqlm.getSQLType(DataTypeDto.fromId(ttf.dataTypeId)));
                sb.append(",\n" + ttf.name + "Time " + sqlm.getTimestampType());
            }
        }

        sb.append(",\nPRIMARY KEY ( id, time )");

        sb.append("\n)");

        return sb.toString();
    }

    public List<String> generateCreateLinkSQL() {
        List<String> list = new ArrayList<String>();

        ThingTypeDto tt = tts.getThingTypeWrapper(thing.thingType.id);
        for (ThingTypeFieldDto ttf : tt.getUdfs()) {
            ThingPropertyDto udf = thing.getUdf(ttf.name);
            if (ttf.dataTypeId == 27 && udf != null) {
                StringBuffer sb = new StringBuffer();
                String code1 = tt.code;
                String code2 = thing.thingType.code;
                String tableName = sqlm.getTableName(code1 + "_" + code2);

                sb.append("CREATE TABLE " + tableName + " (");
                sb.append("time " + sqlm.getTimestampType() + ",\n");
                sb.append("parent " + sqlm.getFKType() + ",\n");
                sb.append("child " + sqlm.getFKType() + ",\n");
                sb.append("PRIMARY KEY ( time, parent, child )\n");
                sb.append(")");

                list.add(sb.toString());
            }
        }

        return list;
    }

    /**
     * <pre>
     * INSERT INTO table_name ( field1, field2,...fieldN ) VALUES ( value1, value2,...valueN );
     * </pre>
     *
     * @return
     */
    public String generateInsertSQL() {
        StringBuffer sb = new StringBuffer();
        sb.append("INSERT INTO " + sqlm.getTableName(thing.thingType.code) + " (");
        sb.append(" id, serialNumber, name, createdDate, modifiedDate, time, groupId, thingTypeId");

        ThingTypeDto tt = tts.getThingTypeWrapper(thing.thingType.id);
        for (ThingTypeFieldDto ttf : tt.getUdfs()) {
            // thing types (id=27) go in link tables !
            if (ttf.dataTypeId != 27) {
                sb.append(", " + ttf.name);
                sb.append(", " + ttf.name + "Time");
            }
        }

        sb.append(" ) VALUES ( ");
        sb.append(thing.id);
        sb.append(", \"" + thing.serialNumber + "\"");
        sb.append(", \"" + thing.name + "\"");
        sb.append(", " + sqlm.getSQLTimestampValue(thing.createdTime.toString()));
        sb.append(", " + sqlm.getSQLTimestampValue(thing.modifiedTime.toString()));
        sb.append(", " + sqlm.getSQLTimestampValue(thing.time.toString()));
        sb.append(", " + thing.group.id);
        sb.append(", " + thing.thingType.id);

        for (ThingTypeFieldDto ttf : tt.getUdfs()) {
            ThingPropertyDto udf = thing.getUdf(ttf.name);
            // thing types (id=27) go in link tables !
            if (ttf.dataTypeId != 27) {
                if (udf != null) {
                    sb.append("\n, " + sqlm.getSQLValue(DataTypeDto.fromId(ttf.dataTypeId), udf));
                    sb.append("\n, " + sqlm.getSQLTimestampValue(udf.time.toString()));
                } else {
                    sb.append("\n, " + "null");
                    sb.append("\n, " + "null");
                }
            }
        }

        sb.append(" )");

        return sb.toString();
    }

    public List<String> generateInsertParentLinksSQL() {
        List<String> list = new ArrayList<String>();

        ThingTypeDto tt = tts.getThingTypeWrapper(thing.thingType.id);
        for (ThingTypeFieldDto ttf : tt.getUdfs()) {
            ThingPropertyDto udf = thing.getUdf(ttf.name);
            // NOTE FK should be link tables !
            if (ttf.dataTypeId == 27 && udf != null) {
                StringBuffer sb = new StringBuffer();
                String time = sqlm.getSQLTimestampValue(thing.time.toString());
                String code1 = tt.code;
                String code2 = thing.thingType.code;
                String tableName = sqlm.getTableName(code1 + "_" + code2);
                String id1 = sqlm.getSQLValue(DataTypeDto.fromId(ttf.dataTypeId), udf);
                String id2 = String.valueOf(thing.id);

                sb.append("INSERT INTO " + tableName);
                sb.append(" VALUES ( " + time + ", " + id1 + ", " + id2 + " )");
                list.add(sb.toString());
            }
        }

        return list;
    }

    public List<String> generateInsertChildLinksSQL() {
        List<String> list = new ArrayList<String>();

        //TODO: !!!!

        return list;
    }

    // select distinct child from edgeBridge_default_rfid_thingtype where
    // time = (select max( time ) from edgeBridge_default_rfid_thingtype);
    public String generateListChildIdsSQL(String tableName) {
        //String code2 = thing.thingType.code;
        //String tableName = sqlm.getTableName( code1 + "_" + code2 );

        StringBuffer sb = new StringBuffer();
        sb.append("select distinct child from " + tableName + "where time = ( select max(time) from " + tableName + ")");
        return sb.toString();
    }
}
