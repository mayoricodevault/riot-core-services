package com.tierconnect.riot.iot.services;

import java.util.*;

import com.tierconnect.riot.appcore.services.ConfigurationService;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.iot.dao.mongo.ThingMongoDAO;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.utils.rest.MD7ResponseHandler;
import com.tierconnect.riot.iot.utils.rest.RestCallException;
import com.tierconnect.riot.iot.utils.rest.RestClient;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


/**
 * Created by pablo on 1/7/15.
 *
 * Handles operations with ThingEx(similar to a repository).
 */
public class ThingExService {
    static Logger logger = Logger.getLogger(FileExportService.class);

    static private ThingExService INSTANCE = new ThingExService();

	private List<String> filterFields;

	public ThingExService() {
		this.filterFields = new ArrayList<>(  );
	}

	public ThingExService(List<String> filterFields) {
		this.filterFields = filterFields;
	}

	/** @deprecated */
    public static ThingExService getInstance()
    {
        return INSTANCE;
    }

    public List<ThingEx> listByThingType(final ThingType thingType)
    {
        logger.info("thing type id " + thingType.getId());
        List<Thing> things = ThingService.getInstance().selectByThingType(thingType);
        List<ThingEx> thingExs = new ArrayList<>();

        logger.info("thing size " + things.size());

        final Map<Long, Map<String, Map<String, Object>>> allFieldValues = new HashMap<>();

        for(Thing thing: things){
            allFieldValues.put(thing.getId(), (Map)ThingMongoDAO.getInstance().getThing(thing.getId()));
        }

        for (final Thing thing : things)
        {
            thingExs.add(new ThingEx()
            {
                @Override
                public Map<String, Object> publicMap()
                {
                    Map<String, Object> values = new LinkedHashMap<String, Object>();
                    values.put("group", thing.getGroup().getName());
                    values.put("thingType", thing.getThingType().getThingTypeCode());
                    values.put("serial", thing.getSerial());
                    values.put("name", thing.getName());

                    //put values from non sql storage
                    for (ThingTypeField thingTypeField : thing.getThingType().getThingTypeFields())
                    {
                        Map<String, Object> fieldValues = (Map)allFieldValues.get(thingTypeField.getName());
                        String value = fieldValues != null ? (String)fieldValues.get("thingTypeFieldId") : "";
                        logger.debug("Thing type " + thingTypeField.getDataType().getId());
                        //do export shifts as names not ids
                        if(thingTypeField.getDataType().getId().compareTo( ThingTypeField.Type.TYPE_SHIFT.value )==0) {
                            value = getShiftNames(value);
                        }

                        //export boolean lower case
                        if(thingTypeField.getDataType().getId().compareTo( ThingTypeField.Type.TYPE_BOOLEAN.value) ==0) {
                            value = value.toLowerCase();
                        }

                        values.put(thingTypeField.getName(), value);
                    }

                    //add parent info
                    Thing parent = thing.getParent();
                    if(parent != null)
                    {
                        values.put("parent", parent.getSerial());
                    }


                    return values;
                }

                @Override
                public boolean hasParent() {
                    Thing parent = thing.getParent();
                    return parent != null;
                }
            });
        }


        return thingExs;
    }

    /******************************
     * This method get list of things based on a query
     ******************************/
   /* public List<ThingEx> listThingsByFilters(final String where) {
        List<ThingEx> thingExs = new ArrayList<>();
        thingExs
        List<Map<String, Object>> docs = (List)ThingMongoDAO.getInstance().getThingUdfValues(where, null, Arrays.asList("*"), null).get("results");

        thingExs.add(  )

        children = things.isEmpty() ? new HashMap<Long, Thing>() : ThingService.getInstance().selectByParents( things );
        logger.info( "got " + things.size() + " things in " + (System.currentTimeMillis() - timeStamp) );

        timeStamp = System.currentTimeMillis();
        //todo n+1 delay here!!!
        //get values from non-sql storage
        List<Long> ids = ThingTypeFieldService.getThingTypeFieldIds(things, filterFields);
        logger.info("field ids size " + ids.size() + " with filters " + filterFields);


        return thingExs;
    }*/

	public List<ThingEx> listByFilters(final String where) {
		List<ThingEx> thingExs = new ArrayList<>();
		List<Thing> things;
		final Map<Long, Thing> children;

		//todo put parser class or something. TOTALLY HARDCODED!!!!
		List<String> noSqlValues = new ArrayList<>();

		List<String> thingWhere = new ArrayList<>();
		if (StringUtils.isNotEmpty( where ))
		{
			String[] andOrTokens = StringUtils.split( where, "&" );

			for( String andOrToken : andOrTokens )
			{
				if (!andOrToken.startsWith( "field." ))
				{
					thingWhere.add(andOrToken);
				}
				else
				{
					String[] fieldTokens = StringUtils.split( andOrToken, "|" );

					for( String field : fieldTokens )
					{
						String[] propertyValue = StringUtils.split( field, "=" );
						noSqlValues.add( propertyValue[1] );
					}
				}
			}
		}

		logger.info( "values for nonsql " +  noSqlValues);

		long timeStamp = System.currentTimeMillis();
		if (!noSqlValues.isEmpty() || true )
		{
			long timeStampThingsFields = System.currentTimeMillis();
			Map<Long, Object> thingAndFields = new HashMap<>();

            List<Map<String, Object>> docs = (List)ThingMongoDAO.getInstance().getThingUdfValues(where, null, Arrays.asList("*"), null).get("results");

			for(Map<String, Object> value: docs) {
//				thingAndFields.putAll( FieldValueDAO.getThings( value ) ); ;
				thingAndFields.put( Long.parseLong(value.get("_id").toString()), value );
			}
			logger.info( "got " + thingAndFields.size() + " things and fields in " + (System.currentTimeMillis() - timeStampThingsFields) );
			//get things that match the filtered field values
			//todo make sure they are values for the fields we want

			things = thingAndFields.keySet().isEmpty() ?
					new ArrayList<Thing>() : ThingService.getInstance().selectAllIn(StringUtils.join( thingWhere, '&'), thingAndFields.keySet() );
		}
		//do not filter. maybe by group
		else
		{
			things = ThingService.getInstance().selectWith( StringUtils.join( thingWhere, '&') );
		}

		children = things.isEmpty() ? new HashMap<Long, Thing>() : ThingService.getInstance().selectByParents( things );
		logger.info( "got " + things.size() + " things in " + (System.currentTimeMillis() - timeStamp) );

		timeStamp = System.currentTimeMillis();
		//todo n+1 delay here!!!
		//get values from non-sql storage
		List<Long> ids = ThingTypeFieldService.getThingTypeFieldIds(things, filterFields);
		logger.info("field ids size " + ids.size() + " with filters " + filterFields);


        final Map<Long, Map<Long, Map<String, Object>>> allFieldValues = new HashMap<>();

        for(Thing thing: things){
            allFieldValues.put(thing.getId(), (Map)ThingMongoDAO.getInstance().getThing(thing.getId()));
        }
		logger.info( "got " + allFieldValues.size() + " all field values in " + (System.currentTimeMillis() - timeStamp) );


		timeStamp = System.currentTimeMillis();
		//thing list
		for (final Thing thing : things)
		{
			thingExs.add(new ThingEx()
			{
				@Override
				public Map<String, Object> publicMap()
				{
					Map<String, Object> values = new LinkedHashMap<String, Object>();
					values.put("serial", thing.getSerial());
					values.put("name", thing.getName());

					//put values from non sql storage
					//todo loop only on fields.
					for (ThingTypeField thingTypeField : thing.getThingType().getThingTypeFields())
					{
						//check if the field is on the list of required fields
						if(filterFields.contains(thingTypeField.getName()))
						{
							Map<String, Object> fieldValues = allFieldValues.get(thing.getId()).get(thingTypeField.getName());
							String value = fieldValues != null ? (String)fieldValues.get("thingTypeFieldId") : "";
							logger.debug("Thing type " + thingTypeField.getDataType().getId());
							//do export shifts as names not ids
							if(thingTypeField.getDataType().getId().compareTo( ThingTypeField.Type.TYPE_SHIFT.value ) == 0 ) {
								value = getShiftNames(value);
							}

							//export boolean lower case
							if(thingTypeField.getDataType().getId().compareTo(ThingTypeField.Type.TYPE_BOOLEAN.value ) == 0) {
								value = value.toLowerCase();
							}

							values.put(thingTypeField.getName(), value);
						}
					}

					//children
					Thing child = children.get( thing.getId() );
					values.put("TagID", child != null ? child.getSerial(): "");

					return values;
				}

				@Override
				public boolean hasParent() {
					Thing parent = thing.getParent();
					return parent != null;
				}
			});
		}

		logger.info( "got things and field values in " + (System.currentTimeMillis() - timeStamp) );


		return thingExs;

	}

    private String getShiftNames(String idsString)
    {
        String[] ids = StringUtils.split(idsString, ',');
        List<String> names = new ArrayList<>();
        for (String id : ids) {
            try {
                Shift shift = ShiftService.getInstance().get(Long.parseLong(id));
                names.add(shift.getName());
            }
            catch (Exception e)
            {
                return "Undefined";
            }
        }

        return StringUtils.join(names, ",");
    }

    /**
     * get thing values from all the fields on the thing
     * @param id the thing id to get the values.
     * @return the field values on a ThingEx.
     * todo get child properties
     */
    public ThingEx get(Long id)
    {
        final Thing thing = ThingService.getInstance().get(id);

        //get thing field names for all properties
        final List<String> properties = new ArrayList<>();
        final List<ThingTypeField> thingTypeFields = new ArrayList<>(thing.getThingType().getThingTypeFields());
        for (ThingTypeField thingTypeField : thingTypeFields) {
            properties.add(thingTypeField.getName());
        }

        return new ThingEx() {
            @Override
            public Map<String, Object> publicMap() {
                return populateValues(thing, thingTypeFields, properties);
            }

            @Override
            public boolean hasParent() {
                return thing.getParent() != null;
            }
        };
    }


    /**
     * get thing values that are on the properties list
     * @param id the thing id to get the values.
     * @param properties names of properties to get the values for
     *
     * @return the field values on a ThingEx.
     * todo get child properties
     */
    public ThingEx get(Long id, List<String> properties) {
        final Thing thing = ThingService.getInstance().get(id);
        final List<String> _properties = properties;
        return new ThingEx() {
            @Override
            public Map<String, Object> publicMap() {
                return populateValues(thing, new ArrayList<ThingTypeField>(thing.getThingType().getThingTypeFields()), _properties);
            }

            @Override
            public boolean hasParent() {
                return thing.getParent() != null;
            }
        };
    }

    /**
     * helper method that gets the values from the thing fields and filters it against properties names.
     * @param thingTypeFields thing fields to get values for
     * @param properties properties names to filter
     * @return the field values on a ThingEx.
     */
    private Map<String, Object> populateValues(Thing thing, List<ThingTypeField> thingTypeFields, List<String> properties)
    {
        //filter fields to go get
        final List<ThingTypeField> filteredThingFields = new ArrayList<>();
        for (ThingTypeField thingField : thingTypeFields) {
            if (properties.contains(thingField.getName())) {
                filteredThingFields.add(thingField);
            }
        }

        return getValues(thing, filteredThingFields);

    }

    /** helper method that actually gets the values **/
    private Map<String, Object> getValues(final Thing thing, final List<ThingTypeField> filteredThingFields)
    {

        List<Long> thingIds = new ArrayList<>();
        thingIds.add(thing.getId());
        List<Long> thingTypeIds = new ArrayList<>();
        for(ThingTypeField thingField: filteredThingFields){
            if(!thingTypeIds.contains(thingField.getId())){
                thingTypeIds.add(thingField.getId());
            }
        }

        Map<String, Object> doc = (Map)ThingMongoDAO.getInstance().getThing(thing.getId());


        Map<String, Object> values = new LinkedHashMap<>();
        Map<String, Object> fieldObject;
        //put values from non sql storage
        for (ThingTypeField thingTypeField : filteredThingFields)
        {
            Map<String, Object> fieldValue = (Map)doc.get(thingTypeField.getName());
            String value = fieldValue != null ? (String)fieldValue.get("thingTypeFieldId") : "";
            logger.debug("Thing type " + thingTypeField.getDataType().getId());
            //do export shifts as names not ids
            if(thingTypeField.getDataType().getId().compareTo( ThingTypeField.Type.TYPE_SHIFT.value ) == 0) {
                value = getShiftNames(value);
            }

            //export boolean lower case
            if(thingTypeField.getDataType().getId().compareTo( ThingTypeField.Type.TYPE_BOOLEAN.value ) == 0) {
                value = value.toLowerCase();
            }
            //get from MD7
            if(thingTypeField.getDataType().getId().compareTo( ThingTypeField.Type.TYPE_IMAGE_URL.value ) == 0)
            {
                value = "";
                String ip = getIpAddress();
                if (!ip.equals(""))
                {
                    MD7ResponseHandler handler = new MD7ResponseHandler(ip, thingTypeField.getName());
                    String epcid = thing.getSerial();
                    String uri = getURI(ip, epcid);

                    RestClient restClient = RestClient.instance();
                    try {
                        restClient.get(uri, handler);
                        value = handler.getImageURI();
                    } catch (RestCallException e) {
                        e.printStackTrace();
                    }

                    // saving in Cassandra
                    //TODO removed insertion into cassandra, verify if insertion in mongo needed
//                    Calendar calendar = new GregorianCalendar();
//
//                    String imageURLValue = FieldValueService.value(thingTypeField.getId());
//                    if (imageURLValue != null)
//                    {
//                        if (!imageURLValue.equals(value)){
//                            FieldValueService.insert(thing.getId(), thingTypeField.getId(), calendar.getTime(), value, thingTypeField.getTimeSeries());
//                        }
//                    }else{
//                        FieldValueService.insert(thing.getId(), thingTypeField.getId(), calendar.getTime(), value, thingTypeField.getTimeSeries());
//                    }

                }
            }

            fieldObject = new LinkedHashMap<>();
            fieldObject.put("value", value);
            fieldObject.put("type", thingTypeField.getDataType().getId());
            values.put(thingTypeField.getName(), fieldObject);
        }
        return values;
    }

    /**
     * Getting MD7 IP address from configuration
     * @return ipAddress
     */
    public String getIpAddress()
    {
        return ConfigurationService.getAsString(GroupService.getInstance().getRootGroup(), "ipAddress");
    }

    public String getURI(String ip, String epcid){
        // http://<ip>/bio/gatequery.php?epc=epcid
        return  "http://" + ip + "/bio/gatequery.php?epc=" + epcid;
    }


}
