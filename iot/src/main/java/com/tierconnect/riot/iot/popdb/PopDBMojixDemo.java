package com.tierconnect.riot.iot.popdb;






import java.lang.reflect.Array;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.utils.Configuration;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;

import com.google.common.collect.Lists;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.GroupTypeService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;
import com.tierconnect.riot.appcore.services.UserService;

/**
 * this seems to be for hand held dev ?
 *
 */
public class PopDBMojixDemo {
	private static final Logger logger = Logger.getLogger( PopDBRiot.class );

	public static void main( String args[] ) throws Exception
	{
		PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
		CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));
		PopDBMojixDemo popdb = new PopDBMojixDemo();
		Transaction transaction = GroupService.getInstance().getGroupDAO().getSession().getTransaction();
		transaction.begin();
		PopDBIOTUtils.initShiroWithRoot();
		popdb.run();
		transaction.commit();
		System.exit( 0 );
	}

	public void run()
	{
		Group rootGroup = GroupService.getInstance().getRootGroup();
		GroupType tenantGroupType = GroupTypeService.getInstance().getTenantGroupType();
		Role tenantAdminRole = RoleService.getInstance().getTenantAdminRole();
		List<Group> groups = createCarvoyantDemoData();
		//createReportDefinitionData2( rootGroup );
	}

	private List<Group> createCarvoyantDemoData()
	{
        User rootUser = UserService.getInstance().get(1L);
		// root group type
		GroupType rootGroupType = GroupTypeService.getInstance().get( 1L );
		

		// company type
		GroupTypeService.getInstance().update( rootGroupType );
		GroupType tenantGroupType = GroupTypeService.getInstance().get( 2L );
		tenantGroupType.setName("Company");
		//GroupType tenantGroupType1 = GroupTypeService.getInstance().get( 2L );
		//tenantGroupType1.setName( "Vendor" );
		
		
		// define root group
		Group rootGroup = GroupService.getInstance().get( 1L );
		// Groups Mojix
		Resource moduleMobileWorkflows = ResourceService.getInstance().insert(Resource.getModuleResource(rootGroup, "Mobile Workflows", "Mobile Workflows"));

				Group mojix = PopDBUtils.popGroup( "Mojix Mobile", "Mojix Mobile", rootGroup, tenantGroupType, "" );

		//resource test
				//resource test
				Resource re=new Resource();
		        re.setAcceptedAttributes("x");
		        re.setGroup(mojix);
		re.setParent(moduleMobileWorkflows);
		re.setTreeLevel(2);
		        re.setLabel("workflows.Check-in.step01");
		        re.setType(ResourceType.METHOD.getId());
		        re.setFqname("");
		        re.setName("workflows.Check-in.step01");
		        
		        Resource re1=new Resource();
		        re1.setAcceptedAttributes("x");
		        re1.setGroup(mojix);
		re1.setParent(moduleMobileWorkflows);
		re1.setTreeLevel(2);
		        re1.setLabel("workflows.Check-in.step02");
		        re1.setType(ResourceType.METHOD.getId());
		        re1.setFqname("");
		        re1.setName("workflows.Check-in.step02");
		        
		        Resource re2=new Resource();
		        re2.setAcceptedAttributes("x");
		        re2.setGroup(mojix);
		re2.setParent(moduleMobileWorkflows);
		re2.setTreeLevel(2);
		        re2.setLabel("workflows.Check-in.step03");
		        re2.setType(ResourceType.METHOD.getId());
		        re2.setFqname("");
		        re2.setName("workflows.Check-in.step03");
		        
		        Resource re3=new Resource();
		        re3.setAcceptedAttributes("x");
		        re3.setGroup(mojix);
		re3.setParent(moduleMobileWorkflows);
		re3.setTreeLevel(2);
		        re3.setLabel("workflows.Check-in");
		        re3.setType(ResourceType.METHOD.getId());
		        re3.setFqname("");
		        re3.setName("workflows.Check-in");
		        
		        Resource re4=new Resource();
		        re4.setAcceptedAttributes("x");
		        re4.setGroup(mojix);
		re4.setParent(moduleMobileWorkflows);
		re4.setTreeLevel(2);
		        re4.setLabel("workflows.Check-out");
		        re4.setType(ResourceType.METHOD.getId());
		        re4.setFqname("");
		        re4.setName("workflows.Check-out");
		        
		        
		        Resource re5=new Resource();
		        re5.setAcceptedAttributes("x");
		        re5.setGroup(mojix);
		re5.setParent(moduleMobileWorkflows);
		re5.setTreeLevel(2);
		        re5.setLabel("workflows.Retrieve");
		        re5.setType(ResourceType.METHOD.getId());
		        re5.setFqname("");
		        re5.setName("workflows.Retrieve");
		        
		        
		        Resource re7=new Resource();
		        re7.setAcceptedAttributes("x");
		        re7.setGroup(mojix);
		re7.setParent(moduleMobileWorkflows);
		re7.setTreeLevel(2);
		        re7.setLabel("workflows.Check-out.step01");
		        re7.setType(ResourceType.METHOD.getId());
		        re7.setFqname("");
		        re7.setName("workflows.Check-out.step01");
		        
		        
		        
		        Resource re6=new Resource();
		        re6.setAcceptedAttributes("x");
		        re6.setGroup(mojix);
		re6.setParent(moduleMobileWorkflows);
		re6.setTreeLevel(2);
		        re6.setLabel("workflows.Find-It");
		        re6.setType(ResourceType.METHOD.getId());
		        re6.setFqname("");
		        re6.setName("workflows.Find-It");
		        
		        ResourceService.getInstance().insert(re);
		        ResourceService.getInstance().insert(re1);
		        ResourceService.getInstance().insert(re2);
		        ResourceService.getInstance().insert(re3);
		        ResourceService.getInstance().insert(re4);
		        ResourceService.getInstance().insert(re5);
		        ResourceService.getInstance().insert(re6);
		        ResourceService.getInstance().insert(re7);
		       // re.setLabel("Stage 2");
		       // ResourceService.getInstance().insert(re);
		       // re.setLabel("Stage 3");
		       // ResourceService.getInstance().insert(re);
		// facility, test wing and zone
		//GroupType storeGroupType = PopDBUtils.popGroupType( "Store", mojix, tenantGroupType, "Store" );
		GroupType storeGroupType = GroupTypeService.getInstance().get(3L);
		GroupService.getInstance().update( rootGroup );
		
		// facility santa monica
		Group santaMonica = PopDBUtils.popGroup("Santa Monica", "Santa Monica", mojix, storeGroupType, "");
		
		// Role
		Role rootRole = RoleService.getInstance().get( 1L );
		rootRole.setName( "Root Administrator" );
		rootRole.setDescription( "Root Administrator" );
		RoleService.getInstance().update( rootRole );
		Role tenantRole = RoleService.getInstance().get( 2L );
		tenantRole.setName( "Company Administrator" );
		tenantRole.setGroup(mojix);
		RoleService.getInstance().update( tenantRole );

     
        ThingType assetThingType = new ThingType();
        assetThingType.setName( "Assets" );
        assetThingType.setGroup( santaMonica );
        assetThingType.setThingTypeCode("mojix.assets");
        
        Set<ThingTypeField> fields = new HashSet<>();

        fields.add( new ThingTypeField( "status", "", "",ThingTypeField.TypeParent.TYPE_PARENT_DATA_TYPE.value
				, DataTypeService.getInstance().get( ThingTypeField.Type.TYPE_TEXT.value) , null) );
        
        
        ThingTypeService.getInstance().insert(assetThingType);
        for( ThingTypeField field : fields )
		{
			field.setThingType( assetThingType );
			ThingTypeFieldService.getInstance().insert( field );
		}
        assetThingType.setThingTypeFields(fields);

		ThingService ts = ThingService.getInstance();
        for (int i = 1; i <=10; i++) {
        	String sn = String.format( "Asset%02d",  i );
        	String sn1 = String.format( "%04d",  i );

			ts.insert(assetThingType, sn, sn1, santaMonica, rootUser);
		}
		
		
		// Users
		rootUser.setEmail( "root@company.com" );
		//User admin = PopDBUtils.popUser( "Admin", mojix, tenantRole );

		UserService.getInstance().update( rootUser );
		Role handHeld=PopDBUtils.popRole("mobileuser", "HandHeld Check-In", null, mojix, storeGroupType);
		Role handHeldTest=PopDBUtils.popRole("mobilerestricted", "HandHeld Restricted", null, mojix, storeGroupType);
		//Role inventoryManager=PopDBUtils.popRole("Inventory Manager", "sm", "Inventory manager", null, mojix, storeGroupType);
		
        List<Resource> resources1 = ResourceService.getInstance().list();
		
		//List<Resource> resources = ResourceService.getInstance().list();
		for( Resource resource : resources1)
		{
		if(!(resource.getName().toString().equals("workflows.Check-in.step01") || resource.getName().toString().equals("workflows.Check-in.step02") ||resource.getName().toString().equals("workflows.Check-in.step03")||resource.getName().toString().equals("workflows.Check-in") ||resource.getName().toString().equals("workflows.Check-out") ||resource.getName().toString().equals("workflows.Retrieve") || resource.getName().toString().equals("workflows.Find-It")|| resource.getName().toString().equals("workflows.Check-out.step01")  )){
			if (resource.getName().toString().equals("reportDefinition") || resource.getName().toString().equals("reportFilter")
				|| resource.getName().toString().equals("reportProperty")) {
			
				RoleResourceService.getInstance().insert(handHeld, resource, "riu");
						
			}
			else{
				
					System.out.println(resource.getName().toString());
					        if (resource.getName().toString().equals("thingType")) {
							System.out.println("entrooo");
							RoleResourceService.getInstance().insert(handHeld, resource, "ru");
							RoleResourceService.getInstance().insert(handHeldTest, resource, "ru");
						}
					        if (resource.getName().toString().equals("thing")) {
					        	RoleResourceService.getInstance().insert(handHeldTest, resource, "r");
								RoleResourceService.getInstance().insert(handHeld, resource, "r");
					        }
				
			}
		}
		else{
			
			if(resource.getName().toString().equals("workflows.Check-in") ||resource.getName().toString().equals("workflows.Check-in.step01"))
			{RoleResourceService.getInstance().insert(handHeldTest, resource, "x");
			RoleResourceService.getInstance().insert(handHeld, resource, "x");
			}
			else{
				RoleResourceService.getInstance().insert(handHeld, resource, "x");	
			}
		}
		}
        
		//Roles for store manager
				for( Resource resource : resources1) {
					if(!(resource.getName().toString().equals("workflows.Check-in.step01") || resource.getName().toString().equals("workflows.Check-in.step02") ||resource.getName().toString().equals("workflows.Check-in.step03")||resource.getName().toString().equals("workflows.Check-in") ||resource.getName().toString().equals("workflows.Check-out") ||resource.getName().toString().equals("workflows.Retrieve") || resource.getName().toString().equals("workflows.Find-It")|| resource.getName().toString().equals("workflows.Check-out.step01")  )){
						if (!resource.getName().startsWith("license")) {
							RoleResourceService.getInstance().insert(handHeld, resource, resource.getAcceptedAttributes());
						}
					}
				}
        //user mojix
        User samUser = PopDBUtils.popUser( "sam", "sam", mojix, handHeld );
        samUser.setFirstName("Sam");
        samUser.setLastName("Levy");
        samUser.setEmail("sam.levy@mojix.com ");
        
        User paulUser = PopDBUtils.popUser( "paulb", "paulb", mojix, handHeldTest );
        paulUser.setFirstName("Paul");
        paulUser.setLastName("Barriga");
        paulUser.setEmail("paul.barriga@mojix.com ");

        
       createReportDefinitionData(mojix);
		return Lists.newArrayList( mojix, santaMonica );
	}
	
	private ReportFilter createReportFilter( String label, String propertyName, String propertyOrder,
			String operatorFilter, String value, Boolean isEditable, ReportDefinition reportDefinition )
	{
		ReportFilter reportFilter = new ReportFilter();
		reportFilter.setLabel( label );
		reportFilter.setPropertyName( propertyName );
		reportFilter.setDisplayOrder( Float.parseFloat( propertyOrder ) );
		reportFilter.setOperator( operatorFilter );
		reportFilter.setValue( value );
		reportFilter.setEditable( isEditable );
		reportFilter.setReportDefinition( reportDefinition );
		return reportFilter;
	}

	private ReportProperty createReportProperty( String label, String propertyName, String propertyOrder,
			ReportDefinition reportDefinition )
	{
		ReportProperty reportProperty = new ReportProperty();
		reportProperty.setLabel( label );
		reportProperty.setPropertyName( propertyName );
		reportProperty.setDisplayOrder( Float.parseFloat( propertyOrder ) );
		reportProperty.setReportDefinition( reportDefinition );
		return reportProperty;
	}
	
	private ReportProperty createReportProperty( String label, String propertyName, String propertyOrder, Long propertyTypeId, 
			ReportDefinition reportDefinition )
	{
		ReportProperty reportProperty = new ReportProperty();
		reportProperty.setLabel( label );
		reportProperty.setPropertyName( propertyName );
		reportProperty.setDisplayOrder( Float.parseFloat( propertyOrder ) );
		reportProperty.setThingType( ThingTypeService.getInstance().get( propertyTypeId ));
		reportProperty.setThingTypeField( ThingTypeFieldService.getInstance().getThingTypeFieldByName( propertyName ).get( 0 ) );

		reportProperty.setReportDefinition( reportDefinition );
		return reportProperty;
	}
	
	private ReportProperty createReportProperty( String label, String propertyName, String propertyOrder, Long propertyTypeId,Boolean as, 
			ReportDefinition reportDefinition )
	{
		ReportProperty reportProperty = new ReportProperty();
		reportProperty.setLabel( label );
		reportProperty.setPropertyName( propertyName );
		reportProperty.setDisplayOrder( Float.parseFloat( propertyOrder ) );
		reportProperty.setThingType( ThingTypeService.getInstance().get( propertyTypeId ));
		reportProperty.setThingTypeField( ThingTypeFieldService.getInstance().getThingTypeFieldByName( propertyName ).get( 0 ) );

		reportProperty.setReportDefinition( reportDefinition );
		return reportProperty;
	}
	
	
	private ReportRule createReportRule( String propertyName, String operator, String value, String color, String style,
			ReportDefinition reportDefinition )
	{
		ReportRule reportRule = new ReportRule();
		reportRule.setPropertyName( propertyName );
		reportRule.setOperator( operator );
		reportRule.setValue( value );
		reportRule.setColor( color );
		reportRule.setStyle( style );
		reportRule.setReportDefinition( reportDefinition );
		return reportRule;
	}

	private void createReportDefinitionData(Group group) {
        User rootUser = UserService.getInstance().get(1L);

		ReportDefinition reportDefinition3 = new ReportDefinition();
		reportDefinition3.setName("All Assets");
		reportDefinition3.setGroup(group);
		reportDefinition3.setReportType("table");
        reportDefinition3.setDefaultTypeIcon("pin");
		
		reportDefinition3.setPinLabels(false);
		reportDefinition3.setZoneLabels(false);
		reportDefinition3.setTrails(false);
		reportDefinition3.setClustering(false);
		reportDefinition3.setPlayback(true);
		reportDefinition3.setNupYup(false);
		reportDefinition3.setDefaultList(false);
        reportDefinition3.setCreatedByUser(rootUser);
		reportDefinition3 = ReportDefinitionService.getInstance().insert(
				reportDefinition3);
		reportDefinition3.setIsMobile(Boolean.FALSE);
		reportDefinition3.setIsMobileDataEntry(Boolean.FALSE);
		String[] labels3 = { "Type","Name","Serial","Status" };
		String[] propertyNames3 = { "thingType.name", "name",
				"serial","status" };
		String[] propertyOrders3 = { "1", "2", "3", "4" };
		Long [] propertyTypeIds3 = {4L, 4L, 4L, 4L};
		for (int it = 0; it < Array.getLength(labels3); it++) {
			ReportProperty reportProperty = createReportProperty(labels3[it],
					propertyNames3[it], propertyOrders3[it],propertyTypeIds3[it], reportDefinition3);
			ReportPropertyService.getInstance().insert(reportProperty);
		}

		String[] labelsFilter3 = {  "Thing Type" };
		String[] propertyNamesFilter3 = { "thingType.id" };
		String[] propertyOrdersFilter3 = { "1" };
		String[] operatorFilter3 = {  "=" };
		String[] value3 = { "4" };
		Boolean[] isEditable3 = { true };

		for (int it = 0; it < Array.getLength(labelsFilter3); it++) {
			ReportFilter reportFilter = createReportFilter(labelsFilter3[it],
					propertyNamesFilter3[it], propertyOrdersFilter3[it],
					operatorFilter3[it], value3[it], isEditable3[it],
					reportDefinition3);
			ReportFilterService.getInstance().insert(reportFilter);
		}
	}
	
	
}
