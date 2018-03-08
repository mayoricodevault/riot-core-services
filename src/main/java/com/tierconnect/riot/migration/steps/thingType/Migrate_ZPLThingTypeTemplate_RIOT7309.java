package com.tierconnect.riot.migration.steps.thingType;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.popdb.PopDBIOTUtils;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.migration.DBHelper;
import com.tierconnect.riot.migration.steps.MigrationStep;
import org.apache.log4j.Logger;

import java.util.Date;
import java.util.List;

/**
 * Created by cvertiz
 * on 11/17/15.
 */
public class Migrate_ZPLThingTypeTemplate_RIOT7309 implements MigrationStep {
    private static Logger logger = Logger.getLogger(Migrate_ZPLThingTypeTemplate_RIOT7309.class);

    @Override
    public void migrateSQLBefore(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateRFIDEncode();
        migrateThingInstancesRFIDEncode();
    }

    /*Update Rfid Encode*/
    private void migrateRFIDEncode()
    {
        //Migrate ThingTypes
        List<ThingType> lstThingType = ThingTypeService.getInstance().getAllThingTypes();
        int cont = 0;
        for(ThingType thingType: lstThingType)
        {
            //Get all thingTypes of type ZPL template
            if(thingType.getThingTypeTemplate().getId().compareTo( 7L )==0)
            {
                cont = 0;
                for(ThingTypeField thingTypeField: thingType.getThingTypeFields())
                {
                    if(thingTypeField.getName().equals( "rfidEncode" )
                            && thingTypeField.getDataType().getId().compareTo( ThingTypeField.Type.TYPE_BOOLEAN.value )==0 )
                    {
                        cont++;
                    }
                }
                if(cont==0)
                {
                    PopDBIOTUtils.popThingTypeField( thingType, "rfidEncode", "", "", ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value,
                            ThingTypeField.Type.TYPE_BOOLEAN.value, false, "true", 16L,
                            ThingTypeField.Type.TYPE_BOOLEAN.value );
                }
            }
        }
        lstThingType.clear();
        lstThingType = null;

    }

    /*Update thing udf rfidEncode in Mongo*/
    private void migrateThingInstancesRFIDEncode()
    {
        //        db.getCollection('things').update(
        //                {thingTypeCode : "FMC.tag"},
        //        {
        //            $set : {NewUDF : {thingTypeFieldId:NumberLong(999), time : ISODate("2015-10-14T19:23:24.502Z"), value : true}}
        //        },
        //        { upsert: true, multi: true }
        //        )

        ThingTypeService.getThingTypeDAO().getSession().flush();
        ThingTypeFieldService.getThingTypeFieldDAO().getSession().flush();
        List<ThingType> lstThingType = ThingTypeService.getInstance().getAllThingTypes();
        for(ThingType thingType : lstThingType)
        {
            //get all thingType of type ZPL Template

            if(thingType.getThingTypeTemplate().getId().compareTo( 7L )==0)
            {
                List<ThingTypeField>  thingTypefield =  ThingTypeFieldService.getInstance().getThingTypeFieldByNameAndTypeCode(
                        "rfidEncode", thingType.getThingTypeCode() );
                //Modify all things with a specific thingTypeCode and they do not have rfidEncode udf
                BasicDBObject query1 = new BasicDBObject("thingTypeCode", thingType.getThingTypeCode() );
                BasicDBObject query2 = new BasicDBObject( "rfidEncode", new BasicDBObject( "$exists", false));
                BasicDBList condtionalOperator = new BasicDBList();
                condtionalOperator.add(query1);
                condtionalOperator.add(query2);
                BasicDBObject andQuery= new BasicDBObject( "$and", condtionalOperator);
                if(MongoDAOUtil.getInstance().things.find( andQuery ).size()>0)
                {
                    BasicDBObject rfidEncodeData = new BasicDBObject( "thingTypeFieldId", thingTypefield.get( 0 ).getId() )
                            .append( "time", new Date() ).append( "value", "true" );
                    BasicDBObject rfidEncode = new BasicDBObject( "rfidEncode", rfidEncodeData );
                    MongoDAOUtil.getInstance().things.update( andQuery, new BasicDBObject( "$set", rfidEncode ), true, true );
                }

            }

        }
    }

    @Override
    public void migrateSQLAfter(String scriptPath) throws Exception {
        DBHelper.executeSQLFile(scriptPath);
    }


}
