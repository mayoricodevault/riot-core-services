package com.tierconnect.riot.migration.older;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mysema.query.BooleanBuilder;
import com.tierconnect.riot.appcore.dao.ResourceDAO;
import com.tierconnect.riot.appcore.entities.*;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.*;
import com.tierconnect.riot.commons.dao.mongo.MongoDAOUtil;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.popdb.PopDBIOTUtils;
import com.tierconnect.riot.iot.services.*;
import com.tierconnect.riot.migration.DBHelper;
import org.jose4j.json.internal.json_simple.JSONObject;

import java.util.*;

/**
 * Created by agutierrez on 10/30/15.
 */
@Deprecated
public class V_030201_030300 implements MigrationStepOld
{
    @Override
    public List<Integer> getFromVersions() {
        return Arrays.asList(30201);
    }

    @Override
    public int getToVersion() {
        return 30300;
    }

    @Override
    public void migrateSQLBefore() throws Exception {
        DBHelper dbHelper = new DBHelper();
        String databaseType = dbHelper.getDataBaseType();
        dbHelper.executeSQLFile("sql/" + databaseType + "/V030201_to_030300.sql");
    }

    @Override
    public void migrateHibernate() throws Exception {
        migrateResources();
        migrateFields();
        migrateRFIDEncode();
        migrateThingInstancesRFIDEncode();
        populateMigrateReportProperty();
        migrateConnection();
    }

    public void migrateResources() {
        ResourceService resourceService = ResourceService.getInstance();
        ResourceDAO resourceDAO = ResourceService.getResourceDAO();
        Resource resourceRD = resourceDAO.selectBy("name", "reportDefinition");
        resourceRD.setAcceptedAttributes("riudaxp");
        resourceService.update(resourceRD);

        Group rootGroup = GroupService.getInstance().getRootGroup();

        if (resourceDAO.selectBy("name", Resource.REPORT_INSTANCES_MODULE) == null) {
            Resource moduleReportInstances = ResourceService.getInstance().insert(Resource.getModuleResource(rootGroup, Resource.REPORT_INSTANCES_MODULE, "Report Instances"));
            List<Resource> resources = resourceDAO.selectAllBy(QResource.resource.type.eq(ResourceType.REPORT_DEFINITION.getId()));
            for (Resource resource0: resources) {
                resource0.setParent(moduleReportInstances);
            }
        }

        RoleResourceService roleResourceService = RoleResourceService.getInstance();
        List<RoleResource> roleResources = roleResourceService.getRoleResourceDAO().selectAllBy(QRoleResource.roleResource.resource.eq(resourceRD));
        for (RoleResource roleResource : roleResources) {
            if (roleResource.getPermissionsList().contains("r")) {
                Set set = new LinkedHashSet<>(roleResource.getPermissionsList());
                set.add("x");
                set.add("p");
                roleResource.setPermissions(set);
                roleResourceService.update(roleResource);
            }
        }
        ReportDefinitionService reportDefinitionService = ReportDefinitionService.getInstance();
        ReportEntryOptionService reos = ReportEntryOptionService.getInstance();
        List<ReportDefinition> reportDefinitions = reportDefinitionService.getReportDefinitionDAO().selectAll();
        for (ReportDefinition reportDefinition: reportDefinitions) {
            reportDefinitionService.createResource(reportDefinition);
            for (ReportEntryOption reportEntryOption : reportDefinition.getReportEntryOption()) {
                reos.createResource(reportEntryOption);
            }
        }

        Resource rdAssignThing = resourceDAO.selectBy("name", "reportDefinition_assignThing");
        Resource rdUnAssignThing = resourceDAO.selectBy("name", "reportDefinition_unAssignThing");
        Resource rdInlineEdit = resourceDAO.selectBy("name", "reportDefinition_inlineEdit");
        Resource reportDefinitionResource = resourceRD;

        Resource r = resourceDAO.selectBy("name", "reportDefinition_inlineEditGroup");
        boolean isNew = false;
        if (r == null) {
            isNew = true;
            r = new Resource();
        }
        r.setGroup(rootGroup);
        r.setFqname("Report Inline Edit");
        r.setName("reportDefinition_inlineEditGroup");
        r.setAcceptedAttributes("x");
        r.setLabel(""+r.getFqname());
        r.setDescription(r.getFqname());
        r.setTreeLevel(2);
        r.setParent(reportDefinitionResource.getParent());
        r.setType(ResourceType.MODULE.getId());
        if (isNew) {
            resourceService.insert(r);
        }

        rdAssignThing.setParent(r);
        rdUnAssignThing.setParent(r);
        rdInlineEdit.setParent(r);

    }

    @Override
    public void migrateSQLAfter() throws Exception {

    }

    private void migrateFields()
    {
        GroupService groupService = GroupService.getInstance();
        Group rootGroup = groupService.getRootGroup();
        PopDBUtils.migrateFieldService("reloadAllThingsThreshold", "reloadAllThingsThreshold", "Things Cache Reload Threshold", rootGroup, "Import Configuration", "java.lang.Long", 3L, true, "1000");
        PopDBUtils.migrateFieldService("sendThingFieldTickle", "sendThingFieldTickle", "Run Rules After Import", rootGroup, "Import Configuration", "java.lang.Boolean", 3L, true, "true");
        PopDBUtils.migrateFieldService("fmcSapEnableSapSyncOnImport", "fmcSapEnableSapSyncOnImport", "Enable SAP Sync on import", rootGroup, "Import Configuration", "java.lang.Boolean", 2L, false,
                "false");
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

    public void migrateConnection()
    {
        Group rootGroup = GroupService.getInstance().getRootGroup();
        ConnectionType dbConnectionType = null;
        dbConnectionType = ConnectionTypeService.getInstance().getConnectionTypeDAO().selectBy(QConnectionType.connectionType.code.eq("DBConnection"));

        Connection connectionTemp = ConnectionService.getConnectionDAO().selectBy(QConnection.connection.code.eq("MSSQLServer"));
        if(connectionTemp == null){// Create only if connection does not exist
            // SQLServer connection example
            Connection connection = new Connection();
            connection.setName( "MSSQLServer" );
            connection.setCode( "MSSQLServer" );
            connection.setGroup(rootGroup);
            connection.setConnectionType( dbConnectionType );
            JSONObject jsonProperties = new JSONObject();
            Map<String, String> mapProperties = new LinkedHashMap<String, String>();
            mapProperties.put( "driver", "com.microsoft.sqlserver.jdbc.SQLServerDriver" );
            mapProperties.put( "password", "Y29udHJvbDEyMyE=" );
            mapProperties.put( "schema", "DWMS" );
            mapProperties.put( "url", "jdbc:sqlserver://localhost;DatabaseName=DWMS" );
            mapProperties.put( "user", "sa" );
            jsonProperties.putAll(mapProperties);
            connection.setProperties(jsonProperties.toJSONString());
            ConnectionService.getInstance().insert( connection );
        }

    }

    /*
	* Method to migrate report Property
	* */
    public void populateMigrateReportProperty()
    {

        try{
            BooleanBuilder be = new BooleanBuilder();
            ReportDefinitionService.getInstance().listPaginated( be, null, null );
            for( ReportDefinition reportDefinition : ReportDefinitionService.getInstance().listPaginated( be,  null, null ))
            {
                if(reportDefinition.getReportProperty()!=null && reportDefinition.getReportProperty().size()>0)
                {
                    for( ReportProperty reportProperty: reportDefinition.getReportProperty() )
                    {
                        if(reportProperty.getThingType()!=null)
                        {
                            //ThingType thingType = ThingTypeService.getInstance().get( reportProperty.getThingTypeIdReport() );
//							if(thingType!=null)
//							{
                            for( ThingTypeField thingTypeField : reportProperty.getThingType().getThingTypeFields() )
                            {
                                if( reportProperty.getPropertyName().equals( thingTypeField.getName() ) )
                                {
                                    reportProperty.setThingTypeField( thingTypeField );
                                    reportProperty.setParentThingType( null );
                                    ReportPropertyService.getReportPropertyDAO().update( reportProperty );
                                    break;
                                }
                            }
//							}

                        }
                    }
                }
                if(reportDefinition.getReportFilter()!=null && reportDefinition.getReportFilter().size()>0)
                {
                    for( ReportFilter reportFilter: reportDefinition.getReportFilter() )
                    {
                        if(reportFilter.getThingType()!=null)
                        {
                            ThingType thingType = reportFilter.getThingType();
                            for( ThingTypeField thingTypeField : thingType.getThingTypeFields() )
                            {
                                if( reportFilter.getPropertyName().equals( thingTypeField.getName() ) )
                                {
                                    reportFilter.setThingTypeField( thingTypeField );
                                    reportFilter.setParentThingType( null );
                                    ReportFilterService.getReportFilterDAO().update( reportFilter );
                                    break;
                                }
                            }

                        }
                    }
                }
                if(reportDefinition.getReportRule()!=null && reportDefinition.getReportRule().size()>0)
                {
                    for( ReportRule reportRule: reportDefinition.getReportRule() )
                    {
                        if(reportRule.getThingType()!=null)
                        {
							/*ThingType thingType = ThingTypeService.getInstance().get( reportRule.getThingTypeIdReport() );
							if(thingType!=null)
							{*/
                            for( ThingTypeField thingTypeField : reportRule.getThingType().getThingTypeFields() )
                            {
                                if( reportRule.getPropertyName().equals( thingTypeField.getName() ) )
                                {
                                    reportRule.setThingTypeField( thingTypeField );
                                    reportRule.setParentThingType( null );
                                    ReportRuleService.getReportRuleDAO().update( reportRule );
                                    break;
                                }
                            }
                            //}
                        }
                    }
                }

                if(reportDefinition.getReportGroupBy()!=null && reportDefinition.getReportGroupBy().size()>0)
                {
                    for( ReportGroupBy reportGroupBy: reportDefinition.getReportGroupBy() )
                    {
                        if(reportGroupBy.getThingType()!=null)
                        {
							/*ThingType thingType = ThingTypeService.getInstance().get( reportRule.getThingTypeIdReport() );
							if(thingType!=null)
							{*/
                            for( ThingTypeField thingTypeField : reportGroupBy.getThingType().getThingTypeFields() )
                            {
                                if( reportGroupBy.getPropertyName().equals( thingTypeField.getName() ) )
                                {
                                    reportGroupBy.setThingTypeField( thingTypeField );
                                    reportGroupBy.setParentThingType( null );
                                    ReportGroupByService.getReportGroupByDAO().update( reportGroupBy );
                                    break;
                                }
                            }
                            //}
                        }
                    }
                }
            }

        }catch(Exception e)
        {
            e.printStackTrace();
        }

    }


}

