package com.tierconnect.riot.iot.dao.mongo;

import com.mongodb.*;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import org.apache.log4j.Logger;

import java.util.*;

public class ThingTypeMongoDAO
{

    private static Logger logger = Logger.getLogger( ThingTypeMongoDAO.class );

    static ThingTypeMongoDAO instance;

    DBCollection thingTypesCollection;

    BasicDBObject docs[];

    BulkWriteOperation builder;

    Set<Long> set = new HashSet<Long>();

    public static ThingTypeMongoDAO getInstance()
    {
        if( instance == null )
        {
            instance = new ThingTypeMongoDAO();
            instance.thingTypesCollection = MongoDAOUtil.getInstance().thingTypesCollection;
        }
        return instance;
    }

    public void dropThingTypesCollection()
    {
        logger.info( "dropping thingTypes collection" );
        thingTypesCollection.drop();
        set.clear();
    }

    public int insertAllThingTypes( List<ThingType> thingTypes ) {

        int count = 0;
        for(ThingType tt : thingTypes )
        {
            insertThingType(tt);
            count++;
        }

        return count;
    }

    public void insertThingType( ThingType thingType ) {

        logger.info( "thingTypeId=" + thingType.getId() + " name=" + thingType.getName() );

        List<ThingType> childrenResult = thingType.getChildren();
        List<ThingType> parentsResult = thingType.getParents();
        Set<ThingTypeField> fieldsResult = thingType.getThingTypeFields();

        Map<String, Long> children = new HashMap<>();
        Map<String, Long> parents = new HashMap<>();
        List< Map< String, Object > > fields = new ArrayList<>();

        for(ThingType child : childrenResult)
        {
            children.put("_id", child.getId());
        }

        for(ThingType parent : parentsResult)
        {
            children.put("_id", parent.getId());
        }

        for(ThingTypeField ttf : fieldsResult)
        {
            Map<String, Object> field = new HashMap<>();
            field.put("_id", ttf.getId());
            field.put("name", ttf.getName());
            field.put("type", ttf.getDataType().getId());
            field.put("unit", ttf.getUnit());
            field.put("symbol", ttf.getSymbol());
            field.put("timeSeries", ttf.getTimeSeries());

            fields.add(field);
        }

        BasicDBObject doc = new BasicDBObject( "_id", thingType.getId() );
        doc.append( "name", thingType.getName() );
        doc.append( "archived", thingType.isArchived() );
        doc.append( "modifiedTime", thingType.getModifiedTime() );
        doc.append( "thingTypeCode", thingType.getThingTypeCode() );
        doc.append( "children", children );
        doc.append("parents", parents );
        doc.append( "autoCreate", thingType.isAutoCreate() );
        doc.append( "fields", fields );

        thingTypesCollection.insert( doc );

        // add to set of known things
        set.add( thingType.getId() );
    }
}
