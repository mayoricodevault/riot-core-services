package com.tierconnect.riot.iot.popdb;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Transaction;

import com.google.common.collect.Lists;
import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.entities.Role;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.GroupTypeService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.ThingTypeMap;
//import com.tierconnect.riot.iot.services.FieldValueService;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.iot.services.ThingTypeMapService;
import com.tierconnect.riot.iot.services.ThingTypeService;

/**
 * This is a data set for QA automation testing
 *
 */
public class PopDBQa {
	private static final Logger logger = Logger.getLogger(PopDBRiot.class);
/*
	public static void main(String args[]) throws Exception 
	{
		PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
		CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));
		PopDBQa popdb = new PopDBQa();
		Transaction transaction = GroupService.getInstance().getGroupDAO().getSession().getTransaction();
		transaction.begin();
		PopDBIOTUtils.initShiroWithRoot();
		popdb.run();
		transaction.commit();
		System.exit(0);
	}
	
	public void run( )
	{
		// TODO: make selection more robust ?
		Group rootGroup = GroupService.getInstance().get(1L);
		// TODO: make selection more robust ?
		GroupType tenantGroupType = GroupTypeService.getInstance().get(2L); 
		GroupType tenantGroupType1 = GroupTypeService.getInstance().get(2L);
		// TODO: make selection more robust ?
		Role tenantAdminRole = RoleService.getInstance().get(2L);
		
		System.out.print( "rootGroup=" + rootGroup );
		System.out.print( "tenant=" + tenantGroupType );
		
		List<Group> groups = createCarvoyantDemoData();


		createDartData(groups);
	}
	
	private List<Group> createCarvoyantDemoData(){
        User rootUser = UserService.getInstance().get(1L);
		// Creating groups types
		GroupType rootGroupType = GroupTypeService.getInstance().get(1L);
		rootGroupType.setDescription("Root Group");
		GroupTypeService.getInstance().update(rootGroupType);
		GroupType tenantGroupType = GroupTypeService.getInstance().get(2L);
		GroupType tenantGroupType1 = GroupTypeService.getInstance().get(2L);
		GroupType tenantGroupType2 = GroupTypeService.getInstance().get(2L);
		tenantGroupType2.setName("prueba");
		tenantGroupType1.setName("Enterprise");
		
		Group rootGroup = GroupService.getInstance().get(1L);
		GroupType cityGroupType1 = PopDBUtils.popGroupType("Enterprise", rootGroup, rootGroupType, "Enterprise");
		GroupService.getInstance().update(rootGroup);
		Group tierConnectGroup = PopDBUtils.popGroup("TierConnect", "TierConnect", rootGroup, tenantGroupType, "TierConnect");
		Group Ford=PopDBUtils.popGroup("Ford", "Ford", rootGroup, cityGroupType1, "group Ford");
		
		// Creating roles
		Role rootRole = RoleService.getInstance().get(1L);
		rootRole.setName("Root Administrator");
		rootRole.setDescription("Root Administrator");
		RoleService.getInstance().update(rootRole);
		Role tenantRole = RoleService.getInstance().get(2L);
		tenantRole.setName("Company Administrator");
		tenantRole.setDescription("The Company Administrator");
		RoleService.getInstance().update(tenantRole);
		tenantGroupType.setName("Company");
		tenantGroupType.setDescription("Company Administrator");
		GroupTypeService.getInstance().update(tenantGroupType);
		
		GroupType cityGroupType = PopDBUtils.popGroupType("City", tierConnectGroup, tenantGroupType, "City");
		GroupType factoGroupType= PopDBUtils.popGroupType("Factory", Ford, cityGroupType1, "Factory");
		// Creation groups
		Group factoryA=PopDBUtils.popGroup("A", "A", Ford, factoGroupType, "Factory main");
		Group factoryB=PopDBUtils.popGroup("B", "B", Ford, factoGroupType, "Factory ");
		Group factoryC=PopDBUtils.popGroup("C", "C", Ford, factoGroupType, "Factory ");
		tierConnectGroup.setGroupType(tenantGroupType);
		tierConnectGroup.setName("TierConnect");
		GroupService.getInstance().update(tierConnectGroup);
		
		Group boston = PopDBUtils.popGroup("Boston", "Boston", tierConnectGroup, cityGroupType, "Boston");
		Group philadelphia = PopDBUtils.popGroup("Philadelphia", "Philadelphia", tierConnectGroup, cityGroupType,"Philadelphia");
		Group lapaz = PopDBUtils.popGroup("La Paz", "La Paz", tierConnectGroup, cityGroupType, "La Paz");
		
		HashSet<Resource> resources = new HashSet<Resource>();

		Role userRole = PopDBUtils.popRole("User", "User", "User", resources, rootGroup, rootGroupType);
		//resources.add(ResourceService.getInstance().insert(Resource.getGroupConfigurationResource(Ford, "apc")));
		//resources.addAll(tenantRole.getRoleResources());
		Role enterprise= PopDBUtils.popRole("Enterprise administrator", "Enterprise administrator", "Enterprise administrator", null, Ford, cityGroupType1);
		List<Resource> resources1 = ResourceService.getInstance().list();
		
		//List<Resource> resources = ResourceService.getInstance().list();
		for( Resource resource : resources1)
		{//System.out.println(resource.getName().toString());

				RoleResourceService.getInstance().insert( enterprise, resource, "riuda" );
				

		}
		
		
		
		// Creating ThingType
		ThingType car = new ThingType();
		car.setName("Car");
		car.setGroup(tierConnectGroup);
		ThingType machine = new ThingType();
		machine.setName("Machine");
		machine.setGroup(Ford);
		ThingType carvoyantDongle = new ThingType();
		carvoyantDongle.setName("Carvoyant Dongle");
		carvoyantDongle.setGroup(tierConnectGroup);
		ThingType controller = new ThingType();
		controller.setName("controller");
		controller.setGroup(Ford);
		ThingTypeService.getInstance().insert(car);
		ThingTypeService.getInstance().insert(carvoyantDongle);
		ThingTypeService.getInstance().insert(machine);
		ThingTypeService.getInstance().insert(controller);
		//controller.setParent(machine);
		ThingTypeMap a=new ThingTypeMap();
		a.setChild(controller);
		a.setParent(machine);
		
		ThingTypeMapService.getInstance().insert(a);
		
		// Creating elements for car
		Set<ThingTypeField> thingFields = new HashSet<>();
		thingFields.add(new ThingTypeField("Vin", "String", "String",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_TEXT.value));
		thingFields.add(new ThingTypeField("Model", "String", "String",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_TEXT.value));
		thingFields.add(new ThingTypeField("Name", "String", "String",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_TEXT.value));
		
		for(ThingTypeField field : thingFields){
			field.setThingType(car);
			ThingTypeFieldService.getInstance().insert(field);
		}
		car.setThingTypeFields(thingFields);
		ThingTypeService.getInstance().update(car);
		
		// Creating thing fields for machine
		thingFields= new HashSet<>();
		thingFields.add(new ThingTypeField("Name", "String", "String",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_TEXT.value));
		thingFields.add(new ThingTypeField("Model", "String", "String",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_TEXT.value));
		for(ThingTypeField field : thingFields){
			field.setThingType(machine);
			ThingTypeFieldService.getInstance().insert(field);
		}
		machine.setThingTypeFields(thingFields);
		ThingTypeService.getInstance().update(machine);
		
		// Creating thing fields for carvoyant dongle
		thingFields = new HashSet<>();
		thingFields.add(new ThingTypeField("Position", "lat/lon", "degress",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_LONLATALT.value));
		thingFields.add(new ThingTypeField("Speed", "mph", "mph",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_NUMBER.value));
		thingFields.add(new ThingTypeField("FuelLevel","gph","gph",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_NUMBER.value ));
		thingFields.add(new ThingTypeField("Odometer","mile","mile",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_NUMBER.value));
		thingFields.add(new ThingTypeField("Heading","degrees","deg",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_NUMBER.value));
		thingFields.add(new ThingTypeField("Diagnostic", "String", "String",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_TEXT.value));
		thingFields.add(new ThingTypeField("Voltage", "BattVoltage", "V",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_NUMBER.value));
		thingFields.add(new ThingTypeField("TripMileage","mile","mi",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_NUMBER.value));
		thingFields.add(new ThingTypeField("RPM", "rpm","rpm",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_NUMBER.value));
		thingFields.add(new ThingTypeField("FuelRate","gal","g",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_NUMBER.value ));
		thingFields.add(new ThingTypeField("EngineTemp", "Temperature","F",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_NUMBER.value));
		thingFields.add(new ThingTypeField("Device Id", "Integer","Integer",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_NUMBER.value));
		
		for(ThingTypeField field : thingFields){
			field.setThingType(carvoyantDongle);
			ThingTypeFieldService.getInstance().insert(field);
		}
		
		carvoyantDongle.setThingTypeFields(thingFields);
		//carvoyantDongle.setParent(car);
		ThingTypeService.getInstance().update(carvoyantDongle);
		ThingTypeMap a1=new ThingTypeMap();
		a1.setChild(carvoyantDongle);
		a1.setParent(car);
		
		ThingTypeMapService.getInstance().insert(a);
		// Creation things for TierConnect
		Thing lesCar = ThingTypeService.getInstance().instantiate(car, "1C4BJWDG9CL233703", new Date(), rootUser);
		lesCar.setName("Les - 2012 Jeep Wrangler");
		Thing scottCar = ThingTypeService.getInstance().instantiate(car, "WBANV93518CZ63905", new Date(), rootUser);
		scottCar.setName("Scott - 2008 BMW 5 Series 535xi");
		Thing lesCarDongle = ThingTypeService.getInstance().instantiate(carvoyantDongle, "C201401013", new Date(), rootUser);
		Thing scottCarDongle = ThingTypeService.getInstance().instantiate(carvoyantDongle, "C201401008", new Date(), rootUser);

		// Things for Ford
		Thing paintGun=ThingTypeService.getInstance().instantiate(machine, "A1B22B250", new Date(), rootUser);
		paintGun.setName("Paint Gun");
		Thing carLift=ThingTypeService.getInstance().instantiate(machine, "A2B33B250", new Date(), rootUser);
		carLift.setName("Car lift");
		Thing abb=ThingTypeService.getInstance().instantiate(controller, "A2B33B250456", new Date(), rootUser);
		Thing siem=ThingTypeService.getInstance().instantiate(controller, "A2B33B250789", new Date(), rootUser);
		abb.setName("ABB250");
		siem.setName("SIEM670");
		abb.setParent(paintGun);
		siem.setParent(carLift);
		lesCarDongle.setParent(lesCar);
		scottCarDongle.setParent(scottCar);
		lesCarDongle.setGroup(boston);
		scottCarDongle.setGroup(philadelphia);
		lesCar.setGroup(boston);
		scottCar.setGroup(philadelphia);
		ThingService.getInstance().update(lesCarDongle);
		ThingService.getInstance().update(scottCarDongle);
		ThingService.getInstance().update(scottCar);
		
		// Creating user
		rootUser.setFirstName("Root Administrator");
		rootUser.setLastName("Root Administrator");
		rootUser.setEmail("root@company.com");
		User adminFord = PopDBUtils.popUser("adminf", Ford, enterprise);
		adminFord.setFirstName("Ford Administrator");
		adminFord.setLastName("Ford Administrator");
		adminFord.setEmail("admin@tierconnect.com");
		User adminUser = PopDBUtils.popUser("adminc", tierConnectGroup, tenantRole);
		adminUser.setFirstName("TierConnect Administrator");
		adminUser.setLastName("TierConnect Administrator");
		adminUser.setEmail("admin@tierconnect.com");
		User les = PopDBUtils.popUser("les", boston, userRole);
		les.setFirstName("Les");
		les.setLastName("Yetton");
		les.setEmail("les.yetton@tierconnect.com");
		User scott = PopDBUtils.popUser("scott", philadelphia, userRole);
		scott.setFirstName("Scott");
		scott.setLastName("Chalfant");
		scott.setEmail("scott.chalfant@tierconnect.com");
		User oscar = PopDBUtils.popUser("oscar", lapaz, userRole);
		oscar.setFirstName("Oscar");
		oscar.setLastName("Luna");
		oscar.setEmail("oscar.luna@tierconnect.com");
		User userA = PopDBUtils.popUser("userA", factoryA, userRole);
		oscar.setFirstName("Oscar");
		oscar.setLastName("Anul");
		oscar.setEmail("oscar.anul@ford.com");
		UserService.getInstance().update(rootUser);
		UserService.getInstance().update(adminUser);
		UserService.getInstance().update(les);
		UserService.getInstance().update(scott);
		UserService.getInstance().update(oscar);
		return Lists.newArrayList(boston, philadelphia, lapaz);
	}
	
	    private void createDartData(List<Group> groups) {
            User rootUser = UserService.getInstance().get(1L);
    	Group tierconnectGroup = GroupService.getInstance().get(2L);
        Group lapaz = null;
        for (Group group : groups) {
            if (group.getName().equals("La Paz")) {
                lapaz = group;
                break;
            }
        }
    	
    	ThingType employee = new ThingType();
    	employee.setName("Employee");
    	employee.setGroup(tierconnectGroup);
    	ThingTypeService.getInstance().insert(employee);
    	
        ThingType dartType = new ThingType();
        dartType.setName("RFID Tag");
        dartType.setGroup(tierconnectGroup);
        //dartType.setParent(employee);
        ThingTypeService.getInstance().insert(dartType);
        ThingTypeMap a=new ThingTypeMap();
		a.setChild(dartType);
		a.setParent(employee);
		
		ThingTypeMapService.getInstance().insert(a);
        Set<ThingTypeField> thingTypeFields = new HashSet<>();
        thingTypeFields.add(new ThingTypeField("Position", "lat/lon", "degress",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_LONLATALT.value));
        thingTypeFields.add(new ThingTypeField("Id", "String", "String",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_TEXT.value));
        thingTypeFields.add(new ThingTypeField("Name", "String", "String",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_TEXT.value));
        thingTypeFields.add(new ThingTypeField("Metadata", "String", "String",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_TEXT.value));
        thingTypeFields.add(new ThingTypeField("type", "String", "String",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, ThingTypeField.Type.TYPE_TEXT.value));

        for (ThingTypeField field : thingTypeFields) {
            field.setThingType(dartType);
            ThingTypeFieldService.getInstance().insert(field);
        }

        dartType.setThingTypeFields(thingTypeFields);
        ThingTypeService.getInstance().update(dartType);
        
        String names[] = new String[] {
                "oscar.luna",
                "pablo.caballero",
                "beatriz.mendoza",
               
        };
        String dartSerials[] = new String[] {
                "00210BC7",
                "00210C1F",
                "12345678"
        };
        
        for (int i = 0; i < names.length; i++) {
            Thing emp = ThingTypeService.getInstance().instantiate(employee, names[i], new Date(), rootUser);
            emp.setName(names[i]);
            Thing thing = ThingTypeService.getInstance().instantiate(dartType, dartSerials[i], new Date(), rootUser);
            thing.setName(names[i]);
            thing.setParent(emp);
            Set<ThingTypeField> fields = thing.getThingType().getThingTypeFields();
            for (ThingTypeField field : fields) {
                if (field.getName().equals("type")) {
                    FieldValueService.insert(thing.getId(), field.getId(),
							new Date(), "rfid", field.getTimeSeries() );
                }
            }
            thing.setGroup(lapaz);
            emp.setGroup(lapaz);
            ThingService.getInstance().update(thing);
            ThingService.getInstance().update(emp);
        }
    }
*/
}
