package com.tierconnect.riot.iot.popdb;

import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.iot.services.ThingsService;
import org.apache.log4j.Logger;

import java.util.*;

/**
 * Created by rchirinos on 5/9/2015.
 */
public class PopulateThings
{
	private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger( PopulateThings.class );

	public static void populateThings()
	{

		try{
			ThingType rfid                   = ThingTypeService.getInstance().get(1L);
			ThingType gps                    = ThingTypeService.getInstance().get(2L);
			ThingType pantThingType          = ThingTypeService.getInstance().getByCode("pants_code");
			ThingType jacketThingType        = ThingTypeService.getInstance().getByCode("jackets_code");
			ThingType shippingOrderThingType = ThingTypeService.getInstance().getByCode("shippingorder_code");
			ThingType asset                  = ThingTypeService.getInstance().getByCode("asset_code");
			ThingType tag                    = ThingTypeService.getInstance().getByCode("tag_code");

			Group santaMonica = GroupService.getInstance().get(3L);
			User  rootUser    = UserService.getInstance().getRootUser();

			int startIniSerial = 472;
			for (int i = 1; i <= 5; i++)
			{
				startIniSerial++;
				instantiateClothingItem(jacketThingType,
										rfid,
										startIniSerial,
										i,
										"J0000" + i,
										"Jacket" + i,
										santaMonica,
										rootUser);
			}
			for (int i = 1; i <= 5; i++)
			{
				startIniSerial++;
				instantiateClothingItem(pantThingType,
										rfid,
										startIniSerial,
										i,
										"P0000" + i,
										"Pants" + i,
										santaMonica,
										rootUser);
			}

            for (int i = 1; i <= 5; i++)
            {
                startIniSerial++;
                instantiateShippingOrderItem(shippingOrderThingType,
                                             asset,
                                             tag,
                                             startIniSerial,
                                             i,
                                             "SO0000" + i,
                                             "ShippingOrder" + i,
                                             santaMonica);

            }
        } catch (Exception e)
		{
			e.printStackTrace();

		}

	}

	public static void instantiateShippingOrderItem(ThingType shippingOrderThingType,
													ThingType assetThingType,
													ThingType tag,
													int startSerial,
													int i,
													String serial,
													String name,
													Group group) throws Exception
	{
		Logger logger = Logger.getLogger(PopulateThings.class);
		/*Set parent*/
		/*Set Udfs*/
		Date   storageDate       = new Date();
		String serialNumChild    = String.format("%021d", startSerial);
		String serialNumChildTwo = String.format("%021d", i);

		//Create a new child Tag
		ThingsService.getInstance().create(new Stack<Long>(),
                                          tag.getThingTypeCode(),
										  group.getHierarchyName(false),
										  serialNumChildTwo,
										  serialNumChildTwo,
										  null,
										  getTagMap(storageDate.getTime(), tag),
										  null,
										  null,
										  false,
										  false,
										  storageDate,
										  true, true);

		logger.info("BEGIN POPULATE SHIPPING ORDERS");
		//Create a new Thing Shipping Order
		Map<String, Object> shippingOrder = ThingsService.getInstance().create(new Stack<Long>(),
                                                                              shippingOrderThingType.getThingTypeCode(),
																			  group.getHierarchyName(false),
																			  name,
																			  serial,
																			  null,
																			  getShippingOrderItemUdf(i,
																									  storageDate.getTime(),
																									  shippingOrderThingType),
																			  null,
																			  null,
																			  false,
																			  false,
																			  storageDate,
																			  true, true);

		//Create a new child Asset
		ThingsService.getInstance().create(new Stack<Long>(),
                                          assetThingType.getThingTypeCode(),
										  group.getHierarchyName(false),
										  serialNumChild,
										  serialNumChild,
										  null,
										  getAssetMap(storageDate.getTime(), assetThingType, shippingOrder),
										  getChildMap(serialNumChildTwo, tag.getThingTypeCode()),
										  null,
										  false,
										  false,
										  storageDate,
										  true, true);
	}

	/*
	* Filled clothing items
	* */
	public static void instantiateClothingItem(ThingType jacketThingType,
											   ThingType rfidtag,
											   int startSerial,
											   int i,
											   String serial,
											   String name,
											   Group group,
											   User createdBy) throws Exception
	{
		/*Set parent*/
		/*Set Udfs*/
		Date storageDate = new Date();
		//int serialChild = startSerial + i;
		String snChild = String.format("%021d", startSerial);

		//Create the child RFID
		Map<String, Object> result = ThingsService.getInstance().create(new Stack<Long>(),
                                                                       rfidtag.getThingTypeCode(),
																	   group.getHierarchyName(false),
																	   snChild,
																	   snChild,
																	   null,
																	   getRFIDItemMap(i,
																					  storageDate.getTime(),
																					  rfidtag),
																	   null,
																	   null,
																	   false,
																	   false,
																	   storageDate,
																	   true, true);

		//Create a new Thing
		result = ThingsService.getInstance().create(new Stack<Long>(),
                                                   jacketThingType.getThingTypeCode(),
												   group.getHierarchyName(false),
												   name,
												   serial,
												   null,
												   getClothingItemUdf(i, storageDate.getTime(), jacketThingType),
												   getChildMap(snChild, rfidtag.getThingTypeCode()),
												   null,
												   false,
												   false,
												   storageDate,
												   true, true);
	}

	/**
	 * Get the map for udf of clothing items
	 */
	public static Map<String, Object> getClothingItemUdf(int i, Long storageTime, ThingType jacketThingType)
	{
		Map<String, Object> udf = new HashMap<String, Object>();
		Map<String, Object> fieldMap = new HashMap<String, Object>();
		for( ThingTypeField field : jacketThingType.getThingTypeFields() )
		{
			fieldMap = new HashMap<String, Object>();
			switch( field.getName().toString() )
			{
				case ("size"):
				{
					if( i % 2 == 0 )
					{
						fieldMap.put( "thingTypeFieldId", field.getId() );
						fieldMap.put( "value", "Large" );
						fieldMap.put( "time", storageTime );
					}
					else
					{
						fieldMap.put( "thingTypeFieldId", field.getId() );
						fieldMap.put( "value", "X-Large" );
						fieldMap.put( "time", storageTime );
					}
					udf.put( field.getName().toString(),fieldMap );
					break;
				}

				case ("color"):
					if( i % 2 == 0 )
					{
						fieldMap.put( "thingTypeFieldId", field.getId() );
						fieldMap.put( "value", "Black" );
						fieldMap.put( "time", storageTime );
					}
					else
					{
						fieldMap.put( "thingTypeFieldId", field.getId() );
						fieldMap.put( "value", "Gray" );
						fieldMap.put( "time", storageTime );
					}
					udf.put( field.getName().toString(),fieldMap );
					break;




			}
		}
		return udf;
	}

	/**
	 * Get the map for udf of clothing items
	 */
	public static Map<String, Object> getShippingOrderItemUdf(int i, Long storageTime, ThingType shippingOrderThingType)
	{
		Map<String, Object> udf = new HashMap<>();
		for (ThingTypeField field : shippingOrderThingType.getThingTypeFields())
		{
			Map<String, Object> fieldMap = new HashMap<>();
			switch (field.getName().toString())
			{
				case ("owner"):
					fieldMap.put("thingTypeFieldId", field.getId());
					fieldMap.put("value", "owner test" + i);
					fieldMap.put("time", storageTime);
					udf.put(field.getName().toString(), fieldMap);
					break;
				case ("status"):
				{
					fieldMap.put("thingTypeFieldId", field.getId());
					if (i % 2 == 0)
					{
						fieldMap.put("value", "open");
					}
					else
					{
						fieldMap.put("value", "closed");
					}

					fieldMap.put("time", storageTime);
					udf.put(field.getName().toString(), fieldMap);
					break;
				}
			}
		} return udf;
	}

	/**
	 * Get the map for udf of rfid items
	 */
	public static Map<String, Object> getRFIDItemMap(int i, Long storageTime, ThingType rfidtag)
	{
		Map<String, Object> udf = new HashMap<String, Object>();
		Map<String, Object> fieldMap = new HashMap<String, Object>();
		/*Set Udfs*/
		for( ThingTypeField field : rfidtag.getThingTypeFields() )
		{
			fieldMap = new HashMap<String, Object>();
			switch( field.getName().toString() )
			{
				/*case ("zone"):
				{
					switch( i % 4 )
					{
						case (0):
						{
							fieldMap.put( "thingTypeFieldId", field.getId() );
							fieldMap.put( "value", "Stockroom" );
							fieldMap.put( "time",storageTime );
							udf.put( field.getName().toString(),fieldMap );
						}
						break;
						case (1):
						{
							fieldMap.put( "thingTypeFieldId", field.getId() );
							fieldMap.put( "value", "Salesfloor" );
							fieldMap.put( "time",storageTime );
							udf.put( field.getName().toString(),fieldMap );
						}
						break;
						case (2):
						{
						    fieldMap.put( "thingTypeFieldId", field.getId() );
							fieldMap.put( "value", "Entrance" );
							fieldMap.put( "time", storageTime );
							udf.put( field.getName().toString(),fieldMap );
						}
						break;
						case (3):
						{
						    fieldMap.put( "thingTypeFieldId", field.getId() );
							fieldMap.put( "value", "POS" );
							fieldMap.put( "time",storageTime );
							udf.put( field.getName().toString(),fieldMap );
						}
						break;
					}
				}
				;
				break;*/
				/*case ("lastDetectTime"):
				    fieldMap.put( "thingTypeFieldId", field.getId() );
					fieldMap.put( "value", null );
					fieldMap.put( "time", storageTime );
					udf.put( field.getName().toString(),fieldMap );
					break;
				case ("lastLocateTime"):
				    fieldMap.put( "thingTypeFieldId", field.getId() );
					fieldMap.put( "value", null );
					fieldMap.put( "time",storageTime );
					udf.put( field.getName().toString(),fieldMap );
					break;*/
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
	 * Get the map for udf of asset items
	 */
	public static Map<String, Object> getAssetMap(Long storageTime,
												  ThingType assetThingType,
												  Map<String, Object>  shippingOrder )
	{
		Map<String, Object> udf = new HashMap<>();

		/*Set Udfs*/
		for (ThingTypeField field : assetThingType.getThingTypeFields())
		{
			Map<String, Object> fieldMap = new HashMap<>();
			switch (field.getName().toString())
			{
				case ("status"):
					fieldMap.put("thingTypeFieldId", field.getId());
					fieldMap.put("value", false);
					fieldMap.put("time", storageTime);
					break;
				case ("shippingOrderField"):
					fieldMap.put("thingTypeFieldId", field.getId());
					Map<String, Object> thing = (Map<String, Object>) shippingOrder.get("thing");
					fieldMap.put("value",thing.get("serial"));
					fieldMap.put("time", storageTime);
					break;
			}
			udf.put(field.getName().toString(), fieldMap);
		}
		System.out.println("RESULT : "+ udf);
		return udf;
	}

	/**
	 * Get the map for udf of rfid items
	 */
	public static Map<String, Object> getTagMap(Long storageTime, ThingType tag)
	{
		Map<String, Object> udf = new HashMap<>();

		/*Set Udfs*/
		for (ThingTypeField field : tag.getThingTypeFields())
		{
			Map<String, Object> fieldMap = new HashMap<>();
			switch (field.getName().toString())
			{
				case ("active"):
					fieldMap.put("thingTypeFieldId", field.getId());
					fieldMap.put("value", false);
					fieldMap.put("time", storageTime);
					udf.put(field.getName().toString(), fieldMap);
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
}
