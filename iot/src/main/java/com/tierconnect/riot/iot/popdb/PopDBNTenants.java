package com.tierconnect.riot.iot.popdb;

import com.tierconnect.riot.appcore.dao.CassandraUtils;
import com.tierconnect.riot.appcore.entities.Group;
import com.tierconnect.riot.appcore.entities.GroupType;
import com.tierconnect.riot.appcore.popdb.PopDBRequired;
import com.tierconnect.riot.appcore.popdb.PopDBUtils;
import com.tierconnect.riot.appcore.services.GroupService;
import com.tierconnect.riot.appcore.services.GroupTypeService;
import com.tierconnect.riot.appcore.utils.Configuration;
import org.hibernate.Transaction;

/**
 * @author agutierrez
 */
public class PopDBNTenants {
    public static void main(String args[]) throws Exception
    {
        PopDBRequired.initJDBCDrivers();
        System.getProperties().put("hibernate.hbm2ddl.auto", "update");
        CassandraUtils.init(Configuration.getProperty("cassandra.host"), Configuration.getProperty("cassandra.keyspace"));
        PopDBNTenants popdb = new PopDBNTenants();
        Transaction transaction = GroupService.getInstance().getGroupDAO().getSession().getTransaction();
        transaction.begin();
        PopDBIOTUtils.initShiroWithRoot();
        popdb.run();
        transaction.commit();
        System.exit(0);
    }

    public void run( ) {
        // TODO: make selection more robust ?
        Group rootGroup = GroupService.getInstance().get(1L);
        // TODO: make selection more robust ?
        GroupType tenantGroupType = GroupTypeService.getInstance().get(2L);

        System.out.print( "rootGroup=" + rootGroup );
        System.out.print( "tenant=" + tenantGroupType );

        for (int i=0; i<20; i++) {
            PopDBUtils.popGroup("Tenant"+i, "tenant"+i, rootGroup, tenantGroupType, "Test tenant "+i);
        }

    }
}
