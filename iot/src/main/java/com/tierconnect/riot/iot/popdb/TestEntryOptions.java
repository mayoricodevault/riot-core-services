package com.tierconnect.riot.iot.popdb;

import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.appcore.utils.QueryUtils;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;

import java.lang.reflect.Array;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by rchirinos on 21/5/2015.
 */
public class TestEntryOptions {

    static Logger logger = Logger.getLogger( TestEntryOptions.class );

    public static void initJDBCDrivers() {
        //explicitly load the mysql and mssql drivers otherwise popdb would fail
        try {
            Class.forName("org.gjt.mm.mysql.Driver");
            logger.info(String.format("registering mysql jdbc driver"));
        }catch (Exception ex){
            //empty
        }
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            logger.info(String.format("registering sqlserver jdbc driver"));
        }catch (Exception ex){
            //empty
        }

    }

//    public static void closeJDBCDrivers() {
//        Enumeration<Driver> drivers = DriverManager.getDrivers();
//        while (drivers.hasMoreElements()) {
//            Driver driver = drivers.nextElement();
//            try {
//                DriverManager.deregisterDriver(driver);
//                logger.info(String.format("deregistering jdbc driver: %s", driver));
//            } catch (SQLException e) {
//                logger.error(String.format("Error deregistering driver %s", driver), e);
//            }
//        }
//    }

    public static void main( String args[] )
    {
       /* TestEntryOptions.initJDBCDrivers();
        System.getProperties().put("hibernate.connection.driver_class", "org.gjt.mm.mysql.Driver");
        System.getProperties().put("hibernate.connection.username", "root");
        System.getProperties().put("hibernate.connection.password", "control123!");
        System.getProperties().put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        System.getProperties().put("hibernate.connection.url", "jdbc:mysql://localhost:3306/riot_main");
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");

        TestEntryOptions popdb = new TestEntryOptions();
        Transaction transaction = ReportDefinitionService.getReportDefinitionDAO().getSession().getTransaction();
        transaction.begin();

        popdb.saveMethod();
        //popdb.deletemethod();

        transaction.commit();
        System.out.println("Program has finished OK");*/
        BooleanBuilder be = new BooleanBuilder();
        be = be.and( QueryUtils.buildSearch(QThingTypeFieldTemplate.thingTypeFieldTemplate, "thingTypeTemplate.id=" + 7L) );
        List<ThingTypeFieldTemplate> thingTypeFieldTemplateLst = ThingTypeFieldTemplateService.getInstance().listPaginated(be,null,null);
    }

    public void deletemethod()
    {
        String[] data = new String[]{"23"};
        for (int i = 0; i < data.length; i++) {
            ReportDefinition report = ReportDefinitionServiceBase.getReportDefinitionDAO().selectById(Long.valueOf(data[i]));
            ReportDefinitionServiceBase.getInstance().delete(report);
        }

    }

    /*Rcc ....*/
    public void saveMethod()
    {
        Group group = GroupService.getInstance().getGroupDAO().selectById(Long.valueOf("2"));
        GroupType groupType = GroupTypeService.getInstance().getGroupTypeDAO().selectById(Long.valueOf("2"));
        createReportDefinitionData(group,groupType);
    }


    /***************************************
     * create Report Definition
     * @param group
     * @param gt
     */
    private void createReportDefinitionData( Group group, GroupType gt )
    {
        User rootUser = UserService.getInstance().get( 1L );
        // Reporte3

        ReportDefinition reportDefinition5 = new ReportDefinition();
        reportDefinition5.setName( "Test Entry options 1" );
        reportDefinition5.setCreatedByUser( rootUser );
        reportDefinition5.setGroup( group );
        reportDefinition5.setReportType( "table" );
        reportDefinition5.setDefaultTypeIcon( "pin" );
        reportDefinition5.setPinLabels( true );
        reportDefinition5.setZoneLabels( true );
        reportDefinition5.setTrails( false );
        reportDefinition5.setClustering( true );
        reportDefinition5.setPlayback( true );
        reportDefinition5.setNupYup( true );
        reportDefinition5.setDefaultList( false );
        reportDefinition5.setGroupTypeFloor( gt );
        reportDefinition5.setDefaultColorIcon( "4DD000" );
        reportDefinition5 = ReportDefinitionService.getInstance().insert( reportDefinition5 );


        /*Create report property*/
        String[] labels5 = { "Brand", "Category", "RFID Tag #", "Department", "Last Detect Time", "Logical Reader", "Color", "Size", "Price",
                "Name", "Type" };
        String[] propertyNames5 = { "brand", "category", "serial", "group.name", "lastDetectTime", "logicalReader", "color", "size",
                "price", "name", "thingType.name" };
        String[] propertyOrders5 = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11" };
        Long[] propertyTypeIds5 = { 1L, 1L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 3L, 3L };
        for( int it = 0; it < Array.getLength(labels5); it++ )
        {
            ReportProperty reportProperty = createReportProperty( labels5[it], propertyNames5[it], propertyOrders5[it],
                    propertyTypeIds5[it], reportDefinition5 );
            ReportPropertyService.getInstance().insert( reportProperty );
        }

        /*Create report filter*/
        String[] labelsFilter5 = { "Brand" };
        String[] propertyNamesFilter5 = { "brand" };
        String[] propertyOrdersFilter5 = { "2" };
        String[] operatorFilter5 = { "=" };
        String[] value5 = { "Calvin Klein" };
        Boolean[] isEditable5 = { true };
        Long[] thingTypeIdReport5 = { 1L };
        for( int it = 0; it < Array.getLength( labelsFilter5 ); it++ )
        {
            ReportFilter reportFilter = createReportFilter(labelsFilter5[it], propertyNamesFilter5[it], propertyOrdersFilter5[it],
                    operatorFilter5[it], value5[it], isEditable5[it], thingTypeIdReport5[it], reportDefinition5);
            ReportFilterService.getInstance().insert( reportFilter );
        }


        //Report entry Options

        String[] namesFilterEntry = { "Screen A" };
        String[] labelsFilterEntry = { "Create Screen A" };
        for( int it = 0; it < Array.getLength( labelsFilterEntry ); it++ )
        {

            ReportEntryOption reportEntryOption = createEntryOptions(namesFilterEntry[it],labelsFilterEntry[it], true, false,
                    true, true, true, reportDefinition5);
            reportEntryOption = ReportEntryOptionService.getInstance().insert( reportEntryOption );

            /**/
            String[] labels6 = { "Brand", "Category", "RFID Tag #", "Department", "Last Detect Time", "Logical Reader", "Color", "Size", "Price",
                    "Name", "Type" };
            String[] propertyNames6 = { "brand", "category", "serial", "group.name", "lastDetectTime", "logicalReader", "color", "size",
                    "price", "name", "thingType.name" };
            String[] propertyOrders6 = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11" };
            Long[] propertyTypeIds6 = { 1L, 1L, 3L, 3L, 3L, 3L, 1L, 1L, 1L, 3L, 3L };
            for( int it6 = 0; it6 < Array.getLength(labels6); it6++ )
            {
                ReportEntryOptionProperty reportEntryOptionProperty = createReportEntryOptionProperty(labels6[it6], propertyNames6[it6], propertyOrders6[it6],
                        propertyTypeIds6[it], reportEntryOption);
                ReportEntryOptionPropertyService.getInstance().insert( reportEntryOptionProperty );
            }
            /**/


        }

    }

    private ReportFilter createReportFilter( String label, String propertyName, String propertyOrder, String operatorFilter, String value,
                                             Boolean isEditable, Long ttId, ReportDefinition reportDefinition )
    {
        ReportFilter reportFilter = new ReportFilter();
        reportFilter.setLabel( label );
        reportFilter.setPropertyName( propertyName );
        reportFilter.setDisplayOrder( Float.parseFloat( propertyOrder ) );
        reportFilter.setOperator( operatorFilter );
        reportFilter.setValue( value );
        reportFilter.setEditable( isEditable );
        reportFilter.setThingType( ThingTypeService.getInstance().get( ttId ) );
        reportFilter.setThingTypeField( ThingTypeFieldService.getInstance().getThingTypeFieldByName( propertyName ).get( 0 ) );
        reportFilter.setReportDefinition( reportDefinition );
        return reportFilter;
    }

    /*Submetodos*/
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

    /*Submetodos*/
    private ReportEntryOptionProperty createReportEntryOptionProperty( String label, String propertyName, String propertyOrder, Long propertyTypeId,
                                                 ReportEntryOption reportEntryOption )
    {
        ReportEntryOptionProperty reportProperty = new ReportEntryOptionProperty();
        reportProperty.setLabel( label );
        reportProperty.setPropertyName( propertyName );
        reportProperty.setDisplayOrder( Float.parseFloat( propertyOrder ) );
        reportProperty.setThingTypeIdReport( propertyTypeId );

        reportProperty.setReportEntryOption(reportEntryOption);
        return reportProperty;
    }

    private ReportEntryOption createEntryOptions( String name,String label, Boolean associate, Boolean deleteOption, Boolean editOption, Boolean newOption,
                                             Boolean rfidOption, ReportDefinition reportDefinition )
    {
        ReportEntryOption reportFilter = new ReportEntryOption();
        reportFilter.setName( name );
        reportFilter.setLabel( label );
        reportFilter.setAssociate( associate );
        reportFilter.setDeleteOption( deleteOption );
        reportFilter.setEditOption( editOption );
        reportFilter.setNewOption( newOption );
        reportFilter.setRFIDPrint( rfidOption );
        reportFilter.setReportDefinition( reportDefinition );

        //reportFilter.setReportEntryOptionProperties();
        return reportFilter;
    }





//
//    public void run()
//    {
//        Group rootGroup = new Group( "root" );
//        rootGroup.setTreeLevel( 1 );
//        rootGroup.setDescription( "" );
//        rootGroup.setCode("root");
//        GroupService.getInstance().insert( rootGroup );
//
//        GroupType rootGroupType = new GroupType();
//        rootGroupType.setGroup( rootGroup);
//        rootGroupType.setName( "root" );
//        rootGroupType.setDescription( "" );
//
//        GroupTypeService.getInstance().insert( rootGroupType );
//
//        rootGroup.setGroupType( rootGroupType );
//        GroupService.getGroupDAO().update( rootGroup );
//
//        HashSet<Resource> resources = new HashSet<Resource>();
//        // make sure we do not get non-appcore classes as well (e.g. vetclinic
//        // and samsung)
//        Resource moduleControl = ResourceService.getInstance().insert(Resource.getModuleResource(rootGroup, "Control", "Control"));
//
//        resources.add( ResourceService.getInstance().insert( Resource.getClassResource( rootGroup, Role.class, moduleControl ) ) );
//        resources.add( ResourceService.getInstance().insert( Resource.getClassResource( rootGroup, Resource.class, moduleControl ) ) );
//        Resource userResource = ResourceService.getInstance().insert(Resource.getClassResource(rootGroup, User.class, moduleControl));
//        resources.add(userResource);
//        resources.add( ResourceService.getInstance().insert( Resource.getClassResource( rootGroup, GroupType.class, moduleControl ) ) );
//        resources.add( ResourceService.getInstance().insert( Resource.getClassResource( rootGroup, Group.class, moduleControl ) ) );
//        resources.add( ResourceService.getInstance().insert( Resource.getClassResource( rootGroup, Field.class, moduleControl ) ) );
//
//        resources.add( ResourceService.getInstance().insert( Resource.getPropertyResource(rootGroup, userResource, "editRoamingGroup", "User Edit Roaming Group", "User Edit Roaming Group") ) );
//
//
//        GroupType tenantGroupType = new GroupType();
//        tenantGroupType.setGroup( rootGroup );
//        tenantGroupType.setName("Tenant");
//        tenantGroupType.setParent(rootGroupType);
//        tenantGroupType.setCode("tenant");
//        GroupTypeService.getInstance().insert( tenantGroupType );
//
//        Group tenantGroup = new Group();
//        tenantGroup.setParent(rootGroup);
//        tenantGroup.setName("Default Tenant");
//        tenantGroup.setCode("DT");
//        tenantGroup.setGroupType(tenantGroupType);
//        GroupService.getInstance().insert(tenantGroup);
//
//        GroupType facilityGroupType = new GroupType();
//        facilityGroupType.setGroup(tenantGroup);
//        facilityGroupType.setName("Facility");
//        facilityGroupType.setParent( tenantGroupType );
//        GroupTypeService.getInstance().insert( facilityGroupType );
//
//        Group facilityGroup = new Group();
//        facilityGroup.setParent(tenantGroup);
//        facilityGroup.setName("Default Facility");
//        facilityGroup.setCode("DF");
//        facilityGroup.setGroupType(facilityGroupType);
//        GroupService.getInstance().insert(facilityGroup);
//
//
//
//        // Populate Resources and Roles
//        Role rootRole = PopDBUtils.popRole("root", "root", "", resources, rootGroup, rootGroupType);
//        Role tenantAdminRole = PopDBUtils.popRole( "Tenant Administrator", "TA", "", resources, tenantGroup,
//                tenantGroupType );
//
//        User rootUser = PopDBUtils.popUser( "root", "root", rootGroup, rootRole );
//        rootUser.setFirstName("Root");
//        rootUser.setLastName("User");
//        rootUser.setEmail("");
//        UserService.getInstance().update( rootUser );
//
//        User tenantUser = PopDBUtils.popUser( "tenant", "tenant", tenantGroup, tenantAdminRole );
//        tenantUser.setFirstName("Tenant");
//        tenantUser.setLastName("User");
//        tenantUser.setEmail("");
//        UserService.getInstance().update( tenantUser );
//
//        Field f = PopDBUtils.popFieldService( "language", "language", "Language", rootGroup, "Look & Feel", "java.lang.String", null, true);
//        PopDBUtils.popGroupField( rootGroup, f, "en" );
//        Field f1 = PopDBUtils.popFieldService( "defaultNavBar", "defaultNavBar", "Default Module", rootGroup, "Look & Feel", "java.lang.String", null, true);
//        PopDBUtils.popGroupField( rootGroup, f1, "control" );
//        Field f2 = PopDBUtils.popFieldService( "pageSize", "pageSize", "Page Size", rootGroup, "Look & Feel", "java.lang.Integer", null, true);
//        PopDBUtils.popGroupField( rootGroup, f2, "15" );
//        Field f3 =PopDBUtils.popFieldService("thing","thing","Thing",rootGroup,"Ownership Levels","java.lang.Integer",3L,false);
//        PopDBUtils.popGroupField(rootGroup, f3, "3");
//        Field f4 =PopDBUtils.popFieldService("thingType","thingType","Thing Type",rootGroup,"Ownership Levels","java.lang.Integer",3L,false);
//        PopDBUtils.popGroupField(rootGroup, f4, "3");
//        Field f5 =PopDBUtils.popFieldService("role","role","Role",rootGroup,"Ownership Levels","java.lang.Integer",3L,false);
//        PopDBUtils.popGroupField(rootGroup, f5, "2");
//        Field f6 =PopDBUtils.popFieldService("groupType","groupType","Group Type",rootGroup,"Ownership Levels","java.lang.Integer",3L,false);
//        PopDBUtils.popGroupField(rootGroup, f6, "2");
//        Field f7 =PopDBUtils.popFieldService("zone","zone","Zone",rootGroup,"Ownership Levels","java.lang.Integer",3L,false);
//        PopDBUtils.popGroupField(rootGroup, f7, "3");
//        Field f8 =PopDBUtils.popFieldService("localMap","localMap","Local Map",rootGroup,"Ownership Levels","java.lang.Integer",3L,false);
//        PopDBUtils.popGroupField(rootGroup, f8, "3");
//        Field f9 =PopDBUtils.popFieldService("report","report","Report",rootGroup,"Ownership Levels","java.lang.Integer",3L,false);
//        PopDBUtils.popGroupField(rootGroup, f9, "3");
//        Field f10 =PopDBUtils.popFieldService("maxReportRecords","maxReportRecords","Max Report Records",rootGroup,"Look & Feel","java.lang.Integer",null,true);
//        PopDBUtils.popGroupField(rootGroup, f10, "1000000");
//        Field f11 = PopDBUtils.popFieldService( "pagePanel", "pagePanel", "Panel Pagination Size", rootGroup, "Look & Feel", "java.lang.Integer", null, true);
//        PopDBUtils.popGroupField( rootGroup, f11, "15" );
//        Field f12 = PopDBUtils.popFieldService( "alertPollFrequency", "alertPollFrequency", "Alert Poll Frequency (secs)", rootGroup, "Look & Feel", "java.lang.Integer", null, true);
//        PopDBUtils.popGroupField( rootGroup, f12, "15" );
//        Field f13 = PopDBUtils.popFieldService( "logicalReader", "logicalReader", "Logical Reader", rootGroup, "Ownership Levels", "java.lang.String", 3L, false);
//        PopDBUtils.popGroupField( rootGroup, f13, "3" );
//        Field f14 = PopDBUtils.popFieldService( "stopAlerts", "stopAlerts", "Stop Alerts", rootGroup, "Look & Feel", "java.lang.Boolean", null, true);
//        PopDBUtils.popGroupField( rootGroup, f14, "false" );
//        Field f15 = PopDBUtils.popFieldService( "shift", "shift", "Shifts", rootGroup, "Ownership Levels", "java.lang.Integer", 3L, false);
//        PopDBUtils.popGroupField( rootGroup, f15, "3" );
//        Field f16 = PopDBUtils.popFieldService( "zoneType", "zoneType", "Zone Type", rootGroup, "Ownership Levels", "java.lang.Integer", 3L, false);
//        PopDBUtils.popGroupField( rootGroup, f16, "3" );
//        Field f17 = PopDBUtils.popFieldService( "edgebox", "edgebox", "Edgebox", rootGroup, "Ownership Levels", "java.lang.Integer", 3L, false);
//        PopDBUtils.popGroupField( rootGroup, f17, "3" );
//        Field f18 = PopDBUtils.popFieldService( "emailSmtpHost", "emailSmtpHost", "Host", rootGroup, "SMTP Email Configuration", "java.lang.String", 3L, false);
//        PopDBUtils.popGroupField( rootGroup, f18, "tcexchange2010.tierconnect.com" );
//        Field f19 = PopDBUtils.popFieldService( "emailSmtpPort", "emailSmtpPort", "Port", rootGroup, "SMTP Email Configuration", "java.lang.Integer", 3L, false);
//        PopDBUtils.popGroupField( rootGroup, f19, "25" );
//        Field f20 = PopDBUtils.popFieldService( "emailSmtpUser", "emailSmtpUser", "User", rootGroup, "SMTP Email Configuration", "java.lang.String", 3L, false);
//        PopDBUtils.popGroupField( rootGroup, f20, "riottest@tierconnect.com" );
//        Field f21 = PopDBUtils.popFieldService( "emailSmtpPassword", "emailSmtpPassword", "Password", rootGroup, "SMTP Email Configuration", "java.lang.String", 3L, false);
//        PopDBUtils.popGroupField( rootGroup, f21, "" );
//        Field f22 = PopDBUtils.popFieldService( "emailSmtpTls", "emailSmtpTls", "TLS", rootGroup, "SMTP Email Configuration", "java.lang.Boolean", 3L, false);
//        PopDBUtils.popGroupField( rootGroup, f22, "false" );
//        Field f23 = PopDBUtils.popFieldService( "emailSmtpSsl", "emailSmtpSsl", "SSL", rootGroup, "SMTP Email Configuration", "java.lang.Boolean", 3L, false);
//        PopDBUtils.popGroupField( rootGroup, f23, "false" );
//        Field f24 = PopDBUtils.popFieldService( "shiftZoneValidation", "shiftZoneValidation", "Shift-Zone Validation", rootGroup, "Job Scheduling", "java.lang.Integer", null, true);
//        PopDBUtils.popGroupField( rootGroup, f24, "60" );
//        Field f25 = PopDBUtils.popFieldService( "sessionTimeout", "sessionTimeout", "Session TimeOut (mins)", rootGroup, "Security Configuration", "java.lang.Integer", 3L, false);
//        PopDBUtils.popGroupField( rootGroup, f25, "10" );
//        Field f26 = PopDBUtils.popFieldService( "zoneGroup", "zoneGroup", "Zone Group", rootGroup, "Ownership Levels", "java.lang.Integer", 3L, false);
//        PopDBUtils.popGroupField( rootGroup, f26, "3" );
//        Field f27 = PopDBUtils.popFieldService( "ipAddress", "ipAddress", "Image Server", rootGroup, "Integration", "java.lang.String", null, true);
//        PopDBUtils.popGroupField( rootGroup, f27, "10.0.31.160" );
//
//        PopDBUtils.migrateFieldService("fmcSapUrl", "fmcSapUrl", "SAP hostname", rootGroup, "Integration", "java.lang.String", 2L, false, "");
//        PopDBUtils.migrateFieldService("fmcSapUsername", "fmcSapUsername", "SAP username", rootGroup, "Integration", "java.lang.String", 2L, false, "");
//        PopDBUtils.migrateFieldService("fmcSapPassword", "fmcSapPassword", "SAP password", rootGroup, "Integration", "java.lang.String", 2L, false, "");
//        PopDBUtils.migrateFieldService("fmcSapNumberOfRetries", "fmcSapNumberOfRetries", "SAP number of retries", rootGroup, "Integration", "java.lang.Integer", 2L, false,
//                "5");
//        PopDBUtils.migrateFieldService("fmcSapWaitSecondsToRetry", "fmcSapWaitSecondsToRetry", "SAP seconds between retries", rootGroup, "Integration", "java.lang.Integer", 2L, false,
//                "30");
//        PopDBUtils.migrateFieldService("batchUpdateLogDirectory", "batchUpdateLogDirectory", "Batch Thing Update log directory", rootGroup, "Integration", "java.lang.String", 2L, false,
//                "");
//
//        Version version = new Version();
//        version.setInstallTime(new Date());
//        version.setDbVersion(""+new CodeVersion().getVersion());
//        VersionService.getInstance().insert(version);
//    }


}
