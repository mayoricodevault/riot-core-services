package com.tierconnect.riot.iot.popdb;

import java.util.*;

import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.iot.entities.*;
import com.tierconnect.riot.iot.services.*;
import org.apache.log4j.Logger;
import org.hibernate.Transaction;

import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.entities.Resource;
import com.tierconnect.riot.appcore.entities.Role;
import com.tierconnect.riot.appcore.entities.User;
import com.tierconnect.riot.iot.entities.ZoneType;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.GroupTypeService;
import com.tierconnect.riot.appcore.services.ResourceService;
import com.tierconnect.riot.appcore.services.RoleResourceService;
import com.tierconnect.riot.appcore.services.RoleService;

public class PopDBVizixRetail
{
    private static final Logger logger = Logger.getLogger( PopDBMojixRetail.class );

    public static void main( String args[] ) throws Exception
    {
        PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
        //CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));


        PopDBVizixRetail popdb = new PopDBVizixRetail();
        Transaction transaction = GroupService.getGroupDAO().getSession().getTransaction();
        transaction.begin();
        PopDBIOTUtils.initShiroWithRoot();
        popdb.run();
        transaction.commit();

        Transaction transaction2 = GroupService.getGroupDAO().getSession().getTransaction();
        logger.info( "******* Populating Things... " );
        transaction2.begin();
        GroupService.getGroupDAO().getSession().refresh( ThingTypeService.getInstance().get( 5L ) );
        transaction2.commit();
        logger.info( "******* End Populating Things... " );
        System.exit( 0 );
    }

    public void run()
    {
        createData();


    }

    private void createData()
    {
        PopDBMojixUtils.modifyExistingRecords();


        Group rootGroup = GroupService.getInstance().getRootGroup();
        GroupType tenantGroupType = GroupTypeService.getInstance().getTenantGroupType();

        // group mojix

        Group vizix= PopDBUtils.popGroup("ViZix Retail","ViZix.retail",GroupService.getInstance().get(1L),tenantGroupType,"");

        GroupType storeGroupType=PopDBUtils.popGroupType("Store",vizix,tenantGroupType,"");


        // Groups Mojix
        // facility
        Group mainStore = PopDBUtils.popGroup( "Main Store", "Retail.Main.Store", vizix, storeGroupType, "" );
        Role tenantRole = RoleService.getInstance().getTenantAdminRole();


        // MINS: -118.444142 34.047880
        LocalMap localmap=PopDBIOTUtils.populateFacilityMap( "Main Store", "images/store.png", mainStore, 55.219885, 120.73,
                25.139854, 72.6,  55.219885, 25.139854, 0.0 ,"m");

        //Zone Type Default
        ZoneType ztd=PopDBIOTUtils.popZoneType(mainStore, "Main Store Zone Type", "MainStoreZoneType", null);


        // ThingTypes
        ThingType tag= PopDBIOTUtils.popThingTypeRFID(mainStore, "retail.RFID.tag");
        tag.setName("Retail RFID Tag");
        ThingType sku=PopDBMojixUtils.popThingTypeSKU(mainStore,"SKU");
        ThingType customer = PopDBMojixUtils.popThingTypeCustomer(mainStore, "Customer");
        ThingType productThingType = PopDBMojixUtils.popThingTypeProductVizix(mainStore, "Product",customer,sku);
        PopDBIOTUtils.popThingTypeMap( productThingType, tag );



        Role companyadmin = PopDBUtils.popRole( "Vizix Administrator", "Vizix Administrator", null, vizix,
                storeGroupType );
        List<Resource> resources1    = ResourceService.list();
        for(Resource r1:resources1){

            RoleResourceService.getInstance().insert( companyadmin, r1, r1.getAcceptedAttributes());
        }


        User vizixadmin = PopDBUtils.popUser( "vizixadmin", "vizixadmin", vizix, companyadmin);



    }




}
