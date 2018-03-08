package com.tierconnect.riot.iot.popdb;

import java.lang.reflect.Array;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
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
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.appcore.services.UserService;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.entities.ReportDefinition;
import com.tierconnect.riot.iot.entities.ReportFilter;
import com.tierconnect.riot.iot.entities.ReportGroupBy;
import com.tierconnect.riot.iot.entities.ReportProperty;
import com.tierconnect.riot.iot.entities.Thing;
import com.tierconnect.riot.iot.entities.ThingType;
import com.tierconnect.riot.iot.entities.ThingTypeField;
import com.tierconnect.riot.iot.entities.ThingTypeMap;
import com.tierconnect.riot.iot.entities.Zone;
import com.tierconnect.riot.iot.entities.ZonePoint;
//import com.tierconnect.riot.iot.services.FieldValueService;
import com.tierconnect.riot.iot.services.ReportDefinitionService;
import com.tierconnect.riot.iot.services.ReportFilterService;
import com.tierconnect.riot.iot.services.ReportGroupByService;
import com.tierconnect.riot.iot.services.ReportPropertyService;
import com.tierconnect.riot.iot.services.ThingService;
import com.tierconnect.riot.iot.services.ThingTypeFieldService;
import com.tierconnect.riot.iot.services.ThingTypeMapService;
import com.tierconnect.riot.iot.services.ThingTypeService;
import com.tierconnect.riot.iot.services.ZonePointService;
import com.tierconnect.riot.iot.services.ZoneService;

/**
 * This is an in-house demo data set using Carvoyant and Dart RFID tag data
 * 
 */
public class PopDBRiot {
	private static final Logger logger = Logger.getLogger(PopDBRiot.class);
	/*
	public static void main(String args[]) throws Exception 
	{
		PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
		CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));

		PopDBRiot popdb = new PopDBRiot();		
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
		// TODO: make selection more robust ?
		Role tenantAdminRole = RoleService.getInstance().get(2L);
		
		System.out.print( "rootGroup=" + rootGroup );
		System.out.print( "tenant=" + tenantGroupType );
		
//		Group riotGroup = PopDBUtils.popGroup( "Riot", "Riot", rootGroup, tenantGroupType, "Riot Group" );
//		Group carvoyantGroup = PopDBUtils.popGroup( "Carvoyant", "Carvoyant", riotGroup, tenantGroupType, "Carvoyant Group" );
		List<Group> groups = createCarvoyantDemoData();
//		User riotUser = PopDBUtils.popUser( "riot", riotGroup, tenantAdminRole );
//		User carvoyantUser = PopDBUtils.popUser( "carvoyant", carvoyantGroup, tenantAdminRole );
		
		
//		createCokeData(riotGroup);
//		createFleetData(riotGroup,carvoyantGroup);
		//createPerformanceData(riotGroup);

		createDartData(groups);
		createZonesData(rootGroup);
		createReportDefinitionData(rootGroup);
	}
	
	private List<Group> createCarvoyantDemoData(){
        User rootUser = UserService.getInstance().get(1L);
		GroupType rootGroupType = GroupTypeService.getInstance().get(1L);
		rootGroupType.setDescription("Root Group");
		GroupTypeService.getInstance().update(rootGroupType);
		GroupType tenantGroupType = GroupTypeService.getInstance().get(2L);
		
		Group rootGroup = GroupService.getInstance().get(1L);
		GroupService.getInstance().update(rootGroup);
		Group tierConnectGroup = PopDBUtils.popGroup("TierConnect", "TierConnect", rootGroup, tenantGroupType, "TierConnect");
		
		
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
		
		tierConnectGroup.setGroupType(tenantGroupType);
		tierConnectGroup.setName("TierConnect");
		GroupService.getInstance().update(tierConnectGroup);
		
		Group boston = PopDBUtils.popGroup("Boston", "Boston", tierConnectGroup, cityGroupType, "Boston");
		Group philadelphia = PopDBUtils.popGroup("Philadelphia", "Philadelphia", tierConnectGroup, cityGroupType,"Philadelphia");
		Group lapaz = PopDBUtils.popGroup("La Paz", "La Paz", tierConnectGroup, cityGroupType, "La Paz");
		
		HashSet<Resource> resources = new HashSet<Resource>();
		Role userRole = PopDBUtils.popRole("User", "User", "User", resources, rootGroup, rootGroupType);
		
		
		ThingType car = new ThingType();
		car.setName("Car");
		car.setGroup(tierConnectGroup);
		ThingType carvoyantDongle = new ThingType();
		carvoyantDongle.setName("Carvoyant Dongle");
		carvoyantDongle.setGroup(tierConnectGroup);
		ThingTypeService.getInstance().insert(car);
		ThingTypeService.getInstance().insert(carvoyantDongle);
		
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
		
		ThingTypeService.getInstance().update(carvoyantDongle);
		
		Thing lesCar = ThingTypeService.getInstance().instantiate(car, "1C4BJWDG9CL233703", new Date(), rootUser);
		lesCar.setName("Les - 2012 Jeep Wrangler");
		Thing scottCar = ThingTypeService.getInstance().instantiate(car, "WBANV93518CZ63905", new Date(), rootUser);
		scottCar.setName("Scott - 2008 BMW 5 Series 535xi");
		Thing lesCarDongle = ThingTypeService.getInstance().instantiate(carvoyantDongle, "C201401013", new Date(), rootUser);
		Thing scottCarDongle = ThingTypeService.getInstance().instantiate(carvoyantDongle, "C201401008", new Date(), rootUser);
		
		lesCarDongle.setParent(lesCar);
		scottCarDongle.setParent(scottCar);
		lesCarDongle.setGroup(boston);
		scottCarDongle.setGroup(philadelphia);
		lesCar.setGroup(boston);
		scottCar.setGroup(philadelphia);
		ThingService.getInstance().update(lesCarDongle);
		ThingService.getInstance().update(scottCarDongle);
		ThingService.getInstance().update(scottCar);
		ThingService.getInstance().update(scottCar);
		
		rootUser.setFirstName("Root Administrator");
		rootUser.setLastName("Root Administrator");
		rootUser.setEmail("root@company.com");
		User adminUser = PopDBUtils.popUser("adminc", "coderoad", tierConnectGroup, tenantRole);
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
		UserService.getInstance().update(rootUser);
		UserService.getInstance().update(adminUser);
		UserService.getInstance().update(les);
		UserService.getInstance().update(scott);
		UserService.getInstance().update(oscar);
		return Lists.newArrayList(boston, philadelphia, lapaz);
	}
	
	private void createPerformanceData(Group riotGroup) {
		for(int i=1;i<=100;i++){
			Thing thing = new Thing();
			thing.setName("perf_"+i);
			thing.setSerial(thing.getName());
			thing.setGroup(riotGroup);
			
			ThingService.insert(thing, new Date());
			logger.info(i);
		}
	}

	

//	private void createCokeData(Group riotGroup) {
//		ThingType cokeMachineType = new ThingType();
//		cokeMachineType.setName("Coke Machine");
//		cokeMachineType.setGroup(riotGroup);
//		ThingTypeService.getInstance().insert(cokeMachineType);
//		
//		List<ThingField> thingFields = new LinkedList<>();
//		ThingField motion1 = new ThingField("Motion1", "boolean", "1,0", CassandraField.TYPE_TEXT, new Date());
//		thingFields.add(motion1);
//		thingFields.add(new ThingField("Distance1", "feet", "Ft", CassandraField.TYPE_TEXT, null));
//		thingFields.add(new ThingField("Temperature", "Farenheit", "F", CassandraField.TYPE_TEXT, null));
//		thingFields.add(new ThingField("Position", "lat/lon", "degress", CassandraField.TYPE_COORDINATE, null));
//		thingFields.add(new ThingField("Message", "String", "sn", CassandraField.TYPE_TEXT, null));
//		thingFields.add(new ThingField("EchoCount", "int", "times", CassandraField.TYPE_TEXT, null));
//
//		Thing thing = ThingTypeService.getInstance().instantiate(cokeMachineType, "vm_101", new Date());
//		thing.setName("Coca Cola Vending Machine");
//		ThingService.getInstance().update(thing, thingFields, new Date());
//
//		Rule rule = new Rule();		
//		rule.setName("NodeRed Filter");
//		rule.setThingId(thing.getId());
//		rule.setFieldId(motion1.getId());
//		rule.setRule("return (oldValue == '0' and newValue == '1');");
//		CassandraRuleDAO.addRule(rule);
//		Action action = new Action();
//		action.setAction("");
//		action.setName("NodeRed Filter");
//		action.setType(Action.ACTION_TYPE_MQTT_ECHO);
//		CassandraActionDAO.addAction(rule, action);
//		
//		Rule ruleFieldAction = new Rule();
//		ruleFieldAction.setName("NodeRed Echo Counter");
//		ruleFieldAction.setThingId(thing.getId());
//		ruleFieldAction.setFieldId(motion1.getId());
//		ruleFieldAction.setRule("return (oldValue == '0' and newValue == '1');");
//		CassandraRuleDAO.addRule(ruleFieldAction);
//		Action ruleFieldActionAction = new Action();
//		ruleFieldActionAction.setAction("if thing.EchoCount == nil or thing.EchoCount == '' then thing.EchoCount = 0 end; thing.EchoCount = thing.EchoCount + 1;");
//		ruleFieldActionAction.setName("NodeRed Echo Counter");
//		ruleFieldActionAction.setType(Action.ACTION_TYPE_FIELD_VALUE);
//		CassandraActionDAO.addAction(ruleFieldAction, ruleFieldActionAction);
//
//		//
//		Thing thing2 = ThingTypeService.getInstance().instantiate(cokeMachineType, "vm_101", new Date());
//		thing2.setName("4th floor");
//		ThingService.getInstance().update(thing2, null, new Date());
//		
//		Thing thing3 = ThingTypeService.getInstance().instantiate(cokeMachineType, "vm_102", new Date());
//		thing3.setName("6th floor");
//		ThingService.getInstance().update(thing3, null, new Date());
//		
//		Thing motion4 = new Thing();
//		motion4.setName("Motion Sensor MS0001");
//		motion4.setSerial("Motion Sensor MS0001");
//		motion4.setParent(thing2);
//		motion4.setGroup(riotGroup);
//		
//		Thing display4 = new Thing();
//		display4.setName("Display D0001");
//		display4.setSerial("Display D0001");
//		display4.setParent(thing2);
//		display4.setGroup(riotGroup);
//		
//		Thing motion6 = new Thing();
//		motion6.setName("Motion Sensor MS0002");
//		motion6.setSerial("Motion Sensor MS0002");
//		motion6.setParent(thing3);
//		motion6.setGroup(riotGroup);
//		
//		Thing display6 = new Thing();
//		display6.setName("Display D0002");
//		display6.setSerial("Display D0002");
//		display6.setParent(thing3);
//		display6.setGroup(riotGroup);
//		
//		ThingService.getInstance().insert(motion4, null, new Date());
//		ThingService.getInstance().insert(display4, null, new Date());
//		ThingService.getInstance().insert(motion6, null, new Date());
//		ThingService.getInstance().insert(display6, null, new Date());
//	}

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
                "gabriel.rivera",
                "pablo.caballero",
                "beatriz.mendoza",
                "kevin.bauer",
                "elmer.zapata",
                "Robert"
        };
        String dartSerials[] = new String[] {
                "00210BC7",
                "00210C1F",
                "00210B9E",
                "00210B25",
                "00210B87",
                "0021D708",
                "12345678"
        };
        
        for (int i = 0; i < names.length; i++) {
            Thing emp = ThingTypeService.getInstance().instantiate(employee, names[i], new Date(), rootUser);
            emp.setName(names[i]);
            Thing thing = ThingTypeService.getInstance().instantiate(dartType, dartSerials[i], new Date(), rootUser);
            thing.setName(dartSerials[i]);
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

    private void createZonesData(Group group) {
        Zone zone = new Zone();
        zone.setName("office zone");
        zone.setDescription("office zone");
        zone.setColor("blue");
        zone.setGroup(group);
        zone = ZoneService.getInstance().insert(zone);
        Double x1 = -68.088535;
        Double y1 = -16.541216;
        Double x2 = -68.088263;
        Double y2 = -16.540950;
        ZonePoint zonePoint1 = new ZonePoint();
        zonePoint1.setX(x1);
        zonePoint1.setY(y1);
        zonePoint1.setArrayIndex(0L);
        zonePoint1.setZone(zone);
        ZonePoint zonePoint2 = new ZonePoint();
        zonePoint2.setX(x2);
        zonePoint2.setY(y1);
        zonePoint2.setArrayIndex(1L);
        zonePoint2.setZone(zone);
        ZonePoint zonePoint3 = new ZonePoint();
        zonePoint3.setX(x2);
        zonePoint3.setY(y2);
        zonePoint3.setArrayIndex(2L);
        zonePoint3.setZone(zone);
        ZonePoint zonePoint4 = new ZonePoint();
        zonePoint4.setX(x1);
        zonePoint4.setY(y2);
        zonePoint4.setArrayIndex(3L);
        zonePoint4.setZone(zone);
        ZonePointService.getInstance().insert(zonePoint1);
        ZonePointService.getInstance().insert(zonePoint2);
        ZonePointService.getInstance().insert(zonePoint3);
        ZonePointService.getInstance().insert(zonePoint4);
        Zone zone2 = new Zone();
        zone2.setName("office zone");
        zone2.setDescription("office zone");
        zone2.setGroup(group);
        zone2 = ZoneService.getInstance().insert(zone2);
        Double xx1 = -119.1775803332518;
        Double yy1 = 44.92528837222359;
        Double xx2 = -117.9673087269966;
        Double yy2 = 28.51023854035695;
        Double xx3 = -91.00126527953009;
        Double yy3 = 26.71475856138787;
        Double xx4 = -81.96920624562615;
        Double yy4 = 42.84286545082156;
        ZonePoint zonePoint5 = new ZonePoint();
        zonePoint5.setX(xx1);
        zonePoint5.setY(yy1);
        zonePoint5.setArrayIndex(0L);
        zonePoint5.setZone(zone2);
        ZonePoint zonePoint6 = new ZonePoint();
        zonePoint6.setX(xx2);
        zonePoint6.setY(yy2);
        zonePoint6.setArrayIndex(1L);
        zonePoint6.setZone(zone2);
        ZonePoint zonePoint7 = new ZonePoint();
        zonePoint7.setX(xx3);
        zonePoint7.setY(yy3);
        zonePoint7.setArrayIndex(2L);
        zonePoint7.setZone(zone2);
        ZonePoint zonePoint8 = new ZonePoint();
        zonePoint8.setX(xx4);
        zonePoint8.setY(yy4);
        zonePoint8.setArrayIndex(3L);
        zonePoint8.setZone(zone2);
        ZonePointService.getInstance().insert(zonePoint5);
        ZonePointService.getInstance().insert(zonePoint6);
        ZonePointService.getInstance().insert(zonePoint7);
        ZonePointService.getInstance().insert(zonePoint8);
    }

    private ReportFilter createReportFilter(String label, String propertyName, String propertyOrder, String operatorFilter, String value, Boolean isEditable, ReportDefinition reportDefinition) {
    	ReportFilter reportFilter = new ReportFilter();
    	reportFilter.setLabel(label);
    	reportFilter.setPropertyName(propertyName);
    	reportFilter.setDisplayOrder(Float.parseFloat(propertyOrder));
    	reportFilter.setOperator(operatorFilter);
    	reportFilter.setValue(value);
    	reportFilter.setEditable(isEditable);
    	reportFilter.setReportDefinition(reportDefinition);
    	return reportFilter;
    }
    
    private ReportProperty createReportProperty(String label, String propertyName, String propertyOrder, ReportDefinition reportDefinition) {
    	ReportProperty reportProperty = new ReportProperty();
    	reportProperty.setLabel(label);
    	reportProperty.setPropertyName(propertyName);
    	reportProperty.setDisplayOrder(Float.parseFloat(propertyOrder));
    	reportProperty.setReportDefinition(reportDefinition);
    	return reportProperty;
    }
    
    private ReportGroupBy createReportGroupBy(String propertyName, String label, String sortBy, Float ranking, Boolean other, ReportDefinition reportDefinition) {
    	ReportGroupBy reportGroupBy = new ReportGroupBy();
    	reportGroupBy.setPropertyName(propertyName);
    	reportGroupBy.setLabel(label);
    	reportGroupBy.setSortBy(sortBy);
    	reportGroupBy.setRanking(ranking);
    	reportGroupBy.setOther(other);
    	reportGroupBy.setReportDefinition(reportDefinition);
    	return reportGroupBy;
    }
    
    private void createReportDefinitionData(Group group) {
        User rootUser = UserService.getInstance().get(1L);

    	ReportDefinition reportDefinition = new ReportDefinition();
    	reportDefinition.setName("Report Thing Type");
    	reportDefinition.setChartFunction("MIN");
    	reportDefinition.setChartSummarizeBy("Temperature");
    	reportDefinition.setChartType("bar");
    	reportDefinition.setChartOrientation("vertical");
    	reportDefinition.setGroup(group);
        reportDefinition.setCreatedByUser(rootUser);
    	reportDefinition = ReportDefinitionService.getInstance().insert(reportDefinition);
    	
    	
    	//Adding ReportProperty
    	String[] labels = {"Serial #", "Name", "Type", "Group", "X", "Y", "Status","Temperature", "Speed"};
    	String[] propertyNames = {"serial", "name", "thingType.name", "group.name", "x", "y", "status", "Temperature", "Speed"};
    	String[] propertyOrders = {"1", "2", "3", "4", "5", "6", "7", "8", "9"};
    	
    	for(int it = 0; it < Array.getLength(labels); it++) {
    		ReportProperty reportProperty = createReportProperty(labels[it], propertyNames[it], propertyOrders[it], reportDefinition);
    		ReportPropertyService.getInstance().insert(reportProperty);
    	}
    	
    	
    	//Adding ReportFilter
    	String[] labelsFilter = {"Group Type", "Group", "Thing", "Name"};
    	String[] propertyNamesFilter = {"group.groupType", "group", "thingType", "name"};
    	String[] propertyOrdersFilter = {"1", "2", "3", "4"};
    	String[] operatorFilter = {"=", "=", "=", "="};
    	String[] value = {"", "", "", ""};
    	Boolean[] isEditable = {true, true, true, true};
    	
    	for(int it = 0; it < Array.getLength(labelsFilter); it++) {
    		ReportFilter reportFilter = createReportFilter(labelsFilter[it], propertyNamesFilter[it], propertyOrdersFilter[it], operatorFilter[it], value[it], isEditable[it],reportDefinition);
    		ReportFilterService.getInstance().insert(reportFilter);
    	}
    		
    	//Adding ReportGroupBy
    	String[] groupByNames = {"thingType.name"};
    	String[] groupByLabels = {"Type"};
    	String[] groupBySortBy = {"", ""};
    	Float[] groupByRanking = {10.0f};
    	Boolean[] groupByOther = {false};
    	
    	for(int it = 0; it < Array.getLength(groupByNames); it++) {
    		ReportGroupBy reportGroupBy = createReportGroupBy(groupByNames[it], groupByLabels[it], groupBySortBy[it], groupByRanking[it], groupByOther[it], reportDefinition);
    		ReportGroupByService.getInstance().insert(reportGroupBy);
    	}
    }
    */
}
