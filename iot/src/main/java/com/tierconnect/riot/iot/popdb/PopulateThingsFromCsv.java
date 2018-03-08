package com.tierconnect.riot.iot.popdb;

import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.iot.services.ThingsService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by ezapata on 25-Jan-16.
 */
public class PopulateThingsFromCsv {
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger( PopulateThings.class );

    public static void populateThings()
    {

        try{
            ThingType rfid= ThingTypeService.getInstance().get(1L);
            ThingType gps = ThingTypeService.getInstance().get(2L);
            ThingType assetThingType = ThingTypeService.getInstance().getByCode( "asset_code" );


            Group santaMonica=GroupService.getInstance().get(3L);
            User rootUser = UserService.getInstance().getRootUser();
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream is = classloader.getResourceAsStream("csv/ViZix Retail Inventory Analytics - ViZixRetail.csv");
            String line="";

            BufferedReader br=new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            while ((line = br.readLine()) != null) {

                instantiateClothingItem( assetThingType, rfid,line, santaMonica, rootUser );
            }


        }catch(Exception e)
        {
            e.printStackTrace();

        }

    }


    /*
    * Filled clothing items
    * */
    public static void instantiateClothingItem( ThingType asssetThingType, ThingType rfidtag, String valuesT
            , Group group, User createdBy ) throws Exception
    {
		/*Set parent*/
		/*Set Udfs*/
        Date storageDate = new Date();
        //int serialChild = startSerial + i;
        String[] values = valuesT.split(",");
        String snChild = values[13];
        Stack<Long> recursivelyStack = new Stack<>();
        //Create the child RFID
        Map<String, Object> result = ThingsService.getInstance().create(recursivelyStack
                ,rfidtag.getThingTypeCode()
                ,group.getHierarchyName(false)
                ,snChild
                ,snChild
                ,null
                ,getRFIDItemMap( valuesT, storageDate.getTime(), rfidtag )//udf
                ,null, null
                ,false, false, storageDate, true, true);

        //Create a new Thing
        result = ThingsService.getInstance().create(recursivelyStack
                ,asssetThingType.getThingTypeCode()
                ,group.getHierarchyName(false)
                ,values[2]
                ,parserSerial(values[2])
                ,null
                ,getAssetUdf(valuesT, storageDate.getTime(), asssetThingType)//udf
                ,getChildMap(snChild,rfidtag.getThingTypeCode() )
                , null
                ,false, false, storageDate, true, true);
    }

    /**
     * Get the map for udf of clothing items
     */
    public static Map<String, Object> getAssetUdf(String valuesT, Long storageTime, ThingType assetThingType)
    {
        Map<String, Object> udf = new HashMap<String, Object>();
        Map<String, Object> fieldMap = new HashMap<String, Object>();
        String[] values = valuesT.split(",");
        for( ThingTypeField field : assetThingType.getThingTypeFields() )
        {
            fieldMap = new HashMap<String, Object>();
            switch( field.getName().toString() )
            {
                case ("SKU"):
                {

                        fieldMap.put( "thingTypeFieldId", field.getId() );
                        fieldMap.put( "value",values[3]);
                        fieldMap.put( "time", storageTime );

                    udf.put( field.getName().toString(),fieldMap );
                    break;
                }

                case ("Category"):

                        fieldMap.put( "thingTypeFieldId", field.getId() );
                        fieldMap.put( "value", values[6] );
                        fieldMap.put( "time", storageTime );

                    udf.put( field.getName().toString(),fieldMap );
                    break;
                case ("SubClass"):
                {

                        fieldMap.put( "thingTypeFieldId", field.getId() );
                        fieldMap.put( "value", values[7] );
                        fieldMap.put( "time", storageTime );

                    udf.put( field.getName().toString(),fieldMap );
                    break;
                }
                case ("Status"):

                        fieldMap.put( "thingTypeFieldId", field.getId() );
                        fieldMap.put( "value", values[10] );
                        fieldMap.put( "time", storageTime );

                    udf.put( field.getName().toString(),fieldMap );
                    break;


            }
        }
        return udf;
    }

    /**
     * Get the map for udf of rfid items
     */
    public static Map<String, Object> getRFIDItemMap(String valuesT, Long storageTime, ThingType rfidtag)
    {
        Map<String, Object> udf = new HashMap<String, Object>();
        Map<String, Object> fieldMap = new HashMap<String, Object>();
		/*Set Udfs*/
        String[] values = valuesT.split(",");
        for( ThingTypeField field : rfidtag.getThingTypeFields() )
        {
            fieldMap = new HashMap<String, Object>();
            switch( field.getName().toString() )
            {
				case ("eNode"):
                    fieldMap.put( "thingTypeFieldId", field.getId() );
                    fieldMap.put( "value", "x3ed9371" );
                    fieldMap.put( "time", storageTime );
                    udf.put( field.getName().toString(),fieldMap );
                    break;
            }
        }

        return udf;
    }

    /**
     * Get the map for children
     */
    public static List<Map<String, Object>> getChildMap(String serial, String thingTypeCode)
    {
        List<Map<String, Object>> children = new ArrayList<>();
        Map<String, Object> child = new HashMap<>();
        child.put( "serialNumber", serial );
        child.put( "thingTypeCode", thingTypeCode );
        children.add( child );
        return children;
    }

    public static String parserSerial(String x)
    {   x=x.replace("-","");
        return x.replace(" ","");
    }
}
